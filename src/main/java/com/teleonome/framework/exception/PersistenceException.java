package com.teleonome.framework.exception;

import java.util.Hashtable;

public class PersistenceException extends Exception {

	private Hashtable details;
	
	public PersistenceException(Hashtable h){
		details=h;
	}
}
