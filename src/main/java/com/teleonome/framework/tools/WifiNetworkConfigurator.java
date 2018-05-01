package com.teleonome.framework.tools;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import com.teleonome.framework.utils.Utils;

public class WifiNetworkConfigurator {

	public WifiNetworkConfigurator(){

		//
		// tp set not passowrd use
		//  set_network 0 key_mgmt NONE
		//
		Scanner scan=null;
		try {
			System.out.println("starting");
			ArrayList result;
			ArrayList listOfSSIDS;
			 String line;
			 boolean debug=false;
			//LinkedHashMap aps = Utils.getSSIDS();
			
			 result = executeCommand( "wpa_cli scan");
			Thread.sleep(5000);
			listOfSSIDS = executeCommand( "wpa_cli scan_results");
			 LinkedHashMap aps = new LinkedHashMap();
			
				String[] tokens;
				for(int i=2;i<listOfSSIDS.size();i++){
					line = (String)listOfSSIDS.get(i);
					//System.out.println("found ssid " + line);
					tokens = line.split("\t");
					aps.put(tokens[tokens.length-1], tokens[tokens.length-3]);
				}
				
			//
			// get current network
			
			String command="wpa_cli status";
			 result = executeCommand( command);
			String currentSSID="", currentId="", currentKeyMgmt="";
			for(int j=0;j<result.size();j++){
				line = (String) result.get(j);
				if(line.startsWith("ssid")){
					currentSSID= line.substring(5);
				}else if(line.startsWith("id=")){
					currentId=line.substring(3);
				}else if(line.startsWith("key_mgmt=")){
					currentKeyMgmt=line.substring(9);
				}
			}

			System.out.println("Currently connected to:" +  currentSSID + "   " +  currentId + " " + currentKeyMgmt);
			System.out.println(" " );
			 scan= new Scanner(System.in);
			boolean goodPassword=false;
			String password="";
			String Id="";
			
			Set  set = aps.entrySet();
			 Iterator iterator;
			 Map.Entry me;
			 int counter=1;
			found_password:
				for(int k=0;k<2;k++){
					command="wpa_cli add_network";
					result = executeCommand( command);
					Id =  (String) result.get(1);
					//System.out.println(command + "  " + result.get(0) + " " +Id);
					
					

			         // Displaying elements of LinkedHashMap
			          iterator = set.iterator();
			          counter=1;
			         while(iterator.hasNext()) {
			             me = (Map.Entry)iterator.next();
			            System.out.println("[" + counter + "] " + me.getKey() + " signal: "+me.getValue());
			            counter++;
			         }
			         System.out.println(" ");
			         
			         final Set<Entry<Integer, String>> mapValues = aps.entrySet();
			         final int maplength = mapValues.size();
			         final Entry<String,String>[] test = new Entry[maplength];
			         mapValues.toArray(test);


					boolean keepGoing=true;
					String selectedSSID="";
					String selectedLine="";
					boolean needPassword=false;
					while(keepGoing){
						System.out.println("*** Select a number from above");
						char c = scan.next().charAt(0);
						int x = Character.getNumericValue(c);
						if(x>0 && x<=aps.size()){
							selectedSSID=test[x-1].getKey();
							selectedLine=(String) listOfSSIDS.get(x+1);
							tokens = selectedLine.split("\t");
							if(debug)System.out.println("selectedLine=" + selectedLine);
							if(tokens[tokens.length-2].contains("WPA")){
								needPassword=true;
							}
							keepGoing=false;
						}
					}

					command="wpa_cli set_network "+ Id +" ssid \\\""+ selectedSSID +"\\\"";
					result = executeCommand( command);
					if(debug)System.out.println(command + "  " + result.get(0) + " " + result.get(1));
					
					if(needPassword){
						System.out.println("*** input password:");
						password="";
						while(password.length()!=8){
							password=scan.nextLine();
							if(password.length()!=8){
								System.out.println(password + " is not 8 characters");
							}
						}

						command="wpa_cli set_network "+ Id +" psk \\\""+ password +"\\\"";
						result = executeCommand( command);
						if(debug)System.out.println(password + "  " +  result.get(1));

					}else{
						command="wpa_cli set_network "+ Id+" key_mgmt NONE";
						result = executeCommand( command);
						if(debug)System.out.println("command no password=  " +  result.get(1));
					}
					
					
					command="wpa_cli select_network "+ Id;
					result = executeCommand( command);
					if(debug)System.out.println(command + "  " + result.get(0) + " " + result.get(1));
					boolean startedScanning=false;
					goodPassword=false;
					String value;
					keepGoing=true;
					found:
						while(keepGoing){
							command="wpa_cli status";
							result = executeCommand( command);
							for(int j=0;j<result.size();j++){
								line = (String) result.get(j);
								if(line.startsWith("wpa_state=")){
									value= line.substring(10);
									System.out.print(".");
									//System.out.print(value);
									if(value.equals("SCANNING") && !startedScanning){
										startedScanning=true;
									}
									if(value.equals("COMPLETED") ){
										goodPassword=true;
										keepGoing=false;
										break found;
									}else if(value.equals("DISCONNECTED") && startedScanning){
										keepGoing=false;
										break found;
									}
								}
							}
						}

					if(goodPassword){
						System.out.println("password  is GOOD ");
					}else{
						System.out.println("password is BAD ");					
					}
					break found_password;
				}
			System.out.println("************ Setting to original values ****************");

			//	command="wpa_cli set_network "+ currentId +" ssid \\\"  "+ currentSSID +"\\\"";
			//	result = executeCommand( command);
			//	System.out.println(command + "  " + result.get(0) + " " + result.get(1));

			if(goodPassword){
				command="wpa_cli select_network "+ Id;
				result = executeCommand( command);
				if(debug)System.out.println(command + "  " + result.get(0) + " " + result.get(1));

			}else{
				command="wpa_cli select_network "+ currentId;
				result = executeCommand( command);
				if(debug)System.out.println(command + "  " + result.get(0) + " " + result.get(1));

			}
			boolean keepGoing=true;
			while(keepGoing){
				command="wpa_cli status "+ currentId;
				result = executeCommand( command);
				for(int j=0;j<result.size();j++){
					line = (String) result.get(j);
					//System.out.println(line);
					if(line.startsWith("wpa_state=") ){
						if(line.substring(10).equals("COMPLETED")){
							keepGoing=false;
						}
						System.out.print(".");
					}
				}
			}
			System.out.println(" ");
			System.out.println("connected to:");
			for(int j=0;j<result.size();j++){
				line = (String) result.get(j);
				if(line.startsWith("ssid=") || line.startsWith("ip_address=")){
					System.out.println(line);
				}
			}
			
			command="sudo ifdown wlano; sudo ifup wlan0 ";
				result = executeCommand( command);
				if(debug)System.out.println(command + "  " + result);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			scan.close();
		}
	}



	private ArrayList executeCommand(String command) throws IOException, InterruptedException{
		Process process = Runtime.getRuntime().exec(new String[]{"bash","-c",command});

		//Process process =pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		ArrayList toReturn = new ArrayList();
		while ( (line = reader.readLine()) != null) {
			toReturn.add(line);
		}
		reader.close();
		process.waitFor();
		return toReturn;
	}


	public static void main(String[] args) {
		new WifiNetworkConfigurator();
	}

}
