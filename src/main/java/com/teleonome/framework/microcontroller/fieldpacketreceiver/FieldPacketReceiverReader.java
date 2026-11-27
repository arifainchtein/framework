package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.io.BufferedReader;
import java.io.Reader;

// Same as SFTPPublisherReader: the answer to the last command is whatever the writer left.
public class FieldPacketReceiverReader extends BufferedReader {

	FieldPacketReceiverWriter aWriter;

	public FieldPacketReceiverReader(Reader in, FieldPacketReceiverWriter w) {
		super(in);
		aWriter = w;
	}

	public String readLine() {
		return aWriter.getIngestionResults();
	}

	public boolean ready() {
		return true;
	}
}
