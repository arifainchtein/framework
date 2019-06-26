package com.teleonome.framework.microcontroller.networkinspectormicrocontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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
		long sampleTimeMillis = System.currentTimeMillis();
		SimpleDateFormat simpleFormatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		String sampleTimeString = simpleFormatter.format(new Timestamp(sampleTimeMillis));
		
		String toReturn="-1#[]#{}#0#0#0#" + sampleTimeMillis + "#" + sampleTimeString;
		
		if(currentCommand.equals("GetSensorData")) {
			
			File file = new File("NetworkSensor.json");
			if(file.isFile()) {
				try {
					toReturn = FileUtils.readFileToString(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				if(toReturn==null || toReturn.split("#").length!=8) {
					toReturn="-1#[]#{}#0#0#0#" + sampleTimeMillis + "#" + sampleTimeString;
				}
			}
			
		}else {
			toReturn= "Ok";	
		}
		//logger.debug("network info file=" + toReturn);
		return toReturn;
	}

	public void setCurrentCommand(String command) {
		// TODO Auto-generated method stub
		currentCommand=command;
	}
}