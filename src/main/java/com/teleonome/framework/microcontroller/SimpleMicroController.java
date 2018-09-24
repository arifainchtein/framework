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
	int numberOfCommandCodesInHistory=0;
	String[] commandCodeHistory = new String[3];
	
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
		plainReader = new PlainReader(new StringReader(dataString), sw);
		 plainWriter = new PlainWriter(sw, plainReader);
		return plainWriter;
	}
 
	public boolean verifyUserCommandCode(String userCode) throws IOException{
		boolean toReturn=false;
		
		String code = getCommandCode();
		if(userCode.equals(code)) {
			return true;
		}else {
			for(int i=0;i<numberOfCommandCodesInHistory;i++){
				if(userCode.equals(commandCodeHistory[i])) {
					return true;
				}
			}
		}
		return toReturn;
	}
	
	
	@Override
	public String getCommandCode()  throws IOException {
		String code="";
		String unEncodedKey = "MyLegoDoor";//FileUtils.readFileToString(new File("SecretKey"), "UTF-8");
		TOTP totp = new TOTP();
		try {
			code = totp.generateCurrentNumberFromUnencodedString(unEncodedKey);

		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("returning code = " + code);
		
		
		
		if(currentCommandCodeHistoryPos<numberOfCommandCodesInHistory){
			commandCodeHistory[currentCommandCodeHistoryPos]=code;
			currentCommandCodeHistoryPos++;
		}else{
			if(numberOfCommandCodesInHistory>0) {
				for(int i=0;i<numberOfCommandCodesInHistory-1;i++){
					commandCodeHistory[i]=commandCodeHistory[i+1];
				}
				commandCodeHistory[numberOfCommandCodesInHistory-1]=code;
			}else {
				commandCodeHistory[numberOfCommandCodesInHistory]=code;
			}
			
		}

		
		
		return code;
	}

}
