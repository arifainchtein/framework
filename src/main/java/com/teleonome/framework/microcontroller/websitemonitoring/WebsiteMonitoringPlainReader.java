
package com.teleonome.framework.microcontroller.websitemonitoring;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

public class WebsiteMonitoringPlainReader extends BufferedReader {

	
	ArrayList<Map.Entry<JSONObject, Integer>> configParamDeneProcessingRequestQueuePositionIndex;
	
	
	
	public WebsiteMonitoringPlainReader(Reader in, ArrayList<Map.Entry<JSONObject, Integer>> cp) {
		super(in);
		// TODO Auto-generated constructor stub
		configParamDeneProcessingRequestQueuePositionIndex=cp;
	}

	public String readLine(){
		//
		// the config params contains denes
		JSONObject dene;
		String url;
		int timeout;
		StringBuffer toReturn = new StringBuffer();
		for (Map.Entry<JSONObject, Integer> entry : configParamDeneProcessingRequestQueuePositionIndex) {
			dene = (JSONObject)entry.getKey();
			//
			// now get the denewords for the url and the timeout
			
        		try {
					timeout = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Connection Timeout in Milliseconds", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
	            	url = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Web Address", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
	            	if(pingURL( url,  timeout)){
	            		toReturn.append("OK#");
	            	}else{
	            		toReturn.append("NOT OK#");
	            	}
	            	
        		} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					toReturn.append("NOT OK#");
				}
		}
		return toReturn.toString();
	}
	
	boolean pingURL(String url, int timeout) {
	    url = url.replaceFirst("^https", "http"); // Otherwise an exception may be thrown on invalid SSL certificates.

	    try {
	        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
	        connection.setConnectTimeout(timeout);
	        connection.setReadTimeout(timeout);
	        connection.setRequestMethod("HEAD");
	        int responseCode = connection.getResponseCode();
	        return (200 <= responseCode && responseCode <= 399);
	    } catch (IOException exception) {
	        return false;
	    }
	}
	
}
