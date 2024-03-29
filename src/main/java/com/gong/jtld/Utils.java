package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_imgproc;
import org.apache.commons.math.stat.descriptive.rank.Median;

import java.nio.*;
import java.util.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

/**
 *
 */
public class Utils {
    public static final int NO_FLIP = 0;
    public static final int X_FLIP  = 1;
    public static final int Y_FLIP  = 2;

//    private static final float DEGREE_TO_RADIAN = (2f*(float)Math.PI)/360f;
//    private static final Random rand = new Random();

    /**
     * Median, but ignore NAN's
     */
    public static double medianIgnoreNan( float[] origValues ){
        Median  median  = new Median();
        int     count   = 0;
        int     x       = 0;

        double[] values = null;
        for (float origValue : origValues) {
            if (!Float.isNaN(origValue)) {
                count++;
            }
        }

        values = new double[count];

        for (float origValue : origValues) {
            if (!Float.isNaN(origValue)) {
                values[x] = origValue;
                x++;
            }
        }

        return( median.evaluate(values) );
    }
    public static double medianIgnoreNan( double[] origValues ){
        Median  median  = new Median();
        int     count   = 0;
        int     x       = 0;

        double[] values = null;
        for (double origValue : origValues) {
            if (!Double.isNaN(origValue)) {
                count++;
            }
        }

        values = new double[count];

        for (double origValue : origValues) {
            if (!Double.isNaN(origValue)) {
                values[x] = origValue;
                x++;
            }
        }

        double result = median.evaluate(values);
        return( result );
    }

    public static CvMat toMatrix( CvPoint2D32f points, int numPoints ) {
        CvMat result = CvMat.create( numPoints, 2 );
        for( int i=0;i<numPoints;i++) {
            points.position(i);
            result.put(i,0,points.x() );
            result.put(i,1,points.y() );
        }
        return(result);
    }


    /**
     * Calculate the pairwise distance for the given matrix using euclidean distance
     * @param matrix1 Rows are observations, columns are variables.. So a set of points would be
     * X1,Y1
     * X2,Y2
     * X3,Y3  ..etc  M rows by N columns
     * @return an array (1DMatrix) with (M*(M-1)) / 2 entries.
     * So for M = 4, you get: (4*3)/2 = 6
     * which corresponds to comparisons with rows: {1-2,1-3,1-4,2-3,2-4,3-4}
     * Euclidean Distance is: Sqrt( dx^2 + dy^2 )
     */
    public static CvMat pairwiseDistance(CvMat sourceMatrix) {
        int   numEntries = (sourceMatrix.rows()*(sourceMatrix.rows()-1))/2;
        CvMat result     = CvMat.create(numEntries,1);

        //Compare Row i to Row j
        int count = 0;
        for( int i=0;i<sourceMatrix.rows();i++) {
            for( int j=i+1;j<sourceMatrix.rows();j++) {
                double deltaX = sourceMatrix.get(i,0) - sourceMatrix.get(j,0);
                double deltaY = sourceMatrix.get(i,1) - sourceMatrix.get(j,1);
                deltaX *= deltaX;
                deltaY *= deltaY;
                result.put( count,0,Math.sqrt(deltaX + deltaY) );
                count++;
            }
        }

        return( result );
    }

    /**
     * Create a matrix but only include the specified rows.
     * @param origMatrix
     * @param validRows
     * @return
     */
    public static CvMat subMatrix( CvMat origMatrix, int[] validRows ) {
        CvMat result = CvMat.create(validRows.length, origMatrix.cols());
        for( int i=0;i<validRows.length;i++) {
            for( int j=0;j<origMatrix.cols();j++) {
                result.put(origMatrix.get(validRows[i],j));
            }
        }
        return( result );
    }

    /**
     * Find the euclidean distance between points
     * @param points1
     * @param points2
     * @param numPoints
     * @return
     */
    public static float[] euclideanDistance (CvPoint2D32f points1, CvPoint2D32f points2, int numPoints ) {
        float[] result = new float[numPoints];

        for( int x=0;x<numPoints;x++){
            points1.position(x);
            points2.position(x);
            result[x] = (float) Math.sqrt( ((points1.x() - points2.x()) * (points1.x() - points2.x())) +
                                           ((points1.y() - points2.y()) * (points1.y() - points2.y())) );
        }
        return( result );
    }

    public static float[] getPatchPattern( IplImage image,
                                           BoundingBox boundingBox,
                                           int patternSize,
                                           int flipStyle) {
        float[]  result = null;

        //extract the image patch from inside the bounding box.
        IplImage patch  = getImagePatch( image, boundingBox, flipStyle );

        result = getPattern( patch, patternSize );

        //BUGBUG: releasing seems to cause memory issues but probably leaks
//        cvReleaseImage( patch );
        return( result );

    }

