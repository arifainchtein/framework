package com.teleonome.framework.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

public class TeleonomeConnectionPool extends BasicDataSource{

	private int currentNumberConnections=0;
	
	public TeleonomeConnectionPool(){
		
	}
	
	public Connection getConnection() throws SQLException{
		currentNumberConnections++;
		//System.out.println("currentNumberConnections=" + currentNumberConnections);
		return super.getConnection();
	}
	 
	public void closeConnection(Connection con) throws SQLException{
		con.close();
		currentNumberConnections--;
		//System.out.println("closing currentNumberConnections=" + currentNumberConnections);
		
	}
	
	public int getCurrentNumberConnections(){
		return currentNumberConnections;
	}
}

