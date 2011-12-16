package com.gong.jtld.test;

import com.googlecode.javacv.cpp.opencv_video;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
//import static com.googlecode.javacv.cpp.opencv_imgproc.cvFindCornerSubPix;
//import static com.googlecode.javacv.cpp.opencv_imgproc.cvGoodFeaturesToTrack;
import static com.googlecode.javacv.cpp.opencv_video.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;

/**
 * LD_LIBRARY_PATH
 * System.getProperty("java.library.path");
 * export LD_LIBRARY_PATH=/opt/local/lib:$LD_LIBRARY_PATH
 * java -Djava.library.path=".:/opt/local/lib" TestApp
 * /opt/local/lib
 * /opt/local/lib/libopencv_core.2.3.dylib
 *
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 *
 */
public class Test2 {
        private static final int MAX_CORNERS = 100;

    public static void main( String[] args ){
        System.out.println("Hello!");
        System.out.println("Path:" + System.getProperty("java.library.path") );

        IplImage image1 = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage image2 = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00006.png", CV_LOAD_IMAGE_GRAYSCALE);


        //CvSize img_sz = cvGetSize(image1);

        //Size of window used to avoid aperature problem
        int win_size = 15;

        // IplImage imgC = cvLoadImage("OpticalFlow1.png",
        // CV_LOAD_IMAGE_UNCHANGED);
        IplImage imgC = cvLoadImage(
                "/Users/jerdavis/devhome/jtld/images/00005.png",
                CV_LOAD_IMAGE_UNCHANGED);

        // Get the features for tracking
//        IplImage eig_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);
//        IplImage tmp_image = cvCreateImage(img_sz, IPL_DEPTH_32F, 1);


        int[] corner_count = { MAX_CORNERS };

        CvPoint2D32f cornersA = new CvPoint2D32f(MAX_CORNERS);
        CvPoint2D32f cornersB = new CvPoint2D32f(MAX_CORNERS);
        CvPoint2D32f cornersC = new CvPoint2D32f(MAX_CORNERS);

        BoundingBox bb1 = new BoundingBox(300,30,335,105);
        cornersA = bb1.getInnerGridPoints(10,10,5);

//        CvArr mask = null;
//        cvGoodFeaturesToTrack( image1,
//                               eig_image,
//                               tmp_image,
//                               cornersA,
//                               corner_count,
//                               0.05,
//                               5.0,
//                               mask,
//                               3, //corner count
//                               0, //quality level
//                               0.04); //min distance

//optional: doesn't seem to help...
//        cornersA.position(0);
//        cvFindCornerSubPix( image1,
//                            cornersA,
//                            corner_count[0],
//                            cvSize(win_size, win_size),
//                            cvSize(-1, -1),
//                            cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03));
//        cornersA.position(0);

        //Initialize cornersC to cornersA to help LK
        for( int x=0;x<MAX_CORNERS;x++) {
            cornersB.position(x).set(cornersA.position(x));
            cornersC.position(x).set(cornersA.position(x));
        }
        cornersA.position(0);
        cornersB.position(0);
        cornersC.position(0);

        // Call Lucas Kanade algorithm
        byte[]  features_found = new byte[MAX_CORNERS];
        float[] feature_errors = new float[MAX_CORNERS];
        byte[]  features_found_back = new byte[MAX_CORNERS];
        float[] feature_errors_back = new float[MAX_CORNERS];
        //Original -JD
        //CvSize pyr_sz = cvSize(image1.width() + 8, image2.height() / 3);

        //CvSize pyr_sz = cvSize(image1.width(), image1.height() );

        //remember 1 channel is grayscale
        IplImage pyrA = cvCreateImage(cvSize(image1.width(), image1.height() ),
                                      IPL_DEPTH_32F,
                                      1);
        IplImage pyrB = cvCreateImage(cvSize(image2.width(), image2.height() ),
                                      IPL_DEPTH_32F,
                                      1);




        cvCalcOpticalFlowPyrLK(image1,
                               image2,
                               pyrA,
                               pyrB,
                               cornersA, //vector of points for which flow needs to be found. **** bb_points(BB1,10,10,5)
                               cornersB, //OUTPUT of calculated new points.
                               corner_count[0], //number of elements in corners
                               cvSize(win_size, win_size),//window, width,height
                               5, //max pyramids to use.
                               features_found,
                               feature_errors,
                               cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03),
                               opencv_video.CV_LKFLOW_INITIAL_GUESSES );
                               //0); //opencv_video.CV_LKFLOW_INITIAL_GUESSES

