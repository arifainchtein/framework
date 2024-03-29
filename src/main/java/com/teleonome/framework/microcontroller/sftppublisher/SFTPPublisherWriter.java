package com.teleonome.framework.microcontroller.sftppublisher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.DenomeUtils;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.CommunicationException;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.utils.StringCompressor;
import com.teleonome.framework.utils.Utils;

public class SFTPPublisherWriter extends BufferedWriter implements SftpProgressMonitor{


	private String publishingResults="";
	private String mqttBrokerAddress;

	private Category logger;
	private  int localPort = 8888;
	private int remotePort = 8888;
	private int sshPort=22;
	private String userName="pi";
	JSONArray configParams;
	String host="", privateKey="";
	DenomeManager aDenomeManager;
	JSch jsch=new JSch();
	Session session=null;
	String teleonomeName="";


	public SFTPPublisherWriter(Writer out, JSONArray a, DenomeManager d) {
		super(out);
		logger = Logger.getLogger(getClass());
		configParams=a;
		aDenomeManager=d;
		JSONObject dene;
		String value="", deneName;
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				deneName = dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
				if(deneName.equals("SFTP Server IP Address")){
					host = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, "SFTP Server IP Address", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}else if(deneName.equals("SFTP Key File Name")) {
					privateKey = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, "SFTP Key File Name", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	public String getPublishingResults(){
		return publishingResults;
	}
	/**
	 * IN this case, the command will contain the entire pulse, because
	 * this writer is only called after the pulse has been completed
	 * and therefore the 
	 */
	public void write(String command, int off, int len) throws IOException {
		publishingResults="";
		logger.debug("received command:" + command);
		//
		// put a three second delay,
		// this is because the AsyncCycle puts a 3 second delay
		// after writing the message
		// this is because otherwise you run the risk of floding the serial bus
		// so to make it standards all microcontrollers do this

		if(command.equals("Publish Via SFTP")) {			
			boolean createdTunnel = createTunnel();
			logger.debug("create tunnel returned" + createdTunnel);

			if(createdTunnel) {
				boolean publishToSFTP = publishToSFTP();
				if(publishToSFTP)publishingResults="Ok-publishing SFTP";
				session.disconnect();
			}
		}else if(command.startsWith("Download Via SFTP")){
			String[] tokens = command.split("#");

			String localDestinationFilename = tokens[1];
			String remoteSourceFileName=tokens[2];

			boolean createdTunnel = createTunnel();
			logger.debug("create tunnel returned" + createdTunnel);

			if(createdTunnel) {
				boolean publishToSFTP = getFromSFTP(localDestinationFilename, remoteSourceFileName);
				if(publishToSFTP)publishingResults="Ok-publishing SFTP";
				session.disconnect();
			}
		}else {
			publishingResults="Ok";
		}
	}




	private boolean createTunnel() {

		JSch.setLogger(new MyLogger());
		String privateKey = "/home/pi/.ssh/dg";
		File privateKeyFile = new File(privateKey);
		logger.debug("creating tunnel privateKeyFile is File=" + privateKeyFile.isFile());

		if(!privateKeyFile.isFile()) {
			publishingResults="Fault#DigitalGeppettoPublisher#Missing Key";
			return false;
		}
		logger.debug("creating tunnel about to read key=" );
		byte[] prvkey=null;
		try {
			prvkey = FileUtils.readFileToByteArray(new File(privateKey));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			publishingResults="Fault#DigitalGeppettoPublisher#Error Reading Key File";
			return false;

		}
		logger.debug("creating tunnel about to read passphrase, prvkey="  + prvkey );

		String passphrase="";
		final byte[] emptyPassPhrase = passphrase.getBytes();
		try {
			jsch.addIdentity(
					userName,    // String userName 
					prvkey,          // byte[] privateKey 
					null,            // byte[] publicKey
					emptyPassPhrase  // byte[] passPhrase
					);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			publishingResults="Fault#DigitalGeppettoPublisher#Error Adding Identity to JSch";
			return false;
		}
		logger.debug("creating tunnel about to get session, host=" + host + " sshPort=" + sshPort );

		try {
			session=jsch.getSession(userName, host, sshPort);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			publishingResults="Fault#DigitalGeppettoPublisher#Error Reading Getting Session";
			return false;
		}

		try {
			jsch.addIdentity(privateKey);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			publishingResults="Fault#DigitalGeppettoPublisher#Error Adding Identity";
			return false;
		}
		logger.debug("identity added, abut to connect ");
		final Properties config = new Properties();  
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		try {
			session.connect(60000);
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
			publishingResults="Fault#DigitalGeppettoPublisher#Error Connecting";
			return false;
		}
		int assinged_port=0;

		//
		// this is to create a tunnel if you want to run a heart publisher

		logger.debug(" connected" );

		// make sure that there us the directory for this teleonome
		//
		Channel channel;
		try {
			channel = session.openChannel("sftp");
			ChannelSftp sftp = (ChannelSftp) channel;
			sftp.connect();
			teleonomeName =  aDenomeManager.getDenomeName();
			String currentRemoteDir = sftp.pwd();
			logger.debug("creating teleonome directory currentRemoteDir" + currentRemoteDir);

			sftp.cd("Teleonome");
			//
			// now chck to see if the directory exists
			//
			boolean found=false;
			Vector files = sftp.ls(".");
			ChannelSftp.LsEntry fileName=null;

			Iterator itFiles = files.iterator();
			while (itFiles.hasNext()) {
				try{
					fileName = (ChannelSftp.LsEntry)itFiles.next();
					logger.debug("fileName=" + fileName.getFilename());
					if(fileName.getFilename().equals(teleonomeName)){
						logger.debug("found directory " + teleonomeName);
						found=true;
					}
				}catch(NoSuchElementException e){
					logger.debug("exception e=" + e.getMessage());
				}

			}

			if(!found)sftp.mkdir(teleonomeName);
			sftp.disconnect();

		} catch (JSchException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			logger.warn(Utils.getStringException(e));
		}

		logger.debug("assinged_port:"+assinged_port+" -> "+remotePort+":"+remotePort);
		return true;

	}

	private boolean getFromSFTP(String localDestinationFilename, String remoteSourceFileName) {
		OutputStream output=null;
		boolean b=false;
		// command = "scp -p -t \"" + destFilename + "\"";
		Channel channel = null;
		try {
			channel = session.openChannel("sftp");
		} catch (JSchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ChannelSftp sftp = (ChannelSftp) channel;

		try {
			sftp.connect();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//send the contents of the source file

		try {
			output = new FileOutputStream(localDestinationFilename);
			sftp.get(remoteSourceFileName,output);

		} catch (IOException ee) {
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		if(channel!=null) {
			channel.disconnect();
		}
		// session.disconnect();

		b= true;


		return b;
	}

	private boolean publishToSFTP() {


		Integer I;
		String  dataString="dataString"; 
		JSONObject dene;
		String value="", deneName;

		//
		// to start the transaction, upload and empty file called start
		//
		String destinationDir="/home/pi/Teleonome/" + teleonomeName;
		String sourceFilename = Utils.getLocalDirectory() + "start";
		try {
			FileUtils.writeStringToFile(new File(sourceFilename), "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		uploadFile( destinationDir, sourceFilename,   "start");
		//
		// since there is a strong chance that the pulse will be published
		// we need to check if there are any any denes of Dene Type DENE_TYPE_DENOMIC_OPERATION
		// because these denes are there to modify the denome before it gets published
		// Note that thismodification is never stored, this is so that the modified denemoe is sent
		// to digital geppetto.
		// this will be used for example to hde the settings and identity pages in the lower navigation
		// for this reason we need to loop over the configParams twice, the first time looking for the config params which have a denetype of DENE_TYPE_DENOMIC_OPERATION
		// perform the operation and then loop again to actually publish the pulse and other info
		//
		// first read the denome from the drive
		//
		File selectedFile = new File(Utils.getLocalDirectory() + "Teleonome.denome");
		if(!selectedFile.isFile()) {
			publishingResults="Fault#DigitalGeppettoPublisher#Error File Teleonome.denome not found";
			return false;
		}
		String fileInString = "";
		try{
			fileInString= FileUtils.readFileToString(selectedFile);
		}catch(IOException e) {
			logger.warn(Utils.getStringException(e));
			publishingResults="Fault#DigitalGeppettoPublisher#Error Reading Teleonome.denome";
			return false;
		}
		JSONObject tempPulseJSONObject = new JSONObject(fileInString);
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				logger.debug("dene " + dene.getString("Name") + " has denetype "+dene.has(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE));
				if(dene.has(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE)) {
					if(dene.getString(TeleonomeConstants.DENE_DENE_TYPE_ATTRIBUTE).equals(TeleonomeConstants.DENE_TYPE_DENOMIC_OPERATION)) {
						JSONObject deneWordOperationJSONObject = (JSONObject) DenomeUtils.getDeneWordAttributeByDeneWordTypeFromDene(dene, TeleonomeConstants.DENEWORD_TYPE_UPDATE_DENEWORD_VALUE, TeleonomeConstants.COMPLETE);
						String targetPointer = deneWordOperationJSONObject.getString(TeleonomeConstants.DENEWORD_TARGET_ATTRIBUTE);
						Object targetValue = deneWordOperationJSONObject.get(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
						logger.debug("about to update the denome before publishing,targetPointer=" + targetPointer + " targetValue="+targetValue );
						tempPulseJSONObject = DenomeUtils.updateDeneWordByIdentity(tempPulseJSONObject,  targetPointer,  targetValue);
					}
				}
			}catch(Exception e) {
				logger.warn(Utils.getStringException(e));
			}
		}
		for(int i=0;i<configParams.length();i++){
			try {
				dene = configParams.getJSONObject(i);
				deneName = dene.getString(TeleonomeConstants.DENEWORD_NAME_ATTRIBUTE);
				logger.debug("publishToSFTP deneName=" + deneName );

				if(deneName.startsWith("SFTP Upload Image")) {
					String imagePointer = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, "Upload Image", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);

					// 
					// in this case the value is a pointer to where the file is.
					// ie
					//
					//                    {
					//                        "Required": true,
					//                        "Value": "@DamnWatcher:Purpose:Sensor Data:RaspberryPi Camera:FileName",
					//                        "Name": "Upload Image",
					//                        "Value Type": "Dene Pointer"
					//                    }
					//
					// now render the pointer to get the name of the file to be uploaded
					try {
						value = (String) aDenomeManager.getDeneWordAttributeByIdentity(new Identity(imagePointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					} catch (InvalidDenomeException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//
					// 
					// if the file is an image, ie the file name ends up with a jpg
					// then you need to add /home/pi/Teleonome/tomcat/webapps/ROOT/
					// because the images will be there, if audio in the audio folder an video in the video folder
					if(value.endsWith(".jpg") || value.endsWith(".png") || value.endsWith(".gif")){
						sourceFilename = "/home/pi/Teleonome/tomcat/webapps/ROOT/images/" + value;
					}else if(value.endsWith(".wav") || value.endsWith(".mp3") ){
						sourceFilename = "/home/pi/Teleonome/tomcat/webapps/ROOT/audio/" + value;
					}else if(value.endsWith(".mpg")  ){
						sourceFilename = "/home/pi/Teleonome/tomcat/webapps/ROOT/video/" + value;
					}
					destinationDir="/home/pi/Teleonome/" + teleonomeName;

					if(new File(sourceFilename).isFile()) {
						uploadFile( destinationDir, sourceFilename,   value);
					}else {
						logger.debug("Did not upload " + sourceFilename + " because it could not be found");
					}

				}else if(deneName.equals("SFTP Publish Contents")) {
					String contentsPointer = (String) aDenomeManager.getDeneWordAttributeByDeneWordNameFromDene(dene, "SFTP Publish Contents", TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
					logger.debug("contentsPointer " + contentsPointer);
					if(contentsPointer.equals(TeleonomeConstants.COMMANDS_PUBLISH_TELEONOME_PULSE)) {

						//
						// compress the file and save it to upload it and then delete it
						//
						//byte[] messageBytes = StringCompressor.compress(fileInString);
						
						File sourceFile = new File("/home/pi/Teleonome/Teleonome.denome");
						logger.debug("sourceFilename " + sourceFilename);
						try {
							//FileUtils.writeStringToFile(sourceFile, tempPulseJSONObject.toString(4), "UTF8");
							//FileUtils.writeByteArrayToFile(tempFile, messageBytes);
							destinationDir="/home/pi/Teleonome/" + teleonomeName;
							//sourceFilename = Utils.getLocalDirectory() + "Teleonome.denome";
							 logger.debug("sending the denome file, destinationDir=" + destinationDir);
							//	uploadFile( destinationDir, sourceFilename,   "Teleonome.denome");
							
							String compressedFileName="/home/pi/Teleonome/Teleonome.zip";
							File compressedFile = new File(compressedFileName);

							Utils.zipFile(sourceFile, compressedFile);
							logger.debug("calling uploadfile the zip file, compressedFileName=" + compressedFileName);
							uploadFile( destinationDir, compressedFileName,   "Teleonome.zip");

							sourceFile.delete();
							compressedFile.delete();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							logger.warn(Utils.getStringException(e));
						}
					}
				}else {
					//
					// if we are here is because there are other parameters that are not 
					// the  content of the image, specifically could be parameters that
					// describe 
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		//
		// to end the transaction, upload and empty file called complete
		//
		destinationDir="/home/pi/Teleonome/" + teleonomeName;
		sourceFilename = Utils.getLocalDirectory() + "complete";
		try {
			FileUtils.writeStringToFile(new File(sourceFilename), "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		uploadFile( destinationDir, sourceFilename,   "complete");

		return true;
	}

	

	private boolean uploadFile(String destinationDir,String sourceFilename,  String destFilename) {
		FileInputStream fis = null;
		boolean b=false;
		try {
			// command = "scp -p -t \"" + destFilename + "\"";
			Channel channel = session.openChannel("sftp");
			ChannelSftp sftp = (ChannelSftp) channel;

			sftp.connect();

			//send the contents of the source file
			fis = new FileInputStream(sourceFilename);
			String currentRemoteDir = sftp.pwd();

			sftp.cd(destinationDir);
			currentRemoteDir = sftp.pwd();
			logger.debug("uploading file in " + destinationDir + " sourceFilename=" + sourceFilename  + " destFilename=" + destFilename);
			sftp.put(sourceFilename, destFilename, this,ChannelSftp.OVERWRITE);
			fis.close();

			channel.disconnect();
			// session.disconnect();

			return true;
		} catch (Exception e) {
			logger.warn(Utils.getStringException(e));
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (Exception ee) {
				logger.warn(Utils.getStringException(e));
			}
		}finally {
			try {
				if(fis!=null)fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return b;
	}


	public static class MyLogger implements com.jcraft.jsch.Logger {
		static java.util.Hashtable name=new java.util.Hashtable();
		static{
			name.put(new Integer(DEBUG), "DEBUG: ");
			name.put(new Integer(INFO), "INFO: ");
			name.put(new Integer(WARN), "WARN: ");
			name.put(new Integer(ERROR), "ERROR: ");
			name.put(new Integer(FATAL), "FATAL: ");
		}
		public boolean isEnabled(int level){
			return true;
		}
		public void log(int level, String message){
			//System.err.print(name.get(new Integer(level)));
			//System.err.println(message);
		}
	}


	long count=0;
	long max=0;
	public void init(int op, String   src, String   dest, long m){
		this.max=m;
		count=0;
		percent=0;

	}

	private long percent=-1;

	public boolean count(long c){
		this.count+=c;
		percent = (count*100)/max;
		return true;
	}

	public void end(){

	}

}
