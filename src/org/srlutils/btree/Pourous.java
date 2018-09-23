// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.util.Iterator;
import org.srlutils.Rand;
import org.srlutils.Simple;
import static org.srlutils.Simple.Exceptions.rte;
import org.srlutils.TaskTimer;
import static org.srlutils.Unsafe.uu;
import org.srlutils.Util;


/*
 * a pourous array
 * stores elements in sorted order
 * the array is broken down into bins with power of 2 size (final bin can be partial)
 * the number of elements in each bin is recorded
 * the number of bins in use is recorded (as next)
 * because bins aren't necessarily full
 *   the position in the array isn't the same as the index
 * insertion is pretty quick
 *   if the bin isn't full just shift that bin
 *   else if there's an empty bin avail, split the bin
 *     optimized for append ... if it's the last used bin, don't split
 *   else borrow from the nearest bin with space avail
 * find by key is pretty quick
 *   iterate thru the bins, then iterate thru the items in the bin
 *   faster than binary search due to being cache-friendly
 * lookup by index is slower than for an array, but still pretty quick
 *   not yet implemented
 *   iterate thru the bins, decrementing the index by bin size --> bin + remainder --> position
 * remove is similar to insertion
 *   implemented and runs, but not tested extensively
 *   could be outstanding corner cases
 * iteration is cheap
 * 
 * probably most similar to a btree
 *   suboptimal in all major characteristics ... insert, remove, lookup, find, iterate
 * 
 * have tried a non-linear ordering (tag: pourous.nonlinear)
 *   place the bin leaders (ie first element of each bin) at the beginning of the array
 *   then have the remaining elements in order
 *   allows for quick scan of the bin leaders (cache friendly)
 *   but overall much slower (2s vs 0.5s) ... see performance notes at the bottom
 * 
 * 
 */

public class Pourous {
    
    // standardizing on SubWorker
    //  array backed worker actually ran slower in recent tests
    //    never ran significantly faster, and isn't viable for general usage
    //  direct worker was redundant, same as SubWorker but not as general

    public static abstract class SubWorker extends Worker {
        int next = 0;
        int [] no;
        int esize = 8;
        long vo;
        protected int next() { return next; }
        protected void next(int val) { next = val; }
        protected int no(int i2) { return no[i2]; }
        protected void no(int i2,int val) { no[i2] = val; }
        protected void copy(int k1,int k2,int len) {
            uu.copyMemory( vo+(k1*esize), vo+(k2*esize), len*esize );
        }
        public void init() {
            vo = uu.allocateMemory( cap*esize );
            no = new int[nc];
        }
        /** verify that vo[ko] gte vo[kp], ie throws rte */
        public void check(int ko,int kp) {
            double v1 = uu.getDouble(vo+kp*esize), v2 = uu.getDouble(vo+ko*esize);
            if (v2 < v1) throw rte(null);
        }
        public void clear() {
            for (int ii = 0; ii < nc; ii++) no[ii] = 0;
            next = 0;
        }
        public void clean() {
            uu.freeMemory(vo);
        }
        /** 
         * a version of split that spreads out the moved bins
         *   not any faster than standard split
         */
        public void split2(SubWorker w2) {
            long v2 = w2.vo;
            int z1 = nc/2;
            int k1 = z1*step, n2 = cap-k1;
            int nmove = cap-k1;
            double pmove = 1.0*nmove/cap;
            int z2 = 0;
            for (; z2 < nc & n2 > 0; z2++) {
                int k2 = z2<<bits;
                int c2 = (int) Math.ceil( cap(z2)*pmove );
                int len = Math.min( c2, n2 );
                uu.copyMemory( vo+k1*esize, v2+k2*esize, len*esize );
                w2.no(z2,len);
                n2 -= len;
                k1 += len;
            }
            w2.next(z2);
            next(z1);
            for (int ii = z1; ii < nc; ii++) no(ii,0);
        }
        /** split off half-ish of this array (which is required to be at cap) into w2 */
        public void split(SubWorker w2) {
            long v2 = w2.vo;
            int n2 = nc/2;
            uu.copyMemory( vo+n2*step*esize, v2, (cap-n2*step)*esize );
            for (int ii=n2, jj=0; ii < nc; ii++, jj++) {
                w2.no[jj] = no[ii];
                no[ii] = 0;
            }
            next = n2;
            w2.next = nc-n2;
        }
        public int num() {
            int sum = 0;
            for (int ii = 0; ii < next; ii++) sum += no[ii];
            return sum;
        }
        public String info() {
            String txt = "no: [ ";
            for (int ii = 0; ii < next; ii++) txt += no[ii] + ",";
            txt += " ]";
            return txt;
        }
        
