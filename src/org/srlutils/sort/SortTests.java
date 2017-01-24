// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.TaskTimer;
import org.srlutils.Rand;
import org.srlutils.Util;
import java.util.Iterator;
import org.srlutils.Sorter.Base;
import org.srlutils.Sorter.Config;
import org.srlutils.Sorter.SortType;
import static org.srlutils.Sorter.SortType.Enum.*;
import static org.srlutils.Simple.Print.prf;

@SuppressWarnings("static-access")
public class SortTests {


    public static String [] randStrings(int nn,int nc) {
        String [] data = new String [nn];
        for (int ii = 0; ii < nn; ii++) data[ii] = randString(nc);
        return data;
    }
    public static String randString(int nc) {
        char [] bnd = Util.bounds( "azAZ".toCharArray() );
        char [] chars = Rand.source.rand( new char[nc], bnd[0], bnd[1] );
        return String.valueOf( chars );
    }




    public static boolean checkSort(String[] vals, int nn) {
        return checkSort( java.util.Arrays.asList( vals ), nn );
    }
    public static <TT extends Comparable> boolean checkSort(Iterable<TT> col,int nn) {
        Iterator <TT> sorter = col.iterator();
        TT v1, vo = sorter.next();
        int numeq = 0;
        for (int ii = 1; ii < nn; ii++, vo = v1) {
            v1 = sorter.next();
            int cmp = vo.compareTo( v1 );
            if ( cmp > 0 )
                return false;
            if ( cmp==0 ) numeq++;
        }
        // just a quick check to make sure that all the values aren't equal
        // for large nn, numeq should be almost zero (it's just random values that happen to be equal)
        return numeq < 1 + nn / 2;
    }


    public static class TestString extends TestComparable<String> {
        String [] vals;
        public void init2(boolean cleanup) { vals = cleanup ? null : randStrings( nn, 4 ); }
        public void set() { sorter.set( vals ); }
    }

    public static class Testi extends TestComparable<Integer> {
        int [] vals;
        public void init2(boolean cleanup) { vals = cleanup ? null : Rand.source.rand( new int[nn], 0, 0 ); }
        public void set() { sorter.set( vals ); }
    }
    public static class Testl extends TestComparable<Long> {
        long [] vals;
        public void init2(boolean cleanup) { vals = cleanup ? null : Rand.source.rand( new long[nn], 0, 0 ); }
        public void set() { sorter.set( vals ); }
    }
    public static class Testf extends TestComparable<Float> {
        float [] vals;
        public void init2(boolean cleanup) { vals = cleanup ? null : Rand.source.rand( new float[nn], 0, 0 ); }
        public void set() { sorter.set( vals ); }
    }
    public static class Testd extends TestComparable<Double> {
        double [] vals, v2;
        public void init2(boolean cleanup) { if (cleanup) vals = null; else { vals = Rand.rand( nn ); v2 = Util.dup( vals ); } }
        public void set() { sorter.set( vals ); }
        public boolean check() {
            if (sorter.info.order && sorter.info.permuted)
                for (int ii=0; ii<nn; ii++)
                    if (v2[order[ii]] != vals[ii]) return false;
            return super.check();
        }
    }

    public abstract static class TestComparable<TT extends Comparable<TT>> extends Test<TT> {
        public void init() {
            init2(false);
            if (prov.info.order) order = Util.colon( nn );
        }
        public void clear() { init2(true); order = null; sorter = null; }
        abstract void set();
        public void makeTitle() {
            title = prov.info.name;
            super.setTitle( title );
        }
        public void payload() {
            sorter = prov.prov();
            set();
            sorter.order( order, null ).config( cfg );
            if ( check() ) throw new RuntimeException(
                    "mismatch -- initial array was sorted before sorting for " + title );
            switch (sortType) {
                case qsort: sorter.qsort(); break;
                case merge: sorter.sort(0,nn); break;
                case qselect: sorter.qselect( nn / 2 ); break;
            }
        }
        public boolean check() {
            TT v1, vo;
            switch (sortType) {
                case qsort:
                case merge:
                    return checkSort( sorter, nn );
                case qselect:
                default:
                    boolean ok = true;
                    int ii, no = sorter.nn/2;
                    vo = sorter.get( no );
                    for (ii =  0; ii < no; ii++)        ok &= vo.compareTo( sorter.get(ii) ) >= 0;
                    for (ii = no; ii < sorter.nn; ii++) ok &= vo.compareTo( sorter.get(ii) ) <= 0;
                    return ok;
            }
        }

    }

    public abstract static class Test<TT> extends TaskTimer.Runner<Integer> {
        int nn, nnbase;
        int [] order;
        Base<TT> sorter, prov;
        Config cfg;
        String title;
        SortType.Enum sortType;
        public Test() { super.setup( 2, "" ); }

        public void config(Integer gain) { if (gain != null) nn = nnbase / gain; }
        public void init() {
            init2(false);
        }
        public abstract void init2(boolean cleanup);
        public void clear() { init2(true); }
        public void makeTitle() {
            title = this.getClass().getSimpleName() + ":" + sortType;
            super.setTitle( title );
        }
        public Test set(Base<TT> _prov, int _nn, Config _cfg,SortType.Enum st) {
            prov = _prov;
            nn = nnbase = _nn;
            cfg = _cfg;
            sortType = st;
            makeTitle();
            return this;
        }
        public abstract void payload();
        public abstract boolean check();
        public void run(int stage) throws Exception {
            if (stage==0) payload();
            else if (stage==1) {
                if ( ! check() )
                    throw new RuntimeException( String.format(
                            "\nmismatch -- check failed for %s :: %s", title, prov.getClass()
                            ) );
                clear();
            }
        }
    }



