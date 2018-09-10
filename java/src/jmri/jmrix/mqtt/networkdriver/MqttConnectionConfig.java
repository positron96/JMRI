
package jmri.jmrix.mqtt.networkdriver;

import jmri.jmrix.mqtt.MqttConnectionTypeList;
import jmri.jmrix.mqtt.networkdriver.MqttAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lionel
 */
public class MqttConnectionConfig extends jmri.jmrix.AbstractNetworkConnectionConfig {

    public MqttConnectionConfig(jmri.jmrix.NetworkPortAdapter p) {
        super(p);
    }

    public MqttConnectionConfig() {
        super();
    }

    @Override
    public String name() {
        return "Network Connection";
    }

    @Override
    protected void setInstance() {
        if (adapter == null) {
            adapter = new MqttAdapter();
            adapter.setPort(1883);
        }
    }

    @Override
    public String getInfo() {
        return("MQTT");
    }

    @Override
    public String getManufacturer() {
        return(MqttConnectionTypeList.GENMAN);
    }
    
    @Override
    public boolean isAutoConfigPossible() {
        return true;
    }
    
    

    // private final static Logger log = LoggerFactory.getLogger(MqttConnectionConfig.class);    

    @Override
    protected void checkInitDone() {
        super.checkInitDone();
        
        super.adNameField.setEnabled(true);
        super.adNameFieldLabel.setEnabled(true);
        //super.serviceTypeField.setEnabled(true);
        //super.serviceTypeFieldLabel.setEnabled(true);
    }
}
