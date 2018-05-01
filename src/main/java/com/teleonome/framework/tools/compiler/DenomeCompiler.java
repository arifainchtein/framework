package com.teleonome.framework.tools.compiler;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MissingDenomeException;

public class DenomeCompiler {
	static String denomeFileInString="";
	private JSONObject internalNucleus;
	private JSONObject purposeNucleus;
	private ArrayList<Map.Entry<JSONObject, String>> jsonObjectAddressProblemsIndex = new ArrayList(); 
	JSONObject denomeJSONObject;
	JSONObject teleonomeJSONObject;

	public DenomeCompiler(){

	}
	public void compile(String filename){
		String fileNameNoExtension = FilenameUtils.getBaseName(filename);

		// TODO Auto-generated method stub
		try {
			denomeFileInString = FileUtils.readFileToString(new File(filename));
			new DenomeCompiler();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String denomeName = null;
		try {
			teleonomeJSONObject = new JSONObject(denomeFileInString);

			denomeJSONObject = teleonomeJSONObject.getJSONObject("Denome");
			denomeName = denomeJSONObject.getString("Name");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


		String nucleusName = null, deneChainName=null, deneName=null, deneWordName=null;
		//
		// now parse them
		JSONArray nucleiArray=null;
		try {
			nucleiArray = denomeJSONObject.getJSONArray("Nuclei");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		JSONObject aJSONObject = null, deneChain = null, dene = null, deneWord = null;
		String name, target;
		String path, valueType;
		boolean validPath=false;
		JSONArray deneChains = null,denes = null,deneWords = null;
		for(int i=0;i<nucleiArray.length();i++){
			try {
				aJSONObject = (JSONObject) nucleiArray.get(i);
				nucleusName = aJSONObject.getString("Name");
				//System.out.println("nucleus=" + nucleusName);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				deneChains = aJSONObject.getJSONArray("DeneChains");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			for(int j=0;j<deneChains.length();j++){
				try {
					deneChain = (JSONObject) deneChains.get(j);
					deneChainName = deneChain.getString("Name");
					//System.out.println("deneChainName=" + deneChainName);
					denes = deneChain.getJSONArray("Denes");
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				//
				//
				for(int k=0;k<denes.length();k++){
					try {
						dene = (JSONObject)denes.getJSONObject(k);
						deneName = dene.getString("Name");
						//System.out.println("deneName=" + deneName);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					//
					// for every dene check to see if there is a target
					// if so make sure that is fully qualified


					target="";
					try{
						target = dene.getString("Target");
					}catch(JSONException e){

					}
					if(target!=null && !target.equals("") && target.startsWith("@")){
						//
						// there is a target, so replace it with complete path
						// the valid values are either:
						//	one term, ie dene, make a complete path
						// three terms teleonomeName:nucleus:denechain - verify that it exists
						// four terms teleonomeName:nucleus:denechain:dene verify that exists

						// and five terms tleonomeName:nucleus:deneChain:dene:deneWord -verify that exists
						String[] tokens=target.substring(1).split(":");
						switch(tokens.length){
						case 1:
							//
							// this is the case where 
							path="@" + denomeName + ":" + nucleusName + ":" +deneChainName + ":" + deneName ;
							try {
								validPath = deneExists(denomeName,nucleusName,deneChainName,tokens[0]);

							} catch (JSONException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							if(validPath){
								try {
									dene.put("Target", path);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}else{
								jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(dene, target));
							}
							break;
						case 3:
							try {
								validPath = validatePath(target);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if(!validPath){
								jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(dene, target));
							}
							break;
						case 4:
							try {
								validPath = validatePath(target);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if(!validPath){
								jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(dene, target));
							}
							break;
						case 5:
							try {
								validPath = validatePath(target);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if(!validPath){
								jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(dene, target));
							}
							break;
						default:
							jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(dene, target));

							break;
						}

					}


					try {
						deneWords = dene.getJSONArray("DeneWords");
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						System.out.println("dene" + deneName + "  does not have denewords");
						jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(dene, "Does not have denewords"));
						e1.printStackTrace();
					}
					for(int l=0;l<deneWords.length();l++){
						try {
							deneWord = deneWords.getJSONObject(l);
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
						//
						// for every deneword that has int or double check that there are units
						//
						try {
							if(!checkDeneWordUnits( denomeName,  nucleusName, deneChain.getString("Name"),  dene.getString("Name"),  deneWord.getString("Name"))){
								jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, "Missing Units"));
							}
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						

						
						//
						// for every deneword check to see if there is a target
						// if so make sure that is fully qualified
						try{
							//
							// the target attribute
							//
							target="";
							try{
								target = deneWord.getString("Target");
							}catch(JSONException e){

							}
							if(target!=null && !target.equals("") && target.startsWith("@")){
								//
								// there is a target, so replace it with complete path
								// the valid values are either:
								//	one term, ie dene, make a complete path
								// three terms teleonomeName:nucleus:denechain - verify that it exists
								// four terms teleonomeName:nucleus:denechain:dene verify that exists

								// and five terms tleonomeName:nucleus:deneChain:dene:deneWord -verify that exists
								String[] tokens=target.substring(1).split(":");
								switch(tokens.length){
								case 1:
									//
									// this is the case where 
									path="@" + denomeName + ":" + nucleusName + ":" +deneChainName + ":" + deneName + ":" + tokens[0];
									validPath = deneExists(denomeName,nucleusName,deneChainName,tokens[0]);
									if(validPath){
										deneWord.put("Target", path);
									}else{
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								case 3:
									validPath = validatePath(target);
									if(!validPath){
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								case 4:
									validPath = validatePath(target);
									if(!validPath){
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								case 5:
									validPath = validatePath(target);
									if(!validPath){
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								default:
									jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));

									break;
								}

							}
							//
							// the Value attribute
							//
							target="";
							valueType="";
							try{
								valueType = deneWord.getString("Value Type");
								if(valueType.equals(TeleonomeConstants.DATATYPE_DENE_POINTER) || valueType.equals(TeleonomeConstants.DENE_TYPE_ACTION_LIST)){
									target = deneWord.getString("Value");
								}
							}catch(JSONException e){
								e.printStackTrace();
								jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, "Value Type missing"));
							}


							if(target!=null && !target.equals("") && target.startsWith("@") ){
								//
								// there is a target, so replace it with complete path
								// the valid values are either:
								//	one term, ie dene, make a complete path
								// three terms teleonomeName:nucleus:denechain - verify that it exists
								// four terms teleonomeName:nucleus:denechain:dene verify that exists

								// and five terms tleonomeName:nucleus:deneChain:dene:deneWord -verify that exists
								String[] tokens=target.substring(1).split(":");
								switch(tokens.length){
								case 1:
									//
									// this is the case where we are putting the dene name only in the same denechain as the dene that holds
									// this deneword
									path="@" + denomeName + ":" + nucleusName + ":" +deneChainName + ":" + tokens[0];
									validPath = deneExists(denomeName,nucleusName,deneChainName,tokens[0]);
									if(validPath){
										deneWord.put("Value", path);
									}else{
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								case 3:
									validPath = validatePath(target);
									if(!validPath){
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								case 4:
									validPath = validatePath(target);
									if(!validPath){
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								case 5:
									validPath = validatePath(target);
									if(!validPath){
										jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));
									}
									break;
								default:
									jsonObjectAddressProblemsIndex.add(new AbstractMap.SimpleEntry<JSONObject, String>(deneWord, target));

									break;
								}

							}



						}catch(JSONException e){
							e.printStackTrace();
							System.out.println(e.getMessage());
						}
					}
				}
			}
		}
//
		// check tht for in denes where denetype is Action, every deneword where the memowrd type is DenePointer the name can not
		// have spaces since hte name is used as variable name in the expression logic and can not
		// have spaces
		//
		// when defining a variable the deneword of the pointer has to be the same as thename used for the variable. ie:
		//{
        //    "Name": "PumpWattage",
        //    "Value": "@Tlaloc:Internal:Actuators:Main Power Switch:PumpWattage",
        //    "Required": true,
       //     "Value Type": "Dene Pointer"
       // }
		//
		// if there are any errors spit them out,
		JSONObject jsonObject;

		if(jsonObjectAddressProblemsIndex.size()>0){
			StringBuffer errorBuffer = new StringBuffer();
			for (Map.Entry<JSONObject, String> entry : jsonObjectAddressProblemsIndex) {

				jsonObject = entry.getKey();
				path = entry.getValue();

				try {
					errorBuffer.append("Bad Path:" + path + " in " + jsonObject.toString(4) + "\n\r");

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				FileUtils.write(new File(fileNameNoExtension + ".errors"), errorBuffer.toString());
				System.out.println("Found Errors look at " + fileNameNoExtension + ".errors");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				FileUtils.write(new File(fileNameNoExtension + ".compiled"), teleonomeJSONObject.toString(4));
				System.out.println("Compiled ok, look at " + fileNameNoExtension + ".compiled");
			} catch (IOException | JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public boolean validatePath(String path) throws JSONException{
		String[] tokens = path.substring(1).split(":");
		String teleonomeName,nucleusName, deneChainName, deneName, deneWordName;

		switch(tokens.length){
		case 1:
			teleonomeName = tokens[0];
			nucleusName=tokens[1];
			deneChainName=tokens[2];
			deneName=tokens[3];
			return deneExists(teleonomeName, nucleusName, deneChainName, deneName);
		case 2:
			teleonomeName = tokens[0];
			nucleusName=tokens[1];
			return nucleusExist(teleonomeName, nucleusName);
		case 3:
			teleonomeName = tokens[0];
			nucleusName=tokens[1];
			deneChainName=tokens[2];
			return deneChainExists(teleonomeName, nucleusName, deneChainName);
		case 4:
			teleonomeName = tokens[0];
			nucleusName=tokens[1];
			deneChainName=tokens[2];
			deneName=tokens[3];
			return deneExists(teleonomeName, nucleusName, deneChainName, deneName);
		case 5:
			teleonomeName = tokens[0];
			nucleusName=tokens[1];
			deneChainName=tokens[2];
			deneName=tokens[3];
			deneWordName=tokens[4];
			return deneWordExists(teleonomeName, nucleusName, deneChainName, deneName, deneWordName);
		}

		return false;
	}


	public boolean nucleusExist(String teleonomeName, String nucleusName) throws JSONException{
		JSONObject denomeObject = teleonomeJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String denomeName = denomeObject.getString("Name");
		if(!denomeName.equals(teleonomeName))return false;
		JSONObject aJSONObject;
		String name, target;
		JSONArray deneChains,denes,deneWords;
		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			if(name.equals(nucleusName))return true;
		}
		return false;

	}

	public boolean deneChainExists(String teleonomeName, String nucleusName, String aDeneChainName) throws JSONException{
		JSONObject denomeObject = teleonomeJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String denomeName = denomeObject.getString("Name");
		if(!denomeName.equals(teleonomeName))return false;
		JSONObject aJSONObject, deneChain,dene;
		String name,deneChainName,target;
		JSONArray deneChains,denes,deneWords;
		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			if(name.equals(nucleusName)){
				deneChains = aJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					deneChain = (JSONObject) deneChains.get(j);
					deneChainName = deneChain.getString("Name");
					if(deneChainName.equals(aDeneChainName))return true;
				}
			}
		}
		return false;
	}

	public boolean deneExists(String teleonomeName, String nucleusName, String aDeneChainName, String aDeneName) throws JSONException{
		JSONObject denomeObject = teleonomeJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String denomeName = denomeObject.getString("Name");
		if(!denomeName.equals(teleonomeName))return false;
		JSONObject aJSONObject, deneChain,dene;
		String name,deneChainName,target;
		JSONArray deneChains,denes,deneWords;
		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			if(name.equals(nucleusName)){
				deneChains = aJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					deneChain = (JSONObject) deneChains.get(j);
					deneChainName = deneChain.getString("Name");
					if(deneChainName.equals(aDeneChainName)){
						denes = deneChain.getJSONArray("Denes");
						//
						//
						for(int k=0;k<denes.length();k++){
							dene = (JSONObject)denes.getJSONObject(k);
							//System.out.println(dene.getString("Name") + " -----> " + aDeneName);
							if(dene.getString("Name").equals(aDeneName)){
								return true;
							}
						}

					}
				}
			}


		}
		return false;
	}

	public boolean deneWordExists(String teleonomeName, String nucleusName, String aDeneChainName, String aDeneName, String aDeneWord) throws JSONException{
		JSONObject denomeObject = teleonomeJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String denomeName = denomeObject.getString("Name");
		if(!denomeName.equals(teleonomeName))return false;
		JSONObject aJSONObject, deneChain,dene;
		String name,deneChainName,target;
		JSONArray deneChains,denes,deneWords;
		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			if(name.equals(nucleusName)){
				deneChains = aJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					deneChain = (JSONObject) deneChains.get(j);
					deneChainName = deneChain.getString("Name");
					if(deneChainName.equals(aDeneChainName)){
						denes = deneChain.getJSONArray("Denes");
						//
						//
						for(int k=0;k<denes.length();k++){
							dene = (JSONObject)denes.getJSONObject(k);
							deneWords = dene.getJSONArray("DeneWords");
							if(dene.getString("Name").equals(aDeneName)){
								for(int l=0;l<deneWords.length();l++){
									if(deneWords.getJSONObject(l).getString("Name").equals(aDeneWord)){
										return true;
									}
								}
							}

						}
					}
				}


			}



		}
		return false;
	}

	public boolean checkDeneWordUnits(String teleonomeName, String nucleusName, String aDeneChainName, String aDeneName, String aDeneWord) throws JSONException{
		JSONObject denomeObject = teleonomeJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String denomeName = denomeObject.getString("Name");
		if(!denomeName.equals(teleonomeName))return false;
		JSONObject aJSONObject, deneChain,dene;
		String name,deneChainName,target;
		JSONArray deneChains,denes,deneWords;
		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			if(name.equals(nucleusName)){
				deneChains = aJSONObject.getJSONArray("DeneChains");
				for(int j=0;j<deneChains.length();j++){
					deneChain = (JSONObject) deneChains.get(j);
					deneChainName = deneChain.getString("Name");
					if(deneChainName.equals(aDeneChainName)){
						denes = deneChain.getJSONArray("Denes");
						//
						//
						for(int k=0;k<denes.length();k++){
							dene = (JSONObject)denes.getJSONObject(k);
							deneWords = dene.getJSONArray("DeneWords");
							if(dene.getString("Name").equals(aDeneName)){
								for(int l=0;l<deneWords.length();l++){
									String valueType= deneWords.getJSONObject(l).getString("Value Type");
									if(	
											valueType.equals("int")||
											valueType.equals("double")
											){
										
										try{
											String units = deneWords.getJSONObject(l).getString("Units");
											if(units!=null && !units.equals("")){
												return true;
											}
										}catch(JSONException e){
											System.out.println("return aDeneName=" + aDeneName + " valuetype:" + valueType + " name=" + deneWords.getJSONObject(l).getString("Name"));
											return false;
										}
									}else return true;
								}
							}

						}
					}
				}


			}



		}
		return false;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		args = new String[1];
		args[0]="FlowBasedController.denome";
		if(args.length!=1){
			System.out.println("Usage DenomeCompiler  denomeFileName");
			System.exit(-1);
		}
		new DenomeCompiler().compile(args[0]);;


	}

}
