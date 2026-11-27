package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.beta.messages.BetaBase64ImageSource;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaImageBlockParam;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaThinkingConfigAdaptive;
import com.anthropic.models.beta.messages.MessageCreateParams;

/**
 * Analyses field records (2026-11-27): for each visit whose "Analyses" list is still empty, asks Claude to interpret
 * it - the soil readings, the note, the photos, and the plant's earlier visits and analyses (the "patient record")
 * - and adds the answer to the visit's Analyses:
 *   {"Created", "Provider": "claude", "Model", "Stop Reason", "Text"}
 *
 * Started by FieldPacketReceiverWriter after each Ingest Field Packets (so it follows the Async Cycle), but runs on
 * a worker thread of its own: it only touches the records on disk, never the serial port, and a slow answer must
 * not hold up the cycle. One worker at a time; records are written under FieldPacketStore.RECORD_LOCK.
 *
 * Off unless FieldPackets/analysis.properties says otherwise (kept out of the denome because it points at a key):
 *   provider=claude                       none (default) | claude; a local LLM can be added as another provider
 *   model=claude-opus-5-5
 *   effort=high                           low | medium | high | xhigh | max
 *   apiKeyFile=/home/pi/Teleonome/AnthropicApiKey   a file holding the API key; empty = ANTHROPIC_API_KEY
 *   maxPhotos=6                           photos per visit sent with the request
 *   maxVisitsPerRun=3
 * A visit that fails is retried after RETRY_AFTER_MS; a refusal is recorded as an analysis (no retry).
 */
public class FieldRecordAnalyzer {

	public static final String PROPERTIES_FILE = "analysis.properties";
	private static final long RETRY_AFTER_MS = 30 * 60 * 1000L;
	private static final Logger logger = Logger.getLogger(FieldRecordAnalyzer.class);

	private static Thread worker = null;
	private static final Map<String, Long> failedAt = new HashMap<String, Long>();  // "<record id>/<packet id>"

	private static final String SYSTEM_PROMPT = String.join("\n",
			"You are a horticulturist helping a farmer in Victoria, Australia (southern hemisphere) look after his plants.",
			"Each request is one visit to one plant: readings from a 7-in-1 RS485 soil probe pushed into the soil at root depth,",
			"phone photos of the plant, the farmer's note, and the plant's earlier visits and your earlier analyses.",
			"",
			"About the probe: moisture is volumetric %, temperature is soil temperature in C, EC is in uS/cm, pH is soil pH.",
			"Its N, P and K (mg/kg) are estimated by the probe from EC, not measured, so treat them as a rough guide and never",
			"as a lab result. Every value depends on moisture, so compare visits at similar moisture. pH needs about 5 minutes",
			"after the probe goes in to settle; if a visit has several readings, the last one counts most.",
			"",
			"Write for the farmer, in plain words, with these headings:",
			"What I see - what the photos and note show (symptoms, where on the plant, how widespread).",
			"What the soil says - the readings, what is normal for this plant, and what changed since earlier visits.",
			"Most likely causes - ranked, with how sure you are and what points to each.",
			"What to check - quick checks in the field that would tell the causes apart.",
			"What to do now - concrete actions, amounts where it matters, and what not to do.",
			"Next visit - when to come back and what to photograph or measure then.",
			"Be honest about what the photos cannot show. Keep it under about 450 words.");

	public static synchronized void startIfIdle(final FieldPacketSettings settings) {
		if (worker != null && worker.isAlive()) return;
		final Properties p = properties(settings);
		if (!"claude".equalsIgnoreCase(p.getProperty("provider", "none").trim())) return;
		worker = new Thread(new Runnable() {
			public void run() {
				try {
					analyzePending(settings, p);
				} catch (Throwable t) {
					logger.warn("Field record analysis stopped: " + t);
				}
			}
		}, "FieldRecordAnalyzer");
		worker.setDaemon(true);
		worker.start();
	}

	static Properties properties(FieldPacketSettings settings) {
		Properties p = new Properties();
		File f = new File(settings.root, PROPERTIES_FILE);
		if (f.isFile()) {
			try (InputStream in = new FileInputStream(f)) {
				p.load(in);
			} catch (Exception e) {
				logger.warn("Cannot read " + f + ": " + e.getMessage());
			}
		}
		return p;
	}

