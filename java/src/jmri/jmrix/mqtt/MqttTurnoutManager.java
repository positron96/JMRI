package jmri.jmrix.mqtt;

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
    synchronized public String createSystemName(String hwAddr, String sysPrefix) throws JmriException {
        if (!sysPrefix.equals( getSystemPrefix() )) {
            log.warn("Suddenly creating sensor with different system prefix: {} vs {}", sysPrefix, getSystemPrefix() );
        }
        
        return sysPrefix+typeLetter()+hwAddr;
    }
    
    private final static Logger log = LoggerFactory.getLogger(MqttTurnoutManager.class);

    
}

