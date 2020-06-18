package com.teleonome.framework.hypothalamus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.teleonome.framework.PowerStatusStruct;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.InvalidMutation;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.utils.StringCompressor;
import com.teleonome.framework.utils.Utils;

public class PulseThread extends Thread{

	/**
	 * 
	 */
	Logger logger;
	private final Hypothalamus anHypothalamus;
	private boolean executedFirstPulseSinceReboot=false;
	private DenomeManager aDenomeManager=null;
	private boolean persistenceOrganismPulses=false;
	private boolean persistencePulse=false;
	BufferedWriter motherOutputStream = null;
	BufferedReader motherInputStream = null;

	public PulseThread(Hypothalamus t){
		anHypothalamus = t;
		logger = Logger.getLogger(getClass());
		aDenomeManager = anHypothalamus.getDenomeManager();
	}

	public boolean isPersistenceOrganismPulses() {
		return persistenceOrganismPulses;
	}

	public boolean isPersistencePulse() {
		return persistencePulse;
	}



	public void run(){
		String denomeIdentityValue;
		BufferedWriter output=null;
		BufferedReader input=null;
		String commandToSend="";
		//
		// are we in the awakening phase of the wps cycle?
		// if so, ask the mother to send data
		
		if(anHypothalamus.motherMicroController!=null) {
			aDenomeManager.storeLifeCycleEvent(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE,System.currentTimeMillis(), TeleonomeConstants.LIFE_CYCLE_EVENT_AWAKE_VALUE);
			try {
				output = anHypothalamus.motherMicroController.getWriter();
				input = anHypothalamus.motherMicroController.getReader();
				logger.info("asking mama for  GetLifeCycleData" );
				 commandToSend = "GetLifeCycleData#1";

				output.write(commandToSend,0,commandToSend.length());
				output.flush();
				logger.debug("line 89 waiting for mama to respond1 wait 5000");
				Thread.sleep(5000);

			} catch (IOException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			
			boolean waitingForMama=true;
			String[] tokens;
			String eventType="";
			long eventTimeMillis=0;
			int eventValue=0;
			while(waitingForMama) {
				logger.debug("line 106");
				try {
					if(input.ready()) {
						String inputLine = "";
						do{
							
							inputLine = input.readLine();
							logger.info("inputLine=" + inputLine);
							
							if(inputLine.length()>5 && !inputLine.startsWith("Ok")&& !inputLine.startsWith("Failure")) {
								logger.info("GetLifeCycleData received inputLine=" + inputLine);
								tokens = inputLine.split("#");
								// find which remembereddenewords come the mother and 
								// storem them using the REMEMBERED_DENEWORD_SOURCE_WPS
								String t0=tokens[0];
								eventTimeMillis = 1000*Long.parseLong(t0);
								eventType = tokens[1];
								eventValue = Integer.parseInt(tokens[2]);
								aDenomeManager.storeLifeCycleEvent(eventType, eventTimeMillis,eventValue);
							}else {
								try {
									logger.info("waiting 2 sec");
									Thread.sleep(2000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							//
						}while(!inputLine.startsWith("Ok") && !inputLine.startsWith("Failure"));


						waitingForMama=false;
					}else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					}

				}catch(IOException e) {
					logger.warn(Utils.getStringException(e));
					logger.info("After erro talking to mama wait 2 sec and try again");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e1));
					}
				}

			}

			String teleonomeName = aDenomeManager.getDenomeName();
			Hashtable<String,ArrayList> deneWordsToRememberByTeleonome = aDenomeManager.getDeneWordsToRememberByTeleonome();

			ArrayList teleonomeRememberedWordsArrayList = deneWordsToRememberByTeleonome.get(teleonomeName);
			logger.debug("for " + teleonomeName + " teleonomeRememberedWordsArrayList: " + teleonomeRememberedWordsArrayList );



			if(teleonomeRememberedWordsArrayList!=null && teleonomeRememberedWordsArrayList.size()>0) {
				//
				// these teleonome does have remembereddenewords
				commandToSend = "GetWPSSensorData#1";
				try {
					output.write(commandToSend,0,commandToSend.length());
					output.flush();
					logger.info("waiting for mama to respond to GetWPSSensorData wait 5000");
					Thread.sleep(5000);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e1));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				
				int adjustedIndex;					
				JSONObject currentlyProcessingSensorValueDene;
				String reportingAddress, unit, valueType,units, sensorValueString;
				waitingForMama=true;
				String[] sensorDataTokens;
				ArrayList<Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex = this.aDenomeManager.getSensorsDeneWordsBySensorRequestQueuePositionByMicroControllerPointer( anHypothalamus.aMotherMicroControllerPointer);
				long wpsRecordTimeMillis=0;

				while(waitingForMama) {
					try {
						if(input.ready()) {
							String inputLine = "";
							logger.debug("line 188");
							do{
								inputLine = input.readLine();
								if(inputLine.length()>5 && !inputLine.startsWith("Ok-")&& !inputLine.startsWith("Failure-")) {
									sensorDataTokens = inputLine.split("#");
									wpsRecordTimeMillis=1000*Long.parseLong(sensorDataTokens[0]);
									logger.info("GetWPSSensorData received inputLine=" + inputLine);
									// find which remembereddenewords come the mother and 
									// ******************
									for (Map.Entry<JSONObject, Integer> entry2 : sensorRequestQueuePositionDeneWordIndex) {
										currentlyProcessingSensorValueDene = entry2.getKey();
										reportingAddress = (String) aDenomeManager.extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,"Reporting Address");
										valueType = (String) aDenomeManager.extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
										units = (String) aDenomeManager.extractDeneWordValueFromDene(currentlyProcessingSensorValueDene,TeleonomeConstants.DENEWORD_UNIT_ATTRIBUTE);

										if(teleonomeRememberedWordsArrayList.contains(reportingAddress)) {
											//
											// note that this is different than in other places where
											// the string is parsed. in ther places we substract 1
											// because token[0] correspond to sensorqueueposition 1
											// however in the case of wps, token0 contains the milliseconds
											adjustedIndex = ((Integer)entry2.getValue()).intValue();
											logger.info("processing sensor token:" + adjustedIndex );   
											sensorValueString = sensorDataTokens[adjustedIndex];
											anHypothalamus.aMnemosyneManager.unwrap( aDenomeManager.getDenomeName()	, wpsRecordTimeMillis, reportingAddress, valueType,sensorValueString, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_WPS, units);			
										}	
									}
								}
								//
							}while(!inputLine.startsWith("Ok") && !inputLine.startsWith("Failure"));
							waitingForMama=false;
					}

				}catch(IOException e) {
					logger.warn(Utils.getStringException(e));
					logger.info("After erro talking to mama wait 2 sec and try again");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}

			}
		} //if(teleonomeRememberedWordsA..
			
			
		//
		// Now the rememberedvalues
		//
			long recordMillis;
			String label,   unit;
			double value;
			try {
				
				logger.info("asking mama for  GetRememberedValueData" );
				commandToSend = "GetRememberedValueData#1";

				output.write(commandToSend,0,commandToSend.length());
				output.flush();
				logger.info("waiting for mama to respond2 wait 5000");
				Thread.sleep(5000);

			} catch (IOException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

			long now = System.currentTimeMillis();
			while(waitingForMama) {
				try {
					if(input.ready()) {
						String inputLine = "";
						do{
							logger.debug("line 265");
							inputLine = input.readLine();
							if(inputLine.length()>5 && !inputLine.startsWith("Ok-")&& !inputLine.startsWith("Failure-")) {
								logger.info("GetRememberedValueData received inputLine=" + inputLine);
								tokens = inputLine.split("#");
								recordMillis = 1000*Long.parseLong(tokens[0]);
								label = tokens[1];
								value = Double.parseDouble(tokens[2]);
								unit=tokens[3];
								aDenomeManager.storeMotherRememberedValue( now,  recordMillis,  label,  value,  unit);
							}
							//
						}while(!inputLine.startsWith("Ok") && !inputLine.startsWith("Failure"));


						waitingForMama=false;
					}

				}catch(IOException e) {
					logger.warn(Utils.getStringException(e));
					logger.info("After erro talking to mama wait 2 sec and try again");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e1));
					}
				}

			}	
			
