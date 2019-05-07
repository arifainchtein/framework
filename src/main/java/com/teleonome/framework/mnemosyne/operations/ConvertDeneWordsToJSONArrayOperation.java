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

public class ConvertDeneWordsToJSONArrayOperation extends MnemosyneOperation {

	Logger logger;
	public ConvertDeneWordsToJSONArrayOperation(JSONObject m) {
		super(m);
		// TODO Auto-generated constructor stub
		logger = Logger.getLogger(getClass());
	}

	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis, String formatedCurrentTimestamp,
			String formatedCurrentDate, String formatedCurrentTime, ZoneId zoneId) {
		try {
			// the destination is a deneword
			String dataDestinationPointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_OPERATION_DESTINATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject dataDestinationDeneWord = (JSONObject)denomeManager.getDeneWordAttributeByIdentity(new Identity(dataDestinationPointer), TeleonomeConstants.COMPLETE);
			logger.debug("dataDestinationPointer="+dataDestinationPointer+ " dataDestinationDeneWord=" + dataDestinationDeneWord);
			//
			// if the destination is in the Mnemosyne Pulse, it would  have gotten erased at the beginning of the pulse
			// therefore you must add it
			if(dataDestinationDeneWord==null) {
				Identity destIdentity =new Identity(dataDestinationPointer);
				if(destIdentity.getDenechainName().equals(TeleonomeConstants.DENECHAIN_MNEMOSYNE_PULSE)) {
					//
					// if we are here we need to create a Dene that looks like:
					//						{
					//                            "Name":"Ra Disk Usage",
					//                            "DeneWords":[
					//                                {
					//                                    "Value": [],
					//                                    "Name": "Ra Disk Usage",
					//                                    "Value Type": "JSON Array",
					//                                    "Required":true
					//                                        
					//                                }
					//                            ]
					//                        }
					JSONObject destDeneChain= denomeManager.getDeneChainByIdentity(destIdentity);
					JSONArray destDenes= destDeneChain.getJSONArray("Denes");
					JSONObject destDene = new JSONObject();
					destDenes.put(destDene);
					destDene.put(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, destIdentity.getDeneName());
					JSONArray deneWords = new JSONArray();
					destDene.put("DeneWords", deneWords);
					dataDestinationDeneWord = new JSONObject();
					deneWords.put(dataDestinationDeneWord);
					dataDestinationDeneWord.put(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, destIdentity.getDeneWordName());
					dataDestinationDeneWord.put(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, destIdentity.getDeneWordName());
					dataDestinationDeneWord.put(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE, TeleonomeConstants.DATATYPE_JSONARRAY);
				}
			}
			//
			//the source is a dene
			String dataSourcePointer = (String) denomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_DATA_SOURCE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject dataSourceDene;
			dataSourceDene = (JSONObject)denomeManager.getDeneByIdentity(new Identity(dataSourcePointer));

			logger.debug("dataSourcePointer="+dataSourcePointer+ " dataDestinationDeneWord=" + dataSourceDene);

			//
			// create an array og jsonobects and set it as a value in the destination
			JSONArray deneWords = dataSourceDene.getJSONArray("DeneWords");
			dataDestinationDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, deneWords);
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
	}

}
