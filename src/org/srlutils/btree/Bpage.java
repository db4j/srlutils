// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.lang.reflect.Field;
import org.srlutils.Simple;
import org.srlutils.Types;
import static org.srlutils.Unsafe.uu;
import static org.srlutils.btree.Btree.extraChecks;

public class Bpage {
    /** representation of a single page of the tree */
    public static abstract class Page<PP extends Page<PP>> {
        // fixme::space -- get rid of leaf, num can be short, and dexs[] only needed for branches
        public int num, leaf;
        public int size, kpage;
        public abstract void dexs(int index,int kpage);
        public abstract int dexs(int index);
        
        public abstract int delete(int ko);
        
        /** return the number of valid elements */
        public int num() { return num; }
        
        public int first() { return 0; }
        
        

        /** return the number of keys in the page - fixme::dry, should be made private or equiv */
        public int numkeys() {
            return leaf==1 ? num : num-1;
        }
        public String info() {
            return String.format( "Page - num:%5d\n", num );
        }
        public boolean valid(int ko) { return ko < num; }
    }
    /** implementation of a single page using direct memory and unsafe access for speed */
    public static class Sheet extends Page<Sheet> implements org.srlutils.Callbacks.Cleanable {
        public byte [] buf;
        public int jar,bs,del;
        int pdex;
        public static int pmeta = 8;
        /** a flag that can be used by the user
         *  known uses:
         *    b6.Bhunk -- the backing array is a dup of the cache, ie safe to modify directly
         */
        public byte flag = 0;
        public boolean isset(int mask) { return (flag&mask) != 0; }
        public int size() { return num*size+bs-jar-del; }
        public long pos(int slot,int index) {
            int delta = size*index+slot;
            if (extraChecks)
                Simple.softAssert( delta >= 0 && size*(index+1) <= bs && size*index+slot < bs );
            return bao+delta;
        }
        public void put(int slot,int index,int    val) { uu.putInt   ( buf, pos( slot, index ), val ); }
        public void put(int slot,int index,long   val) { uu.putLong  ( buf, pos( slot, index ), val ); }
        public void put(int slot,int index,float  val) { uu.putFloat ( buf, pos( slot, index ), val ); }
        public void put(int slot,int index,double val) { uu.putDouble( buf, pos( slot, index ), val ); }
        public void put(int slot,int index,byte   val) { uu.putByte  ( buf, pos( slot, index ), val ); }
        public void putb(int        offset,byte   val) {
            if (extraChecks)
                Simple.softAssert( offset >= 0 && offset+1 <= bs );
            uu.putByte  ( buf, bao+offset, val );
        }
        
        public int comparel(byte [] b1,int k1,int ko) { return Long.compare(getl(b1,k1),getl(buf,ko)); }
        
        /** 
         *  interpret v1 and v2 as byte[8] and compare them (lexicographically)
         *  ie, reverse the bytes and flip the high bit
         */
        private static int compareBytes(long v1,long v2) {
            //   start with an array of bytes with the most significant leftmost
            //   on little endian, reverse the byte order to get unsigned longs
            //   then flip the high bit to get them to sort as signed longs
            //     (according to google-guava.UnsignedLongs)
            return Long.compare(Long.reverseBytes(v1)^Long.MIN_VALUE, Long.reverseBytes(v2)^Long.MIN_VALUE);
        }
        private static final boolean littleEndian = true;
        /**  
         *   compare left(k1,len1) to right(k2,len2)
         *   reads (in blocks of 8 bytes) may pass the end of the arrays
         *     the values aren't used, but in theory could cause a page fault
         *   fixme -- doesn't currently support big endian (should be trivial to implement given a test boxen)
         *   todo::testing -- don't have a harness that uses the full range of chars, eg the high bit
         *     need to verify that the sort order is correct for non-ascii chars
         *   google guava has a similar method
         *     this differs in using bit twiddling (reverse bytes, flip high bit) to do the compare
         *     instead of finding the byte that differs
         *     TestDF shows 10% faster than using guava compare and the code is simpler
         */
        public static int compare(byte[] left,int k1,int len1,byte[] right,int k2,int len2) {
            int min = Math.min(len1,len2);
            int mix = min&~7, max = min+7&~7, shift = (min-mix) << 3;
            long mask = (1L<<shift)-1;
            Simple.softAssert(littleEndian);

            for (int ii = 0; ii < max; ii += 8) {
                long v1 = getl(left,k1+ii), v2 = getl(right,k2+ii);
                if (ii==mix) { v1 &= mask; v2 &= mask; }
                if (v1 != v2) return compareBytes(v1,v2);
            }
            return len1-len2;
        }
        
        

