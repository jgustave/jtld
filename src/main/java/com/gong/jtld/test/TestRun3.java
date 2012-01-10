package com.gong.jtld.test;

import com.gong.jtld.*;


import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestRun3 {

    public static void main( String[] args ){

        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );

        BoundingBox                 initialBoundingBox          = new BoundingBox(300,30,335,105);

        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);

        Jtdl jtdl = new Jtdl( );
        jtdl.init( next, initialBoundingBox );

        int outputImageNo = 2;
        for( int x=6;x<=100;x++){

            String temp     = "00000" + x;
            String inStr  = temp.substring(temp.length()-5,temp.length());
            temp     = "00000" + outputImageNo;
            String outStr  = temp.substring(temp.length()-5,temp.length());
            System.out.println("Image::"+inStr);
            next     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_UNCHANGED);

            jtdl.processFrame( next );

            BoundingBox currentBoundingBox = jtdl.getCurrentBoundingBox();
            if( currentBoundingBox != null ) {

                if( currentBoundingBox.isOutsideImage( next.width(), next.height() ) ) {
                    System.out.println("Bounding Box is Outside Image: " + inStr );
                }

                cvRectangle(
                        next,
                        cvPoint(Math.round(currentBoundingBox.x1), Math.round(currentBoundingBox.y1)),
                        cvPoint(Math.round(currentBoundingBox.x2), Math.round(currentBoundingBox.y2)),
                        CV_RGB(0, 255, 0), 1, 8, 0);
            }

            cvSaveImage("/tmp/imageout-" + outStr + ".png", next);
            //jtdl.fern.dump();
            outputImageNo++;
        }
    }



}
