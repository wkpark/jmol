
/*
 * Copyright 2001 The Jmol Development Team
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */
package org.openscience.jmol.applet;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import org.openscience.jmol.ChemFrame;
import org.openscience.jmol.ChemFile;
import org.openscience.jmol.DisplaySettings;
import org.openscience.jmol.AtomRenderer;
import org.openscience.jmol.FortranFormat;
import org.openscience.jmol.ChemFrameRenderer;

public class DisplayPanel extends Canvas
        implements java.awt.event.ComponentListener,
          java.awt.event.ActionListener {

  private String message = "Waiting for structure...";
  private DisplaySettings settings;
  private ChemFrameRenderer chemFrameRenderer = new ChemFrameRenderer();
  private boolean perspective = false;
  private float fieldOfView;
  private boolean painted = false;
  private boolean haveFile = false;
  private boolean rubberband = false;
  private boolean antiAliased = false;
  private int bx;
  private int by;
  private int rtop;
  private int rbottom;
  private int rleft;
  private int rright;
  private int nframes = 0;
  private int frameIndex = -1;
  private int prevx;
  private int prevy;
  private int outx;
  private int outy;
  private Point3f useMinBound = new Point3f();
  private Point3f useMaxBound = new Point3f();
  private Matrix4d amat = new Matrix4d();    // Matrix to do mouse angular rotations.
  private Matrix4d tmat = new Matrix4d();    // Matrix to do translations.
  private Matrix4d mat = new Matrix4d();    // Final matrix for assembly on screen.
  private boolean matIsValid = false;        // is mat valid ???
  private float zoomFactor = 0.7f;
  private java.awt.Label frameLabel = null;
  float[] quat = new float[4];
  double[] mtmp;
  String[] names;
  Color[] colors;
  ChemFile cf;
  ChemFrame md = null;
  private float xfac0 = 1;    // Zoom factor determined by screen size/initial settings.
  private float xfac = 1;                    // Zoom as performed by the user
  private float xfac2 = 1;                   // Square of zoom performed by the user
  public static final int ROTATE = 0;
  public static final int ZOOM = 1;
  public static final int XLATE = 2;
  public static final int PICK = 3;
  public static final int DEFORM = 4;
  public static final int MEASURE = 5;
  public static final int SINGLEPICK = 0;
  public static final int MULTIPLEPICK = 1;
  public static final String customViewPrefix = "VIEW.";
  public static final String resetViewToken = "HOME";
  public static final String rotateToken = "ROTATE";
  public static final String frameToken = "FRAME";
  public static final String zoomToken = "ZOOM";
  public static final String translateToken = "TRANSLATE";

  private int mode = ROTATE;
  private int pickingMode = SINGLEPICK;
  private static Color backgroundColor = (java.awt.Color.black);

  //Added T.GREY for moveDraw support- should be true while mouse is dragged
  private boolean mouseDragged = false;
  private boolean wireFrameRotation = true;

  //
  //    private boolean showPopupMenu = true;
  //    private int popButtonSideLength =10;
  //    private javax.swing.JPopupMenu popup;
  private java.awt.Image db = null;

  private int drawWidth;
  private int drawHeight;

  public DisplayPanel() {

    super();
    resetView();

    //Create the popup menu.
    //      popup = createPopupFromString(null);
    //      setBackground(java.awt.Color.black);
    //      backgroundColor = (java.awt.Color.black);
    setForeground(java.awt.Color.white);
    AtomRenderer.setImageComponent(this);
    this.addMouseListener(new MyAdapter());
    this.addComponentListener(this);
    this.addMouseMotionListener(new MyMotionAdapter());
    this.addKeyListener(new MyKeyListener());

    //              updateSizes();
  }

  public void updateSizes() {

    java.awt.Dimension s = this.getSize();
    drawWidth = s.width;
    drawHeight = s.height;
    if ((drawWidth > 0) && (drawHeight > 0)) {
      db = createImage(drawWidth, drawHeight);
    } else {
      db = null;
    }
    painted = false;
  }

  public void displaySettingsChanged() {
    painted = false;
  }

  public void resetView() {

    amat.setIdentity();
    tmat.setIdentity();
    xfac = 1;
    xfac2 = 1;
    if (settings != null) {
      settings.setBondScreenScale(xfac0);
      settings.setAtomScreenScale(xfac0);
    }
    quat[0] = 0.0f;
    quat[1] = 0.0f;
    quat[2] = 0.0f;
    quat[3] = 1.0f;
    matIsValid = false;
    painted = false;
    if (db != null) {
      repaint();
    }
  }

  /*
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  rotationString is a string which can specify any rotation.
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  The string is formatted as "xrot,yrot,xrot".
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  There is no Z-rot, since any rotation can be created as a combination
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  of X-Y-X rotations, and this way is programmatically much easier !
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  Angles are in degrees.
  */
  public void doRotationString(String rotationString) {

    StringTokenizer st = new StringTokenizer(rotationString, ",", false);
    float rotTheta = 0;

    try {
      rotTheta = (float) FortranFormat.atof(st.nextToken());
      Matrix4d matrix = new Matrix4d();
      matrix.rotY(rotTheta*Math.PI/180.0);
      amat.mul(matrix, amat);
      rotTheta = (float) FortranFormat.atof(st.nextToken());
      matrix.rotX(rotTheta*Math.PI/180.0);
      amat.mul(matrix, amat);
      rotTheta = (float) FortranFormat.atof(st.nextToken());
      matrix.rotZ(rotTheta*Math.PI/180.0);
      amat.mul(matrix, amat);
    } catch (NoSuchElementException E) {
    }
    matIsValid = false;
    painted = false;
    if (db != null) {
      repaint();
    }
  }

  public void doZoomString(String zoomString) {
    float zf = (float) FortranFormat.atof(zoomString);
    doZoom(zf);
  }

  public void doFrameString(String frameString) {
    int frame = Integer.parseInt(frameString);
    setFrame(frame);
  }

  /* Format is "x,y"
  */
  public void doTranslateString(String translateString) {

    StringTokenizer st = new StringTokenizer(translateString, ",", false);
    float deltax = 0;
    float deltay = 0;

    try {
      deltax = (float) FortranFormat.atof(st.nextToken());
      deltay = (float) FortranFormat.atof(st.nextToken());
    } catch (java.util.NoSuchElementException E) {
    }
    Matrix4d matrix = new Matrix4d();
    matrix.setTranslation(new Vector3d(deltax, deltay, 0.0));
    tmat.add(matrix);
    matIsValid = false;
    painted = false;
    if (db != null) {
      repaint();
    }
  }

  /* Format is:
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  "FRAME=n;ROTATION=x,y,x;ZOOM=n;TRANSLATE=x,y"
  */
  public void doCustomViewString(String customView) {

    StringTokenizer st = new StringTokenizer(customView, ";", false);
    String viewToken = null;

    //              resetView();
    try {
      while (true) {
        viewToken = st.nextToken();
        processViewString(viewToken);
      }
    } catch (NoSuchElementException E) {
    }
  }

  public void processViewString(String viewToken) {

    StringTokenizer st = new StringTokenizer(viewToken, "=", false);

    String token = st.nextToken();
    String data = null;
    try {
      data = st.nextToken();
    } catch (java.util.NoSuchElementException e) {
    }
    if (token.equals(resetViewToken)) {
      resetView();
    } else if (token.equals(rotateToken)) {
      doRotationString(data);
    } else if (token.equals(frameToken)) {
      doFrameString(data);
    } else if (token.equals(zoomToken)) {
      doZoomString(data);
    } else if (token.equals(translateToken)) {
      doTranslateString(data);
    }

    // Ignore unknown tokens.
  }

  public void doZoom(float zoom) {

    xfac *= zoom;
    xfac2 *= zoom * zoom;
    if (settings != null) {
      settings.setBondScreenScale(xfac2 * xfac0);
      settings.setAtomScreenScale(xfac2 * xfac0);
    }
    matIsValid = false;
    painted = false;
    if (db != null) {
      repaint();
    }
  }

  /*
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  This routine sets the initial zoom factor.
  */
  public void setZoomFactor(float factor) {
    zoomFactor = factor * 0.7f;
    if (db != null) {
      init();
    }
  }

  // Make sure AWT knows we are using a buffered image.
  public boolean isDoubleBuffered() {
    return true;
  }

  // Make sure AWT knows we repaint the entire canvas.
  public boolean isOpaque() {
    return true;
  }

  public boolean getAntiAliased() {
    return antiAliased;
  }

  public void setAntiAliased(boolean aa) {
    antiAliased = aa;
  }

  public int getMode() {
    return mode;
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public int getPickingMode() {
    return pickingMode;
  }

  public void setPickingMode(int mode) {
    this.pickingMode = mode;
  }

  public void setDisplaySettings(DisplaySettings settings) {
    this.settings = settings;
    settings.setBondScreenScale(xfac2 * xfac0);
    settings.setAtomScreenScale(xfac2 * xfac0);
    painted = false;
  }

  //   public boolean getPopupMenuActive(){
  //       return showPopupMenu;
  //   }

  //   public void setPopupMenuActive(boolean visible){
  //       showPopupMenu = visible;
  //       if (painted){
  //         painted = false;
  //         repaint();
  //       }
  //   }


  public void setChemFile(ChemFile cf) {

    this.cf = cf;
    painted = false;
    haveFile = true;
    nframes = cf.getNumberFrames();
    this.md = cf.getFrame(0);
    init();
    setFrame(0);
  }

  public void init() {

    md.findBounds();
    useMinBound = md.getMinimumBounds();
    useMaxBound = md.getMaximumBounds();
    updateSizes();
    Point3f size = new Point3f();
    size.sub(useMaxBound, useMinBound);
    float width = size.x;
    if (size.y > width) {
      width = size.y;
    }
    if (size.z > width) {
      width = size.z;
    }
    float f1 = drawWidth / width;
    float f2 = drawHeight / width;
    xfac0 = zoomFactor;
    if (f1 < f2) {
      xfac0 *= f1;
    } else {
      xfac0 *= f2;
    }

    matIsValid = false;
    if (settings != null) {
      settings.setBondScreenScale(xfac2 * xfac0);
      settings.setAtomScreenScale(xfac2 * xfac0);
    }
  }

  // Define a label component which will display the frame label (if one exists)
  public void setFrameLabel(java.awt.Label newLabel) {

    frameLabel = newLabel;
    if ((frameIndex != -1) && (frameLabel != null)) {
      frameLabel.setText(md.getInfo());
      frameLabel.setSize(frameLabel.getPreferredSize());
    }
  }

  public void clearFrameLabel() {
    frameLabel = null;
  }

  public void setFrame(int fr) {

    if (haveFile) {
      if (fr < nframes) {
        md = cf.getFrame(fr);
        frameIndex = fr;
        if (frameLabel != null) {
          frameLabel.setText(md.getInfo());
          frameLabel.setSize(frameLabel.getPreferredSize());
        }
      }
      painted = false;
      repaint();
    }
  }

  public ChemFrame getFrame() {
    return md;
  }

  /** Sets the status message to read whatever is in msg. 'Status message' here means the text in the corner of the applet.**/
  public void setStatusMessage(String msg) {
    message = msg;
    repaint();
  }

  class MyAdapter extends MouseAdapter {

    public void mouseEntered(MouseEvent e) {
      requestFocus();
    }

    public void mousePressed(MouseEvent e) {

      prevx = e.getX();
      prevy = e.getY();

      if (mode == PICK) {
        rubberband = true;
        bx = e.getX();
        rright = bx;
        rleft = bx;
        by = e.getY();
        rtop = by;
        rbottom = by;
      }

    }

    public void mouseClicked(MouseEvent e) {

      if (haveFile) {
        if (pickingMode == MULTIPLEPICK) {
          md.shiftSelectAtom(e.getX(), e.getY());
        } else {
          md.selectAtom(e.getX(), e.getY());
        }
        painted = false;
        repaint();
      }
    }

    public void mouseReleased(MouseEvent e) {

      //NEW LINE T.GREY
      if (mouseDragged && wireFrameRotation) {
        settings.setFastRendering(false);
        if (painted) {
          painted = false;
          repaint();
        }
        mouseDragged = false;
      }

      outx = e.getX();
      outy = e.getY();

      if (mode == PICK) {
        rubberband = false;
        repaint();
      }

    }
  }

  class MyMotionAdapter extends MouseMotionAdapter {

    public void mouseDragged(MouseEvent e) {

      int x = e.getX();
      int y = e.getY();

      //NEW LINE T.GREY
      if (wireFrameRotation) {
        settings.setFastRendering(true);
        mouseDragged = true;
      }
      if (mode == ROTATE) {

        /*
        float[] spin_quat = new float[4];
        Trackball tb = new Trackball(spin_quat,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        (2.0f*x - drawWidth) / drawWidth,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        (drawHeight-2.0f*y) / drawHeight,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        (2.0f*prevx - drawWidth) / drawWidth,
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        (drawHeight-2.0f*prevy) / drawHeight);

        tb.add_quats(spin_quat, quat, quat);
        tb.build_rotmatrix(amat, quat);

        */
        double xtheta = (y - prevy) * (2.0 * Math.PI / drawWidth);
        double ytheta = (x - prevx) * (2.0 * Math.PI / drawHeight);

        Matrix4d matrix = new Matrix4d();
        matrix.rotX(xtheta);
        amat.mul(matrix, amat);
        matrix.rotY(ytheta);
        amat.mul(matrix, amat);
        matIsValid = false;
        painted = false;
        repaint();
      }

      if (mode == XLATE) {
        float dx = (x - prevx);
        float dy = (y - prevy);
        Matrix4d matrix = new Matrix4d();
        matrix.setTranslation(new Vector3d(dx, dy, 0.0));
        tmat.add(matrix);
        matIsValid = false;
        painted = false;
        repaint();
      }

      if (mode == ZOOM) {
        float xs = 1.0f + (float) (x - prevx) / drawWidth;
        float ys = 1.0f + (float) (prevy - y) / drawHeight;
        float scale = (xs + ys) / 2.0f;
        doZoom(scale);
      }

      if (mode == PICK) {
        if (x < bx) {
          rleft = x;
          rright = bx;
        } else {
          rleft = bx;
          rright = x;
        }
        if (y < by) {
          rtop = y;
          rbottom = by;
        } else {
          rtop = by;
          rbottom = y;
        }
        if (haveFile) {
          if (e.isShiftDown()) {
            md.shiftSelectRegion(rleft, rtop, rright, rbottom);
          } else {
            md.selectRegion(rleft, rtop, rright, rbottom);
          }
          painted = false;
          repaint();
        }
      }

      prevx = x;
      prevy = y;
    }
  }

  class MyKeyListener extends java.awt.event.KeyAdapter {

    public void keyPressed(java.awt.event.KeyEvent e) {

      if (e.getKeyCode() == java.awt.event.KeyEvent.VK_SHIFT) {
        mode = ZOOM;
      } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_CONTROL) {
        mode = XLATE;
      }
    }

    public void keyReleased(java.awt.event.KeyEvent e) {
      mode = ROTATE;
    }
  }

  public void rebond() throws Exception {
    if (md != null) {
      md.rebond();
    }
  }

  static void setBackgroundColor(Color bg) {
    backgroundColor = bg;
  }

  static Color getBackgroundColor() {
    return backgroundColor;
  }

  public void setForegroundColor(Color fg) {
    setForeground(fg);
    painted = false;
  }

  public Color getForegroundColor() {
    return getForeground();
  }

  public void setPerspective(boolean p) {
    perspective = p;
    painted = false;
  }

  public boolean getPerspective() {
    return perspective;
  }

  public void setFieldOfView(float fov) {
    fieldOfView = fov;
    painted = false;
  }

  public float getFieldOfView() {
    return fieldOfView;
  }

  public void update(Graphics g) {
    paint(g);
  }

  public void paint(Graphics g) {

    Graphics gps = null;

    if (db == null) {
      return;
    } else if (cf == null) {
      g.drawString(message, 10, 10);
      return;
    }
    gps = db.getGraphics();
    paintBuffer(gps);
    gps.dispose();    // dispose of graphics to prevent system resource problems.
    g.drawImage(db, 0, 0, this);
  }


  public void paintBuffer(Graphics g) {

    //        if (backgroundColor == null) setBackgroundColor();

    Color bg = backgroundColor;
    Color fg = getForeground();

    if (md == null) {
      g.setColor(bg);
      g.fillRect(0, 0, drawWidth, drawHeight);
    } else {
      if (!matIsValid) {

        // Only rebuild the master matrix if component matrices have changed.
        float xft = xfac2 * xfac0;
        painted = false;
        mat.setIdentity();
        Matrix4d matrix = new Matrix4d();
        matrix.setTranslation(new Vector3d(-(useMinBound.x + useMaxBound.x)
                / 2, -(useMinBound.y + useMaxBound.y) / 2,
                  -(useMinBound.z + useMaxBound.z) / 2));
        mat.add(matrix);
        mat.mul(amat, mat);
        matrix.setIdentity();
        matrix.setElement(0, 0, xft);
        matrix.setElement(1, 1, -xft);
        matrix.setElement(2, 2, xft);
        mat.mul(matrix, mat);
        mat.mul(tmat, mat);
        matrix.setZero();
        matrix.setTranslation(new Vector3d(drawWidth / 2, drawHeight / 2,
                drawWidth / 2));
        mat.add(matrix);
        matIsValid = true;
      }
      if (!painted) {

        // Only re-render image buffer if image has changed or have new image buffer.
        md.setMat(mat);
        settings.setAtomZOffset(drawWidth / 2);

        g.setColor(bg);
        g.fillRect(0, 0, drawWidth, drawHeight);
        g.setColor(fg);

        chemFrameRenderer.paint(g, md, settings);

        //md.paint(g, settings);
        if (rubberband) {
          g.setColor(fg);
          g.drawRect(rleft, rtop, rright - rleft, rbottom - rtop);
        }

        //            if (showPopupMenu){
        //               g.setColor(fg);
        //               g.drawRect(0,0,popButtonSideLength,popButtonSideLength);
        //               g.drawLine(0,0,popButtonSideLength,popButtonSideLength);
        //               g.drawLine(0,popButtonSideLength,popButtonSideLength,0);
        //            }
        painted = true;
      }

    }
  }

  public boolean isPainting() {
    return !painted;
  }

  public void componentHidden(java.awt.event.ComponentEvent e) {

    //     Invoked when component has been hidden. 
  }

  public void componentMoved(java.awt.event.ComponentEvent e) {

    //     Invoked when component has been moved. 
  }

  public void componentResized(java.awt.event.ComponentEvent e) {

    //    Invoked when component has been resized. 
    //      requestFocus();
    init();
  }

  public void componentShown(java.awt.event.ComponentEvent e) {

    //     Invoked when component has been shown. 
    //      requestFocus();
    init();
  }

  public void actionPerformed(java.awt.event.ActionEvent e) {

    String command = e.getActionCommand();
    if (command.equals("PREV")) {
      if (frameIndex > 0) {
        setFrame(frameIndex - 1);
      }
    } else if (command.equals("NEXT")) {
      if (frameIndex < (nframes - 1)) {
        setFrame(frameIndex + 1);
      }
    } else if (command.startsWith(customViewPrefix)) {
      doCustomViewString(command.substring(customViewPrefix.length()));
    }
  }

  public void toggleBonds() {
    showBonds(!getShowBonds());
  }

  public boolean getShowBonds() {
    return settings.getShowBonds();
  }

  public void showBonds(boolean doWe) {
    settings.setShowBonds(doWe);
    painted = false;
    repaint();
  }

  public void setWireframeRotation(boolean on) {
    wireFrameRotation = on;
  }

  public boolean getWireframeRotation() {
    return wireFrameRotation;
  }

}
