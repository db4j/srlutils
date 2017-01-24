// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.Sorter;
import org.srlutils.Sorter.Info;

@SuppressWarnings("static-access")
/** a Sorter'ified wrapper for java.util.Arrays */
public abstract class Sortj<TT> extends Sorter.Base<TT> {
    public static Info defInfo = new Info(false,true,"java");
    public Sortj() { super(defInfo); }
    static java.util.Arrays gate = null;

    public void start(boolean needBuf) {}
    public void  quickselect(int k1, int k2, int kth)   { nso(); }
    protected void mergesort(int k1, int k2)            { quicksort(k1,k2); }
    void nso() { throw new UnsupportedOperationException(
                "wrapping java.util.Arrays -- sort type determined by array type, no selection" ); }

    public static class  doubles extends Sortj<Double > {
         double [] vals;
        public Double  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class  floats extends Sortj<Float > {
          float [] vals;
        public Float  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class   bytes extends Sortj<Byte > {
           byte [] vals;
        public Byte  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class   chars extends Sortj<Character > {
           char [] vals;
        public Character  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class  shorts extends Sortj<Short > {
          short [] vals;
        public Short  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class   longs extends Sortj<Long > {
           long [] vals;
        public Long  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class    ints extends Sortj<Integer > {
            int [] vals;
        public Integer  get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
    public static class Objects<TT extends Comparable> extends Sortj<TT> {
             TT [] vals;
        public TT get(int index) { return vals[index]; }
        public void quicksort(int k1, int k2)           { gate.sort( vals, k1, k2 ); }
    }
}











