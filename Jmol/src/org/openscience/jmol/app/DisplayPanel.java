/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.openscience.jmol.app;

import org.jmol.api.*;
import org.jmol.i18n.GT;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.event.*;

public class DisplayPanel extends JPanel
  implements ComponentListener, Printable {
  StatusBar status;
  GuiMap guimap;
  JmolViewer viewer;
  
  private String displaySpeed;

  Dimension startupDimension;
  boolean haveDisplay;
  
  public DisplayPanel(StatusBar status, GuiMap guimap, boolean haveDisplay, int startupWidth, int startupHeight) {
    startupDimension = new Dimension(startupWidth, startupHeight);
    this.haveDisplay = haveDisplay;
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
    viewer.setScreenDimension(haveDisplay? getSize(dimSize) : startupDimension);
  }

  // for now, default to true
  private boolean showPaintTime = true;

  // current dimensions of the display screen
  private final Dimension dimSize = new Dimension();
  private final Rectangle rectClip = new Rectangle();

  public void start() {
    addComponentListener(this);
  }

  void setRotateMode() {
      Jmol.setRotateButton();
      viewer.setSelectionHalos(false);
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
    viewer.setScreenDimension(haveDisplay? getSize(dimSize) : startupDimension);
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
  private HydrogensAction hydrogensAction = new HydrogensAction();
  private MeasurementsAction measurementsAction =
    new MeasurementsAction();
  private SelectallAction selectallAction = new SelectallAction();
  private DeselectallAction deselectallAction = new DeselectallAction();
  private PerspectiveAction perspectiveAction = new PerspectiveAction();
  private AxesAction axesAction = new AxesAction();
  private BoundboxAction boundboxAction = new BoundboxAction();

  void moveTo(String move) {
  if (viewer.getShowBbcage() || viewer.getBooleanProperty("showUnitCell"))
    viewer.evalStringQuiet(move);
  else
    viewer.evalStringQuiet("boundbox on;" + move +";delay 1;boundbox off");
  }

  class HydrogensAction extends AbstractAction {

    public HydrogensAction() {
      super("hydrogensCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.evalStringQuiet("set showHydrogens " + cbmi.isSelected());
    }
  }

  class MeasurementsAction extends AbstractAction {

    public MeasurementsAction() {
      super("measurementsCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.evalStringQuiet ("set showMeasurements " +cbmi.isSelected());
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.evalStringQuiet("select all");
    }
  }

  class DeselectallAction extends AbstractAction {

    public DeselectallAction() {
      super("deselectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.evalStringQuiet("select none");
    }
  }

  class PickAction extends AbstractAction {

    public PickAction() {
      super("pick");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setSelectionHalos(false);
      status.setStatus(1, GT._("Select Atoms"));
    }
  }

  class DeleteAction extends AbstractAction {

    public DeleteAction() {
      super("delete");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      //not implemented (I hope)
      viewer.setSelectionHalos(false);
      status.setStatus(1, GT._("Delete Atoms"));
    }
  }

  class RotateAction extends AbstractAction {

    public RotateAction() {
      super("rotate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setSelectionHalos(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class ZoomAction extends AbstractAction {

    public ZoomAction() {
      super("zoom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setSelectionHalos(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class XlateAction extends AbstractAction {

    public XlateAction() {
      super("xlate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.setSelectionHalos(false);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class FrontAction extends AbstractAction {

    public FrontAction() {
      super("front");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      moveTo("moveto 2.0 front");
    }
  }

  class TopAction extends AbstractAction {

    public TopAction() {
      super("top");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      moveTo("moveto 1.0 front;moveto 2.0 top");
    }
  }

  class BottomAction extends AbstractAction {

    public BottomAction() {
      super("bottom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      moveTo("moveto 1.0 front;moveto 2.0 bottom");
    }
  }

  class RightAction extends AbstractAction {

    public RightAction() {
      super("right");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      moveTo("moveto 1.0 front;moveto 2.0 right");
    }
  }

  class LeftAction extends AbstractAction {

    public LeftAction() {
      super("left");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      moveTo("moveto 1.0 front;moveto 2.0 left");
    }
  }

  class DefineCenterAction extends AbstractAction {

    public DefineCenterAction() {
      super("definecenter");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      viewer.evalStringQuiet("center (selected)");
      setRotateMode();
      viewer.setSelectionHalos(false);
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

  class PerspectiveAction extends AbstractAction {

    public PerspectiveAction() {
      super("perspectiveCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.evalStringQuiet("set PerspectiveDepth " +cbmi.isSelected());
    }
  }

  class AxesAction extends AbstractAction {

    public AxesAction() {
      super("axesCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.evalStringQuiet("set showAxes " + cbmi.isSelected());
    }
  }

  class BoundboxAction extends AbstractAction {

    public BoundboxAction() {
      super("boundboxCheck");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      viewer.evalStringQuiet("set showBoundBox " + cbmi.isSelected());
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

  void setDisplayMenuState() {
    guimap.setSelected("perspectiveCheck", viewer.getPerspectiveDepth());
    guimap.setSelected("hydrogensCheck", viewer.getShowHydrogens());
    guimap.setSelected("measurementsCheck", viewer.getShowMeasurements());
    guimap.setSelected("axesCheck", viewer.getShowAxes());
    guimap.setSelected("boundboxCheck", viewer.getShowBbcage());
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      defineCenterAction,
      hydrogensAction, measurementsAction,
      selectallAction, deselectallAction,
      homeAction, perspectiveAction,
      axesAction, boundboxAction,
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
    int motionEventNumber = viewer.getMotionEventNumber();
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
}


