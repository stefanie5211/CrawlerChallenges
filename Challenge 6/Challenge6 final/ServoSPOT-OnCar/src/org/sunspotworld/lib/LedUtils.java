/*
 * Copyright (c) 2008 Sun Microsystems, Inc.
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
