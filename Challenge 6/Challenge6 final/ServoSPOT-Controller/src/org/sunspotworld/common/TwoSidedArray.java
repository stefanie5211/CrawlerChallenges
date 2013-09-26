/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sunspotworld.common;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

/**
 * see the SharedArray class this is meant to be a simplified/optimized version 
 * meant to be used by only two nods simultaeneously
 * 
 * @author arshan
 * @author Tsuyoshi Miyake <Tsuyoshi.Miyake@Sun.COM>
 * @author Yuting Zhang<ytzhang@bu.edu>
 */
public class TwoSidedArray {

    private static final int CYCLE_TIME = 100;
    private static final int VAL_CNT = 2;
//    private OTAConnection connection;
    private String buddyAddress;
    private RadiogramConnection conn;
    private int[] vals = new int[VAL_CNT];
    private boolean timeoutError = false;

    /**
     * Creates a new instance of SharedArray
     */
    public TwoSidedArray(String address) {
        try {
//            buddyAddress = connection.listenForBuddy(Globals.BROADCAST_PORT, 1000);
            buddyAddress = address;
            // start the radio connection
            startRadioConnection();
        } catch (IOException e) {
            System.out.println("error start radio: " + e.getMessage());
        }
    }

    public TwoSidedArray(String address, long timeout) {
        try {
            buddyAddress = address;
            // start the radio connection
            startRadioConnection(timeout);
        } catch (IOException e) {
            System.out.println("error start radio: " + e.getMessage());
        }
    }

    public void startOutput() throws IOException {
        new DataOutput().start();
    }

    public void startInput() throws IOException {
        new DataInput().start();
    }

    public int getVal(int i) {
        synchronized (vals) {
            return vals[i];
        }
    }

    public void setVal(int i, int value) {
        synchronized (vals) {
            vals[i] = value;
        }
    }

    public synchronized boolean isTimeoutError() {
        return timeoutError;
    }

    private synchronized void setTimeoutError(boolean te) {
        this.timeoutError = te;
    }

    public void startRadioConnection() throws IOException {
        Spot.getInstance().getRadioPolicyManager().setChannelNumber(Globals.CHANNEL_NUMBER);
        Spot.getInstance().getRadioPolicyManager().setPanId(Globals.PAN_ID);
        String url = "radiogram://" + buddyAddress + ":" + Globals.COMMUNICATION_PORT;
        System.out.println("Start radio connection; URL to other radio = " + url);
        conn = (RadiogramConnection) Connector.open(url);
    }

    public void startRadioConnection(long timeout) throws IOException { // for input
        startRadioConnection();
        conn.setTimeout(timeout);
    }

    private class DataOutput extends Thread {

        private Datagram dg;
        int[] history = new int[VAL_CNT];

        private DataOutput() throws IOException {
            for (int x = 0; x < VAL_CNT; x++) {
                history[x] = getVal(x);
            }
            dg = conn.newDatagram(conn.getMaximumLength());
        }

        // XXX consider changing this, maybe at a threshold of values to 
        //     encode the position that has a new value and the new value as opposed
        //     to streaming the whole thing ... 
        // XXX this would be cool if it keep an Object[] and sync'd  the serialized rep
        //     of each ( especially if it was an rsync type thing where only the changed bits
        //     are moved
        private void send() throws IOException {
            boolean isSyncNeed = false;

            for (int x = 0; x < VAL_CNT; x++) {
                if (history[x] != getVal(x)) {
                    isSyncNeed = true;
                    break;
                }
            }

            if (isSyncNeed) {
                dg.reset();
                for (int x = 0; x < VAL_CNT; x++) {
                    int lval = getVal(x);
                    dg.writeInt(lval);
                    history[x] = lval;
                }
                conn.send(dg);
            }
        }

        public void run() {
            System.out.println("started DataOutput thread");

            while (true) {
                try {
                    Utils.sleep(CYCLE_TIME);
                    send();
                } catch (Exception e) {
                    System.out.println("[TM] error while sending: " + e.getMessage());
                }
            }
        }
    }

    private class DataInput extends Thread {

        private Datagram dg;

        public DataInput() throws IOException {
            this.dg = conn.newDatagram(conn.getMaximumLength());
        }

        private void receive() throws IOException {
            try {
                conn.receive(dg); // this will block til a valid datagram comes in
                for (int x = 0; x < VAL_CNT; x++) {
                    setVal(x, dg.readInt());
                }
                if (isTimeoutError()) {
                    setTimeoutError(false);
                }
            } catch (TimeoutException te) {
                System.out.println("[TM] timeout while reading: " + te.getMessage());
                setTimeoutError(true);
            }
        }

        public void run() {
            System.out.println("starting DataInput thread");

            while (true) {
                try {
                    receive();
                    // weve got a new set of values update the globals
                    Utils.sleep(CYCLE_TIME);
                } catch (IOException e) {
                    System.out.println("[TM] error while receiving: " + e.getMessage());
                }
            }
        }
    }
}
