package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;

public class CopyDeneOperation extends MnemosyneOperation {
	Logger logger;
	public CopyDeneOperation(JSONObject m) {
		super(m);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}
	public void process(DenomeManager denomeManager, long currentTimeMillis,
			String formatedCurrentTimestamp,String formatedCurrentDate,
			String formatedCurrentTime, ZoneId zoneId) {
		//
		// get the source dene
		//
		String copySourceDeneIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_DENE_SOURCE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		logger.info("line 3237 copySourceDeneIdentityPointer =" + copySourceDeneIdentityPointer);
		JSONObject copySourceDene = denomeManager.getDenomicElementByIdentity(new Identity(copySourceDeneIdentityPointer));
		JSONObject clonedSourceDene  = new JSONObject(copySourceDene, JSONObject.getNames(copySourceDene));
		clonedSourceDene.put("Timestamp", formatedCurrentTimestamp);
		clonedSourceDene.put("Timestamp Milliseconds", currentTimeMillis);

		String targetMnemosyneDeneChainIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		JSONObject targetMnemosyneDeneChain = denomeManager.getDenomicElementByIdentity(new Identity(targetMnemosyneDeneChainIdentityPointer));
		JSONArray targetMnemosyneDeneChainDenesJSONArray = targetMnemosyneDeneChain.getJSONArray("Denes");

		int newDenePosition=0;
		if(targetMnemosyneDeneChainDenesJSONArray==null){
			targetMnemosyneDeneChainDenesJSONArray=new JSONArray();
			targetMnemosyneDeneChain.put("Denes", targetMnemosyneDeneChainDenesJSONArray);
			newDenePosition=1;
		}

		newDenePosition = denomeManager.getNextPostionForDeneInMnemosyneChain(targetMnemosyneDeneChain, clonedSourceDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
		logger.info("line 3255 newDenePosition =" + newDenePosition);
		clonedSourceDene.put("Position", newDenePosition);
		targetMnemosyneDeneChainDenesJSONArray.put(clonedSourceDene);
	}

}
