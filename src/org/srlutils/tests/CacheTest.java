// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.tests;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.srlutils.DynArray;
import org.srlutils.Rand;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import org.srlutils.btree.Bplus;
import org.srlutils.data.LongHashSet;
import org.srlutils.btree.Pourous;
import org.srlutils.data.TreeDisk;
import org.srlutils.hash.FixedHash;
import org.srlutils.hash.LongHash;

public class CacheTest {

    
    /** 
     * a test for timing a cache
     * we seed the cache with n1 values
     *   using the xorshift which produces unique values (period 2^64-1)
     * then loop thru n2 values
     *   removing seed[ii-n1], adding seed[ii], checking for seed[ii+1]
     */
    
    public static abstract class Tester extends TaskTimer.Runner<Integer> implements Cloneable {
        /** leading source */ public Rand.XorShift x1 = new Rand.XorShift();
        /** lagging source */ public Rand.XorShift x2 = new Rand.XorShift();
        /** null    source */ public Rand.XorShift x3 = new Rand.XorShift();
        public int cacheSize, cbits;
        public int nn, nc;
        public int seed;
        public boolean ok;
        /** a short name describing the test        */  public String name;

        public Tester dup() {
            try { return (Tester) this.clone(); }
            catch (Exception ex) { throw Simple.Exceptions.rte(ex); }
        }
        
        /** if alloc, allocate new resources, else clean them up */
        public abstract void initMap();
        /** return a blurb describing the test */
        public String info() { return String.format( "%2s.%02d.%02d", name, cbits, nc ); }
        { stageNames = "run".split( " " ); }

        public void init() {
            initMap();
            seed = Rand.irand();
            x1.seed( seed );
            x2.seed( seed );
            x3.seed( seed );
            for (int ii = 0; ii < cacheSize; ii++) check( null, x1.next() );
            for (int ii = 0; ii < cacheSize+nn; ii++) x3.next();
            ok = true;
        }

        // x1 and x2 give the same series of unique values ... x1 leads by cacheSize
        // check that the leading value isn't already present
        // remove the lagging value
        // add the leading value
        public void run(int stage) throws Exception {
            for (int ii = 0; ii < nn; ii++) {
                long lagging = x2.next();
                long leading = x1.next();
                for (int jj = 0; jj < nc; jj++)
                    ok &= ! lookup(x3.next());
                check(lagging,leading);
            }
        }
        // i've played with integrating with the run loop ... appears to hurt performance
        public abstract void check(Long lagging,long leading);
        public abstract boolean lookup(long val);

        public boolean finish() throws Exception {
            initMap();
            return ok;
        }

