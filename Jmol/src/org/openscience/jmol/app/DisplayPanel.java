/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
package org.openscience.jmol.app;

import org.openscience.jmol.*;
import org.openscience.jmol.viewer.JmolViewer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.ComponentListener;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

public class DisplayPanel extends JPanel implements ComponentListener {
  private StatusBar status;
  private GuiMap guimap;
  private JmolViewer viewer;
  
  private String displaySpeed;

  public DisplayPanel(StatusBar status, GuiMap guimap) {
    this.status = status;
    this.guimap = guimap;
    if (System.getProperty("painttime", "false").equals("true"))
      showPaintTime = true;
    displaySpeed = System.getProperty("display.speed");
    if (displaySpeed == null) {
        displaySpeed = "ms";
    }
    setDoubleBuffered(false);
  }

  public void setViewer(JmolViewer viewer) {
    this.viewer = viewer;
    viewer.setScreenDimension(getSize());
  }

  // for now, default to true
  private boolean showPaintTime = true;

  // current dimensions of the display screen
  private static final Rectangle rectClip = new Rectangle();

  private Measure measure = null;

  /*
  public Viewer getViewer() {
    return viewer;
  }
  */

  public void setMeasure(Measure measure) {
    this.measure = measure;
  }

  public void firePickedMeasure(int atomnum) {
    measure.firePicked(atomnum);
  }

  public void start() {
    addComponentListener(this);
  }

  private void setRotateMode() {
      Jmol.setRotateButton();
      viewer.setModeMouse(JmolViewer.ROTATE);
      viewer.setSelectionHaloEnabled(false);
  }
    
  public void componentHidden(java.awt.event.ComponentEvent e) {
  }

  public void componentMoved(java.awt.event.ComponentEvent e) {
  }

  public void componentResized(java.awt.event.ComponentEvent e) {
    updateSize();
  }

  public void componentShown(java.awt.event.ComponentEvent e) {
    updateSize();
  }

  private void updateSize() {
    viewer.setScreenDimension(getSize());
    setRotateMode();
  }

  public void paint(Graphics g) {
    if (showPaintTime)
      startPaintClock();
    g.getClipBounds(rectClip);
    g.drawImage(viewer.renderScreenImage(rectClip), 0, 0, null);
    if (showPaintTime)
      stopPaintClock();
  }

