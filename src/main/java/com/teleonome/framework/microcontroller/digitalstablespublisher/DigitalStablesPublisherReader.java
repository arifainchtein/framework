package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.io.BufferedReader;
import java.io.Reader;

// Answers with the result of the last command written to DigitalStablesPublisherWriter, like SFTPPublisherReader.
public class DigitalStablesPublisherReader extends BufferedReader {

	private final DigitalStablesPublisherWriter writer;

	public DigitalStablesPublisherReader(Reader in, DigitalStablesPublisherWriter w) {
		super(in);
		writer = w;
	}

	@Override
	public String readLine() {
		return writer.getPublishingResults();
	}

	// The Hypothalamus closes the reader after every command; nothing to close here.
	@Override
	public void close() {
	}
}
