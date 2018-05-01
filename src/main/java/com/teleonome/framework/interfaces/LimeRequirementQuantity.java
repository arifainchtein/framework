package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.GardenSystemOfUnits;

public interface LimeRequirementQuantity extends Quantity {

	public final static Unit<LimeRequirementQuantity> UNIT = GardenSystemOfUnits.GRAM_PER_SQUARED_METRE;
}
