package org.wg3.datatype;

import java.util.ArrayList;
import org.wg3.storage.Location;
import org.wg3.storage.Array3D;

/**
 *  LatLonHeightGrid is a 3D grid of data.
 * I'm going to use an Array3DfloatRAM for now, which stores the
 * entire thing in RAM.  The LatLonHeightGrids we currently look at are
 * for single radars.
 * 
 * @author Robert Toomey
 */
public class LatLonHeightGrid extends DataType implements Table2DView {

    /** Query object for LatLonHeightGrid */
    public static class LatLonHeightGridQuery extends DataTypeQuery {
    }
    private final ArrayList<Float> heightsMeters;
    private final Array3D<Float> data;
    private final float latResDegs;
    private final float lonResDegs;
    private final int numLats;
    private final int numLons;

    /** Get the number of latitude boxes we have */
    public int getNumLats(){
        return numLats;
    }
    
    /** Get the number of longitude boxes we have */
    public int getNumLons(){
        return numLons;
    }
    
    /** Get the resolution in degrees per latitude */
    public float getLatResDegrees(){
        return latResDegs;
    }
    
    /** Get the resolution in degrees per longitude */
    public float getLonResDegrees(){
        return lonResDegs;
    }
    
    /** is this sync? */
    public Array3D<Float> getData(){
        return data;
    }
    
    // We'll probably have a 'mode' that tells how to read the grid...
    // I can see CONUS OR a different direction...
    // JUST as a test to see if it loaded.....
    @Override
    public int getNumCols() {
        return data.getY();  // num lats
    }

    @Override
    public int getNumRows() {
        return data.getZ(); // num lons
    }

    @Override
    public String getRowHeader(int row) {
        return "Lon" + row;
    }

    @Override
    public String getColHeader(int col) {
        return "Lat" + col;
    }

    @Override
    public boolean getCellValue(int row, int col, CellQuery output) {
        //x = getX, numHeights
        // x height, y lat, z lon
        //(height, lat, lon)
        output.value = data.get(heightsMeters.size() / 2, col, row);
        return true;
    }

    @Override
    public boolean getLocation(LocationType type, int row, int col, Location output) {
        return false;
    }

    @Override
    public boolean getCell(Location input, CellQuery output) {
        output.col = 0;
        output.row = 0;
        return false;
    }

    /** Passed in by builder objects to use to initialize ourselves.
     * This allows us to have final field access from builders.
     */
    public static class LatLonHeightGridMemento extends DataTypeMemento {

        public ArrayList<Float> heightsMeters;
        public Array3D<Float> data;
        public float latResDegs;
        public float lonResDegs;
        public int numLats;
        public int numLons;
    };

    public LatLonHeightGrid(LatLonHeightGridMemento m) {
        super(m);
        // FIXME: assuming heights are sorted..maybe they aren't?
        this.heightsMeters = m.heightsMeters;
        this.data = m.data;
        this.latResDegs = m.latResDegs;
        this.lonResDegs = m.lonResDegs;
        this.numLats = m.numLats;
        this.numLons = m.numLons;

    }

    public void queryData(LatLonHeightGridQuery q) {

        // Quick and dirty here a 15 min rush job, just trying to get it to work for now..
        // FIXME: review and cleanup.

        // Assuming LatLon at NorthWest corner..
        double latDegOrigin = originLocation.getLatitude();
        double lonDegOrigin = originLocation.getLongitude();
        // Get the lat...
        double latDegs = q.inLocation.getLatitude();
        double lonDegs = q.inLocation.getLongitude();
        int latIndex = (int) ((latDegOrigin - latDegs) / latResDegs);
        int lonIndex = (int) ((lonDegs - lonDegOrigin) / lonResDegs);

        if (!((latIndex > 0) && (latIndex < numLats))) {
            q.outDataValue = DataType.MissingData;
            return;
        }
        if (!((lonIndex > 0) && (lonIndex < numLons))) {
            q.outDataValue = DataType.MissingData;
            return;
        }

        // Find the index of the height below us and above us....
        double height = q.inLocation.getHeightKms() * 1000.0;
        int indexBelow = -1;
        int indexAbove = -1;

        boolean found = false;
        for (int i = 0; i < heightsMeters.size(); i++) {
            double curHeight = heightsMeters.get(i);
            if (curHeight > height) {
                indexAbove = i;
                // Below should be the one before...
                if (i > 1) {
                    indexBelow = i - 1;
                }
                found = true;
                break;
            }
        }
        if (!found) {
            indexBelow = heightsMeters.size() - 1;
        }

        if ((indexBelow < 0) || (indexAbove < 0)) {
            q.outDataValue = DataType.MissingData;
        } else {
            double heightAbove = heightsMeters.get(indexAbove);
            double heightBelow = heightsMeters.get(indexBelow);
            double total = heightAbove - heightBelow;

            // interpolate data between heights for now only...
            // Since we have the full grid, we can do tri interpolation 
            float r2 = data.get(indexAbove, latIndex, lonIndex);
            float r1 = data.get(indexBelow, latIndex, lonIndex);
            float weight1 = (float) ((heightAbove - height) / (total));
            float weight2 = (float) ((height - heightBelow) / total);
            double value = weight1 * r1 + weight2 * r2;
            q.outDataValue = (float) value;

           // q.outDataValue = r2;
        }
    }
    
    public int getNumHeights() {
        return data.getX();
    }
    
    public float getHeight(int index){
        return heightsMeters.get(index);
    }
}
