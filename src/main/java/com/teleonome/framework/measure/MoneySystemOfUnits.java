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

public final class MoneySystemOfUnits extends SystemOfUnits{

private MoneySystemOfUnits(){
}

	private static HashSet<Unit<?>> INSTANCES = new HashSet<Unit<?>>();
	private static final MoneySystemOfUnits INSTANCE = new MoneySystemOfUnits();
	
	/**
     * Energy Density is a measure of energy per unit of volume 
     */
    public static final BaseUnit<com.teleonome.framework.interfaces.MoneyQuantity> Money = new BaseUnit<com.teleonome.framework.interfaces.MoneyQuantity>("$");
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

	public static MoneySystemOfUnits getInstance(){
		return INSTANCE;
	}
	@Override
	public Set<Unit<?>> getUnits() {
		// TODO Auto-generated method stub
		return null;
	}


}