        /** set the number of bits of cache and number of elements to test */
        public Tester setup(int $cbits,int $nn, int $nc) {
            cbits = $cbits;
            cacheSize = 1<<cbits;
            nn = $nn;
            nc = $nc;
            super.setup( stageNames.length, info() );
            return this;
        }
    }
    public static class TestTD extends Tester {
        public TreeDisk<Long,Void> tree;
        { name = "TD"; }
        public void initMap() { tree = new TreeDisk.ComparableSet(); }
        public boolean lookup(long val) { return tree.get(val) != null; }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                ok &= tree.get( leading )==null;
                long val = tree.remove( lagging );
                ok &= (val==lagging);
            }
            tree.put( leading );
        }
    }
    
    
    public static class TestTM extends Tester {
        public TreeMap<Long,Void> tree;
        { name = "TM"; }
        public void initMap() { tree = new TreeMap(); }
        public boolean lookup(long val) { return tree.containsKey( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.containsKey( leading );
                boolean haslag  = tree.containsKey( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging );
            }
            tree.put( leading, null );
        }
    }
    public static class TestHM extends Tester {
        public HashMap<Long,Void> tree;
        { name = "HM"; }
        public void initMap() { tree = new HashMap(4*cacheSize); }
        public boolean lookup(long val) { return tree.containsKey( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.containsKey( leading );
                boolean haslag  = tree.containsKey( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging );
            }
            tree.put( leading, null );
        }
    }
    public static class TestPA extends Tester {
        public Pourous.Worker tree;
        public Pourous.SubWorker.L ww;
        { name = "PA"; }
        public void initMap() {
            if (ww != null) { ww.clean(); ww = null; return; }
            int mult = 2;
            ww = new Pourous.SubWorker.L();
            tree = ww;
            tree.init(cacheSize*mult,7);
        }
        public boolean lookup(long val) {
            ww.set(val);
            return tree.find(false) >= 0;
        }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                ww.set(leading);
                boolean haslead = tree.match() >= 0;
                ww.set(lagging);
                boolean haslag  = tree.remove();
                if (haslead || !haslag)
                    ok = false;
                haslag = tree.match() >= 0;
                if (haslag)
                    ok = false;
            }
            ww.set(leading);
            tree.insert();
        }
    }
    public static class TestCH extends Tester {
        private Object mynull = new Object();
        public ConcurrentHashMap<Long,Object> tree;
        { name = "CH"; }
        public void initMap() { tree = new ConcurrentHashMap(2*cacheSize); }
        public boolean lookup(long val) { return tree.containsKey( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.containsKey( leading );
                boolean haslag  = tree.containsKey( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging );
            }
            tree.put( leading, mynull );
        }
    }
    public static class TestC2 extends Tester {
        private Object mynull = new Object();
        public org.srlutils.data.ConcurrentHashMap<Long,Object> tree;
        { name = "C2"; }
        public void initMap() { tree = new org.srlutils.data.ConcurrentHashMap(2*cacheSize); }
        public boolean lookup(long val) { return tree.containsKey( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.containsKey( leading );
                boolean haslag  = tree.containsKey( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging );
            }
            tree.put( leading, mynull );
        }
    }
    public static class TestLH extends Tester {
        public LongHashSet tree;
        { name = "LH"; }
        public void initMap() { tree = new LongHashSet( cbits+2 ); }
        public boolean lookup(long val) { return tree.containsKey( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.containsKey( leading );
                boolean haslag  = tree.containsKey( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                tree.remove( lagging );
            }
            tree.put( leading );
        }
    }
    public static class TestL2 extends Tester {
        public LongHash.Set tree;
        { name = "L2"; }
        public void initMap() { tree = new LongHash.Set().init( cbits+2 ); }
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
    public static class TestJava extends Tester {
        public HashMap<Long,Long> tree;
        { name = "JV"; }
        public void initMap() { tree = new HashMap(4*cacheSize); }
        public boolean lookup(long val) { return tree.containsKey( val ); }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                boolean haslead = tree.containsKey( leading );
                boolean haslag  = tree.containsKey( lagging ); // extra work
                if (haslead || !haslag) ok = false;
                long val = tree.remove( lagging );
                if (val != lagging) ok = false;
            }
            tree.put( leading, leading );
        }
    }
    public static class TestBT extends Tester {
        public Bplus.SetLong tree;
        Bplus.SetLong.Data cd = new Bplus.SetLong.Data();
        { name = "BT"; }
        public void initMap() { (tree = new Bplus.SetLong()).init(); }
        public boolean lookup(long val) { tree.findData(cd.set(val,0)); return cd.match; }
        public void check(Long lagging,long leading) {
            if (lagging != null) {
                tree.findData( cd.set(leading,0) );
                if (cd.match) ok = false;
                tree.remove( cd.set(lagging,0) );
                if (!cd.match || cd.val != lagging) ok = false;
            }
            tree.insert( cd.set(leading,leading) );
        }
    }

    /**
     *  an simple array - too slow to be practical for anything more than a few bits of cache
     *  linear search over the entire array
     *  append at the end
     *  remove by moving the end to the key location
     *  however, a hybrid might work
     *  backed by an array cat of sorted a1[cachesize] + random a2[delta]
     *  binary search over a1, linear over a2
     *  insert by appending to a2, merge with a1 when full
     *  remove:
     *    if in a1, copy values from the right - if too many wholes, merge with a2
     *    if in a2, move the end of a2 to key location
     *  the advantage of this over a hashmap is that primitives don't need to be wrapped
     *  should use a hashmap with open addressing instead ...
     */
    public static class TestAr extends Tester {
        public long [] tree;
        int nt;
        { name = "Ar"; }
        public boolean lookup(long val) { return lookup2(val) < cacheSize; }
        int lookup2(long val) {
            int index = 0;
            while (index < cacheSize && tree[index] != val) index++;
            return index;
        }
        public void initMap() { tree = new long[cacheSize]; nt = 0; }
        public void check(Long lagging,long leading) {
            int klead = lookup2(leading);
            if (lagging != null) {
                int klag = lookup2(lagging);
                if (klead < cacheSize || klag==cacheSize) ok = false;
                tree[klag] = tree[--nt];
            }
            tree[nt++] = leading;
        }
    }
    
    
    public static void main(String [] args) throws Exception {
        Simple.Scripts.cpufreqStash( 1800000 );
        // all randomness should cascade from source, so setting seed to non-null should be deterministic
        Long seed = null;
        org.srlutils.Rand.source.setSeed( seed, true );
        int n2 = 1 << 22;


        TaskTimer tt = new TaskTimer().config(1).init( 4, 4, true, true );
        tt.width = 5;
        tt.dec = 3;

        int nc = 0;
        DynArray.Objects<Tester> da = new DynArray.Objects().init( Tester.class );
        for (int ii = 12; ii <= 20; ii+=4) {
//            da.add( new TestTD().setup( ii, n2, nc ) );
//            da.add( new TestTD().setup( ii, n2, 10 ) );
            da.add( new TestJava().setup( ii, n2,  0 ) );
            da.add( new FixedHash.TestMap().setup( ii, n2,  0 ) );
//            da.add( new TestHM().setup( ii, n2,  0 ) );
//            da.add( new TestCH().setup( ii, n2,  0 ) );
//            da.add( new TestLH().setup( ii, n2,  10 ) );
//            da.add( new TestHM().setup( ii, n2, 10 ) );
//            da.add( new TestCH().setup( ii, n2, 10 ) );
//            if (ii < 10) da.add( new TestAr().setup( ii, n2, nc ) );
//            da.add( new TestBT().setup( ii, n2, nc ) );
//            da.add( new TestTM().setup( ii, n2, nc ) );
        }

        if (true) tt.autoTimer(
                new TestPA().setup( 16, 1<<20, 0 )
                );
        
        // certain seeds (eg, 1L) seem to trigger more aggressive jit code for FixedHash
        // using that here for consistency with compilation
        org.srlutils.Rand.source.setSeed( 1L, true );
        tt.autoTimer( da.trim() );
        org.srlutils.Rand.source.setSeed( null, true );
        tt.autoTimer( da.trim() );
    }
}


