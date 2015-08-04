package org.wg3.storage;

import java.nio.FloatBuffer;

/**
 *  Our general 1 dimensional array.
 * 
 * @author Robert Toomey
 */
public interface Array1D<T> {
     /** Begin a batch (>1) of set/get.  This allows array to optimize caching
     */
    void begin();
    
    /** End a batch (>1) of set/get.  This allows array to optimize caching
     */
    void end();
    
    /** Get a value from given x and y */
    T get(int x);

    /** Set a value given an x and y */
    void set(int x, T value);

    /** Return the full size of the array */
    int size();

    /** Get the lock object for using the raw buffer */
    Object getBufferLock();

    /** Get a FloatBuffer of the data if possible, or return null */
    FloatBuffer getRawBuffer();
}
