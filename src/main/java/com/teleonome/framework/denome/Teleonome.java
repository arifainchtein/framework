 package com.teleonome.framework.denome;

import java.io.Serializable;

public  class Teleonome implements Serializable{

	private String name="";
	private boolean active=false;
	private String inetAddressString="";
	private String networkName="";
	
	public static final long serialVersionUID=1380208644690L;
	
	public Teleonome(){
	}
		
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getInetAddressString() {
		return inetAddressString;
	}

	public void setInetAddressString(String inetAddressString) {
		this.inetAddressString = inetAddressString;
	}

	public String getNetworkName() {
		return networkName;
	}

	public void setNetworkName(String networkName) {
		this.networkName = networkName;
	}

	

}