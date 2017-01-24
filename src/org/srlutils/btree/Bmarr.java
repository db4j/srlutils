// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import org.srlutils.btree.BtTests2;
import org.srlutils.btree.Butil.Modes;


// todo:
//   pop() works ... could implement push()
//   page.parent ... allow path-like navigation without the problems of out-of-date paths
//   finish removal of page.leaf
//   merge2 and delPath are very similar, could be combined
//     delPath looks like it won't work with paths on the right side of the tree
//   would like a means of telling if a page has been modified
//     eg, by interleaved removeHinted() and insert()

/**
 * based on Bminus, backed by key/value/dex arrays
 * 
 * an in-memory btree backed by arrays
 *   no intra-level page links are maintained, ie only an array of children
 * CC: compare context type
 * PP: page type
 */
public abstract class Bmarr<CC extends Bmarr.Context,PP extends Bmarr.Page<PP>> {
    public static final Modes modes = new Modes();
    public int cap = 4096/12; // 1<<8;
    public int pac = 3*cap/8;
    /** a page to use as a scratchpad. not thread-safe, but then nothing else is either */
    public PP scratch;
    
    public abstract PP rootz(PP page);
    public abstract int depth(int level);
    

    public void init() {
        PP root = createPage(true);
        rootz(root);
        scratch = createPage(true);
        depth(0);
    }

