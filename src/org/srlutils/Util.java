// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import static org.srlutils.Array.alloc;

public class Util {
    // comments must come before the methods


    public static boolean[] step(int nn)        { return step( nn, nn/2, nn ); }
    public static boolean[] step(int nn,int k1) { return step( nn, k1,   nn ); }
    public static boolean[] step(int nn, int k1, int k2) {
        boolean[] kk = new boolean[nn];
        for (int ii = k1; ii < k2; ii++) kk[ii] = true;
        return kk;
    }

    /** returns a new array 0:nn-1                   */   public static int    [] colon(          int nn) {                                return colon(0,nn); }
    /** returns a new array ko:ko+nn-1               */   public static int    [] colon(int    ko,int nn) { int    [] kk = new int   [nn]; return colon(ko,kk); }
    /** returns a new array ko:ko+nn-1               */   public static double [] colon(double ko,int nn) { double [] kk = new double[nn]; return colon(ko,kk); }
    /** returns a new array ko:ko+nn-1               */   public static float  [] colon(float  ko,int nn) { float  [] kk = new float [nn]; return colon(ko,kk); }
    /** returns a new array ko:ko+nn-1               */   public static byte   [] colon(byte   ko,int nn) { byte   [] kk = new byte  [nn]; return colon(ko,kk); }
    /** returns a new array ko:ko+nn-1               */   public static char   [] colon(char   ko,int nn) { char   [] kk = new char  [nn]; return colon(ko,kk); }
    /** returns a new array ko:ko+nn-1               */   public static short  [] colon(short  ko,int nn) { short  [] kk = new short [nn]; return colon(ko,kk); }
    /** returns a new array ko:ko+nn-1               */   public static long   [] colon(long   ko,int nn) { long   [] kk = new long  [nn]; return colon(ko,kk); }


    /** fills kk with 0:length-1                     */   public static int    [] colon(int    [] kk) { return colon(0,kk); }
    /** fills kk with 0:length-1                     */   public static double [] colon(double [] kk) { return colon(0,kk); }
    /** fills kk with 0:length-1                     */   public static float  [] colon(float  [] kk) { return colon(0,kk); }
    /** fills kk with 0:length-1                     */   public static long   [] colon(long   [] kk) { return colon(0,kk); }
    /** fills kk with 0:length-1                     */   public static byte   [] colon(byte   [] kk) { return colon((byte) 0,kk); }
    /** fills kk with 0:length-1                     */   public static char   [] colon(char   [] kk) { return colon((char) 0,kk); }
    /** fills kk with 0:length-1                     */   public static short  [] colon(short  [] kk) { return colon((short)0,kk); }


    /** fills kk with ko:ko+length-1                 */   public static int    [] colon(int    ko,int    [] kk) { return colon(ko,1,kk); }
    /** fills kk with ko:ko+length-1                 */   public static double [] colon(double ko,double [] kk) { return colon(ko,1,kk); }
    /** fills kk with ko:ko+length-1                 */   public static float  [] colon(float  ko,float  [] kk) { return colon(ko,1,kk); }
    /** fills kk with ko:ko+length-1                 */   public static long   [] colon(long   ko,long   [] kk) { return colon(ko,1,kk); }
    /** fills kk with ko:ko+length-1                 */   public static byte   [] colon(byte   ko,byte   [] kk) { return colon(ko,(byte) 1,kk); }
    /** fills kk with ko:ko+length-1                 */   public static char   [] colon(char   ko,char   [] kk) { return colon(ko,(char) 1,kk); }
    /** fills kk with ko:ko+length-1                 */   public static short  [] colon(short  ko,short  [] kk) { return colon(ko,(short)1,kk); }



    /** fills kk with ko:step:ko+step*(length-1)     */   public static int    [] colon(int    ko,int    step,int    [] kk) { int    val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }
    /** fills kk with ko:step:ko+step*(length-1)     */   public static long   [] colon(long   ko,long   step,long   [] kk) { long   val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }
    /** fills kk with ko:step:ko+step*(length-1)     */   public static byte   [] colon(byte   ko,byte   step,byte   [] kk) { byte   val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }
    /** fills kk with ko:step:ko+step*(length-1)     */   public static char   [] colon(char   ko,char   step,char   [] kk) { char   val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }
    /** fills kk with ko:step:ko+step*(length-1)     */   public static short  [] colon(short  ko,short  step,short  [] kk) { short  val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }
    /** fills kk with ko:step:ko+step*(length-1)     */   public static double [] colon(double ko,double step,double [] kk) { double val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }
    /** fills kk with ko:step:ko+step*(length-1)     */   public static float  [] colon(float  ko,float  step,float  [] kk) { float  val = ko; for (int ii = 0; ii < kk.length; ii++, val+=step) kk[ii] = val; return kk; }


