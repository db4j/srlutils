// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Simple {

    static private boolean useSpin = isDebug(false);
    
    /** java's assert isn't enabled normally - use this instead */
    public static void softAssert(boolean cond) {
        if (cond) return;
        if (useSpin)
            spinDebug( cond, "assert" );
        else
            // check cond again to allow cancelling the assertion for debugging
            if (!cond) throw new AssertionError( "require check failed" );
    }
    /** java's assert isn't enabled normally - use this instead */
    public static void softAssert(boolean cond,String msg,Object ... args) {
        if (cond) return;
        if (useSpin)
            spinDebug(cond, msg, args);
        else
            if (!cond) throw new AssertionError(String.format(msg, args));
    }

    // attempt to determine if debug flags have been provided
    private static boolean isDebug(boolean onException) {
        boolean isDebug = false;
        try {
            isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().contains("jdwp");
        }
        catch (Exception ex) { isDebug = onException; }
        return isDebug;
    }
    
    /** an alternative to assert ... if cond is false, spin in a sleep loop waiting for the debugger to attach */
    public static void spinDebug(boolean cond,String msg,Object ... args) {
        if (cond) return;
        boolean dbg = true;
        String m2 = "assertion failed ... spinning to wait for debugger to attach\n";
        String m3 = "\t waiting\n";
        System.out.format( m2 );
        System.out.format( "\t" + msg + "\n", args );
        Thread.dumpStack();
        for (int ii = 1; dbg; ii++) {
            if (ii % 300 == 0) System.out.format( m2 );
            if (ii % 600 == 0) System.out.format( m3 );
            sleep(10);
        }
        // nominally should throw an exception at this point
        // except that it is only reachable if the debugger has cleared dbg, so just return
    }
    public static void spinDebug2(boolean cond,String msg,Object ... args) {
        System.out.format(msg,args);
    }
    /** java's assert isn't enabled normally - use this instead */
    public static void hardAssert(boolean cond) {
        if ( !cond ) throw new Error( "require check failed" );
    }
    /** java's assert isn't enabled normally - use this instead */
    public static void hardAssert(boolean cond,String msg) {
        if ( !cond ) throw new Error( msg );
    }
    /** java's assert isn't enabled normally - use this instead */
    public static void hardAssert(Object obj1,Object obj2,String msg) {
        if ( obj1 != obj2 )
            throw new Error( msg == null ? String.format( "assertion failed: %s != %s", obj1, obj2 ) : msg );
    }
    
    public static boolean isEmpty(Object[] args) {
        return args == null || args.length == 0 || (args.length == 1 && args[0]==null);
    }
    public static boolean isEmpty(String[] args) {
        return args == null || args.length == 0 || (args.length == 1 && args[0].equals(""));
    }
    public static boolean isEmpty(String txt) {
        return txt == null || txt.equals("");
    }

    /** do nothing, ie no-operation */
    public static void nop() {}


    public static class Exceptions {

        /** wrap as a runtime exception */
        public static RuntimeException rte(Exception ex) {
            return new RuntimeException( "Exception Captured, propogating as runtime ...", ex );
        }
        /** wrap as an interrupted runtime exception */
        public static IntpRte irte(InterruptedException ex) {
            return new IntpRte( "Interrupt Captured, propogating as runtime ...", ex );
        }
        /** interrupted runtime exception */
        public static class IntpRte extends RuntimeException {
            public IntpRte(String message,InterruptedException cause) { super( message, cause ); }
        }
        /** return a new Runtime exception caused by ex, using a msg generated from the fmt/args String.format pair */
        public static RuntimeException rte(Exception ex, String fmt, Object... args) {
            String msg = String.format( fmt, args );
            return new RuntimeException( msg, ex );
        }
    }


    /** simple notify, ie: synchronized (watch) { watch.notify(); } */
    public static void notify(Object watch) {
        synchronized (watch) { watch.notify(); }
    }
    
    @SuppressWarnings("WaitWhileNotSynced")
    /** call watch.wait(), wrapping interrupts in an IntpRte. can only be called while synch'd */
    public static void wait(Object watch) {
        try { watch.wait(); }
        catch (InterruptedException ex) { throw Exceptions.irte(ex); }
    }
    
    /** join thread, wrapping (rethrowing) InterrupedException as IntpRte */
    public static void join(Thread thread) {
        try { thread.join(); }
        catch (InterruptedException ex) { throw Exceptions.irte(ex); }
    }

    /** sleep for the specified number of milliseconds. #throws IntpRte */
    public static void sleep(int millis) {
        try { Thread.sleep( millis ); }
        catch (InterruptedException ex) { throw Exceptions.irte( ex ); }
    }

    /** convenience functions for printing to System.out */
    public static class Print {
        /** System.out.print */
        public static void pr(Object txt) { System.out.print( txt ); }
        /** System.out.println */
        public static void prl(Object txt) { System.out.println( txt ); }
        /** System.out.format */
        public static void prf(String fmt,Object ... args) { System.out.format( fmt, args ); }
    }
    public static class Reflect {
        /** return a newInstance of the klass -- all exceptions are rethrown as RuntimeExceptions */
        public static <TT> TT alloc(Class<TT> klass,boolean setAccess) {
            try {
                Constructor<TT> ctor = klass.getConstructor();
                if (setAccess) ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(
                        String.format( "\nattempt to create new instance of %s failed", klass ), ex );
            }
        }
        public static String typeString(Class ... types) {
            String txt = "";
            for (Class ptype : types) txt += String.format( "%s, ", ptype.getName() );
            return txt;
        }
        public static <TT> TT newInner(Class<TT> klass,Object outer) {
            try { return klass.getConstructor( outer.getClass() ).newInstance( outer ); }
            catch (Exception ex) { throw Exceptions.rte( ex ); }
        }
        public static Object invoke(Object obj,String name,Object ... params) {
            Class klass = obj.getClass();
            Class [] types = new Class[ params.length ];
            for (int ii = 0; ii < params.length; ii++) types[ii] = params[ii].getClass();
            try {
                Method get = klass.getDeclaredMethod( name, types );
                get.setAccessible( true );
                return get.invoke( obj, params );
            } catch (Exception ex) {
                System.out.format( "Request:%s -- %s\n", name, typeString( types ) );
                for (Method meth : klass.getDeclaredMethods())
                    System.out.format( "Method:%s -- %s\n", meth.getName(), typeString( meth.getParameterTypes()) );
                throw new RuntimeException( ex );
            }
            // public Method getDeclaredMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException, SecurityException
        }
        public static sun.misc.Unsafe getUnsafe() {
            return (sun.misc.Unsafe) getField( sun.misc.Unsafe.class, "theUnsafe" );
        }
        public static Field field(Class klass,String name) {
            try { return klass.getDeclaredField(name); }
            catch (Exception e) { return null; }
        }
        public static Object getField(Class klass,String name) {
            try {
                Field f = klass.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(null);
            }
            catch (Exception e) { return null; }
        }
        /** get all the fields of klass and any superclasses assignable from filter using reflection */
        public static Field[] getFields(Class klass,Class superKlass,Class filter) {
            DynArray.Objects<Field> result = new DynArray.Objects().init(Field.class);
            for (; klass != superKlass; klass = klass.getSuperclass()) {
                Field[] fields = klass.getDeclaredFields();
                for (Field field : fields) {
                    Class fc = field.getType();
                    if (filter.isAssignableFrom( fc )) result.add( field );
                }
            }
            return result.trim();
        }
        // fixme - replace fields with getFields, and use varargs for multiple filters
        /** return all the fields in the class, up the super chain */
        public static Field [] fields(Class klass) {
            ArrayList<Field> list = new ArrayList();
            for (; klass != Object.class; klass = klass.getSuperclass()) {
                Field[] fields = klass.getDeclaredFields();
                for (Field field : fields) list.add( field );
            }
            return list.toArray( new Field[0] );
        }
        /** make a best effort to shallow copy src to dst, field by field - exceptions are rethrown as runtime */
        public static Object dup(Object src,Object dst) {
            Field [] fields = fields( src.getClass() );
            try {
                for (Field field : fields) {
                    field.setAccessible( true );
                    field.set( dst, field.get( src ) );
                }
            } catch (Exception ex) { throw new RuntimeException(ex); }
            return dst;
        }
        /** make a best attempt to set obj.(name) = val,
         * starting at the lowest subclass and working up thru the root, setting accessibility.
         * if the attempt fails, a runtime exception is thrown -- there can be a cause at each level
         * of the class hierarchy, but only one is recorded so it might not explain what really happened
         */
        public static void set(Object obj, String name, Object val) {
            Field field;
            Exception stored = null;
            for ( Class klass = obj.getClass(); klass != Object.class; klass = klass.getSuperclass() ) {
                try {
                    field = klass.getDeclaredField( name );
                    field.setAccessible( true );
                    field.set( obj, val );
                    return;
                } catch (NoSuchFieldException ex) {
                    stored = ex;
                } catch (IllegalAccessException ex) {
                    stored = ex;
                } catch (SecurityException ex) {
                    stored = ex;
                } catch (Exception ex) {
                    ex.equals( ex );
                }
            }
            throw new RuntimeException(
                    String.format( "attempt to assign %s.%s = %s failed", obj, name, val ), 
                    stored );
        }

        /** 
         * get the list of loaded native libraries
         * http://stackoverflow.com/questions/1007861/how-do-i-get-a-list-of-jni-libraries-which-are-loaded
         * had hoped to use this to prevent tomcat from double loading a dll
         *   didn't work - reloaded classes couldn't link to the dll
         * so no obvious use case, but leaving it here for now ...
         */
        public static class ClassScope {
            private static final Field libs;
            static {
                Field lib2 = null;
                try { lib2 = ClassLoader.class.getDeclaredField("loadedLibraryNames"); }
                catch (Exception ex) {}
                libs = lib2;
                libs.setAccessible(true);
            }
            public static String[] getLoadedLibraries(final ClassLoader loader) {
                try {
                    final java.util.Vector<String> libraries = (java.util.Vector<String>) libs.get(loader);
                    return libraries.toArray(new String[] {});
                }
                catch (Exception ex) { throw new RuntimeException(ex); }
            }
            public static void demo() {
                ClassLoader cl = Simple.class.getClassLoader();
                final String[] libraries = ClassScope.getLoadedLibraries( cl );
                for (String lib : libraries) System.out.format( "lib: %s\n", lib );
            }
        }    
        
        
    }
    /** simple rounding methods that work "correctly" for positive mods, and vals that don't approach maxint */
    public static class Rounder {
        /** round down val to a multiple of mod */
        public static int rdown(int val,int mod) { return (val/mod)*mod; }
        /** round down val to a multiple of mod */
        public static long rdown(long val,long mod) { return (val/mod)*mod; }
        /** round val up to mod */
        public static int rup(int val,int mod) { return ((val+mod-1)/mod)*mod; }
        /** round val up to mod */
        public static long rup(long val,long mod) { return ((val+mod-1)/mod)*mod; }
        /** divide val by 2^mod, and round up ... using shift */
        public static int divup2(int val,int mod2) {
            int mod = 1 << mod2;
            return ((val+mod-1) >> mod2);
        }
        /** divide val by mod, and round up */
        public static int divup(int val,int mod) { return ((val+mod-1)/mod); }
        /** divide val by mod, and round up */
        public static long divup(long val,long mod) { return ((val+mod-1)/mod); }
        /** the next multiple of mod */
        public static int next(int val,int mod) { return (val/mod)*mod+mod; }
        /** the next multiple of mod */
        public static long next(long val,long mod) { return (val/mod)*mod+mod; }
    }

    
    
    
    

    public static class Scripts {
        public static void script(String script) {
            try { Runtime.getRuntime().exec( script ).waitFor(); }
            catch (Exception ex) { throw new RuntimeException( "call failed: " + script ); }
        }

        /** call the cpufreq-selector OS script using the governor gov and freq khz */
        public static void cpufreq(String gov,int khz) {
            for (int ii = 0; ii < 4; ii++)
                script( String.format("cpufreq-selector -c %d -g %s -f %d",ii,gov,khz) );
        }
        
        /** 
         *  temporarily set the cpu freq to khz (userspace governor)
         *  and register a shutdown hook to automatically restore it (to ondemand) on program exit
         */
        public static void cpufreqStash(int khz) {
            cpufreq( "userspace", khz );
            Runtime.getRuntime().addShutdownHook( new Thread() {
                public void run() { cpufreq( "ondemand", 0 ); }
            } );
        }

    }
    
    
    
    
    
}













