
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

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class DisplayPanel extends JPanel
    implements MeasurementListListener, PropertyChangeListener {

  public final static int X_AXIS = 1;
  public final static int Y_AXIS = 2;
  public final static int Z_AXIS = 3;

  private boolean haveFile = false;
  private boolean rubberbandSelectionMode = false;
  private int bx, by, rtop, rbottom, rleft, rright;
  private int nframes = 0;
  private static int prevx, prevy, outx, outy;

  private static Dimension dimCurrent = null;    // current pane dimension
  private static Object monitorRecalc = new Object();
  private static int numEvent = 0;               // event number
  private static Matrix4d transformEvent = null; // view transform of the event
  private static DisplaySettings settingsEvent = null; // settings
  private static Dimension dimEvent = null;      // dim at time of the event
  private static int numRecalc = 0;              // event number for recalc
  private static Matrix4d transformRecalc = null;// transform for recalc
  private static DisplaySettings settingsRecalc = null; // settings
  private static Dimension dimRecalc = null;     // for the recalc thread
  private static BufferedImage biRecalc = null;  // offscreen buffer image
  private static Thread threadRecalc = null;     // the recalc thread itself
  private static Object monitorRepaint = new Object();
  private static int numRepaint = 0;             // event num to be repainted
  private static Dimension dimRepaint = null;    // dim of repaint buffer
  private static BufferedImage biRepaint = null; // repaint buffer


  private static Matrix4d amat = new Matrix4d(); // mouse angular rotations.
  private static Matrix4d tmat = new Matrix4d(); // Matrix to do translations.
  private static Matrix4d zmat = new Matrix4d(); // Matrix to do zooming.

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
  private boolean wireFrameRotation = false;
  private boolean movingDrawMode = false;
  private Measure m = null;
  private MeasurementList mlist = null;
  protected DisplaySettings settings;

  public DisplayPanel(StatusBar status, DisplaySettings settings) {
    this.status = status;
    this.settings = settings;
    settings.addPropertyChangeListener(this);
    ShadingAtomRenderer.setImageComponent(this);
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
    AtomType.setJPanel(this);
    this.addMouseListener(new MyAdapter());
    this.addMouseMotionListener(new MyMotionAdapter());

    RepaintManager.currentManager(null).setDoubleBufferingEnabled(false);

    // it is important that these be initialized to identity matrices
    // before any rendering or transformations take place
    amat.setIdentity();
    tmat.setIdentity();
    zmat.setIdentity();

    threadRecalc = new Thread(new Recalc());
    threadRecalc.start();
  }

  public void setChemFile(ChemFile cf) {
    this.cf = cf;
    haveFile = true;
    nframes = cf.getNumberOfFrames();
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

    /*
    System.out.println("amat:");
    System.out.println(amat);
    System.out.println("tmat:");
    System.out.println(tmat);
    System.out.println("zmat:");
    System.out.println(zmat);
    */


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
    matrix.setTranslation(new Vector3d(dimCurrent.width / 2,
        dimCurrent.height / 2, dimCurrent.width / 2));
    viewMatrix.add(matrix);
    /*
    System.out.println(" xmin=" + xmin +
                       " xmax=" + xmax +
                       " ymin=" + ymin +
                       " ymax=" + ymax +
                       " zmin=" + zmin +
                       " zmax=" + zmax +
                       " xfac=" + xfac +
                       " width=" + width +
                       " height=" + height);
    System.out.println(viewMatrix);
    */
    return viewMatrix;
  }

  public void init() {

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
    float f1 = dimCurrent.width / xw;
    float f2 = dimCurrent.height / xw;
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

  public void setFrame(int fr) {

    if (haveFile) {
      if (fr < nframes) {
        setFrame(cf.getFrame(fr));
      }
      recalc();
    }
  }

  private void setFrame(ChemFrame frame) {

    md = frame;
    Measurement.setChemFrame(frame);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    recalc();
  }

  public ChemFrame getFrame() {
    return md;
  }

  //  public void stop() {
  //  }

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
        rubberbandSelectionMode = true;
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
        Atom atom = md.getNearestAtom(e.getX(), e.getY(),
            getViewTransformMatrix());
        if (atom != null) {
          if (mode == PICK) {
            if (e.isShiftDown()) {
              settings.addPickedAtom(atom);
            } else {
              settings.clearPickedAtoms();
              settings.addPickedAtom(atom);
            }
            recalc();
          } else if (mode == DELETE) {
            md.deleteAtom(atom.getAtomNumber());
            recalc();
            status.setStatus(2, "Atom deleted");
          } else if (mode == MEASURE) {
            m.firePicked(atom.getAtomNumber());
          }
        }
      }

    }

    public void mouseReleased(MouseEvent e) {

      if (mouseDragged && wireFrameRotation) {
        settings.setFastRendering(false);
        movingDrawMode = false;
        mouseDragged = false;
        recalc();
      }

      outx = e.getX();
      outy = e.getY();

      if (mode == PICK) {
        rubberbandSelectionMode = false;
        recalc();
      }

    }
  }

  class MyMotionAdapter extends MouseMotionAdapter {

    public void mouseDragged(MouseEvent e) {

      int x = e.getX();
      int y = e.getY();

      if (wireFrameRotation) {
        settings.setFastRendering(true);
        movingDrawMode = true;
        mouseDragged = true;
      }
      if (mode == ROTATE) {
        double xtheta = (y - prevy) * (2.0 * Math.PI / dimCurrent.width);
        double ytheta = (x - prevx) * (2.0 * Math.PI / dimCurrent.height);
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
        float xs = 1.0f + (float) (x - prevx) / (float) dimCurrent.width;
        float ys = 1.0f + (float) (prevy - y) / (float) dimCurrent.height;
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
          Atom[] selectedAtoms = md.findAtomsInRegion(rleft, rtop, rright,
              rbottom, getViewTransformMatrix());
          if (e.isShiftDown()) {
            settings.addPickedAtoms(selectedAtoms);
          } else {
            settings.clearPickedAtoms();
            settings.addPickedAtoms(selectedAtoms);
          }
        }
      }

      recalc();

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

  public void paint(Graphics g) {

    if (backgroundColor == null) {
      setBackgroundColor();
    }

    Color bg = backgroundColor;
    Color fg = getForeground();

    // there may be a better place for this ...
    // like when the window gets sized
    dimCurrent = getSize();

    if (md == null) {
      g.setColor(bg);
      g.fillRect(0, 0, dimCurrent.width, dimCurrent.height);
    } else {
      synchronized(monitorRepaint) {
        if (biRepaint != null) {
          int x = 0;
          int y = 0;
          if ((dimCurrent.width != dimRepaint.width) ||
              (dimCurrent.height != dimRepaint.height)) {
            // window has been resized, so center image
            g.setColor(bg);
            g.fillRect(0, 0, dimCurrent.width, dimCurrent.height);
            x = (dimCurrent.width - dimRepaint.width) / 2;
            y = (dimCurrent.height - dimRepaint.height) / 2;
          }
          g.drawImage(biRepaint, x, y, null);
        }
      }
      if (rubberbandSelectionMode) {
        g.setColor(fg);
        g.drawRect(rleft, rtop, rright - rleft, rbottom - rtop);
      }
    }
  }

  ChemFrameRenderer frameRenderer = new ChemFrameRenderer();
  MeasureRenderer measureRenderer = new MeasureRenderer();

  public Image takeSnapshot() {

    Image snapImage = createImage(this.dimCurrent.width,
                        this.dimCurrent.height);
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
  private aChargeColorAction acchargeAction = new aChargeColorAction();
  private aAtomTypeColorAction actypeAction = new aAtomTypeColorAction();
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
      recalc();
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
      recalc();
    }
  }

  class VectorsAction extends AbstractAction {

    public VectorsAction() {
      super("vectors");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.toggleVectors();
      recalc();
    }
  }

  class HydrogensAction extends AbstractAction {

    public HydrogensAction() {
      super("hydrogens");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.toggleHydrogens();
      recalc();
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (haveFile) {
        settings.addPickedAtoms(md.getAtoms());
        recalc();
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
        settings.clearPickedAtoms();
        recalc();
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
      recalc();
    }
  }

  class aShadingAction extends AbstractAction {

    public aShadingAction() {
      super("ashading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomDrawMode(DisplaySettings.SHADING);
      recalc();
    }
  }

  class aWireframeAction extends AbstractAction {

    public aWireframeAction() {
      super("awireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomDrawMode(DisplaySettings.WIREFRAME);
      recalc();
    }
  }

  class aChargeColorAction extends AbstractAction {

    public aChargeColorAction() {
      super("accharge");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomColorProfile(DisplaySettings.ATOMCHARGE);
      recalc();
    }
  }

  class aAtomTypeColorAction extends AbstractAction {

    public aAtomTypeColorAction() {
      super("actype");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomColorProfile(DisplaySettings.ATOMTYPE);
      recalc();
    }
  }

  class bQuickdrawAction extends AbstractAction {

    public bQuickdrawAction() {
      super("bquickdraw");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.QUICKDRAW);
      recalc();
    }
  }

  class bShadingAction extends AbstractAction {

    public bShadingAction() {
      super("bshading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.SHADING);
      recalc();
    }
  }

  class bLineAction extends AbstractAction {

    public bLineAction() {
      super("bline");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.LINE);
      recalc();
    }
  }

  class bWireframeAction extends AbstractAction {

    public bWireframeAction() {
      super("bwireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setBondDrawMode(DisplaySettings.WIREFRAME);
      recalc();
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
      recalc();
    }
  }

  class TopAction extends AbstractAction {

    public TopAction() {
      super("top");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotX(Math.toRadians(90.0));
      recalc();
    }
  }

  class BottomAction extends AbstractAction {

    public BottomAction() {
      super("bottom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotX(Math.toRadians(-90.0));
      recalc();
    }
  }

  class RightAction extends AbstractAction {

    public RightAction() {
      super("right");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotY(Math.toRadians(90.0));
      recalc();
    }
  }

  class LeftAction extends AbstractAction {

    public LeftAction() {
      super("left");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      amat.rotY(Math.toRadians(-90.0));
      recalc();
    }
  }

  class PlainAction extends AbstractAction {

    public PlainAction() {
      super("plain");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.NOLABELS);
      recalc();
    }
  }

  class SymbolsAction extends AbstractAction {

    public SymbolsAction() {
      super("symbols");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.SYMBOLS);
      recalc();
    }
  }

  class TypesAction extends AbstractAction {

    public TypesAction() {
      super("types");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.TYPES);
      recalc();
    }
  }

  class NumbersAction extends AbstractAction {

    public NumbersAction() {
      super("numbers");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setLabelMode(DisplaySettings.NUMBERS);
      recalc();
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
        float f1 = dimCurrent.width / xw;
        float f2 = dimCurrent.height / xw;
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
      recalc();
    }
  }

  class WireFrameRotationAction extends AbstractAction {

    public WireFrameRotationAction() {
      super("wireframerotation");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      wireFrameRotation = cbmi.isSelected();
    }
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      aquickdrawAction, ashadingAction, awireframeAction, bquickdrawAction,
      bshadingAction, blineAction, bwireframeAction, plainAction,
      symbolsAction, typesAction, numbersAction, bondsAction, atomsAction,
      vectorsAction, hydrogensAction, selectallAction, deselectallAction,
      homeAction, wireframerotationAction,
      acchargeAction, actypeAction
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


  public void propertyChange(PropertyChangeEvent event) {
    
    if (event.getPropertyName().equals(DisplaySettings.atomPickedProperty)) {
        status.setStatus(2, event.getNewValue() + " atoms selected");
    } else if (event.getPropertyName().equals(JmolModel.chemFileProperty)) {
      setChemFile((ChemFile) event.getNewValue());
    } else if (event.getPropertyName().equals(JmolModel.chemFrameProperty)) {
      setFrame((ChemFrame) event.getNewValue());
    }
  }

  public void recalc() {
    synchronized(monitorRecalc) {
      ++numEvent;
      transformEvent = getViewTransformMatrix();
      settingsEvent = settings.copy();
      dimEvent = dimCurrent;
      monitorRecalc.notify();
    }
  }

  class Recalc extends Thread {

    void swapRecalcAndRepaintBuffers() {
      synchronized(monitorRepaint) {
        Dimension dimT = dimRepaint;
        BufferedImage biT = biRepaint;
        dimRepaint = dimRecalc;
        biRepaint = biRecalc;
        numRepaint = numRecalc;
        dimRecalc = dimT;
        biRecalc = biT;
      }
    }

    void checkBufferedImageSize() {
      if ((dimEvent.width <= 0) || (dimEvent.height <= 0))
        return;
      if ((dimRecalc == null) ||
          (dimRecalc.width != dimEvent.width) ||
          (dimRecalc.height != dimEvent.height)) {
        dimRecalc = dimEvent;
        biRecalc = new BufferedImage(dimRecalc.width,
                                     dimRecalc.height,
                                     BufferedImage.TYPE_INT_ARGB);
      }
    }

    public void run() {
      numEvent = numRecalc = 0;
      while (true) {
        synchronized(monitorRecalc) {
          //System.out.println("numEvent=" + numEvent + " numRecalc=" + numRecalc);
          if (numEvent == numRecalc) {
            try {
              //System.out.println(" ... waiting");
              monitorRecalc.wait();
            } catch (InterruptedException e) {
              return;
            }
          }
          if (numEvent == numRecalc) {
            System.out.println("?Que?");
          }
          numRecalc = numEvent;
          transformRecalc = transformEvent;
          settingsRecalc = settingsEvent;
        }
        renderRepaintBuffer();
        swapRecalcAndRepaintBuffers();
        repaint();
      }
    }

    void renderRepaintBuffer() {
      checkBufferedImageSize();
      Graphics2D g2 = biRecalc.createGraphics();
      if (settingsRecalc.isAntiAliased() && !movingDrawMode) {
        String vers = System.getProperty("java.version");
        if (vers.compareTo("1.2") >= 0) {

          //comment out the next 5 lines if compiling under 1.1
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON);
          g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                              RenderingHints.VALUE_RENDER_QUALITY);
        }
      }
      else {
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_OFF);
          g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                              RenderingHints.VALUE_RENDER_SPEED);
      }

      g2.setColor(backgroundColor);
      g2.fillRect(0, 0, dimRecalc.width, dimRecalc.height);
      if ((md != null) &&
          (settingsRecalc != null) &&
          (transformRecalc != null)) {
        // mth - this is used to calculate screen atom diameters
        // I don't yet understand why it isn't part of the transform
        settingsRecalc.setAtomZOffset(dimRecalc.width / 2);

        frameRenderer.paint(g2, md, settingsRecalc, transformRecalc);
        measureRenderer.paint(g2, md, settingsRecalc);
      }
    }
  }
}

