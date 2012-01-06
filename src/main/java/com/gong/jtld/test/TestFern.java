package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.Feature;
import com.googlecode.javacv.cpp.opencv_core;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import jpaul.DataStructs.UnionFind;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    @Test
    public void testPartition() {
        UnionFind<BoundingBox> f= new UnionFind<BoundingBox>();

        BoundingBox bb0 = new BoundingBox(200,200,600,600);
        BoundingBox bb1 = new BoundingBox(1,1,10,10);
        BoundingBox bb2 = new BoundingBox(1,1,100,100);
        BoundingBox bb2b = new BoundingBox(1,1,98,98);
        BoundingBox bb2c = new BoundingBox(10,10,98,98);
        BoundingBox bb3 = new BoundingBox(9,9,10,10);
        BoundingBox bb4 = new BoundingBox(9,9,40,40);
        BoundingBox bb5 = new BoundingBox(9,9,60,60);
        BoundingBox bb6 = new BoundingBox(11,11,60,60);

        List<BoundingBox> boxes = new ArrayList<BoundingBox>();
        boxes.add(bb0);
        boxes.add(bb1);
        boxes.add(bb2);
        boxes.add(bb2b);
        boxes.add(bb2c);
        boxes.add(bb3);
        boxes.add(bb4);
        boxes.add(bb5);
        boxes.add(bb6);

        for( int x=0;x<boxes.size();x++) {
            for( int y=x+1;y<boxes.size();y++) {
                System.out.print(".");
                float overlap = boxes.get(x).overlap(boxes.get(y));
                if( overlap > 0.5f ) {
                    System.out.println("overlap " + x + " " + y );
                    f.union( boxes.get(x), boxes.get(y) );
                }
            }
        }

        Collection<Set<BoundingBox>> foo = f.allNonTrivialEquivalenceClasses();
        System.out.println("" + foo.size() );

//        //f.
//        f.union(5,7);
//        f.union(10,11);
//        f.union(11,19);
//        f.union(19,23);
//        f.union(2,7);
        System.out.println("" + f.toString());
    }
}
