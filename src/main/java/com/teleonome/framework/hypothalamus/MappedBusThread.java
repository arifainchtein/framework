package com.teleonome.framework.hypothalamus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.denome.MutationIdentity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.InvalidMutation;
import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.utils.Utils;

class MappedBusThread extends Thread{
		/**
		 * 
		 */
		private final Hypothalamus hypothalamus;
		String busId="PulseGenerator";
		boolean waitingForConfirmReboot=false;
		boolean waitingForConfirmShutdown=false;
		boolean waitingForConfirmKillPulse=false;
		boolean keepRunning=true;
		BufferedReader input=null;
		BufferedWriter output=null;
		Logger logger=null;
		JSONObject lastPulseJSONObject;
		public MappedBusThread(Hypothalamus h){
			hypothalamus = h;
			keepRunning=true;
			logger = logger.getLogger(getClass());
		}

		public void setKeepRunning(boolean b){
			if(b)logger.info("Async Cycle requested to start");
			else logger.info("Async Cycle requested to stop");
			keepRunning=b;
			waitingForConfirmReboot=false;
			waitingForConfirmShutdown=false;
		}



		public void run(){
			MicroController aMicroController=null;
			String mutationFileName="";
			String  dataPayload = null;
			//Vector commandsToExecuteVector;

			//OutputStreamWriter output=null;
			
			/*
			try {
				output = new OutputStreamWriter(serialPort.getOutputStream());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			 */
			String command=null;
			String commandCode="";
			String message=null;
			byte[] buffer = new byte[512];
			String[] tokens;
			String microControllerPointer;
			CommandRequest aCommandRequest=null ;
			JSONObject dataPayloadJSONObject = null;
			String motherCommandCode=null;
			boolean goodCommandCode=false;
			String clientIp;
			//
			// Before going into the loop, processed this teleonome's remembered words
			//
			try {
				logger.debug("about to do rememberedwords for teleonome");
				processRememberedWords();
			} catch (InvalidDenomeException e2) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e2));
			}
			boolean commandCodeVerified;
			
			while(keepRunning){
				
				motherCommandCode=null;
				command=null;
				commandCode="";
				goodCommandCode=false;
				//
				// get the command from the database
				//
				aCommandRequest = hypothalamus.aDenomeManager.getNextCommandToExecute();
				dataPayloadJSONObject = null;
				clientIp="127.0.0.1";
				if(aCommandRequest!=null){
					command = aCommandRequest.getCommand();
					commandCode = aCommandRequest.getCommandCode();
					logger.info("commandCode=" + commandCode);
					clientIp = aCommandRequest.getClientIp();
					commandCodeVerified=false;
					try {
						commandCodeVerified = hypothalamus.motherMicroController.verifyUserCommandCode(commandCode);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e1));
					}
					 
					if(commandCodeVerified) {
						dataPayload = aCommandRequest.getDataPayload();
						logger.info("Executing command " + command  + " with dataPayload=" + dataPayload);
						goodCommandCode=true;
						if(dataPayload!=null && !dataPayload.equals("")){
							try {
								dataPayloadJSONObject = new JSONObject(dataPayload);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								logger.warn(Utils.getStringException(e));
							}
						}
					}else {
						//
						// if we are here, the user code was wron
						// from the mother, or an invalid code from the originator
						//
						JSONObject commandResponseJSONObject = hypothalamus.aDenomeManager.markCommandAsBadCommandCode(aCommandRequest.getId(), TeleonomeConstants.COMMAND_REQUEST_INVALID_CODE);
						hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_RESPONSE, commandResponseJSONObject.toString());
						logger.debug("COMMANDS CODE DO NOT MATCH commandResponseJSONObject=" + commandResponseJSONObject.toString(4));
						commandCode=null;
					}
					
					
					
					
					
				}
				
				
				
				String asyncData="AsyncData";
				logger.debug("line 105 microControllerPointerMicroControllerIndex=" + hypothalamus.microControllerPointerMicroControllerIndex);
				long asyncRequestDelayMillis=0;
				for(Enumeration en=hypothalamus.microControllerPointerMicroControllerIndex.keys();en.hasMoreElements();){
					microControllerPointer = (String)en.nextElement();
					aMicroController = (MicroController)hypothalamus.microControllerPointerMicroControllerIndex.get(microControllerPointer);
					asyncRequestDelayMillis = aMicroController.getAsyncRequestMillisecondsDelay();
					logger.debug("AsyncCycle is processing " + aMicroController.getName());
					if(aMicroController.isEnableAsyncUpdate()) {
						try {
							output = aMicroController.getWriter();
							logger.debug("requesting asyncdata");
							output.write(asyncData,0,asyncData.length());
							output.flush();

							input = aMicroController.getReader();
							//String inputLine=getInputLine( input);
							boolean ready = input.ready();
							logger.debug("line 114 input.ready()=" + ready);

							if(ready){
								//   logger.debug("about to call readline");
								String inputLine = "";
								do {
									inputLine = input.readLine();
									logger.info("received inputLine=" + inputLine);
									try {
										Thread.sleep(500);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}while(!inputLine.startsWith("Ok") 
										&& !inputLine.startsWith("Command") 
										&& !inputLine.startsWith("Fault")
										&& !inputLine.startsWith(TeleonomeConstants.HEART_TOPIC_ASYNC_CYCLE_UPDATE)
										&& !inputLine.startsWith(TeleonomeConstants.COMMAND_SHUTDOWN)
										&& !inputLine.startsWith(TeleonomeConstants.COMMAND_REBOOT)
										);

								input.close();
								output.close();

								//if(!inputLine.equals("")){
								if(inputLine.equals(TeleonomeConstants.COMMAND_REBOOT)){
									//if(waitingForConfirmReboot){
										output = aMicroController.getWriter();
										output.write(TeleonomeConstants.COMMAND_REBOOTING);
										output.flush();
										logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
										Runtime.getRuntime().exec("sudo reboot");
	
										waitingForConfirmReboot=false;
									//}else{
//									logger.debug("not waitingForConfirmReboot =" + inputLine);
//
//									output = aMicroController.getWriter();
//									output.write(TeleonomeConstants.COMMAND_CONFIRM_REBOOT);
//									output.flush();
//									waitingForConfirmReboot=true;
//									waitingForConfirmShutdown=false;
//									waitingForConfirmKillPulse=false;
									//								}
								}else if(inputLine.equals(TeleonomeConstants.COMMAND_SHUTDOWN)){
									//								if(waitingForConfirmShutdown){
									output = aMicroController.getWriter();
									output.write(TeleonomeConstants.COMMAND_SHUTINGDOWN);
									output.flush();
									logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
									Runtime.getRuntime().exec("sudo shutdown -h now");

									waitingForConfirmShutdown=false;
									//								}else{
									//									output = aMicroController.getWriter();
									//									output.write(TeleonomeConstants.COMMAND_CONFIRM_SHUTDOWN);
									//									output.flush();
									//									waitingForConfirmShutdown=true;
									//									waitingForConfirmReboot=false;
									//									waitingForConfirmKillPulse=false;
									//								}
								}else if(inputLine.equals(TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_HOST)){
									output = aMicroController.getWriter();
									output.write(TeleonomeConstants.COMMAND_SHUTINGDOWN);
									output.flush();
									logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
									try {
										Utils.executeCommand("sudo sh hostmode.sh");
										Utils.executeCommand("sudo shutdown -h now");
										//Runtime.getRuntime().exec("sudo shutdown -h now");

										waitingForConfirmShutdown=false;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}


								}else if(inputLine.equals(TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_NETWORK)){
									output = aMicroController.getWriter();
									output.write(TeleonomeConstants.COMMAND_SHUTINGDOWN);
									output.flush();
									logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
									try {
										Utils.executeCommand("sudo sh networkmode.sh");
										Utils.executeCommand("sudo shutdown -h now");
										//Runtime.getRuntime().exec("sudo shutdown -h now");

										waitingForConfirmShutdown=false;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}


								}else if(inputLine.equals(TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK)){
									//								output = aMicroController.getWriter();
									//								output.write(TeleonomeConstants.COMMAND_REBOOTING);
									//								output.flush();
									//								
									logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
									try {
										Utils.executeCommand("sudo sh networkmode.sh");
										Utils.executeCommand("sudo rebooot");

										waitingForConfirmShutdown=false;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}


								}else if(inputLine.equals(TeleonomeConstants.COMMAND_REBOOT_ENABLE_HOST)){
									//								output = aMicroController.getWriter();
									//								output.write(TeleonomeConstants.COMMAND_REBOOTING);
									//								output.flush();
									logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
									try {
										Utils.executeCommand("sudo sh hostmode.sh");
										Utils.executeCommand("sudo rebooot");

										waitingForConfirmShutdown=false;
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}


								}else if(inputLine.equals(TeleonomeConstants.COMMAND_KILL_PULSE)){
									//	if(waitingForConfirmKillPulse){
									//									output = aMicroController.getWriter();
									//									output.write(TeleonomeConstants.COMMAND_STOPPING_PULSE);
									//									output.flush();
									logger.debug("receive from microcontrolle a pushbutton r =" + inputLine);
									System.exit(0);

									waitingForConfirmKillPulse=false;
									//								}else{
									//									output = aMicroController.getWriter();
									//									output.write(TeleonomeConstants.COMMAND_CONFIRM_STOP_PULSE);
									//									output.flush();
									//									waitingForConfirmShutdown=false;
									//									waitingForConfirmReboot=false;
									//									waitingForConfirmKillPulse=true;
									//								}
								}else if(inputLine.startsWith(TeleonomeConstants.HEART_TOPIC_ASYNC_CYCLE_UPDATE)){
									logger.info("receive AsynC Update  from " + microControllerPointer + " inputLine="+ inputLine);
									ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex = hypothalamus.aDenomeManager.getSensorsDeneWordsBySensorRequestQueuePositionByMicroControllerPointer( microControllerPointer);
									JSONObject currentlyProcessingSensorValueDeneJSONObject;
									int adjustedIndex;
									String sensorValueString="";
									logger.debug("inputLine.substring(17)=" + inputLine.substring(17));
									String[] sensorDataTokens = inputLine.substring(17).split("#");
									String reportingAddress;
									JSONObject dataToPublishJSONObject = new JSONObject();
									if(sensorRequestQueuePositionDeneWordIndex!=null && sensorDataTokens!=null && sensorDataTokens.length>0) {
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
												reportingAddress = (String)  hypothalamus.aDenomeManager.extractDeneWordValueFromDene(currentlyProcessingSensorValueDeneJSONObject,"Reporting Address");
												try{
													Double parseValue = Double.parseDouble(sensorValueString.trim());
													dataToPublishJSONObject.put(reportingAddress, parseValue);
												}catch(NumberFormatException e){
													logger.debug(inputLine + " is not numeric");
												}
											}
											//}
										}
									}
									logger.debug("about to send asyn update to the herarrt " + dataToPublishJSONObject.toString());   
									hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_ASYNC_CYCLE_UPDATE, dataToPublishJSONObject.toString());



								}else if(inputLine.startsWith(TeleonomeConstants.COMMAND_FAULT)){
									//
									// a fault is a tokenized string separated with #
									// token 0 is the word fault
									// token 1 is the type f mutation
									// token 2 is the name of the mutation to be executed
									// token 3 is the commandCode for the command about the fault
									// token 4 is the payload for the command about the fault
									
									
									tokens = inputLine.split("#");
									String mutationType=tokens[1];
									command=tokens[2];
									//password = tokens[3];
									commandCode = tokens[3];
									clientIp="127.0.0.1";
									String faultDataTarget = tokens[4];
									String faultData = "";
									if(tokens.length>5)faultData=tokens[5];
									logger.info("line 546 faultDataTarget=" + faultDataTarget + " faultData="+ faultData);

									hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, inputLine);

									//
									// now create a pathology dene
									//

									String pathologyCause = mutationType;
									String pathologyName = TeleonomeConstants.PATHOLOGY_DENE_MICROCONTROLLER_FAULT;
									String pathologyLocation = new Identity(hypothalamus.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_SENSORS,TeleonomeConstants.PATHOLOGY_LOCATION_MICROCONTROLLER  ).toString();
									Vector extraDeneWords = new Vector();

									JSONObject pathologyDeneDeneWord;
									try {
										pathologyDeneDeneWord = Utils.createDeneWordJSONObject(TeleonomeConstants.SENSOR_VALUE_RANGE_MAXIMUM, ""+faultData ,null,"double",true);
										extraDeneWords.addElement(pathologyDeneDeneWord);
									} catch (JSONException e) {
										// TODO Auto-generated catch block
										logger.warn(Utils.getStringException(e));
									}

									logger.debug("Created pathology dene");

									dataPayloadJSONObject = new JSONObject();

									JSONObject payLoadJSONObject = new JSONObject();
									try {
										dataPayloadJSONObject.put("Mutation Name",command);

										dataPayloadJSONObject.put("Payload", payLoadJSONObject);
										JSONArray updatesArray = new JSONArray();
										payLoadJSONObject.put("Updates"	, updatesArray);

										JSONObject updateJSONObject =  new JSONObject();
										updateJSONObject.put("Target",faultDataTarget);
										updateJSONObject.put("Value" ,mutationType + ":" + faultData);
										updatesArray.put(updateJSONObject);




									} catch (JSONException e) {
										// TODO Auto-generated catch block
										logger.warn(Utils.getStringException(e));
									}

									//
									// create the commandRequest
									//
									JSONObject commandRequestJSONObject = hypothalamus.aDBManager.requestCommandToExecute(command,commandCode, dataPayloadJSONObject.toString(), clientIp);	
									logger.debug("Received Fault, mutationType:" + mutationType + " command:" + command + " commandRquestId=" + commandRequestJSONObject.getInt("id"));;


									try {
										hypothalamus.aDenomeManager.addFaultPathologyDene(microControllerPointer,pathologyName,  pathologyCause,  pathologyLocation,  extraDeneWords);
									} catch (JSONException e) {
										// TODO Auto-generated catch block
										logger.warn(Utils.getStringException(e));
									}





								}else if(inputLine.startsWith(TeleonomeConstants.TIMER_FINISHED)){
									//
									// token 0 is the word TimerFinished
									// token 1 is the name of the mutation to be executed
									// token 2 is the commandCode
									// token 3 is the econds the timer run for the command about the fault
									logger.debug("line 612 inputLine=" + inputLine);

									tokens = inputLine.split("#");
									command=tokens[1];
									command=tokens[2];
									int secondsRunning = Integer.parseInt(tokens[3]);
									String extraData = tokens[4];
									String updatetDataTarget = tokens[5];
									logger.debug("line 617 command=" + command + " secondsRunning=" + secondsRunning + " extraData="+ extraData + " updatetDataTarget=" + updatetDataTarget);



									dataPayloadJSONObject = new JSONObject();

									JSONObject payLoadJSONObject = new JSONObject();
									try {
										dataPayloadJSONObject.put("Mutation Name",command);

										dataPayloadJSONObject.put("Payload", payLoadJSONObject);
										JSONArray updatesArray = new JSONArray();
										payLoadJSONObject.put("Updates"	, updatesArray);

										JSONObject updateJSONObject =  new JSONObject();
										updateJSONObject.put("Target",updatetDataTarget);
										updateJSONObject.put("Value" ,  "Timer Finished:" + secondsRunning + " seconds");
										updatesArray.put(updateJSONObject);




									} catch (JSONException e) {
										// TODO Auto-generated catch block
										Utils.getStringException(e);

									}

									//
									// create the commandRequest
									//
									JSONObject commandRequestJSONObject = hypothalamus.aDBManager.requestCommandToExecute(command,commandCode, dataPayloadJSONObject.toString(), clientIp);	
									logger.debug("line 650 Received Timer Ended,  command:" + command + " commandRquestId=" + commandRequestJSONObject.getInt("id"));
								}else {
									logger.debug("input line not recognized");
								}
								if(input!=null)input.close();
								if(output!=null)output.close();


							}else {
								logger.debug("Closing input because is not ready");
								if(input!=null)input.close();
								if(output!=null)output.close();
							}
							if(asyncRequestDelayMillis>0) {
								try {
									Thread.sleep(asyncRequestDelayMillis);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									logger.warn(Utils.getStringException(e));
								}
							}
						} catch (IOException e) {
							logger.warn("IOException processing " + aMicroController.getName());	
						}
					}
				}
				
				//catch (EOFException e) {
				// TODO Auto-generated catch block
				//	logger.warn(Utils.getStringException(e));
				//}
				//
				
				if(aCommandRequest!=null && command!=null && !command.equals("")  && goodCommandCode){
					logger.debug("line 674about to execute aCommandRequest=" + aCommandRequest + " command " + command + " dataPayloadJSONObject=" + dataPayloadJSONObject);
						//executeCommand( aMicroController, input,  output,  command,  aCommandRequest,  dataPayloadJSONObject );
					executeCommand(  command,  aCommandRequest,  dataPayloadJSONObject );
				
					hypothalamus.mutationIsInEffect=false;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			
			logger.debug("existing run method of mapbusthread");
			try {
				if(input!=null)input.close();
				if(output!=null)output.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
			public void executeCommand( String command, CommandRequest aCommandRequest, JSONObject dataPayloadJSONObject ){
			try {
				
				logger.debug("pulse is executing command1=" + command );

				if(command.equals(TeleonomeConstants.COMMAND_SHUTDOWN) || command.equals(TeleonomeConstants.COMMAND_SHUTDOWN_TEXT)){
					logger.debug("receive from command shuttind down");
					boolean shutdownOk = hypothalamus.motherMicroController.shuttingDownHypothalamus();
					logger.debug("receive from command shuttind down, shutdownOk=" + shutdownOk);
					if(shutdownOk) {
						JSONObject commandResponseJSONObject = hypothalamus.aDenomeManager.markCommandCompleted(aCommandRequest.getId());
						//
						// now check to see if the action that executes the shutdown command
						// that should have an identity of @Egg:Internal:Actuators:Shutdown:Active
						// should be set to false, so is not active on next startup
						Identity shutdownActionIdentity = new Identity(hypothalamus.aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_ACTUATORS,TeleonomeConstants.SHUTDOWN_ACTION,TeleonomeConstants.DENEWORD_ACTIVE);
						try {
							hypothalamus.aDenomeManager.readAndModifyDeneWordByIdentity(shutdownActionIdentity, false);
						} catch (JSONException | InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						
						hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_RESPONSE, commandResponseJSONObject.toString());
						//
						// wait a couple of seconds to make sure the heart does its job
						//
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						
						Runtime.getRuntime().exec("sudo shutdown -h now");
					}else {
						JSONObject commandResponseJSONObject = hypothalamus.aDenomeManager.markCommandAsBadCommandCode(aCommandRequest.getId(),TeleonomeConstants.MOTHER_INVALIDATED_SHUTDOWN);
						hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_RESPONSE, commandResponseJSONObject.toString());
					}
					
					
					
					


				}else if(command.equals(TeleonomeConstants.COMMAND_REBOOT) || command.equals(TeleonomeConstants.COMMAND_REBOOT_TEXT)){
					logger.debug("receive from command reboot");
					boolean rebootOk = hypothalamus.motherMicroController.rebootingHypothalamus();
					if(rebootOk) {
						JSONObject commandResponseJSONObject = hypothalamus.aDenomeManager.markCommandCompleted(aCommandRequest.getId());
						hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_RESPONSE, commandResponseJSONObject.toString());
						//
						// wait a couple of seconds to make sure the heart does its job
						//
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						Runtime.getRuntime().exec("sudo reboot");
					}else {
						JSONObject commandResponseJSONObject = hypothalamus.aDenomeManager.markCommandAsBadCommandCode(aCommandRequest.getId(),TeleonomeConstants.MOTHER_INVALIDATED_REBOOT);
						hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_RESPONSE, commandResponseJSONObject.toString());
					}
					


				}else if(command.equals(TeleonomeConstants.COMMAND_KILL_PULSE)){

					logger.debug("receive command to kill pulse ");
					hypothalamus.aDenomeManager.markCommandCompleted(aCommandRequest.getId());
					System.exit(0);

				}

				logger.debug("line 712 command=" + command);
				if(!command.equals(TeleonomeConstants.COMMAND_QUEUE_EMPTY) ){
					//
					// if there is a payload, apply it to the mutation
					// re save the denome
					// so that when it is executed below, the mutation to be executed 
					// will have the updated data
					JSONObject  mutationJSONObject=null;
					logger.debug("line 720 dataPayloadJSONObject=" + dataPayloadJSONObject);
					
					if(dataPayloadJSONObject!=null){
						try {
							mutationJSONObject = hypothalamus.aDenomeManager.injectPayloadIntoStateMutation(dataPayloadJSONObject);
							logger.debug("line 725 mutationJSONObject=" + mutationJSONObject);
							
						} catch (InvalidMutation e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}

					}

					//
					// flag that we are starting a mutation
					hypothalamus.mutationIsInEffect=true;



					//
					// execute the command
					//

					JSONObject actuatorLogicProcessingDene=null;
					ArrayList arrayList;
					JSONArray actuatorLogicProcessingDeneJSONArray = new JSONArray();
					//JSONObject mutationAttributeJSONObject=null;
					try {
						//mutationJSONObject = aDenomeManager.readImmediateMutation(mutationFileName);
						logger.info("about to read " + command);

						if(command.equals(TeleonomeConstants.COMMAND_REBOOT_ENABLE_HOST) || 
							command.equals(TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_HOST)
						){
							if(dataPayloadJSONObject==null)mutationJSONObject = hypothalamus.aDenomeManager.readImmediateMutation("SetHostMode");
							
						}else if(command.equals(TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK) || 
								command.equals(TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_NETWORK)
								){
									if(dataPayloadJSONObject==null) {
										mutationJSONObject = hypothalamus.aDenomeManager.readImmediateMutation("SetNetworkMode");
									}
									
						}else{
							if(dataPayloadJSONObject==null)mutationJSONObject = hypothalamus.aDenomeManager.readImmediateMutation(command);
						}
						
						
						
						logger.info("mutationJSONObject " + mutationJSONObject);
						String actuatorCommand="";
						if(mutationJSONObject!=null){
							hypothalamus.executeMutation(mutationJSONObject);
						}
					} catch (InvalidMutation e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					} catch (InvalidDenomeException e1) {
						// TODO Auto-generated catch block
						logger.debug(Utils.getStringException(e1));
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
					
					//
					// which means that whatever Denome editing has already ocurred
					// therefore save it to disk so that the webserver can read it
					//
					hypothalamus.aDenomeManager.writeDenomeToDisk();
					//
					// now store the mutation in the database
					// as a MutationEvent
					//
					if(mutationJSONObject!=null){
						try {
							JSONObject mutationEventJSONObject = hypothalamus.aDenomeManager.createMutationEventFromMutation( mutationJSONObject, actuatorLogicProcessingDeneJSONArray);
							hypothalamus.aDenomeManager.storeMutationEvent(mutationEventJSONObject);
						} catch (PersistenceException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					}
					//
					// and update the database
					logger.debug("about to mark commandrequest as cmpleted, " + aCommandRequest.getId());
					JSONObject commandResponseJSONObject = hypothalamus.aDenomeManager.markCommandCompleted(aCommandRequest.getId());
					hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_UPDATE_FORM_RESPONSE, commandResponseJSONObject.toString());
					//
					// finally, if the comand was to reboot or shutdown
					// changing mode, execute the reboot or shutdown
					//
					logger.debug("line 658, command=" + command);
					if(command.equals(TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_HOST)){
						String logFileName="/home/pi/Teleonome/hostmode.log";
						
						Runtime.getRuntime().exec("sudo sh /home/pi/Teleonome/hostmode.sh ");
						
						File file = new File(logFileName);
						while(!file.isFile()){
							
						}
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						Runtime.getRuntime().exec("sudo shutdown -h now");
					}else if(command.equals(TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_NETWORK)){
						
						try {
							
							
							
							String teleonomeName = hypothalamus.aDenomeManager.getDenomeName();
							MutationIdentity identity = new MutationIdentity(teleonomeName,"SetNetworkMode", "On Load", "Update SSID", "Update SSID");
							String ssid = (String) DenomeUtils.getDeneWordAttributeFromMutationByMutationIdentity(mutationJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							identity = new MutationIdentity(teleonomeName,"SetNetworkMode", "On Load", "Update PSK", "Update PSK");
							String password = (String) DenomeUtils.getDeneWordAttributeFromMutationByMutationIdentity(mutationJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							NetworkUtilities.createNetworkSupplicant(ssid, password);
							//
							// now that the supplicant has been created, update the
							// command so that the password is not visible
							JSONObject payload = new JSONObject(aCommandRequest.getDataPayload());
							
							JSONObject payloadJSONObject = payload.getJSONObject("Payload");
							JSONArray updatesJSONArray = payloadJSONObject.getJSONArray("Updates");
							logger.debug("line 698 of inject, payloadJSONObject=" + payloadJSONObject);

							JSONObject updateJSNObject;
							String updateTargetPointer;
							Object updateTargetValue;
							
							for(int j=0;j<updatesJSONArray.length();j++){
								updateJSNObject = updatesJSONArray.getJSONObject(j);
								//
								// each update object has two parameters, the target and the value
								//
								updateTargetPointer = updateJSNObject.getString("Target");
								updateTargetValue = updateJSNObject.get("Value");
								if(updateTargetPointer.equals("@On Load:Update PSK:Update PSK")) {
									updateJSNObject.put("Value", "*");
								}
							}
							hypothalamus.aDenomeManager. offuscateWifiPasswordInCommand(aCommandRequest.getId(), payload.toString());
							
							String logFileName="/home/pi/Teleonome/networkmode.log";
							
							Runtime.getRuntime().exec("sudo sh /home/pi/Teleonome/networkmode.sh " );
							File file = new File(logFileName);
							while(!file.isFile()){
								
							}
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Runtime.getRuntime().exec("sudo shutdown -h now");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
							
						}catch (InvalidMutation | InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						
						
					}else if(command.equals(TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK)){
						try {
							
							String teleonomeName = hypothalamus.aDenomeManager.getDenomeName();
							MutationIdentity identity = new MutationIdentity(teleonomeName,"SetNetworkMode", "On Load", "Update SSID", "Update SSID");
							String ssid = (String) DenomeUtils.getDeneWordAttributeFromMutationByMutationIdentity(mutationJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							identity = new MutationIdentity(teleonomeName,"SetNetworkMode", "On Load", "Update PSK", "Update PSK");
							String password = (String) DenomeUtils.getDeneWordAttributeFromMutationByMutationIdentity(mutationJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.debug("about to create supplicant with " + ssid + " " + password);
							NetworkUtilities.createNetworkSupplicant(ssid, password);
							String logFileName="/home/pi/Teleonome/networkmode.log";
							
							//
							// now that the supplicant has been created, update the
							// command so that the password is not visible
							JSONObject payload = new JSONObject(aCommandRequest.getDataPayload());
							
							JSONObject payloadJSONObject = payload.getJSONObject("Payload");
							JSONArray updatesJSONArray = payloadJSONObject.getJSONArray("Updates");
							logger.debug("line 758 of inject, payloadJSONObject=" + payloadJSONObject);

							JSONObject updateJSNObject;
							String updateTargetPointer;
							Object updateTargetValue;
							
							for(int j=0;j<updatesJSONArray.length();j++){
								updateJSNObject = updatesJSONArray.getJSONObject(j);
								//
								// each update object has two parameters, the target and the value
								//
								updateTargetPointer = updateJSNObject.getString("Target");
								updateTargetValue = updateJSNObject.get("Value");
								if(updateTargetPointer.equals("@On Load:Update PSK:Update PSK")) {
									updateJSNObject.put("Value", "*");
								}
							}
							hypothalamus.aDenomeManager. offuscateWifiPasswordInCommand(aCommandRequest.getId(), payload.toString());
							
							
							Runtime.getRuntime().exec("sudo sh /home/pi/Teleonome/networkmode.sh  " );
							File file = new File(logFileName);
							while(!file.isFile()){
								
							}
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Runtime.getRuntime().exec("sudo reboot");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
							
						}catch (InvalidMutation | InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					}else if(command.equals(TeleonomeConstants.COMMAND_REBOOT_ENABLE_HOST)){
						logger.debug("about to reboot ena=ble host");
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
						
						Runtime.getRuntime().exec("sudo reboot");
					}
				}

			} catch (ConnectException e) {
				// TODO Auto-generated catch block
				logger.debug("The Command Server is not responding");
				hypothalamus.mutationIsInEffect=false;
				try {
					//logger.debug("about to sleep, running=" + running);
					Thread.sleep(10000);
				} catch (InterruptedException qe) {
					// TODO Auto-generated catch block
					logger.debug("Interrupted Sleep of the CommandServer Thread,"  );
					//logger.warn(Utils.getStringException(e));
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.debug("Sleep interrupted");
			}
		}
		
			private void processRememberedWords() throws InvalidDenomeException {
				//
				// now check to see if any chains or words need to be unwrapped
				// first do the chains
				//
				DenomeManager aDenomeManager = hypothalamus.aDenomeManager;
				JSONObject currentPulse = aDenomeManager.getCurrentlyCreatingPulseJSONObject();
				String lastPulseTimestamp = currentPulse.getString(TeleonomeConstants.PULSE_TIMESTAMP);
				long lastPulseTime = currentPulse.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
				MnemosyneManager aMnemosyneManager = hypothalamus.aMnemosyneManager;
				Hashtable<String,ArrayList> deneChainsToRememberByTeleonome = aDenomeManager.getDeneChainsToRememberByTeleonome();
				String rememberedeneChainPointer;
				
				String valueType;
				TimeZone timeZone = aDenomeManager.getTeleonomeTimeZone();
				String teleonomeName = aDenomeManager.getDenomeName();
				ArrayList teleonomeRememberedDeneChainsArrayList = deneChainsToRememberByTeleonome.get(teleonomeName);
				logger.debug("for " + teleonomeName + " teleonomeRememberedDeneChainsArrayList: " + teleonomeRememberedDeneChainsArrayList );
				Object value;
				if(teleonomeRememberedDeneChainsArrayList!=null && teleonomeRememberedDeneChainsArrayList.size()>0) {
					Identity deneChainIdentity;
					for( int i=0;i<teleonomeRememberedDeneChainsArrayList.size();i++) {
						
						rememberedeneChainPointer = (String) teleonomeRememberedDeneChainsArrayList.get(i);	
						deneChainIdentity = new Identity(rememberedeneChainPointer);
						JSONObject deneChainJSONObject = aDenomeManager.getDeneChainByIdentity(deneChainIdentity);
						Hashtable toReturn = new Hashtable();
						JSONArray denes = deneChainJSONObject.getJSONArray("Denes");
						JSONObject dene, deneWord;
						JSONArray deneWords;
						boolean b;
						Identity includedRememberedIdentity;
						for(int l=0;l<denes.length();l++) {
							dene = (JSONObject)denes.get(l);
							deneWords = dene.getJSONArray("DeneWords");
							for(int j=0;j<deneWords.length();j++) {
								deneWord = (JSONObject)deneWords.get(j);
								includedRememberedIdentity = new Identity(deneChainIdentity.getTeleonomeName(), deneChainIdentity.getNucleusName(), deneChainIdentity.getDenechainName(), dene.getString(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE),deneWord.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
								value = getDeneWordByIdentity(currentPulse,includedRememberedIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
								valueType = (String) getDeneWordByIdentity(currentPulse, includedRememberedIdentity, TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
								logger.debug("about to unwrap " + includedRememberedIdentity.toString() + " with value:" + value  + " and valueType=" + valueType);
								aMnemosyneManager.unwrap(timeZone, teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), valueType,value);			
							}
						}
					}
				}
				//
				// now do the denes
				//
				
				Hashtable<String,ArrayList> denesToRememberByTeleonome = aDenomeManager.getDenesToRememberByTeleonome();
				String rememberedenePointer;
				
				ArrayList teleonomeRememberedDenesArrayList = denesToRememberByTeleonome.get(teleonomeName);
				logger.debug("for " + teleonomeName + " teleonomeRememberedDenesArrayList: " + teleonomeRememberedDenesArrayList );
				
				if(teleonomeRememberedDenesArrayList!=null && teleonomeRememberedDenesArrayList.size()>0) {
					Identity deneIdentity;
					for( int i=0;i<teleonomeRememberedDenesArrayList.size();i++) {
						
						rememberedenePointer = (String) teleonomeRememberedDenesArrayList.get(i);	
						deneIdentity = new Identity(rememberedenePointer);
						JSONObject deneJSONObject = aDenomeManager.getDeneByIdentity(deneIdentity);
						Hashtable toReturn = new Hashtable();
						JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
						JSONObject dene, deneWord;
						boolean b;
						Identity includedRememberedIdentity;
						for(int j=0;j<deneWords.length();j++) {
							deneWord = (JSONObject)deneWords.get(j);
							includedRememberedIdentity = new Identity(deneIdentity.getTeleonomeName(), deneIdentity.getNucleusName(), deneIdentity.getDenechainName(), deneJSONObject.getString(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE),deneWord.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
							value = getDeneWordByIdentity(currentPulse,includedRememberedIdentity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							valueType = (String) getDeneWordByIdentity(currentPulse, includedRememberedIdentity, TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
							logger.debug("about to unwrap " + includedRememberedIdentity.toString() + " with value:" + value  + " and valueType=" + valueType);
							aMnemosyneManager.unwrap(timeZone, teleonomeName, lastPulseTime, includedRememberedIdentity.toString(), valueType,value);			
						}
						
					}
				}
				
				//
				// now do the denewords
				//
				
				String rememberedWordPointer;
				
				Hashtable<String,ArrayList> deneWordsToRememberByTeleonome = aDenomeManager.getDeneWordsToRememberByTeleonome();
				logger.debug("deneWordsToRememberByTeleonome " + deneWordsToRememberByTeleonome );
				
				ArrayList teleonomeRememberedWordsArrayList = deneWordsToRememberByTeleonome.get(teleonomeName);
				logger.debug("for " + teleonomeName + " teleonomeRememberedWordsArrayList: " + teleonomeRememberedWordsArrayList );
				
				if(teleonomeRememberedWordsArrayList!=null && teleonomeRememberedWordsArrayList.size()>0) {
					
					for( int i=0;i<teleonomeRememberedWordsArrayList.size();i++) {
						rememberedWordPointer = (String) teleonomeRememberedWordsArrayList.get(i);
						value = getDeneWordByIdentity(currentPulse,new Identity(rememberedWordPointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						valueType = (String) getDeneWordByIdentity(currentPulse, new Identity(rememberedWordPointer), TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
						logger.debug("about to unwrap " + rememberedWordPointer + " with value:" + value  + " and valueType=" + valueType);
						
						if(value!=null && valueType!=null) {
							aMnemosyneManager.unwrap(timeZone, teleonomeName, lastPulseTime, rememberedWordPointer, valueType,value);
						}else {
							logger.warn("Unwrap of " + rememberedWordPointer + " FAILED because value:" + value  + " and valueType=" + valueType);
							
						}
					}
				}
			}
			
			
			public  Object getDeneWordByIdentity(JSONObject dataSource, Identity identity, String whatToBring) throws InvalidDenomeException{
				JSONArray deneChainsArray=null;
				Object toReturn=null;
				try {

					String nucleusName=identity.getNucleusName();
					String deneChainName=identity.getDenechainName();
					String deneName=identity.getDeneName();
					String deneWordName=identity.getDeneWordName();
					
					//	//System.out.println("poijbt 1");
					//
					// now parse them
					JSONObject denomeObject = dataSource.getJSONObject("Denome");
					JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
					String name;
					JSONObject aJSONObject, internalNucleus = null,purposeNucleus = null,mnemosyneNucleus=null, humanInterfaceNucleus=null;
					//	//System.out.println("poijbt 2");
					for(int i=0;i<nucleiArray.length();i++){
						aJSONObject = (JSONObject) nucleiArray.get(i);
						name = aJSONObject.getString("Name");
						if(name.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
							internalNucleus= aJSONObject;
							deneChainsArray = internalNucleus.getJSONArray("DeneChains");
						}else if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
							purposeNucleus= aJSONObject;
							deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
						}else if(name.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
							mnemosyneNucleus= aJSONObject;
						}else if(name.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
							humanInterfaceNucleus= aJSONObject;
						}

					}
					//	//System.out.println("poijbt 3");
					if(nucleusName.equals(TeleonomeConstants.NUCLEI_INTERNAL)){
						deneChainsArray = internalNucleus.getJSONArray("DeneChains");
					}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
						deneChainsArray = purposeNucleus.getJSONArray("DeneChains");
					}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_MNEMOSYNE)){
						deneChainsArray = mnemosyneNucleus.getJSONArray("DeneChains");
					}else if(nucleusName.equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE)){
						deneChainsArray = humanInterfaceNucleus.getJSONArray("DeneChains");
					}
					//	//System.out.println("poijbt 4");
					JSONObject aDeneJSONObject, aDeneWordJSONObject;
					JSONArray denesJSONArray, deneWordsJSONArray;
					String valueType, valueInString;
					Object object;
					for(int i=0;i<deneChainsArray.length();i++){
						aJSONObject = (JSONObject) deneChainsArray.get(i);
						if(aJSONObject.getString("Name").equals(deneChainName)){
							denesJSONArray = aJSONObject.getJSONArray("Denes");
							for(int j=0;j<denesJSONArray.length();j++){
								aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
								//	//System.out.println("poijbt 5");
								if(aDeneJSONObject.getString("Name").equals(deneName)){
									deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
									for(int k=0;k<deneWordsJSONArray.length();k++){
										
										aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
										//subscriberThreadLogger.debug("aDeneWordJSONObject=" + aDeneWordJSONObject.getString("Name") + " deneWordName=" + deneWordName);
										if(aDeneWordJSONObject.getString("Name").equals(deneWordName)){
											//	//System.out.println("poijbt 7");
											if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
												toReturn= aDeneWordJSONObject;
											}else{
												toReturn= aDeneWordJSONObject.get(whatToBring);
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
				dataSource=null;
				return toReturn;
			}
			
		}
	