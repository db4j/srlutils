// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.util.Iterator;
import java.util.Map.Entry;
import org.srlutils.Rand;
import org.srlutils.Array;
import org.srlutils.Simple;
import org.srlutils.Timer;
import org.srlutils.Util;
import org.srlutils.sort.templated.Direct;
import static org.srlutils.Simple.Exceptions.rte;
import org.srlutils.btree.Butil.Modes;


// todo:
//   pop() works on adam ... could implement push()
//   page.parent ... allow path-like navigation without the problems of out-of-date paths
//   finish removal of page.leaf
//   cleanup map.findIndex vs page.findIndex
//   merge2 and delPath are very similar, could be combined
//     delPath looks like it won't work with paths on the right side of the tree
//   would like a means of telling if a page has been modified
//     eg, by interleaved removeHinted() and insert()

/**
 * this is an attempt at making random inserts fast
 * it finds the correct page for an insert, but doesn't attempt to keep the leaf elements sorted
 * ie, appends inserts out-of-order at the end of the array
 * 
 * this is a rework of Btree3 with tighter integration between the map and page
 *   the fundamental operations on a Page are a better abstraction
 * 
 * 
 * an in-memory btree backed by arrays
 *   no intra-level page links are maintained, ie only an array of children
 * KK: key type
 * VV: value type
 * CC: compare context type
 * GG: page type
 */
public abstract class Bplus<KK,VV,CC extends Bplus.Context,PP extends Bplus.Page<PP>> {
    public static final Modes modes = new Modes();
    public int cap = 1<<6;
    public int pac = 3*cap/8;
    /** the root page and the first page */
    public PP rootz, adam;
    /** the last man (and page) on earth */
    public PP matheson;
    public int depth;
    /** a page to use as a scratchpad. not thread-safe, but then nothing else is either */
    public PP scratch;
    
    public void init() {
        matheson = adam = rootz = createPage(true);
        scratch = createPage(true);
    }

    /** find the index of the first key in the page >= key */
    public int findIndex(int mode,PP page,CC context) {
        int k1 = 0, num = page.numkeys(), cmp = -1;
        for (k1=0; k1<num; k1++) {
            cmp = compare( page, k1, context );
            if (cmp <= 0) break;
        }
        context.match = cmp==0;
        return k1;
    }
    /** find the index of the first key in the page >= key */
    public int findIndex2(int mode,PP page,CC context) {
        int kk = 0, k1 = 0, k2 = page.numkeys(), cmp = -1;
        boolean greater = modes.greater( mode );
        boolean match = false;
        while (k1 < k2) {
            kk = (k1 + k2) / 2;
            cmp = compare( page, kk, context );
            cmp = Modes.mapEq( greater, cmp );
            match |= (cmp==0);
            if ( cmp <= 0 ) k2 = kk;
            else k1 = kk + 1;
        }
        context.match = match;
        return k1;
    }
    /** return the first value matching key, or null if none match */
    public VV find(KK key) {
        CC context = context( key, null);
        context.mode = modes.gte;
        findData( context );
        return context.match ? val(context) : null;
    }
    /** find the key represented by data */
    public CC findData(CC context) {
        PP page = null;
        page = rootz;
        int ko = 0, level=0;
        context.match = false;
        while (true) {
            ko = page.findIndex( this, context.mode, context );
            if (level==depth) break;
            page = page.dexs[ko];
            level++;
        }
        if (context.match) getcc( page, context, ko );
        return context;
    }


    /** 
     * split must be done during decent of the tree if the page is full, ie, the parent is never full
     * split page0 with parent index kp into page1 (ie page1 is the new page)
     * modifies the depth if parent is null -- fixme - makes level-tracking complicated
     */
    public PP split(PP page0,PP page1,PP parent,int kp) {
        if ( parent == null ) {
            kp = 0;
            parent = createPage(false);
            rootz = parent;
            parent.dexs[kp] = page0;
            parent.num = 1;
            depth++;
        }
        page0.split( page1 );
        parent.insertSpot( kp );
        key( parent, kp, page0, page0.num-1);
        parent.dexs[kp  ] = page0;
        parent.dexs[kp+1] = page1;
        if (matheson==page0) matheson = page1;
        return page1;
    }
    /** append a new page after page (instead of splitting) */
    public void splitSeq(PP page0,PP page1,PP parent,int kp,CC context) {
        parent.num = kp+2;
        key( parent, kp,   page0, page0.num-1 );
        parent.dexs[kp+1] = page1;
    }
    public void append(CC context) {
        // fixme - bug
        // forcing last() results in corruption - keeping this disabled for now, but needs to be fixed
        //    if (true || path==null) path = last();
        PP page = null;
        if (matheson.num < cap) page = matheson;
        else {
            Path<PP> path = last();
            if (overcap(path.page)) {
                //  { insert(context); return path; }
                path = splitPath(path);
            }
            page = path.page;
        }
        int ko = page.prepInsert( this, context );
        setcc(page,context,ko);
    }
    public void insert(KK key,VV val) {
        CC context = context(key,val);
        insert( context);
    }
    public void insert(CC context) {
        PP parent = null, page = null;
        // used to record the path - could be useful if consequetive values were
        //   strongly correlated, ie try the prev insert() path, and fall back to insert()
        //   if it doesn't match
        //    Path<PP> path = null;
        int ko = -1;
        page = rootz;
        boolean right = true;
        int level = 0;
        while (true) {
            if ( overcap(page) ) {
                PP page1 = createPage(level==depth);
                page.sort( page1 );
                if (right && level > 1 && level==depth) {
                    int cmp = compare( page, page.num-1, context );
                    if (cmp > 0) {
                        splitSeq(page,page1,parent,ko,context);
                        page = page1;
                        break;
                    }
                }
                // invariant -- page is compressed and sorted
                if (parent==null) level++; // fixme -- this needs to match split()
                split( page, page1, parent, ko );
                if ( compare( page, page.num-1, context ) > 0 )
                    page = page1;
            }
            if (level==depth) break;
            ko = findIndex( modes.gte, page, context );
            //    path = new Path().set(path,page,ko);
            right &= (ko==(page.num-1));
            parent = page;
            page = page.dexs[ko];
            level++;
        }
        ko = page.prepInsert( this, context );
        setcc( page, context, ko );
        //    path = new Path().set(path,page,ko);
        // return path;
    }
    /**
     * remove context, checking page first before falling back on remove()
     * returns the page that context was found on, else null
     * 1M int elements (Set)
     *       seq/2==t/t -- pop: 40ms, hint: 55ms, remove:130ms
     *       seq/2==f/t -- pop: 68ms, hint: 72ms, remove:185ms
     *       seq/2==f/f -- pop:---ms, hint:800ms, remove:630ms
     * 
     * if external changes have effected path, eg by merging a page referred to by path
     *   results can be unpredictable, including an exception and silently corrupting the tree
     * fixme -- paths are too fragile !!!
     */
    public Path<PP> removeHinted(CC context,Path<PP> path) {
        if (path==null || path.page==null) return remove(context);
        int cmp = 1;
        if (path.page.valid(path.ko)) cmp = compare( path.page, path.ko, context );
        if (cmp != 0) {
            path.ko = path.page.findIndex( this, modes.gte, context );
            if (! context.match) return remove(context);
        }
        getcc( path.page, context, path.ko );
        PP page = path.page;
        path = delPath(path);
        if (page != path.page)
            path.page.sort(scratch);
        return path;
    }
    
