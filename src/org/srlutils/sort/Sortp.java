// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.Array;
import org.srlutils.Sorter;
import org.srlutils.sort.templated.DirectFast;
import org.srlutils.Sorter.Info;

@SuppressWarnings("static-access")
/** a class'ified wrapper for DirectFast, ie direct access to the elements, without randomized pivots */
public abstract class Sortp<TT> extends Sorter.Base<TT> {
    public static Info defInfo = new Info(false,true,"dfast");
    public Sortp() { super(defInfo); cfg = DirectFast.cfg; }

    public Sortp<TT> config(Sorter.Config _cfg) {
        if (_cfg==null || _cfg==cfg) return this;
        throw new RuntimeException( "unsupported operation\nconfiguration of this sub-class is not supported." +
                " pass null or the default value to bypass this check and leave the default values" );
    }





    public static class  doubles extends Sortp<Double > {
          double [] vals, vbuf;
        DirectFast. doubles.Data data = gate.newData();
        static DirectFast. doubles gate = null;

        public Double  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }

    }



    public static class   floats extends Sortp<Float > {
          float [] vals, vbuf;
        DirectFast.  floats.Data data = gate.newData();
        static DirectFast.  floats gate = null;

        public Float  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class booleans extends Sortp<Boolean > {
        boolean [] vals, vbuf;
        DirectFast.booleans.Data data = gate.newData();
        static DirectFast.booleans gate = null;

        public Boolean  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class    bytes extends Sortp<Byte > {
           byte [] vals, vbuf;
        DirectFast.   bytes.Data data = gate.newData();
        static DirectFast.   bytes gate = null;

        public Byte  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class    chars extends Sortp<Character > {
           char [] vals, vbuf;
        DirectFast.   chars.Data data = gate.newData();
        static DirectFast.   chars gate = null;

        public Character  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class   shorts extends Sortp<Short > {
          short [] vals, vbuf;
        DirectFast.  shorts.Data data = gate.newData();
        static DirectFast.  shorts gate = null;

        public Short  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class    longs extends Sortp<Long > {
           long [] vals, vbuf;
        DirectFast.   longs.Data data = gate.newData();
        static DirectFast.   longs gate = null;

        public Long  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class     ints extends Sortp<Integer > {
            int [] vals, vbuf;
        DirectFast.    ints.Data data = gate.newData();
        static DirectFast.    ints gate = null;

        public Integer  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class   Objects<TT extends Comparable> extends Sortp<TT > {
             TT [] vals, vbuf;
        DirectFast. Objects.Data data = gate.newData();
        static DirectFast. Objects gate = null;

        public TT get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


}











