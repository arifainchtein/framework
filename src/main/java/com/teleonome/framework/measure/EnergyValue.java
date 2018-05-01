package com.teleonome.framework.measure;


import javax.measure.Measurable;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.quantity.Quantity;
import javax.measure.quantity.Energy;
import javax.measure.unit.BaseUnit;
import org.jscience.physics.amount.Amount;

public class EnergyValue extends ParameterizedValue {

	public static Unit< ? extends Quantity> referenceUnit = SI.JOULE;
	

	public EnergyValue(){
	}
	
	public EnergyValue(Unit<Energy> u){
		super(u);
	}
	private double value=0;
	
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
		return referenceUnit;
	}

}
