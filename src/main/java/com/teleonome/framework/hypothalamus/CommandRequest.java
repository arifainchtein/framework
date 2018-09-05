package com.teleonome.framework.hypothalamus;

import java.sql.Timestamp;

public class CommandRequest {
	private int id;
	private Timestamp createdOn;
	private Timestamp executedOn;
	private String command;
	private String status;
	private String dataPayload="";
	private String commandCode;
	private String clientIp;
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Timestamp getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}
	public Timestamp getExecutedOn() {
		return executedOn;
	}
	public void setExecutedOn(Timestamp executedOn) {
		this.executedOn = executedOn;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getDataPayload() {
		return dataPayload;
	}
	public void setDataPayload(String dataPayload) {
		this.dataPayload = dataPayload;
	}
	public String getCommandCode() {
		return commandCode;
	}
	public void setCommandCode(String commandCode) {
		this.commandCode = commandCode;
	}
	public String getClientIp() {
		return clientIp;
	}
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}




}


