package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

//
// Power profile (battery chemistry/capacity and current-sensor parameters), added 2026-10-12. A
// device sends a PowerProfileRecord on the same schedule as its DeviceIdentityRecord (after a real
// reset and daily); Annabelle keeps the latest per device and relays it as one line in AsyncData
// (PowerProfileStore.cpp in DigitalStablesEsp32Lib):
//
// PowerProfileDeserializer#serialHex#version#reason#chemistry#cellsInSeries#capacity_mAh
//   #chargeEfficiency_pct#restFullVoltage_mV#restEmptyVoltage_mV#absorptionVoltage_mV
//   #cutoffVoltage_mV#batteryTempSource#batteryCurrentLsb_uA#panelCurrentLsb_uA#sleepCurrent_uA
//   #unmeasuredBaseline_uA#rssi#snr#receivedAgeSeconds      (20 tokens)
//
// 0 means "use the default for this chemistry" for chargeEfficiency and the rest voltages, and
// "unknown / not measured" for absorption voltage and sleep current. These are the inputs the
// Cerebellum needs to turn the Vital Signs battery ledger into SOC for any product.
//
// Like DeviceIdentityDeserializer this returns a single "Power Profile" Dene, which
// AnnabelleReader places into the device's existing chain (resolved by serial number).
//
public class PowerProfileDeserializer extends AnnabelleDeserializer {
	public static final int TOKEN_COUNT = 20;
	Logger logger;

	private String serialNumber = "";

	public PowerProfileDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	public String getSerialNumber() { return serialNumber; }

	public static String chemistryName(int chemistry) {
		switch (chemistry) {
			case 1: return "LiFePO4";
			case 2: return "Lead-Acid";
			case 3: return "Li-Ion";
			case 4: return "Supercap";
			default: return "Unknown";
		}
	}

	public static String tempSourceName(int source) {
		switch (source) {
			case 1: return "Battery";
			case 2: return "Board";
			default: return "None";
		}
	}

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		String[] tokens = line.split("#");
		if (tokens.length < TOKEN_COUNT) {
			logger.warn("PowerProfileDeserializer: too few tokens (" + tokens.length + "), rejecting: " + line);
			return null;
		}
		try {
			serialNumber = tokens[1].trim();
			int version = Integer.parseInt(tokens[2].trim());
			int reason = Integer.parseInt(tokens[3].trim());
			int chemistry = Integer.parseInt(tokens[4].trim());
			int cellsInSeries = Integer.parseInt(tokens[5].trim());
			long capacityMah = Long.parseLong(tokens[6].trim());
			int chargeEfficiencyPct = Integer.parseInt(tokens[7].trim());
			int restFullMv = Integer.parseInt(tokens[8].trim());
			int restEmptyMv = Integer.parseInt(tokens[9].trim());
			int absorptionMv = Integer.parseInt(tokens[10].trim());
			int cutoffMv = Integer.parseInt(tokens[11].trim());
			int batteryTempSource = Integer.parseInt(tokens[12].trim());
			int batteryLsbUa = Integer.parseInt(tokens[13].trim());
			int panelLsbUa = Integer.parseInt(tokens[14].trim());
			int sleepUa = Integer.parseInt(tokens[15].trim());
			int unmeasuredBaselineUa = Integer.parseInt(tokens[16].trim());
			double rssi = Double.parseDouble(tokens[17].trim());
			double snr = Double.parseDouble(tokens[18].trim());
			long receivedEpoch = System.currentTimeMillis() / 1000 - Long.parseLong(tokens[19].trim());

			if (serialNumber.isEmpty()) {
				logger.warn("PowerProfileDeserializer: empty serial number, rejecting: " + line);
				return null;
			}
			secondsTime = receivedEpoch;
			sourceoriginaltime = receivedEpoch;

			JSONObject dene = new JSONObject();
			dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_POWER_PROFILE);
			JSONArray w = new JSONArray();
			dene.put("DeneWords", w);
			w.put(DenomeUtils.buildDeneWordJSONObject("Battery Chemistry", chemistryName(chemistry), null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Cells In Series", "" + cellsInSeries, null, TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Battery Capacity", "" + capacityMah, "mAh", TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Charge Efficiency", "" + chargeEfficiencyPct, "%", TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Rest Full Voltage", "" + (restFullMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Rest Empty Voltage", "" + (restEmptyMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Absorption Voltage", "" + (absorptionMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Cutoff Voltage", "" + (cutoffMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Battery Temperature Source", tempSourceName(batteryTempSource), null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Battery Current Resolution", "" + batteryLsbUa, "uA", TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Panel Current Resolution", "" + panelLsbUa, "uA", TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Sleep Current", "" + sleepUa, "uA", TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Unmeasured Baseline Current", "" + unmeasuredBaselineUa, "uA", TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Power Profile Reason", DeviceIdentityDeserializer.reasonName(reason), null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Power Profile RSSI", "" + rssi, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Power Profile SNR", "" + snr, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Power Profile Version", "" + version, null, TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Power Profile Received Time", "" + receivedEpoch, null, TeleonomeConstants.DATATYPE_LONG, true));
			return dene;
		} catch (NumberFormatException | JSONException e) {
			logger.warn("PowerProfileDeserializer: could not parse '" + line + "': " + e.getMessage());
			return null;
		}
	}
}
