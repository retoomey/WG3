package org.wg3.storage;

/** Array3D hides the internals of storage of a 3D array.
 * This way we can store it sparse, full, off to disk, etc...
 * 
 * FIXME: add iterators so that sparse data can be accessed
 * without scanning an entire grid...
 * 
 * 
 * @author Robert Toomey
 */
public interface Array3D<T> {
 /** Get a value from given x and y */
    T get(int x, int y, int z);

    /** Set a value given an x and y */
    void set(int x, int y, int z, T value);

    /** Get the 'x' dimension of the array */
    int getX();

    /** Get the 'y' dimension of the array */
    int getY();
    
    /** Get the 'z' dimension of the array */
    int getZ();

    /** Return the full size of the array */
    int size();    
}
