package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.BufferedReader;
import java.io.Reader;

import org.apache.log4j.Logger;

/**
 * Answers the command CleotildeWriter last received (same pattern as NetworkInspectorReader):
 * GetSensorData reads the router and the board and returns the sensor line; the result of an
 * action is its board answer or "Ok".
 */
public class CleotildeReader extends BufferedReader {

	Logger logger;
	private final CleotildeMicroController controller;
	private String currentCommand = "";
	private String actionResult = "Ok";

	public CleotildeReader(Reader in, CleotildeMicroController c) {
		super(in);
		controller = c;
		logger = Logger.getLogger(getClass());
	}

	@Override
	public String readLine() {
		logger.debug("CleotildeReader currentCommand=" + currentCommand);
		if (currentCommand.equals("GetSensorData")) {
			return controller.buildSensorLine();
		}
		return actionResult;
	}

	public void setCurrentCommand(String command) {
		currentCommand = command;
		actionResult = "Ok";
	}

	public void setActionResult(String result) {
		actionResult = result;
	}
}
