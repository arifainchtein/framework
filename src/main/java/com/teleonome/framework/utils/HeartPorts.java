package com.teleonome.framework.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Heart's ports (2026-11-13). Every Teleonome on a Pi uses 1883 (MQTT) and 9999 (websocket), and still does:
 * nothing to set. A Teleonome that shares its machine with another broker (the Factory Teleonome on factory2,
 * where FactoryHeart has 1883) sets them in lib/app.properties, the file the organs already read for the database:
 *   heartPort=1884
 *   heartWebsocketPort=9999
 * The Heart listens on them, every other organ connects to heartPort.
 */
public class HeartPorts {

	public static final int DEFAULT_MQTT_PORT = 1883, DEFAULT_WEBSOCKET_PORT = 9999;
	private static Properties properties = null;

	public static int mqttPort() { return port("heartPort", DEFAULT_MQTT_PORT); }

	public static int websocketPort() { return port("heartWebsocketPort", DEFAULT_WEBSOCKET_PORT); }

	/** tcp://localhost:<heartPort>, what the organs connect to */
	public static String localBrokerAddress() { return "tcp://localhost:" + mqttPort(); }

	private static synchronized int port(String key, int defaultPort) {
		if (properties == null) {
			properties = new Properties();
			try (InputStream in = new FileInputStream(Utils.getLocalDirectory() + "lib/app.properties")) {
				properties.load(in);
			} catch (IOException e) {
				// no file: the defaults
			}
		}
		String v = properties.getProperty(key);
		if (v == null || v.trim().isEmpty()) return defaultPort;
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			return defaultPort;
		}
	}
}
