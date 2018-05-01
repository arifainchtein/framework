package com.teleonome.framework.microcontroller.openweathermap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import com.teleonome.framework.microcontroller.csvmicrocontroller.CSVReader;

public class OpenWeatherMapWriter extends BufferedWriter{

	OpenWeatherMapReader anOpenWeatherMapReader;
	
	public OpenWeatherMapWriter(Writer out, OpenWeatherMapReader c) {
		super(out);
		anOpenWeatherMapReader=c;
		// TODO Auto-generated constructor stub
	}
	
public void write(String command, int off, int len) throws IOException {
		
		if(command.equals("GetSensorData")) {
			anOpenWeatherMapReader.setCurrentCommand(command);
		}
	}
}
