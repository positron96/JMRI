package jmri.jmrix.mqtt.configurexml;

import jmri.configurexml.JmriConfigureXmlException;
import jmri.jmrix.dcc.DccTurnoutManager;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides load and store functionality for configuring MqttTurnoutManagers.
 * <P>
 * Uses the store method from the abstract base class, but provides a load
 * method here.
 *
 * @author Lionel Jeanson Copyright: Copyright (c) 2017
 */
public class MqttSensorManagerXml extends jmri.managers.configurexml.AbstractSensorManagerConfigXML {

    public MqttSensorManagerXml() {
        super();
    }

    @Override
    public void setStoreElementClass(Element sensors) {
        sensors.setAttribute("class", "jmri.jmrix.mqtt.configurexml.MqttSensorManagerXml");
    }

    @Override
    public void load(Element element, Object o) {
        log.error("Invalid method called");
    }

    @Override
    public boolean load(Element shared, Element perNode) throws JmriConfigureXmlException {
        // load individual turnouts
        return this.loadSensors(shared);
    }

    // initialize logging
    private final static Logger log = LoggerFactory.getLogger(MqttSensorManagerXml.class);
}
