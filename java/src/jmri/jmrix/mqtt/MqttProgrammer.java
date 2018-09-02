/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.mqtt;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jmri.AddressedProgrammer;
import jmri.ProgListener;
import jmri.ProgrammerException;
import jmri.ProgrammingMode;
import jmri.jmrix.AbstractProgrammer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author positron
 */
public class MqttProgrammer extends AbstractProgrammer 
        implements MqttEventListener, AddressedProgrammer {
    
    private boolean waiting;
    private ProgListener ll;
    private int cCV;
    private int cVal;
    
    private final int addr;
    private final boolean opsMode;
    private final boolean longAddr;
    
    private final String listenTopic;
    private final MqttAdapter mqtt;
    
    private final Pattern listenTopicRegex = Pattern.compile("(?:.+/)??loco/(?<addr>\\d+)/CV/(?<cv>\\d+)");
    
    public MqttProgrammer(MqttAdapter mqtt) {
        addr = 0;
        longAddr = false;
        opsMode = false;
        this.mqtt = mqtt;
        listenTopic = "loco/+/CV/+";
        mqtt.subscribe(listenTopic, this);
    }
    
    public MqttProgrammer(int addr, boolean longAddr, MqttAdapter mqtt) {
        this.addr = addr;
        this.longAddr = longAddr;
        opsMode = true;
        this.mqtt = mqtt;
        listenTopic = "loco/"+addr+"/CV/+";
        mqtt.subscribe(listenTopic, this);
    }

    @Override
    public List<ProgrammingMode> getSupportedModes() {
        return Arrays.asList(ProgrammingMode.DIRECTBYTEMODE );
    }

    @Override
    protected synchronized void timeout() {
        log.debug("TimeOut called");
        if(waiting) {
            if(ll!=null) ll.programmingOpReply(0, jmri.ProgListener.FailedTimeout);            
            waiting = false;
        }
    }
    
        
    private String pubTopic(int cv) {
        return "loco/"+addr+"/CV/"+cv+"/set";
    }
    
    private void sendGetReq(int cv) {
        waiting = true;
        cCV = cv;
        cVal = -1;
        mqtt.publish(pubTopic(cCV), "".getBytes(), false);        
    }
    
    private void sendSetReq(int cv, int val) {
        waiting = true;
        cCV = cv;
        cVal = val;
        mqtt.publish(pubTopic(cCV), Integer.toString(val).getBytes(), false);
    }

    @Override
    public synchronized void writeCV(int cv, int val, ProgListener p) throws ProgrammerException {
        log.debug("writeCV({}, {})", cv, val);
        
        if(waiting) {
            p.programmingOpReply(0, ProgListener.ProgrammerBusy);
            return;
        }
        ll = p;
        sendSetReq(cv, val);
        restartTimer(5000);
    }

    @Override
    public synchronized void readCV(int cv, ProgListener p) throws ProgrammerException {
        log.debug("readCV({})", cv);
        
        if(waiting) {
            p.programmingOpReply(0, ProgListener.ProgrammerBusy);
            return;
        }
        ll = p;
        sendGetReq(cv);
        restartTimer(5000);
    }

    @Override
    public synchronized void confirmCV(String cv, int val, ProgListener p) throws ProgrammerException {
        log.debug("confirmCV({}, {})", cv, val);
        
        readCV(Integer.parseInt(cv) , p);
        
             
//        if(waiting) {
//            p.programmingOpReply(0, ProgListener.ProgrammerBusy);
//            return;
//        }
//        ll = p;
//        sendGetReq(Integer.parseInt(cv) );
//        cVal = val;
//        restartTimer(5000);
    }
    

    @Override
    public synchronized void notifyMqttMessage(String topic, String message) {
        if(!waiting) {
            return;
        }
        
        Matcher matcher = listenTopicRegex.matcher(topic);
        if (matcher.matches()) {
            int a = Integer.parseInt( matcher.group("addr") );
            if (opsMode && a!=addr) {
                log.info("Wrong address {}, expected {}!", a, addr);
                return;
            }
            int cv = Integer.parseInt( matcher.group("cv") );
            if(cv!=cCV) {
                log.info("Wrong CV {}, expected {}!", cv, cCV);
                return;
            }
            int val = Integer.parseInt(message);
            waiting = false;
            if(ll!=null) {
                ll.programmingOpReply(val, ProgListener.OK);
            }
        }
        
        
    }

    
    
    @Override
    public boolean getLongAddress() {
        if(!opsMode) throw new IllegalStateException("Not addressed");
        return longAddr;
    }

    @Override
    public int getAddressNumber() {
        if(!opsMode) throw new IllegalStateException("Not addressed");
        return addr;
    }

    @Override
    public String getAddress() {
        if(!opsMode) throw new IllegalStateException("Not addressed");
        return ""+addr;
    }
    
    
    private final static Logger log = LoggerFactory.getLogger(MqttProgrammer.class);
    
    
}
