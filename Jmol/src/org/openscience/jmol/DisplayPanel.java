
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
package org.openscience.jmol;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class DisplayPanel extends JPanel
        implements Runnable, MeasurementListListener {

  public static int X_AXIS = 1;
  public static int Y_AXIS = 2;
  public static int Z_AXIS = 3;

  private static boolean Perspective;
  private static float FieldOfView;
  private boolean painted = false;
  private boolean initialized = false;
  private boolean haveFile = false;
  private boolean rubberband = false;
  private boolean AntiAliased = false;
  private int bx, by, rtop, rbottom, rleft, rright;
  private int nframes = 0;
  private static int prevx, prevy, outx, outy;
  private static Matrix4d amat = new Matrix4d();    // Matrix to do mouse angular rotations.
  private static Matrix4d tmat = new Matrix4d();    // Matrix to do translations.
  private static Matrix4d zmat = new Matrix4d();    // Matrix to do zooming.
  float[] quat = new float[4];
  double[] mtmp;
  String[] names;
  Color[] colors;
  ChemFile cf;
  ChemFrame md;
  private float xfac, xmin, xmax, ymin, ymax, zmin, zmax;
  private float scalefudge = 1;
  public static final int ROTATE = 0;
  public static final int ZOOM = 1;
  public static final int XLATE = 2;
  public static final int PICK = 3;
  public static final int DEFORM = 4;
  public static final int MEASURE = 5;
  public static final int DELETE = 6;
  private int mode = ROTATE;
  private static Color backgroundColor = null;
  StatusBar status;

  //Added T.GREY for moveDraw support- should be true while mouse is dragged
  private boolean mouseDragged = false;
  private boolean WireFrameRotation = false;
  private boolean movingDrawMode = false;
  private Measure m = null;
  private MeasurementList mlist = null;
  protected DisplaySettings settings;
  
  public DisplayPanel(StatusBar status, DisplaySettings settings) {
    this.status = status;
    this.settings = settings;
    AtomRenderer.setImageComponent(this);
  }

  public boolean getAntiAliased() {
    return AntiAliased;
  }

  public void setAntiAliased(boolean aa) {
    AntiAliased = aa;
  }

  public int getMode() {
    return mode;
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public void setMeasure(Measure m) {
    this.m = m;
  }

  public void start() {
    new Thread(this).start();
    AtomType.setJPanel(this);
    this.addMouseListener(new MyAdapter());
    this.addMouseMotionListener(new MyMotionAdapter());
  }

  public void setChemFile(ChemFile cf) {

    this.cf = cf;
    haveFile = true;
    nframes = cf.getNumberFrames();
    this.md = cf.getFrame(0);
    Measurement.setChemFrame(md);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    init();
  }

  /**
   * returns the rotation matrix.  This is added for camera and
   * light source rotation in PovRay.
   */
  public Matrix4d getAmat() {
    return amat;
  }

  /**
   * returns the translation matrix.  This is added for camera and
   * light source rotation in PovRay.
   */
  public Matrix4d getTmat() {
    return tmat;
  }

  /**
   * returns the zoom matrix.  This is added for camera and
   * light source rotation in PovRay.
   */
  public Matrix4d getZmat() {
    return zmat;
  }

  public float getXfac() {
    return xfac;
  }

  /**
   *  Returns transform matrix assosiated with the current viewing transform.
   */
  public Matrix4d getViewTransformMatrix() {

    Matrix4d viewMatrix = new Matrix4d();
    viewMatrix.setIdentity();
    Matrix4d matrix = new Matrix4d();
    matrix.setTranslation(new Vector3d(-(xmin + xmax) / 2,
            -(ymin + ymax) / 2, -(zmin + zmax) / 2));
    viewMatrix.add(matrix);
    viewMatrix.mul(amat, viewMatrix);
    matrix.setIdentity();
    matrix.setElement(0, 0, xfac);
    matrix.setElement(1, 1, -xfac);
    matrix.setElement(2, 2, xfac);
    viewMatrix.mul(matrix, viewMatrix);
    viewMatrix.mul(tmat, viewMatrix);
    viewMatrix.mul(zmat, viewMatrix);
    matrix.setZero();
    matrix.setTranslation(new Vector3d(getSize().width / 2,
            getSize().height / 2, getSize().width / 2));
    viewMatrix.add(matrix);
    return viewMatrix;
  }

  public void init() {

    md.findBB();
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    xmin = md.getXMin();
    xmax = md.getXMax();
    ymin = md.getYMin();
    ymax = md.getYMax();
    zmin = md.getZMin();
    zmax = md.getZMax();
    float xw = xmax - xmin;
    float yw = ymax - ymin;
    float zw = zmax - zmin;
    if (yw > xw) {
      xw = yw;
    }
    if (zw > xw) {
      xw = zw;
    }
    float f1 = getSize().width / xw;
    float f2 = getSize().height / xw;
    if (f1 < f2) {
      xfac = f1;
    } else {
      xfac = f2;
    }
    xfac *= 0.7f * scalefudge;
    settings.setAtomScreenScale(xfac);
    settings.setBondScreenScale(xfac);
    settings.setVectorScreenScale(xfac);
    repaint();
  }

  public void setFrame(int fr) {

    if (haveFile) {
      if (fr < nframes) {
        md = cf.getFrame(fr);
        Measurement.setChemFrame(md);
        if (mlist != null) {
          mlistChanged(new MeasurementListEvent(mlist));
        }
      }
      repaint();
    }
  }

  public ChemFrame getFrame() {
    return md;
  }

  public void run() {

    try {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    } catch (Exception e) {
      e.printStackTrace();
    }
    repaint();
  }

  public void stop() {
  }

  public void mlistChanged(MeasurementListEvent mle) {
    MeasurementList source = (MeasurementList) mle.getSource();
    mlist = source;
    md.updateMlists(mlist.getDistanceList(), mlist.getAngleList(),
            mlist.getDihedralList());
  }

  class MyAdapter extends MouseAdapter {

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
        if (mode == PICK) {
          if (e.isShiftDown()) {
            md.shiftSelectAtom(e.getX(), e.getY());
          } else {
            md.selectAtom(e.getX(), e.getY());
          }
          repaint();
          int n = md.getNpicked();
          status.setStatus(2, n + " Atoms Selected");
        } else if (mode == DELETE) {
          md.deleteSelectedAtom(e.getX(), e.getY());
          repaint(); // this seems to have no effect...
          status.setStatus(2, "Atom Deleted");
        } else if (mode == MEASURE) {
          m.firePicked(md.pickMeasuredAtom(e.getX(), e.getY()));
        }
      }

    }

    public void mouseReleased(MouseEvent e) {

      if (mouseDragged && WireFrameRotation) {
        settings.setFastRendering(false);
        movingDrawMode = false;
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

      if (WireFrameRotation) {
        settings.setFastRendering(true);
        movingDrawMode = true;
        mouseDragged = true;
      }
      if (mode == ROTATE) {

        /*
        float[] spin_quat = new float[4];
        Trackball tb = new Trackball(spin_quat,
                                (2.0f*x - getSize().width) / getSize().width,
                                (getSize().height-2.0f*y) / getSize().height,
                                (2.0f*prevx - getSize().width) / getSize().width,
                                (getSize().height-2.0f*prevy) / getSize().height);

        tb.add_quats(spin_quat, quat, quat);
        tb.build_rotmatrix(amat, quat);

        */
        double xtheta = (y - prevy) * (2.0 * Math.PI / getSize().width);
        double ytheta = (x - prevx) * (2.0 * Math.PI / getSize().height);
        Matrix4d matrix = new Matrix4d();
        matrix.rotX(xtheta);
        amat.mul(matrix, amat);
        matrix.rotY(ytheta);
        amat.mul(matrix, amat);
      }

      if (mode == XLATE) {
        float dx = (x - prevx);
        float dy = (y - prevy);
        Matrix4d matrix = new Matrix4d();
        matrix.setTranslation(new Vector3d(dx, dy, 0.0));
        tmat.add(matrix);
      }

      if (mode == ZOOM) {
        float xs = 1.0f + (float) (x - prevx) / (float) getSize().width;
        float ys = 1.0f + (float) (prevy - y) / (float) getSize().height;
        float s = (xs + ys) / 2.0f;
        Matrix4d matrix = new Matrix4d();
        matrix.setElement(0, 0, s);
        matrix.setElement(1, 1, s);
        matrix.setElement(2, 2, s);
        matrix.setElement(3, 3, 1.0);
        zmat.mul(matrix, zmat);
        xfac *= s * s;
        settings.setAtomScreenScale(xfac);
        settings.setBondScreenScale(xfac);
        settings.setVectorScreenScale(xfac);
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
          repaint();
          int n = md.getNpicked();
          status.setStatus(2, n + " Atoms Selected");
        }
      }

      if (painted) {
        painted = false;
        repaint();
      }
      prevx = x;
      prevy = y;
    }
  }

  public void rebond() throws Exception {
    if (md != null) {
      md.rebond();
    }
  }

  static void setBackgroundColor() {
    backgroundColor = Color.getColor("backgroundColor");
  }

  static void setBackgroundColor(Color bg) {
    backgroundColor = bg;
  }

  static Color getBackgroundColor() {
    return backgroundColor;
  }

  static void setPerspective(boolean p) {
    Perspective = p;
  }

  static boolean getPerspective() {
    return Perspective;
  }

  static void setFieldOfView(float fov) {
    FieldOfView = fov;
  }

  static float getFieldOfView() {
    return FieldOfView;
  }

  public void paint(Graphics g) {

    if (AntiAliased && !movingDrawMode) {
      String vers = System.getProperty("java.version");
      if (vers.compareTo("1.2") >= 0) {

        //comment out the next 5 lines if compiling under 1.1
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
      }
    }

    if (backgroundColor == null) {
      setBackgroundColor();
    }

    Color bg = backgroundColor;
    Color fg = getForeground();

    if (md == null) {
      g.setColor(bg);
      g.fillRect(0, 0, getSize().width, getSize().height);
    } else {
      if (!initialized) {

        /*
          start with the unit matrix
        */
        amat.setIdentity();
        tmat.setIdentity();
        zmat.setIdentity();
        quat[0] = 0.0f;
        quat[1] = 0.0f;
        quat[2] = 0.0f;
        quat[3] = 1.0f;
        initialized = true;
      }
      ChemFrame.matunit();
      ChemFrame.matmult(getViewTransformMatrix());
      settings.setAtomZOffset(getSize().width / 2);

      g.setColor(bg);
      g.fillRect(0, 0, getSize().width, getSize().height);
      g.setColor(fg);

      frameRenderer.paint(g, md, settings);
      measureRenderer.paint(g, md, settings);
      if (rubberband) {
        g.setColor(fg);
        g.drawRect(rleft, rtop, rright - rleft, rbottom - rtop);
      }
      painted = true;

    }
  }

  ChemFrameRenderer frameRenderer = new ChemFrameRenderer();
  MeasureRenderer measureRenderer = new MeasureRenderer();
  
  public boolean isPainting() {
    return !painted;
  }

  public Image takeSnapshot() {

    Image snapImage = createImage(this.getSize().width,
                        this.getSize().height);
    Graphics snapGraphics = snapImage.getGraphics();
    paint(snapGraphics);
    return snapImage;
  }

  // The actions:

  private DeleteAction deleteAction = new DeleteAction();
  private PickAction pickAction = new PickAction();
  private RotateAction rotateAction = new RotateAction();
  private ZoomAction zoomAction = new ZoomAction();
  private XlateAction xlateAction = new XlateAction();
  private HomeAction homeAction = new HomeAction();
  private FrontAction frontAction = new FrontAction();
  private TopAction topAction = new TopAction();
  private BottomAction bottomAction = new BottomAction();
  private RightAction rightAction = new RightAction();
  private LeftAction leftAction = new LeftAction();
  private aQuickdrawAction aquickdrawAction = new aQuickdrawAction();
  private aShadingAction ashadingAction = new aShadingAction();
  private aWireframeAction awireframeAction = new aWireframeAction();
  private bQuickdrawAction bquickdrawAction = new bQuickdrawAction();
  private bShadingAction bshadingAction = new bShadingAction();
  private bLineAction blineAction = new bLineAction();
  private bWireframeAction bwireframeAction = new bWireframeAction();
  private PlainAction plainAction = new PlainAction();
  private SymbolsAction symbolsAction = new SymbolsAction();
  private TypesAction typesAction = new TypesAction();
  private NumbersAction numbersAction = new NumbersAction();
  private BondsAction bondsAction = new BondsAction();
  private AtomsAction atomsAction = new AtomsAction();
  private VectorsAction vectorsAction = new VectorsAction();
  private HydrogensAction hydrogensAction = new HydrogensAction();
  private SelectallAction selectallAction = new SelectallAction();
  private DeselectallAction deselectallAction = new DeselectallAction();
  private WireFrameRotationAction wireframerotationAction =
    new WireFrameRotationAction();

  class BondsAction extends AbstractAction {

    public BondsAction() {
      super("bonds");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.toggleBonds();
      repaint();
    }
  }

  class AtomsAction extends AbstractAction {

    public AtomsAction() {
      super("atoms");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.toggleAtoms();
      settings.toggleDrawBondsToAtomCenters();
      repaint();
    }
  }

  class VectorsAction extends AbstractAction {

    public VectorsAction() {
      super("vectors");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.toggleVectors();
      repaint();
    }
  }

  class HydrogensAction extends AbstractAction {

    public HydrogensAction() {
      super("hydrogens");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.toggleHydrogens();
      repaint();
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (haveFile) {
        md.selectAll();
        int n = md.getNpicked();
        status.setStatus(2, n + " Atoms Selected");
        repaint();
      }
    }
  }

  class DeselectallAction extends AbstractAction {

    public DeselectallAction() {
      super("deselectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (haveFile) {
        md.deselectAll();
        int n = md.getNpicked();
        status.setStatus(2, n + " Atoms Selected");
        repaint();
      }
    }
  }

  class aQuickdrawAction extends AbstractAction {

    public aQuickdrawAction() {
      super("aquickdraw");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomDrawMode(DisplaySettings.QUICKDRAW);
      repaint();
    }
  }

  class aShadingAction extends AbstractAction {

    public aShadingAction() {
      super("ashading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomDrawMode(DisplaySettings.SHADING);
      repaint();
    }
  }

  class aWireframeAction extends AbstractAction {

    public aWireframeAction() {
      super("awireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomDrawMode(DisplaySettings.WIREFRAME);
      repaint();
    }
  }

  class bQuickdrawAction extends AbstractAction {

    public bQuickdrawAction() {
      super("bquickdraw");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.QUICKDRAW);
      repaint();
    }
  }

  class bShadingAction extends AbstractAction {

    public bShadingAction() {
      super("bshading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.SHADING);
      repaint();
    }
  }

  class bLineAction extends AbstractAction {

    public bLineAction() {
      super("bline");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.LINE);
      repaint();
    }
  }

  class bWireframeAction extends AbstractAction {

    public bWireframeAction() {
      super("bwireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.WIREFRAME);
      repaint();
    }
  }

  class PickAction extends AbstractAction {

    public PickAction() {
      super("pick");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      // switch mode;
      mode = PICK;
      status.setStatus(1, "Select Atoms");
    }
  }

  class DeleteAction extends AbstractAction {

    public DeleteAction() {
      super("delete");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      // switch mode;
      mode = DELETE;
      status.setStatus(1, "Delete Atoms");
    }
  }

  class RotateAction extends AbstractAction {

    public RotateAction() {
      super("rotate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      // switch mode;
      mode = ROTATE;
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class ZoomAction extends AbstractAction {

    public ZoomAction() {
      super("zoom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      // switch mode;
      mode = ZOOM;
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class XlateAction extends AbstractAction {

    public XlateAction() {
      super("xlate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      // switch mode;
      mode = XLATE;
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class FrontAction extends AbstractAction {

    public FrontAction() {
      super("front");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.setIdentity();
      repaint();
    }
  }

  class TopAction extends AbstractAction {

    public TopAction() {
      super("top");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotX(Math.toRadians(90.0));
      repaint();
    }
  }

  class BottomAction extends AbstractAction {

    public BottomAction() {
      super("bottom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotX(Math.toRadians(-90.0));
      repaint();
    }
  }

  class RightAction extends AbstractAction {

    public RightAction() {
      super("right");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotY(Math.toRadians(90.0));
      repaint();
    }
  }

  class LeftAction extends AbstractAction {

    public LeftAction() {
      super("left");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotY(Math.toRadians(-90.0));
      repaint();
    }
  }

  class PlainAction extends AbstractAction {

    public PlainAction() {
      super("plain");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.NOLABELS);
      repaint();
    }
  }

  class SymbolsAction extends AbstractAction {

    public SymbolsAction() {
      super("symbols");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.SYMBOLS);
      repaint();
    }
  }

  class TypesAction extends AbstractAction {

    public TypesAction() {
      super("types");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.TYPES);
      repaint();
    }
  }

  class NumbersAction extends AbstractAction {

    public NumbersAction() {
      super("numbers");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.NUMBERS);
      repaint();
    }
  }

  class HomeAction extends AbstractAction {

    public HomeAction() {
      super("home");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      amat.setIdentity();
      tmat.setIdentity();
      zmat.setIdentity();
      if (md != null) {
        md.findBB();
        xmin = md.getXMin();
        xmax = md.getXMax();
        ymin = md.getYMin();
        ymax = md.getYMax();
        zmin = md.getZMin();
        zmax = md.getZMax();
        float xw = xmax - xmin;
        float yw = ymax - ymin;
        float zw = zmax - zmin;
        if (yw > xw) {
          xw = yw;
        }
        if (zw > xw) {
          xw = zw;
        }
        float f1 = getSize().width / xw;
        float f2 = getSize().height / xw;
        if (f1 < f2) {
          xfac = f1;
        } else {
          xfac = f2;
        }
        xfac *= 0.7f * scalefudge;
        settings.setAtomScreenScale(xfac);
        settings.setBondScreenScale(xfac);
        settings.setVectorScreenScale(xfac);
      }
      repaint();
    }
  }

  class WireFrameRotationAction extends AbstractAction {

    public WireFrameRotationAction() {
      super("wireframerotation");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      WireFrameRotation = cbmi.isSelected();
    }
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction, frontAction,
      topAction, bottomAction, rightAction, leftAction, aquickdrawAction,
      ashadingAction, awireframeAction, bquickdrawAction, bshadingAction,
      blineAction, bwireframeAction, plainAction, symbolsAction, typesAction,
      numbersAction, bondsAction, atomsAction, vectorsAction, hydrogensAction,
      selectallAction, deselectallAction,
      homeAction, wireframerotationAction
    };
    return defaultActions;
  }

  DisplaySettings getSettings() {
    return settings;
  }

  /** Added for compatability with RasmolScript
   */
  public void rotate(int axis, float angle) {

    if (axis == X_AXIS) {
      amat.rotX(Math.toRadians(angle));
    } else if (axis == Y_AXIS) {
      amat.rotY(Math.toRadians(angle));
    } else if (axis == Z_AXIS) {
      amat.rotZ(Math.toRadians(angle));
    }
  }
}

