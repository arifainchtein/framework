package com.teleonome.framework.measure;


import javax.measure.*;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit; 
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;


public class MeasureUtils {
	
	/**
	 * This method takes the string representation of a unit and return the
	 *  type of ParameterizedValue it is.  It is useful when a class like Seed
	 *   has an attribute called amount that is of type ParameterizedValue, that means
	 *  that it is not possible to know in advance whether the unit is going to be Weight or Volume
	 * @param s
	 * @return
	 */
	public static Class getValueTypeFromString(String s){
		
			Unit unit=null;
		try{
			 unit = Unit.valueOf(s);
		}catch(Exception e){
			//
			// if we are here is because this is not a normal unit
			// so check for possibles
			return DimensionlessValue.class;
		}
		
		if(		unit.equals(SI.GRAM) ||
				unit.equals(NonSI.OUNCE) ||
				unit.equals(SI.KILOGRAM) ||
				unit.equals(NonSI.POUND)
		){
			return WeightValue.class;
			
		}else if(unit.equals(SI.CENTI(SI.METRE)) ||
				unit.equals(NonSI.INCH) ||
				unit.equals(SI.METRE) ||
				unit.equals(NonSI.FOOT)
		){
			return DistanceValue.class;
		}else if(unit.equals(GardenSystemOfUnits.NUMBER_OF_SEEDS)){
			return DimensionlessValue.class;
			
		}else if(unit.equals(MoneySystemOfUnits.Money)){
			return MoneyValue.class;
			
		}else if(unit.equals(SI.CUBIC_METRE) ||
				unit.equals(NonSI.GALLON_LIQUID_US) ||
				unit.equals(NonSI.LITER) 
		){
			return VolumeValue.class;
		}
		return null;
	}

}
