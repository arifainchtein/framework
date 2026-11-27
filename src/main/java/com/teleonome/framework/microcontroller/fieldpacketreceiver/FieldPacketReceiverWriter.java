package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.hypothalamus.Hypothalamus;
import com.teleonome.framework.microcontroller.MotherMicroController;
import com.teleonome.framework.utils.Utils;

/**
 * The Field Packet Receiver's ingestion (2026-11-27), built like SFTPPublisherWriter: the command
 * "Ingest Field Packets" comes from the mutation of the same name, which the Async Cycle (MappedBusThread, its
 * "process the commands" part) runs through the action "Field Packet Receiver Ingest". The work runs on its own
 * thread with a hard deadline, so a stuck packet can't stall the Async Cycle. Every other command is answered
 * "Ok" straight away: the receiver has nothing to do in the synchronous cycle.
 *
 * First the codes, on the Async Cycle's own thread (the caller of write()), because the secret lives in
 * Annabelle's ESP32 and its serial port belongs to this cycle: each waiting packet's code goes to the mother
 * microcontroller's verifyUserCommandCode() - VerifyUserCode#<code>, the same check MappedBusThread makes for
 * a command's code. The ESP32 accepts codes up to 15 minutes old, so a packet waiting longer than
 * CODE_MAX_AGE_MS is refused ("send it again"). Five wrong codes in an hour would lock the ESP32's checking - and
 * with it the Teleonome page's commands - so the receiver allows itself only MAX_WRONG_CODES_PER_HOUR; after that
 * packets are refused unchecked until the hour has passed. A code is asked about once per cycle, however many
 * packets carry it. A Teleonome without a mother microcontroller falls back to the Secret File, if one is set.
 *
 * Then, for each packet whose code passed, oldest first: FieldPacket.check() again (the settings may have changed since it
 * arrived), the processor for its Packet Type, then the zip moves to store/<type>/<id>/. A packet that fails
 * moves to rejected/ with the reason - the reply says "Failure-..." only if ingestion itself broke, never
 * because of one bad packet, and never "Fault" (MappedBusThread turns every Fault into a pathology).
 *
 * The denome only learns that a record exists and how to reach it: one dene per record in Purpose:Field Records,
 *   Record Type  "Plant"
 *   Record Id    "Meyer_lemon"
 *   Record File  /home/pi/Teleonome/FieldPackets/plants/Meyer_lemon/record.json (its photos beside it)
 *   Record Host  this Pi's name, where that file is
 * The record itself (visits, photos, later analyses and follow-ups) stays on disk.
 */
public class FieldPacketReceiverWriter extends BufferedWriter {

	public static final String COMMAND_INGEST = "Ingest Field Packets";
	public static final String FIELD_RECORDS_CHAIN = "Field Records";
	private static final long INGEST_DEADLINE_MS = 120000;
	private static final long CODE_MAX_AGE_MS = 14 * 60 * 1000L;
	private static final int MAX_WRONG_CODES_PER_HOUR = 2;

	// Checks one code; true/false, or null when there is no way to check it. Replaceable for tests.
	public interface CodeVerifier {
		Boolean verify(String code) throws IOException;
	}

	private final Logger logger = Logger.getLogger(getClass());
	private final FieldPacketSettings settings;
	private final DenomeManager aDenomeManager;
	private volatile String ingestionResults = "Ok";
	private Thread ingestThread = null;
	private final LinkedList<Long> wrongCodeTimes = new LinkedList<Long>();
	public CodeVerifier codeVerifier;

	public FieldPacketReceiverWriter(Writer out, FieldPacketSettings s, DenomeManager d, final Hypothalamus h) {
		super(out);
		settings = s;
		aDenomeManager = d;
		codeVerifier = new CodeVerifier() {
			public Boolean verify(String code) throws IOException {
				MotherMicroController mother = h == null ? null : h.getMotherMicroController();
				if (mother != null) return mother.verifyUserCommandCode(code);
				if (settings.secretFile != null && settings.secretFile.isFile()) return new FieldPacketTotp(settings).verify(code);
				return null;
			}
		};
	}

