// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import static org.srlutils.Simple.Print.prf;
import static org.srlutils.Timer.timer;

public class TaskTimer<TT> {
    public int numTimes, numWarmup, total;
    public boolean dbg = false, byIteration = true;
    public TT config;
    /** the width of the numeric fields, the number of decimal points */
    public int width = 15, dec = 6;
    /** number of iterations, number of warmups, order the tests by iteration */
    public TaskTimer init(int ni,int nw,boolean verbose) { return( init( ni, nw, verbose, true ) ); }
    /** number of iterations, number of warmups, order the tests by iteration or by runner */
    public TaskTimer init(int ni,int nw,boolean verbose,boolean byIteration) {
        this.numTimes = ni;
        this.numWarmup = nw;
        this.dbg = verbose;
        this.byIteration = byIteration;
        total = nw + ni;
        return this;
    }
    /** set the field width and the number of decimal points, returns This */
    public TaskTimer widths(int $width,int $dec) { width = $width; dec = $dec; return this; }
    /** set the config object, returns This */
    public TaskTimer config(TT _config) { config = _config; return this; }
    /** try each runner and print the timing results */
    public void autoTimerTry(Runner ... runners) {
        tryTime(runners);
        printTime( runners );
    }
    /** run each runner and print the timing results */
    public void autoTimer(Runner ... runners) throws Exception {
        time(runners);
        printTime( runners );
    }
    /** print the timing results for each runner */
    public void printTime(Runner... runners) {
        String fmt = String.format( "%%%d.%df ", width, dec );
        String txtfmt = String.format( "%%%ds ", width );
        String fail = String.format( txtfmt, "xxxxxx" );
        String sep = " | ";

        // length of these formats (iter & fill) must match
        String iterFmt = "Iter %5d: ";
        String fillFmt = String.format( "%12s", "" );

        int tw = 0;
        for (Runner runner : runners) tw = Math.max( runner.ttl.length(), tw );
        String ttlfmt = String.format( "%%%ds ", tw );


        System.out.format( "per iteration results:\n" );
        System.out.format( "----------------------\n" );
        // print the header line
        System.out.format( fillFmt );
        for (Runner runner : runners)
            if (runner != null) {
                for (Record rec : runner.records)
                    System.out.format( txtfmt, rec.title);
                System.out.format( txtfmt, "total" );
                System.out.format( sep );
            }
        System.out.format( "\n" );

        // print the results iteration by iteration
        for (int ii = 0; ii < total; ii++ ) {
            System.out.format( iterFmt, ii );
            for (Runner runner : runners)
                if ( runner != null ) {
                    double all = 0;
                    for (Record rec : runner.records) {
                        all += rec.times[ii];
                        if ( rec.success[ii] ) System.out.format(fmt, rec.times[ii]);
                        else System.out.print(fail);
                    }
                    System.out.format(fmt, all);
                    System.out.format( sep );
                    if (! runner.success) System.out.format( "failed" );
                }
            System.out.println();
        }

        // print the average
        String txt = "Average   : ";
        for (Runner runner : runners)
            if ( runner != null ) {
                double all = 0;
                for (Record rec : runner.records) {
                    double[] vals = Util.dup(rec.times, numWarmup, numWarmup + numTimes);
                    double mean = Array.sub( vals ).stats().m;
                    all += mean;
                    txt += Util.and(rec.success)
                            ? String.format(fmt, mean)
                            : fail;
                }
                txt += String.format(fmt, all);
                txt += sep;
            }
        System.out.println( txt + "\n\n\n" );

        // print summary -- (mean,std)
        txt = "";
        txt += "Statistics:\n";
        txt += "-----------\n";
        txt += String.format( ttlfmt, "" );
        if ( runners[0] != null )
            for (Record record : runners[0].records) {
                txt += sep;
                txt += String.format( txtfmt, record.title ) + "  " + String.format( txtfmt, "" );
            }
        txt += sep;
        txt += String.format( txtfmt, "totals" ) + "  " + String.format( txtfmt, "" );
        System.out.println( txt );

        txt = "";
        for (Runner runner : runners)
            if ( runner != null ) {
                txt += String.format( ttlfmt, runner.ttl );
                Record [] recs = runner.records;
                int ns = recs.length;
                double [] all = new double[ numTimes ];
                for (int jj = 0; jj < ns; jj++) {
                    Record rec = recs[ jj ];
                    txt += String.format( sep );
                    double[] vals = Util.dup(rec.times, numWarmup, numWarmup + numTimes);
                    Util.Inplace.add( vals, all );
                    Stats.Std std = Array.sub( vals ).stats();
                    txt += Util.and(rec.success)
                            ? String.format(fmt, std.m) + "  " + String.format(fmt, std.s())
                            : fail;
                }
                txt += sep;
                Stats.Std std = Array.sub( all ).stats();
                txt += String.format(fmt, std.m) + "  " + String.format(fmt, std.s());
                txt += "\n";
            }
        System.out.print(txt);
    }

