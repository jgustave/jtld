package com.gong.jtld.test;
import com.gong.jtld.*;
import com.googlecode.javacv.cpp.opencv_core;
import org.junit.Test;

import java.util.List;

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_LOAD_IMAGE_GRAYSCALE;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_LOAD_IMAGE_UNCHANGED;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static org.junit.Assert.*;
/**
 *
 */
public class TestOverlap {

    @Test
    public void testOverlap() {

        BoundingBox bb1 = new BoundingBox(1,1,10,10);
        BoundingBox bb2 = null;


        assertEquals( 1.0, bb1.overlap(bb1), 0.0001 );
        assertEquals( 0.8888888, bb1.overlap(new BoundingBox(1,1,10,9)), 0.0001 );

        assertEquals( 0.0, bb1.overlap(new BoundingBox(20,20,30,30)), 0.0001 );


        bb1 = new BoundingBox(300,30,335,105);
        //0.8696581 BB:[298.0,31.0] [333.0,106.0]
        bb2 = new BoundingBox( 298,31,333,106 );
        assertEquals( 0.8696581, bb1.overlap(bb2),0.0001);
        //System.out.println("" + bb1.overlap(bb2));
    }

    @Test
    public void testBest() {
        Jtdl jtdl = new Jtdl();

        BoundingBox                 boundingBox         = new BoundingBox(300,30,335,105);

        opencv_core.IplImage current     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);
        opencv_core.IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);

        jtdl.init( current, boundingBox );
        List vals = Utils.getBestOverlappingScanBoxes( boundingBox, jtdl.scanningBoundingBoxesList, 4, 0.6f );
        List vals2 = Utils.getBestOverlappingScanBoxes( boundingBox, jtdl.scanningBoundingBoxesList, 4, 0.6f );
        assertEquals(1.0, ((BoundingBox) vals2.get(0)).overlap((BoundingBox) vals2.get(0)), 0.00001);
        List vals3 = Utils.getBestOverlappingScanBoxes( (BoundingBox)vals2.get(0), jtdl.scanningBoundingBoxesList, 4, 0.6f );
        assertEquals( vals.get(0), vals2.get(0) );
        assertEquals( vals2.get(0), vals3.get(0) );
        assertEquals( vals.size(), vals3.size() );
        assertEquals( 10, ((ScaledBoundingBox)vals2.get(0)).getScaleIndex() );
        assertEquals( 1.0, Jtdl.SCALES[10], .0001 );
    }
}
