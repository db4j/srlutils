// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.nio.ByteBuffer;

/** a bunch of convenience methods to help with debugging */
public class Debug {
    public static boolean diff(ByteBuffer buf1,ByteBuffer buf2) {
        buf1.clear();
        buf2.clear();
        boolean diff = false;
        int len = buf1.limit();
        if (len != buf2.limit()) return true;
        for (int ii = 0; ii < len; ii+=4)
            if ( buf1.getInt(ii) != buf2.getInt(ii) ) {
                diff = true;
                System.out.format( "bbdiff: %5d -- %8x %8x\n", ii, buf1.getInt( ii ), buf2.getInt( ii ) );
            }
        return diff;
    }

    public static void print(ByteBuffer ... buffers) {
        int nn = buffers.length;
        int [] lens = new int[ nn ];
        int len = 0;
        for (int ii = 0; ii < nn; ii++) {
            ByteBuffer buffer = buffers[ii];
            buffer.clear();
            lens[ii] = buffer.limit() / 4;
            len = Math.max( len, lens[ii] );
        }
        for (int ii = 0; ii < len; ii++) {
            for (int jj = 0; jj < nn; jj++) {
                if (ii < lens[jj]) {
                    ByteBuffer buffer = buffers[jj];
                    int val = buffer.getInt( ii );
                    System.out.format( "0x%8x ", val );
                }
                else System.out.format( "%8s", "" );
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void print(ByteBuffer buffer,int width) {
        int len = buffer.limit() / 4;
        for (int ii = 0; ii < len; ii++) {
            int val = buffer.getInt( ii );
            String end = (ii%width)==(width-1) ? "\n" : " ";
            System.out.format( "0x%8x" + end, val );
        }
        System.out.println();
    }

}
