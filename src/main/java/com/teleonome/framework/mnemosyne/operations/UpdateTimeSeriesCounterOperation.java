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
			boolean gotValue = true;
			if(o instanceof Integer) {
				int dataSourceValue = (int)o;
				newValueJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataSourceValue);
			}else if (o instanceof Double) {
				double dataSourceValue = (double) o;
				newValueJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataSourceValue);
			}else if (o instanceof Long) {
				long dataSourceValue = (long) o;
				newValueJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataSourceValue);
			}else {
				// Source resolved to null (or some other unexpected type) -- most likely the
				// pointed-to DeneWord was momentarily being rebuilt elsewhere (e.g. an async
				// sensor refresh) at the exact moment this pulse's Actuator ran. Rather than
				// recording a timestamp-only entry with no Value (which downstream chart code
				// then has to defend against), skip adding this pulse to the time series data
				// array below -- but the counter still advances on its normal cadence (see
				// gotValue guard further down), since other Denes key off this counter's cycle
				// (e.g. "(Counter == 0)" conditions) independently of the data array's contents.
				gotValue = false;
				logger.warn("UpdateTimeSeriesCounterOperation: dataSourcePointer=" + dataSourcePointer
						+ " resolved to null or unexpected type (" + (o == null ? "null" : o.getClass().getName())
						+ ") -- skipping this pulse for this time series");
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
			// (skipped entirely when gotValue is false -- see the instanceof checks above --
			// so a pulse with an unreadable source just isn't represented in the data at all,
			// rather than adding a timestamp with no Value)
			if(!gotValue) {
				// no-op: counter above already advanced on its normal cadence
			}else if(dataJSONArray.length()<counterLimit) {
				dataJSONArray.put(newValueJSONObject);
				dataDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataJSONArray);
			}else {
				//
				// Buffer is full: evict the oldest entry (index 0 -- the branch above
				// always appends, so index 0 is always the oldest) and append the new
				// one at the end, keeping a single oldest-first ordering for the whole
				// life of the array. The previous implementation instead built a new
				// array with the new value prepended at index 0 and the old array's
				// tail dropped, which silently flipped the ordering convention the
				// moment the buffer first filled up and evicted the wrong (newest, not
				// oldest) entry each cycle after that. Chart code reads this array in
				// index order and doesn't re-sort, so everything before that flip stayed
				// frozen in ascending order while everything after it grew in descending
				// order at the front -- the same array rendering as two chronologically
				// backwards halves stitched together (observed on ChinampaMonitor's
				// Hypothalamus/Heart/Web Server memory charts, 2026-07-16).
				//
				dataJSONArray.remove(0);
				dataJSONArray.put(newValueJSONObject);
				dataDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dataJSONArray);
			}
		} catch (InvalidDenomeException | JSONException e) {
			Utils.getStringException(e);
		}
	}

}
