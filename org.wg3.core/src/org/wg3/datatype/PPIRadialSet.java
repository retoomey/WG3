package org.wg3.datatype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;
import org.wg3.storage.Array1D;
import org.wg3.storage.Location;

/*
 * This is the original RadialSet which handles PPI, or Plan Position Indicator
 * radial sets, which have fixed elevation and rotating azimuth. 
 * 
 * @author lakshman
 * @author Robert Toomey
 *
 */
public class PPIRadialSet extends RadialSet implements Table2DView {

    private final static Logger LOG = LoggerFactory.getLogger(PPIRadialSet.class);
    // This is a radial set lookup that finds an exact match for a radial given an azimuth.  
    // Need this for the vslice/isosurface in the GUI.
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
    public static class PPIRadialSetMemento extends RadialSetMemento {
    };

    /**
     * The query object for RadialSets.
     */
    public static class PPIRadialSetQuery extends DataTypeQuery {

        /**
         * Query by SphericalLocation, has priority over inLocation.. Try to use
         * this when passing the same location to multiple radial sets of a
         * volume...saves calculation, use the locationToSpherical function
         */
        public SphericalLocation inSphere = null;
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

    public PPIRadialSet(PPIRadialSetMemento m) {
        super(m);

        createAzimuthSearch();
    }

