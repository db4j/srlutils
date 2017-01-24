// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.nio.ByteBuffer;
import org.srlutils.Array.Sub.Objects;
import org.srlutils.Util.Ranged;

public class Array {
    public static int nextPow2(int len) { return len<=0 ? 1 : Integer.highestOneBit(len) << 1; }
    public static long nextPow2(long len) { return len<=0 ? 1 : Long.highestOneBit(len) << 1; }

    /** grow the array (or alloc if needed) to ensure it can hold capacity, using a power of 2 */
    public static int    [] grow(int    [] src,int capacity) { int len = nextPow2( capacity-1  ); return src==null ? alloc(src,len) : Util.dup( src, 0, len ); }
    /** grow the array (or alloc if needed) to ensure it can hold capacity, using a power of 2 */
    public static byte   [] grow(byte   [] src,int capacity) { int len = nextPow2( capacity-1  ); return src==null ? alloc(src,len) : Util.dup( src, 0, len ); }
    /** grow the array (or alloc if needed) to ensure it can hold capacity, using a power of 2 */
    public static double [] grow(double [] src,int capacity) { int len = nextPow2( capacity-1  ); return src==null ? alloc(src,len) : Util.dup( src, 0, len ); }
    /** grow the array (or alloc if needed) to ensure it can hold capacity, using a power of 2 */
    public static long   [] grow(long   [] src,int capacity) { int len = nextPow2( capacity-1  ); return src==null ? alloc(src,len) : Util.dup( src, 0, len ); }

    /** copy src[ko:ko+len) to dst[kd:*] */
    public static void copy(ByteBuffer src,ByteBuffer dst,int ko,int kd,int len) {
        dst.position(kd);
        ByteBuffer b2 = (dst==src) ? src.slice() : dst;
        src.position(ko);
        src.limit(ko+len);
        b2.put(src);
        src.clear();
    }


    


    /** a class to allow generic access to arrays using the java.lang.reflect.Array methods */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static class Wrap<TT> {
        public TT array;
        public Class type;
        public int siz;
        /** return a new array of the same size */
        public TT alloc() { return alloc(siz); }
        /** return a new array of length len */
        public TT alloc(int len) { return (TT) java.lang.reflect.Array.newInstance(type,len); }
        /** return the array or a copy (if length is less than cap) */
        public TT ensure(int cap) {
            return (siz < cap)
                    ? dup( siz, cap )
                    : array==null ? alloc( cap ) : array;
        }
        /** return the array or a copy (if length is less than cap) using power-of-2 growth if needed */
        public TT ensure2(int cap) {
            int cap2 = nextPow2(cap);
            return (siz < cap)
                    ? dup( siz, cap2 )
                    : array==null ? alloc( cap2 ) : array;
        }
        /** return a new array of length cap, and copy the array content on [0,len) */
        public TT dup(int len,int cap) {
            TT dst = alloc( cap );
            if (siz > 0) System.arraycopy( array, 0, dst, 0, len );
            return dst;
        }
        /** return a copy of the array, ie same length, same content */
        public TT dup() { return dup(siz,siz); }
        /** truncate the array to length len if needed */
        public TT trunc(int len) { return len < siz || array==null ? dup(len,len) : array; }
        /** return the array or a copy (if needed) of length len, ie trunc/grow if needed */
        public TT fixed(int len) { return len==siz && array!=null ? array : dup( Math.min(len,siz), len ); }
        /** return the length of the array */
        public int len() { return siz; }
        public Wrap() {}
        public Wrap(TT $array,Class $type) {
            array = $array;
            type = $type;
            siz = (array==null) ? 0 : java.lang.reflect.Array.getLength( array );
        }
    }
    public static Wrap<int []> oo(int [] array) { return new Wrap(array,int .class); }
    public static Wrap<long[]> oo(long[] array) { return new Wrap(array,long.class); }
    public static <TT> Wrap<TT> oo(TT array) { return new Wrap(array,array.getClass().getComponentType()); }
    public static <TT> Wrap<TT> oo(Class<TT> arrayKlass) { return new Wrap(null,arrayKlass.getComponentType()); }


