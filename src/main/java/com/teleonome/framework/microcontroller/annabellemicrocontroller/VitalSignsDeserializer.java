package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

//
// Device vital signs (reset / sleep / power / LoRa-TX telemetry), added 2026-10-05. Each LoRa
// device (Daffodil, Langley, Chinampa) sends a VitalSignsRecord after its data pulse; Annabelle
// keeps the latest per device and relays it as one line in AsyncData (VitalSignsSerializer.cpp
// in DigitalStablesEsp32Lib):
//
// VitalSignsDeserializer#serialHex#version#firmwareBuild#resetCount#lastResetReason#lastResetTime
//   #wakeCount#earlyWakeCount#commaWakeCount#awakeSecondsTotal#sleptSecondsTotal#lastWakeCause
//   #lastWakeDriftSec#lastAwakeMs#wakeVoltage_mV#minVoltageSinceReport_mV#txDurationMs
//   #txBatteryPre_mA#txBatteryPeak_mA#txBatteryPost_mA#txPanel_mA#txV50i_mV#txMinVoltage_mV
//   #loraTxFailCount#seq#i2cDeviceMask#rssi#snr#receivedEpoch            (30 tokens)
//
// Unlike the other deserializers this doesn't return a whole telepathon DeneChain - it returns a
// single "Vital Signs" Dene, which AnnabelleReader places into the device's existing chain
// (resolved by serial number). Counters are running totals since the device's last real reset.
//
public class VitalSignsDeserializer extends AnnabelleDeserializer {
	public static final int TOKEN_COUNT = 30;
	Logger logger;

	private String serialNumber = "";
	private long resetCount = -1;
	private long seq = -1;
	private String lastResetReasonName = "";
	private long lastResetTime = 0;

	public VitalSignsDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	public String getSerialNumber() { return serialNumber; }
	public long getResetCount() { return resetCount; }
	public long getSeq() { return seq; }
	public String getLastResetReasonName() { return lastResetReasonName; }
	public long getLastResetTime() { return lastResetTime; }

	// esp_reset_reason_t (ESP-IDF). On the original ESP32 an EN-pin reset reports POWERON.
	public static String resetReasonName(int reason) {
		switch (reason) {
			case 1: return "POWERON";
			case 2: return "EXT_PIN";
			case 3: return "SW";
			case 4: return "PANIC";
			case 5: return "INT_WDT";
			case 6: return "TASK_WDT";
			case 7: return "WDT";
			case 8: return "DEEPSLEEP";
			case 9: return "BROWNOUT";
			case 10: return "SDIO";
			case 11: return "USB";
			case 12: return "JTAG";
			case 13: return "EFUSE";
			case 14: return "PWR_GLITCH";
			case 15: return "CPU_LOCKUP";
			default: return "UNKNOWN";
		}
	}

