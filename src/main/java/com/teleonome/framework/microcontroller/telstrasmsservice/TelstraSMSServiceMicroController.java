package com.teleonome.framework.microcontroller.telstrasmsservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.plseries.PLSeriesReader;
import com.teleonome.framework.utils.Utils;

public class TelstraSMSServiceMicroController  extends MicroController {
	
	
	
	StringWriter sw = new StringWriter();
	String  dataString="dataString";
	PLSeriesReader aPLSeriesReader = null;//
	Logger logger;
	
	public TelstraSMSServiceMicroController(DenomeManager d, String n) {
		super(d, n);
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(getClass());
	}

	
	@Override
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
	}


	@Override
	public BufferedReader getReader() throws IOException {
		
		return  aPLSeriesReader;
		
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		 
         BufferedWriter stringWriter = new BufferedWriter(sw) ;
         return stringWriter;

	}
}
