package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MotherMicroController;
import com.teleonome.framework.microcontroller.TtySerialPort;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.security.totp.TOTP;
import com.teleonome.framework.utils.Utils;

// com.teleonome.framework.microcontroller.cleotildemicrocontroller.CleotildeMicroController
/**
 * Cleotilde: watches the OpenWrt router (ubus over Ethernet) and drives the Cleotilde board's LEDs
 * and OLED (USB serial). Design: OpenWrtOne/documents/05-teleonome-design.md.
 *
 * GetSensorData returns one '#' line. Positions are fixed - append only, never renumber:
 *   0-8   the NetworkInspector values, unchanged, so the existing Network Status / DeviceList
 *         pages and the whitelist work: NumberOfDevices, NumberOfUnknowDevices, DeviceList,
 *         DeviceListChanges, DownloadSpeed, UploadSpeed, PingTime, SampleTimeMillis, SampleTimeString
 *   9  Internet Up            10 Internet Latency ms   11 Internet Packet Loss %
 *   12 WAN Up                 13 WAN Uptime s          14 WAN IP
 *   15 WAN Download Mbit/s    16 WAN Upload Mbit/s     (live, from the WAN byte counters)
 *   17 Router Uptime s        18 Router Load           19 Router Memory Free %
 *   20 Router CPU Temperature 21 Wifi Clients 2.4 GHz  22 Wifi Clients 5 GHz
 *   23 Router Reachable Via (ethernet / none)
 *   24 Board Reachable        25 Board Input Voltage   26 Board Input Current mA   27 Board Temperature
 *   28 Cleotilde Status       one-line summary for the web app banner (Well Single Value panel)
 *   Traffic watch (TrafficWatcher, added 2026-11-04; design 05 section 7c), all "today" from midnight:
 *   29 Watchlist Connections Today   30 Watchlist MB Today    31 Night Upload MB (this or last night)
 *   32 Top Night Uploader (name + MB) 33 Suspicious Events Today 34 Last Suspicious Event (HH:mm + detail)
 *   35 Countries Today (JSON, MB per country, top 15)        36 Device Traffic Today (JSON, top 20 devices)
 *   37 Upload MB Today (all devices)  38 Download MB Today
 *
 * Every GetSensorData also refreshes the board's LEDs and OLED pages from the same readings.
 *
 * Commands (actuator actions): "Generate Analysis", the three NetworkInspector whitelist commands,
 * and PowerCycle#modem / PowerCycle#router, which are recognised but inactive until the
 * Cleotilde Build 1 relays exist.
 *
 * Config parameters (Microcontroller Config Parameter denes, deneword name = dene name), all optional:
 *   Router Address (192.168.1.1), Router User (cleotilde),
 *   Router Password File (/home/pi/Teleonome/CleotildeRouter.secret),
 *   Board Serial Port (/dev/ttyUSB0), Serial Data Rate (115200), Ping Target (1.1.1.1),
 *   Speed Test Interval Hours (6), Latency Warning Ms (1000; LED 1 yellow above it - this link
 *   normally runs at 600-1000 ms, so the web app rule uses the same limit),
 *   Device History Days (30; how far back the "Not seen" device cards go),
 *   Sweep Networks (192.168.1.0/24; space separated, empty turns the sweep off),
 *   Sweep Interface (eth0),
 *   Mailbox Host (104.154.239.253, chilhuacle/Zhinu), Mailbox User (pi), Mailbox Key File (/home/pi/.ssh/dg),
 *   Mailbox Directory (Teleonome/Mailbox/), Mailbox Poll Seconds (10; 0 turns the doorbell off),
 *   Traffic Poll Seconds (30; 0 turns the traffic watch off), Traffic Flush Minutes (5),
 *   Traffic LAN Prefix (192.168.1.), Traffic Watchlist (CN RU KP IR), Traffic Odd Ports (23 2323 445 ...),
 *   Traffic Night Hours (0-5), Traffic Night Upload MB (20), Traffic Baseline Factor (10),
 *   Traffic Baseline Minimum MB (50), Traffic Learning Days (7), Traffic Alert Unknown Devices (true),
 *   Traffic Flow Days (60), Traffic DNS Days (30),
 *   DigitalStables Reading Minutes (5; 0 = never), DigitalStables Settings File (/home/pi/Teleonome/devicesheart.properties)
 *
 * ---------------------------------------------------------------------------------------------
 * DEVICES.DIGITALSTABLES.COM (added 2026-11-04)
 * ---------------------------------------------------------------------------------------------
 * Every DigitalStables Reading Minutes, one reading of Cleotilde itself goes into the DevicesOutbox, which
 * the DigitalStables Publisher sends to devicesheart as chilhuacle-cleotilde (the login and the product's
 * serial number come from the settings file the factory writes at Send To DS; nothing when it is missing or
 * the Denome's Upload To DigitalStables is false). The reading has internet, WAN, router, Wi-Fi, device
 * counts, the board and the traffic summary. Never the device list, destinations, domains or DNS: that is the
 * household's browsing and stays on Cleotilde's own page.
 *
 * ---------------------------------------------------------------------------------------------
 * DOORBELL (added 2026-10-20)
 * ---------------------------------------------------------------------------------------------
 * DoorbellPoller reads Cleotilde's outbox on Zhinu every 10 s by SFTP, asks the board about each
 * ring's code and writes the answers back for Zhinu's Mailbox page (design 07, section 3). A valid
 * ring is only shown on OLED page 4 for now; opening the Network Services comes with the Network Rules.
 *
 * ---------------------------------------------------------------------------------------------
 * SERIAL LINK TO THE BOARD: NO jSerialComm (changed 2026-10-18)
 * ---------------------------------------------------------------------------------------------
 * The board (ESP32 Wally 17 + Paula shield, CP2104 USB bridge) is reached through TtySerialPort,
 * which opens the tty as a plain file (stty for the line settings, FileInputStream /
 * FileOutputStream for the data). It replaced jSerialComm and the old CleotildeBoard class.
 *
 * Why: jSerialComm's native library leaks memory outside the Java heap while a port is held open.
 * On 2026-10-17/18 Cleotilde's Hypothalamus (-Xmx100m, heap never above ~30 MB) grew from 75 MB
 * to 586 MB resident within about 45 minutes, the kernel OOM-killer started killing processes
 * (Hypothalamus, Tomcat, speedtest, Medula) and the Pi 3B (923 MB RAM, 100 MB swap) froze until it
 * was power-cycled - twice. Native Memory Tracking showed the JVM's own memory flat, so the growth
 * was in a native library, and jSerialComm (libjSerialComm.so) is the only non-JDK one loaded.
 * It is the same leak investigated on Ra and Sento since July 2026: every Hypothalamus holding a
 * jSerialComm port open grows (Ra is OOM-killed about every 3.5 h, ChinampaMonitor grows ~6 MB/h);
 * upgrading to the latest jSerialComm (2.11.4) did not stop it, and opening/closing the port
 * per pulse was rejected because toggling DTR/RTS can reset the ESP32 (they drive EN/IO0 on Wally).
 * With TtySerialPort the process loads no native serial code at all, so there is nothing to leak.
 * Watch "Memory Usage Last 6 Hours" (Home and Diagnostics pages): the Hypothalamus line should
 * now stay flat instead of climbing. If it does, AnnabelleController (same ESP32 board family)
 * gets the same change.
 *
 * Shape (the same as AnnabelleController): the controller owns the port; CleotildeReader and
 * CleotildeWriter wrap its input and output streams. boardCommand() does one request/answer
 * exchange through them, closes the port if it fails and reopens it on the next command, so an
 * unplugged and replugged board comes back on its own. Prefer the stable Board Serial Port name
 * /dev/serial/by-id/usb-Silicon_Labs_CP2104_... once the router's console is also on USB.
 *
 * ---------------------------------------------------------------------------------------------
 * MOTHER (added 2026-10-19)
 * ---------------------------------------------------------------------------------------------
 * Set "Mother Microcontroller" = true on this component (and false on Simple Micro Controller) and
 * the TOTP codes come from the board: GetCommandCode / VerifyUserCode#code, firmware Cleotilde-0.4+,
 * secret set on the board with SetSecret (the same base32 secret as the SecretKey file, so the phone
 * app's codes keep working). SimpleMicroController.verifyUserCommandCode() accepts every code; the
 * board checks it: the current 30 s step, or one of the last 15 minutes once each (3 until 2026-11-04; doorbell rings
 * arrive 1-2 min late), and locks for an hour after 5 wrong codes. No board answer = invalid code.
 */
public class CleotildeMicroController extends MotherMicroController {

