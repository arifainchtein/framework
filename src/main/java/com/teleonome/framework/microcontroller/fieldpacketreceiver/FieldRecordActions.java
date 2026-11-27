package com.teleonome.framework.microcontroller.fieldpacketreceiver;

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
 */
public class FieldRecordActions {

	public static final String[] ACTIONS = { "createArea", "renameArea", "deleteArea", "fileArrival", "discardArrival", "moveSubject" };

	public static boolean known(String action) {
		for (String a : ACTIONS) if (a.equals(action)) return true;
		return false;
	}

	// Returns what was done, for the action's state; FieldPacketException when it can't be done (the reason is kept).
	public static String apply(FieldPacketSettings s, JSONObject a) throws FieldPacketException, IOException {
		String action = a.optString("Action");
		switch (action) {
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
