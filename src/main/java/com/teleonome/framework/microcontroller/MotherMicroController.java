package com.teleonome.framework.microcontroller;

import java.io.IOException;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.hypothalamus.Hypothalamus;

public abstract class MotherMicroController extends MicroController {

	public MotherMicroController(Hypothalamus h,DenomeManager d, String n) {
		super(h,d, n);
		// TODO Auto-generated constructor stub
	}

	public abstract String getCommandCode() throws IOException ;
	public abstract String getDigitalGeppettoCommandCode() throws IOException ;
	public abstract boolean verifyUserCommandCode(String userCode) throws IOException ;
	public abstract boolean rebootingHypothalamus() throws IOException ;
	public abstract boolean shuttingDownHypothalamus() throws IOException ;
}
