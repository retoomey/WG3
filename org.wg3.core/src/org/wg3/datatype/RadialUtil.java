package org.wg3.datatype;

import org.wg3.storage.Location;

/**
 * Static utilities for radial sets
 * 
 * @author Robert Toomey
 */
public class RadialUtil {

    static final float RAD = 0.017453293f;
    static final float Index_of_Refraction = 4.0f / 3.0f;
    static final float EarthRadius = 6371000.0f;
    static final double power1 = Math.pow(Index_of_Refraction * EarthRadius,
            2.0f);
    static final double IRE = Index_of_Refraction * EarthRadius;
    static final double IRE2 = 2.0 * IRE;

    /*
     * // Sources: Radar Theory: // Doviak and Zrnic, Doppler Radar and Weather
     * Observations, page 13 // Oblique Trigonometry: // Beyer, CRC Standard
     * Math Tables, 18th edition, page 148
     * 
     * // Converted from BeamPath_AzRangeToLL from c++ version // FIXME: This
     * ancient function should be updated public static Location
     * getAzRanElLocation( Location station, // Location of station float
     * azimuth, // at degree around radar float rangeKms, // at range in
     * kilometers float elevAngleRAD // at elevation (Radians) ){
     * 
     * double ret_lat = 0, ret_lon = 0, ret_ht = 0;
     * 
     * final float RAD = 0.017453293f; final float
     * Index_of_Refraction=4.0f/3.0f; final float EarthRadius = 6371000.0f;
     * float range = rangeKms*1000.0f; double stationLat =
     * station.getLatitude(); double stationLon = station.getLongitude();
     * 
     * // Height in meters ret_ht = (float) (Math.pow( Math.pow(range,2.0f)+
     * Math.pow(Index_of_Refraction*EarthRadius, 2.0f) + 2.0f * range *
     * Index_of_Refraction*EarthRadius* Math.sin(elevAngleRAD) ,
     * 0.5)-Index_of_Refraction*EarthRadius);
     * 
     * float great_circle_distance = (float) (Index_of_Refraction*EarthRadius*
     * Math.asin((range * Math.cos(elevAngleRAD))/
     * ((Index_of_Refraction*EarthRadius)+ret_ht)));
     * 
     * ret_lat =(90.0f -
     * (Math.acos((Math.cos(great_circle_distance/EarthRadius))*
     * (Math.cos(RAD*(90.0f-stationLat))) + (
     * (Math.sin(great_circle_distance/EarthRadius))* (
     * Math.sin(RAD*(90.0f-stationLat)))*(Math.cos(RAD*azimuth)))))/RAD);
     * 
     * float delta_longitude = (float)
     * (Math.asin((Math.sin(great_circle_distance/EarthRadius)) *
     * (Math.sin(RAD*azimuth))/(Math.sin(RAD*(90.0f-ret_lat)) )))/RAD; ret_lon =
     * stationLon+delta_longitude;
     * 
     * // Final height in Kms ret_ht = station.getHeightKms()+(ret_ht/1000.0);
     * 
     * if (ret_lon <= -180){ ret_lon += 360; } if (ret_lon > 180){ ret_lon -=
     * 360; } return new Location(ret_lat, ret_lon, ret_ht); }
     */
    public static void getAzRanElLocation(Location output, // buffer for output
            // (saves on newing)
            Location station, // Location of station
            double sinAzimuthRAD, // Precomputed azimuth sin and cos around
            // radar
            double cosAzimuthRAD, float rangeKms, // at range in kilometers
            double sinElevAngle, // sin of the elevation (precomputed)
            double cosElevAngle // cos of the elevation (precomputed)
            ) {
        // double elevAngleRAD = RAD*elevAngleDeg;
        double ret_lat, ret_lon, ret_ht;

        float range = rangeKms * 1000.0f;
        double stationLat = station.getLatitude();
        double stationLon = station.getLongitude();

        // Height in meters (Note this depends only on range and sin of
        // elevation angle,
        // so we only need to calculate it once
        ret_ht = (float) (Math.pow(Math.pow(range, 2.0f) + power1 + range
                * IRE2 * sinElevAngle, 0.5) - IRE);
        // ret_ht = 0;

        // GCD can also be per radial....
        float great_circle_distance = (float) (IRE * Math.asin((range * cosElevAngle) / (IRE + ret_ht)));

        double lat2 = (90.0f - stationLat) * RAD;

        double sinGcdER = Math.sin(great_circle_distance / EarthRadius);

        ret_lat = (90.0f - (Math.acos((Math.cos(great_circle_distance
                / EarthRadius))
                * (Math.cos(lat2))
                + ((sinGcdER) * (Math.sin(lat2)) * (cosAzimuthRAD))))
                / RAD);

        float delta_longitude = (float) (Math.asin((sinGcdER) * (sinAzimuthRAD)
                / (Math.sin(RAD * (90.0f - ret_lat)))))
                / RAD;

        ret_lon = stationLon + delta_longitude;

        // Final height in Kms
        ret_ht = station.getHeightKms() + (ret_ht / 1000.0);

        if (ret_lon <= -180) {
            ret_lon += 360;
        }
        if (ret_lon > 180) {
            ret_lon -= 360;
        }

        output.init(ret_lat, ret_lon, ret_ht);
        // return new Location(ret_lat, ret_lon, ret_ht);
    }

