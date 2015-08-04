package org.wg3.storage;

import java.nio.FloatBuffer;

/** Stores 1D arrays as nodes for the DataManager.
 * 
 * Currently stores entire array into a 'single' node.  It will have to
 * fit entirely into RAM.
 * We will have to break up a large array into multiple nodes.
 * 
 * The GUI uses this to store a GL draw data...
 * FIXME: need a count of total nodes function for looping in opengl...
 * 
 * @author Robert Toomey
 *
 */
public class Array1DfloatAsNodes extends DataStorage implements Array1D<Float> {

    private int mySize;
    private float myBackground;
    
    /** The key for this node */
    private final int myKey;
    
    /** The file key for this node */
    private final String myFileKey;
    
    /** Every time we create one, we give it a unique number */
    private static int counter = 1;

    public Array1DfloatAsNodes(int aSize, float backgroundValue) {
        counter++;
        mySize = aSize;
        myFileKey = "Array1D" + "s" + counter;
        myKey = DataManager.getInstance().getNewTileKey();
        myBackground = backgroundValue;

        // Node size based off DataManager node size.
        // Currently we just stick it all in a single node. 
        // FIXME: should probably 'break' up our data into multiple nodes
        // So if our size = 10*recommended we make 10 nodes...
        //int tileSize = DataManager.getInstance().getRecommendedNodeSize();
        // That's it.  Nodes will be created on demand as needed during set/get...
    }

    public String getFileKey(){
    	return myFileKey;
    }
    
    /** Return the raw float buffer for this array.  Currently only one.  Used
     * by GUI to get render buffer for GL data.  If in a different thread then
     * DataManager you need to call synchronize(getBufferLock()){
     * around your access of the FloatBuffer to keep DataManager from 
     * swapping/deleting stuff out from under you
     *
     */
    @Override
    public FloatBuffer getRawBuffer() {    
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        return tile.getRawBuffer();
    }

    @Override
    public Float get(int x) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with set "inline"
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        return tile.get(x);
    }

    @Override
    public void set(int x, Float value) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with get "inline"
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        tile.set(x, value);
    }

    @Override
    public int size() {
        return mySize;
    }

    @Override
    public Object getBufferLock() {
        DataNode tile = DataManager.getInstance().getTile(myKey, mySize, myBackground);
        return tile.getReadLock();
    }

    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }
}
