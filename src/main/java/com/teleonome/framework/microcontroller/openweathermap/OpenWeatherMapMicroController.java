package com.teleonome.framework.microcontroller.openweathermap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.csvmicrocontroller.CSVReader;
import com.teleonome.framework.utils.Utils;

public class OpenWeatherMapMicroController  extends MicroController{
	StringWriter sw = new StringWriter();
	OpenWeatherMapWriter anOpenWeatherMapWriter;
	OpenWeatherMapReader anOpenWeatherMapReader;
	CSVReader cvsReader;
	JSONArray configParams;
	String units, appid, timeZoneId;
	double latitude, longitud;
	int dailyCount=10;
	Logger logger;
	int connectTimeoutMilliseconds,readTimeoutMilliseconds;
	public OpenWeatherMapMicroController(DenomeManager d, String n) {
		super(d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}

	
	
	/**
	 *  the config params is a JSONArray that contains complete Denes. Each one of this Denes contains the
	 *  following denewords names and values:
	 * 
	 *                                       
		"Value": 1,
        "Name": "Processing Request Queue Position",
   
        "Value":"@Icarus:Internal:Sensors:Pitia Status Value",
        "Name": "Sensor Value Pointer",
   
        "Value":2000,
        "Name": "Connection Timeout in Milliseconds",
   
        "Value":"@Icarus:Internal:Sensors:Pitia Status Value",
        "Name": "Sensor Value Pointer",
   
        "Value": "pitia.casete.com.mx",
        "Name": "Web Address", 
	 */
	@Override
	public void init(JSONArray p) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		configParams=p;
		try {
			logger.debug("initializing cvs microcontroller with cnfigparams of " + configParams.toString(4));
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		//
		// now loop over each dene to get the order in the string
		Integer I;
		JSONObject dene;
		String deneName="";
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				deneName = dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
				//System.out.println("denemanem="+ deneName);
				if(deneName.equals("Latitude")) {
					latitude = (double) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Latitude", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("Longitude")) {
					longitud = (double) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Longitude", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("Units")) {
					units = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Units", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("OpenWeatherAPIKey")) {
					appid = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "OpenWeatherAPIKey", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("Connect Timeout Milliseconds")) {
					connectTimeoutMilliseconds = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Connect Timeout Milliseconds", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("Read Timeout Milliseconds")) {
					readTimeoutMilliseconds = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Read Timeout Milliseconds", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("Daily Count")) {
					dailyCount = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Daily Count", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
				}else if(deneName.equals("Latitude")) {
					String timeZoneIdPointer = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Timezone", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);					
					try {
						timeZoneId = (String) aDenomeManager.getDeneWordAttributeByIdentity(new Identity(timeZoneIdPointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		anOpenWeatherMapReader  =new OpenWeatherMapReader(new StringReader(""),latitude, longitud, units, appid, connectTimeoutMilliseconds,readTimeoutMilliseconds,dailyCount, timeZoneId);
		anOpenWeatherMapWriter = new OpenWeatherMapWriter(sw,anOpenWeatherMapReader);
       	logger.info(" openweather microcontroller latitude " + latitude + " longitud=" + longitud + " units=" + units + " appid=" + appid + " timeZoneId=" + timeZoneId);
		
	}
	
	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		anOpenWeatherMapWriter = new OpenWeatherMapWriter(sw,anOpenWeatherMapReader);
       	
         return anOpenWeatherMapWriter;
	}


	@Override
	public BufferedReader getReader() throws IOException {
		String  dataString="dataString";
		
		//anOpenWeatherMapReader  =new OpenWeatherMapReader(new StringReader(""),latitude, longitud, units, appid, connectTimeoutMilliseconds,readTimeoutMilliseconds,dailyCount, timeZoneId);
		
		return anOpenWeatherMapReader;
		
	}
}
