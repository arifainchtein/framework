package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;

/**
 * Traffic watch: who talks to where (design: OpenWrtOne/documents/05-teleonome-design.md, section 7c;
 * added 2026-11-04).
 *
 * Every Traffic Poll Seconds (30) it asks the router, through ubus "file exec" of
 * /usr/libexec/cleotilde-traffic (OpenWrtOne/code/traffic, installed by install_router_traffic.sh):
 *   conntrack  every IPv4 connection leaving the house with its byte counters (nf_conntrack_acct = 1).
 *              Connections stay in the table 2-3 min after they end, so a 30 s poll misses almost nothing.
 *              The difference since the last poll is what the device sent / received in those 30 s.
 *   dns        the dnsmasq query log since the last call (--log-queries=extra: every line carries the
 *              asking device), so each destination IP gets the name the device looked up.
 * Devices: LAN IP -> MAC and name from NetworkSensor.json (the controller's device list). Countries:
 * DB-IP Country Lite in Postgres (TrafficStore.country), offline.
 *
 * Rows are summed per hour x device x destination x port x protocol and written to Postgres every
 * Traffic Flush Minutes (5); a restart loses at most that much. The first poll after a start only
 * takes the counters as a baseline, so long-lived connections aren't counted twice.
 *
 * Rules (each raises a Suspicious Event, at most once per device x rule x detail per day):
 *   Watchlist Country  traffic to a Traffic Watchlist country (CN RU KP IR)
 *   Odd Port           outbound to a Traffic Odd Ports port (telnet, SMB, IRC, Tor...)
 *   Night Upload       a device uploads more than Traffic Night Upload MB inside Traffic Night Hours (0-5)
 *   Baseline Jump      a device's upload today > Traffic Baseline Factor x its 7-day daily average
 *                      (and above Traffic Baseline Minimum MB)
 *   Exfil Ratio        upload today > 3 x download and > Traffic Baseline Minimum MB, for a device that
 *                      over the last 7 days downloaded more than it uploaded
 *   New Country        a country the device has never talked to, once it has Traffic Learning Days of history
 *   Unknown Device     a device that isn't on the whitelist sends traffic to the internet
 * The allowlist (TrafficAllowlist.json in the working directory, written by the web page's Allow button,
 * kept out of the public denome) silences known-good traffic: [{"Mac", "Rule", "Country", "Domain"}],
 * empty or "*" matches anything, Domain matches the end of the name ("tuya.com" covers "a1.tuya.com").
 *
 * The controller reads today's figures for sensor tokens 29-38, LED 4 and OLED page 5.
 */
public class TrafficWatcher extends Thread {

	public static final String ALLOWLIST_FILE = "TrafficAllowlist.json";
	static final String HELPER = "/usr/libexec/cleotilde-traffic";
	private static final int DOMAIN_CACHE_SIZE = 30_000;
	private static final int SERIAL_CACHE_SIZE = 2_000;
	private static final long HOUR = 3_600_000L, DAY = 86_400_000L;
	private static final double MB = 1024.0 * 1024.0;

	private final Logger logger = Logger.getLogger(getClass());
	private final OpenWrtUbusClient ubus;
	private final TrafficStore store;

	// settings (Microcontroller Config Parameters, see CleotildeMicroController)
	long pollMillis = 30_000, flushMillis = 5 * 60_000;
	Set<String> watchlist = new HashSet<>(Arrays.asList("CN", "RU", "KP", "IR"));
	Set<Integer> oddPorts = new HashSet<>(Arrays.asList(23, 2323, 445, 139, 135, 6667, 6697, 9001, 9030, 9050, 1080, 3389, 5900));
	int nightStartHour = 0, nightEndHour = 5;
	double nightUploadMB = 20, baselineFactor = 10, baselineMinimumMB = 50;
	int learningDays = 7, flowDays = 60, dnsDays = 30;
	boolean alertUnknownDevices = true;
	/** The house network; any other source is the router itself (its WAN address). */
	String lanPrefix = "192.168.1.";

	// connection counters at the last poll: "proto src sport dst dport" -> {up, down}
	private Map<String, long[]> lastCounters = null;
	private final Map<String, TrafficStore.Flow> pendingFlows = new HashMap<>();
	private final Map<String, Object[]> pendingDns = new HashMap<>();
	private long lastFlushMillis = System.currentTimeMillis();

