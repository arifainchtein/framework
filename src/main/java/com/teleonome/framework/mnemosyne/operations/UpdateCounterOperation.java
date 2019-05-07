package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.utils.Utils;

public class UpdateCounterOperation extends MnemosyneOperation {

	Logger logger;
	public UpdateCounterOperation(JSONObject m) {
		super(m);
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(getClass());
	}

	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis, String formatedCurrentTimestamp,
			String formatedCurrentDate, String formatedCurrentTime, ZoneId zoneId) {
		// TODO Auto-generated method stub
		try {
			JSONObject counterLimitDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_COUNTER_LIMIT, TeleonomeConstants.COMPLETE);
			String counterLimitDeneWordValueType = counterLimitDeneWord.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
			int counterLimit=-1;
			if(counterLimitDeneWordValueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)) {
				//
				// this is a pointer so render it
				String counterLimitDeneWordPointer = counterLimitDeneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				counterLimit = (int)denomeManager.getDeneWordAttributeByIdentity(new Identity(counterLimitDeneWordPointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			}else {
				//
				// is just an int so get it directly
				counterLimit = counterLimitDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			}


			JSONObject counterIncrementDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_COUNTER_INCREMENT, TeleonomeConstants.COMPLETE);
			String counterIncrementDeneWordValueType = counterIncrementDeneWord.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
			int counterIncrement=-1;
			if(counterIncrementDeneWordValueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)) {
				//
				// this is a pointer so render it
				String counterIncrementDeneWordPointer = counterIncrementDeneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				counterIncrement = (int)denomeManager.getDeneWordAttributeByIdentity(new Identity(counterIncrementDeneWordPointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			}else {
				//
				// is just an int so get it directly
				counterIncrement = counterIncrementDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			}


			String counterPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			JSONObject counterDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);
			int counterCurrentValue = counterDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			logger.debug("line 2933, counterPointer=" + counterPointer + " counterIncrement=" + counterIncrement + " counterCurrentValue=" + counterCurrentValue + " counterLimit=" + counterLimit);

			if(counterCurrentValue<counterLimit) {
				counterCurrentValue++;
				logger.debug("line 3216, increasing the currentcounter to  " + counterCurrentValue );
			}else {
				counterCurrentValue=0;
				logger.debug("line 3218, reseting the currentcounter to  0"  );
			}


			counterDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, counterCurrentValue);
		}catch(InvalidDenomeException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

}
