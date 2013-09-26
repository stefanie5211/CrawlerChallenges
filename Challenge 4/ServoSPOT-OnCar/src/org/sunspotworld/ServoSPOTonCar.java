/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
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
 **/

package org.sunspotworld;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.IDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
//import com.sun.spot.resources.transducers.IAccelerometer3D;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;

import com.sun.spot.util.IEEEAddress;

import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.Utils;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import org.sunspotworld.common.Globals; //
import org.sunspotworld.common.TwoSidedArray; //
import org.sunspotworld.lib.BlinkenLights;
import org.sunspotworld.lib.LedUtils;
import org.sunspotworld.lib.RadioDataIOStream;


import java.io.IOException;
import java.lang.Thread.*;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

/**
 * This class is used to move a servo car consisting of two servos - one for
 * left wheel and the other for right wheel. To combine these servos properly,
 * this servo car moves forward/backward, turn right/left and rorate
 * clockwise/counterclockwise.
 * 
 * The current implementation has 3 modes and you can change these "moving mode" 
 * by pressing sw1. Mode 1 is "Normal" mode moving the car according to the tilt 
 * of the remote controller. Mode 2 is "Reverse" mode moving the car in 
 * a direction opposite to Mode 1. Mode 3 is "Rotation" mode only rotating the
 * car clockwise or counterclockwise according to the tilt.
 * 
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class ServoSPOTonCar extends MIDlet implements ISwitchListener {
    private static final int SERVO_CENTER_VALUE = 1500;
//    private static final int SERVO_MAX_VALUE = 2000;
//    private static final int SERVO_MIN_VALUE = 1000;
    private static final int SERVO1_MAX_VALUE = 2000;
    private static final int SERVO1_MIN_VALUE = 1000;
    private static final int SERVO2_MAX_VALUE = 2000;
    private static final int SERVO2_MIN_VALUE = 1350;
//    private static final int SERVO_HIGH = 500;
//    private static final int SERVO_LOW = 300;
    private static final int SERVO1_HIGH = 20; //steering step high
    private static final int SERVO1_LOW = 10; //steering step low
    private static final int SERVO2_HIGH = 10; //speeding step high
    private static final int SERVO2_LOW = 20; //speeding step low
    private static final int PROG0 = 0; //default program
    private static final int PROG1 = 1; // reversed program
//    private static final int PROG2 = 2;
    // devices
    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private ISwitch sw1 = eDemo.getSwitches()[EDemoBoard.SW1];
    private ISwitch sw2 = eDemo.getSwitches()[EDemoBoard.SW2];
    private ITriColorLED[] leds = eDemo.getLEDs();
    private ITriColorLEDArray myLEDs = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    
    // 1st servo for left & right direction 
    private Servo servo1 = new Servo(eDemo.getOutputPins()[EDemoBoard.H1]);
    // 2nd servo for forward & backward direction
    private Servo servo2 = new Servo(eDemo.getOutputPins()[EDemoBoard.H0]);
    private BlinkenLights progBlinker = new BlinkenLights(0, 3);
    private BlinkenLights velocityBlinker = new BlinkenLights(4, 7);
    private int current1 = SERVO_CENTER_VALUE;
    private int current2 = 1400;
    private int step1 = SERVO1_LOW;
    private int step2 = SERVO2_LOW;
    private int program = PROG0;
    private IAnalogInput proximity1 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput proximity2 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A1];
    IAnalogInput Usensor1 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A2];
    IAnalogInput Usensor2 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A3];
//    private int servo1ForwardValue;
//    private int servo2ForwardValue;
    private int servo1Left = SERVO_CENTER_VALUE +SERVO1_LOW;
    private int servo1Right = SERVO_CENTER_VALUE -SERVO1_LOW;
    private int servo2Forward = SERVO_CENTER_VALUE +SERVO2_LOW;
    private int servo2Back = SERVO_CENTER_VALUE - SERVO2_LOW;
    private double valforward;
    private double valback;
    private double valleft;
    private double valright;
    
    
    //pid control parameter
    private double dt = 0.01;
    private double Kp = 0.4;
    private double Kd = 0.01;
    private double Ki = 0.005;
    private double MAX = 4;
    private double MIN = -4;
    private double epsilon = 0.01;
    
    private double error;
    private double derivative;
    private double output;
    private double pre_error=0;
    private double integral =0;
    private double distance_forward;
    private double stoppoint = 5;
    private int step_new;
    
    
    
    
    
    private void run(){
        servo1.setValue(SERVO_CENTER_VALUE);
    new Thread(){
        public void run(){
                try {
                    UltraSensor1Loop();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
        }
    }.start();
            
    new Thread(){
        public void run(){
            MotorOutputLoop();
        }
    }.start();
    
    new Thread(){
        public void run(){
            ServoOutputLoop();
        }
    }.start();
    
    new Thread(){
        public void run(){
            UltraSensor2Loop();
        }
    }.start();
    
    new Thread(){
        public void run(){
            Proximity1Loop();
        }
    }.start();
        
    new Thread(){
        public void run(){
            Proximity2Loop();
        }
    }.start();
    }
    /** BASIC STARTUP CODE **/
    protected void startApp() throws MIDletStateChangeException { 
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        //ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "SW1");
        //ITriColorLED led = leds.getLED(0);
        //led.setRGB(100,0,0);        
        System.out.println("Hello, world");
        //BootloaderListenerService.getInstance().start();  
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setColor(LEDColor.GREEN);
                        myLEDs.getLED(i).setOn();
                    }
        Utils.sleep(500);
        for (int i = 0; i < myLEDs.size(); i++) {
                        myLEDs.getLED(i).setOff(); 
                    }
        setServoForwardValue();
        progBlinker.startPsilon();
        velocityBlinker.startPsilon();
        // timeout 1000
       /* TwoSidedArray robot = new TwoSidedArray(getAppProperty("buddyAddress"), Globals.READ_TIMEOUT);
        try {
            robot.startInput();
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        sw1.addISwitchListener(this);
        sw2.addISwitchListener(this);

        velocityBlinker.setColor(LEDColor.BLUE);
        progBlinker.setColor(LEDColor.BLUE);
        boolean error = false;
        /*Creat new threads here*/
        run();
}
    
    private void UltraSensor1Loop() throws InterruptedException{
        while(true){
            try{
                double temp = Usensor1.getVoltage();
                valforward=temp*1000/9.765625*2.54;
                Utils.sleep(100);
                //System.out.println(val1);
            }
            catch(Exception ex){
                //ignore
            }
            finally{
            }
        }
    }
    
    private void UltraSensor2Loop(){
        while(true){
            try{
                double temp = Usensor2.getVoltage();
                valback=temp*1000/9.765625*2.54;
                Utils.sleep(100);
            }
            catch(Exception ex){
                //ignore
            }
            finally{
            }
        }
    }
    
    private void Proximity1Loop(){
        while(true){
            try{
                double temp = proximity1.getVoltage();
                //System.out.println("Output voltage = "+vol+" V");
                valleft = 18.67/(temp+0.167);
                //System.out.println("Distance = "+dis+" cm");
            }
            catch(Exception ex){
                //ignore
            }
            finally{
            }
        }
    }
    
    private void Proximity2Loop(){
        while(true){
            try{
                double temp = proximity2.getVoltage();
                //System.out.println("Output voltage = "+vol+" V");
                valright = 18.67/(temp+0.167);
                //System.out.println("Distance = "+dis+" cm");
            }
            catch(Exception ex){
                //ignore
            }
            finally{
            }
        }
    }
    
    private void MotorOutputLoop(){
        while(true){
            try{
                System.out.println("forward distance: "+valforward);
                System.out.println("backword distance: "+valback);
                System.out.println("left distance: "+valleft);
                System.out.println("right distance: "+valright);
                System.out.println("PID:"+Math.abs(PIDcal(60,valforward)));
                step_new=(int)Math.abs(PIDcal(60,valforward));
                if(valforward>120)
                {
                        System.out.println(valforward+" so forward");
                        //servo2.setValue(5);
                        forward();
                        Utils.sleep(100);
                }
                else
                {
                    if(valforward>80)
                    {
                        System.out.println(valforward+" so slow down");
                        //servo2.setValue(4);
                        forward();
                        Utils.sleep(100);
                    }
                    else
                    {
                        System.out.println(valforward+" so stop");
                        stop();
                        Utils.sleep(100);
                    }
                }
            }
            catch(Exception ex){
                //ignore
            }
            finally{
            }
        }
    }
    
    private void ServoOutputLoop(){
        while(true){
            try{
                if(Math.abs(valleft-valright)>10)
                {
                   if(valleft>valright)
                   {
                       servo1.setValue(1750);
                   }
                   else
                   {
                       servo1.setValue(1250);
                   }
                }
                else
                {
                    current1=servo1.getValue();
                    if(Math.abs(current1-SERVO_CENTER_VALUE)<10)
                    {
                    }
                    else
                    {
                    if(current1>SERVO_CENTER_VALUE)
                    {
                        servo1.setValue(SERVO_CENTER_VALUE);
                    }
                    else
                    {
                        servo1.setValue(SERVO_CENTER_VALUE);
                    }
                }
            }
            }
            catch(Exception ex){
                //ignore
            }
            finally{
            }
        }
    }
    
    private void setServoForwardValue(){
        servo1Left = current1 + step1;
        servo1Right = current1 - step1;
        servo2Forward = current2 + step2;
        
        servo2Back = current2 - step2;
        if (step2 == SERVO2_HIGH) {
            velocityBlinker.setColor(LEDColor.GREEN);
        } else {
            velocityBlinker.setColor(LEDColor.BLUE);
        }
    }
    
    //pid control
       private double PIDcal (double setpoint, double actual_position)
    {
        error = setpoint - actual_position;
        
        	//In case of error too small then stop intergration
	if(Math.abs(error) > epsilon)
	{
		integral = integral + error*dt;
	}
        
        derivative = (error - pre_error)/dt;
        output = Kp*error + Ki*integral + Kd*derivative;
        
        //Saturation Filter
	/*if(output > MAX)
	{
		output = MAX;
	}
	else if(output < MIN)
	{
		output = MIN;
	}*/
        //Update error
        pre_error = error;
        
        return output;
    }
       
    // normal mode
    private void program0(int xtilt, int ytilt) {
       if (ytilt > 40) {
            forward();
        } else if (ytilt < -40) {
            backward();
        } else if (xtilt > 40) {
            right();
        } else if (xtilt < -40) {
            left();
        } else {
            stop();
        }     
    }

    // reverse mode
    private void program1(int xtilt, int ytilt) {
        
        
        if (ytilt > 40) {
            backward();
        } else if (ytilt < -40) {
            forward();
        } else if (xtilt > 40) {
            left();
        } else if (xtilt < -40) {
            right();
        } else {
            stop();
        }
    }

  /*  private void program2(int xtilt, int ytilt) {
        if (xtilt > 40) {
            rightRotation();
        } else if (xtilt < -40) {
            leftRotation();
        } else {
            stop();
        }
    }*/

    private void left() {
        System.out.println("left");
        current1 = servo1.getValue();
        if (current1 + step1 < SERVO1_MAX_VALUE){
        servo1.setValue(current1+step1);
        Utils.sleep(50);
        } else{
        servo1.setValue(SERVO1_MAX_VALUE);
        Utils.sleep(50);
        }
 //       servo2.setValue(0);
    }

    private void right() {
        System.out.println("right");
        current1 = servo1.getValue();
        if (current1-step1 > SERVO1_MIN_VALUE){
        servo1.setValue(current1-step1);
        Utils.sleep(50);
        } else{
            servo1.setValue(SERVO1_MIN_VALUE);
            Utils.sleep(50);
        }
        
 //       servo2.setValue(0);
    }

    private void stop() {
//      System.out.println("stop");
   //   servo1.setValue(0);
        servo2.setValue(SERVO_CENTER_VALUE);
    }

    private void backward() {
        System.out.println("forward");
    //    servo1.setValue(0);
        current2= servo2.getValue();

        for (int i=0;i<3;i++){
            current2= servo2.getValue();
        if (current2 + step2 <SERVO2_MAX_VALUE){
            servo2.setValue(current2+step2);
            Utils.sleep(50);
        }else{
        servo2.setValue(SERVO2_MAX_VALUE);
        Utils.sleep(50);
        }
        }
            
  /*     while(current2 + step2 <SERVO2_MAX_VALUE){
        
         servo2.setValue(current2+step2);
         current2= servo2.getValue();
        Utils.sleep(50);
         
}*/
    }

    private void forward() {
    //    System.out.println("backward");
  //      servo1.setValue(0);  
        current2 = servo2.getValue();
        System.out.println("the speed is" +  current2);
       
        for (int i=0;i<3;i++){
        current2 = servo2.getValue();
        if (current2 -step_new>SERVO2_MIN_VALUE){
        servo2.setValue(current2-step_new);
        Utils.sleep(10);
        }else{
        servo2.setValue(SERVO2_MIN_VALUE);
        Utils.sleep(10);
        }
        }//new pid control
        /*for (int i=0;i<3;i++){
        current2 = servo2.getValue();
        if (current2 -step2>SERVO2_MIN_VALUE){
        servo2.setValue(current2-step2);
        Utils.sleep(10);
        }else{
        servo2.setValue(SERVO2_MIN_VALUE);
        Utils.sleep(10);
        }
        }*/
        
    /*  while (current2 - step2 > SERVO2_MIN_VALUE){
        servo2.setValue(current2-step2);
        current2 = servo2.getValue();
        Utils.sleep(50);
                }*/
         
        
    }

  /*  private void leftRotation() {
        System.out.println("leftRotation");
        servo1.setValue(servo1Left);
        servo2.setValue(servo2Forward);
    }*/

 /*   private void rightRotation() {
        System.out.println("rightRotation");
        servo1.setValue(servo1Right);
        servo2.setValue(servo2Back);
    }*/

    public void switchPressed(SwitchEvent sw) {
        System.out.println("SW Pressed" + sw);
        if (sw.getSwitch() == sw1) { // for program change
            if (++program > PROG1) {
                program = PROG0;
            }
            switch (program) {
                case PROG0:
                    progBlinker.setColor(LEDColor.BLUE);
                    break;
                case PROG1:
                    progBlinker.setColor(LEDColor.GREEN);
                    break;
/*                case PROG2:
                    progBlinker.setColor(LEDColor.YELLOW);
                    break;*/
            }
        } else if (sw.getSwitch() == sw2) { // for velocity change
            step1 = (step1 == SERVO1_HIGH) ? SERVO1_LOW : SERVO1_HIGH;
            step2 = (step2 == SERVO2_HIGH) ? SERVO2_LOW : SERVO2_HIGH;
            setServoForwardValue();
        }
    }

    public void switchReleased(SwitchEvent sw) {
    // do nothing
    }
    
    protected void pauseApp() {
        // This will never be called by the Squawk VM
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
        for (int i = 0; i < myLEDs.size(); i++) {
            myLEDs.getLED(i).setOff();
        }
    }
}

