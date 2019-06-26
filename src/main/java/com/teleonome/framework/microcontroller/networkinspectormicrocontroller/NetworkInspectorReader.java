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
		String toReturn="-1#[]#{}#0#0#0";
		if(currentCommand.equals("GetSensorData")) {
			
			File file = new File("NetworkSensor.json");
			if(file.isFile()) {
				try {
					toReturn = FileUtils.readFileToString(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				if(toReturn==null || toReturn.split("#").length!=6) {
					toReturn ="-1#[]#{}#0#0#0";
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