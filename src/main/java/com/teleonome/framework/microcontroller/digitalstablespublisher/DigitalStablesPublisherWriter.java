package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.utils.Utils;

/**
 * Sends the readings in devices_outbox to devicesheart on devices.digitalstables.com over MQTT
 * (DevicesDigitalStables design doc §16). Wired like the SFTP publisher: an action at Execution Point
 * Post Pulse queues the command "Publish To DigitalStables", the asynchronous cycle runs the mutation
 * of that name, and its action writes the command here. write() sends what is due under a deadline
 * and the reader answers with the result.
 *
 * The MQTT connection lives in this object and stays open between pulses (the Hypothalamus closes the
 * writer after every command, so close() keeps it). Acks arrive on devices/<login>/ack at any time and
 * mark their row acked; a row not acked is sent again at a later pulse. The password is read from the
 * file named in the Denome, never from the Denome itself (it travels in pulses), and never logged.
 */
public class DigitalStablesPublisherWriter extends BufferedWriter {

	public static final String COMMAND_PUBLISH = "Publish To DigitalStables";
	private static final long PUBLISH_DEADLINE_MS = 20000;
	private static final int ROWS_PER_PULSE = 200;
	private static final int CONNECT_TIMEOUT_SECONDS = 10;
	private static final int KEEP_ALIVE_SECONDS = 60;
	// a publish the broker refuses gets no PUBACK; without this Paho waits for ever
	private static final long SEND_TIMEOUT_MS = 10000;
	private static final long PRUNE_EVERY_MS = 60 * 60 * 1000L;

	private final Logger logger = Logger.getLogger(getClass());
	private final DenomeManager aDenomeManager;
	private boolean uploadEnabled = false;
	private String server = "";
	private String login = "";
	private String passwordFile = "";
	// Annabelle's own serial (her ESP32's efuse MAC); she does not report it, so it is set in the Denome
	private String annabelleSerial = "";
	private MqttClient client = null;
	private volatile String publishingResults = "";
	private long lastPrune = 0;

	public DigitalStablesPublisherWriter(Writer out, JSONArray configParams, DenomeManager d) {
		super(out);
		aDenomeManager = d;
		for (int i = 0; i < configParams.length(); i++) {
			try {
				JSONObject dene = configParams.getJSONObject(i);
				String deneName = dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
				Object value = aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, deneName, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (value == null) continue;
				if (deneName.equals(DevicesOutbox.UPLOAD_DENEWORD)) {
					uploadEnabled = Boolean.parseBoolean(value.toString().trim());
				} else if (deneName.equals("DigitalStables Server")) {
					server = value.toString().trim();
				} else if (deneName.equals("DigitalStables Login")) {
					login = value.toString().trim();
				} else if (deneName.equals("DigitalStables Password File")) {
					passwordFile = value.toString().trim();
				} else if (deneName.equals("Annabelle Serial")) {
					annabelleSerial = value.toString().trim().toLowerCase();
				}
			} catch (JSONException e) {
				logger.warn(Utils.getStringException(e));
			}
		}
		logger.warn("DigitalStables publisher: upload=" + uploadEnabled + " server=" + server + " login=" + login + " annabelleSerial=" + annabelleSerial);
	}

	public String getPublishingResults() {
		return publishingResults;
	}

