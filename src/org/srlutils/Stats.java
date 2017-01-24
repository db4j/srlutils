// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.io.Serializable;
import java.util.HashMap;

public class Stats {

    /** a (value,count) tuple, representing the frequency of the value. NB: equals is fragile */
    public static class Freq {
        public int val, count;
        public Freq set(int $val) { val = $val; count = 1; return this; }
        public boolean equals(Object obj) { return val == ((Freq) obj).val; }
        public int hashCode() { return val; }
    }

    public static class FreqData {
        HashMap<Freq,Freq> map = new HashMap();
        public FreqData occur(int [] vals) {
            for (int val : vals) {
                Freq key = new Freq().set(val), junk = map.get( key );
                if (junk==null) map.put( key, key );
                else junk.count++;
            }
            return this;
        }
        public Freq [] get() { return map.keySet().toArray( new Freq[0] ); }
    }


    /** return the smallest multiple of modulus >= val */
    public static long modceil(long val,long modulus) {
        long delta = val % modulus;
        return delta==0 ? val : val + modulus - delta;
    }




    private static double sq(double val) { return val*val; }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean, double[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,  float[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,boolean[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - (vals[ii]?1:0) );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,   byte[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,   char[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,  short[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,   long[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }

    /** variance of the elements                     */
    public static double var(int k1, int k2,double mean,    int[] vals) {
        double var = 0;
        for (int ii = k1; ii < k2; ii++) var += sq( mean - vals[ii] );
        return var / (k2-k1);
    }



    public static class Std implements Serializable {
        public double m, v;
        private double s = -1;
        public Std(double mean,double var) { m = mean; v = var; }

        /** the standard deviation */
        public double s() {
            if ( s < 0 ) s = Math.sqrt( v );
            return s;
        }
    }



}
