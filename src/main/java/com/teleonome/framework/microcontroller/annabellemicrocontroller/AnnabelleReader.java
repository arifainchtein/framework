package com.teleonome.framework.microcontroller.annabellemicrocontroller;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.exception.ServletProcessingException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.utils.Utils;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
public class AnnabelleReader extends BufferedReader{
	Logger logger;
	BufferedReader reader;
	String command="";
	DenomeManager aDenomeManager;
	Hypothalamus hypothalamus;                                                                                                                                                                                                                            
	private String currentCommand="";
	public AnnabelleReader(BufferedReader in ,Hypothalamus h,DenomeManager d) {
		super(in);
		reader=in;
		hypothalamus=h;
		logger = Logger.getLogger(getClass().getName());
		aDenomeManager=d;
		// TODO Auto-generated constructor stub
		
	}

	public void close() throws IOException {
		logger.info("about to close AnnabellReader");
		//String trace = Utils.generateMethodTrace();
	//	logger.debug(trace);
		super.close();
	}
	public boolean ready() throws IOException {
		
		return reader.ready();
	}
	public void setCurrentCommand(String s) {
		command=s;
		
		logger.debug("setting , command:" + command);
		
	}
	public String readLine(){
		logger.debug("waiting for response for  command:" + command);
		if(command.equals(""))return "";
		if(command.startsWith(TeleonomeConstants.DELETE_TELEPATHON)) {
			logger.debug("returning because its delete telepathon");
			return "Ok";
		}
		
		if(command.startsWith(TeleonomeConstants.DELETE_STALE_TELEPATHONS)) {
			logger.debug("returning because its delete stale telepathon");
			return "Ok";
		}
		
		String line="", className;
		int counter=0;
		int maxTries=1;
		String deserializer, deviceName;
		String[] tokens;
		boolean keepGoing=true;
		AnnabelleDeserializer cc;
		JSONObject telepathon = null;
		boolean appendString=false;
		while(keepGoing) {
			try {
				if(appendString) {
					line = line + reader.readLine().replaceAll("\u0000", "");
				}else {
					line = reader.readLine().replaceAll("\u0000", "");
				}
				
				logger.debug("line 63, received line=" + line);
				if(line==null || line.contains("Ok-") || line.contains("Failure"))
				{
					keepGoing=false;
					appendString=false;
				}else {
					tokens = line.split("#");
					if(tokens.length>46) {
						appendString=false;
						deserializer=tokens[0];
						int l = "deserializer".length();
						if(deserializer.length()>l && deserializer.contains("DigitalStablesDataDeserializer"))deserializer="DigitalStablesDataDeserializer";
						try {	    
							className = "com.teleonome.framework.microcontroller.annabellemicrocontroller." + deserializer;
							logger.debug("className for deserializer =" + className);
							AnnabelleDeserializer annabellDeserializer = AnnabelleDeserializerFactory.createAnnabellDeserializer(className);
							if(annabellDeserializer!=null) {
								annabellDeserializer.setMnemosyneManager(aDenomeManager.getMnemosyneManager());
								String teleonomeName = aDenomeManager.getDenomeName();
								
								telepathon = annabellDeserializer.deserialise(teleonomeName,line);
								long timeSeconds = annabellDeserializer.getTimeSeconds();
								if(telepathon.has(TeleonomeConstants.DENE_NAME_ATTRIBUTE)) {
									String telepathonName = telepathon.getString(TeleonomeConstants.DENE_NAME_ATTRIBUTE);
									logger.debug("about remove and inject telepathonName =" + telepathonName + " telephathon=" + telepathon.toString(4));
									aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_TELEPATHONS, telepathonName);
									aDenomeManager. injectDeneChainIntoNucleus(TeleonomeConstants.NUCLEI_TELEPATHONS,telepathon);
									try {
										aDenomeManager.storeTelepathon(timeSeconds,  telepathonName,  telepathon);
									} catch (PersistenceException e) {
										// TODO Auto-generated catch block
										logger.warn(Utils.getStringException(e));
									}
									
									hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_TELEPATHON_STATUS, telepathon.toString());
								}else {
									logger.debug("Error deserializing " + line);
								}
								
							}else {
								logger.debug("className=" + className + " does not existis");
							}
							
						} catch (ServletProcessingException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					}else {
						appendString=true;
					}
					
				}
				//
				// now process
			}catch(IOException e) {
				logger.warn("Exception reading line, counter=" + counter);
				logger.warn(Utils.getStringException(e));
				counter++;
			}
			if(counter>maxTries) {
				keepGoing=false;
			}
		}
		     
		logger.debug("the response is:   " + line);
		String cleaned="";
		if(line.contains("Ok-") && telepathon!=null && telepathon.has(TeleonomeConstants.DENE_NAME_ATTRIBUTE)) {
			cleaned=line.substring(line.indexOf("Ok-"));;
			cleaned=TeleonomeConstants.HEART_TOPIC_TELEPATHON_STATUS+"#"+telepathon.toString();
		}else if(line.contains("Read fail") && line.contains("#")){
			cleaned=line.substring(line.lastIndexOf("fail")+4);
		}else {
			cleaned=line;
		}
		logger.debug("cleaned:  " + cleaned);
		
		return cleaned;
	}
	}