        public static class DF extends SubWorker {
            double key;
            float val;
            { esize = 12; }
            protected boolean gt(int k1) { return uu.getDouble(vo+(k1*esize)) > key; }
            protected boolean gte(int k1) { return uu.getDouble(vo+(k1*esize)) >= key; }
            protected boolean eq(int k1) { return uu.getDouble(vo+(k1*esize))==key; }
            protected void setcc(int k1) {
                long index = vo+(k1*esize);
                uu.putDouble(index,key);
                uu.putFloat(index+8,val);
            }
            protected void getcc(int k1) {
                long index = vo+(k1*esize);
                key = uu.getDouble(index);
                val = uu.getFloat(index+8);
            }
            protected double get(int k1) { return uu.getDouble(vo+k1*esize); }
            protected void   set(int k1,double key1) { uu.putDouble(vo+k1*esize,key1); }
            public DF set(double $key) { key = $key; return this; }
            public DF() {}
            public DF(int $cap,int $bits) {
                init($cap,$bits);
            }
        }
        public static class D extends SubWorker {
            double key;
            { esize = 8; }
            protected boolean gt(int k1) { return uu.getDouble(vo+(k1*esize)) > key; }
            protected boolean gte(int k1) { return uu.getDouble(vo+(k1*esize)) >= key; }
            protected boolean eq(int k1) { return uu.getDouble(vo+(k1*esize))==key; }
            protected void setcc(int k1) {
                long index = vo+(k1*esize);
                uu.putDouble(index,key);
            }
            protected void getcc(int k1) {
                long index = vo+(k1*esize);
                key = uu.getDouble(index);
            }
            protected double get(int k1) { return uu.getDouble(vo+k1*esize); }
            protected void   set(int k1,double key1) { uu.putDouble(vo+k1*esize,key1); }
            public D set(double $key) { key = $key; return this; }
        }
        public static class L extends SubWorker {
            long key;
            { esize = 8; }
            protected boolean gt(int k1) { return uu.getLong(vo+(k1*esize)) > key; }
            protected boolean gte(int k1) { return uu.getLong(vo+(k1*esize)) >= key; }
            protected boolean eq(int k1) { return uu.getLong(vo+(k1*esize))==key; }
            protected void setcc(int k1) {
                long index = vo+(k1*esize);
                uu.putLong(index,key);
            }
            protected void getcc(int k1) {
                long index = vo+(k1*esize);
                key = uu.getLong(index);
            }
            protected double get(int k1) { return uu.getDouble(vo+k1*esize); }
            protected void   set(int k1,double key1) { uu.putDouble(vo+k1*esize,key1); }
            public L set(long $key) { key = $key; return this; }
        }
    }
    
