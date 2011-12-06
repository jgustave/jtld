package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import org.apache.commons.math.stat.descriptive.rank.Median;

import java.util.Arrays;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetRectSubPix;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

/**
 *
 */
public class Utils {


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

//    public static void main(String[] args) {
//
//        float[] vals = {1f,2f,3f,4f,5f,6f,7f};
//        System.out.println("" + medianIgnoreNan(vals));
//
//        float[] vals2 = {1f,2f,4f,5f,6f,7f};
//        System.out.println("" + medianIgnoreNan(vals2) );
//
//        float[] vals3 = {1f,2f,Float.NaN,4f,5f,6f,7f};
//        System.out.println("" + medianIgnoreNan(vals3) );
//    }

    public static void main( String[] args ) {
        CvMat mat1 = CvMat.create(3,2);
        CvMat mat2 = CvMat.create(3,2);
        CvMat mat3 = CvMat.create(3,2);
        CvMat mat4 = CvMat.create(3,2);


        mat1.put(0,0,5d);
        mat1.put(0,1,2);
        mat1.put(1,0,5);
        mat1.put(1,1,7);
        mat1.put(2,0,9);
        mat1.put(2,1,10);

        mat2.put(0,0,15);
        mat2.put(0,1,16);
        mat2.put(1,0,17);
        mat2.put(1,1,18);
        mat2.put(2,0,19);
        mat2.put(2,1,15);


        //euclidean distance
        //Sqrt(deltaX^2 + deltaY)

        System.out.println("Hello:" + mat1 );
        System.out.println("Hello:" + mat2 );
        System.out.println("Byte: " + pairwiseDistance(mat1));
        System.out.println("hellodfdf:" + Arrays.toString(mat1.get()) );
        //cvSub(mat2, mat1, mat3, null); //  mat1 - mat2
        //cvDiv(mat1,mat2,mat3,1); //  mat1./mat2
        //System.out.println("Hello:" + mat3 );

//        int numRows = 4;
//        for( int x=0;x<numRows;x++) {
//            for( int y=x+1;y<numRows;y++) {
//                System.out.println("X:" + x + " Y:" + y);
//            }
//        }

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
}
