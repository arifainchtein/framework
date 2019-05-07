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
import com.teleonome.framework.utils.Utils;

public class UpdateTimeSeriesCounterOperation extends MnemosyneOperation {
	Logger logger;
	public UpdateTimeSeriesCounterOperation(JSONObject m) {
		super(m);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis, String formatedCurrentTimestamp,
			String formatedCurrentDate, String formatedCurrentTime, ZoneId zoneId) {
		try {
			//
			// the source of the data
			String dataSourcePointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_SOURCE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			String dataSourceValueType = (String) denomeManager.getDeneWordAttributeByIdentity(new Identity(dataSourcePointer), TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);

			//
			// the new object that will go into the time series
			//
			JSONObject newValueJSONObject=new JSONObject();
			newValueJSONObject.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, denomeManager.getcurrentlyCreatingPulseTimestampMillis());
			Object o = denomeManager.getDeneWordAttributeByIdentity(new Identity(dataSourcePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			logger.debug("line 3476 dataSourcePointer=" + dataSourcePointer + " dataSourceValue=" + o);
			if(o instanceof Integer) {
				int dataSourceValue = (int)o;
				newValueJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataSourceValue);	
			}else if (o instanceof Double) {
				double dataSourceValue = (double) o;
				newValueJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataSourceValue);	
			}else if (o instanceof Long) {
				long dataSourceValue = (long) o;
				newValueJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataSourceValue);	
			}


			logger.debug("newValueJSONObject=" + newValueJSONObject.toString(4));

			int counterLimit = (int) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_LIMIT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			String dataPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject dataDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(new Identity(dataPointer), TeleonomeConstants.COMPLETE);
			//
			// the datJSONArray contains the actual timeseries array
			//
			JSONArray dataJSONArray =  dataDeneWord.getJSONArray(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


			String counterPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject counterDeneWord = (JSONObject)denomeManager.getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);

			int counterCurrentValue = counterDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			if(counterCurrentValue<(counterLimit-1)) {
				counterCurrentValue++;
			}else {
				counterCurrentValue=0;
			}
			counterDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, counterCurrentValue);

			//
			// if the array has less than the limit just add the new one
			// if it already has the limit then create a new JSONArray
			if(dataJSONArray.length()<counterLimit) {
				dataJSONArray.put(newValueJSONObject);
				dataDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataJSONArray);
			}else {
				JSONArray newDataJSONArray = new JSONArray();
				//
				// put the new object in the first position
				newDataJSONArray.put(newValueJSONObject);
				//
				// now put all the other elements except the last one into the new array
				JSONObject jobj;
				for(int i=0;i<counterLimit-1;i++) {
					newDataJSONArray.put(dataJSONArray.get(i));
				}
				//
				// and store the new array
				//
				dataDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, newDataJSONArray);
			}
		} catch (InvalidDenomeException | JSONException e) {
			Utils.getStringException(e);
		}
	}

}
