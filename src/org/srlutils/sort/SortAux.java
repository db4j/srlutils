// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort;

import org.srlutils.sort.templated.DirectFast;

public class SortAux {

    public static void swap(float []a,int k1,int k2) { float v = a[k1]; a[k2] = a[k1]; a[k1] = v; }


    public static void sort2(float a[], int fromIndex, int toIndex) {
        final int NEG_ZERO_BITS = Float.floatToIntBits(-0.0f);

        // Preprocessing phase:  Move NaN's and count -0.0s and make 0.0
        int numNegZeros = 0;
        int i = fromIndex, n = toIndex;
        while(i < n) {
            if (a[i] != a[i]) swap( a, i, --n );
            else {
                if (a[i]==0 && Float.floatToIntBits(a[i])==NEG_ZERO_BITS) {
                    a[i] = 0.0f;
                    numNegZeros++;
                }
                i++;
            }
        }
        // Main sort phase: quicksort everything but the NaN's
	DirectFast.floats.quicksort( new DirectFast.floats.Data().set(a,null), fromIndex, n-1);
        // Postprocessing phase: change 0.0's to -0.0's as required
        if (numNegZeros != 0) {
            int j = java.util.Arrays.binarySearch(a, fromIndex, n, 0.0f); // posn of ANY zero
            do { j--; } while (j>=0 && a[j]==0.0f);
            for (int k=0; k<numNegZeros; k++) a[++j] = -0.0f;
        }
    }
}











