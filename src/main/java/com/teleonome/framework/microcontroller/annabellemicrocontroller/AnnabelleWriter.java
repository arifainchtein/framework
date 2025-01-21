	package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import com.teleonome.framework.utils.Utils;

public class AnnabelleWriter  extends BufferedWriter{

		AnnabelleReader aCajalReader;
		Logger logger;
		Writer output;
		long lastCommandTime=0;
		long minimumIntervalBetweenCommands=2000;
		
		public AnnabelleWriter(Writer out, AnnabelleReader c) {
			super(out);
			aCajalReader=c;
			output=out;
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub
		}

		public void close() throws IOException {
			logger.info("about to close CajalWriter");
			//String trace = Utils.generateMethodTrace();
		//	logger.debug(trace);
			super.close();
		}
		public AnnabelleReader getReader() {
			return aCajalReader;
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
			aCajalReader.setCurrentCommand(command);
			logger.debug("sending  command:" + command);
			output.write(command,0,command.length());
//			if(command.equals("GetSensorData")){
//				aCajalReader.setCurrentCommand(command);
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
