package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.util.List;

/**
 *
 */
public class Fern {

    //init
    public void init(IplImage initialImage,
                     List<ScanningBoundingBoxes> possibleBB,
                     float[][] features ) {
        int imageWidth  = initialImage.width();
        int imageHeight = initialImage.height();
        int numTrees    = features.length;

        //TODO: make features an object and ensure proper size/shape
        int numFeatures = (features.length * features[0].length ) / 4;


    }

    public void getFernPatterns (IplImage fooImage, List<BoundingBox> closestList, int variance) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
