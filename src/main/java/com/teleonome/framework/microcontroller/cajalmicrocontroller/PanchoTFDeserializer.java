package com.teleonome.framework.microcontroller.cajalmicrocontroller;

import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;

public class PanchoTFDeserializer extends CajalDeserializer {

	@Override
	public JSONObject deserialise(String teleonomeName, String line) {
		JSONObject toReturn = new JSONObject();
		String[] tokens = line.split("#");
		String deviceTypeId=tokens[0];
		String deviceName=tokens[1];
		String deviceshortname=tokens[2];
		String serialnumber=tokens[3];
		String groupidentifier=tokens[4];
		int currentFunctionValue = Integer.parseInt(tokens[5]);
		int dataSamplingSec = Integer.parseInt(tokens[6]);
		String latitude=tokens[7];
		String longitude=tokens[8];
		// sensors
		String flow1name=tokens[9];
		double qfactor1 =0.0;
		try{
			qfactor1=Double.parseDouble(tokens[10]);
		}catch(NumberFormatException e) {
			
		}
		String flow2name=tokens[11];
		double qfactor2 = 0;
		try{
			qfactor2=Double.parseDouble(tokens[12]);
		}catch(NumberFormatException e) {
			
		}
		String tank1name=tokens[13];
		double tank1HeightMeters = 0.0;
		try{
			tank1HeightMeters=Double.parseDouble(tokens[14]);
		}catch(NumberFormatException e) {
			
		}
		String tank2name=tokens[15];
		double tank2HeightMeters = 0.0;
		try{
			tank2HeightMeters=Double.parseDouble(tokens[16]);
		}catch(NumberFormatException e) {
			
		}
	    // Purpose
		long secondsTime = Long.parseLong(tokens[17]);
		long lastPulseTime=secondsTime*1000;
		double temperature = Double.parseDouble(tokens[18]);
		double rtcBatVolt = Double.parseDouble(tokens[19]);
		int opMode = Integer.parseInt(tokens[20]);
		double rssi = 0.0;
		try{
			rssi=Double.parseDouble(tokens[21]);
		}catch(NumberFormatException e) {
			
		}
		double snr =0.0;
		try{
			snr=Double.parseDouble(tokens[22]);
		}catch(NumberFormatException e) {
			
		}
	   
		double flowRate = 0.0;
		
		try{
			flowRate=Double.parseDouble(tokens[23]);
		}catch(NumberFormatException e) {
			
		}
		
		int totalMilliLitres =0;
		try{
			totalMilliLitres=Integer.parseInt(tokens[24]);
		}catch(NumberFormatException e) {
			
		}
		double flowRate2 =0;
		try{
			flowRate2=Double.parseDouble(tokens[25]);
		}catch(NumberFormatException e) {
			
		}
		int totalMilliLitres2 = 90;
		try{
			totalMilliLitres2=Integer.parseInt(tokens[26]);
		}catch(NumberFormatException e) {
			
		}
		
		double tank1PressurePsi = 0.0;
		try{
			tank1PressurePsi=Double.parseDouble(tokens[27]);
		}catch(NumberFormatException e) {
			
		}
		double tank1PressureVolts = 0.0;
		try{
			tank1PressureVolts=Double.parseDouble(tokens[28]);
		}catch(NumberFormatException e) {
			
		}
		double tank1WaterLevel = 0.0;
		try{
			tank1WaterLevel=Double.parseDouble(tokens[29]);
		}catch(NumberFormatException e) {
			
		}
	     
		double tank2PressurePsi = 0.0;
		try{
			tank2PressurePsi=Double.parseDouble(tokens[30]);
		}catch(NumberFormatException e) {
			
		}
		double tank2PressureVolts = 0.0;
		try{
			tank2PressureVolts=Double.parseDouble(tokens[31]);
		}catch(NumberFormatException e) {
			
		}
		double tank2WaterLevel = 0.0;
		try{
			tank2WaterLevel=Double.parseDouble(tokens[32]);
		}catch(NumberFormatException e) {
			
		}
	    
		int operatingStatus = (int) Double.parseDouble(tokens[33]);
		int digitalStablesUpload = Integer.parseInt(tokens[34]);
		int secondsSinceLastPulse = Integer.parseInt(tokens[35]);
		
		toReturn.put("Name", deviceName);
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
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Short Name", deviceshortname, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Serial Number", serialnumber, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Group identifier", groupidentifier, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Device Type Id", deviceTypeId, null,TeleonomeConstants.DATATYPE_STRING, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Data Sampling Sec", ""+dataSamplingSec, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Current Function", ""+currentFunctionValue, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Latitude",latitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		configurationDeneWords.put(DenomeUtils.buildDeneWordJSONObject("longitude",longitude, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
	    

		JSONObject sensorDene = new JSONObject();
		denes.put(sensorDene);
		sensorDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, TeleonomeConstants.TELEPATHON_DENE_SENSORS);
		JSONArray sensorDeneWords = new JSONArray();
		sensorDene.put("DeneWords", sensorDeneWords);
		
		// the Cajal Types
				
		if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_1_FLOW) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", flow1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_2_FLOW) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", flow1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 2 Name", flow2name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 2", ""+qfactor2, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_1_FLOW_1_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", flow1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", tank1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+qfactor1, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_1_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", tank1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+tank1HeightMeters, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_2_TANK) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", tank1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+qfactor1, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Name", tank2name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Height", ""+tank2HeightMeters, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));	
		}
	
		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE,  TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray purposeDeneWords = new JSONArray();
		purposeDene.put("DeneWords", purposeDeneWords);
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", ""+secondsTime, null,TeleonomeConstants.DATATYPE_LONG, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Internal Temperature", ""+temperature, null,TeleonomeConstants.DATATYPE_LONG, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Volt", ""+rtcBatVolt, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("OP Mode", ""+opMode, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("rssi", ""+rssi, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("snr", ""+snr, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", ""+operatingStatus, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Digital Stables Upload", ""+digitalStablesUpload, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Since Last Pulse", ""+secondsSinceLastPulse, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", ""+operatingStatus, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		Identity includedRememberedIdentity;
		if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_1_FLOW) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_2_FLOW) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 2", ""+flowRate2, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres 2", ""+totalMilliLitres2, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,totalMilliLitres, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 2");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate2, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres 2");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,totalMilliLitres2, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_1_FLOW_1_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, null,TeleonomeConstants.DATATYPE_INTEGER, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 PressureVolts", ""+tank1PressureVolts, "volts",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Total Millilitres");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,totalMilliLitres, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");			
		
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
			
			
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_1_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 PressureVolts", ""+tank1PressureVolts, "volts",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
			
			
		}else if(currentFunctionValue==TeleonomeConstants.CAJAL_FUN_2_TANK) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 PressureVolts", ""+tank1PressureVolts, "volts",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Pressure Psi", ""+tank2PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 PressureVolts", ""+tank2PressureVolts, "volts",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 2 Water Level", ""+tank2WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));		
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
			
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 2 Water Level");
			aMnemosyneManager.unwrap( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank2WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");			
			
		}
		
		return toReturn;
	}

}
