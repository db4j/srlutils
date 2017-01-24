// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.util.TreeMap;
import org.srlutils.Rand;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;



/** automatic tests of the btree with different providers, and comparison to java's TreeMap */
public abstract class BtTests2 extends TaskTimer.Runner<BtTests2.Config> {
    /** a short name describing the test        */  public String name;
    /** config                                  */  public Config tc;
    /** the keys to add to the mappings         */  public double [] keys;

    public String sntext = "put look";
    public void alloc() { stageNames = sntext.split( " " ); setup( stageNames.length, name ); }
    public void config(Config $tc) { tc = $tc; }

    /** any test-specific initialization  */    public abstract void init2();
    public void init() {
        keys = Rand.rand( tc.nn + 1 );
        init2();
    }
    public static class Config {
        /** the number of insertions nn and the number of inserts per commit ns */
        public int nn;
        public Config set(int $nn) { nn = $nn; return this; }
    }
    public void check(int ii,float kk, float val) {
        if ( kk != val )
            System.out.format( "value mismatch %8d -- key %8.3f --> %8.3f != %8.3f\n",
                    ii, keys[ii], kk, val );
    }
    public static void auto(Long seed,int nn,int passes,int npp,TaskTimer.Runner... runners) throws Exception {
        Rand.source.setSeed( seed, true );
        Simple.Scripts.cpufreqStash( 2300000 );
        BtTests2.Config tc = new BtTests2.Config().set(nn);
        TaskTimer tt = new TaskTimer().config( tc ).init(npp, 0, true, true);
        tt.widths(5,3);
        for (int ii=0; ii<passes; ii++)
            tt.autoTimer(runners);
    }

    
    /** base class for comparing memory based trees without kilim */
    public static abstract class Nokil extends BtTests2 {
        /** lookup the given key              */    public abstract float look(double key);
        /** insert the key/val pair using txn */    public abstract void put(double key,float val);
        public void run(int stage) throws Exception {
            if (stage == 0) insert();
            else if (stage == 1) lookup();
        }
        public void lookup() {
            for (int ii = 0; ii < tc.nn; ii++) {
                float kk = look( keys[ii] );
                check(ii,kk, 0.01F * ii);
            }
        }
        public void insert() {
            for (int ii = 0; ii < tc.nn; ii++) put( keys[ii], 0.01f*ii );
        }
    }
    /** java's builtin treemap */
    public static class Java extends Nokil {
        TreeMap<Double,Float> map;
        { name = "TreeMap"; }
        public void init2() { map = new TreeMap(); }
        // note: tried implementing insert/lookup to bypass the lt.prov stuff
        // ... no statistically significant difference in performance
        public float look(double key) { return map.get( key ); }
        public void put(double key,float val) { map.put( key, val ); }
        public boolean finish() throws Exception { keys = null; map = null; return true; }
    }
    /**  plus and prim are very similar - only major difference */
    public static class Plus extends Nokil {
        Bplus.DF map;
        Bplus.DF.Data cc = new Bplus.DF.Data();
        { name = "nokil.plus"; }
        public void init2() { map = new Bplus.DF(); map.init(); }
        public float look(double key) {
            return map.findData( cc.set(key,-1f) ).val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
        }
    }
    public static class Minus extends Nokil {
        Bminus.DF map;
        Bminus.DF.Data cc = new Bminus.DF.Data();
        { name = "minus.mem"; }
        public void init2() { map = new Bminus.DF(); map.init(); }
        public float look(double key) {
            return map.findData( cc.set(key,-1f) ).val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
        }
    }
    public static class Mina extends Nokil {
        Bminus.DFa map;
        Bminus.DFa.Data cc = new Bminus.DFa.Data();
        { name = "minus.array"; }
        public void init2() { map = new Bminus.DFa(); map.init(); }
        public float look(double key) {
            return map.findData( cc.set(key,-1f) ).val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
        }
    }
    public static class Mindir extends Nokil {
        Bminus.DFd map;
        Bminus.DFd.Data cc = new Bminus.DFd.Data();
        { name = "minus.direct"; }
        public void init2() { map = new Bminus.DFd(); map.init(); }
        public float look(double key) {
            return map.findData( cc.set(key,-1f) ).val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
        }
        public void kiss() { map.clear(); }
    }
    public static class Pour extends Nokil {
        Bminus.DFp map;
        Bminus.DFp.Data cc = new Bminus.DFp.Data();
        { name = "minus.pour"; }
        public void init2() { map = new Bminus.DFp(); map.init(); }
        public float look(double key) {
            return map.findData( cc.set(key,-1f) ).val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
        }
        public void kiss() { map.clear(); }
    }
    
    public static class Demo {
        public static void main(String [] args) throws Exception {
            int nn = 1000000;
            Config tc = new BtTests2.Config().set( nn);
            int kk = 4;
            if (kk==4) {
                TaskTimer tt = new TaskTimer().config( tc ).init( 4, 2, true, true );
//                tt.autoTimer( new Java() );
                tt.autoTimer( new Mina(), new Mindir() );
            }
        }
    }


}

