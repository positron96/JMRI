package jmri.jmrix.mqtt;

import jmri.Turnout;
import jmri.implementation.AbstractTurnout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT implementation of the Turnout interface.
 * <p>
 * Description: extend jmri.AbstractTurnout for MQTT layouts
 *
 * @author Lionel Jeanson Copyright: Copyright (c) 2017
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
        isPair = hwAddr.toUpperCase().startsWith("PAIR");
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
