package com.teleonome.framework.microcontroller.ledborgmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
public class LedborgMicroController  extends MicroController{
	public LedborgMicroController(Hypothalamus h,DenomeManager d, String n) {
		super(h,d, n);
		// TODO Auto-generated constructor stub
	}

	StringWriter sw = new StringWriter();
	LedborgWriter anLedborgWriter;
	LedborgReader anLedborgReader;
	private Logger logger;

	

	@Override
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		anLedborgWriter = new LedborgWriter(new StringWriter());
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return  new LedborgReader(new StringReader("")); 
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return anLedborgWriter;
	}

}
