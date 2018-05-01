package com.teleonome.framework.exception;

import java.util.Hashtable;

public class MicrocontrollerCommunicationException extends Exception {

	private Hashtable details;
	
	public MicrocontrollerCommunicationException(Hashtable h){
		details=h;
	}
}
