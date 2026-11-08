package com.teleonome.framework.denome;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.exception.InvalidMutation;

/**
 * Structural payloads for State Mutations (2026-11-08): a payload that carries whole Denes and DeneChains to
 * inject, and pointers to the Denes and DeneChains to delete, so that a person can make, change and remove
 * Human Interface pages from the web page (the tabs on the Home page, "Telepathon Values Panel").
 *
 * The payload (TeleonomeConstants, next to "Updates"):
 *   "DeneChain Deletions":  ["@T:Human Interface:Greenhouse Page", ...]
 *   "Dene Deletions":       ["@T:Human Interface:Human Interface Control Parameters:Greenhouse", ...]
 *   "Dene Injections":      [{"Injection Target": "@T:Human Interface:Human Interface Control Parameters", "Dene": {...}}]
 *   "DeneChain Injections": [{"Injection Target": "@T:Human Interface", "DeneChain": {...}}]
 *
 * Rules, all checked before anything changes (one bad element and the whole payload is refused):
 *  - the mutation must say where it may work: a DeneWord "Allowed Payload Nucleus" in its "Mutation Configuration"
 *    chain (e.g. Human Interface); every target and every deletion must be in that nucleus of this Teleonome;
 *  - everything injected is marked "Injected By Payload": true (the DeneChain and each of its Denes), and only
 *    elements so marked can be deleted, so a payload can never remove what the Denome was born with
 *    (Home Page, Settings, Control Parameters...);
 *  - a name may not hold ':' (it would break pointers) or start with '@' or '$'; a new Dene or DeneChain may not
 *    take the name of one that is there, unless the same payload deletes it (that is how a page is changed);
 *  - Denes and DeneWords keep only the attributes a Denome uses (Name, Value, Value Type, DeneWord Type,
 *    Required, Units, Default; Name, Dene Type, DeneWords), within size limits.
 *
 * apply() runs where the payload is injected into the mutation (DenomeManager.injectPayloadIntoStateMutation)
 * and returns a copy of the mutation carrying the work, so the mutation stored in the Denome stays as it was.
 * The work itself is done in loadImmediateMutation: deletions after its Dene Deletion step, injections after
 * its Dene Injections step.
 */
public class StructuralPayload {

	private static final Logger logger = Logger.getLogger(StructuralPayload.class);

	private static final int MAX_NAME = 100;
	private static final int MAX_DENES = 200;
	private static final int MAX_DENEWORDS = 60;
	private static final int MAX_STRING_VALUE = 2000;
	private static final int MAX_ELEMENTS = 50;

	private static final Set<String> DENE_KEYS = set("Name", "DeneWords", "Dene Type");
	private static final Set<String> DENEWORD_KEYS = set("Name", "Value", "Value Type", "DeneWord Type", "Required", "Units", "Default");
	private static final Set<String> VALUE_TYPES = set(TeleonomeConstants.DATATYPE_STRING, TeleonomeConstants.DATATYPE_INTEGER,
			TeleonomeConstants.DATATYPE_DOUBLE, TeleonomeConstants.DATATYPE_LONG, TeleonomeConstants.DATATYPE_BOOLEAN,
			TeleonomeConstants.DATATYPE_DENE_POINTER);

	private static Set<String> set(String... values) {
		Set<String> s = new HashSet<String>();
		for(String v : values) s.add(v);
		return s;
	}

	/** Whether the payload carries any structural work. */
	public static boolean isStructural(JSONObject payload) {
		return payload!=null && (payload.has(TeleonomeConstants.MUTATION_PAYLOAD_DENE_INJECTIONS)
				|| payload.has(TeleonomeConstants.MUTATION_PAYLOAD_DENECHAIN_INJECTIONS)
				|| payload.has(TeleonomeConstants.MUTATION_PAYLOAD_DENE_DELETIONS)
				|| payload.has(TeleonomeConstants.MUTATION_PAYLOAD_DENECHAIN_DELETIONS));
	}