    /** find the index of the first key in the page >= key - stepped linear search */
    public int findIndex(PP page,int mode,CC context) {
        boolean greater = modes.greater( mode );
        int step = 16, num = page.numkeys(), k1;
        k1 = findLoop(page,step,num,step,context,greater);
        if (!greater && k1<num && compare(page,k1,context)==0)
            context.match = true;
        return k1;
    }
    int findLoop(PP page,int k1,int num,int step,CC context,boolean greater) {
        for (; k1<num; k1+=step) {
            int cmp = compare( page, k1, context );
            if (greater & cmp==0) cmp = 1;
            if (cmp <= 0) break;
        }
        if (step > 1)
            return findLoop(page,k1-step,num,1,context,greater);
        return k1;
    }
    /** find the key represented by data */
    public CC findData(CC context) {
        initContext(context);
        PP page = null;
        page = rootz(null);
        int ko = 0, level=0;
        context.match = false;
        while (true) {
            ko = findIndex( page, context.mode, context );
            if (level==context.depth) break;
            page = dexs(page,ko,level+1==context.depth);
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
    public PP split(PP page0,PP page1,PP parent,int kp,CC context) {
        if ( parent == null ) {
            parent = createPage(false);
            rootz(parent);
            kp = shift(parent,0);
            key(parent,kp,page0, page0.num-1);
            parent.dexs(kp,page0.kpage);
            depth( ++context.depth );
        }
        split(page0,page1);
        kp = shift(parent,kp);
        int kp1 = nextIndex(parent,kp);
        // for non-leafs, this isn't usually a "valid" key ... but we just did the split so it's ok
        key( parent, kp, page0, page0.num-1);
        parent.dexs(kp , page0.kpage);
        parent.dexs(kp1, page1.kpage);
        return page1;
    }
    /** append a new page after page (instead of splitting) */
    public void splitSeq(PP page0,PP page1,PP parent,int kp,CC context) {
        parent.num = kp+2;
        key( parent, kp,   page0, page0.num-1 );
        parent.dexs(kp+1, page1.kpage);
    }
    /** 
     * append to the tree ... only makes sense for sparse maps
     * the user *knows* that the key is larger than any key in the map
     */
    public void append(CC context) {
        initContext(context);
        PP page = lastPage(context);
        if (overcap(page)) {
            Path<PP> path = lastPath(context);
            path = splitPath(path,context);
            page = path.page;
        }
        int ko = shift(page,page.num);
        setcc(page,context,ko);
    }
    public void insert(CC context) {
        initContext(context);
        PP parent = null, page = null;
        // used to record the path - could be useful if consequetive values were
        //   strongly correlated, ie try the prev insert() path, and fall back to insert()
        //   if it doesn't match
        //    Path<PP> path = null;
        int ko = -1;
        page = rootz(null);
        boolean right = true;
        int level = 0;
        while (true) {
            if ( overcap(page) ) {
                PP page1 = createPage(level==context.depth);
                page.sort( page1 );
                if (right && level > 1 && level==context.depth) {
                    int cmp = compare( page, page.num-1, context );
                    if (cmp > 0) {
                        splitSeq(page,page1,parent,ko,context);
                        page = page1;
                        break;
                    }
                }
                // invariant -- page is compressed and sorted
                if (parent==null) level++; // fixme -- this needs to match split()
                split( page, page1, parent, ko, context );
                if ( compare( page, page.num-1, context ) > 0 )
                    page = page1;
            }
            if (level==context.depth) break;
            ko = findIndex( page, modes.gt, context );
            //    path = new Path().set(path,page,ko);
            right &= (ko==(page.num-1));
            parent = page;
            page = dexs(page,ko,level+1==context.depth);
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
        initContext(context);
        if (path==null || path.page==null) return remove(context);
        int cmp = 1;
        if (path.page.valid(path.ko)) cmp = compare( path.page, path.ko, context );
        if (cmp != 0) {
            path.ko = findIndex( path.page, modes.gte, context );
            if (! context.match) return remove(context);
        }
        getcc( path.page, context, path.ko );
        PP page = path.page;
        path = delPath(path, context);
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
        initContext(context);
        context.match = true;
        PP page = firstPage(context);
        if (page.num==0) { context.match = false; return; }
        page.sort( scratch );
        int ko = page.first();
        getcc(page,context,ko);
        if ( page.num == 1 ) {
            Path<PP> path = firstPath(context);
            path.ko = ko;
            delPath(path, context);
        }
        else page.delete( ko );
    }
    /** delete the first element equal to context.key */
    public Path<PP> remove(CC context) {
        initContext(context);
        // fixme -- passing in a dummy path would speed things up
        Path<PP> path = null;
        PP parent = null, page = null;
        int ko = -1;
        boolean right = true;
        page = rootz(null);
        int level = 0;
        while (true) {
            ko = findIndex( page, context.mode, context );
            path = new Path().set(path,page,ko);
            if (level==context.depth) break;
            if (ko < page.num-1) right = false;
            parent = page;
            page = dexs(page,ko,level+1==context.depth);
            level++;
        }
        if (! context.match) return null;
        getcc( page, context, ko );
        remove(path,context,right);
        return path;
    }
    /** 
     *  remove the element described by path and rebalance the tree
     *  right means that the element is the last element in the tree, ie furthest-right
     */
    public void remove(Path<PP> path,CC context,boolean right) {
        PP page = path.page;
        if (!right) cleanDups(path,context);
        page.delete( path.ko );
        combine( path, context );
    }
    /** 
     * if we've removed the last element in the page and it's a duplicate
     * need to propagate the deletion up the path
     * not required for a furthest-right element
     */
    void cleanDups(Path<PP> path,CC context) {
        // climb the path till find a page that's not going to be empty
        // use that path elements previous entry as key
        // continue to climb the path till reaching a non-last-element, setting the keys
        while (path.prev != null && path.page.num==1)
            path = path.prev;
        Path<PP> po = path;
        while (path.prev != null && path.ko==path.page.num-1) {
            path = path.prev;
            key(path.page,path.ko,po.page,po.ko-1);
        }
    }
    public void combine(Path<PP> path,CC context) {
        boolean yes = true;
        int level = context.depth;
        for (; yes && path.prev != null; path = path.prev, level--)
            yes &= merge2( path, context, level );
        if (yes & context.depth > 0 && path.page.num()==1) {
            PP child = dexs(path.page,0,context.depth==1);
            rootz(child);
            depth( --context.depth );
        }
    }
    /**
     * merge p2 (the right element) into p1 (the left element) and update parent
     * kp is index of p1 in parent
     */
    public void merge(PP p1,PP p2,PP parent,int n1,int n2,int kp) {
        merge(p2,p1);
        key(parent,kp,parent,kp+1);
        parent.delete(kp+1);
    }
    /** 
     * merge path either to the right or left
     * leafs only merge at 0 or pac
     */
    public boolean merge2(Path<PP> path,CC context,int level) {
        PP page = path.page;
        int num = page.num();
        if (level==context.depth && num != 0 && num != pac) return false;
        PP parent = path.prev.page;
        int kp    = path.prev.ko;
        if (num==0) {
            kp = parent.delete(kp);
            if (kp >= 0 && kp < parent.num) {
                path.page = dexs(parent,kp,level==context.depth);
            }
            else {
                // this happens when pop() empties the branch and then remove() gets the last element
                path.page = null;
            }
            return true;
        }
        if (num > pac) return false;
        if (kp > 0) {
            PP p2 = dexs(parent,kp-1,level==context.depth);
            int n2 = p2.num();
            if (num+n2 <= 2*pac) {
                path.page = p2;
                path.ko += p2.num;
                merge( p2, page, parent, n2, num, kp-1 );
                return true;
            }
        }
        if (kp+1 < parent.num) {
            PP p2 = dexs(parent,kp+1,level==context.depth);
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

    Path<PP> next(Path<PP> path,CC context) {
        path.ko++;
        if (path.ko >= path.page.num) return nextPage(path, context);
        return path;
    }

    void getPath(Path<PP> path,CC context) {
        context.match = true;
        getcc( path.page, context, path.ko );
    }
    
    
    Path<PP> splitPath(Path<PP> path,CC context) {
        PP page1 = createPage(true);
        int level = context.depth;
        PP last = null;
        Path<PP> stack = null, tmp = null;
        while (true) {
            // called from append, but prior to the element being added to the page ... 
            //    need to sort !!!
            path.page.sort( page1 );
            
            last = path.page;
            tmp = path;
            path = path.prev;
            tmp.prev = stack;
            stack = tmp;
            level--;
            if (path==null || !overcap(path.page)) break;
            // fixme -- would be nice to pass in a more compatible page1
            // propogate the level and pre-alloc page1s as needed, pass that level-specific page1
            // to sort as the scratch pad
            // if so, can combine the leaf with the last loop (at the bottom of this method)
            
            key( path.page, path.ko, last, last.num-1 );
        }
        if (path==null) {
            PP parent = createPage(false);
            int kp = shift(parent,0);
            PP rootz = rootz(null);
            parent.dexs(kp,rootz.kpage);
            // key get's set below ...
            // key( parent, 0, rootz, rootz.num-1 ); 
            rootz(parent);
            depth( ++context.depth );
            level = 0;
            path = new Path().set(null,parent,kp);
        }
        key( path.page, path.ko, last, last.num-1 );
        while (++level < context.depth) {
            PP page = createPage(false); // fixme -- doesn't need context
            PP parent = path.page;
            // fixme -- num isn't necessarily the end of the array, eg Pourous ...
            //   need something like:
            //     path.ko = shift(parent,parent.last());
            //   where parent.last() returns an insertion point to the far right
            path.ko = parent.num++;
            parent.dexs( path.ko, page.kpage );
            tmp = stack;
            stack = stack.prev;
            path = tmp.set(path,page,0);
        }
        // dry -- can just run the loop 1 more time, but need the page1 ...
        // if you fix the page1, fold this back into the loop
        PP parent = path.page;
        path.ko = parent.num++; // use prepinsert instead
        parent.dexs( path.ko, page1.kpage );
        path = stack.set(path,page1,0);
        return path;
    }
    
    /** 
     * delete the element described by path
     * return a new/modified path that describes the next element
     */
    Path<PP> delPath(Path<PP> path,CC context) {
        path.ko = path.page.delete( path.ko );
        if (path.page.valid(path.ko)) return path;
        boolean del = path.page.num()==0;
        int level = context.depth;
        while (true) {
            int num = path.page.num();
            if (path.prev==null) {
                if (num==1 && context.depth > 0) {
                    PP root = dexs(path.page,0,level+1==context.depth);
                    rootz(root);
                    depth( --context.depth );
                    level = 0;
                    path.set( null, root, 0 );
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
        while (level != context.depth) {
            PP page = dexs(path.page,path.ko,level+1==context.depth);
            level++;
            path = new Path().set(path,page,0);
        }
        if (path.prev == null) rootz(path.page);
        return path;
    }
    
    Path<PP> nextPage(Path<PP> path,CC context) {
        Path<PP> prev = path.prev;
        if (prev==null) return null;
        if (++prev.ko < prev.page.num) {
            PP page = dexs(prev.page,prev.ko,true);
            path.page = page;
            path.ko = 0;
            return path;
        }
        int level = context.depth;
        while (true) {
            path = path.prev;
            level--;
            if (path==null) return null;
            path.ko++;
            if (path.ko < path.page.num) break;
        }
        while (level < context.depth) {
            PP page = dexs(path.page,path.ko,level+1==context.depth);
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
    public Stats stats(CC context) {
        Stats xx = new Stats();
        int num = 0, np = 0;
        for (Path path = first(context); path != null; path = nextPage(path, context)) {
            np++;
            int n2 = path.page.num();
            num += n2;
        }
        int level = 0;
        for (Path path = first(context); path != null; path = path.prev) level++;
        xx.np = np;
        xx.num = num;
        xx.level = level;
        return xx;
    }

    /** return the path to the first page. note the leaf index is 0, not necessarily the first element */
    protected PP lastPage(CC context) {
        PP page = rootz(null);
        for (int level = 0; level < context.depth; level++)
            page = dexs(page,page.num-1,level+1==context.depth);
        return page;
    }
    /** return the path to the first page. note the leaf index is 0, not necessarily the first element */
    protected PP firstPage(CC context) {
        PP page = rootz(null);
        for (int level = 0; level < context.depth; level++)
            page = dexs(page,0,level+1==context.depth);
        return page;
    }
    /** 
     * return the path to the first page.
     * note the leaf index is 0, not necessarily the first element
     */
    public Path<PP> first(CC context) {
        initContext(context);
        return firstPath(context);
    }
    protected Path<PP> firstPath(CC context) {
        Path path = null;
        PP page = rootz(null);
        int level = 0;
        while (true) {
            path = new Path().set(path,page,0);
            if (level==context.depth) break;
            page = dexs(page,0,level+1==context.depth);
            level++;
        }
        return path;
    }
    /** return the path to the last page. note the leaf index is 0, not the last element */
    protected Path<PP> last(CC context) {
        initContext(context);
        return lastPath(context);
    }
    Path<PP> lastPath(CC context) {
        Path path = null;
        PP page = rootz(null);
        int level = 0;
        while (true) {
            int ko = page.num-1;
            path = new Path().set(path,page,ko);
            if (level==context.depth) break;
            page = dexs(page,ko,level+1==context.depth);
            level++;
        }
        path.ko = 0;
        return path;
    }

    /** insert context into page and return the index */
    public void insert(PP page,CC context) {
        int ko = findIndex( page, modes.gt, context );
        ko = shift(page,ko);
        setcc(page,context,ko);
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
    public abstract CC context();

    /** split page src into dst, ie move half of src to dst (which is empty) */
    public abstract void split(PP src,PP dst);
    /** create space for an insertion into page at ko and return the insertion point */
    public abstract int shift(PP page,int ko);

    /** merge src into dst */
    public abstract void merge(PP src,PP dst);

    
    /** read the state variables - potentially expensive */
    public void initContext(CC cc) {
        // depth() and root() can be slow, eg in hunker
        // keep an up-to-date copy here
        cc.depth = depth(-1);
    }

    
    
    /** allocate a new Page of the same type */
    public abstract PP newPage(boolean leaf);
    
    public abstract PP getPage(int kpage);
    /** create a new page */
    public abstract PP createPage(boolean leaf);

    /** is the page too full to add this key/val pair ? */
    public boolean overcap(PP page) { return page.num() == cap; }
    public void clear() {}
    public int nextIndex(PP page,int ko) { return ko+1; }
    

    /** get the page pointed to by parent[index] */
    public PP dexs(PP parent, int index, boolean leaf) {
        int kpage = parent.dexs(index);
        PP page = getPage(kpage);
        boolean l2 = page.leaf==1;
        Simple.softAssert(l2==leaf); // fixme ... remove eventually (but doesn't seem to hurt perf)
        return page;
    }
    
    
    
    
    public static class Context {
        /** did the key represented by this match a key in the tree */
        public boolean match;
        /** the mode for the search */
        public int mode;
        public int depth;
    }

    /** representation of a single page of the tree */
    public static abstract class Page<PP extends Page<PP>> {
        // fixme::space -- get rid of leaf, num can be short, and dexs[] only needed for branches
        public int num, leaf;
        public int size, kpage;
        public abstract void dexs(int index,int kpage);
        public abstract int dexs(int index);
        
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
    }
    /** representation of a single page of the tree */
    public static class ArrayPage<KK,VV,PP extends ArrayPage<KK,VV,PP>> extends Page<PP> {
        public KK keys;
        public VV vals;
        public int [] dexs;
        @SuppressWarnings("SuspiciousSystemArraycopy")
        /** copy or shift the arrays from this[ko:ko+len] to dst[kd,*] */
        public void copy(PP dst,int ko,int kd,int len) {
            if (true)   System.arraycopy( keys, ko, dst.keys, kd, len );
            if (leaf>0) System.arraycopy( vals, ko, dst.vals, kd, len );
            else        System.arraycopy( dexs, ko, dst.dexs, kd, len );
        }
        public void dexs(int index,int kpage) {
            dexs[index] = kpage;
        }
        public int dexs(int index) {
            int k2 = dexs[index];
            return k2;
        }
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
            num = kp;
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
            return ko < num ? ko : (ko==0 ? 0:ko-1);
        }
    }

    public static abstract class ArrayMap<CC extends Context,PP extends ArrayPage<?,?,PP>>
        extends Bmarr<CC,PP> {
        /** the root page */
        public PP rootz;
        int nn = 1<<20;
        int knext = 0;
        PP [] pages;
        public int depth;

        public PP    rootz(PP page) { if (page != null)    rootz = page; return rootz;    }
        public int depth(int level) { if (level >= 0) depth = level; return depth; }
        public void split(PP src,PP dst) { src.split(dst); }
        public int shift(PP page, int ko) { return page.shift(ko); }
        public void merge(PP page0,PP page1) { page0.merge(page1); }
        public void clear() {
            for (int ii = 0; ii < knext; ii++) {
                pages[ii] = null;
            }
            knext = 0;
        }
        public PP getPage(int kpage) { return pages[kpage]; }
        /** create a new page */
        public PP createPage(boolean leaf) {
            PP page = newPage(leaf);
            page.leaf = leaf ? 1:0;
            pages[knext] = page;
            page.kpage = knext++;
            return page;
        }
    }

    public interface SetDF<CC extends Context> {
        public CC set(double key,float val);
        public float get();
        
    }
    public static class DF extends ArrayMap<DF.Data,DF.Page> {
        { pages = new Page[ nn ]; }
        public static class Page extends ArrayPage<double [],float [],Page> {}
        public static class Data extends Context implements SetDF<Data> {
            public double key;
            public float val;
            public Data set(double $key,float $val) { key = $key; val = $val; return this; }
            public float get() { return val; }
        }
        public void setcc(Page page,Data cc,int ko) { page.keys[ko] = cc.key; page.vals[ko] = cc.val; }
        public void getcc(Page page,Data cc,int ko) { cc.key = page.keys[ko]; cc.val = page.vals[ko]; }
        public void key(Page p0,int k0,Page p1,int k1) { p0.keys[k0] = p1.keys[k1]; }
        public Page newPage(boolean leaf) {
            Page p2 = new Page();
            p2.keys = new double[cap];
            if (leaf) p2.vals = new float[cap];
            else      p2.dexs = new int[cap];
            return p2;
        }
        public int compare(Page page,int index,Data data) {
            double k2 = page.keys[index];
            return Double.compare( data.key, k2 );
        }
        public Data context() { return new Data(); }
    }

//    public static class EdenTree extends Eden { { map = new Btree.DF(); name = "edenTree"; } }
    public static class EdenMarr extends Eden { { map = new       DF(); name = "edenMarr"; } }

    /** quick test of the append / pop stuff that uses adam and matheson, ie garden of eden */
    public static abstract class Eden<CC extends Context & SetDF<CC>,TT extends Bmarr<CC,?>> extends TaskTimer.Runner<Void> {
        TT map;
        CC cc;
        int nn = 1<<22;
        boolean ok = true;
        String name = "bmarr.eden";
        public void alloc() { stageNames = "put look".split( " " ); setup(2,name); }

        public void init() { ok = true; map.init(); cc = map.context(); }
        public void run(int stage) throws Exception {
            if (stage == 0) insert();
            else if (stage == 1) lookup();
        }
        public boolean finish() { map.clear(); return ok; }
        public void lookup() {
            for (int ii = 0; ii < nn; ii++) {
                map.pop( cc.set(-1,-1) );
                if (cc.get() != 0.01f*ii || !cc.match)
                    ok = false;
            }
        }
        public void insert() {
            for (int ii = 0; ii < nn; ii++) map.append( cc.set(ii,0.01f*ii) );
        }
    }
    public static class Mindir extends BtTests2.Nokil {
        DF map;
        DF.Data cc = new DF.Data();
        boolean dup = true;
        boolean rem = true;
        { name = "btree.direct"; }
        public void init2() { map = new DF(); map.init(); }
        public float look(double key) {
            if (dup) map.remove( cc.set(key,-1f) );
            if (rem) map.remove( cc.set(key,-1f) );
            else map.findData( cc.set(key,-1f) );
            return cc.val;
        }
        public void put(double key,float val) {
            map.insert( cc.set(key,val) );
            if (dup) map.insert( cc.set(key,val) );
        }
        public void kiss() { map.clear(); }
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
//                        new BtTests2.Plus(),
                        new Mindir()
                        );
                return;
            }
        }
    }
    
}
    

/* 
 *  performance test
 *    Mem3 (backed by Btree3Mem) is comparable to what's currently used in Soup.Hunker
 *    Mindir (backed by Bmarr, ie an array)
 *    BtTests2.Mindir is backed by direct memory
 * 
 * mem3 is slower by a factor of 2-ish, suggesting that there's benefit to using one of the newer btrees
 *   cap = 1<<6

    btree.marr    |    1.377  1.38/8    1.45
    nokil.mem3    |    3.063  2.60/16
    minus.direct  |    1.632  1.46/9

*   number after the slash is time for the total run in seconds (from netbeans)

* 
* with cap = 4096/12, btree.marr |   1.70
*   verified after getPage: 1.72
*   verified after rootz(): 1.70
*   verified after depth(): 1.69, Btree 1.66
*        after cleanDups(): 1.71
*
* 
* 
* eden (ie adam + matheson) only offers a 2.5% speedup over using first+last
*   Eden: 1.58 v 1.62 (noEden==true)
*   this is a pure append/pop test, ie worst case scenario, probably very rare use case
*   not worth the complexity
*   with eden removed (ie no adam or matheson, using first+last instead) performance is on par with eden
* 
* Eden:
*   append+pop (eden) are much faster than insert+remove, so leaving that functionality in
*     even though the use case is probably rare
    btree.eden  |    0.554      |    1.054      |    1.608
    btree.east  |    1.062      |    1.547      |    2.609
    direct.eden |    0.576      |    1.138      |    1.714
    direct.eden2|    0.179      |    1.149      |    1.328 (using splitPath and setcc instead of insert)
    btree.eden2 |    0.292      |    1.079      |    1.370


*     direct is 5%-ish faster for insert/look, slower for append/pop
*        the append/pop result was using the slower form of append, ie insert() applied to the last page
*        using setcc directly on the last element of the last page, direct is slightly faster
*        just as with insert/look
*       
* 
 */

