// copyright nqzero 2017 - see License.txt for terms

package util;

import bsh.EvalError;
import org.srlutils.Simple;





public class Console extends javax.swing.JFrame {
    bsh.util.JConsole bconsole;
    bsh.Interpreter sh;

    public static String init() {
        String txt = "";
        txt += "\n" +
                "";
        return txt;
    }
    
    
    public Console() {
        bconsole = new bsh.util.JConsole();
        this.setSize(600, 600);
        this.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        this.getContentPane().add(bconsole);
    }
    public void run() {
        try {
            System.setOut(bconsole.getOut());
            sh = new bsh.Interpreter(bconsole);
            sh.eval(init());
            sh.run();
        } catch (EvalError ex) {
            Simple.Print.prf( "%s :: %s\n", Console.class.getName(), ex );
        }
    }
    public static void main(String args[]) {
        Console console = new Console();
        console.setVisible(true);
        console.run();
    }
}