	/**
	 * A copy of the mutation with the payload's structural work in it (chains "DeneChain Deletion",
	 * "Dene Deletion", "Dene Injections", "DeneChain Injections"), or the mutation itself when the payload has
	 * none. Throws InvalidMutation, changing nothing, when the payload breaks a rule.
	 */
	public static JSONObject apply(DenomeManager denomeManager, JSONObject mutation, JSONObject payload) throws InvalidMutation {
		if(mutation==null || !isStructural(payload)) return mutation;
		String mutationName = mutation.optString("Name", "");
		String teleonomeName = denomeManager.getDenomeName();
		String nucleus = allowedNucleus(mutation);
		if(nucleus==null) refuse(mutationName, "this mutation does not take Denes or DeneChains in its payload (no \""
				+ TeleonomeConstants.DENEWORD_ALLOWED_PAYLOAD_NUCLEUS + "\" in its " + TeleonomeConstants.DENECHAIN_MUTATION_CONFIGURATION + ")");
		try {
			JSONArray chainDeletions = payload.optJSONArray(TeleonomeConstants.MUTATION_PAYLOAD_DENECHAIN_DELETIONS);
			JSONArray deneDeletions = payload.optJSONArray(TeleonomeConstants.MUTATION_PAYLOAD_DENE_DELETIONS);
			JSONArray deneInjections = payload.optJSONArray(TeleonomeConstants.MUTATION_PAYLOAD_DENE_INJECTIONS);
			JSONArray chainInjections = payload.optJSONArray(TeleonomeConstants.MUTATION_PAYLOAD_DENECHAIN_INJECTIONS);
			int total = length(chainDeletions) + length(deneDeletions) + length(deneInjections) + length(chainInjections);
			if(total > MAX_ELEMENTS) refuse(mutationName, "too many elements (" + total + ")");

			// deletions: in the nucleus, there now, and injected by a payload before
			Set<String> deletedChains = new HashSet<String>();
			Set<String> deletedDenes = new HashSet<String>();   // "chain|dene"
			JSONArray chainDeletionDenes = new JSONArray();
			for(int i=0;i<length(chainDeletions);i++) {
				Identity identity = identity(chainDeletions.getString(i), teleonomeName, nucleus, 3, mutationName);
				JSONObject chain = denomeManager.getDenomicElementByIdentity(identity);
				if(chain==null) refuse(mutationName, "no DeneChain " + identity.toString() + " to delete");
				if(!chain.optBoolean(TeleonomeConstants.INJECTED_BY_PAYLOAD_ATTRIBUTE, false))
					refuse(mutationName, identity.toString() + " was not made by a payload, so a payload may not delete it");
				deletedChains.add(identity.getDenechainName());
				chainDeletionDenes.put(new JSONObject().put("Name", "Delete " + identity.getDenechainName())
						.put(TeleonomeConstants.MUTATION_DELETION_TARGET, identity.toString()));
			}
			JSONArray deneDeletionDenes = new JSONArray();
			for(int i=0;i<length(deneDeletions);i++) {
				Identity identity = identity(deneDeletions.getString(i), teleonomeName, nucleus, 4, mutationName);
				JSONObject dene = denomeManager.getDenomicElementByIdentity(identity);
				if(dene==null) refuse(mutationName, "no Dene " + identity.toString() + " to delete");
				if(!dene.optBoolean(TeleonomeConstants.INJECTED_BY_PAYLOAD_ATTRIBUTE, false))
					refuse(mutationName, identity.toString() + " was not made by a payload, so a payload may not delete it");
				deletedDenes.add(identity.getDenechainName() + "|" + identity.getDeneName());
				deneDeletionDenes.put(new JSONObject().put("Name", "Delete " + identity.getDeneName())
						.put(TeleonomeConstants.MUTATION_DELETION_TARGET, identity.toString()));
			}

			// DeneChain injections: into the nucleus itself, under a name that is free (or freed by this payload)
			JSONArray newChains = new JSONArray();
			Set<String> injectedChains = new HashSet<String>();
			for(int i=0;i<length(chainInjections);i++) {
				JSONObject injection = chainInjections.getJSONObject(i);
				Identity target = identity(injection.getString(TeleonomeConstants.MUTATION_INJECTION_TARGET), teleonomeName, nucleus, 2, mutationName);
				JSONObject chain = cleanChain(injection.getJSONObject(TeleonomeConstants.MUTATION_PAYLOAD_DENECHAIN), mutationName);
				String chainName = chain.getString("Name");
				if(!injectedChains.add(chainName)) refuse(mutationName, "the DeneChain " + chainName + " is injected twice");
				Identity chainIdentity = new Identity(teleonomeName, nucleus, chainName);
				if(denomeManager.getDenomicElementByIdentity(chainIdentity)!=null && !deletedChains.contains(chainName))
					refuse(mutationName, "there is already a DeneChain " + chainIdentity.toString());
				chain.put(TeleonomeConstants.MUTATION_INJECTION_TARGET, target.toString());
				newChains.put(chain);
			}

			// Dene injections: into a DeneChain of the nucleus that is there or comes with this payload
			JSONArray newDenes = new JSONArray();
			Set<String> injectedDenes = new HashSet<String>();
			for(int i=0;i<length(deneInjections);i++) {
				JSONObject injection = deneInjections.getJSONObject(i);
				Identity target = identity(injection.getString(TeleonomeConstants.MUTATION_INJECTION_TARGET), teleonomeName, nucleus, 3, mutationName);
				JSONObject dene = cleanDene(injection.getJSONObject(TeleonomeConstants.MUTATION_PAYLOAD_DENE), mutationName);
				String chainName = target.getDenechainName();
				JSONObject targetChain = denomeManager.getDenomicElementByIdentity(target);
				boolean chainComesNow = injectedChains.contains(chainName);
				if((targetChain==null || deletedChains.contains(chainName)) && !chainComesNow)
					refuse(mutationName, "no DeneChain " + target.toString() + " to put " + dene.getString("Name") + " in");
				if(chainComesNow) refuse(mutationName, "put " + dene.getString("Name") + " in the DeneChain " + chainName + " that this payload injects");
				String key = chainName + "|" + dene.getString("Name");
				if(!injectedDenes.add(key)) refuse(mutationName, "the Dene " + dene.getString("Name") + " is injected twice into " + chainName);
				if(hasDene(targetChain, dene.getString("Name")) && !deletedDenes.contains(key))
					refuse(mutationName, "there is already a Dene " + target.toString() + ":" + dene.getString("Name"));
				dene.put(TeleonomeConstants.MUTATION_INJECTION_TARGET, target.toString());
				newDenes.put(dene);
			}

			// the copy of the mutation that carries it
			JSONObject copy = new JSONObject(mutation.toString());
			JSONArray chains = copy.getJSONArray("DeneChains");
			appendDenes(chains, TeleonomeConstants.DENECHAIN_DENECHAIN_DELETION, chainDeletionDenes);
			appendDenes(chains, TeleonomeConstants.DENECHAIN_DENE_DELETION, deneDeletionDenes);
			appendDenes(chains, TeleonomeConstants.DENECHAIN_DENE_INJECTION, newDenes);
			if(newChains.length()>0) {
				JSONObject carrier = chainNamed(chains, TeleonomeConstants.DENECHAIN_DENECHAIN_INJECTION);
				if(carrier==null) {
					carrier = new JSONObject().put("Name", TeleonomeConstants.DENECHAIN_DENECHAIN_INJECTION).put("DeneChains", new JSONArray());
					chains.put(carrier);
				}
				if(!carrier.has("DeneChains")) carrier.put("DeneChains", new JSONArray());
				for(int i=0;i<newChains.length();i++) carrier.getJSONArray("DeneChains").put(newChains.get(i));
			}
			logger.info("structural payload for " + mutationName + ": delete " + length(chainDeletions) + " DeneChains and "
					+ length(deneDeletions) + " Denes, inject " + newDenes.length() + " Denes and " + newChains.length() + " DeneChains in " + nucleus);
			return copy;
		}catch(JSONException e) {
			refuse(mutationName, "the payload is not well formed: " + e.getMessage());
			return null;
		}
	}

