package com.teleonome.framework.microcontroller.networkinspectormicrocontroller;





import java.io.BufferedReader;
import java.io.Reader;
import java.util.logging.Logger;

public class NetworkInspectorReader extends BufferedReader{

	
	Logger logger;
	private String currentCommand="";
	public NetworkInspectorReader(Reader in ) {
		super(in);
		logger = Logger.getLogger(getClass().getName());
		
		// TODO Auto-generated constructor stub
		
	}

	public String readLine(){
		logger.info("NetworkInspectorReader currentCommand=" + currentCommand);
		if(!currentCommand.equals("GetSensorData"))return "Ok";
		return "";
	}


		public void setCurrentCommand(String command) {
			// TODO Auto-generated method stub
			currentCommand=command;
		}
	}