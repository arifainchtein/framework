package com.teleonome.framework.microcontroller.csvmicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class CSVWriter extends BufferedWriter{

	CSVReader aCSVReader;
	
	public CSVWriter(Writer out, CSVReader c) {
		super(out);
		aCSVReader=c;
		// TODO Auto-generated constructor stub
	}
	
public void write(String command, int off, int len) throws IOException {
		
		if(command.equals("GetSensorData")) {
			aCSVReader.setCurrentCommand(command);
		}
	}
}
