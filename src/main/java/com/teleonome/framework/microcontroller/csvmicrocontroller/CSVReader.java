package com.teleonome.framework.microcontroller.csvmicrocontroller;

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

public class CSVReader extends BufferedReader{

	StringWriter stringWriter;
	String fileName;
	Logger logger;
	File selectedFile;
	private String currentCommand="";
	
	public CSVReader(Reader in, StringWriter s, String f) {
		super(in);
		stringWriter=s;
		fileName=f;
		selectedFile = new File(fileName);
		logger = Logger.getLogger(getClass());
	}

	public void setCurrentCommand(String s) {
		currentCommand=s;
	}
	
	public String readLine(){
		logger.debug("currentCommand=" + currentCommand);
		if(!currentCommand.equals("GetSensorData"))return "Ok";
		//
		// read the last line of a file
	
		logger.debug("Entering read line of CSV Reader, fileName=" + fileName);
		//System.out.println("Entering read line of CSV Reader, fileName=" + fileName);
		
		String command="";
		String lastLine="";
		ReversedLinesFileReader r=null;
		try {
			//
			// read the last line of the file
			//
			if(selectedFile.exists()) {
			 r = new ReversedLinesFileReader(selectedFile);
			String line = r.readLine();
			logger.debug("selectedFile=" + selectedFile + " r=" + r + " line=" + line);
			//System.out.println("CSVReader selectedFile=" + selectedFile + " r=" + r + " line=" + line);
			//
			//the command request will invoke this method, jusrt return empty
			
			if(line!=null && !line.equals("")) {
				lastLine = line.replace(",", "#");
			}
			logger.debug("lastLine=" + lastLine);
			}else {
				logger.warn("selectedFile=" + selectedFile + " is missing");
			}
		}catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(r!=null) {
				try {
					r.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		//System.out.println("lastLine=" + lastLine);
		currentCommand="";
		return lastLine;

	}
}
