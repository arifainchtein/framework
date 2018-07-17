package com.teleonome.framework.persistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.denome.Teleonome;
import com.teleonome.framework.hypothalamus.CommandRequest;
import com.teleonome.framework.interfaces.PersistenceInterface;
import com.teleonome.framework.utils.Utils;

public class PostgresqlPersistenceManager implements PersistenceInterface{

	private static PostgresqlPersistenceManager aPostgresqlPersistenceManager;
	private final String DATABASE_URL = "postgres://postgres:sazirac@localhost:5432/teleonome";
	private TeleonomeConnectionPool connectionPool;
	private Logger logger;  

	private PostgresqlPersistenceManager(){
		logger = Logger.getLogger(getClass());
		logger.debug("Initiating DBManager");
	}

	public static PostgresqlPersistenceManager instance() {

		if(aPostgresqlPersistenceManager==null){
			aPostgresqlPersistenceManager = new PostgresqlPersistenceManager();
			aPostgresqlPersistenceManager.init();
		}
		return aPostgresqlPersistenceManager;
	}


	public void init(){

		URI dbUri;
		try {
			dbUri = new URI(DATABASE_URL);
			String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() +  dbUri.getPath() ;
			System.out.println("dbUrl=" + dbUrl);
			connectionPool = new TeleonomeConnectionPool();

			if (dbUri.getUserInfo() != null) {
				connectionPool.setUsername(dbUri.getUserInfo().split(":")[0]);
				connectionPool.setPassword(dbUri.getUserInfo().split(":")[1]);
			}
			connectionPool.setDriverClassName("org.postgresql.Driver");
			connectionPool.setUrl(dbUrl);
			connectionPool.setMaxTotal(3);
			connectionPool.setMaxWaitMillis(180000);
			connectionPool.setInitialSize(1); 
			System.out.println("connectionPool=" + connectionPool);

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.info(s);
			logger.warn(Utils.getStringException(e));
		}
	}

	public Connection getConnection() throws SQLException{
		return connectionPool.getConnection();
	}

