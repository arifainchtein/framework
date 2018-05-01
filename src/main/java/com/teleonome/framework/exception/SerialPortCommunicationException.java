package com.teleonome.framework.exception;

public class SerialPortCommunicationException extends Exception {

	String message="";
	public SerialPortCommunicationException(String s){
		message=s;
	}
}