	public static final String NETWORK_SENSOR_FILE = "NetworkSensor.json";
	private static final String DEFAULT_NETWORK_SENSOR = "-1#-1#[]#{}#0#0#0#";
	// extra fields on each DeviceList entry for the web app's device page
	public static final String VENDOR = "Vendor";
	public static final String CONNECTION = "Connection";
	public static final String PRIVATE_MAC = "Private MAC";
	public static final String FIRST_SEEN = "First Seen";
	public static final String LAST_SEEN = "Last Seen";
	/** The name the router (or the sweep) gives a device that has a nickname. */
	public static final String ORIGINAL_NAME = "Original Name";
	/**
	 * Identification for the device cards (2026-11-04, DeviceIdentifier): the name the device announces on the
	 * network (mDNS), the best guess of what it is and why, and the domains it looked up most in the last 7 days.
	 */
	public static final String NETWORK_NAME = "Network Name";
	public static final String GUESS = "Guess";
	public static final String GUESS_BECAUSE = "Guess Because";
	public static final String LOOKS_UP = "Looks Up";
	private static final int NETWORK_NAME_TIMEOUT_SECONDS = 12;
	private static final int LOOKS_UP_DOMAINS = 5;
	/**
	 * Nicknames from the Rename button on the device cards (added 2026-11-04): {"mac": "nickname"}, in the
	 * working directory. Kept on the Pi, not on the router, so the router's cleotilde account stays read-only,
	 * and not in the denome, which is public.
	 */
	public static final String NICKNAMES_FILE = "DeviceNicknames.json";
	private static final int NICKNAME_MAX_LENGTH = 40;
	/** Connection of a device only the Pi's sweep found: the router has no trace of it. */
	public static final String SWEEP_ONLY = "sweep only";
	private static final String OUI_FILE = "/usr/share/arp-scan/ieee-oui.txt";
	private static final long BOARD_READ_TIMEOUT_MILLIS = 200;    // one read() on the tty
	private static final long BOARD_ANSWER_TIMEOUT_MILLIS = 2000; // one command's answer
	private static final int MOTHER_TRIES = 3;

	Logger logger;
	private String routerAddress = "192.168.1.1";
	private String routerUser = "cleotilde";
	private String routerPasswordFile = "/home/pi/Teleonome/CleotildeRouter.secret";
	private String boardSerialPort = "/dev/ttyUSB0";
	private int serialDataRate = 115200;
	private String pingTarget = "1.1.1.1";
	private int speedTestIntervalHours = 6;
	private int latencyWarningMs = 1000;
	private int deviceHistoryDays = 30;
	private String sweepNetworks = "192.168.1.0/24";
	private String sweepInterface = "eth0";
	private String mailboxHost = "104.154.239.253";
	private String mailboxUser = "pi";
	private String mailboxKeyFile = "/home/pi/.ssh/dg";
	private String mailboxDirectory = "Teleonome/Mailbox/";
	private int mailboxPollSeconds = 10;
	private DoorbellPoller doorbellPoller;
	private TrafficWatcher trafficWatcher;
	/** The traffic tables, also read by the scan for each device's DNS lookups (null when the watch is off). */
	private TrafficStore trafficStore;
	private final Map<String, String> trafficSettings = new HashMap<>();
	/** LED 4 blinks red this long after a new Suspicious Event. */
	private static final long TRAFFIC_EVENT_BLINK_MILLIS = 3_600_000L;
	private int digitalStablesReadingMinutes = 5;
	private String digitalStablesSettingsFile = "/home/pi/Teleonome/devicesheart.properties";
	private long lastDigitalStablesReadingMillis = 0;
	// the board's doorbell lock, from GetSensorData fields 18-19 (firmware 0.4+), -1 = not known yet
	private volatile int codeFailuresLastHour = -1;
	private volatile boolean codeLockedOut = false;

	private OpenWrtUbusClient ubus;
	private TtySerialPort serialPort;
	private MnemosyneManager aMnemosyneManager;
	private CleotildeReader reader;
	private CleotildeWriter writer;

	// state carried between pulses
	private long lastWanRxBytes = -1, lastWanTxBytes = -1, lastWanSampleMillis = 0;
	private long lastSpeedTestMillis = 0;
	private double lastDownloadSpeed = 0, lastUploadSpeed = 0, lastSpeedTestPing = 0;
	private volatile boolean generatingAnalysis = false;
	private final DecimalFormat twoDecimals = new DecimalFormat("0.00");

	public CleotildeMicroController(Hypothalamus h, DenomeManager d, String n) {
		super(h, d, n);
		logger = Logger.getLogger(getClass());
		aMnemosyneManager = aDenomeManager.getMnemosyneManager();
	}

	@Override
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		for (int i = 0; i < configParams.length(); i++) {
			try {
				JSONObject configDene = configParams.getJSONObject(i);
				String name = configDene.getString("Name");
				Object value = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(configDene, name, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (value == null) continue;
				switch (name) {
					case "Router Address": routerAddress = value.toString(); break;
					case "Router User": routerUser = value.toString(); break;
					case "Router Password File": routerPasswordFile = value.toString(); break;
					case "Board Serial Port": boardSerialPort = value.toString(); break;
					case "Serial Data Rate": serialDataRate = Integer.parseInt(value.toString()); break;
					case "Ping Target": pingTarget = value.toString(); break;
					case "Speed Test Interval Hours": speedTestIntervalHours = Integer.parseInt(value.toString()); break;
					case "Latency Warning Ms": latencyWarningMs = Integer.parseInt(value.toString()); break;
					case "Device History Days": deviceHistoryDays = Integer.parseInt(value.toString()); break;
					case "Sweep Networks": sweepNetworks = value.toString(); break;
					case "Sweep Interface": sweepInterface = value.toString(); break;
					case "Mailbox Host": mailboxHost = value.toString(); break;
					case "Mailbox User": mailboxUser = value.toString(); break;
					case "Mailbox Key File": mailboxKeyFile = value.toString(); break;
					case "Mailbox Directory": mailboxDirectory = value.toString(); break;
					case "Mailbox Poll Seconds": mailboxPollSeconds = Integer.parseInt(value.toString()); break;
					case "DigitalStables Reading Minutes": digitalStablesReadingMinutes = Integer.parseInt(value.toString()); break;
					case "DigitalStables Settings File": digitalStablesSettingsFile = value.toString(); break;
					default:
						if (name.startsWith("Traffic ")) trafficSettings.put(name, value.toString());
						break;
				}
			} catch (JSONException | NumberFormatException e) {
				logger.warn(Utils.getStringException(e));
			}
		}
		String password = "";
		try {
			password = FileUtils.readFileToString(new File(routerPasswordFile), Charset.defaultCharset()).trim();
		} catch (IOException e) {
			logger.warn("Could not read router password from " + routerPasswordFile + ", router readings will fail");
		}
		ubus = new OpenWrtUbusClient(routerAddress, routerUser, password);
		serialPort = new TtySerialPort(boardSerialPort, serialDataRate, BOARD_READ_TIMEOUT_MILLIS);
		// not fatal: the board is optional, and boardCommand() reopens the port when it's back
		if (!serialPort.openPort()) logger.warn("Could not open Cleotilde board on " + boardSerialPort);
		createReaderWriter();
		if (mailboxPollSeconds > 0 && doorbellPoller == null) {
			doorbellPoller = new DoorbellPoller(this, mailboxHost, mailboxUser, mailboxKeyFile, mailboxDirectory, "Cleotilde", mailboxPollSeconds);
			doorbellPoller.start();
		}
		startTrafficWatch();
		logger.info("Cleotilde initialised, router=" + routerAddress + " board=" + boardSerialPort);
	}

	/** The traffic watch (TrafficWatcher), with the Traffic ... config parameters. */
	private void startTrafficWatch() {
		if (trafficWatcher != null) return;
		int pollSeconds = parseIntOr(trafficSettings.getOrDefault("Traffic Poll Seconds", "30"), 30);
		if (pollSeconds <= 0) {
			logger.info("Traffic watch off (Traffic Poll Seconds = 0)");
			return;
		}
		trafficStore = new TrafficStore(com.teleonome.framework.persistence.PostgresqlPersistenceManager.instance());
		TrafficWatcher w = new TrafficWatcher(ubus, trafficStore);
		w.pollMillis = pollSeconds * 1000L;
		for (Map.Entry<String, String> e : trafficSettings.entrySet()) {
			String v = e.getValue().trim();
			try {
				switch (e.getKey()) {
					case "Traffic Flush Minutes": w.flushMillis = Math.max(1, Integer.parseInt(v)) * 60_000L; break;
					case "Traffic LAN Prefix": w.lanPrefix = v; break;
					case "Traffic Watchlist": w.watchlist = new java.util.HashSet<>(java.util.Arrays.asList(v.toUpperCase().split("[ ,]+"))); break;
					case "Traffic Odd Ports":
						java.util.Set<Integer> ports = new java.util.HashSet<>();
						for (String p : v.split("[ ,]+")) if (!p.isEmpty()) ports.add(Integer.parseInt(p));
						w.oddPorts = ports;
						break;
					case "Traffic Night Hours":
						String[] h = v.split("-");
						w.nightStartHour = Integer.parseInt(h[0].trim());
						w.nightEndHour = Integer.parseInt(h[1].trim());
						break;
					case "Traffic Night Upload MB": w.nightUploadMB = Double.parseDouble(v); break;
					case "Traffic Baseline Factor": w.baselineFactor = Double.parseDouble(v); break;
					case "Traffic Baseline Minimum MB": w.baselineMinimumMB = Double.parseDouble(v); break;
					case "Traffic Learning Days": w.learningDays = Integer.parseInt(v); break;
					case "Traffic Alert Unknown Devices": w.alertUnknownDevices = Boolean.parseBoolean(v); break;
					case "Traffic Flow Days": w.flowDays = Integer.parseInt(v); break;
					case "Traffic DNS Days": w.dnsDays = Integer.parseInt(v); break;
					default: break;
				}
			} catch (RuntimeException ex) {
				logger.warn("Bad config parameter " + e.getKey() + " = " + v + ", using the default");
			}
		}
		trafficWatcher = w;
		w.start();
	}