    public static abstract class Worker {
        int bits = 7;
        int step;
        /** the number of bins, nc:total, nf:full only, ie the final bin might be partial */
        int nc, nf, cap;
        /** skip for the sub-bin scan - probably a cache-lines worth is best */
        final int skip = 8;
        /** skip for the bin scan - not sure of the best value */
        final int skip2 = 8;
        int last;
        protected abstract int next();
        protected abstract void next(int val);
        /** is the key vo[k1] gt the current key */
        protected abstract boolean gt(int k1);
        /** is the key vo[k1] gte the current key */
        protected abstract boolean gte(int k1);
        protected abstract int no(int i2);
        protected abstract void no(int i2, int val);
        public void init() {}
        protected abstract void setcc(int k1);
        public abstract void check(int ko,int kp);
        protected abstract void copy(int k1,int k2,int len);
        public void clean() {}
        protected abstract boolean eq(int k1);
        
        
        public Worker init(int $cap,int $bits) {
            cap = $cap;
            bits = $bits;
            nf = nc = cap >> bits;
            step = 1<<bits;
            last = cap - (nc<<bits);
            if (last > 0) nc++;
            else last = step;
            init();
            return this;
        }
        /** 
         * return the first index greater than key, ie where to append
         * corner case:
         *   there's some ambiguity at the end of a partially filled bin
         *   ie it's equivalent to the the start of the next bin
         *   clarification: return the end of the bin in this case
         */
        public int findNext(final boolean abbrev) {
            int i2 = 0, i3 = 1;
            int next = next();
            // fixme::generality -- could make this a loop, with decreasing skip ...
            for (int ii = skip2; ii < next-1; i3 = ii, ii+=skip2)
                if (gt(ii<<bits)) break;
            for (int ii = i3; ii < next; i2 = ii, ii++)
                if (gt(ii<<bits) || abbrev && ii==next-1 && no(ii)==1) break;
            int no = no(i2);
            int k1 = i2<<bits;
            int kend = k1+no, k2 = kend;
            if (abbrev && i2+1==next) k2--;
            for (int ii = k1; ii < k2 && !gt(ii); ii += skip) k1 = ii;
            for (; k1 < k2; k1++)
                if (gt(k1)) break;
            return k1;
        }
        /**
         * return the first position greater than or equal to key
         * always returns a real key position (as opposed to an insertion position)
         * cannot be used for insertion directly (might violate bin monotonicity)
         * if there is no element gte, return cap
         * 
         * abbrev: last element doesn't have a key, assume infinity (for btree)
         */
        public int find(final boolean abbrev) {
            int i2 = 0, i3 = 1;
            if (false) {
                // fast path for when the array is totally full
                //   runs at same speed as Sorted
                for (int ii = i3; ii < nc; i2 = ii, ii++)
                    if (gte(ii<<bits)) break;
                int k1 = i2<<bits, k2 = k1+step;
                for (int ii = k1; ii < k2 && !gte(ii); ii += skip) k1 = ii;
                while (k1 < k2 && !gte(k1)) k1++;
                return k1;
            }
            int next = next();
            // bins can have zero elements (ZE) if a key has been inserted and removed
            // we read the old key and treat it as though it's valid, ie a fence
            for (int ii = skip2; ii < next-1; i3 = ii, ii+=skip2)
                if (gte(ii<<bits)) break;
            for (int ii = i3; ii < next; i2 = ii, ii++)
                if (gte(ii<<bits) || abbrev && ii==next-1 && no(ii)==1) break;
            int no = no(i2);
            int k1 = i2<<bits;
            int kend = k1+no, k2 = kend;
            if (abbrev && i2+1==next) k2--;
            for (int ii = k1; ii < k2 && !gte(ii); ii += skip) k1 = ii;
            while (k1 < k2 && !gte(k1)) k1++;

            if (k1==kend) return nextIndex(k1);
            return k1;
        }
        /** check if key is in the map, returning it's position, else -1 */
        public int match() {
            int k1 = find(false);
            if (k1 < cap && eq(k1)) return k1;
            return -1;
        }
        /** find the first position at or before k1 that is a real position, -1 for no such position */
        public int prev(int k1) {
            int zo = k1>>bits;
            int no = 0;
            while (zo >= 0 && (no= no(zo))==0) zo--;
            if (zo < 0) return -1;
            int ko = zo<<bits;
            return (k1 < ko+no) ? k1:ko+no-1;
        }
        /** find the last position equal to key, else the first position greater */
        public int findLast(boolean abbrev) {
            // find the first greater, then check if prev is equal
            int kg = findNext(false);
            int kp = prev(kg-1);
            if (kp < 0) return kg;
            return (kp >= 0 && eq(kp)) ? kp : -kg-1;
        }
        /**
         * remove the key from the map, returning true if the key is found
         * note: lightly tested, could be (significant) corner cases that aren't working
         */
        public boolean remove() {
            int k1 = match();
            if (k1 < 0) return false;
            int zo = k1 >> bits;
            int no = no(zo) - 1;
            int ko = zo<<bits;
            copy( k1+1, k1, ko+no-k1 );
            no(zo,no);
            // we leave zero-element artifacts behind, but the prev value serves as a
            //   valid value and find*() works as if it's a valid value
            if (no==0) {
                int next = next();
                if (zo+1==next) {
                    while (zo > 0 && no(zo)==0) zo--;
                    next( zo+1 );
                }
            }
            return true;
        }
        

