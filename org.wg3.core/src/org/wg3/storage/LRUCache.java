package org.wg3.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

/**
 * LRUCache  -- Generic data structure for a Least Recently User Cache.
 * 
 * Stores a Least Recently Used Cache where access makes it less likely to be
 * deleted.  The get method causes an item to move to the top of a stack,
 * the size is trimmed to N.  When items are dropped off the bottom of the
 * stack a trimmed message is sent to any listeners.
 * 
 * <K, V> where K is the key and V the value.
 * Example:
 * LRUCache<String, Color>  -- Create a Cache where you look up Colors by String
 * LRUCache<Integer, User> -- Look up 'Users' by integer values.
 * 
 * The LRUCache uses a unique key to reference each item.
 * *
 * For key to item lookup, we use a TreeMap for O(logN) search time. For stack,
 * we use a double linked list, for O(1) operations.  Originally had 
 * an ArrayList, but 'raising' an item requires an O(N) search, then 2*O(N)
 * memory movement to gap and un-gap the item in the array.  The circular
 * linked list is much quicker here.
 * Note that having integer keys would be quicker than string keys since
 * TreeMap will use a Comparator.
 * 
 * @author Robert Toomey
 * 
 */
public class LRUCache<K, V> {

    private final static Logger LOG = LoggerFactory.getLogger(LRUCache.class);

    /**
     * A Cache Listener that gets notified when certain events happen in the
     * LRUCache. The trimmed message is nice for allowing purged objects to
     * clean themselves up.
     *
     */
    @SuppressWarnings("hiding")
	public static interface LRUCacheListener<V2> {

        /**
         * Sent when an item has been trimmed out of the cache
         */
        public void trimmed(V2 o);
    }

    /**
     * LRUCacheItems are holders for the real item. We create a double linked
     * list. This allows us to raise in O(1) time vs a slow ArrayList that has
     * to block memory search/move in 2*O(N) time
     */
    @SuppressWarnings("hiding")
    private static class LRUCacheItem<K3, V3> {

        public K3 key;
        public LRUCacheItem<K3, V3> previous;
        public V3 item;
        public LRUCacheItem<K3, V3> next;
    }

    /**
     * Interface to return true for all objects in cache wanting to be deleted
     */
    public static interface LRUTrimComparator<V4> {

        public boolean shouldDelete(V4 test);
    }
    /**
     * The listeners to changes in the LRUCache
     */
    private ArrayList<LRUCacheListener<V>> myListeners;
    /**
     * The lock for dealing with myLRUCache and myLRUStack
     */
    private final Object myLRULock = new Object();
    /**
     * The lookup map from a 'key' to the object wanted O(logN) binary search
     * here
     */
    private TreeMap<K, LRUCacheItem<K, V>> myLRUCache = new TreeMap<K, LRUCacheItem<K, V>>();
    /*
     * The 'top' item in our LRU Stack
     */
    private LRUCacheItem<K, V> myTopItem = null;
    /*
     * The 'bottom' item in our LRU Stack
     */
    private LRUCacheItem<K, V> myBottomItem = null;
    /**
     * A convenience counter of the stack size
     */
    private int myStackSize = 0;
    /**
     * The smallest setting for the cache size
     */
    private int myMinCacheSize;
    /**
     * The largest setting for the cache size
     */
    private int myMaxCacheSize;
    /**
     * The current full size of the cache. Could have fewer than this many items
     * in the cache
     */
    private int myCacheSize;

    /** Create an LRUCache with a min, max and current cache size. */
    public LRUCache(int min, int current, int max) {
        myMinCacheSize = min;
        myMaxCacheSize = max;
        myCacheSize = current;
    }

    /** Add a listener that can respond to certain cache events */
    public void addListener(LRUCacheListener<V> l) {
        if (myListeners == null) {
            myListeners = new ArrayList<LRUCacheListener<V>>();
        }
        myListeners.add(l);
    }

    /** Fire a trimmed message to any listeners */
    private void trimmed(V o) {
        if (myListeners != null) {
            Iterator<LRUCacheListener<V>> i = myListeners.iterator();
            while (i.hasNext()) {
                i.next().trimmed(o);
            }
        }
    }

    /**
     * Get an item given a key. Getting an item MOVES it up in the LRU stack, as
     * it has been referenced and is now more important than older entries
     */
    public V get(K key) {
        V theThing;
        synchronized (myLRULock) {
            LRUCacheItem<K, V> item = myLRUCache.get(key);
            if (item != null) {
                theThing = item.item;
                if (item != myTopItem) {
                    removeFromStack(item);
                    pushToStack(item);
                }
            } else {
                theThing = null;
            }
        }
        return theThing;
    }

