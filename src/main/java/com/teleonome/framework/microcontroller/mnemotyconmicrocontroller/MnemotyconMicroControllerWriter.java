package com.teleonome.framework.microcontroller.mnemotyconmicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class MnemotyconMicroControllerWriter extends BufferedWriter{
	
	
	public MnemotyconMicroControllerWriter(Writer out) {
		super(out);
		// TODO Auto-generated constructor stub
	}
	
	public void write(String command, int off, int len) throws IOException {
		
	}
	
}
