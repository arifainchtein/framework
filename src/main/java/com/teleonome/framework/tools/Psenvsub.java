package com.teleonome.framework.tools;


import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
* Pubsub envelope subscriber
*/

public class Psenvsub {

    public static void main (String[] args) {
    	Context context=null;
    	Socket subscriber=null;
    	try {
        // Prepare our context and subscriber
         context = ZMQ.context(1);
         subscriber = context.socket(ZMQ.SUB);

        subscriber.connect("tcp://*:5563");
        subscriber.subscribe("B".getBytes());
        while (!Thread.currentThread ().isInterrupted ()) {
            // Read envelope with address
            String address = subscriber.recvStr ();
            // Read message contents
            String contents = subscriber.recvStr ();
            System.out.println(address + " : " + contents);
        }
    	}catch(Exception e) {
    		
    	}finally {
    		try {
    			subscriber.close ();
    		}catch(Exception e) {
    			
    		}
    		
    		try {
    			context.close ();
    		}catch(Exception e) {
    			
    		}
    	}
        
    }
}