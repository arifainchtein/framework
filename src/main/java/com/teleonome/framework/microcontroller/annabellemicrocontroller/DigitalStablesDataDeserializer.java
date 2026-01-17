package com.teleonome.framework.microcontroller.annabellemicrocontroller;




import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

public class DigitalStablesDataDeserializer extends AnnabelleDeserializer {
	Logger logger;
	
	
	public DigitalStablesDataDeserializer() {
		logger = Logger.getLogger(getClass());
	}

	
	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		JSONObject toReturn = new JSONObject();
		String[] tokens = line.split("#");
		logger.debug("line 22,  tokens=" + tokens.length + " received =" +line );
		if(tokens.length<49) {
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
		int currentFunctionValue = 0;
		try{	
			currentFunctionValue =Integer.parseInt(tokens[6].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		int dataSamplingSec = Integer.parseInt(tokens[7].replaceAll("\u0000", ""));
		String latitude=tokens[8];
		String longitude=tokens[9];
		// sensors
		String sensor1name=tokens[10];
		double qfactor1 =0.0;
		try{
			qfactor1=Double.parseDouble(tokens[11].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		String sensor2name=tokens[12];
		double qfactor2 = 0;
		try{
			qfactor2=Double.parseDouble(tokens[13].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double tank1HeightMeters =0.0;
		try{
			tank1HeightMeters=Double.parseDouble(tokens[14].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double tank1maxvollit =0.0;
		try{
			tank1maxvollit=Double.parseDouble(tokens[15].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double tank2HeightMeters =0.0;
		try{
			tank2HeightMeters=Double.parseDouble(tokens[16].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double tank2maxvollit =0.0;
		try{
			tank2maxvollit=Double.parseDouble(tokens[17].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double maximumScepticHeight =0.0;
		try{
			maximumScepticHeight=Double.parseDouble(tokens[18].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
	    // Purpose
		 
		 try{
			 secondsTime = Long.parseLong(tokens[19].replaceAll("\u0000", ""));
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
		
		double temperature = -99;
		try{
			temperature= Double.parseDouble(tokens[20].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double rtcBatVolt = 0.0;
		
		try{
			rtcBatVolt=Double.parseDouble(tokens[21].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		int opMode = -1;
		try{
			opMode=Integer.parseInt(tokens[22].replaceAll("\u0000", ""));
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
	   
		double flowRate = 0.0;
		
		try{
			flowRate=Double.parseDouble(tokens[25].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int totalMilliLitres =0;
		try{
			totalMilliLitres=Integer.parseInt(tokens[26].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double flowRate2 =0;
		try{
			flowRate2=Double.parseDouble(tokens[27].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		int totalMilliLitres2 = 90;
		try{
			totalMilliLitres2=Integer.parseInt(tokens[28].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double tank1PressurePsi = 0.0;
		try{
			tank1PressurePsi=Double.parseDouble(tokens[29].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double tank1WaterLevel = tank1PressurePsi*.703;
		
	     
		double tank2PressurePsi = 0.0;
		try{
			tank2PressurePsi=Double.parseDouble(tokens[30].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double tank2WaterLevel = tank1PressurePsi*.702;
		
	    
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
		double solarVoltage = 0.0;
		try{
			solarVoltage=Double.parseDouble(tokens[36].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double capacitorVoltage = 0.0;
		try{
			capacitorVoltage=Double.parseDouble(tokens[37].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double capacitorCurrent = 0.0;
		try{
			capacitorCurrent=Double.parseDouble(tokens[38].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int totpcode = 0;
		try{
			totpcode=Integer.parseInt(tokens[39].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	
		double outdoortemperature = 0;
		try {
			outdoortemperature = Double.parseDouble(tokens[40].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double outdoorhumidity = 0;
		try {
			outdoorhumidity = Double.parseDouble(tokens[41].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		double measuredHeight = 0;
		try {
			measuredHeight = Double.parseDouble(tokens[42].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		double scepticAvailablePercentage = 0;
		try {
			scepticAvailablePercentage = Double.parseDouble(tokens[43].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double lux = 0;
		try {
			lux = Double.parseDouble(tokens[44].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		int sleepTime =0;
		try {
			sleepTime = Integer.parseInt(tokens[45].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int ledBrightness =0;
		try {
			ledBrightness = Integer.parseInt(tokens[46].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double altitude = 0;
		try {
			altitude = Double.parseDouble(tokens[47].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double minimumEfficiencyForLed=.4, minimumEfficiencyForWifi=.5;
		try {
			minimumEfficiencyForLed = Double.parseDouble(tokens[48].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		try {
			minimumEfficiencyForWifi = Double.parseDouble(tokens[49].replaceAll("\u0000", ""));
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
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Current Function", ""+currentFunctionValue, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Latitude",latitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("longitude",longitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("altitude",""+altitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("minimumEfficiencyForLed",""+minimumEfficiencyForLed, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("minimumEfficiencyForWifi",""+minimumEfficiencyForWifi, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
		JSONObject sensorDene = new JSONObject();
		denes.put(sensorDene);
		sensorDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_SENSORS);
		JSONArray sensorDeneWords = new JSONArray();
		sensorDene.put("DeneWords", sensorDeneWords);
		
		// the Annabell Types
				
		if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_1_FLOW) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_2_FLOW) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 2 Name", sensor2name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 2", ""+qfactor2, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_1_FLOW_1_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+qfactor1, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Max Volume", ""+tank1maxvollit, "Liter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_1_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+tank1HeightMeters, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Max Volume", ""+tank1maxvollit, "Liter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_2_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+qfactor1, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Max Volume", ""+tank1maxvollit, "Liter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Name", sensor2name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Height", ""+tank2HeightMeters, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Max Volume", ""+tank2maxvollit, "Liter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_SCEPTIC_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sceptic Tank Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Sceptic Height", ""+maximumScepticHeight, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height", ""+maximumScepticHeight, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.VOLTAGE_MONITOR) {
			//sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Remote Voltage", ""+flowRate, "Volts",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_TEMP_SOILMOISTURE) {
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_LIGHT_DETECTOR) {
		}
	
		
		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE,  TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray purposeDeneWords = new JSONArray();
		purposeDene.put("DeneWords", purposeDeneWords);
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", ""+secondsTime, null,TeleonomeConstants.DATATYPE_LONG, true));
		if(timeCorrected) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Invalid Time", "true", null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Source Original Time", ""+sourceoriginaltime, null,TeleonomeConstants.DATATYPE_LONG, true));
		}
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null,TeleonomeConstants.DATATYPE_STRING, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Internal Temperature", ""+temperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Volt", ""+rtcBatVolt, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("OP Mode", ""+opMode, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("rssi", ""+rssi, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("snr", ""+snr, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Led Brightness", ""+ledBrightness, null,TeleonomeConstants.DATATYPE_INTEGER, true));
			
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Digital Stables Upload", ""+digitalStablesUpload, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", ""+operatingStatus, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Lora Active", ""+loraActive, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("ds Last Upload", ""+dsLastUpload, null,TeleonomeConstants.DATATYPE_LONG, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Solar Voltage", ""+solarVoltage, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Capacitor Voltage", ""+capacitorVoltage, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Capacitor Current", ""+capacitorCurrent, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sleep Time", ""+sleepTime, null,TeleonomeConstants.DATATYPE_LONG, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("TOTP",""+ totpcode, null,TeleonomeConstants.DATATYPE_STRING, true));
			
		Identity includedRememberedIdentity;
		if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_1_FLOW) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,(double)totalMilliLitres, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_2_FLOW) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 2", ""+flowRate2, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres 2", ""+totalMilliLitres2, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,(double)totalMilliLitres, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 2");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate2, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres 2");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,(double)totalMilliLitres2, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_1_FLOW_1_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, null,TeleonomeConstants.DATATYPE_INTEGER, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,totalMilliLitres, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
			
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_1_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_FUN_2_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Pressure Psi", ""+tank2PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Water Level", ""+tank2WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));		
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 2 Water Level");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank2WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_SCEPTIC_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Measured Height", ""+measuredHeight, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sceptic Available", ""+scepticAvailablePercentage, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Light Level", ""+lux, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Temperature", ""+outdoortemperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Humidity", ""+outdoorhumidity, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Measured Height", ""+measuredHeight, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sceptic Available", ""+scepticAvailablePercentage, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Light Level", ""+lux, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Temperature", ""+outdoortemperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Humidity", ""+outdoorhumidity, null,TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_TEMP_SOILMOISTURE) {
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_LIGHT_DETECTOR) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Light Level", ""+lux, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
		}else if(currentFunctionValue==TeleonomeConstants.VOLTAGE_MONITOR) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Remote Voltage", ""+flowRate, "Volts",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}
		
		return toReturn;
	}


	

}
