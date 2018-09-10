/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.mqtt.networkdriver.configurexml;

import jmri.jmrix.configurexml.AbstractNetworkConnectionConfigXml;
import jmri.jmrix.mqtt.networkdriver.MqttAdapter;
import jmri.jmrix.mqtt.networkdriver.MqttConnectionConfig;

/**
 *
 * @author lionel
 */
public class MqttConnectionConfigXml  extends AbstractNetworkConnectionConfigXml {

    public MqttConnectionConfigXml() {
        super();
    }

    @Override
    protected void getInstance() {
        adapter = new MqttAdapter();
    }

    @Override
    protected void getInstance(Object object) {
        adapter = ((MqttConnectionConfig) object).getAdapter();
    }

    @Override
    protected void register() {
        this.register(new MqttConnectionConfig(adapter));
    }
   
}
