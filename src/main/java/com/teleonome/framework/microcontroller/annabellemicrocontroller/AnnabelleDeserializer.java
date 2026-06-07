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
		protected static boolean isValidName(String name) {
			if (name == null || name.isEmpty()) return false;
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (c < 0x20 || c > 0x7E) return false;
			}
			return true;
		}
}
