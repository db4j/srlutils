package org.srlutils;

public class Unsafe {
    public static final sun.misc.Unsafe uu =
            (sun.misc.Unsafe) Simple.Reflect.getField(sun.misc.Unsafe.class,"theUnsafe");
}
