package com.teleonome.framework.microcontroller.openweathermap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

public class OpenWeatherMapReader extends BufferedReader{

	
	String latitude,  longitud,  units,  appid, timeZoneId;
	int connectTimeoutMilliseconds ,readTimeoutMilliseconds,dailyCount;
	private String currentCommand="";
	Logger logger;
	public OpenWeatherMapReader(Reader in, String la, String lo, String u, String a, int co, int r, int d, String tz) {
		super(in);
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(getClass());
		latitude=la;
		longitud=lo;
		units=u;
		appid=a;
		connectTimeoutMilliseconds=co;
		readTimeoutMilliseconds=r;
		dailyCount=d;
		timeZoneId=tz;
	}

	public String readLine(){
		if(!currentCommand.equals("GetSensorData"))return "";
		
		StringBuffer buffer = new StringBuffer();
		
			try {
				String nowAPI= "http://api.openweathermap.org/data/2.5/weather?lat="+ latitude +"&lon="+longitud+"&appid="+appid+"&units="+units;
				//JSONObject nowJSON = processURL(nowAPI) ;
				
				// http://api.openweathermap.org/data/2.5/forecast?lat=-37.1332&lon=144.47&appid=103df7bb3e4010e033d494f031b483e0&units=metric;
				
				String forecastShort="http://api.openweathermap.org/data/2.5/forecast?lat="+ latitude +"&lon="+longitud+"&appid="+appid+"&units="+units;
				JSONObject forecastShortJSON = processURL(forecastShort) ;
				
				//
				// the format that it returns loks like:
				
//				{"city":
//					{
//						"id":1851632,
//						"name":"Shuzenji",
//					}
//					"coord":
//						{
//							"lon":138.933334,
//							"lat":34.966671
//						},
//					"country":"JP",
//					"cod":"200",
//					"message":0.0045,
//					"cnt":38,
//					"list":[
//					        {
//					        "dt":1406106000,
//					        "main":
//					        	{
//						            "temp":298.77,
//						            "temp_min":298.77,
//						            "temp_max":298.774,
//						            "pressure":1005.93,
//						            "sea_level":1018.18,
//						            "grnd_level":1005.93,
//						            "humidity":87
//						            "temp_kf":0.26
//					           },
//					        "weather":[
//					                   {
//					                	   	"id":804,
//					                	   	"main":"Clouds",
//					                	   	"description":"overcast clouds",
//					                	   	"icon":"04d"}
//					                   ],
//					        "clouds":{"all":88},
//				            "rain":{"3h":0.1025},
//					        "wind":
//					        	{
//					        		"speed":5.71,
//					        		"deg":229.501
//					        	},
//					        "sys":{"pod":"d"},
//					        "dt_txt":"2014-07-23 09:00:00"}
//					        ]}
				
				
				// there is one city element which will be ignored
				// then there is a list array that contains the data
				// in here it returns 40 elements, 8 per day for the next 5 days
				// in this case we will do short term forecast for the next 24 hours
				// ie the first 8 elements of the list array
				//
				Calendar calendar = Calendar.getInstance();
				String cityName="", cityCountry="";
				double cityLatitude=0, cityLongitud=0;
				logger.debug("forecastShortJSON=" + forecastShortJSON.toString(4));
				if(forecastShortJSON.has("city")) {
					JSONObject cityJSONObject = forecastShortJSON.getJSONObject("city");
					cityName = cityJSONObject.getString("name");
					cityCountry = cityJSONObject.getString("country");
					JSONObject cityCoordJSONObject = cityJSONObject.getJSONObject("coord");
					cityLatitude = cityCoordJSONObject.getDouble("lat");
					cityLongitud = cityCoordJSONObject.getDouble("lon");
					
				}
				if(forecastShortJSON.has("list")) {
					JSONArray listJSONArray = forecastShortJSON.getJSONArray("list");
					JSONArray weatherSubElementJSONArray;
					
					TimeZone currentTimeZone = null;
					if(timeZoneId!=null && !timeZoneId.equals("")){
						currentTimeZone = TimeZone.getTimeZone(timeZoneId);
					}else{
						currentTimeZone = TimeZone.getDefault();
					}

					long periodMillisecondsUTC=0;
					String periodTimestampAdjustedToimeZone;
					SimpleDateFormat dailyFormat = new SimpleDateFormat("EEE dd/MM/yy");
					dailyFormat.setTimeZone(currentTimeZone);
					
					SimpleDateFormat hourlyFormatter = new SimpleDateFormat("HH:mm");
					hourlyFormatter.setTimeZone(currentTimeZone);
										

					
					System.out.println("OpenWeatherMapReader currentTimeZone=" + currentTimeZone + " timeZoneId=" + timeZoneId);
					
					JSONObject listElementJSONObject, rainSubElementJSONObject,mainSubElementJSONObject,weatherSubElementJSONObject, cloudsSubElementJSONObject, windSubElementJSONObject;
					for(int i=0;i<8;i++) {
						listElementJSONObject = listJSONArray.getJSONObject(i);
						mainSubElementJSONObject = listElementJSONObject.getJSONObject("main");
						weatherSubElementJSONArray = listElementJSONObject.getJSONArray("weather");
						weatherSubElementJSONObject = weatherSubElementJSONArray.getJSONObject(0);
						cloudsSubElementJSONObject = listElementJSONObject.getJSONObject("clouds");
						if(listElementJSONObject.has("rain")) {
							rainSubElementJSONObject = listElementJSONObject.getJSONObject("rain");
						}else {
							rainSubElementJSONObject=null;
						}
						
						windSubElementJSONObject = listElementJSONObject.getJSONObject("wind");
						periodMillisecondsUTC = new Long(listElementJSONObject.getLong("dt"))*1000;
						//periodTimestampAdjustedToimeZone = sdf.format(new Timestamp(periodMillisecondsUTC));
						
						calendar.setTimeInMillis(periodMillisecondsUTC);
						periodTimestampAdjustedToimeZone = hourlyFormatter.format(calendar.getTime()); 
						
						buffer.append(periodMillisecondsUTC + "#");
						buffer.append(periodTimestampAdjustedToimeZone + "#");    
						buffer.append(mainSubElementJSONObject.getDouble("temp") + "#");
						buffer.append(mainSubElementJSONObject.getDouble("humidity") + "#");
						buffer.append(weatherSubElementJSONObject.getString("description") + "#");
						buffer.append(cloudsSubElementJSONObject.getInt("all") + "#");
						//
						// if there is rain,rainSubElementJSONObject will contain an element with a key 3h, otherwise is empty
						if(rainSubElementJSONObject!=null && rainSubElementJSONObject.has("3h")) {
							buffer.append(rainSubElementJSONObject.getDouble("3h") + "#");
						}else {
							buffer.append("0.0#");
						}
						
						
						buffer.append(windSubElementJSONObject.getDouble("speed") + "#");
						buffer.append(windSubElementJSONObject.getDouble("deg") + "#");
					}
					
					
					String forecastLong="http://api.openweathermap.org/data/2.5/forecast/daily?lat="+ latitude +"&lon="+longitud+"&appid="+appid+"&cnt="+ dailyCount +"&units="+units;
					JSONObject forecastLongJSON = processURL(forecastLong) ;
					 listJSONArray = forecastLongJSON.getJSONArray("list");
					//
					// what returns looks like:
					
					//
					// we dont want the first element of the list because that is for today  and we have that info in the short term forecast
					//
					JSONObject tempSubElementJSONObject;
					
					for(int i=1;i<5;i++) {
						listElementJSONObject = listJSONArray.getJSONObject(i);
						//periodMillisecondsUTC = listElementJSONObject.getLong("dt");
						//periodTimestampAdjustedToimeZone = sdf.format(new Timestamp(periodMillisecondsUTC));
						periodMillisecondsUTC = new Long(listElementJSONObject.getLong("dt"))*1000;
						calendar.setTimeInMillis(periodMillisecondsUTC);
						periodTimestampAdjustedToimeZone = dailyFormat.format(calendar.getTime()); 
						
						tempSubElementJSONObject = listElementJSONObject.getJSONObject("temp");
						weatherSubElementJSONArray = listElementJSONObject.getJSONArray("weather");
						weatherSubElementJSONObject = weatherSubElementJSONArray.getJSONObject(0);
						buffer.append(periodMillisecondsUTC + "#");
						buffer.append(periodTimestampAdjustedToimeZone + "#");
						buffer.append(tempSubElementJSONObject.getDouble("min") + "#");
						buffer.append(tempSubElementJSONObject.getDouble("max") + "#");
						buffer.append(listElementJSONObject.getDouble("humidity") + "#");
						buffer.append(weatherSubElementJSONObject.getString("description") + "#");
						buffer.append(listElementJSONObject.getDouble("speed") + "#");
						buffer.append(listElementJSONObject.getDouble("deg") + "#");
						buffer.append(listElementJSONObject.getDouble("clouds") + "#");
						// if there is no rain there will be no rain element
						//
						if(listElementJSONObject.has("rain")) {
							buffer.append(listElementJSONObject.getDouble("rain") + "#");
						}else {
							buffer.append("0.0#");
						}
					}
					
					buffer.append(cityName + "#");
					buffer.append(cityCountry + "#");
					buffer.append(cityLatitude + "#");
					buffer.append(cityLongitud + "#");
				}else {
					//
					// if we are here is because there was a problem with the service
					// so just create an empty string to make sure that the code continues
				
					
					TimeZone currentTimeZone = null;
					if(timeZoneId!=null && !timeZoneId.equals("")){
						currentTimeZone = TimeZone.getTimeZone(timeZoneId);
					}else{
						currentTimeZone = TimeZone.getDefault();
					}

					long periodMillisecondsUTC=0;
					String periodTimestampAdjustedToimeZone;
					SimpleDateFormat dailyFormat = new SimpleDateFormat("EEE dd/MM/yy");
					dailyFormat.setTimeZone(currentTimeZone);
					periodTimestampAdjustedToimeZone = dailyFormat.format(calendar.getTime()); 
					
					SimpleDateFormat hourlyFormatter = new SimpleDateFormat("HH:mm");
					hourlyFormatter.setTimeZone(currentTimeZone);
					System.out.println("OpenWeatherMapReader currentTimeZone=" + currentTimeZone + " timeZoneId=" + timeZoneId);
					for(int i=0;i<8;i++) {
						buffer.append(periodMillisecondsUTC + "#");
						buffer.append(periodTimestampAdjustedToimeZone + "#");    
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append( "#");
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append( "999#");
					}
					JSONObject tempSubElementJSONObject;
					
					for(int i=1;i<5;i++) {
						buffer.append(periodMillisecondsUTC + "#");
						buffer.append(periodTimestampAdjustedToimeZone + "#");
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append( "#");
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append( "999#");
						buffer.append("0.0#");
					}
					//
					// the name,country,lat and long
					buffer.append("#");
					buffer.append("#");
					buffer.append("0.0#");
					buffer.append("0.0#");
				}
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 currentCommand="";
			return buffer.toString();
		}

		public JSONObject processURL(String urlString) throws MalformedURLException, IOException {
			HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
			connection.setConnectTimeout(connectTimeoutMilliseconds);
			connection.setReadTimeout(readTimeoutMilliseconds);
			connection.setRequestProperty("Accept", "application/json");
			connection.setRequestMethod("GET");
			StringBuffer buffer = new StringBuffer();
			int responseCode = connection.getResponseCode();
			JSONObject toReturnJSON=new JSONObject();
			logger.debug("processURL responseCode " + responseCode);
			if(200 <= responseCode && responseCode <= 399) {
				BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));
				String output;
				while ((output = br.readLine()) != null) {
					buffer.append(output);
				}
				toReturnJSON=new JSONObject(buffer.toString());
			}
			logger.debug("processURL returned " + buffer.toString());
			return toReturnJSON;
		}

		public void setCurrentCommand(String command) {
			// TODO Auto-generated method stub
			currentCommand=command;
		}
	}
