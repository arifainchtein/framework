package com.teleonome.framework.persistence;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.postgresql.util.PGobject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.denome.Teleonome;
import com.teleonome.framework.hypothalamus.CommandRequest;
import com.teleonome.framework.interfaces.PersistenceInterface;
import com.teleonome.framework.utils.Utils;

public class PostgresqlPersistenceManager implements PersistenceInterface{

	private static PostgresqlPersistenceManager aPostgresqlPersistenceManager;
	//private final String DATABASE_URL = "postgres://postgres:sazirac@localhost:5432/teleonome";
	private TeleonomeConnectionPool connectionPool;
	private Logger logger;  
	private static DecimalFormat twoDecimalsFormat = new DecimalFormat(".##");
	DateFormat managedTableDateFormat = new SimpleDateFormat("yyyy_M_d");

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
		int port=-1;
		String pwd="";
		try {
			InputStream input=null;
			try{
				input = new FileInputStream("lib/app.properties");

				Properties prop = new Properties();

				// load a properties file
				prop.load(input);

				// get the property value and print it out
				port = Integer.parseInt(prop.getProperty("port"));
				pwd = prop.getProperty("pwd");
			}catch(IOException e) {
				logger.warn(Utils.getStringException(e));
			}finally {
				if(input!=null) {
					try {
						input.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						logger.warn(Utils.getStringException(e));
					}
				}
			}
			System.out.println("port=" + port);


			String DATABASE_URL = "postgres://postgres:"+pwd+"@localhost:"+ port +"/teleonome";
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
	/**
	 * this method will create the partitions in the database
	 * for all partitioned tables.  Currently, these are
	 * the tables:
	 * 	Pulse 
		Organism Pulse 
		Remembered DeneWords 
		Command Requests 
		Mutation Event 

	 * @param cal it contains the valid year, month and date 
	 *        note that the time fields are ignored
	 * @return
	 */
	public boolean createDailyPartitions(Calendar cal) {
		boolean toReturn=false;
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int date = cal.get(Calendar.DATE);

		String[] tableNames = {TeleonomeConstants.PULSE_TABLE,TeleonomeConstants.ORGANISMPULSE_TABLE,TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE,TeleonomeConstants.COMMAND_REQUESTS_TABLE,TeleonomeConstants.MUTATION_EVENT_TABLE};
		String sql = "";
		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			int value;
			for(int i=0;i<tableNames.length;i++) {
				sql = "CREATE TABLE "+tableNames[i]+"_"+year + "_"+ month + "_"+ date + " as table  "+tableNames[i]+" with no data";
				logger.debug("createDayPartitions, sql=" +sql);
				value = statement.executeUpdate(sql);
				logger.debug("createDayPartitions, value=" +value);
			}
			toReturn=true;
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

		return toReturn;
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



	private ArrayList<String> getAllTableNamesForManagedTable(String prefix) {
		ArrayList<String> toReturn = new ArrayList();
		ArrayList<AbstractMap.SimpleEntry<String,Date>> arrayList = new ArrayList();
		String sql = "SELECT table_name FROM information_schema.tables  WHERE table_schema='public'  AND table_type='BASE TABLE' and table_name like '" + prefix + "_%'";
		logger.debug("getAllTableNamesForManagedTable,sql=" + sql);
		Connection connection = null;
		Statement statement = null;
		ResultSet rs=null;	
		Date tableDate;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			String tableName;
			while(rs.next()){
				tableName = rs.getString(1);
				logger.debug("line 299, tableName=" + tableName);
				tableDate = this.getDateForManagedTable(prefix, tableName);
				arrayList.add(new AbstractMap.SimpleEntry<String,Date>(tableName, tableDate));
				Collections.sort(arrayList, new Comparator<Map.Entry<?, Date>>(){
					public int compare(Map.Entry<?, Date> o1, Map.Entry<?, Date> o2) {
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
					if(connection!=null)connectionPool.closeConnection(connection);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		String tableName;
		for (Map.Entry<String, Date> entry2 : arrayList) {
			tableName = entry2.getKey();

			toReturn.add(tableName);
		}
		logger.debug("getAllTableNamesForManagedTable returning =" + toReturn);
		return toReturn;
	}



	private boolean dropTable(String tableName) {
		Connection connection=null;
		Statement statement=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String command = "drop table " + tableName;
			logger.debug(command);
			statement.executeUpdate(command);
			toReturn=true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.debug(s);

		}finally{

			try {
				if(statement!=null)statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}
			if(connection!=null)closeConnection(connection);
		}

		return toReturn;
	}



	private Date getDateForManagedTable(String prefix, String dailyTableName) {

		// dailyTableName would be something like OrganismPulse_YYYY_MM_dd
		String dateString = dailyTableName.substring(prefix.length()+1);
		Date date=null;
		try {
			logger.debug("line 376 dateString=" + dateString);
			date = managedTableDateFormat.parse(dateString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}
		return date;
	}



	private int deleteFromManagedTabledByPeriod(String tableName, long millisToDeleteFrom) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(millisToDeleteFrom);
		Date limitDate = cal.getTime();
		//
		// now get a list of all the tables that exists and are older than millisToDeleteFrom, which will be deleted
		//
		ArrayList allTables = getAllTableNamesForManagedTable(tableName);
		String dailyTableName, dateString;
		Date date;

		boolean deleteResult=false;
		int deleteCounter=0;
		for(int i=0;i<allTables.size();i++) {
			//Pulse_ 
			dailyTableName = ((String)allTables.get(i));
			date = getDateForManagedTable(tableName,dailyTableName);
			if(limitDate.after(date)) {
				logger.debug("dropping " + dailyTableName);
				deleteResult = dropTable(dailyTableName);
				if(deleteResult)deleteCounter++;
			}
		}
		return deleteCounter;
	}

	public int deleteByPeriodFromOrganismPulse( long millisToDeleteFrom, String teamInfo) {

		return deleteFromManagedTabledByPeriod(TeleonomeConstants.ORGANISMPULSE_TABLE,  millisToDeleteFrom);
		//		String command = "with deleted as (delete from organismpulse where pulsetimemillis < " +  millisToDeleteFrom + " RETURNING *) SELECT count(*) FROM deleted";
		//		if(!teamInfo.equals("")) {
		//			command +=command + " " + teamInfo;
		//		}
		//return deleteByPeriod( command);
	}

	public int deleteByPeriodFromPulse(long millisToDeleteFrom) {
		return deleteFromManagedTabledByPeriod(TeleonomeConstants.PULSE_TABLE,  millisToDeleteFrom);
	}

	public int deleteByPeriodFromRememberedDeneWords(String columnName, String columnValue, long millisToDeleteFrom) {
		return deleteFromManagedTabledByPeriod(TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE,  millisToDeleteFrom);

		//		String command = "with deleted as (delete from remembereddenewords where "+ columnName + "='" +columnValue + "' and timeMillis  < " +  millisToDeleteFrom+ " RETURNING *) SELECT count(*) FROM deleted";
		//		return deleteByPeriod( command);
	}

	public int deleteByPeriodFromCommandRequests(long millisToDeleteFrom) {
		return deleteFromManagedTabledByPeriod(TeleonomeConstants.COMMAND_REQUESTS_TABLE,  millisToDeleteFrom);

		//		String command = "with deleted as (delete from CommandRequests where createdon  < " +  millisToDeleteFrom+ " RETURNING *) SELECT count(*) FROM deleted";
		//		return deleteByPeriod( command);
	}

	public int deleteByPeriodFromMutationEvent(long millisToDeleteFrom) {
		return deleteFromManagedTabledByPeriod(TeleonomeConstants.MUTATION_EVENT_TABLE,  millisToDeleteFrom);
		//		String command = "with deleted as (delete from MutationEvent where createdonMillis  < " +  millisToDeleteFrom+ " RETURNING *) SELECT count(*) FROM deleted";
		//		return deleteByPeriod( command);
	}

	private int deleteTable(String command) {
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		int numberDeleted=0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			logger.debug(command);
			rs = statement.executeQuery(command);
			while(rs.next()) {
				numberDeleted = rs.getInt(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.debug(s);

		}finally{

			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}
			if(connection!=null)closeConnection(connection);
		}

		return numberDeleted;
	}

	private int deleteByPeriod(String command) {
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		int numberDeleted=0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			logger.debug(command);
			rs = statement.executeQuery(command);
			while(rs.next()) {
				numberDeleted = rs.getInt(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			String s = com.teleonome.framework.utils.Utils.getStringException(e);
			System.out.println("line 74 exception " + s);
			logger.debug(s);

		}finally{

			try {
				if(rs!=null)rs.close();
				if(statement!=null)statement.close();
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
		double totalSize=0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String command = "SELECT pg_size_pretty(pg_total_relation_size(relid))   FROM pg_catalog.pg_statio_user_tables where relname like '"+ tableName+"%'";
			logger.debug("getTableSizeMB, sql=" + command);
			rs = statement.executeQuery(command);
			String sizeString="";
			while(rs.next()){
				sizeString=rs.getString(1);
				//
				// if there are no rows, sizeString will be empty
				if(sizeString.length()>1) {
					String[] tokens = sizeString.split(" ");
					size = Double.parseDouble(tokens[0]);
					String units = tokens[1];
					if(units.equalsIgnoreCase("bytes"))size= size/1000000;
					else if(units.equalsIgnoreCase("KB"))size= size/1000;
					else if(units.equalsIgnoreCase("MB"))size= size;
					else if(units.equalsIgnoreCase("GB"))size= size*1000;
					else if(units.equalsIgnoreCase("TB"))size= size*1000000;
				}else {
					size=0;
				}
				logger.debug("getTableSizeMB, adding size=" + size);
				totalSize+=size;
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

		return totalSize;

	}

	public void closeConnection(Connection con){
		try {
			connectionPool.closeConnection(con);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));

		}
	}

	public PGobject extractOrganismDeneWordAttributeValueByIdentityByPeriod(Identity identity, String attribute, long fromMillis, long untilMillis) {
		String organismTeleonomeName = identity.getTeleonomeName();
		String identityString = identity.toString();
		ArrayList<String> tables = getAllManagedTablesForAPeriod(TeleonomeConstants.ORGANISMPULSE_TABLE,fromMillis, untilMillis);

		String sql = "";

		Connection connection = null;
		Statement statement = null;
		PGobject toReturn = null;
		ResultSet rs=null;
		JSONObject data=null;
		long pulseTimestampMillis;
		String valueType, units;
		Object value;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			for(int i=0;i<tables.size();i++) {
				sql = "select data->>'Pulse Timestamp in Milliseconds', DeneWord -> 'Value Type' As valuetype, DeneWord -> 'Value' As Value, DeneWord -> 'Units' As units, from "+ tables.get(i) +"  p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where  Nucleus->>'Name'='"+identity.getNucleusName() +"' and DeneChain->>'Name'='"+ identity.getDenechainName() + "' and Dene->>'Name'='"+ identity.getDeneName()+"' and DeneWord->>'Name'='"+ identity.getDeneWordName() +"' and teleonomeName='"+ organismTeleonomeName+"' and cast(data->>'Pulse Timestamp in Milliseconds' as BIGINT) > "+ fromMillis+" and cast(data->>'Pulse Timestamp in Milliseconds' as BIGINT)< "+ untilMillis+" order by data->>'Pulse Timestamp in Milliseconds' asc";
				logger.debug("extractOrganismDeneWordAttributeValueByIdentityByPeriod,sql=" + sql);
				rs = statement.executeQuery(sql);

				while(rs.next()){
					pulseTimestampMillis =  rs.getLong(1);
					valueType=rs.getString(2);
					value = rs.getObject(3);
					units = rs.getString(4);
					unwrap(organismTeleonomeName, pulseTimestampMillis, identityString, valueType, value, TeleonomeConstants.REMEMBERED_DENEWORD_SOURCE_PULSE, units);
				}
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


	public PGobject getOrganismDeneWordAttributeLastValueByIdentity(Identity identity, String attribute) {
		String organismTeleonomeName = identity.getTeleonomeName();

		ArrayList<String> allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		String lastTableName =  allTables.get(allTables.size()-1);
		String sql = "select DeneWord -> '"+ attribute+"' As Units from "+lastTableName+ " p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where  Nucleus->>'Name'='"+identity.getNucleusName() +"' and DeneChain->>'Name'='"+ identity.getDenechainName() + "' and Dene->>'Name'='"+ identity.getDeneName()+"' and DeneWord->>'Name'='"+ identity.getDeneWordName() +"' and teleonomeName='"+ organismTeleonomeName+"' limit 1";
		logger.debug("getOrganismDeneWordTimeSeriesByIdentity,sql=" + sql);
		Connection connection = null;
		Statement statement = null;
		PGobject toReturn = null;
		ResultSet rs=null;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			rs = statement.executeQuery(sql);
			JSONObject data=null;
			Long L;
			while(rs.next()){
				toReturn = (PGobject) rs.getObject(1);

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

	public JSONObject getPulseByTimestamp( long timemillis) {

		//
		// first find what table is the user asking
		//
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timemillis);
		String tableName = getTableNameByCalendar(TeleonomeConstants.PULSE_TABLE,  cal);

		String sql = "select data from "+tableName +" where pulsetimemillis="+ timemillis + " limit 1";
		logger.debug("getPulseByTimestamp,sql=" + sql);
		Connection connection = null;
		Statement statement = null;
		JSONObject toReturn = new JSONObject();
		ResultSet rs=null;	

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			JSONObject data=null;
			Long L;
			while(rs.next()){
				//
				// only one record comeback
				//
				String pulse = rs.getString(1);
				//
				// it comes with "" so remove them, also remove the first and last characrets because they are 
				// also ""



				try {
					logger.debug("point 1");
					toReturn = new JSONObject(pulse);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
				toReturn = new JSONObject(pulse);
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

	public JSONObject getOrganismPulseByTeleonomeNameAndTimestamp(String teleonomeName, long timemillis) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timemillis);
		String tableName = this.getTableNameByCalendar(TeleonomeConstants.ORGANISMPULSE_TABLE, cal);
		String sql = "select data AS Pulse from "+tableName +" where pulsetimemillis="+ timemillis +" and teleonomeName='"+ teleonomeName +"' limit 1";
		logger.debug("getOrganismPulseByTeleonomeNameAndTimestamp,sql=" + sql);
		Connection connection = null;
		Statement statement = null;
		JSONObject toReturn = new JSONObject();
		ResultSet rs=null;	

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			JSONObject data=null;
			Long L;
			while(rs.next()){
				//
				// only one record comeback
				//
				String pulse = rs.getString(1);
				//
				// it comes with "" so remove them, also remove the first and last characrets because they are 
				// also ""



				try {
					logger.debug("point 1");
					toReturn = new JSONObject(pulse);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}

				//				
				//				
				//				pulse = pulse.replace("\"\"", "\"");
				//				if(pulse.substring(0,1).equals("\"")) {
				//					pulse=pulse.substring(1);
				//				}
				//				
				//				
				//				try {
				//					logger.debug("point 2");
				//					toReturn = new JSONObject(pulse);
				//				} catch (JSONException e) {
				//					// TODO Auto-generated catch block
				//					logger.debug(Utils.getStringException(e));
				//				}
				//				
				//				
				//				
				//				if(pulse.substring(pulse.length()-1).equals("\"")) {
				//					pulse=pulse.substring(0,pulse.length()-1);
				//				}
				//				try {
				//					logger.debug("point 3");
				//					toReturn = new JSONObject(pulse);
				//				} catch (JSONException e) {
				//					// TODO Auto-generated catch block
				//					logger.debug(Utils.getStringException(e));
				//				}


				toReturn = new JSONObject(pulse);
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
	public JSONArray getOrganismDeneWordValueOfTheDayByIdentity(Identity identity, Timestamp startTime, Timestamp endTime, boolean last) {
		String organismTeleonomeName = identity.getTeleonomeName();
		String order="asc";
		if(last)order="desc";
		ArrayList<String> allTables = getAllManagedTablesForAPeriod(TeleonomeConstants.ORGANISMPULSE_TABLE,startTime.getTime(), endTime.getTime());
		String createdOnStart = managedTableDateFormat.format(startTime);
		String createdOnEnd = managedTableDateFormat.format(endTime);
		String sql ="";
		Connection connection = null;
		Statement statement = null;
		JSONArray toReturn = new JSONArray();
		ResultSet rs=null;
		JSONObject data=null;
		Long L;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			for(int i=0;i<allTables.size();i++) {
				sql = "select pulsetimemillis,createdOn, DeneWord -> 'Value' As CurrentPulse from "+allTables.get(i)+" p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where createdOn>='"+ createdOnStart + "' and createdOn<='" + createdOnEnd+"' and Nucleus->>'Name'='"+identity.getNucleusName() +"' and DeneChain->>'Name'='"+ identity.getDenechainName() + "' and Dene->>'Name'='"+ identity.getDeneName()+"' and DeneWord->>'Name'='"+ identity.getDeneWordName() +"' and teleonomeName='"+ organismTeleonomeName+"' order by createdOn "+ order;
				logger.debug("getOrganismDeneWordTimeSeriesByIdentity,sql=" + sql);

				rs = statement.executeQuery(sql);
				while(rs.next()){
					data = new JSONObject();
					data.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, rs.getLong(1));
					data.put(TeleonomeConstants.PULSE_TIMESTAMP, rs.getTimestamp(2));
					data.put("Value", rs.getString(2).replace("\"", ""));
					toReturn.put(data);
				}
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

	public JSONArray getOrganismDeneWordTimeSeriesByIdentity(Identity identity, long startTimeMillis, long endTimeMillis) {
		String organismTeleonomeName = identity.getTeleonomeName();
		ArrayList<String> tables = getAllManagedTablesForAPeriod(TeleonomeConstants.ORGANISMPULSE_TABLE,startTimeMillis, endTimeMillis);


		String sql = "";
		logger.debug("getOrganismDeneWordTimeSeriesByIdentity,sql=" + sql);
		Connection connection = null;
		Statement statement = null;
		JSONArray toReturn = new JSONArray();
		ResultSet rs=null;
		JSONObject data=null;
		Long L;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			for(int i=0;i<tables.size();i++) {
				sql = "select pulsetimemillis,DeneWord -> 'Value' As CurrentPulse from "+ tables.get(i)+" p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where pulsetimemillis>="+ startTimeMillis + " and pulsetimemillis<=" + endTimeMillis+" and Nucleus->>'Name'='"+identity.getNucleusName() +"' and DeneChain->>'Name'='"+ identity.getDenechainName() + "' and Dene->>'Name'='"+ identity.getDeneName()+"' and DeneWord->>'Name'='"+ identity.getDeneWordName() +"' and teleonomeName='"+ organismTeleonomeName+"' order by pulsetimemillis asc";
				logger.debug("getOrganismDeneWordTimeSeriesByIdentity,sql=" + sql);
				rs = statement.executeQuery(sql);
				while(rs.next()){

					data = new JSONObject();
					data.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, rs.getLong(1));
					data.put("Value", rs.getString(2).replace("\"", ""));
					toReturn.put(data);
				}
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

	private ArrayList getAllManagedTablesForAPeriod(String tableName, long startTimeMillis, long endTimeMillis) {
		ArrayList<String> allTables = getAllTableNamesForManagedTable(tableName);
		ArrayList<String> toReturn = new ArrayList();

		String dailyTableName, dateString;
		//
		// because this is daily set the time to 00:00:00 for the start
		// and 23:59:00 for the end
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTimeInMillis(startTimeMillis);
		startCalendar.set(Calendar.HOUR_OF_DAY, 0);
		startCalendar.set(Calendar.MINUTE, 0);
		startCalendar.set(Calendar.SECOND, 0);

		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTimeInMillis(endTimeMillis);
		endCalendar.set(Calendar.HOUR_OF_DAY, 23);
		endCalendar.set(Calendar.MINUTE, 59);
		endCalendar.set(Calendar.SECOND, 0);

		Date startDate = startCalendar.getTime();
		Date endDate = endCalendar.getTime();

		Date date;

		boolean deleteResult=false;
		int deleteCounter=0;
		String[] tokens;
		Calendar aCal = Calendar.getInstance();

		for(int i=0;i<allTables.size();i++) {
			//Pulse_ 
			dailyTableName = ((String)allTables.get(i));
			dateString = dailyTableName.substring(tableName.length() + 1);
			logger.debug("getAllManagedTablesForAPeriod, dateString=" + dateString + " dailyTableName " + dailyTableName);
			tokens=dateString.split("_");
			aCal.set(Calendar.YEAR, Integer.parseInt(tokens[0]));
			aCal.set(Calendar.MONTH, Integer.parseInt(tokens[1])-1);
			aCal.set(Calendar.DATE, Integer.parseInt(tokens[2]));
			//
			// date=Mon Dec 31 00:00:00 AEDT 2018 startDate Fri Jun 14 14:02:09 AEST 2019 endDate=Fri Jun 14 15:02:09 AEST 2019


			date = aCal.getTime();// managedTableDateFormat.parse(dateString);
			logger.debug("date=" + date + " startDate " + startDate + " endDate=" + endDate);
			if(date.compareTo(startDate) >=0  && date.compareTo(endDate) <=0 ) {
				toReturn.add(dailyTableName);
				logger.debug("getAllManagedTablesForAPeriod, adding " + dailyTableName);
			}
		}
		return toReturn;
	}
	public JSONArray getDeneWordTimeSeriesByIdentity(Identity identity, long startTimeMillis, long endTimeMillis) {
		ArrayList tables = getAllManagedTablesForAPeriod(TeleonomeConstants.PULSE_TABLE, startTimeMillis,  endTimeMillis) ;
		String sql = "";
		Connection connection = null;
		Statement statement = null;
		JSONArray toReturn = new JSONArray();
		ResultSet rs=null;
		JSONObject data=null;
		String tableName;
		Long L;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			for(int i=0;i<tables.size();i++) {
				tableName = (String) tables.get(i);
				sql = "select pulsetimemillis,DeneWord -> 'Value' As CurrentPulse from "+ tableName+" p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord where pulsetimemillis>="+ startTimeMillis + " and pulsetimemillis<=" + endTimeMillis+" and Nucleus->>'Name'='"+identity.getNucleusName() +"' and DeneChain->>'Name'='"+ identity.getDenechainName() + "' and Dene->>'Name'='"+ identity.getDeneName()+"' and DeneWord->>'Name'='"+ identity.getDeneWordName() +"' order by pulsetimemillis asc";
				rs = statement.executeQuery(sql);

				while(rs.next()){
					data = new JSONObject();
					data.put(TeleonomeConstants.PULSE_TIMESTAMP_MILLISECONDS, rs.getLong(1));
					data.put("Value", rs.getString(2).replace("\"", ""));
					toReturn.put(data);
				}
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

	public ArrayList<Map.Entry<JSONObject, Long>> getPulseForRangeForOrganism(String teleonomeName, long startPulseMillis, long endPulseMillis){
		Connection connection = null;
		Statement statement = null;
		ArrayList arrayList = new ArrayList();
		ResultSet rs=null;

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			//select * from organismpulse where teleonomeName='Tlaloc' and createdOn >='2016-03-07'
			//select * from organismpulse where teleonomeName='Tlaloc' and createdOn >='2016-03-07' and createdOn <'2016-03-08'
			ArrayList<String> tables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.ORGANISMPULSE_TABLE, startPulseMillis, endPulseMillis);
			String sql = "";
			//System.out.println("in getPulseForRangeForOrganism sql=" + sql);

			JSONObject data=null;
			Long L;
			for(int i=0;i<tables.size();i++) {
				sql = "select  data as text from "+ tables.get(i)+" where teleonomeName='"+ teleonomeName +"' and pulsetimemillis>='"+startPulseMillis +"' and pulsetimemillis<='" + endPulseMillis +"' order by createdOn asc";
				rs = statement.executeQuery(sql);
				while(rs.next()){

					data = new JSONObject(rs.getString(1));
					L = new Long(data.getLong("Pulse Timestamp in Milliseconds"));

					arrayList.add(new AbstractMap.SimpleEntry<JSONObject,Long>(data, L));
					Collections.sort(arrayList, new Comparator<Map.Entry<?, Long>>(){
						public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
							return o1.getValue().compareTo(o2.getValue());
						}});

				}
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
		ArrayList<String> allTables = getAllManagedTablesForAPeriod(TeleonomeConstants.PULSE_TABLE, startPeriod, System.currentTimeMillis());
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "";
			for(int i=0;i<allTables.size();i++) {
				sql = "select count(createdOn) from "+ allTables.get(i) +" where pulseTimeMillis>="+startPeriod;
				logger.debug("getNumberOfPulsesFromTime, sql=" + sql);
				rs = statement.executeQuery(sql);
				while(rs.next()){
					count += rs.getInt(1);	
				}
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

	private ArrayList<String> getManagedTablesByRange(String tableName, long millisToDeleteFrom, long millisToDeleteUntil) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(millisToDeleteFrom);
		Date fromDate = cal.getTime();

		Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(millisToDeleteUntil);
		Date untilDate = cal.getTime();
		//
		// now get a list of all the tables that exists and are older than millisToDeleteFrom, which will be deleted
		//
		ArrayList<String> toReturn = new ArrayList();
		ArrayList<String> allTables = getAllTableNamesForManagedTable(tableName);
		String dailyTableName, dateString;
		Date date;

		for(int i=0;i<allTables.size();i++) {
			//Pulse_ 
			dailyTableName = ((String)allTables.get(i));
			dateString = dailyTableName.substring(tableName.length()+1);
			try {
				date = managedTableDateFormat.parse(dateString);
				logger.debug("getManagedTablesByRange, date=" + date);
				if(date.after(fromDate) && date.before(untilDate)) {
					toReturn.add(dailyTableName);
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
		}
		return toReturn;
	}

	public JSONArray getPulsesFromTime(long startPeriod, int numberOfPulses)throws SQLException{
		Connection connection=null;
		Statement statement = null;
		JSONArray array = new JSONArray();
		ResultSet rs =null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			ArrayList<String> tables = getManagedTablesByRange(TeleonomeConstants.PULSE_TABLE, startPeriod,  System.currentTimeMillis());
			logger.debug("getPulsesFromTime, tables=" + tables);
			String sql ="";
			for(int i=0;i<tables.size();i++) {
				sql = "select data as text from "+ tables.get(i) + " where pulseTimeMillis>="+startPeriod +"  order by pulseTimeMillis asc limit " + numberOfPulses;
				logger.debug("getPulsesFromTime, sql=" + sql);
				rs = statement.executeQuery(sql);
				JSONObject data=null;
				while(rs.next()){
					data = new JSONObject(rs.getString(1));
					array.put(data);
				}
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
		ArrayList<String> allTables = this.getManagedTablesByRange(TeleonomeConstants.ORGANISMPULSE_TABLE, startPeriod, System.currentTimeMillis());
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			String sql = "";
			for(int i=0;i<allTables.size();i++) {
				sql = "select count(createdon) from "+allTables.get(i)+" where teleonomeName='" + teleonomeName + "' and pulseTimeMillis>="+startPeriod;
				logger.debug("sql=" + sql);
				rs = statement.executeQuery(sql);
				while(rs.next()){
					numberOfPulsesRemaining += rs.getInt(1);
				}
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
		ArrayList<String> allTables = this.getManagedTablesByRange(TeleonomeConstants.ORGANISMPULSE_TABLE, startPeriod, System.currentTimeMillis());

		ResultSet rs =null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			JSONObject data=null;
			String sql = "";
			for(int i=0;i<allTables.size();i++) {
				sql = "select data as text from "+ allTables.get(i)+" where teleonomeName='" + teleonomeName + "' and pulseTimeMillis>="+startPeriod +"  order by pulseTimeMillis asc limit " + numberOfPulses;
				logger.debug("sql=" + sql);
				rs = statement.executeQuery(sql);
			}
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
		ArrayList<Map.Entry<JSONObject, Long>> arrayList = new ArrayList();
		Connection connection = null;
		Statement statement = null;
		ResultSet rs=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			ArrayList<Map.Entry<JSONObject, Long>> sensorRequestQueuePositionDeneWordForInitialIndex = new ArrayList(); 
			ArrayList<String> tables = getManagedTablesByRange(TeleonomeConstants.PULSE_TABLE, startPeriod, endPeriod);
			String sql ="";
			JSONObject data=null;
			for(int i=0;i<tables.size();i++) {	
				sql = "select pulseTimeMillis, data as text from "+ tables.get(i)+" where pulseTimeMillis>="+startPeriod +" and pulseTimeMillis<=" + endPeriod +" order by pulseTimeMillis asc";
				rs = statement.executeQuery(sql);
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
			long now = System.currentTimeMillis();
			Calendar cal = Calendar.getInstance();
			String tableName = getTableNameByCalendar(TeleonomeConstants.MUTATION_EVENT_TABLE,  cal);
			if(!tableExists(tableName)) {
				sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.MUTATION_EVENT_TABLE +" with no data";
				int result = statement.executeUpdate(sql);
				logger.debug("table " + tableName + " was nt found so it was created, result=" + result);
			}


			String createdOn = getPostgresDateString(new Timestamp(now));
			sql = "insert into "+tableName+" (createdOn,createdOnMillis, data) values(" + createdOn + ","+ now+",'" + mutationEventData.toString() +"')";
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


	public boolean storeLifeCycleEvent(String eventType, long eventTimeMillis, int eventValue){
		//System.out.println(Utils.generateMethodTrace());
		int id=-1;
		Connection connection = null;
		PreparedStatement preparedStatement=null;
		ResultSet rs=null;
		boolean status=false;

		try {
			connection = connectionPool.getConnection();

			//String createdOn = getPostgresDateString(new Timestamp(eventTimeMillis));
			Timestamp eventTime = new Timestamp(eventTimeMillis);

			String sql = "insert into LifeCycleEvent (eventTime,eventTimeMillis, eventType, eventValue) values (?,?,?,?)";

			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setTimestamp(1, eventTime);
			preparedStatement.setLong(2, eventTimeMillis);
			preparedStatement.setString(3, eventType);
			preparedStatement.setInt(4, eventValue);
			status = preparedStatement.execute();
			logger.debug("storeLifeCycleEvent, eventTime=" + eventTimeMillis + " eventValue=" + eventValue + " eventType=" + eventType);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

		}
		//System.out.println("erturnign from createcommand id=" + id + " command=" + command);

		return status;

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

	public boolean tableExists(String tableName){
		//
		// Create
		String sql="SELECT EXISTS (SELECT 1 FROM   information_schema.tables WHERE  table_schema = 'public'  AND    table_name = '"+tableName+"');";
		logger.debug("tableExists sql=" + sql );
		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			while(rs.next()){
				toReturn=rs.getBoolean(1);
				logger.debug("tableExists toReturn=" + toReturn );
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//System.out.println("bad sql:" + sql);

			logger.warn(Utils.getStringException(e));
		}finally{
			if(connection!=null){
				try {
					rs.close();
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

	//
	// methods related to the network sensor
	//
	public int geNumberOfDevicesInLastSample(){
		Connection connection = null;
		Statement statement = null;
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.NETWORK_DEVICE_ACTIVITY_TABLE,System.currentTimeMillis(), System.currentTimeMillis());
		if(allTables.size()==0)return 0;
		String sql = "select count(deviceMacAddress) from "+ allTables.get(0)+" where scantimemillis in (select scantimemillis from "+ allTables.get(0)+" order by scantimemillis desc limit 1)";
		ResultSet rs = null;
		int toReturn=-1;
		logger.debug("sql=" + sql);
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			while(rs.next()){
				toReturn = rs.getInt(1);
			}
			statement.close();
			connectionPool.closeConnection(connection);

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
	public int geNumberUnknowDevicesInLastSample() {
		Connection connection = null;
		Statement statement = null;
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.NETWORK_DEVICE_ACTIVITY_TABLE,System.currentTimeMillis(), System.currentTimeMillis());
		if(allTables.size()==0)return 0;
		String sql = "select count(deviceMacAddress) from "+ allTables.get(0)+" where whiteliststatus=false and scantimemillis in (select scantimemillis from "+ allTables.get(0)+" order by scantimemillis desc limit 1)";
		ResultSet rs = null;
		int toReturn=-1;
		logger.debug("sql=" + sql);
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);

			while(rs.next()){
				toReturn = rs.getInt(1);
			}
			statement.close();
			connectionPool.closeConnection(connection);

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

	public JSONArray getLastNetworkSensorDeviceActivity( ) {
		JSONArray toReturn = new JSONArray();

		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.NETWORK_DEVICE_ACTIVITY_TABLE,System.currentTimeMillis(), System.currentTimeMillis());
		String sql="";

		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;



		try {

			connection = connectionPool.getConnection();
			statement = connection.createStatement();	

			JSONObject data=null;
			String dataPayload;
			boolean found=false;
			long scanTimeMillis;
			String scanTimeString, deviceName ,deviceIpAddress, deviceMacAddress;
			boolean whiteListStatus,isPresent,isMissing,isNew;
			JSONObject anObject;
			for(int i=0;i<allTables.size();i++) {
				sql="select deviceName ,deviceIpAddress, deviceMacAddress,whiteListStatus,isPresent,isMissing,isNew from "+ allTables.get(i) +" where scantimemillis in (select scantimemillis from "+ allTables.get(i)+" order by scantimemillis desc limit 1) order by whiteListStatus, scantimemillis, deviceName asc";
				logger.debug("getLastNetworkSensorDeviceActivity, sql " + sql);
				rs = statement.executeQuery(sql);
				while(rs.next()){

					deviceName=rs.getString(1);
					deviceIpAddress=rs.getString(2);
					deviceMacAddress=rs.getString(3);
					whiteListStatus=rs.getBoolean(4);
					isPresent=rs.getBoolean(5);
					isMissing=rs.getBoolean(6);
					isNew=rs.getBoolean(7);

					anObject = new JSONObject();

					anObject.put(TeleonomeConstants.DEVICE_NAME, deviceName);
					anObject.put(TeleonomeConstants.IP_ADDRESS, deviceIpAddress);
					anObject.put(TeleonomeConstants.MAC_ADDRESS, deviceMacAddress);
					anObject.put(TeleonomeConstants.WHITE_LIST_STATUS, whiteListStatus);
					anObject.put(TeleonomeConstants.IS_DEVICE_PRESENT, isPresent);
					anObject.put(TeleonomeConstants.IS_DEVICE_MISSING, isMissing);
					anObject.put(TeleonomeConstants.IS_DEVICE_NEW, isNew);
					toReturn.put(anObject);
				}
			}


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

	public JSONArray getNetworkSensorDeviceActivityByPeriod(long startPeriod, long endPeriod) {
		JSONArray toReturn = new JSONArray();

		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.NETWORK_DEVICE_ACTIVITY_TABLE,startPeriod, endPeriod);
		String sql="";

		Connection connection=null;
		Statement statement=null;
		ResultSet rs=null;



		try {

			connection = connectionPool.getConnection();
			statement = connection.createStatement();	
			rs = statement.executeQuery(sql);
			JSONObject data=null;
			String dataPayload;
			boolean found=false;
			long scanTimeMillis;
			String scanTimeString, deviceName ,deviceIpAddress, deviceMacAddress;
			boolean whiteListStatus,isPresent,isMissing,isNew;
			JSONObject anObject;
			for(int i=0;i<allTables.size();i++) {
				sql="select scanTimeMillis, scanTimeString, deviceName ,deviceIpAddress, deviceMacAddress,whiteListStatus,isPresent,isMissing,isNew from "+ allTables.get(i) +" where scantimemillis in (select scantimemillis from "+ allTables.get(i)+" order by scantimemillis desc) order by whiteListStatus, scantimemillis, deviceName asc";
				logger.debug("getLastNetworkSensorDeviceActivity, sql " + sql);

				while(rs.next()){
					scanTimeMillis=rs.getLong(1);
					scanTimeString=rs.getString(2);
					deviceName=rs.getString(3);
					deviceIpAddress=rs.getString(4);
					deviceMacAddress=rs.getString(5);
					whiteListStatus=rs.getBoolean(6);
					isPresent=rs.getBoolean(7);
					isMissing=rs.getBoolean(8);
					isNew=rs.getBoolean(9);

					anObject = new JSONObject();
					anObject.put(TeleonomeConstants.NETWORK_SCAN_MILLIS, scanTimeMillis);
					anObject.put(TeleonomeConstants.NETWORK_SCAN_TIME_STRING, scanTimeString);
					anObject.put(TeleonomeConstants.DEVICE_NAME, deviceName);
					anObject.put(TeleonomeConstants.IP_ADDRESS, deviceIpAddress);
					anObject.put(TeleonomeConstants.MAC_ADDRESS, deviceMacAddress);
					anObject.put(TeleonomeConstants.WHITE_LIST_STATUS, whiteListStatus);
					anObject.put(TeleonomeConstants.IS_DEVICE_PRESENT, isPresent);
					anObject.put(TeleonomeConstants.IS_DEVICE_MISSING, isMissing);
					anObject.put(TeleonomeConstants.IS_DEVICE_NEW, isNew);
					toReturn.put(anObject);
				}
			}


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

	public boolean removeDeviceFromWhiteList(String deviceName) {
		String sql="";
		Connection connection=null;
		Statement statement = null;
		boolean toReturn=false;
		ResultSet rs=null;
		try {

			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));

			sql = "delete from NetworkDeviceWhiteList where deviceName ='"+ deviceName+"'";
			logger.debug("delete  NetworkDeviceWhiteList, sql " + sql);
			int result = statement.executeUpdate(sql);
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



	public boolean addDeviceToWhiteList(String deviceName, String macAddress) {
		String sql="";
		Connection connection=null;
		Statement statement = null;
		boolean toReturn=false;
		ResultSet rs=null;
		try {

			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));

			sql = "insert into NetworkDeviceWhiteList (deviceName , deviceMacAddress) values('"+ deviceName+"','"+macAddress +"')";
			logger.debug("insert  NetworkDeviceWhiteList, sql " + sql);
			int result = statement.executeUpdate(sql);
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
	public boolean isDeviceInWhiteList(String deviceName) {

		Connection connection = null;
		Statement statement = null;
		String sql = "select count(deviceMacAddress) from NetworkDeviceWhiteList where deviceName='"+deviceName +"'";
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

	public boolean storeNetworkDeviceActivity(long scanTimeMillis, String scanTimeString,String  deviceName ,String deviceIpAddress, String deviceMacAddress,boolean whiteListStatus,boolean isPresent,boolean isMissing,boolean isNew) { 
		String sql="";
		Connection connection=null;
		Statement statement = null;
		boolean toReturn=false;
		ResultSet rs=null;
		try {

			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));
			Calendar cal = Calendar.getInstance();
			String tableName = getTableNameByCalendar(TeleonomeConstants.NETWORK_DEVICE_ACTIVITY_TABLE,  cal);
			if(!tableExists(tableName)) {
				sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.NETWORK_DEVICE_ACTIVITY_TABLE +" with no data";
				int result = statement.executeUpdate(sql);
				logger.debug("table " + tableName + " was nt found so it was created, result=" + result);
			}

			sql = "insert into " + tableName + " (scanTimeMillis, scanTimeString, deviceName ,deviceIpAddress, deviceMacAddress,whiteListStatus,isPresent,isMissing,isNew" + 
					") values("+ scanTimeMillis + ",'"+ scanTimeString+"','"+deviceName +"','" + deviceIpAddress + "','"+ deviceMacAddress +"',"+ whiteListStatus + ","+isPresent +"," + isMissing + "," +isNew +")";
			logger.debug("update network, sql " + sql);
			int result = statement.executeUpdate(sql);

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

	//
	// end of methods related to the network sensor
	//
	private String getTableNameByCalendar(String prefix, Calendar cal) {
		String tableName = prefix + "_" + cal.get(Calendar.YEAR) +  "_" + (1+cal.get(Calendar.MONTH)) +  "_" + cal.get(Calendar.DATE);
		return tableName;
	}
	public boolean storePulse(long timestampInMills,String pulseData){

		//
		// before doing anything, check that the pulse is 
		try {
			JSONObject testJSON = new JSONObject(pulseData);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e1));
			return false;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestampInMills);

		String sql="";
		Connection connection=null;
		Statement statement=null;
		boolean toReturn=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String tableName = getTableNameByCalendar(TeleonomeConstants.PULSE_TABLE,  cal);
			if(!tableExists(tableName)) {
				sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.PULSE_TABLE +" with no data";
				int result = statement.executeUpdate(sql);
				logger.debug("table " + tableName + " was nt found so it was created, result=" + result);
			}

			//.replace("\"", "\\\"")
			String createdOn = getPostgresDateString(new Timestamp(System.currentTimeMillis()));
			sql = "insert into "+ tableName+" (createdOn,pulseTimeMillis,data) values(" + createdOn + ","+timestampInMills+",'" + pulseData +"')";
			int result = statement.executeUpdate(sql);

			toReturn= true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//System.out.println("bad sql:" + sql);

			logger.warn(Utils.getStringException(e));
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
		pulseData=null;
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
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(milliSeconds);
			String tableName = getTableNameByCalendar(TeleonomeConstants.PULSE_TABLE,  cal);

			String sql = "select count(createdon) from "+ tableName +" where pulsetimemillis="+ milliSeconds;
			logger.debug("containsPulse, sql=" + sql);
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

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(milliSeconds);
		String tableName = this.getTableNameByCalendar(TeleonomeConstants.ORGANISMPULSE_TABLE, cal);

		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		boolean contains=false;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select count(createdon) from "+tableName+" where teleonomeName='"+ teleonomeName +"' and pulsetimemillis="+ milliSeconds;
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
			logger.warn(Utils.getStringException(e1));
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
			Calendar cal = Calendar.getInstance();
			String tableName = getTableNameByCalendar(TeleonomeConstants.ORGANISMPULSE_TABLE,  cal);
			if(!tableExists(tableName)) {
				sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.ORGANISMPULSE_TABLE +" with no data";
				int result = statement.executeUpdate(sql);
				logger.debug("table " + tableName + " was nt found so it was created, result=" + result);
			}


			sql = "insert into " + tableName + " (teleonomeName, createdOn,pulseTimeMillis,networkAddress,data) values('"+ teleonomeName + "',"+ createdOn+","+pulseTimeMillis +",'" + teleonomeAddress + "','"+ pulseData +"')";
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
		pulseData=null;
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
			ArrayList<String> allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.COMMAND_REQUESTS_TABLE);
			String sql = "";
			for(int i=0;i<allTables.size();i++) {
				sql = "update "+ allTables.get(i)+" set status='"+ TeleonomeConstants.COMMAND_REQUEST_SKIPPED_AT_INIT +"'   where status ='"+ TeleonomeConstants.COMMAND_REQUEST_PENDING_EXECUTION +"' or status='" + TeleonomeConstants.COMMAND_REQUEST_NOT_EXECUTED +"'";
				logger.debug("markAllNonExecutedAsSkipped, sql=" + sql);
				statement.executeUpdate(sql);
			}

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

	public JSONObject requestCommandToExecute(String command, String commandCode,String commandCodeType,  String payLoad, String clientIp, boolean restartRequired){
		//System.out.println(Utils.generateMethodTrace());
		int id=-1;
		Connection connection = null;
		PreparedStatement preparedStatement=null;
		Statement statement=null;
		ResultSet rs=null;
		JSONObject toReturn=new JSONObject();
		long createdOn = System.currentTimeMillis();
		String sql;
		try {
			connection = connectionPool.getConnection();
			Calendar cal = Calendar.getInstance();
			String tableName = getTableNameByCalendar(TeleonomeConstants.COMMAND_REQUESTS_TABLE,  cal);
			statement = connection.createStatement();
			if(!tableExists(tableName)) {
				//
				//dont use create table as table because the id goes from bigserial to bigint
				//
				//sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.COMMAND_REQUESTS_TABLE +" with no data";
				sql="create table "+tableName+"("+
					 "id serial NOT NULL,"+
					  "createdon bigint,"+
					 "executedon bigint,"+
					 " command character varying(500) NOT NULL,"+
					  "status character varying(500) NOT NULL,"+
					  "clientIp character varying(20) NOT NULL,"+
					 " restartRequired boolean not null,"+
					  "commandCode character varying(20) NOT NULL,"+
					"  commandCodeType character varying(20) NOT NULL,"+
					"  requestpayload text,"+
					"  payload text,"+
					 " CONSTRAINT "+ tableName +"_pkey PRIMARY KEY (id)"+
					 ")";

				int result = statement.executeUpdate(sql);
				logger.debug("table " + tableName + " was not found so it was created, result=" + result);
			}
			sql = "insert into "+ tableName + "(createdOn,command, status, payLoad, commandCode,commandCodeType, clientIp, restartRequired) values (?,?,?,?,?,?,?,?) returning id";

			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setLong(1, createdOn);
			preparedStatement.setString(2,  command );
			preparedStatement.setString(3, TeleonomeConstants.COMMAND_REQUEST_PENDING_EXECUTION);
			preparedStatement.setString(4, payLoad);
			preparedStatement.setString(5, commandCode);
			preparedStatement.setString(6,commandCodeType);
			preparedStatement.setString(7, clientIp);
			preparedStatement.setBoolean(8, restartRequired);


			rs = preparedStatement.executeQuery();
			JSONObject data=null;
			CommandRequest aCommandRequest = new CommandRequest();



			while(rs.next()){
				id = rs.getInt(1);
				toReturn.put("id", id);
				toReturn.put("Createdon", createdOn);
				toReturn.put("Command", command);
				toReturn.put("Status", TeleonomeConstants.COMMAND_REQUEST_PENDING_EXECUTION);
				toReturn.put("CommandCode", commandCode);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}finally{
			try {
				if(statement!=null)statement.close();
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}

		}
		//System.out.println("erturnign from createcommand id=" + id + " command=" + command);

		return toReturn;

	}

	public JSONObject isCommandCompleted(int id){

		Connection connection = null;;
		Statement statement = null;
		ArrayList<String> allTables = getAllManagedTablesForAPeriod(TeleonomeConstants.COMMAND_REQUESTS_TABLE, System.currentTimeMillis(), System.currentTimeMillis());
		String tableName = allTables.get(0);
		String sql = "select executedOn,status from "+ tableName + " where id="+ id;
		ResultSet rs = null;
		JSONObject toReturn = new JSONObject();
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(sql);
			CommandRequest aCommandRequest = new CommandRequest();
			String status;
			long executedOn;
			while(rs.next()){
				executedOn=rs.getLong(1);
				status=rs.getString(2);

				if(executedOn>0) {
					toReturn.put(TeleonomeConstants.COMMAND_EXECUTED_ON, executedOn);
					toReturn.put(TeleonomeConstants.COMMAND_EXECUTION_STATUS, status);
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
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.COMMAND_REQUESTS_TABLE, System.currentTimeMillis(), System.currentTimeMillis());
		if(allTables.size()==0)return null;
		//
		// we need the first one and the last one
		//
		String newestTableName = allTables.get(0);
		String sql = "select id,command,commandCode,commandCodeType, payload from "+newestTableName +" where status='"+ TeleonomeConstants.COMMAND_REQUEST_PENDING_EXECUTION +"'  order by createdOn asc limit 1";
		ResultSet rs = null;
		CommandRequest aCommandRequest = new CommandRequest();

		logger.debug("sql=" + sql);
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
				aCommandRequest.setCommandCode(rs.getString(3));
				aCommandRequest.setCommandCodeType(rs.getString(4));
				aCommandRequest.setDataPayload(rs.getString(5));
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

	public JSONObject getAllCommandRequests(boolean includeHuman, boolean includeInternal, int offset, int limit){

		Connection connection = null;
		Connection connection2 = null;
		Statement statement = null;
		Statement statement2 = null;
		String whereClause="";
		//
		// now build the where clause
		// if they are both true then
		// no need for a where clause
		//
		if(includeHuman && !includeInternal) {
			whereClause = " where clientIp != '127.0.0.1'";
		}else if(!includeHuman && includeInternal) {
			whereClause = " where clientIp = '127.0.0.1'";
		}

		String sql;
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.COMMAND_REQUESTS_TABLE,0, System.currentTimeMillis());


		ResultSet rs = null;
		ResultSet rs2 = null;
		CommandRequest aCommandRequest = new CommandRequest();
		JSONObject toReturn = new JSONObject();
		JSONArray valuesJSONArray = new JSONArray();
		toReturn.put("Values", valuesJSONArray);
		//
		// put these two variables for convenience
		//
		toReturn.put("Limit", limit);
		toReturn.put("Offset", offset);
		ArrayList<Map.Entry<JSONObject, Long>> orderedCommands = new ArrayList(); 

		JSONObject o;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			for(int i=0;i<allTables.size();i++) {
				sql = "select id,createdon, executedon,command,status,payload, clientIp from "+allTables.get(i)+" " +  whereClause + "  order by createdOn desc limit " + limit + " offset " + offset;
				logger.debug("getAllCommandRequests sql=" + sql);
				rs = statement.executeQuery(sql);
				while(rs.next()){
					int id = rs.getInt(1);
					long createdon = rs.getLong(2);
					long executedon = rs.getLong(3);
					String command = rs.getString(4);

					String status = rs.getString(5);
					String payload = rs.getString(6);
					String clientIp = rs.getString(7);

					o = new JSONObject();
					o.put("id", id);
					o.put("Createdon", createdon);
					o.put("Executedon", executedon);
					o.put("Command", command);

					o.put("Status", status);
					o.put("Payload", payload);
					o.put("ClientIp", clientIp);



					orderedCommands.add(new AbstractMap.SimpleEntry<JSONObject,Long>(o, createdon));
					Collections.sort(orderedCommands, new Comparator<Map.Entry<?, Long>>(){
						public int compare(Map.Entry<?, Long> o1, Map.Entry<?, Long> o2) {
							return o2.getValue().compareTo(o1.getValue());
						}});
				}
			}

			//
			// because they could be over many tables
			// you need to sort them omce they are in the a
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

		//
		// add the to the valuesJSONArray sorted
		for (Map.Entry<JSONObject, Long> entry3 : orderedCommands) {
			o = entry3.getKey();
			valuesJSONArray.put(o);
		}

		try {
			//
			// now repeat the same command with out the limit and the offset to get the total
			//
			sql = "select count(*) from CommandRequests " +  whereClause ;
			connection2 = connectionPool.getConnection();
			statement2 = connection2.createStatement();
			rs2 = statement2.executeQuery(sql);

			while(rs2.next()){
				int total = rs2.getInt(1);
				toReturn.put("Total", total);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs2!=null)rs2.close();
				if(statement2!=null)statement2.close();
				if(connection2!=null)connectionPool.closeConnection(connection2);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}

	public JSONObject markCommandAsBadCommandCode(int id, String reason){
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ArrayList<String> allTables = getAllManagedTablesForAPeriod(TeleonomeConstants.COMMAND_REQUESTS_TABLE, System.currentTimeMillis(), System.currentTimeMillis());
		String tableName = allTables.get(0);
		String sql = "update "+tableName+" set executedOn=?, status=?   where id=? returning  createdon, executedon, command, commandCodeType, status, commandcode, restartRequired";
		logger.debug("markCommandAsBadCommandCode, sql=" + sql);
		ResultSet rs=null;
		JSONObject toReturn=new JSONObject();
		toReturn.put("id", id);
		try {
			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setLong(1, System.currentTimeMillis());
			preparedStatement.setString(2,  reason );
			preparedStatement.setInt(3, id);

			rs = preparedStatement.executeQuery();
			while(rs.next()){

				long createdon = rs.getLong(1);
				long executedon = rs.getLong(2);
				String command = rs.getString(3);
				String status = rs.getString(4);
				String commandcode = rs.getString(5);
				String commandcodeType = rs.getString(6);
				boolean restartRequired = rs.getBoolean(7);

				toReturn.put("Createdon", createdon);
				toReturn.put("Executedon", executedon);
				toReturn.put("Command", command);
				toReturn.put("Status", status);
				toReturn.put("CommandCode", commandcode);
				toReturn.put("CommandCodeType", commandcodeType);
				toReturn.put("RestartRequired", restartRequired);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}	

	public JSONObject markCommandCompleted(int id){
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ArrayList<String> allTables = getAllManagedTablesForAPeriod(TeleonomeConstants.COMMAND_REQUESTS_TABLE, System.currentTimeMillis(), System.currentTimeMillis());
		String tableName = allTables.get(0);
		String sql = "update "+tableName+" set executedOn=?, status=?   where id=? returning  createdon, executedon, command, status, commandcode, restartRequired";
		ResultSet rs=null;
		JSONObject toReturn=new JSONObject();
		toReturn.put("id", id);
		try {
			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setLong(1, System.currentTimeMillis());
			preparedStatement.setString(2,  TeleonomeConstants.COMMAND_REQUEST_EXECUTED );
			preparedStatement.setInt(3, id);

			rs = preparedStatement.executeQuery();
			while(rs.next()){

				long createdon = rs.getLong(1);
				long executedon = rs.getLong(2);
				String command = rs.getString(3);
				String status = rs.getString(4);
				String commandcode = rs.getString(5);
				boolean restartRequired = rs.getBoolean(6);

				toReturn.put("Createdon", createdon);
				toReturn.put("Executedon", executedon);
				toReturn.put("Command", command);
				toReturn.put("Status", status);
				toReturn.put("CommandCode", commandcode);
				toReturn.put("RestartRequired", restartRequired);

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}

		}
		return toReturn;
	}
	/**
	 * This method is called when executing a command of type Reboot in networkmode
	 * because the servlet needs to store the wifi password in clear text so that
	 * the asynccycle can create the supplicant,the asunccycle invokes this method
	 * after it created the supplicant to shtat the wifi password is never shared
	 * @param id
	 * @return
	 */
	public boolean offuscateWifiPasswordInCommand(int id, String updatedPayload){
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		String sql = "update CommandRequests set payload=?   where id=? ";
		ResultSet rs=null;
		boolean toReturn=false;

		try {
			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1,  updatedPayload );
			preparedStatement.setInt(2, id);
			preparedStatement.execute();
			logger.debug("offuscate wifi password=" + updatedPayload);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.debug(Utils.getStringException(e));
		}finally{
			try {
				if(rs!=null)rs.close();
				if(preparedStatement!=null)preparedStatement.close();
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
			Calendar cal = Calendar.getInstance();
			String tableName = this.getTableNameByCalendar(TeleonomeConstants.PULSE_TABLE, cal);
			String sql = "select data from "+ tableName+" order by createdOn desc limit 1";
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

	public JSONObject getPulseByCreatedOn(long pulsetimemillis){
		Statement statement=null;
		Connection connection=null;
		ResultSet rs=null;
		JSONObject data=null;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(pulsetimemillis);
		String tableName = this.getTableNameByCalendar(TeleonomeConstants.PULSE_TABLE, cal);
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select data from "+ tableName +" where pulsetimemillis ="+pulsetimemillis;
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

	public long getOldestPulseMillis(){
		return getOldestRecordMillis(TeleonomeConstants.PULSE_TABLE, "pulsetimemillis");
	}


	public long getOldestRecordMillis(String tableName, String columnName){

		Connection connection = null;
		Statement statement = null;
		ResultSet rs =null;
		long oldestPulseMillis = 0;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select "+columnName +" from "+tableName + " order by createdOn asc limit 1";
			logger.debug("getLastPulse, sql=" + sql);
			rs = statement.executeQuery(sql);
			while(rs.next()){
				oldestPulseMillis = rs.getLong(1);
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
		logger.debug("getOldestPulseMillis, oldestPulseMillis=" + oldestPulseMillis);
		return oldestPulseMillis;
	}

	public JSONObject getLastPulse(String teleonomeName){

		Connection connection = null;
		Statement statement = null;
		ResultSet rs =null;
		JSONObject data=null;
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			String sql = "select data from lastOrganismPulse where TeleonomeName='"+teleonomeName +"' order by createdOn desc limit 1";
			logger.debug("getLastPulse, sql=" + sql);
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
		logger.debug("getLastPulse, data=" + data.toString(4));
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
	public JSONObject getTeleonomeDataAvailableRanges() {
		ArrayList<String> allTables = getAllTableNamesForManagedTable(TeleonomeConstants.PULSE_TABLE);
		//
		// we need the first one and the last one
		//
		String newestTableName = allTables.get(allTables.size()-1);
		String oldestTableName = allTables.get(0);

		String minCommand = "select  min(createdon) from " + oldestTableName;
		String maxCommand = "select  max(createdon) from " + newestTableName;
		Connection connection=null;
		Statement statement = null; 
		ResultSet rs=null;
		JSONObject toReturn = new JSONObject();
		Timestamp timeMin=null;
		Timestamp timeMax=null;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(minCommand);
			while(rs.next()){
				timeMin=rs.getTimestamp(1);
				if(timeMin!=null) {
					toReturn.put("TimeMin", sdf.format(timeMin));
				}else {
					toReturn.put("TimeMin", timeMin);
				}
				logger.debug("getTeleonomeDataAvailableRanges,TimeMin=" + timeMin );
			}

			rs = statement.executeQuery(maxCommand);
			while(rs.next()){
				timeMax=rs.getTimestamp(1);
				if(timeMax!=null) {
					toReturn.put("TimeMax", sdf.format(timeMax));
				}else {
					toReturn.put("TimeMax", timeMax);
				}
				logger.debug("getTeleonomeDataAvailableRanges,timeMax=" + timeMax );
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
		return toReturn;
	}
	public JSONArray getTeleonomeDataAvailableInOrganism() {

		ArrayList<String> allTables = getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		//
		// we need the first one and the last one
		//
		String newestTableName = allTables.get(allTables.size()-1);
		String oldestTableName = allTables.get(0);
		String minCommand = "select teleonomeName, min(createdon) from " + oldestTableName + " group by teleonomename";
		String maxCommand = "select teleonomeName, max(createdon) from " + newestTableName + " group by teleonomename";

		//String command = "select teleonomeName, min(createdon), max(createdon) from organismpulse group by teleonomename";
		Connection connection=null;
		Statement statement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		JSONObject jsonObject;
		Hashtable h = new Hashtable();
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();

			Timestamp timeMin=null;
			Timestamp timeMax=null;

			String name;
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

			rs = statement.executeQuery(minCommand);

			while(rs.next()){
				name=rs.getString(1);
				timeMin=rs.getTimestamp(2);
				timeMax=rs.getTimestamp(3);
				jsonObject = new JSONObject();
				jsonObject.put("Name", name);
				if(timeMin!=null) {
					jsonObject.put("TimeMin", sdf.format(timeMin));
				}else {
					jsonObject.put("TimeMin", timeMin);
				}
				if(timeMax!=null) {
					jsonObject.put("TimeMax", sdf.format(timeMax));
				}else {
					jsonObject.put("TimeMax", timeMax);
				}
				h.put(name, jsonObject);
			}

			rs = statement.executeQuery(maxCommand);
			while(rs.next()){
				name=rs.getString(1);
				jsonObject = (JSONObject) h.get(name);
				if(jsonObject==null) {
					jsonObject = new JSONObject();
					jsonObject.put("Name", name);
				}
				timeMax=rs.getTimestamp(2);
				if(timeMax!=null) {
					jsonObject.put("TimeMax", sdf.format(timeMax));
				}else {
					jsonObject.put("TimeMax", timeMax);
				}
				h.put(name, jsonObject);
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
		for(Enumeration<JSONObject> en=(Enumeration<JSONObject>) h.values();en.hasMoreElements();) {
			jsonObject = en.nextElement();
			toReturn.put(jsonObject);
		}
		return toReturn;
	}

	public JSONArray getTeleonomeNamesInOrganism() {
		ArrayList allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		String tableName = (String) allTables.get(allTables.size()-1);
		String command = "select distinct(teleonomeName) from " + tableName;
		logger.debug("getTeleonomeNamesInOrganism, sql=" + command);
		Connection connection=null;
		Statement statement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		try {
			connection = connectionPool.getConnection();
			statement = connection.createStatement();
			rs = statement.executeQuery(command);
			Timestamp time=null;
			String name;
			while(rs.next()){
				name=rs.getString(1);
				toReturn.put(name);
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
		return toReturn;
	}

	public JSONArray getNucleiNamesForTeleonomeInOrganism(String teleonomeName) {

		//
		// this command returns the complete nucleus
		//
		//String command = "select NU from organismpulse p, jsonb_array_elements(p.data->'Denome'->'Nuclei') as NU where teleonomeName=?  limit 1";
		//
		// this command returns just the name of the nuclei
		//
		ArrayList allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		String tableName = (String) allTables.get(allTables.size()-1);

		String command = "select  NU -> 'Name' from "+ tableName +" p, jsonb_array_elements(p.data->'Denome'->'Nuclei') as NU where teleonomeName=? and createdon in (select createdon from "+ tableName +" where teleonomeName=? order by createdon desc limit 1)  order by NU -> 'Name'";


		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		try {

			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(command);
			preparedStatement.setString(1, teleonomeName);
			preparedStatement.setString(2, teleonomeName);
			rs = preparedStatement.executeQuery();
			Timestamp time=null;
			String name;
			double value;
			while(rs.next()){
				name=rs.getString(1).replace("\"", "");
				toReturn.put(name);
			}


		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{

			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return toReturn;
	}

	public JSONArray getDeneChainNamesForTeleonomeInOrganism(String teleonomeName, String nucleusName) {
		ArrayList allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		String tableName = (String) allTables.get(allTables.size()-1);

		String command = "select distinct(DeneChain  -> 'Name') from "+ tableName +" p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus, jsonb_array_elements(Nucleus->'DeneChains') As DeneChain where teleonomeName=? and Nucleus->>'Name'=? and createdon in (select createdon from "+tableName+"  where teleonomeName=? order by createdon desc limit 1) order by DeneChain -> 'Name'";
		logger.debug("getDeneChainNamesForTeleonomeInOrganism, command=" + command);
		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		try {

			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(command);
			preparedStatement.setString(1, teleonomeName);
			preparedStatement.setString(2, nucleusName);
			preparedStatement.setString(3, teleonomeName);
			rs = preparedStatement.executeQuery();
			String name;
			while(rs.next()){
				name=rs.getString(1).replace("\"", "");;
				toReturn.put(name);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{

			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}
		return toReturn;
	}

	public JSONArray getDeneNamesForTeleonomeInOrganism(String teleonomeName, String nucleusName, String deneChainName) {
		ArrayList allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		String tableName = (String) allTables.get(allTables.size()-1);

		String command = " select distinct(Dene -> 'Name') As denename from "+tableName +" p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene where teleonomeName=? and createdon in (select createdon from "+tableName +" where teleonomeName=? order by createdon desc limit 1) and Nucleus->>'Name'=? and DeneChain->>'Name'=? order by Dene -> 'Name'";
		logger.debug("getDeneNamesForTeleonomeInOrganism, command=" + command);

		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		try {

			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(command);
			preparedStatement.setString(1, teleonomeName);
			preparedStatement.setString(2, teleonomeName);
			preparedStatement.setString(3, nucleusName);
			preparedStatement.setString(4, deneChainName);

			rs = preparedStatement.executeQuery();
			Timestamp time=null;
			String name;
			double value;
			while(rs.next()){
				name=rs.getString(1).replace("\"", "");;
				toReturn.put(name);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{

			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}
		return toReturn;
	}

	public JSONArray getDeneWordNamesForTeleonomeInOrganism(String teleonomeName, String nucleusName, String deneChainName, String deneName) {
		ArrayList allTables = this.getAllTableNamesForManagedTable(TeleonomeConstants.ORGANISMPULSE_TABLE);
		String tableName = (String) allTables.get(allTables.size()-1);


		String command = " select distinct(DeneWord -> 'Name') As deneWordName from "+tableName +" p, jsonb_array_elements(p.data->'Denome'->'Nuclei')  AS Nucleus,  jsonb_array_elements(Nucleus->'DeneChains') As DeneChain , jsonb_array_elements(DeneChain->'Denes') As Dene, jsonb_array_elements(Dene->'DeneWords') as DeneWord  where teleonomeName=? and createdon in (select createdon from "+ tableName +" where teleonomeName=? order by createdon desc limit 1) and Nucleus->>'Name'=? and DeneChain->>'Name'=? and Dene->>'Name'=?  order by DeneWord -> 'Name'";
		logger.debug("getDeneWordNamesForTeleonomeInOrganism, command=" + command);

		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		try {

			connection = connectionPool.getConnection();
			preparedStatement = connection.prepareStatement(command);
			preparedStatement.setString(1, teleonomeName);
			preparedStatement.setString(2, teleonomeName);
			preparedStatement.setString(3, nucleusName);
			preparedStatement.setString(4, deneChainName);
			preparedStatement.setString(5, deneName);

			rs = preparedStatement.executeQuery();
			Timestamp time=null;
			String name;
			double value;
			while(rs.next()){
				name=rs.getString(1).replace("\"", "");;
				toReturn.put(name);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{

			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}
		return toReturn;
	}


	public static String getUniqueIndex(){
		java.rmi.dgc.VMID v = new java.rmi.dgc.VMID();
		return v.toString();
	}

	public JSONArray getRemeberedDeneWordMaxValue(TimeZone timeZone, String identityPointer,  long startTimeMillis, long  endTimeMillis){
		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		Timestamp time=null;
		JSONObject j;
		double value;
		try {
			ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE, startTimeMillis, endTimeMillis);
			String command ="";		
			java.sql.Timestamp fromTimeValue = new java.sql.Timestamp(startTimeMillis);
			java.sql.Timestamp untilTimeValue = new java.sql.Timestamp(endTimeMillis);
			connection = connectionPool.getConnection();
			for(int i=0;i<allTables.size();i++) {
				command = "SELECT time, value from "+ allTables.get(i)+" where time>=? and time<=? and  identityString=? order by time asc";
				logger.debug("command=" + command);
				preparedStatement = connection.prepareStatement(command);
				Calendar calendarTimeZone = Calendar.getInstance(timeZone);  
				preparedStatement.setTimestamp(1, fromTimeValue, calendarTimeZone);
				preparedStatement.setTimestamp(2, untilTimeValue, calendarTimeZone);
				preparedStatement.setString(3, identityPointer);
				rs = preparedStatement.executeQuery();
				while(rs.next()){
					time=rs.getTimestamp(1);
					value = rs.getDouble(2);
					j = new JSONObject();
					j.put("Pulse Timestamp in Milliseconds", time.getTime());
					j.put("Value", value);
					toReturn.put(j);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{
			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}
		return toReturn;
	}

	public JSONObject getStatsInfoForRemeberedDeneWord(TimeZone timeZone, String identityPointer,  long startTimeMillis, long  endTimeMillis, String stat){
		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONObject toReturn = new JSONObject();
		ArrayList allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE, startTimeMillis, endTimeMillis);
		String command = "";
		Timestamp time=null;
		JSONObject j;
		double value, oldValue;
		double averageAggregation=0;
		try {
			connection = connectionPool.getConnection();
			for(int i=0;i<allTables.size();i++) {
				if(stat.equals(TeleonomeConstants.DENEWORD_MAXIMUM_ATTRIBUTE)) {
					command = "SELECT distinct on(value) time, value from "+allTables.get(i) + " where timeMillis>=? and timeMillis<=? and  identityString=? order by value desc, time desc limit 1";
				}else if(stat.equals(TeleonomeConstants.DENEWORD_MINIMUM_ATTRIBUTE)) {
					command = "SELECT distinct on(value) time, value from "+allTables.get(i) +" where timeMillis>=? and timeMillis<=? and  identityString=? order by value asc, time asc limit 1";
				}else if(stat.equals(TeleonomeConstants.DENEWORD_AVERAGE_ATTRIBUTE)) {
					command = "SELECT avg(value) from "+allTables.get(i) +" where timeMillis>=? and timeMillis<=? and  identityString=?";
				}
				logger.debug("command=" + command);
				preparedStatement = connection.prepareStatement(command);
				preparedStatement.setLong(1, startTimeMillis);
				preparedStatement.setLong(2, endTimeMillis);
				preparedStatement.setString(3, identityPointer);
				rs = preparedStatement.executeQuery();
				while(rs.next()){
					if(stat.equals(TeleonomeConstants.DENEWORD_MAXIMUM_ATTRIBUTE)) {

						time=rs.getTimestamp(1);
						value = rs.getDouble(2);
						if(toReturn.has("Value")) {
							oldValue = toReturn.getDouble("Value");
							if(oldValue<value) {
								toReturn.put("Pulse Timestamp in Milliseconds", time.getTime());
								toReturn.put("Value", value);
							}
						}else {
							toReturn.put("Pulse Timestamp in Milliseconds", time.getTime());
							toReturn.put("Value", value);
						}
					}else if(stat.equals(TeleonomeConstants.DENEWORD_MINIMUM_ATTRIBUTE)) {
						time=rs.getTimestamp(1);
						value = rs.getDouble(2);
						if(toReturn.has("Value")) {
							oldValue = toReturn.getDouble("Value");
							if(oldValue>value) {
								toReturn.put("Pulse Timestamp in Milliseconds", time.getTime());
								toReturn.put("Value", value);
							}
						}else {
							toReturn.put("Pulse Timestamp in Milliseconds", time.getTime());
							toReturn.put("Value", value);
						}
					}else if(stat.equals(TeleonomeConstants.DENEWORD_AVERAGE_ATTRIBUTE)) {

						averageAggregation += rs.getDouble(1);
					}
				}
			}

			if(stat.equals(TeleonomeConstants.DENEWORD_AVERAGE_ATTRIBUTE)) {
				toReturn.put("Value", twoDecimalsFormat.format(averageAggregation/allTables.size()));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}finally{
			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}
		logger.debug("returning " + toReturn.toString(4));
		return toReturn;
	}

	public JSONArray getRemeberedDeneWordStatByPeriod( String identityPointer,  long startTimeMillis, long  endTimeMillis, String kind ){
		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		String direction="desc";
		if(kind.equals(TeleonomeConstants.DENEWORD_MAXIMUM_ATTRIBUTE)) {
			direction="desc";
		}else if(kind.equals(TeleonomeConstants.DENEWORD_MINIMUM_ATTRIBUTE)) {
			direction="asc";
		}
		String command = "";
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE,startTimeMillis, endTimeMillis);
		JSONArray toReturn = new JSONArray();
		Timestamp time=null;
		JSONObject j;
		double value;
		long timeMillis;
		try {
			connection = connectionPool.getConnection();
			for(int i=0;i<allTables.size();i++) {
				command = "SELECT timemillis, value FROM "+allTables.get(i) + "  where identitystring=?  and timeMillis>=? and timeMillis<=? order by timeMillis "+ direction + " limit 1";
				logger.debug("getRemeberedDeneWordStatByPeriod command=" + command);
				preparedStatement = connection.prepareStatement(command);
				preparedStatement.setString(1, identityPointer);
				preparedStatement.setLong(2, startTimeMillis);
				preparedStatement.setLong(3, endTimeMillis);
				rs = preparedStatement.executeQuery();
				while(rs.next()){
					timeMillis=rs.getLong(1);
					value = rs.getDouble(2);
					j = new JSONObject();
					j.put("Pulse Timestamp in Milliseconds", timeMillis);
					j.put("Value", value);
					toReturn.put(j);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{
			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}


		//			String command = "SELECT timemillis, value FROM (" +
		//					  "SELECT *, "+
		//					   " ROW_NUMBER() OVER (PARTITION BY date_trunc('day', time) "+
		//					      "  ORDER BY value "+direction +") AS _rn "+
		//					  "FROM remembereddenewords "+
		//					 " where identitystring=? "+
		//					 " and timeMillis>=? and timeMillis<=? "+
		//					") AS _max "+
		//					"WHERE _rn = 1 "+
		//					"ORDER BY time;";






		return toReturn;
	}

	public JSONArray getRemeberedDeneWordStart( String identityPointer,  long startTimeMillis, long  endTimeMillis){
		logger.info("enter getRemeberedDeneWordStart identityPointer="+ identityPointer );
		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE, startTimeMillis, endTimeMillis);
		String command = "", units="";
		Timestamp time=null;
		JSONObject j;
		double value;
		logger.debug("allTables="+ allTables.size() );
		try {
			connection = connectionPool.getConnection();
			for(int i=0;i<allTables.size();i++) {
				logger.debug("allTables.get(i)="+ allTables.get(i) );
				command = "SELECT time, value, units from "+ allTables.get(i)+" where timeMillis>=? and timeMillis<=? and  identityString=? order by time asc";
				logger.debug("command=" + command);

				preparedStatement = connection.prepareStatement(command);
				preparedStatement.setLong(1, startTimeMillis);
				preparedStatement.setLong(2, endTimeMillis);
				preparedStatement.setString(3, identityPointer);
				rs = preparedStatement.executeQuery();
				while(rs.next()){
					time=rs.getTimestamp(1);
					value = rs.getDouble(2);
					units = rs.getString(3);
					j = new JSONObject();
					j.put("Pulse Timestamp in Milliseconds", time.getTime());
					j.put("Value", value);
					j.put("Units", units);
					toReturn.put(j);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{

			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return toReturn;

	}

	public JSONArray getRemeberedDeneWord(TimeZone timeZone, String identityPointer,  long startTimeMillis, long  endTimeMillis){
		return getRemeberedDeneWordStart(  identityPointer,   startTimeMillis,   endTimeMillis);
		//		Connection connection=null;
		//		PreparedStatement preparedStatement = null; 
		//		ResultSet rs=null;
		//		JSONArray toReturn = new JSONArray();
		//		try {
		//			String command = "SELECT time, value from RememberedDeneWords where timeMillis>=? and timeMillis<=? and  identityString=? order by time asc";
		//			logger.info("command=" + command);
		//			connection = connectionPool.getConnection();
		//			preparedStatement = connection.prepareStatement(command);
		////			java.sql.Timestamp fromTimeValue = new java.sql.Timestamp(startTimeMillis);
		////			java.sql.Timestamp untilTimeValue = new java.sql.Timestamp(endTimeMillis);
		////			
		////			Calendar calendarTimeZone = Calendar.getInstance(timeZone);  
		////			preparedStatement.setTimestamp(1, fromTimeValue, calendarTimeZone);
		////			preparedStatement.setTimestamp(2, untilTimeValue, calendarTimeZone);
		//			preparedStatement.setLong(1, startTimeMillis);
		//			preparedStatement.setLong(2, endTimeMillis);
		//			preparedStatement.setString(3, identityPointer);
		//			
		//			rs = preparedStatement.executeQuery();
		//			Timestamp time=null;
		//			JSONObject j;
		//			double value;
		//			while(rs.next()){
		//				time=rs.getTimestamp(1);
		//				value = rs.getDouble(2);
		//				j = new JSONObject();
		//				j.put("Pulse Timestamp in Milliseconds", time.getTime());
		//				j.put("Value", value);
		//				toReturn.put(j);
		//			}
		//
		//
		//		} catch (SQLException e) {
		//			// TODO Auto-generated catch block
		//			logger.warn(Utils.getStringException(e));
		//
		//		}finally{
		//
		//			if(preparedStatement!=null)
		//				try {
		//					if(rs!=null)rs.close();
		//					preparedStatement.close();
		//				} catch (SQLException e) {
		//					// TODO Auto-generated catch block
		//					logger.debug(Utils.getStringException(e));
		//				}
		//			if(connection!=null)closeConnection(connection);
		//		}
		//
		//		return toReturn;

	}

	public JSONArray getMotherRemeberedValuesByRecordTimeInterval(long intervalStart,long intervalEnd){
		//	public JSONArray getMotherRemeberedValuesByRecordTime(long recordTime){
		Connection connection=null;
		PreparedStatement preparedStatement = null; 
		ResultSet rs=null;
		JSONArray toReturn = new JSONArray();
		Calendar cal = Calendar.getInstance();
		ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.MOTHER_REMEMBERED_VALUES_TABLE, intervalStart, intervalEnd);
		String command ="";
		try {
			long importedOnMillis=0;
			long recordMillis=0L;
			String label= null;
			double value=0;
			String unit= null;

			JSONObject j;

			connection = connectionPool.getConnection();
			for(int i=0;i<allTables.size();i++) {
				command = "SELECT importedOnMillis,recordMillis,label, value,unit from "+allTables.get(i) +  "  where recordMillis>=? &&   recordMillis<=? ";
				logger.debug("getMotherRemeberedValuesByRecordTime command=" + command);
				preparedStatement = connection.prepareStatement(command);
				preparedStatement.setLong(1, intervalStart);
				preparedStatement.setLong(2, intervalEnd);
				rs = preparedStatement.executeQuery();

				while(rs.next()){
					j = new JSONObject();
					j.put("ImportedOnMillis", rs.getLong(1));
					j.put("RecordMillis",rs.getLong(2));
					j.put("Label", rs.getLong(3));
					j.put("Value", rs.getLong(4));
					j.put("Unit", rs.getLong(5));
					logger.debug("getMotherRemeberedRecordsByRecordTime:" + j.toString(4));
					toReturn.put(j);
				}

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));

		}finally{

			if(preparedStatement!=null)
				try {
					if(rs!=null)rs.close();
					preparedStatement.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					logger.debug(Utils.getStringException(e));
				}
			if(connection!=null)closeConnection(connection);
		}

		return toReturn;

	}


	//	public JSONArray getMotherRemeberedValuesByRecordTimeInterval(long intervalStart,long intervalEnd){
	//		Connection connection=null;
	//		PreparedStatement preparedStatement = null; 
	//		ResultSet rs=null;
	//		JSONArray toReturn = new JSONArray();
	//		try {
	//			//
	//			//  first get all distinct recordMillis in the period
	//			ArrayList<String> allTables = this.getAllManagedTablesForAPeriod(TeleonomeConstants.MOTHER_REMEMBERED_VALUES_TABLE, intervalStart, intervalEnd);
	//			String command = "SELECT recordMillis from MotherRememberedValues where recordMillis>=? &&   recordMillis<=?";
	//			logger.info("command=" + command);
	//			connection = connectionPool.getConnection();
	//			
	//			
	//			preparedStatement = connection.prepareStatement(command);
	//			rs = preparedStatement.executeQuery();
	//			ArrayList<Long> recordsMillis = new ArrayList();
	//			while(rs.next()){
	//				recordsMillis.add(rs.getLong(1));
	//			}
	//			preparedStatement.close();
	//			//
	//			// now do it again, but every record,
	//			JSONArray completeRecordSetArray;
	//			for(int i=0;i<recordsMillis.size();i++) {
	//				completeRecordSetArray= getMotherRemeberedValuesByRecordTime(recordsMillis.get(i));
	//				toReturn.put(completeRecordSetArray);
	//			}
	//
	//		} catch (SQLException e) {
	//			// TODO Auto-generated catch block
	//			logger.warn(Utils.getStringException(e));
	//
	//		}finally{
	//
	//			if(preparedStatement!=null)
	//				try {
	//					if(rs!=null)rs.close();
	//					preparedStatement.close();
	//				} catch (SQLException e) {
	//					// TODO Auto-generated catch block
	//					logger.debug(Utils.getStringException(e));
	//				}
	//			if(connection!=null)closeConnection(connection);
	//		}
	//
	//		return toReturn;
	//
	//	}

	public boolean storeMotherRememberedValue( long importedOnMillis,long recordMillis,  String label,  Object value, String unit) {
		String sql="";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		boolean toReturn=false;
		Statement statement=null;
		try {
			connection = connectionPool.getConnection();
			//statement = connection.createStatement();
			Calendar cal = Calendar.getInstance();
			String tableName = getTableNameByCalendar(TeleonomeConstants.MOTHER_REMEMBERED_VALUES_TABLE,  cal);
			statement = connection.createStatement();
			if(!tableExists(tableName)) {
				sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.MOTHER_REMEMBERED_VALUES_TABLE +" with no data";
				int result = statement.executeUpdate(sql);
				logger.debug("table " + tableName + " was nt found so it was created, result=" + result);
			}
			sql = "insert into "+ tableName+" (importedOnMillis, recordMillis, label,value, unit) values(?,?,?,?,?,) ON CONFLICT(recordMillis, label) DO NOTHING";
			logger.debug("storeMotherRememberedValue=" + sql);
			//Calendar calendarTimeZone = Calendar.getInstance(timeZone);  

			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setLong(1, importedOnMillis);
			preparedStatement.setLong(2, recordMillis);

			preparedStatement.setString(3, label);
			preparedStatement.setDouble(4, (double)value);
			preparedStatement.setString(5, unit);

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
				if(statement!=null)statement.close();
				if(preparedStatement!=null)preparedStatement.close();
				if(connection!=null)connectionPool.closeConnection(connection);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.debug(Utils.getStringException(e));
			}
		}
		return toReturn;
	}

	public boolean unwrap( String teleonomeName, long pulseTimeMillis, String identityString, String valueType, Object value, String source, String units) {
		String sql="";
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		boolean toReturn=false;
		Statement statement=null;
		try {
			connection = connectionPool.getConnection();

			java.sql.Timestamp dateTimeValue = new java.sql.Timestamp(pulseTimeMillis);
			Calendar cal = Calendar.getInstance();
			String tableName = getTableNameByCalendar(TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE, cal);
			boolean tableExists = tableExists(tableName);
			logger.debug("line 4246 table " + tableName + "exists=" + tableExists);

			if(!tableExists) {
				//sql = "CREATE TABLE "+tableName+ " as table "+ TeleonomeConstants.REMEMBERED_DENEWORDS_TABLE +" with no data";
				
				sql = "CREATE TABLE "+tableName+ " (" +
						  "time        TIMESTAMPTZ  NOT NULL,"+
						  "timeMillis bigint NOT NULL,"+
						  "teleonomename    TEXT  NOT NULL,"+
						  "identitystring TEXT NOT NULL,"+
						  "source text not null,"+
						  "value    DOUBLE PRECISION  NULL,"+
						  "units text NOT NULL,"+
						  "CONSTRAINT pk_tbl_"+ tableName +" PRIMARY KEY (timeMillis,teleonomename, identitystring)"+
						")";
				logger.debug("line 4260 sql " + sql);
				
				
				statement = connection.createStatement();
				int result = statement.executeUpdate(sql);
				logger.debug("line 4264 table " + tableName + " was nt found so it was created, result=" + result);


//				sql = "alter table " + tableName + " add CONSTRAINT pk_tbl_"+ tableName+" PRIMARY KEY (timeMillis,teleonomename, identitystring)";
//				result = statement.executeUpdate(sql);
//				logger.debug("created primary key command executed, result=" + result);
			}


			sql = "insert into "+ tableName +" (time,timeMillis, teleonomeName,identityString,value, source, units) values(?,?,?,?,?,?,?) ON CONFLICT(timeMillis, teleonomeName,identityString) DO NOTHING";
			logger.debug("unwrap=" + sql);
			//Calendar calendarTimeZone = Calendar.getInstance(timeZone);  

			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setTimestamp(1, dateTimeValue);
			preparedStatement.setLong(2, pulseTimeMillis);

			preparedStatement.setString(3, teleonomeName);
			preparedStatement.setString(4, identityString);


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

				preparedStatement.setDouble(5, d);
			}else if(valueType.equals(TeleonomeConstants.DATATYPE_INTEGER)) {
				logger.debug("line 3251, value=" + value );
				if(value instanceof Integer) {
					d = ((Integer)value).doubleValue();
				}else if(value instanceof String) {
					d = new Double(Integer.parseInt((String)value));
				}else {
					d = ((Integer)value).doubleValue();
				}


				preparedStatement.setDouble(5, d);
			}else {
				preparedStatement.setDouble(5, (double) value);				
			}
			logger.debug("source=" + source);
			preparedStatement.setString(6, source);
			preparedStatement.setString(7, units);
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
				if(statement!=null)statement.close();
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
