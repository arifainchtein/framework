package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;

import org.json.JSONObject;

/**
 * Changes a person makes to the field records (2026-11-27), from the record pages (FieldRecordPages). Like a packet,
 * an action arrives at the listener (POST /fieldaction, JSON body, X-Totp-Code header), waits in the inbox as
 * inbox/<action id>.action with its code, and is carried out in the Async Cycle once Annabelle's ESP32 has accepted
 * the code. Actions:
 *   {"Action": "createArea",     "Name"}
 *   {"Action": "renameArea",     "Name", "New Name"}
 *   {"Action": "deleteArea",     "Name"}                       its plants and beds stay, with no area
 *   {"Action": "fileArrival",    "Arrival Id", "Record Id"}    into an existing plant or bed, or
 *   {"Action": "fileArrival",    "Arrival Id", "Name", "Type": "Plant"|"Bed", "Area"}   into a new one
 *   {"Action": "discardArrival", "Arrival Id"}                 kept in discarded/, not deleted
 *   {"Action": "moveSubject",    "Record Id", "Area"}          "" = no area
 *   {"Action": "setAnalysis",    "Enabled", "Model", "Effort"} + a key, kept apart (FieldPacketStore.writeSecret):
 *                                writes /home/pi/Teleonome/secrets/anthropic.key (owner-only, the folder too) when a key
 *                                came, and analysis.properties
 *   {"Action": "addCredit",      "Amount", "Note"}             credits bought in the Console (AnalysisSpend)
 *   {"Action": "setBalance",     "Amount"}                     the balance the Console shows: counting starts again
 */
public class FieldRecordActions {

	public static final String[] ACTIONS = { "createArea", "renameArea", "deleteArea", "fileArrival", "discardArrival", "moveSubject", "setAnalysis", "addCredit", "setBalance" };
	public static final String KEY_FILE = "anthropic.key";

	// The Teleonome's secrets (2026-11-27): <Teleonome dir>/secrets/, owner-only - /home/pi/Teleonome/secrets/anthropic.key
	// (the Field Packets Directory is <Teleonome dir>/FieldPackets).
	public static File secretsDir(FieldPacketSettings s) {
		File teleonome = s.root.getAbsoluteFile().getParentFile();
		return new File(teleonome == null ? s.root : teleonome, "secrets");
	}

	public static File keyFile(FieldPacketSettings s) {
		return new File(secretsDir(s), KEY_FILE);
	}
	public static final String[] MODELS = { "claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5" };
	public static final String[] EFFORTS = { "low", "medium", "high", "xhigh", "max" };

	public static boolean known(String action) {
		for (String a : ACTIONS) if (a.equals(action)) return true;
		return false;
	}

	// Returns what was done, for the action's state; FieldPacketException when it can't be done (the reason is kept).
	public static String apply(FieldPacketSettings s, JSONObject a) throws FieldPacketException, IOException {
		return apply(s, a, null);
	}

	// key: the API key that came with a setAnalysis action, or null
	public static String apply(FieldPacketSettings s, JSONObject a, String key) throws FieldPacketException, IOException {
		String action = a.optString("Action");
		switch (action) {
		case "setAnalysis": {
			String model = a.optString("Model", MODELS[0]), effort = a.optString("Effort", "high");
			if (!java.util.Arrays.asList(MODELS).contains(model)) throw new FieldPacketException("unknown model " + model);
			if (!java.util.Arrays.asList(EFFORTS).contains(effort)) throw new FieldPacketException("unknown effort " + effort);
			File keyFile = keyFile(s);
			if (key != null && !key.isEmpty()) {
				File dir = secretsDir(s);
				dir.mkdirs();
				try {
					java.nio.file.Files.setPosixFilePermissions(dir.toPath(), java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
				} catch (UnsupportedOperationException e) {
					// not a POSIX file system
				}
				if (!key.matches("sk-ant-[A-Za-z0-9_-]{20,300}")) throw new FieldPacketException("that does not look like an Anthropic API key (sk-ant-...)");
				FieldPacketStore.writeOwnerOnly(keyFile, key);
			}
			boolean on = a.optBoolean("Enabled", true);
			if (on && !keyFile.isFile()) throw new FieldPacketException("no API key yet - paste one to turn analysis on");
			String props = "# written by the Field Records Analysis page " + FieldSubjects.now() + "\n"
					+ "provider=" + (on ? "claude" : "none") + "\nmodel=" + model + "\neffort=" + effort + "\napiKeyFile=" + keyFile.getAbsolutePath() + "\n";
			FieldPacketStore.writeOwnerOnly(new File(s.root, "analysis.properties")  /* FieldRecordAnalyzer.PROPERTIES_FILE; not touched here so the SDK isn't loaded */, props);
			return "Analysis " + (on ? "on, " + model + ", effort " + effort : "off") + (key != null && !key.isEmpty() ? ", new key saved" : "");
		}
		case "addCredit":
			return AnalysisSpend.addCredit(s, a.optString("Amount"), a.optString("Note", ""));
		case "setBalance":
			return AnalysisSpend.setBalance(s, a.optString("Amount"));
		case "createArea":
			FieldSubjects.createArea(s, a.optString("Name"));
			return "Area " + a.optString("Name").trim() + " created";
		case "renameArea":
			FieldSubjects.renameArea(s, a.optString("Name"), a.optString("New Name"));
			return "Area " + a.optString("Name") + " is now " + a.optString("New Name").trim();
		case "deleteArea":
			FieldSubjects.deleteArea(s, a.optString("Name"));
			return "Area " + a.optString("Name") + " deleted; its plants and beds have no area now";
		case "fileArrival": {
			String id = FieldSubjects.fileArrival(s, a.optString("Arrival Id"), a.optString("Record Id", ""), a.optString("Name"), a.optString("Type"), a.optString("Area", ""));
			return "Filed into " + id;
		}
		case "discardArrival":
			FieldSubjects.discardArrival(s, a.optString("Arrival Id"));
			return "Arrival discarded (kept in discarded/)";
		case "moveSubject":
			FieldSubjects.setArea(s, a.optString("Record Id"), a.optString("Area", ""));
			return a.optString("Record Id") + (a.optString("Area", "").isEmpty() ? " has no area now" : " moved to " + a.optString("Area"));
		default:
			throw new FieldPacketException("unknown action " + action);
		}
	}
}
