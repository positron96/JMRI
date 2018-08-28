package jmri.jmrix.mqtt;

import java.lang.reflect.Field;
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

    protected int address;
    
    private String sendTopicBase;


    public MqttThrottle(SystemConnectionMemo memo, MqttAdapter controller) {
        super(memo);
        mqttAdapter = controller;
        
        log.debug("MqttThrottle constructor");
    }

    public MqttThrottle(SystemConnectionMemo memo, LocoAddress address, MqttAdapter controller) {
        super(memo);
        this.mqttAdapter = controller;
        this.setDccAddress(address.getNumber() );
        this.speedIncrement = SPEED_STEP_128_INCREMENT;
        this.speedStepMode = DccThrottle.SpeedStepMode128;

        f0Momentary = f1Momentary = f2Momentary = f3Momentary = f4Momentary
                = f5Momentary = f6Momentary = f7Momentary = f8Momentary = f9Momentary
                = f10Momentary = f11Momentary = f12Momentary = false;

        sendTopicBase = "loco/"+address.getNumber()+"/";
        log.debug("DCCppThrottle constructor called for address {}", address);

    }
    
    private void setF(int f, boolean v)  {
        log.debug("set F{} to {}", f, v);
        try {
            Field field = getClass().getSuperclass().getDeclaredField("f"+f);
            boolean old = field.getBoolean(this);
            field.setBoolean(this, v);
            sendMessage("F"+f, v ? "1" : "0");
            if (old != v) notifyPropertyChangeListener("F"+f, old, v);
        } catch (NoSuchFieldException | SecurityException | 
                IllegalArgumentException | IllegalAccessException ex) {
            log.error("could not set Function", ex);
        }
    }
    
    
    @Override
    public void setF0(boolean v) {  setF(0, v);  }

    @Override
    public void setF1(boolean v) {  setF(1, v);  }

    @Override
    public void setF2(boolean v) {  setF(2, v);  }

    @Override
    public void setF3(boolean v) {  setF(3, v);  }
    @Override
    public void setF4(boolean v) {  setF(4, v);  }

    @Override
    public void setF5(boolean v) {  setF(5, v);  }

    @Override
    public void setF6(boolean v) {  setF(6, v);  }

    @Override
    public void setF7(boolean v) {  setF(7, v);  }

    @Override
    public void setF8(boolean v) {  setF(8, v);  }

    @Override
    public void setF9(boolean v) {  setF(9, v);  }

    @Override
    public void setF10(boolean v) {  setF(10, v);  }

    @Override
    public void setF11(boolean v) {  setF(11, v);  }

    @Override
    public void setF12(boolean v) {  setF(12, v);  }

    @Override
    public void setF13(boolean v) {  setF(13, v);  }

    @Override
    public void setF14(boolean v) {  setF(14, v);  }

    @Override
    public void setF15(boolean v) {  setF(15, v);  }

    @Override
    public void setF16(boolean v) {  setF(16, v);  }

    @Override
    public void setF17(boolean v) {  setF(17, v);  }

    @Override
    public void setF18(boolean v) {  setF(18, v);  }

    @Override
    public void setF19(boolean v) {  setF(19, v);  }

    @Override
    public void setF20(boolean v) {  setF(20, v);  }

    @Override
    public void setF21(boolean v) {  setF(21, v);  }

    @Override
    public void setF22(boolean v) {  setF(22, v);  }

    @Override
    public void setF23(boolean v) {  setF(23, v);  }

    @Override
    public void setF24(boolean v){  setF(24, v);  }

    @Override
    public void setF25(boolean v) {  setF(25, v);  }
    
    @Override
    public void setF26(boolean v) {  setF(26, v);  }

    @Override
    public void setF27(boolean v) {  setF(27, v);  }

    @Override
    public void setF28(boolean v) {  setF(28, v);  }
    
    
    
    @Override
    protected void sendMomentaryFunctionGroup1() {   }

    @Override
    protected void sendMomentaryFunctionGroup2() {   }

    @Override
    protected void sendMomentaryFunctionGroup3() {   }

    @Override
    protected void sendMomentaryFunctionGroup4() {   }

    @Override
    protected void sendMomentaryFunctionGroup5() {   }
    

   
    private void sendSpeed() {
        int v = Math.round(getSpeedSetting() / getSpeedIncrement());
        if (! super.getIsForward() ) {
            v = -v;
        }
        sendMessage("speed", ""+v, false );
    }
    
    /* 
     * setSpeedSetting - notify listeners and send the new speed to the
     * command station.
     */
    @Override
    synchronized public void setSpeedSetting(float speed) {
        log.debug("set Speed to: {},  Current step mode is: {}",  speed, this.speedStepMode);
        if (speed > 1) {
            speed = (float) 1.0;
        }
        super.setSpeedSetting(speed);
        if (speed < 0) {
            sendEmergencyStop();
        } else {
            sendSpeed();
        }
    }

    private void sendEmergencyStop() {
        sendMessage("EMGR_STOP", "" );
    }

    /* Since there is only one "throttle" command to the DCC++ base station,
     * when we change the direction, we must also re-set the speed.
     */
    @Override
    public void setIsForward(boolean forward) {
        super.setIsForward(forward);
        sendSpeed();
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

    private int setDccAddress(int newaddress) {
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
        return new DccLocoAddress(address, address<128 ? LocoAddress.Protocol.DCC_SHORT : LocoAddress.Protocol.DCC_LONG);
    }

    synchronized protected void sendMessage(String topic, String msg) {
        mqttAdapter.publish(sendTopicBase+topic, msg.getBytes() );
    }
    
    synchronized protected void sendMessage(String topic, String msg, boolean retain) {
        mqttAdapter.publish(sendTopicBase+topic, msg.getBytes(), retain);
    }

    @Override
    public void notifyMqttMessage(String topic, String message) {
        log.info("got {}: {}", topic, message);
    }


    // register for notification
    private final static Logger log = LoggerFactory.getLogger(MqttThrottle.class);
    

}
