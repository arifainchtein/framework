package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

/**
 * Wraps the board's serial output (TtySerialPort, see CleotildeMicroController). Receives the
 * commands the pulse and the actuator actions send (same pattern as NetworkInspectorWriter) and
 * hands them to CleotildeMicroController; board commands go out through sendToBoard().
 */
public class CleotildeWriter extends BufferedWriter {

	Logger logger;
	private final CleotildeReader reader;
	private final CleotildeMicroController controller;

	public CleotildeWriter(Writer out, CleotildeReader r, CleotildeMicroController c) {
		super(out);
		reader = r;
		controller = c;
		logger = Logger.getLogger(getClass());
	}

	@Override
	public void write(String command, int off, int len) throws IOException {
		reader.setCurrentCommand(command);
		String[] tokens = command.split("#");
		if (command.equals("Generate Analysis")) {
			controller.startAnalysis();
		} else if (command.startsWith("AddDeviceToWhiteList")) {
			controller.addDeviceToWhiteList(tokens[1], tokens[2]);
		} else if (command.startsWith("RemoveDeviceFromWhiteList")) {
			controller.removeDeviceFromWhiteList(tokens[1], tokens.length > 2 ? tokens[2] : "");
		} else if (command.startsWith("ForgetDevice")) {
			controller.forgetDevice(tokens[1], tokens.length > 2 ? tokens[2] : "");
		} else if (command.startsWith("AddAllDevicesToWhiteList")) {
			controller.addAllDevicesToWhiteList();
		} else if (command.startsWith("PowerCycle")) {
			reader.setActionResult(controller.powerCycle(tokens.length > 1 ? tokens[1] : ""));
		} else if (!command.equals("GetSensorData")) {
			logger.debug("Cleotilde ignoring command " + command);
		}
	}

	/**
	 * Sends one command to the board exactly as given, with no line ending: the firmware reads a
	 * command as whatever arrives within 100 ms (Serial.readString), so send one and wait for its
	 * answer before the next.
	 */
	void sendToBoard(String command) throws IOException {
		super.write(command, 0, command.length());
		super.flush();
	}
}
