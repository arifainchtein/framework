package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.EnergySystemOfUnits;
import com.teleonome.framework.measure.GardenSystemOfUnits;

public interface NumberOfSeedsQuantity extends Quantity {

	public final static Unit<NumberOfSeedsQuantity> UNIT = GardenSystemOfUnits.NUMBER_OF_SEEDS;
}
