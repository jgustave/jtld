package com.gong.jtld.test;

import com.gong.jtld.*;

import java.util.List;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestRun {


    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );


        Tracker                     tracker             = new Tracker();
        BoundingBox                 boundingBox         = new BoundingBox(300,30,335,105);
        BoundingBox                 updatedBoundingBox  = new BoundingBox(300,30,335,105);

        TrackerResult               result              = null;
        //int                         patchSize           = 25;
        CvFont                      font                = new CvFont(CV_FONT_HERSHEY_PLAIN, 1.0, 1);
        //NearestNeighbor             nearestNeighbor     = null;

        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = null;
        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);


        Jtdl jtdl = new Jtdl( );
        jtdl.init( next, boundingBox );
        NearestNeighbor nearestNeighbor = jtdl.nearestNeighbor;

//
//        //jtdl.fern.dump();
//        System.out.println("Searching...");
//        double best = 0.0;
//        for( ScanningBoundingBoxes boxes : jtdl.scanningBoundingBoxesList ) {
//            for( ScaledBoundingBox scaledBox : boxes.boundingBoxes ) {
//                IplImage patch = Utils.getImagePatch( test, scaledBox );
//                int[] features = jtdl.fern.getFeatures( patch, scaledBox.scaleIndex );
//                float value = jtdl.fern.measureVotes( features );
//                if( value > 0.0 ) {
//                    NearestNeighbor.Foo foo = jtdl.nearestNeighbor.getFoo(test, scaledBox );
//                    if( foo.relativeSimilarity > best ) {
//                        best = foo.relativeSimilarity;
//                        System.out.println("Votes:" + value + " " + foo.relativeSimilarity );
//                        cvSaveImage("/tmp/found-"+ scaledBox + "-v-" + value + ".png", patch);
//                    }
//                }
//            }
//        }




        int outputImageNo = 2;
        for( int x=6;x<=100;x++){
            NearestNeighbor.Foo foo = null;
            String temp     = "00000" + x;
            String inStr  = temp.substring(temp.length()-5,temp.length());
            temp     = "00000" + outputImageNo;
            String outStr  = temp.substring(temp.length()-5,temp.length());
            System.out.println("::"+inStr);
            nextGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_GRAYSCALE);
            next     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_UNCHANGED);

            result      = tracker.track(currentGray, nextGray, boundingBox );
            int[] validIndexes = Tracker.getValidIndexes(result);

            if( validIndexes.length > 0 ) {
                updatedBoundingBox = Tracker.predictBoundingBox( boundingBox, result, validIndexes);

                foo = nearestNeighbor.getFoo( nextGray, updatedBoundingBox );
                BoundingBox fakeBox = new BoundingBox(10,
                                                      updatedBoundingBox.y1,
                                                      10+updatedBoundingBox.getWidth(),
                                                      updatedBoundingBox.y2 );
                if( !fakeBox.isOutsideImage( nextGray ) ) {
                    NearestNeighbor.Foo fakeFoo = nearestNeighbor.getFoo( nextGray, fakeBox );
                    cvPutText( next,
                               "" + fakeFoo.relativeSimilarity,
                               cvPoint(30,90),
                               font,
                               CV_RGB(10, 10, 10) );
                }
                cvPutText( next,
                           "" + foo.relativeSimilarity ,
                           cvPoint(30,60),
                           font,
                           CV_RGB(10, 10, 10) );


                if( !boundingBox.isOutsideImage(nextGray) ) {
                    //TODO: update data
                    jtdl.learn( nextGray, updatedBoundingBox );
                    //System.out.println("Status:" + nearestNeighbor.getStatus() );
                }

            }else {
                System.out.println("Uh oh, no valid Indexes:" + inStr);
                //need to wait till detector finds object again
                //null BB.
            }
            boundingBox = updatedBoundingBox;
            //currentGray.release();
            currentGray = nextGray;

            if( boundingBox.isOutsideImage( next.width(), next.height() ) ) {
                System.out.println("Bounding Box is Outside Image: " + inStr );
            }

            cvRectangle(
                    next,
                    cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                    cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                    CV_RGB(0, 255, 0), 1, 8, 0);
            for( int y=0;y<validIndexes.length;y++) {
                result.origPoints.position(validIndexes[y]);
                result.foundPoints.position(validIndexes[y]);
                cvCircle( next,
                          cvPoint(Math.round(result.foundPoints.x()),
                                  Math.round(result.foundPoints.y())),
                          1,
                          CV_RGB(255, 255, 0), 1, 8, 0 );
                cvLine(next,
                   cvPoint(Math.round(result.origPoints.x()),Math.round(result.origPoints.y())),
                   cvPoint(Math.round(result.foundPoints.x()),Math.round(result.foundPoints.y())),
                   CV_RGB(255, 0, 0), 1, 8, 0);

            }
            cvSaveImage("/tmp/imageout-" + outStr + ".png", next);
            outputImageNo++;
        }
//
//        System.out.println("Searching...");
//        IplImage test        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00001.png", CV_LOAD_IMAGE_GRAYSCALE);
//        double best = 0.0;
//        for( ScanningBoundingBoxes boxes : jtdl.scanningBoundingBoxesList ) {
//            for( ScaledBoundingBox scaledBox : boxes.boundingBoxes ) {
//                IplImage patch = Utils.getImagePatch( test, scaledBox );
//                int[] features = jtdl.fern.getFeatures( patch, scaledBox.scaleIndex );
//                float value = jtdl.fern.measureVotes( features );
//                if( value > 0.0 ) {
//                    NearestNeighbor.Foo foo = jtdl.nearestNeighbor.getFoo(test, scaledBox );
//                    if( foo.relativeSimilarity > best ) {
//                        best = foo.relativeSimilarity;
//                        System.out.println("Votes:" + value + " " + foo.relativeSimilarity );
//                        cvSaveImage("/tmp/found-"+ scaledBox + "-v-" + value + "-s-" + best + ".png", patch);
//                    }
//                }
//            }
//        }


    }



}
