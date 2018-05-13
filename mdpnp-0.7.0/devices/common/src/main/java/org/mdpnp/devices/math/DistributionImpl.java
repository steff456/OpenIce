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
package org.mdpnp.devices.math;

/**
 * @author Jeff Plourde
 *
 */
public class DistributionImpl implements Distribution {

    private double m1 = 0.0;
    private double m2 = 0.0;
    private double m3 = 0.0;
    private double m4 = 0.0;
    private double dev = 0.0;
    private double nDev = 0.0;
    private double nDevSq = 0.0;
    private int n = 0;
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;

    @Override
    public String toString() {
        return "Cnt" + getRealSamples() + "\tAvg:" + getAverage() + "\tStdDev:" + getStdDev() + "\tMin:" + getMinimum() + "\tMax:" + getMaximum()
                + "\tSkew:" + getSkewness() + "\tKurt:" + getKurtosis();
    }

    public double getAverage() {
        return m1;
    }

    public double getKurtosis() {
        double kurtosis = Double.NaN;
        if (n > 3) {
            double variance = getVariance();
            if (n <= 3 || variance < 10E-20) {
                kurtosis = 0.0;
            } else {
                double n0 = (double) n;
                kurtosis = (n0 * (n0 + 1) * m4 - 3 * m2 * m2 * (n0 - 1)) / ((n0 - 1) * (n0 - 2) * (n0 - 3) * variance * variance);
            }
        }
        return kurtosis;
    }

    public double getMaximum() {
        return this.max;
    }

    public double getMinimum() {
        return this.min;
    }

    public int getRealSamples() {
        return this.n;
    }

    public double getSkewness() {
        double variance = getVariance();
        if (variance < 10E-20) {
            return 0.0d;
        } else {
            double n0 = (double) n;
            return (n0 * m3) / ((n0 - 1) * (n0 - 2) * Math.sqrt(variance) * variance);
        }
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public double getVariance() {
        // n = 0
        // mean = 0
        // M2 = 0
        //
        // foreach x in data:
        // n = n + 1
        // delta = x - mean
        // mean = mean + delta/n
        // M2 = M2 + delta*(x - mean) // This expression uses the new value of
        // mean
        // end for
        //
        // variance_n = M2/n
        // variance = M2/(n - 1)

        return m2 / ((double) n - 1.0);
    }

    public void newPoint(double d) {
        double prevM2 = m2;
        double prevM3 = m3;

        n++;
        double n0 = (double) n;
        dev = d - m1;
        nDev = dev / n0;
        m1 += nDev;
        m2 += (n0 - 1) * dev * nDev;
        nDevSq = nDev * nDev;
        m3 = m3 - 3.0 * nDev * prevM2 + (n0 - 1) * (n0 - 2) * nDevSq * dev;
        m4 = m4 - 4.0 * nDev * prevM3 + 6.0 * nDevSq * prevM2 + ((n0 * n0) - 3 * (n0 - 1)) * (nDevSq * nDevSq * (n0 - 1) * n0);
        this.min = Math.min(d, this.min);
        this.max = Math.max(d, this.max);
    }

    public void newPoint(Object p) {
        if (!(p instanceof Number)) {
            throw new IllegalArgumentException("To regress x and y must be of Number type.");
        }
        newPoint(((Number) p).doubleValue());
    }

    public Distribution reset() {
        this.n = 0;
        this.m1 = 0.0;
        this.m2 = 0.0;
        this.m3 = 0.0;
        this.m4 = 0.0;
        this.dev = 0.0;
        this.nDev = 0.0;
        this.nDevSq = 0.0;

        this.max = Double.MIN_VALUE;
        this.min = Double.MAX_VALUE;
        return this;
    }

    public static void main(String[] args) {
        Distribution d = new DistributionImpl();
        double[] v = new double[] { 1.0, 3.0, -4.0, 1.0, 11.0 };
        for (double i : v) {
            d.newPoint(i);
        }
        System.out.println(d);

    }
}
