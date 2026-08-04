package com.teleonome.framework.microcontroller.annabellemicrocontroller;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.exception.PersistenceException;
import com.teleonome.framework.exception.ServletProcessingException;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             
public class AnnabelleReader extends BufferedReader{
	Logger logger;
	BufferedReader reader;
	String command="";
	DenomeManager aDenomeManager;
	PostgresqlPersistenceManager aDBManager;
	Hypothalamus hypothalamus;
	private String currentCommand="";
	//
	// Set before an AsyncData request (via AsyncDataCount, queried first) so
	// readLine() knows how many data lines to expect before the "Ok-" sentinel.
	// Without this, a corrupted/lost sentinel (e.g. from a stray debug line
	// landing mid-response) left the drain loop below with no bound at all --
	// it only gave up on repeated IOExceptions, never on a read that just never
	// arrives. -1 means "unknown", i.e. behave as before (sentinel-only exit).
	// See conversation 2026-07-16 (ChinampaMonitor Hypothalamus hangs).
	//
	private int expectedDataLineCount = -1;
	public void setExpectedDataLineCount(int n) {
		expectedDataLineCount = n;
	}
	public AnnabelleReader(BufferedReader in ,Hypothalamus h,DenomeManager d, PostgresqlPersistenceManager db) {
		super(in);
		reader=in;
		hypothalamus=h;
		logger = Logger.getLogger(getClass().getName());
		aDenomeManager=d;
		aDBManager=db;
		// TODO Auto-generated constructor stub

	}

