// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import java.util.Iterator;
import java.util.TreeMap;
import org.srlutils.DynArray;
import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import org.srlutils.rand.Source;

/**
 * a red black tree implementation - intended to allow non-unique values
 * it also provides an concrete entry which can be used to navigate directly
 * credits:
 *   http://en.wikipedia.org/wiki/Red-black_tree
 *   http://book.huihoo.com/gnu-libavl/RB-Balancing-Rule.html
 *
 * benchmarked against java.util.TreeMap and performance seems similar
 * TT: the type of the nodes
 * CC: the type of the compare data (Void is ok)
 * no attempt is made to ensure thread safety
 * modifications from multiple threads is explicitly forbidden, @see checkThreads
 * 
 * link worth looking at:
 * http://eternallyconfuzzled.com/tuts/datastructures/jsw_tut_rbtree.aspx
 */
public abstract class TreeDisk<TT,CC> implements Iterable<TreeDisk.Entry<TT>> {

    /**
     * color definitions RED==false
     * <ul>
     *        <li>root is black (not required, but the convention used here)
     *   </li><li>all leaves are black (not required)
     *   </li><li>both children of every red node are black (rule #1)
     *   </li><li>every simple path from a given node to any of its non-branching descendants (leaves)
     *              contains the same number of black nodes (rule #2)
     *   </li>
     * </ul>
     */
    public static final boolean RED = false, BLACK = true;
    /** the null-node marker      */ public Entry dummy = new Dummy(this);
    /** the root node of the tree */ public Entry<TT> root = dummy;
    /** the size of the tree      */ public int size;
    /** enable debugging info */
    public Debug dbg = new Debug();
    /** a means of visualizing the tree - uninitialized until it is used */
    public Enview view;
    private int nget, nput, ndel;
    
    /** control debugging info */
    public class Debug {
        /**
         * enable debugging
         *   fix: textual cues about fixes
         *   graph: visualize the graph
         */
        public boolean fix = false, graph = false, ensure = false;
        /** update the graph if graph is enabled */
        public void graph() { if (graph) TreeDisk.this.graph(); }
        /** the thread that is allowed to modify the tree */
        public Thread thread;
    }
    /** a callback to allow incremental viewing */
    public static interface Enview { public void graph(TreeDisk tree); }
    public void graph() { if (view != null) view.graph(this); }

    public Info clearInfo() { Info info = info(); nget = nput = ndel; return info; }
    public Info info() { return new Info(this); }
    public static class Info {
        public int nget, nput, ndel;
        private Info() {}
        public Info(TreeDisk t) { nget = t.nget; nput = t.nput; ndel = t.ndel; }
        public String print() { return String.format( "nget:%d, nput:%d, ndel:%d", nget, nput, ndel ); }
        public static Info dummy() { return new Info(); }
    }
    
    
    
    /**
     * the intention for this structure is that it be used from only a single thread
     * synchronized is possible, but hasn't been accounted for in the design
     * this method enforces that the tree is modified only from a single thread
     * all methods that modify the tree should use this as a check
     */
    public void checkThreads() {
        Thread t2 = Thread.currentThread();
        if (dbg.thread==null) dbg.thread = t2;
        if (dbg.thread != t2)
            org.srlutils.Simple.softAssert( false, "multi-thread modifications: %s and %s", dbg.thread, t2 );
        if (dbg.ensure) ensure();
    }

    /** clear the tree */
    public void clear() { root = dummy; size = 0; }

    /** @return the size of the tree */
    public int size() { return size; }

    /** a marker class to indicate that a node is empty */
    public static class Dummy extends Entry {
        public TreeDisk tree;
        public Dummy(TreeDisk $tree) { super($tree); tree = $tree; parent = left = right = this; color = BLACK; }
        public void right(Entry entry) {}
        public void left(Entry entry) {}
        public void parent(Entry entry) {}
        public String toString() { return "dummy"; }
        public void color(boolean _color) {}
        public void check(DepthData d1) {}
        public Dummy nul() { return null; }
        public boolean real() { return false; }
        public boolean fake() { return true; }
    }


    /**
     *  a marker class to allow methods to indicate that no match was found,
     *  but allow the location info to be preserved, ie (parent, val, cmp)
     *  should never be added to the tree itself (ie, recreate as a real Entry instead
     */
    public static class Loc<TT> extends Entry<TT> {
        /** the location of the entry relative to it's parent */
        public int cmp;
        public Loc(TreeDisk tree) { super(tree); }
        public Loc<TT> cmp(int $cmp) { cmp = $cmp; return this; }
        /** add a pre-located entry to the tree */
        public Entry<TT> insertEntry(TreeDisk tree) {
            Entry<TT> dup = new Entry(tree).set( val, parent );
            tree.insertEntry( dup, cmp );
            return dup;
        }
        public boolean loc() { return true; }
    }

    /** a node in the tree */
    public static class Entry<TT> {
        /** the data the node represents */
        public TT val;
        /** relations */
        public Entry<TT> left, right, parent;
        /** color */
        public boolean color = BLACK;

        public Entry(TreeDisk tree) { left = right = parent = tree.dummy; }

