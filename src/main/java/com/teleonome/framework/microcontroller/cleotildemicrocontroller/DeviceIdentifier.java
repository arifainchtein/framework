package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * What a device on the network probably is (added 2026-11-04), so an unknown device can be named from its card:
 * its maker (from the MAC), the name it announces on the network (mDNS, avahi-resolve; Apple devices, Pis and most
 * Linux machines answer), and the domains it looks up (TrafficStore, from the router's DNS log), which give most
 * devices away within minutes (an iPhone asks for iphone-ld.apple.com, a Tuya plug for tuyaus.com ...).
 * Used by CleotildeMicroController's scan (the cards) and by the web app's IdentifyDevice form (the Identify button),
 * so both say the same thing.
 */
public final class DeviceIdentifier {

	private static final Logger logger = Logger.getLogger(DeviceIdentifier.class);

	private DeviceIdentifier() {
	}

	/** First match wins: {regex over "vendor | names | connection | domains", guess[, why: else the matched text]}. */
	private static final String[][] RULES = {
			{ "iphone-ld\\.apple|\\biphone\\b", "iPhone" },
			{ "\\bipad\\b", "iPad" },
			{ "mac-?mini|macbook|\\bimac\\b|mac-?pro|mac-?studio", "Mac" },
			{ "\\| wired \\|.*time-ios\\.apple|appletv|apple-?tv|\\btv\\.apple\\.com", "Apple TV", "a wired Apple device using the iOS/tvOS time server" },
			{ "airport|time-?capsule", "Apple AirPort base station" },
			{ "apple-?watch", "Apple Watch" },
			{ "homepod", "HomePod" },
			{ "tuya", "Tuya smart device (plug, light or camera)" },
			{ "ewelink|sonoff|itead", "Sonoff / eWeLink smart switch" },
			{ "tplinkcloud|kasa|tapo", "TP-Link Kasa / Tapo smart device" },
			{ "meross", "Meross smart device" },
			{ "shelly", "Shelly smart relay" },
			{ "hikvision|hik-connect|ezviz", "Hikvision / EZVIZ camera" },
			{ "dahua|imou", "Dahua / Imou camera" },
			{ "wyze", "Wyze camera" },
			{ "ring\\.com", "Ring doorbell / camera" },
			{ "arlo", "Arlo camera" },
			{ "nest\\.com|dropcam", "Google Nest device" },
			{ "alexa|amazon-?echo|device-metrics-us|fireoscaptiveportal|amazonvideo|aiv-cdn", "Amazon Echo / Fire TV" },
			{ "googlecast|chromecast|\\bcast\\b", "Chromecast / Google speaker" },
			{ "roku", "Roku" },
			{ "samsungcloud|samsungosp|samsung\\.com|smartthings", "Samsung device (phone or TV)" },
			{ "lgtvsdp|lgsmartad|webos", "LG smart TV" },
			{ "sonos", "Sonos speaker" },
			{ "xboxlive|xbox", "Xbox" },
			{ "playstation|\\bpsn\\b", "PlayStation" },
			{ "nintendo", "Nintendo Switch" },
			{ "netflix", "streaming device (Netflix)" },
			{ "printer|\\bhp\\b.*smart|epson|brother|canon|xerox|kyocera|ricoh|lexmark|dell-s2810", "Printer" },
			{ "msftconnecttest|windowsupdate|wns\\.windows|settings-win", "Windows PC" },
			{ "connectivity-check\\.ubuntu|snapcraft|archive\\.ubuntu|debian\\.org", "Linux PC" },
			{ "raspberry ?pi", "Raspberry Pi (a Teleonome?)" },
			{ "espressif", "ESP32 / ESP8266 board" },
			{ "android\\.clients|connectivitycheck\\.(gstatic|android)|play\\.googleapis", "Android phone or tablet" },
			{ "apple|icloud|aaplimg", "Apple device" },
			{ "tp-?link", "TP-Link network device" },
			{ "ubiquiti|\\bubnt\\b", "Ubiquiti network device" },
	};
	private static final List<Pattern> PATTERNS = new ArrayList<>();
	static {
		for (String[] r : RULES) PATTERNS.add(Pattern.compile(r[0], Pattern.CASE_INSENSITIVE));
	}

	/** {"Guess", "Because"} ("" when nothing matches). */
	public static JSONObject guess(String vendor, String networkName, String connection, Collection<String> domains) {
		String text = " | " + nz(vendor) + " | " + nz(networkName) + " | " + nz(connection) + " | " + String.join(" ", domains) + " |";
		for (int i = 0; i < RULES.length; i++) {
			java.util.regex.Matcher m = PATTERNS.get(i).matcher(text);
			if (m.find()) {
				String because = RULES[i].length > 2 ? RULES[i][2] : m.group().replace("|", "").trim();
				return new JSONObject().put("Guess", RULES[i][1]).put("Because", because);
			}
		}
		if (nz(vendor).startsWith("Private MAC")) {
			return new JSONObject().put("Guess", "phone, tablet or laptop (private address)").put("Because", "random MAC address");
		}
		return new JSONObject().put("Guess", "").put("Because", "");
	}

	/**
	 * The names devices announce on the network, ip -> name without ".local" (avahi-resolve -a, all addresses in one
	 * call, stopped after timeoutSeconds). Devices that don't answer are left out.
	 */
	public static Map<String, String> networkNames(Collection<String> ips, int timeoutSeconds) {
		Map<String, String> names = new HashMap<>();
		List<String> cmd = new ArrayList<>();
		cmd.add("avahi-resolve");
		cmd.add("-a");
		for (String ip : ips) {
			if (ip != null && ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) cmd.add(ip);  // only addresses go on the command line
		}
		if (cmd.size() == 2) return names;
		Process process;
		try {
			process = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start();
		} catch (IOException e) {
			logger.debug("avahi-resolve not available: " + e.getMessage());
			return names;
		}
		Thread stopper = new Thread(() -> {
			try {
				Thread.sleep(timeoutSeconds * 1000L);
				process.destroy();  // ends the read loop below
			} catch (InterruptedException e) {
				// finished in time
			}
		}, "avahi-resolve-timeout");
		stopper.setDaemon(true);
		stopper.start();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = r.readLine()) != null) {
				String[] f = line.trim().split("\\s+");
				if (f.length >= 2) {
					String n = f[1].endsWith(".local") ? f[1].substring(0, f[1].length() - 6) : f[1];
					if (!n.isEmpty()) names.put(f[0], n);
				}
			}
		} catch (IOException e) {
			// stopped by the timeout: keep what was read
		} finally {
			stopper.interrupt();
			process.destroy();
		}
		return names;
	}

	/** The domain names in TrafficStore.domains() rows. */
	public static List<String> domainNames(JSONArray domainRows) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < domainRows.length(); i++) out.add(domainRows.getJSONObject(i).optString("Domain"));
		return out;
	}

	private static String nz(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT).equals("null") ? "" : s;
	}
}
