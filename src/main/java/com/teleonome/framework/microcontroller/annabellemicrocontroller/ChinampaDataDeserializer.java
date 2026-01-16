package com.teleonome.framework.microcontroller.annabellemicrocontroller;




import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

public class ChinampaDataDeserializer extends AnnabelleDeserializer {
	Logger logger;
	
	
	public ChinampaDataDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	
	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		JSONObject toReturn = new JSONObject();
		String[] tokens = line.split("#");
		logger.debug("line 22,  tokens=" + tokens.length + " received =" +line );
		if(tokens.length<55) {
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
		
		int dataSamplingSec=0;
		try{	
			dataSamplingSec =Integer.parseInt(tokens[6].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		String latitude=tokens[7];
		String longitude=tokens[8];
		
		boolean pumprelaystatus = false;
		try{	
			int i1=Integer.parseInt(tokens[9].replaceAll("\u0000", ""));
			if(i1==1)pumprelaystatus =true;
		}catch(NumberFormatException e) {
			
		}
		
		boolean fishtankoutflowsolenoidrelaystatus = false;
		try{	
			int i1=Integer.parseInt(tokens[10].replaceAll("\u0000", ""));
			if(i1==1)fishtankoutflowsolenoidrelaystatus =true;
		}catch(NumberFormatException e) {
			
		}
		
	
		double fishtankoutflowflowRate =0.0;
		try{
			fishtankoutflowflowRate=Double.parseDouble(tokens[11].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double fishtankFlowSensorQFactor =0.0;
		try{
			fishtankFlowSensorQFactor=Double.parseDouble(tokens[12].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double minimumFishTankLevel =0.0;
		try{
			minimumFishTankLevel=Double.parseDouble(tokens[13].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double maximumFishTankLevel =0.0;
		try{
			maximumFishTankLevel=Double.parseDouble(tokens[14].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double fishTankMeasuredHeight =0.0;
		try{
			fishTankMeasuredHeight=Double.parseDouble(tokens[15].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double fishTankHeight =0.0;
		try{
			fishTankHeight=Double.parseDouble(tokens[16].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double minimumSumpTroughLevel =0.0;
		try{
			minimumSumpTroughLevel=Double.parseDouble(tokens[17].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double maximumSumpTroughLevel =0.0;
		try{
			maximumSumpTroughLevel=Double.parseDouble(tokens[18].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double sumpTroughMeasuredHeight =0.0;
		try{
			sumpTroughMeasuredHeight=Double.parseDouble(tokens[19].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double sumpTroughHeight =0.0;
		try{
			sumpTroughHeight=Double.parseDouble(tokens[20].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int sumpTroughStaleDataSeconds =0;
		try{
			sumpTroughStaleDataSeconds=Integer.parseInt(tokens[21].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int fishTankStaleDataSeconds =0;
		try{
			fishTankStaleDataSeconds=Integer.parseInt(tokens[22].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		

		boolean alertstatus = false;
		try{	
			int i1=Integer.parseInt(tokens[23].replaceAll("\u0000", ""));
			if(i1==1)alertstatus =true;
		}catch(NumberFormatException e) {
			
		}
		
		int alertcode =0;
		try{
			alertcode=Integer.parseInt(tokens[24].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		float pumpflowRate =0;
		try{
			pumpflowRate=Float.parseFloat(tokens[25].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		int microtemperature =0;
		try{
			microtemperature=Integer.parseInt(tokens[26].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		
	    // Purpose
		 
		 try{
			 secondsTime = Long.parseLong(tokens[27].replaceAll("\u0000", ""));
			}catch(NumberFormatException e) {
				
			}
		long lastPulseTime=secondsTime*1000;
		boolean timeCorrected=false;
		long sourceoriginaltime=secondsTime;
		long now = System.currentTimeMillis();
		if(lastPulseTime< (now - (3*60*1000))  || lastPulseTime> (now - (3*60*1000))) {
			secondsTime=now/1000;
			timeCorrected=true;
		}
		double rtcBatVolt = 0.0;
		
		try{
			rtcBatVolt=Double.parseDouble(tokens[28].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	
		
		double rssi = 0.0;
		try{
			rssi=Double.parseDouble(tokens[29].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double snr =0.0;
		try{
			snr=Double.parseDouble(tokens[30].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	   

		
		int digitalStablesUpload = 0;
		try{
			digitalStablesUpload=Integer.parseInt(tokens[31].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int secondsSinceLastPulse =0;
		try{
			secondsSinceLastPulse=Integer.parseInt(tokens[32].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int checksum =0;
		try{
			checksum=Integer.parseInt(tokens[33].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int loraActive =0;
		try{
			loraActive=Integer.parseInt(tokens[34].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		long dsLastUpload =0;
		try{
			dsLastUpload= Long.parseLong(tokens[35].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		int totpcode = 0;
		try{
			totpcode=Integer.parseInt(tokens[36].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	
		int secondsSinceLastFishTankData = 0;
		try{
			secondsSinceLastFishTankData=Integer.parseInt(tokens[37].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int previousFishTankMeasuredHeight = 0;
		try{
			previousFishTankMeasuredHeight=Integer.parseInt(tokens[38].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int secondsSinceLastSumpTroughData = 0;
		try{
			secondsSinceLastSumpTroughData=Integer.parseInt(tokens[39].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int previousSumpTroughMeasuredHeight = 0;
		try{
			previousSumpTroughMeasuredHeight=Integer.parseInt(tokens[40].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	
		// the sensorstatus
		boolean uTempTooHigh = Boolean.parseBoolean(tokens[41].replaceAll("\u0000", ""));
		boolean fishTankHeightSuddenChange = Boolean.parseBoolean(tokens[42].replaceAll("\u0000", ""));
		boolean sumpTroughHeightSuddenChange = Boolean.parseBoolean(tokens[43].replaceAll("\u0000", ""));
		boolean fishTankWaterTemperatureSuddenChange = Boolean.parseBoolean(tokens[44].replaceAll("\u0000", ""));
		boolean sumpTroughWaterTemperatureSuddenChange = Boolean.parseBoolean(tokens[5].replaceAll("\u0000", ""));
		boolean TDSSuddenChange = Boolean.parseBoolean(tokens[46].replaceAll("\u0000", ""));
		boolean ECSuddenChange = Boolean.parseBoolean(tokens[47].replaceAll("\u0000", ""));
		boolean DOSuddenChange = Boolean.parseBoolean(tokens[48].replaceAll("\u0000", ""));
		boolean PHSuddenChange = Boolean.parseBoolean(tokens[49].replaceAll("\u0000", ""));
		boolean ORPSuddenChange = Boolean.parseBoolean(tokens[50].replaceAll("\u0000", ""));
		boolean notUsed1 = Boolean.parseBoolean(tokens[51].replaceAll("\u0000", ""));
		boolean notUsed2 = Boolean.parseBoolean(tokens[52].replaceAll("\u0000", ""));
		
		int microtemperatureMaximum = 0;
		try{
			microtemperatureMaximum=Integer.parseInt(tokens[53].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int outdoortemperature = 0;
		try{
			outdoortemperature=Integer.parseInt(tokens[54].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int outdoorhumidity = 0;
		try{
			outdoorhumidity=Integer.parseInt(tokens[55].replaceAll("\u0000", ""));
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
		
	
		    

	    sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sump Trough Stale Data Seconds", ""+sumpTroughStaleDataSeconds, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
	    sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Stale Data Seconds", ""+fishTankStaleDataSeconds, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
	    
		sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Flow Sensor Q Factor", ""+fishtankFlowSensorQFactor, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Minimum Fish Tank Level", ""+minimumFishTankLevel, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Fish Tank Level", ""+maximumFishTankLevel, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Height", ""+fishTankHeight, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			

	    sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Minimum Sump Trough Level", ""+minimumSumpTroughLevel, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
	    sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Sump Trough Level", ""+maximumSumpTroughLevel, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
	    sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sump TroughHeight", ""+sumpTroughHeight, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
	    sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("u Temperature Maximum", ""+microtemperatureMaximum, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		
		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE,  TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray purposeDeneWords = new JSONArray();
		purposeDene.put("DeneWords", purposeDeneWords);
	

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Outflow Flow Rate", ""+fishtankoutflowflowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Pump Flow Rate", ""+pumpflowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Alert Status", ""+alertstatus, "",TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Pump Relay Status", ""+pumprelaystatus, "",TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Outflow Solenoid Relay Status", ""+fishtankoutflowsolenoidrelaystatus, "",TeleonomeConstants.DATATYPE_BOOLEAN, true));
		
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Alert Code", ""+alertcode, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Measured Height", ""+fishTankMeasuredHeight, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sump Trough Measured Height", ""+sumpTroughMeasuredHeight, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", ""+secondsTime, null,TeleonomeConstants.DATATYPE_LONG, true));
		
		if(timeCorrected) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Invalid Time", "true", null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Source Original Time", ""+sourceoriginaltime, null,TeleonomeConstants.DATATYPE_LONG, true));
		}
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null,TeleonomeConstants.DATATYPE_STRING, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("PCB Temperature", ""+microtemperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Volt", ""+rtcBatVolt, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("rssi", ""+rssi, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("snr", ""+snr, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
			
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Digital Stables Upload", ""+digitalStablesUpload, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Since Last Pulse", ""+secondsSinceLastPulse, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Lora Active", ""+loraActive, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("ds Last Upload", ""+dsLastUpload, null,TeleonomeConstants.DATATYPE_LONG, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("TOTP",""+ totpcode, null,TeleonomeConstants.DATATYPE_STRING, true));
			
		// added Dec 31 2025
		//
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Since Last Fish Tank Data", ""+secondsSinceLastFishTankData, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Previous Fish Tank Measured Height", ""+previousFishTankMeasuredHeight, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Since Last Sump Trough Data", ""+secondsSinceLastSumpTroughData, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Previous Sump Trough Measured Height", ""+previousSumpTroughMeasuredHeight, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("uTemp Too High", ""+uTempTooHigh, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Height Sudden Change", ""+fishTankHeightSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sump Trough Height Sudden Change", ""+sumpTroughHeightSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Fish Tank Water Temperature Sudden Change", ""+fishTankWaterTemperatureSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sump Trough Water Temperature Sudden Change", ""+sumpTroughWaterTemperatureSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("TDS Sudden Change", ""+TDSSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("EC Sudden Change", ""+ECSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("DO Sudden Change", ""+DOSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("PH Sudden Change", ""+loraActive, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("ORPSuddenChange", ""+ORPSuddenChange, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Temperature", ""+outdoortemperature, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Humidity", ""+outdoorhumidity, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		
		
		return toReturn;
	}


	

}
