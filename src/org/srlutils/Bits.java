// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

/** representation of a bit-field */
public class Bits {
    /** position of the first bit kk and number of bits nn */
    public int kk,nn;
    public long mask, max;
    /** initialize to first bit $kk, number of bits $nn */
    public Bits(int $kk, int $nn) {
        kk = $kk;
        nn = $nn;
        mask = -1L >>> (64-nn);
        mask <<= kk;
        max = (1L<<nn) - 1;
    }
    /** set this flag to val in word */
    public long set(long word,long val) {
        word &= -1L^mask;
        val <<= kk;
        val &= mask;
        word |= val;
        return word;
    }
    /** get the value of this flag from word */
    public int get(long word) {
        word &= mask;
        return (int) (word >>> kk);
    }
    public long getl(long word) {
        word &= mask;
        return (word >>> kk);
    }
    
    private static class Demo {
        public long flag = 0;
        Bits aa = new Bits(32,16), bb = new Bits(48,16);
        public void set(long av,long bv) {
            flag = aa.set(flag,av);
            flag = bb.set(flag,bv);
            long ao = aa.get(flag),
                    bo = bb.get(flag);
            if (ao==av && bo==bv) {}
            else {
                System.out.format( "%09x %09x -- %09x %09x\n", ao, av, bo, bv );
                Simple.softAssert( false );
            }
        }
        public void test() {
            for (long av=0; av<aa.max; av++)
                for (long bv=0; bv<bb.max; bv++)
                    set(av,bv);
        }
    }
    
    public static void main(String [] args) {
        Bits bitsJar = new Bits( 48, 12 );
        Bits bitsPrev = new Bits( 32, 16 );
        long flag = 0;
        flag = bitsJar.set(flag,4088);
        System.out.println( bitsJar.get(flag) );
        flag = bitsPrev.set(flag,-1);
        System.out.println( bitsJar.get(flag) );
        System.out.format( "max: %d, val: %d\n", bitsPrev.max, bitsPrev.get(flag) );
        
        new Demo().test();
    }
}
