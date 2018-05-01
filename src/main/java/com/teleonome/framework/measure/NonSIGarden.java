package com.teleonome.framework.measure;


import javax.measure.quantity.Area;
import javax.measure.quantity.Energy;
import javax.measure.unit.Unit;
import javax.measure.unit.SI.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public final class NonSIGarden{

private NonSIGarden(){
}

	private static HashSet<Unit<?>> INSTANCES = new HashSet<Unit<?>>();

	/**
     * A unit of volume equal to 125 ml
     */
    public static final Unit<Area> SQUARE_FOOT = nonSI(javax.measure.unit.SI.SQUARE_METRE.divide(10));
    
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


}