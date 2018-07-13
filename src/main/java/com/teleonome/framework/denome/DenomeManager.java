package com.teleonome.framework.denome;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.apache.commons.jexl2.JexlArithmetic;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject; 

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager.MutationActionsExecutionResult;
import com.teleonome.framework.exception.InvalidDeneStructureRequestException;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.InvalidMutation;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.hypothalamus.CommandRequest;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;


public class DenomeManager {

	//
	// Application level variables read from the Denome
	//
	private int basePulseFrequency=0;
	private int interSensorReadTimeoutMilliseconds=0;
	private String timeZone="";
	int pacemakerPid=-1;
	String localIpAddress="", hostName="";
	String selectedDenomeFileName="";

	SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	SimpleDateFormat statusMessagedateTimeFormat = new SimpleDateFormat("HH:mm");

	private static DenomeManager aDenomeManager=null;
	private Logger logger;
	private PostgresqlPersistenceManager aDBManager=null;
	JSONObject internalNucleus=null;
	JSONObject purposeNucleus=null;
	JSONObject mnemosyneNucleus=null;
	JSONObject humanInterfaceNucleus=null;

	JSONObject denomeJSONObject = null;
	JSONObject currentlyCreatingPulseJSONObject=null;
	JSONObject previousPulseJSONObject=null;
	String ssidConnectedTo="";

	Hashtable microControllerNameMicroControllerParamsIndex = new Hashtable();

	Vector sensorDenesVector = new Vector();
	Vector actuatorDenesVector = new Vector();
	public Hashtable pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex = new Hashtable();
	public Hashtable pointerToMicroControllerActuatorExecutionPositionDeneIndex = new Hashtable();


	ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 
	ArrayList<Map.Entry<JSONObject, Integer>> actuatorExecutionPositionDeneForInitialIndex = new ArrayList();

	ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex = new ArrayList(); 
	ArrayList<Map.Entry<JSONObject, Integer>> actuatorExecutionPositionDeneIndex = new ArrayList();
	JSONArray analyticonDenesJSONArray = new JSONArray();
	JSONArray mnemosyconDenesJSONArray = new JSONArray();

	Hashtable pointerToMicroControllerSensorsDeneWordsForInitialBySensorRequestQueuePositionIndex = new Hashtable();
	Hashtable pointerToMicroControllerActuatorExecutionPositionForInitialDeneIndex  = new Hashtable();



	ArrayList<Map.Entry<String, Integer>> microControllerPointerProcessingQueuePositionIndex = new ArrayList();

	Hashtable<String,ArrayList> actuatorDeneNameActuatorActionEvaluationPositionActionIndex = new Hashtable();
	Hashtable<String,ArrayList> actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex = new Hashtable();


	Hashtable<String,JSONArray> externalDataNameDeneWords = new Hashtable();
	JSONArray microProcessorsDenesJSONArray=null;

	/**
	 * the hashtable that contains the last pulse of every source of external data
	 * 
	 */
	Hashtable<String, JSONObject> lastExternalPulse = new Hashtable();
	SimpleDateFormat simpleFormatter = new SimpleDateFormat("E yyyy.MM.dd HH:mm:ss Z");

	/**
	 *  the timestamp that represents when a pulse started
	 *   it is used to calculate how long it takes to 
	 *   create a pulse
	 */
	long previousPulseMillis = 0;
	long currentPulseStartTimestampMillis=0;
	String currentPulseStatusMessage=TeleonomeConstants.STATUS_MESSAGE_USE_CURRENT_PULSE_SECONDS;
	private Vector analyticonsDataSourcesLate = new Vector();
	String teleonomeName="";
	private static final JexlEngine jexl = new JexlEngine(null, new NoStringCoercionArithmetic(), null, null);
	
	//
	// a hashmap that contains as key the name of the foreign teleonome and as value an ArrayList with the identity pointers of all data items needed from that teleonome/
	HashMap externalDataLocationHashMap = new HashMap();
	
	static {
		jexl.setCache(512);
		jexl.setLenient(false);
		jexl.setSilent(false);
	}


	public static class NoStringCoercionArithmetic extends JexlArithmetic {
		public NoStringCoercionArithmetic(boolean lenient) {
			super(lenient);
		}

		public NoStringCoercionArithmetic() {
			this(false);
		}

		@Override
		public Object add(Object left, Object right) {
			if ((left instanceof String && ((String)left).contains("#")) || (right instanceof String && ((String)right).contains("#"))) {
				return left.toString() + right.toString();
			}
			else {
				return super.add(left, right);
			}
		}
	}



	public DenomeManager(){
		logger = Logger.getLogger(getClass());
		logger.debug("Initiating Denome Manager");
		aDBManager = PostgresqlPersistenceManager.instance();
	}

	public static DenomeManager instance() throws MissingDenomeException {

		if(aDenomeManager==null){
			aDenomeManager = new DenomeManager();
			aDenomeManager.init();
		}
		return aDenomeManager;
	}

	public String getSelectedDenomeFileName(){
		return selectedDenomeFileName;
	}

	private void init() throws MissingDenomeException{
		File localDir = new File(Utils.getLocalDirectory());
		File[] files = localDir.listFiles();
		selectedDenomeFileName="";
		found:
			for(int i=0;i<files.length;i++){
				//
				// force the name to be Teleonome.denome
				//if(FilenameUtils.getExtension(files[i].getAbsolutePath()).equals("denome")){
				if(files[i].getName().equals("Teleonome.denome")){

					selectedDenomeFileName = files[i].getAbsolutePath();
					logger.debug("reading denome from " +selectedDenomeFileName);

					break found;
				}
			}

		loadDenome(selectedDenomeFileName);
	}

	public void loadDenome() throws MissingDenomeException{
		loadDenome(selectedDenomeFileName);
	}

	

	public void writeDenomeToDisk(){
		//
		// now write the denome
		//
		try {
			FileUtils.write(new File(selectedDenomeFileName), currentlyCreatingPulseJSONObject.toString(4));
			FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/Teleonome.denome"), currentlyCreatingPulseJSONObject.toString(4));

		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		logger.debug("Saved pulse to " + selectedDenomeFileName);

	}
	String denomeName="";
	private String initialIdentityMode;

	public String getDenomeName(){
		return denomeName;
	}

	


	private void loadDenome(String fn) throws MissingDenomeException{

		//
		// read the denome from the hard disk
		//  if its not found, then read it from the db
		//
		String stringFormDenome="";
		try {
			File selectedFile = new File(fn);
			logger.debug("reading denome from " +selectedDenomeFileName);

			stringFormDenome = FileUtils.readFileToString(selectedFile);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		if(stringFormDenome.equals("")){
			Hashtable info = new Hashtable();
			info.put("message", "The denome file was not found in " + Utils.getLocalDirectory());
			throw new MissingDenomeException(info);
		}

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

			denomeJSONObject = new JSONObject(stringFormDenome);
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			denomeName = denomeObject.getString("Name");
			//
			// make sure it is garbage cllected
			//
			stringFormDenome=null;


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

			JSONObject processingDeneChain = this.getDeneChainByName(denomeJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);


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
							Object o = deneChainsPurpose.remove(i);
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
			JSONObject aDeneChainJSONObject, aDeneValueJSONObject, actuatorActionJSONObject = null;
			JSONArray sensorDenesJSONArray;
			JSONArray sensorValuesJSONArray, actuatorActionConditionsJSONArray;

			JSONArray onStartActionsDenesJSONArray, actuatorInitialDenesJSONArray, actuatorDenesJSONArray, actuatorActionsJSONArray, descriptiveDenesJSONArray;
			Integer executionPosition=0;

			int sensorRequestQueuePosition=0;
			int evaluationPosition = 0;
			String deneName="";
			String completeLocationString, aDeneJSONObject_Name;
			String valueName="";
			String valueType="";


			actuatorDeneNameActuatorActionEvaluationPositionActionIndex = new Hashtable();
			actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex = new Hashtable();
			ArrayList<Map.Entry<JSONObject, String>>  valuesFromOtherTeleonomesIndex = new ArrayList(); 

			//
			// process the sensors and actuators
			//
			String actuatorDeneName = null;
			JSONObject sensorDene, aDeneJSONObject, actuatorDeneJSONObject, aDeneWordJSONObject;
			JSONArray vitalDeneWordsJsonArray, deneWordValuesArray;
			JSONArray deneWordValues;
			String aDeneWordValuePointer;
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
					//logger.debug("line 516 aDeneJSONObject=" + aDeneJSONObject.toString(4));
					pointerToMicroController =  (String) getDeneWordAttributeByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//logger.debug("line 516 aDeneJSONObject=" + aDeneJSONObject.toString(4));
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
					for(int j=0;j<v.size();j++){
						aDeneJSONObject = (JSONObject) v.elementAt(j);
						isSensor = DenomeUtils.isDeneOfType(aDeneJSONObject, TeleonomeConstants.DENE_TYPE_SENSOR);

						if(isSensor){
							sensorValuesPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

							sensorValuesJSONArray = this.loadDenesFromPointers(sensorValuesPointersJSONArray);


							logger.debug("sensorValuesJSONArray.length=" + sensorValuesJSONArray.length());
							for(int k=0;k<sensorValuesJSONArray.length();k++){

								aDeneValueJSONObject = (JSONObject) sensorValuesJSONArray.get(k);
								logger.debug("k="+ k + " aDeneValueJSONObject:" + aDeneValueJSONObject);

								I = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneValueJSONObject, "Sensor Request Queue Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

								sensorRequestQueuePositionDeneWordIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(aDeneValueJSONObject, I));
								Collections.sort(sensorRequestQueuePositionDeneWordIndex, new Comparator<Map.Entry<?, Integer>>(){
									public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
										return o1.getValue().compareTo(o2.getValue());
									}});

							}
						}else if(DenomeUtils.isDeneOfType(aDeneJSONObject, TeleonomeConstants.DENE_TYPE_ON_START_SENSOR)){
							sensorValuesPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(aDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

							sensorValuesJSONArray = this.loadDenesFromPointers(sensorValuesPointersJSONArray);



							for(int k=0;k<sensorValuesJSONArray.length();k++){
								aDeneValueJSONObject = (JSONObject) sensorValuesJSONArray.get(k);

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
					pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex.put(pointerToMicroController, sensorRequestQueuePositionDeneWordIndex);
					pointerToMicroControllerSensorsDeneWordsForInitialBySensorRequestQueuePositionIndex.put(pointerToMicroController, sensorRequestQueuePositionDeneWordForInitialIndex);
					logger.debug("storing data for microcontrollerpointer: " + pointerToMicroController + " sensorRequestQueuePositionDeneWordIndex=" + sensorRequestQueuePositionDeneWordIndex.size() + " sensorRequestQueuePositionDeneWordForInitialIndex:" + sensorRequestQueuePositionDeneWordForInitialIndex.size());
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
					v = (Vector)pointerToMicroControllerActuatorDenesVectorIndex.get(pointerToMicroController);
					actuatorExecutionPositionDeneIndex = new ArrayList();
					for(int j=0;j<v.size();j++){
						aDeneJSONObject = (JSONObject) v.elementAt(j);
						actuatorDeneName = aDeneJSONObject.getString("Name");
						executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(aDeneJSONObject,"Execution Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
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

					// 
					//
					// at this point actuatorExecutionPositionDeneWordIndex contains the actuators sorted according to the execution position
					// next for every actuatordene sort its actions according to their evaluation position
					// so evaluate wheter or not to execute an action
					for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneIndex) {

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
			// Cant assume that all teleonomes will have actuators
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
			if(deneChainNameDeneChainIndex.containsKey(TeleonomeConstants.DENECHAIN_MNEMOSYCONS)) {

				JSONObject anMnemosyconsDeneChainJSONObject = (JSONObject)deneChainNameDeneChainIndex.get(TeleonomeConstants.DENECHAIN_MNEMOSYCONS);
				//logger.debug("in denomemagager anAnalyticonsDeneChainJSONObject= " + anAnalyticonsDeneChainJSONObject);
				if(anMnemosyconsDeneChainJSONObject!=null){
					mnemosyconDenesJSONArray = DenomeUtils.getDenesByDeneType(anMnemosyconsDeneChainJSONObject, TeleonomeConstants.DENE_TYPE_MNEMOSYCON);
				}

			}

			//
			// process the purpose nucleus
			//
			JSONArray purposeNucleusDeneChains = (JSONArray) purposeNucleus.get("DeneChains");
			JSONArray purposeDenesJSONArray, purposeDeneWordsJSONArray, externalDataDeneWordsJSONArray;
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
			JSONArray v = new JSONArray();
			String externalPath;
			Identity identity;
			String teleonomeName, deneType;
			for(int i=0;i<deneChains.length();i++){
				deneChain = deneChains.getJSONObject(i);
				if(!deneChain.has("Name")) {
					System.out.println("dene chain has no nmae=" + purposeNucleus.toString(4));
				}
				if(deneChain.getString("Name").equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)){

					denes = deneChain.getJSONArray("Denes");
					for(int j=0;j<denes.length();j++){
						dene = denes.getJSONObject(j);
						deneType = dene.getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE);
						if(deneType.equals(TeleonomeConstants.DENE_TYPE_EXTERNAL_DATA_SOURCE)){	
							v = DenomeUtils.getAllDeneWordsFromDeneByDeneWordType(dene , TeleonomeConstants.DENEWORD_DATA_LOCATION_ATTRIBUTE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							for(int k=0;k<v.length();k++){
								externalPath = ((String)v.get(k)).substring(1);
								identity = new Identity(externalPath);
								teleonomeName = identity.getTeleonomeName();
								if(!toReturn.contains(teleonomeName)){
									toReturn.addElement(teleonomeName);
								}
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		return toReturn;
	}

	public void setProcessInfo(int p) {
		pacemakerPid=p;
	}

	public void setNetworkInfo(String l, String h, String i){
		localIpAddress=l;
		hostName=h;
		initialIdentityMode=i;
	}

	public JSONArray getMnemsyconDenesJSONArray(){
		return mnemosyconDenesJSONArray;
	}
	public JSONArray getAnalyticonDenesJSONArray(){
		return analyticonDenesJSONArray;
	}
	/**
	 * this method gets called everytime this Teleonome receives a pulse from another Teleonome
	 * @param teleonomeName
	 * @param jsonMessage
	 */

	public void updateExternalData(String teleonomeName, JSONObject jsonMessage){
		logger.debug("received updated from "+teleonomeName+ " with size "+jsonMessage.toString().length() + " and lastExternalPulse=" + lastExternalPulse.size());
		//
		// now instead of storing the entire pulse
		// extract the data that is required for the External Data
		// and store that
		ArrayList externalDataLocations = (ArrayList) externalDataLocationHashMap.get(teleonomeName);
		String externalDataPointer;
		Identity externalDataIdentity;
		Object value;
		JSONObject externalDataLastPulseInfoJSONObject = new JSONObject();
		
		long lastPulseExternalTimeInMillis = jsonMessage.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
		externalDataLastPulseInfoJSONObject.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, lastPulseExternalTimeInMillis);
		
		String lastPulseExternalTime = jsonMessage.getString(TeleonomeConstants.PULSE_TIMESTAMP);
		externalDataLastPulseInfoJSONObject.put(TeleonomeConstants.PULSE_TIMESTAMP, lastPulseExternalTime);
		
		Identity externalDataCurrentPulseIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA,"Vital",TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY );
		Identity numberOfPulseForStaleIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL,TeleonomeConstants.DENEWORD_TYPE_NUMBER_PULSES_BEFORE_LATE );
		try{
		    int externalCurrentPulse = (Integer)DenomeUtils.getDeneWordByIdentity(jsonMessage, externalDataCurrentPulseIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		    externalDataLastPulseInfoJSONObject.put(externalDataCurrentPulseIdentity.toString(), externalCurrentPulse);
			
		    int numberOfPulseForStale = (Integer)DenomeUtils.getDeneWordByIdentity(jsonMessage, numberOfPulseForStaleIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		    externalDataLastPulseInfoJSONObject.put(numberOfPulseForStaleIdentity.toString(), numberOfPulseForStale);
			
		}catch(InvalidDenomeException e){
			logger.warn(Utils.getStringException(e));
		}
		if(externalDataLocations!=null) {
			for(int i=0;i<externalDataLocations.size();i++) {
				externalDataPointer = (String) externalDataLocations.get(i);
				externalDataIdentity = new Identity(externalDataPointer);
				try {
					value = DenomeUtils.getDeneWordByIdentity(jsonMessage, externalDataIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					externalDataLastPulseInfoJSONObject.put(externalDataPointer, value);
				} catch (InvalidDenomeException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}

			}
		}
		
		lastExternalPulse.put(teleonomeName,externalDataLastPulseInfoJSONObject );
		
		
		//lastExternalPulse.put(teleonomeName,jsonMessage );

	}


	public Hashtable<String,JSONArray> getExternalDataNameDeneWords(){
		return externalDataNameDeneWords;
	}

	/**
	 * This method is called during the pulse creation to populate all the external variables 
	 * it is the new way, where lasrtExtenalData does not contain the complete pulse,
	 * but just a simple json object with the necesary data
	 * 
	 */
	public Vector processExternalData(){
		//
		// get the address of the deneword where this data is going to
		String reportingAddress, deneWordName;
		Vector teleonomeToReconnect = new Vector();
		try {
			
			JSONObject currentlyCreatingPulseDenome = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
			String teleonomeName = currentlyCreatingPulseDenome.getString("Name");
			JSONArray currentlyCreatingPulseNuclei = currentlyCreatingPulseDenome.getJSONArray("Nuclei");
			JSONArray deneWords;

			JSONObject jsonObject, jsonObjectChain, jsonObjectDene, jsonObjectDeneWord;
			JSONArray chains, denes;
			String externalDataDeneName;
			JSONObject lastPulseExternalTeleonomeJSONObject;
			String externalSourceOfData;

			JSONObject pathologyDeneChain = null, pathologyDeneDeneWord;
			JSONArray pathologyDenes=null, pathologyDeneDeneWords;
			JSONObject pathologyDene;
			String pathologyLocation = "";
			try {
				pathologyDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_PATHOLOGY);
				pathologyDenes = pathologyDeneChain.getJSONArray("Denes");

			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));

			}

			long lastPulseExternalTimeInMillis,difference;
			String lastPulseExternalTime;
			Identity externalDataCurrentPulseIdentity,numberOfPulseForStaleIdentity;
			int secondsToStale=180;
			//String valueType;

			for(int i=0;i<currentlyCreatingPulseNuclei.length();i++){
				jsonObject = currentlyCreatingPulseNuclei.getJSONObject(i);
				if(jsonObject.getString("Name").equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					chains = jsonObject.getJSONArray("DeneChains");
					for(int j=0;j<chains.length();j++){
						jsonObjectChain = chains.getJSONObject(j);

						if(jsonObjectChain.toString().length()>10 && jsonObjectChain.getString("Name").equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)){
							denes = jsonObjectChain.getJSONArray("Denes");

							for(int k=0;k<denes.length();k++){
								jsonObjectDene = denes.getJSONObject(k);
								externalDataDeneName = jsonObjectDene.getString("Name");
								//
								// the externalDataDeneName is the name of the External Teleonome
								// lastPulseExternalTeleonomeJSONObject contains the last pulse
								// of that teleonome
								//
								logger.debug("line 824 denomemanamger, looking for  " + externalDataDeneName + " and lastExternalPulse size=" + lastExternalPulse.size() + " and lastExternalPulse.get[" + lastExternalPulse.containsKey(externalDataDeneName));
								lastPulseExternalTeleonomeJSONObject = (JSONObject)lastExternalPulse.get(externalDataDeneName );
								//
								// there could be the situation where lastPulseExternalTeleonomeJSONObject==null
								// this is because there is no data yet from that teleonome
								//  only proceed if you have data
								//
								// the other problem is when there is external data but the data is stale
								// and therefore should not be used, both of these cases will end up in the
								// pathology report of the denome
								//
								logger.debug("line 1031 denomemanamger, lastPulseExternalTeleonomeJSONObject is not equal to null  " + (lastPulseExternalTeleonomeJSONObject!=null) );

								if(lastPulseExternalTeleonomeJSONObject!=null){
									//
									// check if the data is stale
										logger.debug("line 1111   " + (lastPulseExternalTeleonomeJSONObject.toString(4)) );

									lastPulseExternalTimeInMillis = lastPulseExternalTeleonomeJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
									lastPulseExternalTime = lastPulseExternalTeleonomeJSONObject.getString(TeleonomeConstants.PULSE_TIMESTAMP);
									long now= System.currentTimeMillis();
									difference = now-lastPulseExternalTimeInMillis;
									logger.debug("difference="+ difference + " now=" + now + " lastPulseExternalTimeInMillis=" + lastPulseExternalTimeInMillis + " secondsToStale=" + secondsToStale);

									externalDataCurrentPulseIdentity = new Identity(externalDataDeneName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA,"Vital",TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY );
									secondsToStale=180;
									numberOfPulseForStaleIdentity = new Identity(externalDataDeneName,TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL,TeleonomeConstants.DENEWORD_TYPE_NUMBER_PULSES_BEFORE_LATE );

									try{
										int externalCurrentPulse = (Integer)lastPulseExternalTeleonomeJSONObject.getInt( externalDataCurrentPulseIdentity.toString());
										int numberOfPulseForStale = (Integer)lastPulseExternalTeleonomeJSONObject.getInt( numberOfPulseForStaleIdentity.toString());
										
										secondsToStale = externalCurrentPulse * numberOfPulseForStale;
										logger.debug("externalCurrentPulse="+ externalCurrentPulse + " numberOfPulseForStale=" + numberOfPulseForStale + " secondsToStale=" + secondsToStale);

									}catch(NullPointerException e){
										logger.warn(Utils.getStringException(e));

									}
									deneWords = jsonObjectDene.getJSONArray("DeneWords");
									boolean dataIsStale=false;


									if(difference>secondsToStale){
										dataIsStale=true;
										jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
										jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE);
										logger.debug("data is stale");
										//
										// now create the pathology dene
										//
										pathologyDene = new JSONObject();
										pathologyDenes.put(pathologyDene);

										pathologyDene.put("Name", TeleonomeConstants.PATHOLOGY_DENE_EXTERNAL_DATA);
										pathologyDeneDeneWords = new JSONArray();

										pathologyDene.put("DeneWords", pathologyDeneDeneWords);
										//
										// create the Cause deneword
										//
										pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, TeleonomeConstants.PATHOLOGY_DATA_STALE ,null,"String",true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);
										//
										// create the location deneword
										pathologyLocation = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_EXTERNAL_DATA,externalDataDeneName  ).toString();
										//
										// exterbDataDeneName contains the name of the teleonome that needs to reconnect
										// add it to the return variable
										if(!teleonomeToReconnect.contains(externalDataDeneName)){
											logger.debug(externalDataDeneName + " is stale, adding to recoonectList");

											teleonomeToReconnect.addElement(externalDataDeneName);
										}

										pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);


										pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Last Pulse Timestamp in Millis", ""+lastPulseExternalTimeInMillis ,null,"long",true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);


										pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Last Pulse Timestamp", lastPulseExternalTime ,null,"String",true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);



									}else{
										jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
										jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_OK);
										//
										// first put all the values from the last pulse of this external teleonome
										//
										logger.debug("line 1154 deneWords.length()=" + deneWords.toString(4));

										for(int l=0;l<deneWords.length();l++){
											jsonObjectDeneWord = deneWords.getJSONObject(l);
											//
											// jsonObjectDeneWord is the deneword so first
											// get the data location to know the source of data
											// strip the first character which is a @
											// check that it has a data location because
											// there are denewords in the external data dene
											// that do not have a datalocation, for example
											// the deneword called ExternalDataStatus which stores
											// whether the data is stale or not
											//
											if(jsonObjectDeneWord.has("Data Location")){
												externalSourceOfData = jsonObjectDeneWord.getString("Data Location");
												//
												// now get the value from 
												logger.debug("line 1267 dataIsStale=" + dataIsStale + " externalSourceOfData=" + externalSourceOfData);
												
												Object externalData =  lastPulseExternalTeleonomeJSONObject.get(externalSourceOfData);
												logger.debug("line 1270 externalData=" + externalData);
												
												if(externalData!=null)jsonObjectDeneWord.put("Value", externalData);	
											}

										}
									}
								}else{
									logger.debug("no data from " + externalDataDeneName + " setting all dene to stale");


									jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
									jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE);

									logger.debug("line 1248 disallowexternal data jsonObjectDene  " + jsonObjectDene.getString("Name") + " is stale");



									// add to the pathology denechain
									//
									// now create the pathology dene
									//
									pathologyDene = new JSONObject();
									pathologyDenes.put(pathologyDene);

									pathologyDene.put("Name", TeleonomeConstants.PATHOLOGY_DENE_EXTERNAL_DATA);
									pathologyDeneDeneWords = new JSONArray();

									pathologyDene.put("DeneWords", pathologyDeneDeneWords);
									//
									// create the Cause deneword
									//
									pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, TeleonomeConstants.PATHOLOGY_DATA_NOT_AVAILABLE ,null,"String",true);
									pathologyDeneDeneWords.put(pathologyDeneDeneWord);
									//
									if(!teleonomeToReconnect.contains(externalDataDeneName)){
										logger.debug(externalDataDeneName + " is stale, adding to recoonectList");
										teleonomeToReconnect.addElement(externalDataDeneName);
									}

									// create the location deneword
									pathologyLocation = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_EXTERNAL_DATA,externalDataDeneName  ).toString();
									pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
									pathologyDeneDeneWords.put(pathologyDeneDeneWord);

								}
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		//
		// there is a problem because iam reconneting all the time
		// creating many subscriber threads, to see the effect
		// i am going to always empty and see what happens with the
		//return teleonomeToReconnect;
		return teleonomeToReconnect;//new Vector();
	}
	
	/**
	 * This method is the original method which assumed that the complete
	 *  pulse of the other teleonomes is stred in lastExternalPulse
	 *  this method is now replaced with the one above
	 *  where lstExternlaData only has a simple json object with the values for the necesary info called during the pulse creation to populate all the external variables 
	 * 
	 */
	public Vector processExternalDataFullPulse(){
		//
		// get the address of the deneword where this data is going to
		String reportingAddress, deneWordName;
		Vector teleonomeToReconnect = new Vector();
		try {
			//reportingAddress = (String) extractDeneWordValueFromDene(currentlyProcessingSensorDeneWordValue,"Reporting Address");
			//
			// the address will be of the form # 
			//String[] tokens = reportingAddress.substring(1,reportingAddress.length()).split(":");
			//String teleonomeName = tokens[0];
			//String nucleusName = tokens[1];
			//String deneChainName = tokens[2];
			//String deneName = tokens[3];
			//String deneWordLabel = tokens[4];

			JSONObject currentlyCreatingPulseDenome = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
			String teleonomeName = currentlyCreatingPulseDenome.getString("Name");
			JSONArray currentlyCreatingPulseNuclei = currentlyCreatingPulseDenome.getJSONArray("Nuclei");
			JSONArray deneWords;

			JSONObject jsonObject, jsonObjectChain, jsonObjectDene, jsonObjectDeneWord;
			JSONArray chains, denes;
			String externalDataDeneName;
			JSONObject lastPulseExternalTeleonomeJSONObject;
			String externalSourceOfData;

			JSONObject pathologyDeneChain = null, pathologyDeneDeneWord;
			JSONArray pathologyDenes=null, pathologyDeneDeneWords;
			JSONObject pathologyDene;
			String pathologyLocation = "";
			try {
				pathologyDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_PATHOLOGY);
				pathologyDenes = pathologyDeneChain.getJSONArray("Denes");

			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));

			}

			long lastPulseExternalTimeInMillis,difference;
			String lastPulseExternalTime;
			Identity externalDataCurrentPulseIdentity,numberOfPulseForStaleIdentity;
			int secondsToStale=180;
			String valueType;

			for(int i=0;i<currentlyCreatingPulseNuclei.length();i++){
				jsonObject = currentlyCreatingPulseNuclei.getJSONObject(i);
				if(jsonObject.getString("Name").equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					chains = jsonObject.getJSONArray("DeneChains");
					for(int j=0;j<chains.length();j++){
						jsonObjectChain = chains.getJSONObject(j);

						if(jsonObjectChain.toString().length()>10 && jsonObjectChain.getString("Name").equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)){
							denes = jsonObjectChain.getJSONArray("Denes");

							for(int k=0;k<denes.length();k++){
								jsonObjectDene = denes.getJSONObject(k);
								externalDataDeneName = jsonObjectDene.getString("Name");
								//
								// the externalDataDeneName is the name of the External Teleonome
								// lastPulseExternalTeleonomeJSONObject contains the last pulse
								// of that teleonome
								//
								logger.debug("line 824 denomemanamger, looking for  " + externalDataDeneName + " and lastExternalPulse size=" + lastExternalPulse.size() + " and lastExternalPulse.get[" + lastExternalPulse.containsKey(externalDataDeneName));
								lastPulseExternalTeleonomeJSONObject = (JSONObject)lastExternalPulse.get(externalDataDeneName );
								//
								// there could be the situation where lastPulseExternalTeleonomeJSONObject==null
								// this is because there is no data yet from that teleonome
								//  only proceed if you have data
								//
								// the other problem is when there is external data but the data is stale
								// and therefore should not be used, both of these cases will end up in the
								// pathology report of the denome
								//
								logger.debug("line 1031 denomemanamger, lastPulseExternalTeleonomeJSONObject is not equal to null  " + (lastPulseExternalTeleonomeJSONObject!=null) );

								if(lastPulseExternalTeleonomeJSONObject!=null){
									//
									// check if the data is stale
									if( !lastPulseExternalTeleonomeJSONObject.has(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS)) {
										logger.debug("line 1111   " + (lastPulseExternalTeleonomeJSONObject.toString(4)) );

									}
									lastPulseExternalTimeInMillis = lastPulseExternalTeleonomeJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
									lastPulseExternalTime = lastPulseExternalTeleonomeJSONObject.getString(TeleonomeConstants.PULSE_TIMESTAMP);
									long now= System.currentTimeMillis();
									difference = now-lastPulseExternalTimeInMillis;
									logger.debug("difference="+ difference + " now=" + now + " lastPulseExternalTimeInMillis=" + lastPulseExternalTimeInMillis + " secondsToStale=" + secondsToStale);

									externalDataCurrentPulseIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA,"Vital",TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY );
									secondsToStale=180;
									numberOfPulseForStaleIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL,TeleonomeConstants.DENEWORD_TYPE_NUMBER_PULSES_BEFORE_LATE );

									try{
										int externalCurrentPulse = (Integer)DenomeUtils.getDeneWordByIdentity(lastPulseExternalTeleonomeJSONObject, externalDataCurrentPulseIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
										int numberOfPulseForStale = (Integer)DenomeUtils.getDeneWordByIdentity(lastPulseExternalTeleonomeJSONObject, numberOfPulseForStaleIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

										secondsToStale = externalCurrentPulse * numberOfPulseForStale;
										logger.debug("externalCurrentPulse="+ externalCurrentPulse + " numberOfPulseForStale=" + numberOfPulseForStale + " secondsToStale=" + secondsToStale);

									}catch(InvalidDenomeException e){
										logger.warn(Utils.getStringException(e));

									}catch(NullPointerException e){
										logger.warn(Utils.getStringException(e));

									}
									deneWords = jsonObjectDene.getJSONArray("DeneWords");
									boolean dataIsStale=false;


									if(difference>secondsToStale){
										dataIsStale=true;
										jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
										jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE);
										logger.debug("data is stale");
										//
										// now create the pathology dene
										//
										pathologyDene = new JSONObject();
										pathologyDenes.put(pathologyDene);

										pathologyDene.put("Name", TeleonomeConstants.PATHOLOGY_DENE_EXTERNAL_DATA);
										pathologyDeneDeneWords = new JSONArray();

										pathologyDene.put("DeneWords", pathologyDeneDeneWords);
										//
										// create the Cause deneword
										//
										pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, TeleonomeConstants.PATHOLOGY_DATA_STALE ,null,"String",true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);
										//
										// create the location deneword
										pathologyLocation = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_EXTERNAL_DATA,externalDataDeneName  ).toString();
										//
										// exterbDataDeneName contains the name of the teleonome that needs to reconnect
										// add it to the return variable
										if(!teleonomeToReconnect.contains(externalDataDeneName)){
											logger.debug(externalDataDeneName + " is stale, adding to recoonectList");

											teleonomeToReconnect.addElement(externalDataDeneName);
										}

										pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);


										pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Last Pulse Timestamp in Millis", ""+lastPulseExternalTimeInMillis ,null,"long",true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);


										pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Last Pulse Timestamp", lastPulseExternalTime ,null,"String",true);
										pathologyDeneDeneWords.put(pathologyDeneDeneWord);



									}else{
										jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
										jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_OK);
										//
										// first put all the values from the last pulse of this external teleonome
										//
										logger.debug("line 1154 deneWords.length()=" + deneWords.length());

										for(int l=0;l<deneWords.length();l++){
											jsonObjectDeneWord = deneWords.getJSONObject(l);
											//
											// jsonObjectDeneWord is the deneword so first
											// get the data location to know the source of data
											// strip the first character which is a @
											// check that it has a data location because
											// there are denewords in the external data dene
											// that do not have a datalocation, for example
											// the deneword called ExternalDataStatus which stores
											// whether the data is stale or not
											//
											if(jsonObjectDeneWord.has("Data Location")){
												externalSourceOfData = jsonObjectDeneWord.getString("Data Location").substring(1);
												valueType = jsonObjectDeneWord.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);

												//
												// the external source of data can be of two kinds
												// its either the pulse in which case the form would be
												// Ra:Pulse Timestamp
												//
												// or is a complete path in whcih it would be of the form
												//
												// @Ra:Purpose:Solar Energy Sytem:Current Data:Charge Amperes
												//
												// we need to extract the value from the location and 
												Object externalData =  getDeneWordValueByNameFromExternalTeleonome( lastPulseExternalTeleonomeJSONObject,  externalSourceOfData);
												logger.debug("line 1181 dataIsStale=" + dataIsStale + " externalSourceOfData=" + externalSourceOfData + " externalData=" + externalData);
												if(externalData!=null)jsonObjectDeneWord.put("Value", externalData);	
											}

										}
									}
								}else{
									logger.debug("no data from " + externalDataDeneName + " setting all dene to stale");


									jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
									jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE);

									logger.debug("line 1248 disallowexternal data jsonObjectDene  " + jsonObjectDene.getString("Name") + " is stale");



									// add to the pathology denechain
									//
									// now create the pathology dene
									//
									pathologyDene = new JSONObject();
									pathologyDenes.put(pathologyDene);

									pathologyDene.put("Name", TeleonomeConstants.PATHOLOGY_DENE_EXTERNAL_DATA);
									pathologyDeneDeneWords = new JSONArray();

									pathologyDene.put("DeneWords", pathologyDeneDeneWords);
									//
									// create the Cause deneword
									//
									pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, TeleonomeConstants.PATHOLOGY_DATA_NOT_AVAILABLE ,null,"String",true);
									pathologyDeneDeneWords.put(pathologyDeneDeneWord);
									//
									if(!teleonomeToReconnect.contains(externalDataDeneName)){
										logger.debug(externalDataDeneName + " is stale, adding to recoonectList");
										teleonomeToReconnect.addElement(externalDataDeneName);
									}

