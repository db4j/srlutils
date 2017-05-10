// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.text.DecimalFormat;
import java.util.Arrays;
import org.apache.commons.lang3.RandomStringUtils;
import org.srlutils.DynArray;
import org.srlutils.Simple;
import org.srlutils.Types;
import org.srlutils.Util;
import org.srlutils.btree.Bpage.Sheet;

public abstract class TestString<CC extends Bmeta.Context<?,?,CC>> {

    public static class SI extends Bmeta<SI.Data,String,Integer,Bstring.ValsString> {
        { setup(new Bstring.ValsString(),new Btypes.ValsInt()); }
        public Data context() { return new Data(); }
        public static class Data extends Bmeta.Context<String,Integer,Data> {
            public Data set(String  key)            { return set(key,-1); }
            public Data set(String $key,int $val) { key = $key; val = $val; return this; }
            public int val() { return match ? val : -1; }
            public String format(int both) {
                String txt = (key==null) ? "" : key;
                if (txt.length() > 30) txt = txt.substring(0,30);
                if (both==0) 
                    return String.format( "%-30s", txt);
                return String.format( "%-30s --> %5d", txt, val);
            }
        }
    }
    public static class LS extends Bmeta<LS.Data,Long,String,Btypes.ValsLong> {
        { setup(new Btypes.ValsLong(),new Bstring.ValsString()); }
        public Data context() { return new Data(); }
        public static class Data extends Bmeta.Context<Long,String,Data> {
            public Data set(long  key)            { return set(key,""); }
            public Data set(long $key,String $val) { key = $key; val = $val; return this; }

            public String format(int both) {
                String txt = (val==null) ? "" : val;
                if (txt.length() > 30) txt = txt.substring(0,30);
                if (both==0) 
                    return String.format("%8d"          , key);
                return     String.format("%8d --> %-30s", key, txt);
            }
        }
    }
    
