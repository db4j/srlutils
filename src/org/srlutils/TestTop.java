// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;
import org.srlutils.btree.TestDF;
import org.srlutils.btree.TestString;

public class TestTop {
    enum Commands {
        df;
    }
    public static abstract class Testable {
        String name;
        abstract void test() throws Exception;
    }
    class BtreeDF extends Testable {
        { name = "btree.df"; }
        void test() throws Exception { TestDF.auto(null,1000000,3,2,new TestDF.Tester(new TestDF.Dtree.DF())); }
    }
    
    public static void main(String [] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "";
        if (cmd.equals("btree.df"))
            TestDF.auto(null,1000000,3,2,new TestDF.Tester(new TestDF.Dtree.DF()));
        else if (cmd.equals("btree.ss"))
            TestDF.auto(null,1000000,3,2,new TestDF.Testers(new TestDF.SSmeta.DF()));
        else if (cmd.equals("btree.if"))
            TestDF.auto(null,1000000,3,2,new TestDF.Testeri(new TestDF.Itree.IF()));
        else if (cmd.equals("btree.megamorphic"))
            TestDF.auto(null,1000000,3,2
                    , new TestDF.Tester (new TestDF. Dtree.DF())
                    , new TestDF.Testers(new TestDF.SSmeta.DF())
                    , new TestDF.Testeri(new TestDF. Itree.IF())
            );
        else if (cmd.equals("btree.testString")) {
            Simple.Scripts.cpufreqStash( 2300000 );
            new TestString.Key().randomWalk(1<<22,1<<20,10);
        }
        else if (cmd.equals("btree.testStringVal")) {
            Simple.Scripts.cpufreqStash( 2300000 );
            new TestString.Val().randomWalk(1<<22,1<<21,10);
        }
        else
            printlist();
    }
    static void printlist() {
        String [] names = new String[] {
            "btree.df",
            "btree.df.ss",
            "btree.if"
        };
        for (String name : names) System.out.println(name);
    }
}
