package com.teleonome.framework.microcontroller.networkinspectormicrocontroller;



import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.utils.Utils;


public class NetworkInspectorWriter  extends BufferedWriter{

	NetworkInspectorReader aNetworkInspectorReader;
	Logger logger;
	int arpScanRetry=8;
	boolean generatingAnalysis=false;
	
	DecimalFormat twoDecimalFormat = new DecimalFormat("0.00");
	MnemosyneManager aMnemosyneManager;
	
	public NetworkInspectorWriter(Writer out, NetworkInspectorReader c, int ap, MnemosyneManager m) {
		super(out);
		aNetworkInspectorReader=c;
		logger = Logger.getLogger(getClass());
		arpScanRetry=ap;
		aMnemosyneManager=m;
		// TODO Auto-generated constructor stub
	}

	public void write(String command, int off, int len) throws IOException {

		aNetworkInspectorReader.setCurrentCommand(command);
		if(command.equals("Generate Analysis")) {
			logger.debug("starting generate analysis thread");
			GenerateAnalysisThread a = new GenerateAnalysisThread();
			a.start();
		
		}else if(command.startsWith("AddDeviceToWhiteList")){
			String[] tokens = command.split("#");
			String deviceName=tokens[1];
			String deviceMacAddress=tokens[2];
			logger.debug("AddToWhiteList deviceName=" + deviceName + " " + deviceMacAddress);
			boolean b = aMnemosyneManager.addDeviceToWhiteList(deviceName, deviceMacAddress);
			logger.debug("AddToWhiteList deviceName=" + deviceName + " " + deviceMacAddress + " " + b);
			updateWhiteListStatusInFile( deviceName, true);			
			
		}else if(command.startsWith("RemoveDeviceFromWhiteList")){
			String[] tokens = command.split("#");
			String deviceName=tokens[1];
			logger.debug("RemoveDeviceFromWhiteList deviceName=" + deviceName);
			boolean b = aMnemosyneManager.removeDeviceFromWhiteList(deviceName);
			logger.debug("RemoveDeviceFromWhiteList deviceName=" + deviceName  + " " + b);
			
			updateWhiteListStatusInFile( deviceName, false);
		}
	}

	private void updateWhiteListStatusInFile(String deviceName, boolean status) throws IOException {
		String finalSensorDataString = FileUtils.readFileToString(new File("NetworkSensor.json"),Charset.defaultCharset());
		String[] sensorTokens = finalSensorDataString.split("#");
		JSONArray lastNetworkActivityJSONArray = new JSONArray(sensorTokens[2]);
		JSONObject anObject;
		found:
		for(int i=0;i<lastNetworkActivityJSONArray.length();i++) {
			anObject = lastNetworkActivityJSONArray.getJSONObject(i);
			if(deviceName.equals(anObject.getString(TeleonomeConstants.DEVICE_NAME))) {
				anObject.put(TeleonomeConstants.WHITE_LIST_STATUS, status);
				logger.debug("updateWhiteListStatusInFile=" + deviceName);
				break found;
			}
		}
		
		String newFinalSensorDataString =sensorTokens[0] + "#" + sensorTokens[1] + "#" + lastNetworkActivityJSONArray.toString() + "#" + sensorTokens[3]+"#" + sensorTokens[4]+"#"+sensorTokens[5]+"#" +sensorTokens[6] + "#" + sensorTokens[7] + "#" + sensorTokens[8];
		FileUtils.writeStringToFile(new File("NetworkSensor.json"), newFinalSensorDataString ,  Charset.defaultCharset(),false);
		
	}
	class GenerateAnalysisThread extends Thread{
		public void run() {
			
			long startingTime = System.currentTimeMillis();
			generatingAnalysis=true;

			int numDevices;
			JSONArray  previousDeviceListJSONArray = null;
			String[] tokens;
			long arpScanDuration,  diffAnalysisDuration;
			JSONObject diffAnalysisJSONObject = new JSONObject();
			//
			// Check if there is an analysis
			//
			String previousData = null;
			try {
				previousData = FileUtils.readFileToString(new File("NetworkSensor.json") ,  Charset.defaultCharset());
				tokens =  previousData.split("#");
				previousDeviceListJSONArray = new JSONArray(tokens[2]);
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));
			}
			
			
			
