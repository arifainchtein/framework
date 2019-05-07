package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.utils.Utils;

public class UpdateValueOperation extends MnemosyneOperation {
	Logger logger;
	public UpdateValueOperation(JSONObject m) {
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
		try {
			if(operation.equals(TeleonomeConstants.MNEMOSYNE_DENEWORD_TRANSFORMATION_OPERATION)){

				logger.debug("line 2765 mnemosyneDene=" + mnemosyneDene.toString(4));
				//
				// the process to follow is:
				// 1)Get the function used to transform
				// 2)Get the source of the data, this would be a number
				// 3)get the Target Deneword which is where the result of the operation is stored, this is adeneword
				// 4)transform the source data and store it in the target
				// 5) Update Time fields

				//
				// 1)Function
				//
				String function = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Function", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("transofrm function=" + function );



				//
				//2) get the data source
				//
				String dataSourceIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_TYPE_TRANSFORMATION_DATA_SOURCE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("line 2751 targetDeneWordIdentityPointer=" + dataSourceIdentityPointer);
				Identity aggregateValueDeneWordIdentity = new Identity(dataSourceIdentityPointer);
				Object aggregateValueObject;

				aggregateValueObject = denomeManager.getDeneWordAttributeByIdentity(aggregateValueDeneWordIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				logger.debug("line 2778 aggregateValueObject=" + aggregateValueObject + " class=" + aggregateValueObject.getClass().toString());



				//
				// 3)target 
				//
				String targetDeneWordIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("line 2751 targetDeneWordIdentityPointer=" + targetDeneWordIdentityPointer);
				Identity targetDeneWordIdentity = new Identity(targetDeneWordIdentityPointer);
				JSONObject targetDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(targetDeneWordIdentity,  TeleonomeConstants.COMPLETE);
				//
				// now use the identity of the deneword to get the dene, we need this because we need to update the 
				// timestamp
				Identity targetDeneIdentity  = new Identity(targetDeneWordIdentity.getTeleonomeName(),targetDeneWordIdentity.getNucleusName(), targetDeneWordIdentity.getDenechainName(), targetDeneWordIdentity.getDeneName());
				JSONObject targetDene = denomeManager.getDeneByIdentity(targetDeneIdentity);
				//
				// 4)Transform the value
				//
				if(function.equals(TeleonomeConstants.MNEMOSYNE_DENEWORD_TRANSFORMATION_OPERATION_FUNCTION_ELAPSED_TIME)) {
					int value = ((Double)aggregateValueObject).intValue();				
					String resultingValue = Utils.getElapsedSecondsToHoursMinutesSecondsString(value);
					logger.debug("line 3372 value=" + value + " resultingValue=" + resultingValue );
					targetDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, resultingValue);
				}
				//
				//
				//5) Update times values
				//
				logger.debug("line 3380 About to set the time for the dene in the update,"+  formatedCurrentTime);
				targetDene.put("Timestamp", formatedCurrentTime);
				targetDene.put("Timestamp Milliseconds", currentTimeMillis);

			}else if(operation.equals(TeleonomeConstants.MNEMOSYNE_DENEWORD_AGGREGATION_OPERATION)){

				logger.debug("line 2765 mnemosyneDene=" + mnemosyneDene.toString(4));
				//
				// the process to follow is:
				// 1)Get the source of the data, this would be a number
				// 2)Gete the value to add this is also a number
				// 3)get the Target Deneword which is where the result of the operation is stored, this is adeneword
				// 4)Add the source to the value and store it in the target
				// 5) Update Time fields


				//
				// 1)source
				//
				String aggregateFromDeneWordIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate From", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("aggregateFromDeneWordIdentityPointer=" + aggregateFromDeneWordIdentityPointer );

				Identity aggregateFromDeneWordIdentity = new Identity(aggregateFromDeneWordIdentityPointer);
				Object aggregateFromValueObject =  denomeManager.getDeneWordAttributeByIdentity(aggregateFromDeneWordIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				double aggregateFromValue=0;
				if(aggregateFromValueObject instanceof Integer) {
					aggregateFromValue = ((Integer)aggregateFromValueObject).doubleValue();				
				}else if(aggregateFromValueObject instanceof Double) {
					aggregateFromValue = (double)aggregateFromValueObject;				
				}else if(aggregateFromValueObject instanceof String) {
					aggregateFromValue = Double.parseDouble((String)aggregateFromValueObject);				
				}


				//
				//2) get the value
				//
				String aggregateValueDeneWordIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate Value", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				Identity aggregateValueDeneWordIdentity = new Identity(aggregateValueDeneWordIdentityPointer);
				Object aggregateValueObject = denomeManager.getDeneWordAttributeByIdentity(aggregateValueDeneWordIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("line 2778 aggregateValueObject=" + aggregateValueObject + " class=" + aggregateValueObject.getClass().toString());

				double aggregateValue=0;

				if(aggregateValueObject instanceof Integer) {
					aggregateValue = ((Integer)aggregateValueObject).doubleValue();				
				}else if(aggregateValueObject instanceof Double) {
					aggregateValue = (double)aggregateValueObject;				
				}else if(aggregateValueObject instanceof String) {
					aggregateValue = Double.parseDouble((String)aggregateValueObject);				
				}


				//
				// 3)target 
				//
				String targetDeneWordIdentityPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("line 2751 targetDeneWordIdentityPointer=" + targetDeneWordIdentityPointer);
				Identity targetDeneWordIdentity = new Identity(targetDeneWordIdentityPointer);
				JSONObject targetDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(targetDeneWordIdentity,  TeleonomeConstants.COMPLETE);
				//
				// now use the identity of the deneword to get the dene, we need this because we need to update the 
				// timestamp
				Identity targetDeneIdentity  = new Identity(targetDeneWordIdentity.getTeleonomeName(),targetDeneWordIdentity.getNucleusName(), targetDeneWordIdentity.getDenechainName(), targetDeneWordIdentity.getDeneName());
				JSONObject targetDene = denomeManager.getDeneByIdentity(targetDeneIdentity);
				//
				// 4)Add source to value and store
				//
				double total= aggregateValue + aggregateFromValue;
				logger.debug("line 2781 aggregateValue=" + aggregateValue + " aggregateFromValue=" + aggregateFromValue + " targetDeneWord=" + targetDeneWord);
				targetDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, total);
				//
				//
				//5) Update times values
				//
				logger.debug("line 2830 About to set the time for the dene in the update,"+  formatedCurrentTime);
				targetDene.put("Timestamp", formatedCurrentTime);
				targetDene.put("Timestamp Milliseconds", currentTimeMillis);

			}
		} catch (InvalidDenomeException | JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
	}
}
