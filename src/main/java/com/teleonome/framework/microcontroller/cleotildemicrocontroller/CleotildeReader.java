package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Logger;

import com.teleonome.framework.microcontroller.TtySerialPort;

/**
 * Wraps the board's serial input (TtySerialPort, see CleotildeMicroController) and answers the
 * command CleotildeWriter last received: GetSensorData returns the full sensor line (board +
 * router readings, built by the controller); an action returns its result or "Ok".
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

	/** Drops whatever the board printed since the last command (debug output, WiFi manager). */
	void dropStaleInput() throws IOException {
		while (super.ready()) super.read();
	}

	/**
	 * Reads lines from the board until one starts with expectedPrefix (null means the
	 * "Ok-"/"Failure-" acknowledgement), skipping its debug lines. Null if nothing matched within
	 * timeoutMillis. A read timeout (nothing for BOARD_READ_TIMEOUT_MILLIS) just keeps waiting
	 * until the deadline; any other IOException means the port is gone and is passed on so the
	 * controller can reopen it.
	 */
	String readBoardAnswer(String expectedPrefix, long timeoutMillis) throws IOException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (System.currentTimeMillis() < deadline) {
			String line;
			try {
				line = super.readLine();
			} catch (TtySerialPort.SerialTimeoutException e) {
				continue;
			}
			if (line == null) break;
			line = line.trim();
			if (line.isEmpty()) continue;
			boolean expected = expectedPrefix == null
					? line.startsWith("Ok-") || line.startsWith("Failure-")
					: line.startsWith(expectedPrefix);
			if (expected) return line;
			logger.debug("Cleotilde board said: " + line);
		}
		return null;
	}
}
