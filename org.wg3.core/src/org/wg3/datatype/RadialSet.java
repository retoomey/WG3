package org.wg3.datatype;

import org.wg3.log.Logger;
import org.wg3.log.LoggerFactory;
import org.wg3.storage.Location;

/**
 * A RadialSet. Root class of all RadialSet
 *
 * @author Robert Toomey
 */
public class RadialSet extends DataType {

	@SuppressWarnings("unused")
    private final static Logger LOG = LoggerFactory.getLogger(RadialSet.class);
    /**
     * Fixed angle in degrees of this radial set. This is the unchanging angle
     * of the RadialSet, it will be elevation for PPI and azimuth for RHI
     */
    private final float fixedAngleDegs;
    /**
     * Fixed angle in radians of this radial set
     */
    private final float fixedAngleRads;
    /**
     * Tan of fixed angle, cached for the beam height distance function
     */
    private final float fixedAngleTan;
    /**
     * Sin of the fixed angle cached for getLocation and other needs
     */
    private final float fixedAngleSin;
    /**
     * Cos of the fixed angle cached for getLocation and other needs
     */
    private final float fixedAngleCos;
    /**
     * The array of Radial that we hold. These are in load order, not sorted in
     * any way.
     */
    protected final Radial[] radials;
    /**
     * Range to the first gate of the RadialSet in Kms
     */
    private final float rangeToFirstGate;
    /**
     * The maximum number of gates of all Radial
     */
    private final int myMaxGateNumber;
    /**
     * Cached valued for location to sphere. These change when originLocation
     * does
     */
    private final double rlx, rly, rlz;
    private final double zvx, zvy, zvz;
    private final double xvx, xvy, xvz;
    private final double yvx, yvy, yvz;


    /**
     * Passed in by builder objects to use to initialize ourselves. This allows
     * us to have final field access from builders.
     */
    public static class RadialSetMemento extends DataTypeMemento {

        /**
         * The fixed angle in degrees of this radial set, for PPI this is the
         * elevation of the RadialSet
         */
        public float fixedAngleDegs;
        /**
         * Our radials
         */
        public Radial[] radials;
        /**
         * Range to the first gate of the RadialSet in Kms
         */
        public float rangeToFirstGate;
        /**
         * The maximum number of gates of all Radial
         */
        public int maxGateNumber = 0;
    };

    /**
     * Create a DataType given a DataTypeMemento object
     */
    public RadialSet(RadialSetMemento m) {
        super(m);
        this.fixedAngleDegs = m.fixedAngleDegs;
        this.fixedAngleRads = (float) Math.toRadians(fixedAngleDegs);
        this.fixedAngleTan = (float) Math.tan(fixedAngleRads);
        this.fixedAngleSin = (float) Math.sin(fixedAngleRads);
        this.fixedAngleCos = (float) Math.cos(fixedAngleRads);
        this.radials = m.radials;
        this.rangeToFirstGate = m.rangeToFirstGate;
        this.myMaxGateNumber = m.maxGateNumber;
        
        // This stuff doesn't change unless ORIGINLOCATION does.
        // Made originLocation final, if changed need to call this when set

        double ree = Location.EarthRadius + originLocation.getHeightKms(); // kms
        double phi2 = originLocation.getLongitude() * Math.PI / 180.0; // radians();
        double beta2 = originLocation.getLatitude() * Math.PI / 180.0; // .radians();

        double cosBeta2 = Math.cos(beta2);
        rlx = ree * Math.cos(phi2) * cosBeta2;
        rly = ree * Math.sin(phi2) * cosBeta2;
        rlz = ree * Math.sin(beta2);
        //  double zvx, zvy, zvz;

        double zvNorm = Math.sqrt(rlx * rlx + rly * rly + rlz * rlz);
        if (zvNorm > 0) {
            zvx = rlx / zvNorm;
            zvy = rly / zvNorm;
            zvz = rlz / zvNorm;
        } else {
            zvx = 0;
            zvy = 0;
            zvz = 0;
        }
        //double xvx, xvy, xvz;
        double xvNorm = Math.sqrt(zvy * zvy + zvx * zvx);
        if (xvNorm > 0) {
            xvx = -zvy / xvNorm;
            xvy = zvx / xvNorm;
            xvz = 0; // 0/xvNorm;
        } else {
            xvx = 0;
            xvy = 0;
            xvz = 0;
        }
        //double yvx, yvy, yvz;
        yvx = zvy * xvz - zvz * xvy;
        yvy = zvz * xvx - zvx * xvz;
        yvz = zvx * xvy - zvy * xvx;
    }

    /**
     * The maximum number of gates of any Radial in us
     */
    public int getNumGates() {
        return myMaxGateNumber;
    }

    /**
     * Get the number of Radial objects in this RadialSet
     */
    public int getNumRadials() {
        if (radials == null) {
            return 0;
        }
        return radials.length;
    }

    /**
     * Get the fixed angle of the RadialSet in degrees
     */
    public float getFixedAngleDegs() {
        return fixedAngleDegs;
    }

    /**
     * Get the fixed angle of the RadialSet in radians
     */
    public float getFixedAngleRads() {
        return fixedAngleRads;
    }

