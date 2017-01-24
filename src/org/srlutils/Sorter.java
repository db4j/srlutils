// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import org.srlutils.rand.Source;
import java.util.Iterator;
import org.srlutils.Simple.Reflect;
import org.srlutils.sort.Sort;
import org.srlutils.sort.Sorto;
import static org.srlutils.Simple.Print.prf;
import static org.srlutils.Simple.Print.prl;
import org.srlutils.sort.Sorto.doubles;



public abstract class Sorter {
    
    /** use insertion sorted for short arrays */
    public static final int INSERT_LIMIT = 30;

    
    public static class SortType {
        public static enum Enum {
            qsort, merge, qselect;
        }
    }


    public static class Config {
        /** the subarray size below which to sort by insertion sort (inefficient for large arrays) */
        public final int         limit;
        
        /** should the pivot use a random index (robust to sorted input, but slower) */
        public final boolean     robust;
        
        /** a random source, defaults to Rand.source - provide your own for reproduceable results */
        public final Source rand;
        public Config() { this(null,null,null); }
        public Config(Config xx) { this(xx.limit,xx.robust,xx.rand); }
        public Config(Integer _limit, Boolean _robust, Source _rand) {
            limit =   _limit==null ? INSERT_LIMIT :  _limit;
            robust = _robust==null ? true         : _robust;
            rand =     _rand==null ? Rand.source  :   _rand;
        }
    }

    public static class Info {
        public boolean order;
        public boolean permuted;
        public String name;
        public Info(boolean _order,boolean _permuted,String _name) {
            order = _order;
            permuted = _permuted;
            name = _name;
        }
    }

    /** an Iterator for the sorted array */
    public static class Iter<TT> implements Iterator<TT> {
        int index;
        Base<TT> base;
        Iter(Base base) { this.base = base; }
        public boolean hasNext() { return index < base.nn; }
        public TT next() { return base.get( index++ ); }
        public void remove() {}
    }

    public static class Part {
        public int k1, k2;

        public Part() {}
        public Part(int k1, int k2) { this.k1 = k1; this.k2 = k2; }
        public void set(int k1, int k2) { this.k1 = k1; this.k2 = k2; }
    }

    public static abstract class Order<TT> extends Base<TT> {
        public Order(Info _info) { super(_info); }
        public int [] order, obuf;
        private int [] invert;
        public Order<TT> order(int[] _order, int[] _buffer) { order = _order; obuf = _buffer; return this; }
        public int [] invert() { if (invert==null) invert = Sorter.invert( order ); return order; }


        public Order<TT> sort(int qo, int r2)           { return (Order<TT>) super.sort(  qo, r2 );        }
        public Order<TT> sort()                         { return (Order<TT>) super.sort();                 }
        public Order<TT> qsort(int k1, int k2)          { return (Order<TT>) super.qsort( k1, k2 );        }
        public Order<TT> qsort()                        { return (Order<TT>) super.qsort();                }
        public Order<TT> qselect(int kth)               { return (Order<TT>) super.qselect(         kth ); }
        public Order<TT> qselect(int k1,int k2,int kth) { return (Order<TT>) super.qselect( k1, k2, kth ); }

    }

    /** base class for storing and sorting a region of an array */
    public static abstract class Base<TT> implements Iterable<TT>, Cloneable {
        /** array length           */     public int nn;
        /** configuration          */     public Config cfg = new Config();
        public Info info;

        public Base(Info _info) { info = _info; }


        /** return the Iterator    */     public Iterator<TT> iterator() { return new Iter(this); }
        /** get the value at index */     public abstract TT get(int index);
        /** set the configuration  */
        public Base<TT> config(Config _cfg) { if (_cfg != null) cfg = _cfg; return this; }

        /** set the sub-class array vals. this _must_ be overridden if vals doesn't exist or isn't an array */
        public Base<TT> set(Object array) {
            if ( array == null || !array.getClass().isArray() )
                throw new RuntimeException( array + " is not a valid array" );
            nn = java.lang.reflect.Array.getLength( array );
            Reflect.set( this, "vals", array );
            return this;
        }
        /** set the order and order buffer arrays (can be null), only meaningful for Sorters that are info.ordered.
         * this is optional -- if not called, the index position will be alloc'd and used. returns this */
        public Base<TT> order(int[] _order, int[] _buffer) { return this; }

        /** return a new instance, ie as a factory */
        public Base<TT> prov() {
            try                    { return (Base<TT>) this.clone();
            } catch (Exception ex) { throw new RuntimeException( "Allocation of a new instance failed", ex ); }
        }


        /** sort the entire region */
        public Base<TT> sort() { return sort( 0, nn ); }
        /** sort the region [ qo, r2 ) */
        public Base<TT> sort(int qo, int r2) {
            start( true );
            mergesort( qo, r2 );
            return this;
        }
        /** quick sort the backing array */
        public Base<TT> qsort() { return qsort( 0, nn ); }
        /** quick sort the region [ k1, k2 ) */
        public Base<TT> qsort(int k1, int k2) {
            start(false);
            quicksort( k1, k2 );
            return this;
        }
        /** quick select the kth value from the entire region */
        public Base<TT> qselect(int kth) { return qselect( 0, nn, kth ); }
        /** quick select the kth value over the region [k1,k2) */
        public Base<TT> qselect(int k1,int k2,int kth) {
            start(false);
            quickselect( k1, k2, kth );
            return this;
        }