        //Now calculate the optical flow in reverse.
        //If we get back(or close) to the original point, we have a high confidence in that result.
        cvCalcOpticalFlowPyrLK(image2,
                               image1,
                               pyrB,
                               pyrA,
                               cornersB, //vector of points for which flow needs to be found. **** bb_points(BB1,10,10,5)
                               cornersC, //OUTPUT of calculated new points.
                               corner_count[0], //number of elements in corners
                               cvSize(win_size, win_size),//window, width,height
                               5, //max pyramids to use.
                               features_found_back,
                               feature_errors_back,
                               cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03),
                               opencv_video.CV_LKFLOW_INITIAL_GUESSES | opencv_video.CV_LKFLOW_PYR_A_READY | opencv_video.CV_LKFLOW_PYR_B_READY );

        //amount of error when optically flowing the same points forward and back again.
        //small error is good
        float[] forwardBackardError = euclideanDistance( cornersA, cornersC, MAX_CORNERS );
        //correlation of 1 is good, 0 is bad.
        float[] normCrossCor        = normalizedCrossCorrelation( image1,
                                                                  image2,
                                                                  cornersA,
                                                                  cornersB,
                                                                  MAX_CORNERS,
                                                                  features_found,
                                                                  win_size,
                                                                  CV_TM_CCOEFF_NORMED );

        // Make an image of the results
        int numFound = 0;
        for (int i = 0; i < corner_count[0]; i++) {
            if (features_found[i] == 0 || feature_errors[i] > 100) {
                //System.out.println("Error is " + feature_errors[i] + "/n");
                continue;
            }
            //System.out.println("Got it/n");


            numFound++;
            cornersA.position(i);
            cornersB.position(i);
            cornersC.position(i);
            CvPoint p0 = cvPoint(Math.round(cornersA.x()), Math.round(cornersA.y()));
            CvPoint p1 = cvPoint(Math.round(cornersB.x()), Math.round(cornersB.y()));
            if (features_found[i] != 0 && feature_errors[i] < 100  || features_found_back[i] != 0  && feature_errors_back[i] != 0 ) {
//                if( Math.round(cornersA.position(i).x()) == Math.round(cornersC.position(i).x())  &&
//                    Math.round(cornersA.position(i).y()) == Math.round(cornersC.position(i).y())) {
                if( forwardBackardError[i] < 1 ) {
                    cvLine(imgC,
                       p0,
                       p1,
                       CV_RGB(255, 0, 0), 1, 8, 0);
                }
            }
            //System.out.println("From " + p0 + " to " + p1 );
        }
        System.out.println("Found:" + numFound + " Features");

        cvRectangle(
                imgC,
                cvPoint(Math.round(bb1.x1), Math.round(bb1.y1)),
                cvPoint(Math.round(bb1.x2), Math.round(bb1.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);

        for( int x=0;x<100;x++) {
            cornersA.position(x);
            cornersB.position(x);
            cornersC.position(x);
            //System.out.println("BAR: " + cornersA );
            if (features_found[x] != 0 && feature_errors[x] < 100  || features_found_back[x] != 0  && feature_errors_back[x] != 0 ) {

//                if( Math.round(cornersA.position(x).x()) == Math.round(cornersC.position(x).x())  &&
//                    Math.round(cornersA.position(x).y()) == Math.round(cornersC.position(x).y())) {
                if( forwardBackardError[x] < 1 ) {

                    cvCircle( imgC,
                              cvPoint(Math.round(cornersA.x()), Math.round(cornersA.y())),
                              1,
                              CV_RGB(0, 0, 255), 1, 8, 0 );
                //if( features_found[x] != 0  && features_found_back[x] != 0 ) {
                    cvCircle( imgC,
                              cvPoint(Math.round(cornersC.x()), Math.round(cornersC.y())),
                              1,
                              CV_RGB(255, 255, 0), 1, 8, 0 );
                    }

                System.out.println(" OrigPoint   : " + cvPoint(Math.round(cornersA.x()), Math.round(cornersA.y())) +
                                   " MovedTo     : " + cvPoint(Math.round(cornersB.x()), Math.round(cornersB.y())) +
                                   " ReturnTo    :" + cvPoint(Math.round(cornersC.x()), Math.round(cornersC.y())) );
            }
            //}
        }


        cvSaveImage(
                "/tmp/image0-1.png", imgC);
        cvNamedWindow( "LKpyr_OpticalFlow", 0 );
        cvShowImage( "LKpyr_OpticalFlow", imgC );


        cvWaitKey(0);

    }

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
     * @param numPoints
     * @param windowSize
     * @param matchMethod
     */

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
public static float[] normalizedCrossCorrelation(IplImage     currentImage,
                                   IplImage     nextImage,
                                   CvPoint2D32f pointsToFind,
                                   CvPoint2D32f foundPoints,
                                   int          numPoints,
                                   byte[]       featuresFound,
                                   int          windowSize,
                                   int          matchMethod) {

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

    private static class BoundingBox {
        public final float x1;
        public final float y1;
        public final float x2;
        public final float y2;

        private BoundingBox (float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            if( x2 < x1  ||  y2 < y1 ) {
                throw new RuntimeException("Not a BB");
            }
            if( x1< 0  ||  y1 < 0 ) {
                throw new RuntimeException("Not a BB");
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

        public CvPoint2D32f getInnerGridPoints( int numX, int numY, int margin ) {
            CvPoint2D32f result = new CvPoint2D32f(numX*numY);

            BoundingBox marginBox = innerBox(margin);
            float stepWidth  = marginBox.getWidth() / (numX - 1 );
            float stepHeight = marginBox.getHeight() / (numY - 1);

            for( int x=0;x<(numX*numY);x++) {
                result.position(x);
                result.set((marginBox.x1 + (stepWidth * (x%numX) )),
                           (marginBox.y1 + ((x/numY)*stepHeight) ) );
            }
            result.position(0);
            return( result );
        }
    }
}