    /**
     * Get the cached Sin of fixed angle of the RadialSet
     */
    public float getFixedAngleSin() {
        return fixedAngleSin;
    }

    /**
     * Get the cached Cos of fixed angle of the RadialSet
     */
    public float getFixedAngleCos() {
        return fixedAngleCos;
    }

    /**
     * Get the cached Tan of fixed angle of the RadialSet
     */
    public float getFixedAngleTan() {
        return fixedAngleTan;
    }

    /**
     * Get given radial number
     */
    public Radial getRadial(int index) {
        return radials[index];
    }

    /**
     * Get the range to the beginning of the first gate in Kms for all Radial
     */
    public float getRangeToFirstGateKms() {
        return rangeToFirstGate;
    }

    /**
     * Return a double used to sort a volume of this DataType. For example, for
     * RadialSets this would normally be fixed angle of the RadialSet. For
     * example, PPIRadialSets will sort by elevation. For example, RHIRadialSets
     * will sort by azimuth.
     *
     * @return value in volume
     */
    @Override
    public double sortInVolume() {
        return getFixedAngleDegs();
    }

    /**
     * String debugging output
     */
    @Override
    public String toStringDB() {
        String s = "RadialSet " + getTypeName() + " at " + fixedAngleDegs + " has "
                + radials.length + " radials." + " the first:\n";
        return s;
    }

    public final void updateCachedValues() {

        
    }

    /**
     * In volumes, a location is used to query multiple radial sets that share
     * the same center location, ux, uy, uz, etc. This takes a location in space
     * and caches all of the calculations in relation to the volume. This speeds
     * up VSlice/Isosurface rendering calculations.
     *
     * This then is passed into the query function.
     *
     * @FIXME: This might only apply to PPI RadialSet, not sure yet..
     */
    public void locationToSphere(Location l, PPIRadialSet.SphericalLocation out) {
        // All the math expanded (avoids new allows some short cuts for speed)
        // Remember in a GUI volume render we typically call this in a monster loop
        // while the user is dragging so every ms counts...

        final double r = Location.EarthRadius + l.getHeightKms(); // kms
        final double phi = l.getLongitude() * Math.PI / 180.0; // radians();
        final double beta = l.getLatitude() * Math.PI / 180.0; // .radians();
        final double cosBeta = Math.cos(beta);

        final double coneX = (r * Math.cos(phi) * cosBeta) - rlx;
        final double coneY = (r * Math.sin(phi) * cosBeta) - rly;
        final double coneZ = (r * Math.sin(beta)) - rlz;

        final double crossX = coneY * zvz - coneZ * zvy;
        final double crossY = coneZ * zvx - coneX * zvz;
        final double crossZ = coneX * zvy - coneY * zvx;

        out.range = Math.sqrt(coneX * coneX + coneY * coneY + coneZ * coneZ);

        final double dotx = crossX * xvx + crossY * xvy + crossZ * xvz;
        final double doty = crossX * yvx + crossY * yvy + crossZ * yvz;
        final double az = (360.0 - (Math.toDegrees(Math.atan2(doty, dotx))));
        out.azimuthDegs = Radial.normalizeDegrees((float) az);

        final double dotz = coneX * zvx + coneY * zvy + coneZ * zvz;
        out.elevDegs = Math.asin(dotz / out.range) * 180 / Math.PI;
    }

    /**
     * beamwidth of first radial, or 1 degree if there is no radial.
     */
    public float getBeamWidthKms() {
        if (radials.length > 0) {
            radials[0].getBeamWidthDegrees();
        }
        return 1;
    }

    /**
     * gatewidth of first radial, or 1 km if there is no radial.
     */
    public float getGateWidthKms() {
        if (radials.length > 0) {
            return radials[0].getGateWidthKms();
        }
        return 1.0f;
    }

    /**
     * nyquist of first radial, or 0 if there is no radial.
     */
    public float getNyquistMetersPerSecond() {
        if (radials.length > 0) {
            return radials[0].getNyquistMetersPerSecond();
        }
        return 0;
    }

    /**
     * The center location of the RadialSet
     */
    public Location getRadarLocation() {
        return originLocation;
    }

    /**
     * @return 0 if vcp is unknown.
     */
    public int getVCP() {
        try {
            return Integer.parseInt(getAttribute("vcp").toString());
        } catch (Exception e) {
            // number format, null, etc.
            return 0;
        }
    }

    /**
     * Given speed in meters per second, a direction in degrees (for the SRM
     * vector), and an azimuth, generates the SRM value
     *
     * @param speedMS Meters per second of SRM vector
     * @param dirDegs Direction in degs of SRM vector from radar center
     * @param azimuthDegs Azimuth of Radial wanting SRM delta for
     * @return the SRM delta value (added to gate value)
     */
    public static float getSRMDelta(float speedMS, float dirDegs, float azimuthDegs) {
        float aglRadians = (float) Math.toRadians(azimuthDegs - dirDegs);
        float vr = (float) (Math.cos(aglRadians) * speedMS);
        vr = ((int) (vr * 2.0f + 0.5f)) * 0.5f;
        return vr;
    }
}