    public static void main(String [] args) throws Exception {
//        Rand.source.setSeed( 0L, true );
        int [] use = new int [] { 0,0,1,0 };


        Config cfg = new Config( 30, true, null );
        Config fast = new Config( 30, false, null );
        Config slow = new Config( 100, true, null );
        int nn = (1<<20) + 100;
        if (args.length > 0) nn = Integer.parseInt( args [0] ) * (1<<20) / 10 / 8;
        prf( "size: %d\n", nn );

        SortType.Enum stype = qsort;

        TaskTimer.Runner[] tests = new TaskTimer.Runner[] {
            new Testd().set( new Sortj.doubles(), nn, null, stype ),
            new Testd().set( new Sortp.doubles(), nn, null, stype ),
            new Testd().set( new Sort.doubles(), nn, null, stype ),
            new Testd().set( new Sorto.doubles(), nn, null, stype ),
            new Testd().set( new Sorti.doubles(), nn, null, stype ),

//            new Testd().set( new Sorto.doubles(), nn, null, stype ),
//            new Testf().set( new Sorto.floats(), nn, null, stype ),
//            new Testl().set( new Sorto.longs(), nn, null, stype ),
//            new Testi().set( new Sorto.ints(), nn, null, stype ),
//            new TestString().set( new Sortp.Objects(), nn/4, null, merge ),
//            new TestString().set( new Sort.Objects(), nn/4, null, merge ),
//            new TestString().set( new Sortj.Objects(), nn/4, null, merge ),
        };
        TaskTimer.Runner[] testInts = new TaskTimer.Runner[] {
            new Testi().set( new Sortj.ints(), nn, null, stype ),
            new Testi().set( new Sort .ints(), nn, null, stype ),
            new Testi().set( new Sorto.ints(), nn, null, stype ),
        };

        
        if (false) {
            Base<String> sorter = new Sortp.Objects().set( randStrings( 100, 10 ) ).sort();
            for (String txt :  sorter) prf( "%s\n", txt );
            return;
        }
        if (false) {
            float [] vals = Rand.source.rand(new float[40],0,0);
            int ii;
            for (ii = 0; ii < vals.length; ii+=5) vals[ii] = -0.0f;
            for (ii = 3; ii < vals.length; ii+=13) vals[ii] = 0.0f;
            for (ii = 1; ii < vals.length; ii+=7) vals[ii] = Float.NaN;
            SortAux.sort2( vals, 0, vals.length );
//            java.util.Arrays.sort( vals );
            for (float val : vals) prf( "%8.3f\n", val );
            return;
        }
        if (false) {
            double [] vals = Rand.source.rand(new double[440],0,0), val2 = Util.dup( vals );
            int [] order = Util.colon( vals.length );
            Base<Double> sorter = new Sorto.doubles().set( vals ).order(order,null).qsort();
            for (int ii = 0; ii < sorter.nn; ii++)
                prf( "%5d %8.3f %8.3f %8.3f %5d\n", ii, vals[ii], val2[order[ii]], val2[ii], order[ii] );
            return;
        }
//        new TaskTimer().config(10).init( 2, 1, true, true ).autoTimer( tests );

        new TaskTimer().config(1).init( 4, 4, true, true ).autoTimer( tests );
        new TaskTimer().config(1).init( 4, 4, true, true ).autoTimer( testInts );
    }


}



/*

performance results comparing with builtin sort
- my code was written / tweaked on java 7 (maybe even java 6)
- java 7 introduced a dual-pivot quicksort (my algo should be comparable to java 6 sort)
- DirectFast is the most comparable to java's sort
- index and order algos are pretty variable - slow for doubles on sun-8 and for ints on sun-7 and sun-8
- haven't tested on an openjdk-8
- all these classes are templated instead of inheritance-based to avoid inlining problems
- java's builtin sort is the overall winner and wins in almost all cases (as expected)
- for object sort, the array is scattered - can use direct byte arrays and unsafe for max performance



Statistics: qsort, sun-java8, 2.0GHz locked, sort only (ie, stage 1)
-----------
doubles
        |          time              std              openjdk-7     sun-7
  java  |        0.153201          0.015411  |        0.147877    0.159794 
 dfast  |        0.173599          0.001051  |        0.172946    0.155661 
direct  |        0.177718          0.001424  |        0.171333    0.172672 
 order  |        0.239878          0.018698  |        0.185715    0.203303 
 index  |        0.372388          0.054342  |        0.290628    0.318796 


ints
        |          time              std              openjdk-7     sun-7
  java  |        0.139209          0.003513  |        0.175867    0.154329 
direct  |        0.268310          0.038895  |        0.143063    0.251834 
 order  |        0.243009          0.028878  |        0.174878    0.212246 



Use Cases: 
- Direct is more robust to sorted input than (at least) some versions of java sort
- Indiect or Order if the sort order is needed
- mergesort (java only uses for objects)
- qselect
- otherwise, use java's sort


*/