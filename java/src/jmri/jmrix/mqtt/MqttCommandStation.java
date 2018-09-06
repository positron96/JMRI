package jmri.jmrix.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Current implementation does nothing and is not used.
 * <p>
 * Based on {@link jmri.jmrix.dccpp.DCCppCommandStation}
 */
public class MqttCommandStation implements jmri.CommandStation {

    private MqttSystemConnectionMemo adaptermemo;

    /**
     * ctor
     */
    public MqttCommandStation() {
        super();
    }

    /**
     * Send a specific packet to the rails.
     *
     * @param packet  Byte array representing the packet, including the
     *                error-correction byte. Must not be null.
     * @param repeats Number of times to repeat the transmission.
     */
    @Override
    public boolean sendPacket(byte[] packet, int repeats) {
        return true;
    }

    public void setSystemConnectionMemo(MqttSystemConnectionMemo memo) {
        adaptermemo = memo;
    }

    @Override
    public String getUserName() {
        if (adaptermemo == null) {
            return "MQTT";
        }
        return adaptermemo.getUserName();
    }

    @Override
    public String getSystemPrefix() {
        if (adaptermemo == null) {
            log.warn("MqttCommandStation.getSystemPrefix called while systemmemo was not set. Returning stub");
            return "MQTT";
            
        }
        return adaptermemo.getSystemPrefix();
    }

    private final static Logger log = LoggerFactory.getLogger(MqttCommandStation.class);

}
