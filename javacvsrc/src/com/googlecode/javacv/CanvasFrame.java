/*
 * Copyright (C) 2009,2010,2011 Samuel Audet
 *
 * This file is part of JavaCV.
 *
 * JavaCV is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCV is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCV.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.javacv;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_ProfileRGB;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JRootPane;

import static com.googlecode.javacv.cpp.opencv_core.*;

/**
 *
 * @author Samuel Audet
 * 
 * Make sure OpenGL is enabled to get low latency, something like
 *      export _JAVA_OPTIONS=-Dsun.java2d.opengl=True
 *
 */
public class CanvasFrame extends JFrame {
    public static String[] getScreenDescriptions() {
        GraphicsDevice[] screens = getScreenDevices();
        String[] descriptions = new String[screens.length];
        for (int i = 0; i < screens.length; i++) {
            descriptions[i] = screens[i].getIDstring();
        }
        return descriptions;
    }
    public static DisplayMode getDisplayMode(int screenNumber) {
        GraphicsDevice[] screens = getScreenDevices();
        if (screenNumber >= 0 && screenNumber < screens.length) {
            return screens[screenNumber].getDisplayMode();
        } else {
            return null;
        }
    }
    public static double getGamma(int screenNumber) {
        GraphicsDevice[] screens = getScreenDevices();
        if (screenNumber >= 0 && screenNumber < screens.length) {
            return getGamma(screens[screenNumber]);
        } else {
            return 0.0;
        }
    }

