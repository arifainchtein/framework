package com.teleonome.framework.microcontroller.annabellmicrocontroller;

import org.json.JSONObject;

import com.teleonome.framework.mnemosyne.MnemosyneManager;

public abstract class AnnabellDeserializer {

		protected MnemosyneManager aMnemosyneManager=null;
		
		public abstract JSONObject deserialise(String teleonomeName, String line);
		
		public void setMnemosyneManager(MnemosyneManager a) {
			aMnemosyneManager=a;
		}
}
