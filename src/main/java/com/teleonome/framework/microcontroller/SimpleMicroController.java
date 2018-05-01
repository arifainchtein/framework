package com.teleonome.framework.microcontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.utils.Utils;

public class SimpleMicroController extends MicroController {
	StringWriter sw = new StringWriter();
	String  dataString="dataString";
	PlainReader plainReader = new PlainReader(new StringReader(dataString), sw);
	
	
	public SimpleMicroController(DenomeManager d, String n) {
		super(d, n);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub

	}


	@Override
	public BufferedReader getReader() throws IOException {
		
		return  plainReader;
		
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		 
         BufferedWriter stringWriter = new BufferedWriter(sw) ;
         return stringWriter;

	}

}
