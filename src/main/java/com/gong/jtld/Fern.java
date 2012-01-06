package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_features2d.PatchGenerator;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d;

import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;
import static com.gong.jtld.Jtdl.SCALES;


import java.util.*;

/**
 *
 */
public class Fern {
    private static final Random rand = new Random();
    //scaledWindows

    private final PatchGenerator generator;
    private final opencv_core.CvRNG rng = new opencv_core.CvRNG();



    //private final List<int[]> positiveFeatures = new ArrayList<int[]>();
    //private final List<int[]> negativeFeatures = new ArrayList<int[]>();
    private float minThreshold = 0.6f;
    private float negativeThreshold;
    private float positiveThreshold;

    private final int numFerns;
    private final int featuresPerFern;
    private final int numWarpsInit   = 100;
    private final int numWarpsUpdate = 20;


    //[Scale][numFerns*featuresPerFern]  since we want to recognize the object at multiple scales
    private final Feature[][] features;
    //[Per Fern][2^features per fern]
    //the 2^features is because we test for the presense or absense which is just a binary number
    //so we need to represent all possible combinations of features
    private final int[][]     positiveCounter;
    private final int[][]     negativeCounter;
    private final float[][]   posteriors;

    private       boolean     isFirst   = false;

    public Fern (int numFerns, int featuresPerFern ) {
        //Various scaling factors of the initialBB
        this.numFerns           = numFerns;
        this.negativeThreshold  = 0.5f*numFerns;
        this.positiveThreshold  = 0.5f*numFerns;
        this.featuresPerFern    = featuresPerFern;

        int totalFeatures       = numFerns * featuresPerFern;
        this.features           = new Feature[SCALES.length][totalFeatures];
        this.positiveCounter    = new int[numFerns][(int)Math.pow(2,featuresPerFern)];
        this.negativeCounter    = new int[numFerns][(int)Math.pow(2,featuresPerFern)];
        this.posteriors         = new float[numFerns][(int)Math.pow(2,featuresPerFern)];

        double scaling = 0.02;
        double theta   = 20.0;
        double phi     = 20.0;


        opencv_features2d.LDetector unusedBugWorkAround = new opencv_features2d.LDetector();
        generator = new opencv_features2d.PatchGenerator(0.0, //background min
                                                         0.0, //background max
                                                         5.0,  //noise range
                                                         true, //random blur
                                                         1.0-scaling,//lambda (min scaling)
                                                         1.0+scaling,//max scaling
                                                         -theta*Math.PI/180.0, //theta
                                                         theta*Math.PI/180.0,
                                                         -phi*Math.PI/180.0, //phi
                                                         phi*Math.PI/180.0);
    }

    public float getMinThreshold () {
        return minThreshold;
    }

    public void init(IplImage initialImage,
                     BoundingBox initialBox,
                     List<ScaledBoundingBox> bestBoxes,
                     List<ScaledBoundingBox> variedWorstOverlaps) {
        this.isFirst            = false;
        this.negativeThreshold  = 0.5f*numFerns;
        this.positiveThreshold  = 0.5f*numFerns;
        int totalFeatures       = numFerns * featuresPerFern;

        //GENERATE THE FEATURES
        float width  = initialBox.getWidth();
        float height = initialBox.getHeight();
        for( int x=0;x<totalFeatures;x++) {
            //Same feature but at different scales
            float rand1 = rand.nextFloat();
            float rand2 = rand.nextFloat();
            float rand3 = rand.nextFloat();
            float rand4 = rand.nextFloat();

            //Features at different scales
            for( int y=0;y<SCALES.length;y++) {
                Feature feature = new Feature( (int)(width*SCALES[y] * rand1),
                                               (int)(height*SCALES[y] * rand2),
                                               (int)(width*SCALES[y] * rand3),
                                               (int)(height*SCALES[y] * rand4)  );
                features[y][x] = feature;
            }
        }
        int numFeatureElements = (int)Math.pow(2,featuresPerFern);
        for( int x=0;x<numFerns;x++) {
            for( int y=0;y<numFeatureElements;y++){
                positiveCounter[x][y]=0;
                negativeCounter[x][y]=0;
                posteriors[x][y]=0;
            }
        }

        initFirst(initialImage, bestBoxes, variedWorstOverlaps);
    }

