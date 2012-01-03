package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.util.*;

/**
 *
 */
public class NearestNeighbor {

    private final List<float[]> positivePatchPatterns = new ArrayList<float[]>(256);
    private final List<float[]> negativePatchPatterns = new ArrayList<float[]>(256);


    private final int                           patchSize;
    private       double                        validThreshold          = 0.65;
    private       double                        positiveThreshold       = 0.60;
    private       double                        negativeThreshold       = 0.5;

    public NearestNeighbor( int patchSize ) {
        this.patchSize = patchSize;
    }

    public double getValidThreshold () {
        return validThreshold;
    }

    public void init (IplImage image,
                      List<BoundingBox> bestOverlaps,
                      List<BoundingBox> worstOverlaps ) {

        validThreshold      = 0.65;
        positiveThreshold   = 0.60;
        negativeThreshold   = 0.5;
        this.positivePatchPatterns.clear();
        this.negativePatchPatterns.clear();
        train( image, bestOverlaps, worstOverlaps );
    }

    public void train (IplImage image,
                       List<BoundingBox> bestOverlaps,
                       List<BoundingBox> worstOverlaps ) {

        //TODO: optimize, reuse patch mem don't acucmulate list
        List<float[]>  positivePatchPatterns = getPositivePatchPatterns( image, bestOverlaps.get(0), patchSize );
        List<float[]>  negativePatchPatterns = getNegativePatchPatterns( image, worstOverlaps, patchSize );

        Set<float[]> positives = new HashSet<float[]>();
        positives.addAll( positivePatchPatterns );

        List<float[]> patches = new ArrayList<float[]>();
        patches.addAll( positivePatchPatterns );
        patches.addAll( negativePatchPatterns );
        Collections.shuffle( patches );
        //important to have positive first to init
        patches.add(0,positivePatchPatterns.get(0));

        for( float[] patch : patches ) {
            //Since we know the patch is apriori a Positive or Negative patch..
            //AND we look at the Similarity (0 being similar to a negative, 1 being similar to a positive)
            //We say: If it's a Positive Patch, but more similar to a negative patch.. we want to add it to
            //the collection. (and vv)
            if( positives.contains(patch) ) {
                if( this.positivePatchPatterns.size() == 0 ) {
                    this.positivePatchPatterns.add( patch );
                    //dump( "Positive:" + this.positivePatchPatterns.size(), patch );
                } else {
                    Foo foo = getFoo(patch);
                    if( foo.relativeSimilarity <= positiveThreshold ) {
                        this.positivePatchPatterns.add( patch );
                        //dump( "Positive-" + foo.relativeSimilarity +"-" + this.positivePatchPatterns.size(), patch );
                    }
                }
            }else { //assume negativePatch
                if( this.negativePatchPatterns.size() == 0 ) {
                    this.negativePatchPatterns.add( patch );
                    //dump( "Negative:" + this.positivePatchPatterns.size(), patch );
                }else {
                    Foo foo = getFoo(patch);
                    if( foo.relativeSimilarity > negativeThreshold ) {
                        //dump( "Negative-" + foo.relativeSimilarity + "-" + this.negativePatchPatterns.size(), patch );
                        this.negativePatchPatterns.add( patch );
                    }
                }
            }
        }
    }

    public void updateNearestNeighborThreshold(IplImage image, List<ScaledBoundingBox> negativeBoxes ) {

        for( BoundingBox box : negativeBoxes ) {
            //TODO: optimize re-use patch mem
            float[] negativePatch = Utils.getPatchPattern( image, box, patchSize );
            NearestNeighbor.Foo foo = getFoo(negativePatch);
            if( foo.relativeSimilarity > positiveThreshold ) {
                positiveThreshold =  foo.relativeSimilarity;
            }
        }
        if( positiveThreshold > validThreshold ) {
            validThreshold =  positiveThreshold;
        }
    }

    private static List<float[]> getPositivePatchPatterns ( IplImage image,
                                                            BoundingBox bestBox,
                                                            int patternSize ) {
        List<float[]> result = new ArrayList<float[]>();

        result.add( Utils.getPatchPattern( image,
                                           bestBox,
                                           patternSize ) );
        result.add( Utils.getPatchPattern( image,
                                           bestBox,
                                           patternSize,
                                           Utils.X_FLIP) );
        result.add( Utils.getPatchPattern( image,
                                           bestBox,
                                           patternSize,
                                           Utils.Y_FLIP) );
        return( result );
    }


    private static List<float[]> getNegativePatchPatterns (IplImage image,
                                                           List<BoundingBox> worstOverlaps,
                                                           int patternSize ) {
        List<float[]> result = new ArrayList<float[]>();

        for( BoundingBox box : worstOverlaps ) {
            result.add( Utils.getPatchPattern( image,
                                               box,
                                               patternSize ) );
        }
        return( result );
    }

    public Foo getFoo( float[] patch ) {

        //Shouldn't need this.. because we branch earlier.. but forces us to add
        //patch to collection
        if( positivePatchPatterns.size() == 0 ) {
            return( new Foo(0,0));
        }else if( negativePatchPatterns.size() == 0 ) {
            return( new Foo(1,1));
        }

        double[] positiveNormalizedCorrelations  = Utils.normalizedCorrelation( patch, positivePatchPatterns );
        double[] negativeNormalizedCorrelations  = Utils.normalizedCorrelation( patch, negativePatchPatterns );

        double deltaNegative                     = 1 - Utils.max(negativeNormalizedCorrelations);
        double deltaPositive                     = 1 - Utils.max(positiveNormalizedCorrelations);

        double relativeSimilarity                = deltaNegative / (deltaNegative+deltaPositive);
        double subMax                            = Utils.max( positiveNormalizedCorrelations,
                                                             (int)Math.ceil(positiveNormalizedCorrelations.length/2 ) );
        deltaPositive = 1 - subMax;
        double conservativeSimilarity            = deltaNegative/ (deltaNegative + deltaPositive );

        return( new Foo(relativeSimilarity, conservativeSimilarity) );
    }

    public Foo getFoo (IplImage image, BoundingBox boundingBox) {
        float[] patchPattern = Utils.getPatchPattern( image, boundingBox, patchSize );
        return( getFoo( patchPattern ) );
    }

    public String getStatus () {
        return( "Positive:" + positivePatchPatterns.size() + " Negative:" + negativePatchPatterns.size() );
    }



    public static class Foo {
        public final double relativeSimilarity;
        public final double conservativeSimilarity;

        public Foo (double relativeSimilarity, double conservativeSimilarity) {
            this.relativeSimilarity = relativeSimilarity;
            this.conservativeSimilarity = conservativeSimilarity;
        }
    }
}
