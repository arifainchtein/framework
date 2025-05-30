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
	
	public abstract void init(JSONArray params) throws MicrocontrollerCommunicationException;
	public abstract BufferedReader getReader() throws IOException;
	//public abstract InputStream getReader() throws IOException;
	public abstract BufferedWriter getWriter() throws IOException;
	
	
}