//			try {
//				if(input!=null)input.close();
//				if(output!=null)output.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}
			
			
			logger.info("abut to create an end awake life cycle event");
		aDenomeManager.storeLifeCycleEvent(TeleonomeConstants.LIFE_CYCLE_EVENT_END_AWAKE, System.currentTimeMillis(),TeleonomeConstants.LIFE_CYCLE_EVENT_AWAKE_VALUE);
	}
	
	
	
	boolean status=false;
	//
	// do the identity check, essentially, check that if the denome says
	// we are in a network mode, that we are actually have a valid ip address
	Identity maxNumRebootsBeforeIdentitySwitchIdentity = new Identity(this.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_IDENTITY_SWITCH_CONTROL_PARAMETERS, TeleonomeConstants.DENEWORD_MAXIMUM_NUMBER_REBOOTS_BEFORE_IDENTITY_SWITCH);
	Identity currentIdentityStateIdentity = new Identity(this.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_TYPE_CURRENT_IDENTITY_MODE);
	Identity currentNumRebootsBeforeIdentitySwitchIdentity = new Identity(this.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_PURPOSE,TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_CURRENT_NUMBER_REBOOTS_BEFORE_IDENTITY_SWITCH);
	Identity identitySwitchEventsMnemosyneDestinationSourceIdentity = new Identity(this.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_IDENTITY_SWITCH_CONTROL_PARAMETERS, TeleonomeConstants.DENEWORD_IDENTITY_SWITCH_EVENTS_MNEMOSYNE_DESTINATION);
	String identitySwitchEventsMnemosyneDestinationIdentityPointer = "";
	Identity setIdentityActuatorComandDeneWordIdentity = new Identity(this.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_ACTUATORS,"Set Identity",TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_TRUE_EXPRESSION);
	logger.debug("setIdentityActuatorComandDeneWordIdentity=" + setIdentityActuatorComandDeneWordIdentity.toString());	

	int maxNumRebootsBeforeIdentitySwitch=0;
	try {
		identitySwitchEventsMnemosyneDestinationIdentityPointer = (String) this.aDenomeManager.getDeneWordAttributeByIdentity(identitySwitchEventsMnemosyneDestinationSourceIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		maxNumRebootsBeforeIdentitySwitch = (int) this.aDenomeManager.getDeneWordAttributeByIdentity(maxNumRebootsBeforeIdentitySwitchIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
	} catch (InvalidDenomeException | JSONException e3) {
		// TODO Auto-generated catch block
		logger.warn(Utils.getStringException(e3));

	}

	//

	try {
		this.aDenomeManager.disallowExternalData();
		denomeIdentityValue = this.aDenomeManager.getInitialIdentityState();
		logger.debug("denomeIdentityValue=" + denomeIdentityValue + " initOperationalMode" + anHypothalamus.initOperationalMode);
		if(!anHypothalamus.initOperationalMode.equals(denomeIdentityValue)){
			//
			// if we are here it means that we are not in the right mode
			// so change the denome to reflect the value of initOperationalMode
			// and add
			logger.debug("Mistmatched Identity Status,initOperationalMode="+  anHypothalamus.initOperationalMode + " denomeIdentityValue=" + denomeIdentityValue);
			//
			// first add a Pathology dene to the Mnemosyne
			//
			String pathologyCause = TeleonomeConstants.PATHOLOGY_NETWORK_STATE_MISTMATCH;
			String pathologyName = TeleonomeConstants.PATHOLOGY_NETWORK_STATE_MISTMATCH;
			String pathologyLocation = TeleonomeConstants.PATHOLOGY_LOCATION_NETWORK;
			JSONObject pathologyDene = new JSONObject();
			pathologyDene.put("Name", pathologyName);
			JSONArray pathologyDeneDeneWords = new JSONArray();
			pathologyDene.put("DeneWords", pathologyDeneDeneWords);

			Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

			JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_MILLISECONDS, "" + cal.getTime().getTime() ,null,"long",true);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);
			pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_TIMESTAMP, anHypothalamus.simpleFormatter.format(cal.getTime()) ,null,"long",true);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);
			pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_DENEWORD_OPERATING_SYSTEM_NETWORK_IDENTITY, anHypothalamus.initOperationalMode ,null,"String",true);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);
			pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_DENEWORD_DENOME_NETWORK_IDENTITY, denomeIdentityValue ,null,"String",true);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);

			pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_CAUSE, pathologyCause ,null,"String",true);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);
			//
			// create the location deneword
			pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_LOCATION, pathologyLocation ,null,TeleonomeConstants.DATATYPE_DENE_POINTER,true);
			pathologyDeneDeneWords.put(pathologyDeneDeneWord);			
			//
			// you need to add a pathology dene to the mnemosyne, but you can not call
			// aDenomeManager.addPathologyDeneToMnemosyne because that method
			// depends on currentCreatingPulse which because this is happening before initializepulse
			// is still null
			Identity mnemosynePathologyDeneChainIdentity = new Identity(this.aDenomeManager.getDenomeName(), TeleonomeConstants.NUCLEI_MNEMOSYNE,TeleonomeConstants.DENECHAIN_MNEMOSYNE_PATHOLOGY);
			logger.debug("mnemosynePathologyDeneChainIdentity=" + mnemosynePathologyDeneChainIdentity.toString());
			this.aDenomeManager.readAndModifyAddDeneToDenChainByIdentity(mnemosynePathologyDeneChainIdentity,pathologyDene);
		}
		String value="";
		if(anHypothalamus.initOperationalMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_ORGANISM)){
			value = "NetworkMode";
		}else{
			value = "HostMode";						
		}
		//
		// Now set the value correctly to make sure the actuator is marked correctly

		this.aDenomeManager.readAndModifyDeneWordByIdentity(setIdentityActuatorComandDeneWordIdentity, value);

	} catch (InvalidDenomeException | JSONException e2) {
		// TODO Auto-generated catch block
		logger.warn(Utils.getStringException(e2));

	}

	//
	// in case the pacemaker crashed and somehow the identityactuator command is not in sync
	// verify that if we are in self, the identituy actuator is set to the correct value
	// as given by

	//
	// loop over every microcontroller
	// for sensors and actuators

	ArrayList<Map.Entry<String, Integer>> microControllerPointerProcessingQueuePositionIndex = this.aDenomeManager.getMicroControllerPointerProcessingQueuePositionIndex();


	//Vector allMicroControllers = new Vector();
	MicroController aMicroController;
	//OutputStreamWriter output;

	long startPulseTimestamp = System.currentTimeMillis();
	String aMicroControllerName;
	String microControllerPointer;

	ArrayList<Entry<JSONObject, Integer>> actuatorExecutionPositionDeneIndex = null;
	ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex;

	//InputStream input;
	//
	// run the microcontroller loop twice
	// the first time to send a message to the microcontroller that the 
	// pulse has begun so as to it not send any commands from the user
	// in the middle of a pulse
	// and the second one to actually execute the commands
	String microprocessorResponse="";



	do{
		try{
			while(anHypothalamus.mutationIsInEffect){
				logger.info("Mutation in Effect");
			}



			//FileUtils.deleteQuietly(new File("KillPulse.info"));
			//FileUtils.deleteQuietly(new File("EndPulse.info"));
			//FileUtils.writeByteArrayToFile(new File("StartPulse.info"), (""+System.currentTimeMillis()).getBytes());

			logger.info("11111aMappedBusThread=" + anHypothalamus.aMappedBusThread);
			if(anHypothalamus.aMappedBusThread!=null){

				anHypothalamus.aMappedBusThread.setKeepRunning(false);
				while( anHypothalamus.aMappedBusThread.isAlive()){
					logger.info("waiting for mapped bus to finish");
					Thread.sleep(500);
				}
			}else {
				logger.info("anHypothalamus.aMappedBusThread is null ");
			}




			//FileUtils.deleteQuietly(new File("PulseProgres"));
			//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("Initializing Pulse 10%").getBytes());
			anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Initializing Pulse " );
			//
			// check if there is a mother, if there is, let it know that we are starting a pulse
			//
			logger.debug("motherMicroController=" + anHypothalamus.motherMicroController);
			if(anHypothalamus.motherMicroController!=null) {
				output = anHypothalamus.motherMicroController.getWriter();//new OutputStreamWriter(serialPort.getOutputStream());
				input = anHypothalamus.motherMicroController.getReader();//new BufferedReader(new InputStreamReader(serialPort.getInputStream()));

				Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

				String now=anHypothalamus.timeFormatter.format(cal.getTime());

				logger.info("telling mama pulse is starting at " +now);
				 commandToSend = "PulseStart#"+now;

				//output.write("GetSensorData",0,"GetSensorData".length());
				output.write(commandToSend,0,commandToSend.length());
				output.flush();
				logger.info("waiting for mama to respond2 wait 5000");
				

				boolean waitingForMama=true;
				do {
					try {
						Thread.sleep(2000);
						
						if(input.ready()) {
							String inputLine = input.readLine();
							logger.info("received 2 inputLine=" + inputLine);
							if(inputLine.equals("Ok-PulseStart")){
								waitingForMama=false;
							}
						}

					}catch(IOException e) {
						logger.warn(Utils.getStringException(e));
						logger.info("After erro talking to mama wait 2 sec and try again");
						Thread.sleep(2000);
					}

				}while(waitingForMama);


				input.close();
				output.close();
			}

			aDenomeManager.storeLifeCycleEvent(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE,System.currentTimeMillis(), TeleonomeConstants.LIFE_CYCLE_EVENT_SYNCHRONOUS_VALUE);

			//
			// check to see if we are schedule to prune the mnemosyne
			logger.debug("performTimePrunningAnalysis=" + anHypothalamus.performTimePrunningAnalysis);

			if(anHypothalamus.performTimePrunningAnalysis) {
				anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Performing Mnemosyne Prunning" );

				anHypothalamus.aMnemosyneManager.performTimePrunningAnalysis();
				anHypothalamus.performTimePrunningAnalysis=false;
			}

			String teleonomeName="";
			logger.warn("initializing pulse");
			startPulseTimestamp = System.currentTimeMillis();
			try {
				teleonomeName = this.aDenomeManager.initializePulse();
			} catch (MissingDenomeException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

			//
			// the first step in the pulse cycle is to check the 
			// thenetwork status, essentially, check to see if 
			// are suppose to be in a network environment
			// but we are disconnected because the teleonome
			// can not contact the network, if this is the case
			// then we need to 
			//1) Add dene to the pathology mnemosyne to indicate disconnection
			//2) get the maximum number of reboot before switching
			// 3) get the current number of reboting before methamoprhosys
			// 4)if the current number== maximum number 
			// 		add a menomsyine pathology dene that indicates metamorphosys event
			//      switch to host mode
			// reboot again

			denomeIdentityValue = (String) this.aDenomeManager.getDeneWordAttributeByIdentity(currentIdentityStateIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			if(denomeIdentityValue.equals(TeleonomeConstants.TELEONOME_IDENTITY_ORGANISM)){
				if(!NetworkUtilities.isNetworkStatusOk()){
					//
					// if we are here is because we are suppose to be in an organism
					// but the network is not reachable so first add a pathology dene to the
					// purpose, since we are in the pulse
					String pathologyCause = TeleonomeConstants.PATHOLOGY_NETWORK_UNAVAILABLE;
					String pathologyName = TeleonomeConstants.PATHOLOGY_NETWORK_UNAVAILABLE;
					String pathologyLocation = TeleonomeConstants.PATHOLOGY_LOCATION_NETWORK;
					Vector extraDeneWords = new Vector();
					Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));
					JSONObject pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_MILLISECONDS, "" + cal.getTime().getTime() ,null,"long",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);
					pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.PATHOLOGY_EVENT_TIMESTAMP, anHypothalamus.simpleFormatter.format(cal.getTime()) ,null,"long",true);
					extraDeneWords.addElement(pathologyDeneDeneWord);

					this.aDenomeManager.addPurposePathologyDene(pathologyName, pathologyCause, pathologyLocation, extraDeneWords);
					//aDenomeManager.addPathologyDeneToMnemosyne(pathologyName, pathologyCause, pathologyLocation, extraDeneWords);
					int currentNumRebootsBeforeMetamorphosis=0;
					try {
						currentNumRebootsBeforeMetamorphosis = (int) this.aDenomeManager.getDeneWordAttributeByIdentity(currentNumRebootsBeforeIdentitySwitchIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					}catch(Exception e) {
						logger.warn(Utils.getStringException(e));
					}
					logger.info("netwok is not reachable,currentNumRebootsBeforeMetamorphosis=" + currentNumRebootsBeforeMetamorphosis +" maxNumRebootsBeforeIdentitySwitch=" + maxNumRebootsBeforeIdentitySwitch);

					if(currentNumRebootsBeforeMetamorphosis<maxNumRebootsBeforeIdentitySwitch){
						//
						// in this case, we need to:
						// 1)increase the value of currentNumRebootsBeforeMetamorphosis 
						// 2)Add a  and event to metamorphosisEventsMnemosyneDestination
						// 3) and reboot
						// but keep the mode as network
						currentNumRebootsBeforeMetamorphosis++;
						logger.info("about to update currentNumRebootsBeforeMetamorphosis=" + currentNumRebootsBeforeMetamorphosis );

						this.aDenomeManager.updateDeneWordCurrentPulse(currentNumRebootsBeforeIdentitySwitchIdentity.toString(), currentNumRebootsBeforeMetamorphosis);
					}else{
						//
						// in this case, we need to :
						// 1)reset the value of currentNumRebootsBeforeMetamorphosis to zero
						// 2)Add a  and event to metamorphosisEventsMnemosyneDestination
						// 3)switch mode to host
						// 4)reboot
						logger.info("netwok is not reachable,and switching to host mode =" + currentNumRebootsBeforeMetamorphosis );

						this.aDenomeManager.updateDeneWordCurrentPulse(currentIdentityStateIdentity.toString(), "HostMode");
						this.aDenomeManager.updateDeneWordCurrentPulse(currentNumRebootsBeforeIdentitySwitchIdentity.toString(), 0);
						logger.info("about to reboot enable host");
						String logFileName="/home/pi/Teleonome/hostmode.log";
						Runtime.getRuntime().exec("sudo sh /home/pi/Teleonome/hostmode.sh " );
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						File file = new File(logFileName);
						while(!file.isFile()){

						}
					}
					//
					// save the denome to disk and reboot
					//
					this.aDenomeManager.addExogenousMetamorphosisEventDeneToMnemosyneDeneChain(new Identity(identitySwitchEventsMnemosyneDestinationIdentityPointer));
					this.aDenomeManager.writeDenomeToDisk();
					Runtime.getRuntime().exec("sudo reboot");

				}
			}
			
				//
				// process the telepathons
				//
				// for each telepathon look here extract its PowerManagerStatus
//				Vector telepathons  = anHypothalamus.aDenomeManager.getTelepathonsByMicroControllerPointerByExecutionPosition(microControllerPointer);
//				logger.debug("Checking for " + telepathons.size()+ " telepathns events coming from  " + aMicroController.getName());
//				if(telepathons.size()>0) {
//					JSONObject telepathon;
//					String messageToSend, inputLine;
//					int counter=0;
//					int expectedFields = PowerStatusStruct.class.getDeclaredFields().length;
//					String telepathonName="";
//					String[] powerStatusTokens;
//					if(anHypothalamus.motherMicroController!=null) {
//						try {
//							for(int k=0;k<telepathons.size();k++) {
//								telepathon=(JSONObject) telepathons.elementAt(k);
//								telepathonName = telepathon.getString(TeleonomeConstants.DENE_NAME_ATTRIBUTE);
//									//									//
//									// send message and process data
//									//
//									do{
//										//logger.debug("sending GetSensorData counter=" + counter);
//										//String commandToSend = "GetSensorData" + counter;
//	
//										logger.debug("sending GetTelepathonPowerStatusData#"+ telepathonName);
//										messageToSend = "GetTelepathonPowerStatusData#"+ telepathonName;
//	
//										//output.write("GetSensorData",0,"GetSensorData".length());
//										output.write(commandToSend,0,commandToSend.length());
//										output.flush();
//										inputLine = input.readLine();
//										logger.debug("received inputLine=" + inputLine);
//										counter++;
//										powerStatusTokens = inputLine.split("#");
//										for(int l=0;l<powerStatusTokens.length;l++) {
//											logger.debug("k=" + k + " powerStatusTokens[k]= "+ powerStatusTokens[l]);
//										}
//										logger.debug("sending GetSensorData received=" + powerStatusTokens.length + " need=" +expectedFields+ "  counter=" + counter + " inputLine=" + inputLine);
//									}while(powerStatusTokens.length!=expectedFields);
//									
//									logger.debug("inputLine.length()>0=" + (inputLine.length()>0));
//									if(inputLine.length()>0){
//										for (Map.Entry<JSONObject, Integer> entry2 : sensorRequestQueuePositionDeneWordIndex) {
//											currentlyProcessingSensorValueDeneJSONObject = entry2.getKey();
//											logger.debug("currentlyProcessingSensorValueDeneJSONObject=" + currentlyProcessingSensorValueDeneJSONObject);
//	
//											adjustedIndex = ((Integer)entry2.getValue()).intValue()-1;
//											logger.debug("processing sensor token:" + adjustedIndex );   
//											sensorValueString = sensorDataTokens[adjustedIndex];
//											logger.debug("processing sensor token:" + adjustedIndex + " resutled in " + sensorValueString);   
//											//
//											// the sensorRequestQueuePosition starts at 1 but the sensorDataTokens start at 0 so
//											// 
//											// logger.debug("inputLIne=" + inputLine);
//											if(sensorValueString!=null && !sensorValueString.equals("")){
//												aDenomeManager.addSensorValueRenderedDeneWordToPulse(currentlyProcessingSensorValueDeneJSONObject, sensorValueString);
//											}
//											//}
//										}
//									}else{
//										logger.debug("microprocessor sent blank sensor data line" + adjustedIndex );
//									}
//									
//									
//									input = aMicroController.getReader();
//									//String inputLine=getInputLine( input);
//									boolean ready = input.ready();
//									logger.debug("line 664 input.ready()=" + ready);
//									
//									if(ready){
//										//   logger.debug("about to call readline");
//										String inputLine = "";
//										
//										do {
//											inputLine = input.readLine();
//											logger.info(aMicroController.getName()  + " was sent=" + messageToSend + " received=" + inputLine);
//											try {
//												Thread.sleep(500);
//											} catch (InterruptedException e) {
//												// TODO Auto-generated catch block
//												e.printStackTrace();
//											}
//											if(inputLine.startsWith("Ok")  || inputLine.startsWith("Failure")) {
//												keepGoing=false;
//											}else {
//												keepGoing=true;
//												//
//												// now process the event by parsing the inputLine 
//												// 
//												//
//												if(inputLine.length()>0){
//													try{
//														processEvent( eventJSONObject , inputLine) ;											
//													}catch (InvalidDenomeException e1) {
//														// TODO Auto-generated catch block
//														logger.warn(Utils.getStringException(e1));
//													}
//												}
//											}
//										}while(keepGoing );
//	
//										input.close();
//										output.close();
//									}
//								
//							}
//						}catch(IOException e) {
//							logger.warn(Utils.getStringException(e));
//						} catch (InvalidDenomeException e2) {
//							// TODO Auto-generated catch block
//							logger.warn(Utils.getStringException(e2));
//						}
//					}
//				}
			//
			// then process the external data
			//
			//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("Processing External Data 10%").getBytes());
			anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Processing External Data");
			logger.debug("about to process external data");
			anHypothalamus.teleonomesToReconnect =	this.aDenomeManager.processExternalData();
			logger.debug("finished processing external data, onto microcontrollers, therer are " + microControllerPointerProcessingQueuePositionIndex.size());


			//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("Processing Sensors 30%").getBytes());

			microprocessorResponse="";
			//				for (Map.Entry<String, Integer> entry : microControllerPointerProcessingQueuePositionIndex) {
			//					microControllerPointer = (String)entry.getKey();
			//					aMicroController = (MicroController)anHypothalamus.microControllerPointerMicroControllerIndex.get(microControllerPointer);
			//					logger.debug("processing microControllerPointer=" + microControllerPointer+ " aMicroController=" +  aMicroController);						
			//
			//					aMicroControllerName = aMicroController.getName();
			//					logger.debug("processing aMicroControllerName=" + aMicroControllerName);						
			//					
			//					//output = aMicroController.getWriter();//new OutputStreamWriter(serialPort.getOutputStream());
			//					//output.write(TeleonomeConstants.PULSE_STATUS_STARTED,0,TeleonomeConstants.PULSE_STATUS_STARTED.length());
			//					//output.flush();
			//					input = aMicroController.getReader();//new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			//					logger.debug("about to wait for microprocessor to respond");						
			//
			//					//microprocessorResponse = input.readLine();
			//					logger.debug("microprocessorResponse=" + microprocessorResponse);						
			//					input.close();
			//				}

			logger.debug("microControllerPointerProcessingQueuePositionIndex=" + microControllerPointerProcessingQueuePositionIndex.size());

			long  startMicroController= System.currentTimeMillis();


			for (Map.Entry<String, Integer> entry : microControllerPointerProcessingQueuePositionIndex) {
				microControllerPointer = (String)entry.getKey();
				logger.debug("line 651   microControllerPointer=" + microControllerPointer);
				aMicroController = (MicroController)anHypothalamus.microControllerPointerMicroControllerIndex.get(microControllerPointer);
				aMicroControllerName = aMicroController.getName();
				logger.debug("line 654 aMicroControllerName=" + aMicroControllerName );
				anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Processing MicroController " + aMicroControllerName);

				output = aMicroController.getWriter();//new OutputStreamWriter(serialPort.getOutputStream());
				input = aMicroController.getReader();//new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
				

				if(!executedFirstPulseSinceReboot){
					//public static final String DENE_TYPE_ON_START_ACTION = "On Start Action";
					//public static final String DENE_TYPE_ON_START_SENSOR = "On Start Sensor";
					sensorRequestQueuePositionDeneWordIndex = this.aDenomeManager.getSensorsDeneWordsBySensorRequestQueuePositionForInitialByMicroControllerPointer( microControllerPointer);
					actuatorExecutionPositionDeneIndex = this.aDenomeManager.getActuatorExecutionPositionDeneForInitialByMicroControllerPointerIndex(microControllerPointer);
					logger.debug("line 1858 processing initial actuatorExecutionPositionDeneIndex=" + actuatorExecutionPositionDeneIndex);
					logger.info("In Initial Sensors and Actions,starting to process MicroController  " + aMicroController.getName());

					processMicroProcessor(aMicroController, teleonomeName, sensorRequestQueuePositionDeneWordIndex, actuatorExecutionPositionDeneIndex, input, output, true);
				}
				sensorRequestQueuePositionDeneWordIndex = this.aDenomeManager.getSensorsDeneWordsBySensorRequestQueuePositionByMicroControllerPointer( microControllerPointer);
				actuatorExecutionPositionDeneIndex = this.aDenomeManager.getActuatorExecutionPositionDeneByMicroControllerPointerIndex(microControllerPointer);
				//logger.debug("line 1871 processing microcontroller normal sensorRequestQueuePositionDeneWordIndex=" + sensorRequestQueuePositionDeneWordIndex + " actuatorExecutionPositionDeneIndex=" + actuatorExecutionPositionDeneIndex);
				logger.info("starting to process MicroController  " + aMicroController.getName());
				logger.debug("line 697 processing microcontroller normal actuatorExecutionPositionDeneIndex=" + actuatorExecutionPositionDeneIndex);
				
				processMicroProcessor(aMicroController, teleonomeName,sensorRequestQueuePositionDeneWordIndex, actuatorExecutionPositionDeneIndex, input, output, false);

				//output.write(TeleonomeConstants.PULSE_STATUS_FINISHED,0,TeleonomeConstants.PULSE_STATUS_FINISHED.length());
				//output.flush();
				//microprocessorResponse = input.readLine();
				input.close();
			}
			executedFirstPulseSinceReboot=true;


			//
			// now process the analyticons
			//
			boolean analyticonsOperonActive=false;
			Identity analyticonsOperonActiveIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_OPERONS,TeleonomeConstants.DENE_OPERON_CONTROL, TeleonomeConstants.DENEWORD_ANALYTICONS_ACTIVE);
			try {
				analyticonsOperonActive = (boolean) this.aDenomeManager.getDeneWordAttributeByIdentity(analyticonsOperonActiveIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			} catch (InvalidDenomeException | JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//
			// read all the denes that need to be evaluated
			//  
			JSONArray analyticonDenesJSONArray = this.aDenomeManager.getAnalyticonDenesJSONArray( );
			logger.debug("analyticonDenesJSONArray=" + analyticonDenesJSONArray);
			anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Starting Anaylticons " );

			JSONObject deneWord;
			Identity analysisProfileIdentity,dataSourceIdentity, reportingAddressIdentity;
			JSONObject analyticonDene, analysisProfileDene, reportingDeneWord;
			String functionName;
			if(analyticonsOperonActive && analyticonDenesJSONArray!=null && analyticonDenesJSONArray.length()>0) {
				for(int i=0;i<analyticonDenesJSONArray.length();i++){
					try {
						analyticonDene = (JSONObject) analyticonDenesJSONArray.getJSONObject(i);
						logger.debug("analyticonDene=" + analyticonDene);
						dataSourceIdentity = new Identity((String)this.aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analyticonDene, TeleonomeConstants.DENEWORD_TYPE_ANALYTICON_DATA_SOURCE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						logger.debug("dataSourceIdentity=" + dataSourceIdentity);
						analysisProfileIdentity = new Identity((String)this.aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analyticonDene, TeleonomeConstants.DENEWORD_TYPE_ANALYTICON_PROFILE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						logger.debug("analysisProfileIdentity=" + analysisProfileIdentity);
						analysisProfileDene = this.aDenomeManager.getDeneByIdentity(analysisProfileIdentity);
						//logger.debug("analysisProfileDene=" + analysisProfileDene);

						reportingAddressIdentity = new Identity((String)this.aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(analyticonDene, "Reporting Address", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
						logger.debug("reportingAddressIdentity=" + reportingAddressIdentity);
						reportingDeneWord = this.aDenomeManager.getDeneWordByIdentity(reportingAddressIdentity);
						//logger.debug("reportingDeneWord=" + reportingDeneWord);
						//if(reportingDeneWord==null){
						///	System.exit(0);
						//}
						functionName = (String) this.aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(analysisProfileDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_PROCESSING_FUNCTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("functionName=" + functionName);
						Object[] parameters =  {dataSourceIdentity,analysisProfileDene};
						logger.debug("parameters=" + parameters);
						Method mnemomsyneMethod = MnemosyneManager.class.getMethod(functionName,Identity.class, JSONObject.class);
						logger.debug("mnemomsyneMethod=" + mnemomsyneMethod);

						anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Executing Anaylticon " + analyticonDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE)+ "  " + (i+1) + " out of "  + analyticonDenesJSONArray.length());

						Object result = mnemomsyneMethod.invoke(anHypothalamus.aMnemosyneManager,parameters);
						logger.debug("result=" + result);
						//
						// now that you have the result add it to the reportingDeneWord
						reportingDeneWord.put(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE, result);
						logger.debug("result after storing =" + reportingDeneWord.toString(4));

					} catch (JSONException e) {
						logger.warn(Utils.getStringException(e));
					} catch (InvalidDenomeException e) {
						logger.warn(Utils.getStringException(e));
					} catch (NoSuchMethodException e) {
						logger.warn(Utils.getStringException(e));
					} catch (SecurityException e) {
						logger.warn(Utils.getStringException(e));
					} catch (IllegalAccessException e) {
						logger.warn(Utils.getStringException(e));
					} catch (IllegalArgumentException e) {
						logger.warn(Utils.getStringException(e));
					} catch (InvocationTargetException e) {
						Throwable ee = e.getCause();
						logger.debug(Utils.getStringException(ee));
					}
				}
			}
			//
			// end of analyticon processing
			//
			//
			// now process the Mnemosycons
			//
			anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Starting Mnemosycons");
			boolean mnemosyconsOperonActive=false;
			Identity mnemosyconsOperonActiveIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_OPERONS,TeleonomeConstants.DENE_OPERON_CONTROL, TeleonomeConstants.DENEWORD_MNEMOSYCONS_ACTIVE);
			try {
				//logger.debug("aDenomeManager=" + this.aDenomeManager + " mnemosyconsOperonActiveIdentity=" + mnemosyconsOperonActiveIdentity);
				mnemosyconsOperonActive = (boolean) this.aDenomeManager.getDeneWordAttributeByIdentity(mnemosyconsOperonActiveIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			} catch (InvalidDenomeException | JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//
			// read all the denes that need to be evaluated
			//  
			JSONArray mnemosyconsDenesJSONArray = this.aDenomeManager.getMnemsyconDenesJSONArray( );
			logger.debug("mnemosyconsnDenesJSONArray=" + mnemosyconsDenesJSONArray);
			Identity mnemosyconProfileIdentity;
			JSONObject mnemosyconDene, mnemosyconProfileDene;
			if(mnemosyconsOperonActive && mnemosyconsDenesJSONArray!=null && mnemosyconsDenesJSONArray.length()>0) {
				for(int i=0;i<mnemosyconsDenesJSONArray.length();i++){
					try {
						mnemosyconDene = (JSONObject) mnemosyconsDenesJSONArray.getJSONObject(i);
						logger.debug("mnemosyconsDene=" + mnemosyconDene);
						if(!(boolean) this.aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconDene, TeleonomeConstants.DENEWORD_ACTIVE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)) {
							String name = (String) this.aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconDene, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.debug("skiping mnemosycon " + name + " because is not Active");
							continue;
						}
						//
						// check to see if there is a profile, if there is not then skip it
						// it means this mnemosycon is a remembered deneword type
						//
						String mnemosyconProfileIdentityPointer = (String)this.aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_PROFILE_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(mnemosyconProfileIdentityPointer==null) {
							String name = (String) this.aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyconDene, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.debug("skiping mnemosycon " + name + " because it does not have a profile");
							continue;
						}
						String mnemosyconType = (String)this.aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_TYPE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("mnemosyconType=" + mnemosyconType);

						mnemosyconProfileIdentity = new Identity(mnemosyconProfileIdentityPointer);
						logger.debug("mnemosyconProfileIdentity=" + mnemosyconProfileIdentity);
						mnemosyconProfileDene = this.aDenomeManager.getDeneByIdentity(mnemosyconProfileIdentity);
						functionName = (String) this.aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconProfileDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_PROCESSING_FUNCTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("functionName=" + functionName);


						Object[] parameters =  {mnemosyconProfileDene, mnemosyconType};
						logger.debug("parameters=" + parameters);

						Method mnemomsyneMethod = MnemosyneManager.class.getMethod(functionName, JSONObject.class, String.class);
						logger.debug("mnemomsyneMethod=" + mnemomsyneMethod);

						anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Executing Mnemosycon " + mnemosyconDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE)+ "  " + (i+1) + " out of "  + mnemosyconsDenesJSONArray.length());

						Object result = mnemomsyneMethod.invoke(anHypothalamus.aMnemosyneManager,parameters);
						logger.debug ("mnemomsyneMethod.invoke result=" + result);
						//
						// now check to see if this mnemsocyon is recurrent or onetime.  if its one time then set Active to false
						//
						// the default is to be recurrent, so a mnemosycon that does not have the "DeneWord Type": "Mnemosycon Remember Recurrence",
						// then is recurrent
						Object recurrentDeneWordObject =  aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(mnemosyconDene, TeleonomeConstants.DENEWORD_ACTIVE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(recurrentDeneWordObject!=null) {
							JSONObject recurrentDeneWordJSONObject = (JSONObject)recurrentDeneWordObject;
							String recurrentValue = recurrentDeneWordJSONObject.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							if(recurrentValue.equals(TeleonomeConstants.MNEMOSYNE_RECURRENCE_ONE_TIME)) {
								Identity id = new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_MNEMOSYCONS, mnemosyconDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE), TeleonomeConstants.DENEWORD_ACTIVE);
								aDenomeManager.updateDeneWordCurrentPulse(id.toString(), false);
							}

						}
					} catch (JSONException e) {
						logger.warn(Utils.getStringException(e));
					} catch (InvalidDenomeException e) {
						logger.warn(Utils.getStringException(e));
					} catch (NoSuchMethodException e) {
						logger.warn(Utils.getStringException(e));
					} catch (SecurityException e) {
						logger.warn(Utils.getStringException(e));
					} catch (IllegalAccessException e) {
						logger.warn(Utils.getStringException(e));
					} catch (IllegalArgumentException e) {
						logger.warn(Utils.getStringException(e));
					} catch (InvocationTargetException e) {
						Throwable ee = e.getCause();
						logger.debug(Utils.getStringException(ee));
					}
				}
			}
			//
			// end of Mnemosycons
			//
			long  totalMicroController= System.currentTimeMillis()-startMicroController;

			//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("Finishing up 90%").getBytes());
			anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Finished Pulse " );

			//FileUtils.deleteQuietly(new File("PulseProgres"));
			//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("Wrapping up 95%").getBytes());
			//logger.debug("Abvout to write EndPulse");

			//FileUtils.deleteQuietly(new File("StartPulse.info"));
			//logger.debug("deleted StartPulse");

			//FileUtils.writeByteArrayToFile(new File(Utils.getLocalDirectory() + "EndPulse.info"), (""+System.currentTimeMillis()).getBytes());
			//logger.debug("wrote EndPulse");



			JSONObject jsonMessage = this.aDenomeManager.generatePulse();
			logger.debug("generated pulse");

			long timestampInMills=0;
			try {
				timestampInMills = jsonMessage.getLong("Pulse Timestamp in Milliseconds");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			String pulse = jsonMessage.toString();
			//logger.debug(pulse);
			long  startStoringAndPublishing= System.currentTimeMillis();
			//
			// notify both buses
			//
			if(anHypothalamus.isExoZeroNetworkActive()) {
				anHypothalamus.exoZeroPublisher.sendMore ("Status");
				logger.debug("published  sendmore to zeromq, about to generate pulse");
				anHypothalamus.exoZeroPublisher.send(pulse); 
				logger.debug("published  pulse to zeromq");
			}

			byte[] pulseBytes = StringCompressor.compress(pulse);
			logger.warn("published  pulse to zeromq, byte size=" + pulseBytes.length);
			//JSONObject purpose = aDenomeManager.getDeneByPointer( TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL);
			//publishToHeart(TeleonomeConstants.HEART_TOPIC_STATUS, purpose.toString());
			anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_STATUS, pulse);
			Identity identity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_PERSIST_PULSE);

			if(aDenomeManager.hasDeneWordByIdentity(identity)) {
				persistencePulse = (boolean) aDenomeManager.getDeneWordAttributeByIdentity(identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("about to store pulse, persistencePulse=" + persistencePulse);  
				if(persistencePulse) {
					this.aDenomeManager.storePulse(timestampInMills,pulse);
				}
			}

			identity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_PERSIST_ORGANISM_PULSE);

			if(aDenomeManager.hasDeneWordByIdentity(identity)) {
				persistenceOrganismPulses = (boolean) aDenomeManager.getDeneWordAttributeByIdentity(identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug(" persistenceOrganismPulses=" + persistenceOrganismPulses);  
			}
			
			//
			// now see if the teleonome should be copied to other location in the drive
			//
			identity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE,TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_PERSIST_PULSE_ADDITIONAL_LOCATIONS_POINTER);

			if(aDenomeManager.hasDeneWordByIdentity(identity)) {
				JSONObject additionalLocationJSNObject =  (JSONObject) aDenomeManager.getDeneWordAttributeByIdentity(identity, TeleonomeConstants.COMPLETE);
				JSONArray additionalLocationsJSONArray = additionalLocationJSNObject.getJSONArray(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE);
				String location;
				File locationDir;
				File otherLocationFile; 
				for(int i=0;i<additionalLocationsJSONArray.length();i++) {
					location = additionalLocationsJSONArray.getString(i);
					if(!location.equals("/home/pi/Teleonome") && !location.equals("/home/pi/Teleonome/tomcat/webapps/ROOT")) {
						locationDir = new File(location);
						if(!locationDir.isDirectory()) {
							locationDir.mkdirs();
						}
						otherLocationFile = new File(location + "/Teleonome.denome"); 
						FileUtils.writeStringToFile(otherLocationFile, pulse, "UTF8");
					}
				}
				persistenceOrganismPulses = (boolean) aDenomeManager.getDeneWordAttributeByIdentity(identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug(" persistenceOrganismPulses=" + persistenceOrganismPulses);  
			}

			anHypothalamus.currentPulseInMilliSeconds = this.aDenomeManager.getCurrentPulseFrequencyMilliseconds();
			long  storingAndPublishing= System.currentTimeMillis()-startStoringAndPublishing;



			long  pulseProduccionDuration= System.currentTimeMillis()-startPulseTimestamp;

			logger.debug("about to finished");  


			//FileUtils.deleteQuietly(new File("PulseProgres"));
			//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("").getBytes());
			logger.warn("finished executing pulse totalMicroController=" + totalMicroController + " storingAndPublishing=" + storingAndPublishing + " pulseProduccionDuration=" + pulseProduccionDuration + " sent pulse about to sleep for " + anHypothalamus.currentPulseInMilliSeconds);
			double availableMemory = Runtime.getRuntime().freeMemory()/1024000;

			System.gc();
			double afterGcMemory = Runtime.getRuntime().freeMemory()/1024000;

			logger.info("Aavailable memory after generating pulse before gc=" + availableMemory + " after gc=" + afterGcMemory);

			//
			//  if there is a mother,  let it know that the pulse is done
			//
			if(anHypothalamus.motherMicroController!=null) {
				motherOutputStream = anHypothalamus.motherMicroController.getWriter();//new OutputStreamWriter(serialPort.getOutputStream());

				 commandToSend="";
				Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));
				logger.info("telling mama pulse is done");

				//String inputLine = motherInputStream.readLine();
				//logger.info("received inputLine=" + inputLine);
				String inputLine="";
				do {
					commandToSend = "PulseFinished#"+anHypothalamus.timeFormatter.format(cal.getTime());
					motherOutputStream.write(commandToSend,0,commandToSend.length());
					motherOutputStream.flush();
					Thread.sleep(3000);
					motherInputStream = anHypothalamus.motherMicroController.getReader();//new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
					try {

						//if(motherInputStream.ready()) {
						inputLine = motherInputStream.readLine();
						logger.info("received inputLine=" + inputLine);
						//}

					}catch(IOException e) {
						logger.warn(Utils.getStringException(e));
						logger.info("After erro talking to mama wait 2 sec and try again");
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}while(!inputLine.equals("Ok-PulseFinished"));


				if(motherInputStream!=null) {
					logger.info("about to close motherInputStream,="+ motherInputStream);	
					motherInputStream.close();
				}

				if(motherOutputStream!=null) {
					logger.info("about to close motheroutputstream");
					motherOutputStream.close();
				}
			}

			aDenomeManager.storeLifeCycleEvent(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE, System.currentTimeMillis(), TeleonomeConstants.LIFE_CYCLE_EVENT_SYNCHRONOUS_VALUE);
			
			logger.info("starting mappbus thread, pulse is going to sleep for " + anHypothalamus.currentPulseInMilliSeconds);  

			anHypothalamus.aMappedBusThread= new MappedBusThread(anHypothalamus);
			anHypothalamus.aMappedBusThread.start();
			Thread.sleep(anHypothalamus.currentPulseInMilliSeconds);
			logger.info("finishing sleep");  

		}catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (PersistenceException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}while(true);
}

void processMicroProcessor(MicroController aMicroController, String teleonomeName, ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex, ArrayList<Entry<JSONObject, Integer>> actuatorExecutionPositionDeneIndex, BufferedReader input, BufferedWriter output, boolean forInitial) throws IOException, InterruptedException{
	JSONObject anActuatorDeneJSONObject;
	String actuatorCommand, pointerToActionSuccessTasks=null, mnemosyneOperationIndexPointer=null;

	int counter=0;

	//
	// get the sensorDataString
	//
	String[] sensorDataTokens;
	String inputLine="";
	TimeLimiter timeLimiter = new SimpleTimeLimiter();
	TimerCallable aTimerCallable;
	int adjustedIndex=0;
	String sensorValueString="";
	JSONObject currentlyProcessingSensorValueDeneJSONObject;
	//
	// the sensorRequestQueuePositionDeneWordIndex could be null
	//
	Identity sensorOperonActiveIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_OPERONS,TeleonomeConstants.DENE_OPERON_CONTROL, TeleonomeConstants.DENEWORD_SENSORS_ACTIVE);
	boolean sensorOperonActive=false;;
	try {
		sensorOperonActive = (boolean) aDenomeManager.getDeneWordAttributeByIdentity(sensorOperonActiveIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
	} catch (InvalidDenomeException | JSONException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	if(sensorRequestQueuePositionDeneWordIndex!=null) {
		for (Map.Entry<JSONObject, Integer> entry2 : sensorRequestQueuePositionDeneWordIndex) {
			currentlyProcessingSensorValueDeneJSONObject = entry2.getKey();
			adjustedIndex = ((Integer)entry2.getValue()).intValue()-1;
			logger.debug("counter=" + counter + " adjustedIndex=" + adjustedIndex + " currentlyProcessingSensorValueDeneJSONObject=" + currentlyProcessingSensorValueDeneJSONObject.getString("Name"));
			counter++;  
		}
	}


	if(sensorOperonActive && sensorRequestQueuePositionDeneWordIndex!=null && sensorRequestQueuePositionDeneWordIndex.size()>0 ){
		do{
			//logger.debug("sending GetSensorData counter=" + counter);
			//String commandToSend = "GetSensorData" + counter;

			logger.debug("sending GetSensorData");
			String commandToSend = "GetSensorData";

			//output.write("GetSensorData",0,"GetSensorData".length());
			output.write(commandToSend,0,commandToSend.length());
			output.flush();
			inputLine = input.readLine();
			logger.debug("received inputLine=" + inputLine);
			counter++;
			sensorDataTokens = inputLine.split("#");
			for(int k=0;k<sensorDataTokens.length;k++) {
				logger.debug("k=" + k + " sensorDataTokens[k]= "+ sensorDataTokens[k]);
			}
			logger.debug("sending GetSensorData received=" + sensorDataTokens.length + " need=" +sensorRequestQueuePositionDeneWordIndex.size() + "  counter=" + counter + " inputLine=" + inputLine);
		}while(sensorDataTokens.length!=sensorRequestQueuePositionDeneWordIndex.size());
		
		logger.debug("inputLine.length()>0=" + (inputLine.length()>0));
		if(inputLine.length()>0){
			for (Map.Entry<JSONObject, Integer> entry2 : sensorRequestQueuePositionDeneWordIndex) {
				currentlyProcessingSensorValueDeneJSONObject = entry2.getKey();
				logger.debug("currentlyProcessingSensorValueDeneJSONObject=" + currentlyProcessingSensorValueDeneJSONObject);

				adjustedIndex = ((Integer)entry2.getValue()).intValue()-1;
				logger.debug("processing sensor token:" + adjustedIndex );   
				sensorValueString = sensorDataTokens[adjustedIndex];
				logger.debug("processing sensor token:" + adjustedIndex + " resutled in " + sensorValueString);   
				//
				// the sensorRequestQueuePosition starts at 1 but the sensorDataTokens start at 0 so
				// 
				// logger.debug("inputLIne=" + inputLine);
				if(sensorValueString!=null && !sensorValueString.equals("")){
					aDenomeManager.addSensorValueRenderedDeneWordToPulse(currentlyProcessingSensorValueDeneJSONObject, sensorValueString);
				}
				//}
			}
		}else{
			logger.debug("microprocessor sent blank sensor data line" + adjustedIndex );
		}
	}
	logger.debug("line 2327 processed sensors,about to do actuators");

	//FileUtils.deleteQuietly(new File("PulseProgres"));
	//FileUtils.writeByteArrayToFile(new File("PulseProgress"), ("Processing actuators 60%").getBytes());
	anHypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Processing Actuators Pulse " );


	//
	// now do the actuators for this microcontrller
	//
	// loop over every microntroller and execute every action of that controller
	//
	ArrayList<Map.Entry<String, JSONArray>> actuatorCommandCodeActionSuccessTaskPointerArrayList;
	ArrayList<Map.Entry<JSONObject, Integer>> actuatorActionEvaluationPositionActionIndex;
	String actionListName="", executionPoint;
	boolean actuatorOperonActive=false;
	Identity actuatorOperonActiveIdentity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_OPERONS,TeleonomeConstants.DENE_OPERON_CONTROL, TeleonomeConstants.DENEWORD_ACTUATORS_ACTIVE);
	try {
		actuatorOperonActive = (boolean) aDenomeManager.getDeneWordAttributeByIdentity(sensorOperonActiveIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
	} catch (InvalidDenomeException | JSONException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	//logger.debug("line 871 actuatorOperonActive=" + actuatorOperonActive + " actuatorExecutionPositionDeneIndex=" + actuatorExecutionPositionDeneIndex);
	if(actuatorOperonActive && actuatorExecutionPositionDeneIndex!=null && actuatorExecutionPositionDeneIndex.size()>0){

		for (Map.Entry<JSONObject, Integer> entry4 : actuatorExecutionPositionDeneIndex) {

			anActuatorDeneJSONObject = entry4.getKey();
			logger.debug("line 2359 anActuatorDeneJSONObject=" + anActuatorDeneJSONObject.getString("Name"));

			try {
				String deneWordOperationPointer="";
				boolean actuatorCommandIsOperation=false;
				//actuatorCommandCodeActionSuccessTaskPointerArrayList = aDenomeManager.evaluateActuatorDene(teleonomeName, anActuatorDeneJSONObject, forInitial);
				//
				// because an action of type evaluatin coould modify the expression of another action, you need to process each 
				// action individually
				//
				actuatorActionEvaluationPositionActionIndex = aDenomeManager.getOrderedActuatorDeneActions(teleonomeName, anActuatorDeneJSONObject, forInitial);
				///////////////////////////////////////////////////////
				JSONObject actuatorActionJSONObject;
				ArrayList arrayList;
				AbstractMap.SimpleEntry<String, JSONArray> entry;
				String codonName;
				//
				// check to see if actuatorActionEvaluationPositionActionIndex is null, because you could have an actuator
				// that only has startup actions and therefore it could be null
				String commandToExecute;
				String payload;
				if(actuatorActionEvaluationPositionActionIndex!=null){
					for (Map.Entry<JSONObject, Integer> action : actuatorActionEvaluationPositionActionIndex) {
						actuatorActionJSONObject = action.getKey();
						entry = aDenomeManager.evaluateAction(teleonomeName, actuatorActionJSONObject);
						//logger.debug(" line 2231 actuatorAction=" + actuatorActionJSONObject.getString("Name") + " returns " + entry);
						//
						// if the action is not active, ie the Active DeneWord is set to false
						//it will return a null, so only add them if they are not null
						if(entry!=null){
							//
							// if we are here is because the action needs to be executed
							// now check to see if the action has an 
							// TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT
							// Deneword
							executionPoint = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE );
							logger.debug(" line 2294 executionPoint=" + executionPoint);
							if(executionPoint==null || executionPoint.equals(TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT_IMMEDIATE)){

								actuatorCommand = entry.getKey();
								actuatorCommandIsOperation=false;
								//
								// check if the command needs to be rendered
								logger.debug("line 2248 before rendering actuatorCommand=" + actuatorCommand);
								if(actuatorCommand.startsWith("@")){
									//
									// if the actuatorCommand is a pointer,it means this action is an evaluation
									//
									deneWordOperationPointer = actuatorCommand;
									actuatorCommandIsOperation=true;
								}else if(actuatorCommand.startsWith("$")){

									if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_IP_ADDRESS_FOR_LCD)){
										actuatorCommand="IPAddr#" + anHypothalamus.primaryIpAddress;
									}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_SSID_FOR_LCD)){

										actuatorCommand="SSID#" + NetworkUtilities.getCurrentConnectedSSiD();
									}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_DO_NOTHING)){
										actuatorCommand=TeleonomeConstants.COMMANDS_DO_NOTHING;
									}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_SET_MICROCONTROLLER_RTC)){
										Calendar cal = Calendar.getInstance();
										actuatorCommand="SetClock#" + cal.get(Calendar.YEAR) + "#" + (cal.get(Calendar.MONTH)+1) + "#" + cal.get(Calendar.DATE) + "#" + cal.get(Calendar.HOUR_OF_DAY) + "#" + cal.get(Calendar.MINUTE) + "#"  + cal.get(Calendar.SECOND);
									}else if(actuatorCommand.equals(TeleonomeConstants.COMMAND_VERIFY_FILE_CREATION_DATE)){
										Calendar cal = Calendar.getInstance();
										actuatorCommand="SetClock#" + cal.get(Calendar.YEAR) + "#" + (cal.get(Calendar.MONTH)+1) + "#" + cal.get(Calendar.DATE) + "#" + cal.get(Calendar.HOUR_OF_DAY) + "#" + cal.get(Calendar.MINUTE) + "#"  + cal.get(Calendar.SECOND);
									}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_GET_LAST_REMEMBERED_DENEWORD_FOR_EACH_TELEONOME)){
										JSONArray lastRememberedWordForEachTeleonome = aDenomeManager.getLastRememberedWordForEachTeleonome();
										//
										// now get the destination which is a deneword in actuatorActionJSONObject
										String destinationIdentityPointer = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(anActuatorDeneJSONObject, TeleonomeConstants.LAST_REMEMBERED_DENEWORD_FOR_EACH_TELEONOME_DESTINATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
										if(destinationIdentityPointer!=null && destinationIdentityPointer.contains("@")) {
											try {
												 aDenomeManager.updateDeneWordCurrentPulse( destinationIdentityPointer, lastRememberedWordForEachTeleonome);
											} catch (InvalidDenomeException e) {
												// TODO Auto-generated catch block
												logger.warn(Utils.getStringException(e));
											}
											
										}
									}else{
										actuatorCommand = Utils.renderCommand(actuatorCommand);
									}
								}
								logger.debug ("line 2389 after rendering actuatorCommand=" + actuatorCommand);
								if(!actuatorCommand.equals("")){
									//
									// there is a command called do nothing which is used 
									// when all we want is to invoke the success tasks
									// because they change data, 
									// this command means dont send anything to the microcontroller
									//
									JSONArray info = entry.getValue();
									JSONObject infoJSONObject;
									for(int i=0;i<info.length();i++) {
										infoJSONObject = info.getJSONObject(i);
										if(infoJSONObject.has(TeleonomeConstants.ACTION_SUCCESS_TASK_LABEL)) {
											pointerToActionSuccessTasks = infoJSONObject.getString(TeleonomeConstants.ACTION_SUCCESS_TASK_LABEL);
										}else {
											if(infoJSONObject.has(TeleonomeConstants.MNEMOSYNE_OPERATION_INDEX_LABEL)) {
												mnemosyneOperationIndexPointer = infoJSONObject.getString(TeleonomeConstants.MNEMOSYNE_OPERATION_INDEX_LABEL);
											}
										}
									}

									logger.debug("login 2292 about to send " + actuatorCommand + " actuatorCommandIsOperation=" + actuatorCommandIsOperation + " pointerToActionSuccessTasks=" + pointerToActionSuccessTasks); 
									if(actuatorCommandIsOperation){
										boolean b = aDenomeManager.evaluateDeneWordOperation(deneWordOperationPointer);
										logger.debug("login 2295 deneWordOperationPointer " + deneWordOperationPointer + " returned =" + b); 
									}else if(!actuatorCommand.equals(TeleonomeConstants.COMMANDS_DO_NOTHING) ){
										logger.debug("line 2297 Actuator Command=" + actuatorCommand);
										output.write(actuatorCommand,0,actuatorCommand.length());
										//serialPortOutputStream.write( actuatorCommand.getBytes() );
										Thread.sleep(3000);
										output.flush();
										counter=0;
										inputLine = input.readLine();
										logger.debug("line 2304 actuator cmmand response=" + inputLine);
									}
									//
									// now that the actuator command has been sent
									// apply the Action Success Task

									if(pointerToActionSuccessTasks!=null){

										logger.debug("line 983 action succests pointer=" + pointerToActionSuccessTasks);
										aDenomeManager.executeActionSuccessTasks(pointerToActionSuccessTasks);
									}
									//
									// now apply the MnemosyneOperations
									//
									if(mnemosyneOperationIndexPointer!=null) {
										//
										// because this method is shared with the mutations
										// it is expecting a JSONArray of the denes already rendered
										// mnemosyneOperationIndexPointer is a pointer to a dene
										//  that contains all the menmosyne operations
										//
										// as an example
										//
										//
										//				                            {
										//				                                "DeneWords": [
										//				                                    {
										//				                                        "DeneWord Type": "Mnemosyne Operation",
										//				                                        "Value": "@Sento:Internal:Actuators:Create Run Completed Dene for Daily",
										//				                                        "Name": "Create Run Completed Dene for Daily",
										//				                                        "Value Type": "Dene Pointer"
										//				                                    },
										//				                                    {
										//				                                        "DeneWord Type": "Mnemosyne Operation",
										//				                                        "Value": "@Sento:Internal:Actuators:Aggregate Total Volume for Daily",
										//				                                        "Name": "Aggregate Total Volume for Daily",
										//				                                        "Value Type": "Dene Pointer"
										//				                                    }
										//				                                ],
										//				                                "Name": "Turn Pump Off Mnemosyne Operations True Expressions"
										//				                            }

										JSONObject mnemosyneOperationsDene, mnemosyneOperationDene;
										String mnemosyneOperationPointer="";
										try {
											mnemosyneOperationsDene = aDenomeManager.getDeneByIdentity(new Identity(mnemosyneOperationIndexPointer));
											logger.debug("mnemosyneOperationIndexPointer=" + mnemosyneOperationIndexPointer);// + " mnemosyneOperationsDene=" + mnemosyneOperationsDene);
											if(mnemosyneOperationsDene!=null ) {
												JSONArray mnemosyneOperationPointers = DenomeUtils.getAllDeneWordsFromDeneByDeneWordType(mnemosyneOperationsDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
												logger.debug("mnemosyneOperationPointers=" + mnemosyneOperationPointers);

												JSONArray mnemosyneDenes = new JSONArray();
												for(int i=0;i<mnemosyneOperationPointers.length();i++) {
													mnemosyneOperationPointer = mnemosyneOperationPointers.getString(i);
													mnemosyneOperationDene = aDenomeManager.getDeneByIdentity(new Identity(mnemosyneOperationPointer));
													mnemosyneDenes.put(mnemosyneOperationDene);
												}
												if(mnemosyneDenes.length()>0) {
													aDenomeManager.executeMnemosyneOperations( mnemosyneDenes);
												}
											}

										} catch (InvalidDenomeException e) {
											// TODO Auto-generated catch block
											logger.warn(Utils.getStringException(e));
										} catch (InvalidMutation e) {
											// TODO Auto-generated catch block
											logger.warn(Utils.getStringException(e));
										}

									}
								}







							}else if(executionPoint!=null || executionPoint.equals(TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT_POST_PULSE)){
								//
								// we need to create a Command that will be executed
								// after the pulse, during the CommandRequests
								//

								String commandCodeType = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionJSONObject,TeleonomeConstants.DENEWORD_CODE_TYPE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE );
								String commandCode="";
								if(commandCodeType.equals(TeleonomeConstants.TELEONOME_SECURITY_CODE)) {
									commandCode = anHypothalamus.motherMicroController.getCommandCode();
								}else if(commandCodeType.equals(TeleonomeConstants.DIGITAL_GEPPETTO_SECURITY_CODE)) {
									commandCode = anHypothalamus.motherMicroController.getDigitalGeppettoCommandCode();
								}
								commandToExecute = entry.getKey();


								payload="";
								boolean restartRequired=false;
								JSONObject responseJSON = anHypothalamus.aDBManager.requestCommandToExecute(commandToExecute,commandCode, commandCodeType, payload, "127.0.0.1", restartRequired);
								logger.info("line 2442, stored post pulse,commandToExecute=" + commandToExecute + " commandCode=" + commandCode + " id=" + responseJSON.getInt("id") );
							}{

							}
						}
					}
				}					






			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

		}
	}
	//
	logger.info("processed MicroController  " + aMicroController.getName());
}

public String getInputLine(InputStream input) throws IOException{
	int inByte = -1;
	StringBuffer buff = new StringBuffer();

	while((inByte = input.read()) != -1) {
		char c = (char)inByte;
		if (c == '\r') {
			continue;
		}
		if (c == '\n') {
			logger.debug("returning from getInputLine=" +  buff.toString());
			return buff.toString();
		}
		buff.append((char)inByte);
	}
	return "";
}

class TimerCallable implements Callable{
	//BufferedReader inputStream;
	InputStream inputStream;
	public TimerCallable (InputStream in){
		inputStream=in;
	}
	public String call() {
		try {
			//return inputStream.readLine();
			return getInputLine( inputStream);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.debug("IOException getting sensor data from arduino");
		}
		return "";
	}
}
}