        public Entry<TT> nul() { return this; }
        public boolean real() { return true; }
        public boolean fake() { return false; }
        /** is the entry a Loc, ie represent a hypothetical location in the tree and not an existing node */
        public boolean loc() { return false; }

        /** 
         * fix the links (using this as truth) that should be pointing to this node
         */
        public Entry<TT> fixLinks(Entry<TT> old) {
            left .parent(this);
            right.parent(this);
            if (parent.right==old) parent.right(this);
            else                   parent.left (this);
            return this;
        }


        /** copy all the meta info (ie everything except the value) from xx into this, return This */
        public Entry<TT> copyMeta(Entry <TT> xx) {
            left   = xx.left;
            right  = xx.right;
            parent = xx.parent;
            color  = xx.color;
            return this;
        }
        /** set the val and parent for the Entry, return This */
        public Entry set(TT val, Entry<TT> parent) { this.val = val; this.parent = parent; return this; }

        /** set this.right = entry */
        public void  right(Entry entry) {  right = entry; }
        /** set this.left = entry */
        public void   left(Entry entry) {   left = entry; }
        /** set this.parent = entry */
        public void parent(Entry entry) { parent = entry; }
        /** set color */
        public void color(boolean _color) { color = _color; }

        /** are both children present and black ? */
        public boolean black2() { return right.color==BLACK && left.color==BLACK; }


        
        /** return the previous entry in the tree, dummy to indicate no prev exists */
        public Entry<TT> prev() { return trav(false,false); }
        /** return the next entry in the tree, dummy to indicate no next exists */
        public Entry<TT> next() { return trav(true ,false); }
        /** return the @right (else left) child */
        public Entry<TT> child(boolean right) { return right ? this.right : left; }
        /**
         * return the next (if true, else prev) entry in the tree
         * wrap around to the first/last if requested, else return dummy to indicate no next exists
         * note: this is a generic method, ie next or prev
         *   microbenchmarked against the dedicated methods and it appears competitive
         *   dropping the dedicated methods
         */
        public Entry<TT> trav(boolean next,boolean wrap) {
            Entry<TT> xx = child(next), yy = this, po = parent;
            if (xx.fake()) {
                // nothing to the right, next is the first parent st we're not the right-hand child
                while (po.real() && yy == po.child(next)) {
                    yy = po;
                    po = po.parent;
                }
                if (po.real()) return po;
                if (!wrap) return po; // ie, dummy
                while (yy.real()) {
                    po = yy;
                    yy = yy.child(!next);
                }
                return po;
            }
            // take the left-most member of the right-hand subtree
            while (xx.child(!next).real()) xx = xx.child(!next);
            return xx;
        }
        /**
         * derive the owning tree by descending the left branches, the Dummy node keeps a copy of the tree
         * the owning TreeDisk is not stored to save a bit of space
         */
        private static TreeDisk getTree(Entry xx) {
            while (xx.real()) xx = xx.left;
            return ((Dummy)xx).tree;
        }
        /** remove this entry from tree (must be the tree it belongs to) */
        public void remove(TreeDisk tree) { tree.deleteEntry( this ); }
        /** slightly inefficient remove ... provide the tree for improved performance */
        public void remove() { remove( getTree(this) ); }
        /**
         * insert if not already entreed
         * add this entry's payload to the node if the entry is a location
         *   a new "pure" entry will be allocated and returned
         * otherwise do nothing, return this
         */
        public Entry<TT> insertEntry(TreeDisk tree) {
            return this;
        }
        /**
         * check the invariants for the sub-tree rooted by this node
         * violations return true
         * results are stored in d1
         */
        public void check(DepthData dd) {
            dd.depth = dd.black = 0;

            // recursively check the children
            left.check(dd);
            int depth = dd.depth, black = dd.black;
            right.check(dd);


            // check rule #1
            if (color==RED && (left.color==RED || right.color==RED)) {
                dd.red++;
                if (dd.redNode==null) dd.redNode = this;
            }

            // check rule #2
            dd.depth = Math.max( dd.depth, depth );
            dd.depth++;
            int b1 = black, b2 = dd.black;
            if (b1==b2 && b1 >= 0) dd.black += (color==BLACK ? 1 : 0);
            else {
                dd.black = -1;
                if (dd.blackNode == null) dd.blackNode = this;
            }
        }
        public String toString() {
            return (color ? "b" : "r") + (left.fake() ? " " : "l") + (right.fake() ? " ":"r") + ":" + val;
        }
    }

    /**
     * representation of the invariants check
     * 1. the black depth should be uniform
     * 2. no red node should have red children
     * 3. the delta from one node to the next should never be negative (using compare())
     */
    public static class DepthData<TT> implements Cloneable {
        /** black and red entries that violate the invariants, null for not violated */
        public Entry<TT> blackNode, redNode;
        /** the depth of the tree, the black-depth, number of red violations, and the worst delta */
        public int depth, black, red, worst;
        /** does the tree violate the invariants */
        public boolean violates() { return black < 0 || red > 0 || worst < 0; }
        /** the depth of the valid tree, or -1 to indicate a violation of the invariants */
        public int depth() { return violates() ? -1 : depth; }
        public String toString() {
            String rn = redNode==null ? "" : String.format( "(%s)", redNode );
            String bn = blackNode==null ? "" : String.format( "(%s)", blackNode );
            return String.format( "red: %d%s, black: %d%s, depth: %d, worst: %d", red, rn, black, bn, depth, worst );
        }
    }

