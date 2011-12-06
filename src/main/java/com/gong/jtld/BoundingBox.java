package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

/**
 *
 */
public class BoundingBox {

    public final float x1;
    public final float y1;
    public final float x2;
    public final float y2;

    public BoundingBox (float x1, float y1, float x2, float y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        if( x2 < x1  ||  y2 < y1 ) {
            throw new RuntimeException("Not a BB " + this);
        }
    }
    public float getWidth() {
        return( x2 - x1 );
    }
    public float getHeight() {
        return( y2 - y1 );
    }

    /**
     * Return an inner box by the given margin.
     * @param margin
     * @return
     */
    public BoundingBox innerBox( int margin ) {
        return new BoundingBox( x1 + margin,
                                y1 + margin,
                                x2 - margin,
                                y2 - margin );
    }

    /**
     * Create a Grid of points inside the bounding box.
     * @param numColumns
     * @param numRows
     * @param margin
     * @return
     */
    public CvPoint2D32f getInnerGridPoints( int numColumns, int numRows, int margin ) {
        CvPoint2D32f    result     = new CvPoint2D32f(numColumns*numRows);
        BoundingBox     marginBox  = innerBox(margin);
        float           stepWidth  = marginBox.getWidth() / (numColumns - 1 );
        float           stepHeight = marginBox.getHeight() / (numRows - 1);

        for( int x=0;x<(numColumns*numRows);x++) {
            result.position(x);
            result.set((marginBox.x1 + (stepWidth * (x%numColumns) )),
                       (marginBox.y1 + ((x/numRows)*stepHeight) ) );
        }
        result.position(0);
        return( result );
    }
    public String toString() {
        return( "BB:[" + x1 + "," + y1 + "] [" + x2 + "," + y2 + "]" );
    }
}
