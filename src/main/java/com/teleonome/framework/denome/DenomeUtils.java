package com.teleonome.framework.denome;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;



import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.InvalidMutation;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.exception.TeleonomeValidationException;
import com.teleonome.framework.utils.Utils;

public class DenomeUtils {

	private static Logger logger = Logger.getLogger(com.teleonome.framework.denome.DenomeUtils.class);
	/**
	 * this methods returns the memory status dene
	 * which contains the first 5 lines of the top command
	 * the process specific lines are returned in the methd below
	 * @return
	 */
	public static JSONObject generateMemoryStatusDene() {
		JSONObject memoryStatusDene = new JSONObject();
		InputStream is = null;
		try {
			memoryStatusDene.put("Name", TeleonomeConstants.DENE_MEMORY_STATUS);
			JSONArray memoryStatusDeneWords = new JSONArray();
			memoryStatusDene.put("DeneWords", memoryStatusDeneWords);
			JSONObject deneWord = null;
			int pacemakerPid = Integer.parseInt(FileUtils.readFileToString(new File("PaceMakerProcess.info")).split("@")[0]);
			int webPid = Integer.parseInt(FileUtils.readFileToString(new File("WebServerProcess.info")).split("@")[0]);
			int heartProcessInfo = Integer.parseInt(FileUtils.readFileToString(new File("heart/HeartProcess.info")).split("@")[0]);
			String[] cmdArray = { "top", "-n1","-b","-p"+ pacemakerPid , "-p" + webPid , "-p" + heartProcessInfo  };
			ProcessBuilder pb = new ProcessBuilder(cmdArray);
			pb.redirectError();

			Process p = pb.start();

			is = p.getInputStream();
			int value = -1;
			StringBuffer buffer = new StringBuffer();
			while ((value = is.read()) != -1) {
				buffer.append((char)value);
			}
			int exitCode = p.waitFor();


			String[] lines = buffer.toString().split("\\r?\\n");
			String[] splited;
			//    System.out.println("lines=" + lines.length);
			for(int i=0;i<lines.length;i++) {
				switch(i) {

				case 0:
					//
					// top - 09:27:57 up 14:06,  4 users,  load average: 0.71, 0.58, 0.80
					//
					splited = lines[i].split(",");
					//			    		System.out.println("lines[i]=" + lines[i]);
					//			    		System.out.println("splited[0]=" + splited[0]);
					//			    		System.out.println("splited[1]=" + splited[1]);
					//			    		System.out.println("splited[2]=" + splited[2]);
					//			    		System.out.println("splited[3]=" + splited[3]);
					//			    		System.out.println("splited[4]=" + splited[4]);
					//			    		

					String theTime =splited[0].split("up")[0];
					String upTime =splited[0].split("up")[1];
					String numberUsers = splited[1];

					deneWord = DenomeUtils.buildDeneWordJSONObject("Time",""+theTime,null,"String",true);
					memoryStatusDeneWords.put(deneWord);

					deneWord = DenomeUtils.buildDeneWordJSONObject("Up Time",""+upTime,null,"String",true);
					memoryStatusDeneWords.put(deneWord);


					double loadAverage1Minute = 0;
					double loadAverage5Minute = 0;
					double loadAverage15Minute = 0;
					//
					// the very fist time that the pulse run this parsing does not
					// work so just let it be zero, it will correct itself
					// after the pacemaker has beeen runing for more than one minute
					try{
						loadAverage1Minute = Double.parseDouble(splited[2].split(":")[1]);
						loadAverage5Minute = Double.parseDouble(splited[3]);
						loadAverage15Minute = Double.parseDouble(splited[4]);

					}catch(ArrayIndexOutOfBoundsException e) {

					}catch(java.lang.NumberFormatException e) {

					}
					//System.out.println("lines[i]="+lines[i]);


					deneWord = DenomeUtils.buildDeneWordJSONObject("Load Average 1 Minute",""+loadAverage1Minute,null,"double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Load Average 5 Minute",""+loadAverage5Minute,null,"double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Load Average 15 Minute",""+loadAverage15Minute,null,"double",true);
					memoryStatusDeneWords.put(deneWord);
					break;
				case 1:
					// Tasks:   4 total,   0 running,   4 sleeping,   0 stopped,   0 zombie
					splited = lines[i].split(",");
					for(int j=0;j<splited.length;j++) {
						//		System.out.println("line " + j + " token " + j + " value=" + splited[j]);
					}
					break;
				case 2:
					// %Cpu(s): 22.7 us, 15.0 sy,  0.0 ni, 57.5 id,  3.5 wa,  0.0 hi,  1.2 si,  0.0 st

					splited = lines[i].split(":")[1].trim().split(",");
					//	System.out.println("ddd=" +  lines[i].split(":")[1]);//[0].split(" ")[0]);
					//	System.out.println("splited=" + splited.length);//[0].split(" ")[0]);

					double cpuUsageAsPercentByUser = Double.parseDouble(splited[0].trim().split("\\s+")[0]);
					double cpuUsageAsPercentBySystem = Double.parseDouble(splited[1].trim().split("\\s+")[0]);
					double cpuUsageAsPercentByLowPriorityServices = Double.parseDouble(splited[2].trim().split("\\s+")[0]);
					double cpuUsageAsPercentByIdleProcess = Double.parseDouble(splited[3].trim().split("\\s+")[0]);
					double cpuUsageAsPercentByIOWait = Double.parseDouble(splited[4].trim().split("\\s+")[0]);
					double cpuUsageAsPercentByHardwareInterrupt = Double.parseDouble(splited[5].trim().split("\\s+")[0]);
					double cpuUsageAsPercentBySoftwareInterrupts = Double.parseDouble(splited[6].trim().split("\\s+")[0]);
					double cpuUsageAsPercentByStealTime = Double.parseDouble(splited[7].trim().split("\\s+")[0]);

					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By User",""+cpuUsageAsPercentByUser,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By System",""+cpuUsageAsPercentBySystem,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Low Priority Services",""+cpuUsageAsPercentByLowPriorityServices,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Idle Process",""+cpuUsageAsPercentByIdleProcess,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By IOWait",""+cpuUsageAsPercentByIOWait,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Hardware Interrupt",""+cpuUsageAsPercentByHardwareInterrupt,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Cpu Usage As Percent By Software Interrupts",""+cpuUsageAsPercentBySoftwareInterrupts,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Usage As Percent By Steal Time",""+cpuUsageAsPercentByStealTime,"%","double",true);
					memoryStatusDeneWords.put(deneWord);
					break;
				case 3:
					//
					//KiB Mem:    380416 total,   364956 used,    15460 free,     5028 buffers
					splited = lines[i].split(":")[1].split(",");
					double totalSystemMemory = Double.parseDouble(splited[0].trim().split("\\s+")[0])/1000;
					int freeSystemMemory = Integer.parseInt(splited[1].trim().split("\\s+")[0])/1000;
					int memoryUsed = Integer.parseInt(splited[2].trim().split("\\s+")[0])/1000;
					double bufferCache = Double.parseDouble(splited[3].trim().split("\\s+")[0]);
					logger.debug("lines[i]=" + lines[i]);
					logger.debug("splited[2]="+ splited[2]);
					logger.debug("memoryUsed="+ memoryUsed);

					deneWord = DenomeUtils.buildDeneWordJSONObject("Total System Memory",""+totalSystemMemory,"Mb","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Free System Memory",""+freeSystemMemory,"Mb","int",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Memory Used",""+memoryUsed,"Mb","int",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Buffer Cache",""+bufferCache,"Kb","double",true);
					memoryStatusDeneWords.put(deneWord);

					break;
				case 4:
					//KiB Swap:   102396 total,      628 used,   101768 free.    97036 cached Mem
					splited = lines[i].split(":")[1].split(",");
					double totalSwapAvailable= Double.parseDouble(splited[0].trim().split(" ")[0]);
					double totalSwapUsed = Double.parseDouble(splited[1].trim().split(" ")[0]);
					//
					// there is a bug look at the period after the word free above
					// bviously they meant a , but put . so do something weird
					// to parse 
					// 101768 free.    97036 cached Mem
					//System.out.println("splited[2].trim()=" + splited[2].trim().split("\\s+")[2]);

					double totalSwapFree = Double.parseDouble(splited[2].trim().split("\\s+")[0].trim().split("\\s+")[0]);
					double cachedMemory = Double.parseDouble(splited[2].trim().split("\\s+")[2].trim().split("\\s+")[0]);

					deneWord = DenomeUtils.buildDeneWordJSONObject("Total Swap Available",""+totalSwapAvailable,"Kb","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Total Swap Used",""+totalSwapUsed,"Kb","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Total Swap Free",""+totalSwapFree,"Kb","double",true);
					memoryStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Cached Memory",""+cachedMemory,"Kb","double",true);
					memoryStatusDeneWords.put(deneWord);
					break;
				}
				//  System.out.println("memoryStatusDene " + memoryStatusDene.toString(4) );
			}

		} catch (IOException exp) {
			exp.printStackTrace();
		} catch (InterruptedException ex) {
			//Logger.getLogger(JavaApplication256.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			if(is!=null)is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		return memoryStatusDene;
	}

