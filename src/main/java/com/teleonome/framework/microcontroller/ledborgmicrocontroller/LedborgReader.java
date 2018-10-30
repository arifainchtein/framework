package com.teleonome.framework.microcontroller.ledborgmicrocontroller;

import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.log4j.Logger;

public class LedborgReader  extends BufferedReader{

		public LedborgReader(Reader in) {
			super(in);
		}

		
		
		public String readLine(){
			return "Ok";
		}
}
