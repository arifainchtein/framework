package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.EnergySystemOfUnits;

public interface EnergyDensityQuantity extends Quantity {

	public final static Unit<EnergyDensityQuantity> UNIT = EnergySystemOfUnits.JOULE_PER_CUBIC_METRE;
}
