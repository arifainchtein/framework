package com.teleonome.framework.microcontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.security.totp.TOTP;
import com.teleonome.framework.utils.Utils;


public class SimpleMicroController extends MotherMicroController {
	StringWriter sw = new StringWriter();
	String  dataString="dataString";
	PlainReader plainReader;
	PlainWriter plainWriter;
	Logger logger = Logger.getLogger(getClass());
	String[] previousCodes= new String[3];
	int currentCommandCodeHistoryPos;
	int numberOfCommandCodesInHistory=5;
	String[] commandCodeHistory = new String[5];
	
	public SimpleMicroController(DenomeManager d, String n) {
		super(d, n);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public void init(JSONArray params) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		 
	}


	@Override
	public BufferedReader getReader() throws IOException {
		return  plainReader;
	}

	@Override
	public BufferedWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		//
		// force a generation of the code so that the codehistory is always
		// populated with valid codes.
		//String code = getCommandCode();
		//logger.debug("generated code " + code);
		
		plainReader = new PlainReader(new StringReader(dataString), sw);
		 plainWriter = new PlainWriter(sw, plainReader);
		return plainWriter;
	}
 
	public boolean verifyUserCommandCode(String userCode) throws IOException{
		boolean toReturn=false;
		
		String code = getCommandCode();
		logger.debug("line 83 userCode=" + userCode + " code=" + code);
		if(userCode.equals(code)) {
			return true;
		}else {
			for(int i=0;i<numberOfCommandCodesInHistory;i++){
				logger.debug("commandCodeHistory[i]=" + commandCodeHistory[i]);
				if(userCode.equals(commandCodeHistory[i])) {
					return true;
				}
			}
		}
		return toReturn;
	}
	
	@Override
	public  boolean rebootingHypothalamus() throws IOException{
		boolean toReturn=true;
		return toReturn;
	}
	
	@Override
	public  boolean shuttingDownHypothalamus() throws IOException{
		boolean toReturn=true;
		return toReturn;
	}
	
	
	@Override
	public String getCommandCode()  throws IOException {
		String code="";
		String unEncodedKey = FileUtils.readFileToString(new File("SecretKey"), "UTF-8").trim();
		String chomped=StringUtils.chomp(unEncodedKey);
		logger.debug("unEncodedKey = " + unEncodedKey + " chomped" + chomped);
		TOTP totp = new TOTP();
		try {
			code = totp.generateCurrentNumberFromUnencodedString(unEncodedKey);
			logger.debug("unEncodedKey code = " + code );
			code = totp.generateCurrentNumberFromUnencodedString(chomped);
			logger.debug("chomped code = " + code );
			
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("returning code = " + code);
		boolean inHistory=false;
		
		for(int i=0;i<numberOfCommandCodesInHistory;i++){
	        if( commandCodeHistory[i]!=null && !commandCodeHistory[i].equals("") && code.equals( commandCodeHistory[i])) {
	        	inHistory=true;
	        	logger.debug("found in Code History=" + commandCodeHistory[i]);
	        }
	      }
		
		if(!inHistory) {
			if(currentCommandCodeHistoryPos<numberOfCommandCodesInHistory){
			      commandCodeHistory[currentCommandCodeHistoryPos]=code;
			      currentCommandCodeHistoryPos++;
		    }else{
		      for(int i=0;i<numberOfCommandCodesInHistory-1;i++){
		        commandCodeHistory[i]=commandCodeHistory[i+1];
		      }
		      commandCodeHistory[numberOfCommandCodesInHistory-1]=code;
		    }
			
		}
		
		
		for(int i=0;i<numberOfCommandCodesInHistory-1;i++){
			logger.debug("Code History=" + commandCodeHistory[i]);
		}
		
		return code;
	}


	@Override
	public String getDigitalGeppettoCommandCode() throws IOException {
		// TODO Auto-generated method stub
		String code="";
		String unEncodedKey = FileUtils.readFileToString(new File("DigitalGeppettoSecretKey"), "UTF-8").trim();
		TOTP totp = new TOTP();
		try {
			code = totp.generateCurrentNumberFromUnencodedString(StringUtils.chomp(unEncodedKey));
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("returning code = " + code);
		return code;
	}

}