	/** loadImmediateMutation, after its Dene Deletion step: removes the DeneChains in "DeneChain Deletion". */
	public static void deleteDeneChains(DenomeManager denomeManager, Hashtable nameMutationDeneChainIndex) {
		JSONObject deletions = (JSONObject) nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DENECHAIN_DELETION);
		if(deletions==null) return;
		JSONArray denes = deletions.optJSONArray("Denes");
		for(int i=0;i<length(denes);i++) {
			String pointer = denes.getJSONObject(i).getString(TeleonomeConstants.MUTATION_DELETION_TARGET);
			Identity identity = new Identity(pointer);
			JSONObject nucleus = denomeManager.getDenomicElementByIdentity(new Identity(identity.getTeleonomeName(), identity.getNucleusName()));
			JSONArray chains = nucleus==null ? null : nucleus.optJSONArray("DeneChains");
			boolean removed = false;
			for(int j=0;j<length(chains);j++) {
				JSONObject chain = chains.getJSONObject(j);
				if(chain.optString("Name").equals(identity.getDenechainName()) && chain.optBoolean(TeleonomeConstants.INJECTED_BY_PAYLOAD_ATTRIBUTE, false)) {
					chains.remove(j);
					removed = true;
					break;
				}
			}
			logger.info("DeneChain deletion " + pointer + (removed ? " done" : " not found"));
		}
	}

	/** loadImmediateMutation, after its Dene Injections step: adds the DeneChains in "DeneChain Injections". */
	public static void injectDeneChains(DenomeManager denomeManager, Hashtable nameMutationDeneChainIndex) {
		JSONObject injections = (JSONObject) nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DENECHAIN_INJECTION);
		if(injections==null) return;
		JSONArray newChains = injections.optJSONArray("DeneChains");
		for(int i=0;i<length(newChains);i++) {
			JSONObject chain = new JSONObject(newChains.getJSONObject(i).toString());
			String target = (String) chain.remove(TeleonomeConstants.MUTATION_INJECTION_TARGET);
			JSONObject nucleus = denomeManager.getDenomicElementByIdentity(new Identity(target));
			if(nucleus==null || !nucleus.has("DeneChains")) {
				logger.warn("DeneChain injection: no nucleus " + target + " for " + chain.optString("Name"));
				continue;
			}
			nucleus.getJSONArray("DeneChains").put(chain);
			logger.info("DeneChain injection " + target + ":" + chain.optString("Name") + " done");
		}
	}

	// ---- checks ----

	/** The nucleus the mutation's Mutation Configuration lets a payload change, or null. */
	private static String allowedNucleus(JSONObject mutation) {
		JSONArray chains = mutation.optJSONArray("DeneChains");
		JSONObject config = chainNamed(chains, TeleonomeConstants.DENECHAIN_MUTATION_CONFIGURATION);
		JSONArray denes = config==null ? null : config.optJSONArray("Denes");
		for(int i=0;i<length(denes);i++) {
			JSONArray words = denes.getJSONObject(i).optJSONArray("DeneWords");
			for(int j=0;j<length(words);j++) {
				JSONObject word = words.getJSONObject(j);
				if(TeleonomeConstants.DENEWORD_ALLOWED_PAYLOAD_NUCLEUS.equals(word.optString("Name"))) {
					String nucleus = word.optString("Value", "").trim();
					return nucleus.isEmpty() ? null : nucleus;
				}
			}
		}
		return null;
	}

	/** The pointer as an Identity of this Teleonome, in the allowed nucleus, with exactly that many parts. */
	private static Identity identity(String pointer, String teleonomeName, String nucleus, int parts, String mutationName) throws InvalidMutation {
		if(pointer==null || !pointer.startsWith("@")) refuse(mutationName, "not a pointer: " + pointer);
		String[] tokens = pointer.substring(1).split(":", -1);
		if(tokens.length!=parts) refuse(mutationName, pointer + " should have " + parts + " parts");
		for(String t : tokens) if(t.trim().isEmpty()) refuse(mutationName, pointer + " has an empty part");
		if(!tokens[0].equals(teleonomeName)) refuse(mutationName, pointer + " is not in " + teleonomeName);
		if(!tokens[1].equals(nucleus)) refuse(mutationName, pointer + " is outside " + nucleus + ", the only nucleus this mutation may change");
		return new Identity(pointer);
	}

	private static void checkName(String name, String what, String mutationName) throws InvalidMutation {
		if(name==null || name.trim().isEmpty()) refuse(mutationName, what + " without a name");
		if(name.length()>MAX_NAME || name.indexOf(':')>=0 || name.startsWith("@") || name.startsWith("$") || !name.equals(name.trim()))
			refuse(mutationName, what + " name not allowed: " + name);
	}

	private static JSONObject cleanChain(JSONObject chain, String mutationName) throws InvalidMutation {
		String name = chain.optString("Name", null);
		checkName(name, "DeneChain", mutationName);
		JSONArray denes = chain.optJSONArray("Denes");
		if(denes==null) refuse(mutationName, "the DeneChain " + name + " has no Denes");
		if(denes.length()>MAX_DENES) refuse(mutationName, "the DeneChain " + name + " has too many Denes");
		JSONArray clean = new JSONArray();
		Set<String> names = new HashSet<String>();
		for(int i=0;i<denes.length();i++) {
			JSONObject dene = cleanDene(denes.getJSONObject(i), mutationName);
			if(!names.add(dene.getString("Name"))) refuse(mutationName, "two Denes named " + dene.getString("Name") + " in " + name);
			clean.put(dene);
		}
		return new JSONObject().put("Name", name).put("Denes", clean).put(TeleonomeConstants.INJECTED_BY_PAYLOAD_ATTRIBUTE, true);
	}

	private static JSONObject cleanDene(JSONObject dene, String mutationName) throws InvalidMutation {
		String name = dene.optString("Name", null);
		checkName(name, "Dene", mutationName);
		for(Iterator<String> it=dene.keys(); it.hasNext();) {
			String key = it.next();
			if(!DENE_KEYS.contains(key) && !key.equals(TeleonomeConstants.INJECTED_BY_PAYLOAD_ATTRIBUTE))
				refuse(mutationName, "the Dene " + name + " has an attribute a payload may not set: " + key);
		}
		JSONArray words = dene.optJSONArray("DeneWords");
		if(words==null) refuse(mutationName, "the Dene " + name + " has no DeneWords");
		if(words.length()>MAX_DENEWORDS) refuse(mutationName, "the Dene " + name + " has too many DeneWords");
		JSONArray clean = new JSONArray();
		Set<String> names = new HashSet<String>();
		for(int i=0;i<words.length();i++) {
			JSONObject word = words.getJSONObject(i);
			String wordName = word.optString("Name", null);
			checkName(wordName, "DeneWord", mutationName);
			if(!names.add(wordName)) refuse(mutationName, "two DeneWords named " + wordName + " in " + name);
			JSONObject out = new JSONObject();
			for(Iterator<String> it=word.keys(); it.hasNext();) {
				String key = it.next();
				if(!DENEWORD_KEYS.contains(key)) refuse(mutationName, "the DeneWord " + name + ":" + wordName + " has an attribute a payload may not set: " + key);
				Object v = word.get(key);
				if(v instanceof JSONObject || v instanceof JSONArray) refuse(mutationName, "the DeneWord " + name + ":" + wordName + " has a nested " + key);
				if(v instanceof String && ((String) v).length()>MAX_STRING_VALUE) refuse(mutationName, "the DeneWord " + name + ":" + wordName + " is too long");
				out.put(key, v);
			}
			if(!out.has("Value")) refuse(mutationName, "the DeneWord " + name + ":" + wordName + " has no Value");
			if(!VALUE_TYPES.contains(out.optString("Value Type"))) refuse(mutationName, "the DeneWord " + name + ":" + wordName + " has no known Value Type");
			clean.put(out);
		}
		JSONObject out = new JSONObject().put("Name", name).put("DeneWords", clean).put(TeleonomeConstants.INJECTED_BY_PAYLOAD_ATTRIBUTE, true);
		if(dene.has("Dene Type")) {
			String type = dene.optString("Dene Type", "");
			if(type.length()>MAX_NAME) refuse(mutationName, "the Dene " + name + " has too long a Dene Type");
			out.put("Dene Type", type);
		}
		return out;
	}

	private static boolean hasDene(JSONObject chain, String name) {
		JSONArray denes = chain==null ? null : chain.optJSONArray("Denes");
		for(int i=0;i<length(denes);i++) if(name.equals(denes.getJSONObject(i).optString("Name"))) return true;
		return false;
	}

	private static JSONObject chainNamed(JSONArray chains, String name) {
		for(int i=0;i<length(chains);i++) {
			JSONObject chain = chains.optJSONObject(i);
			if(chain!=null && name.equals(chain.optString("Name"))) return chain;
		}
		return null;
	}

	private static void appendDenes(JSONArray chains, String chainName, JSONArray denes) {
		if(denes.length()==0) return;
		JSONObject chain = chainNamed(chains, chainName);
		if(chain==null) {
			chain = new JSONObject().put("Name", chainName).put("Denes", new JSONArray());
			chains.put(chain);
		}
		if(!chain.has("Denes")) chain.put("Denes", new JSONArray());
		for(int i=0;i<denes.length();i++) chain.getJSONArray("Denes").put(denes.get(i));
	}

	private static int length(JSONArray a) {
		return a==null ? 0 : a.length();
	}

	private static void refuse(String mutationName, String why) throws InvalidMutation {
		logger.warn("structural payload for " + mutationName + " refused: " + why);
		Hashtable<String,String> details = new Hashtable<String,String>();
		details.put("message", "Structural payload refused: " + why);
		throw new InvalidMutation(details);
	}
}