			//
			// 1 Get the current device list
			//
			logger.debug("Starting again at " + new Date());
			startingTime = System.currentTimeMillis();
			
			
			Hashtable<String, JSONObject> arpScanInfo =  getArpScanInfo(arpScanRetry);
			logger.debug("got getArpScanInfos, arpScanInfo=" + arpScanInfo.size());
			JSONObject nmapDetail,infoObj;
			JSONArray deviceListJSONArray = new JSONArray();
			JSONObject sensorDataJSONObject;
			String arpScanIpAddress;
			for (Enumeration<String> en = arpScanInfo.keys();en.hasMoreElements();) {
				arpScanIpAddress = en.nextElement();
				infoObj = arpScanInfo.get(arpScanIpAddress);

				sensorDataJSONObject = new JSONObject();
				sensorDataJSONObject.put(TeleonomeConstants.IP_ADDRESS, infoObj.getString(TeleonomeConstants.IP_ADDRESS));
				sensorDataJSONObject.put(TeleonomeConstants.MAC_ADDRESS, infoObj.getString(TeleonomeConstants.MAC_ADDRESS));
				
				if(infoObj.has(TeleonomeConstants.DEVICE_NAME)) {
					sensorDataJSONObject.put(TeleonomeConstants.DEVICE_NAME, infoObj.getString(TeleonomeConstants.DEVICE_NAME));
				}else {
					sensorDataJSONObject.put(TeleonomeConstants.DEVICE_NAME, infoObj.getString(TeleonomeConstants.IP_ADDRESS));
				}
				deviceListJSONArray.put(sensorDataJSONObject);
			}
			int numberOfDevices = deviceListJSONArray.length();
			
			
			
			
			
			long arpScanEndTime= System.currentTimeMillis();
			arpScanDuration = arpScanEndTime - startingTime;
			//
			// 2 perform the differential analysis
			//
			diffAnalysisJSONObject = new JSONObject();
			logger.debug("previousDeviceListJSONArray=" + previousDeviceListJSONArray);
			if(previousDeviceListJSONArray!=null) {
				//
				//perform analysis
				diffAnalysisJSONObject = performDiffAnalaysis(previousDeviceListJSONArray, deviceListJSONArray);
				logger.debug(diffAnalysisJSONObject.toString(4));
				previousDeviceListJSONArray = new JSONArray(deviceListJSONArray.toString());	
			}
			long diffAnalysisEndTime= System.currentTimeMillis();
			diffAnalysisDuration = System.currentTimeMillis()- diffAnalysisEndTime;

