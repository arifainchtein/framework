package com.teleonome.framework.microcontroller.plseries;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.utils.Utils;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;;


public class PLSeriesReader extends BufferedReader {

	
	public final static String BOOST = "Boost";
	public final static String ABSORTION = "Absortion";
	public final static String EQUALIZE= "Equalize";
	public final static String FLOAT = "Float";
	
	DecimalFormat decimalFormat = new DecimalFormat("###.##");
	
	Logger logger;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;
	private double voltageSystemCorrectorFactor=1;
	//
	// pl20 is 0.1
	//pl40 0.2
	// pl60=0.4
	private double chargeCurrentFactor=1;
	private String dataLine="";
	private int PAUSE_BETWEEN_DATA=3000;
	private String currentCommand="";
	private boolean asyncMode=false;
	private boolean readerReady=true;
	
	public PLSeriesReader(Reader in,InputStream io,OutputStream oo, int sv) {
		super(in);
		logger = Logger.getLogger(getClass());
		voltageSystemCorrectorFactor=sv/12;
		serialPortInputStream = io;
		 serialPortOutputStream = oo;
	}
	  
	public void setCurrentCommand(String s) {
		logger.debug("set current command=" +s);
			currentCommand=s;
		
	}
	
	public void setAsyncMode(boolean b) {
		asyncMode=b;
	}
	@Override
	public String readLine(){
		logger.info("readLine, current Command=" +currentCommand + " asyncMode=" +asyncMode);
		
		if(currentCommand.startsWith("PulseStart")) {
			asyncMode=false;
			return "Ok-PulseStart";
		}
		else if(currentCommand.startsWith("PulseFinished")) {
			currentCommand="";
			return "Ok-PulseFinished";
		}
		
		
		logger.debug("plseries invoking readline " );
		logger.debug("Ra-" + "about to read data");
		
		logger.debug("Ra-" + "about to read currentCharge, PAUSE_BETWEEN_DATA=" + PAUSE_BETWEEN_DATA);
		double currentCharge  = getCurrentCharge();
		logger.debug("Ra- currentCharge" + currentCharge);
		try {
			Thread.sleep(PAUSE_BETWEEN_DATA);
		} catch (InterruptedException e3) {
			// TODO Auto-generated catch block
			logger.info(Utils.getStringException(e3));
		}
		
		
		
		
		
		
		logger.debug("Ra-" + "about to read voltage");
		double batteryVoltage  = getCurrentVoltage();
		logger.debug("Ra- batteryVoltage" + batteryVoltage);
		try {
			Thread.sleep(PAUSE_BETWEEN_DATA);
		} catch (InterruptedException e3) {
			// TODO Auto-generated catch block
			logger.info(Utils.getStringException(e3));
		}
		
		String batteryState = getBatteryState();
		logger.debug("Ra-" + "battery state=" +  batteryState);
		try {
			Thread.sleep(PAUSE_BETWEEN_DATA);
		} catch (InterruptedException e3) {
			// TODO Auto-generated catch block
			logger.info(Utils.getStringException(e3));
		}
		
		
		logger.debug("Ra- about to read currentLoad" );
		double currentLoad = getCurrentLoad();
		logger.debug("Ra- currentLoad" + currentLoad);
		try {
			Thread.sleep(PAUSE_BETWEEN_DATA);
		} catch (InterruptedException e3) {
			// TODO Auto-generated catch block
			logger.info(Utils.getStringException(e3));
		}
		
		
		
//		
//		double maxBatVoltageToday = getMaxBatVoltageToday();
//		logger.debug("Ra- maxBatVoltageToday" + maxBatVoltageToday);
//		try {
//			Thread.sleep(PAUSE_BETWEEN_DATA);
//		} catch (InterruptedException e3) {
//			// TODO Auto-generated catch block
//			logger.info(Utils.getStringException(e3));
//		}
//		logger.debug("Ra- about to get minBatVoltageToday" );
//		double minBatVoltageToday  = getMinBatVoltageToday();
//		logger.debug("Ra- minBatVoltageToday" + minBatVoltageToday);
//		try {
//			Thread.sleep(PAUSE_BETWEEN_DATA);
//		} catch (InterruptedException e3) {
//			// TODO Auto-generated catch block
//			logger.info(Utils.getStringException(e3));
//		}
//		
//		logger.debug("Ra- about totalChargeAmpHoursForToday" );
//		double totalChargeAmpHoursForToday = getTotalChargeAmpHoursForToday();
//		logger.debug("Ra- totalChargeAmpHoursForToday" + totalChargeAmpHoursForToday);
//		try {
//			Thread.sleep(PAUSE_BETWEEN_DATA);
//		} catch (InterruptedException e3) {
//			// TODO Auto-generated catch block
//			logger.info(Utils.getStringException(e3));
//		}
//		
//		
//		logger.debug("Ra- about to get currentStateOfCharge" );
//		double currentStateOfCharge = getCurrentStateOfCharge();
//		logger.debug("Ra- currentStateOfCharge" + currentStateOfCharge);
//		
//		try {
//			Thread.sleep(PAUSE_BETWEEN_DATA);
//		} catch (InterruptedException e3) {
//			// TODO Auto-generated catch block
//			logger.info(Utils.getStringException(e3));
//		}
//		
//		
//		
//		
//		logger.debug("Ra- about to get totalLoadAmpHoursForToday" );
//		double totalLoadAmpHoursForToday =  getTotalLoadAmpHoursForToday();
//		logger.debug("Ra- totalLoadAmpHoursForToday" + totalLoadAmpHoursForToday);
//		try {
//			Thread.sleep(PAUSE_BETWEEN_DATA);
//		} catch (InterruptedException e3) {
//			// TODO Auto-generated catch block
//			logger.info(Utils.getStringException(e3));
//		}
		
		
		
		double maxBatVoltageToday = 55;//getMaxBatVoltageToday();
		double minBatVoltageToday  = 46;//getMinBatVoltageToday();
		double totalChargeAmpHoursForToday = 10;//getTotalChargeAmpHoursForToday();
		double currentStateOfCharge = getCurrentStateOfCharge();
		double totalLoadAmpHoursForToday = 100;//getTotalLoadAmpHoursForToday();
		String prefix="";
		logger.info("about to return ");
		if(currentCommand.equals("AsyncData"))prefix =  "AsyncCycleUpdate#";
		dataLine =prefix + batteryVoltage+ "#"+ currentCharge + "#"+ currentLoad+ "#" + currentStateOfCharge + "#"+ batteryState + "#"+ totalLoadAmpHoursForToday+ "#"+ totalChargeAmpHoursForToday + "#"+ minBatVoltageToday +"#" + maxBatVoltageToday;
		
		logger.info("plseries returning " + dataLine);
		currentCommand="";
		return dataLine;
	}
	
	
	public double getCurrentVoltage() {
		try {
			logger.debug("about to getCurrentVoltage data, serialPortInputStream=" + serialPortInputStream);
			//
			// loopback
			/*
			serialPortOutputStream.write( 187 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 68 );
			*/
			// voltage for one plc
			//
			/*
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 50 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 235 );
			*/
			//
			// for pla
			//
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 50 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 74 );
			serialPortOutputStream.write( 4 );
			
			serialPortOutputStream.flush();
			
		    logger.debug("about to read data after sending");
			
			byte[] buffer = new byte[1024];
			logger.debug("point 3c");
			serialPortInputStream.read(buffer);
			int responseCode = convertByteToInt(buffer);
			logger.debug("getCurrentVoltage responseCode:" + responseCode);
        	double voltage=0;
        	if(responseCode==200){
	        	serialPortInputStream.read(buffer);
	        	int voltageUnprocessed = convertByteToInt(buffer);
	        	voltage = round(0.1*voltageUnprocessed*voltageSystemCorrectorFactor,2);
 			}else{
 				logger.debug("PLA-"+"getCurrentVoltage, returning 0 because response code was " + responseCode);
 				//logger.debug("getCurrentVoltage, returning 0 because response code was " + responseCode);
 				
 			}
        	 logger.debug("rutrn voltageg=" + voltage);
			return voltage;
		} catch( IOException e ) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.debug("PLA-"+sw.toString());
			//logger.debug(sw.toString());
		}
		return -1;
	}
	
	public String getBatteryState(){
		try {
			logger.debug("about to getBatteryState");
			
			//
			// for pla
			//
			
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 101 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 125 );
			serialPortOutputStream.write( 4);
			
			serialPortOutputStream.flush();
			
			
			byte[] buffer = new byte[2];
			logger.debug("line 284");
			serialPortInputStream.read(buffer);
			int responseCode = convertByteToInt(buffer);
        	logger.debug("getState responseCode:" + responseCode);
        	
        	
        	String status="";
        	if(responseCode==200){
        		serialPortInputStream.read(buffer);
            	byte returnData = buffer[1];
            	int result = buffer[0] & 3;
            	logger.debug("PLA-"+"getBatteryState= " +result);
            	
            	//logger.debug("buffer[0]=" + buffer[0]  + " getState " + result);
            	
            	switch(result){
            		case 0:
            			status=BOOST;
            			break;
            		case 1:
            			status=EQUALIZE;
            			break;
            		case 2:
            			status=ABSORTION;
            			break;
            		case 3:
            			status=FLOAT;
            			break;
            			
            	}
 			}else{
 				logger.debug("PLA-"+"getBatteryState, returning 0 because response code was " + responseCode);
 				//logger.debug("getBatteryState, returning NA because response code was " + responseCode);
 				status="NA";
 			}
 			////logger.debug("getCurrentCharge charge buffer+" + buffer + " chargeCurrentUnprocessed=" + loadCurrentUnprocessed +" current= " + loadCurrent);
        	logger.debug("PLA-"+"getBatteryState returning = " +status);
        	
        	return status;
		} catch( IOException e ) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.debug("PLA-"+sw.toString());
			//logger.debug(sw.toString());
		}
		return "";
	}
	
	public double getCurrentStateOfCharge(){
		try {
			////logger.debug("about to getCurrentLoad");
			
			//
			// for pla
			//
			
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 181 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 205 );
			serialPortOutputStream.write( 4);
			
			serialPortOutputStream.flush();
			
			
			byte[] buffer = new byte[1024];
			////logger.debug("point 3c");
			serialPortInputStream.read(buffer);
			int responseCode = convertByteToInt(buffer);
        	logger.debug("PLA-"+"getCurrentStateOfCharge responseCode:" + responseCode);
        	
			double loadCurrent=0;
        	if(responseCode==200){
        		serialPortInputStream.read(buffer);
            	double loadCurrentUnprocessed = convertByteToDouble(buffer);
            	logger.debug("PLA-"+"getCurrentStateOfCharge loadCurrentUnprocessed:" + loadCurrentUnprocessed);
            	loadCurrent = round(loadCurrentUnprocessed*chargeCurrentFactor,2);
 			}else{
 				logger.debug("PLA-"+"getCurrentStateOfCharge, returning 0 because response code was " + responseCode);
 				//logger.debug("getCurrentStateOfCharge, returning 0 because response code was " + responseCode);
 				
 			}
        	
        	////logger.debug("getCurrentCharge charge buffer+" + buffer + " chargeCurrentUnprocessed=" + loadCurrentUnprocessed +" current= " + loadCurrent);
			return loadCurrent;
		} catch( IOException e ) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.debug("PLA-"+sw.toString());
			//logger.debug(sw.toString());
		}
		return -1;
	}
	
	public double getCurrentLoad() {
		// TODO Auto-generated method stub
		try {
			////logger.debug("about to getCurrentLoad");
			
			//
			// for pla
			//
			
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 206 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 230 );
			serialPortOutputStream.write( 4);
			
			serialPortOutputStream.flush();
			
			
			byte[] buffer = new byte[1024];
			logger.debug("about to read new wy");
			//serialPortInputStream.read(buffer);
			
			
			int readCount = readInputStreamWithTimeout(serialPortInputStream, buffer, 6000);  // 6 second timeout
			logger.debug("readCount=" + readCount);
		
			
			
			int responseCode = convertByteToInt(buffer);
			logger.debug("getCurrentLoad responseCode:" + responseCode);
        	
			
			double loadCurrent=0;
			logger.debug("PLA-"+"getCurrentLoad responseCode=" + responseCode);
			
        	if(responseCode==200){
        		serialPortInputStream.read(buffer);
            	double loadCurrentUnprocessed = convertByteToDouble(buffer);
            	loadCurrent = round(loadCurrentUnprocessed*chargeCurrentFactor,2);
     			logger.debug("PLA-"+"getCurrentLoad charge buffer+" + buffer + " chargeCurrentUnprocessed=" + loadCurrentUnprocessed +" current= " + loadCurrent);
    			//logger.debug("getCurrentLoad charge buffer+" + buffer + " chargeCurrentUnprocessed=" + loadCurrentUnprocessed +" current= " + loadCurrent);
    	 	}else{
 				logger.debug("PLA-"+"getCurrentLoad, returning 0 because response code was " + responseCode);
 				//logger.debug("getCurrentLoad, returning 0 because response code was " + responseCode);	
 			}
        	return loadCurrent;
		} catch( IOException e ) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.debug("PLA-"+sw.toString());
			//logger.debug(sw.toString());
		}
		
		return -1;	}
	
	public double getCurrentCharge() {
		// TODO Auto-generated method stub
		
		try {
		//	//logger.debug("about to getCurrentCharge");
			
			//
			// for pla
			//
			
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 205 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 229 );
			serialPortOutputStream.write( 4);
			
			serialPortOutputStream.flush();
			
			
			byte[] buffer = new byte[1024];
			logger.debug("point 3c");
			serialPortInputStream.read(buffer);
			int responseCode = convertByteToInt(buffer);
        logger.debug("getCurrentCharge responseCode:" + responseCode);
			double chargeCurrent=0;
        	if(responseCode==200){
        		serialPortInputStream.read(buffer);
            	double chargeCurrentUnprocessed = convertByteToDouble(buffer);
            	chargeCurrent = round(chargeCurrentUnprocessed*chargeCurrentFactor,2);
     			logger.debug("getCurrentCharge charge buffer+" + buffer + " chargeCurrentUnprocessed=" + chargeCurrentUnprocessed +" current= " + chargeCurrent);
 			}else{
 				logger.debug("PLA-"+"getCurrentCharge, returning 0 because response code was " + responseCode);
 				logger.debug("getCurrentCharge, returning 0 because response code was " + responseCode);	
 			}
        	
        	return chargeCurrent;
		} catch( IOException e ) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.debug("PLA-"+sw.toString());
			//logger.debug(sw.toString());
		}
		
		return -1;
	}

	public double getTotalLoadAmpHoursForToday() {
		// TODO Auto-generated method stub
		try {
			////logger.debug("about to getTotalChargeAmpHoursForToday");
			
			//
			// for pla
			//
			/*
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 198 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 222 );
			serialPortOutputStream.write( 4);
			*/
			serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 203 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 227 );
			serialPortOutputStream.write( 4);
			
			serialPortOutputStream.flush();
			
			
			byte[] buffer = new byte[1024];
			////logger.debug("point 3c");
			serialPortInputStream.read(buffer);
			int responseCode = convertByteToInt(buffer);
        //	//logger.debug("getTotalChargeAmpHoursForToday responseCode:" + responseCode);
        	
        	serialPortInputStream.read(buffer);
        	double chargeLoadUnprocessed = convertByteToDouble(buffer);
        	//
        	// do the high byte
        	//
        	serialPortOutputStream.write( 20 );
			serialPortOutputStream.write( 204 );
			serialPortOutputStream.write( 0 );
			serialPortOutputStream.write( 228 );
			serialPortOutputStream.write( 4);
			
			serialPortOutputStream.flush();
			
			
			buffer = new byte[1024];
			////logger.debug("point 3c");
			serialPortInputStream.read(buffer);
			responseCode = convertByteToInt(buffer);
        //	//logger.debug("getTotalChargeAmpHoursForToday responseCode:" + responseCode);
        	
			double chargeLoadAmperHours=0;
        	if(responseCode==200){
        		serialPortInputStream.read(buffer);
            	double chargeLoadUnprocessedHighByte = convertByteToDouble(buffer);
            	
            	//logger.debug("load low byte:" + chargeLoadUnprocessed + " load high byte:" +  chargeLoadUnprocessedHighByte);
            	double totalEnergy = chargeLoadUnprocessed + chargeLoadUnprocessedHighByte*256;
     			chargeLoadAmperHours = round(totalEnergy*chargeCurrentFactor,2);
     		//	//logger.debug("getTotalChargeAmpHoursForToday charge buffer+" + buffer + " chargeCurrentUnprocessed=" + chargeCurrentUnprocessed +" chargeCurrentAmperHours= " + chargeCurrentAmperHours);
        	}else{
 				logger.debug("PLA-"+"getTotalLoadAmpHoursForToday, returning 0 because response code was " + responseCode);
 				//logger.debug("getTotalLoadAmpHoursForToday, returning 0 because response code was " + responseCode);	
 			}
        	return chargeLoadAmperHours;
		} catch( IOException e ) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			logger.debug("PLA-"+sw.toString());
			//logger.debug(sw.toString());
		}
		
		return -1;
	}

	
	public double getTotalChargeAmpHoursForToday() {
		// TODO Auto-generated method stub
		
				try {
					////logger.debug("about to getTotalChargeAmpHoursForToday");
					
					//
					// for pla
					// anything below 256 amp will be handled by the low byte
					// which has address of 188
					// anything above that will be handled by high byte 
					// which has address of 189
					//
					
					serialPortOutputStream.write( 20 );
					serialPortOutputStream.write( 188 );
					serialPortOutputStream.write( 0 );
					serialPortOutputStream.write( 212 );
					serialPortOutputStream.write( 4);
					
					serialPortOutputStream.flush();
					
					
					byte[] buffer = new byte[1024];
					////logger.debug("point 3c");
					serialPortInputStream.read(buffer);
					int responseCode = convertByteToInt(buffer);
		        //	//logger.debug("getTotalChargeAmpHoursForToday responseCode:" + responseCode);
		        	
		        	serialPortInputStream.read(buffer);
		        	double chargeCurrentUnprocessedLowByte = convertByteToDouble(buffer);
		        	//
		        	// do the high byte value
		        	//
		        	serialPortOutputStream.write( 20 );
					serialPortOutputStream.write( 189 );
					serialPortOutputStream.write( 0 );
					serialPortOutputStream.write( 213 );
					serialPortOutputStream.write( 4);
					
					serialPortOutputStream.flush();
					
					
					buffer = new byte[1024];
					////logger.debug("point 3c");
					serialPortInputStream.read(buffer);
					responseCode = convertByteToInt(buffer);
		        //	//logger.debug("getTotalChargeAmpHoursForToday responseCode:" + responseCode);
		        	
					double chargeCurrentAmperHours=0;
		        	if(responseCode==200){
		        		serialPortInputStream.read(buffer);
			        	double chargeCurrentUnprocessedHighByte = convertByteToDouble(buffer);
			        	
			        	//logger.debug("chargeCurrentUnprocessedLowByte=" + chargeCurrentUnprocessedLowByte + " chargeCurrentUnprocessedHighByte=" + chargeCurrentUnprocessedHighByte);
			        	double total = chargeCurrentUnprocessedLowByte + chargeCurrentUnprocessedHighByte*256;
			 			 chargeCurrentAmperHours = round(total*chargeCurrentFactor,2);
			 		//	//logger.debug("getTotalChargeAmpHoursForToday charge buffer+" + buffer + " chargeCurrentUnprocessed=" + chargeCurrentUnprocessed +" chargeCurrentAmperHours= " + chargeCurrentAmperHours);
		        	}else{
		 				logger.debug("PLA-"+"getTotalChargeAmpHoursForToday, returning 0 because response code was " + responseCode);
		 				//logger.debug("getTotalChargeAmpHoursForToday, returning 0 because response code was " + responseCode);	
		 			}
					return chargeCurrentAmperHours;
				} catch( IOException e ) {
					e.printStackTrace();
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					logger.debug("PLA-"+sw.toString());
					//logger.debug(sw.toString());
				}
				
				return -1;
	}

	public double getMaxBatVoltageToday() {
		// TODO Auto-generated method stub
		
				try {
				//	//logger.debug("about to getMaxBatVoltageToday");
					
					//
					// for pla
					//
					serialPortOutputStream.write( 20 );
					serialPortOutputStream.write( 182 );
					serialPortOutputStream.write( 0 );
					serialPortOutputStream.write( 206 );
					serialPortOutputStream.write( 4 );
					
					serialPortOutputStream.flush();
					
					
					byte[] buffer = new byte[1024];
					//logger.debug("point 3c maximum voltage");
					serialPortInputStream.read(buffer);
					int responseCode = convertByteToInt(buffer);
		        	//logger.debug("getMaxBatVoltageToday responseCode:" + responseCode);
		        	double voltage=0;
		        	if(responseCode==200){
		        		serialPortInputStream.read(buffer);
			        	int voltageUnprocessed = convertByteToInt(buffer);
			        	
			 			voltage = round(0.1*voltageUnprocessed*voltageSystemCorrectorFactor,2);
			 			//logger.debug("getMaxBatVoltageToday voltage= " + voltage);			 		//	//logger.debug("getTotalChargeAmpHoursForToday charge buffer+" + buffer + " chargeCurrentUnprocessed=" + chargeCurrentUnprocessed +" chargeCurrentAmperHours= " + chargeCurrentAmperHours);
		        	}else{
		 				logger.debug("PLA-"+"getMaxBatVoltageToday, returning 0 because response code was " + responseCode);
		 				//logger.debug("getMaxBatVoltageToday, returning 0 because response code was " + responseCode);	
		 			}
		        	
		        	
		 					        	
					return voltage;
				} catch( IOException e ) {
					e.printStackTrace();
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					logger.debug("PLA-"+sw.toString());
					//logger.debug(sw.toString());
				}
			
		return 0;
	}

	public double getMinBatVoltageToday() {
		// TODO Auto-generated method stub
		try {
			//	//logger.debug("about to getMaxBatVoltageToday");
				
				//
				// for pla
				//
				serialPortOutputStream.write( 20 );
				serialPortOutputStream.write( 183 );
				serialPortOutputStream.write( 0 );
				serialPortOutputStream.write( 207 );
				serialPortOutputStream.write( 4 );
				
				serialPortOutputStream.flush();
				
				
				byte[] buffer = new byte[1024];
				//logger.debug("point 3c min voltage");
				serialPortInputStream.read(buffer);
				int responseCode = convertByteToInt(buffer);
	        	//logger.debug("getMinBatVoltageToday responseCode:" + responseCode);
	        	double voltage=0;
	        	if(responseCode==200){
		        	serialPortInputStream.read(buffer);
		        	int voltageUnprocessed = convertByteToInt(buffer);
		        	voltage = round(0.1*voltageUnprocessed*voltageSystemCorrectorFactor,2);
		 			//logger.debug("getMinBatVoltageToday voltage= " + voltage);
	        	}else{
	 				logger.debug("PLA-"+"getMinBatVoltageToday, returning 0 because response code was " + responseCode);
	 				//logger.debug("getMinBatVoltageToday, returning 0 because response code was " + responseCode);	
	 			}
	        	
	        	
				return voltage;
			} catch( IOException e ) {
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				logger.debug("PLA-"+sw.toString());
				//logger.debug(sw.toString());
			}
		
	return 0;
	}

	
	public static int readInputStreamWithTimeout(InputStream is, byte[] b, int timeoutMillis)
		     throws IOException  {
		     int bufferOffset = 0;
		     long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
		     while (System.currentTimeMillis() < maxTimeMillis && bufferOffset < b.length) {
		         int readLength = java.lang.Math.min(is.available(),b.length-bufferOffset);
		         // can alternatively use bufferedReader, guarded by isReady():
		         int readResult = is.read(b, bufferOffset, readLength);
		         if (readResult == -1) break;
		         bufferOffset += readResult;
		     }
		     return bufferOffset;
		 }
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public int convertByteToInt(byte[] b)
	{           
		ByteBuffer buffer = ByteBuffer.wrap(b);
		buffer.order(ByteOrder.nativeOrder());  // if you want little-endian
		int result = buffer.getShort();  
		return result;
	}
	public double convertByteToDouble(byte[] b)
	{           
		ByteBuffer buffer = ByteBuffer.wrap(b);
		buffer.order(ByteOrder.nativeOrder());  // if you want little-endian
		double result = buffer.getShort();  
		return result;
	}
	
	
	
	
}
