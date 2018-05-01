package com.teleonome.framework.servlet;

import java.io.EOFException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;

//import io.mappedbus.MappedBusReader;

@WebServlet( name="AsyncServlet",value = {"/async"},asyncSupported = true)
public class AsyncServlet extends HttpServlet 
{


	//MappedBusReader pulseStatusReader = new MappedBusReader(Utils.getLocalDirectory() + TeleonomeConstants.MAPPED_BUS_FILE_PULSE_STATUS, TeleonomeConstants.MAPPED_BUS_FILE_PULSE_STATUS_SIZE, TeleonomeConstants.MAPPED_BUS_RECORD_SIZE);
	String busId="MonoNannyServlet";
	boolean  generatingPulse=false;
	long startingTime=0;
	ServletContext servletContext;
	private static final long serialVersionUID = 1L;
	SimpleDateFormat aSimpleDateFormat = new SimpleDateFormat("HH:mm dd/MM/yy");
	com.teleonome.framework.denome.DenomeViewManager aDenomeViewerManager = new com.teleonome.framework.denome.DenomeViewManager();
	String denomeFileName="";
	String param ="";
	FileWatcherThread aFileWatcherThread = new FileWatcherThread();
	ScheduledThreadPoolExecutor executer;
	boolean sentFaultCommand=true;