    /**
     * Get an item given a key and remove it from our management
     */
    public V pop(K key) {
        V theThing = null;
        synchronized (myLRULock) {
            LRUCacheItem<K, V> item = myLRUCache.get(key);
            if (item != null) {
                theThing = item.item;
                remove(item);
            }
        }
        return theThing;
    }

    /**
     * Remove an item from stack, but leave it in the key lookup
     */
    private void removeFromStack(LRUCacheItem<K, V> item) {
        // Have to remove from the circular linked list....
        LRUCacheItem<K, V> prevItem = item.previous;
        LRUCacheItem<K, V> nextItem = item.next;
        if (prevItem != null) {
            prevItem.next = nextItem;
        } else {
            myTopItem = nextItem;
        }
        if (nextItem != null) {
            nextItem.previous = prevItem;
        } else {
            myBottomItem = prevItem;
        }
        item.next = item.previous = null;
    }

    /**
     * Push an item to the stack, assuming key lookup already there
     */
    private void pushToStack(LRUCacheItem<K, V> item) {
        // Push to top of stack
        LRUCacheItem<K, V> oldTop = myTopItem;
        item.next = oldTop;
        item.previous = null;
        if (oldTop != null) {
            oldTop.previous = item;
        }
        myTopItem = item;
        if (myBottomItem == null) {
            myBottomItem = item;
        }
    }

    /**
     * Private routine to remove an item. Sync done elsewhere
     */
    private void remove(LRUCacheItem<K, V> item) {

        // Remove from stack first....
        removeFromStack(item);

        // Remove from key lookup...
        myLRUCache.remove(item.key);
        myStackSize--;
    }

    /**
     * Made by me for debugging, checks everything to see if cache/stack and
     * everything is ok....
     */
    /*
    private void dumpStack(String who) {
        synchronized (myLRULock) {
            //LOG.debug("DUMP STACK: " + who);
            boolean done = false;
            int counter = 0;
            LRUCacheItem<K, V> current = myTopItem;
            while (current != null) {  // O(N)
                if (current == myBottomItem) {
                    String b = "null";
                    if (myBottomItem != null) {
                        b = Integer.toString(myBottomItem.hashCode());
                    }
                    //LOG.debug("This is Bottom " + b);
                }
                LRUCacheItem<K, V> next = current.next;
                LRUCacheItem<K, V> prev = current.previous;
                String n = "null";
                String p = "null";
                if (next != null) {
                    n = Integer.toString(next.hashCode());
                }
                if (prev != null) {
                    p = Integer.toString(prev.hashCode());
                }

                // LOG.debug(counter + " : " + current.hashCode() + " (p " + p + ")(n " + n);
                current = current.next;
                counter++;
                if (counter > myStackSize) {
                    break;
                }
            }
            if (counter == myStackSize) {
                //LOG.debug("Stack size is consistent at " + counter);
            } else {
                LOG.debug("BAD STACK SIZE " + myStackSize + " != " + counter);
            }
            if (myLRUCache.size() != counter) {
                LOG.debug("BAD TREEMAP SIZE " + counter + " != " + myLRUCache.size());
            }
        }
    }
*/
    
    /**
     * Make a copy of the current stack. Used by GUI for synchronized access to
     * our T objects. Note that the individual T objects if modified will cause
     * sync issues, but the whole point of a cache to to keep sets of repeated
     * non-modified objects
     *
     * This is O(N), but only used by the debugger to dump the stack, so we'll
     * accept it.
     */
    public ArrayList<V> getStackCopy() {
        ArrayList<V> aList;
        synchronized (myLRULock) {  // Make sure not changing while copied
            aList = new ArrayList<V>();
            LRUCacheItem<K, V> current = myTopItem;
            while (current != null) {  // O(N)
                aList.add(current.item);
                current = current.next;
            }
        }
        return aList;
    }

    /** Put a new item on top of the stack */
    public void put(K key, V putMe) {

        // Make room for the item if needed..
        trimCache(myCacheSize - 1);
        synchronized (myLRULock) {

            // Create new item
            LRUCacheItem<K, V> newTop = new LRUCacheItem<K, V>();
            newTop.key = (K) key;
            newTop.item = putMe;

            // Push to top of stack
            pushToStack(newTop);

            // Add to key lookup
            myLRUCache.put(key, newTop);
            myStackSize++;
        }
    }

