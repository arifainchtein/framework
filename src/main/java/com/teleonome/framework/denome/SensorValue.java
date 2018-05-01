package com.teleonome.framework.denome;

public class SensorValue {
	private String name="";
	private String unit="";
	private String port="";
	private String valueType="";
	private int positionInSerialString=0;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getValueType() {
		return valueType;
	}
	public void setValueType(String type) {
		this.valueType = type;
	}
	public int getPositionInSerialString() {
		return positionInSerialString;
	}
	public void setPositionInSerialString(int positionInSerialString) {
		this.positionInSerialString = positionInSerialString;
	}
	
	
	
	
}
