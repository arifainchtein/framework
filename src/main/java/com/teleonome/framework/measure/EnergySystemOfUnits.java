package com.teleonome.framework.measure;


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

public final class EnergySystemOfUnits extends SystemOfUnits{

private EnergySystemOfUnits(){
}

	private static HashSet<Unit<?>> INSTANCES = new HashSet<Unit<?>>();
	private static final EnergySystemOfUnits INSTANCE = new EnergySystemOfUnits();
	
	/**
     * Energy Density is a measure of energy per unit of volume 
     */
    public static final BaseUnit<com.teleonome.framework.interfaces.EnergyDensityQuantity> JOULE_PER_CUBIC_METRE = new BaseUnit<com.teleonome.framework.interfaces.EnergyDensityQuantity>("J/m3");
    public static final BaseUnit<com.teleonome.framework.interfaces.SpecificEnergyQuantity> JOULE_PER_KILOGRAM = new BaseUnit<com.teleonome.framework.interfaces.SpecificEnergyQuantity>("J/Kg");
    //1 MJ � 0.28 kWh
    public static final Unit<com.teleonome.framework.interfaces.EnergyDensityQuantity> kWh = JOULE_PER_CUBIC_METRE.times(4.184);
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

	public static EnergySystemOfUnits getInstance(){
		return INSTANCE;
	}
	@Override
	public Set<Unit<?>> getUnits() {
		// TODO Auto-generated method stub
		return null;
	}


}