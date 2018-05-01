package com.teleonome.framework.microcontroller.gnuserialport;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class GNUArduinoUnoWriter extends BufferedWriter{

	GNUArduinoReader anArduinoUnoReader;
	
	public GNUArduinoUnoWriter(Writer out, GNUArduinoReader c) {
		super(out);
		anArduinoUnoReader=c;
		// TODO Auto-generated constructor stub
	}
	
public void write(String command, int off, int len) throws IOException {
		
		anArduinoUnoReader.setCurrentCommand(command);
		
	}
}