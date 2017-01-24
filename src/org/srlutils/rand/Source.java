// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.rand;

import org.srlutils.Rand.Seeded;

public class Source {
        /** the random number generator - probably shouldn't be accessed directly */
        public Seeded prng;
        public Source()               { prng = new Seeded();     }
        public Source(long seed)      { prng = new Seeded(seed); }
        public Source(Seeded $source) { prng = $source;          }

        /** new array, length nn, random values [0,1)    */   public double [] rand (int nn) { return rand(new double [nn]); }
        /** new array, length nn, random values [0,1)    */   public float  []frand (int nn) { return rand(new  float [nn]); }
        /** new array, length nn, random values          */   public int    []irand (int nn) { return rand(new    int [nn]); }
        /** new array, length nn, random values          */   public long   []lrand (int nn) { return rand(new   long [nn]); }

        /** random value             */ public    int irand ()                      { return         prng.nextInt();                       }
        /** random value [0,1)       */ public double  rand ()                      { return         prng.nextDouble();                    }
        /** gaussian val m=0,u=1     */ public double  randn()                      { return         prng.nextGaussian();                  }
        /** random value [min,max)   */ public    int  rand (    int min,  int max) { return              nextInt( min, max );             }
        /** random value [min,max)   */ public   long  rand (   long min, long max) { return              nextLong( min, max );            }
        /** random value [min,max)   */ public   byte  rand (   byte min, byte max) { return  (byte) (min +      nextUnsigned( max-min )); }
        /** random value [min,max)   */ public   char  rand (   char min, char max) { return  (char) (min +      nextUnsigned( max-min )); }
        /** random value [min,max)   */ public  short  rand (  short min,short max) { return (short) (min +      nextUnsigned( max-min )); }

        /** random value [min,max) */
        public double rand(double min,double max) { double val = prng.nextDouble(); return (1-val)*min + max*val; }
        /** random value [min,max) */
        public  float rand( float min, float max) {  float val = prng.nextFloat (); return (1-val)*min + max*val; }


        // fixme::performance -- could use the "whole" int as multiple vals ...
        /** fill array vals with random values           */   public    char [] rand(    char [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = (char) prng.nextInt();  return vals; }
        /** fill array vals with random values           */   public   short [] rand (  short [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = (short) prng.nextInt(); return vals; }

        /** fill array vals with random values [0,1)     */   public double [] rand (double [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = prng.nextDouble();   return vals; }
        /** fill array vals with gausian(0,1)            */   public double [] randn(double [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = prng.nextGaussian(); return vals; }
        /** fill array vals with random values [0,1)     */   public float  [] rand (float  [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = prng.nextFloat ();   return vals; }
        /** fill array vals with random values           */   public int    [] rand (int    [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = prng.nextInt();      return vals; }
        /** fill array vals with random values           */   public boolean [] rand (boolean [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = prng.nextBoolean ();   return vals; }
        /** fill array vals with random values           */   public    byte [] rand (   byte [] vals)         {                                                 prng.nextBytes(vals);  return vals; }
        /** fill array vals with random values           */   public    long [] rand (   long [] vals)         { for (int ii=0; ii<vals.length; ii++) vals[ii] = prng.nextLong ();      return vals; }


        /** fill v2 with random vals over [min,max)       */  public    byte[] rand(   byte[]v2,   byte min,   byte max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }
        /** fill v2 with random vals over [min,max)       */  public    char[] rand(   char[]v2,   char min,   char max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }
        /** fill v2 with random vals over [min,max)       */  public   short[] rand(  short[]v2,  short min,  short max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }
        /** fill v2 with random vals over [min,max)       */  public     int[] rand(    int[]v2,    int min,    int max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }
        /** fill v2 with random vals over [min,max)       */  public  double[] rand( double[]v2, double min, double max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }
        /** fill v2 with random vals over [min,max)       */  public   float[] rand(  float[]v2,  float min,  float max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }
        /** fill v2 with random vals over [min,max)       */  public    long[] rand(   long[]v2,   long min,   long max) { for(int ii=0;ii<v2.length;ii++) v2[ii]=rand(min,max); return v2; }


    /**
     * set the random number generator seed
     * if seed==null, use a random seed
     * if verbose, print the seed
     * return the seed that was used.
     */
    public long setSeed(Long seed,boolean verbose) {
        if (seed==null) seed = prng.setSeed();
        else                   prng.setSeed( seed );
        if (verbose) System.out.format( "seed = %dL;\n", seed );
        return seed;
    }

    /** set a new seed, returning this */
    public Source setSeed(long seed) { prng.setSeed( seed ); return this; }

    /** set and return a new seed - this is useful to create a "fence" of repeatable numbers */
    public long setSeed() { return prng.setSeed(); }




    /** random value [min,max) */
    public int nextInt(int min,int max) {
        return min + nextUnsigned( max - min );
    }
    /** random value [0,nn), nn is treated as unsigned, use nn=0 for the full range */
    public int nextUnsigned(int nn) {
        if (nn > 0) return prng.nextInt( nn );
        if (nn == 0) return prng.nextInt();
        if (nn==Integer.MIN_VALUE) return prng.nextInt() >>> 1;

        // overflow ... so at worst 50% are outside the range
        int val;
        do  val = prng.nextInt(); while ( val < 0 && val >= nn );
        return val;
    }
    /** random value [min,max), use max==MinLong to indicate MaxLong+1  */
    public long  nextLong(long min,long max) {
        return min + nextUnsigned( max - min );
    }
    /** random value [0,nn), nn is treated as unsigned, use nn=0 for full range  */
    public long  nextUnsigned(long nn) {

        // degenerate case ... the full range
        if ( nn == 0 ) return prng.nextLong();

        // power of 2
        if ((nn & -nn) == nn) {
            int shift = Long.numberOfLeadingZeros( nn ) + 1;
            return prng.nextLong() >>> shift;
        }

        // overflow ... so at worst 50% are outside the range
        if ( nn < 0 ) {
            long val;
            do  val = prng.nextLong(); while ( val >= nn );
            return val;
        }

        // nn <= MaxLong
        long bits, val, next;
        do {
            bits = prng.nextLong() >>> 1;// positive
                                         // fixme::performane -- could use the extra bit to reduce collisions ...
            val = bits % nn;             // bits-val --> (bits/nn)*nn --> max value is MaxLong/n*n <= MaxLong
            next = bits - val + nn;      // next:start of the next bin, max val at nn == bits == MaxLong
                                         //   --> max(next) = 2*Long.MAX = -2
                                         // don't have to worry about next == MinLong ... nn can't be a factor
        } while (next < 0);
        return val;
    }


    // delegate all the Random stuff ... debated leaving this out and having the user access prng directly

    /** @see   java.util.Random#setSeed(long) */
    public long    nextLong   ()              { return prng.nextLong(); }
    public int     nextInt    (int n)         { return prng.nextInt( n ); }
    public int     nextInt    ()              { return prng.nextInt(); }
    public float   nextFloat  ()              { return prng.nextFloat(); }
    public double  nextDouble ()              { return prng.nextDouble(); }
    public boolean nextBoolean()              { return prng.nextBoolean(); }
    public void    nextBytes  (byte[] bytes)  {        prng.nextBytes( bytes ); }
    public synchronized double nextGaussian() { return prng.nextGaussian(); }



    
}