	// DNS: serial -> first name asked (so a CNAME chain is reported by the name the device asked for),
	// "client|answerIp" -> name, answerIp -> name (when another device asked)
	private final Map<String, String> nameBySerial = lru(SERIAL_CACHE_SIZE);
	private final Map<String, String> nameByClientAndIp = lru(DOMAIN_CACHE_SIZE);
	private final Map<String, String> nameByIp = lru(DOMAIN_CACHE_SIZE);

	// devices from NetworkSensor.json: ip -> {mac, name, whitelisted}
	private Map<String, String[]> devicesByIp = new HashMap<>();
	/** mac -> the name last written onto its stored rows (TrafficStore.renameDevice); empty at start = sync all once */
	private final Map<String, String> namesOnRows = new HashMap<>();
	private long devicesFileModified = -1;

	// today (reset at midnight; reloaded from Postgres at start)
	private long dayStart;
	private final Map<String, long[]> todayByDevice = new HashMap<>();          // mac -> {up, down}
	private final Map<String, Map<String, Long>> todayCountriesByDevice = new HashMap<>();
	private final Map<String, Long> todayCountries = new HashMap<>();
	private final Map<String, String> deviceNames = new HashMap<>();
	private int watchlistConnectionsToday = 0;
	private long watchlistBytesToday = 0;
	private final Map<String, Long> nightUpload = new HashMap<>();             // this (or last) night
	private long nightStart = 0;
	private final Set<String> raisedToday = new HashSet<>();
	private volatile int eventsToday = 0;
	private volatile JSONObject lastEvent = null;

	// history for the rules (reloaded at midnight)
	private Map<String, long[]> weekTotals = new HashMap<>();                  // mac -> 7-day {up, down}
	private Map<String, Set<String>> knownCountries = new HashMap<>();
	private Map<String, Long> firstSeen = new HashMap<>();

	private JSONArray allowlist = new JSONArray();
	private long allowlistModified = -1;
	private boolean failureLogged = false;
	private volatile boolean routerHelperOk = false;
	// what the pulse thread reads: rebuilt at the end of every round, so the pulse never waits on the
	// watcher (which can sit on a router or Postgres timeout)
	private volatile String tokens = "0#0.00#0.00##0##{}#[]#0.00#0.00";
	private volatile Object[] totals = { 0L, 0L, "", 0L };

	TrafficWatcher(OpenWrtUbusClient ubus, TrafficStore store) {
		super("TrafficWatcher");
		setDaemon(true);
		this.ubus = ubus;
		this.store = store;
	}

	@Override
	public void run() {
		logger.info("Traffic watch started, poll " + pollMillis / 1000 + " s, flush " + flushMillis / 60000 + " min, watchlist " + watchlist);
		while (!store.ensureTables()) {
			if (!sleepQuietly(60_000)) return;
		}
		synchronized (this) {
			startDay(System.currentTimeMillis());
		}
		while (!isInterrupted()) {
			long now = System.currentTimeMillis();
			synchronized (this) {
				pollOnce(now);
			}
			if (!sleepQuietly(pollMillis)) break;
		}
		synchronized (this) {
			flush(System.currentTimeMillis());
		}
	}

	/** One round, under the lock: the pulse thread reads today's figures through sensorTokens(). */
	private void pollOnce(long now) {
			try {
				if (now >= dayStart + DAY) {
					flush(now);
					store.deleteOld(flowDays, dnsDays);
					startDay(now);
				}
				refreshDevices();
				refreshAllowlist();
				pollDns(now);
				pollConntrack(now);
				routerHelperOk = true;
				if (failureLogged) logger.info("Traffic watch reaches the router again");
				failureLogged = false;
			} catch (IOException | JSONException e) {
				routerHelperOk = false;
				if (!failureLogged) logger.warn("Traffic poll failed, will keep trying (is " + HELPER + " installed?): " + e.getMessage());
				failureLogged = true;
				lastCounters = null;  // the next good poll is a new baseline
			} catch (RuntimeException e) {
				logger.warn("Traffic watch: " + e, e);
			}
			if (now - lastFlushMillis >= flushMillis) {
				flush(now);
				checkDailyRules(now);
			}
			tokens = buildTokens();
			totals = buildTotals();
	}

