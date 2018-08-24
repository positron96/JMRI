package jmri.jmrix.mqtt;

import jmri.jmrix.dccpp.*;
import javax.swing.JOptionPane;
import jmri.JmriException;
import jmri.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement Sensor Manager for DCC++ systems.
 * <p>
 * System names are "DxppSnnn", where Dx is the system prefix and nnn is the sensor number without padding.
 *
 * @author Paul Bender Copyright (C) 2003-2010
 * @author Mark Underwood Copyright (C) 2015
 */
public class MqttSensorManager extends jmri.managers.AbstractSensorManager {

    private final MqttAdapter mqttAdapter;
    protected String prefix = null;

    /**
     * Create an new DCC++ SensorManager.
     * Has to register for DCC++ events.
     *
     * @param ma the MqttAdapter to connect the SensorManager to
     * @param sysPrefix the system connection prefix string as set for this connection in SystemConnectionMemo
     */
    public MqttSensorManager(MqttAdapter ma, String sysPrefix) {
        super();
        mqttAdapter = ma;
        prefix = sysPrefix;        
    }

    @Override
    public String getSystemPrefix() {
        return prefix;
    }

    // to free resources when no longer used
    @Override
    public void dispose() {        
        super.dispose();
    }

    @Override
    public Sensor createNewSensor(String systemName, String userName) throws IllegalArgumentException {
        Sensor s = new MqttSensor(mqttAdapter, systemName);
        s.setUserName(userName);
        return s;
    }

    @Override
    public boolean allowMultipleAdditions(String systemName) {
        return true;
    }

    
    @Override
    synchronized public String createSystemName(String hwAddr, String sysPrefix) throws JmriException {
        if (!sysPrefix.equals( getSystemPrefix() )) {
            log.warn("Suddenly creating sensor with different system prefix: {} vs {}", sysPrefix, getSystemPrefix() );
        }
        int encoderAddress = 0;
        int channel = 0;

        if (hwAddr.contains(":")) {
            // Address format passed is in the form of encoderAddress:input or T:turnout address
            int seperator = hwAddr.indexOf(":");
            try {
                encoderAddress = Integer.valueOf(hwAddr.substring(0, seperator)).intValue();
                channel = Integer.valueOf(hwAddr.substring(seperator + 1)).intValue();
            } catch (NumberFormatException ex) {
                log.error("Unable to convert {} into the cab and input format of nn:xx", hwAddr);
                JOptionPane.showMessageDialog(null, Bundle.getMessage("WarningAddressAsNumber"),
                        Bundle.getMessage("WarningTitle"), JOptionPane.ERROR_MESSAGE);
                throw new JmriException("Hardware Address passed should be a number");
            }
            
        } else {
            // Entered in using the old format
            try {
                encoderAddress = Integer.parseInt(hwAddr);
                channel = 0;
            } catch (NumberFormatException ex) {
                log.error("Unable to convert {} Hardware Address to a number", hwAddr);
                throw new JmriException("Hardware Address passed should be a number");
            }
        }

        curAddr = encoderAddress;
        curCh = channel;
        return currentSystemName(sysPrefix);
    }
    
    private String currentSystemPostfix() {
        return curAddr + "_" + curCh;
    }
    
    private String currentSystemName(String sysPrefix) {
        return sysPrefix + typeLetter() + currentSystemPostfix();
    }

    private volatile int curAddr; 
    private volatile int curCh;

    /**
     * Provide next valid address.
     * Does not enforce any rules on the encoder or input values.
     */
    @Override
    synchronized public String getNextValidAddress(String curAddress, String prefix) {

        String tmpSName = "";

        try {
            tmpSName = createSystemName(curAddress, prefix);
        } catch (JmriException ex) {
            jmri.InstanceManager.getDefault(jmri.UserPreferencesManager.class).
                    showErrorMessage(Bundle.getMessage("ErrorTitle"), Bundle.getMessage("ErrorConvertNumberX", curAddress), "" + ex, "", true, false);
            return null;
        }
        //Check to determine if the systemName is in use, return null if it is,
        //otherwise return the next valid address.
        Sensor s = getBySystemName(tmpSName);
        if (s != null) {
            for (int x = 1; x < 15; x++) {
                curCh += 1;
                tmpSName = currentSystemName(prefix);
                s = getBySystemName(tmpSName);
                if (s == null) {
                    return currentSystemPostfix();
                }
            }
            return null;
        } else {
            return currentSystemPostfix();
        }
    }



    /**
     * Validate system name format.
     *
     * @return VALID if system name has a valid format, else returns INVALID
     */
    @Override
    public NameValidity validSystemNameFormat(String systemName) {
        return NameValidity.VALID; //: NameValidity.INVALID;
    }

    /**
     * Provide a manager-specific tooltip for the Add new item beantable pane.
     */
    @Override
    public String getEntryToolTip() {
        String entryToolTip = Bundle.getMessage("AddOutputEntryToolTip");
        return entryToolTip;
    }

    private final static Logger log = LoggerFactory.getLogger(MqttSensorManager.class);

}