    public int getNumFerns () {
        return numFerns;
    }

    public void train( IplImage image,
                       BoundingBox boundingBox,
                       List<ScaledBoundingBox> bestOverlaps,
                       List<ScaledBoundingBox> worstOverlaps ) {
        initFirst( image, bestOverlaps, worstOverlaps );
    }



    @SuppressWarnings ({"unchecked"})
    public void initFirst(IplImage image, List<ScaledBoundingBox> bestBoxes, List<ScaledBoundingBox> worstBoxes ) {
        BoundingBox hullBox          = BoundingBox.getHullBox( (List)bestBoxes );
        List<int[]> positiveFeatures = new ArrayList<int[]>();
        List<int[]> negativeFeatures = new ArrayList<int[]>();


        //source image as blurred image to start?
        IplImage    patch          = null;
        IplImage    smoothImage    = image.clone();
        //IplImage    smoothed = null;

        cvSmooth( image, smoothImage, CV_GAUSSIAN, 9,9, 1.5, 1.5 );

        int numWarps = isFirst?numWarpsInit:numWarpsUpdate;
        isFirst = false;

//This morph doesn't work as well...
        patch = smoothImage.clone();
        for( int x=0;x<numWarps;x++) {
            cvResetImageROI( patch );
            cvSetImageROI( patch, hullBox.getRect() );
            generator.generate( smoothImage,
                                hullBox.getCenter(),
                                patch,
                                hullBox.getSize(),
                                rng );
            cvResetImageROI( patch );
            cvSetImageROI( patch, bestBoxes.get(0).getRect() );
            positiveFeatures.add( getFeatures( patch, bestBoxes.get(0).scaleIndex ) );
        }


        patch = Utils.getImagePatch(smoothImage, bestBoxes.get(0) );

        for( int x=0;x<numWarps;x++) {
            //first image is unwarped
            if( x>0) {
                //Randomly warp into a new patch
                generator.generate( smoothImage,
                                    bestBoxes.get(0).getCenter(),
                                    patch,
                                    bestBoxes.get(0).getSize(),
                                    rng );
            }
            positiveFeatures.add(getFeatures( patch, bestBoxes.get(0).scaleIndex ) );
        }
        //TODO: free patch


        for(ScaledBoundingBox box : worstBoxes ) {
            cvSetImageROI( smoothImage, box.getRect() );
            negativeFeatures.add(getFeatures( smoothImage, box.scaleIndex ) );
        }
        cvResetImageROI( smoothImage );
        train( positiveFeatures, negativeFeatures );

        //TODO: free smoothedImage
    }

    /**
     * Look at an image and find the features
     * @param image IMAGE PASSED IN SHOULD BE THE CROPPED AND BLURRED IMAGE
     * @param scaleIndex The scale that we are getting features at.
     * @return One Fern per numFerns
     */
    public int[] getFeatures( IplImage image, int scaleIndex ) {
        int[] ferns = new int[this.numFerns];
        int fern = 0;
        for( int x=0;x<numFerns;x++) {
            fern = 0;
            for( int y=0;y<this.featuresPerFern;y++) {
                fern <<= 1;
                fern |= features[scaleIndex][x*this.featuresPerFern+y].eval(image);
            }
            ferns[x] = fern;
        }
        return( ferns );
    }

    /**
     * Given ferns, and each ferns posterior probability, ADD together the votes
     *  I think we add since this is SEMI-naive and each fern is independent
     * @param ferns
     * @return
     */
    public float measureVotes( int[] ferns ) {
        float votes = 0f;
        for( int x=0;x<numFerns;x++) {
            votes += posteriors[x][ferns[x]];
        }
        return( votes );
    }


