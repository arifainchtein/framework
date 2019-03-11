package com.teleonome.framework.microcontroller.pifourvaluesfourdigitdisplaysmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherReader;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherWriter;

public class PiFourValuesFourDigitDisplaysMicroController  extends MicroController{

	
	PiFourValuesFourDigitDisplaysWriter aPiFourValuesFourDigitDisplaysWriter;
	PiFourValuesFourDigitDisplaysReader aPiFourValuesFourDigitDisplaysReader;
	StringWriter sw = new StringWriter();
	
		String dgTeleonomeName="";
		Logger logger;
		
		public PiFourValuesFourDigitDisplaysMicroController(DenomeManager d, String n) {
			super(d, n);
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub  
		}
		
		public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
			
			//
			// configParams will contain 3 denes (plus a codon), which represents every display,
			// the three dnewords are:
			// Identity -  a pointer to where the data is stored
			// Clock pin:  the pin on the pi that represents the clock signal
			// Data pin: the pin on the pi that represents the data signal
			
			aPiFourValuesFourDigitDisplaysWriter = new PiFourValuesFourDigitDisplaysWriter(sw, configParams, aDenomeManager) ;
			aPiFourValuesFourDigitDisplaysReader = new PiFourValuesFourDigitDisplaysReader(new StringReader(""), aPiFourValuesFourDigitDisplaysWriter) ;
			 
	       	logger.info(" PiFourValuesFourDigitDisplaysMicroController  " );
	       	
		}
		
		@Override
		public BufferedWriter getWriter() throws IOException {
			// TODO Auto-generated method stub
			  return aPiFourValuesFourDigitDisplaysWriter;
		}


		@Override
		public BufferedReader getReader() throws IOException {
			return aPiFourValuesFourDigitDisplaysReader;
		}
	}
