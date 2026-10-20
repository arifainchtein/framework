package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * The doorbell's home end (design: OpenWrtOne/documents/07-network-rules-design.md, section 3).
 *
 * Every Poll Seconds (10) it reads Cleotilde's outbox on Zhinu (chilhuacle) by SFTP:
 *   Teleonome/Mailbox/Cleotilde.json            {"Updated", "Messages": [{"Id", "Kind": "Ring", "Time",
 *                                               "Services", "Minutes", "Code"}]}, written by Zhinu
 * asks the board about each new ring's code, and answers in
 *   Teleonome/Mailbox/Cleotilde.delivered.json  {"Updated", "Messages": [{"Id", "Time", "Services",
 *                                               "Minutes", "Received", "Result": valid|invalid|expired}]}
 * which Zhinu's Mailbox page shows. 10 s instead of the pulse (about 70 s) was Ari's choice on
 * 2026-10-20; with a pulse, a ring reached the board 1-2 minutes late.
 *
 * Zhinu is treated as untrusted: it never sees whether a code is right until the board has used it,
 * and the board accepts each earlier 30 s step only once. A ring older than RING_MAX_AGE_MILLIS is
 * answered "expired" without asking the board, so old or replayed rings don't count towards the
 * board's lockout (5 wrong codes = locked for an hour). Answered ids are kept in DoorbellRings.json
 * so a restart doesn't ask the board again about a code it has already used.
 *
 * The lock (added 2026-10-20): after 5 wrong codes in an hour the board refuses every code. Both
 * files carry "Locked" and "WrongCodesLastHour" (from the board's GetSensorData, so up to a pulse
 * late), and the Release button on Cleotilde's page (home network only, no code: the locked board
 * would refuse it) leaves RELEASE_FLAG_FILE, which the next round turns into ResetCodeLockout.
 * Zhinu has no password in front of the doorbell: a stranger can at worst lock it until Ari releases it.
 *
 * The session stays open between polls and is opened again after any failure.
 */
public class DoorbellPoller extends Thread {

	public static final String LOCAL_FILE = "DoorbellRings.json";
	/** Written by the Cleotilde web app's ReleaseDoorbellLock form, in the same working directory. */
	public static final String RELEASE_FLAG_FILE = "DoorbellReleaseLock";
	static final String RESULT_VALID = "valid", RESULT_INVALID = "invalid", RESULT_EXPIRED = "expired";
	/** The board accepts a code from the last 3 minutes, counted when Cleotilde receives it. */
	private static final long RING_MAX_AGE_MILLIS = 3 * 60_000L;
	/** A ring dated this far in the future means one of the clocks is wrong. */
	private static final long RING_MAX_FUTURE_MILLIS = 60_000L;
	private static final int MAX_RINGS_PER_POLL = 3;
	private static final int RESULTS_KEPT = 20;
	private static final int TIMEOUT_MILLIS = 20_000;

	private final Logger logger = Logger.getLogger(getClass());
	private final CleotildeMicroController controller;
	private final String host, user, keyFile, remoteDirectory, teleonomeName;
	private final long pollMillis;

	private final JSch jsch = new JSch();
	private Session session;
	private ChannelSftp sftp;
	private boolean identityAdded = false;
	private long lastOutboxUpdated = -1;
	private boolean failureLogged = false;
	/** The last RESULTS_KEPT answers, oldest first; also the list of ids already answered. */
	private JSONArray results = new JSONArray();
	private long lastReleaseMillis = 0;
	private String lastReleaseResult = "";
	/** The status last written, to write the files again only when it changes. */
	private String lastStatus = "";

	DoorbellPoller(CleotildeMicroController controller, String host, String user, String keyFile,
			String remoteDirectory, String teleonomeName, int pollSeconds) {
		super("DoorbellPoller");
		setDaemon(true);
		this.controller = controller;
		this.host = host;
		this.user = user;
		this.keyFile = keyFile;
		this.remoteDirectory = remoteDirectory.endsWith("/") ? remoteDirectory : remoteDirectory + "/";
		this.teleonomeName = teleonomeName;
		this.pollMillis = pollSeconds * 1000L;
		loadResults();
	}

	@Override
	public void run() {
		logger.info("Doorbell poller started, " + user + "@" + host + ":" + remoteDirectory + teleonomeName + ".json every " + pollMillis / 1000 + " s");
		while (!isInterrupted()) {
			releaseIfAsked();  // before the network: it must work while chilhuacle is unreachable
			try {
				pollOnce();
				if (failureLogged) logger.info("Doorbell poller reaches " + host + " again");
				failureLogged = false;
			} catch (JSchException | SftpException | IOException | JSONException e) {
				// logged once per outage, not every 10 s
				if (!failureLogged) logger.warn("Doorbell poll failed, will keep trying: " + e);
				failureLogged = true;
				disconnect();
			}
			try {
				Thread.sleep(pollMillis);
			} catch (InterruptedException e) {
				break;
			}
		}
		disconnect();
	}

	private void releaseIfAsked() {
		File flag = new File(RELEASE_FLAG_FILE);
		if (!flag.exists()) return;
		if (!flag.delete()) logger.warn("Could not delete " + RELEASE_FLAG_FILE);
		lastReleaseResult = controller.releaseCodeLock();
		lastReleaseMillis = System.currentTimeMillis();
		logger.info("Doorbell lock released from Cleotilde's page: " + lastReleaseResult);
	}

	private JSONObject status() {
		return new JSONObject()
				.put("Locked", controller.isCodeLockedOut())
				.put("WrongCodesLastHour", controller.getCodeFailuresLastHour())
				.put("LastRelease", lastReleaseMillis)
				.put("LastReleaseResult", lastReleaseResult);
	}

