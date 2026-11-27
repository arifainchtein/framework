package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The Field Packet Receiver's reception (2026-11-27): an HTTP listener of its own (the JDK's HttpServer, no new
 * dependency), running on its own threads, independent of the pulse and of the Async Cycle. It only checks the
 * packet's structure and stores it; the code is checked, and the packet unpacked and processed, in the Async Cycle
 * (FieldPacketReceiverWriter).
 *
 *   POST /fieldpacket          body: the zip; header X-Totp-Code: the sender's current TOTP code
 *        200 {"Status":"Received","Packet Id":..}  in the inbox with its code; checked and ingested at the next
 *                                                  Async Cycle
 *        200 {"Status":"Already Received",..}      the same Packet Id is waiting or was ingested (a re-send is
 *                                                  harmless; a re-send of a rejected packet replaces it)
 *        400 not a valid packet / type not accepted / no code, 413 too big, 503 the inbox is full
 *   GET  /fieldpacket/<packet id>   {"State": "Inbox" | "Ingested" | "Rejected" | "Unknown", "Reason"?} - only the
 *        state, never content, so it needs no code
 *   GET  /fieldrecords/...          read-only pages of the records (FieldRecordPages)
 *
 * Why the code is not checked here: the secret lives in Annabelle's ESP32 (VerifyUserCode, through the mother
 * microcontroller), and its serial port belongs to the Async Cycle - a second thread talking on it would mix its
 * bytes with AsyncData's. The ESP32 accepts codes up to 15 minutes old, so the Async Cycle checks it in time.
 */
public class FieldPacketListener {

	private static final Logger logger = Logger.getLogger(FieldPacketListener.class);
	public static final String TOTP_HEADER = "X-Totp-Code";

	private static final int MAX_WAITING = 20;  // packets in the inbox before more are refused

	private final FieldPacketSettings settings;
	private HttpServer server;

	public FieldPacketListener(FieldPacketSettings s) {
		settings = s;
	}

	public void start() throws IOException {
		settings.inbox().mkdirs();
		settings.store().mkdirs();
		settings.rejected().mkdirs();
		settings.plants().mkdirs();
		server = HttpServer.create(new InetSocketAddress(settings.listenPort), 0);
		server.createContext("/fieldpacket", this::handlePacket);
		server.createContext("/fieldaction", this::handleAction);
		final FieldRecordPages pages = new FieldRecordPages(settings);
		server.createContext("/fieldrecords", ex -> {
			try {
				pages.handle(ex);
			} catch (Throwable e) {  // Errors too (e.g. a class missing from the jar): answer, don't just drop
				logger.warn("Field record page failed: " + e);
				try {
					reply(ex, 500, status("Error", null, String.valueOf(e)));
				} catch (Throwable ignored) {
					ex.close();
				}
			}
		});
		server.setExecutor(Executors.newFixedThreadPool(2, r -> {
			Thread t = new Thread(r, "FieldPacketListener");
			t.setDaemon(true);
			return t;
		}));
		server.start();
		logger.info("Field Packet Receiver listening on port " + settings.listenPort);
	}

	public void stop() {
		if (server != null) server.stop(0);
	}

	private void handlePacket(HttpExchange ex) throws IOException {
		try {
			String path = ex.getRequestURI().getPath();
			if ("GET".equals(ex.getRequestMethod()) && path.startsWith("/fieldpacket/")) {
				String id = path.substring("/fieldpacket/".length());
				reply(ex, 200, FieldPacketStore.state(settings, id));
				return;
			}
			if (!"POST".equals(ex.getRequestMethod())) {
				reply(ex, 405, status("Error", null, "POST a packet zip to /fieldpacket"));
				return;
			}
			String code = ex.getRequestHeaders().getFirst(TOTP_HEADER);
			if (code == null || !code.trim().matches("[0-9]{6}")) {
				reply(ex, 400, status("Error", null, "no 6-digit code in " + TOTP_HEADER));
				return;
			}
			if (FieldPacketStore.waiting(settings) >= MAX_WAITING) {
				reply(ex, 503, status("Error", null, "the inbox is full - send again after the next Async Cycle"));
				return;
			}
			byte[] zip = readBody(ex.getRequestBody(), settings.maximumBytes);
			if (zip == null) {
				reply(ex, 413, status("Error", null, "larger than " + settings.maximumBytes / (1024 * 1024) + " MB"));
				return;
			}
			FieldPacket packet;
			try {
				packet = FieldPacket.open(zip);
				packet.check(settings.acceptedTypes);
			} catch (FieldPacketException e) {
				logger.warn("Field packet refused from " + ex.getRemoteAddress() + ": " + e.getMessage());
				reply(ex, 400, status("Error", null, e.getMessage()));
				return;
			}
			String id = packet.getPacketId();
			if (FieldPacketStore.known(settings, id)) {
				reply(ex, 200, status("Already Received", id, null));
				return;
			}
			FieldPacketStore.forgetRejected(settings, id);  // a re-send of a rejected packet replaces it
			FieldPacketStore.writeCode(settings, id, code.trim());
			File tmp = new File(settings.inbox(), id + ".zip.part");
			Files.write(tmp.toPath(), zip);
			Files.move(tmp.toPath(), new File(settings.inbox(), id + ".zip").toPath(), StandardCopyOption.ATOMIC_MOVE);
			logger.info("Field packet received: " + packet.getPacketType() + " " + id + " from " + packet.getSender() + ", "
					+ zip.length / 1024 + " KB");
			reply(ex, 200, status("Received", id, null).put("Packet Type", packet.getPacketType()));
		} catch (Throwable e) {
			logger.warn("Field packet reception failed: " + e);
			reply(ex, 500, status("Error", null, String.valueOf(e.getMessage())));
		}
	}