	private boolean sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			return false;
		}
	}

	// ------------------------------------------------------------------ router

	private String runHelper(String mode) throws IOException {
		JSONObject r = ubus.call("file", "exec", new JSONObject().put("command", HELPER).put("params", new JSONArray().put(mode)));
		if (r.optInt("code", 0) != 0) throw new IOException(HELPER + " " + mode + " exited " + r.optInt("code") + ": " + r.optString("stderr"));
		return r.optString("stdout", "");
	}

	private void pollDns(long now) throws IOException {
		String out = runHelper("dns");
		long hour = now - now % HOUR;
		for (String line : out.split("\n")) {
			String[] f = line.trim().split(" ");
			if (f.length < 5) continue;
			String serial = f[0], client = f[1], type = f[2], name = f[3].toLowerCase(), value = f[4];
			if (type.equals("query")) {
				nameBySerial.putIfAbsent(serial, name);
				if (name.endsWith(".lan") || !name.contains(".") || name.endsWith(".arpa")) continue;
				String[] dev = device(client);
				String mac = dev != null ? dev[0] : client.startsWith(lanPrefix) ? "ip:" + client : TrafficStore.ROUTER;
				String who = dev != null ? dev[1] : client.startsWith(lanPrefix) ? client : "Router";
				String key = hour + "|" + mac + "|" + name;
				Object[] c = pendingDns.get(key);
				if (c == null) pendingDns.put(key, new Object[] { who, 1 });
				else c[1] = (Integer) c[1] + 1;
			} else if (value.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {  // reply / cached with an IPv4 answer
				String asked = nameBySerial.getOrDefault(serial, name);
				nameByClientAndIp.put(client + "|" + value, asked);
				nameByIp.put(value, asked);
			}
		}
	}

	private void pollConntrack(long now) throws IOException {
		String out = runHelper("conntrack");
		Map<String, long[]> counters = new HashMap<>();
		boolean baseline = lastCounters == null;
		long hour = now - now % HOUR;
		for (String line : out.split("\n")) {
			String[] f = line.trim().split(" ");
			if (f.length < 7) continue;
			String proto = f[0], src = f[1], dst = f[3];
			long up, down;
			int dport;
			try {
				dport = Integer.parseInt(f[4]);
				up = Long.parseLong(f[5]);
				down = Long.parseLong(f[6]);
			} catch (NumberFormatException e) {
				continue;
			}
			String key = proto + " " + src + " " + f[2] + " " + dst + " " + f[4];
			counters.put(key, new long[] { up, down });
			if (baseline) continue;
			long[] before = lastCounters.get(key);
			boolean isNew = before == null || up < before[0] || down < before[1];
			long dUp = isNew ? up : up - before[0], dDown = isNew ? down : down - before[1];
			if (!isNew && dUp == 0 && dDown == 0) continue;
			account(now, hour, src, dst, dport, proto, dUp, dDown, isNew);
		}
		lastCounters = counters;
	}

	// ------------------------------------------------------------------ accounting

	private void account(long now, long hour, String src, String dst, int dport, String proto, long dUp, long dDown, boolean isNew) {
		String[] dev = device(src);
		String mac, name;
		boolean whitelisted;
		if (dev != null) {
			mac = dev[0];
			name = dev[1];
			whitelisted = "true".equals(dev[2]);
		} else if (src.startsWith(lanPrefix)) {
			mac = "ip:" + src;
			name = src;
			whitelisted = false;
		} else {
			mac = TrafficStore.ROUTER;  // the router's own connections, from its WAN address
			name = "Router";
			whitelisted = true;
		}
		String country = store.country(dst);
		String domain = nameByClientAndIp.getOrDefault(src + "|" + dst, nameByIp.getOrDefault(dst, ""));

		String k = hour + "|" + mac + "|" + dst + "|" + dport + "|" + proto;
		TrafficStore.Flow flow = pendingFlows.get(k);
		if (flow == null) {
			flow = new TrafficStore.Flow(hour, mac, dst, dport, proto);
			pendingFlows.put(k, flow);
		}
		flow.device = name;
		flow.country = country;
		if (!domain.isEmpty()) flow.domain = domain;
		flow.upBytes += dUp;
		flow.downBytes += dDown;
		if (isNew) flow.connections++;

		deviceNames.put(mac, name);
		long[] t = todayByDevice.computeIfAbsent(mac, x -> new long[2]);
		t[0] += dUp;
		t[1] += dDown;
		if (!country.isEmpty()) {
			todayCountries.merge(country, dUp + dDown, Long::sum);
			todayCountriesByDevice.computeIfAbsent(mac, x -> new HashMap<>()).merge(country, dUp + dDown, Long::sum);
		}
		firstSeen.putIfAbsent(mac, hour);

		String where = (domain.isEmpty() ? "" : domain + " ") + dst + ":" + dport + (country.isEmpty() ? "" : " " + country);
		if (watchlist.contains(country)) {
			if (isNew) watchlistConnectionsToday++;
			watchlistBytesToday += dUp + dDown;
			raise(now, "Watchlist Country", mac, name, country, dst, domain, country, name + " → " + where);
		}
		if (oddPorts.contains(dport)) {
			raise(now, "Odd Port", mac, name, country, dst, domain, String.valueOf(dport), name + " → port " + dport + " " + where);
		}
		if (!whitelisted && !TrafficStore.ROUTER.equals(mac)) {
			if (alertUnknownDevices) raise(now, "Unknown Device", mac, name, country, dst, domain, "", "Unknown device " + name + " is talking to the internet (" + where + ")");
		}
		if (!country.isEmpty() && learned(mac, now)) {
			Set<String> known = knownCountries.computeIfAbsent(mac, x -> new HashSet<>());
			if (known.add(country)) {
				raise(now, "New Country", mac, name, country, dst, domain, country, name + " talked to " + country + " for the first time (" + where + ")");
			}
		}
		if (inNight(now) && dUp > 0) {
			if (now - nightStart > 12 * HOUR) {  // a new night
				nightUpload.clear();
				nightStart = now;
			}
			long total = nightUpload.merge(mac, dUp, Long::sum);
			if (total > nightUploadMB * MB) {
				raise(now, "Night Upload", mac, name, country, dst, domain, "",
						name + " uploaded " + mb(total) + " MB tonight (latest " + where + ")");
			}
		}
	}

	/** The New Country rule needs some history first, or everything is new on day one. */
	private boolean learned(String mac, long now) {
		Long first = firstSeen.get(mac);
		return first != null && now - first >= learningDays * DAY;
	}

	private void checkDailyRules(long now) {
		double minimum = baselineMinimumMB * MB;
		for (Map.Entry<String, long[]> e : todayByDevice.entrySet()) {
			String mac = e.getKey();
			long up = e.getValue()[0], down = e.getValue()[1];
			if (up < minimum || !learned(mac, now)) continue;
			String name = deviceNames.getOrDefault(mac, mac);
			long[] week = weekTotals.get(mac);
			double averageUp = week == null ? 0 : week[0] / 7.0;
			if (up > baselineFactor * Math.max(averageUp, 1)) {
				raise(now, "Baseline Jump", mac, name, "", "", "", "",
						name + " uploaded " + mb(up) + " MB today, normally " + mb((long) averageUp) + " MB a day");
			}
			if (up > 3 * down && week != null && week[1] > week[0]) {
				raise(now, "Exfil Ratio", mac, name, "", "", "", "",
						name + " uploaded " + mb(up) + " MB but downloaded only " + mb(down) + " MB today");
			}
		}
	}

	private void raise(long now, String rule, String mac, String name, String country, String ip, String domain, String detailKey, String detail) {
		String key = rule + "|" + mac + "|" + detailKey;
		if (raisedToday.contains(key)) return;
		raisedToday.add(key);
		if (allowed(mac, rule, country, domain)) return;
		JSONObject ev = new JSONObject().put("Time", now).put("Rule", rule).put("Mac", mac).put("Device", name)
				.put("Country", country).put("Remote IP", ip).put("Domain", domain).put("Detail", detail);
		store.storeEvent(ev);
		eventsToday++;
		lastEvent = ev;
		logger.warn("Suspicious traffic: " + rule + ": " + detail);
	}

	private boolean allowed(String mac, String rule, String country, String domain) {
		for (int i = 0; i < allowlist.length(); i++) {
			JSONObject a = allowlist.optJSONObject(i);
			if (a == null) continue;
			if (!matches(a.optString("Mac"), mac) || !matches(a.optString("Rule"), rule) || !matches(a.optString("Country"), country)) continue;
			String d = a.optString("Domain").toLowerCase();
			if (d.isEmpty() || d.equals("*") || domain.equals(d) || domain.endsWith("." + d)) return true;
		}
		return false;
	}

	private static boolean matches(String pattern, String value) {
		return pattern == null || pattern.isEmpty() || pattern.equals("*") || pattern.equalsIgnoreCase(value);
	}

	// ------------------------------------------------------------------ days and flushing

	private void startDay(long now) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(now);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		dayStart = c.getTimeInMillis();
		todayByDevice.clear();
		todayCountriesByDevice.clear();
		todayCountries.clear();
		raisedToday.clear();
		watchlistConnectionsToday = 0;
		watchlistBytesToday = 0;
		eventsToday = 0;
		lastEvent = null;
		// what's already in Postgres for today (after a restart)
		for (Map.Entry<String, long[]> e : store.totalsByDevice(dayStart, dayStart + DAY).entrySet()) todayByDevice.put(e.getKey(), e.getValue());
		JSONArray devices = store.devices(dayStart);
		for (int i = 0; i < devices.length(); i++) {
			JSONObject d = devices.getJSONObject(i);
			String mac = d.getString("Mac");
			deviceNames.put(mac, d.optString("Device", mac));
			JSONObject cs = d.getJSONObject("Countries");
			for (String cc : cs.keySet()) {
				todayCountries.merge(cc, cs.getLong(cc), Long::sum);
				todayCountriesByDevice.computeIfAbsent(mac, x -> new HashMap<>()).put(cc, cs.getLong(cc));
				if (watchlist.contains(cc)) watchlistBytesToday += cs.getLong(cc);
			}
		}
		JSONArray events = store.events(dayStart, 1000);
		for (int i = 0; i < events.length(); i++) {
			JSONObject ev = events.getJSONObject(i);
			if (i == 0) lastEvent = ev;
			eventsToday++;
		}
		// raised-today keys aren't stored; an event raised before a restart may be raised once more
		weekTotals = store.totalsByDevice(dayStart - 7 * DAY, dayStart);
		knownCountries = store.countriesByDevice(dayStart + DAY);
		firstSeen = store.firstSeen();
		logger.info("Traffic day " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(dayStart)) + ": " + todayByDevice.size()
				+ " devices so far, " + eventsToday + " events, history for " + firstSeen.size() + " devices");
	}

	private void flush(long now) {
		lastFlushMillis = now;
		if (!pendingFlows.isEmpty()) {
			List<TrafficStore.Flow> rows = new ArrayList<>(pendingFlows.values());
			if (store.storeFlows(rows)) pendingFlows.clear();
			else if (pendingFlows.size() > 50_000) pendingFlows.clear();  // Postgres is down for long: don't fill the heap
		}
		if (!pendingDns.isEmpty()) {
			if (store.storeDns(pendingDns)) pendingDns.clear();
			else if (pendingDns.size() > 50_000) pendingDns.clear();
		}
	}

	// ------------------------------------------------------------------ devices and allowlist

	private void refreshDevices() {
		File f = new File(CleotildeMicroController.NETWORK_SENSOR_FILE);
		if (!f.isFile() || f.lastModified() == devicesFileModified) return;
		try {
			String[] t = FileUtils.readFileToString(f, Charset.defaultCharset()).split("#", -1);
			if (t.length < 3) return;
			JSONArray list = new JSONArray(t[2]);
			Map<String, String[]> byIp = new HashMap<>();
			for (int i = 0; i < list.length(); i++) {
				JSONObject d = list.getJSONObject(i);
				// every device in the list, present or not: its rows carry its current name (map, tables, Identify)
				String m = d.optString(TeleonomeConstants.MAC_ADDRESS).toLowerCase(), n = d.optString(TeleonomeConstants.DEVICE_NAME, m);
				if (!m.isEmpty() && !n.isEmpty() && !n.equals(namesOnRows.get(m))) {
					store.renameDevice(m, n);
					namesOnRows.put(m, n);
					if (deviceNames.containsKey(m)) deviceNames.put(m, n);
				}
				if (!d.optBoolean(TeleonomeConstants.IS_DEVICE_PRESENT, true)) continue;
				String ip = d.optString(TeleonomeConstants.IP_ADDRESS), mac = d.optString(TeleonomeConstants.MAC_ADDRESS).toLowerCase();
				if (ip.isEmpty() || mac.isEmpty()) continue;
				byIp.put(ip, new String[] { mac, d.optString(TeleonomeConstants.DEVICE_NAME, mac),
						String.valueOf(d.optBoolean(TeleonomeConstants.WHITE_LIST_STATUS)) });
			}
			devicesByIp = byIp;
			devicesFileModified = f.lastModified();
		} catch (IOException | JSONException e) {
			logger.debug("Traffic watch could not read the device list: " + e.getMessage());
		}
	}

	private String[] device(String ip) {
		return devicesByIp.get(ip);
	}

	private void refreshAllowlist() {
		File f = new File(ALLOWLIST_FILE);
		if (!f.isFile()) {
			allowlist = new JSONArray();
			return;
		}
		if (f.lastModified() == allowlistModified) return;
		try {
			allowlist = new JSONArray(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
			allowlistModified = f.lastModified();
			logger.info("Traffic allowlist: " + allowlist.length() + " entries");
		} catch (IOException | JSONException e) {
			logger.warn("Could not read " + ALLOWLIST_FILE + ": " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------ for the controller (pulse thread)

	/** Sensor tokens 29-38, already '#'-joined (no leading '#'), as of the last round. */
	String sensorTokens() {
		return tokens;
	}

	private String buildTokens() {
		long nightTotal = 0;
		String topNight = "";
		long topNightBytes = 0;
		for (Map.Entry<String, Long> e : nightUpload.entrySet()) {
			nightTotal += e.getValue();
			if (e.getValue() > topNightBytes) {
				topNightBytes = e.getValue();
				topNight = deviceNames.getOrDefault(e.getKey(), e.getKey()) + " " + mb(topNightBytes) + " MB";
			}
		}
		JSONObject countries = new JSONObject();
		todayCountries.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(15)
				.forEach(e -> countries.put(e.getKey(), Double.parseDouble(mb(e.getValue()))));
		JSONArray devices = new JSONArray();
		long totalUp = 0, totalDown = 0;
		for (long[] t : todayByDevice.values()) {
			totalUp += t[0];
			totalDown += t[1];
		}
		todayByDevice.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue()[0] + b.getValue()[1], a.getValue()[0] + a.getValue()[1]))
				.limit(20).forEach(e -> {
					JSONObject cs = new JSONObject();
					Map<String, Long> byCountry = todayCountriesByDevice.getOrDefault(e.getKey(), new HashMap<>());
					byCountry.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(5)
							.forEach(c -> cs.put(c.getKey(), Double.parseDouble(mb(c.getValue()))));
					devices.put(new JSONObject().put("Device", deviceNames.getOrDefault(e.getKey(), e.getKey())).put("Mac", e.getKey())
							.put("Up MB", Double.parseDouble(mb(e.getValue()[0]))).put("Down MB", Double.parseDouble(mb(e.getValue()[1])))
							.put("Countries", cs));
				});
		JSONObject ev = lastEvent;
		String last = ev == null ? "" : new SimpleDateFormat("HH:mm").format(new Date(ev.getLong("Time"))) + " " + ev.optString("Detail");
		return clean(String.valueOf(watchlistConnectionsToday)) + "#" + mb(watchlistBytesToday) + "#" + mb(nightTotal)
				+ "#" + clean(topNight) + "#" + eventsToday + "#" + clean(last) + "#" + clean(countries.toString())
				+ "#" + clean(devices.toString()) + "#" + mb(totalUp) + "#" + mb(totalDown);
	}

	int getEventsToday() {
		return eventsToday;
	}

	JSONObject getLastEvent() {
		return lastEvent;
	}

	boolean isRouterHelperOk() {
		return routerHelperOk;
	}

	/** {up, down} bytes today, the top device's name and its bytes, as of the last round. */
	Object[] todayTotals() {
		return totals;
	}

	private Object[] buildTotals() {
		long up = 0, down = 0, top = -1;
		String topName = "";
		for (Map.Entry<String, long[]> e : todayByDevice.entrySet()) {
			up += e.getValue()[0];
			down += e.getValue()[1];
			if (e.getValue()[0] + e.getValue()[1] > top) {
				top = e.getValue()[0] + e.getValue()[1];
				topName = deviceNames.getOrDefault(e.getKey(), e.getKey());
			}
		}
		return new Object[] { up, down, topName, Math.max(top, 0) };
	}

	// ------------------------------------------------------------------ helpers

	private boolean inNight(long now) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(now);
		int h = c.get(Calendar.HOUR_OF_DAY);
		return nightStartHour <= nightEndHour ? h >= nightStartHour && h < nightEndHour : h >= nightStartHour || h < nightEndHour;
	}

	static String mb(long bytes) {
		return String.format(java.util.Locale.ROOT, "%.2f", bytes / MB);
	}

	/** Sensor values are '#'-separated. */
	private static String clean(String s) {
		return s == null ? "" : s.replace('#', ' ').replace('\n', ' ');
	}

	private static <K, V> Map<K, V> lru(int max) {
		return new LinkedHashMap<K, V>(256, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> e) {
				return size() > max;
			}
		};
	}
}
