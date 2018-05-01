package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.quantity.Volume;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.EnergySystemOfUnits;

public interface UserDefinedVolumeQuantity extends Quantity {

	public final static Unit<Volume> UNIT = SI.CUBIC_METRE;
}
