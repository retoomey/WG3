package org.wg3.storage;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

/**
 *   Breaks a 2D array down into a grid where each DataManager node is a square
 * with sides ~= sqrt(nodesize). This works well for data such as LatLonGrids
 * since the user is usually over a subsection of the LatLonGrid (think graph
 * paper). Well, ok..in the **GUI** they are.
 *
 *   For doing an algorithm, it might be more efficient to store nodes as 'rows'
 * of data instead of squares, since less node swapping will occur. (You're
 * likely to iterate over the grid this way..) In other words, store the data in
 * the way you're most likely to access it to maximize cache hits.
 *
 *   @author Robert Toomey
 *
 */
public class Array2DfloatAsTiles extends DataStorage implements Array2D<Float> {

	@SuppressWarnings("unused")
    private final static Logger LOG = LoggerFactory.getLogger(Array2DfloatAsTiles.class);
	
    /** The full number of possible x values in floats */
    private final int myX;
    /** The full number of possible y values in floats */
    private final int myY;
    /** The number of possible tiles in the X */
    private final int myNumberX;
    /** The number of possible tiles in the Y */
    private final int myNumberY;
    /** The default background of the array */
    private float myBackground;
    /**
     *  The 'side' of a 2D tile, which we will set to sqrt of the DataManager
     * tile size
     */
    private final int mySide;
    /**
     *  The 'side' squared
     */
    private final int mySideSquared;
    /** A pre-loaded row of tiles for mass 'set' calls.  Speeds up loading */
    private ArrayList<DataNode> myWorkingTiles = null;
    private int myCurrentOrderedRow = 0;
    /** The base key for our tiles */
    private int myKeyBase;

    // Initializer block...Shared by all constructors, called before constructors
    {
        // Tile size based off DataManager node size
        // int tileSize = DataManager.getInstance().getRecommendedNodeSize();
        // mySide = (int) Math.floor(Math.sqrt(tileSize));
        mySide = 200; // 256 data values a side...
        mySideSquared = mySide * mySide;
    }

    @Override
    public void beginRowOrdered() {

        // Row order should be looping like this:
        // for (y=0...y++)
        // for (x=0...x++)
        // Note it will still work if you violate order, but it's optimized
        // to do a single row of tiles at a time...
        myCurrentOrderedRow = 0;
        myWorkingTiles = preloadTilesForRow(myCurrentOrderedRow);

    }

    private ArrayList<DataNode> preloadTilesForRow(int tileRow) {
        // So we preload the tiles for a single row...when y increases, we
        // buffer the next row of tiles and put back the old ones...
        int tilesPerRow = (myX / mySide) + 1;  // +1 so 375/100 ==> 3 +1 = 4 tiles
        ArrayList<DataNode> theTiles = new ArrayList<DataNode>();
        for (int x = 0; x < tilesPerRow; x++) {
            // final String key = myTileRoot + "x" + x + "y" + tileRow;
            final int theKey = myKeyBase + (tileRow * myNumberX) + x;
            DataNode tile = DataManager.getInstance().popTile(theKey, mySideSquared, myBackground);
            theTiles.add(tile);
        }
        return theTiles;
    }

    private void unloadTilesForRow() {
        if (myWorkingTiles != null) {
            Iterator<DataNode> i = myWorkingTiles.iterator();
            while (i.hasNext()) {
                DataNode tile = i.next();
                DataManager.getInstance().pushTile((Integer) tile.getCacheKey(), tile);
            }
            myWorkingTiles = null;
        }
    }

    @Override
    public void endRowOrdered() {
        unloadTilesForRow();
    }

    /**
     *  Not to be implemented directly, this class wraps a column in the 2D float
     * array. This acts like a general class right now, but could be optimized
     * later so I'm keeping it internal.
     */
    private static class Array1DfloatTileCol implements Array1D<Float> {

        private Array2DfloatAsTiles my2DArray;
        private int myColumn;

        public Array1DfloatTileCol(Array2DfloatAsTiles data, int col) {
            my2DArray = data;
            myColumn = col;
        }

        @Override
        public Float get(int x) {
            return my2DArray.get(myColumn, x);
        }

        @Override
        public void set(int x, Float value) {
            my2DArray.set(myColumn, x, value);
        }

        @Override
        public int size() {
            return my2DArray.getY();
        }

