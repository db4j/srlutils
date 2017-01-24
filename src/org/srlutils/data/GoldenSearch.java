// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

public class GoldenSearch {

    public static final double r5 = Math.sqrt( 5 );
    public static final double a = .5*(3-r5);
    public static final double c = r5 - 2;
    public static final double caa = (c+a)/a;
    public static final double aac = a/(a+c);
    public static final double phi = a/(2*a+c);
    /*
     * requirements: the cost function must be unimodal with a max at the desired point
     * mappings:
     *   x-->f, y-->g, z-->h, w-->p
     * invariants:
     *   either of the following 2. in each case w is calculated
     *     (x) --a-- (y) --c-- (w) --a-- (z)
     *     (z) --a-- (w) --c-- (y) --a-- (x)
     *   g >= f, h
     */

    public static abstract class Costable {
        public boolean verbose = false;
        public abstract double cost(double x);

        public double search(double delta) { return search( delta, 0, 2*a+c ); }
        /** perform the golder search, starting at [x,z] and tolerance delta, return the maxima */
        public double search(double delta,double x,double z) {
            double y = x + phi*(z-x), w;
            double f = cost( x ), g = cost( y ), h = cost( z ), p;

            while (true) {
                if      (h>g && h>f) { y = z; g = h; z += (y-x)*caa; h = cost( z ); }
                else if (f>g && f>h) { y = x; g = f; x -= (z-x)*aac; f = cost( x ); }
                else break;
            }
            while (z-x > delta || x-z > delta) {
                if (verbose) System.out.format(
                        "Costable.search :: %8.3f %8.3f %8.3f -- %8.3f %8.3f %8.3f\n", x, y, z, f, g, h
                        );
                w = x + (y-x)*caa;
                p = cost( w );
                if (p>g) { x=y; f=g; y=w; g=p; }
                else     { z=x; h=f; x=w; f=p; }
            }
            return (x+z)/2;
        }
    }


    public static void main(String [] args) {
        final double xo = -77;

        Costable test = new Costable() {
            public double cost(double x) {
                return -(x - xo) * (x - xo);
            }
        };
        double x1 = test.search( .001, -70, 100 );
        System.out.format( "xo: %8.3f, x1: %8.3f\n", xo, x1 );
    }

}
