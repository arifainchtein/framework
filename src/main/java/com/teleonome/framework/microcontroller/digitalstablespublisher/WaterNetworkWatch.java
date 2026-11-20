package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.utils.Utils;

/**
 * The water networks' checks without a browser (2026-11-20), so an alert reaches the farmer's phone (devices.digitalstables
 * .com sends it by web push and email) and Annabelle's own page (the Emergency Channel banner). The same checks the page
 * shows (deviceviews-waternetwork.js), on the newest readings in the Denome's Telepathons nucleus, at every Publish To
 * DigitalStables:
 * <ul>
 * <li><b>leak between meters</b>: an outlet's meter shows more than the meters its pipes end at, by more than the network's
 * Balance Tolerance (only when each of those meters is fed by that pipe alone, and all were read within 3 minutes);</li>
 * <li><b>unexplained flow</b>: a meter with defined sinks shows a flow none of them explains (a new use, or a leak after it);</li>
 * <li><b>overflowing</b>: a tank's level stands above its Overflow Height.</li>
 * </ul>
 * A problem must last the network's Alert After Seconds before it is an alert; when it is gone the farmer hears "OK again".
 * Which problems were alerted is kept in {@value #STATE_FILE}, so a restart in between still sends "OK again". A leak out
 * of a tank without a level sensor (TopTank) cannot be seen here, as on the page.
 */
public class WaterNetworkWatch {

	public static final String STATE_FILE = "WaterNetworkAlerts.info";
	private static final long APART_SECONDS = 180;
	private static final Logger logger = Logger.getLogger(WaterNetworkWatch.class);

	/** One alert to send: to devices (MQTT) and, when raised, to the Heart's Emergency Channel. */
	public static class Alert {
		public final String key, state, title, message, network;
		Alert(String key, String state, String title, String message, String network) {
			this.key = key; this.state = state; this.title = title; this.message = message; this.network = network;
		}
		public JSONObject toJSON() {
			return new JSONObject().put("key", key).put("state", state).put("title", title).put("message", message).put("network", network)
					.put("at", System.currentTimeMillis());
		}
	}

	private static final Map<String, Long> firstSeen = new HashMap<String, Long>();

	private WaterNetworkWatch() {
	}

	/** Runs the checks; returns the alerts and "OK again"s to send now (and remembers what was alerted). */
	public static synchronized List<Alert> check(DenomeManager dm) {
		List<Alert> out = new ArrayList<Alert>();
		JSONObject state = readState();
		JSONObject alerted = state.optJSONObject("Alerted");
		if (alerted == null) alerted = new JSONObject();
		Set<String> problemsNow = new HashSet<String>();
		long now = System.currentTimeMillis();
		try {
			JSONArray chains = WaterNetworksSync.networks(dm);
			for (int i = 0; i < chains.length(); i++) {
				Network n = new Network(chains.getJSONObject(i), dm);
				for (Map.Entry<String, String[]> p : n.problems().entrySet()) {
					String key = p.getKey();
					problemsNow.add(key);
					if (!firstSeen.containsKey(key)) firstSeen.put(key, now);
					if (!alerted.has(key) && now - firstSeen.get(key) >= n.alertAfterSeconds * 1000L) {
						out.add(new Alert(key, "alert", p.getValue()[0], p.getValue()[1], n.name));
						alerted.put(key, new JSONObject().put("Title", p.getValue()[0]).put("Network", n.name).put("At", now));
					}
				}
			}
		} catch (Exception e) {
			logger.warn("water network checks failed: " + Utils.getStringException(e));
			return out;   // nothing is cleared on a failed check
		}
		for (String key : new HashSet<String>(firstSeen.keySet())) if (!problemsNow.contains(key)) firstSeen.remove(key);
		for (String key : new ArrayList<String>(alerted.keySet())) {
			if (problemsNow.contains(key)) continue;
			JSONObject was = alerted.getJSONObject(key);
			out.add(new Alert(key, "cleared", was.optString("Title"), "Back to normal: " + was.optString("Title") + ".", was.optString("Network")));
			alerted.remove(key);
		}
		// what devices has not acknowledged yet goes again at every publish (an internet outage loses nothing)
		JSONArray outbox = state.optJSONArray("Outbox");
		if (outbox == null) outbox = new JSONArray();
		for (Alert a : out) outbox.put(a.toJSON());
		while (outbox.length() > 50) outbox.remove(0);
		state.put("Outbox", outbox);
		state.put("Alerted", alerted);
		state.put("Checked At", now);
		writeState(state);
		return out;
	}

