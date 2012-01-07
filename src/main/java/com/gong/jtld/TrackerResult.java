package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.CvPoint2D32f;

/**
 *
 */
public class TrackerResult {

    //These are the original Points that we are going to track flow from.
    public final CvPoint2D32f   origPoints;
    //These are the Points we found the flow to.
    public final CvPoint2D32f   foundPoints;
    public final float[]        forwardBackwardError;
    public final float[]        normCrossCorrelation;
    public final byte[]         featuresFound;
    public final float[]        featureErrors;
    public final int[]          validIndexes;

    public TrackerResult (CvPoint2D32f origPoints,
                          CvPoint2D32f foundPoints,
                          float[] forwardBackwardError,
                          float[] normCrossCorrelation,
                          byte[] featuresFound,
                          float[] featureErrors ) {

        //I'm straddling C/Java here
        this.forwardBackwardError = forwardBackwardError.clone();
        this.normCrossCorrelation = normCrossCorrelation.clone();
        this.featuresFound        = featuresFound.clone();
        this.featureErrors        = featureErrors.clone();
        if( getNumPoints() == 0 ) {
            throw new RuntimeException("No POints");
        }
        this.origPoints           = new CvPoint2D32f(getNumPoints());
        this.foundPoints          = new CvPoint2D32f(getNumPoints());
        for( int x=0;x<getNumPoints();x++) {
            this.foundPoints.position(x).set( foundPoints.position(x) );
            this.origPoints.position(x).set( origPoints.position(x) );
        }

        this.foundPoints.position(0);
        this.origPoints.position(0);

        this.validIndexes = Tracker.getValidIndexes( this );
    }

    public boolean isValid() {
        return( this.validIndexes.length > 0 );
    }


    public int getNumPoints() {
        return( forwardBackwardError.length );
    }

    public void destroy() {
        this.foundPoints.deallocate();
        this.origPoints.deallocate();
    }
}
