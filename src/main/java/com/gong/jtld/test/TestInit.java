package com.gong.jtld.test;

import com.gong.jtld.*;
import com.googlecode.javacv.cpp.opencv_core;

import java.util.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestInit {


    /**
     *
     * @param initialImage Gray scale
     * @param overlaps 0/1 how much does each overlap initial BB.
     */
    private static void initalPositive( IplImage initialImage,
                                        List<ScanningBoundingBoxes> boundingBoxeses,
                                        float[] overlaps ) {

        //get index/bb that has highest overlap
        //I don't know why we need to standardize on BB's
        BoundingBox bestBox = null;



        //Get BB's that overlap > 0.6 .. Get (sorted) max p_par.num_closest
        //also sent to fern later

        //get a bounding box/convex hull that covers all of the 0.6 overlap

        //get the specific columns and rows in the hull, and make a BB.
        BoundingBox similarBox;

        List<ScanningBoundingBoxes> similarBoundingBoxes;

        //pEx
        List<float[]> PositiveExamplePatterns;
        //getPattern( initialImage, bestBox, PATCHSIZE )
        //TODO: can also get the mirror image. (two patterns)

        //Now we will stretch/mangle the
        //to generate other similar positive patterns.
        int NUM_WARPS = 20;
        for( int x=0;x<NUM_WARPS;x++) {

        }






    }

    public static void main( String[] args ){
        System.out.println("Helloo!");
        System.out.println("Path:" + System.getProperty("java.library.path") );


        Tracker                     tracker             = new Tracker();
        BoundingBox                 boundingBox         = new BoundingBox(300,30,335,105);
        BoundingBox                 updatedBoundingBox  = new BoundingBox(300,30,335,105);
        List<ScanningBoundingBoxes> testBoundingBoxes   = null;
        TrackerResult               result              = null;
        int                         patchSize           = 25;
        CvFont                      font                = new CvFont(CV_FONT_HERSHEY_PLAIN, 1.0, 1);

        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);
        IplImage nextGray    = null;
        IplImage next        = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_UNCHANGED);

        //Get all possible BB's used in scanning the image for matches.
        testBoundingBoxes = BoundingBox.createTestBoxes( boundingBox, currentGray.width(),
                                                         currentGray.height(),
                                                         24 );

        //Generate Features used by Fern.. Random now.. but there are many better alg.
        //float[][] features = FeatureGenerator.generateFeatures(6, 23);

        //These are used by Nearest Neighbor
        List<float[]>  positivePatchPatterns = new ArrayList<float[]>();
        List<float[]>  negativePatchPatterns = new ArrayList<float[]>();


        //get the top N+1 overlaps > 0.6
        List<BoundingBox> bestOverlaps  = getBestOverlappingScanBoxes(boundingBox, testBoundingBoxes);
        //Get a random sampling of the 100 worst. <0.2 overlap
        List<BoundingBox> worstOverlaps = getWorstOverlappingScanBoxes(boundingBox, testBoundingBoxes);
        BoundingBox       bestBox       = bestOverlaps.remove(0);
        BoundingBox       hullBox       = BoundingBox.getHullBox( bestOverlaps );

        positivePatchPatterns = getPositivePatchPatterns( currentGray, bestBox, patchSize );
        negativePatchPatterns = getNegativePatchPatterns( currentGray, worstOverlaps, patchSize );


        NearestNeighbor nearestNeighbor = new NearestNeighbor();
        nearestNeighbor.init( positivePatchPatterns, negativePatchPatterns );


        System.out.println("Blah");

        //Draw initial bounding box. on first image
        cvRectangle( next,
                     cvPoint(Math.round(updatedBoundingBox.x1),
                             Math.round(updatedBoundingBox.y1)),
                     cvPoint(Math.round(updatedBoundingBox.x2),
                             Math.round(updatedBoundingBox.y2)),
                     CV_RGB(0, 255, 0), 1, 8, 0);
        cvSaveImage("/tmp/imageout-00001.png", next );


        int outputImageNo = 2;
        for( int x=6;x<=100;x++){
            NearestNeighbor.Foo foo = null;
            String temp     = "00000" + x;
            String inStr  = temp.substring(temp.length()-5,temp.length());
            temp     = "00000" + outputImageNo;
            String outStr  = temp.substring(temp.length()-5,temp.length());
            System.out.println("::"+inStr);
            nextGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_GRAYSCALE);
            next     = cvLoadImage("/Users/jerdavis/devhome/jtld/images/"+inStr+".png", CV_LOAD_IMAGE_UNCHANGED);

            result      = tracker.track(currentGray, nextGray, boundingBox );
            int[] validIndexes = Tracker.getValidIndexes(result);

            if( validIndexes.length > 0 ) {
                updatedBoundingBox = Tracker.predictBoundingBox(boundingBox, result, validIndexes);

                foo = nearestNeighbor.getFoo( nextGray, updatedBoundingBox, patchSize );
                cvPutText( next,
                           "" + foo.relativeSimilarity,
                           cvPoint(30,60),
                           font,
                           CV_RGB(0, 255, 0) );

            }else {
                System.out.println("Uh oh, no valid Indexes:" + inStr);
                //need to wait till detector finds object again
                //null BB.
            }
            boundingBox = updatedBoundingBox;
            //currentGray.release();
            currentGray = nextGray;

            if( boundingBox.isOutsideImage( next.width(), next.height() ) ) {
                System.out.println("Bounding Box is Outside Image: " + inStr );
            }

            cvRectangle(
                    next,
                    cvPoint(Math.round(updatedBoundingBox.x1), Math.round(updatedBoundingBox.y1)),
                    cvPoint(Math.round(updatedBoundingBox.x2), Math.round(updatedBoundingBox.y2)),
                    CV_RGB(0, 255, 0), 1, 8, 0);
            for( int y=0;y<validIndexes.length;y++) {
                result.origPoints.position(validIndexes[y]);
                result.foundPoints.position(validIndexes[y]);
                cvCircle( next,
                          cvPoint(Math.round(result.foundPoints.x()),Math.round(result.foundPoints.y())),
                          1,
                          CV_RGB(255, 255, 0), 1, 8, 0 );
                cvLine(next,
                   cvPoint(Math.round(result.origPoints.x()),Math.round(result.origPoints.y())),
                   cvPoint(Math.round(result.foundPoints.x()),Math.round(result.foundPoints.y())),
                   CV_RGB(255, 0, 0), 1, 8, 0);

            }
            cvSaveImage("/tmp/imageout-" + outStr + ".png", next);
            outputImageNo++;
        }

    }


    private static List<BoundingBox> getWorstOverlappingScanBoxes (BoundingBox boundingBox, List<ScanningBoundingBoxes> testBoundingBoxes) {
        float CUTOFF      = 0.2f;
        int   NUM_CLOSEST = 100;

        //BUGBUG: This isn't strictly correct... need to handle duplicate overlaps
        TreeMap<Float,BoundingBox> closestMap = new TreeMap<Float, BoundingBox>();

        //Look at every Scan.BB
        for(ScanningBoundingBoxes boxes : testBoundingBoxes ) {
            for( BoundingBox box : boxes.boundingBoxes ) {
                float overlap = boundingBox.overlap(box);
                if( overlap < CUTOFF ) {
                    closestMap.put( overlap, box );
                }
            }
        }

        List<BoundingBox> worstList = new ArrayList<BoundingBox>();
        for( int x=0;x<NUM_CLOSEST+1;x++) {
            worstList.add(closestMap.pollFirstEntry().getValue());
        }
        Collections.shuffle( worstList );

        return( worstList.subList(0, Math.min(100,worstList.size() ) ) );
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


    /**
     * Get the N+1 most overlapping BB's.. +1 for the best.. and N for the rest.
     * @param boundingBox
     * @param testBoundingBoxes
     * @return
     */
    public static List<BoundingBox> getBestOverlappingScanBoxes( BoundingBox boundingBox, List<ScanningBoundingBoxes> testBoundingBoxes ) {

        float CUTOFF = 0.6f;
        int NUM_CLOSEST = 10;

        //BUGBUG: This isn't strictly correct... need to handle duplicate overlaps
        TreeMap<Float,BoundingBox> closestMap = new TreeMap<Float, BoundingBox>();

        //Look at every Scan.BB
        for(ScanningBoundingBoxes boxes : testBoundingBoxes ) {
            for( BoundingBox box : boxes.boundingBoxes ) {
                float overlap = boundingBox.overlap(box);
                if( overlap > CUTOFF ) {
                    closestMap.put( overlap, box );
                }
            }
        }

        List<BoundingBox> closestList = new ArrayList<BoundingBox>();
        for( int x=0;x<NUM_CLOSEST+1;x++) {
            closestList.add( closestMap.pollFirstEntry().getValue() );
        }

        return( closestList );
    }


}
