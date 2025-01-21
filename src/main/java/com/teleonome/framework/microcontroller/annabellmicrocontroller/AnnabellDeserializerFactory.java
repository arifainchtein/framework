package com.teleonome.framework.microcontroller.annabellmicrocontroller;


import java.util.*;
import javax.servlet.ServletContext;
import java.lang.reflect.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.teleonome.framework.exception.ServletProcessingException;

public class AnnabellDeserializerFactory extends Object {

			
    public AnnabellDeserializerFactory() {
    }

    /**
     * Creates any object given by its class.
     * @param aClass. 
     * @param AppServer the AppServer
     * @return BasePage The empty persistent object.
     */
    public static AnnabellDeserializer createAnnabellDeserializer(String className) throws ServletProcessingException{ 
           
        boolean objectCreated = false;       
        AnnabellDeserializer aAnnabellDeserializer = null;
        
        try {
		    Class aClass = Class.forName(className);
            //get the constructors that belong to the class
            Constructor[] constructors = aClass.getConstructors();
            for ( int i=0; i < constructors.length; i++ ) {
                Class[] parameterTypes = constructors[i].getParameterTypes();
                if (parameterTypes.length == 0){
                    Object[] anArray = { };
                    aAnnabellDeserializer = (AnnabellDeserializer) constructors[i].newInstance(anArray);
                    objectCreated = true;
                    break;
                }    
            }    
        }catch ( InstantiationException e ) {                                                                                                                                                    
            Hashtable info = new Hashtable();
            info.put("Exception Thrown","InstantiationException");
            info.put("Exception",e.getMessage());
            info.put("In Method","createObject");
            info.put("In Class","ProcessingFormHandlerFactory 1");
            throw new ServletProcessingException(info);
        }catch ( IllegalAccessException e ) {                                                                                                                                                    
            Hashtable info = new Hashtable();
            info.put("Exception Thrown","IllegalAccessException");
            info.put("Exception",e.getMessage());
            info.put("In Method","createObject");
            info.put("In Class","ProcessingFormHandlerFactory 2");
            throw new ServletProcessingException(info);        
        }catch ( InvocationTargetException e ) {                                                                                                                                                    
            Hashtable info = new Hashtable();
            info.put("Exception Thrown","InvocationTargetException");
            info.put("Exception",e.getMessage());
            info.put("In Method","createObject");
            info.put("In Class","ProcessingFormHandlerFactory 3" );
            throw new ServletProcessingException(info);        
        }catch ( ClassNotFoundException e ) {                                                                                                                                                    
        	aAnnabellDeserializer=null;
        }
       
        if (objectCreated == false) {
        	aAnnabellDeserializer=null;
        }                     
       return aAnnabellDeserializer;
    }   
}
