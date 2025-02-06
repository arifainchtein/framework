package com.teleonome.framework.microcontroller.plseries;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.PlainReader;
import com.teleonome.framework.utils.Utils;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class PLSeriesMicroController extends MicroController {
			

			StringWriter sw = new StringWriter();
			String  dataString="dataString";
			PLSeriesReader aPLSeriesReader = null;//
			Logger logger;
			private static final String PORT_NAMES[] = {  "/dev/ttyAMA0","/dev/ttyUSB0"};
			SerialPort serialPort;
			
			private static final int TIME_OUT = 20000;
			private int DATA_RATE = 2400;
			InputStream serialPortInputStream = null;
			OutputStream serialPortOutputStream = null;
			int SYSTEM_VOLTAGE=48;
			PLSeriesWriter aPlSeriesWriter=null;
			
			
			public PLSeriesMicroController(Hypothalamus h,DenomeManager d, String n) {
				super(h,d, n);
				setEnableAsyncUpdate(true);
				// TODO Auto-generated constructor stub
				logger = Logger.getLogger(getClass());
			}

			
			@Override
			public void init(JSONArray params) throws MicrocontrollerCommunicationException {
				// TODO Auto-generated method stub
				
				try {
					logger.debug("about to initialize serial communications");
					initializeSerialComm();
					logger.debug("about to create the reader and the writed");
					
					aPLSeriesReader = new PLSeriesReader(new StringReader(dataString), serialPortInputStream,serialPortOutputStream, SYSTEM_VOLTAGE);
					aPlSeriesWriter = new PLSeriesWriter(sw, aPLSeriesReader) ;
			        
					 
				} catch (SerialPortCommunicationException e) {
					// TODO Auto-generated catch block
					String text = Utils.getStringException(e);
					logger.warn(text);
					Hashtable h = new Hashtable();
					h.put("info", text);
					throw new MicrocontrollerCommunicationException(h);
				}
			}

			public void initializeSerialComm() throws SerialPortCommunicationException {
				
				Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

				CommPortIdentifier portId = null;
				CommPortIdentifier currPortId=null;
				while (portId == null && portEnum.hasMoreElements()) {
					currPortId = (CommPortIdentifier) portEnum.nextElement();
					//System.out.println("currPortId=" + currPortId.getName());
					logger.debug("PLA-"+"looking for ports, currPortId=" + currPortId);

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
					logger.debug("PLA-"+"Could not find COM port.");
					throw new SerialPortCommunicationException("Could not find COM port");
				}
				logger.debug("PLA-"+"Found COM Port.");
				try {
					serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
					serialPort.setSerialPortParams(DATA_RATE,
							SerialPort.DATABITS_8,
							SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);

					// open the streams
					logger.debug("PLA-"+"serialPort, set parameters");
					
					serialPort.notifyOnDataAvailable(true);
					///serialPort..write().write(InetAddress.getLocalHost().toString().t());
					serialPortInputStream = serialPort.getInputStream();
					serialPortOutputStream = serialPort.getOutputStream();
					logger.debug("PLA-"+"serialPort, got streams");
					
					if (serialPortInputStream == null) {
						//System.out.println("serialPortInputStream is null.");
						logger.debug("PLA-"+"serialPortInputStream is null.");
						throw new SerialPortCommunicationException("SerialPortInputStream is null");
					}
					
					if (serialPortOutputStream == null) {
						//System.out.println("serialPortOutputStream is null.");
						logger.debug("PLA-"+"serialPortOutputStream is null.");
						throw new SerialPortCommunicationException("SerialPortOutputStream is null");
					}
				} catch (Exception e) {
					//System.out.println(e.toString());
					// TODO Auto-generated catch block
					StringWriter sw = new StringWriter();
					e.printStackTrace( new PrintWriter( sw )    );
					String callStack = sw.toString();
					logger.info(callStack);
					//System.out.println(sw.toString());
				}
				
			}
			
			 
			@Override
			public BufferedReader getReader() throws IOException {
				logger.info(" When asking the reader.getCurrentCommand()=" + aPlSeriesWriter.getCurrentCommand());
				if(aPlSeriesWriter.getCurrentCommand().startsWith("AsyncData")) {
					if(enableAsyncUpdate) {
						aPLSeriesReader.setAsyncMode(true);
					}
				}
				//aPLSeriesReader.setCurrentCommand("PulseFinished");
//				if(!aPlSeriesWriter.getCurrentCommand().equals("GetSensorData")) {
//					aPLSeriesReader.setCurrentCommand(aPlSeriesWriter.getCurrentCommand());
//				}
				return  aPLSeriesReader;
				
			}
			/**
			 * note how the reader is always created when asking for a writer
			 * that way we guarantee that the command passed to the writer will end up in the reader
			 */
			@Override
			public BufferedWriter getWriter() throws IOException {
				// TODO Auto-generated method stub
				aPLSeriesReader = new PLSeriesReader(new StringReader(dataString), serialPortInputStream,serialPortOutputStream, SYSTEM_VOLTAGE);
				
				aPlSeriesWriter = new PLSeriesWriter(sw, aPLSeriesReader) ;
		        
				 return aPlSeriesWriter;

			}
}