    /**
     * Create a
     * @param image
     * @param boundingBox
     * @param patternSize
     * @return
     */
    public static float[] getPatchPattern( IplImage image, BoundingBox boundingBox, int patternSize ) {
        return( getPatchPattern( image, boundingBox, patternSize, NO_FLIP ) );
    }

    /**
     * Extract the image patch specified by the BB
     * @param image
     * @param boundingBox
     * @return
     */
    public static IplImage getImagePatch( IplImage image, BoundingBox boundingBox) {
        return( getImagePatch( image, boundingBox, NO_FLIP ) );
    }
    public static IplImage getImagePatch( IplImage image, BoundingBox boundingBox, int flipStyle ) {

        //Not sure if I want to round/ceil/floor
        IplImage dest = cvCreateImage( cvSize((int)Math.ceil(boundingBox.getWidth()),
                                              (int)Math.ceil(boundingBox.getHeight())),
                                       image.depth(),
                                       image.nChannels() );
        CvPoint2D32f center = boundingBox.getCenter();
        cvGetRectSubPix( image, dest, center );
        center.deallocate();

        if(flipStyle == NO_FLIP ) {
            return(dest);
        }else if( flipStyle == X_FLIP ) {
            IplImage flipImage = dest.clone();
            cvFlip(dest, flipImage, 0 );
            cvReleaseImage( dest );
            return(flipImage );
        }else if( flipStyle == Y_FLIP ) {
            IplImage flipImage = dest.clone();
            cvFlip(dest, flipImage, 1 );
            cvReleaseImage( dest );
            return(flipImage );
        }else {
            throw new RuntimeException("Bad flip:" + flipStyle);
        }
    }

    /**
     * Pattern is used in pattern matching on image.
     * An image is scaled to a particular size.
     * And then we take the Zero Mean And Unit Variance.
     * I assume this is going to be used on a GRAY scale image.
     * Float is fine because we are taking an 8bit value and
     * just subtracting the mean..
     * @param image
     * @return
     */
    public static float[] getPattern( IplImage image, int patternSize ) {
        //resize the image
        //cast to double array
        //get mean of double array
        //subtract mean from orig double array

        IplImage destImage = cvCreateImage(cvSize(patternSize,patternSize),
                                           image.depth(),
                                           image.nChannels() );

        cvResize( image, destImage, opencv_imgproc.CV_INTER_LINEAR );

        ByteBuffer  bb       = destImage.getByteBuffer();
        //BUGBUG: Not totally sure if there isn't an alignment issue...
        int         numBytes = patternSize * patternSize * (image.depth()/8) * image.nChannels();
        float[]     data     = new float[ numBytes ];
        long        sum      = 0;
        for( int x=0;x<numBytes;x++) {
            int temp = (int)(bb.get(x) & 0x000000FF);
            sum += temp;
            data[x] = temp;
        }

        float  meanVal = (float)sum / (float)numBytes;

        for( int x=0;x<data.length;x++) {
            data[x] -= meanVal;
        }
        cvReleaseImage(destImage);

        return( data );
    }


    /**
     *
     * This helps with brightness changes from frame to frame
     * http://en.wikipedia.org/wiki/Cross-correlation
     * http://opencv.itseez.com/doc/tutorials/imgproc/histograms/template_matching/template_matching.html
     * @param currentImage
     * @param nextImage
     * @param pointsToFind
     * @param foundPoints
     * @param numPoints
     * @param featuresFound are the foundPoints valid?
     * @param windowSize
     * @param matchMethod
     */
    public static float[] normalizedCrossCorrelation(IplImage currentImage,
                                   IplImage nextImage,
                                   CvPoint2D32f pointsToFind,
                                   CvPoint2D32f foundPoints,
                                   int          numPoints,
                                   byte[]       featuresFound,
                                   int          windowSize,
                                   int          matchMethod) {
//TODO: cleanup
    float[] result          = new float[numPoints];
    //Why8 bit? why not 32 bit?
	IplImage sourceRect     = cvCreateImage( cvSize(windowSize, windowSize), 8, 1 );
	IplImage templateRect   = cvCreateImage( cvSize(windowSize, windowSize), 8, 1 );
    //we only want the one point.
	IplImage resultRect     = cvCreateImage( cvSize( 1, 1 ), IPL_DEPTH_32F, 1 );

	for (int i = 0; i < numPoints; i++) {
		if (featuresFound[i] != 0) {

            pointsToFind.position(i);
            foundPoints.position(i);
			cvGetRectSubPix( currentImage, sourceRect, pointsToFind );
			cvGetRectSubPix( nextImage, templateRect, foundPoints );

            //Search on sourceRect,
            //Search for templateRect,
            //get back resultRect
            //using method
			cvMatchTemplate( sourceRect, templateRect, resultRect, matchMethod );
            result[i] = resultRect.getFloatBuffer(0).get();

		} else {
			result[i] = 0.0f;
		}
	}
	cvReleaseImage( sourceRect );
	cvReleaseImage( templateRect );
	cvReleaseImage( resultRect );

    return( result );
}

