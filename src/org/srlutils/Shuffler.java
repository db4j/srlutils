// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import org.srlutils.rand.Source;
import org.srlutils.Simple.Print;
import org.srlutils.Simple.Reflect;

/** a collection of classes and methods that shuffle an array */
public class Shuffler {


    /** base class for the shuffler implementations
     * sub-classes must have a
     */
    public static abstract class Base {
        public int nn;
        public Source rnd = Rand.source;

        /** set the Rand.Source, the default is Rand.source */
        public Base set(Source _rnd) { rnd = _rnd; return this; }
        /** shuffle the _array, which much be an array of the appropriate type, ie must match the subclass */
        public void shuffle(Object array) {
            set( array );
            for (int ii = nn; ii > 1; ii--) swap( ii - 1, rnd.rand( 0,ii ) );
        }
        /** set the sub-class array v. this _must_ be overridden if v doesn't exist or isn't an array */
        protected void set(Object array) {
            if (array==null || ! array.getClass().isArray())
                throw new RuntimeException( array + " is not a valid array" );
            nn = java.lang.reflect.Array.getLength( array );
            Reflect.set( this, "v", array );
        }
        /** swap the elements at ii and jj */
        abstract void swap(int ii, int jj);
    }





    /** type specific */ public static class  doubles extends Base { double[]v; void swap(int i, int j) { double tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class booleans extends Base {boolean[]v; void swap(int i, int j) {boolean tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class   floats extends Base {  float[]v; void swap(int i, int j) {  float tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class    bytes extends Base {   byte[]v; void swap(int i, int j) {   byte tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class    chars extends Base {   char[]v; void swap(int i, int j) {   char tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class   shorts extends Base {  short[]v; void swap(int i, int j) {  short tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class     ints extends Base {    int[]v; void swap(int i, int j) {    int tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** type specific */ public static class    longs extends Base {   long[]v; void swap(int i, int j) {   long tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }
    /** generic    */ public static class Objects<TT> extends Base {     TT[]v; void swap(int i, int j) {     TT tmp = v[i]; v[i] = v[j]; v[j] = tmp; } }


    /** shuffle vals, using rnd */ public static  double [] shuffle( double [] vals,Source rnd) { new  doubles().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static   float [] shuffle(  float [] vals,Source rnd) { new   floats().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static boolean [] shuffle(boolean [] vals,Source rnd) { new booleans().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static    byte [] shuffle(   byte [] vals,Source rnd) { new    bytes().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static    char [] shuffle(   char [] vals,Source rnd) { new    chars().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static   short [] shuffle(  short [] vals,Source rnd) { new   shorts().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static    long [] shuffle(   long [] vals,Source rnd) { new    longs().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static     int [] shuffle(    int [] vals,Source rnd) { new     ints().set(rnd).shuffle( vals ); return vals; }
    /** shuffle vals, using rnd */ public static <TT> TT [] shuffle(     TT [] vals,Source rnd) { new  Objects().set(rnd).shuffle( vals ); return vals; }



    /** shuffle vals, using Rand.source */ public static  double [] shuffle( double [] vals) { new  doubles().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static   float [] shuffle(  float [] vals) { new   floats().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static boolean [] shuffle(boolean [] vals) { new booleans().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static    byte [] shuffle(   byte [] vals) { new    bytes().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static    char [] shuffle(   char [] vals) { new    chars().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static   short [] shuffle(  short [] vals) { new   shorts().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static    long [] shuffle(   long [] vals) { new    longs().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static     int [] shuffle(    int [] vals) { new     ints().shuffle( vals ); return vals; }
    /** shuffle vals, using Rand.source */ public static <TT> TT [] shuffle(     TT [] vals) { new  Objects().shuffle( vals ); return vals; }



    public static void main(String [] args) {
        new doubles().set( Rand.source ).shuffle( Util.colon(0.0, 20) );
        for (int val : shuffle( Util.colon( 'a', 20 ))) Print.prl( val );
        for (boolean val : shuffle( Util.step( 20 ))) Print.prl( val );
    }

}











