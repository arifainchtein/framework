package com.teleonome.framework.microcontroller.pifourvaluesfourdigitdisplaysmicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.utils.Utils;

public class PiFourValuesFourDigitDisplaysWriter extends BufferedWriter {
	private DenomeManager aDenomeManager;
	Logger logger;
	JSONArray params;
	private String publishingResults="";
	public PiFourValuesFourDigitDisplaysWriter(Writer out,JSONArray p, DenomeManager d) {
		super(out);
		// TODO Auto-generated constructor stub
		aDenomeManager=d;
		logger = Logger.getLogger(getClass());
		params=p;
	}

	public String getPublishingResults(){
		return publishingResults;
	}
	
	public void write(String command, int off, int len) throws IOException {
		publishingResults="";
		logger.debug("receive command " + command);
		if(command.startsWith("Update")) {
			JSONObject displayInfo;
			int clockPin, dataPin;
			String identityPointer;
			ArrayList results;
			String resultsString;
			for(int i=0;i<params.length();i++) {
				displayInfo = params.getJSONObject(i);
				clockPin = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(displayInfo, "Clock Pin", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				dataPin = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(displayInfo, "Data Pin", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				command = "python single.py " + clockPin + " " + dataPin;
				try {
					results = Utils.executeCommand(command);
					 resultsString = String.join(", ", results);
						logger.debug("command=" + command + " results=" + resultsString);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					publishingResults="Fault#PiFourValuesFourDigitDisplaysMicroController#" + i;
				}
				
			}
		}
		publishingResults="Ok-Update";
	}
}
