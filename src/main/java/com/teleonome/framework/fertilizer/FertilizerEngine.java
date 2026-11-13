package com.teleonome.framework.fertilizer;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.DenomeValidator;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;

/**
 * Fertilizer as a class (2026-11-13, Avocado design §6): injects a sperm into a Denome and gives back the result.
 * The same steps as the Fertilizer command line, which now calls this, but:
 *   - no files, no working folder, no System.exit, no killing of a Hypothalamus: the caller does what it needs
 *     (the command line writes files and can undo; the Hypothalamus reloads its own Denome between pulses; the
 *     factory's Incubator shows the result);
 *   - the Denome and the sperm given are not touched (it works on copies);
 *   - a sperm problem (a homeobox index pointing at nothing, a hox dene without Target) is a failed result,
 *     not an exit.
 *
 * Purpose Type Mutate: the sperm's @Egg: pointers are renamed to the living Denome's name, and only the validation
 * problems the sperm brought refuse it (a living Denome has problems of its own). Purpose Type Create: the Denome
 * takes the sperm's Name and Description and @NewEgg: pointers are renamed (as the command line did), every
 * problem refuses it.
 */
public class FertilizerEngine {

	private static final Logger logger = Logger.getLogger(FertilizerEngine.class);

	/**
	 * @param denome the whole pulse object ({"Denome": ...}), the Egg or a living Denome
	 * @param sperm  the whole sperm ({"Sperm": ...})
	 */
	public static FertilizationResult fertilize(JSONObject denome, JSONObject sperm) {
		FertilizationResult result = new FertilizationResult();
		String denomeInString = denome.toString();
		try {
			JSONObject pulseJSONObject = new JSONObject(denomeInString);
			JSONObject completeSpermJSONObject = new JSONObject(sperm.toString());

			JSONObject spermJSONObject = completeSpermJSONObject.getJSONObject(TeleonomeConstants.SPERM);
			JSONObject purposeJSONObject = spermJSONObject.getJSONObject(TeleonomeConstants.SPERM_PURPOSE);
			String purposeType = purposeJSONObject.getString(TeleonomeConstants.SPERM_PURPOSE_TYPE);
			boolean create = purposeType.equals(TeleonomeConstants.SPERM_PURPOSE_TYPE_CREATE);
			JSONObject denomeJSONObject = pulseJSONObject.getJSONObject("Denome");

			if (create) {
				String newTeleonomeName = purposeJSONObject.getString(TeleonomeConstants.SPERM_PURPOSE_NAME);
				String newTeleonomeDescription = purposeJSONObject.getString(TeleonomeConstants.SPERM_PURPOSE_DESCRIPTION);
				denomeJSONObject.put("Name", newTeleonomeName);
				denomeJSONObject.put("Description", newTeleonomeDescription);
				pulseJSONObject = new JSONObject(pulseJSONObject.toString().replace("@NewEgg:", "@" + newTeleonomeName + ":"));
				result.say("Creating " + newTeleonomeName);
			} else {
				String livingName = denomeJSONObject.getString("Name");
				if (!livingName.equals("Egg")) {
					completeSpermJSONObject = new JSONObject(completeSpermJSONObject.toString().replace("@Egg:", "@" + livingName + ":"));
					spermJSONObject = completeSpermJSONObject.getJSONObject(TeleonomeConstants.SPERM);
					result.say("Mutating " + livingName + ": the sperm's @Egg: pointers now name " + livingName);
				} else {
					result.say("Mutating Egg");
				}
			}

			JSONObject hypothalamusJSONObject = spermJSONObject.getJSONObject(TeleonomeConstants.SPERM_HYPOTHALAMUS);
			JSONArray homeboxesJSONArray = hypothalamusJSONObject.getJSONArray(TeleonomeConstants.SPERM_HYPOTHALAMUS_HOMEOBOXES);
			JSONArray actionsJSONArray = hypothalamusJSONObject.getJSONArray(TeleonomeConstants.SPERM_HYPOTHALAMUS_ACTIONS);
			JSONArray mutationsJSONArray = hypothalamusJSONObject.optJSONArray(TeleonomeConstants.SPERM_HYPOTHALAMUS_MUTATIONS);

			//
			// the actions, sorted by Execution Position, before and after the homeoboxes
			//
			ArrayList<Map.Entry<JSONObject, Integer>> preHomebox = new ArrayList<Map.Entry<JSONObject, Integer>>();
			ArrayList<Map.Entry<JSONObject, Integer>> postHomebox = new ArrayList<Map.Entry<JSONObject, Integer>>();
			for (int i = 0; i < actionsJSONArray.length(); i++) {
				JSONObject action = actionsJSONArray.getJSONObject(i);
				String executionPoint = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.SPERM_ACTION_DENEWORD_EXECUTION_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (executionPoint == null) continue;
				Integer executionPosition = (Integer) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.SPERM_ACTION_DENEWORD_EXECUTION_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if (executionPoint.equals(TeleonomeConstants.SPERM_ACTION_DENEWORD_EXECUTION_POINT_PRE_HOMEBOX)) {
					preHomebox.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(action, executionPosition));
				} else if (executionPoint.equals(TeleonomeConstants.SPERM_ACTION_DENEWORD_EXECUTION_POINT_POST_HOMEBOX)) {
					postHomebox.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(action, executionPosition));
				}
			}
			Collections.sort(preHomebox, (x, y) -> x.getValue().compareTo(y.getValue()));
			Collections.sort(postHomebox, (x, y) -> x.getValue().compareTo(y.getValue()));