	private static void analyzePending(FieldPacketSettings settings, Properties p) throws Exception {
		int maxVisits = Integer.parseInt(p.getProperty("maxVisitsPerRun", "3").trim());
		File[] records = settings.plants().listFiles(File::isDirectory);
		if (records == null) return;
		AnthropicClient client = null;
		int done = 0;
		for (File dir : records) {
			File recordFile = new File(dir, FieldPacketStore.RECORD_FILE);
			if (!recordFile.isFile()) continue;
			JSONObject record;
			synchronized (FieldPacketStore.RECORD_LOCK) {
				record = new JSONObject(new String(Files.readAllBytes(recordFile.toPath()), StandardCharsets.UTF_8));
			}
			JSONArray visits = record.optJSONArray("Visits");
			if (visits == null) continue;
			for (int i = 0; i < visits.length() && done < maxVisits; i++) {
				JSONObject visit = visits.getJSONObject(i);
				JSONArray analyses = visit.optJSONArray("Analyses");
				if (analyses != null && analyses.length() > 0) continue;
				String key = dir.getName() + "/" + visit.optString("Packet Id");
				Long failed = failedAt.get(key);
				if (failed != null && System.currentTimeMillis() - failed < RETRY_AFTER_MS) continue;
				if (client == null) client = client(p);
				try {
					JSONObject analysis = analyze(client, p, record, i, dir);
					addAnalysis(recordFile, visit.optString("Packet Id"), analysis);
					failedAt.remove(key);
					logger.info("Analysed " + key + " (" + analysis.optString("Stop Reason") + ")");
				} catch (Exception e) {
					failedAt.put(key, System.currentTimeMillis());
					logger.warn("Could not analyse " + key + ", trying again in " + RETRY_AFTER_MS / 60000 + " min: " + e);
				}
				done++;
			}
		}
	}

	private static AnthropicClient client(Properties p) throws Exception {
		String keyFile = p.getProperty("apiKeyFile", "").trim();
		AnthropicOkHttpClient.Builder b = AnthropicOkHttpClient.builder().timeout(Duration.ofMinutes(5));
		if (!keyFile.isEmpty()) {
			b.apiKey(new String(Files.readAllBytes(new File(keyFile).toPath()), StandardCharsets.UTF_8).trim());
			return b.build();
		}
		return AnthropicOkHttpClient.fromEnv();  // ANTHROPIC_API_KEY
	}

