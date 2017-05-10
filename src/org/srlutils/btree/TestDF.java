// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.util.Arrays;
import org.srlutils.Rand;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import org.srlutils.Types;
import org.srlutils.Util;
import org.srlutils.btree.Bstring.ValsString;
import org.srlutils.btree.Bpage.Sheet;
import org.srlutils.btree.Btypes.ValsTuple;
import org.srlutils.btree.Btypes.ValsVoid;

public class TestDF {
    /** an interface for accessing contexts that represent "double to float" maps (ie for testing) */
    public interface DFcontext<CC extends DFcontext> {
        public CC set(double key);
        public CC set(double key,float val);
        public float val();
    }
    public interface AFcontext<CC extends AFcontext> {
        public CC set(byte [] key);
        public CC set(byte [] key,float val);
        public float val();
    }
    public interface SFcontext<CC extends SFcontext> {
        public CC set(String key);
        public CC set(String key,float val);
        public float val();
    }
    public interface IIcontext<CC extends IIcontext> {
        public CC set(int key);
        public CC set(int key,float val);
        public float val();
    }
    public static abstract class Base extends TaskTimer.Runner<Base.Config> {
        /** a short name describing the test        */  public String name;
        /** config                                  */  public Config tc;

        public String sntext = "put look rem chk";
        boolean ok;
        public void alloc() { stageNames = sntext.split( " " ); setup( stageNames.length, name ); }
        public void config(Config $tc) { tc = $tc; }
        public boolean finish() { return ok; }

        public static class Config {
            /** the number of insertions nn */
            public int nn;
            public Config set(int $nn) { nn = $nn; return this; }
        }
    }

