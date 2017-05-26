// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import org.srlutils.Timer;
import org.srlutils.Util;
import java.util.Iterator;
import org.srlutils.Simple;
import static org.srlutils.Simple.Exceptions.rte;

// todo:
//   if next() and prev() were methods (instead of fields) provided by a 3rd party class
//   then non-conforming classes could be handled without requiring any extra storage
//   (as opposed to the inner class method which wastes a additional object+reference)
//
//   the Listee + Lister classes could be merged, but if the Lister methods were called
//   on a what was really just a node it would result in silent losage
//   would also increase the likelyhood of method name conflicts

/**
 * a doubly linking list node, intended to be extended ... making the extended class a node itself
 *   ie, payload as node
 *   as opposed to an external list node with a reference to a payload
 * head <--> next.node.prev <--> tail
 * TT is the node type
 * for the case where direct inheritance is impossible, Listee can be mixed in using an inner class
 */
public class Listee<TT extends Listee<TT>> {
    /** the next element, ie in the direction of head, null indicating the end of the list */
    public TT next;
    /** the previous element, ie in the direction of tail, null indicating the end of the list */
    public TT prev;

    /** null out the links to the rest of the list - unsafe, ie the list is not maintained */
    public void cleanup() { next = prev = null; }

    /**
     * only for circular lists, ie ones created with the static append or equivalent
     * return the next element in the list or null to indicate the list is terminated
     * @param base the first element in the list
     * @return the next element, or null if already the last element
     */
    public TT next(TT base) {
        return next==base ? null : next;
    }
    /**
     * maintain a circular linked list, inserting node before base
     * @param <TT> the node type
     * @param base the first element of the list or null to indicate the list is empty
     * @param node the node to insert
     * @return the new base, which is unchanged unless base is null
     */
    public static <TT extends Listee<TT>> TT append(TT base,TT node) {
        if (base==null) { node.next = node.prev = node; return node; }
        TT prev = base.prev;
        node.next = base;
        node.prev = prev;
        prev.next = node;
        base.prev = node;
        return base;
    }

    
    /** the base of a doubly linked list */
    public static class Lister<TT extends Listee<TT>> implements Iterable<TT> {
        // note: check is useful for debugging, but is made final for speed ... remove if needed
        /** perform checks prior to most operations */
        public final boolean check = false;
        /** the head of the list, ie the end that push and pop */
        public TT head;
        /** the tail of the list, ie the end that append and drop */
        public TT tail;
        private Thread thread;
        private final boolean dbg = false;
        public int size;


        public void checkThreads() {
            Thread t2 = Thread.currentThread();
            if (thread==null) thread = t2;
            if (thread != t2)
                org.srlutils.Simple.softAssert( false, "multi-thread modifications: %s and %s", thread, t2 );
        }
        public boolean dump(String txt) {
            if (txt==null) txt = "";
            int cnt = 0;
            TT last = null;
            boolean match = true;
            for (TT node = head; node != null; last = node, node = node.prev, cnt++) {
                match &= (last==node.next && (last==null || last.prev==node));
            }
            System.out.format( "%sSummary: %6d nodes, for:%5b, rev:%5b\n", txt, cnt, last==tail, match );
            return last==tail && match;
        }
        public int size() { return size; }
        
        
        /** add node to the tail of the list */
        public void append(TT node) {
            if ( check ) checkUnlinked( node );
            node.next = tail;
            if (tail==null)      head = node;
            else            tail.prev = node;
            tail = node;
            size++;
        }
        /** push node into the head of the list */
        public void push(TT node) {
            if (check) checkUnlinked( node );
            node.prev = head;
            if (head==null)      tail = node;
            else            head.next = node;
            head = node;
            size++;
        }
        /** add node to the list behind base, ie closer to tail */
        public void addAfter(TT node,TT base) {
            if (check) { checkUnlinked(node); checkLinked(base); }
            node.next = base;
            node.prev = base.prev;
            if (base.prev != null) base.prev.next = node;
            base.prev = node;
            if (tail == base) tail = node;
            size++;
        }
        /** drop the last entry off the tail of the list, null its links and return it */
        public TT drop() {
            TT node = tail;
            if (check) checkLinked(node);
            if (node!=null) {
                tail = node.next;
                node.next = node.prev = null;
                if (tail==null) head = null;
                size--;
            }
            return node;
        }
        /** pop the first entry off the head of the list, null its links and return it */
        public TT pop() {
            TT node = head;
            if (check) checkLinked(node);
            if (node!=null) {
                head = node.prev;
                node.next = node.prev = null;
                if (head==null) tail = null;
                else head.next = null;
                size--;
            }
            return node;
        }
        /** remove node from the list */
        public void remove(TT node) {
            if (check) checkLinked(node);
            if (head==node) head = node.prev;
            if (tail==node) tail = node.next;
            if (node.prev != null) node.prev.next = node.next;
            if (node.next != null) node.next.prev = node.prev;
            node.next = node.prev = null;
            size--;
        }

