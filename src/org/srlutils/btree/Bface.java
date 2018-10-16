// copyright nqzero 2017 - see License.txt for terms

package org.srlutils.btree;


public interface Bface<CC extends Btree.Context> {

    void clear();

    /** return a new tree-specific Context with key and val */
    CC context();

    Btree.Range<CC> findPrefix(CC c1);

    Btree.Range<CC> findRange(CC c1, CC c2);

    /** insert the key value pair in context into the map */
    void insert(CC context);

    /** delete the first element equal to context.key */
    CC remove(CC context);

    /** read the state variables - potentially expensive */
    void init(CC cc);
    public void findData(CC context);
    
}