    /**
     * remove the first element from the tree
     * doesn't combine the page until size goes to zero
     * returns the first page and populates context
     * used to accept a path which was used instead of adam/first()
     * but that creates complications when interleaved with other modifications
     */
    public void pop(CC context) {
        context.match = true;
        PP page = adam;
        if (page.num==0) { context.match = false; return; }
        page.sort( scratch );
        int ko = page.first();
        getcc(page,context,ko);
        if ( page.num == 1 ) {
            Path<PP> path = first();
            path.ko = ko;
            delPath( path );
        }
        else page.delete( ko );
    }
    /** delete the first element equal to context.key */
    public Path<PP> remove(CC context) {
        // fixme -- passing in a dummy path would speed things up
        Path<PP> path = null;
        PP parent = null, page = null;
        int ko = -1;
        page = rootz;
        int level = 0;
        while (true) {
            ko = page.findIndex( this, context.mode, context );
            path = new Path().set(path,page,ko);
            if (level==depth) break;
            parent = page;
            page = page.dexs[ko];
            level++;
        }
        if (! context.match) return null;
        getcc( page, context, ko );
        remove(path,context);
        return path;
    }
    public void remove(Path<PP> path,CC context) {
        PP page = path.page;
        page.delete( path.ko );
        combine( path, context );
    }
    public void combine(Path<PP> path,CC context) {
        boolean yes = true;
        int level = 0;
        for (; yes && path.prev != null; path = path.prev, level++)
            yes &= merge2( path, context, level );
        if (yes && level>0) merge2( path, context, -1 );
        if (adam==null) adam = first().page;
    }
    /**
     * merge p2 (the right element) into p1 (the left element) and update parent
     * kp is index of p1 in parent
     */
    public void merge(PP p1,PP p2,PP parent,int n1,int n2,int kp) {
        p2.merge(p1);
        key(parent,kp,parent,kp+1);
        parent.delete(kp+1);
        if (matheson==p2) matheson = p1;
    }
    /** level: 0-->leaf, -1:root */
    public boolean merge2(Path<PP> path,CC context,int level) {
        PP page = path.page;
        int num = page.num();
        if (level==0 && num != 0 && num != pac) return false;
        if (level==-1) {
            // delete the root level if it's not a leaf, ie not the only level
            if (num==1) {
                rootz = page.dexs[0];
                depth--;
            }
            return false;
        }
        PP parent = path.prev.page;
        int kp    = path.prev.ko;
        if (num==0) {
            boolean eve = adam==path.page;
            kp = parent.delete(kp);
            if (kp >= 0 && kp < parent.num) {
                path.page = parent.dexs[kp];
                if (eve) adam = path.page;
            }
            else {
                // this happens when pop() empties the branch and then remove() gets the last element
                path.page = null;
                if (eve) adam = null;
            }
            return true;
        }
        if (num > pac) return false;
        if (kp > 0) {
            PP p2 = parent.dexs[kp-1];
            int n2 = p2.num();
            if (num+n2 <= 2*pac) {
                path.page = p2;
                merge( p2, page, parent, n2, num, kp-1 );
                return true;
            }
        }
        if (kp+1 < parent.num) {
            PP p2 = parent.dexs[kp+1];
            int n2 = p2.num();
            if (num+n2 <= 2*pac) {
                merge( page, p2, parent, num, n2, kp );
                return true;
            }
        }
        return false;
    }
    public static class Path<PP extends Page> {
        Path<PP> prev;
        PP page;
        int ko;
        Path set(Path $prev,PP $page,int $ko) { prev = $prev; page = $page; ko = $ko; return this; }
    }

