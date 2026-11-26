package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

//
// Gloria solar pump controller (Projects/Gloria), added 2026-11-26. Gloria started as a copy of Langley and sent
// LangleyData until then, so it arrived here through LangleyDataDeserializer as "Device Type Id" Langley. It now
// sends its own GloriaData packet; Annabelle relays it as (SensorSampleStore.h, GloriaDataQueue):
//
// GloriaDataDeserializer#1#devicename#shortname#serial#secondsTime#batteryV#batterymA#solarV#solarmA#
//   runtimeH#overnightmAh#temperature#batteryTemperature#rtcBatV#chemistry#rssi#snr#operatingStatus#sleep#
//   wifiStatus#opMode#relay#pumpOn#auto#pumpFault#emergencyOpen#pumpReason#flowLpm#flowTotalL#sensors#
//   sensorsMissing#latitude#longitude#altitude#     (35 tokens: split("#") drops the empty one after the last #)
//
// The same name and serial number as before, so the device's chain is taken over, now as a Gloria. Its RS485
// sensor readings (soil probes, rain) come separately, as SensorSampleDeserializer lines.
//
public class GloriaDataDeserializer extends AnnabelleDeserializer {
	public static final int TOKEN_COUNT = 35;
	Logger logger;

	public GloriaDataDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	private static double d(String[] t, int i) {
		try { return Double.parseDouble(t[i].replaceAll(" ", "")); } catch (Exception e) { return 0.0; }
	}

	private static int n(String[] t, int i) {
		try { return (int) Double.parseDouble(t[i].replaceAll(" ", "")); } catch (Exception e) { return 0; }
	}

	// GloriaPumpRule.h's PumpReason, in order - keep in step
	private static final String[] PUMP_REASONS = { "idle", "on: rule", "on: manual START", "off: stop rule", "off: no rule",
			"off: stale data", "off: dry run (no flow)", "off: low battery", "off: manual time limit", "off: manual low battery" };

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		JSONObject toReturn = new JSONObject();
		String[] tokens = line.split("#");
		logger.debug("GloriaDataDeserializer: tokens=" + tokens.length + " received=" + line);
		if (tokens.length < TOKEN_COUNT) {
			logger.debug("GloriaDataDeserializer: bad data received");
			return new JSONObject();
		}
		String deviceName = tokens[2];
		if (!isValidName(deviceName)) {
			logger.warn("GloriaDataDeserializer: rejecting packet - garbled device name: '" + deviceName + "'");
			return new JSONObject();
		}
		String shortName = tokens[3];
		String serialNumber = tokens[4];
		try {
			secondsTime = Long.parseLong(tokens[5].replaceAll(" ", ""));
		} catch (NumberFormatException e) {
			secondsTime = 0;
		}
		if (secondsTime == 0) {
			logger.warn("GloriaDataDeserializer: rejecting packet - unparseable seconds time '" + tokens[5] + "' for " + deviceName);
			return new JSONObject();
		}
		boolean timeCorrected = false;
		long now = System.currentTimeMillis();
		if (Math.abs(secondsTime * 1000 - now) > 3 * 60 * 1000) {
			secondsTime = now / 1000;
			timeCorrected = true;
		}
		sourceoriginaltime = secondsTime;

		double batteryVoltage = d(tokens, 6), batteryCurrentMa = d(tokens, 7), solarVoltage = d(tokens, 8), solarCurrentMa = d(tokens, 9);
		double estimatedRuntime = d(tokens, 10), overnightMah = d(tokens, 11), temperature = d(tokens, 12);
		double batteryTemperature = d(tokens, 13), rtcBatVolt = d(tokens, 14);
		int chemistry = n(tokens, 15);
		double rssi = d(tokens, 16), snr = d(tokens, 17);
		int operatingStatus = n(tokens, 18), sleepTime = n(tokens, 19), wifiStatus = n(tokens, 20), opMode = n(tokens, 21);
		int relay = n(tokens, 22), pumpOn = n(tokens, 23), autoMode = n(tokens, 24), pumpFault = n(tokens, 25);
		int emergencyOpen = n(tokens, 26), pumpReason = n(tokens, 27);
		double flowLpm = d(tokens, 28), flowTotal = d(tokens, 29);
		int sensors = n(tokens, 30), sensorsMissing = n(tokens, 31);
		double latitude = d(tokens, 32), longitude = d(tokens, 33), altitude = d(tokens, 34);

		toReturn.put("Name", deviceName);
		toReturn.put("Short Name", shortName);
		toReturn.put("Serial Number", serialNumber);
		String sentBy = sentByFrom(tokens);
		toReturn.put("Sent By", sentBy);
		String routes = routesFrom(tokens);
		toReturn.put("Routes", routes);
		toReturn.put("Raw Data", line);
		toReturn.put("Seconds Time", secondsTime);

		JSONArray denes = new JSONArray();
		toReturn.put("Denes", denes);

