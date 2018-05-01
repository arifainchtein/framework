package com.teleonome.framework.measure;


import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Volume;
import javax.measure.quantity.Energy;
import javax.measure.unit.BaseUnit;
import javax.measure.unit.SI;
import javax.measure.unit.SystemOfUnits;
import javax.measure.unit.Unit;
import javax.measure.unit.SI;
import javax.measure.unit.SI.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class GardenSystemOfUnits extends SystemOfUnits{

private GardenSystemOfUnits(){
}

	private static HashSet<Unit<?>> INSTANCES = new HashSet<Unit<?>>();
	private static final GardenSystemOfUnits INSTANCE = new GardenSystemOfUnits();
	
	/**
     * NUMBER_OF_SEEDS is a measure of seeds
     */
	public static final BaseUnit<com.teleonome.framework.interfaces.NumberOfSeedsQuantity> NUMBER_OF_SEEDS = new BaseUnit<com.teleonome.framework.interfaces.NumberOfSeedsQuantity>("Seeds");
    
	public static final BaseUnit<com.teleonome.framework.interfaces.LimeRequirementQuantity> GRAM_PER_SQUARED_METRE = new BaseUnit<com.teleonome.framework.interfaces.LimeRequirementQuantity>("g/m2");
	public static final BaseUnit<com.teleonome.framework.interfaces.SoilMineralQuantity> MILLIGRAM_PER_LITER = new BaseUnit<com.teleonome.framework.interfaces.SoilMineralQuantity>("mg/l");
    
    public static final Unit<com.teleonome.framework.interfaces.LimeRequirementQuantity> TONNE_PER_ACRE = GRAM_PER_SQUARED_METRE.times(4.184);
	
   
    	/////////////////////
	// Collection View //
	/////////////////////

	/**
	 * Returns a read only view over the SI units defined in this class.
	 *
	 * @return the collection of SI units.
	 */
	public static Set<Unit<?>> units() {
		return Collections.unmodifiableSet(INSTANCES);
	}

	/**
	 * Adds a new unit to the collection.
	 *
	 * @param  unit the unit being added.
	 * @return <code>unit</code>.
	 */
	private static <U extends Unit> U nonSI(U unit) {
		INSTANCES.add(unit);
		return unit;
	}

	public static GardenSystemOfUnits getInstance(){
		return INSTANCE;
	}
	@Override
	public Set<Unit<?>> getUnits() {
		// TODO Auto-generated method stub
		return null;
	}


}