package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.utils.Utils;

// com.teleonome.framework.microcontroller.cleotildemicrocontroller.CleotildeMicroController
/**
 * Cleotilde: watches the OpenWrt router (ubus over Ethernet) and drives the Cleotilde board's LEDs
 * and OLED (USB serial). Design: OpenWrtOne/documents/05-teleonome-design.md.
 *
 * GetSensorData returns one '#' line. Positions are fixed - append only, never renumber:
 *   0-8   the NetworkInspector values, unchanged, so the existing Network Status / DeviceList
 *         pages and the whitelist work: NumberOfDevices, NumberOfUnknowDevices, DeviceList,
 *         DeviceListChanges, DownloadSpeed, UploadSpeed, PingTime, SampleTimeMillis, SampleTimeString
 *   9  Internet Up            10 Internet Latency ms   11 Internet Packet Loss %
 *   12 WAN Up                 13 WAN Uptime s          14 WAN IP
 *   15 WAN Download Mbit/s    16 WAN Upload Mbit/s     (live, from the WAN byte counters)
 *   17 Router Uptime s        18 Router Load           19 Router Memory Free %
 *   20 Router CPU Temperature 21 Wifi Clients 2.4 GHz  22 Wifi Clients 5 GHz
 *   23 Router Reachable Via (ethernet / none)
 *   24 Board Reachable        25 Board Input Voltage   26 Board Input Current mA   27 Board Temperature
 *   28 Cleotilde Status       one-line summary for the web app banner (Well Single Value panel)
 *
 * Every GetSensorData also refreshes the board's LEDs and OLED pages from the same readings.
 *
 * Commands (actuator actions): "Generate Analysis", the three NetworkInspector whitelist commands,
 * and PowerCycle#modem / PowerCycle#router, which are recognised but inactive until the
 * Cleotilde Build 1 relays exist.
 *
 * Config parameters (Microcontroller Config Parameter denes, deneword name = dene name), all optional:
 *   Router Address (192.168.1.1), Router User (cleotilde),
 *   Router Password File (/home/pi/Teleonome/CleotildeRouter.secret),
 *   Board Serial Port (/dev/ttyUSB0), Serial Data Rate (115200), Ping Target (1.1.1.1),
 *   Speed Test Interval Hours (6)
 */
public class CleotildeMicroController extends MicroController {

	public static final String NETWORK_SENSOR_FILE = "NetworkSensor.json";
	private static final String DEFAULT_NETWORK_SENSOR = "-1#-1#[]#{}#0#0#0#";

	Logger logger;
	private String routerAddress = "192.168.1.1";
	private String routerUser = "cleotilde";
	private String routerPasswordFile = "/home/pi/Teleonome/CleotildeRouter.secret";
	private String boardSerialPort = "/dev/ttyUSB0";
	private int serialDataRate = 115200;
	private String pingTarget = "1.1.1.1";
	private int speedTestIntervalHours = 6;

	private OpenWrtUbusClient ubus;
	private CleotildeBoard board;
	private MnemosyneManager aMnemosyneManager;
	private CleotildeReader reader;
	private CleotildeWriter writer;

	// state carried between pulses
	private long lastWanRxBytes = -1, lastWanTxBytes = -1, lastWanSampleMillis = 0;
	private long lastSpeedTestMillis = 0;
	private double lastDownloadSpeed = 0, lastUploadSpeed = 0, lastSpeedTestPing = 0;
	private volatile boolean generatingAnalysis = false;
	private final DecimalFormat twoDecimals = new DecimalFormat("0.00");

	public CleotildeMicroController(Hypothalamus h, DenomeManager d, String n) {
		super(h, d, n);
		logger = Logger.getLogger(getClass());
		aMnemosyneManager = aDenomeManager.getMnemosyneManager();
	}

