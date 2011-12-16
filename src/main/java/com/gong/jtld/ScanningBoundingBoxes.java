package com.gong.jtld;

import java.util.ArrayList;
import java.util.List;

/**
 * When scanning an image, these are the bounding boxes we are going to use.
 */
public class ScanningBoundingBoxes {
    //BB
    //Num X shifts per Y shift
    //pointer to scale used

    //Scale
    public  final List<BoundingBox> boundingBoxes = new ArrayList<BoundingBox>();
    public final int scaleHeight;
    public final int scaleWidth;
    //This is the number of X Shifted BB's before the Y shift
    public final int numXShifts;

    public ScanningBoundingBoxes (int scaleHeight, int scaleWidth, int numXShifts) {
        this.scaleHeight = scaleHeight;
        this.scaleWidth  = scaleWidth;
        this.numXShifts  = numXShifts;
    }
}
