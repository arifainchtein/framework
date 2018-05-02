package com.teleonome.framework.mnemosyne;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;

import com.teleonome.framework.denome.DenomeManager.MutationActionsExecutionResult;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.InvalidMutation;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;

public class MnemosyneManager {

	Logger logger;
	private PostgresqlPersistenceManager aDBManager=null;
	private static MnemosyneManager aMnemosyneManager;
	private DenomeManager aDenomeManager;
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
	SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss",Locale.US);
	
	Socket exoZeroPublisher=null;
	MqttClient anMqttClient=null;
	public MnemosyneManager(DenomeManager d, MqttClient m){
		logger = Logger.getLogger(getClass());
		logger.info("Initiating MnemosyneManager Manager");
		aDenomeManager = d;
		anMqttClient=m;
		aDBManager = PostgresqlPersistenceManager.instance();
	}

	/**
	 * this instance methd is for the Hypothalamus
	 * 
	 * @param d
	 * @param p
	 * @return
	 * @throws MissingDenomeException
	 */
	public static MnemosyneManager instance(DenomeManager d, MqttClient m) throws MissingDenomeException {

		if(aMnemosyneManager==null){
			aMnemosyneManager = new MnemosyneManager(d,m);
			aMnemosyneManager.init();
		}
		return aMnemosyneManager;
	}

	
	private void init(){

	}

