package com.teleonome.framework.tools;

import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.exception.InvalidDenomeException;
import com.teleonome.framework.exception.MissingDenomeException;

public class TestDenome {

	public TestDenome() {
		try {
			DenomeManager aDenomeManager = DenomeManager.instance();
			String d = (String)aDenomeManager.getDeneWordAttributeByIdentity(new Identity("@Sento:Purpose:Operational Data:Heart:Virtual Memory Used By Process"), "Value");
			System.out.println(d);
		} catch (MissingDenomeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidDenomeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestDenome();
	}

}
