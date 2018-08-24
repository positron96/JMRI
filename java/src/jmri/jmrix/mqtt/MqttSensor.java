package jmri.jmrix.mqtt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jmri.implementation.AbstractSensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extend jmri.AbstractSensor for DCC++ layouts.
 *
 * @author Paul Bender Copyright (C) 2003-2010
 * @author Mark Underwood Copyright (C) 2015
 *
 * Based on XNetSensor
 */
public class MqttSensor extends AbstractSensor implements MqttEventListener {

    private String devAddr;
    private int channel;

    //private int nibble;      /* Is this sensor in the upper or lower 
    //nibble for the feedback encoder */

    
    private String listenTopic, cmdTopic;

    protected MqttAdapter mqttAdapter = null;
    
    public MqttSensor(MqttAdapter controller, String systemName) {
        super(systemName);
        mqttAdapter = controller;
        init();
    }

    
    /**
     * Common initialization for both constructors
     */
    private void init() {
        // store address
        String id = super.getSystemName();
        //prefix = jmri.InstanceManager.getDefault(jmri.jmrix.dccpp.DCCppSensorManager.class).getSystemPrefix();
        String regex = "\\D*(\\d+)_(\\d+)";
        Matcher m = Pattern.compile(regex).matcher(id);
        if (m.matches()) {
            devAddr = m.group(1);
            channel = Integer.parseInt(m.group(2));
        }

        log.debug("New sensor id {} parsed into address {} channel {}", id, devAddr, channel);
        
        listenTopic = devAddr+"/"+channel;
        cmdTopic = listenTopic+"/request";
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
        super.dispose();
    }

    private final static Logger log = LoggerFactory.getLogger(MqttSensor.class);

}
