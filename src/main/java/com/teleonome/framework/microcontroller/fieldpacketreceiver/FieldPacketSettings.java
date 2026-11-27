package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;

// The Field Packet Receiver's config parameters, read once from the denes of its Config Parameter List.
public class FieldPacketSettings {

	public static final String CONFIG_DENE_PREFIX = "Field Packet Receiver ";

	public int listenPort = 8095;
	public File root = new File("/home/pi/Teleonome/FieldPackets");
	public Set<String> acceptedTypes = new LinkedHashSet<String>();
	public File secretFile = null;  // only without a mother microcontroller (FieldPacketReceiverWriter)
	public long maximumBytes = 20L * 1024 * 1024;

	public FieldPacketSettings(JSONArray configParams) {
		acceptedTypes.add("SoilVisit");
		if (configParams == null) return;
		for (int i = 0; i < configParams.length(); i++) {
			JSONObject dene = configParams.optJSONObject(i);
			if (dene == null) continue;
			// the dene is "Field Packet Receiver <parameter>" (FieldPacketReceiverHomeoBoxGenerator), its deneword the
			// bare parameter name; a dene named just "<parameter>" works too
			String name = dene.optString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, "");
			if (name.startsWith(CONFIG_DENE_PREFIX)) name = name.substring(CONFIG_DENE_PREFIX.length());
			Object value;
			try {
				value = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(dene, name, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			} catch (Exception e) {
				continue;
			}
			if (value == null) continue;
			String s = value.toString().trim();
			switch (name) {
			case "Listen Port":
				listenPort = Integer.parseInt(s);
				break;
			case "Field Packets Directory":
				root = new File(s);
				break;
			case "Accepted Packet Types":
				acceptedTypes.clear();
				for (String t : s.split(",")) if (!t.trim().isEmpty()) acceptedTypes.add(t.trim());
				break;
			case "Secret File":
				secretFile = s.isEmpty() ? null : new File(s);
				break;
			case "Maximum Packet Megabytes":
				maximumBytes = (long) (Double.parseDouble(s) * 1024 * 1024);
				break;
			default:
				break;
			}
		}
	}

	public File inbox() { return new File(root, "inbox"); }
	public File store() { return new File(root, "store"); }
	public File rejected() { return new File(root, "rejected"); }
	public File plants() { return new File(root, "plants"); }

	public String toString() {
		return "port " + listenPort + ", directory " + root + ", accepts " + acceptedTypes + ", secret file " + secretFile
				+ ", maximum " + maximumBytes / (1024 * 1024) + " MB";
	}
}
