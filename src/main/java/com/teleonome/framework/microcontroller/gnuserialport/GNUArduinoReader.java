package com.teleonome.framework.microcontroller.gnuserialport;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.apache.commons.io.*;
import com.teleonome.framework.utils.Utils;

public class GNUArduinoReader  extends BufferedReader{
	Logger logger;
	BufferedReader reader;
	String command="";
	public GNUArduinoReader(BufferedReader in) {
		super(in);	
		reader=in;
		logger = Logger.getLogger(getClass());
		logger.debug("Just Created an GNUArduinoReader");
		
	}

	public void close() throws IOException {
		logger.info("about to close GNUArduinoReader");
		String trace = Utils.generateMethodTrace();
		logger.debug(trace);
		super.close();
	}
	public boolean ready() throws IOException {
		
		return reader.ready();
	}
	public void setCurrentCommand(String s) {
		command=s;
	}
	public String readLine() throws IOException{
		logger.debug("about to send readline, command:" + command);
		//if(command.equals(""))return "";
		
		    
		   String line = reader.readLine();
		    
		    
		
		logger.debug("the response is:   " + line);
		String cleaned="";
		if(line.contains("Ok-")) {
			cleaned=line.substring(line.indexOf("Ok-"));;
		}else if(line.contains("Read fail") && line.contains("#")){
			cleaned=line.substring(line.lastIndexOf("fail")+4);
		}else {
			cleaned=line;
		}
		logger.debug("cleaned:  " + cleaned);
		
		return cleaned;
	}
}
