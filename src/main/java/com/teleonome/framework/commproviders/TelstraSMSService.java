package com.teleonome.framework.commproviders;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONObject;

import com.teleonome.framework.exception.CommunicationException;



public class TelstraSMSService extends CommunicationProvider {

	
	private final String APP_KEY="hZY3vrPtXOI0Vn8oeKTOAAJEM3laUP0C";
	private final String APP_SECRET="HuMvMgC1UXBiFGnA";
    private String RECIPIENT_NUMBER="0406759601";
	
	
	
	/*
	# * Recipient number should be in the format of "04xxxxxxxx" where x is a digit
	# * Authorization header value should be in the format of "Bearer xxx" where xxx is access token returned 
	#   from a previous GET https://api.telstra.com/v1/oauth/token request.
	RECIPIENT_NUMBER=your_number
	TOKEN=your_token
	 
	curl -H "Content-Type: application/json" \
	-H "Authorization: Bearer $TOKEN" \
	-d "{\"to\":\"$RECIPIENT_NUMBER\", \"body\":\"Hello!\"}" \
	"https://api.telstra.com/v1/sms/messages"
	*/
	
	
	public void sendMessage(String  message) throws CommunicationException {
		 //
		// get the tokern first
		//
		String token = getToken();
		
		String smsURL = "https://api.telstra.com/v1/sms/messages";
		//String payload = "{\"to\":\"" + RECIPIENT_NUMBER + "\", \"body\":\""+message +"\"}";
		String payload = "{\"to\":\"" + RECIPIENT_NUMBER + "\",\"body\":\"" + message + "\"}";
		String smsResponse = postMessage(smsURL, payload, token);
	}
	
	
	
	private String postMessage(String urlString, String payload, String token){
		 String line;
		    StringBuffer jsonString = new StringBuffer();
		    try {
		    	
		    	URL url = new URL(urlString);
		        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		        
		        connection.setDoInput(true);
		        connection.setDoOutput(true);
		        connection.setRequestMethod("POST");
		        connection.setSSLSocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault());
		        connection.setRequestProperty("Content-length", ""+payload.length());
		        connection.setRequestProperty("Accept", "application/json");
		        
		        connection.setRequestProperty("Authorization","Bearer "+ token);
		        connection.setRequestProperty("Content-Type", "application/json");
		        
		        
		       // DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
				//wr.writeBytes(payload);
			//	wr.flush();
			//	wr.close();
				
				
		        
		        OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
		        writer.write(payload);
		        writer.close();
		        int responseCode = connection.getResponseCode();
				
		        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		        while ((line = br.readLine()) != null) {
		                jsonString.append(line);
		        }
		        br.close();
		        connection.disconnect();
		        return jsonString.toString();
		    } catch (Exception e) {
		            throw new RuntimeException(e.getMessage());
		    }
	}
	
	private String getToken(){
		String line="";
		StringBuffer jsonString = new StringBuffer();
		
		try {
	    	String tokenURL = "https://api.telstra.com/v1/oauth/token?client_id="+ APP_KEY + "&client_secret=" + APP_SECRET + "&grant_type=client_credentials&scope=SMS";
		    URL url = new URL(tokenURL);

	        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
	        
	        //connection.setDoInput(true);
	        connection.setDoOutput(true);
	        connection.setRequestMethod("GET");
	        connection.setSSLSocketFactory((SSLSocketFactory)SSLSocketFactory.getDefault());
	        connection.setRequestProperty("Content-length", "");
	        //connection.setRequestProperty("Accept", "application/json");
	        
	        //connection.setRequestProperty("Authorization","Bearer "+ credential.getAccessToken());
	        //connection.setRequestProperty("Content-Type", "application/json");
	        //OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
	        //writer.write(payload);
	        //writer.close();
	        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        while ((line = br.readLine()) != null) {
	        	 
	                jsonString.append(line);
	        }
	        br.close();
	        connection.disconnect();
	        JSONObject obj = new JSONObject(jsonString.toString());
	        
	        return (String)obj.get("access_token"); //jsonString.toString();
	    } catch (Exception e) {
	            throw new RuntimeException(e.getMessage());
	    }
	}
	


}
