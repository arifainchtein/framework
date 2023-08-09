package com.teleonome.framework.microcontroller.cajalmicrocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.LifeCycleEventListener;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.SerialPortCommunicationException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.MotherMicroController;
import com.teleonome.framework.utils.Utils;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
public class CajalController extends MotherMicroController implements SerialPortEventListener,  LifeCycleEventListener {

	Logger logger;
	String SerialPortID = "/dev/ttyAMA0";
	private static final String PORT_NAMES[] = { "/dev/tty.usbmodem641", "/dev/ttyACM0", "/dev/ttyAMA0", "/dev/ttyUSB0","/dev/ttyUSB1","/dev/cu.usbmodem1411" };
	SerialPort serialPort=null;
	private CajalReader input;
	//private InputStream input;
	
	//private BufferedWriter output;
	private CajalWriter output;

	private static final int TIME_OUT = 5000;
	private int DATA_RATE = 9600;
	private int INTER_SENSOR_READ_TIME_OUT_MILLISECONDS=100;
	InputStream serialPortInputStream = null;
	OutputStream serialPortOutputStream = null;
	
	public CajalController(DenomeManager d, String n){
		super(d,n);
		logger = Logger.getLogger(getClass());
		setEnableAsyncUpdate(true);
	}
	
	public void processLifeCycleEvent(String lifeCycleEvent) {
		// TODO Auto-generated method stub
		logger.debug("1-processing life cycle even=t=" + lifeCycleEvent);
		BufferedWriter writer;
		try {
			writer = getWriter();
			if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE)) {
				try {
					//writer.write(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE.length());
					sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_START_SYNCHRONOUS_CYCLE);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE)) {
				try {
					sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE);
					//writer.write(TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_END_SYNCHRONOUS_CYCLE.length());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE)) {
				try {
					sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE);
				//	writer.write(TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_START_ASYNCHRONOUS_CYCLE.length());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE)) {
				try {
					sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE);
					//writer.write(TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_END_ASYNCHRONOUS_CYCLE.length());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}else if(lifeCycleEvent.equals(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE)) {
				try {
					sendCommand(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE);
				//	writer.write(TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE, 0, TeleonomeConstants.LIFE_CYCLE_EVENT_START_AWAKE.length());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	}
	
	@Override
	public void init(JSONArray configParams) throws MicrocontrollerCommunicationException {
		// TODO Auto-generated method stub
		CommPortIdentifier portId = null;
		try {
			INTER_SENSOR_READ_TIME_OUT_MILLISECONDS = ((Integer)aDenomeManager.getDeneWordValueByName(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_VITAL, "Inter Sensor Read Timeout Milliseconds")).intValue();

		} catch (InvalidDenomeException e1) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e1));
			System.exit(-1);
		}
		
		Enumeration portEnum = null;
		CommPortIdentifier.getPortIdentifiers();

		


		CommPortIdentifier currPortId=null;
		int counter=0;
		int maxNumberReconnects=3;
		boolean keepGoing=true;
		do {
			 portEnum = CommPortIdentifier.getPortIdentifiers();
			 logger.debug("looking for ports, portEnum=" + portEnum);
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
				if(counter<=maxNumberReconnects) {
					counter++;
					logger.info("Could not find Serial Port," + counter + " out of " + maxNumberReconnects);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
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
		try {
			//
			// get the data rate for the arduno ie get the DeneWord , get the dene that represents the arduino
			JSONArray allDenes = (JSONArray)aDenomeManager.getDeneByDeneType(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_COMPONENTS, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);
			JSONObject cajalMicrocontrollerDene = null;
			JSONObject microcontrollerDene = null;
			String className="";
			for(int i=0;i<allDenes.length();i++) {
				microcontrollerDene = allDenes.getJSONObject(i);
				className = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(microcontrollerDene, TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(className.equals("com.teleonome.framework.microcontroller.cajalmicrocontroller.CajalController")) {
					cajalMicrocontrollerDene=microcontrollerDene;
				}
			}
			if(cajalMicrocontrollerDene==null) {
				logger.warn("Could not find Cajal Microcontroller Dene");
				Hashtable<String, String> h = new Hashtable();
				h.put("message","Could not find COM port");
				throw new MicrocontrollerCommunicationException(h);
			}
			try {
				Object o =  DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(cajalMicrocontrollerDene, TeleonomeConstants.DENEWORD_MICROCONTROLLER_ASYNC_REQUEST_DELAY_MILLIS, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				if(o!=null) {
					asyncRequestMillisecondsDelay = (int)o;
					logger.info("Using asyncRequestMillisecondsDelay=" + asyncRequestMillisecondsDelay);
				}else {
					logger.info("Did not find asyncRequestMillisecondsDelay");
				}
				
			}catch(Exception e) {
				logger.warn(Utils.getStringException(e));
			}
//			String pointerToCommParamsDene =  (String)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(arduinoUnoMicrocontrollerDene, TeleonomeConstants.DENEWORD_MICROCONTROLLER_COMMUNICATION_PROTOCOL, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
//			logger.debug("using pointerToCommParamsDene=" + pointerToCommParamsDene);
//			JSONObject commParamsDene = aDenomeManager.getDeneByIdentity(new Identity(pointerToCommParamsDene));
//			DATA_RATE = ((Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(commParamsDene, "Serial Data Rate", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)).intValue();
			
			JSONObject configDene;
			logger.debug(" configParams.size= " + configParams.length());
			
			for(int i=0;i<configParams.length();i++){
				try {
					configDene = configParams.getJSONObject(i);
					logger.debug(" configDene.getString(Name)= " + configDene.getString("Name"));
					if(configDene.getString("Name").equals("Serial Data Rate")) {
						DATA_RATE = ((Integer)DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(configDene, "Serial Data Rate", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE)).intValue();
						logger.debug(" Cajal microcontroller DATA_RATE " + DATA_RATE);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
				
			}
			
			logger.debug("using datarate=" + DATA_RATE);
		    counter=0;
			boolean openAndTested=false;
			logger.debug("about to open port , sleeping 1 sec first" );
			
			Thread.sleep(1000);
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
			logger.debug("opened port , sleeping another  sec " );
			Thread.sleep(1000);
			//serialPort.disableReceiveTimeout();
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			serialPort.enableReceiveTimeout(30000);
			serialPort.enableReceiveThreshold(1);
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			//serialPort.setRTS(false);
			//serialPort.setDTR(true);
			//serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |  SerialPort.FLOWCONTROL_RTSCTS_OUT);
			serialPort.setDTR(true);
			// open the streams

			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
			
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
				
				//
				// now open and test it
				//
				input = new CajalReader(new BufferedReader(new InputStreamReader(serialPortInputStream)), aDenomeManager);
				output = new CajalWriter(new OutputStreamWriter(serialPortOutputStream),input);

				try{
					
					logger.info("About to ping");
					String actuatorCommand="Ping";
					output.write(actuatorCommand,0,actuatorCommand.length());
					//serialPortOutputStream.write( actuatorCommand.getBytes() );
					Thread.sleep(1000);
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
					Thread.sleep(10000);
				}
			}while(!openAndTested);
			
			
			//
			// to make sure that the serial port has not hung, do a test
			//
			logger.debug("finished initializing Cajal" );

		} catch (Exception e) {

			// TODO Auto-generated catch block
			StringWriter sw = new StringWriter();
			e.printStackTrace( new PrintWriter( sw )    );
			String callStack = sw.toString();
			System.out.println(callStack);
			logger.warn(callStack);
		}
	}

	public CajalReader getReader() throws IOException{
		//logger.debug("Arduino uno asking for reader" );
		//String trace = Utils.generateMethodTrace();
		//logger.debug(trace);
		return output.getReader();
	}
	
	

	public BufferedWriter getWriter() throws IOException{
		//logger.debug("Arduino uno asking for writer1" );
		input = new CajalReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream())), aDenomeManager);
		output = new CajalWriter(new OutputStreamWriter(serialPort.getOutputStream()),input );

		return output;
	}


	@Override
	public void serialEvent(SerialPortEvent arg0) {
		// TODO Auto-generated method stub
		//logger.debug("serialEvent received " + arg0.getEventType() );
	}

	public boolean verifyUserCommandCode(String userCode) throws IOException{
		String actuatorCommand = "VerifyUserCode#" + userCode;
		String result = "";
		boolean toReturn=true;
		boolean keepGoing=true;
		String commandCode="";
		int maxTries=5;
		int counter=0;
		while(keepGoing) {
			result = sendCommand(actuatorCommand);
			logger.debug("actuatorCommandt=" + actuatorCommand + " resultr=" + result);
			
			if(	result.equals(TeleonomeConstants.COMMAND_REQUEST_INVALID_CODE) ||
				result.equals(TeleonomeConstants.COMMAND_REQUEST_VALID_CODE)
			) {
				if(result.equals(TeleonomeConstants.COMMAND_REQUEST_INVALID_CODE)) {
					toReturn=false;
				}else if(result.equals(TeleonomeConstants.COMMAND_REQUEST_VALID_CODE)) {
					toReturn=true;
				}
				keepGoing=false;
			}else {
				counter++;
				logger.debug("bad response to validate user result=" + result + " asking again,counter=" + counter + " maxTries=" + maxTries);
				if(counter>=maxTries) {
					toReturn=false;
					keepGoing=false;
				}else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		return toReturn;
	}

	@Override
	public String getCommandCode() throws IOException {
		// TODO Auto-generated method stub
	//	output = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
		String actuatorCommand = "GetCommandCode";
		boolean keepGoing=true;
		String commandCode="";
		int maxTries=5;
		int counter=0;
		while(keepGoing) {
			commandCode = sendCommand(actuatorCommand);
			if(commandCode!=null && commandCode.length()==6) {
				keepGoing=false;
			}else {
				counter++;
				logger.debug("bad command code=" + commandCode + " asking again,counter=" + counter + " maxTries=" + maxTries);
				
				if(counter>=maxTries) {
					commandCode="999999";
					keepGoing=false;
				}else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return commandCode;
		
	}
	
	@Override
	public String getDigitalGeppettoCommandCode() throws IOException {
		// TODO Auto-generated method stub
		//output = new BufferedWriter(new OutputStreamWriter(serialPort.getOutputStream()));
		String actuatorCommand = "GetDigitalGeppettoCommandCode";
		boolean keepGoing=true;
		String commandCode="";
		int maxTries=5;
		int counter=0;
		while(keepGoing) {
			commandCode = sendCommand(actuatorCommand);
			if(commandCode!=null && commandCode.length()==6) {
				keepGoing=false;
			}else {
				counter++;
				logger.debug("bad command code=" + commandCode + " asking again,counter=" + counter + " maxTries=" + maxTries);
				
				if(counter>=maxTries) {
					commandCode="999999";
					keepGoing=false;
				}else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return commandCode;
		
	}
	
	@Override
	public  boolean rebootingHypothalamus() throws IOException{
		
		String result = "";
		boolean toReturn=true;
		boolean keepGoing=true;
		String commandCode="";
		int maxTries=5;
		int counter=0;
		while(keepGoing) {
			result = sendCommand(TeleonomeConstants.MOTHER_COMMAND_REBOOT_HYPOTHALAMUS);
			if(	result.equals(TeleonomeConstants.MOTHER_COMMAND_REBOOT_HYPOTHALAMUS_OK)) {
				toReturn=true;
				keepGoing=false;
			}else {
				counter++;
				logger.debug("bad response to rebootingHypothalamus  result=" + result + " asking again,counter=" + counter + " maxTries=" + maxTries);
				if(counter>=maxTries) {
					toReturn=false;
					keepGoing=false;
				}else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		return toReturn;
	}
	
	@Override
	public  boolean shuttingDownHypothalamus() throws IOException{
		String result = "";
		boolean toReturn=true;
		boolean keepGoing=true;
		String commandCode="";
		int maxTries=5;
		int counter=0;
		while(keepGoing) {
			result = sendCommand(TeleonomeConstants.MOTHER_COMMAND_SHUTDOWN_HYPOTHALAMUS);
			if(	result.equals(TeleonomeConstants.MOTHER_COMMAND_SHUTDOWN_HYPOTHALAMUS_OK)) {
				toReturn=true;
				keepGoing=false;
			}else {
				counter++;
				logger.debug("bad response to shuttingDownHypothalamus  result=" + result + " asking again,counter=" + counter + " maxTries=" + maxTries);
				if(counter>=maxTries) {
					toReturn=false;
					keepGoing=false;
				}else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		return toReturn;
	}
	
	private String sendCommand(String actuatorCommand) throws IOException {
		// TODO Auto-generated method stub
		input = new CajalReader(new BufferedReader(new InputStreamReader(serialPort.getInputStream())), aDenomeManager);
		
		output = new CajalWriter(new OutputStreamWriter(serialPort.getOutputStream()),input);
		logger.debug("sending command:"+ actuatorCommand);
		output.write(actuatorCommand,0,actuatorCommand.length());
		//serialPortOutputStream.write( actuatorCommand.getBytes() );
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output.flush();
			String inputLine = input.readLine();
		logger.debug("receivibg response :"+ inputLine);
		input.close();
		output.close();
		
		return inputLine;
	}
	

	
	

}