    public static class Tester<CC extends Btree.Context & DFcontext<CC>,TT extends Bface<CC>>
    extends Base {
        TT map;
        CC cc;
        double [] keys;
        public Tester(TT $map) { map = $map; name = map.getClass().getSimpleName(); }
        public void init() { keys = Rand.rand(tc.nn+1); cc = map.context(); map.init(cc); ok = true; }
        public void kiss() { map.clear(); }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final float v1 = 0.01f*jj;
                final boolean chk = stage==2 && false;
                if (chk)
                    Simple.nop();
                if      (stage==0) map.insert(cc.set(keys[jj],v1));
                else if (stage==2) {
                    map.remove(cc.set(keys[jj]));
                    Simple.softAssert(cc.match & cc.val()==v1,"Mindir.remove.nomatch %d %f",jj,keys[jj]);
                }
                else {
                    map.findData(cc.set(keys[jj]));
                    boolean aok = true;
                    if (cc.val() != (stage==1 ? v1:-1f)) ok = aok = false;
                    if (!aok)
                        System.out.format( "not ok %3d:%5d ... %8.3f --> %8.3f\n",
                                stage, jj, keys[jj], cc.val() );
                }
                if (chk) {
                    check(0,tc.nn,1,jj,stage);
                    System.out.format( "chk.2 completed -- %5d\n", jj );
                }
            }
        }
        public void check(int ko,int nn,int delta,int jj,int stage) {
            map.findData(cc.set(keys[51788]));
            for (int kk = ko; kk < nn; kk += delta) {
                cc.set(keys[kk]);
                map.findData(cc);
                float goal = stage==0
                        ? (kk >  jj) ? -1f : 0.01f*kk
                        : (kk <= jj) ? -1f : 0.01f*kk;
                Float val = cc.match ? cc.val() : -1f;
                if (val != goal)
                    Simple.softAssert(false,"insert corrupted: %d, %d, %8.3f --> %8.3f <> %8.3f\n",
                            jj,kk,keys[kk],val,goal);
            }
        }
    }
    public static class Testeri<CC extends Btree.Context & IIcontext<CC>,TT extends Bface<CC>> 
    extends Base {
        TT map;
        CC cc;
        int [] keys;
        int collisions;
        public Testeri(TT $map) { map = $map; name = map.getClass().getSimpleName(); }
        public void init() { keys = Rand.irand(tc.nn+1); cc = map.context(); map.init(cc); ok = true; }
        public boolean finish() {
            long num = tc.nn, range = 1L<<32, max = (num*num+range-1)/range;
            if (collisions > max) {
                System.out.format("WARNING !!! -- collisions exceeded: %5d of %5d allowed\n", collisions, max);
                return false;
            }
            return ok;
        }
        public void kiss() { map.clear(); collisions = 0; }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final float v1 = 0.01f*jj, goal = stage==3 ? -1f:v1;
                if      (stage==0) map.insert  (cc.set(keys[jj],v1));
                else if (stage==2) map.remove  (cc.set(keys[jj]));
                else               map.findData(cc.set(keys[jj]));
                if (stage > 0 && cc.val() != goal) {
                    int kk = (int) Math.round(cc.val()*100);
                    if (cc.match & kk >= 0 & kk < keys.length && keys[kk]==keys[jj])
                        collisions++;
                    else
                        ok = false;
                }
            }
        }
    }
    public static class Testera<CC extends Btree.Context & AFcontext<CC>,
                                TT extends Btree.DirectMap<CC> & Bface<CC>> 
                        extends Base
    {
        TT map;
        CC cc;
        byte [][] keys;
        public Testera(TT $map) { map = $map; name = map.getClass().getSimpleName(); }
        public void init() {
            double [] keys2 = Rand.rand(tc.nn+1);
            keys = new byte[tc.nn+1][];
            for (int ii = 0; ii < tc.nn+1; ii++)
                keys[ii] = Bstring.getBytes(keys2[ii]+"");
            cc = map.context();
            map.init(cc);
            ok = true;
        }
        public void kiss() {
            System.out.format(map.info());
            map.clear();
        }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final float v1 = 0.01f*jj, goal = stage==3 ? -1f:v1;
                if      (stage==0) map.insert  (cc.set(keys[jj],v1));
                else if (stage==2) map.remove  (cc.set(keys[jj]));
                else               map.findData(cc.set(keys[jj]));
                if (stage > 0 && cc.val() != goal)
                    ok = false;
            }
        }
    }
    public static class Testers<CC extends Btree.Context & SFcontext<CC>,
                                TT extends Btree.DirectMap<CC> & Bface<CC>> 
                        extends Base
    {
        TT map;
        CC cc;
        String [] keys;
        boolean iterate = false;
        public Testers(TT $map) { map = $map; name = map.getClass().getSimpleName(); }
        public void init() {
            double [] keys2 = Rand.rand(tc.nn+1);
            keys = new String[tc.nn+1];
            for (int ii = 0; ii < tc.nn+1; ii++)
                keys[ii] = keys2[ii]+"";
            cc = map.context();
            map.init(cc);
            ok = true;
        }
        public int iterate() {
            int count = 0;
            String [] dup = Util.dup(keys,0,tc.nn);
            Arrays.sort(dup);
            SSmeta.Data data = (SSmeta.Data) cc;
            for (Btree.Range range = map.getall(cc); range.next(); count++)
                if (! data.key.equals(dup[count])) {
                    map.findData(cc.set(dup[count]));
                    Simple.softAssert(false);
                }
            System.out.format( "%s, %d entries of %d\n", map.info(), count, tc.nn );
            return count;
        }
        public void kiss() {
            if (cc instanceof SSmeta.Data & iterate) iterate();
            map.clear();
        }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final float v1 = 0.01f*jj, goal = stage==3 ? -1f:v1;
                if      (stage==0) map.insert  (cc.set(keys[jj],v1));
                else if (stage==2) map.remove  (cc.set(keys[jj]));
                else               map.findData(cc.set(keys[jj]));
                if (stage > 0 && cc.val() != goal)
                    ok = false;
            }
        }
    }
    public static class Tester2<CC extends Btree.Context & DFcontext<CC>,TT extends Bface<CC>> 
    extends Tester<CC,TT> {
        Tester2(TT $map) { super($map); }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final float v1 = 0.01f*jj, goal = stage==3 ? -1f:v1;
                if      (stage==0) map.insert  (cc.set(keys[jj],v1));
                else if (stage==2) map.remove  (cc.set(keys[jj]));
                else               map.findData(cc.set(keys[jj]));
                if (stage > 0 && cc.val() != goal) ok = false;
            }
        }
    }
    public static class Tester3<CC extends Btree.Context & DFcontext<CC>,TT extends Bface<CC>> 
    extends Tester<CC,TT> {
        Tester3(TT $map) { super($map); }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final float v1 = 0.01f*jj, goal = stage==3 ? -1f:v1;
                if      (stage==0) map.insert  (cc.set(keys[jj],v1));
                else if (stage==2) map.remove  (cc.set(keys[jj]));
                else               map.findData(cc.set(keys[jj]));
                if (stage > 0 && cc.val() != goal) ok = false;
            }
        }
    }
    public static class Tester4<CC extends Btree.Context & DFcontext<CC>,TT extends Btree.DirectMap<CC> & Bface<CC>> 
    extends Tester<CC,TT> {
        Tester4(TT $map) { super($map); }
        public void run(final int stage) {
            for (int jj = 0; jj < tc.nn; jj++) {
                final double key = keys[jj];
                final float v1 = 0.01f*jj, goal = stage==3 ? -1f:v1;
                if      (stage==0) map.insert  (cc.set(key,v1));
                else if (stage==2) map.remove  (cc.set(key));
                else               map.findData(cc.set(key));
                if (stage > 0 && cc.val() != goal) ok = false;
            }
        }
        public void kiss() {
            System.out.format(map.info());
            map.clear();
        }
    }
    
    public static class Itree {
        public static class Data extends Btree.Context implements TestDF.IIcontext {
            public int key;
            public float val;
            public Data set(int $key,float $val) { key = $key; val = $val; return this; }
            public Data set(int key) { return set(key,-1f); }
            public float val() { return match ? val:-1f; }
        }
        public static class IF extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._int.size,Types.Enum._float.size); }
            public void setcc(Sheet page,Data cc,int ko) { page.put(pkey,ko,cc.key);  page.put(pval,ko,cc.val);  }
            public void getcc(Sheet page,Data cc,int ko) { cc.key=page.geti(pkey,ko); cc.val=page.getf(pval,ko); }
            int key(Sheet page,int index) { return page.geti(pkey,index); }
            protected int compare(Sheet page,int index,Data data) { return Butil.compare(data.key,key(page,index)); }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
        public static class IF2 extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._int.size,Types.Enum._float.size); }
            public void setcc(Sheet page,Data cc,int ko) { page.put(pkey,ko,cc.key);  page.put(pval,ko,cc.val);  }
            public void getcc(Sheet page,Data cc,int ko) { cc.key=page.geti(pkey,ko); cc.val=page.getf(pval,ko); }
            int key(Sheet page,int index) { return page.geti(pkey,index); }
            protected int compare(Sheet page,int index,Data data) { return Butil.compare(data.key,key(page,index)); }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
    }
    public static class Dtree {
        public static class Data extends Btree.Context implements TestDF.DFcontext {
            public double key;
            public float val;
            public Data set(double $key,float $val) { key = $key; val = $val; return this; }
            public Data set(double key) { return set(key,-1f); }
            public float val() { return match ? val:-1f; }
        }
        public static class DF extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._double.size,Types.Enum._float.size); }
            public void setcc(Sheet page,Data cc,int ko) {
                page.put(pkey,ko,cc.key);
                page.put(pval,ko,cc.val);
            }
            public void getcc(Sheet page,Data cc,int ko) {
                cc.key = page.getd(pkey,ko);
                cc.val = page.getf(pval,ko);
            }
            double key(Sheet page,int index) { return page.getd(pkey,index); }
            protected int compare(Sheet page,int index,Data data) {
                return Butil.compare(data.key,key(page,index));
            }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
        public static class DF2 extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._double.size,Types.Enum._float.size); }
            public void setcc(Sheet page,Data cc,int ko) {
                page.put(pkey,ko,cc.key);
                page.put(pval,ko,cc.val);
            }
            public void getcc(Sheet page,Data cc,int ko) {
                cc.key = page.getd(pkey,ko);
                cc.val = page.getf(pval,ko);
            }
            double key(Sheet page,int index) { return page.getd(pkey,index); }
            protected int compare(Sheet page,int index,Data data) {
                return Butil.compare(data.key,key(page,index));
            }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
        public static class DF3 extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._double.size,Types.Enum._float.size); }
            public void setcc(Sheet page,Data cc,int ko) {
                page.put(pkey,ko,cc.key);
                page.put(pval,ko,cc.val);
            }
            public void getcc(Sheet page,Data cc,int ko) {
                cc.key = page.getd(pkey,ko);
                cc.val = page.getf(pval,ko);
            }
            double key(Sheet page,int index) { return page.getd(pkey,index); }
            protected int compare(Sheet page,int index,Data data) {
                return Butil.compare(data.key,key(page,index));
            }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
        public static class DF4 extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._double.size,Types.Enum._float.size); }
            public void setcc(Sheet page,Data cc,int ko) {
                page.put(pkey,ko,cc.key);
                page.put(pval,ko,cc.val);
            }
            public void getcc(Sheet page,Data cc,int ko) {
                cc.key = page.getd(pkey,ko);
                cc.val = page.getf(pval,ko);
            }
            double key(Sheet page,int index) { return page.getd(pkey,index); }
            protected int compare(Sheet page,int index,Data data) {
                return Butil.compare(data.key,key(page,index));
            }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
        public static class DD extends Btree.DirectMap<Data> implements Bface<Data> {
            { init(Types.Enum._double.size,Types.Enum._double.size); }
            public void setcc(Sheet page,Data cc,int ko) {
                page.put(pkey,ko,cc.key);
                page.put(pval,ko,(double)cc.val);
            }
            public void getcc(Sheet page,Data cc,int ko) {
                cc.key = page.getd(pkey,ko);
                cc.val = (float) page.getd(pval,ko);
            }
            double key(Sheet page,int index) { return page.getd(pkey,index); }
            protected int compare(Sheet page,int index,Data data) {
                return Butil.compare(data.key,key(page,index));
            }
            protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = compare( page, k1, context );
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
    }
    
    public static class Dmeta {
        public static class Data extends Bmeta.Context<Double,Float,Data> implements DFcontext {
            public Data set(double key) { return super.set(key,-1f); }
            public Data set(double key,float val) { return super.set(key,val); }
            public float val() { return val; }
        }
        public static class DF extends Bmeta<Data,Double,Float,Btypes.ValsDouble>  implements Bface<Data> {
            public DF() { setup(new Btypes.ValsDouble(),new Btypes.ValsFloat()); }
            public int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = keys.compare(context.key,page,k1,null);
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
            public Data context() { return new Data(); }
        }
    }
    public static class SSmeta {
        public static class Data extends Bmeta.Context<String,Float,Data>
                implements DFcontext<Data>, SFcontext<Data> {
            public Data set(double  key)            { return set(key+"",-1f); }
            public Data set(double  key,float $val) { return set(key+"",$val); }
            public Data set(String  key)            { return set(key,-1f); }
            public Data set(String $key,float $val) { key = $key; val = $val; return this; }
            public float val() { return match ? val : -1f; }
        }
        public static class DF extends Bmeta<Data,String,Float,Bstring.ValsString> {
            { setup(new Bstring.ValsString(),new Btypes.ValsFloat()); }
            public Data context() { return new Data(); }
        }
        public static class Cheese extends Bmeta<Data,String,Float,Bstring.ValsCheese> {
            { setup(new Bstring.ValsCheese(),new Btypes.ValsFloat()); }
            public Data context() { return new Data(); }
        }
    }
    public static class VVmeta {
        // works with either Tester, Testers, or Testera
        public static class Data extends Bmeta.Context<byte [],Float,Data>
                implements TestDF.AFcontext<Data>, TestDF.DFcontext<Data>, SFcontext<Data> {
            public Data set(byte [] key) { return set(key,-1f); }
            public Data set(byte [] $key,float $val) { key = $key; val = $val; return this; }
            public Data set(String key)             { return set(key            ,-1f); }
            public Data set(String $key,float $val) { return set(Bstring.getBytes($key),$val); };
            public Data set(double key)             { return set(key            ,-1f); }
            public Data set(double $key,float $val) { return set($key+""        ,$val); }
            public float val() { return match ? val : -1f; }
        }
        public static class DF extends Bmeta<Data,byte [],Float,Bstring.ValsBytes> {
            { setup(new Bstring.ValsBytes(),new Btypes.ValsFloat()); }
            public Data context() { return new Data(); }
        }
    }
    public static class SVmeta {
        public static class Data extends Bmeta.Context<Object[],Void,Data> implements DFcontext<Data> {
            public Data set(double key) { return super.set(new Object[]{key+"",null},null); }
            public Data set(double key,float val) { return super.set(new Object[]{key+"",val+""},null); }
            public float val() { return match ? Float.parseFloat((String) key[1]) : -1f; }
        }
        public static class DF extends Bmeta<Data,Object[],Void,ValsTuple> implements Bface<Data> {
            { setup(new ValsTuple(new ValsString(),new ValsString()),new ValsVoid()); }
            public Data context() { return new Data(); }
            public int findLoop2(Sheet page,int k1,int num,int step,Data context,boolean greater) {
                for (; k1<num; k1+=step) {
                    int cmp = keys.compare(context.key,page,k1,null);
                    if (greater & cmp==0) cmp = 1;
                    if (cmp <= 0) break;
                }
                if (step > 1)
                    return findLoop(page,k1-step,num,1,context,greater);
                return k1;
            }
        }
    }
    
    public static void auto(Long seed,int nn,int passes,int npp,TaskTimer.Runner... runners) throws Exception {
        Rand.source.setSeed( seed, true );
        Simple.Scripts.cpufreqStash( 2300000 );
        Base.Config tc = new Base.Config().set(nn);
        TaskTimer tt = new TaskTimer().config( tc ).init(npp, 0, true, true);
        tt.widths(5,3);
        for (int ii=0; ii<passes; ii++)
            tt.autoTimer(runners);
    }
    /*
     * trying to quantify the performance of the string maps
     *   ie, bisecting the differences between Btree.DF and SSmeta.DF
     *   times are 10^6 random doubles, put and look only (ie no remove)
     *   remove scales similarly
     *   Bmeta.val is ValsFloat in all cases
     * 
     * SSmeta.DF/Tester4 -- 3.98 guava compare
     * SSmeta.DF/Testers -- 3.08
     * VVmeta.DF/Testera -- 2.83
     * VVmeta.DF2/s/cmp4 -- 2.50 without zeropad
     * VVmeta.DF2/s/cmp4 -- 2.48 with    zeropad (cost of munging is counted)
     *           /s/cmp2 -- 2.43         zeropad 
     *           /s/cmp2 -- 2.16         zeropad (using the unsafe string copy)
     * VVmeta.DF2/a/cmp4 -- 2.30 without zeropad
     *                      2.06 with    zeropad (cost of munging is not counted)
     *             /cmp2 -- 1.95 (zeropad required) note: this loop just optimizes well (wtf?)
     * SS.cheese /s/cmp4 -- 2.56 pef=000
     *                      2.64 zeropad (munging + larger size costs more than the faster compare)
     *                      2.53 simplepad (ie, zeropad + simplified getBytes loop)
     *                      2.25 unsafepad + recycling (little endian)
     *                      2.57 unsafepad + recycling, but storing compact
     *                      2.42 unsafepad + recycling + big-endian
     *             cmp2     2.45 simplepad
     *                      2.29 unsafepad (ie, zeropad using unsafe string copy)
     *                      2.24 unsafepad + recycling
     * VVmeta.DF2/s/cmp2 -- 2.26 unsafepad
     * SSmeta.Raw/s/char -- 2.91 compare accesses the strings char [] internal rep byte by byte
     * SS.cheese /s/cmp4 -- 2.52 unsafe copy of String char [], stored as endian, unsigned compare, ie lexicographical
     *                      2.20   with zeropad
     * SSmeta.DF/s       -- 2.80 compare(reverse+flip)


     * the dynlen stuff isn't a problem ...
     * Tester4 + Dmeta.Big + ValsDouble4  : 2.01  -- 3 doubles per key, quick generation
     *                       ValsDouble5  : 5.22  -- 24 byte key (not dynlen), char data, guava compare
     *                       ValsDouble5a : 2.81  --   vd5 + sparse binary data (3 bits per byte), guava compare
     *                       ValsDouble5b : 2.17  --   vd5a + compare as 3 longs
     

     * zeropad --> 1100 pages vs 880 pages, ie 25% (as expected given 19 byte strings)
     * 
     * summary:
     *   the jit loves the cmp2 loop, cmp4 is 5% slower
     *   not using zeropad costs 15%
     *     but copying the array for zeropad costs 15% (ie the zeropad delta between Testers/a)
     *   guava compare is 22% slower than cmp4 (ie flip+endian)
     *   converting a string to the byte array costs 9%
     *   converting doubles to strings costs 29%
     *   dynlen is cheap (same speed as fixed length stuff)
     * 
     * action:
     *   ValsCheese grabs the underlying utf-16 for a string and copies the low order bytes using unsafe
     *     it encodes the raw bytes so they can be compared as signed longs
     *     15-20% faster than the naive implementation (ValsString) when running monomorphic
     *       suspect that it will better handle megamorphic usage
     *     all testing code should be removed (ie ValsDouble[2345], ValsRaw, ValsBytes2, ValsString2, etc)
     *       for future reference - these attempted to bisect the difference between storing strings and doubles
     *     zeropad is 10% faster - leaving it in for now ... still more space efficient than a String []
     *       should be moved to an instance variable once Testera/ValsRaw don't need it
     * 
     * anachronisms:
     *   ValsRaw -- stored strings and compared byte by byte against the underlying string - very slow
     *              demoed by SSmeta.Raw
     *   ValsBytes2 -- ValsBytes with alternative compares (byte-wise, longwise, munged)
     *                 this is the fastest of the string maps (VVmeta.DF2/a/cmp2 above - 1.95 sec)
     *                 stored the entries in signed long order with zeropad and a simplified compare loop
     *                 bytes were pre-rendered, so not apples to apples
     *   ValsDouble[23467] -- stored doubles using extra space to simulate String performance
     *   ValsDouble5 -- sparse octal rep of double using 24 bytes and long compares
     *                  effectively a non-dynlen version of ValsBytes2, notably *not* significantly faster
     *                  demoed by Dmeta.Big
     *                  store as a fixed length byte array of the string characters and compare as byte arrays
     *                  5a: use a sparse octal (3 bits per byte) instead of chars
     *                  5b: use longs to compare (instead of the guava bit-twiddling), ie it's not lexacographical
     *   ValsString2 -- alternative ways of extracting the string (not useful)
     * 
     * 
     * debriefing:
     *   all testing was put+look, but remove performance is similar
     *   SS.cheese/s/cmp4 - final (ie no zeropad, unsafe copy of String.values, lexicographical)
     *     put:1.185  look:1.279  remove:1.521  chk:0.120  total:4.104
     * 
     * update:
     *   ValsString using the same compare(reverse+flip) method as ValsCheese is 10% faster than with guava compare
     * 
     * 
     */
    public static void main(String[] args) throws Exception {
        auto(null,1000000,3,2
//                , new Testeri(new Itree.IF())
//                ,new Tester(new Dtree.DF ())
//                ,new Tester(new Dtree.DF2 ())
//                ,new Tester(new Dtree.DF3 ())
                ,new Testers(new SSmeta.DF ())
//                ,new Tester2(new Dtree.DF2())
//                ,new Tester3(new Dtree.DF3())
//                ,new Tester(new Dtree.DD ())
//                ,new Testers(new SSmeta.Cheese ())
                );
    }
    
}

