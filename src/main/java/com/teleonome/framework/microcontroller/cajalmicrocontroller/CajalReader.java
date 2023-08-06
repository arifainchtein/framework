package com.teleonome.framework.microcontroller.cajalmicrocontroller;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.ServletProcessingException;
import com.teleonome.framework.utils.Utils;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
public class CajalReader extends BufferedReader{
	Logger logger;
	BufferedReader reader;
	String command="";
	DenomeManager aDenomeManager;
	                                                                                                                                                                                                                               
	private String currentCommand="";
	public CajalReader(BufferedReader in ,DenomeManager d) {
		super(in);
		reader=in;
		logger = Logger.getLogger(getClass().getName());
		aDenomeManager=d;
		// TODO Auto-generated constructor stub
		
	}

	public void close() throws IOException {
		logger.info("about to close GNUArduinoReader");
		String trace = Utils.generateMethodTrace();
		logger.debug(trace);
		super.close();
	}
	public boolean ready() throws IOException {
		
		return reader.ready();
	}
	public void setCurrentCommand(String s) {
		command=s;
	}
	public String readLine(){
		logger.debug("about to send readline, command:" + command);
		//if(command.equals(""))return "";
		
		String line="", className;
		int counter=0;
		int maxTries=3;
		String deviceType, deviceName;
		String[] tokens;
		boolean keepGoing=true;
		CajalDeserializer aCajalDeserializer;
		JSONObject telephathon;
		while(keepGoing) {
			try {
				line = reader.readLine();
				logger.debug("received line=" + line);
				if(line==null || line.contains("Ok-")) {
					keepGoing=false;
				}else {
					tokens = line.split("#");
					deviceType=tokens[0];
					
					try {	
						className = "com.teleonome.framework.microcontroller.cajalmicrocontroller." + deviceType + "Deserializer";
						logger.debug("className for deserializer =" + className);
						aCajalDeserializer = CajalDeserializerFactory.createCajalDeserializer(className);
						aCajalDeserializer.setMnemosyneManager(aDenomeManager.getMnemosyneManager());
						String teleonomeName = aDenomeManager.getDenomeName();
						telephathon = aCajalDeserializer.deserialise(teleonomeName,line);
						String telepathonName = telephathon.getString(TeleonomeConstants.DENE_NAME_ATTRIBUTE);
						aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_TELEPATHONS, telepathonName);
						aDenomeManager. injectDeneChainIntoNucleus(TeleonomeConstants.NUCLEI_TELEPATHONS,telephathon);
					} catch (ServletProcessingException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
				}
				//
				// now process
			}catch(IOException e) {
				logger.warn("Exception reading line, counter=" + counter);
				counter++;
			}
			if(counter>maxTries) {
				keepGoing=false;
			}
		}
		     
		logger.debug("the response is:   " + line);
		String cleaned="";
		if(line.contains("Ok-")) {
			cleaned=line.substring(line.indexOf("Ok-"));;
		}else if(line.contains("Read fail") && line.contains("#")){
			cleaned=line.substring(line.lastIndexOf("fail")+4);
		}else {
			cleaned=line;
		}
		logger.debug("cleaned:  " + cleaned);
		
		return cleaned;
	}
	}