	private static JSONObject analyze(AnthropicClient client, Properties p, JSONObject record, int visitIndex, File dir) throws Exception {
		String model = p.getProperty("model", "claude-opus-5-5").trim();
		int maxPhotos = Integer.parseInt(p.getProperty("maxPhotos", "6").trim());
		JSONArray visits = record.getJSONArray("Visits");
		JSONObject visit = visits.getJSONObject(visitIndex);

		StringBuilder text = new StringBuilder();
		text.append("Plant: ").append(record.optString("Plant")).append("\n\n");
		if (visitIndex > 0) {
			text.append("Earlier visits, oldest first:\n");
			for (int i = Math.max(0, visitIndex - 6); i < visitIndex; i++) text.append(describe(visits.getJSONObject(i), true)).append("\n");
			text.append("\n");
		} else {
			text.append("This is the first visit to this plant.\n\n");
		}
		text.append("This visit:\n").append(describe(visit, false));

		List<BetaContentBlockParam> content = new ArrayList<BetaContentBlockParam>();
		JSONArray photos = visit.optJSONArray("Photos");
		int sent = 0;
		if (photos != null) {
			for (int i = 0; i < photos.length() && sent < maxPhotos; i++) {
				String name = photos.getJSONObject(i).optString("File");
				if (!name.matches("[A-Za-z0-9_.-]+")) continue;  // only files in the record's own folder
				File f = new File(dir, name);
				if (!f.isFile()) continue;
				content.add(BetaContentBlockParam.ofImage(BetaImageBlockParam.builder()
						.source(BetaBase64ImageSource.builder()
								.mediaType(BetaBase64ImageSource.MediaType.IMAGE_JPEG)
								.data(Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())))
								.build())
						.build()));
				sent++;
			}
		}
		text.append("\n").append(sent).append(" photo(s) of this visit are attached above.");
		content.add(BetaContentBlockParam.ofText(text.toString()));

		MessageCreateParams params = MessageCreateParams.builder()
				.model(model)
				.maxTokens(16000L)
				.system(SYSTEM_PROMPT)
				.thinking(BetaThinkingConfigAdaptive.builder().build())
				.outputConfig(BetaOutputConfig.builder().effort(effort(p.getProperty("effort", "high"))).build())
				// a refusal is answered by another model instead of stopping (server-side fallbacks, routed by category)
				.addBeta("server-side-fallback-2026-07-01")
				.putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
				.addUserMessageOfContentBlockParams(content)
				.build();
		BetaMessage response = client.beta().messages().create(params);

		StringBuilder answer = new StringBuilder();
		response.content().forEach(block -> block.text().ifPresent(t -> answer.append(t.text())));
		String stop = response.stopReason().map(Object::toString).orElse("");
		if (response.stopReason().isPresent() && response.stopReason().get().equals(BetaStopReason.REFUSAL) && answer.length() == 0) {
			answer.append("(The model declined to analyse this visit.)");
		}
		JSONObject a = new JSONObject();
		a.put("Created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(new Date()));
		a.put("Provider", "claude");
		a.put("Model", response.model().toString());
		a.put("Stop Reason", stop);
		a.put("Photos Sent", sent);
		a.put("Text", answer.toString().trim());
		return a;
	}

	// One visit as text for the prompt; earlier visits carry their analyses too.
	private static String describe(JSONObject v, boolean withAnalyses) {
		StringBuilder sb = new StringBuilder();
		sb.append("- ").append(v.optString("Visit Time")).append("\n");
		if (!v.optString("Note").isEmpty()) sb.append("  Farmer's note: ").append(v.optString("Note")).append("\n");
		JSONArray readings = v.optJSONArray("Readings");
		if (readings != null) {
			for (int i = 0; i < readings.length(); i++) {
				JSONObject r = readings.getJSONObject(i);
				sb.append(String.format("  Reading %s: moisture %s %%, soil temp %s C, pH %s, EC %s uS/cm, N %s, P %s, K %s mg/kg (estimated), salt %s%n",
						r.optString("time"), r.opt("moist"), r.opt("temp"), r.opt("ph"), r.opt("ec"), r.opt("n"), r.opt("p"), r.opt("k"), r.opt("salt")));
			}
			if (readings.length() == 0) sb.append("  No soil readings this visit.\n");
		}
		JSONArray photos = v.optJSONArray("Photos");
		if (withAnalyses) {
			sb.append("  Photos: ").append(photos == null ? 0 : photos.length()).append(" (not attached)\n");
			JSONArray analyses = v.optJSONArray("Analyses");
			if (analyses != null && analyses.length() > 0) {
				sb.append("  Your analysis then: ").append(analyses.getJSONObject(analyses.length() - 1).optString("Text")).append("\n");
			}
			JSONArray follow = v.optJSONArray("Follow Ups");
			if (follow != null && follow.length() > 0) sb.append("  Follow ups: ").append(follow.toString()).append("\n");
		}
		return sb.toString();
	}

	private static BetaOutputConfig.Effort effort(String s) {
		switch (s.trim().toLowerCase()) {
		case "low": return BetaOutputConfig.Effort.LOW;
		case "medium": return BetaOutputConfig.Effort.MEDIUM;
		case "xhigh": return BetaOutputConfig.Effort.XHIGH;
		case "max": return BetaOutputConfig.Effort.MAX;
		default: return BetaOutputConfig.Effort.HIGH;
		}
	}

	// Adds the analysis to the visit, re-reading the record under the lock (an ingest may have added visits since).
	private static void addAnalysis(File recordFile, String packetId, JSONObject analysis) throws Exception {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			JSONObject record = new JSONObject(new String(Files.readAllBytes(recordFile.toPath()), StandardCharsets.UTF_8));
			JSONArray visits = record.getJSONArray("Visits");
			for (int i = 0; i < visits.length(); i++) {
				JSONObject v = visits.getJSONObject(i);
				if (!packetId.equals(v.optString("Packet Id"))) continue;
				JSONArray analyses = v.optJSONArray("Analyses");
				if (analyses == null) {
					analyses = new JSONArray();
					v.put("Analyses", analyses);
				}
				analyses.put(analysis);
			}
			record.put("Last Updated", analysis.optString("Created"));
			FieldPacketStore.writeJson(recordFile, record);
		}
	}
}
