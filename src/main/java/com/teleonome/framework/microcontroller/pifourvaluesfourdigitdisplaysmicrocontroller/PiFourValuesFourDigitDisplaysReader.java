package com.teleonome.framework.microcontroller.pifourvaluesfourdigitdisplaysmicrocontroller;

import java.io.BufferedReader;
import java.io.Reader;

public class PiFourValuesFourDigitDisplaysReader extends BufferedReader{
	PiFourValuesFourDigitDisplaysWriter aPiFourValuesFourDigitDisplaysWriter;
	
	public PiFourValuesFourDigitDisplaysReader(Reader in, PiFourValuesFourDigitDisplaysWriter w) {
		super(in);
		aPiFourValuesFourDigitDisplaysWriter=w;
	}

	
	
	public String readLine(){
		return aPiFourValuesFourDigitDisplaysWriter.getPublishingResults();
	}
}
