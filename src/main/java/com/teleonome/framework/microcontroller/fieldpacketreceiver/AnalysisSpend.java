package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The estimated Claude credit balance (2026-11-27). The API does not tell a program the account's remaining prepaid
 * credits, so Annabelle keeps her own ledger: the credits Ari says he bought (or the balance he read in the Console),
 * minus what each analysis cost - worked out from the token counts the API returns with every answer and the
 * published per-million-token prices below. It only counts what this key spends here; the Console is the real figure.
 *
 * FieldPackets/analysis-spend.json:
 *   {"Credits": [{"Amount", "At", "Note"}], "Spent": "<USD>", "Analyses": n,
 *    "Since": <time of the last "set balance">, "Recent": [{"At", "What", "Model", "Cost"}] (last 50)}
 * Written under FieldPacketStore.RECORD_LOCK.
 */
public class AnalysisSpend {

	public static final String FILE = "analysis-spend.json";

	// USD per million tokens: input, output, cache read, cache write (5 min). Claude API list prices, 2026-11.
	// A model not listed (a fallback that answered instead) is priced as the requested one.
	private static final Object[][] PRICES = {
		{ "claude-opus-5-5", 4.00, 20.00, 0.20, 5.00 },
		{ "claude-opus-5", 5.00, 25.00, 0.50, 6.25 },
		{ "claude-opus-4-8", 5.00, 25.00, 0.50, 6.25 },
		{ "claude-sonnet-5-5", 2.00, 10.00, 0.10, 2.50 },
		{ "claude-sonnet-5", 2.00, 10.00, 0.20, 2.50 },
		{ "claude-haiku-5-5", 0.10, 0.50, 0.01, 0.125 },
	};

	private static double[] price(String model, String fallbackModel) {
		for (String m : new String[] { model, fallbackModel }) {
			if (m == null) continue;
			for (Object[] p : PRICES) {
				if (m.equals(p[0]) || m.startsWith((String) p[0] + "-")) return new double[] { (Double) p[1], (Double) p[2], (Double) p[3], (Double) p[4] };
			}
		}
		return new double[] { 4.00, 20.00, 0.20, 5.00 };
	}

	// The cost of one answer, in USD.
	public static BigDecimal cost(String model, String requestedModel, long input, long output, long cacheRead, long cacheWrite) {
		double[] p = price(model, requestedModel);
		double usd = (input * p[0] + output * p[1] + cacheRead * p[2] + cacheWrite * p[3]) / 1_000_000.0;
		return BigDecimal.valueOf(usd).setScale(4, RoundingMode.HALF_UP);
	}

	public static JSONObject read(FieldPacketSettings s) throws IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			File f = new File(s.root, FILE);
			JSONObject j = f.isFile() ? FieldSubjects.readJson(f) : new JSONObject();
			if (!j.has("Credits")) j.put("Credits", new JSONArray());
			if (!j.has("Spent")) j.put("Spent", "0");
			if (!j.has("Analyses")) j.put("Analyses", 0);
			if (!j.has("Recent")) j.put("Recent", new JSONArray());
			return j;
		}
	}

	private static void write(FieldPacketSettings s, JSONObject j) throws IOException {
		FieldPacketStore.writeJson(new File(s.root, FILE), j);
	}

	public static void addSpend(FieldPacketSettings s, String what, String model, BigDecimal cost) throws IOException {
		synchronized (FieldPacketStore.RECORD_LOCK) {
			JSONObject j = read(s);
			j.put("Spent", new BigDecimal(j.optString("Spent", "0")).add(cost).toPlainString());
			j.put("Analyses", j.optInt("Analyses") + 1);
			JSONArray recent = j.getJSONArray("Recent"), kept = new JSONArray();
			recent.put(new JSONObject().put("At", FieldSubjects.now()).put("What", what).put("Model", model).put("Cost", cost.toPlainString()));
			for (int i = Math.max(0, recent.length() - 50); i < recent.length(); i++) kept.put(recent.get(i));
			j.put("Recent", kept);
			write(s, j);
		}
	}

	public static BigDecimal credits(JSONObject j) {
		BigDecimal total = BigDecimal.ZERO;
		JSONArray c = j.optJSONArray("Credits");
		for (int i = 0; c != null && i < c.length(); i++) total = total.add(new BigDecimal(c.getJSONObject(i).optString("Amount", "0")));
		return total;
	}

	public static BigDecimal balance(JSONObject j) {
		return credits(j).subtract(new BigDecimal(j.optString("Spent", "0")));
	}

	// Ari bought credits in the Console
	public static String addCredit(FieldPacketSettings s, String amount, String note) throws IOException, FieldPacketException {
		BigDecimal a = money(amount);
		if (a.signum() <= 0) throw new FieldPacketException("the amount must be more than 0");
		synchronized (FieldPacketStore.RECORD_LOCK) {
			JSONObject j = read(s);
			j.getJSONArray("Credits").put(new JSONObject().put("Amount", a.toPlainString()).put("At", FieldSubjects.now()).put("Note", note == null ? "" : note));
			write(s, j);
			return "Added $" + a.toPlainString() + " - about $" + balance(j).setScale(2, RoundingMode.HALF_UP).toPlainString() + " left";
		}
	}

	// Ari read the real balance in the Console: start again from it (spending is counted from now)
	public static String setBalance(FieldPacketSettings s, String amount) throws IOException, FieldPacketException {
		BigDecimal a = money(amount);
		if (a.signum() < 0) throw new FieldPacketException("the balance can't be below 0");
		synchronized (FieldPacketStore.RECORD_LOCK) {
			JSONObject j = read(s);
			j.put("Credits", new JSONArray().put(new JSONObject().put("Amount", a.toPlainString()).put("At", FieldSubjects.now()).put("Note", "balance read in the Console")));
			j.put("Spent", "0");
			j.put("Analyses", 0);
			j.put("Since", FieldSubjects.now());
			write(s, j);
			return "Balance set to $" + a.toPlainString();
		}
	}

	private static BigDecimal money(String amount) throws FieldPacketException {
		try {
			BigDecimal a = new BigDecimal(amount == null ? "" : amount.trim().replace("$", "").replace(",", ""));
			if (a.compareTo(new BigDecimal("100000")) > 0) throw new FieldPacketException("that is more than $100,000");
			return a.setScale(2, RoundingMode.HALF_UP);
		} catch (NumberFormatException e) {
			throw new FieldPacketException("not an amount of dollars: " + amount);
		}
	}
}
