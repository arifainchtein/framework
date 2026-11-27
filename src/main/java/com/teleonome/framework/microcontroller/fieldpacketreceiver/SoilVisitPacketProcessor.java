package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * SoilVisit packets (version 1, from PaulaDeployer's "Test Soil Sensor", 2026-11-27): one visit to one plant -
 * the soil probe's readings, phone photos and a note. data.json is the visit as PaulaDeployer saved it:
 *   {"plant", "note", "savedAt", "probeAddress",
 *    "readings": [{"time", "address", "moist", "temp", "ph", "ec", "n", "p", "k", "salt", "raw"}],
 *    "photos": [{"file": "photos/photo_1.jpg", "takenAt"}]}
 *
 * The visit is added to the plant's record (plants/<record id>/record.json), the "patient record":
 *   {"Record Id", "Record Type": "Plant", "Plant", "Created", "Last Updated",
 *    "Visits": [{"Packet Id", "Packet Type", "Sender", "Visit Time", "Received", "Note", "Probe Address",
 *                "Readings": [...], "Photos": [{"File", "Taken At"}], "Analyses": [], "Follow Ups": []}]}
 * "Analyses" (an LLM's interpretation) and "Follow Ups" are left empty for what comes later. A packet that is
 * ingested twice (it can't be, the Packet Id is checked) would not add the visit twice either.
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
		String plant = data.optString("plant", "").trim();
		if (plant.isEmpty()) throw new FieldPacketException("the visit has no plant name");
		JSONArray readings = data.optJSONArray("readings");
		if (readings == null) readings = new JSONArray();

		String recordId = FieldPacketStore.recordId(plant);
		File dir = FieldPacketStore.recordDir(settings, recordId);
		dir.mkdirs();
		File recordFile = new File(dir, FieldPacketStore.RECORD_FILE);
		String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
		JSONObject record;
		if (recordFile.isFile()) {
			record = new JSONObject(new String(Files.readAllBytes(recordFile.toPath()), "UTF-8"));
		} else {
			record = new JSONObject();
			record.put("Record Id", recordId);
			record.put("Record Type", "Plant");
			record.put("Plant", plant);
			record.put("Created", now);
			record.put("Visits", new JSONArray());
		}
		JSONArray visits = record.getJSONArray("Visits");
		for (int i = 0; i < visits.length(); i++) {
			if (packet.getPacketId().equals(visits.getJSONObject(i).optString("Packet Id"))) {
				return pointer(recordId, plant, recordFile);  // already in the record
			}
		}

		JSONArray photos = new JSONArray();
		JSONArray sentPhotos = data.optJSONArray("photos");
		if (sentPhotos != null) {
			for (int i = 0; i < sentPhotos.length(); i++) {
				JSONObject p = sentPhotos.getJSONObject(i);
				String name = p.optString("file");
				byte[] jpeg = packet.file(name);
				if (jpeg == null) throw new FieldPacketException("data.json lists " + name + " but it is not in the packet");
				String stored = packet.getPacketId() + "_" + new File(name).getName();
				Files.write(new File(dir, stored).toPath(), jpeg);
				JSONObject photo = new JSONObject();
				photo.put("File", stored);
				photo.put("Taken At", p.optString("takenAt", ""));
				photos.put(photo);
			}
		}

		JSONObject visit = new JSONObject();
		visit.put("Packet Id", packet.getPacketId());
		visit.put("Packet Type", packet.getPacketType());
		visit.put("Sender", packet.getSender());
		visit.put("Visit Time", packet.getManifest().optString("Created", data.optString("savedAt", "")));
		visit.put("Received", now);
		visit.put("Note", data.optString("note", ""));
		visit.put("Probe Address", data.optInt("probeAddress", 2));
		visit.put("Readings", readings);
		visit.put("Photos", photos);
		visit.put("Analyses", new JSONArray());
		visit.put("Follow Ups", new JSONArray());
		visits.put(visit);
		record.put("Last Updated", now);
		FieldPacketStore.writeJson(recordFile, record);
		return pointer(recordId, plant, recordFile);
	}

	private static JSONObject pointer(String recordId, String plant, File recordFile) {
		JSONObject o = new JSONObject();
		o.put("Record Id", recordId);
		o.put("Record Type", "Plant");
		o.put("Record Name", plant);
		o.put("Record File", recordFile.getAbsolutePath());
		return o;
	}
}
