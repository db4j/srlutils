// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.hash;

import java.util.Iterator;
import org.srlutils.Simple;

public class LongHash {
    
    // IntHash is a generated file ... edit L*ongHash instead and regen
    // see FixedHash for directions
    
    // todo
    //   tombstone (similar logic as for magic - detect location/hash mismatch) for fast deletion
    //   for modify-heavy usage, could use a pre-map to hold changes with occassional commit
    //     ie, might help cache usage
    //   precompute anti --> magic ranges
    //   allow custom hashes ???
    

    public static final class Map<VV> extends Set {
        private VV [] vals;
        public VV get(long key) {
            int index = at(key);
            return index >= 0 ? vals[index] : null;
        }
        public Map<VV> init(int $nbits) { return (Map<VV>) super.init($nbits); }
        public VV put(long key,VV val) {
            int index = put(key);
            VV prior = vals[index];
            vals[index] = val;
            return prior;
        }
        /** remove and get the key, return the associated key else null */
        public VV rag(long key) {
            int index = at(key);
            if (index < 0) return null;
            VV val = vals[index];
            fill(index);
            size--;
            return val;
        }
        final void copy(int found,int i2) { vals[found] = vals[i2]; }
        final void clear(int found) { vals[found] = null; }
        public void init() { super.init(); vals = (VV[]) new Object[len]; }
        

        public Iter values() { return new Iter(); }
        public class Iter implements Iterator<VV>, Iterable<VV> {
            int index;
            public boolean hasNext() {
                index = advance(index);
                return index < len;
            }
            public VV next() { return vals[index++]; }
            public void remove() { removeByIndex( --index ); }
            public Iterator<VV> iterator() { return this; }
        }
        
    }

    public static class Set extends FixedHash.Set {
        private long magic = 0;
        private long anti;
        private long [] keys;
        private boolean multi;
        public Set() { multi = false; }
        public Set(boolean $multi) { multi = $multi; }
        public Set init(int $nbits) { return (Set) super.init($nbits); }
        /** clear the set */
        public void clear() {
            for (int ii = 0; ii < len; ii++) keys[ii] = magic;
            for (int ii = 0; ii < len; ii++) clear(ii);
            size = 0;
        }
        /** is key in the set ? */
        public boolean contains(long key) { return at(key) >= 0; }
        /** put key into the map (if not already present) and return the index */
        public int put(long key) {
            int index;
            if (multi)
                index = ip(key);
            else {
                index = at(key);
                if (index >= 0) return index;
                index = -index-1;
            }
            if (key==magic)
                key = anti;
            keys[index] = key;
            size++;
            return index;
        }
        /** advance from index to the next non-magic index */
        public int advance(int index) {
            while (index < len && keys[index]==magic)
                index++;
            return index;
        }
        void removeByIndex(int index) {
            fill(index);
            size--;
        }
        /** remove the key from the map, return the index (negative, ie insertion point, for not present) */
        public int remove(long key) {
            int index = at(key);
            if (index < 0) return index;
            fill(index);
            size--;
            return index;
        }
        /** backfill the array slots */
        public void fill(int found) {

            // the last closed address
            int iiend = found;
            do iiend = next(iiend); while (keys[iiend] != magic);
            iiend = prev(iiend);
            
            boolean debugMagic = false;
            int thresh = len>>1;
            int iiswap = iiend;
            while (found != iiend && iiswap != found) {
                // can only push a key back as far as it's original hash
                // start at the end and repeatedly sweap thru until we've swapped with the end
                long k2 = keys[iiswap];
                int i2 = index(k2);
                if (k2==anti) {
                    // the stored key is antimagic, so the real key could be
                    // either antimagic or zero, use the location to figure it out
                    int delta = mask( iiswap-i2+len );
                    if (debugMagic)
                        System.out.format( "anti-magic hit: %d \n", iiswap );
                    if (delta >= thresh) {
                        // this has been hit both ways and seems to work, but hard to test
                        // leaving it here till more confident. see FixedHash.TestLH
                        if (debugMagic) System.out.format(
                                "----------------anti-magic hit ... woot %d ------------------------\n",
                                iiswap );
                        i2 = zero;
                    }
                }
                // is found "to the right of" i2 (handle the wrap-around)
                if (gte(found,i2)) {
                    // the first swappable slot, swap and restart from the right
                    keys[found] = k2;
                    copy(found,iiswap);
                    found = iiswap;
                    iiswap = iiend;
                }
                else
                    iiswap = prev(iiswap);
            }
            keys[found] = magic;
            clear(found);
        }
        public void init() {
            anti = FixedHash.antihash[nbits];
            keys = new long[len];
        }
        void copy(int found,int i2) {}
        void clear(int found) {}
        /** return the index or insertion point (ie, -index-1, ie strictly negative) for key */
        protected final int at(long key) {
            int index = index(key);
            if (key==magic)
                key = anti;
            while (true) {
                long k2 = keys[index];
                if (k2==magic) return -index-1;
                if (k2==key) return index;
                index = next(index);
            }
        }
        /** return the insertion point for key, ie allow multiple copies of each key */
        final int ip(long key) {
            int index = index(key);
            while (keys[index] != magic) index = next(index);
            return index;
        }
    }
    public static void main(String [] args) throws Exception {
        int nbits = 21;
        int nn = 1<<nbits;
        int first = -nn/2, last = nn/2;
        {
            Set tree = new Set().init(nbits+2);
            for (int ii = first; ii < last; ii++) tree.put( ii );
            for (int ii = first; ii < last; ii++) {
                boolean found = tree.contains( ii );
                boolean f2 = tree.contains( ii+nn );
                boolean f3 = tree.contains( ii-1 );
                if (!found || f2 || f3)
                    Simple.softAssert(false);
                tree.remove(ii);
            }
        }

        {
            Map<Integer> tree = new Map<Integer>().init(nbits+2);
            for (int ii = first; ii < last; ii++) tree.put( ii, ii );
            long sum = 0;
            long max = 0;
            Iterator<Integer> iter = tree.values();
            for (int val : tree.values())
                sum -= val;
            for (int ii = first; ii < last; ii++) {
                max += ii;
                boolean found = tree.contains( ii );
                boolean f2 = tree.contains( ii+nn );
                boolean f3 = tree.contains( ii-1 );
                if (!found || f2 || f3)
                    Simple.softAssert(false);
                int val = tree.rag(ii);
                Simple.softAssert( val==ii );
            }
            System.out.format( "sum:%d, max:%d\n", sum, max );
        }

        FixedHash.mainSet( null );
    }
    
}
