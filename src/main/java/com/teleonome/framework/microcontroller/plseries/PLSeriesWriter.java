package com.teleonome.framework.microcontroller.plseries;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class PLSeriesWriter  extends BufferedWriter{

	PLSeriesReader reader;
	String currentCommand="";
	public PLSeriesWriter(Writer out, PLSeriesReader r) {
		super(out);
		reader=r;
		// TODO Auto-generated constructor stub
	}

	public void setReader(PLSeriesReader r) {
		reader=r;
	}
	public void setCurrentCommand(String s) {
		currentCommand=s;
	}
	public String getCurrentCommand() {
		return currentCommand;
	}
	public void write(String command, int off, int len) throws IOException {
		
		setCurrentCommand(command);
		reader.setCurrentCommand(command);
		
		
	}
	
}
