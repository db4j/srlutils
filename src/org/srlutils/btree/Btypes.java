// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import static org.srlutils.btree.TestDF.*;
import java.io.Serializable;
import org.srlutils.Simple;
import org.srlutils.Types;
import org.srlutils.btree.Bstring.ValsString;
import org.srlutils.btree.TestDF.DFcontext;
import org.srlutils.btree.Bpage.Sheet;

public class Btypes {
    public abstract static class Element<EE,CC> implements Serializable {
        public int slot;
        final public boolean dynlen;
        Element() { dynlen = false; }
        Element(boolean $dynlen) { dynlen = $dynlen; }
        public void config(int aslot) { slot = aslot; }

        public int size(EE val,Object data) { return size(); }
        
        public int size(Sheet page,int index) { return size(); }
        public abstract EE get(Sheet page,int index);
        public abstract void set(Sheet page,int index,EE val,Object data);
        public String format(EE val) { return val.toString(); }
        /** compare (as per Comparable) key1 with the key stored in page at index2, ie key2,
         * using data returned by compareData */
        public abstract int compare(EE val1,Sheet page,int index2,Object data);
        /** return data (null ok) to be passed to compare(), if prefix is true, accept stem matches */
        public CC compareData(EE val1,boolean prefix,Object past) { return null; }
        /** return the size of value in bytes   */         public abstract int size();
        public int copyPayload(Sheet src,Sheet dst,Sheet base,int index,int basejar,int dstjar) {
            return 0;
        }
    }
    public abstract static class Primitive<EE> extends Element<EE,Void> {
        protected EE baseType;
    }
    public static class ValsInt extends Primitive<Integer> {
        public int  getp(Sheet page,int index) { return page.geti( slot, index ); }
        public void setp(Sheet page,int index,int val) { page.put( slot, index, val ); }
        public Integer  get (Sheet page,int index) { return getp( page, index ); }
        public void   set (Sheet page,int index,Integer val,Object data) { page.put(slot,index,val); }
        public String format(Integer val) { return String.format( "%8d", val ); }
        public int compare(Integer val1,Sheet page,int index2,Object data) {
            int val2 = getp( page, index2 );
            // fixme -- use signum, it's faster ... not doing it now cause i'm not ready to test it
//            return Integer.signum( val1 - val2 );
            return (val1 < val2) ? -1 : (val1==val2) ? 0 : 1;
        }
        public int size() { return Types.sizeof( baseType ); }
    }
    public static class ValsLong extends Primitive<Long> {
        public long  getp(Sheet page,int index) { return page.getl( slot, index ); }
        public Long  get (Sheet page,int index) { return getp( page, index ); }
        public void  set (Sheet page,int index,Long val,Object data) { page.put(slot,index,val); }
        public String format(Long val) { return String.format( "%8d", val ); }
        public int compare(Long val1,Sheet page,int index2,Object data) {
            long val2 = getp( page, index2 );
            return (val1 < val2) ? -1 : (val1==val2) ? 0 : 1;
        }
        public int size() { return Types.sizeof( baseType ); }
    }
    public static class ValsDouble extends Primitive<Double> {
        public double  getp(Sheet page,int index) { return page.getd( slot, index ); }
        public Double  get (Sheet page,int index) { return getp( page, index ); }
        public void    set (Sheet page,int index,Double val,Object data) { page.put(slot,index,val); }
        public String format(Double val) { return String.format( "%8.3f", val ); }
        public int compare(Double val1,Sheet page,int index2,Object data) {
            return Butil.compare( val1, getp( page, index2 ) );
        }
        public int size() { return Types.sizeof( baseType ); }
    }
    public static class ValsFloat extends Primitive<Float> {
        public float  getp(Sheet page,int index) { return page.getf( slot, index ); }
        public Float  get (Sheet page,int index) { return getp( page, index ); }
        public void   set (Sheet page,int index,Float val,Object data) { page.put(slot,index,val); }
        public String format(Float val) { return String.format( "%8.3f", val ); }
        public int compare(Float val1,Sheet page,int index2,Object data) {
            return Float.compare( val1, getp( page, index2 ) );
        }
        public int size() { return Types.sizeof( baseType ); }
    }
    
    
    public static class ValsTuple extends Element<Object [],ValsTuple.Data> implements Bstring.ValsFace {
        Element [] subs;
        int nn;

        public void shift(Sheet page,int index,Bstring.ValsFace ... other) {
            for (int ii=0; ii<nn; ii++)
                if (subs[ii].dynlen)
                    ((Bstring.ValsFace) subs[ii]).shift(page,index,other);
        }
        
        /** right shift everything left of offset by length */
        public void offset(Sheet page,int offset,int len) {
            for (int ii=0; ii<nn; ii++)
                if (subs[ii].dynlen)
                    ((Bstring.ValsFace) subs[ii]).offset(page,offset,len);
        }

        private static boolean isdyn(Element [] subs) {
            for (Element sub : subs)
                if (sub.dynlen) return true;
            return false;
        }
        
        public ValsTuple(Element ... $subs) {
            super(isdyn($subs));
            subs = $subs;
            nn = subs.length;
        }
        
        public static class Data {
            boolean prefix;
            Object [] edata;
            public Data(boolean $prefix,Object [] $edata) { prefix = $prefix; edata = $edata; }
        }

