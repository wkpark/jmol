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
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class DisplayPanel extends JPanel
    implements MeasurementListListener, PropertyChangeListener {

  private boolean rubberbandSelectionMode = false;
  private int bx, by, rtop, rbottom, rleft, rright;
  private static int prevMouseX, prevMouseY;

  // for now, default to true
  private boolean showPaintTime = true;

  // current dimensions of the display screen
  private static Dimension dimCurrent = null;
  private static final Rectangle rectClip = new Rectangle();
  // previous dimensions ... used to detect resize operations
  private static Dimension dimPrevious = null;

  public static final int ROTATE = 0;
  public static final int ZOOM = 1;
  public static final int XLATE = 2;
  public static final int PICK = 3;
  public static final int DEFORM = 4;
  public static final int MEASURE = 5;
  public static final int DELETE = 6;
  private int modeMouse = ROTATE;

  private StatusBar status;

  private Measure measure = null;
  private static DisplayControl control;

  public DisplayPanel(StatusBar status, DisplayControl control) {
    this.status = status;
    this.control = control;
    if (System.getProperty("painttime", "false").equals("true"))
      showPaintTime = true;
  }

  /*
  public DisplaySettings getSettings() {
    return control.getSettings();
  }
  */

  public DisplayControl getDisplayControl() {
    return control;
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
    String vers = System.getProperty("java.version");
    control.setJvm12orGreater(vers.compareTo("1.2") >= 0);
  }

  private void setRotateMode() {
      Jmol.setRotateButton();
      modeMouse = ROTATE;
  }
    
  public ChemFrame getFrame() {
    return control.getFrame();
  }

  public void mlistChanged(MeasurementListEvent mle) {
    control.mlistChanged(mle);
  }

  class MyAdapter extends MouseAdapter {

    public void mousePressed(MouseEvent e) {

      prevMouseX = e.getX();
      prevMouseY = e.getY();

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

      if (control.haveFile()) {
        Atom atom = control.getFrame().getNearestAtom(e.getX(), e.getY());
        switch (modeMouse) {
        case PICK:
          if (atom == null) {
            control.clearSelection();
            break;
          }
          if (!e.isShiftDown()) {
            int selectionCount = control.countSelection();
            boolean wasSelected = control.isSelected(atom);
            control.clearSelection();
            if (selectionCount == 1 && wasSelected)
              break;
          }
          control.toggleSelection(atom);
          break;
        case DELETE:
          if (atom != null) {
            control.getFrame().deleteAtom(atom);
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
      control.setMouseDragged(false);
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

      control.setMouseDragged(true);
      switch (modeMouse) {
      case ROTATE:
        control.rotateBy(x - prevMouseX, y - prevMouseY);
        break;
      case XLATE:
        control.translateBy(x - prevMouseX, y - prevMouseY);
        break;
      case ZOOM:
        double xs = 1.0 + (double)(x - prevMouseX) / dimCurrent.width;
        double ys = 1.0 + (double)(prevMouseY - y) / dimCurrent.height;
        double s = (xs + ys) / 2;
        control.multiplyZoomScale(s);
        break;
      case PICK:
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
        if (control.haveFile()) {
          Atom[] selectedAtoms =
            control.getFrame().findAtomsInRegion(rleft, rtop, rright, rbottom);
          if (e.isShiftDown()) {
            control.addSelection(selectedAtoms);
          } else {
            control.clearSelection();
            control.addSelection(selectedAtoms);
          }
        }
        break;
      }
      prevMouseX = x;
      prevMouseY = y;
      // I can't figure out if I want this repaint here or not
      repaint();
    }
  }

  public static void setBackgroundColor(Color bg) {
    control.setBackgroundColor(bg);
  }

  public static Color getBackgroundColor() {
    return control.getBackgroundColor();
  }

  public void paint(Graphics g) {
    if (showPaintTime)
      startPaintClock();

    Color fg = getForeground();

    dimCurrent = getSize();
    if (dimPrevious == null)
      control.setScreenDimension(dimCurrent);
    rectClip.setBounds(0, 0, dimCurrent.width, dimCurrent.height);
    g.getClipBounds(rectClip);
    g.setColor(control.getBackgroundColor());
    g.fillRect(rectClip.x, rectClip.y, rectClip.width, rectClip.height);
    if (control.getFrame() != null) {
      if (! dimCurrent.equals(dimPrevious)) {
        control.scaleFitToScreen(dimCurrent);
        setRotateMode();
      }
      control.maybeEnableAntialiasing(g);

      frameRenderer.paint(g, rectClip, control);
      measureRenderer.paint(g, rectClip, control);
      if (rubberbandSelectionMode) {
        g.setColor(fg);
        g.drawRect(rleft, rtop, rright - rleft, rbottom - rtop);
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
  private PerspectiveAction perspectiveAction = new PerspectiveAction();
  private UseGraphics2DAction useGraphics2DAction = new UseGraphics2DAction();
  private DoubleBufferAction doubleBufferAction = new DoubleBufferAction();
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
      control.setShowBonds(!control.getShowBonds());
    }
  }

  class AtomsAction extends AbstractAction {

    public AtomsAction() {
      super("atoms");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setShowAtoms(!control.getShowAtoms());
      repaint();
    }
  }

  class VectorsAction extends AbstractAction {

    public VectorsAction() {
      super("vectors");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setShowVectors(!control.getShowVectors());
    }
  }

  class HydrogensAction extends AbstractAction {

    public HydrogensAction() {
      super("hydrogens");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setShowHydrogens(!control.getShowHydrogens());
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (control.haveFile()) {
        control.addSelection(control.getFrame().getAtoms());
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

      if (control.haveFile()) {
        control.clearSelection();
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
      control.setAtomDrawMode(DisplayControl.QUICKDRAW);
      repaint();
    }
  }

  class aShadingAction extends AbstractAction {

    public aShadingAction() {
      super("ashading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setAtomDrawMode(DisplayControl.SHADING);
    }
  }

  class aWireframeAction extends AbstractAction {

    public aWireframeAction() {
      super("awireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setAtomDrawMode(DisplayControl.WIREFRAME);
      repaint();
    }
  }

  class aChargeColorAction extends AbstractAction {

    public aChargeColorAction() {
      super("accharge");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setAtomColorProfile(DisplayControl.ATOMCHARGE);
      repaint();
    }
  }

  class aAtomTypeColorAction extends AbstractAction {

    public aAtomTypeColorAction() {
      super("actype");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setAtomColorProfile(DisplayControl.ATOMTYPE);
      repaint();
    }
  }

  class bQuickdrawAction extends AbstractAction {

    public bQuickdrawAction() {
      super("bquickdraw");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setBondDrawMode(DisplayControl.QUICKDRAW);
      repaint();
    }
  }

  class bShadingAction extends AbstractAction {

    public bShadingAction() {
      super("bshading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setBondDrawMode(DisplayControl.SHADING);
      repaint();
    }
  }

  class bLineAction extends AbstractAction {

    public bLineAction() {
      super("bline");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setBondDrawMode(DisplayControl.LINE);
      repaint();
    }
  }

  class bWireframeAction extends AbstractAction {

    public bWireframeAction() {
      super("bwireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setBondDrawMode(DisplayControl.WIREFRAME);
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
      control.rotateFront();
    }
  }

  class TopAction extends AbstractAction {

    public TopAction() {
      super("top");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.rotateToX(90);
    }
  }

  class BottomAction extends AbstractAction {

    public BottomAction() {
      super("bottom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.rotateToX(-90);
    }
  }

  class RightAction extends AbstractAction {

    public RightAction() {
      super("right");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.rotateToY(90);
    }
  }

  class LeftAction extends AbstractAction {

    public LeftAction() {
      super("left");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.rotateToY(-90);
    }
  }

  class DefineCenterAction extends AbstractAction {

    public DefineCenterAction() {
      super("definecenter");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setCenterAsSelected();
      setRotateMode();
    }
  }

  class PerspectiveAction extends AbstractAction {

    public PerspectiveAction() {
      super("perspective");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setPerspectiveDepth(cbmi.isSelected());
    }
  }

  class UseGraphics2DAction extends AbstractAction {

    public UseGraphics2DAction() {
      super("usegraphics2d");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setWantsGraphics2D(cbmi.isSelected());
    }
  }

  class DoubleBufferAction extends AbstractAction {

    public DoubleBufferAction() {
      super("doublebuffer");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      RepaintManager.currentManager(null).
        setDoubleBufferingEnabled(cbmi.isSelected());
      System.out.println("isDoubleBufferingEnabled()=" +
                         RepaintManager.currentManager(null).
                         isDoubleBufferingEnabled());
    }
  }

  class Test1Action extends AbstractAction {

    public Test1Action() {
      super("test1");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setWantsGraphics2D(true);
    }
  }

  class Test2Action extends AbstractAction {

    public Test2Action() {
      super("test2");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setWantsGraphics2D(false);
      repaint();
    }
  }

  class Test3Action extends AbstractAction {

    public Test3Action() {
      super("test3");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      //      rotateZ(45);
      repaint();
    }
  }

  class Test4Action extends AbstractAction {

    public Test4Action() {
      super("test4");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      //      rotate(new AxisAngle4f(1, 1, 1, Math.PI/4));
      repaint();
    }
  }

  class PlainAction extends AbstractAction {

    public PlainAction() {
      super("plain");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setLabelMode(DisplayControl.NOLABELS);
      repaint();
    }
  }

  class SymbolsAction extends AbstractAction {

    public SymbolsAction() {
      super("symbols");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setLabelMode(DisplayControl.SYMBOLS);
      repaint();
    }
  }

  class TypesAction extends AbstractAction {

    public TypesAction() {
      super("types");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setLabelMode(DisplayControl.TYPES);
      repaint();
    }
  }

  class NumbersAction extends AbstractAction {

    public NumbersAction() {
      super("numbers");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setLabelMode(DisplayControl.NUMBERS);
      repaint();
    }
  }

  class HomeAction extends AbstractAction {

    public HomeAction() {
      super("home");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.homePosition();
      setRotateMode();
    }
  }

  class WireFrameRotationAction extends AbstractAction {

    public WireFrameRotationAction() {
      super("wireframerotation");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setWireframeRotation(cbmi.isSelected());
    }
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      defineCenterAction, perspectiveAction,
      useGraphics2DAction, doubleBufferAction,
      test1Action, test2Action, test3Action, test4Action,
      aquickdrawAction, ashadingAction, awireframeAction, bquickdrawAction,
      bshadingAction, blineAction, bwireframeAction, plainAction,
      symbolsAction, typesAction, numbersAction, bondsAction, atomsAction,
      vectorsAction, hydrogensAction, selectallAction, deselectallAction,
      homeAction, wireframerotationAction,
      acchargeAction, actypeAction
    };
    return defaultActions;
  }

  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(JmolModel.chemFileProperty)) {
      control.setChemFile((ChemFile) event.getNewValue());
      setRotateMode();
    } else if (event.getPropertyName().equals(JmolModel.chemFrameProperty)) {
      control.setFrame((ChemFrame) event.getNewValue());
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

  public final static int X_AXIS = 1;
  public final static int Y_AXIS = 2;
  public final static int Z_AXIS = 3;

  public void rotate(int axis, double angle) {
    if (axis == X_AXIS) {
      control.rotateToX(Math.toRadians(angle));
    } else if (axis == Y_AXIS) {
      control.rotateToY(Math.toRadians(angle));
    } else if (axis == Z_AXIS) {
      control.rotateToZ(Math.toRadians(angle));
    }
  }
}


