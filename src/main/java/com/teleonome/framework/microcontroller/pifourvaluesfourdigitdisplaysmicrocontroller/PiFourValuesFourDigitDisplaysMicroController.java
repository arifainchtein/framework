package com.teleonome.framework.microcontroller.pifourvaluesfourdigitdisplaysmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;

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

public class PiFourValuesFourDigitDisplaysMicroController  extends MicroController implements LifeCycleEventListener{
	private static int PIN_NUMBER_RED = 4;
	private static int PIN_NUMBER_GREEN = 17;
	private static int PIN_NUMBER_BLUE = 18;
	 private static int colors[] = {  0x00FF00, 0x0000FF };


	PiFourValuesFourDigitDisplaysWriter aPiFourValuesFourDigitDisplaysWriter;
	PiFourValuesFourDigitDisplaysReader aPiFourValuesFourDigitDisplaysReader;
	StringWriter sw = new StringWriter();

	String dgTeleonomeName="";
	Logger logger;
	JSONArray configParams;

	public PiFourValuesFourDigitDisplaysMicroController(DenomeManager d, String n) {
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

		aPiFourValuesFourDigitDisplaysWriter = new PiFourValuesFourDigitDisplaysWriter(sw, configParams, aDenomeManager) ;
		aPiFourValuesFourDigitDisplaysReader = new PiFourValuesFourDigitDisplaysReader(new StringReader(""), aPiFourValuesFourDigitDisplaysWriter) ;

		Gpio.wiringPiSetup();

		SoftPwm.softPwmCreate(PIN_NUMBER_RED, 0, 100);
		SoftPwm.softPwmCreate(PIN_NUMBER_GREEN, 0, 100);
		SoftPwm.softPwmCreate(PIN_NUMBER_BLUE, 0, 100);

		logger.info(" PiFourValuesFourDigitDisplaysMicroController  " );

	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		aPiFourValuesFourDigitDisplaysWriter = new PiFourValuesFourDigitDisplaysWriter(sw, configParams, aDenomeManager) ;
		aPiFourValuesFourDigitDisplaysReader = new PiFourValuesFourDigitDisplaysReader(new StringReader(""), aPiFourValuesFourDigitDisplaysWriter) ;

		return aPiFourValuesFourDigitDisplaysWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		return aPiFourValuesFourDigitDisplaysReader;
	}

	@Override
	public void processLifeCycleEvent(String lifeCycleEvent) {
		// TODO Auto-generated method stub
		logger.debug("processing life cycle even=t=" + lifeCycleEvent);
		if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)) {
			//String command = "neouart -i 00ff0000 00ff0000";
			
			ledColorSet(0x00FF00);
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event start pulse produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}

		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
			ledColorSet(0x00FF00);
//			String command = "neouart -i 0000ff00 0000ff00";
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event end pulse produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE)) {
//			String command = "neouart -i 0000ff00 0000ff00";
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
			ledColorSet(0x000FF);
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE)) {
//			String command = "neouart -i 00ff0000 00ff0000";
//			try {
//				ArrayList<String> results  = Utils.executeCommand(command);
//				logger.debug("processing life cycle event LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE produced=" + results);
//			} catch (IOException | InterruptedException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
			ledColorSet(0x000FF);
		}
	}

	private int map(int x, int in_min, int in_max, int out_min, int out_max) {
		return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}

	private void ledColorSet(int color) {
		int r_val, g_val, b_val;

		r_val = (color & 0xFF0000) >> 16; // get red value
		g_val = (color & 0x00FF00) >> 8; // get green value
		b_val = (color & 0x0000FF) >> 0; // get blue value

		r_val = map(r_val, 0, 255, 0, 100); // change a num(0~255) to 0~100
		g_val = map(g_val, 0, 255, 0, 100);
		b_val = map(b_val, 0, 255, 0, 100);

		SoftPwm.softPwmWrite(PIN_NUMBER_RED, 100 - r_val); // change duty cycle
		SoftPwm.softPwmWrite(PIN_NUMBER_GREEN, 100 - g_val);
		SoftPwm.softPwmWrite(PIN_NUMBER_BLUE, 100 - b_val);
	}
}
