package com.teleonome.framework.microcontroller.internetaudiomicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherReader;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherWriter;
import com.teleonome.framework.utils.Utils;


public class InternetAudioMicroController extends MicroController{
	InternetAudioWriter anInternetAudioWriter;
	InternetAudioReader anInternetAudioReader;
	String dgTeleonomeName="";
	Logger logger;
	NetworkThread networkThread;
	public InternetAudioMicroController(Hypothalamus h,DenomeManager d, String n) {
		super(h,d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub  
	}
	
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		//
		// Start the Thread that will communicate with the api server
		//
		try {
            networkThread = new NetworkThread(new URI("localhost:24879"));
        } catch (URISyntaxException ex) {
           Utils.getStringException(ex);
        }
		
		anInternetAudioWriter = new InternetAudioWriter(networkThread, new StringWriter(), configParams, aDenomeManager) ;
		anInternetAudioReader = new InternetAudioReader(new StringReader(""), anInternetAudioWriter) ;
		 
       	logger.info(" InternetAudioMicroController microcontroller dgTeleonomeName " + dgTeleonomeName);
       	
	}
	
	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		  return anInternetAudioWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		return anInternetAudioReader;
	}

}