	private void pollOnce() throws JSchException, SftpException, IOException {
		connect();
		boolean answered = answerNewRings();
		String status = status().toString();
		if (answered || !status.equals(lastStatus)) {
			saveResults();
			uploadResults();
			lastStatus = status;
		}
	}

	/** True if a ring got its answer this round. */
	private boolean answerNewRings() throws SftpException {
		JSONObject outbox;
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			sftp.get(remoteDirectory + teleonomeName + ".json", bytes);
			outbox = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
		} catch (SftpException e) {
			if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) return false;  // nobody has rung yet
			throw e;
		}
		long updated = outbox.optLong("Updated", 0);
		if (updated == lastOutboxUpdated) return false;

		JSONArray messages = outbox.optJSONArray("Messages");
		boolean answered = false;
		int asked = 0;
		for (int i = 0; messages != null && i < messages.length() && asked < MAX_RINGS_PER_POLL; i++) {
			JSONObject m = messages.getJSONObject(i);
			String id = m.optString("Id");
			if (!"Ring".equals(m.optString("Kind")) || id.isEmpty() || isAnswered(id)) continue;
			JSONObject r = answer(m);
			if (r == null) break;  // the board didn't answer: try again next poll, until the ring expires
			results.put(r);
			answered = true;
			if (!RESULT_EXPIRED.equals(r.getString("Result"))) asked++;
		}
		while (results.length() > RESULTS_KEPT) results.remove(0);
		// only once everything is answered, so a ring left over by MAX_RINGS_PER_POLL is seen next time
		boolean pending = false;
		for (int i = 0; messages != null && i < messages.length(); i++) {
			JSONObject m = messages.getJSONObject(i);
			if ("Ring".equals(m.optString("Kind")) && !isAnswered(m.optString("Id"))) pending = true;
		}
		if (!pending) lastOutboxUpdated = updated;
		return answered;
	}

	/** The answer to one ring, or null if the board could not be asked. */
	private JSONObject answer(JSONObject ring) {
		long now = System.currentTimeMillis();
		long age = now - ring.optLong("Time", 0);
		JSONObject r = new JSONObject();
		r.put("Id", ring.optString("Id"));
		r.put("Time", ring.optLong("Time", 0));
		r.put("Services", ring.optJSONArray("Services") == null ? new JSONArray() : ring.getJSONArray("Services"));
		r.put("Minutes", ring.optInt("Minutes", 0));
		r.put("Received", now);
		String result;
		if (age > RING_MAX_AGE_MILLIS || age < -RING_MAX_FUTURE_MILLIS) {
			result = RESULT_EXPIRED;
		} else {
			Boolean valid = controller.askBoardToVerify(ring.optString("Code"));
			if (valid == null) {
				logger.warn("Doorbell ring " + r.getString("Id") + ": no answer from the board, will ask again");
				return null;
			}
			result = valid ? RESULT_VALID : RESULT_INVALID;
		}
		r.put("Result", result);
		logger.info("Doorbell ring " + r.getString("Id") + " services=" + r.get("Services") + " minutes=" + r.get("Minutes")
				+ " age=" + age / 1000 + " s -> " + result);
		controller.doorbellAnswered(r);
		return r;
	}

	private boolean isAnswered(String id) {
		for (int i = 0; i < results.length(); i++) {
			if (id.equals(results.getJSONObject(i).optString("Id"))) return true;
		}
		return false;
	}

	private void connect() throws JSchException {
		if (session != null && session.isConnected() && sftp != null && sftp.isConnected()) return;
		disconnect();
		if (!identityAdded) {
			jsch.addIdentity(keyFile);
			identityAdded = true;
		}
		session = jsch.getSession(user, host, 22);
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");  // as SFTPPublisherWriter; Zhinu is untrusted anyway
		session.setConfig(config);
		session.setTimeout(TIMEOUT_MILLIS);
		session.setServerAliveInterval(15_000);
		session.setServerAliveCountMax(2);
		session.connect(TIMEOUT_MILLIS);
		sftp = (ChannelSftp) session.openChannel("sftp");
		sftp.connect(TIMEOUT_MILLIS);
	}

	private void disconnect() {
		if (sftp != null) sftp.disconnect();
		if (session != null) session.disconnect();
		sftp = null;
		session = null;
	}

	/** Written to a temporary name and renamed, so Zhinu never reads half a file. */
	private void uploadResults() throws SftpException {
		String target = remoteDirectory + teleonomeName + ".delivered.json";
		String tmp = remoteDirectory + "." + teleonomeName + ".delivered.json.tmp";
		byte[] data = new JSONObject().put("Updated", System.currentTimeMillis()).put("Messages", results)
				.put("Status", status()).toString().getBytes(StandardCharsets.UTF_8);
		sftp.put(new ByteArrayInputStream(data), tmp, ChannelSftp.OVERWRITE);
		try {
			sftp.rename(tmp, target);  // posix-rename@openssh.com: replaces the old file
		} catch (SftpException e) {
			sftp.rm(target);
			sftp.rename(tmp, target);
		}
	}

	private void loadResults() {
		File f = new File(LOCAL_FILE);
		if (!f.isFile()) return;
		try {
			results = new JSONObject(FileUtils.readFileToString(f, StandardCharsets.UTF_8)).getJSONArray("Messages");
		} catch (IOException | JSONException e) {
			logger.warn("Could not read " + LOCAL_FILE + ", starting with no answered rings: " + e.getMessage());
		}
	}

	private void saveResults() {
		try {
			FileUtils.writeStringToFile(new File(LOCAL_FILE), new JSONObject().put("Messages", results)
					.put("Status", status()).toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.warn("Could not write " + LOCAL_FILE + ": " + e.getMessage());
		}
	}
}
