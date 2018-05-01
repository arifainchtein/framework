package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.EnergySystemOfUnits;

public interface SpecificEnergyQuantity extends Quantity {

	public final static Unit<SpecificEnergyQuantity> UNIT = null;//EnergySystemOfUnits.JOULE_PER_CUBIC_METRE;
}
