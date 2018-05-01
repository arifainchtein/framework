package com.teleonome.framework.process;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import com.teleonome.framework.utils.Utils;


public class DiscoverTeleonoms extends Thread {

	private int discoveryFrequency=60;
	private Hashtable knownTeleonoms = new Hashtable();
	private Hashtable presentTeleonoms = new Hashtable();
	private Hashtable notPresentTeleonoms = new Hashtable();
	
	static JmDNS bonjourService=null;
	//String bonjourServiceType = "_workstation._tcp.local.";
	String bonjourServiceType = "_teleonome._tcp.local.";
	Logger logger=null;
	
	public DiscoverTeleonoms() {
		logger = Logger.getLogger(getClass());
	}
	public void setDiscoveryFrequency(int i){
		discoveryFrequency=i;
	}
	
	public int setDiscoveryFrequency(){
		return discoveryFrequency;
	}
	
	public Hashtable getKnownTeleonoms(){
		return knownTeleonoms;
	}
	
	public Hashtable getPresentTeleonoms(){
		return presentTeleonoms;
	}
	
	public Hashtable getNotPresentTeleonoms(){
		return notPresentTeleonoms;
	}
	
	public void run(){
		while(true){
					
			try {
				InetAddress ipToBindToZeroMQ = Utils.getIpAddressForNetworkMode();
				bonjourService = JmDNS.create(ipToBindToZeroMQ,null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); 
			}
			bonjourService.addServiceListener(bonjourServiceType, new SampleListener());
			ServiceInfo[] serviceInfos = bonjourService.list(bonjourServiceType);
			Inet4Address[] addresses;
			Inet4Address address;
			String name="";
			presentTeleonoms = new Hashtable();
			for (ServiceInfo info : serviceInfos) {
				logger.debug("DiscoverTeleonoms  resolve service " + info.getName()  + " : " + info.getURL());
			  addresses = info.getInet4Addresses();
			  for(int i=0;i<addresses.length;i++){
				  address = addresses[i];
				  name = info.getName().split("\\[")[0].trim();
				 logger.debug("DiscoverTeleonoms hostName" + name  + ", address: " + address.getHostAddress());
				  knownTeleonoms.put(name, address.getHostAddress());
				  presentTeleonoms.put(name, address.getHostAddress());
			  }
			}
			//
			// now populate the notPresentTeleonoms
			notPresentTeleonoms = new Hashtable();
			for(Enumeration en= knownTeleonoms.keys();en.hasMoreElements();){
				name = (String)en.nextElement();
				if(presentTeleonoms.get(name)==null){
					notPresentTeleonoms.put(name, knownTeleonoms.get(name));
				}
				
			}
			try {
				bonjourService.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			 
	        
			
			try {
				logger.debug("DiscoverTeleonoms about to sleep for " +discoveryFrequency + " seconds");
				Thread.sleep(discoveryFrequency*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public InetAddress getIpAddress1() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(!inetAddr.isLoopbackAddress()){
					if(inetAddr.isSiteLocalAddress()){
						return inetAddr;
					}else{
						potential=inetAddr;
					}
				}
				
			}
		}
		return potential;
	}

	 static class SampleListener implements ServiceListener {
	        @Override
	        public void serviceAdded(ServiceEvent event) {
	         //  System.out.println("Service added   : " + event.getName() + "." + event.getType());
	            bonjourService.requestServiceInfo(event.getType(), event.getName());
	            
	        }

	        @Override
	        public void serviceRemoved(ServiceEvent event) {
	          //  System.out.println("Service removed : " + event.getName() + "." + event.getType());
	        }

	        @Override
	        public void serviceResolved(ServiceEvent ev) {
	        	// System.out.println("Service resolved: "
	        	//           + ev.getInfo().getQualifiedName()
	        	 //       + " port:" + ev.getInfo().getPort());
	        	        
	        }
	    }
	 
}
