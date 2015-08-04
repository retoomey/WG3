package org.wg3.storage;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;
import org.wg3.storage.LRUCache.LRUCacheListener;

/**
 * The data manager will handle: Loading/Offloading data from disk to ram...
 * Keep track of total data size... Other things as developed...
 *
 * This works at a raw data level, not DataType or Products or anything, at
 * least for now the purpose is to allow access to massive numbers of floats.
 * DataManager
 *
 * DataManager keeps a key counter of integer for tiles.
 *
 * @author Robert Toomey
 *
 */
public class DataManager implements LRUCacheListener<DataNode> {

    /**
     * The subdirectory we use to offload any data nodes from RAM
     */
    public final static String tempNodes = "datanodes";
    private static DataManager instance = null;
    private final static Logger LOG = LoggerFactory.getLogger(DataManager.class);
    private String myDiskLocation;
    private File myTempDir = null;
    /**
     * Our counter for returning id values for cache items We start at 'min'
     * value of int and rise....in theory if we roll over it might confuse the
     * system if by chance we're storing that full number of tiles.
     */
    private final Object myCounterSync = new Object();
    private int myCounter = Integer.MIN_VALUE;
    /**
     * Number of nodes we try to hold in RAM (RAM cache size) The LRUCache will
     * hold this many objects
     */
    private final int myRAMCacheNodeMaxCount = 300;
    private final int mySizePerNode = 10000;  // Size in floats
    /**
     * The cache for DataNode objects
     */
    LRUCache<Integer, DataNode> myRAMCache = new LRUCache<Integer, DataNode>(
            50, myRAMCacheNodeMaxCount, 500);
    /**
     * Number of bytes allocated by program
     */
    private long myAllocatedBytes = 0;
    /**
     * Number of bytes deallocated by program
     */
    private long myDeallocatedBytes = 0;
    /**
     * Number of bytes failed to allocate by program
     */
    private long myFailedAllocatedBytes = 0;
    private boolean myAddListener = true;

    private DataManager() {
        // Exists only to defeat instantiation.
        // FIXME: make GUI able to change this....
        myDiskLocation = System.getProperty("java.io.tmpdir");
        LOG.info("OS temporary directory is: " + myDiskLocation);
        try {
            myTempDir = createTempDir();
            LOG.info("Using root temp directory:" + myTempDir.getAbsolutePath());
            System.setProperty("java.io.tmpdir", myTempDir.getAbsolutePath());
            LOG.info("DataManager temp is " + myTempDir.getAbsolutePath());
        } catch (IOException e) {
             String t = System.getProperty("java.io.tmpdir");
             LOG.error("Unable to create temp directory...default is "+t);
             LOG.error("Reason: "+e.toString());
        }



        // We create a 'datacache' array...

    }

    /**
     * Get a single new key for our LRU cache
     */
    public int getNewTileKey() {
        return getNewTileKeyRange(1);
    }

    /**
     * Return the base of a range of tiles. So if a data structure could have
     * say 1000 tiles, it passes in 1000 and we return the base and add 1000.
     *
     * Note this is cheap, just a counter. Tiles are lazy created later, this
     * just reserves a key for them.
     *
     * @param needed number of tiles needed
     * @return
     */
    public int getNewTileKeyRange(int needed) {
        int base;
        synchronized (myCounterSync) {
            base = myCounter;
            myCounter += needed;
            return base;
        }
    }

    /**
     * Using this function for all creation of ByteBuffers will allow us to
     * track the memory usage better...caller should call deallocate below when
     * the ByteBuffer is set to null
     *
     * @return new ByteBuffer or null
     */
    public ByteBuffer allocate(int aSize, String who) {
        ByteBuffer bb = ByteBuffer.allocateDirect(aSize);
        if (bb != null) {
            myAllocatedBytes += aSize;
        } else {
            myFailedAllocatedBytes += aSize;
        }
        return bb;
    }

    /**
     * Anyone calling allocate above should call this to let us know it's been
     * nulled. Doesn't mean JVM or native library has Garbage collected it
     * though...just counting for debugging purposes.
     *
     * @param aSize
     * @param who
     */
    public void deallocate(int aSize, String who) {
        myAllocatedBytes -= aSize;
        myDeallocatedBytes += aSize;
    }

    public long getAllocatedBytes() {
        return myAllocatedBytes;
    }

    public long getDeallocatedBytes() {
        return myDeallocatedBytes;
    }

    public long getFailedAllocatedBytes() {
        return myFailedAllocatedBytes;
    }

    public int getNumberOfCachedItems() {
        return myRAMCache.getCacheFilledSize();
    }

    public String getTempDirName(String subname) {
        File dir = getTempDir(subname);
        return dir.getAbsolutePath();
    }

    public File getTempDir(String subname) {
        String path = getRootTempDir();
        File temporaryDir = new File(path, subname);
        if (!temporaryDir.exists()) {
            LOG.info("Creating temp directory " + temporaryDir.getAbsolutePath());
            temporaryDir.delete();
            temporaryDir.mkdir();
            temporaryDir.deleteOnExit();
        }
        return temporaryDir;
    }

    public String getRootTempDir() {
        return myTempDir.getAbsolutePath();
    }

