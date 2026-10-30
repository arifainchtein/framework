package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;

/**
 * Sends the readings an Annabelle relays to devices.digitalstables.com (devicesheart, MQTT), set up in
 * the Denome like the SFTP Publisher Controller. Config DeneWords: Upload To DigitalStables,
 * DigitalStables Server, DigitalStables Login, DigitalStables Password File, Annabelle Serial (her
 * ESP32's efuse MAC, sent as relay.annabelleSerial so devicesheart records the path "via <serial>").
 */
public class DigitalStablesPublisherMicroController extends MicroController {

	private DigitalStablesPublisherWriter writer;
	private DigitalStablesPublisherReader reader;
	private final Logger logger;

	public DigitalStablesPublisherMicroController(Hypothalamus h, DenomeManager d, String n) {
		super(h, d, n);
		logger = Logger.getLogger(getClass());
	}

	@Override
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		writer = new DigitalStablesPublisherWriter(new StringWriter(), configParams, aDenomeManager);
		reader = new DigitalStablesPublisherReader(new StringReader(""), writer);
		logger.info("DigitalStablesPublisherMicroController initialized");
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		return writer;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return reader;
	}
}