	// POST /fieldaction  JSON body {"Action", ...} (FieldRecordActions), header X-Totp-Code -> {"Status":"Received","Action Id"}
	// GET  /fieldaction/<id> -> {"State": "Inbox" | "Done" | "Rejected" | "Unknown", "Result"?, "Reason"?}
	// Like a packet, the action waits in the inbox until the Async Cycle has checked its code with the ESP32.
	private void handleAction(HttpExchange ex) throws IOException {
		try {
			String path = ex.getRequestURI().getPath();
			if ("GET".equals(ex.getRequestMethod()) && path.startsWith("/fieldaction/")) {
				reply(ex, 200, FieldPacketStore.state(settings, path.substring("/fieldaction/".length())));
				return;
			}
			if (!"POST".equals(ex.getRequestMethod())) {
				reply(ex, 405, status("Error", null, "POST an action to /fieldaction"));
				return;
			}
			String code = ex.getRequestHeaders().getFirst(TOTP_HEADER);
			if (code == null || !code.trim().matches("[0-9]{6}")) {
				reply(ex, 400, status("Error", null, "no 6-digit code in " + TOTP_HEADER));
				return;
			}
			if (FieldPacketStore.waiting(settings) >= MAX_WAITING) {
				reply(ex, 503, status("Error", null, "the inbox is full - try again after the next Async Cycle"));
				return;
			}
			byte[] body = readBody(ex.getRequestBody(), 16 * 1024);
			JSONObject action;
			try {
				action = new JSONObject(new String(body == null ? new byte[0] : body, StandardCharsets.UTF_8));
			} catch (Exception e) {
				reply(ex, 400, status("Error", null, "the action is not JSON"));
				return;
			}
			if (!FieldRecordActions.known(action.optString("Action"))) {
				reply(ex, 400, status("Error", null, "unknown action " + action.optString("Action")));
				return;
			}
			String id = "act-" + java.util.UUID.randomUUID();
			settings.inbox().mkdirs();
			FieldPacketStore.writeCode(settings, id, code.trim());
			File tmp = new File(settings.inbox(), id + ".part");
			Files.write(tmp.toPath(), action.toString().getBytes(StandardCharsets.UTF_8));
			Files.move(tmp.toPath(), new File(settings.inbox(), id + FieldPacketStore.ACTION_EXTENSION).toPath(), StandardCopyOption.ATOMIC_MOVE);
			logger.info("Field record action received: " + action.optString("Action") + " " + id);
			reply(ex, 200, new JSONObject().put("Status", "Received").put("Action Id", id));
		} catch (Throwable e) {
			reply(ex, 500, status("Error", null, String.valueOf(e)));
		}
	}

	private static JSONObject status(String status, String id, String why) {
		JSONObject o = new JSONObject();
		o.put("Status", status);
		if (id != null) o.put("Packet Id", id);
		if (why != null) o.put("Reason", why);
		return o;
	}

	private static byte[] readBody(InputStream in, long max) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[65536];
		int n;
		while ((n = in.read(buf)) > 0) {
			out.write(buf, 0, n);
			if (out.size() > max) return null;
		}
		return out.toByteArray();
	}

	private static void reply(HttpExchange ex, int code, JSONObject body) throws IOException {
		byte[] b = body.toString().getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.sendResponseHeaders(code, b.length);
		try (OutputStream out = ex.getResponseBody()) {
			out.write(b);
		}
	}
}
