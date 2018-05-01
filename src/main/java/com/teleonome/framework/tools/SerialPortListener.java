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

import org.apache.log4j.Logger;


import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SerialPortListener {

	//Logger logger;
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
	
	public SerialPortListener() {
		System.out.println("before init");
		init();
		System.out.println("after init");
		BufferedReader reader=null;
		try {
			reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
			 */



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
		new SerialPortListener();
	}

}
