package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.IOException;

import org.json.JSONObject;

/**
 * Turns one kind of field packet into a record. One subclass per "Packet Type", named <Packet Type>PacketProcessor
 * in this package (SoilVisit -> SoilVisitPacketProcessor) and found by reflection, the same way
 * AnnabelleDeserializerFactory finds a deserializer. A new kind of packet = one new class + its type added to the
 * receiver's "Accepted Packet Types"; nothing else changes.
 *
 * Runs in the Async Cycle (FieldPacketReceiverWriter), after FieldPacket.check() has passed.
 */
public abstract class FieldPacketProcessor {

	/**
	 * Processes the packet and returns the record it went into, for the denome's pointer dene:
	 *   {"Record Id", "Record Type", "Record Name", "Record File"}
	 * Throws FieldPacketException when the packet's content is wrong; it is then rejected with that reason.
	 */
	public abstract JSONObject process(FieldPacket packet, FieldPacketSettings settings) throws FieldPacketException, IOException;

	public static FieldPacketProcessor forType(String packetType) throws FieldPacketException {
		String className = FieldPacketProcessor.class.getPackage().getName() + "." + packetType + "PacketProcessor";
		try {
			return (FieldPacketProcessor) Class.forName(className).getConstructor().newInstance();
		} catch (ClassNotFoundException e) {
			throw new FieldPacketException("no processor for " + packetType + " packets (" + className + ")");
		} catch (Exception e) {
			throw new FieldPacketException("cannot create " + className + ": " + e);
		}
	}
}
