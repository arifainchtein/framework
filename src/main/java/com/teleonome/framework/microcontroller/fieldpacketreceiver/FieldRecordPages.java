package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

/**
 * Read-only pages for the field records (2026-11-27), served by FieldPacketListener:
 *   GET /fieldrecords/                 every plant: last visit, last readings, how many visits
 *   GET /fieldrecords/<record id>/     one plant's timeline, newest visit first: readings, note, photos, analyses
 *   GET /fieldrecords/<record id>/<file>   a photo, or record.json
 * Nothing here changes anything. Like the Teleonome's own pages they are open on the local network; there is no
 * code check (codes go through Annabelle's ESP32, which belongs to the Async Cycle).
 */
public class FieldRecordPages {

	private static final String[][] VALUES = {
		{"moist", "Moisture %", "1"}, {"temp", "Soil &deg;C", "1"}, {"ph", "pH", "1"}, {"ec", "EC uS/cm", "0"},
		{"n", "N*", "0"}, {"p", "P*", "0"}, {"k", "K*", "0"}, {"salt", "Salt", "0"}};

	private static final String STYLE = "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
			+ "<style>:root{--bg:#fff;--fg:#212529;--muted:#6c757d;--line:#dee2e6;--card:#f8f9fa;--link:#2a670e}"
			+ "@media(prefers-color-scheme:dark){:root{--bg:#1b1d1f;--fg:#e9ecef;--muted:#adb5bd;--line:#3a3f44;--card:#24272a;--link:#8fd16a}}"
			+ "body{font-family:system-ui,sans-serif;margin:0 auto;max-width:900px;padding:16px;background:var(--bg);color:var(--fg)}"
			+ "a{color:var(--link)}h1{font-size:22px}h2{font-size:18px;margin:0 0 6px}.m{color:var(--muted);font-size:13px}"
			+ ".card{border:1px solid var(--line);border-radius:10px;padding:12px;margin:12px 0;background:var(--card)}"
			+ "table{border-collapse:collapse;font-size:14px;margin:6px 0;width:100%;overflow-x:auto;display:block}"
			+ "td,th{border:1px solid var(--line);padding:4px 6px;text-align:right;white-space:nowrap}th:first-child,td:first-child{text-align:left}"
			+ ".ph{display:flex;flex-wrap:wrap;gap:6px;margin:8px 0}.ph img{width:140px;height:140px;object-fit:cover;border-radius:8px}"
			+ ".note{white-space:pre-wrap}.an{white-space:pre-wrap;border-left:3px solid var(--link);padding:6px 10px;margin:8px 0;background:var(--bg)}"
			+ "</style>";

	private final FieldPacketSettings settings;

	public FieldRecordPages(FieldPacketSettings s) {
		settings = s;
	}

	public void handle(HttpExchange ex) throws IOException {
		String path = ex.getRequestURI().getPath().substring("/fieldrecords".length());
		if (path.isEmpty()) {
			redirect(ex, "/fieldrecords/");
			return;
		}
		String[] parts = path.replaceAll("^/+", "").split("/");
		if (path.equals("/")) {
			html(ex, 200, listPage());
			return;
		}
		if (parts.length == 1 && !path.endsWith("/")) {
			redirect(ex, "/fieldrecords/" + parts[0] + "/");
			return;
		}
		if (parts.length == 1) {
			File f = FieldPacketStore.recordFile(settings, parts[0], FieldPacketStore.RECORD_FILE);
			if (f == null) {
				html(ex, 404, "<p>No such record.</p>");
				return;
			}
			html(ex, 200, recordPage(read(f)));
			return;
		}
		File f = parts.length == 2 ? FieldPacketStore.recordFile(settings, parts[0], parts[1]) : null;
		if (f == null) {
			html(ex, 404, "<p>No such file.</p>");
			return;
		}
		ex.getResponseHeaders().set("Content-Type", f.getName().endsWith(".jpg") ? "image/jpeg" : "application/json; charset=utf-8");
		ex.sendResponseHeaders(200, f.length());
		try (OutputStream out = ex.getResponseBody()) {
			Files.copy(f.toPath(), out);
		}
	}

