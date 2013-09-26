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

import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IAnalogInput;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.SwitchEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.Servo;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.lib.BlinkenLights;
import com.sun.spot.resources.transducers.ILightSensor;
import java.util.Random; //package to get a random value
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
/**
 * This class is used to move a servo car consisting of two servos - one for
 * left wheel and the other for right wheel. To combine these servos properly,
 * this servo car moves forward/backward, turn right/left and rotate
 * clockwise/counterclockwise.2
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
    private static final int SERVO_CENTER_VALUE = 1300;
    //private static final int SERVO_MAX_VALUE = 2000;
    //private static final int SERVO_MIN_VALUE = 1000;
    private static final int SERVO1_MAX_VALUE = 2000;
    private static final int SERVO1_MIN_VALUE = 1000;
    private static final int SERVO2_MAX_VALUE = 1400;
    private static final int SERVO2_MIN_VALUE = 1130;
    //private static final int SERVO_HIGH = 500;
    //private static final int SERVO_LOW = 300;
    private static final int SERVO1_HIGH = 20; //steering step high
    private static final int SERVO1_LOW = 150; //steering step low
    private static final int SERVO2_HIGH = 10; //speeding step high
    private static final int SERVO2_LOW = 20; //speeding step low
    private static final int PROG0 = 0; //default program
    private static final int PROG1 = 1; // reversed program
    private static final int TuringValue=200;//steer left when lightlevel is greater than this value
    //private static final int PROG2 = 2;
    // Devices
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
    private int current2 = SERVO_CENTER_VALUE;
    private int step1 = SERVO1_LOW;
    private int step2 = SERVO2_LOW;
    private int program = PROG0;
    private IAnalogInput proximity1 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A0];
    private IAnalogInput proximity2 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A1];
    IAnalogInput ForwardSensor = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A2];
    IAnalogInput Usensor2 = EDemoBoard.getInstance().getAnalogInputs()[EDemoBoard.A3];
    //light sensor
    ILightSensor lightSensor = (ILightSensor) Resources.lookup(ILightSensor.class);
    //private int servo1ForwardValue;
    //private int servo2ForwardValue;
    private int servo1Left = SERVO_CENTER_VALUE +SERVO1_LOW;
    private int servo1Right = SERVO_CENTER_VALUE -SERVO1_LOW;
    private int servo2Forward = SERVO_CENTER_VALUE +SERVO2_LOW;
    private int servo2Back = SERVO_CENTER_VALUE - SERVO2_LOW;
    private double valforward;
    private double valleft;
    private double valright;
    private int lightlevel;
    
    //pid control parameter
    private double dt = 0.01;
    private double Kp = 0.5;
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
    private double p1 = -10.76;
    private double p2 = 67.71;
    private double p3 = -163.9;
    private double p4 = 169.3;
    private double[] leftraw =new double [5];
    private double[] rightraw=new double [5];
    private double[] frontraw=new double [5];
    
    private static final String VERSION = "1.0";
    private static final int INITIAL_CHANNEL_NUMBER = 24;//IProprietaryRadio.DEFAULT_CHANNEL;
    private static final short PAN_ID               = IRadioPolicyManager.DEFAULT_PAN_ID;
    private static final String BROADCAST_PORT      = "151";
    private static final int RADIO_TEST_PACKET      = 42;
    private static final int PACKETS_PER_SECOND     = 5;
    private static final int PACKET_INTERVAL        = 1000 / PACKETS_PER_SECOND;
    private static final int  forwardDistance = 50;
    private static final int DistanceFromWall = 60;
    private int channel = INITIAL_CHANNEL_NUMBER;
    private int power = 32;         // Start with max transmit power
    
    private void run() {
        /*new Thread() {  // Ultrasonic Sensor Thread
            public void run() {
                try {
                    TrigerListenerLoop();  //get triger value     
                } catch (InterruptedException ex) {
                    System.out.println("TrigerListenerLoop Exception: "+ex);
                }
            }
        }.start();*/
        
        new Thread() {  // Ultrasonic Sensor Thread
            public void run() {
                try {
                    ProximityFrontLoop();  //get ultra sensor value       
                } catch (InterruptedException ex) {
                    System.out.println("ProximityFrontLoop Exception: "+ex);
                }
            }
        }.start();   

        new Thread() {  // Proximity1 Sensor Thread
            public void run() {
                try {
                    Proximity1Loop();  // Get 
                } catch (InterruptedException ex) {
                    System.out.println("Proximity1Loop Exception: "+ex);
                }
            }
        }.start(); 

        new Thread() {  // Proximity2 Sensor Thread
            public void run() {
                try {
                    Proximity2Loop();  // 
                } catch (InterruptedException ex) {
                    System.out.println("Proximity2Loop Exception: "+ex);
                }
            }
        }.start(); 

        new Thread() {  // Motor Output Thread
            public void run() {
                MotorOutputLoop_new(); // 
            }
        }.start();

        new Thread() {  // Servo Output Thread
            public void run() {
                ServoOutputLoop_new();  // 
            }
        }.start();
        
        new Thread() {  // light sensor Thread
            public void run() {
                try {
                    LightSensorLoop();  //
                } catch (InterruptedException ex) {
                }
            }
        }.start();
    }
    
    /** BASIC STARTUP CODE **/
    protected void startApp() throws MIDletStateChangeException {
        BootloaderListenerService.getInstance().start();   // monitor the USB (if connected) and recognize commands from host
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        
        IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
        rpm.setChannelNumber(channel);
        rpm.setPanId(PAN_ID);
        rpm.setOutputPower(power - 32);
        
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
        //setServoForwardValue();
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
        
        servo1.setValue(1500);
        servo2.setValue(SERVO_CENTER_VALUE);
        run();  //enter the main control function
    }
    
    private void TrigerListenerLoop() throws InterruptedException {
        RadiogramConnection rcvConn = null;
        try {
                rcvConn = (RadiogramConnection)Connector.open("radiogram://:" + BROADCAST_PORT);
                Radiogram rdg = (Radiogram)rcvConn.newDatagram(rcvConn.getMaximumLength());
                while(true)
                {
                    try{
                System.out.println("try to receive");
                rdg.reset();
                rcvConn.receive(rdg);           // listen for a packet
                System.out.print("have received");
                if(rdg.readInt()==1)
                {
                    System.out.println("Keep going!");
                }
                else if(rdg.readInt()==2)
                {
                    stop();
                    System.out.println("Stop!");
                }
                Utils.sleep(100);
                    }
                    catch
                         (TimeoutException tex)
                    {}
                }
        }
        catch(IOException ex)
        {
            System.out.println(ex.getMessage());
        }
    }
    
    // Forward-facing sensor
    private void ProximityFrontLoop() throws InterruptedException {
        while(true) {
            try {
                double temp = ForwardSensor.getVoltage();
                //calculate the forward distance
                 valforward = (0.7595 / (temp -0.1443 ))*30.5;
                 if(valforward<=0)
                 {
                     valforward = 200;
                 }
                //valforward = temp*1000/9.765625*2.54;
                //refresh the array
                /*frontraw[0]=frontraw[1];
                frontraw[1]=frontraw[2];
                frontraw[2]=frontraw[3];
                frontraw[3]=frontraw[4];
                frontraw[4]=temp;
                //calculate the average as the forward distance
                valforward=(frontraw[0]+frontraw[1]+frontraw[2]+frontraw[3]+frontraw[4])/5;*/
                //System.out.println("forward distance="+valforward+"cm");
                Utils.sleep(100);
            } catch (Exception ex) {
                //ignore
            } finally {
                // successfully get a value
            }
        }
    }
    
    // Light Sensor
    private void LightSensorLoop() throws InterruptedException {
        while(true) {
            try {
                lightlevel = lightSensor.getAverageValue();     // average in case fluorescent light
                System.out.println("LightSensor.getValue() = " + lightlevel);
                Utils.sleep(2000);
            } catch (Exception ex) {
                //ignore
            } finally {
                // successfully get a value
            }
        }
    }
    
    private void Proximity1Loop() throws InterruptedException {
        while(true) {
            try {
                double temp = proximity1.getVoltage();
                temp  = p1*(pow(temp, 3))+ p2*(pow(temp, 2))+ p3*temp +p4;
                //refresh the array
                leftraw[0]=leftraw[1];
                leftraw[1]=leftraw[2];
                leftraw[2]=leftraw[3];
                leftraw[3]=leftraw[4];
                leftraw[4]=temp;
                //calculate the average as the left distance
                valleft=(leftraw[0]+leftraw[1]+leftraw[2]+leftraw[3]+leftraw[4])/5;
                //System.out.println("left distance="+valleft+"cm");
                Utils.sleep(50);
            } catch (Exception ex) {
                //ignore
            } finally {
                // successfully get a value
            }
        }
    }
    
    private void Proximity2Loop() throws InterruptedException {
        while(true) {
            try {
                double temp = proximity2.getVoltage();
                temp  = p1*(pow(temp, 3))+ p2*(pow(temp, 2))+ p3*temp +p4;
                //refresh the array
                rightraw[0]=rightraw[1];
                rightraw[1]=rightraw[2];
                rightraw[2]=rightraw[3];
                rightraw[3]=rightraw[4];
                rightraw[4]=temp;
                //calculate the average as the forward distance
                valright=(rightraw[0]+rightraw[1]+rightraw[2]+rightraw[3]+rightraw[4])/5;
                //System.out.println("right distance="+valright+"cm");
                Utils.sleep(50);
            } catch (Exception ex) {
                //ignore
            } finally {
                // successfully get a value
            }
        }
    }
    
    public double pow(double x, double y)
    {
        int den = 1024; //declare the denominator to be 1024  
        /*Conveniently 2^10=1024, so taking the square root 10  
        times will yield our estimate for n.¡¡In our example  
        n^3=8^2n^1024 = 8^683.*/
        int num = (int)(y*den); // declare numerator
        int iterations;
        iterations = 10;
        double n = Double.MAX_VALUE; /* we initialize our
         * estimate, setting it to max*/
        while( n >= Double.MAX_VALUE && iterations > 1)
        {
            /*¡¡We try to set our estimate equal to the right
             * hand side of the equation (e.g., 8^2048).¡¡If this
             * number is too large, we will have to rescale. */
            n = x;
            for( int i=1; i < num; i++ )n*=x;
            /*here, we handle the condition where our starting
             * point is too large*/
            if( n >= Double.MAX_VALUE )
            {
                iterations--;
                den = (int)(den / 2);
                num = (int)(y*den); //redefine the numerator
            }
        }
         /*************************************************  
         ** We now have an appropriately sized right-hand-side.  
         ** Starting with this estimate for n, we proceed.  
         **************************************************/
        for( int i = 0; i < iterations; i++ )
        {
            n = Math.sqrt(n);
        }
        // Return our estimate
        return n;
    }
    
    // Rotate Motor to go Forward
    private void MotorOutputLoop_new() {
        while(true) {
             System.out.println(valforward);
            /*if(valforward>forwardDistance) {
               
                //forward();
                //servo2.setValue(1400);
                forward();
                Utils.sleep(100);
            } else if(valforward<=forwardDistance){
                stop();
                Utils.sleep(100);
            }*/
            forward();
            Utils.sleep(100);
        }
    }
    
    // Steering Left and Right using PID Controller
    private void ServoOutputLoop_new() {
        while(true) {
            //step1=GetRandomStep();
            //System.out.println("RandomStep="+step1+", leftdistance="+valleft+"cm,rightdistance="+valright+"cm");
            try {
                if(lightlevel>TuringValue)
                {
                    //Turn left
                    System.out.println("lightlevel"+lightlevel);
                    servo1.setValue(SERVO1_MAX_VALUE);
                    //left_new();
                }
                else{
                    //System.out.println("left value"+valleft);
                    //System.out.println("right value"+valright);
                    //when the car is not approaching the beacon
                if ((valleft<DistanceFromWall)&&(valright>DistanceFromWall)) {
                    right_new(200);
                }
                else if ((valleft>DistanceFromWall)&&(valright<DistanceFromWall)) {
                    left_new(200);
                }
                else if ((valleft<DistanceFromWall)&&(valright<DistanceFromWall)) {
                     if(valleft>valright) {  
                         left_new(step1); 
                     } else {
                         right_new(step1);
                     }
                }
                else {
                    servo1.setValue(1500);
                    //System.out.println("go straight");
                }
                }
                //make steering decision tenth a second
                Utils.sleep(50);
            } catch (Exception ex) {
                //ignore
            } finally {
                //
            }
        }
    }
    
    private void setServoForwardValue() {  //initialize the value of the car
        servo1Left = current1;
        servo1Right = current1 ;
        servo2Forward = current2;        
        servo2Back = current2 ;
        if (step2 == SERVO2_HIGH) {
            velocityBlinker.setColor(LEDColor.GREEN);
        } else {
            velocityBlinker.setColor(LEDColor.BLUE);
        }
    }
    
    // PID Controller
    private double PIDcal (double setpoint, double actual_position) {
        error = setpoint - actual_position;
        //System.out.println("actualpoint: "+actual_position+"\nerror: "+error);
        //In case of error too small then stop intergration
	if(Math.abs(error) > epsilon) {
		integral = integral + error*dt;
	}
        //System.out.println("integral: "+integral);
        derivative = (error - pre_error)/dt;
        output = Kp*error + Ki*integral + Kd*derivative;
        //System.out.println("derivative: "+derivative);
        pre_error = error;
        //System.out.println("output: "+output);
        return output;
    }

    // Left Steering value
    private void left_new(int step) {
        //System.out.println("left");
        current1 = servo1.getValue();
        if (current1 + step1 < SERVO1_MAX_VALUE){        
            servo1.setValue(current1+step1);
            //System.out.println("left value now"+servo1.getValue());
            Utils.sleep(50);
        } else {
            servo1.setValue(SERVO1_MAX_VALUE);
            Utils.sleep(50);
        }
    }
    
    // Right Steering value
    private void right_new(int step) {
        //System.out.println("right");
        current1 = servo1.getValue();
        if (current1-step1> SERVO1_MIN_VALUE){
            servo1.setValue(current1-step1);
            //System.out.println("right value now"+servo1.getValue());
            Utils.sleep(50);
        } else {
            servo1.setValue(SERVO1_MIN_VALUE);
            Utils.sleep(50);
        }
    }
    
    private void forward() {
        //System.out.println("backward");
        //servo1.setValue(0);  
        current2 = servo2.getValue();
        //System.out.println("the speed is" +  current2);
        for (int i=0;i<3;i++) {
            current2 = servo2.getValue();
            if (current2 -step2>SERVO2_MIN_VALUE) {
                servo2.setValue(current2-step2);
                Utils.sleep(50);
            } else {
                servo2.setValue(SERVO2_MIN_VALUE);
                Utils.sleep(50);
            }
        }   
    }

    // Stop the crawler
    private void stop() {
        System.out.println("stop");
        //servo1.setValue(0);
        servo2.setValue(SERVO_CENTER_VALUE);
    }

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
                //case PROG2:
                    //progBlinker.setColor(LEDColor.YELLOW);
                    //break;
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

