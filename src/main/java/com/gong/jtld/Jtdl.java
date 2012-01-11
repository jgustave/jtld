package com.gong.jtld;

import com.gong.jtld.NearestNeighbor.Foo;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.CvMat;
import com.googlecode.javacv.cpp.opencv_core.IplImage;


import static com.googlecode.javacv.cpp.opencv_core.*;

import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvIntegral;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

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
    public final Cluster                        cluster             = new Cluster();

    private      BoundingBox                    currentBoundingBox  = null;

    private      IplImage                       grayImage1          = null;
    private      IplImage                       grayImage2          = null;
    private      boolean                        isCurrentImageOne   = true;
    private      IplImage                       smoothedGray        = null;

    //Matricies used in calculating Variance of in an image
    private      CvMat                          iisum               = null;
    private      CvMat                          iisqsum             = null;

    //Template Bounding boxes used when scanning the image for matches.
    public       ScaledBoundingBox[]            scanningBoxes       = null;

    //Various Parameters
    private int     numFerns                = 20;
    private int     featuresPerFern         = 13; //numFerns*(2^featuresPerFern) space
    private int     patchSize               = 25;
    private int     minWindowSize           = 24;
    private int     maxBestBoxes            = 10;
    private int     maxWorstBoxes           = 100;
    private float   minOverlapCutoff        = 0.6f;
    private float   maxOverlapCutoff        = 0.2f;
    private int     initialNegativeSamples  = 100;
    private int     updateNegativeSamples   = 100;
    public final float trackerWeight = 10.0f;

    //After reading in the image (in color) we convert it to Gray scale
    //private      IplImage   grayScaleImage = null;





    private final   CvScalar   mean     = new CvScalar(1);
    private final   CvScalar   stdDev   = new CvScalar(1);
    private         double     variance = 0.0;

    @SuppressWarnings ({"unchecked"})
    public Jtdl( ) {
        fern            = new Fern( numFerns, featuresPerFern );
        nearestNeighbor = new NearestNeighbor( patchSize );
    }

    public BoundingBox getCurrentBoundingBox () {
        return currentBoundingBox;
    }

    @SuppressWarnings ({"unchecked"})
    /**
     * There is an assumption (in several places) that the dimensions of the image will not change after init.
     * .. If it does.. bad things.
     */
    public void init(IplImage initialImage, BoundingBox initialBoundingBox ) {

        this.currentBoundingBox = initialBoundingBox;

        if(grayImage1 != null ) {
            grayImage1.release();
        }
        grayImage1 = IplImage.create( initialImage.width(), initialImage.height(), IPL_DEPTH_8U, 1 );

        if( grayImage2 != null ) {
            grayImage2.release();
        }
        grayImage2 = IplImage.create( initialImage.width(), initialImage.height(), IPL_DEPTH_8U, 1 );
        isCurrentImageOne = true;

        if( smoothedGray != null ) {
            smoothedGray.release();
        }
        smoothedGray = IplImage.create( initialImage.width(), initialImage.height(), IPL_DEPTH_8U, 1 );

        tracker.init( initialImage );

        //Init Matricies used to calculate Variance
        if( iisum != null ) {
            iisum.release();
        }
        iisum          = opencv_core.CvMat.create(initialImage.height() + 1, initialImage.width() + 1, CV_32F, 1);
        if( iisqsum != null ) {
            iisqsum.release();
        }
        iisqsum        = opencv_core.CvMat.create(initialImage.height() + 1, initialImage.width() + 1, CV_64F, 1);

        IplImage    nextGray    = getNextGray();
        cvConvertImage( initialImage, nextGray, 0 );
        cvIntegral(nextGray, iisum, iisqsum, null );

        cvAvgSdv( nextGray, mean, stdDev, null );

        variance = stdDev.getVal(0) * stdDev.getVal(0) * 0.5;

        //These are a bunch of Bounding Boxes that we use for searching.
        scanningBoxes = BoundingBox.createTestBoxeArray( initialBoundingBox,
                                                         SCALES,
                                                         initialImage.width(),
                                                         initialImage.height(),
                                                         minWindowSize );


        List<ScaledBoundingBox> bestOverlaps  = (List)Utils.getBestOverlappingScanBoxes( initialBoundingBox,
                                                                             scanningBoxes,
                                                                             maxBestBoxes,
                                                                             minOverlapCutoff );
        List<ScaledBoundingBox> worstOverlaps = (List)Utils.getWorstOverlappingScanBoxes( initialBoundingBox,
                                                                              scanningBoxes,
                                                                              maxOverlapCutoff );
        Collections.shuffle( worstOverlaps ); //for random test/train split
        List<ScaledBoundingBox> variedWorstOverlaps = getVariantOverlaps( worstOverlaps );

        worstOverlaps       = worstOverlaps.subList(0,Math.min(initialNegativeSamples,worstOverlaps.size()));
        variedWorstOverlaps = variedWorstOverlaps.subList(0,Math.min(initialNegativeSamples,variedWorstOverlaps.size()));


        List<ScaledBoundingBox> trainWorstOverlaps       = worstOverlaps.subList(0, worstOverlaps.size()/2 );
        List<ScaledBoundingBox> trainVariedWorstOverlaps = variedWorstOverlaps.subList(0, variedWorstOverlaps.size()/2 );
        List<ScaledBoundingBox> testWorstOverlaps        = worstOverlaps.subList(worstOverlaps.size()/2, worstOverlaps.size() );
        List<ScaledBoundingBox> testVariedWorstOverlaps  = variedWorstOverlaps.subList(variedWorstOverlaps.size() / 2, variedWorstOverlaps.size());

        //Split the negative examples into train and test sets.
        //Fern only wants highly variant negatives.. NN wants all.
        //But keep all the positive examples.
        //Save the test sets to update thresholds.

        nearestNeighbor.init(nextGray, (List)bestOverlaps, (List)trainWorstOverlaps );
        fern.init(nextGray, initialBoundingBox, bestOverlaps, trainVariedWorstOverlaps );

        //Update thresholds
        nearestNeighbor.updateNearestNeighborThreshold( nextGray, testWorstOverlaps );
        fern.updateMinThreshold( nextGray, testVariedWorstOverlaps );

        flipGrayImages();
    }

    public IplImage getCurrentGray() {
        if(isCurrentImageOne) {
            return( grayImage1 );
        }else {
            return( grayImage2 );
        }
    }
    public IplImage getNextGray() {
        if(isCurrentImageOne) {
            return( grayImage2 );
        }else {
            return( grayImage1 );
        }
    }
    private void flipGrayImages() {
        isCurrentImageOne =!isCurrentImageOne;
    }

    /**
     * Should update currentBoundingBox with new location.. or set it to null if lost
     * @param image
     */
    public void processFrame( IplImage nextImage ) {
        if( nextImage.width() != grayImage1.width() ||  nextImage.height() != grayImage1.height() ) {
            throw new RuntimeException("Change in dimensions" );
        }
        IplImage        currentGray                 = getCurrentGray();
        IplImage        nextGray                    = getNextGray();
        TrackerResult   trackerResult               = null;
        BoundingBox     trackerPredictedBoundingBox = null;
        Foo             trackerConfidence           = null;
        List<Jtdl.SubResult> dResult = null;

        //Load the image as Gray Scale, etc
        prepNextImage( nextImage );


        //If we currently have a Bounding Box
        if( currentBoundingBox != null ) {

            //Use the OPtical Tracker to track to next
            trackerResult      = tracker.track( currentGray, nextGray, currentBoundingBox );

            if( trackerResult.isValid() ) {
                //TODO: null these if unstable
                trackerPredictedBoundingBox = Tracker.predictBoundingBox( currentBoundingBox, trackerResult );
                trackerConfidence           = nearestNeighbor.getFooDebug( nextGray, trackerPredictedBoundingBox );
            }else {
                trackerPredictedBoundingBox = null;
                trackerConfidence = null;
            }
        }

        //Now use the image detector to see if we can find object elsewhere
        dResult = detect( );

        if( trackerConfidence != null ) {

            if( dResult.size() > 0 ) {

                System.out.println("Detected:" + dResult.size() );
                int otherDetections=0;
                Jtdl.SubResult savedDetection = null;
                for(Jtdl.SubResult subResult : dResult ) {
                    if( subResult.boundingBox.overlap( trackerPredictedBoundingBox ) < 0.5f
                        && subResult.similarity > trackerConfidence.relativeSimilarity ) {
                        otherDetections++;
                        savedDetection = subResult;
                    }
                }

                if( otherDetections == 1
                    && !trackerPredictedBoundingBox.isOutsideImage( nextGray )
                    && savedDetection.similarity > nearestNeighbor.getValidSwitchThreshold() ) {
                    //boxType = DETECTED;
                    System.out.println( "Found a Better Object!" );
                    //We found a more confident detection outside of the tracker...
                    //Change to it.
                    currentBoundingBox = savedDetection.boundingBox;
                }else {
                    //We are going to weight any highly overlapping Detected BBs with the Tracker BB
                    int closeX1 = 0;
                    int closeY1 = 0;
                    int closeX2 = 0;
                    int closeY2 = 0;
                    int numCloseDetections = 0;
                    for(Jtdl.SubResult subResult : dResult ) {
                        if( subResult.boundingBox.overlap( trackerPredictedBoundingBox ) > 0.7f ){
                            numCloseDetections++;
                            closeX1 += subResult.boundingBox.x1;
                            closeY1 += subResult.boundingBox.y1;
                            closeX2 += subResult.boundingBox.x2;
                            closeY2 += subResult.boundingBox.y2;
                        }
                    }
                    if( numCloseDetections > 0 ) {
                        //boxType = WEIGHTED;
                        System.out.println("NumCloseDetections:" + numCloseDetections );
                        //We are going to modify the tracker slightly with our results.
                        //But we are going to weight it to favor the Tracker
                        float weight = (trackerWeight + numCloseDetections);
                        currentBoundingBox = new BoundingBox( (trackerWeight*trackerPredictedBoundingBox.x1 + closeX1) / weight,
                                                              (trackerWeight*trackerPredictedBoundingBox.y1 + closeY1) / weight,
                                                              (trackerWeight*trackerPredictedBoundingBox.x2 + closeX2) / weight,
                                                              (trackerWeight*trackerPredictedBoundingBox.y2 + closeY2) / weight );
                    }else {
                        //Just stick with the optical tracker
                        System.out.println("Stick with Tracker but detected:" + dResult.size() );
                        currentBoundingBox = trackerPredictedBoundingBox;
                    }
                }
            }else {
                System.out.println("No Detections");
                currentBoundingBox = trackerPredictedBoundingBox;
            }
        }else { //not tracked by tracker
            if( dResult.size() == 1
                &&  dResult.get(0).similarity > nearestNeighbor.getValidSwitchThreshold() ) {
                System.out.println("No Tracker, but We detected...");
                //boxType = DETECTED;
                currentBoundingBox = dResult.get(0).boundingBox;
            }else {
                System.out.println("We are lost.");
                currentBoundingBox = null;
            }
        }


        //Now learn what's inside the new BB
        if(  currentBoundingBox != null  &&  !currentBoundingBox.isOutsideImage(nextGray) ) {
            learn( );
        }


        flipGrayImages();
    }

    /**
     * We only want worst overlaps if they are highly variant.. meaning they have information in them.
     * @param image
     * @param worstOverlaps
     * @return
     */
    private List<ScaledBoundingBox> getVariantOverlaps ( List<ScaledBoundingBox> worstOverlaps ) {
        List<ScaledBoundingBox> result = new ArrayList<ScaledBoundingBox>();
        for( ScaledBoundingBox box : worstOverlaps ) {
            if( Utils.getVariance( box, iisum, iisqsum ) > variance * 0.5 ) {
                result.add( box );
            }
        }
        return( result );
    }

    /**
     * convert to Gray, create integral and smoothed data, means and stddev
     * @param nextImage The full color image
     */
    public void prepNextImage( IplImage nextImage ) {
        IplImage        nextGray                    = getNextGray();
        cvConvertImage( nextImage, nextGray, 0 );
        cvIntegral(nextGray, iisum, iisqsum, null );
        cvSmooth( nextGray, smoothedGray, CV_GAUSSIAN, 9,9, 1.5, 1.5 );

        //TODO: might be optional:
        cvAvgSdv( nextGray, mean, stdDev, null );
        variance = stdDev.getVal(0) * stdDev.getVal(0) * 0.5;
    }

    @SuppressWarnings ({"unchecked", "RedundantCast"})
    public void learn ( ) {
        IplImage    nextGray    = getNextGray();

        List<ScaledBoundingBox> bestOverlaps  = (List)Utils.getBestOverlappingScanBoxes( currentBoundingBox,
                                                                             scanningBoxes,
                                                                             maxBestBoxes,
                                                                             minOverlapCutoff );
        List<ScaledBoundingBox> worstOverlaps = (List)Utils.getWorstOverlappingScanBoxes( currentBoundingBox,
                                                                              scanningBoxes,
                                                                              maxOverlapCutoff );
        Collections.shuffle( worstOverlaps );
        List<ScaledBoundingBox> variedWorstOverlaps = getVariantOverlaps( worstOverlaps );

        worstOverlaps       = worstOverlaps.subList(0,Math.min(updateNegativeSamples,
                                                               worstOverlaps.size()));
        variedWorstOverlaps = variedWorstOverlaps.subList(0,Math.min(updateNegativeSamples,
                                                                     variedWorstOverlaps.size()));

        //ScaledBoundingBox bestBox = bestOverlaps.get(0);

        nearestNeighbor.train(nextGray, (List) bestOverlaps, (List) worstOverlaps);
        fern.train(nextGray, currentBoundingBox, bestOverlaps, variedWorstOverlaps);
    }

    public List<SubResult> detect( ) {
        IplImage        nextGray        = getNextGray();
        List<SubResult> subResult       = new ArrayList<SubResult>();
        List<SubResult> result          = new ArrayList<SubResult>();
        float           fernThreshold   = fern.getNumFerns()*fern.getMinThreshold();

        for( ScaledBoundingBox scaledBox : this.scanningBoxes ) {

            //First exclude areas that don't have high enough variance.
            double testVariance = Utils.getVariance( scaledBox, iisum, iisqsum );
            if( testVariance >= variance ) {
                //Then exclude if fern isn't good enough
                float fernValue = fern.measureVotes(smoothedGray, scaledBox );
                if( fernValue > fernThreshold ) {

                    NearestNeighbor.Foo foo = nearestNeighbor.getFooDebug( nextGray, scaledBox );

                    if( foo.relativeSimilarity > nearestNeighbor.getValidThreshold() ) {
                        //result.add( fernValue, foo.relativeSimilarity, scaledBox );
                        subResult.add( new SubResult( fernValue, foo.relativeSimilarity, scaledBox ) );
                    }
                }
            }
        }

        if( subResult.size() > 0 ) {
            result = this.cluster.cluster( subResult );
        }

        return(result);
    }

    public static class SubResult {
        public final float              fernValue;
        public final double             similarity;
        public final BoundingBox        boundingBox;

        public SubResult (float fernValue, double similarity, BoundingBox boundingBox) {
            this.fernValue = fernValue;
            this.similarity = similarity;
            this.boundingBox = boundingBox;
        }
        public String toString() {
            return("SR: sim:" + similarity +" fern:" + fernValue + " " + boundingBox );
        }
    }
}