	public String getIngestionResults() {
		return ingestionResults;
	}

	public void write(String command, int off, int len) throws IOException {
		logger.debug("received command:" + command);
		if (command != null && command.startsWith(COMMAND_INGEST)) {
			int[] refused = { 0 };
			List<File> verified = checkCodes(refused);
			String r = verified.isEmpty() ? "Ok-Ingest Field Packets#Ingested=0|Rejected=0" : ingestWithDeadline(verified);
			if (verified.isEmpty()) syncDenome();  // the first cycle after a restart puts the chains in
			ingestionResults = r + (refused[0] > 0 ? "|Code Refused=" + refused[0] : "");
			// visits not analysed yet (new ones, or earlier failures) - on its own thread, see FieldRecordAnalyzer
			try {
				FieldRecordAnalyzer.startIfIdle(settings);
			} catch (Throwable t) {  // e.g. the Anthropic SDK missing from the jar: ingestion must go on regardless
				logger.warn("Field record analysis not started: " + t);
			}
		} else {
			ingestionResults = "Ok";
		}
	}

	public void write(String command) throws IOException {
		write(command, 0, command == null ? 0 : command.length());
	}

	public void flush() {
	}

	public void close() {
	}

	// The waiting packets whose codes passed, oldest first; the others are moved to rejected/ with the reason.
	private List<File> checkCodes(int[] refused) {
		List<File> verified = new ArrayList<File>();
		File[] zips = settings.inbox().listFiles((d, name) -> name.endsWith(".zip") || name.endsWith(FieldPacketStore.ACTION_EXTENSION));
		if (zips == null) return verified;
		Arrays.sort(zips, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
		Map<String, Boolean> asked = new HashMap<String, Boolean>();
		long now = System.currentTimeMillis();
		for (Iterator<Long> it = wrongCodeTimes.iterator(); it.hasNext();) if (now - it.next() > 3600000L) it.remove();
		for (File zip : zips) {
			String packetId = FieldPacketStore.idOf(zip);
			String[] code = FieldPacketStore.readCode(settings, packetId);
			String why = null;
			if (code == null) {
				why = "no code came with the packet";
			} else if (now - Long.parseLong(code[1]) > CODE_MAX_AGE_MS) {
				why = "its code expired before the Async Cycle could check it - send it again";
			} else {
				Boolean ok = asked.get(code[0]);
				if (ok == null && !asked.containsKey(code[0])) {
					if (wrongCodeTimes.size() >= MAX_WRONG_CODES_PER_HOUR) {
						why = "not checked: " + wrongCodeTimes.size() + " wrong codes in the last hour (the ESP32 locks checking after 5) - send it again later";
					} else {
						try {
							ok = codeVerifier.verify(code[0]);
						} catch (IOException e) {
							logger.warn("Field packet " + packetId + ": the code check failed: " + e.getMessage());
							continue;  // stays in the inbox, checked again next cycle while the code is still good
						}
						asked.put(code[0], ok);
						if (Boolean.FALSE.equals(ok)) wrongCodeTimes.add(now);
					}
				}
				if (why == null) {
					if (ok == null) why = "this Teleonome has no way to check codes (no mother microcontroller, no Secret File)";
					else if (!ok) why = "wrong code - check the sender's secret and clock (real UTC)";
				}
			}
			if (why == null) {
				verified.add(zip);
				continue;
			}
			refused[0]++;
			logger.warn("Field packet " + packetId + " refused: " + why);
			try {
				FieldPacketStore.reject(settings, zip, packetId, why);
			} catch (IOException e) {
				logger.warn("could not move " + zip + " to rejected/: " + e.getMessage());
			}
		}
		return verified;
	}

	private String ingestWithDeadline(final List<File> zips) {
		if (ingestThread != null && ingestThread.isAlive()) return "Failure-Ingest Field Packets#previous ingestion still running";
		FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
			public String call() {
				return ingest(zips);
			}
		});
		ingestThread = new Thread(task, "FieldPacketIngest");
		ingestThread.setDaemon(true);
		ingestThread.start();
		try {
			return task.get(INGEST_DEADLINE_MS, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			return "Failure-Ingest Field Packets#no result after " + INGEST_DEADLINE_MS / 1000 + " s";
		} catch (ExecutionException e) {
			logger.warn(Utils.getStringException(e));
			return "Failure-Ingest Field Packets#" + e.getCause();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return "Failure-Ingest Field Packets#interrupted";
		}
	}

