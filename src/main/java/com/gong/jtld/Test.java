package com.gong.jtld;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 *
 */
public class Test {

    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );

        IplImage image1 = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00001.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage image2 = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00002.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage imgC   = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00001.png", CV_LOAD_IMAGE_UNCHANGED);
        IplImage imgD   = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00002.png", CV_LOAD_IMAGE_UNCHANGED);

        Tracker         tracker     = new Tracker();
        BoundingBox     boundingBox = new BoundingBox(285,25,315,90);
        TrackerResult   result      = tracker.track(image1, image2, boundingBox );

        // Make an image of the results

        //Draw the bounding box
        cvRectangle(
                imgC,
                cvPoint(Math.round(boundingBox.x1), Math.round(boundingBox.y1)),
                cvPoint(Math.round(boundingBox.x2), Math.round(boundingBox.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);

        //Display valid tracks.
        for (int i = 0; i < result.featuresFound.length; i++) {
            if (result.featuresFound[i] == 0 || result.featureErrors[i] > 100) {
                continue;
            }

            result.origPoints.position(i);
            result.foundPoints.position(i);

            CvPoint p0 = cvPoint(Math.round(result.origPoints.x()), Math.round(result.origPoints.y()));
            CvPoint p1 = cvPoint(Math.round(result.foundPoints.x()), Math.round(result.foundPoints.y()));

            if( result.forwardBackwardError[i] < 1 ) {
                cvLine(imgC,
                   p0,
                   p1,
                   CV_RGB(255, 0, 0), 1, 8, 0);
                cvCircle( imgC,
                          cvPoint(Math.round(result.origPoints.x()), Math.round(result.origPoints.y())),
                          1,
                          CV_RGB(0, 0, 255), 1, 8, 0 );
            }
        }

        //Note: I'm not sure I would use median. Maybe just threshold.. FB < N && NCC > M

        float   medianForwardBackwardError = (float)Utils.medianIgnoreNan( result.forwardBackwardError );
        float   medianNormCrossCorrelation = (float)Utils.medianIgnoreNan( result.normCrossCorrelation );

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

        BoundingBox updatedBoundingBox = predictBoundingBox( boundingBox, result, validIndexes );
        System.out.println("Orig:" + boundingBox + " new:" + updatedBoundingBox );
        cvRectangle(
                imgD,
                cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);

        result.destroy();

        cvSaveImage("/tmp/image0-1.png", imgC);
        cvSaveImage("/tmp/image0-2.png", imgD);
        cvNamedWindow( "LKpyr_OpticalFlow", 0 );
        cvShowImage( "LKpyr_OpticalFlow", imgC );
        cvShowImage( "LKpyr_OpticalFlow-2", imgD );
        cvWaitKey(0);
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
    private static BoundingBox predictBoundingBox ( BoundingBox origBoundingBox,
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

        double shiftX  = 0.5*(medianScale-1)*(origBoundingBox.getWidth()-1);
        double shiftY  = 0.5*(medianScale-1)*(origBoundingBox.getHeight()-1);

        //BB1  = [BB0(1)-s1; BB0(2)-s2; BB0(3)+s1; BB0(4)+s2] + [dx; dy; dx; dy];
        BoundingBox result = new BoundingBox( (float)(origBoundingBox.x1 - shiftX + medianChangeX),
                                              (float)(origBoundingBox.y1 - shiftY + medianChangeY),
                                              (float)(origBoundingBox.x2 + shiftX + medianChangeX),
                                              (float)(origBoundingBox.y2 + shiftY + medianChangeY) );

        return( result );
    }
}