	public void forget(JSONObject aMnemosyconForgetParameters) {
		long mnemosyconProcessingStartingTime = System.currentTimeMillis();
		//
		// the Codon has the Mnemosycon Name
		//
		String aMnemosyconName = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(aMnemosyconForgetParameters, TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		//
		// Get the expression for the mnemosycon
		//
		String mnemosyconForgetApproach = (String) DenomeUtils.getDeneWordAttributeByDeneWordTypeFromDene(aMnemosyconForgetParameters, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		//
		// create the dene for the processing
		//
		JSONObject mnemosyconLogicProcessingDeneChain=null;
		JSONArray mnemosyconLogicProcessingDenes=null;
		try {
			mnemosyconLogicProcessingDeneChain = aDenomeManager.getDeneChainByPointer(TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_MNEMOSYCON_LOGIC_PROCESSING);
			logger.debug("mnemosyconLogicProcessingDeneChain=" + mnemosyconLogicProcessingDeneChain);
			mnemosyconLogicProcessingDenes = mnemosyconLogicProcessingDeneChain.getJSONArray("Denes");

		} catch (JSONException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSONObject mnemosyconProcessingDene = new JSONObject();
		mnemosyconLogicProcessingDenes.put(mnemosyconProcessingDene);
		String mnemosyconLogicProcessingDeneName = aMnemosyconName + " "  + "Processing";

		mnemosyconProcessingDene.put("Name",mnemosyconLogicProcessingDeneName);
		mnemosyconProcessingDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_MNEMOSYCON_PROCESSING);

		JSONArray mnemosyconProcessingDeneDeneWords = new JSONArray();
		mnemosyconProcessingDene.put("DeneWords", mnemosyconProcessingDeneDeneWords);
		mnemosyconLogicProcessingDeneName = aMnemosyconName + " Processing";
		
		JSONObject mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", aMnemosyconName,null,"String",true);
		mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
		//
		// now evaluate the expression
		// if it returns true then enter into the rules loop
		//
		boolean keepGoing=true;
		String mnemosyconRulesPointer = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(aMnemosyconForgetParameters, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_RULES_LIST_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		JSONObject mnemosyconRulesListDeneJSONObject=null;
		try {
			mnemosyconRulesListDeneJSONObject = aDenomeManager.getDeneByIdentity(new Identity(mnemosyconRulesPointer));
		} catch (InvalidDenomeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		JSONArray mnemosyconRulesPointersJSONArray = aDenomeManager.getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyconRulesListDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_RULE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		JSONArray mnemosyconRulesJSONArray = aDenomeManager.renderDeneWordsFromPointers(mnemosyconRulesPointersJSONArray);
		
		JSONObject mnemosyconRuleJSONObject;
		int executionPosition;
		ArrayList<Map.Entry<JSONObject, Integer>> mnemosyneRulesExecutionPositionIndex = new ArrayList();

		for(int i=0;i<mnemosyconRulesJSONArray.length();i++){
			mnemosyconRuleJSONObject = mnemosyconRulesJSONArray.getJSONObject(i);
			executionPosition = (Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconRuleJSONObject,TeleonomeConstants.DENEWORD_EXECUTION_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			mnemosyneRulesExecutionPositionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(mnemosyconRuleJSONObject, new Integer(executionPosition)));
		}
		Collections.sort(mnemosyneRulesExecutionPositionIndex, new IntegerCompare());
		int counter=0;
		String mnemosyconRuleSource,mnemosyconRuleTimeUnit, mnemosyconRuleTeamParameter=null;
		int mnemosyconRuleTimeUnitValue;
		long now=0;
		long millisToDeleteFrom=0;
		int rowsDeleted=0;
		JSONObject mnemosyconRuleProcessingDene, mnemosyconRuleProcessingDeneWord;
		JSONArray mnemosyconRuleProcessingDeneDeneWords;
		SimpleDateFormat mnemosyneTimeFormat = new SimpleDateFormat(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
		String formattedToDeleteFromTimestamp, teamParam;
		long ruleExecutionDurationMillis,mnemosyconExecutionDuration;
		long startRuleMillis;
		long ruleDurationMillis;
		long totalSpace = new File("/").getTotalSpace()/1024000;
		int maximumPercentageDatabase = (int) DenomeUtils.getDeneWordAttributeByDeneWordTypeFromDene(aMnemosyconForgetParameters, TeleonomeConstants.MNEMOSYCON_MAXIMUM_PERCENTAGE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		double sizeToCompare=0;
		boolean executedSuccesfully=true;
		
		do {
			if(mnemosyconForgetApproach.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_DATABASE_SIZE_TO_DISK_SIZE)){
				sizeToCompare = aDBManager.getDatabaseSizeInMB();
				
				 mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Database Size", sizeToCompare,"mb","double",true);
				mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
				
			}else if(mnemosyconForgetApproach.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_ORGANISMPULSE_SIZE_TO_DISK_SIZE)){
				sizeToCompare = aDBManager.getTableSizeInMB("OrganismPulse");
				
				mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("OrganismPulse Table Size", sizeToCompare,"mb","double",true);
				mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
			
			}else if(mnemosyconForgetApproach.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_PULSE_SIZE_TO_DISK_SIZE)){
				sizeToCompare = aDBManager.getTableSizeInMB("Pulse"); 
				mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Pulse Table Size", sizeToCompare,"mb","double",true);
				mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
			
			}
			
			keepGoing = sizeToCompare>(totalSpace*maximumPercentageDatabase)/100;
			mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Total Space", totalSpace,null,"double",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
			mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Codon", aMnemosyconName,null,"String",true);
			mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
			
			
			if(keepGoing) {
				//
				// get the next rule
				startRuleMillis =  System.currentTimeMillis();
				
				mnemosyconRuleTeamParameter=null;
				Map.Entry<JSONObject, Integer> entry = (Map.Entry<JSONObject, Integer>)mnemosyneRulesExecutionPositionIndex.get(counter);
				mnemosyconRuleJSONObject = entry.getKey();
				mnemosyconRuleSource = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconRuleJSONObject, TeleonomeConstants.MNEMOSYCON_RULE_SOURCE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				mnemosyconRuleTimeUnit= (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconRuleJSONObject, TeleonomeConstants.MNEMOSYCON_RULE_TIME_UNIT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				mnemosyconRuleTimeUnitValue= (int) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconRuleJSONObject, TeleonomeConstants.MNEMOSYCON_RULE_TIME_UNIT_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				now=System.currentTimeMillis();
				millisToDeleteFrom=0;
				if(mnemosyconRuleTimeUnit.equals(TeleonomeConstants.TIME_UNIT_DAY)) {
					
					millisToDeleteFrom=now - mnemosyconRuleTimeUnitValue*24*60*60*1000;
					
				}else if(mnemosyconRuleTimeUnit.equals(TeleonomeConstants.TIME_UNIT_WEEK)) {

					millisToDeleteFrom=now - mnemosyconRuleTimeUnitValue*7*24*60*60*1000;
					
				}else if(mnemosyconRuleTimeUnit.equals(TeleonomeConstants.TIME_UNIT_MONTH)) {

					millisToDeleteFrom=now - mnemosyconRuleTimeUnitValue*30*24*60*60*1000;
					
				}else if(mnemosyconRuleTimeUnit.equals(TeleonomeConstants.TIME_UNIT_YEAR)) {

					millisToDeleteFrom=now - mnemosyconRuleTimeUnitValue*365*24*60*60*1000;
					
				}
				//
				// excute the delete
				//
				if(mnemosyconRuleSource.equals(TeleonomeConstants.PULSE_TABLE)) {
					rowsDeleted = aDBManager.deleteByPeriodFromPulse(millisToDeleteFrom);
				}else if(mnemosyconRuleSource.equals(TeleonomeConstants.ORGANISMPULSE_TABLE)) {
					//
					// teamParam is just text that gets added to the delete command
					// so it must contain all the rendered values in sql form
					// there are three potential values
					teamParam="";
					mnemosyconRuleTeamParameter= (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconRuleJSONObject, TeleonomeConstants.MNEMOSYCON_RULE_TEAM_PARAMETER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(mnemosyconRuleTeamParameter!=null && !mnemosyconRuleTeamParameter.equals(TeleonomeConstants.MNEMOSYCON_RULE_TEAM_PARAMETER_ALL)) {
						
						StringBuffer teamsList = new StringBuffer();
						try {
							JSONObject teamDefinitionJSONObject = aDenomeManager.getDeneByIdentity(new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_MNEMOSYCONS, TeleonomeConstants.DENE_MNEMOSYCON_TEAM_DEFINITION));
							JSONArray allTeamMembers = aDenomeManager.getAllDeneWordAttributeByDeneWordTypeFromDene(teamDefinitionJSONObject, TeleonomeConstants.MNEMOSYCON_TEAM_MEMBER,TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							
							for(int i=0;i<allTeamMembers.length();i++) {
								if(i>0)teamsList.append(",");
								teamsList.append("'"+allTeamMembers.getString(i)+"'");
							}
						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
						
						//
						// allTeamMembers contains all the names of the teleonome in this teleonome'team
						if(mnemosyconRuleTeamParameter.equals(TeleonomeConstants.MNEMOSYCON_RULE_TEAM_PARAMETER_TEAM)) {
							//
							// in this case we want to delete records from the team members so 
							teamParam = " and teleonomeName in (" + teamsList + ")";
							
						}else if(mnemosyconRuleTeamParameter.equals(TeleonomeConstants.MNEMOSYCON_RULE_TEAM_PARAMETER_NOT_TEAM)) {
							//
							// in this case we want to delete records from the not  team members so 
							teamParam = " and teleonomeName not in (" + teamsList + ")";
						}
					}
					
					rowsDeleted = aDBManager.deleteByPeriodFromOrganismPulse(millisToDeleteFrom, teamParam);
				}
				aDBManager.vacuum();
				
				
				formattedToDeleteFromTimestamp = mnemosyneTimeFormat.format(millisToDeleteFrom);
				
				
				//
				// now create a dene for this rule 
				// also create a deneword for the parent dene
				// that contains the pointer to this dene as a 
				// deneword
				
				mnemosyconRuleProcessingDene = new JSONObject();
				mnemosyconLogicProcessingDenes.put(mnemosyconRuleProcessingDene);
				mnemosyconRuleProcessingDene.put("Name", mnemosyconRuleJSONObject.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE) + " Processing");
				mnemosyconRuleProcessingDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_MNEMOSYCON_RULE_PROCESSING);
				mnemosyconRuleProcessingDeneDeneWords = new JSONArray();
				mnemosyconRuleProcessingDene.put("DeneWords", mnemosyconRuleProcessingDeneDeneWords);
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Codon", aMnemosyconName,null,"String",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Rows Deleted", rowsDeleted,null,"int",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Delete Until ", formattedToDeleteFromTimestamp,null,"String",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				//
				// now check again to see if we need to keepGoing to the next rule
				//
				if(mnemosyconForgetApproach.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_DATABASE_SIZE_TO_DISK_SIZE)){
					sizeToCompare = aDBManager.getDatabaseSizeInMB();
					
					mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Database Size", sizeToCompare,"mb","double",true);
					mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
					
				}else if(mnemosyconForgetApproach.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_ORGANISMPULSE_SIZE_TO_DISK_SIZE)){
					sizeToCompare = aDBManager.getTableSizeInMB("OrganismPulse");
					
					mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("OrganismPulse Table Size", sizeToCompare,"mb","double",true);
					mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				}else if(mnemosyconForgetApproach.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FORGET_APPROACH_PULSE_SIZE_TO_DISK_SIZE)){
					sizeToCompare = aDBManager.getTableSizeInMB("Pulse");
					mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Pulse Table Size", sizeToCompare,"mb","double",true);
					mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				}
				
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Apply Next Rule", keepGoing,null,"boolean",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject("Total Space", totalSpace,null,"double",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_RULE_SOURCE, mnemosyconRuleSource,null,"String",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_RULE_TIME_UNIT, mnemosyconRuleTimeUnit,null,"String",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);

				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_RULE_TIME_UNIT_VALUE, mnemosyconRuleTimeUnitValue,null,"int",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);

				ruleDurationMillis = System.currentTimeMillis()-startRuleMillis;
				mnemosyconRuleProcessingDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.MNEMOSYCON_RULE_EXECUTION_MILLIS, ruleDurationMillis,"milliseconds","long",true);
				mnemosyconRuleProcessingDeneDeneWords.put(mnemosyconRuleProcessingDeneWord);
				
				//
				// now compare
				keepGoing = sizeToCompare>(totalSpace*maximumPercentageDatabase)/100;
				counter++;
				//
				// now check to see if we need to keep going and if so,
				// if there are any rules left, if its not, then 
				// add a pathology dene saying that  eventough we executed all the rules
				// the expression was not satisfied
				
				if(keepGoing && (counter>=mnemosyneRulesExecutionPositionIndex.size())) {
					//
					// set executedSuccesfully to false
					//
					executedSuccesfully=false;
					//
					// add the dene
					//
					String mnemosyconPathologyLocationPointer= (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(aMnemosyconForgetParameters, TeleonomeConstants.MNEMOSYCON_PATHOLOGY_MNEMOSYNE_LOCATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					JSONObject selectedPathologyDeneChain=null;
					try {
						selectedPathologyDeneChain = aDenomeManager.getDeneChainByIdentity( new Identity(mnemosyconPathologyLocationPointer));
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}


					String pathologyName = TeleonomeConstants.PATHOLOGY_MNEMOSYCON_FAILED;
					String pathologyCause = TeleonomeConstants.PATHOLOGY_MNEMOSYCON_FAILED;
					String pathologyLocation = aMnemosyconName;
					
					
					Vector extraDeneWords = new Vector();
					Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));
					JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_MILLISECONDS, "" + cal.getTime().getTime() ,null,"long",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_TIMESTAMP, mnemosyneTimeFormat.format(cal.getTime()) ,null,"long",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					
					keepGoing = sizeToCompare>(totalSpace*maximumPercentageDatabase)/100;
					
					String data = "Size to Compare=" + sizeToCompare + " totalSpace*maximumPercentageDatabase=" + (totalSpace*maximumPercentageDatabase)/100;
					
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_DETAILS_LABEL, data ,null,"String",true);
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
					// as the threshold
					for(int i=0;i<extraDeneWords.size();i++){
						pathologyDeneDeneWord=(JSONObject)extraDeneWords.elementAt(i);
						pathologyDeneDeneWords.put(pathologyDeneDeneWord);
					}
					
					//
					// now set keepGoing to false to exit the loop
					keepGoing=false;
				}
			}
		}while(keepGoing);
		
		long totalExecutionDuration = System.currentTimeMillis()-mnemosyconProcessingStartingTime;
		mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Total Execution Duration Milliseconds", totalExecutionDuration,"milliseconds","long",true);
		mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
		mnemosyconLogicProcessingCodonDeneDeneWord = Utils.createDeneWordJSONObject("Number Rules Processed", counter,"","int",true);
		mnemosyconProcessingDeneDeneWords.put(mnemosyconLogicProcessingCodonDeneDeneWord);
		//
		// now check to see if the mnemosycon executed succesfully if so return the tasks for success, if not return the task for failure
		//
		String pointerToTasks = null;
		if(executedSuccesfully) {
			pointerToTasks =  (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(aMnemosyconForgetParameters, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_SUCCESS_TASKS_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		}else {
			pointerToTasks =  (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(aMnemosyconForgetParameters, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_FAILURE_TASKS_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		}
		
		if(pointerToTasks!=null && !pointerToTasks.equals("")) {
			aDenomeManager.executeActionSuccessTasks(pointerToTasks);
		}
		
	}
	

	class IntegerCompare implements Comparator<Map.Entry<?, Integer>>{
		public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}
	
	
	public JSONArray remeberDeneWord(Identity identity, long startTimeMillis, long endTimeMillis) {
		return aDBManager.getDeneWordTimeSeriesByIdentity( identity,  startTimeMillis,  endTimeMillis);
	}
	
	
	
	
	public void performTimePrunningAnalysis(){
		logger.info("entering time prunning analysis");
		long timePrunningAnalysisStartTime=System.currentTimeMillis();
		String selectedDenomeFileName=aDenomeManager.getSelectedDenomeFileName();
		File selectedFile = new File(selectedDenomeFileName);
		String denomeFileInString;
		JSONObject denomeJSONObject=null;
		try {
			denomeFileInString = FileUtils.readFileToString(selectedFile);
			denomeJSONObject = new JSONObject(denomeFileInString);
			JSONObject denomeObject = denomeJSONObject.getJSONObject("Denome");
			JSONArray nuclei = denomeObject.getJSONArray("Nuclei");
			JSONObject mnemosyneJSONObject=null;
			found:
			for(int i=0;i<nuclei.length();i++){
				if(nuclei.getJSONObject(i).getString("Name").equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
					mnemosyneJSONObject = nuclei.getJSONObject(i);
					break found;
				}
			}
			JSONArray mnemosyneDeneChains = mnemosyneJSONObject.getJSONArray("DeneChains");
			JSONObject mnemosyneDeneChain;
			String mnemosyneDeneChainName="";
			JSONArray denes, deneWords;
			JSONObject dene, deneWord;
			long deneTimestampMillis;
			LocalDateTime deneLocalDateTime;
			LocalDate deneLocalDate;
			int currentDeneHour;

			LocalDateTime deneDateTime;
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
			
			//
			// do the count no matter what
			for(int i=0;i<mnemosyneDeneChains.length();i++){
				mnemosyneDeneChain=mnemosyneDeneChains.getJSONObject(i);
				mnemosyneDeneChainName = mnemosyneDeneChain.getString("Name");
				denes = mnemosyneDeneChain.getJSONArray("Denes");
				if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_PULSE_COUNT)){
					for(int j=0;j<denes.length();j++){
						dene = denes.getJSONObject(j);
						
					}

				}
			}
			

			LocalDateTime currentTime = LocalDateTime.now();
			LocalDate currentDate = LocalDate.now();
			
			int currentHour = currentTime.getHour();
			int today = currentTime.getDayOfMonth();
			
			LocalDate todayLocalDate = LocalDate.now();
			LocalDate yesterdayLocalDate = todayLocalDate.minusDays(1);
			TemporalField woy = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear(); 
			int currentWeekNumber = currentTime.get(woy);
			int deneWeekNumber;
			
			int currentMonth = currentTime.getMonthValue();
			int currentYear = currentTime.getYear();
			int deneMonth, deneYear;
			
			int currentQuarter = Utils.getQuarter(currentDate);
			int deneQuarter=0;
			String mnemosynePrunningStrategy="";
			
			
			
			boolean keepGoing=false;
			for(int i=0;i<mnemosyneDeneChains.length();i++){
				mnemosyneDeneChain=mnemosyneDeneChains.getJSONObject(i);
				mnemosyneDeneChainName = mnemosyneDeneChain.getString("Name");
				denes = mnemosyneDeneChain.getJSONArray("Denes");
				logger.info("for prunning looking at mnemosyneDeneChainName=" + mnemosyneDeneChainName);
				if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_CURRENT_HOUR)){
					
					keepGoing=false;
					do{
						keepGoing=false;
						start_again:
						for(int j=0;j<denes.length();j++){
							dene = denes.getJSONObject(j);
							deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
							deneLocalDateTime =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
							currentDeneHour = deneLocalDateTime.getHour();
							logger.info("from hourly, currentDeneHour=" + currentDeneHour + " currentHour=" + currentHour);
							if(currentDeneHour!=currentHour){
								//
								// now check if there is a Dene attribute called MNEMOSYNE_PRUNNING_STRATEGY
								// if there is not, then just delete it
								if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
									mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
									if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
										logger.info("removing dene from hourly, deneLocalDateTime=" + deneLocalDateTime);
										denes.remove(j);
										keepGoing=true;
										break start_again;
									}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
										//
										// get this dene's denewords and loop over every one  and set its value to default
										//
										deneWords = dene.getJSONArray("DeneWords");
										for(int k=0;k<deneWords.length();k++){
											deneWord = deneWords.getJSONObject(k);
											if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
												deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
											}
										}
									}
								}else {
									logger.info("removing dene from hourly, deneLocalDateTime=" + deneLocalDateTime);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}
							}
						}
					}while(keepGoing);
					

				}else if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_CURRENT_DAY)){
//					
					
					String deneName="";
					keepGoing=false;
					do{
						keepGoing=false;
						start_again:
						for(int j=0;j<denes.length();j++){
							dene = denes.getJSONObject(j);
							deneName=dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
							deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
							deneLocalDate =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
							
							logger.info("from todday, deneName=" + deneName + " currentDate=" + currentDate + " deneLocalDate=" + deneLocalDate);
							if(!currentDate.equals(deneLocalDate)){
								if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
									mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
									if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
										logger.info("removing dene "+ deneName + " from daily,with prunning strategy remove deneLocalDate=" + deneLocalDate);
										denes.remove(j);
										keepGoing=true;
										break start_again;
									}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
										//
										// get this dene's denewords and loop over every one  and set its value to default
										//
										logger.info("resetting dene "+deneName +" from daily,with prunning strategy reset deneLocalDate=" + deneLocalDate);
										
										deneWords = dene.getJSONArray("DeneWords");
										for(int k=0;k<deneWords.length();k++){
											deneWord = deneWords.getJSONObject(k);
											if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
												logger.info("resetting dene "+deneName +" from daily,with prunning strategy reset reset value=" + deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
												
												deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
											}
										}
									}
								}else {
									logger.info("removing dene from daily,no prunning strategy deneLocalDate=" + deneLocalDate);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}
							}
						}
					}while(keepGoing);
					
					
				}else if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_YESTERDAY)){
					start_again:
					for(int j=0;j<denes.length();j++){
						dene = denes.getJSONObject(j);
						deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
						deneLocalDate =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
						if(!yesterdayLocalDate.equals(deneLocalDate)){
							if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
								mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
								if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
									logger.info("removing dene from yesterday, deneLocalDate=" + deneLocalDate);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
									//
									// get this dene's denewords and loop over every one  and set its value to default
									//
									deneWords = dene.getJSONArray("DeneWords");
									for(int k=0;k<deneWords.length();k++){
										deneWord = deneWords.getJSONObject(k);
										if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
											deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
										}
									}
								}
							}else {
								logger.info("removing dene from yesterday, deneLocalDate=" + deneLocalDate);
								denes.remove(j);
								keepGoing=true;
								break start_again;
							}
						}
					}
				}else if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_CURRENT_WEEK)){
					
					keepGoing=false;
					do{
						keepGoing=false;
						start_again:
						for(int j=0;j<denes.length();j++){
							dene = denes.getJSONObject(j);
							deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
							deneLocalDate =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
							deneWeekNumber = deneLocalDate.get(woy);
							logger.info("from currentweek, deneWeekNumber=" + deneWeekNumber + " currentWeekNumber=" + currentWeekNumber);
							if(deneWeekNumber!=currentWeekNumber){
								if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
									mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
									if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
										logger.info("removing dene from currentweek, deneLocalDate=" + deneLocalDate);
										denes.remove(j);
										keepGoing=true;
										break start_again;
									}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
										//
										// get this dene's denewords and loop over every one  and set its value to default
										//
										deneWords = dene.getJSONArray("DeneWords");
										for(int k=0;k<deneWords.length();k++){
											deneWord = deneWords.getJSONObject(k);
											if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
												deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
											}
										}
									}
								}else {
									logger.info("removing dene from curentweek, deneLocalDate=" + deneLocalDate);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}
							}
						}
					}while(keepGoing);
					
				}else if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_CURRENT_MONTH)){
//					for(int j=0;j<denes.length();j++){
//						dene = denes.getJSONObject(j);
//						deneTimestamp = dene.getString("Timestamp");
//						deneLocalDate = LocalDate.parse(deneTimestamp, formatter);
//						deneMonth = deneLocalDate.getMonthValue();
//						deneYear = deneLocalDate.getYear();
//						if(deneMonth!=currentMonth || deneYear!=currentYear){
//							denes.remove(j);
//						}
//					}
//					
					keepGoing=false;
					do{
						keepGoing=false;
						start_again:
						for(int j=0;j<denes.length();j++){
							dene = denes.getJSONObject(j);
							deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
							deneLocalDate =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
							deneMonth = deneLocalDate.getMonthValue();
							deneYear = deneLocalDate.getYear();
							
							logger.info("from currentmonth, deneMonth=" + deneMonth + " currentMonth=" + currentMonth + " deneYear=" + deneYear + " currentYear=" + currentYear);
							if(deneMonth!=currentMonth || deneYear!=currentYear){
								if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
									mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
									if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
										logger.info("removing dene from currentmonth, deneLocalDate=" + deneLocalDate);
										denes.remove(j);
										keepGoing=true;
										break start_again;
									}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
										//
										// get this dene's denewords and loop over every one  and set its value to default
										//
										deneWords = dene.getJSONArray("DeneWords");
										for(int k=0;k<deneWords.length();k++){
											deneWord = deneWords.getJSONObject(k);
											if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
												deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
											}
										}
									}
								}else {
									logger.info("removing dene from currentmonth, deneLocalDate=" + deneLocalDate);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}
							}
						}
					}while(keepGoing);
					
					
				}else if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_CURRENT_QUARTER)){
//					for(int j=0;j<denes.length();j++){
//						dene = denes.getJSONObject(j);
//						deneTimestamp = dene.getString("Timestamp");
//						deneLocalDate = LocalDate.parse(deneTimestamp, formatter);
//						deneQuarter = Utils.getQuarter(deneLocalDate);
//						deneYear = deneLocalDate.getYear();
//						if(deneQuarter!=deneQuarter || deneYear!=currentYear){
//							denes.remove(j);
//						}
//					}
					
					keepGoing=false;
					do{
						keepGoing=false;
						start_again:
						for(int j=0;j<denes.length();j++){
							dene = denes.getJSONObject(j);
							deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
							deneLocalDate =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
							deneQuarter = Utils.getQuarter(deneLocalDate);
							deneYear = deneLocalDate.getYear();
							logger.info("from currenquarter, deneQuarter=" + deneQuarter + " deneYear=" + deneYear + " currentQuarter=" + currentQuarter + " currentYear=" + currentYear);
							
							if(deneQuarter!=currentQuarter || deneYear!=currentYear){	
								if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
									mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
									if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
										logger.info("removing dene from currentquarter, deneLocalDate=" + deneLocalDate);
										denes.remove(j);
										keepGoing=true;
										break start_again;
									}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
										//
										// get this dene's denewords and loop over every one  and set its value to default
										//
										deneWords = dene.getJSONArray("DeneWords");
										for(int k=0;k<deneWords.length();k++){
											deneWord = deneWords.getJSONObject(k);
											if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
												deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
											}
										}
									}
								}else {
									logger.info("removing dene from currentquarter, deneLocalDate=" + deneLocalDate);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}
							}
						}
					}while(keepGoing);
					
					
				}else if(mnemosyneDeneChainName.equals(TeleonomeConstants.MNEMOSYNE_DENECHAIN_CURRENT_YEAR)){
					
					keepGoing=false;
					do{
						keepGoing=false;
						start_again:
						for(int j=0;j<denes.length();j++){
							dene = denes.getJSONObject(j);
							deneTimestampMillis = dene.getLong("Timestamp Milliseconds");
							deneLocalDate =  Instant.ofEpochMilli(deneTimestampMillis).atZone(ZoneId.systemDefault()).toLocalDate();
							deneYear = deneLocalDate.getYear();
							logger.info("from currenyear, deneYear=" + deneYear + " currentYear=" + currentYear);
							
							if( deneYear!=currentYear){	
								if(dene.has(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY)) {
									mnemosynePrunningStrategy =dene.getString(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY);
									if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_ERASE)) {
										logger.info("removing dene from currentyear, deneLocalDate=" + deneLocalDate);
										denes.remove(j);
										keepGoing=true;
										break start_again;
									}else if(mnemosynePrunningStrategy.equals(TeleonomeConstants.MNEMOSYNE_PRUNNING_STRATEGY_RESET)) {
										//
										// get this dene's denewords and loop over every one  and set its value to default
										//
										deneWords = dene.getJSONArray("DeneWords");
										for(int k=0;k<deneWords.length();k++){
											deneWord = deneWords.getJSONObject(k);
											if(deneWord.has(TeleonomeConstants.DENEWORD_DEFAULT_VALUE)) {
												deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE,deneWord.get(TeleonomeConstants.DENEWORD_DEFAULT_VALUE));
											}
										}
									}
								}else {
									logger.info("removing dene from currentyear, deneLocalDate=" + deneLocalDate);
									denes.remove(j);
									keepGoing=true;
									break start_again;
								}
							}
						}
					}while(keepGoing);
					
				}
				
			}
		
		
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		long timePrunningAnalysisDuration = System.currentTimeMillis()-timePrunningAnalysisStartTime;

		try {
			logger.info("about to save denome after mnemosyne prunning");
			denomeJSONObject.put("Mnemosyne Time Prunning Analysis Duration Millis", timePrunningAnalysisDuration);
			FileUtils.write(new File(selectedDenomeFileName), denomeJSONObject.toString(4));
			//FileUtils.write(new File("arijunk.json"), denomeJSONObject.toString(4));
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e1));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
	}

