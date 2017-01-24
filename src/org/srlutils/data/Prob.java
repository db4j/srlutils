// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import static org.srlutils.Array.grow;

public class Prob {
    public static class Prec {
        public double dbl;
        public int bias;
        public Prec(double $dbl          ) { dbl = $dbl;               }
        public Prec(double $dbl,int $bias) { dbl = $dbl; bias = $bias; }

        public double get() { return Math.scalb( dbl, bias ); }

        public Prec div (double d2) { dbl /= d2;                      return renorm(); }
        public Prec mult(double d2) { dbl *= d2;                      return renorm(); }
        public Prec div (Prec   p2) { dbl /= p2.dbl; bias -= p2.bias; return renorm(); }
        public Prec mult(Prec   p2) { dbl *= p2.dbl; bias += p2.bias; return renorm(); }
        public Prec renorm() {
            int exp = Math.getExponent( dbl );
            dbl = Math.scalb( dbl, -exp );
            bias += exp;
            return this;
        }
    }
    public static class IntegerGamma {
        public double x, g;
        public int j;
        public double [] gk, pk;
        public int [] bk;
        public Prec fp;
        // e^-x * sum x^k / k!
        //   break the e^-x term into a power of 2 (bias) and a fraction part frac
        // f = frac * x^k / k!
        // g_k = sum i = 0:k-1 (f_i * 2^bias)
        public IntegerGamma(double $x, int len) {
            x = $x;
            g = 0;
            fp = new Prec( Math.exp(-x) );
            calcTo( len );
        }
        /** calculate and return gamma(x,k) */
        public double eval(int k) {
            if ( k >= gk.length ) calcTo( k+1 );
            // fixme::precision -- should probably be using Math.expm1
            return 1 - gk[k];
        }
        public Prec pois(int k) {
            if ( k >= gk.length ) calcTo( k+1 );
            return new Prec( pk[k], bk[k] );
        }

        public final void calcTo(int len) {
            pk = grow( pk, len );
            bk = grow( bk, len );
            gk = grow( gk, len );
            while (j < gk.length) {
                gk[j] = g;
                pk[j] = fp.dbl;
                bk[j] = fp.bias;
                j++;
                // fixme::precision -- if the bias was incremented only when needed, the sum g could
                //   use the scaled values and then downshift when bias changes ... higher precision
                g += fp.get();
                fp.mult( x/j );
            }
        }
        public void test(int nn) {
            double [] theirs = new double[ nn ];
            // note: uncomment this block to compare the values with apache commons math
            //       commented out to avoid drawing in a dependency
            // fixme::test -- would be nice to have a dependency-free test ... eg, checking a poisson distribution
            //    try {
            //        for (int k = 0; k < nn; k++)
            //            theirs[k] = org.apache.commons.math.special.Gamma.regularizedGammaP( k, x );
            //    }
            //    catch (Exception ex) {}
            double ours = 0;
            for (int k = 1; k < nn; k++)
                System.out.format( "%5d -- %8.3f %8.3f -- % 8e\n", k, ours = eval(k), theirs[k], theirs[k] - ours );
        }

    }

    public static void main(String [] args) {
        int x = 700;
        new IntegerGamma( x, 100 ).test( x*2 );
    }

}
