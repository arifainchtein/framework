package com.teleonome.framework.hypothalamus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.denome.DenomeManager.MutationActionsExecutionResult;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.InvalidMutation;
import com.teleonome.framework.exception.MicrocontrollerCommunicationException;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.microcontroller.MicroController;
import com.teleonome.framework.microcontroller.MotherMicroController;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.process.DiscoverTeleonoms;
import com.teleonome.framework.utils.Utils;

public abstract class Hypothalamus {
	
	public DenomeManager aDenomeManager=null;
	public Logger logger=null;
	MqttClient anMqttClient ;
	//
	// primaryIpAddress represents the variable that will be sent to
	// the mother to be displayed in an lcd
	// depending on the available hardware, it will give preference
	// to network values.
	// therefore, it will first check to see if wlan0 has a value, if not
	// it will check eth0 and if there is no value it will use
	// the value for wlan1 (which will be of the type 176.16.1.1)
	//
	public String primaryIpAddress="";
	private JSONObject networkAdapterInfoJSONObject;
	public Hashtable microControllerPointerMicroControllerIndex;
	public String hostName;
	String initOperationalMode;
	public Vector teleonomesToReconnect =new Vector();
	SimpleDateFormat simpleFormatter = new SimpleDateFormat("dd/MM/yy HH:mm");
	SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	MotherMicroController motherMicroController=null;
	String aMotherMicroControllerPointer="";
	String mqttBrokerAddress = "tcp://localhost:1883";
    String mqttClientId = TeleonomeConstants.PROCESS_HYPOTHALAMUS;
    MemoryPersistence persistence = new MemoryPersistence();
    public boolean performTimePrunningAnalysis=false;
    int pacemakerPid=-1;
  //private String buildNumber="17/05/2017 00:52";
  	public PostgresqlPersistenceManager aDBManager=null;
  	public boolean storePulseData=true;
  	public boolean storeOrganismPulseData=true;
  	
	/*
	 * end f variables configure via gui
	 */
	DecimalFormat decimalFormat = new DecimalFormat("###.##");
	String exoZeroNetworkIpAddress="";
	String endoZeroNetworkIpAddress="";
	
	public MnemosyneManager aMnemosyneManager=null;
	Socket exoZeroPublisher=null;
	boolean exoZeroNetworkActive=false;
	Socket endoZeroPublisher=null;
	boolean endoZeroNetworkActive=false;
	int currentPulseInMilliSeconds=0;
	int basePulseInMilliSeconds=60;
	boolean actuatorStatus=true;
	private JSONObject currentlyProcessingSensorDeneWordValueJSONObject=null;

	public Socket subscriber;

	public Context exoZeroContext=null;
	public Context endoZeroContext=null;
	public DiscoverTeleonoms aDiscoverTeleonoms=null;
	public Hashtable subscriberList = new Hashtable();
	public Hashtable subscriberListByName = new Hashtable();
	public Hashtable subscriberListByAddress = new Hashtable();
	protected String processName;
	private boolean waitingfromArduinoResponse=false;

	//CommandServerConduit commandServerConduit=null;
	//CommandServerCommunicationThread aCommandServerCommunicationThread = null;

