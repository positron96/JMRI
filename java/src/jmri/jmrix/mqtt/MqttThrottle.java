package jmri.jmrix.mqtt;

import java.util.concurrent.LinkedBlockingQueue;
import jmri.DccLocoAddress;
import jmri.DccThrottle;
import jmri.LocoAddress;
import jmri.jmrix.AbstractThrottle;
import jmri.jmrix.SystemConnectionMemo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of DccThrottle with code specific to a DCC++ connection.
 *
 * @author Paul Bender (C) 2002-2010
 * @author Giorgio Terdina (C) 2007
 * @author Mark Underwood (C) 2015
 *
 * Based on XNetThrottle by Paul Bender and Giorgio Terdina
 */
public class MqttThrottle extends AbstractThrottle implements MqttEventListener {

    protected MqttAdapter mqttAdapter = null;

    // status of the throttle
    protected static final int THROTTLEIDLE = 0;  // Idle Throttle
    protected static final int THROTTLESPEEDSENT = 2;  // Sent speed/dir command to locomotive
    protected static final int THROTTLEFUNCSENT = 4;   // Sent a function command to locomotive.

    public int requestState = THROTTLEIDLE;

    protected int address;
    
    private String sendTopicBase;


    public MqttThrottle(SystemConnectionMemo memo, MqttAdapter controller) {
        super(memo);
        mqttAdapter = controller;
        
        if (log.isDebugEnabled()) {
            log.debug("DCCppThrottle constructor");
        }
    }

    /**
     * Constructor
     */
    public MqttThrottle(SystemConnectionMemo memo, LocoAddress address, MqttAdapter controller) {
        super(memo);
        this.mqttAdapter = controller;
        this.setDccAddress(address.getNumber() 
        );
        this.speedIncrement = SPEED_STEP_128_INCREMENT;
        this.speedStepMode = DccThrottle.SpeedStepMode128;

        f0Momentary = f1Momentary = f2Momentary = f3Momentary = f4Momentary
                = f5Momentary = f6Momentary = f7Momentary = f8Momentary = f9Momentary
                = f10Momentary = f11Momentary = f12Momentary = false;

        sendTopicBase = "loco/"+address.getNumber()+"/";
        if (log.isDebugEnabled()) {
            log.debug("DCCppThrottle constructor called for address " + address);
        }
    }
    
    private byte b2b(boolean v) {
        return v==false ? (byte)0 : (byte)1;
    }

    /**
     * Send the DCC++ message to set the state of locomotive direction and
     * functions F0, F1, F2, F3, F4
     */
    @Override
    protected void sendFunctionGroup1() {
        log.debug("sendFunctionGroup1(): f0 {} f1 {} f2 {} f3 {} f4{}",
                f0, f1, f2, f3, f4);

        sendMessage("fg1", new byte[]{ b2b(f0), b2b(f1), b2b(f2), b2b(f3), b2b(f4) } );
    }

    /**
     * Send the DCC++ message to set the state of functions F5, F6, F7, F8
     */
    @Override
    protected void sendFunctionGroup2() {
        
        log.debug("sendFunctionGroup2(): f5 {} f6 {} f7 {} f8",
                f5, f6, f7, f8);

        sendMessage("fg2", new byte[]{ b2b(f5), b2b(f6), b2b(f7), b2b(f8) });
    }

    /**
     * Send the DCC++ message to set the state of functions F9, F10, F11, F12
     */
    @Override
    protected void sendFunctionGroup3() {
        
        log.debug("sendFunctionGroup3(): f9 {} f10 {} f11 {} f12",
                f9, f10, f11, f12);

        sendMessage("fg3", new byte[]{ b2b(f9), b2b(f10), b2b(f11), b2b(f12) } );
    }

    /**
     * Send the DCC++ message to set the state of functions F13, F14, F15, F16,
     * F17, F18, F19, F20
     */
    @Override
    protected void sendFunctionGroup4() {
        
         log.debug("sendFunctionGroup4(): f13 {} f14 {} f15 {} f16",
                f13, f14, f15, f6);

        sendMessage("fg4", new byte[] { b2b(f13), b2b(f14), b2b(f15), b2b(f16), 
            b2b(f17), b2b(f18), b2b(f19), b2b(f20) });
    }

    /**
     * Send the DCC++ message to set the state of functions F21, F22, F23, F24,
     * F25, F26, F27, F28
     */
    @Override
    protected void sendFunctionGroup5() {
        log.debug("sendFunctionGroup5(): f21 {} f22 {} f23 {} f24 {} f25 {} f26 {} f27 {} f28 {}",
                f21, f22, f23, f24, f25, f26, f27, f28);

        sendMessage("fg5", new byte[]{ b2b(f21), b2b(f22), b2b(f23), b2b(f24), 
            b2b(f25), b2b(f26), b2b(f27), b2b(f28) } );
    }

    /**
     * Send the DCC++ message to set the Momentary state of locomotive functions
     * F0, F1, F2, F3, F4
     */
    @Override
    protected void sendMomentaryFunctionGroup1() {
        log.debug("sendMomentaryFunctionGroup1(): f0 {} f1 {} f2 {} f3 {} f4{}",
                f0Momentary, f1Momentary, f2Momentary, f3Momentary, f4Momentary);

        sendMessage("fg1m", new byte[]{ b2b(f0Momentary), b2b(f1Momentary), 
            b2b(f2Momentary), b2b(f3Momentary), b2b(f4Momentary) } );
    }