	// esp_sleep_wakeup_cause_t (ESP-IDF)
	public static String wakeCauseName(int cause) {
		switch (cause) {
			case 0: return "RESET";      // not a deep-sleep wake
			case 2: return "EXT0";
			case 3: return "EXT1";
			case 4: return "TIMER";
			case 5: return "TOUCHPAD";
			case 6: return "ULP";
			case 7: return "GPIO";
			case 8: return "UART";
			default: return "OTHER(" + cause + ")";
		}
	}

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		String[] tokens = line.split("#");
		if (tokens.length < TOKEN_COUNT) {
			logger.warn("VitalSignsDeserializer: too few tokens (" + tokens.length + "), rejecting: " + line);
			return null;
		}
		try {
			serialNumber = tokens[1].trim();
			int version = Integer.parseInt(tokens[2].trim());
			long firmwareBuild = Long.parseLong(tokens[3].trim());
			resetCount = Long.parseLong(tokens[4].trim());
			int lastResetReason = Integer.parseInt(tokens[5].trim());
			lastResetTime = Long.parseLong(tokens[6].trim());
			long wakeCount = Long.parseLong(tokens[7].trim());
			long earlyWakeCount = Long.parseLong(tokens[8].trim());
			long commaWakeCount = Long.parseLong(tokens[9].trim());
			long awakeSecondsTotal = Long.parseLong(tokens[10].trim());
			long sleptSecondsTotal = Long.parseLong(tokens[11].trim());
			int lastWakeCause = Integer.parseInt(tokens[12].trim());
			int lastWakeDriftSec = Integer.parseInt(tokens[13].trim());
			int lastAwakeMs = Integer.parseInt(tokens[14].trim());
			int wakeVoltageMv = Integer.parseInt(tokens[15].trim());
			int minVoltageMv = Integer.parseInt(tokens[16].trim());
			int txDurationMs = Integer.parseInt(tokens[17].trim());
			int txPreMa = Integer.parseInt(tokens[18].trim());
			int txPeakMa = Integer.parseInt(tokens[19].trim());
			int txPostMa = Integer.parseInt(tokens[20].trim());
			int txPanelMa = Integer.parseInt(tokens[21].trim());
			int txV50iMv = Integer.parseInt(tokens[22].trim());
			int txMinVoltageMv = Integer.parseInt(tokens[23].trim());
			long loraTxFailCount = Long.parseLong(tokens[24].trim());
			seq = Long.parseLong(tokens[25].trim());
			int i2cDeviceMask = Integer.parseInt(tokens[26].trim());
			double rssi = Double.parseDouble(tokens[27].trim());
			double snr = Double.parseDouble(tokens[28].trim());
			long receivedEpoch = Long.parseLong(tokens[29].trim());

			if (serialNumber.isEmpty()) {
				logger.warn("VitalSignsDeserializer: empty serial number, rejecting: " + line);
				return null;
			}
			secondsTime = receivedEpoch;
			sourceoriginaltime = receivedEpoch;
			lastResetReasonName = resetReasonName(lastResetReason);

			JSONObject dene = new JSONObject();
			dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_VITAL_SIGNS);
			JSONArray w = new JSONArray();
			dene.put("DeneWords", w);
			// resets
			w.put(DenomeUtils.buildDeneWordJSONObject("Reset Count", "" + resetCount, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Last Reset Reason", lastResetReasonName, null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Last Reset Time", "" + lastResetTime, null, TeleonomeConstants.DATATYPE_LONG, true));
			// sleep
			w.put(DenomeUtils.buildDeneWordJSONObject("Wake Count", "" + wakeCount, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Early Wake Count", "" + earlyWakeCount, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Aborted Wake Count", "" + commaWakeCount, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Awake Seconds Total", "" + awakeSecondsTotal, "s", TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Slept Seconds Total", "" + sleptSecondsTotal, "s", TeleonomeConstants.DATATYPE_LONG, true));
			long cycleSeconds = awakeSecondsTotal + sleptSecondsTotal;
			if (cycleSeconds > 0) {
				double awakePercent = Math.round(1000.0 * awakeSecondsTotal / cycleSeconds) / 10.0;
				w.put(DenomeUtils.buildDeneWordJSONObject("Awake Percent", "" + awakePercent, "%", TeleonomeConstants.DATATYPE_DOUBLE, true));
			}
			w.put(DenomeUtils.buildDeneWordJSONObject("Last Wake Cause", wakeCauseName(lastWakeCause), null, TeleonomeConstants.DATATYPE_STRING, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Last Wake Drift", "" + lastWakeDriftSec, "s", TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Last Awake Duration", "" + lastAwakeMs, "ms", TeleonomeConstants.DATATYPE_INTEGER, true));
			// power (0 mV = not measured -> left out)
			if (wakeVoltageMv > 0) w.put(DenomeUtils.buildDeneWordJSONObject("Wake Voltage", "" + (wakeVoltageMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			if (minVoltageMv > 0) w.put(DenomeUtils.buildDeneWordJSONObject("Min Voltage Since Report", "" + (minVoltageMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			if (wakeVoltageMv > 0 && txMinVoltageMv > 0) {
				w.put(DenomeUtils.buildDeneWordJSONObject("Battery Sag", "" + ((wakeVoltageMv - txMinVoltageMv) / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			}
			// last LoRa TX
			w.put(DenomeUtils.buildDeneWordJSONObject("TX Duration", "" + txDurationMs, "ms", TeleonomeConstants.DATATYPE_INTEGER, true));
			if (txPreMa != 0 || txPeakMa != 0 || txPostMa != 0) {
				w.put(DenomeUtils.buildDeneWordJSONObject("TX Battery Pre", "" + txPreMa, "mA", TeleonomeConstants.DATATYPE_INTEGER, true));
				w.put(DenomeUtils.buildDeneWordJSONObject("TX Battery Peak", "" + txPeakMa, "mA", TeleonomeConstants.DATATYPE_INTEGER, true));
				w.put(DenomeUtils.buildDeneWordJSONObject("TX Battery Post", "" + txPostMa, "mA", TeleonomeConstants.DATATYPE_INTEGER, true));
			}
			if (txPanelMa >= 0) w.put(DenomeUtils.buildDeneWordJSONObject("TX Panel Current", "" + txPanelMa, "mA", TeleonomeConstants.DATATYPE_INTEGER, true));
			if (txV50iMv > 0) w.put(DenomeUtils.buildDeneWordJSONObject("TX V50I", "" + (txV50iMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			if (txMinVoltageMv > 0) w.put(DenomeUtils.buildDeneWordJSONObject("TX Min Voltage", "" + (txMinVoltageMv / 1000.0), "V", TeleonomeConstants.DATATYPE_DOUBLE, true));
			// radio / peripherals / identity
			w.put(DenomeUtils.buildDeneWordJSONObject("LoRa TX Fail Count", "" + loraTxFailCount, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Sequence", "" + seq, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("I2C Device Mask", "" + i2cDeviceMask, null, TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Vital Signs RSSI", "" + rssi, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Vital Signs SNR", "" + snr, null, TeleonomeConstants.DATATYPE_DOUBLE, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Firmware Build", "" + firmwareBuild, null, TeleonomeConstants.DATATYPE_LONG, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Vital Signs Version", "" + version, null, TeleonomeConstants.DATATYPE_INTEGER, true));
			w.put(DenomeUtils.buildDeneWordJSONObject("Vital Signs Received Time", "" + receivedEpoch, null, TeleonomeConstants.DATATYPE_LONG, true));
			return dene;
		} catch (NumberFormatException | JSONException e) {
			logger.warn("VitalSignsDeserializer: could not parse '" + line + "': " + e.getMessage());
			return null;
		}
	}
}
