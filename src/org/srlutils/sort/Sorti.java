// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.Sorter;
import org.srlutils.Array;
import org.srlutils.Util;
import org.srlutils.Sorter.Info;
import org.srlutils.sort.templated.Indirect;

@SuppressWarnings("static-access")
/** a class'ified wrapper for DirectOrder, ie direct access to the elements, with order, and config'able */
public abstract class Sorti<TT> extends Sorter.Order<TT> {
    public static Info defInfo = new Info(true,false,"index");
    public Sorti() { super(defInfo); }







    public static class  doubles extends Sorti<Double > {
         double [] vals, vbuf;
        Indirect. doubles.Data data = gate.newData();
        static Indirect. doubles gate = null;

        public Double  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class   floats extends Sorti<Float > {
          float [] vals, vbuf;
        Indirect.  floats.Data data = gate.newData();
        static Indirect.  floats gate = null;

        public Float  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class booleans extends Sorti<Boolean > {
        boolean [] vals, vbuf;
        Indirect.booleans.Data data = gate.newData();
        static Indirect.booleans gate = null;

        public Boolean  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class    bytes extends Sorti<Byte > {
           byte [] vals, vbuf;
        Indirect.   bytes.Data data = gate.newData();
        static Indirect.   bytes gate = null;

        public Byte  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class    chars extends Sorti<Character > {
           char [] vals, vbuf;
        Indirect.   chars.Data data = gate.newData();
        static Indirect.   chars gate = null;

        public Character  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class   shorts extends Sorti<Short > {
          short [] vals, vbuf;
        Indirect.  shorts.Data data = gate.newData();
        static Indirect.  shorts gate = null;

        public Short  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class    longs extends Sorti<Long > {
           long [] vals, vbuf;
        Indirect.   longs.Data data = gate.newData();
        static Indirect.   longs gate = null;

        public Long  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class     ints extends Sorti<Integer > {
            int [] vals, vbuf;
        Indirect.    ints.Data data = gate.newData();
        static Indirect.    ints gate = null;

        public Integer  get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





    public static class  Objects<TT extends Comparable> extends Sorti<TT> {
             TT [] vals, vbuf;
        Indirect. Objects.Data data = gate.newData();
        static Indirect. Objects gate = null;

        public TT get(int index) { return vals[order[index]]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
            data.config( cfg );
            if (order == null) order = Util.colon(nn);
            if (needBuf && obuf==null) obuf = new int[nn];
            data.order( order, obuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }





}











