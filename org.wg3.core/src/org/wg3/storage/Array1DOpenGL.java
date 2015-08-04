package org.wg3.storage;

import java.nio.FloatBuffer;

/**
 *  Stores 1D array used by openGL drawing for the DataManager.
 *
 *   OpenGL wants a full ram piece. For now we make the entire thing fit into ram
 * when being accessed. Due to 'maximum' vertices, etc...eventually might have
 * to break it up into pieces..but this will work for now.
 *
 *   @author Robert Toomey
 *
 */
public class Array1DOpenGL extends DataStorage implements Array1D<Float> {

    /** The length of our data */
    private int mySize;
    
    /** The default background value of data */
    private float myBackground;
    
    /**
     *  The key for this node.  We only have 'one' for now...
     * FIXME:  Make us handle arrays that are larger than max OpenGL range?
     */
    private final int myKey;
    
    /** The tile we are holding and working with.  Just easier to hold onto
     * it temporarily if we are doing lots of set calls
     */
    private DataNode myWorkingTile;
  
    public Array1DOpenGL(int aSize, float backgroundValue) {
        mySize = aSize;
        myKey = DataManager.getInstance().getNewTileKey();
        myBackground = backgroundValue;
    }

    /**
     *  Return the raw float buffer for this array. Currently only one. Used by
     * GUI to get render buffer for GL data. If in a different thread then
     * DataManager you need to call synchronize(getBufferLock()){ around your
     * access of the FloatBuffer to keep DataManager from swapping/deleting
     * stuff out from under you
     *
     */
    @Override
    public FloatBuffer getRawBuffer() {

        // Note if myWorkingTile != null then we are currently setting values
        // in the array.  Since we use a late set 'offset' that always increases,
        // at worse we have more data than we need.  So this allows rendering
        // to draw partial rendering in a different thread with no issues:
        // Example... data length or 100, we have a length of 50....
        // setter thread add 200 data...length is STILL 50....draw's ok...
        // setter thread finally sets length to 250...draw's ok...
        if (myWorkingTile != null) {
            return myWorkingTile.getRawBuffer();
        }

        // Grab the cached tile.
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        return tile.getRawBuffer();
    }

    @Override
    public Float get(int x) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with set "inline"
        if (myWorkingTile != null) {
            return myWorkingTile.get(x);
        }
        
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        return tile.get(x);
    }

    /**
     *  Start a mass set of data. We can pop the tile while we work on it thus
     * avoiding crazy amounts of searching.
     *
     *  You don't have to call begin/end, but it is much faster to do so.
     */
    @Override
    public void begin() {
        myWorkingTile = DataManager.getInstance().popTile(myKey, mySize, myBackground);
    }

    /**
     *  End a mass set of data. We push the tile into data manage because we're
     * done with it and want it managed
     */
    @Override
    public void end() {
        DataManager.getInstance().pushTile(myKey, myWorkingTile);
        myWorkingTile = null;
    }

    /**
     *  Set is the monster for creating stuff.
     */
    @Override
    public void set(int x, Float value) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with get "inline"

        if (myWorkingTile != null) {
            myWorkingTile.set(x, value);
        } else {
            DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
            tile.set(x, value);
        }
    }
    
    /** Use to quickly add float data to verts.  Assumes called between begin and end,
     * checks left to caller.
     */
    public int set(int index, float[] data){
     
        myWorkingTile.set(index, data);
        return index+data.length;
    }

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public Object getBufferLock() {
        if (myWorkingTile != null) {
            return myWorkingTile.getReadLock();
        }
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        return tile.getReadLock();
    }
}
