package com.teleonome.framework.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A person stopping one of the organs beside the Hypothalamus (Heart, Cerebellum, Hippocampus) from the Diagnostics
 * page (2026-11-22), the same way "Stop Hypothalamus" works:
 *   - the web page sends the StopOrgan command with the 6-digit code and {"Organ": name}
 *   - the Hypothalamus checks the code, writes &lt;Organ&gt;Stopped.info (Stopped At, Until, Client Ip) and kills the organ
 *   - while that file is there and not past Until, the Medula does not restart the organ
 *   - "Start" on the page deletes the file and runs the organ's start script; past Until the Medula deletes it itself,
 *     so a forgotten stop does not leave the organ off for good
 * Everything here is shared by the Hypothalamus, the Medula and the web app, so they agree on names and paths.
 */
public class OrganStop {

	public static final String HEART = "Heart";
	public static final String CEREBELLUM = "Cerebellum";
	public static final String HIPPOCAMPUS = "Hippocampus";
	public static final String[] ORGANS = { HEART, CEREBELLUM, HIPPOCAMPUS };

	public static boolean isOrgan(String organ) {
		for (String o : ORGANS) if (o.equals(organ)) return true;
		return false;
	}

	/** HeartStopped.info, CerebellumStopped.info, HippocampusStopped.info, in the Teleonome directory */
	public static String stopFileName(String organ) {
		return organ + "Stopped.info";
	}

	/** the jar the organ runs: what pgrep/pkill look for */
	public static String jarName(String organ) {
		return organ + ".jar";
	}

	/** the organ's start script, relative to the Teleonome directory (the Heart lives in heart/) */
	public static String startScript(String organ) {
		return HEART.equals(organ) ? "heart/StartHeartBG.sh" : "Start" + organ + "BG.sh";
	}

	/** the stop file, when there and not past its Until; an expired or unreadable one is deleted and counts as none */
	public static JSONObject readStop(String dir, String organ) {
		File f = new File(dir + stopFileName(organ));
		if (!f.isFile()) return null;
		try {
			JSONObject stop = new JSONObject(FileUtils.readFileToString(f, StandardCharsets.UTF_8));
			if (System.currentTimeMillis() < stop.optLong("Until", 0)) return stop;
		} catch (IOException | JSONException e) {
			// unreadable: no stop
		}
		FileUtils.deleteQuietly(f);
		return null;
	}

	public static boolean isStopped(String dir, String organ) {
		return readStop(dir, organ) != null;
	}

	public static JSONObject writeStop(String dir, String organ, int minutes, String clientIp) throws IOException, JSONException {
		long now = System.currentTimeMillis(), until = now + minutes * 60000L;
		JSONObject stop = new JSONObject();
		stop.put("Organ", organ);
		stop.put("Stopped At", now);
		stop.put("Until", until);
		stop.put("Until Text", new java.util.Date(until).toString());
		stop.put("Client Ip", clientIp == null ? "" : clientIp);
		stop.put("By", "Stop " + organ);
		FileUtils.writeStringToFile(new File(dir + stopFileName(organ)), stop.toString(), StandardCharsets.UTF_8);
		return stop;
	}

	/** whether a process running the organ's jar is alive here (reads /proc, so no shell is needed) */
	public static boolean isRunning(String organ) {
		String jar = jarName(organ);
		File[] procs = new File("/proc").listFiles((d, n) -> n.matches("\\d+"));
		if (procs == null) return false;
		for (File p : procs) {
			try {
				byte[] cmd = FileUtils.readFileToByteArray(new File(p, "cmdline"));
				// arguments are separated by NUL: look for the jar as a whole argument or a path ending in it
				for (String arg : new String(cmd, StandardCharsets.UTF_8).split("\u0000")) {
					if (arg.equals(jar) || arg.endsWith("/" + jar)) return true;
				}
			} catch (IOException e) {
				// the process ended while we looked
			}
		}
		return false;
	}
}
