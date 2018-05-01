package com.teleonome.framework.mnemosyne;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.denome.Teleonome;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;

public class MnemosyneIndexer {

	/**
	 * variables to configure
	 */
	private int UPDATE_FREQUENCY_SECONDS=120;

	/*
	 *  end of variables to configure
	 */
	private PostgresqlPersistenceManager aDBManager;
	private MnemosyneManager aMnemosyneManager;
	private String[] chainsToProcess = new String[3];
	private String[] indexToProcess = new String[2];
	private Logger logger;

	public MnemosyneIndexer(){
		aDBManager = PostgresqlPersistenceManager.instance();
		logger = Logger.getLogger(getClass());
		try {
			DenomeManager aDenomeManager = DenomeManager.instance();
			
			aMnemosyneManager = null;// MnemosyneManager.instance(aDenomeManager);
		} catch (MissingDenomeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		chainsToProcess[0]= TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA;
		chainsToProcess[1]= TeleonomeConstants.DENECHAIN_SENSOR_DATA;
		chainsToProcess[2]= TeleonomeConstants.DENECHAIN_EXTERNAL_DATA;

		indexToProcess[0]=TeleonomeConstants.TODAY_INDEX;
		indexToProcess[1]=TeleonomeConstants.LAST_24_HOURS_INDEX;


		UpdateIndexThread anUpdateIndexThread= new UpdateIndexThread();
		anUpdateIndexThread.start();
	}

	public class UpdateIndexThread extends Thread{

		public UpdateIndexThread(){

		}

		public void run(){
			do{
				String teleonomeName;
				Teleonome teleonome;
				Identity identity;

				//
				// first delete the records for this teleonome since they will be recreated
				//
				


				Vector<Teleonome> allTeleonomes = aDBManager.getAllTeleonomes();
				String sqlStatement;
				SimpleDateFormat dateFormater = new SimpleDateFormat("yyy-MM-dd HH:mm:ss"); 
				Calendar cal = Calendar.getInstance();

				long last24HoursEndPulseTimestampMillis = cal.getTime().getTime();
				cal.add(Calendar.HOUR, -24);
				long last24HoursStartPulseTimestampMillis = cal.getTime().getTime();

				//
				// Today
				//
				cal.setTime(new Timestamp(System.currentTimeMillis()));
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				long todayStartPulseTimestampMillis = cal.getTime().getTime();
				cal.add(Calendar.DATE, 1);
				long todayEndPulseTimestampMillis = cal.getTime().getTime();

				for(int i=0;i<allTeleonomes.size();i++){
					teleonome = (Teleonome)allTeleonomes.elementAt(i);
					teleonomeName = teleonome.getName();
					System.out.println("about to process indexes for " + teleonomeName );
					for(int j=0;j<chainsToProcess.length;j++){
						identity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_PURPOSE, chainsToProcess[j]);
						System.out.println("about to process " + identity.toString());
						for(int k=0;k<indexToProcess.length;k++){
							if( indexToProcess[k].equals(TeleonomeConstants.TODAY_INDEX)){
								processChain( identity,  todayStartPulseTimestampMillis,  todayEndPulseTimestampMillis, indexToProcess[k]);
							}else if(indexToProcess[k].equals(TeleonomeConstants.LAST_24_HOURS_INDEX)){
								processChain( identity,  last24HoursStartPulseTimestampMillis,  last24HoursEndPulseTimestampMillis, indexToProcess[k]);
							}
						}
					}
				}
				try {
					System.out.println("finished generating todayIndex about to sleep " );
					Thread.sleep(UPDATE_FREQUENCY_SECONDS*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}while(true);
		}
	}



	private void processChain(Identity identity, long startPulseTimestampMillis, long endPulseTimestampMillis, String whichIndex){

		//
		// now get the data for this 
		//
		String teleonomeName = identity.getTeleonomeName();
		ArrayList<Map.Entry<JSONObject, Long>> pulsesForTeleonome = aDBManager.getPurposeChainForIndexing(identity.toString(), startPulseTimestampMillis, endPulseTimestampMillis);

		JSONObject purposeDeneChain;
		long timeStampMillis;
		JSONObject  dene, deneWord;
		JSONArray denes, deneWords;
		ArrayList<Map.Entry<Object, Long>> deneWordValueByPeriod;
		Identity deneWordIdentity;
		Object deneWordValue;
		JSONObject deneWordPair = new JSONObject();
		JSONArray deneWordIndex = new JSONArray();
		Hashtable<String, Object> flattenChainIndex;
		String identityString;
		SimpleDateFormat hourlyFormat = new SimpleDateFormat("HH:mm:ss");
		Hashtable identityStringArrayIndex = new Hashtable();

		for (Map.Entry<JSONObject, Long> entry3 : pulsesForTeleonome) {
			purposeDeneChain = entry3.getKey();
			timeStampMillis = ((Long)entry3.getValue()).longValue();
			try {

				flattenChainIndex = DenomeUtils.flattenChain( teleonomeName, purposeDeneChain);//aMnemosyneManager.getDeneWordByPeriodFromOrganismPulse(deneWordIdentity,  startPulseTimestampString,  endPulseTimestampString);
				
				for(Enumeration en=flattenChainIndex.keys();en.hasMoreElements();){
					identityString = (String)en.nextElement();
					deneWordValue = flattenChainIndex.get(identityString);

					deneWordValueByPeriod = (ArrayList<Map.Entry<Object,Long>>)identityStringArrayIndex.get(identityString);
					if(deneWordValueByPeriod==null)deneWordValueByPeriod = new ArrayList();

					deneWordValueByPeriod.add(new AbstractMap.SimpleEntry<Object,Long>(deneWordValue, new Long(timeStampMillis)));
					Collections.sort(deneWordValueByPeriod, new Comparator<Map.Entry<?, Long>>(){
						public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
							return o1.getValue().compareTo(o2.getValue());
						}});

					identityStringArrayIndex.put(identityString, deneWordValueByPeriod);
				}




			} catch (InvalidDenomeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		//
		// now identityStringArrayIndex.put(identityString, deneWordValueByPeriod); has the data for all pulse for that 
		// teleonome and denechain
		// now store them in the database
		Object o;
		String timestampString;

		Timestamp pulseTimestamp;
		
		
		for(Enumeration en=identityStringArrayIndex.keys();en.hasMoreElements();){
			identityString = (String)en.nextElement();
			deneWordValueByPeriod = (ArrayList<Entry<Object, Long>>) identityStringArrayIndex.get(identityString);
			deneWordIndex= new JSONArray();

			for ( Map.Entry<Object, Long> entry3 : deneWordValueByPeriod) {
				o = entry3.getKey();
				pulseTimestamp = new Timestamp(((Long)entry3.getValue()).longValue());
				timestampString = hourlyFormat.format(pulseTimestamp);
				try {
					deneWordPair= new JSONObject();
					deneWordPair.put("Time",timestampString);
					deneWordPair.put("Value",o);
					deneWordIndex.put(deneWordPair);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			aDBManager.deleteIndex(whichIndex, identityString);
			
			aDBManager.storeIndex(whichIndex,identityString, deneWordIndex);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MnemosyneIndexer();
	}

}
