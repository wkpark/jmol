
/*
 * Copyright 2002 The Jmol Development Team
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

import org.openscience.jmol.render.*;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.RepaintManager;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;
import javax.vecmath.AxisAngle4f;

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

  // for now, default to true
  private boolean showPaintTime = true;
  private static float scalePixelsPerAngstrom;
  private static float scaleDefaultPixelsPerAngstrom;
  private static final Matrix4f matrixRotate = new Matrix4f();
  private static final Matrix4f matrixTranslate = new Matrix4f();
  private static final Matrix4f matrixViewTransform = new Matrix4f();
  private static final Matrix4f matrixTemp = new Matrix4f();
  private static final Vector3f vectorTemp = new Vector3f();

  // current dimensions of the display screen
  private static Dimension dimCurrent = null;
  private static int minScreenDimension;
  // previous dimensions ... used to detect resize operations
  private static Dimension dimPrevious = null;
  ChemFile cf;
  public ChemFrame chemframe;
  public static final int ROTATE = 0;
  public static final int ZOOM = 1;
  public static final int XLATE = 2;
  public static final int PICK = 3;
  public static final int DEFORM = 4;
  public static final int MEASURE = 5;
  public static final int DELETE = 6;
  private int modeMouse = ROTATE;
  private static Color backgroundColor = null;
  private StatusBar status;

  private boolean antialiasCapable = false;
  //Added T.GREY for moveDraw support- should be true while mouse is dragged
  private boolean mouseDragged = false;
  private boolean wireFrameRotation = false;
  private Measure measure = null;
  private MeasurementList mlist = null;
  public DisplaySettings settings;

  public DisplayPanel(StatusBar status, DisplaySettings settings) {
    this.status = status;
    this.settings = settings;
    settings.addPropertyChangeListener(this);
    AtomShape.setImageComponent(this);
    if (System.getProperty("painttime", "false").equals("true"))
      showPaintTime = true;
  }

  public int getMode() {
    return modeMouse;
  }

  public void setMode(int modeMouse) {
    this.modeMouse = modeMouse;
  }

  public void setMeasure(Measure measure) {
    this.measure = measure;
  }

  public void start() {
    this.addMouseListener(new MyAdapter());
    this.addMouseMotionListener(new MyMotionAdapter());
    RepaintManager.currentManager(null).setDoubleBufferingEnabled(false);
    String vers = System.getProperty("java.version");
    antialiasCapable = vers.compareTo("1.2") >= 0;
  }

  public void setChemFile(ChemFile cf) {
    this.cf = cf;
    haveFile = true;
    nframes = cf.getNumberOfFrames();
    this.chemframe = cf.getFrame(0);
    Measurement.setChemFrame(chemframe);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    homePosition();
  }

  public float getScalePixelsPerAngstrom() {
    return scalePixelsPerAngstrom;
  }

  public float getPovScale() {
    return scalePixelsPerAngstrom / scaleDefaultPixelsPerAngstrom;
  }

  public Matrix4f getPovRotateMatrix() {
    return new Matrix4f(matrixRotate);
  }

  public Matrix4f getPovTranslateMatrix() {
    Matrix4f matrixPovTranslate = new Matrix4f(matrixTranslate);
    matrixPovTranslate.get(vectorTemp);
    vectorTemp.x /= scalePixelsPerAngstrom;
    vectorTemp.y /= -scalePixelsPerAngstrom; // need to invert y axis
    vectorTemp.z /= scalePixelsPerAngstrom;
    matrixPovTranslate.set(vectorTemp);
    return matrixPovTranslate;
  }

  /**
   *  Returns transform matrix assosiated with the current viewing transform.
   */
  public Matrix4f getViewTransformMatrix() {
    // you absolutely *must* watch the order of these operations
    matrixViewTransform.setIdentity();
    // first, translate the coordinates back to the center
    matrixTemp.setZero();
    vectorTemp.set(chemframe.getRotationCenter());
    matrixTemp.setTranslation(vectorTemp);
    matrixViewTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixViewTransform.mul(matrixRotate);
    matrixViewTransform.mul(matrixRotate, matrixViewTransform);
    // now scale to screen coordinates
    matrixTemp.set(scalePixelsPerAngstrom);
    matrixTemp.m11=-scalePixelsPerAngstrom; // invert y dimension
    matrixViewTransform.mul(matrixTemp, matrixViewTransform);
    // translate
    matrixViewTransform.mul(matrixTranslate, matrixViewTransform);
    // now translate to the center of the screen
    matrixTemp.setZero();
    // This z dimension is here because of the approximations used
    // to calculate atom sizes
    vectorTemp.x =
      vectorTemp.z = dimCurrent.width / 2;
    vectorTemp.y = dimCurrent.height / 2;
    matrixTemp.setTranslation(vectorTemp);
    matrixViewTransform.add(matrixTemp);
    return matrixViewTransform;
  }

  public void homePosition() {
    matrixRotate.setIdentity();
    matrixTranslate.setIdentity();
    scaleFitToScreen();
    setRotateMode();
  }

  public void setRotateMode() {
      Jmol.setRotateButton();
      modeMouse = ROTATE;
  }
    

  public void scaleFitToScreen() {
    minScreenDimension = dimCurrent.width;
    if (dimCurrent.height < minScreenDimension)
      minScreenDimension = dimCurrent.height;
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (minScreenDimension > 2)
      minScreenDimension -= 2;
    scalePixelsPerAngstrom =
      minScreenDimension / 2 / chemframe.getRotationRadius();
    scaleDefaultPixelsPerAngstrom = scalePixelsPerAngstrom;

    settings.setAtomScreenScale(scalePixelsPerAngstrom);
    settings.setBondScreenScale(scalePixelsPerAngstrom);
    settings.setVectorScreenScale(scalePixelsPerAngstrom);
  }

  public void setFrame(int fr) {

    if (haveFile) {
      if (fr < nframes) {
        setFrame(cf.getFrame(fr));
      }
      repaint();
    }
  }

  private void setFrame(ChemFrame frame) {
    chemframe = frame;
    Measurement.setChemFrame(frame);
    if (mlist != null) {
      mlistChanged(new MeasurementListEvent(mlist));
    }
    repaint();
  }

  public ChemFrame getFrame() {
    return chemframe;
  }

  public void mlistChanged(MeasurementListEvent mle) {
    MeasurementList source = (MeasurementList) mle.getSource();
    mlist = source;
    chemframe.updateMlists(mlist.getDistanceList(),
                           mlist.getAngleList(),
                           mlist.getDihedralList());
  }

  class MyAdapter extends MouseAdapter {

    public void mousePressed(MouseEvent e) {

      prevx = e.getX();
      prevy = e.getY();

      if (modeMouse == PICK) {
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
        Atom atom = chemframe.getNearestAtom(e.getX(), e.getY());
        switch (modeMouse) {
        case PICK:
          if (!e.isShiftDown())
            settings.clearPickedAtoms();
          if (atom != null)
            settings.addPickedAtom(atom);
          repaint();
          break;
        case DELETE:
          if (atom != null) {
            chemframe.deleteAtom(atom);
            status.setStatus(2, "Atom deleted");
          }
          repaint();
          break;
        case MEASURE:
          if (atom != null)
            measure.firePicked(atom.getAtomNumber());
        }
      }
    }

    public void mouseReleased(MouseEvent e) {

      if (mouseDragged) {
        settings.setFastRendering(false);
        mouseDragged = false;
        repaint();
      }
      if (modeMouse == PICK) {
        rubberbandSelectionMode = false;
        repaint();
      }

    }
  }

  class MyMotionAdapter extends MouseMotionAdapter {

    public void mouseDragged(MouseEvent e) {

      int x = e.getX();
      int y = e.getY();

      mouseDragged = true;
      if (wireFrameRotation) {
        settings.setFastRendering(true);
      }
      if (modeMouse == ROTATE) {
        // what fraction of PI radians do you want to rotate?
        // the full screen width corresponds to a PI (180 degree) rotation
        // if you grab an atom near the outside edge of the molecule,
        // you can essentially "pull it" across the screen and it will
        // track with the mouse cursor

        // a change in the x coordinate generates a rotation about the y axis
        float ytheta = (float)Math.PI * (x - prevx) / minScreenDimension;
        matrixTemp.rotY(ytheta);
        matrixRotate.mul(matrixTemp, matrixRotate);
        // and, of course, delta y controls rotation about the x axis
        float xtheta = (float)Math.PI * (y - prevy) / minScreenDimension;
        matrixTemp.rotX(xtheta);
        matrixRotate.mul(matrixTemp, matrixRotate);
      }

      if (modeMouse == XLATE) {
        matrixTranslate.m03 += (x - prevx);
        matrixTranslate.m13 += (y - prevy);
      }

      if (modeMouse == ZOOM) {
        float xs = 1.0f + (float) (x - prevx) / dimCurrent.width;
        float ys = 1.0f + (float) (prevy - y) / dimCurrent.height;
        float s = (xs + ys) / 2;
        scalePixelsPerAngstrom *= s;
        settings.setAtomScreenScale(scalePixelsPerAngstrom);
        settings.setBondScreenScale(scalePixelsPerAngstrom);
        settings.setVectorScreenScale(scalePixelsPerAngstrom);
      }

      if (modeMouse == PICK) {
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
          Atom[] selectedAtoms =
            chemframe.findAtomsInRegion(rleft, rtop, rright, rbottom);
          if (e.isShiftDown()) {
            settings.addPickedAtoms(selectedAtoms);
          } else {
            settings.clearPickedAtoms();
            settings.addPickedAtoms(selectedAtoms);
          }
        }
      }

      prevx = x;
      prevy = y;
      repaint();
    }
  }

  public void rebond() throws Exception {
    if (chemframe != null) {
      chemframe.rebond();
    }
  }

  public static void setBackgroundColor() {
    backgroundColor = Color.getColor("backgroundColor");
  }

  public static void setBackgroundColor(Color bg) {
    backgroundColor = bg;
  }

  public static Color getBackgroundColor() {
    return backgroundColor;
  }

  public void paint(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    if (showPaintTime)
      startPaintClock();

    if (backgroundColor == null) {
      setBackgroundColor();
    }

    Color bg = backgroundColor;
    Color fg = getForeground();

    dimCurrent = getSize();

    g2d.setColor(bg);
    g2d.fillRect(0, 0, dimCurrent.width, dimCurrent.height);
    if (chemframe != null) {
      if (! dimCurrent.equals(dimPrevious))
        scaleFitToScreen();
      if (antialiasCapable && settings.isAntiAliased() && !mouseDragged) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
      }
      g2d.setColor(bg);
      g2d.fillRect(0, 0, dimCurrent.width, dimCurrent.height);

      Matrix4f matrix = getViewTransformMatrix();
      settings.setAtomZOffset(dimCurrent.width / 2);

      frameRenderer.paint(g2d, chemframe, settings, matrix);
      measureRenderer.paint(g2d, chemframe, settings);
      if (rubberbandSelectionMode) {
        g2d.setColor(fg);
        g2d.drawRect(rleft, rtop, rright - rleft, rbottom - rtop);
      }
      if (showPaintTime)
        stopPaintClock();
    }
    dimPrevious = dimCurrent;
  }

  ChemFrameRenderer frameRenderer = new ChemFrameRenderer();
  MeasureRenderer measureRenderer = new MeasureRenderer();

  public Image takeSnapshot() {

    Image snapImage = createImage(dimCurrent.width, dimCurrent.height);
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
  private DefineCenterAction defineCenterAction = new DefineCenterAction();
  private Test1Action test1Action = new Test1Action();
  private Test2Action test2Action = new Test2Action();
  private Test3Action test3Action = new Test3Action();
  private Test4Action test4Action = new Test4Action();
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
        settings.addPickedAtoms(chemframe.getAtoms());
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
        settings.clearPickedAtoms();
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

  class aChargeColorAction extends AbstractAction {

    public aChargeColorAction() {
      super("accharge");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomColorProfile(DisplaySettings.ATOMCHARGE);
      repaint();
    }
  }

  class aAtomTypeColorAction extends AbstractAction {

    public aAtomTypeColorAction() {
      super("actype");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      settings.setAtomColorProfile(DisplaySettings.ATOMTYPE);
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
      if (measure.isShowing()) {
        modeMouse = MEASURE;
      } else {
        modeMouse = PICK;
      }
      status.setStatus(1, "Select Atoms");
    }
  }

  class DeleteAction extends AbstractAction {

    public DeleteAction() {
      super("delete");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      modeMouse = DELETE;
      status.setStatus(1, "Delete Atoms");
    }
  }

  class RotateAction extends AbstractAction {

    public RotateAction() {
      super("rotate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      modeMouse = ROTATE;
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class ZoomAction extends AbstractAction {

    public ZoomAction() {
      super("zoom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      modeMouse = ZOOM;
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class XlateAction extends AbstractAction {

    public XlateAction() {
      super("xlate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      modeMouse = XLATE;
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class FrontAction extends AbstractAction {

    public FrontAction() {
      super("front");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      matrixRotate.setIdentity();
      repaint();
    }
  }

  class TopAction extends AbstractAction {

    public TopAction() {
      super("top");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      matrixRotate.rotX((float)Math.toRadians(90.0));
      repaint();
    }
  }

  class BottomAction extends AbstractAction {

    public BottomAction() {
      super("bottom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      matrixRotate.rotX((float)Math.toRadians(-90.0));
      repaint();
    }
  }

  class RightAction extends AbstractAction {

    public RightAction() {
      super("right");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      matrixRotate.rotY((float)Math.toRadians(90.0));
      repaint();
    }
  }

  class LeftAction extends AbstractAction {

    public LeftAction() {
      super("left");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      matrixRotate.rotY((float)Math.toRadians(-90.0));
      repaint();
    }
  }

  class DefineCenterAction extends AbstractAction {

    public DefineCenterAction() {
      super("definecenter");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      int[] picked = settings.getPickedAtoms().elements();
      Point3f center = null;
      if (picked.length > 0) {
        // just take the average of all the points
        center = new Point3f(); // defaults to 0,0,0
        for (int i = 0; i < picked.length; ++i)
          center.add(chemframe.getAtomAt(picked[i]).getPosition());
        center.scale(1.0f / picked.length); // just divide by the quantity
      }
      chemframe.setRotationCenter(center);
      settings.clearPickedAtoms();
      scaleFitToScreen();
      setRotateMode();
      repaint();
    }
  }

  class Test1Action extends AbstractAction {

    public Test1Action() {
      super("test1");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      rotateX(45);
      repaint();
    }
  }

  class Test2Action extends AbstractAction {

    public Test2Action() {
      super("test2");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      rotateY(45);
      repaint();
    }
  }

  class Test3Action extends AbstractAction {

    public Test3Action() {
      super("test3");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      rotateZ(45);
      repaint();
    }
  }

  class Test4Action extends AbstractAction {

    public Test4Action() {
      super("test4");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      rotate(new AxisAngle4f(1, 1, 1, (float)Math.PI/4));
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
      homePosition();
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
      wireFrameRotation = cbmi.isSelected();
    }
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      defineCenterAction, test1Action, test2Action, test3Action, test4Action,
      aquickdrawAction, ashadingAction, awireframeAction, bquickdrawAction,
      bshadingAction, blineAction, bwireframeAction, plainAction,
      symbolsAction, typesAction, numbersAction, bondsAction, atomsAction,
      vectorsAction, hydrogensAction, selectallAction, deselectallAction,
      homeAction, wireframerotationAction,
      acchargeAction, actypeAction
    };
    return defaultActions;
  }

  public DisplaySettings getSettings() {
    return settings;
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

  // code to record last and average times
  // last and average times are shown in the status window

  final static int maxTimes = 50; // how many samples in the average
  private static int cTimes = 0;
  private static int longestTime = 0;
  private static int lastTime = 0;
  private static int iTimes = 0;
  private static int totalTime = 0;
  private static int[] aTimes = new int[maxTimes];

  private void resetTimes() {
    cTimes = iTimes = totalTime = longestTime = lastTime = 0;
  }

  private void recordTime(int time) {
    if (time == lastTime)
      return;
    if (cTimes < maxTimes) {
      totalTime += time;
      aTimes[cTimes++] = time;
    } else {
      totalTime -= aTimes[iTimes];
      totalTime += time;
      aTimes[iTimes] = time;
      if (++iTimes == maxTimes)
        iTimes = 0;
    }
    lastTime = time;
    if (time > longestTime)
      longestTime = time;
  }

  private long timeBegin;

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
  }

  private void stopPaintClock() {
    int time = (int)(System.currentTimeMillis() - timeBegin);
    recordTime(time);
    showTimes();
  }

  private String fmt(int num) {
    if (num < 10)
      return "  " + num;
    if (num < 100)
      return " " + num;
    return "" + num;
  }

  private void showTimes() {
    int timeAverage = totalTime / cTimes;
    status.setStatus(3, fmt(lastTime) + "ms : " + fmt(timeAverage) + "ms");
  }

  public void rotate(int axis, float angle) {
    if (axis == X_AXIS) {
      matrixRotate.rotX((float)Math.toRadians(angle));
    } else if (axis == Y_AXIS) {
      matrixRotate.rotY((float)Math.toRadians(angle));
    } else if (axis == Z_AXIS) {
      matrixRotate.rotZ((float)Math.toRadians(angle));
    }
  }
  public void rotateX(float angleRadians) {
    matrixTemp.rotX(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    repaint();
  }
  public void rotateY(float angleRadians) {
    matrixTemp.rotY(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    repaint();
  }
  public void rotateZ(float angleRadians) {
    matrixTemp.rotZ(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
    repaint();
  }
  public void rotateX(int angleDegrees) {
    rotateX((float)Math.toRadians(angleDegrees));
  }
  public void rotateY(int angleDegrees) {
    rotateY((float)Math.toRadians(angleDegrees));
  }
  public void rotateZ(int angleDegrees) {
    rotateZ((float)Math.toRadians(angleDegrees));
  }
  public void rotate(AxisAngle4f axisAngle) {
    matrixTemp.setIdentity();
    matrixTemp.setRotation(axisAngle);
    matrixRotate.mul(matrixTemp, matrixRotate);
    repaint();
  }
  public void setCenter(Point3f center) {
    chemframe.setRotationCenter(center);
  }
}


