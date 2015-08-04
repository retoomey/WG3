package org.wg3.datatype;

/**
 *
 * @author Robert Toomey
 */
/**
 * A Cache of radial heights from center of radar outwards.  Used
 * to increase speed with radial sets.
 * 
 * RadialHeightGateCache.
 */
public class RadialATHeightGateCache {

    /**
     * Cache of height value for each gate
     */
    public double[] heights;
    /**
     * Cache of gcd of sin for each gate
     */
    public double[] gcdSinCache;
    /**
     * Cache of gcd of cos for each gate
     */
    public double[] gcdCosCache;
    /**
     * Size of us
     */
    public int size;

    /**
     * Create a height gate cache
     */
    public RadialATHeightGateCache(RadialSet set, Radial radial, int maxGateCount,
            double sinElevAngle, double cosElevAngle) {
        if (radial != null) {
            generateHeightForEachGate(set, radial, maxGateCount, sinElevAngle, cosElevAngle);
        }
    }

    /**
     * Generate the height for each gate
     */
    private void generateHeightForEachGate(RadialSet set, Radial aRadial, int maxGateCount, double sinElevAngle, double cosElevAngle) {
        // System.out.println("Begin height cache....");
        heights = new double[maxGateCount + 1];
        // gcdCache = new double[maxGateCount+1];
        gcdSinCache = new double[maxGateCount + 1];
        gcdCosCache = new double[maxGateCount + 1];
        size = maxGateCount + 1;
        double rangeMeters = set.getRangeToFirstGateKms() * 1000.0;
        double gateWidthMeters = aRadial.getGateWidthKms() * 1000.0;
        //	System.out.println("Gate width meters is "+gateWidthMeters);
        for (int i = 0; i <= maxGateCount; i++) {
            heights[i] = RadialUtil.getAzRanElHeight(rangeMeters,
                    sinElevAngle);
            double gcd = RadialUtil.getGCD(rangeMeters,
                    cosElevAngle, heights[i]);
            gcdSinCache[i] = RadialUtil.getGCDSin(gcd);
            gcdCosCache[i] = RadialUtil.getGCDCos(gcd);
            rangeMeters += gateWidthMeters;
        }
    }
}