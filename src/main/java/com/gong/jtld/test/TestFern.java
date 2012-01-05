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
    /**
     * Verify Features are created correctly and compare pixels correctly.
     */
    public void testFeatures() {
        int width  = 20;
        int height = 30;
        IplImage testImage = IplImage.create( width, height, IPL_DEPTH_8U, 1 );

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


        for( int x=0;x<255;x++) {
            int rowVal = x%width;
            int heightVal = x/width;
            int rowValNext = (x+1)%width;
            int heightValNext = (x+1)/width;
            Feature feature1 = new Feature(rowVal,heightVal,rowValNext,heightValNext);
            Feature feature2 = new Feature(rowVal,heightVal,rowVal,heightVal);
            Feature feature3 = new Feature(rowValNext,heightValNext,rowVal,heightVal);

            assertEquals( 0, feature1.eval( testImage ) );
            assertEquals( 0, feature2.eval( testImage ) );
            assertEquals( 1, feature3.eval( testImage ) );
        }

    }
}
