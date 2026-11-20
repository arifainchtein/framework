package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.utils.Utils;

/**
 * Sends this Teleonome's water networks to devices.digitalstables.com (2026-11-20), so the farm's page there shows
 * the same networks, read-only, without making them again by hand. The networks are the "Water Network &lt;name&gt;"
 * DeneChains of Purpose (sperm WaterNetworks; made and changed on the Teleonome's own page).
 * <p>
 * At every "Publish To DigitalStables" the writer asks {@link #due}: the networks are sent, all of them in one message
 * on devices/&lt;login&gt;/waternetworks, when they differ from what devicesheart last acknowledged (a hash of them),
 * after every reconnect, and when a person pressed "Update Digital Stables" (which empties the hash). The answer comes
 * on devices/&lt;login&gt;/ack as {"waterNetworksHash", "outcome"}. What was last sent and acknowledged is kept in
 * {@value #STATE_FILE} in the Teleonome folder, which the web page reads to say when Digital Stables was last updated.
 * No networks at all is sent too (an empty list), so a network deleted here goes there as well.
 */
public class WaterNetworksSync {

	public static final String STATE_FILE = "DigitalStablesWaterNetworks.info";
	public static final String TOPIC_LEAF = "waternetworks";
	static final String DEFAULT_PREFIX = "Water Network ";
	private static final long RESEND_UNACKED_MS = 5 * 60 * 1000L;
	private static final Logger logger = Logger.getLogger(WaterNetworksSync.class);

	private WaterNetworksSync() {
	}

	/** The message to send now, or null when Digital Stables already has these networks. */
	public static JSONObject due(DenomeManager aDenomeManager, boolean justConnected) {
		try {
			JSONArray networks = networks(aDenomeManager);
			String hash = hash(networks.toString());
			JSONObject state = readState();
			boolean acked = hash.equals(state.optString("Acked Hash", ""));
			boolean sentRecently = hash.equals(state.optString("Sent Hash", "")) && System.currentTimeMillis() - state.optLong("Sent At", 0) < RESEND_UNACKED_MS;
			if (!justConnected && (acked || sentRecently)) return null;
			JSONObject message = new JSONObject();
			message.put("teleonome", aDenomeManager.getDenomeName());
			message.put("sentAt", System.currentTimeMillis());
			message.put("waterNetworksHash", hash);
			message.put("networks", networks);
			return message;
		} catch (Exception e) {
			logger.warn("water networks: could not prepare them for DigitalStables: " + Utils.getStringException(e));
			return null;
		}
	}

	/** Called once the message went to the broker. */
	public static void sent(JSONObject message) {
		JSONObject state = readState();
		state.put("Sent Hash", message.getString("waterNetworksHash"));
		state.put("Sent At", message.getLong("sentAt"));
		JSONArray names = new JSONArray();
		JSONArray networks = message.getJSONArray("networks");
		for (int i = 0; i < networks.length(); i++) names.put(networks.getJSONObject(i).optString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
		state.put("Sent Networks", names);
		writeState(state);
	}

	/** devicesheart's answer; true when it was about water networks. */
	public static boolean onAck(JSONObject ack) {
		if (!ack.has("waterNetworksHash")) return false;
		JSONObject state = readState();
		String outcome = ack.optString("outcome", "");
		state.put("Outcome", outcome);
		state.put("Answered At", System.currentTimeMillis());
		if (outcome.equals("stored")) {
			state.put("Acked Hash", ack.getString("waterNetworksHash"));
			state.put("Acked At", System.currentTimeMillis());
		} else {
			logger.warn("DigitalStables refused the water networks: " + outcome);
		}
		writeState(state);
		return true;
	}

	// the "Water Network ..." chains of Purpose, as they are in the Denome
	static JSONArray networks(DenomeManager aDenomeManager) {
		JSONArray list = new JSONArray();
		JSONArray nuclei = aDenomeManager.getDenomeJSONObject().getJSONObject("Denome").getJSONArray("Nuclei");
		for (int i = 0; i < nuclei.length(); i++) {
			JSONObject nucleus = nuclei.getJSONObject(i);
			if (!TeleonomeConstants.NUCLEI_PURPOSE.equals(nucleus.optString("Name"))) continue;
			JSONArray chains = nucleus.optJSONArray("DeneChains");
			String prefix = prefix(chains);
			for (int j = 0; chains != null && j < chains.length(); j++) {
				JSONObject chain = chains.getJSONObject(j);
				if (chain.optString("Name").startsWith(prefix)) list.put(chain);
			}
		}
		return list;
	}

	// Purpose:Water Networks:Water Network Settings:Network DeneChain Prefix
	private static String prefix(JSONArray chains) {
		for (int j = 0; chains != null && j < chains.length(); j++) {
			JSONObject chain = chains.getJSONObject(j);
			if (!"Water Networks".equals(chain.optString("Name"))) continue;
			JSONArray denes = chain.optJSONArray("Denes");
			for (int k = 0; denes != null && k < denes.length(); k++) {
				JSONArray words = denes.getJSONObject(k).optJSONArray("DeneWords");
				for (int w = 0; words != null && w < words.length(); w++) {
					JSONObject word = words.getJSONObject(w);
					if ("Network DeneChain Prefix".equals(word.optString("Name")) && word.optString("Value").length() > 0) return word.getString("Value");
				}
			}
		}
		return DEFAULT_PREFIX;
	}

	static String hash(String text) throws Exception {
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) sb.append(String.format("%02x", b));
		return sb.toString();
	}

	static File stateFile() {
		return new File(Utils.getLocalDirectory() + STATE_FILE);
	}

	static JSONObject readState() {
		try {
			File f = stateFile();
			if (f.isFile()) return new JSONObject(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
		} catch (Exception e) {
			logger.warn("water networks: unreadable " + STATE_FILE + ", starting again: " + e.getMessage());
		}
		return new JSONObject();
	}

	static void writeState(JSONObject state) {
		try {
			FileUtils.writeStringToFile(stateFile(), state.toString(4), StandardCharsets.UTF_8);
		} catch (Exception e) {
			logger.warn("water networks: could not write " + STATE_FILE + ": " + e.getMessage());
		}
	}
}
