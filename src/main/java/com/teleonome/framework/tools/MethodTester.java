package com.teleonome.framework.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.security.totp.TOTP;
import com.teleonome.framework.utils.Utils;

public class MethodTester {
	Logger logger;
	public MethodTester(){
		//boolean l = NetworkUtilities.isNetworkStatusOk();
		//System.out.println("NetworkStatus , l=" + l);
		//String url="http://tlaloc.chilhuacle.com";
		//pingURL(url,2000);
		//calculateSunrise();
		logger = Logger.getLogger(getClass());
		//String arg="MyLegoDoor";
		//String s= String.format("%040x", new BigInteger(1, arg.getBytes("UTF-8")));
//	System.out.println(s);
		//recognizeImages();generateTOTPString()
	
		TOTP totp = new TOTP();
		String code;
		try {
			code = totp.generateCurrentNumberFromUnencodedString("SecretCode");
			System.out.println("code = " + code);
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
	 
	 
	
	
	
	
	
	
	
		
		
		

	public void recognizeImages() {
		try {
			String path="/Users/arifainchtein/Data/Teleonome/Ra/ocr/";
			String formatedCurrentTime="29-06-2018_1339";
			String currentImageFileName="Selectronic_"+formatedCurrentTime + ".png";
			String currentStateOfChargeImageFileName="Selectronic_StateOfCharge_"+formatedCurrentTime + ".png";
			String currentBatteryVoltageImageFileName="Selectronic_Battery_Voltage_"+formatedCurrentTime + ".png";
			String currentACLoadImageFileName="Selectronic_AC_Load_"+formatedCurrentTime + ".png";
			String currentACLoadTodayImageFileName="Selectronic_AC_Load_Today_"+formatedCurrentTime + ".png";
			String currentBatteryPowerImageFileName="Selectronic_Battery_Power_"+formatedCurrentTime + ".png";
			String currentSolarChargeImageFileName="Selectronic_Solar_Charge_"+formatedCurrentTime + ".png";
			String currentBatteryNetImageFileName="Selectronic_Battery_Net_Today_"+formatedCurrentTime + ".png";
			String currentGeneratorStatusImageFileName="Selectronic_Generator_Status_"+formatedCurrentTime + ".png";
			String currentGeneratorStartedImageFileName="Selectronic_Generator_Started_"+formatedCurrentTime + ".png";
			
			
			//
			// code to extract and recognized the Battery Net Today
			//
			String batteryNetTodayImageName =  path + currentBatteryNetImageFileName;
			//
			// now process the image, first convert it to bw
			String command = "convert "+batteryNetTodayImageName+" -type bilevel SelectronicBatteryNetToday.png";
			ArrayList results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicBatteryNetToday.png ./SelectronicBatteryNetToday";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			String recognizedText = FileUtils.readFileToString(new File("SelectronicBatteryNetToday.txt"));
			logger.debug("SelectronicBatteryNetToday text=" + recognizedText);
			//
			// the recognized text is of the form -19.5 kwh
			//
			double recognisedBatteryNetToday = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recgnized SelectronicBatteryNetToday=" + recognisedBatteryNetToday);
			
			
			//
			// code to extract and recognized the Battery Net Today
			//
			String solarChargeImageFileName =  path + currentSolarChargeImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+solarChargeImageFileName+" -type bilevel SelectronicSolarCharge.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicSolarCharge.png ./SelectronicSolarCharge";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			 recognizedText = FileUtils.readFileToString(new File("SelectronicSolarCharge.txt"));
			logger.debug("SelectronicSolarCharge text=" + recognizedText);
			//
			// the recognized text is of the form -19.5 kwh
			//
			double recognisedSolarCharge = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recgnized recognisedSolarCharge=" + recognisedSolarCharge);
			
			
			//
			// code to extract and recognized the Battery Power
			//
			String batteryPowerImageFileName =  path + currentBatteryPowerImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+batteryPowerImageFileName+" -type bilevel SelectronicBatteryPower.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicBatteryPower.png ./SelectronicBatteryPower";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			 recognizedText = FileUtils.readFileToString(new File("SelectronicBatteryPower.txt"));
			logger.debug("SelectronicBatteryPower text=" + recognizedText);
			//
			// the recognized text is of the form -19.5 kwh
			//
			double recognisedBatteryPower = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recgnized recognisedBatteryPower=" + recognisedBatteryPower);
		
			
			//
			// code to extract and recognized the state of charge
			//
			String stateOfChargeImageName = path + currentStateOfChargeImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+stateOfChargeImageName+" -type bilevel SelectronicStateOfCharge.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicStateOfCharge.png ./SelectronicStateOfCharge";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			 recognizedText = FileUtils.readFileToString(new File("SelectronicStateOfCharge.txt"));
			logger.debug("recgnized text=" + recognizedText);
			//
			// the recognized text is of the form 524v
			//
			double recognisedStateOfCharge = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recognisedStateOfCharge text=" + recognisedStateOfCharge);
			
			
			
			//
			// code to extract and recognized the battery voltage
			//
			String batteryVoltageImageName = path + currentBatteryPowerImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+batteryVoltageImageName+" -type bilevel SelectronicBatteryVoltage.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicBatteryVoltage.png ./SelectronicBatteryVoltage";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			 recognizedText = FileUtils.readFileToString(new File("SelectronicBatteryVoltage.txt"));
			logger.debug("recgnized text=" + recognizedText);
			//
			// the recognized text is of the form 524v
			//
			double recognisedBatteryVoltage = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recognisedBatteryVoltage=" + recognisedBatteryVoltage);
			
			//
			// code to extract and recognized the ACLoadImageFileName
			//
			String acLoadImageFileName = path + currentACLoadImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+acLoadImageFileName+" -type bilevel SelectronicACLoad.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicACLoad.png ./SelectronicACLoad";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			 recognizedText = FileUtils.readFileToString(new File("SelectronicACLoad.txt"));
			logger.debug("recgnized text=" + recognizedText);
			//
			// the recognized text is of the form 524v
			//
			double recognisedACLoad = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recognisedACLoad=" + recognisedACLoad);
			


			//
			// code to extract and recognized the ACLoadTodayImageFileName
			//
			String acLoadTodayImageFileName = path + currentACLoadTodayImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+acLoadTodayImageFileName+" -type bilevel SelectronicACLoadToday.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicACLoadToday.png ./SelectronicACLoadToday";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			 recognizedText = FileUtils.readFileToString(new File("SelectronicACLoadToday.txt"));
			logger.debug("recgnized text=" + recognizedText);
			//
			// the recognized text is of the form 524v
			//
			double recognisedACLoadToday = Double.parseDouble(recognizedText.substring(0, 3))/10;
			logger.debug("recognisedACLoadToday=" + recognisedACLoadToday);
			
			
			
			
			
			//
			// code to extract and recognized the Generator Status
			//
			String generatorStatusImageFileName = path + currentGeneratorStatusImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+generatorStatusImageFileName+" -type bilevel SelectronicGeneratorStatus.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicGeneratorStatus.png ./SelectronicGeneratorStatus";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			String recognisedGeneratorStatus = FileUtils.readFileToString(new File("SelectronicGeneratorStatus.txt"));
			logger.debug("recgnized recognisedGeneratorStatus=" + recognisedGeneratorStatus);
			
			//
			// code to extract and recognized the Generator Started
			//
			String generatorStartedImageFileName = path + currentGeneratorStartedImageFileName;
			//
			// now process the image, first convert it to bw
			 command = "convert "+generatorStartedImageFileName+" -type bilevel SelectronicGeneratorStarted.png";
			 results = Utils.executeCommand(command);
			//
			// now recognized it
			command = "tesseract SelectronicGeneratorStarted.png ./SelectronicGeneratorStarted";
			results = Utils.executeCommand(command);
			// Now read the recognized file
			String recognisedGeneratorStarted = FileUtils.readFileToString(new File("SelectronicGeneratorStarted.txt"));
			logger.debug("recgnized recognisedGeneratorStarted=" + recognisedGeneratorStarted);
		}catch(IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public void listIP() throws SocketException {
			NetworkInterface networkInterface;
			InetAddress inetAddr, potential=null;
			
			for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
				networkInterface  = enu.nextElement();
				for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
					inetAddr = (InetAddress)ifaces.nextElement();
					if(!inetAddr.isLoopbackAddress())System.out.println("address " + inetAddr.getHostAddress() + " is not loopback");
					else System.out.println("address " + inetAddr.getHostAddress() + " is  loopback");
					if(inetAddr.isSiteLocalAddress())System.out.println("address " + inetAddr.getHostAddress() + " is not sitelocalAddress");
					else System.out.println("address " + inetAddr.getHostAddress() + " is sitelocalAddress");
				}
			}
	}
	
	
	
	public void generateMemoryStatusDene() {
		InputStream is=null;
		try {
			
			
			JSONObject memoryStatusDene = new JSONObject();
			memoryStatusDene.put("Name", TeleonomeConstants.DENE_SYSTEM_DATA);
			JSONArray memoryStatusDeneWords = new JSONArray();
			memoryStatusDene.put("DeneWords", memoryStatusDeneWords);
			
			
			
			JSONObject deneWord = null;
			
		
			
			
			int pacemakerPid = Integer.parseInt(FileUtils.readFileToString(new File("PaceMakerProcess.info")).split("@")[0]);
			int webPid = Integer.parseInt(FileUtils.readFileToString(new File("WebServerProcess.info")).split("@")[0]);
			int heartProcessInfo = Integer.parseInt(FileUtils.readFileToString(new File("heart/HeartProcess.info")).split("@")[0]);
			String[] cmdArray = { "top", "-n1","-b","-p"+ pacemakerPid , "-p" + webPid , "-p" + heartProcessInfo  };
			ProcessBuilder pb = new ProcessBuilder(cmdArray);
			pb.redirectError();
			
			    Process p = pb.start();
			    System.out.println ("method 1.1");
			    
			     is = p.getInputStream();
			    int value = -1;
			    StringBuffer buffer = new StringBuffer();
			    while ((value = is.read()) != -1) {
			    	buffer.append((char)value);
			    }
			    int exitCode = p.waitFor();
			    
			    
			    String[] lines = buffer.toString().split("\\r?\\n");
			    String[] splited;
			//    System.out.println("lines=" + lines.length);
			    for(int i=0;i<lines.length;i++) {
			    	System.out.println("1-line=" + lines[i]);
			    	switch(i) {
			    	case 0:
			    		//
			    		// top - 09:27:57 up 14:06,  4 users,  load average: 0.71, 0.58, 0.80
			    		//
			    		splited = lines[i].split(",");
			    		System.out.println("case0=");
			    		String theTime =splited[0].split("up")[0];
			    		String upTime =splited[0].split("up")[1];
			    		String numberUsers = splited[1];
			    		
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Time",""+theTime,null,"String",true);
						memoryStatusDeneWords.put(deneWord);
						
						deneWord = DenomeUtils.buildDeneWordJSONObject("Up Time",""+upTime,null,"String",true);
						memoryStatusDeneWords.put(deneWord);
						
						
			    		double loadAverage1Minute = Double.parseDouble(splited[2].split(":")[1]);
			    		double loadAverage5Minute = Double.parseDouble(splited[3]);
			    		double loadAverage15Minute = Double.parseDouble(splited[4]);
			    		
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Load Average 1 Minute",""+loadAverage1Minute,null,"double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Load Average 5 Minute",""+loadAverage5Minute,null,"double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Load Average 15 Minute",""+loadAverage15Minute,null,"double",true);
						memoryStatusDeneWords.put(deneWord);
			    		break;
			    	case 1:
			    		System.out.println("case1=");
			    		 // Tasks:   4 total,   0 running,   4 sleeping,   0 stopped,   0 zombie
			    		splited = lines[i].split(",");
					    		for(int j=0;j<splited.length;j++) {
					    	//		System.out.println("line " + j + " token " + j + " value=" + splited[j]);
					    		}
			    		break;
			    	case 2:
			    		// %Cpu(s): 22.7 us, 15.0 sy,  0.0 ni, 57.5 id,  3.5 wa,  0.0 hi,  1.2 si,  0.0 st
			    		
			    		splited = lines[i].split(":")[1].trim().split(",");
			    	//	System.out.println("ddd=" +  lines[i].split(":")[1]);//[0].split(" ")[0]);
			    	//	System.out.println("splited=" + splited.length);//[0].split(" ")[0]);
			    		
			    		double cpuUsageAsPercentByUser = Double.parseDouble(splited[0].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentBySystem = Double.parseDouble(splited[1].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentByLowPriorityServices = Double.parseDouble(splited[2].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentByIdleProcess = Double.parseDouble(splited[3].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentByIOWait = Double.parseDouble(splited[4].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentByHardwareInterrupt = Double.parseDouble(splited[5].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentBySoftwareInterrupts = Double.parseDouble(splited[6].trim().split("\\s+")[0]);
			    		double cpuUsageAsPercentByStealTime = Double.parseDouble(splited[7].trim().split("\\s+")[0]);
					   
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By User",""+cpuUsageAsPercentByUser,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By System",""+cpuUsageAsPercentBySystem,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Low Priority Services",""+cpuUsageAsPercentByLowPriorityServices,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Idle Process",""+cpuUsageAsPercentByIdleProcess,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By IOWait",""+cpuUsageAsPercentByIOWait,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Hardware Interrupt",""+cpuUsageAsPercentByHardwareInterrupt,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Cpu Usage As Percent By Software Interrupts",""+cpuUsageAsPercentBySoftwareInterrupts,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Steal Time",""+cpuUsageAsPercentByStealTime,"%","double",true);
						memoryStatusDeneWords.put(deneWord);
			    		break;
			    	case 3:
			    		//
			    		//KiB Mem:    380416 total,   364956 used,    15460 free,     5028 buffers
			    		splited = lines[i].split(":")[1].split(",");
			    		double totalSystemMemory = Double.parseDouble(splited[0].trim().split("\\s+")[0]);
			    		double freeSystemMemory = Double.parseDouble(splited[1].trim().split("\\s+")[0]);
			    		double memoryUsed = Double.parseDouble(splited[2].trim().split("\\s+")[0]);
			    		double bufferCache = Double.parseDouble(splited[3].trim().split("\\s+")[0]);
			    		
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Total System Memory",""+totalSystemMemory,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Free System Memory",""+freeSystemMemory,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Memory Used",""+memoryUsed,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Buffer Cache",""+bufferCache,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);

			    		break;
			    	case 4:
			    		//KiB Swap:   102396 total,      628 used,   101768 free.    97036 cached Mem
			    		splited = lines[i].split(":")[1].split(",");
			    		double totalSwapAvailable= Double.parseDouble(splited[0].trim().split(" ")[0]);
			    		double totalSwapUsed = Double.parseDouble(splited[1].trim().split(" ")[0]);
			    		//
			    		// there is a bug look at the period after the word free above
			    		// bviously they meant a , but put . so do something weird
			    		// to parse 
			    		// 101768 free.    97036 cached Mem
			    		//System.out.println("splited[2].trim()=" + splited[2].trim().split("\\s+")[2]);
			    		
			    		double totalSwapFree = Double.parseDouble(splited[2].trim().split("\\s+")[0].trim().split("\\s+")[0]);
			    		double cachedMemory = Double.parseDouble(splited[2].trim().split("\\s+")[2].trim().split("\\s+")[0]);
			    		
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Total Swap Available",""+totalSwapAvailable,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Total Swap Used",""+totalSwapUsed,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Total Swap Free",""+totalSwapFree,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Cached Memory",""+cachedMemory,"Kb","double",true);
						memoryStatusDeneWords.put(deneWord);
			    		break;
			    	case 5:
			    		// empty line
			    		break;
			    	case 6:
			    		// heading  PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND
			    		break;
			    	case 7:
			    	case 8:
			    	case 9:
			    		//24587 pi        20   0  171808  42972   5468 S  0.0 11.3   4:11.48 java
			    		splited = lines[i].trim().split("\\s+");
			    		int processId = Integer.parseInt(splited[0]);
			    		String processName="";
			    		if(processId==pacemakerPid) {
			    			processName  =TeleonomeConstants.PROCESS_HYPOTHALAMUS;
			    			
			    		}else if(processId==webPid) {
			    			processName  =TeleonomeConstants.PROCESS_HEART;
			    			
			    		}else if(processId==heartProcessInfo) {
			    			processName  =TeleonomeConstants.PROCESS_WEB_SERVER;
			    			
			    		}
			    		String user = splited[1];
			    		int priority = Integer.parseInt(splited[2]);
			    		int niceLevel = Integer.parseInt(splited[3]);
			    		int virtualMemoryUedByProcess = Integer.parseInt(splited[4]);
			    		int residentMemoryUedByProcess = Integer.parseInt(splited[5]);
			    		int shareableMemory = Integer.parseInt(splited[6]);
			    		String currentStatus = splited[7];
			    		double cpuUsedByProcessAsPercentage = Double.parseDouble(splited[8]);
			    		double memoryUsedByProcessAsPercentage = Double.parseDouble(splited[9]);
			    		String timeUsedByProcessAsPercentage = splited[10];
			    		String command =  splited[11];
			    		
			    		JSONObject processStatusDene = new JSONObject();
			    		processStatusDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_PROCESS_MEMORY_INFO);
			    		processStatusDene.put("Name", processName);
						JSONArray processStatusDeneWords = new JSONArray();
						processStatusDene.put("DeneWords", processStatusDeneWords);
								    		
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("User",""+user,null,"String",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Priority",""+priority,null,"int",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Nice Level",""+niceLevel,null,"",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Virtual Memory Used By Process",""+virtualMemoryUedByProcess,"Kb","int",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Resident Memory Used By Process",""+residentMemoryUedByProcess,"Kb","int",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Shareable Memory",""+shareableMemory,"Kb","int",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Used By Process As Percentage",""+cpuUsedByProcessAsPercentage,"Kb","double",true);
			    		processStatusDeneWords.put(deneWord);
			    		deneWord = DenomeUtils.buildDeneWordJSONObject("Time Used By Process As Percentage",""+timeUsedByProcessAsPercentage,"Kb","double",true);
			    		processStatusDeneWords.put(deneWord);
			    		
			    		System.out.println("processStatusDene=" + processStatusDene.toString(4));
			    		
			    		break;
			    	
			    	}
			    }
			    System.out.println("memoryStatusDene " + memoryStatusDene.toString(4) );

			    
			} catch (IOException exp) {
			    exp.printStackTrace();
			} catch (InterruptedException ex) {
			    //Logger.getLogger(JavaApplication256.class.getName()).log(Level.SEVERE, null, ex);
			}finally {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
	}
	
	private static void inheritIO(final InputStream src, final PrintStream dest) {
	    new Thread(new Runnable() {
	        public void run() {
	            Scanner sc = new Scanner(src);
	            while (sc.hasNextLine()) {
	                dest.println(sc.nextLine());
	            }
	            sc.close();
	        }
	    }).start();
	}
	public void calculateSunrise(){
		String timezone = "Australia/Melbourne";
		String latitude = "-37.133056";
		String longitude = "144.475";
		Location location = new Location(latitude, longitude);
		SunriseSunsetCalculator sunriseSunsetCalculator = new SunriseSunsetCalculator(location, timezone);
		TimeZone currentTimeZone = TimeZone.getTimeZone(timezone);
		Calendar officialSunset, officialSunrise;
		Calendar currentCalendar = Calendar.getInstance(currentTimeZone);
		currentCalendar.set(Calendar.YEAR, 2016);
		currentCalendar.set(Calendar.MONTH, 0);
		currentCalendar.set(Calendar.DATE, 1);
		DateFormat timeFormat = new SimpleDateFormat( "HH:mm");
		DateFormat dateFormat = new SimpleDateFormat( "dd/MM/yyy");
		long daylength;
		for(int i=1;i<366;i++){
			
			
			officialSunset = sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(currentCalendar);
			officialSunrise = sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(currentCalendar);
			daylength = officialSunset.getTimeInMillis()-officialSunrise.getTimeInMillis();
			
			System.out.println(dateFormat.format(currentCalendar.getTime()) + " " + timeFormat.format(officialSunrise.getTime()) + " " + timeFormat.format(officialSunset.getTime()) + " " + daylength + " " +  Utils.getElapsedTimeHoursMinutesSecondsString(daylength) );
			currentCalendar.add(Calendar.DATE, 1);
		}
	
	}
	
	boolean pingURL(String url, int timeout) {
	    url = url.replaceFirst("^https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

	    try {
	        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	        connection.setConnectTimeout(timeout);
	        connection.setReadTimeout(timeout);
	        connection.setRequestMethod("HEAD");
	        int responseCode = connection.getResponseCode();
	        return (200 <= responseCode && responseCode <= 399);
	    } catch (IOException exception) {
	        return false;
	    }
	}
	public void  MethodTester3(){
		try {
			LinkedHashMap l = NetworkUtilities.getConnectedClients();
			Set set = l.keySet();
			Iterator it = set.iterator();
			String name, ipaddress;
			while(it.hasNext()){
				name=(String)it.next();
				ipaddress = (String) l.get(name);
				System.out.println("name=" + name + "  ipaddress=" + ipaddress);		
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void  MethodTester2(){

		//
		// tp set not passowrd use
		//  set_network 0 key_mgmt NONE
		//
		Scanner scan=null;
		try {
			System.out.println("starting");
			ArrayList result;
			ArrayList listOfSSIDS;
			String line;
			boolean debug=false;
			//LinkedHashMap aps = Utils.getSSIDS();

			// using wpa_cli
			//result = executeCommand( "wpa_cli scan");
			//Thread.sleep(5000);
			//listOfSSIDS = executeCommand( "wpa_cli scan_results");

			//
			// using 
			result = executeCommand( "sudo  iw dev wlan0 scan ap-force");
			boolean foundTSF=false;
			boolean foundSSID=false;
			String signal;
			ArrayList units = new ArrayList();
			HashMap unitHashMap = new HashMap();
			String[] tokens;
			for(int j=0;j<result.size();j++){
				line = (String) result.get(j);
				//System.out.println("line=" + line);
				if(line.contains("TSF:")){
					foundTSF=true;
					if(unitHashMap.size()>0){
						units.add(unitHashMap);
					}
					unitHashMap = new HashMap();
				}
				if(line.contains("signal:")){
					unitHashMap.put("Signal", line.substring(8));
				}else if(line.contains("SSID:")){
					tokens = line.split(":");
					unitHashMap.put("SSID", tokens[1].trim());
				}else if(line.contains("* Authentication suites:")){
					tokens = line.split(":");
					unitHashMap.put("Authentication", tokens[1].trim());
				}
			}



			 scan= new Scanner(System.in);
			boolean goodPassword=false;
			String password="";
			String Id="";


			// Displaying elements of LinkedHashMap
			
			for(int i=0;i<units.size();i++) {
				unitHashMap = (HashMap)units.get(i);
				System.out.println("[" + i + "] " + unitHashMap.get("SSID") + " signal: "+unitHashMap.get("Signal"));
			}
			System.out.println(" ");



			boolean keepGoing=true;
			String selectedSSID="";
			String selectedLine="";
			boolean needPassword=false;
			String authentication;
			
			while(keepGoing){
				System.out.println("*** Select a number from above");
				char c = scan.next().charAt(0);
				int x = Character.getNumericValue(c);
				if(x>=0 && x<units.size()){
					unitHashMap = (HashMap)units.get(x);
					selectedSSID=(String) unitHashMap.get("SSID");
					authentication = (String) unitHashMap.get("Authentication");
					if(debug)System.out.println("selectedLine=" + selectedLine);
					if(authentication!=null){
						needPassword=true;
					}
					keepGoing=false;
				}
			}


			if(needPassword){
				System.out.println("*** input password:");
				password="";
				while(password.length()!=8){
					password=scan.nextLine();
					if(password.length()!=8){
						System.out.println(password + " is not 8 characters");
					}
				}


			}


			System.out.println(" ");

			System.out.println("Save Configuration? (y or n)");
			keepGoing=true;
			boolean saveConfiguration=false;
			while(keepGoing){
				String myLine = scan.nextLine();
				if(myLine.equals("y") || myLine.equals("n")){
					keepGoing=false;
					if(myLine.equals("y")){
						saveConfiguration=true;
					}
				}else{
					System.out.println("Save Configuration? (y or n)");
				}
			}
			if(saveConfiguration){
				String newLine = System.getProperty("line.separator");
				StringBuffer data = new StringBuffer();
				data.append("country=AU" + newLine);
				data.append("ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev" + newLine);
				data.append( newLine);
				data.append("network={" + newLine);
				data.append("ssid=\"" + selectedSSID + "\"" + newLine);
				if(needPassword){
					data.append("psk=\"" + password + "\"" + newLine);
				}else{
					data.append("key_mgmt=NONE" + newLine);
				}
				data.append("}" + newLine);
			
				FileUtils.write(new File("/etc/wpa_supplicant/wpa_supplicant.network"), data.toString());
			}
			
			System.out.println("Apply and Reboot? (y or n)");
			keepGoing=true;
			boolean applyAndReboot=false;
			while(keepGoing){
				String myLine = scan.nextLine();
				if(myLine.equals("y") || myLine.equals("n")){
					keepGoing=false;
					if(myLine.equals("y")){
						applyAndReboot=true;
					}
				}else{
					System.out.println("Apply and Reboot? (y or n))");
				}
			}
			
			if(applyAndReboot){
				executeCommand("sh networkmode.sh");
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			scan.close();
		}
	}



	private ArrayList executeCommand(String command) throws IOException, InterruptedException{
		Process process = Runtime.getRuntime().exec(new String[]{"bash","-c",command});

		//Process process =pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		ArrayList toReturn = new ArrayList();
		while ( (line = reader.readLine()) != null) {
			toReturn.add(line);
		}
		reader.close();
		process.waitFor();
		return toReturn;
	}


	public static void main(String[] args) {
		new MethodTester();
	}

}
