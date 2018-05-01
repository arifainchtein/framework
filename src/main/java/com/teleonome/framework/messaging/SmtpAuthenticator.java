package com.teleonome.framework.messaging;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class SmtpAuthenticator extends Authenticator {
	String pass = "";
	   String login = "";

	   public SmtpAuthenticator() {
	      super();
	   }

	   public SmtpAuthenticator(String login, String pass) {
	      super();
	      this.login = login;
	      this.pass = pass;
	   }

	   public PasswordAuthentication getPasswordAuthentication() {
	      if (pass.equals(""))
	         return null;
	      else
	         return new PasswordAuthentication(login, pass);
	   }
}
