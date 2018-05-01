package com.teleonome.framework.microcontroller.digitalgeppettopublisher;

import java.io.BufferedReader;
import java.io.Reader;

public class DigitalGeppettoPublisherReader extends BufferedReader{

	DigitalGeppettoPublisherWriter aDigitalGeppettoWriter;
	
	public DigitalGeppettoPublisherReader(Reader in, DigitalGeppettoPublisherWriter d) {
		super(in);
		aDigitalGeppettoWriter=d;
		// TODO Auto-generated constructor stub
	}

	public String readLine(){
		return aDigitalGeppettoWriter.getPublishingResults();
	}
}