    /**
     * Calculate the attenuation height of a radial point
     * 
     * @param rangeMeters
     *            The range in meters from center of radar
     * @param sinElevAngle
     *            The sin of the elevation angle
     * @return the height at point
     */
    public static double getAzRanElHeight(double rangeMeters, // at range in
            // kilometers
            double sinElevAngle // sin of the elevation (pre-computed)
            ) {
        double ret_ht;

        //ret_ht = (float) (Math.pow(Math.pow(rangeMeters, 2.0f) + power1
        //		+ rangeMeters * IRE2 * sinElevAngle, 0.5) - IRE);

        // Straight range..no attenuation (matches with VSlice)
        ret_ht = sinElevAngle * rangeMeters;
        return ret_ht;
    }

    /**
     * Calculate the attenuation great circle distance of a radial point
     * 
     * @param rangeMeters
     *            The range in meters from center of radar
     * @param cosElevAngle
     *            The cos of the elevation angle
     * @return the height at point
     */
    public static double getGCD(double rangeMeters, double cosElevAngle, // cos
            // of
            // the
            // elevation
            // (pre-computed)
            double height) {
        return (IRE * Math.asin((rangeMeters * cosElevAngle) / (IRE + height)));
    }

    public static double getGCDSin(double great_circle_distance) {
        return (Math.sin(great_circle_distance / EarthRadius));
    }

    public static double getGCDCos(double great_circle_distance) {
        return (Math.cos(great_circle_distance / EarthRadius));
    }

    public static void getAzRan1(Location output, // buffer for output (saves on
            // newing)
            Location station, // Location of station
            double sinAzimuthRAD, // Precomputed azimuth sin and cos around
            // radar
            double cosAzimuthRAD, float rangeKms, // at range in kilometers
            double sinElevAngle, // sin of the elevation (precomputed)
            double cosElevAngle // cos of the elevation (precomputed)
            , double ret_ht, // height (cached)
            double gcdSin, double gcdCos) {

        double ret_lat, ret_lon;

        double stationLat = station.getLatitude();
        double stationLon = station.getLongitude();

        double lat2 = (90.0f - stationLat) * RAD;

        ret_lat = (90.0f - (Math.acos((gcdCos) * (Math.cos(lat2))
                + ((gcdSin) * (Math.sin(lat2)) * (cosAzimuthRAD))))
                / RAD);

        float delta_longitude = (float) (Math.asin((gcdSin) * (sinAzimuthRAD)
                / (Math.sin(RAD * (90.0f - ret_lat)))))
                / RAD;

        ret_lon = stationLon + delta_longitude;

        // Final height in Kms
        ret_ht = station.getHeightKms() + (ret_ht / 1000.0);

        if (ret_lon <= -180) {
            ret_lon += 360;
        }
        if (ret_lon > 180) {
            ret_lon -= 360;
        }

        output.init(ret_lat, ret_lon, ret_ht);
    }
}
