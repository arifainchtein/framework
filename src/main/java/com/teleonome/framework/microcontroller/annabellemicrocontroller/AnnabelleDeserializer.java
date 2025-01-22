package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.json.JSONObject;

import com.teleonome.framework.mnemosyne.MnemosyneManager;

public abstract class AnnabelleDeserializer {

		protected MnemosyneManager aMnemosyneManager=null;
		protected long secondsTime=0;
		public abstract JSONObject deserialise(String teleonomeName, String line);
		public void setMnemosyneManager(MnemosyneManager a) {
			aMnemosyneManager=a;
		}
		
		public long getTimeSeconds() {
			// TODO Auto-generated method stub
			return secondsTime;
		}
}
