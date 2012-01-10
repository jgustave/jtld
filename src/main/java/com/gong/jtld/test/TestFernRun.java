package com.gong.jtld.test;

import com.gong.jtld.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestFernRun {


    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );


        BoundingBox                 boundingBox         = new BoundingBox(300,30,335,105);

        TrackerResult               result              = null;
        CvFont                      font                = new CvFont(CV_FONT_HERSHEY_PLAIN, 1.0, 1);

        IplImage current     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);
        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);


        Jtdl jtdl = new Jtdl( );



        jtdl.init( current, boundingBox );
        ScaledBoundingBox bestBox = (ScaledBoundingBox)Utils.getBestOverlappingScanBoxes(boundingBox, jtdl.scanningBoxes, 1, 0.6f ).get(0);
        jtdl.learn( );
        jtdl.learn(  );
        System.out.println("Votes:" + jtdl.fern.measureVotesDebug( currentGray, bestBox ) );



    }
}
