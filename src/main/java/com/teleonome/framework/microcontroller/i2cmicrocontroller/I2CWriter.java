package com.teleonome.framework.microcontroller.i2cmicrocontroller;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import java.io.IOException;
import com.pi4j.platform.PlatformAlreadyAssignedException;
import com.pi4j.util.Console;

import com.pi4j.wiringpi.SoftPwm;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.microcontroller.plseries.PLSeriesReader;
import com.teleonome.framework.utils.Utils;

import java.io.UnsupportedEncodingException;
public class I2CWriter extends BufferedWriter {
	String currentCommand="";
	I2CReader reader;
	private I2CDevice device;
	private Logger logger;
	I2CBus i2c;
	JSONArray configParams;
	private DenomeManager aDenomeManager;
	public static final int I2C_ADDRESS = 11;
	private String publishingResults="";

	public I2CWriter(Writer out, I2CDevice d, JSONArray c, DenomeManager dm) {
		super(out);
		logger = Logger.getLogger(getClass());
		device=d;
		System.gc();
		configParams=c;
		aDenomeManager=dm;
		logger.debug("Initialized I2CWriter");

	}
	//
	// override the close method so the stream is not closed
	//
	public void close() {
		
	}
	public void setReader(I2CReader r) {
		reader=r;

	}
	public void setCurrentCommand(String s) {
		currentCommand=s;
	}
	public String getCurrentCommand() {
		return currentCommand;
	}

	public void write(String command, int off, int len) throws IOException {
		setCurrentCommand(command);
		reader.setCurrentCommand(command);
		if(currentCommand==TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE ||
				currentCommand==TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE ||
				currentCommand==TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE ||
				currentCommand==TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE ||
				currentCommand==TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE ||
				currentCommand==TeleonomeConstants.LIFE_CYCLE_EVENT_END_AWAKE ||
				currentCommand==TeleonomeConstants.USER_COMMAND	
			){
			String i2ccommand = command.replace(" ", "") +"#1#$";
			byte[] b2=i2ccommand.getBytes("ISO-8859-1");
			device.write(I2CMicroController.I2C_ADDR,b2);     
		}else if(command.startsWith("Update")) {
			JSONObject displayInfo;

			double value;
			String identityPointer;
			ArrayList results;
			String resultsString,valuePointer;
			JSONObject dataSource;
			logger.debug("params= " + configParams.toString(4));
			StringBuffer i2ccommandbuffer = new StringBuffer("SetDisplays#");

			for(int i=0;i<configParams.length();i++) {
				displayInfo = configParams.getJSONObject(i);

				try {
					valuePointer = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(displayInfo, "Identity", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//
					// check if the pointer is in external data,if so, check t see if the data is stale
					//
					Object o;
					logger.debug( " valuePointer=" + valuePointer + " isext=" + valuePointer.contains(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA));;
					if(valuePointer.contains(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)) {
						if(aDenomeManager.isExternalDataOk(valuePointer)) {
							o=  aDenomeManager.getDeneWordAttributeByIdentity(new Identity(valuePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
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
						o=  aDenomeManager.getDeneWordAttributeByIdentity(new Identity(valuePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(o instanceof Double) {
							value=(double)o;
						}else if(o instanceof Integer) {
							value=((Integer)o).doubleValue();
						}else {
							value=9999;
						}
					}

					i2ccommandbuffer.append(value + "#");

				} catch (InvalidDenomeException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					publishingResults="Fault#I2CMicroController#" + i;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					publishingResults="Fault#I2CMicroController#" + i;
				}

			}
			i2ccommandbuffer.append("$");
			logger.debug("line 141 about to send via i2c:"  +i2ccommandbuffer.toString() );
			byte[] b2=i2ccommandbuffer.toString().getBytes("ISO-8859-1");
			try{
				device.write(I2CMicroController.I2C_ADDR,b2);      
			}catch(IOException e) {
				logger.warn(Utils.getStringException(e));
			}
		}
		publishingResults="Ok-Update";
	}
}