			for (Map.Entry<JSONObject, Integer> entry : preHomebox) {
				preHomeboxAction(pulseJSONObject, entry.getKey(), result);
			}

			//
			// the homeoboxes: first every hox dene of every index (so the denes exist), then removers and carriers
			//
			for (int i = 0; i < homeboxesJSONArray.length(); i++) {
				JSONObject homeobox = homeboxesJSONArray.getJSONObject(i);
				JSONArray homeoboxDenes = homeobox.getJSONArray(TeleonomeConstants.SPERM_HOMEOBOX_DENES);
				for (int j = 0; j < homeoboxDenes.length(); j++) {
					JSONObject homeoboxDene = homeoboxDenes.getJSONObject(j);
					if (!homeoboxDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(TeleonomeConstants.SPERM_HOMEOBOX_INDEX)) continue;
					JSONArray indexDeneWords = homeoboxDene.getJSONArray("DeneWords");
					for (int k = 0; k < indexDeneWords.length(); k++) {
						JSONObject indexDeneWord = indexDeneWords.getJSONObject(k);
						if (!indexDeneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE).equals(TeleonomeConstants.SPERM_HOX_DENE_POINTER)) continue;
						String denePointer = indexDeneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						JSONObject hoxDene = getDeneBySpermIdentity(completeSpermJSONObject, new FertilizationIdentity(denePointer));
						if (hoxDene == null) {
							return failed(result, "The sperm is misconfigured: the index of homeobox " + homeobox.getString("Name") + " points to " + denePointer + ", which is not in the homeobox");
						}
						if (!hoxDene.has(TeleonomeConstants.SPERM_HOX_DENE_TARGET)) {
							return failed(result, "The sperm is misconfigured: hox dene " + hoxDene.getString("Name") + " (" + denePointer + ") has no Target");
						}
						insertHoxDene(pulseJSONObject, hoxDene, result);
					}
				}
				for (int j = 0; j < homeoboxDenes.length(); j++) {
					JSONObject homeoboxDene = homeoboxDenes.getJSONObject(j);
					String deneType = homeoboxDene.optString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE, "");
					if (deneType.equals(TeleonomeConstants.SPERM_DENE_TYPE_DENEWORD_REMOVER)) {
						JSONArray removerDeneWords = homeoboxDene.getJSONArray("DeneWords");
						for (int k = 0; k < removerDeneWords.length(); k++) {
							Identity target = new Identity(removerDeneWords.getJSONObject(k).getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
							boolean removed = DenomeUtils.removeDeneWordFromDeneByIdentity(pulseJSONObject, target);
							result.say((removed ? "Removed " : "Did not find, to remove: ") + target);
						}
					} else if (deneType.equals(TeleonomeConstants.SPERM_DENE_TYPE_DENEWORD_CARRIER)) {
						carryDeneWords(pulseJSONObject, homeoboxDene, result);
					}
				}
			}

			//
			// the actions after the homeoboxes: DeneWord updates
			//
			for (Map.Entry<JSONObject, Integer> entry : postHomebox) {
				JSONObject action = entry.getKey();
				if (!action.optString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE, "").equals(TeleonomeConstants.SPERM_DENE_TYPE_UPDATE_DENEWORD_VALUE)) continue;
				String updateListPointer = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.SPERM_ACTION_DENWORD_UPDATE_VALUE_LIST, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject updateList = getDeneBySpermIdentity(completeSpermJSONObject, new FertilizationIdentity(updateListPointer));
				if (updateList == null) {
					return failed(result, "The sperm is misconfigured: action " + action.optString("Name") + " points to the update list " + updateListPointer + ", which is not in the sperm");
				}
				JSONArray updates = updateList.getJSONArray("DeneWords");
				for (int i = 0; i < updates.length(); i++) {
					JSONObject update = updates.getJSONObject(i);
					String pointer = update.getString(TeleonomeConstants.SPERM_ACTION_DENE_TARGET);
					Object value = update.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if (!DenomeUtils.containsDenomicElementByIdentity(pulseJSONObject, new Identity(pointer))) {
						return failed(result, "Update DeneWord: " + pointer + " is not in the Denome");
					}
					pulseJSONObject = DenomeUtils.updateDeneWordByIdentity(pulseJSONObject, pointer, value);
					result.say("Updated " + pointer + " to " + value);
				}
			}

			//
			// the mutations, added whole
			//
			if (mutationsJSONArray != null) {
				for (int i = 0; i < mutationsJSONArray.length(); i++) {
					JSONObject mutation = mutationsJSONArray.getJSONObject(i);
					if (!mutation.has(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE)) continue;
					if (!DenomeUtils.containsMutation(pulseJSONObject, mutation.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE))) {
						DenomeUtils.addMutationToMutations(pulseJSONObject, mutation);
						result.say("Added mutation " + mutation.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
					}
				}
			}

			result.denome = pulseJSONObject;
			result.changes = DenomeDiff.compare(denome, pulseJSONObject);

			//
			// the check
			//
			JSONArray problems = DenomeValidator.validate(pulseJSONObject.toString());
			if (!create) {
				Set<String> before = new HashSet<String>();
				JSONArray beforeProblems = DenomeValidator.validate(denomeInString);
				for (int i = 0; i < beforeProblems.length(); i++) before.add(validationKey(beforeProblems.getJSONObject(i)));
				JSONArray introduced = new JSONArray();
				for (int i = 0; i < problems.length(); i++) {
					if (!before.contains(validationKey(problems.getJSONObject(i)))) introduced.put(problems.getJSONObject(i));
				}
				result.problemsBefore = beforeProblems.length();
				problems = introduced;
				if (beforeProblems.length() > 0) result.say("The Denome being mutated already had " + beforeProblems.length() + " validation problems; only the " + introduced.length() + " this sperm brought stop the fertilization");
			}
			result.introducedProblems = problems;
			if (problems.length() > 0) {
				return failed(result, "The fertilization produced a malformed Denome: " + problems.length() + " validation problems");
			}
			result.ok = true;
			result.say("Fertilization complete: " + result.changes.size() + " changes");
			return result;
		} catch (JSONException | InvalidDenomeException | MissingDenomeException | ClassCastException e) {
			logger.warn("fertilization failed", e);
			return failed(result, e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private static FertilizationResult failed(FertilizationResult result, String why) {
		result.ok = false;
		result.failure = why;
		result.say(why);
		return result;
	}

	// a validation problem, to tell the ones a mutation brought from the ones the Denome had
	private static String validationKey(JSONObject problem) {
		return problem.optString("Error Title") + "|" + problem.optString("Problem Identity");
	}

	/** Create Dene Chain, Create Mutation */
	private static void preHomeboxAction(JSONObject pulseJSONObject, JSONObject action, FertilizationResult result) throws JSONException, InvalidDenomeException {
		String type = action.optString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE, "");
		String targetPointer = action.getString(TeleonomeConstants.SPERM_ACTION_DENE_TARGET);
		if (type.equals(TeleonomeConstants.SPERM_DENE_TYPE_CREATE_DENE_CHAIN)) {
			String chainName = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.SPERM_ACTION_DENEWORD_DENECHAIN_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if (DenomeUtils.containsDenomicElementByIdentity(pulseJSONObject, new Identity(targetPointer + ":" + chainName))) {
				result.say("Dene chain " + targetPointer + ":" + chainName + " already exists");
				return;
			}
			JSONObject chain = new JSONObject();
			chain.put(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE, chainName);
			chain.put("Denes", new JSONArray());
			DenomeUtils.addDeneChainToNucleusByIdentity(pulseJSONObject, chain, new Identity(targetPointer));
			result.say("Created dene chain " + targetPointer + ":" + chainName);
		} else if (type.equals(TeleonomeConstants.SPERM_DENE_TYPE_CREATE_MUTATION)) {
			String mutationName = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.SPERM_ACTION_DENEWORD_MUTATION_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if (DenomeUtils.containsMutation(pulseJSONObject, mutationName)) {
				result.say("Mutation " + mutationName + " already exists");
				return;
			}
			JSONObject mutation = new JSONObject();
			mutation.put("Name", mutationName);
			mutation.put(TeleonomeConstants.DENEWORD_ACTIVE, true);
			mutation.put("Execution Mode", (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, "Execution Mode", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
			mutation.put(TeleonomeConstants.MUTATION_TYPE_ATTRIBUTE, (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.MUTATION_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
			// optional: "Time" for a mutation the Hypothalamus runs on a schedule; left out, it is run by command
			Object invocationMode = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(action, TeleonomeConstants.MUTATION_INVOCATION_MODE_ATTRIBUTE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if (invocationMode != null && !invocationMode.toString().equals("")) {
				mutation.put(TeleonomeConstants.MUTATION_INVOCATION_MODE_ATTRIBUTE, invocationMode.toString());
			}
			JSONArray chains = new JSONArray();
			mutation.put("DeneChains", chains);
			for (String name : new String[] { TeleonomeConstants.DENECHAIN_ON_LOAD_MUTATION, TeleonomeConstants.DENECHAIN_ACTIONS_TO_EXECUTE,
					TeleonomeConstants.DENECHAIN_MNEMOSYCONS_TO_EXECUTE, TeleonomeConstants.MUTATION_PROCESSING_LOGIC_DENE_CHAIN_NAME,
					TeleonomeConstants.DENECHAIN_MNEMOSYNE_OPERATIONS, "Mutation Configuration" }) {
				chains.put(new JSONObject().put("Name", name).put("Denes", new JSONArray()));
			}
			JSONObject carrier = new JSONObject();
			carrier.put(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE, TeleonomeConstants.DENE_TYPE_DENEWORD_CARRIER);
			carrier.put("DeneWords", new JSONArray());
			carrier.put(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE, TeleonomeConstants.DENE_TYPE_DENEWORD_CARRIER);
			chains.put(new JSONObject().put("Name", TeleonomeConstants.DENECHAIN_DENEWORD_INJECTION).put("Denes", new JSONArray().put(carrier)));
			chains.put(new JSONObject().put("Name", TeleonomeConstants.DENECHAIN_DENE_INJECTION).put("Denes", new JSONArray()));
			chains.put(new JSONObject().put("Name", TeleonomeConstants.DENECHAIN_ON_FINISH_MUTATION).put("Denes", new JSONArray()));
			DenomeUtils.addMutationToMutations(pulseJSONObject, mutation);
			result.say("Created mutation " + mutationName);
		}
	}

	/** a hox dene goes to its Target: a dene chain, or a mutation's dene chain */
	private static void insertHoxDene(JSONObject pulseJSONObject, JSONObject hoxDene, FertilizationResult result) throws JSONException, InvalidDenomeException {
		String targetPointer = hoxDene.getString(TeleonomeConstants.SPERM_HOX_DENE_TARGET);
		Identity target = new Identity(targetPointer);
		hoxDene.remove(TeleonomeConstants.SPERM_HOX_DENE_TARGET);
		String deneName = hoxDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
		if (DenomeUtils.isMutationIdentity(targetPointer)) {
			// the mutation's name is the second token, where a nucleus would be
			String mutationName = target.getNucleusName();
			if (DenomeUtils.containsMutation(pulseJSONObject, mutationName)) {
				DenomeUtils.addDeneToMutationDeneChainByIdentity(pulseJSONObject, hoxDene, target);
				result.say("Added " + targetPointer + ":" + deneName);
			} else {
				result.say("Did not add " + targetPointer + ":" + deneName + ": the mutation does not exist");
			}
		} else if (DenomeUtils.containsDenomicElementByIdentity(pulseJSONObject, new Identity(targetPointer + ":" + deneName))) {
			result.say("Did not add " + targetPointer + ":" + deneName + ": it already exists");
		} else {
			DenomeUtils.addDeneToDeneChainByIdentity(pulseJSONObject, hoxDene, target);
			result.say("Added " + targetPointer + ":" + deneName);
		}
	}

	/** DeneWord Carrier: DeneWords into an existing dene (made empty first if it is missing) */
	private static void carryDeneWords(JSONObject pulseJSONObject, JSONObject carrierDene, FertilizationResult result) throws JSONException, InvalidDenomeException {
		String targetPointer = carrierDene.getString(TeleonomeConstants.SPERM_HOX_DENE_TARGET);
		Identity target = new Identity(targetPointer);
		JSONArray deneWords = carrierDene.getJSONArray("DeneWords");
		for (int k = 0; k < deneWords.length(); k++) {
			JSONObject deneWord = deneWords.getJSONObject(k);
			if (!DenomeUtils.containsDenomicElementByIdentity(pulseJSONObject, target)) {
				JSONObject newDene = new JSONObject();
				newDene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, target.getDeneName());
				newDene.put("DeneWords", new JSONArray());
				DenomeUtils.addDeneToDeneChainByIdentity(pulseJSONObject, newDene, target.getNucleusName(), target.getDenechainName());
				result.say("Added " + targetPointer + " for the DeneWords it carries");
			}
			String wordPointer = targetPointer + ":" + deneWord.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
			if (DenomeUtils.containsDenomicElementByIdentity(pulseJSONObject, new Identity(wordPointer))) {
				result.say("Did not add " + wordPointer + ": it already exists");
			} else {
				DenomeUtils.addDeneWordToDeneByIdentity(pulseJSONObject, deneWord, target);
				result.say("Added " + wordPointer);
			}
		}
	}

	/** a dene (or action) in the sperm by its pointer, @Sperm:Hypothalamus:<homeobox or action>[:<dene>] */
	public static JSONObject getDeneBySpermIdentity(JSONObject completeSpermJSONObject, FertilizationIdentity identity) throws JSONException {
		JSONObject hypothalamus = completeSpermJSONObject.getJSONObject(TeleonomeConstants.SPERM).getJSONObject(TeleonomeConstants.SPERM_HYPOTHALAMUS);
		JSONArray actions = hypothalamus.getJSONArray(TeleonomeConstants.SPERM_HYPOTHALAMUS_ACTIONS);
		for (int i = 0; i < actions.length(); i++) {
			if (actions.getJSONObject(i).getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(identity.getHomeoBoxName())) return actions.getJSONObject(i);
		}
		JSONArray homeoboxes = hypothalamus.getJSONArray(TeleonomeConstants.SPERM_HYPOTHALAMUS_HOMEOBOXES);
		for (int i = 0; i < homeoboxes.length(); i++) {
			JSONObject homeobox = homeoboxes.getJSONObject(i);
			if (!homeobox.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(identity.getHomeoBoxName())) continue;
			JSONArray denes = homeobox.getJSONArray(TeleonomeConstants.SPERM_HOMEOBOX_DENES);
			for (int j = 0; j < denes.length(); j++) {
				if (denes.getJSONObject(j).getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(identity.getHomeBoxDeneName())) return denes.getJSONObject(j);
			}
		}
		return null;
	}
}