	/** The alerts devices has not acknowledged yet, oldest first. */
	public static synchronized JSONArray outbox() {
		JSONArray o = readState().optJSONArray("Outbox");
		return o == null ? new JSONArray() : o;
	}

	/** devicesheart's answer {alertKey, outcome}; true when it was about an alert. Removes the oldest of that key. */
	public static synchronized boolean onAck(JSONObject ack) {
		if (!ack.has("alertKey")) return false;
		JSONObject state = readState();
		JSONArray outbox = state.optJSONArray("Outbox");
		for (int i = 0; outbox != null && i < outbox.length(); i++) {
			if (ack.getString("alertKey").equals(outbox.getJSONObject(i).optString("key"))) { outbox.remove(i); break; }
		}
		if (!"stored".equals(ack.optString("outcome"))) logger.warn("DigitalStables refused alert " + ack.optString("alertKey") + ": " + ack.optString("outcome"));
		writeState(state);
		return true;
	}

	// ---- one network, read from its DeneChain (the format deviceviews-waternetwork.js writes) ----
	static class Network {
		final String name, chainName;
		double tolerance = 1.0, minFlow = 0.5;
		int alertAfterSeconds = 90;
		final DenomeManager dm;
		final Map<String, JSONObject> tanks = new HashMap<String, JSONObject>();          // telepathon -> its dene
		final List<String[]> conns = new ArrayList<String[]>();                          // [from key, to key]
		final Map<String, double[]> sinks = new LinkedHashMap<String, double[]>();       // name -> {min, max}
		final Map<String, String> labels = new HashMap<String, String>();                // meter key -> its name (PumpF)

		Network(JSONObject chain, DenomeManager dm) {
			this.dm = dm;
			this.chainName = chain.getString("Name");
			String n = chainName.startsWith(WaterNetworksSync.DEFAULT_PREFIX) ? chainName.substring(WaterNetworksSync.DEFAULT_PREFIX.length()) : chainName;
			JSONArray denes = chain.optJSONArray("Denes");
			for (int i = 0; denes != null && i < denes.length(); i++) {
				JSONObject d = denes.getJSONObject(i);
				String type = d.optString("Dene Type");
				if (type.equals("Water Network Settings")) {
					n = str(d, "Network Name", n);
					tolerance = num(d, "Balance Tolerance", tolerance);
					minFlow = num(d, "Minimum Flow", minFlow);
					alertAfterSeconds = (int) num(d, "Alert After Seconds", alertAfterSeconds);
				} else if (type.equals("Water Source") || type.equals("Water Measuring Point")) {
					if ("Tank".equals(str(d, "Source Kind", ""))) tanks.put(d.getString("Name"), d);
					for (int m = 1; m < 20; m++) {
						String p = str(d, "Meter " + m + " Pointer", null);
						if (p == null) break;
						String k = key(p, false);
						String label = str(d, "Meter " + m + " Name", "");
						if (k != null && !label.isEmpty()) labels.put(k, label);
					}
				} else if (type.equals("Water Defined Sink")) {
					sinks.put(d.getString("Name"), new double[] { num(d, "Minimum Flow", 0), num(d, "Maximum Flow", 0) });
				}
			}
			this.name = n;
			for (int i = 0; denes != null && i < denes.length(); i++) {
				JSONObject d = denes.getJSONObject(i);
				if (!"Water Connection".equals(d.optString("Dene Type"))) continue;
				String a = key(str(d, "From", ""), "Overflow".equals(str(d, "From Port", ""))), b = key(str(d, "To", ""), false);
				if (a != null && b != null) conns.add(new String[] { a, b });
			}
		}

