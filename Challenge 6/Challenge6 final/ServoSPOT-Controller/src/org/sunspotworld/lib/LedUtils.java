/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.lib;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;

/**
 * A utility class for demo board LEDs
 * 
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class LedUtils {
    private static ITriColorLED[] leds = EDemoBoard.getInstance().getLEDs();
    
    /**
     * setColor() for all LEDs
     * 
     * @param color color
     */
    public static void setColorAll(LEDColor color) {
        for (int i = 0; i < leds.length; i++) {
            leds[i].setColor(color);
        }
    }
    
    /**
     * setRGB() for all LEDs
     * 
     * @param r the intensity of the red portion, in the range 0-255
     * @param g the intensity of the green portion, in the range 0-255
     * @param b the intensity of the blue portion, in the range 0-255
     */
    public static void setRGBAll(int r, int g, int b) {
        for (int i = 0; i < leds.length; i++) {
            leds[i].setRGB(r, g, b);
        }
    }
    
    /**
     * setOn() for all LEDs
     */
    public static void setOnAll() {
        for (int i = 0; i < leds.length; i++) {
            leds[i].setOn();
        }
    }
    
    /**
     * setOff() for all LEDs
     */
    public static void setOffAll() {
        for (int i = 0; i < leds.length; i++) {
            leds[i].setOff();
        }
    }

}