	/** Tokens 29-38; zeros until the watch runs (and when it is off), so the positions never move. */
	private String trafficTokens() {
		return trafficWatcher == null ? "0#0.00#0.00##0##{}#[]#0.00#0.00" : trafficWatcher.sensorTokens();
	}

	/** Reader and writer over the board's serial streams, the same shape as AnnabelleController. */
	private void createReaderWriter() {
		reader = new CleotildeReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream(), StandardCharsets.UTF_8)), this);
		writer = new CleotildeWriter(new OutputStreamWriter(serialPort.getOutputStream(), StandardCharsets.UTF_8), reader, this);
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		createReaderWriter();
		return writer;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return reader;
	}

	@Override
	public void reconnect() throws IOException {
		if (!serialPort.openPort()) throw new IOException("Could not open Cleotilde board on " + boardSerialPort);
		createReaderWriter();
	}

	/**
	 * One request/answer exchange with the board: drop stale output, send the command, wait for
	 * the line starting with expectedPrefix (null = "Ok-"/"Failure-"). Null if the board didn't
	 * answer. If the port has gone (USB unplugged, ESP32 replaced) it is closed here and reopened
	 * on the next command, so a replugged board comes back without restarting the Hypothalamus.
	 */
	synchronized String boardCommand(String command, String expectedPrefix, long timeoutMillis) {
		if (!serialPort.isOpen()) {
			if (!serialPort.openPort()) return null;
			createReaderWriter();
		}
		try {
			reader.dropStaleInput();
			writer.sendToBoard(command);
			String answer = reader.readBoardAnswer(expectedPrefix, timeoutMillis);
			if (answer == null) logger.warn("No answer from Cleotilde board to " + command);
			return answer;
		} catch (IOException e) {
			logger.warn("Cleotilde board port failed, will reopen: " + e.getMessage());
			serialPort.closePort();
			return null;
		}
	}

	/** OLED page 1-6: a title and up to 6 lines of about 21 characters. */
	private void oledPage(int page, String title, String... lines) {
		StringBuilder c = new StringBuilder("Oled#").append(page).append('#').append(oledText(title));
		for (String l : lines) c.append('#').append(oledText(l));
		boardCommand(c.toString(), null, BOARD_ANSWER_TIMEOUT_MILLIS);
	}

	private static String oledText(String s) {
		if (s == null) return "";
		String t = s.replace('#', ' ').replace('\n', ' ');
		return t.length() > 21 ? t.substring(0, 21) : t;
	}

	// ------------------------------------------------------------------ sensor line

	String buildSensorLine() {
		long now = System.currentTimeMillis();
		String networkSensor = readNetworkSensorFile(now);

		Map<String, Object> r = readRouter(now);
		double[] ping = pingInternet();  // up(1/0), latency ms, loss %
		String boardLine = boardCommand("GetSensorData", "Cleotilde#", BOARD_ANSWER_TIMEOUT_MILLIS);
		String[] b = boardLine == null ? null : boardLine.split("#", -1);
		if (b != null && b.length > 19) {
			codeFailuresLastHour = parseIntOr(b[18], -1);
			codeLockedOut = "1".equals(b[19].trim());
		}

		StringBuilder line = new StringBuilder(networkSensor);
		line.append('#').append(ping[0] > 0)
			.append('#').append(twoDecimals.format(ping[1]))
			.append('#').append(twoDecimals.format(ping[2]))
			.append('#').append(r.get("wanUp"))
			.append('#').append(r.get("wanUptime"))
			.append('#').append(r.get("wanIp"))
			.append('#').append(twoDecimals.format((Double) r.get("wanDownMbps")))
			.append('#').append(twoDecimals.format((Double) r.get("wanUpMbps")))
			.append('#').append(r.get("routerUptime"))
			.append('#').append(twoDecimals.format((Double) r.get("load")))
			.append('#').append(r.get("memFreePercent"))
			.append('#').append(twoDecimals.format((Double) r.get("cpuTemp")))
			.append('#').append(r.get("clients24"))
			.append('#').append(r.get("clients5"))
			.append('#').append(r.get("reachableVia"))
			.append('#').append(b != null)
			.append('#').append(b != null && b.length > 3 ? b[3] : "0")
			.append('#').append(b != null && b.length > 4 ? b[4] : "0")
			.append('#').append(b != null && b.length > 6 ? b[6] : "0")
			.append('#').append(statusSummary(networkSensor, r, ping))
			.append('#').append(trafficTokens());

		updateFace(networkSensor, r, ping);
		queueDigitalStablesReading(now, networkSensor, r, ping, b);
		return line.toString();
	}

	// ------------------------------------------------------------------ devices.digitalstables.com

	/** The product's serial number from the factory's settings file, "" when there is none yet. */
	private String productSerialNumber() {
		java.util.Properties p = new java.util.Properties();
		try (java.io.FileInputStream in = new java.io.FileInputStream(digitalStablesSettingsFile)) {
			p.load(in);
		} catch (IOException e) {
			return "";
		}
		String serial = p.getProperty("annabelle.serialnumber", "").trim().toLowerCase();
		return serial.matches("[0-9a-f]{12}") ? serial : "";
	}

	private static JSONObject telepathonWord(String name, Object value, String valueType, String units) {
		JSONObject w = new JSONObject().put("Name", name).put("Value", value).put("Value Type", valueType).put("Required", true);
		if (units != null) w.put("Units", units);
		return w;
	}

	/**
	 * One reading of Cleotilde for the DevicesOutbox, shaped like a relayed telepathon (Configuration +
	 * Purpose denes), so devices.digitalstables.com draws it like any other device. Never throws.
	 */
	void queueDigitalStablesReading(long now, String networkSensor, Map<String, Object> r, double[] ping, String[] b) {
		if (digitalStablesReadingMinutes <= 0 || now - lastDigitalStablesReadingMillis < digitalStablesReadingMinutes * 60_000L) return;
		try {
			JSONObject telepathon = buildDigitalStablesTelepathon(now, networkSensor, r, ping, b);
			if (telepathon == null) return;
			lastDigitalStablesReadingMillis = now;
			com.teleonome.framework.microcontroller.digitalstablespublisher.DevicesOutbox.addReading(aDenomeManager, telepathon, now / 1000);
		} catch (RuntimeException e) {
			logger.warn("Could not queue Cleotilde's reading for DigitalStables: " + e.getMessage());
		}
	}

	/** null when the factory hasn't given this Pi its settings (no serial number yet). */
	JSONObject buildDigitalStablesTelepathon(long now, String networkSensor, Map<String, Object> r, double[] ping, String[] b) {
		String serial = productSerialNumber();
		if (serial.isEmpty()) return null;
		long secondsTime = now / 1000;
		String[] ns = networkSensor.split("#", -1);
		String[] t = trafficTokens().split("#", -1);  // 29-38, see the class comment
		JSONArray config = new JSONArray()
				.put(telepathonWord("Device Type Id", "Cleotilde", "String", null))
				.put(telepathonWord("Serial Number", serial, "String", null))
				.put(telepathonWord("Firmware Version", b != null && b.length > 16 ? b[16].trim() : "", "String", null));
		JSONArray purpose = new JSONArray()
				.put(telepathonWord("Seconds Time", secondsTime, "long", null))
				.put(telepathonWord("Internet Up", ping[0] > 0, "boolean", null))
				.put(telepathonWord("Internet Latency", Math.round(ping[1]), "long", "ms"))
				.put(telepathonWord("Internet Packet Loss", ping[2], "double", "%"))
				.put(telepathonWord("WAN Up", r.get("wanUp"), "boolean", null))
				.put(telepathonWord("WAN Uptime", r.get("wanUptime"), "long", "s"))
				.put(telepathonWord("Router Reachable", !"none".equals(r.get("reachableVia")), "boolean", null))
				.put(telepathonWord("Router Uptime", r.get("routerUptime"), "long", "s"))
				.put(telepathonWord("Router Load", Math.round((Double) r.get("load") * 100) / 100.0, "double", null))
				.put(telepathonWord("Router Memory Free", r.get("memFreePercent"), "int", "%"))
				.put(telepathonWord("Router Temperature", Math.round((Double) r.get("cpuTemp") * 10) / 10.0, "double", "C"))
				.put(telepathonWord("WiFi Clients", (Integer) r.get("clients24") + (Integer) r.get("clients5"), "int", null))
				.put(telepathonWord("Devices", parseIntOr(ns[0], -1), "int", null))
				.put(telepathonWord("Unknown Devices", parseIntOr(ns[1], -1), "int", null))
				.put(telepathonWord("Board Reachable", b != null, "boolean", null))
				.put(telepathonWord("Board Voltage", b != null && b.length > 3 ? parseDoubleOr(b[3], 0) : 0.0, "double", "V"))
				.put(telepathonWord("Board Current", b != null && b.length > 4 ? parseDoubleOr(b[4], 0) : 0.0, "double", "mA"))
				.put(telepathonWord("Board Temperature", b != null && b.length > 6 ? parseDoubleOr(b[6], 0) : 0.0, "double", "C"));
		if (trafficWatcher != null && t.length >= 10) {
			JSONObject last = trafficWatcher.getLastEvent();
			String lastEvent = last == null ? "" : new SimpleDateFormat("HH:mm").format(new Timestamp(last.optLong("Time")))
					+ " " + last.optString("Rule") + ": " + last.optString("Device")
					+ (last.optString("Country").isEmpty() ? "" : " (" + last.optString("Country") + ")");
			StringBuilder countries = new StringBuilder();
			try {
				JSONObject cs = new JSONObject(t[6]);
				cs.keySet().stream().sorted((x, y) -> Double.compare(cs.getDouble(y), cs.getDouble(x))).limit(6)
						.forEach(k -> countries.append(countries.length() == 0 ? "" : ", ").append(k).append(' ').append(cs.get(k)));
			} catch (JSONException e) {
				// leave it empty
			}
			purpose.put(telepathonWord("Suspicious Events Today", parseIntOr(t[4], 0), "int", null))
					.put(telepathonWord("Last Suspicious Event", lastEvent, "String", null))
					.put(telepathonWord("Upload Today", parseDoubleOr(t[8], 0), "double", "MB"))
					.put(telepathonWord("Download Today", parseDoubleOr(t[9], 0), "double", "MB"))
					.put(telepathonWord("Watchlist Connections Today", parseIntOr(t[0], 0), "int", null))
					.put(telepathonWord("Watchlist Traffic Today", parseDoubleOr(t[1], 0), "double", "MB"))
					.put(telepathonWord("Night Upload", parseDoubleOr(t[2], 0), "double", "MB"))
					.put(telepathonWord("Top Countries Today", countries.toString(), "String", "MB"))
					.put(telepathonWord("Traffic Watch Running", trafficWatcher.isRouterHelperOk(), "boolean", null));
		}
		return new JSONObject().put("Name", "Cleotilde").put("Short Name", "Cleotilde").put("Serial Number", serial)
				.put("Seconds Time", secondsTime).put("Raw Data", "")  // never the sensor line: it has the device list
				.put("Denes", new JSONArray()
						.put(new JSONObject().put("Name", "Configuration").put("DeneWords", config))
						.put(new JSONObject().put("Name", "Purpose").put("DeneWords", purpose)));
	}

	private static double parseDoubleOr(String s, double fallback) {
		try {
			return Double.parseDouble(s.trim());
		} catch (NumberFormatException | NullPointerException e) {
			return fallback;
		}
	}

	/** "Internet UP 14 ms · Router up 2d 3h, 55 C · Wi-Fi 12+2 · 31 devices, 9 unknown" */
	private String statusSummary(String networkSensor, Map<String, Object> r, double[] ping) {
		StringBuilder s = new StringBuilder();
		if (ping[0] > 0) {
			s.append("Internet UP ").append(Math.round(ping[1])).append(" ms");
			if (ping[2] > 0) s.append(", ").append(Math.round(ping[2])).append("% loss");
		} else {
			s.append("Internet DOWN");
		}
		if ("none".equals(r.get("reachableVia"))) {
			s.append(" \u00B7 Router UNREACHABLE");
		} else {
			long up = (Long) r.get("routerUptime");
			s.append(" \u00B7 Router up ").append(up / 86400).append("d ").append((up / 3600) % 24).append("h, ")
				.append(Math.round((Double) r.get("cpuTemp"))).append(" C");
			s.append(" \u00B7 Wi-Fi ").append(r.get("clients24")).append('+').append(r.get("clients5"));
		}
		String[] ns = networkSensor.split("#", -1);
		if (!ns[0].equals("-1")) s.append(" \u00B7 ").append(ns[0]).append(" devices, ").append(ns[1]).append(" unknown");
		return s.toString();  // never contains '#', the token separator
	}

	private String readNetworkSensorFile(long now) {
		String sampleTimeString = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Timestamp(now));
		String fallback = DEFAULT_NETWORK_SENSOR + now + "#" + sampleTimeString;
		File file = new File(NETWORK_SENSOR_FILE);
		if (!file.isFile()) return fallback;
		try {
			String s = FileUtils.readFileToString(file, Charset.defaultCharset()).trim();
			return s.split("#", -1).length == 9 ? s : fallback;
		} catch (IOException e) {
			logger.warn(Utils.getStringException(e));
			return fallback;
		}
	}

	/** Router readings; on failure the values are zero and reachableVia is "none". */
	private Map<String, Object> readRouter(long now) {
		Map<String, Object> r = new HashMap<>();
		r.put("wanUp", false); r.put("wanUptime", 0L); r.put("wanIp", "");
		r.put("wanDownMbps", 0.0); r.put("wanUpMbps", 0.0);
		r.put("routerUptime", 0L); r.put("load", 0.0); r.put("memFreePercent", 0);
		r.put("cpuTemp", 0.0); r.put("clients24", 0); r.put("clients5", 0);
		r.put("radiosUp", 0); r.put("radios", 0);
		r.put("reachableVia", "none");
		try {
			JSONObject info = ubus.call("system", "info");
			r.put("routerUptime", info.getLong("uptime"));
			r.put("load", info.getJSONArray("load").getLong(0) / 65536.0);
			JSONObject mem = info.getJSONObject("memory");
			r.put("memFreePercent", (int) Math.round(100.0 * mem.getLong("available") / mem.getLong("total")));
			r.put("reachableVia", "ethernet");

			JSONObject wan = ubus.call("network.interface.wan", "status");
			r.put("wanUp", wan.optBoolean("up"));
			r.put("wanUptime", wan.optLong("uptime"));
			JSONArray v4 = wan.optJSONArray("ipv4-address");
			if (v4 != null && v4.length() > 0) r.put("wanIp", v4.getJSONObject(0).optString("address"));
			String wanDevice = wan.optString("l3_device", wan.optString("device", ""));
			if (!wanDevice.isEmpty()) {
				JSONObject stats = ubus.call("network.device", "status", new JSONObject().put("name", wanDevice)).getJSONObject("statistics");
				long rx = stats.getLong("rx_bytes"), tx = stats.getLong("tx_bytes");
				if (lastWanRxBytes >= 0 && now > lastWanSampleMillis && rx >= lastWanRxBytes && tx >= lastWanTxBytes) {
					double seconds = (now - lastWanSampleMillis) / 1000.0;
					r.put("wanDownMbps", (rx - lastWanRxBytes) * 8 / seconds / 1e6);
					r.put("wanUpMbps", (tx - lastWanTxBytes) * 8 / seconds / 1e6);
				}
				lastWanRxBytes = rx; lastWanTxBytes = tx; lastWanSampleMillis = now;
			}

			JSONObject temp = ubus.call("file", "read", new JSONObject().put("path", "/sys/class/thermal/thermal_zone0/temp"));
			r.put("cpuTemp", Double.parseDouble(temp.getString("data").trim()) / 1000.0);

			JSONArray devices = ubus.call("iwinfo", "devices").getJSONArray("devices");
			int clients24 = 0, clients5 = 0, radiosUp = 0;
			for (int i = 0; i < devices.length(); i++) {
				String dev = devices.getString(i);
				JSONObject radio = ubus.call("iwinfo", "info", new JSONObject().put("device", dev));
				int clients = ubus.call("iwinfo", "assoclist", new JSONObject().put("device", dev)).getJSONArray("results").length();
				if (radio.optInt("frequency") > 0) radiosUp++;
				if (radio.optInt("frequency") < 3000) clients24 += clients;
				else clients5 += clients;
			}
			r.put("clients24", clients24); r.put("clients5", clients5);
			r.put("radiosUp", radiosUp); r.put("radios", devices.length());
		} catch (IOException | JSONException | NumberFormatException e) {
			logger.warn("Reading the router failed: " + e.getMessage());
		}
		return r;
	}

	/** Ping from the Pi through the router: {up 1/0, average latency ms, packet loss %}. */
	private double[] pingInternet() {
		double[] result = { 0, 0, 100 };
		try {
			// 10 pings 0.2 s apart (about 2 s; intervals under 1 s need root, which the Hypothalamus
			// is). It was 3 until 2026-10-18: one lost ping read as 33% and tripped the 20% alarm.
			ArrayList lines = Utils.executeCommand("ping -c 10 -i 0.2 -W 2 " + pingTarget);
			for (Object o : lines) {
				String l = o.toString();
				if (l.contains("packet loss")) {
					String loss = l.replaceAll(".*?([0-9.]+)% packet loss.*", "$1");
					result[2] = Double.parseDouble(loss);
				} else if (l.contains("min/avg/max")) {
					String[] values = l.substring(l.indexOf('=') + 1).trim().split("/");
					result[1] = Double.parseDouble(values[1]);
				}
			}
			result[0] = result[2] < 100 ? 1 : 0;
		} catch (IOException | InterruptedException | NumberFormatException e) {
			logger.warn("ping failed: " + e.getMessage());
		}
		return result;
	}

	// ------------------------------------------------------------------ the face (LEDs + OLED)

	/**
	 * LED 1 internet, 2 router health, 3 Wi-Fi, 4 devices/security, 5 Teleonome (design section 5).
	 * TODO critical-device list: LED 4 red when a critical device is missing (needs the critical
	 * list table, like the whitelist).
	 */
	void updateFace(String networkSensor, Map<String, Object> r, double[] ping) {
		boolean routerReachable = !"none".equals(r.get("reachableVia"));
		String internet = ping[0] == 0 ? "R" : (ping[1] >= latencyWarningMs || ping[2] > 0 ? "Y" : "G");

		String router;
		if (!routerReachable) router = "r";
		else if ((Double) r.get("cpuTemp") > 85 || (Integer) r.get("memFreePercent") < 15 || (Long) r.get("routerUptime") < 3600) router = "Y";
		else router = "G";

		int radios = (Integer) r.get("radios"), radiosUp = (Integer) r.get("radiosUp");
		String wifi = !routerReachable ? "O" : radiosUp == 0 ? "R" : radiosUp < radios ? "Y" : "G";

		String[] ns = networkSensor.split("#", -1);
		int unknownDevices = parseIntOr(ns[1], -1);
		String devices = unknownDevices < 0 ? "O" : unknownDevices > 0 ? "Y" : "G";
		// traffic watch: blinking red for an hour after a new Suspicious Event, then at least yellow until midnight
		int trafficEvents = trafficWatcher == null ? 0 : trafficWatcher.getEventsToday();
		JSONObject lastTrafficEvent = trafficWatcher == null ? null : trafficWatcher.getLastEvent();
		if (lastTrafficEvent != null && System.currentTimeMillis() - lastTrafficEvent.optLong("Time") < TRAFFIC_EVENT_BLINK_MILLIS) devices = "r";
		else if (trafficEvents > 0 && !"O".equals(devices)) devices = "Y";

		boardCommand("SetLeds#" + internet + "#" + router + "#" + wifi + "#" + devices + "#G", null, BOARD_ANSWER_TIMEOUT_MILLIS);

		long up = (Long) r.get("routerUptime");
		oledPage(1, "Internet",
				ping[0] > 0 ? "UP " + Math.round(ping[1]) + " ms, loss " + Math.round(ping[2]) + "%" : "DOWN",
				"WAN " + r.get("wanIp"),
				"Down " + twoDecimals.format((Double) r.get("wanDownMbps")) + " Mbit/s",
				"Up   " + twoDecimals.format((Double) r.get("wanUpMbps")) + " Mbit/s",
				"Speed " + twoDecimals.format(lastDownloadSpeed) + "/" + twoDecimals.format(lastUploadSpeed));
		oledPage(2, "Router",
				routerReachable ? "Up " + (up / 86400) + "d " + ((up / 3600) % 24) + "h " + ((up / 60) % 60) + "m" : "UNREACHABLE",
				"CPU " + twoDecimals.format((Double) r.get("cpuTemp")) + " C",
				"Load " + twoDecimals.format((Double) r.get("load")),
				"Memory free " + r.get("memFreePercent") + "%");
		oledPage(3, "Devices",
				"Total   " + (ns[0].equals("-1") ? "?" : ns[0]),
				"Unknown " + (unknownDevices < 0 ? "?" : String.valueOf(unknownDevices)),
				"WiFi 2.4 " + r.get("clients24") + "  5G " + r.get("clients5"));
		if (trafficWatcher != null) {
			Object[] t = trafficWatcher.todayTotals();
			String last = lastTrafficEvent == null ? "none" : new SimpleDateFormat("HH:mm").format(new Timestamp(lastTrafficEvent.optLong("Time")))
					+ " " + lastTrafficEvent.optString("Rule");
			String lastWho = lastTrafficEvent == null ? "" : lastTrafficEvent.optString("Device");
			oledPage(5, "Security",
					trafficWatcher.isRouterHelperOk() ? "Events today " + trafficEvents : "Traffic: no router",
					"Last " + last,
					lastWho,
					"Up " + TrafficWatcher.mb((Long) t[0]) + " Dn " + TrafficWatcher.mb((Long) t[1]) + " MB",
					"Top " + t[2]);
		}
	}

	private static int parseIntOr(String s, int fallback) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	// ------------------------------------------------------------------ analysis (device list)

	void startAnalysis() {
		if (generatingAnalysis) {
			logger.debug("analysis already running");
			return;
		}
		new Thread(this::generateAnalysis, "CleotildeAnalysis").start();
	}

	/**
	 * Same output as NetworkInspector's GenerateAnalysisThread (NetworkSensor.json + Mnemosyne),
	 * but the device list comes from the router instead of arp-scan: who is associated on Wi-Fi,
	 * plus complete entries in the router's ARP table, named from DHCP leases / host hints.
	 */
	private void generateAnalysis() {
		generatingAnalysis = true;
		try {
			JSONArray previous = new JSONArray();
			File file = new File(NETWORK_SENSOR_FILE);
			if (file.isFile()) {
				String[] tokens = FileUtils.readFileToString(file, Charset.defaultCharset()).split("#", -1);
				if (tokens.length > 2) previous = new JSONArray(tokens[2]);
			}

			JSONArray deviceList = routerDeviceList();
			JSONObject changes = diff(previous, deviceList);

			long now = System.currentTimeMillis();
			if (now - lastSpeedTestMillis > speedTestIntervalHours * 3600_000L) runSpeedTest(now);

			String sampleTimeString = new SimpleDateFormat("dd/MM/yy HH:mm:ss").format(new Timestamp(now));
			aMnemosyneManager.storeNetworkStatus(deviceList, changes, now, sampleTimeString);
			JSONArray lastActivity = addHistory(aMnemosyneManager.getLastNetworkSensorDeviceActivity(), now);
			addDeviceDetails(lastActivity, deviceList);
			// Counted here from the present devices only: Mnemosyne's counts include the devices
			// reported missing in this sample, which kept LED 4 yellow for devices long gone.
			int numberOfDevices = 0, numberUnknown = 0;
			for (int i = 0; i < lastActivity.length(); i++) {
				JSONObject d = lastActivity.getJSONObject(i);
				if (!d.optBoolean(TeleonomeConstants.IS_DEVICE_PRESENT)) continue;
				numberOfDevices++;
				if (!d.optBoolean(TeleonomeConstants.WHITE_LIST_STATUS)) numberUnknown++;
			}
			String data = numberOfDevices + "#" + numberUnknown + "#" + lastActivity + "#" + changes
					+ "#" + twoDecimals.format(lastDownloadSpeed) + "#" + twoDecimals.format(lastUploadSpeed)
					+ "#" + twoDecimals.format(lastSpeedTestPing) + "#" + now + "#" + sampleTimeString;
			FileUtils.writeStringToFile(file, data, Charset.defaultCharset(), false);
			logger.debug("analysis done, devices=" + numberOfDevices + " unknown=" + numberUnknown);
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		} finally {
			generatingAnalysis = false;
		}
	}

	private JSONArray routerDeviceList() throws IOException {
		Map<String, String> names = new HashMap<>();
		Map<String, String> ips = new HashMap<>();
		JSONObject hints = ubus.call("luci-rpc", "getHostHints");
		for (Iterator<String> it = hints.keys(); it.hasNext();) {
			String key = it.next();
			String mac = key.toLowerCase();
			JSONObject h = hints.getJSONObject(key);
			if (h.has("name")) names.put(mac, h.getString("name"));
			JSONArray v4 = h.optJSONArray("ipaddrs");
			if (v4 != null && v4.length() > 0) ips.put(mac, v4.getString(0));
		}
		JSONArray leases = ubus.call("luci-rpc", "getDHCPLeases").optJSONArray("dhcp_leases");
		if (leases != null) {
			for (int i = 0; i < leases.length(); i++) {
				JSONObject l = leases.getJSONObject(i);
				String mac = l.optString("macaddr").toLowerCase();
				if (!l.optString("hostname").isEmpty()) names.putIfAbsent(mac, l.getString("hostname"));
				if (!l.optString("ipaddr").isEmpty()) ips.put(mac, l.getString("ipaddr"));
			}
		}

		Map<String, String> present = new HashMap<>();  // mac -> connection
		JSONArray radios = ubus.call("iwinfo", "devices").getJSONArray("devices");
		for (int i = 0; i < radios.length(); i++) {
			String dev = radios.getString(i);
			int frequency = ubus.call("iwinfo", "info", new JSONObject().put("device", dev)).optInt("frequency");
			JSONArray assoc = ubus.call("iwinfo", "assoclist", new JSONObject().put("device", dev)).getJSONArray("results");
			for (int j = 0; j < assoc.length(); j++) {
				present.put(assoc.getJSONObject(j).getString("mac").toLowerCase(), frequency < 3000 ? "wifi 2.4" : "wifi 5");
			}
		}
		// /proc/net/arp: IP, HW type, Flags, HW address, Mask, Device. Flags 0x2 = complete.
		String arp = ubus.call("file", "read", new JSONObject().put("path", "/proc/net/arp")).getString("data");
		for (String row : arp.split("\n")) {
			String[] f = row.trim().split("\\s+");
			if (f.length < 6 || !f[2].equals("0x2") || !f[5].startsWith("br-lan")) continue;
			String mac = f[3].toLowerCase();
			ips.putIfAbsent(mac, f[0]);
			present.putIfAbsent(mac, "wired");
		}
		for (Map.Entry<String, String> e : sweep().entrySet()) {
			String mac = e.getKey();
			if (present.containsKey(mac)) continue;
			ips.put(mac, e.getValue());  // a fixed IP; a lease the router remembers may be stale
			if (e.getValue().equals(routerAddress)) names.putIfAbsent(mac, "OpenWrt router");
			present.put(mac, SWEEP_ONLY);
		}

		JSONObject nicknames = readNicknames();
		// the names devices announce themselves (mDNS): used when the router has none, and shown on the card
		java.util.List<String> presentIps = new java.util.ArrayList<>();
		for (String mac : present.keySet()) if (!ips.getOrDefault(mac, "").isEmpty()) presentIps.add(ips.get(mac));
		Map<String, String> networkNames = DeviceIdentifier.networkNames(presentIps, NETWORK_NAME_TIMEOUT_SECONDS);
		JSONArray list = new JSONArray();
		for (Map.Entry<String, String> e : present.entrySet()) {
			String mac = e.getKey();
			JSONObject d = new JSONObject();
			d.put(TeleonomeConstants.MAC_ADDRESS, mac);
			d.put(TeleonomeConstants.IP_ADDRESS, ips.getOrDefault(mac, ""));
			String networkName = networkNames.getOrDefault(ips.getOrDefault(mac, ""), "");
			if (!networkName.isEmpty()) d.put(NETWORK_NAME, networkName);
			// nickname, else the router's name, else the name it announces, else its MAC
			String name = deviceName(names.containsKey(mac) ? names.get(mac) : networkName.isEmpty() ? mac : networkName);
			String nickname = nicknames.optString(mac, "");
			d.put(TeleonomeConstants.DEVICE_NAME, nickname.isEmpty() ? name : nickname);
			if (!nickname.isEmpty()) d.put(ORIGINAL_NAME, name);
			d.put(CONNECTION, e.getValue());
			list.put(d);
		}
		return list;
	}

	/**
	 * Active ARP sweep from the Pi (added 2026-10-18), mac -> ip. The router only knows devices
	 * whose traffic goes through it; a device with a fixed IP that only talks to others on the
	 * switch or behind another access point (the shed link radios .251/.254, KitchenPanel) never
	 * shows up there. The Pi is on the same switch, and devices answer ARP even when they ignore
	 * ping, so this finds them. A /24 takes about 2 s. The Hypothalamus runs as root, which
	 * arp-scan needs.
	 * The router still sees devices the sweep misses (Wi-Fi in power save, the Pi itself), so the
	 * sweep only adds to the router's list.
	 */
	private Map<String, String> sweep() {
		Map<String, String> found = new HashMap<>();
		// both go into a shell command, so only network/interface characters
		String networks = sweepNetworks.replace(",", " ").trim();
		if (networks.isEmpty()) return found;
		if (!networks.matches("[0-9./ ]+") || !sweepInterface.matches("[A-Za-z0-9._-]+")) {
			logger.warn("bad Sweep Networks / Sweep Interface, not sweeping: " + sweepNetworks + " " + sweepInterface);
			return found;
		}
		try {
			ArrayList lines = Utils.executeCommand("/usr/sbin/arp-scan -I " + sweepInterface + " -q -x -r 2 " + networks);
			for (Object line : lines) {
				String[] f = line.toString().trim().split("\\s+");
				if (f.length >= 2 && f[1].matches("[0-9a-fA-F:]{17}")) found.putIfAbsent(f[1].toLowerCase(), f[0]);
			}
		} catch (IOException | InterruptedException e) {
			logger.warn("sweep failed: " + e.getMessage());
		}
		return found;
	}

	/**
	 * The router's host hints end in its local domain ("Annabelle.lan"); arp-scan, and so the
	 * existing whitelist entries, used the bare name.
	 */
	private static String deviceName(String routerName) {
		return routerName.endsWith(".lan") ? routerName.substring(0, routerName.length() - 4) : routerName;
	}

	/**
	 * Adds Vendor, Connection and Private MAC to each device of the list Mnemosyne returns (which
	 * only has name, IP, MAC and the status flags), for the web app's device page: most devices
	 * without a DHCP name can be recognised by vendor ("Apple", "Espressif", "Raspberry Pi").
	 * Private MAC: the locally administered bit is set, as phones and laptops do when they
	 * randomise their Wi-Fi MAC, so the vendor can't be known.
	 */
	/**
	 * Adds First Seen / Last Seen (millis) to the present devices and replaces the absent ones with
	 * every device seen in the last Device History Days that isn't here now (added 2026-10-18).
	 * The last scan alone only reports a device as missing once, so it vanished from the cards
	 * about 3 minutes after leaving; the history lets the cards say when it was last here, and
	 * Forget (forgetDevice) deletes that history.
	 */
	private JSONArray addHistory(JSONArray lastActivity, long now) {
		JSONObject sightings = aMnemosyneManager.getNetworkDeviceSightings(now - deviceHistoryDays * 86_400_000L);
		JSONArray toReturn = new JSONArray();
		JSONObject nicknames = readNicknames();
		java.util.Set<String> presentMacs = new java.util.HashSet<>();
		for (int i = 0; i < lastActivity.length(); i++) {
			JSONObject d = lastActivity.getJSONObject(i);
			if (!d.optBoolean(TeleonomeConstants.IS_DEVICE_PRESENT)) continue;
			String mac = d.optString(TeleonomeConstants.MAC_ADDRESS).toLowerCase();
			presentMacs.add(mac);
			JSONObject s = sightings.optJSONObject(mac);
			if (s != null) d.put(FIRST_SEEN, s.getLong(FIRST_SEEN)).put(LAST_SEEN, s.getLong(LAST_SEEN));
			toReturn.put(d);
		}
		for (String mac : sightings.keySet()) {
			if (presentMacs.contains(mac)) continue;
			JSONObject s = sightings.getJSONObject(mac);
			String name = s.optString(TeleonomeConstants.DEVICE_NAME, mac);
			String nickname = nicknames.optString(mac, "");
			JSONObject d = new JSONObject();
			if (!nickname.isEmpty() && !nickname.equals(name)) {
				d.put(ORIGINAL_NAME, name);
				name = nickname;
			}
			d.put(TeleonomeConstants.DEVICE_NAME, name);
			d.put(TeleonomeConstants.MAC_ADDRESS, mac);
			d.put(TeleonomeConstants.IP_ADDRESS, s.optString(TeleonomeConstants.IP_ADDRESS, ""));
			d.put(TeleonomeConstants.WHITE_LIST_STATUS, aMnemosyneManager.isDeviceInWhiteList(name, mac));
			d.put(TeleonomeConstants.IS_DEVICE_PRESENT, false);
			d.put(TeleonomeConstants.IS_DEVICE_MISSING, true);
			d.put(TeleonomeConstants.IS_DEVICE_NEW, false);
			d.put(FIRST_SEEN, s.getLong(FIRST_SEEN)).put(LAST_SEEN, s.getLong(LAST_SEEN));
			toReturn.put(d);
		}
		return toReturn;
	}

	private void addDeviceDetails(JSONArray devices, JSONArray presentNow) {
		Map<String, String> connections = new HashMap<>(), originalNames = new HashMap<>(), networkNames = new HashMap<>();
		for (int i = 0; i < presentNow.length(); i++) {
			JSONObject d = presentNow.getJSONObject(i);
			connections.put(d.optString(TeleonomeConstants.MAC_ADDRESS), d.optString(CONNECTION));
			if (d.has(ORIGINAL_NAME)) originalNames.put(d.optString(TeleonomeConstants.MAC_ADDRESS), d.getString(ORIGINAL_NAME));
			if (d.has(NETWORK_NAME)) networkNames.put(d.optString(TeleonomeConstants.MAC_ADDRESS), d.getString(NETWORK_NAME));
		}
		long weekAgo = System.currentTimeMillis() - 7 * 86_400_000L;
		java.util.Set<String> prefixes = new java.util.HashSet<>();
		for (int i = 0; i < devices.length(); i++) {
			String p = ouiPrefix(devices.getJSONObject(i).optString(TeleonomeConstants.MAC_ADDRESS));
			if (p != null) prefixes.add(p);
		}
		Map<String, String> vendors = lookUpVendors(prefixes);
		for (int i = 0; i < devices.length(); i++) {
			JSONObject d = devices.getJSONObject(i);
			String mac = d.optString(TeleonomeConstants.MAC_ADDRESS).toLowerCase();
			boolean privateMac = isPrivateMac(mac);
			String prefix = ouiPrefix(mac);
			d.put(VENDOR, privateMac ? "Private MAC" : prefix == null ? "" : vendors.getOrDefault(prefix, ""));
			d.put(CONNECTION, connections.getOrDefault(mac, ""));
			d.put(PRIVATE_MAC, privateMac);
			if (originalNames.containsKey(mac)) d.put(ORIGINAL_NAME, originalNames.get(mac));
			if (networkNames.containsKey(mac)) d.put(NETWORK_NAME, networkNames.get(mac));
			// what it looks up, and the best guess of what it is (present devices; the traffic watch has the DNS)
			JSONArray looksUp = new JSONArray();
			if (trafficStore != null && d.optBoolean(TeleonomeConstants.IS_DEVICE_PRESENT, true) && !mac.isEmpty()) {
				for (String domain : DeviceIdentifier.domainNames(trafficStore.domains(mac, weekAgo, LOOKS_UP_DOMAINS))) looksUp.put(domain);
			}
			if (looksUp.length() > 0) d.put(LOOKS_UP, looksUp);
			java.util.List<String> domains = new java.util.ArrayList<>();
			for (int k = 0; k < looksUp.length(); k++) domains.add(looksUp.getString(k));
			// the names it goes by (announced, and the router's or the old one behind a nickname) are clues too
			String names = d.optString(NETWORK_NAME) + " " + d.optString(ORIGINAL_NAME, d.optString(TeleonomeConstants.DEVICE_NAME));
			JSONObject guess = DeviceIdentifier.guess(d.optString(VENDOR), names, d.optString(CONNECTION), domains);
			if (!guess.getString("Guess").isEmpty()) d.put(GUESS, guess.getString("Guess")).put(GUESS_BECAUSE, guess.getString("Because"));
		}
	}

	/** "28:37:37:3f:a3:35" -> "283737", or null if it isn't a MAC address. */
	private static String ouiPrefix(String mac) {
		String hex = mac == null ? "" : mac.replace(":", "").replace("-", "").toUpperCase();
		return hex.matches("[0-9A-F]{12}") ? hex.substring(0, 6) : null;
	}

	private static boolean isPrivateMac(String mac) {
		String p = ouiPrefix(mac);
		return p != null && (Integer.parseInt(p.substring(1, 2), 16) & 0x2) != 0;
	}

	/**
	 * Reads the IEEE vendor file once per analysis for just the prefixes needed, instead of keeping
	 * ~30,000 vendors in the Hypothalamus heap. arp-scan's file: "XXXXXX<TAB>Vendor".
	 */
	private Map<String, String> lookUpVendors(java.util.Set<String> prefixes) {
		Map<String, String> found = new HashMap<>();
		File f = new File(OUI_FILE);
		if (prefixes.isEmpty() || !f.isFile()) return found;
		try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f))) {
			String line;
			while ((line = r.readLine()) != null && found.size() < prefixes.size()) {
				int tab = line.indexOf('\t');
				if (tab != 6) continue;
				String p = line.substring(0, 6).toUpperCase();
				if (prefixes.contains(p)) found.put(p, line.substring(tab + 1).trim());
			}
		} catch (IOException e) {
			logger.warn("Could not read " + OUI_FILE + ": " + e.getMessage());
		}
		return found;
	}

	/**
	 * Names that left / arrived since the last scan. Matched by MAC (since 2026-11-04, when nicknames came:
	 * by name, a renamed device was reported as one leaving and another arriving); a device without a MAC
	 * is matched by name.
	 */
	private JSONObject diff(JSONArray previous, JSONArray current) {
		JSONArray missing = new JSONArray(), added = new JSONArray();
		Map<String, String> before = new HashMap<>(), now = new HashMap<>();  // mac (or name) -> name
		// Only devices that were present last time: one that was already missing was reported
		// then, so it isn't reported again and drops off the list instead of staying forever.
		for (int i = 0; i < previous.length(); i++) {
			JSONObject p = previous.getJSONObject(i);
			if (p.optBoolean(TeleonomeConstants.IS_DEVICE_PRESENT, true)) before.put(diffKey(p), p.optString(TeleonomeConstants.DEVICE_NAME));
		}
		for (int i = 0; i < current.length(); i++) {
			JSONObject c = current.getJSONObject(i);
			now.put(diffKey(c), c.optString(TeleonomeConstants.DEVICE_NAME));
		}
		for (Map.Entry<String, String> e : before.entrySet()) if (!now.containsKey(e.getKey())) missing.put(e.getValue());
		for (Map.Entry<String, String> e : now.entrySet()) if (!before.containsKey(e.getKey())) added.put(e.getValue());
		return new JSONObject().put("Missing", missing).put("New", added);
	}

	private static String diffKey(JSONObject d) {
		String mac = d.optString(TeleonomeConstants.MAC_ADDRESS, "").toLowerCase();
		return mac.isEmpty() ? "name:" + d.optString(TeleonomeConstants.DEVICE_NAME) : mac;
	}

	// ------------------------------------------------------------------ nicknames (Rename button)

	JSONObject readNicknames() {
		File f = new File(NICKNAMES_FILE);
		if (!f.isFile()) return new JSONObject();
		try {
			return new JSONObject(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
		} catch (IOException | JSONException e) {
			logger.warn("Could not read " + NICKNAMES_FILE + ", no nicknames: " + e.getMessage());
			return new JSONObject();
		}
	}

	/**
	 * RenameDevice#mac#nickname from the Rename button (code-protected like the whitelist commands); an
	 * empty nickname goes back to the router's name. The card changes at once (NetworkSensor.json is updated
	 * here); the whitelist and the history match by MAC, so a known device stays known.
	 */
	synchronized void renameDevice(String mac, String nickname) {
		String m = mac == null ? "" : mac.trim().toLowerCase();
		if (!m.matches("[0-9a-f]{2}(:[0-9a-f]{2}){5}")) {
			logger.info("RenameDevice refused, bad MAC " + mac);
			return;
		}
		String n = nickname == null ? "" : nickname.replaceAll("[\\p{Cntrl}#]", "").trim();
		if (n.length() > NICKNAME_MAX_LENGTH) n = n.substring(0, NICKNAME_MAX_LENGTH).trim();
		JSONObject nicknames = readNicknames();
		if (n.isEmpty()) nicknames.remove(m);
		else nicknames.put(m, n);
		try {
			FileUtils.writeStringToFile(new File(NICKNAMES_FILE), nicknames.toString(2), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warn("Could not write " + NICKNAMES_FILE + ": " + e.getMessage());
			return;
		}
		logger.info("RenameDevice " + m + " -> " + (n.isEmpty() ? "(router name)" : n));
		try {
			File file = new File(NETWORK_SENSOR_FILE);
			if (!file.isFile()) return;
			String[] t = FileUtils.readFileToString(file, Charset.defaultCharset()).split("#", -1);
			if (t.length != 9) return;
			JSONArray devices = new JSONArray(t[2]);
			for (int i = 0; i < devices.length(); i++) {
				JSONObject d = devices.getJSONObject(i);
				if (!m.equalsIgnoreCase(d.optString(TeleonomeConstants.MAC_ADDRESS))) continue;
				String original = d.has(ORIGINAL_NAME) ? d.getString(ORIGINAL_NAME) : d.optString(TeleonomeConstants.DEVICE_NAME);
				if (n.isEmpty()) {
					d.put(TeleonomeConstants.DEVICE_NAME, original);
					d.remove(ORIGINAL_NAME);
				} else {
					d.put(TeleonomeConstants.DEVICE_NAME, n);
					d.put(ORIGINAL_NAME, original);
				}
			}
			t[2] = devices.toString();
			FileUtils.writeStringToFile(file, String.join("#", t), Charset.defaultCharset(), false);
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	private void runSpeedTest(long now) {
		try {
			ArrayList results = Utils.executeCommand("speedtest-cli --json");
			if (!results.isEmpty()) {
				JSONObject s = new JSONObject(results.get(0).toString());
				lastDownloadSpeed = s.getDouble("download") / (8 * 1024 * 1024);
				lastUploadSpeed = s.getDouble("upload") / (8 * 1024 * 1024);
				lastSpeedTestPing = s.getDouble("ping");
				lastSpeedTestMillis = now;
			}
		} catch (IOException | InterruptedException | JSONException e) {
			logger.warn("speedtest failed: " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------ whitelist (NetworkInspector parity)

	void addDeviceToWhiteList(String deviceName, String mac) {
		logger.debug("AddToWhiteList " + deviceName + " " + mac + " " + aMnemosyneManager.addDeviceToWhiteList(deviceName, mac));
		updateWhiteListStatusInFile(deviceName, true);
	}

	/** mac may be empty (older web app); when given, rows saved under an older name go too. */
	void removeDeviceFromWhiteList(String deviceName, String mac) {
		logger.debug("RemoveDeviceFromWhiteList " + deviceName + " " + mac + " " + aMnemosyneManager.removeDeviceFromWhiteList(deviceName, mac));
		updateWhiteListStatusInFile(deviceName, false);
	}

	/**
	 * Forget, for devices in "Not seen" only (the web app shows it there): deletes the device's
	 * history so it leaves the list. Its whitelist entry is kept, so it is still known if it comes
	 * back. A device that is here now would just be seen again at the next scan, so it is refused.
	 */
	void forgetDevice(String deviceName, String mac) {
		try {
			File file = new File(NETWORK_SENSOR_FILE);
			String[] t = FileUtils.readFileToString(file, Charset.defaultCharset()).split("#", -1);
			if (t.length != 9) return;
			JSONArray devices = new JSONArray(t[2]), kept = new JSONArray();
			for (int i = 0; i < devices.length(); i++) {
				JSONObject d = devices.getJSONObject(i);
				boolean same = mac.isEmpty() ? deviceName.equals(d.optString(TeleonomeConstants.DEVICE_NAME))
						: mac.equalsIgnoreCase(d.optString(TeleonomeConstants.MAC_ADDRESS));
				if (same && d.optBoolean(TeleonomeConstants.IS_DEVICE_PRESENT)) {
					logger.info("ForgetDevice refused, " + deviceName + " " + mac + " is present");
					return;
				}
				if (!same) kept.put(d);
			}
			logger.debug("ForgetDevice " + deviceName + " " + mac + " " + aMnemosyneManager.forgetNetworkDevice(deviceName, mac));
			t[2] = kept.toString();
			FileUtils.writeStringToFile(file, String.join("#", t), Charset.defaultCharset(), false);
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	void addAllDevicesToWhiteList() {
		try {
			String[] tokens = FileUtils.readFileToString(new File(NETWORK_SENSOR_FILE), Charset.defaultCharset()).split("#", -1);
			JSONArray devices = new JSONArray(tokens[2]);
			for (int i = 0; i < devices.length(); i++) {
				JSONObject d = devices.getJSONObject(i);
				if (!d.optBoolean(TeleonomeConstants.WHITE_LIST_STATUS)) {
					addDeviceToWhiteList(d.getString(TeleonomeConstants.DEVICE_NAME), d.getString(TeleonomeConstants.MAC_ADDRESS));
				}
			}
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	private void updateWhiteListStatusInFile(String deviceName, boolean status) {
		try {
			File file = new File(NETWORK_SENSOR_FILE);
			String[] t = FileUtils.readFileToString(file, Charset.defaultCharset()).split("#", -1);
			if (t.length != 9) return;
			JSONArray devices = new JSONArray(t[2]);
			for (int i = 0; i < devices.length(); i++) {
				if (deviceName.equals(devices.getJSONObject(i).optString(TeleonomeConstants.DEVICE_NAME))) {
					devices.getJSONObject(i).put(TeleonomeConstants.WHITE_LIST_STATUS, status);
					break;
				}
			}
			t[2] = devices.toString();
			FileUtils.writeStringToFile(file, String.join("#", t), Charset.defaultCharset(), false);
		} catch (IOException | JSONException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

	// ------------------------------------------------------------------ mother (TOTP on the board)

	@Override
	public String getCommandCode() throws IOException {
		for (int i = 0; i < MOTHER_TRIES; i++) {
			// the answer is the bare code, so accept any line and check it is 6 digits
			String answer = boardCommand("GetCommandCode", "", BOARD_ANSWER_TIMEOUT_MILLIS);
			if (answer != null && answer.matches("\\d{6}")) return answer;
			logger.warn("GetCommandCode: bad answer from board: " + answer);
		}
		return "";
	}

	/** Commands from Cleotilde's page: firmware 14+ accepts the same code for 15 minutes (the page remembers it). */
	@Override
	public boolean verifyUserCommandCode(String userCode) throws IOException {
		return Boolean.TRUE.equals(askBoardToVerify(userCode, false));
	}

	/**
	 * True / false as the board answered, null if it gave no answer: the framework treats that as
	 * invalid, the doorbell poller asks again rather than calling a good code wrong.
	 * ring: the doorbell's once-only check (VerifyRingCode, firmware 14+; Zhinu is untrusted, so no replays);
	 * otherwise VerifyUserCode, which firmware 14+ lets the page reuse within the window. Firmware before 14
	 * doesn't know VerifyRingCode, so a ring falls back to VerifyUserCode, which was once-only there.
	 */
	Boolean askBoardToVerify(String userCode, boolean ring) {
		if (userCode == null || !userCode.trim().matches("\\d{6}")) return false;
		String verb = ring ? "VerifyRingCode#" : "VerifyUserCode#";
		for (int i = 0; i < MOTHER_TRIES; i++) {
			// a resend after a lost answer is safe: the board answers valid again for 15 s
			String answer = boardCommand(verb + userCode.trim(), null, BOARD_ANSWER_TIMEOUT_MILLIS);
			if (TeleonomeConstants.COMMAND_REQUEST_VALID_CODE.equals(answer)) return true;
			if (TeleonomeConstants.COMMAND_REQUEST_INVALID_CODE.equals(answer)) return false;
			if (answer != null && answer.startsWith("Failure-Unknown command") && verb.startsWith("VerifyRingCode")) {
				verb = "VerifyUserCode#";  // firmware before 14
				i--;
				continue;
			}
			logger.warn(verb + ": bad answer from board: " + answer);
		}
		return null;
	}

	int getCodeFailuresLastHour() {
		return codeFailuresLastHour;
	}

	boolean isCodeLockedOut() {
		return codeLockedOut;
	}

	/**
	 * Release of the doorbell lock (5 wrong codes = every code refused for an hour), asked for with the
	 * Release button on Cleotilde's page, which only the home network reaches; no code is needed, as
	 * the board would refuse any while locked. Firmware 8 forgets the wrong codes (ResetCodeLockout);
	 * older firmware keeps them only in RAM, so a Reboot clears them too.
	 */
	String releaseCodeLock() {
		String answer = boardCommand("ResetCodeLockout", null, BOARD_ANSWER_TIMEOUT_MILLIS);
		if (answer != null && answer.startsWith("Failure-Unknown command")) {
			answer = boardCommand("Reboot", null, BOARD_ANSWER_TIMEOUT_MILLIS);
		}
		logger.info("Doorbell lock release -> " + answer);
		if (answer != null && answer.startsWith("Ok-")) {
			codeFailuresLastHour = 0;
			codeLockedOut = false;
		}
		return answer == null ? "Failure-No answer from board" : answer;
	}

	/** OLED page 4 shows the last ring. TODO: a valid ring sets Requested Open on its Network Rules. */
	void doorbellAnswered(JSONObject ring) {
		String when = new SimpleDateFormat("dd/MM HH:mm:ss").format(new Timestamp(ring.optLong("Received")));
		JSONArray services = ring.optJSONArray("Services");
		String what = services == null || services.length() == 0 ? "code test" : services.join(",").replace("\"", "");
		oledPage(4, "Doorbell", when, "Code " + ring.optString("Result"), what, ring.optInt("Minutes") / 60.0 + " h");
	}

	/** Same as SimpleMicroController: the DigitalGeppetto secret stays in its file on the Pi. */
	@Override
	public String getDigitalGeppettoCommandCode() throws IOException {
		String unEncodedKey = FileUtils.readFileToString(new File("DigitalGeppettoSecretKey"), "UTF-8").trim();
		try {
			return new TOTP().generateCurrentNumberFromUnencodedString(unEncodedKey);
		} catch (GeneralSecurityException e) {
			logger.warn(Utils.getStringException(e));
			return "";
		}
	}

	/** The board has no power control over the Pi, so there is nothing to tell it (as SimpleMicroController). */
	@Override
	public boolean rebootingHypothalamus() throws IOException {
		return true;
	}

	@Override
	public boolean shuttingDownHypothalamus() throws IOException {
		return true;
	}

	// ------------------------------------------------------------------ power control (inactive)

	/**
	 * PowerCycle#modem / PowerCycle#router. Built now so the denome actions exist; inactive until
	 * Cleotilde Build 1 has relays. The board answers "Failure-NoRelayHardware" today.
	 */
	String powerCycle(String target) {
		int channel = "modem".equalsIgnoreCase(target) ? 1 : "router".equalsIgnoreCase(target) ? 2 : 0;
		if (channel == 0) return "Failure-Unknown power target " + target;
		String answer = boardCommand("Relay#" + channel + "#CYCLE#10", null, BOARD_ANSWER_TIMEOUT_MILLIS);
		logger.info("PowerCycle " + target + " -> " + answer);
		return answer == null ? "Failure-No answer from board" : answer;
	}
}
