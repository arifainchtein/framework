package com.teleonome.framework.microcontroller.arduinouno;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fazecast.jSerialComm.SerialPort;
import com.teleonome.framework.LifeCycleEventListener;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MotherMicroController;
import com.teleonome.framework.utils.Utils;

/**
 * jSerialComm-era replacement for the old RXTX-based GNUArduinoUno, deleted
 * in commit 3d13059 ("removing rxtx and adding jSerialComm") without ever
 * getting a direct replacement - every other Teleonome had already moved to
 * PLSeriesMicroController/AnnabelleController by then, but Sento still talks
 * to a plain Arduino Uno over serial. Protocol and denome-config reads are
 * ported unchanged from GNUArduinoUno; only the serial API calls are swapped
 * for their jSerialComm equivalents, following the same translation already
 * established in PLSeriesMicroController.initializeSerialComm().
 */
public class ArduinoUno extends MotherMicroController implements LifeCycleEventListener {

	Logger logger;
	private static final String PORT_NAMES[] = { "/dev/tty.usbmodem641", "/dev/ttyACM0", "/dev/ttyAMA0",
			"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/cu.usbmodem1411" };
	SerialPort serialPort = null;
	private ArduinoUnoReader input;
	private BufferedWriter output;

	private int DATA_RATE = 9600;
	private int INTER_SENSOR_READ_TIME_OUT_MILLISECONDS = 100;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;

	public ArduinoUno(Hypothalamus h, DenomeManager d, String n) {
		super(h, d, n);
		logger = Logger.getLogger(getClass());
		setEnableAsyncUpdate(true);
	}