    /**
     * check the invariants for the tree
     * returns the DepthData
     * compliance returns the black-depth (which must be the same for all non-branching paths)
     * the max height (not black height) is 2*log2( size+1 )
     */
    public DepthData<TT> check() {
        int worst = 1;
        for (Entry<TT> last = first(), entry = last.next(); entry.real(); last = entry, entry = entry.next()) {
            CC cc = compareData( entry.val );
            int cmp = compare( entry.val, last.val, cc );
            if (dbg.ensure && cmp < 0)
                System.out.format( "check.compare :: %s, %s\n", last.val, entry.val );
            worst = Math.min( worst, cmp );
        }
        DepthData d1 = new DepthData();
        root.check( d1 );
        d1.worst = worst;
        return d1;
    }

    /** ensure that the invariants are met - throws a runtime exception if not */
    public void ensure() {
        DepthData dd = check();
        if (dd.violates() && dbg.ensure) {
            dump( " -- " );
            graph();
            check();
        }
        org.srlutils.Simple.softAssert( !dd.violates(), "map violates invariants ... %s", dd );
    }

    /**
     * the theoretical bound on the depth for a tree of this size, ie 2*log2(size+1)
     *   http://www.eli.sdsu.edu/courses/fall95/cs660/notes/RedBlackTree/RedBlack.html
     */
    public static int theoryDepth(int size) {
        int log = 31 - Integer.numberOfLeadingZeros( size + 1 );
        return 2 * log;
    }
    public        int theoryDepth()         { return theoryDepth( size ); }

    public Iter          <TT>  iterator()                { return new Iter( first() ); }
    public Iter.Value          iteratorValue()           { return iterator().new Value(); }

    public Iterable<TT> valueIter = new ValueIter();
    public class ValueIter implements Iterable<TT> {
        public Iterator<TT> iterator() { return new Iter( first() ).value(); }
    }

    public class Iter<TT> implements Iterator<Entry<TT>> {
        public Entry<TT> next;

        public Iter(Entry<TT> first) { next = first; }
        public boolean hasNext() { return next.real(); }

        public Entry<TT> next() {
            Entry<TT> ret = next;
            next = next.next();
            return ret;
        }
        public void remove() { throw new UnsupportedOperationException( "Not supported yet." ); }
        public Value value() { return new Value(); }
        public class Value implements Iterator<TT> {
            public boolean hasNext() { return Iter.this.hasNext(); }
            public TT      next   () { return Iter.this.next().val; }
            public void    remove () {        Iter.this.remove(); }
        }
    }

    public boolean isEmpty() { return size == 0; }


    /** return the first entry in the tree, ie the left-most, or dummy if empty */
    public Entry<TT> first() {
        Entry entry = root; while (entry. left != dummy) entry = entry. left; return entry;
    }
    /** return the last entry in the tree, ie the right-most, or dummy if empty */
    public Entry<TT> last() {
        Entry entry = root; while (entry.right != dummy) entry = entry.right; return entry;
    }

    /** return >0 if v1 > v2, cc is the compareData associated with v1 */
    public abstract int compare(TT v1,TT v2, CC cc);
    /** user data that will be passed to compare() */
    public CC compareData(TT v1) { return null; }


    /** get the first entry greater than or equal to key, or dummy if no such key exists */
    public Entry<TT> ceiling(TT key) {
        nget++;
        if (key==null || key==dummy) return last();
        Entry<TT> xx = root, last = dummy, best = dummy;
        CC cc = compareData( key );
        while (xx != dummy) {
            int cmp = compare( key, xx.val, cc );
            if (cmp > 0)      {            xx = xx.right; }
            else if (cmp < 0) { last = xx; xx = xx.left;  }
            else              { best = xx; xx = xx.right; }
        }
        return best==dummy ? last : best;
    }

    /** get the first entry less than or equal to key, or dummy if no such key exists */
    public Entry<TT> floor(TT key) {
        nget++;
        if (key==null || key==dummy) return first();
        Entry<TT> xx = root, last = dummy, best = dummy;
        CC cc = compareData( key );
        while (xx != dummy) {
            int cmp = compare( key, xx.val, cc );
            if (cmp > 0)      { last = xx; xx = xx.right; }
            else if (cmp < 0) {            xx = xx.left;  }
            else              { best = xx; xx = xx.left;  }
        }
        return best==dummy ? last : best;
    }

    /** insert val into the tree and return it's entry */
    public Entry<TT> put(TT key) {
        checkThreads();
        Entry<TT> xx = root, parent = dummy;
        CC cc = compareData( key );
        int cmp = 0;
        while (xx != dummy) {
            parent = xx;
            cmp = compare( key, xx.val, cc );
            xx = (cmp < 0) ? xx.left : xx.right;
        }
        Entry<TT> child = new Entry(this).set( key, parent );
        insertEntry( child, cmp );
        return child;
    }