        @Override
        public FloatBuffer getRawBuffer() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getBufferLock() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void begin() {
        }

        @Override
        public void end() {
        }
    }

    /**
     *  Not to be implemented directly, this class wraps a column in the 2D float
     * array
     */
    private static class Array1DfloatTileRow implements Array1D<Float> {

        private Array2DfloatAsTiles my2DArray;
        private int myRow;

        public Array1DfloatTileRow(Array2DfloatAsTiles data, int row) {
            my2DArray = data;
            myRow = row;
        }

        @Override
        public Float get(int x) {
            return my2DArray.get(x, myRow);
        }

        @Override
        public void set(int x, Float value) {
            my2DArray.set(x, myRow, value);
        }

        @Override
        public int size() {
            return my2DArray.getX();
        }

        @Override
        public FloatBuffer getRawBuffer() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Object getBufferLock() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void begin() {
        }

        @Override
        public void end() {
        }
    }

    public Array2DfloatAsTiles(int x, int y, float backgroundValue) {
        myX = x;
        myY = y;
        myBackground = backgroundValue;

        /** figure out the max number of tiles for 2D grid */
        myNumberX = (x / mySide) + 1;  // Ex: 375/100 = 3 --> 3+1 = 4 100 width tiles
        myNumberY = (y / mySide) + 1;

        /** ..and reserve keys for them */
        myKeyBase = DataManager.getInstance().getNewTileKeyRange(myNumberX * myNumberY);

        // That's it.  Tiles will be created on demand as needed during set/get...
    }

    @Override
    public Float get(int x, int y) {

        // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
        // This code duplicates with set "inline"
        // This is pretty slow since it does math everytime
        final int tileX = x / mySide;
        final int tileY = y / mySide;
        final int theKey = myKeyBase + (tileY * myNumberX) + tileX;
        DataNode tile = DataManager.getInstance().getTile(theKey, mySideSquared, myBackground);

        final int localX = x - (mySide * tileX);
        final int localY = y - (mySide * tileY);
        final int at = (localY * mySide) + localX;  // 'x' order
        return tile.get(at);
    }

    @Override
    public void set(int x, int y, Float value) {

        int tileX = x / mySide;
        int tileY = y / mySide;
        if (myWorkingTiles != null) {

            // Cache a row of tiles...
            if (tileY != this.myCurrentOrderedRow) {
                unloadTilesForRow();
                myCurrentOrderedRow = tileY;
                myWorkingTiles = this.preloadTilesForRow(tileY);
            }

            //String key = myTileRoot + "x" + tileX + "y" + tileY;

            // Attempt to get tile from preloaded ones....
            DataNode tile = myWorkingTiles.get(tileX);

            // Debug..compare key to actual tile...
            //if (!(tile.getCacheKey().equals(key))) {
            //    LOG.error("KEY FAILURE: " + key + "!= " + tile.getCacheKey());
            //}
            int localX = x - (mySide * tileX);
            int localY = y - (mySide * tileY);
            int at = (localY * mySide) + localX;  // 'x' order
            //  LOG.debug("SET " + key + " " + at + " == " + value + " " + myX + ", " + myY);
            tile.set(at, value);
        } else {

            // Slower than snails mating...

            // Here we have the CPU and IO hit (the speed cost we pay to save RAM)
            // This code duplicates with get "inline"
            final int theKey = myKeyBase + (tileY * myNumberX) + tileX;
            DataNode tile = DataManager.getInstance().getTile(theKey, mySideSquared, myBackground);
            int localX = x - (mySide * tileX);
            int localY = y - (mySide * tileY);
            int at = (localY * mySide) + localX;  // 'x' order
            //LOG.debug("SET " + key + " " + at + " == " + value + " " + myX + ", " + myY);
            tile.set(at, value);
        }
    }

    @Override
    public int getX() {
        return myX;
    }

    @Override
    public int getY() {
        return myY;
    }

    @Override
    public int size() {
        return myX * myY;
    }

    @Override
    /**
     *  We treat a constant X value as a column
     */
    public Array1D<Float> getCol(int i) {
        return new Array1DfloatTileCol(this, i);
    }

    @Override
    /**
     *  We treat a constant Y value as a row
     */
    public Array1D<Float> getRow(int i) {
        return new Array1DfloatTileRow(this, i);
    }
}