	@Override
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		for (int i = 0; i < configParams.length(); i++) {
			try {
				JSONObject configDene = configParams.getJSONObject(i);
				String name = configDene.getString("Name");
				Object value = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(configDene, name, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (value == null) continue;
				switch (name) {
					case "Router Address": routerAddress = value.toString(); break;
					case "Router User": routerUser = value.toString(); break;
					case "Router Password File": routerPasswordFile = value.toString(); break;
					case "Board Serial Port": boardSerialPort = value.toString(); break;
					case "Serial Data Rate": serialDataRate = Integer.parseInt(value.toString()); break;
					case "Ping Target": pingTarget = value.toString(); break;
					case "Speed Test Interval Hours": speedTestIntervalHours = Integer.parseInt(value.toString()); break;
					default: break;
				}
			} catch (JSONException | NumberFormatException e) {
				logger.warn(Utils.getStringException(e));
			}
		}
		String password = "";
		try {
			password = FileUtils.readFileToString(new File(routerPasswordFile), Charset.defaultCharset()).trim();
		} catch (IOException e) {
			logger.warn("Could not read router password from " + routerPasswordFile + ", router readings will fail");
		}
		ubus = new OpenWrtUbusClient(routerAddress, routerUser, password);
		board = new CleotildeBoard(boardSerialPort, serialDataRate);
		board.open();  // not fatal: the board is optional, reopened on demand
		reader = new CleotildeReader(new StringReader(""), this);
		writer = new CleotildeWriter(new StringWriter(), reader, this);
		logger.info("Cleotilde initialised, router=" + routerAddress + " board=" + boardSerialPort);
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		reader = new CleotildeReader(new StringReader(""), this);
		writer = new CleotildeWriter(new StringWriter(), reader, this);
		return writer;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return reader;
	}

	@Override
	public void reconnect() throws IOException {
		board.open();
	}

	// ------------------------------------------------------------------ sensor line

	String buildSensorLine() {
		long now = System.currentTimeMillis();
		String networkSensor = readNetworkSensorFile(now);

		Map<String, Object> r = readRouter(now);
		double[] ping = pingInternet();  // up(1/0), latency ms, loss %
		String[] b = board.getSensorData();

		StringBuilder line = new StringBuilder(networkSensor);
		line.append('#').append(ping[0] > 0)
			.append('#').append(twoDecimals.format(ping[1]))
			.append('#').append(twoDecimals.format(ping[2]))
			.append('#').append(r.get("wanUp"))
			.append('#').append(r.get("wanUptime"))
			.append('#').append(r.get("wanIp"))
			.append('#').append(twoDecimals.format((Double) r.get("wanDownMbps")))
			.append('#').append(twoDecimals.format((Double) r.get("wanUpMbps")))
			.append('#').append(r.get("routerUptime"))
			.append('#').append(twoDecimals.format((Double) r.get("load")))
			.append('#').append(r.get("memFreePercent"))
			.append('#').append(twoDecimals.format((Double) r.get("cpuTemp")))
			.append('#').append(r.get("clients24"))
			.append('#').append(r.get("clients5"))
			.append('#').append(r.get("reachableVia"))
			.append('#').append(b != null)
			.append('#').append(b != null && b.length > 3 ? b[3] : "0")
			.append('#').append(b != null && b.length > 4 ? b[4] : "0")
			.append('#').append(b != null && b.length > 6 ? b[6] : "0")
			.append('#').append(statusSummary(networkSensor, r, ping));

		updateFace(networkSensor, r, ping);
		return line.toString();
	}

	/** "Internet UP 14 ms · Router up 2d 3h, 55 C · Wi-Fi 12+2 · 31 devices, 9 unknown" */
	private String statusSummary(String networkSensor, Map<String, Object> r, double[] ping) {
		StringBuilder s = new StringBuilder();
		if (ping[0] > 0) {
			s.append("Internet UP ").append(Math.round(ping[1])).append(" ms");
			if (ping[2] > 0) s.append(", ").append(Math.round(ping[2])).append("% loss");
		} else {
			s.append("Internet DOWN");
		}
		if ("none".equals(r.get("reachableVia"))) {
			s.append(" \u00B7 Router UNREACHABLE");
		} else {
			long up = (Long) r.get("routerUptime");
			s.append(" \u00B7 Router up ").append(up / 86400).append("d ").append((up / 3600) % 24).append("h, ")
				.append(Math.round((Double) r.get("cpuTemp"))).append(" C");
			s.append(" \u00B7 Wi-Fi ").append(r.get("clients24")).append('+').append(r.get("clients5"));
		}
		String[] ns = networkSensor.split("#", -1);
		if (!ns[0].equals("-1")) s.append(" \u00B7 ").append(ns[0]).append(" devices, ").append(ns[1]).append(" unknown");
		return s.toString();  // never contains '#', the token separator
	}

