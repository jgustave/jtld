package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.cvConvertImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvIntegral;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Jeremy's Tracking Detecting and Learning
 */
public class Jtdl {

    /**
     * This is used to look for an object at various scalings
     * 1.2^(-10 to 10)
     * We use the scales to create scanningBoundingBoxes from the initial bounding box, scaled by these amounts.
     */
    public static final double[] SCALES = { 0.16151,0.19381,0.23257,0.27908,0.33490,
                                            0.40188,0.48225,0.57870,0.69444,0.83333,
                                            1.00000,1.20000,1.44000,1.72800,2.07360,
                                            2.48832,2.98598,3.58318,4.29982,5.15978,6.19174 };

    //Lucas Kanade Optical Flow Tracker
    public final Tracker                        tracker             = new Tracker();
    //Simple Nearest Neighbor
    public final NearestNeighbor                nearestNeighbor;
    //Semi Nieve Bayesian Image Detector based on Ferns
    public final Fern                           fern;

    //Template Bounding boxes used when scanning the image for matches.
    public       List<ScanningBoundingBoxes>    scanningBoundingBoxesList;

    //Various Parameters
    private int     numFerns                = 10;
    private int     featuresPerFern         = 20; //numFerns*(2^featuresPerFern) space
    private int     patchSize               = 25;
    private int     minWindowSize           = 24;
    private int     maxBestBoxes            = 10;
    private int     maxWorstBoxes           = 100;
    private float   minOverlapCutoff        = 0.6f;
    private float   maxOverlapCutoff        = 0.2f;

    //After reading in the image (in color) we convert it to Gray scale
    private      IplImage   grayScaleImage = null;

    //Matricies used in calculating Variance of in an image
    private      CvMat      iisum   = null;
    private      CvMat      iisqsum = null;

    private final   CvScalar   mean     = new CvScalar(1);
    private final   CvScalar   stdDev   = new CvScalar(1);
    private         double     variance = 0.0;

    @SuppressWarnings ({"unchecked"})
    public Jtdl( ) {
        fern            = new Fern( numFerns, featuresPerFern );
        nearestNeighbor = new NearestNeighbor( patchSize );
    }

    @SuppressWarnings ({"unchecked"})
    /**
     * There is an assumption (in several places) that the dimensions of the image will not change after init.
     * .. If it does.. bad things.
     */
    public void init(IplImage initialImage, BoundingBox initialBoundingBox ) {

        if( grayScaleImage != null ) {
            //Not sure how/if GC is going to double release.. this may not be necessary
            grayScaleImage.release();
            iisum.release();
            iisqsum.release();
        }
        iisum       = opencv_core.CvMat.create(initialImage.height() + 1, initialImage.width() + 1, CV_32F, 1);
        iisqsum     = opencv_core.CvMat.create(initialImage.height() + 1, initialImage.width() + 1, CV_64F, 1);
        grayScaleImage = IplImage.create( initialImage.width(), initialImage.height(), IPL_DEPTH_8U, 1 );

        cvConvertImage( initialImage, grayScaleImage, 0 );
        cvIntegral(grayScaleImage, iisum, iisqsum, null );

        cvAvgSdv( grayScaleImage, mean, stdDev, null );

        variance = stdDev.getVal(0) * stdDev.getVal(0) * 0.5;
//        double fooV = Utils.getVariance(initialBoundingBox,iisum,iisqsum)*0.5;
//        System.out.println("Var:" + variance + " foo:" + fooV );

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
                                                                              maxOverlapCutoff );
        Collections.shuffle( worstOverlaps ); //for random test/train split
        List<ScaledBoundingBox> variedWorstOverlaps = getVariantOverlaps(grayScaleImage, worstOverlaps);

        //TODO: BUG BUG online has them all.. but here we dont.. not sure why
        worstOverlaps = worstOverlaps.subList(0,Math.min(100,worstOverlaps.size()));
        variedWorstOverlaps = variedWorstOverlaps.subList(0,Math.min(100,variedWorstOverlaps.size()));


