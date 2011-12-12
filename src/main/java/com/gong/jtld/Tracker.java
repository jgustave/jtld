package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.opencv_video;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_CCOEFF_NORMED;
import static com.googlecode.javacv.cpp.opencv_video.cvCalcOpticalFlowPyrLK;

/**
 * This implements the Optical Flow Tracker and uses the Lucas Kanade algorithm to
 * track a box between images.
 * http://www.cs.columbia.edu/CAVE/projects/nnsearch/
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

    private       int           lastWidth               = 0;
    private       int           lastHeight              = 0;

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
        if( currentImage.width() != nextImage.width()  &&  currentImage.height() != nextImage.height() ) {
            throw new RuntimeException("Image size not consistent");
        }

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

//        for( int x=0;x<numTrackedPoints();x++){
//            System.out.println("TrackedPoint:" + temp.position(x));
//        }


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

//        for( int x=0;x<numTrackedPoints();x++){
//            System.out.println("FROM:" + currentPoints.position(x) + " TO:" + nextPoints.position(x) );
//        }

        currentPoints.position(0);
        nextPoints.position(0);
        reversePoints.position(0);
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

    public static int[] getValidIndexes(TrackerResult result ) {
        float   medianForwardBackwardError = (float)Utils.medianIgnoreNan( result.forwardBackwardError );
        float   medianNormCrossCorrelation = (float)Utils.medianIgnoreNan( result.normCrossCorrelation );

//        if( medianForwardBackwardError > 10 ) {
//            System.out.println("Median Forward Backward Error is too high:" + medianForwardBackwardError );
//            return( new int[0] );
//        }

        //Now find which of our results were valid, and save the indexes for later
        int     numValidIndexes = 0;
        int[]   tempIndexes    = new int[result.getNumPoints()];
        for( int x=0;x<result.getNumPoints();x++) {
            if( result.forwardBackwardError[x] < medianForwardBackwardError  && result.normCrossCorrelation[x] > medianNormCrossCorrelation ) {
                tempIndexes[numValidIndexes] = x;
                numValidIndexes++;
            }
        }
        int[]   validIndexes   = new int[numValidIndexes];
        System.arraycopy(tempIndexes,0,validIndexes,0,numValidIndexes);

        return( validIndexes );
    }

    /**
     * Given the original bounding box, and the results from optical flow Tracker, predict where the new bounding box is.
     * This is going to look at two things... One is a general shift of the box... (left/right/up/down) etc.
     * The other consideration is zooming in/out. (Z). We accomplish the first by looking at the median change between the
     * X and Y between sets.
     * The second we look at the pairwise distance.. Or How the points relate to each other in each set.
     * Imagine the points getting more and less dense (Zoom In/Out respectively)
     * @param origBoundingBox
     * @param trackerResult results from the Tracker
     * @param validIndexes Only these indexes in the results are to be considered
     * @return
     */
    public static BoundingBox predictBoundingBox ( BoundingBox origBoundingBox,
                                                    TrackerResult trackerResult,
                                                    int[] validIndexes ) {

        //int numPoints       = trackerResult.getNumPoints();
        int numValidPoints  = validIndexes.length;
        CvMat origPoints    = CvMat.create(numValidPoints, 2);
        CvMat foundPoints   = CvMat.create(numValidPoints, 2);

        for( int i=0;i<numValidPoints;i++) {
            trackerResult.origPoints.position(validIndexes[i]);
            trackerResult.foundPoints.position(validIndexes[i]);
            origPoints.put(i,0,trackerResult.origPoints.x());
            origPoints.put(i,1,trackerResult.origPoints.y());
            foundPoints.put(i,0,trackerResult.foundPoints.x());
            foundPoints.put(i,1,trackerResult.foundPoints.y());
        }


//        CvMat origPoints  = Utils.toMatrix( result.origPoints, result.getNumPoints() );
//        CvMat foundPoints = Utils.toMatrix( result.foundPoints, result.getNumPoints() );
        CvMat deltaPoints = CvMat.create(numValidPoints, 2 );
        cvSub( foundPoints, origPoints, deltaPoints, null );

        double medianChangeX = Utils.medianIgnoreNan( Utils.getColumnSlice( deltaPoints, 0) );
        double medianChangeY = Utils.medianIgnoreNan( Utils.getColumnSlice( deltaPoints, 1) );

        //Calculate Pairwise Distances for noth old and new
        CvMat origPairwiseDistance  = Utils.pairwiseDistance(origPoints);
        CvMat newPairwiseDistance   = Utils.pairwiseDistance(foundPoints);

        //Use that to determine if the Bounding Box needs to be scaled
        CvMat pairwiseScale         = CvMat.create(origPairwiseDistance.rows(),1);
        cvDiv(newPairwiseDistance, origPairwiseDistance, pairwiseScale, 1 );

        double medianScale          = Utils.medianIgnoreNan(pairwiseScale.get());


        //this is scaling... we are shifting in or out based on the scale.
        //I think the half is because half goes on each side.
        double shiftX  = 0.5*(medianScale-1)*(origBoundingBox.getWidth()+1);
        double shiftY  = 0.5*(medianScale-1)*(origBoundingBox.getHeight()+1);

        //Then we add in the change in x/y
        BoundingBox result = new BoundingBox( (float)(origBoundingBox.x1 - shiftX + medianChangeX),
                                              (float)(origBoundingBox.y1 - shiftY + medianChangeY),
                                              (float)(origBoundingBox.x2 + shiftX + medianChangeX),
                                              (float)(origBoundingBox.y2 + shiftY + medianChangeY) );

        return( result );
    }

}