		// "@Annabelle:Telepathons:PipeSplit:Purpose:Flow Rate 1" -> "PipeSplit:Flow Rate 1"; "@Annabelle:Purpose:<chain>:Pool" -> "Pool"
		String key(String pointer, boolean overflow) {
			if (pointer == null || !pointer.startsWith("@")) return null;
			String[] p = pointer.substring(1).split(":");
			if (p.length == 5 && p[1].equals("Telepathons")) return p[2] + ":" + p[4];
			if (p.length == 4 && p[1].equals(TeleonomeConstants.NUCLEI_PURPOSE) && p[2].equals(chainName)) return p[3] + (overflow ? " overflow" : "");
			return null;
		}

		boolean isMeter(String k) { return k.indexOf(':') > 0; }

		String label(String k) {
			if (!isMeter(k)) return k;
			String t = k.substring(0, k.indexOf(':')), w = k.substring(k.indexOf(':') + 1);
			return t + " " + (labels.containsKey(k) ? labels.get(k) : w.replace("Flow Rate", "Flow"));
		}

		// the newest value of a Purpose word of a telepathon, and when the telepathon was read (s); null when none
		JSONObject purpose(String telepathon) {
			try {
				JSONObject chain = dm.getDeneChainByPointer(TeleonomeConstants.NUCLEI_TELEPATHONS, telepathon);
				JSONArray denes = chain == null ? null : chain.optJSONArray("Denes");
				for (int i = 0; denes != null && i < denes.length(); i++) if ("Purpose".equals(denes.getJSONObject(i).optString("Name"))) return denes.getJSONObject(i);
			} catch (Exception e) {
				// not there
			}
			return null;
		}
		Double value(String telepathon, String word) {
			JSONObject p = purpose(telepathon);
			if (p == null) return null;
			Object v = wordValue(p, word);
			if (v == null) return null;
			try {
				double d = Double.parseDouble(v.toString());
				return d == -99 ? null : d;
			} catch (NumberFormatException e) {
				return null;
			}
		}
		Double meter(String k) { return value(k.substring(0, k.indexOf(':')), k.substring(k.indexOf(':') + 1)); }
		Double secondsOf(String k) { String t = isMeter(k) ? k.substring(0, k.indexOf(':')) : k; return value(t, "Seconds Time"); }

		List<String> children(String k) { List<String> l = new ArrayList<String>(); for (String[] c : conns) if (c[0].equals(k)) l.add(c[1]); return l; }
		int parents(String k) { int n = 0; for (String[] c : conns) if (c[1].equals(k)) n++; return n; }