	public void processLifeCycleEvent(String lifeCycleEvent) {
		logger.debug("processing life cycle event=" + lifeCycleEvent);
		try {
			if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)) {
				sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE);
			} else if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
				sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE);
			} else if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE)) {
				sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE);
			} else if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE)) {
				sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE);
			} else if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE)) {
				sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE);
			}
		} catch (IOException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	@Override
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		try {
			INTER_SENSOR_READ_TIME_OUT_MILLISECONDS = ((Integer) aDenomeManager.getDeneWordValueByName(
					TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE,
					TeleonomeConstants.DENE_VITAL, "Inter Sensor Read Timeout Milliseconds")).intValue();
		} catch (InvalidDenomeException e1) {
			logger.warn(Utils.getStringException(e1));
			System.exit(-1);
		}

		SerialPort portId = null;
		SerialPort[] allPorts;
		int counter = 0;
		int maxNumberReconnects = 3;
		boolean keepGoing = true;
		do {
			allPorts = SerialPort.getCommPorts();
			logger.debug("looking for ports, found " + allPorts.length + " ports");
			for (SerialPort port : allPorts) {
				logger.debug("looking for ports, currPortId=" + port.getSystemPortName());
				for (String portName : PORT_NAMES) {
					if (port.getSystemPortName().equals(portName) || port.getSystemPortName().startsWith(portName)) {
						portId = port;
						break;
					}
				}
				if (portId != null) break;
			}
			if (portId == null) {
				if (counter <= maxNumberReconnects) {
					counter++;
					logger.info("Could not find Serial Port," + counter + " out of " + maxNumberReconnects);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					logger.warn("Could not find COM port.");
					Hashtable<String, String> h = new Hashtable();
					h.put("message", "Could not find COM port");
					throw new MicrocontrollerCommunicationException(h);
				}
			} else {
				keepGoing = false;
			}
		} while (keepGoing);

		logger.debug("Found COM Port1.");
		try {
			JSONArray allDenes = (JSONArray) aDenomeManager.getDeneByDeneType(TeleonomeConstants.NUCLEI_INTERNAL,
					TeleonomeConstants.DENECHAIN_COMPONENTS, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);
			JSONObject arduinoUnoMicrocontrollerDene = null;
			JSONObject microcontrollerDene = null;
			String className = "";
			for (int i = 0; i < allDenes.length(); i++) {
				microcontrollerDene = allDenes.getJSONObject(i);
				className = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(microcontrollerDene,
						TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME,
						TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (className.equals("com.teleonome.framework.microcontroller.arduinouno.ArduinoUno")) {
					arduinoUnoMicrocontrollerDene = microcontrollerDene;
				}
			}
			if (arduinoUnoMicrocontrollerDene == null) {
				logger.warn("Could not find Arduino Uno Microcontroller Dene");
				Hashtable<String, String> h = new Hashtable();
				h.put("message", "Could not find COM port");
				throw new MicrocontrollerCommunicationException(h);
			}
			try {
				Object o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(arduinoUnoMicrocontrollerDene,
						TeleonomeConstants.DENEWORD_MICROCONTROLLER_ASYNC_REQUEST_DELAY_MILLIS,
						TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (o != null) {
					asyncRequestMillisecondsDelay = (int) o;
					logger.info("Using asyncRequestMillisecondsDelay=" + asyncRequestMillisecondsDelay);
				} else {
					logger.info("Did not find asyncRequestMillisecondsDelay");
				}
			} catch (Exception e) {
				logger.warn(Utils.getStringException(e));
			}

			JSONObject configDene;
			logger.debug(" configParams.size= " + configParams.length());
			for (int i = 0; i < configParams.length(); i++) {
				try {
					configDene = configParams.getJSONObject(i);
					logger.debug(" configDene.getString(Name)= " + configDene.getString("Name"));
					if (configDene.getString("Name").equals("Serial Data Rate")) {
						DATA_RATE = ((Integer) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(configDene,
								"Serial Data Rate", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)).intValue();
						logger.debug(" arduino uno microcontroller DATA_RATE " + DATA_RATE);
					}
				} catch (JSONException e) {
					logger.warn(Utils.getStringException(e));
				}
			}

			logger.debug("using datarate=" + DATA_RATE);
			counter = 0;
			boolean openAndTested = false;
			logger.debug("about to open port , sleeping 10 sec first");
			Thread.sleep(10000);

			serialPort = portId;
			serialPort.setComPortParameters(DATA_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
			serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
					30000, 5000);
			serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);

			if (!serialPort.openPort()) {
				logger.warn("Failed to open serial port");
				Hashtable<String, String> h = new Hashtable();
				h.put("message", "Failed to open serial port");
				throw new MicrocontrollerCommunicationException(h);
			}
			logger.debug("opened port , sleeping another 10 sec ");
			Thread.sleep(10000);
			serialPort.setDTR();

			do {
				serialPortInputStream = serialPort.getInputStream();
				serialPortOutputStream = serialPort.getOutputStream();

				if (serialPortInputStream == null) {
					logger.warn("serialPortInputStream is null.");
					throw new SerialPortCommunicationException("SerialPortInputStream is null");
				}
				if (serialPortOutputStream == null) {
					logger.warn("serialPortOutputStream is null.");
					throw new SerialPortCommunicationException("SerialPortOutputStream is null");
				}

				input = new ArduinoUnoReader(new BufferedReader(new InputStreamReader(serialPortInputStream)));
				output = new BufferedWriter(new OutputStreamWriter(serialPortOutputStream));

				try {
					logger.info("About to ping");
					String actuatorCommand = "Ping";
					output.write(actuatorCommand, 0, actuatorCommand.length());
					Thread.sleep(1000);
					output.flush();
					logger.info("waiting for mother to answer");

					String inputLine = input.readLine();
					logger.info("mother answered =" + inputLine);

					openAndTested = true;
				} catch (IOException e) {
					logger.warn(Utils.getStringException(e));
				}
				if (!openAndTested) {
					logger.warn("Ping Failed, retrying in 10 secs, counter=" + counter);
					counter++;
					Thread.sleep(10000);
				}
			} while (!openAndTested);

			logger.debug("finished initializing Arduino Uno");

		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String callStack = sw.toString();
			logger.warn(callStack);
		}
	}

	public ArduinoUnoReader getReader() throws IOException {
		input = new ArduinoUnoReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream())));
		return input;
	}

	public BufferedWriter getWriter() throws IOException {
		output = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
		return output;
	}

	public boolean verifyUserCommandCode(String userCode) throws IOException {
		String actuatorCommand = "VerifyUserCode#" + userCode;
		String result = "";
		boolean toReturn = true;
		boolean keepGoing = true;
		int maxTries = 5;
		int counter = 0;
		while (keepGoing) {
			result = sendCommand(actuatorCommand);
			logger.debug("actuatorCommandt=" + actuatorCommand + " resultr=" + result);

			if (result.equals(TeleonomeConstants.COMMAND_REQUEST_INVALID_CODE)
					|| result.equals(TeleonomeConstants.COMMAND_REQUEST_VALID_CODE)) {
				toReturn = result.equals(TeleonomeConstants.COMMAND_REQUEST_VALID_CODE);
				keepGoing = false;
			} else {
				counter++;
				logger.debug("bad response to validate user result=" + result + " asking again,counter=" + counter
						+ " maxTries=" + maxTries);
				if (counter >= maxTries) {
					toReturn = false;
					keepGoing = false;
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return toReturn;
	}

	@Override
	public String getCommandCode() throws IOException {
		String actuatorCommand = "GetCommandCode";
		boolean keepGoing = true;
		String commandCode = "";
		int maxTries = 5;
		int counter = 0;
		while (keepGoing) {
			commandCode = sendCommand(actuatorCommand);
			if (commandCode != null && commandCode.length() == 6) {
				keepGoing = false;
			} else {
				counter++;
				logger.debug("bad command code=" + commandCode + " asking again,counter=" + counter + " maxTries="
						+ maxTries);
				if (counter >= maxTries) {
					commandCode = "999999";
					keepGoing = false;
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return commandCode;
	}

	@Override
	public String getDigitalGeppettoCommandCode() throws IOException {
		String actuatorCommand = "GetDigitalGeppettoCommandCode";
		boolean keepGoing = true;
		String commandCode = "";
		int maxTries = 5;
		int counter = 0;
		while (keepGoing) {
			commandCode = sendCommand(actuatorCommand);
			if (commandCode != null && commandCode.length() == 6) {
				keepGoing = false;
			} else {
				counter++;
				logger.debug("bad command code=" + commandCode + " asking again,counter=" + counter + " maxTries="
						+ maxTries);
				if (counter >= maxTries) {
					commandCode = "999999";
					keepGoing = false;
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return commandCode;
	}

	@Override
	public boolean rebootingHypothalamus() throws IOException {
		String result = "";
		boolean toReturn = true;
		boolean keepGoing = true;
		int maxTries = 5;
		int counter = 0;
		while (keepGoing) {
			result = sendCommand(TeleonomeConstants.MOTHER_COMMAND_REBOOT_HYPOTHALAMUS);
			if (result.equals(TeleonomeConstants.MOTHER_COMMAND_REBOOT_HYPOTHALAMUS_OK)) {
				toReturn = true;
				keepGoing = false;
			} else {
				counter++;
				logger.debug("bad response to rebootingHypothalamus  result=" + result + " asking again,counter="
						+ counter + " maxTries=" + maxTries);
				if (counter >= maxTries) {
					toReturn = false;
					keepGoing = false;
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return toReturn;
	}

	@Override
	public boolean shuttingDownHypothalamus() throws IOException {
		String result = "";
		boolean toReturn = true;
		boolean keepGoing = true;
		int maxTries = 5;
		int counter = 0;
		while (keepGoing) {
			result = sendCommand(TeleonomeConstants.MOTHER_COMMAND_SHUTDOWN_HYPOTHALAMUS);
			if (result.equals(TeleonomeConstants.MOTHER_COMMAND_SHUTDOWN_HYPOTHALAMUS_OK)) {
				toReturn = true;
				keepGoing = false;
			} else {
				counter++;
				logger.debug("bad response to shuttingDownHypothalamus  result=" + result + " asking again,counter="
						+ counter + " maxTries=" + maxTries);
				if (counter >= maxTries) {
					toReturn = false;
					keepGoing = false;
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return toReturn;
	}

	private String sendCommand(String actuatorCommand) throws IOException {
		output = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
		logger.debug("sending command:" + actuatorCommand);
		output.write(actuatorCommand, 0, actuatorCommand.length());
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		output.flush();
		input = new ArduinoUnoReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream())));
		String inputLine = input.readLine();
		logger.debug("receiving response :" + inputLine);

		return inputLine;
	}
}
