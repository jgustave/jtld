package com.gong.jtld;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

import java.util.*;

import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;

/**
 *
 */
public class NearestNeighbor {

    private final List<float[]> positivePatchPatterns = new ArrayList<float[]>();
    private final List<float[]> negativePatchPatterns = new ArrayList<float[]>();

    private final List<ScanningBoundingBoxes> scanningBoundingBoxeses;
    private final int patchSize;

    public NearestNeighbor(List<ScanningBoundingBoxes> scanBoxes, int patchSize ) {
        this.scanningBoundingBoxeses = scanBoxes;
        this.patchSize = patchSize;
    }

    public void init (IplImage image, List<BoundingBox> bestOverlaps, List<BoundingBox> worstOverlaps) {


        //List<Map<IplImage,float[]>
        //List<Map>
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
                    if( foo.relativeSimilarity <= 0.65 ) {
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
                    if( foo.relativeSimilarity > 0.5 ) {
                        //dump( "Negative-" + foo.relativeSimilarity + "-" + this.negativePatchPatterns.size(), patch );
                        this.negativePatchPatterns.add( patch );
                    }
                }
            }
        }
    }

//
//    public void init (IplImage image, List<BoundingBox> bestOverlaps, List<BoundingBox> worstOverlaps) {
//
//
//        //List<Map<IplImage,float[]>
//        //List<Map>
//        Map<float[],IplImage>  positivePatchPatterns = getPositivePatchPatternsDebug( image, bestOverlaps.get(0), patchSize );
//        Map<float[],IplImage>  negativePatchPatterns = getNegativePatchPatternsDebug( image, worstOverlaps, patchSize );
//
//        Set<float[]> positives = new HashSet<float[]>();
//        positives.addAll( positivePatchPatterns.keySet() );
//
//        List<float[]> patches = new ArrayList<float[]>();
//        patches.addAll( positivePatchPatterns.keySet() );
//        patches.addAll( negativePatchPatterns.keySet() );
//        Collections.shuffle( patches );
//        //important to have positive first to init
//        patches.add(0,positivePatchPatterns.keySet().iterator().next() );
//
//        for( float[] patch : patches ) {
//
//
//            //Since we know the patch is apriori a Positive or Negative patch..
//            //AND we look at the Similarity (0 being similar to a negative, 1 being similar to a positive)
//            //We say: If it's a Positive Patch, but more similar to a negative patch.. we want to add it to
//            //the collection. (and vv)
//            if( positives.contains(patch) ) {
//                if( this.positivePatchPatterns.size() == 0 ) {
//                    this.positivePatchPatterns.add( patch );
//                    dump( "Positive:" + this.positivePatchPatterns.size(), positivePatchPatterns.get(patch) );
//                } else {
//                    Foo foo = getFoo(patch);
//                    if( foo.relativeSimilarity <= 0.65 ) {
//                        this.positivePatchPatterns.add( patch );
//                        dump( "Positive-" + foo.relativeSimilarity +"-" + this.positivePatchPatterns.size(), positivePatchPatterns.get(patch) );
//                    }
//                }
//            }else { //assume negativePatch
//                if( this.negativePatchPatterns.size() == 0 ) {
//                    this.negativePatchPatterns.add( patch );
//                    dump( "Negative:" + this.positivePatchPatterns.size(), negativePatchPatterns.get(patch) );
//                }else {
//                    Foo foo = getFoo(patch);
//                    if( foo.relativeSimilarity > 0.5 ) {
//                        dump( "Negative-" + foo.relativeSimilarity + "-" + this.negativePatchPatterns.size(), negativePatchPatterns.get(patch) );
//                        this.negativePatchPatterns.add( patch );
//                    }
//                }
//            }
//        }
//    }


//
//    private void dump(String msg, IplImage image ) {
//
////        IplImage dest = cvCreateImage( cvSize(patchSize,
////                                              patchSize),
////                                       8,
////                                       1 );
////        BytePointer data = dest.imageData();
////        float min = Utils.min( pattern );
////
////        for(float val : pattern ) {
////            data.put((byte)(val - min) );
////        }
////
////        cvSaveImage("/tmp/"+msg+".png", dest);
////        cvReleaseImage( dest );
//        cvSaveImage("/tmp/"+msg+".png", image );
//
//    }
//

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
//    private static Map<float[],IplImage> getPositivePatchPatternsDebug ( IplImage image,
//                                                                        BoundingBox bestBox,
//                                                                        int patternSize ) {
//        Map<float[],IplImage> temp = new LinkedHashMap<float[], IplImage>();
//
//        temp.put( Utils.getPatchPattern( image,
//                                         bestBox,
//                                         patternSize ),
//                  Utils.getImagePatch( image, bestBox, Utils.NO_FLIP ) );
//        temp.put( Utils.getPatchPattern( image,
//                                         bestBox,
//                                         patternSize,
//                                         Utils.X_FLIP),
//                  Utils.getImagePatch( image, bestBox, Utils.X_FLIP ) );
//        temp.put( Utils.getPatchPattern( image,
//                                         bestBox,
//                                         patternSize,
//                                         Utils.Y_FLIP),
//                  Utils.getImagePatch( image, bestBox, Utils.Y_FLIP ) );
//        return(temp);
//    }

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
//
//    private static Map<float[],IplImage> getNegativePatchPatternsDebug (IplImage image,
//                                                                        List<BoundingBox> worstOverlaps,
//                                                                        int patternSize ) {
//        Map<float[],IplImage> temp = new HashMap<float[], IplImage>();
//
//        for( BoundingBox box : worstOverlaps ) {
//            temp.put(Utils.getPatchPattern( image,
//                                            box,
//                                            patternSize ),
//                     Utils.getImagePatch( image, box ) );
//        }
//        return( temp );
//    }

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
