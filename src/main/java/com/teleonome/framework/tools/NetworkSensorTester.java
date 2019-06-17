package com.teleonome.framework.tools;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.utils.Utils;

public class NetworkSensorTester {

	static String IP_ADDRESS="IP Address";
	static String DEVICE_NAME="Device Name";
	static DecimalFormat twoDecimalFormat = new DecimalFormat("0.00");
	public static void main(String[] args) {
		String sensorDataString;
		long startingTime = System.currentTimeMillis();
		int arpScanRetry=8;
		System.out.println("about to getArpScanInfo");	
		if(args.length>0 ) {
			startingTime = System.currentTimeMillis();
			arpScanRetry = Integer.parseInt(args[0]);
		}
		int numDevices, prevNumDevices=-1;
		JSONArray deviceListJSONArray, previousDeviceListJSONArray = null;
		String[] tokens;
		JSONObject diffAnalysisJSNArray = new JSONObject();
		while(true) {
			//
			// 1 Get the current device list
			//
			sensorDataString = performAnalysis(arpScanRetry);
			tokens =  sensorDataString.split("#");
			numDevices = Integer.parseInt(tokens[0]);
			deviceListJSONArray = new JSONArray(tokens[1]);
			System.out.println("Starting again at " + new Date());
			
			//
			// 2 perform the differential analysis
			//
			diffAnalysisJSNArray = new JSONObject();
			if(prevNumDevices==-1) {
				prevNumDevices = numDevices;
				previousDeviceListJSONArray = new JSONArray(tokens[1]);	
			}else {
				//
				//perform analysis
				diffAnalysisJSNArray = performDiffAnalaysis(previousDeviceListJSONArray, deviceListJSONArray);
				System.out.println(diffAnalysisJSNArray.toString(4));
				prevNumDevices = numDevices;
				previousDeviceListJSONArray = new JSONArray(tokens[1]);	
			}
			//
			// 3- get the connection speed
			//
			String command = "speedtest-cli --json";
			JSONObject connectionInfoJSONObject = null;
			double downloadSpeed=0,uploadSpeed=0, pingTime=0;
			try {
				ArrayList<String> results = Utils.executeCommand(command);
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
			
			//
			// generate the 	
			try {
				String finalSensorDataString =sensorDataString + "#" + diffAnalysisJSNArray.toString()+"#" + twoDecimalFormat.format(downloadSpeed)+"#"+twoDecimalFormat.format(uploadSpeed)+"#" +twoDecimalFormat.format(pingTime);
				System.out.println(finalSensorDataString);
				FileUtils.writeStringToFile(new File("NetworkSensor.json"), finalSensorDataString + System.lineSeparator() , true);
				System.out.println("about to sleep 60 at " + new Date());
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private static JSONObject performDiffAnalaysis(JSONArray previousDeviceListJSONArray,JSONArray deviceListJSONArray) {
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
		
		
		return toReturn;
	}
	public static String performAnalysis(int arpScanRetry) {
		long startingTime=System.currentTimeMillis();
		Hashtable<String, JSONObject> arpScanInfo =  getArpScanInfo(arpScanRetry);
		System.out.println("got getArpScanInfos");
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
		System.out.println("ArpScanInfo found # of devices=" + arpScanInfo.size());
		long totalTime = System.currentTimeMillis()-startingTime;
		System.out.println(" Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
		String getSensorDataString =  arpScanInfo.size() + "#"+ sensorDataStringJSONArray.toString();
		return getSensorDataString;
	}
	
	
	public static void main3(String[] args) {
		boolean detailed=false;
		if(args.length>0 && args[0].equals("-d")) detailed=true;
		
		long startingTime = System.currentTimeMillis();
		System.out.println("about to get adapters");
		JSONObject networkAdapterInfoJSONObject  = NetworkUtilities.getAvailableAdapters();
		Iterator it = networkAdapterInfoJSONObject.keys();
		String adapterName;
		String primaryIpAddress;
		String[] ipTokens;
		ArrayList<String> currentMasks = new ArrayList();
		String mask;
		System.out.println("got adapters");
		while(it.hasNext()) {
			adapterName = (String) it.next();
			primaryIpAddress=networkAdapterInfoJSONObject.getString(adapterName);
			ipTokens = primaryIpAddress.split(Pattern.quote("."));
			if(ipTokens.length>2) {
				//System.out.println("ipTokens=" + ipTokens.length + " adapterName=" + adapterName + " primaryIpAddress=" + primaryIpAddress);
				
				mask = ipTokens[0] + "." +ipTokens[1] + "." +ipTokens[2] + "." ; 
				if(!currentMasks.contains(mask)) {
					currentMasks.add(mask);
				}
			}else {
				//System.out.println("No INfo adapterName=" + adapterName + " primaryIpAddress=" + primaryIpAddress);
				
			}
		}
		String command ,aMask;
		ArrayList<String> nmapResults;
		String macAddress="", provider="";
		String nmapLine="", remainder="", deviceName = "", ipAddress="";
		JSONObject  deviceJSONObject = new JSONObject();
		JSONArray devices = new JSONArray();
		Hashtable nmapHashtable = new Hashtable();
		for (int i=0;i<currentMasks.size();i++) {
			 aMask = currentMasks.get(i);
			
			command = "sudo nmap -PR "+aMask+"1/24 -sn";
			System.out.println("About to execute nmap command for mask " + aMask);
			try {
				nmapResults = Utils.executeCommand(command);
				deviceName="";
				ipAddress="";
				macAddress="";
				provider="";
				//System.out.println("aMask=" + aMask + " command=" + command  + " nmapResults=" + nmapResults.size());
				 
				for(int j=0;j<nmapResults.size();j++) {
					nmapLine = nmapResults.get(j);
					
					if(nmapLine.startsWith("Nmap scan report for ")) {
						 deviceJSONObject = new JSONObject();
						 devices.put(deviceJSONObject);
						remainder = nmapLine.substring(21);
						if(remainder.contains("(")) {
							deviceName = remainder.substring(0,remainder.indexOf("(")).trim();
							ipAddress = remainder.substring(remainder.indexOf("(")+1, remainder.indexOf(")")).trim();
							deviceJSONObject.put(DEVICE_NAME,  deviceName);
							deviceJSONObject.put(IP_ADDRESS,  ipAddress.trim());
							nmapHashtable.put(ipAddress.trim(), deviceJSONObject);
						}else {
							deviceJSONObject.put(IP_ADDRESS,  remainder.trim());
							nmapHashtable.put(remainder.trim(), deviceJSONObject);
						}
						
						
					}else if(nmapLine.startsWith("MAC Address:")) {
						
						macAddress = nmapLine.substring(13, 31).trim();
						provider = nmapLine.substring(32).replace("(", "").replace(")", "");
						deviceJSONObject.put("MAC Address",  macAddress);
						deviceJSONObject.put("Provider",  provider);
					}
				}
				
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		Hashtable<String, JSONObject> arpScanInfo =  getArpScanInfo(8);
		
		
		
//		System.out.println("******************");
//		System.out.println("NMAP devices");
		
		for (int i=0;i<devices.length();i++) {
			deviceJSONObject = devices.getJSONObject(i);
			//System.out.println("deviceJSONObject=" + deviceJSONObject.toString(4));
			//System.out.println("DeviceName="+deviceJSONObject.get(DEVICE_NAME) + " ipAddress="+ deviceJSONObject.get("IP Address") + " macAddress=" + deviceJSONObject.get("MAC Address") + " provider="+ deviceJSONObject.get("Provider"));
		}
//		System.out.println("******************");
		
		
//		System.out.println("arp-scan devices");
		String arpScanIpAddress;
		JSONObject arpScanInfoJSONObject;
		for (Enumeration<String> en = arpScanInfo.keys();en.hasMoreElements();) {
			arpScanIpAddress = en.nextElement();
			arpScanInfoJSONObject = arpScanInfo.get(arpScanIpAddress);
			
		//	System.out.println("arpScanIp :" + arpScanInfoJSONObject.toString(4));
		}
		System.out.println("******************");
		JSONArray sensorDataStringJSONArray = new JSONArray();
		JSONObject sensorDataJSONObject;
		
		for (int i=0;i<devices.length();i++) {
			deviceJSONObject = devices.getJSONObject(i);
			if(!arpScanInfo.containsKey(deviceJSONObject.get(IP_ADDRESS))) {
				//System.out.println("arpScanInfo did not find " + deviceJSONObject.get(IP_ADDRESS));
				arpScanInfo.put(deviceJSONObject.getString(IP_ADDRESS), deviceJSONObject);
			}
		}
		
		
//		for (Enumeration<String> en = arpScanInfo.keys();en.hasMoreElements();) {
//			arpScanIpAddress = en.nextElement();
//			if(!nmapHashtable.containsKey(arpScanIpAddress)) {
//				System.out.println("namp did not find:" + arpScanIpAddress + " which arp-scan did find");
//			}
//		}
		JSONObject nmapDetail,infoObj;
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
			if(detailed) {
				nmapDetail = getDetailedNMAPInfo( arpScanIpAddress);
				infoObj.put("Details", nmapDetail);
			}
			System.out.println(infoObj.toString(4));
		}
		System.out.println("NMAP found # of devices=" + devices.length());
		System.out.println("ArpScanInfo found # of devices=" + arpScanInfo.size());
		System.out.println("returning info for " + arpScanInfo.size() + " devices");
		long totalTime = System.currentTimeMillis()-startingTime;
		System.out.println(" Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
		System.out.println( arpScanInfo.size() + "#"+ sensorDataStringJSONArray.toString());
	}
	
	public static Hashtable<String, JSONObject> getArpScanInfo(int arpScanRetry) {
		// TODO Auto-generated method stub
		boolean detailed=false;
		Hashtable<String, JSONObject> toReturn = new Hashtable();
		
		
		String command = "sudo arp-scan --retry="+ arpScanRetry+" --ignoredups --n -I wlan1 --localnet";
		long startingTime = System.currentTimeMillis();
		int deviceCount=0;
		JSONObject itemJSNObject;
		try {
			ArrayList<String> results = Utils.executeCommand(command);
			String[] tokens, portInfoTokens;
			ArrayList<String> nmapResults, getentResults;
			String ipAddress, macAddress, deviceName, nmapLine,remainder, port, state, service;
			boolean foundPortLine=false;
			for(int i=0;i<results.size();i++) {
				if(i<2) {
					//System.out.println(results.get(i));
				}else {
					//System.out.println(results.get(i));
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
		long totalTime = System.currentTimeMillis()-startingTime;
		//System.out.println("getArpScanInfo Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
		return toReturn;
	}
	
	public static JSONObject getDetailedNMAPInfo(String ipAddress) {
		// TODO Auto-generated method stub
		boolean detailed=false;
		long startingTime = System.currentTimeMillis();
		int deviceCount=0;
		JSONObject toReturn = new JSONObject();
		try {
			String[] tokens, portInfoTokens;
			ArrayList<String> nmapResults, getentResults;
			String  macAddress, deviceName, nmapLine,remainder, port, state, service;
			boolean foundPortLine=false;

			nmapResults = Utils.executeCommand("nmap " + ipAddress);
			foundPortLine=false;
			for(int j=0;j<nmapResults.size();j++) {
				nmapLine = nmapResults.get(j);
				if(nmapLine.startsWith("Nmap scan report for ")) {
					remainder = nmapLine.substring(21);
					if(remainder.contains("(")) {
						deviceName = remainder.substring(0,remainder.indexOf("(")).trim();
						//System.out.println("setting devicename by nmap " + deviceName);
					}
				}else if(nmapLine.startsWith("PORT")) {
					foundPortLine=true;
				}else if(nmapLine.startsWith("Nmap done")) {

				}else {
					if(foundPortLine) {
						//22/tcp open  ssh
						portInfoTokens = StringUtils.split(nmapLine);//.split("\\s+");
						if(portInfoTokens.length>2) {
							port = portInfoTokens[0].trim();
							state = portInfoTokens[1].trim();
							service = portInfoTokens[2].trim();
							//System.out.println("port=" + port + " state=" + state + " service=" + service);
							toReturn.put("Port", port);
							toReturn.put("State", state);
							toReturn.put("Service", service);
							
						}else {
							//System.out.println("portInfoTokens.length=" + portInfoTokens.length + "  nmapLine=" + nmapLine );
						}
					}
				}
			}
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println(Utils.getStringException(e));
		}
		long totalTime = System.currentTimeMillis()-startingTime;
		//System.out.println("Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
		return toReturn;
	}

	public static void main2(String[] args) {
		// TODO Auto-generated method stub
		boolean detailed=false;
		if(args.length>0 && args[0].equals("-d")) detailed=true;

		String command = "sudo arp-scan --retry=8 --ignoredups --n -I wlan1 --localnet";
		long startingTime = System.currentTimeMillis();
		int deviceCount=0;
		try {
			ArrayList<String> results = Utils.executeCommand(command);
			String[] tokens, portInfoTokens;
			ArrayList<String> nmapResults, getentResults;
			String ipAddress, macAddress, deviceName, nmapLine,remainder, port, state, service;
			boolean foundPortLine=false;
			for(int i=0;i<results.size();i++) {
				if(i<2) {
					System.out.println(results.get(i));
				}else {
					System.out.println(results.get(i));
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
						System.out.println("ipAddress=" + ipAddress + " macAddress=" + macAddress + " deviceName=" + deviceName);
						if(detailed) {
							nmapResults = Utils.executeCommand("nmap " + ipAddress);
							foundPortLine=false;
							for(int j=0;j<nmapResults.size();j++) {
								nmapLine = nmapResults.get(j);
								if(nmapLine.startsWith("Nmap scan report for ")) {
									remainder = nmapLine.substring(21);
									if(remainder.contains("(")) {
										deviceName = remainder.substring(0,remainder.indexOf("(")).trim();
										System.out.println("setting devicename by nmap " + deviceName);
									}
								}else if(nmapLine.startsWith("PORT")) {
									foundPortLine=true;
								}else if(nmapLine.startsWith("Nmap done")) {

								}else {
									if(foundPortLine) {
										//22/tcp open  ssh
										portInfoTokens = StringUtils.split(nmapLine);//.split("\\s+");
										if(portInfoTokens.length>2) {
											port = portInfoTokens[0].trim();
											state = portInfoTokens[1].trim();
											service = portInfoTokens[2].trim();
											System.out.println("port=" + port + " state=" + state + " service=" + service);
										}else {
											System.out.println("portInfoTokens.length=" + portInfoTokens.length + "  nmapLine=" + nmapLine );

										}

									}
								}

							}
						}
					}
				}
			}
			System.out.println("thre are " + deviceCount + " devices");
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println(Utils.getStringException(e));
		}
		long totalTime = System.currentTimeMillis()-startingTime;
		System.out.println("Total time=" + Utils.getElapsedTimeHoursMinutesSecondsString(totalTime));
	}

}
