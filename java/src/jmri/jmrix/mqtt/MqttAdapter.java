package jmri.jmrix.mqtt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lionel
 */
public class MqttAdapter extends jmri.jmrix.AbstractNetworkPortController implements MqttCallback {

    private final static String PROTOCOL = "tcp://";
    private final static String CLID = "JMRI";
    private String BASETOPIC = "rail/";

    HashMap<String, ArrayList<MqttEventListener>> mqttEventListeners;

    MqttClient mqttClient;

    public MqttAdapter() {
        super(new MqttSystemConnectionMemo());
                
        option2Name = "MQTTprefix";
        options.put(option2Name, new Option("MQTT Prefix :", new String[]{"rail/", ""}));
        allowConnectionRecovery = true;
    }

    @Override
    public void configure() {        
        log.debug("Doing configure...");
        
        BASETOPIC = getOptionState(option2Name);
                
        mqttEventListeners = new HashMap<>();
        getSystemConnectionMemo().setMqttAdapter(this);
        getSystemConnectionMemo().configureManagers();
        mqttClient.setCallback(this);
    }

    @Override
    public void connect() throws IOException {
     
        log.debug("Doing connect...");
        try {
            String clientID = CLID + "-" + this.getUserName();
            mqttClient = new MqttClient(PROTOCOL + getCurrentPortName(), clientID);
            mqttClient.connect();
        } catch (MqttException ex) {
            throw new IOException("Can't create MQTT client", ex);
        }
    }

    @Override
    public MqttSystemConnectionMemo getSystemConnectionMemo() {
        return (MqttSystemConnectionMemo) super.getSystemConnectionMemo();
    }

    public void subscribe(String topic, MqttEventListener mel) {
        if (mqttEventListeners == null || mqttClient == null) {
            jmri.util.Log4JUtil.warnOnce(log, "Trying to subscribe before connect/configure is done");
            return;
        }
        try {
            String fullTopic = BASETOPIC + topic;
            log.debug("subscribing to {}", fullTopic);
            if (mqttEventListeners.containsKey(fullTopic)) {
                if (!mqttEventListeners.get(fullTopic).contains(mel)) {
                    mqttEventListeners.get(fullTopic).add(mel);
                }
                return;
            }
            ArrayList<MqttEventListener> mels = new ArrayList<>();
            mels.add(mel);
            mqttEventListeners.put(fullTopic, mels);
            mqttClient.subscribe(fullTopic);
        } catch (MqttException ex) {
            log.error("Can't subscribe : ", ex);
        }
    }

    public void unsubscribe(String topic, MqttEventListener mel) {
        log.debug("unsubscribing from {}", topic);
        String fullTopic = BASETOPIC + topic;
        mqttEventListeners.get(fullTopic).remove(mel);
        if (mqttEventListeners.get(fullTopic).isEmpty()) {
            try {
                log.debug("really unsubscribing from {}", fullTopic);
                mqttClient.unsubscribe(fullTopic);
                mqttEventListeners.remove(fullTopic);
            } catch (MqttException ex) {
                log.error("Can't unsubscribe : ", ex);
            }
        }
    }

    public void unsubscribeAll(MqttEventListener mel) {
        mqttEventListeners.keySet().forEach((t) -> {
            unsubscribe(t, mel);
        });
    }

    public void publish(String topic, byte[] payload) {
        publish(topic, payload, true);
    }
    
    public void publish(String topic, byte[] payload, boolean retained) {
        try {
            String fullTopic = BASETOPIC + topic;
            mqttClient.publish(fullTopic, payload, 0, retained);
        } catch (MqttException ex) {
            log.error("Can't publish : ", ex);
        }
    }

    public MqttClient getMQttClient() {
        return mqttClient;
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        log.warn("Lost MQTT broker connection...");
        if (this.allowConnectionRecovery) {
            log.info("...trying to reconnect");
            try {
                mqttClient.connect();
                mqttClient.setCallback(this);
                for (String t : mqttEventListeners.keySet()) {
                    mqttClient.subscribe(t);
                }
            } catch (MqttException ex) {
                log.error("Unable to reconnect", ex);
            }
            return;
        }
        log.error("Won't reconnect");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        log.debug("Message received, topic : {}", topic);
        
        AtomicBoolean processed = new AtomicBoolean(false);
        mqttEventListeners.entrySet().stream()
                .filter( tt->MqttTopic.isMatched(tt.getKey(), topic) )
                .flatMap(tt->tt.getValue().stream() )
                .forEach( (list) -> {            
                    list.notifyMqttMessage(topic, new String( mm.getPayload() ) );
                    processed.set(true);
                });
        if (!processed.get() ) {
            log.error("No one subscribed to {}", topic);
        }
        
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        log.debug("Message delivered");
    }

    private final static Logger log = LoggerFactory.getLogger(MqttAdapter.class);
}