	private String listPage() throws IOException {
		StringBuilder sb = new StringBuilder("<!doctype html><html><head>" + STYLE + "<title>Field Records</title></head><body><h1>Field Records</h1>");
		File[] dirs = settings.plants().listFiles(File::isDirectory);
		if (dirs == null || dirs.length == 0) sb.append("<p>No records yet. Send a visit from PaulaDeployer's Test Soil Sensor.</p>");
		else {
			Arrays.sort(dirs, (a, b) -> Long.compare(new File(b, FieldPacketStore.RECORD_FILE).lastModified(), new File(a, FieldPacketStore.RECORD_FILE).lastModified()));
			for (File d : dirs) {
				File f = new File(d, FieldPacketStore.RECORD_FILE);
				if (!f.isFile()) continue;
				JSONObject r = read(f);
				JSONArray visits = r.optJSONArray("Visits");
				int n = visits == null ? 0 : visits.length();
				JSONObject last = n > 0 ? visits.getJSONObject(n - 1) : null;
				sb.append("<div class=\"card\"><h2><a href=\"").append(esc(d.getName())).append("/\">").append(esc(r.optString("Plant"))).append("</a></h2>")
						.append("<div class=\"m\">").append(n).append(" visit").append(n == 1 ? "" : "s").append(", last ").append(esc(r.optString("Last Updated"))).append("</div>");
				if (last != null) {
					JSONObject reading = lastReading(last);
					if (reading != null) sb.append("<div class=\"m\">").append(summary(reading)).append("</div>");
					if (!last.optString("Note").isEmpty()) sb.append("<div class=\"note\">").append(esc(last.optString("Note"))).append("</div>");
					sb.append(thumbs(d.getName(), last, 4));
				}
				sb.append("</div>");
			}
		}
		return sb.append("</body></html>").toString();
	}

	private String recordPage(JSONObject r) {
		String id = r.optString("Record Id");
		StringBuilder sb = new StringBuilder("<!doctype html><html><head>" + STYLE + "<title>" + esc(r.optString("Plant")) + "</title></head><body>"
				+ "<p class=\"m\"><a href=\"../\">All records</a> &middot; <a href=\"record.json\">record.json</a></p>"
				+ "<h1>" + esc(r.optString("Plant")) + "</h1><p class=\"m\">Record since " + esc(r.optString("Created")) + "</p>");
		JSONArray visits = r.optJSONArray("Visits");
		if (visits == null) visits = new JSONArray();
		// all readings over time, oldest first, so a trend shows at a glance
		boolean any = false;
		StringBuilder table = new StringBuilder("<table><tr><th>Visit</th>");
		for (String[] c : VALUES) table.append("<th>").append(c[1]).append("</th>");
		table.append("</tr>");
		for (int i = 0; i < visits.length(); i++) {
			JSONObject reading = lastReading(visits.getJSONObject(i));
			if (reading == null) continue;
			any = true;
			table.append("<tr><td>").append(esc(shortTime(visits.getJSONObject(i).optString("Visit Time")))).append("</td>");
			for (String[] c : VALUES) table.append("<td>").append(num(reading, c[0], Integer.parseInt(c[2]))).append("</td>");
			table.append("</tr>");
		}
		table.append("</table><div class=\"m\">The last reading of each visit. * N, P, K are estimated by the probe from EC, not measured.</div>");
		if (any) sb.append("<div class=\"card\"><h2>Soil over time</h2>").append(table).append("</div>");

		for (int i = visits.length() - 1; i >= 0; i--) {
			JSONObject v = visits.getJSONObject(i);
			sb.append("<div class=\"card\"><h2>").append(esc(shortTime(v.optString("Visit Time")))).append("</h2>")
					.append("<div class=\"m\">From ").append(esc(v.optString("Sender"))).append(", received ").append(esc(v.optString("Received"))).append("</div>");
			if (!v.optString("Note").isEmpty()) sb.append("<p class=\"note\">").append(esc(v.optString("Note"))).append("</p>");
			JSONArray readings = v.optJSONArray("Readings");
			if (readings != null && readings.length() > 0) {
				sb.append("<table><tr><th>Reading</th>");
				for (String[] c : VALUES) sb.append("<th>").append(c[1]).append("</th>");
				sb.append("</tr>");
				for (int j = 0; j < readings.length(); j++) {
					JSONObject rd = readings.getJSONObject(j);
					sb.append("<tr><td>").append(esc(shortTime(rd.optString("time")))).append("</td>");
					for (String[] c : VALUES) sb.append("<td>").append(num(rd, c[0], Integer.parseInt(c[2]))).append("</td>");
					sb.append("</tr>");
				}
				sb.append("</table>");
			}
			sb.append(thumbs(id, v, 100));
			JSONArray analyses = v.optJSONArray("Analyses");
			if (analyses == null || analyses.length() == 0) {
				sb.append("<div class=\"m\">No analysis yet").append(analysisOn() ? " - it is made after the next Async Cycle." : " (analysis is off: see FieldPackets/analysis.properties).").append("</div>");
			} else {
				for (int j = 0; j < analyses.length(); j++) {
					JSONObject a = analyses.getJSONObject(j);
					sb.append("<div class=\"m\">Analysis ").append(esc(a.optString("Created"))).append(" by ").append(esc(a.optString("Model"))).append("</div>")
							.append("<div class=\"an\">").append(esc(a.optString("Text"))).append("</div>");
				}
			}
			JSONArray follow = v.optJSONArray("Follow Ups");
			if (follow != null && follow.length() > 0) sb.append("<div class=\"m\">Follow ups: ").append(esc(follow.toString())).append("</div>");
			sb.append("</div>");
		}
		return sb.append("</body></html>").toString();
	}

