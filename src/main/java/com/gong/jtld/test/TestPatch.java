package com.gong.jtld.test;

import com.gong.jtld.*;

import java.util.*;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestPatch {


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


        BoundingBox                 boundingBox         = new BoundingBox(300,30,335,105);
        List<ScanningBoundingBoxes> testBoundingBoxes   = null;


        IplImage currentGray = cvLoadImage("/Users/jerdavis/devhome/jtld/images/00005.png", CV_LOAD_IMAGE_GRAYSCALE);

        testBoundingBoxes = BoundingBox.createTestBoxes( boundingBox,
                                                         Jtdl.SCALES,
                                                         currentGray.width(),
                                                         currentGray.height(),
                                                         24 );

        float       CUTOFF = 0.6f;
        int         NUM_CLOSEST = 10; //Keep this many for the hull;
        BoundingBox bestBox = null;
        float       max = 0;
        List<Float> overlapList = new ArrayList<Float>();

        //BUGBUG: This isn't strictly correct... need to handle duplicate overlaps
        TreeMap<Float,BoundingBox> closestMap = new TreeMap<Float, BoundingBox>();

        for(ScanningBoundingBoxes boxes : testBoundingBoxes ) {
            for( BoundingBox box : boxes.boundingBoxes ) {
                float overlap = boundingBox.overlap(box);

                if( overlap > max ) {
                    bestBox = box;
                    max = overlap;
                }
                if( overlap > CUTOFF ) {
                    closestMap.put( overlap, box );
                }
                overlapList.add( overlap );
            }
        }
        NavigableMap<Float,BoundingBox> nav = closestMap.descendingMap();
        List<BoundingBox> closestList = new ArrayList<BoundingBox>();
        nav.pollFirstEntry();
        for( int x=0;x<NUM_CLOSEST;x++) {
            Map.Entry<Float,BoundingBox> entry = nav.pollFirstEntry();
            if( entry == null ) {
                break;
            }
            closestList.add(entry.getValue());
        }

        BoundingBox hullBox = BoundingBox.getHullBox( closestList );


        float[] bestPatchPattern = Utils.getPatchPattern(currentGray, bestBox, 15);
        //TODO: Also do MirrorBestPattern

//
//        //Fern f = new Fern();
//        int variance = 0;
//
//        IplImage fooImage = null;
//        f.getFernPatterns( fooImage,
//                           closestList,
//                           variance );
//
//


        System.out.println("Foo");


    }



}
