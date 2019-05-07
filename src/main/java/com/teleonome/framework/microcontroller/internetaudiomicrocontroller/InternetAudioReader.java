package com.teleonome.framework.microcontroller.internetaudiomicrocontroller;

import java.io.BufferedReader;
import java.io.Reader;

import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherWriter;

public class InternetAudioReader extends BufferedReader{

	InternetAudioWriter anInternetAudioWriter;
	
	public InternetAudioReader(Reader in, InternetAudioWriter d) {
		super(in);
		anInternetAudioWriter=d;
		// TODO Auto-generated constructor stub
	}

	public String readLine(){
		return anInternetAudioWriter.getStatus();
	}
}