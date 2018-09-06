package jmri.jmrix.mqtt;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import jmri.JmriException;
import jmri.Turnout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement turnout manager for MQTT systems
 * <P>
 * System names are "MTnnn", where nnn is the turnout number without padding.
 *
 * @author Lionel Jeanson Copyright: Copyright (c) 2017
 */
public class MqttTurnoutManager extends jmri.managers.AbstractTurnoutManager {
    private final MqttAdapter mqttAdapter;
    private final String systemPrefix;

    MqttTurnoutManager(MqttAdapter ma, String sysPrefix) {
        super();
        mqttAdapter = ma;
        systemPrefix = sysPrefix;        
    }

    @Override
    public String getSystemPrefix() {
        return systemPrefix;
    }

    @Override
    public Turnout createNewTurnout(String systemName, String userName) {
        String prefix = systemPrefix + typeLetter();
        Turnout t = new MqttTurnout(mqttAdapter, systemName, 
                systemName.substring(prefix.length()) );
        t.setUserName(userName);

        return t;
    }

    @Override
    public String getEntryToolTip() {
        String entryToolTip = Bundle.getMessage("AddOutputEntryToolTip");
        return entryToolTip;
    }
    
    @Override
    synchronized public String createSystemName(String hwAddr, String sysPrefix) {
        if (!sysPrefix.equals( getSystemPrefix() )) {
            log.warn("Suddenly creating sensor with different system prefix: {} vs {}", sysPrefix, getSystemPrefix() );
        }
        
        return sysPrefix+typeLetter()+hwAddr;
    }
    
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
        Turnout s = getBySystemName(tmpSName);
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
    
    private final static Logger log = LoggerFactory.getLogger(MqttTurnoutManager.class);

    
}

