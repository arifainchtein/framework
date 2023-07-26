package com.teleonome.framework.microcontroller.curlmicrocontroller;


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
public class CurlWriter extends BufferedWriter {
	private DenomeManager aDenomeManager;
	Logger logger;
	JSONArray configParams;
	private String publishingResults="";
	String host;
	
	public CurlWriter(Writer out,JSONArray p, DenomeManager d) {
		super(out);
		// TODO Auto-generated constructor stub
		aDenomeManager=d;
		logger = Logger.getLogger(getClass());
		configParams=p;
		
		configParams=p;
		aDenomeManager=d;
		JSONObject dene;
		String value="", deneName;
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				deneName = dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
				if(deneName.equals("Curl IP Address")){
					host = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, "SFTP Server IP Address", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	public String getPublishingResults(){
		return "";
	}
	
	public void write(String command, int off, int len) throws IOException {
		publishingResults="";
		logger.debug("receive command " + command);
		publishingResults="Ok-Update";
	}
}
