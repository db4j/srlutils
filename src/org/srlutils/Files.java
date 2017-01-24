// copyright nqzero 2017 - see License.txt for terms

package org.srlutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import static org.srlutils.Simple.Exceptions.rte;
import java.io.InputStream;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;

public class Files {

    public static void tryClose(Closeable door) { try { door.close(); } catch(Exception ex) {} }
    /** close door, wrapping any exceptions as runtime exceptions */
    public static void rteClose(Closeable door) {
        try { door.close(); }
        catch(Exception ex) { throw rte(ex,"close failed for closeable: '%s'",door); }
    }

    /** write txt to filename */
    public static void writetofile(String txt,String filename) {
        try {
            String mode = "rw";
            RandomAccessFile fid = new RandomAccessFile( filename, mode );
            fid.setLength( 0 );
            fid.write( txt.getBytes() );
            fid.close();
        } catch (Exception ex) {
            throw rte( ex, "failed to write string: %s, to file: %s", Text.summary( txt ), filename );
        }
    }
    /** read the entire file: filename, as a byte array */
    public static byte [] readbytes(String filename) {
        return readbytes(filename,0,null,0,0).bytes;
    }
    /** the result of a read */
    public static class ReadInfo {
        /** the data that was read */  public byte [] bytes;
        /** number of bytes read   */  public int num;
    }
    /**
     * wrapper for RandomAccessFile.read()
     * read len bytes from filename at position, filling bites starting at offset
     * if multiple exceptions are encountered (eg due to close() failing) throws the first only
     * @param bites if null, allocate a new array and read all available bytes
     * @param len if negative, fill bites
     * @return the byte array and number of bytes read
     * @throws RuntimeException wrapping any exceptions
     */
    public static ReadInfo readbytes(String filename,long position,byte [] bites,int offset,int len) {
        ReadInfo read = new ReadInfo();
        RandomAccessFile fid = null;
        RuntimeException rte = null;
        try {
            String mode = "r";
            // fixme:memleak -- looks like fid is leaked ...
            fid = new RandomAccessFile( filename, mode );
            if (bites==null) {
                long size = fid.length() - position;
                if (size+offset > Integer.MAX_VALUE)
                    throw new Exception(String.format(
                            "attempt to read the entirity of file '%s', size %d, is too large",filename,len));
                len = (int) size;
                bites = new byte[len+offset];
            }
            if (len < 0) len = bites.length - offset;
            fid.seek(position);
            read.num = fid.read(bites,offset,len);
            read.bytes = bites;
            return read;
        }
        catch (Exception ex) {
            rte = rte( ex, "failed to read string from file: %s", filename );
            throw rte;
        }
        finally {
            if (rte==null) rteClose(fid);
            else           tryClose(fid);
        }
    }
    /** read the entire file: filename, as a single String */
    static public String readfile(String filename) {
        return new String( readbytes( filename ) );
    }
    /** read and return bytes from stream until end of file, returns null on error */
    public static DynArray.bytes readStream(InputStream stream) {
        try {
            // fixme::efficiency -- should allow passing in either the array or the initial size
            DynArray.bytes da = new DynArray.bytes();
            da.alloc( 16 );
            while (true) {
                int len = stream.read( da.vo, da.size, da.getSpace() );
                if (len < 0) break;
                da.size += len;
                da.ensureAdditional( 1 );
            }
            return da;
        } catch (IOException ex) {
            return null;
        }
    }
    public static Object load(byte [] data) { return load( data, 0, data.length ); }
    public static Object load(byte [] data,int offset,int length) { return load(data,offset,length,null); }
    public static Object load(byte [] data,int offset,int length,ClassLoader loader) {
        if (length==-1) length = data.length - offset;
        ByteArrayInputStream stream = null;
        ObjectInputStream in = null;
        Object obj = null;
        RuntimeException rte = null;
        try {
            stream = new ByteArrayInputStream( data, offset, length );
            in = (loader==null)
                    ? new ObjectInputStream(stream)
                    : new ClassLoaderObjectInputStream(loader,stream);
            obj = in.readObject();
        } catch (Exception ex) {
            rte = rte( ex, "could not instatiate object from data: %s", data );
        } finally {
            try { in.close(); } catch (Exception ex) {}
            try { stream.close(); } catch (Exception ex) {}
        }
        if (rte != null) throw rte;
        return obj;
    }
    public static byte [] save(Object obj) {
        ObjectOutputStream out = null;
        ByteArrayOutputStream stream = null;
        byte [] data = null;
        RuntimeException rte = null;
        try {
            stream = new ByteArrayOutputStream();
            out = new ObjectOutputStream(stream);
            out.writeObject(obj);
            data = stream.toByteArray();
        } catch (Exception ex) {
            rte = rte( ex, "could not save object: %s, to byte[]", obj );
        } finally {
            try { out.close(); } catch (Exception ex) {}
            try { stream.close(); } catch (Exception ex) {}
        }
        if (rte != null) throw rte;
        return data;
    }
    public static void save(Object obj, String name) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        RuntimeException rte = null;
        try {
            fos = new FileOutputStream(name);
            out = new ObjectOutputStream(fos);
            out.writeObject(obj);
        } catch (Exception ex) {
            rte = rte( ex, "failed to save object: %s, to file: %s", obj, name );
        } finally {
            try {
                out.close();
                fos.close();
            } catch (Exception ex) {
            }
        }
        if (rte != null) throw rte;
    }
    /**
     * representation of an object read from disk.
     * including the object itself, the filename, and the size (number of bytes read from disk).
     * fields should be accessed directly.
     */
    public static class DiskObject {
        public Object object;
        public String name;
        public long size;
        public DiskObject(Object object,String name,long size) {
            this.object = object;
            this.name = name;
            this.size = size;
        }
    }
    /** load an object from file name, the returned size seems to be right, but i don't see it in the java spec */
    public static DiskObject load(String name) {
        FileInputStream fin = null;
        ObjectInputStream in = null;
        Object obj = null;
        long size = 0;
        RuntimeException rte = null;
        try {
            fin = new FileInputStream(name);
            in = new ObjectInputStream(fin);
            obj = in.readObject();
            size = fin.getChannel().position(); // hack::undocumented -- is this in the javadocs ?
        } catch (ClassNotFoundException ex) {
            rte = rte( ex, "could not instatiate object from file: %s", name );
        } catch (IOException ex) {
            rte = rte( ex, "could not read object from file: %s", name );
        } finally {
            try {
                in.close();
                fin.close();
            } catch (Exception ex) {
            }
        }
        if (rte != null) throw rte;
        return new DiskObject( obj, name, size );
    } 


    public Files() {
    }
    
}