// 2013.06.25 -- seth lytle
// duplicates could span blocks so switching to using findPath + next(path)
//   what is the performance cost ???
//   runs are medians of several runs of Tester+Dtree.DF
//   looks are 10-20% slower, removes 10% slower
//   for maps that don't allow duplicates could use the old findData approach
//     ie, don't save the path to the location, always insert to the left
//                 |   put          |  look          |   rem          |   chk          | totals         
//   findData: DF  | 0.520   0.018  | 0.410   0.015  | 0.529   0.000  | 0.024   0.001  | 1.483   0.034
//   findPath: DF  | 0.534   0.002  | 0.461   0.014  | 0.562   0.002  | 0.029   0.000  | 1.586   0.014 
// Testers+SSmeta.DF appears 7% slower as well (3.0s vs 2.8s)


/*
 * megamorphic vs monomorphic (tester + btree)
 *     for tester, refers to Tester[123]
 *     for btree, refers to whether DF[23] extends Btree or Btree[23]
 *       Btree[23] are duplicates of Btree (not committed to git)
 *     notation: DF2.1 means Dtree.DF extending Btree2 run with Tester1
 *     times are always for DF with Btree1
 * 
 *   123+123 --> 1.492, 1.462, 1.453               --> 1.462
 *   1+1     --> 1.490, 1.540, 1.472, 1.455, 1.522 --> 1.490
 *   111+123 --> 1.495, 1.489, 1.503               --> 1.495
 *   111+111 --> 1.681, 1.595, 1.633               --> 1.633
 *   123+111 --> 1.584, 1.586, 1.636               --> 1.586
 *   DF+IF+IF --> 1.552, 1.620, 1.716, 1.623       --> 1.621
 *   DF+SS.3    --> 1.470
 *   DF+DF2.2+SS.3 --> 1.473
 *   DF+SS.3+IF  --> 1.654, 1.607
 *   DF+DF.2+SS.3 --> 1.618
 *   DF.1+DF.2+DF.3+SS.4+IF*2 --> 1.633, 1.672
 *   DF*3+SS.4+IF*2 --> 1.649
 *   DF*3+SS+IF*2   --> 1.663, 1.657
 * 
 * summary: 20% penalty for 3 Btree subclasses (all with their own findLoop), slightly more for more
 *          Tester polymorphism has a lesser effect, ie for polymorphic btrees the tester effect is negligible
 * 
 * insert, find, and remove all seem to "scale" evenly, ie take a fixed perent of the total time
 * 
 */