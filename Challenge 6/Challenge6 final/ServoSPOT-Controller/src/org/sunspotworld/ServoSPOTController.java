/*
 * ServoSPOTController.java
 *
 * Created on Jul 19, 2012 9:46:39 PM;
 */

package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.transducers.IAccelerometer3D;

import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.lib.BlinkenLights;
import org.sunspotworld.lib.LedUtils;
import org.sunspotworld.common.Globals; //
import org.sunspotworld.common.TwoSidedArray;

import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import com.sun.spot.peripheral.Spot;





/**
 * This class is used to control a servo car remotely. This sends values
 * measured by demoboard accelerometer to the servo car.
 * 
 * You must specify buddyAddress, that is the SPOT address on the car to
 * communicate each other.
 * 
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class ServoSPOTController extends MIDlet implements ISwitchListener{
    
    private static final String VERSION = "1.0";
    private static final int INITIAL_CHANNEL_NUMBER = 24;//IProprietaryRadio.DEFAULT_CHANNEL;
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String BROADCAST_PORT      = "151";
    private static final int RADIO_TEST_PACKET      = 42;
    private static final int PACKETS_PER_SECOND     = 5;
    private static final int PACKET_INTERVAL        = 1000 / PACKETS_PER_SECOND;
    private int channel = INITIAL_CHANNEL_NUMBER;
    private int power = 32;         // Start with max transmit power

    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private IAccelerometer3D accel = (IAccelerometer3D)Resources.lookup(IAccelerometer3D.class);
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ISwitch sw1 = eDemo.getSwitches()[EDemoBoard.SW1];
    private ISwitch sw2 = eDemo.getSwitches()[EDemoBoard.SW2];
    
    protected void startApp() throws MIDletStateChangeException {
        System.out.println("Hello, world");
        BootloaderListenerService.getInstance().start();  
        
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setColor(LEDColor.GREEN);
                        myLEDs.getLED(i).setOn();
                    }
        Utils.sleep(500);
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setOff(); 
                    }
        
        BlinkenLights blinker = new BlinkenLights();
        blinker.startPsilon();
        
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);
        
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);

        String buddyAddress = getAppProperty("buddyAddress");
        if (buddyAddress == null) {
            throw new RuntimeException("the property buddyAddress must be set in the manifest");
        }
        TwoSidedArray controller = new TwoSidedArray(buddyAddress);

        try {
            controller.startOutput();
         
        //    accel.setRestOffsets();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        blinker.setColor(LEDColor.BLUE);
        while (true) {
            try {
                controller.setVal(0, (int) (accel.getTiltX() * 100));
                controller.setVal(1, (int) (accel.getTiltY() * 100));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Utils.sleep(20);
        }
    }
    
     public void switchPressed(SwitchEvent sw) {
        System.out.println("SW Pressed" + sw);
        //create connection
        RadiogramConnection txConn = null;
        try
        {
        txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
        Datagram xdg = txConn.newDatagram(txConn.getMaximumLength()); //create new datagram
        xdg.reset();
        if (sw.getSwitch() == sw1) { // for program change
            System.out.println("sw1");
            xdg.reset();
            xdg.writeInt(1);
            txConn.send(xdg);
        }else if(sw.getSwitch() == sw2)
        {
            xdg.reset();
            xdg.writeInt(2);
            txConn.send(xdg);
            System.out.println("sw2");
        }else
        {
            xdg.reset();
            System.out.println("0");
        }
        }catch(IOException ex)
        {
            System.out.println(ex.getMessage());
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    public void switchReleased(SwitchEvent evt) {
    }
}
   // protected void pauseApp() {
        // This is not currently called by the Squawk VM
   // }

    /**
     * Called if the MIDlet is terminated by the system.
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true the MIDlet must cleanup and release all resources.
     */
