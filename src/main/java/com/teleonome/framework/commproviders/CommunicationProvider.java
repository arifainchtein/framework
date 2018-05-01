package com.teleonome.framework.commproviders;

import com.teleonome.framework.exception.CommunicationException;

public abstract class CommunicationProvider {

	public abstract void sendMessage(String message) throws CommunicationException;
}
