package com.teleonome.framework.microcontroller.annabellemicrocontroller;

import org.json.JSONObject;

import com.teleonome.framework.mnemosyne.MnemosyneManager;

public abstract class AnnabelleDeserializer {

		protected MnemosyneManager aMnemosyneManager=null;
		
		public abstract JSONObject deserialise(String teleonomeName, String line);
		
		public void setMnemosyneManager(MnemosyneManager a) {
			aMnemosyneManager=a;
		}
}
