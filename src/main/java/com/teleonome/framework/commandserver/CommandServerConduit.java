/**
 * 
 */
package com.teleonome.framework.commandserver;

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import com.teleonome.framework.TeleonomeConstants;

public class CommandServerConduit{

		
		Socket serverSocket = null;
		OutputStream out = null;
		InputStream in = null;
		ObjectOutputStream objectOutputStream = null;
		ObjectInputStream objectInputStream =null;
		
		private OutputStream outputStream;
		private InputStream inputStream;
		private boolean channelOpen=false;
		
		
		public CommandServerConduit() throws UnknownHostException, IOException{
			 serverSocket = new Socket(TeleonomeConstants.COMMAND_SERVER_IP_LOCALHOST, TeleonomeConstants.COMMAND_SERVER_PORT);
			 outputStream = serverSocket.getOutputStream();
			 inputStream = serverSocket.getInputStream();
			 //objectOutputStream = new ObjectOutputStream(outputStream);
			//objectInputStream = new ObjectInputStream(inputStream); 
			channelOpen=true;
			System.out.println("DocManagerConduit connected to localhost:"+  TeleonomeConstants.COMMAND_SERVER_PORT);	
		}
		
		
		public CommandServerConduit(String serverIpAddress, int serverIpPort) throws UnknownHostException, IOException{
			System.out.println("DocManagerConduit is connecting to " + serverIpAddress + ":"+  serverIpPort);	
			 serverSocket = new Socket(serverIpAddress, serverIpPort);
			 outputStream = serverSocket.getOutputStream();
			 inputStream = serverSocket.getInputStream();
			 //objectOutputStream = new ObjectOutputStream(outputStream);
			
			channelOpen=true;
			System.out.println("DocManagerConduit connected to " + serverIpAddress + ":"+  serverIpPort);	
		}
		
		
		public void disconnect() throws IOException, ClassNotFoundException{					
			objectOutputStream.writeUTF("DisconnectUser?");
			objectOutputStream.writeObject(new Vector());				
			objectOutputStream.flush();
			
		}
		
		
		public Vector executeCommand(String command) throws IOException, ClassNotFoundException{
			
																									
			return executeCommand( command, new Vector());
			
		}
		 
		public boolean isConnectionOk(){
			Vector v;
			try {
				v = executeCommand("CheckConnectionStatus?", new Vector());
				if(v!=null && v.size()>0 && v.elementAt(0).equals("ok"))return true;
				else return false;
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		
		public Vector executeCommand(String command, Vector vector) throws IOException, ClassNotFoundException{
			//System.out.println("DocManagerConduit, about to send:" + command);
			//
			// before sending the command, check that the connection is still alive
			//
			System.out.println("execute command 1");
			objectOutputStream = new ObjectOutputStream(outputStream);
			objectOutputStream.writeUTF(command);
			objectOutputStream.writeObject(vector);
			objectOutputStream.flush();	
			System.out.println("execute command 2");
				
			
			//
			// the return object
			//
			objectInputStream = new ObjectInputStream(inputStream); 
			System.out.println("execute command 3");
			
			Vector v= (Vector) objectInputStream.readObject();
			System.out.println("execute command 4");
			
			objectOutputStream.reset();
			objectInputStream.close();
			return v;
			
		}
		
		
	}