    /**
     * search the tree for an entry matching val and return it if found
     * otherwise, if alloc, return a Loc describing the location that it would insert at
     * else return null
     */
    public Entry<TT> getEntryOrLocation(TT key,boolean alloc) {
        nget++;
        Entry<TT> xx = root, parent = dummy;
        int cmp = 0;
        CC cc = compareData( key );
        while (xx != dummy) {
            parent = xx;
            cmp = compare( key, xx.val, cc );
            if (cmp==0) return xx;
            xx = (cmp < 0) ? xx.left : xx.right;
        }
        return alloc ? new Loc(this).cmp( cmp ).set( key, parent ) : null;
    }

    /** remove the first entry matching key from the tree and return it's value, else null */
    public TT remove(TT key) {
        Entry<TT> pp = getEntryOrLocation( key, false );
        TT ret = null;
        if ( pp != null ) { ret = pp.val; deleteEntry( pp ); }
        return ret;
    }

    /** return the first matching entry in the tree, else null */
    public Entry<TT> getEntry(TT key) { return getEntryOrLocation( key, false ); }

    /** return the value of the first matching entry in the tree, else null */
    public TT get(TT key) {
        Entry<TT> pp = getEntryOrLocation( key, false );
        return pp==null ? null : pp.val;
    }

    /** search the tree for an entry matching val and return it, otherwise create and return a new entry(key) */
    public Entry<TT> getEntryOrPut(TT key) {
        return getEntryOrLocation( key, true ).insertEntry( this );
    }

    /** insert after ref, ie at either ref.right or ref.next().left, one of which is empty */
    public Entry<TT> insertAfter(Entry<TT> ref,TT val) {
        Entry<TT> dup = new Entry(this).set( val, ref.parent );
        int cmp = -1;
        if (ref==dummy)            dup.parent = first();
        else if (ref.right==dummy)        cmp = 1;
        else                       dup.parent = ref.next().parent;
        insertEntry(dup,cmp);
        return dup;
    }

    /** add a pre-located entry to the tree as the (right if cmp gte 0 else left) child of it's parent */
    public void insertEntry(Entry<TT> entry,int cmp) {
        nput++;
        checkThreads();
        size++;
        Entry parent = entry.parent;
        if (root == dummy) { root = entry; return; }

        // invariant: pre-located entries are always at dummy links
        if (cmp < 0) parent.left =  entry;
        else         parent.right = entry;
        if (dbg.fix) dump( "before fix: " );
        fixAfterInsertion(entry);
        if (dbg.fix) dump( "after fix: " );
    }


    /** set up the links to make xx the right child of po */
    public void right(Entry po,Entry xx) { po.right( xx ); xx.parent( po ); }

    /** set up the links to make xx the left child of po */
    public void left(Entry po,Entry xx) { po.left( xx ); xx.parent( po ); }

    /**
     * rotate the tree, ie pull up the right child (r-> means the right arrow)
     * po r-> rt l-> lt     ===>   rt l-> po r-> lt
     *   po                 rt
     *     \               /
     *      rt     ==>    po
     *     /               \
     *   lt                 lt
     */
    public boolean rotateLeft(Entry po) {
        Entry rt = po.right;
        right( po, rt.left );
        if (po.parent == dummy) root = rt;
        if (po.parent.left == po) left( po.parent, rt );
        else                      right( po.parent, rt );
        left( rt, po );
        return false;
    }
    // po l--> lt r--> rt   ====>  lt r--> po l--> rt, returns false
    /**
     * rotate the tree, ie pull up the left child
     *
     *     po               lt
     *    /                   \
     *  lt         ==>         po
     *    \                   /
     *     rt               rt
     */
    public boolean rotateRight(Entry po) {
        Entry lt = po.left;
        left( po, lt.right );
        if (po.parent == dummy) root = lt;
        if (po.parent.left == po) left( po.parent, lt );
        else                      right( po.parent, lt );
        right( lt, po );
        return false;
    }

    //  http://book.huihoo.com/gnu-libavl/RB-Balancing-Rule.html
    //  1.  No red node has a red child.
    //  2.  Every simple path from a given node to one of its non-branching node descendants contains the same number of black nodes.
    //  by always inserting as a red we never violate #2
    //    so all we need to do is fix #1, and propogate it up the tree
    //  should be comparable to doug lea's RBTree/RBCell implementation (public domain)
    //    ftp://g.oswego.edu/pub/java/collections.tar.gz
    public void fixAfterInsertion(Entry<TT> xx) {
        xx.color( RED );

        while (xx != dummy && xx != root && xx.parent.color == RED) {
            Entry po = xx.parent, pp = po.parent, aunt = pp.left, yy = dummy;

            if (pp.left.color == RED && pp.right.color == RED) {
                pp.left.color(BLACK);
                pp.right.color( BLACK );
                pp.color( RED );
                xx = pp;
            }
            else if (po == pp.left) {
                if (xx == po.right) {
                    rotateLeft( po );
                    xx = po; po = po.parent; pp = po.parent;
                }
                po.color( BLACK );
                pp.color( RED );
                rotateRight( pp );
            } else {
                if (xx == po.left) {
                    rotateRight( po );
                    xx = po; po = po.parent; pp = po.parent;
                }
                po.color( BLACK );
                pp.color( RED );
                rotateLeft( pp );
            }
        }
        root.color( BLACK );
    }