    Path<PP> next(Path<PP> path) {
        path.ko++;
        if (path.ko >= path.page.num) return nextPage(path);
        return path;
    }

    void getPath(Path<PP> path,CC context) {
        getcc( path.page, context, path.ko );
    }
    
    
    Path<PP> splitPath(Path<PP> path) {
        PP page1 = createPage(true);
        int levs = 0;
        PP last = null;
        Path<PP> stack = null, tmp = null;
        if (matheson==path.page) matheson = page1;
        while (true) {
            // called from append, but prior to the element being added to the page ... need to sort !!!
            path.page.sort( page1 );
            
            last = path.page;
            tmp = path;
            path = path.prev;
            tmp.prev = stack;
            stack = tmp;
            levs++;
            if (path==null || path.page.num < cap) break;
            // fixme -- would be nice to pass in a more compatible page1
            // propogate the level and pre-alloc page1s as needed, pass that level-specific page1
            // to sort as the scratch pad
            // if so, can combine the leaf with the last loop (at the bottom of this method)
            
            key( path.page, path.ko, last, last.num-1 );
        }
        if (path==null) {
            PP parent = createPage(false);
            parent.dexs[0] = rootz;
            // key get's set below ...
            // key( parent, 0, rootz, rootz.num-1 ); 
            parent.num = 1;
            rootz = parent;
            depth++;
            path = new Path().set(null,rootz,0);
        }
        key( path.page, path.ko, last, last.num-1 );
        while (--levs >= 1) {
            PP page = createPage(levs==0); // fixme -- doesn't need context
            PP parent = path.page;
            path.ko = parent.num++;
            parent.dexs[path.ko] = page;
            tmp = stack;
            stack = stack.prev;
            path = tmp.set(path,page,0);
        }
        // dry -- can just run the loop 1 more time, but need the page1 ...
        // if you fix the page1, fold this back into the loop
        PP parent = path.page;
        path.ko = parent.num++; // use prepinsert instead
        parent.dexs[path.ko] = page1;
        path = stack.set(path,page1,0);
        return path;
    }
    
    /** delete the element described by path
     * return a new/modified path that describes the next element */
    Path<PP> delPath(Path<PP> path) {
        path.ko = path.page.delete( path.ko );
        if (path.page.valid(path.ko)) return path;
        boolean del = path.page.num()==0;
        boolean eve = path.page==adam && del;
        int level = depth;
        boolean setLast = (matheson==path.page);
        while (true) {
            int num = path.page.num();
            if (path.prev==null) {
                if (num==1 && depth > 0) {
                    rootz = path.page.dexs[0];
                    depth--;
                    level = 0;
                    path.set( null, rootz, 0 );
                }
                break;
            }
            // is the next element over capacity ???
            if (path.page.valid(path.ko)) break;
            if (path.page.num() > 0) del = false;
            path = path.prev;
            level--;
            // fixme -- looks like this doesn't work right if any page is the rightmost element 
            //   need to decrement ko ???
            if (del) path.page.delete(path.ko);
            else          path.ko++;
        }
        while (level != depth) {
            PP page = path.page.dexs[path.ko];
            level++;
            path = new Path().set(path,page,0);
        }
        if (eve) {
            adam.slurp( path.page );
            if (path.prev == null) rootz = adam;
            else path.prev.page.dexs[ path.prev.ko ] = adam;
            path.page = adam;
        }
        if (setLast) matheson = path.page;
        return path;
    }
    
    Path<PP> nextPage(Path<PP> path) {
        Path<PP> prev = path.prev;
        if (prev==null) return null;
        if (++prev.ko < prev.page.num) {
            PP page = prev.page.dexs[prev.ko];
            path.page = page;
            path.ko = 0;
            return path;
        }
        int level = depth;
        while (true) {
            path = path.prev;
            level--;
            if (path==null) return null;
            path.ko++;
            if (path.ko < path.page.num) break;
        }
        while (level < depth) {
            PP page = path.page.dexs[path.ko];
            level++;
            path = new Path().set(path,page,0);
        }
        return path;
    }
    public static class Stats {
        int np, num, level;
        public String toString() {
            return String.format( "stats: np:%5d, num:%5d, level:%5d", np, num, level );
        }
    }
    public Stats stats() {
        Stats xx = new Stats();
        int num = 0, np = 0;
        for (Path path = first(); path != null; path = nextPage(path)) {
            np++;
            int n2 = path.page.num();
            num += n2;
        }
        int level = 0;
        for (Path path = first(); path != null; path = path.prev) level++;
        xx.np = np;
        xx.num = num;
        xx.level = level;
        return xx;
    }

    /** return the path to the first page. note the leaf index is 0, not necessarily the first element */
    public Path<PP> first() {
        Path path = null;
        PP page = rootz;
        int level = 0;
        while (true) {
            path = new Path().set(path,page,0);
            if (level==depth) break;
            page = page.dexs[0];
            level++;
        }
        return path;
    }
    /** return the path to the last page. note the leaf index is 0, not the last element */
    public Path<PP> last() {
        Path path = null;
        PP page = rootz;
        int level = 0;
        while (true) {
            int ko = page.num-1;
            path = new Path().set(path,page,ko);
            if (level==depth) break;
            page = page.dexs[ko];
            level++;
        }
        path.ko = 0;
        return path;
    }
    
    

