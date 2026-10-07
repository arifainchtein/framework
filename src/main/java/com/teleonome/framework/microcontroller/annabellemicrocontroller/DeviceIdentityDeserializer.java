package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

//
// Device identity (product definition + running firmware build), added 2026-10-07. A device sends
// a DeviceIdentityRecord after its VitalSigns when one is due (after a real reset, after
// SetProductDefinition, and daily); Annabelle keeps the latest per device and relays it as one
// line in AsyncData (DeviceIdentityStore.cpp in DigitalStablesEsp32Lib):
//
// DeviceIdentityDeserializer#serialHex#version#firmwareBuild#labelBuild#commissionDate#reason
//   #name#firmware#pcbs#powerSource#battery#rssi#snr#receivedAgeSeconds      (15 tokens)
//
// labelBuild is the build that was running when the firmware label was saved; if it differs from
// firmwareBuild (or is 0, i.e. saved by older firmware) the label isn't known to describe the
// running code - "Firmware Label Current" says which.
//
// Like VitalSignsDeserializer this returns a single "Device Identity" Dene, which AnnabelleReader
// places into the device's existing chain (resolved by serial number).
//
public class DeviceIdentityDeserializer extends AnnabelleDeserializer {
	public static final int TOKEN_COUNT = 15;
	Logger logger;

	private String serialNumber = "";

	public DeviceIdentityDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	public String getSerialNumber() { return serialNumber; }

	public static String reasonName(int reason) {
		switch (reason) {
			case 1: return "Reset";
			case 2: return "Label Changed";
			case 3: return "Daily";
			default: return "UNKNOWN(" + reason + ")";
		}
	}

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		// -1: keep empty strings (an unset product definition field) instead of dropping them
		String[] tokens = line.split("#", -1);
		if (tokens.length < TOKEN_COUNT) {
			logger.warn("DeviceIdentityDeserializer: too few tokens (" + tokens.length + "), rejecting: " + line);
			return null;
		}
		try {
			serialNumber = tokens[1].trim();
			int version = Integer.parseInt(tokens[2].trim());
			long firmwareBuild = Long.parseLong(tokens[3].trim());
			long labelBuild = Long.parseLong(tokens[4].trim());
			long commissionDate = Long.parseLong(tokens[5].trim());
			int reason = Integer.parseInt(tokens[6].trim());
			String name = tokens[7].trim();
			String firmware = tokens[8].trim();
			String pcbs = tokens[9].trim();
			String powerSource = tokens[10].trim();
			String battery = tokens[11].trim();
			double rssi = Double.parseDouble(tokens[12].trim());
			double snr = Double.parseDouble(tokens[13].trim());
			long receivedEpoch = System.currentTimeMillis() / 1000 - Long.parseLong(tokens[14].trim());

			if (serialNumber.isEmpty()) {
				logger.warn("DeviceIdentityDeserializer: empty serial number, rejecting: " + line);
				return null;
			}
			secondsTime = receivedEpoch;
			sourceoriginaltime = receivedEpoch;
			boolean labelCurrent = labelBuild != 0 && labelBuild == firmwareBuild;

			JSONObject dene = new JSONObject();
			dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_DEVICE_IDENTITY);
			JSONArray w = new JSONArray();
			dene.put("DeneWords", w);
			w.put(DenomeUtils.buildDeneWordJSONObject("Product Definition", name, null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Firmware Label", firmware, null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Firmware Label Current", "" + labelCurrent, null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Firmware Build", "" + firmwareBuild, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Label Build", "" + labelBuild, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("PCBs", pcbs, null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Power Source", powerSource, null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Battery", battery, null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Commission Date", "" + commissionDate, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Identity Reason", reasonName(reason), null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Identity RSSI", "" + rssi, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Identity SNR", "" + snr, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Device Identity Version", "" + version, null, TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Device Identity Received Time", "" + receivedEpoch, null, TeleonomeConstants.DATATYPE_LONG, true));
			return dene;
		} catch (NumberFormatException | JSONException e) {
			logger.warn("DeviceIdentityDeserializer: could not parse '" + line + "': " + e.getMessage());
			return null;
		}
	}
}