	public void init(){
		startingTime = System.currentTimeMillis();
		//// System.out.println("Async  Starting async servlet at " + new Timestamp(startingTime));
		servletContext = getServletContext();
		//
		// now identify which is the menome we need to read
		//
		File directory = new File(".");

		File[] files = directory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".denome");
			}
		});
		Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
		denomeFileName = directory.getAbsolutePath() + "/" +  files[0].getName();//.getAbsolutePath();
		//// System.out.println("in AsynServlet init, " + denomeFileName);

		executer = new ScheduledThreadPoolExecutor(1);

	}




	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		final  AsyncContext ctx = req.startAsync(req, resp);
		ctx.setTimeout(40000);
		// 3.0 add listener
		ctx.addListener(new AsyncListener() {

			@Override
			public void onTimeout(AsyncEvent arg0) throws IOException {
				//// System.out.println("Async  onTimeout about to refres request"); 

				ctx.getResponse().getWriter().print("RefreshRequest");

			}

			@Override
			public void onStartAsync(AsyncEvent arg0) throws IOException {
				//// System.out.println("Async  onStartAsync...");              
			}

			@Override
			public void onError(AsyncEvent arg0) throws IOException {
				//// System.out.println("Async  onError...");    
				aFileWatcherThread.interrupt();
				aFileWatcherThread=null;
			}

			@Override
			public void onComplete(AsyncEvent arg0) throws IOException {
				//// System.out.println("Async  onComplete terminating aFileWatcherThread...");


			}
		});


		aFileWatcherThread.setContext(ctx);
		executer.execute(aFileWatcherThread);


	}    




	class FileWatcherThread extends Thread{
		//  AsyncContext anAsyncContext;
		AsyncContext asyncContext;
		public FileWatcherThread(){
			// anAsyncContext=c;

		}
		public void setContext(AsyncContext s){
			asyncContext=s;
		}
		public void run(){

			final Path path = FileSystems.getDefault().getPath(Utils.getLocalDirectory());
			//	// System.out.println(path);
			try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
				final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
				boolean keepRunning=true;
				while (keepRunning) {
					final WatchKey wk = watchService.take();
					for (WatchEvent<?> event : wk.pollEvents()) {
						//we only register "ENTRY_MODIFY" so the context is always a Path.
						final Path changed = (Path) event.context();
						File selectedFile = changed.toFile();

						String extension = FilenameUtils.getExtension(selectedFile.getAbsolutePath());
						String fileName = FilenameUtils.getName(selectedFile.getAbsolutePath());
						// System.out.println("Async  changed:" + changed + " extension:" + extension);
						if (fileName.equals("PulseProgress")) {
							//
							// wait 100ms,
							Thread.sleep(50);
							String pulseProgressString = FileUtils.readFileToString(selectedFile);
							//// System.out.println("Async  reading pulse progress:" +pulseProgressString );
							if(!pulseProgressString.equals("")){
								asyncContext.getResponse().setContentType("text/html");

								asyncContext.getResponse().setCharacterEncoding("UTF-8");
								try {
									//asyncContext.getResponse().getWriter().print(dataForBrowser.toString());
									asyncContext.getResponse().getWriter().print(pulseProgressString);
									asyncContext.getResponse().getWriter().flush();
									asyncContext.complete();
									keepRunning=false;
									//asyncContext.getResponse().flushBuffer();
								} catch (IOException ex) {
									// System.out.println("Async  fail");
								}
							}

						}else if (extension.equals("denome")) {
							//// System.out.println("Async  pulse has finished, reading " + fileName);

							JSONObject operationalDataDeneChain=null,sensorDataDeneChain=null;

							long endPulseTimeInMillis = 0;
							File endInfoFile = new File(Utils.getLocalDirectory() + "EndPulse.info");

							if(endInfoFile.isFile())endPulseTimeInMillis = Long.parseLong(FileUtils.readFileToString(endInfoFile));

							//	// System.out.println("Async  reading denome from " +selectedFile.getName());

							String denomeFileInString = FileUtils.readFileToString(selectedFile);
							//// System.out.println("Async  after pulse has finished, denomeFileInString=" );

							// // System.out.println("Async  s: " + s);

							if(denomeFileInString!=null && denomeFileInString.length()>10){
								try {
									String data = FileUtils.readFileToString(new File(denomeFileName));
									JSONObject denomeJSONObject = new JSONObject(data);
									servletContext.setAttribute("CurrentPulseString", data);
									
									
									String pulseTimestamp = denomeJSONObject.getString("Pulse Timestamp");
									long pulseTimestampInMilliseconds = denomeJSONObject.getLong("Pulse Timestamp in Milliseconds");
									long pulseDuration = denomeJSONObject.getLong("Pulse Creation Duration Millis");
									//
									// get the operational data chain in the purpose nucleus
									//
									JSONObject internalVitalDene=null, internalDescriptiveDeneChain=null;
									String timeZoneId = "UTC";
									int basePulseFrequency=60;
									String currentIdentityMode="";
									try {
										internalDescriptiveDeneChain = DenomeUtils.getDeneChainByName(denomeJSONObject,TeleonomeConstants.NUCLEI_INTERNAL,  TeleonomeConstants.DENECHAIN_DESCRIPTIVE);
										internalVitalDene = DenomeUtils.getDeneByName(internalDescriptiveDeneChain, "Vital");
										timeZoneId = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(internalVitalDene, "Timezone", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
										
										basePulseFrequency = ((int) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(internalVitalDene, "Base Pulse Frequency", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE))/1000;
										operationalDataDeneChain = DenomeUtils.getDeneChainByName(denomeJSONObject,TeleonomeConstants.NUCLEI_PURPOSE,  TeleonomeConstants.DENECHAIN_OPERATIONAL_DATA);
										sensorDataDeneChain = DenomeUtils.getDeneChainByName(denomeJSONObject,TeleonomeConstants.NUCLEI_PURPOSE,  TeleonomeConstants.DENECHAIN_SENSOR_DATA);
									} catch (InvalidDenomeException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}

									//// System.out.println("AsyncServlet, timeZoneId=" + timeZoneId);
									TimeZone currentTimeZone = null;
									if(timeZoneId!=null && !timeZoneId.equals("")){
										currentTimeZone = TimeZone.getTimeZone(timeZoneId);
									}else{
										currentTimeZone = TimeZone.getDefault();
									}

									

									JSONObject vitalDene = DenomeUtils.getDeneByName(operationalDataDeneChain, "Vital");
									
									currentIdentityMode = (String) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(vitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_IDENTITY_MODE, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
									
									int currentPulse = (Integer) DenomeUtils.getDeneWordAttributeByDeneWordNameFromDene(vitalDene, TeleonomeConstants.DENEWORD_TYPE_CURRENT_PULSE_FREQUENCY, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
									//// System.out.println("AsyncServlet, currentPulse=" + currentPulse);
									String newLine = System.getProperty("line.separator");
									//
									// end of processinglgic
									//
									//
									// now make the object
									//
									JSONObject dataForBrowser = new JSONObject();

									dataForBrowser.put("Enable UI", true);
									dataForBrowser.put("Base Pulse Frequency",basePulseFrequency);	
									dataForBrowser.put("Current Identity Mode",currentIdentityMode);
									
									dataForBrowser.put("Current Pulse", currentPulse);
									dataForBrowser.put("Pulse Timestamp", pulseTimestamp);
									dataForBrowser.put("Pulse Timestamp in Milliseconds", pulseTimestampInMilliseconds);
									dataForBrowser.put("Pulse Creation Duration Millis",pulseDuration);
									String s = TeleonomeConstants.PULSE_FINISHED_MAPPED_BUS_MESSAGE + " at  "+ aSimpleDateFormat.format(new Timestamp(endPulseTimeInMillis));
									dataForBrowser.put("Pulse Status", s);
									dataForBrowser.put("Data Source","AsyncServlet");

									



									//// System.out.println("Async  yep is a pulse:" );

									//
									// ok we are good

								


									asyncContext.getResponse().setContentType("text/plain");
									//asyncContext.getResponse().setContentType("application/json;charset=UTF-8");

									asyncContext.getResponse().setCharacterEncoding("UTF-8");
									try {

										//asyncContext.getResponse().getWriter().print(dataForBrowser.toString());

										//	// System.out.println("Async  about to send " + dataForBrowser.toString());
										asyncContext.getResponse().getWriter().print(dataForBrowser.toString());
										asyncContext.getResponse().getWriter().flush();
										asyncContext.complete();
										keepRunning=false;
										//asyncContext.getResponse().flushBuffer();
									} catch (IOException ex) {
										// System.out.println("Async  fail");
									}

									

									generatingPulse=false;

								}catch (JSONException e) {
									// TODO Auto-generated catch block
									keepRunning=false;
									// System.out.println("Async  A pulse was not formated properluy, text=" + denomeFileInString);
								}
							} 
						}else  if (changed.endsWith("StartPulse.info")) {
							//// System.out.println("Async  pulse has started");

							long startedPulseTimeInMillis =0;
							String s = FileUtils.readFileToString(selectedFile);
							if(!s.equals(""))startedPulseTimeInMillis=Long.parseLong(s);
							String lastEvent=TeleonomeConstants.STARTING_PULSE_MAPPED_BUS_MESSAGE + ":" + aSimpleDateFormat.format(new Timestamp(startedPulseTimeInMillis));

							//asyncContext.getResponse().setContentType("application/json");
							asyncContext.getResponse().setContentType("text/html");

							asyncContext.getResponse().setCharacterEncoding("UTF-8");
							
								try {
									JSONObject dataForBrowser = new JSONObject();
									dataForBrowser.put("Enable UI", false);
									//asyncContext.getResponse().getWriter().print(dataForBrowser.toString());
									asyncContext.getResponse().getWriter().print(lastEvent);
									asyncContext.getResponse().getWriter().flush();
									//asyncContext.getResponse().flushBuffer();
									asyncContext.complete();
									keepRunning=false;
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}



							
						}
					}
					// reset the key
					boolean valid = wk.reset();
					if (!valid) {
						// System.out.println("Async  Key has been unregisterede");
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				String s = Utils.getStringException(e);
				// System.out.println("s=" + s);

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				String s = Utils.getStringException(e);
				// System.out.println("s=" + s);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				String s = Utils.getStringException(e);
				//ystem.out.println("s=" + s);

			}
			//// System.out.println("Async  Existin FileWatcherThread");
		}
	}


}