    /** new array with vals[kk]                      */   public static double  [] select(double  []vals,int []kk) { double  [] ret = new double [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static boolean [] select(boolean []vals,int []kk) { boolean [] ret = new boolean[kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static float   [] select(float   []vals,int []kk) { float   [] ret = new float  [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static short   [] select(short   []vals,int []kk) { short   [] ret = new short  [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static long    [] select(long    []vals,int []kk) { long    [] ret = new long   [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static char    [] select(char    []vals,int []kk) { char    [] ret = new char   [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static int     [] select(int     []vals,int []kk) { int     [] ret = new int    [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static byte    [] select(byte    []vals,int []kk) { byte    [] ret = new byte   [kk.length]; return select( vals, kk, ret ); }
    /** new array with vals[kk]                      */   public static <TT> TT [] select(TT      []vals,int []kk) { TT [] ret = Array.alloc(vals,kk.length); return select( vals, kk, ret ); }

    /** fills ret with vals[kk]                      */   public static double  [] select(double  []vals,int []kk, double  []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static boolean [] select(boolean []vals,int []kk, boolean []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static float   [] select(float   []vals,int []kk, float   []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static byte    [] select(byte    []vals,int []kk, byte    []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static char    [] select(char    []vals,int []kk, char    []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static short   [] select(short   []vals,int []kk, short   []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static int     [] select(int     []vals,int []kk, int     []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static long    [] select(long    []vals,int []kk, long    []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }
    /** fills ret with vals[kk]                      */   public static <TT> TT [] select(TT      []vals,int []kk, TT      []ret) { for (int ii=0; ii<kk.length; ii++) ret[ii] = vals[kk[ii]]; return ret; }

    /** new array with vals[kk]                      */   public static double [] select(double  []vals,boolean []kk) { double  []ret = new double [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static float  [] select(float   []vals,boolean []kk) { float   []ret = new float  [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static byte   [] select(byte    []vals,boolean []kk) { byte    []ret = new byte   [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static char   [] select(char    []vals,boolean []kk) { char    []ret = new char   [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static short  [] select(short   []vals,boolean []kk) { short   []ret = new short  [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static int    [] select(int     []vals,boolean []kk) { int     []ret = new int    [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static long   [] select(long    []vals,boolean []kk) { long    []ret = new long   [ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static boolean[] select(boolean []vals,boolean []kk) { boolean []ret = new boolean[ sum(kk) ]; select( vals, kk, ret ); return ret; }
    /** new array with vals[kk]                      */   public static <TT> TT[] select(TT      []vals,boolean []kk) { TT [] ret = Array.alloc(vals,sum(kk) ); select( vals, kk, ret ); return ret; }


    /** fills ret with vals[kk], return length       */   public static int       select(double []vals,boolean []kk,  double []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(float  []vals,boolean []kk,  float  []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(byte   []vals,boolean []kk,  byte   []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(char   []vals,boolean []kk,  char   []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(short  []vals,boolean []kk,  short  []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(int    []vals,boolean []kk,  int    []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(long   []vals,boolean []kk,  long   []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static int       select(boolean[]vals,boolean []kk,  boolean[]ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }
    /** fills ret with vals[kk], return length       */   public static <TT> int  select(TT     []vals,boolean []kk,  TT     []ret) { int jj=0; for (int ii=0; ii<kk.length; ii++) { if ( kk[ii] ) ret[jj++] = vals[ii]; } return jj; }



    
    
    /** cumulative sum, overwrite                    */   public static double [] cumsum(double...vals) { for (int ii=1; ii < vals.length; ii++) vals[ii] += vals[ii-1]; return vals; }
    /** cumulative sum, overwrite                    */   public static int    [] cumsum(int   ...vals) { for (int ii=1; ii < vals.length; ii++) vals[ii] += vals[ii-1]; return vals; }

    
    /** sum of the elements                          */   public static  double sum( double...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static  double sum(  float...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static     int sum(boolean...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static    long sum(   byte...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static    long sum(   char...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static    long sum(  short...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static    long sum(   long...vals) { return Ranged.sum( 0, vals.length, vals ); }
    /** sum of the elements                          */   public static    long sum(    int...vals) { return Ranged.sum( 0, vals.length, vals ); }

    /** element by element difference                */   public static double [] ddiff(double...vals) { double ret[] = new double[vals.length]; return diff( vals, ret ); }
    /** element by element difference                */   public static int    [] idiff(int   ...vals) { int    ret[] = new int   [vals.length]; return diff( vals, ret ); }
    /** element by element difference, trimmed       */   public static double [] ddift(double...vals) { int nn=vals.length;  if(nn>0)nn--; return diff( vals, new double[nn] ); }
    /** element by element difference, trimmed       */   public static int    [] ddift(int   ...vals) { int nn=vals.length;  if(nn>0)nn--; return diff( vals, new int   [nn] ); }


    public static class Ranged {
        /** sum of     vals[k1:k2)  */   public static  double sum(int k1,int k2, double...vals) {  double val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }
        /** sum of     vals[k1:k2)  */   public static  double sum(int k1,int k2,  float...vals) {  double val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }
        /** sum of     vals[k1:k2)  */   public static     int sum(int k1,int k2,boolean...vals) {     int val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii] ? 1:0; return val; }
        /** sum of     vals[k1:k2)  */   public static    long sum(int k1,int k2,   byte...vals) {    long val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }
        /** sum of     vals[k1:k2)  */   public static    long sum(int k1,int k2,   char...vals) {    long val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }
        /** sum of     vals[k1:k2)  */   public static    long sum(int k1,int k2,  short...vals) {    long val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }
        /** sum of     vals[k1:k2)  */   public static    long sum(int k1,int k2,   long...vals) {    long val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }
        /** sum of     vals[k1:k2)  */   public static    long sum(int k1,int k2,    int...vals) {    long val = 0; for (int ii=k1; ii < k2; ii++) val += vals[ii]; return val; }


        /** minimum of the vals[k1,k2)                   */   public static  double min(int k1,int k2, double...vals) { double val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static   float min(int k1,int k2,  float...vals) {  float val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static    byte min(int k1,int k2,   byte...vals) {   byte val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static    char min(int k1,int k2,   char...vals) {   char val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static   short min(int k1,int k2,  short...vals) {  short val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static    long min(int k1,int k2,   long...vals) {   long val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static     int min(int k1,int k2,    int...vals) {    int val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
        /** minimum of the vals[k1,k2)                   */   public static <TT extends Comparable> TT min(int k1,int k2,     TT...vals) {     TT val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii].compareTo(val) < 0) val = vals[ii]; return val; }

        /** maximum of the vals[k1,k2)                   */   public static  double max(int k1,int k2, double...vals) { double val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static   float max(int k1,int k2,  float...vals) {  float val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static    byte max(int k1,int k2,   byte...vals) {   byte val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static    char max(int k1,int k2,   char...vals) {   char val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static   short max(int k1,int k2,  short...vals) {  short val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static    long max(int k1,int k2,   long...vals) {   long val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static     int max(int k1,int k2,    int...vals) {    int val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > val) val = vals[ii]; return val; }
        /** maximum of the vals[k1,k2)                   */   public static <TT extends Comparable> TT max(int k1,int k2,     TT...vals) {     TT val = vals[k1]; for (int ii=k1+1; ii<k2; ii++) if (vals[ii].compareTo(val) > 0) val = vals[ii]; return val; }

        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2, double...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2,  float...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2,   byte...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2,   char...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2,  short...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2,   long...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static int   mini(int k1,int k2,    int...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] < vals[ko]) ko=ii; return ko; }
        /** index of the min of vals[k1,k2)               */   public static <TT extends Comparable>
                                                                            int   mini(int k1,int k2,     TT...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii].compareTo(vals[ko]) < 0) ko=ii; return ko; }

        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2, double...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2,  float...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2,   byte...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2,   char...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2,  short...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2,   long...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static int   maxi(int k1,int k2,    int...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii] > vals[ko]) ko=ii; return ko; }
        /** index of the max of vals[k1,k2)               */   public static <TT extends Comparable>
                                                                            int    maxi(int k1,int k2,     TT...vals) { int ko=k1; for (int ii=k1+1; ii<k2; ii++) if (vals[ii].compareTo(vals[ko]) > 0) ko=ii; return ko; }

        
        // booleans are special, and maybe not that useful, but keeping them for consistency, and for generating other classes
        /** minimum of the vals[k1,k2)                   */   public static boolean min (int k1,int k2,boolean...vals) { for (int ii=k1; ii<k2; ii++) if (!vals[ii]) return false; return true;  }
        /** minimum of the vals[k1,k2)                   */   public static boolean max (int k1,int k2,boolean...vals) { for (int ii=k1; ii<k2; ii++) if ( vals[ii]) return true;  return false; }
        /** minimum of the vals[k1,k2)                   */   public static     int mini(int k1,int k2,boolean...vals) { for (int ii=k1; ii<k2; ii++) if (!vals[ii]) return ii; return 0; }
        /** minimum of the vals[k1,k2)                   */   public static     int maxi(int k1,int k2,boolean...vals) { for (int ii=k1; ii<k2; ii++) if ( vals[ii]) return ii; return 0; }


    }




    /** element by element difference                */   public static  double [] diff( double[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static   float [] diff(  float[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static boolean [] diff(boolean[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static    byte [] diff(   byte[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static    char [] diff(   char[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static   short [] diff(  short[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static    long [] diff(   long[]vals) { return diff( vals, alloc( vals ) ); }
    /** element by element difference                */   public static     int [] diff(    int[]vals) { return diff( vals, alloc( vals ) ); }


    /** element by element difference                */   public static  double [] dift( double[]vals) { return diff( vals, new  double[vals.length-1] ); }
    /** element by element difference                */   public static   float [] dift(  float[]vals) { return diff( vals, new   float[vals.length-1] ); }
    /** element by element difference                */   public static boolean [] dift(boolean[]vals) { return diff( vals, new boolean[vals.length-1] ); }
    /** element by element difference                */   public static    byte [] dift(   byte[]vals) { return diff( vals, new    byte[vals.length-1] ); }
    /** element by element difference                */   public static    char [] dift(   char[]vals) { return diff( vals, new    char[vals.length-1] ); }
    /** element by element difference                */   public static   short [] dift(  short[]vals) { return diff( vals, new   short[vals.length-1] ); }
    /** element by element difference                */   public static    long [] dift(   long[]vals) { return diff( vals, new    long[vals.length-1] ); }
    /** element by element difference                */   public static     int [] dift(    int[]vals) { return diff( vals, new     int[vals.length-1] ); }


    /** element by element difference                */   public static  double [] diff( double[]vals, double[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = vals[ii+1]-vals[ii]; return ret; }
    /** element by element difference                */   public static   float [] diff(  float[]vals,  float[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = vals[ii+1]-vals[ii]; return ret; }
    /** element by element difference                */   public static boolean [] diff(boolean[]vals,boolean[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = vals[ii+1] ^ vals[ii]; return ret; }
    /** element by element difference                */   public static    byte [] diff(   byte[]vals,   byte[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = (byte) (vals[ii + 1] - vals[ii]); return ret; }
    /** element by element difference                */   public static    char [] diff(   char[]vals,   char[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = (char) (vals[ii + 1] - vals[ii]); return ret; }
    /** element by element difference                */   public static   short [] diff(  short[]vals,  short[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = (short) (vals[ii + 1] - vals[ii]); return ret; }
    /** element by element difference                */   public static    long [] diff(   long[]vals,   long[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = vals[ii+1]-vals[ii]; return ret; }
    /** element by element difference                */   public static     int [] diff(    int[]vals,    int[]ret) { for (int ii=0; ii<vals.length-1; ii++) ret[ii] = vals[ii+1]-vals[ii]; return ret; }



    /** logical and of the elements                  */   public static boolean and(boolean...vals) { return sum(vals)==vals.length; }
    /** logical and of the elements                  */   public static boolean  or(boolean...vals) { return sum(vals) > 0; }



    /** minimum of the elements                      */   public static  double min( double...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static   float min(  float...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static    byte min(   byte...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static    char min(   char...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static   short min(  short...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static    long min(   long...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static     int min(    int...vals) { return Ranged.min(0,vals.length,vals); }
    /** minimum of the elements                      */   public static <TT extends Comparable> TT min(     TT...vals) { return Ranged.min(0,vals.length,vals); }


    /** maximum of the elements                      */   public static  double max( double...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static   float max(  float...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static    byte max(   byte...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static    char max(   char...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static   short max(  short...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static    long max(   long...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static     int max(    int...vals) { return Ranged.max(0,vals.length,vals); }
    /** maximum of the elements                      */   public static <TT extends Comparable> TT max(     TT...vals) { return Ranged.max(0,vals.length,vals); }


    /** index of the minimum                         */   public static int   mini( double...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static int   mini(  float...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static int   mini(   byte...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static int   mini(   char...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static int   mini(  short...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static int   mini(   long...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static int   mini(    int...vals) { return Ranged.mini(0,vals.length,vals); }
    /** index of the minimum                         */   public static <TT extends Comparable> int   mini(     TT...vals) { return Ranged.mini(0,vals.length,vals); }


    /** index of the maximum                         */   public static int   maxi( double...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static int   maxi(  float...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static int   maxi(   byte...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static int   maxi(   char...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static int   maxi(  short...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static int   maxi(   long...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static int   maxi(    int...vals) { return Ranged.maxi(0,vals.length,vals); }
    /** index of the maximum                         */   public static <TT extends Comparable> int   maxi(     TT...vals) { return Ranged.maxi(0,vals.length,vals); }




    /** minimum of the elements                      */   public static    int imin(   int...vals) {    int val = vals[0]; for (int ii=1; ii<vals.length; ii++) if (vals[ii] < val) val = vals[ii]; return val; }
    /** maximum of the elements                      */   public static    int imax(   int...vals) {    int val = vals[0]; for (int ii=1; ii<vals.length; ii++) if (vals[ii] > val) val = vals[ii]; return val; }

    /** indices of the bounds  [min,max]             */   public static int    [] boundi(double...vals) { int k1,k2=k1=0; for (int ii=1; ii<vals.length; ii++) if (vals[ii] < vals[k1]) k1=ii; else if (vals[ii] > vals[k2]) k2=ii; return new int[] { k1, k2 }; }

    /** values of the bounds   [min,max]             */   public static  double [] bounds( double...vals) {  double v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new  double[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static   float [] bounds(  float...vals) {   float v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new   float[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static boolean [] bounds(boolean...vals) { boolean v0,v1=true,v2=false; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0)      v2 = v0; else              v1 = v0; } return new boolean[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static    byte [] bounds(   byte...vals) {    byte v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new    byte[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static    char [] bounds(   char...vals) {    char v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new    char[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static   short [] bounds(  short...vals) {   short v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new   short[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static    long [] bounds(   long...vals) {    long v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new    long[] { v1, v2 }; }
    /** values of the bounds   [min,max]             */   public static     int [] bounds(    int...vals) {     int v0,v1,v2=v1=vals[0]; for (int ii=1; ii<vals.length; ii++) { v0 = vals[ii]; if (v0 < v1) v1 = v0; else if (v0 > v2) v2 = v0; } return new     int[] { v1, v2 }; }


    public static int divup(int val,int den) { return (val+den-1) / den; }



    /** indices of the bounds  [min,max]             */   public static int    [] boundi(int   ...vals) { int k1,k2=k1=0; for (int ii=1; ii<vals.length; ii++) if (vals[ii] < vals[k1]) k1=ii; else if (vals[ii] > vals[k2]) k2=ii; return new int[] { k1, k2 }; }

    /** bound each element     [min,max]             */   public static double [] bound(double min,double max,double...vals) { for (int ii = 0; ii < vals.length; ii++) vals[ii] = Scalar.bound( min, max, vals[ii] ); return vals; }
    /** bound each element     [min,max]             */   public static int    [] bound(int    min,int    max,int   ...vals) { for (int ii = 0; ii < vals.length; ii++) vals[ii] = Scalar.bound( min, max, vals[ii] ); return vals; }

    public static class Scalar {
        /** bound each element     [min,max]             */
        public static double bound(double min,double max,double val) { return (val < min) ? min : (val > max ? max : val); }
        public static long   bound(long   min,long   max,long   val) { return (val < min) ? min : (val > max ? max : val); }
        public static int    bound(int    min,int    max,int    val) { return (val < min) ? min : (val > max ? max : val); }
    }



    public static class Inplace {
        /** add v1 to v2, overwrite                      */   public static  double [] add( double[]v1, double[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }
        /** add v1 to v2, overwrite                      */   public static   float [] add(  float[]v1,  float[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }
        /** add v1 to v2, overwrite                      */   public static    byte [] add(   byte[]v1,   byte[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }
        /** add v1 to v2, overwrite                      */   public static    char [] add(   char[]v1,   char[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }
        /** add v1 to v2, overwrite                      */   public static   short [] add(  short[]v1,  short[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }
        /** add v1 to v2, overwrite                      */   public static    long [] add(   long[]v1,   long[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }
        /** add v1 to v2, overwrite                      */   public static     int [] add(    int[]v1,    int[]v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1[ii]; return v2; }



        /** add v1 to v2, overwrite                      */   public static  double [] add( double v1, double...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }
        /** add v1 to v2, overwrite                      */   public static   float [] add(  float v1,  float...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }
        /** add v1 to v2, overwrite                      */   public static    byte [] add(   byte v1,   byte...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }
        /** add v1 to v2, overwrite                      */   public static    char [] add(   char v1,   char...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }
        /** add v1 to v2, overwrite                      */   public static   short [] add(  short v1,  short...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }
        /** add v1 to v2, overwrite                      */   public static    long [] add(   long v1,   long...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }
        /** add v1 to v2, overwrite                      */   public static     int [] add(    int v1,    int...v2) { for (int ii=0; ii < v2.length; ii++) v2[ii] += v1; return v2; }

        /** multiply v2 by v1, overwrite v2              */   public static  double [] mult( double v1, double...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }
        /** multiply v2 by v1, overwrite v2              */   public static   float [] mult(  float v1,  float...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }
        /** multiply v2 by v1, overwrite v2              */   public static    byte [] mult(   byte v1,   byte...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }
        /** multiply v2 by v1, overwrite v2              */   public static    char [] mult(   char v1,   char...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }
        /** multiply v2 by v1, overwrite v2              */   public static   short [] mult(  short v1,  short...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }
        /** multiply v2 by v1, overwrite v2              */   public static    long [] mult(   long v1,   long...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }
        /** multiply v2 by v1, overwrite v2              */   public static     int [] mult(    int v1,    int...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]*=v1; return v2; }

        /** xor v1 to v2, overwrite v2                   */   public static boolean [] xor(boolean v1,boolean...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]^=v1; return v2; }
        /** and v2 with v1, overwrite v2                 */   public static boolean [] and(boolean v1,boolean...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]&=v1; return v2; }
        /** and v2 with v1, overwrite v2                 */   public static boolean [] or (boolean v1,boolean...v2) { for (int ii=0;ii<v2.length;ii++) v2[ii]|=v1; return v2; }
    }






    /** swap elements of an array                    */   public static void swap(double [] vals,int k1,int k2) { double vo = vals[k1]; vals[k1] = vals[k2]; vals[k2] = vo; }
    /** swap elements of an array                    */   public static void swap(int    [] vals,int k1,int k2) { int    vo = vals[k1]; vals[k1] = vals[k2]; vals[k2] = vo; }
    /** swap elements of an array                    */   public static void swap(Object [] vals,int k1,int k2) { Object vo = vals[k1]; vals[k1] = vals[k2]; vals[k2] = vo; }

    /** return a new copy of an array                */   public static double [] dup(double [] vals) { return (double []) vals.clone(); }
    /** return a new copy of an array                */   public static float  [] dup(float  [] vals) { return (float  []) vals.clone(); }
    /** return a new copy of an array                */   public static long   [] dup(long   [] vals) { return (long   []) vals.clone(); }
    /** return a new copy of an array                */   public static int    [] dup(int    [] vals) { return (int    []) vals.clone(); }
    /** return a new copy of an array                */   public static short  [] dup(short  [] vals) { return (short  []) vals.clone(); }
    /** return a new copy of an array                */   public static char   [] dup(char   [] vals) { return (char   []) vals.clone(); }
    /** return a new copy of an array                */   public static byte   [] dup(byte   [] vals) { return (byte   []) vals.clone(); }
    /** return a new copy of an array                */   public static boolean[] dup(boolean[] vals) { return (boolean[]) vals.clone(); }


    /*
     * note:
     * dup(vals,k1,k2) vals can be null
     * dup(src,dst) src must be non-null
     */

    /** return a new copy of vals[k1:k2), 0-padded */   public static  double [] dup( double [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new  double[k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static  double [] dup( double [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static  double [] dup( double [] src, double [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), overlap ok */   public static  double [] dup( double [] src,int k1,int k2, double [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }


    /** return a new copy of vals[k1:k2), 0-padded */   public static   float [] dup(  float [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new   float[k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static   float [] dup(  float [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static   float [] dup(  float [] src,  float [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), overlap ok */   public static   float [] dup(  float [] src,int k1,int k2,  float [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }





    /** return a new copy of vals[k1:k2), 0-padded */   public static boolean [] dup(boolean [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new boolean[k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static boolean [] dup(boolean [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static boolean [] dup(boolean [] src,boolean [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), overlap ok */   public static boolean [] dup(boolean [] src,int k1,int k2,boolean [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }





    /** return a new copy of vals[k1:k2), 0-padded */   public static    byte [] dup(   byte [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new    byte[k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static    byte [] dup(   byte [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static    byte [] dup(   byte [] src,   byte [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), overlap ok */   public static    byte [] dup(   byte [] src,int k1,int k2,   byte [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }





    /** return a new copy of vals[k1:k2), 0-padded */   public static    char [] dup(   char [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new    char[k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static    char [] dup(   char [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static    char [] dup(   char [] src,   char [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), overlap ok */   public static    char [] dup(   char [] src,int k1,int k2,   char [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }





    /** return a new copy of vals[k1:k2), 0-padded */   public static   short [] dup(  short [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new   short[k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static   short [] dup(  short [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static   short [] dup(  short [] src,  short [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), overlap ok */   public static   short [] dup(  short [] src,int k1,int k2,  short [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }




    /** return a new copy of vals[k1:k2), 0-padded */   public static long   [] dup(long   [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new long  [k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static long   [] dup(long   [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static long   [] dup(long   [] src,long   [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*)             */   public static long   [] dup(long   [] src,int k1,int k2,long   [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }

    /** return a new copy of vals[k1:k2), 0-padded */   public static int    [] dup(int    [] vals,int k1,int k2) { return dup( vals, k1, imin(k2,vals==null ? 0 : vals.length), new int   [k2-k1], 0 ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static int    [] dup(int    [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** copy src into dst, return dst, overlap only  */   public static int    [] dup(int    [] src,int    [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*), safe       */   public static int[] dup(int[] src, int k1, int k2, int[] dst, int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }
    /** copy src[k1:k2) -> dst[offset:*), unsafe     */
    public static int[] dupFast(int[] src, int k1, int k2, int[] dst, int offset) {
        System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }

    
    /** return a new copy of vals[k1:* ), 0-padded */   public static <TT> TT     [] dup(TT     [] vals,Class type,int k1) { return dup( vals, type, k1, vals.length ); }
    /** return a new copy of vals[k1:* ), 0-padded */   public static <TT> TT     [] dup(TT     [] vals,int k1) { return dup( vals, k1, vals.length ); }
    /** return a new copy of vals[k1:k2), 0-padded */   public static <TT> TT     [] dup(TT     [] vals,int k1,int k2) {
        Class type = vals.getClass().getComponentType();
        return dup( vals, type, k1, k2 );
    }
    /** return a new copy of vals[k1:k2), 0-padded */   public static <TT> TT     [] dup(TT     [] vals,Class type,int k1,int k2) {
        // generic array creation
        TT [] arr = (TT[]) java.lang.reflect.Array.newInstance( type, k2-k1 );
        int oldend = imin(k2,vals==null ? 0 : vals.length);
        return dup( vals, k1, oldend, arr, 0 );
    }
    /** copy src into dst, return dst, overlap only  */   public static <TT> TT     [] dup(TT     [] src,TT     [] dst) { int nn = imin( src.length, dst.length ); System.arraycopy(src, 0, dst, 0, nn); return dst; }
    /** copy src[k1:k2) -> dst[offset:*)             */   public static <TT> TT     [] dup(TT     [] src,int k1,int k2,TT [] dst,int offset) {
        if (isrange(0, k2 - k1, 0, dst.length - offset))
            System.arraycopy(src, k1, dst, offset, k2 - k1);
        return dst;
    }

    /** return the next mult of block larger than val */
    public static int  nextMult(int  val,int  block) { return (val/block + 1)*block; }
    /** return the next mult of block larger than val */
    public static long nextMult(long val,long block) { return (val/block + 1)*block; }

    /** check that the ranges are not empty */
    private static boolean isrange(int j1,int j2,int k1,int k2) { return j1<j2 && k1<k2; }

}