    /**
     * Send the DCC++ message to set the momentary state of functions F5, F6,
     * F7, F8
     */
    @Override
    protected void sendMomentaryFunctionGroup2() {
        log.debug("sendMomentaryFunctionGroup2(): f5 {} f6 {} f7 {} f8",
                f5Momentary, f6Momentary, f7Momentary, f8Momentary);

        sendMessage("fg2m", new byte[]{ b2b(f5Momentary), b2b(f6Momentary), 
            b2b(f7Momentary), b2b(f8Momentary) });
    }

    /**
     * Send the DCC++ message to set the momentary state of functions F9, F10,
     * F11, F12
     */
    @Override
    protected void sendMomentaryFunctionGroup3() {
        log.debug("sendMomentaryFunctionGroup3(): f9 {} f10 {} f11 {} f12",
                f9Momentary, f10Momentary, f11Momentary, f12Momentary);

        sendMessage("fg3m", new byte[]{ b2b(f9Momentary), b2b(f10Momentary), 
            b2b(f11Momentary), b2b(f12Momentary) } );
    }

    /**
     * Send the DCC++ message to set the momentary state of functions F13, F14,
     * F15, F16 F17 F18 F19 F20
     */
    @Override
    protected void sendMomentaryFunctionGroup4() {
         log.debug("sendMomentaryFunctionGroup4(): f13 {} f14 {} f15 {} f16",
                f13Momentary, f14Momentary, f15Momentary, f6Momentary);

        sendMessage("fg4m", new byte[] { b2b(f13Momentary), b2b(f14Momentary), 
            b2b(f15Momentary), b2b(f16Momentary), b2b(f17Momentary), 
            b2b(f18Momentary), b2b(f19Momentary), b2b(f20Momentary) });
    }

    /**
     * Send the DCC++ message to set the momentary state of functions F21, F22,
     * F23, F24 F25 F26 F27 F28
     */
    @Override
    protected void sendMomentaryFunctionGroup5() {
        log.debug("sendMomentaryFunctionGroup5(): f21 {} f22 {} f23 {} f24 {} f25 {} f26 {} f27 {} f28 {}",
                f21Momentary, f22Momentary, f23Momentary, f24Momentary, f25Momentary, f26Momentary, f27Momentary, f28Momentary);

        sendMessage("fg5m", new byte[]{ b2b(f21Momentary), b2b(f22Momentary), 
            b2b(f23Momentary), b2b(f24Momentary), b2b(f25Momentary), 
            b2b(f26Momentary), b2b(f27Momentary), b2b(f28Momentary) } );
    }

    /* 
     * setSpeedSetting - notify listeners and send the new speed to the
     * command station.
     */
    @Override
    synchronized public void setSpeedSetting(float speed) {
        if (log.isDebugEnabled()) {
            log.debug("set Speed to: " + speed
                    + " Current step mode is: " + this.speedStepMode);
        }
        super.setSpeedSetting(speed);
        if (speed < 0) {
            /* we're sending an emergency stop to this locomotive only */
            sendEmergencyStop();
        } else {
            if (speed > 1) {
                speed = (float) 1.0;
            }

            sendMessage("speed", (""+(int)speed).getBytes() );
        }
    }

    /* Since xpressnet has a seperate Opcode for emergency stop,
     * We're setting this up as a seperate protected function
     */
    protected void sendEmergencyStop() {
        
        sendMessage("EMGR_STOP", new byte[]{} );
    }

    /* Since there is only one "throttle" command to the DCC++ base station,
     * when we change the direction, we must also re-set the speed.
     */
    @Override
    public void setIsForward(boolean forward) {
        super.setIsForward(forward);
        setSpeedSetting(this.speedSetting);
    }

    /*
     * setSpeedStepMode - set the speed step value and the related
     *                    speedIncrement value.
     * <P>
     * @param Mode - the current speed step mode - default should be 128
     *              speed step mode in most cases
     *
     * NOTE: DCC++ only supports 128-step mode.  So we ignore the speed
     * setting, even though we store it.
     */
    @Override
    public void setSpeedStepMode(int Mode) {
        super.setSpeedStepMode(Mode);
    }

    /**
     * Dispose when finished with this object. After this, further usage of this
     * Throttle object will result in a JmriException.
     * <p>
     * This is quite problematic, because a using object doesn't know when it's
     * the last user.
     */
    @Override
    protected void throttleDispose() {
        active = false;
        finishRecord();
    }

    public int setDccAddress(int newaddress) {
        address = newaddress;
        return address;
    }

    public int getDccAddress() {
        return address;
    }

    // to handle quantized speed. Note this can change! Valued returned is
    // always positive.
    @Override
    public float getSpeedIncrement() {
        return speedIncrement;
    }


    @Override
    public LocoAddress getLocoAddress() {
        return new MqttLocoAddress(address);
    }

    synchronized protected void sendMessage(String topic, byte[] msg) {
        mqttAdapter.publish(sendTopicBase+topic, msg);
    }

    @Override
    public void notifyMqttMessage(String topic, String message) {
        log.info("got "+message);
    }


    // register for notification
    private final static Logger log = LoggerFactory.getLogger(MqttThrottle.class);
    
    
    public static class MqttLocoAddress implements LocoAddress {
        private final int addr;

        public MqttLocoAddress(int addr) {
            this.addr = addr;
        }

        @Override
        public int getNumber() {
            return addr;
        }

        @Override
        public Protocol getProtocol() {
            return addr<128 ? Protocol.DCC_SHORT : Protocol.DCC_LONG;
        }
        
    }
}
