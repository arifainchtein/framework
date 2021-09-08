package com.teleonome.framework.microcontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

public class PlainWriter extends BufferedWriter{
	Logger logger;
	PlainReader plainReader;
	
	public PlainWriter(Writer out, PlainReader p) {
		super(out);
		logger = Logger.getLogger(getClass());
		plainReader=p;
	}
	
	public void write(String command, int off, int len) throws IOException {
		logger.debug("Get command " + command);
		plainReader.setCurrentCommand(command);
	}
}
