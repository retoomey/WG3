package org.wg3.storage;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

/**
 * A data node, a 'small' amount of data, usually representing a part of a
 * larger 2D, 3D array of float data. These nodes are cached to disk/moved in
 * and out of ram on demand in an LRU queue controlled by DataManager. Currently
 * nodes are created/filled when accessed with a set OR a get, which could be
 * improved by only creating empty tiles on a 'set' call that is not background.
 *
 * Node instances don't correspond with the actual data. Data may be written to
 * disk and the node disposed and then recreated later.
 *
 * FIXME: Add sparse ability?
 *
 */
public class DataNode {

    private final static Logger LOG = LoggerFactory.getLogger(DataManager.class);  // use datamanager log?
    /**
     * The key representing this data tile. Final
     */
    private final int myKey;
    /**
     * Synchronization required for reading/writing/using
     */
    //private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Object myBufferLock = new Object();
    private ByteBuffer myDataByte;
    /**
     * Background data for 'missing' data
     */
    private float myBackground = 0.0f;
    /**
     * Is data currently in RAM?
     */
    private boolean myLoaded = false;
    /**
     * Have we read in our data from disk
     */
    private boolean myWasLoadedFromDisk = false;
    /**
     * The 'true' size of the data we store (as if not sparse)
     */
    private int mySize = 0;
    /**
     * If a tile reads in from disk, and set is never called on it, then the
     * data on disk is still good..so we do not rewrite it on a purge
     *
     */
    private boolean mySetChanged = false;
    /**
     * Do we compress? makes a big difference in file size
     */
    private static final boolean myCompress = true;
    /**
     * The zero we store... FIXME: this value is unusable...humm
     */
    public static final float STORED_ZERO = -500000.0f;

    /**
     * Create a data tile with given key name
     */
    public DataNode(int key, int firstSize, float background) {
        myKey = key;
        mySize = firstSize;
        myBackground = background;
    }

    public void setBackground(float b) {
        myBackground = b;
    }

    /**
     * The tile lock, allows synchronized READ access to the raw float buffer of
     * the tile, which in general is required only by openGL since it needs a
     * vector. Use get/set routines for changing data otherwise
     */
    public Object getReadLock() {
        return myBufferLock;
        //return lock.readLock();
    }

    public Object getWriteLock() {
        return myBufferLock;
    }

    /**
     * Get the raw buffer only if loaded. GUI uses this to quickly render data
     * from a tile. You must use the getBufferLock method to synchronize around
     * using this buffer. This is so that the data will be 'frozen' and not
     * stolen out from under you by the disk offloading thread.
     * synchronized(tile.getBufferLock()){ FloatBuffer g = tile.getRawBuffer();
     * ...do something like opengl glDrawArrays(..,..,g); }
     *
     * @return
     */
    public FloatBuffer getRawBuffer() {

        //lock.readLock().lock();
        FloatBuffer fb = null;
        if (myDataByte != null) {
            myDataByte.rewind();
            fb = myDataByte.asFloatBuffer();
        }
        return fb;

    }

    /**
     * We convert background values to zero for storage and zero to
     * background...this is because we sparse the tiles by access and so we
     * assume values of zero for missing tiles...
     */
    public static float toBackground(float value, float background) {
        /* if (value == 0.0) {
         value =  STORED_ZERO;
         } else if (value == background) {
         value = 0.0f;
         }*/
        if (background == 0.0f) {
            // nada...
        } else {
            if (value == 0.0) {
                value = STORED_ZERO; // 0.0 becomes special case
            } else if (value == background) { // Say 10.0 or 0.0
                value = 0.0f;
            }
        }
        return value;
    }

    /**
     * Convert back from zero into true zero
     */
    public static float fromBackground(float value, float background) {
        // Map '0' to myBackground.  This prevents us having to
        // fill in the background for non-sparse which is slower than $%(@%@
        if (background == 0.0f) {
            // nada
        } else {
            if (value == 0.0) {
                value = background;
            } else if (value == STORED_ZERO) {
                value = 0.0f;
            }
        }
        return value;
    }

    public void set(int index, float value) {

        synchronized (getWriteLock()) {
            //synchronized(myBufferLock){
            if (myLoaded && (index < mySize)) {
                try {
                    value = toBackground(value, myBackground);
                    myDataByte.asFloatBuffer().put(index, value);
                    mySetChanged = true;
                } catch (IndexOutOfBoundsException i) {
                    LOG.error("Tried to put v[" + index + "] = " + value);
                    LOG.error("Size is " + mySize);
                }
            } else {
                if (!myLoaded) {
                    LOG.error("Can't set value on unloaded tile:" + myKey + ".  Out of memory?");
                } else {
                    LOG.error("Out of bounds. " + index + "> " + mySize + " on tile " + myKey);
                }
                // FIXME: notify DataManager, try to get more RAM? 
            }
        }
    }

    public void set(int index, float[] data) {

        synchronized (getWriteLock()) {
            //synchronized(myBufferLock){
            if (myLoaded && (index < mySize + data.length)) {
                try {
                    for (int i = 0; i < data.length; i++) {
                        data[i] = toBackground(data[i], myBackground);
                    }

                    // Different object each time, position would reset with
                    // another call to asFloatBuffer
                    FloatBuffer fb = myDataByte.asFloatBuffer();
                    fb.position(index);
                    fb.put(data, 0, data.length);  // put just calls this anyway
                    fb.position(0);
                    mySetChanged = true;
                } catch (IndexOutOfBoundsException i) {
                    LOG.error("Tried to put array v[" + index + "] += array size " + data.length);
                    LOG.error("Size is " + mySize);
                }
            } else {
                if (!myLoaded) {
                    LOG.error("Can't set value on unloaded tile:" + myKey + ".  Out of memory?");
                } else {
                    LOG.error("Out of bounds. " + index + "> " + mySize + " on tile " + myKey);
                }
                // FIXME: notify DataManager, try to get more RAM? 
            }
        }
    }

