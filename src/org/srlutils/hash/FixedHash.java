// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.hash;

import org.srlutils.Rand;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import org.srlutils.tests.CacheTest;

public class FixedHash<K, V> {


    /** values that put thru the hash() result in index len/2 */
    public static int [] antihash = new int[] {
           0,           0,           0,           0,           1,          33,          23,          86,
         179,         183,         708,         341,        3259,       10370,       28031,       16608,
       25535,      236463,      194932,       45918,      362534,     1311321,     2827383,      498823,
     3324315,    22452707,     8484913,   262374560,   109328802,   302163021,  1369763485,           0,
    };
    
    public static int [] zeropos = new int[] {
           0,           0,           0,           0,           2,          18,          50,         114,
         242,         242,         754,        1778,        3826,        3826,       12018,       12018,
       44786,      110322,      241394,      241394,      765682,      765682,      765682,      765682,
     9154290,    25931506,    59485938,   126594802,   126594802,   126594802,   663465714,           0,
    };        
    
    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because ConcurrentHashMap uses power-of-two length hash tables,
     * that otherwise encounter collisions for hashCodes that do not
     * differ in lower or upper bits.
     */
    public static int hash(int h) {
        // NB: from doug lea's CHM, under the public domain "license"
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }
    public static int hash(long h) {
        // this is how openjdk converts a long to an int for hashing
        // fixme - verify that the composite is a good hash
	int val = (int)(h ^ (h >>> 32));
        return hash(val);
    }

    public static int[] buildAntiMagic() {
        System.out.format( "zero hashes to: %d\n", hash(0) );
        Set tree;
        int [] mates = new int[32], zeros = new int[32];
        bits:
        for (int bits = 4; bits < 31; bits++) {
            tree = new Set().init(bits);
            int zero = tree.mask( hash(0) );
            zeros[bits] = zero;
            int target = tree.mask( hash(0) + (1<<(bits-1)) );
            // find the first key that results in a hash of tree.len/2, ie, halfway from index 0
            for (int ii = 0; ii < (1L<<32); ii++) {
                int val = tree.index(ii);
                if (val==target) {
                    mates[bits] = ii;
                    int delta = Math.abs( target-zero ) - tree.len/2;
                    System.out.format( "%3d %12d %12d %12d\n", bits, ii, target, delta );
                    continue bits;
                }
            }
            System.out.format( "no mate found for %d\n", bits );
        }
        
        System.out.format( "zero hashes to: %d\n", hash(0) );
        System.out.format( "antiMagic:\n" );
        org.srlutils.Array.println( mates, "\t", "%12d,", 8 );
        System.out.format( "\nzeros:\n" );
        org.srlutils.Array.println( zeros, "\t", "%12d,", 8 );
        System.out.format( "\n" );
        return mates;
    }
    
    // we need a source that is likely to go thru zero so we can test antimagic
    // use an int xorshift and just skip values outside a range
    public static class XorInt extends Rand.XorShift {
        private int aa = 1, bb = 3, cc = 10;
        /** the seed */
        public int xo;
        public long max;
        public XorInt set(long $max) { max = $max; return this; }
        public void seed(long seed) { xo = (int) seed; }
        public long next2() {
            xo ^= (xo << aa);
            xo ^= (xo >>> bb);
            xo ^= (xo << cc);
            return xo;
        }
        public long next() {
            long mask = (1L<<32)-1;
            while (true) {
                long val = next2() & mask;
                if (val < max) return val;
            }
        }
    }

    /** override the rng to force zeros to be likely */
    public static class TestLH extends CacheTest.Tester {
        public LongHash.Set tree;
        int offset = -1;
        int period;
        { name = "LH"; }
        public void initMap() {
            // use 50% fill to make multiple "zeros" more likely
            tree = new LongHash.Set().init( cbits + 1 );
            offset = -1 << (cbits - 1);
            long p1 = 0L + nn + cacheSize + 1 + nn*nc;
            // this p1+offset combo results in triggering antimagic about 50% of the runs (both paths)
            offset = -100000;
            p1 = 2*nn;
            period = (int) p1;
            x1 = new XorInt().set(period);
            x2 = new XorInt().set(period);
            x3 = new XorInt().set(period);
        }
        public boolean lookup(long val) { return tree.contains( val+offset ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.contains( leading+offset );
                boolean haslag  = tree.contains( lagging+offset ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging+offset );
            }
            tree.put( leading+offset );
        }
    }
    // IntHash is generated from LongHash
    /*
        cdmod u+w IntHash.java
        cat LongHash.java | sed -e "s/Long/Int/g" -e "s/long/int/g" > IntHash.java
        chmod u-w IntHash.java
    */
    public static class Set {
        public int len, nbits, size, zero;
        private int mask, wrapMask;

