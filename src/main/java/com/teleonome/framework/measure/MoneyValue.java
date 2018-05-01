package com.teleonome.framework.measure;


import javax.measure.Measurable;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;
import javax.measure.unit.BaseUnit;
import org.jscience.physics.amount.Amount;
import org.jscience.economics.money.Money;
import com.teleonome.framework.interfaces.MoneyQuantity;

public class MoneyValue extends ParameterizedValue {

	public static Unit< ? extends Quantity> referenceUnit = MoneySystemOfUnits.Money;
	

	public MoneyValue(){
	}
	
	public MoneyValue(Unit<MoneyQuantity> u){
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
