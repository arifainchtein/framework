package com.teleonome.framework.microcontroller.pifourvaluesfourdigitdisplaysmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.LifeCycleEventListener;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherReader;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherWriter;
import com.teleonome.framework.utils.Utils;

public class PiFourValuesFourDigitDisplaysMicroController  extends MicroController implements LifeCycleEventListener{

	
	PiFourValuesFourDigitDisplaysWriter aPiFourValuesFourDigitDisplaysWriter;
	PiFourValuesFourDigitDisplaysReader aPiFourValuesFourDigitDisplaysReader;
	StringWriter sw = new StringWriter();
	
		String dgTeleonomeName="";
		Logger logger;
		JSONArray configParams;
		
		public PiFourValuesFourDigitDisplaysMicroController(DenomeManager d, String n) {
			super(d, n);
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub  
		}
		
		public void init(JSONArray c) throws MicrocontrollerCommunicationException {
			configParams=c;
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
			aPiFourValuesFourDigitDisplaysWriter = new PiFourValuesFourDigitDisplaysWriter(sw, configParams, aDenomeManager) ;
			aPiFourValuesFourDigitDisplaysReader = new PiFourValuesFourDigitDisplaysReader(new StringReader(""), aPiFourValuesFourDigitDisplaysWriter) ;
			
			  return aPiFourValuesFourDigitDisplaysWriter;
		}


		@Override
		public BufferedReader getReader() throws IOException {
			return aPiFourValuesFourDigitDisplaysReader;
		}

		@Override
		public void processLifeCycleEvent(String lifeCycleEvent) {
			// TODO Auto-generated method stub
			logger.debug("processing life cycle even=t=" + lifeCycleEvent);
			if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)) {
				String command = "neouart -i ff0000";
				try {
					ArrayList<String> results  = Utils.executeCommand(command);
					logger.debug("processing life cycle event start pulse produced=" + results);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				
			}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
				String command = "neouart -i 00ff00";
				try {
					ArrayList<String> results  = Utils.executeCommand(command);
					logger.debug("processing life cycle event start pulse produced=" + results);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}
		}
	}