    /** replace xx with replace in the tree and clear xx.parent */
    public void swapChild(Entry<TT> xx,Entry<TT> replace) {
        if      (xx == xx.parent.left ) xx.parent.left  = replace;
        else if (xx == xx.parent.right) xx.parent.right = replace;
        xx.parent( dummy );
    }


    /** replace xx with rep in the tree, setting/fixing all links (note: val is untouched) */
    public Entry<TT> replaceEntry(Entry<TT> xx,Entry<TT> rep) {
        if (root==xx) root = rep;
        return rep.copyMeta(xx).fixLinks(xx);
    }


    /** delete the entry from the tree */
    public void deleteEntry(Entry<TT> po) {
        ndel++;
        checkThreads();
        dbg.graph();
        size--;

        // if internal, swap with next, which won't be internal
        //   copy the meta data instead of swapping pointers so that navigation can be preserved
        if (po.left != dummy && po.right != dummy) {
            Entry<TT> nxt = po.next();
            if (true) {
                Entry<TT> tmp = replaceEntry( nxt, new Entry(this) );
                replaceEntry( po, nxt );
                po.copyMeta( dummy );
                po = tmp;
            }
            else if (false) {
                po.val = nxt.val;
                Entry<TT> tmp = new Entry(this).copyMeta(nxt).fixLinks(nxt);
                po = tmp;
            }
            else {
                po.val = nxt.val;
                po = nxt;
            }
            dbg.graph();
        }

        // invariant: at most 1 child
        Entry<TT> replacement = po.left==dummy ? po.right : po.left;

        if (replacement != dummy) {
            // swap replacement in for the entry
            replacement.parent = po.parent;
            if (po.parent == dummy)       root  = replacement;
            else                   swapChild( po, replacement );

            po.left = po.right = dummy;
            // if we were red, then parent and child are black - invariant is maintaned
            // but if we were black, then we're changing the black-depth of the child (if any) or the parent
            if (po.color == BLACK) fixAfterDeletion(replacement);
        }
        else if (po.parent == dummy) root = dummy;
        else {
            //  No children. Use self as phantom replacement and unlink.
            if (po.color == BLACK) fixAfterDeletion(po);
            if (po.parent != dummy) swapChild( po, dummy ); // fixme::dry -- already tested po.parent==dummy ...
        }
    }


    /**
     *  the black-depth of the sub-tree beginning with the parent has been reduced by 1
     *  need to replace the lost black by going up the tree
     */
    public void fixAfterDeletion(Entry<TT> xx) {
        dbg.graph();
        while (xx != root && xx.color == BLACK) {

            boolean lefty = xx==xx.parent.left;
            Entry<TT> cross = null, criss = null, sib = lefty ? xx.parent.right : xx.parent.left;


            // ensure that sib is black
            if (sib.color == RED) {
                // parent can't be red if sib is red ... flip them and rotate sib up the tree
                //   guarantees the new sib is black (it's the child of the old sib which was red)
                sib.color( BLACK );
                xx.parent.color( RED );
                if (lefty) { rotateLeft ( xx.parent ); sib = xx.parent.right; } // new sib is the child of red
                else       { rotateRight( xx.parent ); sib = xx.parent.left;  } //   ==> black
                dbg.graph();
            }


            // invariant: sib is black, still need to add a black to parent's depth
            // need to remove a black from sib to offset the eventual addition to the parent
            if ( sib.black2() ) {
                // safe to make sib red, now just need to add a black to parent
                sib.color( RED );
                xx = xx.parent;
            }
            else {
                if (lefty) { criss = sib.right; cross = sib.left;  }
                else       { criss = sib.left ; cross = sib.right; }
                if ( criss.color == BLACK ) {
                    // rotation maintains the invariant
                    // cross is red (criss is black, and they're not both black)
                    // when done the new sib will be black and the new criss will be red
                    cross.color( BLACK );
                    sib.color( RED );
                    if (lefty) { rotateRight( sib ); sib = xx.parent.right; }
                    else       { rotateLeft ( sib ); sib = xx.parent.left;  }
                    dbg.graph();
                }
                // invariants: black:x,sib red:criss
                // if the parent is black, sib's promotion fixes x's violation
                //   - the red criss is flipped to account for the loss of black parent
                // if parent is red, parent flips, fixes x's violation
                //   - sib flips red, criss flips black --> ok
                //   - cross picks up parent's black so depth ok
                sib.color( xx.parent.color );
                xx.parent.color( BLACK );

                if (lefty) { sib.right.color(BLACK); rotateLeft ( xx.parent ); }
                else       { sib.left .color(BLACK); rotateRight( xx.parent ); }
                xx = root;
                dbg.graph();
            }
        }
        xx.color( BLACK );
        dbg.graph();
    }

