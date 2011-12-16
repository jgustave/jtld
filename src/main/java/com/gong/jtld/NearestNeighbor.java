package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.util.*;

/**
 *
 */
public class NearestNeighbor {

    private final List<float[]> positivePatchPatterns = new ArrayList<float[]>();
    private final List<float[]> negativePatchPatterns = new ArrayList<float[]>();

    public void init ( List<float[]> positivePatchPatterns, List<float[]> negativePatchPatterns ) {
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
                } else {
                    Foo foo = getFoo(patch);


                    if( foo.relativeSimilarity <= 0.65 ) {
                    //TODO: not sure if we want to insert after most correlated...
                        this.positivePatchPatterns.add( patch );
                    }
                }
            }else { //assume negativePatch
                if( this.negativePatchPatterns.size() == 0 ) {
                    this.negativePatchPatterns.add( patch );
                }else {
                    Foo foo = getFoo(patch);
                    if( foo.relativeSimilarity > 0.5 ) {
                        this.negativePatchPatterns.add( patch );
                    }
                }
            }
        }

        //randomize positive and negative... but best should still be first.
        //Combine both sets and randomize...

        //just add if there are no pos/neg in the global yet

    }

    public Foo getFoo( float[] patch ) {
        if( positivePatchPatterns.size() == 0 ) {
            return( new Foo(0,0));
        }else if( negativePatchPatterns.size() == 0 ) {
            return( new Foo(1,1));
        }

        float[] positiveNormalizedCorrelations  = Utils.normalizedCorrelation( patch, positivePatchPatterns );
        float[] negativeNormalizedCorrelations  = Utils.normalizedCorrelation( patch, negativePatchPatterns );

        float deltaNegative                     = 1 - Utils.max(negativeNormalizedCorrelations);
        float deltaPositive                     = 1 - Utils.max(positiveNormalizedCorrelations);

        float relativeSimilarity                = deltaNegative / (deltaNegative+deltaPositive);
        float subMax                            = Utils.max( positiveNormalizedCorrelations,
                                                             (int)Math.ceil(positiveNormalizedCorrelations.length/2 ) );
        deltaPositive = 1 - subMax;
        float conservativeSimilarity            = deltaNegative/ (deltaNegative + deltaPositive );

        return( new Foo(relativeSimilarity, conservativeSimilarity) );
    }

    public Foo getFoo (IplImage image, BoundingBox boundingBox, int patchSize) {
        float[] patchPattern = Utils.getPatchPattern( image, boundingBox, patchSize );
        return( getFoo( patchPattern ) );
    }

    public static class Foo {
        public final float relativeSimilarity;
        public final float conservativeSimilarity;

        public Foo (float relativeSimilarity, float conservativeSimilarity) {
            this.relativeSimilarity = relativeSimilarity;
            this.conservativeSimilarity = conservativeSimilarity;
        }
    }
}