    public void tryTime(Runner ... runners) {
        try {
            time(runners);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void time(Runner ... runners) throws Exception {
        for (Runner runner : runners) if (runner != null) runner.alloc(total);
        if (byIteration) byIter( runners );
        else             byRunner( runners );
        if (dbg) prf( "\n\n" );
    }
    public void byIter(Runner ... runners) throws Exception {
        for (Runner runner : runners) runner.prenup();
        for (int ii = 0; ii < total; ii++) {
            if (dbg) prf("Iter %3d:  ", ii);
            int [] order = Shuffler.shuffle( Util.colon(runners.length) );
            for (int kk = 0; kk < runners.length; kk++ ) {
                Runner runner = runners[ order[ kk ] ];
                if (runner != null) runner.time( ii, dbg, config );
            }
            if (dbg) prf( "\n" );
            for (Runner runner : runners) runner.kiss();
        }
    }
    public void byRunner(Runner... runners) throws Exception {
        for (Runner runner : runners) {
            runner.prenup();
            prf( runner.ttl );
            for (int ii = 0; ii < total; ii++) {
                if ( runner != null ) runner.time( ii, false, config );
                runner.kiss();
                prf( "." );
            }
            if (dbg) prf( "\n" );
        }
    }

    public static class Record {
        double times [];
        boolean success [];
        String title;
        public Record init(String title) { this.title = title; return this; }
        public Record init(int ni) {
            times = new double[ ni ];
            success = new boolean[ ni ];
            return this;
        }

        public String toString() {
            double time = Util.sum( times );
            boolean cess = Util.and( success );
            return String.format("%30s: %8.3f %s", title, time, cess ? "" : "failed");
        }
    }

    public static abstract class Runner<TT> {
        int stages = 1;
        public Record [] records;
        public String ttl = this.getClass().getSimpleName();
        public String [] stageNames = null;
        boolean success;
        /** set the number of stages and the title (if non-null), returns true */
        public boolean setup(int stages,String ttl) {
            this.stages = stages;
            if (ttl != null) this.ttl = ttl;
            return true;
        }
        public void setTitle(String _ttl) { ttl = _ttl; }
        /** run the Runner as though it had been called in the timing loop, nn times using config */
        public void runAll(int nn,TT config) throws Exception {
            alloc(nn);
            prenup();
            for (int ii = 0; ii < nn; ii++) time( ii, true, config );
            kiss();
        }
        /** 
         * this should run the test once using config, but untested (2011.08.11)
         * only run finish() and kiss() if cleanup true
         */
        public void runOnce(TT config,boolean cleanup) throws Exception {
            prenup();
            this.config( config );
            this.init();
            for (int stage = 0; stage < this.stages; stage++)
                this.run( stage );
            if (cleanup) {
                success = this.finish();
                kiss();
            }
        }

        /** 
         * optional user function -- called before anything else is accessed, in the order of registration
         * alloc any resources, eg set names and stages
         */
        public void alloc() {}

        /** allocate storage for the runs -- called just before the run begins, in the order of registration */
        public void alloc(int numTimes) {
            alloc();
            records = new Record[ stages ];
            for (int ii = 0; ii < stages; ii++) {
                String name = stageNames==null ? String.format( "%s.%d", ttl, ii ) : stageNames[ii];
                records[ii] = new Record().init( name );
                records[ii].init( numTimes );
            }
        }
        /** optional user function -- called once before the beginning of the run, in the run order */
        public void prenup() {}

        /** optional user function -- called for each run before init, passes the config object */
        public void config(TT config) {}

        /** optional user function -- called once per iteration prior to the test run(), untimed */
        public void init() throws Exception {}

        /** the timed portion of the test, called once for each stage of the test */
        public abstract void run(int stage) throws Exception;

        /**
         * optional user function -- called once per run after the test run(), in the run order, untimed
         * return true to indicate success of the run
         */
        public boolean finish() throws Exception { return true; }

        /** optional user function -- called once per run, after finish, in the order provided */
        public void kiss() {}

        /** the main timing routing - calls: config, init, loops thru the run stages, finish */
        public void time(int ii,boolean dbg,TT config) throws Exception {
            if (dbg) System.out.format( "%s,", this.ttl );
            this.config( config );
            this.init();
            for (int stage = 0; stage < this.stages; stage++) {
                timer.tic();
                this.run( stage );
                this.records[stage].times[ii] = timer.tock();
                this.records[stage].success[ii] = true;
            }
            success = this.finish();
        }

    }
}