		JSONObject configurationDene = new JSONObject();
		denes.put(configurationDene);
		configurationDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION);
		JSONArray conf = new JSONArray();
		configurationDene.put("DeneWords", conf);
		conf.put(DenomeUtils.buildDeneWordJSONObject("Device Type Id", "Gloria", null, TeleonomeConstants.DATATYPE_STRING, true));
		conf.put(DenomeUtils.buildDeneWordJSONObject("Serial Number", serialNumber, null, TeleonomeConstants.DATATYPE_STRING, true));
		conf.put(DenomeUtils.buildDeneWordJSONObject("Battery Chemistry", chemistry == 1 ? "LiFePO4" : "Lead-Acid", null, TeleonomeConstants.DATATYPE_STRING, true));
		// same casing as LangleyDataDeserializer (read by GraveyardShift.java etc)
		conf.put(DenomeUtils.buildDeneWordJSONObject("Latitude", "" + latitude, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		conf.put(DenomeUtils.buildDeneWordJSONObject("longitude", "" + longitude, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		conf.put(DenomeUtils.buildDeneWordJSONObject("altitude", "" + altitude, null, TeleonomeConstants.DATATYPE_DOUBLE, true));

		JSONObject sensorDene = new JSONObject();
		denes.put(sensorDene);
		sensorDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_SENSORS);
		sensorDene.put("DeneWords", new JSONArray());

		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray p = new JSONArray();
		purposeDene.put("DeneWords", p);
		p.put(DenomeUtils.buildDeneWordJSONObject("Sent By", sentBy, null, TeleonomeConstants.DATATYPE_STRING, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Routes", routes, null, TeleonomeConstants.DATATYPE_STRING, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Route Count", "" + routeCount(routes), null, TeleonomeConstants.DATATYPE_INTEGER, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", "" + secondsTime, null, TeleonomeConstants.DATATYPE_LONG, true));
		if (timeCorrected) {
			p.put(DenomeUtils.buildDeneWordJSONObject("Invalid Time", "true", null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			p.put(DenomeUtils.buildDeneWordJSONObject("Source Original Time", "" + sourceoriginaltime, null, TeleonomeConstants.DATATYPE_LONG, true));
		}
		p.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null, TeleonomeConstants.DATATYPE_STRING, true));

		p.put(DenomeUtils.buildDeneWordJSONObject("Pump On", "" + (pumpOn == 1), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Pump Mode", autoMode == 1 ? "AUTO" : "MANUAL", null, TeleonomeConstants.DATATYPE_STRING, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Pump Fault", "" + (pumpFault == 1), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Emergency Switch Open", "" + (emergencyOpen == 1), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Pump Reason", pumpReason >= 0 && pumpReason < PUMP_REASONS.length ? PUMP_REASONS[pumpReason] : "" + pumpReason, null, TeleonomeConstants.DATATYPE_STRING, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate", "" + flowLpm, "L/min", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Flow Total", "" + flowTotal, "L", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("RS485 Sensors", "" + sensors, null, TeleonomeConstants.DATATYPE_INTEGER, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("RS485 Sensors Missing", "" + sensorsMissing, null, TeleonomeConstants.DATATYPE_INTEGER, true));

		p.put(DenomeUtils.buildDeneWordJSONObject("Solar Voltage", "" + solarVoltage, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Solar Current", "" + solarCurrentMa, "mA", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Battery Voltage", "" + batteryVoltage, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Battery Current", "" + batteryCurrentMa, "mA", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Estimated Runtime", "" + estimatedRuntime, "hours", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Voltage", "" + rtcBatVolt, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Overnight Mah", "" + overnightMah, "mAh", TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Internal Temperature", "" + temperature, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		if (batteryTemperature > -100 && batteryTemperature < 150) {
			p.put(DenomeUtils.buildDeneWordJSONObject("Battery Temperature", "" + batteryTemperature, "C", TeleonomeConstants.DATATYPE_DOUBLE, true));
		}
		p.put(DenomeUtils.buildDeneWordJSONObject("Relay Status", "" + (relay == 1), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", "" + operatingStatus, null, TeleonomeConstants.DATATYPE_INTEGER, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Sleep Time", "" + sleepTime, "s", TeleonomeConstants.DATATYPE_INTEGER, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Wifi Status", "" + wifiStatus, null, TeleonomeConstants.DATATYPE_INTEGER, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("Op Mode", "" + opMode, null, TeleonomeConstants.DATATYPE_INTEGER, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("rssi", "" + rssi, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		p.put(DenomeUtils.buildDeneWordJSONObject("snr", "" + snr, null, TeleonomeConstants.DATATYPE_DOUBLE, true));

		long timeMillis = secondsTime * 1000;
		Identity battery = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS, deviceName, "Purpose", "Battery Voltage");
		aMnemosyneManager.unwrapDouble(teleonomeName, timeMillis, battery.toString(), TeleonomeConstants.DATATYPE_DOUBLE, batteryVoltage, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "V");
		Identity flow = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS, deviceName, "Purpose", "Flow Rate");
		aMnemosyneManager.unwrapDouble(teleonomeName, timeMillis, flow.toString(), TeleonomeConstants.DATATYPE_DOUBLE, flowLpm, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "L/min");
		return toReturn;
	}
}
