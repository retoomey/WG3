package org.wg3.storage;

/**
 *
 * @author Robert Toomey
 */
public class V2 {

    public final float x;
    public final float y;

    public V2(float xi, float yi) {
        x = xi;
        y = yi;
    }

    public V2(double xi, double yi) {
        x = (float) xi;
        y = (float) yi;
    }
}
