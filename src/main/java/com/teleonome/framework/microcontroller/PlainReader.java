package com.teleonome.framework.microcontroller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.log4j.Logger;

import com.teleonome.framework.TeleonomeConstants;

public class PlainReader extends BufferedReader{
	Logger logger;
	int counter=0;
	int maximum=2;
	int value=0;
	StringWriter stringWriter;
	String currentCommand="";
	public PlainReader(Reader in, StringWriter s) {
		super(in);
		logger = Logger.getLogger(getClass());
		stringWriter=s;
	}

	public void setCurrentCommand(String command) {
		// TODO Auto-generated method stub
		currentCommand=command;
		logger.debug("got command " + currentCommand);
	}

	public String readLine(){
		logger.debug("executing command " + currentCommand);
		if(currentCommand.contains("PulseStart")) {
			return "Ok-PulseStart";
		}else if(currentCommand.contains("PulseFinished")) {
			return "Ok-PulseFinished";
		}else if(currentCommand.contains("GetLifeCycleData")){
			return "Ok-GetLifeCycleData";
		}else {
			if(counter<maximum){
				counter++;
			}else{
				counter=0;
				if(value==0)value=1;
				else value=0;
			}
			//System.out.println("counter="+ counter + " value" + value);
			return "Ok";//value + "#";
		}
	}
}