		/** key -> {title, message} of each problem now */
		Map<String, String[]> problems() {
			Map<String, String[]> found = new LinkedHashMap<String, String[]>();
			Set<String> outlets = new HashSet<String>();
			for (String[] c : conns) outlets.add(c[0]);
			for (String o : outlets) {
				if (!isMeter(o)) continue;
				Double out = meter(o);
				if (out == null) continue;
				List<String> kids = children(o);
				// leak between meters
				boolean allMeters = !kids.isEmpty();
				for (String k : kids) if (!isMeter(k) || parents(k) != 1) allMeters = false;
				if (allMeters) {
					double sum = 0, lo = Double.MAX_VALUE, hi = 0;
					boolean ok = true;
					for (String k : kids) { Double v = meter(k); if (v == null) { ok = false; break; } sum += v; }
					for (String k : concat(o, kids)) { Double s = secondsOf(k); if (s == null) { ok = false; break; } lo = Math.min(lo, s); hi = Math.max(hi, s); }
					double diff = out - sum;
					if (ok && hi - lo <= APART_SECONDS && diff > tolerance) {
						String names = join(kids);
						found.put(chainName + "|leak|" + o, new String[] { "Water lost between " + label(o) + " and " + names,
								String.format("%s shows %.1f l/m but %s add up to %.1f l/m: %.1f l/m is leaving the pipe between them (%s).", label(o), out, names, sum, diff, name) });
					}
				}
				// unexplained flow on a meter with defined sinks
				List<double[]> mine = new ArrayList<double[]>();
				List<String> mineNames = new ArrayList<String>();
				for (String k : kids) if (sinks.containsKey(k)) { mine.add(sinks.get(k)); mineNames.add(k); }
				if (!mine.isEmpty() && out >= minFlow) {
					double unexplained = unexplained(out, mine);
					if (unexplained > tolerance) {
						found.put(chainName + "|unexplained|" + o, new String[] { "Unexplained flow on " + label(o),
								String.format("%s shows %.1f l/m and %.1f l/m of it matches none of %s: a new use, or a leak after the meter (%s).", label(o), out, unexplained, String.join(", ", mineNames), name) });
					}
				}
			}
			// tanks overflowing
			for (Map.Entry<String, JSONObject> t : tanks.entrySet()) {
				JSONObject d = t.getValue();
				double over = num(d, "Overflow Height", 0);
				String levelPointer = str(d, "Level Pointer", null);
				if (over <= 0 || levelPointer == null) continue;
				String lk = key(levelPointer, false);
				Double v = lk == null ? null : meter(lk);
				if (v == null) continue;
				String units = str(d, "Level Units", "");
				double depth = units.equals("%") ? v / 100 * num(d, "Height When Full", 0) : units.equals("cm") ? v * 10 : v;
				if (units.equals("%") && num(d, "Height When Full", 0) <= 0) continue;
				if (depth > over) found.put(chainName + "|overflow|" + t.getKey(), new String[] { t.getKey() + " is overflowing",
						String.format("%s's level is %.0f mm, above its overflow at %.0f mm (%s).", t.getKey(), depth, over, name) });
			}
			return found;
		}

		// as the page: one sink whose range holds it, else two whose ranges add up, else the biggest below it
		static double unexplained(double v, List<double[]> s) {
			for (double[] a : s) if (v >= a[0] && v <= a[1]) return 0;
			for (int i = 0; i < s.size(); i++) for (int j = i + 1; j < s.size(); j++)
				if (v >= s.get(i)[0] + s.get(j)[0] && v <= s.get(i)[1] + s.get(j)[1]) return 0;
			double best = -1;
			for (double[] a : s) if (v > a[1] && a[1] > best) best = a[1];
			return best >= 0 ? v - best : v;
		}

		String join(List<String> keys) { List<String> l = new ArrayList<String>(); for (String k : keys) l.add(label(k)); return String.join(" + ", l); }
		static List<String> concat(String a, List<String> b) { List<String> l = new ArrayList<String>(); l.add(a); l.addAll(b); return l; }
	}

	static Object wordValue(JSONObject dene, String name) {
		JSONArray words = dene.optJSONArray("DeneWords");
		for (int i = 0; words != null && i < words.length(); i++) {
			JSONObject w = words.getJSONObject(i);
			if (name.equals(w.optString("Name"))) return w.opt("Value");
		}
		return null;
	}
	static String str(JSONObject dene, String name, String dflt) { Object v = wordValue(dene, name); return v == null ? dflt : v.toString(); }
	static double num(JSONObject dene, String name, double dflt) {
		Object v = wordValue(dene, name);
		if (v == null) return dflt;
		try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return dflt; }
	}

	static JSONObject readState() {
		try {
			File f = new File(Utils.getLocalDirectory() + STATE_FILE);
			if (f.isFile()) return new JSONObject(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
		} catch (Exception e) {
			logger.warn("unreadable " + STATE_FILE + ": " + e.getMessage());
		}
		return new JSONObject();
	}
	static void writeState(JSONObject s) {
		try {
			FileUtils.writeStringToFile(new File(Utils.getLocalDirectory() + STATE_FILE), s.toString(4), StandardCharsets.UTF_8);
		} catch (Exception e) {
			logger.warn("could not write " + STATE_FILE + ": " + e.getMessage());
		}
	}
}
