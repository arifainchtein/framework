package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;

/**
 * The field records' pages (2026-11-27), served by FieldPacketListener and shown in the Teleonome's Home page pills:
 *   GET /fieldrecords/                       areas, Arrivals, plants and beds in no area, "+ Area"
 *   GET /fieldrecords/area/<area>/           one area: its plants and beds; rename / delete the area
 *   GET /fieldrecords/arrivals/              visits waiting to be filed, each with a File form
 *   GET /fieldrecords/arrivals/<id>/<photo>
 *   GET /fieldrecords/new-area               just the "+ Area" form (the pill of that name)
 *   GET /fieldrecords/subjects.json          {"Areas": [..], "Subjects": [{"Record Id","Name","Type","Area","Visits","Last Visit"}]}
 *                                            - Paula's "For" list
 *   GET /fieldrecords/<record id>/           one plant or bed: readings over time, its visits, photos, analyses; Move
 *   GET /fieldrecords/<record id>/<file>     a photo, or record.json
 * Reading is open on the local network, like the Teleonome's pages. Every change is an action (FieldRecordActions)
 * posted to /fieldaction with the 6-digit code, carried out at the next Async Cycle - the pages show it as waiting,
 * then reload.
 */
public class FieldRecordPages {

	private static final String[][] VALUES = {
		{"moist", "Moisture %", "1"}, {"temp", "Soil &deg;C", "1"}, {"ph", "pH", "1"}, {"ec", "EC uS/cm", "0"},
		{"n", "N*", "0"}, {"p", "P*", "0"}, {"k", "K*", "0"}, {"salt", "Salt", "0"}};

