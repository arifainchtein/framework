package com.teleonome.framework.tools;

import java.net.InetAddress;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
* Pubsub envelope publisher
*/

public class Psenvpub {

    public static void main (String[] args) throws Exception {
    	Context context=null;
    	 Socket publisher=null;
    	try {
        // Prepare our context and publisher
         context = ZMQ.context(1);
         publisher = context.socket(ZMQ.PUB);

        publisher.bind("tcp://*:5563");
        while (!Thread.currentThread ().isInterrupted ()) {
            // Write two messages, each with an envelope and content
            publisher.sendMore ("A");
            publisher.send ("We don't want to see this");
            publisher.sendMore ("B");
            publisher.send(InetAddress.getLocalHost().getCanonicalHostName()+ ":We would like to see this");
        }
    	}catch(Exception e) {
    		
    	}
        publisher.close ();
        context.term ();
        context.close();
    }
}