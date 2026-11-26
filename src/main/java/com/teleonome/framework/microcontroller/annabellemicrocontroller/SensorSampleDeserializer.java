package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

//
// Soil / rain samples, added 2026-11-26. Any device with RS485 sensors (Gloria's fixed probes, Paula2 walking the
// farm with a soil probe and GPS, a bench test of bought soil) sends a SensorSampleRecord over LoRa; Annabelle
// relays each one once (SensorSampleStore.h):
//
// SensorSampleDeserializer#1#devicename#serial#secondsTime#sampleId#kind#role#driver#address#latitude#longitude#
//   altitude#hdop#rssi#snr#heardEpoch#valueCount#id:value#id:value#...#
//
// kind: monitor (installed sensor), spot (one-off sample at a GPS position), test (bench). role: other, rain,
// soilref (the rain-only reference plot), soiltest. hdop 0 = the position is the device's configured site, not a
// GPS fix. Values are "id:value" with the driver's value ids (moist, temp, ph, ec, n, p, k, salt, ...); "nan" =
// not read.
//
// Returns one Dene, "Sensor <driver> <address>", that AnnabelleReader puts into the sending device's chain
// (resolved by serial number, like the Power Profile): the latest sample of that sensor. Every value, and the
// position, is also remembered (Mnemosyne) under that Dene, so the soil analysis has the time series of every
// sensor on every device, and each sample's source - device, kind, role and place - stays with it.
//
public class SensorSampleDeserializer extends AnnabelleDeserializer {
	public static final int TOKEN_COUNT = 18;  // without the values
	Logger logger;

	private String serialNumber = "";
	private String deneName = "";

	public SensorSampleDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	public String getSerialNumber() { return serialNumber; }
	public String getDeneName() { return deneName; }

	private static double d(String[] t, int i) {
		try { return Double.parseDouble(t[i].replaceAll(" ", "")); } catch (Exception e) { return Double.NaN; }
	}

	// The value ids of the drivers in ModbusSensorDrivers.cpp -> label and units for the denome
	private static String[] labelOf(String id) {
		switch (id) {
			case "moist": return new String[] { "Moisture", "%" };
			case "temp": return new String[] { "Temperature", "C" };
			case "ph": return new String[] { "pH", null };
			case "ec": return new String[] { "EC", "uS/cm" };
			case "n": return new String[] { "Nitrogen", "mg/kg" };
			case "p": return new String[] { "Phosphorus", "mg/kg" };
			case "k": return new String[] { "Potassium", "mg/kg" };
			case "salt": return new String[] { "Salt", null };
			case "dosat": return new String[] { "DO Saturation", "%" };
			case "domgl": return new String[] { "Dissolved Oxygen", "mg/L" };
			case "rain": return new String[] { "Rain", null };
			default: return new String[] { id, null };
		}
	}

	private static String roleName(String role) {
		switch (role) {
			case "rain": return "Rain";
			case "soilref": return "Soil Reference";
			case "soiltest": return "Soil Test";
			default: return "Other";
		}
	}

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		String[] tokens = line.split("#");
		logger.debug("SensorSampleDeserializer: tokens=" + tokens.length + " received=" + line);
		if (tokens.length < TOKEN_COUNT) {
			logger.debug("SensorSampleDeserializer: bad data received");
			return null;
		}
		String deviceName = tokens[2];
		serialNumber = tokens[3];
		try {
			secondsTime = Long.parseLong(tokens[4].replaceAll(" ", ""));
		} catch (NumberFormatException e) {
			secondsTime = 0;
		}
		boolean timeCorrected = false;
		long now = System.currentTimeMillis();
		if (secondsTime == 0 || Math.abs(secondsTime * 1000 - now) > 3 * 60 * 60 * 1000L) {
			// a sample can be up to a few minutes old when sent; only a clearly wrong clock is replaced
			sourceoriginaltime = secondsTime;
			secondsTime = now / 1000;
			timeCorrected = true;
		} else {
			sourceoriginaltime = secondsTime;
		}
		String sampleId = tokens[5];
		String kind = tokens[6];
		String role = tokens[7];
		String driver = tokens[8];
		String address = tokens[9];
		double latitude = d(tokens, 10), longitude = d(tokens, 11), altitude = d(tokens, 12), hdop = d(tokens, 13);
		double rssi = d(tokens, 14), snr = d(tokens, 15);
		int valueCount = 0;
		try { valueCount = Integer.parseInt(tokens[17].trim()); } catch (NumberFormatException e) {}

