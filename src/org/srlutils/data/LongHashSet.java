// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.data;

import org.srlutils.Simple;
import org.srlutils.TaskTimer;
import org.srlutils.tests.CacheTest;

public class LongHashSet<K, V> {

    /**
     * Applies a supplemental hash function to a given hashCode, which
     * defends against poor quality hash functions.  This is critical
     * because ConcurrentHashMap uses power-of-two length hash tables,
     * that otherwise encounter collisions for hashCodes that do not
     * differ in lower or upper bits.
     */
    private static int hash(int h) {
        // NB: from doug lea's CHM, under the public domain "license"
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }
    static int hash(long h) {
        // this is how openjdk converts a long to an int for hashing
        // fixme - verify that the composite is a good hash
	int val = (int)(h ^ (h >>> 32));
        return hash(val);
    }

    // todo:
    //   shouldn't need to exclude the magic value
    //     at zero use something from the other half of the map to indicate magic
    //   more generally, use the position in the map to provide part of the key
    //     ie, a 29 bit table of 8-bit "keys" could reconstruct 32-bit keys
    
    public int len, mask, nbits, wrapMask, size;
    public final long magic = 0;
    public long [] keys;
    public V [] vals;

    /**
     * hashmap of longs
     * open addressing
     * power of 2 size: $nbits
     * no resizing
     * 
     */
    public LongHashSet(int $nbits) {
        nbits = $nbits;
        len = 1 << nbits;
        mask = len-1;
        keys = new long[len];
        wrapMask = 1<<(nbits-1);
        if (magic != 0) clear();
    }
    public void clear() {
        for (int ii = 0; ii < len; ii++) keys[ii] = magic;
        size = 0;
    }
    
    public int index(long key) { return hash(key)&mask; }
    
    public void put(long key) {
        if (key==magic) return;
        int index = index(key);
        while (true) {
            long k2 = keys[index];
            if (k2==magic) { keys[index] = key; size++; return; }
            if (k2==key) return;
            index = next(index);
        }
    }
    /** is k1 "after" k2 ... factoring in wrap-around */
    private boolean gte(int k1,int k2) {
        int delta = k1-k2;
        // d > thresh --> false
        // d >= 0     --> true
        // d < -thresh --> true
        // d < 0       --> false
        // check vs wrapMask instead, it's quicker than ifs (8M put/rem:  2.197 vs 2.226)
        int bits = delta & wrapMask;
        return bits==0; 
    }
    
    /** backfill the array slots */
    public void fill(int found) {

        // the last closed address
        int iiend = found;
        do iiend = next(iiend); while (keys[iiend] != magic);
        iiend = prev(iiend);

        int iiswap = iiend;
        while (found != iiend && iiswap != found) {
            // can only push a key back as far as it's original hash
            // start at the end and repeatedly sweap thru until we've swapped with the end
            long k2 = keys[iiswap];
            int i2 = index(k2);
            // is found "to the right of" i2 (handle the wrap-around)
            if (gte(found,i2)) {
                // the first swappable slot, swap and restart from the right
                keys[found] = k2;
                found = iiswap;
                iiswap = iiend;
            }
            else
                iiswap = prev(iiswap);
        }
        keys[found] = magic;
    }
    
    
    public boolean remove(long key) {
        if (key==magic) return false;
        int index = hash(key)&mask;
        while (true) {
            long k2 = keys[index];
            if (k2==magic) return false;
            if (k2==key) break;
            index = next(index);
        }
        fill(index);
        size--;
        return true;
    }
    private int next(int index) { return (index+1)&mask; }
    private int prev(int index) { return (index-1)&mask; }
    
    public V get(long key) {
        if (key==magic) return null;
        int index = index(key);
        while (true) {
            long k2 = keys[index];
            if (k2==magic) return null;
            if (k2==key) return vals[index];
            index = next(index);
        }
    }
    public boolean containsKey(long key) {
        if (key==magic) return false;
        int index = index(key);
        while (true) {
            long k2 = keys[index];
            if (k2==magic) return false;
            if (k2==key) return true;
            index = next(index);
        }
    }
    
    public static void main(String [] args) throws Exception {
        int nbits = 12;
        LongHashSet tree = new LongHashSet( nbits+2 );
        int nn = 1<<nbits;
        for (int ii = 1; ii < nn; ii++) tree.put( ii );
        for (int ii = 1; ii < nn; ii++) {
            boolean found = tree.containsKey( ii );
            boolean f2 = tree.containsKey( ii+nn );
            boolean f3 = tree.containsKey( ii-1 );
            if (!found || f2 || f3)
                Simple.softAssert(false);
            tree.remove(ii);
        }
        
        if (true) return;
        
        TaskTimer tt = new TaskTimer().config(1).init( 4, 4, true, true );
        tt.width = 5;
        tt.dec = 3;

        CacheTest.Tester test = new CacheTest.TestLH().setup( 20, 1<<22,  0 );
        tt.autoTimer( test );
    }

}
