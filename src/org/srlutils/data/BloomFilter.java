// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import org.srlutils.Rand;
import org.srlutils.Rand.XorShift;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;

/**
 * a bloom filter (see http://en.wikipedia.org/wiki/Bloom_filter)
 *   ie, a probabilistic set with no false negatives
 * using the XorShift rng as a primitive hash function
 * expects nx values to be stored in bit array data of length mo using ko hashes
 * grows mo to a power of 2 to speed up random number generation
 * 
 * multiple filters can be used with the same backing array
 *   no problems with multiple readers
 *   multiple writers could collide, would result in false negatives
 *   usage: dup() returns a new filter backed by the same array
 */
public class BloomFilter {
    /** constant: ln(2)      */  public static final double ln2 = Math.log( 2 );
    /** constant: ln(2)^2    */  public static final double ln22 = ln2*ln2;

    /** return the optimal array size (mo) given the nominal capacity and false alarm rate */
    public static int bestm(int $nx,double $far) {
        double best = -1.0 * $nx * Math.log( $far ) / ln22;
        return (int) Math.nextUp( best );
    }

    /** the hash generator */
    public XorShift xor = new XorShift();
    /** the guts of the bloom filter */
    public final Base base;
    public int size;

    /** init the base with the expected number of members and the desired false alarm rate, returns This */
    public BloomFilter init(int $nx, double $far) { base.init( $nx, $far ); return this; }

    public BloomFilter() { base = new Base(); }
    public BloomFilter(Base $base) { base = $base; }

    /** initialize the hasher with val */
    public void prep(long val) { xor.seed( val ); }

    /**
     * get the next index
     * performance note: using  "bits % mo" instead of "bits >>> nbits"
     *   resulted in a 100% speed penalty (verified post-rand-seed-bug-fix)
     *   this means that relaxing the mo-power-of-2 requirement is not a practical option
     * performance note:
     *   tested with int and long args
     *     int args limit to 32 bits of bits, ie 512M backing array
     *     longs are limited by the backing array, either 1G for byte[] or 4G for int[]
     *     performance penalty for long args is approx 0.5%, chose long args
     */
    public long next() { return xor.next() >>> (64-base.nbits); }

    /**
     * duplicate the filter using the same backing array
     *   allows access from another thread
     *   multiple writers weaken the contract, resulting in the possibility of false negatives
     */
    public BloomFilter dup() { return new BloomFilter(base); }


    /**
     * store val and return the prev membership state
     * performance note: tested put v set and 9% slower so can't eliminate set
     */
    public boolean put(long val) {
        prep( val );
        boolean found = true;
        for (int ii = 0; ii < base.ko; ii++) found &= base.putBit( next() );
        if (!found) base.nc++;
        return found;
    }

    /** store val */
    public void set(long val) {
        prep( val );
        for (int ii = 0; ii < base.ko; ii++) base.setBit( next() );
        base.nc++;
    }

    /** return the membership state of val */
    public boolean get(long val) {
        prep( val );
        boolean found = true;
        for (int ii = 0; ii < base.ko && found; ii++) found &= base.getBit( next() );
        return found;
    }





    /**
     * the backing array - can be used with multiple filters
     *   multiple writers weakens the contract, ie result in the possibility of false negatives
     *   multiple readers cause no problems
     * max size is limited by the backing array type and the index type (implicitly unsigned)
     *   array is always a power of 2, so longest array is 2^30
     *   with both being ints it's limited by the index and 512M (128M * 4bytes) backing array is the max
     *   with a long index the array size is the limit, meaning 4G as ints, 1G as bytes
     *   performance note: using byte [] backing array was 2% slower
     *   only advantage is a slightly reduced risk of contention for simultaneous writers (multi-thread)
     *   still no guarantee so doesn't make sense
     */
    public static class Base {
        /** the size of the data element */
        public static final int ds = 32;
        /** the mask required to get the position-within-the-data-element portion of the index */
        public static final int mod = ds - 1;
        /** the right shift required to get the address portion of the index */
        public static final int shift = 32 - Integer.numberOfLeadingZeros( mod );


