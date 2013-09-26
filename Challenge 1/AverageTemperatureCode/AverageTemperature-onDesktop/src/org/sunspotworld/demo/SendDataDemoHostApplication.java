/*
 * SendDataDemoHostApplication.java
 *
 * Copyright (c) 2008-2009 Sun Microsystems, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.*;

import com.sun.spot.peripheral.ota.OTACommandServer;
import java.text.DateFormat;
import java.util.Date;
import javax.microedition.io.*;
import java.io.*;
import java.io.BufferedWriter;
import java.io.File;

/**
 * This application is the 'on Desktop' portion of the SendDataDemo. 
 * This host application collects sensor samples sent by the 'on SPOT'
 * portion running on neighboring SPOTs and just prints them out. 
 *   
 * @author: Vipul Gupta
 * modified: Ron Goldman
 */

public class SendDataDemoHostApplication {
    // Broadcast port on which we listen for sensor samples
    private static final int HOST_PORT = 67;
    private double val1=0, val2=0, val3=0; 
    private double averageTemp=0;
    boolean flag1 = false, flag2 = false, flag3 = false;
    java.io.File f1= new java.io.File("sensor_1.txt");
    java.io.File f2= new java.io.File("sensor_2.txt");
    java.io.File f3= new java.io.File("sensor_3.txt"); 
    java.io.File f4= new java.io.File("sensor_4.txt"); 
    private void run() throws Exception {
        RadiogramConnection rCon;
        Datagram dg;
        DateFormat fmt = DateFormat.getTimeInstance();
        
        try {
            // Open up a server-side broadcast radiogram connection
            // to listen for sensor readings being sent by different SPOTs
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + HOST_PORT);
            dg = rCon.newDatagram(rCon.getMaximumLength());
        } catch (Exception e) {
             System.err.println("setUp caught " + e.getMessage());
             throw e;
        }

        // Main data collection loop
        while (true) {
            try {
                // Read sensor sample received over the radio
                rCon.receive(dg);
                String addr = dg.getAddress();  // read sender's Id            
                long time = dg.readLong();      // read time of the reading
                double val = dg.readDouble();         // read the sensor value
                
                if ( addr.equals("0014.4F01.0000.359D"))
                {
                     val1 = val;
                     this.printData(1, val1, fmt.format(new Date(time))); 
                     this.writeToFile(f1, val1, fmt.format(new Date(time))); 
                     flag1 = true;
                }
                if ( addr.equals("0014.4F01.0000.45BB"))
                {
                    val2 = val;
                    this.printData(2, val2, fmt.format(new Date(time)));
                    this.writeToFile(f2, val2, fmt.format(new Date(time)));
                    flag2 = true; 
                }
                if ( addr.equals("0014.4F01.0000.437C"))
                {
                    val3 = val;
                    this.printData(3, val3, fmt.format(new Date(time)));
                    this.writeToFile(f3, val3, fmt.format(new Date(time)));
                    flag3 = true;
                }
                if ( flag1 == true && flag2 == true && flag3 == true)
                {
                    averageTemp = this.getAverageTemp(val1,val2,val3);
                    System.out.println( "average temperater is "+ String.format("%.2f", averageTemp)  +" F , time is " + fmt.format(new Date(time)));    
                    this.writeToFile(f4, averageTemp, fmt.format(new Date(time)));
                    flag1 = false;
                    flag2 = false;
                    flag3 = false;
                }
                
               /* System.out.println(fmt.format(new Date(time)) + "  from: " + addr + "   value = " + val);*/
                
                
                        
            } catch (Exception e) {
                System.err.println("Caught " + e +  " while reading sensor samples.");
                throw e;
            }             
        }
    }
    
    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        // register the application's name with the OTA Command server & start OTA running
        OTACommandServer.start("SendDataDemo");

        SendDataDemoHostApplication app = new SendDataDemoHostApplication();
        app.run();
    }
    public double getAverageTemp(double v1, double v2, double v3){
        double average;
        average = (v1 + v2 + v3)/3;
        return average;
    }
    public void printData(int number, double val, String time){
        System.out.println("mote " + number +" temperature is " + String.format("%.2f", val) + " F " + ", time is " + time);
    }
    public void writeToFile(File f, double val, String time) throws Exception{
        
        try {
                BufferedWriter output1 = new BufferedWriter(new FileWriter(f, true));
                output1.write(new Double(val).toString());
                output1.newLine();
                output1.write(time);
                output1.newLine();
                output1.close(); 
            }
            catch (Exception e) {
                System.err.println("Caught " + e +  " while reading sensor samples.");
                throw e;
            }    
        
    }
    public double getRealTemp(double sensorTemp, double m, double b){
        double realTemp;
        realTemp = m*sensorTemp + b;
        return realTemp;
    }
}
