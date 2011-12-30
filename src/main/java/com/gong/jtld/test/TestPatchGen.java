package com.gong.jtld.test;

import com.gong.jtld.*;
import com.googlecode.javacv.cpp.opencv_features2d;

import java.util.List;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestPatchGen {



    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );


        Tracker                     tracker             = new Tracker();
        BoundingBox                 boundingBox         = new BoundingBox(300,30,335,105);
        BoundingBox                 updatedBoundingBox  = new BoundingBox(300,30,335,105);
        List<ScanningBoundingBoxes> testBoundingBoxes   = null;
        TrackerResult               result              = null;
        int                         patchSize           = 25;
        CvFont                      font                = new CvFont(CV_FONT_HERSHEY_PLAIN, 1.0, 1);
        NearestNeighbor             nearestNeighbor     = null;

        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = null;
        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);


        CvRNG rng = new CvRNG();
        opencv_features2d.LDetector ld = new opencv_features2d.LDetector();
        opencv_features2d.PatchGenerator gen = new opencv_features2d.PatchGenerator(0.0, //background min
                                                                                    0.0, //background max
                                                                                    5.0,  //noise range
                                                                                    true, //random blur
                                                                                    1.0-0.02,//lambda (min scaling)
                                                                                    1.0+0.02,//max scaling
                                                                                    -20.0*Math.PI/180.0, //theta
                                                                                    20.0*Math.PI/180.0,
                                                                                    -20.0*Math.PI/180.0, //phi
                                                                                    20.0*Math.PI/180.0);

        //Utils.getImagePatch( currentGray, boundingBox );
        IplImage destImage = IplImage.create(boundingBox.getSize(),8,1);
        for( int x=0;x<10;x++)
        {
            gen.generate( currentGray, boundingBox.getCenter(), destImage, boundingBox.getSize(), rng);
            cvSaveImage("/tmp/imageout-w"+x+".png", destImage);
        }
    }



}