    Bmeta<CC,?,?,?> map;
    CC cc;
    org.srlutils.rand.Source r1 = new org.srlutils.rand.Source(), r2 = new org.srlutils.rand.Source();
    { 
        Long seed = null;
        r1.setSeed(seed,true);
    }
    public static class IJ extends Btree.DirectMap<IJ.Data> {
        { init(Types.Enum._int.size,Types.Enum._long.size); }
        public void setcc(Sheet page,Data cc,int ko) { page.put(pkey,ko,cc.key);  page.put(pval,ko,cc.val);  }
        public void getcc(Sheet page,Data cc,int ko) { cc.key=page.geti(pkey,ko); cc.val=page.getl(pval,ko); }
        int key(Sheet page,int index) { return page.geti(pkey,index); }
        protected int compare(Sheet page,int index,Data data) { return Butil.compare(data.key,key(page,index)); }
        protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
            for (; k1<num; k1+=step) {
                int cmp = compare( page, k1, context );
                if (greater & cmp==0) cmp = 1;
                if (cmp <= 0) break;
            }
            if (step > 1)
                return findLoop(page,k1-step,num,1,context,greater);
            return k1;
        }
        public Data context() { return new Data(); }
        public static class Data extends Btree.Context {
            public int key;
            public long val;
            public Data set(int $key,long $val) { key = $key; val = $val; return this; }
        }
    }
    DynArray.ints kdels = new DynArray.ints();
    java.util.TreeMap<Integer,Long> ktree = new java.util.TreeMap();
    DynArray.longs keys = new DynArray.longs();
    boolean kib = true;
    IJ kbree = new IJ();
    IJ.Data kcc = kbree.context();
    { kbree.init(kcc); }
    int target, num, decade = 1<<10;
    double limit = .999;

    public int delta() {
        int tb = 1<<this.tb;
        int delta = (int) Util.Scalar.bound(-limit*tb, limit*tb, 1.0*(target-num)/target*tb );
        return delta;
    }
    
    // 0:look, 1:verify, 2:insert, 3:remove
    public int type(long magic) {
        int type = (int) ((magic & mtype) >>> nseed+nlen);
        int delta = delta();
        if (type < thresh[0])       return 0;
        if (type < thresh[1])       return 1;
        if (type < thresh[2]+delta) return 2;
        return 3;
    }
    public long seed(long magic) { return magic & mseed; }
    public int length(long magic) {
        long len = magic & mlen >>> nseed;
        return min + (int) (len % max);
    }

    int charType = 2;
    int max = 256, min = 4;
    int nseed = 32, nlen = 16, ntype = 16; // low to high
    long mseed = ((1L<<nseed)-1);
    long  mlen = ((1L<<nlen )-1) << nseed;
    long mtype = ((1L<<ntype)-1) << nseed+nlen;
    int tb = ntype-2;
    int [] thresh = new int[] { 1<<tb, 2<<tb, 3<<tb };
    DynArray.chars alphabet = new DynArray.chars();
    {
        for (char ii = 'a'; ii <= 'z'; ii++) alphabet.add(ii);
        for (char ii = 'α'; ii <= 'ω'; ii++) alphabet.add(ii);
        for (char ii = 'ᵃ'; ii <= 'ᵛ'; ii++) alphabet.add(ii);
        for (char ii = '⒜'; ii <= '⒵'; ii++) alphabet.add(ii);
        for (char ii = '耀'; ii <= '耲'; ii++) alphabet.add(ii);
    }

    /** generate and return a random string from key */
    public String sprout(long key) {
        int len = (int) ((key & mlen) >>> nseed);
        long seed = key & mseed;
        if (charType==2) {
            r2.setSeed(seed);
            return RandomStringUtils.random(len,0,0,false,false,null,r2.prng);
        }
        char [] chars;
        if (charType==1) {
            int [] kv = r2.setSeed(seed).rand(new int[len],0,alphabet.size);
            chars = Util.select(alphabet.vo,kv);
        }
        else
            chars = r2.setSeed(seed).rand(new char[len],'a','z');
        String string = new String(chars);
        return string;
    }
    /**  return a key consisting of a seed and a non-zero length */
    public long key(long magic) {
        long len = length(magic);
        long seed = seed(magic);
        long key = len << nseed | seed;
        return key;
    }
    boolean force = false;
    public void rotate() {
        int size = num + kdels.size;
        if (!treed & num*20 < size) {
            treed = true;
            ktree.clear();
            kbree.clear();
            kbree.init(kcc);
            for (int ii = 0; ii < keys.size; ii++) {
                if (!kib|force)  if (keys.vo[ii] != 0) ktree.   put(        ii,keys.vo[ii]);
                if ( kib) if (keys.vo[ii] != 0) kbree.insert(kcc.set(ii,keys.vo[ii]));
            }
            System.out.println("rotate to treed");
        }
        else if (treed & num*5 > size) {
            treed = false;
            for (int ii = 0; ii < size; ii++) keys.vo[ii] = 0;
            if (kib)
            for (Btree.Range<IJ.Data> range = kbree.getall(kcc); range.next();)
                keys.set(kcc.key,kcc.val);
            else
            for (java.util.Map.Entry<Integer,Long> pair : ktree.entrySet())
                keys.set(pair.getKey(),pair.getValue());
            System.out.println("rotate to xxxxx");
        }
    }
    public void del(int ko) {
        long key = get(ko);
        if (useTotal) totalBytes -= sprout(key).getBytes().length;
        kdels.add(ko);
        if (treed & kib) kbree.remove(kcc.set(ko,0));
        if (treed & (!kib | force)) ktree.remove(ko);
        else keys.set(ko,0);
        num--;
        rotate();
    }
    boolean treed = false;
    public long get(int ko) {
        long v1;
        if (kib & treed) { kbree.findData(kcc.set(ko,0)); v1 = kcc.val; }
        else v1 = treed ? ktree.get(ko) : keys.get(ko);
        if (force && treed && v1 != ktree.get(ko))
            Simple.softAssert(false);
        return v1;
    }
    boolean useTotal = false;
    int totalBytes = 0;
    /** add key to the stored keys array, returning the index */
    public int add(long key) {
        int ko = (kdels.size==0) ? num : kdels.vo[--kdels.size];
        if (treed & kib)            kbree.insert(kcc.set(ko,key));
        if (treed & (!kib | force)) ktree.   put(        ko,key);
        else keys.set(ko,key);
        num++;
        rotate();
        if (useTotal) totalBytes += sprout(key).getBytes().length;
        return ko;
    }
    public int last() {
        if (treed & kib) {
            kbree.last(kcc);
            return kcc.key;
        }
        if (treed)
            return ktree.lastKey();
        for (int ii = keys.size; --ii >= 0;)
            if (keys.vo[ii] > 0) return ii;
        return 0;
    }
    /**
     * convert the seed portion of magic to an index into the stored keys array
     * if magic is an index+1, will return the next key in the set
     * ie, it acts like an iterator
     */
    public int index(long magic) {
        long seed = magic & ~mtype;
        int size = num + kdels.size;
        int ko = (int) (seed % size);
        if (kib & treed) {
            kcc.mode = Btree.modes.gte;
            kbree.findData(kcc.set(ko,0));
            if (!kcc.match) kbree.findData(kcc.set(0,0));
            if (force) Simple.softAssert(kcc.key==ktceil(ko));
            return kcc.key;
        }
        if (treed)
            return ktceil(ko);
        while (keys.vo[ko]==0)
            if (++ko==keys.size) ko = 0;
        return ko;
    }
    int ktceil(int ko) {
        Integer k2 = ktree.ceilingKey(ko);
        k2 = (k2==null) ? ktree.ceilingKey(0) : k2;
        return k2;
    }
    /** choose a "random" index to the stored keys and return the path in the map to that key */
    abstract Btree.Path getPath(int ko);

    org.srlutils.Timer timer = new org.srlutils.Timer();
    int [] counts = new int[4];
    public int timedLoop(int size,int jj,int jo,int nn) {
        target = size;
        Arrays.fill(counts,0);
        timer.start();
        long magic = 0;
        for (int ii = 0; ii < nn; ii++, jo++) {
            magic = process(jo);
        }
        double time = timer.tock();
        check(jo);
        int [] info = map.getInfo();
        double ratio = 100.0*num/Math.max(last(),1);
        DecimalFormat formatter = new DecimalFormat("0.00E0");
        int nb = 2*(totalBytes + num*7);
        String total = useTotal ? formatter.format(nb) + String.format(" %3d",nb/num) : "na";
        String sizestr = formatter.format(size) + "/" + formatter.format(num);
        System.out.format(
                "%5d -- %s delta: %6d time:%8.3f -- %5.1f%%, %3d %5d %3d -- %5.1f%%, %s\n",
                jj, sizestr, delta(), time, 100.0*counts[2]/nn, info[0], info[1], info[2], ratio, total);
        return jo;
    }
    public void fixedWalk(int operationsPerPass) {
        cc = map.context();
        map.init(cc);
        int jj=0, jo=0;
        jo = timedLoop(1000000,jj,jo,operationsPerPass);
        jo = timedLoop(     10,jj,jo,operationsPerPass);
        map.dump(cc);
        jo = timedLoop(1000000,jj,jo,operationsPerPass);
        jo = timedLoop(     10,jj,jo,operationsPerPass);
        map.dump(cc);
    }
    public void randomWalk(int operationsPerPass,int maxSize,int numPasses) {
        cc = map.context();
        map.init(cc);
        int jj=0, jo=0;
        for (jj=0; jj < numPasses; jj++) {
            int size = r1.nextInt(0,maxSize);
            jo = timedLoop(size,jj,jo,operationsPerPass);
        }
    }
    public abstract void check(long key1,String sprout1);
    public void check(int jo) {
        map.verify(cc);
        for (int ii=0, ko=0; ii < num; ii++, ko++) {
            ko = index(ko);
            getPath(ko);
        }
    }
    public static class Key extends TestString<SI.Data> {
        { map = new SI(); }
        Btree.Path getPath(int ko) {
            long key = get(ko);
            String sprout = sprout(key);
            cc.mode = Btree.modes.gte;
            Btree.Path path = map.findPath(cc.set(sprout),true);
            int next = 0;
            while (true) {
                check(key,sprout);
                if (cc.val==ko) return path;
                path = map.next(path,cc);
                map.getPath(path,cc);
                next++;
            }
        }
        /** verify that the context matches key1 */
        public void check(long key1,String sprout1) {
            Simple.softAssert(cc.match);
            int found = cc.val;
            long key2 = get(found);
            if (key2 != key1) {
                String sprout2 = sprout(key2);
                Simple.softAssert(sprout1.equals(sprout2));
            }
        }
        SI.Data ccset(long key,int index) { return cc.set(sprout(key),index); }
    }
    public static class Val extends TestString<LS.Data> {
        { map = new LS(); }
        Btree.Path getPath(int ko) {
            long key = get(ko);
            String sprout = sprout(key);
            cc.mode = Btree.modes.gte;
            Btree.Path path = map.findPath(cc.set(key),true);
            check(key,sprout);
            return path;
        }
        public void check(long key1,String sprout1) {
            Simple.softAssert(cc.match);
            String sprout2 = cc.val;
            Simple.softAssert(sprout1.equals(sprout2));
        }
        LS.Data ccset(long key,int index) { return cc.set(key,index >= 0 ? sprout(key) : ""); }
    }
    abstract CC ccset(long key,int index);
    public long process(int jo) {
        long magic = r1.nextLong();

        int type = type(magic);
        counts[type]++;
        // look verify insert remove
        if (type==0) {
            long key = key(magic);
            String sprout = sprout(key);
            cc.mode = Btree.modes.eq;
            map.findData(ccset(key,-1));
            if (cc.match)
                check(key,sprout);
        }
        if (type==1 & num > 0)
            getPath(index(magic));
        if (type==2) {
            long key = key(magic);
            int index = add(key);
            cc.mode = Btree.modes.gt;
            map.insert(ccset(key,index));
        }
        if (type==3 & num > 0) {
            int ko = index(magic);
            Btree.Path path = getPath(ko);
            map.remove(path,cc,false);
            del(ko);
        }
        return magic;
    }
    

    public static class Demo {
        public static void main(String [] args) {
            Simple.Scripts.cpufreqStash( 2300000 );
            TestString test = args.length > 0 ? new Val() : new Key();
            test.randomWalk(1<<22,1<<22,10);
        }
    }
}



