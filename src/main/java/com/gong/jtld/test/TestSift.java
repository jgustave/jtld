package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.Tracker;
import com.gong.jtld.TrackerResult;
import com.gong.jtld.Utils;
import com.googlecode.javacv.cpp.opencv_features2d;
import com.googlecode.javacv.cpp.opencv_imgproc;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvIntegral;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 20 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestSift {

    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );

        BoundingBox boundingBox        = new BoundingBox(300,30,335,105);
        BoundingBox otherBoundingBox   = new BoundingBox(323,30,358,105);
        TrackerResult result           = null;


        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00010.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage normal1     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);
        IplImage normal2     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00010.png", CV_LOAD_IMAGE_UNCHANGED);

        opencv_features2d.KeyPoint keyPoints1 = new opencv_features2d.KeyPoint();
        opencv_features2d.KeyPoint keyPoints2 = new opencv_features2d.KeyPoint();
        opencv_features2d.SIFT sift = new opencv_features2d.SIFT();

        //int patternSize
        IplImage subImage1 = Utils.getImagePatch( currentGray, boundingBox );
        IplImage subImage2 = Utils.getImagePatch( nextGray, otherBoundingBox );
//        IplImage destImage1 = cvCreateImage(cvSize(patternSize,patternSize),
//                                           image.depth(),
//                                           image.nChannels() );

//        cvResize( image, destImage, opencv_imgproc.CV_INTER_LINEAR );

        opencv_features2d.SiftDescriptorExtractor dd = new opencv_features2d.SiftDescriptorExtractor();
        //dd.compute();
        sift.detect( subImage1, null, keyPoints1 );
        sift.detect( subImage2, null, keyPoints2 );

        IplImage i;
        //cvIntegral();

        for( int x=0;x<100;x++) {
            keyPoints1.position(x);
            keyPoints2.position(x);
            System.out.println("" + keyPoints1.pt() + "  " + keyPoints2.pt() );

                cvCircle( normal1,
                          cvPoint(Math.round(keyPoints1.pt().x()),
                                  Math.round(keyPoints1.pt().y())),
                          1,
                          CV_RGB(255, 255, 0), 1, 8, 0 );
                cvCircle( normal2,
                          cvPoint(Math.round(keyPoints2.pt().x()),
                                  Math.round(keyPoints2.pt().y())),
                          1,
                          CV_RGB(255, 255, 0), 1, 8, 0 );
        }

        cvRectangle(
                normal1,
                cvPoint(Math.round(boundingBox.x1), Math.round(boundingBox.y1)),
                cvPoint(Math.round(boundingBox.x2), Math.round(boundingBox.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);
        cvRectangle(
                normal2,
                cvPoint(Math.round(otherBoundingBox.x1), Math.round(otherBoundingBox.y1)),
                cvPoint(Math.round(otherBoundingBox.x2), Math.round(otherBoundingBox.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);


        cvSaveImage(
                "/tmp/image0-1.png", normal1);
        cvSaveImage(
                "/tmp/image0-2.png", normal1);
        cvNamedWindow( "LKpyr_OpticalFlow", 0 );
        cvShowImage( "LKpyr_OpticalFlow", normal1 );
        cvNamedWindow( "LKpyr_OpticalFlow2", 0 );
        cvShowImage( "LKpyr_OpticalFlow2", normal2 );

        cvWaitKey(0);

    }

}
