package com.teleonome.framework.microcontroller.annabellemicrocontroller;




import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

public class SeedlingMonitorDataDeserializer extends AnnabelleDeserializer {
	Logger logger;
	
	
	public SeedlingMonitorDataDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	
	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		JSONObject toReturn = new JSONObject();
		String[] tokens = line.split("#");
		logger.debug("line 22,  tokens=" + tokens.length + " received =" +line );
		if(tokens.length<24) {
			logger.debug("Bad data received");
			return new JSONObject();
		}
	
		String deserializer = tokens[0];
		String deviceTypeId=tokens[1];
		String deviceName=tokens[2];
		logger.debug("line 18, DigitalStablesData deviceName=" + deviceName);
		String deviceshortname=tokens[3];
		String serialnumber=tokens[4];
		String groupidentifier=tokens[5];
		
		int dataSamplingSec = Integer.parseInt(tokens[7].replaceAll("\u0000", ""));
		String latitude=tokens[8];
		String longitude=tokens[9];
		
		 // Purpose
		 
		
		double greenhouseTemp =0.0;
		try{
			greenhouseTemp=Double.parseDouble(tokens[11].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}

		double greenhouseHum = 0;
		try{
			greenhouseHum=Double.parseDouble(tokens[13].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double temperature = 0;
		try{
			temperature=Double.parseDouble(tokens[13].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double outdoorTemperature =0.0;
		try{
			outdoorTemperature=Double.parseDouble(tokens[14].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		boolean humidifierstatus=false;
		try{
			humidifierstatus=Boolean.parseBoolean(tokens[14].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	 
		 try{
			 secondsTime = Long.parseLong(tokens[19].replaceAll("\u0000", ""));
			}catch(NumberFormatException e) {
				
			}
		long lastPulseTime=secondsTime*1000;
		
		
		double rtcBatVolt = 0.0;
		
		try{
			rtcBatVolt=Double.parseDouble(tokens[21].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		double rssi = 0.0;
		try{
			rssi=Double.parseDouble(tokens[23].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double snr =0.0;
		try{
			snr=Double.parseDouble(tokens[24].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	   
		
		int operatingStatus = 0; 
		try{
			operatingStatus=Integer.parseInt(tokens[31].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int digitalStablesUpload = 0;
		try{
			digitalStablesUpload=Integer.parseInt(tokens[32].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int secondsSinceLastPulse =0;
		try{
			secondsSinceLastPulse=Integer.parseInt(tokens[33].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int checksum =0;
		try{
			checksum=Integer.parseInt(tokens[34].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int loraActive =0;
		try{
			loraActive=Integer.parseInt(tokens[35].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		long dsLastUpload =0;
		try{
			dsLastUpload= Long.parseLong(tokens[36].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int totpcode = 0;
		try{
			totpcode=Integer.parseInt(tokens[40].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	
		
		logger.debug("line 324,finished parsing");
		
		toReturn.put("Name", deviceName);
		toReturn.put("Short Name", deviceshortname);
		toReturn.put("Serial Number", serialnumber);
		toReturn.put("Raw Data", line);
		
		JSONArray denes = new JSONArray();
		toReturn.put("Denes", denes);
		//
		// configuration
		//
		JSONObject configurationDene = new JSONObject();
		denes.put(configurationDene);
		configurationDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE,TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION);

		JSONArray configurationDeneWords = new JSONArray();
		configurationDene.put("DeneWords", configurationDeneWords);
		//configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Serial Number", serialnumber, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Group identifier", groupidentifier, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Device Type Id", deviceTypeId, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Data Sampling Sec", ""+dataSamplingSec, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Latitude",latitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("longitude",longitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
		JSONObject sensorDene = new JSONObject();
		denes.put(sensorDene);
		sensorDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_SENSORS);
		JSONArray sensorDeneWords = new JSONArray();
		sensorDene.put("DeneWords", sensorDeneWords);
		
		// the Annabell Types
				
		
	
		
		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE,  TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray purposeDeneWords = new JSONArray();
		purposeDene.put("DeneWords", purposeDeneWords);
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", ""+secondsTime, null,TeleonomeConstants.DATATYPE_LONG, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null,TeleonomeConstants.DATATYPE_STRING, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Internal Temperature", ""+temperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Volt", ""+rtcBatVolt, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("rssi", ""+rssi, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("snr", ""+snr, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
			
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Digital Stables Upload", ""+digitalStablesUpload, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Since Last Pulse", ""+secondsSinceLastPulse, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", ""+operatingStatus, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Lora Active", ""+loraActive, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("ds Last Upload", ""+dsLastUpload, null,TeleonomeConstants.DATATYPE_LONG, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("TOTP",""+ totpcode, null,TeleonomeConstants.DATATYPE_STRING, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Temperature", ""+outdoorTemperature, "C",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seedling Temperature", ""+greenhouseTemp, "C",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seedling Humidity", ""+greenhouseHum, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Humidifier Status", ""+humidifierstatus, "",TeleonomeConstants.DATATYPE_BOOLEAN, true));
		
		Identity includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Outdoor Temperature");
		aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,outdoorTemperature, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "C");			
		
		includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Seedling Temperature");
		aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,greenhouseTemp, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "C");			
		
		includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Seedling Humidity");
		aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,greenhouseHum, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "%");			
		
		includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Humidifier Status");
		aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_BOOLEAN,humidifierstatus, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "");			
		
		
		return toReturn;
	}


	

}
