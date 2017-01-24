// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

public class Timer {
    static final double gain = 1.0 / 1000000000.0;
    public static Timer timer = new Timer();

    public long time = 0, elapsed = 0, last = 0;
    /** toggle the stopwatch without accumulating */
    public void start() { long orig = time; time = System.nanoTime(); last = time - orig; }
    /** reset and start the stopwatch */
    public void tic() { start(); elapsed = 0; }
    /** return the elapsed time(ms) since the last tic or toc and reset the timer */
    public double tock() {
        start();
        long total = elapsed + last;
        elapsed = 0;
        return total * gain;
    }
    /** toggle the stopwatch, store and accumulate the time since last toggle, return This */
    public Timer lap() {
        start();
        elapsed += last;
        return this;
    }
    /** return the time(ms) since the last toggle */
    public double last() { return last * gain; }
    /** return the accumulated time since the last reset */
    public double total() { return elapsed * gain; }
    /** return the elapsed time(ms) since the last tic or toc */

    public double tval() { return (System.nanoTime() - time) * gain; }
    /** return and print the elapsed time(ms) since the last tic or toc */
    public double toc() {
        double delta = tock();
        System.out.format("delta: %f\n", delta);
        return delta;
    }

    
    public static class Array extends Timer {
        public long [] times;
        public Array(int nn) { times = new long[ nn ]; }
        public Array add(int ko) { lap(); times[ko] += last; return this; }
        public double get(int ko) { return times[ko] * gain; }
        public Array clear() { times = new long[ times.length ]; return this; }
    }


    
}
