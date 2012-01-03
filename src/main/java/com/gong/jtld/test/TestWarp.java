package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.ScanningBoundingBoxes;

import com.googlecode.javacv.cpp.opencv_features2d;
import static com.googlecode.javacv.cpp.opencv_core.cvAvgSdv;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar;


import java.nio.FloatBuffer;
import java.util.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvIntegral;
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
        //IplImage result = Utils.warpImage2( currentGray, boundingBox );

        CvScalar mean   = new CvScalar(1);
        CvScalar stdDev = new CvScalar(1);
        cvAvgSdv( currentGray, mean, stdDev, null );
        System.out.println("mean:" + mean );
        System.out.println("stdev:" + stdDev );

//        IplImage iisum = IplImage.create(currentGray.width()+1, currentGray.height()+1, IPL_DEPTH_32F, 1);
//        IplImage iisqsum = IplImage.create(currentGray.width()+1, currentGray.height()+1, IPL_DEPTH_64F, 1);

//        CvMat iisum = cvMat(currentGray.width()+1, currentGray.height()+1, CV_32F, null );
//        CvMat iisqsum = cvMat(currentGray.width()+1, currentGray.height()+1, CV_64F, null );

        CvMat iisum = CvMat.create(currentGray.height()+1, currentGray.width()+1, CV_32F, 1 );
        CvMat iisqsum = CvMat.create(currentGray.height()+1, currentGray.width()+1, CV_64F, 1 );
        cvIntegral(currentGray, iisum, iisqsum, null);

        System.out.println("Hello:" + getVariance(boundingBox, iisum, iisqsum ) );

//        FloatBuffer fb = iisum.getFloatBuffer();
//        for( int x=0;x<(currentGray.width()*currentGray.height());x++) {
//            if( fb.get(x) != 0 )
//                System.out.println(" " + fb.get(x) );
//        }
//        ByteBuffer bb = iisqsum.getByteBuffer();
//        for( int x=0;x<10000;x++)
//        {
//            if( bb.get(x) != 0 )
//                System.out.println(" " + bb.get(x) );
//        }
//        cvSaveImage("/tmp/blarg.png", iisum);
        //meanStdDev
        cvSmooth(currentGray, currentGray, CV_GAUSSIAN, 9, 9, 1.5, 1.5);
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

    }

    public static double getVariance( BoundingBox boundingBox, CvMat sum, CvMat squareSum ){

        double bottomRightSum       = sum.get( (int)(boundingBox.y2),
                                               (int)(boundingBox.x2) );
        double bottomLeftSum        = sum.get( (int)(boundingBox.y2),
                                               (int)(boundingBox.x1) );
        double topRightSum          = sum.get( (int)(boundingBox.y1),
                                               (int)(boundingBox.x2) );
        double topLeftSum           = sum.get( (int)(boundingBox.y1),
                                               (int)(boundingBox.x1) );
        double bottomRightSquareSum = squareSum.get( (int)(boundingBox.y2),
                                                     (int)(boundingBox.x2) );
        double bottomLeftSquareSum  = squareSum.get( (int)(boundingBox.y2),
                                                     (int)(boundingBox.x1) );
        double topRightSquareSum    = squareSum.get( (int)(boundingBox.y1),
                                                     (int)(boundingBox.x2) );
        double topLeftSquareSum     = squareSum.get( (int)(boundingBox.y1),
                                                     (int)(boundingBox.x1) );

        double mean         = (bottomRightSum+topLeftSum-topRightSum-bottomLeftSum) / ((double) boundingBox.getArea());
        double squareMean   = (bottomRightSquareSum+topLeftSquareSum-topRightSquareSum-bottomLeftSquareSum) / ((double) boundingBox.getArea());
        //so the variance is the difference in the integrated squareMean and meanSquare
        return( squareMean - mean * mean);
    }
}
