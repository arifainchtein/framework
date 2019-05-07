package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;

public class AddDeneWordToDeneOperation extends MnemosyneOperation {
		Logger logger;
		public AddDeneWordToDeneOperation(JSONObject m) {
			super(m);
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub
		}
		public void process(DenomeManager denomeManager, long currentTimeMillis,
				String formatedCurrentTimestamp,String formatedCurrentDate,
				String formatedCurrentTime, ZoneId zoneId) {
			
			//
			// get the operation to see what we are doing
			// 
			JSONObject selectedDene=null; 
			String operation = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_OPERATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			//
			// the next few lines are common to all the different
			// type of operations in the add_deneword_to_dene
			//
			// we are adding a new deneword to an existing dene
			// get that existing dene first.  the address of the dene is
			// located as a value in the deneword of type Mnemosyne Target
			logger.debug("mnemosyneDene=" + mnemosyneDene.toString(4));

			String targetDeneIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			logger.debug("line 2576 targetDeneIdentityPointer=" + targetDeneIdentityPointer);
			Identity identity = new Identity(targetDeneIdentityPointer);
			Identity deneChainIdentity = new Identity(identity.getTeleonomeName(), identity.getNucleusName(), identity.getDenechainName());
			String deneName = identity.getDeneName();
			JSONObject deneChain = denomeManager.getDenomicElementByIdentity(deneChainIdentity);
			//
			// The potentialDenes is an array with all the denes, so now identity which is the dene we want
			// besides the actuall dene, we need which copy of the dene, since there could be many denes with the same name
			// if we are storing one every hour, at noon there would be 12 copies of the same dene in the today chain
			// so we need to specify which copy of the dene we want to use.  for this we use need to get the 
			// deneword called MNEMOSYNE_DENEWORD_TARGET_POSITION, which could be a number or a variable like 
			// COMMAND_MNEMOSYNE_LAST_DENE_POSITION
			String targetPosition= (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_TARGET_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONArray denes = deneChain.getJSONArray("Denes");
			selectedDene = denomeManager.getDeneFromDeneJSONArrayByPostion(denes,  deneName, targetPosition);
			//
			// at this point, selectedDene is the one we need to add a deneword to
			// To simplify the code, as part o the standard, the dene that is being used
			// already contains a deneword which will be used as a template
			// this deneword has the type of Create DeneWord Source
			//
			// so we need to get it and replace the value attribute of this deneword
			// with that value that is calculated by aggregation.  so first get the deneword
			JSONObject createDeneWordSource = (JSONObject) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_CREATE_DENEWORD_SOURCE, TeleonomeConstants.COMPLETE);
			//
			// use that deneword to create a new one that will be inserted, the source has all the data except the actuall value
			// that needs to be calculated
			//
			JSONObject newDeneWord = new JSONObject();
			newDeneWord.put("Name", createDeneWordSource.getString("Name"));
			newDeneWord.put("Required", createDeneWordSource.getBoolean("Required"));
			newDeneWord.put("Value Type", createDeneWordSource.getString("Value Type"));
			//
			// and add this word to the DeneWords of the selectedDene
			JSONArray selectedDeneDeneWords = selectedDene.getJSONArray("DeneWords");
			selectedDeneDeneWords.put(newDeneWord);

			//
			// the value will change according to the operation type
			//
			logger.debug("operation=" + operation);
			if(operation.equals(TeleonomeConstants.MNEMOSYNE_CREATE_DENEWORD_ADD_TO_DENE_OPERATION)){

				Object o = createDeneWordSource.get("Value");
				if((o instanceof String) ) {
					String ov = (String)o;
					if(ov.equals(TeleonomeConstants.COMMANDS_CURRENT_DATE)) {
						newDeneWord.put("Value", formatedCurrentDate);
						logger.debug("formatedCurrentDate=" + formatedCurrentDate);
					}else if(ov.equals(TeleonomeConstants.COMMANDS_CURRENT_HOUR)) {
						newDeneWord.put("Value", formatedCurrentTime);
						logger.debug("formatedCurrentHour=" + formatedCurrentTime);
					}else if(ov.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP)) {
						newDeneWord.put("Value", formatedCurrentTimestamp);
						logger.debug("formatedCurrentTimestamp=" + formatedCurrentTimestamp);
					}else if(ov.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)) {
						newDeneWord.put("Value", currentTimeMillis);
						logger.debug("formatedCurrentTimestamp=" + formatedCurrentTimestamp);
					}else if(ov.startsWith("@")) {
						try {
							newDeneWord.put("Value", denomeManager.getDeneWordAttributeByIdentity(new Identity(ov), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						} catch (JSONException | InvalidDenomeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						logger.debug("formatedCurrentHour=" + formatedCurrentTime);
					}else {
						newDeneWord.put("Value", o);
					}
				}


			}else if(operation.equals(TeleonomeConstants.MNEMOSYNE_DENEWORD_AGGREGATION_OPERATION)){

				//
				// finally, calculated the value this is done by taking a source and adding a value to it
				// both the source and the value  can be from the mnemosyne and therefore can be more than one dene with the 
				// same name, but it can also be from the rest of the denome in which case you will only
				// have one dene. the source data is dound in "Aggregate From" and is a pointer which points 
				// all the way to the DeneWord, so we need to first extract the address of the Dene, to discover
				// which dene to use
				//
				// source
				//
				String aggregateFromDeneWordIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate From", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("aggregateFromDeneWordIdentityPointer=" + aggregateFromDeneWordIdentityPointer );

				Identity aggregateFromDeneWordIdentity = new Identity(aggregateFromDeneWordIdentityPointer);
				logger.debug( " aggregateFromDeneWordIdentity=" + aggregateFromDeneWordIdentity);
				Identity aggregateFromDeneIdentity = new Identity(aggregateFromDeneWordIdentity.getTeleonomeName(),aggregateFromDeneWordIdentity.getNucleusName(),aggregateFromDeneWordIdentity.getDenechainName(),aggregateFromDeneWordIdentity.getDeneName());

				logger.debug("aggregateFromDeneIdentity=" + aggregateFromDeneIdentity);

				JSONArray allAggregateFromDenes;
				double sourceValue=0;
				try {
					allAggregateFromDenes = denomeManager.getAllDenesByIdentity(aggregateFromDeneIdentity);
					//
					// first check to see if there is none, only one or more than one
					// if this is the first aggregation there will be none therefore get the
					// default attribute.  if we are starting, allAggregateFromDenes will
					// have dene ,since it could have been created in a previous operation
					// in the same cycle, but it will not have an actual value which is calculated
					// by this operatin, for this reason, get the default value
					JSONObject aggregateFromDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate From", TeleonomeConstants.COMPLETE);
					Double defaultValue = aggregateFromDeneWord.getDouble("Default");
					logger.debug("defaultValue=" + defaultValue );
					//
					logger.debug("allAggregateFromDenes.klength=" + allAggregateFromDenes.length());

					JSONObject deneToAggregateFrom=null;
					
					//
					// if this is the first time we do this, then we just created a dene
					// therefore there will not be something to aggreagete from
					//
					if(allAggregateFromDenes.length()<=1){
						//
						// get the default value
						//
						sourceValue = defaultValue;

					}else{
						//
						// there is more than one, therefore get the deneword called "Aggregate From Dene Position" that will tell you
						// which dene to use 
						String aggregateFromDenePosition = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene,  "Aggregate From Dene Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//
						// now use that variable to get the actual dene we are going to use
						//
						deneToAggregateFrom = denomeManager.getDeneFromDeneJSONArrayByPostion(allAggregateFromDenes,  aggregateFromDeneWordIdentity.getDeneName(), aggregateFromDenePosition);

						Object o = denomeManager.getDeneWordAttributeByDeneWordNameFromDene(deneToAggregateFrom, aggregateFromDeneWordIdentity.getDeneWordName(), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

						if(o==null){
							sourceValue=defaultValue;
						}else if (o instanceof String){
							sourceValue = Double.parseDouble((String)o);
						}else if(o instanceof Double){
							sourceValue=((Double)o).doubleValue();
						}else if(o instanceof Integer){
							sourceValue=((Integer)o).doubleValue();
						}

					}
				} catch (InvalidDenomeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				//
				// assume is a double since we are aggregating
				//

				//
				// value
				// 
				String aggregateValueDeneWordIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate Value", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				Identity aggregateValueDeneWordIdentity = new Identity(aggregateValueDeneWordIdentityPointer);
				Identity aggregateValueDeneIdentity = new Identity(aggregateValueDeneWordIdentity.getTeleonomeName(),aggregateValueDeneWordIdentity.getNucleusName(),aggregateValueDeneWordIdentity.getDenechainName(),aggregateValueDeneWordIdentity.getDeneName());

				JSONArray allAggregateValueDenes;
				try {
					allAggregateValueDenes = denomeManager.getAllDenesByIdentity(aggregateValueDeneWordIdentity);
					//
					// 
					//
					JSONObject deneToAggregateValue=null;
					if(allAggregateValueDenes.length()==1){
						deneToAggregateValue = allAggregateValueDenes.getJSONObject(0);
					}else{
						//
						// there is more than one, therefore get the deneword called "Aggregate From Dene Position" that will tell you
						// which dene to use 
						String aggregateValueDenePosition = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene,  "Aggregate Value Dene Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//
						// now use that variable to get the actual dene we are going to use
						//
						deneToAggregateValue = denomeManager.getDeneFromDeneJSONArrayByPostion(allAggregateValueDenes,  aggregateValueDeneWordIdentity.getDeneName(), aggregateValueDenePosition);
						//
						// asume is a double since we are aggregating
						// but because it can be a deneword from purpose
						// receive it as an object

						Object o = denomeManager.getDeneWordAttributeByDeneWordNameFromDene(deneToAggregateValue, aggregateValueDeneWordIdentity.getDeneWordName(), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

						double value=0;

						if(o==null){
							value=0;
						}else if (o instanceof String){
							value = Double.parseDouble((String)o);
						}else if(o instanceof Double){
							value=((Double)o).doubleValue();
						}else if(o instanceof Integer){
							value=((Integer)o).doubleValue();
						}

						//
						// finally, add these two values and set the resulting as the value for the newDeneWord
						//
						double total= sourceValue + value;
						logger.debug("sourceValue=" + sourceValue + " value=" + value);
						newDeneWord.put("Value", total);
					}
				} catch (InvalidDenomeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				

			}
		}
}
