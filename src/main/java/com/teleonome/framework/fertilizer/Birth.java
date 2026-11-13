package com.teleonome.framework.fertilizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Birth (2026-11-13, Avocado design §6): the Egg gets its Teleonome's name, before any sperm is made against it.
 * Every whole word "Egg" in any text attribute becomes the new name:
 *   - the Denome's own Name;
 *   - pointers, @Egg:Internal:... becomes @Factory:Internal:..., and the Egg-named elements inside them
 *     (@Egg:Purpose:Analysis:Egg Disk Pie Chart Data becomes @Factory:Purpose:Analysis:Factory Disk Pie Chart Data);
 *   - the denes, actions and words named after it (Egg Database Pie Chart Data, Generate Egg Disk Available Chart
 *     Action, Remember Egg-MainBatteryVoltage...).
 *   - pointers in every other attribute too: Target, Injection Target, Deletion Target (the mutations'), and the
 *     Denome's Host Name.
 * Every text attribute is looked at, never the keys. "Whole word" means Eggplant would not change. The Egg given is
 * not changed (it works on a copy).
 */
public class Birth {

	public static class Result {
		public boolean ok = false;
		public String failure = null;
		/** the named Denome, the whole pulse object {"Denome": ...} */
		public JSONObject denome = null;
		/** every text changed, with its attribute: "Name: Egg Disk Pie Chart Data -> Factory Disk Pie Chart Data" */
		public List<String> renamed = new ArrayList<String>();
		public int pointersRenamed = 0, namesRenamed = 0, valuesRenamed = 0;
	}

	private static final Pattern NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,30}$");

	/** null when the name can be a Teleonome's name, else why not */
	public static String nameProblem(String name) {
		if (name == null || !NAME.matcher(name).matches()) return "A Teleonome's name starts with a letter and has only letters, digits and _ (2 to 31 characters)";
		if (name.equalsIgnoreCase("Egg")) return "Pick a name other than Egg";
		return null;
	}

	public static Result name(JSONObject egg, String newName) {
		Result result = new Result();
		String problem = nameProblem(newName);
		if (problem != null) { result.failure = problem; return result; }
		try {
			JSONObject pulse = new JSONObject(egg.toString());
			JSONObject denome = pulse.getJSONObject("Denome");
			String oldName = denome.getString("Name");
			if (!oldName.equals("Egg")) {
				result.failure = "This is " + oldName + ", not the Egg: only the Egg is named at birth";
				return result;
			}
			Pattern word = Pattern.compile("\\bEgg\\b");
			walk(pulse, word, newName, "", result);
			result.denome = pulse;
			result.ok = true;
		} catch (JSONException e) {
			result.failure = "The Egg is not a Denome: " + e.getMessage();
		}
		return result;
	}

	private static void walk(Object node, Pattern word, String newName, String where, Result result) {
		if (node instanceof JSONObject) {
			JSONObject o = (JSONObject) node;
			for (String attribute : new ArrayList<String>(o.keySet())) {
				Object v = o.opt(attribute);
				if (!(v instanceof String)) continue;
				String before = (String) v;
				Matcher m = word.matcher(before);
				if (!m.find()) continue;
				String after = m.replaceAll(Matcher.quoteReplacement(newName));
				o.put(attribute, after);
				if (attribute.equals("Name")) result.namesRenamed++;
				else if (before.startsWith("@")) result.pointersRenamed++;
				else result.valuesRenamed++;
				result.renamed.add(attribute + ": " + before + " -> " + after);
			}
			Iterator<String> keys = o.keys();
			while (keys.hasNext()) {
				Object child = o.get(keys.next());
				if (child instanceof JSONObject || child instanceof JSONArray) walk(child, word, newName, where, result);
			}
		} else if (node instanceof JSONArray) {
			JSONArray a = (JSONArray) node;
			for (int i = 0; i < a.length(); i++) {
				Object v = a.get(i);
				if (v instanceof String && word.matcher((String) v).find()) {
					String after = word.matcher((String) v).replaceAll(Matcher.quoteReplacement(newName));
					a.put(i, after);
					if (((String) v).startsWith("@")) result.pointersRenamed++; else result.valuesRenamed++;
					result.renamed.add("List item: " + v + " -> " + after);
				} else {
					walk(v, word, newName, where, result);
				}
			}
		}
	}
}
