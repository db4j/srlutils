// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;


public class Text {
    /** format a summary of the string txt, ie similar to what toString does for Object */
    public static String summary(String txt) {
        if (txt==null) return "Null";
        else return String.format( "%s @ %x, length %d",
                txt.getClass().getName(), Integer.toHexString(txt.hashCode()), txt.length() );
    }

}
