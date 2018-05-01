package com.teleonome.framework.microcontroller.csvpublishermicrocontroller;



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

public class CSVPublisherMicroController extends MicroController {
	CSVPublisherWriter aCSVPublisherWriter;
	JSONArray configParams;
	String pathToFile="";
	Logger logger;
	public CSVPublisherMicroController(DenomeManager d, String n) {
		super(d, n);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}



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



				logger.debug(" cvs microcontroller pathToFile " + pathToFile);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		StringWriter sw = new StringWriter();
		aCSVPublisherWriter= new CSVPublisherWriter(sw,exportFileName,exportOperation, pointerToExportIdentityExportColumnPositionIndex, aDenomeManager);
	}


	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		logger.debug("requesting the writer");
		return aCSVPublisherWriter;

	}


	@Override
	public BufferedReader getReader() throws IOException {

		return  new CVSPublisherReader(new StringReader("")); 

	}
}
