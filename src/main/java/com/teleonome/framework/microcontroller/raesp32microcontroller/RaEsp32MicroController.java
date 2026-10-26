package com.teleonome.framework.microcontroller.raesp32microcontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fazecast.jSerialComm.SerialPort;
import com.teleonome.framework.LifeCycleEventListener;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.utils.Utils;

/**
 * Ra's local status display: a Wally ESP32 board (CP2104 USB bridge) with the Paula shield
 * (OLED, 6 WS2812 LEDs, slide switch) on one of the Pi's USB ports, running RaEsp32.ino.
 * It shows voltage/charge/load/time/IP without the website, flashes the LEDs on each
 * life cycle event, and lets the slide switch ask for a clean "sudo reboot" when Ra has
 * dropped off the network (instead of pulling the plug and risking the SD card).
 *
 * All serial traffic runs on one worker thread fed by a queue, so a display that is
 * unplugged, resetting or wedged can never block the pulse thread or the async cycle:
 * processLifeCycleEvent() only enqueues. Between commands the worker polls UserCommand;
 * when the device answers $Reboot (switch flicked and confirmed on the device) it answers
 * Rebooting and reboots, the same exchange MappedBusThread does for a mother's push button.
 * That poll is done here rather than through "Has UserCommands" because MappedBusThread
 * waits for the reply with no timeout, which would freeze Ra's async cycle whenever the
 * display is unplugged.
 *
 * Denewords read from this microcontroller's own dene (all optional):
 *   Serial Data Rate             default 115200
 *   Serial Port Name             e.g. ttyUSB1; empty = the first CP2104 found
 *   User Command Poll Seconds    default 5
 *   Battery Voltage Pointer, Charge Pointer, Load Pointer, State Of Charge Pointer,
 *   Battery State Pointer        denewords sent to the display after every pulse
 */
public class RaEsp32MicroController extends MicroController implements LifeCycleEventListener {

	// Silicon Labs CP2104 on the Wally board
	public static final int CP2104_VENDOR_ID = 0x10C4;
	public static final int CP2104_PRODUCT_ID = 0xEA60;

	public static final String DENEWORD_SERIAL_DATA_RATE = "Serial Data Rate";
	public static final String DENEWORD_SERIAL_PORT_NAME = "Serial Port Name";
	public static final String DENEWORD_USER_COMMAND_POLL_SECONDS = "User Command Poll Seconds";
	public static final String DENEWORD_BATTERY_VOLTAGE_POINTER = "Battery Voltage Pointer";
	public static final String DENEWORD_CHARGE_POINTER = "Charge Pointer";
	public static final String DENEWORD_LOAD_POINTER = "Load Pointer";
	public static final String DENEWORD_STATE_OF_CHARGE_POINTER = "State Of Charge Pointer";
	public static final String DENEWORD_BATTERY_STATE_POINTER = "Battery State Pointer";

	private static final int QUEUE_CAPACITY = 20;
	private static final long REPLY_TIMEOUT_MILLIS = 3000;
	private static final long RECONNECT_INTERVAL_MILLIS = 30000;
	private static final long AFTER_OPEN_SETTLE_MILLIS = 2500;   // opening the port can reset the ESP32

	private Logger logger;
	private int dataRate = 115200;
	private String serialPortName = "";
	private int userCommandPollSeconds = 5;
	private String batteryVoltagePointer = "";
	private String chargePointer = "";
	private String loadPointer = "";
	private String stateOfChargePointer = "";
	private String batteryStatePointer = "";

	private SerialPort serialPort = null;
	private long lastConnectAttemptMillis = 0;
	private final LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<String>(QUEUE_CAPACITY);
	private Thread worker;
	private volatile boolean rebootRequested = false;

	public RaEsp32MicroController(Hypothalamus h, DenomeManager d, String n) {
		super(h, d, n);
		logger = Logger.getLogger(getClass());
		setEnableAsyncUpdate(false);
	}

	/**
	 * True for the display's USB serial port, so other serial microcontrollers on Ra
	 * (PLSeriesMicroController, PL2303 adapter) can skip it whatever ttyUSB number it got.
	 */
	public static boolean isRaEsp32Port(SerialPort port) {
		return port.getVendorID() == CP2104_VENDOR_ID && port.getProductID() == CP2104_PRODUCT_ID;
	}

