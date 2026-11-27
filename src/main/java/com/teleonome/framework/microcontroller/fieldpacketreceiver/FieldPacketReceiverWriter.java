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
			ingestionResults = r + (refused[0] > 0 ? "|Code Refused=" + refused[0] : "");
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
		File[] zips = settings.inbox().listFiles((d, name) -> name.endsWith(".zip"));
		if (zips == null) return verified;
		Arrays.sort(zips, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
		Map<String, Boolean> asked = new HashMap<String, Boolean>();
		long now = System.currentTimeMillis();
		for (Iterator<Long> it = wrongCodeTimes.iterator(); it.hasNext();) if (now - it.next() > 3600000L) it.remove();
		for (File zip : zips) {
			String packetId = zip.getName().substring(0, zip.getName().length() - ".zip".length());
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
		int ingested = 0, rejected = 0;
		for (File zip : zips) {
			String packetId = zip.getName().substring(0, zip.getName().length() - ".zip".length());
			try {
				FieldPacket packet = FieldPacket.open(Files.readAllBytes(zip.toPath()));
				packet.check(settings.acceptedTypes);
				if (!packet.getPacketId().equals(packetId)) throw new FieldPacketException("the file name does not match the Packet Id");
				JSONObject pointer = FieldPacketProcessor.forType(packet.getPacketType()).process(packet, settings);
				File dir = FieldPacketStore.storeDir(settings, packet.getPacketType(), packetId);
				dir.mkdirs();
				Files.move(zip.toPath(), new File(dir, "packet.zip").toPath());
				FieldPacketStore.forgetCode(settings, packetId);
				FieldPacketStore.writeJson(new File(dir, FieldPacket.MANIFEST), packet.getManifest());
				markRecord(pointer);
				ingested++;
				logger.info("Field packet ingested: " + packet.getPacketType() + " " + packetId + " -> record " + pointer.optString("Record Id"));
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
		return "Ok-Ingest Field Packets#Ingested=" + ingested + "|Rejected=" + rejected;
	}

	// Adds the record's pointer dene to Purpose:Field Records, once per record. Nothing else of the record goes
	// into the denome.
	private void markRecord(JSONObject pointer) {
		String recordName = pointer.optString("Record Name");
		try {
			JSONObject existing = null;
			try {
				existing = aDenomeManager.getDeneChainByName(TeleonomeConstants.NUCLEI_PURPOSE, FIELD_RECORDS_CHAIN);
			} catch (Exception e) {
				existing = null;
			}
			JSONArray denes = new JSONArray();
			if (existing != null && existing.optJSONArray("Denes") != null) {
				JSONArray old = existing.getJSONArray("Denes");
				for (int i = 0; i < old.length(); i++) {
					JSONObject dene = old.getJSONObject(i);
					if (recordName.equals(dene.optString(TeleonomeConstants.DENE_NAME_ATTRIBUTE))) return;  // already marked
					denes.put(dene);
				}
			}
			JSONObject dene = new JSONObject();
			dene.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, recordName);
			JSONArray words = new JSONArray();
			dene.put("DeneWords", words);
			words.put(DenomeUtils.buildDeneWordJSONObject("Record Type", pointer.optString("Record Type"), null, TeleonomeConstants.DATATYPE_STRING, true));
			words.put(DenomeUtils.buildDeneWordJSONObject("Record Id", pointer.optString("Record Id"), null, TeleonomeConstants.DATATYPE_STRING, true));
			words.put(DenomeUtils.buildDeneWordJSONObject("Record File", pointer.optString("Record File"), null, TeleonomeConstants.DATATYPE_STRING, true));
			words.put(DenomeUtils.buildDeneWordJSONObject("Record Host", hostName(), null, TeleonomeConstants.DATATYPE_STRING, true));
			denes.put(dene);
			JSONObject chain = new JSONObject();
			chain.put(TeleonomeConstants.DENE_DENE_NAME_ATTRIBUTE, FIELD_RECORDS_CHAIN);
			chain.put("Denes", denes);
			if (existing != null) aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_PURPOSE, FIELD_RECORDS_CHAIN);
			aDenomeManager.injectDeneChainIntoNucleus(TeleonomeConstants.NUCLEI_PURPOSE, chain);
		} catch (Exception e) {
			// the record is on disk either way; the pointer is added when the next visit of this plant comes in
			logger.warn("could not add " + recordName + " to " + FIELD_RECORDS_CHAIN + ": " + Utils.getStringException(e));
		}
	}

	private static String hostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName() + ".local";
		} catch (Exception e) {
			return "localhost";
		}
	}
}
