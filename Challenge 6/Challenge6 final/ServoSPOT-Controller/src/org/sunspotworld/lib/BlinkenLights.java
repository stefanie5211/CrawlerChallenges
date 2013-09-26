/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.lib;



import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.resources.transducers.ILightSensor;

import com.sun.spot.util.Utils;
import java.io.IOException;

/**
 * simple utility to run some effects on the LEDs of the sensorboard
 * 
 * @author arshan
 * @author bob
 * @author David Mercier <david.mercier@sun.com>
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 */
public class BlinkenLights {

    private EDemoBoard eDemo = EDemoBoard.getInstance();
    private ITriColorLED[] leds = eDemo.getLEDs();
    private ILightSensor light = eDemo.getLightSensor();
    private LEDColor myColor = LEDColor.RED;
    private LightRunner lightrunner = null;
    private int speed = 70;
    private int lightThreshold = 10;
    private int indexFrom = 0;
    private int indexTo = leds.length - 1;

    public BlinkenLights() {
    }

    public BlinkenLights(int from, int to) {
        this(from, to, 10);
    }

    public BlinkenLights(int from, int to, int light) {
        this.indexFrom = from;
        this.indexTo = to;
        this.lightThreshold = light;
    }

    public LEDColor getColor() {
        return myColor;
    }

    public void setColor(LEDColor color) {
        myColor = color;
    }

    public void setDelay(int delayMs) {
        speed = delayMs;
    }

    public void lightsOn() {
        for (int x = indexFrom; x <= indexTo; x++) {
            leds[x].setOn();
        }
    }

    public void lightsOff() {
        for (int x = indexFrom; x <= indexTo; x++) {
            leds[x].setOff();
        }
    }

    public void setLED(int led, int r, int g, int b) {
        if (lightrunner != null) {
            stopPsilon();
        }
        leds[led].setRGB(r, g, b);
    }

    public void startPsilon() {
        stopPsilon();
        lightrunner = new LightRunner();
        lightrunner.setPriority(Thread.MIN_PRIORITY);
        lightrunner.start();
    }

    public void stopPsilon() {
        if (lightrunner != null) {
            lightrunner.stopThread();
            lightrunner = null;
        }
    }

    private class LightRunner extends Thread {

        private boolean keepemrunning = true;

        public void stopThread() {
            keepemrunning = false;
        }

        private int reduce(int input) {
            return (input < 10) ? 0 : input / 2;
        }

        private void decay() {
            for (int x = indexFrom; x <= indexTo; x++) {
                leds[x].setRGB(reduce(leds[x].getRed()), reduce(leds[x].getGreen()), reduce(leds[x].getBlue()));
            }
        }

        public void run() {
            int current = 0; // LED No.
            int trend = 1;   // 0: increasing LED No. 1: decreasing LED No.

            for (int x = indexFrom; x <= indexTo; x++) {
                leds[x].setOn();
                leds[x].setRGB(20, 20, 20);
                Utils.sleep(70);
                leds[x].setRGB(0, 0, 0);
            }

            while (keepemrunning) {
                try {
                    lightThreshold(light.getValue());
                } catch (IOException e) {
                    lightThreshold(0);
                }

                if (trend == 1) {
                    current++;
                } else {
                    current--;
                }

                leds[current].setColor(myColor);
                decay();
                if (current == indexTo) {
                    trend = 0;
                }
                if (current == indexFrom) {
                    trend = 1;
                }
                Utils.sleep(speed);
            }
        }

        private void lightThreshold(final int val) {
            if (val > lightThreshold) {
                lightsOn();
            } else {
                lightsOff();
            }
        }
    }
}
