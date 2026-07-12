package com.teleonome.framework.zhinupublisher;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import com.teleonome.framework.utils.Utils;

public class ZhinuPublisher implements MqttCallbackExtended{

	static final String BROKER_URL = "tcp://chilhuacle.info:1883";
	static final String clientID = "Ra";
	static final String userName = "chilhuacle";
	static final String password = "9cc67c92a1eb1687cb8c5d97b64e1a866c69a21110bc8531ed40f278dc3f3e11";
	MqttClient myClient;
	MqttConnectOptions connOpt;
	Logger logger;
	static final Boolean subscriber = false;
	static final Boolean publisher = true;
	//
	// publish() used to connect + publish + disconnect synchronously on the
	// caller's thread (often Moquette's own message-processing thread), so a
	// slow/unreachable chilhuacle.info stalled the whole broker. Run it here
	// instead, on a background thread, same fix pattern as the constructor's
	// connect.
	//
	// This is a best-effort backup of farm data to a remote server: it must
	// never block the caller, and if chilhuacle.info is down it must fail
	// gracefully rather than pile up work. A plain newSingleThreadExecutor()
	// has an unbounded queue, so a prolonged outage would queue farm data in
	// memory forever waiting for a connection that may not come back for a
	// long time. Bound the queue and drop the oldest queued publish once full
	// -- keeping the most recent data is more useful than an ever-growing
	// backlog of stale data anyway.
	//
	private static final int PUBLISH_QUEUE_CAPACITY = 20;
	private static final long PUBLISH_TIMEOUT_MILLIS = 5000;
	private final ExecutorService publishExecutor = new ThreadPoolExecutor(
			1, 1, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<>(PUBLISH_QUEUE_CAPACITY),
			r -> {
				Thread t = new Thread(r, "ZhinuPublisher-publish");
				t.setDaemon(true);
				return t;
			},
			new ThreadPoolExecutor.DiscardOldestPolicy()
	);
	public ZhinuPublisher() {
		logger = Logger.getLogger(getClass());
		connOpt = new MqttConnectOptions();

		connOpt.setCleanSession(false);
		connOpt.setKeepAliveInterval(30);
		connOpt.setConnectionTimeout(10);
		connOpt.setAutomaticReconnect(true);
		//connOpt.setUserName(userName);
		//connOpt.setPassword(password.toCharArray());

		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
		} catch (MqttException e) {
			logger.warn(Utils.getStringException(e));
			return;
		}

		//
		// connect in the background: chilhuacle.info is an external broker over
		// the internet, and a stalled/unreachable connect() here must never block
		// the caller (Heart startup runs synchronously through this constructor)
		//
		Thread connectThread = new Thread(() -> {
			try {
				myClient.connect(connOpt);
				logger.debug("Connected to " + BROKER_URL);
			} catch (MqttException e) {
				logger.warn(Utils.getStringException(e));
			}
		}, "ZhinuPublisher-connect");
		connectThread.setDaemon(true);
		connectThread.start();
	}
	/**
	 * 
	 * connectionLost
	 * This callback is invoked upon losing the MQTT connection.
	 * 
	 */
	@Override
	public void connectionLost(Throwable t) {
		logger.warn("Connection to " + BROKER_URL + " lost: " + t.getMessage());
		// connOpt.setAutomaticReconnect(true) handles reconnection itself, with
		// Paho's own backoff -- connectComplete() below logs when it succeeds.
	}

	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		if (reconnect) {
			logger.warn("Reconnected to " + serverURI);
		} else {
			logger.debug("Connected to " + serverURI);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		logger.debug("-------------------------------------------------");
		logger.debug("| Topic:" + topic);
		logger.debug("| Message: " + new String(message.getPayload()));
		logger.debug("-------------------------------------------------");
		
	}
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	public void publish(String topicName,String messageText) {
		if(myClient==null) {
			logger.warn("ZhinuPublisher has no client, skipping publish of topic " + topicName);
			return;
		}
		publishExecutor.submit(() -> doPublish(topicName, messageText));
	}

	private void doPublish(String topicName, String messageText) {
		//
		// connOpt has setAutomaticReconnect(true), so Paho is already retrying
		// in the background with its own backoff. Calling connect() here too
		// (as this used to) raced with it -- both open a CONNECT with the same
		// client ID at once, the broker kills whichever loses, and every
		// publish() call kept re-triggering that with no backoff of its own
		// (the same anti-pattern the SFTP publisher had). Just skip this
		// publish and let automatic reconnect finish the job.
		//
		if(!myClient.isConnected()) {
			logger.warn("Not connected to " + BROKER_URL + ", skipping publish of topic " + topicName + ", automatic reconnect will re-establish the session");
			return;
		}

		// setup topic
		// topics on m2m.io are in the form <domain>/<stuff>/<thing>
		MqttTopic topic = myClient.getTopic(topicName);

		// subscribe to topic if subscriber
		if (subscriber) {
			try {
				int subQoS = 0;
				myClient.subscribe(topicName, subQoS);
			} catch (Exception e) {
				logger.warn(Utils.getStringException(e));
			}
		}

		// publish messages if publisher
		if (publisher) {

		   		int pubQoS = 0;
				MqttMessage message = new MqttMessage(messageText.getBytes());
		    	message.setQos(pubQoS);
		    	message.setRetained(false);

		    	// Publish the message
		    	logger.debug("Publishing to topic \"" + topic + "\" qos " + pubQoS);
		    	MqttDeliveryToken token = null;
		    	try {
		    		// publish message to broker
					token = topic.publish(message);
			    	//
			    	// waitForCompletion() with no timeout blocks forever if the
			    	// connection looks up but is actually degraded (half-open TCP,
			    	// no acks coming back) -- that would stall this dedicated
			    	// publish thread indefinitely instead of failing gracefully.
			    	//
					token.waitForCompletion(PUBLISH_TIMEOUT_MILLIS);
				} catch (Exception e) {
					logger.warn(Utils.getStringException(e));
				}
			}
		//
		// connection is kept open (not disconnected) so subsequent publishes
		// don't pay the connect cost again; connectionLost()/isConnected()
		// above handle reconnecting if chilhuacle.info drops the link.
	}

}
