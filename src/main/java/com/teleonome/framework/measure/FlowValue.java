package com.teleonome.framework.measure;


import javax.measure.unit.Unit; 
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;

import javax.measure.quantity.Quantity;
import javax.measure.quantity.VolumetricFlowRate;
import org.jscience.physics.amount.Amount;

public class FlowValue extends ParameterizedValue {

	
	public static Unit< ? extends Quantity> referenceUnit = SI.CUBIC_METRE.divide(SI.SECOND);
	
	private double value=0;
	
	public FlowValue(){
		//System.out.println("initizlizing  distance");
	}
	
	public FlowValue(Unit<VolumetricFlowRate> u){
		super(u);
	}
	
	public double doubleValue(Unit unit) {
		
		return Amount.valueOf(value, getDefaultUnit()).doubleValue(unit);
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

	public void setValue(double f) {
		this.value = f;
	}
	
	public Unit getDefaultUnit(){
		return defaultUnit;
	}

}