	private boolean analysisOn() {
		return "claude".equalsIgnoreCase(FieldRecordAnalyzer.properties(settings).getProperty("provider", "none").trim());
	}

	private static String thumbs(String recordId, JSONObject visit, int max) {
		JSONArray photos = visit.optJSONArray("Photos");
		if (photos == null || photos.length() == 0) return "";
		StringBuilder sb = new StringBuilder("<div class=\"ph\">");
		for (int i = 0; i < photos.length() && i < max; i++) {
			String href = "/fieldrecords/" + esc(recordId) + "/" + esc(photos.getJSONObject(i).optString("File"));
			sb.append("<a href=\"").append(href).append("\"><img loading=\"lazy\" src=\"").append(href).append("\" alt=\"photo\"></a>");
		}
		return sb.append("</div>").toString();
	}

	private static JSONObject lastReading(JSONObject visit) {
		JSONArray r = visit.optJSONArray("Readings");
		return r == null || r.length() == 0 ? null : r.getJSONObject(r.length() - 1);
	}

	private static String summary(JSONObject r) {
		StringBuilder sb = new StringBuilder();
		for (String[] c : VALUES) sb.append(c[1]).append(" ").append(num(r, c[0], Integer.parseInt(c[2]))).append(" &middot; ");
		return sb.substring(0, sb.length() - " &middot; ".length());
	}

	private static String num(JSONObject r, String key, int decimals) {
		if (!r.has(key)) return "&ndash;";
		return String.format("%." + decimals + "f", r.optDouble(key));
	}

	private static String shortTime(String iso) {
		// "2026-11-27T04:30:06Z" -> "2026-11-27 04:30 UTC"; anything else as it is
		return iso.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d.*Z") ? iso.substring(0, 10) + " " + iso.substring(11, 16) + " UTC" : iso;
	}

	private static JSONObject read(File f) throws IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
		}
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static void redirect(HttpExchange ex, String to) throws IOException {
		ex.getResponseHeaders().set("Location", to);
		ex.sendResponseHeaders(302, -1);
		ex.close();
	}

	private static void html(HttpExchange ex, int code, String body) throws IOException {
		byte[] b = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
		ex.sendResponseHeaders(code, b.length);
		try (OutputStream out = ex.getResponseBody()) {
			out.write(b);
		}
	}
}