	private String readNetworkSensorFile(long now) {
		String sampleTimeString = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Timestamp(now));
		String fallback = DEFAULT_NETWORK_SENSOR + now + "#" + sampleTimeString;
		File file = new File(NETWORK_SENSOR_FILE);
		if (!file.isFile()) return fallback;
		try {
			String s = FileUtils.readFileToString(file, Charset.defaultCharset()).trim();
			return s.split("#", -1).length == 9 ? s : fallback;
		} catch (IOException e) {
			logger.warn(Utils.getStringException(e));
			return fallback;
		}
	}

	/** Router readings; on failure the values are zero and reachableVia is "none". */
	private Map<String, Object> readRouter(long now) {
		Map<String, Object> r = new HashMap<>();
		r.put("wanUp", false); r.put("wanUptime", 0L); r.put("wanIp", "");
		r.put("wanDownMbps", 0.0); r.put("wanUpMbps", 0.0);
		r.put("routerUptime", 0L); r.put("load", 0.0); r.put("memFreePercent", 0);
		r.put("cpuTemp", 0.0); r.put("clients24", 0); r.put("clients5", 0);
		r.put("radiosUp", 0); r.put("radios", 0);
		r.put("reachableVia", "none");
		try {
			JSONObject info = ubus.call("system", "info");
			r.put("routerUptime", info.getLong("uptime"));
			r.put("load", info.getJSONArray("load").getLong(0) / 65536.0);
			JSONObject mem = info.getJSONObject("memory");
			r.put("memFreePercent", (int) Math.round(100.0 * mem.getLong("available") / mem.getLong("total")));
			r.put("reachableVia", "ethernet");

			JSONObject wan = ubus.call("network.interface.wan", "status");
			r.put("wanUp", wan.optBoolean("up"));
			r.put("wanUptime", wan.optLong("uptime"));
			JSONArray v4 = wan.optJSONArray("ipv4-address");
			if (v4 != null && v4.length() > 0) r.put("wanIp", v4.getJSONObject(0).optString("address"));
			String wanDevice = wan.optString("l3_device", wan.optString("device", ""));
			if (!wanDevice.isEmpty()) {
				JSONObject stats = ubus.call("network.device", "status", new JSONObject().put("name", wanDevice)).getJSONObject("statistics");
				long rx = stats.getLong("rx_bytes"), tx = stats.getLong("tx_bytes");
				if (lastWanRxBytes >= 0 && now > lastWanSampleMillis && rx >= lastWanRxBytes && tx >= lastWanTxBytes) {
					double seconds = (now - lastWanSampleMillis) / 1000.0;
					r.put("wanDownMbps", (rx - lastWanRxBytes) * 8 / seconds / 1e6);
					r.put("wanUpMbps", (tx - lastWanTxBytes) * 8 / seconds / 1e6);
				}
				lastWanRxBytes = rx; lastWanTxBytes = tx; lastWanSampleMillis = now;
			}

			JSONObject temp = ubus.call("file", "read", new JSONObject().put("path", "/sys/class/thermal/thermal_zone0/temp"));
			r.put("cpuTemp", Double.parseDouble(temp.getString("data").trim()) / 1000.0);

			JSONArray devices = ubus.call("iwinfo", "devices").getJSONArray("devices");
			int clients24 = 0, clients5 = 0, radiosUp = 0;
			for (int i = 0; i < devices.length(); i++) {
				String dev = devices.getString(i);
				JSONObject radio = ubus.call("iwinfo", "info", new JSONObject().put("device", dev));
				int clients = ubus.call("iwinfo", "assoclist", new JSONObject().put("device", dev)).getJSONArray("results").length();
				if (radio.optInt("frequency") > 0) radiosUp++;
				if (radio.optInt("frequency") < 3000) clients24 += clients;
				else clients5 += clients;
			}
			r.put("clients24", clients24); r.put("clients5", clients5);
			r.put("radiosUp", radiosUp); r.put("radios", devices.length());
		} catch (IOException | JSONException | NumberFormatException e) {
			logger.warn("Reading the router failed: " + e.getMessage());
		}
		return r;
	}

	/** Ping from the Pi through the router: {up 1/0, average latency ms, packet loss %}. */
	private double[] pingInternet() {
		double[] result = { 0, 0, 100 };
		try {
			ArrayList lines = Utils.executeCommand("ping -c 3 -W 2 " + pingTarget);
			for (Object o : lines) {
				String l = o.toString();
				if (l.contains("packet loss")) {
					String loss = l.replaceAll(".*?([0-9.]+)% packet loss.*", "$1");
					result[2] = Double.parseDouble(loss);
				} else if (l.contains("min/avg/max")) {
					String[] values = l.substring(l.indexOf('=') + 1).trim().split("/");
					result[1] = Double.parseDouble(values[1]);
				}
			}
			result[0] = result[2] < 100 ? 1 : 0;
		} catch (IOException | InterruptedException | NumberFormatException e) {
			logger.warn("ping failed: " + e.getMessage());
		}
		return result;
	}

	// ------------------------------------------------------------------ the face (LEDs + OLED)

	/**
	 * LED 1 internet, 2 router health, 3 Wi-Fi, 4 devices/security, 5 Teleonome (design section 5).
	 * TODO critical-device list: LED 4 red when a critical device is missing (needs the critical
	 * list table, like the whitelist).
	 */
	void updateFace(String networkSensor, Map<String, Object> r, double[] ping) {
		boolean routerReachable = !"none".equals(r.get("reachableVia"));
		String internet = ping[0] == 0 ? "R" : (ping[1] >= 100 || ping[2] > 0 ? "Y" : "G");

		String router;
		if (!routerReachable) router = "r";
		else if ((Double) r.get("cpuTemp") > 85 || (Integer) r.get("memFreePercent") < 15 || (Long) r.get("routerUptime") < 3600) router = "Y";
		else router = "G";

		int radios = (Integer) r.get("radios"), radiosUp = (Integer) r.get("radiosUp");
		String wifi = !routerReachable ? "O" : radiosUp == 0 ? "R" : radiosUp < radios ? "Y" : "G";

		String[] ns = networkSensor.split("#", -1);
		int unknownDevices = parseIntOr(ns[1], -1);
		String devices = unknownDevices < 0 ? "O" : unknownDevices > 0 ? "Y" : "G";

		board.setLeds(internet, router, wifi, devices, "G");

		long up = (Long) r.get("routerUptime");
		board.oledPage(1, "Internet",
				ping[0] > 0 ? "UP " + Math.round(ping[1]) + " ms, loss " + Math.round(ping[2]) + "%" : "DOWN",
				"WAN " + r.get("wanIp"),
				"Down " + twoDecimals.format((Double) r.get("wanDownMbps")) + " Mbit/s",
				"Up   " + twoDecimals.format((Double) r.get("wanUpMbps")) + " Mbit/s",
				"Speed " + twoDecimals.format(lastDownloadSpeed) + "/" + twoDecimals.format(lastUploadSpeed));
		board.oledPage(2, "Router",
				routerReachable ? "Up " + (up / 86400) + "d " + ((up / 3600) % 24) + "h " + ((up / 60) % 60) + "m" : "UNREACHABLE",
				"CPU " + twoDecimals.format((Double) r.get("cpuTemp")) + " C",
				"Load " + twoDecimals.format((Double) r.get("load")),
				"Memory free " + r.get("memFreePercent") + "%");
		board.oledPage(3, "Devices",
				"Total   " + (ns[0].equals("-1") ? "?" : ns[0]),
				"Unknown " + (unknownDevices < 0 ? "?" : String.valueOf(unknownDevices)),
				"WiFi 2.4 " + r.get("clients24") + "  5G " + r.get("clients5"));
	}

	private static int parseIntOr(String s, int fallback) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	// ------------------------------------------------------------------ analysis (device list)

	void startAnalysis() {
		if (generatingAnalysis) {
			logger.debug("analysis already running");
			return;
		}
		new Thread(this::generateAnalysis, "CleotildeAnalysis").start();
	}

	/**
	 * Same output as NetworkInspector's GenerateAnalysisThread (NetworkSensor.json + Mnemosyne),
	 * but the device list comes from the router instead of arp-scan: who is associated on Wi-Fi,
	 * plus complete entries in the router's ARP table, named from DHCP leases / host hints.
	 */
	private void generateAnalysis() {
		generatingAnalysis = true;
		try {
			JSONArray previous = new JSONArray();
			File file = new File(NETWORK_SENSOR_FILE);
			if (file.isFile()) {
				String[] tokens = FileUtils.readFileToString(file, Charset.defaultCharset()).split("#", -1);
				if (tokens.length > 2) previous = new JSONArray(tokens[2]);
			}

			JSONArray deviceList = routerDeviceList();
			JSONObject changes = diff(previous, deviceList);

			long now = System.currentTimeMillis();
			if (now - lastSpeedTestMillis > speedTestIntervalHours * 3600_000L) runSpeedTest(now);

			String sampleTimeString = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Timestamp(now));
			aMnemosyneManager.storeNetworkStatus(deviceList, changes, now, sampleTimeString);
			JSONArray lastActivity = aMnemosyneManager.getLastNetworkSensorDeviceActivity();
			int numberOfDevices = aMnemosyneManager.geNumberOfDevicesInLastSample();
			int numberUnknown = aMnemosyneManager.geNumberUnknowDevicesInLastSample();
			String data = numberOfDevices + "#" + numberUnknown + "#" + lastActivity + "#" + changes
					+ "#" + twoDecimals.format(lastDownloadSpeed) + "#" + twoDecimals.format(lastUploadSpeed)
					+ "#" + twoDecimals.format(lastSpeedTestPing) + "#" + now + "#" + sampleTimeString;
			FileUtils.writeStringToFile(file, data, Charset.defaultCharset(), false);
			logger.debug("analysis done, devices=" + numberOfDevices + " unknown=" + numberUnknown);
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		} finally {
			generatingAnalysis = false;
		}
	}

	private JSONArray routerDeviceList() throws IOException {
		Map<String, String> names = new HashMap<>();
		Map<String, String> ips = new HashMap<>();
		JSONObject hints = ubus.call("luci-rpc", "getHostHints");
		for (Iterator<String> it = hints.keys(); it.hasNext();) {
			String key = it.next();
			String mac = key.toLowerCase();
			JSONObject h = hints.getJSONObject(key);
			if (h.has("name")) names.put(mac, h.getString("name"));
			JSONArray v4 = h.optJSONArray("ipaddrs");
			if (v4 != null && v4.length() > 0) ips.put(mac, v4.getString(0));
		}
		JSONArray leases = ubus.call("luci-rpc", "getDHCPLeases").optJSONArray("dhcp_leases");
		if (leases != null) {
			for (int i = 0; i < leases.length(); i++) {
				JSONObject l = leases.getJSONObject(i);
				String mac = l.optString("macaddr").toLowerCase();
				if (!l.optString("hostname").isEmpty()) names.putIfAbsent(mac, l.getString("hostname"));
				if (!l.optString("ipaddr").isEmpty()) ips.put(mac, l.getString("ipaddr"));
			}
		}

		Map<String, String> present = new HashMap<>();  // mac -> connection
		JSONArray radios = ubus.call("iwinfo", "devices").getJSONArray("devices");
		for (int i = 0; i < radios.length(); i++) {
			String dev = radios.getString(i);
			int frequency = ubus.call("iwinfo", "info", new JSONObject().put("device", dev)).optInt("frequency");
			JSONArray assoc = ubus.call("iwinfo", "assoclist", new JSONObject().put("device", dev)).getJSONArray("results");
			for (int j = 0; j < assoc.length(); j++) {
				present.put(assoc.getJSONObject(j).getString("mac").toLowerCase(), frequency < 3000 ? "wifi 2.4" : "wifi 5");
			}
		}
		// /proc/net/arp: IP, HW type, Flags, HW address, Mask, Device. Flags 0x2 = complete.
		String arp = ubus.call("file", "read", new JSONObject().put("path", "/proc/net/arp")).getString("data");
		for (String row : arp.split("\n")) {
			String[] f = row.trim().split("\\s+");
			if (f.length < 6 || !f[2].equals("0x2") || !f[5].startsWith("br-lan")) continue;
			String mac = f[3].toLowerCase();
			ips.putIfAbsent(mac, f[0]);
			present.putIfAbsent(mac, "wired");
		}

		JSONArray list = new JSONArray();
		for (Map.Entry<String, String> e : present.entrySet()) {
			String mac = e.getKey();
			JSONObject d = new JSONObject();
			d.put(TeleonomeConstants.MAC_ADDRESS, mac);
			d.put(TeleonomeConstants.IP_ADDRESS, ips.getOrDefault(mac, ""));
			d.put(TeleonomeConstants.DEVICE_NAME, names.getOrDefault(mac, mac));  // the whitelist is keyed by name
			d.put("Connection", e.getValue());
			list.put(d);
		}
		return list;
	}

	private JSONObject diff(JSONArray previous, JSONArray current) {
		JSONArray missing = new JSONArray(), added = new JSONArray();
		java.util.Set<String> before = new java.util.HashSet<>(), now = new java.util.HashSet<>();
		for (int i = 0; i < previous.length(); i++) before.add(previous.getJSONObject(i).optString(TeleonomeConstants.DEVICE_NAME));
		for (int i = 0; i < current.length(); i++) now.add(current.getJSONObject(i).optString(TeleonomeConstants.DEVICE_NAME));
		for (String n : before) if (!now.contains(n)) missing.put(n);
		for (String n : now) if (!before.contains(n)) added.put(n);
		return new JSONObject().put("Missing", missing).put("New", added);
	}

	private void runSpeedTest(long now) {
		try {
			ArrayList results = Utils.executeCommand("speedtest-cli --json");
			if (!results.isEmpty()) {
				JSONObject s = new JSONObject(results.get(0).toString());
				lastDownloadSpeed = s.getDouble("download") / (8 * 1024 * 1024);
				lastUploadSpeed = s.getDouble("upload") / (8 * 1024 * 1024);
				lastSpeedTestPing = s.getDouble("ping");
				lastSpeedTestMillis = now;
			}
		} catch (IOException | InterruptedException | JSONException e) {
			logger.warn("speedtest failed: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------ whitelist (NetworkInspector parity)

	void addDeviceToWhiteList(String deviceName, String mac) {
		logger.debug("AddToWhiteList " + deviceName + " " + mac + " " + aMnemosyneManager.addDeviceToWhiteList(deviceName, mac));
		updateWhiteListStatusInFile(deviceName, true);
	}

	void removeDeviceFromWhiteList(String deviceName) {
		logger.debug("RemoveDeviceFromWhiteList " + deviceName + " " + aMnemosyneManager.removeDeviceFromWhiteList(deviceName));
		updateWhiteListStatusInFile(deviceName, false);
	}

	void addAllDevicesToWhiteList() {
		try {
			String[] tokens = FileUtils.readFileToString(new File(NETWORK_SENSOR_FILE), Charset.defaultCharset()).split("#", -1);
			JSONArray devices = new JSONArray(tokens[2]);
			for (int i = 0; i < devices.length(); i++) {
				JSONObject d = devices.getJSONObject(i);
				if (!d.optBoolean(TeleonomeConstants.WHITE_LIST_STATUS)) {
					addDeviceToWhiteList(d.getString(TeleonomeConstants.DEVICE_NAME), d.getString(TeleonomeConstants.MAC_ADDRESS));
				}
			}
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	private void updateWhiteListStatusInFile(String deviceName, boolean status) {
		try {
			File file = new File(NETWORK_SENSOR_FILE);
			String[] t = FileUtils.readFileToString(file, Charset.defaultCharset()).split("#", -1);
			if (t.length != 9) return;
			JSONArray devices = new JSONArray(t[2]);
			for (int i = 0; i < devices.length(); i++) {
				if (deviceName.equals(devices.getJSONObject(i).optString(TeleonomeConstants.DEVICE_NAME))) {
					devices.getJSONObject(i).put(TeleonomeConstants.WHITE_LIST_STATUS, status);
					break;
				}
			}
			t[2] = devices.toString();
			FileUtils.writeStringToFile(file, String.join("#", t), Charset.defaultCharset(), false);
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	// ------------------------------------------------------------------ power control (inactive)

	/**
	 * PowerCycle#modem / PowerCycle#router. Built now so the denome actions exist; inactive until
	 * Cleotilde Build 1 has relays. The board answers "Failure-NoRelayHardware" today.
	 */
	String powerCycle(String target) {
		int channel = "modem".equalsIgnoreCase(target) ? 1 : "router".equalsIgnoreCase(target) ? 2 : 0;
		if (channel == 0) return "Failure-Unknown power target " + target;
		String answer = board.command("Relay#" + channel + "#CYCLE#10");
		logger.info("PowerCycle " + target + " -> " + answer);
		return answer == null ? "Failure-No answer from board" : answer;
	}
}
