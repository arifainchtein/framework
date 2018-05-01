package com.teleonome.framework.exception;

import java.util.Hashtable;

public class TeleonomeException extends Exception {
	
	Hashtable details;
	
	public TeleonomeException(Hashtable h){
		details=h;
	}
	
	public Hashtable getDetails(){
		return details;
	}

	public String getMessage(){
		return (String) details.get("message");
	}
}
