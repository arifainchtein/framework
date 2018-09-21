package com.teleonome.framework.microcontroller;

import java.io.IOException;

import com.teleonome.framework.denome.DenomeManager;

public abstract class MotherMicroController extends MicroController {

	public MotherMicroController(DenomeManager d, String n) {
		super(d, n);
		// TODO Auto-generated constructor stub
	}

	public abstract String getCommandCode() throws IOException ;
	
}