        /** insert the key into the map */
        public void insert() {
            int k1 = findNext(false);
            k1 = distribute(k1);
            setcc(k1);
        }
        /** return the capacity of bin ksub */
        int cap(int ksub) { return ksub==nf ? last:step; }
        /** return the space available in bin ksub */
        int space(int ksub) { return (ksub==nf ? last : step) - no(ksub); }
        /** 
         * move nw spaces from right (bin zr) to left (bin zo)
         *   if nw is zero, move half plus four of the available space
         * requirements: zo, and everything between zo and zr, must be full
         */
        void moveLeft(int zr,int zo,int nw) {
            // ny stayes in the source (zr), nw spaces move to the new bin (zo)
            int cz = no(zr);
            int space = cap(zr)-cz;
            if (nw==0)
                nw = Math.min( space, space/2 + 4 );
            // [k1,k2) is the portion of zo that gets right-shifted, making space
            int k2 = (zo+1)*step;
            int k1 = k2-nw;
            int k3 = zr*step + cz;
            copy( k1, k2, k3-k1 );
            no(zo,step-nw);
            no(zr,cz+nw);
            if (zr==nf) next(nc);
        }
        /** 
         * move space from the right (bin zr) to the left (bin zo), ko as insertion point
         * return the new insertion point
         */
        int distLeft(int zr,int zo,int ko) {
            int delta = zr-zo;
            int z1 = zo+1;
            if (delta > 1) moveLeft(zr,z1,0);
            int cz = no(z1);
            int space = cap(z1)-cz;
            int k2 = z1*step;
            if (ko+space >= k2) space = k2-ko;
            moveLeft(z1,zo,space);
            no( zo, step-space+1 );
            if (ko+space < k2)
                copy( ko, ko+1, k2-space-ko );
            return ko;
        }
        /*
         * a simpler way to move space to the left
         *   a little slower for large caps
         *   could probably use either method ...
         */
        int distLeft2(int zr,int zo,int ko) {
            int cz = no(zr);
            int k2 = (zo+1)*step;
            int nw = cap(zr)-cz;
            nw = Math.min(nw,nw/2+4);
            if (ko+nw >= k2) nw = k2-ko;
            moveLeft(zr,zo,nw);
            no( zo, step-nw+1 );
            if (ko+nw < k2)
                copy( ko, ko+1, k2-nw-ko );
            return ko;
         }
        
