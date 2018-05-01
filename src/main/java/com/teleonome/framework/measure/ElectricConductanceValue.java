package com.teleonome.framework.measure;


import javax.measure.Measurable;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;

import javax.measure.quantity.Quantity;
import javax.measure.quantity.Volume;
import javax.measure.unit.BaseUnit;
import org.jscience.physics.amount.Amount;

public class ElectricConductanceValue extends ParameterizedValue {
	
	public Unit< ? extends Quantity> referenceUnit = SI.MILLI(SI.SIEMENS);
	private double value=0;
	
	
	public ElectricConductanceValue(Unit<javax.measure.quantity.ElectricConductance> u){
		super(u);
	}
	
	public ElectricConductanceValue(){
	}
	
	
	
	
	public double doubleValue(Unit unit) {
		// TODO Auto-generated method stub
		return Amount.valueOf(value, getDefaultUnit()).doubleValue(unit);
	}

	public long longValue(Unit unit) throws ArithmeticException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double distance) {
		this.value = distance;
	}
	
	public Unit getDefaultUnit(){
		return defaultUnit;
	}
	
}