        static final long bao = uu.arrayBaseOffset(byte[].class);
        static final long dao = uu.arrayBaseOffset(double[].class);
        static final long lao = uu.arrayBaseOffset(long[].class);
        static final long cao = uu.arrayBaseOffset(char[].class);
        public static byte   getb(byte [] array,int offset) { return uu.getByte  (array,bao+offset); }
        public static byte   getb(char [] array,int offset) { return uu.getByte  (array,cao+offset); }
        public static double getd(byte [] array,int offset) { return uu.getDouble(array,bao+offset); }
        public static long   getl(byte [] array,int offset) { return uu.getLong  (array,bao+offset); }

        public static void put(byte [] dst,int offset,long val) { uu.putLong(dst,bao+offset,val); }
        public static void put(byte [] src,double [] dst) { uu.copyMemory(src,bao,dst,dao,src.length); }
        public static void put(byte [] src,long   [] dst) { uu.copyMemory(src,bao,dst,lao,src.length); }
        public static void put(byte [] src,char   [] dst) { 
            Simple.softAssert(dst.length*2 >= src.length);
            uu.copyMemory(src,bao,dst,cao,src.length); }
        public static void put(char [] src,byte   [] dst) {
            Simple.softAssert(dst.length >= 2*src.length);
            uu.copyMemory(src,cao,dst,bao,src.length*2); }
        
        public void putl(int offset,long val) {
            if (extraChecks)
                Simple.softAssert( offset >= 0 && offset+8 <= bs );
            uu.putLong(buf,bao+offset,val);
        }
        public void puti(int offset,int val) {
            if (extraChecks)
                Simple.softAssert( offset >= 0 && offset+4 <= bs );
            uu.putInt(buf,bao+offset,val);
        }

        static final long stringValueOffset;
        static {
            Field field = Simple.Reflect.field(String.class,"value");
            stringValueOffset = uu.objectFieldOffset(field);
        }
        public static char [] getStringInternals(String src) {
            return (char []) uu.getObject(src,stringValueOffset);
        }

        
        public byte   getb(int         offset) { return uu.getByte  ( buf, bao+offset         ); }
        public byte   getb(int slot,int index) { return uu.getByte  ( buf, pos( slot, index ) ); }
        public int    geti(int slot,int index) { return uu.getInt   ( buf, pos( slot, index ) ); }
        public long   getl(int slot,int index) { return uu.getLong  ( buf, pos( slot, index ) ); }
        public double getd(int slot,int index) { return uu.getDouble( buf, pos( slot, index ) ); }
        public float  getf(int slot,int index) { return uu.getFloat ( buf, pos( slot, index ) ); }
        public long   getl(int         offset) { return uu.getLong  ( buf, bao+offset         ); }
        public double getd(int         offset) { return uu.getDouble( buf, bao+offset         ); }

        public Sheet init(int $bs,int $size,int $pdex,byte [] data) {
            size = $size;
            bs = $bs;
            jar = bs-pmeta;
            del = 0;
            pdex = size - Types.Enum._int.size;
            return this;
        }
        
        public void clean() { buf = null; }

        /** get some info about the jni copy - left here as a marker / reminder
         *  the direct byte buffers are using direct memory - not sure if the same holds for
         *  unsafe access to jvm allocated memory */
        private static void jniCopyInfo() throws Exception {
            /*
             * from java.nio.Bits (which is used by DirectByteBuffer)
            // These numbers represent the point at which we have empirically
            // determined that the average cost of a JNI call exceeds the expense
            // of an element by element copy.  These numbers may change over time.
            static final int JNI_COPY_TO_ARRAY_THRESHOLD   = 6;
            static final int JNI_COPY_FROM_ARRAY_THRESHOLD = 6;
             */
            java.nio.ByteBuffer x;
            Class klass = Class.forName( "java.nio.Bits" );
            int jctat = (Integer) Simple.Reflect.getField( klass, "JNI_COPY_TO_ARRAY_THRESHOLD" );
            System.out.format( "Bits: %s, jctat:%d\n", klass, jctat );
            // Bits: class java.nio.Bits, jctat:6
        }
        // fixme -- move all the uu.copyMemory calls to a single method
        //  might make sense to use a loop for short lengths
        final boolean useCopy = true;

