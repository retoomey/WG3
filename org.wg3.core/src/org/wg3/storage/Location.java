package org.wg3.storage;

/**
 * @author Robert Toomey
 * 
 */
public class Location {

    private double lat;
    private double lon;
    private double ht;
    /** in kilometers. */
    public static final double EarthRadius = 6380;

    /** lat, lon in degrees and height in kilometers. */
    public Location(double lat, double lon, double ht) {
        init(lat, lon, ht);
    }

    public final void init(double lat, double lon, double ht) {
       
        /** Force lat in range -90 to 89 */
        this.lat = (mod(lat+90, 180))-90;
        
        /** Force lon in range -180 to 179 */
        this.lon = (mod(lon+180, 360))-180.0;
        this.ht = ht;

        // Contours has location objects with height 0
        //if (lat < -90 || lat > 90 || lon < -180 || lon > 180 || ht < -0.01)
       // if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
       //     throw new IndexOutOfBoundsException("Invalid earth location" + this);
       // }

    }
    
    /** True modulus */
    private final double mod(double x, double y) {
        double result = x % y;
        if (result < 0) {
            result += y;
        }
        return result;
    }
    
    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lon;
    }

    public double getHeightKms() {
        return ht;
    }

    @Override
    public String toString() {
        return "[ " + lat + ", " + lon + ", " + ht + "]";
    }

}
