// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.sort.templated;

import org.srlutils.Sorter.Config;
import org.srlutils.Sorter.Part;
import org.srlutils.Util;
import org.srlutils.sort.MacroDevel.Markers.*;
import static org.srlutils.sort.MacroDevel.Markers.*;

public class Direct {
    public static final Config cfg = new Config( 30, true, null );
    public static final boolean yes = yes( which() );

    public static class DataRoot {
        Config cfg = Direct.cfg;
        public DataRoot config(Config _cfg) { cfg = _cfg; return this; }
    }

    private static final int [] order(DataRoot zz) { return null; }
    private static final int [] obuf(DataRoot zz) { return null; }

    private static final No which() { return null; }

    /** return a random value in [k1,k2) */
    private static int rand(DataRoot zz,int k1,int k2) { return k1+zz.cfg.rand.rand( 0,k2-k1 ); }















    /* XXX - remainder of this file is semi-auto-generated from MacroDevel.java, see readme.txt */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class  doubles {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
             double [] vals, vbuf;
            public Data set( double[] _vals, double[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv( double v1, double v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv( double v1, double v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
             double [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
             double vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
             double [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
             double vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
             double [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
             double vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
             double [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
             double v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                 double v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
             double [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
             double v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                 double v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
             double [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
             double v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                 double v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
             double [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
             double [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
              double [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                     double vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
              double [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                     double vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
             double [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                 double vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap( double[] vals, int [] order, int k1, int k2) {
            { double t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap( double[] vals,int k1,int k2) {
             double t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>  double val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>  double val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
              double vals[] = zz.vals;
             int [] order = order(zz);
              double xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
             double vals[] = zz.vals;
             double xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class   floats {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
              float [] vals, vbuf;
            public Data set(  float[] _vals,  float[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(  float v1,  float v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(  float v1,  float v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
              float [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
              float vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
              float [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
              float vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
              float [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
              float vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
              float [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
              float v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                  float v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
              float [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
              float v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                  float v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
              float [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
              float v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                  float v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
              float [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
              float [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
               float [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                      float vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
               float [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                      float vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
              float [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                  float vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(  float[] vals, int [] order, int k1, int k2) {
            {  float t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(  float[] vals,int k1,int k2) {
              float t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>   float val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>   float val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
               float vals[] = zz.vals;
             int [] order = order(zz);
               float xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
              float vals[] = zz.vals;
              float xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class booleans {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
            boolean [] vals, vbuf;
            public Data set(boolean[] _vals,boolean[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(boolean v1,boolean v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(boolean v1,boolean v2) { return !v1 && v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
            boolean [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
            boolean vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
            boolean [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
            boolean vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
            boolean [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
            boolean vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
            boolean [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
            boolean v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                boolean v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
            boolean [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
            boolean v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                boolean v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
            boolean [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
            boolean v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                boolean v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
            boolean [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
            boolean [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
             boolean [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                    boolean vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
             boolean [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    boolean vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
            boolean [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                boolean vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(boolean[] vals, int [] order, int k1, int k2) {
            {boolean t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(boolean[] vals,int k1,int k2) {
            boolean t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT> boolean val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT> boolean val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
             boolean vals[] = zz.vals;
             int [] order = order(zz);
             boolean xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
            boolean vals[] = zz.vals;
            boolean xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class    bytes {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
               byte [] vals, vbuf;
            public Data set(   byte[] _vals,   byte[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(   byte v1,   byte v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(   byte v1,   byte v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
               byte [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
               byte vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
               byte [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
               byte vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
               byte [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
               byte vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
               byte [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
               byte v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                   byte v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
               byte [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
               byte v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                   byte v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
               byte [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
               byte v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                   byte v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
               byte [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
               byte [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
                byte [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                       byte vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
                byte [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                       byte vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
               byte [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                   byte vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(   byte[] vals, int [] order, int k1, int k2) {
            {   byte t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(   byte[] vals,int k1,int k2) {
               byte t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>    byte val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>    byte val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
                byte vals[] = zz.vals;
             int [] order = order(zz);
                byte xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
               byte vals[] = zz.vals;
               byte xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class    chars {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
               char [] vals, vbuf;
            public Data set(   char[] _vals,   char[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(   char v1,   char v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(   char v1,   char v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
               char [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
               char vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
               char [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
               char vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
               char [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
               char vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
               char [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
               char v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                   char v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
               char [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
               char v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                   char v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
               char [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
               char v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                   char v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
               char [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
               char [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
                char [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                       char vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
                char [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                       char vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
               char [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                   char vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(   char[] vals, int [] order, int k1, int k2) {
            {   char t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(   char[] vals,int k1,int k2) {
               char t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>    char val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>    char val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
                char vals[] = zz.vals;
             int [] order = order(zz);
                char xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
               char vals[] = zz.vals;
               char xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class   shorts {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
              short [] vals, vbuf;
            public Data set(  short[] _vals,  short[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(  short v1,  short v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(  short v1,  short v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
              short [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
              short vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
              short [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
              short vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
              short [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
              short vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
              short [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
              short v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                  short v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
              short [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
              short v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                  short v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
              short [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
              short v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                  short v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
              short [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
              short [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
               short [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                      short vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
               short [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                      short vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
              short [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                  short vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(  short[] vals, int [] order, int k1, int k2) {
            {  short t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(  short[] vals,int k1,int k2) {
              short t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>   short val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>   short val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
               short vals[] = zz.vals;
             int [] order = order(zz);
               short xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
              short vals[] = zz.vals;
              short xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class    longs {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
               long [] vals, vbuf;
            public Data set(   long[] _vals,   long[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(   long v1,   long v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(   long v1,   long v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
               long [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
               long vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
               long [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
               long vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
               long [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
               long vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
               long [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
               long v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                   long v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
               long [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
               long v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                   long v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
               long [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
               long v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                   long v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
               long [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
               long [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
                long [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                       long vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
                long [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                       long vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
               long [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                   long vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(   long[] vals, int [] order, int k1, int k2) {
            {   long t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(   long[] vals,int k1,int k2) {
               long t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>    long val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>    long val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
                long vals[] = zz.vals;
             int [] order = order(zz);
                long xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
               long vals[] = zz.vals;
               long xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class     ints {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
                int [] vals, vbuf;
            public Data set(    int[] _vals,    int[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT> boolean eqv(    int v1,    int v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv(    int v1,    int v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
                int [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
                int vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
                int [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
                int vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
                int [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
                int vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Index which,Part part) {
                int [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
                int v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                    int v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,Yes which,Part part) {
                int [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
                int v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                    int v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT> void qrotate(Data<TT> zz,No which,Part part) {
                int [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
                int v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                    int v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
                int [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
                int [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
                 int [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                        int vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
                 int [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                        int vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
                int [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                    int vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT> void swap(    int[] vals, int [] order, int k1, int k2) {
            {    int t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT> void swap(    int[] vals,int k1,int k2) {
                int t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT>     int val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT>     int val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, Index which,int a, int b, int c) {
                 int vals[] = zz.vals;
             int [] order = order(zz);
                 int xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT> int med3(Data<TT> zz, Object which,int a, int b, int c) {
                int vals[] = zz.vals;
                int xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
    /* XXX - start copy on this line */






































    /* an agregate of static methods for sorting */
    public static class  Objects {



        /** data needed to represent a sort, ie the array data and any buffers */
        public static class Data<TT> extends DataRoot {
                 TT [] vals, vbuf;
            public Data set(     TT[] _vals,     TT[] _vbuf) {
                vals = _vals;
                vbuf = _vbuf;
                return this;
            }
        }
        /** convenience for static delegates */
        public static Data newData() { return new Data(); }

        /** quicksort the region [k1,k2) */
        public static void quicksort(Data zz,int k1,int k2) {
            int nn = k2-k1;
            if ( k1 >= k2 )            return;
            if ( nn < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, which(), k1, k2 ); return; }
            Part part = qpartition( zz, k1, k2 );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }
        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static Part qpartition(Data zz,int k1,int k2) {
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            swap( zz, kpivot, k2-1 );
            qrotate( zz, which(), part );
            qrelocate( zz, which(), part.k1, part.k2, k2 );
            part.set( part.k1, part.k1+k2-part.k2 );
            return part;
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, which(), ii, Math.min(ii+block, r2) );
            if (nblocks > 1) mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        public static void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, which(), qo, q2, r2 );
                flip( zz, which(), qo, r2-qo );
            }
        }


        /* ----------------------------- private stuff below here --------------------------------------- */


        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        private static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** choose a value to use as a pivot in the region [k1,k2) */
        private static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, which(), rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, which(), k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** is the vals[k1] &lt vals[k2] */
        private static <TT extends Comparable> boolean lt(Data<TT> zz, int k1, int k2) {
            return ltv( val( zz, which(), k1 ), val( zz, which(), k2 ) );
        }
        /** v1==v2 */      static <TT extends Comparable> boolean eqv(     TT v1,     TT v2) { return v1.compareTo(v2)==0; }
        /** v1 &lt v2 */   static <TT extends Comparable> boolean ltv(     TT v1,     TT v2) { return v1.compareTo(v2) < 0; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT extends Comparable> void mergeBuffer(Data<TT> zz,Index which,int qo,int q2,int r2) {
                 TT [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
                 TT vq = vals[order[qq]], vr = vals[order[rr]];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { obuf[ii]=order[qq]; kv=qq++;            vq = vals[order[qq]]; }
                else              { obuf[ii]=order[rr]; kv=rr++; if (rr<r2) vr = vals[order[rr]]; }
                ii++;
            }
            while (qq < q2) { obuf[ii++] = order[qq++]; }
            while (rr < r2) { obuf[ii++] = order[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT extends Comparable> void mergeBuffer(Data<TT> zz,Yes which,int qo,int q2,int r2) {
                 TT [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = order(zz), obuf = obuf(zz);
            int qq = qo, rr = q2, ii = 0, kv;
                 TT vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                obuf[ii] = order[kv];
                ii++;
            }
            while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
        }
        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        private static <TT extends Comparable> void mergeBuffer(Data<TT> zz,No which,int qo,int q2,int r2) {
                 TT [] vals = zz.vals, vbuf = zz.vbuf;
            int qq = qo, rr = q2, ii = 0, kv;
                 TT vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                ii++;
            }
            while (qq < q2) { vbuf[ii++] = vals[qq++]; }
            while (rr < r2) { vbuf[ii++] = vals[rr++]; }
        }

        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT extends Comparable> void qrotate(Data<TT> zz,Index which,Part part) {
                 TT [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
                 TT v2 = vals[order[k2]];
            for (int ii = k1; ii < k2; ii++) {
                     TT v1 = vals[order[ii]];
                if      ( ltv(v1,v2) ) swapi( order, ii, k1++ );
                else if ( eqv(v1,v2) ) swapi( order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1]
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT extends Comparable> void qrotate(Data<TT> zz,Yes which,Part part) {
                 TT [] vals = zz.vals;
            int [] order = order(zz);
            int k1 = part.k1, k2 = part.k2-1;
                 TT v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                     TT v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        private static <TT extends Comparable> void qrotate(Data<TT> zz,No which,Part part) {
                 TT [] vals = zz.vals;
            int k1 = part.k1, k2 = part.k2-1;
                 TT v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                     TT v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT extends Comparable> void qrelocate(Data<TT> zz,Index which,int ro,int qo,int q2) {
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swapi( order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT extends Comparable> void qrelocate(Data<TT> zz,Yes which,int ro,int qo,int q2) {
                 TT [] vals = zz.vals;
            int [] order = order(zz);
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        private static <TT extends Comparable> void qrelocate(Data<TT> zz,No which,int ro,int qo,int q2) {
                 TT [] vals = zz.vals;
            for (int ii = qo; ii < q2; ii++) swap( vals,        ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT extends Comparable> void insertionsort(Data<TT> zz,Index which,int qo, int q2) {
                  TT [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                    int oo = order[jj];
                         TT vo = vals[oo];
                    while (--ii >= qo && ltv(vo,vals[order[ii]]) ) { order[ii+1]=order[ii]; }
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT extends Comparable> void insertionsort(Data<TT> zz,Yes which,int qo, int q2) {
                  TT [] vals = zz.vals;
             int [] order = order(zz);
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                         TT vo = vals[jj];
                    int oo = order[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) { vals[ii+1]=vals[ii]; order[ii+1]=order[ii]; }
                    vals[ii+1] = vo;
                    order[ii + 1] = oo;
                }
        }
        /** use insertion sort for small regions [qo,q2) */
        private static <TT extends Comparable> void insertionsort(Data<TT> zz,No which,int qo, int q2) {
                 TT [] vals = zz.vals;
            for (int jj = qo + 1; jj < q2; ++jj) {
                int ii = jj;
                     TT vo = vals[jj];
                while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                vals[ii+1] = vo;
            }
        }
        /** swap the elements at k1 and k2 */
        private static <TT extends Comparable> void swap(Data<TT> zz, int k1, int k2) { swap( zz, which(), k1, k2 ); }
        private static <TT extends Comparable> void swap(Data<TT> zz,Index which,int k1,int k2) { swapi(       order(zz), k1, k2 ); }
        private static <TT extends Comparable> void swap(Data<TT> zz,Yes which,int k1,int k2) { swap( zz.vals, order(zz), k1, k2 ); }
        private static <TT extends Comparable> void swap(Data<TT> zz,No  which,int k1,int k2) { swap( zz.vals,            k1, k2 ); }
        /** swap the elements (both order and value) at k1 and k2, direct access */
        private static <TT extends Comparable> void swap(     TT[] vals, int [] order, int k1, int k2) {
            {     TT t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
            { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** swap value elements */
        private static <TT extends Comparable> void swap(     TT[] vals,int k1,int k2) {
                 TT t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2;
        }
        /** swap value elements, for indirect access */
        private static <TT extends Comparable> void swapi(int [] order,int k1,int k2) {
            int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2;
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Index which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,Yes which,int qo,int nv) {
            Util.dup( obuf(zz), 0, nv, order(zz), qo );
            Util.dup( zz.vbuf, 0, nv, zz.vals, qo );
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        private static void flip(Data zz,No which,int qo, int nv) { Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        private static <TT extends Comparable>      TT val(Data<TT> zz,Index  which,int kk) { return zz.vals[order(zz)[kk]]; }
        /** get the value at kk */
        private static <TT extends Comparable>      TT val(Data<TT> zz,Object which,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT extends Comparable> int med3(Data<TT> zz, Index which,int a, int b, int c) {
                  TT vals[] = zz.vals;
             int [] order = order(zz);
                  TT xa=vals[order[a]], xb=vals[order[b]], xc=vals[order[c]];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }
        private static <TT extends Comparable> int med3(Data<TT> zz, Object which,int a, int b, int c) {
                 TT vals[] = zz.vals;
                 TT xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }


































    /* XXX - end copy on this line */
}
