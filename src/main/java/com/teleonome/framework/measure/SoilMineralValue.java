package com.teleonome.framework.measure;


import javax.measure.unit.Unit; 
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.measure.unit.SystemOfUnits;

import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Area;
import org.jscience.physics.amount.Amount;

import com.teleonome.framework.interfaces.SoilMineralQuantity;

public class SoilMineralValue extends ParameterizedValue {

	
	public static Unit< SoilMineralQuantity> referenceUnit = GardenSystemOfUnits.MILLIGRAM_PER_LITER;
	
	private double value=0;
	
	public SoilMineralValue(){
		//System.out.println("initizlizing  distance");
	}
	
	public SoilMineralValue(Unit<SoilMineralQuantity> u){
		super(u);
	}
	
	public double doubleValue(Unit unit) {
		
		Amount<Area> l=null;
		try{
				 l = Amount.valueOf(value, getDefaultUnit());
				 //System.out.println("doubleValue of area value "  + getDefaultUnit());
		}catch(Exception e){
			System.out.println("the exception is "  + e);
		}
		double d = l.doubleValue(unit);
		return d;
	}

	public long longValue(Unit unit) throws ArithmeticException {
		
		return 0;
	}

	public int compareTo(Object o) {
		
		return 0;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double distance) {
		this.value = distance;
	}
	
	public Unit getDefaultUnit(){
		return referenceUnit;
	}

}
