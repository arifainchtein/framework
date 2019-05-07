package com.teleonome.framework.mnemosyne.operations;

import java.time.ZoneId;

import org.json.JSONObject;


import com.teleonome.framework.denome.DenomeManager;

public abstract class MnemosyneOperation {
	JSONObject mnemosyneDene;
	public MnemosyneOperation(JSONObject m) {
		mnemosyneDene=m;
	}
	
	
	public abstract  void process(DenomeManager denomeManager, long currentTimeMillis, 
							String formatedCurrentTimestamp,String formatedCurrentDate,
							String formatedCurrentTime, ZoneId zoneId) ;
}
