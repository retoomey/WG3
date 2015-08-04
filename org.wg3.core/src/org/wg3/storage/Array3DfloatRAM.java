package org.wg3.storage;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

/**
 *  a 3D array.  Only use this if you have the ram for it.
 * 
 * @author Robert Toomey
 */
public class Array3DfloatRAM implements Array3D<Float> {

    private final static Logger LOG = LoggerFactory.getLogger(Array2DfloatRAM.class);
    private int myX;
    private int myY;
    private int myZ;
    private float myBackground;
    private float[][][] myArray;
    private boolean myValid = false;

    public Array3DfloatRAM(int x, int y, int z, float backgroundValue) {
        myX = x;
        myY = y;
        myZ = z;
        myBackground = backgroundValue;

        try {
            myArray = new float[x][y][z];
            myValid = true;

            // Bleh, fill the background.  Then again, we shouldn't really
            // be using this for anything BIG...
            // We could do the 'shift' trick, set background to 0 and true
            // zero to something else.
            for (int x1 = 0; x1 < x; x1++) {
                for (int y1 = 0; y1 < y; y1++) {
                    for (int z1 = 0; z1 < z; z1++) {
                        myArray[x1][y1][z1] = myBackground;
                    }
                }
            }
        } catch (OutOfMemoryError mem) {
            LOG.warn("Array3D storage not enough heap space for float[" + x + "][" + y + "][" + z + "] array");
        }
    }

    @Override
    public Float get(int x, int y, int z) {
        if (myValid) {
            return myArray[x][y][z];
        }
        return myBackground;
    }

    @Override
    public void set(int x, int y, int z, Float value) {
        if (myValid) {
            myArray[x][y][z] = value;
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
    public int getZ() {
        return myZ;
    }

    @Override
    public int size() {
        return myX * myY * myZ;
    }
}