    /**
     * Create a sorted list of end azimuth numbers, which allows us to binary
     * search for a Radial by azimuth very quickly. Note that this doesn't mean
     * the RadialSet is sorted.
     */
    protected final void createAzimuthSearch() {
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
     * Return a new query object. Use when the datatype is unknown
     */
    @Override
    public DataTypeQuery getNewQueryObject() {
        return new PPIRadialSetQuery();
    }

    /**
     * Wrap a generic DataTypeQuery to our type. Call getNewQueryObject above
     * and pass in
     */
    @Override
    public void queryData(DataTypeQuery q) {
        queryData((PPIRadialSetQuery) (q));
    }

    /**
     * For a given location, get the information into a data object. For speed
     * the data object is typically pre-created and reused Note: This method is
     * overloading DataType's function, not overriding. The object has to
     * actually be a RadialSetQuery or cast to it for this to get called
     * instead.
     */
    public void queryData(PPIRadialSetQuery q) {

        boolean haveLocation = false;

        SphericalLocation a = null;
        if (q.inSphere != null) {
            a = q.inSphere;
            haveLocation = true;
        } else {
            if (q.inLocation != null) {
                a = new SphericalLocation();
                this.locationToSphere(q.inLocation, a);
                haveLocation = true;
            }
        }

        if (haveLocation) {

            q.outAzimuthDegrees = (float) a.azimuthDegs;

            // Get the elevation of the point (estimate)
            if (q.inUseHeight) {
                Radial first = getRadial(0);
                double bw = first.getBeamWidthDegrees();
                //double elev = Math.asin(fromConeApex.dotProduct(myUz) / norm) * 180 / Math.PI;
                double elev_diff = a.elevDegs - getFixedAngleDegs();

                // We want the interpolation weight (for bi/tri-linear)
                if (q.inNeedInterpolationWeight) {
                    // FIXME: math could be cached in a for speed in volumes

                    /*
                     * Math for height calculation.... This
                     * is first part of the lat/lon/height
                     * weights so I can do true
                     * bilinear/trilinear interpolation.
                     * Bleh.. Here's the math:
                     *
                     * Line 1: Line from sample point
                     * straight down to ground...The height
                     * Polar to cartesion of the 'sample'
                     * point... e = a.elev; (r*cos(e),
                     * r*sin(e)); Point 2: (r*cos(e), 0) ==>
                     * X = r*cos(e); the vertical line....
                     *
                     * Line 2 (radial beam) Point 1: (0,0);
                     * Point 2: (R*cos(elevation),
                     * R*sin(elevation);
                     *
                     * y = mx+b where z = elevation; m =
                     * tan(z); b = 0; ==> y = x*tan(z); //
                     * For radar beam line...
                     *
                     * x1 = r*cos(e); // Sample point y1 =
                     * r*sin(e); x2 = r*cos(e); // Point on
                     * the 'beam' y2 = r*cos(e)*tan(z);
                     *
                     * // Now the distance from the beam
                     * point to the sample point...
                     * (x2-x1)^2 = 0; (y2-y1)^2 =
                     * (r*cos(e)*tan(z)-(r*sin(e)))^2
                     * distance =
                     * abs(r*(cos(e)*tan(z)-sin(e)));
                     */
                    // Calculate the weight for interpolation in height
                    // We convert from radial polar space to cartesian and get 
                    // the distance from sample point to the beam line.

                    // Projection in Height of the sampled point onto the beam
                    // of the radar....or is it? rofl..
                    // double eR = Math.toRadians(a.elevDegs);
                    // double d2R = elevRads;
                    // double h = a.range * (Math.cos(eR) * Math.sin(d2R) / Math.cos(d2R) - Math.sin(eR));
                    // double h = a.range * (Math.cos(eR) * Math.tan(d2R) - Math.sin(eR));
                    double h = a.getHeightWeight(getFixedAngleTan());

                    // if (h < 0) {
                    //     h = -h;
                    // } // sqrt of square is abs         
                    // if (a.elevDegs < elevDegs) {
                    //     h = -h;
                    //  }
                    q.outDistanceHeight = (float) (h);
                    // Ignore beam width filter, we want
                    // the value AND the weight for interpolation
                    //elev_diff = 0;
                }

                // Beam width filter.  Outside beam width in the vertical?
                if (Math.abs(elev_diff) > bw / 2.0) {

                    // FIXME: Should we still get this stuff anyway?  Will slow down volumes like vslice
                    // Probably need more in flags
                    q.outHitRadialNumber = -1;
                    q.outHitGateNumber = -1;
                    q.outInAzimuth = false;
                    q.outInRange = false;
                    if (!q.inNeedInterpolationWeight) {
                        q.outDataValue = MissingData;
                        return;
                    }
                }
            }

            // Search radials by end azimuth
            if (radials != null) {
                int index = Arrays.binarySearch(angleToRadial, (float) a.azimuthDegs);
                int radialIndex = (index < 0) ? -(index + 1) : index;

                if ((radialIndex >= 0) && (radialIndex < angleToRadial.length)) { // within all radial end values
                    Radial candidate = azimuthRadials[radialIndex];
                    q.outHitRadialNumber = candidate.getIndex();
                    q.outInAzimuth = candidate.contains((float) a.azimuthDegs);

                    final double gate = (a.range - this.getRangeToFirstGateKms()) / candidate.getGateWidthKms();
                    final int gateNumber = (int) Math.floor(gate);
                    q.outHitGateNumber = gateNumber;

                    // Is the query within range for this Radial?  Range is up to the last piece of available data.
                    Array1D<Float> gates = candidate.getValues();
                    q.outInRange = (gateNumber >= 0) && (gateNumber < gates.size());

                    // Valid data must be in azimuth and range...? Note for vslice this will make black gaps in azimuth
                    //if ((q.outInAzimuth) && (q.outInRange)){
                    if (q.outInRange) {
                        /*
                         * Experiment, playing with
                         * range interpolation to see
                         * how it looks... float
                         * dataCore =
                         * gates.get(gateNumber);
                         * q.outDataValue = dataCore;
                         *
                         * if
                         * (DataType.isRealDataValue(q.outDataValue)){
                         * if
                         * (q.inNeedInterpolationWeight)
                         * { double weight1 = gate -
                         * gateNumber; // .5 = center, 0
                         * floor, 1 to q.outDataValue =
                         * dataCore; if (weight1 >= .5)
                         * { if (gateNumber + 1 <
                         * gates.size()) { float dataUp1
                         * = gates.get(gateNumber + 1);
                         * if
                         * (DataType.isRealDataValue(dataUp1)){
                         * // at weight == 1, 50% up,
                         * 50% core 0 // at weight = .5,
                         * 0% up, 100% core. .5 // y2 =
                         * 1 // y1 = .5 // y = weight1
                         * // R1 core // R2 .5*dataUp1;
                         *
                         * float i1 = (float) ((1 -
                         * weight1) / .5) * dataCore;
                         * //float i2 = (float)
                         * (((weight1 - .5) / .5) * (.5
                         * * dataUp1)); float i2 =
                         * (float) (((weight1 - .5) /
                         * .5) * (dataUp1));
                         * q.outDataValue = i1 + i2;
                         * //q.outDataValue = 1000;
                         * return; } } else { //
                         * q.outDataValue = dataCore;
                         * return; } } else { if
                         * (gateNumber - 1 > 0) { float
                         * dataDown =
                         * gates.get(gateNumber - 1); if
                         * (DataType.isRealDataValue(dataDown)){
                         * // y2 = .5 // y1 = 0 // y =
                         * weight1 // R2 core // R1
                         * .5*dataDown;
                         *
                         * float i1 = (float) (((.5 -
                         * weight1) / .5) * (.5 *
                         * dataDown)); float i2 =
                         * (float) (((weight1) / .5) *
                         * (dataCore)); //
                         * q.outDataValue = i1 + i2;
                         *
                         * return; } } else { //
                         * q.outDataValue = dataCore;
                         * return; }
                         *
                         * }
                         * }
                         * }
                         * }else{
                         */
                        q.outDataValue = gates.get(gateNumber);
                        return;
                    }
                }
            }
        }
        q.outDataValue = MissingData;
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
    public boolean getCellValue(int row, int col, CellQuery output) {
        float value = DataType.MissingData;
        int rcount = getNumRadials();
        if (col < rcount) {
            Radial r = getRadial((col));
            if (r != null) {
                int count = getNumGates();
                if (count > 0) {
                    value = r.getValue((count - row - 1));
                }
            }
        }
        output.value = value;
        return true;
    }

    @Override
    public boolean getLocation(LocationType type, int row, int col,
            Location output) {
        if ((col >= getNumCols()) || (row >= getNumRows())) {
            LOG.error("Table out of bounds : (" + col + "," + row + ") bounds [" + getNumCols() + "," + getNumRows());
            return false;
        }
        boolean success = false;

        Radial r = getRadial((col));
        if (r != null) {
            int count = getNumGates();
            if (count > 0) {
                Location center = getRadarLocation();
                int irow = getNumRows() - row - 1;
                float rangeKms = getRangeToFirstGateKms();
                float w = r.getGateWidthKms();
                float startRAD = r.getStartRadians();
                float endRAD = r.getEndRadians();
                float elevSin = getFixedAngleSin();
                float elevCos = getFixedAngleCos();
                // FIXME clean this up.
                switch (type) {
                    case CENTER: {
                        float fullRangeKms = rangeKms + (w * ((irow) + .5f));

                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin((startRAD + endRAD) / 2.0f), // Precomputed azimuth sin and cos around	
                                Math.cos((startRAD + endRAD) / 2.0f), fullRangeKms, // at range in kilometers
                                elevSin, // sin of the elevation (precomputed)
                                elevCos // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case TOP_LEFT: {
                        float fullRangeKms = rangeKms + (w * (irow + 1));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(startRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(startRAD), fullRangeKms, // at range in kilometers
                                elevSin, // sin of the elevation (precomputed)
                                elevCos // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case TOP_RIGHT: {
                        float fullRangeKms = rangeKms + (w * (irow + 1));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(endRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(endRAD), fullRangeKms, // at range in kilometers
                                elevSin, // sin of the elevation (precomputed)
                                elevCos // cos of the elevation (precomputed)
                                );
                    }
                    break;
                    case BOTTOM_LEFT: {
                        float fullRangeKms = rangeKms + (w * (irow));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(startRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(startRAD), fullRangeKms, // at range in kilometers
                                elevSin, // sin of the elevation (precomputed)
                                elevCos // cos of the elevation (precomputed)
                                );
                    }
                    break;

                    case BOTTOM_RIGHT: {
                        float fullRangeKms = rangeKms + (w * (irow));
                        RadialUtil.getAzRanElLocation(output,// (saves on newing)
                                center,
                                Math.sin(endRAD), // Precomputed azimuth sin and cos around	
                                Math.cos(endRAD), fullRangeKms, // at range in kilometers
                                elevSin, // sin of the elevation (precomputed)
                                elevCos // cos of the elevation (precomputed)
                                );
                    }
                    break;
                }

                success = true;
            }
        }

        return success;
    }

    @Override
    public boolean getCell(Location input, CellQuery output) {
        PPIRadialSetQuery q = new PPIRadialSetQuery();
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

    /**
     * Create an SRM delta value for each radial we have. This method is called
     * by the GUI Storm Relative Motion filter. Keeping the logic in RadialSet.
     * The GUI filter allows us to show SRM without modifying the original
     * RadialSet.
     */
    public ArrayList<Float> createSRMDeltas(float speedMS, float dirDegrees) {
        /*
         ArrayList<Float> srmDeltas = new ArrayList<Float>();
         if (azimuthRadials != null) {
         for (Radial r : azimuthRadials) {
         float aglRadians = (float) Math.toRadians((r.getMidAzimuthDegs() - dirDegrees));
         float vr = (float) (Math.cos(aglRadians) * speedMS);
         vr = ((int) (vr * 2.0f + 0.5f)) * 0.5f;
         srmDeltas.add(vr);
         }
         }
         return srmDeltas;
         */
        return null;
    }
}
