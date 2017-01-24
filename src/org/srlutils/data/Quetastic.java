// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import org.srlutils.Util;
import org.srlutils.Simple;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.srlutils.Rand;
import org.srlutils.Timer;
import static org.srlutils.Simple.Exceptions.irte;

public class Quetastic {
    public Lock  fullLock = new ReentrantLock();
    public Lock emptyLock = new ReentrantLock();
    public AtomicInteger count = new AtomicInteger();
    public final Condition notEmpty, notFull;
    public int nlock = 0;
    public int capHigh = 0, capLow = 0, cap = 0;
    public boolean capLive = false;


    public Quetastic setCap(int low,int high) {
        capLive = true;
        capHigh = high;
        capLow  = low;
        return this;
    }
    
    public static enum Mode {
        Limit, Force, None;
    }
    
    public Quetastic() {
        notEmpty = emptyLock.newCondition();
        notFull  =  fullLock.newCondition();
    }

    public Quetastic init() { nlock = 0; return this; }
    
    /** 
     * add data to que, returning the prior (or resampled) count
     * if limit and the capacity is exceeded, block until space is available (throws IntpRte)
     *   the block occurs after the add
     */
    public <TT> int offer(Queue<TT> que,TT data,Mode mode) {
        // fixme:efficiency -- only need to mode.force if que.empty(), but no way to atomically 
        //   offer and check empty-ness ... could modify doug lea's code ???
        que.offer( data );
        return update( mode );
    }
    public <TT> int offer2(Queue<TT> que,TT data,Mode mode) {
        que.offer( data );
        return 0;
    }
    public int update(Mode mode) {
        int cnt = count.getAndIncrement();
        if (cnt==0 || mode==Mode.Force) {
            emptyLock.lock();
            notEmpty.signal();
            emptyLock.unlock();
            return cnt;
        }
        if (! capLive || mode != Mode.Limit) return cnt;
        if (cnt >= cap) {
            if (cnt==capHigh) cap = capLow;
            try {
                fullLock.lock();
                while ((cnt=count.get()) >= capLow) notFull.await();
            }
            catch (InterruptedException ex) { throw irte(ex); }
            finally { fullLock.unlock(); }
        }
        return cnt;
    }

    public void waitNotEmpty2(Queue que) {
        if (que.isEmpty()) {
            try {
                emptyLock.lock();
                nlock++;
                while (que.isEmpty()) notEmpty.await();
            }
            catch (InterruptedException ex) { throw irte(ex); }
            finally { emptyLock.unlock(); }
        }
    }

    public int waitNotEmpty() {
        int cnt = count.get();
        if (cnt==0) {
            try {
                emptyLock.lock();
                nlock++;
                while ((cnt=count.get())==0) notEmpty.await();
            }
            catch (InterruptedException ex) { throw irte(ex); }
            finally { emptyLock.unlock(); }
        }
        return cnt;
    }
    
    public int resolve(int ntaken) {
        int cnt = count.getAndAdd( -ntaken ), left = cnt-ntaken;
        if (capLive==false) return left;
        if (cnt >= capLow && left < capLow) {
            cap = capHigh;
            fullLock.lock();
            notFull.signalAll();
            fullLock.unlock();
        }
        return left;
    }
    
    public static class Node<SS extends Node> {
        public SS next;
        public static class Ref<TT> extends Node<Ref<TT>> {
            public TT data;
            public Ref<TT> set(TT $data) { data = $data; return this; }
        }
    }

    public static class RefStack<TT> extends Stack<Node.Ref<TT>> {
        public void push(TT data) { push( new Node.Ref().set( data ) ); }
        public TT poll() { Node.Ref<TT> node = pop(); return node==null ? null : node.data; }
    }

    public <SS extends Node<SS>> int offer(Stack<SS> que,SS node,boolean limit) {
        que.push( node );
        return update(limit ? Mode.Limit : Mode.None);
    }
    public <TT> int offer(RefStack<TT> que,TT data,boolean limit) {
        return offer( que, new Node.Ref<TT>().set( data ), limit );
    }
    
    public static class Stack<SS extends Node<SS>> {
        public AtomicReference<SS> head = new AtomicReference();

        public boolean tryPush(SS node) {
            return head.compareAndSet( node.next = head.get(), node );
        }
        public void push(SS node) {
            for (boolean done = false; !done;) done = tryPush(node);
        }
        public SS pop() {
            SS node = null;
            for (boolean done = false; !done;)
                done = ((node = head.get()) == null) || head.compareAndSet( node, node.next );
            return node;
        }
    }

    public static class TestStack {
        public Stack<IntNode> stack = new Stack();
        public int wsum, rsum;
        public Timer timer = new Timer();
        public double tock;
        