    /** set the key and val (if not leaf) in context to the values from page at ko */
    public abstract void getcc(PP page,CC context,int ko);
    /** set the key and val (if not leaf) in page at ko to the vals in context */
    public abstract void setcc(PP page,CC context,int ko);
    /** copy the value from page1[k1] to page0[k0] */
    public abstract void key(PP page0,int k0,PP page1,int k1);
    /** compare the key in page at index with the key in data */
    public abstract int compare(PP page,int index,CC data);
    /** return a new tree-specific Context with key and val */
    public abstract CC context(KK key,VV val);
    /** return data.val */
    public abstract VV val(CC data);
    

    

    
    /** allocate a new Page of the same type */
    public abstract PP newPage(boolean leaf);

    /** create a new page */
    public PP createPage(boolean leaf) {
        PP page = newPage(leaf);
        page.leaf = leaf ? 1:0;
        return page;
    }
    /** is the page too full to add this key/val pair ? */
    public boolean overcap(PP page) { return page.num() == cap; }

    
    /** representation of a single page of the tree */
    public static class MapPage<KK,VV,PP extends MapPage<KK,VV,PP>> extends PageShift<KK,PP> {
//        public KK keys;
        public VV vals;

        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copyLeaf(MapPage dst,int ko,int kd,int len) {
            System.arraycopy( vals, ko, dst.vals, kd, len );
        }
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copy(PP dst,int ko,int kd,int len) {
            if (true)    System.arraycopy( keys, ko, dst.keys, kd, len );
            if (dexs!=null) System.arraycopy( dexs, ko, dst.dexs, kd, len );
            else            copyLeaf(dst,ko,kd,len);
        }
    }
    public static abstract class Page3<KK> extends Page2<KK> {
        public boolean valid(int ko) { return ko < nf+num; }
        public int delete(int ko) {
            if (ko > nf) return super.delete(ko);
            if (na==num) na--;
            num--;
            nf++;
            return nf;
        }
        public <CC extends Context> int prepInsert(Bplus<?,?,CC,Page2<KK>> map,CC context) {
            if (nf+num==map.cap) {
                copy( this, nf, 0, num );
                nf = 0;
            }
            int ko = nf+num;
            num++;
            na++;
            return ko;
        }
        public int first() { return nf; }
    }
    public static abstract class Page2<KK> extends PageShift<KK,Page2<KK>> {
        /** number of out of order elements */
        int na;
        int nf;
        public void slurp(Page2<KK> page) {
            num = page.num;
            keys = page.keys;
            na = page.na;
            nf = page.nf;
        }
        /** not applicable - can never be a branch */
        public void insertSpot(int ko) {}

        public <CC extends Context> int prepInsert(Bplus<?,?,CC,Page2<KK>> map,CC context) {
            int ko = num;
            num++;
            na++;
            return ko;
        }
        public int delete(int ko) {
            copy( (Page2) this, ko+1, ko, nf+num-ko-1 );
            if (ko >= nf+num-na) na--;
            num--;
            return ko;
        }
        public void compress() {
            if (nf > 0) copy( this, nf, 0, num );
            nf = 0;
        }
        /** sort this page using p2 as a scratch pad */
        public void sort(Page2<KK> scratch) {
            if (na==0) return;
            sort( scratch, nf+0, nf+num-na, nf+num );
            na = 0;
        }
        /** [n1,n2) is already sorted, [n2,n3) unsorted - merge sort [n1,n3) */
        public abstract void sort(Page2<KK> scratch,int n1,int n2,int n3);
        /** merge this into dst */
        public void merge(Page2<KK> dst1) {
            Page2 dst = (Page2) dst1;
            // [ dst fully sorted, this fully sorted ]
            // shift dst to zero (region r1), copy this to dst (region r2), sort r1, sort r2
            // tried another layout: [ dst.sorted, this.sorted, dst.unsorted, this.unsorted ]
            //   seems like that should be faster, but overall performance was slower
            //   see merge() ... about to delete it
            dst.compress();
            copy( dst, nf, dst.num, num );
            if (dst.na > 0) dst.sort( this,       0, dst.num-dst.na, dst.num );
            if (    na > 0) dst.sort( this, dst.num, dst.num+num-na, dst.num+num );
            dst.num += num;
            dst.na = 0;
            num = 0;
            keys = null;
        }
        public void copy(Page2<KK> dst,int ko,int kd,int len) {
            System.arraycopy( keys, ko, dst.keys, kd, len );
        }
        public <CC extends Context> int findIndex(Bplus<?,?,CC,Page2<KK>> map,int mode,CC context) {
            context.match = false;
            int k1 = nf+0, n2 = nf+num-na, n3 = nf+num, cmp = -1;
            for (; k1<n2; k1++) {
                cmp = map.compare( this, k1, context );
                if (cmp <= 0) break;
            }
            if (cmp==0) {
                context.match = true;
                return k1;
            }
            for (k1 = n2; k1 < n3; k1++) {
                cmp = map.compare( this, k1, context );
                if (cmp==0) { context.match = true; break; }
            }
            return k1;
        }
    }
    public static class PageShift<KK,PP extends PageShift<KK,PP>> extends Page<PP> {
        public KK keys;
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copy(PP dst,int ko,int kd,int len) {
            System.arraycopy( keys, ko, dst.keys, kd, len );
            System.arraycopy( dexs, ko, dst.dexs, kd, len );
        }
        public void slurp(PP page) {
            num = page.num;
            keys = page.keys;
        }
    }
    /** representation of a single page of the tree */
    public static class Page<PP extends Page<PP>> {
        // fixme::space -- get rid of leaf, num can be short, and dexs[] only needed for branches
        public int num, leaf;
        public PP [] dexs;
        /** create space for an entry at ko, only called by the map for branches, ie not leafs */
        public void insertSpot(int ko) {
            copy( (PP) this, ko, ko+1, num-ko );
            num++;
        }
        /** find the index at which context inserts into this, prep the page (eg shift), return index */
        public <CC extends Context> int prepInsert(Bplus<?,?,CC,PP> map,CC context) {
            int ko = map.findIndex( modes.gte, (PP) this, context );
            insertSpot(ko);
            return ko;
        }
        
