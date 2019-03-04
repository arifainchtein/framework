package com.teleonome.framework.denome;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Map.Entry;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.exception.InvalidDeneStructureRequestException;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.exception.TeleonomeValidationException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;


public class DenomeViewManager {

	//
	// Application level variables read from the Denome
	//
	private int basePulseFrequency=0;
	private int interSensorReadTimeoutMilliseconds=0;
	private String timeZone="";

	String localIpAddress="", hostName="";
	String selectedDenomeFileName="";

	SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z");
	private static DenomeViewManager aDenomeManager=null;
	private Logger logger;
	JSONObject internalNucleus=null;
	JSONObject purposeNucleus=null;
	JSONObject mnemosyneNucleus=null;
	JSONObject humanInterfaceNucleus=null;
	JSONObject denomeJSONObject = null;
	JSONObject previousPulseJSONObject=null;
	JSONObject denomeObject=null;
	Hashtable deneChainNameDeneChainIndex = new Hashtable();


	public Hashtable pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex = new Hashtable();
	public Hashtable pointerToMicroControllerActuatorExecutionPositionDeneIndex = new Hashtable();
	ArrayList<Map.Entry<JSONObject, Integer>> actuatorExecutionPositionDeneForInitialIndex = new ArrayList();

	ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex = new ArrayList(); 
	ArrayList<Map.Entry<JSONObject, Integer>> actuatorExecutionPositionDeneIndex = new ArrayList();
	ArrayList<Map.Entry<String, Integer>> microControllerPointerProcessingQueuePositionIndex = new ArrayList();
	Hashtable<String,ArrayList> actuatorDeneNameActuatorActionEvaluationPositionActionIndex = new Hashtable();
	Hashtable<String,ArrayList> actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex = new Hashtable();

	Hashtable pointerToMicroControllerSensorDenesVectorIndex = new Hashtable();
	Hashtable pointerToMicroControllerActuatorExecutionPositionForInitialDeneIndex  = new Hashtable();


	//
	// new variables fr new loaddenome method
	//
	JSONArray analyticonDenesJSONArray ,mnemosyconDenesJSONArray ;
	JSONArray microProcessorsDenesJSONArray;
	String denomeName;
	Vector sensorDenesVector = new Vector();
	ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 
	Hashtable pointerToMicroControllerSensorsDeneWordsForInitialBySensorRequestQueuePositionIndex = new Hashtable();
	Hashtable<String,ArrayList> denesToRememberByTeleonome = new Hashtable();
	Hashtable<String,JSONArray> externalDataNameDeneWords = new Hashtable();	
	HashMap externalDataLocationHashMap = new HashMap();
	Hashtable microControllerNameMicroControllerParamsIndex = new Hashtable();
	Hashtable<String,ArrayList> deneWordsToRememberByTeleonome = new Hashtable();
	Hashtable<String,ArrayList> deneChainsToRememberByTeleonome = new Hashtable();
	JSONArray rememeberedDeneWordsMnemosyconDenesJSONArray = new JSONArray();
	
	public DenomeViewManager(){
		logger = Logger.getLogger(getClass());
		logger.debug("Initiating Denome Manager");

	}

	public static DenomeViewManager instance() throws MissingDenomeException {

		if(aDenomeManager==null){
			aDenomeManager = new DenomeViewManager();

		}
		return aDenomeManager;
	}

	public Hashtable getPointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex() {
		return pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex;
	}
	
	
	public Hashtable getPointerToMicroControllerSensorDenesVectorIndex(){
		return pointerToMicroControllerSensorDenesVectorIndex;		
	}


	public ArrayList<Map.Entry<String, Integer>> getMicroControllerPointerProcessingQueuePositionIndex(){
		return microControllerPointerProcessingQueuePositionIndex;
	}

	public Hashtable getMicroControllerNameActuatorsIndex(){
		return pointerToMicroControllerActuatorExecutionPositionDeneIndex;
	}


	public JSONArray getMutations(String mutationType){
		if(!denomeObject.has("Mutations")){
			logger.debug("Denome does not have Mutations");
			return null;
		}
		String mutationName="";
		try {
			JSONArray mutationsJSONArray = denomeObject.getJSONArray("Mutations");
			JSONObject mutation;
			JSONArray toReturn = new JSONArray();
			String type;

			for(int i=0;i<mutationsJSONArray.length();i++){
				mutation = mutationsJSONArray.getJSONObject(i);
				mutationName = mutation.getString("Name");
				type = mutation.getString(TeleonomeConstants.MUTATION_TYPE_ATTRIBUTE);
				if(type.equals(mutationType)){
					toReturn.put(mutation);
				}
			}
			return toReturn;

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug("the mutation " + mutationName + " does not have a type");
			e.printStackTrace();
		}
		return null;
	}


	/**
	 *  breaks the name into lines
	 * @param name - the name to break
	 * @param separator -  what character to use to separate normally space
	 * @param wordsPerLine 
	 * @param outputSeparator - how are the lines separated, in html <br>
	 * @return
	 */

	public String prepareDeneWordNameForDisplay(String name, String separator, int wordsPerLine, String outputSeparator){
		String[] tokens = name.split(separator);
		if(tokens.length<=wordsPerLine)return name;
		int wordCounter=0;
		StringBuffer buffer = new StringBuffer();
		for(int i=0;i<tokens.length;i++){
			buffer.append(tokens[i]);
			if(wordCounter==0){
				buffer.append(" ");
			}

			wordCounter++;
			if(wordCounter==wordsPerLine){
				buffer.append(outputSeparator);
				wordCounter=0;
			}
		}
		return buffer.toString();
	}

	public void loadDenome(String denomeFileInString) throws MissingDenomeException, TeleonomeValidationException{
		denomeJSONObject = new JSONObject(denomeFileInString);
		loadDenome( denomeJSONObject);
	}

	// ********************************************
	public void loadDenome(JSONObject denomeJSONObject) throws MissingDenomeException{//
		

		try {
			//
			// initialize the variables so as to not duplicate
			//
			actuatorExecutionPositionDeneIndex = new ArrayList();

			actuatorExecutionPositionDeneForInitialIndex = new ArrayList();
			sensorRequestQueuePositionDeneWordIndex = new ArrayList();
			analyticonDenesJSONArray = new JSONArray();
			mnemosyconDenesJSONArray = new JSONArray();

			pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex = new Hashtable();
			pointerToMicroControllerSensorsDeneWordsForInitialBySensorRequestQueuePositionIndex= new Hashtable();
			microControllerPointerProcessingQueuePositionIndex = new ArrayList();

			//
			// end of variable initialization
			//

			
			denomeObject = denomeJSONObject.getJSONObject("Denome");
			
			denomeName = denomeObject.getString("Name");
			//
			// make sure it is garbage cllected
			//
			


			//
			// now parse them
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject;
			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					internalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					purposeNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					mnemosyneNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
					humanInterfaceNucleus= aJSONObject;
				}

			}
			if(internalNucleus==null){
				Hashtable info = new Hashtable();
				String m = "The internalNucleus was not found. Using: " + selectedDenomeFileName;
				info.put("message", m);
				throw new MissingDenomeException(info);
			}

			if(purposeNucleus==null){
				Hashtable info = new Hashtable();
				String m = "The purposeNucleus was not found. Using: " + selectedDenomeFileName;
				info.put("message", m);
				throw new MissingDenomeException(info);
			}
			//
			// Now check to see if there is a processing chain, (iuf the denome has not actuators there will not be) if so delete the processing denechain  DENECHAIN_ACTUATOR_LOGIC_PROCESSING
			// so that when this pulse is written to disk only the processing info
			// for this pulse is stored

			this.getDeneChainByName(denomeJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);


			JSONArray deneChainsPurpose = purposeNucleus.getJSONArray("DeneChains");
			JSONObject deneChain;


			//
			// to prune do this until there are no more processing logic
			logger.debug("before removing the processing logic, the number f denechains in purpose is =" +deneChainsPurpose.length());

			boolean keepGoing=true;
			boolean removed=false;
			while(keepGoing){
				removed=false;
				found:
					for(int i=0;i<deneChainsPurpose.length();i++){
						deneChain = deneChainsPurpose.getJSONObject(i);
						logger.debug("in load denome,deneChain=" + deneChain.getString("Name") + " size=" + deneChain.getJSONArray("Denes").length() );
						if(deneChain.getString("Name").equals( TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING)){
							deneChainsPurpose.remove(i);
							//logger.debug("removing o=" + o);
							removed=true;
							break found;
						}
					}
				if(!removed)keepGoing=false;
			}
			logger.debug("after removing the processing logic, the number f denechains in purpose is =" +deneChainsPurpose.length());





			// create the vector of sensors and actuators, you are storing the dene that 
			// represents the sensor or the actuator

			JSONArray internalNucleusDeneChains = (JSONArray) internalNucleus.get("DeneChains");
			JSONObject aDeneChainJSONObject, aDeneValueJSONObject;
			JSONArray sensorDenesJSONArray;
			JSONArray sensorValuesJSONArray;

			JSONArray onStartActionsDenesJSONArray, actuatorDenesJSONArray, descriptiveDenesJSONArray;
			Integer executionPosition=0;

			int evaluationPosition = 0;
			String aDeneJSONObject_Name;
			actuatorDeneNameActuatorActionEvaluationPositionActionIndex = new Hashtable();
			actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex = new Hashtable();
			new ArrayList(); 