    public static class ComparableSet<TT extends Comparable> extends TreeDisk<TT,Void> {
        public int compare(TT v1, TT v2,Void cc) { return v1.compareTo( v2 ); }
    }

    public void dump(String pre) {
        Entry entry = first();
        while ( entry.real() ) {
            System.out.println( pre + entry );
            entry = entry.next();
        }
        System.out.println();
    }






    public static abstract class Generator {
        public Source source = new Source();
        public Long seed;
        /** set the source seed back to the last seed if reset, else increment it */
        public long clear(boolean reset) {
            kk = 0;
            if (seed==null)      seed = source.setSeed();
            else if (reset)             source.setSeed( seed );
            else                        source.setSeed( ++seed );
            return seed;
        }
        public int start = 0, kk = 0, skip = 1;
        abstract int val();
        public String name() { return getClass().getSimpleName(); }

        public static class Rand extends Generator {
            {start = Integer.MIN_VALUE;}
            int val() { return source.nextInt(); }
        }
        public static class Up   extends Generator { int val() { return kk++; } };
        public static class Down extends Generator {
            int offset = 0;
            int val() { return offset - kk++; }
        };
        /**
         * a composite generator that alternates between random and sequential values
         *   sequential runs are of random length on [0,skip), and begin pdown/total of the time
         */
        public static class Comp extends Generator {
            {start = Integer.MIN_VALUE;}
            public int pup = 100, pdown = 200, total = 1000;
            int last, till, dir;
            int val() {
                if (last != till) { last += dir; return last; }
                int val = source.nextInt();
                int  v2 = source.nextInt(0,Integer.MAX_VALUE), v3 = v2 % total;

                if (v3 > pdown) return val;

                int jump = 1;
                dir = v3<pup ? jump : -jump;
                last = val;
                till = val + (v2%skip)*dir;
//                System.out.format( "jump: %8d till %8d\n", last, till-last );
                return val;
            }
            public String name() { return String.format( "comp(%3d,%3d)", skip, pdown ); }
            public void set(int $skip) { skip = $skip; }
            public long clear(boolean reset) { last = till; return super.clear( reset ); }
            public void format(int rows) {
                int vo = val(), v1;
                for (int ii = 0; ii < rows; ii++) {
                    for (int jj = 0; jj < 8; jj++, vo=v1) {
                        v1 = val();
                        System.out.format( "%8d\t", v1-vo );
                    }
                    System.out.format( "\n" );
                }
            }
        };
    }

    public static abstract class Tester extends TaskTimer.Runner<Integer> implements Iterable<Integer> {
        /** offset removed values and reinsert      */  public int offset = Integer.MAX_VALUE;
        /** the number of elements to insert        */  public int nn;
        /** the source for inserts                  */  public Generator mode;
        /** a short name describing the test        */  public String name;
        /** the seed that was used to drive the run */  public long savedSeed;
        /** test results                            */  public int min, max, remissed, missed, depth, untrav;

        /** insert val into the map */
        public abstract void insert(int val);
        /** return the (first) element matching val from the map, else null */
        public abstract Integer get(int val);
        /** remove the (first) element matching val from the map */
        public abstract Integer remove(int val);
        /** return the depth of the map */
        public abstract int depth();
        /** if alloc, allocate new resources, else clean them up */
        public abstract Tester initMap(boolean alloc);
        /** return a blurb describing the test */
        public String info() { return String.format( "%2s.%s", name, mode.name() ); }
        { stageNames = "put trav rem look".split( " " ); }

        public void init() {
            savedSeed = mode.clear( true );
            initMap(true);
        }

        public void run(int stage) throws Exception {
            if (stage==0)
                for (int ii = 0; ii < nn; ii++) insert( mode.val() );
            else if (stage==1) trav();
            else if (stage==2) remove();
            else if (stage==3) lookup();
        }

        public boolean finish() throws Exception {
            depth = depth();
            initMap( false );
            // advance the seed so that the next run is unique ...
            savedSeed = mode.clear( false );
            return true;
        }

        public void kiss() {
            int td = theoryDepth( nn );
            // note:
            // the removed elements are offset and then reinserted ... if the offset value hits a
            // value that's already been removed, and there's a 2nd copy of that value waiting to be
            // removed, the unique maps could show a difference between untrav and remissed
            String trav = (untrav==remissed)
                    ? String.format( "[%2d,%9d]", min, max )
                    : String.format( "fail* %d", untrav );
            System.out.format(
                "%-20s -- %18s, missed: %4d/%-4d, depth: %2d/%2d -- %dL\n",
                info(),   trav, remissed, missed,      depth, td, savedSeed
            );
        }

