// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.Array;
import org.srlutils.Sorter;
import org.srlutils.Sorter.Info;

@SuppressWarnings("static-access")
/** a class'ified wrapper for MacroDevel, intended only for testing -- real use should be thru a rendered class */
public abstract class MacroSort<TT> extends Sorter.Base<TT> {
    public static Info defInfo = new Info(true,false,"macro");
    public MacroSort() { super(defInfo); }

    public static class  doublesm extends MacroSort<Double > {
         double [] vals, vbuf;
        MacroDevel. doubles.Data data = gate.newData();
        static MacroDevel. doubles gate = null;
        static MacroDevel top = null;

        public Double  get(int index) { return vals[index]; }
        public void start(boolean needBuf) {
            if ( needBuf ) vbuf = Array.alloc( vals );
            data.set( vals, vbuf );
        }
        public void    quicksort(int k1, int k2)            { gate.quicksort(   data, k1, k2      ); }
        public void  quickselect(int k1, int k2, int kth)   { gate.quickselect( data, k1, k2, kth ); }
        protected void mergesort(int k1, int k2)            { gate.mergesort(   data, k1, k2      ); }
    }

}











