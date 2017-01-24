// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.util.Iterator;


/*
 * fixme:api
 * grow is mostly-public, but if it's called directly it results in an inconsistent array
 * class names should be Capitalized
 */




/**
 * generic array [0,last], capable of growing, current length: size, capacity: max
 *   don't need raw gets and sets since the backing array is exposed
 *   can be used as long as it's ensure()'d first
 *   TT -- the element type
 *   UU -- the array type, ie for objects it's equivalent to TT[]
 */
public abstract class DynArray<TT,UU> implements Iterable<TT> {
    /** the backing array                 */  public UU vo;
    /** the size of the current array     */  public int size;
    /** the capacity of the storage array */  public int max;

    /** grow array to hold exactly len elements */  protected abstract void grow(int len);
    /** @return vo[index]                       */  public abstract TT geto(int index);
    /** set vo[index] = val                     */  public abstract void seto(int index,TT val);


    /** 
     * ensure that the array is big enough to hold len elements, growing by powers of 2 as needed
     * returns the backing array
     */
    public UU ensure(int len) { if ( len > max ) grow( max = Array.nextPow2( len ) ); return vo; }
    /** ensure that there is enough space for len additional elements, returning the backing array */
    public UU ensureAdditional(int len) { return ensure( size+len ); }
    /** ensure that the array is big enough to hold 1 more element, returning the backing array */
    public UU ensure() { return ensure(size+1); }

    /** allocate a new backing array of exactly len */
    public void alloc(int len) { max = len; grow( max ); }
    /** use array as the backing array - uses reflection to calc the length */
    public void alloc(UU array) {
        vo = array;
        max = java.lang.reflect.Array.getLength( vo );
    }
    /** use array as the backing array of length len */
    public void alloc(UU array,int len) { vo = array; max = len; }

    /** @return the available space, ie max - size */
    public int getSpace() { return max - size; }

    public class Iter implements Iterator<TT> {
        public int kk;
        public boolean hasNext() { return kk < size; }
        public TT next() { return geto( kk++ ); }
        public void remove() { throw new UnsupportedOperationException( "Not Applicable" ); }
    }
    public Iterator<TT> iterator() { return new Iter(); }





    /**
     * prepare the array for insertion at the index
     * ensures the backing array is large enough and sets size if needed
     */
    public void prep(int index) {
        ensure( index + 1 );
        if (size <= index) size = index+1;
    }

    /** trim and return backing array  */
    public UU trim() { grow( size ); max = size; return vo; }


    /** append array [k1,k2) to the dynamic array */
    public void append(UU array,int k1,int k2) {
        ensure( size + k2 - k1 );
        System.arraycopy( array, k1, vo, size, k2-k1 );
        size += k2 - k1;
    }

    /** make sub refer to this array(0:size) */
    public void sub(Array.Sub<?,?,?,UU> sub) {
        sub.k1 = 0;
        sub.k2 = size;
        sub.vo = vo;
    }

    /** shift index and everything to the right to the right by shift (must be positive) */
    public void shift(int index,int shift) {
        ensure( size+shift );
        System.arraycopy( vo, index, vo, index+shift, size-index );
        size += shift;
    }
    /** insert val at pos, shift the rest of the array to the right */
    public void insert(int index,TT val) {
        shift( index, 1 );
        seto( index, val );
    }


    /** dynamically sized array */
    public static class  doubles extends DynArray<Double,double []> {
        { vo = new double[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index, double val) { prep(index); vo[index]=val; }
        /** array[end]=val, return end */   public int add( double val) { ensure(size+1); vo[size]=val; return size++; }
        /** get array[index]           */   public  double get(int index) { return vo[index]; }
        /** generic get                */   public Double geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Double val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class   floats extends DynArray<Float,float []> {
        { vo = new float[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,  float val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public   float get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(  float val) { ensure(size+1); vo[size]=val; return size++; }
        /** generic get                */   public Float geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Float val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class booleans extends DynArray<Boolean,boolean []> {
        { vo = new boolean[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,boolean val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public boolean get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(boolean val) { ensure(size+1); vo[size]=val; return size++; }
        /** generic get                */   public Boolean geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Boolean val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class    bytes extends DynArray<Byte,byte []> {
        { vo = new byte[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,   byte val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public    byte get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(   byte val) { ensure(size+1); vo[size]=val; return size++; }
        public String text() { return new String( vo, 0, size ); }
        /** generic get                */   public Byte geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Byte val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class    chars extends DynArray<Character,char []> {
        { vo = new char[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,   char val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public    char get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(   char val) { ensure(size+1); vo[size]=val; return size++; }
        /** generic get                */   public Character geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Character val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class   shorts extends DynArray<Short,short []> {
        { vo = new short[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,  short val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public   short get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(  short val) { ensure(size+1); vo[size]=val; return size++; }
        /** generic get                */   public Short geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Short val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class    longs extends DynArray<Long,long []> {
        { vo = new long[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,   long val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public    long get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(   long val) { ensure(size+1); vo[size]=val; return size++; }

        /** generic get                */   public Long geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Long val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class     ints extends DynArray<Integer,int []> {
        { vo = new int[0]; }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,    int val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public     int get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(    int val) { ensure(size+1); vo[size]=val; return size++; }
        /** generic get                */   public Integer geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,Integer val) { set(index,val); }
    }



    /** dynamically sized array */
    public static class  Objects<TT> extends DynArray<TT,TT []> {
             public Objects initLike(TT example) { vo = Array.newArrayLike( example, 0 ); return this; }
             public Objects<TT> init(Class<TT> klass) { vo = Array.newArray( klass, 0 ); return this; }
             /** return a new Objects initialized with the klass - static to allow type inference */
             public static <TT> Objects<TT> neww(Class<TT> klass) { return new Objects().init( klass ); }
        /** grow backing array         */   public void grow(int len) { vo = Util.dup(vo,0,len); }
        /** array[index] = val         */   public void set(int index,     TT val) { prep(index); vo[index]=val; }
        /** get array[index]           */   public      TT get(int index) { return vo[index]; }
        /** array[end]=val, return end */   public int add(     TT val) { ensure(size+1); vo[size]=val; return size++; }
        /** generic get                */   public TT geto(int index) { return get(index); }
        /** generic set                */   public void seto(int index,TT val) { set(index,val); }
        public TT pop() { TT val = vo[--size]; vo[size] = null; return val; }
        public void clear() { for (int ii=0; ii < size; ii++) vo[ii] = null; size = 0; }
    }



    // fixme::succinctness -- move Objects.neww() to toplevel















}

















