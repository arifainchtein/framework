package com.teleonome.framework.microcontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReversedLinesFileReader;

import com.teleonome.framework.TeleonomeConstants;

public class PlainReader extends BufferedReader{
	int counter=0;
	int maximum=2;
	int value=0;
	StringWriter stringWriter;
	public PlainReader(Reader in, StringWriter s) {
		super(in);
		stringWriter=s;
	}

	public String readLine(){
		
		if(counter<maximum){
			counter++;
		}else{
			counter=0;
			if(value==0)value=1;
			else value=0;
		}
		//System.out.println("counter="+ counter + " value" + value);
		return value + "#";

	}
}
