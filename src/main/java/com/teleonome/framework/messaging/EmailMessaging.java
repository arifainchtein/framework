package com.teleonome.framework.messaging;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import javax.mail.internet.InternetAddress;

import com.teleonome.framework.exception.CommunicationException;

public class EmailMessaging {

	private static void sendMessage(String senderEmailAddress, String fixedRecipient, String subject , String content, String smtpServer) throws CommunicationException{
		  sendMessage(senderEmailAddress,  fixedRecipient,  subject ,  content,  smtpServer, null, null);
	  }
	
	public static void sendMessage(String senderEmailAddress, String  destinationEmail, String subject , String content, String smtpServer, String user, String pass) throws CommunicationException{
		  Vector v = new Vector();
		  v.addElement(destinationEmail);
		  sendMessage( senderEmailAddress, v,  subject ,  content,  smtpServer,  user,  pass);
	  }
	
	
	public static void sendMessage(String senderEmailAddress, Vector fixedRecipients, String subject , String content, String smtpServer, String user, String pass) throws CommunicationException{
    	try{			
    				// Setup mail server;
    				if(smtpServer==null){;
    					Hashtable h = new Hashtable();
    					h.put("info","The smtp server was not set.  The email was not sent");
    					throw new CommunicationException(h);
    				}
    				
    				// Get system properties";
    				Properties props = System.getProperties();			
    				props.put("mail.smtp.host", smtpServer);
    				javax.mail.Session session=null;
    				SmtpAuthenticator auth=null;
    				if(user!=null && pass!=null){
    					auth = new SmtpAuthenticator(user, pass);
    					props.put("mail.smtp.auth", "true");
    				}
    				
    				session = javax.mail.Session.getDefaultInstance(props, auth);
    				// Get session";
    				// Define message";
    				javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
    				message.setFrom(new javax.mail.internet.InternetAddress(senderEmailAddress));
    				//
    				// the Recipient
    				//
    				//
    				InternetAddress[] receipientsList = new InternetAddress[fixedRecipients.size()];
    				for(int i=0;i<fixedRecipients.size();i++){
    					receipientsList[i]=new javax.mail.internet.InternetAddress((String)fixedRecipients.elementAt(i));
    				}
    				message.addRecipients(javax.mail.Message.RecipientType.TO, receipientsList);	
    				//message.addRecipient(javax.mail.Message.RecipientType.TO, new javax.mail.internet.InternetAddress(fixedRecipient));			
    				message.setSubject(subject);
    				message.setContent(content,"text/html");
    				
    				// Send message"
    				javax.mail.Transport.send(message);
    				//System.out.println("Message sent to " + fixedRecipient + " with content "  + content);
    				 				
    				}catch(javax.mail.internet.AddressException e){
    					Hashtable h = new Hashtable();
    					h.put("info","Exception has ocurred, e=" + e);
    					throw new CommunicationException(h);
    				}catch(javax.mail.MessagingException e){
    					Hashtable h = new Hashtable();
    					h.put("info","Exception has ocurred, e=" + e);
    					throw new CommunicationException(h);
    				}			

    			//***************************************************************
    		
    			
    		}
}