	MappedBusThread aMappedBusThread =null;
	boolean mutationIsInEffect=false;
//  Initialize poll set
    ZMQ.Poller items;


	
    
	
	public Hypothalamus() {
		mqttClientId = TeleonomeConstants.PROCESS_HYPOTHALAMUS;
		String fileName =  Utils.getLocalDirectory() + "lib/Log4J.properties";
		PropertyConfigurator.configure(fileName);
		logger = Logger.getLogger(getClass());
		Calendar cal = Calendar.getInstance();//TimeZone.getTimeZone("GMT+10:00"));

	 processName = ManagementFactory.getRuntimeMXBean().getName();
		try {
			FileUtils.writeStringToFile(new File("PaceMakerProcess.info"), processName);
			pacemakerPid = Integer.parseInt(processName.split("@")[0]);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e1));
		}


		
		try {
			exoZeroContext = ZMQ.context(1);
			startExoZeroPublisher();
			endoZeroContext = ZMQ.context(1);
			startEndoZeroPublisher();
			
           connectToHeart();
            
			aDBManager = PostgresqlPersistenceManager.instance();
			aDenomeManager = DenomeManager.instance();
			aMnemosyneManager = MnemosyneManager.instance(aDenomeManager, anMqttClient);
			aDenomeManager.setMnemosyneManager(aMnemosyneManager);
			try {
				networkAdapterInfoJSONObject = NetworkUtilities.getAvailableAdapters();
				//
				//
				if(networkAdapterInfoJSONObject.has(TeleonomeConstants.WLAN0) && !networkAdapterInfoJSONObject.getString(TeleonomeConstants.WLAN0).equals("")) {
					primaryIpAddress=networkAdapterInfoJSONObject.getString(TeleonomeConstants.WLAN0);
				}else if(networkAdapterInfoJSONObject.has(TeleonomeConstants.ETH0) && !networkAdapterInfoJSONObject.getString(TeleonomeConstants.ETH0).equals("")) {
					primaryIpAddress=networkAdapterInfoJSONObject.getString(TeleonomeConstants.ETH0);
				}else if(networkAdapterInfoJSONObject.has(TeleonomeConstants.WLAN1) && !networkAdapterInfoJSONObject.getString(TeleonomeConstants.WLAN1).equals("")) {
					primaryIpAddress=networkAdapterInfoJSONObject.getString(TeleonomeConstants.WLAN1);
				}
				
				
				hostName = InetAddress.getLocalHost().getCanonicalHostName();
				initOperationalMode = FileUtils.readFileToString(new File("InitOperationalMode"));
				aDenomeManager.setNetworkInfo(networkAdapterInfoJSONObject, hostName, initOperationalMode);
				aDenomeManager.setProcessInfo(pacemakerPid);
				/*
				Calendar officialSunset, officialSunrise;
				location = new Location(latitude, longitude);
				SunriseSunsetCalculator sunriseSunsetCalculator = new SunriseSunsetCalculator(location, timezone);
				Timezone currentTimeZone = TimeZone.getTimeZone(timezone);
				currentCalendar = Calendar.getInstance(currentTimeZone);
				
				officialSunset = sunriseSunsetCalculator.getOfficialSunsetCalendarForDate(currentCalendar);
				officialSunrise = sunriseSunsetCalculator.getOfficialSunriseCalendarForDate(currentCalendar);
				long dayLengthInMilliseconds = officialSunset.getTime() - officialSunrise.getTime();
				
				aSolarSystemRecord.setSunriseTime(timeFormat.format(officialSunrise.getTime()));
				aSolarSystemRecord.setSunsetTime(timeFormat.format(officialSunset.getTime()));
				*/
				
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

			Integer I = (Integer)aDenomeManager.getDeneWordValueByName(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_DESCRIPTIVE, TeleonomeConstants.DENE_VITAL, "Base Pulse Frequency");
			if(I!=null)basePulseInMilliSeconds = I.intValue();

			//
			// only start the observer thread if
			// we are in organu=ism mode,
			// if we are in host mode, dont start it
			InetAddress exoZeroInetAddress=null;
			try {
				exoZeroInetAddress = NetworkUtilities.getExoZeroNetworkAddress();
			} catch (SocketException | UnknownHostException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			//
			// if the teleonome has only one network card and it is set 
			// to host, then exoZeroInetAddress will be null
			if(exoZeroInetAddress!=null) {
				logger.debug("Starting DiscoverTeleonomes");
				aDiscoverTeleonoms = new DiscoverTeleonoms();
				aDiscoverTeleonoms.start();
			}else {
				logger.debug("NOT Starting DiscoverTeleonomes");
			}
			


			/*
			do{
				//logger.debug("waiting for teleonom discovery");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}while(aDiscoverTeleonoms.getPresentTeleonoms().size()==0);
			 */
			
			//
			// now initialize every microcontroller
			//
			JSONObject componentDeneChain=null;
			JSONArray microcontrollersJSONArray=null;
			try {
				componentDeneChain = aDenomeManager.getDeneChainByName(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_COMPONENTS);
				microcontrollersJSONArray = aDenomeManager.getDenesByDeneType(componentDeneChain, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}


			JSONObject microcontrollerJSONObject;
			String microcontrollerProcessingClassName = "";
			Class aProcessingClass;
			MicroController aMicroController;
			String aMicroControllerName;
			String aMicroControllerPointer;
			String denomeName = aDenomeManager.getDenomeName();
			microControllerPointerMicroControllerIndex = new Hashtable();
			JSONArray microControllerParams;
			Object IsMother = null;
			logger.info("initializing the " + microcontrollersJSONArray.length() + " microcontrollers");
			
			for(int i=0;i<microcontrollersJSONArray.length();i++){
				try {
					microcontrollerJSONObject = microcontrollersJSONArray.getJSONObject(i);
					aMicroControllerName = microcontrollerJSONObject.getString("Name");
					microControllerParams = aDenomeManager.getMicroControllerParamsForMicroController(aMicroControllerName);
					
					logger.debug("microControllerParams=" + microControllerParams.toString(4));
					microcontrollerProcessingClassName = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(microcontrollerJSONObject, TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("reading microcontroller classname=" + microcontrollerProcessingClassName);
					aProcessingClass = Class.forName(microcontrollerProcessingClassName);
					aMicroController = (MicroController)aProcessingClass.getDeclaredConstructor(new Class[]{DenomeManager.class, String.class}).newInstance(aDenomeManager, aMicroControllerName);
					aMicroController.init(microControllerParams);
					logger.debug("invoked init in  microcontroller " + microcontrollerProcessingClassName);
					
					aMicroControllerPointer = "@" + denomeName +":"+ TeleonomeConstants.NUCLEI_INTERNAL + ":" + TeleonomeConstants.DENECHAIN_COMPONENTS+ ":" + aMicroControllerName;
					microControllerPointerMicroControllerIndex.put(aMicroControllerPointer, aMicroController);
					
					IsMother = aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(microcontrollerJSONObject, TeleonomeConstants.DENEWORD_MOTHER_MICROCONTROLER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(IsMother!=null && IsMother instanceof Boolean && ((Boolean)IsMother).booleanValue()) {
						motherMicroController = (MotherMicroController) aMicroController;
						aMotherMicroControllerPointer=aMicroControllerPointer;
						logger.debug("Found Mother=" + aMicroControllerName );
					}

				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (NoSuchMethodException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (SecurityException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				} catch (MicrocontrollerCommunicationException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
			}


			Calendar calendar = Calendar.getInstance();
			long millisToNextHour = Utils.millisToNextHour(calendar);
			logger.info("about to start the timebsased executor, with a delay of " + Utils.getElapsedTimeHoursMinutesSecondsString(millisToNextHour) );
			new HypothalamusScheduledThreadPoolExecutor(1).scheduleAtFixedRate(new TimeBasedMutationsTask(),millisToNextHour , 60*60*1000, TimeUnit.MILLISECONDS);
			
			//new HypothalamusScheduledThreadPoolExecutor(1).scheduleAtFixedRate(new TimeBasedMutationsTask(),1 , 2, TimeUnit.MINUTES);
			
			
			//
			// at the begining of the thread make all the pending commands as skipped
			// so that there are no commands to execute before the first pulse
			//logger.debug("about to skipp existing commands");
			aDenomeManager.markAllNonExecutedAsSkipped();
		} catch (MissingDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

			logger.debug("Fatal Exception, message:"+ e.getMessage());
			System.exit(-1);
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			logger.debug("Fatal Exception, message:"+ e.getMessage());
			System.exit(-1);
		}
	}
	
	/**
	 * this method is called one the human updates a microcontroller config param
	 *  so that they get refreshed in the reader and writer
	 * @param microControllerName
	 */
	public void reInitializeMicroControllerByName(String microControllerName) {
		//
		// now initialize every microcontroller
		//
		JSONObject componentDeneChain=null;
		JSONArray microcontrollersJSONArray=null;
		try {
			componentDeneChain = aDenomeManager.getDeneChainByName(TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_COMPONENTS);
			microcontrollersJSONArray = aDenomeManager.getDenesByDeneType(componentDeneChain, TeleonomeConstants.DENE_TYPE_MICROCONTROLLER);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}


		JSONObject microcontrollerJSONObject;
		String microcontrollerProcessingClassName = "";
		Class aProcessingClass;
		MicroController aMicroController;
		String aMicroControllerName;
		String aMicroControllerPointer;
		String denomeName = aDenomeManager.getDenomeName();
		microControllerPointerMicroControllerIndex = new Hashtable();
		JSONArray microControllerParams;
		Object IsMother = null;
		logger.info("Updating the  microcontroller " + microControllerName);
		
		for(int i=0;i<microcontrollersJSONArray.length();i++){
			try {
				microcontrollerJSONObject = microcontrollersJSONArray.getJSONObject(i);
				aMicroControllerName = microcontrollerJSONObject.getString("Name");
				
				if(!microControllerName.equals(aMicroControllerName))continue;
				
				microControllerParams = aDenomeManager.getMicroControllerParamsForMicroController(aMicroControllerName);
				
				logger.debug("microControllerParams=" + microControllerParams.toString(4));
				microcontrollerProcessingClassName = (String) aDenomeManager.getDeneWordAttributeByDeneWordTypeFromDene(microcontrollerJSONObject, TeleonomeConstants.DENEWORD_TYPE_MICROCONTROLLER_PROCESSING_CLASSNAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.debug("reading microcontroller classname=" + microcontrollerProcessingClassName);
				aProcessingClass = Class.forName(microcontrollerProcessingClassName);
				aMicroController = (MicroController)aProcessingClass.getDeclaredConstructor(new Class[]{DenomeManager.class, String.class}).newInstance(aDenomeManager, aMicroControllerName);
				aMicroController.init(microControllerParams);
				logger.debug("invoked init in  microcontroller " + microcontrollerProcessingClassName);
				
				aMicroControllerPointer = "@" + denomeName +":"+ TeleonomeConstants.NUCLEI_INTERNAL + ":" + TeleonomeConstants.DENECHAIN_COMPONENTS+ ":" + aMicroControllerName;
				microControllerPointerMicroControllerIndex.put(aMicroControllerPointer, aMicroController);
				
//				For now dont allow to reset the mother
//				
//				IsMother = aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(microcontrollerJSONObject, TeleonomeConstants.DENEWORD_MOTHER_MICROCONTROLER, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
//				if(IsMother!=null && IsMother instanceof Boolean && ((Boolean)IsMother).booleanValue()) {
//					motherMicroController = aMicroController;
//					logger.debug("Found Mother=" + aMicroControllerName );
//				}

			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (MicrocontrollerCommunicationException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}
	}
	protected void stopExoZeroPublisher() {
		boolean unbindedOk = exoZeroPublisher.unbind("tcp://" + exoZeroNetworkIpAddress + ":5563");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
		logger.info("unbinding exozero publisher returns " + unbindedOk);
	}
	
	protected void stopEndoZeroPublisher() {
		boolean unbindedOk = endoZeroPublisher.unbind("tcp://" + endoZeroNetworkIpAddress + ":5563");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
		logger.info("unbinding endozero publisher returns " + unbindedOk);
	}
	
	public boolean isExoZeroNetworkActive() {
		return exoZeroNetworkActive;
	}
	
	public boolean isEndoZeroNetworkActive() {
		return endoZeroNetworkActive;
	}
	
	
	
	protected void startExoZeroPublisher() {
		// TODO Auto-generated method stub
		try {
			InetAddress exoZeroInetAddress = NetworkUtilities.getExoZeroNetworkAddress();
			//
			// if the teleonome has only one network card and it is set 
			// to host, then exoZeroInetAddress will be null
			if(exoZeroInetAddress!=null) {
				exoZeroPublisher = exoZeroContext.socket(ZMQ.PUB);
				exoZeroPublisher.setHWM(1);
				exoZeroNetworkIpAddress = exoZeroInetAddress.getHostAddress();
				logger.info("binding zeromq to " + exoZeroNetworkIpAddress);
				exoZeroPublisher.bind("tcp://" + exoZeroNetworkIpAddress + ":5563");
				exoZeroNetworkActive=true;
			}
		} catch (SocketException | UnknownHostException e2) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e2));	
		}
	}

	protected void startEndoZeroPublisher() {
		// TODO Auto-generated method stub
		try {
			InetAddress endoZeroInetAddress = NetworkUtilities.getEndoZeroNetworkAddress();
			//
			// if the teleonome has only one network card and it is set 
			// to network, then endooZeroInetAddress will be null
			if(endoZeroInetAddress!=null) {
				endoZeroPublisher = endoZeroContext.socket(ZMQ.PUB);
				endoZeroPublisher.setHWM(1);
				endoZeroNetworkIpAddress = endoZeroInetAddress.getHostAddress();
				logger.info("binding zeromq to " + endoZeroNetworkIpAddress);
				endoZeroPublisher.bind("tcp://" + endoZeroNetworkIpAddress + ":5563");
				endoZeroNetworkActive=true;
			}
		} catch (SocketException | UnknownHostException e2) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e2));	
		}
	}
	
	public DenomeManager getDenomeManager() {
		return aDenomeManager;
	}
	
	public InetAddress getIpAddress() throws SocketException, UnknownHostException{
		NetworkInterface networkInterface;
		InetAddress inetAddr, potential=null;
		//
		// pay attention to the fact that if a teleonome has 2 network cards, 
		// one as a host and one as part of a network, the inetAddress needs to belong
		// to the network interface connected to the organism network, otherwise the exozero network
		// will not receive the pulse
		for(Enumeration <NetworkInterface> enu = NetworkInterface.getNetworkInterfaces();enu.hasMoreElements();){
			networkInterface  = enu.nextElement();
			for(Enumeration ifaces = networkInterface.getInetAddresses();ifaces.hasMoreElements();){
				inetAddr = (InetAddress)ifaces.nextElement();
				if(!inetAddr.isLoopbackAddress() && !inetAddr.getHostAddress().equals("172.16.1.1")){
					if(inetAddr.isSiteLocalAddress()){
						return inetAddr;
					}else{
						potential=inetAddr;
					}
				}

			}
		}
		return potential;
	}

		private void connectToHeart() {
			 try {
				 
				
				 
	        	anMqttClient = new MqttClient(mqttBrokerAddress, mqttClientId, persistence);
	        	
	            MqttConnectOptions connOpts = new MqttConnectOptions();
	           // connOpts.setKeepAliveInterval(120);
	            connOpts.setAutomaticReconnect(true);
	            connOpts.setCleanSession(true);
	            connOpts.setKeepAliveInterval(300);
	           // connOpts.setMaxInflight(1000);
	            logger.warn("Connecting to Heart: "+mqttBrokerAddress);
					anMqttClient.connect(connOpts);
					//mqttToken.waitForCompletion(10000);
					logger.warn("Connected to Heart: "+mqttBrokerAddress);
					 try {
						 MqttMessage message = new MqttMessage("Hello".getBytes());
						 logger.warn("pibt1");
							
						 message.setQos(TeleonomeConstants.HEART_QUALITY_OF_SERVICE);
						 logger.warn("pibt2");
							
							anMqttClient.publish("Hello", message);
							 logger.warn("pibt3");
								
						} catch (MqttException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					logger.warn("Heart connection ready ");
			             
				} catch (MqttException e1) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e1));
				}
	        
		}
	public synchronized void publishToHeart(String topic, String messageText) {
		
		byte[] messageBytes = messageText.getBytes();
		
		int numberOfReconnectAttempt=3;
		int currentAttempt=0;
		MqttMessage message = new MqttMessage(messageBytes);
		 message.setQos(TeleonomeConstants.HEART_QUALITY_OF_SERVICE);
	    message.setRetained(true);
	    
	    try {
			logger.debug("about to Update the Heart, topic: " + topic  + " HEART_QUALITY_OF_SERVICE=" + TeleonomeConstants.HEART_QUALITY_OF_SERVICE + " message size=" +messageBytes.length + " anMqttClient.isConnected()=" + anMqttClient.isConnected());

			do {
				if(!anMqttClient.isConnected()) {
					
					logger.warn("Reconnecting to heart, currentAttempt=" + currentAttempt);
					anMqttClient.reconnect();
					try {
						currentAttempt++;
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}	
			}while(!anMqttClient.isConnected() && (currentAttempt<numberOfReconnectAttempt) );
			
			
			
			if(anMqttClient.isConnected()) {
				logger.debug("heart is connected about to publish to topic " + topic);
				anMqttClient.publish(topic, message);
			}else {
				logger.warn("Unable to publish to the heart");
			}
		//	logger.debug("published  to " + topic + "  in mqtt");

		} catch (MqttException e) {
			// TODO Auto-generated catch block
			logger.warn("Exception publishing to heart:" + Utils.getStringException(e));
			
		}
	}
	

	public class TimeBasedMutationsTask implements Runnable {

		public TimeBasedMutationsTask() {
			
		}
	    @Override
	    public void run() {
	            
		    	performTimePrunningAnalysis=true;
		    	
		    	try {
					if(aDenomeManager.getCurrentlyCreatingPulseJSONObject() != null) {
						logger.info("about to execute TimeBasedMutations");
						executeTimeBasedMutations();
					}else {
						logger.info("Did not execute TimeBasedMutations because getCurrentlyCreatingPulseJSONObject is null ");
					}
				} catch (InvalidMutation | InvalidDenomeException e) {
					// TODO Auto-generated catch block
					logger.warn(Utils.getStringException(e));
				}
	    }


	}
	
	

	/**
	 * this method is called once per hour
	 */
	public void executeTimeBasedMutations() throws InvalidMutation, InvalidDenomeException {
		Calendar calendar = Calendar.getInstance();
		int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
		int currentDayInWeek = calendar.get(Calendar.DAY_OF_WEEK);
		int currentDayInMonth = calendar.get(Calendar.DAY_OF_MONTH);
		int currentMonthInYear = calendar.getMaximum(Calendar.MONTH);
		
		JSONArray timeBasedMutationsJSONArray = aDenomeManager.getTimeBasedMutations();
		JSONObject timeBasedMutationJSONObject;
		String mutationExecutionTime;
		JSONObject deneChainJSONObject;
		JSONObject mutationTimeConfigurationDene=null;
		JSONArray mutationTimeConfigurationDenes=null;
		int mutationHourInTheDay=0;
		int mutationDayInWeek=0;
		int mutationDayInMonth=0;
		int mutationMonthInYear=0;
		logger.info("entering execute TimeBasedMutations");
		for(int i=0;i<timeBasedMutationsJSONArray.length();i++) {
			timeBasedMutationJSONObject = timeBasedMutationsJSONArray.getJSONObject(i);
			JSONArray mutationDeneChains = timeBasedMutationJSONObject.getJSONArray("DeneChains");
			logger.info(timeBasedMutationJSONObject.getString("Name")  + " has mutationDeneChains.length()=" + mutationDeneChains.length());
			mutationTimeConfigurationDene=null;
			//found:
			for(int j=0;j<mutationDeneChains.length();j++) {
				deneChainJSONObject = mutationDeneChains.getJSONObject(j);
				logger.info("looking at " + deneChainJSONObject.getString("Name") );
				if(deneChainJSONObject.getString("Name").equals("Mutation Configuration")){
					mutationTimeConfigurationDenes = deneChainJSONObject.getJSONArray("Denes");	
					for(int k=0;k<mutationTimeConfigurationDenes.length();k++) {
						logger.info("looking at dene " + mutationTimeConfigurationDenes.getJSONObject(k).getString("Name"));
						if(mutationTimeConfigurationDenes.getJSONObject(k).getString("Name").equals("Time Mutation Configuration")) {
							mutationTimeConfigurationDene = mutationTimeConfigurationDenes.getJSONObject(k);
							logger.info("found  dene " + mutationTimeConfigurationDene.toString(4));
							//break found;
						}
				}
			}
		
				
			logger.info("mutationTimeConfigurationDene " +(mutationTimeConfigurationDene!=null));
				
			if(mutationTimeConfigurationDene!=null) {
				mutationExecutionTime = (String)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_TYPE_TIME_BASED_MUTATION_EXECUTION_TIME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				logger.info("mutationExecutionTime= " +mutationExecutionTime);
				if(mutationExecutionTime==null)mutationExecutionTime="";
				if(mutationExecutionTime.equals(TeleonomeConstants.MNEMOSYNE_HOURLY_MUTATION)) {
					try {
						executeMutation(timeBasedMutationJSONObject);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
				}

				if(mutationExecutionTime.equals(TeleonomeConstants.MNEMOSYNE_DAILY_MUTATION) ) {
					mutationHourInTheDay = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_HOUR_IN_DAY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.info("mutationHourInTheDay= " +mutationHourInTheDay + " currentHour=" + currentHour);
					
					if(currentHour==mutationHourInTheDay) {
						try {
							executeMutation(timeBasedMutationJSONObject);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						} 
					}
				}
				
				if(mutationExecutionTime.equals(TeleonomeConstants.MNEMOSYNE_WEEKLY_MUTATION) ) {
					mutationHourInTheDay = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_HOUR_IN_DAY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					mutationDayInWeek = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_DAY_IN_WEEK, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(mutationDayInWeek == currentDayInWeek && currentHour==mutationHourInTheDay) {
						try {
							executeMutation(timeBasedMutationJSONObject);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						} 
					}
				}
				
				if(mutationExecutionTime.equals(TeleonomeConstants.MNEMOSYNE_MONTHLY_MUTATION) ) {
					mutationHourInTheDay = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_HOUR_IN_DAY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					mutationDayInMonth = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_DAY_IN_MONTH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(mutationDayInMonth == currentDayInMonth && currentHour==mutationHourInTheDay) {
						try {
							executeMutation(timeBasedMutationJSONObject);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						} 
					}
				}
				
				if(mutationExecutionTime.equals(TeleonomeConstants.MNEMOSYNE_YEARLY_MUTATION) ) {
					mutationHourInTheDay = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_HOUR_IN_DAY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					mutationDayInMonth = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_DAY_IN_MONTH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					mutationMonthInYear = (int)aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(mutationTimeConfigurationDene, TeleonomeConstants.DENEWORD_MONTH_IN_YEAR, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if(mutationMonthInYear== (1+currentMonthInYear) && mutationDayInMonth == currentDayInMonth && currentHour==mutationHourInTheDay) {
						try {
							executeMutation(timeBasedMutationJSONObject);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						} 
					}
				}
				
			}
		}	
	}
}
	
	public void executeMutation(JSONObject mutationJSONObject) throws InvalidMutation, InvalidDenomeException, IOException {
		JSONObject actuatorLogicProcessingDene=null;
		ArrayList arrayList;
		String actuatorCommand="";
		JSONArray actuatorLogicProcessingDeneJSONArray = new JSONArray();
		String microControllerPointer;
		MicroController aMicroController;
		
		ArrayList<Map.Entry<String, MutationActionsExecutionResult>>  microControllerPointerMutationActionsExecutionResultArrayList;
		logger.info("executing mutation:" + mutationJSONObject.getString("Name"));
		
		microControllerPointerMutationActionsExecutionResultArrayList = aDenomeManager.loadImmediateMutation(mutationJSONObject);
		logger.debug("microControllerPointerMutationActionsExecutionResultArrayList:" + microControllerPointerMutationActionsExecutionResultArrayList);
		MutationActionsExecutionResult aMutationActionsExecutionResult;
		String pointerToActionSuccessTasks, pointerToMnemosyneTasks;
		boolean actuatorCommandIsOperation=false;
		String deneWordOperationPointer="";
		BufferedWriter output=null;
		BufferedReader input=null;
		for (Map.Entry<String, MutationActionsExecutionResult> entry3 : microControllerPointerMutationActionsExecutionResultArrayList) {
			microControllerPointer = entry3.getKey();
			aMutationActionsExecutionResult = (MutationActionsExecutionResult)entry3.getValue();
			arrayList = aMutationActionsExecutionResult.getCommandToExecute();
			pointerToActionSuccessTasks = aMutationActionsExecutionResult.getPointerToActionSuccessTasks();
			pointerToMnemosyneTasks = aMutationActionsExecutionResult.getPointerToMnemosyneTasks();
			logger.info("arrayList:" + arrayList);
			logger.info("line 2633 pointerToActionSuccessTasks:" + pointerToActionSuccessTasks);
			logger.info("line 2634 pointerToMnemosyneTasks:" + pointerToMnemosyneTasks);

			Map.Entry<String,JSONObject> entry = (Entry<String,JSONObject>) arrayList.get(0);
			actuatorCommand = entry.getKey();
			logger.info("line 2638 actuatorCommand:" + actuatorCommand);
			if(actuatorCommand!=null){
				actuatorLogicProcessingDene = entry.getValue();
				if(!actuatorCommand.equals(TeleonomeConstants.COMMANDS_DO_NOTHING)){
					actuatorCommandIsOperation=false;
					//
					// check if the command needs to be rendered
					logger.debug("before rendering actuatorCommand=" + actuatorCommand);
					if(actuatorCommand.startsWith("@")){
						//
						// if the actuatorCommand is a pointer,it means this action is an evaluation
						// 
						deneWordOperationPointer = actuatorCommand;
						actuatorCommandIsOperation=true;
					}else if(actuatorCommand.startsWith("$")){
						if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_GENERATE_DIGITAL_GEPPETTO_CODE)){
							String digitalGeppettoCode = this.motherMicroController.getDigitalGeppettoCommandCode();
							
						}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_IP_ADDRESS_FOR_LCD)){
							actuatorCommand="IPAddr#" + primaryIpAddress;
						}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_DO_NOTHING)){
							actuatorCommand=TeleonomeConstants.COMMANDS_DO_NOTHING;
						}else if(actuatorCommand.equals(TeleonomeConstants.COMMANDS_SET_MICROCONTROLLER_RTC)){
							Calendar cal = Calendar.getInstance();
							actuatorCommand="SetClock#" + cal.get(Calendar.YEAR) + "#" + (cal.get(Calendar.MONTH)+1) + "#" + cal.get(Calendar.DATE) + "#" + cal.get(Calendar.HOUR_OF_DAY) + "#" + cal.get(Calendar.MINUTE) + "#"  + cal.get(Calendar.SECOND);
						}else if(actuatorCommand.equals(TeleonomeConstants.COMMAND_SHUTDOWN)){
							//
							logger.info("the mutation contains the shutdown command");
							//
							// Because this method will not return, first save the denometodisk
							aDenomeManager.writeDenomeToDisk();
							//
							// now update the Denome so that is ready to start next time it wakes up 
							// this involves check to see if the action that executes the shutdown command
							// that should have an identity of @NewEgg:Internal:Actuators:Shutdown:Active
							// should be set to false, so is not active on next startup
							//
							
							Identity shutdownActionIdentity = new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_INTERNAL,TeleonomeConstants.DENECHAIN_ACTUATORS,TeleonomeConstants.SHUTDOWN_ACTION,TeleonomeConstants.DENEWORD_ACTIVE);
							boolean updateOk=false;
							try {
								updateOk = aDenomeManager.readAndModifyDeneWordByIdentity(shutdownActionIdentity, false);
								logger.info ("read and modify denome to set shutdown active to false, now shutttiigdown, updateOk=" + updateOk);
							} catch (JSONException | InvalidDenomeException e) {
								// TODO Auto-generated catch block
								logger.warn(Utils.getStringException(e));
							}
							
							Identity mnemosyneWPSCounterIdentity = new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_MNEMOSYNE,TeleonomeConstants.MNEMOSYNE_DENECHAIN_PULSE_COUNT,TeleonomeConstants.MNEMOSYNE_DENE_WPS_CYCLE_PULSE_COUNT,TeleonomeConstants.DENEWORD_MNEMOSYNE_COUNTER);
							 updateOk=false;
							try {
								updateOk = aDenomeManager.readAndModifyDeneWordByIdentity(mnemosyneWPSCounterIdentity, 0);
								logger.info ("read and modify denome to set the counter at , to zero with updateok=" + updateOk);
							} catch (JSONException | InvalidDenomeException e) {
								// TODO Auto-generated catch block
								logger.warn(Utils.getStringException(e));
							}
							
							
							//
							// update the heart
							publishToHeart(TeleonomeConstants.HEART_TOPIC_PULSE_STATUS_INFO, "Shutting down... " );
							
							//
							// put a sleep to let ther things finish
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							Runtime.getRuntime().exec("sudo shutdown -h now");
							//
							// send this thread to sleep so the shutdown can proceed
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}else{
							actuatorCommand = Utils.renderCommand(actuatorCommand);
						}
					}
					logger.info ("after rendering actuatorCommand=" + actuatorCommand);
					if(!actuatorCommand.equals("")){
						//
						// there is a command called do nothing which is used 
						// when all we want is to invoke the success tasks
						// because they change data, 
						// this command means dont send anything to the microcontroller
						//
						
						logger.info("about to send " + actuatorCommand + " actuatorCommandIsOperation=" + actuatorCommandIsOperation); 
						if(actuatorCommandIsOperation){
							aDenomeManager.evaluateDeneWordOperation(deneWordOperationPointer);
							
							
						}else if(!actuatorCommand.equals(TeleonomeConstants.COMMANDS_DO_NOTHING) ){
							BufferedWriter output1 = null;
							BufferedReader input1 = null;
							
							try {
								aMicroController = (MicroController)microControllerPointerMicroControllerIndex.get(microControllerPointer);
								 output = aMicroController.getWriter();//new OutputStreamWriter(serialPort.getOutputStream());
								 input = aMicroController.getReader();
								
								//InputStream input = aMicroController.getReader();
								logger.info("in executing command loading mutation, about to send " + actuatorCommand +" to " + output + " microControllerPointer=" + microControllerPointer + " aMicroController=" + aMicroController); 
								output.write(actuatorCommand,0,actuatorCommand.length());
								//output.flush();
								
								logger.info("waiting for response " ); 
								
								String inputLine = input.readLine();
								logger.info("actuator cmmand response=" + inputLine);
							}catch(IOException e) {
								logger.warn(Utils.getStringException(e));
								
							}finally {
								try {
									if (input!=null)input.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									logger.warn(Utils.getStringException(e));
								}
								try {
									if (output!=null)output.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									logger.warn(Utils.getStringException(e));
								}
								
								
							}
						}
						
						
					}	
				
				} // if(!actuatorCommand.equals(TeleonomeConstants.COMMANDS_DO_NOTHING)){
				
				//
				// now that the actuator command has been sent
				// apply the Action Success Task
				logger.info("line 2719 pointerToActionSuccessTasks=" + pointerToActionSuccessTasks);
				if(pointerToActionSuccessTasks!=null){
					aDenomeManager.executeActionSuccessTasks(pointerToActionSuccessTasks);
				}
				
				if(pointerToMnemosyneTasks!=null) {
					String mnemosyneOperationPointer;
					JSONObject mnemosyneOperationDene;
					try {
						JSONObject mnemosyneOperationsDene = aDenomeManager.getDeneByIdentity(new Identity(pointerToMnemosyneTasks));
						logger.info("line 2728 pointerToMnemosyneTasks=" + pointerToMnemosyneTasks + " mnemosyneOperationsDene=" + mnemosyneOperationsDene);
						if(mnemosyneOperationsDene!=null ) {
							JSONArray mnemosyneOperationPointers = DenomeUtils.getAllDeneWordsFromDeneByDeneWordType(mnemosyneOperationsDene, TeleonomeConstants.DENEWORD_TYPE_MNEMOSYNE_OPERATION, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
							logger.info("line 2731 mnemosyneOperationPointers=" + mnemosyneOperationPointers);

							JSONArray mnemosyneDenes = new JSONArray();
							for(int i=0;i<mnemosyneOperationPointers.length();i++) {
								mnemosyneOperationPointer = mnemosyneOperationPointers.getString(i);
								mnemosyneOperationDene = aDenomeManager.getDeneByIdentity(new Identity(mnemosyneOperationPointer));
								mnemosyneDenes.put(mnemosyneOperationDene);
							}
							if(mnemosyneDenes.length()>0) {
								aDenomeManager.executeMnemosyneOperations( mnemosyneDenes);
							}
						}

					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					} catch (InvalidMutation e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
				}
				
				//String inputLine = getInputLine( input);
				actuatorLogicProcessingDeneJSONArray.put(actuatorLogicProcessingDene);
				
			}
			
			
			

		}
	}
}
