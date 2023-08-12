package com.teleonome.framework.microcontroller.cajalmicrocontroller;


import java.util.*;
import javax.servlet.ServletContext;
import java.lang.reflect.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.teleonome.framework.exception.ServletProcessingException;

public class CajalDeserializerFactory extends Object {

			
    public CajalDeserializerFactory() {
    }

    /**
     * Creates any object given by its class.
     * @param aClass. 
     * @param AppServer the AppServer
     * @return BasePage The empty persistent object.
     */
    public static CajalDeserializer createCajalDeserializer(String className) throws ServletProcessingException{ 
           
        boolean objectCreated = false;       
        CajalDeserializer aCajalDeserializer = null;
        
        try {
		    Class aClass = Class.forName(className);
            //get the constructors that belong to the class
            Constructor[] constructors = aClass.getConstructors();
            for ( int i=0; i < constructors.length; i++ ) {
                Class[] parameterTypes = constructors[i].getParameterTypes();
                if (parameterTypes.length == 0){
                    Object[] anArray = { };
                    aCajalDeserializer = (CajalDeserializer) constructors[i].newInstance(anArray);
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
        	aCajalDeserializer=null;
        }
       
        if (objectCreated == false) {
        	aCajalDeserializer=null;
        }                     
       return aCajalDeserializer;
    }   
}
