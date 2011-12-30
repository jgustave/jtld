package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

import java.util.ArrayList;
import java.util.List;

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
        if( numColumns <= 0 || numRows <= 0) {
            throw new RuntimeException("getInnerGridPoints too Small " + numRows + " " + numColumns );
        }
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
    public boolean isOutsideImage(int imageWidth, int imageHeight ) {
        return( x1 < 0 || y1 < 0 || x2 >= imageWidth  || y2 >= imageHeight );
    }
    public boolean isOutsideImage (opencv_core.IplImage image) {
        return( isOutsideImage( image.width(), image.height() ));
    }
    public String toString() {
        return( "BB:[" + x1 + "," + y1 + "] [" + x2 + "," + y2 + "]" );
    }

    /**
     * Return a pairwise overlap metric (from 0 to 1)
     * Result will be of length  (N*(N-1))/2
     * @param boundingBoxes
     * @return
     */
    public static double[] overlap(List<BoundingBox> boundingBoxes ) {
        return( null );
    }

    /**
     * Return N*M results.. I think N is going to be 1, M is whatever.
     * @param boundingBoxes1
     * @param boundingBoxes2
     * @return
     */
    public static double[] overlap(List<BoundingBox> boundingBoxes1, List<BoundingBox> boundingBoxes2 ) {
        return( null );
    }

    public float overlap( BoundingBox that ) {

        if( this.x1 > that.x2 ||
            this.y1 > that.y2 ||
            this.x2 < that.x1 ||
            this.y2 < that.y1 ) {
            return( 0 );
        }

        //+1 because we are talking about the actual rows/columns of an image. (or do we need this?)
        //and a line is width 0, but is one column/row
        float columnIntersection = Math.min( this.x2, that.x2 ) - Math.max( this.x1, that.x1 );
        float rowIntersection    = Math.min( this.y2, that.y2 ) - Math.max( this.y1, that.y1 );
        float intersectionArea   = columnIntersection * rowIntersection;
        float thisArea           = this.getArea();
        float otherArea          = that.getArea();

        return( intersectionArea / (thisArea + otherArea - intersectionArea) );

    }

    public float getArea() {
        //TODO: add one to w/l?
        return( (x2-x1) * (y2-y1) );
    }

    /**
     * Create a bunch of BB's that are all over the image space. We will use them to search around..etc
     * @param origBox
     * @param imageWidth
     * @param imageHeight
     * @param minWindowSize is the minimum dimension size (width or Height)
     * @return
     */
    public static List<ScanningBoundingBoxes> createTestBoxes(BoundingBox origBox,
                                                              double[] scales,
                                                              int imageWidth,
                                                              int imageHeight,
                                                              int minWindowSize) {
        List<ScanningBoundingBoxes> result = new ArrayList<ScanningBoundingBoxes>();
        double   shift  = 0.1;
        //We will scale the initial bounding box in various interesting ways.

//        //1.2^(-10 to 10)
//        double[] scales = { 0.16151,0.19381,0.23257,0.27908,0.33490,
//                            0.40188,0.48225,0.57870,0.69444,0.83333,
//                            1.00000,1.20000,1.44000,1.72800,2.07360,
//                            2.48832,2.98598,3.58318,4.29982,5.15978,6.19174 };

        int[] possibleWidths            = new int[scales.length];
        int[] possibleHeights           = new int[scales.length];
        double[] shiftedHW              = new double[scales.length];
        double[] shiftedHeights         = new double[scales.length];

        if( origBox.getWidth() < minWindowSize  ||  origBox.getHeight() < minWindowSize ) {
            throw new RuntimeException("Too Small" + origBox );
        }

        for( int x=0;x<scales.length;x++) {
            possibleWidths[x]           = (int)Math.round((origBox.getWidth()+1) * scales[x]);
            possibleHeights[x]          = (int)Math.round((origBox.getHeight()+1) * scales[x]);
            shiftedHeights[x]           = possibleHeights[x] * shift;
            shiftedHW[x]                = Math.min(possibleHeights[x], possibleWidths[x]) * shift;
        }

        List<Integer> lefts = new ArrayList<Integer>();
        List<Integer> tops  = new ArrayList<Integer>();
        //bbf = 2  2  Img.H   Img.W
        for( int x=0;x<scales.length;x++) {

            if( possibleWidths[x] < minWindowSize || possibleHeights[x] <minWindowSize ) {
                continue;
            }

            lefts.clear();
            tops.clear();

            double left = 2;
            int maxWidth  = imageWidth - possibleWidths[x]-1;
            int maxHeight = imageHeight - possibleHeights[x]-1;

            //This Offset is matlab rounding compat.. but in the end..
            //this won't matter.. optimize it later.
            while( left - (0.00001) <= maxWidth ) {
                lefts.add((int)Math.round(left));
                left += shiftedHeights[x];
            }

            double top = 2;
            while( top - (0.00001) <= maxHeight ) {
                tops.add((int)Math.round(top));
                top += shiftedHW[x];
            }
            ScanningBoundingBoxes subResult = new ScanningBoundingBoxes( //possibleHeights[x],
                                                                         //possibleWidths[x],
                                                                         lefts.size() );

            for( int i=0;i<tops.size();i++)
            {
                for( int j=0;j<lefts.size();j++) {
                    subResult.boundingBoxes.add(
                            new ScaledBoundingBox( (float) lefts.get(j),
                                                 (float) tops.get(i),
                                                 (float) (lefts.get(j) + possibleWidths[x] - 1),
                                                 (float) (tops.get(i) + possibleHeights[x] - 1),
                                                 x ));
                }
            }
            if( subResult.boundingBoxes.size() > 0 ) {
                result.add(subResult);
            }
        }
        return( result );

    }
    public static void main(String[] args ) {
        BoundingBox bb = new BoundingBox(1,1,30,30);
        List<ScanningBoundingBoxes> result = createTestBoxes(bb, Jtdl.SCALES, 75, 50, 24);
        System.out.println("Result " + result );
    }

    /**
     * Given a set of BB's, return the smallest "convex hull" BB that encapsulates them all.
     * @param boxes
     * @return
     */
    public static BoundingBox getHullBox (List<BoundingBox> boxes) {
        float minX1 = boxes.get(0).x1;
        float minY1 = boxes.get(0).y1;
        float maxX2 = boxes.get(0).x2;
        float maxY2 = boxes.get(0).y2;

        for( BoundingBox box : boxes ) {
            if( box.x1 < minX1 ) {
                minX1 = box.x1;
            }
            if( box.y1 < minY1 ) {
                minY1 = box.y1;
            }
            if( box.x2 > maxX2 ) {
                maxX2 = box.x2;
            }
            if( box.y2 > maxY2 ) {
                maxY2 = box.y2;
            }
        }
        return( new BoundingBox( minX1, minY1, maxX2, maxY2 ) );
    }

    public CvPoint2D32f toQuadrangle() {
        CvPoint2D32f srcPoints = new CvPoint2D32f(4);
//clockwise
        srcPoints.position(0).set( x1, y1);
        srcPoints.position(1).set( x1+getWidth(), y1);
        srcPoints.position(2).set( x1+getWidth(), y1 + getHeight());
        srcPoints.position(3).set( x1, y1 + getHeight());


//counterclock
//        srcPoints.position(0).set( x1, y1);
//        srcPoints.position(1).set( x1, y1 + getHeight());
//        srcPoints.position(2).set( x1+getWidth(), y1 + getHeight());
//        srcPoints.position(3).set( x1+getWidth(), y1);

//row major
//        srcPoints.position(0).set( x1, y1);
//        srcPoints.position(1).set( x1+getWidth(), y1 );
//        srcPoints.position(2).set( x1, y1 + getHeight());
//        srcPoints.position(3).set( x1+getWidth(), y1+getHeight());

//column Major
//        srcPoints.position(0).set( x1, y1);
//        srcPoints.position(1).set( x1, y1 + getHeight() );
//        srcPoints.position(2).set( x1+getWidth(), y1);
//        srcPoints.position(3).set( x1+getWidth(), y1+getHeight());

        srcPoints.position(0);
        return( srcPoints );
    }

    public CvPoint2D32f toQuadrangleBar() {
        CvPoint2D32f srcPoints = new CvPoint2D32f(4);
///clockwise
        srcPoints.position(0).set( x1+10, y1);
        srcPoints.position(1).set( x1-10+getWidth(), y1);
        srcPoints.position(2).set( x1+getWidth(), y1 + getHeight()-15);
        srcPoints.position(3).set( x1, y1 + getHeight());


//counterclock
//        srcPoints.position(0).set( x1, y1);
//        srcPoints.position(1).set( x1, y1 + getHeight());
//        srcPoints.position(2).set( x1+getWidth(), y1 + getHeight());
//        srcPoints.position(3).set( x1+getWidth(), y1);

//row major
//        srcPoints.position(0).set( x1, y1);
//        srcPoints.position(1).set( x1+getWidth(), y1 );
//        srcPoints.position(2).set( x1, y1 + getHeight());
//        srcPoints.position(3).set( x1+getWidth(), y1+getHeight());

//column Major
//        srcPoints.position(0).set( x1, y1);
//        srcPoints.position(1).set( x1, y1 + getHeight() );
//        srcPoints.position(2).set( x1+getWidth(), y1);
//        srcPoints.position(3).set( x1+getWidth(), y1+getHeight());

        srcPoints.position(0);
        return( srcPoints );
    }

    public CvPoint2D32f getCenter () {
        CvPoint2D32f result = new CvPoint2D32f(1);
        result.set(x1 + (getWidth()*0.5f),
                   y1 + (getHeight()*0.5f) );
        return( result );
    }


    public CvSize getSize () {
        CvSize size = new CvSize(1);
        size.width((int)(getWidth()+0.5f));
        size.height((int) (getHeight() + 0.5f));
        return( size );
    }

    public CvRect getRect () {
        return( cvRect((int)(x1+0.5f),(int)(y1+0.5f),(int)(getWidth()+0.5f), (int)(getHeight()+0.5f) ) );
    }
}
