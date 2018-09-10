package jmri.jmrix.mqtt;

import jmri.jmrix.mqtt.networkdriver.MqttAdapter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jmri.implementation.AbstractSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extend jmri.AbstractSensor for MQTT protocol.
 * 
 * <p>
 * Subscribes to topic 
 *
 * @author Paul Bender Copyright (C) 2003-2010
 * @author Mark Underwood Copyright (C) 2015
 * @author positron, 2018
 *
 * Based on XNetSensor
 */
public class MqttSensor extends AbstractSensor implements MqttEventListener {
    
    private final String listenTopic;    
    private final String cmdTopic;

    protected MqttAdapter mqttAdapter = null;
    
    public MqttSensor(MqttAdapter controller, String systemName, String hwAddr) {
        super(systemName);
        mqttAdapter = controller;
        
        listenTopic = "acc/"+hwAddr;
        cmdTopic = listenTopic+"/get";
        mqttAdapter.subscribe(listenTopic, this);
        
    }

    
    /** {@inheritDoc }  */
    @Override
    public void requestUpdateFromLayout() {
        mqttAdapter.publish(cmdTopic, new byte[]{} );
    }
    
    @Override
    public void notifyMqttMessage(String topic, String message) {
        if (!topic.endsWith(listenTopic)) {
            log.error("Got a message whose topic (" + topic + ") wasn't for me (" + listenTopic + ")");
            return;
        }
        switch (message) {
            case "ON":
            case "1":
                setOwnState(ACTIVE);
                break;
            case "OFF":
            case "0":
                setOwnState(INACTIVE);
                break;
            default:
                log.warn("Unknow state : " + message + " (topic : " + topic + ")");
                break;
        }
    }

    @Override
    public void dispose() {
        mqttAdapter.unsubscribe(listenTopic, this);
        super.dispose();
    }

    private final static Logger log = LoggerFactory.getLogger(MqttSensor.class);

}
