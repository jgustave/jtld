package com.gong.jtld.test;

import com.gong.jtld.Feature;
import com.googlecode.javacv.cpp.opencv_core;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestFern {

    @Test
    public void testFeatures() {
        int width  = 20;
        int height = 30;
        IplImage testImage = IplImage.create( width, height, IPL_DEPTH_8U, 1 );
        //testImage.get
        //

        //opencv_core.CvMat mat = (opencv_core.CvMat)testImage;
        ByteBuffer buffer = testImage.getByteBuffer();
        for( int x=0;x<width*height;x++) {
            buffer.put(x,(byte)x);
        }
        Feature feature = null;
        feature = new Feature(0,0,0,0);
        assertEquals(0, feature.eval(testImage));
        feature = new Feature(0,0,1,0);
        assertEquals( 0, feature.eval( testImage ) );
        feature = new Feature(1,0,0,0);
        assertEquals( 1, feature.eval( testImage ) );

        feature = new Feature(1,10,0,10);
        assertEquals( 1, feature.eval( testImage ) );

//
//        for( int x=0;x<255;x++) {
//            feature = new Feature(1,0,0,0);
//            assertEquals( 1, feature.eval( testImage ) );
//        }

    }
}
