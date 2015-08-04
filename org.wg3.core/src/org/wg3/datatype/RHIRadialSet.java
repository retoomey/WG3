package org.wg3.datatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;
import org.wg3.storage.Location;

/**
 * This is a RadialSet which handles RHI, or Range Height Indicator radial sets,
 * which have fixed azimuth and changing elevation.
 *
 * @author Robert Toomey
 */
public class RHIRadialSet extends RadialSet implements Table2DView {
    
    private final static Logger LOG = LoggerFactory.getLogger(RHIRadialSet.class);
    // This is a radial set lookup that finds an exact match for a radial given an azimuth.  Need this for the vslice/isosurface
    // in the GUI.
    /**
     * A sorted array of end azimuth, each corresponding to azimuthRadials
     * below, this gives us a o(nlogn) binary search of radials given an angle
     * (which for 360 radials is about 8 searches per angle . Memory cost: one
     * Float, one Radial reference per Radial, typically O(365) Speed cost:
     * Typically 8 searches O(log(360))
     */
    private float[] angleToRadial;
    /**
     * Radials corresponding the angleToRadial above (these are sorted by
     * increasing end azimuth).
     */
    private Radial[] azimuthRadials;

    /**
     * Passed in by builder objects to use to initialize ourselves. This allows
     * us to have final field access from builders.
     */
    public static class RHIRadialSetMemento extends RadialSetMemento {
    };

    /**
     * The query object for RadialSets.
     */
    public static class RHIRadialSetQuery extends DataType.DataTypeQuery {

        /**
         * Query by SphericalLocation, has priority over inLocation.. Try to use
         * this when passing the same location to multiple radial sets of a
         * volume...saves calculation, use the locationToSpherical function
         */
        public PPIRadialSet.SphericalLocation inSphere = null;
        /**
         * Used by the GUI to tell which RadialSet in a RadialSetVolume query
         * was in
         */
        public int outRadialSetNumber = -1;
        /**
         * The radial index the last query found. This is found by binary search
         * of sorted end azimuth. This may be outside the Radial azimuth range
         * if outInAzimuth is false. Radial 1: 0-2 degrees, Radial 2: 5-8
         * degrees. 4 degrees --> Radial 2, outInAzimuth = false 5 degrees -->
         * Radial 2, outInAzimuth = true
         */
        public int outHitRadialNumber;
        /**
         * Our we within the azimuth of the hit radial number? Some stuff might
         * need to know this, do we in vslice smear azimuth or show blank gaps?
         */
        public boolean outInAzimuth;
        /**
         * The radial gate number the last query found. Note this is only set if
         * data is HIT by location exactly
         */
        public int outHitGateNumber;
        /**
         * Is the gate in range? The query assumes an infinite size RadialSet
         * for gate calculation
         */
        public boolean outInRange;
        /**
         * The azimuth for query in degrees
         */
        public float outAzimuthDegrees;
    };
    
    public RHIRadialSet(RHIRadialSetMemento m) {
        super(m);
        createElevSearch();
    }

    /**
     * Create a sorted list of end azimuth numbers, which allows us to binary
     * search for a Radial by azimuth very quickly. Note that this doesn't mean
     * the RadialSet is sorted.
     */
    protected final void createElevSearch() {
        // This assumes no two radials have the same end angle, even if they do,
        // should still work, just indeterminate which of the 2 radials you'll get
        if (radials != null && (radials.length > 0)) {
            angleToRadial = new float[radials.length];
            azimuthRadials = Arrays.copyOf(radials, radials.length);

            // Sort the azimuth radials by end angle...
            Arrays.sort(azimuthRadials, new Comparator<Radial>() {
                @Override
                public int compare(Radial o1, Radial o2) {
                    double u1 = o1.getEndDegrees();
                    double u2 = o2.getEndDegrees();
                    if (u1 < u2) {
                        return -1;
                    }
                    if (u1 > u2) {
                        return 1;
                    }
                    return 0;
                }
            });

            // Create the angle list from the sorted radials...
            for (int i = 0; i < angleToRadial.length; i++) {
                angleToRadial[i] = azimuthRadials[i].getEndDegrees();
            }
        }
    }

    /**
     * A Spherical coordinate location centered around our RadialSet center.
     * Used for speed in VSlice/Isosurface calculations. Filled in by our
     * locationToSphere method The trick is that all radial sets in a volume
     * will give the same result for a given location.
     */
    public static class SphericalLocation {

