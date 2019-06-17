package com.teleonome.framework.microcontroller.commandlinecontroller;



import java.io.BufferedReader;
import java.io.Reader;
import java.util.logging.Logger;

public class CommandLineReader extends BufferedReader{

	
	Logger logger;
	private String currentCommand="";
	public CommandLineReader(Reader in ) {
		super(in);
		logger = Logger.getLogger(getClass().getName());
		
		// TODO Auto-generated constructor stub
		
	}

	public String readLine(){
		logger.info("CommandLineReader currentCommand=" + currentCommand);
		if(!currentCommand.equals("GetSensorData"))return "Ok";
		return "";
	}


		public void setCurrentCommand(String command) {
			// TODO Auto-generated method stub
			currentCommand=command;
		}
	}