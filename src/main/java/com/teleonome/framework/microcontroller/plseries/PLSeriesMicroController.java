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
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.PlainReader;
import com.teleonome.framework.utils.Utils;
import com.fazecast.jSerialComm.*;

public class PLSeriesMicroController extends MicroController {
			

			StringWriter sw = new StringWriter();
			String  dataString="dataString";
			PLSeriesReader aPLSeriesReader = null;//
			Logger logger;
			private static final String PORT_NAMES[] = {  "/dev/ttyUSB0","ttyUSB0"};
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
			
			
			public void initializeSerialComm() throws SerialPortCommunicationException, MicrocontrollerCommunicationException {
				SerialPort portId = null;
				SerialPort[] allPorts = null;
				int counter=0;
				int maxNumberReconnects=3;
				boolean keepGoing=true;
				do {
					allPorts = SerialPort.getCommPorts();
					logger.debug("looking for ports, found " + allPorts.length + " ports");
					
					for (SerialPort port : allPorts) {
						logger.debug("looking for ports, currPortId=" + port.getSystemPortName());
			
						for (String portName : PORT_NAMES) {
							if (port.getSystemPortName().equals(portName) || port.getSystemPortName().startsWith(portName)) {
								portId = port;
								break;
							}
						}
						if (portId != null) break;
					}
					
					if (portId == null) {
						if(counter<=maxNumberReconnects) {
							counter++;
							logger.info("Could not find Serial Port," + counter + " out of " + maxNumberReconnects);
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}else {
							logger.warn("Could not find COM port.");
							Hashtable<String, String> h = new Hashtable();
							h.put("message","Could not find COM port");
							throw new MicrocontrollerCommunicationException(h);
						}
					}else {
						keepGoing=false;
					}
				}while(keepGoing);
				logger.debug("Found COM Port1.");
				
					
					
					logger.debug("using datarate=" + DATA_RATE);
				    counter=0;
					boolean openAndTested=false;
					logger.debug("about to open port , sleeping 1 sec first" );
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					

					// Configure and open the serial port
								serialPort = portId;
								serialPort.setComPortParameters(DATA_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
								serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 30000, 5000);
								serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
								
								if (!serialPort.openPort()) {
									logger.warn("Failed to open serial port");
									Hashtable<String, String> h = new Hashtable();
									h.put("message","Failed to open serial port");
									throw new MicrocontrollerCommunicationException(h);
								}
								
								logger.debug("opened port , sleeping another  sec " );
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								serialPortInputStream = serialPort.getInputStream();
								serialPortOutputStream = serialPort.getOutputStream();

								if (serialPortInputStream == null) {
									logger.warn("serialPortInputStream is null.");
									Hashtable<String, String> h = new Hashtable();
									h.put("message","SerialPortInputStream is null");
									throw new MicrocontrollerCommunicationException(h);
								}

								if (serialPortOutputStream == null) {
									logger.warn("serialPortOutputStream is null.");
									Hashtable<String, String> h = new Hashtable();
									h.put("message","SerialPortOutputStream is null");
									throw new MicrocontrollerCommunicationException(h);
								}

								// Set DTR and RTS - the PL-series MeterBus adapter is an unpowered
								// cable that draws its power parasitically off these two control
								// lines, so both must be held high or the adapter never responds.
								// RXTX asserted both of these by default on open; jSerialComm does not.
								serialPort.setDTR();
								serialPort.setRTS();

					//
					// to make sure that the serial port has not hung, do a test
					//
					logger.debug("finished initializing Annabelle" );

				
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