        /** 
         * move nw space from left to right, from bin zr to bin zo 
         *   if nw is zero, move half plus four of the available space
         * requirements: zo, and everything between zo and zr, must be full
         */
        void moveRight(int zr,int zo,int nw) {
            int cz = no(zr);
            int space = cap(zr)-cz;
            if (nw==0)
                nw = Math.min( space, space/2 + 4 );
            int k1 = zr*step + cz;
            int k2 = (zr+1)*step;
            int k3 = k2 + nw;
            int k4 = (zo+1)*step;
            copy( k2, k1, nw );
            copy( k3, k2, k4-k3 );
            no(zo,step-nw);
            no(zr,cz+nw);
        }
        /** 
         * move space from left to right, from bin zr to bin zo, ko as insertion point
         * return the new insertion point
         */
        int distRight(int zr,int zo,int ko) {
            int delta = zo-zr;
            int z1 = zo-1;
            if (delta > 1) moveRight(zr,z1,0);
            int cz = no(z1);
            int space = step-cz;
            int k2 = zo*step;
            no(z1,cz+1);
            if (ko==k2)
                // it's the first element in the bin, so just insert into the previous bin
                // this also catches the insert-at-the-end-of-the-array case
                return ko-space;
            copy( k2, k2-space, 1 );
            copy( k2+1, k2, ko-k2-1 );
            ko--;
            return ko;
        }
        /*
         * a simpler way to move space to the right
         *   slower for large caps (doesn't keep the available space as well distributed)
         * note: unused ... here for proof of concept
         */
        private int distRight2(int zr,int zo,int ko) {
            int kr = zr<<bits, nr = no(zr), ks = kr+step;
            // if it's the first element in the bin, just insert into the previous bin
            if (ko > ks) {
                copy( ks, kr+nr, 1 );
                copy( ks+1, ks, ko-ks );
            }
            no(zr,nr+1);
            return ko-1;
        }

        
        /**
         *  bin borrowing
         *  borrow a position from another bin for insertion point ko
         *  return the new insertion point
         */
        public int nearest(int ko) {
            int k1;
            int zo = ko >> bits;
            int zr = zo+1;
            int distr = 1, distl = 1, spacer = 0, spacel = 0;
            for (; zr < nc; zr++, distr++)
                if ((spacer = space(zr)) > 0) break;
            int zl = zo-1;
            int lend = spacer==0 ? 0 : Math.max( 0, zo-distr );
            for (; zl >= lend; zl--, distl++)
                if ((spacel = space(zl)) > 0) break;
            if (spacel==0 || spacer>0 && distr < distl || (distr==distl && spacer >= spacel)) 
                k1 = distLeft(zr,zo,ko);
            else
                k1 = distRight(zl,zo,ko);
            return k1;
        }
        public int distribute(int k1) {
            int ksub = k1>>bits;
            if (ksub==nc)
                ksub--;
            int no = no(ksub);
            int next = next();
            int step1 = step;
            if (ksub==nf) {
                step1 = last;
                next(nc);
            }
            if (no < step1 && (!usess2 | ksub==0 | ksub < next | ksub >= nf)) {
                int len = (ksub << bits)+no-k1;
                // special case::append -- overflowed into the first empty bin, don't split the last one
                //   for pure append this is optimal, but for occassional randoms we still get gaps
                if (no==0 && ksub==next)
                    next(next+1);
                else
                    copy(k1,k1+1,len);
                no(ksub,no+1);
            }
            else if (next < nf & usess2)
                k1 = ss2(k1);
            else if (next < nf)
                k1 = subsplit(k1);
            else
                k1 = nearest(k1);
            return k1;
        }
        static final boolean usess2 = false;
        
        private int ss2(int k1) {
            int ne = step/nf, ko=0, ii;
            int z1 = k1/ne, nr = k1-z1*ne;
            for (ii = 0; ii < nf-1; ii++, ko += ne) {
                no(ii,ne);
                copy(ko, ii<<bits, ne);
            }
            if (z1==nf) {
                z1--;
                nr = ne;
            }
            int nl = step-ii*ne;
            copy(ko, ii<<bits, nl);
            no(ii,nl);
            next(nf);
            int n1 = no(z1);
            no(z1,n1+1);
            int k2 = (z1<<bits) + nr;
            copy(k2,k2+1,n1-nr);
            return k2;
        }
        /**
         * k1's bin is full, split it (assumes that there are unallocated bins, ie next lt nf
         * could do some really neat things here, eg
         *   - spread everything out, ie use as many bins as possible
         *   - check the statistics on bin-fullness to detect the usage pattern
         *       ie, if there's a bunch of full bins, it's a sign that most inserts are appends
         *       and then spread things optimally for that usage
         */
        public int subsplit(int k1) {
            int ksub = k1>>bits;
            int next = next();
            copy( (ksub+1)*step, (ksub+2)*step, (next-ksub-1)*step );
            for (int ii = next-1; ii > ksub; ii--)
                no( ii+1, no(ii) );
            int num2 = step/2;
            int kbreak = ksub*step+step/2;
            int kbin2 = (ksub+1)*step;
            copy( kbreak, kbin2, num2 );
            no(ksub+1,  num2  );
            no(ksub  ,  num2);
            next(next+1);
            if (k1 >= kbreak) {
                k1 += step/2;
                copy( k1, k1+1, kbin2 + num2 - k1 );
                no(ksub+1,num2+1);
            }
            else {
                copy( k1, k1+1, ksub*step + num2 - k1 );
                no(ksub,num2+1);
            }
            return k1;
        }
        /**
         * check that the array is sorted, and that size is correct
         * throws a runtime exception on failure
         */
        public void check(int size) {
            int nv = 0;
            int kp = 0;
            int ii=0, jj=1;
            int next = next();
            for (; ii < next; ii++, jj=0) {
                int ko = ii*step;
                int ni = no(ii);
                if (ni > cap(ii))
                    throw rte(null,"bin %d is too big: %d v %d. total size: %d",ii,ni,step,size);
                nv += ni;
                for (; jj < ni; kp=ko+jj, jj++)
                    check(ko+jj,kp);
            }
            if (nv != size) 
                throw rte(null,"check.size -- notEqual: %d v %d",nv,size);
                    
        }
        /** return the last valid position in the map */
        public int lastIndex() {
            int zo = next()-1;
            int no = no(zo);
            return (zo << bits) + no-1;
        }
        // fixme::dup -- this is the same as prev()
        /** return the real location at or before k1, negative to indicate no previous */
        public int prevIndex(int k1) {
            int zo = k1 >> bits;
            int ko = zo << bits;
            if (k1 >= ko) return k1;
            int no = 0;
            do zo--;
            while (zo >= 0 && (no=no(zo))==0);
            return (zo<<bits) + no-1;
        }

