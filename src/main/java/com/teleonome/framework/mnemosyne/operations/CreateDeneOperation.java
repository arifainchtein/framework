package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;

public class CreateDeneOperation extends MnemosyneOperation {
	Logger logger;
	public CreateDeneOperation(JSONObject m) {
		super(m);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis,
			String formatedCurrentTimestamp,String formatedCurrentDate,
			String formatedCurrentTime, ZoneId zoneId) {
			// TODO Auto-generated method stub
		//
		// list the files and create a dene for each one
		// there can be multiple targets, ie you can add a dene to 
		// the current hour, current day etc
		// so first get the targets, which are identity pointers that point to the chain where this new dene
		//needs to be created
		//
		JSONArray targetsJSONArray = denomeManager.getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		//
		// ok, now we have the targets, and we know we need to create a Dene, so first create a Dene
		// add all the DeneWords and then add the Dene to the Target
		// each target looks like:
		//   "@MonoNanny:Mnemosyne:Mnemosyne Today",
		//

		String destinationIdentityPointer, newDeneName;
		JSONObject destinationDeneChain, newDene;
		int newDenePosition;
		JSONArray destinationDenes, newDeneDeneWords, copyDeneWordPointersJSONArray;
		for(int j=0;j<targetsJSONArray.length();j++){
			destinationIdentityPointer = targetsJSONArray.getString(j);
			logger.info("destinationIdentityPointer=" + destinationIdentityPointer);
			destinationDeneChain = denomeManager.getDenomicElementByIdentity(new Identity(destinationIdentityPointer));
			destinationDenes = destinationDeneChain.getJSONArray("Denes");
			newDenePosition=0;
			if(destinationDenes==null){
				destinationDenes=new JSONArray();
				destinationDeneChain.put("Denes", destinationDenes);
				newDenePosition=1;
			}

			newDene = new JSONObject();
			newDeneDeneWords = new JSONArray();
			newDene.put("DeneWords", newDeneDeneWords);
			//
			// the name of this new dene is a deneword where the name is "New Dene Name'
			newDeneName = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_NEW_DENE_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			newDene.put("Name", newDeneName);
			//
			// now that we have the name, look for the next position, this means, go to the chain
			// and see what is the position of the last copy of that dene that we have in the chain
			// destinationDenes could have several copies of the same dene
			// for example, if this is a daily, and you are running the process every hour
			// and is 8am, by the time you are adding a new dene, there are already 7 denes with the same
			//name, but what distinguish them is that each dene has a an attribute called "Position"
			// that way when we want to make reports or operationswe know which is the last value

			newDenePosition = denomeManager.getNextPostionForDeneInMnemosyneChain(destinationDeneChain, newDeneName);
			newDene.put("Position", newDenePosition);
			//
			//
			// also add when this dene was created


			newDene.put("Timestamp", formatedCurrentTime);
			newDene.put("Timestamp Milliseconds", currentTimeMillis);
			//
			// Now add denewords to this new dene, you can either copy an existing deneword or create a new one from scratch
			//
			copyDeneWordPointersJSONArray = denomeManager.getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_COPY_DENEWORD, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			//
			// the above array will contain pointers to areas in the denome, we want the whole DenWord so
			//
			String deneWordPointer;
			JSONObject deneWordToCopy;
			for(int i=0;i<copyDeneWordPointersJSONArray.length();i++){
				deneWordPointer = copyDeneWordPointersJSONArray.getString(i);
				try {
					deneWordToCopy  = denomeManager.getDeneWordByIdentity(new Identity(deneWordPointer));
					//
					// as an added attribute, store the pointer as a "Source" Attribute
					deneWordToCopy.put("Source", deneWordPointer);
					//
					// now add this deneword to the new dene
					//
					newDeneDeneWords.put(deneWordToCopy);
				} catch (InvalidDenomeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}

			//
			// finally add the new dene to the chain
			logger.info("adding new dene =" + newDene.getString("Name"));
			destinationDenes.put(newDene);
		}
	}

}
