package com.teleonome.framework.zhinupublisher;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import com.teleonome.framework.utils.Utils;

public class ZhinuPublisher implements MqttCallback{

	static final String BROKER_URL = "tcp://chilhuacle.info:1883";
	static final String clientID = "Ra";
	static final String userName = "chilhuacle";
	static final String password = "9cc67c92a1eb1687cb8c5d97b64e1a866c69a21110bc8531ed40f278dc3f3e11";
	MqttClient myClient;
	MqttConnectOptions connOpt;
	Logger logger;
	static final Boolean subscriber = false;
	static final Boolean publisher = true;
	public ZhinuPublisher() {
		logger = Logger.getLogger(getClass());
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		//connOpt.setUserName(userName);
		//connOpt.setPassword(password.toCharArray());
		
		// Connect to Broker
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			logger.warn(Utils.getStringException(e));
			//System.exit(-1);
		}
		
		logger.debug("Connected to " + BROKER_URL);
	}
	/**
	 * 
	 * connectionLost
	 * This callback is invoked upon losing the MQTT connection.
	 * 
	 */
	@Override
	public void connectionLost(Throwable t) {
		logger.debug("Connection lost!");
		// code to reconnect to the broker would go here if desired
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
		if(!myClient.isConnected()) {
			try {
				myClient.connect(connOpt);
			} catch (MqttException e) {
				logger.warn(Utils.getStringException(e));
				//System.exit(-1);
			}
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
			    	// Wait until the message has been delivered to the broker
					token.waitForCompletion();
					Thread.sleep(100);
				} catch (Exception e) {
					logger.warn(Utils.getStringException(e));
				}
			}			
		
		// disconnect
		try {
			// wait to ensure subscribed messages are delivered
			if (subscriber) {
				Thread.sleep(5000);
			}
			myClient.disconnect();
		} catch (Exception e) {
			logger.warn(Utils.getStringException(e));
		}
	}
	
}
