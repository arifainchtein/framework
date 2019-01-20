package com.teleonome.framework.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.io.FileUtils;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class SendOneCommandToArduino {

	private static String buildNumber="";
	//Logger logger;
	String SerialPortID = "/dev/ttyUSB0";
	private static final String PORT_NAMES[] = { "/dev/tty.usbmodem641", "/dev/ttyACM0", "/dev/ttyAMA0", "/dev/ttyACM1","/dev/ttyUSB0","/dev/cu.usbmodem1411" };
	SerialPort serialPort;
	private BufferedReader input=null;
	private BufferedWriter output=null;

	private static final int TIME_OUT = 20000;
	private int DATA_RATE = 9600;
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
		init();

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
		if(serialPort!=null)serialPort.close();
	}

	public ArrayList<String> getCommandExecutionResults(){
		return commandExecutionResults;
	}
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
}
