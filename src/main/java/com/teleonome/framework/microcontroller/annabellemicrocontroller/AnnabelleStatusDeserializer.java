package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.utils.Utils;

/**
 * Annabelle's own status (2026-11-01, Annabelle firmware 10): one line in the AsyncData reply, sent when
 * something is new (a packet arrived, the weather forecast went out or failed, the first after a reset) and
 * at least every 10 minutes. It becomes a telepathon "Annabelle" with her serial number, so it is stored,
 * shown and sent on to devices.digitalstables.com like the devices she hears (DevicesOutbox).
 *
 * Wire format (Annabelle.ino pushAnnabelleStatusToSerial), '#' separated:
 *   AnnabelleStatusDeserializer#1#name#serialnumber#epoch#uptimeSec#firmwareVersion#
 *   weatherEpoch#weatherDownload#weatherLora#weatherRetries#weatherTrigger#weatherPending#
 *   rxPackets#noPacket#rxDropped#txCount#deafPct#wifiReconnects#wifiDownSecMax#
 *   resetReason#lowMemRestarts#heapFree8#heapMin8#heapBlock#loopMsMax#allocFailCount
 *
 * weatherEpoch 0: no forecast sent since the reset. weatherDownload: 1 ok, 2 failed, 3 no internet.
 * weatherLora: 0 sent, -1 nothing to send, 1 LoRa init failed, 2 CAD timeout, 3 channel busy, 4 TX failed,
 * 5 channel busy after the retries.
 */
public class AnnabelleStatusDeserializer extends AnnabelleDeserializer {

	public static final String DEVICE_TYPE = "Annabelle";
	/** Tokens in format 1, the leading "AnnabelleStatusDeserializer" included. */
	public static final int TOKEN_COUNT = 27;

	private final Logger logger = Logger.getLogger(getClass());

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		String[] tokens = line.trim().split("#", -1);
		if (tokens.length < TOKEN_COUNT || !"1".equals(tokens[1])) {
			logger.debug("AnnabelleStatusDeserializer: not format 1 with " + TOKEN_COUNT + " tokens: " + line);
			return new JSONObject();
		}
		String deviceName = tokens[2];
		String serialNumber = tokens[3].trim().toLowerCase();
		if (!isValidName(deviceName) || !serialNumber.matches("[0-9a-f]{12}")) {
			logger.warn("AnnabelleStatusDeserializer: rejecting garbled name or serial number: " + line);
			return new JSONObject();
		}
		secondsTime = toLong(tokens[4]);
		if (secondsTime <= 0) {
			logger.warn("AnnabelleStatusDeserializer: rejecting, no time: " + line);
			return new JSONObject();
		}
		long now = System.currentTimeMillis() / 1000;
		boolean timeCorrected = false;
		long annabelleTime = secondsTime;
		if (Math.abs(secondsTime - now) > 3 * 60) {
			secondsTime = now;
			timeCorrected = true;
		}
		sourceoriginaltime = secondsTime;

		long weatherEpoch = toLong(tokens[7]);
		int weatherDownload = (int) toLong(tokens[8]);
		int weatherLora = (int) toLong(tokens[9]);

		JSONObject telepathon = new JSONObject();
		telepathon.put("Name", deviceName);
		telepathon.put("Short Name", deviceName);
		telepathon.put("Serial Number", serialNumber);
		telepathon.put("Raw Data", line);
		telepathon.put("Seconds Time", secondsTime);
		JSONArray denes = new JSONArray();
		telepathon.put("Denes", denes);

		JSONArray configuration = dene(denes, TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION);
		configuration.put(word("Device Type Id", DEVICE_TYPE, null, TeleonomeConstants.DATATYPE_STRING));
		configuration.put(word("Serial Number", serialNumber, null, TeleonomeConstants.DATATYPE_STRING));
		configuration.put(word("Firmware Version", tokens[6], null, TeleonomeConstants.DATATYPE_INTEGER));

		dene(denes, TeleonomeConstants.TELEPATHON_DENE_SENSORS);