        /** shift the this[ko:end) to this[ko+1,*) */
        public int delete(int ko) {
            copy( (PP) this, ko+1, ko, num-ko-1 );
            num--;
            return ko==num ? ko-1 : ko;
        }
        
        /** return the number of valid elements */
        public int num() { return num; }
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        public void copy(PP dst,int ko,int kd,int len) {}
        
        /** sort this page as prep for a split */
        public void sort(PP tmp) {}
        public int first() { return 0; }
        

        /** move half the keys in this page to next */
        public void split(PP dst) {
            int kp = num/2, np = num-kp;
            copy(dst,kp,0,np);
            dst.num = np;
            num = np;
        }
        /** merge this into dst */
        public void merge(PP dst) {
            copy( dst, 0, dst.num, num );
            dst.num += num;
            num = 0;
            dexs = null;
        }
 
        /** return the number of keys in the page - fixme::dry, should be made private or equiv */
        public int numkeys() {
            return leaf==1 ? num : num-1;
        }
        public String info() {
            return String.format( "Page - num:%5d\n", num );
        }
        /** find the index of context. fixme -- need to clear up usage of this and map.findIndex() */
        public <CC extends Context> int findIndex(Bplus<?,?,CC,PP> map,int mode,CC context) {
            int k1 = 0, n2 = numkeys(), cmp = -1;
            for (k1=0; k1<n2; k1++) {
                cmp = map.compare( (PP) this, k1, context );
                if (cmp <= 0) break;
            }
            context.match = cmp==0;
            return k1;
        }
        public boolean valid(int ko) { return ko < num; }
        /** slurp the contents of page into this, always called on a leaf */
        public void slurp(PP page) {
            num = page.num; // leaf 
        }
    }

    public static class Context {
        /** did the key represented by this match a key in the tree */
        public boolean match;
        /** the mode for the search */
        public int mode;
    }

