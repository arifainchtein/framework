package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

public class LangleyDataDeserializer extends AnnabelleDeserializer {
	Logger logger;

	public LangleyDataDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	// Wire format (see LangleyDataSerializer.cpp in DigitalStablesEsp32Lib):
	// LangleyDataDeserializer#devicename#deviceshortname#secondsTime#externalBatteryVoltage#
	// energizerBatteryVoltage#energizerBatteryCurrent#temperature#fenceVoltage#fenceVoltageMin#
	// fenceVoltageMax#fenceVoltageAvg#pulseCount#rssi#snr#operatingStatus#wpsFrequencySeconds#
	// maxWPSVoltage#minWPSVoltage#secondsSinceLastPulse#solarVoltage#solarCurrentMa#batteryVoltage#
	// batteryCurrentMa#estimatedRuntime#batteryChemistry#rtcBatVolt#checksum
	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		JSONObject toReturn = new JSONObject();
		String[] tokens = line.split("#");
		logger.debug("LangleyDataDeserializer: tokens=" + tokens.length + " received=" + line);
		if (tokens.length < 28) {
			logger.debug("Bad data received");
			return new JSONObject();
		}

		String deviceName = tokens[1];
		if (!isValidName(deviceName)) {
			logger.warn("LangleyDataDeserializer: rejecting packet — garbled device name: '" + deviceName + "'");
			return new JSONObject();
		}
		String deviceshortname = tokens[2];

		try {
			secondsTime = Long.parseLong(tokens[3].replaceAll(" ", ""));
		} catch (NumberFormatException e) {
			secondsTime = 0;
		}
		if (secondsTime == 0) {
			logger.warn("LangleyDataDeserializer: rejecting packet — unparseable seconds time token: '" + tokens[3] + "' for device: " + deviceName);
			return new JSONObject();
		}
		sourceoriginaltime = secondsTime;
		long lastPulseTime = secondsTime * 1000;
		boolean timeCorrected = false;
		long now = System.currentTimeMillis();
		if (Math.abs(lastPulseTime - now) > 3 * 60 * 1000) {
			secondsTime = now / 1000;
			timeCorrected = true;
			logger.debug("LangleyDataDeserializer: corrected secondsTime=" + secondsTime);
		}

