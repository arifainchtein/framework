package com.teleonome.framework.microcontroller.pifourvaluesfourdigitdisplaysmicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
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
			double value;
			String identityPointer;
			ArrayList results;
			String resultsString,valuePointer;
			JSONObject dataSource;
			for(int i=0;i<params.length();i++) {
				displayInfo = params.getJSONObject(i);
				
				try {
					clockPin = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(displayInfo, "Clock Pin", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					dataPin = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(displayInfo, "Data Pin", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					valuePointer = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(displayInfo, "Identity", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//
					// check if the pointer is in external data,if so, check t see if the data is stale
					//
					logger.debug("clockPin=" + clockPin + " dataPin=" + dataPin + " valuePointer=" + valuePointer + " isext=" + valuePointer.contains(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA));;
					if(valuePointer.contains(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)) {
						if(aDenomeManager.isExternalDataOk(valuePointer)) {
							Object o=  aDenomeManager.getDeneWordAttributeByIdentity(new Identity(valuePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							if(o instanceof Double) {
								value=(double)o;
							}else if(o instanceof Integer) {
								value=((Integer)o).doubleValue();
							}else {
								value=9999;
							}
						}else {
							value=9999;
						}
						
					}else {
						value = (double) aDenomeManager.getDeneWordAttributeByIdentity(new Identity(valuePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						
					}
					
					command = "python single.py " + clockPin + " " + dataPin + " " + value;
					results = Utils.executeCommand(command);
					 resultsString = String.join(", ", results);
						logger.debug("command=" + command + " results=" + resultsString);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					publishingResults="Fault#PiFourValuesFourDigitDisplaysMicroController#" + i;
				} catch (InvalidDenomeException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					publishingResults="Fault#PiFourValuesFourDigitDisplaysMicroController#" + i;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					publishingResults="Fault#PiFourValuesFourDigitDisplaysMicroController#" + i;
				}
				
			}
		}
		publishingResults="Ok-Update";
	}
}
