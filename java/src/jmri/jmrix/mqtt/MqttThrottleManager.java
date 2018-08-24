package jmri.jmrix.mqtt;

import java.util.HashMap;
import jmri.LocoAddress;
import jmri.ThrottleManager;
import jmri.jmrix.AbstractThrottleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DCC++ implementation of a ThrottleManager based on the
 * AbstractThrottleManager.
 *
 * @author Paul Bender Copyright (C) 2002-2004
 * @author Mark Underwood Copyright (C) 2015
 *
 * Based on XNetThrottleManager by Paul Bender
 */
public class MqttThrottleManager extends AbstractThrottleManager implements ThrottleManager {

    protected HashMap<LocoAddress, MqttThrottle> throttles = new HashMap<>(5);

    //protected MqttSystemConnectionMemo tc = null;
    
    private MqttAdapter mqttAdapter;

    /**
     * Constructor.
     */
    public MqttThrottleManager(MqttSystemConnectionMemo memo, MqttAdapter a) {
        super(memo);
        // connect to the TrafficManager
        
        mqttAdapter = a;
        
        //tc = memo.getDCCppTrafficController();

        // Register to listen for throttle messages
        //tc.addDCCppListener(DCCppInterface.THROTTLE, this);
    }

    /**
     * Request a new throttle object be created for the address, and let the
     * throttle listeners know about it.
     * <p>
     */
    @Override
    public void requestThrottleSetup(LocoAddress address, boolean control) {
        MqttThrottle throttle;
        if (log.isDebugEnabled()) {
            log.debug("Requesting Throttle: " + address);
        }
        if (throttles.containsKey(address)) {
            notifyThrottleKnown(throttles.get(address), address);
        } else {
            /*if (tc.getCommandStation().requestNewRegister(address.getNumber()) == DCCppConstants.NO_REGISTER_FREE) {
                log.error("No Register available for Throttle. Address = {}", address);
                return;
            }*/
            throttle = new MqttThrottle(adapterMemo, address, mqttAdapter);
            throttles.put(address, throttle);
            notifyThrottleKnown(throttle, address);
        }
    }

    /*
     * DCC++ based systems DO NOT use the Dispatch Function
     * (do they?)
     */
    @Override
    public boolean hasDispatchFunction() {
        return false;
    }

    /*
     * DCC++ based systems can have multiple throttles for the same 
     * device
     */
    @Override
    protected boolean singleUse() {
        return false;
    }

    /**
     * Address 128 and above is a long address
     * <p>
     */
    @Override
    public boolean canBeLongAddress(int address) {
        return isLongAddress(address);
    }

    /**
     * Address 127 and below is a short address
     * <p>
     */
    @Override
    public boolean canBeShortAddress(int address) {
        return !isLongAddress(address);
    }

    /**
     * There are no ambiguous addresses on this system.
     */
    @Override
    public boolean addressTypeUnique() {
        return true;
    }

    /*
     * Local method for deciding short/long address
     * (is it?)
     */
    static protected boolean isLongAddress(int num) {
        return (num >= 128);
    }

    /**
     * What speed modes are supported by this system? value should be xor of
     * possible modes specifed by the DccThrottle interface DCC++ supports
     * 14,27,28 and 128 speed step modes
     */
    @Override
    public int supportedSpeedModes() {
        return (jmri.DccThrottle.SpeedStepMode128);
    }



    @Override
    public void releaseThrottle(jmri.DccThrottle t, jmri.ThrottleListener l) {
    }

    @Override
    public boolean disposeThrottle(jmri.DccThrottle t, jmri.ThrottleListener l) {
        if (super.disposeThrottle(t, l)) {
            MqttThrottle lnt = (MqttThrottle) t;
            lnt.throttleDispose();
            return true;
        }
        return false;
    }

    private final static Logger log = LoggerFactory.getLogger(MqttThrottleManager.class);

}
