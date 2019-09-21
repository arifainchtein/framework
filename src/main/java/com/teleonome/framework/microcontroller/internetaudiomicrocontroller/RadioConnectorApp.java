package com.teleonome.framework.microcontroller.internetaudiomicrocontroller;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class RadioConnectorApp
{
	public static void main ( String[] args )
	{
		String address = 	"http://www.70sradiohits.com ";
		address = "http://streams.kqed.org/kqedradio.m3u";
		address= " https://streams.kqed.org/kqedradio";
		//  address =( "http://radio.flex.ru:8000/radionami" );
		// address= ( "http://bbcwssc.ic.llnwd.net/stream/bbcwssc_mp1_ws-einws");
		//address = "http://stream.krushradio.com/stream";
		try
		{

			
			if(address.contains(".m3u")) {
				 address = getM3Url(address);
			}
//				try {
//					streamSampledAudio ( url);
//				} catch (UnsupportedAudioFileException | LineUnavailableException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}else {
				playRadioStream ( address);
//			}

		}
		catch ( IOException e )
		{
			e.printStackTrace ();
		}
		catch ( JavaLayerException e )
		{
			e.printStackTrace ();
		}
	}

//	/** Read sampled audio data from the specified URL and play it */
//	public static void streamSampledAudio1(URL url)
//			throws IOException, UnsupportedAudioFileException,
//			LineUnavailableException
//	{
//		AudioInputStream ain = null;  // We read audio data from here
//		SourceDataLine line = null;   // And write it here.
//
//		try {
//			// Get an audio input stream from the URL
//			ain=AudioSystem.getAudioInputStream(url);
//
//			// Get information about the format of the stream
//			AudioFormat format = ain.getFormat( );
//			DataLine.Info info=new DataLine.Info(SourceDataLine.class,format);
//
//			// If the format is not supported directly (i.e. if it is not PCM
//			// encoded), then try to transcode it to PCM.
//			if (!AudioSystem.isLineSupported(info)) {
//				// This is the PCM format we want to transcode to.
//				// The parameters here are audio format details that you
//				// shouldn't need to understand for casual use.
//				AudioFormat pcm =
//						new AudioFormat(format.getSampleRate( ), 16,
//								format.getChannels( ), true, false);
//
//				// Get a wrapper stream around the input stream that does the
//				// transcoding for us.
//				ain = AudioSystem.getAudioInputStream(pcm, ain);
//
//				// Update the format and info variables for the transcoded data
//				format = ain.getFormat( ); 
//				info = new DataLine.Info(SourceDataLine.class, format);
//			}
//
//			// Open the line through which we'll play the streaming audio.
//			line = (SourceDataLine) AudioSystem.getLine(info);
//			line.open(format);  
//
//			// Allocate a buffer for reading from the input stream and writing
//			// to the line.  Make it large enough to hold 4k audio frames.
//			// Note that the SourceDataLine also has its own internal buffer.
//			int framesize = format.getFrameSize( );
//			byte[  ] buffer = new byte[4 * 1024 * framesize]; // the buffer
//			int numbytes = 0;                               // how many bytes
//
//			// We haven't started the line yet.
//			boolean started = false;
//
//			for(;;) {  // We'll exit the loop when we reach the end of stream
//				// First, read some bytes from the input stream.
//				int bytesread=ain.read(buffer,numbytes,buffer.length-numbytes);
//				// If there were no more bytes to read, we're done.
//				if (bytesread == -1) break;
//				numbytes += bytesread;
//
//				// Now that we've got some audio data to write to the line,
//				// start the line, so it will play that data as we write it.
//				if (!started) {
//					line.start( );
//					started = true;
//				}
//
//				// We must write bytes to the line in an integer multiple of
//				// the framesize.  So figure out how many bytes we'll write.
//				int bytestowrite = (numbytes/framesize)*framesize;
//
//				// Now write the bytes. The line will buffer them and play
//				// them. This call will block until all bytes are written.
//				line.write(buffer, 0, bytestowrite);
//
//				// If we didn't have an integer multiple of the frame size, 
//				// then copy the remaining bytes to the start of the buffer.
//				int remaining = numbytes - bytestowrite;
//				if (remaining > 0)
//					System.arraycopy(buffer,bytestowrite,buffer,0,remaining);
//				numbytes = remaining;
//			}
//
//			// Now block until all buffered sound finishes playing.
//			line.drain( );
//		}
//		finally { // Always relinquish the resources we use
//			if (line != null) line.close( );
//			if (ain != null) ain.close( );
//		}
//	}

	private static void playRadioStream ( String spec ) throws IOException, JavaLayerException
	{
		// Connection
		URLConnection urlConnection = new URL ( spec ).openConnection ();

		// If you have proxy
		//        Properties systemSettings = System.getProperties ();
		//        systemSettings.put ( "proxySet", true );
		//        systemSettings.put ( "http.proxyHost", "host" );
		//        systemSettings.put ( "http.proxyPort", "port" );
		// If you have proxy auth
		//        BASE64Encoder encoder = new BASE64Encoder ();
		//        String encoded = encoder.encode ( ( "login:pass" ).getBytes () );
		//        urlConnection.setRequestProperty ( "Proxy-Authorization", "Basic " + encoded );

		// Connecting
		urlConnection.connect ();

		// Playing
		Player player = new Player ( urlConnection.getInputStream () );
		player.play ();
	}

	public static String getM3Url(String url) {
		
		URL u;
		InputStream is = null;
		DataInputStream dis;
		String s;
		URL returnURL=null;
		String addressUrl=null;
		try {
			u = new URL(url);
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			
			while ((s = dis.readLine()) != null)
			{
				System.out.println("received=" + s);
				addressUrl=s;				
			}
			if(addressUrl!=null)returnURL = new URL(addressUrl);
		}catch (MalformedURLException mue) {
			System.err.println("Ouch - a MalformedURLException happened.");
			mue.printStackTrace();
			System.exit(2);
		} catch (IOException ioe) {
			System.err.println("Oops- an IOException happened.");
			ioe.printStackTrace();
			System.exit(3);
		}finally {
			try {
				is.close();
			} catch (IOException ioe) {
			}
		}
		return addressUrl;
	}
}
