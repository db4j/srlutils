// copyright nqzero 2017 - see License.txt for terms





















    /* an agregate of static methods for sorting */
    public static class  doubles {
        private static final Config cfg = new Config( 30, false, null );


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
            if ( nn < cfg.limit ) { insertionsort( zz, k1, k2 ); return; }
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            partition( zz, kpivot, part );
            quicksort( zz,      k1,   part.k1 );
            quicksort( zz, part.k2,        k2 );
        }
        /** choose a value to use as a pivot */
        public static int choose(Data zz, int k1, int k2) {
            return cfg.robust
                    ? med3( zz, rand(zz,k1,k2), rand(zz,k1,k2), rand(zz,k1,k2) )
                    : med3( zz, k1,               (k1+k2-1)/2,        k2-1 );
        }
        /** return a random value in [k1,k2) */
        protected static int rand(Data zz,int k1,int k2) { return k1+cfg.rand.irandv( k2-k1 ); }
        /** quick select the kth element over the region [k1,k2) */
        public static void quickselect(Data zz,int k1,int k2,int kth) {
            if (k1 >= k2)              return;
            if ( k2-k1 < cfg.limit ) { insertionsort( zz, k1, k2 ); return; }
            int kpivot = choose( zz, k1, k2 );
            Part part = new Part(k1,k2);
            partition( zz, kpivot, part );
            int p1 = part.k1, p2 = part.k2;
            if      (kth<p1)  quickselect( zz, k1,   p1, kth );
            else if (kth>p2)  quickselect( zz, p2, k2,   kth );
        }

        /** partition the region part.[k1,k2) around the value at kp
         * when complete, vals[...,k1) &lt val[kp]==vals[k1,k2) &lt vals[k2,...) */
        public static void partition(Data zz,int kp,Part part) {
            int end = part.k2;
            swap( zz, kp, end-1 );
            rotate( zz, part );
            relocate( zz, part.k1, part.k2, end );
            part.set( part.k1, part.k1+end-part.k2 );
        }
        /** sort the region [ qo, r2 ] */
        public static void mergesort(Data zz,int qo, int r2) {
            int nblocks = 1;
            int nn = r2-qo;
            while (nblocks * cfg.limit < nn) nblocks *= 2;
            int block = Util.divup( nn, nblocks );
            for (int ii = qo; ii < r2; ii+=block) insertionsort( zz, ii, Math.min(ii+block, r2) );
            mergeRecursive( zz, qo, r2, block*nblocks/2, block );
        }
        /** recursively merge the region [qo,r2], starting with a length block, limited to minBlock */
        public static void mergeRecursive(Data zz,int qo,int r2,int block,int minBlock) {
            int r1 = qo + block, q2 = r1;
            if (block > minBlock) {
                if (r1>=r2) q2 = r2;
                else mergeRecursive( zz, r1, r2, block/2, minBlock );
                     mergeRecursive( zz, qo, q2, block/2, minBlock );
            }
            if (r1 < r2) merge( zz, qo, q2, r2 );
        }
        /** merge the regions [qo,q2) x [q2,r2) if they're not already sorted */
        static protected void merge(Data zz,int qo,int q2,int r2) {
            if ( lt(zz,q2,q2-1) ) {
                mergeBuffer( zz, qo, q2, r2 );
                flip( zz, qo, r2-qo );
            }
        }
        /** is the vals[k1] &lt vals[k2] */
        public static <TT> boolean lt(Data<TT> zz,int k1,int k2) { return ltv( val(zz,k1), val(zz,k2) ); }
        /** v1==v2 */      static <TT> boolean eqv( double v1, double v2) { return v1 == v2; }
        /** v1 &lt v2 */   static <TT> boolean ltv( double v1, double v2) { return v1 < v2; }




        // ---------------- all methods that access storage directly should be below this line ------------------





        /** merge the regions [qo,q2) x [q2,r2) of vals into the buffer region [0,...) */
        public static <TT> void mergeBuffer(Data<TT> zz,int qo,int q2,int r2) {
             double [] vals = zz.vals, vbuf = zz.vbuf;
             int [] order = zz.order, obuf = zz.obuf;
            int qq = qo, rr = q2, ii = 0, kv;
            boolean yes = (order != null);
            double vq = vals[qq], vr = vals[rr];
            for ( ; qq < q2 && rr < r2; ) {
                /* gaurd against overrunning r2 */
                if ( ltv(vq,vr) ) { vbuf[ii] = vq; kv=qq++;            vq = vals[qq]; }
                else              { vbuf[ii] = vr; kv=rr++; if (rr<r2) vr = vals[rr]; }

                if (yes) obuf[ii] = order[kv];
                ii++;
            }
            if (yes) {
                while (qq < q2) { obuf[ii] = order[qq]; vbuf[ii++] = vals[qq++]; }
                while (rr < r2) { obuf[ii] = order[rr]; vbuf[ii++] = vals[rr++]; }
            } else {
                while (qq < q2) {                       vbuf[ii++] = vals[qq++]; }
                while (rr < r2) {                       vbuf[ii++] = vals[rr++]; }
            }
        }
        /** reorder the region part.[k1,k2] about v2=v[k2-1] 
         * and adjust part st v[...,k1) &lt v[k2,...] &lt v[k1,k2)==v2 */
        public static <TT> void rotate(Data<TT> zz,Part part) {
             double [] vals = zz.vals;
             int [] order = zz.order;
            int k1 = part.k1, k2 = part.k2-1;
             double v2 = vals[k2];
            for (int ii = k1; ii < k2; ii++) {
                 double v1 = vals[ii];
                if      ( ltv(v1,v2) ) swap( vals, order, ii, k1++ );
                else if ( eqv(v1,v2) ) swap( vals, order, ii--, --k2 );
            }
            part.set(k1,k2);
        }
        /** move the block of values [qo,q2) to the region [ro,...) */
        public static <TT> void relocate(Data<TT> zz,int ro,int qo,int q2) {
              double [] vals = zz.vals;
             int [] order = zz.order;
            for (int ii = qo; ii < q2; ii++) swap( vals, order, ro++, ii );
        }
        /** use insertion sort for small regions [qo,q2) */
        protected static <TT> void insertionsort(Data<TT> zz,int qo, int q2) {
             double [] vals = zz.vals;
             int [] order = zz.order;
             boolean yes = (order != null);
             if (yes)
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                     double vo = vals[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                    vals[ii+1] = vo;
                    int oo = order[jj];
                    for (int kk = jj - 1; kk > ii; kk--) order[kk + 1] = order[kk];
                    order[ii + 1] = oo;
                }
             else
                for (int jj = qo + 1; jj < q2; ++jj) {
                    int ii = jj;
                     double vo = vals[jj];
                    while (--ii >= qo && ltv(vo,vals[ii]) ) vals[ii+1] = vals[ii];
                    vals[ii+1] = vo;
                }
        }
        /** swap the elements at k1 and k2 */
        public static <TT> void swap(Data<TT> zz,int k1,int k2) { swap( zz.vals, zz.order, k1, k2 ); }
        /** swap the elements at k1 and k2, direct access */
        public static <TT> void swap( double[] vals, int [] order, int k1, int k2) {
            { double t2 = vals[k1]; vals[k1] = vals[k2]; vals[k2] = t2; }
//            if (order != null) { int t2 = order[k1]; order[k1] = order[k2]; order[k2] = t2; }
        }
        /** copy the buffer range [0,nv) to vals [qo,...) */
        public static void flip(Data zz,int qo, int nv) {      Util.dup( zz.vbuf, 0, nv, zz.vals, qo ); }
        /** get the value at kk */
        static <TT>  double val(Data<TT> zz,int kk) { return zz.vals[kk]; }
        /** return the median of the values at a, b and c */
        private static <TT> int med3(Data<TT> zz, int a, int b, int c) {
             double vals[] = zz.vals;
             double xa=vals[a], xb=vals[b], xc=vals[c];
            return ltv(xa,xb)
                    ? (ltv(xb,xc) ? b : ltv(xa,xc) ? c : a)
                    : (ltv(xc,xb) ? b : ltv(xc,xa) ? c : a);
        }


    }

