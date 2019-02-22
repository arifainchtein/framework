package com.teleonome.framework.microcontroller.sftppublisher;

import java.io.BufferedReader;
import java.io.Reader;

import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherWriter;

public class SFTPPublisherReader extends BufferedReader{

	SFTPPublisherWriter aSFTPWriter;
	
	public SFTPPublisherReader(Reader in, SFTPPublisherWriter d) {
		super(in);
		aSFTPWriter=d;
		// TODO Auto-generated constructor stub
	}

	public String readLine(){
		return aSFTPWriter.getPublishingResults();
	}
}
