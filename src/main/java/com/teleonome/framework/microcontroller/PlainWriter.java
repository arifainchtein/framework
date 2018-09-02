package com.teleonome.framework.microcontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class PlainWriter extends BufferedWriter{

	PlainReader plainReader;
	
	public PlainWriter(Writer out, PlainReader p) {
		super(out);
		plainReader=p;
	}
	
	public void write(String command, int off, int len) throws IOException {
		plainReader.setCurrentCommand(command);
	}
}