    public static double[] getColumnSlice (CvMat sourceMatrix, int column) {
        double[] result = new double[sourceMatrix.rows()];
        for( int i=0;i<sourceMatrix.rows();i++) {
            result[i] =  sourceMatrix.get(i,column);
        }
        return( result );
    }
//
//    public static IplImage warpImage (IplImage    currentGray,
//                                      BoundingBox boundingBox) {
//
//        //For init: 'noise',5,'angle',20,'shift',0.02,'scale',0.02
//        //For Update: 'noise',5,'angle',10,'shift',0.02,'scale',0.02
//
//        int noise = 5;
//        int angle = 20;
//        float shift = 0.02f;
//        float scale = 0.02f;
//        float centerX = 0.5f*(boundingBox.x1 + boundingBox.x2);
//        float centerY = 0.5f*(boundingBox.y1 + boundingBox.y2);
//
//        CvMat resultMatrix = CvMat.create(3,3,CV_64F);
//        CvPoint2D32f srcPoints = boundingBox.toQuadrangle();
//
//        //grow/shrink H/W
//        //Rotate L/R
//        CvPoint2D32f destPoints = boundingBox.toQuadrangleBar();
//
//
//
//        cvGetPerspectiveTransform( srcPoints, destPoints, resultMatrix );
//
//        System.out.println("MAT:\n" + resultMatrix );
//
//        IplImage    dest = currentGray.clone();
//        cvWarpPerspective( currentGray, dest, resultMatrix );
//
//        //TODO: add a constant noise to entire img.
//
//        return dest;  //To change body of created methods use File | Settings | File Templates.
//    }
//
//
//
//    /**
//     * Meh!
//     * @param currentGray
//     * @param boundingBox
//     * @return
//     */
//    public static IplImage warpImage2 (IplImage    currentGray,
//                                      BoundingBox boundingBox) {
//
//        //For init: 'noise',5,'angle',20,'shift',0.02,'scale',0.02
//        //For Update: 'noise',5,'angle',10,'shift',0.02,'scale',0.02
//
//        int noise = 5;
//        int angle = 20;
//        float shift = 0.02f;
//        float scale = 0.02f;
//        float centerX = 0.5f*(boundingBox.x1 + boundingBox.x2);
//        float centerY = 0.5f*(boundingBox.y1 + boundingBox.y2);
//
//        CvMat shift1 = CvMat.create(3,3,CV_32F);
//        shift1.put(1f,0f,-centerX,
//                   0f,1f,-centerY,
//                   0f,0f,1f);
////        shift1.put(1f,0f,0f,
////                   0f,1f,0f,
////                   0f,0f,1f);
//
//
//        System.out.println("shift1\n" + shift1);
//
//        float scaleAmnt = 1f-scale*(rand.nextFloat()-0.5f);
//        CvMat scaleMat  = CvMat.create(3,3,CV_32F);
//        scaleMat.put(scaleAmnt,0f,0f,
//                     0f,scaleAmnt,0f,
//                     0f,0f,1f);
//
//        System.out.println("scale\n" + scaleMat);
//
//        float angleAmnt = DEGREE_TO_RADIAN * angle * (rand.nextFloat()-0.5f);
//        float cosineAmnt = (float)Math.cos(angleAmnt);
//        float sinAmnt    = (float)Math.sin(angleAmnt);
//        CvMat angleMat  = CvMat.create(3,3,CV_32F);
//        angleMat.put(cosineAmnt,-sinAmnt,0f,
//                     sinAmnt,cosineAmnt,0f,
//                     0f,0f,1f );
//        System.out.println("angle\n" + angleMat);
//
//
//        float shiftRight  = shift * boundingBox.getHeight() * (rand.nextFloat()-0.5f);
//        float shiftCenter = shift * boundingBox.getWidth() * (rand.nextFloat()-0.5f);
//
//        CvMat shift2  = CvMat.create(3,3,CV_32F);
//        shift2.put(1f,0f,shiftCenter,
//                     0, 1,shiftRight,
//                     0f,0f,1f );
//        System.out.println("shift2\n" + shift2);
//
//        CvMat warpMat = CvMat.create(3,3,CV_32F);
//        CvMat tempMat = CvMat.create(3,3,CV_32F );
//
//        cvMatMul(shift1,scaleMat,tempMat);
//
//        cvMatMul(tempMat,angleMat,warpMat);
//        cvMatMul(warpMat,shift2,tempMat);
//
//        cvMatMul(tempMat,tempMat,warpMat);
//        CvMat warpInvMat = CvMat.create(3,3,CV_32F);
//        cvInvert( warpMat, warpInvMat );
//
//
////MAT:
////[ 3.5447762, -6.616915, -55.22388
////  0.2238806, 0.41791046, -1.3432835
////  0.0074626864, -0.020895522, 1.0 ]
//
////        warpMat.put(0.9769908, 0.22125624, -55.4906,
////                    -0.22125624, 0.9769908, -1.3119,
////                    0.0, 0.0, 1.0 );
//
////        warpMat.put( 3.5447762, -6.616915, -55.22388,
////                     0.2238806, 0.41791046, -1.3432835,
////                     0.0074626864, -0.020895522, 1.0 );
//
//
//        System.out.println("warp\n" + warpMat);
//
////   //width
////    bbW = bb_width(bb)-1;
////    //height
////    bbH = bb_height(bb)-1;
////
////    //seems to be the bb centered over the origin.
////    box = [-bbW/2 bbW/2 -bbH/2 bbH/2];
////
//
//
//        IplImage    dest = currentGray.clone();
//        //IplImage    dest = cvCreateImage(cvSize(100,100),8,1);
//
////0.97849    0.15052  -61.16216
////   -0.15052    0.97849  -65.37352
////    0.00000    0.00000    1.00000
//
//        //cvMatMul(src,src,dst);
//
//        //Might consider64f
////        CvMat matrix = CvMat.create(3,3,CV_32F);
////        matrix.put(0.97849,0.15052,-1.16216,
////                   -0.15052,0.97849,-5.37352,
////                   0.00000,0.00000,1.00000);
////        System.out.println("" + matrix );
//        //matrix.p
//        //cvWarpPerspective( Utils.getImagePatch( currentGray, boundingBox ), dest, matrix );
//        cvWarpPerspective( currentGray, dest, warpMat );
//
//        return dest;  //To change body of created methods use File | Settings | File Templates.
//    }


