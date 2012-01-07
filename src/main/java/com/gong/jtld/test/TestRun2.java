package com.gong.jtld.test;

import com.gong.jtld.*;

import java.util.List;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestRun2 {


    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );


        BoundingBox                 trackerPredictedBoundingBox = null;
        NearestNeighbor.Foo         trackerConfidence           = null;
        BoundingBox                 detectedBoundingBox         = null;
        BoundingBox                 boundingBox                 = new BoundingBox(300,30,335,105);
        BoundingBox                 updatedBoundingBox          = new BoundingBox(300,30,335,105);

        TrackerResult               trackerResult               = null;
        DetectorResult              detectorResult              = null;
        CvFont                      font                        = new CvFont(CV_FONT_HERSHEY_PLAIN, 1.0, 1);

        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = null;
        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);

        Jtdl jtdl = new Jtdl( );
        jtdl.init( next, boundingBox );

        int outputImageNo = 2;
        for( int x=6;x<=100;x++){

            NearestNeighbor.Foo foo = null;
            String temp     = "00000" + x;
            String inStr  = temp.substring(temp.length()-5,temp.length());
            temp     = "00000" + outputImageNo;
            String outStr  = temp.substring(temp.length()-5,temp.length());
            System.out.println("Image::"+inStr);
            nextGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_GRAYSCALE);
            next     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_UNCHANGED);

            //TODO: prealloc and pass in trackerResult
            trackerResult      = jtdl.tracker.track(currentGray, nextGray, boundingBox );



            //int[] validIndexes = Tracker.getValidIndexes(trackerResult);
            //TODO: tracker should have confidence, and isValid (> threshold)

            //detectorResult = jtdl.detect( nextGray );

            if( trackerResult.isValid() ) {

                //This is where
                trackerPredictedBoundingBox = Tracker.predictBoundingBox( boundingBox, trackerResult );
                //TODO: getFooWithGrayScale
                trackerConfidence = jtdl.nearestNeighbor.getFooDebug( nextGray, trackerPredictedBoundingBox );
                if( trackerConfidence.relativeSimilarity > jtdl.nearestNeighbor.getValidThreshold() ) {
                    System.out.println("Tracked and similar");
                }else {
                    System.out.println("Tracked but not similar");
                }
//
//                //TODO: put later
                if( !trackerPredictedBoundingBox.isOutsideImage(nextGray) ) {

                    List<Jtdl.SubResult> dResult = jtdl.detect( nextGray );
                    if( dResult.size() == 0 ) {
                        System.out.println("No Detections");
                    }else {
                        for(Jtdl.SubResult sr : dResult ) {
                            System.out.println("SR.Votes:" + sr.fernValue + " " + sr.similarity );
                            cvSaveImage("/tmp/SRfound-"+ sr.boundingBox + "-v-" + sr.fernValue + "-" +sr.similarity+ ".png", Utils.getImagePatch( next, sr.boundingBox ) );
                        }
                    }

//
//                    for(Jtdl.SubResult sr : dResult ) {
//                        cvSaveImage("/tmp/help" + sr + ".png", Utils.getImagePatch( nextGray, sr.boundingBox ) );
//                    }
//                    cvSaveImage("/tmp/tracked" + trackerPredictedBoundingBox + ".png", Utils.getImagePatch( nextGray, trackerPredictedBoundingBox ) );
                    jtdl.learn( nextGray, trackerPredictedBoundingBox );
                }



            }else {
                System.out.println("Not Tracked in Image:" + inStr);
                trackerConfidence = null;

            }

            updatedBoundingBox = trackerPredictedBoundingBox;

            boundingBox = updatedBoundingBox;
            currentGray = nextGray;

            if( boundingBox.isOutsideImage( next.width(), next.height() ) ) {
                System.out.println("Bounding Box is Outside Image: " + inStr );
            }

            //Updated BB
            cvRectangle(
                    next,
                    cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                    cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                    CV_RGB(0, 255, 0), 1, 8, 0);

//            //Flow vectors
//            for( int y=0;y<validIndexes.length;y++) {
//                trackerResult.origPoints.position(validIndexes[y]);
//                trackerResult.foundPoints.position(validIndexes[y]);
//                cvCircle( next,
//                          cvPoint(Math.round(trackerResult.foundPoints.x()),
//                                  Math.round(trackerResult.foundPoints.y())),
//                          1,
//                          CV_RGB(255, 255, 0), 1, 8, 0 );
//                cvLine(next,
//                   cvPoint(Math.round(trackerResult.origPoints.x()),Math.round(trackerResult.origPoints.y())),
//                   cvPoint(Math.round(trackerResult.foundPoints.x()),Math.round(trackerResult.foundPoints.y())),
//                   CV_RGB(255, 0, 0), 1, 8, 0);
//
//            }

            cvSaveImage("/tmp/imageout-" + outStr + ".png", next);
            //jtdl.fern.dump();
            outputImageNo++;
        }

        jtdl.fern.dump();
        jtdl.nearestNeighbor.dump();
        System.out.println("Searching...");
        IplImage searchImage = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00001.png", CV_LOAD_IMAGE_GRAYSCALE );
        jtdl.detect( searchImage );

//        double best = 0.0;
//        for( ScanningBoundingBoxes boxes : jtdl.scanningBoundingBoxesList ) {
//            for( ScaledBoundingBox scaledBox : boxes.boundingBoxes ) {
//                float value = jtdl.fern.measureVotesDebug(searchImage, scaledBox );
//                if( value > 0.0 ) {
//                    NearestNeighbor.Foo foo = jtdl.nearestNeighbor.getFooDebug( searchImage, scaledBox );
//                    if( foo.relativeSimilarity > best ) {
//                        best = foo.relativeSimilarity;
//                        System.out.println("Votes:" + value + " " + foo.relativeSimilarity );
//                        cvSaveImage("/tmp/found-"+ scaledBox + "-v-" + value + "-" +foo.relativeSimilarity+ ".png", Utils.getImagePatch( searchImage, scaledBox ) );
//                    }
//                }
//            }
//        }

    }



}
