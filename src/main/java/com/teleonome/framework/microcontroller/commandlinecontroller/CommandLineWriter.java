package com.teleonome.framework.microcontroller.commandlinecontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import com.teleonome.framework.utils.Utils;

public class CommandLineWriter  extends BufferedWriter{

		CommandLineReader aCommandLineReader;
		Logger logger;
		
		
		public CommandLineWriter(Writer out, CommandLineReader c) {
			super(out);
			aCommandLineReader=c;
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub
		}

		public void write(String command, int off, int len) throws IOException {

			if(command.equals("GetSensorData")){
				aCommandLineReader.setCurrentCommand(command);
			}else{
				try {
					//
					// the command is a hash separated string that has the 
					// following structure:
					// token[0]=the name of the command to be called
					// token[1]... token[n] parameters that will be passed with the command 
					//
					// as an example to execute the command testping 64
					// the actuator command true expression would be
					//testping#64
					ArrayList<String> results = Utils.executeCommand(command);
					logger.info("results=" + results);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}
		}

	}