        List<ScaledBoundingBox> trainWorstOverlaps       = worstOverlaps.subList(0, worstOverlaps.size()/2 );
        List<ScaledBoundingBox> trainVariedWorstOverlaps = variedWorstOverlaps.subList(0, variedWorstOverlaps.size()/2 );
        List<ScaledBoundingBox> testWorstOverlaps        = worstOverlaps.subList(worstOverlaps.size()/2, worstOverlaps.size() );
        List<ScaledBoundingBox> testVariedWorstOverlaps  = variedWorstOverlaps.subList(variedWorstOverlaps.size() / 2, variedWorstOverlaps.size());

        //Split the negative examples into train and test sets.
        //Fern only wants highly variant negatives.. NN wants all.
        //But keep all the positive examples.
        //Save the test sets to update thresholds.

        nearestNeighbor.init(grayScaleImage, (List)bestOverlaps, (List)trainWorstOverlaps );
        fern.init(grayScaleImage, initialBoundingBox, bestOverlaps, trainVariedWorstOverlaps );

        //Update thresholds
        nearestNeighbor.updateNearestNeighborThreshold( grayScaleImage, testWorstOverlaps );
        fern.updateMinThreshold( grayScaleImage, testVariedWorstOverlaps );
    }

    /**
     * We only want worst overlaps if they are highly variant.. meaning they have information in them.
     * @param image
     * @param worstOverlaps
     * @return
     */
    private List<ScaledBoundingBox> getVariantOverlaps (IplImage image, List<ScaledBoundingBox> worstOverlaps ) {
        List<ScaledBoundingBox> result = new ArrayList<ScaledBoundingBox>();
        for( ScaledBoundingBox box : worstOverlaps ) {
            if( Utils.getVariance( box, iisum, iisqsum ) > variance * 0.5 ) {
                result.add( box );
            }
        }
        return( result );
    }

    @SuppressWarnings ({"unchecked"})
    public void learn (IplImage nextImage, BoundingBox updatedBoundingBox) {

        cvConvertImage( nextImage, grayScaleImage, 0 );
        cvIntegral(grayScaleImage, iisum, iisqsum, null );


        List<ScaledBoundingBox> bestOverlaps  = (List)Utils.getBestOverlappingScanBoxes( updatedBoundingBox,
                                                                             scanningBoundingBoxesList,
                                                                             maxBestBoxes,
                                                                             minOverlapCutoff );
        List<ScaledBoundingBox> worstOverlaps = (List)Utils.getWorstOverlappingScanBoxes( updatedBoundingBox,
                                                                              scanningBoundingBoxesList,
                                                                              maxOverlapCutoff );
        Collections.shuffle( worstOverlaps );
        List<ScaledBoundingBox> variedWorstOverlaps = getVariantOverlaps(grayScaleImage, worstOverlaps);

        //TODO: BUG BUG online has them all.. but here we dont.. not sure why
        worstOverlaps = worstOverlaps.subList(0,Math.min(100,worstOverlaps.size()));
        variedWorstOverlaps = variedWorstOverlaps.subList(0,Math.min(100,variedWorstOverlaps.size()));

        ScaledBoundingBox bestBox = bestOverlaps.get(0);
        System.out.println("BestNN:" + nearestNeighbor.getFoo(grayScaleImage, bestBox).relativeSimilarity);
        System.out.println("BestFern:" + fern.measureVotesDebug( grayScaleImage, bestBox ) );

        nearestNeighbor.train(grayScaleImage, (List) bestOverlaps, (List) worstOverlaps);
        fern.train(grayScaleImage, updatedBoundingBox, bestOverlaps, variedWorstOverlaps);
        System.out.println("PostBestNN:" + nearestNeighbor.getFoo(grayScaleImage, bestBox).relativeSimilarity);
        System.out.println("PostBestFern:" + fern.measureVotesDebug( grayScaleImage, bestBox ) );
    }


}
