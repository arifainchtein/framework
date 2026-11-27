package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * SoilVisit packets (version 1, from PaulaDeployer's "Test Soil Sensor", 2026-11-27): one visit to one plant or bed -
 * the soil probe's readings, phone photos and a note. data.json is the visit as PaulaDeployer saved it:
 *   {"plant" (the subject's name), "note", "savedAt", "probeAddress",
 *    "readings": [{"time", "address", "moist", "temp", "ph", "ec", "n", "p", "k", "salt", "raw"}],
 *    "photos": [{"file": "photos/photo_1.jpg", "takenAt"}],
 *    and, since areas (all optional, so older PaulaDeployers still work):
 *    "target": "<record id>"   the existing plant or bed the visit is for (Paula's "For" list)
 *    "arrivals": true          leave it in Arrivals even if it has a target
 *    "subjectType": "Plant" | "Bed",  "area": "<area name>"   suggestions for a new subject}
 *
 * Where the visit goes (FieldSubjects):
 *   - "arrivals": true                          -> Arrivals, with its target / name / type / area as suggestions
 *   - a target that exists                      -> that subject's timeline
 *   - no target, but a subject of that name     -> that subject's timeline (a subject, once filed, is remembered)
 *   - otherwise (a new plant or bed)            -> Arrivals, for a person to file
 * A visit in a record: {"Packet Id", "Packet Type", "Sender", "Visit Time", "Received", "Note", "Probe Address",
 * "Readings": [...], "Photos": [{"File", "Taken At"}], "Analyses": [], "Follow Ups": []}.
 */
public class SoilVisitPacketProcessor extends FieldPacketProcessor {

	public JSONObject process(FieldPacket packet, FieldPacketSettings settings) throws FieldPacketException, IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			return processLocked(packet, settings);
		}
	}

	private JSONObject processLocked(FieldPacket packet, FieldPacketSettings settings) throws FieldPacketException, IOException {
		if (packet.getPacketVersion() != 1) throw new FieldPacketException("SoilVisit version " + packet.getPacketVersion() + " is not known (1 is)");
		JSONObject data = packet.json("data.json");
		String name = data.optString("plant", "").trim();
		String target = data.optString("target", "").trim();
		if (name.isEmpty() && target.isEmpty()) throw new FieldPacketException("the visit says neither which plant or bed it is for, nor a name");
		JSONArray readings = data.optJSONArray("readings");
		if (readings == null) readings = new JSONArray();

		// photos, named as they are kept: <packet id>_photo_N.jpg
		Map<String, byte[]> photoBytes = new LinkedHashMap<String, byte[]>();
		JSONArray photos = new JSONArray();
		JSONArray sentPhotos = data.optJSONArray("photos");
		if (sentPhotos != null) {
			for (int i = 0; i < sentPhotos.length(); i++) {
				JSONObject p = sentPhotos.getJSONObject(i);
				String file = p.optString("file");
				byte[] jpeg = packet.file(file);
				if (jpeg == null) throw new FieldPacketException("data.json lists " + file + " but it is not in the packet");
				String stored = packet.getPacketId() + "_" + new File(file).getName();
				photoBytes.put(stored, jpeg);
				photos.put(new JSONObject().put("File", stored).put("Taken At", p.optString("takenAt", "")));
			}
		}

		JSONObject visit = new JSONObject();
		visit.put("Packet Id", packet.getPacketId());
		visit.put("Packet Type", packet.getPacketType());
		visit.put("Sender", packet.getSender());
		visit.put("Visit Time", packet.getManifest().optString("Created", data.optString("savedAt", "")));
		visit.put("Received", FieldSubjects.now());
		visit.put("Note", data.optString("note", ""));
		visit.put("Probe Address", data.optInt("probeAddress", 2));
		visit.put("Readings", readings);
		visit.put("Photos", photos);
		visit.put("Analyses", new JSONArray());
		visit.put("Follow Ups", new JSONArray());

		String recordId = null;
		if (!data.optBoolean("arrivals", false)) {
			if (!target.isEmpty() && FieldSubjects.recordFile(settings, target) != null) recordId = target;
			else if (target.isEmpty() && !name.isEmpty() && FieldSubjects.recordFile(settings, FieldPacketStore.recordId(name)) != null) recordId = FieldPacketStore.recordId(name);
		}

		JSONObject result = new JSONObject();
		if (recordId != null) {
			File dir = FieldPacketStore.recordDir(settings, recordId);
			for (Map.Entry<String, byte[]> e : photoBytes.entrySet()) java.nio.file.Files.write(new File(dir, e.getKey()).toPath(), e.getValue());
			FieldSubjects.addVisit(settings, recordId, visit, null);
			result.put("Record Id", recordId);
		} else {
			JSONObject suggested = new JSONObject();
			suggested.put("Name", name);
			suggested.put("Type", FieldSubjects.normalType(data.optString("subjectType", FieldSubjects.TYPE_PLANT)));
			suggested.put("Area", data.optString("area", ""));
			suggested.put("Record Id", target);
			FieldSubjects.addArrival(settings, packet.getPacketId(), visit, suggested, photoBytes);
			result.put("Arrival", packet.getPacketId());
		}
		return result;
	}
}
