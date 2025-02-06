package com.teleonome.framework.microcontroller.digitalgeppettopublisher;

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
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;

public class DigitalGeppettoPublisherMicroController extends MicroController{
	DigitalGeppettoPublisherWriter aDigitalGeppettoWriter;
	DigitalGeppettoPublisherReader aDigitalGeppettoReader;
	String dgTeleonomeName="";
	Logger logger;
	public DigitalGeppettoPublisherMicroController(Hypothalamus h,DenomeManager d, String n) {
		super(h,d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub  
	}
	
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		
		 aDigitalGeppettoWriter = new DigitalGeppettoPublisherWriter(new StringWriter(), configParams, aDenomeManager) ;
		 aDigitalGeppettoReader = new DigitalGeppettoPublisherReader(new StringReader(""), aDigitalGeppettoWriter) ;
		 
       	logger.info(" DigitalGeppettoPublisherMicroController microcontroller dgTeleonomeName " + dgTeleonomeName);
       	
	}
	
	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		  return aDigitalGeppettoWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		return aDigitalGeppettoReader;
	}

}
