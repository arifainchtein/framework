package com.teleonome.framework.denome;

import org.apache.commons.validator.routines.EmailValidator;
import org.json.JSONObject;

public class DenomeParser {

	public DenomeParser(){
		
	}
	
	public void parse(JSONObject denomeText){
		
	}
	
	public boolean validateTypeEmail(String emailAddress){
		EmailValidator emailValidator = EmailValidator.getInstance();
		return emailValidator.isValid(emailAddress);   
	}
}
