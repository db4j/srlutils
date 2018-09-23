// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.srlutils.Rand;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import static org.srlutils.Unsafe.uu;
import org.srlutils.btree.BtTests2;
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
public abstract class Bminus<KK,VV,CC extends Bminus.Context,PP extends Bminus.Page<PP>> {
    public static final Modes modes = new Modes();
    public int cap = 4096/12; // 1<<8;
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

    /** find the index of the first key in the page >= key - stepped linear search */
    public int findIndex1(int mode,PP page,CC context) {
        if (false) return findIndex3(mode,page,context);
        int step = 16;
        int k1 = 0, num = page.numkeys(), cmp = -1, k2 = 0;
        for (k1=0; k1<num; k2 = k1, k1+=step) {
            cmp = compare( page, k1, context );
            if (cmp <= 0) break;
        }
        for (k1=k2; k1<num; k1++) {
            cmp = compare( page, k1, context );
            if (cmp <= 0) break;
        }
        context.match = cmp==0;
        return k1;
    }
    /** find the index of the first key in the page >= key - linear search */
    public int findIndex3(int mode,PP page,CC context) {
        int k1 = 0, num = page.numkeys(), cmp = -1;
        for (k1=0; k1<num; k1++) {
            cmp = compare( page, k1, context );
            if (cmp <= 0) break;
        }
        context.match = cmp==0;
        return k1;
    }
    /** find the index of the first key in the page >= key - binary search */
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
            ko = findIndex( page, context.mode, context );
            if (level==depth) break;
            page = page.dexs(pages, ko);
            level++;
        }
        if (context.match) getcc( page, context, ko );
        return context;
    }
    
    int nn = 1<<20;
    int knext = 0;
    PP [] pages;


    /** 
     * split must be done during decent of the tree if the page is full, ie, the parent is never full
     * split page0 with parent index kp into page1 (ie page1 is the new page)
     * modifies the depth if parent is null -- fixme - makes level-tracking complicated
     */
    public PP split(PP page0,PP page1,PP parent,int kp) {
        if ( parent == null ) {
            if (useDump)
                ((DFp)this).dump();
            parent = createPage(false);
            rootz = parent;
            kp = shift(parent,0);
            parent.dexs(kp,page0);
            depth++;
            if (useDump)
                ((DFp)this).dump();
        }
        split(page0,page1);
        kp = shift(parent,kp);
        int kp1 = nextIndex(parent,kp);
        key( parent, kp, page0, page0.num-1);
        parent.dexs(kp , page0);
        parent.dexs(kp1, page1);
        if (matheson==page0) matheson = page1;
        if (useDump)
            ((DFp)this).dump();
        return page1;
    }
    boolean useDump = false;
    /** append a new page after page (instead of splitting) */
    public void splitSeq(PP page0,PP page1,PP parent,int kp,CC context) {
        parent.num = kp+2;
        key( parent, kp,   page0, page0.num-1 );
        parent.dexs(kp+1, page1);
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
        insert(page,context);
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
            ko = findIndex( page, modes.gt, context );
            //    path = new Path().set(path,page,ko);
            right &= (ko==(page.num-1));
            parent = page;
            page = page.dexs(pages,ko);
            level++;
        }
        insert(page,context);
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
            path.ko = findIndex( path.page, modes.gte, context );
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
            ko = findIndex( page, context.mode, context );
            path = new Path().set(path,page,ko);
            if (level==depth) break;
            parent = page;
            page = page.dexs(pages,ko);
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
        merge(p2,p1);
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
                rootz = page.dexs(pages,0);
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
                path.page = parent.dexs(pages,kp);
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
            PP p2 = parent.dexs(pages,kp-1);
            int n2 = p2.num();
            if (num+n2 <= 2*pac) {
                path.page = p2;
                merge( p2, page, parent, n2, num, kp-1 );
                return true;
            }
        }
        if (kp+1 < parent.num) {
            PP p2 = parent.dexs(pages,kp+1);
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
            // called from append, but prior to the element being added to the page ... 
            //    need to sort !!!
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
            int kp = shift(parent,0);
            parent.dexs(kp,rootz);
            // key get's set below ...
            // key( parent, 0, rootz, rootz.num-1 ); 
            rootz = parent;
            depth++;
            path = new Path().set(null,rootz,kp);
        }
        key( path.page, path.ko, last, last.num-1 );
        while (--levs >= 1) {
            PP page = createPage(levs==0); // fixme -- doesn't need context
            PP parent = path.page;
            // fixme -- num isn't necessarily the end of the array, eg Pourous ...
            //   need something like:
            //     path.ko = shift(parent,parent.last());
            //   where parent.last() returns an insertion point to the far right
            path.ko = parent.num++;
            parent.dexs( path.ko, page );
            tmp = stack;
            stack = stack.prev;
            path = tmp.set(path,page,0);
        }
        // dry -- can just run the loop 1 more time, but need the page1 ...
        // if you fix the page1, fold this back into the loop
        PP parent = path.page;
        path.ko = parent.num++; // use prepinsert instead
        parent.dexs( path.ko, page1 );
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
                    rootz = path.page.dexs(pages,0);
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
            PP page = path.page.dexs(pages,path.ko);
            level++;
            path = new Path().set(path,page,0);
        }
        if (eve) {
            adam.slurp( path.page );
            if (path.prev == null) rootz = adam;
            else path.prev.page.dexs( path.prev.ko, adam );
            path.page = adam;
        }
        if (setLast) matheson = path.page;
        return path;
    }
    
    Path<PP> nextPage(Path<PP> path) {
        Path<PP> prev = path.prev;
        if (prev==null) return null;
        if (++prev.ko < prev.page.num) {
            PP page = prev.page.dexs(pages,prev.ko);
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
            PP page = path.page.dexs(pages,path.ko);
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

    /** 
     * return the path to the first page.
     * note the leaf index is 0, not necessarily the first element
     */
    public Path<PP> first() {
        Path path = null;
        PP page = rootz;
        int level = 0;
        while (true) {
            path = new Path().set(path,page,0);
            if (level==depth) break;
            page = page.dexs(pages,0);
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
            page = page.dexs(pages,ko);
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

    /** split page src into dst, ie move half of src to dst (which is empty) */
    public abstract void split(PP src,PP dst);
    /** create space for an insertion into page at ko and return the insertion point */
    public abstract int shift(PP page,int ko);
    public int findIndex(PP page,int mode,CC context) { return findIndex1(mode,page,context); }

    /** insert context into page and return the index */
    public void insert(PP page,CC context) {
        int ko = findIndex( page, modes.gt, context );
        ko = shift(page,ko);
        setcc(page,context,ko);
    }
    /** merge src into dst */
    public abstract void merge(PP src,PP dst);
    

    

    
    /** allocate a new Page of the same type */
    public abstract PP newPage(boolean leaf);

    /** create a new page */
    public PP createPage(boolean leaf) {
        PP page = newPage(leaf);
        page.leaf = leaf ? 1:0;
        pages[knext] = page;
        page.kpage = knext++;
        return page;
    }
    /** is the page too full to add this key/val pair ? */
    public boolean overcap(PP page) { return page.num() == cap; }
    public void clear() {}
    public int nextIndex(PP page,int ko) { return ko+1; }
    

    public static class DFa extends BasicMap<Double,Float,DFa.Data,DFa.Page> {

        
        public static class Page extends ArrayPage<double [],float [],Page> {}
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
            else      p2.dexs = new int[cap];
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            return Double.compare( data.key, page.keys[index] );
        }
        public Data context(Double key,Float val) { return new Data().set(key,val==null ? 0 : val); }
        { pages = new Page[ nn ]; }
    }
    public static class ArrayPage<KK,VV,PP extends ArrayPage<KK,VV,PP>> extends BasicPage<PP> {
        public void commit() {}
        public int [] dexs;
        public KK keys;
        public VV vals;
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copy(ArrayPage dst,int ko,int kd,int len) {
            if (true)   System.arraycopy( keys, ko, dst.keys, kd, len );
            if (leaf>0) System.arraycopy( vals, ko, dst.vals, kd, len );
            else        System.arraycopy( dexs, ko, dst.dexs, kd, len );
        }
        public void dexs(int index,PP p2) {
            dexs[index] = p2.kpage;
        }
        public PP dexs(PP [] pages, int index) {
            int k2 = dexs[index];
            return pages[k2];
        }
    }
    
    /** representation of a single page of the tree */
    public static class MapPage<PP extends MapPage<PP>> extends BasicPage<PP> {
        public ByteBuffer buf;

        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copy(PP dst,int ko,int kd,int len) {
            org.srlutils.Array.copy( this.buf, dst.buf, ko*size, kd*size, len*size );
        }
        public void slurp(PP page) {
            num = page.num;
            buf = page.buf;
        }
        public void dexs(int index,PP p2) {
            buf.position( index*size + size - 4 );
            buf.putInt( p2.kpage );
        }
        public PP dexs(PP [] pages, int index) {
            buf.position( index*size + size - 4 );
            int k2 = buf.getInt();
            return pages[k2];
        }
    }

    /** representation of a single page of the tree */
    public static class DirectPage<PP extends DirectPage<PP>> extends BasicPage<PP> {
        public long buf;

        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        public void copy(PP dst,int ko,int kd,int len) {
            uu.copyMemory( buf+ko*size, dst.buf+kd*size, len*size );
        }
        public void slurp(PP page) {
            num = page.num;
            buf = page.buf;
        }
        public void dexs(int index,PP p2) {
            int offset = index*size + size - 4;
            uu.putInt( buf+offset, p2.kpage );
        }
        public PP dexs(PP [] pages, int index) {
            int offset = index*size + size - 4;
            int k2 = uu.getInt(buf+offset);
            return pages[k2];
        }
    }

    /** a map backed by a Pourous array */
    public static class DFp extends Bminus<Double,Float,DFp.Data,DFp.Page> {

        public void split(Page src,Page dst) { src.split(dst); }
        public int shift(Page page,int ko) { return page.shift(ko); }
        public int nextIndex(Page page,int ko) {
            return page.dw.nextIndex(ko+1);
        }

        public void merge(Page src, Page dst) {}
        public static class Page extends PourousPage<Page> {
            { size = 12; }
            void set(Data cc,int ko) {
                dw.key = cc.key;
                dw.val = cc.val;
                dw.setcc(ko);
            }
            void get(Data cc,int ko) {
                dw.getcc(ko);
                cc.key = dw.key;
                cc.val = dw.val;
            }
            double key(int ko) { return dw.get(ko); }
            void key(int ko,Page src,int ksrc) {
                dw.set(ko,src.key(ksrc));
            }

            public int shift(int ko) {
                ko = dw.distribute(ko);
                num++;
                return ko;
            }
            
        }
        public static class Data extends Context {
            public double key;
            public float val;
            public Data set(double $key,float $val) { key = $key; val = $val; return this; }
        }
        { pages = new Page[ nn ]; }
        public void setcc(Page page, Data cc, int ko) { page.set(cc,ko); }
        public void getcc(Page page,Data cc,int ko) { page.get(cc,ko); }
        public void key(Page p0, int k0, Page p1, int k1) {
            p0.key(k0,p1,k1);
        }
        public Float val(Data data) { return data.val; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            Pourous.Worker pb = p2.dw;
            pb.init(cap,6);
            // for best results p2.dw.skip=5 (12 bytes * 5 --> 60, cache line is 64)
            //   but it's final, so need to set manually
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            double key = data.key;
            double k2 = page.dw.get(index);
            return key<k2 ? -1 : key==k2 ? 0:1;
        }
        public Data context(Double key,Float val) { return new Data().set(key,val==null ? 0 : val); }
        public void clear() {
            for (int ii = 0; ii < knext; ii++)
                pages[ii].dw.clean();
        }
/*
 * find()
 *   x -- searching branch for find -- num-1 keys, find 1st real gte (use abbrev)
 *   x -- searching leaf for find - 1st real gte
 * findNext()
 *   x -- searching branch for insert -- 
         * num-1 keys, find the *last* real eq else 1st real gt (use abbrev)
 *   x -- searching leaf for insert - 1st position gt
 */
        
        public int findIndex(Page page,int mode,Data context) {
            Pourous.Worker pb = page.dw;
            boolean greater = modes.greater( mode );
            page.dw.key = context.key;
            context.match = false;
            if (greater) {
                if (page.leaf==0)
                    return findBranch(page);
                else
                    return pb.findNext(false);
            }
            int k1 = pb.find(page.leaf==0);
            if (page.leaf==1 && k1 < cap && pb.eq(k1))
                    context.match = true;
            return k1;
        }
        public int findBranch(Page page) {
            Pourous.Worker pb = page.dw;
            int k1 = pb.findNext(true);
            int k2 = pb.lastIndex();
            k1 = Math.min(k1,k2);
            int k3 = pb.prevIndex(k1-1);
            if (k3 >= 0 && pb.eq(k3)) return k3;
            k1 = pb.nextIndex(k1);
            return k1;
        }
        public void dump() {
            System.out.println( "-----------------------------------------------------------" );
            dump( rootz, "" );
            System.out.println( "-----------------------------------------------------------" );
        }
        public void dump(Page page,String prefix) {
            Pourous.Worker pb = page.dw;
            System.out.format( "%spage %5d.%s, %5d entries, %5d\n",
                    prefix, page.kpage, page.leaf==1 ? "leaf":"branch", page.dw.num(), page.num );
            System.out.format( "%s\t%s\n", prefix, page.dw.info() );
            for (int k1 : pb.getIter()) {
                page.dw.getcc(k1);
                if (page.leaf==0) {
                    Page p2 = page.dexs(pages,k1);
                    System.out.format( "%s- branch:%5d key:%8.3f --> %d\n", 
                            prefix, k1, page.dw.key, p2.kpage );
                    dump(p2,prefix+"  ");
                }
                else {
                    System.out.format( "%s| %5d: %8.3f --> %8.3f\n",
                            prefix, k1, page.dw.key, page.dw.val );
                }
            }
            
        }
        
    }
    public static class PourousPage<PP extends PourousPage<PP>> extends Page<PP> {
        Pourous.SubWorker.DF dw = new Pourous.SubWorker.DF();
        public void split(PP dst) {
            dw.split(dst.dw);
            num = dw.num();
            dst.num = dst.dw.num();
        }

        public void slurp(PP page) {
            num = page.num;
            dw = page.dw;
        }
        public void dexs(int index,PP p2) {
            int offset = index*size + size - 4;
            uu.putInt( dw.vo+offset, p2.kpage );
        }
        public PP dexs(PP [] pages, int index) {
            int offset = index*size + size - 4;
            int k2 = uu.getInt(dw.vo+offset);
            return pages[k2];
        }
        public int delete(int ko) { return 0; }
    }
    
    public static abstract class BasicMap<KK,VV,CC extends Context,PP extends BasicPage<PP>>
        extends Bminus<KK,VV,CC,PP> {
        public void split(PP src,PP dst) { src.split(dst); }
        public int shift(PP page, int ko) { return page.shift(ko); }
        public void merge(PP page0,PP page1) { page0.merge(page1); }
    }
    public static abstract class BasicPage<PP extends BasicPage<PP>> extends Page<PP> {
        /** create space for an entry at ko, only called by the map for branches, ie not leafs */
        public int shift(int ko) {
            copy( (PP) this, ko, ko+1, num-ko );
            num++;
            return ko;
        }
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
        }
        /** shift the this[ko:end) to this[ko+1,*) */
        public int delete(int ko) {
            copy( (PP) this, ko+1, ko, num-ko-1 );
            num--;
            return ko==num ? ko-1 : ko;
        }
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        public void copy(PP dst,int ko,int kd,int len) {}
    }
    
    /** representation of a single page of the tree */
    public static abstract class Page<PP extends Page<PP>> {
        // fixme::space -- get rid of leaf, num can be short, and dexs[] only needed for branches
        public int num, leaf;
        public int size, kpage;
        public abstract void dexs(int index,PP p2);
        public abstract PP dexs(PP [] pages, int index);
        
        public abstract int delete(int ko);
        
        
        /** return the number of valid elements */
        public int num() { return num; }
        
        /** sort this page as prep for a split */
        public void sort(PP tmp) {}
        public int first() { return 0; }
        
        


        /** return the number of keys in the page - fixme::dry, should be made private or equiv */
        public int numkeys() {
            return leaf==1 ? num : num-1;
        }
        public String info() {
            return String.format( "Page - num:%5d\n", num );
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

    public static class DF extends BasicMap<Double,Float,DF.Data,DF.Page> {
        public static class Page extends MapPage<Page> {
            { size = 12; }
            void set(Data cc,int ko) {
                buf.position( ko*size );
                buf.putDouble( cc.key ).putFloat( cc.val );
            }
            void get(Data cc,int ko) {
                buf.position( ko*size );
                cc.key = buf.getDouble();
                cc.val = buf.getFloat();
            }
        }
        public static class Data extends Context {
            public double key;
            public float val;
            public Data set(double $key,float $val) { key = $key; val = $val; return this; }
        }
        { pages = new Page[ nn ]; }
        public void setcc(Page page, Data cc, int ko) { page.set(cc,ko); }
        public void getcc(Page page,Data cc,int ko) { page.get(cc,ko); }
        public void key(Page p0, int k0, Page p1, int k1) {
            double key = p1.buf.getDouble( k1*p1.size );
            p0.buf.putDouble( k0*p0.size, key );
        }
        public Float val(Data data) { return data.val; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            p2.size = leaf ? 12 : 12;
            p2.buf = ByteBuffer.allocateDirect( cap*p2.size );
            p2.buf.order( ByteOrder.nativeOrder() );
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            double key = page.buf.getDouble( index*page.size );
            return Double.compare( data.key, key );
        }
        public Data context(Double key,Float val) { return new Data().set(key,val==null ? 0 : val); }
    }
    /** 
     * a direct memory version - performs on the order of the array-backed version
     *   BtTests2 results are 10% slower than DFa
     */
    public static class DFd extends BasicMap<Double,Float,DFd.Data,DFd.Page> {
        public static class Page extends DirectPage<Page> {
            { size = 12; }
            void set(Data cc,int ko) {
                int offset = ko*size;
                uu.putDouble( buf+offset, cc.key );
                uu.putFloat( buf+offset+8, cc.val );
            }
            void get(Data cc,int ko) {
                int offset = ko*size;
                cc.key = uu.getDouble( buf+offset );
                cc.val = uu.getFloat( buf+offset+8 );
            }
            double key(int ko) { return uu.getDouble( buf+ko*size ); }
            void key(int ko,Page src,int ksrc) {
                uu.putDouble( buf + ko*size, src.key(ksrc) );
            }
        }
        public static class Data extends Context {
            public double key;
            public float val;
            public Data set(double $key,float $val) { key = $key; val = $val; return this; }
        }
        { pages = new Page[ nn ]; }
        public void setcc(Page page, Data cc, int ko) { page.set(cc,ko); }
        public void getcc(Page page,Data cc,int ko) { page.get(cc,ko); }
        public void key(Page p0, int k0, Page p1, int k1) {
            p0.key(k0,p1,k1);
        }
        public Float val(Data data) { return data.val; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            p2.size = leaf ? 12 : 12;
            p2.buf = uu.allocateMemory( cap*p2.size );
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            double key = data.key;
            double k2 = page.key(index);
            return key<k2 ? -1 : key==k2 ? 0:1;
        }
        public Data context(Double key,Float val) { return new Data().set(key,val==null ? 0 : val); }
        public void clear() {
            for (int ii = 0; ii < knext; ii++)
                uu.freeMemory( pages[ii].buf );
        }
    }
    
    public static class Demo {
        public static void main(String[] args) throws Exception {
            if (true) {
                Simple.Scripts.cpufreqStash( 2300000 );
                int nn = 1000000;
                BtTests2.Config tc = new BtTests2.Config().set( nn);
                TaskTimer tt = new TaskTimer().config( tc ).init( 3, 1, true, true );
                tt.widths( 8, 3 );
                tt.autoTimer( 
                        new BtTests2.Pour()
//                        , 
//                        new BtTests2.Mindir()
                        );
                return;
            }
            Bminus.DFp map = new Bminus.DFp();
            Bminus.DFp.Data cc = new Bminus.DFp.Data();
            int nn = 1<<12;
            nn = 1000000;
            Long seed = null;
            Rand.source.setSeed( seed, true );
            double [] keys = Rand.rand(nn+1);
            map.init();
            for (int ii = 0; ii < nn; ii++) {
//                System.out.println(ii);
                if (ii==572)
                    Simple.nop();
                map.insert( cc.set(keys[ii],ii+.5f) );
            }
            boolean dbg = false;
            if (dbg) map.dump();
            map.insert( cc.set(keys[nn],nn+.5f) );
            if (dbg) map.dump();

            for (int ii = 0; ii <= nn; ii++) {
                if (ii==552)
                    Simple.nop();
                double key = keys[ii];
                map.findData( cc.set(key,0) );
                if ( !cc.match || ii%100000==0 || cc.key != key)
                    System.out.format( "%5b %4d %8.3f %8.3f diff:%g\n", cc.match, ii, cc.key, cc.val,
                            cc.key-key);
            }
            map.findData(cc.set(keys[nn],0));
            map.clear();
        }
    }
    
    
    
}
    


/*


* test of map performance backed by Pourous
*   direct uses direct memory, but as a monolithic array
*   pour uses a Pourous array
*   pour usually wins, typically 5%-ish, max 20%
*   cap = 4096/12
*   eg:

              |             put  |            look  |          totals      run#2      run#3
  minus.pour  |        0.772714  |        0.716276  |        1.488989   1.522723   1.534461
minus.direct  |        1.048261  |        0.632078  |        1.680339   1.566505   1.590678




* pour: bin size 6 bits, bin.skip = 5, with sub-bin-scan
              |             put               look  |          totals                   
  minus.pour  |        0.782881  |        0.695535  |        1.478417   1.464380   1.450406
minus.direct  |        1.086748  |        0.630495  |        1.717244   1.630265   1.626199
























*/

