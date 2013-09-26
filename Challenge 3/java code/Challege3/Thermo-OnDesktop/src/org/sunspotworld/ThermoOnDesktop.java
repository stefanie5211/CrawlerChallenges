	/*
 * ThermoOnDesktop.java
 *
 * Created on Sep 19, 2012 10:03:13 PM;
 */

package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.util.IEEEAddress;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import com.sun.spot.io.j2me.radiostream.*;
import com.sun.spot.io.j2me.radiogram.*;

import java.io.*;
import javax.microedition.io.*;

import com.sun.spot.client.DummySpotClientUI;
import com.sun.spot.client.SerialPortWrapper;
import java.text.DateFormat;
import java.util.Date;

/**
 * host app to collect data with Thermo-OnDesktop
 * @author Yuting Zhang <ytzhang@bu.edu>
 * @author Jing Wang
 */
public class ThermoOnDesktop {

    public static final String BROADCAST_PORT = "13";
    private long SPOTaddr;
    private double accelX, accelY, accelZ;
//    private double tiltX, tiltY, tiltZ;
    private double tempC, tempF;
    
    /**
     * Print out our radio address.
     */
    public void run() {
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
    }

    public void startReceiverThread() {
        
                DateFormat fmt = DateFormat.getTimeInstance();//get time

                RadiogramConnection dgConnection = null;
                Datagram dg = null;
                
                String splittime;
                String splitnewtime;
                        
                try {
                    dgConnection = (RadiogramConnection) Connector.open("radiogram://:"+BROADCAST_PORT);
                    dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
                } catch (IOException e) {
                    System.out.println("Can not open radiogram receiver connection");
                    e.printStackTrace();
                    return;
                }
                        	   
                
                               
               while(true){
                    try {
                        dg.reset();
                        dgConnection.receive(dg);
                        long time = dg.readLong();  //get time
                        SPOTaddr = dg.readLong();
                        //accelX = dg.readDouble();
                	//accelY = dg.readDouble();
                	//accelZ = dg.readDouble();
//                        tiltX = dg.readDouble();
//                        tiltY = dg.readDouble();
//                        tiltZ = dg.readDouble();
                	tempC = dg.readDouble();
                	tempF = dg.readDouble();
                	
                        System.out.println(fmt.format(new Date(time))+ IEEEAddress.toDottedHex(SPOTaddr) +
                        "   "  + tempC + "   " + tempF + "   ");
                        
                        splittime=fmt.format(new Date(time));
                        splitnewtime=splittime.replaceAll("\\s", "");

                        
	                        
		           try { 
			        File file = new File("/Users/XiaoxuShen/Documents/Matlab/new/SPOTAccelTempData2.txt");
			               //String port = "/dev/cu.usbmodem411";
			               //SerialPortWrapper pw = new SerialPortWrapper(port, new DummySpotClientUI(true));
			               //InputStream in = new SerialPortWrapper.NoTimeoutInputStream(pw.getInputStream());
			               //OutputStream out = pw.getOutputStream();  // in case we need to send characters to the SPOT
			        BufferedWriter outb = new BufferedWriter(new FileWriter(file, true));
			        outb.write("     [java] SunSPOT:"+dg.getAddress()+","+ time +","+tempC+","+tempF+",");
                           outb.newLine();
			        	   outb.close();
		               } catch (IOException e) {
		               	   System.out.println("Writel File Error1");
		               }

                    
                        /*
                        try {
 							   BufferedWriter out = new BufferedWriter(new FileWriter("~/Dropbox/SNETProj/FinalProjMedium/Matlab/res.txt", true));
    						   out.write("aString");
    						   out.close();
							} catch (IOException e) {
								System.out.println("Writel File Error");
							}
						*/
                        
                    } catch (IOException e) {
                        System.out.println("No datagram received");
                    }
                    //outb.close();
                }
    }
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) {
        ThermoOnDesktop app = new ThermoOnDesktop();
        app.run();
        app.startReceiverThread();
        System.exit(0);
    }
}
