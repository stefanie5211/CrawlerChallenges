/*
 * ThermoBroadcast.java
 *
 * Created on Sep 19, 2012 3:26:42 PM;
 */

package org.sunspotworld;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import com.sun.spot.peripheral.NoRouteException;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.Utils;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.resources.transducers.IAccelerometer3D;
import java.io.IOException;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * * This program collect acceleration and temperature data and Send them to base station. 
 * @author: Yuting Zhang <ytzhang@bu.edu>
 * @author Jing Wang
 */
 
public class ThermoBroadcast extends MIDlet {
	
	private IAccelerometer3D accel = (IAccelerometer3D) Resources.lookup(IAccelerometer3D.class);
	private ITriColorLEDArray leds = (ITriColorLEDArray)Resources.lookup(ITriColorLEDArray.class);
        private ITemperatureInput tempSensor = (ITemperatureInput)Resources.lookup(ITemperatureInput.class);
        
        public static final String BROADCAST_PORT = "13";
        
        private double accelX, accelY, accelZ;
        private double tiltX, tiltY, tiltZ;
        private double tempC, tempF;
	
	
	

	public void startSender () {
	          DatagramConnection dgConnection = null;
	          Datagram dg = null;
	          try {
	              // specify broadcast_port
	              dgConnection = (DatagramConnection) Connector.open("radiogram://broadcast:"+ BROADCAST_PORT);
	              
	              dg = dgConnection.newDatagram(100);
	              System.out.println("Maxleng for Packet is : " + dgConnection.getMaximumLength());
	          } catch (IOException ex) {
	              System.out.println("Could not open radiogram broadcast connection");
	              ex.printStackTrace();
	              return;
	          }
	          
	                   
	          /*
	          * Start to Collect the accel data and tilt angle data and send them
	          */
                  
                  
	          long ourAddr =  RadioFactory.getRadioPolicyManager().getIEEEAddress();
	          System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
	          
	          for (int i=0; i<leds.size();i++){
                      leds.getLED(i).setRGB(50, 50, 50);
                      leds.getLED(i).setOn();
                  }
                  for (int i=0;i<leds.size();i++){
                      leds.getLED(i).setOff();
                  }
	          
	          leds.setColor(LEDColor.BLUE);  // set them to be blue when lit     
		      int oldOffset = 3;
	          
	          while (true) {
	          	try {
                            long now = System.currentTimeMillis();
	           		//accelX = accel.getAccelX();
	           		//accelY = accel.getAccelY();
	           		//accelZ = accel.getAccelZ();
                                //tiltX = accel.getTiltX();
                                //tiltY = accel.getTiltY();
                                //tiltZ = accel.getTiltZ();
	           		tempC = tempSensor.getCelsius();
	           		tempF = tempSensor.getFahrenheit();
	           		
	           		
	           		// LED will show the tiltX
                            int tiltXAng = (int)Math.toDegrees(tiltX); // returns [-90, +90]
	                   int offset = -tiltXAng / 15;                
	                   if (offset < -3) offset = -3;
	                   if (offset > 3 ) offset =  3;
	                   if (oldOffset != offset) {
	                    leds.getLED(3 + oldOffset).setOff(); // clear display
	                    leds.getLED(4 + oldOffset).setOff();
	                    leds.getLED(3 + offset).setOn();     // use 2 LEDs to display "bubble""
	                    leds.getLED(4 + offset).setOn(); 
	                    oldOffset = offset;
	                   }
	           		
	           	
	              		// Write the string into the dataGram.
	           		// dg.writeLong(ourAddr);
	           		dg.reset();
                                dg.writeLong(now);
                                dg.writeLong(ourAddr);
                                //dg.writeLong(ourAddr);
	           		//dg.writeDouble(accelX);
	           		//dg.writeDouble(accelY);
	           		//dg.writeDouble(accelZ);
//                              dg.writeDouble(tiltX);
//	           		dg.writeDouble(tiltY);
//	           		dg.writeDouble(tiltZ);
	           		dg.writeDouble(tempC);
	           		dg.writeDouble(tempF);
	           		System.out.println( now + IEEEAddress.toDottedHex(ourAddr)+"," + tempC+","+tempF+",");
	           		
	       	   		//Send DataGram
		       		dgConnection.send(dg);
	           		
	           		// Sleep for 200 milliseconds.
	           		Utils.sleep(200); 
	          		
	           		} catch (Exception e) {
	          			System.err.println("Caught " + e + " while collecting/sending sensor sample.");
	          		}
	          }
	}


    
    protected void startApp() throws MIDletStateChangeException {
        
        System.out.println("basic telemetry with Accel & Temp starts");
        // Listen for downloads/commands over USB connection
        new com.sun.spot.service.BootloaderListenerService().getInstance().start();
        
        startSender();
    }
    
    protected void pauseApp() {
    }
    
    /**
     * Called if the MIDlet is terminated by the system.
     * I.e. if startApp throws any exception other than MIDletStateChangeException,
     * if the isolate running the MIDlet is killed with Isolate.exit(), or
     * if VM.stopVM() is called.
     * 
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     *    cleanup and release all resources. If false the MIDlet may throw
     *    MIDletStateChangeException  to indicate it does not want to be destroyed
     *    at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        leds.setOff();
    }
    
}
