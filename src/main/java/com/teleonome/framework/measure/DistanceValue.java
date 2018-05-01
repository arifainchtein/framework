package com.teleonome.framework.measure;


import javax.measure.unit.Unit; 
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;

import javax.measure.quantity.Quantity;
import javax.measure.quantity.Length;
import org.jscience.physics.amount.Amount;

public class DistanceValue extends ParameterizedValue {

	
	public static Unit<Length> referenceUnit = SI.METER;
	
	
	
	private double value=0;
	
	public DistanceValue(){
		//System.out.println("initizlizing  distance");
	}
	
	public DistanceValue(Unit<Length> u){
		super(u);
		//System.out.println("initizlizing  distance with unit " + u);
	}
	
	public double doubleValue(Unit unit) {
		Amount<Length> l=null;
		try{
				 l = Amount.valueOf(value, getDefaultUnit());
				 //System.out.println("in distacne referenceUnit=" + referenceUnit + " value in unit, l= "  + l.doubleValue(unit));
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
	
	public Unit getReferenceUnit(){
		return referenceUnit;
	}
	


}
