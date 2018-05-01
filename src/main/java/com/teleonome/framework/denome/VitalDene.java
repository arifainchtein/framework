package com.teleonome.framework.denome;

public class VitalDene extends Dene {

	private int basePulseFrequencySeconds=60;
	private int serialDataRate=9600;
	private String createBy="";
	private String contactInfo="";
	private String version="1.0";
			
	
	public VitalDene(){
		
	}


	public int getBasePulseFrequencySeconds() {
		return basePulseFrequencySeconds;
	}


	public void setBasePulseFrequencySeconds(int basePulseFrequencySeconds) {
		this.basePulseFrequencySeconds = basePulseFrequencySeconds;
	}


	public int getSerialDataRate() {
		return serialDataRate;
	}


	public void setSerialDataRate(int serialDataRate) {
		this.serialDataRate = serialDataRate;
	}


	public String getCreateBy() {
		return createBy;
	}


	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}


	public String getContactInfo() {
		return contactInfo;
	}


	public void setContactInfo(String contactInfo) {
		this.contactInfo = contactInfo;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}
	
	
}