        /** print the sorted values using the default toString() */
        public void print(String fmt) {
            prl( "------------------------------------------------" );
            for (int ii = 0; ii < nn; ii++) prf( fmt, get( ii ) );
        }

        /** print the sorted values using the default toString() */
        public void print() {
            prl( "------------------------------------------------" );
            for (int ii = 0; ii < nn; ii++) prl( get( ii ) );
        }

        /** initialize resources for a sort operation, including the storage buffer, if needBuf */
        public abstract void start(boolean needBuf);
        /** quicksort the region [ k1, k2 ), return 0 */
        public abstract void quicksort(int k1,int k2);
        /** quickselect the kth element in the region [k1,k2) */
        public abstract void quickselect(int k1,int k2,int kth);
        /** sort the region [k1,k2), returns 0 */
        protected abstract void mergesort(int k1, int k2);
    }




    /** invert a one to one compact mapping, ie all values in the map must be less than the length.
     * returns a new array of the inverse mapping */
    public static int [] invert(int [] map) {
        int [] rev = Array.alloc(map);
        for (int ii = 0; ii < map.length; ii++) rev[map[ii]] = ii;
        return rev;
    }



    /** quick pivot the kth value  */ public static  int [] selecto( double [] vals,int kth) { return new  Sorto. doubles().set( vals ).qselect(kth).order; }


    /** sort and return the values */ public static  double [] sort( double [] vals) { new  Sort. doubles().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static   float [] sort(  float [] vals) { new  Sort.  floats().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static boolean [] sort(boolean [] vals) { new  Sort.booleans().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static    byte [] sort(   byte [] vals) { new  Sort.   bytes().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static    char [] sort(   char [] vals) { new  Sort.   chars().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static   short [] sort(  short [] vals) { new  Sort.  shorts().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static    long [] sort(   long [] vals) { new  Sort.   longs().set( vals ).qsort(); return vals; }
    /** sort and return the values */ public static     int [] sort(    int [] vals) { new  Sort.    ints().set( vals ).qsort(); return vals; }
    /** sort and return the values */
    public static <TT extends Comparable> TT[] sort(TT[] vals) {
        new Sort.Objects().set( vals ).sort();
        return vals;
    }



    /** sort values and return Sorter */ public static  Sorto. doubles make( double [] vals) { return (Sorto. doubles) new  Sorto. doubles().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.  floats make(  float [] vals) { return (Sorto.  floats) new  Sorto.  floats().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.booleans make(boolean [] vals) { return (Sorto.booleans) new  Sorto.booleans().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.   bytes make(   byte [] vals) { return (Sorto.   bytes) new  Sorto.   bytes().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.   chars make(   char [] vals) { return (Sorto.   chars) new  Sorto.   chars().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.  shorts make(  short [] vals) { return (Sorto.  shorts) new  Sorto.  shorts().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.   longs make(   long [] vals) { return (Sorto.   longs) new  Sorto.   longs().set( vals ); }
    /** sort values and return Sorter */ public static  Sorto.    ints make(    int [] vals) { return (Sorto.    ints) new  Sorto.    ints().set( vals ); }
    /** sort values and return Sorter */ public static <TT> Sorto. Objects make(     TT [] vals) { return (Sorto. Objects) new  Sorto. Objects().set( vals ); }


    public static int [] order(Order sorter,int [] order,int k1,int k2) { return sorter.order(order,null).sort(k1,k2).order; }

    /** sort values and return order */
    public static int[] order( double[] vals, int [] order, int k1, int k2) {
        return order( new Sorto.doubles().set( vals ),order,k1,k2);
    }



    /** sort values and return order */ public static      int [] order( double [] vals) { return new  Sorto. doubles().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(  float [] vals) { return new  Sorto.  floats().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(boolean [] vals) { return new  Sorto.booleans().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(   byte [] vals) { return new  Sorto.   bytes().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(   char [] vals) { return new  Sorto.   chars().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(  short [] vals) { return new  Sorto.  shorts().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(   long [] vals) { return new  Sorto.   longs().set( vals ).sort().order; }
    /** sort values and return order */ public static      int [] order(    int [] vals) { return new  Sorto.    ints().set( vals ).sort().order; }
    /** sort values and return order */
    public static <TT extends Comparable> int[] order(TT[] vals) {
        return new Sorto.Objects().set( vals ).sort().order;
    }





    public static Sorter.Base test(Object array) {
        if ( array == null || !array.getClass().isArray() )
            throw new RuntimeException( array + " is not a valid array" );
        Class type = Array.type( array );
        if (false) {}
        else if ( type.equals(double.class) ) return new Sorto.doubles().set(array).sort();
        return null;
    }




    public static void main(String [] args) throws Exception {
        Rand.source.setSeed( 0L, true );
        int [] use = new int [] { 0,0,1,0 };

        test( Rand.rand( 100 ) ).print( "%8.3f\n" );


    }


}