        /** move node to the tail, node *must* already be inserted into the list */
        public void moveToTail(TT node) {
            if (check) checkLinked(node);
            if (node != tail) {
                remove(node);
                append(node);
            }
        }
        
        
        public class Iter implements Iterator<TT> {
            public TT curr, next = Lister.this.head;
            public boolean hasNext() { return next != null; }
            public TT next() { curr = next; next = next.prev; return curr; }
            public void remove() { Lister.this.remove(curr); }
        }

        public Iterator<TT> iterator() { return new Iter(); }

        /** verify that the list is consistent, ie size is the number of elements and head leads to tail */
        public void check() {
            int c1 = 0, c2 = 0;
            TT t2 = tail, h2 = head;
            for (TT val = head; val != null; t2 = val, val = val.prev) c1++;
            for (TT val = tail; val != null; h2 = val, val = val.next) c2++;
            if (c1 != c2 || c1 != size || h2 != head || t2 != tail)
                throw rte( null,
                        "Listee.inconsistent -- size:%d %d %d, connect:%b %b",
                        size, c1, c2, h2==head, t2==tail );
        }
        /** check that the node is a legal node to be removed */
        public void checkUnlinked(TT node) {
            Simple.softAssert(
                    node.next==null && node.prev==null && node!=head,
                    "node invalid for insertion" );
        }
        /** check that the node is a legal node to be removed */
        public void checkLinked(TT node) {
            Simple.softAssert(
                    (node==head || node.next != null) && (node==tail || node.prev != null ),
                    "node invalid for removal" );
        }
        public boolean isnode(TT node) { return node==head || node.next != null; }
    }







    private static class Demo {
        public Lister<Phone> phones;
        { phones = new Lister(); }
        public Lister<Address.Link> addys = new Lister();

        public static class Phone extends Listee<Phone> { public int number; }
        public static class Blah { public String blah; }
        /** can't extend with Listee, so embed it */
        public static class Address extends Blah {
            public Link link = new Link();
            public class Link extends Listee<Link> {
                public Address self() { return Address.this; }
            }
        }
        public static Phone newPhone(int number) {
            Phone phone = new Phone();
            phone.number = number;
            return phone;
        }