	private String ingest(List<File> zips) {
		int ingested = 0, rejected = 0, actions = 0;
		for (File zip : zips) {
			String packetId = FieldPacketStore.idOf(zip);
			try {
				if (zip.getName().endsWith(FieldPacketStore.ACTION_EXTENSION)) {
					JSONObject action = new JSONObject(new String(Files.readAllBytes(zip.toPath()), java.nio.charset.StandardCharsets.UTF_8));
					String result = FieldRecordActions.apply(settings, action);
					FieldPacketStore.actionsDone(settings).mkdirs();
					FieldPacketStore.writeJson(new File(FieldPacketStore.actionsDone(settings), packetId + ".json"),
							new JSONObject().put("Action", action).put("Result", result).put("Done", FieldSubjects.now()));
					zip.delete();
					FieldPacketStore.forgetCode(settings, packetId);
					actions++;
					logger.info("Field record action " + packetId + ": " + result);
					continue;
				}
				FieldPacket packet = FieldPacket.open(Files.readAllBytes(zip.toPath()));
				packet.check(settings.acceptedTypes);
				if (!packet.getPacketId().equals(packetId)) throw new FieldPacketException("the file name does not match the Packet Id");
				JSONObject where = FieldPacketProcessor.forType(packet.getPacketType()).process(packet, settings);
				File dir = FieldPacketStore.storeDir(settings, packet.getPacketType(), packetId);
				dir.mkdirs();
				Files.move(zip.toPath(), new File(dir, "packet.zip").toPath());
				FieldPacketStore.forgetCode(settings, packetId);
				FieldPacketStore.writeJson(new File(dir, FieldPacket.MANIFEST), packet.getManifest());
				ingested++;
				logger.info("Field packet ingested: " + packet.getPacketType() + " " + packetId + " -> "
						+ (where.has("Record Id") ? "record " + where.optString("Record Id") : "Arrivals"));
			} catch (FieldPacketException e) {
				rejected++;
				logger.warn("Field packet " + packetId + " rejected: " + e.getMessage());
				try {
					FieldPacketStore.reject(settings, zip, packetId, e.getMessage());
				} catch (IOException io) {
					logger.warn("could not move " + zip + " to rejected/: " + io.getMessage());
				}
			} catch (Exception e) {
				// left in the inbox, tried again next Async Cycle
				logger.warn("Field packet " + packetId + " not ingested: " + Utils.getStringException(e));
				return "Failure-Ingest Field Packets#" + packetId + ": " + e.getMessage();
			}
		}
		syncDenome();
		return "Ok-Ingest Field Packets#Ingested=" + ingested + "|Actions=" + actions + "|Rejected=" + rejected;
	}

	// The denome only knows that areas and records exist and where they are (2026-11-27):
	//   Purpose:Field Records          "Field Records Receiver" (Record Host, Listen Port) + one pointer dene per
	//                                  plant or bed in no area
	//   Purpose:Field Records <Area>   "Area" (Area Name, Created) + one pointer dene per plant or bed in it
	// A pointer dene, named after the subject: Record Type (Plant/Bed), Record Id, Record File, Record Host.
	// Built from disk (FieldSubjects) and put in only when it changed; chains of areas that no longer exist go.
	private String lastSynced = null;

