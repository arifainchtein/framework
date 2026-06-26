package com.teleonome.framework.microcontroller.annabellemicrocontroller;




import org.apache.log4j.Logger;
import org.json.JSONObject;

public class CommaRecordDeserializer extends AnnabelleDeserializer {
	Logger logger;

	public CommaRecordDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	// Wire format: CommaRecordDeserializer#time#voltage#devicename#serialnumber#totpcode#index#total#checksum
	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		String[] tokens = line.split("#");
		logger.debug("CommaRecordDeserializer: tokens=" + tokens.length);
		if (tokens.length < 9) {
			logger.warn("CommaRecordDeserializer: too few tokens (" + tokens.length + "), rejecting");
			return new JSONObject();
		}

		secondsTime = 0;
		try {
			secondsTime = Long.parseLong(tokens[1].trim());
		} catch (NumberFormatException e) {
			logger.debug("CommaRecordDeserializer: bad time token: " + tokens[1]);
		}
		if (secondsTime == 0) {
			logger.warn("CommaRecordDeserializer: rejecting packet — time is zero or unparseable");
			return new JSONObject();
		}
		sourceoriginaltime = secondsTime;

		String deviceName = tokens[3].trim();
		if (!isValidName(deviceName)) {
			logger.warn("CommaRecordDeserializer: rejecting packet — garbled device name: '" + deviceName + "'");
			return new JSONObject();
		}

		double voltage = 0.0;
		try {
			voltage = Double.parseDouble(tokens[2].trim());
		} catch (NumberFormatException e) {
			logger.debug("CommaRecordDeserializer: bad voltage token: " + tokens[2]);
		}

		String serialnumber = tokens[4].trim();

		long totpcode = 0;
		try {
			totpcode = Long.parseLong(tokens[5].trim());
		} catch (NumberFormatException e) {
			logger.debug("CommaRecordDeserializer: bad totpcode token: " + tokens[5]);
		}

		int index = 0;
		try {
			index = Integer.parseInt(tokens[6].trim());
		} catch (NumberFormatException e) {
			logger.debug("CommaRecordDeserializer: bad index token: " + tokens[6]);
		}

		int total = 0;
		try {
			total = Integer.parseInt(tokens[7].trim());
		} catch (NumberFormatException e) {
			logger.debug("CommaRecordDeserializer: bad total token: " + tokens[7]);
		}

		int checksum = 0;
		try {
			checksum = Integer.parseInt(tokens[8].trim());
		} catch (NumberFormatException e) {
			logger.debug("CommaRecordDeserializer: bad checksum token: " + tokens[8]);
		}

		JSONObject toReturn = new JSONObject();
		toReturn.put("devicename", deviceName);
		toReturn.put("time", secondsTime);
		toReturn.put("voltage", voltage);
		toReturn.put("serialnumber", serialnumber);
		toReturn.put("totpcode", totpcode);
		toReturn.put("index", index);
		toReturn.put("total", total);
		toReturn.put("checksum", checksum);
		return toReturn;
	}
}