        /** set the number of elements to insert and the generator, and setup the test */
        public Tester setup(int $nn, Generator $mode) {
            nn = $nn;
            mode = $mode;
            super.setup( stageNames.length, info() );
            return this;
        }
        /** test the traverse of all the elements in the tree */
        public void trav() {
            int last = mode.start;
            int count = 0;
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
            for (Integer val : this) {
                min = Math.min( min, val - last );
                max = Math.max( max, val - last );
                last = val;
                count++;
            }
            untrav = nn - count;
        }
        /** remove all the elements from the map in the order they were added, offset them, and re-add */
        public void remove() {
            int count = 0;
            mode.clear( true );
            for (int ii = 0; ii < nn; ii++) {
                int val = mode.val();
                Integer found = remove( val );
                if (found == null) count++;
                else               insert( found+offset );
            }
            remissed = count;
        }
        /**
         * verify that all the elements (offset) that should be in the map are
         * then check a range that shouldn't
         */
        public void lookup() {
            int count = 0;
            int no = nn / 16;
            int [] other = new int[ 2*no ];
            mode.clear( true );
            for (int ii = 0; ii < nn; ii++) {
                int val = mode.val()+offset;
                Integer found = get( val );
                if (found == null) count++;
                if (val >= -no && val < no) other[ no + val ] = 1;
            }
            int count2 = 0;
            for (int ii = 0; ii < other.length; ii++) {
                if (other[ii] == 0) {
                    Integer found = get( -no + ii );
                    if (found != null) count2--;
                }
            }
            missed = (count == 0) ? count2 : count;
        }
    }

    /** make multiple traversal passes using next() */
    public static abstract class TestTraverse extends TestTD {
        { stageNames = "put trav".split( " " ); }
        public void run(int stage) throws Exception {
            if (stage==0)
                for (int ii = 0; ii < nn; ii++) insert( mode.val() );
            else if (stage==1) trav();
        }
        public void trav() {
            int last = mode.start;
            int count = 0;
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;
            for (int ii = 0; ii < 10; ii++) {
                count = 0;
                last = mode.start;
                for (Entry<Integer> first = first(); first.real(); first = next(first)) {
                    Integer val = first.val;
                    min = Math.min( min, val - last );
                    max = Math.max( max, val - last );
                    last = val;
                    count++;
                }
            }
            untrav = nn - count;
        }
        public Entry<Integer> first() { return map.last(); }
        public Entry<Integer> next(Entry<Integer> entry) { return entry.trav(false,false); }
    }

    public static class TestTD extends Tester {
        { name = "TD"; }
        TreeDisk<Integer,Void> map;
        public void insert(int val) { map.put( val ); }
        public Integer get(int val) { return map.get( val ); }
        public Iterator<Integer> iterator() { return map.iterator().new Value(); }
        public TestTD initMap(boolean alloc) { map = alloc ? new ComparableSet() : null; return this; }
        public int depth() { return map.check().depth(); }
        public Integer remove(int val) { return map.remove( val ); }
    }
    public static class TestTM extends Tester {
        { name = "TM"; }
        public Object marker = new Object();
        TreeMap<Integer,Object> map;
        public void insert(int val) { Object obj = map.put( val, marker ); }
        public Integer get(int val) { return map.containsKey( val ) ? val : null; }
        public Iterator<Integer> iterator() { return map.keySet().iterator(); }
        public TestTM initMap(boolean alloc) { map = alloc ? new TreeMap() : null; return this; }
        public int depth() { return 0; }
        public Integer remove(int val) { return map.remove( val )==marker ? val : null; }
    }

    public static class Demo {
    public static void main(String [] args) throws Exception {
        Simple.Scripts.cpufreq( "userspace", 1800000 );
        // all randomness should cascade from source, so setting seed to non-null should be deterministic
        Long seed = null;
        org.srlutils.Rand.source.setSeed( seed, true );
        int n2 = 1 << 20;

        if (false) {
            // a run can be reproduced by providing the generator seed (and params)
            Generator.Comp gen = new Generator.Comp();
            gen.skip = 312;
            gen.seed = 2751689643794220336L;
            new TestTD().setup( n2, gen ).runAll(1,0);
            return;
        }

        DynArray.Objects<Tester> da = new DynArray.Objects().init( Tester.class );

        TaskTimer tt = new TaskTimer().config(1).init( 4, 4, true, true );
        tt.width = 5;
        tt.dec = 3;

        int nt = 1, skip = 1;
        skip = 1 << 1;

        for (int ii = 0; ii < nt; ii++, skip <<= 3) {
            Generator.Comp gen = new Generator.Comp();
            gen.skip = skip;
            da.add( new TestTD().setup( n2, gen ) );
            da.add( new TestTM().setup( n2, gen ) );
        }
        tt.autoTimer( da.trim() );

        int ns = 1 << 20;
        Tester   up = new TestTD().setup(ns,new Generator.Up());
        Tester   um = new TestTM().setup(ns,new Generator.Up());
        Tester down = new TestTD().setup(ns,new Generator.Down());
        Tester dowm = new TestTM().setup(ns,new Generator.Down());
        tt.autoTimer( up, um, down, dowm );
        Simple.Scripts.cpufreq( "ondemand", 0 );
    }
    }


}




