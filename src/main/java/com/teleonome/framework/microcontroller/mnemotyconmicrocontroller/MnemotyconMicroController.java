package com.teleonome.framework.microcontroller.mnemotyconmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.PlainReader;

public class MnemotyconMicroController extends MicroController {
			

			StringWriter sw = new StringWriter();
			String  dataString="dataString";
			PlainReader plainReader = new PlainReader(new StringReader(dataString), sw);
			
			
			public MnemotyconMicroController(DenomeManager d, String n) {
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
