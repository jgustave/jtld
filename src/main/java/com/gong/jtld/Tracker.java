package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_video;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_CCOEFF_NORMED;
import static com.googlecode.javacv.cpp.opencv_video.cvCalcOpticalFlowPyrLK;

/**
 * This implements the Optical Flow Tracker and uses the Lucas Kanade algorithm to
 * track a box between images.
 */
public class Tracker {

    //Where we are tracking from.
    private final CvPoint2D32f  currentPoints;
    //The Result.. Where we tracked to.
    private final CvPoint2D32f  nextPoints;
    //Now Track in reverse to see if we end up at start again.
    private final CvPoint2D32f  reversePoints;

    //Inside the bounding box, how many rows of tracked points are there?
    private final int           numTrackedRows;
    //Inside the bounding box, how many columns of tracked points are there?
    private final int           numTrackedColumns;
    private final int           trackMargin;
    private final int           windowSize;

    private final byte[]        featuresFound;
    private final float[]       featureErrors;
    private final byte[]        featuresFoundBack;
    private final float[]       featureErrorsBack;

    private       IplImage      pyramidA                = null;
    private       IplImage      pyramidB                = null;

    public Tracker() {
        this(15, 10, 10, 5);
    }

    public Tracker( int windowSize,
                    int numTrackedRows,
                    int numTrackedColumns,
                    int trackMargin ) {

        this.windowSize         = windowSize;
        this.trackMargin        = trackMargin;
        this.numTrackedRows     = numTrackedRows;
        this.numTrackedColumns  = numTrackedColumns;

        this.currentPoints      = new CvPoint2D32f( numTrackedPoints() );
        this.nextPoints         = new CvPoint2D32f( numTrackedPoints() );
        this.reversePoints      = new CvPoint2D32f( numTrackedPoints() );

        this.featuresFound      = new byte[numTrackedPoints()];
        this.featureErrors      = new float[numTrackedPoints()];
        this.featuresFoundBack  = new byte[numTrackedPoints()];
        this.featureErrorsBack  = new float[numTrackedPoints()];
    }

    private int numTrackedPoints() {
        return( numTrackedRows * numTrackedColumns);
    }

    /**
     * Track from current image to next image, inside the given bounding box on currentImage
     * @param currentImage
     * @param nextImage
     * @param trackBox
     * @return
     */
    public TrackerResult track( IplImage    currentImage,
                                IplImage    nextImage,
                                BoundingBox trackBox ) {
        if( pyramidA == null ) {
            //NOTE: images should be same size, and not change size durring run.
            pyramidA = cvCreateImage(cvSize(currentImage.width(), currentImage.height() ),
                                     IPL_DEPTH_32F,
                                     1);
            pyramidB = cvCreateImage(cvSize(nextImage.width(), nextImage.height() ),
                                     IPL_DEPTH_32F,
                                     1);
        }

        //Get the Points we are going to try and track, and initialize the results to those values
        //to help LK.
        CvPoint2D32f temp = trackBox.getInnerGridPoints( numTrackedRows,
                                                         numTrackedColumns,
                                                         trackMargin );

        for( int x=0;x<numTrackedPoints();x++) {
            currentPoints.position(x).set(temp.position(x));
            nextPoints.position(x).set(temp.position(x));
            reversePoints.position(x).set(temp.position(x));
        }
        currentPoints.position(0);
        nextPoints.position(0);
        reversePoints.position(0);
        temp.deallocate();

        cvCalcOpticalFlowPyrLK(currentImage,
                               nextImage,
                               pyramidA,
                               pyramidB,
                               currentPoints, //vector of points for which flow needs to be found.
                               nextPoints, //OUTPUT of calculated new points.
                               numTrackedPoints(), //number of elements in corners
                               cvSize(windowSize, windowSize),//window, width,height
                               5, //max pyramids to use.
                               featuresFound,
                               featureErrors,
                               cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03),
                               opencv_video.CV_LKFLOW_INITIAL_GUESSES );

        //Now calculate the optical flow in reverse.
        //If we get back(or close) to the original point, we have a high confidence in that result.
        cvCalcOpticalFlowPyrLK(nextImage,
                               currentImage,
                               pyramidB,
                               pyramidA,
                               nextPoints, //vector of points for which flow needs to be found.
                               reversePoints, //OUTPUT of calculated new points. (which should be ~~  currentPoints
                               numTrackedPoints(), //number of elements in corners
                               cvSize(windowSize, windowSize),//window, width,height
                               5, //max pyramids to use.
                               featuresFoundBack,
                               featureErrorsBack,
                               cvTermCriteria(CV_TERMCRIT_ITER | CV_TERMCRIT_EPS, 20, 0.03),
                               opencv_video.CV_LKFLOW_INITIAL_GUESSES | opencv_video.CV_LKFLOW_PYR_A_READY | opencv_video.CV_LKFLOW_PYR_B_READY );

        //amount of error when optically flowing the same points forward and back again.
        //small error is good
        float[] forwardBackardError = Utils.euclideanDistance( currentPoints, reversePoints, numTrackedPoints() );
        //correlation of 1 is good, 0 is bad.
        float[] normCrossCor        = Utils.normalizedCrossCorrelation( currentImage,
                                                                        nextImage,
                                                                        currentPoints,
                                                                        nextPoints,
                                                                        numTrackedPoints(), featuresFound,
                                                                        windowSize,
                                                                        CV_TM_CCOEFF_NORMED );

        TrackerResult result = new TrackerResult( currentPoints,
                                                  nextPoints,
                                                  forwardBackardError,
                                                  normCrossCor,
                                                  featuresFound,
                                                  featureErrors );
        return( result );
    }
}
