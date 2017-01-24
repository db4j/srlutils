// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.Array;
import org.srlutils.Sorter;
import org.srlutils.sort.templated.Direct;
import org.srlutils.Sorter.Info;

@SuppressWarnings("static-access")
/** a class'ified wrapper for Direct, ie direct access to the elements, without randomized pivots */
public abstract class Sort<TT> extends Sorter.Base<TT> {
    public static Info defInfo = new Info(false,true,"direct");
    public Sort() { super(defInfo); cfg = Direct.cfg; }


    
    


    public static class  doubles extends Sort<Double > {
          double [] vals, vbuf;
        Direct. doubles.Data data = gate.newData();
        static Direct. doubles gate = null;

        public Double  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }

    }



    public static class   floats extends Sort<Float > {
          float [] vals, vbuf;
        Direct.  floats.Data data = gate.newData();
        static Direct.  floats gate = null;

        public Float  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class booleans extends Sort<Boolean > {
        boolean [] vals, vbuf;
        Direct.booleans.Data data = gate.newData();
        static Direct.booleans gate = null;

        public Boolean  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class    bytes extends Sort<Byte > {
           byte [] vals, vbuf;
        Direct.   bytes.Data data = gate.newData();
        static Direct.   bytes gate = null;

        public Byte  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class    chars extends Sort<Character > {
           char [] vals, vbuf;
        Direct.   chars.Data data = gate.newData();
        static Direct.   chars gate = null;

        public Character  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class   shorts extends Sort<Short > {
          short [] vals, vbuf;
        Direct.  shorts.Data data = gate.newData();
        static Direct.  shorts gate = null;

        public Short  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class    longs extends Sort<Long > {
           long [] vals, vbuf;
        Direct.   longs.Data data = gate.newData();
        static Direct.   longs gate = null;

        public Long  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class     ints extends Sort<Integer > {
            int [] vals, vbuf;
        Direct.    ints.Data data = gate.newData();
        static Direct.    ints gate = null;

        public Integer  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }
    public static class   Objects<TT extends Comparable> extends Sort<TT > {
             TT [] vals, vbuf;
        Direct. Objects.Data data = gate.newData();
        static Direct. Objects gate = null;

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











