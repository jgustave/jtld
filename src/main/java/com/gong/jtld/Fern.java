package com.gong.jtld;

import com.googlecode.javacpp.BytePointer;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_features2d.PatchGenerator;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d;

import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
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
    private float minThreshold;
    private float negativeThreshold;
    private float positiveThreshold;
    private final int numFerns;
    private final int featuresPerFern;
    private final int numWarps = 10;


    //[Scale][numFerns*featuresPerFern]  since we want to recognize the object at multiple scales
    private final Feature[][] features;
    //[Per Fern][2^features per fern]
    //the 2^features is because we test for the presense or absense which is just a binary number
    //so we need to represent all possible combinations of features
    private final int[][]     positiveCounter;
    private final int[][]     negativeCounter;
    private final float[][]   posteriors;

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


        opencv_features2d.LDetector unusedBugWorkAround = new opencv_features2d.LDetector();
        generator = new opencv_features2d.PatchGenerator(0.0, //background min
                                                        0.0, //background max
                                                        5.0,  //noise range
                                                        true, //random blur
                                                        1.0-0.02,//lambda (min scaling)
                                                        1.0+0.02,//max scaling
                                                        -20.0*Math.PI/180.0, //theta
                                                        20.0*Math.PI/180.0,
                                                        -20.0*Math.PI/180.0, //phi
                                                        20.0*Math.PI/180.0);
    }

    public void init(IplImage initialImage,
                     BoundingBox initialBox,
                     List<ScaledBoundingBox> bestBoxes,
                     List<ScaledBoundingBox> variedWorstOverlaps) {
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
                Feature feature = new Feature( (int)(width * rand1),
                                               (int)(height * rand2),
                                               (int)(width * rand3),
                                               (int)(height * rand4)  );
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
        //BoundingBox hullBox          = BoundingBox.getHullBox( (List)bestBoxes );
        List<int[]> positiveFeatures = new ArrayList<int[]>();
        List<int[]> negativeFeatures = new ArrayList<int[]>();


        //source image as blurred image to start?
        IplImage    patch    = null;
        //IplImage    smoothed = null;

        //BUGBUG WE are being destructive...
        cvSmooth( image, image, CV_GAUSSIAN, 9,9, 1.5, 1.5 );


//This morph doesn't work as well...
//        patch = image.clone();
//        for( int x=0;x<10*numWarps;x++) {
//            cvSetImageROI( patch, hullBox.getRect() );
//            generator.generate( image, hullBox.getCenter(), patch, hullBox.getSize(), rng );
//            cvSetImageROI( patch, bestBoxes.get(0).getRect() );
//            positiveFeatures.add( getFeatures( patch, bestBoxes.get(0).scaleIndex ) );
//        }


        patch = Utils.getImagePatch(image, bestBoxes.get(0) );
        for( int x=0;x<numWarps;x++) {
            //first image is unwarped
            if( x>0) {
                //Randomly warp into a new patch
                generator.generate( image,
                                    bestBoxes.get(0).getCenter(),
                                    patch,
                                    bestBoxes.get(0).getSize(),
                                    rng );
            }
            positiveFeatures.add(getFeatures( patch, bestBoxes.get(0).scaleIndex ) );
        }
        //TODO: free patch


        for(ScaledBoundingBox box : worstBoxes ) {
            //IplImage badImage = Utils.getImagePatch( image, box );
            cvSetImageROI( image, box.getRect() );

            //TODO: variance:
////If there isn't much variance in the image, then don't bother.. not a good example
//            if (getVar(grid[idx],iisum,iisqsum)<var*0.5f) {
//                  continue;
//            }


            negativeFeatures.add(getFeatures( image, box.scaleIndex ) );
        }
        cvResetImageROI( image );
        train( positiveFeatures, negativeFeatures );
    }

    /**
     * Look at an image and find the features
     * @param image
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
//    public int[] getFeatures( IplImage image, ScaledBoundingBox boundingBox ) {
//        int[] ferns = new int[this.numFerns];
//        int fern = 0;
//        for( int x=0;x<numFerns;x++) {
//            fern = 0;
//            for( int y=0;y<this.featuresPerFern;y++) {
//                fern <<= 1;
//                fern |= features[boundingBox.getScaleIndex()][x*this.featuresPerFern+y].eval(image);
//            }
//            ferns[x] = fern;
//        }
//        return( ferns );
//    }

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
                posteriors[x][index]=0;
            }else{
                posteriors[x][index]= (float)positiveCounter[x][index]/(float)(positiveCounter[x][index] + negativeCounter[x][index]);
            }
        }
    }

    public void updateMinThreshold(List<int[]> negativeFeatures ){
        float threshold = 0.0f;
        for (int[] negativeFeature : negativeFeatures) {
            threshold = measureVotes(negativeFeature) / numFerns;
            if (threshold > minThreshold) {
                minThreshold = threshold;
            }
        }
    }

    public void train( List<int[]> positiveFeatures, List<int[]> negativeFeatures ){

        positiveThreshold = minThreshold*numFerns;

        Set<int[]> negativo = new HashSet<int[]>();
        for( int[] neg : negativeFeatures ) {
            negativo.add( neg );
        }
        List<int[]> foo = new ArrayList<int[]>();
        foo.addAll( negativeFeatures );
        foo.addAll( positiveFeatures );
        Collections.shuffle( foo );

        for( int[] sample : foo ) {
            if( negativo.contains( sample ) ) {
                updatePosterior( false, sample );
            }else {
                updatePosterior( true, sample );
            }
        }
    }

    public void dump() {
        for( int x=0;x<posteriors.length;x++) {
            for( int y=0;y<posteriors[x].length;y++) {
                if( posteriors[x][y] != 0.0 ) {
                    System.out.println("Got: " +x + " " + y + " :" + posteriors[x][y] );
                }
            }
        }
    }

    public void getFernPatterns (IplImage fooImage, List<BoundingBox> closestList, int variance) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public class Feature {
        public final int x1;
        public final int y1;
        public final int x2;
        public final int y2;

        public Feature (int x1, int y1, int x2, int y2) {
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
            int         widthStep = image.widthStep();
            BytePointer imageData = image.imageData();
            return( (imageData.get(widthStep*y1+x1) > imageData.get(widthStep*y2+x2))?1:0 );
        }
    }
}
