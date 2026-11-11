package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

/**
 * Annabelle's 5-day weather forecast (2026-11-12, Annabelle firmware 14): one line in the AsyncData reply after
 * each OpenWeather download (at most once an hour), right after her status line. It becomes the "Weather
 * Forecast" Dene of the telepathon "Annabelle" (AnnabelleReader.applySerialDene, carried over by every later
 * status line), so the Teleonome shows it and DevicesOutbox sends it on to devices.digitalstables.com with her
 * status. Annabelle is the only thing that downloads the forecast: what every screen shows is what the field
 * devices were sent over LoRa (its first 8 slots).
 *
 * Wire format (Annabelle.ino pushForecastToSerial), '#' separated:
 *   WeatherForecastDeserializer#1#serialnumber#ageSec#timezoneOffsetSec#count#slot#slot#...
 *   slot = dt,temperature,humidity,cloudiness,pressure,rain,pop,wind,code
 *   dt: UTC epoch seconds; C; %; %; hPa; mm over the 3 hours; chance of rain %; wind m/s; OpenWeather
 *   condition id (2xx thunder, 3xx drizzle, 5xx rain, 6xx snow, 7xx fog/haze, 800 clear, 80x clouds).
 *   ageSec: seconds since Annabelle downloaded it (her clock is not UTC, so no timestamp of hers).
 *
 * The Dene's words:
 *   Provider "OpenWeather"; Downloaded (epoch seconds, on the Pi's clock); Time Zone Offset (seconds east of UTC,
 *   OpenWeather's for the farm); Slot Count; Columns (the slot fields, in order); Forecast (the slots as a JSON
 *   array of arrays, in Columns order - one word instead of ~360, so the stored rows stay small).
 */
public class WeatherForecastDeserializer extends AnnabelleDeserializer {

	public static final String DENE_NAME = "Weather Forecast";
	public static final String COLUMNS = "dt,temperature,humidity,cloudiness,pressure,rain,pop,wind,code";
	/** WeatherForecastDeserializer, format, serial number, age, time zone, count. */
	public static final int HEADER_TOKENS = 6;
	private static final int SLOT_FIELDS = 9;
	private static final int MAX_SLOTS = 40;

	private final Logger logger = Logger.getLogger(getClass());
	private String serialNumber = "";

	public String getSerialNumber() { return serialNumber; }

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		String[] tokens = line.trim().split("#", -1);
		if (tokens.length < HEADER_TOKENS + 1 || !"1".equals(tokens[1])) {
			logger.debug("WeatherForecastDeserializer: not format 1: " + line);
			return null;
		}
		String serial = tokens[2].trim().toLowerCase();
		if (!serial.matches("[0-9a-f]{12}")) {
			logger.warn("WeatherForecastDeserializer: rejecting garbled serial number: " + tokens[2]);
			return null;
		}
		long ageSec = toLong(tokens[3], -1);
		long timezoneOffset = toLong(tokens[4], 0);
		int count = (int) toLong(tokens[5], -1);
		if (ageSec < 0 || count < 1 || count > MAX_SLOTS || tokens.length != HEADER_TOKENS + count) {
			logger.warn("WeatherForecastDeserializer: rejecting, age " + ageSec + ", count " + count + ", tokens " + tokens.length);
			return null;
		}
		JSONArray slots = new JSONArray();
		long previousDt = 0;
		for (int i = 0; i < count; i++) {
			String[] f = tokens[HEADER_TOKENS + i].trim().split(",", -1);
			if (f.length != SLOT_FIELDS) {
				logger.warn("WeatherForecastDeserializer: rejecting, slot " + i + " has " + f.length + " fields");
				return null;
			}
			long dt = toLong(f[0], 0);
			if (dt <= previousDt) {   // a garbled time; the slots are 3 hours apart, in order
				logger.warn("WeatherForecastDeserializer: rejecting, slot " + i + " time " + f[0] + " not after the one before");
				return null;
			}
			previousDt = dt;
			try {
				JSONArray slot = new JSONArray();
				slot.put(dt);
				slot.put(Double.parseDouble(f[1]));
				slot.put(Integer.parseInt(f[2].trim()));
				slot.put(Integer.parseInt(f[3].trim()));
				slot.put(Integer.parseInt(f[4].trim()));
				slot.put(Double.parseDouble(f[5]));
				slot.put(Integer.parseInt(f[6].trim()));
				slot.put(Double.parseDouble(f[7]));
				slot.put(Integer.parseInt(f[8].trim()));
				slots.put(slot);
			} catch (NumberFormatException e) {
				logger.warn("WeatherForecastDeserializer: rejecting, slot " + i + " is not numbers: " + tokens[HEADER_TOKENS + i]);
				return null;
			}
		}
		serialNumber = serial;
		long downloaded = System.currentTimeMillis() / 1000 - ageSec;
		secondsTime = downloaded;
		sourceoriginaltime = downloaded;

		JSONObject dene = new JSONObject();
		dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, DENE_NAME);
		JSONArray words = new JSONArray();
		dene.put("DeneWords", words);
		words.put(word("Provider", "OpenWeather", null, TeleonomeConstants.DATATYPE_STRING));
		words.put(word("Downloaded", "" + downloaded, null, TeleonomeConstants.DATATYPE_LONG));
		words.put(word("Time Zone Offset", "" + timezoneOffset, "s", TeleonomeConstants.DATATYPE_LONG));
		words.put(word("Slot Count", "" + count, null, TeleonomeConstants.DATATYPE_INTEGER));
		words.put(word("Columns", COLUMNS, null, TeleonomeConstants.DATATYPE_STRING));
		words.put(word("Forecast", slots.toString(), null, TeleonomeConstants.DATATYPE_STRING));
		return dene;
	}

	private static JSONObject word(String name, String value, String units, String type) {
		return DenomeUtils.buildDeneWordJSONObject(name, value, units, type, true);
	}

	private static long toLong(String s, long defaultValue) {
		try {
			return Long.parseLong(s.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
