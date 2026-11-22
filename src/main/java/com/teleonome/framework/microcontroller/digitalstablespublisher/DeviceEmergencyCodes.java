package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.denome.DenomeManager;

/**
 * Device emergencies on the Emergency Channel (2026-11-23). A field device reports a problem as a number in the
 * Purpose word {@value #ALERT_CODE_WORD} of its readings; this table says which numbers, for which Device Type Id,
 * are emergencies the farmer must hear about. {@link WaterNetworkWatch#check} adds them to the water network alerts,
 * so they take the same road: Annabelle's page (Heart topic "Emergency Channel") and devices.digitalstables.com (web
 * push to the farmer's phones + email), with "OK again" when the device's newest reading no longer has the code.
 * <p>
 * The device debounces (it only reports a code it acts on), so there is no Alert After Seconds here: the first
 * reading with the code raises the alert. To add a device: give its firmware an alert code in its record, make sure
 * its deserializer puts it in the Purpose word "Alert Code", and add a line to {@link #CODES}. See the "Emergency
 * Channel" document.
 */
public class DeviceEmergencyCodes {

	public static final String ALERT_CODE_WORD = "Alert Code";
	public static final String KEY_PREFIX = "device:";

	/** Device Type Id -> alert code -> {title, what it means / what to do}. */
	static final Map<String, Map<Integer, String[]>> CODES = new HashMap<String, Map<Integer, String[]>>();
	static {
		Map<Integer, String[]> chinampa = new LinkedHashMap<Integer, String[]>();
		chinampa.put(10, new String[] { "Sump nearly full",
				"The sump is at its ceiling, so the fish tank outflow solenoid is held closed. If it stays, check the sump level and where its water comes from." });
		chinampa.put(11, new String[] { "System paused: fish tank not draining",
				"The fish tank level did not drop with the solenoid open. The solenoid and pump are off. Check the fish tank level sensor, the solenoid and the outlet; then ClearPause, or it resumes when the reading makes sense again." });
		chinampa.put(12, new String[] { "System paused: solenoid open too long",
				"The solenoid was open longer than the water budget. The solenoid and pump are off until ClearPause. Check both level sensors and the solenoid." });
		chinampa.put(13, new String[] { "System paused: fish tank not filling",
				"The fish tank level did not rise with the pump on. The solenoid and pump are off. Check the pump, the sump level and the fish tank sensor; then ClearPause, or it resumes when the reading makes sense again." });
		CODES.put("Chinampa", chinampa);
	}

	private DeviceEmergencyCodes() {
	}

	/** key ("device:<telepathon>:<code>") -> {title, message, telepathon} for every telepathon whose newest reading has an emergency code. */
	static Map<String, String[]> problems(DenomeManager dm) throws Exception {
		return problems(dm.getAllTelepathons());
	}

	/** The same, from the Telepathons nucleus' DeneChains. */
	static Map<String, String[]> problems(JSONArray chains) {
		Map<String, String[]> found = new LinkedHashMap<String, String[]>();
		for (int i = 0; chains != null && i < chains.length(); i++) {
			JSONObject chain = chains.getJSONObject(i);
			String telepathon = chain.optString("Name");
			String type = null;
			Integer code = null;
			JSONArray denes = chain.optJSONArray("Denes");
			for (int j = 0; denes != null && j < denes.length(); j++) {
				JSONObject d = denes.getJSONObject(j);
				if ("Configuration".equals(d.optString("Name"))) type = WaterNetworkWatch.str(d, "Device Type Id", null);
				else if ("Purpose".equals(d.optString("Name"))) {
					double c = WaterNetworkWatch.num(d, ALERT_CODE_WORD, -1);
					if (c >= 0) code = (int) c;
				}
			}
			if (type == null || code == null || !CODES.containsKey(type)) continue;
			String[] meaning = CODES.get(type).get(code);
			if (meaning == null) continue;
			found.put(KEY_PREFIX + telepathon + ":" + code, new String[] { telepathon + ": " + meaning[0], meaning[1], telepathon });
		}
		return found;
	}
}