    public static double getGamma(GraphicsDevice screen) {
        ColorSpace cs = screen.getDefaultConfiguration().getColorModel().getColorSpace();
        if (cs.isCS_sRGB()) {
            return 2.2;
        } else {
            try {
                return ((ICC_ProfileRGB)((ICC_ColorSpace)cs).getProfile()).getGamma(0);
            } catch (Exception e) { }
        }
        return 0.0;
    }
    public static GraphicsDevice getScreenDevice(int screenNumber) throws Exception {
        GraphicsDevice[] screens = getScreenDevices();
        if (screenNumber >= screens.length) {
            throw new Exception("CanvasFrame Error: Screen number " + screenNumber + " not found. " +
                                "There are only " + screens.length + " screens.");
        }
        return screens[screenNumber];//.getDefaultConfiguration();
    }
    public static GraphicsDevice[] getScreenDevices() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    }

    public CanvasFrame(String title) {
        this(title, 0.0);
    }
    public CanvasFrame(String title, double gamma) {
        super(title);
        init(false, null, gamma);
    }

    public CanvasFrame(String title, GraphicsConfiguration gc) {
        this(title, gc, 0.0);
    }
    public CanvasFrame(String title, GraphicsConfiguration gc, double gamma) {
        super(title, gc);
        init(false, null, gamma);
    }

    public CanvasFrame(String title, int screenNumber, DisplayMode displayMode) throws Exception {
        this(title, screenNumber, displayMode, 0.0);
    }
    public CanvasFrame(String title, int screenNumber, DisplayMode displayMode, double gamma) throws Exception {
        super(title, getScreenDevice(screenNumber).getDefaultConfiguration());
        init(true, displayMode, gamma);
    }

    @Override public void dispose() {
        bufferStrategy.dispose();
        super.dispose();
    }
    private void init(final boolean fullScreen, final DisplayMode displayMode, final double gamma) {
        Runnable r = new Runnable() { public void run() {
            GraphicsDevice gd = getGraphicsConfiguration().getDevice();
            DisplayMode d = gd.getDisplayMode(), d2 = null;
            if (displayMode != null && d != null) {
                int w = displayMode.getWidth();
                int h = displayMode.getHeight();
                int b = displayMode.getBitDepth();
                int r = displayMode.getRefreshRate();
                d2 = new DisplayMode(w > 0 ? w : d.getWidth(),    h > 0 ? h : d.getHeight(),
                                     b > 0 ? b : d.getBitDepth(), r > 0 ? r : d.getRefreshRate());
            }
            if (fullScreen) {
                setUndecorated(true);
                getRootPane().setWindowDecorationStyle(JRootPane.NONE);
                setResizable(false);
                gd.setFullScreenWindow(CanvasFrame.this);
            }
            if (d2 != null && !d2.equals(d)) {
                gd.setDisplayMode(d2);
            }
            double g = gamma == 0.0 ? getGamma(gd) : gamma;
            invgamma = g == 0.0 ? 1.0 : 1.0/g;

            // must be called after the fullscreen stuff, but before
            // getting our BufferStrategy
            setVisible(true);

            canvas = new Canvas() {
                @Override public void paint(Graphics g) {
                    // Try to redraw the front buffer when the OS says it has stomped
                    // on it, using the back buffer. Calling bufferStrategy.show() here
                    // sometimes throws NullPointerException or IllegalStateException,
                    // but otherwise seems to work fine.
                    try {
                        bufferStrategy.show();
                    } catch (NullPointerException e) {
                    } catch (IllegalStateException e) { }
                }
            };
            if (fullScreen) {
                canvas.setSize(getSize());
                needInitialResize = false;
            } else {
                needInitialResize = true;
            }
            getContentPane().add(canvas);
            canvas.setVisible(true);
            canvas.createBufferStrategy(2);
            //canvas.setIgnoreRepaint(true);
            bufferStrategy = canvas.getBufferStrategy();

            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    addKeyEventDispatcher(new KeyEventDispatcher() {
                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        synchronized (CanvasFrame.this) {
                            keyEvent = e;
                            CanvasFrame.this.notify();
                        }
                    }
                    return false;
                }
            });
        }};

        if (EventQueue.isDispatchThread()) {
            r.run();
        } else {
            try {
                EventQueue.invokeAndWait(r);
            } catch (Exception ex) { }
        }
    }

    public DisplayMode getDisplayMode() {
        return getGraphicsConfiguration().getDevice().getDisplayMode();
    }

    // used for example as debugging console...
    public static CanvasFrame global = null;

    // maximum is 60 ms on Metacity and Windows XP, and 90 ms on Compiz Fusion,
    // but set the default to twice as much for safety...
    public static final long DEFAULT_LATENCY = 120;
    private long latency = DEFAULT_LATENCY;

    private KeyEvent keyEvent = null;

    private Canvas canvas = null;
    private boolean needInitialResize = false;
    private BufferStrategy bufferStrategy = null;
    private double invgamma;

    public long getLatency() {
        // if there exists some way to estimate the latency in real time,
        // add it here
        return latency;
    }
    public void setLatency(long latency) {
        this.latency = latency;
    }
    public void waitLatency() {
        try {
            Thread.sleep(getLatency());
        } catch (InterruptedException ex) { }
    }

    public KeyEvent waitKey() {
        return waitKey(0);
    }
    public synchronized KeyEvent waitKey(int delay) {
        try {
            keyEvent = null;
            wait(delay);
        } catch (InterruptedException ex) { }
        KeyEvent e = keyEvent;
        keyEvent = null;
        return e;
    }

    public Canvas getCanvas() {
        return canvas;
    }
    @Override public BufferStrategy getBufferStrategy() {
        return bufferStrategy;
    }

    public Graphics2D createGraphics() {
        return (Graphics2D)bufferStrategy.getDrawGraphics();
    }
    public void releaseGraphics(Graphics2D g) {
        g.dispose();
        bufferStrategy.show();
    }

    public Dimension getCanvasSize() {
        return canvas.getSize();
    }
    public void setCanvasSize(int width, int height) {
        // there is apparently a bug in Java code for Linux, and what happens goes like this:
        // 1. Canvas gets resized, checks the visible area (has not changed) and updates
        // BufferStrategy with the same size. 2. pack() resizes the frame and changes
        // the visible area 3. We call Canvas.setSize() with different dimensions, to make
        // it check the visible area and reallocate the BufferStrategy almost correctly
        // 4. We resize the Canvas to the desired size... pff..
        setExtendedState(NORMAL); // force unmaximization.. 
        canvas.setSize(width, height);
        pack();
        canvas.setSize(width+1, height+1);
        canvas.setSize(width, height);
        needInitialResize = false;
    }

    public void showImage(Image image, final int w, final int h) {
        if (image == null) {
            return;
        } else if(isResizable() && needInitialResize) {
            if (EventQueue.isDispatchThread()) {
                setCanvasSize(w, h);
            } else {
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        public void run() {
                            setCanvasSize(w, h);
                        }
                    });
                } catch (Exception ex) { }
            }
        }
        Graphics2D g = createGraphics();
        g.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight(), null);
        releaseGraphics(g);
    }
    public void showImage(Image image, double scale) {
        if (image == null)
            return;
        int w = (int)Math.round(image.getWidth (null)*scale);
        int h = (int)Math.round(image.getHeight(null)*scale);
        showImage(image, w, h);
    }
    public void showImage(Image image) {
        showImage(image, 1.0);
    }

    // Java2D will do gamma correction for TYPE_CUSTOM BufferedImage, but
    // not for the standard types, so we need to do it manually...
    public void showImage(IplImage image, int w, int h) {
        showImage(image.getBufferedImage(image.getBufferedImageType() ==
                BufferedImage.TYPE_CUSTOM ? 1.0 : invgamma), w, h);
    }
    public void showImage(IplImage image, double scale) {
        showImage(image.getBufferedImage(image.getBufferedImageType() ==
                BufferedImage.TYPE_CUSTOM ? 1.0 : invgamma), scale);
    }
    public void showImage(IplImage image) {
        showImage(image.getBufferedImage(image.getBufferedImageType() ==
                BufferedImage.TYPE_CUSTOM ? 1.0 : invgamma));
    }

    public void showColor(Color color) {
        Graphics2D g = createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        releaseGraphics(g);
    }
    public void showColor(CvScalar color) {
        showColor(new Color((int)color.red(), (int)color.green(), (int)color.blue()));
    }

    // this should not be called from the event dispatch thread, but if it is,
    // it should still work... it should simply be slower as it will timeout
    // waiting for the moved event
    public static void tile(final CanvasFrame[] frames) {

        class MovedListener extends ComponentAdapter {
            boolean moved = false;
            @Override public void componentMoved(ComponentEvent e) {
                moved = true;
                Component c = e.getComponent();
                synchronized (c) {
                    c.notify();
                }
            }
        }
        final MovedListener movedListener = new MovedListener();

        // layout the canvas frames for the cameras in tiles
        int canvasCols = (int)Math.round(Math.sqrt(frames.length));
        if (canvasCols*canvasCols < frames.length) {
            // if we don't get a square, favor horizontal layouts
            // since screens are usually wider than cameras...
            // and we also have title bars, tasks bar, menus, etc that
            // takes up vertical space
            canvasCols++;
        }
        int canvasX = 0, canvasY = 0;
        int canvasMaxY = 0;
        for (int i = 0; i < frames.length; i++) {
            final int n = i;
            final int x = canvasX;
            final int y = canvasY;
            try {
                movedListener.moved = false;
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        frames[n].addComponentListener(movedListener);
                        frames[n].setLocation(x, y);
                    }
                });
                int count = 0;
                while (!movedListener.moved && count < 5) {
                    // wait until the window manager actually places our window...
                    // wait a maximum of 500 ms since this does not work if
                    // we are on the event dispatch thread. also some window
                    // managers like Windows do not always send us the event...
                    synchronized (frames[n]) {
                        frames[n].wait(100);
                    }
                    count++;
                }
                EventQueue.invokeAndWait(new Runnable() {
                    public void run() {
                        frames[n].removeComponentListener(movedListener);
                    }
                });
            } catch (Exception ex) { }
            canvasX = frames[i].getX()+frames[i].getWidth();
            canvasMaxY = Math.max(canvasMaxY, frames[i].getY()+frames[i].getHeight());
            if ((i+1)%canvasCols == 0) {
                canvasX = 0;
                canvasY = canvasMaxY;
            }
        }
    }

}
