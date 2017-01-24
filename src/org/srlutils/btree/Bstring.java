// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.nio.ByteOrder;
import org.srlutils.Simple;
import org.srlutils.btree.Bpage.Sheet;

// todo:testing -- test on big-endian, ie masks and shifts

/*
 * the following features are mutually incompatible (not technically, but in terms of benefit)
 *   choose one
 * 
 * feature request -- long comparison
 *   instead of comparing byte by byte, compare longs
 *   store in the native byteorder and then just compare the longs
 *  hiroshiyamauchi.blogspot.com/2010/08/fast-unsigned-byte-lexicographical.html
 *  code.google.com/p/guava-libraries/source/browse/guava/src/
 *    com/google/common/primitives/UnsignedBytes.java
 * 
 * feature request -- incremental storage
 *   compose strings of a portion of the previous string + a new portion
 *   search has to be linear (instead of stepped)
 *   only need to compare the new portion
 */


public class Bstring {
    public static final boolean big = ByteOrder.nativeOrder()==ByteOrder.BIG_ENDIAN;
    /** number of bits for the length and offset fields, ie needs to match the maps blocksize */
    static final int nb = 12;
    /** shift for length */
    static final int shift1 = big ? 32-2*nb : 0;
    /** shift for offset */
    static final int shift2 = shift1 + nb;
    /** preserve mask - preserve this portion of the underlying int when writing */
    static final int pmask = big ? 0x000000ff : 0xff000000;
    /** the field mask, ie for length and offset */
    public static final int mask = (1 << nb) - 1;
    static final int lengthMask = mask << shift1;
    static final int offsetMask = mask << shift2;
    
    
    public static class ValsString extends ValsVar<String,Cmpr> {
        public String get(Sheet page,int index) {
            byte [] bytes = get2(page,index);
            return new String(bytes);
        }
        public void set(Sheet page,int index,String val,Object data) {
            set2(page,index,((Cmpr)data).bytes);
        }
        public Cmpr compareData(String val,boolean prefix,Object past) {
            return super.compareData2(val.getBytes(),prefix);
        }
        public String format(String val) { return val; }
    }
    public static class ValsBytes extends ValsVar<byte[],Cmpr> {
        public byte[] get(Sheet page,int index) {
            return get2(page,index);
        }
        public void set(Sheet page,int index,byte[] val,Object data) {
            set2(page,index,val);
        }
        public Cmpr compareData(byte[] val,boolean prefix,Object past) {
            return super.compareData2(val,prefix);
        }
    }
    public static final boolean zeropad = false;
    public static byte[] getBytes(String vo) { return encodeOrdered(vo,null); }

    // fixme::endianness -- big endian isn't fully supported - should be easy
    private static final boolean little = false;
    private static long flip(long val) { return Long.reverseBytes(val^Long.MIN_VALUE); }
    private static long flipx(long val) { return val^Long.MIN_VALUE; }

    /**
     *  given a munged string stored in bytes at offset of length len, return the un-munged bytes
     *  in a new array of length len, ie suitable for passing to new String()
     *  @see #encodeOrdered
     */
    private static byte [] decodeOrdered(byte [] bytes,int offset,int len) {
        int mix = len&~7, max = len+7&~7, rem = len-mix, shift = 8*(max-len);
        byte [] dst = new byte[len];
        for (int ii = 0; ii < mix; ii += 8)
            Sheet.put(dst, ii, flip(Sheet.getl(bytes,offset+ii)));
        if (!Bstring.zeropad & mix < len) {
            long val = flip(Sheet.getl(bytes,offset+mix) << shift);
            for (int ii = 0; ii < rem; ii++, val >>= 8) dst[mix+ii] = (byte) val;
        }
        return dst;
    }
    /**
     *  return a byte representation of src using the low bytes of the utf-16
     *  munge the bytes so that comparing as (signed) longs yields lexicographical order
     *  ie, reverse the bytes (on little endian) and flip the high bit
     */
    private static byte [] encodeOrdered(String src,byte [] dst) {
        char [] value = Sheet.getStringInternals(src);
        int len = value.length, mix = len&~7, max = len+7&~7;
        if (dst==null || max > dst.length) dst = new byte[max];
        int ii = 0;

        if (little) {
            for (; ii < len; ii++) dst[ii] = (byte) value[ii];
            for (; ii < max; ii++) dst[ii] = 0;
        }
        else {
            // fixme -- reads past end of the array - discard the values but in theory this could crash ???
            long val = 0;
            for (; ii < len; ii++) {
                val = val << 8 | (byte) value[ii];
                if ((ii&7)==7) {
                    Sheet.put(dst,ii&~7,flipx(val));
                    val = 0;
                }
            }
            if (mix < len) Sheet.put(dst,mix,flipx(val << 8*(max-len)));
        }
        return dst;
    }
    