/*
 *  general scheme is have a random source r1 --> metakeys
 *  each metakey --> an action: put, look or remove, a seed and a length
 *  a second random source r2, r2.seed(metakey.seed)
 *  for puts:
 *    r2 --> data, a new random char[length]
 *    insert (data,keys.length) tuple into the map and append metakey to stored keys array
 *  for looks:
 *    map to index into keys, then use that metakey to regenerate and data and lookup in map
 *  for removes:
 *    map index into keys, use metakey to regenerate data and remove from map
 *    zero out entry in the key array and add the loc to the array of deleted keys
 *      which are recycled before 
 * 
 * 
 *  results:
 *    runs to completion: 1000 loops of 2^23 operations, target sizes up to 2^23
 *    strings are 4-260 chars between 'a' and 'z'
 *    initial loops run in 46-49 seconds, after loop 750 they started taking longer 46-80 seconds
 *    after 990 up to 100 seconds, selected loops follow:
 * 
 *   11 -- target: 7949362 final: 4382334 delta:   7351 time:  46.981 -- 2095270 2097882 3336065 859391 
 *   12 -- target: 3416170 final: 3698870 delta:  -1355 time:  47.498 -- 2099730 2095526 1754944 2438408 
 *   13 -- target: 5088535 final: 4477006 delta:   1968 time:  48.164 -- 2097760 2095416 2486784 1708648 
 *  220 -- target:   23926 final:   23898 delta:     19 time: 167.220 -- 2095667 2099558 1115274 3078109 
 *  818 -- target:     386 final:     392 delta:   -254 time:3585.958 -- 2096353 2098169  639767 3554319 
 *  996 -- target: 6975256 final: 4819906 delta:   5062 time:  70.117 -- 2096106 2096147 2989162 1207193 
 *  997 -- target: 6912685 final: 5775360 delta:   2695 time:  76.918 -- 2096928 2095322 2575906 1620452 
 *  998 -- target: 7382835 final: 6470898 delta:   2023 time:  88.759 -- 2098236 2097056 2444427 1748889 
 * 
 *  total runtime 2300 minutes
 * 
 * 
 * 
 */