	@Override
	public void write(String command, int off, int len) throws IOException {
		publishingResults = "";
		if (!command.equals(COMMAND_PUBLISH)) {
			publishingResults = "Ok";
			return;
		}
		if (!uploadEnabled) {
			publishingResults = "Ok-Upload off";
			return;
		}
		long deadline = System.currentTimeMillis() + PUBLISH_DEADLINE_MS;
		int sent = 0;
		try {
			ensureConnected();
			List<DevicesOutbox.Row> rows = DevicesOutbox.due(ROWS_PER_PULSE);
			String topic = "devices/" + login + "/reading";
			for (DevicesOutbox.Row row : rows) {
				if (System.currentTimeMillis() > deadline) break;
				JSONObject message = new JSONObject(row.reading.toString());
				JSONObject relay = new JSONObject();
				relay.put("outboxId", row.id);
				relay.put("teleonome", aDenomeManager.getDenomeName());
				if (!annabelleSerial.isEmpty()) relay.put("annabelleSerial", annabelleSerial);
				relay.put("receivedAt", row.createdMillis);
				message.put("relay", relay);
				MqttMessage mqttMessage = new MqttMessage(message.toString().getBytes(StandardCharsets.UTF_8));
				mqttMessage.setQos(1);
				client.publish(topic, mqttMessage);
				DevicesOutbox.markSent(row.id);
				sent++;
			}
			pruneNowAndThen();
			JSONObject status = DevicesOutbox.status();
			publishingResults = "Ok-Published " + sent + "#pending=" + status.getLong("pending") + "#oldestPendingSeconds=" + status.getLong("oldestPendingSeconds");
			if (sent > 0) logger.info("DigitalStables: " + publishingResults);
		} catch (Exception e) {
			// "Failure", like the SFTP publisher; the rows stay and go at a later pulse
			publishingResults = "Failure-Publish To DigitalStables#sent=" + sent + "#" + e.getClass().getSimpleName() + ": " + e.getMessage();
			logger.warn("DigitalStables publish failed after " + sent + " sent: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if (client != null && !client.isConnected()) disconnectQuietly();
		}
	}

	private synchronized void ensureConnected() throws MqttException, IOException {
		if (client != null && client.isConnected()) return;
		disconnectQuietly();
		MqttConnectOptions options = new MqttConnectOptions();
		options.setUserName(login);
		options.setPassword(readPassword());
		options.setCleanSession(true);
		options.setConnectionTimeout(CONNECT_TIMEOUT_SECONDS);
		options.setKeepAliveInterval(KEEP_ALIVE_SECONDS);
		options.setAutomaticReconnect(false);  // reconnected here at the next pulse, with the subscription
		// devicesheart wants the client id to be the login name
		client = new MqttClient(server, login, new MemoryPersistence());
		client.setTimeToWait(SEND_TIMEOUT_MS);
		client.connect(options);
		client.subscribe("devices/" + login + "/ack", 1, (topic, message) -> onAck(new String(message.getPayload(), StandardCharsets.UTF_8)));
		logger.warn("DigitalStables: connected to " + server + " as " + login);
	}

	// {"outboxId", "serialnumber", "secondsTime", "outcome"}; every outcome that comes back is final
	private void onAck(String payload) {
		try {
			JSONObject ack = new JSONObject(payload);
			if (!ack.has("outboxId")) return;
			String outcome = ack.optString("outcome", "");
			DevicesOutbox.markAcked(ack.getLong("outboxId"), outcome);
			if (!outcome.equals("stored") && !outcome.equals("duplicate")) {
				logger.warn("DigitalStables refused reading " + ack.optString("serialnumber") + "/" + ack.opt("secondsTime") + ": " + outcome);
			}
		} catch (Exception e) {
			logger.warn("DigitalStables: bad ack: " + e.getMessage());
		}
	}

	// The file AddLogin wrote on the devices machine: devicesheart.password=<password>
	private char[] readPassword() throws IOException {
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(passwordFile)) {
			properties.load(in);
		}
		String password = properties.getProperty("devicesheart.password");
		if (password == null || password.trim().isEmpty()) throw new IOException("no devicesheart.password in " + passwordFile);
		return password.trim().toCharArray();
	}

	private void pruneNowAndThen() {
		long now = System.currentTimeMillis();
		if (now - lastPrune < PRUNE_EVERY_MS) return;
		lastPrune = now;
		try {
			int deleted = DevicesOutbox.deleteOldAcked();
			if (deleted > 0) logger.info("DigitalStables: deleted " + deleted + " acked outbox rows older than " + DevicesOutbox.KEEP_ACKED_DAYS + " days");
		} catch (Exception e) {
			logger.warn("DigitalStables: could not prune the outbox: " + e.getMessage());
		}
	}

	private void disconnectQuietly() {
		if (client == null) return;
		try {
			if (client.isConnected()) client.disconnectForcibly(1000, 1000);
		} catch (MqttException e) {
			// going away anyway
		}
		try {
			client.close();
		} catch (MqttException e) {
			// going away anyway
		}
		client = null;
	}

	// The Hypothalamus closes the writer after every command; the MQTT connection must survive that.
	@Override
	public void close() {
	}

	@Override
	public void flush() {
	}
}