        public static class IntNode extends Node<IntNode> {
            public int data;
            public IntNode set(int $data) { data = $data; return this; }
        }
        public class Reader extends Thread {
            public void run() {
                rsum = 0;
                timer.tic();
                IntNode node = null;
                boolean done = false;
                while (!done || node != null) {
                    node = stack.pop();
                    if (node==null) continue;
                    if (node.data == -1) { done = true; continue; }
                    rsum += node.data;
                }
                tock = timer.tock();
            }
        }
        public class Writer extends Thread {
            public void run() {
                wsum = 0;
                int nn = 1<<20;
                for (int ii = 0; ii < nn; ii++) {
                    int val = Rand.irand();
                    if (val == -1) continue;
                    wsum += val;
                    stack.push( new IntNode().set(val) );
                }
                stack.push( new IntNode().set( -1 ) );
            }
        }
        public void test() {
            Reader reader = new Reader();
            Writer writer = new Writer();
            writer.start();
            reader.start();
            Simple.join( reader );
            Simple.join( writer );
            System.out.format( "Stack::verify -- sum: %12d, success: %5b, time: %8.3f\n", rsum, rsum==wsum, tock );
        }
    }
    
    public static class Test {
        public Quetastic meta;
        public Queue<Integer> q1, q2;
        public int nw = 1, nn = 1<<10, np = 1<<10;
        public int pause = -1;
        public Timer timer = new Timer();
        public boolean dbg = false;

        public Test() {
            meta = new Quetastic();
            q1 = new ConcurrentLinkedQueue();
            q2 = new ConcurrentLinkedQueue();
        }

        public void numWriters(int $nw) { nw = $nw; nn /= nw; }
        public void cap(int bits) {
            meta.setCap( 1<<bits, 1<<(bits+1) );
        }
        
        
        public class Reader extends Thread {
            public void run() {
                int numWriters = nw;
                int nt = nw * nn;
                int [] deltas = new int[ nt ];
                int [] nis = new int[ nt ];
                meta.waitNotEmpty();
                timer.tic();
                int v1 = 0, ni = 0, id = 0, v2 = 0;
                while (numWriters > 0) {
                    ni++;
                    int na = meta.waitNotEmpty();
                    Integer val = null;
                    int ii = 0;
                    for (ii = 0; ii < na; ii++) {
                        if ((val = q2.poll()) != null) {
                            nis[id] = ni;
                            deltas[id] = val - v1;
                            id++;
                        }
                        else {
                            v2 = q1.poll();
                            if (v2 == -1) numWriters--;
                            else {
                                Simple.softAssert( nw > 1 || v2-v1 == 1 );
                                v1 = v2;
                            }
                        }
                    }
                    meta.resolve( na );
                    if (dbg) System.out.format( "-" );
                }
                double toc = timer.tock();
                if (dbg) {
                    System.out.format( "\n\n" );
                    for (int ii = 0; ii < nt; ii++) {
                        System.out.format( "Reader.read -- q2:%5d,   delta:%5d,   ni: %5d\n",
                                ii, deltas[ii], nis[ii] );
                    }
                }
                System.out.format( "Reader.done -- total: %8d, max: %5d, time: %8.3f, passes: %5d, locks: %5d\n",
                        nt*np, Util.max(deltas), toc, ni, meta.nlock );
            }
        }
        public class Writer extends Thread {
            public void run() {
                int msg = 1;
                for (int ii = 0; ii < nn; ii++) {
                    for (int jj = 0; jj < np; jj++)
                        meta.offer( q1, msg++, Quetastic.Mode.Limit );
                    meta.offer( q2, msg, Quetastic.Mode.None );
                    if (pause >= 0) Simple.sleep(pause);
                    if (dbg) System.out.format( "." );
                }
                meta.offer( q1, -1, Quetastic.Mode.None );
            }
        }
        public void test() {
            meta.init();
            Reader reader = new Reader();
            reader.start();
            Writer [] ww = new Writer[ nw ];
            for (int ii = 0; ii < nw; ii++) ww[ii] = new Writer();
            for (int ii = 0; ii < nw; ii++) ww[ii].start();
            Simple.join( reader );
        }
    }
    
    public static void main(String [] args) {
        boolean forceStack = false;
        if (forceStack || args.length > 0 && "stack".equals( args[0] )) {
            TestStack ts = new TestStack();
            for (int ii = 0; ii < 100; ii++) ts.test();
            return;
        }
        Test test = new Test();
        test.cap( 10 );
        test.numWriters( 1 );
        if (args.length > 0) test.cap( Integer.parseInt( args[0] ) );
        for (int ii = 0; ii < 100; ii++) test.test();
    }
    
}
