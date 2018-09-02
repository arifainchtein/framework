package com.teleonome.framework.servlet;


import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.commandserver.CommandServerConduit;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.exception.ServletProcessingException;
import com.teleonome.framework.mnemosyne.MnemosyneManager;
import com.teleonome.framework.network.NetworkUtilities;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.process.DiscoverTeleonoms;
import com.teleonome.framework.utils.Utils;


import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.sql.*;
import java.awt.Color;



public class TeleonomeServlet extends HttpServlet  {
	SimpleDateFormat aSimpleDateFormat = new SimpleDateFormat("HH:mm dd/MM/yy");
	String fileSep = System.getProperty("file.separator");
	String denomeFileName="";
	String currentIdentityMode="";
	private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
	private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
	private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB



	MnemosyneManager aMnemosyneManager;
	String busId="EggServlet";

	Hashtable subscriberList = new Hashtable();

	Logger logger;
	/**
	 * the number of milliseconds for timeout
	 */
	long systemTimeOut=0;;
	protected String FILE_SEPARATOR = System.getProperty("file.separator");


	ServletContext servletContext;

	com.teleonome.framework.denome.DenomeViewManager aDenomeViewerManager = new com.teleonome.framework.denome.DenomeViewManager();
	boolean sentFaultCommand=true;
	private PostgresqlPersistenceManager aDBManager=null;

