package com.teleonome.framework.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;

public class Utils {

	private static Logger logger = Logger.getLogger(com.teleonome.framework.utils.Utils.class);
	
	/**
	 * this method reads the ssh conf file and looks for a line
	 * PasswordAuthentication no
	 * if it finds it returns falseexicp 
	 * otherwise it returns true
	 * Note that this code assumes that no # is in the PasswordAuthentication line, either before or after the text
	 * @return
	 */
	public static boolean isSSHPasswordAllowed() {
		
		try {
			List<String> sshConfigFileLines = FileUtils.readLines( new File("/etc/ssh/sshd_config"));
			Iterator it = sshConfigFileLines.iterator();
			String line;
			while(it.hasNext()) {
				line = (String) it.next();
				if(line.contains("PasswordAuthentication no")) {
					if(line.contains("#")) {
						return true;
					}else {
						return false;
					}
				}
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	public static boolean disablePasswordLogin() {
		//# only allow key based logins
				try {
					ArrayList a1 = executeCommand("sudo sed -n 'H;${x;s/\\#PasswordAuthentication yes/PasswordAuthentication no/;p;}' /etc/ssh/sshd_config > tmp_sshd_config");
					ArrayList a11 = executeCommand("sudo sed -n 'H;${x;s/\\#ChallengeResponseAuthentication yes/ChallengeResponseAuthentication no/;p;}' tmp_sshd_config > tmp_sshd_config2");
					ArrayList a111 = executeCommand("sudo sed -n 'H;${x;s/\\#UsePAM yes/UsePAM no/;p;}' tmp_sshd_config2 > tmp_sshd_config3");
					
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				try {
					ArrayList a2 = executeCommand("sudo cat tmp_sshd_config3 > /etc/ssh/sshd_config");
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				try {
					ArrayList a3 = executeCommand("sudo rm tmp_sshd_config");
					ArrayList a31 = executeCommand("sudo rm tmp_sshd_config2");
					ArrayList a32 = executeCommand("sudo rm tmp_sshd_config3");
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
				return true;
	}
	
	
	public static String generateJavascriptTeleonomeConstants(){
		StringBuffer buffer = new StringBuffer();
		Field[] declaredFields = TeleonomeConstants.class.getDeclaredFields();
		List<Field> staticFields = new ArrayList<Field>();
		for (Field field : declaredFields) {
		    if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
		    	if (java.lang.reflect.Modifier.isPublic(field.getModifiers()) &&
		    		field.getType().isAssignableFrom(String.class)
		    			) {
		    		//buffer.append("var " + field.getName()  + "=\"" +field.get)
		    	}
		        staticFields.add(field);
		    }
		}
		return buffer.toString();
	}
	
	
	
	public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        final int BUFFER_SIZE = 4096;
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
	
	
	/*
	 * returns which board you are using, currently the supported values are
	 *  the value to the right of the :  is the Revision for that model
	 * 	Raspberry Pi Model A: 0007, 0008, 0009
	 *	Raspberry Pi Model B (Rev 1.0, 256Mb): 0002, 0003
	 *	Raspberry Pi Model B (Rev 2.0, 256Mb): 0004, 0005, 0006
	 * 	Raspberry Pi Model B (Rev 2.0, 512Mb): 000d, 000e, 000f
	 *	Raspberry Pi Model A+: 0012
	 *  Raspberry Pi Model B+: 0010, 0013
	 *	Raspberry Pi 2 Model B: 1041
	 * Raspberry Pi 3 Model B: a02082,a22082 
	 */
	public static String getComputerModel(){
		ArrayList result=new ArrayList();
		String modelName="Unknown";
		try {
			result = Utils.executeCommand( "cat /proc/cpuinfo ");
			//
			// look for Revision
			String line;
			for(int i=0;i<result.size();i++){
				line  = (String) result.get(i);
				if(line.startsWith("Revision")){
					String[] tokens = line.split(":");
					//
					// the second token will contain the value
					// compare it with the list above
					if(tokens[1].contains("0007") || tokens[1].contains("0008") || tokens[1].contains("0009")){
						modelName="Raspberry Pi Model A";
					}else if(tokens[1].contains("0002") || tokens[1].contains("0003") ){
						modelName="Raspberry Pi Model B (Rev 1.0, 256Mb)";
					}else if(tokens[1].contains("0004") || tokens[1].contains("0005") || tokens[1].contains("0006")){
						modelName="Raspberry Pi Model B (Rev 2.0, 256Mb)";
					}else if(tokens[1].contains("000d") || tokens[1].contains("000e") || tokens[1].contains("000f")){
						modelName="Raspberry Pi Model B (Rev 2.0, 512Mb)";
					}else if(tokens[1].contains("0012") ){
						modelName="Raspberry Pi Model A+";
					}else if(tokens[1].contains("0010") || tokens[1].contains("0013")){
						modelName="Raspberry Pi Model B+";
					}else if(tokens[1].contains("1041") ){
						modelName="Raspberry Pi 2 Model B";
					}else if(tokens[1].contains("02082") || tokens[1].contains("22082") ){
						modelName="Raspberry Pi 3 Model B";
					}else if(tokens[1].contains("a020d3")  ){
						modelName="Raspberry Pi 3 Model B+";
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			//logger.debug(Utils.getStringException(e));
		}
		return modelName;
	}
	
	public static JSONObject getUSBDevicesDene(){
		ArrayList result=new ArrayList();
		String modelName="Unknown";
		JSONObject uSBDevicesDene = new JSONObject();
		JSONObject deneWord;
		String[] tokens;
		String name, value;
		
		try {
			uSBDevicesDene.put(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, TeleonomeConstants.DENE_USB_DEVICES);
			JSONArray deneWords = new JSONArray();
			uSBDevicesDene.put("DeneWords", deneWords);
			
			result = Utils.executeCommand( "lsusb");
			String line;
			for(int i=0;i<result.size();i++){
				line  = (String) result.get(i);
				//
				// the line format is
				// Bus 001 Device 004: ID 148f:5370 Ralink Technology, Corp. RT5370 Wireless Adapter
				// there are two : but the tokenization is with the first ne
				tokens = line.split(":");
				if(tokens.length > 1){
					name = tokens[0];
					value = tokens[1] + ":" + tokens[2];
					deneWord = createDeneWordJSONObject( name,  value, null,"String", true);
					deneWords.put(deneWord);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uSBDevicesDene;	
	}
	
	public static boolean isPulseLate(JSONObject pulseJSONObject) throws InvalidDenomeException {
		long lastPulseMillis = pulseJSONObject.getLong(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS);
		String lastPulseDate = pulseJSONObject.getString(TeleonomeConstants.PULSE_TIMESTAMP);
		logger.info("lastPulseDate=" + lastPulseDate + " lastPulseMillis=" + lastPulseMillis);
		JSONObject denomeObject = pulseJSONObject.getJSONObject("Denome");
		String tN = denomeObject.getString("Name");


		Identity identity = new Identity(tN, TeleonomeConstants.NUCLEI_INTERNAL, TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_TYPE_NUMBER_PULSES_BEFORE_LATE);
		int numberOfPulsesBeforeIsLate =  (Integer) DenomeUtils.getDeneWordByIdentity(pulseJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

		identity = new Identity(tN, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY);
		int currentPulseFrequency = (Integer)DenomeUtils.getDeneWordByIdentity(pulseJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

		identity = new Identity(tN, TeleonomeConstants.NUCLEI_PURPOSE, TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA, TeleonomeConstants.DENE_VITAL, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_GENERATION_DURATION);
		int currentPulseGenerationDuration = (Integer)DenomeUtils.getDeneWordByIdentity(pulseJSONObject, identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		logger.info("currentPulseFrequency=" + currentPulseFrequency + " numberOfPulsesBeforeIsLate=" + numberOfPulsesBeforeIsLate);

		long now = System.currentTimeMillis();
		long timeSinceLastPulse =  now - lastPulseMillis;

		//
		// There are two cases, depending whether:
		//
		// currentPulseFrequency > currentPulseGenerationDuration in this case we are in a teleonome that 
		// executes a pulse every minute but the creation of the pulse is less than one minute
		//
		//currentPulseFrequency < currentPulseGenerationDuration in this case we are in a teleonome that 
		// takes a long time to complete a pulse cycle.  For example a teleonome that is processing analyticons
		// or mnemosycons might take 20 minutes to complete the pulse and wait one minute before starting again
		
		boolean late=(numberOfPulsesBeforeIsLate*(currentPulseFrequency  + currentPulseGenerationDuration))< timeSinceLastPulse;
		
		
//		boolean late=false;
//		if(currentPulseFrequency > currentPulseGenerationDuration) {
//			if((numberOfPulsesBeforeIsLate*(currentPulseFrequency  + currentPulseGenerationDuration))< timeSinceLastPulse)late=true;
//		}else {
//			if((numberOfPulsesBeforeIsLate*currentPulseGenerationDuration)< timeSinceLastPulse)late=true;
//		}
		return late;
	}
	
	public static JSONObject getComputerInfoDene(){
		ArrayList result=new ArrayList();
		String modelName="Unknown";
		JSONObject computerInfoDene = new JSONObject();
		JSONObject deneWord;
		String[] tokens;
		String name, value;
		
		try {
			computerInfoDene.put(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, TeleonomeConstants.DENE_COMPUTER_INFO);
			JSONArray deneWords = new JSONArray();
			computerInfoDene.put("DeneWords", deneWords);
			String line;
			result = Utils.executeCommand( "uname -a");
			line  = (String) result.get(0);
			deneWord = createDeneWordJSONObject( "uname info",  line, null,"String", true);
			deneWords.put(deneWord);
			
			result = Utils.executeCommand("lsb_release -ic");
			
			for(int i=0;i<result.size();i++){
				line  = (String) result.get(i);
				////logger.debug("line:" + line);
				//
				tokens = line.split(":");
				if(tokens.length > 1){
					name = tokens[0];
					value = tokens[1];
					deneWord = createDeneWordJSONObject( name,  value, null,"String", true);
					deneWords.put(deneWord);
				}
			}
			deneWord = createDeneWordJSONObject( TeleonomeConstants.DENEWORD_COMPUTER_MODEL,  getComputerModel(), null,"String", true);
			deneWords.put(deneWord);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return computerInfoDene;	
	}
	
	public static JSONObject getProcesorInfoDene(){
		ArrayList result=new ArrayList();
		String modelName="Unknown";
		JSONObject uSBDevicesDene = new JSONObject();
		JSONObject deneWord;
		String[] tokens;
		String name, value;
		
		try {
			uSBDevicesDene.put(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE, TeleonomeConstants.DENE_PROCESSOR_INFO);
			JSONArray deneWords = new JSONArray();
			uSBDevicesDene.put("DeneWords", deneWords);
			
			result = Utils.executeCommand( "cat /proc/cpuinfo");
			String line;
			for(int i=0;i<result.size();i++){
				line  = (String) result.get(i);
				////logger.debug("line:" + line);
				//
				tokens = line.split(":");
				if(tokens.length > 1){
					name = tokens[0];
					value = tokens[1];
					deneWord = createDeneWordJSONObject( name,  value, null,"String", true);
					deneWords.put(deneWord);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uSBDevicesDene;	
	}
	
	public static JSONObject createDeneWordJSONObject(String name, Object value, String units,String valueType, boolean required) throws JSONException{
		JSONObject deneWord = new JSONObject();
		deneWord.put("Name", name);
		if(value!=null) {
			deneWord.put("Value",  value);
		}
		deneWord.put("Value Type", valueType);
		if(units!=null)deneWord.put("Units", units);
		deneWord.put("Required", required);
		return deneWord;
	}
	
	
	
	
	public static ArrayList executeCommand(String command) throws IOException, InterruptedException{
		logger.debug("about to create process for the command:" + command);
		Process process = Runtime.getRuntime().exec(new String[]{"sh","-c",command});
		logger.debug("created process for the command:" + command);
		
		ArrayList toReturn = new ArrayList();
		
		//logger.debug("executeCommand about to start reader");
		
		//Process process =pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		logger.debug("executeCommand reader created:" + command);
		
		
		while ( (line = reader.readLine()) != null) {
			logger.debug("adding line:" + line);
			toReturn.add(line);
		}
		reader.close();
		if(!process.waitFor(500, TimeUnit.MILLISECONDS)) {
		    //timeout - kill the process.
			logger.debug("executeCommand The command:" + command + " was killed after 500 milliseconds");
			toReturn.add("executeCommand The command:" + command + " was killed after 500 milliseconds");
		    process.destroy(); // consider using destroyForcibly instead
		}
		logger.debug("executeCommand returning :" + toReturn);
		
		return toReturn;
	}
	
	
	
	

	

	
	public static LinkedHashMap getSSIDSold() {
		// TODO Auto-generated method stub
		LinkedHashMap<String, Map<String,HashMap>> toReturn = new LinkedHashMap();
		try {
			//Runtime.getRuntime().exec("sudo wpa_cli scan");
			 try {
				ArrayList info =  executeCommand("sudo wpa_cli scan");
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String fileName =  getLocalDirectory() + "ssids";
			//logger.debug("saving ssids file to " + fileName);
			//Runtime.getRuntime().exec("wpa_cli scan_results >" + fileName);
			
			ArrayList v=null;
			
			try {
				v =  executeCommand("sudo wpa_cli scan_results");
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			//logger.debug("saved ssid info to " + fileName);
			File file = new File(fileName);
			//while(!file.isFile()){
				//System.out.print(".");
			//}
			//ArrayList v = readFileLineByLine(file);
			String line;
			String[] tokens;
			HashMap hm;
			for(int i=2;i<v.size();i++){
				line = (String)v.get(i);
				tokens = line.split("\t");
				hm = new HashMap();
				hm.put(tokens[tokens.length-2],tokens[tokens.length-3]);
				toReturn.put(tokens[tokens.length-1], hm);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return toReturn;
	}
	
	public static String generateMethodTrace(){
    	StringWriter sw = new StringWriter();    
    	new Throwable().printStackTrace( new PrintWriter( sw )    );
    	return sw.toString();
	}
	
	public static ArrayList readFileLineByLine(File file) throws FileNotFoundException, IOException{
		
		 // Open the file that is the first 
		 // command line parameter
		 ArrayList v = new ArrayList();
		 FileInputStream fstream = new FileInputStream(file);
		 // Get the object of DataInputStream
		 DataInputStream in = new DataInputStream(fstream);
		 BufferedReader br = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
		 String strLine;
		 //Read File Line By Line
		 while ((strLine = br.readLine()) != null)   {
			 // Print the content on the console
			 v.add(strLine);
		 }
		 //Close the input stream
		 in.close();
		 return v;
	}
	
	public static int getQuarter(LocalDate cal) {
		int month = cal.get(ChronoField.MONTH_OF_YEAR);
		switch (Month.of(month)) {
		case JANUARY:
		case FEBRUARY:
		case MARCH:
		default:
			return 0;
		case APRIL:
		case MAY:
		case JUNE:
			return 1;
		case JULY:
		case SEPTEMBER:
			return 2;
		case OCTOBER:
		case NOVEMBER:
		case DECEMBER:
			return 3;
		}
	}

	public static String getStringException(Exception e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		return exceptionAsString;
	}

	public static String getStringException(Throwable e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		return exceptionAsString;
	}

	public static String getElapsedSecondsToHoursMinutesSecondsString(int elapsedTime) {       
		String format = String.format("%%0%dd", 2);  
		String seconds = String.format(format, elapsedTime % 60);  
		String minutes = String.format(format, (elapsedTime % 3600) / 60);  
		String hours = String.format(format, elapsedTime / 3600);  
		String time =  hours + ":" + minutes + ":" + seconds;  
		return time;  
	} 

	public static String getElapsedTimeHoursMinutesSecondsString(long e) {       
		String format = String.format("%%0%dd", 2);  
		long elapsedTime = e / 1000;  
		String seconds = String.format(format, elapsedTime % 60);  
		String minutes = String.format(format, (elapsedTime % 3600) / 60);  
		String hours = String.format(format, elapsedTime / 3600);  
		String time =  hours + ":" + minutes + ":" + seconds;  
		return time;  
	} 

	public static String readableFileSize(long size) {
		if(size <= 0) return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	public static String getTimeForLCD(){
		Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		String minuteString;
		if(minute<10){
			minuteString="0"+minute;
		}else{
			minuteString=""+minute;
		}
		int second = cal.get(Calendar.SECOND);
		return "Time"+ hour + ":" + minuteString;
	}

	public static String renderCommand(String command){
		if(command.equals(TeleonomeConstants.COMMANDS_DATE_FOR_LCD)){
			return getDateForLCD();
		}else if(command.equals(TeleonomeConstants.COMMANDS_TIME_FOR_LCD)){
			return getTimeForLCD();
		}else if(command.equals(TeleonomeConstants.COMMANDS_DATE_FOR_EXPORT_FILENAME)){
			return getTimeForExportFileName();
		}
		return "";
	}

	public static String getTimeForExportFileName(){
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+10:00"));
		int  day = cal.get(Calendar.DATE);
		int  month = cal.get(Calendar.MONTH)+1;
		int  year = cal.get(Calendar.YEAR);
		
		
		return day + "_" + month + "_"+ year;
	}

	public static String getDateForLCD(){
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+10:00"));
		int  day = cal.get(Calendar.DATE);
		int  month = cal.get(Calendar.MONTH)+1;

		return "Date"+ day + "/" + month;
	}

	public static Calendar generateCalendarObjectForToday(){

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+10:00"));
		int  day = cal.get(Calendar.DATE);
		int  month = cal.get(Calendar.MONTH);
		int  year = cal.get(Calendar.YEAR);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);

		Calendar cal2 = Calendar.getInstance();
		cal2.set(Calendar.YEAR, year);
		cal2.set(Calendar.MONTH, month);
		cal2.set(Calendar.DATE, day);

		cal2.set(Calendar.HOUR_OF_DAY, 0);
		cal2.set(Calendar.MINUTE, 0);
		cal2.set(Calendar.SECOND, 0);
		cal2.set(Calendar.MILLISECOND, 0);
		return cal2;
	}


	public Calendar generateCalendarObjectForTodayWithTime(){

		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+10:00"));
		int  day = cal.get(Calendar.DATE);
		int  month = cal.get(Calendar.MONTH);
		int  year = cal.get(Calendar.YEAR);

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		int second = cal.get(Calendar.SECOND);

		Calendar cal2 = Calendar.getInstance();
		cal2.set(Calendar.YEAR, year);
		cal2.set(Calendar.MONTH, month);
		cal2.set(Calendar.DATE, day);

		cal2.set(Calendar.HOUR_OF_DAY, hour);
		cal2.set(Calendar.MINUTE, minute);
		cal2.set(Calendar.SECOND, second);
		cal2.set(Calendar.MILLISECOND, 0);
		return cal2;
	}

	public Calendar generateCalendarObjectForDate(int day, int month, int year){

		Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("GMT+10:00"));
		cal2.set(Calendar.YEAR, year);
		cal2.set(Calendar.MONTH, month);
		cal2.set(Calendar.DATE, day);

		cal2.set(Calendar.HOUR_OF_DAY, 0);
		cal2.set(Calendar.MINUTE, 0);
		cal2.set(Calendar.SECOND, 0);
		cal2.set(Calendar.MILLISECOND, 0);
		return cal2;
	}

	public static String getLocalDirectory(){
		String hh;
		Properties pp;
		pp = System.getProperties();
		hh = pp.getProperty("user.dir");
		File currentDir = new File(hh);
		String sep = currentDir.separator;
		return hh + sep ;
	}

	public static InetAddress getInetAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(!inetAddr.isLoopbackAddress()){
					if(inetAddr.isSiteLocalAddress()){
						return inetAddr;
					}else{
						potential=inetAddr;
					}
				}

			}
		}
		return potential;
	}

	public static String getIpAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		
		//
		// pay attention to the fact that if a teleonome has 2 network cards, 
		// one as a host and one as part of a network, the inetAddress needs to belong
		// to the network interface connected to the organism network, otherwise the exozero network
		// will not receive the pulse
		int numberOfNetworkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces()).size();
		
		
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(numberOfNetworkInterfaces>1) {
					if(!inetAddr.isLoopbackAddress() && !inetAddr.getHostAddress().equals("172.16.1.1")){
						if(inetAddr.isSiteLocalAddress()){
							return inetAddr.getHostAddress();
						}else{
							potential=inetAddr;
						}
					}
				}else {
					if(!inetAddr.isLoopbackAddress()){
						if(inetAddr.isSiteLocalAddress()){
							return inetAddr.getHostAddress();
						}else{
							potential=inetAddr;
						}
					}
				}
			}
		}
		return potential.getHostAddress();
	}

	/**
	 * the address 172.16.1.1 (TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS) is hardwired as the address that the adas are going to connect to
	 * so make sure that you do not return this address, because this method is called to
	 * identify which network card will be bound to ZeroMQ network
	 */
	public static InetAddress getExoZeroNetworkAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(!inetAddr.getHostAddress().equals(TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS)) {
					if(!inetAddr.isLoopbackAddress()){
						if(inetAddr.isSiteLocalAddress()){
							return inetAddr;
						}else{
							potential=inetAddr;
						}
					}
				}
			}
		}
		return potential;
	}

	/**
	 * the address 172.16.1.1 (TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS) is hardwired as the address that the adas are going to connect to
	 * so make sure that you do not return this address, because this method is called to
	 * identify which network card will be bound to ZeroMQ network
	 */
	public static InetAddress getEndoZeroNetworkAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(inetAddr.getHostAddress().equals(TeleonomeConstants.ADA_INTERNAL_HOST_IPADDRESS)) {
					if(!inetAddr.isLoopbackAddress()){
						if(inetAddr.isSiteLocalAddress()){
							return inetAddr;
						}else{
							potential=inetAddr;
						}
					}
				}
			}
		}
		return potential;
	}

	
	public static void zipFile(File sourceFile, File destinationFile) throws IOException {

		FileOutputStream fos = new FileOutputStream(destinationFile);
		ZipOutputStream zipOut = new ZipOutputStream(fos);
		FileInputStream fis = new FileInputStream(sourceFile);
		ZipEntry zipEntry = new ZipEntry(sourceFile.getName());
		zipOut.putNextEntry(zipEntry);
		final byte[] bytes = new byte[1024];
		int length;
		while((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		zipOut.close();
		fis.close();
		fos.close();
	}

	public static void unZipFile(File fileToDecompress, String path) throws IOException {
       byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileToDecompress));
        ZipEntry zipEntry = zis.getNextEntry();
        String desFileName;
        while(zipEntry != null){
            String fileName = zipEntry.getName();
            File newFile = new File(path + fileName);
            desFileName = path +"/"+ fileName;
            logger.debug("unzipping " + desFileName);
            FileOutputStream fos = new FileOutputStream(desFileName);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }
}
