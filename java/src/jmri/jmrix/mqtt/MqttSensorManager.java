package jmri.jmrix.mqtt;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import jmri.JmriException;
import jmri.Sensor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement Sensor Manager for DCC++ systems.
 * <p>
 * Based on {@link jmri.jmrix.dccpp.DCCppSensorManager}.
 */
public class MqttSensorManager extends jmri.managers.AbstractSensorManager {

    private final MqttAdapter mqttAdapter;
    protected String sysPrefix = null;

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
        this.sysPrefix = sysPrefix;        
    }

    @Override
    public String getSystemPrefix() {
        return sysPrefix;
    }

    // to free resources when no longer used
    @Override
    public void dispose() {        
        super.dispose();
    }

    @Override
    public Sensor createNewSensor(String systemName, String userName) throws IllegalArgumentException {
        String prefix = getSystemPrefix() + typeLetter();
        Sensor s = new MqttSensor(mqttAdapter, systemName, 
                systemName.substring(prefix.length()));
        s.setUserName(userName);
        return s;
    }

    @Override
    public boolean allowMultipleAdditions(String systemName) {
        return true;
    }

    
    @Override
    synchronized public String createSystemName(String hwAddr, String sysPrefix) {
        if (!sysPrefix.equals( getSystemPrefix() )) {
            log.warn("Suddenly creating sensor with different system prefix: {} vs {}", sysPrefix, getSystemPrefix() );
        }
        return sysPrefix + typeLetter() + hwAddr;
    }

    /**
     * Provide next valid address.
     * Does not enforce any rules on the encoder or input values.
     */
    @Override
    synchronized public String getNextValidAddress(String curAddress, String prefix) {
        
        Pattern pp = Pattern.compile(".*?(\\d+).*?"); // greedy number and non-greedy pefix and suffix
        Matcher matcher = pp.matcher(curAddress);
        
        if(!matcher.matches()) {
            log.error("Unable to extract number from Hardware Address {}", curAddress);
            jmri.InstanceManager
                    .getDefault(jmri.UserPreferencesManager.class)
                    .showErrorMessage(Bundle.getMessage("ErrorTitle"),
                            Bundle.getMessage("ErrorConvertNumberX", curAddress), 
                            "", "", true, false);
            return null;
        }
        
        int numAddr = Integer.parseInt(matcher.group(1));

        Function<Integer,String> genAddr = (num)->new StringBuilder(curAddress)
                .replace(matcher.start(1), matcher.end(1), Integer.toString(num))
                .toString();
        
        
        String tmpSName =  createSystemName(curAddress, prefix);
        
        //Check to determine if the systemName is in use, return null if it is,
        //otherwise return the next valid address.
        Sensor s = getBySystemName(tmpSName);
        if(s==null) return curAddress;
        

        for (int x = 1; x < 15; x++) {
            numAddr ++;
            String tmpHwAddr = genAddr.apply(numAddr);
            tmpSName = createSystemName(tmpHwAddr, prefix);
            s = getBySystemName(tmpSName);
            if (s == null) {
                return tmpHwAddr;
            }
        }
        
        log.warn("Exhaused search of empty addresses");
        return null;

    }



    /**
     * Validate system name format. Mqtt address can be anything.
     * @returns {@link NameValidity#VALID }
     */
    @Override
    public NameValidity validSystemNameFormat(String systemName) {
        return NameValidity.VALID; 
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
