package com.teleonome.framework.microcontroller.annabellemicrocontroller;




import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;

public class AnnabelleWriter  extends BufferedWriter{
	DenomeManager aDenomeManager;
	PostgresqlPersistenceManager aDBManager;
	AnnabelleReader aAnnabelleReader;
	Logger logger;
	Writer output;
	long lastCommandTime=0;
	long minimumIntervalBetweenCommands=2000;

	public AnnabelleWriter(Writer out, AnnabelleReader c, DenomeManager d, PostgresqlPersistenceManager db) {
		super(out);
		aAnnabelleReader=c;
		aDenomeManager=d;
		aDBManager=db;
		output=out;
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}

	public void close() throws IOException {
		logger.info("about to close AnnabelleWriter");
		//String trace = Utils.generateMethodTrace();
		//	logger.debug(trace);
		super.close();
	}
	public AnnabelleReader getReader() {
		return aAnnabelleReader;
	}
	public void write(String command, int off, int len) throws IOException {
		long now=System.currentTimeMillis();
		long diff = now-lastCommandTime;
		if(minimumIntervalBetweenCommands>diff) {
			long sleeptime = minimumIntervalBetweenCommands-diff;
			logger.debug("Sleeping for " + sleeptime+ "milliseconds before sending command");
			try {
				Thread.sleep(sleeptime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			logger.debug("Sending command since its been " + diff+ "since the last  command");
		}
		aAnnabelleReader.setCurrentCommand(command);
		if(command.startsWith(TeleonomeConstants.DELETE_TELEPATHON)){
			String[] tokens = command.split("#");
			String telepathonName=tokens[1];
			logger.debug("RemoveTelepathon telepathonName=" + telepathonName);
			aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_TELEPATHONS, telepathonName);
		}else if(command.startsWith(TeleonomeConstants.DELETE_STALE_TELEPATHONS)){
			String[] tokens = command.split("#");
			String telepathonTime=tokens[1];
			logger.debug("Removing All Stale Telepathons by " + telepathonTime);
			
			// get all telepathons
			JSONArray telepathons = aDenomeManager.getAllTelepathons();
			JSONObject telepathon;
			String telepathonName;
			ArrayList<String> staleTelepathons = new ArrayList<String>();
			for(int i=0;i<telepathons.length();i++) {
				telepathon=telepathons.getJSONObject(i);

				telepathonName = telepathon.getString(TeleonomeConstants.DENE_NAME_ATTRIBUTE);
				logger.debug("line 76 telepathonName= " + telepathonName);
				Identity identity = new Identity(aDenomeManager.getDenomeName(),TeleonomeConstants.NUCLEI_TELEPATHONS, telepathonName, TeleonomeConstants.TELEPATHON_DENE_PURPOSE, TeleonomeConstants.TELEPHATON_DENEWORD_SECONDS_TIME );
				logger.debug("line 80 identity= " + identity.toString());
				try {
					Number secondsTimeNumber = (Number)aDenomeManager.getDeneWordAttributeByIdentity(identity, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					if (secondsTimeNumber == null) {
						logger.warn("Seconds Time missing or unresolvable for telepathon '" + telepathonName + "' — marking stale");
						staleTelepathons.add(telepathonName);
					} else {
						long secondsTime = secondsTimeNumber.longValue();
						logger.debug("line 82 secondsTime= " + secondsTime);
						if(telepathonTime.equals(TeleonomeConstants.MNEMOSYNE_HOURLY_MUTATION)) {
							if(System.currentTimeMillis()/1000 > (secondsTime+3600) ) {
								logger.debug("Marking Hourly Stale  " + telepathonName);
								staleTelepathons.add(telepathonName);
							}
						}else if(telepathonTime.equals(TeleonomeConstants.MNEMOSYNE_DAILY_MUTATION)) {
							if(System.currentTimeMillis()/1000 > (secondsTime+24*3600) ) {
								logger.debug("Marking Daily Stale  " + telepathonName);
								staleTelepathons.add(telepathonName);
							}
						}
					}
				} catch (InvalidDenomeException e) {
					logger.warn(Utils.getStringException(e));
				}catch (Exception e) {
					logger.warn(Utils.getStringException(e));
				}

			}
			for(String staleTelepathonName : staleTelepathons) {
				logger.debug("Removing Stale  " + staleTelepathonName);
				aDenomeManager.removeDeneChain(TeleonomeConstants.NUCLEI_TELEPATHONS, staleTelepathonName);
			}

		}else if(command.startsWith(TeleonomeConstants.RENAME_TELEPATHON)){
			String[] tokens = command.split("#");
			String oldName = tokens[1];
			String newName = tokens[2];
			logger.debug("RenameTelepathon " + oldName + " -> " + newName);

			Identity telepathonIdentity = new Identity(aDenomeManager.getDenomeName(), TeleonomeConstants.NUCLEI_TELEPATHONS, oldName);
			JSONObject telepathonDeneChain = aDenomeManager.getDenomicElementByIdentity(telepathonIdentity);
			if(telepathonDeneChain!=null){
				//
				// record the corrupted name Cerebellum flagged before renaming the
				// DeneChain out from under it, so the correction stays visible in
				// the denome itself rather than only in whatever triggered it
				try {
					JSONArray telepathonDenes = telepathonDeneChain.getJSONArray("Denes");
					for(int i=0;i<telepathonDenes.length();i++){
						JSONObject aDene = telepathonDenes.getJSONObject(i);
						if(TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION.equals(aDene.optString("Name"))){
							JSONArray configDeneWords = aDene.getJSONArray("DeneWords");
							configDeneWords.put(Utils.createDeneWordJSONObject(TeleonomeConstants.DENEWORD_TELEPATHON_REPORTED_NAME, oldName, null, "String", true));
							break;
						}
					}
				} catch (Exception e) {
					logger.warn(Utils.getStringException(e));
				}

				telepathonDeneChain.put(TeleonomeConstants.DENE_NAME_ATTRIBUTE, newName);
				logger.info("Renamed telepathon DeneChain " + oldName + " -> " + newName + " (corrected by Cerebellum)");
			}else{
				logger.warn("RenameTelepathon: could not find DeneChain '" + oldName + "' under Telepathons nucleus, nothing to rename");
			}

			//
			// keep today's partition consistent with the denome - historical
			// partitions from before the correction keep the old name, same as
			// any other point-in-time record
			try {
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Australia/Melbourne"));
				String tableName = "telepathon_" + cal.get(Calendar.YEAR) + "_" + (cal.get(Calendar.MONTH)+1) + "_" + cal.get(Calendar.DAY_OF_MONTH);
				Connection conn = aDBManager.getConnection();
				try {
					PreparedStatement ps = conn.prepareStatement("UPDATE " + tableName + " SET telepathonname=? WHERE telepathonname=?");
					try {
						ps.setString(1, newName);
						ps.setString(2, oldName);
						int updated = ps.executeUpdate();
						logger.info("RenameTelepathon: updated " + updated + " row(s) in " + tableName);
					} finally {
						ps.close();
					}
				} finally {
					conn.close();
				}
			} catch (SQLException e) {
				logger.warn(Utils.getStringException(e));
			}

		}else if(command.equals("GetSensorData")) {
			//
			// Annabelle has no native GetSensorData protocol - the only
			// "sensor" reading it reports through this mechanism is its own
			// Port Status, derived from a Ping round trip (Ok-Ping means the
			// serial link is alive). AnnabelleReader.readLine() translates
			// the raw response into true/false for the Port Status sensor
			// value.
			//
			logger.debug("Translating GetSensorData to Ping to check port status");
			String pingCommand = "Ping";
			output.write(pingCommand,0,pingCommand.length());
		}else {
			logger.debug("sending  command:" + command);
			output.write(command,0,command.length());
		}
		//	logger.debug("sending  command:" + command);
		//	output.write(command,0,command.length());
		//			if(command.equals("GetSensorData")){
		//				aAnnabelleReader.setCurrentCommand(command);
		//			}else{
		//				try {
		//					//
		//					// the command is a hash separated string that has the 
		//					// following structure:
		//					// token[0]=the name of the command to be called
		//					// token[1]... token[n] parameters that will be passed with the command 
		//					//
		//					// as an example to execute the command testping 64
		//					// the actuator command true expression would be
		//					//testping#64
		//					ArrayList<String> results = Utils.executeCommand(command);
		//					logger.info("results=" + results);
		//				} catch (IOException | InterruptedException e) {
		//					// TODO Auto-generated catch block
		//					logger.warn(Utils.getStringException(e));
		//				}
		//			}
	}

}