	private static final String STYLE = "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
			+ "<style>:root{--bg:#fff;--fg:#212529;--muted:#6c757d;--line:#dee2e6;--card:#f8f9fa;--link:#2a670e;--warn:#b45309}"
			+ "@media(prefers-color-scheme:dark){:root{--bg:#1b1d1f;--fg:#e9ecef;--muted:#adb5bd;--line:#3a3f44;--card:#24272a;--link:#8fd16a;--warn:#f59f00}}"
			+ "body{font-family:system-ui,sans-serif;margin:0 auto;max-width:900px;padding:16px;background:var(--bg);color:var(--fg)}"
			+ "a{color:var(--link)}h1{font-size:22px;margin:4px 0 10px}h2{font-size:18px;margin:0 0 6px}.m{color:var(--muted);font-size:13px}"
			+ ".card{border:1px solid var(--line);border-radius:10px;padding:12px;margin:12px 0;background:var(--card)}"
			+ ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:10px}.grid .card{margin:0}"
			+ "table{border-collapse:collapse;font-size:14px;margin:6px 0;width:100%;overflow-x:auto;display:block}"
			+ "td,th{border:1px solid var(--line);padding:4px 6px;text-align:right;white-space:nowrap}th:first-child,td:first-child{text-align:left}"
			+ ".ph{display:flex;flex-wrap:wrap;gap:6px;margin:8px 0}.ph img{width:140px;height:140px;object-fit:cover;border-radius:8px}"
			+ ".note{white-space:pre-wrap}.an{white-space:pre-wrap;border-left:3px solid var(--link);padding:6px 10px;margin:8px 0;background:var(--bg)}"
			+ "form{display:flex;flex-wrap:wrap;gap:6px;align-items:center;margin:8px 0}input,select,button{font:inherit;padding:6px 8px;border-radius:6px;border:1px solid var(--line);background:var(--bg);color:var(--fg)}"
			+ "button{background:var(--link);color:#fff;border:0;cursor:pointer}button.sec{background:transparent;color:var(--fg);border:1px solid var(--line)}"
			+ ".badge{display:inline-block;min-width:20px;padding:1px 7px;border-radius:10px;background:var(--warn);color:#fff;font-size:13px;text-align:center}"
			+ ".msg{font-size:13px;color:var(--warn)}.msg:empty{display:none}nav{font-size:14px;margin-bottom:6px}"
			+ "</style>";

	// Every form here: fields + a code -> POST /fieldaction, then wait for the Async Cycle and reload.
	private static final String SCRIPT = "<script>"
			+ "function frCode(){var c=document.getElementById('frCode');var v=c?c.value.trim():'';if(!/^\\d{6}$/.test(v)){alert('Type the 6-digit code first (at the top of the page).');return null;}"
			+ "try{sessionStorage.setItem('frCode',v)}catch(e){}return v;}"
			+ "function frAct(action,msgId){var code=frCode();if(!code)return false;var m=document.getElementById(msgId);if(m)m.textContent='Sending...';"
			+ "fetch('/fieldaction',{method:'POST',headers:{'X-Totp-Code':code,'Content-Type':'application/json'},body:JSON.stringify(action)})"
			+ ".then(function(r){return r.json()}).then(function(j){if(j.Status!=='Received'){if(m)m.textContent='Not sent: '+(j.Reason||j.Status);return;}"
			+ "if(m)m.textContent='Waiting for the next Async Cycle (Annabelle checks the code, then does it)...';frWait(j['Action Id'],m,0);})"
			+ ".catch(function(){if(m)m.textContent='Could not reach the receiver.'});return false;}"
			+ "function frWait(id,m,n){setTimeout(function(){fetch('/fieldaction/'+id).then(function(r){return r.json()}).then(function(s){"
			+ "if(s.State==='Done'){if(m)m.textContent=s.Result+' - reloading';setTimeout(function(){location.reload()},800);}"
			+ "else if(s.State==='Rejected'){if(m)m.textContent='Not done: '+(s.Reason||'refused');}"
			+ "else if(n<120){frWait(id,m,n+1);}else if(m){m.textContent='Still waiting - is the Hypothalamus running?';}})},5000);}"
			+ "document.addEventListener('DOMContentLoaded',function(){var c=document.getElementById('frCode');try{var v=sessionStorage.getItem('frCode');if(c&&v)c.value=v}catch(e){}});"
			+ "</script>";

	private static final String CODE_BOX = "<form onsubmit=\"return false\"><label class=\"m\" for=\"frCode\">6-digit code (for changes)</label>"
			+ "<input id=\"frCode\" inputmode=\"numeric\" maxlength=\"6\" size=\"7\" autocomplete=\"one-time-code\"></form>";

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
			html(ex, 200, indexPage());
			return;
		}
		if (path.equals("/subjects.json")) {
			json(ex, subjectsJson());
			return;
		}
		if (path.equals("/analysis")) {
			html(ex, 200, analysisPage());
			return;
		}
		if (path.equals("/new-area")) {
			html(ex, 200, page("+ Area", "<h1>New area</h1>" + CODE_BOX + newAreaForm()));
			return;
		}
		if (parts[0].equals("area") && parts.length >= 2) {
			html(ex, 200, areaPage(parts[1]));
			return;
		}
		if (parts[0].equals("arrivals")) {
			if (parts.length == 1) {
				html(ex, 200, arrivalsPage());
				return;
			}
			File d = FieldSubjects.arrivalDir(settings, parts[1]);
			if (parts.length == 3 && d != null && parts[2].matches("[A-Za-z0-9_.-]+\\.jpg") && new File(d, parts[2]).isFile()) {
				file(ex, new File(d, parts[2]));
				return;
			}
			html(ex, 404, "<p>No such arrival.</p>");
			return;
		}
		if (parts.length == 1 && !path.endsWith("/")) {
			redirect(ex, "/fieldrecords/" + parts[0] + "/");
			return;
		}
		if (parts.length == 1) {
			File f = FieldSubjects.recordFile(settings, parts[0]);
			if (f == null) {
				html(ex, 404, "<p>No such plant or bed.</p>");
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
		file(ex, f);
	}

	// ---------------------------------------------------------------- pages

	private String indexPage() throws IOException {
		StringBuilder sb = new StringBuilder("<h1>Field Records</h1>");
		List<JSONObject> arrivals = FieldSubjects.arrivals(settings);
		sb.append("<div class=\"card\"><h2><a href=\"arrivals/\">Arrivals</a> ").append(arrivals.isEmpty() ? "" : "<span class=\"badge\">" + arrivals.size() + "</span>")
				.append("</h2><div class=\"m\">").append(arrivals.isEmpty() ? "Nothing waiting to be filed." : "Visits waiting to be filed into a plant or bed.").append("</div></div>");
		JSONArray areas = FieldSubjects.areas(settings);
		List<JSONObject> subjects = FieldSubjects.subjects(settings);
		sb.append("<h2>Areas</h2><div class=\"grid\">");
		for (int i = 0; i < areas.length(); i++) {
			String a = areas.getJSONObject(i).optString("Name");
			int n = 0;
			for (JSONObject r : subjects) if (a.equals(r.optString("Area"))) n++;
			sb.append("<div class=\"card\"><h2><a href=\"area/").append(enc(a)).append("/\">").append(esc(a)).append("</a></h2><div class=\"m\">")
					.append(n).append(n == 1 ? " plant or bed" : " plants and beds").append("</div></div>");
		}
		if (areas.length() == 0) sb.append("<div class=\"m\">No areas yet. Make one below - the Orangerie, the Greenhouse, the Potager...</div>");
		sb.append("</div>");
		StringBuilder none = new StringBuilder();
		for (JSONObject r : subjects) if (r.optString("Area").isEmpty()) none.append(subjectCard(r));
		if (none.length() > 0) sb.append("<h2 style=\"margin-top:16px\">Not in an area</h2><div class=\"m\">Open one and use Move to give it an area.</div>").append(none);
		sb.append("<div class=\"card\"><h2><a href=\"analysis\">Analysis</a></h2><div class=\"m\">Claude's interpretation of each visit: API key, model, on/off.</div></div>");
		sb.append("<div class=\"card\"><h2>+ Area</h2>").append(CODE_BOX).append(newAreaForm()).append("</div>");
		return page("Field Records", sb.toString());
	}

	private String newAreaForm() {
		return "<form onsubmit=\"return frAct({Action:'createArea',Name:this.n.value},'frNewArea')\"><input name=\"n\" placeholder=\"e.g. Orangerie\" maxlength=\"40\" required>"
				+ "<button>Create area</button></form><div id=\"frNewArea\" class=\"msg\"></div>";
	}

	private String areaPage(String area) throws IOException {
		if (!FieldSubjects.areaExists(settings, area)) return page("No such area", "<nav><a href=\"/fieldrecords/\">Field Records</a></nav><p>There is no area " + esc(area) + ".</p>");
		StringBuilder sb = new StringBuilder("<nav><a href=\"/fieldrecords/\">Field Records</a></nav><h1>" + esc(area) + "</h1>");
		int n = 0;
		for (JSONObject r : FieldSubjects.subjects(settings)) {
			if (!area.equals(r.optString("Area"))) continue;
			sb.append(subjectCard(r));
			n++;
		}
		if (n == 0) sb.append("<p class=\"m\">No plants or beds here yet. File one from <a href=\"/fieldrecords/arrivals/\">Arrivals</a>, or Move one here from its page.</p>");
		String a = jsString(area);
		sb.append("<div class=\"card\"><h2>Change this area</h2>").append(CODE_BOX)
				.append("<form onsubmit=\"return frAct({Action:'renameArea',Name:").append(a).append(",'New Name':this.n.value},'frArea')\"><input name=\"n\" value=\"").append(esc(area)).append("\" maxlength=\"40\"><button>Rename</button></form>")
				.append("<form onsubmit=\"return confirm('Delete the area? Its plants and beds stay, with no area.') && frAct({Action:'deleteArea',Name:").append(a).append("},'frArea')\"><button class=\"sec\">Delete area</button></form>")
				.append("<div id=\"frArea\" class=\"msg\"></div></div>");
		return page(area, sb.toString());
	}

	private String arrivalsPage() throws IOException {
		StringBuilder sb = new StringBuilder("<nav><a href=\"/fieldrecords/\">Field Records</a></nav><h1>Arrivals</h1>");
		List<JSONObject> arrivals = FieldSubjects.arrivals(settings);
		if (arrivals.isEmpty()) return page("Arrivals", sb.append("<p class=\"m\">Nothing waiting. Visits for a plant or bed Annabelle already knows go straight to it.</p>").toString());
		sb.append("<p class=\"m\">Each visit below is waiting for you to say which plant or bed it is for.</p>").append(CODE_BOX);
		JSONArray areas = FieldSubjects.areas(settings);
		List<JSONObject> subjects = FieldSubjects.subjects(settings);
		for (JSONObject arr : arrivals) {
			String id = arr.optString("Arrival Id");
			JSONObject v = arr.optJSONObject("Visit"), sug = arr.optJSONObject("Suggested");
			if (v == null) continue;
			if (sug == null) sug = new JSONObject();
			sb.append("<div class=\"card\"><h2>").append(esc(sug.optString("Name", "(no name)"))).append(" <span class=\"m\">").append(esc(sug.optString("Type"))).append("</span></h2>")
					.append("<div class=\"m\">").append(esc(shortTime(v.optString("Visit Time")))).append(" from ").append(esc(v.optString("Sender")))
					.append(sug.optString("Area").isEmpty() ? "" : ", suggested area " + esc(sug.optString("Area"))).append("</div>");
			if (!v.optString("Note").isEmpty()) sb.append("<p class=\"note\">").append(esc(v.optString("Note"))).append("</p>");
			JSONObject reading = lastReading(v);
			if (reading != null) sb.append("<div class=\"m\">").append(summary(reading)).append("</div>");
			JSONArray photos = v.optJSONArray("Photos");
			if (photos != null && photos.length() > 0) {
				sb.append("<div class=\"ph\">");
				for (int i = 0; i < photos.length(); i++) {
					String href = "/fieldrecords/arrivals/" + enc(id) + "/" + enc(photos.getJSONObject(i).optString("File"));
					sb.append("<a href=\"").append(href).append("\"><img loading=\"lazy\" src=\"").append(href).append("\" alt=\"photo\"></a>");
				}
				sb.append("</div>");
			}
			String msg = "frArr" + Math.abs(id.hashCode()), jid = jsString(id);
			// into an existing plant or bed
			sb.append("<form onsubmit=\"return frAct({Action:'fileArrival','Arrival Id':").append(jid).append(",'Record Id':this.r.value},'").append(msg).append("')\">")
					.append("<select name=\"r\" required><option value=\"\">An existing plant or bed...</option>");
			for (JSONObject r : subjects) {
				String rid = r.optString("Record Id");
				boolean sel = rid.equals(sug.optString("Record Id")) || rid.equals(FieldPacketStore.recordId(sug.optString("Name", "")));
				sb.append("<option value=\"").append(esc(rid)).append("\"").append(sel ? " selected" : "").append(">").append(esc(r.optString("Plant")))
						.append(r.optString("Area").isEmpty() ? "" : " (" + esc(r.optString("Area")) + ")").append("</option>");
			}
			sb.append("</select><button>File</button></form>");
			// or a new one
			sb.append("<form onsubmit=\"return frAct({Action:'fileArrival','Arrival Id':").append(jid).append(",Name:this.n.value,Type:this.t.value,Area:this.a.value},'").append(msg).append("')\">")
					.append("<input name=\"n\" value=\"").append(esc(sug.optString("Name"))).append("\" placeholder=\"New plant or bed\" required>")
					.append("<select name=\"t\"><option").append(FieldSubjects.TYPE_BED.equals(sug.optString("Type")) ? "" : " selected").append(">Plant</option><option")
					.append(FieldSubjects.TYPE_BED.equals(sug.optString("Type")) ? " selected" : "").append(">Bed</option></select>")
					.append(areaSelect("a", sug.optString("Area"), areas)).append("<button>File as new</button></form>")
					.append("<form onsubmit=\"return confirm('Discard this visit? It is kept in discarded/, not deleted.') && frAct({Action:'discardArrival','Arrival Id':").append(jid).append("},'").append(msg).append("')\"><button class=\"sec\">Discard</button></form>")
					.append("<div id=\"").append(msg).append("\" class=\"msg\"></div></div>");
		}
		return page("Arrivals", sb.toString());
	}

	private String recordPage(JSONObject r) throws IOException {
		String id = r.optString("Record Id"), area = r.optString("Area");
		StringBuilder sb = new StringBuilder("<nav><a href=\"/fieldrecords/\">Field Records</a>"
				+ (area.isEmpty() ? "" : " &rsaquo; <a href=\"/fieldrecords/area/" + enc(area) + "/\">" + esc(area) + "</a>")
				+ " &middot; <a href=\"record.json\">record.json</a></nav>"
				+ "<h1>" + esc(r.optString("Plant")) + " <span class=\"m\">" + esc(r.optString("Record Type", "Plant")) + "</span></h1>"
				+ "<p class=\"m\">" + (area.isEmpty() ? "Not in an area" : "In " + esc(area)) + " &middot; record since " + esc(r.optString("Created")) + "</p>");
		JSONArray visits = r.optJSONArray("Visits");
		if (visits == null) visits = new JSONArray();
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
			sb.append("</div>");
		}
		sb.append("<div class=\"card\"><h2>Move</h2>").append(CODE_BOX)
				.append("<form onsubmit=\"return frAct({Action:'moveSubject','Record Id':").append(jsString(id)).append(",Area:this.a.value},'frMove')\">")
				.append(areaSelect("a", area, FieldSubjects.areas(settings))).append("<button>Move</button></form><div id=\"frMove\" class=\"msg\"></div></div>");
		return page(r.optString("Plant"), sb.toString());
	}

	// Analysis (2026-11-27): paste the Anthropic API key, pick model and effort, on/off - a setAnalysis action. The
	// key never comes back to the page: only whether one is saved and its last 4 characters.
	private String analysisPage() throws IOException {
		java.util.Properties p = new java.util.Properties();
		File pf = new File(settings.root, "analysis.properties");
		if (pf.isFile()) {
			try (java.io.InputStream in = new java.io.FileInputStream(pf)) {
				p.load(in);
			}
		}
		boolean on = "claude".equalsIgnoreCase(p.getProperty("provider", "none").trim());
		String model = p.getProperty("model", FieldRecordActions.MODELS[0]).trim(), effort = p.getProperty("effort", "high").trim();
		File keyFile = new File(p.getProperty("apiKeyFile", FieldRecordActions.keyFile(settings).getAbsolutePath()).trim());
		String keyText = "none saved";
		if (keyFile.isFile()) {
			String k = new String(Files.readAllBytes(keyFile.toPath()), StandardCharsets.UTF_8).trim();
			keyText = "saved, ends in &hellip;" + esc(k.length() > 4 ? k.substring(k.length() - 4) : "");
		}
		StringBuilder sb = new StringBuilder("<nav><a href=\"/fieldrecords/\">Field Records</a></nav><h1>Analysis</h1>");
		sb.append("<div class=\"card\"><h2>Now</h2><div>Analysis is <strong>").append(on ? "on" : "off").append("</strong>")
				.append(on ? " &middot; " + esc(model) + ", effort " + esc(effort) : "").append("</div><div class=\"m\">API key: ").append(keyText).append("</div>");
		File st = new File(settings.root, "analysis-status.json");
		if (st.isFile()) {
			JSONObject j = read(st);
			sb.append("<div class=\"").append(j.optBoolean("Ok") ? "m" : "msg").append("\">Last: ").append(esc(j.optString("Text"))).append(" (").append(esc(j.optString("At"))).append(")</div>");
		}
		sb.append("<div class=\"m\">Each filed visit that has no analysis yet is sent to Claude after an Async Cycle: its photos (up to 6), readings, note and the plant's earlier visits.</div></div>");
		// the estimated balance (AnalysisSpend)
		JSONObject spend = AnalysisSpend.read(settings);
		java.math.BigDecimal credits = AnalysisSpend.credits(spend), left = AnalysisSpend.balance(spend);
		sb.append("<div class=\"card\"><h2>Credits</h2>");
		if (spend.getJSONArray("Credits").length() == 0) {
			sb.append("<div class=\"m\">No credits entered yet. Add what you bought in the Console (Settings &rarr; Billing), and the page keeps an estimate of what is left.</div>");
		} else {
			sb.append("<div style=\"font-size:28px;font-weight:700\"").append(left.signum() <= 0 ? " class=\"msg\"" : "").append(">about $").append(left.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(" left</div>")
					.append("<div class=\"m\">$").append(credits.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(" in credits &minus; $")
					.append(new java.math.BigDecimal(spend.optString("Spent", "0")).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()).append(" spent on ")
					.append(spend.optInt("Analyses")).append(spend.optInt("Analyses") == 1 ? " analysis" : " analyses")
					.append(spend.has("Since") ? " since " + esc(spend.optString("Since")) : "").append("</div>");
			JSONArray recent = spend.getJSONArray("Recent");
			if (recent.length() > 0) {
				sb.append("<table><tr><th>Analysis</th><th>Model</th><th>Cost</th></tr>");
				for (int i = recent.length() - 1; i >= Math.max(0, recent.length() - 10); i--) {
					JSONObject r = recent.getJSONObject(i);
					sb.append("<tr><td>").append(esc(r.optString("At"))).append(" &middot; ").append(esc(r.optString("What"))).append("</td><td>").append(esc(r.optString("Model")))
							.append("</td><td>$").append(esc(r.optString("Cost"))).append("</td></tr>");
				}
				sb.append("</table>");
			}
		}
		sb.append(CODE_BOX)
				.append("<form onsubmit=\"return frAct({Action:'addCredit',Amount:this.a.value,Note:this.n.value},'frCredit')\">$<input name=\"a\" inputmode=\"decimal\" size=\"7\" placeholder=\"10.00\" required>")
				.append("<input name=\"n\" placeholder=\"note (optional)\" maxlength=\"60\"><button>Add credits</button></form>")
				.append("<form onsubmit=\"return frAct({Action:'setBalance',Amount:this.a.value},'frCredit')\">$<input name=\"a\" inputmode=\"decimal\" size=\"7\" required>")
				.append("<button class=\"sec\">Set to the Console's balance</button></form><div id=\"frCredit\" class=\"msg\"></div>")
				.append("<div class=\"m\">An estimate: what each analysis cost (from the tokens the API reports and the list prices) taken off what you entered. It only knows what Annabelle spends with this key - the Console shows the real balance.</div></div>");
		sb.append("<div class=\"card\"><h2>Change</h2>").append(CODE_BOX)
				.append("<form onsubmit=\"return frAct({Action:'setAnalysis',Key:this.k.value,Model:this.m.value,Effort:this.e.value,Enabled:this.o.checked},'frAnalysis')\">")
				.append("<input name=\"k\" type=\"password\" autocomplete=\"off\" placeholder=\"").append(keyFile.isFile() ? "Paste a new key, or leave empty to keep it" : "Paste your Anthropic API key (sk-ant-...)").append("\" style=\"flex:1 1 260px\">")
				.append("<select name=\"m\">");
		for (String m : FieldRecordActions.MODELS) sb.append("<option").append(m.equals(model) ? " selected" : "").append(">").append(m).append("</option>");
		sb.append("</select><select name=\"e\">");
		for (String e : FieldRecordActions.EFFORTS) sb.append("<option").append(e.equals(effort) ? " selected" : "").append(">").append(e).append("</option>");
		sb.append("</select><label class=\"m\"><input name=\"o\" type=\"checkbox\"").append(on || !keyFile.isFile() ? " checked" : "").append("> on</label>")
				.append("<button>Save</button></form><div id=\"frAnalysis\" class=\"msg\"></div>")
				.append("<div class=\"m\">The key is saved on this Pi in /home/pi/Teleonome/secrets/anthropic.key, readable only by the Teleonome. It is never shown again, logged, or put in the denome.</div></div>");
		return page("Analysis", sb.toString());
	}

	private String subjectsJson() throws IOException {
		JSONArray areas = new JSONArray();
		JSONArray a = FieldSubjects.areas(settings);
		for (int i = 0; i < a.length(); i++) areas.put(a.getJSONObject(i).optString("Name"));
		JSONArray subjects = new JSONArray();
		for (JSONObject r : FieldSubjects.subjects(settings)) {
			JSONArray visits = r.optJSONArray("Visits");
			int n = visits == null ? 0 : visits.length();
			subjects.put(new JSONObject().put("Record Id", r.optString("Record Id")).put("Name", r.optString("Plant"))
					.put("Type", r.optString("Record Type", FieldSubjects.TYPE_PLANT)).put("Area", r.optString("Area"))
					.put("Visits", n).put("Last Visit", n > 0 ? visits.getJSONObject(n - 1).optString("Visit Time") : ""));
		}
		return new JSONObject().put("Areas", areas).put("Subjects", subjects).toString();
	}

	// ---------------------------------------------------------------- pieces

	private String subjectCard(JSONObject r) {
		JSONArray visits = r.optJSONArray("Visits");
		int n = visits == null ? 0 : visits.length();
		JSONObject last = n > 0 ? visits.getJSONObject(n - 1) : null;
		StringBuilder sb = new StringBuilder("<div class=\"card\"><h2><a href=\"/fieldrecords/").append(enc(r.optString("Record Id"))).append("/\">")
				.append(esc(r.optString("Plant"))).append("</a> <span class=\"m\">").append(esc(r.optString("Record Type", "Plant"))).append("</span></h2>")
				.append("<div class=\"m\">").append(n).append(n == 1 ? " visit" : " visits").append(", last ").append(esc(last == null ? "-" : shortTime(last.optString("Visit Time")))).append("</div>");
		if (last != null) {
			JSONObject reading = lastReading(last);
			if (reading != null) sb.append("<div class=\"m\">").append(summary(reading)).append("</div>");
			if (!last.optString("Note").isEmpty()) sb.append("<div class=\"note\">").append(esc(last.optString("Note"))).append("</div>");
			sb.append(thumbs(r.optString("Record Id"), last, 4));
		}
		return sb.append("</div>").toString();
	}

	private static String areaSelect(String name, String selected, JSONArray areas) {
		StringBuilder sb = new StringBuilder("<select name=\"").append(name).append("\"><option value=\"\">No area</option>");
		for (int i = 0; i < areas.length(); i++) {
			String a = areas.getJSONObject(i).optString("Name");
			sb.append("<option value=\"").append(esc(a)).append("\"").append(a.equals(selected) ? " selected" : "").append(">").append(esc(a)).append("</option>");
		}
		return sb.append("</select>").toString();
	}

	// Read here, not through FieldRecordAnalyzer: that class needs the Anthropic SDK, and the pages must work without it.
	private boolean analysisOn() {
		java.util.Properties p = new java.util.Properties();
		File f = new File(settings.root, "analysis.properties");
		if (f.isFile()) {
			try (java.io.InputStream in = new java.io.FileInputStream(f)) {
				p.load(in);
			} catch (IOException e) {
				return false;
			}
		}
		return "claude".equalsIgnoreCase(p.getProperty("provider", "none").trim());
	}

	private static String thumbs(String recordId, JSONObject visit, int max) {
		JSONArray photos = visit.optJSONArray("Photos");
		if (photos == null || photos.length() == 0) return "";
		StringBuilder sb = new StringBuilder("<div class=\"ph\">");
		for (int i = 0; i < photos.length() && i < max; i++) {
			String href = "/fieldrecords/" + enc(recordId) + "/" + enc(photos.getJSONObject(i).optString("File"));
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
		return iso.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d.*Z") ? iso.substring(0, 10) + " " + iso.substring(11, 16) + " UTC" : iso;
	}

	private static String page(String title, String body) {
		return "<!doctype html><html><head>" + STYLE + "<title>" + esc(title) + "</title>" + SCRIPT + "</head><body>" + body + "</body></html>";
	}

	private static JSONObject read(File f) throws IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			return new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
		}
	}

	private static String enc(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (Exception e) {
			return s;
		}
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
	}

	// a JS string literal, safe inside a double-quoted HTML attribute
	private static String jsString(String s) {
		return esc(JSONObject.quote(s));
	}

	private static void file(HttpExchange ex, File f) throws IOException {
		ex.getResponseHeaders().set("Content-Type", f.getName().endsWith(".jpg") ? "image/jpeg" : "application/json; charset=utf-8");
		ex.sendResponseHeaders(200, f.length());
		try (OutputStream out = ex.getResponseBody()) {
			Files.copy(f.toPath(), out);
		}
	}

	private static void redirect(HttpExchange ex, String to) throws IOException {
		ex.getResponseHeaders().set("Location", to);
		ex.sendResponseHeaders(302, -1);
		ex.close();
	}

	private static void json(HttpExchange ex, String body) throws IOException {
		byte[] b = body.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");  // PaulaDeployer reads it from its Pi
		ex.sendResponseHeaders(200, b.length);
		try (OutputStream out = ex.getResponseBody()) {
			out.write(b);
		}
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
