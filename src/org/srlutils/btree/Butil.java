// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

public class Butil {
    public static int compare(double key1,double key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    public static int compare(float  key1,float  key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    public static int compare(int    key1,int    key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    public static int compare(long   key1,long   key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    public static int compare(short  key1,short  key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    public static int compare(byte   key1,byte   key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    public static int compare(char   key1,char   key2) { return key1>key2 ? 1 : key1<key2 ? -1:0; }
    
    /** 
     * representation of the different search modes
     * 0: greater than or equal
     * 1: strictly greater than
     * 2: strictly greater than a prefix
     */
    public static class Modes {
        int next = 0;
        /** find the first >=           element */ public final int gte = next++;
        /** find the first >            element */ public final int gt = next++;
        /** find the first element > prefix     */ public final int gtp = next++;
        /** find the first element > prefix     */ public final int eq  = next++;
        boolean [] prefix, after;
        public Modes() {
            prefix = new boolean [ next ];
            prefix[ gtp ] = true;
            after = new boolean [ next ];
            after[ gt ] = true;
            after[ gtp ] = true;
        }
        /** does this search mode imply prefix matching ? */
        public boolean prefix(int mode) { return prefix[ mode ]; }
        /** 
         * does this search mode imply 
         *   true:"greater than"
         *   or false:"greater than or equal" matching ?
         */
        public boolean greater(int mode) { return after[ mode ]; }
        /**
         * map equality to larger for the signum function returned by comparisons,
         * ie if greater is true, find the entry greater than, instead of greater than or equal
         */
        public static int mapEq(boolean greater,int cmp) { return (cmp==0 && greater) ? 1 : cmp; }
        public boolean eq(int mode) { return mode==eq; }
    }
}