		double externalBatteryVoltage = 0.0;
		try { externalBatteryVoltage = Double.parseDouble(tokens[4].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double energizerBatteryVoltage = 0.0;
		try { energizerBatteryVoltage = Double.parseDouble(tokens[5].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double energizerBatteryCurrent = 0.0;
		try { energizerBatteryCurrent = Double.parseDouble(tokens[6].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double temperature = 0.0;
		try { temperature = Double.parseDouble(tokens[7].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double fenceVoltage = 0.0;
		try { fenceVoltage = Double.parseDouble(tokens[8].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double fenceVoltageMin = 0.0;
		try { fenceVoltageMin = Double.parseDouble(tokens[9].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double fenceVoltageMax = 0.0;
		try { fenceVoltageMax = Double.parseDouble(tokens[10].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double fenceVoltageAvg = 0.0;
		try { fenceVoltageAvg = Double.parseDouble(tokens[11].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		int pulseCount = 0;
		try { pulseCount = Integer.parseInt(tokens[12].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double rssi = 0.0;
		try { rssi = Double.parseDouble(tokens[13].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double snr = 0.0;
		try { snr = Double.parseDouble(tokens[14].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		int operatingStatus = 0;
		try { operatingStatus = (int) Double.parseDouble(tokens[15].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double wpsFrequencySeconds = 0.0;
		try { wpsFrequencySeconds = Double.parseDouble(tokens[16].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double maxWPSVoltage = 0.0;
		try { maxWPSVoltage = Double.parseDouble(tokens[17].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double minWPSVoltage = 0.0;
		try { minWPSVoltage = Double.parseDouble(tokens[18].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		long secondsSinceLastPulse = 0;
		try { secondsSinceLastPulse = Long.parseLong(tokens[19].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double solarVoltage = 0.0;
		try { solarVoltage = Double.parseDouble(tokens[20].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double solarCurrentMa = 0.0;
		try { solarCurrentMa = Double.parseDouble(tokens[21].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double batteryVoltage = 0.0;
		try { batteryVoltage = Double.parseDouble(tokens[22].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double batteryCurrentMa = 0.0;
		try { batteryCurrentMa = Double.parseDouble(tokens[23].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		double estimatedRuntime = 0.0;
		try { estimatedRuntime = Double.parseDouble(tokens[24].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		// 0 = lead-acid, 1 = LiFePO4 - matches the BatteryChemistry enum in BatteryPowerManager.h
		// (DigitalStablesEsp32Lib). Read from hardware at boot, not user-set - see
		// detectBatteryChemistry() in Langley.ino.
		int batteryChemistry = 0;
		try { batteryChemistry = Integer.parseInt(tokens[25].replaceAll(" ", "")); } catch (NumberFormatException e) {}
		String batteryChemistryName = (batteryChemistry == 1) ? "LiFePO4" : "Lead-Acid";

		// Voltage of the RTC's backup coin cell (CR1220), via Valentino's R41/R46 divider on
		// GPIO36. -1 = ADC pinned near VCC - floating pin/divider not installed/cell missing.
		double rtcBatVolt = 0.0;
		try { rtcBatVolt = Double.parseDouble(tokens[26].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		int checksum = 0;
		try { checksum = Integer.parseInt(tokens[27].replaceAll(" ", "")); } catch (NumberFormatException e) {}

		logger.debug("LangleyDataDeserializer: finished parsing");

		toReturn.put("Name", deviceName);
		toReturn.put("Short Name", deviceshortname);
		toReturn.put("Raw Data", line);
		toReturn.put("Seconds Time", secondsTime);

		JSONArray denes = new JSONArray();
		toReturn.put("Denes", denes);

		//
		// configuration
		//
		JSONObject configurationDene = new JSONObject();
		denes.put(configurationDene);
		configurationDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION);

		JSONArray configurationDeneWords = new JSONArray();
		configurationDene.put("DeneWords", configurationDeneWords);
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Device Type Id", "Langley", null, TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Battery Chemistry", batteryChemistryName, null, TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("WPS Frequency Seconds", "" + wpsFrequencySeconds, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Max WPS Voltage", "" + maxWPSVoltage, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Min WPS Voltage", "" + minWPSVoltage, null, TeleonomeConstants.DATATYPE_DOUBLE, true));

		JSONObject sensorDene = new JSONObject();
		denes.put(sensorDene);
		sensorDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_SENSORS);
		JSONArray sensorDeneWords = new JSONArray();
		sensorDene.put("DeneWords", sensorDeneWords);

		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray purposeDeneWords = new JSONArray();
		purposeDene.put("DeneWords", purposeDeneWords);

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", "" + secondsTime, null, TeleonomeConstants.DATATYPE_LONG, true));
		if (timeCorrected) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Invalid Time", "true", null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Source Original Time", "" + sourceoriginaltime, null, TeleonomeConstants.DATATYPE_LONG, true));
		}
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null, TeleonomeConstants.DATATYPE_STRING, true));

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fence Voltage", "" + fenceVoltage, "kV", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fence Voltage Min", "" + fenceVoltageMin, "kV", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fence Voltage Max", "" + fenceVoltageMax, "kV", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fence Voltage Avg", "" + fenceVoltageAvg, "kV", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Pulse Count", "" + pulseCount, null, TeleonomeConstants.DATATYPE_INTEGER, true));

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Energizer Battery Voltage", "" + energizerBatteryVoltage, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Energizer Battery Current", "" + energizerBatteryCurrent, "mA", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("External Battery Voltage", "" + externalBatteryVoltage, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Solar Voltage", "" + solarVoltage, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Solar Current", "" + solarCurrentMa, "mA", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Battery Voltage", "" + batteryVoltage, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Battery Current", "" + batteryCurrentMa, "mA", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Estimated Runtime", "" + estimatedRuntime, "hours", TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Voltage", "" + rtcBatVolt, "V", TeleonomeConstants.DATATYPE_DOUBLE, true));

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Internal Temperature", "" + temperature, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", "" + operatingStatus, null, TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Since Last Pulse", "" + secondsSinceLastPulse, null, TeleonomeConstants.DATATYPE_INTEGER, true));

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("rssi", "" + rssi, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("snr", "" + snr, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Checksum", "" + checksum, null, TeleonomeConstants.DATATYPE_INTEGER, true));

		Identity includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS, deviceName, "Purpose", "Fence Voltage Avg");
		aMnemosyneManager.unwrapDouble(teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE, fenceVoltageAvg, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "kV");

		return toReturn;
	}
}