									// create the location deneword
									pathologyLocation = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_EXTERNAL_DATA,externalDataDeneName  ).toString();
									pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
									pathologyDeneDeneWords.put(pathologyDeneDeneWord);

								}
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		//
		// there is a problem because iam reconneting all the time
		// creating many subscriber threads, to see the effect
		// i am going to always empty and see what happens with the
		//return teleonomeToReconnect;
		return teleonomeToReconnect;//new Vector();
	}
	/**
	 * this method is called when the application first start, to mark
	 * all external data as stale until we start getting data from the other
	 * teleonomes
	 * @return
	 * @throws IOException 
	 */
	public Vector disallowExternalData() {
		//
		// get the address of the deneword where this data is going to
		String reportingAddress, deneWordName;
		Vector teleonomeToReconnect = new Vector();
		try {

			File selectedFile = new File(Utils.getLocalDirectory() + "Teleonome.denome");
			logger.debug("reading denome from " +selectedDenomeFileName);
			String initialIdentityState="";
			JSONObject denomeJSONObject = new JSONObject(FileUtils.readFileToString(selectedFile));

			JSONObject denome = denomeJSONObject.getJSONObject("Denome");	
			JSONArray currentlyCreatingPulseNuclei = denome.getJSONArray("Nuclei");
			JSONArray deneWords;

			JSONObject jsonObject, jsonObjectChain, jsonObjectDene, jsonObjectDeneWord;
			JSONArray chains, denes;
			String externalDataDeneName;
			JSONObject lastPulseExternalTeleonomeJSONObject;



			long lastPulseExternalTimeInMillis,difference;
			String lastPulseExternalTime;
			Identity externalDataCurrentPulseIdentity,numberOfPulseForStaleIdentity;
			int secondsToStale=180;
			String valueType;

			for(int i=0;i<currentlyCreatingPulseNuclei.length();i++){
				jsonObject = currentlyCreatingPulseNuclei.getJSONObject(i);
				if(jsonObject.getString("Name").equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					chains = jsonObject.getJSONArray("DeneChains");
					for(int j=0;j<chains.length();j++){
						jsonObjectChain = chains.getJSONObject(j);

						if(jsonObjectChain.toString().length()>10 && jsonObjectChain.getString("Name").equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)){
							denes = jsonObjectChain.getJSONArray("Denes");
							for(int k=0;k<denes.length();k++){
								jsonObjectDene = denes.getJSONObject(k);
								jsonObjectDeneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(jsonObjectDene, TeleonomeConstants.EXTERNAL_DATA_STATUS, TeleonomeConstants.COMPLETE);
								jsonObjectDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE);

								logger.debug("line 1248 disallowexternal data jsonObjectDene  " + jsonObjectDene.getString("Name") + " is stale");
							}
						}
					}
				}
			}
			FileUtils.write(selectedFile, denomeJSONObject.toString(4));
			FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/Teleonome.denome"), denomeJSONObject.toString(4));

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}catch(IOException e){

		}

		return teleonomeToReconnect;
	}


	class IntegerCompare implements Comparator<Map.Entry<?, Integer>>{
		public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}


	public DeneWord getDeneWordByName(String nucleusName, String deneChainName, String deneName, String deneWordName){
		return null;
	}
	public JSONArray getDeneByDeneType(String nucleusName,String deneChainName, String deneType) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		JSONArray toReturn = new JSONArray();
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
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

	public HashMap mapDeneWordsToPointers(JSONArray deneWordPointers){
		HashMap toReturn = new HashMap();
		JSONObject deneWord;
		for(int i=0;i<deneWordPointers.length();i++){
			try {
				deneWord = getDeneWordByIdentity(new Identity(deneWordPointers.getString(i)));
				logger.debug("deneWordPointers.getString(i)=" + deneWordPointers.getString(i) + " deneWord=" + deneWord);
				toReturn.put(deneWordPointers.getString(i),deneWord);
			} catch (InvalidDenomeException | JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}
		return toReturn;
	}

	public JSONArray renderDeneWordsFromPointers(JSONArray deneWordPointers){
		JSONArray toReturn = new JSONArray();
		JSONObject deneWord;
		for(int i=0;i<deneWordPointers.length();i++){
			try {
				deneWord = getDeneWordByIdentity(new Identity(deneWordPointers.getString(i)));
				logger.debug("deneWordPointers.getString(i)=" + deneWordPointers.getString(i) + " deneWord=" + deneWord);
				toReturn.put(deneWord);
			} catch (InvalidDenomeException | JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}
		return toReturn;
	}

	public JSONArray loadDenesFromPointers(JSONArray denePointers){
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<denePointers.length();i++){
			try {
				logger.debug("loading dene from pointer " + denePointers.getString(i));
				toReturn.put(getDeneByIdentity(new Identity(denePointers.getString(i))));
			} catch (InvalidDenomeException | JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}
		return toReturn;
	}

	public Object getDeneWordAttributeByIdentity(Identity identity, String whatToBring) throws InvalidDenomeException, JSONException{
		JSONObject deneWord = getDeneWordByIdentity( identity);
		if(deneWord==null)return null;
		if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
			return deneWord;
		}else{
			return deneWord.get(whatToBring);
		}
	}

	/**
	 *  note that if the identity refers to a command like $Current_Time_Millis the deneword created will not have the correct name
	 *  since it will have that command, this would have to be corrected by the calling class, ie replace the "Name" attribute
	 * @param identity
	 * @return
	 * @throws InvalidDenomeException
	 */
	public JSONObject getDeneWordByIdentity(Identity identity) throws InvalidDenomeException{
		//
		// if we are pointing at itself return the default
		if(identity.isCommand()){
			if(identity.getCommandValue().equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
				try {
					long pulseTimeInMillis = currentlyCreatingPulseJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
					Unit millisecondUnit=SI.MILLI(SI.SECOND);
					JSONObject renderedDeneObject = Utils.createDeneWordJSONObject(TeleonomeConstants.CURRENT_TIMESTAMP_VARIABLE_NAME, ""+pulseTimeInMillis, millisecondUnit.toString(), TeleonomeConstants.DATATYPE_LONG, true);
					return renderedDeneObject;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				return null;
			}
		}else{
			return getDeneWordByPointer(identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName(), identity.getDeneWordName());
		}
		return null;
	}
	public boolean hasDeneWordByIdentity(Identity identity) throws InvalidDenomeException{
		return hasDeneWordByIdentity(identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName(), identity.getDeneWordName());
	}
	public boolean hasDeneWordByIdentity(String nucleusName,String deneChainName, String deneName, String deneWordName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {
			logger.debug("hasDeneWordByIdentity, nucleusName="  + nucleusName +" deneChainName=" + deneChainName + " deneName=" + deneName  + " deneWordName="  + deneWordName);
			//
			// now parse them
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject;
			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//logger.debug("name=" + name);
				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					internalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					purposeNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					mnemosyneNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
					humanInterfaceNucleus = aJSONObject;
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
				//	logger.debug("getDeneWordByPointer inside denechain, " + aJSONObject.getString("Name") +" " + deneChainName);


				//logger.debug(aJSONObject);

				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					//logger.debug("getDeneWordByPointer1 , deneChainName " + deneChainName + " has " + denesJSONArray.length() + " denes");



					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						//	logger.debug("getDeneWordByPointer inside denes, aDeneJSONObject.getString(Name)=" + aDeneJSONObject.getString("Name"));
						if(aDeneJSONObject.getString("Name").equals(deneName)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							//	logger.debug("getDeneWordByPointer found dene,");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								//	logger.debug("getDeneWordByPointer inside denewords");
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.getString("Name").equals(deneWordName)){
									return true;
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
		return false;
	}

	public JSONObject getDeneWordByPointer(String nucleusName,String deneChainName, String deneName, String deneWordName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {
			logger.debug("getDeneWordByPointer, nucleusName="  + nucleusName +" deneChainName=" + deneChainName + " deneName=" + deneName  + " deneWordName="  + deneWordName);
			//
			// now parse them
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject;
			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//logger.debug("name=" + name);
				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					internalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					purposeNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					mnemosyneNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
					humanInterfaceNucleus = aJSONObject;
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
				//	logger.debug("getDeneWordByPointer inside denechain, " + aJSONObject.getString("Name") +" " + deneChainName);


				//logger.debug(aJSONObject);

				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					//logger.debug("getDeneWordByPointer1 , deneChainName " + deneChainName + " has " + denesJSONArray.length() + " denes");



					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						//	logger.debug("getDeneWordByPointer inside denes, aDeneJSONObject.getString(Name)=" + aDeneJSONObject.getString("Name"));
						if(aDeneJSONObject.getString("Name").equals(deneName)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							//	logger.debug("getDeneWordByPointer found dene,");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								//	logger.debug("getDeneWordByPointer inside denewords");
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.getString("Name").equals(deneWordName)){
									return aDeneWordJSONObject;
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

	public Object getTimeSeriesElementByPointer(Identity identity, String whatToReturn) throws InvalidDenomeException{
		//
		// if we are pointing at itself return the default
		if(identity.isCommand()){
			if(identity.getCommandValue().equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
				try {
					long pulseTimeInMillis = currentlyCreatingPulseJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
					Unit millisecondUnit=SI.MILLI(SI.SECOND);
					JSONObject renderedDeneObject = Utils.createDeneWordJSONObject(TeleonomeConstants.CURRENT_TIMESTAMP_VARIABLE_NAME, ""+pulseTimeInMillis, millisecondUnit.toString(), TeleonomeConstants.DATATYPE_LONG, true);
					return renderedDeneObject;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				return null;
			}
		}else{
			return getTimeSeriesElementByPointer(identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName(), identity.getDeneWordName(), identity.getTimeSeriesElementPosition(), whatToReturn);
		}
		return null;
	}

	public Object getTimeSeriesElementByPointer(String nucleusName,String deneChainName, String deneName, String deneWordName, int elementPointer, String whatToReturn) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {
			logger.debug("getDeneWordByPointer, nucleusName="  + nucleusName +" deneChainName=" + deneChainName + " deneName=" + deneName  + " deneWordName="  + deneWordName);
			//
			// now parse them
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject;
			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//logger.debug("name=" + name);
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
				logger.debug("getDeneWordByPointer inside denechain, " + aJSONObject.getString("Name") +" " + deneChainName);


				//logger.debug(aJSONObject);

				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					logger.debug("getDeneWordByPointer, deneChainName " + deneChainName + " has " + denesJSONArray.length() + " denes");

					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						logger.debug("getDeneWordByPointer inside denes, aDeneJSONObject.getString(Name)=" + aDeneJSONObject.getString("Name"));
						if(aDeneJSONObject.getString("Name").equals(deneName)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							logger.debug("getDeneWordByPointer found dene,");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								logger.debug("getDeneWordByPointer inside denewords");
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.getString("Name").equals(deneWordName)){
									JSONArray elementsJSONArray = aDeneWordJSONObject.getJSONArray(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
									if(whatToReturn.equals(TeleonomeConstants.COMPLETE_TIMESERIES_ELEMENT)) {
										return elementsJSONArray.get(elementPointer);
									}else if(whatToReturn.equals(TeleonomeConstants.TIMESERIES_TIMESTAMP)) {
										JSONObject o =  (JSONObject) elementsJSONArray.get(elementPointer);
										return o.getLong(TeleonomeConstants.DATATYPE_TIMESTAMP_MILLISECONDS);
									}else if(whatToReturn.equals(TeleonomeConstants.TIMESERIES_VALUE)) {
										JSONObject o =  (JSONObject) elementsJSONArray.get(elementPointer);
										return o.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
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


	public JSONObject getDeneChainByName(String nucleusName, String deneChainName) throws JSONException{
		JSONArray deneChainsArray=null;
		if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
			deneChainsArray = internalNucleus.getJSONArray("DeneChains");
		}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
			deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
		}
		JSONObject aDeneChain;
		for(int j=0;j<deneChainsArray.length();j++){
			aDeneChain = deneChainsArray.getJSONObject(j);
			if(aDeneChain.get("Name").equals(deneChainName)){
				return aDeneChain;
			}
		}
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
	/**
	 * returns all the denes that have the same identity.  In the mnemosyne, you can have multiple
	 * denes with the same identity where
	 * @param identity
	 * @return
	 * @throws InvalidDenomeException
	 */
	public JSONArray getAllDenesByIdentity(Identity identity) throws InvalidDenomeException{
		return getAllDenesByPointer(identity.getTeleonomeName(), identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName());
	}

	public JSONArray getAllDenesByPointer(String teleonomeName, String nucleusName,String deneChainName, String deneName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		JSONArray toReturn = new JSONArray();
		logger.debug("getdenebyidentity, nucleusName=" + nucleusName + " deneChainName=" + deneChainName + " deneName=" + deneName);
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
				//	logger.debug("getdenebyidentity point 1 denechain " + aJSONObject.getString("Name"));
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					//logger.debug("getdenebyidentity point 2, denesJSONArray.length()=" + denesJSONArray.length());

					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						//logger.debug("getdenebyidentity point3 " + aDeneJSONObject.getString("Name"));

						if(aDeneJSONObject.getString("Name").equals(deneName)){
							toReturn.put(aDeneJSONObject);
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

	public JSONObject getNucleiByIdentity(Identity identity) throws InvalidDenomeException{
		return getNucleiByPointer(identity.getTeleonomeName(), identity.getNucleusName());
	}

	public JSONObject getNucleiByPointer(String teleonomeName, String nucleusName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		JSONObject toReturn = new JSONObject();
		logger.debug("getNucleiByPointer, nucleusName=" + nucleusName );
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				toReturn = internalNucleus;
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				toReturn = purposeNucleus ;
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				toReturn = mnemosyneNucleus ;
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				toReturn = humanInterfaceNucleus ;
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

	public JSONArray getDenesFromDeneChainByIdentity(Identity deneChainIdentity) throws JSONException, InvalidDenomeException{
		JSONObject deneChainJSONObject = getDeneChainByIdentity(deneChainIdentity);
		return deneChainJSONObject.getJSONArray("Denes");
	}

	public JSONObject getDeneChainByIdentity(Identity identity) throws InvalidDenomeException{
		return getDeneChainByPointer(identity.getNucleusName(),identity.getDenechainName());
	}

	public JSONObject getDeneChainByPointer(String nucleusName,String deneChainName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		logger.debug("getDeneChainByPointer, nucleusName=" + nucleusName + " deneChainName=" + deneChainName);
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


			JSONObject aJSONObject;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//	logger.debug("getdenebyidentity point 1 denechain " + aJSONObject.getString("Name"));
				if(aJSONObject.getString("Name").equals(deneChainName)){
					return aJSONObject;
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
		logger.debug("getdenebyidentity, nucleusName=" + nucleusName + " deneChainName=" + deneChainName + " deneName=" + deneName);
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//	logger.debug("getdenebyidentity point 1 denechain " + aJSONObject.getString("Name"));
				if(aJSONObject.getString("Name").equals(deneChainName)){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					//logger.debug("getdenebyidentity point 2, denesJSONArray.length()=" + denesJSONArray.length());

					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						//logger.debug("getdenebyidentity point3 " + aDeneJSONObject.getString("Name"));

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
		//
		// if the data needed is the Pulse timestamp,, it would be expressed as Ra:Pulse Timestamp
		// so check to see if this is the case
		// 
		String[] tokens = deneWordPointer.split(":");
		if(tokens.length==2){
			//
			// we need an atribute from the object
			return externalTeleonomePulse.get(tokens[1]);
		}

		JSONObject externalDenomeJSONObject = externalTeleonomePulse.getJSONObject("Denome");

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

	public void storeMutationEvent(JSONObject mutationEventJSONObject) throws PersistenceException{
		aDBManager.storeMutationEvent(mutationEventJSONObject);
	}

	public void storePulse(long timestampInMills, String pulse) throws PersistenceException{
		aDBManager.storePulse(timestampInMills,pulse);
	}

	public void storeOrganismPulse(String teleonome, String teleonomeAddress, String pulse, String status,String operationMode, String identity, long timeStampMillis) throws PersistenceException{
		aDBManager.storeOrganismPulse(teleonome,teleonomeAddress,pulse, status,  operationMode,  identity, timeStampMillis);
	}

	public JSONObject getLastPulse(){
		return aDBManager.getLastPulse();
	}

	public JSONObject getLastPulse(String teleonomName){
		return aDBManager.getLastPulse(teleonomName);
	}


	public Vector<Teleonome> getAllTeleonomes(){
		return aDBManager.getAllTeleonomes();
	}


	public JSONObject getDenomeJSONObject(){
		return denomeJSONObject;
	}

	public boolean registerTeleonome(String teleonomeName, String status, String operationMode, String identity, String networkName, String teleonomeAddress){
		return aDBManager.registerTeleonome(  teleonomeName,   status,  operationMode,  identity,  networkName,  teleonomeAddress);
	}
	public boolean markAllNonExecutedAsSkipped(){
		return aDBManager.markAllNonExecutedAsSkipped();
	}
	public void markCommandCompleted(int id){
		aDBManager.markCommandCompleted( id);
	}
	public CommandRequest getNextCommandToExecute(){
		return aDBManager.getNextCommandToExecute();
	}

	public void getNextCommandToExecute(int id){
		aDBManager.markCommandCompleted(id);
	}

	public JSONObject getDenomicElementByIdentity(Identity identity){

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
				if(deneChainName.equals(""))return mnemosyneNucleus;
				deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				if(deneChainName.equals(""))return humanInterfaceNucleus;
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
								if(aDeneWordJSONObject.get("Name").equals(deneWordName)){
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

	public JSONObject injectPayloadIntoStateMutation(JSONObject payload) throws IOException, InvalidMutation{
		//
		// this is the read from the menome way
		//
		JSONArray mutationsJSONArray;
		JSONObject selectedMutationJSONObject=null;
		try {
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			String mutationName = payload.getString("Mutation Name");
			logger.debug("line 1950 of inject, mutationName=" + mutationName);

			JSONArray updatesJSONArray;
			mutationsJSONArray = denomeObject.getJSONArray("Mutations");
			JSONObject mutationJSONObject, payloadJSONObject;
			String updateTargetPointer;
			Object updateTargetValue;
			String[] tokens;
			String targetDeneChain,targetDene,targetDeneWord;
			for(int i=0;i<mutationsJSONArray.length();i++){
				mutationJSONObject = (JSONObject) mutationsJSONArray.getJSONObject(i);
				logger.debug("line 1965 of inject, mutationJSONObject=" + mutationJSONObject.getString("Name") + " " + mutationJSONObject.getString("Mutation Type"));

				if(mutationJSONObject.getString("Name").equals(mutationName) && mutationJSONObject.getString(TeleonomeConstants.MUTATION_TYPE_ATTRIBUTE).equals(TeleonomeConstants.MUTATION_TYPE_STATE)){

					//
					// the next code will mutate this mutation by setting the values
					// that this mutation will use when executed
					// this is the way that a human will pass parameters to denom
					// the payload 
					payloadJSONObject = payload.getJSONObject("Payload");
					updatesJSONArray = payloadJSONObject.getJSONArray("Updates");
					logger.debug("line 1965 of inject, payloadJSONObject=" + payloadJSONObject);

					JSONObject updateJSNObject;
					for(int j=0;j<updatesJSONArray.length();j++){
						updateJSNObject = updatesJSONArray.getJSONObject(j);
						//
						// each update object has two parameters, the target and the value
						updateTargetPointer = updateJSNObject.getString("Target");
						updateTargetValue = updateJSNObject.get("Value");
						logger.debug("line 1982 demomemanager updateTargetPointer:" + updateTargetPointer + " updateTargetValue:" + updateTargetValue);
						//
						// the target contains a pointer which is relative to the mutation
						// ie the path begins with the name of the denechain in the mutation object
						// as an example would be
						// "Target":"@On Load:Update Only When In Float:Update Only When In Float",
						//
						// so taht
						tokens = updateTargetPointer.split(":");
						targetDeneChain = tokens[0];
						if(targetDeneChain.startsWith("@"))targetDeneChain = targetDeneChain.substring(1);
						targetDene = tokens[1];
						targetDeneWord = tokens[2];
						logger.debug("line 1993 demomemanager targetDeneChain:" + targetDeneChain + " targetDene:" + targetDene + " targetDeneWord:" + targetDeneWord);

						//
						// now loop over all the object of the target mutation
						JSONArray mutationDeneChains, mutationDenes,mutationDeneWords;
						JSONObject mutationDeneChain, mutationDene, mutationDeneWord;

						mutationDeneChains = mutationJSONObject.getJSONArray("DeneChains");
						//logger.debug("line 1560 demomemanager mutationDeneChains:" + mutationDeneChains.length());

						for(int k=0;k<mutationDeneChains.length();k++){
							mutationDeneChain=mutationDeneChains.getJSONObject(k);
							logger.debug("line 2005 demomemanager mutationDeneChain.getString(Name):" + mutationDeneChain.getString("Name"));

							if(mutationDeneChain.getString("Name").equals(targetDeneChain)){
								mutationDenes = mutationDeneChain.getJSONArray("Denes");
								for(int l=0;l<mutationDenes.length();l++){
									mutationDene = mutationDenes.getJSONObject(l);
									//	logger.debug("line 1566 demomemanager mutationDene:" + mutationDene.getString("Name"));

									if(mutationDene.getString("Name").equals(targetDene)){
										mutationDeneWords=mutationDene.getJSONArray("DeneWords");
										for(int m=0;m<mutationDeneWords.length();m++){
											mutationDeneWord = mutationDeneWords.getJSONObject(m);
											logger.debug("line 1958 demomemanager mutationDeneWord:" + mutationDeneWord.getString("Name"));



											if(mutationDeneWord.getString("Name").equals(targetDeneWord)){
												logger.debug("line 1963 demomemanager updating value:" + updateTargetValue + " mutationDeneWord=" + mutationDeneWord);

												mutationDeneWord.put("Value",updateTargetValue);
											}
										}
									}
								}
							}
						}

					}
					selectedMutationJSONObject=mutationJSONObject;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		//	logger.debug("Finished injecting payload, exiting for now");
		//	System.exit(0);
		return selectedMutationJSONObject;

	}


	public JSONObject readImmediateMutation(String mutationName) throws IOException, InvalidMutation{
		//
		// this is the read from the menome way
		//
		JSONArray mutationsJSONArray;
		try {
			//
			// need to reload the denome again because if the mutation had a payload
			// it would have modified the denome and written the modification to disk

			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");

			mutationsJSONArray = denomeObject.getJSONArray("Mutations");
			JSONObject mutationJSONObject;
			for(int i=0;i<mutationsJSONArray.length();i++){
				mutationJSONObject = (JSONObject) mutationsJSONArray.getJSONObject(i);
				if(mutationJSONObject.getString("Name").equals(mutationName) && mutationJSONObject.getString("Mutation Type").equals(TeleonomeConstants.MUTATION_TYPE_STATE)){
					return mutationJSONObject;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		return null;
	}


	public JSONArray getTimeBasedMutations() {
		JSONArray timeBasedMutationsJSONArray = new JSONArray();

		JSONObject denomeObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
		JSONArray mutationsJSONArray = denomeObject.getJSONArray("Mutations");
		JSONObject mutationJSONObject, payloadJSONObject;
		String updateTargetPointer;
		Object updateTargetValue;
		String[] tokens;
		String targetDeneChain,targetDene,targetDeneWord;
		for(int i=0;i<mutationsJSONArray.length();i++){
			mutationJSONObject = (JSONObject) mutationsJSONArray.getJSONObject(i);
			if(mutationJSONObject.has(TeleonomeConstants.MUTATION_INVOCATION_MODE_ATTRIBUTE) && mutationJSONObject.getString(TeleonomeConstants.MUTATION_INVOCATION_MODE_ATTRIBUTE).equals(TeleonomeConstants.MUTATION_INVOCATION_MODE_TIME)){
				timeBasedMutationsJSONArray.put(mutationJSONObject);
			}	
		}
		return timeBasedMutationsJSONArray;
	}



	public void executeMnemosyneOperations(JSONArray mnemosyneDenes) throws IOException, InvalidMutation, InvalidDenomeException, JSONException{


		ArrayList<Entry<JSONObject, Integer>> mnemosyneOperationExecutionPositionDeneIndex = getActuatorExecutionPositionDeneIndex();

		JSONObject mnemosyneDene;
		int executionPosition;
		ArrayList<Map.Entry<JSONObject, Integer>> mnemosyneOperationsExecutionPositionIndex = new ArrayList();
		logger.debug("line 2439 mnemosyneDenes size=" + mnemosyneDenes.length());

		for(int i=0;i<mnemosyneDenes.length();i++){
			mnemosyneDene = mnemosyneDenes.getJSONObject(i);
			executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene,"Execution Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			mnemosyneOperationsExecutionPositionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(mnemosyneDene, new Integer(executionPosition)));
		}
		Collections.sort(mnemosyneOperationsExecutionPositionIndex, new IntegerCompare());
		//
		// now execute each operation
		//
		String deneType, newDeneName, destinationIdentityPointer;
		JSONObject destinationDeneChain;
		JSONArray destinationDenes;
		String deneWordPointer;
		JSONObject deneWordToCopy;
		Identity targetIdentity;

		JSONArray targetsJSONArray = null;
		JSONObject newDene = null;
		JSONArray newDeneDeneWords = null;
		JSONArray copyDeneWordPointersJSONArray = null;
		int newDenePosition;

		long currentTimeMillis = System.currentTimeMillis();
		Instant instant = Instant.ofEpochMilli(currentTimeMillis);
		Identity timeZoneIdentity = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_VITAL, "Timezone");
		String timeZoneId = (String) getDeneWordAttributeByIdentity(timeZoneIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		TimeZone currentTimeZone = null;
		if(timeZoneId!=null && !timeZoneId.equals("")){
			currentTimeZone = TimeZone.getTimeZone(timeZoneId);
		}else{
			currentTimeZone = TimeZone.getDefault();
		}

		LocalDateTime ldt = LocalDateTime.ofInstant(instant, currentTimeZone.toZoneId());

		LocalDateTime currentTime = LocalDateTime.now();

		DateTimeFormatter timeStampformatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
		String formatedCurrentTimestamp = currentTime.format(timeStampformatter);

		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_DATE_FORMAT);
		String formatedCurrentDate = currentTime.format(dateFormatter);

		DateTimeFormatter timeformatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIME_FORMAT);
		String formatedCurrentTime = currentTime.format(timeformatter);


		logger.debug("line 2479 mnemosyneOperationsExecutionPositionIndex size=" + mnemosyneOperationsExecutionPositionIndex.size());
		for (Map.Entry<JSONObject, Integer> entry : mnemosyneOperationsExecutionPositionIndex) {
			mnemosyneDene = entry.getKey();
			deneType = mnemosyneDene.getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE);
			logger.debug("line 2504 deneType =" + deneType);
			if(deneType.equals(TeleonomeConstants.MNEMOSYNE_CREATE_DENE_OPERATION)){
				//
				// list the files and create a dene for each one
				// there can be multiple targets, ie you can add a dene to 
				// the current hour, current day etc
				// so first get the targets, which are identity pointers that point to the chain where this new dene
				//needs to be created
				//
				targetsJSONArray = getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				//
				// ok, now we have the targets, and we know we need to create a Dene, so first create a Dene
				// add all the DeneWords and then add the Dene to the Target
				// each target looks like:
				//   "@MonoNanny:Mnemosyne:Mnemosyne Today",
				//


				for(int j=0;j<targetsJSONArray.length();j++){
					destinationIdentityPointer = targetsJSONArray.getString(j);
					logger.debug("destinationIdentityPointer=" + destinationIdentityPointer);
					destinationDeneChain = getDenomicElementByIdentity(new Identity(destinationIdentityPointer));
					destinationDenes = destinationDeneChain.getJSONArray("Denes");
					newDenePosition=0;
					if(destinationDenes==null){
						destinationDenes=new JSONArray();
						destinationDeneChain.put("Denes", destinationDenes);
						newDenePosition=1;
					}

					newDene = new JSONObject();
					newDeneDeneWords = new JSONArray();
					newDene.put("DeneWords", newDeneDeneWords);
					//
					// the name of this new dene is a deneword where the name is "New Dene Name'
					newDeneName = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_NEW_DENE_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					newDene.put("Name", newDeneName);
					//
					// now that we have the name, look for the next position, this means, go to the chain
					// and see what is the position of the last copy of that dene that we have in the chain
					// destinationDenes could have several copies of the same dene
					// for example, if this is a daily, and you are running the process every hour
					// and is 8am, by the time you are adding a new dene, there are already 7 denes with the same
					//name, but what distinguish them is that each dene has a an attribute called "Position"
					// that way when we want to make reports or operationswe know which is the last value

					newDenePosition = getNextPostionForDeneInMnemosyneChain(destinationDeneChain, newDeneName);
					newDene.put("Position", newDenePosition);
					//
					//
					// also add when this dene was created


					newDene.put("Timestamp", formatedCurrentTime);
					newDene.put("Timestamp Milliseconds", currentTimeMillis);

					//
					// now get the directory where the files are located:
					//

					// Now add denewords to this new dene, you can either copy an existing deneword or create a new one from scratch
					copyDeneWordPointersJSONArray = getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_COPY_DENEWORD, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//
					// the above array will contain pointers to areas in the denome, we want the whole DenWord so


					for(int i=0;i<copyDeneWordPointersJSONArray.length();i++){
						deneWordPointer = copyDeneWordPointersJSONArray.getString(i);
						deneWordToCopy  = this.getDeneWordByIdentity(new Identity(deneWordPointer));
						//
						// as an added attribute, store the pointer as a "Source" Attribute
						deneWordToCopy.put("Source", deneWordPointer);
						//
						// now add this deneword to the new dene
						//
						newDeneDeneWords.put(deneWordToCopy);
					}

					//
					// finally add the new dene to the chain
					destinationDenes.put(newDene);
				}

			}else if(deneType.equals(TeleonomeConstants.MNEMOSYNE_LIST_FILE_INFO_OPERATION)){	
				//
				// we are going to create a new Dene andd it to the mnemosyne
				// there can be multiple targets, ie you can add a dene to 
				// the current hour, current day etc
				// so first get the targets, which are identity pointers that point to the chain where this new dene
				//needs to be created
				//
				targetsJSONArray = getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				//
				// ok, now we have the targets, and we know we need to create a Dene, so first create a Dene
				// add all the DeneWords and then add the Dene to the Target
				// each target looks like:
				//   "@MonoNanny:Mnemosyne:Mnemosyne Today",
				//
				JSONObject fileLastModifiedOnDeneWord, fileSizeDeneWord,fileNameDeneWord;

				String pathToFiles = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_FILE_LIST_PATH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				//
				// the name of this new dene is a deneword where the name is "New Dene Name'
				newDeneName = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_NEW_DENE_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				File pathToFilesDir = new File(pathToFiles);
				File[] files=pathToFilesDir.listFiles();
				File aFile;
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
				Instant fileInstant;
				String formatedFileLastModifiedOnTime;
				for(int j=0;j<targetsJSONArray.length();j++){
					destinationIdentityPointer = targetsJSONArray.getString(j);
					logger.debug("destinationIdentityPointer=" + destinationIdentityPointer);
					destinationDeneChain = getDenomicElementByIdentity(new Identity(destinationIdentityPointer));
					destinationDenes = destinationDeneChain.getJSONArray("Denes");
					newDenePosition=0;
					if(destinationDenes==null){
						destinationDenes=new JSONArray();
						destinationDeneChain.put("Denes", destinationDenes);
						newDenePosition=1;
					}

					//
					// for this target list the files and create a dene for each one
					//
					for(int k=0;k<files.length;k++){
						aFile = files[k];
						newDene = new JSONObject();
						newDeneDeneWords = new JSONArray();
						newDene.put("DeneWords", newDeneDeneWords);
						newDene.put("Name", newDeneName);
						//
						// now that we have the name, look for the next position, this means, go to the chain
						// and see what is the position of the last copy of that dene that we have in the chain
						// destinationDenes could have several copies of the same dene
						// for example, if this is a daily, and you are running the process every hour
						// and is 8am, by the time you are adding a new dene, there are already 7 denes with the same
						//name, but what distinguish them is that each dene has a an attribute called "Position"
						// that way when we want to make reports or operationswe know which is the last value

						newDenePosition = getNextPostionForDeneInMnemosyneChain(destinationDeneChain, newDeneName);
						newDene.put("Position", newDenePosition);
						//
						//
						// also add when this dene was created


						newDene.put("Timestamp", formatedCurrentTime);
						newDene.put("Timestamp Milliseconds", currentTimeMillis);

						//
						// now add the following 3 denewords to this dene
						//
						// 1)The deneword that represents the file name
						//
						fileNameDeneWord = Utils.createDeneWordJSONObject("File Name", aFile.getName() ,null,"String",true);
						newDeneDeneWords.put(fileNameDeneWord);
						//
						// 2)The deneword that represents the file size
						//
						fileSizeDeneWord = Utils.createDeneWordJSONObject("File Size", aFile.length()/1024 ,"kb","String",true);
						newDeneDeneWords.put(fileSizeDeneWord);
						//
						// 3)The deneword that represents the file last modified date
						//
						fileInstant = Instant.ofEpochMilli(aFile.lastModified());
						LocalDateTime fileLastMofiedOnLocalDateTime = LocalDateTime.ofInstant(fileInstant, currentTimeZone.toZoneId());
						formatedFileLastModifiedOnTime = fileLastMofiedOnLocalDateTime.format(formatter);
						fileLastModifiedOnDeneWord = Utils.createDeneWordJSONObject("Last Modified On", formatedFileLastModifiedOnTime ,null,"String",true);
						newDeneDeneWords.put(fileLastModifiedOnDeneWord);
						//
						// finally add the new dene to the chain
						destinationDenes.put(newDene);

					}
				} // for targets

			}else if(deneType.equals(TeleonomeConstants.MNEMOSYNE_ADD_DENEWORD_TO_DENE_OPERATION)){
				//
				// get the operation to see what we are doing
				// 
				JSONObject selectedDene=null; 
				String operation = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_OPERATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				//
				// the next few lines are common to all the different
				// type of operations in the add_deneword_to_dene
				//
				// we are adding a new deneword to an existing dene
				// get that existing dene first.  the address of the dene is
				// located as a value in the deneword of type Mnemosyne Target
				logger.debug("mnemosyneDene=" + mnemosyneDene.toString(4));

				String targetDeneIdentityPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("line 2576 targetDeneIdentityPointer=" + targetDeneIdentityPointer);
				Identity identity = new Identity(targetDeneIdentityPointer);
				Identity deneChainIdentity = new Identity(identity.getTeleonomeName(), identity.getNucleusName(), identity.getDenechainName());
				String deneName = identity.getDeneName();
				JSONObject deneChain = this.getDenomicElementByIdentity(deneChainIdentity);
				//
				// The potentialDenes is an array with all the denes, so now identity which is the dene we want
				// besides the actuall dene, we need which copy of the dene, since there could be many denes with the same name
				// if we are storing one every hour, at noon there would be 12 copies of the same dene in the today chain
				// so we need to specify which copy of the dene we want to use.  for this we use need to get the 
				// deneword called MNEMOSYNE_DENEWORD_TARGET_POSITION, which could be a number or a variable like 
				// COMMAND_MNEMOSYNE_LAST_DENE_POSITION
				String targetPosition= (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_TARGET_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONArray denes = deneChain.getJSONArray("Denes");
				selectedDene = getDeneFromDeneJSONArrayByPostion(denes,  deneName, targetPosition);
				//
				// at this point, selectedDene is the one we need to add a deneword to
				// To simplify the code, as part o the standard, the dene that is being used
				// already contains a deneword which will be used as a template
				// this deneword has the type of Create DeneWord Source
				//
				// so we need to get it and replace the value attribute of this deneword
				// with that value that is calculated by aggregation.  so first get the deneword
				JSONObject createDeneWordSource = (JSONObject) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_CREATE_DENEWORD_SOURCE, TeleonomeConstants.COMPLETE);
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
							newDeneWord.put("Value", getDeneWordAttributeByIdentity(new Identity(ov), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
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
					String aggregateFromDeneWordIdentityPointer = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate From", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("aggregateFromDeneWordIdentityPointer=" + aggregateFromDeneWordIdentityPointer );

					Identity aggregateFromDeneWordIdentity = new Identity(aggregateFromDeneWordIdentityPointer);
					logger.debug( " aggregateFromDeneWordIdentity=" + aggregateFromDeneWordIdentity);
					Identity aggregateFromDeneIdentity = new Identity(aggregateFromDeneWordIdentity.getTeleonomeName(),aggregateFromDeneWordIdentity.getNucleusName(),aggregateFromDeneWordIdentity.getDenechainName(),aggregateFromDeneWordIdentity.getDeneName());

					logger.debug("aggregateFromDeneIdentity=" + aggregateFromDeneIdentity);

					JSONArray allAggregateFromDenes = getAllDenesByIdentity(aggregateFromDeneIdentity);
					//
					// first check to see if there is none, only one or more than one
					// if this is the first aggregation there will be none therefore get the
					// default attribute.  if we are starting, allAggregateFromDenes will
					// have dene ,since it could have been created in a previous operation
					// in the same cycle, but it will not have an actual value which is calculated
					// by this operatin, for this reason, get the default value
					JSONObject aggregateFromDeneWord = (JSONObject) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate From", TeleonomeConstants.COMPLETE);
					Double defaultValue = aggregateFromDeneWord.getDouble("Default");
					logger.debug("defaultValue=" + defaultValue );
					//
					logger.debug("allAggregateFromDenes.klength=" + allAggregateFromDenes.length());

					JSONObject deneToAggregateFrom=null;
					double sourceValue=0;
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
						String aggregateFromDenePosition = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene,  "Aggregate From Dene Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//
						// now use that variable to get the actual dene we are going to use
						//
						deneToAggregateFrom = getDeneFromDeneJSONArrayByPostion(allAggregateFromDenes,  aggregateFromDeneWordIdentity.getDeneName(), aggregateFromDenePosition);

						Object o = getDeneWordAttributeByDeneWordNameFromDene(deneToAggregateFrom, aggregateFromDeneWordIdentity.getDeneWordName(), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

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
					//
					// assume is a double since we are aggregating
					//

					//
					// value
					// 
					String aggregateValueDeneWordIdentityPointer = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate Value", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					Identity aggregateValueDeneWordIdentity = new Identity(aggregateValueDeneWordIdentityPointer);
					Identity aggregateValueDeneIdentity = new Identity(aggregateValueDeneWordIdentity.getTeleonomeName(),aggregateValueDeneWordIdentity.getNucleusName(),aggregateValueDeneWordIdentity.getDenechainName(),aggregateValueDeneWordIdentity.getDeneName());

					JSONArray allAggregateValueDenes = getAllDenesByIdentity(aggregateValueDeneWordIdentity);
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
						String aggregateValueDenePosition = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene,  "Aggregate Value Dene Position", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//
						// now use that variable to get the actual dene we are going to use
						//
						deneToAggregateValue = getDeneFromDeneJSONArrayByPostion(allAggregateValueDenes,  aggregateValueDeneWordIdentity.getDeneName(), aggregateValueDenePosition);
					}
					//
					// asume is a double since we are aggregating
					// but because it can be a deneword from purpose
					// receive it as an object

					Object o = getDeneWordAttributeByDeneWordNameFromDene(deneToAggregateValue, aggregateValueDeneWordIdentity.getDeneWordName(), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

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
			}else if(deneType.equals(TeleonomeConstants.MNEMOSYNE_UPDATE_VALUE_OPERATION)){
				//
				// get the operation to see what we are doing
				// 
				JSONObject selectedDene=null; 
				String operation = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_OPERATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
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
					String function = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Function", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("transofrm function=" + function );

					

					//
					//2) get the data source
					//
					String dataSourceIdentityPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_TYPE_TRANSFORMATION_DATA_SOURCE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("line 2751 targetDeneWordIdentityPointer=" + dataSourceIdentityPointer);
					Identity aggregateValueDeneWordIdentity = new Identity(dataSourceIdentityPointer);
					Object aggregateValueObject = this.getDeneWordAttributeByIdentity(aggregateValueDeneWordIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("line 2778 aggregateValueObject=" + aggregateValueObject + " class=" + aggregateValueObject.getClass().toString());

					

					//
					// 3)target 
					//
					String targetDeneWordIdentityPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("line 2751 targetDeneWordIdentityPointer=" + targetDeneWordIdentityPointer);
					Identity targetDeneWordIdentity = new Identity(targetDeneWordIdentityPointer);
					JSONObject targetDeneWord = (JSONObject) getDeneWordAttributeByIdentity(targetDeneWordIdentity,  TeleonomeConstants.COMPLETE);
					//
					// now use the identity of the deneword to get the dene, we need this because we need to update the 
					// timestamp
					Identity targetDeneIdentity  = new Identity(targetDeneWordIdentity.getTeleonomeName(),targetDeneWordIdentity.getNucleusName(), targetDeneWordIdentity.getDenechainName(), targetDeneWordIdentity.getDeneName());
					JSONObject targetDene = getDeneByIdentity(targetDeneIdentity);
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
					String aggregateFromDeneWordIdentityPointer = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate From", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("aggregateFromDeneWordIdentityPointer=" + aggregateFromDeneWordIdentityPointer );

					Identity aggregateFromDeneWordIdentity = new Identity(aggregateFromDeneWordIdentityPointer);
					Object aggregateFromValueObject =  this.getDeneWordAttributeByIdentity(aggregateFromDeneWordIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
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
					String aggregateValueDeneWordIdentityPointer = (String) getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, "Aggregate Value", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					Identity aggregateValueDeneWordIdentity = new Identity(aggregateValueDeneWordIdentityPointer);
					Object aggregateValueObject = this.getDeneWordAttributeByIdentity(aggregateValueDeneWordIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
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
					String targetDeneWordIdentityPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("line 2751 targetDeneWordIdentityPointer=" + targetDeneWordIdentityPointer);
					Identity targetDeneWordIdentity = new Identity(targetDeneWordIdentityPointer);
					JSONObject targetDeneWord = (JSONObject) getDeneWordAttributeByIdentity(targetDeneWordIdentity,  TeleonomeConstants.COMPLETE);
					//
					// now use the identity of the deneword to get the dene, we need this because we need to update the 
					// timestamp
					Identity targetDeneIdentity  = new Identity(targetDeneWordIdentity.getTeleonomeName(),targetDeneWordIdentity.getNucleusName(), targetDeneWordIdentity.getDenechainName(), targetDeneWordIdentity.getDeneName());
					JSONObject targetDene = getDeneByIdentity(targetDeneIdentity);
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
			}else if(deneType.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_UPDATE_TIMESERIES_COUNTER)){
				//
				// in this case we need first get the data source

				//
				// the source of the data
				String dataSourcePointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_SOURCE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				String dataSourceValueType = (String) getDeneWordAttributeByIdentity(new Identity(dataSourcePointer), TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);

				//
				// the new object that willgo into the timeseries
				//
				JSONObject newValueJSONObject=new JSONObject();
				newValueJSONObject.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, getcurrentlyCreatingPulseTimestampMillis());
				Object o = this.getDeneWordAttributeByIdentity(new Identity(dataSourcePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("dataSourcePointer=" + dataSourcePointer + " dataSourceValue=" + o);
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

				int counterLimit = (int) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_LIMIT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				String dataPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject dataDeneWord = (JSONObject) getDeneWordAttributeByIdentity(new Identity(dataPointer), TeleonomeConstants.COMPLETE);
				//
				// the datJSONArray contains the actual timeseries array
				//
				JSONArray dataJSONArray =  dataDeneWord.getJSONArray(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


				String counterPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject counterDeneWord = (JSONObject) getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);

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

			}else if(deneType.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_CONVERT_DENEWORDS_TO_JSONARRAY)) {


				//
				// the destination is a deneword
				String dataDestinationPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_OPERATION_DESTINATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject dataDestinationDeneWord = (JSONObject)getDeneWordAttributeByIdentity(new Identity(dataDestinationPointer), TeleonomeConstants.COMPLETE);
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
						JSONObject destDeneChain= this.getDeneChainByIdentity(destIdentity);
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
				String dataSourcePointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_DATA_SOURCE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject dataSourceDene = (JSONObject)getDeneByIdentity(new Identity(dataSourcePointer));
				logger.debug("dataSourcePointer="+dataSourcePointer+ " dataDestinationDeneWord=" + dataSourceDene);

				//
				// create an array og jsonobects and set it as a value in the destination
				JSONArray deneWords = dataSourceDene.getJSONArray("DeneWords");
				dataDestinationDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, deneWords);

			}else if(deneType.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_COPY_TIMESERIES_ELEMENT_TO_TIMESERIES)){
				//
				// in this case we need first get the data source

				//
				// the source of the data
				String dataSourcePointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_SOURCE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONArray dataSourceValue = (JSONArray)getDeneWordAttributeByIdentity(new Identity(dataSourcePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				//
				// get the selector
				String selector = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_ELEMENT_SELECTOR, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

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
				int counterLimit = (int) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_LIMIT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				String dataPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_DATA_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject dataDeneWord = (JSONObject) getDeneWordAttributeByIdentity(new Identity(dataPointer), TeleonomeConstants.COMPLETE);
				//
				// the datJSONArray contains the actual timeseries array
				//
				JSONArray dataJSONArray =  dataDeneWord.getJSONArray(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


				String counterPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_TIMESERIES_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject counterDeneWord = (JSONObject) getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);

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

			}else if(deneType.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_UPDATE_COUNTER)) {
				//
				// the counter limit can either be directly an int or it can point to another deneword that contains an int
				//

				JSONObject counterLimitDeneWord = (JSONObject) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_COUNTER_LIMIT, TeleonomeConstants.COMPLETE);
				String counterLimitDeneWordValueType = counterLimitDeneWord.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
				int counterLimit=-1;
				if(counterLimitDeneWordValueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)) {
					//
					// this is a pointer so render it
					String counterLimitDeneWordPointer = counterLimitDeneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					counterLimit = (int)getDeneWordAttributeByIdentity(new Identity(counterLimitDeneWordPointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}else {
					//
					// is just an int so get it directly
					counterLimit = counterLimitDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}


				JSONObject counterIncrementDeneWord = (JSONObject) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_COUNTER_INCREMENT, TeleonomeConstants.COMPLETE);
				String counterIncrementDeneWordValueType = counterIncrementDeneWord.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
				int counterIncrement=-1;
				if(counterIncrementDeneWordValueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)) {
					//
					// this is a pointer so render it
					String counterIncrementDeneWordPointer = counterIncrementDeneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					counterIncrement = (int)getDeneWordAttributeByIdentity(new Identity(counterIncrementDeneWordPointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}else {
					//
					// is just an int so get it directly
					counterIncrement = counterIncrementDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}


				String counterPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				JSONObject counterDeneWord = (JSONObject) getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);
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


			}else if(deneType.equals(TeleonomeConstants.DENE_TYPE_MNEMOSYNE_OPERATION_RESET_COUNTER)) {

				String counterPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION_COUNTER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				JSONObject counterDeneWord = (JSONObject) getDeneWordAttributeByIdentity(new Identity(counterPointer), TeleonomeConstants.COMPLETE);
				int counterCurrentValue = counterDeneWord.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("line 2933, reset counter counterPointer=" + counterPointer );

				counterCurrentValue=0;

				counterDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, counterCurrentValue);


			}

		}
	}

	private int getNextPostionForDeneInMnemosyneChain(JSONObject destinationDeneChain, String deneName) throws JSONException{
		JSONArray denes = destinationDeneChain.getJSONArray("Denes");
		int currentMaximum=0;
		int aDenePosition=0;
		JSONObject dene;
		for(int i=0;i<denes.length();i++){
			dene = denes.getJSONObject(i);
			if(dene.getString("Name").equals(deneName)){
				aDenePosition = dene.getInt("Position");
				if(aDenePosition>currentMaximum)currentMaximum=aDenePosition;
			}
		}
		return (currentMaximum+1);
	}
	/**
	 *  this method returns the dene with the dene name required that has the position as passed by the parameter
	 * the postion parameter is String because it can be a command like COMMAND_MNEMOSYNE_LAST_DENE_POSITION or
	 *  COMMAND_MNEMOSYNE_PREVIOUS_TO_LAST_DENE_POSITION or it can be a number
	 * @param denes
	 * @param deneName
	 * @param position
	 * @return
	 * @throws JSONException
	 */
	private JSONObject getDeneFromDeneJSONArrayByPostion(JSONArray denes, String deneName, String position) throws JSONException{
		logger.debug("denes=" + denes + " deneName=" + deneName + " position=" + position);
		ArrayList<Map.Entry<JSONObject, Integer>> deneByPositionIndex = new ArrayList();
		int currentMaximum=0;
		int aDenePosition=0;
		JSONObject dene;
		int maximumPosition=0;
		int minimumPosition=999999999;
		for(int i=0;i<denes.length();i++){
			dene = denes.getJSONObject(i);
			if(dene.getString("Name").equals(deneName)){
				aDenePosition = dene.getInt("Position");
				logger.debug("aDenePosition=" + aDenePosition);

				if(aDenePosition<minimumPosition)minimumPosition=aDenePosition;
				if(aDenePosition>maximumPosition)maximumPosition=aDenePosition;
				deneByPositionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(dene, new Integer(aDenePosition)));
			}
		}
		Collections.sort(deneByPositionIndex, new IntegerCompare());
		logger.debug("deneByPositionIndex=" + deneByPositionIndex.size());
		//
		// at this point, deneByPositionIndex is ordered by position so return the one requested
		JSONObject toReturn = null;
		found:
			for (Map.Entry<JSONObject, Integer> entry : deneByPositionIndex) {
				dene = entry.getKey();
				aDenePosition = entry.getValue();

				if(position.equals(TeleonomeConstants.COMMAND_MNEMOSYNE_LAST_DENE_POSITION) && aDenePosition==maximumPosition){
					toReturn=dene;
					break found;
				}else if(position.equals(TeleonomeConstants.COMMAND_MNEMOSYNE_PREVIOUS_TO_LAST_DENE_POSITION) && aDenePosition==(maximumPosition-1)){
					toReturn=dene;
					break found;
				}else if(position.equals(TeleonomeConstants.COMMAND_MNEMOSYNE_FIRST_POSITION) && aDenePosition==(minimumPosition)){
					toReturn=dene;
					break found;
				}else{
					//
					// try parsing it
					try{
						int aPosition = Integer.parseInt(position);
						if(aDenePosition==(aPosition)){
							toReturn=dene;
							break found;
						}
					}catch(NumberFormatException e){

					}

				}
			}
		return toReturn;
	}

	public ArrayList loadImmediateMutation(JSONObject mutationObject) throws IOException, InvalidMutation, InvalidDenomeException{

		String mutationFileInString;
		//Vector commandsToExecuteVector = new Vector();
		ArrayList commandToExecute;
		JSONObject mutationDeneJSONObject=null, mutationDeneChainJSONObject;
		ArrayList<Map.Entry<String, MutationActionsExecutionResult>>  microControllerPointerMutationActionsExecutionResultArrayList = new ArrayList(); 

		try {
			logger.debug("load immediate mutation  starting " );

			//
			// now parse them
			//JSONObject mutationObject = mutationJSONObject.getJSONObject("Mutation");
			//
			// only process this mutation if its active
			//
			if(!mutationObject.getBoolean("Active"))return new ArrayList();
			logger.debug("load immediate mutation  after checking active " );

			String executionMode = mutationObject.getString("Execution Mode");
			if(!executionMode.equals(TeleonomeConstants.MUTATION_EXECUTION_MODE_IMMEDIATE)){
				Hashtable details = new Hashtable();
				details.put("message", "The rcrcution type of the mutation must be Immediate");
				throw new InvalidMutation(details);
			}
			JSONArray deneChainsArray = mutationObject.getJSONArray("DeneChains");
			logger.debug("load immediate mutation  after getting denechains " );

			//
			// inde
			Hashtable nameMutationDeneChainIndex = new Hashtable();
			for(int i=0;i<deneChainsArray.length();i++){
				mutationDeneChainJSONObject = (JSONObject) deneChainsArray.get(i);
				nameMutationDeneChainIndex.put(mutationDeneChainJSONObject.getString("Name"), mutationDeneChainJSONObject);
			}
			//
			logger.debug("load immediate mutation  after nameMutationDeneChainIndex " );
			//
			// perform onload denechain
			//
			//	check that is mime tyoe "set memword"
			//	get the denewords and the target is where you changing the value
			//	the mutaation value deneword is the value you are setting
			//	function would be
			//	setValueOfADeneWord(Identity )
			String target, deneWordName, targetDeneWordName;
			JSONObject deneWord;
			JSONObject injectionTarget;
			Object targetDeneWordValue;
			JSONArray denes;

			JSONObject onLoadMutationDeneChainJSONObject=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENE_TYPE_ON_LOAD_MUTATION);
			logger.debug("load immediate mutation  onLoadMutationDeneChainJSONObject="  + onLoadMutationDeneChainJSONObject);

			if(onLoadMutationDeneChainJSONObject!=null){
				denes = onLoadMutationDeneChainJSONObject.getJSONArray("Denes");
				for(int i=0;i<denes.length();i++){
					mutationDeneJSONObject = (JSONObject) denes.get(i);
					//
					// get the meemowords

					JSONArray deneWordsJSONArray = mutationDeneJSONObject.getJSONArray("DeneWords");
					//
					// because we are on the On Load dene chain, every dene will have three denewords:
					// Name="Target"  the value will contain a pointer to the the target deneword to alter
					// Name="Mutation Name" the name of the deneword to change the value
					// Name="Mutation Value" the new value to set the deneword  
					for(int j=0;j<deneWordsJSONArray.length();j++){

						deneWord = (JSONObject) deneWordsJSONArray.get(j);
						target = deneWord.getString("Target");
						logger.debug("denewordName=" + deneWord.getString("Name") + " target=" + target);

						//
						// the value can beString, int booolean, date
						targetDeneWordValue = deneWord.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//
						// the target can be either a pointer to another dene
						// or it can start with a $ for example $FileSystem which means it will 
						// not modify another section of the denome, but rather it will be used by
						// some code to mify the file system
						if(target.startsWith("@")){
							injectionTarget = getDenomicElementByIdentity(new Identity(target));
							injectionTarget.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, targetDeneWordValue);
							logger.debug("load immediate mutation  onLoadMutation  just performed an edit in onLoad target=" + target + " targetDeneWordValue=" + targetDeneWordValue );

						}

					}

				}

			}
			//
			// then do the DeneWord Injection chain, injecting the memwords
			// using the target parameter. There is only one dene in this chain
			//
			JSONObject deneWordInjections=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DENEWORD_INJECTION);
			logger.debug("load immediate mutation  deneWordInjections " + deneWordInjections);
			String injectionTargetPointer;

			if(deneWordInjections!=null){
				denes = deneWordInjections.getJSONArray("Denes");
				//
				// there is only one dene
				JSONObject dene = denes.getJSONObject(0);
				JSONArray deneWordsJSONArray = dene.getJSONArray("DeneWords");
				for(int j=0;j<deneWordsJSONArray.length();j++){
					deneWord = (JSONObject) deneWordsJSONArray.get(j);
					deneWordName = deneWord.getString("Name");
					logger.debug("in denewordinjections, deneWordName=" + deneWordName);
					target = deneWord.getString("Target");
					targetDeneWordValue = deneWord.getString("Value");
					injectionTarget = getDenomicElementByIdentity(new Identity(target));
					injectionTarget.put(deneWordName, targetDeneWordValue);

				}
			}
			//
			// then do the Dene Injection Chain
			//
			JSONObject deneInjections=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DENE_INJECTION);
			logger.debug("load immediate mutation  deneInjections " + deneInjections);

			if(deneInjections!=null){
				denes = deneInjections.getJSONArray("Denes");
				for(int i=0;i<denes.length();i++){
					mutationDeneJSONObject = (JSONObject) denes.get(i);
					injectionTargetPointer = mutationDeneJSONObject.getString("Injection Target");
					//
					// mutationDeneJSONObject has an expiration time in seconds after
					// the mutation, ie if the value is 600, it means ten minutes from now
					// therefore add the expiration time it with the correct timestamp
					int expirationSeconds = mutationDeneJSONObject.getInt("Expiration Seconds");
					mutationDeneJSONObject.put("Expiration", System.currentTimeMillis() + expirationSeconds*1000);

					injectionTarget = getDenomicElementByIdentity(new Identity(injectionTargetPointer));
					injectionTarget.put("Denes",mutationDeneJSONObject);
				}
			}

			//
			// finally do the Dene Actions Executions
			// which must be executed now
			//
			JSONObject actionToExecute=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_ACTIONS_TO_EXECUTE);
			JSONObject mutationActionDeneJSONObject;
			logger.debug("load immediate mutation  actionToExecute " + actionToExecute);

			if(actionToExecute!=null){
				denes = DenomeUtils.getDenesByDeneType(actionToExecute, TeleonomeConstants.DENE_TYPE_ACTION_LIST);
				ArrayList arrayList;
				JSONArray actionDeneWordPointers;
				JSONArray actuatorDeneWordPointers, microControllerPointersJSONArray;
				String actuatorDeneWordPointer;
				JSONObject actuatorJSONObject=null;

				//
				// actionDeneWordPointers cntains an array of string which are pointers to the denes that contain the evaluation postion
				String denePointer;
				JSONObject actionDene = null;
				Integer evaluationPosition;
				String microControllerPointer=null;

				for(int i=0;i<denes.length();i++){
					//
					// every action dene in has two denewords, one that points to the action dene to be executed
					// and one that points to the actuator, which is needed to get the microcontroller pointer
					microControllerPointer=null;
					mutationActionDeneJSONObject = (JSONObject) denes.get(i);
					actionDeneWordPointers = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(mutationActionDeneJSONObject, TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					actuatorDeneWordPointers = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(mutationActionDeneJSONObject, TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					//
					// there is only actuator so get the first element
					actuatorDeneWordPointer=null;
					if(actuatorDeneWordPointers.length()>0){
						actuatorDeneWordPointer = actuatorDeneWordPointers.getString(0);
						actuatorJSONObject = getDeneByIdentity(new Identity(actuatorDeneWordPointer));
						microControllerPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(actuatorJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_MICROCONTROLLER_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//
						// again there will only be one and only one so that the zero value
						if(microControllerPointersJSONArray.length()>0){
							microControllerPointer = microControllerPointersJSONArray.getString(0);
						}
					}
					logger.debug("in load mutation immediately, actionDeneWordPointers=" + actionDeneWordPointers);
					//
					// every item in actionPointers is an action that needs to be executed
					// and all the actions in this dene must come from the same actuator
					// therefore there is only one pointer to the microcontroller
					// only execute the actions if we have a microControllerPointer
					//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

					MutationActionsExecutionResult aMutationActionsExecutionResult;
					if(microControllerPointer!=null){
						for(int n=0;n<actionDeneWordPointers.length();n++){
							denePointer = (String)actionDeneWordPointers.getString(n);
							logger.debug("in load mutation immediately, denePointer=" + denePointer);						
							//
							// now execute the action
							JSONObject actuatorActionJSONObject = getDeneByIdentity(new Identity(denePointer));
							logger.debug("in load mutation immediately, microControllerPointer=" + microControllerPointer);						
							//
							// commandsToExecute is an ArrayList with one memeber 
							// ArrayList<Map.Entry<String, JSONObject>> toReturn = new ArrayList();
							//	
							// check to see if there are any after execution actions
							//
							String pointerToActionSuccessTasks = (String) this.getDeneWordAttributeByDeneWordTypeFromDene(actuatorActionJSONObject,  TeleonomeConstants.DENEWORD_TYPE_ACTION_SUCCESS_TASK_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							String pointerToMnemosyneTasks = (String) this.getDeneWordAttributeByDeneWordTypeFromDene(actuatorActionJSONObject,  TeleonomeConstants.MNEMOSYNE_OPERATION_INDEX_LABEL, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

							logger.debug("in load mutation immediately, pointerToActionSuccessTasks=" + pointerToActionSuccessTasks);						
							logger.debug("in load mutation immediately, actuatorActionJSONObject=" + actuatorActionJSONObject);						

							//
							// now load the action
							commandToExecute = aDenomeManager.evaluateMutationAction(actuatorActionJSONObject);	
							logger.debug("in load mutation immediately, commandToExecute=" + commandToExecute);						

							aMutationActionsExecutionResult = new MutationActionsExecutionResult(pointerToActionSuccessTasks, commandToExecute, selectedDenomeFileName, pointerToMnemosyneTasks);

							//
							// check t
							microControllerPointerMutationActionsExecutionResultArrayList.add(new AbstractMap.SimpleEntry<String, MutationActionsExecutionResult>(microControllerPointer,aMutationActionsExecutionResult));	

							//commandsToExecuteVector.addElement(commandToExecute);
						}

					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		logger.debug("microControllerPointerMutationActionsExecutionResultArrayList=" + microControllerPointerMutationActionsExecutionResultArrayList);						

		return microControllerPointerMutationActionsExecutionResultArrayList;
	}

	public class MutationActionsExecutionResult{

		private String pointerToActionSuccessTasks;
		private ArrayList commandToExecute;
		private String pulseFileName;
		private String pointerToMnemosyneTasks;

		public MutationActionsExecutionResult(String p, ArrayList l, String fn, String pmt){
			pointerToActionSuccessTasks=p;
			commandToExecute=l;
			pulseFileName=fn;
			pointerToMnemosyneTasks = pmt;
		}

		public String getPointerToActionSuccessTasks(){
			return pointerToActionSuccessTasks;
		}
		public String getPointerToMnemosyneTasks(){
			return pointerToMnemosyneTasks;
		}

		public String getPulseFileName(){
			return pulseFileName;
		}

		public ArrayList getCommandToExecute(){
			return commandToExecute;
		}
	}

	private void loadMutations() throws IOException{
		//
		// scan the file system to see if there are any mutations that need to be imported
		/*
		if(selectedDenomeFileName==null ||selectedDenomeFileName.equals("")){
			File localDir = new File(Utils.getLocalDirectory());
			File[] files = localDir.listFiles();
			selectedDenomeFileName="";
			found:
				for(int i=0;i<files.length;i++){
					if(FilenameUtils.getExtension(files[i].getAbsolutePath()).equals("denome")){
						selectedDenomeFileName = files[i].getAbsolutePath();
						logger.debug("reading denome from " +selectedDenomeFileName);
						break found;
					}
				}
		}
		 */
		File directory = new File(".");
		// get just files, not directories
		File[] files = directory.listFiles((FileFilter) FileFileFilter.FILE);
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
		String mutationFileInString;
		JSONObject mutationJSONObject=null, mutationDeneJSONObject=null, mutationDeneChainJSONObject;
		String executionMode;
		JSONObject extractedMutationObject=null;
		String mutationName="";
		for (File file : files) {
			String mutationFileName=file.getAbsolutePath();
			if(FilenameUtils.getExtension(mutationFileName).equals("mutation")){
				logger.debug("About to process mutation " + mutationFileName);
				try {
					mutationFileInString = FileUtils.readFileToString(file);


					mutationJSONObject = new JSONObject(mutationFileInString);
					//
					// now parse them

					extractedMutationObject = mutationJSONObject.getJSONObject("Mutation");
					//
					// only process this mutation if its active
					//
					if(!extractedMutationObject.getBoolean("Active"))continue;
					mutationName = extractedMutationObject.getString("Name");
					//
					// check to see if this is a Structural or State mutation
					// if its structural then apply it,
					// if its state, then just add it to the denome
					// so it can be executed later
					Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

					if(extractedMutationObject.getString("Mutation Type").equals(TeleonomeConstants.MUTATION_TYPE_STATE)){
						JSONObject denomeObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
						JSONArray mutationsJSONArray = denomeObject.getJSONArray("Mutations");
						//
						// now add the timestamp of the mutation
						extractedMutationObject.put("Mutated On Timestamp", simpleFormatter.format(cal.getTime()));
						extractedMutationObject.put("Mutated On Timestamp in Milliseconds", cal.getTime().getTime());

						mutationsJSONArray.put(extractedMutationObject);
						//
						// now change the extension of the file to mutated so that it will not be read again
						//
						File destFile = new File(FilenameUtils.getBaseName(file.getAbsolutePath()) + ".mutated");
						FileUtils.moveFile(file, destFile);
						continue;
					}


					JSONArray deneChainsArray = extractedMutationObject.getJSONArray("DeneChains");
					//
					// inde
					Hashtable nameMutationDeneChainIndex = new Hashtable();
					for(int i=0;i<deneChainsArray.length();i++){
						mutationDeneChainJSONObject = (JSONObject) deneChainsArray.get(i);
						nameMutationDeneChainIndex.put(mutationDeneChainJSONObject.getString("Name"), mutationDeneChainJSONObject);
					}


					//
					// perform onload denechain
					//
					//	check that is mime tyoe "set memword"
					//	get the denewords and the target is where you changing the value
					//	the mutaation value deneword is the value you are setting
					//	function would be
					//	setValueOfADeneWord(Identity )
					String injectionTargetPointer,  targetDeneWordName;
					JSONObject deneWord;
					JSONObject injectionTarget;
					Object targetDeneWordValue;
					JSONArray denes;

					JSONObject onLoadMutationDeneChainJSONObject=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENE_TYPE_ON_LOAD_MUTATION);

					if(onLoadMutationDeneChainJSONObject!=null){
						denes = onLoadMutationDeneChainJSONObject.getJSONArray("Denes");
						for(int i=0;i<denes.length();i++){
							mutationDeneJSONObject = (JSONObject) denes.get(i);
							//
							// get the meemowords

							JSONArray deneWordsJSONArray = mutationDeneJSONObject.getJSONArray("DeneWords");
							for(int j=0;j<deneWordsJSONArray.length();j++){
								deneWord = (JSONObject) deneWordsJSONArray.get(j);
								injectionTargetPointer = deneWord.getString("Injection Target");

								//
								// the value can beString, int booolean, date
								targetDeneWordValue = deneWord.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								injectionTarget = getDenomicElementByIdentity(new Identity(injectionTargetPointer));
								injectionTarget.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, targetDeneWordValue);
							}

						}

					}
					//
					// then do the DeneWord Injection chain, injecting the memwords
					// using the target parameter. There is only one dene in this chain
					//
					JSONObject deneWordInjections=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DENEWORD_INJECTION);
					JSONArray deneWords;
					if(deneWordInjections!=null){
						denes = deneWordInjections.getJSONArray("Denes");
						//
						// if there is one, there could nly be one, but it could be blank
						if(denes.length()>0){
							JSONObject dene = denes.getJSONObject(0);
							JSONObject cloneDeneWord;
							JSONArray deneWordsJSONArray = dene.getJSONArray("DeneWords");
							for(int j=0;j<deneWordsJSONArray.length();j++){
								deneWord = (JSONObject) deneWordsJSONArray.get(j);
								injectionTargetPointer = deneWord.getString("Injection Target");
								injectionTarget = getDenomicElementByIdentity(new Identity(injectionTargetPointer));
								//the injectionTarget is the Dene f DeneType Action List
								// therefore get the DeneWords and add it to it
								deneWords = injectionTarget.getJSONArray("DeneWords");
								//
								// now clone the deneword to remove
								// the target which is not needed
								cloneDeneWord = new JSONObject(deneWord.toString());
								cloneDeneWord.remove("Injection Target");
								deneWords.put(cloneDeneWord);

							}
						}
					}
					//
					// then do the Dene Injection Chain
					//
					JSONObject deneInjections=(JSONObject)nameMutationDeneChainIndex.get(TeleonomeConstants.DENECHAIN_DENE_INJECTION);
					if(deneInjections!=null){
						denes = deneInjections.getJSONArray("Denes");
						for(int i=0;i<denes.length();i++){
							mutationDeneJSONObject = (JSONObject) denes.get(i);
							injectionTargetPointer = mutationDeneJSONObject.getString("Injection Target");
							//
							// mutationDeneJSONObject could have an expiration time in seconds after
							// the mutation, ie if the value is 600, it means ten minutes from now
							// therefore add the expiration time it with the correct timestamp
							if(mutationDeneJSONObject.has(TeleonomeConstants.EXPIRATION_SECONDS)){
								int expirationSeconds = mutationDeneJSONObject.getInt(TeleonomeConstants.EXPIRATION_SECONDS);
								mutationDeneJSONObject.put("Expiration", System.currentTimeMillis() + expirationSeconds*1000);
							}
							injectionTarget = getDenomicElementByIdentity(new Identity(injectionTargetPointer));
							//
							// since we are injecting a dene, the target will be pointing to the denechain, whcich is
							// represneted by injectionTarget, for that reason, exgtract the Denes JasonArray
							// and add the dene to that array
							JSONArray denesJsonArray = injectionTarget.getJSONArray("Denes");

							denesJsonArray.put(mutationDeneJSONObject);
						}
					}

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}

				//
				// at this point the mutation has been succesfully loaded so delete the file
				// and c
				JSONObject mutationEventJSONObject = createMutationEventFromMutation( extractedMutationObject,  null);
				try {
					aDenomeManager.storeMutationEvent(mutationEventJSONObject);

				} catch (PersistenceException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				//
				// incorporate the mutation into the denome
				//

				try {
					JSONObject denomeObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
					JSONArray mutationsJSONArray = denomeObject.getJSONArray("Mutations");
					mutationsJSONArray.put(mutationEventJSONObject);

					logger.debug("The denome has mutated succesfully by " + mutationName );

					//
					// save the denome with the mutation
					//
					//
					// Copy the currentpulse to be the previous pulse
					//
					File currentPulseFile = new File(selectedDenomeFileName);
					String previousPulseFileName = FilenameUtils.getBaseName(selectedDenomeFileName) + ".unmutated";

					File previousPulseFile = new File(previousPulseFileName);
					logger.debug("about to copy " + selectedDenomeFileName + " to " + previousPulseFileName);

					FileUtils.copyFile(currentPulseFile, previousPulseFile);
					//
					// now write the denome
					//
					FileUtils.write(new File(selectedDenomeFileName), currentlyCreatingPulseJSONObject.toString(4));
					logger.debug("Saved pulse to " + selectedDenomeFileName);
					//
					// and reload the denme so that the next mutation acts over the mutated denome
					loadDenome(selectedDenomeFileName);

				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException  e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (MissingDenomeException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}


				//
				// now erase the mutation file from the hard drive
				FileUtils.deleteQuietly(file);	
			}
		}

	}

	public JSONObject createMutationEventFromMutation(JSONObject mutationJSONObject, JSONArray actuatorLogicProcessingDeneJSONArray){
		// Put the timestamp of the pulse because the command $Current_Time will use it to return a value
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

		int  day = cal.get(Calendar.DATE);
		int  month = cal.get(Calendar.MONTH);
		int  year = cal.get(Calendar.YEAR);
		//
		//remove any expired denes from the denome
		//
		//JSONObject mutationEventJSONObject = new JSONObject();

		try {
			mutationJSONObject.put("Mutation Event Timestamp", simpleFormatter.format(cal.getTime()));
			mutationJSONObject.put("Mutation Event Timestamp in Milliseconds", cal.getTime().getTime());


			JSONArray mutationDeneChainsJSONObject  = mutationJSONObject.getJSONArray("DeneChains");
			//
			// before adding the processing logic, erase all the other ones to make sure that a pulse can nly
			// carry the last processing chain otherwise you will end up with a denome with thousands of processing
			//
			JSONObject processingLogicMemeChain=null;
			logger.debug("before removing processing chains =" +mutationDeneChainsJSONObject.length());
			//
			// to prune do this until there are no more processing logic
			boolean keepGoing=true;
			boolean removed=false;
			while(keepGoing){
				removed=false;
				found:
					for(int i=0;i<mutationDeneChainsJSONObject.length();i++){
						processingLogicMemeChain = mutationDeneChainsJSONObject.getJSONObject(i);
						if(processingLogicMemeChain.getString("Name").equals( TeleonomeConstants.MUTATION_PROCESSING_LOGIC_DENE_CHAIN_NAME)){
							Object o = mutationDeneChainsJSONObject.remove(i);
							//logger.debug("o=" + o);
							removed=true;
							break found;
						}
					}
				if(!removed)keepGoing=false;
			}
			logger.debug("after removing befire adding a new one processing chains =" +mutationDeneChainsJSONObject.length());

			//
			// now add it
			//
			processingLogicMemeChain = new JSONObject();
			mutationDeneChainsJSONObject.put(processingLogicMemeChain);

			logger.debug("after   adding the new one processing chains =" +mutationDeneChainsJSONObject.length());

			//
			// no put data into the denechain that will contain the denes fr the processinglogic

			if(actuatorLogicProcessingDeneJSONArray!=null){
				processingLogicMemeChain.put("Name", TeleonomeConstants.MUTATION_PROCESSING_LOGIC_DENE_CHAIN_NAME);
				processingLogicMemeChain.put("Denes", actuatorLogicProcessingDeneJSONArray);
			}
			//mutationEventJSONObject.put("Mutation", mutationJSONObject);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		return mutationJSONObject;
	}


	/**
	 *  the purpose f this methd is to read the value of the identity in
	 *  purpose;operational data:vital:
	 *  this happens at the very beginning of the lifeccycle, before
	 *  the first pulse even ocurrs.  this is because we need to compare
	 *  the value in the denome to that value in the initOperationalMode
	 *  file created by the rc.local script when the pi first starts
	 *  because if they are different, then a pathology dene needs to be
	 *  added to the mnemosyne.  Because this happens outside of the pulse
	 *  cycle the denome file needs to be read separately
	 * @return
	 * @throws InvalidDenomeException 
	 * @throws JSONException 
	 */
	public String getInitialIdentityState() throws InvalidDenomeException, JSONException{

		File selectedFile = new File(Utils.getLocalDirectory() + "Teleonome.denome");
		logger.debug("reading denome from " +selectedDenomeFileName);
		String initialIdentityState="";
		try {
			String fileInString = FileUtils.readFileToString(selectedFile);
			JSONObject denomeJSONObject = new JSONObject(fileInString);

			JSONObject denome = denomeJSONObject.getJSONObject("Denome");
			String tN = denome.getString("Name");
			Identity identity = new Identity(tN, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_TYPE_INITIAL_IDENTITY_MODE);
			initialIdentityState =  (String) DenomeUtils.getDeneWordByIdentity(denomeJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		return initialIdentityState;

	}

	public void readAndModifyAddDeneToDenChainByIdentity(Identity deneChainIdentity,JSONObject newDene) throws JSONException, InvalidDenomeException{
		File selectedFile = new File(Utils.getLocalDirectory() + "Teleonome.denome");
		logger.debug("readAndModifyDeneWordByIdentity, reading denome from " +selectedDenomeFileName);
		String initialIdentityState="";
		try {
			JSONObject denomeJSONObject = new JSONObject(FileUtils.readFileToString(selectedFile));
			String nucleusName = deneChainIdentity.getNucleusName();
			String deneChainName = deneChainIdentity.getDenechainName();
			logger.debug("nucleusName=" + nucleusName + " deneChainName=" + deneChainName);
			JSONObject deneChain = (JSONObject) DenomeUtils.getDeneChainByName(denomeJSONObject, nucleusName, deneChainName);
			JSONArray denes = deneChain.getJSONArray("Denes");
			denes.put(newDene);
			try {
				FileUtils.write(selectedFile, denomeJSONObject.toString(4));
				FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/Teleonome.denome"), denomeJSONObject.toString(4));

			} catch (IOException | JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean readAndModifyDeneWordByIdentity(Identity targetDeneWordIdentity,Object value) throws JSONException, InvalidDenomeException{
		File selectedFile = new File(Utils.getLocalDirectory() + "Teleonome.denome");
		String initialIdentityState="";
		boolean toReturn=false;
		try {
			JSONObject localDenomeJSONObject = new JSONObject(FileUtils.readFileToString(selectedFile));
			logger.debug("targetDeneWordIdentity=" + targetDeneWordIdentity.toString());

			JSONObject deneWord = (JSONObject) DenomeUtils.getDeneWordByIdentity(localDenomeJSONObject, targetDeneWordIdentity, TeleonomeConstants.COMPLETE);
			logger.debug("readAndModifyDeneWordByIdentity, deneWord " +deneWord + " value=" + value);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, value);

			try {
				FileUtils.write(selectedFile, localDenomeJSONObject.toString(4));
				FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/Teleonome.denome"), localDenomeJSONObject.toString(4));
				toReturn=true;
			} catch (IOException | JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			logger.debug("Saved pulse to " + selectedDenomeFileName);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toReturn;
	}




	public String initializePulse() throws MissingDenomeException, IOException{
		logger.debug("initializing pulse");
		if(selectedDenomeFileName==null || selectedDenomeFileName.equals("")){
			File localDir = new File(Utils.getLocalDirectory());
			File[] files = localDir.listFiles();
			selectedDenomeFileName="";
			found:
				for(int i=0;i<files.length;i++){
					if(FilenameUtils.getExtension(files[i].getAbsolutePath()).equals("denome")){
						selectedDenomeFileName = files[i].getAbsolutePath();
						logger.debug("reading denome from " +selectedDenomeFileName);
						break found;
					}
				}

		}
		loadDenome(selectedDenomeFileName);


		//

		//
		// first save the currentCreatingPulse to become the previousPulse,
		// which could be needed if the denome has @Previous_Pulse_Value
		//
		if(currentlyCreatingPulseJSONObject!=null){
			previousPulseJSONObject  = new JSONObject(currentlyCreatingPulseJSONObject, JSONObject.getNames(currentlyCreatingPulseJSONObject));
			previousPulseMillis = previousPulseJSONObject.getLong("Pulse Timestamp in Milliseconds");
			logger.debug("previouspulsejsonobject is not null, previousPulseMillis=" + previousPulseMillis);
		}else {
			logger.debug("previouspulsejsonobject is null");
		}


		currentlyCreatingPulseJSONObject  = new JSONObject(denomeJSONObject, JSONObject.getNames(denomeJSONObject));
		//
		// the first thing to do is to check if there are any mutations to load
		// if so, the loadMutation method will load them, reload the denome immediately 
		// after loading every mutation, so that by the time the methd is finished,
		// the variable  currentlyCreatingPulseJSONObject is now mutated
		loadMutations();

		/*
		try {
			JSONObject m = this.getDeneWordByIdentity(new Identity("@Tlaloc:Purpose:Operational Data:Power Point:BorePumpStatus"));
			logger.debug("In init pulse denome:"+m.toString(4));

			logger.debug("in init pulse:" + getCurrentPulseValueForDeneWord("@Tlaloc:Purpose:Operational Data:Flow:BorePumpStatus"));
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		 */

		//
		// Put the timestamp of the pulse because the command $Current_Time will use it to return a value
		currentPulseStartTimestampMillis = System.currentTimeMillis();
		SimpleDateFormat simpleFormatter = new SimpleDateFormat("dd/MM/yy HH:mm");
		Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

		int  day = cal.get(Calendar.DATE);
		int  month = cal.get(Calendar.MONTH);
		int  year = cal.get(Calendar.YEAR);
		//
		//remove any expired denes from the denome
		//
		Date newPulseDate = cal.getTime();
		try {
			logger.debug("initialize pulse se Pulse Timestamp="+ simpleFormatter.format(newPulseDate));
			currentlyCreatingPulseJSONObject.put("Pulse Timestamp", simpleFormatter.format(newPulseDate));
			currentlyCreatingPulseJSONObject.put("Pulse Timestamp in Milliseconds", newPulseDate.getTime());

		} catch (JSONException e) {
			Utils.getStringException(e);
		}
		//
		// now add the Purpose:State denechain from the previous pulse as well as
		// remove the Actuator Logic Processing from the currentlycreatedpulse
		// because it will be added it during the pulse
		//
		// create the DeneChain that will represent the Actuator Logic Processing
		//

		try {
			JSONObject denomeJSONObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeJSONObject.getJSONArray("Nuclei");
			JSONObject aNucleusJSONObject;
			JSONArray deneChains;
			teleonomeName = denomeJSONObject.getString("Name");

			ArrayList<Entry<JSONObject, Integer>> actuatorExecutionPositionDeneIndex = getActuatorExecutionPositionDeneIndex();
			JSONObject anActuatorDeneJSONObject, actuatorActionJSONObject;
			String actuatorName="";
			JSONArray actuatorActionConditionsJSONArray, actuatorLogicConditionsProcessingDeneDeneWords;
			JSONObject actuatorActionConditionJSONObject, actuatorLogicConditionProcessingDene;
			String conditionName;
			JSONObject actuatorLogicProcessingDeneChain =null;
			JSONObject mnemotyconProcessingDeneChain =null;
			JSONObject pathologyDeneChain =null;
			boolean success;

			for(int i=0;i<nucleiArray.length();i++){
				aNucleusJSONObject = nucleiArray.getJSONObject(i);
				if(aNucleusJSONObject.getString("Name").equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					deneChains = aNucleusJSONObject.getJSONArray("DeneChains");
					//
					// only add the actuatorLogicProcessingDeneChain if there are actions either startup or standard
					//xx
					logger.debug("about to check to see if we need to create processing chains(actuatorExecutionPositionDeneIndex.size()="  + actuatorExecutionPositionDeneIndex.size() + "  actuatorExecutionPositionDeneForInitialIndex.size()=" + actuatorExecutionPositionDeneForInitialIndex.size());
					if(actuatorExecutionPositionDeneIndex.size()>0 || actuatorExecutionPositionDeneForInitialIndex.size()>0){
						//
						// first remove the one from the previous pulse
						//
						try {
							success = DenomeUtils.removeChainFromNucleus(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);	
							logger.debug("removing the actuator processing chain, result:" + success);


						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						//
						// then add a new one
						actuatorLogicProcessingDeneChain = new JSONObject();
						deneChains.put(actuatorLogicProcessingDeneChain);
						actuatorLogicProcessingDeneChain.put("Name", TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);
					}
					//
					// only add the mnemotyconProcessingDeneChain if there are mnemotycons that are active
					//xx
					if(mnemosyconDenesJSONArray.length()>0 ){
						//
						// first remove the one from the previous pulse
						//
						try {
							success = DenomeUtils.removeChainFromNucleus(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_MNEMOSYCON_PROCESSING);	
							logger.debug("removing the mnemotycon processing, result:" + success);


						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						//
						// then add a new one
						mnemotyconProcessingDeneChain = new JSONObject();
						deneChains.put(mnemotyconProcessingDeneChain);
						JSONArray mnemotyconProcessingDenes = new JSONArray();
						mnemotyconProcessingDeneChain.put("Denes", mnemotyconProcessingDenes);
						mnemotyconProcessingDeneChain.put("Name", TeleonomeConstants.DENECHAIN_MNEMOSYCON_PROCESSING);
						logger.debug("added new mnemotyconProcessingDeneChain" );
					}

					//
					// now do the pathology denechain
					// first remove the existing one
					// and then add it
					try {
						success = DenomeUtils.removeChainFromNucleus(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_PATHOLOGY);
						logger.debug("removing the pathology, result:" + success);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.debug("did not find pathology denechain");
					}	


					pathologyDeneChain = new JSONObject();
					deneChains.put(pathologyDeneChain);
					JSONArray pathologyDenes = new JSONArray();

					pathologyDeneChain.put("Denes", pathologyDenes);
					pathologyDeneChain.put("Name", TeleonomeConstants.DENECHAIN_PATHOLOGY);




					//
					// now remove the System Data dene from 
					int deneRemoved=0;
					try {
						deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_SYSTEM_DATA);
						logger.debug("removing the System data dene from operational, removed:" + deneRemoved);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.debug("did not find System Data at initpulse while trying to remove it");
					}	

					//					//
					//					// now remove the Alert Messages Dene dene from 
					//					 deneRemoved=0;
					//					try {
					//						deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_ALERT_MESSAGES);
					//						logger.debug("removing the Alert Messages  dene from operational, removed:" + deneRemoved);
					//					} catch (InvalidDenomeException e) {
					//						// TODO Auto-generated catch block
					//						logger.debug("did not find Alert Messages at initpulse while trying to remove it");
					//					}

					//
					// now remove the Memory Status dene from 
					deneRemoved=0;
					try {
						deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_MEMORY_STATUS);
						logger.debug("removing the Memry Status dene from operational, removed:" + deneRemoved);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.debug("did not find System Data at initpulse while trying to remove it");
					}	
					//
					// remove the 4 DENE_TYPE_PROCESS_MEMORY_INFO denes from the denechain
					//
					try {
						deneRemoved = DenomeUtils.removeAllDenesFromChainByDeneType(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_TYPE_PROCESS_MEMORY_INFO);
						logger.debug("removing the DENE_TYPE_PROCESS_MEMORY_INFOdene from operational, removed:" + deneRemoved);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.debug("did not find System Data at initpulse while trying to remove it");
					}

					//
					// now add them again with the updated values
					// we do this here so that the rest of the pulse has access to the updated
					// values before the actions are executed.  This is so that you can 
					// create a do-nothing action that has the successactiontask which 
					// will copy values of the memry process to the mnemosynce counter denechain
					// fir example. that way, implementing a memory monitoring reporintg grpah 
					// is as simple as creating a human interface dene of type linechart that
					// point to the source of the data to the pulse counter menmosyne section
					//
					// Memory Status Dene
					//
					JSONObject memoryStatusDene = DenomeUtils.generateMemoryStatusDene();
					JSONArray processMemoryStatusArray = DenomeUtils.generateProcessMemoryStatusDene();
					JSONObject operationalDataDeneChain=null;
					try {
						operationalDataDeneChain = DenomeUtils.getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA);
					} catch (InvalidDenomeException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					JSONArray operationalDataDenes = operationalDataDeneChain.getJSONArray("Denes");
					operationalDataDenes.put(memoryStatusDene);
					for(int j=0;j<processMemoryStatusArray.length();j++) {
						operationalDataDenes.put(processMemoryStatusArray.getJSONObject(j));
					}



					//
					// now remove the Wifi Info dene from 

					try {
						deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_WIFI_INFO);
						logger.debug("removing the Wifi Info data dene from operational, result:" + deneRemoved);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
						logger.debug("did not find Wifi Info at initpulse while trying to remove it");
					}

					// Remove all the pathology denes from the purpose vital

					//
					try {
						int deneWordsRemoved = DenomeUtils.removeDeneWordFromDeneByDeneWordType(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.PATHOLOGY_DENEWORD_TYPE);
						logger.debug("removing the pathology denewords from the vital dene from purpose operational data, removed:" + deneWordsRemoved);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
						logger.debug("did not find Vital at initpulse while trying to remove it");
					}	


					//
					// System Data Dene
					//
					File directory = new File(".");
					long totalSpace = directory.getTotalSpace()/1024000;
					long freeSpace = directory.getFreeSpace()/1024000;
					long usableSpace = directory.getUsableSpace()/1024000;
					System.gc();
					double availableMemory = Runtime.getRuntime().freeMemory()/1024000;
					double maxMemory = Runtime.getRuntime().maxMemory()/1024000;
					double dbSize = aDBManager.getDatabaseSizeInMB();
					double pulseSize = aDBManager.getTableSizeMB("pulse");
					double organimsPulseSize = aDBManager.getTableSizeMB("organismpulse");

					JSONObject systemDataDene = new JSONObject();
					operationalDataDenes.put(systemDataDene);
					systemDataDene.put("Name", TeleonomeConstants.DENE_SYSTEM_DATA);
					JSONArray systemDataDeneWords = new JSONArray();
					systemDataDene.put("DeneWords", systemDataDeneWords);

					JSONObject deneWord = DenomeUtils.buildDeneWordJSONObject("Total Space",""+totalSpace,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Free Space",""+freeSpace,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Usable Space",""+usableSpace,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Available Memory",""+availableMemory,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Maximum Memory",""+maxMemory,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Database Size",""+dbSize,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Pulse Table Size",""+pulseSize,"MB","double",true);
					systemDataDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Organism Pulse Table Size",""+organimsPulseSize,"MB","double",true);
					systemDataDeneWords.put(deneWord);

					//
					// hypothalamus
					//
					String hypothalamusBuildNumber="N.A.";
					try {
						hypothalamusBuildNumber = FileUtils.readFileToString(new File("TeleonomeHypothalamusBuild.info"));

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					deneWord = DenomeUtils.buildDeneWordJSONObject("Hypothalamus Build Number",""+hypothalamusBuildNumber,null,"String",true);
					systemDataDeneWords.put(deneWord);
					//
					// medula
					//
					String medulaBuildNumber="N.A.";
					try {
						medulaBuildNumber = FileUtils.readFileToString(new File("MedulaBuild.info"));

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					deneWord = DenomeUtils.buildDeneWordJSONObject("Medula Build Number",""+medulaBuildNumber,null,"String",true);	
					systemDataDeneWords.put(deneWord);
					//
					// heart
					//
					String heartBuildNumber="N.A.";
					try {
						heartBuildNumber = FileUtils.readFileToString(new File("heart/HeartBuild.info"));

					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					deneWord = DenomeUtils.buildDeneWordJSONObject("Heart Build Number",""+heartBuildNumber,null,"String",true);	
					systemDataDeneWords.put(deneWord);

					//'
					/// system info
					///




					logger.debug("about to get wifi info");
					JSONObject wifiDataDene = new JSONObject();
					wifiDataDene.put("Name", TeleonomeConstants.DENE_WIFI_INFO);
					JSONArray wifiDataDeneWords = new JSONArray();
					wifiDataDene.put("DeneWords", wifiDataDeneWords);

					deneWord = DenomeUtils.buildDeneWordJSONObject("Host IP Address",localIpAddress,null,"String",true);
					wifiDataDeneWords.put(deneWord);

					operationalDataDenes.put(wifiDataDene);
					try {


						String command="iwconfig wlan0";
						ArrayList commandResults = Utils.executeCommand(command);
						//logger.debug("commandResults=" + commandResults.size() + commandResults);
						deneWord = DenomeUtils.buildDeneWordJSONObject("Interface","wlan0",null,"String",true);
						wifiDataDeneWords.put(deneWord);
						String line;
						if(commandResults.size()>0){
							for(int k=0;k<commandResults.size();k++){
								line = ((String)commandResults.get(k)).trim();
								String[] tokens = line.split("  ");
								for(int j=0;j<tokens.length;j++){
									logger.debug("tokens[j]=" + tokens[j]);
									//
									// there is one token without = which contains
									// the interface, ie wlan0
									if(tokens[j].indexOf("=")>-1){
										String[] tokens2 = tokens[j].trim().split("=");		
										logger.debug(tokens2[0] + ":" + tokens2[1]);
										deneWord = DenomeUtils.buildDeneWordJSONObject(tokens2[0],tokens2[1],null,"String",true);
										wifiDataDeneWords.put(deneWord);
									}else if(tokens[j].indexOf(":")>-1){
										String[] tokens2 = tokens[j].trim().split(":");		
										logger.debug(tokens2[0] + ":" + tokens2[1]);
										deneWord = DenomeUtils.buildDeneWordJSONObject(tokens2[0],tokens2[1],null,"String",true);
										wifiDataDeneWords.put(deneWord);
									}else{
										if(!tokens[j].contains("wlan") && tokens[j].length()>0){
											deneWord = DenomeUtils.buildDeneWordJSONObject("Support",tokens[j],null,"String",true);
											wifiDataDeneWords.put(deneWord);
										}
									}

								}

							}
						}


					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
					logger.debug("looking for ssids");
					//
					// add the available SSIDs
					JSONArray availableSSIDs = NetworkUtilities.getSSID(false);
					JSONObject ssid;
					String ssidName, signal;
					for(int j=0;j<availableSSIDs.length();j++){
						ssid = availableSSIDs.getJSONObject(j);

						signal = ssid.getString("Signal");
						ssidName = ssid.getString("SSID");
						//ssid.getString("Authentication");
						logger.debug("ssid="+ ssid + " signal=" + signal + " ssidName=" + ssidName);
						deneWord = DenomeUtils.buildDeneWordJSONObject("SSID:" + ssidName,signal,null,"String",true);
						wifiDataDeneWords.put(deneWord);


					}

					logger.debug("End of System Data Dene");

					//
					// End of System Data Dene
					//


					JSONArray actuatorLogicProcessingDenes = new JSONArray();
					JSONObject actuatorLogicProcessingDene = null;
					JSONArray actuatorLogicProcessingDeneDeneWords = null;
					JSONObject actuatorLogicProcessingDeneDeneWord = null;
					JSONObject actuatorLogicProcessingCodonDeneDeneWord=null;

					String codonName="";
					String actionName="";
					//
					// loop over the actuators to generate all the initial processing denes for the initial
					// and the standard
					// first do the initial
					//

					//String actionListName;
					String actuatorLogicProcessingDeneName; 
					logger.debug("actuatorExecutionPositionDeneForInitialIndex:" + actuatorExecutionPositionDeneForInitialIndex.size());

					for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneForInitialIndex) {
						anActuatorDeneJSONObject = entry.getKey();
						//logger.debug("line 3215 anActuatorDeneJSONObject:" + anActuatorDeneJSONObject);

						actuatorName = anActuatorDeneJSONObject.getString("Name");
						//actionListName = (String)getDeneWordAttributeByDeneWordTypeFromDene(anActuatorDeneJSONObject, TeleonomeConstants.DENE_TYPE_ACTION_LIST, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
						//logger.debug("actionListName=" + actionListName );

						//actuatorDeneNameActuatorActionEvaluationPositionActionIndex..get(actuatorName)
						codonName = actuatorName + " Processing";
						try { 
							//	logger.debug("line 3224 actuatorLogicProcessingDeneChain:" + actuatorLogicProcessingDeneChain);

							actuatorLogicProcessingDeneChain.put("Denes", actuatorLogicProcessingDenes);
							actuatorLogicProcessingDeneChain.put("Name", TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);

							ArrayList<Map.Entry<JSONObject, Integer>> actuatorActionEvaluationPositionActionIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex.get(actuatorName);

							//logger.debug("line 3231 actuatorActionEvaluationPositionActionIndex:" + actuatorActionEvaluationPositionActionIndex);

							for (Map.Entry<JSONObject, Integer> action : actuatorActionEvaluationPositionActionIndex) {
								actuatorActionJSONObject = action.getKey();
								logger.debug("line 3235a actuatorActionJSONObject:" + actuatorActionJSONObject.has("Name") +  " name=" +  actuatorActionJSONObject.getString("Name"));

								// create the ProcessingLogicDene
								//
								actuatorLogicProcessingDene = new JSONObject();
								logger.debug("line 3240 creating processing for actuatorLogicProcessingDene=" + actuatorLogicProcessingDene );

								actuatorLogicProcessingDenes.put(actuatorLogicProcessingDene);

								actionName = actuatorActionJSONObject.getString("Name");

								actuatorLogicProcessingDeneName = codonName + "_" + actionName;
								actuatorLogicProcessingDene.put("Name", actuatorLogicProcessingDeneName);
								actuatorLogicProcessingDeneDeneWords = new JSONArray();
								actuatorLogicProcessingDene.put("DeneWords", actuatorLogicProcessingDeneDeneWords);
								actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", actuatorLogicProcessingDeneName ,null,"String",true);
								actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
								logger.debug("actuatorLogicProcessingCodonDeneDeneWorde=" + actuatorLogicProcessingCodonDeneDeneWord );


								//
								// the conditions for the action as a JSONArray of pointers to the denes
								// that have the information
								//
								JSONArray actuatorActionConditionsPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								actuatorActionConditionsJSONArray = loadDenesFromPointers(actuatorActionConditionsPointersJSONArray);

								logger.debug("line 3259="  );

								for(int j=0;j<actuatorActionConditionsJSONArray.length();j++){

									actuatorActionConditionJSONObject = (JSONObject) actuatorActionConditionsJSONArray.get(j);

									//
									// the condition is
									//String condition = "#Purpose:Purpose1:Operations:Bore Pump Status#==OFF";

									conditionName = actuatorActionConditionJSONObject.getString("Name");	
									logger.debug("line 3271 conditionname=" +  conditionName);

									actuatorLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject(conditionName + " Processing", "@" + conditionName + " Processing" ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
									actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingDeneDeneWord);

								}

							}

						} catch (JSONException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}
					// then do the standard
					//
					logger.debug("actuatorExecutionPositionDeneIndex=" + actuatorExecutionPositionDeneIndex.size());
					for (Map.Entry<JSONObject, Integer> entry : actuatorExecutionPositionDeneIndex) {
						anActuatorDeneJSONObject = entry.getKey();
						actuatorName = anActuatorDeneJSONObject.getString("Name");
						//actionListName = (String)getDeneWordAttributeByDeneWordTypeFromDene(anActuatorDeneJSONObject, TeleonomeConstants.DENE_TYPE_ACTION_LIST, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);

						//actuatorDeneNameActuatorActionEvaluationPositionActionIndex..get(actuatorName)
						codonName = actuatorName + " Processing";
						try { 
							actuatorLogicProcessingDeneChain.put("Denes", actuatorLogicProcessingDenes);
							actuatorLogicProcessingDeneChain.put("Name", TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);

							ArrayList<Map.Entry<JSONObject, Integer>> actuatorActionEvaluationPositionActionIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)actuatorDeneNameActuatorActionEvaluationPositionActionIndex.get(actuatorName);


							for (Map.Entry<JSONObject, Integer> action : actuatorActionEvaluationPositionActionIndex) {
								actuatorActionJSONObject = action.getKey();
								actionName = actuatorActionJSONObject.getString("Name");
								logger.debug("creating processing for actionName=" + actionName );
								// create the ProcessingLogicDene
								//
								actuatorLogicProcessingDene = new JSONObject();
								actuatorLogicProcessingDenes.put(actuatorLogicProcessingDene);
								actuatorLogicProcessingDeneName = codonName + "_" + actionName;
								actuatorLogicProcessingDene.put("Name", actuatorLogicProcessingDeneName);
								actuatorLogicProcessingDeneDeneWords = new JSONArray();
								actuatorLogicProcessingDene.put("DeneWords", actuatorLogicProcessingDeneDeneWords);
								actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", actuatorLogicProcessingDeneName ,null,"String",true);
								actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);


								//
								// the conditions for the action as a JSONArray of pointers to the denes
								// that have the information
								//
								JSONArray actuatorActionConditionsPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								actuatorActionConditionsJSONArray = loadDenesFromPointers(actuatorActionConditionsPointersJSONArray);


								for(int j=0;j<actuatorActionConditionsJSONArray.length();j++){

									actuatorActionConditionJSONObject = (JSONObject) actuatorActionConditionsJSONArray.get(j);

									//
									// the condition is
									//String condition = "#Purpose:Purpose1:Operations:Bore Pump Status#==OFF";

									conditionName = actuatorActionConditionJSONObject.getString("Name");				
									actuatorLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject(conditionName + " Processing", "@" + conditionName + " Processing" ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
									actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingDeneDeneWord);

								}

							}



						} catch (JSONException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
					}		

				}else if(aNucleusJSONObject.getString("Name").equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					deneChains = aNucleusJSONObject.getJSONArray("DeneChains");

					JSONObject deneChain;
					for(int j=0;j<deneChains.length();j++){
						deneChain = deneChains.getJSONObject(j);
						if(deneChain.getString("Name").equals(TeleonomeConstants.DENECHAIN_DESCRIPTIVE)){
							//
							// remove and regenerate the ProcessorInfo and USB device
							//
							JSONArray denes = deneChain.getJSONArray("Denes");

							int deneRemoved=0;
							try {
								deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_PROCESSOR_INFO);
								logger.debug("removing the DENE_PROCESSOR_INFO Info data dene from operational, result:" + deneRemoved);
								JSONObject processorInfoDene = Utils.getProcesorInfoDene();
								denes.put(processorInfoDene);


								deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_USB_DEVICES);
								logger.debug("removing the DENE_USB_DEVICES Info data dene from operational, result:" + deneRemoved);
								JSONObject usbDevicesDene = Utils.getUSBDevicesDene();
								denes.put(usbDevicesDene);

								deneRemoved = DenomeUtils.removeDeneFromChain(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_COMPUTER_INFO);
								logger.debug("removing the DENE_COMPUTER_INFO Info data dene from operational, result:" + deneRemoved);
								JSONObject computerInfoDene = Utils.getComputerInfoDene();
								denes.put(computerInfoDene);

							} catch (InvalidDenomeException e) {
								// TODO Auto-generated catch block
								logger.warn(Utils.getStringException(e));
								logger.debug("did not find Wifi Info at initpulse while trying to remove it");
							}
						}
					}

				}else if(aNucleusJSONObject.getString("Name").equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					deneChains = aNucleusJSONObject.getJSONArray("DeneChains");
					//
					// now do the Mnemosyne Pulse denechain
					// first remove the existing one
					// and then add it
					try {
						success = DenomeUtils.removeChainFromNucleus(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_MNEMOSYNE, TeleonomeConstants.DENECHAIN_MNEMOSYNE_PULSE);
						logger.debug("removing the MNEMOSYNE PULSE, result:" + success);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.debug("did not find MNEMOSYNE PULSE denechain");
					}	


					JSONObject mnemosynePulseDeneChain = new JSONObject();
					deneChains.put(mnemosynePulseDeneChain);
					JSONArray mnemosynePulseDenes = new JSONArray();

					mnemosynePulseDeneChain.put("Denes", mnemosynePulseDenes);
					mnemosynePulseDeneChain.put("Name", TeleonomeConstants.DENECHAIN_MNEMOSYNE_PULSE);
				}
			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}




		try {
			JSONObject currentlyCreatingPulseDenomeJSONObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
			currentlyCreatingPulseDenomeJSONObject.put("Host Name", hostName);


		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		logger.debug("finished initializing pulse");
		return teleonomeName;

	}

	public JSONObject injectDeneChainIntoNucleus(JSONObject sourceOfData, String nucleusName, JSONObject deneChain) throws JSONException{
		JSONObject denomeArray = sourceOfData.getJSONObject("Denome");
		JSONArray nucleiArray = denomeArray.getJSONArray("Nuclei");
		JSONObject aNucleusJSONObject, aDeneChain;
		JSONArray deneChains;
		for(int i=0;i<nucleiArray.length();i++){
			aNucleusJSONObject = nucleiArray.getJSONObject(i);
			if(aNucleusJSONObject.getString("Name").equals(nucleusName)){
				deneChains = aNucleusJSONObject.getJSONArray("DeneChains");
				deneChains.put(deneChain);
			}
		}
		return null;
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

	public JSONArray getAllDenesFromDeneChainByDeneName(JSONObject deneChain, String deneName) throws JSONException{
		JSONArray denesJSONArray = deneChain.getJSONArray("Denes");
		JSONArray toReturn = new JSONArray();
		logger.debug("getDeneFromDeneChainByDeneName point 2, deneName=" + deneName + " deneChain=" + deneChain.getString("Name"));
		JSONObject aDeneJSONObject;
		for(int j=0;j<denesJSONArray.length();j++){
			aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
			//logger.debug("getdenebyidentity point3 " + aDeneJSONObject.getString("Name"));
			if(aDeneJSONObject.getString("Name").equals(deneName)){
				toReturn.put(aDeneJSONObject);
			}
		}
		return toReturn;
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
					if(aDeneChain.has("Name") && aDeneChain.get("Name").equals(deneChainName)){
						return aDeneChain;
					}
				}
			}
		}
		return null;
	}

	public JSONObject getCurrentlyCreatingPulseJSONObject(){
		return currentlyCreatingPulseJSONObject;
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


	public void addSensorValueRenderedDeneWordToPulse(JSONObject currentlyProcessingSensorValueDene, String inputLine){

		//
		// get the address of the deneword where this data is going to
		String reportingAddress, deneWordName, sourceDeneName, unit, timeStringFormat, valueType;
		double rangeMaximum=0, rangeMinimum=0, renderedValue;
		boolean ignoreMaximumMinimum=false;
		try {
			sourceDeneName = currentlyProcessingSensorValueDene.getString("Name");
			reportingAddress = (String) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,"Reporting Address");
			unit = (String) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.DENEWORD_UNIT_ATTRIBUTE);

			valueType = (String) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);

			//
			// rangeMaximum and rangeMinimum can be null, if the data been sent from the sensor is of type ON/OFF or some
			// other non numerical information
			try{
				Double parseValue = Double.parseDouble(inputLine.trim());
			}catch(NumberFormatException e){
				logger.info(inputLine + " is not numeric");
				ignoreMaximumMinimum=true;
			}
			if(!ignoreMaximumMinimum){
				try{
					//
					// the maximum and minimum can come in two forms,
					// they can either be a number or they can be a pointer to another dene
					// this second case will be for example if there is a temperature pump controlled pump
					// and the maximum value for the temperature of the pump is given by another dene
					Object rangeMaximumRawValue  = extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.SENSOR_VALUE_RANGE_MAXIMUM);
					if(rangeMaximumRawValue instanceof String && rangeMaximumRawValue.toString().startsWith("@")){
						rangeMaximum = Double.parseDouble((String)this.getDeneWordAttributeByIdentity(new Identity(rangeMaximumRawValue.toString()), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						logger.info("rangemaximum is a pointer and the rendered value is " + rangeMaximum);
					}else{
						try{
							rangeMaximum = (Double) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.SENSOR_VALUE_RANGE_MAXIMUM);
						}catch(java.lang.ClassCastException e) {
							logger.info("class cast exception to double,inputLine=" + inputLine);
							rangeMaximum = ((Integer) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.SENSOR_VALUE_RANGE_MAXIMUM)).doubleValue();
						}
					}

					Object rangeMinimumRawValue  = extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.SENSOR_VALUE_RANGE_MINIMUM);
					if(rangeMinimumRawValue instanceof String && rangeMinimumRawValue.toString().startsWith("@")){
						rangeMinimum = Double.parseDouble((String)this.getDeneWordAttributeByIdentity(new Identity(rangeMinimumRawValue.toString()), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));

					}else{
						try{
							rangeMinimum = (Double) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.SENSOR_VALUE_RANGE_MINIMUM);
						}catch(java.lang.ClassCastException e) {
							logger.info("class cast exception to double,inputLine=" + inputLine);
							rangeMinimum = ((Integer) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.SENSOR_VALUE_RANGE_MINIMUM)).doubleValue();
						}
					}
				}catch(NullPointerException e){
					//
					// if there is no info for max and min it will throw a null pointer.  this can 
					// happen when the value in question is long for a timestamp type of value
					ignoreMaximumMinimum=true;
				} catch (InvalidDenomeException e) {
					// TODO Auto-generated catch block
					ignoreMaximumMinimum=true;
					e.printStackTrace();
				}
			}
			//
			// the address will be of the form # 
			String[] tokens = reportingAddress.substring(1,reportingAddress.length()).split(":");
			String teleonomeName = tokens[0];
			String nucleusName = tokens[1];
			String deneChainName = tokens[2];
			String deneName = tokens[3];
			String deneWordLabel = tokens[4];

			logger.info("line 2704 denomemanager, " + reportingAddress + " sourceDeneName=" + sourceDeneName + " unit=" + unit);
			JSONObject currentlyCreatingPulseDenome = currentlyCreatingPulseJSONObject.getJSONObject("Denome");

			JSONArray currentlyCreatingPulseNuclei = currentlyCreatingPulseDenome.getJSONArray("Nuclei");
			JSONArray deneWords;

			JSONObject jsonObject, jsonObjectChain, jsonObjectDene, jsonObjectDeneWord;
			JSONArray chains, denes;
			for(int i=0;i<currentlyCreatingPulseNuclei.length();i++){
				jsonObject = currentlyCreatingPulseNuclei.getJSONObject(i);
				if(jsonObject.getString("Name").equals(nucleusName)){
					chains = jsonObject.getJSONArray("DeneChains");
					for(int j=0;j<chains.length();j++){
						jsonObjectChain = chains.getJSONObject(j);
						if(jsonObjectChain.toString().length()>10 && jsonObjectChain.getString("Name").equals(deneChainName)){
							denes = jsonObjectChain.getJSONArray("Denes");

							for(int k=0;k<denes.length();k++){
								jsonObjectDene = denes.getJSONObject(k);
								if(jsonObjectDene.getString("Name").equals(deneName)){
									deneWords = jsonObjectDene.getJSONArray("DeneWords");
									for(int l=0;l<deneWords.length();l++){
										jsonObjectDeneWord = deneWords.getJSONObject(l);
										//logger.info("jsonObjectDeneWord, " + jsonObjectDeneWord.getString("Name") +  " deneWordLabel=" + deneWordLabel);
										if(jsonObjectDeneWord.getString("Name").equals(deneWordLabel)){
											//
											// store the value in the deneword
											// if its a timestring, you need to convert it
											// to milliseconds
											//


											if(unit!=null && unit.equals(TeleonomeConstants.DENEWORD_TIMESTRING_VALUE)){

												timeStringFormat = (String) extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.DENEWORD_TIMESTRING_FORMAT_VALUE);
												SimpleDateFormat sd = new SimpleDateFormat(timeStringFormat);
												logger.info("timeStringFormat, " + timeStringFormat);

												try {
													Date d = sd.parse(inputLine);
													logger.info("d.getTime(), " + d.getTime());
													jsonObjectDeneWord.put("Value", d.getTime());
												} catch (ParseException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
													jsonObjectDeneWord.put("Value", inputLine);
												}

											}else{
												logger.info("line 5449 valueType=" + valueType + " the value as line =" + inputLine);
												//
												// now check to see what type of value it is
												//
												if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)) {
													int v = Integer.parseInt(inputLine);
													logger.info("storing into value inte ger =" + v);
													jsonObjectDeneWord.put("Value", v);
												}else if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)) {
													double v = Double.parseDouble(inputLine);
													logger.info("storing into value double=" + v);
													jsonObjectDeneWord.put("Value", v);
												}else if(valueType.equals(TeleonomeConstants.DATATYPE_LONG)) {
													long v = Long.parseLong(inputLine);
													logger.info("storing into value long=" + v);
													jsonObjectDeneWord.put("Value", v);
												}else if(valueType.equals(TeleonomeConstants.DATATYPE_STRING)) {
													jsonObjectDeneWord.put("Value", inputLine);
												}
												
												
											}

											//
											// now check to see if the values numeric and are out of range
											//
											//
											if(!ignoreMaximumMinimum){
												renderedValue = Double.parseDouble(inputLine);
												if(renderedValue>rangeMaximum || renderedValue<rangeMinimum){

													//
													// we are out of range, so write a pathology dene
													//
													String pathologyCause = TeleonomeConstants.PATHOLOGY_DATA_OUT_OF_RANGE;
													String pathologyName = TeleonomeConstants.PATHOLOGY_DENE_SENSOR_OUT_OF_RANGE;
													String pathologyLocation = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_SENSORS,sourceDeneName  ).toString();
													Vector extraDeneWords = new Vector();

													JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_VALUE_RANGE_MAXIMUM, ""+rangeMaximum ,null,"double",true);
													extraDeneWords.addElement(pathologyDeneDeneWord);

													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_VALUE_RANGE_MINIMUM, ""+rangeMinimum ,null,"double",true);
													extraDeneWords.addElement(pathologyDeneDeneWord);

													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_CURRENT_VALUE, ""+renderedValue ,null,"double",true);
													extraDeneWords.addElement(pathologyDeneDeneWord);

													addPurposePathologyDene(pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);


													/*

													JSONObject pathologyDeneChain = null, pathologyDeneDeneWord;
													JSONArray pathologyDenes=null, pathologyDeneDeneWords;
													JSONObject pathologyDene;
													String pathologyLocation = "";
													try {
														pathologyDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_PATHOLOGY);
														pathologyDenes = pathologyDeneChain.getJSONArray("Denes");

													} catch (JSONException e2) {
														// TODO Auto-generated catch block
														e2.printStackTrace();
													}

													//
													// now create the pathology dene
													//
													pathologyDene = new JSONObject();
													pathologyDenes.put(pathologyDene);

													pathologyDene.put("Name", TeleonomeConstants.PATHOLOGY_DENE_SENSOR_OUT_OF_RANGE);
													pathologyDeneDeneWords = new JSONArray();

													pathologyDene.put("DeneWords", pathologyDeneDeneWords);
													//
													// create the Cause deneword
													//
													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, TeleonomeConstants.PATHOLOGY_DATA_OUT_OF_RANGE ,null,"String",true);
													pathologyDeneDeneWords.put(pathologyDeneDeneWord);
													//
													// create the location deneword
													pathologyLocation = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_SENSORS,sourceDeneName  ).toString();
													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
													pathologyDeneDeneWords.put(pathologyDeneDeneWord);
													//
													// to make it easier to display the pathology dene, add the current value as well
													// as the thresholds

													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_VALUE_RANGE_MAXIMUM, ""+rangeMaximum ,null,"double",true);
													pathologyDeneDeneWords.put(pathologyDeneDeneWord);

													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_VALUE_RANGE_MINIMUM, ""+rangeMinimum ,null,"double",true);
													pathologyDeneDeneWords.put(pathologyDeneDeneWord);

													pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_CURRENT_VALUE, ""+renderedValue ,null,"double",true);
													pathologyDeneDeneWords.put(pathologyDeneDeneWord);
													 */
												}
											}

										}
									}
								}
							}
						}
					}
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}


	}

	public void addExogenousMetamorphosisEventDeneToMnemosyneDeneChain(Identity mnemosyneDeneChainIdentity) throws JSONException, InvalidDenomeException{
		JSONObject   deneDeneWord;
		JSONArray denes=null;
		JSONObject dene;
		try {
			JSONObject metamorphosisEventsMnemosyneDestinationDeneChain = aDenomeManager.getDeneChainByIdentity(mnemosyneDeneChainIdentity);

			denes = metamorphosisEventsMnemosyneDestinationDeneChain.getJSONArray("Denes");

		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		//
		// now create the pathology dene
		//
		dene = new JSONObject();
		denes.put(dene);
		dene.put("Name", TeleonomeConstants.DENE_TYPE_IDENTITY_SWITCH_EVENT);
		dene.put(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE, TeleonomeConstants.DENE_TYPE_IDENTITY_SWITCH_EVENT);
		LocalDateTime currentTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
		String formatedCurrentTime = currentTime.format(formatter);

		dene.put("Timestamp", formatedCurrentTime);
		dene.put("Timestamp Milliseconds",  System.currentTimeMillis());

		JSONArray deneWords = new JSONArray();

		dene.put("DeneWords", deneWords);

		Calendar cal = Calendar.getInstance();
		JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.IDENTITY_SWITCH_EVENT_MILLISECONDS, "" + cal.getTime().getTime() ,null,"long",true);
		deneWords.put(pathologyDeneDeneWord);
		pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.IDENTITY_SWITCH_EVENT_TIMESTAMP, simpleFormatter.format(cal.getTime()) ,null,"long",true);
		deneWords.put(pathologyDeneDeneWord);

	}

	//	public void addPathologyDeneToMnemosyne(String pathologyDeneName, String pathologyCause, String pathologyLocation, Vector<JSONObject> extraDeneWords) throws JSONException{
	//		JSONObject pathologyDeneChain = null;
	//		try {
	//			pathologyDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_MNEMOSYNE,TeleonomeConstants.DENECHAIN_MNEMOSYNE_PATHOLOGY);
	//			addPathologyDene(pathologyDeneChain, pathologyDeneName,  pathologyCause,  pathologyLocation, extraDeneWords);
	//		} catch (JSONException e2) {
	//			// TODO Auto-generated catch block
	//			e2.printStackTrace();
	//		}
	//	}

	public void addPurposePathologyDene(String pathologyDeneName, String pathologyCause, String pathologyLocation, Vector<JSONObject> extraDeneWords) throws JSONException{
		JSONObject pathologyDeneChain = null;
		try {
			pathologyDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_PATHOLOGY);
			addPathologyDene(pathologyDeneChain, pathologyDeneName,  pathologyCause,  pathologyLocation, extraDeneWords);
		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}


	public void addFaultPathologyDene(String microControllerPointer, String pathologyName, String pathologyCause, String pathologyLocation, Vector<JSONObject> extraDeneWords) throws JSONException{
		JSONObject selectedPathologyDeneChain = null;
		try {
			//
			// add a pathology dene to the denome
			// fiurst get the location, it will be a pointer to a denechain inside of the mnemosyne
			//
			Identity id2 = new Identity(microControllerPointer);
			JSONObject microControllerDene = this.getDeneByIdentity(id2);
			String mnemosyneLocationPointer = (String) getDeneWordAttributeByDeneWordTypeFromDene(microControllerDene, TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_FAULT_PATHOLOGY_MNEMOSYNE_LOCATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			if(mnemosyneLocationPointer!=null) {
				String medulaPathologyLocationPointer  = (String) DenomeUtils.getDeneWordByIdentity(denomeJSONObject, id2, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				selectedPathologyDeneChain = DenomeUtils.getDeneChainByIdentity(denomeJSONObject, new Identity(medulaPathologyLocationPointer));


				Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));
				JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_MILLISECONDS, "" + cal.getTime().getTime() ,null,"long",true);
				extraDeneWords.addElement(pathologyDeneDeneWord);
				pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_TIMESTAMP, simpleFormatter.format(cal.getTime()) ,null,"long",true);
				extraDeneWords.addElement(pathologyDeneDeneWord);



				JSONArray pathologyDenes=null, pathologyDeneDeneWords;
				JSONObject pathologyDene;
				try {
					pathologyDenes = selectedPathologyDeneChain.getJSONArray("Denes");

				} catch (JSONException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}

				//
				// now create the pathology dene
				//
				pathologyDene = new JSONObject();
				pathologyDenes.put(pathologyDene);

				pathologyDene.put("Name", pathologyName);
				pathologyDeneDeneWords = new JSONArray();
				pathologyDene.put(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE, TeleonomeConstants.DENE_PATHOLOGY);

				LocalDateTime currentTime = LocalDateTime.now();
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
				String formatedCurrentTime = currentTime.format(formatter);
				pathologyDene.put(TeleonomeConstants.DATATYPE_TIMESTAMP, formatedCurrentTime);

				pathologyDene.put(TeleonomeConstants.DATATYPE_TIMESTAMP_MILLISECONDS, System.currentTimeMillis());
				pathologyDene.put("DeneWords", pathologyDeneDeneWords);
				//
				// create the Cause deneword
				//
				pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, pathologyCause ,null,"String",true);
				pathologyDeneDeneWords.put(pathologyDeneDeneWord);
				//
				// create the location deneword
				pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
				pathologyDeneDeneWords.put(pathologyDeneDeneWord);
				//
				// to make it easier to display the pathology dene, add the current value as well
				// as the thresholds

				for(int i=0;i<extraDeneWords.size();i++){
					pathologyDeneDeneWord=(JSONObject)extraDeneWords.elementAt(i);
					pathologyDeneDeneWords.put(pathologyDeneDeneWord);
				}


				try {
					FileUtils.write(new File(Utils.getLocalDirectory() + "Teleonome.denome"), denomeJSONObject.toString(4));
					FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/Teleonome.denome"), denomeJSONObject.toString(4));

				} catch (IOException | JSONException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			}


		} catch (InvalidDenomeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}


	private void addPathologyDene(JSONObject pathologyDeneChain, String pathologyDeneName, String pathologyCause, String pathologyLocation, Vector<JSONObject> extraDeneWords) throws JSONException{
		JSONObject   pathologyDeneDeneWord;
		JSONArray pathologyDenes=null, pathologyDeneDeneWords;
		JSONObject pathologyDene;
		try {
			pathologyDenes = pathologyDeneChain.getJSONArray("Denes");

		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		//
		// now create the pathology dene
		//
		pathologyDene = new JSONObject();
		pathologyDenes.put(pathologyDene);

		pathologyDene.put("Name", pathologyDeneName);
		pathologyDeneDeneWords = new JSONArray();

		pathologyDene.put("DeneWords", pathologyDeneDeneWords);
		//
		// create the Cause deneword
		//
		pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, pathologyCause ,null,"String",true);
		pathologyDeneDeneWords.put(pathologyDeneDeneWord);
		//
		// create the location deneword
		pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
		pathologyDeneDeneWords.put(pathologyDeneDeneWord);
		//
		// to make it easier to display the pathology dene, add the current value as well
		// as the thresholds

		for(int i=0;i<extraDeneWords.size();i++){
			pathologyDeneDeneWord=(JSONObject)extraDeneWords.elementAt(i);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);
		}

	}


	public Object getDeneWordAttributeByDeneWordTypeFromDene(JSONObject deneJSONObject , String type, String whatToBring) throws JSONException{
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			if(!deneWord.has("DeneWord Type"))continue;
			String deneWordValueType = deneWord.getString("DeneWord Type");
			if(deneWordValueType.equals(type)){
				if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
					return deneWord;
				}else{
					return deneWord.get(whatToBring);
				}

			}
		}
		return null;
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


	public Object getDeneWordAttributeByDeneWordNameFromDene(JSONObject deneJSONObject , String name, String whatToBring) throws JSONException{
		logger.debug("getDeneWordAttributeByDeneWordNameFromDene, name=" + name + " deneJSONObject=" + deneJSONObject);
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			String deneWordName = deneWord.getString("Name");
			logger.debug("getByName, deneWordName=" + deneWordName );

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

	public ArrayList evaluateMutationAction(Identity identity) throws InvalidDenomeException{
		JSONObject actuatorActionJSONObject = getDeneByIdentity(identity);
		return evaluateMutationAction(actuatorActionJSONObject);	
	}


	public ArrayList evaluateMutationAction(JSONObject actuatorActionJSONObject) throws InvalidDenomeException{

		String actionName = null, actionExpressionString = null;
		try {
			actionName = actuatorActionJSONObject.getString("Name");
			boolean active = (Boolean)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,"Active",TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			logger.debug("in evaluateMutationAction, actionName=" + actionName + " active=" + active);

			if(!active)return null;

			actionExpressionString = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,"Expression",TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		//
		// we dont care about the integer, since it is already sorted
		JSONObject actuatorLogicProcessingDene = new JSONObject();

		try {

			logger.debug("in evaluateMutationAction, actionExpressionString=" + actionExpressionString);

			//
			// create the actionExpression and condition
			Expression actionExpression = jexl.createExpression(actionExpressionString);
			MapContext jexlActionContext = new MapContext();
			//
			// now evaluate every one of the conditions 
			// by creating an expression for each condition
			// this conditionexpressin will then be fed to 
			// the action expression for final evaluation
			//
			JSONArray actuatorActionConditionPointersJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONArray actuatorActionConditionNamesJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
			String actuatorActionConditionPointer, conditionName, codonName, actuatorLogicProcessingDeneName;
			JSONObject actuatorActionConditionJSONObject;
			Expression conditionExpression = null;
			MapContext jexlConditionContext = null;
			JSONObject actuatorActionConditionVariableJSONObject;
			boolean allVariableInConditionRenderedSuccesfully = false;
			String actuatorActionConditionVariable_Name = "";
			String actuatorActionConditionVariable_Value = "";
			String actuatorActionConditionVariable_Type="";
			Object actuatorActionConditionVariable_Value_Rendered=null;

			String commandToExecute="";
			logger.debug("in evaluateMutationAction, actuatorActionConditionPointersJSONArray=" + actuatorActionConditionPointersJSONArray);
			//
			// set this variable here because there is a posibility that this action
			// does not have any conditions, like int he case where the epxression is 1==1
			// abd actuatorActionConditionPointersJSONArray=[] so it will not go into the loop
			//
			allVariableInConditionRenderedSuccesfully=true;

			for(int j=0;j<actuatorActionConditionPointersJSONArray.length();j++){
				actuatorActionConditionPointer = (String) actuatorActionConditionPointersJSONArray.getString(j);
				conditionName = actuatorActionConditionNamesJSONArray.getString(j);

				actuatorActionConditionJSONObject=null;
				//
				// actuatorActionConditionJSONObject is a deneword get the value which is a denepointer and render it
				try {
					actuatorActionConditionJSONObject = getDeneByIdentity(new Identity(actuatorActionConditionPointer));
				} catch (InvalidDenomeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//
				// the condition is
				//String condition = "#Purpose:Purpose1:Operations:Bore Pump Status#==OFF";

				//conditionName = actuatorActionConditionJSONObject.getString("Name");

				codonName = actionName + " Processing";
				//
				// Create the condition Name
				//
				actuatorLogicProcessingDeneName = codonName + " " +actionName + " " +conditionName;

				String actuatorActionConditionVariable_Expression = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionConditionJSONObject, "Expression", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


				//
				// processing logic 
				//
				actuatorLogicProcessingDene.put("Name", actuatorLogicProcessingDeneName);
				JSONArray actuatorLogicProcessingDeneDeneWords = new JSONArray();
				actuatorLogicProcessingDene.put("DeneWords", actuatorLogicProcessingDeneDeneWords);
				JSONObject actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", codonName,null,"String",true);
				actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
				actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Expression", actuatorActionConditionVariable_Expression,null,"String",true);
				actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);


				boolean onLackOfDataForCondition = (boolean) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionConditionJSONObject, "On Lack Of Data", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				JSONArray actuatorActionConditionVariablesPointersJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actuatorActionConditionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE,  TeleonomeConstants.DENEWORD_TYPE_CONDITION_VARIABLE_POINTER,TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONArray actuatorActionConditionVariablesJSONArray = renderDeneWordsFromPointers(actuatorActionConditionVariablesPointersJSONArray);

				//actuatorActionConditionVariablesJSONArray  = actuatorActionConditionJSONObject.getJSONArray("Variables");

				conditionExpression = jexl.createExpression(actuatorActionConditionVariable_Expression);
				jexlConditionContext = new MapContext();

				//
				// All variables in a condition must be rendered for the condition to be evaluate it.
				//set this flag to true, if any of the data is not there, then it will set to false
				//
				allVariableInConditionRenderedSuccesfully=true;

				for(int k=0;k<actuatorActionConditionVariablesJSONArray.length();k++){
					actuatorActionConditionVariableJSONObject = (JSONObject) actuatorActionConditionVariablesJSONArray.get(k);

					actuatorActionConditionVariable_Name = actuatorActionConditionVariableJSONObject.getString("Name");
					actuatorActionConditionVariable_Value = actuatorActionConditionVariableJSONObject.getString("Value");
					actuatorActionConditionVariable_Type = actuatorActionConditionVariableJSONObject.getString("Value Type");


					if(actuatorActionConditionVariable_Value.startsWith("@")){
						//
						// this is a pointer to another value in the denome, so get it
						try {
							actuatorActionConditionVariable_Value_Rendered = getCurrentPulseValueForDeneWord(actuatorActionConditionVariable_Value);
							//
							//
							// if we are referencing external data and the data has not arrived yet
							// the value will be "Undefined"
							if(actuatorActionConditionVariable_Value_Rendered.equals(TeleonomeConstants.VALUE_UNDEFINED)){
								//
								// check to see if there is a default, will throw an exception if its not there
								//
								try{
									actuatorActionConditionVariable_Value_Rendered = actuatorActionConditionVariableJSONObject.getString("Default");
									if(actuatorActionConditionVariable_Value_Rendered.equals(TeleonomeConstants.VALUE_UNDEFINED)){
										allVariableInConditionRenderedSuccesfully=false;
									}
								}catch(JSONException e){
									allVariableInConditionRenderedSuccesfully=false;
								}

							}
						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}

						//
						// this is a command
						if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
							actuatorActionConditionVariable_Value_Rendered = System.currentTimeMillis();
						}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_MINUTE_IN_THE_HOUR)){
							Calendar rightNow = Calendar.getInstance();
							actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.MINUTE);
							logger.debug("rendering command of COMMANDS_CURRENT_MINUTE_IN_THE_HOUR= " + actuatorActionConditionVariable_Value_Rendered);

						}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_HOUR_IN_DAY)){
							Calendar rightNow = Calendar.getInstance();
							actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.HOUR_OF_DAY);
							logger.debug("rendering command of COMMANDS_CURRENT_HOUR_IN_DAY= " + actuatorActionConditionVariable_Value_Rendered);

						}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_DAY_IN_WEEK)){
							Calendar rightNow = Calendar.getInstance();
							actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.DAY_OF_WEEK);
							logger.debug("rendering command of COMMANDS_CURRENT_DAY_IN_WEEK= " + actuatorActionConditionVariable_Value_Rendered);

						}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_DAY_IN_MONTH)){
							Calendar rightNow = Calendar.getInstance();
							actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.DAY_OF_MONTH);
							logger.debug("rendering command of COMMANDS_CURRENT_DAY_IN_MONTH= " + actuatorActionConditionVariable_Value_Rendered);

						}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP)){
							actuatorActionConditionVariable_Value_Rendered = dateTimeFormat.format(new Timestamp(System.currentTimeMillis()));
						}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_PREVIOUS_PULSE_MILLIS)){

							if(previousPulseJSONObject!=null){
								long previousPulseTimestamp = previousPulseJSONObject.getLong("Pulse Timestamp in Milliseconds");
								actuatorActionConditionVariable_Value_Rendered = previousPulseTimestamp;

							}else{
								allVariableInConditionRenderedSuccesfully=false;
								logger.debug("rendering variable with previous pulse millis, previousPulseJSONObject is null");
							}
						}
					}else{


						if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.VALUE_UNDEFINED)){
							//
							// check to see if there is a default, will throw an exception if its not there
							//
							try{
								actuatorActionConditionVariable_Value = actuatorActionConditionVariableJSONObject.getString("Default");
								if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.VALUE_UNDEFINED)){
									allVariableInConditionRenderedSuccesfully=false;
								}
							}catch(JSONException e){
								allVariableInConditionRenderedSuccesfully=false;
							}

						}
						actuatorActionConditionVariable_Value_Rendered=actuatorActionConditionVariable_Value;



					}



					actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(conditionName, actuatorActionConditionVariable_Value_Rendered.toString(),null,actuatorActionConditionVariable_Type.toString(),true);
					actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
					logger.debug("actuatorActionConditionVariable_Name=" + actuatorActionConditionVariable_Name + " actuatorActionConditionVariable_Value=" + actuatorActionConditionVariable_Value);

					if(!actuatorActionConditionVariable_Value_Rendered.equals(TeleonomeConstants.VALUE_UNDEFINED)){
						jexlConditionContext.set(actuatorActionConditionVariable_Name,actuatorActionConditionVariable_Value_Rendered);
					}


				}

				//
				// Now that we finished setting the variables of the condition, evaluate it
				// as long as allVariableInConditionRenderedSuccesfully=true


				//
				// and set this value in the actionMap
				logger.debug("allVariableInConditionRenderedSuccesfully=" + allVariableInConditionRenderedSuccesfully);
				if(allVariableInConditionRenderedSuccesfully){
					boolean conditionEval = ((Boolean)conditionExpression.evaluate(jexlConditionContext)).booleanValue();
					jexlActionContext.set(conditionName,conditionEval);

					actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Result", new Boolean(conditionEval).toString(),null,"boolean",true);
					actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
					logger.debug("conditionName=" + conditionName+ " conditionEval=" + conditionEval);

				}else{
					jexlActionContext.set(conditionName,onLackOfDataForCondition);
					actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Result", new Boolean(onLackOfDataForCondition).toString(),null,"boolean",true);
					actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
				}
			}
			//
			// now that we finished evaluating every condition
			// evaluate the expression for the action
			//
			logger.debug("in evaluateMutationAction at the end , allVariableInConditionRenderedSuccesfully=" + allVariableInConditionRenderedSuccesfully);


			if(allVariableInConditionRenderedSuccesfully){
				if(((Boolean)actionExpression.evaluate(jexlActionContext)).booleanValue()){
					//
					// if we are here is because thee expression came back as true
					// so the command needs to be executed, so extract it and 
					// store it
					// 

					commandToExecute = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("in  true commandToExecute=" + commandToExecute + " actuatorActionJSONObject="  + actuatorActionJSONObject);

					ArrayList<Map.Entry<String, JSONObject>> toReturn = new ArrayList();
					toReturn.add(new AbstractMap.SimpleEntry<String, JSONObject>(commandToExecute, actuatorLogicProcessingDene));	
					return toReturn;

				}
			}else{


				commandToExecute = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_FALSE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("in  false commandToExecute=" + commandToExecute);

				ArrayList<Map.Entry<String, JSONObject>> toReturn = new ArrayList();
				toReturn.add(new AbstractMap.SimpleEntry<String, JSONObject>(commandToExecute, actuatorLogicProcessingDene));	
				return toReturn;

			}

		} catch (JSONException e1) {
			// TODO Auto-generated catch block

			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			logger.debug("exception thrown:" + e1.getMessage());
			e1.printStackTrace();
		}
		return null;
	}


	/**
	 * this methods returns the actions for an actuator ordered according to their execution position
	 * @param teleonomeName
	 * @param anActuatorActionListDeneJSONObject
	 * @param forInitial
	 * @return
	 * @throws JSONException
	 */

	public ArrayList<Map.Entry<JSONObject, Integer>> getOrderedActuatorDeneActions(String teleonomeName, JSONObject anActuatorActionListDeneJSONObject, boolean forInitial) throws JSONException{
		//
		//ok now the actions are in the correct order so evaluate each one of them
		//
		String actionListName="";
		try {
			actionListName = anActuatorActionListDeneJSONObject.getString("Name");
			logger.debug("line 4953 actionListName=" + actionListName);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		ArrayList<Map.Entry<String, JSONArray>> actuatorCommandCodeActionSuccessTaskPointerArrayList = new ArrayList(); 

		ArrayList<Map.Entry<JSONObject, Integer>> actuatorActionEvaluationPositionActionIndex=null;
		if(forInitial){
			actuatorActionEvaluationPositionActionIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex.get(actionListName);		

		}else{
			actuatorActionEvaluationPositionActionIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)actuatorDeneNameActuatorActionEvaluationPositionActionIndex.get(actionListName);		
		}
		return actuatorActionEvaluationPositionActionIndex;
	}


	//
	// this method has been deprecated because it does not take into account that the expresion of an action can be modified
	// by another action that executes before
	//	public ArrayList<Map.Entry<String, JSONArray>> evaluateActuatorDene(String teleonomeName, JSONObject anActuatorActionListDeneJSONObject, boolean forInitial) throws JSONException{
	//		//
	//		//ok now the actions are in the correct order so evaluate each one of them
	//		//
	//		String actionListName="", executionPoint;
	//		try {
	//			actionListName = anActuatorActionListDeneJSONObject.getString("Name");
	//			logger.debug("line 4953 actionListName=" + actionListName);
	//		} catch (JSONException e) {
	//			// TODO Auto-generated catch block
	//			logger.warn(Utils.getStringException(e));
	//		}
	//		ArrayList<Map.Entry<String, JSONArray>> actuatorCommandCodeActionSuccessTaskPointerArrayList = new ArrayList(); 
	//
	//		ArrayList<Map.Entry<JSONObject, Integer>> actuatorActionEvaluationPositionActionIndex=null;
	//		if(forInitial){
	//			actuatorActionEvaluationPositionActionIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)actuatorDeneNameActuatorActionEvaluationPositionActionForInitialIndex.get(actionListName);		
	//
	//		}else{
	//			actuatorActionEvaluationPositionActionIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)actuatorDeneNameActuatorActionEvaluationPositionActionIndex.get(actionListName);		
	//		}
	//
	//		JSONObject actuatorActionJSONObject;
	//		ArrayList arrayList;
	//		AbstractMap.SimpleEntry<String, JSONArray> entry;
	//		String codonName;
	//		//
	//		// check to see if actuatorActionEvaluationPositionActionIndex is null, because you could have an actuator
	//		// that only has startup actions and therefore it could be null
	//		String commandToExecute, pointerToActionSuccessTasks;
	//		String payload;
	//		logger.info("line 4979 actuatorActionEvaluationPositionActionIndex=" + actuatorActionEvaluationPositionActionIndex);
	//		if(actuatorActionEvaluationPositionActionIndex!=null){
	//			for (Map.Entry<JSONObject, Integer> action : actuatorActionEvaluationPositionActionIndex) {
	//				actuatorActionJSONObject = action.getKey();
	//				entry = evaluateAction(teleonomeName, actuatorActionJSONObject);
	//				logger.info(" line 4983 actuatorAction=" + actuatorActionJSONObject.getString("Name") + " returns " + entry);
	//				//
	//				// if the action is not active, ie the Active DeneWord is set to false
	//				//it will return a null, so only add them if they are not null
	//				if(entry!=null){
	//					//
	//					// if we are here is because the action needs to be executed
	//					// now check to see if the action has an 
	//					// TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT
	//					// Deneword
	//					executionPoint = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE );
	//					if(executionPoint==null || executionPoint.equals(TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT_IMMEDIATE)){
	//						actuatorCommandCodeActionSuccessTaskPointerArrayList.add(entry);	
	//					}else if(executionPoint!=null || executionPoint.equals(TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT_POST_PULSE)){
	//						//
	//						// we need to create a Command that will be executed
	//						// after the pulse, during the CommandRequests
	//						commandToExecute = entry.getKey();
	//
	//						payload="";
	//						int id = aDBManager.requestCommandToExecute(commandToExecute, payload);
	//					}{
	//
	//					}
	//				}
	//			}
	//		}
	//		return actuatorCommandCodeActionSuccessTaskPointerArrayList;
	//	}

	/**
	 * 
	 *  this method is called by the Pulse Thread when an  action 
	 *  has a pointer instead of an actual code
	 *  This method loads the dene and gets its DeneType
	 *  it then invoques the appropiate method that executes the function
	 *  Note that in compatibility with previous version of the Denome
	 *  
	 *  of this action is not sent to the microcontroller but instead an operation is performed
	 *  
	 * @param deneWordOperationPointer - the pointer where the operation information is located
	 */
	public boolean evaluateDeneWordOperation(String deneWordOperationPointer){
		
		boolean toReturn=false;
		
		try {
			JSONObject evaluationDeneJSONObject = getDeneByIdentity( new Identity(deneWordOperationPointer));
			if(evaluationDeneJSONObject.has(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE)) {
				String deneType = evaluationDeneJSONObject.getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE);
				if(deneType.equals(TeleonomeConstants.DENE_TYPE_DENEWORD_OPERATION_EXPRESSION_EVALUATION)) {
					return evaluateExpressionDeneWordOperation( deneWordOperationPointer);
				}else if(deneType.equals(TeleonomeConstants.DENE_TYPE_DENEWORD_OPERATION_EXPRESSION_SWITCH)) {
					return evaluateSwitchDeneWordOperation(deneWordOperationPointer);
				}else if(deneType.equals("Action")) {
					//
					// this is a hack until i update all my existing denomes
					return evaluateExpressionDeneWordOperation( deneWordOperationPointer);
				}
			}
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		
		
		return toReturn;
	}
	
	
	public boolean evaluateSwitchDeneWordOperation(String deneWordOperationPointer){
		boolean toReturn=false;
		
		//finish this
		
		return toReturn;
	}
	
	
	
	/**
	 * 
	 *  this method evaluates a deneword operation, this means that the actuator commmand code
	 *  of this action is not sent to the microcontroller but instead an operation is performed
	 *  
	 * @param deneWordOperationPointer - the pointer where the operation information is located
	 */
	public boolean evaluateExpressionDeneWordOperation(String deneWordOperationPointer){
		boolean toReturn=false;
		try {
			logger.info("Evaluate Deneword operation,deneWordOperationPointer=" + deneWordOperationPointer);

			JSONObject evaluationParametersJSONObject = getDeneByIdentity( new Identity(deneWordOperationPointer));
			//***************
			JexlContext jexlActionContext = null;

			String variableName="", variableValueType;
			Object variableValue;
			String actionName="", destinationPointer="", actionExpressionString="";

			logger.info("line 5502 evaluationParametersJSONObject=" + evaluationParametersJSONObject);
			JSONObject actuatorLogicProcessingDeneChain = null;

			JSONArray actuatorLogicProcessingDenes=null;
			try {
				actuatorLogicProcessingDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);
				//logger.info("actuatorLogicProcessingDeneChain=" + actuatorLogicProcessingDeneChain);
				actuatorLogicProcessingDenes = actuatorLogicProcessingDeneChain.getJSONArray("Denes");

			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			try {
				boolean active = (Boolean)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(evaluationParametersJSONObject,"Active",TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(!active)return toReturn;
				actionName = evaluationParametersJSONObject.getString("Name");
				destinationPointer =  (String) getDeneWordAttributeByDeneWordTypeFromDene(evaluationParametersJSONObject,TeleonomeConstants.DENEWORD_TYPE_OPERATION_DESTINATION,TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				actionExpressionString = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(evaluationParametersJSONObject,"Expression",TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));
			}
			
			
			//
			// Create the condition Name
			//
			//actuatorLogicProcessingDeneName = codonName + " " +actionName + " " +conditionName + " Processing";
			String codonName = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(evaluationParametersJSONObject, "Codon", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			// create the processing dene for the action
			JSONObject actuatorActionEvaluationLogicProcessingDene = new JSONObject();
			actuatorLogicProcessingDenes.put(actuatorActionEvaluationLogicProcessingDene);
			String actuatorActionEvaluationLogicProcessingDeneName = codonName + " " +actionName + " "  + "Processing";

			actuatorActionEvaluationLogicProcessingDene.put("Name", actuatorActionEvaluationLogicProcessingDeneName);
			actuatorActionEvaluationLogicProcessingDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_ACTUATOR_DENE_OPERATION_EVALUATION_PROCESSING);

			JSONArray actuatorActionEvaluationLogicProcessingDeneDeneWords = new JSONArray();
			actuatorActionEvaluationLogicProcessingDene.put("DeneWords", actuatorActionEvaluationLogicProcessingDeneDeneWords);

			JSONObject actuatorActionEvaluationLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject("Action Name", actionName,null,"String",true);
			actuatorActionEvaluationLogicProcessingDeneDeneWords.put(actuatorActionEvaluationLogicProcessingDeneDeneWord);
			
			actuatorActionEvaluationLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_ACTION_EXPRESSION, actionExpressionString,null,"String",true);
			actuatorActionEvaluationLogicProcessingDeneDeneWords.put(actuatorActionEvaluationLogicProcessingDeneDeneWord);

			actuatorActionEvaluationLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject("Actuator Name", codonName,null,"String",true);
			actuatorActionEvaluationLogicProcessingDeneDeneWords.put(actuatorActionEvaluationLogicProcessingDeneDeneWord);

			
			
			
			
			JSONArray actuatorActionVariablesJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(evaluationParametersJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_OPERATION_VARIABLE, TeleonomeConstants.COMPLETE);
			logger.info("Evaluate Deneword operation,actionExpressionString=" + actionExpressionString + " there are " + actuatorActionVariablesJSONArray.length() + " variables");
			//
			// only do evaluation if there are variables, this is because there
			try {
				//
				// create the actionExpression and condition
				Expression actionExpression = jexl.createExpression(actionExpressionString);
				jexlActionContext = new MapContext();
				//
				// now evaluate every one of the variables 
				// by creating an expression for each condition
				// this conditionexpressin will then be fed to 
				// the action expression for final evaluation
				//
				JSONObject  actionVariableValueJSONObject, resolvedVariablePointerJSONObject;
				boolean allVariablesInExpressionRenderedSuccesfully=true;
				Object variableRawValue;
				String variableIdentityPointer, resolvedVariablePointerValueType="";
				for(int j=0;j<actuatorActionVariablesJSONArray.length();j++){
					actionVariableValueJSONObject =  actuatorActionVariablesJSONArray.getJSONObject(j);
					variableName = actionVariableValueJSONObject.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
					variableValueType = actionVariableValueJSONObject.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
					variableRawValue= actionVariableValueJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
 
					variableValue=null;

					logger.info("actionVariableValueJSONObject=" + actionVariableValueJSONObject.toString(4));
					logger.info("Evaluate Deneword operation,variableName=" + variableName);
					logger.info("Evaluate Deneword operation,variableRawValue=" + variableRawValue);
					logger.info("Evaluate Deneword operation,variableValueType=" + variableValueType);

					if(variableValueType.equals(TeleonomeConstants.DENEWORD_TYPE_POINTER)){
						variableIdentityPointer = actionVariableValueJSONObject.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

						resolvedVariablePointerJSONObject = (JSONObject) getDeneWordAttributeByIdentity(new Identity(variableIdentityPointer), TeleonomeConstants.COMPLETE);
						if(resolvedVariablePointerJSONObject!=null){
							resolvedVariablePointerValueType = resolvedVariablePointerJSONObject.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);

							if(resolvedVariablePointerValueType.equals("double")){
								variableValue =new Double( resolvedVariablePointerJSONObject.getDouble(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
							}else if(resolvedVariablePointerValueType.equals("int")){
								variableValue = new Integer( resolvedVariablePointerJSONObject.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
							}else if(resolvedVariablePointerValueType.equals("long")){
								variableValue = new Long( resolvedVariablePointerJSONObject.getLong(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
							}else if(resolvedVariablePointerValueType.equals("String")){
								variableValue = resolvedVariablePointerJSONObject.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							}

						}else{
							allVariablesInExpressionRenderedSuccesfully=false;
						}
					}else if(variableValueType.equals("String") && variableRawValue.toString().startsWith("$")){
						String commandData = actionVariableValueJSONObject.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.info("rendering command commandData= " + commandData);
						//
						// this is a command
						if(commandData.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
							//	logger.info("rendering command of COMMANDS_CURRENT_TIMESTAMP_MILLIS");
							variableValue = System.currentTimeMillis();
						}else if(commandData.equals(TeleonomeConstants.COMMANDS_CURRENT_MINUTE_IN_THE_HOUR)){
							Calendar rightNow = Calendar.getInstance();
							variableValue = rightNow.get(Calendar.MINUTE);
							logger.info("rendering command of COMMANDS_CURRENT_MINUTE_IN_THE_HOUR= " + variableValue);

						}else if(commandData.equals(TeleonomeConstants.COMMANDS_CURRENT_HOUR_IN_DAY)){
							Calendar rightNow = Calendar.getInstance();
							variableValue = rightNow.get(Calendar.HOUR_OF_DAY);
							logger.info("rendering command of COMMANDS_CURRENT_HOUR_IN_DAY= " + variableValue);

						}else if(commandData.equals(TeleonomeConstants.COMMANDS_CURRENT_DAY_IN_WEEK)){
							Calendar rightNow = Calendar.getInstance();
							variableValue = rightNow.get(Calendar.DAY_OF_WEEK);
							logger.info("rendering command of COMMANDS_CURRENT_DAY_IN_WEEK= " + variableValue);

						}else if(commandData.equals(TeleonomeConstants.COMMANDS_CURRENT_DAY_IN_MONTH)){
							Calendar rightNow = Calendar.getInstance();
							variableValue = rightNow.get(Calendar.DAY_OF_MONTH);
							logger.info("rendering command of COMMANDS_CURRENT_DAY_IN_MONTH= " + variableValue);

						}else if(commandData.equals(TeleonomeConstants.COMMANDS_PREVIOUS_PULSE_MILLIS)){
							logger.info("rendering COMMANDS_PREVIOUS_PULSE_MILLIS, previousPulseJSONObject= " + previousPulseJSONObject);
							if(previousPulseJSONObject!=null){
								long previousPulseTimestamp = previousPulseJSONObject.getLong("Pulse Timestamp in Milliseconds");
								variableValue = previousPulseTimestamp;

							}else{
								allVariablesInExpressionRenderedSuccesfully=false;
							}
						}else{
							allVariablesInExpressionRenderedSuccesfully=false;
						}
					}else{

						if(variableValueType.equals("double")){
							variableValue =new Double( actionVariableValueJSONObject.getDouble(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						}else if(variableValueType.equals("int")){
							variableValue = new Integer( actionVariableValueJSONObject.getInt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						}else if(variableValueType.equals("long")){
							variableValue = new Long( actionVariableValueJSONObject.getLong(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						}else if(variableValueType.equals("String")){
							variableValue = actionVariableValueJSONObject.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						}
					}
					logger.info("line 4585 Evaluate Deneword operation,after rendering abpout to set variableName=" + variableName + " variableValue=" + variableValue);
			
					actuatorActionEvaluationLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject(variableName, variableValue,null,variableValueType.toString(),true);
					actuatorActionEvaluationLogicProcessingDeneDeneWord.put(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_EVALUATED_VARIABLE);
					actuatorActionEvaluationLogicProcessingDeneDeneWords.put(actuatorActionEvaluationLogicProcessingDeneDeneWord);
			
					if(variableValue!=null){
						
						
						jexlActionContext.set(variableName,variableValue);	
					}else{
						allVariablesInExpressionRenderedSuccesfully=false;
					}
				}
				logger.info("line 4583 allVariablesInExpressionRenderedSuccesfully=" + allVariablesInExpressionRenderedSuccesfully);

				
				
				if(allVariablesInExpressionRenderedSuccesfully){
					Object result = actionExpression.evaluate(jexlActionContext);	
					JSONObject destinationJSONObject = this.getDeneWordByIdentity(new Identity(destinationPointer));
					logger.info("line 4597 Evaluate Deneword operation,after rendering abpout to set destinationPointer=" + destinationPointer + " result=" + result);
					if(result instanceof Double) {
						result = Math.ceil((double)result);
					}
					destinationJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, result); 
					
					
					actuatorActionEvaluationLogicProcessingDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_ACTION_PROCESSING_RESULT, result,null,"double",true);
					actuatorActionEvaluationLogicProcessingDeneDeneWords.put(actuatorActionEvaluationLogicProcessingDeneDeneWord);

					
					toReturn=true;
				}
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e1));
			}


			//****************
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		logger.debug("Evaluate Deneword operation,returning " + toReturn );

		return toReturn;
	}

	public AbstractMap.SimpleEntry<String, JSONArray>  evaluateAction(String teleonomeName, JSONObject actuatorActionJSONObject) throws JSONException{
		String commandToExecute="";
		//logger.debug("evaluate action actuatorActionJSONObject=" + actuatorActionJSONObject);
		//logger.debug("processing " + actuatorActionJSONObject.toString(4));
		String actuatorActionConditionVariable_Name, actuatorActionConditionPointer;
		Object actuatorActionConditionVariable_Value = null;
		Object actuatorActionConditionVariable_Value_Rendered=null, actuatorActionConditionVariable_Type;
		boolean onLackOfDataForCondition=false;
		boolean allVariableInConditionRenderedSuccesfully=true;
		boolean conditionEval=false;

		JSONObject actuatorActionConditionVariableJSONObject;
		JSONArray actuatorActionConditionPointersJSONArray;
		JSONObject actuatorActionConditionJSONObject;
		Expression actionExpression = null;
		Expression conditionExpression = null;
		JSONArray actuatorActionConditionVariablesJSONArray;
		HashMap mapDeneWordsToPointers;

		boolean variableIsExternalData=false;
		boolean externalDataVariableOk=false;

		JexlContext jexlActionContext = null;
		JexlContext jexlConditionContext = null;

		String conditionName="", actuatorActionConditionVariable_Expression;

		String actionName="", actuatorLogicProcessingDeneName;
		JSONObject actuatorLogicProcessingDene, actuatorLogicProcessingCodonDeneDeneWord;
		JSONArray actuatorLogicProcessingDeneDeneWords;

		JSONArray actuatorActionConditionVariablesPointersJSONArray;
		String actionExpressionString="";
		JSONObject actuatorLogicProcessingDeneChain = null;

		JSONArray actuatorLogicProcessingDenes=null;
		try {
			actuatorLogicProcessingDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING);
			//logger.debug("actuatorLogicProcessingDeneChain=" + actuatorLogicProcessingDeneChain);
			actuatorLogicProcessingDenes = actuatorLogicProcessingDeneChain.getJSONArray("Denes");

		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try {
			boolean active = (Boolean)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,"Active",TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if(!active)return null;
			actionName = actuatorActionJSONObject.getString("Name");

			actionExpressionString = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,"Expression",TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		AbstractMap.SimpleEntry<String, JSONArray> toReturn=null;
		try {
			//
			// Create the condition Name
			//
			//actuatorLogicProcessingDeneName = codonName + " " +actionName + " " +conditionName + " Processing";
			String codonName = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, "Codon", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			// create the processing dene for the action
			JSONObject actuatorActionLogicProcessingDene = new JSONObject();
			actuatorLogicProcessingDenes.put(actuatorActionLogicProcessingDene);
			String actuatorActionLogicProcessingDeneName = codonName + " " +actionName + " "  + "Processing";

			actuatorActionLogicProcessingDene.put("Name", actuatorActionLogicProcessingDeneName);
			actuatorActionLogicProcessingDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_ACTUATOR_ACTION_PROCESSING);

			JSONArray actuatorActionLogicProcessingDeneDeneWords = new JSONArray();
			actuatorActionLogicProcessingDene.put("DeneWords", actuatorActionLogicProcessingDeneDeneWords);





			//
			// create the actionExpression and condition
			logger.debug ("actionExpressionString=" + actionExpressionString );
			actionExpression = jexl.createExpression(actionExpressionString);
			jexlActionContext = new MapContext();
			//
			// now evaluate every one of the conditions 
			// by creating an expression for each condition
			// this conditionexpressin will then be fed to 
			// the action expression for final evaluation
			//
			actuatorActionConditionPointersJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONArray actuatorActionConditionNamesJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
			logger.debug("actuatorActionConditionPointersJSONArray=" + actuatorActionConditionPointersJSONArray);
			for(int j=0;j<actuatorActionConditionPointersJSONArray.length();j++){
				actuatorActionConditionPointer = (String) actuatorActionConditionPointersJSONArray.getString(j);
				conditionName = actuatorActionConditionNamesJSONArray.getString(j);
				logger.debug("conditionName=" + conditionName);
				actuatorActionConditionJSONObject=null;
				//
				// actuatorActionConditionJSONObject is a deneword get the value which is a denepointer and render it
				try {
					actuatorActionConditionJSONObject = getDeneByIdentity(new Identity(actuatorActionConditionPointer));
				} catch (InvalidDenomeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//
				// the condition is 
				//String condition = "#Purpose:Purpose1:Operations:Bore Pump Status#==OFF";

				//conditionName = actuatorActionConditionJSONObject.getString("Name");


				actuatorLogicProcessingDeneName = codonName + " " +actionName + " " +conditionName + " Processing";

				actuatorActionConditionVariable_Expression = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionConditionJSONObject, "Expression", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


				actuatorLogicProcessingDene = new JSONObject();
				actuatorLogicProcessingDenes.put(actuatorLogicProcessingDene);
				actuatorLogicProcessingDene.put("Name", actuatorLogicProcessingDeneName);
				actuatorLogicProcessingDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_ACTUATOR_CONDITION_PROCESSING);




				actuatorLogicProcessingDeneDeneWords = new JSONArray();
				actuatorLogicProcessingDene.put("DeneWords", actuatorLogicProcessingDeneDeneWords);
				actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", codonName,null,"String",true);
				actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
				actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_CONDITION_EXPRESSION, actuatorActionConditionVariable_Expression,null,"String",true);
				actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);

				//String sourcePointer = "@" + teleonomeName +":" + TeleonomeConstants.NUCLEI_INTERNAL +":" + TeleonomeConstants.DENECHAIN_ACTUATORS +":" +codonName + " " +actionName + " " +conditionName;
				actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Source", actuatorActionConditionPointer,null,"DenePointer",true);
				actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);

				onLackOfDataForCondition = (boolean) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionConditionJSONObject, "On Lack Of Data", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);



				actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.CONDITION_NAME, conditionName,null,"String",true);
				actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);



				actuatorActionConditionVariablesPointersJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actuatorActionConditionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE,  TeleonomeConstants.DENEWORD_TYPE_CONDITION_VARIABLE_POINTER,TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				actuatorActionConditionVariablesJSONArray = renderDeneWordsFromPointers(actuatorActionConditionVariablesPointersJSONArray);

				mapDeneWordsToPointers = mapDeneWordsToPointers(actuatorActionConditionVariablesPointersJSONArray);
				logger.debug("mapDeneWordsToPointers=" + mapDeneWordsToPointers);

				//actuatorActionConditionVariablesJSONArray  = actuatorActionConditionJSONObject.getJSONArray("Variables");
				logger.debug("actuatorActionConditionVariable_Expression=" + actuatorActionConditionVariable_Expression);



				//
				// check to see if the condition expresion begins with a @
				// if so, this is a pointer that will point to a dene that will
				// contain a Non Algebraic Condition

				if(actuatorActionConditionVariable_Expression.startsWith("@")){
					//
					// pass the pointer to the function that will evaluate the condition
					// this will be of the type "mnemosyne_today has a dene with name=xxx					
					conditionEval = evaluateConditionPointer(actuatorActionConditionVariable_Expression);
					jexlActionContext.set(conditionName,conditionEval);

					actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Result", new Boolean(conditionEval).toString(),null,"boolean",true);
					actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
					logger.debug("conditionName=" + conditionName+ " pointing to " + actuatorActionConditionVariable_Expression + " conditionEval=" + conditionEval);


				}else{
					conditionExpression = jexl.createExpression(actuatorActionConditionVariable_Expression);
					jexlConditionContext = new MapContext();
					//
					// All variables in a condition must be rendered for the condition to be evaluate it.
					//set this flag to true, if any of the data is not there, then it will set to false
					//
					allVariableInConditionRenderedSuccesfully=true;
					//logger.debug("actuatorActionConditionVariablesJSONArray=" + actuatorActionConditionVariablesJSONArray);
					Iterator it = mapDeneWordsToPointers.entrySet().iterator();
					String deneWordPointer;
					while (it.hasNext()) {
						Map.Entry pair = (Map.Entry)it.next();
						deneWordPointer = (String) pair.getKey();
						actuatorActionConditionVariableJSONObject= (JSONObject) pair.getValue();
						it.remove(); // avoids a ConcurrentModificationException

						actuatorActionConditionVariable_Name = actuatorActionConditionVariableJSONObject.getString("Name");
						actuatorActionConditionVariable_Type = actuatorActionConditionVariableJSONObject.getString("Value Type");

						
						//
						// now check to see if this data came from the external data and if so, whether is stale or not
						//
						if(deneWordPointer.contains(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA)) {
							//
							// the pointer will be something like @Sento:Purpose:External Data:Tlaloc:SolarRadiation
							// now check the ExternalDataStatus deneword of the same dene where the pointer points to
							// it is danger, it means its stall and therefore set allVariableInConditionRenderedSuccesfully=false; 
							Identity deneWordIdentity = new Identity(deneWordPointer);
							Identity denewordStatusIdentity = new Identity(deneWordIdentity.getTeleonomeName(),deneWordIdentity.getNucleusName(), deneWordIdentity.getDenechainName(),deneWordIdentity.getDeneName(), TeleonomeConstants.EXTERNAL_DATA_STATUS);
							logger.debug("the dene for external denewordStatusIdentity is " + denewordStatusIdentity.toString());
							//
							// now get the value
							String externalDeneStatus=TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE;
							try {
								externalDeneStatus = (String) this.getDeneWordAttributeByIdentity(denewordStatusIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								logger.debug("externalDeneStatus after getting data by pointer " + externalDeneStatus);

							} catch (InvalidDenomeException e) {
								// TODO Auto-generated catch block
								logger.debug(Utils.getStringException(e));
							}
							if(externalDeneStatus.equals(TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE)) {
								allVariableInConditionRenderedSuccesfully=false;
								
								actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(actuatorActionConditionVariable_Name, TeleonomeConstants.STATUS_MESSAGE_EXTERNAL_DATA_STALE,null,actuatorActionConditionVariable_Type.toString(),true);
								actuatorLogicProcessingCodonDeneDeneWord.put(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_EVALUATED_VARIABLE);
								actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
								
								
							}
						}

						logger.debug("after externalDeneStatus allVariableInConditionRenderedSuccesfully " + allVariableInConditionRenderedSuccesfully);

						if(allVariableInConditionRenderedSuccesfully) {


							logger.debug("actuatorActionConditionVariableJSONObject=" + actuatorActionConditionVariableJSONObject.toString(4));
							
							if(actuatorActionConditionVariable_Type.equals("double")){
								actuatorActionConditionVariable_Value =new Double( actuatorActionConditionVariableJSONObject.getDouble("Value"));

							}else if(actuatorActionConditionVariable_Type.equals("int")){
								actuatorActionConditionVariable_Value = ""+ actuatorActionConditionVariableJSONObject.getInt("Value");
							}else if(actuatorActionConditionVariable_Type.equals("long")){
								actuatorActionConditionVariable_Value = ""+ actuatorActionConditionVariableJSONObject.getLong("Value");
							}else if(actuatorActionConditionVariable_Type.equals("boolean")){
								actuatorActionConditionVariable_Value =""+ actuatorActionConditionVariableJSONObject.getBoolean("Value");
							}else if(actuatorActionConditionVariable_Type.equals(TeleonomeConstants.DENEWORD_TYPE_POINTER) ||
									actuatorActionConditionVariable_Type.equals("Timestamp") ||
									actuatorActionConditionVariable_Type.equals("String")){
								actuatorActionConditionVariable_Value =  actuatorActionConditionVariableJSONObject.getString("Value");
							}


							logger.debug("actuatorActionConditionVariable_Name=" + actuatorActionConditionVariable_Name);
							logger.debug("actuatorActionConditionVariable_Value=" + actuatorActionConditionVariable_Value);
							logger.debug("actuatorActionConditionVariable_Type=" + actuatorActionConditionVariable_Type);

							if(actuatorActionConditionVariable_Type.equals(TeleonomeConstants.DENEWORD_TYPE_POINTER) ){
								//
								// this is a pointer to another value in the denome, so get it
								try {
									actuatorActionConditionVariable_Value_Rendered = getCurrentPulseValueForDeneWord(actuatorActionConditionVariable_Value.toString());
									logger.debug("line 5772 of denomemanager, actuatorActionConditionVariable_Value_Rendered=" + actuatorActionConditionVariable_Value_Rendered + " actuatorActionConditionVariable_Value=" + actuatorActionConditionVariable_Value);
									//
									//
									// if we are referencing external data and the data has not arrived yet
									// the value will be "Undefined"
									// Also, check if the external data is stale,
									variableIsExternalData=false;
									externalDataVariableOk=false;

									variableIsExternalData=isPointerExternalData(actuatorActionConditionVariable_Value_Rendered.toString());
									if(variableIsExternalData) {
										externalDataVariableOk=isExternalDataOk(actuatorActionConditionVariable_Value_Rendered.toString());
										if(!externalDataVariableOk)allVariableInConditionRenderedSuccesfully=false;
									}
									logger.debug("line 5785 of denomemanager, variableIsExternalData=" + variableIsExternalData + " externalDataVariableOk=" + externalDataVariableOk);


									if(actuatorActionConditionVariable_Value_Rendered.equals(TeleonomeConstants.VALUE_UNDEFINED)){
										//
										// check to see if there is a default, will throw an exception if its not there
										//
										try{
											actuatorActionConditionVariable_Value_Rendered = actuatorActionConditionVariableJSONObject.getString("Default");
											if(actuatorActionConditionVariable_Value_Rendered.equals(TeleonomeConstants.VALUE_UNDEFINED)){
												allVariableInConditionRenderedSuccesfully=false;
											}
										}catch(JSONException e){
											allVariableInConditionRenderedSuccesfully=false;
										}

									}
								} catch (InvalidDenomeException e) {
									// TODO Auto-generated catch block
									logger.warn(Utils.getStringException(e));
								}
							}else if(actuatorActionConditionVariable_Type.equals("String") && actuatorActionConditionVariable_Value.toString().startsWith("$")){
								logger.debug("line 3406 of denomemanager, actuatorActionConditionVariable_Value_Rendered=" + actuatorActionConditionVariable_Value_Rendered + " actuatorActionConditionVariable_Value=" + actuatorActionConditionVariable_Value);

								//
								// this is a command
								if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
									//	logger.debug("rendering command of COMMANDS_CURRENT_TIMESTAMP_MILLIS");
									actuatorActionConditionVariable_Value_Rendered = System.currentTimeMillis();
								}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_MINUTE_IN_THE_HOUR)){
									Calendar rightNow = Calendar.getInstance();
									actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.MINUTE);
									logger.debug("rendering command of COMMANDS_CURRENT_MINUTE_IN_THE_HOUR= " + actuatorActionConditionVariable_Value_Rendered);

								}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_HOUR_IN_DAY)){
									Calendar rightNow = Calendar.getInstance();
									actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.HOUR_OF_DAY);
									logger.debug("rendering command of COMMANDS_CURRENT_HOUR_IN_DAY= " + actuatorActionConditionVariable_Value_Rendered);

								}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_DAY_IN_WEEK)){
									Calendar rightNow = Calendar.getInstance();
									actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.DAY_OF_WEEK);
									logger.debug("rendering command of COMMANDS_CURRENT_DAY_IN_WEEK= " + actuatorActionConditionVariable_Value_Rendered);

								}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_DAY_IN_MONTH)){
									Calendar rightNow = Calendar.getInstance();
									actuatorActionConditionVariable_Value_Rendered = rightNow.get(Calendar.DAY_OF_MONTH);
									logger.debug("rendering command of COMMANDS_CURRENT_DAY_IN_MONTH= " + actuatorActionConditionVariable_Value_Rendered);

								}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP)){
									actuatorActionConditionVariable_Value_Rendered = dateTimeFormat.format(new Timestamp(System.currentTimeMillis()));
								}else if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.COMMANDS_PREVIOUS_PULSE_TIMESTAMP)){

									if(previousPulseJSONObject!=null){
										long previousPulseTimestamp = previousPulseJSONObject.getLong("Pulse Timestamp in Milliseconds");
										actuatorActionConditionVariable_Value_Rendered = previousPulseTimestamp;

									}else{
										allVariableInConditionRenderedSuccesfully=false;
									}
								}
							}else{


								if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.VALUE_UNDEFINED)){
									//
									// check to see if there is a default, will throw an exception if its not there
									//
									try{
										actuatorActionConditionVariable_Value = actuatorActionConditionVariableJSONObject.getString("Default");
										if(actuatorActionConditionVariable_Value.equals(TeleonomeConstants.VALUE_UNDEFINED)){
											allVariableInConditionRenderedSuccesfully=false;
										}
									}catch(JSONException e){
										allVariableInConditionRenderedSuccesfully=false;
									}

								}
								actuatorActionConditionVariable_Value_Rendered=actuatorActionConditionVariable_Value;



							}
							//	logger.debug("conditionName=" + conditionName);



							actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(actuatorActionConditionVariable_Name, actuatorActionConditionVariable_Value_Rendered.toString(),null,actuatorActionConditionVariable_Type.toString(),true);
							actuatorLogicProcessingCodonDeneDeneWord.put(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_EVALUATED_VARIABLE);



							actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);

							if(!actuatorActionConditionVariable_Value_Rendered.equals(TeleonomeConstants.VALUE_UNDEFINED)){
								logger.debug("line 3137 actuatorActionConditionVariable_Name=" + actuatorActionConditionVariable_Name + " actuatorActionConditionVariable_Value_Rendered=" + actuatorActionConditionVariable_Value_Rendered);

								jexlConditionContext.set(actuatorActionConditionVariable_Name,actuatorActionConditionVariable_Value_Rendered);
							}

						}
					}


					//
					// Now that we finished setting the variables of the condition, evaluate it
					// as long as allVariableInConditionRenderedSuccesfully=true
					//
					// and set this value in the actionMap
					logger.debug("line 4893 allVariableInConditionRenderedSuccesfully=" + allVariableInConditionRenderedSuccesfully);
					if(allVariableInConditionRenderedSuccesfully){
						conditionEval = ((Boolean)conditionExpression.evaluate(jexlConditionContext)).booleanValue();
						jexlActionContext.set(conditionName,conditionEval);

						actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_CONDITION_PROCESSING_RESULT, new Boolean(conditionEval),null,"boolean",true);
						actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
						logger.debug("conditionName=" + conditionName+ " conditionEval=" + conditionEval);

					}else{
						jexlActionContext.set(conditionName,onLackOfDataForCondition);
						actuatorLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_CONDITION_PROCESSING_RESULT, new Boolean(onLackOfDataForCondition),null,"boolean",true);
						actuatorLogicProcessingDeneDeneWords.put(actuatorLogicProcessingCodonDeneDeneWord);
					}
				}
			}
			//
			// now that we finished evaluating every condition
			// evaluate the expression for the action
			//

			JSONObject actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Action Name", actionName,null,"String",true);
			actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);
			actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_ACTION_EXPRESSION, actionExpressionString,null,"String",true);
			actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);

			actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Actuator Name", codonName,null,"String",true);
			actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);


			//
			// initialize the result to false because if there is data missing, then allVariableInConditionRenderedSuccesfully will be false
			// but we can still have a action success failure tasks
			boolean result =false;
			if(allVariableInConditionRenderedSuccesfully){
				result = ((Boolean)actionExpression.evaluate(jexlActionContext)).booleanValue();
				logger.debug("action expression evaluation "+ actionExpressionString + " result="+ result);
			}

			if(result){
				//
				// if we are here is because thee expression came back as true
				// so the command needs to be executed, so extract it and 
				// store it
				// 
				commandToExecute = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				String pointerToActionSuccessTasks = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTION_SUCCESS_TASK_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				String pointerToMnemosyneOperationIndex = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_COMMAND_TO_EXECUTE, commandToExecute,null,"String",true);
				actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);

				actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_ACTION_PROCESSING_RESULT, true,null,"boolean",true);
				actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);


				logger.debug("action expression evaluation returning command true which is "+ commandToExecute );
				JSONArray info = new JSONArray();
				if(pointerToActionSuccessTasks!=null) {
					JSONObject actionSuccessTasksJSONObject = new JSONObject();
					actionSuccessTasksJSONObject.put(TeleonomeConstants.ACTION_SUCCESS_TASK_LABEL, pointerToActionSuccessTasks);
					info.put(actionSuccessTasksJSONObject);
				}
				if(pointerToMnemosyneOperationIndex!=null) {
					JSONObject mnemosyneOperationIndexJSONObject = new JSONObject();
					mnemosyneOperationIndexJSONObject.put(TeleonomeConstants.MNEMOSYNE_OPERATION_INDEX_LABEL, pointerToMnemosyneOperationIndex);
					info.put(mnemosyneOperationIndexJSONObject);
				}
				toReturn= new AbstractMap.SimpleEntry<String, JSONArray>(commandToExecute,info);

			}else{
				//
				// check to see if there is a false expression actuator command code
				commandToExecute = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_FALSE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("action expression evaluation returning command false which is "+ commandToExecute  );
				actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_ACTION_PROCESSING_RESULT, false,null,"boolean",true);
				actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);

				if(commandToExecute!=null){
					String pointerToActionSuccessTasks = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTION_SUCCESS_TASK_FALSE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					String pointerToMnemosyneOperationIndex = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION_FALSE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

					actuatorActionLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_COMMAND_TO_EXECUTE, commandToExecute,null,"String",true);
					actuatorActionLogicProcessingDeneDeneWords.put(actuatorActionLogicProcessingCodonDeneDeneWord);

					logger.debug("action expression evaluation returning command false pointerToActionSuccessTasks "+ pointerToActionSuccessTasks  );

					JSONArray info = new JSONArray();

					if(pointerToActionSuccessTasks!=null) {
						JSONObject actionSuccessTasksJSONObject = new JSONObject();
						actionSuccessTasksJSONObject.put(TeleonomeConstants.ACTION_SUCCESS_TASK_LABEL, pointerToActionSuccessTasks);
						info.put(actionSuccessTasksJSONObject);
					}
					if(pointerToMnemosyneOperationIndex!=null) {
						JSONObject mnemosyneOperationIndexJSONObject = new JSONObject();
						mnemosyneOperationIndexJSONObject.put(TeleonomeConstants.MNEMOSYNE_OPERATION_INDEX_LABEL, pointerToMnemosyneOperationIndex);
						info.put(mnemosyneOperationIndexJSONObject);
					}

					toReturn= new AbstractMap.SimpleEntry<String, JSONArray>(commandToExecute,info);


				}
			}


		} catch (JSONException e1) {
			// TODO Auto-generated catch block

			e1.printStackTrace();
		}
		return toReturn;
	}

	private boolean isPointerExternalData(String pointer) {
		Identity identity = new Identity(pointer);
		return (identity.getNucleusName().equals(TeleonomeConstants.NUCLEI_PURPOSE) && identity.getDenechainName().equals(TeleonomeConstants.DENECHAIN_EXTERNAL_DATA));
	}

	private boolean isExternalDataOk(String pointer) {
		Identity pointerIdentity = new Identity(pointer);
		Identity externalDataStatusIdentity = new Identity(pointerIdentity.getTeleonomeName(), pointerIdentity.getNucleusName(), pointerIdentity.getDenechainName(),pointerIdentity.getDeneName(),TeleonomeConstants.EXTERNAL_DATA_STATUS);
		boolean toReturn=false;
		try {
			toReturn= getDeneWordAttributeByIdentity(externalDataStatusIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE).equals(TeleonomeConstants.EXTERNAL_DATA_STATUS_OK);
		} catch (InvalidDenomeException | JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		return toReturn;
	}


	public void addMnemosyconProcessingDene(String codonName, long processingStartMillis, long processingEndMillis, int numberOfPulses, String functioName, String publishedTeleonomeName, String estimatedTimeForFinishingCycleString, int numberOfPulsesLeftBeforeCompletingCycle,  long firstPulseInBatchMilliseconds, long lastPulseInBatchMilliseconds, int updatedNumberOfPulsesToRetrieveDuringThisPulse, int neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse ) {
		JSONArray mnemosyconProcessingDenes=null;
		JSONObject mnemosyconProcessingDeneChain=null;
		try {
			mnemosyconProcessingDeneChain = getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_MNEMOSYCON_PROCESSING);
			logger.debug("mnemosyconProcessingDeneChain=" + mnemosyconProcessingDeneChain);
			mnemosyconProcessingDenes = mnemosyconProcessingDeneChain.getJSONArray("Denes");

			// create the processing dene for the action
			JSONObject mnemosyconProcessingDene = new JSONObject();
			mnemosyconProcessingDenes.put(mnemosyconProcessingDene);
			String mnemosyconProcessingDeneName = codonName  + " "  + "Processing";

			mnemosyconProcessingDene.put("Name", mnemosyconProcessingDeneName);
			mnemosyconProcessingDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_MNEMOSYCON_ACTION_PROCESSING);

			JSONArray mnemosyconProcessingDeneDeneWords = new JSONArray();
			mnemosyconProcessingDene.put("DeneWords", mnemosyconProcessingDeneDeneWords);
			//			JSONObject mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", codonName,null,"String",true);
			//			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			JSONObject mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_ORGANISM_TELEONOME_TO_PUBLISH, publishedTeleonomeName,null,"String",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_FUNCTION, functioName,null,"String",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Started At Millis", processingStartMillis,null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Started At Timestamp", simpleFormatter.format(processingStartMillis),null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Ended At Millis", processingEndMillis,null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);
			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Ended At Timestamp", simpleFormatter.format(processingEndMillis),null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Took Milliseconds", Utils.getElapsedTimeHoursMinutesSecondsString(processingEndMillis-processingStartMillis),null,"String",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);


			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution First Record Millis", firstPulseInBatchMilliseconds,null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution First Record Timestamp", simpleFormatter.format(firstPulseInBatchMilliseconds),null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Last Record Millis", lastPulseInBatchMilliseconds,null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Batch Execution Last Record Timestamp", simpleFormatter.format(lastPulseInBatchMilliseconds),null,"long",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Number Of Records Left Before Completing Cycle", numberOfPulsesLeftBeforeCompletingCycle,null,"int",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Estimated Time For Finishing Cycle String", estimatedTimeForFinishingCycleString,null,"String",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			if(updatedNumberOfPulsesToRetrieveDuringThisPulse>-1) {

				if(numberOfPulses!=updatedNumberOfPulsesToRetrieveDuringThisPulse) {
					mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Prior Number Of Pulses To Retrieve During This Pulse", numberOfPulses,null,"int",true);
					mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

					mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Update Number Of Pulses To Retrieve During This Pulse", updatedNumberOfPulsesToRetrieveDuringThisPulse,null,"int",true);
					mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);
				}
				mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH, updatedNumberOfPulsesToRetrieveDuringThisPulse,null,"int",true);
				mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

				if(neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse>-1 && neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse!=updatedNumberOfPulsesToRetrieveDuringThisPulse) {
					mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH_NEEDED_FOR_TIME_RESTRICTION, neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse,null,"int",true);
					mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

				}

			}else {
				mnemosyconProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH, numberOfPulses,null,"int",true);
				mnemosyconProcessingDeneDeneWords.put(mnemosyconProcessingCodonDeneDeneWord);

			}


		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}


	/**
	 * this method will evaluate a condition pointer.  this means that an actin condition instead of
	 * having an algebraic expression with variables, it has a pointer, which is passed to this function
	 * this pointer will point to a dene of type   "Condition Denomic Operation"
	 * This Dene will have the following DenWords:
	 * Data Source -  a pointer that points where to look for the denomic object in question.  this data can be a dene, a deneword, a denechain
	 * Operation = This deneword will dictate what we are doing,  Currently the valid operations check whether a denomic element exists or does not exists.
	 * as the need arise, i will add other operations
	 * it returns a boolean to mark whether the operation was succesfull or not
	 * 
	 * This function is used to find if a dene exists.  this can be usefull when doing schedules, like
	 * execute this action once per day.  in this example, the datasource will point to the dene in the 
	 * Mnemosyne Today denechain and will check if exists.  if there is, then we have
	 * run it today, if there is not, the we have not run it.  Since the Mnemsonye Today is cleared once per day
	 * by the method performTimePrunningAnalysis of the Mnemosyne Manager, it will be cleared once a day
	 * at midnight.  
	 * @param denePointer
	 * @return
	 */
	private boolean evaluateConditionPointer(String denePointer){
		boolean toReturn=false;

		try {
			JSONObject conditionDenomicOperation = getDeneByIdentity(new Identity(denePointer));
			if(conditionDenomicOperation!=null){
				String dataSourcePointer  = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(conditionDenomicOperation, TeleonomeConstants.CONDITION_DENOMIC_OPERATION_DATA_SOURCE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				JSONObject dataSource = this.getDeneChainByIdentity(new Identity(dataSourcePointer));

				String operation  = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(conditionDenomicOperation, TeleonomeConstants.CONDITION_DENOMIC_OPERATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

				if(operation.equals(TeleonomeConstants.DENOMIC_ELEMENT_EXISTS)){
					//
					// if we are here it means that we will return true if the datasourcepointer, which has to be a denechain
					// contains a dene by the name stored in operationTarget
					// 
					if(dataSource!=null)toReturn=true;

				}else if(operation.equals(TeleonomeConstants.DENOMIC_ELEMENT_DOES_NOT_EXISTS)){
					//
					// if we are here it means that we will return true if the datasourcepointer, 
					// points to a denomic element that does not exist
					if(dataSource==null)toReturn=true;
				}



			}
		} catch (InvalidDenomeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return toReturn;
	}
	// 
	// the input is the address of the deneword that we need the latest value
	// from the currentpulse.  The string will have a @ as the first and last character
	// and the rest will be separated by :
	// ie #Purpose:Purpose1:Operations:Bore Pump Status#
	//
	public Object getCurrentPulseValueForDeneWord(String value) throws InvalidDenomeException{
		String[] tokens = value.substring(1,value.length()).split(":");

		String teleonomeName = tokens[0];
		String nucleusName = tokens[1];
		String deneChainName = tokens[2];
		String deneName = tokens[3];
		String deneWordLabel = tokens[4];
		JSONObject aJSONObject, cpInternalNucleus=null,cpPurposeNucleus=null;

		JSONObject denomeObject;
		try {
			denomeObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					cpInternalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					cpPurposeNucleus= aJSONObject;
				}

			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}




		JSONArray deneChainsArray=null;
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = cpInternalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = cpPurposeNucleus.getJSONArray("DeneChains");
			}

			JSONObject  aDeneJSONObject, aDeneWordJSONObject;
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
								if(aDeneWordJSONObject.getString("Name").equals(deneWordLabel)){

									object = aDeneWordJSONObject.get("Value");
									;
									if(object.equals(TeleonomeConstants.VALUE_UNDEFINED)){
										//
										// if its undefined check to see if there is a default
										try{
											object = aDeneWordJSONObject.get("Default");
											return object;
										}catch(JSONException e){
											//
											// if there is no Default we end up in here
											//
											logger.debug(aDeneWordJSONObject.toString() + " does not have default and it has undefined");
										}
										return TeleonomeConstants.VALUE_UNDEFINED;
									}else return object;
									/*
									 valueType = aDeneWordJSONObject.getString("Value Type");

									if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)){
										Integer I = new Integer((int) object);
										return I;
									}else if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)){
										Double D = new Double((double) object);
										return D;
									}
									 */
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
		return TeleonomeConstants.VALUE_UNDEFINED;
	}

	/**
	 * returns an array list with the key been the pointer to the microcontroller and the value the order in which they must be processed
	 * @return
	 */
	public ArrayList<Map.Entry<String, Integer>> getMicroControllerPointerProcessingQueuePositionIndex(){
		return microControllerPointerProcessingQueuePositionIndex;
	}



	public ArrayList<Map.Entry<JSONObject, Integer>> getSensorsDeneWordsBySensorRequestQueuePositionForInitialByMicroControllerPointer(String pointer){
		return (ArrayList<Entry<JSONObject, Integer>>) pointerToMicroControllerSensorsDeneWordsForInitialBySensorRequestQueuePositionIndex.get(pointer);
	}

	public ArrayList<Map.Entry<JSONObject, Integer>> getActuatorExecutionPositionDeneForInitialByMicroControllerPointerIndex(String pointer){
		return (ArrayList<Entry<JSONObject, Integer>>) pointerToMicroControllerActuatorExecutionPositionForInitialDeneIndex.get(pointer);
	}






	public ArrayList<Map.Entry<JSONObject, Integer>> getSensorsDeneWordsBySensorRequestQueuePositionByMicroControllerPointer(String pointer){
		return (ArrayList<Entry<JSONObject, Integer>>) pointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex.get(pointer);
	}

	public ArrayList<Map.Entry<JSONObject, Integer>> getActuatorExecutionPositionDeneByMicroControllerPointerIndex(String pointer){
		return (ArrayList<Entry<JSONObject, Integer>>) pointerToMicroControllerActuatorExecutionPositionDeneIndex.get(pointer);
	}

	public ArrayList<Map.Entry<JSONObject, Integer>> getSensorsDeneWordsBySensorRequestQueuePosition(){
		return sensorRequestQueuePositionDeneWordIndex;
	}

	public ArrayList<Map.Entry<JSONObject, Integer>> getActuatorExecutionPositionDeneIndex(){
		return actuatorExecutionPositionDeneIndex;
	}

	public JSONArray getMicroControllerParamsForMicroController(String aMicroControllerName) {
		// TODO Auto-generated method stub
		if(microControllerNameMicroControllerParamsIndex.containsKey(aMicroControllerName)){
			return (JSONArray)microControllerNameMicroControllerParamsIndex.get(aMicroControllerName);
		}else{
			return new JSONArray();
		}

	}

	public void setCurrentPulseStatusMessage(String s){
		currentPulseStatusMessage=s;
	}

	public JSONObject generatePulse(){
		logger.debug("entering generate pulse");

		//	Timestamp now = new Timestamp(System.currentTimeMillis());
		//	SimpleDateFormat simpleFormatter = new SimpleDateFormat("E yyyy.MM.dd HH:mm:ss Z");
		//
		//	int  day = cal.get(Calendar.DATE);
		//	int  month = cal.get(Calendar.MONTH);
		//	int  year = cal.get(Calendar.YEAR);
		//
		//remove any expired denes from the denome
		//
		//	try {
		//currentlyCreatingPulseJSONObject.put("Pulse Timestamp", simpleFormatter.format(cal.getTime()));
		//currentlyCreatingPulseJSONObject.put("Pulse Timestamp in Milliseconds", cal.getTime().getTime());
		//
		// perform the eliminatin of the denes that expired:
		// assume that only actions have expiration dates
		//
		// loop over every denechain
		purgeExpiredDenes();
		//
		// generate the System Data Dene
		long pulseDuration = System.currentTimeMillis()-currentPulseStartTimestampMillis;
		//
		// generate the System Info dene at the end
		// because it calls gc so as to clear the memory
		// as close to the end as possible

		File directory = new File(".");
		long totalSpace = directory.getTotalSpace()/1024000;
		long freeSpace = directory.getFreeSpace()/1024000;
		long usableSpace = directory.getUsableSpace()/1024000;

		System.gc();
		double availableMemory = Runtime.getRuntime().freeMemory()/1024000;
		double maxMemory = Runtime.getRuntime().maxMemory()/1024000;
		double dbSize = aDBManager.getDatabaseSizeInMB();

		JSONObject operationalDataDeneChain;
		//
		// Add the denes for the purpose:operational data
		//
		// these are:
		//
		// 1)SystemData - contains information about disk space and memory usage
		// 2)Memory STatus
		// 3) Vital - contains status information
		// 4)Alert Messages - Any alert messages generated in this pulse
		try {
			operationalDataDeneChain = DenomeUtils.getDeneChainByName(currentlyCreatingPulseJSONObject, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA);
			JSONArray operationalDataDenes = operationalDataDeneChain.getJSONArray("Denes");




			//
			// Vital
			//


			JSONObject purposeVitalDene = DenomeUtils.getDeneFromDeneChainByDeneName(operationalDataDeneChain, TeleonomeConstants.DENE_VITAL);
			JSONArray purposeVitalDeneWords = purposeVitalDene.getJSONArray("DeneWords");
			int operationalModeRedValue=0;
			int operationalModeGreenValue=0;
			String bootstrapOperationalMode="";
			int operationalModeBlueValue=0;
			String teleonomeStatusMessage="";
			Vector area0Messages1 = new Vector();
			int currentLevel=0;
			String status="";

			String teleonomeStatus=  " " +TeleonomeConstants.TELEONOME_STATUS_ACTIVE + " " + statusMessagedateTimeFormat.format(new Date());
			logger.debug("gt purposevitaldene");
			//
			// here is where we decide what state to declare
			// compile the pathology denes
			// and also check for memory usage and disk space
			// usage
			//
			// check to see if the current denome has the dene
			//Internal:Descriptive:Operational Parameters Thresholds
			JSONObject operationalParametersThresholdDene = getDeneByIdentity(new Identity("@" + denomeName + ":" + TeleonomeConstants.NUCLEI_INTERNAL + ":" + TeleonomeConstants.DENECHAIN_DESCRIPTIVE +":" + TeleonomeConstants.DENE_OPERATIONAL_PARAMETERS_THRESHOLDS ));
			int maximumPulseGenerationDuration=0;
			if(operationalParametersThresholdDene!=null){
				int minimumAvailableMemoryMB = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(operationalParametersThresholdDene, "Minimum Available Memory", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(availableMemory<minimumAvailableMemoryMB){	
					//
					// we are out of range, so write a pathology dene
					//
					String pathologyCause = TeleonomeConstants.PATHOLOGY_AVAILABLE_MEMORY_BELOW_THRESHOLD;
					String pathologyName = TeleonomeConstants.PATHOLOGY_AVAILABLE_MEMORY_BELOW_THRESHOLD;
					String pathologyLocation = new Identity(teleonomeName  ).toString();
					Vector extraDeneWords = new Vector();
					JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Minimum Available Memory", ""+minimumAvailableMemoryMB ,null,"double",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Available Memory", ""+availableMemory ,null,"double",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					addPurposePathologyDene(pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);

				}

				int minimumAvailableDiskSpaceMB = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(operationalParametersThresholdDene, "Minimum Available Disk Space", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(freeSpace<minimumAvailableDiskSpaceMB){
					String pathologyCause = TeleonomeConstants.PATHOLOGY_DISK_SPACE_BELOW_THRESHOLD;
					String pathologyName = TeleonomeConstants.PATHOLOGY_DISK_SPACE_BELOW_THRESHOLD;
					String pathologyLocation = new Identity(teleonomeName  ).toString();
					Vector extraDeneWords = new Vector();
					JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Free Space", ""+freeSpace ,null,"double",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Minimum Available Disk Space MB", ""+minimumAvailableDiskSpaceMB ,null,"double",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					addPurposePathologyDene(pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);


				}

				maximumPulseGenerationDuration = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(operationalParametersThresholdDene, "Maximum Pulse Generation Duration", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.warn("Pulse generation duration above Maximum Pulse Generation Duration Threshold, pulseDuration=" +pulseDuration + " maximumPulseGenerationDuration=" + maximumPulseGenerationDuration);

				if(pulseDuration>maximumPulseGenerationDuration){
					logger.warn("Pulse generation duration above Maximum Pulse Generation Duration Threshold, pulseDuration=" +pulseDuration + " maximumPulseGenerationDuration=" + maximumPulseGenerationDuration);


					String pathologyCause = TeleonomeConstants.PATHOLOGY_PULSE_DURATION_ABOVE_THRESHOLD;
					String pathologyName = TeleonomeConstants.PATHOLOGY_PULSE_DURATION_ABOVE_THRESHOLD;
					String pathologyLocation = new Identity(teleonomeName  ).toString();
					Vector extraDeneWords = new Vector();
					JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Pulse Duration", ""+pulseDuration ,null,"double",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Maximum Pulse Generation Duration", ""+maximumPulseGenerationDuration ,null,"double",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					addPurposePathologyDene(pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);

				}

			}else{
				logger.debug("operationalParametersThresholdDene is null");
			}

			//
			// check to see if there are any Analyticons that depends on a pulse that is late
			//
			if(analyticonsDataSourcesLate.size()>0){
				String pathologyCause = TeleonomeConstants.PATHOLOGY_ANALYTICON_SOURCES_LATE;
				String pathologyName = TeleonomeConstants.PATHOLOGY_ANALYTICON_SOURCES_LATE;
				String pathologyLocation = new Identity(teleonomeName  ).toString();
				Vector extraDeneWords = new Vector();
				JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Pulse Duration", ""+pulseDuration ,null,"double",true);

				for(int i=0;i<analyticonsDataSourcesLate.size();i++){

					extraDeneWords.addElement(pathologyDeneDeneWord);
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject("Message " + (i+1), ""+analyticonsDataSourcesLate.elementAt(i) ,null,"String",true);
				}
			}

			if(currentPulseStatusMessage.equals(TeleonomeConstants.STATUS_MESSAGE_USE_CURRENT_PULSE_MILLIS)){
				teleonomeStatusMessage = "Pulse: "+pulseDuration;
			}else if(currentPulseStatusMessage.equals(TeleonomeConstants.STATUS_MESSAGE_USE_CURRENT_PULSE_SECONDS)){
				teleonomeStatusMessage = "Pulse: "+pulseDuration/1000;
			}else if(currentPulseStatusMessage.equals(TeleonomeConstants.STATUS_MESSAGE_USE_CURRENT_PULSE_MINUTES)){
				teleonomeStatusMessage = "Pulse: "+pulseDuration/60000;
			}else if(currentPulseStatusMessage.equals(TeleonomeConstants.STATUS_MESSAGE_USE_CURRENT_AND_AVAILABLE_PULSE_SECONDS)){
				teleonomeStatusMessage = "CP: "+ (pulseDuration/1000) + " AV:" + (maximumPulseGenerationDuration/1000 - pulseDuration/1000);
			}else if(currentPulseStatusMessage.equals(TeleonomeConstants.STATUS_MESSAGE_USE_CURRENT_AVAILABLE_PULSE_NUMBER_ANALYTICONS)){
				int numberAnanlyticons = aDenomeManager.getAnalyticonDenesJSONArray( ).length();
				teleonomeStatusMessage = "C:"+ (pulseDuration/1000) + " L:" + (maximumPulseGenerationDuration/1000 - pulseDuration/1000) + " A:" + numberAnanlyticons;
			}else{
				teleonomeStatusMessage=currentPulseStatusMessage;
			}


			if(currentLevel==0){
				operationalModeRedValue=0;
				operationalModeGreenValue=255;
				operationalModeBlueValue=0;
				status=TeleonomeConstants.TELEONOME_STATUS_ACTIVE;
				bootstrapOperationalMode=TeleonomeConstants.BOOTSTRAP_SUCCESS;

			}else if(currentLevel==1){
				operationalModeRedValue=255;
				operationalModeGreenValue=255;
				operationalModeBlueValue=0;
				status=TeleonomeConstants.TELEONOME_STATUS_CAUTION;
				bootstrapOperationalMode=TeleonomeConstants.BOOTSTRAP_WARNING;
			}else if(currentLevel>=2){
				operationalModeRedValue=255;
				operationalModeGreenValue=0;
				operationalModeBlueValue=0;
				status=TeleonomeConstants.TELEONOME_STATUS_ALERT;
				bootstrapOperationalMode=TeleonomeConstants.BOOTSTRAP_DANGER;
			}







			//
			// now check to see if there are any pathology denes, if there are set the 
			//
			Identity pathologyDeneChainIdentity = new Identity(denomeName, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_PATHOLOGY);
			JSONArray pathologyDenes = getDenesFromDeneChainByIdentity(pathologyDeneChainIdentity);

			if(pathologyDenes.length()>0){
				operationalModeRedValue=255;
				operationalModeGreenValue=0;
				operationalModeBlueValue=0;
				status=TeleonomeConstants.TELEONOME_STATUS_ALERT;
				bootstrapOperationalMode=TeleonomeConstants.BOOTSTRAP_DANGER;
			}

			JSONObject deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_TYPE_INITIAL_IDENTITY_MODE, TeleonomeConstants.COMPLETE);
			logger.debug("initialIdentityMode=" + initialIdentityMode + " deneWord= " + deneWord);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  initialIdentityMode);	

			//
			// for now put the initial as the current, will change later
			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_IDENTITY_MODE, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  initialIdentityMode);	


			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  getCurrentPulseFrequencyMilliseconds());	

			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_GENERATION_DURATION, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  pulseDuration);	

			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_STATUS, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  teleonomeStatus);	


			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.TELEONOME_STATUS_ALERT_MESSAGE, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  teleonomeStatusMessage);	


			//deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_OPERATIONAL_MODE, TeleonomeConstants.COMPLETE);
			//deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  TeleonomeConstants.TELEONOME_OPERATION_MODE_NORMAL);	


			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_OPERATIONAL_STATUS_RED_VALUE, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  operationalModeRedValue);	

			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_OPERATIONAL_STATUS_GREEN_VALUE, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  operationalModeGreenValue);	

			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_OPERATIONAL_STATUS_BLUE_VALUE, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  operationalModeBlueValue);	

			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_OPERATIONAL_STATUS_BLINK_VALUE, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  0);	

			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, TeleonomeConstants.DENEWORD_OPERATIONAL_STATUS_BOOTSTRAP_EQUIVALENT, TeleonomeConstants.COMPLETE);
			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  bootstrapOperationalMode);	



			//int previousNumberMessages =  (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, "Number of Alert Messages", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			//			deneWord = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(purposeVitalDene, "Number of Pathology Denes", TeleonomeConstants.COMPLETE);
			//			deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,  area0Messages.size());	






			//
			// End of Vital
			//

		} catch (InvalidDenomeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}




		//
		// now save it to the hard drive
		//
		try {
			//
			// Copy the currentpulse to be the previous pulse
			//
			currentlyCreatingPulseJSONObject.put("Pulse Creation Duration Millis", pulseDuration);

			File currentPulseFile = new File(selectedDenomeFileName);
			String previousPulseFileName = FilenameUtils.getBaseName(selectedDenomeFileName) + ".previous_pulse";

			File previousPulseFile = new File(previousPulseFileName);
			logger.debug("about to copy " + selectedDenomeFileName + " to " + previousPulseFileName);

			FileUtils.copyFile(currentPulseFile, previousPulseFile);
			//
			// now write the denome, do it twice, first so that you can get the pulse size
			// then modify the purpose:operational data:vital:Pulse Size Kb deneword
			//
			File  selectedDenomeFile = new File(selectedDenomeFileName);
			String finalPulseInStringForm = currentlyCreatingPulseJSONObject.toString(4);
			long pulseSize = selectedDenomeFile.length()/1024;
			String pointerToPulseSizeDW=this.denomeName + ":" + TeleonomeConstants.NUCLEI_PURPOSE + ":" + TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA + ":" + TeleonomeConstants.DENE_VITAL + ":" + TeleonomeConstants.DENEWORD_PULSE_SIZE_KB;
			try {
				updateDeneWordCurrentPulse( pointerToPulseSizeDW, pulseSize);
			} catch (InvalidDenomeException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

			FileUtils.write(selectedDenomeFile, currentlyCreatingPulseJSONObject.toString(4));

			logger.debug("Saved pulse to " + selectedDenomeFileName);
			logger.debug("saving pusle with a timestamps of " + currentlyCreatingPulseJSONObject.getString("Pulse Timestamp"));
			FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/Teleonome.denome"), currentlyCreatingPulseJSONObject.toString(4));
			FileUtils.write(new File(Utils.getLocalDirectory() + "tomcat/webapps/ROOT/" + teleonomeName + ".pulse"), currentlyCreatingPulseJSONObject.toString(4));
			System.gc();
			double afterGcMemory = Runtime.getRuntime().freeMemory()/1024000;

			logger.debug("available memory after generating pulse before gc=" + availableMemory + " after gc=" + afterGcMemory);


		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		/*
		try {
			JSONObject m = this.getDeneWordByIdentity(new Identity("@Tlaloc:Purpose:Operational Data:Power Point:BorePumpStatus"));
			logger.debug("In denome:"+m.toString(4));

			logger.debug("pulse:" + getCurrentPulseValueForDeneWord("@Tlaloc:Purpose:Operational Data:Flow:BorePumpStatus"));
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		 */
		return currentlyCreatingPulseJSONObject;
		//} catch (JSONException e) {
		//	// TODO Auto-generated catch block
		//	logger.warn(Utils.getStringException(e));
		//}
		//return null;

	}

	public String getcurrentlyCreatingPulseTimestamp() {
		return (String)currentlyCreatingPulseJSONObject.getString("Pulse Timestamp");
	}
	public long getcurrentlyCreatingPulseTimestampMillis() {
		return currentlyCreatingPulseJSONObject.getLong("Pulse Timestamp in Milliseconds");
	}	

	public int getCurrentPulseFrequencyMilliseconds() throws InvalidDenomeException{

		Integer I = (Integer)aDenomeManager.getDeneWordValueByName(TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY);
		if(I!=null)return I.intValue();
		else return 60000;
	}

	private void purgeExpiredDenes(){

		try {

			//
			// now parse them
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			JSONObject aJSONObject, aDeneChainJSONObject,aDeneJSONObject,aDeneWordJSONObject;
			String name;
			JSONArray denesJSONArray, deneWordsJSONArray;
			JSONArray deneChainsArray=null;
			boolean removedDene;
			for(int n=0;n<nucleiArray.length();n++){
				aJSONObject = (JSONObject) nucleiArray.get(n);
				deneChainsArray = internalNucleus.getJSONArray("DeneChains");
				for(int i=0;i<deneChainsArray.length();i++){
					aDeneChainJSONObject = (JSONObject) deneChainsArray.get(i);
					denesJSONArray = aDeneChainJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						removedDene=false;
						long expirationDate=-1;
						try{
							expirationDate = aDeneJSONObject.getLong("Expiration");

						}catch(JSONException e){
						}

						if(expirationDate>-1){
							if(System.currentTimeMillis()>expirationDate){
								//
								// this dene has expired, remove it
								//
								denesJSONArray.remove(j);
								removedDene=true;
							}
						}
						if(!removedDene){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								expirationDate=-1;
								try{
									expirationDate = aDeneWordJSONObject.getLong("Expiration");

								}catch(JSONException e){
								}

								if(expirationDate>-1){
									if(System.currentTimeMillis()>expirationDate){
										deneWordsJSONArray.remove(k);
									}
								}
							}
						}		
					}
				}
			}
		}catch(JSONException e){

		}
	}

	public void executeActionSuccessTasks( String pointerToActionSuccessTasks) {
		// TODO Auto-generated method stub
		//from anActuatorDeneJSONObject give me the DeneWord Type Action
		try {
			JSONObject actionSuccessTask = this.getDeneByIdentity(new Identity(pointerToActionSuccessTasks));
			logger.debug("line 5307pointerToActionSuccessTasks=" + pointerToActionSuccessTasks + " actionSuccessTask=" + actionSuccessTask);
			JSONArray actionSuccessTaskDeneWords = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(actionSuccessTask, TeleonomeConstants.DENEWORD_TYPE_UPDATE_DENEWORD_VALUE, TeleonomeConstants.COMPLETE);
			JSONObject toDoDeneWordJSONObject;
			String target, valueType;
			Object value;
			JSONObject targetDeneWord;
			Object renderedValue=null;
			for(int i=0;i<actionSuccessTaskDeneWords.length();i++){
				try {
					toDoDeneWordJSONObject = actionSuccessTaskDeneWords.getJSONObject(i);
					logger.debug("toDoDeneWordJSONObject=" + toDoDeneWordJSONObject);

					//
					//the target is what variable and the value is what to set it at
					valueType = toDoDeneWordJSONObject.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);


					value = toDoDeneWordJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					target = toDoDeneWordJSONObject.getString(TeleonomeConstants.DENEWORD_TARGET_ATTRIBUTE);
					//
					// check to see if the value is a command, like $Current_Time
					if(value.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP_MILLIS)){
						renderedValue = System.currentTimeMillis()/1000;
						logger.debug("rendering currentimemillis for an action=" + renderedValue);
					}else if(value.equals(TeleonomeConstants.COMMANDS_CURRENT_TIMESTAMP)){
						DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
						renderedValue = formatter.format(LocalDateTime.now());
						logger.debug("rendering currentimemillis for an action=" + renderedValue);
					}else if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER) && (value instanceof java.lang.String) &&  ((String)value).startsWith("@") ){
						//
						// if we are here then we have a situation where the value
						// contains a pointer to another dene, expressed as  @name of dene
						// ie :
						//"Name": "Communication Protocol",
						//"Value": "@Serial Parameters",
						//
						// now check to see if the valueType=DENEWORD_TIMESTRING_VALUE
						// because if its does, the value would be a long since it would have been 
						// converted to long when storing it
						JSONObject sourceDataJSONObject = getDeneWordByIdentity(new Identity((String)value));
						logger.debug("value=" + value + " sourceDataJSONObject=" + sourceDataJSONObject);
						if(valueType.equals(TeleonomeConstants.DENEWORD_TIMESTRING_VALUE)){
							renderedValue ="" +  (Long)sourceDataJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						}else{
							renderedValue = sourceDataJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						}



					}else{
						renderedValue=value;
					}

					//
					// this will set it in the denome
					//
					logger.debug("target=" + target + " valueType=" + valueType + " value=" + value + " renderedValue=" + renderedValue);
					targetDeneWord = getDeneWordByIdentity(new Identity(target));
					logger.debug("targetDeneWord=" + targetDeneWord);
					targetDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, renderedValue);
					//
					// this will set it in the currentPulse
					updateDeneWordCurrentPulse(target, renderedValue);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}

			}
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
	}
	
	public Object updateDeneWordCurrentPulse(String pointerToDeneWord, Object valueToUpdate) throws InvalidDenomeException{
		String[] tokens = pointerToDeneWord.substring(1,pointerToDeneWord.length()).split(":");

		String teleonomeName = tokens[0];
		String nucleusName = tokens[1];
		String deneChainName = tokens[2];
		String deneName = tokens[3];
		String deneWordLabel = tokens[4];
		JSONObject aJSONObject, cpInternalNucleus=null,cpPurposeNucleus=null, cpMnemosyneNucleus=null, cpHumanInterfaceNucleus=null;

		JSONObject denomeObject;
		try {
			denomeObject = currentlyCreatingPulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");

			String name;
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
					cpInternalNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
					cpPurposeNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					cpMnemosyneNucleus= aJSONObject;
				}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
					cpHumanInterfaceNucleus= aJSONObject;
				}

			}
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}




		JSONArray deneChainsArray=null;
		try {
			if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
				deneChainsArray = cpInternalNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
				deneChainsArray = cpPurposeNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
				deneChainsArray = cpMnemosyneNucleus.getJSONArray("DeneChains");
			}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
				deneChainsArray = cpHumanInterfaceNucleus.getJSONArray("DeneChains");
			}

			JSONObject  aDeneJSONObject, aDeneWordJSONObject;
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
								if(aDeneWordJSONObject.getString("Name").equals(deneWordLabel)){
									aDeneWordJSONObject.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, valueToUpdate);									
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
		return TeleonomeConstants.VALUE_UNDEFINED;
	}

	public void setAnalyticonsDataSourcesLate(Vector l) {
		// TODO Auto-generated method stub
		analyticonsDataSourcesLate=l;
	}


}
