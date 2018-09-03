/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jmri.jmrix.mqtt;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
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
    
    private final Executor notifier = Executors.newSingleThreadExecutor();
    
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
    protected void timeout() {
        if(waiting) {
            log.debug("TimeOut called");
            waiting = false;
            if(ll!=null) {
                notifier.execute( ()-> ll.programmingOpReply(0, jmri.ProgListener.FailedTimeout) );
            }
            
        } else {
            log.debug("TimeOut called, but not waiting");
        }
    }
    
        
    private String pubTopic(int cv) {
        return "loco/"+addr+"/CV/"+cv+"/set";
    }
    
    private void sendGetReq(int cv) {
        synchronized (this) {
            waiting = true;
            cCV = cv;
            cVal = -1;
        }
        mqtt.publish(pubTopic(cCV), "".getBytes(), false);    
        restartTimer(25000);
    }
    
    private void sendSetReq(int cv, int val) {
        synchronized (this) {
            waiting = true;
            cCV = cv;
            cVal = val;
        }
        mqtt.publish(pubTopic(cCV), Integer.toString(val).getBytes(), false);
        restartTimer(25000);
    }

    @Override
    public void writeCV(int cv, int val, ProgListener p) throws ProgrammerException {
        log.debug("writeCV({}, {})", cv, val);
        
        if(waiting) {
            p.programmingOpReply(0, ProgListener.ProgrammerBusy);
            return;
        }
        ll = p;
        sendSetReq(cv, val);        
    }

    @Override
    public void readCV(int cv, ProgListener p) throws ProgrammerException {
        log.debug("readCV({})", cv);
        
        if(waiting) {
            p.programmingOpReply(0, ProgListener.ProgrammerBusy);
            return;
        }
        ll = p;
        sendGetReq(cv);
    }

    @Override
    public void confirmCV(String cv, int val, ProgListener p) throws ProgrammerException {
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
    public void notifyMqttMessage(String topic, String message) {
        synchronized (this) {
            if(!waiting) { return;  }
        }
        
        Matcher matcher = listenTopicRegex.matcher(topic);
        if (matcher.matches()) {
            int a = Integer.parseInt( matcher.group("addr") );
            if (opsMode && a!=addr) {
                log.info("Wrong address {}, expected {}!", a, addr);
                return;
            }
            synchronized (this) {
                int cv = Integer.parseInt( matcher.group("cv") );
                if(cv!=cCV) {
                    log.info("Wrong CV {}, expected {}!", cv, cCV);
                    return;
                }
                int val = Integer.parseInt(message);
                log.info("Programmer processing CV {}, value {}", cv, val);
                waiting = false;
                stopTimer();
                if(ll!=null) {
                    notifier.execute( ()->ll.programmingOpReply(val, ProgListener.OK) );
                }
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
