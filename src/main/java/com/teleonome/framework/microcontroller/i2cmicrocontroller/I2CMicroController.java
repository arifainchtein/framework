package com.teleonome.framework.microcontroller.i2cmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import com.teleonome.framework.LifeCycleEventListener;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.utils.Utils;
public class I2CMicroController  extends MicroController implements LifeCycleEventListener{

	StringWriter sw = new StringWriter();
	I2CWriter anI2CWriter;
	I2CReader anI2CReader;
	private Logger logger;
	JSONArray configParams;

	I2CBus i2c = null;
	I2CDevice device = null;
	public static final int I2C_ADDR = 11; 

	
	public I2CMicroController(Hypothalamus h,DenomeManager d, String n) {
		super(h,d, n);
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(getClass());
	}

	public void processLifeCycleEvent(String lifeCycleEvent) {
		// TODO Auto-generated method stub
		logger.debug("1-processing life cycle even=t=" + lifeCycleEvent);
		if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)) {
			try {
				anI2CWriter.write(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE.length());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
			try {
				anI2CWriter.write(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE.length());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE)) {
			try {
				anI2CWriter.write(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE.length());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE)) {
			try {
				anI2CWriter.write(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE.length());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE)) {
			try {
				anI2CWriter.write(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE.length());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}
	}

	@Override
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		configParams=params;
		try {
			i2c = I2CFactory.getInstance(I2CBus.BUS_1);
		} catch (UnsupportedBusNumberException | IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		try {
			device = i2c.getDevice(I2C_ADDR);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		anI2CWriter = new I2CWriter(new StringWriter(), device,configParams, aDenomeManager);
		anI2CReader = new I2CReader(new StringReader(""),anI2CWriter, device);
		anI2CWriter.setReader(anI2CReader);
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		return anI2CReader; 
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return anI2CWriter;
	}

}
