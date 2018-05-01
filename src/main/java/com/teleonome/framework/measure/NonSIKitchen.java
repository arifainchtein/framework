package com.teleonome.framework.measure;


import javax.measure.quantity.Volume;
import javax.measure.quantity.Energy;
import javax.measure.unit.Unit;
import javax.measure.unit.SI.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public final class NonSIKitchen{

private NonSIKitchen(){
}

	private static HashSet<Unit<?>> INSTANCES = new HashSet<Unit<?>>();

	/**
     * A unit of volume equal to 125 ml
     */
    public static final Unit<Volume> CUP = nonSI(javax.measure.unit.SI.CUBIC_METRE.divide(4000));
    public static final Unit<Volume> TABLESPOON = nonSI(CUP.divide(16));
    public static final Unit<Volume> TEASPOON = nonSI(TABLESPOON.divide(3));
    public static final Unit<Energy> KILOCALORIE = nonSI(javax.measure.unit.SI.JOULE.times(4.184));

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