        /** the number of hashes        */  public int ko;
        /** the bit array               */  public int [] data;
        /** the bit array length        */  public int mo;
        /** expected number of elements */  public int nx;
        /** number of bits, ie log2(mo) */  public int nbits;

        /**
         * approximate count of set elements
         *   put only increments if new, set increments regardless
         *   increments are not synchronized so increments can also be missed
         */
        public int nc;


        /**
         * init the filter for the nominal capacity (no) and array size (mo)
         * mo gets rounded up to the next power of 2, which must be positive integer
         */
        public void init(int $nx,int $mo) {
            nx = $nx;
            int len = Simple.Rounder.rup( $mo, 8 );
            nbits = 31 - Integer.numberOfLeadingZeros( len-1 ) + 1;
            mo = 1 << nbits;
            Simple.softAssert( mo > 0, "BloomFilter::arg -- calculated array size is too large: %d --> %d", $mo, mo );
            // fixme::speed -- if we grow mo, we can shrink ko --> same far (though suboptimal)
            //   octave: function val = far(k,r); val = (1-e^(-k/r))^k; endfunction;
            ko = (int) Math.ceil( ln2 * mo / nx );
            data = new int[ mo >>> shift ];
        }
        /** init the filter for the nominal capacity (no) and false alarm rate */
        public void init(int $nx,double $far) { init( $nx, bestm( $nx, $far ) ); }

        /** returns the estimated false alarm rate */
        public double far() {
            double base = 1.0 - Math.exp( -1.0 * ko * nx / mo );
            return Math.pow( base, ko );
        }
        /** set the bit at index and return the previous state of the bit */
        public boolean putBit(long index) {
            int addr = (int) (index >>> shift), pos = (int) (index & mod), mask = 1 << pos, prev = data[ addr ];
            data[ addr ] |= mask;
            return (prev & mask) != 0;
        }
        /** set the bit at index */
        public void setBit(long index) {
            int addr = (int) (index >>> shift), pos = (int) (index & mod), mask = 1 << pos;
            data[ addr ] |= mask;
        }
        /** get the bit at index */
        public boolean getBit(long index) {
            int addr = (int) (index >>> shift), pos = (int) (index & mod), mask = 1 << pos;
            return (data[ addr ] & mask) != 0;
        }
        /** info describing the filter ... nx, ko, mo, far() */
        public String info() {
            return String.format( "BloomFilter: %d * %d --> %d, far: %8.3f", nx, ko, mo, far() );
        }
    }

    public static class Test extends TaskTimer.Runner<Void> {
        /** size, add, check    */    public int nn = 1<<20, na = nn, nc = 100*nn, nf;
        /** the desired far     */    public double dfar = 0.05;
        public long seed;
        public boolean found;
        BloomFilter bf;
        XorShift xor = new XorShift();
        public String name = "random ^A";
        { xor.seed( Rand.irand() ); System.out.format( "seed: %d\n", xor.xo ); }

        public void alloc() { stageNames = "set get".split( " " ); setup( 2, name ); }
        public void init() throws Exception {
            bf = new BloomFilter().init( nn, dfar );
            nf = 0;
            found = true;
            seed = xor.xo;
        }

        public void run(int stage) throws Exception {
            if (stage==0) set();
            else {
                xor.seed( seed );
                get();
                check();
            }
        }

        /** offset the random feed to keep the filter from syncing with the test randomness */
        public long next() { return xor.next() ^ seed; }

        public void   set() { for (int ii = 0; ii < na; ii++)          bf.set( next() ); }
        public void   get() { for (int ii = 0; ii < na; ii++) found &= bf.get( next() ); }
        public void check() { for (int ii = 0; ii < nc; ii++)    nf += bf.get( next() ) ? 1 : 0; }

        public boolean finish() throws Exception {
            double efar = bf.base.far(), far = 1.0*nf/nc;
            boolean done = found && Math.abs( far - efar ) < Math.max( .01, efar );
            System.out.format( "found: %b, alarms: %d, far: %8.3f, ~far: %8.3f", found, nf, far, efar );
            bf = null;
            return done;
        }
    }

    public static void main(String [] args) throws Exception {
        TaskTimer tt = new TaskTimer().widths(8,4).init( 4, 2, true, true );
        tt.autoTimer(
                new Test()
                );
    }

}
