package com.gong.jtld;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

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

        if( x1 > image.width() || x2 > image.width() || y1 > image.height() || y2 > image.height() ) {
            throw new RuntimeException("Feature is off image");
        }
        int         widthStep = image.widthStep();
        BytePointer imageData = image.imageData();
        int val1 = (imageData.get(widthStep*y1+x1)&0x000000FF);
        int val2 = (imageData.get(widthStep*y2+x2)&0x000000FF);

        return( (val1 > val2)?1:0 );
    }
    public String toString() {
        return("Feature [" +x1 + "," + y1 + "] [" + x2 + "," + y2 + "]" );
    }
}
