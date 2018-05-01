package com.teleonome.framework.hypothalamus;

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

import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class TestComms {
	String SerialPortID = "/dev/ttyAMA0";
	private static final String PORT_NAMES[] = { "/dev/tty.usbmodem641", "/dev/ttyACM0", "/dev/ttyAMA0", "/dev/ttyACM1","/dev/ttyUSB0","/dev/cu.usbmodem1411" };
	SerialPort serialPort;
	private BufferedReader input;
	//private InputStream input;
	private static final int TIME_OUT = 20000;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;
	
	private OutputStreamWriter output;

	public TestComms(){
		// TODO Auto-generated method stub
				Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

				CommPortIdentifier portId = null;
				

				CommPortIdentifier currPortId=null;
				while (portId == null && portEnum.hasMoreElements()) {
					currPortId = (CommPortIdentifier) portEnum.nextElement();
					//System.out.println("currPortId=" + currPortId.getName());
				
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
					System.exit(-1);
				}
				try {
					//
					// get the data rate for the arduno ie get the DeneWord , get the dene that represents the arduino
					serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
					serialPort.disableReceiveTimeout();
					serialPort.enableReceiveThreshold(1);
					serialPort.setSerialPortParams(9600,
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
						throw new SerialPortCommunicationException("SerialPortInputStream is null");
					}

					if (serialPortOutputStream == null) {
						System.out.println("serialPortOutputStream is null.");
						throw new SerialPortCommunicationException("SerialPortOutputStream is null");
					}

					input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
					BufferedWriter writer=  new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
					
					String commandToSend = "GetSensorData";
					for(int i=0;i<100;i++){
						writer.write("GetSensorData",0,"GetSensorData".length());
						writer.flush();
						String inputLine = input.readLine();
						String[] tokens = inputLine.split("#");
						System.out.println("received inputLine=" + inputLine + " tokens=" + tokens.length);
						Thread.sleep(1000);
					}
					
					
					
					
					
				} catch (Exception e) {

					// TODO Auto-generated catch block
					StringWriter sw = new StringWriter();
					e.printStackTrace( new PrintWriter( sw )    );
					String callStack = sw.toString();
					System.out.println(callStack);
					
				}

	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestComms();
	}

}
