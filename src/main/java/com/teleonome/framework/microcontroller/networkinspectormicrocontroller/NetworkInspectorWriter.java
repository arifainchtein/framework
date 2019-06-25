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
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.utils.Utils;

public class NetworkInspectorWriter  extends BufferedWriter{

	NetworkInspectorReader aNetworkInspectorReader;
	Logger logger;
	int arpScanRetry=8;
	String IP_ADDRESS="IP Address";
	String DEVICE_NAME="Device Name";
	DecimalFormat twoDecimalFormat = new DecimalFormat("0.00");

	public NetworkInspectorWriter(Writer out, NetworkInspectorReader c, int ap) {
		super(out);
		aNetworkInspectorReader=c;
		logger = Logger.getLogger(getClass());
		arpScanRetry=ap;
		// TODO Auto-generated constructor stub
	}

	public void write(String command, int off, int len) throws IOException {

		aNetworkInspectorReader.setCurrentCommand(command);
		if(command.equals("GenerateAnalysis")) {
			logger.debug("starting generate analysis thread");
			GenerateAnalysisThread a = new GenerateAnalysisThread();
			a.start();
		
		}
	}

	class GenerateAnalysisThread extends Thread{
		public void run() {
			String sensorDataString;
			long startingTime = System.currentTimeMillis();


			int numDevices, prevNumDevices=-1;
			JSONArray deviceListJSONArray, previousDeviceListJSONArray = null;
			String[] tokens;
			long arpScanDuration,  diffAnalysisDuration;
			JSONObject diffAnalysisJSNArray = new JSONObject();
			//
			// 1 Get the current device list
			//
			logger.debug("Starting again at " + new Date());
			startingTime = System.currentTimeMillis();
			sensorDataString = performAnalysis(arpScanRetry);
			tokens =  sensorDataString.split("#");
			numDevices = Integer.parseInt(tokens[0]);
			deviceListJSONArray = new JSONArray(tokens[1]);
			long arpScanEndTime= System.currentTimeMillis();
			arpScanDuration = arpScanEndTime - startingTime;
			//
			// 2 perform the differential analysis
			//
			diffAnalysisJSNArray = new JSONObject();
			logger.debug("prevNumDevices=" + prevNumDevices);
			if(prevNumDevices==-1) {
				prevNumDevices = numDevices;
				previousDeviceListJSONArray = new JSONArray(tokens[1]);	
			}else {
				//
				//perform analysis
				diffAnalysisJSNArray = performDiffAnalaysis(previousDeviceListJSONArray, deviceListJSONArray);
				logger.debug(diffAnalysisJSNArray.toString(4));
				prevNumDevices = numDevices;
				logger.debug("setting prevNumDevices=" + prevNumDevices);
				previousDeviceListJSONArray = new JSONArray(tokens[1]);	
			}
			long diffAnalysisEndTime= System.currentTimeMillis();
			diffAnalysisDuration = System.currentTimeMillis()- diffAnalysisEndTime;

			//
			// 3- get the connection speed
			//
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

			//
			// generate the 	
			try {
				long sampleTimeMillis = System.currentTimeMillis();
				SimpleDateFormat simpleFormatter = new SimpleDateFormat("dd/MM/yy HH:mm");
				String sampleTimeString = simpleFormatter.format(new Timestamp(sampleTimeMillis));
				String finalSensorDataString =sensorDataString + "#" + diffAnalysisJSNArray.toString()+"#" + twoDecimalFormat.format(downloadSpeed)+"#"+twoDecimalFormat.format(uploadSpeed)+"#" +twoDecimalFormat.format(pingTime) + "#" + sampleTimeMillis + "#" + sampleTimeString;
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
				previousDeviceName =  previousSensorDataJSONObject.getString(DEVICE_NAME);
				found=false;
				found:
					for(int j=0;j<deviceListJSONArray.length();j++) {
						currentSensorDataJSONObject = deviceListJSONArray.getJSONObject(j);
						currentDeviceName =  currentSensorDataJSONObject.getString(DEVICE_NAME);
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
				currentDeviceName =  currentSensorDataJSONObject.getString(DEVICE_NAME);
				found=false;
				found:
					for(int i=0;i<previousDeviceListJSONArray.length();i++) {
						previousSensorDataJSONObject = previousDeviceListJSONArray.getJSONObject(i);
						previousDeviceName =  previousSensorDataJSONObject.getString(DEVICE_NAME);
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
		private  String performAnalysis(int arpScanRetry) {
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
				sensorDataJSONObject.put(IP_ADDRESS, infoObj.getString(IP_ADDRESS));
				if(infoObj.has(DEVICE_NAME)) {
					sensorDataJSONObject.put(DEVICE_NAME, infoObj.getString(DEVICE_NAME));
				}else {
					sensorDataJSONObject.put(DEVICE_NAME, infoObj.getString(IP_ADDRESS));
				}
				sensorDataStringJSONArray.put(sensorDataJSONObject);
			}
			//System.out.println("ArpScanInfo found # of devices=" + arpScanInfo.size());
			long totalTime = System.currentTimeMillis()-startingTime;
			//System.out.println(" Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
			String getSensorDataString =  arpScanInfo.size() + "#"+ sensorDataStringJSONArray.toString();
			//logger.debug("getSensorDataString=" + getSensorDataString);
			
			return getSensorDataString;
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
								itemJSNObject.put(IP_ADDRESS, ipAddress.trim());
								itemJSNObject.put("MacAddress", macAddress);
								itemJSNObject.put(DEVICE_NAME, deviceName);
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
