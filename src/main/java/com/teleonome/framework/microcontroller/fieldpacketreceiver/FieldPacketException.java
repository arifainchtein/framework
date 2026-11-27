package com.teleonome.framework.microcontroller.fieldpacketreceiver;

// A field packet that cannot be accepted or processed; the message says why and is kept with the rejected packet.
public class FieldPacketException extends Exception {

	private static final long serialVersionUID = 1L;

	public FieldPacketException(String message) {
		super(message);
	}
}
