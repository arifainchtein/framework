package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.utils.Utils;

public class CopyTimeseriesElementToTimeseriesOperation extends MnemosyneOperation {
	Logger logger;
	public CopyTimeseriesElementToTimeseriesOperation(JSONObject m) {
		super(m);
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(getClass());
	}

	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis, String formatedCurrentTimestamp,
			String formatedCurrentDate, String formatedCurrentTime, ZoneId zoneId) {
		// TODO Auto-generated method stub
		try {
			//
			// the source of the data
			String dataSourcePointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_SOURCE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONArray dataSourceValue = (JSONArray)denomeManager.getDeneWordAttributeByIdentity(new Identity(dataSourcePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			//
			// get the selector
			String selector = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_ELEMENT_SELECTOR, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			logger.debug("copy timeseries dataSourceValue.length()=" + dataSourceValue.length() + " selector=" + selector+ " dataSourcePointer=" + dataSourcePointer + " dataSourceValue=" + dataSourceValue);


			double valueToCopyJSONObject=0;
			JSONObject anyJSONObject=null;
			Object aValue=0;
			JSONObject selectedJSONObject=null;
			if(dataSourceValue.length()>0) {
				for(int i=0;i<dataSourceValue.length();i++) {
					anyJSONObject = dataSourceValue.getJSONObject(i);

					aValue = anyJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

					if(selector.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_SELECTOR_MAXIMUM_VALUE)) {
						if(aValue instanceof Integer) {
							int v = (int)aValue;
							if(v>=valueToCopyJSONObject) {
								valueToCopyJSONObject=v;
								selectedJSONObject=anyJSONObject;
							}
						}else if(aValue instanceof Double) {
							double v = (double)aValue;
							if(v>=valueToCopyJSONObject) {
								valueToCopyJSONObject=v;
								selectedJSONObject=anyJSONObject;
							}
						}else if(aValue instanceof Long) {
							long v = (long)aValue;
							if(v>=valueToCopyJSONObject) {
								valueToCopyJSONObject=v;
								selectedJSONObject=anyJSONObject;
							}
						}


					}else if(selector.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_SELECTOR_MINIMUM_VALUE)) {
						if(aValue instanceof Integer) {
							int v = (int)aValue;
							if(v<=valueToCopyJSONObject) {
								valueToCopyJSONObject=v;
								selectedJSONObject=anyJSONObject;
							}
						}else if(aValue instanceof Double) {
							double v = (double)aValue;
							if(v<=valueToCopyJSONObject) {
								valueToCopyJSONObject=v;
								selectedJSONObject=anyJSONObject;
							}
						}else if(aValue instanceof Long) {
							long v = (long)aValue;
							if(v<=valueToCopyJSONObject) {
								valueToCopyJSONObject=v;
								selectedJSONObject=anyJSONObject;
							}
						}
					}
				}
			}




			if(selectedJSONObject!=null)logger.debug("selectedJSONObject=" + selectedJSONObject.toString(4));
			else logger.debug("selectedJSONObjectis null=" );
			int counterLimit = (int) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_LIMIT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			String dataPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject dataDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(new Identity(dataPointer), TeleonomeConstants.COMPLETE);
			//
			// the datJSONArray contains the actual timeseries array
			//
			JSONArray dataJSONArray =  dataDeneWord.getJSONArray(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


			String counterPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject counterDeneWord = (JSONObject) denomeManager.getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);

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
				dataJSONArray.put(selectedJSONObject);
				dataDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataJSONArray);
			}else {
				JSONArray newDataJSONArray = new JSONArray();
				//
				// put the new object in the first position
				newDataJSONArray.put(selectedJSONObject);
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
		}catch(InvalidDenomeException e) {
			logger.warn(Utils.getStringException(e));
		}
	}

}
