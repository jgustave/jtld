package com.gong.jtld;

import java.util.Random;

/**
 * Generates Features used by the Fern classifier
 */
public class FeatureGenerator {
    private static final Random rand = new Random();

    /**
     * TODO: revist this later
     * @param numTrees
     * @param numFeatures
     * @return
     */
    public static float[][] generateFeatures( int numTrees, int numFeatures ) {
        float[][]   result  = null;

        result = new float[numTrees][numFeatures];

        for( int x=0;x<numTrees;x++) {
            for( int y=0;y<numFeatures;y++) {
                result[x][y] = rand.nextFloat();
            }
        }

        return( result );
    }
}
