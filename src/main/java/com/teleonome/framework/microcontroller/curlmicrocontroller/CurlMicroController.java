package com.teleonome.framework.microcontroller.curlmicrocontroller;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.SoftPwm;
import com.teleonome.framework.LifeCycleEventListener;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherReader;
import com.teleonome.framework.microcontroller.sftppublisher.SFTPPublisherWriter;
import com.teleonome.framework.utils.Utils;

public class CurlMicroController  extends MicroController implements LifeCycleEventListener{
//	private static int PIN_NUMBER_RED = 4;
//	private static int PIN_NUMBER_GREEN = 17;
//	private static int PIN_NUMBER_BLUE = 18;
//	 private static int colors[] = {  0x00FF00, 0x0000FF };

	 final GpioController gpio = GpioFactory.getInstance();
	 final GpioPinDigitalOutput extendedOperationPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07,PinState.HIGH);
	 
	 final GpioPinDigitalOutput synchronousCyclePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00,PinState.LOW);
	 final GpioPinDigitalOutput aSynchronousCyclePin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW);
	    
	CurlWriter aCurlWriter;
	CurlReader aCurlReader;
	StringWriter sw = new StringWriter();

	String dgTeleonomeName="";
	Logger logger;
	JSONArray configParams;

	public CurlMicroController(DenomeManager d, String n) {
		super(d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub  
	}

	public void init(JSONArray c) throws MicrocontrollerCommunicationException {
		configParams=c;
		//
		// configParams will contain 3 denes (plus a codon), which represents every display,
		// the three dnewords are:
		// Identity -  a pointer to where the data is stored
		// Clock pin:  the pin on the pi that represents the clock signal
		// Data pin: the pin on the pi that represents the data signal

		aCurlWriter = new CurlWriter(sw, configParams, aDenomeManager) ;
		aCurlReader = new CurlReader(new StringReader(""), aCurlWriter) ;

		
		logger.info(" PiFourValuesFourDigitDisplaysMicroController  " );

	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		aCurlWriter = new CurlWriter(sw, configParams, aDenomeManager) ;
		aCurlReader = new CurlReader(new StringReader(""), aCurlWriter) ;

		return aCurlWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		return aCurlReader;
	}

	@Override
	public void processLifeCycleEvent(String lifeCycleEvent) {
		// TODO Auto-generated method stub
		logger.debug("1-processing life cycle even=t=" + lifeCycleEvent);
		if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)) {
			//String command = "neouart -i 00ff0000 00ff0000";
			synchronousCyclePin.high();
			
			logger.debug("2-synchronousCyclePin.isHigh=" + synchronousCyclePin.isHigh());
			
			//ledColorSet(0x00FF00);
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event start pulse produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}

		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
			//synchronousCyclePin.toggle();
			synchronousCyclePin.low();
			//ledColorSet(0x00FF00);
//			String command = "neouart -i 0000ff00 0000ff00";
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event end pulse produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE)) {
			//aSynchronousCyclePin.toggle();
			aSynchronousCyclePin.high();
			logger.debug("3b-aSynchronousCyclePin.isHigh=" + aSynchronousCyclePin.isHigh());
			
//			String command = "neouart -i 0000ff00 0000ff00";
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
			//ledColorSet(0x000FF);
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE)) {
			//aSynchronousCyclePin.toggle();
			aSynchronousCyclePin.low();
//			String command = "neouart -i 00ff0000 00ff0000";
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
			//ledColorSet(0x000FF);
		}
	}


}
