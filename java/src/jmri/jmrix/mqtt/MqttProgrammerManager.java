package jmri.jmrix.mqtt;

import jmri.AddressedProgrammer;
import jmri.Programmer;
import jmri.managers.DefaultProgrammerManager;

/**
 * Extend DefaultProgrammerManager to provide ops mode programmers on DCC++
 *
 * @see jmri.managers.DefaultProgrammerManager
 * @author Paul Bender Copyright (C) 2003
 * @author Mark Underwood Copyright (C) 2015
  *
 * Based on XNetProgrammerManager by Paul Bender
 */
public class MqttProgrammerManager extends DefaultProgrammerManager {
    private final MqttAdapter mqttAdapter;


    public MqttProgrammerManager(Programmer pProgrammer, MqttSystemConnectionMemo memo) {
        super(pProgrammer, memo);
        mqttAdapter = memo.getMqttAdapter();
    }

    @Override
    public boolean isAddressedModePossible() {
        return true;
    }

    @Override
    public AddressedProgrammer getAddressedProgrammer(boolean pLongAddress, int pAddress) {
        return new MqttProgrammer(pAddress, pLongAddress, mqttAdapter);
    }

    @Override
    public AddressedProgrammer reserveAddressedProgrammer(boolean pLongAddress, int pAddress) {
        return null;
    }
}


