// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;

import java.util.ArrayList;
import org.srlutils.Simple;
import org.srlutils.btree.TestDF.DFcontext;
import org.srlutils.btree.Bpage.Sheet;
import org.srlutils.btree.Btypes.Element;


/**
 * a btree subclass that uses a generic Element to access the keys and values
 * @param <KK> key type
 * @param <VV> val type
 * @param <PP> page type
 */
public abstract class Bmeta<CC extends Bmeta.Context<KK,VV,CC>,KK,VV,EE extends Btypes.Element<KK,?>>
        extends Btree.DirectMap<CC> {

    static boolean checkDel = false;

    protected EE keys;
    protected Element<VV,?> vals;

    protected void setup(EE $keys,Element<VV,?> $vals) {
        keys = $keys;
        vals = $vals;
        keys.config(0);
        vals.config( keys.size() );
        init(keys.size(),vals.size());
    }
    

    public static class Context<KK,VV,CC extends Context<KK,VV,CC>> extends Btree.Context {
        public KK key;
        public VV val;
        protected Object keydata, valdata;
        private boolean prepped;
        public CC set(KK $key,VV $val) { key = $key; val = $val; return (CC) this; }
        protected void init(Bmeta<?,KK,VV,?> map) {
            boolean prefix = Btree.modes.prefix(mode);
            if (key != null) keydata = map.keys.compareData(key,prefix,keydata);
            if (val != null) valdata = map.vals.compareData(val,prefix,null);
        }
        public CC find(Bmeta map) {
            map.findData( (CC) this);
            return (CC) this;
        }
        public VV get() { return val; }
    }
    /** read the state variables - potentially expensive */
    void initContext(CC cc) {
        super.initContext(cc);
        // fixme -- should we skip cc.init in some cases eg pop() and first()
        cc.init(this);
    }
    protected void merge(Sheet src,Sheet dst) {
        int size = size(dst,src,null,src.leaf==1,null,0);
        boolean dbg = false;
        if (dbg) {
            dump(src,"src: ");
            dump(dst,"dst: ");
        }
        // fixme -- both src and dst are sparse, sort and write directly to dst instead of using tmp ???
        Sheet tmp = newPage(true,null,true);
        int jar = dst.bs-dst.pmeta, jo = jar;
        jar = compress(src,tmp,src,jar,0);
        jar = compress(dst,tmp,dst,jar,0);
        dst.jar = jar;
        dst.del = 0;
        super.merge(src,dst);
        tmp.rawcopy(dst,jar,jar,jo-jar);
        if (dbg) {
            dump(dst,"merg:");
            int s2 = size(dst,null,null,src.leaf==1,null,0);
            System.out.format( "sizes: %d vs %d\n", size, s2 );
            System.out.println( "--------------------------------------------------------------------------------" );
        }
    }
    protected int compress(Sheet src,Sheet dst,Sheet ref,int jar,int shift) {
        int numk = (ref.leaf==1) ? ref.num : ref.num-1;
        if (keys.dynlen)
            for (int ii = numk-1; ii >= 0; ii--)
                jar -= keys.copyPayload(src,dst,ref,ii,jar+shift,jar);
        if (vals.dynlen & src.leaf==1)
            for (int ii = ref.num-1; ii >= 0; ii--)
                jar -= vals.copyPayload(src,dst,ref,ii,jar+shift,jar);
        return jar;
    }
    protected void copyPayload(Sheet src,Sheet dst) {
        int jar = dst.bs-dst.pmeta;
        jar = compress(src,dst,dst,jar,0);
        dst.jar = jar;
        int shift = src.bs-src.pmeta-jar;
        jar = compress(src,dst,src,jar,shift);
        dst.del = src.del = 0;
        // make the last key a "ghost" so that it can be propogated to the parent
        if (src.leaf==0) {
            jar -= src.del = keys.copyPayload(src,dst,src,src.num-1,jar+shift,jar);
            // fixme::hack (or kludge) -- ghosting seems prone for trouble
            // fixme::checks -- need to verify that there's space, ie that compress() won't crush the ghost
        }
        src.jar = jar+shift;
        dst.rawcopy( src, jar, src.jar, src.bs-src.pmeta-src.jar );
    }
    void check(Sheet page) {
        Simple.softAssert(page.num*page.size <= page.jar);
    }
    Sheet splitPage(Path<Sheet> path,CC context,Sheet left,Sheet right) {
        Sheet    page0       = path.page;
        Sheet          page1 = createPage(right==null,context);
        int ksplit = bisect(path,context,page1,left,right);
        return page1;
    }
    protected int bisect(Path<Sheet> path,CC cc,Sheet page1,Sheet left,Sheet right) {
        Sheet page = path.page;
        int ko=path.ko, kb=0;
        int cs = left==null 
                ? keys.size(cc.key,cc.keydata) + vals.size(cc.val,cc.valdata)
                : keys.size(left,left.num-1) + dexsize;
        int size2 = page.size()+cs+mmeta;
        int thresh = size2/2;
        int size = mmeta;
        boolean dyn = keys.dynlen | vals.dynlen & page.leaf==1;
        if (dyn) for (; kb < page.num; kb++) {
            if (kb==ko) {
                if (size+cs >= thresh) {
                    if (thresh-size < size+cs-thresh) { path.page = page1; path.ko = 0; }
                    else size += cs;
                    break;
                }
                size += cs;
            }
            int delta = keys.size(page,kb) + (left==null ? vals.size(page,kb):dexsize);
            if (size+delta >= thresh) {
                int ii = kb;
                if (size+delta-thresh <= thresh-size) { kb++; size += delta; }
                if (ko > ii) { path.page = page1; path.ko -= kb; }
                break;
            }
            size += delta;
        }
        else
            kb = bisectFixed(path,page1);
        // fixme -- if confident in the bisect size calc, move the split to the caller
        //   better modularization
        //   but for now, this allows the debug info below
        bisect(path,page,cc,page1,left,right,kb);
        boolean dbg = false;
        if (dbg & dyn) {
            int d2 = size(page1) - size;
            int d3 = keys.size(page1,0)+4;
            int diff = size(page)+page.del-size;
            int d4 = size2 - size;
            if (d2 > d3 | diff != 0 | d4 != size(page1))
                Simple.softAssert(false);
        }
        return kb;
    }
    void branch(Path<Sheet> path,Sheet page,CC cc,Sheet page1,Sheet left,Sheet right,int kb) {
        shift(path.page,path.ko);
        path.page.dexs(path.ko,left.kpage);
        int jo = page.jar;
        key(path.page,path.ko,left,left.num-1);
        if (path.ko==path.page.num-1) {
            Simple.softAssert(page==path.page);
            // ie, it's the left page and add as the final element
            page.del = jo-page.jar; // replace the ghost
            page1.dexs(0,right.kpage);
        }
        else
            path.page.dexs(path.ko+1,right.kpage);
    }
    void bisect(Path<Sheet> path,Sheet page,CC cc,Sheet page1,Sheet left,Sheet right,int kb) {
        prep(page);
        /* fixme::perf -- rather than precomputing the split-point, should be able to compute it on the fly
         * using TestDF.SSmeta.DF+Testers, dynamic bisect is 15% slower than bisectFixed (4.6s vs 4s)
         * walk thru the src left to right, copying the payload to dst
         * when dst.jar reaches threshhold, copy the rest of the key/vals to dst and continue */
        page.split(page1,kb);
        if (keys.dynlen | vals.dynlen & page.leaf==1)
            copyPayload(page,page1);
        // soup.b6 does weird things with insert and requires changes to already be committed in that case
        //   keeping it the same in srlutils.btree for easier sync in the future even tho it's a kludge
        if (right==null) {
            commit(page,cc);
            commit(page1,cc);
            insert(path.page,cc,path.ko);
        }
        else {
            if (right != null) branch(path,page,cc,page1,left,right,kb);
            commit(page,cc);
            commit(page1,cc);
        }
        checkDel(page,false);
        checkDel(page1,false);
    }
    int size(Sheet page) {
        int size2 = bs - (page.jar - page.num*page.size) - page.del;
        return size2;
    }

    void checkDel(Sheet page,boolean force) {
        if (!(checkDel | force)) return;
        int size1 = mmeta;
        if (page.leaf==1)
            for (int ii = 0; ii < page.num; ii++)
                size1 += keys.size(page,ii) + vals.size(page,ii);
        else {
            for (int ii = 0; ii < page.numkeys(); ii++)
                size1 += keys.size(page,ii) + dexsize;
            if (page.num > 0) 
                size1 += keysize+dexsize;
        }
        int size2 = bs - (page.jar - page.num*page.size) - page.del;
        Simple.softAssert(size1==size2);
    }
    
    
    
    /*
     * performance note: deferred-compress (ie sparse jar) vs shift-in-place (dense), ie deletion strategies
     *   can either leave holes in jar and compress when needed on insert
     *   or immediately shift the jar (to fill the hole) and offset all the key/value pointers into the jar
     * best case: batch removes (eg TestDF.SSmeta), dense jar is 42% slower than the sparse jar (ie deferred)
     * worst case: alternating inserts and removes at near capacity ... don't have a test case yet
     * fixme::performance -- need to test alternating inserts and removes
     */
    /**
     * compress page if needed
     * prereq: space exists to add cc or left[ko] to page
     */
    void compress(Sheet page,CC cc,int ko,Sheet left,boolean leaf) {
        if (!Bstring.ValsVar.sparseDelete) return;
        if (space(page,cc,leaf,left) < page.del) {
            checkDel(page,false);
            Sheet tmp = newPage(leaf,cc,true);
            int jo=tmp.jar, jar = page.jar = compress(page,tmp,page,tmp.jar,0);
            tmp.rawcopy(page,jar,jar,jo-jar);
            page.del = 0;
            checkDel(page,false);
        }
    }
    protected void setcc(Sheet page,CC cc,int ko) {
        check(page);
        keys.set(page,ko,cc.key,cc.keydata);
        vals.set(page,ko,cc.val,cc.valdata);
    }
    protected void getcc(Sheet page,CC cc,int ko) {
        cc.key = keys.get(page,ko);
        cc.val = vals.get(page,ko);
    }
    protected void key(Sheet p0,int k0,Sheet p1,int k1) {
        p1.rawcopy(p0,k1,k0,pkey,keysize);
        if (keys.dynlen)
            p0.jar -= keys.copyPayload(p1,p0,p0,k0,p0.jar,p0.jar);
        check(p0);
    }

    protected void merge(Sheet p1,Sheet p2,Sheet parent,int n1,int n2,int kp,CC context) {
        checkDel(p1,false);
        checkDel(p2,false);
        int ko = p1.num-1;
        merge(p2,p1);
        free(p2);
        if (p1.leaf==0)
            key(p1,ko,parent,kp);
        checkDel(p1,false);
        parent.rawcopy(parent,kp,kp+1,pdex,dexsize);
        delete(parent,kp);
        commit(p1,context);
        commit(parent,context);
        check(parent);
    }
    
    boolean overcap(Sheet page,CC cc,boolean leaf,Sheet left) {
        return space(page,cc,leaf,left) < 0;
    }
    /** return the number of bytes of free space that would remain after the insertion of cc or left */
    protected int space(Sheet page,CC cc,boolean leaf,Sheet left) {
        int delta = leaf
                ? keys.size(cc.key,cc.keydata) + vals.size(cc.val,cc.valdata)
                : keys.size(left,left.num-1) + dexsize;
        return (page.jar+page.del) - (page.size*page.num + delta);
    }
    public CC insert(KK key,VV value) {
        CC context = context().set(key,value);
        insert(context);
        return context;
    }
    public final void insert(CC context) {
        if (keys.dynlen | vals.dynlen)       insert2(context);
        else                           super.insert (context);
    }
    int delete(Sheet page,int index) {
        prep(page);
        // copy the lower jar into the gap
        // iterate the keys+vals and offset any in the lower jar
        boolean dbg = false;
        if (dbg) dump(page,"pre : ");
        if (dbg) System.out.println("--------------------------------------------------");
        {
            boolean keyed = page.leaf==1 || index < page.num-1;
            boolean kd = keys.dynlen, vd = vals.dynlen && page.leaf==1;
            Bstring.ValsFace ko = kd ? (Bstring.ValsFace) keys : null;
            Bstring.ValsFace vo = vd ? (Bstring.ValsFace) vals : null;
            if (kd && keyed)         ko.shift(page,index  ,ko,vo);
            else if (kd & index > 0) ko.shift(page,index-1,ko,vo);
            if (vd)                  vo.shift(page,index  ,ko,vo);
        }
        int ret = page.delete(index);
        check(page);
        if (dbg) dump(page,"post: ");
        if (dbg) System.exit(0);
        checkDel(page,dbg);
        return ret;
    }
    protected int size(Sheet page,Sheet other,CC cc,boolean leaf,Sheet parent,int kp) {
        int base = super.size(page,other,null,leaf,parent,kp);
        if (cc != null) {
            base += keys.size(cc.key,cc.keydata);
            base += leaf ? vals.size(cc.val,cc.valdata) : dexsize;
        }
        base += (bs-mmeta-page.jar-page.del);
        if (other != null) base += (bs-mmeta-other.jar-other.del);
        if (parent != null) base += keys.size(parent,kp) - keysize;
        return base;
    }
    public CC context() { return (CC) new Context(); }
    protected int compare(Sheet page,int index,CC data) {
        return keys.compare( data.key, page, index, data.keydata );
    }
    public VV find(KK key) {
        CC context = (CC) context().set(key,null);
        findData(context);
        return context.match ? context.val : null;
    }
    public class Range extends Btree.Range<CC> {
        public Range() { super(Bmeta.this); }
        public ArrayList<KK> keys() {
            ArrayList<KK> vals = new ArrayList();
            while (next()) vals.add(cc.key);
            return vals;
        }
        public ArrayList<VV> vals() {
            ArrayList<VV> vals = new ArrayList();
            while (next())
                vals.add(cc.val);
            return vals;
        }
    }
    public Range findRange(KK key1,KK key2) {
        return (Range) findRange(context().set(key1, null),
                context().set(key2, null));
    }
    public Range findPrefix(KK key) {
        return (Range) findPrefix(context().set(key, null));
    }

    public Range getall(CC context) { return (Range) super.getall(context); }
    public Range getall() { return (Range) super.getall(context()); }
    
    protected Range range() { return new Range(); }




    static class DF extends Bmeta<DF.Data,Double,Float,Btypes.ValsDouble>
        implements Bface<DF.Data>
    {
        public DF() { setup(new Btypes.ValsDouble(),new Btypes.ValsFloat()); }
        public static class Data extends Bmeta.Context<Double,Float,Data> implements DFcontext {
            public Data set(double key) { return super.set(key,-1f); }
            public Data set(double key,float val) { return super.set(key,val); }
            public float val() { return val; }
        }
        protected int findLoop(Sheet page,int k1,int num,int step,Data context,boolean greater) {
            for (; k1<num; k1+=step) {
                int cmp = keys.compare(context.key,page,k1,null);
                if (greater & cmp==0) cmp = 1;
                if (cmp <= 0) break;
            }
            if (step > 1)
                return findLoop(page,k1-step,num,1,context,greater);
            return k1;
        }
        public Data context() { return new Data(); }
    }    

    static class Demo {
        public static void main(String[] args) throws Exception {
            TestDF.auto( null, 1000000, 1, 3, new TestDF.Tester(new DF()) );
            TestDF.auto( null, 1000000, 1, 3, new TestDF.Tester(new DF()) );
        }
    }
}
