package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.GardenSystemOfUnits;

public interface SoilMineralQuantity extends Quantity {

	public final static Unit<SoilMineralQuantity> UNIT = GardenSystemOfUnits.MILLIGRAM_PER_LITER;
}
