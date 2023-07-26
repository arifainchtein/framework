package com.teleonome.framework.microcontroller.curlmicrocontroller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

public class CurlReader extends BufferedReader{
	CurlWriter aCurlWriter;
	private String dataLine="";
	private String currentCommand="";
	
	public CurlReader(Reader in, CurlWriter w) {
		super(in);
		aCurlWriter=w;
	}

	public void setCurrentCommand(String s) {
	//	logger.debug("set current command=" +s);
			currentCommand=s;
		
	}
	
	public String readLine(){
		if(currentCommand.startsWith("GetSensorData")) {
	//	JSONObject data = readJsonFromUrl(String url);
		
	//	String dataLine = seedlin+ "#"+ currentCharge + "#"+ currentLoad+ "#" + currentStateOfCharge + "#"+ batteryState + "#"+ totalLoadAmpHoursForToday+ "#"+ totalChargeAmpHoursForToday + "#"+ minBatVoltageToday +"#" + maxBatVoltageToday;
			
		}
		return aCurlWriter.getPublishingResults();
	}
	
	public JSONObject processURL(String urlString) throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
		//connection.setConnectTimeout(connectTimeoutMilliseconds);
		//connection.setReadTimeout(readTimeoutMilliseconds);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestMethod("GET");
		StringBuffer buffer = new StringBuffer();
		int responseCode = connection.getResponseCode();
		JSONObject toReturnJSON=new JSONObject();
		//logger.debug("processURL responseCode " + responseCode);
		if(200 <= responseCode && responseCode <= 399) {
			BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
			String output;
			while ((output = br.readLine()) != null) {
				buffer.append(output);
			}
			toReturnJSON=new JSONObject(buffer.toString());
		}
	//	logger.debug("processURL returned " + buffer.toString());
		return toReturnJSON;
	}

}