        public void demo() {
            for (int ii = 0; ii < 10; ii++) phones.push(newPhone(ii));
            for (int ii = 0; ii < 10; ii++) {
                Address addy = new Address();
                addy.blah = "goodbye cruel world " + ii;
                addys.push( addy.link );
            }
            for (Phone phone : phones) System.out.format( "phone: %d\n", phone.number );
            for (Address.Link link : addys) System.out.format( "addy: %s\n", link.self().blah );
            System.out.format( "size ... phone:%d, addy:%d\n", phones.size, addys.size );
            int nt = 1 << 20;
            int [] indices = Util.colon(nt), lin = Util.colon(nt), flip = Util.colon(nt-1,-1,new int[nt]);
            Phone [] array = new Phone[nt];
            for (int ii = 0; ii < nt; ii++) array[ii] = newPhone(ii);
            for (int kk = 0; kk<9; kk++)
            for (int jj = 0; jj < 3; jj++) {
                Lister<Phone> p2 = new Lister();
                org.srlutils.Shuffler.shuffle( indices );
                Phone [] a1 = Util.select( array, indices ), a2 = array, a3 = Util.select( array, flip );
                Phone [] b1=null,b2=null;
                if (kk==0) { b1=a1; b2=a1; }
                if (kk==1) { b1=a1; b2=a2; }
                if (kk==2) { b1=a1; b2=a3; }
                if (kk==3) { b1=a2; b2=a1; }
                if (kk==4) { b1=a2; b2=a2; }
                if (kk==5) { b1=a2; b2=a3; }
                if (kk==6) { b1=a3; b2=a1; }
                if (kk==7) { b1=a3; b2=a2; }
                if (kk==8) { b1=a3; b2=a3; }
                String d1 = desc(b1,a1,a2,a3);
                String d2 = desc(b2,a1,a2,a3);
                
                double t1=0, t2=0, t3=0;
                Timer.timer.tic();
                for (int ii = 0; ii < nt; ii++) p2.append( b1[ii] );
                t1 = Timer.timer.tval();
                Phone phone = p2.head, tail = p2.tail;
                while (phone != tail) phone = phone.prev;
                if ( phone!=tail )
                    System.out.format( "not a tail: %d\n", phone.number );
                t2 = Timer.timer.tval()-t1;
                for (int ii = 0; ii < nt-1; ii++) p2.remove( b2[ii] );
                t3 = Timer.timer.tock()-t2;
                System.out.format( "mode:%5d, last element: %8d in %8.3f %8.3f %8.3f -- %s, %s\n",
                        kk, p2.head.number, t1, t2, t3, d1, d2 );
            }
        }


        public static void main(String [] args) {
            Simple.Scripts.cpufreq( "userspace", 1800000 );
            new Demo().demo();
            Simple.Scripts.cpufreq( "ondemand", 0 );
        }
    }
    
    public static <TT> String desc(TT p1,TT a1,TT a2,TT a3) {
        if (p1==a1) return "rnd";
        if (p1==a2) return "seq";
        if (p1==a3) return "rev";
        return "none";
    }

    /** an example of using Listee directly, ie as a circular linked list managed by static methods */
    public static class Direct {
        public static void main(String [] args) {
            Demo.Phone first = null, phone;
            for (int ii=0; ii < 10; ii++) first = first.append(first,Demo.newPhone(ii));
            for (phone = first; phone != null; phone = phone.next(first))
                System.out.println(phone.number);
        }
    }

}


/*

    -Xmx2g -Xms2g -XX:-UseConcMarkSweepGC  -XX:NewRatio=1 -XX:SurvivorRatio=1
    1.8ghz
    insertion and removal of 1M entries in a linked list
    sequential and reverse are much faster ... got to be cache
    random is much slower - access to the node, node.prev, and node.next are all out of order

            last       insrt     remov    ins  rem
            ----       -----     -----    ---  ---
          584716 in    0.055,    0.121 -- rnd, rnd
         1048575 in    0.055,    0.083 -- rnd, seq
               0 in    0.055,    0.083 -- rnd, rev
          878068 in    0.014,    0.150 -- seq, rnd
         1048575 in    0.014,    0.015 -- seq, seq
               0 in    0.014,    0.016 -- seq, rev
          179651 in    0.014,    0.156 -- rev, rnd
         1048575 in    0.014,    0.016 -- rev, seq
               0 in    0.014,    0.015 -- rev, rev


        iterate seems to be somewhat anywhere from 25% slower than insert to 100% faster
        in general performance is all over the place, dependent on gc and what other operations
            are being done. eq, sequential insert and remove can be 15 or 30 ms, iterate 7 or 38 ms

        conclusion: it's fast for sequential but timing is dominated by gc and jit, hard to quantify

 */