			//
			// process the sensors and actuators
			//
			String actuatorDeneName = null;
			JSONObject aDeneJSONObject, actuatorDeneJSONObject, aDeneWordJSONObject;
			JSONArray vitalDeneWordsJsonArray;
			//
			// get the denechain int an index to process them in a specific order
			//
			Hashtable deneChainNameDeneChainIndex = new Hashtable();
			for(int i=0;i<internalNucleusDeneChains.length();i++){
				aDeneChainJSONObject = (JSONObject) internalNucleusDeneChains.get(i);
				name = aDeneChainJSONObject.getString("Name");
				deneChainNameDeneChainIndex.put(name, aDeneChainJSONObject);
				logger.debug("in denomemanager, adding denechain " + name);
			}
			logger.debug("complete dene chains");
			//for(int i=0;i<internalNucleusDeneChains.length();i++){
			//
			// process the chains in the order
			// descriptive
			// compoents
			// sensors
			// actuators
			// Analyticons
			//
			JSONObject aDescriptiveDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DESCRIPTIVE);
			descriptiveDenesJSONArray = (JSONArray)aDescriptiveDeneChainJSONObject.get("Denes");
			//
			// process the vital dene
			//
			for(int j=0;j<descriptiveDenesJSONArray.length();j++){
				aDeneJSONObject = (JSONObject) descriptiveDenesJSONArray.get(j);
				aDeneJSONObject_Name = aDeneJSONObject.getString("Name");
				if(aDeneJSONObject_Name.equals(TeleonomeConstants.DENE_VITAL)){
					vitalDeneWordsJsonArray = aDeneJSONObject.getJSONArray("DeneWords");
					for(int k=0;k<vitalDeneWordsJsonArray.length();k++){
						aDeneWordJSONObject = (JSONObject) vitalDeneWordsJsonArray.get(k);
						if(aDeneWordJSONObject.getString("Name").equals(TeleonomeConstants.VITAL_DENEWORD_BASE_PULSE_FREQUENCY)){
							basePulseFrequency = aDeneWordJSONObject.getInt("Value");
						}else if(aDeneWordJSONObject.getString("Name").equals(TeleonomeConstants.VITAL_DENEWORD_INTER_SENSOR_READ_TIMEOUT_MILLISECONDS)){
							this.interSensorReadTimeoutMilliseconds = aDeneWordJSONObject.getInt("Value");
						}else if(aDeneWordJSONObject.getString("Name").equals(TeleonomeConstants.VITAL_DENEWORD_TIMEZONE)){
							this.timeZone = aDeneWordJSONObject.getString("Value");
						}
					}
				}
			}
			//
			// process




			JSONObject aComponentsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_COMPONENTS);

			//
			// Cant assume that all teleonomes will have Compnents
			String pointerToMicroController;
			JSONObject microProcessorDene;
			Integer I;

			if(aComponentsDeneChainJSONObject!=null){
				//
				// get all the microcontrollers
				// sort them according to the procesisng position
				// and store them in microControllerPointerProcessingQueuePositionIndex 

				microProcessorsDenesJSONArray = DenomeUtils.getDenesByDeneType(aComponentsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);
				microControllerPointerProcessingQueuePositionIndex = new ArrayList();
				String microProcessorName, microControllerConfigParameterPointer, microControllerConfigParameterListPointer;
				JSONObject microControllerConfigParameterListDene, microControllerConfigParameterDeneWord;
				JSONArray microControllerParams, microControllerParamsPointers;
				logger.debug("microProcessorsDenesJSONArray.length()=" + microProcessorsDenesJSONArray.length());

				for(int m=0;m<microProcessorsDenesJSONArray.length();m++){
					microProcessorDene = microProcessorsDenesJSONArray.getJSONObject(m);
					microProcessorName = microProcessorDene.getString("Name");
					logger.debug("microProcessorName=" + microProcessorName);
					pointerToMicroController = "@" +  denomeName + ":" + TeleonomeConstants.NUCLEI_INTERNAL + ":" + TeleonomeConstants.DENECHAIN_COMPONENTS + ":" + microProcessorName;
					//
					// Process the MicroController Config Parameter
					//
					microControllerParams = new JSONArray();
					microControllerConfigParameterListPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(microProcessorDene, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER_CONFIG_PARAMETER_LIST, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("microControllerConfigParameterListPointer=" + microControllerConfigParameterListPointer);

					if(microControllerConfigParameterListPointer!=null && !microControllerConfigParameterListPointer.equals("")){
						try {
							//
							// get the dene that has the list
							logger.debug("microControllerConfigParameterListPointer=" + microControllerConfigParameterListPointer);
							microControllerConfigParameterListDene = this.getDeneByIdentity(new Identity(microControllerConfigParameterListPointer));
							//
							// get just the value for each deneword in the list, this is a pointer to the actual dene
							// 
							microControllerParamsPointers = getAllDeneWordAttributeByDeneWordTypeFromDene(microControllerConfigParameterListDene, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER_CONFIG_PARAMETER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							//
							// now loop over every pointer to get the dene
							//
							for(int n=0;n<microControllerParamsPointers.length();n++){
								microControllerConfigParameterPointer = microControllerParamsPointers.getString(n);
								logger.debug("microControllerConfigParameterPointer=" + microControllerConfigParameterPointer);
								microControllerConfigParameterDeneWord = getDeneByIdentity(new Identity(microControllerConfigParameterPointer));
								microControllerParams.put(microControllerConfigParameterDeneWord);
							}		
						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						microControllerNameMicroControllerParamsIndex.put(microProcessorName, microControllerParams);
					}

					//
					// Process the Queue Position
					//
					I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(microProcessorDene, "Processing Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("Processing Queue Position=" + I);
					microControllerPointerProcessingQueuePositionIndex.add(new AbstractMap.SimpleEntry<String, Integer>(pointerToMicroController, I));
					Collections.sort(microControllerPointerProcessingQueuePositionIndex, new Comparator<Map.Entry<?, Integer>>(){
						public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
							return o1.getValue().compareTo(o2.getValue());
						}});
				}

			}

			boolean isSensor=false;
			Vector v;
			JSONObject aSensorsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_SENSORS);
			//
			// Cant assume that all teleonomes will have sensors
			if(aSensorsDeneChainJSONObject!=null){

				sensorDenesJSONArray = getDenesByDeneType(aSensorsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_SENSOR);
				Hashtable pointerToMicroControllerSensorDenesVectorIndex = new Hashtable();
				for(int j=0;j<sensorDenesJSONArray.length();j++){
					aDeneJSONObject = (JSONObject) sensorDenesJSONArray.get(j);
					logger.info("line 474 aDeneJSONObject=" + aDeneJSONObject.getString("Name"));
					pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.info("line 476 pointerToMicroController=" + pointerToMicroController);
					v = (Vector)pointerToMicroControllerSensorDenesVectorIndex.get(pointerToMicroController);
					if(v==null)v = new Vector();
					v.addElement(aDeneJSONObject);
					pointerToMicroControllerSensorDenesVectorIndex.put(pointerToMicroController,v);
				}
				sensorDenesVector = new Vector();
				JSONArray sensorValuesPointersJSONArray;
				for(Enumeration<String> en = pointerToMicroControllerSensorDenesVectorIndex.keys();en.hasMoreElements();){
					pointerToMicroController = en.nextElement();
					v = (Vector)pointerToMicroControllerSensorDenesVectorIndex.get(pointerToMicroController);
					sensorRequestQueuePositionDeneWordIndex = new ArrayList();
					logger.info("line 488,pointerToMicroController=" + pointerToMicroController );
					for(int j=0;j<v.size();j++){
						aDeneJSONObject = (JSONObject) v.elementAt(j);
						isSensor = DenomeUtils.isDeneOfType(aDeneJSONObject, TeleonomeConstants.DENE_TYPE_SENSOR);
						logger.info("line 491 aDeneJSONObject.=" + aDeneJSONObject.getString("Name") + " isSensor="+isSensor);
						if(isSensor){
							sensorValuesPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

							sensorValuesJSONArray = this.loadDenesFromPointers(sensorValuesPointersJSONArray);


							logger.info("sensorValuesJSONArray.length=" + sensorValuesJSONArray.length());
							for(int k=0;k<sensorValuesJSONArray.length();k++){

								aDeneValueJSONObject = (JSONObject) sensorValuesJSONArray.get(k);
								logger.info("k="+ k + " aDeneValueJSONObject:" + aDeneValueJSONObject);

								I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneValueJSONObject, "Sensor Request Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

								sensorRequestQueuePositionDeneWordIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(aDeneValueJSONObject, I));
								Collections.sort(sensorRequestQueuePositionDeneWordIndex, new Comparator<Map.Entry<?, Integer>>(){
									public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
										return o1.getValue().compareTo(o2.getValue());
									}});
								logger.info("line 511, after sorting sensorRequestQueuePositionDeneWordIndex="+ sensorRequestQueuePositionDeneWordIndex.size() + " aDeneValueJSONObject:" + aDeneValueJSONObject.getString("Name"));

							}
						}else if(DenomeUtils.isDeneOfType(aDeneJSONObject, TeleonomeConstants.DENE_TYPE_ON_START_SENSOR)){
							sensorValuesPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

							sensorValuesJSONArray = this.loadDenesFromPointers(sensorValuesPointersJSONArray);
							

							for(int k=0;k<sensorValuesJSONArray.length();k++){
								aDeneValueJSONObject = (JSONObject) sensorValuesJSONArray.get(k);
								logger.info("line 521 aDeneJSONObject.=" + aDeneValueJSONObject.getString("Name"));

								I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneValueJSONObject, "Sensor Request Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								logger.debug("In denomemanager, aDeneValueJSONObject=" + aDeneValueJSONObject.getString("Name") + " Sensor Request Queue Position=" + I);

								sensorRequestQueuePositionDeneWordForInitialIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(aDeneValueJSONObject, I));
								Collections.sort(sensorRequestQueuePositionDeneWordForInitialIndex, new Comparator<Map.Entry<?, Integer>>(){
									public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
										return o1.getValue().compareTo(o2.getValue());
									}});

							}
						}


					}
					
					//
					// finishing running through the sensors of a microcontroller, so store it
					//
					logger.info("line 541");
					pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex.put(pointerToMicroController, sensorRequestQueuePositionDeneWordIndex);
					pointerToMicroControllerSensorsDeneWordsForInitialBySensorRequestQueuePositionIndex.put(pointerToMicroController, sensorRequestQueuePositionDeneWordForInitialIndex);
					logger.info("line 543 storing data for microcontrollerpointer: " + pointerToMicroController + " sensorRequestQueuePositionDeneWordIndex=" + sensorRequestQueuePositionDeneWordIndex.size() + " sensorRequestQueuePositionDeneWordForInitialIndex:" + sensorRequestQueuePositionDeneWordForInitialIndex.size());
				}
			}
			//
			// do the actuatorsinitial first
			//
			JSONObject anActuatorsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_ACTUATORS);


			//
			// Cant assume that all teleonomes will have actuators
			//
			JSONObject onStartActionJSONObject;
			String actionCodonName;
			String actionListDeneName;
			if(anActuatorsDeneChainJSONObject!=null){
				//
				// identify which actions should be executed on the start pulse
				// from those actions, get the codon to get the name of the actuator
				// with the actuat
				onStartActionsDenesJSONArray = getDenesByDeneType(anActuatorsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_ON_START_ACTION);

				Hashtable pointerToMicroControllerActuatorDenesVectorForInitialIndex = new Hashtable();
				for(int j=0;j<onStartActionsDenesJSONArray.length();j++){


					onStartActionJSONObject = (JSONObject) onStartActionsDenesJSONArray.getJSONObject(j);
					logger.debug("onStartActionJSONObject=" + onStartActionJSONObject);
					actionCodonName = (String)getDeneWordAttributeByDeneWordNameFromDene(onStartActionJSONObject,TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//
					// the codonName is also the name of the actuator that contains this action, so get the actuator
					actuatorDeneJSONObject = getDeneFromDeneChainByDeneName(anActuatorsDeneChainJSONObject,actionCodonName);
					logger.debug("line 628 actuatorDeneJSONObject=" + actuatorDeneJSONObject);

					pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(actuatorDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("line 631 pointerToMicroController=" + pointerToMicroController);
					v = (Vector)pointerToMicroControllerActuatorDenesVectorForInitialIndex.get(pointerToMicroController);
					if(v==null)v = new Vector();
					if(!v.contains(actuatorDeneJSONObject)) {
						v.addElement(actuatorDeneJSONObject);
						logger.debug("line 634 adding actuator dene to vector, size =" + v.size());
					}
					pointerToMicroControllerActuatorDenesVectorForInitialIndex.put(pointerToMicroController,v);
				}


				JSONObject actionListDene=null;

				for(Enumeration<String> en = pointerToMicroControllerActuatorDenesVectorForInitialIndex.keys();en.hasMoreElements();){
					pointerToMicroController = en.nextElement();
					v = (Vector)pointerToMicroControllerActuatorDenesVectorForInitialIndex.get(pointerToMicroController);
					actuatorExecutionPositionDeneForInitialIndex = new ArrayList();
					for(int j=0;j<v.size();j++){
						actuatorDeneJSONObject = (JSONObject) v.elementAt(j);
						actuatorDeneName = actuatorDeneJSONObject.getString("Name");
						executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorDeneJSONObject,"Execution Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("line 652 actuatorDeneName=" + actuatorDeneName + " executionPosition=" + executionPosition);
						if(executionPosition!=null && executionPosition>-1){
							//
							// The Dene that contains the executive position deneword also has a dene of type Action list,
							// the value of that deneword is a pointer to the dene that contains the action
							String actionListPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(actuatorDeneJSONObject,TeleonomeConstants.DENEWORD_TYPE_ON_START_ACTION_LIST, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							//
							// now use the pointer to get to the denes that contain the actions
							// th  pointer could be null
							logger.debug(" line 657, actionListPointer=" + actionListPointer);

							if(actionListPointer!=null){
								try {
									actionListDene = getDeneByIdentity(new Identity(actionListPointer));
								} catch (InvalidDenomeException e) {
									// TODO Auto-generated catch block
									logger.warn(Utils.getStringException(e));
								}
								logger.debug(" line 581, actionListDene=" + actionListDene+ " executionPosition=" + executionPosition);
								actuatorExecutionPositionDeneForInitialIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionListDene, new Integer(executionPosition)));
							}
						}
					}

					Collections.sort(actuatorExecutionPositionDeneForInitialIndex, new IntegerCompare());
					pointerToMicroControllerActuatorExecutionPositionForInitialDeneIndex.put(pointerToMicroController, actuatorExecutionPositionDeneForInitialIndex);



					// 
					//
					// at this point actuatorExecutionPositionDeneForInitialIndex contains the actuators sorted according to the execution position
					// next for every actuatordene sort its actions according to their evaluation position
					// so evaluate wheter or not to execute an action

					logger.debug("actuatorExecutionPositionDeneForInitialIndex=" + actuatorExecutionPositionDeneForInitialIndex.size());;
					for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneForInitialIndex) {
						actionListDene = entry.getKey();
						actionListDeneName = actionListDene.getString("Name");
						logger.debug(" line 689 actionListDeneName=" + actionListDeneName);
						//
						// the actionListDene contains denewords of type Dene Pointer which we need to resolve
						// the value of the evaluation pointer
						//
						// so first get the denewords of typeDene Pointer which will point to the dene that contains the evaluation position
						// the method returns a JSONArray of JSONObjects
						//
						//logger.debug("actionListDene=" + actionListDene);
						JSONArray actionDeneWordPointers = (JSONArray) DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actionListDene, TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE,TeleonomeConstants.DENEWORD_TYPE_ACTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug(" line 689 actionDeneWordPointers=" + actionDeneWordPointers.length());


						//
						// actionDeneWordPointers cntains an array of string which are pointers to the denes that contain the evaluation postion
						String denePointer;
						JSONObject actionDene = null;
						ArrayList<Map.Entry<JSONObject, Integer>>  actuatorActionEvaluationPositionActionIndex = new ArrayList(); 
						for(int n=0;n<actionDeneWordPointers.length();n++){
							denePointer = (String)actionDeneWordPointers.getString(n);
							try {
								actionDene = getDeneByIdentity(new Identity(denePointer));
								evaluationPosition = (Integer)getDeneWordAttributeByDeneWordNameFromDenePointer( denePointer, TeleonomeConstants.EVALUATION_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);  
								actuatorActionEvaluationPositionActionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionDene, new Integer(evaluationPosition)));
								//
								// Now store this actuatorActionEvaluationPositionActionIndex which contains the order and what actions to
								// execute FOR A SPECIFICACTUATOR into a Vector whcih will maintain the order of execution of the
								// actuators.  this is critical because every actuator has an action with evaluation position eual to 1

							} catch (InvalidDenomeException e) {
								// TODO Auto-generated catch block
								logger.warn(Utils.getStringException(e));
							}

						}


						//
						// now sort the actions
						//
						Collections.sort(actuatorActionEvaluationPositionActionIndex, new IntegerCompare());
						//
						// we are inside of the loop processing the actuators in the correct order and
						// actuatorActionEvaluationPositionActionIndex contains the action denes
						// in the correct order, so create a list that contains the actuatorName and the actions to be executed
						///
						logger.debug("line 737, actionListDeneName=" + actionListDeneName + " actuatorActionEvaluationPositionActionIndex=" + actuatorActionEvaluationPositionActionIndex);
						actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex.put(actionListDeneName, actuatorActionEvaluationPositionActionIndex);
					}
				}
				//
				// then the normal
				//
				actuatorDenesJSONArray = getDenesByDeneType(anActuatorsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_ACTUATOR);

				Hashtable pointerToMicroControllerActuatorDenesVectorIndex = new Hashtable();
				for(int j=0;j<actuatorDenesJSONArray.length();j++){
					aDeneJSONObject = (JSONObject) actuatorDenesJSONArray.getJSONObject(j);
					pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					v = (Vector)pointerToMicroControllerActuatorDenesVectorIndex.get(pointerToMicroController);
					if(v==null)v = new Vector();
					v.addElement(aDeneJSONObject);
					pointerToMicroControllerActuatorDenesVectorIndex.put(pointerToMicroController,v);
				}


				actionListDene=null;

				for(Enumeration<String> en = pointerToMicroControllerActuatorDenesVectorIndex.keys();en.hasMoreElements();){
					pointerToMicroController = en.nextElement();
					logger.debug("line 779 pointerToMicroController=" + pointerToMicroController);
					v = (Vector)pointerToMicroControllerActuatorDenesVectorIndex.get(pointerToMicroController);
					actuatorExecutionPositionDeneIndex = new ArrayList();
					for(int j=0;j<v.size();j++){
						aDeneJSONObject = (JSONObject) v.elementAt(j);
						actuatorDeneName = aDeneJSONObject.getString("Name");
						executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneJSONObject,"Execution Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug(" line 786 actuatorDeneName=" + actuatorDeneName+ " executionPosition=" + executionPosition);
						if(executionPosition!=null && executionPosition>-1){
							//
							// The Dene that contains the executive position deneword also has a dene of type Action list,
							// the value of that deneword is a pointer to the dene that contains the action
							String actionListPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject,TeleonomeConstants.DENE_TYPE_ACTION_LIST, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							//
							// now use the pointer to get to the denes that contain the actions
							//
							logger.debug("actionListPointer=" + actionListPointer);
							//
							// if the actuators are only for startup this could be null
							if(actionListPointer!=null){
								try {
									actionListDene = getDeneByIdentity(new Identity(actionListPointer));
									logger.debug("line 801 actionListDene=" + actionListDene.toString(4));
								} catch (InvalidDenomeException e) {
									// TODO Auto-generated catch block
									logger.warn(Utils.getStringException(e));
								}
								actuatorExecutionPositionDeneIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionListDene, new Integer(executionPosition)));
							}
						}
					}

					Collections.sort(actuatorExecutionPositionDeneIndex, new IntegerCompare());
					pointerToMicroControllerActuatorExecutionPositionDeneIndex.put(pointerToMicroController, actuatorExecutionPositionDeneIndex);
					logger.debug("line 813 , pointerToMicroController=" + pointerToMicroController +" actuatorExecutionPositionDeneIndex size="+ actuatorExecutionPositionDeneIndex.size());
					// 
					//
					// at this point actuatorExecutionPositionDeneWordIndex contains the actuators sorted according to the execution position
					// next for every actuatordene sort its actions according to their evaluation position
					// so evaluate wheter or not to execute an action
					for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneIndex) {

						actionListDene = entry.getKey();
						actionListDeneName = actionListDene.getString("Name");
						logger.debug("line 823 , actionListDeneName=" + actionListDeneName);

						//
						// the actionListDene contains denewords of type Dene Pointer which we need to resolve
						// the value of the evaluation pointer
						//
						// so first get the denewords of typeDene Pointer which will point to the dene that contains the evaluation position
						// the method returns a JSONArray of JSONObjects
						//
						//logger.debug("actionListDene=" + actionListDene);
						JSONArray actionDeneWordPointers = (JSONArray) DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actionListDene, TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE,TeleonomeConstants.DENEWORD_TYPE_ACTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


						//
						// actionDeneWordPointers cntains an array of string which are pointers to the denes that contain the evaluation postion
						String denePointer;
						JSONObject actionDene = null;
						ArrayList<Map.Entry<JSONObject, Integer>>  actuatorActionEvaluationPositionActionIndex = new ArrayList(); 
						for(int n=0;n<actionDeneWordPointers.length();n++){
							denePointer = (String)actionDeneWordPointers.getString(n);
							try {
								actionDene = getDeneByIdentity(new Identity(denePointer));
								evaluationPosition = (Integer)getDeneWordAttributeByDeneWordNameFromDenePointer( denePointer, "Evaluation Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);  
								actuatorActionEvaluationPositionActionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionDene, new Integer(evaluationPosition)));
								//
								// Now store this actuatorActionEvaluationPositionActionIndex which contains the order and what actions to
								// execute FOR A SPECIFICACTUATOR into a Vector whcih will maintain the order of execution of the
								// actuators.  this is critical because every actuator has an action with evaluation position eual to 1

							} catch (InvalidDenomeException e) {
								// TODO Auto-generated catch block
								logger.warn(Utils.getStringException(e));
							}

						}


						//
						// now sort the actions
						//
						Collections.sort(actuatorActionEvaluationPositionActionIndex, new IntegerCompare());
						//
						// we are inside of the loop processing the actuators in the correct order and
						// actuatorActionEvaluationPositionActionIndex contains the action denes
						// in the correct order, so create a list that contains the actuatorName and the actions to be executed
						///
						logger.debug("line 869 , actionListDeneName=" + actionListDeneName + " actuatorActionEvaluationPositionActionIndex.size()=" + actuatorActionEvaluationPositionActionIndex.size());
						actuatorDeneNameActuatorActionEvaluationPositionActionIndex.put(actionListDeneName, actuatorActionEvaluationPositionActionIndex);
					}
				}
			}





			//
			// End of actuator dene chain processing
			//
			//mnemosyconExecutionPositionDeneIndex
			//
			// begin processing analyticons
			//
			//
			// Cant assume that all teleonomes will have analyticons
			//
			if(deneChainNameDeneChainIndex.containsKey(TeleonomeConstants.DENECHAIN_ANALYTICONS)) {
				JSONObject anAnalyticonsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_ANALYTICONS);
				//logger.debug("in denomemagager anAnalyticonsDeneChainJSONObject= " + anAnalyticonsDeneChainJSONObject);
				if(anAnalyticonsDeneChainJSONObject!=null){
					analyticonDenesJSONArray = DenomeUtils.getDenesByDeneType(anAnalyticonsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_ANALYTYCON);
				}
			}
			//logger.debug("line 858 deneChainNameDeneChainIndex=" + deneChainNameDeneChainIndex);
			//
			// end of processing analyticons
			//
			// process the mnemosycons
			// cant assumew all teleonome have mnemosycons
			//
			if(deneChainNameDeneChainIndex.containsKey(TeleonomeConstants.DENECHAIN_MNEMOSYCONS)) {

				// then get all the denes of type DENE_TYPE_MNEMOSYCON_DENEWORDS_TO_REMEMBER 
				//
				// Hashtable<String,ArrayList> deneWordsToRememberByTeleonome

				deneWordsToRememberByTeleonome = new Hashtable();
				denesToRememberByTeleonome = new Hashtable();
				deneChainsToRememberByTeleonome = new Hashtable();

				JSONObject rememberedWordsMnemosyconJSONObject;
				boolean active=false;
				JSONArray rememberedDeneWordsJSONArray,rememberedDenesJSONArray, rememberedDeneChainsJSONArray;
				String rememberedDeneWordTeleonomeName,rememberedDeneTeleonomeName, rememberedDeneChainTeleonomeName,rememberedDeneWordPointer, rememberedDeneChainPointer, rememberedDenePointer;
				Identity rememberedDeneWordIdentity, rememberedDeneIdentity, rememberedDeneChainIdentity;
				ArrayList teleonomeRememeberedWordsArrayList, teleonomeRememberedDenesArrayList, teleonomeRememberedDeneChainsArrayList;

				JSONObject anMnemosyconsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_MNEMOSYCONS);
				logger.debug("in denomemagager anMnemosyconsDeneChainJSONObject= " + anMnemosyconsDeneChainJSONObject);
				if(anMnemosyconsDeneChainJSONObject!=null){
					mnemosyconDenesJSONArray = getDenesByDeneType(anMnemosyconsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_MNEMOSYCON);
					rememeberedDeneWordsMnemosyconDenesJSONArray = getDenesByDeneType(anMnemosyconsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_MNEMOSYCON_DENEWORDS_TO_REMEMBER);
					for(int i=0;i<rememeberedDeneWordsMnemosyconDenesJSONArray.length();i++) {
						rememberedWordsMnemosyconJSONObject = rememeberedDeneWordsMnemosyconDenesJSONArray.getJSONObject(i);
						active = (boolean) this.getDeneWordAttributeByDeneWordNameFromDene(rememberedWordsMnemosyconJSONObject, TeleonomeConstants.DENEWORD_ACTIVE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(active) {
							//
							// the denechainstoremember
							//
							rememberedDeneChainsJSONArray = getAllDeneWordAttributeByDeneWordTypeFromDene(rememberedWordsMnemosyconJSONObject, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENECHAIN, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.debug("rememberedDeneChainsJSONArray= " + rememberedDeneChainsJSONArray);

							//
							// this array will contain elements that are actually pointers, 
							//
							// "@Tlaloc:Purpose:Sensor Data:Solar Radiation:Solar Radiation Data","@Tlaloc:Purpose:Sensor Data:Ambient Temperature:Ambient Temperature Data"
							//
							// get the name of the teleonome and use to get the vector of all the other remembered words, and stored the identity in the vector

							if(rememberedDeneChainsJSONArray!=null && rememberedDeneChainsJSONArray.length()>0) {
								for(int j=0;j<rememberedDeneChainsJSONArray.length();j++) {
									rememberedDeneChainPointer= rememberedDeneChainsJSONArray.getString(j);

									rememberedDeneChainIdentity = new Identity(rememberedDeneChainPointer);
									rememberedDeneChainTeleonomeName = rememberedDeneChainIdentity.getTeleonomeName();

									logger.debug("rememberedDeneChainTeleonomeName=" + rememberedDeneChainTeleonomeName + " rememberedDeneChainPointer= " + rememberedDeneChainPointer);

									teleonomeRememberedDeneChainsArrayList = deneChainsToRememberByTeleonome.get(rememberedDeneChainTeleonomeName);
									if(teleonomeRememberedDeneChainsArrayList==null)teleonomeRememberedDeneChainsArrayList = new ArrayList();
									teleonomeRememberedDeneChainsArrayList.add(rememberedDeneChainPointer);
									logger.debug("adding to remembered denechains= " + rememberedDeneChainPointer);
									deneChainsToRememberByTeleonome.put(rememberedDeneChainTeleonomeName, teleonomeRememberedDeneChainsArrayList);

								}
							}
							//
							// the denestoremember
							//
							rememberedDenesJSONArray = getAllDeneWordAttributeByDeneWordTypeFromDene(rememberedWordsMnemosyconJSONObject, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.debug("rememberedDeneChainsJSONArray= " + rememberedDeneChainsJSONArray);

							if(rememberedDenesJSONArray!=null && rememberedDenesJSONArray.length()>0) {
								for(int j=0;j<rememberedDenesJSONArray.length();j++) {
									rememberedDenePointer= rememberedDenesJSONArray.getString(j);

									rememberedDeneIdentity = new Identity(rememberedDenePointer);
									rememberedDeneTeleonomeName = rememberedDeneIdentity.getTeleonomeName();

									logger.debug("rememberedDeneTeleonomeName=" + rememberedDeneTeleonomeName + " rememberedDenePointer= " + rememberedDenePointer);

									teleonomeRememberedDenesArrayList = denesToRememberByTeleonome.get(rememberedDeneTeleonomeName);
									if(teleonomeRememberedDenesArrayList==null)teleonomeRememberedDenesArrayList = new ArrayList();
									teleonomeRememberedDenesArrayList.add(rememberedDenePointer);
									logger.debug("adding to remembered dene= " + rememberedDenePointer);
									denesToRememberByTeleonome.put(rememberedDeneTeleonomeName, teleonomeRememberedDenesArrayList);

								}
							}
							//
							// the denewords to remember
							//
							rememberedDeneWordsJSONArray = getAllDeneWordAttributeByDeneWordTypeFromDene(rememberedWordsMnemosyconJSONObject, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENEWORD, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.debug("rememberedDeneWordsJSONArray= " + rememberedDeneWordsJSONArray);

							//
							// this array will contain elements that are actually pointers, 
							//
							// "@Tlaloc:Purpose:Sensor Data:Solar Radiation:Solar Radiation Data","@Tlaloc:Purpose:Sensor Data:Ambient Temperature:Ambient Temperature Data"
							//
							// get the name of the teleonome and use to get the vector of all the other remembered words, and stored the identity in the vector
							for(int j=0;j<rememberedDeneWordsJSONArray.length();j++) {
								rememberedDeneWordPointer= rememberedDeneWordsJSONArray.getString(j);

								rememberedDeneWordIdentity = new Identity(rememberedDeneWordPointer);
								rememberedDeneWordTeleonomeName = rememberedDeneWordIdentity.getTeleonomeName();

								logger.debug("rememberedDeneWordTeleonomeName=" + rememberedDeneWordTeleonomeName + " rememberedDeneWordPointer= " + rememberedDeneWordPointer);

								teleonomeRememeberedWordsArrayList = deneWordsToRememberByTeleonome.get(rememberedDeneWordTeleonomeName);
								if(teleonomeRememeberedWordsArrayList==null)teleonomeRememeberedWordsArrayList = new ArrayList();
								teleonomeRememeberedWordsArrayList.add(rememberedDeneWordPointer);
								logger.debug("adding to remembered denewords= " + rememberedDeneWordPointer);
								deneWordsToRememberByTeleonome.put(rememberedDeneWordTeleonomeName, teleonomeRememeberedWordsArrayList);

							}
						}

					}
				}

			}

			//
			// process the purpose nucleus
			//
			JSONArray purposeNucleusDeneChains = (JSONArray) purposeNucleus.get("DeneChains");
			JSONArray externalDataDeneWordsJSONArray;
			JSONObject externalDataDeneJSONObject,externalDeneWordJSONObject;
			String dataLocation, sourceTeleonomeName;
			externalDataNameDeneWords = new Hashtable();
			String deneType;
			externalDataLocationHashMap = new HashMap();
			Identity dataLocationIdentity=null;
			ArrayList externalDataLocations=null;
			for(int i=0;i<purposeNucleusDeneChains.length();i++){
				aDeneChainJSONObject = (JSONObject) purposeNucleusDeneChains.get(i);

				if(aDeneChainJSONObject.has("Name") && aDeneChainJSONObject.getString("Name").equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)){
					JSONArray externalDataDenes = aDeneChainJSONObject.getJSONArray("Denes");
					for(int j=0;j<externalDataDenes.length();j++){
						//
						// check to see if this dene is of type Visualization Info,
						// if so skip it

						externalDataDeneJSONObject = (JSONObject) externalDataDenes.get(j);
						deneType = externalDataDeneJSONObject.getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE);
						if(deneType.equals(TeleonomeConstants.DENE_TYPE_EXTERNAL_DATA_SOURCE)){	
							sourceTeleonomeName = externalDataDeneJSONObject.getString("Name");
							externalDataDeneWordsJSONArray  = externalDataDeneJSONObject.getJSONArray("DeneWords");
							externalDataNameDeneWords.put(sourceTeleonomeName, externalDataDeneWordsJSONArray);
							for(int k=0;k<externalDataDeneWordsJSONArray.length();k++){
								externalDeneWordJSONObject = (JSONObject) externalDataDeneWordsJSONArray.get(k);
								logger.debug("line 929, externalDeneWordJSONObject=" + externalDeneWordJSONObject.getString("Name") );
								if(externalDeneWordJSONObject.has("Data Location")) {
									dataLocation = (String) externalDeneWordJSONObject.getString("Data Location");
									dataLocationIdentity = new Identity(dataLocation);
									logger.debug("line 933, for external teleonome =" + dataLocationIdentity.getTeleonomeName() + " adding "+ dataLocation );

									externalDataLocations = (ArrayList) externalDataLocationHashMap.get(dataLocationIdentity.getTeleonomeName());
									if(externalDataLocations==null)externalDataLocations = new ArrayList();
									if(!externalDataLocations.contains(dataLocation))externalDataLocations.add(dataLocation);
									externalDataLocationHashMap.put(dataLocationIdentity.getTeleonomeName(), externalDataLocations);
								}


							}

						}
					}
				}
			}



		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();
			logger.warn(Utils.getStringException(e));

			String m = "The denome file was not formated properly.  Path: " + selectedDenomeFileName + " Error:" + e.getMessage() + "\rStacktrace:";
			logger.debug(m);
			info.put("message", m);
			throw new MissingDenomeException(info);
		}
    }
	
	public JSONObject getDeneFromDeneChainByDeneName(JSONObject deneChain, String deneName) throws JSONException{
		JSONArray denesJSONArray = deneChain.getJSONArray("Denes");
		logger.debug("getDeneFromDeneChainByDeneName point 2, deneName=" + deneName + " deneChain=" + deneChain.getString("Name"));
		JSONObject aDeneJSONObject;
		for(int j=0;j<denesJSONArray.length();j++){
			aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
			//logger.debug("getdenebyidentity point3 " + aDeneJSONObject);

			if(aDeneJSONObject.getString("Name").equals(deneName)){
				return aDeneJSONObject;
			}
		}
		return null;
	}
	
	public Object getDeneWordAttributeByDeneWordNameFromDene(JSONObject deneJSONObject , String name, String whatToBring) throws JSONException{
		//logger.debug("getDeneWordAttributeByDeneWordNameFromDene, name=" + name + " deneJSONObject=" + deneJSONObject);
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			String deneWordName = deneWord.getString("Name");
			//logger.debug("getByName, deneWordName=" + deneWordName );

			if(deneWordName.equals(name)){
				if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
					return deneWord;
				}else{
					return deneWord.get(whatToBring);
				}

			}
		}
		return null;
	}
	// **********************************************
	
	
	
	
	
	public void loadDenomeOriginal(JSONObject denomeJSONObject) throws MissingDenomeException, TeleonomeValidationException{
		//
		// read the denome from the hard disk
		//  if its not found, then read it from the db
		//



		try {
			//
			// initialize the variables so as to not duplicate
			//


			//
			// end of variable initialization
			//

			
			denomeObject = denomeJSONObject.getJSONObject("Denome");
			logger.debug("line 217 just read the denomeObject, length=" + denomeObject.toString().length());
			String denomeName = denomeObject.getString("Name");
			//
			// now parse them
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject;
			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					internalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					purposeNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					mnemosyneNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
					humanInterfaceNucleus= aJSONObject;
				}

			}
			if(internalNucleus==null){
				Hashtable info = new Hashtable();
				String m = "The internalNucleus was not found. Using: " + selectedDenomeFileName;
				info.put("message", m);
				throw new MissingDenomeException(info);
			}

			if(purposeNucleus==null){
				Hashtable info = new Hashtable();
				String m = "The purposeNucleus was not found. Using: " + selectedDenomeFileName;
				info.put("message", m);
				throw new MissingDenomeException(info);
			}


			JSONArray internalNucleusDeneChains = (JSONArray) internalNucleus.get("DeneChains");
			JSONObject aDeneChainJSONObject;
			for(int i=0;i<internalNucleusDeneChains.length();i++){
				aDeneChainJSONObject = (JSONObject) internalNucleusDeneChains.get(i);
				name = aDeneChainJSONObject.getString("Name");
				deneChainNameDeneChainIndex.put(name, aDeneChainJSONObject);
			}

			//
			// Process the structures ready for reporting
			//

			JSONObject aComponentsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_COMPONENTS);
			JSONObject aDeneJSONObject, aDeneValueJSONObject;
			//
			// get all the microcontrollers
			// sort them according to the procesisng position
			// and store them in microControllerPointerProcessingQueuePositionIndex 
			String pointerToMicroController=null;
			Vector v;
			if(aComponentsDeneChainJSONObject!=null){
				JSONArray microProcessorsDenesJSONArray = DenomeUtils.getDenesByDeneType(aComponentsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);
				microControllerPointerProcessingQueuePositionIndex = new ArrayList();
				JSONObject microProcessorDene;
				Integer I;
				for(int m=0;m<microProcessorsDenesJSONArray.length();m++){
					microProcessorDene = microProcessorsDenesJSONArray.getJSONObject(m);
					pointerToMicroController = "@" +  denomeName + ":" + TeleonomeConstants.NUCLEI_INTERNAL + ":" + TeleonomeConstants.DENECHAIN_COMPONENTS + ":" + microProcessorDene.getString("Name");
					I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(microProcessorDene, "Processing Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					microControllerPointerProcessingQueuePositionIndex.add(new AbstractMap.SimpleEntry<String, Integer>(pointerToMicroController, I));
					Collections.sort(microControllerPointerProcessingQueuePositionIndex, new Comparator<Map.Entry<?, Integer>>(){
						public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
							return o1.getValue().compareTo(o2.getValue());
						}});
				}


				boolean isSensor=false;
				JSONObject aSensorsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_SENSORS);
				if(aSensorsDeneChainJSONObject!=null){
					JSONArray sensorDenesJSONArray = getDenesByDeneType(aSensorsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_SENSOR);
					if(sensorDenesJSONArray!=null && sensorDenesJSONArray.length()>0){
					
					pointerToMicroControllerSensorDenesVectorIndex = new Hashtable();
					for(int j=0;j<sensorDenesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) sensorDenesJSONArray.get(j);
						pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						v = (Vector)pointerToMicroControllerSensorDenesVectorIndex.get(pointerToMicroController);
						if(v==null)v = new Vector();
						v.addElement(aDeneJSONObject);
						pointerToMicroControllerSensorDenesVectorIndex.put(pointerToMicroController,v);
					}
					Vector sensorDenesVector = new Vector();
					JSONArray sensorValuesJSONArray;
					JSONArray sensorValuesPointersJSONArray;
					for(Enumeration<String> en = pointerToMicroControllerSensorDenesVectorIndex.keys();en.hasMoreElements();){
						pointerToMicroController = en.nextElement();
						v = (Vector)pointerToMicroControllerSensorDenesVectorIndex.get(pointerToMicroController);

						for(int j=0;j<v.size();j++){
							aDeneJSONObject = (JSONObject) v.elementAt(j);
							isSensor = DenomeUtils.isDeneOfType(aDeneJSONObject, TeleonomeConstants.DENE_TYPE_SENSOR);

							if(isSensor){
								//
								// this is a sensor so store it

								sensorValuesPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								sensorValuesJSONArray = this.loadDenesFromPointers(sensorValuesPointersJSONArray);
								//logger.debug("sensorValuesPointersJSONArray=" + sensorValuesPointersJSONArray + " sensorValuesJSONArray=" + sensorValuesJSONArray);
								
								if(sensorValuesJSONArray.length()>0){	

									for(int k=0;k<sensorValuesJSONArray.length();k++){
										aDeneValueJSONObject = (JSONObject) sensorValuesJSONArray.get(k);

										I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneValueJSONObject, "Sensor Request Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

										sensorRequestQueuePositionDeneWordIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(aDeneValueJSONObject, I));
										Collections.sort(sensorRequestQueuePositionDeneWordIndex, new Comparator<Map.Entry<?, Integer>>(){
											public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
												return o1.getValue().compareTo(o2.getValue());
											}});

									}
								}
							}

						}
					}
					//
					// finishing running through the sensors of a microcontroller, so store it
					//
					pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex.put(pointerToMicroController, sensorRequestQueuePositionDeneWordIndex);
				}
				}

				JSONObject anActuatorsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_ACTUATORS);
				JSONArray actuatorDenesJSONArray = getDenesByDeneType(anActuatorsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_ACTUATOR);



				JSONArray onStartActionsDenesJSONArray = getDenesByDeneType(anActuatorsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_ON_START_ACTION);
				JSONObject onStartActionJSONObject, actuatorDeneJSONObject;
				String actionCodonName;
				Integer evaluationPosition, executionPosition;
				String actuatorDeneName;

				Hashtable pointerToMicroControllerActuatorDenesVectorForInitialIndex = new Hashtable();
				for(int j=0;j<onStartActionsDenesJSONArray.length();j++){


					onStartActionJSONObject = (JSONObject) onStartActionsDenesJSONArray.getJSONObject(j);
					logger.debug("onStartActionJSONObject=" + onStartActionJSONObject);
					actionCodonName = (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(onStartActionJSONObject,TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//
					// the codonName is also the name of the actuator that contains this action, so get the actuator
					actuatorDeneJSONObject = DenomeUtils.getDeneFromDeneChainByDeneName(anActuatorsDeneChainJSONObject,actionCodonName);
					logger.debug("actuatorDeneJSONObject=" + actuatorDeneJSONObject);
					if(actuatorDeneJSONObject!=null){
						pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(actuatorDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("pointerToMicroController=" + pointerToMicroController);
						v = (Vector)pointerToMicroControllerActuatorDenesVectorForInitialIndex.get(pointerToMicroController);
						if(v==null)v = new Vector();
						v.addElement(actuatorDeneJSONObject);
						pointerToMicroControllerActuatorDenesVectorForInitialIndex.put(pointerToMicroController,v);
					}
				}


				JSONObject actionListDene=null;

				for(Enumeration<String> en = pointerToMicroControllerActuatorDenesVectorForInitialIndex.keys();en.hasMoreElements();){
					pointerToMicroController = en.nextElement();
					v = (Vector)pointerToMicroControllerActuatorDenesVectorForInitialIndex.get(pointerToMicroController);

					for(int j=0;j<v.size();j++){
						aDeneJSONObject = (JSONObject) v.elementAt(j);
						actuatorDeneName = aDeneJSONObject.getString("Name");
						executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneJSONObject,"Execution Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(executionPosition!=null && executionPosition>-1){
							//
							// The Dene that contains the executive position deneword also has a dene of type Action list,
							// the value of that deneword is a pointer to the dene that contains the action
							String actionListPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject,TeleonomeConstants.DENEWORD_TYPE_ON_START_ACTION_LIST, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							//
							// now use the pointer to get to the denes that contain the actions
							// th  pointer could be null
							if(actionListPointer!=null){
								try {
									actionListDene = getDeneByIdentity(new Identity(actionListPointer));
								} catch (InvalidDenomeException e) {
									// TODO Auto-generated catch block
									logger.debug(Utils.getStringException(e));
								}

								actuatorExecutionPositionDeneForInitialIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionListDene, new Integer(executionPosition)));
							}
						}
					}

					Collections.sort(actuatorExecutionPositionDeneForInitialIndex, new IntegerCompare());
					pointerToMicroControllerActuatorExecutionPositionForInitialDeneIndex.put(pointerToMicroController, actuatorExecutionPositionDeneForInitialIndex);
				}


				// 
				//
				// at this point actuatorExecutionPositionDeneForInitialIndex contains the actuators sorted according to the execution position
				// next for every actuatordene sort its actions according to their evaluation position
				// so evaluate wheter or not to execute an action
				String actionListDeneName;
				for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneForInitialIndex) {

					actionListDene = entry.getKey();
					actionListDeneName = actionListDene.getString("Name");
					//
					// the actionListDene contains denewords of type Dene Pointer which we need to resolve
					// the value of the evaluation pointer
					//
					// so first get the denewords of typeDene Pointer which will point to the dene that contains the evaluation position
					// the method returns a JSONArray of JSONObjects
					//
					//logger.debug("actionListDene=" + actionListDene);
					JSONArray actionDeneWordPointers = (JSONArray) DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actionListDene, TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE,TeleonomeConstants.DENEWORD_TYPE_ACTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


					//
					// actionDeneWordPointers cntains an array of string which are pointers to the denes that contain the evaluation postion
					String denePointer;
					JSONObject actionDene = null;
					ArrayList<Map.Entry<JSONObject, Integer>>  actuatorActionEvaluationPositionActionIndex = new ArrayList(); 
					for(int n=0;n<actionDeneWordPointers.length();n++){
						denePointer = (String)actionDeneWordPointers.getString(n);
						try {
							actionDene = getDeneByIdentity(new Identity(denePointer));
							evaluationPosition = (Integer)getDeneWordAttributeByDeneWordNameFromDenePointer( denePointer, "Evaluation Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);  
							actuatorActionEvaluationPositionActionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionDene, new Integer(evaluationPosition)));
							//
							// Now store this actuatorActionEvaluationPositionActionIndex which contains the order and what actions to
							// execute FOR A SPECIFICACTUATOR into a Vector whcih will maintain the order of execution of the
							// actuators.  this is critical because every actuator has an action with evaluation position eual to 1

						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.debug(Utils.getStringException(e));
						}

					}


					//
					// now sort the actions
					//
					Collections.sort(actuatorActionEvaluationPositionActionIndex, new IntegerCompare());
					//
					// we are inside of the loop processing the actuators in the correct order and
					// actuatorActionEvaluationPositionActionIndex contains the action denes
					// in the correct order, so create a list that contains the actuatorName and the actions to be executed
					///

					actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex.put(actionListDeneName, actuatorActionEvaluationPositionActionIndex);
				}




				Hashtable pointerToMicroControllerActuatorDenesVectorIndex = new Hashtable();
				for(int j=0;j<actuatorDenesJSONArray.length();j++){
					aDeneJSONObject = (JSONObject) actuatorDenesJSONArray.getJSONObject(j);
					pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					v = (Vector)pointerToMicroControllerActuatorDenesVectorIndex.get(pointerToMicroController);
					if(v==null)v = new Vector();
					v.addElement(aDeneJSONObject);
					pointerToMicroControllerActuatorDenesVectorIndex.put(pointerToMicroController,v);
				}


				JSONObject anActuatorDeneJSONObject;

				for(Enumeration<String> en = pointerToMicroControllerActuatorDenesVectorIndex.keys();en.hasMoreElements();){
					pointerToMicroController = en.nextElement();
					v = (Vector)pointerToMicroControllerActuatorDenesVectorIndex.get(pointerToMicroController);
					actuatorExecutionPositionDeneIndex=new ArrayList();
					for(int j=0;j<v.size();j++){
						anActuatorDeneJSONObject = (JSONObject) v.elementAt(j);
						actuatorDeneName = anActuatorDeneJSONObject.getString("Name");
						executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(anActuatorDeneJSONObject,"Execution Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(executionPosition!=null && executionPosition>-1){
							//
							// The Dene that contains the executive position deneword also has a dene of type Action list,
							// the value of that deneword is a pointer to the dene that contains the action
							//logger.debug(" aDeneJSONObject=" + anActuatorDeneJSONObject);
							//logger.debug("anActuatorDeneJSONObject=" + anActuatorDeneJSONObject);
							String actionListPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(anActuatorDeneJSONObject,TeleonomeConstants.DENE_TYPE_ACTION_LIST, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							//
							// now use the pointer to get to the denes that contain the actions
							//
							if(actionListPointer!=null){
								try {
									logger.debug("actionListPointer=" + actionListPointer);
									actionListDene = getDeneByIdentity(new Identity(actionListPointer));
								} catch (InvalidDenomeException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								if(actionListDene==null) {
									Hashtable info = new Hashtable();
									String m = "The denome file was not formated properly.  actionListPointer is missing: " ;
									
									
									
									info.put("ProblemIdentity", actionListPointer);
									info.put("Error Title",m);
									info.put("CurrentValue", actionListPointer);
									
									throw new TeleonomeValidationException(info);
								}
							}
							actuatorExecutionPositionDeneIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionListDene, new Integer(executionPosition)));
						}
					}

					Collections.sort(actuatorExecutionPositionDeneIndex, new IntegerCompare());
					pointerToMicroControllerActuatorExecutionPositionDeneIndex.put(pointerToMicroController, actuatorExecutionPositionDeneIndex);

				}




				// 
				//
				// at this point actuatorExecutionPositionDeneWordIndex contains the actuators sorted according to the execution position
				// next for every actuatordene sort its actions according to their evaluation position
				// so evaluate wheter or not to execute an action
				for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneIndex) {

					actionListDene = entry.getKey();
					//logger.debug("k=" + actionListDene + " v=" + entry.getValue() );
					
					
					actionListDeneName = actionListDene.getString("Name");
					//
					// the actionListDene contains denewords of type Dene Pointer which we need to resolve
					// the value of the evaluation pointer
					//
					// so first get the denewords of typeDene Pointer which will point to the dene that contains the evaluation position
					// the method returns a JSONArray of JSONObjects
					//
					JSONArray actionDeneWordPointers = (JSONArray) DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actionListDene, TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE,TeleonomeConstants.DENEWORD_TYPE_ACTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


					//@Ra:Internal:Actuators:Aggergate Generator Run Time
					// actionDeneWordPointers cntains an array of string which are pointers to the denes that contain the evaluation postion
					String denePointer;
					JSONObject actionDene = null;
					ArrayList<Map.Entry<JSONObject, Integer>>  actuatorActionEvaluationPositionActionIndex = new ArrayList(); 
					for(int n=0;n<actionDeneWordPointers.length();n++){
						denePointer = (String)actionDeneWordPointers.getString(n);
						try {
							actionDene = getDeneByIdentity(new Identity(denePointer));
							logger.debug("line 577 denePointer=" + denePointer + " actionDene=" + actionDene);
							evaluationPosition = (Integer)getDeneWordAttributeByDeneWordNameFromDenePointer( denePointer, "Evaluation Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);  
							actuatorActionEvaluationPositionActionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(actionDene, new Integer(evaluationPosition)));
							//
							// Now store this actuatorActionEvaluationPositionActionIndex which contains the order and what actions to
							// execute FOR A SPECIFICACTUATOR into a Vector whcih will maintain the order of execution of the
							// actuators.  this is critical because every actuator has an action with evaluation position eual to 1

						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}


					//
					// now sort the actions
					//
					Collections.sort(actuatorActionEvaluationPositionActionIndex, new IntegerCompare());



					//
					// we are inside of the loop processing the actuators in the correct order and
					// actuatorActionEvaluationPositionActionIndex contains the action denes
					// in the correct order, so create a list that contains the actuatorName and the actions to be executed
					///

					actuatorDeneNameActuatorActionEvaluationPositionActionIndex.put(actionListDeneName, actuatorActionEvaluationPositionActionIndex);

				}

			}









			//
			// end of process structures
			//

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();
			e.printStackTrace();

			String m = "The denome file was not formated properly.  Path: " + selectedDenomeFileName + " Error:" + e.getMessage() + "\rStacktrace:";
			info.put("message", m);
			throw new MissingDenomeException(info);
		}
	}

	class IntegerCompare implements Comparator<Map.Entry<?, Integer>>{
		public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}

	public JSONArray getAllDeneWordAttributeByDeneWordTypeFromDene(JSONObject deneJSONObject , String type, String whatToBring) throws JSONException{
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			if(!deneWord.has("DeneWord Type"))continue;
			String deneWordValueType = deneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE);
			if(deneWordValueType.equals(type)){
				if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
					toReturn.put(deneWord);
				}else{
					toReturn.put(deneWord.get(whatToBring));
				}
			}
		}
		return toReturn;
	}
	/**
	 *  this method will return a list of all the teleonomes that are needed in the  external data
	 *  denechain and it is used in SubscriberThreads
	 * @return
	 */

	public Vector getExternalTeleonomeNamesRequired(){
		Vector toReturn = new Vector();

		try {
			JSONArray deneChains = purposeNucleus.getJSONArray("DeneChains");
			JSONObject deneChain, dene;
			JSONArray denes;
			Vector v = new Vector();
			String externalPath;
			Identity identity;
			String teleonomeName;
			for(int i=0;i<deneChains.length();i++){
				deneChain = deneChains.getJSONObject(i);
				if(deneChain.getString("Name").equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)){

					denes = deneChain.getJSONArray("Denes");
					for(int j=0;j<denes.length();j++){
						dene = denes.getJSONObject(j);
						v = DenomeUtils.getDeneWordAttributeForAllDeneWordsFromDene(dene , TeleonomeConstants.DENEWORD_DATA_LOCATION_ATTRIBUTE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						for(int k=0;k<v.size();k++){
							externalPath = ((String)v.elementAt(k)).substring(1);
							identity = new Identity(externalPath);
							teleonomeName = identity.getTeleonomeName();
							if(!toReturn.contains(teleonomeName)){
								toReturn.addElement(teleonomeName);
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toReturn;
	}


	public void setNetworkInfo(String l, String h){
		localIpAddress=l;
		hostName=h;
	}



	public JSONArray preparePhysiologyTable(){
		JSONArray toReturn=null;
		try {
			toReturn = getDeneByDeneType(TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_SENSORS, TeleonomeConstants.DENE_TYPE_SENSOR);
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return toReturn;
	}

	public String renderJQTreeData(String pulse) throws JSONException{



		JSONArray deneChainsArray=null;

		JSONObject toReturn = new JSONObject();

		//
		// now parse them
		JSONObject dataSource = new JSONObject(pulse);
		JSONObject denomeObject = dataSource.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String name;
		JSONObject aJSONObject, nucleus = null;
		JSONObject aDeneJSONObject, aDeneWordJSONObject;
		JSONArray denesJSONArray, deneWordsJSONArray;
		String valueType, valueInString;
		Object object;

		JSONArray toReturnNucleusArray = new JSONArray();

		JSONObject toReturnNucleus = new JSONObject();
		JSONArray toReturnChainArray = new JSONArray();

		JSONObject toReturnChain = new JSONObject();
		JSONObject toReturnDene = new JSONObject();
		JSONArray toReturnDenesArray = new JSONArray();
		JSONArray toReturnDeneWordsArray;
		JSONObject toReturnDeneWord = new JSONObject();


		for(int p=0;p<nucleiArray.length();p++){
			nucleus = (JSONObject) nucleiArray.get(p);


			deneChainsArray = nucleus.getJSONArray("DeneChains");
			toReturnNucleus = new JSONObject();
			toReturnNucleusArray.put(toReturnNucleus);

			toReturnNucleus.put("label", nucleus.getString("Name"));
			toReturnChainArray = new JSONArray();
			toReturnNucleus.put("children", toReturnChainArray);

			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				toReturnChain = new JSONObject();
				toReturnChainArray.put(toReturnChain);
				toReturnChain.put("label",aJSONObject.getString("Name"));



				toReturnDenesArray = new JSONArray();
				toReturnChain.put("children", toReturnDenesArray);


				denesJSONArray = aJSONObject.getJSONArray("Denes");
				for(int j=0;j<denesJSONArray.length();j++){
					aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
					toReturnDene = new JSONObject();
					toReturnDenesArray.put(toReturnDene);

					toReturnDene.put("label",aDeneJSONObject.getString("Name"));
					//
					// add the other parameters
					Iterator keys = aDeneJSONObject.keys();
					while(keys.hasNext()){
						String n = (String) keys.next();
						if(!n.equals("DeneWords") && !n.equals("Name")){	
							Object oo = aDeneJSONObject.get(n);
							toReturnDene.put(n,oo);

						}
					}

					toReturnDeneWordsArray = new JSONArray();
					toReturnDene.put("children", toReturnDeneWordsArray);


					deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
					for(int k=0;k<deneWordsJSONArray.length();k++){
						toReturnDeneWord = new JSONObject();
						toReturnDeneWordsArray.put(toReturnDeneWord);
						aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);

						toReturnDeneWord.put("label", aDeneWordJSONObject.getString("Name"));

						//
						// add the other parameters
						keys = aDeneWordJSONObject.keys();
						while(keys.hasNext()){
							String n = (String) keys.next();
							if( !n.equals("Name")){	
								toReturnDeneWord.put(n,aDeneWordJSONObject.get(n));
							}
						}
					}

				}


			}
		}


		return toReturnNucleusArray.toString();       
	}



	public DeneWord getDeneWordByName(String nucleusName, String deneChainName, String deneName, String deneWordName){
		return null;
	}

	public JSONArray getDenesByDeneType(JSONObject aDeneChainJSONObject,String deneType) throws JSONException{
		JSONArray denes = aDeneChainJSONObject.getJSONArray("Denes");
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<denes.length();i++){
			if(denes.getJSONObject(i).has(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE) && denes.getJSONObject(i).getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE).equals(deneType)){
				toReturn.put(denes.getJSONObject(i));
			}
		}
		return toReturn;
	}

	public JSONArray getDeneByDeneType(String nucleusName,String deneChainName, String deneType) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		JSONArray toReturn = new JSONArray();
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						try {
							if(aDeneJSONObject.getString("Dene Type").equals(deneType)){
								toReturn.put(aDeneJSONObject);
							}
						} catch (JSONException e) {
							// if we are here is because there is no Dene Type attribute in this dene,
							// this is ok, but in future version of the compiler this might change
						}
					}

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return toReturn;
	}

	public Object getDeneWordValueByDeneType(String nucleusName,String deneChainName, String deneType, String deneWordLabel) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);

						if(aDeneJSONObject.getString("Dene Type").equals(deneType)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								object = aDeneWordJSONObject.get("Value");
								valueType = aDeneWordJSONObject.getString("Value Type");
								//
								// before checking the name, check the type, because it is a
								// DenePointers then the name will not match\
								String pointerValue="";
								if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)){
									//
									// the deneWordLabel will have a form "@Communication Protocol:Serial Data Rate"
									// The first part of the string represents the deneword we are looking for
									// we then take the value and use it as a pointer to the name of the dene 
									// has the information.  the deneword in that secundary dene is given
									// by the pointer value

									String denePointerValue = deneWordLabel.substring(1);
									String[] denePointerValueTokens = denePointerValue.split(":");
									deneWordLabel = denePointerValueTokens[0];
									pointerValue = denePointerValueTokens[1];
									//
									// we know it will be in the same codon, which means the same denechain
									// and the name of the dene would be the value deneword represented 
									// by deneWordLabelRendered

									// if we are here then we have a situation where the value
									// contains a pointer to another dene, expressed as  @name of dene
									// ie :
									//"Name": "Communication Protocol",
									//"Value": "@Serial Parameters",
									//
									// we need to get the dene where the name is 
									String deneNameToPointTo =  ((String)object).substring(1);
									Object renderedPointer = getDeneWordValueByName( nucleusName, deneChainName,deneNameToPointTo, pointerValue);
									return renderedPointer;

								}




								if(aDeneWordJSONObject.getString("Name").equals(deneWordLabel)){

									object = aDeneWordJSONObject.get("Value");
									valueType = aDeneWordJSONObject.getString("Value Type");
									if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)){
										Integer I = new Integer((int) object);
										return I;
									}else if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)){
										Double D = new Double((double) object);
										return D;
									}else if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)){
										//
										// if we are here then we have a situation where the value
										// contains a pointer to another dene, expressed as  @name of dene
										// ie :
										//"Name": "Communication Protocol",
										//"Value": "@Serial Parameters",
										//
										// we need to get the dene where the name is 
										Object renderedPointer = getDeneWordValueByName( nucleusName, deneChainName, (String)object, pointerValue);
										return renderedPointer;


									}
								}
							}
						}
					}

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return null;
	}

	public JSONArray renderDeneWordsFromPointers(JSONArray deneWordPointers){
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<deneWordPointers.length();i++){
			try {
				toReturn.put(getDeneWordByIdentity(new Identity(deneWordPointers.getString(i))));
			} catch (InvalidDenomeException | JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return toReturn;
	}

	public JSONArray loadDenesFromPointers(JSONArray denePointers){
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<denePointers.length();i++){
			try {
				toReturn.put(getDeneByIdentity(new Identity(denePointers.getString(i))));
			} catch (InvalidDenomeException | JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return toReturn;
	}


	public JSONObject getDeneWordByIdentity(Identity identity) throws InvalidDenomeException{
		//
		// if we are pointing at itself return the default
		if(identity.isCommand()){
			if(identity.getCommandValue().equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
				try {
					long pulseTimeInMillis = denomeJSONObject.getLong("Pulse Timestamp in Milliseconds");
					Unit millisecondUnit=SI.MILLI(SI.SECOND);
					JSONObject renderedDeneObject = this.createDeneWordJSONObject(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS, ""+pulseTimeInMillis, millisecondUnit.toString(), TeleonomeConstants.DATATYPE_LONG, true);
					return renderedDeneObject;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
		}else{
			return getDeneWordByPointer(identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName(), identity.getDeneWordName());
		}
		return null;
	}

	public JSONObject getDeneWordByPointer(String nucleusName,String deneChainName, String deneName, String deneWordName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {


			//
			// now parse them
			logger.debug("line 1044 denomeJSONObject=" + denomeJSONObject);
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject;
			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					internalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					purposeNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					mnemosyneNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
					humanInterfaceNucleus= aJSONObject;
				}

			}

			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						object =  denesJSONArray.get(j);
						
						if(object!=null && object instanceof JSONObject) {
							aDeneJSONObject = (JSONObject) object;
							if(aDeneJSONObject.getString("Name").equals(deneName)){
								deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
								for(int k=0;k<deneWordsJSONArray.length();k++){
									aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
									if(aDeneWordJSONObject.getString("Name").equals(deneWordName)){
										return aDeneWordJSONObject;
									}
								}
							}
						}else {
							return null;
						}
					}

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return null;
	}

	public JSONObject getDeneByIdentity(Identity identity) throws InvalidDenomeException{
		return getDeneByPointer(identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName());
	}

	public JSONObject getDeneByPointer(String nucleusName,String deneChainName, String deneName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);

						if(aDeneJSONObject.getString("Name").equals(deneName)){
							return aDeneJSONObject;
						}
					}

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return null;
	}




	public Object getDeneWordAttributeByDeneWordNameFromDenePointer(String denePointer,String whatDeneWord, String whatToReturn) throws JSONException, InvalidDenomeException{

		JSONObject dene = getDeneByIdentity(new Identity(denePointer));
		return DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene( dene , whatDeneWord, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

	}

	/**
	 * gets the value of a deneword of an external teleonome
	 * 
	 * @param externalTeleonomePulse
	 * @param deneWordPointer
	 * @return
	 * @throws InvalidDenomeException
	 * @throws JSONException 
	 */
	public Object getDeneWordValueByNameFromExternalTeleonome(JSONObject externalTeleonomePulse, String deneWordPointer) throws InvalidDenomeException, JSONException{
		JSONArray deneChainsArray=null;

		JSONObject externalDenomeJSONObject = externalTeleonomePulse.getJSONObject("Denome");

		String[] tokens = deneWordPointer.split(":");
		if(tokens.length==2){
			//
			// the user wants the Pulse Timestamp or any other Denome Level variable
			return externalDenomeJSONObject.getString(tokens[1]);	
		}
		//
		// if we are here is because we need a deneword, so
		// first parse the data
		String teleonomeName = tokens[0];
		String externalNucleusName = tokens[1];
		String externalDeneChainName = tokens[2];
		String externalDeneName = tokens[3];
		String externalDeneWordName = tokens[4];
		JSONObject aJSONObject;
		String name;
		JSONArray externalDeneChainArray=null;

		JSONArray externalNucleiArray = externalDenomeJSONObject.getJSONArray("Nuclei");
		for(int i=0;i<externalNucleiArray.length();i++){
			aJSONObject = (JSONObject) externalNucleiArray.get(i);
			name = aJSONObject.getString("Name");
			if(name.equals(externalNucleusName)){
				externalDeneChainArray= aJSONObject.getJSONArray("DeneChains");
			}
		}

		try {

			JSONObject  aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<externalDeneChainArray.length();i++){
				aJSONObject = (JSONObject) externalDeneChainArray.get(i);
				if(aJSONObject.getString("Name").equals(externalDeneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);

						if(aDeneJSONObject.getString("Name").equals(externalDeneName)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								object = aDeneWordJSONObject.get("Value");
								valueType = aDeneWordJSONObject.getString("Value Type");


								if(aDeneWordJSONObject.getString("Name").equals(externalDeneWordName)){
									return object;
								}
							}
							//
							// if we are here is because we did not return, ie we did not find the
							// requested DeneWordName, so we need to assume that this parameters is located
							// in another Dene of the same Codon of the Dene pased as parameter, look for that
							//
						}
					}

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The external for " + teleonomeName +" is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return null;
	}


	public Object getDeneWordValueByName(String nucleusName,String deneChainName, String deneName, String deneWordLabel) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		String backupDeneWordLabel=deneWordLabel;
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);

						if(aDeneJSONObject.getString("Name").equals(deneName)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								object = aDeneWordJSONObject.get("Value");
								valueType = aDeneWordJSONObject.getString("Value Type");
								//
								// before checking the name, check the type, because it is a
								// DenePointers then the name will not match\
								String pointerValue="";
								if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)){
									//
									// the value will have a form "@Communication Protocol:Serial Data Rate"
									// The first part of the string represents the dene where the value 
									// contains a pointer to the address of the dene that contains the value
									// we want
									String denePointerValue = ((String)object).substring(1);
									String[] denePointerValueTokens = denePointerValue.split(":");
									deneWordLabel = denePointerValueTokens[0];
									pointerValue = denePointerValueTokens[1];
									//
									// we know it will be in the same codon, which means the same denechain
									// and the name of the dene would be the value deneword represented 
									// by deneWordLabelRendered

								}else deneWordLabel=backupDeneWordLabel;

								if(aDeneWordJSONObject.getString("Name").equals(deneWordLabel)){


									if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)){
										Integer I = new Integer((int) object);
										return I;
									}else if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)){
										Double D = new Double((double) object);
										return D;
									}else if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)){
										//
										// if we are here then we have a situation where the value
										// contains a pointer to another dene, expressed as  @name of dene
										// ie :
										//"Name": "Communication Protocol",
										//"Value": "@Serial Parameters",
										//
										// we need to get the dene where the name is 
										Object renderedPointer = getDeneWordValueByName( nucleusName, deneChainName, (String)object, pointerValue);
										return renderedPointer;


									}
								}
							}
							//
							// if we are here is because we did not return, ie we did not find the
							// requested DeneWordName, so we need to assume that this parameters is located
							// in another Dene of the same Codon of the Dene pased as parameter, look for that
							//
						}
					}

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return null;
	}



	public JSONObject getDenomeJSONObject(){
		return denomeJSONObject;
	}



	public JSONObject getMenomicElementByIdentity(Identity identity){

		String nucleusName=identity.getNucleusName();
		String deneChainName = identity.getDenechainName();
		String deneName = identity.getDeneName();
		String deneWordName = identity.getDeneWordName();

		JSONArray deneChainsArray=null;
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				if(deneChainName.equals(""))return internalNucleus;
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				if(deneChainName.equals(""))return purposeNucleus;
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					if(deneName.equals(""))return aJSONObject;

					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);

						if(aDeneJSONObject.getString("Name").equals(deneName)){
							if(deneWordName.equals(""))return aDeneJSONObject;

							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.get("Name").equals("deneWordName")){
									return aDeneWordJSONObject;
								}

							}
						}
					}
				}
			}
		}catch(JSONException e){

		}
		return null;
	}


	public JSONArray getDeneChainsForMutation(String mutationName){
		JSONArray toReturn = new JSONArray();

		try {
			JSONArray mutationsJSONArray = denomeObject.getJSONArray("Mutations");
			JSONObject mutation;
			String type;

			for(int i=0;i<mutationsJSONArray.length();i++){
				mutation = mutationsJSONArray.getJSONObject(i);
				mutationName = mutation.getString("Name");
				if(mutationName.equals(mutation.getString("Name"))){
					toReturn =  mutation.getJSONArray("DeneChains");
				}

			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug("the mutation " + mutationName + " does not have a type");
			e.printStackTrace();
		}
		return toReturn;

	}


	public JSONArray getDeneChainsForNucleus(String nucleusName){
		try {
			JSONArray nuclei = denomeObject.getJSONArray("Nuclei");
			JSONObject nucleus;
			for(int i=0;i<nuclei.length();i++){
				nucleus=(JSONObject)nuclei.getJSONObject(i);
				if(nucleus.getString("Name").equals(nucleusName)){
					JSONArray deneChains = nucleus.getJSONArray("DeneChains");
					return deneChains;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public JSONObject getDeneChainByName(JSONObject sourceOfData, String nucleusName, String deneChainName) throws JSONException{
		JSONObject denomeArray = sourceOfData.getJSONObject("Denome");
		JSONArray nucleiArray = denomeArray.getJSONArray("Nuclei");
		JSONObject aNucleusJSONObject, aDeneChain;
		JSONArray deneChains;
		for(int i=0;i<nucleiArray.length();i++){
			aNucleusJSONObject = nucleiArray.getJSONObject(i);
			if(aNucleusJSONObject.getString("Name").equals(nucleusName)){
				deneChains = aNucleusJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					aDeneChain = deneChains.getJSONObject(j);
					if(aDeneChain.get("Name").equals(deneChainName)){
						return aDeneChain;
					}
				}
			}
		}
		return null;
	}

	public JSONObject getDeneByName(JSONObject sourceOfData, String nucleusName, String deneChainName, String deneName) throws JSONException{
		JSONObject denomeArray = sourceOfData.getJSONObject("Denome");
		JSONArray nucleiArray = denomeArray.getJSONArray("Nuclei");
		JSONObject aNucleusJSONObject, aDeneChain,aDene;
		JSONArray deneChains;
		JSONArray denes;

		for(int i=0;i<nucleiArray.length();i++){
			aNucleusJSONObject = nucleiArray.getJSONObject(i);
			if(aNucleusJSONObject.getString("Name").equals(nucleusName)){
				deneChains = aNucleusJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					aDeneChain = deneChains.getJSONObject(j);
					if(aDeneChain.get("Name").equals(deneChainName)){
						denes = aDeneChain.getJSONArray("Denes");
						for(int k=0;k<denes.length();k++){
							aDene = denes.getJSONObject(k);
							if(aDene.getString("Name").equals(deneName)){
								return aDene;
							}
						}
					}
				}
			}
		}
		return null;
	}

	public Object getDeneWordByName(JSONObject sourceOfData, String nucleusName, String deneChainName, String deneName, String deneWordName, String whatToReturn) throws JSONException{

		JSONObject deneWord = getDeneWordByName( sourceOfData,  nucleusName,  deneChainName,  deneName, deneWordName);
		if(deneWord==null)return null;
		if(whatToReturn.equals(TeleonomeConstants.COMPLETE))return deneWord;
		else{
			return deneWord.get(whatToReturn);
		}

	}

	public JSONObject getDeneWordByName(JSONObject sourceOfData, String nucleusName, String deneChainName, String deneName, String deneWordName) throws JSONException{
		JSONObject denomeArray = sourceOfData.getJSONObject("Denome");
		JSONArray nucleiArray = denomeArray.getJSONArray("Nuclei");
		JSONObject aNucleusJSONObject, aDeneChain,aDene, aDeneWord;
		JSONArray deneChains;
		JSONArray denes, deneWords;

		for(int i=0;i<nucleiArray.length();i++){
			aNucleusJSONObject = nucleiArray.getJSONObject(i);
			if(aNucleusJSONObject.getString("Name").equals(nucleusName)){
				deneChains = aNucleusJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					aDeneChain = deneChains.getJSONObject(j);
					if(aDeneChain.get("Name").equals(deneChainName)){
						denes = aDeneChain.getJSONArray("Denes");
						for(int k=0;k<denes.length();k++){
							aDene = denes.getJSONObject(k);
							if(aDene.getString("Name").equals(deneName)){
								deneWords = aDene.getJSONArray("DeneWords");
								for(int l=0;l<deneWords.length();l++){
									aDeneWord = deneWords.getJSONObject(l);
									if(aDeneWord.getString("Name").equals(deneWordName)){
										return aDeneWord;
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	public JSONArray getAllDenesInDeneChainByName( String nucleusName, String deneChainName) throws JSONException{
		try {
			JSONArray nuclei = denomeObject.getJSONArray("Nuclei");
			JSONObject nucleus, deneChain;
			for(int i=0;i<nuclei.length();i++){
				nucleus=(JSONObject)nuclei.getJSONObject(i);
				if(nucleus.getString("Name").equals(nucleusName)){
					JSONArray deneChains = nucleus.getJSONArray("DeneChains");
					for(int j=0;j<deneChains.length();j++){
						deneChain=(JSONObject)deneChains.getJSONObject(j);
						if(deneChain.getString("Name").equals(deneChainName)){
							JSONArray denes = deneChain.getJSONArray("Denes");
							return denes;
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * This method will loop over all the denewords of the dene passed as the first parameter
	 * and extract the value of the dene word whose name is passed as the second parameter
	 * Note that this method can not be used with Dene Pointers.  If a deneWordLabel
	 *  is requested that is of type Dene Pointer an exception will be throwb
	 * @param dene
	 * @param deneWordName
	 * @return
	 * @throws JSONException 
	 */
	public Object extractDeneWordValueFromDene(JSONObject aDeneJSONObject, String deneWordLabel) throws JSONException{
		JSONObject aDeneWordJSONObject;
		JSONArray deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
		Object object;
		//String valueType;
		for(int k=0;k<deneWordsJSONArray.length();k++){
			aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
			object = aDeneWordJSONObject.get("Value");

			if(aDeneWordJSONObject.getString("Name").equals(deneWordLabel)){
				return object;
			}
		}
		return null;
	}

	public Object getDeneWordAttributeByDeneWordTypeFromDene(JSONObject aJSONObject , String type, String whatToBring) throws JSONException{
		JSONArray deneWords = aJSONObject.getJSONArray("DeneWords");
		String deneWordValueType;
		JSONObject deneWord;
		for(int i=0;i<deneWords.length();i++){
			deneWord = deneWords.getJSONObject(i); 
			if(deneWord.has(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE)){
				deneWordValueType = deneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE);
				if(deneWordValueType.equals(type)){
					if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
						return deneWord;
					}else{
						return deneWord.get(whatToBring);
					}
				}
			}
		}
		return null;
	}

	public JSONObject createDeneWordJSONObject(String name, String value, String units,String valueType, boolean required) throws JSONException{
		JSONObject deneWord = new JSONObject();
		deneWord.put("Name", name);
		if(value!=null)deneWord.put("Value", value);
		deneWord.put("Value Type", valueType);
		if(units!=null)deneWord.put("Units", units);
		deneWord.put("Required", required);
		return deneWord;

	}





}
