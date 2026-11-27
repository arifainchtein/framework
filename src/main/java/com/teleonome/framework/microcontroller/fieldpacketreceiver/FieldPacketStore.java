package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.json.JSONObject;

/**
 * Where field packets and records live on disk, under the "Field Packets Directory":
 *   inbox/<packet id>.zip                  received, waiting for the Async Cycle
 *   store/<type>/<packet id>/packet.zip    ingested, kept as received
 *   rejected/<packet id>.zip + .txt        received but failed ingestion, with the reason
 *   plants/<record id>/record.json         a plant's record ("patient record") and its photos
 * The denome only knows that a record exists and where to get it (a dene in Purpose:Field Records); the record
 * itself, which grows with every visit, photo, analysis and follow-up, stays here.
 */
public class FieldPacketStore {

	public static final String RECORD_FILE = "record.json";
	// held while a record.json is read and rewritten (ingestion and FieldRecordAnalyzer both write records)
	public static final Object RECORD_LOCK = new Object();
	private static final String NAME_PATTERN = "[A-Za-z0-9_.-]+";

	// Waiting or ingested; a rejected packet may be sent again (with a new code), so it does not count.
	public static boolean known(FieldPacketSettings s, String packetId) {
		String st = state(s, packetId).optString("State");
		return "Inbox".equals(st) || "Ingested".equals(st) || "Arrivals".equals(st) || "Done".equals(st);
	}

	public static int waiting(FieldPacketSettings s) {
		File[] zips = s.inbox().listFiles((d, name) -> name.endsWith(".zip") || name.endsWith(ACTION_EXTENSION));
		return zips == null ? 0 : zips.length;
	}

	// Actions (FieldRecordActions) wait in the inbox as <id>.action beside their .code; when carried out, the
	// result is kept as actions/<id>.json.
	public static final String ACTION_EXTENSION = ".action";

	public static File actionsDone(FieldPacketSettings s) {
		return new File(s.root, "actions");
	}

	// "x.zip" / "x.action" -> "x"
	public static String idOf(File f) {
		String n = f.getName();
		int dot = n.lastIndexOf('.');
		return dot > 0 ? n.substring(0, dot) : n;
	}

