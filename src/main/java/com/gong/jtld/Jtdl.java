package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Jeremy's Tracking Detecting and Learning
 */
public class Jtdl {

    /**
     * This is used to look for an object at various scalings
     * 1.2^(-10 to 10)
     */
    public static final double[] SCALES = { 0.16151,0.19381,0.23257,0.27908,0.33490,
                                            0.40188,0.48225,0.57870,0.69444,0.83333,
                                            1.00000,1.20000,1.44000,1.72800,2.07360,
                                            2.48832,2.98598,3.58318,4.29982,5.15978,6.19174 };

    public final NearestNeighbor nearestNeighbor;
    public final Fern            fern;
    public final List<ScanningBoundingBoxes> scanningBoundingBoxesList;

    @SuppressWarnings ({"unchecked"})
    public Jtdl( IplImage initialImage, BoundingBox initialBoundingBox) {
        int numFerns                = 10;
        int featuresPerFern         = 20; //2^featuresPerFern space
        int patchSize               = 25;
        int minWindowSize           = 24;
        int maxBestBoxes            = 10;
        int maxWorstBoxes           = 100;
        float minOverlapCutoff      = 0.6f;
        float maxOverlapCutoff      = 0.2f;

        scanningBoundingBoxesList = BoundingBox.createTestBoxes( initialBoundingBox,
                                                                                             SCALES,
                                                                                             initialImage.width(),
                                                                                             initialImage.height(),
                                                                                             minWindowSize );

        List<ScaledBoundingBox> bestOverlaps  = (List)Utils.getBestOverlappingScanBoxes( initialBoundingBox,
                                                                             scanningBoundingBoxesList,
                                                                             maxBestBoxes,
                                                                             minOverlapCutoff );
        List<ScaledBoundingBox> worstOverlaps = (List)Utils.getWorstOverlappingScanBoxes( initialBoundingBox,
                                                                              scanningBoundingBoxesList,
                                                                              maxWorstBoxes,
                                                                              maxOverlapCutoff );

        //TODO: we only want WorstOverlaps that have interesting image data in them.
        List<ScaledBoundingBox> variedWorstOverlaps = getVariantOverlaps(initialImage, worstOverlaps);

        fern            = new Fern( numFerns,
                                    featuresPerFern,
                                    initialImage,
                                    initialBoundingBox,
                                    scanningBoundingBoxesList,
                                    bestOverlaps,
                                    variedWorstOverlaps );
        nearestNeighbor = new NearestNeighbor( scanningBoundingBoxesList,
                                               patchSize );
        nearestNeighbor.init( initialImage, (List)bestOverlaps, (List)worstOverlaps );
    }

    private List<ScaledBoundingBox> getVariantOverlaps (IplImage initialImage, List<ScaledBoundingBox> worstOverlaps) {
        return( worstOverlaps );
    }

    @SuppressWarnings ({"unchecked"})
    public void learn (IplImage nextGray, BoundingBox updatedBoundingBox) {
        List bestOverlaps  = Utils.getBestOverlappingScanBoxes( updatedBoundingBox,
                                                           scanningBoundingBoxesList,
                                                           10,
                                                           0.6f );
        List worstOverlaps = Utils.getWorstOverlappingScanBoxes( updatedBoundingBox,
                                                            scanningBoundingBoxesList,
                                                            100,
                                                            0.2f );

        nearestNeighbor.init( nextGray, bestOverlaps, worstOverlaps );
        fern.train( nextGray, updatedBoundingBox, bestOverlaps, worstOverlaps );
    }
}