  public Image takeSnapshot() {
    return viewer.getScreenImage();
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
  private aNoneAction anoneAction = new aNoneAction();
  private aWireframeAction awireframeAction = new aWireframeAction();
  private aShadingAction ashadingAction = new aShadingAction();
  private aChargeColorAction acchargeAction = new aChargeColorAction();
  private aAtomTypeColorAction actypeAction = new aAtomTypeColorAction();
  private bNoneAction bnoneAction = new bNoneAction();
  private bWireframeAction bwireframeAction = new bWireframeAction();
  private bShadingAction bshadingAction = new bShadingAction();
  private PlainAction plainAction = new PlainAction();
  private SymbolsAction symbolsAction = new SymbolsAction();
  private TypesAction typesAction = new TypesAction();
  private NumbersAction numbersAction = new NumbersAction();
  private HydrogensAction hydrogensAction = new HydrogensAction();
  private VectorsAction vectorsAction = new VectorsAction();
  private MeasurementsAction measurementsAction =
    new MeasurementsAction();
  private SelectallAction selectallAction = new SelectallAction();
  private DeselectallAction deselectallAction = new DeselectallAction();
  private WireFrameRotationAction wireframerotationAction =
    new WireFrameRotationAction();
  private PerspectiveAction perspectiveAction = new PerspectiveAction();
  private AxesAction axesAction = new AxesAction();
  private BoundboxAction boundboxAction = new BoundboxAction();

  class HydrogensAction extends AbstractAction {

    public HydrogensAction() {
      super("hydrogens");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShowHydrogens(cbmi.isSelected());
    }
  }

  class VectorsAction extends AbstractAction {

    public VectorsAction() {
      super("vectors");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShowVectors(cbmi.isSelected());
    }
  }

  class MeasurementsAction extends AbstractAction {

    public MeasurementsAction() {
      super("measurements");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShowMeasurements(cbmi.isSelected());
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (viewer.haveFile()) {
        viewer.selectAll();
      }
    }
  }

  class DeselectallAction extends AbstractAction {

    public DeselectallAction() {
      super("deselectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (viewer.haveFile()) {
        viewer.clearSelection();
      }
    }
  }

  class aNoneAction extends AbstractAction {

    public aNoneAction() {
      super("anone");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleAtom(JmolViewer.NONE);
    }
  }

  class aShadingAction extends AbstractAction {

    public aShadingAction() {
      super("ashading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleAtom(JmolViewer.SHADED);
    }
  }

  class aWireframeAction extends AbstractAction {

    public aWireframeAction() {
      super("awireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleAtom(JmolViewer.WIREFRAME);
    }
  }

  class aChargeColorAction extends AbstractAction {

    public aChargeColorAction() {
      super("accharge");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeAtomColorProfile(JmolViewer.ATOMCHARGE);
    }
  }

  class aAtomTypeColorAction extends AbstractAction {

    public aAtomTypeColorAction() {
      super("actype");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeAtomColorProfile(JmolViewer.ATOMTYPE);
    }
  }

  class bNoneAction extends AbstractAction {

    public bNoneAction() {
      super("bnone");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleBond(JmolViewer.NONE);
    }
  }

  class bShadingAction extends AbstractAction {

    public bShadingAction() {
      super("bshading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleBond(JmolViewer.SHADED);
    }
  }

  class bWireframeAction extends AbstractAction {

    public bWireframeAction() {
      super("bwireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleBond(JmolViewer.WIREFRAME);
    }
  }

  class PickAction extends AbstractAction {

    public PickAction() {
      super("pick");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      if (measure.isShowing()) {
        viewer.setModeMouse(JmolViewer.MEASURE);
      } else {
        viewer.setModeMouse(JmolViewer.PICK);
        viewer.setSelectionHaloEnabled(true);
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
      viewer.setModeMouse(JmolViewer.DELETE);
      viewer.setSelectionHaloEnabled(false);
      status.setStatus(1, "Delete Atoms");
    }
  }

  class RotateAction extends AbstractAction {

    public RotateAction() {
      super("rotate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeMouse(JmolViewer.ROTATE);
      viewer.setSelectionHaloEnabled(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class ZoomAction extends AbstractAction {

    public ZoomAction() {
      super("zoom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeMouse(JmolViewer.ZOOM);
      viewer.setSelectionHaloEnabled(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class XlateAction extends AbstractAction {

    public XlateAction() {
      super("xlate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeMouse(JmolViewer.XLATE);
      viewer.setSelectionHaloEnabled(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class FrontAction extends AbstractAction {

    public FrontAction() {
      super("front");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.rotateFront();
    }
  }

  class TopAction extends AbstractAction {

    public TopAction() {
      super("top");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.rotateToX(90);
    }
  }

  class BottomAction extends AbstractAction {

    public BottomAction() {
      super("bottom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.rotateToX(-90);
    }
  }

  class RightAction extends AbstractAction {

    public RightAction() {
      super("right");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.rotateToY(90);
    }
  }

  class LeftAction extends AbstractAction {

    public LeftAction() {
      super("left");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.rotateToY(-90);
    }
  }

  class DefineCenterAction extends AbstractAction {

    public DefineCenterAction() {
      super("definecenter");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setCenterAsSelected();
      setRotateMode();
      viewer.setSelectionHaloEnabled(false);
    }
  }

  class PlainAction extends AbstractAction {

    public PlainAction() {
      super("plain");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleLabel(JmolViewer.NOLABELS);
    }
  }

  class SymbolsAction extends AbstractAction {

    public SymbolsAction() {
      super("symbols");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleLabel(JmolViewer.SYMBOLS);
    }
  }

  class TypesAction extends AbstractAction {

    public TypesAction() {
      super("types");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleLabel(JmolViewer.TYPES);
    }
  }

  class NumbersAction extends AbstractAction {

    public NumbersAction() {
      super("numbers");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setStyleLabel(JmolViewer.NUMBERS);
    }
  }

  class HomeAction extends AbstractAction {

    public HomeAction() {
      super("home");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.homePosition();
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
      viewer.setWireframeRotation(cbmi.isSelected());
    }
  }

  class PerspectiveAction extends AbstractAction {

    public PerspectiveAction() {
      super("perspective");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setPerspectiveDepth(cbmi.isSelected());
    }
  }

  class AxesAction extends AbstractAction {

    public AxesAction() {
      super("axes");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShowAxes(cbmi.isSelected());
    }
  }

  class BoundboxAction extends AbstractAction {

    public BoundboxAction() {
      super("boundbox");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShowBoundingBox(cbmi.isSelected());
    }
  }

  MenuListener menuListener = new MenuListener() {
      public void menuSelected(MenuEvent e) {
        String menuKey = guimap.getKey(e.getSource());
        if (menuKey.equals("Jmol.display")) {
          setDisplayMenuState();
        }
      }
      public void menuDeselected(MenuEvent e) {
      }
      public void menuCanceled(MenuEvent e) {
      }
    };

  public MenuListener getMenuListener() {
    return menuListener;
  }

  private void setDisplayMenuState() {
    guimap.setSelected("Jmol.wireframerotation",
                       viewer.getWireframeRotation());
    guimap.setSelected("Jmol.perspective", viewer.getPerspectiveDepth());
    guimap.setSelected("Jmol.hydrogens", viewer.getShowHydrogens());
    guimap.setSelected("Jmol.vectors", viewer.getShowVectors());
    guimap.setSelected("Jmol.measurements", viewer.getShowMeasurements());
    guimap.setSelected("Jmol.axes", viewer.getShowAxes());
    guimap.setSelected("Jmol.boundbox", viewer.getShowBoundingBox());
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      defineCenterAction,
      anoneAction, awireframeAction, ashadingAction,
      bnoneAction, bwireframeAction, bshadingAction,
      plainAction,
      symbolsAction, typesAction, numbersAction,
      hydrogensAction, vectorsAction, measurementsAction,
      selectallAction, deselectallAction,
      homeAction, wireframerotationAction, perspectiveAction,
      axesAction, boundboxAction,
      acchargeAction, actypeAction,
    };
    return defaultActions;
  }

  // code to record last and average times
  // last and average of all the previous times are shown in the status window

  private static int timeLast = 0;
  private static int timeCount;
  private static int timeTotal;

  private void resetTimes() {
    timeCount = timeTotal = 0;
    timeLast = -1;
  }

  private void recordTime(int time) {
    if (timeLast != -1) {
      timeTotal += timeLast;
      ++timeCount;
    }
    timeLast = time;
  }

  private long timeBegin;
  private boolean wereInMotion;
  private boolean inMotion;

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
    inMotion = viewer.getInMotion();
    if (!wereInMotion & inMotion)
      resetTimes();
  }

  private void stopPaintClock() {
    int time = (int)(System.currentTimeMillis() - timeBegin);
    recordTime(time);
    showTimes();
    wereInMotion = inMotion;
  }

  private String fmt(int num) {
    if (num < 0)
      return "---";
    if (num < 10)
      return "  " + num;
    if (num < 100)
      return " " + num;
    return "" + num;
  }

  private void showTimes() {
    int timeAverage =
      (timeCount == 0)
      ? -1
      : (timeTotal + timeCount/2) / timeCount; // round, don't truncate
    if (displaySpeed.equalsIgnoreCase("fps")) {
        status.setStatus(3, fmt(1000/timeLast) + "FPS : " + fmt(1000/timeAverage) + "FPS");
    } else {
        status.setStatus(3, fmt(timeLast) + "ms : " + fmt(timeAverage) + "ms");
    }
  }

  public final static int X_AXIS = 1;
  public final static int Y_AXIS = 2;
  public final static int Z_AXIS = 3;

  public void rotate(int axis, double angle) {
    if (axis == X_AXIS) {
      viewer.rotateToX(Math.toRadians(angle));
    } else if (axis == Y_AXIS) {
      viewer.rotateToY(Math.toRadians(angle));
    } else if (axis == Z_AXIS) {
      viewer.rotateToZ(Math.toRadians(angle));
    }
  }
}


