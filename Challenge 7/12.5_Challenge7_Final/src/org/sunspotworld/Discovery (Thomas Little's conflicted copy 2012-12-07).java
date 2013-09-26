/*
 * SunSpotApplication.java
 *
 * Created on Nov 15, 2012 1:44:50 AM;
 */

package org.sunspotworld;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.IProprietaryRadio;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
//import com.sun.spot.peripheral.radio.routing.RoutingPolicyManager;
//import com.sun.spot.peripheral.radio.mhrp.aodv.AODVManager;
//import com.sun.spot.peripheral.radio.shrp.SingleHopManager;
import com.sun.spot.util.IEEEAddress;

import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ILed;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ILightSensor;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;

import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Routines to turn multiple SPOTs broadcasting their information and discover
 * peers in the same PAN
 *
 * The SPOT uses the LEDs to display its status as follows:
 * LED 0:
 * Red = missed an expected packet
 * Green = received a packet 
 *
 * LED 1-5:
 * Leader displayed count of connected spots in green
 * 
 * LED 6:
 * display TiltChange flag when sw1 is pressed
 * Blue = false
 * Green = true
 * 
 * LED 7:
 * Red = right tilt 
 * Blue = left tilt
 *
 * Press left switch to change neighbor's tilt state:
 * by sending out tilt change flag in the datagram
 * SW1 = change neighbor's tilt
 * 
 * Switch 2 right now is used to adjust transmitting power
 *
 * Note: Each group need to use their own channels to avoid interference from others
 * channel 26 is default
 * Group 1: 11-13
 * Group 2: 14-16
 * Group 4: 20-22
 * Group 5: 23-25
 * Group 6: 17-19
 * 
 * 
 * @author Yuting Zhang
 * date: Nov 28,2012
 * Note: this work is base on Radio Strength demo from Ron Goldman
 */
public class Discovery extends MIDlet {

    private static final String VERSION = "1.0";
    // CHANNEL_NUMBER  default as 26, each group set their own correspondingly
    //private static final int CHANNEL_NUMBER = IProprietaryRadio.DEFAULT_CHANNEL; 
    private static final int CHANNEL_NUMBER = 23;
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String BROADCAST_PORT      = "42";
    private static final int PACKETS_PER_SECOND     = 2;
    private static final int PACKET_INTERVAL        = 1000 / PACKETS_PER_SECOND;    // 1000/2 = 0.5 seconds
 //   private static AODVManager aodv = AODVManager.getInstance();
    
    private int channel = CHANNEL_NUMBER;
    private int power = 32;                             // Start with max transmit power
    
    private ISwitch sw1 = (ISwitch)Resources.lookup(ISwitch.class, "SW1");
    private ISwitch sw2 = (ISwitch)Resources.lookup(ISwitch.class, "SW2");
    private ITriColorLEDArray leds = (ITriColorLEDArray)Resources.lookup(ITriColorLEDArray.class);
    private ITriColorLED statusLED = leds.getLED(0);
    private IAccelerometer3D accel = (IAccelerometer3D)Resources.lookup(IAccelerometer3D.class);
//    private ILightSensor light = (ILightSensor)Resources.lookup(ILightSensor.class);

    private LEDColor red   = new LEDColor(50,0,0);
    private LEDColor green = new LEDColor(0,50,0);
    private LEDColor blue  = new LEDColor(0,0,50);
    private LEDColor yellow = new LEDColor(50,50,0);
    private LEDColor purple = new LEDColor(50,0,50);
    private LEDColor random = new LEDColor(0,50,50);
    private LEDColor white = new LEDColor(50,50,50);
    
    private double Xtilt;
    
    private boolean xmitDo = true;
    private boolean recvDo = true;
    private boolean ledsInUse = false;
    
    private long myAddr = 0; // own MAC addr (ID)
    
    private long leader = 0;  // leader MAC addr 
    private long follower = 0; // follower MAC addr
    private long TimeStamp;
    private int tilt = 0; // initialized as 0, right == 1, left == -1
    private boolean tiltchange = false;
    private boolean resetFlag = false;
    
    private int moteCount = 1;  // used to count how many motes are connected to leader
    private long[] moteAddr = {0, 0, 0, 0, 0};
    private long[] sortedAddr = {0, 0, 0, 0, 0};
    private int mynum = 4;
    
    private long StartTime;
    private long tempTime;
    
     /**
     * Loop to continually broadcast message.
     * message format
     * (long)myAddr,(long)follower,(long)leader,(long)TimeStamp,(int)tilt,
     * (boolean)tiltchang,(int)power,(int)count
     */
    private void xmitLoop () {
        //ILed led = Spot.getInstance().getGreenLed();
        RadiogramConnection txConn = null;
        xmitDo = true;
        while (xmitDo) {
            try {
                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
                long count = 0;
                while (xmitDo) {
                    TimeStamp = System.currentTimeMillis();
                    // Check and update tilt value plus LED after an infection
                    if (tiltchange == true) {
                        if (tilt == 1) {
                            leds.getLED(7).setColor(red);
                            leds.getLED(7).setOn();
                        } else if (tilt == -1) {
                            leds.getLED(7).setColor(blue);
                            leds.getLED(7).setOn();
                        } else {
                            leds.getLED(7).setColor(purple);    // in case tilt value is buggy
                            leds.getLED(7).setOn();
                        }
                    } else if (tiltchange == false) {
                        Xtilt = accel.getTiltX();   // get local tilt value
                        if (Xtilt > 0) {
                            tilt =1;
                            leds.getLED(7).setColor(red);
                            leds.getLED(7).setOn();
                        } else if (Xtilt < 0) {
                            tilt = -1;
                            leds.getLED(7).setColor(blue);
                            leds.getLED(7).setOn();
                        }
                    }
                        
                    
                            
                    count++;
                    if (count >= Long.MAX_VALUE) { count = 0; }
                    xdg.reset();
                    xdg.writeLong(myAddr); // own MAC address
                    xdg.writeLong(leader); // own leader's MAC address
                    xdg.writeLong(follower); // own follower's MAC address
                    xdg.writeLong(TimeStamp); // current timestamp
                    xdg.writeInt(tilt); //local or infected tilt
                    xdg.writeBoolean(tiltchange); //tiltchange flag if sw1 is pressed
                    xdg.writeBoolean(resetFlag);//resetflag
                    xdg.writeInt(power); // own power
                    xdg.writeLong(count); // local count
                    txConn.send(xdg);
                    long delay = (TimeStamp+ PACKET_INTERVAL- System.currentTimeMillis()) - 2;
                    if (delay > 0) {
                        pause(delay);
                    }
                }
            } catch (IOException ex) {
                // ignore
            } finally {
                if (txConn != null) {
                    try {
                        txConn.close();
                    } catch (IOException ex) { }
                }
            }
        }
    }
    
    /**
     * Loop to receive packets and discover peers information
     * (long)srcAddr,(long)srcLeader,(long)srcFollow,(long)srcTime,(int)srcSTEER,(int)srcSPEED
     * 
     * [TO DO]
     * sort out leader-follower by their MAC address order 
     * very most leader needs to know himself as the leader, then launch movement
     * very most follower needs to know himself as the last 
     */
    private void recvLoop () {
        ILed led = Spot.getInstance().getRedLed();
        RadiogramConnection rcvConn = null;
        recvDo = true;
        int nothing = 0;
        StartTime = System.currentTimeMillis(); // only executes once, needed to initialize the Start time
        while (recvDo) {
            try {
                rcvConn = (RadiogramConnection)Connector.open("radiogram://:" + BROADCAST_PORT);
                rcvConn.setTimeout(PACKET_INTERVAL - 5);
                Radiogram rdg = (Radiogram)rcvConn.newDatagram(rcvConn.getMaximumLength());
                long count = 0;
                boolean ledOn = false;
                while (recvDo) {
                    try {
                        rdg.reset();
                        rcvConn.receive(rdg);           // listen for a packet
                        led.setOn();
                        statusLED.setColor(green);
                        statusLED.setOn();
                        TimeStamp = System.currentTimeMillis();
                        long srcAddr = rdg.readLong(); // src MAC address
                        long srcLeader = rdg.readLong(); // src's leader
                        long srcFollow = rdg.readLong(); // src's follow
                        long srcTime = rdg.readLong(); // src's timestamp
                        int srcTilt = rdg.readInt(); // src's STEER
                        boolean srcTiltChange = rdg.readBoolean(); // src's SPEED
                        boolean srcResetFlag = rdg.readBoolean();//src's resetFlag
                        int pow = rdg.readInt();
                        
                        //String srcID = IEEEAddress.toDottedHex(srcAddr); 
                        //System.out.println("srcAddr is " + srcID);

                        // Discovery and store new motes in Array 1
                        for (int ii=1; ii < 5; ii++) {  // check for address in the local list
                            if (moteAddr[ii] == srcAddr) {
                                break;  // this ID already stored, skip the rest.
                            } else if (moteAddr[ii] == 0) {
                                moteAddr[ii] = srcAddr; // store this new address
                                moteCount++;
                                break;  // skip the rest of the array
                            }
                        }
//                        System.out.println("-----Mote Array-----");
//                        String zeroID = IEEEAddress.toDottedHex(moteAddr[0]);    // convert to MAC address in hex
//                        System.out.println("zero is " + zeroID);
//                        String firstID = IEEEAddress.toDottedHex(moteAddr[1]);    // convert to MAC address in hex
//                        System.out.println("first is " + firstID);
//                        String secondID = IEEEAddress.toDottedHex(moteAddr[2]);    // convert to MAC address in hex
//                        System.out.println("second is " + secondID);
//                        String thirdID = IEEEAddress.toDottedHex(moteAddr[3]);    // convert to MAC address in hex
//                        System.out.println("third is " + thirdID);
//                        String fourthID = IEEEAddress.toDottedHex(moteAddr[4]);    // convert to MAC address in hex
//                        System.out.println("fourth is " + fourthID);
                        
                         
                                                                        
                        // Check datagram for new infection or reset
                        if ((resetFlag == true || srcResetFlag == true) && srcTiltChange == false) {  
                            System.out.println("Infection check true for leader reset");
                            tiltchange = false;                     //get back to normal tilt
                        } else if (srcFollow == sortedAddr[mynum] && srcTiltChange == true) { //if this datagram is to me I should follow the action
                            System.out.println("Infection check true for myAddr");
                            tiltchange = true;             // follow the action from src
                            tilt = srcTilt;
                        } else {
                            System.out.println("Infection check default");
                        }
                        
                        // Show LED count for number of motes, only for Leader mote.
                        System.out.println("leader =" + leader);
                        if (myAddr == leader) {
                            for (int ii=1; ii <= moteCount; ii++) { // leader turn on LEDs for the total count of connected motes
                                leds.getLED(ii).setColor(white); 
                                leds.getLED(ii).setOn();
                            }
                            for (int ii=moteCount+1; ii <= 5; ii++) { // leader turns off the rest of the count LEDs
                                leds.getLED(ii).setOff();
                            } 
                        } else {
                            for (int ii=1; ii <= 5; ii++) { // turn off all count LEDs if not the leader
                                leds.getLED(ii).setOff();
                            }
                        }
                        
                        // 3 second interval update
                        tempTime = TimeStamp;   // Time is in milli seconds
                        if (tempTime - StartTime >= 3000) {
                            String followTemp = IEEEAddress.toDottedHex(srcFollow);
                            System.out.println("srcTiltChange = "+srcTiltChange+", srcTilt = "+srcTilt+", srcFollow = "+followTemp);
                            // Copy Array 1 into Array 2
                            System.arraycopy(moteAddr, 0, sortedAddr, 0, moteAddr.length);
                            // Sort Array 2 (bubble sort)
                            long temp;
                            for (int ii=0; ii<5; ii++) {
                                for (int jj=4; jj>ii; --jj) {
                                    if (sortedAddr[jj] < sortedAddr[jj-1]) {
                                         temp = sortedAddr[jj];
                                         sortedAddr[jj] = sortedAddr[jj-1];
                                         sortedAddr[jj-1] = temp;
                                    }
                                }
                            }
                            // Leader Election
                            leader = sortedAddr[4];
                            // find out where am I and define my follower
                            for (int ii=4; ii >= 0; ii--) {
                                if (myAddr == sortedAddr[ii]) {
                                    mynum = ii;
                                    if (ii>0 && sortedAddr[ii-1] != 0) {
                                        follower = sortedAddr[ii-1];
                                    } else {
                                        follower = leader;
                                    }
                                }
                            }
                            // Clear Array 1
                            for (int ii=1; ii < 5; ii++) {  // array 1 element 0 never changes, as it is this mote's address
                                moteAddr[ii] = 0;
                            }
                            moteCount = 1;  // reset mote counter to 1, accounting for self
                            StartTime = TimeStamp;  // set new start time
                            // Display the Leader's address
                            //String leaderID = IEEEAddress.toDottedHex(sortedAddr[4]);    // convert to MAC address in hex
                            //System.out.println("leader is " + leaderID);
                        }                         
//                        System.out.println("-----Sorted Array-----");
//                        String leaderID = IEEEAddress.toDottedHex(sortedAddr[4]);    // convert to MAC address in hex
//                        System.out.println("leader is " + leaderID);
//                        String follow1ID = IEEEAddress.toDottedHex(sortedAddr[3]);    // convert to MAC address in hex
//                        System.out.println("follow1 is " + follow1ID);
//                        String follow2ID = IEEEAddress.toDottedHex(sortedAddr[2]);    // convert to MAC address in hex
//                        System.out.println("follow2 is " + follow2ID);
//                        String follow3ID = IEEEAddress.toDottedHex(sortedAddr[1]);    // convert to MAC address in hex
//                        System.out.println("follow3 is " + follow3ID);
//                        String follow4ID = IEEEAddress.toDottedHex(sortedAddr[0]);    // convert to MAC address in hex
//                        System.out.println("follow4 is " + follow4ID);
                        // Display the timestamp
                        //System.out.println("timeStamp is " + TimeStamp + "\n");
                        nothing = 0;
                        //led.setOff();                            
                    } catch (TimeoutException ex) {        // timeout - display no packet received
                        statusLED.setColor(red);
                        statusLED.setOn();
                        nothing++;
                        System.out.println("nothing = "+ nothing);
                            if (nothing >= 6) {
                                leader= RadioFactory.getRadioPolicyManager().getIEEEAddress();//if I can not hear anything I become the leader
                                moteCount = 1;  // reset mote counter to 1, accounting for self
                                // Clear Array 1
                                for (int ii=0; ii < 4; ii++) {  // array 1 element 0 never changes, as it is this mote's address
                                    sortedAddr[ii] = 0;
                                    leds.getLED(ii+1).setOff();
                                }
                                leds.getLED(1).setColor(white); 
                                leds.getLED(1).setOn();
                                nothing = 0;
                            }
                        }
                    }
            } catch (IOException ex) {
                // ignore
            } finally {
                if (rcvConn != null) {
                    try {
                        rcvConn.close();
                    } catch (IOException ex) { }
                }
            }
        }
    }
    /**
     * For LED Display so even if only one alive the led display can be still right
     */
    private void LedDisplayLoop(){
        
    }
    
    /**
     * Pause for a specified time.
     *
     * @param time the number of milliseconds to pause
     */
    private void pause (long time) {
        try {
            Thread.currentThread().sleep(time);
        } catch (InterruptedException ex) { /* ignore */ }
    }

    /**
     * Initialize any needed variables.
     */
    private void initialize() { 
        myAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        moteAddr[0] = myAddr;
        statusLED.setColor(red);     // Red = not active
        statusLED.setOn();
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
    //    AODVManager rp = Spot.getInstance().
    }
    

    /**
     * Main application run loop.
     */
    private void run() {
        System.out.println("Radio Signal Strength Test (version " + VERSION + ")");
        System.out.println("Packet interval = " + PACKET_INTERVAL + " msec");
        
        new Thread() {
            public void run () {
                xmitLoop();
            }
        }.start();                      // spawn a thread to transmit packets
        new Thread() {
            public void run () {
                recvLoop();
            }
        }.start();
        new Thread() {
            public void run () {
                LedDisplayLoop();
            }
        }.start(); // spawn a thread to receive packets
        respondToSwitches();            // this thread will handle User input via switches
    }

    /**
     * Display a number (base 2) in LEDs 1-7
     *
     * @param val the number to display
     * @param col the color to display in LEDs
     */
    private void displayNumber(int val, LEDColor col) {
        for (int i = 0, mask = 1; i < 7; i++, mask <<= 1) {
            leds.getLED(7-i).setColor(col);
            leds.getLED(7-i).setOn((val & mask) != 0);
        }
    }

    /**
     * Loop waiting for user to press a switch.
     *<p>
     * Since ISwitch.waitForChange() doesn't really block we can loop on both switches ourself.
     *<p>
     * Detect when either switch is pressed by displaying the current value.
     * After 1 second, if it is still pressed start cycling through values every 0.5 seconds.
     * After cycling through 4 new values speed up the cycle time to every 0.3 seconds.
     * When cycle reaches the max value minus one revert to slower cycle speed.
     * Ignore other switch transitions for now.
     *
     */
    private void respondToSwitches() {
        while (true) {
            pause(100);    // check every 0.1 seconds
            if (sw1.isClosed()) {   // button pressed, start timing
                //pause(1000);    // wait 1.0 second
                pause(500);    // wait 0.5 second
                if (sw1.isClosed()) {   // second button press OR button held
                    // reset the tilt state of all motes in the system, to be local only
                    if (myAddr == leader && resetFlag == false) {
                        tiltchange = false;
                        resetFlag = true;
                        leds.getLED(6).setColor(red);
                        leds.getLED(6).setOn();
                        pause(1000);
                        resetFlag = false;
                        leds.getLED(6).setColor(green);
                        leds.getLED(6).setOn();
                    }
                } else {    // button not being pressed after 0.5 seconds
                    // any other mote will start a new infection
                    tiltchange = true;
                    sortedAddr[mynum] = 0;
                    leds.getLED(6).setColor(yellow);
                    leds.getLED(6).setOn();
                    try {
                        Xtilt = accel.getTiltX();   // get local tilt value
                    } catch (IOException ex) {
                        // ignored
                    }
                    // set the tilt change flag to true, then set tilt value
                    if (Xtilt > 0) {
                        tilt =1;
                        leds.getLED(7).setColor(red);
                        leds.getLED(7).setOn();
                    } else if(Xtilt < 0) {
                        tilt = -1;
                        leds.getLED(7).setColor(blue);
                        leds.getLED(7).setOn();
                    }
                }
                pause(1000);
                displayNumber(0, blue);
            }
            if (sw2.isClosed()) {
                int cnt = 0;
                ledsInUse = true;
                displayNumber(power, red);
                pause(1000);    // wait 1.0 second
                if (sw2.isClosed()) {
                    while (sw2.isClosed()) {
                        power++;
                        if (power > 30) { cnt = 0; }
                        if (power > 32) { power = 0; }
                        displayNumber(power, red);
                        cnt++;
                        pause(cnt < 5 ? 500 : 300);    // wait 0.5 second
                    }
                    Spot.getInstance().getRadioPolicyManager().setOutputPower(power - 32);
                }
                pause(1000);    // wait 1.0 second
                displayNumber(0, blue);
            }
            ledsInUse = false;
        }
    }
    
    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException {
	// Listen for downloads/commands over USB connection
	new com.sun.spot.service.BootloaderListenerService().getInstance().start();
        initialize();
        run();
    }

    /**
     * This will never be called by the Squawk VM.
     */
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     * @param unconditional If true the MIDlet must cleanup and release all resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

}