package com.teleonome.framework.microcontroller.cajalmicrocontroller;

import org.json.JSONObject;

public abstract class CajalDeserializer {

		public abstract JSONObject deserialise(String line);
}
