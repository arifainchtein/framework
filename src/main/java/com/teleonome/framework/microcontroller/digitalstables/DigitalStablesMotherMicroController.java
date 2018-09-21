package com.teleonome.framework.microcontroller.digitalstables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.MotherMicroController;
import com.teleonome.framework.microcontroller.gnuserialport.GNUArduinoReader;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class DigitalStablesMotherMicroController extends MotherMicroController {

		Logger logger;
		private static final String PORT_NAMES[] = { "/dev/ttyUSB0" };
		SerialPort serialPort=null;
		private BufferedReader input;
		//private InputStream input;
		
		private BufferedWriter output;

		private static final int TIME_OUT = 20000;
		private int DATA_RATE = 9600;
		private int INTER_SENSOR_READ_TIME_OUT_MILLISECONDS=100;
		InputStream serialPortInputStream = null;
		OutputStream serialPortOutputStream = null;
		
		public DigitalStablesMotherMicroController(DenomeManager d, String n){
			super(d,n);
			logger = Logger.getLogger(getClass());
		}
		
		
		@Override
		public void init(JSONArray params) throws MicrocontrollerCommunicationException {
			// TODO Auto-generated method stub
			Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

			CommPortIdentifier portId = null;
			try {
				INTER_SENSOR_READ_TIME_OUT_MILLISECONDS = ((Integer)aDenomeManager.getDeneWordValueByName(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_VITAL, "Inter Sensor Read Timeout Milliseconds")).intValue();

			} catch (InvalidDenomeException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				logger.debug("Fatal Exception, message:"+ e1.getMessage());
				System.exit(-1);
			}


			CommPortIdentifier currPortId=null;
			while (portId == null && portEnum.hasMoreElements()) {
				currPortId = (CommPortIdentifier) portEnum.nextElement();
				//System.out.println("currPortId=" + currPortId.getName());
				logger.debug("looking for ports, currPortId=" + currPortId);

				for (String portName : PORT_NAMES) {
					if ( currPortId.getName().equals(portName) || currPortId.getName().startsWith(portName) ){
						// Try to connect to the Arduino on this port
						portId = currPortId;
						break;
					}
				}
			}
			if (portId == null) {
				//System.out.println("Could not find COM port.");
				logger.warn("Could not find COM port.");
				Hashtable<String, String> h = new Hashtable();
				h.put("message","Could not find COM port");
				throw new MicrocontrollerCommunicationException(h);
			}
			logger.debug("Found COM Port.");
			try {
				//
				// get the data rate for the arduno ie get the DeneWord , get the dene that represents the arduino
				JSONArray allDenes = (JSONArray)aDenomeManager.getDeneByDeneType(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_COMPONENTS, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);
				JSONObject motherMicrocontrollerDene = null;
				JSONObject microcontrollerDene = null;
				String className="";
				for(int i=0;i<allDenes.length();i++) {
					microcontrollerDene = allDenes.getJSONObject(i);
					className = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(microcontrollerDene, TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(className.equals("com.teleonome.framework.microcontroller.MotherMicroController")) {
						motherMicrocontrollerDene=microcontrollerDene;
					}
				}
				if(motherMicrocontrollerDene==null) {
					logger.warn("Could not find Mother Microcontroller Dene");
					Hashtable<String, String> h = new Hashtable();
					h.put("message","Could not find COM port");
					throw new MicrocontrollerCommunicationException(h);
				}
				String pointerToCommParamsDene =  (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(motherMicrocontrollerDene, TeleonomeConstants.DENEWORD_MICROCONTROLLER_COMMUNICATION_PROTOCOL, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("using pointerToCommParamsDene=" + pointerToCommParamsDene);
				
				JSONObject commParamsDene = aDenomeManager.getDeneByIdentity(new Identity(pointerToCommParamsDene));
				DATA_RATE = ((Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(commParamsDene, "Serial Data Rate", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)).intValue();
				logger.debug("using datarate=" + DATA_RATE);
				serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
				serialPort.disableReceiveTimeout();
				serialPort.enableReceiveThreshold(1);
				serialPort.setSerialPortParams(DATA_RATE,
						SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				// open the streams

				//serialPort.addEventListener(this);
				//serialPort.notifyOnDataAvailable(true);

				///serialPort..write().write(InetAddress.getLocalHost().toString().t());
				serialPortInputStream = serialPort.getInputStream();
				serialPortOutputStream = serialPort.getOutputStream();

				if (serialPortInputStream == null) {
					System.out.println("serialPortInputStream is null.");
					logger.debug("serialPortInputStream is null.");
					throw new SerialPortCommunicationException("SerialPortInputStream is null");
				}

				if (serialPortOutputStream == null) {
					System.out.println("serialPortOutputStream is null.");
					logger.debug("serialPortOutputStream is null.");
					throw new SerialPortCommunicationException("SerialPortOutputStream is null");
				}

				/*
			        try{
			        	String message="Sazirac is ready on " + graphMaker.getIpAddress() + ".  The arduino is listening on usb port " + uSBPort.trim();
						Utils.sendMessage(monitoringEmailAddress, monitoringEmailAddress, message, message, sMTPServer, sMTPServerName, sMTPServerPassword);
					}catch (ActionNotCompletedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				 */



				//
				// SKIP configure pins
				//int sensorNumberOfReadsPerPulse = 10;
				// output.write(longToBytes(sensorNumberOfReadsPerPulse));


				logger.debug("finished initializing Arduino Uno" );

			} catch (Exception e) {

				// TODO Auto-generated catch block
				StringWriter sw = new StringWriter();
				e.printStackTrace( new PrintWriter( sw )    );
				String callStack = sw.toString();
				System.out.println(callStack);
				logger.warn(callStack);
			}
		}

		public BufferedReader getReader() throws IOException{
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			return input;
		}
		
		

		/*
		public InputStream getReader() throws IOException{
			input = serialPort.getInputStream();
			return input;
		}
		*/
		
		public BufferedWriter getWriter() throws IOException{
			return  new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
			//return output;
		}
		
		@Override
		public String getCommandCode() throws IOException {
			// TODO Auto-generated method stub
			output = getWriter();
			String actuatorCommand = "GetCommandCode";
			return sendCommand( actuatorCommand);
		}
		

		private String sendCommand(String actuatorCommand) throws IOException {
			// TODO Auto-generated method stub
			output = getWriter();
			
			output.write(actuatorCommand,0,actuatorCommand.length());
			//serialPortOutputStream.write( actuatorCommand.getBytes() );
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			output.flush();
			input = new GNUArduinoReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream())));
			String inputLine = input.readLine();
			return inputLine;
		}
}
