package com.teleonome.framework.microcontroller.fieldpacketreceiver;

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
 * Field Packet Receiver (2026-11-27): lets a Teleonome receive packets of field information - a zip with a
 * manifest.json, the data, a note and photos - from field tools such as Paula (PaulaDeployer). The first
 * packet type is SoilVisit (a soil probe reading + photos + note for one plant, the start of a plant's
 * "patient record"); more types are added as FieldPacketProcessor classes.
 *
 * Built like SFTPPublisherMicroController:
 *   - Reception is independent of both cycles: init() starts FieldPacketListener, an HTTP listener thread
 *     that checks the zip's manifest and drops it, with the sender's TOTP code, into the inbox.
 *   - The code is checked in the Async Cycle, through the mother microcontroller (Annabelle's ESP32 holds the
 *     customer's secret: VerifyUserCode), before anything is unpacked.
 *   - Ingestion happens in the Async Cycle, through an action and a mutation (FieldPacketReceiverHomeoBoxGenerator
 *     in sertoli): the post-pulse action "Field Packet Receiver Prepare Ingest" raises the command
 *     "Ingest Field Packets", which runs the mutation of the same name; MappedBusThread executes its action,
 *     "Field Packet Receiver Ingest", whose command reaches FieldPacketReceiverWriter.write() and unpacks,
 *     validates and processes every packet waiting in the inbox.
 *   - Every other command (the pulse's own requests) is answered "Ok" without doing anything, so the
 *     synchronous cycle never waits on it.
 *
 * Config parameters (the "Field Packet Receiver Controller Config Parameter List"):
 *   Listen Port               the HTTP port (default 8095)
 *   Field Packets Directory   inbox/, store/, rejected/, plants/ go here (default /home/pi/Teleonome/FieldPackets)
 *   Accepted Packet Types     comma separated, e.g. "SoilVisit"; anything else is refused at reception
 *   Secret File               only for a Teleonome without a mother microcontroller: a file holding the base32
 *                             secret (never the secret itself); empty = not used
 *   Maximum Packet Megabytes  uploads larger than this are refused (default 20)
 */
public class FieldPacketReceiverMicroController extends MicroController {

	FieldPacketReceiverWriter aWriter;
	FieldPacketReceiverReader aReader;
	FieldPacketListener aListener;
	Logger logger;

	public FieldPacketReceiverMicroController(Hypothalamus h, DenomeManager d, String n) {
		super(h, d, n);
		logger = Logger.getLogger(getClass());
	}

	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		FieldPacketSettings settings = new FieldPacketSettings(configParams);
		aWriter = new FieldPacketReceiverWriter(new StringWriter(), settings, aDenomeManager, hypothalamus);
		aReader = new FieldPacketReceiverReader(new StringReader(""), aWriter);
		if (aListener == null) {
			aListener = new FieldPacketListener(settings);
			try {
				aListener.start();
			} catch (IOException e) {
				// The Teleonome still runs; nothing can be received until the port is free
				logger.warn("Field Packet Receiver could not listen on port " + settings.listenPort + ": " + e.getMessage());
			}
		}
		logger.info("FieldPacketReceiverMicroController started, " + settings);
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		return aWriter;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return aReader;
	}
}