	/**
	 * this method will initialize the application and all the necessary managers
	 */  
	public void init(){
		logger = Logger.getLogger(getClass());


		aDBManager = PostgresqlPersistenceManager.instance();
		//try {
		//	aMnemosyneManager = MnemosyneManager.instance();
		//} catch (MissingDenomeException e1) {
		// TODO Auto-generated catch block
		//		e1.printStackTrace();
		//	}

		//
		// now identify which is the menome we need to read
		//


		File directory = new File(Utils.getLocalDirectory());

		File[] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".denome");
			}
		});



		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
		denomeFileName = directory.getAbsolutePath() + "/" +  files[0].getName();//.getAbsolutePath();
		System.out.println("in Teleonomeservlet init, " + denomeFileName);
	}


	public void  doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{		
		String errorMessage="";
		HttpSession session = req.getSession(true);
		String formName = req.getParameter("formName");
		boolean waitForResponse=true;
		String className;
		String command=null;
		System.out.println("Egg doGet, formName=" + formName);
		if(formName.equals("loadInit")){
			//
			//Check to see if there is a file 	FileUtils.deleteQuietly(new File("EndPulse.info"));
			File file = new File(Utils.getLocalDirectory() + "StartPulse.info");
			String lastEvent="";
			if(file.isFile()){
				lastEvent="StartPulse";
				long endPulseTimeInMillis = Long.parseLong(FileUtils.readFileToString(file));
				lastEvent=TeleonomeConstants.STARTING_PULSE_MAPPED_BUS_MESSAGE + ": " + aSimpleDateFormat.format(new Timestamp(endPulseTimeInMillis));



			}else {
				file = new File(Utils.getLocalDirectory() + "EndPulse.info");
				if(file.isFile()){
					long endPulseTimeInMillis = Long.parseLong(FileUtils.readFileToString(file));
					lastEvent=TeleonomeConstants.PULSE_FINISHED_MAPPED_BUS_MESSAGE + ": " + aSimpleDateFormat.format(new Timestamp(endPulseTimeInMillis));
				}
			}
			String dataForBrowser = readOperationalData();

			if(dataForBrowser!=null){
				//try {
				//	dataForBrowser.put("Pulse Status", lastEvent);
				//} catch (JSONException e) {
				//	// TODO Auto-generated catch block
				//		e.printStackTrace();
				//	}

				res.setContentType("application/json;charset=UTF-8");
				PrintWriter out = res.getWriter();
				out.print(dataForBrowser);
				out.flush();
				out.close();
			}
			return;
		}else if(formName.equals("IsPulseActive")){
			//
			// read the pulse file and 
			String dataForBrowser = readOperationalData();
			//System.out.println("inside IsPulseActive, dataForBrowser=" + dataForBrowser);

			if(dataForBrowser!=null){
				res.setContentType("application/json;charset=UTF-8");
				PrintWriter out = res.getWriter();
				out.print(dataForBrowser);
				out.flush();
				out.close();
			}

		}else if(formName!=null && !formName.equals("null") &&  !formName.equals("")){
			className = "com.teleonomeframework.servlet." + formName + "ProcessingHandler";

			ProcessingFormHandler processingFormHandler;
			try {
				processingFormHandler = ProcessingFormHandlerFactory.createProcessingFormHandler(className,req,res, getServletContext());
				processingFormHandler.process();
			} catch (ServletProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return;
	}



	public void  doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException{
		String errorMessage="";
		HttpSession session = req.getSession(true);
		String formName = req.getParameter("formName");
		String action = req.getParameter("action");


		boolean waitForResponse=true;
		System.out.println("TeleonomeServlet  Do Post formaName=" + formName);
		String command=null;
		String payLoad="";
		//
		// the command to start the pulse must be handled separately
		//
		if(formName.equals(TeleonomeConstants.COMMAND_START_PULSE)){	
			Runtime.getRuntime().exec("sudo sh StartPulse.sh");
			//
			// now wait until StartPulse.info
			boolean keepWaiting=true;
			while(keepWaiting){
				try {
					File file = new File(Utils.getLocalDirectory() + "EndPulse.info");
					if(file.isFile()){
						keepWaiting=false;
					}

					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			//
			// if we are here is because we are ready, the pulse has started
			return; 
		}



		if(formName.equals("ReSignal")){
			String enableNetworkMode = req.getParameter("EnableNetworkMode");

			if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
				command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK;

				String ssid = req.getParameter("AvailableNetworks");
				String password = req.getParameter("password");

				JSONObject payLoadParentJSONObject = new JSONObject();

				JSONObject payLoadJSONObject = new JSONObject();
				try {
					payLoadParentJSONObject.put("Mutation Name","SetNetworkMode");

					payLoadParentJSONObject.put("Payload", payLoadJSONObject);
					JSONArray updatesArray = new JSONArray();
					payLoadJSONObject.put("Updates"	, updatesArray);

					JSONObject updateJSONObject =  new JSONObject();
					updateJSONObject.put("Target","@On Load:Update SSID:Update SSID");
					updateJSONObject.put("Value",ssid);
					updatesArray.put(updateJSONObject);

					JSONObject updateJSONObject2 =  new JSONObject();
					updateJSONObject2.put("Target","@On Load:Update PSK:Update PSK");
					updateJSONObject2.put("Value", password );
					updatesArray.put(updateJSONObject2);

					System.out.println("mononanyservlet, setting newtork info, ssid=" + ssid + " ps=" + password);

					updatesArray.put(updateJSONObject);
					payLoad=payLoadParentJSONObject.toString();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


			}



			System.out.println("the action is " + action);
			if(action.equals(TeleonomeConstants.COMMAND_REBOOT)){
				if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
					if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
						command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_NETWORK;
					}else{
						command = action;
					}

				}else{
					String enableHostMode = req.getParameter("EnableHostMode");
					System.out.println("the enableHostMode is " + enableHostMode);
					if(enableHostMode!=null && enableHostMode.equals("Yes")){
						command = TeleonomeConstants.COMMAND_REBOOT_ENABLE_HOST;
					}else{
						command = action;
					}
				}
			}else if(action.equals(TeleonomeConstants.COMMAND_SHUTDOWN)){
				if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
					System.out.println("the enableNetworkMode is " + enableNetworkMode);

					if(enableNetworkMode!=null && enableNetworkMode.equals("Yes")){
						command = TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_NETWORK;
					}else{
						command = action;
					}

				}else{
					String enableHostMode = req.getParameter("EnableHostMode");
					System.out.println("the enableHostMode is " + enableHostMode);
					if(enableHostMode!=null && enableHostMode.equals("Yes")){
						command = TeleonomeConstants.COMMAND_SHUTDOWN_ENABLE_HOST;
					}else{
						command = action;
					}
				}
			}else{
			}


			//
			// delete the startpulse.info and endpulse.info
			File file = new File(Utils.getLocalDirectory() + "StartPulse.info");
			if(file.isFile())file.delete();
			file = new File(Utils.getLocalDirectory() + "EndPulse.info");
			if(file.isFile())file.delete();
			//
			// write the kill file so as to mark it dead
			//
			FileUtils.writeByteArrayToFile(new File("KillPulse.info"), (""+System.currentTimeMillis()).getBytes());



		}else if(formName!=null && !formName.equals("null") &&  !formName.equals("")){
			String className = "com.teleonomeframework.servlet." + formName + "ProcessingHandler";

			ProcessingFormHandler processingFormHandler;
			try {
				processingFormHandler = ProcessingFormHandlerFactory.createProcessingFormHandler(className,req,res, getServletContext());
				processingFormHandler.process();
				command = processingFormHandler.getCommand();
				payLoad = processingFormHandler.getPayload();

			} catch (ServletProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("TeleonomeServlet about to send:" + command);
		sendCommand(command, payLoad);

		String dataForBrowser = readOperationalData();
		if(dataForBrowser!=null){

			if(formName.equals("ReSignal")){
				if(command.contains("Reboot")){
					res.sendRedirect("Rebooting.jsp");
				}else if(command.contains("Shutdown")){
					res.sendRedirect("Shutdown.jsp");
				}
			}else{
				res.setContentType("text/html");

				PrintWriter out = res.getWriter();
				out.print(dataForBrowser);
				out.flush();
				out.close();
				System.out.println("EggServlet sent response,");
			}
		}
	}

	/**
	 * Processes requests for both HTTP
	 * <code>GET</code> and
	 * <code>POST</code> methods.
	 *
	 * @param request servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String errorMessage="";
		HttpSession session = req.getSession(true);
		String formName = req.getParameter("formName");
		boolean waitForResponse=true;

		String command=null;
		System.out.println("DenomeViewer doGet, formName=" + formName);
		final String nextPage = req.getParameter("NextPage");

		String className = "com.denomeviewer.page." + formName + "ProcessingHandler";

		ProcessingFormHandler processingFormHandler;
		try {
			processingFormHandler = ProcessingFormHandlerFactory.createProcessingFormHandler(className,req,res, getServletContext());
			processingFormHandler.process();
		} catch (ServletProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


	private String getFileName(final Part part) {
		final String partHeader = part.getHeader("content-disposition");
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(
						content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}


	public void sendCommand(String command, String payLoad){
		System.out.println("Egg  sending.. =" + command);
		String commandCode="";
		int id = aDBManager.requestCommandToExecute(command,commandCode, payLoad);
		System.out.println("EggServlet id=" + id);
		//
		// now keep waiiting until the command is executed
		//
		boolean waitForCommandToComplete=true;
		int counter=0;
		int numberOfTries=10;
		while(waitForCommandToComplete){
			counter++;
			waitForCommandToComplete= !aDBManager.isCommandCompleted(id);
			System.out.println("Egg wait for command to complete=" + waitForCommandToComplete + " counter=" + counter);

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	protected Hashtable getFileUploadFormParameters(HttpServletRequest request){
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		//System.out.println("isMultipart="+isMultipart);


		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(MEMORY_THRESHOLD);
		ServletFileUpload upload = new ServletFileUpload(factory);
		upload.setFileSizeMax(MAX_FILE_SIZE);
		upload.setSizeMax(MAX_REQUEST_SIZE);
		ServletContext servletContext = this.getServletConfig().getServletContext();
		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
		factory.setRepository(repository);

		Hashtable toReturn = new Hashtable();
		List items=null;
		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			e.printStackTrace();
		}
		Iterator itr = items.iterator();
		while (itr.hasNext()) {
			FileItem item = (FileItem)(itr.next());

			if (item.isFormField()) {
				try{
					String field=item.getFieldName();
					String value=item.getString();
					toReturn.put(field, value);
				}catch(Exception e){

				}
			}else{
				try {
					String fieldName = item.getFieldName();
					String fileName = item.getName();
					if(fileName!=null && !fileName.equals("")){
						String contentType = item.getContentType();
						boolean isInMemory = item.isInMemory();
						long sizeInBytes = item.getSize();

						String tempFileName = repository.getAbsolutePath() + fileSep + fileName;  
						//System.out.println("Saviung uploaded file to " + tempFileName);
						toReturn.put(fieldName, fileName);  
						File savedFile = new File(tempFileName);
						item.write(savedFile); 
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		return toReturn;
	}



	private String readOperationalData() throws IOException{
		try {

			JSONObject denomeJSONObject = new JSONObject(FileUtils.readFileToString(new File(denomeFileName)));
			JSONObject operationalDataDeneChain = DenomeUtils.getDeneChainByName(denomeJSONObject,TeleonomeConstants.NUCLEI_PURPOSE,  TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA);

			JSONObject vitalDene = DenomeUtils.getDeneByName(operationalDataDeneChain, "Vital");
			int currentPulse = ((Integer) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(vitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE))/1000;
			currentIdentityMode = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(vitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_IDENTITY_MODE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);


			// the connected info
			JSONObject dataForBrowser = new JSONObject();
			dataForBrowser.put("Pulse", dataForBrowser);
			//System.out.println("MnoNannyServlet, currentIdentityMode=" + currentIdentityMode);

			if(currentIdentityMode.equals(TeleonomeConstants.TELEONOME_IDENTITY_SELF)){
				LinkedHashMap linkedMap = NetworkUtilities.getConnectedClients();
				Set set = linkedMap.keySet();
				Iterator it = set.iterator();
				String name, ipaddress;
				JSONArray clientsJSONArray = new JSONArray();
				JSONObject client;
				while(it.hasNext()){
					name=(String)it.next();
					ipaddress = (String) linkedMap.get(name);
					//System.out.println("name=" + name + "  ipaddress=" + ipaddress);	
					client = new JSONObject();
					client.put("name", name);
					client.put("ipaddress", ipaddress);

					clientsJSONArray.put(client);
				} 

				dataForBrowser.put("Connected Clients", clientsJSONArray);
			}else{
				JSONObject wifiOperationalDataDene = DenomeUtils.getDeneByName(operationalDataDeneChain, TeleonomeConstants.DENE_WIFI_INFO);
				String ipAddress = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(wifiOperationalDataDene, "Host IP Address", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				String essid = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(wifiOperationalDataDene, "ESSID", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				dataForBrowser.put("ESSID", essid);				
				dataForBrowser.put("IP Address", ipAddress);
			}

			JSONArray ssidJSONArray = NetworkUtilities.getSSID(false);

			dataForBrowser.put("Available SSIDs", ssidJSONArray);

			//
			//
			// now make the object
			//
			String pulseTimestamp = denomeJSONObject.getString("Pulse Timestamp");
			long pulseTimestampInMilliseconds = denomeJSONObject.getLong("Pulse Timestamp in Milliseconds");
			long pulseDuration = denomeJSONObject.getLong("Pulse Creation Duration Millis");





			boolean alive=false;
			//System.out.println("MonoNannyServlet  currentPulse=" + currentPulse + " last pulse was " + (System.currentTimeMillis()-pulseTimestampInMilliseconds)/1000 );
			if(  (System.currentTimeMillis()-pulseTimestampInMilliseconds)/1000 < 2*currentPulse)alive=true;
			//
			// one last, since the above algorithm checks whether the last pulse is at least 2 pulse old
			// so if the user kills the pulse, for two minutes the above algorithm will give the wrong status
			// so check to see if there is a file called "PulseKilled.info"  if it there is, then the pulse is dead
			// and the above algorithm should be ignored, 
			//The PulseGenerator will delete the KillPulse.info when it starts

			File file = new File(Utils.getLocalDirectory() + "KillPulse.info");
			if(file.isFile())alive=false;
			dataForBrowser.put("Alive", alive);

			//
			// to determine if we are in a pulse or not,
			// look for the file StartPulse.info
			File startPulseInfoFile = new File(Utils.getLocalDirectory() + "StartPulse.info");
			String pulseStatusMessage="";
			File endPulseInfoFile = new File(Utils.getLocalDirectory() + "EndPulse.info");
			if(startPulseInfoFile.isFile()){
				long startPulseTimeInMillis = Long.parseLong(FileUtils.readFileToString(startPulseInfoFile));
				pulseStatusMessage=TeleonomeConstants.STARTING_PULSE_MAPPED_BUS_MESSAGE + " at " + aSimpleDateFormat.format(new Timestamp(startPulseTimeInMillis));

			}else if(endPulseInfoFile.isFile()){
				long endPulseTimeInMillis = Long.parseLong(FileUtils.readFileToString(endPulseInfoFile));
				pulseStatusMessage = TeleonomeConstants.PULSE_FINISHED_MAPPED_BUS_MESSAGE + " at "+ aSimpleDateFormat.format(new Timestamp(endPulseTimeInMillis));
			}
			dataForBrowser.put("Pulse Status", pulseStatusMessage);

			return dataForBrowser.toString();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			String s = Utils.getStringException(e);
			System.out.println("s=" + s);

		}
		return null;
	}




	public String getServletInfo() {
		return "the main servlet of the Egg";
	}
}