	/**
	 * this method returns and array containg denes.  each dene is the line from the top
	 * process for each of the three process, cerebellum, heart and web.  for the top 5 lines see the methiod above
	 * @return
	 */
	public static JSONArray generateProcessMemoryStatusDene() {
		JSONArray toReturn = new JSONArray();
		InputStream is=null;
		try {

			JSONObject deneWord = null;
			int pacemakerPid = Integer.parseInt(FileUtils.readFileToString(new File("PaceMakerProcess.info")).split("@")[0]);
			int webPid = Integer.parseInt(FileUtils.readFileToString(new File("WebServerProcess.info")).split("@")[0]);
			int heartProcessInfo = Integer.parseInt(FileUtils.readFileToString(new File("heart/HeartProcess.info")).split("@")[0]);
			String[] cmdArray = { "top", "-n1","-b","-p"+ pacemakerPid , "-p" + webPid , "-p" + heartProcessInfo  };
			ProcessBuilder pb = new ProcessBuilder(cmdArray);
			pb.redirectError();

			Process p = pb.start();

			is = p.getInputStream();
			int value = -1;
			StringBuffer buffer = new StringBuffer();
			while ((value = is.read()) != -1) {
				buffer.append((char)value);
			}
			int exitCode = p.waitFor();


			String[] lines = buffer.toString().split("\\r?\\n");
			String[] splited;
			//    System.out.println("lines=" + lines.length);
			for(int i=0;i<lines.length;i++) {
				switch(i) {
				case 7:
				case 8:
				case 9:
					//24587 pi        20   0  171808  42972   5468 S  0.0 11.3   4:11.48 java
					splited = lines[i].trim().split("\\s+");
					int processId = Integer.parseInt(splited[0]);
					String processName="";
					if(processId==pacemakerPid) {
						processName  =TeleonomeConstants.PROCESS_HYPOTHALAMUS;

					}else if(processId==heartProcessInfo) {
						processName  =TeleonomeConstants.PROCESS_HEART;

					}else if(processId==webPid) {
						processName  =TeleonomeConstants.PROCESS_WEB_SERVER;

					}
					String user = splited[1];
					int priority = Integer.parseInt(splited[2]);
					int niceLevel = Integer.parseInt(splited[3]);
					int virtualMemoryUedByProcess = Integer.parseInt(splited[4]);
					int residentMemoryUedByProcess = 0;
					try{
						residentMemoryUedByProcess = Integer.parseInt(splited[5]);
					}catch(NumberFormatException e) {

					}
					int shareableMemory = Integer.parseInt(splited[6]);
					String currentStatus = splited[7];
					double cpuUsedByProcessAsPercentage = Double.parseDouble(splited[8]);
					double memoryUsedByProcessAsPercentage = Double.parseDouble(splited[9]);
					String timeUsedByProcessAsPercentage = splited[10];
					String command =  splited[11];

					JSONObject processStatusDene = new JSONObject();
					processStatusDene.put("Dene Type", TeleonomeConstants.DENE_TYPE_PROCESS_MEMORY_INFO);
					processStatusDene.put("Name", processName);
					JSONArray processStatusDeneWords = new JSONArray();
					processStatusDene.put("DeneWords", processStatusDeneWords);

					deneWord = DenomeUtils.buildDeneWordJSONObject("User",""+user,null,"String",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Priority",""+priority,null,"int",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Nice Level",""+niceLevel,null,"",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Virtual Memory Used By Process",""+virtualMemoryUedByProcess,"Kb","int",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Resident Memory Used By Process",""+residentMemoryUedByProcess,"Kb","int",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Shareable Memory",""+shareableMemory,"Kb","int",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Current Status",""+currentStatus,null,"String",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("CPU Used By Process As Percentage",""+cpuUsedByProcessAsPercentage,"%","double",true);
					processStatusDeneWords.put(deneWord);
					deneWord = DenomeUtils.buildDeneWordJSONObject("Time Process Has Been Running",""+timeUsedByProcessAsPercentage,null,"String",true);
					processStatusDeneWords.put(deneWord);

					if(processName.equals(TeleonomeConstants.PROCESS_HYPOTHALAMUS)) {
						double hypothalamusAvailableMemory = Runtime.getRuntime().freeMemory()/1024000;
						double hypothalamusMaxMemory = Runtime.getRuntime().maxMemory()/1024000;

						deneWord = DenomeUtils.buildDeneWordJSONObject(TeleonomeConstants.HYPOTHALAMUS_PROCESS_AVAILABLE_MEMORY,""+hypothalamusAvailableMemory,"Mb","double",true);
						processStatusDeneWords.put(deneWord);
						deneWord = DenomeUtils.buildDeneWordJSONObject(TeleonomeConstants.HYPOTHALAMUS_PROCESS_MAXIMUM_MEMORY,""+hypothalamusMaxMemory,"Mb","double",true);
						processStatusDeneWords.put(deneWord);
					}else if(processName.equals(TeleonomeConstants.PROCESS_WEB_SERVER)) {
						String webserverPingInfoS = FileUtils.readFileToString(new File("WebServerPing.info"));
						if(webserverPingInfoS!=null) {
							JSONObject webserverPingInfo = new JSONObject(webserverPingInfoS);
							double webserverAvailableMemory = webserverPingInfo.getDouble(TeleonomeConstants.WEBSERVER_PROCESS_AVAILABLE_MEMORY);
							double webserverMaxMemory = webserverPingInfo.getDouble(TeleonomeConstants.WEBSERVER_PROCESS_MAXIMUM_MEMORY);
							deneWord = DenomeUtils.buildDeneWordJSONObject(TeleonomeConstants.WEBSERVER_PROCESS_AVAILABLE_MEMORY,""+webserverAvailableMemory,"Mb","double",true);
							processStatusDeneWords.put(deneWord);
							deneWord = DenomeUtils.buildDeneWordJSONObject(TeleonomeConstants.WEBSERVER_PROCESS_MAXIMUM_MEMORY,""+webserverMaxMemory,"Mb","double",true);
							processStatusDeneWords.put(deneWord);
						}

					}else if(processName.equals(TeleonomeConstants.PROCESS_HEART)) {
						String heartPingInfoS = FileUtils.readFileToString(new File("heart/HeartPing.info"));
						if(heartPingInfoS!=null) {
							JSONObject heartPingInfo = new JSONObject(heartPingInfoS);
							double heartAvailableMemory = heartPingInfo.getDouble(TeleonomeConstants.HEART_PROCESS_AVAILABLE_MEMORY);
							double heartMaxMemory = heartPingInfo.getDouble(TeleonomeConstants.HEART_PROCESS_MAXIMUM_MEMORY);
							deneWord = DenomeUtils.buildDeneWordJSONObject(TeleonomeConstants.HEART_PROCESS_AVAILABLE_MEMORY,""+heartAvailableMemory,"Mb","double",true);
							processStatusDeneWords.put(deneWord);
							deneWord = DenomeUtils.buildDeneWordJSONObject(TeleonomeConstants.HEART_PROCESS_MAXIMUM_MEMORY,""+heartMaxMemory,"Mb","double",true);
							processStatusDeneWords.put(deneWord);
						}
					}
					//System.out.println("processStatusDene=" + processStatusDene.toString(4));
					toReturn.put(processStatusDene);
					break;

				}
			}

		} catch (IOException exp) {
			exp.printStackTrace();
		} catch (InterruptedException ex) {
			//Logger.getLogger(JavaApplication256.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			if(is!=null)is.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return toReturn;
	}
	public boolean deneContainsDeneWordByName(JSONObject deneJSONObject,String name ){
		JSONArray deneWords;
		JSONObject deneWord;
		JSONArray toReturn = new JSONArray();
		String deneName;
		try {
			deneWords = deneJSONObject.getJSONArray("DeneWords");
			for(int i=0;i<deneWords.length();i++){
				deneWord = deneWords.getJSONObject(i);
				try{
					deneName = deneWord.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
					if(deneName.equals(name)){
						return true;
					}
				}catch(JSONException e){

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public static Object getDeneWordAttributeFromMutationByMutationIdentity(JSONObject mutationObject, MutationIdentity identity, String whatToBring)throws IOException, InvalidMutation, InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {

			deneChainsArray = mutationObject.getJSONArray("DeneChains");
			//	//System.out.println("poijbt 4");
			JSONObject aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			JSONObject aJSONObject;

			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(identity.getDenechainName())){
					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
						//	//System.out.println("poijbt 5");
						if(aDeneJSONObject.getString("Name").equals(identity.getDeneName())){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								//	//System.out.println("poijbt 6");
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.getString("Name").equals(identity.getDeneWordName())){
									//	//System.out.println("poijbt 7");
									if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
										return aDeneWordJSONObject;
									}else{
										return aDeneWordJSONObject.get(whatToBring);
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






	static class AscendingIntegerCompare implements Comparator<Map.Entry<?, Integer>>{
		public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
	}

	static class DescendingIntegerCompare implements Comparator<Map.Entry<?, Integer>>{
		public int compare(Map.Entry<?, Integer> o1, Map.Entry<?, Integer> o2) {
			return o2.getValue().compareTo(o1.getValue());
		}
	}

	public static JSONArray getDenesSorteByPostiond(JSONArray denes, String deneName, boolean ascending) throws JSONException{
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

				if(aDenePosition<minimumPosition)minimumPosition=aDenePosition;
				if(aDenePosition>maximumPosition)maximumPosition=aDenePosition;
				deneByPositionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(dene, new Integer(aDenePosition)));
			}
		}
		if(ascending)Collections.sort(deneByPositionIndex, new AscendingIntegerCompare());
		else Collections.sort(deneByPositionIndex, new DescendingIntegerCompare());
		JSONArray toReturn = new JSONArray();
		for (Map.Entry<JSONObject, Integer> entry : deneByPositionIndex) {
			dene = entry.getKey();
			toReturn.put(dene);
		}
		return toReturn;
	}

	public static JSONObject getDeneFromDeneJSONArrayByPostion(JSONArray denes, String deneName, String position) throws JSONException{
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

				if(aDenePosition<minimumPosition)minimumPosition=aDenePosition;
				if(aDenePosition>maximumPosition)maximumPosition=aDenePosition;
				deneByPositionIndex.add(new AbstractMap.SimpleEntry<JSONObject, Integer>(dene, new Integer(aDenePosition)));
			}
		}
		Collections.sort(deneByPositionIndex, new AscendingIntegerCompare());
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


	public static Hashtable flattenChain(String teleonomeName, JSONObject deneChain)throws InvalidDenomeException {

		String deneChainName="";
		try {
			deneChainName = deneChain.getString("Name");
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Hashtable toReturn = new Hashtable();
		JSONArray denes=null;
		try {
			denes = deneChain.getJSONArray("Denes");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new Hashtable();
		}
		Object value;
		JSONObject dene, deneWord;
		JSONArray deneWords;
		Identity identity;
		String deneName, deneWordName;

		for(int i=0;i<denes.length();i++){
			try {
				dene = denes.getJSONObject(i);
				deneName = dene.getString("Name");
				deneWords = dene.getJSONArray("DeneWords");
				for(int j=0;j<deneWords.length();j++){
					deneWord = deneWords.getJSONObject(j);
					deneWordName = deneWord.getString("Name");
					value = deneWord.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					identity = new Identity(teleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, deneChainName,deneName,deneWordName);
					toReturn.put(identity.toString(), value);		
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return toReturn;

	}




	/**
	 * the first parameter is a denechain
	 * the second parameter is the type of denes to extract from that denechain
	 * returns an Array of Denes that are of the required type
	 * 
	 * @param aDeneChainJSONObject
	 * @param deneType
	 * @return
	 * @throws JSONException
	 */

	public static JSONObject getDeneByName(JSONObject aDeneChainJSONObject,String deneName) throws JSONException{
		JSONArray denes = aDeneChainJSONObject.getJSONArray("Denes");
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<denes.length();i++){
			if(denes.getJSONObject(i).getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(deneName)){
				return denes.getJSONObject(i);
			}
		}
		return null;
	}



	public static JSONArray getDenesByDeneType(JSONObject aDeneChainJSONObject,String deneType) throws JSONException{
		JSONArray denes = aDeneChainJSONObject.getJSONArray("Denes");
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<denes.length();i++){
			if(denes.getJSONObject(i).has(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE) && denes.getJSONObject(i).getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE).equals(deneType)){
				toReturn.put(denes.getJSONObject(i));
			}
		}
		return toReturn;
	}


	public static boolean isMutationIdentity(String hoxDeneTargetPointer){
		Identity id = new Identity(hoxDeneTargetPointer);
		boolean toReturn=true;
		if(id.getNucleusName().equals(TeleonomeConstants.NUCLEI_HUMAN_INTERFACE) ||
				id.getNucleusName().equals(TeleonomeConstants.NUCLEI_INTERNAL) ||
				id.getNucleusName().equals(TeleonomeConstants.NUCLEI_MNEMOSYNE) ||
				id.getNucleusName().equals(TeleonomeConstants.NUCLEI_PURPOSE)
				) {
			toReturn=false;
		}
		return toReturn;
	}


	public static boolean isDeneOfType(JSONObject dene, String whichDeneType){
		boolean itIs=false;
		try{
			String deneType = dene.getString("Dene Type");
			if(deneType.equals(whichDeneType)){
				itIs=true;
			}
		}catch(JSONException e){

		}
		return itIs;
	}
	/**
	 * This method is used by the subscriber thread, to detect if there is a problem with
	 * the exozero network, ie if there is another teleonome waiting for data from this 
	 * teleonome.
	 * if it returns true then the exozero publisher needs to be restarted
	 * There are two places to check, the external data and all the mnemosycons of type DENE_TYPE_MNEMOSYCON_DENEWORDS_TO_REMEMBER
	 * 
	 * @param publisherTeleonomeName - the name of the publisher teleonome
	 * @param dependentTeleonomePulse - the data of the teleonome dependind of the publisherteleonome data
	 * @return
	 */
	public static boolean isSomebodyWaitingForMe(String publisherTeleonomeName, JSONObject dependentTeleonomePulse){
		//
		// get the address of the deneword where this data is going to
		String reportingAddress, deneWordName;
		Vector teleonomeToReconnect = new Vector();
		boolean somebodyIsWating=false;
		try {

			JSONObject dependentPulseDenome = dependentTeleonomePulse.getJSONObject("Denome");
			String dependentTeleonomeName = dependentPulseDenome.getString("Name");
			JSONArray dependentPulseNuclei = dependentPulseDenome.getJSONArray("Nuclei");
			JSONArray deneWords;

			JSONObject jsonObject, jsonObjectChain, jsonObjectDene, jsonObjectDeneWord;
			JSONArray chains, denes;
			String externalDataDeneName;
			JSONObject lastPulseExternalTeleonomeJSONObject;
			String externalSourceOfData;


			long lastPulseExternalTimeInMillis,difference;
			String lastPulseExternalTime;
			Identity externalDataCurrentPulseIdentity,numberOfPulseForStaleIdentity;
			int secondsToStale=180;
			//String valueType;

			for(int i=0;i<dependentPulseNuclei.length();i++){
				jsonObject = dependentPulseNuclei.getJSONObject(i);
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
								logger.debug("line 662 Denomemutils, looking for  " + externalDataDeneName);
								if(publisherTeleonomeName.equals(externalDataDeneName)) {

									String externalDeneStatus=TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE;
									Identity denewordStatusIdentity = new Identity(dependentTeleonomeName,TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_EXTERNAL_DATA,publisherTeleonomeName, TeleonomeConstants.EXTERNAL_DATA_STATUS);
									try {
										externalDeneStatus = (String) getDeneWordByIdentity(dependentTeleonomePulse, denewordStatusIdentity,  TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
									} catch (InvalidDenomeException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									logger.debug("externalDeneStatus after getting data by pointer " + externalDeneStatus);
									if(externalDeneStatus.equals(TeleonomeConstants.EXTERNAL_DATA_STATUS_STALE)) {
										somebodyIsWating=true;
									}

								}


							}
						}
					}
				}
			}
			//
			// now check the mnemosycons of denetype DENE_TYPE_MNEMOSYCON_DENEWORDS_TO_REMEMBER
			//

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		return somebodyIsWating;
	}


	private String getDeneWordAttributeByIdentity(Identity denewordStatusIdentity, String denewordValueAttribute) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * you pass a dene as a first parameter and the value of the dene attribute "Dene Type" as a second parameter
	 * and the third parameter is what part of th the DeneWord you want and in the third parameter
	 * you say what you want back. If you want the whole deneword you pass TeleonomeConstants.COMPLETE, otherwise
	 * you pass one of the Teleonome constants
	 * it reutrns what you asked for in the deneword called what you call it from the dene in the first parameter
	 * @param deneJSONObject
	 * @param aDeneWordName
	 * @param whatToReturn
	 * @return
	 * @throws JSONException
	 */
	public static JSONArray getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(JSONObject dene, String whatDeneType, String whatToReturn){
		JSONArray deneWords;
		JSONArray toReturn = new JSONArray();
		try {
			deneWords = dene.getJSONArray("DeneWords");
			for(int i=0;i<deneWords.length();i++){
				JSONObject deneWord = deneWords.getJSONObject(i);
				try{
					String deneTypeName = deneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE);
					if(deneTypeName.equals(whatDeneType)){
						if(whatToReturn.equals(TeleonomeConstants.COMPLETE)){
							toReturn.put(deneWord);
						}else{
							toReturn.put(deneWord.get(whatToReturn));
						}
					}
				}catch(JSONException e){

				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return toReturn;
	}

	public static  JSONObject getDeneFromDeneChainByDeneName(JSONObject deneChain, String deneName) throws JSONException{
		JSONArray denesJSONArray = deneChain.getJSONArray("Denes");
		JSONObject aDeneJSONObject;
		for(int j=0;j<denesJSONArray.length();j++){
			aDeneJSONObject = (JSONObject) denesJSONArray.get(j);
			//logger.debug("getdenebyidentity point3 " + aDeneJSONObject.getString("Name"));

			if(aDeneJSONObject.getString("Name").equals(deneName)){
				return aDeneJSONObject;
			}
		}
		return null;
	}

	/**
	 * you pass a dene as a first parameter and the name of the DeneWord you want and in the third parameter
	 * you say what you want back. If you want the whole deneword you pass TeleonomeConstants.COMPLETE, otherwise
	 * you pass one of the Teleonome constants
	 * it reutrns what you asked for in the deneword called what you call it from the dene in the first parameter
	 * @param deneJSONObject
	 * @param aDeneWordName
	 * @param whatToReturn
	 * @return
	 * @throws JSONException
	 */

	public static Object getDeneWordAttributeByDeneWordNameFromDene(JSONObject deneJSONObject , String aDeneWordName, String whatToReturn) throws JSONException{
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			String deneWordName = deneWord.getString("Name");
			if(deneWordName.equals(aDeneWordName)){
				if(whatToReturn.equals(TeleonomeConstants.COMPLETE)){
					return deneWord;
				}else{
					return deneWord.get(whatToReturn);
				}
			}
		}
		return null;
	}
	public static Object getDeneWordAttributeByDeneWordTypeFromDene(JSONObject deneJSONObject , String aDeneWordType, String whatToReturn) throws JSONException{
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		//System.out.println("in getDeneWordAttributeByDeneWordTypeFromDene, aDeneWordType=" + aDeneWordType);
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			if(deneWord.has(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE)){
				String deneWordType = deneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE);
				if(deneWordType.equals(aDeneWordType)){
					if(whatToReturn.equals(TeleonomeConstants.COMPLETE)){
						return deneWord;
					}else{
						//System.out.println("in getDeneWordAttributeByDeneWordTypeFromDene, aDeneWordType=" + aDeneWordType + " whatToReturn=" + whatToReturn + " deneWord.get(whatToReturn)=" + deneWord.get(whatToReturn));

						return deneWord.get(whatToReturn);
					}
				}
			}

		}
		return null;
	}

	/**
	 * you pass a dene as a first parameter and in the second parameter
	 * you say what attribute you want back. The method wll return the value desired for all denewords in the dene
	 * 
	 * @param deneJSONObject
	 * @param aDeneWordName
	 * @param whatToReturn
	 * @return
	 * @throws JSONException
	 */

	public static Vector getDeneWordAttributeForAllDeneWordsFromDene(JSONObject deneJSONObject ,String attributeName, String whatToReturn) throws JSONException{
		JSONArray deneWords = deneJSONObject.getJSONArray("DeneWords");
		Vector toReturn = new Vector();
		for(int i=0;i<deneWords.length();i++){
			JSONObject deneWord = deneWords.getJSONObject(i); 
			String deneWordName = deneWord.getString("Name");
			if(whatToReturn.equals(TeleonomeConstants.COMPLETE)){
				toReturn.addElement(deneWord);
			}else{
				toReturn.addElement(deneWord.get(whatToReturn));
			}

		}
		return toReturn;
	}

	/**
	 * 
	 * @param dene - the dene that contains the desired deneword
	 * @param deneWordAttributeName, the name of the attribute to search on
	 * @param deneValueType - the value to use in the search through the DeneWords
	 * @param whatToReturn  the name of the attribute to return
	 * @return
	 * @throws JSONException
	 */
	public static JSONArray getAllMeweWordsFromDeneByDeneWordType(JSONObject dene, String deneWordAttributeName, String deneValueType, String whatToReturn)throws JSONException{

		JSONArray deneWords = dene.getJSONArray("DeneWords");
		JSONObject deneWord;
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<deneWords.length();i++){
			////System.out.println("getting the next deneworld, i=" + i);
			deneWord = (JSONObject)deneWords.getJSONObject(i);

			try{
				if(deneWord.getString(deneWordAttributeName).equals(deneValueType)){
					if(whatToReturn.equals(TeleonomeConstants.COMPLETE)){
						toReturn.put(deneWord);
					}else{
						toReturn.put(deneWord.getString(whatToReturn));
					}
				}
			}catch(JSONException e){

			}
		}
		return toReturn;
	}


	/**
	 * 
	 * @param dene - the dene that contains the desired deneword
	 * @param deneWorType, the deneword type of the attribute to search on
	 * @param whatToReturn  the name of the attribute to return
	 * @return
	 * @throws JSONException
	 */
	public static JSONArray getAllDeneWordsFromDeneByDeneWordType(JSONObject dene, String deneWordType, String whatToReturn)throws JSONException{

		JSONArray deneWords = dene.getJSONArray("DeneWords");
		JSONObject deneWord;
		JSONArray toReturn = new JSONArray();
		for(int i=0;i<deneWords.length();i++){
			deneWord = (JSONObject)deneWords.getJSONObject(i);
			logger.debug("deneWord=" + deneWord.toString(4));
			try{
				if(	deneWord.has(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE) && 
						deneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE).equals(deneWordType)){

					if(whatToReturn.equals(TeleonomeConstants.COMPLETE)){
						toReturn.put(deneWord);
					}else{
						toReturn.put(deneWord.getString(whatToReturn));
					}
				}
			}catch(JSONException e){
				logger.warn(Utils.getStringException(e));
			}
		}
		return toReturn;
	}


	public static Object getDeneWordByIdentity(JSONObject dataSource, Identity identity, String whatToBring) throws InvalidDenomeException{
		//
		// if we are pointing at itself return the default
		if(identity.isCommand()){
			return null;
		}else{
			return getDeneWordByPointer(dataSource,identity.getNucleusName(),identity.getDenechainName(), identity.getDeneName(), identity.getDeneWordName(), whatToBring);
		}
	}








	public static Object getDeneWordByPointer(JSONObject dataSource, String nucleusName,String deneChainName, String deneName, String deneWordName, String whatToBring) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		try {

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
								//	//System.out.println("poijbt 6");
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.getString("Name").equals(deneWordName)){
									//	//System.out.println("poijbt 7");
									if(whatToBring.equals(TeleonomeConstants.COMPLETE)){
										return aDeneWordJSONObject;
									}else{
										return aDeneWordJSONObject.get(whatToBring);
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

	public static JSONObject buildDeneWordJSONObject(String name, String value, String units,String valueType, boolean required) throws JSONException{
		JSONObject deneWord = new JSONObject();
		deneWord.put("Name", name);
		if(value!=null) {
			if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)) {
				double d = Double.parseDouble(value);
				deneWord.put("Value", d);
			}else if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)) {
				int i = Integer.parseInt(value);
				deneWord.put("Value", i);
			}else if(valueType.equals(TeleonomeConstants.DATATYPE_LONG)) {
				long l = Long.parseLong(value);
				deneWord.put("Value", l);
			}else {
				deneWord.put("Value", value);
			}

		}
		deneWord.put("Value Type", valueType);
		if(units!=null)deneWord.put("Units", units);
		deneWord.put("Required", required);
		return deneWord;

	}

	public static boolean removeChainFromNucleus(JSONObject pulseJSONObject, String nucleusName,  String deneChainName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		try {


			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");


			JSONObject aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					deneChainsArray.remove(i);
					return true;
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

	public static void addMutationToMutations(JSONObject pulseJSONObject,JSONObject mutation) {
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		JSONArray mutationsArray = denomeObject.getJSONArray("Mutations");
		mutationsArray.put(mutation);
	}

	public static boolean addDeneToMutationDeneChainByIdentity(JSONObject pulseJSONObject,JSONObject dene, Identity targetIdentity) {
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		JSONArray mutationsArray = denomeObject.getJSONArray("Mutations");
		String mutationName = targetIdentity.getNucleusName();
		String deneChainName = targetIdentity.getDenechainName();
		boolean toReturn =false;
		//
		// now parse them
		String name;
		JSONObject aJSONObject, selectedMutation = null;

		for(int i=0;i<mutationsArray.length();i++){
			aJSONObject = (JSONObject) mutationsArray.get(i);
			name = aJSONObject.getString("Name");

			if(name.equals(mutationName)){
				selectedMutation= aJSONObject;
			}
		}
		if(selectedMutation==null)return toReturn;
		JSONArray deneChainsArray = selectedMutation.getJSONArray("DeneChains");
		JSONObject aDeneJSONObject, deneChain;
		JSONArray denesJSONArray;
		String valueType, valueInString;
		Object object;
		boolean keepGoing=true;
		for(int i=0;i<deneChainsArray.length();i++){
			aJSONObject = (JSONObject) deneChainsArray.get(i);
			//System.out.println("removing dene, from denechain, aJSONObject=" + aJSONObject);
			if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
				deneChain = aJSONObject;
				denesJSONArray = deneChain.getJSONArray("Denes");
				denesJSONArray.put(dene);
				toReturn=true;
			} 
		}
		return toReturn;
	}


	public static void addDeneChainToNucleusByIdentity(JSONObject pulseJSONObject, JSONObject deneChain, Identity targetIdentity) throws InvalidDenomeException {
		addDeneChainToNucleusByIdentity(pulseJSONObject, deneChain,  targetIdentity.getNucleusName());
	}

	public static void addDeneChainToNucleusByIdentity(JSONObject pulseJSONObject, JSONObject deneChain, String nucleusName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		int toReturn =0;
		try {


			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			deneChainsArray.put(deneChain);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();

			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}

	}

	public static boolean addDeneWordToDeneByIdentity(JSONObject pulseJSONObject, JSONObject deneWord, Identity targetDeneChainidentity) throws InvalidDenomeException {
		return addDeneWordToDeneByIdentity(pulseJSONObject, deneWord,  targetDeneChainidentity.getNucleusName(), targetDeneChainidentity.getDenechainName(), targetDeneChainidentity.getDeneName());
	}

	public static boolean addDeneWordToDeneByIdentity(JSONObject pulseJSONObject, JSONObject deneWord, String nucleusName,  String deneChainName, String deneName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		boolean toReturn =false;
		try {
			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain;
			JSONArray denesJSONArray;
			String valueType, valueInString;
			Object object;
			boolean keepGoing=true;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("removing dene, from denechain, aJSONObject=" + aJSONObject);
				if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					denesJSONArray = deneChain.getJSONArray("Denes");
					done:
						for(int j=0;j<denesJSONArray.length();j++){
							aDeneJSONObject = denesJSONArray.getJSONObject(j);
							if(aDeneJSONObject.getString("Name").equals(deneName)){
								JSONArray denewordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
								denewordsJSONArray.put(deneWord);
								toReturn=true;
								break done;
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

	public static boolean removeDeneWordFromDeneByIdentity(JSONObject pulseJSONObject, Identity targetDeneWordIdentity) throws InvalidDenomeException {
		return removeDeneWordFromDeneByIdentity(pulseJSONObject,  targetDeneWordIdentity.getNucleusName(), targetDeneWordIdentity.getDenechainName(), targetDeneWordIdentity.getDeneName(), targetDeneWordIdentity.getDeneWordName());
	}

	public static boolean removeDeneWordFromDeneByIdentity(JSONObject pulseJSONObject, String nucleusName,  String deneChainName, String deneName, String deneWordName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		int toReturn =0;
		try {


			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain;
			JSONArray denesJSONArray;
			String valueType, valueInString;
			Object object;
			boolean keepGoing=true;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("removing dene, from denechain, aJSONObject=" + aJSONObject);
				if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					denesJSONArray = deneChain.getJSONArray("Denes");
					done:
						for(int j=0;j<denesJSONArray.length();j++){
							aDeneJSONObject = denesJSONArray.getJSONObject(j);
							if(aDeneJSONObject.getString("Name").equals(deneName)){
								JSONArray deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
								for(int k=0;k<deneWordsJSONArray.length();k++){
									JSONObject deneWord = (JSONObject) deneWordsJSONArray.get(k);
									if(deneWord.getString("Name").equals(deneWordName)){
										deneWordsJSONArray.remove(k);
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



	public static boolean addDeneToDeneChainByIdentity(JSONObject pulseJSONObject, JSONObject dene, Identity targetDeneChainidentity) throws InvalidDenomeException {
		return addDeneToDeneChainByIdentity(pulseJSONObject, dene,  targetDeneChainidentity.getNucleusName(), targetDeneChainidentity.getDenechainName());
	}

	public static boolean addDeneToDeneChainByIdentity(JSONObject pulseJSONObject, JSONObject dene, String nucleusName,  String deneChainName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		boolean toReturn =false;
		try {


			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain;
			JSONArray denesJSONArray;
			String valueType, valueInString;
			Object object;
			boolean keepGoing=true;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("removing dene, from denechain, aJSONObject=" + aJSONObject);
				if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					denesJSONArray = deneChain.getJSONArray("Denes");
					denesJSONArray.put(dene);
					toReturn=true;

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

	public void remveCodonFromDeneChain(JSONObject denechain, String codonName){

	}

	public static int remveCodonFromDeneChain(JSONObject pulseJSONObject, String nucleusName,  String deneChainName, String codonValue) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		int toReturn =0;
		try {
			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain;
			JSONArray denesJSONArray;
			String valueType, valueInString;
			Object object;
			boolean keepGoing=true;
			JSONArray deneWords;
			JSONObject deneWord;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("removing dene, from denechain, aJSONObject=" + aJSONObject);
				if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					denesJSONArray = deneChain.getJSONArray("Denes");

					do{
						//System.out.println("the size of the chain while removing codon " + codonValue + " is " + denesJSONArray.length());

						keepGoing=false;
						for(int j=0;j<denesJSONArray.length();j++){
							aDeneJSONObject = denesJSONArray.getJSONObject(j);
							//System.out.println("deneName=" + deneName  + " aDeneJSONObject.getStrin="+ aDeneJSONObject.getString("Name"));
							deneWords = aDeneJSONObject.getJSONArray("DeneWords");
							found:
								for(int k=0;k<deneWords.length();k++){
									deneWord = (JSONObject) deneWords.get(k);
									if(deneWord.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(TeleonomeConstants.CODON) && 
											deneWord.getString(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE).equals(codonValue) 	
											){
										//System.out.println("removed dene " + aDeneJSONObject.toString(4));
										denesJSONArray.remove(j);
										break found;
									}
								}
							keepGoing=true;
						}
						//	System.out.println("finshed loop keepGoing " + keepGoing );

					}while(keepGoing);
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

	public static int removeAllDenesFromChainByDeneType(JSONObject pulseJSONObject, String nucleusName,  String deneChainName, String deneType) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		int toReturn =0;
		try {
			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain;
			JSONArray denesJSONArray;
			String valueType, valueInString;
			Object object;
			boolean keepGoing=true;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("line 1106 removing dene, from denechain, aJSONObject=" + aJSONObject.getString("Name"));
				if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					while(keepGoing){
						keepGoing=false;
						denesJSONArray = deneChain.getJSONArray("Denes");
						for(int j=0;j<denesJSONArray.length();j++){
							aDeneJSONObject = denesJSONArray.getJSONObject(j);
							//System.out.println("line 1114 deneName=" + aDeneJSONObject.getString("Name")  + " aDeneJSONObject.getStrin="+ aDeneJSONObject.getString("Name"));
							if(aDeneJSONObject.has(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE) && 
									aDeneJSONObject.get(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE).equals(deneType)){
								denesJSONArray.remove(j);
								toReturn++;
								keepGoing=true;
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
		return toReturn;
	}
	public static int removeDeneFromChain(JSONObject pulseJSONObject, String nucleusName,  String deneChainName, String deneName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		//	System.out.println("line 1140 about to remove " + nucleusName + ":"  + deneChainName + ":"+ deneName);
		int toReturn =0;
		try {


			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			//System.out.println("removing dene, from denechain, selectedNucleus=" + selectedNucleus);
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain;
			JSONArray denesJSONArray;
			String valueType, valueInString;
			Object object;
			boolean keepGoing=true;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("removing dene, from denechain, aJSONObject=" + aJSONObject);
				if(aJSONObject.has("Name") && aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					//					while(keepGoing){
					//						keepGoing=false;
					denesJSONArray = deneChain.getJSONArray("Denes");
					//	System.out.println("line 1175 denesJSONArray.length()=" + denesJSONArray.length());

					done:
						for(int j=0;j<denesJSONArray.length();j++){
							aDeneJSONObject = denesJSONArray.getJSONObject(j);
							//	System.out.println("line 1177 deneName=" + deneName  + " aDeneJSONObject.getStrin="+ aDeneJSONObject.getString("Name"));
							if(aDeneJSONObject.getString("Name").equals(deneName)){
								//		System.out.println(" about line 1179 eneName=" + deneName  + " aDeneJSONObject.getStrin="+ aDeneJSONObject.getString("Name"));
								denesJSONArray.remove(j);
								toReturn++;
								//keepGoing=true;
								break done;
							}
						}
					//	System.out.println("line 1189 after removing");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = denesJSONArray.getJSONObject(j);
						//System.out.println("line 1192 deneName=" + deneName  + " aDeneJSONObject.getStrin="+ aDeneJSONObject.getString("Name"));
					}
					//					}
				} 
			}


		} catch (JSONException e) {
			// TODO Auto-generated catch block
			// TODO Auto-generated catch block
			Hashtable info = new Hashtable();
			System.out.println(Utils.getStringException(e));
			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return toReturn;
	}

	public static int removeDeneWordFromDeneByDeneWordType(JSONObject pulseJSONObject, String nucleusName,  String deneChainName, String deneName, String deneWordType) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		int deneWordsRemoved=0;
		//System.out.println("removeDeneWordFromDeneByDeneWordType,poin 0 deneName=" + deneName + " deneWordType="+ deneWordType);
		try {
			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");

				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject aDeneJSONObject, deneChain, deneWord;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;

			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				////System.out.println("removeDeneWordFromDeneByDeneWordType,poin 1 " + aJSONObject);
				if(!aJSONObject.has("Name")){
					//	//System.out.println("does not have name,poin 1a " + aJSONObject);
				}else if(aJSONObject.getString("Name").equals(deneChainName)){
					deneChain = aJSONObject;
					denesJSONArray = deneChain.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = denesJSONArray.getJSONObject(j);
						////System.out.println("removeDeneWordFromDeneByDeneWordType,poin 2 " + aDeneJSONObject);
						if(aDeneJSONObject.getString("Name").equals(deneName)){
							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							////System.out.println("deneWordsJSONArray,poin 2a size " + deneWordsJSONArray.length());

							for(int k=0;k<deneWordsJSONArray.length();k++){

								deneWord = deneWordsJSONArray.getJSONObject(k);
								//	//System.out.println("deneWordsJSONArray,poin 2b  " + deneWord);

								if(deneWord.has(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE) && deneWord.getString(TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE).equals(deneWordType)){
									deneWordsJSONArray.remove(k);
									System.out.println("removeDeneWordFromDeneByDeneWordType,poin 4 deneWord removed=" + deneWord.getString("Name"));
									deneWordsRemoved++;
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
			//System.out.println("deneWordsJSONArray,exception  2b  " + e);
			String m = "The denome is not formated Correctly. Error:" + e.getMessage() +" Stacktrace:" + ExceptionUtils.getStackTrace(e);
			info.put("message", m);
			throw new InvalidDenomeException(info);
		}
		return deneWordsRemoved;
	}

	public static boolean containsMutation(JSONObject pulseJSONObject, String mutationName){
		JSONObject mutationsObject = pulseJSONObject.getJSONObject("Denome");
		JSONArray mutationsArray = mutationsObject.getJSONArray("Mutations");
		String name;
		if(mutationsArray.length()>0) {
			for(int i=0;i<mutationsArray.length();i++){
				name =  mutationsArray.getJSONObject(i).getString("Name");
				if(name.equals(mutationName)){
					return true;
				}
			}
		}
		return false;
	}

	public static boolean containsDenomicElementByIdentity(JSONObject pulseJSONObject, Identity identity){

		String nucleusName=identity.getNucleusName();
		String deneChainName = identity.getDenechainName();
		String deneName = identity.getDeneName();
		String deneWordName = identity.getDeneWordName();
		JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject, selectedNucleus = null;
		JSONArray denesJSONArray, deneWordsJSONArray;
		String valueType, valueInString, name;
		Object object;

		JSONArray deneChainsArray=null;
		try {
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//System.out.println("nuclei name=" + name);
				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			if(selectedNucleus==null)return false;

			deneChainsArray = selectedNucleus.getJSONArray("DeneChains");

			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				if(aJSONObject.getString("Name").equals(deneChainName)){
					if(deneName.equals(""))return true;

					denesJSONArray = aJSONObject.getJSONArray("Denes");
					for(int j=0;j<denesJSONArray.length();j++){
						aDeneJSONObject = (JSONObject) denesJSONArray.get(j);

						if(aDeneJSONObject.getString("Name").equals(deneName)){
							if(deneWordName.equals(""))return true;

							deneWordsJSONArray = aDeneJSONObject.getJSONArray("DeneWords");
							for(int k=0;k<deneWordsJSONArray.length();k++){
								aDeneWordJSONObject = (JSONObject) deneWordsJSONArray.get(k);
								if(aDeneWordJSONObject.get("Name").equals(deneWordName)){
									return true;
								}

							}
						}
					}
				}
			}
		}catch(JSONException e){

		}
		return false;
	}

	public static JSONObject getDenomicElementByIdentity(JSONObject pulseJSONObject, Identity identity){

		String nucleusName=identity.getNucleusName();
		String deneChainName = identity.getDenechainName();
		String deneName = identity.getDeneName();
		String deneWordName = identity.getDeneWordName();
		JSONObject aJSONObject, aDeneJSONObject, aDeneWordJSONObject, selectedNucleus = null;
		JSONArray denesJSONArray, deneWordsJSONArray;
		String valueType, valueInString, name;
		Object object;

		JSONArray deneChainsArray=null;
		try {
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//System.out.println("nuclei name=" + name);
				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			if(selectedNucleus==null)return null;

			deneChainsArray = selectedNucleus.getJSONArray("DeneChains");

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
	public static JSONObject getDeneByIdentity(JSONObject pulseJSONObject, Identity deneIdentity) throws InvalidDenomeException{
		return getDeneByPointer(pulseJSONObject, deneIdentity.getNucleusName(),deneIdentity.getDenechainName(), deneIdentity.getDeneName());
	}

	public static String getTeleonomeName(JSONObject pulseJSONObject) throws JSONException {
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		return denomeObject.getString("Name");
	}

	public static JSONObject getDeneByPointer(JSONObject pulseJSONObject, String nucleusName,String deneChainName, String deneName) throws InvalidDenomeException{
		JSONArray deneChainsArray=null;
		//
		// now parse them

		String name;
		JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;
		try{
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//System.out.println("nuclei name=" + name);
				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			if(selectedNucleus==null)return null;
			deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
			JSONObject  aDeneJSONObject, aDeneWordJSONObject;
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

	public static JSONObject getDeneChainByIdentity(JSONObject pulseJSONObject, Identity identity) throws InvalidDenomeException {
		return getDeneChainByName( pulseJSONObject, identity.getNucleusName(), identity.getDenechainName()); 
	}

	public static JSONArray getAllDeneChainNamesForNucleus(JSONObject pulseJSONObject, String nucleusName) {
		JSONArray deneChains = getAllDeneChainsForNucleus( pulseJSONObject,  nucleusName);
		JSONArray toReturn = new JSONArray();
		JSONObject deneChain;
		for(int i=0;i<deneChains.length();i++){
			deneChain = (JSONObject) deneChains.get(i);
			toReturn.put(deneChain.getString("Name"));
		}
		return toReturn;
	}

	public static JSONArray getAllDeneChainsForNucleus(JSONObject pulseJSONObject, String nucleusName) {
		// TODO Auto-generated method stub
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String name;
		JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			//System.out.println("nuclei name=" + name);
			if(name.equals(nucleusName)){
				selectedNucleus= aJSONObject;
			}
		}
		if(selectedNucleus==null)return null;
		JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
		return deneChainsArray;
	}
	//	public static JSONArray getAllDeneNamesForDeneChain(JSONObject pulseJSONObject, String nucleusName, String deneChainName) {
	//		JSONArray denes = getAllDenesForDeneChain( pulseJSONObject,  nucleusName,  deneChainName);
	//		JSONArray toReturn = new JSONArray();
	//		JSONObject dene;
	//		for(int j=0;j<denes.length();j++) {
	//			 dene = denes.getJSONObject(j);
	//			toReturn.put(dene.getString("Name"));
	//		}
	//		return toReturn;
	//	}
	public static JSONArray getAllDeneNamesForDeneChain(JSONObject pulseJSONObject, String nucleusName, String deneChainName) {
		// TODO Auto-generated method stub
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String name;
		JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			//System.out.println("nuclei name=" + name);
			if(name.equals(nucleusName)){
				selectedNucleus= aJSONObject;
			}
		}
		if(selectedNucleus==null)return null;
		JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
		JSONArray deneNamesArrays = new JSONArray();
		for(int i=0;i<deneChainsArray.length();i++){
			aJSONObject = (JSONObject) deneChainsArray.get(i);
			//System.out.println("aJSONObject.getString()=" + aJSONObject.getString("Name") + " deneChainName=" + deneChainName);
			if(aJSONObject.getString("Name").equals(deneChainName)){
				JSONArray denes = aJSONObject.getJSONArray("Denes");
				for(int j=0;j<denes.length();j++) {
					//System.out.println("deneNamesArrays)=" + deneNamesArrays + " denes.getJSONObject(j)=" + denes.getJSONObject(j));
					deneNamesArrays.put(denes.getJSONObject(j).getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
				}
			}
		}

		return deneNamesArrays;
	}

	//	public static JSONArray getAllDeneWordNamesForDene(JSONObject pulseJSONObject, String nucleusName, String deneChainName, String deneName) {
	//		JSONArray deneWords = getAllDeneWordsForDene( pulseJSONObject,  nucleusName,  deneChainName, deneName);
	//		JSONArray toReturn = new JSONArray();
	//		JSONObject deneWord;
	//		for(int j=0;j<deneWords.length();j++) {
	//			deneWord = deneWords.getJSONObject(j);
	//			toReturn.put(deneWord.getString("Name"));
	//		}
	//		return toReturn;
	//	}

	public static JSONArray getAllDeneWordNamesForDene(JSONObject pulseJSONObject, String nucleusName, String deneChainName, String deneName) {
		// TODO Auto-generated method stub
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
		String name;
		JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

		for(int i=0;i<nucleiArray.length();i++){
			aJSONObject = (JSONObject) nucleiArray.get(i);
			name = aJSONObject.getString("Name");
			//System.out.println("nuclei name=" + name);
			if(name.equals(nucleusName)){
				selectedNucleus= aJSONObject;
			}
		}
		if(selectedNucleus==null)return null;
		JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");
		JSONArray deneWordNamesArrays = new JSONArray();
		for(int i=0;i<deneChainsArray.length();i++){
			aJSONObject = (JSONObject) deneChainsArray.get(i);
			//System.out.println("aJSONObject.getString()=" + aJSONObject.getString("Name") + " deneChainName=" + deneChainName);
			if(aJSONObject.getString("Name").equals(deneChainName)){
				JSONArray denes = aJSONObject.getJSONArray("Denes");
				for(int j=0;j<denes.length();j++) {
					if(denes.getJSONObject(j).getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE).equals(deneName)) {
						JSONObject dene = denes.getJSONObject(j);
						JSONArray deneWords = dene.getJSONArray("DeneWords");
						for(int k=0;k<deneWords.length();k++) {
							deneWordNamesArrays.put(deneWords.getJSONObject(k).getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE));
						}
					}
				}
			}
		}

		return deneWordNamesArrays;
	}

	public static JSONObject getDeneChainByName(JSONObject pulseJSONObject, String nucleusName,  String deneChainName) throws InvalidDenomeException {
		// TODO Auto-generated method stub
		try {


			//
			// now parse them
			JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
			JSONArray nucleiArray = denomeObject.getJSONArray("Nuclei");
			String name;
			JSONObject aJSONObject, selectedNucleus = null,purposeNucleus = null;

			for(int i=0;i<nucleiArray.length();i++){
				aJSONObject = (JSONObject) nucleiArray.get(i);
				name = aJSONObject.getString("Name");
				//System.out.println("nuclei name=" + name);
				if(name.equals(nucleusName)){
					selectedNucleus= aJSONObject;
				}
			}
			if(selectedNucleus==null)return null;
			JSONArray deneChainsArray = selectedNucleus.getJSONArray("DeneChains");

			//System.out.println("selectedNucleus=" + selectedNucleus);
			JSONObject aDeneJSONObject, aDeneWordJSONObject;
			JSONArray denesJSONArray, deneWordsJSONArray;
			String valueType, valueInString;
			Object object;
			for(int i=0;i<deneChainsArray.length();i++){
				aJSONObject = (JSONObject) deneChainsArray.get(i);
				//System.out.println("aJSONObject.getString()=" + aJSONObject.getString("Name") + " deneChainName=" + deneChainName);
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

	/**
	 * this methods takes a denome and updates a deneword value  it then returns the updated denome
	 * @param sourceData - the denome to update
	 * @param pointerToDeneWord - the pointer to the deneword to update
	 * @param valueToUpdate - the value
	 * @return the update sourceData
	 * @throws InvalidDenomeException
	 */
	public static JSONObject updateDeneWordByIdentity(JSONObject sourceData, String pointerToDeneWord, Object valueToUpdate) throws InvalidDenomeException{
		String[] tokens = pointerToDeneWord.substring(1,pointerToDeneWord.length()).split(":");

		String teleonomeName = tokens[0];
		String nucleusName = tokens[1];
		String deneChainName = tokens[2];
		String deneName = tokens[3];
		String deneWordLabel = tokens[4];
		System.out.println("nucleusName="+nucleusName+" deneChainName:"+deneChainName+" deneName " + deneName + " deneWordLabel " + deneWordLabel);
		JSONObject aJSONObject, cpInternalNucleus=null,cpPurposeNucleus=null, cpMnemosyneNucleus=null, cpHumanInterfaceNucleus=null;

		JSONObject denomeObject;
		try {
			denomeObject = sourceData.getJSONObject("Denome");
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
									System.out.println("Updated " + deneWordLabel + " to a value of " + valueToUpdate);
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
		return sourceData;
	}

	// **********************************sdhfkjsdfldjfklsdjflksdjfsdkljflsdkjf

	// * ****************************
	public static ArrayList generateDenomePhysiologyReportHTMLTable(JSONObject pulse) throws MissingDenomeException, TeleonomeValidationException {
		ArrayList<String> reportLines = new ArrayList();
		SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss Z");
		com.teleonome.framework.denome.DenomeViewManager aDenomeViewerManager = new com.teleonome.framework.denome.DenomeViewManager();


		JSONObject denomeObject =  pulse.getJSONObject("Denome");
		String teleonomeName = denomeObject.getString("Name");
		aDenomeViewerManager.loadDenome(pulse);
		JSONArray stateMutationsJSONArray = aDenomeViewerManager
				.getMutations(TeleonomeConstants.MUTATION_TYPE_STATE);
		JSONArray structureMutationsJSONArray = aDenomeViewerManager
				.getMutations(TeleonomeConstants.MUTATION_TYPE_STRUCTURE);
		int structureMutationsJSONArrayLength = 0;
		if (structureMutationsJSONArray != null)
			structureMutationsJSONArrayLength = structureMutationsJSONArray.length();
		int stateMutationsJSONArrayLength = 0;
		if (stateMutationsJSONArray != null)
			stateMutationsJSONArrayLength = stateMutationsJSONArray.length();

		Hashtable pointerToMicroControllerSensorDenesVectorIndex= aDenomeViewerManager.getPointerToMicroControllerSensorsDeneWordsBySensorRequestQueuePositionIndex();
		ArrayList<Map.Entry<String, Integer>>  microControllerPointerProcessingQueuePositionIndex = aDenomeViewerManager.getMicroControllerPointerProcessingQueuePositionIndex();
		Vector sensorDeneVector;
		JSONObject sensorDeneJSONObject;

		int numberOfMCU=microControllerPointerProcessingQueuePositionIndex.size();

		reportLines.add("<!DOCTYPE html>");
		reportLines.add("<html>");
		reportLines.add("<head>");
		reportLines.add("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.4.0/css/bootstrap.min.css\">");
		reportLines.add("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js\"></script>");
		reportLines.add("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.4.0/js/bootstrap.min.js\"></script>");
		reportLines.add("<meta charset=\"utf-8\">");
		reportLines.add("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">");

		reportLines.add("<style>");


		reportLines.add(" .disabled {");
		reportLines.add("     pointer-events:none; ");
		reportLines.add("    opacity:0.6;        "); 
		reportLines.add("}");

		reportLines.add(" .breadcrumb    {");
		reportLines.add("    background-color: white;");
		reportLines.add("}");
		reportLines.add(".CloseDetail{");
		reportLines.add("    margin-top:-20px;");
		reportLines.add("         }");
		reportLines.add("  .panel-heading-microcontroller{");

		reportLines.add(" }");

		reportLines.add(" .panel-default > .panel-heading-microcontroller {");
		reportLines.add("    background: rgb(185, 240, 174); ");
		reportLines.add("     color: #000; ");
		reportLines.add(" }");
		reportLines.add("	#Detail-Area{");
		reportLines.add("   	margin-top:10px;");
		reportLines.add("   	z-index: 100;");
		reportLines.add("   }");
		reportLines.add("   .sensor{");
		reportLines.add("   	margin-top:20px;");
		reportLines.add("   }");
		reportLines.add("   .ActionTitle{");
		reportLines.add("   	font-size:24px;");
		reportLines.add("   }");
		reportLines.add("   .ActionDetailSection{");
		reportLines.add("    	margin-top:0px;");
		reportLines.add("   	margin-left:20px;");
		reportLines.add("  		margin-right:20px;");
		reportLines.add("    }");
		reportLines.add("    .actuator-selector{");
		reportLines.add(" 		margin-top:20px;");
		reportLines.add("     }");
		reportLines.add("     .actuator-level{");
		reportLines.add("		margin-left:100px;");
		reportLines.add("	}");

		reportLines.add("    .dropdown-submenu {");
		reportLines.add("        position: relative;");
		reportLines.add("     }");

		reportLines.add("   .dropdown-submenu .dropdown-menu {");
		reportLines.add("     	top: 0;");
		reportLines.add("    	left: 100%;");
		reportLines.add("     	margin-top: -1px;");
		reportLines.add("   }");

		reportLines.add("	.ReportTable {");
		reportLines.add("  		font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif;");
		reportLines.add("  		border-collapse: collapse;");
		reportLines.add("  		width: 100%;");
		reportLines.add("	}");

		reportLines.add("	.ReportTable td, #ReportTable th {");
		reportLines.add("  		border: 1px solid #ddd;");
		reportLines.add("	  	padding: 8px;");
		reportLines.add("	}");

		reportLines.add("	.ReportTable tr:nth-child(even){background-color: #f2f2f2;}");

		reportLines.add("	.ReportTable tr:hover {background-color: #ddd;}");

		reportLines.add("	.ReportTable th {");
		reportLines.add("		padding-top: 12px;");
		reportLines.add("  		padding-bottom: 12px;");
		reportLines.add("  		text-align: left;");
		reportLines.add("  		background-color: #4CAF50;");
		reportLines.add("  		color: white;");
		reportLines.add("	}");
		reportLines.add("</style>");
		reportLines.add("</head>");
		reportLines.add("<body>");

		int startingX = 400;
		int startingY = 30;
		int mainHeight= 500;
		int arrowHeadHeight=10;
		int arrowHeadWidth=10;
		int lineArrowLength=50;
		int mcuBoxWidth=180;
		//      250                 180           50                  20
		int mcuBoxTotalWidth=mcuBoxWidth+ lineArrowLength + 2*arrowHeadWidth;
		int mcuBoxHeight=50;
		
		
		int startYValueForMCUBox=100;
		int nameMargin=10;
		int textY=130;
		
		
		int mcuLineYPos=startYValueForMCUBox + mcuBoxHeight/2;

		

		int externalDataX=100;
		int externalDataY=100;
		int externalDataText=50;
		int externalDataLineX1=externalDataX + mcuBoxWidth;
		int arrowGoingDownLength=50;
		
		int boundingRectangleX=50;
		int boundingRectangleY=50;
		 //                                                   250
		int boundingRectangleWidth= 20 + mcuBoxTotalWidth* (numberOfMCU + 1) ;
		int boundingRectangleHeight=boundingRectangleY + 2*mcuBoxHeight + arrowGoingDownLength +  boundingRectangleY;
		
		
		int mainWidth=2*boundingRectangleWidth + 20;
		
		int startXValueForMCUBox=externalDataLineX1+lineArrowLength;
		
		//                100                    50                50                    20
		int secondRowY= startYValueForMCUBox + mcuBoxHeight + arrowGoingDownLength + arrowHeadHeight;
		//                    220           25             
		int secondRowTextY = 5+secondRowY + mcuBoxHeight/2 ;

		reportLines.add("<svg  id=\"MainDiagram\" height=\""+ mainHeight+"\" width=\""+ mainWidth+"\">");
		reportLines.add("<defs>");
		reportLines.add("<marker id=\"arrow\" markerWidth=\"10\" markerHeight=\"10\" refX=\"0\" refY=\"3\" orient=\"auto\" markerUnits=\"strokeWidth\" viewBox=\"0 0 10 10\">");
		reportLines.add("<path d=\"M0,0 L0,6 L9,3 z\" fill=\"#00\" />");
		reportLines.add("</marker>");
		reportLines.add("</defs>");
		reportLines.add("<g>");

		reportLines.add("<text x=\""+startingX+"\" y=\""+startingY+"\" font-family=\"Verdana\" font-size=\"20\" >"+ teleonomeName+"</text>");

		reportLines.add("<rect x=\""+boundingRectangleX+"\" y=\""+boundingRectangleY+"\" width=\""+boundingRectangleWidth +"\" height=\""+boundingRectangleHeight+"\" rx=\"11\" ry=\"11\" style=\"fill: rgb(232, 237, 238);; stroke: black; stroke-width: 4px;\"/>");

		reportLines.add("<rect  x=\""+externalDataX+"\" y=\""+externalDataY+"\" width=\""+mcuBoxWidth +"\" height=\""+mcuBoxHeight+" \" rx=\"11\" ry=\"11\" style=\"fill: rgb(174, 235, 240);; stroke: black; stroke-width: 4px;\"/>");
		reportLines.add("<text x=\""+(externalDataX+nameMargin)+"\" y=\""+textY+"\" font-family=\"Verdana\" font-size=\"12\" >External Data</text>");
		reportLines.add("<line x1=\""+externalDataLineX1+"\" y1=\""+mcuLineYPos+"\" x2=\""+startXValueForMCUBox+"\" y2=\""+ mcuLineYPos +"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");

		Integer queuePosition=null;
		String microControllerPointer;
		String microControllerName, microControllerNameNoSpaces;
		int counter=0;
		int currentX=0;
		int lineX1=0;
		int lineX2=0;
		for (Map.Entry<String, Integer> entry : microControllerPointerProcessingQueuePositionIndex) {
			microControllerPointer = (String)entry.getKey();
			queuePosition = (Integer)entry.getValue();
			microControllerName = microControllerPointer.split(":")[microControllerPointer.split(":").length-1];
			currentX = startXValueForMCUBox +(counter*mcuBoxTotalWidth);
			lineX1=currentX + mcuBoxWidth;
			lineX2=lineX1+lineArrowLength;

			reportLines.add("<rect class=\"Microcontroller\" data-name=\""+ microControllerName +"\"  x=\""+currentX+"\" y=\""+startYValueForMCUBox+"\" width=\""+mcuBoxWidth +"\" height=\""+mcuBoxHeight+"\" rx=\"11\" ry=\"11\" style=\"fill: rgb(185, 240, 174); stroke: black; stroke-width: 4px;\"/>");
			reportLines.add("<text class=\"Microcontroller\" data-name=\""+ microControllerName +"\"  x=\""+ (currentX +nameMargin) +"\" y=\""+textY+"\" font-family=\"Verdana\" font-size=\"12\" >"+microControllerName+"</text>");
			counter++;
			//
			// increment the counter first, because the line for the last mcu does not go to the end and moves to the right
			// instead the x is half way throught the box and it goes down
			if(counter<numberOfMCU) {
				reportLines.add("<line x1=\""+lineX1+"\" y1=\""+mcuLineYPos+"\" x2=\""+lineX2+"\" y2=\""+mcuLineYPos+"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");
			}else {
				lineX1=currentX + mcuBoxWidth/2;
				lineX2=lineX1;
				int lastLineY1=startYValueForMCUBox + mcuBoxHeight;
				int lastLineY2=lastLineY1 + arrowGoingDownLength;
				reportLines.add("<line x1=\""+lineX1+"\" y1=\""+lastLineY1+"\" x2=\""+lineX2+"\" y2=\""+lastLineY2+"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");

			}
		}

		//
		// at this point currentX is the beginning of the last box of the first row
		// and the beginning of the first box f the second row

		
		reportLines.add("<rect x=\""+currentX + "\" y=\""+ secondRowY +"\" width=\""+mcuBoxWidth +"\" height=\""+mcuBoxHeight+"\" rx=\"11\" ry=\"11\" style=\"fill: rgb(241, 220, 159); stroke: black; stroke-width: 4px;\"/>");
		reportLines.add("<text x=\""+( currentX + nameMargin)+"\" y=\""+secondRowTextY + "\" font-family=\"Verdana\" font-size=\"12\" >Analyticons</text>");
		reportLines.add("<line x1=\""+ currentX +"\" y1=\""+ secondRowTextY+"\" x2=\""+(currentX-lineArrowLength) +"\" y2=\""+secondRowTextY+"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");


		currentX -= mcuBoxTotalWidth;
		reportLines.add("<rect x=\""+currentX+ "\" y=\""+secondRowY+"\" width=\""+mcuBoxWidth +"\" height=\""+ mcuBoxHeight + "\" rx=\"11\" ry=\"11\" style=\"fill: rgb(241, 159, 223); stroke: black; stroke-width: 4px;\"/>");
		reportLines.add("<text x=\""+ (currentX+nameMargin) + "\" y=\""+secondRowTextY+"\" font-family=\"Verdana\" font-size=\"12\" >Mnemosycons</text>");
		//      460 = 500-40
		int endOfArrowToAsyncBoxX = currentX-lineArrowLength-arrowHeadWidth;

		reportLines.add("<line x1=\""+currentX+"\" y1=\""+ secondRowTextY +"\" x2=\""+ endOfArrowToAsyncBoxX +"\" y2=\""+secondRowTextY+"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");

		//
		// the asynchrnous is the last box of the second row
		// its length will be the length needed to return to the middle of the external data to make the return up arrow
		// that point is defined by
		int middleOfExternalDataBoxX = externalDataX + mcuBoxWidth/2; // 100+75
		//    285=460-175
		int asyncBoxWidth = endOfArrowToAsyncBoxX - middleOfExternalDataBoxX-2*lineArrowLength;
		int asyncBoxStartingX = endOfArrowToAsyncBoxX-asyncBoxWidth-lineArrowLength;
		reportLines.add("<rect x=\""+asyncBoxStartingX+"\" y=\""+secondRowY+"\" width=\""+asyncBoxWidth+"\" height=\""+mcuBoxHeight+"\" rx=\"11\" ry=\"11\" style=\"fill: rgb(241, 192, 159); stroke: black; stroke-width: 4px;\"/>");
		reportLines.add("<text x=\""+(asyncBoxStartingX + nameMargin)+"\" y=\""+secondRowTextY+"\" font-family=\"Verdana\" font-size=\"12\" >Asynchronous Period (60 seconds)</text>");

		reportLines.add("<line x1=\""+ asyncBoxStartingX +"\" y1=\""+ secondRowTextY +"\" x2=\""+middleOfExternalDataBoxX+"\" y2=\""+secondRowTextY+"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");
		//
		//back up to external data
		// 170
		int bottomOfExternalDataY = externalDataY + mcuBoxHeight+2*arrowHeadHeight;
		
		reportLines.add("<line x1=\""+(middleOfExternalDataBoxX-arrowHeadWidth)+"\" y1=\""+ secondRowTextY +"\" x2=\""+ (middleOfExternalDataBoxX-arrowHeadWidth)+"\" y2=\""+bottomOfExternalDataY+"\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrow)\" />");
		reportLines.add("</g>");
		reportLines.add("</svg>");

		reportLines.add("<div class=\"container\" id=\"Detail-Area\">"); 
		//
		// now loop over the microcontrollers to render every one
		//
		JSONObject  sensorValueDeneJSONObject;
		JSONArray sensorValuesPointersJSONArray, sensorValuesJSONArray;
		Integer sensorRequestQueuePosition;
		//ArrayList sensorTableBySensorArrayList = new ArrayList();
		ArrayList actuatorTableByActuatorArrayList = new ArrayList();
		JSONObject anActuatorActionListDeneJSONObject, actionJSONObject;
		JSONArray pointersToActionsJSONArray, actionsJSONArray;
		JSONArray actuatorActionConditionPointersJSONArray;
		String actuatorActionConditionPointer;
		String conditionName, reportingAddress;
		JSONObject reportingAddressDeneWord, actuatorActionConditionJSONObject, actuatorDene = null;
		String actuatorName, actuatorPointer, actionType;
		int actuatorExecution;
		ArrayList<Map.Entry<JSONObject, Integer>> actuatorExecutionPositionActionListDeneIndex;
		String unitsText;
		//JSONObject sensorReportLine;
		Map.Entry<JSONObject, Integer> valueMap;
		String sensorValueName, sensorName;
		String variableName,variableValue, defaultValue;
		Hashtable microControllerNameActuatorsIndex = aDenomeViewerManager.getMicroControllerNameActuatorsIndex();

		ArrayList<Map.Entry<JSONObject, Integer>> sensorRequestQueuePositionDeneWordIndex; 
		ArrayList<Map.Entry<JSONObject, Integer>> actuatorRequestQueuePositionDeneWordIndex = new ArrayList(); 
		pointerToMicroControllerSensorDenesVectorIndex = aDenomeViewerManager.getPointerToMicroControllerSensorDenesVectorIndex();
		Vector allSensorDenesForMicroController;
		JSONObject aSensorDeneJSONObject;
		boolean isSensor;

		for (Map.Entry<String, Integer> entry : microControllerPointerProcessingQueuePositionIndex) {
			microControllerPointer = (String)entry.getKey();
			queuePosition = (Integer)entry.getValue();

			microControllerName = microControllerPointer.split(":")[microControllerPointer.split(":").length-1];
			microControllerNameNoSpaces= microControllerName.replace(" ", "");
			logger.info("line 2414,pointerToMicroController=" + microControllerPointer );
			sensorRequestQueuePositionDeneWordIndex = (ArrayList)pointerToMicroControllerSensorDenesVectorIndex.get( microControllerPointer);

			allSensorDenesForMicroController = (Vector)pointerToMicroControllerSensorDenesVectorIndex.get(microControllerPointer);
			if(allSensorDenesForMicroController!=null) {
				logger.debug("line 2422 allSensorDenesForMicroController="+ allSensorDenesForMicroController.size() );
			}else {
				logger.debug("line 2422 allSensorDenesForMicroController is null");
			}
			//					



			reportLines.add("	<div class=\"row Microcontroller-Details\"  id=\""+microControllerNameNoSpaces+"-Details\">");
			reportLines.add("		<div class=\"panel panel-default\">");
			//
			// panel heading
			//
			reportLines.add("			<div class=\"panel-heading panel-heading-microcontroller\">");
			reportLines.add("				<div class=\"col-10 text-center\">");  
			reportLines.add("					"+microControllerName );
			reportLines.add("				</div>");
			reportLines.add("				<div class=\"col-2\"> ");  
			reportLines.add("					<button type=\"button\" class=\"btn-md pull-right CloseDetail\" >&times;</button>");
			reportLines.add("				</div>");
			reportLines.add("			</div>");

			//
			// panel body
			//
			reportLines.add("			<div class=\"panel-body\">");
			reportLines.add("				<ul class=\"nav nav-tabs justify-content-center\">");  
			reportLines.add("					<li class=\"nav-item\">");
			reportLines.add("						<a class=\"nav-link active\" href=\"#\" onclick=\"return show_detail('"+microControllerNameNoSpaces+"-SensorDetails');\">Sensors</a>");
			reportLines.add("					</li>");
			reportLines.add("					<li class=\"nav-item\">");
			reportLines.add("						<a class=\"nav-link\" href=\"#\" onclick=\"return show_detail('"+microControllerNameNoSpaces+"-ActuatorDetails');\">Actuators</a>");
			reportLines.add("					</li>");
			reportLines.add("				</ul>");

			reportLines.add("				<div class=\"SensorDetail\" id=\""+microControllerNameNoSpaces+"-SensorDetails\" style=\"display: none;\"> ");  			

			if(allSensorDenesForMicroController!=null && allSensorDenesForMicroController.size()>0) {
				reportLines.add("					<div class=\"row\">");
				//
				// draw every sensor with a 3 column button
				for(int j=0;j<allSensorDenesForMicroController.size();j++){
					aSensorDeneJSONObject = (JSONObject) allSensorDenesForMicroController.elementAt(j);
					isSensor = DenomeUtils.isDeneOfType(aSensorDeneJSONObject, TeleonomeConstants.DENE_TYPE_SENSOR);
					logger.info("line 491 aDeneJSONObject.=" + aSensorDeneJSONObject.getString("Name") + " isSensor="+isSensor);
					if(isSensor){
						reportLines.add("						<div class=\"col-sm-3 sensor\">");
						reportLines.add("							<button class=\"btn btn-primary dropdown-toggle sensor\" type=\"button\">"+aSensorDeneJSONObject.getString("Name") +" </button>");
						reportLines.add("						</div>");
					}
				}

				reportLines.add("					</div>");
				reportLines.add("					<div class=\"row\">");
				reportLines.add("						<hr class=\"col-xs-12\"  >");
				reportLines.add("					</div>");// 

				reportLines.add("					Sensor Values in order of GetSensorData String");
				if(sensorRequestQueuePositionDeneWordIndex!=null){
					reportLines.add("<h3>Sensors</h3><br>");
					reportLines.add("<table class=\"ReportTable\">");
					reportLines.add("<tr><th>Sensor Name</th><th>Value</th><th>"+TeleonomeConstants.DENEWORD_SENSOR_REQUEST_QUEUE_POSITION+"</th><th>"+TeleonomeConstants.DENEWORD_UNIT_ATTRIBUTE+"</th><th>"+TeleonomeConstants.DENEWORD_REPORTING_ADDRESS+"</th></tr>");

					logger.debug("line 2229 sensorRequestQueuePositionDeneWordIndex=" + sensorRequestQueuePositionDeneWordIndex.size());
					for (int m=0;m<sensorRequestQueuePositionDeneWordIndex.size();m++){
						//					sensorDeneJSONObject = ((Map.Entry<JSONObject, Integer>)sensorRequestQueuePositionDeneWordIndex.get(m)).getKey();
						//					sensorValuesPointersJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(sensorDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_SENSOR_VALUE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						//					logger.debug("line 2246 sensorValuesPointersJSONArray=" + sensorValuesPointersJSONArray);
						//					sensorValuesJSONArray = aDenomeViewerManager.loadDenesFromPointers(sensorValuesPointersJSONArray);
						//					
						//					logger.debug("line 2248 sensorValuesJSONArray=" + sensorValuesJSONArray.length());
						//					for(int k=0;k<sensorValuesJSONArray.length();k++){
						//										value = sensorValuesJSONArray.getJSONObject(k);
						valueMap = (Map.Entry<JSONObject, Integer>)sensorRequestQueuePositionDeneWordIndex.get(m);
						sensorValueDeneJSONObject =valueMap.getKey();
						logger.debug("line 2256,  sensorValueDeneJSONObject=" + sensorValueDeneJSONObject);
						sensorValueName = sensorValueDeneJSONObject.getString("Name");
						logger.debug("line 2258,  sensorValueName=" + sensorValueName);
						sensorName = (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(sensorValueDeneJSONObject, TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("line 2260,  sensorName=" + sensorName);

						sensorRequestQueuePosition = valueMap.getValue();
						logger.debug("line 2264 sensorRequestQueuePosition=" + sensorRequestQueuePosition);
						reportingAddress = (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(sensorValueDeneJSONObject, TeleonomeConstants.DENEWORD_REPORTING_ADDRESS, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("line 2266 reportingAddress=" + reportingAddress);
						unitsText="";
						try {
							reportingAddressDeneWord = (JSONObject) DenomeUtils.getDeneWordByIdentity(pulse, new Identity(reportingAddress), TeleonomeConstants.COMPLETE);
							logger.debug("line 2270 reportingAddressDeneWord=" + reportingAddressDeneWord);
							unitsText = reportingAddressDeneWord.getString( TeleonomeConstants.DENEWORD_UNIT_ATTRIBUTE);
							logger.debug("line 2272 unitsText=" + unitsText);
						} catch (InvalidDenomeException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
						reportLines.add("<tr><td>"+ sensorName + "</td><td>"+sensorValueName+"</td><td>"+sensorRequestQueuePosition+"</td><td>"+unitsText+"</td><td>"+reportingAddress+"</td></tr>");		
						logger.debug("line 2264");

						//}
					}
					reportLines.add("</table>");
				}
			}else {
				logger.debug("line 2244 sensorDeneVector is null");
				reportLines.add("<h3>No Sensors</h3><br>");

			}



			reportLines.add("				</div>"); //class=\"SensorDetail\ 

			
			//
			// create an element for every actuator in the microcontroller
			//
			JSONObject actionInfo;
			JSONArray actionSectionInfoJSONArray = new  JSONArray();
			actuatorExecutionPositionActionListDeneIndex = (ArrayList<Map.Entry<JSONObject, Integer>>)microControllerNameActuatorsIndex.get(microControllerPointer);
			if(actuatorExecutionPositionActionListDeneIndex!=null && actuatorExecutionPositionActionListDeneIndex.size()>0){
				
			reportLines.add("				<div class=\"ActuatorDetail\" id=\""+microControllerNameNoSpaces+"-ActuatorDetails\" style=\"display: none;\"> ");  

				
				counter=0;
				for(Map.Entry<JSONObject, Integer> entry4 : actuatorExecutionPositionActionListDeneIndex) {
					anActuatorActionListDeneJSONObject = entry4.getKey();
					//logger.debug("line 2545 anActuatorActionListDeneJSONObject=" + anActuatorActionListDeneJSONObject.toString(4));

					//logger.debug("anActuatorActionListDeneJSONObject=" + anActuatorActionListDeneJSONObject);
					//
					// the codon is the name of the actuator
					//
					actuatorName = (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(anActuatorActionListDeneJSONObject, TeleonomeConstants.CODON, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("line 2552 anActuatorActionListDeneJSONObject=" + anActuatorActionListDeneJSONObject.getString("Name"));
					//
					// then get the actuator dene
					actuatorPointer = new Identity(teleonomeName, TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_ACTUATORS, actuatorName).toString();
					try {
						logger.debug("actuatorPointer=" + actuatorPointer);
						actuatorDene = DenomeUtils.getDeneByIdentity(pulse, new Identity(actuatorPointer));
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
					actuatorName = actuatorDene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
					actuatorExecution = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorDene, TeleonomeConstants.DENEWORD_EXECUTION_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					reportLines.add("				<div class=\"row\">");

					//
					// dont add an arrow for the first 
					if(counter>0) {
						//
						// the arrow
						//
						reportLines.add("					<div class=\"col-sm-1\">");
						reportLines.add("						<svg   id=\"arrow1\" height=\"150\" width=\"250\">");
						reportLines.add("							<defs>");
						reportLines.add("								<marker id=\"arrowHead\" markerWidth=\"10\" markerHeight=\"10\" refX=\"0\" refY=\"3\" orient=\"auto\" markerUnits=\"strokeWidth\" viewBox=\"0 0 10 10\">");
						reportLines.add("						        	<path d=\"M0,0 L0,6 L9,3 z\" fill=\"#00\" />");
						reportLines.add("								</marker>");
						reportLines.add("							</defs>");
						reportLines.add("							<g>");
						reportLines.add("							<line x1=\"0\" y1=\"35\" x2=\"50\" y2=\"35\" style=\"stroke:#000; stroke-width:2\" marker-end=\"url(#arrowHead)\" />");
						reportLines.add("							</g>");
						reportLines.add("						</svg>");
						reportLines.add("					</div>"); //<div class=\"col-sm-1\">");
					}

					counter++;


					reportLines.add("					<div class=\"col-sm-2  dropdown\">");
					reportLines.add("						<button class=\"btn btn-primary dropdown-toggle actuator-selector\" type=\"button\" data-toggle=\"dropdown\">" + actuatorExecution  + "-" + actuatorName);
					reportLines.add("						<span class=\"caret\"></span></button>");
					reportLines.add("						<ul class=\"col-sm-12 text-center dropdown-menu actuator-level\">");
					reportLines.add("							<li><a tabindex=\"-1\"  href=\"#\"  class=\"disabled\" >Select Action</a></li>");
					String actuatorCommandTrueExpression, actuatorCommandFalseExpression, executionPoint,actionName, actionNameNoSpaces, actionExpression;
					int actionEvaluationPosition;

					// loop over every action of the actuator
					pointersToActionsJSONArray = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(anActuatorActionListDeneJSONObject, TeleonomeConstants.DENEWORD_TYPE_ACTION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					actionsJSONArray = aDenomeViewerManager.loadDenesFromPointers(pointersToActionsJSONArray);
					logger.debug("line 2606, for "  + microControllerPointer + " there are pointersToActionsJSONArray=" + pointersToActionsJSONArray.length() +  " actions=" + actionsJSONArray.length());
					for (int l=0;l<actionsJSONArray.length();l++) {

						actionJSONObject = actionsJSONArray.getJSONObject(l);
						actionName =  actionJSONObject.getString( TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
						actionEvaluationPosition = (int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actionJSONObject, TeleonomeConstants.EVALUATION_POSITION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("line 2612, for actionEvaluationPosition="  + actionEvaluationPosition);
						
						actionNameNoSpaces = actionName.replace(" ", "");
						actionExpression = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actionJSONObject, TeleonomeConstants.DENEWORD_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("line 2612, for actionExpression="  + actionExpression);
						
						executionPoint = TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT_IMMEDIATE;
						Object o = DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actionJSONObject, TeleonomeConstants.DENEWORD_ACTION_EXECUTION_POINT, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(o!=null ) {
							executionPoint=(String) o;
						}
						actuatorCommandTrueExpression="";
						actuatorCommandFalseExpression="";
						 o =  DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_TRUE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						if(o !=null) {
							actuatorCommandTrueExpression=(String)o;
						}
						 o =  DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_FALSE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							if(o !=null) {
								actuatorCommandFalseExpression=(String)o;
							}
						actuatorCommandFalseExpression = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actionJSONObject, TeleonomeConstants.DENEWORD_ACTUATOR_COMMAND_CODE_FALSE_EXPRESSION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						
						logger.debug("line 2635, for actuatorCommandTrueExpression="  + actuatorCommandTrueExpression);
						
						actionInfo = new JSONObject();
						actionSectionInfoJSONArray.put(actionInfo);
						actionInfo.put("MicroControllerName", microControllerName);
						actionInfo.put("ActionEvaluationPosition", actionEvaluationPosition);
						actionInfo.put("ActuatorName", actuatorName);
						actionInfo.put("ActionName", actionName);
						actionInfo.put("ActionExpression", actionExpression);
						actionInfo.put("ExecutionPoint", executionPoint);
						actionInfo.put("ActuatorCommandTrueExpression", actuatorCommandTrueExpression);
						actionInfo.put("ActuatorCommandFalseExpression", actuatorCommandFalseExpression);
						
						actuatorActionConditionPointersJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						JSONArray actuatorActionConditionNamesJSONArray = DenomeUtils.getAllMeweWordsFromDeneByDeneWordType(actionJSONObject,TeleonomeConstants.DENEWORD_DENEWORD_TYPE_ATTRIBUTE, TeleonomeConstants.DENEWORD_TYPE_ACTUATOR_CONDITION_POINTER, TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
						actionInfo.put("ActuatorActionConditionPointersJSONArray", actuatorActionConditionPointersJSONArray);
						actionInfo.put("ActuatorActionConditionNamesJSONArray", actuatorActionConditionNamesJSONArray);
						
						logger.debug("line 2653, for actuatorCommandTrueExpression="  + actuatorCommandTrueExpression);
						
						
						
						reportLines.add("							<li><a tabindex=\"-1\" class=\"action-link\"  href=\"#\"  onclick=\"return show_action('"+actionNameNoSpaces+"Section');\" >" +actuatorExecution +"."+ actionEvaluationPosition +" - " + actionName + "</a></li>");
					}
					reportLines.add("			           </ul>");
					reportLines.add("					</div>");



					reportLines.add("				</div>");//class=\"row\"

				}
			}

			logger.debug("line 2669, finished sensors=m actionSectionInfoJSONArray.length()=" + actionSectionInfoJSONArray.length()  );
			
			reportLines.add("					<div class=\"row\">");
			reportLines.add("						<hr class=\"col-xs-12\"  >");
			reportLines.add("					</div>");// 

			

			//
			// now do the panels for each Action
			
			String actionName;
			String actionNameNoSpaces, actuatorCommandTrueExpression, actuatorCommandFalseExpression;
			String actionExpression, executionPoint;
			int actionEvaluationPosition;
			JSONArray actuatorActionConditionNamesJSONArray;
			for(int i=0;i<actionSectionInfoJSONArray.length();i++) {
				actionInfo = actionSectionInfoJSONArray.getJSONObject(i);
				microControllerName=actionInfo.getString("MicroControllerName");
				actuatorName=actionInfo.getString("ActuatorName");
				actionName=actionInfo.getString("ActionName");
				actionExpression = actionInfo.getString("ActionExpression").replace("<", "&lt;" ).replace(">", "&gt;" ).replace("&", "&amp;" );
				actionNameNoSpaces = actionName.replace(" ", "");
				actionEvaluationPosition = actionInfo.getInt("ActionEvaluationPosition" );
				executionPoint = actionInfo.getString("ExecutionPoint");
				
				if(actionInfo.has("ActuatorCommandTrueExpression")) {
					actuatorCommandTrueExpression=actionInfo.getString("ActuatorCommandTrueExpression");
				}else {
					actuatorCommandTrueExpression="";
				}
				
				if(actionInfo.has("ActuatorCommandFalseExpression")) {
					actuatorCommandFalseExpression=actionInfo.getString("ActuatorCommandFalseExpression");
				}else {
					actuatorCommandFalseExpression="";
				}
				
				if(actionInfo.has("ActuatorActionConditionPointersJSONArray" )) {
					actuatorActionConditionPointersJSONArray = actionInfo.getJSONArray("ActuatorActionConditionPointersJSONArray" );
				}else {
					actuatorActionConditionPointersJSONArray=new JSONArray();
				}
				if(actionInfo.has("ActuatorActionConditionNamesJSONArray" )) {
					actuatorActionConditionNamesJSONArray = actionInfo.getJSONArray("ActuatorActionConditionNamesJSONArray" );
				}else {
					actuatorActionConditionNamesJSONArray=new JSONArray();
				}
				
				logger.debug("line 2698, actuatorName=" + actuatorName );
				//
				// Breadcrumb div
				//
				reportLines.add("					<div class=\"ActionDetailSection\" id=\""+ actionNameNoSpaces +"Section\" style=\"display:none\">");
				reportLines.add("						<div class=\"ActionDetailBreadcrumb\" id=\""+ actionNameNoSpaces +"CardBreadcrumb\" >");
				reportLines.add("							<nav class=\"\" aria-label=\"breadcrumb\">");
				reportLines.add("							<ol class=\"breadcrumb\">");
				reportLines.add("								<li class=\"breadcrumb-item\"><a href=\"#\">"+microControllerName+"</a></li>");
				reportLines.add("								<li class=\"breadcrumb-item\"><a href=\"#\">"+actuatorName+"</a></li>");
				reportLines.add("								<li class=\"breadcrumb-item active\" aria-current=\"page\">"+ actionName +"</li>");
				reportLines.add("							</ol>");
				reportLines.add("						</nav>");
				reportLines.add("						</div>"); //class=\"ActionDetailBreadcrumb\"
				//
				//Detail div
				//

				reportLines.add("						<div class=\"card ActionDetail\" id=\""+ actionNameNoSpaces +"Card\" >");


				reportLines.add("							<div class=\"card-header text-center\">");
				reportLines.add("								<span class=\"ActionTitle\">"+actionName+"</span> ");

				reportLines.add("							</div>");
				reportLines.add("							<div class=\"card-body\">");
				reportLines.add("								<p class=\"card-text\">Execution Order:<b>2</b></p>");
				reportLines.add("								<p class=\"card-text\">Execution Point:<b>"+ executionPoint + "</b></p>");

				reportLines.add("								<p class=\"card-text\">Expression:<b>"+ actionExpression +"</b></p>");
				reportLines.add("								<table class=\"ReportTable\">");
				reportLines.add("									<tr><th>Condition Name</th><th>Expression</th><th>Variables</th></tr>");
				reportLines.add("									<tr>");

				

				for(int j=0;j<actuatorActionConditionPointersJSONArray.length();j++){
					actuatorActionConditionPointer = (String) actuatorActionConditionPointersJSONArray.getString(j);
					conditionName = actuatorActionConditionNamesJSONArray.getString(j);

					logger.debug("line 2738 conditionName=" + conditionName);

					actuatorActionConditionJSONObject=null;
					//
					// actuatorActionConditionJSONObject is a deneword get the value which is a denepointer and render it
					try {
						logger.debug("line 2368 actuatorActionConditionPointer=" +actuatorActionConditionPointer);
						actuatorActionConditionJSONObject = aDenomeViewerManager.getDeneByIdentity(new Identity(actuatorActionConditionPointer));
						String conditionExpression = (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(actuatorActionConditionJSONObject, "Expression", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

						reportLines.add("										<td>" + conditionName + "</td>");
						reportLines.add("										<td><b>" + conditionExpression.replace("<", "&lt;" ).replace(">", "&gt;" ).replace("&", "&amp;" ) + "</b></td>");
						reportLines.add("										<td>");
						reportLines.add("											<table>");
						//
						// process the variables for this condition
						//
						JSONArray variables = DenomeUtils.getDeneWordAttributeForAllDeneWordsByDeneWordTypeFromDene(actuatorActionConditionJSONObject, TeleonomeConstants.DENEWORD_TYPE_CONDITION_VARIABLE_POINTER, TeleonomeConstants.COMPLETE);
						logger.debug("line 2374, variables=" + variables);
						JSONObject variableJSONObject;
						for(int k=0;k<variables.length();k++){		
							variableJSONObject = variables.getJSONObject(k);
							logger.debug("line 2380, variableJSONObject=" + variableJSONObject);
							variableName = variableJSONObject.getString("Name");
							variableValue = variableJSONObject.getString("Value");
							reportLines.add("												<tr><td>"+variableName+"</td><td>"+variableValue+"</td></tr>");
							defaultValue = "";
							if(variableJSONObject.has("Default")) {
								defaultValue = variableJSONObject.getString("Default");
							}

						}
					} catch (InvalidDenomeException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					reportLines.add("											</table>");
					reportLines.add("										</td>");
				}
				reportLines.add("								</table> ");// class=\"ReportTable\">");
				reportLines.add("								<<p class=\"card-text\">Actuator Command Code True Expression:"+actuatorCommandTrueExpression+"</p>");
				reportLines.add("								<<p class=\"card-text\">Actuator Command Code False Expression:"+actuatorCommandFalseExpression+"</p>");
				reportLines.add("								</div>");// card-body
				reportLines.add("								<div class=\"card-footer\">");
				reportLines.add("									<a href=\"#\" class=\"card-link\">Card link</a>");
				reportLines.add("									<a href=\"#\" class=\"card-link\">Another link</a>");
				reportLines.add("								</div>");
				reportLines.add("						</div>"); //class=\"card ActionDetail\"
				reportLines.add("					</div>"); //class=\"ActionDetailSection\"
			}

			reportLines.add("				</div>"); //class=\"ActuatorDetail\ 


			reportLines.add("			</div>"); //class=\"panel-body\"


			reportLines.add("		</div>");//panel panel-default">"); 
			reportLines.add("	</div>");// class=\"row Microcontroller-Details\" "); 

		}

		reportLines.add("</div>");// class=\"container\" id=\"Detail-Area\">"); 
		
		reportLines.add("<script src=\"https://code.jquery.com/jquery-1.10.2.js\"></script>");
		reportLines.add("<script>");
		reportLines.add("	function show_detail(toShow){");
		reportLines.add("		$('.SensorDetail').hide();");
		reportLines.add("		        $('.ActuatorDetail').hide();");
		reportLines.add("		       $('.ActionDetailSection').hide();");
		reportLines.add("		var show = document.getElementById(toShow);");
		reportLines.add("		show.style.display = \"\";");
		reportLines.add("	}");

		reportLines.add("	function show_action(toShow) {");
		reportLines.add("	$('.ActionDetailSection').hide();");
		reportLines.add("		var show = document.getElementById(toShow);");
		reportLines.add("		show.style.display = \"\";");        
		reportLines.add("	}");

		reportLines.add("	$(document).ready(function(){");
		reportLines.add("	    $('.Microcontroller-Details').hide();");
		  
		reportLines.add("	    $('.dropdown-submenu a.test').on(\"click\", function(e){");
		reportLines.add("	    	$('.dropdown-submenu>ul').hide();");
		reportLines.add("	    	$(this).next('ul').toggle();");
		reportLines.add("	    	e.stopPropagation();");
		reportLines.add("	    	e.preventDefault();");
		reportLines.add("	    });");
		       
		reportLines.add("	    $('.dropdown-submenu a.test').on(\"click\", function(e){");
		reportLines.add("	    	$('.dropdown-submenu>ul').hide();");
		reportLines.add("	    	$(this).next('ul').toggle();");
		reportLines.add("	    	e.stopPropagation();");
		reportLines.add("	    	e.preventDefault();");
		reportLines.add("	    });");
		        


		reportLines.add("	   $(\".Microcontroller\").on('click',function(){");
		reportLines.add("	   		$('.Microcontroller-Details').hide();");

		reportLines.add("	   		var microName = $(this).data(\"name\");");
		reportLines.add("	   		$(\"#MainDiagram\").hide();");
		reportLines.add("	   		$('.ActionDetailSection').hide();");
		       
		reportLines.add("	   		$(\"#\" + microName.replace(/ /g,'') +\"-Details\").show();");
		reportLines.add("	    });");

		reportLines.add("	    $(\".CloseDetail\").on('click',function(){");
		reportLines.add("	    $('.ActionDetailSection').hide();");
		reportLines.add("	    $('.SensorDetail').hide();");

		reportLines.add("	    $('.Microcontroller-Details').hide();");
		reportLines.add("	    $(\"#MainDiagram\").show();");
		reportLines.add("	    });");
		reportLines.add("	});");    
		reportLines.add("</script>");
	
	reportLines.add("</body></html>");
	return reportLines;
}
}