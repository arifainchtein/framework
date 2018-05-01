package com.teleonome.framework.microcontroller.csvmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.websitemonitoring.WebsiteMonitoringPlainReader;
import com.teleonome.framework.utils.Utils;

public class CSVMicroController extends MicroController {
	StringWriter sw = new StringWriter();
	CSVReader  aCSVReader;
	CSVWriter aCSVWriter;
	JSONArray configParams;
	String pathToFile="";
	Logger logger;
	public CSVMicroController(DenomeManager d, String n) {
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
		JSONArray exportPointerArray;
		Object o;
		Integer exportColumnPosition;
		String pointerToExportIdentity;
		String exportFileName="";
		String exportOperation="";
		ArrayList<Map.Entry<String, Integer>> pointerToExportIdentityExportColumnPositionIndex = new ArrayList();

		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				//
				// first to see if this parameter is the one the Reader uses to locate the file to read from
				//
				o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "PathToFile", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(o!=null && o instanceof String) {
					pathToFile=o.toString();
				}else {
					//
					// this dene is not for the reader
					o=DenomeUtils.getDeneWordAttributeByDeneWordTypeFromDene(dene, TeleonomeConstants.DENEWORD_TYPE_DENEWORD_EXPORT_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(o!=null) {
						pointerToExportIdentity=o.toString();
						//
						// if we are is because there is a deneword of type export pointer, which means there has to be a deneword DENEWORD_TYPE_DENEWORD_EXPORT_COLUMN_POSITION
						exportColumnPosition=(Integer)DenomeUtils.getDeneWordAttributeByDeneWordTypeFromDene(dene, TeleonomeConstants.DENEWORD_TYPE_DENEWORD_EXPORT_COLUMN_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						
						pointerToExportIdentityExportColumnPositionIndex.add(new AbstractMap.SimpleEntry<String, Integer>(pointerToExportIdentity, exportColumnPosition));
						Collections.sort(pointerToExportIdentityExportColumnPositionIndex, new Comparator<Map.Entry<?, Integer>>(){
							public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
								return o1.getValue().compareTo(o2.getValue());
							}});
					}else {
						//
						// check for the file Name of the export functionality
						//
						o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Export File Name", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(o!=null && o instanceof String) {
							exportFileName=o.toString();
						}else {
							//
							// check to see if this is the export type parameter
							o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, "Export Operation", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							if(o!=null && o instanceof String) {
								exportOperation=o.toString();
							}
						}
					}
				}
				
				
				logger.debug(" cvs microcontroller pathToFile " + pathToFile);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		StringWriter sw = new StringWriter();
		 aCSVWriter= new CSVWriter(sw, aCSVReader);
		 aCSVReader= new CSVReader(new StringReader(""), sw, Utils.getLocalDirectory() + pathToFile);
		 
	}
	

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		aCSVReader= new CSVReader(new StringReader(""), sw, Utils.getLocalDirectory() + pathToFile);
		
		aCSVWriter= new CSVWriter(sw, aCSVReader);  
         return aCSVWriter;

	}


	@Override
	public BufferedReader getReader() throws IOException {
		String  dataString="dataString";
		 
		return  aCSVReader; 
		
	}
}
