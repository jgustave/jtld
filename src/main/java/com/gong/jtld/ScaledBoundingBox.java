package com.gong.jtld;

/**
 *
 */
public class ScaledBoundingBox extends BoundingBox {

    public final int scaleIndex;

    public ScaledBoundingBox (float x1, float y1, float x2, float y2, int scaleIndex) {
        super( x1, y1, x2, y2);
        this.scaleIndex = scaleIndex;
    }

    public int getScaleIndex () {
        return scaleIndex;
    }
}
