package com.teleonome.framework.servlet;




/**
 * this class is the parent of all classes that
 * are use to process forms
 */
import javax.servlet.*;
import java.io.*;
import java.util.Hashtable;
import javax.servlet.http.*;
import org.apache.log4j.*;

import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.exception.ServletProcessingException;


public abstract class ProcessingFormHandler{

	protected HttpServletRequest request;
	
	protected String actionName;
	protected ServletContext servletContext;
	protected HttpServletResponse response;
	protected HttpSession session;
	protected Logger logger;
	protected String command, payload;
	protected Class aClass=null;
	
	
     public ProcessingFormHandler(HttpServletRequest req,HttpServletResponse res,ServletContext servletContext) {
			this.request=req;
			this.response=res;
			this.actionName=actionName;
			this.servletContext=servletContext;	
			logger = Logger.getLogger(getClass());	
			session = request.getSession();
	}
		
	
	/**
	 * process the request and returns the string that represents the class
	 * to be followed next
	 */
	public abstract void process() throws ServletProcessingException;
	

	public HttpServletRequest getRequest(){
		return request;
	}
	
	
	
	public HttpServletResponse getResponse(){
		return response;
	}
	
	/**
	 * this is used by the action manager
	 *@return ServletContext
	 *@see com.casete.framework.managers.ActionManager
	 */
	public ServletContext getServletContext(){
		return servletContext;
	}

	public String getPayload(){
		return payload;
	}
	public String getCommand(){
		return command;
	}
	
}	