        public void get(int slot,int index,byte [] val,int k1,int len) {
            get(size*index+slot,val,k1,len);
        }
        /** get len bytes from the page at offset and place into val at k1 */
        public void get(int offset,byte [] val,int k1,int len) {
            // fixme:performance -- use java.nio.Bits.copyToArray
            long pos = bao+offset;
            if (extraChecks)
                Simple.softAssert( offset >= 0 && offset+len <= bs );
//          iter:   Mindir  |   30.483      0.671  |   21.296      0.147  |   51.779      0.816 
//          copy:   Mindir  |   13.532      0.115  |    9.220      0.158  |   22.752      0.259 
            if (useCopy) uu.copyMemory(buf,bao+offset,val,bao+k1,len);
            else for (int ii=0; ii<len; ii++)
                val[k1++] = uu.getByte(pos++);
        }
        public void put(int slot,int index,byte [] val,int k1,int len) {
            put(size*index+slot,val,k1,len);
        }
        public void put(int offset,byte [] val,int k1,int len) {
            // fixme:performance -- use java.nio.Bits.copyToArray
            long pos = bao+offset;
            if (extraChecks)
                Simple.softAssert( offset >= 0 && offset+len <= bs );
            if (useCopy) uu.copyMemory(val,bao+k1,buf,pos,len);
            else for (int ii=0; ii<len; ii++)
                uu.putByte( pos++, val[k1++] );
        }
        public void commit() {
            long d2 = del, n2 = num, j2 = jar;
            long data = (d2<<32) | (n2<<16) | j2;
            putl(bs-pmeta,data);
        }
        public void load() {
            long mask = (1<<16)-1;
            long data = getl(bs-pmeta);
            jar = (int) (data & mask);
            num = (int) ((data>>>16) & mask);
            del = (int) ((data>>>32) & mask);
        }
        public int nextPos(int len) {
            if ( false ) System.out.format( "kpage: %5d, offset: %5d, len: %5d, num: %5d\n",
                        kpage, jar, len, num );
            jar -= len;
            return jar;
        }
        
        /** copy len bytes from this.ko to dst.kd */
        public void rawcopy(Sheet dst,int ko,int kd,int len) {
            if (extraChecks)
                Simple.softAssert( ko>=0 & ko+len<bs & kd>=0 & kd+len<bs );
            uu.copyMemory( buf, bao+ko, dst.buf, bao+kd, len );
        }

        /** copy len bytes from (ko,slot) to (kd,slot) */
        public void rawcopy(Sheet dst,int ko,int kd,int slot,int len) {
            if (extraChecks)
                Simple.softAssert( ko>=0 & ko*size+slot+len<bs & kd>=0 & kd*dst.size+slot+len<bs );
            uu.copyMemory( buf, bao+ko*size+slot, dst.buf, bao+kd*dst.size+slot, len );
        }
        
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        public void copy(Sheet dst,int ko,int kd,int len) {
            if (extraChecks)
                Simple.softAssert( ko>=0 & (ko+len)*size<bs & kd>=0 & (kd+len)*dst.size<bs );
            uu.copyMemory( buf, bao+ko*size, dst.buf, bao+kd*size, len*size );
        }
        public void dexs(int index,int kpage) {
            int offset = index*size + pdex;
            if (extraChecks)
                Simple.softAssert(offset > 0 & offset+4<bs);
            uu.putInt( buf, bao+offset, kpage );
        }
        public int dexs(int index) {
            int offset = index*size + pdex;
            if (extraChecks)
                Simple.softAssert(offset > 0 & offset+4<bs);
            int k2 = uu.getInt(buf, bao+offset);
            return k2;
        }
        /** create space for an entry at ko, only called by the map for branches, ie not leafs */
        public int shift(int ko) {
            copy( (Sheet) this, ko, ko+1, num-ko );
            num++;
            return ko;
        }
        /** move keys(kbisect,:) in this page to next */
        public void split(Sheet dst,int kbisect) {
            int np = num-kbisect;
            copy(dst,kbisect,0,np);
            dst.num = np;
            num = kbisect;
        }
        /** move half the keys in this page to next */
        public void split(Sheet dst) { split(dst,num/2); }
        /** merge this into dst */
        public void merge(Sheet dst) {
            copy( dst, 0, dst.num, num );
            dst.num += num;
            num = 0;
        }
        /** shift the this[ko:end) to this[ko+1,*) */
        public int delete(int ko) {
            copy( (Sheet) this, ko+1, ko, num-ko-1 );
            num--;
            return ko < num ? ko : (ko==0 ? 0:ko-1);
        }
    }
    
}