        public void set(Sheet page,int index,Object[] vals,Object data) {
            for (int ii=0; ii<nn; ii++) subs[ii].set(page, index, vals[ii],((Data)data).edata[ii]);
        }
        public Object [] get(Sheet page,int index) {
            Object [] vals = new Object[nn];
            for (int ii=0; ii<nn; ii++) vals[ii] = subs[ii].get(page,index);
            return vals;
        }
        public Data compareData(Object[] vals,boolean prefix,Object past) {
            Object [] edata = null;
            for (int ii=0; ii<nn; ii++) {
                Object data = vals[ii]==null ? null:subs[ii].compareData(vals[ii], prefix,past);
                if (data != null) {
                    if (edata == null) edata = new Object[nn];
                    edata[ii] = data;
                }
            }
            return new Data(prefix, edata);
        }
        public int compare(Object [] vals,Sheet page,int index2,Object dato) {
            Data data = (Data) dato;
            boolean deep = data.edata != null;
            Object vo;
            int c1=0, ii;
            for (ii=0; ii<nn && c1==0 && (vo=vals[ii])!=null; ii++)
                c1 = subs[ii].compare(vo, page, index2, deep ? data.edata[ii] : null);
            return c1;
        }
        public int size() {
            int size = 0;
            for (int ii=0; ii<nn; ii++) size += subs[ii].size();
            return size;
        }
        public void config(int aslot) {
            slot = aslot;
            for (int ii=0; ii<nn; ii++) {
                subs[ii].config( aslot );
                aslot += subs[ii].size();
            }
        }
        public int size(Object [] vals,Object dato) {
            int size = 0;
            Data data = (Data) dato;
            Object [] edata = data.edata;
            for (int ii=0; ii<nn; ii++) size += subs[ii].size(vals[ii],edata==null ? null:edata[ii]);
            return size;
        }

        public String format(Object[] val) {
            String txt = "";
            for (int ii=0; ii<nn; ii++)
                txt += subs[ii].format(val[ii]) + ".";
            return txt;
        }
        public int copyPayload(Sheet src,Sheet dst,Sheet base,int index,int basejar,int dstjar) {
            int total = 0;
            for (int ii=0; ii<nn; ii++)
                total += subs[ii].copyPayload(src,dst,base,index,basejar-total,dstjar-total);
            return total;
        }
        public int size(Sheet page,int index) {
            int size = 0;
            for (Element sub : subs) size += sub.size(page,index);
            return size;
        }
        
    }
    public static class ValsVoid extends Element<Void,Void> {
        public Void  get (Sheet page,int index) { return null; }
        public void   set(Sheet page,int index,Void val,Object data) {}
        public String format(Void val) { return ""; }
        public int compare(Void val1,Sheet page,int index2,Object data) { return -1; }
        public int size() { return 0; }
    }
    public static class DF extends Bmeta<DF.Data,Object [],Void,ValsTuple> implements Bface<DF.Data> {
        public DF() { setup(new ValsTuple(new ValsDouble(),new ValsFloat()),new ValsVoid()); }
        public static class Data extends Bmeta.Context<Object [],Void,Data> implements DFcontext {
            public Data set(double key) { return super.set(new Object[]{key,null},null); }
            public Data set(double key,float val) { return super.set(new Object[]{key,val},null); }
            public float val() { return (Float) (match ? key[1]:-1f); }
        }
        public Data context() { return new Data(); }
    }

    
    public static class DF2 extends Bmeta<DF2.Data,Object[],Void,ValsTuple> implements Bface<DF2.Data> {
        { setup(new ValsTuple(new ValsString(),new ValsString()),new ValsVoid()); }
        public Data context() { return new Data(); }
        public static class Data extends Bmeta.Context<Object[],Void,Data> implements DFcontext<Data> {
            public Data set(double key) { return super.set(new Object[]{key+"",null},null); }
            public Data set(double key,float val) { return super.set(new Object[]{key+"",val+""},null); }
            public float val() { return match ? Float.parseFloat((String) key[1]) : -1f; }
        }
    }

    static class Demo<CC extends Btree.Context & DFcontext<CC>,TT extends Bface<CC>> {
        TT map;
        Demo(TT $map) { map = $map; }
        
        void check(int k1,int k2,int step,int ko) {
            boolean del = ko < 0;
            if (del) ko = -ko;

            CC cc = map.context();
            for (int ii = k1; ii < k2; ii += step) {
                cc.set(1.0*ii);
                map.findPrefix(cc);
                boolean expected = del ? ii>=ko : ii<ko;
                if (expected)
                    Simple.softAssert(cc.match && cc.val()==-1f*ii);
                else
                    Simple.softAssert(!cc.match);
            }
        }


        void test() {
            int nn = 10000;
            map.init( map.context() );
            CC cc = map.context();
            for (int ii = 0; ii < nn; ii++) {
                if (ii==18839)
                    Simple.nop();
                map.insert( cc.set(1.0*ii,-1.0f*ii) );
                if (ii >= 18800 && ii%1000==0) check(0,ii+1,1,ii+1);
            }
            check(0,nn,1,nn);
            for (int ii = 0; ii < nn; ii++) {
                map.remove( cc.set(1.0*ii) );
                Simple.softAssert( cc.match && cc.val() == -1f*ii );
                if (false) check(0,nn,1,-ii-1);
            }
            check(0,nn,1,-nn);
        }

        public static void main(String [] args) throws Exception {
            if (false) { new Demo(new DF()).test(); return; }
            BtTests2.auto( null, 1000000, 1, 1, new Tester2(new DF2()), new Tester(new DF()) );
            BtTests2.auto( null, 1000000, 1, 2, new Tester2(new DF2()), new Tester(new DF()) );
        }
    }
}