    public static class ValsCheese extends ValsVar<String,ValsCheese.Cmpr> {
        public static class Cmpr extends Bstring.Cmpr {
            int num;
            public Cmpr(byte[] $bytes,boolean $prefix,int $num) { bytes = $bytes; prefix = $prefix; num = $num; }
        }
        public String get(Sheet page,int index) {
            byte [] bytes = get3(page,index);
            return new String(bytes);
        }
        byte [] get3(Sheet page, int index) {
            int bits = page.geti(slot,index);
            int len = len(bits);
            int off = off(bits);
            return decodeOrdered(page.buf,off,len);
        }
        public void set(Sheet page,int index,String val,Object data) {
            Cmpr jack = (Cmpr) data;
            set3(page,index,jack.bytes,jack.num);
        }
        void set3(Sheet page,int index,byte [] bytes,int len) {
            int off = page.nextPos( len );
            setBits(page,index,off,len);
            if (Btree.extraChecks) {
                Simple.softAssert( index < page.num );
                Simple.softAssert( page.num*page.size <= off );
            }
            if (zeropad)
                page.put( off, bytes, 0, len );
            else {
                int min = len&~7, rem = len-min;
                page.put(off,bytes,0,min);
                if (len > min) page.put(off+min,bytes,min+8-rem,rem);
            }
        }
        public Cmpr compareData(String val,boolean prefix,Object past) {
            byte [] bytes = encodeOrdered(val,past==null ? null:((Cmpr) past).bytes);
            int len = val.length();
            if (zeropad) len = len+7&~7;
            return new Cmpr(bytes,prefix,len);
        }
        public String format(String val) { return val; }
        public int cmp2(byte [] b1,Sheet page,int off,int m2) {
            int cmp = 0, ii = 0;
            for (; cmp==0 & ii <  m2 && (cmp = page.comparel(b1,ii,off+ii))==0;) ii += 8;
            return cmp;
        }
        public int cmp4(byte [] b1,Sheet page,int off,int len1,int len2) {
            int min = Math.min(len1,len2), ii = 0;
            long v1=0, v2=0;
            do {
                v1 = Sheet.getl(      b1,    ii);
                v2 = Sheet.getl(page.buf,off+ii);
                ii += 8;
            } while (ii < min & v1==v2);
            if (!zeropad & ii > len2) v2 <<= 8*(ii-len2);
            // if prefix, mask out v2
            return Long.compare(v1,v2);
        }
        public int compare(String s1,Sheet page,int index2,Object data) {
            Cmpr cmpr = (Cmpr) data;
            int bits = page.geti(slot,index2);
            int len1 = cmpr.num;
            int len2 = len(bits);
            int off = off(bits);
            byte [] b1 = cmpr.bytes;
            int cmp;
            if (zeropad) cmp = cmp2(b1,page,off,Math.min(len1,len2));
            else         cmp = cmp4(b1,page,off,len1,len2);
            cmp = cmp==0 ? (cmpr.prefix ? 0:len1-len2) : cmp;
            return cmp;
        }
    }
    
    
    /*
     * on deletion of a dynlen value, a "hole" is left
     * current scheme is to right shift everything left of the whole, ie fill it
     * then offset all the values that pointed into the shifted portion
     * fixme:performance - defer compression till overcap and then compress
     */
    public static interface ValsFace {
        public void shift(Sheet page,int index,ValsFace ... other);
        public void offset(Sheet page,int offset,int len);
    }
    
    public static abstract class ValsVar<TT,CC> extends Btypes.Element<TT,CC> implements ValsFace {
        boolean dbg = false;
        public ValsVar() { super(true); }
        public static int bsize = 3;

        protected int lenx(int bits) { return (bits >>> shift1) & mask; }
        protected int len(int bits) { return (bits >>> shift1) & mask; }
        int off(int bits) { return (bits >>> shift2) & mask; }
        
        
        public int len(Sheet page,int index) {
            int bits = page.geti(slot,index);
            int len = len(bits);
            return len;
        }
        public int off(Sheet page,int index) {
            int bits = page.geti(slot,index);
            int off = off(bits);
            return off;
        }
        
        public static final boolean sparseDelete = true;
        public void shift(Sheet page,int index,ValsFace ... others) {
            int bits = page.geti(slot,index);
            int len = len(bits);
            int off = off(bits);
            if (sparseDelete) {
                page.del += len;
                return;
            }
            page.rawcopy(page, page.jar, page.jar+len, off-page.jar);
            page.jar += len;
            for (ValsFace other : others)
                if (other != null)
                    other.offset(page,off,len);
        }
        
        /** right shift everything left of offset by length */
        public void offset(Sheet page,int offset,int len) {
            for (int ii = 0; ii < page.num; ii++) {
                int bits = page.geti(slot,ii);
                int k2 = off(bits), b2 = (k2+len) << shift2;
                if (k2 <= offset)
                    page.put(slot, ii, bits&~offsetMask|b2 );
            }
        }

