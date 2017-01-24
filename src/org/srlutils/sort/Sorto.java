// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.Sorter;
import org.srlutils.Array;
import org.srlutils.Util;
import org.srlutils.Sorter.Info;
import org.srlutils.sort.templated.DirectOrder;

@SuppressWarnings("static-access")
/** a class'ified wrapper for DirectOrder, ie direct access to the elements, with order, and config'able */
public abstract class Sorto<TT> extends Sorter.Order<TT> {
    public static Info defInfo = new Info(true,true,"order");
    public Sorto() { super(defInfo); }


    public Sorto<TT> order(int[] _order, int[] _buffer) { order = _order; obuf = _buffer; return this; }
    public Sorto sort() { return (Sorto) super.sort(); }
    public Sorto<TT> set(Object array) { return (Sorto) super.set( array ); }


    public static class  doubles extends Sorto<Double > {
         double [] vals, vbuf;
        DirectOrder. doubles.Data data = gate.newData();
        static DirectOrder. doubles gate = null;

        public Double  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class   floats extends Sorto<Float > {
          float [] vals, vbuf;
        DirectOrder.  floats.Data data = gate.newData();
        static DirectOrder.  floats gate = null;

        public Float  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class booleans extends Sorto<Boolean > {
        boolean [] vals, vbuf;
        DirectOrder.booleans.Data data = gate.newData();
        static DirectOrder.booleans gate = null;

        public Boolean  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class    bytes extends Sorto<Byte > {
           byte [] vals, vbuf;
        DirectOrder.   bytes.Data data = gate.newData();
        static DirectOrder.   bytes gate = null;

        public Byte  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class    chars extends Sorto<Character > {
           char [] vals, vbuf;
        DirectOrder.   chars.Data data = gate.newData();
        static DirectOrder.   chars gate = null;

        public Character  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class   shorts extends Sorto<Short > {
          short [] vals, vbuf;
        DirectOrder.  shorts.Data data = gate.newData();
        static DirectOrder.  shorts gate = null;

        public Short  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class    longs extends Sorto<Long > {
           long [] vals, vbuf;
        DirectOrder.   longs.Data data = gate.newData();
        static DirectOrder.   longs gate = null;

        public Long  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class     ints extends Sorto<Integer > {
            int [] vals, vbuf;
        DirectOrder.    ints.Data data = gate.newData();
        static DirectOrder.    ints gate = null;

        public Integer  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }


    public static class  Objects<TT extends Comparable> extends Sorto<TT> {
             TT [] vals, vbuf;
        DirectOrder. Objects.Data data = gate.newData();
        static DirectOrder. Objects gate = null;

        public TT  get(int index) { return vals[index]; }
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
        public void    mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }








}











