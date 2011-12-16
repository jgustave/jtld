package com.gong.jtld.test;
import com.gong.jtld.BoundingBox;
import org.junit.Test;
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
}
