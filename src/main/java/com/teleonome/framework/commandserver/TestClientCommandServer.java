package com.teleonome.framework.commandserver;

import java.io.IOException;

import com.teleonome.framework.TeleonomeConstants;

public class TestClientCommandServer {

	public TestClientCommandServer(){
		String command;
		try {
			CommandServerConduit commandServerConduit = new CommandServerConduit();
			
			String response = (String) commandServerConduit.executeCommand("RequestCommand?Command=Turn LCD On").elementAt(0);
			System.out.println("Response to RequestCommand:" + response);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestClientCommandServer();
	}

}
