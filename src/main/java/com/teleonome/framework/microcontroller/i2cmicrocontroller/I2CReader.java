package com.teleonome.framework.microcontroller.i2cmicrocontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.pi4j.io.i2c.I2CDevice;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.utils.Utils;

public class I2CReader  extends BufferedReader{
	Logger logger;
	private String currentCommand="";
	private I2CWriter anI2CWriter;
	private I2CDevice device;
	public I2CReader(Reader in, I2CWriter w, I2CDevice d) {
		super(in);
		anI2CWriter=w;
		device=d;
	}


	//
	// override the close method so the stream is not closed
	//
	public void close() {
		
	}

	public String readLine(){
		String toReturn="";
		byte[] b2 =  new byte[20];
		String response="";
		try {
			
			device.read(b2,0,b2.length);
			for(int i=0;i<b2.length;i++){
				System.out.println("i="+i + " b2[i]=" + b2[i]);
				if(b2[i]<=0)b2[i]=32;
			}
			response=new String(b2, "UTF-8").trim();
			System.out.println("response=" + response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		if(currentCommand.equals("GetSensorData")) {
			return response;
		}else if(currentCommand.startsWith("SetDisplays")) {
			return response;
		}else if(currentCommand.equals(TeleonomeConstants.USER_COMMAND)) {
				return response;
		}else {
			toReturn= "Ok";	
		}
		//logger.debug("network info file=" + toReturn);
		return toReturn;
	}

	public void setCurrentCommand(String command) {
		// TODO Auto-generated method stub
		currentCommand=command;
	}
}