	@Override
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		try {
			Identity ownIdentity = new Identity(aDenomeManager.getDenomeName(), TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_COMPONENTS, getName());
			JSONObject ownDene = aDenomeManager.getDeneByIdentity(ownIdentity);
			Object o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(ownDene, DENEWORD_SERIAL_DATA_RATE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if (o instanceof Number) dataRate = ((Number) o).intValue();
			o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(ownDene, DENEWORD_SERIAL_PORT_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if (o instanceof String) serialPortName = ((String) o).trim();
			o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(ownDene, DENEWORD_USER_COMMAND_POLL_SECONDS, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if (o instanceof Number) userCommandPollSeconds = Math.max(1, ((Number) o).intValue());
			batteryVoltagePointer = readPointer(ownDene, DENEWORD_BATTERY_VOLTAGE_POINTER);
			chargePointer = readPointer(ownDene, DENEWORD_CHARGE_POINTER);
			loadPointer = readPointer(ownDene, DENEWORD_LOAD_POINTER);
			stateOfChargePointer = readPointer(ownDene, DENEWORD_STATE_OF_CHARGE_POINTER);
			batteryStatePointer = readPointer(ownDene, DENEWORD_BATTERY_STATE_POINTER);
		} catch (Exception e) {
			logger.warn("Could not read the RaEsp32 dene, using defaults: " + Utils.getStringException(e));
		}
		logger.info("RaEsp32 dataRate=" + dataRate + " serialPortName='" + serialPortName + "' poll=" + userCommandPollSeconds + "s");
		//
		// a missing display is not fatal - the worker keeps looking for it every 30 s
		//
		worker = new Thread(new Runnable() {
			public void run() {
				runWorker();
			}
		}, "RaEsp32Worker");
		worker.setDaemon(true);
		worker.start();
	}

	private String readPointer(JSONObject dene, String deneWordName) {
		Object o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, deneWordName, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		return o instanceof String ? (String) o : "";
	}

	public void processLifeCycleEvent(String lifeCycleEvent) {
		if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)
				|| lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE)
				|| lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE)) {
			enqueue(lifeCycleEvent);
		} else if (lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
			enqueue(lifeCycleEvent);
			//
			// the pulse just finished, so the sensor denewords hold this pulse's values
			//
			enqueue(buildRaDataCommand());
			enqueue(buildNetInfoCommand());
			enqueue(buildSetTimeCommand());
		}
	}

	private void enqueue(String command) {
		if (!commandQueue.offer(command)) {
			// display missing or slow - drop rather than grow; the next pulse resends everything
			logger.debug("RaEsp32 queue full, dropping " + command);
		}
	}

	//
	// Commands sent to the display
	//

	private String buildRaDataCommand() {
		return "RaData#" + readNumber(batteryVoltagePointer) + "#" + readNumber(chargePointer) + "#" + readNumber(loadPointer)
				+ "#" + readNumber(stateOfChargePointer) + "#" + readText(batteryStatePointer);
	}

	private String readNumber(String pointer) {
		Object o = readValue(pointer);
		if (o instanceof Number) return String.format("%.2f", ((Number) o).doubleValue());
		if (o instanceof String) {
			try {
				return String.format("%.2f", Double.parseDouble(((String) o).trim()));
			} catch (NumberFormatException e) {
			}
		}
		return "nan";
	}

	private String readText(String pointer) {
		Object o = readValue(pointer);
		if (o == null) return "-";
		String s = o.toString().replace('#', ' ').trim();
		return s.equals("") ? "-" : s;
	}

	private Object readValue(String pointer) {
		if (pointer == null || !pointer.startsWith("@")) return null;
		try {
			return aDenomeManager.getDeneWordAttributeByIdentity(new Identity(pointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		} catch (Exception e) {
			logger.debug("RaEsp32 could not read " + pointer + ": " + e.getMessage());
			return null;
		}
	}

	// NetInfo#<primary ip>#<wlan0 ip>#<eth0 ip>#<ssid>, '-' for anything missing. Read fresh every
	// pulse (not Hypothalamus.primaryIpAddress, set once at boot) so the display shows the
	// network actually dropping.
	private String buildNetInfoCommand() {
		String wlan0 = interfaceAddress(TeleonomeConstants.WLAN0);
		String eth0 = interfaceAddress(TeleonomeConstants.ETH0);
		String primary = !wlan0.equals("-") ? wlan0 : eth0;
		if (primary.equals("-")) {
			try {
				InetAddress anyAddress = hypothalamus.getIpAddress();
				if (anyAddress != null) primary = anyAddress.getHostAddress();
			} catch (Exception e) {
				logger.debug("RaEsp32 getIpAddress failed: " + e.getMessage());
			}
		}
		String ssid = "-";
		try {
			String s = NetworkUtilities.getCurrentConnectedSSiD();
			if (s != null && !s.trim().equals("")) ssid = s.trim().replace('#', ' ');
		} catch (Exception e) {
			logger.debug("RaEsp32 could not read the SSID: " + e.getMessage());
		}
		return "NetInfo#" + primary + "#" + wlan0 + "#" + eth0 + "#" + ssid;
	}

	private String interfaceAddress(String interfaceName) {
		try {
			NetworkInterface networkInterface = NetworkInterface.getByName(interfaceName);
			if (networkInterface == null || !networkInterface.isUp()) return "-";
			for (Enumeration<InetAddress> e = networkInterface.getInetAddresses(); e.hasMoreElements();) {
				InetAddress address = e.nextElement();
				if (address instanceof Inet4Address && !address.isLoopbackAddress()) return address.getHostAddress();
			}
		} catch (SocketException e) {
			logger.debug("RaEsp32 could not read " + interfaceName + ": " + e.getMessage());
		}
		return "-";
	}

	// SetTime#date#month#yy#dayOfWeek#hour#minute#second, the order PCF8563TimeManager.setTime uses
	private String buildSetTimeCommand() {
		Calendar cal = Calendar.getInstance();
		return "SetTime#" + cal.get(Calendar.DATE) + "#" + (cal.get(Calendar.MONTH) + 1) + "#" + (cal.get(Calendar.YEAR) % 100)
				+ "#" + (cal.get(Calendar.DAY_OF_WEEK) - 1) + "#" + cal.get(Calendar.HOUR_OF_DAY) + "#" + cal.get(Calendar.MINUTE)
				+ "#" + cal.get(Calendar.SECOND);
	}

	//
	// Worker thread - the only code that touches the serial port
	//

	private void runWorker() {
		while (true) {
			try {
				if (!ensureConnected()) {
					// nobody to talk to; keep only what the next pulse would not resend anyway
					commandQueue.clear();
					Thread.sleep(1000);
					continue;
				}
				String command = commandQueue.poll(userCommandPollSeconds, TimeUnit.SECONDS);
				if (command == null) command = TeleonomeConstants.USER_COMMAND;
				ArrayList<String> reply = exchange(command);
				if (command.equals(TeleonomeConstants.USER_COMMAND) && reply.contains(TeleonomeConstants.COMMAND_REBOOT)) {
					reboot();
				}
			} catch (InterruptedException e) {
				logger.warn("RaEsp32 worker interrupted, stopping");
				return;
			} catch (IOException e) {
				logger.warn("RaEsp32 serial port failed, will look for it again: " + e.getMessage());
				closePort();
			} catch (Exception e) {
				logger.warn(Utils.getStringException(e));
			}
		}
	}

	private void reboot() {
		if (rebootRequested) return;
		rebootRequested = true;
		logger.warn("RaEsp32 display asked for a reboot (slide switch confirmed), rebooting");
		try {
			exchange(TeleonomeConstants.COMMAND_REBOOTING);
		} catch (IOException e) {
			logger.warn("Could not tell the RaEsp32 display we are rebooting: " + e.getMessage());
		}
		try {
			Runtime.getRuntime().exec("sudo reboot");
		} catch (IOException e) {
			rebootRequested = false;
			logger.warn(Utils.getStringException(e));
		}
	}

	private boolean ensureConnected() {
		if (serialPort != null && serialPort.isOpen()) return true;
		long now = System.currentTimeMillis();
		if (now - lastConnectAttemptMillis < RECONNECT_INTERVAL_MILLIS) return false;
		lastConnectAttemptMillis = now;
		SerialPort port = findPort();
		if (port == null) {
			logger.debug("RaEsp32 display not found");
			return false;
		}
		port.setComPortParameters(dataRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
		port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
		port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
		if (!port.openPort()) {
			logger.warn("RaEsp32 could not open " + port.getSystemPortName());
			return false;
		}
		serialPort = port;
		try {
			Thread.sleep(AFTER_OPEN_SETTLE_MILLIS);
			ArrayList<String> reply = exchange("Ping");
			logger.info("RaEsp32 display on " + port.getSystemPortName() + " answered " + reply);
		} catch (IOException e) {
			logger.warn("RaEsp32 ping failed: " + e.getMessage());
			closePort();
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		return true;
	}

	private SerialPort findPort() {
		for (SerialPort port : SerialPort.getCommPorts()) {
			if (!serialPortName.equals("")) {
				if (port.getSystemPortName().equals(serialPortName) || ("/dev/" + port.getSystemPortName()).equals(serialPortName)) return port;
			} else if (isRaEsp32Port(port)) {
				return port;
			}
		}
		return null;
	}

	private void closePort() {
		if (serialPort != null) {
			try {
				serialPort.closePort();
			} catch (Exception e) {
				logger.debug("RaEsp32 closePort: " + e.getMessage());
			}
		}
		serialPort = null;
	}

	/**
	 * Sends one command (no terminator - the device frames on the pause after it, like
	 * Annabelle's Serial.readString) and returns the lines it answered, up to and including
	 * the Ok-/Failure- line, or what arrived before REPLY_TIMEOUT_MILLIS.
	 */
	private ArrayList<String> exchange(String command) throws IOException {
		ArrayList<String> lines = new ArrayList<String>();
		if (serialPort == null) throw new IOException("port not open");
		// whatever is waiting is stale: boot messages after a reset, a late answer
		byte[] scratch = new byte[256];
		int available;
		while ((available = serialPort.bytesAvailable()) > 0) {
			serialPort.readBytes(scratch, Math.min(available, scratch.length));
		}
		if (available < 0) throw new IOException("port closed");
		byte[] bytes = command.getBytes(StandardCharsets.US_ASCII);
		if (serialPort.writeBytes(bytes, bytes.length) != bytes.length) throw new IOException("write failed");
		logger.debug("RaEsp32 sent " + command);

		StringBuilder line = new StringBuilder();
		long deadline = System.currentTimeMillis() + REPLY_TIMEOUT_MILLIS;
		byte[] one = new byte[1];
		while (System.currentTimeMillis() < deadline) {
			available = serialPort.bytesAvailable();
			if (available < 0) throw new IOException("port closed");
			if (available == 0) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("interrupted");
				}
				continue;
			}
			if (serialPort.readBytes(one, 1) != 1) continue;
			char c = (char) one[0];
			if (c == '\r') continue;
			if (c != '\n') {
				if (line.length() < 512) line.append(c);
				continue;
			}
			String received = line.toString();
			line.setLength(0);
			if (received.equals("")) continue;
			lines.add(received);
			if (received.startsWith("Ok") || received.startsWith("Failure")) return lines;
		}
		logger.debug("RaEsp32 no complete answer to " + command + ", got " + lines);
		return lines;
	}

	//
	// The framework's synchronous reader/writer. Nothing in Ra's denome points sensors or
	// actuators at this microcontroller, but if an actuator action does (e.g. $IPForLCD), the
	// command goes through the same queue and the caller gets an immediate Ok.
	//

	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new StringReader("")) {
			public String readLine() {
				return "Ok-Queued";
			}

			public boolean ready() {
				return true;
			}
		};
	}

	public BufferedWriter getWriter() throws IOException {
		return new BufferedWriter(new StringWriter()) {
			private final StringBuilder pending = new StringBuilder();

			public void write(String s, int off, int len) {
				pending.append(s, off, off + len);
			}

			public void write(String s) {
				pending.append(s);
			}

			public void flush() {
				if (pending.length() > 0) {
					enqueue(pending.toString());
					pending.setLength(0);
				}
			}

			public void close() {
				flush();
			}
		};
	}
}
