package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.json.JSONObject;

import com.teleonome.framework.mnemosyne.MnemosyneManager;

public abstract class AnnabelleDeserializer {

		protected MnemosyneManager aMnemosyneManager=null;
		protected long secondsTime=0;
		public abstract JSONObject deserialise(String teleonomeName, String line);
		public abstract long getTimeSeconds();
		public void setMnemosyneManager(MnemosyneManager a) {
			aMnemosyneManager=a;
		}
}
