package com.teleonome.framework.microcontroller.internetaudiomicrocontroller;

import org.apache.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.teleonome.framework.utils.Utils;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Gianlu
 */
public class NetworkThread extends Thread {
    private final Client client;
    
    Logger logger;
    NetworkThread( URI uri) {
    	logger = Logger.getLogger(getClass());
        client = new Client(uri);
       start();
    }

    private static void addParams( JSONObject obj,  String params) {
        if (params == null) return;

        if (params.isEmpty()) {
            obj.put("params", "");
        } else if ((params.startsWith("{") && params.endsWith("}")) || (params.startsWith("[") && params.endsWith("]"))) {
            obj.put("params", new JSONTokener(params));
        } else {
            obj.put("params", params);
        }
    }

    void close() {
        client.close();
    }

    @Override
    public void run() {
        try {
            client.connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void sendGeneral( String id,  String method,  String params) {
        JSONObject obj = new JSONObject();
        obj.put("jsonrpc", "2.0");
        obj.put("id", id);
        obj.put("method", method);
        addParams(obj, params);

        client.send(obj.toString());
        
    }

    void sendPlayer( String suffix) {
        sendGeneral(String.valueOf(ThreadLocalRandom.current().nextInt(1000)), "player." + suffix, null);
    }

    void sendMercury( String method,  String uri,  String contentType,  Map<String, String> headers,  Callback listener) {
        String id = String.valueOf(ThreadLocalRandom.current().nextInt(1000));

        JSONObject obj = new JSONObject();
        obj.put("jsonrpc", "2.0");
        obj.put("id", id);
        obj.put("method", "mercury.request");

        JSONObject params = new JSONObject();
        obj.put("params", params);

        params.put("method", method);
        params.put("uri", uri);
        if (contentType != null && !contentType.isEmpty())
            params.put("contentType", contentType);

        if (!headers.isEmpty()) {
            JSONArray array = new JSONArray(headers.size());
            params.put("headers", array);
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                JSONObject e = new JSONObject();
                e.put("key", entry.getKey());
                e.put("value", entry.getValue());
                array.put(e);
            }
        }

        client.send(obj.toString());
        
    }

    public interface Callback {
        void response( JSONObject json);
    }

    public interface Listener {
        void connected();

        void error( Throwable ex);

        void closed();

        void unknownResponse( JSONObject obj);
    }

    private class Client extends WebSocketClient {
        Client( URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
           // Platform.runLater(listener::connected);
        	logger.debug("onOpen status=" + serverHandshake.getHttpStatus());
        }

        @Override
        public void onMessage(String s) {
            JSONObject obj = new JSONObject(new JSONTokener(s));
            String id = obj.getString("id");
            logger.debug("onMessage status=" + obj.toString(4));
        }

        @Override
        public void onClose(int i, String s, boolean b) {
        	logger.debug("onClose s=" + s);
        }

        @Override
        public void onError(Exception ex) {
        	logger.debug("onError s=" + Utils.getStringException(ex));
        }
    }
}

