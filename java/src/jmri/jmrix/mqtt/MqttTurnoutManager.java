package jmri.jmrix.mqtt;

import jmri.Turnout;

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
        Turnout t;
        int addr = Integer.parseInt(systemName.substring( systemPrefix.length() ));
        t = new MqttTurnout(mqttAdapter, addr);
        t.setUserName(userName);

        return t;
    }

    @Override
    public String getEntryToolTip() {
        String entryToolTip = Bundle.getMessage("AddOutputEntryToolTip");
        return entryToolTip;
    }

    
}

