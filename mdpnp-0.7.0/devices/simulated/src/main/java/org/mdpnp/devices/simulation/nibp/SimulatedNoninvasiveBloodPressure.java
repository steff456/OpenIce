/*******************************************************************************
 * Copyright (c) 2014, MD PnP Program
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.mdpnp.devices.simulation.nibp;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeff Plourde
 *
 */
public class SimulatedNoninvasiveBloodPressure implements Runnable {

    private final Random random = new Random();

    protected void beginInflation() {

    }

    protected void beginDeflation() {

    }

    protected void endDeflation() {

    }

    protected void updateInflation(int inflation) {

    }

    protected void updateReading(int systolic, int diastolic, int pulse) {

    }

    protected void updateNextInflationTime(long nextInflationTime) {

    }

    protected void simulateReading(int systolic, int diastolic, final int pulserate) {

        beginInflation();

        int tgtInflation = systolic + 30;
        int inflation = 0;

        while (running && inflation < tgtInflation) {
            updateInflation(inflation);

            inflation += random.nextInt(10) + 1;
            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!running) {
            return;
        }

        beginDeflation();

        while (running & inflation > 0) {
            updateInflation(inflation);

            inflation -= random.nextInt(10) + 1;
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!running) {
            return;
        }
        updateInflation(inflation);

        updateReading(systolic, diastolic, pulserate);

        endDeflation();

    }

    protected void simulateRandomReading() {
        float f = random.nextFloat();
        simulateReading((int) (f * 55) + 110, (int) (f * 35) + 70, (int) (f * 55) + 55);
    }

    private boolean running = true;

    private int[] singleOverride = null;

    private Long nextInflation = 0L;

    // TODO this is too quick
    private static final long WAITING_NOTIFY_INTERVAL = 1000L;

    public void run() {

        while (running) {
            try {
                long now = System.currentTimeMillis();
                synchronized (this) {
                    long diff = nextInflation - now;
                    long nextRoundMinute = diff % WAITING_NOTIFY_INTERVAL;
                    while (running && diff > 0) {
                        try {
                            this.wait(Math.max(1, Math.min(diff, nextRoundMinute)));
                            updateNextInflationTime(getNextInflationTimeRemaining());
                        } catch (InterruptedException e) {
                            log.error("interrupted", e);
                        }
                        now = System.currentTimeMillis();
                        diff = nextInflation - now;
                        nextRoundMinute = diff % WAITING_NOTIFY_INTERVAL;
                    }
                }
                if (!running) {
                    break;
                }
                updateNextInflationTime(getNextInflationTimeRemaining());

                int[] singleOverride = this.singleOverride;
                this.singleOverride = null;

                this.nextInflation = null;

                if (null != singleOverride) {
                    simulateReading(singleOverride[0], singleOverride[1], singleOverride[2]);
                } else {
                    simulateRandomReading();
                }
            } catch (Exception e) {
                log.error("in NIBP simulator loop", e);
            } finally {
                long now = System.currentTimeMillis();
                synchronized (this) {
                    endDeflation();

                    this.nextInflation = now + INTERVAL;
                    this.notifyAll();
                }
                updateNextInflationTime((long) nextInflation);
            }
        }
        log.trace("Here ends the NIBP simulator thread");
    }

    private static final Logger log = LoggerFactory.getLogger(SimulatedNoninvasiveBloodPressure.class);
    private static final long INTERVAL = 3 * 60 * 1000L;

    private final Object threadLock = new Object();
    private Thread t;

    public SimulatedNoninvasiveBloodPressure() {
        super();

    }

    public void connect(String str) {
        endDeflation();
        synchronized (threadLock) {
            if (null != t) {
                running = false;
                try {
                    t.join(5000L);
                } catch (InterruptedException e) {
                    log.error("interrupted", e);
                }
                t = null;
            }
            running = true;
            t = new Thread(this);
            t.setDaemon(true);
            t.start();
        }
    }

    public void disconnect() {
        synchronized (threadLock) {
            if (t != null) {
                running = false;
                try {
                    t.join(5000L);
                } catch (InterruptedException e) {
                    log.error("interrupted", e);
                }
                this.t = null;
            }
        }
    }

    private Long getNextInflationTimeRemaining() {
        return null == nextInflation ? null : (nextInflation - System.currentTimeMillis());
    }

    public synchronized void doInflate() {
        this.nextInflation = 0L;
        this.notifyAll();
    }

    public synchronized void doSimulate(int systolic, int diastolic, int pulserate) {
        this.singleOverride = new int[] { systolic, diastolic, pulserate };
        this.nextInflation = 0L;
        this.notifyAll();
    }

}
