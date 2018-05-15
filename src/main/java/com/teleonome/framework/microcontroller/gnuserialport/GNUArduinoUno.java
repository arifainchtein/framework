package com.teleonome.framework.microcontroller.gnuserialport;



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
import com.teleonome.framework.utils.Utils;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

public class GNUArduinoUno extends MicroController implements SerialPortEventListener {

	Logger logger;
	String SerialPortID = "/dev/ttyAMA0";
	private static final String PORT_NAMES[] = { "/dev/tty.usbmodem641", "/dev/ttyACM0", "/dev/ttyAMA0", "/dev/ttyACM1","/dev/ttyUSB0","/dev/cu.usbmodem1411" };
	SerialPort serialPort=null;
	private GNUArduinoReader input;
	//private InputStream input;
	
	private BufferedWriter output;

	private static final int TIME_OUT = 5000;
	private int DATA_RATE = 2400;
	private int INTER_SENSOR_READ_TIME_OUT_MILLISECONDS=100;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;
	
	public GNUArduinoUno(DenomeManager d, String n){
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
			logger.warn(Utils.getStringException(e1));
			System.exit(-1);
		}


		CommPortIdentifier currPortId=null;
		while (portId == null && portEnum.hasMoreElements()) {
			currPortId = (CommPortIdentifier) portEnum.nextElement();
			//System.out.println("currPortId=" + currPortId.getName());
			logger.debug("looking for ports, currPortId=" + currPortId.getName());

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
		logger.debug("Found COM Port1.");
		try {
			//
			// get the data rate for the arduno ie get the DeneWord , get the dene that represents the arduino
			JSONArray allDenes = (JSONArray)aDenomeManager.getDeneByDeneType(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_COMPONENTS, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);
			JSONObject arduinoUnoMicrocontrollerDene = null;
			JSONObject microcontrollerDene = null;
			String className="";
			for(int i=0;i<allDenes.length();i++) {
				microcontrollerDene = allDenes.getJSONObject(i);
				className = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(microcontrollerDene, TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(className.equals("com.teleonome.framework.microcontroller.gnuserialport.GNUArduinoUno")) {
					arduinoUnoMicrocontrollerDene=microcontrollerDene;
				}
			}
			if(arduinoUnoMicrocontrollerDene==null) {
				logger.warn("Could not find Arduino Uno Microcontroller Dene");
				Hashtable<String, String> h = new Hashtable();
				h.put("message","Could not find COM port");
				throw new MicrocontrollerCommunicationException(h);
			}
			String pointerToCommParamsDene =  (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(arduinoUnoMicrocontrollerDene, TeleonomeConstants.DENEWORD_MICROCONTROLLER_COMMUNICATION_PROTOCOL, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			logger.debug("using pointerToCommParamsDene=" + pointerToCommParamsDene);
			
			JSONObject commParamsDene = aDenomeManager.getDeneByIdentity(new Identity(pointerToCommParamsDene));
			DATA_RATE = ((Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(commParamsDene, "Serial Data Rate", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)).intValue();
			logger.debug("using datarate=" + DATA_RATE);
			int counter=0;
			boolean openAndTested=false;
			do {
				logger.debug("about to open port 2" );
				serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
				//serialPort.disableReceiveTimeout();
				serialPort.enableReceiveTimeout(30000);
				serialPort.enableReceiveThreshold(0);
				serialPort.setSerialPortParams(DATA_RATE,
						SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
				//serialPort.setRTS(false);
				//serialPort.setDTR(true);

				// open the streams

				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);

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
				
				//
				// now open and test it
				//
				input = new GNUArduinoReader(new BufferedReader(new InputStreamReader(serialPortInputStream)));
				output = new BufferedWriter(new OutputStreamWriter(serialPortOutputStream));

				try{
					
					logger.info("About to ping");
					String actuatorCommand="Ping";
					output.write(actuatorCommand,0,actuatorCommand.length());
					//serialPortOutputStream.write( actuatorCommand.getBytes() );
					//Thread.sleep(1000);
					output.flush();
					logger.info("waiting for mother to answer" );
					
					String inputLine = input.readLine();
					logger.info("mother answered =" + inputLine);
					
					openAndTested=true;
				}catch(IOException e) {
					logger.warn(Utils.getStringException(e));
				}
				if(!openAndTested) {
					logger.warn("Ping Failed,closing serial port, retrying in 10 secs, counter="+counter );
					counter++;
					serialPort.close();
					Thread.sleep(10000);
				}
			}while(!openAndTested);
			
			
			//
			// to make sure that the serial port has not hung, do a test
			//
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

	public GNUArduinoReader getReader() throws IOException{
		//logger.debug("Arduino uno asking for reader" );
		//String trace = Utils.generateMethodTrace();
		//logger.debug(trace);
		
		input = new GNUArduinoReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream())));
		
		return input;
	}
	
	

	public BufferedWriter getWriter() throws IOException{
		//logger.debug("Arduino uno asking for writer1" );
		output = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));

		return output;
	}


	@Override
	public void serialEvent(SerialPortEvent arg0) {
		// TODO Auto-generated method stub
		//logger.debug("serialEvent received " + arg0.getEventType() );
	}
	

	
	

}
