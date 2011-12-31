package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.Fern;
import com.gong.jtld.ScanningBoundingBoxes;
import com.gong.jtld.Utils;
import com.googlecode.javacv.cpp.opencv_features2d;

import java.util.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_highgui.cvShowImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWaitKey;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestWarp {



    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );


        BoundingBox     boundingBox         = new BoundingBox(300,30,335,105);
        List<ScanningBoundingBoxes> testBoundingBoxes = null;

        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        //IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage result = Utils.warpImage2( currentGray, boundingBox );


        cvSmooth( currentGray, currentGray , CV_GAUSSIAN, 9,9, 1.5, 1.5 );
        //cvSaveImage("/tmp/smoothed.png", currentGray);
        opencv_features2d.LDetector unusedBugWorkAround = new opencv_features2d.LDetector();
        opencv_features2d.PatchGenerator generator = new opencv_features2d.PatchGenerator(0.0, //background min
                                                        0.0, //background max
                                                        5.0,  //noise range
                                                        true, //random blur
                                                        1.0-0.02,//lambda (min scaling)
                                                        1.0+0.02,//max scaling
                                                        -20.0*Math.PI/180.0, //theta
                                                        20.0*Math.PI/180.0,
                                                        -20.0*Math.PI/180.0, //phi
                                                        20.0*Math.PI/180.0);

        //cvSetImageROI( currentGray, boundingBox.getRect() );
        cvSaveImage("/tmp/prewarp.png", currentGray);
        //currentGray.
        CvRNG rng = new CvRNG();
        IplImage warpPatch = currentGray.clone();

        for( int x=0;x<10;x++) {

            //cvResetImageROI( warpPatch );
            cvSetImageROI( warpPatch, boundingBox.getRect() );

            generator.generate( currentGray, boundingBox.getCenter(), warpPatch, boundingBox.getSize(), rng );
//            cvSetImageROI( currentGray, boundingBox.getRect() );
//            cvCopy( warpPatch, currentGray);
//            cvResetImageROI( currentGray );
            cvResetImageROI( warpPatch );
            cvSaveImage("/tmp/warp"+x+".png", warpPatch );
        }


        //currentGray.c
//        generator.generate( currentGray, boundingBox.getCenter(), currentGray, boundingBox.getSize(), rng );
//        generator.generate( currentGray, boundingBox.getCenter(), currentGray, boundingBox.getSize(), rng );



//
//        cvLine(
//                result,
//                cvPoint(Math.round(boundingBox.x1+10), Math.round(boundingBox.y1)),
//                cvPoint(Math.round(boundingBox.x2-10), Math.round(boundingBox.y1)),
//                CV_RGB(0, 255, 0), 1, 8, 0);
//
//        cvLine(
//                result,
//                cvPoint(Math.round(boundingBox.x1), Math.round(boundingBox.y2)),
//                cvPoint(Math.round(boundingBox.x2), Math.round(boundingBox.y2)-15),
//                CV_RGB(0, 255, 0), 1, 8, 0);
//
//
//
//        cvSaveImage("/tmp/warp1.png", result);
//        cvNamedWindow( "Warp", 0 );
//        cvShowImage( "Warp", result );
//
//
//        cvWaitKey(0);


    }



}
