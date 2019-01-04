package com.teleonome.framework.network;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
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
		////System.out.println("writing supplicant:" +supplicantFileContents.toString() );
		FileUtils.writeStringToFile(new File("/etc/wpa_supplicant/wpa_supplicant.network"), supplicantFileContents.toString());
	}
	
//	public static Object getNetworkingInfo() {
//		ArrayList initialData = Utils.executeCommand("ip a");
//		
//	}
	
	
	public static LinkedHashMap getConnectedClients() throws IOException, InterruptedException{
		ArrayList initialData = Utils.executeCommand("sudo hostapd_cli all_sta");
		//
		// look for dot11RSNAStatsSTAAddress
		String line;
		String[] tokens;
		ArrayList macAddress = new ArrayList();
		for(int i=0;i<initialData.size();i++){
			line = (String) initialData.get(i);
			//sud hostapd_cli all_sta//System.out.println("pint 1 line=" + line );	
			if(line.startsWith("dot11RSNAStatsSTAAddress")){
				tokens = line.split("=");
				////System.out.println("pint 1a tokens[1].trim()=" + tokens[1].trim() );	
				
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
			////System.out.println("pint 2 line=" + line );	
			
			name = line.substring(0,line.indexOf("(")).trim();
			ipaddress = line.substring(line.indexOf("(")+1, line.indexOf(")")).trim();
			macAdd = line.substring(line.indexOf(" at ")+3, line.indexOf(" [")).trim();
			////System.out.println("pint 2 name=" + name + " ipaddress=" + ipaddress + " macAdd=" + macAdd);	
			
			if(macAddress.contains(macAdd)){
			//	//System.out.println("pint 2a name=" + name + " ipaddress=" + ipaddress );	
				
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
	
	
//	public static JSONObject getNetworkInfo() throws SocketException, UnknownHostException{
//		NetworkInterface networkInterface;
//		InetAddress inetAddr, potential=null;
//		
//		//
//		// pay attention to the fact that if a teleonome has 2 network cards, 
//		// one as a host and one as part of a network, the inetAddress needs to belong
//		// to the network interface connected to the organism network, otherwise the exozero network
//		// will not receive the pulse
//		int numberOfNetworkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces()).size();
//		boolean hasEth0=false;
//		boolean hasWlan0=false;
//		boolean hasWlan1=false;
//		Hashtable interfacesNameIndex = new Hashtable();
//		
//		logger.info("numberOfNetworkInterfaces=" + numberOfNetworkInterfaces);
//		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
//			networkInterface  = enu.nextElement();
//			if(networkInterface.getDisplayName().equals("eth0")) {
//				hasEth0=true;
//				interfacesNameIndex.put("eth0", networkInterface);
//			}
//			else if(networkInterface.getDisplayName().equals("wlan0")) {
//				hasWlan0=true;
//				interfacesNameIndex.put("wlan0", networkInterface);
//			}
//			else if(networkInterface.getDisplayName().equals("wlan1")) {
//				hasWlan1=true;
//				interfacesNameIndex.put("wlan1", networkInterface);
//			}
//		}
//		//
//		// if eth0 is available return that
//		//
//		if(hasEth0) {
//			networkInterface = (NetworkInterface) interfacesNameIndex.get("eth0");
//			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
//				inetAddr = (InetAddress)ifaces.nextElement();
//				logger.info("inetAddr=" + inetAddr.getHostAddress());
//				if(!inetAddr.isLoopbackAddress() && !inetAddr.getHostAddress().equals("172.16.1.1")){
//					if(inetAddr.isSiteLocalAddress()){
//						return inetAddr.getHostAddress();
//					}
//				}
//			} 
//		}
//		//
//		// now check to see if we have one or two wifis,
//		// if so then wlan0 is the external facing
//		// and wlan1 is the internal wifi,
//		// therefore either way return wlan0
//		//
//		if(hasWlan0) {
//			networkInterface = (NetworkInterface) interfacesNameIndex.get("wlan0");
//			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
//				inetAddr = (InetAddress)ifaces.nextElement();
//				logger.info("inetAddr=" + inetAddr.getHostAddress());
//				if(!inetAddr.isLoopbackAddress() ){
//					if(inetAddr.isSiteLocalAddress()){
//						return inetAddr.getHostAddress();
//					}
//				}
//			}
//		}
//		return null;
//	}

	/**
	 * the address 172.16.1.1 (TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS) is hardwired as the address that the adas are going to connect to
	 * so make sure that you do not return this address, because this method is called to
	 * identify which network card will be bound to ZeroMQ network
	 */
	public static InetAddress getExoZeroNetworkAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(!inetAddr.getHostAddress().equals(TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS)) {
					if(!inetAddr.isLoopbackAddress()){
						if(inetAddr.isSiteLocalAddress()){
							return inetAddr;
						}else{
							potential=inetAddr;
						}
					}
				}
			}
		}
		return potential;
	}

	/**
	 * the address 172.16.1.1 (TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS) is hardwired as the address that the adas are going to connect to
	 * so make sure that you do not return this address, because this method is called to
	 * identify which network card will be bound to ZeroMQ network
	 */
	public static InetAddress getEndoZeroNetworkAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(inetAddr.getHostAddress().equals(TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS)) {
					if(!inetAddr.isLoopbackAddress()){
						if(inetAddr.isSiteLocalAddress()){
							return inetAddr;
						}else{
							potential=inetAddr;
						}
					}
				}
			}
		}
		return potential;
	}
	
	
	
	public static JSONObject getNetworkInterfaces() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		JSONObject toReturn = new JSONObject();
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			toReturn.put(networkInterface.getName(), "");
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(!inetAddr.isLoopbackAddress()){
					toReturn.put(networkInterface.getName(), inetAddr.getHostAddress());
				}
			}
		}
		return toReturn;
	}
	
	public static JSONObject getAvailableAdapters(){
		ArrayList result=new ArrayList();;
		JSONObject toReturn = new JSONObject();
		logger.debug("inside of getAvailableAdapters");
		try {
			//if(debug)//System.out.println("in getSSID, About to execute command");
			result = Utils.executeCommand( " netstat -i");
			logger.debug("result=" + result.toString());
			//
			// the return format is:
			
//			Kernel Interface table
//			Iface      MTU    RX-OK RX-ERR RX-DRP RX-OVR    TX-OK TX-ERR TX-DRP TX-OVR Flg
//			eth0      1500    53106      0      0 0         32538      0      0      0 BMRU
//			lo       65536    24306      0      0 0         24306      0      0      0 LRU
//			wlan0     1500        0      0   7024 0             0      0      0      0 BMU
//			wlan1     1500    19209      0      5 0         14125      0      0      0 BMRU
			
			// so ignore the first two rows and get the element of the first row
			// ignoring lo
			String line;
			String ipAddress,adapter;
			for(int j=2;j<result.size();j++){
				line = (String) result.get(j);
				logger.debug("line=" + line);
				adapter = line.split(" ")[0];
				if(!adapter.equals("lo")) {
					ipAddress=getIpAddressByInterfaceName(adapter);
					toReturn.put(adapter, ipAddress);
				}
				
			}
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			//System.out.println(Utils.getStringException(e));
		}
		
		return toReturn;
	}
	
	public static String getIpAddressByInterfaceName(String interfaceName) {
		
		String toReturn="";
		ArrayList result=new ArrayList();;
		try {
			//if(debug)//System.out.println("in getSSID, About to execute command");
			result = Utils.executeCommand( "ifconfig "+ interfaceName +" | grep 'inet ' | sed -e 's/:/ /' | awk '{print $2}'");
			if(result.size()>0) {
				toReturn = (String) result.get(0);
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			//System.out.println(Utils.getStringException(e));
		}
		return toReturn;
	}
	
	
	public static JSONArray getSSID(boolean debug){
		ArrayList result=new ArrayList();;
		try {
			//if(debug)//System.out.println("in getSSID, About to execute command");
			result = Utils.executeCommand( "sudo  iw dev wlan0 scan ap-force");
			//if(debug)//System.out.println("in getSSID, executed command, size="+ result.size());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			//System.out.println(Utils.getStringException(e));
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
		logger.debug("getssid returning =" + units.toString(4));
		return units;
	}
	
	public static boolean isNetworkStatusOk(){
		boolean networkStatusOk=true;
		ArrayList routeInfo;
		try {
			routeInfo = Utils.executeCommand("ip route show default 0.0.0.0/0");
			logger.debug("routeInfo=" + routeInfo);
			//
			// the answer will be one line like
			// 
			// default via 10.0.0.138 dev wlan0
			//
			// therefore parse it twice
			
			if(routeInfo.size()>0){
				String line =((String) routeInfo.get(0)).trim();
				String gatewayAddress = line.substring(12).split(" ")[0].trim();
				logger.debug("isNetworkStatusOk, gatewayAddress=" + gatewayAddress);
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
					logger.debug("isNetworkStatusOk, line=" + line);
					
					if(line.contains("Destination Host is Unreachable")){
						networkStatusOk=false;
						return networkStatusOk;
					}
				}
			}
			
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logger.debug("isNetworkStatusOk, returning=" + networkStatusOk);
		return networkStatusOk;
	}
}