    /** return the length of the array (must be an array) or 0 if null */
    public static int len(Object array) { return array==null ? 0 : java.lang.reflect.Array.getLength( array ); }


    /** return a new array of the same type as vals  */   public static  double [] alloc( double[]vals) { return new  double[len(vals)]; }
    /** return a new array of the same type as vals  */   public static   float [] alloc(  float[]vals) { return new   float[len(vals)]; }
    /** return a new array of the same type as vals  */   public static boolean [] alloc(boolean[]vals) { return new boolean[len(vals)]; }
    /** return a new array of the same type as vals  */   public static    byte [] alloc(   byte[]vals) { return new    byte[len(vals)]; }
    /** return a new array of the same type as vals  */   public static    char [] alloc(   char[]vals) { return new    char[len(vals)]; }
    /** return a new array of the same type as vals  */   public static   short [] alloc(  short[]vals) { return new   short[len(vals)]; }
    /** return a new array of the same type as vals  */   public static    long [] alloc(   long[]vals) { return new    long[len(vals)]; }
    /** return a new array of the same type as vals  */   public static     int [] alloc(    int[]vals) { return new     int[len(vals)]; }
    /** return a new array of the same type as vals  */   public static  <TT> TT[] alloc(     TT[]vals) { return newArray( type(vals), len(vals) ); }


    /** return a new length nn array, type'd as vals */   public static  double [] alloc( double[]vals,int nn) { return new  double[nn]; }
    /** return a new length nn array, type'd as vals */   public static   float [] alloc(  float[]vals,int nn) { return new   float[nn]; }
    /** return a new length nn array, type'd as vals */   public static boolean [] alloc(boolean[]vals,int nn) { return new boolean[nn]; }
    /** return a new length nn array, type'd as vals */   public static    byte [] alloc(   byte[]vals,int nn) { return new    byte[nn]; }
    /** return a new length nn array, type'd as vals */   public static    char [] alloc(   char[]vals,int nn) { return new    char[nn]; }
    /** return a new length nn array, type'd as vals */   public static   short [] alloc(  short[]vals,int nn) { return new   short[nn]; }
    /** return a new length nn array, type'd as vals */   public static    long [] alloc(   long[]vals,int nn) { return new    long[nn]; }
    /** return a new length nn array, type'd as vals */   public static     int [] alloc(    int[]vals,int nn) { return new     int[nn]; }
    /** return a new length nn array, type'd as vals */   public static  <TT> TT[] alloc(     TT[]vals,int nn) { return newArray( type(vals), nn ); }



    public static <TT> TT [] newArray(Class<TT> type,int nn) { return (TT[]) java.lang.reflect.Array.newInstance( type, nn ); }
    public static <TT> TT [] newArrayLike(TT example,int nn) { return (TT[]) java.lang.reflect.Array.newInstance( example.getClass(), nn ); }

    public static <TT> Class<TT> type(TT [] vals) { return (Class<TT>) vals.getClass().getComponentType(); }
    /** return the component type of array, or null if not an array */
    public static Class type(Object array) {
        if ( array == null || !array.getClass().isArray() ) return null;
        return array.getClass().getComponentType();
    }

