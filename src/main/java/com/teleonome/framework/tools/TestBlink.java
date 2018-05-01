package com.teleonome.framework.tools;

import java.io.IOException;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class TestBlink {

	public TestBlink() {
		 try {
			Runtime.getRuntime().exec("sudo sh Blink.sh");
		} catch (IOException e) {
			// TODO Auto-gnanenerated catch block
			e.printStackTrace();
		}
		 
		 String command ="echo 1 >/sys/class/leds/led0/brightness";
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestBlink();
	}

}
