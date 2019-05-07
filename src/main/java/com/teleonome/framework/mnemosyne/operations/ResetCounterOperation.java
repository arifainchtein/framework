package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;

public class ResetCounterOperation extends MnemosyneOperation {

	public ResetCounterOperation(JSONObject m) {
		super(m);
		logger = Logger.getLogger(getClass());
	}

	Logger logger;

	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis, String formatedCurrentTimestamp,
			String formatedCurrentDate, String formatedCurrentTime, ZoneId zoneId) {

		// TODO Auto-generated method stub

		try {
			String counterPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			//
			JSONObject counterDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);
			int counterCurrentValue = counterDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			logger.debug("line 2933, reset counter counterPointer=" + counterPointer );

			counterCurrentValue=0;

			counterDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, counterCurrentValue);
		} catch (InvalidDenomeException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

}