        /** return the real location at or after k1, nc*step to indicate no next */
        public int nextIndex(int k1) {
            int zo = k1 >> bits;
            int next = next();
            int ko = zo << bits;
            if (zo >= next || k1 < ko+no(zo)) return k1;
            do zo++;
            while (zo < next && no(zo)==0);
            return zo << bits;
        }
        /** return an iterator for the elements in the array */
        public Iter getIter() { return new Iter(); }
        /** an element iterator */
        public class Iter implements Iterator<Integer>, Iterable<Integer> {
            int k1 = 0;
            public Iterator<Integer> iterator() { return this; }
            public boolean hasNext() {
                for (int zo = k1>>bits; zo < nc; zo++, k1 = zo<<bits) {
                    int ko = zo<<bits;
                    int no = no(zo);
                    if (k1 < ko+no) return true;
                }
                return false;
            }
            public Integer next() { return k1++; }
            // not implemeneted, but probably could be
            public void remove() {}
        }
    }
    

    /** 
     * quick performance test for pure inserts, intended for large arrays
     */
    public static class TestWorker extends TaskTimer.Runner<Void> {
        /** the keys to add to the mappings         */  public double [] keys;
        SubWorker.D ww;
        int nn, nc, mult = 1;
        public void alloc() { setup(1, "SubWorker"); }
        public void init() {
            keys = Rand.rand(nn);
            ww = new SubWorker.D();
            ww.init(mult*nn,10);
        }
        public void run(int stage) throws Exception {
            for (int ii = 0; ii < nn; ii++) {
                ww.set(keys[ii]);
                ww.insert();
            }
        }
        public boolean finish() throws Exception {
            ww.check(nn);
            ww.clean();
            return true;
        }
    }
    static boolean presort = false;

    public static void main(String[] args) throws Exception {
        int nn = (1<<18) + 100;
        Long seed = null;
        Rand.source.setSeed( seed, true );
        if (true) {
            Simple.Scripts.cpufreqStash( 2300000 );
            TaskTimer tt = new TaskTimer().init( 4, 2, true, true );
            tt.widths( 6, 3 );
            TestWorker to = new TestWorker();
            to.nn = nn;
            int ns = 1<<20, cs = 1<<9;
            TestSorted ts = new TestSorted().pre(ns,cs);
            TestPour tp = new TestPour().pre(ns,cs);
            tt.autoTimer(tp,ts);
            return;
        }
        nn = (1<<9);
        double [] keys = Rand.rand(nn);
        SubWorker.DF ww = new SubWorker.DF();
        ww.init(nn,5);
   //        nn /= 2;
        boolean dbg = true;
        int no = nn;
        for (int ii = 0; ii < no; ii++) {
            if (ii==28895)
                Simple.nop();
            ww.set(keys[ii]);
            ww.insert();
            if (dbg) ww.check(ii+1);
        }
        for (int ii = no; ii < nn; ii++) {
            ww.set(keys[ii]).insert();
            if (dbg) ww.check(ii+1);
        }
        ww.check(  nn );
        long sum = ww.num();

        for (int ii = 0; ii < nn; ii++) {
            boolean found = ww.set(keys[ii]).remove();
            if (! found)
                Simple.softAssert(false);
            if (dbg) ww.check(nn-ii-1);
        }
        

        System.out.format( "done ... size: %d v %d\n", sum, nn );
        
        
    }
}