    public static double normalizedCorrelation( double[] patch1, double[] patch2 ) {

        double correlation = 0;
        double norm1       = 0;
        double norm2       = 0;

        if( patch1.length != patch2.length ){
            throw new RuntimeException("Patches not same size");
        }

        for( int x=0;x<patch1.length;x++) {
            correlation += patch1[x] * patch2[x];
            norm1 += patch1[x] * patch1[x];
            norm2 += patch2[x] * patch2[x];
        }

        return( ((correlation / Math.sqrt(norm1*norm2)) + 1) / 2.0 );
    }

    /**
     * compare mainPatch against all other patches
     * @param patch1
     * @param patches
     * @return
     */
    public static double[] normalizedCorrelation( float[] mainPatch, List<float[]> patches ) {
        double[] result = new double[patches.size()];
        for( int x=0;x<patches.size();x++) {
            result[x] = normalizedCorrelation(mainPatch, patches.get(x));
        }
        return( result );
    }
    /**
     * Not sure about precision yet.
     * @param patch1
     * @param patch2
     * @return
     */
    public static double normalizedCorrelation( float[] patch1, float[] patch2 ) {

        double correlation = 0;
        double norm1       = 0;
        double norm2       = 0;

        if( patch1.length != patch2.length ){
            throw new RuntimeException("Patches not same size");
        }

        //I wonder if we should/can do a recursive version to increase percision?
        for( int x=0;x<patch1.length;x++) {
            correlation += (double)patch1[x] * (double)patch2[x];
            norm1 += (double)patch1[x] * (double)patch1[x];
            norm2 += (double)patch2[x] * (double)patch2[x];
        }
        //opencv_features2d.SiftDescriptorExtractor fl;
        //fl.compute();

        return( (((correlation / Math.sqrt(norm1*norm2)) + 1.0) / 2.0) );
    }

