package com.teleonome.framework.microcontroller.raspberrypicameracontroller;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.utils.Utils;

public class RaspberryPiCameraWriter extends BufferedWriter{

	RaspberryPiCameraReader aRaspberryPiCameraReader;
	Logger logger;
	private String destinationDirectory="/home/pi/Teleonome/tomcat/webapps/ROOT/";
	private long streamingServerPid=0;
	private int streamingServerPort=18745;
	private String videoFileName = "test_video.h264";
	
	public RaspberryPiCameraWriter(Writer out, RaspberryPiCameraReader c) {
		super(out);
		aRaspberryPiCameraReader=c;
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}

	public void write(String command, int off, int len) throws IOException {

		if(command.equals("GetSensorData")){
			aRaspberryPiCameraReader.setCurrentCommand(command);
		}else if(command.equals("StartStreamVideo")){
			//
			// first open the firewall
			//
			String openPortForStreamingCommand = "sudo ufw allow " + streamingServerPort;
			logger.info("RaspberryPiCameraWriter opened port for streaming=" + openPortForStreamingCommand);
			try {
				ArrayList results = Utils.executeCommand(openPortForStreamingCommand);
				logger.info("results=" + results);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
				logger.warn(Utils.getStringException(e));
			}
			String cameraCommand = "raspivid -o - -t 0 | tee " + videoFileName + " | cvlc -v stream:///dev/stdin --sout '#standard{access=http,mux=ts,dest=:8080' :demux=h264";
			try {
				ArrayList results = startStreamingServer(cameraCommand);
				logger.info("results=" + results);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}	
		}else if(command.equals("StopVideo")){
			//
			// there are two things that happens when the video stop			
			//
			// first close the server
			//
			String closeServerCommand = "sudo kill -9 " + streamingServerPid;
			try {
				ArrayList results = Utils.executeCommand(closeServerCommand);
				logger.info("RaspberryPiCameraWriter closing streaming server results=" + results);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			
			//
			// second you close the port in the firewall
			//
			String closeFirewallPortForStreamingCommand = "sudo ufw deny " + streamingServerPort;
			logger.info("RaspberryPiCameraWriter close port for streaming=" + closeFirewallPortForStreamingCommand);
			try {
				ArrayList results = Utils.executeCommand(closeFirewallPortForStreamingCommand);
				logger.info("RaspberryPiCameraWriter closed firewall port results=" + results);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			
		}else if(command.equals("StartStreamAndSaveVideo")){
			//
			// first open the firewall
			//
			String openPortForStreamingCommand = "sudo ufw allow " + streamingServerPort;
			logger.info("RaspberryPiCameraWriter opened port for streaming=" + openPortForStreamingCommand);
			try {
				ArrayList results = Utils.executeCommand(openPortForStreamingCommand);
				logger.info("results=" + results);
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}
			
			LocalDateTime currentTime = LocalDateTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.CAMERA_TIMESTAMP_FILENAME_FORMAT);
			String formatedCurrentTime = currentTime.format(formatter);
			String fileName=formatedCurrentTime + ".jpg";
			String cameraCommand = "raspivid -t -0 -w 1280 -h 720 -fps 25 -b 2000000 -o - | ffmpeg -i - -vcodec copy -an -f flv -metadata streamName=myStream tcp://0.0.0.0:6666";
			logger.info("RaspberryPiCameraReader cameraCommand=" + cameraCommand);

			try {
				ArrayList results = startStreamingServer(cameraCommand);
				logger.info("results=" + results);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}				
		}else if(command.equals("StartSaveVideo")){
			//
			// this is the same as the StartStreamAndSave except we dont open the port
			// that way even if the server is streaming, nobody will get to it because the port is
			// not open
			String cameraCommand = "raspivid -o - -t 0 | tee test_video.h264 | cvlc -v stream:///dev/stdin --sout '#standard{access=http,mux=ts,dest=:8080' :demux=h264";
			try {
				ArrayList results = startStreamingServer(cameraCommand);
				logger.info("results=" + results);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.warn(Utils.getStringException(e));
			}	
		}else if(command.startsWith("DownloadFile")){
			
		}else if(command.startsWith("DeleteFile")){
			//
			// the command to delete the files is
			// sudo find /home/pi/Teleonome/tomcat/webapps/ROOT/*.jpg -type f ! -name '28-11-2017*.jpg' -delete
		}	
	}

	private ArrayList startStreamingServer(String cameraCommand) throws IOException{
		//
		// Start the server
		//
		LocalDateTime currentTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.CAMERA_TIMESTAMP_FILENAME_FORMAT);
		String formatedCurrentTime = currentTime.format(formatter);
		String fileName=formatedCurrentTime + ".jpg";
		logger.info("RaspberryPiCameraReader cameraCommand=" + cameraCommand);

		Process process = Runtime.getRuntime().exec(new String[]{"sh","-c",cameraCommand});
		//System.out.println("created process for the command:" + command);

		ArrayList toReturn = new ArrayList();

		//System.out.println("executeCommand about to start reader");

		//Process process =pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line = null;
		//System.out.println("executeCommand reader created:" + command);

		
		while ( (line = reader.readLine()) != null) {
			logger.debug("adding line:" + line);
			toReturn.add(line);
		}
		reader.close();

		try {
			if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
				Field f = process.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				streamingServerPid = f.getLong(process);
				f.setAccessible(false);
			}
		} catch (Exception e) {
			streamingServerPid = -1;
		}
		return toReturn;
	}




}
