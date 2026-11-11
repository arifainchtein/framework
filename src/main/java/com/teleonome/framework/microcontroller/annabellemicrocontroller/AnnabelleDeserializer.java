package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.json.JSONObject;

import com.teleonome.framework.mnemosyne.MnemosyneManager;

public abstract class AnnabelleDeserializer {

		protected MnemosyneManager aMnemosyneManager=null;
		protected long secondsTime=0;
		protected long sourceoriginaltime=0;
		public abstract JSONObject deserialise(String teleonomeName, String line);
		public void setMnemosyneManager(MnemosyneManager a) {
			aMnemosyneManager=a;
		}

		public long getTimeSeconds() {
			// TODO Auto-generated method stub
			return secondsTime;
		}

		public long getSourceoriginaltime() {
			// TODO Auto-generated method stub
			return sourceoriginaltime;
		}

		// Returns false if the name contains non-printable or non-ASCII characters
		// (e.g. Unicode replacement chars or null bytes from a corrupted serial frame).
		// The relay that resent this packet (2026-11-11): Annabelle ends a data line with
		// "sentby:<name>" ("Paula", "Lan1", "Chinamp"), or "sentby:" when the device sent it itself.
		// Keyed, so it is found whatever the line's length; "" when the line has none (older Annabelle).
		protected static String sentByFrom(String[] tokens) {
			for (int i = tokens.length - 1; i >= 0 && i >= tokens.length - 3; i--) {
				String t = tokens[i].replaceAll("\u0000", "").trim();
				if (t.startsWith("sentby:")) return t.substring("sentby:".length()).trim();
			}
			return "";
		}

		// Every route that delivered this reading to Annabelle (2026-11-11): "routes:direct,Lan1,Paula" -
		// she collects the copies for a few seconds and sends the reading once. "" when the line has
		// none (older Annabelle) or she no longer remembered it.
		protected static String routesFrom(String[] tokens) {
			for (int i = tokens.length - 1; i >= 0 && i >= tokens.length - 3; i--) {
				String t = tokens[i].replaceAll("\\u0000", "").trim();
				if (t.startsWith("routes:")) return t.substring("routes:".length()).trim();
			}
			return "";
		}

		protected static int routeCount(String routes) {
			return routes.isEmpty() ? 0 : routes.split(",").length;
		}

		protected static boolean isValidName(String name) {
			if (name == null || name.isEmpty()) return false;
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (c < 0x20 || c > 0x7E) return false;
			}
			return true;
		}
}
