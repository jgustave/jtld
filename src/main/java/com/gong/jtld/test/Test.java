package com.gong.jtld.test;

import com.gong.jtld.*;
import com.sun.corba.se.impl.logging.UtilSystemException;

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


        IplImage img1   = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00002.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage img2   = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage img3   = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00008.png", CV_LOAD_IMAGE_GRAYSCALE);

        BoundingBox boundingBox1 = new BoundingBox(285,40,315,115); //00002
        BoundingBox boundingBox2 = new BoundingBox(300,30,335,105); //00005
        BoundingBox boundingBox3 = new BoundingBox(320,65,355,140); //00008

        Jtdl jtdl = new Jtdl();
        jtdl.init( img1, boundingBox1 );

        BoundingBox boundingBox1a =Utils.getBestOverlappingScanBoxes( boundingBox1, jtdl.scanningBoxes, 1, 0.6f ).get(0);
        BoundingBox boundingBox2a =Utils.getBestOverlappingScanBoxes( boundingBox2, jtdl.scanningBoxes, 1, 0.6f ).get(0);
        BoundingBox boundingBox3a =Utils.getBestOverlappingScanBoxes( boundingBox3, jtdl.scanningBoxes, 1, 0.6f ).get(0);

        System.out.println("Fern:" + jtdl.fern.measureVotesDebug( img1, (ScaledBoundingBox)boundingBox1a ) );
        System.out.println("Fern:" + jtdl.fern.measureVotesDebug( img2, (ScaledBoundingBox)boundingBox2a ) );
        System.out.println("Fern:" + jtdl.fern.measureVotesDebug( img3, (ScaledBoundingBox)boundingBox3a ) );

        jtdl.learn(  );

        System.out.println("Fern:" + jtdl.fern.measureVotesDebug( img1, (ScaledBoundingBox)boundingBox1a ) );
        System.out.println("Fern:" + jtdl.fern.measureVotesDebug( img2, (ScaledBoundingBox)boundingBox2a ) );
        System.out.println("Fern:" + jtdl.fern.measureVotesDebug( img3, (ScaledBoundingBox)boundingBox3a ) );


//
//        cvRectangle(
//                imgD,
//                cvPoint(Math.round(boundingBox.x1), Math.round(boundingBox.y1)),
//                cvPoint(Math.round(boundingBox.x2), Math.round(boundingBox.y2)),
//                CV_RGB(0, 255, 0), 1, 8, 0);
//
////        result.destroy();
//
//        //imgD = Utils.getImagePatch( imgC, boundingBox );
////        cvSaveImage("/tmp/image0-1.png", imgC);
////        cvSaveImage("/tmp/image0-2.png", imgD);
////        cvNamedWindow( "LKpyr_OpticalFlow", 0 );
////        cvShowImage( "LKpyr_OpticalFlow", imgC );
//        cvShowImage( "LKpyr_OpticalFlow-2", imgD );
//        cvWaitKey(0);
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
