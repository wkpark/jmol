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
import java.awt.event.ComponentListener;
import javax.swing.event.MenuListener;
import javax.swing.event.MenuEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JPanel;
import javax.swing.RepaintManager;

/**
 *  @author  Bradley A. Smith (bradley@baysmith.com)
 *  @author  J. Daniel Gezelter
 */
public class DisplayPanel extends JPanel
  implements MeasurementListListener, PropertyChangeListener,
             ComponentListener {
  private StatusBar status;
  private GuiMap guimap;
  private DisplayControl control;
  
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
  }

  public void setDisplayControl(DisplayControl control) {
    this.control = control;
  }

  // for now, default to true
  private boolean showPaintTime = true;

  // current dimensions of the display screen
  private static Dimension dimCurrent = null;
  private static final Rectangle rectClip = new Rectangle();

  private Measure measure = null;

  public DisplayControl getDisplayControl() {
    return control;
  }

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
      control.setModeMouse(DisplayControl.ROTATE);
  }
    
  public ChemFrame getFrame() {
    return control.getFrame();
  }

  public void mlistChanged(MeasurementListEvent mle) {
    control.mlistChanged(mle);
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
    dimCurrent = getSize();
    if ((dimCurrent.width == 0) || (dimCurrent.height == 0))
      dimCurrent = null;
    control.setScreenDimension(dimCurrent);
    control.scaleFitToScreen();
    setRotateMode();
  }

  public void paint(Graphics g) {
    if (showPaintTime)
      startPaintClock();
    g.getClipBounds(rectClip);
    g.setColor(control.getColorBackground());
    g.fillRect(rectClip.x, rectClip.y, rectClip.width, rectClip.height);
    if (control.getFrame() != null) {
      control.setGraphicsContext(g, rectClip);
      frameRenderer.paint(g, control);
      // FIXME -- measurements rendered incorrectly
      // this is in the wrong spot because the display of measurements
      // needs to take z-order into account
      if (control.getShowMeasurements())
        measureRenderer.paint(g, rectClip, control);
      Rectangle rect = control.getRubberBandSelection();
      if (rect != null) {
        g.setColor(control.getColorRubberband());
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
      }
      if (showPaintTime)
        stopPaintClock();
    }
    control.notifyRepainted();
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
  private aNoneAction anoneAction = new aNoneAction();
  private aInvisibleAction ainvisibleAction = new aInvisibleAction();
  private aQuickdrawAction aquickdrawAction = new aQuickdrawAction();
  private aShadingAction ashadingAction = new aShadingAction();
  private aWireframeAction awireframeAction = new aWireframeAction();
  private aChargeColorAction acchargeAction = new aChargeColorAction();
  private aAtomTypeColorAction actypeAction = new aAtomTypeColorAction();
  private bNoneAction bnoneAction = new bNoneAction();
  private bQuickdrawAction bquickdrawAction = new bQuickdrawAction();
  private bShadingAction bshadingAction = new bShadingAction();
  private bBoxAction bboxAction = new bBoxAction();
  private bWireframeAction bwireframeAction = new bWireframeAction();
  private PlainAction plainAction = new PlainAction();
  private SymbolsAction symbolsAction = new SymbolsAction();
  private TypesAction typesAction = new TypesAction();
  private NumbersAction numbersAction = new NumbersAction();
  private BondsAction bondsAction = new BondsAction();
  private AtomsAction atomsAction = new AtomsAction();
  private HydrogensAction hydrogensAction = new HydrogensAction();
  private VectorsAction vectorsAction = new VectorsAction();
  private MeasurementsAction measurementsAction =
    new MeasurementsAction();
  private SelectallAction selectallAction = new SelectallAction();
  private DeselectallAction deselectallAction = new DeselectallAction();
  private WireFrameRotationAction wireframerotationAction =
    new WireFrameRotationAction();
  private PerspectiveAction perspectiveAction = new PerspectiveAction();

  class BondsAction extends AbstractAction {

    public BondsAction() {
      super("bonds");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setShowBonds(cbmi.isSelected());
    }
  }

  class AtomsAction extends AbstractAction {

    public AtomsAction() {
      super("atoms");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setShowAtoms(cbmi.isSelected());
    }
  }

  class HydrogensAction extends AbstractAction {

    public HydrogensAction() {
      super("hydrogens");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setShowHydrogens(cbmi.isSelected());
    }
  }

  class VectorsAction extends AbstractAction {

    public VectorsAction() {
      super("vectors");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setShowVectors(cbmi.isSelected());
    }
  }

  class MeasurementsAction extends AbstractAction {

    public MeasurementsAction() {
      super("measurements");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      JCheckBoxMenuItem cbmi = (JCheckBoxMenuItem) e.getSource();
      control.setShowMeasurements(cbmi.isSelected());
    }
  }

  class SelectallAction extends AbstractAction {

    public SelectallAction() {
      super("selectall");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {

      if (control.haveFile()) {
        control.selectAll();
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
      }
    }
  }

  class aNoneAction extends AbstractAction {

    public aNoneAction() {
      super("anone");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleAtom(DisplayControl.NONE);
      // FIXME -- these repaints are not necessary
      // confirm later when not working on something else
      repaint();
    }
  }

  class aInvisibleAction extends AbstractAction {

    public aInvisibleAction() {
      super("ainvisible");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleAtom(DisplayControl.INVISIBLE);
      // FIXME -- mth -- these repaints are not necessary
      // confirm later when not working on something else
      repaint();
    }
  }

  class aQuickdrawAction extends AbstractAction {

    public aQuickdrawAction() {
      super("aquickdraw");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleAtom(DisplayControl.QUICKDRAW);
      repaint();
    }
  }

  class aShadingAction extends AbstractAction {

    public aShadingAction() {
      super("ashading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleAtom(DisplayControl.SHADING);
    }
  }

  class aWireframeAction extends AbstractAction {

    public aWireframeAction() {
      super("awireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleAtom(DisplayControl.WIREFRAME);
      repaint();
    }
  }

  class aChargeColorAction extends AbstractAction {

    public aChargeColorAction() {
      super("accharge");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setModeAtomColorProfile(DisplayControl.ATOMCHARGE);
      repaint();
    }
  }

  class aAtomTypeColorAction extends AbstractAction {

    public aAtomTypeColorAction() {
      super("actype");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setModeAtomColorProfile(DisplayControl.ATOMTYPE);
      repaint();
    }
  }

  class bNoneAction extends AbstractAction {

    public bNoneAction() {
      super("bnone");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleBond(DisplayControl.NONE);
      // FIXME -- these repaints are not necessary
      // confirm later when not working on something else
      repaint();
    }
  }

  class bQuickdrawAction extends AbstractAction {

    public bQuickdrawAction() {
      super("bquickdraw");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleBond(DisplayControl.QUICKDRAW);
      repaint();
    }
  }

  class bShadingAction extends AbstractAction {

    public bShadingAction() {
      super("bshading");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleBond(DisplayControl.SHADING);
      repaint();
    }
  }

  class bBoxAction extends AbstractAction {

    public bBoxAction() {
      super("bbox");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleBond(DisplayControl.BOX);
      repaint();
    }
  }

  class bWireframeAction extends AbstractAction {

    public bWireframeAction() {
      super("bwireframe");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleBond(DisplayControl.WIREFRAME);
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
        control.setModeMouse(DisplayControl.MEASURE);
      } else {
        control.setModeMouse(DisplayControl.PICK);
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
      control.setModeMouse(DisplayControl.DELETE);
      status.setStatus(1, "Delete Atoms");
    }
  }

  class RotateAction extends AbstractAction {

    public RotateAction() {
      super("rotate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setModeMouse(DisplayControl.ROTATE);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class ZoomAction extends AbstractAction {

    public ZoomAction() {
      super("zoom");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setModeMouse(DisplayControl.ZOOM);
      status.setStatus(1, ((JComponent) e.getSource()).getToolTipText());
    }
  }

  class XlateAction extends AbstractAction {

    public XlateAction() {
      super("xlate");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setModeMouse(DisplayControl.XLATE);
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

  class PlainAction extends AbstractAction {

    public PlainAction() {
      super("plain");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleLabel(DisplayControl.NOLABELS);
      repaint();
    }
  }

  class SymbolsAction extends AbstractAction {

    public SymbolsAction() {
      super("symbols");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleLabel(DisplayControl.SYMBOLS);
      repaint();
    }
  }

  class TypesAction extends AbstractAction {

    public TypesAction() {
      super("types");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleLabel(DisplayControl.TYPES);
      repaint();
    }
  }

  class NumbersAction extends AbstractAction {

    public NumbersAction() {
      super("numbers");
      this.setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
      control.setStyleLabel(DisplayControl.NUMBERS);
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
                       control.getWireframeRotation());
    guimap.setSelected("Jmol.perspective", control.getPerspectiveDepth());
    guimap.setSelected("Jmol.bonds", control.getShowBonds());
    guimap.setSelected("Jmol.atoms", control.getShowAtoms());
    guimap.setSelected("Jmol.hydrogens", control.getShowHydrogens());
    guimap.setSelected("Jmol.vectors", control.getShowVectors());
    guimap.setSelected("Jmol.measurements", control.getShowMeasurements());
    final String[] modeLabel =
      {"Jmol.plain", "Jmol.symbols", "Jmol.types", "Jmol.numbers"};
    guimap.setSelected(modeLabel[control.getStyleLabel()], true);
    final String[] modeAtom =
      {"Jmol.aquickdraw", "Jmol.ashading", "Jmol.awireframe",
       "Jmol.ainvisible", "Jmol.anone"};
    guimap.setSelected(modeAtom[control.getStyleAtom()], true);
    final String[] modeBond =
      {"Jmol.bquickdraw", "Jmol.bshading", "Jmol.bwireframe",
       "Jmol.bbox", "Jmol.bnone"};
    guimap.setSelected(modeBond[control.getStyleBond()], true);
    final String[] modeColor =
      {"Jmol.actype", "Jmol.accharge"};
    guimap.setSelected(modeColor[control.getModeAtomColorProfile()], true);
  }

  public Action[] getActions() {

    Action[] defaultActions = {
      deleteAction, pickAction, rotateAction, zoomAction, xlateAction,
      frontAction, topAction, bottomAction, rightAction, leftAction,
      defineCenterAction,
      aquickdrawAction, ashadingAction, awireframeAction,
      ainvisibleAction, anoneAction, 
      bquickdrawAction, bshadingAction, bwireframeAction,
      bboxAction, bnoneAction,
      plainAction,
      symbolsAction, typesAction, numbersAction,
      bondsAction, atomsAction, hydrogensAction,
      vectorsAction, measurementsAction,
      selectallAction, deselectallAction,
      homeAction, wireframerotationAction, perspectiveAction,
      acchargeAction, actypeAction,
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

  private void startPaintClock() {
    timeBegin = System.currentTimeMillis();
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
      control.rotateToX(Math.toRadians(angle));
    } else if (axis == Y_AXIS) {
      control.rotateToY(Math.toRadians(angle));
    } else if (axis == Z_AXIS) {
      control.rotateToZ(Math.toRadians(angle));
    }
  }
}


