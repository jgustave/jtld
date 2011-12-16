package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.Fern;
import com.gong.jtld.ScanningBoundingBoxes;
import com.gong.jtld.Utils;

import java.util.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_highgui.cvShowImage;
import static com.googlecode.javacv.cpp.opencv_highgui.cvWaitKey;

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

        IplImage result = Utils.warpImage2( currentGray, boundingBox );

        cvLine(
                result,
                cvPoint(Math.round(boundingBox.x1+10), Math.round(boundingBox.y1)),
                cvPoint(Math.round(boundingBox.x2-10), Math.round(boundingBox.y1)),
                CV_RGB(0, 255, 0), 1, 8, 0);

        cvLine(
                result,
                cvPoint(Math.round(boundingBox.x1), Math.round(boundingBox.y2)),
                cvPoint(Math.round(boundingBox.x2), Math.round(boundingBox.y2)-15),
                CV_RGB(0, 255, 0), 1, 8, 0);



        cvSaveImage("/tmp/warp1.png", result);
        cvNamedWindow( "Warp", 0 );
        cvShowImage( "Warp", result );


        cvWaitKey(0);


    }



}
