package com.gong.jtld;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/**
 * This is a Fern Feature.
 * If the intensity of one pixel is > than the other... then the feature is present.
 */
public class Feature {
    public final int x1;
    public final int y1;
    public final int x2;
    public final int y2;

    public Feature (int x1, int y1, int x2, int y2) {
        if( x1 <0 || y1 <0 || x2 < 0 || y2 < 0 ) {
            throw new RuntimeException("Bad Feature");
        }
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Evaluate the feature on the given image.
     * @param image
     * @return 0 or 1
     */
    public int eval (IplImage image) {
        
        if( x1 >= image.width() || x2 >= image.width() || y1 >= image.height() || y2 >= image.height() ) {
            throw new RuntimeException("Feature is off image " +  this + " w:" + image.width() + " h:" + image.height() );
        }
        int         widthStep = image.widthStep();
        BytePointer imageData = image.imageData();
        //Image should be unsigned 8 bit gray scale
//        int val1 = getVal( image, x1, y1 );
//        int val2 = getVal( image, x2, y2 );
        int val1 = (imageData.get(widthStep*y1+x1)&0x000000FF);
        int val2 = (imageData.get(widthStep*y2+x2)&0x000000FF);

        return( (val1 > val2)?1:0 );
    }

    public static int getVal( final IplImage image, final int x, final int y ) {
        opencv_core.IplROI roi = image.roi();
        int xOffset   = 0;
        int yOffset   = 0;

        if( roi != null ) {
            xOffset   = image.roi().xOffset();
            yOffset   = image.roi().yOffset();
            int roiWidth  = image.roi().width();
            int roiHeight = image.roi().height();

            if( x >= roiWidth  ||  y >= roiHeight ) {
                throw new RuntimeException("out of bounds");
            }
        }

        BytePointer imageData = image.imageData();
        return( (imageData.get( image.widthStep()*(y+yOffset)+(x+xOffset))&0x000000FF) );
    }

    public String toString() {
        return("Feature [" +x1 + "," + y1 + "] [" + x2 + "," + y2 + "]" );
    }
}
