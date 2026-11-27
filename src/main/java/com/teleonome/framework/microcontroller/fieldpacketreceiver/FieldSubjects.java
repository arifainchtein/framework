package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Areas, subjects and Arrivals (2026-11-27). A subject is what visits are about: a Plant (the Meyer lemon next wall)
 * or a Bed (raised bed 3). Subjects live in areas (the Orangerie, the Greenhouse, the Potager), one area each, or in
 * none yet. A visit that is not for a known subject waits in Arrivals until a person files it.
 *
 * On disk, under the Field Packets Directory (the only copy; the denome only points at it, see syncDenome):
 *   areas.json                         {"Areas": [{"Name", "Created"}]}
 *   plants/<record id>/record.json     a subject: {"Record Id", "Record Type": "Plant"|"Bed", "Plant" (its name),
 *                                      "Area" ("" = none), "Created", "Last Updated", "Visits": [...]} + its photos
 *   arrivals/<packet id>/arrival.json  a visit not filed yet: {"Visit": {...as in a record}, "Suggested": {"Name",
 *                                      "Type", "Area", "Record Id"}, "Received"} + its photos
 * Every read-modify-write is under FieldPacketStore.RECORD_LOCK.
 */
public class FieldSubjects {

	public static final String AREAS_FILE = "areas.json";
	public static final String ARRIVAL_FILE = "arrival.json";
	public static final String TYPE_PLANT = "Plant";
	public static final String TYPE_BED = "Bed";
	public static final String AREA_NAME_PATTERN = "[A-Za-z0-9][A-Za-z0-9 _'-]{0,39}";
	private static final String ID_PATTERN = "[A-Za-z0-9_.-]+";

	public static File arrivalsDir(FieldPacketSettings s) {
		return new File(s.root, "arrivals");
	}

