package com.teleonome.framework.microcontroller.networkinspectormicrocontroller;



import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import com.teleonome.framework.utils.Utils;

public class NetworkInspectorWriter  extends BufferedWriter{

		NetworkInspectorReader aNetworkInspectorReader;
		Logger logger;
		
		
		public NetworkInspectorWriter(Writer out, NetworkInspectorReader c) {
			super(out);
			aNetworkInspectorReader=c;
			logger = Logger.getLogger(getClass());
			// TODO Auto-generated constructor stub
		}

		public void write(String command, int off, int len) throws IOException {

			aNetworkInspectorReader.setCurrentCommand(command);
			
		}

	}
