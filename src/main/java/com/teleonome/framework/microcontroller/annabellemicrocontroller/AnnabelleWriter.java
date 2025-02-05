	package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.utils.Utils;

public class AnnabelleWriter  extends BufferedWriter{
		DenomeManager aDenomeManager;
		AnnabelleReader aAnnabelleReader;
		Logger logger;
		Writer output;
		long lastCommandTime=0;
		long minimumIntervalBetweenCommands=2000;
		
		public AnnabelleWriter(Writer out, AnnabelleReader c, DenomeManager d) {
			super(out);
			aAnnabelleReader=c;
			aDenomeManager=d;
			output=out;
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub
		}

		public void close() throws IOException {
			logger.info("about to close AnnabelleWriter");
			//String trace = Utils.generateMethodTrace();
		//	logger.debug(trace);
			super.close();
		}
		public AnnabelleReader getReader() {
			return aAnnabelleReader;
		}
		public void write(String command, int off, int len) throws IOException {
			long now=System.currentTimeMillis();
			long diff = now-lastCommandTime;
			if(minimumIntervalBetweenCommands>diff) {
				long sleeptime = minimumIntervalBetweenCommands-diff;
				logger.debug("Sleeping for " + sleeptime+ "milliseconds before sending command");
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				logger.debug("Sending command since its been " + diff+ "since the last  command");
			}
			aAnnabelleReader.setCurrentCommand(command);
			 if(command.startsWith(TeleonomeConstants.DELETE_TELEPATHON)){
					String[] tokens = command.split("#");
					String telepathonName=tokens[1];
					logger.debug("RemoveTelepathon telepathonName=" + telepathonName);
					aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_TELEPATHONS, telepathonName);
				}
			logger.debug("sending  command:" + command);
			output.write(command,0,command.length());
//			if(command.equals("GetSensorData")){
//				aAnnabelleReader.setCurrentCommand(command);
//			}else{
//				try {
//					//
//					// the command is a hash separated string that has the 
//					// following structure:
//					// token[0]=the name of the command to be called
//					// token[1]... token[n] parameters that will be passed with the command 
//					//
//					// as an example to execute the command testping 64
//					// the actuator command true expression would be
//					//testping#64
//					ArrayList<String> results = Utils.executeCommand(command);
//					logger.info("results=" + results);
//				} catch (IOException | InterruptedException e) {
//					// TODO Auto-generated catch block
//					logger.warn(Utils.getStringException(e));
//				}
//			}
		}

	}
