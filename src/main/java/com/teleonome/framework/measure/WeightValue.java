package com.teleonome.framework.measure;


import javax.measure.Measurable;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Mass;
import javax.measure.unit.BaseUnit;
import org.jscience.physics.amount.Amount;

public class WeightValue extends ParameterizedValue {

	public Unit< ? extends Quantity> referenceUnit = SI.GRAM;
	
	public WeightValue(Unit<Mass> u){
		super(u);
	}
	public WeightValue(){
		
	}
	
	private double value=0;
	
	public double doubleValue(Unit unit) {
		// TODO Auto-generated method stub
		return Amount.valueOf(value, defaultUnit).doubleValue(unit);
	}

	public long longValue(Unit unit) throws ArithmeticException {
		// TODO Auto-generated method stub
		return Amount.valueOf(value, defaultUnit).longValue(unit);
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
	
	public Unit getReferenceUnit(){
		return referenceUnit;
	}
	
	public Unit getDefaultUnit(){
		return defaultUnit;
	}

}