	public double getTableSizeInMB(String tableName) {
		String sql ="SELECT"+
	    "total_size/1024000 AS total_size"+
	    "FROM ("+
	        "SELECT"+
	            "table_name,"+
	           " pg_table_size(table_name) AS table_size,"+
	           " pg_indexes_size(table_name) AS indexes_size,"+
	           " pg_total_relation_size(table_name) AS total_size"+
	      "  FROM ("+
	        "    SELECT table_name  AS table_name"+
	        "    FROM information_schema.tables where table_name='"+ tableName +"'"+
	       " ) AS all_tables"+
	      "  ORDER BY total_size DESC"+
	   " ) AS pretty_sizes";
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		double size=-1;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

		    rs = statement.executeQuery(sql);
			String sizeString="";
			while(rs.next()){
				size=rs.getDouble(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		

		}finally{

			if(statement!=null)
				try {
					if(rs!=null)rs.close();
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return size;
		

	}
	
public JSONArray vacuum() {
		
		Connection connection=null;
		Statement st2=null;
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		JSONObject warningJSONObject;
		
		try{
			connection = getConnection();
			
			 st2 = connection.createStatement();
			 st2.executeUpdate("VACUUM ANALYZE VERBOSE"); 
			
			 SQLWarning warning = st2.getWarnings();
			  
			 if (warning != null)
			 {
			      while (warning != null)
			     {
			    	  warningJSONObject = new JSONObject();
			    	  warningJSONObject.put("Message", warning.getMessage());
			    	  warningJSONObject.put("SQLState", warning.getSQLState());
			    	  warningJSONObject.put("Error Code", warning.getErrorCode());
			    	  toReturn.put(warningJSONObject);
			         
			         warning = warning.getNextWarning();
			     }
			 }
		}
		catch(SQLException e) {
			
		}finally {
			if(st2!=null)
				try {
					st2.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			if(connection!=null) closeConnection(connection);
		}
		return toReturn;
	}
	
	public int deleteByPeriodFromOrganismPulse( long millisToDeleteFrom, String teamInfo) {
		
		String command = "delete from organismpulse where pulsetimemillis < " +  millisToDeleteFrom;
		if(!teamInfo.equals("")) {
			command +=command + " " + teamInfo;
		}
		return deleteByPeriod( command);
	}

	public int deleteByPeriodFromPulse(long millisToDeleteFrom) {
		String command = "delete from pulse where pulsetimemillis < " +  millisToDeleteFrom;
		return deleteByPeriod( command);
	}
	
	private int deleteByPeriod(String command) {
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		int numberDeleted=0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			
			
			rs = statement.executeQuery(command);
			String sizeString="";
			while(rs.next()){
				numberDeleted=rs.getInt(1);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.debug(s);

		}finally{

			if(statement!=null)
				try {
					if(rs!=null)rs.close();
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return numberDeleted;
	}
	
	public double getDatabaseSizeInMB(){
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		double size=-1;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			String command = "SELECT pg_size_pretty(pg_database_size(pg_database.datname)) AS size FROM pg_database where pg_database.datname= 'teleonome'";
			rs = statement.executeQuery(command);
			String sizeString="";
			while(rs.next()){
				sizeString=rs.getString(1);
			}

			String[] tokens = sizeString.split(" ");
			size = Double.parseDouble(tokens[0]);
			String units = tokens[1];
			if(units.equalsIgnoreCase("KB"))size= size/1000;
			else if(units.equalsIgnoreCase("MB"))size= size;
			else if(units.equalsIgnoreCase("GB"))size= size*1000;
			else if(units.equalsIgnoreCase("TB"))size= size*1000000;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.debug(s);

		}finally{

			if(statement!=null)
				try {
					if(rs!=null)rs.close();
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return size;

	}

	public double getTableSizeMB(String tableName){
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		double size=-1;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String command = "SELECT pg_size_pretty(pg_total_relation_size(relid))   FROM pg_catalog.pg_statio_user_tables where relname='"+ tableName+"'";
			rs = statement.executeQuery(command);
			String sizeString="";
			while(rs.next()){
				sizeString=rs.getString(1);
			}

			String[] tokens = sizeString.split(" ");
			size = Double.parseDouble(tokens[0]);
			String units = tokens[1];
			if(units.equalsIgnoreCase("KB"))size= size/1000;
			else if(units.equalsIgnoreCase("MB"))size= size;
			else if(units.equalsIgnoreCase("GB"))size= size*1000;
			else if(units.equalsIgnoreCase("TB"))size= size*1000000;

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.debug(s);

		}finally{

			if(statement!=null)
				try {
					if(rs!=null)rs.close();
					statement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return size;

	}
	
	public void closeConnection(Connection con){
		try {
			connectionPool.closeConnection(con);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));

		}
	}
	
	
	public JSONArray getDeneWordTimeSeriesByIdentity(Identity identity, long startTimeMillis, long endTimeMillis) {
		String sql = "select pulsetimemillis,DeneWord -> 'Value' As CurrentPulse from pulse p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where pulsetimemillis>="+ startTimeMillis + " and pulsetimemillis<=" + endTimeMillis+" and Nucleus->>'Name'='"+identity.getNucleusName() +"' and DeneChain->>'Name'='"+ identity.getDenechainName() + "' and Dene->>'Name'='"+ identity.getDeneName()+"' and DeneWord->>'Name'='"+ identity.getDeneWordName() +"' order by pulsetimemillis asc";
		Connection connection = null;
		Statement statement = null;
		JSONArray toReturn = new JSONArray();
		ResultSet rs=null;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			rs = statement.executeQuery(sql);
			JSONObject data=null;
			Long L;
			while(rs.next()){

				data = new JSONObject();
				data.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, rs.getLong(1));
				data.put(identity.getDeneWordName(), rs.getString(2).replace("\"", ""));
				toReturn.put(data);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					if(connection!=null)connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return toReturn;
	}
	public int deleteIndex(String index, String identityString){
		Connection connection = null;
		Statement statement = null;
		int value=-1;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			String sql = "delete from DeneWordIndex where identityString='"+ identityString  +"' and indexName='"+ index +"'";
			value = statement.executeUpdate(sql);
			//System.out.println("deleted " + value + " sql:" + sql);
			statement.close();
			connectionPool.closeConnection(connection);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));

		}finally{
			if(connection!=null){
				try {
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		} 
		return value;
	}

	public ArrayList<Map.Entry<JSONObject, Long>> getPulseForRangeForOrganism(String teleonomeName, String startPulseTimestampString, String endPulseTimestampString){
		Connection connection = null;
		Statement statement = null;
		ArrayList arrayList = new ArrayList();
		ResultSet rs=null;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 
			//select * from organismpulse where teleonomeName='Tlaloc' and createdOn >='2016-03-07'
			//select * from organismpulse where teleonomeName='Tlaloc' and createdOn >='2016-03-07' and createdOn <'2016-03-08'

			String sql = "select  data as text from organismpulse where teleonomeName='"+ teleonomeName +"' and createdOn>='"+startPulseTimestampString +"' and createdOn<='" + endPulseTimestampString +"' order by createdOn asc";
			//System.out.println("in getPulseForRangeForOrganism sql=" + sql);
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			Long L;
			while(rs.next()){

				data = new JSONObject(rs.getString(1));
				L = new Long(data.getLong("Pulse Timestamp in Milliseconds"));

				arrayList.add(new AbstractMap.SimpleEntry<JSONObject,Long>(data, L));
				Collections.sort(arrayList, new Comparator<Map.Entry<?, Long>>(){
					public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}});

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return arrayList;
	}
	public int getNumberOfPulsesFromTime(long startPeriod)throws SQLException{
		Connection connection=null;
		Statement statement = null;
		int count=0;
		ResultSet rs =null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 

			String sql = "select count(createdOn) from pulse where pulseTimeMillis>="+startPeriod;
			rs = statement.executeQuery(sql);

			while(rs.next()){
				count = rs.getInt(1);	
			}
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		return count;
	}

	public JSONArray getPulsesFromTime(long startPeriod, int numberOfPulses)throws SQLException{
		Connection connection=null;
		Statement statement = null;
		JSONArray array = new JSONArray();

		ResultSet rs =null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 

			String sql = "select data as text from pulse where pulseTimeMillis>="+startPeriod +"  order by pulseTimeMillis asc limit " + numberOfPulses;
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			while(rs.next()){
				data = new JSONObject(rs.getString(1));
				array.put(data);

			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		return array;
	}
	public int getNumberOrganismPulsesFromTime(String teleonomeName, long startPeriod, int numberOfPulses)throws SQLException{
		Connection connection=null;
		Statement statement = null;
		int numberOfPulsesRemaining=0;
		ResultSet rs =null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 

			String sql = "select count(createdon) from organismpulse where teleonomeName='" + teleonomeName + "' and pulseTimeMillis>="+startPeriod;
			logger.info("sql=" + sql);
			rs = statement.executeQuery(sql);
			while(rs.next()){
				numberOfPulsesRemaining = rs.getInt(1);
			}
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		return numberOfPulsesRemaining;
	}

	public JSONArray getOrganismPulsesFromTime(String teleonomeName, long startPeriod, int numberOfPulses)throws SQLException{
		Connection connection=null;
		Statement statement = null;
		JSONArray array = new JSONArray();

		ResultSet rs =null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 

			String sql = "select data as text from organismpulse where teleonomeName='" + teleonomeName + "' and pulseTimeMillis>="+startPeriod +"  order by pulseTimeMillis asc limit " + numberOfPulses;
			logger.info("sql=" + sql);
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			while(rs.next()){
				data = new JSONObject(rs.getString(1));
				array.put(data);

			}
		}catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		return array;
	}

	public ArrayList<Map.Entry<JSONObject, Long>> getPulseForRange(long startPeriod, long endPeriod){
		ArrayList arrayList = new ArrayList();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 

			String sql = "select pulseTimeMillis, data as text from pulse where pulseTimeMillis>="+startPeriod +" and pulseTimeMillis<=" + endPeriod +" order by pulseTimeMillis asc";
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			Long L;
			while(rs.next()){
				L = new Long(rs.getLong(1));
				data = new JSONObject(rs.getString(2));

				arrayList.add(new AbstractMap.SimpleEntry<JSONObject,Long>(data, L));
				Collections.sort(arrayList, new Comparator<Map.Entry<?, Long>>(){
					public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
						return o1.getValue().compareTo(o2.getValue());
					}});

			}
			statement.close();
			connectionPool.closeConnection(connection);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return arrayList;
	}
	public boolean storeMutationEvent(JSONObject mutationEventData){
		String sql="";
		Connection connection = null;
		Statement statement=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			//.replace("\"", "\\\"")
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));
			sql = "insert into MutationEvent (createdOn,data) values(" + createdOn + ",'" + mutationEventData.toString() +"')";
			int result = statement.executeUpdate(sql);

			toReturn=true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("bad sql:" + sql);
			logger.warn(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return toReturn;
	}

	public boolean storeIndex(String indexName, String identityString, JSONArray todayIndex){
		String sql="";
		Connection connection = null;
		Statement statement = null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			//.replace("\"", "\\\"")
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));
			sql = "insert into DeneWordIndex(indexName,identityString,data) values('"+indexName +"','" + identityString + "','" + todayIndex.toString() +"')";
			int result = statement.executeUpdate(sql);
			statement.close();
			connectionPool.closeConnection(connection);
			toReturn=true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("bad sql:" + sql);
			logger.warn(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return toReturn;
	}

	public Hashtable getIndexData(String indexName){
		String sql="";
		Hashtable toReturn = new Hashtable();
		Connection connection = null;
		Statement statement=null;
		ResultSet rs=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			//.replace("\"", "\\\"")
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));
			sql = "select identityString,data as text from DeneWordIndex where indexName='"+indexName +"'";
			rs = statement.executeQuery(sql);
			String identityString,data;
			while(rs.next()){
				identityString = rs.getString(1);
				data = rs.getString(2);
				toReturn.put(identityString, data);

			}


		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println("bad sql:" + sql);
			logger.warn(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return toReturn;
	}

	public boolean storeRemembered(long recordMillis,String destination, String sourceTeleonomeName, String sourceTeleonomeIPAddress, String learnOtherHistoryTeleonomeName) {

		String sql="";
		Connection connection=null;
		Statement statement=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			//.replace("\"", "\\\"")
			long importedOnMillis=System.currentTimeMillis();
			String importedOnTimestamp = getPostgresDateString(new Timestamp(importedOnMillis));
			String recordTimestamp = getPostgresDateString(new Timestamp(recordMillis));

			sql = "insert into RememberedRecords(importedOnMillis,importedOnTimestamp,recordMillis, recordTimestamp,destination,sourceTeleonomeName,sourceTeleonomeIPAddress, learnOtherHistoryTeleonomeName) values(" + importedOnMillis + ",'" + importedOnTimestamp + "',"+recordMillis+ ",'"+recordTimestamp+"','"+destination+"','" + sourceTeleonomeName + "','" + sourceTeleonomeIPAddress+"','" + learnOtherHistoryTeleonomeName +"')";
			int result = statement.executeUpdate(sql);

			toReturn= true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//System.out.println("bad sql:" + sql);
			Utils.getStringException(e);
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return toReturn;
	}


	public boolean storePulse(long timestampInMills,String pulseData){

		//
		// before doing anything, check that the pulse is 
		try {
			JSONObject testJSON = new JSONObject(pulseData);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		String sql="";
		Connection connection=null;
		Statement statement=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			//.replace("\"", "\\\"")
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));
			sql = "insert into Pulse (createdOn,pulseTimeMillis,data) values(" + createdOn + ","+timestampInMills+",'" + pulseData +"')";
			int result = statement.executeUpdate(sql);

			toReturn= true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//System.out.println("bad sql:" + sql);
			Utils.getStringException(e);
			logger.debug(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					statement.close();
					connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return toReturn;
	}

	public int getNumberRecordsRemembered(long importedOnMillis, String destination, String learnOtherHistoryTeleonomeName) {
		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		int count=0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql="select count(recordMillis) from RememberedRecords where importedOnMillis>"+importedOnMillis + " and destination='" + destination +"'" + " and learnOtherHistoryTeleonomeName='" + learnOtherHistoryTeleonomeName + "'";

			rs = statement.executeQuery(sql);
			JSONObject data=null;
			while(rs.next()){
				count=rs.getInt(1);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return count;
	}
	public boolean containsPulse(long milliSeconds) {

		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		boolean contains=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select count(createdon) from Pulse where pulsetimemillis="+ milliSeconds;
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			while(rs.next()){
				if(rs.getInt(1)>0)contains=true;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return contains;
	}

	public boolean containsOrganismPulse(long milliSeconds, String teleonomeName) {

		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		boolean contains=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select count(createdon) from OrganismPulse where teleonomeName='"+ teleonomeName +"' and pulsetimemillis="+ milliSeconds;
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			while(rs.next()){
				if(rs.getInt(1)>0)contains=true;
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return contains;
	}


	public boolean storeOrganismPulse(String teleonomeName,String teleonomeAddress ,String pulseData, String status, String operationMode, String identity, long pulseTimeMillis){

		//
		// before doing anything, check that the pulse is 
		try {
			JSONObject testJSON = new JSONObject(pulseData);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		String sql="";
		Connection connection=null;
		Statement statement = null;
		boolean toReturn=false;
		ResultSet rs=null;
		try {

			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));

			sql = "insert into OrganismPulse (teleonomeName, createdOn,pulseTimeMillis,networkAddress,data) values('"+ teleonomeName + "',"+ createdOn+","+pulseTimeMillis +",'" + teleonomeAddress + "','"+ pulseData +"')";
			int result = statement.executeUpdate(sql);
			//
			//
			// now delete the data for this teleonome from the lastOrganismPulse
			//
			sql="delete from lastOrganismPulse where teleonomeName='" + teleonomeName +"'";
			result = statement.executeUpdate(sql);
			//
			// and insert it again
			////
			sql = "insert into lastOrganismPulse (teleonomeName, createdOn,pulseTimeMillis,networkAddress,data) values('"+ teleonomeName + "',"+ createdOn+","+pulseTimeMillis +",'" + teleonomeAddress + "','"+ pulseData +"')";
			result = statement.executeUpdate(sql);

			//
			// now check if we have this teleonome in the database
			//
			sql = "select count(name) from Teleonome where name='" + teleonomeName + "'";
			rs = statement.executeQuery(sql);
			int number=0;
			while(rs.next()){
				number = rs.getInt(1);
			}
			if(number==0){
				sql = "insert into Teleonome (identity, operationMode, status,lastPulseDate, name) values('"+ identity + "', '"+operationMode + "', '"+ status+"', " + createdOn + ",'" + teleonomeName + "')";
				result = statement.executeUpdate(sql);
			}else{
				sql = "update Teleonome set identity='"+ identity + "', operationMode='"+operationMode + "', status ='"+ status+"', lastPulseDate=" + createdOn + "  where Name='" + teleonomeName + "'";
				result = statement.executeUpdate(sql);

			}



			toReturn=true;
		} catch (SQLException e) {
			//System.out.println("bad sql:" + sql);
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return toReturn;
	}

	public JSONObject getLastDigitalGeppettoPulse(){
		Connection connection = null;
		Statement statement = null;
		ResultSet rs =null;
		JSONObject data=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select data from DigitalGeppettoPulse order by createdon desc limit 1";
			rs = statement.executeQuery(sql);
			while(rs.next()){
				data = new JSONObject(rs.getObject(1).toString());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}
		}
		return data;
	}


	public boolean storeDigitalGeppettoPulse(String organismName,String orgAddress ,String pulseData, long pulseTimeMillis){
		String sql="";
		Connection connection = null;
		Statement statement=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));

			sql = "insert into DigitalGeppettoPulse (organismName,createdOn, orgAddress,data,pulseTimeMillis) values('"+ organismName + "',"+ createdOn+",'" + orgAddress + "','"+ pulseData +"',"+pulseTimeMillis + ")";
			int result = statement.executeUpdate(sql);


			toReturn= true;
		} catch (SQLException e) {
			System.out.println("bad sql:" + sql);
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}


	public boolean markAllNonExecutedAsSkipped(){
		Connection connection = null;
		Statement statement = null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "update CommandRequests set status='"+ TeleonomeConstants.COMMAND_REQUEST_SKIPPED_AT_INIT +"'   where status='" + TeleonomeConstants.COMMAND_REQUEST_NOT_EXECUTED +"'";
			statement.executeUpdate(sql);
			toReturn= true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}	

	public int requestCommandToExecute(String command,  String payLoad){
		//System.out.println(Utils.generateMethodTrace());
		int id=-1;
		Connection connection = null;
		Statement statement=null;
		ResultSet rs=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "insert into CommandRequests (createdOn,command, status, payLoad) values ("+ System.currentTimeMillis() + ",'"+ command+"','"+ TeleonomeConstants.COMMAND_REQUEST_NOT_EXECUTED +"','" + payLoad +"') returning id";
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			CommandRequest aCommandRequest = new CommandRequest();

			while(rs.next()){
				id = rs.getInt(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

		}
		//System.out.println("erturnign from createcommand id=" + id + " command=" + command);
		
		return id;

	}

	public boolean isCommandCompleted(int id){
		boolean toReturn=false;
		Connection connection = null;;
		Statement statement = null;
		String sql = "select status from CommandRequests where id="+ id;
		ResultSet rs = null;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			CommandRequest aCommandRequest = new CommandRequest();
			while(rs.next()){
				if(rs.getString(1).equals(TeleonomeConstants.COMMAND_REQUEST_EXECUTED)){
					toReturn= true;
				}
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}

	public CommandRequest getNextCommandToExecute(){

		Connection connection = null;
		Statement statement = null;
		String sql = "select id,command, payload from CommandRequests where status='"+ TeleonomeConstants.COMMAND_REQUEST_NOT_EXECUTED +"'  order by createdOn asc limit 1";
		ResultSet rs = null;
		CommandRequest aCommandRequest = new CommandRequest();


		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			String dataPayload;
			boolean found=false;
			while(rs.next()){
				aCommandRequest.setId(rs.getInt(1));
				aCommandRequest.setCommand(rs.getString(2));
				aCommandRequest.setDataPayload(rs.getString(3));
				found=true;
			}
			if(!found){
				aCommandRequest= null;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return aCommandRequest;
	}


	public boolean markCommandCompleted(int id){
		Connection connection = null;
		Statement statement = null;
		String sql = "update CommandRequests set executedOn="+System.currentTimeMillis()+", status='"+ TeleonomeConstants.COMMAND_REQUEST_EXECUTED +"'   where id=" + id;

		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			statement.executeUpdate(sql);
			toReturn=true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}	

	public JSONObject getLastPulse(){
		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		JSONObject data=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select data from Pulse order by createdOn desc limit 1";
			rs = statement.executeQuery(sql);

			while(rs.next()){
				data = new JSONObject((String)rs.getObject(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return data;
	}

	public JSONObject getPulseByCreatedOn(String createdOn){
		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		JSONObject data=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select data from Pulse where createdOn ="+createdOn;
			rs = statement.executeQuery(sql);

			while(rs.next()){
				data = new JSONObject((String)rs.getObject(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return data;
	}

	public Hashtable generateLastPulseIndex(){
		Hashtable lastPulseIndex = new Hashtable();

		Connection connection = null;
		Statement statement =null;
		ResultSet rs=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select teleonomeName,data from lastOrganismPulse";
			rs = statement.executeQuery(sql);
			String teleonomeName;
			String data=null;
			while(rs.next()){
				teleonomeName = rs.getString(1);
				data = rs.getObject(2).toString();
				lastPulseIndex.put(teleonomeName, data);
			}


		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return lastPulseIndex;
	}

	public JSONObject getLastPulse(String teleonomeName){

		Connection connection = null;
		Statement statement = null;
		ResultSet rs =null;
		JSONObject data=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select data from lastOrganismPulse where TeleonomeName='"+teleonomeName +"'";
			rs = statement.executeQuery(sql);
			while(rs.next()){
				data = new JSONObject(rs.getObject(1).toString());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				rs.close();
				statement.close();
				connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}
		}
		return data;
	}

	public Vector<Teleonome> getAllTeleonomes(){

		Connection connection = null;
		Statement statement = null;
		String sql = "select * from Teleonome order by name";
		ResultSet rs = null;
		String a=null;
		Teleonome teleonome;
		String name="";
		boolean active=false;
		String inetAddressString="";
		Vector<Teleonome> toReturn = new Vector();


		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			while(rs.next()){
				name = rs.getString(1);
				active = rs.getBoolean(2);
				inetAddressString =  rs.getString(3);

				teleonome = new Teleonome();
				teleonome.setName(name);
				teleonome.setActive(active);
				teleonome.setInetAddressString(inetAddressString);
				toReturn.addElement(teleonome);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}

	public boolean teleonomeExist(String teleonomeName){
		Connection connection = null;
		Statement statement = null;
		String sql = "select count(name) from Teleonome where name='"+teleonomeName +"'";
		ResultSet rs = null;
		int count=0;
		boolean toReturn=false;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			while(rs.next()){
				count = rs.getInt(1);
			}
			statement.close();
			connectionPool.closeConnection(connection);
			if(count>0)toReturn= true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}

	public Teleonome getTeleonomeByName(String teleonomeName){

		Connection connection = null;
		Statement statement = null;
		String sql = "select name, status, networkAddress  from Teleonome where name='" + teleonomeName + "'";
		ResultSet rs = null;
		String a=null;
		Teleonome teleonome=null;
		String name="", status;
		boolean active=false;
		String inetAddressString="";
		Vector<Teleonome> toReturn = new Vector();


		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			while(rs.next()){
				name = rs.getString(1);
				status = rs.getString(2);
				if(status.equals(TeleonomeConstants.TELEONOME_STATUS_DISCOVERED) || status.equals(TeleonomeConstants.TELEONOME_STATUS_ACTIVE)) {
					active=true;
				}
				inetAddressString =  rs.getString(3);
				teleonome = new Teleonome();
				teleonome.setName(name);
				teleonome.setActive(active);
				teleonome.setInetAddressString(inetAddressString);

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		} 
		return teleonome;
	}

	public boolean registerTeleonome(String teleonomeName, String status, String operationMode, String identity, String networkName, String teleonomeAddress){
		Teleonome teleonome =getTeleonomeByName( teleonomeName);
		Connection connection =null;
		Statement statement = null;
		String sql = "";
		boolean toReturn=false;
		int result=0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();


			if( teleonome!=null){
				//
				// exist already, update it
				//
				sql = "update Teleonome set identity='"+ identity+"', operationMode='"+ operationMode +"', status='" +status + "',networkName='"+ networkName + "', networkAddress='" + teleonomeAddress +"'  where name='"+ teleonomeName + "'";
				result = statement.executeUpdate(sql);
				toReturn= true;
			}else{
				//
				// does not exist, create it
				//
				sql = "insert into Teleonome (name,status,operationMode,identity, networkName, networkAddress) values('"+ teleonomeName + "','" + status+ "','" + operationMode +"','" + identity +"','" +  networkName+ "','" +teleonomeAddress+"')";
				result = statement.executeUpdate(sql);
				toReturn= true;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(statement!=null)statement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}
			
		} 
			return toReturn;
	}

		public static String getSimplePostgresDateString(Timestamp ts){
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(ts.getTime());
			String monthValue="";
			String dateValue="";
			String hourValue="";
			String minuteValue="";
			String secondValue="";

			if(calendar.get(Calendar.MONTH)>8)monthValue = "" + (calendar.get(Calendar.MONTH) +1);
			else monthValue = "0" + (calendar.get(Calendar.MONTH) +1);

			if(calendar.get(Calendar.DATE)>9)dateValue = "" + (calendar.get(Calendar.DATE));
			else dateValue = "0" + (calendar.get(Calendar.DATE));

			String text = calendar.get(Calendar.YEAR ) + "-" + monthValue + "-" + dateValue  ;
			return  text;

		}

		public static String getPostgresDateString(Timestamp ts){
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(ts.getTime());
			String monthValue="";
			String dateValue="";
			String hourValue="";
			String minuteValue="";
			String secondValue="";

			if(calendar.get(Calendar.MONTH)>8)monthValue = "" + (calendar.get(Calendar.MONTH) +1);
			else monthValue = "0" + (calendar.get(Calendar.MONTH) +1);

			if(calendar.get(Calendar.DATE)>9)dateValue = "" + (calendar.get(Calendar.DATE));
			else dateValue = "0" + (calendar.get(Calendar.DATE));


			if(calendar.get(Calendar.HOUR_OF_DAY)>9)hourValue = "" + (calendar.get(Calendar.HOUR_OF_DAY));
			else hourValue = "0" + (calendar.get(Calendar.HOUR_OF_DAY));


			if(calendar.get(Calendar.MINUTE)>9)minuteValue = "" + (calendar.get(Calendar.MINUTE));
			else minuteValue = "0" + (calendar.get(Calendar.MINUTE));

			if(calendar.get(Calendar.SECOND)>9)secondValue = "" + (calendar.get(Calendar.SECOND));
			else secondValue = "0" + (calendar.get(Calendar.SECOND));

			String text = dateValue + monthValue + (calendar.get(Calendar.YEAR ))  + " " + hourValue +":"+   minuteValue +":"+ secondValue;
			return  "to_timestamp('"+ text + "', 'DDMMYYYY HH24:MI:SS')";

		}

		/**
		 *  the purpose of this method is to take a pulse, extract the purpose chains
		 *   and store them in a table that would make it easier to index
		 * @param pulse
		 */
		public void storePurposeForIndexing(JSONObject pulse){
			try {
				long pulseTimestamp = pulse.getLong("Pulse Timestamp in Milliseconds");
				JSONObject denomeJSONObject = pulse.getJSONObject("Denome");
				String teleonomeName =  denomeJSONObject.getString("Name");
				//
				// now parse them
				JSONArray nucleiArray = denomeJSONObject.getJSONArray("Nuclei");

				JSONObject aJSONObject,purposeNucleus=null;
				String name;
				for(int i=0;i<nucleiArray.length();i++){
					aJSONObject = (JSONObject) nucleiArray.get(i);
					name = aJSONObject.getString("Name");
					if(name.equals(TeleonomeConstants.NUCLEI_PURPOSE)){
						purposeNucleus= aJSONObject;
					}
				}

				if(purposeNucleus==null)return;

				JSONArray deneChainsPurpose = purposeNucleus.getJSONArray("DeneChains");
				JSONObject deneChain;
				String identityString;
				boolean result;
				for(int i=0;i<deneChainsPurpose.length();i++){
					deneChain = deneChainsPurpose.getJSONObject(i);
					//
					// index everything except the processing logic
					if(!deneChain.getString("Name").equals( TeleonomeConstants.DENECHAIN_ACTUATOR_LOGIC_PROCESSING)){
						identityString = "@" + teleonomeName + ":" + TeleonomeConstants.NUCLEI_PURPOSE + ":" + deneChain.getString("Name");
						result = storePurposeChainInfo(identityString, pulseTimestamp,deneChain);
						//System.out.println("storing purpose chain index " + identityString + " pulseTimestamp: " + pulseTimestamp + " was " + result);
					}
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

		}

		public ArrayList<Map.Entry<JSONObject, Long>> getPurposeChainForIndexing(String identityString, long startPulseTimestampMillis, long endPulseTimestampMillis){
			Connection connection = null;
			Statement statement = null;
			ArrayList arrayList = new ArrayList();
			ResultSet rs=null;
			try {
				connection = connectionPool.getConnection();
				statement = connection.createStatement();

				ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 

				String sql = "select  pulseTimeMillis,data as text from PurposeChainInfo where identityString='"+ identityString +"' and pulseTimeMillis>="+startPulseTimestampMillis +" and pulseTimeMillis<=" + endPulseTimestampMillis +" order by pulseTimeMillis asc";
				//System.out.println("getPurposeChainForIndexing:" + sql);
				rs = statement.executeQuery(sql);
				JSONObject data=null;
				Long L;
				while(rs.next()){
					L = new Long(rs.getLong(1));
					data = new JSONObject(rs.getString(2));

					arrayList.add(new AbstractMap.SimpleEntry<JSONObject,Long>(data, L));
					Collections.sort(arrayList, new Comparator<Map.Entry<?, Long>>(){
						public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
							return o1.getValue().compareTo(o2.getValue());
						}});
				}


			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}finally{
				try {
					if(rs!=null)rs.close();
					if(statement!=null)statement.close();
					if(connection!=null)connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}

			}
			return arrayList;
		}

		private boolean storePurposeChainInfo(String identityString, long pulseTimestamp, JSONObject deneChain) {

			String sql="";
			Connection connection = null;
			Statement statement = null;
			boolean toReturn=false;
			try {
				connection = connectionPool.getConnection();
				statement = connection.createStatement();
				sql = "insert into PurposeChainInfo (identityString,pulseTimeMillis,data) values('"+ identityString + "'," + pulseTimestamp+ ",'" + deneChain.toString() +"')";
				//System.out.println("storePurposeChainInfo=" + sql);
				int result = statement.executeUpdate(sql);
				statement.close();
				connectionPool.closeConnection(connection);
				toReturn= true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println("bad sql=" + sql);
				logger.warn(Utils.getStringException(e));
			}finally{
				try {
					if(statement!=null)statement.close();
					if(connection!=null)connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}

			}
			return toReturn;
		}


		public static String getUniqueIndex(){
			java.rmi.dgc.VMID v = new java.rmi.dgc.VMID();
			return v.toString();
		}

		public boolean unwrap(TimeZone timeZone, String teleonomeName, long pulseTimeMillis, String identityString, String valueType, Object value) {
			String sql="";
			Connection connection = null;
			PreparedStatement preparedStatement = null;
			boolean toReturn=false;
			try {
				connection = connectionPool.getConnection();
				//statement = connection.createStatement();
				java.sql.Timestamp dateTimeValue = new java.sql.Timestamp(pulseTimeMillis);
				
				sql = "insert into RememberedDeneWords (time, teleonomeName,identityString,value) values(?,?,?,?)";
				logger.debug("storePurposeChainInfo=" + sql);
				Calendar calendarTimeZone = Calendar.getInstance(timeZone);  
				
				preparedStatement = connection.prepareStatement(sql);
				preparedStatement.setTimestamp(1, dateTimeValue, calendarTimeZone);
				preparedStatement.setString(2, teleonomeName);
				preparedStatement.setString(3, identityString);
				double d = 0;
				if(valueType.equals(TeleonomeConstants.DATATYPE_DOUBLE)) {
					
					if(value instanceof String) {
						 d = Double.parseDouble((String)value);
					}else if(value instanceof Integer) {
						//
						// if the value is rendered as 0 and the value type is double
						// it gets interpreted as an integer, 
						 d = ((Integer)value).doubleValue();
					}else {
						 d = (double)value;
					}
					
					preparedStatement.setDouble(4, d);
				}else if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)) {
					
					if(value instanceof String) {
						 d = Integer.getInteger((String)value).doubleValue();
					}else {
						d = ((Integer)value).doubleValue();
					}
					
					
					preparedStatement.setDouble(4, d);
				}
				int result = preparedStatement.executeUpdate();
				preparedStatement.close();
				connectionPool.closeConnection(connection);
				toReturn= true;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println("bad sql=" + sql);
				logger.warn(Utils.getStringException(e));
			}finally{
				try {
					if(preparedStatement!=null)preparedStatement.close();
					if(connection!=null)connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			}
			return toReturn;
		}

//		public ArrayList getHistoricalDeneWordValueByIdentity(Identity identity) {
//			String sql="select createdon, DeneWord -> 'Value' As CurrentPulse from pulse p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  " + 
//					"jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, " + 
//					"jsonb_array_elements(Dene->'DeneWords') as DeneWord where  Nucleus->>'Name'='"+identity.getNucleusName()+"' and DeneChain->>'Name'='"+identity.getDenechainName()+"' " + 
//					"and Dene->>'Name'='"+identity.getDeneName()+"' and DeneWord->>'Name'='"+identity.getDeneWordName()+"' order by createdon;";
//			
//			Connection connection = null;
//			Statement statement = null;
//			ArrayList arrayList = new ArrayList();
//			ResultSet rs=null;
//			try {
//				connection = connectionPool.getConnection();
//				statement = connection.createStatement();
//
//				
//				//System.out.println("getPurposeChainForIndexing:" + sql);
//				rs = statement.executeQuery(sql);
//				JSONObject data=null;
//				Timestamp createdOnResult;
//				while(rs.next()){
//					createdOnResult = rs.getTimestamp(1);
//					data = new JSONObject(rs.getString(2));
//					arrayList.add(new AbstractMap.SimpleEntry<JSONObject,Long>(data, L));
//					
//				}
//
//
//			} catch (SQLException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				logger.warn(Utils.getStringException(e));
//			}finally{
//				try {
//					if(rs!=null)rs.close();
//					if(statement!=null)statement.close();
//					if(connection!=null)connectionPool.closeConnection(connection);
//				} catch (SQLException e) {
//					// TODO Auto-generated catch block
//					logger.debug(Utils.getStringException(e));
//				}
//
//			}
//			return arrayList;
//		}

	}
