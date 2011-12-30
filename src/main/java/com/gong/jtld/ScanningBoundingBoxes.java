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
    public  final List<ScaledBoundingBox> boundingBoxes = new ArrayList<ScaledBoundingBox>();
//    public final int scaleHeight;
//    public final int scaleWidth;
    //This is the number of X Shifted BB's before the Y shift
    public final int numXShifts;

    public ScanningBoundingBoxes (int numXShifts) {
//        this.scaleHeight = scaleHeight;
//        this.scaleWidth  = scaleWidth;
        this.numXShifts  = numXShifts;
    }
}
