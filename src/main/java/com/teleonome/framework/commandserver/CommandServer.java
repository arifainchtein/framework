package com.teleonome.framework.commandserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import com.teleonome.framework.TeleonomeConstants;


public class CommandServer extends Thread{

	private ServerSocket incomingServerSocket;
	private int monitoringFrequencySeconds=10;
	public Vector commandsToExecute = new Vector();



	public CommandServer(){
		try {
			incomingServerSocket = new ServerSocket(TeleonomeConstants.COMMAND_SERVER_PORT, 60);
			start();
			System.out.println("CommandServer Started on port " + TeleonomeConstants.COMMAND_SERVER_PORT);
			
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	public void run(){
		Socket incoming;
		CommandServerConnection serverConnection;
		for (;;){  
			try{
				incoming = incomingServerSocket.accept( );
				InetAddress playerAddress = incoming.getInetAddress();
				System.out.println();
				
				System.out.println("Received connection from " + playerAddress.toString() + " commandsToExecute=" + commandsToExecute);
				serverConnection = new CommandServerConnection(incoming);
				serverConnection.start();
				try {
					Thread.sleep(monitoringFrequencySeconds*1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}

			}catch(IOException e){					
				System.out.println("error connecting e=" + e);
				e.printStackTrace();
			}
		}
	}


	class CommandServerConnection extends Thread{

		private Socket socket;
		InputStreamReader inputStream;
		OutputStream outputStream;
		String parameters;
		
		public CommandServerConnection(Socket s){
			socket=s;

		}
		public void run(){
			InputStream inputStream=null;
			String nextCommand;
			try {
				inputStream = socket.getInputStream();
				//System.out.println("point 1");

				ObjectInputStream returningObjectInputStream = null;
				//System.out.println("point 2");


				returningObjectInputStream = new ObjectInputStream(inputStream);
				//System.out.println("point 3");

				String commandString = returningObjectInputStream.readUTF();
				//System.out.println("point 4");

				String actualCommand = commandString.substring(0, commandString.indexOf("?")) ;
				Vector toReturn = new Vector();
				
				if(actualCommand.equals("NextCommandToProcess")){		
				
					if(commandsToExecute.size()>0){
						nextCommand = (String) commandsToExecute.elementAt(0);
						toReturn.addElement(nextCommand);
					}else{
						
						toReturn.addElement(TeleonomeConstants.COMMAND_QUEUE_EMPTY);
					}
					

				}else if(actualCommand.equals("RequestCommand")){	
					parameters = commandString.substring(commandString.indexOf("?")+1) ;
					Hashtable parsedParameters = parseParameters(parameters);
					String command=(String) parsedParameters.get("Command");
					
					commandsToExecute.addElement(command);
					toReturn.addElement("Requested");
					
					
				}else if(actualCommand.equals("ProcessedCommand")){	
					parameters = commandString.substring(commandString.indexOf("?")+1) ;
					Hashtable parsedParameters = parseParameters(parameters);
					
					String result =(String) parsedParameters.get("Result");
					String command=(String) parsedParameters.get("Command");
					
					if(result!=null && result.equals("OK")){
						commandsToExecute.removeElement(command);
					}
					toReturn.addElement(TeleonomeConstants.COMMAND_QUEUE_EMPTY);
				}
				
				System.out.println(" line 138 commandsToExecute=" + commandsToExecute);
				
				OutputStream outputStream = socket.getOutputStream();
				ObjectOutputStream sendCommandObjectOutputStream = new ObjectOutputStream(outputStream);
				sendCommandObjectOutputStream.writeObject(toReturn);
				sendCommandObjectOutputStream.flush();
				sendCommandObjectOutputStream.close();
				returningObjectInputStream.close();
				inputStream.close();
				
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				e1.printStackTrace();
			}
		}

		protected Hashtable parseParameters(String parameters){
			Hashtable h = new Hashtable();
			//
			// first get the parameter pairs
			//
			String chain="", key="", value="";
			System.out.println("Parsing parameters in DocServerCommand, parameters=" + parameters);
			StringTokenizer st = new StringTokenizer(parameters,"&");
			StringTokenizer st2;
			while(st.hasMoreTokens()){
				chain = st.nextToken();
				st2 = new StringTokenizer(chain,"=");
				key= st2.nextToken();
				value= st2.nextToken();
				h.put(key,value);
			}
			return h;
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stu
		new com.teleonome.framework.commandserver.CommandServer();
	}
	
}