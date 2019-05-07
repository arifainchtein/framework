package com.teleonome.framework.microcontroller.internetaudiomicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;

public class InternetAudioWriter extends BufferedWriter{
	DenomeManager aDenomeManager;
	JSONArray configParams;
	Logger logger;
	NetworkThread networkThread;
	ArrayList<Map.Entry<JSONObject, Integer>> indexedStations;
	JSONObject currentStation;
	RadioConnector radio;
	
	public InternetAudioWriter(NetworkThread n, Writer out, JSONArray a, DenomeManager d) {
		super(out);
		logger = Logger.getLogger(getClass());
		networkThread=n;
		configParams=a;
		aDenomeManager=d;
		JSONObject dene;
		String value="", deneName;
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				deneName = dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
				if(deneName.equals("SFTP Server IP Address")){
					//host = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, "SFTP Server IP Address", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void generateIndexedStations(JSONArray a){
		indexedStations = new ArrayList(); 
		JSONObject param;
		for(int i=0;i<a.length();i++){
			param = a.getJSONObject(i);
			indexedStations.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(param, new Integer(param.getInt("KnobPosition"))));
			Collections.sort(indexedStations, new Comparator<Map.Entry<?, Integer>>(){
				public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
					return o1.getValue().compareTo(o2.getValue());
				}});
		}
	}
	
	public void write(String command, int off, int len) throws IOException {
		
		logger.debug("received command:" + command);
		//
		// put a three second delay,
		// this is because the AsyncCycle puts a 3 second delay
		// after writing the message
		// this is because otherwise you run the risk of floding the serial bus
		// so to make it standards all microcontrollers do this
		
		
	    Random rand = new Random(); 
	    int value = rand.nextInt(1000);
	    if(command.startsWith("StationChange")) {
	    	String previousStationName = currentStation.getString("StationName");
			if(previousStationName.equals("Spotify")) {
				processSpotify("Pause");
			}else {
				processRadio("Pause", null);
			}
	    	
	    	String[] tokens = command.split("#");
			int stationNumber = Integer.parseInt(tokens[1]);
			currentStation = (JSONObject) indexedStations.get(stationNumber);
			String currentStationName = currentStation.getString("StationName");
			String stationURL = currentStation.getString("StationURL");
			if(currentStationName.equals("Spotify")) {
				processSpotify("Play");
			}else {
				processRadio("Play", stationURL);
			}
			
		}else {
			String currentStationName = currentStation.getString("StationName");
			if(currentStationName.equals("Spotify")) {
				processSpotify(command);
			}else {
				processRadio(command, null);
			}
		}

		
		
			
	}

	private void processRadio(String command, String data) {
	
		if(command.equals("Play")) {
			radio = new RadioConnector(data);
			radio.start();
		}else if(command.equals("Pause")) {
			radio.Terminate();
			try {
				radio.join();
		    } catch (InterruptedException ex) {
		        //catch the exception
		    }
		}
	}
	
	private void processSpotify(String command) {
		if(command.equals("Play")) {
			networkThread.sendPlayer( "player.play");
		}else if(command.equals("Pause")) {
				networkThread.sendPlayer("player.pause");
		}else if(command.equals("PlayPause")) {
				networkThread.sendPlayer( "player.playPause");
		}else if(command.equals("Next")) {
				networkThread.sendPlayer( "player.next");
		}else if(command.equals("Previous")) {
				networkThread.sendPlayer("player.prev");
		}
	}

	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}
}
