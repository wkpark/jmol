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
import org.openscience.jmol.viewer.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;

public class DisplayPanel extends JPanel
  implements ComponentListener, Printable {
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
    viewer.setScreenDimension(getSize(dimSize));
  }

  // for now, default to true
  private boolean showPaintTime = true;

  // current dimensions of the display screen
  private final Dimension dimSize = new Dimension();
  private final Rectangle rectClip = new Rectangle();

  public void start() {
    addComponentListener(this);
  }

  private void setRotateMode() {
      Jmol.setRotateButton();
      viewer.setModeMouse(JmolConstants.MOUSE_ROTATE);
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
    viewer.setScreenDimension(getSize(dimSize));
    setRotateMode();
  }

  public void paint(Graphics g) {
    if (showPaintTime)
      startPaintClock();
    g.getClipBounds(rectClip);
    viewer.renderScreenImage(g, dimSize, rectClip);
    if (showPaintTime)
      stopPaintClock();
  }

  public int print(Graphics g, PageFormat pf, int pageIndex) {
    Graphics2D g2 = (Graphics2D)g;
    if (pageIndex > 0)
      return Printable.NO_SUCH_PAGE;
    rectClip.x = rectClip.y = 0;
    int screenWidth = rectClip.width = viewer.getScreenWidth();
    int screenHeight = rectClip.height = viewer.getScreenHeight();
    Image image = viewer.getScreenImage();
    int pageX = (int)pf.getImageableX();
    int pageY = (int)pf.getImageableY();
    int pageWidth = (int)pf.getImageableWidth();
    int pageHeight = (int)pf.getImageableHeight();
    float scaleWidth = pageWidth / (float)screenWidth;
    float scaleHeight = pageHeight / (float)screenHeight;
    float scale = (scaleWidth < scaleHeight ? scaleWidth : scaleHeight);
    if (scale < 1) {
      int width =(int)(screenWidth * scale);
      int height =(int)(screenHeight * scale);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                          RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2.drawImage(image, pageX, pageY, width, height, null);
    } else {
      g2.drawImage(image, pageX, pageY, null);
    }
    viewer.releaseScreenImage();
    return Printable.PAGE_EXISTS;
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
  private aChargeColorAction acchargeAction = new aChargeColorAction();
  private aAtomTypeColorAction actypeAction = new aAtomTypeColorAction();
  private HydrogensAction hydrogensAction = new HydrogensAction();
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
      super("hydrogensCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShowHydrogens(cbmi.isSelected());
    }
  }

  class MeasurementsAction extends AbstractAction {

    public MeasurementsAction() {
      super("measurementsCheck");
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

      if (viewer.haveFrame()) {
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

      if (viewer.haveFrame()) {
        viewer.clearSelection();
      }
    }
  }

  class aChargeColorAction extends AbstractAction {

    public aChargeColorAction() {
      super("accharge");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeAtomColorProfile(JmolConstants.PALETTE_FORMALCHARGE);
    }
  }

  class aAtomTypeColorAction extends AbstractAction {

    public aAtomTypeColorAction() {
      super("actype");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeAtomColorProfile(JmolConstants.PALETTE_NONE_CPK);
    }
  }

  class PickAction extends AbstractAction {

    public PickAction() {
      super("pick");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeMouse(JmolConstants.MOUSE_PICK);
      viewer.setSelectionHaloEnabled(true);
      status.setStatus(1, "Select Atoms");
    }
  }

  class DeleteAction extends AbstractAction {

    public DeleteAction() {
      super("delete");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setModeMouse(JmolConstants.MOUSE_DELETE);
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
      viewer.setModeMouse(JmolConstants.MOUSE_ROTATE);
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
      viewer.setModeMouse(JmolConstants.MOUSE_ZOOM);
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
      viewer.setModeMouse(JmolConstants.MOUSE_XLATE);
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
      viewer.setCenterBitSet(viewer.getBitSetSelection());
      setRotateMode();
      viewer.setSelectionHaloEnabled(false);
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
      super("wireframerotationCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setWireframeRotation(cbmi.isSelected());
    }
  }

  class PerspectiveAction extends AbstractAction {

    public PerspectiveAction() {
      super("perspectiveCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setPerspectiveDepth(cbmi.isSelected());
    }
  }

  class AxesAction extends AbstractAction {

    public AxesAction() {
      super("axesCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShapeShow(JmolConstants.SHAPE_AXES, cbmi.isSelected());
    }
  }

  class BoundboxAction extends AbstractAction {

    public BoundboxAction() {
      super("boundboxCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.setShapeShow(JmolConstants.SHAPE_BBCAGE, cbmi.isSelected());
    }
  }

  MenuListener menuListener = new MenuListener() {
      public void menuSelected(MenuEvent e) {
        String menuKey = guimap.getKey(e.getSource());
        if (menuKey.equals("display")) {
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
    //    guimap.setSelected("wireframerotationCheck",
    //                       viewer.getWireframeRotation());
    guimap.setSelected("perspectiveCheck", viewer.getPerspectiveDepth());
    guimap.setSelected("hydrogensCheck", viewer.getShowHydrogens());
    guimap.setSelected("measurementsCheck", viewer.getShowMeasurements());
    guimap.setSelected("axesCheck",
                       viewer.getShapeShow(JmolConstants.SHAPE_AXES));
    guimap.setSelected("boundboxCheck",
                       viewer.getShapeShow(JmolConstants.SHAPE_BBCAGE));
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      defineCenterAction,
      hydrogensAction, measurementsAction,
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
  private int lastMotionEventNumber;

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
    int motionEventNumber = viewer.motionEventNumber;
    if (lastMotionEventNumber != motionEventNumber) {
      lastMotionEventNumber = motionEventNumber;
      resetTimes();
    }
  }

  private void stopPaintClock() {
    int time = (int)(System.currentTimeMillis() - timeBegin);
    recordTime(time);
    showTimes();
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
      viewer.rotateToX((float)Math.toRadians(angle));
    } else if (axis == Y_AXIS) {
      viewer.rotateToY((float)Math.toRadians(angle));
    } else if (axis == Z_AXIS) {
      viewer.rotateToZ((float)Math.toRadians(angle));
    }
  }
}


