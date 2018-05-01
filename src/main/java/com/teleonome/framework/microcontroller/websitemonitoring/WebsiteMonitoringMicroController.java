package com.teleonome.framework.microcontroller.websitemonitoring;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.csvmicrocontroller.CSVReader;
import com.teleonome.framework.utils.Utils;

public class WebsiteMonitoringMicroController extends MicroController {
	private JSONArray configParams;
	StringWriter sw = new StringWriter();
	ArrayList<Map.Entry<JSONObject, Integer>> configParamDeneProcessingRequestQueuePositionIndex = new ArrayList();

	public WebsiteMonitoringMicroController(DenomeManager d, String n) {
		super(d, n);
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
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		configParams=params;
		//
		// now loop over each dene to get the order in the string
		Integer I;
		JSONObject dene;
		
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Processing Request Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				configParamDeneProcessingRequestQueuePositionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(dene, I));
				
				Collections.sort(configParamDeneProcessingRequestQueuePositionIndex, new Comparator<Map.Entry<?, Integer>>(){
					public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}});
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}

	@Override
	public BufferedReader getReader() throws IOException {
		// TODO Auto-generated method stub
		String  dataString="dataString";
		return  new WebsiteMonitoringPlainReader(new StringReader(dataString),configParamDeneProcessingRequestQueuePositionIndex);
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		 BufferedWriter stringWriter = new BufferedWriter(sw) ;
         return stringWriter;

	}

}
