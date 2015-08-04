package org.wg3.storage;

/**
 * Lat/Lon stored as degrees
 *  No radian is here to discourage transforming back and forth...doing this
 * a million times a second adds lag.
 * 
 * @author Robert Toomey
 */
public class LLD extends V2 {

    public LLD(LLD i){
        super(i.x, i.y);
    }
    
    public LLD(float lat, float lon) {
       super(lat, lon);
    }

    public LLD(double lat, double lon) {
        super(lat, lon);
    }
    
    public float latDegrees(){
        return x;
    }
    
    public float lonDegrees(){
        return y;
    }
}
