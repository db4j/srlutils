// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import static org.srlutils.Types.Enum.*;

public class Types {

    public enum Enum {
        _boolean( sizeof( false ) ),
        _int    ( sizeof( 0 ) ),
        _long   ( sizeof( 0L ) ),
        _float  ( sizeof( 0.0f ) ),
        _double ( sizeof( 0.0 ) ),
        _byte   ( sizeof( (byte) 0 ) ),
        _short  ( sizeof( (short) 0 ) ),
        _char   ( sizeof( 'a' ) ),
        ;
        public final int size;
        Enum(int size) { this.size = size; }
        public int size() { return size; }
    }

    /** size in bytes */ public static int sizeof(boolean val) { return   Integer.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(int     val) { return   Integer.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(long    val) { return      Long.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(float   val) { return     Float.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(double  val) { return    Double.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(byte    val) { return              1            ; }
    /** size in bytes */ public static int sizeof(short   val) { return     Short.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(char    val) { return Character.SIZE / Byte.SIZE; }
    
    /** size in bytes */ public static int sizeof(Boolean val)   { return   Integer.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(Integer val)   { return   Integer.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(Long    val)   { return      Long.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(Float   val)   { return     Float.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(Double  val)   { return    Double.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(Byte    val)   { return              1            ; }
    /** size in bytes */ public static int sizeof(Short   val)   { return     Short.SIZE / Byte.SIZE; }
    /** size in bytes */ public static int sizeof(Character val) { return Character.SIZE / Byte.SIZE; }


    public static int compare(int v1,int v2) { return (v1<v2 ? -1 : (v1==v2 ? 0 : 1)); }
    public static int compare(long v1,long v2) { return (v1<v2 ? -1 : (v1==v2 ? 0 : 1)); }
    public static int compare(boolean v1,boolean v2) { return v1==v2 ? 0 : v1==true ? 1 : -1; }
    
}











