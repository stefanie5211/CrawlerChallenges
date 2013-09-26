/*
 * RadioStrength_BS.java
 *
 * Created on Nov 5, 2012 10:43:37 PM;
 */

package org.sunspotworld;

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiogram.*;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.util.IEEEAddress;

import javax.microedition.io.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.*;
import java.io.BufferedWriter;
import java.io.File;


/**
 * Sample Sun SPOT host application
 */
public class RadioStrength_BS {
    public static final String BROADCAST_PORT       = "151";
    private static final int RADIO_TEST_PACKET      = 42;
    private static final int PACKETS_PER_SECOND     = 5;
    private static final int PACKET_INTERVAL        = 1000 / PACKETS_PER_SECOND;
    
    /**
     * Print out our radio address.
     */
    public void run() {
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
         new Thread() {
            public void run () {
                startSendThread();
            }
        }.start();                     // spawn a thread to transmit packets
        
        new Thread() {
            public void run () {
                startReceiveThread();
            }
        }.start(); 
    }
    
    private void initialize() { 
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();       
        //int currentChannel = rpm.getChannelNumber();       
        //short currentPan = rpm.getPanId();       
        //int currentPower = rpm.getOutputPower(); 
        rpm.setChannelNumber(24);  // valid range is 11 to 26       rpm.setPanId((short) 6);       
        rpm.setOutputPower(0);  // valid range is -32 to 0 (for spots)      
    }
  
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    private void startReceiveThread(){
        RadiogramConnection dgConnection = null;
        Datagram dg = null;
        String result;              
        File writeFile = new File("Z:/EC544/Challenge5Matlab/DataFile.txt");  //create a new file to store data
        try {
            dgConnection = (RadiogramConnection) Connector.open("radiogram://:"+BROADCAST_PORT);
            dg = dgConnection.newDatagram(100);//connect the port and build the connection
        } catch (IOException e) {
            System.out.println("Can not open radiogram receiver connection");
            e.printStackTrace();
            return;
        }
        while(true) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");//设置日期格式
                dgConnection.receive(dg);
                if(dg.getAddress().equals("0014.4F01.0000.45BB"))
                {
                    result=df.format(new Date())+","+dg.readUTF();
                    System.out.println(result);
                    writeToFile(writeFile, result);
                }
            } catch(Exception e) {
                System.err.println(e.getMessage());
            }    
        }            
    }
    
    private void startSendThread() {
        RadiogramConnection txConn = null;
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();       
        rpm.setChannelNumber(24);
        while (true) {
            try {
                txConn = (RadiogramConnection)Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
                txConn.setMaxBroadcastHops(1);      // don't want packets being rebroadcasted
                Datagram xdg = txConn.newDatagram(txConn.getMaximumLength());
                long count = 0;
                while (true) {
                    //led.setOn();
                    long nextTime = System.currentTimeMillis() + PACKET_INTERVAL;
                    count++;
                    if (count >= Long.MAX_VALUE) { count = 0; }
                    xdg.reset();
                    xdg.writeByte(RADIO_TEST_PACKET);
                    //xdg.writeInt(power);
                    xdg.writeLong(count);
                    txConn.send(xdg);
                    //led.setOff();
               }
            } catch (IOException ex) {
                // ignore
            } finally {
                if (txConn != null) {
                    try {
                        txConn.close();
                    } catch (IOException ex) { /* ignore */ }
                }
            }
        }
    }
   
    public void writeToFile(File file, String message) throws Exception{
        try {
            BufferedWriter outb = new BufferedWriter(new FileWriter(file, true));
            outb.write(message);
            outb.newLine();
            outb.close();
        } catch (IOException e) {
            System.out.println("Write File Error");
        } catch (Exception e) {
            System.err.println("Caught " + e +  " while reading sensor samples.");
            throw e;
        }    
    }
    
    public static void main(String[] args) throws IOException, Exception {
        RadioStrength_BS app = new RadioStrength_BS();
        app.initialize();
        app.run();
    }
}
