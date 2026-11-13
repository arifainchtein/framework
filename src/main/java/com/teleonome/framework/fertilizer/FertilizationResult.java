package com.teleonome.framework.fertilizer;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * What FertilizerEngine.fertilize gives back (2026-11-13). Nothing in it has been written anywhere: the caller
 * decides (the Fertilizer command line writes files, the Hypothalamus reloads its Denome, the factory shows it).
 */
public class FertilizationResult {

	/** true when the Denome was made and passed the check */
	public boolean ok = false;
	/** why it failed, null when ok */
	public String failure = null;
	/** the fertilized Denome (the whole pulse object, {"Denome": ...}); also set when the check failed, to look at */
	public JSONObject denome = null;
	/** the validation problems the sperm brought (all of them, for Create) */
	public JSONArray introducedProblems = new JSONArray();
	/** Mutate: how many problems the Denome already had before the sperm */
	public int problemsBefore = 0;
	/** what happened, in order, for a person to read */
	public List<String> messages = new ArrayList<String>();
	/** every difference between the Denome given and the one made */
	public List<DenomeDiff.Change> changes = new ArrayList<DenomeDiff.Change>();

	void say(String message) { messages.add(message); }

	public JSONObject toJSON() {
		JSONObject o = new JSONObject();
		o.put("OK", ok);
		o.put("Failure", failure == null ? JSONObject.NULL : failure);
		o.put("Introduced Problems", introducedProblems);
		o.put("Problems Before", problemsBefore);
		o.put("Messages", new JSONArray(messages));
		JSONArray c = new JSONArray();
		for (DenomeDiff.Change change : changes) c.put(change.toJSON());
		o.put("Changes", c);
		return o;
	}
}
