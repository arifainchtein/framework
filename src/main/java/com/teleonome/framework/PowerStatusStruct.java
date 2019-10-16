package com.teleonome.framework;

public class PowerStatusStruct {
	long sampleTime ;
	float batteryVoltage ;
	int currentValue;
	float capacitorVoltage;
	byte internalBatteryStateOfCharge;
	float regulatorVoltage;
	int operatingStatus;
	float dailyMinBatteryVoltage;
	float dailyMaxBatteryVoltage;
	float dailyMinBatteryCurrent;
	float dailyMaxBatteryCurrent;
	float dailyBatteryOutEnergy;
	float dailyPoweredDownInLoopSeconds;
	float hourlyBatteryOutEnergy;
	float hourlyPoweredDownInLoopSeconds;
	long totalDiskUse;
	long totalDiskAvailable;
	byte pauseDuringWPS;
	
	public long getSampleTime() {
		return sampleTime;
	}
	public float getBatteryVoltage() {
		return batteryVoltage;
	}
	public int getCurrentValue() {
		return currentValue;
	}
	public float getCapacitorVoltage() {
		return capacitorVoltage;
	}
	public byte getInternalBatteryStateOfCharge() {
		return internalBatteryStateOfCharge;
	}
	public float getRegulatorVoltage() {
		return regulatorVoltage;
	}
	public int getOperatingStatus() {
		return operatingStatus;
	}
	public float getDailyMinBatteryVoltage() {
		return dailyMinBatteryVoltage;
	}
	public float getDailyMaxBatteryVoltage() {
		return dailyMaxBatteryVoltage;
	}
	public float getDailyMinBatteryCurrent() {
		return dailyMinBatteryCurrent;
	}
	public float getDailyMaxBatteryCurrent() {
		return dailyMaxBatteryCurrent;
	}
	public float getDailyBatteryOutEnergy() {
		return dailyBatteryOutEnergy;
	}
	public float getDailyPoweredDownInLoopSeconds() {
		return dailyPoweredDownInLoopSeconds;
	}
	public float getHourlyBatteryOutEnergy() {
		return hourlyBatteryOutEnergy;
	}
	public float getHourlyPoweredDownInLoopSeconds() {
		return hourlyPoweredDownInLoopSeconds;
	}
	public long getTotalDiskUse() {
		return totalDiskUse;
	}
	public long getTotalDiskAvailable() {
		return totalDiskAvailable;
	}
	public byte getPauseDuringWPS() {
		return pauseDuringWPS;
	}
	public void setSampleTime(long sampleTime) {
		this.sampleTime = sampleTime;
	}
	public void setBatteryVoltage(float batteryVoltage) {
		this.batteryVoltage = batteryVoltage;
	}
	public void setCurrentValue(int currentValue) {
		this.currentValue = currentValue;
	}
	public void setCapacitorVoltage(float capacitorVoltage) {
		this.capacitorVoltage = capacitorVoltage;
	}
	public void setInternalBatteryStateOfCharge(byte internalBatteryStateOfCharge) {
		this.internalBatteryStateOfCharge = internalBatteryStateOfCharge;
	}
	public void setRegulatorVoltage(float regulatorVoltage) {
		this.regulatorVoltage = regulatorVoltage;
	}
	public void setOperatingStatus(int operatingStatus) {
		this.operatingStatus = operatingStatus;
	}
	public void setDailyMinBatteryVoltage(float dailyMinBatteryVoltage) {
		this.dailyMinBatteryVoltage = dailyMinBatteryVoltage;
	}
	public void setDailyMaxBatteryVoltage(float dailyMaxBatteryVoltage) {
		this.dailyMaxBatteryVoltage = dailyMaxBatteryVoltage;
	}
	public void setDailyMinBatteryCurrent(float dailyMinBatteryCurrent) {
		this.dailyMinBatteryCurrent = dailyMinBatteryCurrent;
	}
	public void setDailyMaxBatteryCurrent(float dailyMaxBatteryCurrent) {
		this.dailyMaxBatteryCurrent = dailyMaxBatteryCurrent;
	}
	public void setDailyBatteryOutEnergy(float dailyBatteryOutEnergy) {
		this.dailyBatteryOutEnergy = dailyBatteryOutEnergy;
	}
	public void setDailyPoweredDownInLoopSeconds(float dailyPoweredDownInLoopSeconds) {
		this.dailyPoweredDownInLoopSeconds = dailyPoweredDownInLoopSeconds;
	}
	public void setHourlyBatteryOutEnergy(float hourlyBatteryOutEnergy) {
		this.hourlyBatteryOutEnergy = hourlyBatteryOutEnergy;
	}
	public void setHourlyPoweredDownInLoopSeconds(float hourlyPoweredDownInLoopSeconds) {
		this.hourlyPoweredDownInLoopSeconds = hourlyPoweredDownInLoopSeconds;
	}
	public void setTotalDiskUse(long totalDiskUse) {
		this.totalDiskUse = totalDiskUse;
	}
	public void setTotalDiskAvailable(long totalDiskAvailable) {
		this.totalDiskAvailable = totalDiskAvailable;
	}
	public void setPauseDuringWPS(byte pauseDuringWPS) {
		this.pauseDuringWPS = pauseDuringWPS;
	}
}
