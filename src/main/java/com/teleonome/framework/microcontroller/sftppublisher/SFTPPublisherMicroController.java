package com.teleonome.framework.microcontroller.sftppublisher;




import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;

public class SFTPPublisherMicroController extends MicroController{
	SFTPPublisherWriter aSFTPWriter;
	SFTPPublisherReader aSFTPReader;
	String dgTeleonomeName="";
	Logger logger;
	public SFTPPublisherMicroController(DenomeManager d, String n) {
		super(d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub  
	}
	
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		
		 aSFTPWriter = new SFTPPublisherWriter(new StringWriter(), configParams, aDenomeManager) ;
		 aSFTPReader = new SFTPPublisherReader(new StringReader(""), aSFTPWriter) ;
		 
       	logger.info(" SFTPPublisherMicroController microcontroller dgTeleonomeName " + dgTeleonomeName);
       	
	}
	
	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		  return aSFTPWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		return aSFTPReader;
	}

}