        /**
         * Angle in degrees
         */
        public double azimuthDegs;
        /**
         * Angle in degrees
         */
        public double elevDegs;
        /**
         * Distance in Kms
         */
        public double range;
        /**
         * True if we cached this stuff. Will be the same value for all
         * RadialSets for a particular sample location
         */
        private boolean cachedSinCos = false;
        private double elevSin;
        private double elevCos;

        /**
         * Given the tan of an elevation, get the weight in height from the
         * actual beam at our elevDegs. For example, if inElev == elevDegs, then
         * the value is zero, if inElev < elevDegs the value is -, if inElev >
         * elevDegs the value is +
         *
         * @param inElevTan The tan of the other elevation
         * @return
         */
        public double getHeightWeight(double inElevTan) {
            // Trig so slow, cache it...
            if (!cachedSinCos) {
                double rads = Math.toRadians(elevDegs);
                elevCos = Math.cos(rads);
                elevSin = Math.sin(rads);
                cachedSinCos = true;
            }
            return -(range * (elevCos * inElevTan - elevSin));
        }
    }

    /**
     * For a given location, get the information into a data object. For speed
     * the data object is typically pre-created and reused Note: This method is
     * overloading DataType's function, not overriding. The object has to
     * actually be a RadialSetQuery or cast to it for this to get called
     * instead.
     */
    public void queryData(PPIRadialSet.PPIRadialSetQuery q) {

        // REWRITE needed probably for new case...
    }

    /**
     * Create an SRM delta value for each radial we have. This method is called
     * by the GUI Storm Relative Motion filter. Keeping the logic in RadialSet.
     * The GUI filter allows us to show SRM without modifying the original
     * RadialSet.
     */
    public ArrayList<Float> createSRMDeltas(float speedMS, float dirDegrees) {
        /*
         ArrayList<Float> srmDeltas = new ArrayList<Float>();
         // FIXME: RHI will have a single repeated SRM value, since all the radials
         // have the same azimuth...
		
         if (azimuthRadials != null) {
         for (Radial r : azimuthRadials) {
         float aglRadians = (float) Math.toRadians((r.getMidAzimuthDegs() - dirDegrees));
         float vr = (float) (Math.cos(aglRadians) * speedMS);
         vr = ((int) (vr * 2.0f + 0.5f)) * 0.5f;
         srmDeltas.add(vr);
         }
         }
         return srmDeltas;
         * */
        return null;
    }

    // Table2D implementation --------------------------------------------------------
    @Override
    public int getNumCols() {
        return (getNumRadials());
    }
    
    @Override
    public int getNumRows() {
        return (getNumGates());
    }
    
    @Override
    public String getRowHeader(int row) {
        return (String.format("%6.2f", getRowRangeKms(row)));
    }
    
    public float getRowRangeKms(int row) {
        int index = getNumRows() - (row) - 1;
        float firstGateKms = getRangeToFirstGateKms();
        float perGateKms = getGateWidthKms();
        float rangeKms = firstGateKms + (index * perGateKms);
        return rangeKms;
        
    }
    
    @Override
    public String getColHeader(int col) {
        float azDegs = getColAzimuth(col);
        
        if (Float.isNaN(azDegs)) { // slow??
            return "";
        } else {
            return (String.format("%6.2f", azDegs));
        }
    }
    
    public float getColAzimuth(int col) {
        float azDegs = Float.NaN;
        if (col < getNumRadials()) { // Do we need this check?
            Radial aRadial = getRadial((col));
            if (aRadial != null) {
                azDegs = aRadial.getStartDegrees();
            }
        }
        return azDegs;
    }
    
    @Override
    public boolean getCellValue(int row, int col, Table2DView.CellQuery output) {
        float value = 0;
        Radial r = getRadial((col));
        if (r != null) {
            int count = getNumGates();
            if (count > 0) {
                value = r.getValue((count - row - 1));
            }
        }
        output.value = value;
        return true;
    }
    
    @Override
    public boolean getLocation(Table2DView.LocationType type, int row, int col,
            Location output) {
        LOG.error("getLocation for RHI is not implemented yet");
        return false;
    }
    
    @Override
    public boolean getCell(Location input, Table2DView.CellQuery output) {
        PPIRadialSet.PPIRadialSetQuery q = new PPIRadialSet.PPIRadialSetQuery();
        q.inLocation = input;
        q.inUseHeight = false;
        queryData(q);
        boolean withinTable;
        int row = q.outHitGateNumber;
        int col = q.outHitRadialNumber;
        output.row = getNumRows() - row - 1;
        output.col = col;
        output.rowInRange = ((row > 0) && (row < getNumRows()));
        output.colInRange = ((col > 0) && (col < getNumRows()));
        withinTable = output.rowInRange && output.colInRange;
        return withinTable;
    }
    
}