    /**
     * FIXME: I'm going to use this dir as 'root' for the entire display, even
     * tricking others into using it
     *
     * @return
     * @throws IOException
     */
    public static File createTempDir() throws IOException {
        final File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
        File newTempDir;
        final int maxAttempts = 9;
        int attemptCount = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss"); // Note: hour is UTC time
        java.util.Date date = new java.util.Date();

        do {
            attemptCount++;
            if (attemptCount > maxAttempts) {
                throw new IOException(
                        "The highly improbable has occurred! Failed to "
                        + "create a unique temporary directory after "
                        + maxAttempts + " attempts.");
            }
            // Use date as part of temp directory for debugging.
            String extra = (attemptCount == 1) ? "" : Integer.toString(attemptCount);
            String dirName = "WG2-" + dateFormat.format(date) + extra;
            LOG.info("Trying to create directory " + dirName);
            // The name of the 'root' directory just as a random number
            //String dirName = "WG2-"+UUID.randomUUID().toString();
            newTempDir = new File(sysTempDir, dirName);
        } while (newTempDir.exists());

        if (newTempDir.mkdirs()) {
            LOG.info("Created temp dir of name " + newTempDir.getAbsolutePath());
            return newTempDir;
        }

        throw new IOException(
                "Failed to create temp dir named "
                + newTempDir.getAbsolutePath());
    }

    /**
     * Get the recommended size in floats of a tile. A tile is allowed to fudge
     * this size somewhat, but you should try to stick to it. This is not a
     * 'dimension' but raw memory, since we are used for different dimensional
     * data structures. FIXME: be able to set in GUI. This would force a purge
     * of all current tiles, including any disk storage, which would in turn
     * require purging of all products, etc. Would be a big deal, so changing
     * this other than startup probably not a good idea.
     *
     * @return tile length
     */
    public int getRecommendedNodeSize() {
        return mySizePerNode;
    }

    public int getMaxMemoryInBytes() {
        return (getRecommendedNodeSize() * 4) * myRAMCacheNodeMaxCount;
    }

    public static DataManager getInstance() {
        if (instance == null) {
            DataManager newOne = new DataManager();
            
            // Don't set instance until initialization stuff is complete
            int RAMsizeBytes = newOne.getMaxMemoryInBytes();
            float inGB = (RAMsizeBytes / 1024.0f / 1024.0f / 1024.0f);
            LOG.info("DataManager initialized, max RAM allowed is currently " + inGB + " GB");
            
            instance = newOne; // Safe now to set (for reading)
        }
        return instance;
    }
    /**
     * Get a tile from the DataManager
     *
     * @param key the 'key' of tile...
     * @return null or the found tile
     */
    private static long getCount = 0;
    private static long hitCount = 0;
    private static long printCount = 0;

    public DataNode popTile(int key, int firstSize, float background) {
        DataNode theTile;

        theTile = myRAMCache.pop(key);

        // Tile not in cache, create it and add it to cache
        if (theTile == null) {
            theTile = new DataNode(key, firstSize, background);
            theTile.loadNodeIntoRAM();
        } else {
            // LOG.debug("Cache hit POP "+theTile);
        }
        return (theTile);
    }

    public void pushTile(int key, DataNode tile) {
        if (tile != null) {
            if (myAddListener) {  // Add listener on first push....
                myRAMCache.addListener(this);
                myAddListener = false;
            }
            myRAMCache.put(key, tile);
            //LOG.debug("PUSH TILE "+tile);
        }
    }

    public DataNode getTile(int key, int firstSize, float background) {

        DataNode theTile;
        theTile = myRAMCache.get(key);

        // Tile not in cache, create it and add it to cache
        if (theTile == null) {

            theTile = new DataNode(key, firstSize, background);
            boolean success = ((theTile != null) && (theTile.loadNodeIntoRAM()));
            if (success) {
                // theTile.setCacheKey() constructor
                LOG.debug("Tile RAM Loaded: " + theTile.getCacheKey() + " read: " + success);
                myRAMCache.put(key, theTile);
                // CommandManager.getInstance().cacheManagerNotify();
            } else {
                LOG.error("Wasn't able to create/load a tile");
            }
            // Tile already found in cache
        } else {
            hitCount++;
            // LOG.debug("Tile RAM HIT: "+theTile.getCacheKey());
        }
        getCount++;
        // }
        if (printCount++ > 1000000) {
            printCount = 0;
                LOG.debug("Current tile stats: " + getCount + " with " + hitCount + " --> " + hitCount / getCount);
        }
        return (theTile);
    }

    public void dataCreated(DataStorage storage, int memoryGuess) {
        //myCurrentData.put(storage, memoryGuess);
    }

    /**
     * Used for debugging...causes purge of all RAM tiles and forces them
     * written to disk...
     */
    public void purgeAllTiles() {
        LOG.debug("Tile PURGING ALL FROM RAM ");
        myRAMCache.clear();
    }

    /**
     * Called by LRUCache when we are trimmed from the LRU. This DataManager LRU
     * is for tiles currently in RAM. So we need to purge our stuff to disk
     */
    @Override
    public void trimmed(DataNode o) {
        o.purgeNodeFromRAM();
    }
}
