package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONObject;

/**
 * One field packet: a zip whose root holds manifest.json, which says what the packet is -
 *   {
 *     "Packet Type": "SoilVisit",          picks the FieldPacketProcessor (SoilVisitPacketProcessor)
 *     "Packet Version": 1,                 the type's layout version, so a type can change later
 *     "Packet Id": "<uuid>",               unique per packet: a re-send of the same packet is harmless
 *     "Sender": "Paula",                   who made it
 *     "Created": "2026-11-27T10:41:00Z",   when it was made (not when it was sent)
 *     "Files": { "data.json": "<sha256 hex>", "photos/photo_1.jpg": "<sha256 hex>" }
 *   }
 * The type is stated, never guessed (unlike LoRa packets, which are told apart by size). Every file the manifest
 * lists must be in the zip with that SHA-256, and nothing else may be (a cut-off upload or a stray file fails).
 * Names are relative, without "..", so nothing can be written outside a packet's own folder.
 */
public class FieldPacket {

	public static final String MANIFEST = "manifest.json";
	private static final String ID_PATTERN = "[A-Za-z0-9_-]{8,80}";
	private static final String TYPE_PATTERN = "[A-Za-z][A-Za-z0-9]{1,40}";
	private static final String NAME_PATTERN = "[A-Za-z0-9_.-]+(/[A-Za-z0-9_.-]+)*";
	private static final int MAX_FILES = 200;

	private final JSONObject manifest;
	private final Map<String, byte[]> files;

	private FieldPacket(JSONObject m, Map<String, byte[]> f) {
		manifest = m;
		files = f;
	}

	public static FieldPacket open(byte[] zip) throws FieldPacketException {
		Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
			ZipEntry e;
			byte[] buf = new byte[65536];
			while ((e = in.getNextEntry()) != null) {
				if (e.isDirectory()) continue;
				String name = e.getName();
				if (!name.matches(NAME_PATTERN) || name.contains("..")) throw new FieldPacketException("bad file name in the zip: " + name);
				if (files.size() >= MAX_FILES) throw new FieldPacketException("more than " + MAX_FILES + " files");
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				int n;
				while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
				files.put(name, out.toByteArray());
			}
		} catch (IOException e) {
			throw new FieldPacketException("not a readable zip: " + e.getMessage());
		}
		byte[] m = files.get(MANIFEST);
		if (m == null) throw new FieldPacketException("no " + MANIFEST + " at the zip's root");
		JSONObject manifest;
		try {
			manifest = new JSONObject(new String(m, StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new FieldPacketException(MANIFEST + " is not JSON: " + e.getMessage());
		}
		return new FieldPacket(manifest, files);
	}

	// Everything that can be checked without processing the packet.
	public void check(Set<String> acceptedTypes) throws FieldPacketException {
		String type = getPacketType();
		if (!type.matches(TYPE_PATTERN)) throw new FieldPacketException("bad Packet Type: " + type);
		if (!acceptedTypes.contains(type)) throw new FieldPacketException("this Teleonome does not accept " + type + " packets (it accepts " + acceptedTypes + ")");
		if (!getPacketId().matches(ID_PATTERN)) throw new FieldPacketException("bad Packet Id: " + getPacketId());
		if (getPacketVersion() < 1) throw new FieldPacketException("no Packet Version");
		JSONObject listed = manifest.optJSONObject("Files");
		if (listed == null) throw new FieldPacketException("the manifest has no Files");
		for (Iterator<String> it = listed.keys(); it.hasNext();) {
			String name = it.next();
			byte[] data = files.get(name);
			if (data == null) throw new FieldPacketException("listed but missing: " + name);
			if (!sha256(data).equalsIgnoreCase(listed.optString(name))) throw new FieldPacketException("SHA-256 does not match: " + name);
		}
		for (String name : files.keySet()) {
			if (!name.equals(MANIFEST) && !listed.has(name)) throw new FieldPacketException("in the zip but not in the manifest: " + name);
		}
	}

	public JSONObject getManifest() { return manifest; }
	public String getPacketType() { return manifest.optString("Packet Type", ""); }
	public int getPacketVersion() { return manifest.optInt("Packet Version", 0); }
	public String getPacketId() { return manifest.optString("Packet Id", ""); }
	public String getSender() { return manifest.optString("Sender", ""); }
	public Map<String, byte[]> getFiles() { return files; }

	public byte[] file(String name) { return files.get(name); }

	public JSONObject json(String name) throws FieldPacketException {
		byte[] b = files.get(name);
		if (b == null) throw new FieldPacketException("no " + name + " in the packet");
		try {
			return new JSONObject(new String(b, StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new FieldPacketException(name + " is not JSON: " + e.getMessage());
		}
	}

	public static String sha256(byte[] data) {
		try {
			byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
			StringBuilder sb = new StringBuilder();
			for (byte b : d) sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
