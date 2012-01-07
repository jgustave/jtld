package com.gong.jtld.test;

import com.gong.jtld.*;

import java.util.List;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestRun3 {
    public static final int TRACKER  = 0;
    public static final int WEIGHTED = 1;
    public static final int DETECTED = 2;

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

            int boxType = TRACKER;
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

            List<Jtdl.SubResult> dResult = null;

            dResult = jtdl.detect( nextGray );

            if( trackerResult.isValid() ) {
                //TODO: null these if unstable
                trackerPredictedBoundingBox = Tracker.predictBoundingBox( boundingBox, trackerResult );
                trackerConfidence           = jtdl.nearestNeighbor.getFooDebug( nextGray, trackerPredictedBoundingBox );
            }
            updatedBoundingBox = trackerPredictedBoundingBox;

            if( trackerConfidence != null ) {

                if( dResult.size() > 0 ) {
                    System.out.println("Detected:" + dResult.size() );
                    int otherDetections=0;
                    Jtdl.SubResult savedDetection = null;
                    for(Jtdl.SubResult subResult : dResult ) {
                        if( subResult.boundingBox.overlap( trackerPredictedBoundingBox ) < 0.5f
                            && subResult.similarity > trackerConfidence.relativeSimilarity ) {
                            otherDetections++;
                            savedDetection = subResult;
                        }
                    }

                    if( otherDetections == 1 && !trackerPredictedBoundingBox.isOutsideImage(nextGray) && savedDetection.similarity > jtdl.nearestNeighbor.getValidSwitchThreshold() ) {
                        boxType = DETECTED;
                        System.out.println( "Found a Better Object!" );
                        //We found a more confident detection outside of the tracker...
                        //Change to it.
                        updatedBoundingBox =  savedDetection.boundingBox;
                    }else {
                        int closeX1 = 0;
                        int closeY1 = 0;
                        int closeX2 = 0;
                        int closeY2 = 0;
                        int numCloseDetections = 0;
                        for(Jtdl.SubResult subResult : dResult ) {
                            if( subResult.boundingBox.overlap( trackerPredictedBoundingBox ) > 0.7f ){
                                numCloseDetections++;
                                closeX1 += subResult.boundingBox.x1;
                                closeY1 += subResult.boundingBox.y1;
                                closeX2 += subResult.boundingBox.x2;
                                closeY2 += subResult.boundingBox.y2;
                            }
                        }
                        if( numCloseDetections > 0 ) {
                            boxType = WEIGHTED;
                            System.out.println("NumCloseDetections:" + numCloseDetections );
                            //We are going to modify the tracker slightly with our results.
                            //But we are going to weight it to favor the Tracker
                            float weight = (jtdl.trackerWeight + numCloseDetections);
                            updatedBoundingBox = new BoundingBox( (jtdl.trackerWeight*trackerPredictedBoundingBox.x1 + closeX1) / weight,
                                                                  (jtdl.trackerWeight*trackerPredictedBoundingBox.y1 + closeY1) / weight,
                                                                  (jtdl.trackerWeight*trackerPredictedBoundingBox.x2 + closeX2) / weight,
                                                                  (jtdl.trackerWeight*trackerPredictedBoundingBox.y2 + closeY2) / weight );
                        }else {
                            //Just stick with the optical tracker
                            System.out.println("Stick with Tracker but detected:" + dResult.size() );
                            updatedBoundingBox = trackerPredictedBoundingBox;
                        }
                    }
                }else {
                    System.out.println("No Detections");
                }
            }else { //not tracked tracker

                if( dResult.size() == 1  &&  dResult.get(0).similarity > jtdl.nearestNeighbor.getValidSwitchThreshold() ) {
                    System.out.println("No Tracker, but We detected...");
                    boxType = DETECTED;
                    //TODO: extra confidence for this case...
                    //If Cluster ==1 and Confident.. then reInit
                    updatedBoundingBox = dResult.get(0).boundingBox;

                }else {
                    System.out.println("We are lost.");
                    updatedBoundingBox          = null;
                }
            }

            if(  updatedBoundingBox != null  &&  !updatedBoundingBox.isOutsideImage(nextGray) ) {
                jtdl.learn( nextGray, updatedBoundingBox );
            }

            boundingBox = updatedBoundingBox;
            currentGray = nextGray;

            if( boundingBox != null ) {

                if( boundingBox.isOutsideImage( next.width(), next.height() ) ) {
                    System.out.println("Bounding Box is Outside Image: " + inStr );
                }

                //Updated BB
                CvScalar color = null;
                if( boxType == DETECTED ) {
                    color = CV_RGB(255, 0, 0);
                }else if( boxType == WEIGHTED ) {
                    color = CV_RGB(0, 0, 255);
                }else {
                    color = CV_RGB(0, 255, 0);
                }

                cvRectangle(
                        next,
                        cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                        cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                        color, 1, 8, 0);
            }
            dResult = null;

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
