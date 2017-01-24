// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import org.srlutils.Simple;
import org.srlutils.TaskTimer;
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
public abstract class Bparr<KK,VV,CC extends Bparr.Context,PP extends Bparr.Page<PP>> {
    public static final Modes modes = new Modes();
    public int cap = 4096/12; // 1<<6;
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
    public int findIndex(PP page, int mode, CC context) {
        int k1 = 0, num = page.numkeys(), cmp = -1;
        for (k1=0; k1<num; k1++) {
            cmp = compare( page, k1, context );
            if (cmp <= 0) break;
        }
        context.match = cmp==0;
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
        parent.shift( kp );
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
            ko = findIndex( page, modes.gte, context );
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


    
    
    
    
    public static class Context {
        /** did the key represented by this match a key in the tree */
        public boolean match;
        /** the mode for the search */
        public int mode;
    }

    /** representation of a single page of the tree */
    public static class Page<PP extends Page<PP>> {
        // fixme::space -- get rid of leaf, num can be short, and dexs[] only needed for branches
        public int num, leaf;
        public PP [] dexs;
        /** create space for an entry at ko, only called by the map for branches, ie not leafs */
        public void shift(int ko) {
            copy( (PP) this, ko, ko+1, num-ko );
            num++;
        }
        /** find the index at which context inserts into this, prep the page (eg shift), return index */
        public <CC extends Context> int prepInsert(Bparr<?,?,CC,PP> map,CC context) {
            int ko = map.findIndex( (PP) this, modes.gte, context );
            shift(ko);
            return ko;
        }
        
        /** shift the this[ko:end) to this[ko+1,*) */
        public int delete(int ko) {
            copy( (PP) this, ko+1, ko, num-ko-1 );
            num--;
            return ko < num ? ko : (ko==0 ? 0:ko-1);
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
        public <CC extends Context> int findIndex(Bparr<?,?,CC,PP> map,int mode,CC context) {
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
    
    /** representation of a single page of the tree */
    public static class ArrayPage<KK,VV,PP extends ArrayPage<KK,VV,PP>> extends Page<PP> {
        public KK keys;
        public VV vals;

        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copyLeaf(ArrayPage dst,int ko,int kd,int len) {
            System.arraycopy( vals, ko, dst.vals, kd, len );
        }
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        @SuppressWarnings("SuspiciousSystemArraycopy")
        public void copy(PP dst,int ko,int kd,int len) {
            if (true)    System.arraycopy( keys, ko, dst.keys, kd, len );
            if (dexs!=null) System.arraycopy( dexs, ko, dst.dexs, kd, len );
            else            copyLeaf(dst,ko,kd,len);
        }
        public void slurp(PP page) {
            num = page.num; // leaf 
            keys = page.keys;
            vals = page.vals;
            dexs = page.dexs;
        }
    }

    public static class DF extends Bparr<Double,Float,DF.Data,DF.Page> {
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
            else      p2.dexs = new Page[cap];
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            return Double.compare( data.key, page.keys[index] );
        }
        public Data context(Double key,Float val) { return new Data().set(key,val==null ? 0 : val); }
    }
    public static class II extends Bparr<Integer,Integer,II.Data,II.Page> {
        public static class Page extends ArrayPage<int [],int [],Page> {}
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
    
    
    public static class Tray extends BtTests2.Nokil {
        DF map;
        DF.Data cc = new DF.Data();
        { name = "nokil.tray"; }
        public void init2() { map = new DF(); map.init(); }
        public float look(double key) {
            return map.findData( cc.set(key,-1f) ).val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
        }
    }
    

    public static class Demo {
        public static void main(String [] args) throws Exception {
            if (true) {
                Simple.Scripts.cpufreqStash( 2300000 );
                int nn = 1000000;
                BtTests2.Config tc = new BtTests2.Config().set( nn);
                TaskTimer tt = new TaskTimer().config( tc ).init( 3, 1, true, true );
                tt.widths( 8, 3 );
                tt.autoTimer(
                        new Tray()
                        );
                return;
            }
        }
    }
    
}


