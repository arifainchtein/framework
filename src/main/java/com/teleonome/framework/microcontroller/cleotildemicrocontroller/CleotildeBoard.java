package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;

import com.fazecast.jSerialComm.SerialPort;
import com.teleonome.framework.utils.Utils;

/**
 * The Cleotilde ESP32 board (Wally 17 + Paula) on USB serial. Protocol and GetSensorData field
 * positions: DigitalStables/Projects/Cleotilde/CLAUDE.md. Every command is answered with one line:
 * "Ok-...", "Failure-...", or for GetSensorData a '#' line starting with "Cleotilde#".
 * The board's own debug output and WiFi manager messages share the line, so unrelated lines are
 * skipped until the expected answer or the timeout.
 */
public class CleotildeBoard {

	private final String portName;
	private final int dataRate;
	private SerialPort port = null;
	Logger logger;

	public CleotildeBoard(String p, int rate) {
		portName = p;
		dataRate = rate;
		logger = Logger.getLogger(getClass());
	}

	public synchronized boolean isOpen() {
		return port != null && port.isOpen();
	}

	public synchronized boolean open() {
		close();
		SerialPort candidate = SerialPort.getCommPort(portName);
		candidate.setComPortParameters(dataRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
		candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
		candidate.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		// DTR/RTS low: on Wally they drive EN/IO0 through the auto-reset circuit, so opening the
		// port must not reset the ESP32 or put it in download mode.
		candidate.clearDTR();
		candidate.clearRTS();
		if (!candidate.openPort()) {
			logger.warn("Could not open Cleotilde board on " + portName);
			return false;
		}
		port = candidate;
		logger.info("Cleotilde board opened on " + portName);
		return true;
	}

	public synchronized void close() {
		if (port != null && port.isOpen()) port.closePort();
		port = null;
	}

	/**
	 * Sends one command and returns the board's answer, or null if there was none within
	 * timeoutMillis. expectedPrefix null means "the Ok-/Failure- acknowledgement".
	 */
	public synchronized String command(String command, String expectedPrefix, long timeoutMillis) {
		if (!isOpen() && !open()) return null;
		try {
			InputStream in = port.getInputStream();
			OutputStream out = port.getOutputStream();
			while (port.bytesAvailable() > 0) in.read();  // drop stale output
			out.write(command.getBytes(StandardCharsets.UTF_8));
			out.flush();

			long deadline = System.currentTimeMillis() + timeoutMillis;
			StringBuilder line = new StringBuilder();
			while (System.currentTimeMillis() < deadline) {
				if (port.bytesAvailable() <= 0) {
					Thread.sleep(10);
					continue;
				}
				int c = in.read();
				if (c < 0) continue;
				if (c == '\n') {
					String answer = line.toString().trim();
					line.setLength(0);
					if (answer.isEmpty()) continue;
					boolean expected = expectedPrefix == null
							? answer.startsWith("Ok-") || answer.startsWith("Failure-")
							: answer.startsWith(expectedPrefix);
					if (expected) return answer;
					logger.debug("Cleotilde board said: " + answer);
				} else if (c != '\r') {
					line.append((char) c);
				}
			}
			logger.warn("No answer from Cleotilde board to " + command);
		} catch (IOException | InterruptedException e) {
			logger.warn(Utils.getStringException(e));
			close();  // reopened on the next command
		}
		return null;
	}

	public String command(String command) {
		return command(command, null, 2000);
	}

	/** The board's GetSensorData line split on '#', or null if the board didn't answer. */
	public String[] getSensorData() {
		String line = command("GetSensorData", "Cleotilde#", 2000);
		return line == null ? null : line.split("#", -1);
	}

	/** One colour letter per LED (G Y R B W O, lower case blinks), LEDs 1-5. */
	public boolean setLeds(String... colours) {
		StringBuilder c = new StringBuilder("SetLeds");
		for (String colour : colours) c.append('#').append(colour);
		String answer = command(c.toString());
		return answer != null && answer.startsWith("Ok-");
	}

	/** OLED page 1-6: a title and up to 6 lines of about 21 characters. */
	public boolean oledPage(int page, String title, String... lines) {
		StringBuilder c = new StringBuilder("Oled#").append(page).append('#').append(clean(title));
		for (String l : lines) c.append('#').append(clean(l));
		String answer = command(c.toString());
		return answer != null && answer.startsWith("Ok-");
	}

	private static String clean(String s) {
		if (s == null) return "";
		String t = s.replace('#', ' ').replace('\n', ' ');
		return t.length() > 21 ? t.substring(0, 21) : t;
	}
}
