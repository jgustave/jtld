package com.gong.jtld;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 20 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class Test4 {

    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );

        Tracker         tracker             = new Tracker();
        BoundingBox     boundingBox         = new BoundingBox(300,30,335,105);
        BoundingBox     updatedBoundingBox  = new BoundingBox(300,30,335,105);
        TrackerResult   result              = null;


        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = null;
        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);

        //Draw initial bounding box. on first image
        cvRectangle(
                next,
                cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);
        cvSaveImage("/tmp/imageout-00001.png", next );

        //next.release();

        int imageNo = 2;
        for( int x=6;x<99;x++){
            //tracker = new Tracker();
            String temp     = "00000" + x;
            String inStr  = temp.substring(temp.length()-5,temp.length());
            temp     = "00000" + imageNo;
            String outStr  = temp.substring(temp.length()-5,temp.length());
            nextGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_GRAYSCALE);
            next     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_UNCHANGED);

            result      = tracker.track(currentGray, nextGray, boundingBox );
            int[] validIndexes = getValidIndexes( result );
            if( validIndexes.length > 0 ) {
                updatedBoundingBox = predictBoundingBox( boundingBox, result, validIndexes );
            }else {
                System.out.println("Uh oh, no valid Indexes");
            }
            boundingBox = updatedBoundingBox;
            //currentGray.release();
            currentGray = nextGray;

            cvRectangle(
                    next,
                    cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                    cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                    CV_RGB(0, 255, 0), 1, 8, 0);
            cvSaveImage("/tmp/imageout-"+outStr+".png", next );
            imageNo++;
        }

    }

    private static int[] getValidIndexes(TrackerResult result ) {
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