    public float measureVotesDebug( IplImage image, ScaledBoundingBox boundingBox ) {
        IplImage    patch          = null;
        IplImage    smoothImage    = image.clone();
        cvSmooth( image, smoothImage, CV_GAUSSIAN, 9, 9, 1.5, 1.5 );
        patch = Utils.getImagePatch( smoothImage, boundingBox );
        return( measureVotes( getFeatures( patch, boundingBox.scaleIndex ) ) );
        //TODO: clean up memory
    }
    public float measureVotes( IplImage image, ScaledBoundingBox boundingBox ) {
        IplImage    patch          = null;
        patch = Utils.getImagePatch( image, boundingBox );
        return( measureVotes( getFeatures( patch, boundingBox.scaleIndex ) ) );
        //TODO: clean up memory
    }


    public void updatePosterior(boolean isPositive, int[] ferns ){

          //In Bayes' theorem, P(A), the prior, is the initial degree of belief in proposition A. P(A | B),
          // the posterior, is the degree of belief having accounted for evidence B.
          // P(B | A) / P(B) represents the support B provides for A.
        int index = 0;

        for( int x=0;x<numFerns;x++) {
            index = ferns[x];

            if( isPositive ) {
                positiveCounter[x][index]++;
            }else {
                negativeCounter[x][index]++;
            }

            if( positiveCounter[x][index] == 0 ){
                posteriors[x][index] = 0; //anti div by zero
            }else{
                posteriors[x][index] = ((float)positiveCounter[x][index]) /
                                          ((float)(positiveCounter[x][index] + negativeCounter[x][index]));
            }
        }
    }

    /**
     * Given the Test Data, find the most confident Negative Image.
     * We will use this as a threshold to determine if we update the classifier.
     * @param image
     * @param negativeBoxes
     */
    public void updateMinThreshold(IplImage image, List<ScaledBoundingBox> negativeBoxes ) {
        //TODO: optimze ..reuse int[] memory
        float threshold = 0.0f;
        for( ScaledBoundingBox negativeBox : negativeBoxes ) {
            int[] negativeFeatures = getFeatures( image, negativeBox.scaleIndex );
            threshold = measureVotes(negativeFeatures) / numFerns;
            if (threshold > minThreshold) {
                minThreshold = threshold;
            }
        }
    }

    public void train( List<int[]> positiveFeatures, List<int[]> negativeFeatures ){

        positiveThreshold = minThreshold*numFerns;

        Set<int[]> positivo = new HashSet<int[]>();
        for( int[] positive : positiveFeatures ) {
            positivo.add( positive );
        }
        List<int[]> foo = new ArrayList<int[]>();
        foo.addAll( positiveFeatures );
        foo.addAll( negativeFeatures );
        Collections.shuffle( foo );

        for( int[] sample : foo ) {
            float vote = measureVotes( sample );
            if( positivo.contains( sample ) ) {
                //If a Positive Sample and voted < positiveThreshold then update.
                if( vote <= positiveThreshold ) {
                    updatePosterior( true, sample );
                }
            }else {
                //If a negative Sample, and it voted greater than negativeThreshold.. then update.
                if( vote >= negativeThreshold ) {
                    updatePosterior( false, sample );
                }
            }
        }
    }

    public void dump() {
//        for( int x=0;x<posteriors.length;x++) {
//            for( int y=0;y<posteriors[x].length;y++) {
//                if( posteriors[x][y] != 0.0 ) {
//                    System.out.println("Got: " +x + " " + y + " :" + posteriors[x][y] );
//                }
//            }
//        }
//
        int numVals = (int)Math.pow(2,featuresPerFern);
        for( int x=0;x<numFerns;x++) {
            int[] pCount = new int[featuresPerFern];
            int[] nCount = new int[featuresPerFern];
            for( int y=0;y<numVals;y++) {
                for( int z=0;z<featuresPerFern;z++) {
                    //See if the z bit is set in y
                    if( ( y & (0x00000001<<z)) != 0 ) {
                    //if( bitIsSet(z, y)) {
                        pCount[z] += positiveCounter[x][y];
                        nCount[z] += negativeCounter[x][y];
                    }
                }
            }
            System.out.println("Fern:" + x );
            System.out.println("PositiveCounts:" + Arrays.toString( pCount));
            System.out.println("NegativeCounts:" + Arrays.toString( nCount));
        }
    }

    public void getFernPatterns (IplImage fooImage, List<BoundingBox> closestList, int variance) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
