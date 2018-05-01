package com.teleonome.framework.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.microcontroller.csvmicrocontroller.CSVMicroController;

public class TestStreams {

	public TestStreams(){

		CSVMicroController aMicroController = new CSVMicroController(null,null);
		BufferedWriter output=null;
		BufferedReader reader=null;
		String s;
		try {
			reader = aMicroController.getReader();
			output = aMicroController.getWriter();
			for(int i=0;i<18;i++){
				output.write("" + i);
				output.flush();

				s = reader.readLine();
				System.out.println("i=" + i + " s=" +s);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//new OutputStreamWriter(serialPort.getOutputStream());
		
		try {
			reader.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestStreams();
	}

}
