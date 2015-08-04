package org.wg3.datatype;

import org.wg3.storage.Array1D;

/**
 * A Radial holds a 'beam' of gate data that shares a fixed angle
 * 
 * @author lakshman
 * @author Robert Toomey
 * 
 */
public class Radial {

    /** The 1D float that stores the radial */
    private Array1D<Float> array;
    /** The angle in degrees of the Radial. */
    private final float degrees;
    /** The angle in radians of the Radial. */
    private final float radians;
    /** The width of the beam in degrees */
    private final float beamWidthDegs;
    /** The total degrees in azimuth of the beam */
    private final float spacingDegrees;
    /** The nyquist of this radial in meters per second */
    private final float nyquistMetersPerSecond;
    /** The constant length in Kms from radar center that each gate is */
    private final float gateWidthKms;
    /** The index of this radial inside of a RadialSet in creation order */
    private int index = -1;

    /** in degrees and kilometers. Does not copy array 
     * @param ny 
     * @param  */
    public Radial(float inDegrees, float beamWidthDegs,
            float azimuthalSpacing, float gateWidth,
            float nyquist, Array1D<Float> values, int i) {
        this.gateWidthKms = gateWidth;
        this.array = values;
        this.degrees = inDegrees;
	this.radians = (float) Math.toRadians(inDegrees);
        this.beamWidthDegs = beamWidthDegs;
        this.spacingDegrees = azimuthalSpacing;
        this.nyquistMetersPerSecond = nyquist;
        this.index = i;
    }

    /** puts the given angle in the range [0,360) */
    public static float normalizeDegrees(float degs) {
        // in range [0,360)
        if (degs < 0) {
            degs += 360;
        } else if (degs >= 360) {
            degs -= 360;
        }
        return degs;
    }

    public boolean contains(float az) {
        // returns range [0,360)
        float diff = normalizeDegrees(az - degrees);
        return (diff < spacingDegrees);
    }

    public Array1D<Float> getValues() {
        return array;
    }

    /** Set an index value.  Used for ordering within a RadialSet */
    public void setIndex(int i) {
        index = i;
    }

    /** Get index value within a RadialSet */
    public int getIndex() {
        return index;
    }

    /** Get the start degrees of the Radial.  Might not be normalized */
    public float getStartDegrees() {
        return degrees;
    }

    /** Get the start radians of the Radial.  Might not be normalized */
    public float getStartRadians() {
        return radians;
    }

    /**
     * Get the end degrees of the Radial.  Typically start degrees plus the spacing.
     */
    public float getEndDegrees() {
        return (degrees + spacingDegrees);
    }

    /**
     * Get the end radians of the Radial.  Typically start degrees plus the spacing.
     */
    public float getEndRadians() {
        return (float) Math.toRadians(getEndDegrees());
    }

    /**
     * Get the mid degrees of the Radial.
     */
    public float getMidDegrees() {
        return (float) (degrees + 0.5 * spacingDegrees);
    }


    // Size

    /** Get the spacing or span in degrees from start angle to end angle.
     The degree area that the arc of the data covers
     */
    public float getSpacingDegrees() {
        return spacingDegrees;
    }

    /** Beam width of the radial in degrees, always positive */
    public float getBeamWidthDegrees() {
        return beamWidthDegs;
    }

    /** in meters */
    public float getGateWidthKms() {
        return gateWidthKms;
    }

    public float getValue(int index) {
        return array.get(index);
    }

    public int getNumGates() {
        return array.size();
    }

    /** in m/s */
    public float getNyquistMetersPerSecond() {
        return nyquistMetersPerSecond;
    }

    /** debugging output */
    public String toStringDB() {
        String s = "Radial " + degrees + " to " + getEndDegrees() + " deg"
                + " " + gateWidthKms + "km " + " first 10 values: \n";
        StringBuilder buf = new StringBuilder(s);
        for (int i = 0; i < 10; ++i) {
            s += getValue(i) + " ";
            buf.append(getValue(i));
            buf.append(' ');
        }
        buf.append('\n');
        s = buf.toString();
        return (s);
    }
}