//	public int myHistoryRecordsReceivedSince(JSONObject mnemosyconProfileDene) {
//		
//		//long importedOnMillis=;
//	//	String destination=;
//		String learnOtherHistoryTeleonomeName="";
//		
//		//int numberRecordsReceived = aDBManager.getNumberRecordsRemembered( importedOnMillis,  destination,  learnOtherHistoryTeleonomeName);
//		//logger.info("creating addMnemosyconProcessingDene," + codon +" " +  processingStartMillis +" " +  processingEndMillis +" " +    numberOfPulsesToRetrieveDuringThisPulse +" " +  estimatedTimeForFinishingCycleString  +" " +  numberOfRecordsLeftBeforeCompletingCycle );
//	//	aDenomeManager.addMnemosyconProcessingDene( codon,  processingStartMillis,  processingEndMillis,  numberOfPulsesToRetrieveDuringThisPulse, "sharePulseBatch", TeleonomeConstants.TELEONOME_IDENTITY_SELF,estimatedTimeForFinishingCycleString, numberOfRecordsLeftBeforeCompletingCycle, firstPulseInBatchMilliseconds, lastPulseInBatchMilliseconds, updatedNumberOfPulsesToRetrieveDuringThisPulse,neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse);
//		 
//	}
	
	public void sharePulseBatch(JSONObject mnemosyconProfileDene) {
		long processingStartMillis = System.currentTimeMillis();
		String codon="";
		long millisStartingPoint;
		
		try {
			millisStartingPoint = (long) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NEXT_BATCH_MILLIS_STARTING_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			JSONObject cycleStartMillisDeneWord = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_CYCLE_START_MILLISECONDS, TeleonomeConstants.COMPLETE);
			if(millisStartingPoint==0) {
				//
				// if we are here is because we are starting the cycle
				// so mark the denewords
				cycleStartMillisDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, processingStartMillis);
				
				JSONObject deneWord = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_CYCLE_START_TIMESTAMP, TeleonomeConstants.COMPLETE);
				deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dateFormat.format(processingStartMillis));
			}
			
			long cycleStartedOnMillis = cycleStartMillisDeneWord.getLong(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			int maximumNumberRecordPerBatch = (int) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_MAXIMUM_NUMBER_OF_RECORDS_PER_BATCH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			
			int numberOfPulsesToRetrieveDuringThisPulse =  (int) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			String pointerToActionSuccessTasks =  (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.DENEWORD_TYPE_ACTION_SUCCESS_TASK_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			codon =  (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			int numberOfRecordsLeftBeforeCompletingCycle = aDBManager.getNumberOfPulsesFromTime(millisStartingPoint);
			int secondsToCompleteCycle=-1;
			long millisecondsRemaining=-1;
			Object o = aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene,TeleonomeConstants.MNEMOSYCON_NUMBER_OF_SECONDS_TO_COMPLETE_CYCLE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if(o!=null && o instanceof Integer) {
				secondsToCompleteCycle=(int)o;
				millisecondsRemaining = (cycleStartedOnMillis + secondsToCompleteCycle*1000) - processingStartMillis;
			}
			int updatedNumberOfPulsesToRetrieveDuringThisPulse=-1;
			String estimatedTimeForFinishingCycleString="";
			int neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse=-1;
			try {
				int currentPulseFrequencyMilliseconds  = aDenomeManager.getCurrentPulseFrequencyMilliseconds();
				long estimatedTimeForFinishingCycle = currentPulseFrequencyMilliseconds*numberOfRecordsLeftBeforeCompletingCycle;
				estimatedTimeForFinishingCycleString = Utils.getElapsedTimeHoursMinutesSecondsString(estimatedTimeForFinishingCycle);
				//
				// if secondsToCompleteCycle>0 then we need to calculate to see if the numberOfPulsesToRetrieveDuringThisPulse 
				// needs to be increased
				if(secondsToCompleteCycle>0 ) {
					if((estimatedTimeForFinishingCycle/1000)>millisecondsRemaining){
						updatedNumberOfPulsesToRetrieveDuringThisPulse = (int) (numberOfPulsesToRetrieveDuringThisPulse*((estimatedTimeForFinishingCycle/1000)/millisecondsRemaining));
					}else {
						updatedNumberOfPulsesToRetrieveDuringThisPulse = (int) (numberOfPulsesToRetrieveDuringThisPulse*(millisecondsRemaining/(estimatedTimeForFinishingCycle/1000)));
					}
					//
					// now check to see if we have a maximum
					//
					if(maximumNumberRecordPerBatch>-1 && maximumNumberRecordPerBatch<updatedNumberOfPulsesToRetrieveDuringThisPulse) {
						updatedNumberOfPulsesToRetrieveDuringThisPulse= maximumNumberRecordPerBatch;
						neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse=updatedNumberOfPulsesToRetrieveDuringThisPulse;
					}
				}
			} catch (InvalidDenomeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			JSONArray batchOfPulses =  null;
			if(updatedNumberOfPulsesToRetrieveDuringThisPulse>-1) {
				batchOfPulses = aDBManager.getPulsesFromTime(millisStartingPoint, updatedNumberOfPulsesToRetrieveDuringThisPulse);
			}else {
				batchOfPulses = aDBManager.getPulsesFromTime(millisStartingPoint, numberOfPulsesToRetrieveDuringThisPulse);
						
			}
			String pulse, pulseTimestamp="";
			JSONObject pulseJSONObject;
			long pulseTimestampMillis=0;
			long firstPulseInBatchMilliseconds=0;
			if(batchOfPulses.length()>0) {
				for(int i=0;i<batchOfPulses.length();i++) {
					pulseJSONObject = batchOfPulses.getJSONObject(i);
					pulse = pulseJSONObject.toString();
					//
					// Publish the pulse to the ExoZero Network
					//
					Context exozeroContext = ZMQ.context(1);
					Socket exoZeroPublisher = exozeroContext.socket(ZMQ.PUB);
					exoZeroPublisher.setHWM(2);
					String ipToBindToZeroMQ="";
					try {
						ipToBindToZeroMQ = Utils.getIpAddressForNetworkMode().getHostAddress();
					} catch (SocketException | UnknownHostException e2) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e2));
						
					}
					logger.info("binding zeromq to " + ipToBindToZeroMQ);
					exoZeroPublisher.bind("tcp://" + ipToBindToZeroMQ + ":" + TeleonomeConstants.EXOZERO_MNEMOSYNE_MANAGER_PORT);
					
					exoZeroPublisher.sendMore("Remember_" + aDenomeManager.getDenomeName());
					exoZeroPublisher.send(pulse); 
					exoZeroPublisher.close();
					exozeroContext.close();
					    
					logger.debug("published  pulse to zeromq");
					
					//
					// End of Publish the pulse to the ExoZero Network
					//
					
				
					pulseTimestampMillis = pulseJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
					pulseTimestamp = pulseJSONObject.getString(TeleonomeConstants.PULSE_TIMESTAMP);
					if(i==0) {
						firstPulseInBatchMilliseconds=pulseTimestampMillis;
					}
				}
			}else {
				//
				// if we are here then there are no more records, so
				// start again from the beginning
				pulseTimestampMillis=0;
				pulseTimestamp="";
				
			}
			long lastPulseInBatchMilliseconds=pulseTimestampMillis;
			
			JSONObject deneWordMillis = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NEXT_BATCH_MILLIS_STARTING_POINT, TeleonomeConstants.COMPLETE);
			deneWordMillis.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, pulseTimestampMillis);
			
			
			if(updatedNumberOfPulsesToRetrieveDuringThisPulse>-1) {
				logger.info("Updating " + TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH + " to " + updatedNumberOfPulsesToRetrieveDuringThisPulse);
				JSONObject deneWordNumPulses = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH, TeleonomeConstants.COMPLETE);
				deneWordNumPulses.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, updatedNumberOfPulsesToRetrieveDuringThisPulse);	
			}
			
			long processingEndMillis = System.currentTimeMillis();
			//
			// create the processing name
			//
			logger.info("creating addMnemosyconProcessingDene," + codon +" " +  processingStartMillis +" " +  processingEndMillis +" " +    numberOfPulsesToRetrieveDuringThisPulse +" " +  estimatedTimeForFinishingCycleString  +" " +  numberOfRecordsLeftBeforeCompletingCycle );
			aDenomeManager.addMnemosyconProcessingDene( codon,  processingStartMillis,  processingEndMillis,  numberOfPulsesToRetrieveDuringThisPulse, "sharePulseBatch", TeleonomeConstants.TELEONOME_IDENTITY_SELF,estimatedTimeForFinishingCycleString, numberOfRecordsLeftBeforeCompletingCycle, firstPulseInBatchMilliseconds, lastPulseInBatchMilliseconds, updatedNumberOfPulsesToRetrieveDuringThisPulse,neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse);
			
			
		}catch (JSONException | SQLException e) {
			// TODO Auto-generated catch block
			String error = Utils.getStringException(e);
			logger.info(error);
			// add pathology dene
			String pathologyCause = "";
			if(e instanceof SQLException) {
				pathologyCause = TeleonomeConstants.PATHOLOGY_SQL_EXCEPTION;
			}else {
				pathologyCause = TeleonomeConstants.PATHOLOGY_JSON_EXCEPTION;
			}
			String pathologyName = TeleonomeConstants.PATHOLOGY_DENE_MNEMSYCON_PROCESSING_ERROR;
			
			String pathologyLocation = new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_MNEMOSYCONS,codon).toString();
			Vector extraDeneWords = new Vector();

			JSONObject pathologyDeneDeneWord;
			try {
				pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EXCEPTION_STACK_TRACE, Utils.getStringException(e) ,null,"String",true);
				extraDeneWords.addElement(pathologyDeneDeneWord);
				aDenomeManager.addPurposePathologyDene(pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}
	
	public void shareOrganismPulseBatch(JSONObject mnemosyconProfileDene) {
		long processingStartMillis = System.currentTimeMillis();
		String codon="";
		long millisStartingPoint;
		try {
			logger.info("mnemosyconProfileDene=" + mnemosyconProfileDene);
			try{
				millisStartingPoint = (long) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NEXT_BATCH_MILLIS_STARTING_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			
			}catch(ClassCastException e) {
				millisStartingPoint = (int) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NEXT_BATCH_MILLIS_STARTING_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				
			}
			
			
			JSONObject cycleStartMillisDeneWord = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_CYCLE_START_MILLISECONDS, TeleonomeConstants.COMPLETE);
			if(millisStartingPoint==0) {
				//
				// if we are here is because we are starting the cycle
				// so mark the denewords
				cycleStartMillisDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, processingStartMillis);
				
				JSONObject deneWord = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_CYCLE_START_TIMESTAMP, TeleonomeConstants.COMPLETE);
				deneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, dateFormat.format(processingStartMillis));
			}
			
			long cycleStartedOnMillis = cycleStartMillisDeneWord.getLong(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			
			Integer I = (Integer) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_MAXIMUM_NUMBER_OF_RECORDS_PER_BATCH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE); 
			int maximumNumberRecordPerBatch = -1;
			if(I!=null)maximumNumberRecordPerBatch=I.intValue();
			
			 I = (Integer) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_MINIMUM_NUMBER_OF_RECORDS_PER_BATCH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE); 
			int minmumNumberRecordPerBatch = -1;
			if(I!=null)minmumNumberRecordPerBatch=I.intValue();
			
			
			int numberOfPulsesToRetrieveDuringThisPulse =  (int) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			String pointerToActionSuccessTasks =  (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.DENEWORD_TYPE_ACTION_SUCCESS_TASK_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			String teleonomeName =  (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_ORGANISM_TELEONOME_TO_PUBLISH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			codon =  (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			int numberOfPulseRecordsLeftBeforeCycleCompletion = aDBManager.getNumberOrganismPulsesFromTime(teleonomeName, millisStartingPoint, numberOfPulsesToRetrieveDuringThisPulse);
			JSONArray batchOfPulses =  aDBManager.getOrganismPulsesFromTime(teleonomeName, millisStartingPoint, numberOfPulsesToRetrieveDuringThisPulse);
			
			int secondsToCompleteCycle=-1;
			long millisecondsRemaining=-1;
			Object o = aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene,TeleonomeConstants.MNEMOSYCON_NUMBER_OF_SECONDS_TO_COMPLETE_CYCLE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if(o!=null && o instanceof Integer) {
				secondsToCompleteCycle=(int)o;
				millisecondsRemaining = (cycleStartedOnMillis + secondsToCompleteCycle*1000) - processingStartMillis;
			}
			int updatedNumberOfPulsesToRetrieveDuringThisPulse=-1;
			int neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse=-1;
			int numberOfPulsesLeftBeforeCompletingCycle = numberOfPulseRecordsLeftBeforeCycleCompletion/numberOfPulsesToRetrieveDuringThisPulse;
			String estimatedTimeForFinishingCycleString="";
			try {
				int currentPulseFrequencyMilliseconds  = aDenomeManager.getCurrentPulseFrequencyMilliseconds();
				long estimatedTimeForFinishingCycle = currentPulseFrequencyMilliseconds*numberOfPulsesLeftBeforeCompletingCycle;
				estimatedTimeForFinishingCycleString = Utils.getElapsedTimeHoursMinutesSecondsString(estimatedTimeForFinishingCycle);
				//
				// if secondsToCompleteCycle>0 then we need to calculate to see if the numberOfPulsesToRetrieveDuringThisPulse 
				// needs to be increased
				if(secondsToCompleteCycle>0 ) {
					if(estimatedTimeForFinishingCycle>millisecondsRemaining){
						updatedNumberOfPulsesToRetrieveDuringThisPulse = (int) (numberOfPulsesToRetrieveDuringThisPulse*((estimatedTimeForFinishingCycle/1000)/secondsToCompleteCycle));
					}else {
						updatedNumberOfPulsesToRetrieveDuringThisPulse = (int) (numberOfPulsesToRetrieveDuringThisPulse*(millisecondsRemaining/estimatedTimeForFinishingCycle));
					}
					//
					// now check to see if we have a maximum
					//
					if(maximumNumberRecordPerBatch>-1 && maximumNumberRecordPerBatch<updatedNumberOfPulsesToRetrieveDuringThisPulse) {
						updatedNumberOfPulsesToRetrieveDuringThisPulse= maximumNumberRecordPerBatch;
						neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse=updatedNumberOfPulsesToRetrieveDuringThisPulse;
					}
					//
					// now check to see if we have a minimum
					//
					if(minmumNumberRecordPerBatch>-1 && minmumNumberRecordPerBatch>updatedNumberOfPulsesToRetrieveDuringThisPulse) {
						updatedNumberOfPulsesToRetrieveDuringThisPulse= minmumNumberRecordPerBatch;
						neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse=updatedNumberOfPulsesToRetrieveDuringThisPulse;
					}
				}
			} catch (InvalidDenomeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String pulse, pulseTimestamp="";
			JSONObject pulseJSONObject;
			long pulseTimestampMillis=0;
			long firstPulseInBatchMilliseconds=0;
			if(batchOfPulses.length()>0) {
				for(int i=0;i<batchOfPulses.length();i++) {
					pulseJSONObject = batchOfPulses.getJSONObject(i);
					pulse = pulseJSONObject.toString();
					
					
					
					//
					// Publish the pulse to the ExoZero Network
					//
					Context exozeroContext = ZMQ.context(1);
					Socket exoZeroPublisher = exozeroContext.socket(ZMQ.PUB);
					exoZeroPublisher.setHWM(2);
					String ipToBindToZeroMQ="";
					try {
						ipToBindToZeroMQ = Utils.getIpAddressForNetworkMode().getHostAddress();
					} catch (SocketException | UnknownHostException e2) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e2));
						
					}
					logger.info("binding zeromq to " + ipToBindToZeroMQ);
					exoZeroPublisher.bind("tcp://" + ipToBindToZeroMQ + ":5563");
					
					exoZeroPublisher.sendMore("Remember_" + teleonomeName);
					exoZeroPublisher.send(pulse); 
					exoZeroPublisher.close();
					exozeroContext.close();
					    
					logger.debug("published  pulse to zeromq");
					
					//
					// End of Publish the pulse to the ExoZero Network
					//
					
					
					
					pulseTimestampMillis = pulseJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
					if(i==0) {
						firstPulseInBatchMilliseconds=	pulseTimestampMillis;
					}
					pulseTimestamp = pulseJSONObject.getString(TeleonomeConstants.PULSE_TIMESTAMP);
				}
			}else {
				//
				// if we are here then there are no more records, so
				// start again from the beginning
				pulseTimestampMillis=0;
				pulseTimestamp="";
				
			}
			long lastPulseInBatchMilliseconds=pulseTimestampMillis;
			long processingEndMillis = System.currentTimeMillis();
			
			JSONObject deneWordMillis = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NEXT_BATCH_MILLIS_STARTING_POINT, TeleonomeConstants.COMPLETE);
			deneWordMillis.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, pulseTimestampMillis);
			
			if(updatedNumberOfPulsesToRetrieveDuringThisPulse>-1) {
				logger.info("Updating " + TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH + " to " + updatedNumberOfPulsesToRetrieveDuringThisPulse);
				JSONObject deneWordNumPulses = (JSONObject) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconProfileDene, TeleonomeConstants.MNEMOSYCON_NUMBER_OF_PULSES_TO_RETRIEVE_PER_BATCH, TeleonomeConstants.COMPLETE);
				deneWordNumPulses.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, updatedNumberOfPulsesToRetrieveDuringThisPulse);	
			}
			//
			// create the processing name
			//
			logger.info("creating addMnemosyconProcessingDene,batchOfPulses="+ batchOfPulses.length()+ " "  + codon +" " +  processingStartMillis +" " +  processingEndMillis +" " +    numberOfPulsesToRetrieveDuringThisPulse +" " +  estimatedTimeForFinishingCycleString  +" " +  numberOfPulsesLeftBeforeCompletingCycle );
			
			aDenomeManager.addMnemosyconProcessingDene( codon,  processingStartMillis,  processingEndMillis,  numberOfPulsesToRetrieveDuringThisPulse, "shareOrganismPulseBatch", teleonomeName, estimatedTimeForFinishingCycleString, numberOfPulseRecordsLeftBeforeCycleCompletion, firstPulseInBatchMilliseconds, lastPulseInBatchMilliseconds, updatedNumberOfPulsesToRetrieveDuringThisPulse, neededUpdatedNumberOfPulsesToRetrieveDuringThisPulse);
			
		} catch (JSONException | SQLException e) {
			// TODO Auto-generated catch block
			String error = Utils.getStringException(e);
			logger.info(error);
			// add pathology dene
			String pathologyCause = "";
			if(e instanceof SQLException) {
				pathologyCause = TeleonomeConstants.PATHOLOGY_SQL_EXCEPTION;
			}else {
				pathologyCause = TeleonomeConstants.PATHOLOGY_JSON_EXCEPTION;
			}
			String pathologyName = TeleonomeConstants.PATHOLOGY_DENE_MNEMSYCON_PROCESSING_ERROR;
			
			String pathologyLocation = new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_MNEMOSYCONS,codon).toString();
			Vector extraDeneWords = new Vector();

			JSONObject pathologyDeneDeneWord;
			try {
				pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EXCEPTION_STACK_TRACE, Utils.getStringException(e) ,null,"String",true);
				extraDeneWords.addElement(pathologyDeneDeneWord);
				aDenomeManager.addPurposePathologyDene(pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}
	
	
	//
	// Denome embedded functions
	// this is for the value TeleonomeConstants.MNEMOSYNE_PROCESSING_FUNCTION_RANGE_EVALUATION

	public JSONArray evaluateRange(Identity dataSourceIdentity, JSONObject analysisProfileDene){
		logger.debug("entering evaluate range");
		JSONArray arrayToReturn = new JSONArray();
		Connection connection=null;
		PreparedStatement preparedStatement=null;
		ResultSet rs=null;
		String timeZoneName="UTC";
		try{
			String denomeName=dataSourceIdentity.getTeleonomeName();
			String nucleusName=dataSourceIdentity.getNucleusName();
			String deneChainName=dataSourceIdentity.getDenechainName();
			String deneName=dataSourceIdentity.getDeneName();
			String deneWordName=dataSourceIdentity.getDeneWordName();

			logger.debug("denomeName=" + denomeName);
			logger.debug("nucleusName=" + nucleusName);
			logger.debug("deneChainName=" + deneChainName);
			logger.debug("deneName=" + deneName);
			logger.debug("deneWordName=" + deneWordName);

			//
			// the intervals can either be a String like "Now" or a number like -86400
			// so get them as object and test them

			Object intervalStart=  aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_START, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			Object intervalEnd =  aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_END, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			
			try {
				timeZoneName = (String) aDenomeManager.getDeneWordAttributeByIdentity(new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,  TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL, "Timezone"  ), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			} catch (InvalidDenomeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(timeZoneName));


			String sampleType = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene,  TeleonomeConstants.MNEMOSYNE_PROCESSING_SAMPLE_TYPE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			int sampleFrequency = (int) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene,  TeleonomeConstants.MNEMOSYNE_PROCESSING_SAMPLE_FREQUENCY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			String returnOrder="";
			if(sampleType.equals(TeleonomeConstants.MNEMOSYNE_ANALYSIS_MAXIMUM)){
				returnOrder="desc";
			}else if(sampleType.equals(TeleonomeConstants.MNEMOSYNE_ANALYSIS_MINIMUM)){
				returnOrder="asc";
			}

			logger.debug("intervalStart=" + intervalStart);
			logger.debug("intervalEnd=" + intervalEnd);
			logger.debug("sampleType=" + sampleType);
			logger.debug("sampleFrequency=" + sampleFrequency);
			logger.debug("timeZoneName=" + timeZoneName);
			//
			String sql="";
			long valueTimestamp;
			double returnedValue;
			int numberOfIterations=0;
			long currentRangeStart=0, currentRangeEnd=0;
			JSONObject valueJSON=null;
			//
			// check the end first
			if(intervalEnd instanceof String){
				if(intervalEnd.equals(TeleonomeConstants.MNEMOSYNE_PROCESSING_NOW)){
					//
					// the end is now, so check for the start, which could be expressed as a negative number (number of seconds before)
					// 

					try{
						int startValue=0;
						if(intervalStart instanceof Integer){
							startValue = ((Integer)intervalStart).intValue();
						}

						//
						if(startValue<0){
							//
							// ok, we want the range from now to a number before now, so for example if startValue=-86400 this means
							// we want to start 24 hours before now, ie 24*60*60.  to get the number of iterations divide the start
							// startValue by the frequency which is also expressed in seconds, so if the frequency is 1800 (i.e.30 minutes)
							// we would get 48.  numberOfIterations is int because we always want an integer
							// because its a negative we substract add it to the currentime to get the currentrangestart
							numberOfIterations=(-1*startValue)/sampleFrequency;
							
							currentRangeStart=Calendar.getInstance(TimeZone.getTimeZone(timeZoneName)).getTimeInMillis()+startValue*1000;
							currentRangeEnd=currentRangeStart + sampleFrequency*1000;

						}
					}catch(NumberFormatException e){

					}
				}else{

				}
			}else{
				//
				// intervalEnd is not a string, cast it as a number
			}

			logger.info("evaluate range number of iterations=" + numberOfIterations);
			if(numberOfIterations>0){
				connection = aDBManager.getConnection();
				logger.debug("got connection connection=" + connection);

				long iterationStartTime=0;
				long iterationDurationMillis=0;
				long extractionProcessStart=System.currentTimeMillis();
				long timeLeft=0;
				long now=0;
				long averagePerIteration=0;
				for(int i=0;i<numberOfIterations;i++){

					iterationStartTime=System.currentTimeMillis();

					sql="select p.data->>'Pulse Timestamp in Milliseconds' As Timestamp, DeneWord -> 'Value' As CurrentLoad from organismpulse p, "
							+ "jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , "
							+ "jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where  "
							+ "Nucleus->>'Name'='"+ nucleusName +"' and DeneChain->>'Name'='"+ deneChainName +"' and Dene->>'Name'='"+  deneName +"' and "
							+ "DeneWord->>'Name'='"+ deneWordName +"' and p.data->'Denome'->>'Name'='"+ denomeName +"' and "
							+ "cast((p.data->>'Pulse Timestamp in Milliseconds') as bigint) > "+ currentRangeStart +" and " 
							+ "cast((p.data->>'Pulse Timestamp in Milliseconds') as bigint) < "+ currentRangeEnd 
							+ " order by pulsetimemillis "+ returnOrder +" limit 1";

					logger.debug("sql=" + sql);
					preparedStatement = connection.prepareStatement(sql);

					//preparedStatement.setLong(1, currentRangeStart);	
					//preparedStatement.setLong(2, currentRangeEnd);
					calendar.setTimeInMillis(currentRangeStart);
					String startDate = dateFormat.format(calendar.getTime());
					calendar.setTimeInMillis(currentRangeEnd);
					String endDate = dateFormat.format(calendar.getTime());
					
					rs = preparedStatement.executeQuery();
					returnedValue=-1;
					valueTimestamp=-1;
					while(rs.next()){
						valueTimestamp = rs.getLong(1);
						returnedValue = Double.parseDouble(rs.getString(2).replace("\"", ""));

						valueJSON = new JSONObject();
						valueJSON.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, valueTimestamp);
						valueJSON.put("Value", returnedValue);
						arrayToReturn.put(valueJSON);
						logger.debug("result valueJSON=" + valueJSON.toString(4) );
					}
					now = System.currentTimeMillis();
					iterationDurationMillis = now -iterationStartTime;
					averagePerIteration = (now-extractionProcessStart)/(i+1);
					timeLeft = averagePerIteration*(numberOfIterations - (i+1));
					String messageText = "Iteration " + i + " out of " + numberOfIterations +" For Period (" + startDate + " ," + endDate + ") Result=("+ valueTimestamp + "," +returnedValue + ") Iteration took "+ Utils.getElapsedTimeHoursMinutesSecondsString(iterationDurationMillis) + " ETA:" + Utils.getElapsedTimeHoursMinutesSecondsString(timeLeft);
					logger.info(messageText);
					messageText= timeFormat.format(new Date()) + "-" + messageText;
					MqttMessage message = new MqttMessage(messageText.getBytes());
				    message.setQos(TeleonomeConstants.HEART_QUALITY_OF_SERVICE);
				    message.setRetained(true);
					try {
						anMqttClient.publish(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO_SECUNDARY, message);
					} catch (MqttException e1) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e1));
					}
					//
					// the next iteration increment the start and ending values
					// so that the next startvalue is the same as the previous endValue
					// and the next endValue is the current endValue plus the frequency
					currentRangeStart = currentRangeEnd;
					currentRangeEnd= currentRangeEnd + sampleFrequency*1000;
					//
					// sleep a bit to let other processes do things
					//
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		}catch(JSONException e){
			logger.info(Utils.getStringException(e));
		}catch(SQLException e){
			logger.info(Utils.getStringException(e));
		}finally{
			try{
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)aDBManager.closeConnection(connection);
			}catch(SQLException e){
				logger.info(Utils.getStringException(e));
			}

		}

		return arrayToReturn;
	}

	public JSONArray getHistory(Identity dataSourceIdentity, JSONObject analysisProfileDene){
		logger.info("entering evaluate range");
		JSONArray arrayToReturn = new JSONArray();
		Connection connection=null;
		PreparedStatement preparedStatement=null;
		ResultSet rs=null;
		String timeZoneName="UTC";
		try{
			String denomeName=dataSourceIdentity.getTeleonomeName();
			String nucleusName=dataSourceIdentity.getNucleusName();
			String deneChainName=dataSourceIdentity.getDenechainName();
			String deneName=dataSourceIdentity.getDeneName();
			String deneWordName=dataSourceIdentity.getDeneWordName();

			logger.info("denomeName=" + denomeName);
			logger.info("nucleusName=" + nucleusName);
			logger.info("deneChainName=" + deneChainName);
			logger.info("deneName=" + deneName);
			logger.info("deneWordName=" + deneWordName);

			//
			// the intervals can either be a String like "Now" or a number like -86400
			// so get them as object and test them

			Object intervalStart=  aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_START, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			Object intervalEnd =  aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_PROCESSING_INTERVAL_END, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			
			try {
				timeZoneName = (String) aDenomeManager.getDeneWordAttributeByIdentity(new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,  TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL, "Timezone"  ), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			} catch (InvalidDenomeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone(timeZoneName));


			String sampleType = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene,  TeleonomeConstants.MNEMOSYNE_PROCESSING_SAMPLE_TYPE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			int sampleFrequency = (int) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene,  TeleonomeConstants.MNEMOSYNE_PROCESSING_SAMPLE_FREQUENCY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

			String returnOrder="";
			if(sampleType.equals(TeleonomeConstants.MNEMOSYNE_ANALYSIS_MAXIMUM)){
				returnOrder="desc";
			}else if(sampleType.equals(TeleonomeConstants.MNEMOSYNE_ANALYSIS_MINIMUM)){
				returnOrder="asc";
			}

			logger.info("intervalStart=" + intervalStart);
			logger.info("intervalEnd=" + intervalEnd);
			logger.info("sampleType=" + sampleType);
			logger.info("sampleFrequency=" + sampleFrequency);
			logger.info("timeZoneName=" + timeZoneName);
			//
			String sql="";
			long valueTimestamp;
			double returnedValue;
			int numberOfIterations=0;
			long currentRangeStart=0, currentRangeEnd=0;
			JSONObject valueJSON=null;
			//
			// check the end first
			if(intervalEnd instanceof String){
				if(intervalEnd.equals(TeleonomeConstants.MNEMOSYNE_PROCESSING_NOW)){
					//
					// the end is now, so check for the start, which could be expressed as a negative number (number of seconds before)
					// 

					try{
						int startValue=0;
						if(intervalStart instanceof Integer){
							startValue = ((Integer)intervalStart).intValue();
						}

						//
						if(startValue<0){
							//
							// ok, we want the range from now to a number before now, so for example if startValue=-86400 this means
							// we want to start 24 hours before now, ie 24*60*60.  to get the number of iterations divide the start
							// startValue by the frequency which is also expressed in seconds, so if the frequency is 1800 (i.e.30 minutes)
							// we would get 48.  numberOfIterations is int because we always want an integer
							// because its a negative we substract add it to the currentime to get the currentrangestart
							numberOfIterations=(-1*startValue)/sampleFrequency;
							
							currentRangeStart=Calendar.getInstance(TimeZone.getTimeZone(timeZoneName)).getTimeInMillis()+startValue*1000;
							currentRangeEnd=currentRangeStart + sampleFrequency*1000;

						}
					}catch(NumberFormatException e){

					}
				}else{

				}
			}else{
				//
				// intervalEnd is not a string, cast it as a number
			}

			logger.info("number of iterations=" + numberOfIterations);
			if(numberOfIterations>0){
				connection = aDBManager.getConnection();
				logger.info("got connection connection=" + connection);


				for(int i=0;i<numberOfIterations;i++){

					//1000*(SELECT extract(epoch from now())-(24*60*60))
					//currentRangeStart=0;

					//1000*(SELECT extract(epoch from now())-(24*60*60)  + "+ sampleFrequency+
					//currentRangeEnd=0;

					sql="select DeneWord -> 'Value' As CurrentPulse from pulse p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  " +
					"jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene," +
					"jsonb_array_elements(Dene->'DeneWords') as DeneWord where createdon in (select createdon from pulse" +
					"order by createdon desc limit 1) and Nucleus->>'Name'='Purpose' and DeneChain->>'Name'='Mnemosycon Processing' " +
					"and Dene->>'Name'='Share Teleonome From Organism History Processing' and DeneWord->>'Name'='Batch Execution Took Milliseconds'" +
					"and cast((p.data->>'Pulse Timestamp in Milliseconds') as bigint) > 0 and cast((p.data->>'Pulse Timestamp in Milliseconds') as bigint) < 1493614731570 order by createdon";
					
					
					sql="select p.data->>'Pulse Timestamp in Milliseconds' As Timestamp, DeneWord -> 'Value' As CurrentLoad from organismpulse p, "
							+ "jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , "
							+ "jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where  "
							+ "Nucleus->>'Name'='"+ nucleusName +"' and DeneChain->>'Name'='"+ deneChainName +"' and Dene->>'Name'='"+  deneName +"' and "
							+ "DeneWord->>'Name'='"+ deneWordName +"' and p.data->'Denome'->>'Name'='"+ denomeName +"' and "
							+ "cast((p.data->>'Pulse Timestamp in Milliseconds') as bigint) > "+ currentRangeStart +" and " 
							+ "cast((p.data->>'Pulse Timestamp in Milliseconds') as bigint) < "+ currentRangeEnd 
							+ " order by pulsetimemillis "+ returnOrder +" limit 1";

					//logger.info("sql=" + sql);
					preparedStatement = connection.prepareStatement(sql);

					//preparedStatement.setLong(1, currentRangeStart);	
					//preparedStatement.setLong(2, currentRangeEnd);
					calendar.setTimeInMillis(currentRangeStart);
					String startDate = dateFormat.format(calendar.getTime());
					calendar.setTimeInMillis(currentRangeEnd);
					String endDate = dateFormat.format(calendar.getTime());
					
					logger.info("about to execute i=" + i + " currentRangeStart=" + currentRangeStart + " currentRangeEnd=" + currentRangeEnd + " start:" + startDate + " end:" + endDate);
					rs = preparedStatement.executeQuery();
					while(rs.next()){
						valueTimestamp = rs.getLong(1);
						returnedValue = Double.parseDouble(rs.getString(2).replace("\"", ""));

						valueJSON = new JSONObject();
						valueJSON.put("Timestamp in Millis", valueTimestamp);
						valueJSON.put("Value", returnedValue);
						arrayToReturn.put(valueJSON);
						logger.info("result valueJSON=" + valueJSON.toString(4) );
					}
					//
					// the next iteration increment the start and ending values
					// so that the next startvalue is the same as the previous endValue
					// and the next endValue is the current endValue plus the frequency
					currentRangeStart = currentRangeEnd;
					currentRangeEnd= currentRangeEnd + sampleFrequency*1000;

				}
			}
		}catch(JSONException e){
			logger.info(Utils.getStringException(e));
		}catch(SQLException e){
			logger.info(Utils.getStringException(e));
		}finally{
			try{
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)aDBManager.closeConnection(connection);
			}catch(SQLException e){
				logger.info(Utils.getStringException(e));
			}

		}

		return arrayToReturn;
	}
	

	//
	// end of Denome embedded functions
	//
	public ArrayList<Map.Entry<Object, Long>> getDeneWordByPeriodFromOrganismPulse(Identity identity, String startPulseTimestampString, String endPulseTimestampString) {
		if(identity.getDeneWordName()==null || identity.getDeneWordName().equals("")){
			return null;
		}
		ArrayList<Map.Entry<JSONObject, Long>> array = aDBManager.getPulseForRangeForOrganism(identity.getTeleonomeName(), startPulseTimestampString,  endPulseTimestampString);	
		JSONObject denomePulse;
		long pulseTimestamp;
		ArrayList<Map.Entry<Object, Long>> toReturn = new ArrayList();
		Object o;
		for (Map.Entry<JSONObject, Long> entry3 : array) {
			denomePulse = entry3.getKey();
			pulseTimestamp = ((Long)entry3.getValue()).longValue();
			try {
				o = DenomeUtils.getDeneWordByIdentity(denomePulse, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


				toReturn.add(new AbstractMap.SimpleEntry<Object,Long>(o, pulseTimestamp));
				Collections.sort(toReturn, new Comparator<Map.Entry<?, Long>>(){
					public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}});

			} catch (InvalidDenomeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return toReturn;
	}

	public ArrayList<Map.Entry<Object, Long>> getDeneWordByPeriod(Identity identity, long startPulseTimestamp, long endPulseTimestamp) {
		if(identity.getDeneWordName()==null || identity.getDeneWordName().equals("")){
			return null;
		}
		ArrayList<Map.Entry<JSONObject, Long>> array = aDBManager.getPulseForRange(startPulseTimestamp,  endPulseTimestamp);	
		JSONObject denomePulse;
		long pulseTimestamp;
		ArrayList<Map.Entry<Object, Long>> toReturn = new ArrayList();
		Object o;
		for (Map.Entry<JSONObject, Long> entry3 : array) {
			denomePulse = entry3.getKey();
			pulseTimestamp = ((Long)entry3.getValue()).longValue();
			try {
				o = DenomeUtils.getDeneWordByIdentity(denomePulse, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


				toReturn.add(new AbstractMap.SimpleEntry<Object,Long>(o, pulseTimestamp));
				Collections.sort(toReturn, new Comparator<Map.Entry<?, Long>>(){
					public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}});

			} catch (InvalidDenomeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return toReturn;
	}
}