	public void close() throws IOException {
		logger.info("about to close AnnabellReader");
		//String trace = Utils.generateMethodTrace();
	//	logger.debug(trace);
		super.close();
	}
	public boolean ready() throws IOException {
		
		return reader.ready();
	}
	public void setCurrentCommand(String s) {
		command=s;
		
		logger.debug("setting , command:" + command);
		
	}
	public String readLine(){
		logger.debug("waiting for response for  command:" + command);
		if(command.equals(""))return "";
		if(command.startsWith(TeleonomeConstants.DELETE_TELEPATHON)) {
			logger.debug("returning because its delete telepathon");
			return "Ok";
		}
		
		if(command.startsWith(TeleonomeConstants.DELETE_STALE_TELEPATHONS)) {
			logger.debug("returning because its delete stale telepathon");
			return "Ok";
		}

		if(command.startsWith(TeleonomeConstants.RENAME_TELEPATHON)) {
			logger.debug("returning because its rename telepathon");
			return "Ok";
		}

		String line="", className;
		int counter=0;
		int maxTries=10;
		int linesRead=0;
		//
		// Neither maxTries (IOExceptions only) nor expectedDataLineCount (opt-in,
		// only set before AsyncDataCount-style requests) bounds a degraded/noisy
		// radio link that keeps producing plausible-looking lines that never hit
		// an "Ok-"/"Failure" sentinel -- this loop could otherwise drain lines
		// indefinitely, relying entirely on PulseThread's blunt 30s whole-process
		// watchdog to notice and kill Hypothalamus. Give it its own deadline so a
		// stuck read fails fast, here, at the actual point of failure. This has to
		// be an INACTIVITY timeout, reset on every successfully-read line, not a
		// fixed deadline for the whole call -- AsyncData can legitimately emit one
		// line per queued record across six device queues (see MappedBusThread's
		// AsyncDataCount comment), and at 115200 baud a real backlog after a radio
		// outage (already seen repeatedly on this host today) can easily take
		// longer than 10s to drain even though every line is arriving fine. A flat
		// deadline would kill exactly the bulk catch-up transfer you want to
		// survive; measuring silence instead of total duration only catches an
		// actually-wedged read. See conversation 2026-08-04 (ChinampaMonitor
		// AsyncCycle wedges).
		//
		long readLineDeadlineMillis = System.currentTimeMillis() + 10000;
		String deserializer, deviceName;
		String[] tokens;
		boolean keepGoing=true;
		AnnabelleDeserializer cc;
		JSONObject telepathon = null;
		boolean appendString=false;
		boolean processString=false;
		//
		// AsyncDataCount replies with the count on its own line, then a separate
		// "Ok-AsyncDataCount" sentinel line. The sentinel-only match below would
		// otherwise return "Ok-AsyncDataCount" itself and silently drop the count
		// line as an unrecognized line -- capture it here instead.
		//
		String capturedAsyncDataCountValue = null;
		String capturedQueueStatusValue = null;
		while(keepGoing) {
			try {
				String rawLine = reader.readLine();
				if(rawLine==null) {
					logger.warn("underlying reader returned null (stream closed?), giving up on this response");
					line=null;
					keepGoing=false;
					appendString=false;
					continue;
				}
				if(appendString) {
					line = line + rawLine.replaceAll("\u0000", "");
				}else {
					line = rawLine.replaceAll("\u0000", "");
				}
				linesRead++;
				//
				// Forward progress -- push the deadline out another 10s so a large
				// but healthily-progressing transfer never trips it, no matter how
				// long the whole response takes.
				//
				readLineDeadlineMillis = System.currentTimeMillis() + 10000;

				logger.debug("line 63, received line=" + line);
				if(line.contains("Ok-") || line.contains("Failure"))
				{
					keepGoing=false;
					appendString=false;
				}else if(expectedDataLineCount>=0 && linesRead>expectedDataLineCount+1) {
					//
					// We were told (via AsyncDataCount) to expect at most
					// expectedDataLineCount data lines plus one terminal "Ok-"/"Failure"
					// line. Blowing past that without hitting the sentinel means it was
					// lost or corrupted (e.g. an unrelated debug line interleaved into
					// the stream) -- stop waiting rather than draining forever.
					//
					logger.warn("exceeded expected line count (" + expectedDataLineCount + " data lines) without seeing a terminal Ok-/Failure line, giving up on this response after " + linesRead + " lines");
					keepGoing=false;
					appendString=false;
				}else if(command.equals("AsyncDataCount") && capturedAsyncDataCountValue==null) {
					capturedAsyncDataCountValue = line.trim();
				}else if(command.equals("AsyncDataCount") && line.startsWith("QueueStatus#") && capturedQueueStatusValue==null) {
					//
					// Per-type available/dropped/flash-health-days breakdown, added
					// 2026-08-04 (see conversation) -- the second line of the
					// AsyncDataCount reply, after the bare total captured above and
					// before the "Ok-AsyncDataCount" sentinel.
					//
					capturedQueueStatusValue = line.trim();
				}else {
					tokens = line.split("#");
					deserializer=tokens[0];
					logger.debug("line 90, deserializer=" + deserializer);
					int l = "deserializer".length();
					 processString=false;
					if(deserializer.length()>l) {
						
						if(tokens.length>46 && deserializer.contains("DigitalStablesDataDeserializer")) {
							deserializer="DigitalStablesDataDeserializer";
							processString=true;
							appendString=false;
						}else if(tokens.length>24 && deserializer.contains("SeedlingMonitorDataDeserializer")) {
							deserializer="SeedlingMonitorDataDeserializer";
							processString=true;
							appendString=false;
						}else if(tokens.length>35 && deserializer.contains("ChinampaDataDeserializer")) {
							deserializer="ChinampaDataDeserializer";
							processString=true;
							appendString=false;
						}else if(tokens.length>8 && deserializer.contains("CommaRecordDeserializer")) {
							deserializer="CommaRecordDeserializer";
							processString=true;
							appendString=false;
						}else if(tokens.length>27 && deserializer.contains("LangleyDataDeserializer")) {
							deserializer="LangleyDataDeserializer";
							processString=true;
							appendString=false;
						}
					}
					
					if(processString) {
						try {	    
							className = "com.teleonome.framework.microcontroller.annabellemicrocontroller." + deserializer;
							logger.debug("className for deserializer =" + className);
							AnnabelleDeserializer annabellDeserializer = AnnabelleDeserializerFactory.createAnnabellDeserializer(className);
							if(annabellDeserializer!=null) {
								annabellDeserializer.setMnemosyneManager(aDenomeManager.getMnemosyneManager());
								String teleonomeName = aDenomeManager.getDenomeName();

								if(annabellDeserializer instanceof CommaRecordDeserializer) {
									JSONObject commaData = annabellDeserializer.deserialise(teleonomeName, line);
									long sourceoriginaltime = annabellDeserializer.getSourceoriginaltime();
									if(commaData != null && commaData.has("devicename") && commaData.has("serialnumber")) {
										String devicename = commaData.getString("devicename");
										String serialnumber = commaData.getString("serialnumber");
										if(aDenomeManager.isKnownTelepathonDevice(devicename, serialnumber)) {
											try {
												aDenomeManager.storeCommaRecord(sourceoriginaltime, devicename, commaData);
											} catch (PersistenceException e) {
												logger.warn(Utils.getStringException(e));
											}
										} else {
											logger.warn("CommaRecord rejected — no known device with name='" + devicename + "' serialnumber='" + serialnumber + "'");
										}
									} else {
										logger.debug("CommaRecordDeserializer returned empty result for: " + line);
									}
								} else {
									telepathon = annabellDeserializer.deserialise(teleonomeName,line);
									long sourceoriginaltime = annabellDeserializer.getSourceoriginaltime();
									if(telepathon!=null && telepathon.has(TeleonomeConstants.DENE_NAME_ATTRIBUTE)) {
										String telepathonName = telepathon.getString(TeleonomeConstants.DENE_NAME_ATTRIBUTE);
										// Occasional serial-line corruption truncates/garbles the device name
										// token (e.g. "TopTank" -> "TOPT", "Chinampa" -> "ghSUMP") while the
										// serial number token survives intact -- observed 2026-07-21/22, two
										// bad rows landed in telepathon_2026_7_21 as their own phantom
										// telepathons. Where the deserializer reports a Serial Number, reject
										// only if that serial number is already registered under a *different*
										// name (the actual corruption signature) -- a serial number never seen
										// before is treated as a legitimate new device and allowed to bootstrap
										// its own DeneChain. (An earlier version of this guard required an
										// exact name+serial match against an already-known telepathon, which
										// deadlocked every brand-new device: it could never become "known"
										// without first being accepted. Fixed 2026-07-24.) Deserializers that
										// don't report a Serial Number keep the prior unvalidated behaviour.
										boolean accept = true;
										if (telepathon.has("Serial Number")) {
											String serialNumber = telepathon.getString("Serial Number");
											String knownName = aDenomeManager.getKnownNameForSerial(serialNumber);
											accept = (knownName == null) || knownName.equals(telepathonName);
											if (!accept) {
												logger.warn("Telepathon record rejected -- serialnumber='" + serialNumber
														+ "' is already registered under name='" + knownName
														+ "' but this record reports name='" + telepathonName
														+ "' (likely corrupted device-name token): " + line);
											}
										}
										if (accept) {
											//
											// Idempotency guard, added 2026-08-04 (see conversation).
											// storeTelepathon() below is already safe to call twice for
											// the same reading (it checks for an existing row before
											// inserting), but this live-state overwrite wasn't -- a
											// duplicate or out-of-order record (a LoRa repeater
											// re-transmitting a packet it heard from the origin device,
											// or Annabelle's flash overflow tier resending a backlog
											// after an interrupted drain) could otherwise regress the
											// live denome to a stale reading after a newer one already
											// landed. Skip the live-state overwrite when what's already
											// there is at least as fresh; history still gets stored
											// either way, below.
											//
											long existingSecondsTime = -1;
											try {
												existingSecondsTime = aDenomeManager.getTelepathonSecondsTime(telepathonName);
											} catch (JSONException je) {
												logger.warn("could not check existing telepathon timestamp for '" + telepathonName + "': " + Utils.getStringException(je));
											}
											if (existingSecondsTime >= sourceoriginaltime) {
												logger.debug("Telepathon '" + telepathonName + "' record (sourceoriginaltime=" + sourceoriginaltime
														+ ") is not newer than current live state (Seconds Time=" + existingSecondsTime
														+ ") -- skipping live-state overwrite, likely a repeater or overflow-resend duplicate");
											} else {
												aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_TELEPATHONS, telepathonName);
												aDenomeManager. injectDeneChainIntoNucleus(TeleonomeConstants.NUCLEI_TELEPATHONS,telepathon);
											}
											try {
												aDenomeManager.storeTelepathon(sourceoriginaltime,  telepathonName,  telepathon);
											} catch (PersistenceException e) {
												logger.warn(Utils.getStringException(e));
											}
											hypothalamus.publishToHeart(TeleonomeConstants.HEART_TOPIC_TELEPATHON_STATUS, telepathon.toString());
										}
									}else {
										logger.debug("Error deserializing " + line);
									}
								}

							}else {
								logger.debug("className=" + className + " does not existis");
							}
							
						} catch (ServletProcessingException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					}
				}
				//
				// now process
			}catch(IOException e) {
				logger.warn("Exception reading line, counter=" + counter);
				logger.warn(Utils.getStringException(e));
				counter++;
			}
			if(counter>maxTries) {
				keepGoing=false;
			}
			if(keepGoing && System.currentTimeMillis()>readLineDeadlineMillis) {
				logger.warn("readLine saw no forward progress for 10s (" + linesRead + " line(s) read so far, no terminal Ok-/Failure line) -- likely a wedged read, giving up on this response");
				keepGoing=false;
				appendString=false;
				line=null;
			}
		}
		     
		logger.debug("the response is:   " + line);
		expectedDataLineCount = -1;
		String cleaned="";
		if(line==null) {
			cleaned="";
		}else if(command.equals("AsyncData")) {
			//
			// Must be checked before the telepathon shortcut below -- DSD (or any
			// non-Comma type) is almost always present in a real AsyncData batch,
			// which leaves `telepathon` non-null with a "Name" field by the time
			// the sentinel line is reached, so that branch would otherwise hijack
			// and discard the per-type downloaded-counts now appended to
			// "Ok-AsyncData#..." (added 2026-08-04), replacing it with an
			// unrelated leftover telepathon JSON blob. Return the raw sentinel
			// line as-is so the caller (MappedBusThread) can parse it directly.
			//
			cleaned = line;
		}else if(line.contains("Ok-") && telepathon!=null && telepathon.has(TeleonomeConstants.DENE_NAME_ATTRIBUTE)) {
			cleaned=line.substring(line.indexOf("Ok-"));;
			cleaned=TeleonomeConstants.HEART_TOPIC_TELEPATHON_STATUS+"#"+telepathon.toString();
		}else if(line.contains("Read fail") && line.contains("#")){
			cleaned=line.substring(line.lastIndexOf("fail")+4);
		}else if(command.equals("AsyncDataCount")) {
			cleaned = "AsyncDataCount#" + (capturedAsyncDataCountValue!=null ? capturedAsyncDataCountValue : "")
				+ (capturedQueueStatusValue!=null ? ("|" + capturedQueueStatusValue) : "");
		}else if(command.equals("GetSensorData")) {
			//
			// AnnabelleWriter translated GetSensorData into a Ping - Ok-Ping
			// means the serial link is alive, anything else (including an
			// empty line from maxTries being exceeded above) means it isn't.
			//
			cleaned = line.contains("Ok-Ping") ? "true" : "false";
		}else {
			cleaned=line;
		}
		logger.debug("cleaned:  " + cleaned);

		return cleaned;
	}
	}