// 2011.04.05 results, TD is us, TM is java treemap
//   in general we're faster
//   generator is composite with a 20% chance of starting sequential runs, with lengths 1 8 64 and 512
//   ie, from totally random, to mostly sequential
//        Statistics:
//        -----------
//                          |      put             |     trav             |      rem             |     look             |   totals
//        TD.comp(  1,200)  |    5.716      0.082  |    0.238      0.041  |   18.959      0.507  |    8.093      0.275  |   33.007      0.802
//        TM.comp(  1,200)  |    6.446      0.597  |    0.214      0.084  |   20.187      0.555  |    9.142      0.175  |   35.989      0.273
//        TD.comp(  8,200)  |    4.004      0.094  |    0.187      0.039  |   12.731      0.205  |    5.214      0.104  |   22.137      0.103
//        TM.comp(  8,200)  |    4.612      0.563  |    0.184      0.071  |   13.632      0.330  |    5.799      0.270  |   24.226      0.494
//        TD.comp( 64,200)  |    1.753      0.021  |    0.119      0.012  |    5.718      0.114  |    1.770      0.141  |    9.360      0.148
//        TM.comp( 64,200)  |    1.966      0.145  |    0.112      0.025  |    6.349      0.121  |    2.095      0.031  |   10.522      0.055
//        TD.comp(512,200)  |    1.205      0.048  |    0.093      0.005  |    3.417      0.545  |    1.120      0.578  |    5.834      0.047
//        TM.comp(512,200)  |    1.917      0.662  |    0.080      0.010  |    3.372      0.652  |    0.970      0.010  |    6.339      0.023



// next v trav(next,wrap)
//                             put     trav    total  |      put     trav    total  |
//            Iter     0:    7.380    2.213    9.593  |    6.608    1.617    8.225  | 
//            Iter     1:    7.100    3.114   10.214  |    6.825    2.140    8.965  | 
//            Iter     2:    6.610    2.623    9.234  |    6.573    3.214    9.787  | 
//            Iter     3:    5.939    2.193    8.131  |    6.400    2.315    8.716  | 
//            Iter     4:    7.159    1.383    8.542  |    6.080    1.137    7.217  | 
//            Iter     5:    6.146    3.106    9.252  |    6.594    3.702   10.296  | 
//            Iter     6:    6.812    2.267    9.078  |    6.107    3.125    9.232  | 
//            Iter     7:    5.921    1.846    7.766  |    6.466    2.026    8.492  | 
//            Average   :    6.509    2.150    8.660  |    6.312    2.497    8.809  | 
//
//        TD.comp(  1,200)  |    6.509      0.498  |    2.150      0.634  |    8.660      0.578
//        TD.comp(  1,200)  |    6.312      0.223  |    2.497      0.990  |    8.809      1.121




// trav(t), next, trav(f), prev, trav(val=t)
//        TD.comp(  1,200)  |    6.806      0.347  |    2.089      0.645  |    8.895      0.411
//        TD.comp(  1,200)  |    6.627      0.508  |    1.996      0.753  |    8.623      0.769
//        TD.comp(  1,200)  |    6.116      0.139  |    2.924      0.558  |    9.040      0.517
//        TD.comp(  1,200)  |    6.199      0.257  |    3.056      0.908  |    9.255      0.833
//        TD.comp(  1,200)  |    6.435      0.597  |    2.178      1.006  |    8.614      0.744

// prev
//        TD.comp(  1,200)  |    6.618      0.444  |    2.662      0.947  |    9.279      1.042


// using an abstract toplevel and 4 subclasses
//   next, trav(f), prev, trav(t)
//        Statistics:
//        -----------
//                          |      put             |     trav             |   totals
//        TD.comp(  1,200)  |    6.314      0.491  |    2.546      0.665  |    8.860      0.368
//        TD.comp(  1,200)  |    6.805      0.612  |    2.354      0.966  |    9.159      0.619
//        TD.comp(  1,200)  |    6.278      0.375  |    2.889      0.575  |    9.167      0.732
//        TD.comp(  1,200)  |    6.059      0.095  |    1.495      0.360  |    7.554      0.337



/*

        conclusions, for 1M entries, at 1.8ghz
        treemap is 10% faster for sequential data, treedisk is 10% faster for random
            should try to understand why
  


    random
                      |   put          |  trav          |   rem          |  look          | totals         
    TD.comp(  2,200)  | 1.565   0.109  | 0.090   0.055  | 3.830   0.032  | 1.663   0.227  | 7.148   0.293 
    TM.comp(  2,200)  | 1.604   0.120  | 0.142   0.024  | 4.270   0.217  | 1.899   0.180  | 7.914   0.255 

    sequential
             |   put          |  trav          |   rem          |  look          | totals         
      TD.Up  | 0.450   0.006  | 0.041   0.000  | 0.992   0.018  | 0.330   0.052  | 1.813   0.056 
      TM.Up  | 0.396   0.008  | 0.035   0.002  | 0.740   0.017  | 0.305   0.017  | 1.475   0.031 
    TD.Down  | 0.449   0.022  | 0.057   0.003  | 0.660   0.009  | 0.321   0.021  | 1.487   0.043 
    TM.Down  | 0.368   0.008  | 0.049   0.001  | 0.616   0.029  | 0.322   0.017  | 1.354   0.031 

    inserting 1M of random data into treemap takes 1.5s, removing them takes 2s
        based on runs with very simple insert/remove loops
        numbers here are a little higher - test framework overhead ???
    much faster with sequential data, .40s to add, .22s to remove






















*/