    public static class DF extends Bplus<Double,Float,DF.Data,DF.Page> {
        public static class Page extends MapPage<double [],float [],Page> {}
        public static class Data extends Context {
            public double key;
            public float val;
            public Data set(double $key,float $val) { key = $key; val = $val; return this; }
        }
        public void setcc(Page page,Data cc,int ko) { page.keys[ko] = cc.key; page.vals[ko] = cc.val; }
        public void getcc(Page page,Data cc,int ko) { cc.key = page.keys[ko]; cc.val = page.vals[ko]; }
        public void key(Page p0,int k0,Page p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public Float val(Data data) { return data.val; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            p2.keys = new double[cap];
            if (leaf) p2.vals = new float[cap];
            else      p2.dexs = new Page[cap];
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            return Double.compare( data.key, page.keys[index] );
        }
        public Data context(Double key,Float val) { return new Data().set(key,val==null ? 0 : val); }
    }
    public static class II extends Bplus<Integer,Integer,II.Data,II.Page> {
        public static class Page extends MapPage<int [],int [],Page> {}
        public static class Data extends Context {
            public int key;
            public int val;
            public Data set(int $key,int $val) { key = $key; val = $val; return this; }
        }
        public void setcc(Page page,Data cc,int ko) { page.keys[ko] = cc.key; page.vals[ko] = cc.val; }
        public void getcc(Page page,Data cc,int ko) { cc.key = page.keys[ko]; cc.val = page.vals[ko]; }
        public void key(Page p0,int k0,Page p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public Integer val(Data data) { return data.val; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            p2.keys = new int[cap];
            if (leaf) p2.vals = new int[cap];
            else      p2.dexs = new Page[cap];
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            int key = data.key;
            int keyp = page.keys[index];
            return key < keyp ? -1 : key==keyp ? 0:1;
        }
        public Data context(Integer key,Integer val) { return new Data().set(key,val==null ? 0 : val); }
    }
    public static class IO<VV> extends Bplus<Integer,VV,IO.Data<VV>,IO.Page> {
        public static class Page extends MapPage<int [],Object [],Page> {}
        public static class Data<VV> extends Context {
            public int key;
            public VV val;
            public Data<VV> set(int $key,VV $val) { key = $key; val = $val; return this; }
        }
        public void setcc(Page page,Data cc,int ko) { page.keys[ko] = cc.key; page.vals[ko] = cc.val; }
        public void getcc(Page page,Data cc,int ko) { cc.key = page.keys[ko]; cc.val = page.vals[ko]; }
        public void key(Page p0,int k0,Page p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public VV val(Data data) { return (VV) data.val; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            p2.keys = new int[cap];
            if (leaf) p2.vals = new Object[cap];
            else      p2.dexs = new Page[cap];
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            int key = data.key;
            int keyp = page.keys[index];
            return key < keyp ? -1 : key==keyp ? 0:1;
        }
        public Data context(Integer key,VV val) { return new Data().set(key,val==null ? 0 : val); }
    }
    public static class Set<PP extends PageShift<int[],PP>>
            extends Bplus<Integer,Void,Set.Data,PP> {
        public static class Data extends Context {
            public int key;
            public int val;
            public Data set(int $key,int $val) { key = $key; val = $val; return this; }
        }
        public static class Page extends Page3<int []> {
            /** [n1,n2) is already sorted, [n2,n3) unsorted - merge sort [n1,n3) */
            public void sort(Page2 scratch,int n1,int n2,int n3) {
                Direct.ints.Data data = new Direct.ints.Data();
                data.set( keys, (int []) scratch.keys );
                Direct.ints.mergesort( data, n2, n3 );
                if (n2 > n1) Direct.ints.merge( data, n1, n2, n3 );
            }
        }
        public void setcc(PP page,Data cc,int ko) { page.keys[ko] = cc.key; }
        public void getcc(PP page,Data cc,int ko) { cc.key = page.keys[ko]; }
        public void key(PP p0,int k0,PP p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public Void val(Data data) { return null; }
        public PP newPage(boolean leaf) {
            if (leaf) {
                // fixme -- Page3's use less space, but make things marginally slower
                //   in some cases significantly slower eg, insert() of sequential keys
                //   must prevent the jit from optimizing (adam is always a Page3)
                Page2 page = new Page();
                page.keys = new int[cap];
                PageShift ps = page;
                return (PP) ps;
            }
            else {
                PageShift page = new PageShift();
                page.keys = new int[cap];
                page.dexs = new Bplus.Page[cap];
                return (PP) page;
            }
        }
        public Data context(Integer key,Void val) { return new Data().set(key,0); }
        public int compare(PP page,int index,Data data) {
            int key = data.key;
            int keyp = page.keys[index];
            return key < keyp ? -1 : key==keyp ? 0:1;
        }
    }
    public static class SetLong<PP extends PageShift<long[],PP>>
            extends Bplus<Long,Void,SetLong.Data,PP> {
        public static class Data extends Context {
            public long key;
            public long val;
            public Data set(long $key,long $val) { key = $key; val = $val; return this; }
        }
        public static class Page extends Page3<long []> {
            /** [n1,n2) is already sorted, [n2,n3) unsorted - merge sort [n1,n3) */
            public void sort(Page2 scratch,int n1,int n2,int n3) {
                Direct.longs.Data data = new Direct.longs.Data();
                data.set( keys, (long []) scratch.keys );
                Direct.longs.mergesort( data, n2, n3 );
                if (n2 > n1) Direct.longs.merge( data, n1, n2, n3 );
            }
        }
        public void setcc(PP page,Data cc,int ko) { page.keys[ko] = cc.key; }
        public void getcc(PP page,Data cc,int ko) { cc.key = page.keys[ko]; }
        public void key(PP p0,int k0,PP p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public Void val(Data data) { return null; }
        public PP newPage(boolean leaf) {
            PageShift page;
            if (leaf) {
                page = new Page();
            }
            else {
                // fixme - combine these ...
                page = new PageShift();
                page.dexs = new Bplus.Page[cap];
            }
            page.keys = new long[cap];
            return (PP) page;
        }
        public Data context(Long key,Void val) { return new Data().set(key,0); }
        public int compare(PP page,int index,Data data) {
            long key = data.key;
            long keyp = page.keys[index];
            return key < keyp ? -1 : key==keyp ? 0:1;
        }
    }
    public static class ObjectSet<KK extends Comparable,PP extends PageShift<KK[],PP>>
            extends Bplus<Integer,Void,ObjectSet.Data<KK>,PP> {
        Class<KK> klass;
        public ObjectSet<KK,PP> setKlass(Class<KK> $klass) { klass = $klass; return this; }
        public static class Data<KK> extends Context {
            public KK key;
        }
        public static class Page<KK> extends Page3<KK []> {
            /** [n1,n2) is already sorted, [n2,n3) unsorted - merge sort [n1,n3) */
            public void sort(Page2 scratch,int n1,int n2,int n3) {
                Direct.Objects.Data data = new Direct.Objects.Data();
                data.set( keys, (KK []) scratch.keys );
                Direct.Objects.mergesort( data, n2, n3 );
                if (n2 > n1) Direct.Objects.merge( data, n1, n2, n3 );
            }
        }
        public void setcc(PP page,Data<KK> cc,int ko) { page.keys[ko] = cc.key; }
        public void getcc(PP page,Data<KK> cc,int ko) { cc.key = page.keys[ko]; }
        public void key(PP p0,int k0,PP p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public Void val(Data data) { return null; }
        public PP newPage(boolean leaf) {
            if (leaf) {
                // fixme -- Page3's use less space, but make things marginally slower
                //   in some cases significantly slower eg, insert() of sequential keys
                //   must prevent the jit from optimizing (adam is always a Page3)
                Page<KK> page = new Page();
                page.keys = Array.newArray( klass, cap );
                PageShift ps = page;
                return (PP) ps;
            }
            else {
                PageShift page = new PageShift();
                page.keys = Array.newArray( klass, cap );
                page.dexs = new Bplus.Page[cap];
                return (PP) page;
            }
        }
        public Data context(Integer key,Void val) { return new Data(); }
        public int compare(PP page,int index,Data<KK> data) {
            return data.key.compareTo( page.keys[index] );
        }
    }

    
    public static class CycleTester {
        int nn, delta;
        int [] i1, i2;
        Set map = new Set();
        Set.Data cd = map.context(0,null);
        int iitest = 0, mod = 100000;
        { map.init(); }

        public void test() {
            i2 = Util.colon(nn);
            int min = -1;
            int kk = nn - delta;
            for (int ii = 0; ii < nn+delta; ii++, kk++) {
                if (kk==nn) kk = 0;
                if (ii==iitest)
                    ii += 0;
                if (ii < nn) {
                    boolean dbg = false;
                    map.insert(cd.set(i1[ii],0));
                    if (dbg) checkUp(ii,min,"check.insert:");
                    if (dbg) checkOver(ii-1,"over.insert:");
                    map.append( cd.set(ii+nn,0));
                    if (dbg) checkUp(ii,min,"check.append:");
                    if (dbg) checkOver(ii,"over.append:");
                }
                cd.match = false;
                if (ii < delta) {
                    map.remove( cd.set(i1[kk],0) );
                    if (cd.match) throw rte( null, "fictious: %5d, %5d\n", ii, i1[kk] );
                }
                else {
                    boolean dbg = false;
                    int key = i1[kk];
                    if (key <= min) { i2[key] = -1; key += nn; }
                    cd.match = true;
                    map.remove( cd.set(key,0) );
                    if (!cd.match && key > min && key < ii+nn)
                        throw rte( null, "missing: %5d, %5d, %5d, %5d\n", kk, i1[kk], key, min );
                    if (dbg) check( ii, min, "check1" );
                    if (dbg) checkOver( ii, "over1" );
                    map.pop(cd);
                    if (!cd.match)
                        throw rte( null, "ordering: %5d, %5d, %5d, %5d\n", kk, i1[kk], key, min );
                    if (cd.key >= nn) i2[cd.key-nn] = -1;
                    min = Math.max( cd.key, min );
                    if (dbg) check( ii, min, "check2" );
                    if (dbg) checkOver( ii, "over2" );
                }
            }
        }
        public boolean skip(int kk) {
            return !(kk >= iitest && (kk%mod)==0);
        }
        public void checkOver(int iibase,String msg) {
            if (skip(iibase)) return;
            int last = Math.min( iibase, nn );
            for (int ii = 0; ii < last; ii++) {
                int valid = i2[ii];
                if (valid==-1) continue;
                int key = ii+nn;
                map.findData( cd.set(key,0) );
                if (!cd.match)
                    throw rte( null, "%s: %5d, %5d, %5d\n", msg, iibase, ii, key );
            }
            
        }
        public void checkUp(int iibase,int min,String msg) {
            if (skip(iibase)) return;
            int first = Math.max( 0, iibase-delta );
            for (int ii = first; ii <= iibase; ii++) {
                int key = i1[ii];
                if (key <= min) continue;
                map.findData( cd.set(key,0) );
                if (!cd.match)
                    throw rte( null, "%s: %5d, %5d, %5d\n", msg, iibase, ii, key );
            }
            
        }
        public void check(int iibase,int min,String msg) {
            if (skip(iibase)) return;
            int kk = iibase - delta;
            int last = Math.min( nn, iibase );
            for (int ii = kk+1; ii < last; ii++) {
                int key = i1[ii];
                if (key <= min) continue;
                map.findData( cd.set(key,0) );
                if (!cd.match)
                    throw rte( null, "%s: %5d, %5d, %5d, %5d\n", msg, iibase, ii, key, min );
            }
            
        }
    }

    /** insert foods into a map as Integers, and then remove them left to right */
    public static void testObj(int [] foods) {
        ObjectSet<Integer,?> map = new ObjectSet();
        ObjectSet.Data<Integer> cd = new ObjectSet.Data();
        map.setKlass( Integer.class );
        map.init();
        for (int ii = 0; ii < foods.length; ii++) {
            cd.key = foods[ii];
            map.insert( cd );
        }
        for (int ii = 0; ii < foods.length; ii++) {
            map.pop(cd);
            if (!cd.match || cd.key != ii)
                throw rte( null );
        }
    }
    /** insert foods into a TreeMap as Integers, and then iterate thru them left to right
     *    delete elements if del
     */
    public static void testJava(int [] foods,boolean del) {
        java.util.TreeMap<Integer,Void> map = new java.util.TreeMap();
        for (int ii = 0; ii < foods.length; ii++)
            map.put( foods[ii], null );
        int ii = 0;
        Iterator<Entry<Integer, Void>> iterator = map.entrySet().iterator();
        if (del) while (iterator.hasNext()) {
            Integer key = iterator.next().getKey();
            iterator.remove();
            if (key != ii) throw rte( null );
            ii++;
        }
        else for (Integer key : map.keySet()) {
            if (key != ii) throw rte( null );
            ii++;
        }
        if (ii != foods.length)
            throw rte(null);
    }

    /**
     * compare Bplus2 and TreeMap using nt iterations of nn elements
     * either random ordering (if rnd) or sequential
     * results: 1M values at 1.8ghz
     *   random     -- nq: 1.3s, java: 1.6s
     *   sequential -- nq:  .4s, java:  .6s, java without deletion: .4s (ie, iterate but not delete)
     */
    public static void apples(int nn,int nt,boolean rnd) {
        for (int ii = 0; ii < nt; ii++) {
            int [] f1 = Util.colon(nn);
            if (rnd) org.srlutils.Shuffler.shuffle( f1 );
            int [] f2 = Util.colon(nn);
            if (rnd) org.srlutils.Shuffler.shuffle( f2 );
            Timer.timer.tic();
            testObj( f1 );
            double t1 = Timer.timer.tock();
            testJava( f2, true );
            double t2 = Timer.timer.tock();
            System.out.format( "apples %5b -- nq:%8.3f, java:%8.3f\n", rnd, t1, t2 );
        }
    }
    
    
    
    
    public static void main(String [] args) {
        Simple.Scripts.cpufreq( "userspace", 1800000 );
//        Rand.source.prng.setSeedSeed( 1L );
        if (false) {
            apples( 1<<20, 10, true );
            apples( 1<<20, 100, false );
            return;
        }
        
//                II map = new II();
//                II.Data cd = map.context(0,0);
//                Path<II.Page> path = null;
        Set map = new Set();
        Set.Data cd = map.context(0,null);
        int nn = 1<<20, nt = 20;
        String txt = "";
        boolean seq = false, seq2 = false, append = seq && true;
        boolean pop = seq2 && true, hint = seq2 && true;
        for (int jj = 0; jj < nt; jj++) {
            Long seed = null;
            Rand.source.setSeed( seed, false );
            System.out.format( "%20dL -- ", Rand.source.prng.seed );
            map.init();
            int [] i1, i2;
            int [] a1 = Util.colon(nn), a2 = Util.colon(nn);
            org.srlutils.Shuffler.shuffle( a2 );
            i1 = seq ? a1 : a2;
            i2 = seq2 ? a1 : a2;
            int                                 iitest = 120, mod = 1;
            CycleTester cycle = new CycleTester();
            cycle.nn = nn;
            cycle.delta = nn/2;
            cycle.i1 = a2; // always shuffled
            Timer.timer.tic();
            if (jj < nt/2) {
                cycle.test();
                Timer.timer.toc();
                continue;
            }
            int ii = 0;
            if (append) {
                for (ii = 0; ii < nn; ii++)
                    map.append(cd.set(i1[ii],ii));
            }
            else {
            for (; ii < nn-1; ii++) {
                boolean dbg = false;
                if (true && ii==64)
                    ii += 0;
                cd.set(i1[ii],ii);
                if (append)        map.append(cd);
                else               map.insert(cd);
                if (dbg && ii==125)
                    map.findData( cd.set(i1[ii],0) );
                if (ii >= iitest && (ii%mod)==0 && dbg) {
                    for (int kk = 0; kk<=ii; kk++) {
                        map.findData( cd.set(i1[kk],0) );
                        if (!cd.match)
                            throw new RuntimeException(
                                String.format( "insert.dropped: %5d, %5d, %5d, %5d\n", ii, kk, i1[ii], i1[kk] )
                                    );
                    }
                }
            }
            map.insert( cd.set(i1[ii],ii));
            }
            ii = 0;
            double t1 = Timer.timer.tock();
            Stats stats = map.stats();
            for (ii=0; ii < nn; ii++) {
                map.findData( cd.set(i1[ii],0) );
                if (! cd.match)
                    throw rte( null, "unfound: %5d, %5d\n", ii, i2[ii] );
            }
            double t3 = Timer.timer.tock();
            Path<Page> path = null;
            if ((seq2 || pop || hint) && true) {
                for (ii = 0; ii < nn-1; ii++) {
                    if (pop)              map.pop         ( cd );
                    else if (hint) path = map.removeHinted( cd.set(i2[ii],0), path );
                    else                  map.remove      ( cd.set(i2[ii],0) );
                    if (!cd.match || cd.key != i2[ii])
                        throw rte( null, "pop.missed: %5d, %5d, %5d, %s\n", ii, cd.key, i2[ii], path );
                }
            }
            else for (ii = 0; ii < nn-1; ii++) {
                boolean dbg = false;
                int kktest = 32, iicrit = 32;
                if (dbg && ii==iicrit)
                    map.findData( cd.set(i2[kktest],0) );
                if (pop)         map.pop( cd );
                else             map.remove( cd.set(i2[ii],0) );
                boolean success = cd.match && cd.key==i2[ii];
                if (dbg && ii==iicrit)
                    map.findData( cd.set(i2[kktest],0) );
                if (dbg && ii < kktest && false) {
                    map.findData( cd.set(i2[kktest],0) );
                    if (!cd.match) throw new RuntimeException( "dropped kktest " + ii );
                }
                if (ii >= iitest && (ii%mod)==0 && dbg) {
                    for (int kk = ii+1; kk<nn; kk++) {
                        map.findData( cd.set(i1[kk],0) );
                        if (!cd.match)
                            throw new RuntimeException(
                                String.format( "dropped: %5d, %5d, %5d, %5d\n", ii, kk, i1[ii], i1[kk] )
                                    );
                    }
                }
                if (! success)
                    throw rte( null, "remove.missed: %5d, %5d, %5d, %5d\n", ii, i2[ii], cd.key, i2[ii] );
            }
            double t2 = Timer.timer.tock();
            txt += map.stats() + "\n";
            map.remove( cd.set(i2[ii],0) );
            System.out.format( "nn: %5d, time: %8.3f %8.3f, %8.3f, last: %5d, %5d, %5b, %s\n",
                    nn, t1, t3, t2, cd.key, cd.val, cd.match, stats );
        }
        System.out.println( txt );
        System.out.format( "depth: %b, adam: %b\n", map.depth, map.adam==map.rootz );
        Simple.Scripts.cpufreq( "ondemand", 0 );
    }
    
}


