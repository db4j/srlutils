// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import org.srlutils.rand.Source;
import java.util.Random;

public class Rand {
    /**
     * convienient source for when fine-grain control isn't needed,
     * eg for a single thread where everything is deterministic - also used for the automatic seeds for
     * new Seeded's ... allowing for controlling all automatic Seeded's from a single location if needed
     */
    public static final Source source = new Source( new Random().nextLong() );

    /** new array, length nn, random values          */   public static int    []irand(int nn) { return source.irand (nn); }
    /** new array, length nn, random values [0,1)    */   public static double [] rand(int nn) { return source. rand (nn); }
    /** random value                                 */   public static int      irand()       { return source.nextInt(); }



    /**
     * an extension of java.util.Random that allows for easier control of the seeds by keeping
     * track of the current seed
     * an internal rng rnd is maintained as a source of seeds
     * allowing for automatic-yet-reproducible seeds
     * at one point used an auto-increment long as the seedSeed
     * but this resulted in very little randomness in the first few seeds
     * (and potentially weird cross-correlations ... not my area of expertise but looks suspicious)
     * not sure that rnd solves this problem entirely, but it's a much better
     */
    public static class Seeded extends Random {

        /** the current seed - read only, to write use setSeed */
        public long seed;
        private Random rnd = new Random();
        public Long seedSeed = null;

        /** construct using a random seed from Rand.source */
        public Seeded() {
            super();
            seedSeed = rnd.nextLong();
            rnd.setSeed( seedSeed );
            seed = rnd.nextLong();
            super.setSeed( seed );
        }

        /** construct using the provided $seed */
        public Seeded(long $seed) {
            super( $seed );
            seed = $seed;
        }

        /** set and save the seed */
        public void setSeed(long $seed) {
            seedSeed = null;
            seed = $seed;
            super.setSeed( $seed );
        }

        public int next(int bits) { return super.next( bits ); }

        /** set, save and return a new seed - this is useful to create a "fence" of repeatable numbers */
        public long setSeed() {
            if (seedSeed==null) {
                seedSeed = rnd.nextLong();
                rnd.setSeed( seedSeed );
            }
            seed = rnd.nextLong();
            super.setSeed( seed );
            return seed;
        }
        /** set the seed for the underlying source of seeds, ie to allow a repeatable stream */
        public void setSeedSeed(long seed) { seedSeed = seed; rnd.setSeed( seed ); }
        
        public long setSeed(Long $seed,boolean verbose) {
            seed = ($seed==null) ? rnd.nextLong() : $seed;
            super.setSeed( seed );
            if (verbose) System.out.format( "seed = %dL;\n", seed );
            return seed;
        }
        /** initialize the source of seeds, printing the result if verbose */
        public long init(Long $seedSeed,boolean verbose) {
            seedSeed = ($seedSeed==null) ? rnd.nextLong() : $seedSeed;
            rnd.setSeed(seedSeed);
            if (verbose) System.out.format( "seedSeed = %dL;\n", seedSeed );
            return seedSeed;
        }
    }

    /**
     * a pseudo random number generator
     * credited to George Marsaglia, 2003
     * from: http://www.javamex.com/tutorials/random_numbers/xorshift.shtml
     * it appears that the (21,35,4) comes from Numerical Recipes (isbn-10:0521880688)
     * never produces 0, period is 2^64-1, every number is unique for the period
     */
    public static class XorShift {
        /** the seed */
        public long xo;
        public void seed(long seed) { xo = seed; }
        public long next() {
            xo ^= (xo << 21);
            xo ^= (xo >>> 35);
            xo ^= (xo << 4);
            return xo;
        }
        public long next(int nbits) { return next() >>> (64 - nbits); }
    }

    
    
    
    
    public static void main(String [] args) {
        System.out.println( "using consequetive seeds results in a correlated stream" );
        System.out.println( "\t\t min, max, fraction of a gig that is covered" );
        int np = 1;
        // random isn't that random in the beginning ...
        for (int jj = 0; jj < 10; jj++) {
            java.util.Random rnd = new java.util.Random();
            int nn = 100;
            int [] vals = new int[nn];
            long seed = rnd.nextLong();
            for (int ii = 0; ii < 100; ii++) {
                rnd.setSeed( seed+ii );
                for (int kk = 0; kk < np; kk++)
                vals[ii] = rnd.nextInt();
            }
            int [] bounds = org.srlutils.Util.bounds( vals );
            System.out.format( "bounds: %20d, %20d, %8.3f\n",
                    bounds[0], bounds[1], 1.0*(1L*bounds[1]-bounds[0])/(1<<30) );
            int [] diff = org.srlutils.Util.dift( vals );
//            for (int val : diff) { System.out.format( "%20d\n", val ); }
        }
        /*
            using the first value (np=1)
            bounds:          -1598587611,          -1500476642,    0.091
            bounds:            479567137,            570752626,    0.085
            bounds:           -864134869,           -772179882,    0.086
            bounds:            135200973,            233311942,    0.091
            bounds:          -1681140458,          -1338329188,    0.319
            bounds:          -2026178022,          -1979238656,    0.044
            bounds:          -1387688990,          -1338825880,    0.046
            bounds:          -2100684502,           -820624911,    1.192
            bounds:          -2139282104,           2104513588,    3.952
            bounds:            284609983,            382720953,    0.091

            using the 10th value (np=10), the values cover the entire int space
                though they may still be poorly distributed
            */



    }
    
    
    
    
    
    
}