    /**
     * Kinda defeats the point, but get an object without raising it within the
     * LRU stack....normally you would just call get
     *
     * @param key
     * @return
     */
    public V getWithoutRaising(K key) {
        LRUCacheItem<K, V> item = myLRUCache.get(key);
        V theThing;
        if (item != null) {
            theThing = (V) item.item;
        } else {
            theThing = null;
        }
        return theThing;
    }

    /**
     * Clear all entries from the cache
     */
    public void clear() {
        ArrayList<V> purged = new ArrayList<V>();

        synchronized (myLRULock) {
            myLRUCache.clear();

            LRUCacheItem<K, V> current = myTopItem;

            while (current != null) {  // O(N)
                LRUCacheItem<K, V> worker = current;
                current = current.next;

                // Extra 'cleanup' wipe out the links...just doing this
                // to help garbage collection out.
                worker.previous = null;
                worker.next = null;
                purged.add((V) worker.item);

                worker.item = null;

            }
            myTopItem = null;
            myBottomItem = null;
            myStackSize = 0;
        }

        // Notify on purged items outside of sync loop..
        Iterator<V> i = purged.iterator();
        while (i.hasNext()) {
            trimmed(i.next());
        }
    }

    /**
     * Set the minimum size of the cache. This is the size we trim too
     */
    public void setMinCacheSize(int min) {
        myMinCacheSize = min;
        if (myMaxCacheSize < myMinCacheSize) {
            myMaxCacheSize = myMinCacheSize;
        }
        if (myCacheSize < myMinCacheSize) {
            setCacheSize(myMinCacheSize);
        }
    }

    /**
     * Set the maximum size of the cache.
     * @param max 
     */
    public void setMaxCacheSize(int max) {
        myMaxCacheSize = max;
        if (myMinCacheSize > myMaxCacheSize) {
            myMinCacheSize = myMaxCacheSize;
        }
        if (myCacheSize > myMaxCacheSize) {
            setCacheSize(myMaxCacheSize);
        }
    }

    /**
     * Set the current size of the cache
     */
    public void setCacheSize(int size) {
        if ((size >= myMinCacheSize) && (size <= myMaxCacheSize)) {
            myCacheSize = size;
            trimCache(myCacheSize);
        }
    }

    /**
     * Get the current cache size
     */
    public int getCacheSize() {
        return myCacheSize;
    }

    /**
     * Get the current filled cache size
     */
    public int getCacheFilledSize() {
        synchronized (myLRULock) {
            //return myLRUStack.size();
            return myStackSize;
        }
    }

    /**
     * Trim cache down to the MIN_CACHE_SIZE
     */
    public void trimCache(int toSize) {

        // Don't trim less than zero
        if (toSize < 0) {
            toSize = 0;
        }
        try {
            ArrayList<V> purged = new ArrayList<V>();

            synchronized (myLRULock) {
                LRUCacheItem<K, V> current = myBottomItem;
                while (current != null) {
                    LRUCacheItem<K, V> worker = current;
                    current = current.previous;

                    if (myStackSize > toSize) {
                        purged.add(worker.item);
                        remove(worker);
                    } else {
                        break; // done
                    }
                }
            }

            // Notify on purged items outside of sync loop..
            Iterator<V> i = purged.iterator();
            while (i.hasNext()) {
                trimmed(i.next());
            }

        } catch (Exception e) {
            LOG.error("Exception purging cache element " + e.toString());
        }
    }

    /**
     * Trim all objects matching a LRUTrimComparator
     */
    public int trimCacheMatching(LRUTrimComparator<V> compare) {
        int removed = 0;
        try {
            ArrayList<V> purged = new ArrayList<V>();
            synchronized (myLRULock) {

                LRUCacheItem<K, V> current = myTopItem;
                while (current != null) {
                    LRUCacheItem<K, V> worker = current;
                    current = current.next; // should be correct even after deleting current.

                    if (compare.shouldDelete(worker.item)) {
                        purged.add(worker.item);
                        remove(worker);
                    }

                }

                // Notify on purged items outside of sync loop..
                Iterator<V> i = purged.iterator();
                while (i.hasNext()) {
                    trimmed(i.next());
                }
            }
        } catch (Exception e) {
            LOG.error("Exception purging cache for index " + e.toString());
        }
        return removed;
    }
}