/*

 * performance as a function of cache size and number of iters
 * each iter is a lookup, remove, and insert
 *   with nc extra lookups
 * linear with iters, loggish with cache size
 * hashmap > btree > treedisk > treemap (because the data is random, would be better for seq)
 * the java maps (tree,hash) have to do an extra lookup since the remove methods don't indicate success
 * abstr: using abstractMap (backed by a hashmap) slows things down vs hashmap about 30%

            512k        TD       HM       BT       TM    abstr
            --------------------------------------------------
            TD.08  | 0.246    0.070    0.218    0.277    0.091
            TD.11  | 0.372    0.096    0.278    0.477    0.120
            TD.14  | 0.754    0.129    0.364    1.089    0.154

 * cost of reads ... 1M put/del, 10M reads, treedisk vs hashmap
 *   lookups are relatively cheap ... .36 seconds per million at 13 bit cache treedisk, .08s for hashmap
 *                   TD.0   TD.10  delta     HM.0   HM.10   HM.delta
        TD.08.00  | 0.533   1.667    1.1    0.192   0.721   
        TD.13.00  | 1.205   4.859    3.6    0.222   1.005   .8 sec per 10 million reads
        TD.14.00  | 1.591   7.160    5.5    0.251   1.223   

 * hashmap vs concurrentHashmap single-threaded performance, 19 bits of insert
 *   tested over 19 to 22 bits of inserts
 *   for small caches (less than 18 bits) of write-heavy usage, HM seems better
 *   for read-heavy usage or large caches (20 bits or more) they're about even
 *   doug lea's version (srl.data.CHM) *might* be a little faster than CHM for write-heavy
 * 
 * 19 bits - read heavy
 *                     HM     CHM      DL
        HM.14.10  | 0.605   0.586   0.698   
        HM.16.10  | 1.027   0.997   1.030
        HM.18.10  | 1.313   1.274   1.288

 * 22 bits of insert - write heavy
 *                     HM      LH   HM.10   LH.10      CH      DL  DL.pur
        HM.12.00  | 1.081   1.612   3.460   3.677   
        HM.14.00  | 1.206   1.776   5.520   4.826   2.036   1.749   1.475   
        HM.16.00  | 1.687   2.328   7.891   7.446   2.434   2.247   1.863   
        HM.18.00  | 2.306   2.361  10.426   9.926   2.880   2.757   2.218   
        HM.20.00  | 3.479   3.563  14.116  11.994   
        HM.21.00  | 6.994   5.100  16.611  17.633   

 *   HM with 4*cacheSize (vs 2x) initial size, 22 bits write-heavy - no change
 *                   HM*2    HM*4                 HM*2    HM*4
        LH.19.00  | 2.961   2.842    LH.21.00  | 6.332   7.096   


 * 23 bits - lhs.gte: ifs vs bits (25% fill) vs bits (50% fill)
 *    bits are faster, 50% fill slows things down significantly (still faster than HM)
 *    HM was run with 4*cache
 *    assume that high HM variability is due to gc (LH is usually around 0.03)
 *    each column is pure, ie was run by itself
 * 
 *                    ifs    bits     50%      HM  HM.std
        LH.12.00  | 1.103   1.072   1.587   1.984   0.228
        LH.16.00  | 1.759   1.740   2.278   3.301   0.239
        LH.20.00  | 2.226   2.197   3.126   7.361   0.302

 *                     LH      HM      CH  LH.pur
        LH.12.10  | 4.039   6.068   8.227   3.281
        LH.16.10  | 8.790  12.887  16.407   6.477
        LH.20.10  |12.852  25.002  26.217   8.958

 * 
 * 19 bits of insert, 70 reads per put/rem ... simulating demo2.Build
 *    LH.pur is an LH-only run (presumably some gc is spilling from TD to LH)
 *    as TD cache gets larger, performance drops dramatically
 * 
 *                     TD     LH  LH.pur
        TD.12.70  | 8.866  1.273   0.999   
        TD.14.70  |19.685  1.686   1.210   
        TD.16.70  |35.548  3.066   2.097   
        TD.18.70  |50.883  4.046   2.642   
        TD.20.70  |59.492  4.615   2.964   

 * java hashmap (TestJava) vs fixed map (FixedHash.TestMap)
 *   21 bits of inserts, nc = 0 or 10, 22 bits of insert
 *   fixed is faster
 *     java uses autoboxing for the key, fixed does not
 *     smaller and linear scan are cache friendly
 * 
                       jv      fm    jv10    fm10   22.jv   22.fm
        JV.12.00  | 0.651   0.426   1.971   1.257   1.221   0.915   
        JV.16.00  | 1.210   0.817   4.019   2.858   2.006   1.586   
        JV.20.00  | 2.372   1.229   7.541   4.085   4.879   2.666   

* 
* Pourous Array
*   allows for iteration so equiv to a tree, not a hash
*   TestPA(18,2^18,0) was 20% slower than TestTM which is pretty reasonable
*     which is much slower than the hash based tests
*   16 bits of cache, 20 bits of data --> 2.26 seconds
*     includes an additional lookup that the key has actually been removed
 
 
 
 */