	// The sender's code, kept beside the zip until the Async Cycle checks it: inbox/<packet id>.code holds
	// "<code> <received millis>".
	public static void writeCode(FieldPacketSettings s, String packetId, String code) throws IOException {
		Files.write(new File(s.inbox(), packetId + ".code").toPath(), (code + " " + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
	}

	// {code, received millis as a string}, or null when the packet came without one
	public static String[] readCode(FieldPacketSettings s, String packetId) {
		try {
			String[] t = new String(Files.readAllBytes(new File(s.inbox(), packetId + ".code").toPath()), StandardCharsets.UTF_8).trim().split(" ");
			return t.length == 2 ? t : null;
		} catch (IOException e) {
			return null;
		}
	}

	public static void forgetCode(FieldPacketSettings s, String packetId) {
		new File(s.inbox(), packetId + ".code").delete();
		new File(s.inbox(), packetId + SECRET_EXTENSION).delete();  // a key that came with an action never outlives it
	}

	// An API key sent with a setAnalysis action is kept apart from the action, owner-only, until the action is
	// carried out or refused (forgetCode removes it either way) - never in the action, the logs or the denome.
	public static final String SECRET_EXTENSION = ".secret";

	public static void writeSecret(FieldPacketSettings s, String id, String secret) throws IOException {
		writeOwnerOnly(new File(s.inbox(), id + SECRET_EXTENSION), secret);
	}

	public static String readSecret(FieldPacketSettings s, String id) {
		try {
			return new String(Files.readAllBytes(new File(s.inbox(), id + SECRET_EXTENSION).toPath()), StandardCharsets.UTF_8).trim();
		} catch (IOException e) {
			return null;
		}
	}

	public static void writeOwnerOnly(File f, String text) throws IOException {
		f.getParentFile().mkdirs();
		File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
		Files.write(tmp.toPath(), new byte[0]);
		try {
			Files.setPosixFilePermissions(tmp.toPath(), java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
		} catch (UnsupportedOperationException e) {
			// not a POSIX file system
		}
		Files.write(tmp.toPath(), text.getBytes(StandardCharsets.UTF_8));
		Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	public static void forgetRejected(FieldPacketSettings s, String packetId) {
		new File(s.rejected(), packetId + ".zip").delete();
		new File(s.rejected(), packetId + ".txt").delete();
	}

	public static JSONObject state(FieldPacketSettings s, String packetId) {
		JSONObject o = new JSONObject();
		o.put("Packet Id", packetId);
		if (packetId == null || !packetId.matches(NAME_PATTERN)) {
			o.put("State", "Unknown");
			return o;
		}
		if (new File(s.inbox(), packetId + ".zip").isFile() || new File(s.inbox(), packetId + ACTION_EXTENSION).isFile()) {
			o.put("State", "Inbox");
			return o;
		}
		File done = new File(actionsDone(s), packetId + ".json");
		if (done.isFile()) {
			o.put("State", "Done");
			try {
				o.put("Result", new JSONObject(new String(Files.readAllBytes(done.toPath()), StandardCharsets.UTF_8)).optString("Result"));
			} catch (Exception e) {
				// the state is still right
			}
			return o;
		}
		if (new File(FieldSubjects.arrivalsDir(s), packetId).isDirectory()) {
			o.put("State", "Arrivals");
			return o;
		}
		File reason = new File(s.rejected(), packetId + ".txt");
		if (reason.isFile()) {
			o.put("State", "Rejected");
			try {
				o.put("Reason", new String(Files.readAllBytes(reason.toPath()), StandardCharsets.UTF_8).trim());
			} catch (IOException e) {
				// the state is still right
			}
			return o;
		}
		File[] types = s.store().listFiles(File::isDirectory);
		if (types != null) {
			for (File t : types) {
				if (new File(t, packetId).isDirectory()) {
					o.put("State", "Ingested");
					o.put("Packet Type", t.getName());
					return o;
				}
			}
		}
		o.put("State", "Unknown");
		return o;
	}

	public static File storeDir(FieldPacketSettings s, String type, String packetId) {
		return new File(new File(s.store(), type), packetId);
	}

	public static void reject(FieldPacketSettings s, File zip, String packetId, String reason) throws IOException {
		s.rejected().mkdirs();
		Files.move(zip.toPath(), new File(s.rejected(), zip.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.write(new File(s.rejected(), packetId + ".txt").toPath(), (reason + "\n").getBytes(StandardCharsets.UTF_8));
		forgetCode(s, packetId);
	}

	public static File recordDir(FieldPacketSettings s, String recordId) {
		return new File(s.plants(), recordId);
	}

	// A record's file, or null - never a path outside the record's folder.
	public static File recordFile(FieldPacketSettings s, String recordId, String name) {
		if (recordId == null || name == null || !recordId.matches(NAME_PATTERN) || !name.matches(NAME_PATTERN)
				|| recordId.startsWith(".") || name.startsWith(".")) return null;
		File f = new File(recordDir(s, recordId), name);
		return f.isFile() ? f : null;
	}

	// "Meyer lemon by the shed" -> "Meyer_lemon_by_the_shed": the same plant name always gives the same record.
	public static String recordId(String plantName) {
		String id = plantName.trim().replaceAll("[^A-Za-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
		if (id.length() > 60) id = id.substring(0, 60);
		return id.isEmpty() ? "Unnamed_Plant" : id;
	}

	public static void writeJson(File f, JSONObject o) throws IOException {
		f.getParentFile().mkdirs();
		File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
		Files.write(tmp.toPath(), o.toString(2).getBytes(StandardCharsets.UTF_8));
		Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}
}
