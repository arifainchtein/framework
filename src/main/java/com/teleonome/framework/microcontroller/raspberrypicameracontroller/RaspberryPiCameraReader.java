package com.teleonome.framework.microcontroller.raspberrypicameracontroller;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.utils.Utils;

public class RaspberryPiCameraReader extends BufferedReader{

	
	Logger logger;
	private String currentCommand="";
	private String destinationDirectory="/home/pi/Teleonome/tomcat/webapps/ROOT/images/";
	public RaspberryPiCameraReader(Reader in ) {
		super(in);
		logger = Logger.getLogger(getClass().getName());
		
		// TODO Auto-generated constructor stub
		
	}

	public String readLine(){
		logger.info("RaspberryPiCameraReader currentCommand=" + currentCommand);
		if(!currentCommand.equals("GetSensorData"))return "Ok";
		
		LocalDateTime currentTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.CAMERA_TIMESTAMP_FILENAME_FORMAT);
		String formatedCurrentTime = currentTime.format(formatter);
		String fileName=formatedCurrentTime + ".jpg";
		String cameraCommand = "raspistill  -w 640 -h 480 -q 75 -o " + destinationDirectory + fileName;
		logger.info("RaspberryPiCameraReader cameraCommand=" + cameraCommand);
		
		try {
			ArrayList results = Utils.executeCommand(cameraCommand);
			logger.info("results=" + results);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		currentCommand="";
		return fileName;
	}


		public void setCurrentCommand(String command) {
			// TODO Auto-generated method stub
			currentCommand=command;
		}
	}
