package jmri.jmrix.mqtt;

import jmri.Turnout;
import jmri.implementation.AbstractTurnout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT implementation of the Turnout interface.
 * <p>
 * Description: extend jmri.AbstractTurnout for MQTT layouts.
 * 
 * <p>
 * This class sends either "CLOSED"/"THROWN" or "0"/"1" messages over MQTT.
 * This depends on provided address (in system name).
 * If address contains "PAIR", the turnout is assumed to use two outputs and 
 * messages "THROWN/CLOSED" are sent to decoder, letting the decoder to control 
 * the outputs. Otherwise, the turnout is assumed to use single output 
 * of the decoder and values "0"/"1" are sent. This mode is useful for operating
 * signal heads and masts.
 * 
 * <p>
 * Examples of addresses that can be entered when creating a turnout: 
 * <ul>
 * <li>"123" - means that this address only operates one output.
 * <li>"123/1" - means 1st output of decoder 123.
 * <li>"123/PAIR/2" - means second pair of decoder 123.
 * </ul>
 * 
 * 
 * It's up to decoder to interpret topic names as it pleases. It can listen to 
 * topics "123", "124", "125", "126" to operate 4 outputs, or it can listen to topics
 * "123/0", "123/1" etc to operate outputs.
 * 
 * <p>
 * Topics are generated from addresses by prepending them with system prefix 
 * (from {@link MqttAdapterTest}} that defaults to "rail" and accessory decoder 
 * prefix that is "acc". So for address 123/PAIR/0, the MQTT topic will be
 * "rail/acc/123/PAIR/0". Note the lack of leading slash.
 * 
 *
 * @author Lionel Jeanson Copyright: Copyright (c) 2017
 * @author positron, 2018
 */
public class MqttTurnout extends AbstractTurnout implements MqttEventListener {

    private final MqttAdapter mqttAdapter;
    private final String topic;
    

    private static final String PAIR_CLOSED = "CLOSED";
    private static final String PAIR_THROWN = "THROWN";
    private final String closedText;
    private final String thrownText;
    private final boolean isPair;

    MqttTurnout(MqttAdapter ma, String systemName, String hwAddr) {
        super(systemName);
        mqttAdapter = ma;
        topic = "acc/" + hwAddr.toUpperCase();
        isPair = hwAddr.toUpperCase().contains("PAIR");
        if (isPair) {
            closedText = PAIR_CLOSED;
            thrownText = PAIR_THROWN;
        } else {
            closedText = "0";
            thrownText = "1";
        }
        //mqttAdapter.subscribe(topic, this);        
    }

    // Turnouts do support inversion
    @Override
    public boolean canInvert() {
        return true;
    }

    // Handle a request to change state by sending a formatted DCC packet
    @Override
    protected void forwardCommandChangeToLayout(int s) {
        
        if ((s & Turnout.CLOSED) != 0 && (s & Turnout.THROWN) != 0) {
            // first look for the double case, which we can't handle
            // this is the disaster case!
            LOG.error("Cannot command both CLOSED and THROWN " + s);
            return;
        }
        if ((s & Turnout.CLOSED) != 0) {
            // send a CLOSED command
            sendMessage(_inverted ? thrownText : closedText);
        } 
        if( (s & Turnout.THROWN) != 0) {
            // send a THROWN command
            sendMessage(_inverted ? closedText : thrownText);
        }
    }

    @Override
    protected void turnoutPushbuttonLockout(boolean _pushButtonLockout) {
        LOG.debug("Send command to {} to topic {}" , _pushButtonLockout ? "Lock" : "Unlock", topic );

    }

    private void sendMessage(String c) {
        mqttAdapter.publish(topic, c.getBytes());
    }

    @Override
    public void notifyMqttMessage(String topic, String message) {
        if (!this.topic.endsWith(topic)) {
            LOG.error("Got a message whose topic (" + topic + ") wasn't for me (" + topic + ")");
            return;
        }
        switch (message) {
            case "0":
            case "OFF":
            case PAIR_CLOSED:                
                newKnownState(CLOSED);
                break;
            case "1":
            case "ON":
            case PAIR_THROWN:
                newKnownState(THROWN);
                break;
            default:
                LOG.warn("Unknow state : " + message + " (topic : " + topic + ")");
                break;
        }
    }

    private final static Logger LOG = LoggerFactory.getLogger(MqttTurnout.class);
}
