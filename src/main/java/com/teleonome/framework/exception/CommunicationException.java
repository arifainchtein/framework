package com.teleonome.framework.exception;

import java.util.Hashtable;

public class CommunicationException extends Exception {

	private Hashtable details;
	
	public CommunicationException(Hashtable h){
		details=h;
	}
}
