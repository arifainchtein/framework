package com.teleonome.framework.microcontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;

public abstract class MicroController {
	private String name;
	protected DenomeManager aDenomeManager; 
	protected boolean enableAsyncUpdate=false;
	protected int asyncRequestMillisecondsDelay=1000;
	protected boolean asyncContinuous=false;
	protected final Hypothalamus hypothalamus;
	
	
	public MicroController(Hypothalamus h, DenomeManager d, String n){
		aDenomeManager=d;
		name=n;
		hypothalamus=h;
	}
	
	public int getAsyncRequestMillisecondsDelay() {
		return asyncRequestMillisecondsDelay;
	}



	public void setAsyncRequestMillisecondsDelay(int asyncRequestMillisecondsDelay) {
		this.asyncRequestMillisecondsDelay = asyncRequestMillisecondsDelay;
	}

	public Hypothalamus getHypothalamus() {
		return hypothalamus;
	}


	
	
	
	
	public String getName(){
		return name;
	}
	
	public void setName(String n){
		name=n;
	}
	
	public boolean isEnableAsyncUpdate() {
		return enableAsyncUpdate;
	}
	
	public void setEnableAsyncUpdate(boolean b) {
		 enableAsyncUpdate=b;
	}

	public boolean isAsyncContinuous() {
		return asyncContinuous;
	}

	public void setAsyncContinuous(boolean b) {
		asyncContinuous=b;
	}
	
	public abstract void init(JSONArray params) throws MicrocontrollerCommunicationException;
	public abstract BufferedReader getReader() throws IOException;
	//public abstract InputStream getReader() throws IOException;
	public abstract BufferedWriter getWriter() throws IOException;

	/**
	 * Forces this microcontroller to drop and re-establish its underlying
	 * connection (e.g. closing and reopening a serial port), so a caller
	 * that has given up on a stuck/unresponsive connection (like
	 * MappedBusThread's async data poll) can ask for a fresh one instead of
	 * repeatedly retrying the same possibly-wedged connection. Default is a
	 * no-op, since most microcontrollers (HTTP/API-based, etc.) have no
	 * persistent connection to reset - override where a reconnect is
	 * meaningful (e.g. AnnabelleController).
	 */
	public void reconnect() throws IOException {
	}

}
