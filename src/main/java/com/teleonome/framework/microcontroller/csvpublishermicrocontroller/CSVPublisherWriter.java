package com.teleonome.framework.microcontroller.csvpublishermicrocontroller;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.utils.Utils;

public class CSVPublisherWriter extends BufferedWriter{
	private ArrayList<Map.Entry<String, Integer>> pointerToExportIdentityExportColumnPositionIndex;
	private DenomeManager aDenomeManager;
	private String exportFileName;
	private String exportOperation;
	Logger logger;
	
	public CSVPublisherWriter(Writer out,  String fN, String eo,ArrayList<Map.Entry<String, Integer>> p, DenomeManager d) {
		super(out);
		logger = Logger.getLogger(getClass());
		pointerToExportIdentityExportColumnPositionIndex = p;
		aDenomeManager=d;
		exportFileName=fN;
		exportOperation=eo;
		logger.debug("exportFileName=" + exportFileName);
		// TODO Auto-generated constructor stub
	}
	
	public void write(String command, int off, int len) throws IOException {
		logger.debug("receive command " + command);
		if(command.startsWith("CSV Export")) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(aDenomeManager.getcurrentlyCreatingPulseTimestampMillis() +","+ aDenomeManager.getcurrentlyCreatingPulseTimestamp());
			
			String pointer;
			JSONObject valueDeneWord;
			Identity identity;
			String valueType="";
			StringBuffer headerBuffer = new StringBuffer();
			headerBuffer.append("TimestampMillis,Timestamp");
			logger.debug("receive pointerToExportIdentityExportColumnPositionIndex " + pointerToExportIdentityExportColumnPositionIndex);
			for (Map.Entry<String, Integer> entry2 : pointerToExportIdentityExportColumnPositionIndex) {
				pointer = entry2.getKey();
				logger.debug("pointer=" + pointer );
				buffer.append(",");
				headerBuffer.append(",");
				try {
					identity = new Identity(pointer);
					headerBuffer.append(identity.getDeneWordName());
					valueDeneWord = (JSONObject) aDenomeManager.getDeneWordAttributeByIdentity(identity, TeleonomeConstants.COMPLETE);
					valueType = valueDeneWord.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
					if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)) {
						buffer.append(valueDeneWord.getDouble(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
					}else if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)) {
						buffer.append(valueDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
					}else if(valueType.equals(TeleonomeConstants.DATATYPE_LONG)) {
						buffer.append(valueDeneWord.getLong(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
					}else if(valueType.equals(TeleonomeConstants.DATATYPE_STRING)) {
						buffer.append(valueDeneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
					}
				} catch (InvalidDenomeException | JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				  
			}
			String ops = "";//
			// now check t see if the value for filename contains $Date_For_File_Export
			// if so replace that substring for the actual date
			if(exportFileName.contains(TeleonomeConstants.COMMANDS_DATE_FOR_EXPORT_FILENAME)) {
				exportFileName = exportFileName.replace(TeleonomeConstants.COMMANDS_DATE_FOR_EXPORT_FILENAME, Utils.renderCommand(TeleonomeConstants.COMMANDS_DATE_FOR_EXPORT_FILENAME));
			}
			//
			// check to see if the exportfilename begins with COMMAND_LOCATION_WEBSERVER_ROOT
			// if so, then you must save it to the webroot, which is /home/pi/Teleonome/tomcat/webapps/ROOT/
			//
			if(exportFileName.startsWith(TeleonomeConstants.COMMAND_LOCATION_WEBSERVER_ROOT)) {
				exportFileName = exportFileName.replace(TeleonomeConstants.COMMAND_LOCATION_WEBSERVER_ROOT, "/home/pi/Teleonome/tomcat/webapps/ROOT");
			}
			
			logger.debug("rendered file name to " + exportFileName);
				
			if(exportOperation.equals(TeleonomeConstants.EXPORT_OPERATION_CREATE)) {
				try {
					String complete = headerBuffer.toString() + System.lineSeparator() + buffer.toString() + System.lineSeparator();
					logger.debug("about to create file " + exportFileName + " " + buffer.toString());
				    Files.write(Paths.get(exportFileName), complete.getBytes(), StandardOpenOption.CREATE);
				}catch (IOException e) {
				    //exception handling left as an exercise for the reader
					logger.warn(Utils.getStringException(e));
				}
			}else if(exportOperation.equals(TeleonomeConstants.EXPORT_OPERATION_APPEND)) {
				try {
					if(!Files.exists( Paths.get(Utils.getLocalDirectory() + exportFileName))) {
						String complete = headerBuffer.toString() + System.lineSeparator() + buffer.toString() + System.lineSeparator();
						logger.debug("about to create because no fule file " + exportFileName + " " +complete);
					    Files.write( Paths.get(exportFileName), complete.getBytes(), StandardOpenOption.CREATE);
						
					}else {
						String complete =  buffer.toString() + System.lineSeparator();
						logger.debug("about to append file " + exportFileName + " " + complete);
					    Files.write( Paths.get( exportFileName), complete.getBytes(), StandardOpenOption.APPEND);
					}
				}catch (IOException e) {
				    //exception handling left as an exercise for the reader
					logger.warn(Utils.getStringException(e));
				}
			}
			
		}
	}
}