    /** swap vals[k1], vals[k2] */     public static void swap( double[] vals,int k1,int k2) { double tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(  float[] vals,int k1,int k2) {  float tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(boolean[] vals,int k1,int k2) {boolean tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(   byte[] vals,int k1,int k2) {   byte tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(   char[] vals,int k1,int k2) {   char tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(  short[] vals,int k1,int k2) {  short tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(   long[] vals,int k1,int k2) {   long tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static void swap(    int[] vals,int k1,int k2) {    int tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }
    /** swap vals[k1], vals[k2] */     public static <TT> void swap(TT[] vals,int k1,int k2) {     TT tmp=vals[k1]; vals[k2]=vals[k1]; vals[k1]=tmp; }



    /**
     * concatenate any number or arrays
     * @param <TT> the type of the arrays (must be an array type)
     * @param vv the arrays (or a single array of arrays)
     * @return the left to right concatenation of vv, ie a new array of the same type as vv
     */
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static <TT> TT concat(TT...vv) {
        int len = 0, ko = 0;
        int [] lens = new int[vv.length];
        for (int ii=0; ii < vv.length; ii++)
            len += lens[ii] = java.lang.reflect.Array.getLength(vv[ii]);
        Class type = type(vv[0]);
        TT tt = (TT) java.lang.reflect.Array.newInstance(type,len);
        for (int ii=0; ii<vv.length; ko += lens[ii], ii++) 
            System.arraycopy(vv[ii],0,tt,ko,lens[ii]);
        return tt;
    }


    /** return a sub-array */  public static Sub. doubles sub( double...vo) { return new Sub. doubles().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.  floats sub(  float...vo) { return new Sub.  floats().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.booleans sub(boolean...vo) { return new Sub.booleans().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.   bytes sub(   byte...vo) { return new Sub.   bytes().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.   chars sub(   char...vo) { return new Sub.   chars().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.  shorts sub(  short...vo) { return new Sub.  shorts().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.   longs sub(   long...vo) { return new Sub.   longs().set(0,vo.length,vo); }
    /** return a sub-array */  public static Sub.    ints sub(    int...vo) { return new Sub.    ints().set(0,vo.length,vo); }
    /** return a sub-array */  public static <SS,TT> Sub. Objects<SS,TT> sub(TT...vo) { return (Objects<SS, TT>) new Sub. Objects().set(0,vo.length,vo); }



    /** classes for representing sub-arrays */
    public static abstract class Sub<SS,TT,UU,AA> {
        public int k1,k2;
        public AA vo;
        public abstract  UU sum();
        public abstract  double mean();
        public abstract  double var(double mean);
        public abstract  TT min();
        public abstract  TT max();
        public abstract  int mini();
        public abstract  int maxi();
        public abstract  TT get(int kk);

        public Stats.Std stats() {
            double mean = mean();
            double var = var( mean );
            return new Stats.Std( mean, var );
        }
        
        public void println(String fmt,int width,String pfx) {
            String txt = printf(fmt,width,pfx);
            System.out.println( txt );
        }
        public String printf(String fmt,int width,String pfx) {
            String txt = pfx;
            for (int ii = 0, jj = k1; jj < k2; ii++, jj++) {
                String eol = "";
                if (ii==width) {
                    eol = "\n" + pfx;
                    ii = 0;
                }
                txt += eol + String.format( fmt, get(jj) );
            }
            return txt;
        }



        /**
         *  wrap the array $vo - uses reflection to find the length ... when allocating many small arrays, explicitly
         *  providing the length might be faster
         *  @return this
         */
        public SS set(AA $vo) {
            vo = $vo;
            k2 = java.lang.reflect.Array.getLength( vo );
            return (SS) this;
        }
        /** set the range [k1,k2], returns this */
        public SS set(int $k1,int $k2) { k1 = $k1; k2 = $k2; return (SS) this; }
        /** set the backing array $vo and the range [$k1,$k2] */
        public SS set(int $k1,int $k2,AA $vo) { vo = $vo; k1 = $k1; k2 = $k2; return (SS) this; }


        /** sub-array vo[k1,k2) */
        public static class doubles extends Sub<doubles,Double ,Double,double []> {
            public  Double sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Double min() { return Ranged.min(k1,k2,vo); }
            public  Double max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Double get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class  floats extends Sub<floats,Float ,Double,float []> {
            public  Double sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Float min() { return Ranged.min(k1,k2,vo); }
            public  Float max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Float get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class booleans extends Sub<booleans,Boolean ,Long,boolean []> {
            public  Long sum() { return (long) Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Boolean min() { return Ranged.min(k1,k2,vo); }
            public  Boolean max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Boolean get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class   bytes extends Sub<bytes,Byte ,Long,byte []> {
            public  Long sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Byte min() { return Ranged.min(k1,k2,vo); }
            public  Byte max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Byte get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class   chars extends Sub<chars,Character ,Long,char []> {
            public  Long sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Character min() { return Ranged.min(k1,k2,vo); }
            public  Character max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Character get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class  shorts extends Sub<shorts,Short ,Long,short []> {
            public  Long sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Short min() { return Ranged.min(k1,k2,vo); }
            public  Short max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Short get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class   longs extends Sub<longs,Long ,Long,long []> {
            public  Long sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Long min() { return Ranged.min(k1,k2,vo); }
            public  Long max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Long get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) */
        public static class    ints extends Sub<ints,Integer ,Long,int []> {
            public  Long sum() { return Ranged.sum(k1,k2,vo); }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { return Stats.var(k1,k2,mean,vo); }
            public  Integer min() { return Ranged.min(k1,k2,vo); }
            public  Integer max() { return Ranged.max(k1,k2,vo); }
            public  int mini() { return Ranged.mini(k1,k2,vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,vo); }
            public  Integer get(int kk) { return vo[k1+kk]; }
        }


        /** sub-array vo[k1,k2) -- not all operations are supported */
        public static class Objects<SS,TT> extends Sub<SS,TT,Long,TT []> {
            public  Long sum() { nse(); return 0L; }
            public  double mean() { return 1.0*sum()/(k2-k1); }
            public  double var(double mean) { nse(); return 0; }
            public      TT min() { return (TT) Ranged.min(k1,k2,(Comparable[])vo); }
            public      TT max() { return (TT) Ranged.max(k1,k2,(Comparable[])vo); }
            public  int mini() { return Ranged.mini(k1,k2,(Comparable[])vo); }
            public  int maxi() { return Ranged.maxi(k1,k2,(Comparable[])vo); }
            public  TT get(int kk) { return vo[k1+kk]; }
            static void nse() { throw new UnsupportedOperationException( "some methods are not appropriate for all sub-classes" ); }
        }


    }

    public static void println(int [] vals,String pfx,String fmt,int width) {
        new Sub.ints().set(vals).println( fmt, width, pfx );
    }

    

    public static void main(String [] args) {
        int [] a1 = new int[]{1,2,3}, a2 = new int[]{4,5,6};
        int [] a3 = concat(a1,a2);
        for (int vo : a3) System.out.println(vo);
        System.out.println(new String(concat("hello".getBytes()," world".getBytes())));
        
        
        Simple.softAssert( nextPow2(0) == 1 && nextPow2(1) == 2 && nextPow2(7) == 8 );
        Simple.softAssert(
                   grow((int[])null,7).length==8
                && grow(new int[4],13).length==16 
                && grow(new int[21],11).length == 16
                );
        int [] vo = null;
        try {
            // make sure that this throws oob
            int x = oo(vo).ensure(51)[63];
            throw new Exception();
        }
        catch (Exception ex) {
            Simple.softAssert( ex instanceof ArrayIndexOutOfBoundsException );
        }
        vo = oo( int[].class ).dup();
        Integer[] dup = oo( Integer[].class ).dup();

        
        int [] vals = new int[55];
        for (int ii = 0; ii < vals.length; ii++) vals[ii] = ii+7;
        Sub.ints sub = new Sub.ints().set( vals );
        System.out.println( "pretty printing an array: " );
        sub.println( "%8d", 8, ">\t" );
        System.out.println( "again: " );
        new Sub.ints().set( vals ).println( "%8d", 8, "-\t" );
    }

}











