package com.teleonome.framework.microcontroller.ledborgmicrocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

import com.pi4j.wiringpi.SoftPwm;


public class LedborgWriter extends BufferedWriter {
	
	public static void SetLedBorg(float red, float green, float blue)
	{
		// Calculate PWM levels from 0.0 to 1.0
		int iRed = (int)(red * 100);
		int iGreen = (int)(green * 100);
		int iBlue = (int)(blue * 100);
System.out.println("in static setledborg, iRed=" + iRed + " iGreen=" + iGreen + " iBlue=" + iBlue);
		// Set the PWM levels
		SoftPwm.softPwmWrite(0, iRed);      // Red
		SoftPwm.softPwmWrite(2, iGreen);    // Green
		SoftPwm.softPwmWrite(3, iBlue);     // Blue
	}

	private Logger logger;
	
	public LedborgWriter(Writer out) {
		super(out);
		logger = Logger.getLogger(getClass());
		
		com.pi4j.wiringpi.Gpio.wiringPiSetup();


		// Create the software PWMs, 0 to 100
		SoftPwm.softPwmCreate(0, 0, 100);   // Red
		SoftPwm.softPwmCreate(2, 0, 100);   // Green
		SoftPwm.softPwmCreate(3, 0, 100);   // Blue
		// TODO Auto-generated constructor stub
		//
		// now get the timeout value from the system object
		//

		SetLedBorg(1.0f,0f,0f);
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SetLedBorg(0.0f,1.0f,0f);
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SetLedBorg(0.0f,0f,1.0f);
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SetLedBorg(0.0f,0.0f,0.0f);

		System.gc();
		logger.debug("Initialized LedborWriter");
		
	}
	
public void write(String command, int off, int len) throws IOException {
	
		String[] tokens = command.split("#");
		float redValue= (float)Integer.parseInt(tokens[0]);
		float greenValue= (float)Integer.parseInt(tokens[1]);
		float blueValue= (float)Integer.parseInt(tokens[2]);
		logger.debug(" LedborWriter received redValue=" + redValue + " greenValue=" + greenValue + " blueValue=" + blueValue);
		float redColorValue=(float)(redValue/255);
		float greenColorValue=(float)(greenValue/255);
		float blueColorValue=(float)(blueValue/255);

		SetLedBorg(redColorValue,greenColorValue,blueColorValue);
		
	}
}