			//
			// 3- get the connection speed
			//
			logger.debug("about to execute speedtest-cli" );
			String speedtestCommand = "speedtest-cli --json";
			JSONObject connectionInfoJSONObject = null;
			double downloadSpeed=0,uploadSpeed=0, pingTime=0;
			try {
				ArrayList<String> results = Utils.executeCommand(speedtestCommand);
				//
				// only one line comes back and its a jsnobject
				//
				if(results.size()>0) {
					connectionInfoJSONObject = new JSONObject(results.get(0));
					downloadSpeed = connectionInfoJSONObject.getDouble("download")/(8*1024);
					uploadSpeed = connectionInfoJSONObject.getDouble("upload")/(8*1024);
					pingTime = connectionInfoJSONObject.getDouble("ping");

				}
			} catch (IOException | InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			long connectionSpeedDuration = System.currentTimeMillis()- diffAnalysisEndTime;
			logger.debug("connectionSpeedDuration=" +connectionSpeedDuration);
			
			logger.debug("Generating String " )
			//
			// generate the string
			try {
				long sampleTimeMillis = System.currentTimeMillis();
				SimpleDateFormat simpleFormatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
				String sampleTimeString = simpleFormatter.format(new Timestamp(sampleTimeMillis));
				aMnemosyneManager.storeNetworkStatus(deviceListJSONArray, diffAnalysisJSONObject, sampleTimeMillis , sampleTimeString);
				JSONArray lastNetworkActivityJSONArray = aMnemosyneManager.getLastNetworkSensorDeviceActivity();
				numberOfDevices = aMnemosyneManager.geNumberOfDevicesInLastSample();
				int numberUnknowDevices = aMnemosyneManager.geNumberUnknowDevicesInLastSample();
				String finalSensorDataString =numberOfDevices + "#" + numberUnknowDevices + "#" + lastNetworkActivityJSONArray.toString() + "#" + diffAnalysisJSONObject.toString()+"#" + twoDecimalFormat.format(downloadSpeed)+"#"+twoDecimalFormat.format(uploadSpeed)+"#" +twoDecimalFormat.format(pingTime) + "#" + sampleTimeMillis + "#" + sampleTimeString;
				logger.debug("finalSensorDataString=" +finalSensorDataString);
				
				
				FileUtils.writeStringToFile(new File("NetworkSensor.json"), finalSensorDataString ,  Charset.defaultCharset(),false);
				long totalTime = System.currentTimeMillis()-startingTime;

				logger.debug("arpScanDuration=" + Utils.getElapsedTimeHoursMinutesSecondsString(arpScanDuration));
				logger.debug("diffAnalysisDuration=" + Utils.getElapsedTimeHoursMinutesSecondsString(diffAnalysisDuration));
				logger.debug("connectionSpeedDuration=" + Utils.getElapsedTimeHoursMinutesSecondsString(connectionSpeedDuration));

				logger.debug(" Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));


			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			generatingAnalysis=false;
		}


		private  JSONObject performDiffAnalaysis(JSONArray previousDeviceListJSONArray,JSONArray deviceListJSONArray) {
			JSONObject toReturn = new JSONObject();
			JSONArray missingDevices = new JSONArray();
			JSONArray newDevices = new JSONArray();

			toReturn.put("Missing", missingDevices);
			toReturn.put("New", newDevices);

			//
			// first check those who were previous but are missing now
			//
			JSONObject previousSensorDataJSONObject,currentSensorDataJSONObject;
			String previousDeviceName, currentDeviceName;
			boolean found=false;
			for(int i=0;i<previousDeviceListJSONArray.length();i++) {
				previousSensorDataJSONObject = previousDeviceListJSONArray.getJSONObject(i);
				previousDeviceName =  previousSensorDataJSONObject.getString(TeleonomeConstants.DEVICE_NAME);
				found=false;
				found:
					for(int j=0;j<deviceListJSONArray.length();j++) {
						currentSensorDataJSONObject = deviceListJSONArray.getJSONObject(j);
						currentDeviceName =  currentSensorDataJSONObject.getString(TeleonomeConstants.DEVICE_NAME);
						if(currentDeviceName.equals(previousDeviceName)) {
							found=true;
							break found;
						}
					}
				if(!found) {
					missingDevices.put(previousDeviceName);
				}
			}

			//
			// now check those who were not in the previous but are in the current
			//
			for(int j=0;j<deviceListJSONArray.length();j++) {
				currentSensorDataJSONObject = deviceListJSONArray.getJSONObject(j);
				currentDeviceName =  currentSensorDataJSONObject.getString(TeleonomeConstants.DEVICE_NAME);
				found=false;
				found:
					for(int i=0;i<previousDeviceListJSONArray.length();i++) {
						previousSensorDataJSONObject = previousDeviceListJSONArray.getJSONObject(i);
						previousDeviceName =  previousSensorDataJSONObject.getString(TeleonomeConstants.DEVICE_NAME);
						if(currentDeviceName.equals(previousDeviceName)) {
							found=true;
							break found;
						}
					}
				if(!found) {
					newDevices.put(currentDeviceName);
				}

			}

			logger.debug("performdiffanalysis returning=" +toReturn.toString(4));

			return toReturn;
		}
		private  JSONArray performAnalysis(int arpScanRetry) {
			long startingTime=System.currentTimeMillis();
			Hashtable<String, JSONObject> arpScanInfo =  getArpScanInfo(arpScanRetry);
			logger.debug("got getArpScanInfos, arpScanInfo=" + arpScanInfo.size());
			JSONObject nmapDetail,infoObj;
			JSONArray sensorDataStringJSONArray = new JSONArray();
			JSONObject sensorDataJSONObject;
			String arpScanIpAddress;
			for (Enumeration<String> en = arpScanInfo.keys();en.hasMoreElements();) {
				arpScanIpAddress = en.nextElement();
				infoObj = arpScanInfo.get(arpScanIpAddress);

				sensorDataJSONObject = new JSONObject();
				sensorDataJSONObject.put(TeleonomeConstants.IP_ADDRESS, infoObj.getString(TeleonomeConstants.IP_ADDRESS));
				sensorDataJSONObject.put(TeleonomeConstants.MAC_ADDRESS, infoObj.getString(TeleonomeConstants.MAC_ADDRESS));
				if(infoObj.has(TeleonomeConstants.DEVICE_NAME)) {
					sensorDataJSONObject.put(TeleonomeConstants.DEVICE_NAME, infoObj.getString(TeleonomeConstants.DEVICE_NAME));
				}else {
					sensorDataJSONObject.put(TeleonomeConstants.DEVICE_NAME, infoObj.getString(TeleonomeConstants.IP_ADDRESS));
				}
				sensorDataStringJSONArray.put(sensorDataJSONObject);
			}
			//System.out.println("ArpScanInfo found # of devices=" + arpScanInfo.size());
			long totalTime = System.currentTimeMillis()-startingTime;
			//System.out.println(" Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
			//String getSensorDataString =  arpScanInfo.size() + "#"+ sensorDataStringJSONArray.toString();
			//logger.debug("getSensorDataString=" + getSensorDataString);
			
			return sensorDataStringJSONArray;
		}

		public Hashtable<String, JSONObject> getArpScanInfo(int arpScanRetry) {
			// TODO Auto-generated method stub
			long startingTime = System.currentTimeMillis();
			boolean detailed=false;
			Hashtable<String, JSONObject> toReturn = new Hashtable();

			// toReturn.put(adapter, ipAddress);
			JSONObject networkInfoJSONObject = NetworkUtilities.getAvailableAdapters();
			String scanningWlan="";
			if(networkInfoJSONObject.has("wlan0")) {
				if(!networkInfoJSONObject.getString("wlan0").equals(TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS)){
					scanningWlan = "wlan0";
				}
			}else if(networkInfoJSONObject.has("wlan1")) {
				if(!networkInfoJSONObject.getString("wlan1").equals(TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS)){
					scanningWlan ="wlan1";
				}
			}
			logger.debug("scanningWlan=" + scanningWlan);
			if(!scanningWlan.equals("")) {
				String command = "sudo arp-scan --retry="+ arpScanRetry+" --ignoredups --n -I "+ scanningWlan +" --localnet";
				
				int deviceCount=0;
				JSONObject itemJSNObject;
				try {
					ArrayList<String> results = Utils.executeCommand(command);
					logger.debug("results=" + results.toString());
					String[] tokens, portInfoTokens;
					ArrayList<String> nmapResults, getentResults;
					String ipAddress, macAddress, deviceName, nmapLine,remainder, port, state, service;
					boolean foundPortLine=false;
					for(int i=0;i<results.size();i++) {
						if(i<2) {
							//System.out.println(results.get(i));
						}else {
							logger.debug(results.get(i));
							tokens = results.get(i).split("\t");
							if(tokens.length>1) {
								ipAddress = tokens[0];
								macAddress = tokens[1];
								deviceName = tokens[2];
								getentResults = Utils.executeCommand("getent hosts " + ipAddress);
								if(getentResults.size()>0) {
									//System.out.println("getentResults.get(0)=" + getentResults.get(0));
									deviceName=getentResults.get(0).split("\\s+")[1];
								}
								deviceCount++;
								//System.out.println("ipAddress=" + ipAddress + " macAddress=" + macAddress + " deviceName=" + deviceName);
								itemJSNObject = new JSONObject();
								itemJSNObject.put(TeleonomeConstants.IP_ADDRESS, ipAddress.trim());
								itemJSNObject.put(TeleonomeConstants.MAC_ADDRESS, macAddress);
								itemJSNObject.put(TeleonomeConstants.DEVICE_NAME, deviceName);
								toReturn.put(ipAddress.trim(), itemJSNObject);

							}
						}
					}
					//System.out.println("thre are " + deviceCount + " devices");
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					System.out.println(Utils.getStringException(e));
				}
			}
			long totalTime = System.currentTimeMillis()-startingTime;
			//System.out.println("getArpScanInfo Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
			return toReturn;
		}
	}
}