/*

* nn = 2^18
* 

* using 2-pass find, with skip=32 and 7 bits, .58 seconds at 2.3 Ghz
*   ie, 2048 bins of 128 doubles
*   first pass of find does 64 compares, second does 32
*   did a quick search and this was the fastest combination
*   but doesn't account for lookup by index which is order of the number of bins


* ts v tp, ie sorted array v pourous array
*   each entry is a double so 2^9 is 4k block size
*   sorted is O(cap), pour O(ln(cap))
*   pour doesn't win till significantly larger than 4k block size
*   note: for simplicity sorted is implemented as double[] instead of direct memory
*     so it's not really an apples to apples, but probably close
* 
* entries  cap          sorted           pour   
*    2^20  2^ 8       0.101748          0.134
*          2^ 9       0.136065          0.15     -- equiv to 4k blocks
*          2^10       0.203329          0.17
*          2^11       0.338655          0.18

* 
* non-linear ordering
*   2^18 elements, 7 bit bins
*   linear: .5-.6 seconds
*   non-linear: 1.5-2.0 seconds
*   copy is *much* slower
*   it's possible that implementing copy in c using jni
*     would run at full speed and this could still be a win
*     but for now the extra calls to copyMemory are a killer
*   it's also possible that bin alignment is a problem
*     ie, the contiguous portions of the bin are now 2^bits - 1 in size
*     so the bins cross cache alignment boundaries
*     
* 
* 
 */

























































/*
    * TestPour and TestSorted implement the same test
    * TestPour is backed by Pourous
    * TestSorted is backed by a simple sorted array
    * they both insert a bunch of keys into the array, filling it np times
    *   each time the array is filled, the 1st element is added to a sum and the array is cleared
    * ie, they test insert performance
    */