        /**
         * hashmap of longs
         * open addressing
         * power of 2 size: $nbits
         * no resizing
         * 
         */
        public Set init(int $nbits) {
            nbits = $nbits;
            len = 1 << nbits;
            mask = len-1;
            wrapMask = 1<<(nbits-1);
            zero = zeropos[nbits];
            init();
            // fixme::opt -- should be able to calculate the range over which anti --> magic
            //   instead of doing it inside fill()
            return this;
        }
        public void init() {}
        public int size() { return size; }
        public boolean isEmpty() { return size==0; }

        /** is k1 "after" k2 ... factoring in wrap-around */
        final boolean gte(int k1,int k2) {
            int delta = k1-k2;
            // d > thresh --> false
            // d >= 0     --> true
            // d < -thresh --> true
            // d < 0       --> false
            // check vs wrapMask instead, it's quicker than ifs (8M put/rem:  2.197 vs 2.226)
            int bits = delta & wrapMask;
            return bits==0; 
        }
        public final int mask(int hash) { return hash&mask; }
        public final int index(long key) { return hash(key)&mask; }
        public final int next(int index) { return (index+1)&mask; }
        public final int prev(int index) { return (index-1)&mask; }
    }
    
    public static class TestSet extends CacheTest.Tester {
        public LongHash.Set tree;
        boolean map = true;
        { name = "F2"; }
        public void initMap() {
            tree = new LongHash.Set();
            if (map) tree = new LongHash.Map();
            tree.init( cbits+2 );
        }
        public boolean lookup(long val) { return tree.contains( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.contains( leading );
                boolean haslag  = tree.contains( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging );
            }
            tree.put( leading );
        }
    }
    public static class TestMap extends CacheTest.Tester {
        public LongHash.Map<Long> tree;
        { name = "FM"; }
        public void initMap() { tree = new LongHash.Map<Long>().init( cbits+2 ); }
        public boolean lookup(long val) { return tree.contains( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.contains( leading );
                boolean haslag  = tree.contains( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                long val = tree.rag( lagging );
                if (val != lagging) ok = false;
            }
            tree.put( leading, leading );
        }
    }
    public static void test(CacheTest.Tester dup,Long seed) throws Exception {
        TaskTimer tt = new TaskTimer().config(1).init( 4, 4, true, true );
        tt.width = 5;
        tt.dec = 3;

        org.srlutils.Rand.source.setSeed( seed, true );
        tt.autoTimer( 
                dup.dup().setup( 12, 1<<22,  0 ),
                dup.dup().setup( 16, 1<<22,  0 ),
                dup.dup().setup( 20, 1<<22,  0 )
        );
        org.srlutils.Rand.source.setSeed( null, true );
        tt.autoTimer( 
                dup.dup().setup( 12, 1<<22,  0 ),
                dup.dup().setup( 16, 1<<22,  0 ),
                dup.dup().setup( 20, 1<<22,  0 )
        );
    }
    
    public static void mainSet(String [] args) throws Exception {
        TestSet set = new TestSet();
        TestMap map = new TestMap();
        CacheTest.TestJava java = new CacheTest.TestJava();
        set.map = false;
        Long seed = -8082837079645001398L;
        test(set,seed);
        test(map,seed);
        test(java,seed);
    }
    
    public static void main(String [] args) throws Exception {
  
        if (true) mainSet(null);
        
        // build the antimagic array ... need to rerun if the hash changes
        if (false) buildAntiMagic();
        
        
        TaskTimer tt = new TaskTimer().config(1).init( 4, 4, true, true );
        tt.width = 5;
        tt.dec = 3;

        // fixme -- need a test that actually hits the anti magic
        if (false) tt.autoTimer(
                new TestLH().setup( 14, 1<<26,  0 )
        );

        org.srlutils.Rand.source.setSeed( -8082837079645001398L, true );
        tt.autoTimer( 
                new CacheTest.TestL2().setup( 12, 1<<22,  0 ),
                new CacheTest.TestL2().setup( 16, 1<<22,  0 ),
                new CacheTest.TestL2().setup( 20, 1<<22,  0 )
        );
        org.srlutils.Rand.source.setSeed( null, true );
        tt.autoTimer( 
                new CacheTest.TestL2().setup( 12, 1<<22,  0 ),
                new CacheTest.TestL2().setup( 16, 1<<22,  0 ),
                new CacheTest.TestL2().setup( 20, 1<<22,  0 )
        );

    }

}


/*

 * longset and longmap performance, 2 runs each (F2 and FM) by running mainSet()
 *   and 1 of java HashMap (JV)
                      set     set     map     map    java
        F2.12.00  | 0.312   0.313   0.580   0.549   0.727   
        F2.16.00  | 0.603   0.650   1.147   1.341   1.331   
        F2.20.00  | 0.793   0.779   1.955   1.959   3.776   


















































*/