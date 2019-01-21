package com.teleonome.framework.denome;

import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.exception.TeleonomeValidationException;
import com.teleonome.framework.utils.Utils;

public class DenomeValidator {

	private static Logger logger = Logger.getLogger(com.teleonome.framework.denome.DenomeValidator.class);
	private static DenomeViewManager aDenomeViewerManager = new com.teleonome.framework.denome.DenomeViewManager();

	public DenomeValidator() {

	}

	public static JSONArray validate(String denomeInString) throws JSONException, MissingDenomeException{

		JSONObject pulse = new JSONObject(denomeInString);
		JSONObject denomeObject = pulse.getJSONObject("Denome");
		String teleonomeName = denomeObject.getString("Name");

		JSONArray errorReportJSONArray = new JSONArray();
		JSONObject errorJSONObject;

		try {
			aDenomeViewerManager.loadDenome(denomeInString);
		} catch (TeleonomeValidationException e1) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e1));
			Hashtable info = e1.getDetails();
			String problemIdentity=(String) info.get("ProblemIdentity");
			String errorTitle=(String) info.get("Error Title");
			String currentValue=(String) info.get("CurrentValue");

			errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
			errorReportJSONArray.put(errorJSONObject);
		}
		//
		// perform the tests
		// verify that every pointer resolves correctly
		//
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		JSONArray mutationsArray = denomeObject.getJSONArray("Mutations");

		JSONObject nucleusJSONObject, deneChainJSONObject,deneJSONObject,resolvedDeneWordJSONObject, deneWordJSONObject, resolvedDeneJSONObject;
		JSONArray deneChainsJSONArray, denesJSONArray, deneWordsJSONArray;
		String name, valueType, denePointer,errorMessage, deneWordType;
		Object denePointerObject;
		boolean ignore=false;
		for(int i=0;i<nucleiArray.length();i++){
			nucleusJSONObject = (JSONObject) nucleiArray.get(i);
			//System.out.println("looking at nucleus " + nucleusJSONObject.getString("Name"));
			deneChainsJSONArray = nucleusJSONObject.getJSONArray("DeneChains");
			for(int j=0;j<deneChainsJSONArray.length();j++){
				deneChainJSONObject = deneChainsJSONArray.getJSONObject(j);
				if(!deneChainJSONObject.has("Name")){
					String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name");
					String errorTitle = "DeneChain Missing Name";
					String currentValue = "";
					errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
					errorReportJSONArray.put(errorJSONObject);
					continue;
				}


				//System.out.println("looking at denechain " + deneChainJSONObject.getString("Name"));
				denesJSONArray = deneChainJSONObject.getJSONArray("Denes");
				logger.debug("denesJSONArray=" + denesJSONArray.toString(4));
				for(int k=0;k<denesJSONArray.length();k++){
					deneJSONObject = denesJSONArray.getJSONObject(k);
					if(!deneJSONObject.has("Name")){
						String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
								":" + deneChainJSONObject.getString("Name");
						String errorTitle = "Dene Missing Name";
						String currentValue = "";
						errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
						errorReportJSONArray.put(errorJSONObject);
					}else{
						if(!deneJSONObject.has("DeneWords")){

							String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
									":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name");
							String errorTitle = "Dene Missing DeneWords";
							String currentValue = "";
							errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
							errorReportJSONArray.put(errorJSONObject);
						}else{
							deneWordsJSONArray = deneJSONObject.getJSONArray("DeneWords");
							for(int l=0;l<deneWordsJSONArray.length();l++){
								deneWordJSONObject = deneWordsJSONArray.getJSONObject(l);

								if(!deneWordJSONObject.has(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE)){
									String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
											":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") +
											":" + deneWordJSONObject.getString("Name");
									String errorTitle = "DeneWord missing Value Type";
									String currentValue = "";

									errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
									errorReportJSONArray.put(errorJSONObject);

								}else{
									valueType = deneWordJSONObject.getString(TeleonomeConstants.DENEWORD_VALUETYPE_ATTRIBUTE);
									deneWordType="";
									if(deneWordJSONObject.has(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE)) {
										deneWordType = deneWordJSONObject.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE);
									}


									if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER)){
										if(!deneWordJSONObject.has(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)){
											String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
													":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") +
													":" + deneWordJSONObject.getString("Name");

											String errorTitle = "DeneWord Missing Value";
											String currentValue = "";
											errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
											errorReportJSONArray.put(errorJSONObject);
											continue;
										}
										denePointerObject = deneWordJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

										if(denePointerObject instanceof String && ((String)denePointerObject).startsWith("$")){

										}else if(denePointerObject instanceof String && ((String)denePointerObject).startsWith("@")){
											try {
												Identity identity = new Identity((String)denePointerObject);
												//System.out.println("line 160 denePointer=" + denePointer + "       DeneWord is null=" + (resolvedDeneJSONObject==null) );
												//
												// there are several cases when a pointer points to a dene that will not be found in the denome when fertilized
												// This include:
												// 1)Remembered denewords will not be found in the denome, speially because they can come from other Teleonomes
												// 2)Action Processing
												// 3)Mnemosycon Processing
												ignore=false;
												if(
														(identity.getNucleusName().equals(TeleonomeConstants.NUCLEI_PURPOSE) && (identity.getDenechainName().equals(TeleonomeConstants.DENE_TYPE_ACTUATOR_ACTION_PROCESSING) || identity.getDenechainName().equals(TeleonomeConstants.DENE_TYPE_MNEMOSYCON_PROCESSING)))	
														||
														deneWordType.equals(TeleonomeConstants.DENEWORD_TYPE_MNEMOSYCON_REMEMBERED_DENEWORD)
														) {
														ignore=true;
												}
												
												
												if(!ignore) {
													if(identity.isDene()){
														resolvedDeneJSONObject = aDenomeViewerManager.getDeneByIdentity(identity);
														//
														// check to see if tis a RememberedDeneowrd

														if(!ignore && resolvedDeneJSONObject==null){

															String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
																	":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") ;

															String errorTitle = "Unresolved Dene Pointer";
															String currentValue =  denePointerObject.toString();

															errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
															errorReportJSONArray.put(errorJSONObject);
														}
													}else if(identity.isDeneWord()){
														logger.debug("about to get " + identity);
														resolvedDeneWordJSONObject = aDenomeViewerManager.getDeneWordByIdentity(identity);
														//System.out.println("line 77 denePointer=" + denePointer + "       DeneWord is null=" + (resolvedDeneWordJSONObject==null) );

														if(resolvedDeneWordJSONObject==null){

															String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
																	":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") +
																	":" + deneWordJSONObject.getString("Name");

															String errorTitle = "Unresolved DeneWord Pointer";
															String currentValue =  denePointerObject.toString();

															errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
															errorReportJSONArray.put(errorJSONObject);
														}
													}
												}
												} catch (NullPointerException e) {
													// TODO Auto-generated catch block

													String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
															":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") +
															":" + deneWordJSONObject.getString("Name") ;

													String errorTitle = "Null Pointer resolving a DeneWord pointer:" + denePointerObject.toString();
													String currentValue =  Utils.getStringException(e);
													errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
													errorReportJSONArray.put(errorJSONObject);

												} catch (InvalidDenomeException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}else{

											}
										}

										//
										// check to see if this deneword has a target, if the target is a pointer, then check to see
										// if the dene that it points to exists
										//
										if(deneWordJSONObject.has(TeleonomeConstants.DENEWORD_TARGET_ATTRIBUTE)){
											denePointer = deneWordJSONObject.getString(TeleonomeConstants.DENEWORD_TARGET_ATTRIBUTE);
											//System.out.println("looking at dw " + deneWordJSONObject.getString("Name") + " target denepointer " + denePointer);
											if(denePointer.startsWith("$")){

											}else if(denePointer.startsWith("@")){
												try {
													resolvedDeneJSONObject = aDenomeViewerManager.getDeneByIdentity(new Identity(denePointer));
													//System.out.println("line 287 resolvedDeneJSONObject is null= " + (resolvedDeneJSONObject == null));

													if(resolvedDeneJSONObject==null){

														String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
																":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") +
																":" + deneWordJSONObject.getString("Name");

														String errorTitle = "Unresolved Target Dene Pointer";
														String currentValue =  denePointer;

														errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
														errorReportJSONArray.put(errorJSONObject);
													}
												} catch (NullPointerException e) {
													// TODO Auto-generated catch block

													String problemIdentity= "@" + teleonomeName + ":" + nucleusJSONObject.getString("Name") + 
															":" + deneChainJSONObject.getString("Name") + ":" + deneJSONObject.getString("Name") +
															":" + deneWordJSONObject.getString("Name") ;

													String errorTitle = "Null Pointer resolving a DeneWord pointer:" + denePointer;
													String currentValue =  Utils.getStringException(e);
													errorJSONObject = generateError( problemIdentity,  errorTitle,  currentValue);
													errorReportJSONArray.put(errorJSONObject);

												} catch (InvalidDenomeException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
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
			return errorReportJSONArray;
		}

		private static JSONObject generateError(String problemIdentity, String errorTitle, String currentValue) throws JSONException{
			JSONObject errorJSONObject = new JSONObject();
			errorJSONObject.put("Problem Identity", problemIdentity);
			errorJSONObject.put("Error Title",errorTitle);
			errorJSONObject.put("Current Value", currentValue);
			return errorJSONObject;
		}
	}
