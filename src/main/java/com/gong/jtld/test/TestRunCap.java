package com.gong.jtld.test;

import com.gong.jtld.BoundingBox;
import com.gong.jtld.Jtdl;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.OpenCVFrameGrabber;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

/**
 * DYLD_FALLBACK_LIBRARY_PATH=/opt/local/lib/
 * ffmpeg -qscale 5 -r 5 -b 9600 -i imageout-%05d.png movie.mp4
 */
public class TestRunCap {
    private static Point start = null;
    private static Point end = null;
    private static Jtdl jtdl = null;
    private static BoundingBox firstBox   = null;
    private static BoundingBox currentBox = null;
    public static void main( String[] args ) throws Exception {

        jtdl = new Jtdl();
        System.out.println("Helloo!");
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0);
        grabber.start();
        CanvasFrame canvas = new CanvasFrame( "Test" );

        canvas.getCanvas().addMouseListener(
                new MouseListener() {
                    public void mouseClicked (MouseEvent mouseEvent) {}

                    public void mousePressed (MouseEvent mouseEvent) {
                        start = mouseEvent.getLocationOnScreen();
                    }

                    public void mouseReleased (MouseEvent mouseEvent) {
                        end = mouseEvent.getLocationOnScreen();
                        firstBox = new BoundingBox(start.x, start.y, end.x, end.y );
                    }

                    public void mouseEntered (MouseEvent mouseEvent) {}

                    public void mouseExited (MouseEvent mouseEvent) {}
                });

        IplImage image = null;
        boolean didInit = false;

        while( true ) {
            image = grabber.grab();
            if( !didInit  &&  firstBox != null ) {

                IplImage temp = image.clone();
                cvRectangle(
                            temp,
                            cvPoint(Math.round(firstBox.x1), Math.round(firstBox.y1)),
                            cvPoint(Math.round(firstBox.x2), Math.round(firstBox.y2)),
                            CV_RGB(255, 0, 0), 1, 8, 0);
                canvas.showImage( temp );

                jtdl.init( image, firstBox );
                didInit = true;
            } else {
                if( didInit ) {
                    jtdl.processFrame( image );
                }
                currentBox = jtdl.getCurrentBoundingBox();
                if( currentBox != null ) {
                        cvRectangle(
                            image,
                            cvPoint(Math.round(currentBox.x1), Math.round(currentBox.y1)),
                            cvPoint(Math.round(currentBox.x2), Math.round(currentBox.y2)),
                            CV_RGB(255, 0, 0), 1, 8, 0);
                }
            }
            canvas.showImage( image );
        }

    }



}