    public static float max (float[] vals) {
        float max = vals[0];
        for( float val : vals ) {
            if( val > max ) {
                max = val;
            }
        }
        return( max );
    }
    public static float max (float[] vals, int limit) {
        float max = vals[0];
        for( int x=0;x<limit;x++) {
            float val = vals[x];
            if( val > max ) {
                max = val;
            }
        }
        return( max );
    }
    public static double max (double[] vals) {
        double max = vals[0];
        for( double val : vals ) {
            if( val > max ) {
                max = val;
            }
        }
        return( max );
    }
    public static double max (double[] vals, int limit) {
        double max = vals[0];
        for( int x=0;x<limit;x++) {
            double val = vals[x];
            if( val > max ) {
                max = val;
            }
        }
        return( max );
    }

    /**
     * Get the N+1 most overlapping BB's.. +1 for the best.. and N for the rest.
     * @param boundingBox
     * @param testBoundingBoxes
     * @return
     */
    public static List<ScaledBoundingBox> getBestOverlappingScanBoxes( BoundingBox boundingBox,
                                                                       ScaledBoundingBox[] testBoundingBoxes,
                                                                       int maxResults,
                                                                       float minCutoff ) {
        //TODO: treemap inefficient ... create static Object(Float,BB) and Arrays.sort
        TreeMap<Float,List<ScaledBoundingBox>> closestMap = new TreeMap<Float, List<ScaledBoundingBox>>();

        for( ScaledBoundingBox box : testBoundingBoxes ) {
            float overlap = boundingBox.overlap(box);
            if( overlap > minCutoff ) {
                List<ScaledBoundingBox> boxList = closestMap.get(overlap);
                if( boxList == null ) {
                    boxList = new ArrayList<ScaledBoundingBox>();
                    closestMap.put( overlap, boxList );
                }
                boxList.add( box );
            }
        }

        //Now we reduce to the maxBoxes
        List<ScaledBoundingBox> closestList = new ArrayList<ScaledBoundingBox>();
        while(closestList.size() < maxResults ) {
            if( closestMap.size() == 0 ) {
                break;
            }
            closestList.addAll( closestMap.pollLastEntry().getValue() );
        }
        return( closestList.subList(0, Math.min( closestList.size(), maxResults ) ) );
    }

    /**
     * Get ALL of the worst overlapping BB's that are < than maxcutoff overlap
     * We will shuffle and select later
     * @param boundingBox
     * @param testBoundingBoxes
     * @param maxCutoff
     * @return
     */
    public static List<ScaledBoundingBox> getWorstOverlappingScanBoxes (BoundingBox boundingBox,
                                                                        ScaledBoundingBox[] testBoundingBoxes,
                                                                        float maxCutoff ) {


        List<ScaledBoundingBox> worstList = new ArrayList<ScaledBoundingBox>(256);

        //Look at every Scan.BB
        for( ScaledBoundingBox box : testBoundingBoxes ) {
            float overlap = boundingBox.overlap(box);
            if( overlap < maxCutoff ) {
                worstList.add( box );
            }
        }
        return( worstList );
    }

    public static float min (float[] vals) {
        float min = vals[0];
        for( float val : vals ) {
            if( val < min ) {
                min = val;
            }
        }
        return( min );
    }

    public static double getVariance( BoundingBox boundingBox, CvMat sum, CvMat squareSum ){

        double bottomRightSum       = sum.get( (int)(boundingBox.y2),
                                               (int)(boundingBox.x2) );
        double bottomLeftSum        = sum.get( (int)(boundingBox.y2),
                                               (int)(boundingBox.x1) );
        double topRightSum          = sum.get( (int)(boundingBox.y1),
                                               (int)(boundingBox.x2) );
        double topLeftSum           = sum.get( (int)(boundingBox.y1),
                                               (int)(boundingBox.x1) );
        double bottomRightSquareSum = squareSum.get( (int)(boundingBox.y2),
                                                     (int)(boundingBox.x2) );
        double bottomLeftSquareSum  = squareSum.get( (int)(boundingBox.y2),
                                                     (int)(boundingBox.x1) );
        double topRightSquareSum    = squareSum.get( (int)(boundingBox.y1),
                                                     (int)(boundingBox.x2) );
        double topLeftSquareSum     = squareSum.get( (int)(boundingBox.y1),
                                                     (int)(boundingBox.x1) );

        double mean         = (bottomRightSum+topLeftSum-topRightSum-bottomLeftSum) / ((double) boundingBox.getArea());
        double squareMean   = (bottomRightSquareSum+topLeftSquareSum-topRightSquareSum-bottomLeftSquareSum) / ((double) boundingBox.getArea());
        //so the variance is the difference in the integrated squareMean and meanSquare
        return( squareMean - mean * mean);
    }
}