	static String now() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date());
	}

	static JSONObject readJson(File f) throws IOException {
		return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
	}

	public static String normalType(String t) {
		return TYPE_BED.equalsIgnoreCase(t == null ? "" : t.trim()) ? TYPE_BED : TYPE_PLANT;
	}

	// ---------------------------------------------------------------- areas

	public static JSONArray areas(FieldPacketSettings s) throws IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File f = new File(s.root, AREAS_FILE);
			if (!f.isFile()) return new JSONArray();
			JSONArray a = readJson(f).optJSONArray("Areas");
			return a == null ? new JSONArray() : a;
		}
	}

	public static boolean areaExists(FieldPacketSettings s, String name) throws IOException {
		JSONArray a = areas(s);
		for (int i = 0; i < a.length(); i++) if (a.getJSONObject(i).optString("Name").equals(name)) return true;
		return false;
	}

	private static void writeAreas(FieldPacketSettings s, JSONArray areas) throws IOException {
		FieldPacketStore.writeJson(new File(s.root, AREAS_FILE), new JSONObject().put("Areas", areas));
	}

	public static void createArea(FieldPacketSettings s, String name) throws IOException, FieldPacketException {
		name = name == null ? "" : name.trim();
		if (!name.matches(AREA_NAME_PATTERN)) throw new FieldPacketException("an area name is 1-40 letters, digits, spaces, _ ' -");
		synchronized (FieldPacketStore.RECORD_LOCK) {
			if (areaExists(s, name)) throw new FieldPacketException("there is already an area " + name);
			JSONArray a = areas(s);
			a.put(new JSONObject().put("Name", name).put("Created", now()));
			writeAreas(s, a);
		}
	}

	public static void renameArea(FieldPacketSettings s, String name, String newName) throws IOException, FieldPacketException {
		newName = newName == null ? "" : newName.trim();
		if (!newName.matches(AREA_NAME_PATTERN)) throw new FieldPacketException("an area name is 1-40 letters, digits, spaces, _ ' -");
		synchronized (FieldPacketStore.RECORD_LOCK) {
			if (!areaExists(s, name)) throw new FieldPacketException("no area " + name);
			if (!newName.equals(name) && areaExists(s, newName)) throw new FieldPacketException("there is already an area " + newName);
			JSONArray a = areas(s);
			for (int i = 0; i < a.length(); i++) if (a.getJSONObject(i).optString("Name").equals(name)) a.getJSONObject(i).put("Name", newName);
			writeAreas(s, a);
			for (JSONObject r : subjects(s)) if (name.equals(r.optString("Area"))) setArea(s, r.optString("Record Id"), newName);
		}
	}

	// The area's subjects stay, with no area (they show under "Not in an area").
	public static void deleteArea(FieldPacketSettings s, String name) throws IOException, FieldPacketException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			if (!areaExists(s, name)) throw new FieldPacketException("no area " + name);
			JSONArray a = areas(s), kept = new JSONArray();
			for (int i = 0; i < a.length(); i++) if (!a.getJSONObject(i).optString("Name").equals(name)) kept.put(a.getJSONObject(i));
			writeAreas(s, kept);
			for (JSONObject r : subjects(s)) if (name.equals(r.optString("Area"))) setArea(s, r.optString("Record Id"), "");
		}
	}

	// ---------------------------------------------------------------- subjects

	public static List<JSONObject> subjects(FieldPacketSettings s) throws IOException {
		List<JSONObject> list = new ArrayList<JSONObject>();
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File[] dirs = s.plants().listFiles(File::isDirectory);
			if (dirs == null) return list;
			Arrays.sort(dirs, (x, y) -> x.getName().compareToIgnoreCase(y.getName()));
			for (File d : dirs) {
				File f = new File(d, FieldPacketStore.RECORD_FILE);
				if (!f.isFile()) continue;
				try {
					list.add(readJson(f));
				} catch (Exception e) {
					// a hand-edited record that no longer parses - left alone
				}
			}
		}
		return list;
	}

	public static File recordFile(FieldPacketSettings s, String recordId) {
		if (recordId == null || !recordId.matches(ID_PATTERN) || recordId.startsWith(".")) return null;
		File f = new File(FieldPacketStore.recordDir(s, recordId), FieldPacketStore.RECORD_FILE);
		return f.isFile() ? f : null;
	}

	public static void setArea(FieldPacketSettings s, String recordId, String area) throws IOException, FieldPacketException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File f = recordFile(s, recordId);
			if (f == null) throw new FieldPacketException("no plant or bed " + recordId);
			if (area != null && !area.isEmpty() && !areaExists(s, area)) throw new FieldPacketException("no area " + area);
			JSONObject r = readJson(f);
			r.put("Area", area == null ? "" : area);
			r.put("Last Updated", now());
			FieldPacketStore.writeJson(f, r);
		}
	}

	// A new subject; its id comes from its name, made unique if a subject of another name already has it.
	public static String createSubject(FieldPacketSettings s, String name, String type, String area) throws IOException, FieldPacketException {
		name = name == null ? "" : name.trim();
		if (name.isEmpty()) throw new FieldPacketException("the plant or bed has no name");
		if (area != null && !area.isEmpty() && !areaExists(s, area)) throw new FieldPacketException("no area " + area);
		synchronized (FieldPacketStore.RECORD_LOCK) {
			String base = FieldPacketStore.recordId(name), id = base;
			for (int i = 2; recordFile(s, id) != null; i++) id = base + "_" + i;
			JSONObject r = new JSONObject();
			r.put("Record Id", id);
			r.put("Record Type", normalType(type));
			r.put("Plant", name);
			r.put("Area", area == null ? "" : area);
			r.put("Created", now());
			r.put("Last Updated", now());
			r.put("Visits", new JSONArray());
			File dir = FieldPacketStore.recordDir(s, id);
			dir.mkdirs();
			FieldPacketStore.writeJson(new File(dir, FieldPacketStore.RECORD_FILE), r);
			return id;
		}
	}

	// Adds a visit (with its photos, still in fromDir) to a subject; a visit already there (same Packet Id) is not added twice.
	public static void addVisit(FieldPacketSettings s, String recordId, JSONObject visit, File fromDir) throws IOException, FieldPacketException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File f = recordFile(s, recordId);
			if (f == null) throw new FieldPacketException("no plant or bed " + recordId);
			JSONObject r = readJson(f);
			JSONArray visits = r.getJSONArray("Visits");
			for (int i = 0; i < visits.length(); i++) if (visit.optString("Packet Id").equals(visits.getJSONObject(i).optString("Packet Id"))) return;
			JSONArray photos = visit.optJSONArray("Photos");
			if (photos != null && fromDir != null) {
				for (int i = 0; i < photos.length(); i++) {
					String name = photos.getJSONObject(i).optString("File");
					File src = new File(fromDir, name);
					if (name.matches(ID_PATTERN) && src.isFile()) Files.copy(src.toPath(), new File(f.getParentFile(), name).toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			visits.put(visit);
			r.put("Last Updated", now());
			FieldPacketStore.writeJson(f, r);
		}
	}

	// ---------------------------------------------------------------- arrivals

	public static List<JSONObject> arrivals(FieldPacketSettings s) throws IOException {
		List<JSONObject> list = new ArrayList<JSONObject>();
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File[] dirs = arrivalsDir(s).listFiles(File::isDirectory);
			if (dirs == null) return list;
			Arrays.sort(dirs, (x, y) -> Long.compare(x.lastModified(), y.lastModified()));
			for (File d : dirs) {
				File f = new File(d, ARRIVAL_FILE);
				if (!f.isFile()) continue;
				try {
					list.add(readJson(f).put("Arrival Id", d.getName()));
				} catch (Exception e) {
					// left alone
				}
			}
		}
		return list;
	}

	public static File arrivalDir(FieldPacketSettings s, String arrivalId) {
		if (arrivalId == null || !arrivalId.matches(ID_PATTERN) || arrivalId.startsWith(".")) return null;
		File d = new File(arrivalsDir(s), arrivalId);
		return new File(d, ARRIVAL_FILE).isFile() ? d : null;
	}

	// photos: name -> bytes, already named as they will be stored (<packet id>_photo_N.jpg)
	public static void addArrival(FieldPacketSettings s, String packetId, JSONObject visit, JSONObject suggested, java.util.Map<String, byte[]> photos) throws IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File d = new File(arrivalsDir(s), packetId);
			d.mkdirs();
			for (java.util.Map.Entry<String, byte[]> e : photos.entrySet()) Files.write(new File(d, e.getKey()).toPath(), e.getValue());
			JSONObject a = new JSONObject();
			a.put("Visit", visit);
			a.put("Suggested", suggested);
			a.put("Received", now());
			FieldPacketStore.writeJson(new File(d, ARRIVAL_FILE), a);
		}
	}

	// Files an arrival into an existing subject (recordId), or into a new one (name, type, area) when recordId is empty.
	// Returns the subject's id.
	public static String fileArrival(FieldPacketSettings s, String arrivalId, String recordId, String name, String type, String area) throws IOException, FieldPacketException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File d = arrivalDir(s, arrivalId);
			if (d == null) throw new FieldPacketException("no arrival " + arrivalId + " (already filed?)");
			if (recordId == null || recordId.isEmpty()) recordId = createSubject(s, name, type, area);
			else if (recordFile(s, recordId) == null) throw new FieldPacketException("no plant or bed " + recordId);
			JSONObject a = readJson(new File(d, ARRIVAL_FILE));
			addVisit(s, recordId, a.getJSONObject("Visit"), d);
			deleteDir(d);
			return recordId;
		}
	}

	public static void discardArrival(FieldPacketSettings s, String arrivalId) throws IOException, FieldPacketException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File d = arrivalDir(s, arrivalId);
			if (d == null) throw new FieldPacketException("no arrival " + arrivalId);
			File discarded = new File(new File(s.root, "discarded"), arrivalId);
			discarded.getParentFile().mkdirs();
			Files.move(d.toPath(), discarded.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void deleteDir(File d) {
		File[] files = d.listFiles();
		if (files != null) for (File f : files) f.delete();
		d.delete();
	}
}
