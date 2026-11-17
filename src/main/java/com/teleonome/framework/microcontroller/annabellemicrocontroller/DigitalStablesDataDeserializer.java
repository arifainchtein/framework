
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
		if(tokens.length<53) {
			logger.debug("Bad data received");
			return new JSONObject();
		}
		// token[53] = wifiStatus (added after wakeTimeSec; absent in older firmware)
	
		String deserializer = tokens[0];
		String deviceTypeId=tokens[1];
		if(deviceTypeId.equals("pffodil"))deviceTypeId="Daffodil";
		//if(deviceTypeId.equals("pffodil"))deviceTypeId="Daffodil";
		//if(deviceTypeId.equals("Daffodil"))deviceTypeId="Daffodil";
		
		String deviceName=tokens[2];
		logger.debug("line 18, DigitalStablesData deviceName=" + deviceName);
		if (!isValidName(deviceName)) {
			logger.warn("DigitalStablesDataDeserializer: rejecting packet — garbled device name: '" + deviceName + "'");
			return new JSONObject();
		}
		
		if (!deviceTypeId.equals("Daffodil")) {
			logger.warn("DigitalStablesDataDeserializer: rejecting packet — deviceTypeId: '" + deviceTypeId + "'");
			return new JSONObject();
		}
		
		String deviceshortname=tokens[3];
		String serialnumber=tokens[4];
		String groupidentifier=tokens[5];
		int currentFunctionValue = 0;
		boolean usingSolarPower = true;
		try{
			currentFunctionValue = Integer.parseInt(tokens[6].replaceAll("\u0000", ""));
			usingSolarPower = currentFunctionValue >= 0;
			currentFunctionValue = Math.abs(currentFunctionValue);
		}catch(NumberFormatException e) {

		}
		int dataSamplingSec = Integer.parseInt(tokens[7].replaceAll("\u0000", ""));
		String latitude=tokens[8];
		if(latitude.equals("ovf"))latitude="0";
		String longitude=tokens[9];
		if(longitude.equals("ovf"))longitude="0";
		
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
			 logger.debug("line 104 secondsTime=" + secondsTime);
			 secondsTime = Long.parseLong(tokens[19].replaceAll("\u0000", ""));
			 logger.debug("line 106,  secondsTime=" + secondsTime);
			}catch(NumberFormatException e) {
			}
		if (secondsTime == 0) {
			logger.warn("DigitalStablesDataDeserializer: rejecting packet \u2014 unparseable seconds time token: '" + tokens[19] + "' for device: " + deviceName);
			return new JSONObject();
		}
		long lastPulseTime=secondsTime*1000;
		
		
		boolean timeCorrected=false;
		long now = System.currentTimeMillis();
		if(Math.abs(lastPulseTime - now) > 3*60*1000) {
			secondsTime=now/1000;
			timeCorrected=true;
			logger.debug("line 117, corrected secondsTime=" + secondsTime);
		}
		// captured after correction so a device with a bad/uninitialized clock persists
		// under today's date instead of scattering into garbage-dated partitions (e.g.
		// telepathon_1938_6_15) - see conversation 2026-07-21 (TopTank RTC).
		sourceoriginaltime=secondsTime;
		
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
			// the firmware sends a float ("73976.55"), which Integer.parseInt rejected -> always 0 (2026-11-16)
			totalMilliLitres=(int)Math.round(Double.parseDouble(tokens[26].replaceAll("\u0000", "").trim()));
		}catch(NumberFormatException e) {
			
		}
		double flowRate2 =0;
		try{
			flowRate2=Double.parseDouble(tokens[27].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		int totalMilliLitres2 = 90;
		try{
			// the firmware sends a float ("73976.55"), which Integer.parseInt rejected -> always 0 (2026-11-16)
			totalMilliLitres2=(int)Math.round(Double.parseDouble(tokens[28].replaceAll("\u0000", "").trim()));
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
		double batteryVoltage = 0.0;
		try{
			batteryVoltage=Double.parseDouble(tokens[36].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double v50Voltage = 0.0;
		try{
			v50Voltage=Double.parseDouble(tokens[37].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double batteryCurrent = 0.0;
		try{
			batteryCurrent=Double.parseDouble(tokens[38].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double estimatedRuntime = 0.0;
		try{
			estimatedRuntime=Double.parseDouble(tokens[39].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
	
		int totpcode = 0;
		try{
			totpcode=Integer.parseInt(tokens[40].replaceAll(" ", ""));
		}catch(NumberFormatException e) {

		}

		double outdoortemperature = 0;
		try {
			outdoortemperature = Double.parseDouble(tokens[41].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		double outdoorhumidity = 0;
		try {
			outdoorhumidity = Double.parseDouble(tokens[42].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		double measuredHeight = 0;
		try {
			measuredHeight = Double.parseDouble(tokens[43].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		
		// token[44] used to be scepticAvailablePercentage; firmware repurposed this wire slot
		// to panelVoltage (Wally solar-panel INA219 bus voltage, -99 if sensor absent) since
		// scepticAvailablePercentage was purely derived (measuredHeight/MAX_DISTANCE) and never
		// needed on the wire.
		double panelVoltage = -99;
		try {
			panelVoltage = Double.parseDouble(tokens[44].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {

		}
		
		double lux = 0;
		try {
			lux = Double.parseDouble(tokens[45].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		int sleepTime =0;
		try {
			sleepTime = Integer.parseInt(tokens[46].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int ledBrightness =0;
		try {
			ledBrightness = Integer.parseInt(tokens[47].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double altitude = 0;
		try {
			altitude = Double.parseDouble(tokens[48].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		double minimumEfficiencyForLed=.4, minimumEfficiencyForWifi=.5;
		try {
			minimumEfficiencyForLed = Double.parseDouble(tokens[49].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		try {
			minimumEfficiencyForWifi = Double.parseDouble(tokens[50].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		int asyncData =0;
		try {
			asyncData = Integer.parseInt(tokens[51].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		int wakeTimeSec = 0;
		try {
			wakeTimeSec = Integer.parseInt(tokens[52].replaceAll("\u0000", ""));
		}catch(NumberFormatException e) {
			
		}
		
		// token[53] = wifiStatus: 0=off, 1=AP, 2=STA no internet, 3=STA+internet
		// absent in older firmware — defaults to 0
		int wifiStatus = 0;
		if (tokens.length > 53) {
			try {
				wifiStatus = Integer.parseInt(tokens[53].replaceAll(" ", ""));
			}catch(NumberFormatException e) {

			}
		}

		// token[54] = panelCurrent: Wally solar-panel INA219 current, mA; -99 if sensor absent.
		// Appended at the very end so no existing field's wire position shifted; absent in
		// older firmware — defaults to -99.
		double panelCurrent = -99;
		if (tokens.length > 54) {
			try {
				panelCurrent = Double.parseDouble(tokens[54].replaceAll(" ", ""));
			}catch(NumberFormatException e) {

			}
		}

		// token[55]/[56] = measuredHeight2/maximumScepticHeight2, appended 2026-09-01; absent in
		// older firmware. measuredHeight2 is overloaded by function: 2nd trough level (cm) in
		// ANNABELL_DAFFODIL_2_WATER_TROUGH, water temperature (degrees C) in
		// ANNABELL_DAFFODIL_WATER_TROUGH_WATER_TEMP (no struct change on the Daffodil side).
		double measuredHeight2 = -99;
		if (tokens.length > 55) {
			try {
				measuredHeight2 = Double.parseDouble(tokens[55].replaceAll("\u0000", "").trim());
			}catch(NumberFormatException e) {

			}
		}
		double maximumScepticHeight2 = 0;
		if (tokens.length > 56) {
			try {
				maximumScepticHeight2 = Double.parseDouble(tokens[56].replaceAll("\u0000", "").trim());
			}catch(NumberFormatException e) {

			}
		}

		// token[57]/[58] = Level Setup Full depth / Low alarm depth (troughlevelmaximumcm/minimumcm,
		// water depths in cm) - appended by Annabelle 2026-11-16. An older Annabelle has sentby: there,
		// which fails to parse and leaves these at -1 (= not sent).
		double fullDepth = -1, lowAlarmDepth = -1;
		if (tokens.length > 57) {
			try {
				fullDepth = Double.parseDouble(tokens[57].replaceAll("\u0000", "").trim());
			}catch(NumberFormatException e) {
			}
		}
		if (tokens.length > 58) {
			try {
				lowAlarmDepth = Double.parseDouble(tokens[58].replaceAll("\u0000", "").trim());
			}catch(NumberFormatException e) {
			}
		}
		// token[59] = relayStatus (2026-11-17): the device's own LoRa relay is on (LoRaRelay.h) -> Purpose
		// "Relay Status", which the devices "Relay" badge reads (as LangleyDataDeserializer). Only "0"/"1"
		// count: an older Annabelle has sentby:/routes: there, and then no "Relay Status" is written.
		Boolean relayStatus = null;
		if (tokens.length > 59) {
			String rv = tokens[59].replaceAll("\u0000", "").trim();
			if (rv.equals("1")) relayStatus = Boolean.TRUE;
			else if (rv.equals("0")) relayStatus = Boolean.FALSE;
		}

		logger.debug("line 324,finished parsing");

		toReturn.put("Name", deviceName);
		toReturn.put("Short Name", deviceshortname);
		toReturn.put("Serial Number", serialnumber);
		// who resent it - "" = the device itself (2026-11-11, see AnnabelleDeserializer.sentByFrom)
		String sentBy = sentByFrom(tokens);
		toReturn.put("Sent By", sentBy);
		// every route that delivered it, e.g. "direct,Lan1" (2026-11-11, AnnabelleDeserializer.routesFrom)
		String routes = routesFrom(tokens);
		toReturn.put("Routes", routes);
		toReturn.put("Raw Data", line);
		toReturn.put("Seconds Time", secondsTime);
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
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Sceptic Height", ""+maximumScepticHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height", ""+maximumScepticHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_TANK1) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Height", ""+tank1HeightMeters, "Meter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Max Volume", ""+tank1maxvollit, "Liter",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height", ""+maximumScepticHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_2_WATER_TROUGH) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height", ""+maximumScepticHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough 2 Name", sensor2name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height 2", ""+maximumScepticHeight2, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_WATER_TEMP) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height", ""+maximumScepticHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_FLOW_TROUGH) {
			// sensor1name carries the flow meter name in this function; the trough uses maximumScepticHeight
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow 1 Name", sensor1name, null,TeleonomeConstants.DATATYPE_STRING, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("QFactor 1", ""+qfactor1, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Maximum Height", ""+maximumScepticHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}
		// Ultrasonic trough modes: the Level Setup depths, so the devices card can show % full
		// (depth = Maximum Height - Measured Height; % = depth / Full Depth). 2026-11-16.
		if (fullDepth > 0 && (currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_TANK1
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_2_WATER_TROUGH
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_WATER_TEMP
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_FLOW_TROUGH)) {
			sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Full Depth", ""+fullDepth, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
			if (lowAlarmDepth >= 0) sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Low Alarm Depth", ""+lowAlarmDepth, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
		}
		if(currentFunctionValue==TeleonomeConstants.VOLTAGE_MONITOR) {
			//sensorDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Remote Voltage", ""+flowRate, "Volts",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_TEMP_SOILMOISTURE) {
			
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_LIGHT_DETECTOR) {
		}
	
		
		JSONObject purposeDene = new JSONObject();
		denes.put(purposeDene);
		purposeDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE,  TeleonomeConstants.TELEPATHON_DENE_PURPOSE);
		JSONArray purposeDeneWords = new JSONArray();
		purposeDene.put("DeneWords", purposeDeneWords);
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sent By", sentBy, null, TeleonomeConstants.DATATYPE_STRING, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Routes", routes, null, TeleonomeConstants.DATATYPE_STRING, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Route Count", "" + routeCount(routes), null, TeleonomeConstants.DATATYPE_INTEGER, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Seconds Time", ""+secondsTime, null,TeleonomeConstants.DATATYPE_LONG, true));
		if(timeCorrected) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Invalid Time", "true", null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		}
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Using Solar Power", ""+usingSolarPower, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Source Original Time", ""+sourceoriginaltime, null,TeleonomeConstants.DATATYPE_LONG, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Local Time", Utils.epochToLocalTimeString(secondsTime), null,TeleonomeConstants.DATATYPE_STRING, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Async Data",""+ asyncData, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Wake Time Sec",""+wakeTimeSec, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Wifi Status",""+wifiStatus, null,TeleonomeConstants.DATATYPE_INTEGER, true));

		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Internal Temperature", ""+temperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Battery Volt", ""+rtcBatVolt, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		if (opMode >= 0) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Op Mode",        "" + ((opMode & 0x01) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Weather Fresh",  "" + ((opMode & 0x02) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("INA219 Found",   "" + ((opMode & 0x04) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("BH1750 Found",   "" + ((opMode & 0x08) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("ADS1115 Found",  "" + ((opMode & 0x10) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("RTC Found",      "" + ((opMode & 0x20) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("DS18B20 Found",  "" + ((opMode & 0x40) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("SHT Found",      "" + ((opMode & 0x80) != 0), null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
		}
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("rssi", ""+rssi, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("snr", ""+snr, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Led Brightness", ""+ledBrightness, null,TeleonomeConstants.DATATYPE_INTEGER, true));
			
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Digital Stables Upload", ""+digitalStablesUpload, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Operating Status", ""+operatingStatus, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Lora Active", ""+loraActive, null,TeleonomeConstants.DATATYPE_BOOLEAN, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("ds Last Upload", ""+dsLastUpload, null,TeleonomeConstants.DATATYPE_LONG, true));
		
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Panel Voltage", ""+panelVoltage, "Volts",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Panel Current", ""+panelCurrent, "mA",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Battery Voltage", ""+batteryVoltage, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("V50 Voltage", ""+v50Voltage, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Battery Current", ""+batteryCurrent, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Estimated Runtime", ""+estimatedRuntime, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Sleep Time", ""+sleepTime, null,TeleonomeConstants.DATATYPE_INTEGER, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("TOTP",""+ totpcode, null,TeleonomeConstants.DATATYPE_STRING, true));
		if (relayStatus != null) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Relay Status", "" + relayStatus, null, TeleonomeConstants.DATATYPE_BOOLEAN, true));
		}

		// Firmware reads and transmits these every cycle regardless of currentFunctionValue
		// (SHT/BH1750/ultrasonic are independent of the configured mode) — emit them
		// unconditionally instead of only for SCEPTIC_TANK/WATER_TROUGH, so every mode
		// (e.g. FUN_1_TANK) shows its environmental readings too.
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Measured Height", ""+measuredHeight, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Light Level", ""+lux, "lux",TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Temperature", ""+outdoortemperature, null,TeleonomeConstants.DATATYPE_DOUBLE, true));
		purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Outdoor Humidity", ""+outdoorhumidity, null,TeleonomeConstants.DATATYPE_DOUBLE, true));

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
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH) {
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_TANK1) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Pressure Psi", ""+tank1PressurePsi, "liters",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Tank 1 Water Level", ""+tank1WaterLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Tank 1 Water Level");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,tank1WaterLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "liters");
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_2_WATER_TROUGH) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Measured Height 2", ""+measuredHeight2, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_WATER_TEMP) {
			// measuredHeight2 carries the water temperature in this function; -99 = probe missing
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Water Temperature", ""+measuredHeight2, "\u00B0C",TeleonomeConstants.DATATYPE_DOUBLE, true));
			if (measuredHeight2 != -99) {
				includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Water Temperature");
				aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,measuredHeight2, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "\u00B0C");
			}
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_FLOW_TROUGH) {
			// trough level (Measured Height) is already emitted unconditionally above
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Flow Rate 1", ""+flowRate, "l/m",TeleonomeConstants.DATATYPE_DOUBLE, true));
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Total Millilitres", ""+totalMilliLitres, "ml",TeleonomeConstants.DATATYPE_INTEGER, true));
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Flow Rate 1");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,flowRate, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "l/m");
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Total Millilitres");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,(double)totalMilliLitres, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "ml");
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_TEMP_SOILMOISTURE) {
		}else if(currentFunctionValue==TeleonomeConstants.ANNABELL_LIGHT_DETECTOR) {
		}else if(currentFunctionValue==TeleonomeConstants.VOLTAGE_MONITOR) {
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Remote Voltage", ""+flowRate, "Volts",TeleonomeConstants.DATATYPE_DOUBLE, true));

		}
		// Ultrasonic trough modes: the water level as stored words, so it gets history charts like the
		// flow words (2026-11-17). Same maths as the devices card/detail and the device's own page:
		// depth = Maximum Height (sensor to bottom) - Measured Height (sensor to water), % = depth / Full Depth.
		// Measured Height <= 0 is a failed ultrasonic read, so nothing is stored for that packet.
		if ((currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_TANK1
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_2_WATER_TROUGH
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_WATER_TROUGH_WATER_TEMP
				|| currentFunctionValue==TeleonomeConstants.ANNABELL_DAFFODIL_FLOW_TROUGH)
				&& measuredHeight > 0 && maximumScepticHeight > 0) {
			double troughDepth = Math.max(0, maximumScepticHeight - measuredHeight);
			troughDepth = Math.round(troughDepth * 10) / 10.0;
			purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough Depth", ""+troughDepth, "cm",TeleonomeConstants.DATATYPE_DOUBLE, true));
			includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Trough Depth");
			aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,troughDepth, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "cm");
			if (fullDepth > 0) {
				double troughLevel = Math.round(troughDepth / fullDepth * 100);
				purposeDeneWords.put(DenomeUtils.buildDeneWordJSONObject("Trough Level", ""+troughLevel, "%",TeleonomeConstants.DATATYPE_DOUBLE, true));
				includedRememberedIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_TELEPATHONS,deviceName, "Purpose","Trough Level");
				aMnemosyneManager.unwrapDouble( teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), TeleonomeConstants.DATATYPE_DOUBLE,troughLevel, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_TELEPATHON, "%");
			}
		}
		
		return toReturn;
	}


	

}
