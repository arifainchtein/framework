package com.teleonome.framework.microcontroller.commandlinecontroller;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;

public class CommandLineController extends MicroController{
	StringWriter sw = new StringWriter();
	CommandLineWriter aCommandLineWriter;
	CommandLineReader aCommandLineReader;
	Logger logger;
	
	public CommandLineController(DenomeManager d, String n) {
		super(d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}

	
	
	/**
	 *  the config params is a JSONArray that contains complete Denes. Each one of this Denes contains the
	 *  following denewords names and values:
	 * 
	 *                                       
		"Value": 1,
        "Name": "Processing Request Queue Position",
   
        "Value":"@Icarus:Internal:Sensors:Pitia Status Value",
        "Name": "Sensor Value Pointer",
   
        "Value":2000,
        "Name": "Connection Timeout in Milliseconds",
   
        "Value":"@Icarus:Internal:Sensors:Pitia Status Value",
        "Name": "Sensor Value Pointer",
   
        "Value": "pitia.casete.com.mx",
        "Name": "Web Address", 
	 */
	@Override
	public void init(JSONArray p) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		try {
			logger.debug("initializing RaspberryPiCameraController " );
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		
		aCommandLineReader  =new CommandLineReader(new StringReader(""));
		aCommandLineWriter = new CommandLineWriter(sw,aCommandLineReader);
       	logger.info(" completed init for CommandLineController");
       	
	}
	
	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		aCommandLineReader  =new CommandLineReader(new StringReader(""));
		aCommandLineWriter = new CommandLineWriter(sw,aCommandLineReader);
	       
         return aCommandLineWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		String  dataString="dataString";
		return aCommandLineReader;
		
	}
}
