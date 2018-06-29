package com.teleonome.framework.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.utils.Utils;

public class NetworkUtilities {

	 private static Logger logger = Logger.getLogger(com.teleonome.framework.network.NetworkUtilities.class);
	
	public static void createNetworkSupplicant(String ssid, String password) throws IOException{
		
		String newLine = System.getProperty("line.separator");
		StringBuffer supplicantFileContents = new StringBuffer("country=AU" + newLine);
		supplicantFileContents.append("ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev" + newLine);
		supplicantFileContents.append("update_config=1" + newLine);
		supplicantFileContents.append(newLine);
		supplicantFileContents.append("network={" + newLine);
		supplicantFileContents.append("ssid=\"" + ssid + "\"" + newLine);
		if(password.equals("")){
			supplicantFileContents.append("key_mgmt=NONE" + newLine);
		}else{
			supplicantFileContents.append("psk=\"" + password + "\"" + newLine);
		}
		supplicantFileContents.append("}" + newLine);		
		////logger.debug("writing supplicant:" +supplicantFileContents.toString() );
		FileUtils.writeStringToFile(new File("/etc/wpa_supplicant/wpa_supplicant.network"), supplicantFileContents.toString());
	}
	
	public static LinkedHashMap getConnectedClients() throws IOException, InterruptedException{
		ArrayList initialData = Utils.executeCommand("sudo hostapd_cli all_sta");
		//
		// look for dot11RSNAStatsSTAAddress
		String line;
		String[] tokens;
		ArrayList macAddress = new ArrayList();
		for(int i=0;i<initialData.size();i++){
			line = (String) initialData.get(i);
			//sud hostapd_cli all_sta//logger.debug("pint 1 line=" + line );	
			if(line.startsWith("dot11RSNAStatsSTAAddress")){
				tokens = line.split("=");
				////logger.debug("pint 1a tokens[1].trim()=" + tokens[1].trim() );	
				
				macAddress.add(tokens[1].trim());
			}
		}
		LinkedHashMap toReturn = new LinkedHashMap();
		ArrayList arpData = Utils.executeCommand("sudo arp -a");
		// first line is the header so skipt it
		// iPhone-2 (172.24.1.148) at 7c:fa:df:cc:f6:8a [ether] on wlan0
		String macAdd, name, ipaddress;
		for(int i=0;i<arpData.size();i++){
			line = (String) arpData.get(i);
			//
			// get the name
			//
			////logger.debug("pint 2 line=" + line );	
			
			name = line.substring(0,line.indexOf("(")).trim();
			ipaddress = line.substring(line.indexOf("(")+1, line.indexOf(")")).trim();
			macAdd = line.substring(line.indexOf(" at ")+3, line.indexOf(" [")).trim();
			////logger.debug("pint 2 name=" + name + " ipaddress=" + ipaddress + " macAdd=" + macAdd);	
			
			if(macAddress.contains(macAdd)){
			//	//logger.debug("pint 2a name=" + name + " ipaddress=" + ipaddress );	
				
				toReturn.put(name, ipaddress);
			}
		}
		return toReturn;
	}
	
	public static String getCurrentConnectedSSiD() {
		String toReturn="";
		try {
			ArrayList result = Utils.executeCommand( "sudo iwgetid -r");
			if(result!=null && result.size()>0)toReturn=(String) result.get(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toReturn;
	}
	
	public static JSONArray getSSID(boolean debug){
		ArrayList result=new ArrayList();;
		try {
			//if(debug)//logger.debug("in getSSID, About to execute command");
			result = Utils.executeCommand( "sudo  iw dev wlan0 scan ap-force");
			//if(debug)//logger.debug("in getSSID, executed command, size="+ result.size());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			//logger.debug(Utils.getStringException(e));
		}
		boolean foundTSF=false;
		boolean foundSSID=false;
		String signal;
		JSONArray units = new JSONArray();
		JSONObject ssidInfo = new JSONObject();
		String[] tokens;
		String line;
		for(int j=0;j<result.size();j++){
			line = (String) result.get(j);
			if(debug) {
				logger.debug("line=" + line);
			}
			if(line.contains("TSF:")){
				foundTSF=true;
//				if(ssidInfo.length()>0){
//					units.put(ssidInfo);
//				}
//				ssidInfo = new JSONObject();
				ssidInfo = new JSONObject();
				units.put(ssidInfo);
			}
			if(line.contains("signal:")){
				try {
					ssidInfo.put("Signal", line.substring(8));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if(line.contains("SSID:")){
				tokens = line.split(":");
				try {
					ssidInfo.put("SSID", tokens[1].trim());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if(line.contains("* Authentication suites:")){
				tokens = line.split(":");
				try {
					ssidInfo.put("Authentication", tokens[1].trim());
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(debug) {
				logger.debug("ssidInfo=" + ssidInfo.toString(4));
			}
		}
		return units;
	}
	
	public static boolean isNetworkStatusOk(){
		boolean networkStatusOk=true;
		ArrayList routeInfo;
		try {
			routeInfo = Utils.executeCommand("ip route show default 0.0.0.0/0");
			//
			// the answer will be one line like
			// 
			// default via 10.0.0.138 dev wlan0
			//
			// therefore parse it twice
			
			if(routeInfo.size()>0){
				String line =((String) routeInfo.get(0)).trim();
				String gatewayAddress = line.substring(12).split(" ")[0].trim();
				////logger.debug("isNetworkStatusOk, gatewayAddress=" + gatewayAddress);
				ArrayList pingInfo = Utils.executeCommand("ping " + gatewayAddress + " -c 3");
				//
				// the result will be 4 lines as follows if it can not find the gateway:
				//
				//PING 10.0.0.199 (10.0.0.199) 56(84) bytes of data.
				//From 10.0.0.38 icmp_seq=1 Destination Host Unreachable
				//		From 10.0.0.38 icmp_seq=2 Destination Host Unreachable
				//		From 10.0.0.38 icmp_seq=3 Destination Host Unreachable
				//
				// or like this is if everything is ok
				//
				//PING 10.0.0.138 (10.0.0.138) 56(84) bytes of data.
				//64 bytes from 10.0.0.138: icmp_req=1 ttl=64 time=1.12 ms
				//64 bytes from 10.0.0.138: icmp_req=2 ttl=64 time=0.786 ms
				//64 bytes from 10.0.0.138: icmp_req=3 ttl=64 time=0.822 ms
				
				//
				// skip the first line and check to see if the words Destination Host Unreachable
				// are present
				//
				for(int i=1;i<pingInfo.size();i++){
					line = (String) pingInfo.get(i);
					////logger.debug("isNetworkStatusOk, line=" + line);
					
					if(line.contains("Destination Host Unreachable")){
						networkStatusOk=false;
						return networkStatusOk;
					}
				}
			}
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return networkStatusOk;
	}
}
