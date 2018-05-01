package com.teleonome.framework.microcontroller.csvpublishermicrocontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.log4j.Logger;

public class CVSPublisherReader  extends BufferedReader{

		public CVSPublisherReader(Reader in) {
			super(in);
		}

		
		
		public String readLine(){
			return "Ok";
		}
}
