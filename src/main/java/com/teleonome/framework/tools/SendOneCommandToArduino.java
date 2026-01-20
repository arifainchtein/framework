package com.teleonome.framework.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fazecast.jSerialComm.*;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.microcontroller.annabellemicrocontroller.AnnabelleReader;
import com.teleonome.framework.microcontroller.annabellemicrocontroller.AnnabelleWriter;
import com.teleonome.framework.utils.Utils;
import org.apache.log4j.Logger;


public class SendOneCommandToArduino {

	private static String buildNumber="";
	Logger logger;
	String SerialPortID = "/dev/ttyUSB0";
	private static final String PORT_NAMES[] = { "ttyUSB0","/dev/ttyUSB1"};
	SerialPort serialPort;
	private BufferedReader input=null;
	private BufferedWriter output=null;

	private static final int TIME_OUT = 20000;
	private int DATA_RATE = 115200;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;
	boolean verbose=false;
	ArrayList<String> commandExecutionResults = new ArrayList();

	public SendOneCommandToArduino(String command, boolean v, File file) {
		ArrayList<String> commands = new ArrayList();
		commands.add(command);
		process( commands,  v,  file);
	}
	public SendOneCommandToArduino(ArrayList<String> commands, boolean v, File file) {
		process(commands,  v,  file);
	}
	private void process(ArrayList<String> commands, boolean v, File file) {
		// TODO Auto-generated constructor stub
		verbose=v;
		try {
			init();
		} catch (MicrocontrollerCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		BufferedWriter oneCommandOutput=null;
		BufferedReader reader=null;
		// **************
		try {
			oneCommandOutput=  new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
			reader = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			String command;

			for(int i=0;i<commands.size();i++) {

				command = commands.get(i);
				if(command.startsWith("$")) {
					if(command.startsWith("$Delay#")) {
						int seconds = Integer.parseInt(command.substring(7));
						System.out.println("delaying " + seconds + " seconds at " + new Date());
						try {
							Thread.sleep(seconds*1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}else {
					System.out.println("sending " + command);			
					oneCommandOutput.write(command,0,command.length());
					//serialPortOutputStream.write( actuatorCommand.getBytes() );
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					oneCommandOutput.flush();
					if(verbose)System.out.println("waiting for response ");
					String line;
					if(command.equals("GetSensorData")) {
						line = reader.readLine();
						commandExecutionResults.add(line);
						if(file!=null) {
							FileUtils.writeStringToFile(file, line + System.lineSeparator(), Charset.defaultCharset(), true);
						}else {
							System.out.println(line);
						}
					}else if( command.equals("exportDSDCSV") ) {	
						SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy");
						String currentDate = dateFormat.format(new Date());
						String fileName = "exportDSD_" + currentDate + ".txt";	
						FileWriter fileWriter = new FileWriter(fileName, true);
						BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
						int counter=1;
						do{
							line = reader.readLine();
							System.out.println(line);
							bufferedWriter.write(line);
							bufferedWriter.newLine();  // Add a newline character
							counter++;
						}while(!line.equals("")  );
						// Close the resources
						bufferedWriter.close();
						System.out.println(counter + " lines exported to  " + fileName);
					}else if( command.startsWith("GenerateDSDReport") ) {	
						SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy");
						String currentDate = dateFormat.format(new Date());
						String fileName = "reportDSD_" + currentDate + ".txt";	
						FileWriter fileWriter = new FileWriter(fileName, true);
						BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
						int counter=1;
						do{
							line = reader.readLine();
							System.out.println(line);
							bufferedWriter.write(line);
							bufferedWriter.newLine();  // Add a newline character
							counter++;
						}while(!line.equals("Ok-GenerateDSDReport")  );
						// Close the resources
						bufferedWriter.close();
						System.out.println(counter + " lines exported to  " + fileName);
					}else {
						do{
							line = reader.readLine();
							commandExecutionResults.add(line);
							if(file!=null) {
								FileUtils.writeStringToFile(file, line + System.lineSeparator(), Charset.defaultCharset(), true);
							}else {
								System.out.println(line);
							}
						}while(!line.contains("Ok") && !line.contains("Failure") );

					}
				}

			}


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			if(serialPortInputStream!=null)serialPortInputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(serialPortOutputStream!=null)
			try {
				serialPortOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if(serialPort!=null)serialPort.closePort();
	}

	public ArrayList<String> getCommandExecutionResults(){
		return commandExecutionResults;
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

private void closeSerialPort() {
	serialPort.closePort();
}
private void connectToSerialPort() throws IOException, SerialPortCommunicationException {
	boolean openAndTested=false;
	int counter=0;
	do {


		///serialPort..write().write(InetAddress.getLocalHost().toString().t());
		serialPortInputStream = serialPort.getInputStream();
		serialPortOutputStream = serialPort.getOutputStream();

		if (serialPortInputStream == null) {
			System.out.println("serialPortInputStream is null.");
			logger.warn("serialPortInputStream is null.");
			throw new SerialPortCommunicationException("SerialPortInputStream is null");
		}

		if (serialPortOutputStream == null) {
			System.out.println("serialPortOutputStream is null.");
			logger.warn("serialPortOutputStream is null.");
			throw new SerialPortCommunicationException("SerialPortOutputStream is null");
		}

		
		try{

			logger.info("About to ping");
			String actuatorCommand="Ping";
			output.write(actuatorCommand,0,actuatorCommand.length());
			//serialPortOutputStream.write( actuatorCommand.getBytes() );
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			output.flush();
			logger.info("waiting for mother to answer" );

			String inputLine = input.readLine();
			logger.info("mother answered =" + inputLine);

			openAndTested=true;
			output.close();
			input.close();
		}catch(IOException e) {
			logger.warn(Utils.getStringException(e));
		}
		if(!openAndTested) {
			logger.warn("Ping Failed, retrying in 10 secs, counter="+counter );
			counter++;
			//serialPort.close();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}while(!openAndTested);
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
			if(verbose)System.out.println("looking for ports, currPortId=" + currPortId);

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
			System.exit(0);

		}
		if(verbose)System.out.println("Found COM Port.");
		try {
			//
			// get the data rate for the arduno ie get the DeneWord , get the dene that represents the arduino
			if(verbose)System.out.println("using datarate=" + DATA_RATE);
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
				if(verbose)System.out.println("serialPortInputStream is null.");
			}

			if (serialPortOutputStream == null) {
				if(verbose)System.out.println("serialPortOutputStream is null.");

			}




			//
			// SKIP configure pins
			//int sensorNumberOfReadsPerPulse = 10;
			// output.write(longToBytes(sensorNumberOfReadsPerPulse));


			if(verbose)System.out.println("finished initializing" );

		} catch (Exception e) {

			// TODO Auto-generated catch block
			StringWriter sw = new StringWriter();
			e.printStackTrace( new PrintWriter( sw )    );
			String callStack = sw.toString();
			System.out.println(callStack);

		}
	}
 */
}
