package com.teleonome.framework.tools;

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
import org.json.JSONException;
import org.json.JSONObject;

import com.fazecast.jSerialComm.*;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.utils.Utils;

public class SerialPortListener {

	Logger logger;
	String SerialPortID = "/dev/ttyUSB0";
	private static final String PORT_NAMES[] = { "/dev/tty.usbmodem641", "/dev/ttyACM0", "/dev/ttyAMA0", "/dev/ttyACM1","/dev/ttyUSB0","/dev/cu.usbmodem1411" };
	SerialPort serialPort;
	private BufferedReader input;
	//private InputStream input;

	private OutputStreamWriter output;

	private static final int TIME_OUT = 20000;
	private int DATA_RATE = 9600;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;

	public SerialPortListener() throws IOException {
		System.out.println("before init");
		try {
			init();
		} catch (MicrocontrollerCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("after init");
		BufferedReader reader=null;
		reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
		String line;
		try {
			while (true) {
				System.out.println("BOUT TO READ LINE");
				line = reader.readLine();
				System.out.println(line);
				Thread.sleep(2000);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
	public void init() throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		//CommPortIdentifier portId = null;


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

		JSONObject configDene;


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
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 30000, 0);
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

		// Set DTR
		serialPort.setDTR();

		// Add event listener for data available
		serialPort.addDataListener(new SerialPortDataListener() {
			@Override
			public int getListeningEvents() {
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
			}

			@Override
			public void serialEvent(SerialPortEvent event) {
				if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
					// Handle data available event
					// This replaces the serialPortEventListener functionality
				}
			}
		});

		//
		// to make sure that the serial port has not hung, do a test
		//
		logger.debug("finished initializing Annabelle" );


	}

	/*
	public void init() {
		// TODO Auto-generated method stub
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		CommPortIdentifier portId = null;


		CommPortIdentifier currPortId=null;
		while (portId == null && portEnum.hasMoreElements()) {
			currPortId = (CommPortIdentifier) portEnum.nextElement();
			//System.out.println("currPortId=" + currPortId.getName());
			System.out.println("looking for ports, currPortId=" + currPortId);

			for (String portName : PORT_NAMES) {
				if ( currPortId.getName().equals(portName) || currPortId.getName().startsWith(portName) ){
					// Try to connect to the Arduino on this port
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");

		}
		System.out.println("Found COM Port.");
		try {
			//
			// get the data rate for the arduno ie get the DeneWord , get the dene that represents the arduino
			System.out.println("using datarate=" + DATA_RATE);
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
			}

			if (serialPortOutputStream == null) {
				System.out.println("serialPortOutputStream is null.");

		}

			/*
		        try{
		        	String message="Sazirac is ready on " + graphMaker.getIpAddress() + ".  The arduino is listening on usb port " + uSBPort.trim();
					Utils.sendMessage(monitoringEmailAddress, monitoringEmailAddress, message, message, sMTPServer, sMTPServerName, sMTPServerPassword);
				}catch (ActionNotCompletedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 




			//
			// SKIP configure pins
			//int sensorNumberOfReadsPerPulse = 10;
			// output.write(longToBytes(sensorNumberOfReadsPerPulse));


			System.out.println("finished initializing" );

		} catch (Exception e) {

			// TODO Auto-generated catch block
			StringWriter sw = new StringWriter();
			e.printStackTrace( new PrintWriter( sw )    );
			String callStack = sw.toString();
			System.out.println(callStack);

		}
	}
	 */
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

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			new SerialPortListener();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
