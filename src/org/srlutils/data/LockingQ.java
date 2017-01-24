// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.srlutils.Simple;
import static org.srlutils.Simple.Exceptions.irte;


/** a wrapper for a que<TT> that uses a shared lock, allows waiting for multiple ques */
public class LockingQ<TT> {

    public final Lock lock;
    public final Queue<TT> que;
    public final LockSet set;

    public LockingQ(Queue<TT> que,LockSet set) {
        this.que = que;
        this.set = set;
        lock = set.lock;
    }

    /** signal a kcond condition: 0 --> notEmpty, 1 --> notFull */
    public void sig(int kcond) { ( kcond == 0 ? set.notEmpty : set.notFull ).signal(); }

    public      TT poll()       { lock.lock(); try {       TT tt = que.poll();    sig(1); return  tt; } finally { lock.unlock(); } }
    /** locked equiv of: return que.offer(ee) */
    public boolean offer(TT ee) { lock.lock(); try { boolean ret = que.offer(ee); sig(0); return ret; } finally { lock.unlock(); } }



    /** a lock and conditions for the ques */
    public static class LockSet {
        public final Lock lock;
        public final Condition notEmpty;
        public final Condition notFull;
        public LockingQ [] ques;

        public LockSet(Lock lock) {
            this.lock = lock;
            notEmpty = lock.newCondition();
            notFull =  lock.newCondition();
        }
        /** set the ques that should be conditioned upon */
        public LockSet setQ(LockingQ ... $ques) { ques = $ques; return this; }
        /** wait for the queues to go notEmpty */
        public void waitNotEmpty() throws InterruptedException { waitNot(notEmpty); }
        /** wait for the queues to go notFull */
        public void waitNotFull() throws InterruptedException { waitNot(notFull); }
        /** wait for notEmpty (if emptyOrFull) else notFull, wrapping interrupts in IntpRte */
        public void wrappedWait(boolean emptyOrFull) {
            try { waitNot( emptyOrFull ? notEmpty : notFull ); }
            catch (InterruptedException ex) {
                throw irte( ex );
            }
        }
        public void waitNot(Condition cond) throws InterruptedException {
             lock.lock();
             try {
                 for (Object obj = null; obj==null;) {
                     for (int ii = 0; obj==null && ii < ques.length; ii++) obj = ques[ii].que.peek();
                     if (obj==null) cond.await();
                 }
             }
             finally { lock.unlock(); }
        }

    }



    /** a class that demonstrates how to use the LockingQ ... writes to either que will awake the lockset */
    public static class Demo extends Thread {
        public LockingQ.LockSet lockset;
        public LockingQ<Integer> q1, q2;
        public int last = 0, max = 100;

        public Demo init() {
            lockset = new LockingQ.LockSet( new ReentrantLock() );
            q1 = new LockingQ( new LinkedList(), lockset );
            q2 = new LockingQ( new LinkedList(), lockset );
            lockset.setQ( q1, q2 );
            return this;
        }
        public void run() {
            while (! isInterrupted() && last < max) {
                try {
                    lockset.waitNotEmpty();
                    Integer user = q1.poll();
                    if (user==null) user = q2.poll();
                    if (user!=null) last = Math.max( last, user );
                }
                catch (InterruptedException ex) { interrupt(); return; }
            }
        }
    }


    /** send 0..100 to the demo que and wait for it to respond to each msg */
    public static void main(String [] args) {
        Demo demo = new Demo().init();
        demo.start();
        for (int ii = 0; ii <= demo.max;) {
            if (demo.last+1 < ii) { Simple.sleep(0); continue; }
            System.out.format( "adding %5d, last: %5d\n", ii, demo.last );
            LockingQ<Integer> que = (ii&1)==0 ? demo.q1 : demo.q2;
            que.offer( ii );
            ii++;
        }
        Simple.sleep(10);
        System.out.format( "last should equal max or max-1\n" );
        System.out.format( "max: %5d, last: %5d\n", demo.max, demo.last );
    }

}