		deneName = "Sensor " + (driver.isEmpty() ? "unknown" : driver) + " " + address;
		JSONObject dene = new JSONObject();
		dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, deneName);
		JSONArray w = new JSONArray();
		dene.put("DeneWords", w);
		w.put(DenomeUtils.buildDeneWordJSONObject("Source Device", deviceName, null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Source Serial Number", serialNumber, null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Sensor Model", driver, null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Sensor Address", address, null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Role", roleName(role), null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Sample Kind", kind, null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Sample Id", sampleId, null, TeleonomeConstants.DATATYPE_STRING, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", "" + secondsTime, null, TeleonomeConstants.DATATYPE_LONG, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null, TeleonomeConstants.DATATYPE_STRING, true));
		if (timeCorrected) {
			w.put(DenomeUtils.buildDeneWordJSONObject("Invalid Time", "true", null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Source Original Time", "" + sourceoriginaltime, null, TeleonomeConstants.DATATYPE_LONG, true));
		}
		w.put(DenomeUtils.buildDeneWordJSONObject("Latitude", "" + latitude, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("longitude", "" + longitude, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("altitude", "" + altitude, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("Position Source", hdop > 0 ? "GPS" : "Device Site", null, TeleonomeConstants.DATATYPE_STRING, true));
		if (hdop > 0) w.put(DenomeUtils.buildDeneWordJSONObject("HDOP", "" + hdop, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("rssi", "" + rssi, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		w.put(DenomeUtils.buildDeneWordJSONObject("snr", "" + snr, null, TeleonomeConstants.DATATYPE_DOUBLE, true));

		long timeMillis = secondsTime * 1000;
		for (int i = 0; i < valueCount && 18 + i < tokens.length; i++) {
			String t = tokens[18 + i].trim();
			int colon = t.indexOf(':');
			if (colon <= 0) continue;
			String id = t.substring(0, colon);
			double v;
			try { v = Double.parseDouble(t.substring(colon + 1)); } catch (NumberFormatException e) { continue; }
			if (Double.isNaN(v)) continue;
			String[] lu = labelOf(id);
			w.put(DenomeUtils.buildDeneWordJSONObject(lu[0], "" + v, lu[1], TeleonomeConstants.DATATYPE_DOUBLE, true));
			// the time series for the analysis (a bench test is remembered too: its Dene says Sample Kind test)
			Identity identity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS, deviceName, deneName, lu[0]);
			aMnemosyneManager.unwrapDouble(teleonomeName, timeMillis, identity.toString(), TeleonomeConstants.DATATYPE_DOUBLE, v, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, lu[1] == null ? "" : lu[1]);
		}
		// where each reading was taken: a walked sample's position changes from sample to sample
		if (hdop > 0) {
			Identity lat = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS, deviceName, deneName, "Latitude");
			aMnemosyneManager.unwrapDouble(teleonomeName, timeMillis, lat.toString(), TeleonomeConstants.DATATYPE_DOUBLE, latitude, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "");
			Identity lon = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS, deviceName, deneName, "longitude");
			aMnemosyneManager.unwrapDouble(teleonomeName, timeMillis, lon.toString(), TeleonomeConstants.DATATYPE_DOUBLE, longitude, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "");
		}
		return dene;
	}
}
