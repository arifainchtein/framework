package com.teleonome.framework.interfaces;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.denome.Teleonome;

public interface PersistenceInterface {

	public boolean storePulse(long pulseTimeMillis,String pulseData);
	public JSONObject getLastPulse();
	public JSONObject getLastPulse(String teleonomeName);
	public Vector<Teleonome> getAllTeleonomes();
	public boolean teleonomeExist(String teleonomeName);
	public Teleonome getTeleonomeByName(String teleonomeName);
	public boolean registerTeleonome(String teleonomeName, String status, String operationMode, String identity, String networkName, String teleonomeAddress);
	public boolean storeOrganismPulse(String teleonome,String teleonomeAddress, String pulse, String statu, String operationMode, String identity, long pulseTimeMillis);
}