    /**
     * Get only value if myInRam. Check skipped here for speed. Caller should
     * call load before using data... Hummmm. Tile auto load?
     */
    public float get(int index) {

        synchronized (getReadLock()) {
            //synchronized(myBufferLock){
            if ((myLoaded) && (index < mySize)) {
                myDataByte.rewind();  // Probably not needed
                FloatBuffer fb = myDataByte.asFloatBuffer();

                float value = fb.get(index);

                value = fromBackground(value, myBackground);

                return value;
            }
            return myBackground;
        }
    }

    /**
     * Called by the data manager after creating us to load any old data
     */
    public boolean loadNodeIntoRAM() {
        synchronized (getWriteLock()) {
            boolean success = false;
            try {

                // LOG.info("Allocation node " + this);

                myDataByte = DataManager.getInstance().allocate(mySize * 4, "DataNode");
                // myDataByte = ByteBuffer.allocateDirect(mySize * 4);
                myDataByte.order(ByteOrder.nativeOrder());

                myLoaded = true;
                success = true;

                readFromDisk();
                mySetChanged = false;
                // Wow this is crazy slow...I _have_ to believe I'm
                // doing this wrong.  We'll map 0 to myBackground instead...
                //if (!readFromDisk()){
                // Do the initial background fill....for sparse we wouldn't need this... 
                //FloatBuffer fb = ((ByteBuffer)myDataByte.rewind()).asFloatBuffer();
                //while (fb.hasRemaining()){
                //	fb.put(myBackground);
                //}
                //myDataByte.rewind();
                //}
            } catch (OutOfMemoryError m) {
                myLoaded = false;
                LOG.error("Could not allocate " + mySize + " floats for Tile " + myKey);
            }
            return success;
        }
    }

    public boolean purgeNodeFromRAM() {
        boolean success;
        success = writeToDisk();
        synchronized (getWriteLock()) {
            if (myDataByte != null) {
                DataManager.getInstance().deallocate(mySize * 4, "DataNode");
            }
            myDataByte = null; // Delete from RAM
            myLoaded = false;  // Mark as unloaded
        }
        return success;
    }

    /**
     * Get the base path for reading/writing this node to disk
     */
    private String getBaseFilePath() {
        String path = DataManager.getInstance().getTempDirName(DataManager.tempNodes);
        path += "/" + getCacheKey() + ".data";
        return path;
    }

    /**
     * Offload tile to disk and purge ram usage of tile, called by DataManager
     * before disposing tile
     */
    private boolean writeToDisk() {

        // We're _read_ing from the data and writing to disk....
        synchronized (getReadLock()) {
            boolean success = false;
            if (myDataByte != null) {

                boolean needUpdateDisk = true;
                if (myWasLoadedFromDisk) {
                    if (!mySetChanged) {
                        needUpdateDisk = false;
                    }
                }

                if (needUpdateDisk) {
                    try {
                        // FIXME: do we need lock for the file (thread2 might be readFromDisk below)
                        String basepath = getBaseFilePath();
                        String path = myCompress ? basepath + ".gz" : basepath;

                        //FileOutputStream fout = new FileOutputStream(path);
                        //FileChannel fc = fout.getChannel();
                        OutputStream theStream;
                        try {
                            theStream = new FileOutputStream(path);
                            theStream = new GZIPOutputStream(theStream);
                        } catch (FileNotFoundException e) {

                            // Second try, will exception to the FileNotFound below
                            path = basepath;
                            theStream = new FileOutputStream(path);
                        }

                        WritableByteChannel fc = Channels.newChannel(theStream);
                        myDataByte.rewind();
                        fc.write(myDataByte);
                        fc.close();
                        success = true;
                    } catch (FileNotFoundException e) {
                        LOG.error("Can't offload Tile to disk " + myKey + " " + e);
                    } catch (IOException e) {
                        LOG.error("Can't offload Tile to disk " + myKey + " " + e);
                    }
                } else {
                    //  LOG.debug("Skip writing " + getCacheKey() + " to disk because it's the same data");
                }
            } else {
                LOG.error("offload to disk with null myDataByte? " + myWasLoadedFromDisk);
            }
            return success;
        }
    }

    /**
     * Restore data into RAM if we can
     */
    private boolean readFromDisk() {

        // We're reading from disk and _write_ing to the data
        synchronized (getWriteLock()) {
            boolean success = false;
            //LOG.info("Restore tile: "+myKey);
            try {
                String basepath = getBaseFilePath();
                String path = basepath + ".gz";
                InputStream theStream;
                try {
                    theStream = new FileInputStream(path);
                } catch (FileNotFoundException e) {

                    // Second try, will exception to the FileNotFound below
                    path = basepath;
                    theStream = new FileInputStream(path);
                }
                // FileInputStream fout = new FileInputStream(path);
                // FileChannel fc = fout.getChannel();
                if (path.endsWith(".gz")) {
                    theStream = new GZIPInputStream(theStream);
                }
                ReadableByteChannel fc = Channels.newChannel(theStream);
                myDataByte.rewind();
                fc.read(myDataByte);
                myDataByte.rewind();
                fc.close();
                success = true;
                myWasLoadedFromDisk = true;

            } catch (FileNotFoundException e) {
                // This is 'ok', may never have been written to disk...
                success = false;
            } catch (SecurityException s) {
                success = false;
            } catch (IOException e) {
                LOG.error("Disk error restoring tile " + myKey + " " + e);
            }
            return success;
        }
    }

    public int getCacheKey() {
        return myKey;
    }
}