		JSONArray purpose = dene(denes, TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		purpose.put(word("Seconds Time", "" + secondsTime, null, TeleonomeConstants.DATATYPE_LONG));
		if (timeCorrected) {
			purpose.put(word("Invalid Time", "true", null, TeleonomeConstants.DATATYPE_BOOLEAN));
			purpose.put(word("Source Original Time", "" + annabelleTime, null, TeleonomeConstants.DATATYPE_LONG));
		}
		purpose.put(word("Local Time", Utils.epochToLocalTimeString(secondsTime), null, TeleonomeConstants.DATATYPE_STRING));
		purpose.put(word("Uptime", "" + toLong(tokens[5]), "s", TeleonomeConstants.DATATYPE_LONG));
		// weather forecast: when it last went out (or failed), how, and why not
		purpose.put(word("Weather Forecast Time", "" + weatherEpoch, null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Weather Forecast Age", "" + (weatherEpoch > 0 ? Math.max(0, annabelleTime - weatherEpoch) : -1), "s", TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Weather Forecast Result", weatherResult(weatherDownload, weatherLora, (int) toLong(tokens[10]), weatherEpoch), null, TeleonomeConstants.DATATYPE_STRING));
		purpose.put(word("Weather Forecast Sent", "" + (weatherEpoch > 0 && weatherLora == 0), null, TeleonomeConstants.DATATYPE_BOOLEAN));
		purpose.put(word("Weather Download", "" + weatherDownload, null, TeleonomeConstants.DATATYPE_INTEGER));
		purpose.put(word("Weather LoRa", "" + weatherLora, null, TeleonomeConstants.DATATYPE_INTEGER));
		purpose.put(word("Weather Retries", "" + toLong(tokens[10]), null, TeleonomeConstants.DATATYPE_INTEGER));
		purpose.put(word("Weather Trigger", tokens[11], null, TeleonomeConstants.DATATYPE_STRING));
		purpose.put(word("Weather Pending", "" + "1".equals(tokens[12]), null, TeleonomeConstants.DATATYPE_BOOLEAN));
		// the radio, since Annabelle started
		purpose.put(word("Packets Received", "" + toLong(tokens[13]), null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Packets Corrupted", "" + toLong(tokens[14]), null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Packets Dropped", "" + toLong(tokens[15]), null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Sends", "" + toLong(tokens[16]), null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Not Listening", "" + toDouble(tokens[17]), "%", TeleonomeConstants.DATATYPE_DOUBLE));
		// health
		purpose.put(word("WiFi Reconnects", "" + toLong(tokens[18]), null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("WiFi Longest Gap", "" + toLong(tokens[19]), "s", TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Last Reset Reason", tokens[20], null, TeleonomeConstants.DATATYPE_STRING));
		purpose.put(word("Low Memory Restarts", "" + toLong(tokens[21]), null, TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Free Memory", "" + toLong(tokens[22]), "bytes", TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Lowest Free Memory", "" + toLong(tokens[23]), "bytes", TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Largest Free Block", "" + toLong(tokens[24]), "bytes", TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Longest Main Loop", "" + toLong(tokens[25]), "ms", TeleonomeConstants.DATATYPE_LONG));
		purpose.put(word("Failed Memory Requests", "" + toLong(tokens[26]), null, TeleonomeConstants.DATATYPE_LONG));
		return telepathon;
	}

	/** In words, as Annabelle's own page says it. */
	static String weatherResult(int download, int lora, int retries, long weatherEpoch) {
		if (weatherEpoch <= 0) return "none sent since Annabelle started";
		String downloadProblem = download == 2 ? "download failed" : download == 3 ? "no internet" : null;
		switch (lora) {
			case 0: return downloadProblem == null ? "sent" : "sent the previous forecast (" + downloadProblem + ")";
			case -1: return "not sent: no forecast data" + (downloadProblem == null ? "" : " (" + downloadProblem + ")");
			case 1: return "not sent: LoRa init failed";
			case 2: return "not sent: CAD timeout";
			case 3: return "not sent: channel busy";
			case 4: return "not sent: TX failed";
			case 5: return "not sent: channel busy after " + retries + " retries";
			default: return "not sent: LoRa error " + lora;
		}
	}

	private static JSONArray dene(JSONArray denes, String name) {
		JSONObject dene = new JSONObject();
		dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, name);
		JSONArray words = new JSONArray();
		dene.put("DeneWords", words);
		denes.put(dene);
		return words;
	}

	private static JSONObject word(String name, String value, String units, String type) {
		return DenomeUtils.buildDeneWordJSONObject(name, value, units, type, true);
	}

	private static long toLong(String s) {
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static double toDouble(String s) {
		try {
			return Double.parseDouble(s.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
