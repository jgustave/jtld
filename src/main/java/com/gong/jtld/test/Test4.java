package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.Tracker;
import com.gong.jtld.TrackerResult;
import com.gong.jtld.Utils;
import com.googlecode.javacv.cpp.opencv_imgproc;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class Test4 {

    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );

        Tracker tracker             = new Tracker();
//        BoundingBox     boundingBox         = new BoundingBox(285,25,315,90);
//        BoundingBox     updatedBoundingBox  = new BoundingBox(285,25,315,90);
        BoundingBox boundingBox         = new BoundingBox(300,30,335,105);
        BoundingBox     updatedBoundingBox  = new BoundingBox(300,30,335,105);
        TrackerResult result              = null;


        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = null;
        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);

        //Draw initial bounding box. on first image
        cvRectangle(
                next,
                cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                CV_RGB(0, 255, 0), 1, 8, 0);
        cvSaveImage("/tmp/imageout-00001.png", next );


//
//        IplImage destImage = cvCreateImage(cvSize(20,20),
//                                           next.depth(),
//                                           next.nChannels() );
//
//        cvResize( next, destImage, opencv_imgproc.CV_INTER_LINEAR );
//        cvSaveImage("/tmp/patch.png", destImage );
//        //cvSaveImage("/tmp/patch.png", Utils.getImagePatch(next, boundingBox));
        float[] val = Utils.getPattern(Utils.getImagePatch(next, boundingBox), 20);
        //next.release();

        int outputImageNo = 2;
        for( int x=6;x<=100;x++){
            String temp     = "00000" + x;
            String inStr  = temp.substring(temp.length()-5,temp.length());
            temp     = "00000" + outputImageNo;
            String outStr  = temp.substring(temp.length()-5,temp.length());
            System.out.println("::"+inStr);
            nextGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_GRAYSCALE);
            next     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_UNCHANGED);

            result      = tracker.track(currentGray, nextGray, boundingBox );
            int[] validIndexes = Tracker.getValidIndexes(result);
            if( result.isValid() ) {
                updatedBoundingBox = Tracker.predictBoundingBox(boundingBox, result);
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
                          cvPoint(Math.round(result.foundPoints.x()),Math.round(result.foundPoints.y())),
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

    }



}
