package org.wg3.storage;

import java.nio.FloatBuffer;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;

/**
 * Full 2D float array in RAM.  Not recommended since for massive radar data
 * this will be a RAM pig, though on high RAM algorithm machines it's
 * not an issue.
 * 
 * Also implements two wrapper classes for accessing an entire row/col
 * as an Array1Dfloat
 * 
 * @author Robert Toomey
 *
 */
public class Array2DfloatRAM implements Array2D<Float> {

    private final static Logger LOG = LoggerFactory.getLogger(Array2DfloatRAM.class);
    private int myX;
    private int myY;
    private float myBackground;
    private float[][] myArray;
    private boolean myValid = false;

    @Override
    public void beginRowOrdered() {
       // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void endRowOrdered() {
       // throw new UnsupportedOperationException("Not supported yet.");
    }

    /** Not to be implemented directly, this class wraps a column in the 2D float array */
    private static class Array1DfloatRAMCOLUMN implements Array1D<Float> {

        private Array2DfloatRAM my2DArray;
        private int myColumn;

        public Array1DfloatRAMCOLUMN(Array2DfloatRAM ram, int col) {
            my2DArray = ram;
            myColumn = col;
        }

        @Override
        public Float get(int x) {
            if (my2DArray.myValid) {
                return my2DArray.myArray[x][myColumn];
            }
            return 0.0f;
        }

        @Override
        public void set(int x, Float value) {
            if (my2DArray.myValid) {
                my2DArray.myArray[x][myColumn] = value;
            }
        }

        @Override
        public int size() {
            if (my2DArray.myValid) {
                return my2DArray.getY();
            }
            return 0;
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

    /** Not to be implemented directly, this class wraps a column in the 2D float array */
    private static class Array1DfloatRAMROW implements Array1D<Float> {

        private Array2DfloatRAM my2DArray;
        private int myRow;

        public Array1DfloatRAMROW(Array2DfloatRAM ram, int row) {
            my2DArray = ram;
            myRow = row;
        }

        @Override
        public Float get(int x) {
            if (my2DArray.myValid) {
                return my2DArray.myArray[myRow][x];
            }
            return 0.0f;
        }

        @Override
        public void set(int x, Float value) {
            if (my2DArray.myValid) {
                my2DArray.myArray[myRow][x] = value;
            }
        }

        @Override
        public int size() {
            if (my2DArray.myValid) {
                return my2DArray.getX();
            }
            return 0;
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

    public Array2DfloatRAM(int x, int y, float backgroundValue) {
        myX = x;
        myY = y;
        myBackground = backgroundValue;

        try {
            myArray = new float[x][y];
            myValid = true;
        } catch (OutOfMemoryError mem) {
            LOG.warn("Array2D storage not enough heap space for float[" + x + "][" + y + "] array");
        }
    }

    @Override
    public Float get(int x, int y) {
        if (myValid) {
            return myArray[x][y];
        }
        return myBackground;
    }

    @Override
    public void set(int x, int y, Float value) {
        if (myValid) {
            myArray[x][y] = value;
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
    public Array1D<Float> getCol(int i) {
        // Note the only memory here is the object, not the array
        return new Array1DfloatRAMCOLUMN(this, i);
    }

    @Override
    public Array1D<Float> getRow(int i) {
        // Note the only memory here is the object, not the array
        return new Array1DfloatRAMROW(this, i);
    }
}
