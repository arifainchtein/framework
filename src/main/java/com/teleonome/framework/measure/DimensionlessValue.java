package com.teleonome.framework.measure;


import javax.measure.Measurable;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;

import javax.measure.quantity.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.BaseUnit;
import org.jscience.physics.amount.Amount;

public class DimensionlessValue extends ParameterizedValue {

	public Unit< ? extends Quantity> referenceUnit = Dimensionless.UNIT;
	private double value=0;
	
	
	public DimensionlessValue(Unit<Dimensionless> u){
		super(u);
	}
	
	public DimensionlessValue(){
	}
	
	
	
	
	public double doubleValue(Unit unit) {
		return value;
	}

	public long longValue(Unit unit) throws ArithmeticException {
		// TODO Auto-generated method stub
		return (long)value;
	}

	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double d) {
		this.value = d;
	}
	
	public Unit getDefaultUnit(){
		return defaultUnit;
	}
	
}
