package com.teleonome.framework.microcontroller.networkinspectormicrocontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.teleonome.framework.utils.Utils;

public class NetworkInspectorReader extends BufferedReader{

	Logger logger;
	private String currentCommand="";
	
	public NetworkInspectorReader(Reader in ) {
		super(in);
		logger = Logger.getLogger(getClass().getName());
	}

	public String readLine(){
		logger.info("NetworkInspectorReader currentCommand=" + currentCommand);
		String toReturn="Ok";
		if(!currentCommand.equals("GetSensorData")) {
			String sensorString="-1#[]#{}#0#0#0";
			try {
				toReturn = FileUtils.readFileToString(new File("NetworkSensor.json"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}else {
			toReturn= "Ok";	
		}
		return toReturn;
	}

	public void setCurrentCommand(String command) {
		// TODO Auto-generated method stub
		currentCommand=command;
	}
}