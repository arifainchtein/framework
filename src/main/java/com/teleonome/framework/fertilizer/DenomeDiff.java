package com.teleonome.framework.fertilizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The differences between two Denomes (2026-11-13), for the Incubator's step 4 and the Hypothalamus' log.
 * Lists of named things (Nuclei, DeneChains, Denes, DeneWords, Mutations...) are matched by Name, so the path of
 * a change reads like an Identity: "Human Interface:Human Interface Control Parameters:Settings:Include in
 * Navigation". A thing added or removed is one change, not one per attribute. Two things with the same Name in one
 * list are told apart by their order ("Name#2").
 */
public class DenomeDiff {

	public static final String ADDED = "Added", REMOVED = "Removed", CHANGED = "Changed";

	public static class Change {
		public final String type, path, attribute;
		public final Object before, after;

		Change(String type, String path, String attribute, Object before, Object after) {
			this.type = type; this.path = path; this.attribute = attribute; this.before = before; this.after = after;
		}

		public JSONObject toJSON() {
			JSONObject o = new JSONObject();
			o.put("Type", type);
			o.put("Path", path);
			if (attribute != null) o.put("Attribute", attribute);
			o.put("Before", before == null ? JSONObject.NULL : before);
			o.put("After", after == null ? JSONObject.NULL : after);
			return o;
		}

		public String toString() {
			if (type.equals(CHANGED)) return CHANGED + " " + path + (attribute == null ? "" : " [" + attribute + "]") + ": " + before + " -> " + after;
			return type + " " + path;
		}
	}

	/** compares the "Denome" of two pulse objects */
	public static List<Change> compare(JSONObject beforePulse, JSONObject afterPulse) {
		List<Change> changes = new ArrayList<Change>();
		walk(beforePulse.optJSONObject("Denome"), afterPulse.optJSONObject("Denome"), "", changes);
		return changes;
	}

	private static void walk(JSONObject a, JSONObject b, String path, List<Change> out) {
		TreeSet<String> keys = new TreeSet<String>();
		if (a != null) keys.addAll(a.keySet());
		if (b != null) keys.addAll(b.keySet());
		for (String k : keys) {
			if (k.equals("Name")) continue;
			Object x = a == null ? null : a.opt(k), y = b == null ? null : b.opt(k);
			if (isNamedList(x) || isNamedList(y)) {
				walkNamedList(x instanceof JSONArray ? (JSONArray) x : new JSONArray(), y instanceof JSONArray ? (JSONArray) y : new JSONArray(), path, out);
			} else if (x instanceof JSONObject && y instanceof JSONObject) {
				walk((JSONObject) x, (JSONObject) y, path, out);
			} else if (!same(x, y)) {
				out.add(new Change(x == null ? ADDED : y == null ? REMOVED : CHANGED, path.isEmpty() ? k : path, path.isEmpty() ? null : k, plain(x), plain(y)));
			}
		}
	}

	private static void walkNamedList(JSONArray a, JSONArray b, String path, List<Change> out) {
		Map<String, JSONObject> x = byName(a), y = byName(b);
		for (String name : x.keySet()) {
			String p = path.isEmpty() ? name : path + ":" + name;
			if (!y.containsKey(name)) out.add(new Change(REMOVED, p, null, null, null));
			else walk(x.get(name), y.get(name), p, out);
		}
		for (String name : y.keySet()) {
			if (!x.containsKey(name)) out.add(new Change(ADDED, path.isEmpty() ? name : path + ":" + name, null, null, null));
		}
	}

	private static boolean isNamedList(Object o) {
		if (!(o instanceof JSONArray) || ((JSONArray) o).length() == 0) return false;
		Object first = ((JSONArray) o).opt(0);
		return first instanceof JSONObject && ((JSONObject) first).has("Name");
	}

	private static Map<String, JSONObject> byName(JSONArray list) {
		Map<String, JSONObject> m = new LinkedHashMap<String, JSONObject>();
		for (int i = 0; i < list.length(); i++) {
			JSONObject o = list.optJSONObject(i);
			if (o == null) continue;
			String name = o.optString("Name", "#" + (i + 1)), key = name;
			for (int n = 2; m.containsKey(key); n++) key = name + "#" + n;
			m.put(key, o);
		}
		return m;
	}

	private static boolean same(Object x, Object y) {
		if (x == null || y == null) return x == y;
		if (x instanceof JSONObject || x instanceof JSONArray) return x.toString().equals(y.toString());
		if (x instanceof Number && y instanceof Number) return ((Number) x).doubleValue() == ((Number) y).doubleValue();
		return x.equals(y);
	}

	/** short values as they are, objects and lists as text */
	private static Object plain(Object o) {
		if (o == null || o == JSONObject.NULL) return null;
		if (o instanceof JSONObject || o instanceof JSONArray) {
			String s = o.toString();
			return s.length() > 200 ? s.substring(0, 200) + "..." : s;
		}
		return o;
	}
}
