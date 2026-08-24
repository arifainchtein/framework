package com.teleonome.framework.microcontroller.arduinouno;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.teleonome.framework.utils.Utils;

/**
 * jSerialComm-era port of the old RXTX-based GNUArduinoReader (deleted in
 * commit 3d13059, "removing rxtx and adding jSerialComm") - this class was
 * pure java.io wrapping with no RXTX dependency, so the retry-on-IOException
 * and response-cleaning logic carries over unchanged.
 */
public class ArduinoUnoReader extends BufferedReader {
	Logger logger;
	BufferedReader reader;
	String command = "";

	public ArduinoUnoReader(BufferedReader in) {
		super(in);
		reader = in;
		logger = Logger.getLogger(getClass());
		logger.debug("Just Created an ArduinoUnoReader");
	}

	public void close() throws IOException {
		logger.info("about to close ArduinoUnoReader");
		String trace = Utils.generateMethodTrace();
		logger.debug(trace);
		super.close();
	}

	public boolean ready() throws IOException {
		return reader.ready();
	}

	public void setCurrentCommand(String s) {
		command = s;
	}

	public String readLine() {
		logger.debug("about to send readline, command:" + command);
		String line = "";
		int counter = 0;
		int maxTries = 3;
		boolean keepGoing = true;
		while (keepGoing) {
			try {
				line = reader.readLine();
				keepGoing = false;
			} catch (IOException e) {
				logger.warn("Exception reading line, counter=" + counter);
				counter++;
			}
			if (counter > maxTries) {
				keepGoing = false;
			}
		}

		logger.debug("the response is:   " + line);
		String cleaned = "";
		if (line.contains("Ok-")) {
			cleaned = line.substring(line.indexOf("Ok-"));
		} else if (line.contains("Read fail") && line.contains("#")) {
			cleaned = line.substring(line.lastIndexOf("fail") + 4);
		} else {
			cleaned = line;
		}
		logger.debug("cleaned:  " + cleaned);

		return cleaned;
	}
}
