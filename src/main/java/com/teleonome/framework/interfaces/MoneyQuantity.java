package com.teleonome.framework.interfaces;


import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.teleonome.framework.measure.EnergySystemOfUnits;
import com.teleonome.framework.measure.MoneySystemOfUnits;

public interface MoneyQuantity extends Quantity {

	public final static Unit<MoneyQuantity> UNIT = MoneySystemOfUnits.Money;
}