	void syncDenome() {
		try {
			java.util.Map<String, JSONArray> chains = new java.util.LinkedHashMap<String, JSONArray>();
			JSONArray base = new JSONArray();
			JSONObject receiver = new JSONObject().put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, "Field Records Receiver").put("DeneWords", new JSONArray()
					.put(DenomeUtils.buildDeneWordJSONObject("Record Host", hostName(), null, TeleonomeConstants.DATATYPE_STRING, true))
					.put(DenomeUtils.buildDeneWordJSONObject("Listen Port", "" + settings.listenPort, null, TeleonomeConstants.DATATYPE_INTEGER, true)));
			base.put(receiver);
			chains.put(FIELD_RECORDS_CHAIN, base);
			JSONArray areas = FieldSubjects.areas(settings);
			for (int i = 0; i < areas.length(); i++) {
				JSONObject area = areas.getJSONObject(i);
				JSONArray denes = new JSONArray();
				denes.put(new JSONObject().put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, "Area").put("DeneWords", new JSONArray()
						.put(DenomeUtils.buildDeneWordJSONObject("Area Name", area.optString("Name"), null, TeleonomeConstants.DATATYPE_STRING, true))
						.put(DenomeUtils.buildDeneWordJSONObject("Created", area.optString("Created"), null, TeleonomeConstants.DATATYPE_STRING, true))));
				chains.put(FIELD_RECORDS_CHAIN + " " + area.optString("Name"), denes);
			}
			for (JSONObject r : FieldSubjects.subjects(settings)) {
				String chain = r.optString("Area", "").isEmpty() ? FIELD_RECORDS_CHAIN : FIELD_RECORDS_CHAIN + " " + r.optString("Area");
				if (!chains.containsKey(chain)) chain = FIELD_RECORDS_CHAIN;  // an area that was removed by hand
				File f = new File(FieldPacketStore.recordDir(settings, r.optString("Record Id")), FieldPacketStore.RECORD_FILE);
				chains.get(chain).put(new JSONObject().put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, r.optString("Plant")).put("DeneWords", new JSONArray()
						.put(DenomeUtils.buildDeneWordJSONObject("Record Type", r.optString("Record Type", FieldSubjects.TYPE_PLANT), null, TeleonomeConstants.DATATYPE_STRING, true))
						.put(DenomeUtils.buildDeneWordJSONObject("Record Id", r.optString("Record Id"), null, TeleonomeConstants.DATATYPE_STRING, true))
						.put(DenomeUtils.buildDeneWordJSONObject("Record File", f.getAbsolutePath(), null, TeleonomeConstants.DATATYPE_STRING, true))
						.put(DenomeUtils.buildDeneWordJSONObject("Record Host", hostName(), null, TeleonomeConstants.DATATYPE_STRING, true))));
			}
			String now = new JSONObject(chains).toString();
			if (now.equals(lastSynced) || aDenomeManager == null) return;
			for (String name : purposeChainNames()) {
				if (name.equals(FIELD_RECORDS_CHAIN) || name.startsWith(FIELD_RECORDS_CHAIN + " ")) aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_PURPOSE, name);
			}
			for (java.util.Map.Entry<String, JSONArray> e : chains.entrySet()) {
				JSONObject chain = new JSONObject();
				chain.put(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE, e.getKey());
				chain.put("Denes", e.getValue());
				aDenomeManager.injectDeneChainIntoNucleus(TeleonomeConstants.NUCLEI_PURPOSE, chain);
			}
			lastSynced = now;
		} catch (Exception e) {
			// the records are on disk either way; the denome is brought up to date at the next ingest
			logger.warn("could not bring " + FIELD_RECORDS_CHAIN + " up to date in the denome: " + Utils.getStringException(e));
		}
	}

	private List<String> purposeChainNames() {
		List<String> names = new ArrayList<String>();
		JSONObject pulse = aDenomeManager.getCurrentlyCreatingPulseJSONObject();
		if (pulse == null) return names;
		JSONArray nuclei = pulse.getJSONObject("Denome").getJSONArray("Nuclei");
		for (int i = 0; i < nuclei.length(); i++) {
			JSONObject n = nuclei.getJSONObject(i);
			if (!TeleonomeConstants.NUCLEI_PURPOSE.equals(n.optString("Name"))) continue;
			JSONArray chains = n.optJSONArray("DeneChains");
			for (int j = 0; chains != null && j < chains.length(); j++) names.add(chains.getJSONObject(j).optString(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE));
		}
		return names;
	}

	private static String hostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName() + ".local";
		} catch (Exception e) {
			return "localhost";
		}
	}
}
