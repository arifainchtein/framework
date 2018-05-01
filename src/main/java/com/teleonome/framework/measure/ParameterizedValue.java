package com.teleonome.framework.measure;


import javax.measure.Measurable;
import javax.measure.unit.Unit;



public abstract class ParameterizedValue implements Measurable {
	
	protected Unit<?> defaultUnit = null;
	
	
	public abstract double getValue() ;
	public abstract void setValue(double d);
	
	public ParameterizedValue(){
		//System.out.println("initizlizing  parametrized");
	}
	
	public ParameterizedValue(Unit u){
		defaultUnit=u;
	}
	public Unit getDefaultUnit(){
		return defaultUnit;
	}
	
	public void setDefaultUnit(Unit u){
		defaultUnit=u;
	}
	
	public String format(){
		return getValue() + " " + defaultUnit.toString();
	}
}