/** test insert performance for Pourous */
class TestPour extends TaskTimer.Runner<Void> {
    /** the keys to add to the mappings         */  public double [] keys;
    Pourous.SubWorker.D [] map;
    int nn, mult = 1, cap = 1<<9, np;
    double sum;
    boolean ok;
    public TestPour pre(int $nn,int $cap) {
        nn = $nn;
        cap = $cap;
        np = nn / cap;
        keys = new double[nn];
        map = new Pourous.SubWorker.D[np];
        for (int ii=0; ii < np; ii++) {
            map[ii] = new Pourous.SubWorker.D();
            map[ii].init(mult*cap,6);
        }
        setup( 3, "pour" );
        stageNames = "add cap chk".split(" ");
        return this;
    }
    public void init() {
        Rand.source.rand(keys);
        if (Pourous.presort) java.util.Arrays.sort(keys);
        sum = 0;
    }
    public void run(int stage) throws Exception {
        if (stage < 2) {
            int pos = stage==0 ? 0:cap/2;
            for (int ii = 0; ii < np; ii++) {
                for (int jj = 0, k1 = ii*cap+pos; jj < cap/2; jj++, k1++) {
                    map[ii].set(keys[k1]).insert();
//                    map[ii].check(pos+jj+1);
                }
            }
        }
        else if (stage==2) {
            ok = true;
            double delta = 1.0/cap;
            for (int ii = 0, k1 = 0; ii < np; ii++) {
                for (double key=-delta; key > -1; key -= delta)
                    ok &= map[ii].set(key).match() < 0;
                for (int jj = 0; jj < cap; jj++, k1++) {
                    boolean bad = map[ii].set(keys[k1]).match() < 0;
                    if (bad) {
                        map[ii].set(keys[k1]).match();
                        ok = false;
                    }
                }
            }
        }
    }
    public boolean finish() throws Exception {
        for (int ii=0; ii < np; ii++) {
            sum += map[ii].get(cap/2);
            map[ii].clear();
        }
        System.out.format( "sum: %8.3f ", sum/np);
        return ok;
    }
}
/** test insert performance for a sorted array */
class TestSorted extends TaskTimer.Runner<Void> {
    /** the keys to add to the mappings         */  public double [] keys;
    Sorted.D [] map;
    int nn, mult = 1, cap = 1<<9, np;
    double sum;
    boolean ok;
    public TestSorted pre(int $nn,int $cap) {
        nn = $nn;
        cap = $cap;
        np = nn / cap;
        keys = new double[nn];
        map = new Sorted.D[np];
        for (int ii=0; ii < np; ii++)
            map[ii] = new Sorted.D(cap);
        setup( 3, "flat" );
        stageNames = "add cap chk".split(" ");
        return this;
    }
    public void init() {
        Rand.source.rand(keys);
        if (Pourous.presort) java.util.Arrays.sort(keys);
        np = nn / cap;
        sum = 0;
    }
    public void run(int stage) throws Exception {
        if (stage < 2) {
            int pos = stage==0 ? 0:cap/2;
            for (int ii = 0; ii < np; ii++)
                for (int jj = 0, k1 = ii*cap+pos; jj < cap/2; jj++, k1++)
                    map[ii].insert(keys[k1]);
        }
        else if (stage==2) {
            ok = true;
            double delta = 1.0/cap;
            for (int ii = 0, k1 = 0; ii < np; ii++) {
                for (double key=-delta; key > -1; key -= delta)
                    ok &= map[ii].match(key) < 0;
                for (int jj = 0; jj < cap; jj++, k1++)
                    ok &= map[ii].match(keys[k1]) >= 0;
            }
        }
    }
    public boolean finish() throws Exception {
        for (int ii=0; ii < np; ii++) {
            sum += map[ii].get(cap/2);
            map[ii].clear();
        }
        System.out.format( "sum: %8.3f ", sum/np);
        return true;
    }
}



/**  a simple monolithic array with similar interface as Pourous */
class Sorted {
    public int step = 32, esize = 8;
    public static class D extends Sorted {
        public long keys;
        public int num;
        public D(int cap) {
            keys = uu.allocateMemory( cap*esize );
        }
        public void clear() { num = 0; }
        boolean gt(int k1,double key) { return get(k1) > key; }
        boolean gte(int k1,double key) { return get(k1) >= key; }
        boolean eq(int k1,double key) { return get(k1) == key; }
        public int find(double key) {
            int k1 = 0;
            for (int ii = step; ii < num && !gt(ii,key); ii += step) k1 = ii;
            while (k1 < num && !gt(k1,key)) k1++;
            return k1;
        }
        public int match(double key) {
            int k1 = find(key);
            if (k1 < num && eq(k1,key)) return k1;
            return -1;
        }
        public double get(int index) {
            return uu.getDouble( keys+index*esize );
        }
        public void set(int index,double key) {
            uu.putDouble( keys+index*esize, key );
        }
        public void insert(double key) {
            int k1 = 0;
            for (int ii = 0; ii < num && !gt(ii,key); ii += step) k1 = ii;
            while (k1 < num && !gt(k1,key)) k1++;
            long pos = keys + k1*esize;
            uu.copyMemory( pos, pos+esize, (num-k1)*esize );
            set(k1,key);
            num++;
        }
    }
}


/*
 * pourous v sorted, n:20, cap:9 ... flat.chk is faster (it's simpler)
 *   these are using the same unsafe methods to access memory
 *   close to apples to apples
              |    add           |    cap           |    chk           | totals          
        pour  |  0.062    0.000  |  0.086    0.000  |  0.140    0.000  |  0.288    0.000 
        flat  |  0.064    0.000  |  0.103    0.000  |  0.118    0.000  |  0.286    0.001 
  pre-sorted  |  0.042           |  0.054
  pre-pour    |  0.051           |  0.060

* pre-sorted is insertion of pre-sorted keys into Sorted, ie no memmove needed
*   a rough measure of the time doing find
*   pre-pour is the same for Pourous


*/

