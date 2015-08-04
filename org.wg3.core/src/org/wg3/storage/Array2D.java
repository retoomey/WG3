package org.wg3.storage;;

/** Array2D hides the internals of storage of a 2D array.
 * This way we can store it sparse, full, off to disk, etc...
 * 
 * FIXME: add iterators so that sparse data can be accessed
 * without scanning an entire grid...
 * 
 * 
 * @author Robert Toomey
 */
public interface Array2D<T> {

    /** Get a value from given x and y */
    T get(int x, int y);

    /** Set a value given an x and y */
    void set(int x, int y, T value);

    /** Get the 'x' dimension of the array */
    int getX();

    /** Get the 'y' dimension of the array */
    int getY();

    /** Return the full size of the array */
    int size();

    /** Return an efficient 1D float access to given col of 2Dfloat,
     * this method 'may' copy, but probably shouldn't.  Return null
     * if you can't implement this */
    Array1D<T> getCol(int i);

    /** Return an efficient 1D float access to given col of 2Dfloat,
     * this method 'may' copy, but probably shouldn't.  Return null
     * if you can't implement this */
    Array1D<T> getRow(int i);
    
    /** begin row ordered for a mass get/set of data.  Allows array to
     * optimize sub-tile loading.  This should expect a looped call with 
     * row ordered calls.  X before Y.
     */
    public void beginRowOrdered();
    
    /** being row orderer for a mass get/set of data.  Allows array to
     * optimize sub-tile loading
     */
    public void endRowOrdered();
}