        /** 
         *  read (len,off) from base and copy payload from src to dst(jar)
         *    updates base to reflect the new pos
         */
        public int copyPayload(Sheet src,Sheet dst,Sheet base,int index,int basejar,int dstjar) {
            int bits = base.geti(slot,index);
            int lenx = lenx(bits); // stored length value (ie could be magic)
            int len = len(bits);   // payload length
            int off = off(bits);
            src.rawcopy( dst, off, dstjar-len, len );
            setBits(base,index,basejar-len,lenx);
            return len;
        }
        byte [] get2(Sheet page,int off,int len) {
            byte[] bytes = new byte[len];
            page.get(off, bytes, 0, len);
            return bytes;
        }
        
        public byte [] get2(Sheet page, int index) {
            int bits = page.geti(slot,index);
            int len = len(bits);
            int off = off(bits);
            byte[] bytes = new byte[len];
            page.get( off, bytes, 0, len );
            if (dbg) System.out.format( "get: %x %d %d\n", bits, len, off );
            return bytes;
        }
        // return -1 for non-toast, otherwise the offset for the toast info
        public int getx(Sheet page, int index) {
            int bits = page.geti(slot,index);
            int len = lenx(bits);
            int off = off(bits);
            return len==mask ? off:-1;
        }

        void setBits(Sheet page,int index,int off,int len) {
            int data = page.geti( slot, index );
            int bits = (data & pmask) | (len << shift1) | (off << shift2);
            page.put(slot,index,bits);
            if (dbg) System.out.format( "set: %x %d %d\n", bits, len, off );
        }
        public void set2(Sheet page, int index,byte [] val) {
            byte[] bytes = val;
            int len = bytes.length;
            int off = page.nextPos( len );
            setBits(page,index,off,len);
            if (Btree.extraChecks) {
                Simple.softAssert( index < page.num );
                Simple.softAssert( page.num*page.size <= off );
            }
            page.put( off, bytes, 0, len );
        }
        public int toastSize = 8;
        public int setx(Sheet page,int index) {
            int len = mask;
            int off = page.nextPos(toastSize);
            setBits(page,index,off,len);
            return off;
        }
        public static boolean verbose = false;
        public int compare(TT s1,Sheet page,int index2,Object data) {
            Cmpr cmpr = (Cmpr) data;
            int bits = page.geti(slot,index2);
            int len2 = len(bits);
            int off = off(bits);
            byte [] b1 = cmpr.bytes;
            return Sheet.compare(b1,0,b1.length,page.buf,off,len2);
        }
        /*
         * insert to the right, ie if key == a branch entry, insert into the next entry's branch
         * find (and remove) to the left, but check next() as well
         */
        /** 
         * return a byte array that divides b1 and b2
         * 
         */
        public byte [] shortest(byte [] b1,int k1,int n1,byte [] b2,int k2,int n2) {
            return null;
        }

        public int size() { return bsize; }

        /*
         * would be nice to skip creation of the strings, and just compare the underlying
         * byte arrays ...
         * from http://en.wikipedia.org/wiki/UTF8#Advantages
         * "Sorting of UTF-8 strings as arrays of unsigned bytes will produce
         *  the same results as sorting them based on Unicode code points."
         * found it thru this article that's talking about similar stuff:
         * http://brunodumon.wordpress.com/2010/02/17/
         * building-indexes-using-hbase-mapping-strings-numbers-and-dates-onto-bytes/
         * @param val
         * @return 
         */
        public Cmpr compareData2(byte [] val,boolean prefix) { return new Cmpr(val,prefix); }

        public int size(Sheet page,int index) { return size() + len(page,index); }
        public int size(TT val,Object cmpr) {
            return size() + ((Cmpr) cmpr).bytes.length;
        }
    }
    public static class Cmpr {
        public boolean prefix;
        public byte[] bytes;
        public Cmpr() {}
        public Cmpr(byte[] $bytes,boolean $prefix) { bytes = $bytes; prefix = $prefix; }
    }
    public static class DF extends Bmeta<DF.Data,String,String,ValsString> {
        { setup(new ValsString(),new ValsString()); }
        public Data context() { return new Data(); }
        public static class Data extends Bmeta.Context<String,String,Data> implements TestDF.DFcontext<Data> {
            public Data set(double key) { return set(key,-1f); }
            public Data set(double $key,float $val) { key = $key+""; val = $val+""; return this; }
            public float val() { return match ? Float.parseFloat(val) : -1f; }
        }
    }

    public static void main(String [] args) throws Exception {
        TestDF.main(null);
        if (true) return;
        TestDF.auto( null, 1000000, 1, 1, new TestDF.Tester(new DF()) );
        TestDF.auto( null, 1000000, 1, 3, new TestDF.Tester(new DF()) );
    }
}




/*
 * performance note:
 *   using a string::string mapping for DF ... 8.8 seconds
 *   using double::string                      3.5 seconds
 *         double::float                       1.86 seconds
 * 
 * dedicated test harness vs generic (but monomorphic) harness - no difference
 *     Mindir2 -- Average   :    2.846    3.000    3.977    0.833   10.656  | 
 *     Bmeta   -- Average   :    2.907    3.035    4.087    0.850   10.878  | 

 */



































