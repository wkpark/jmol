/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
package org.openscience.jmol;

import org.openscience.jmol.render.AtomRenderer;
import org.openscience.jmol.render.BondRenderer;
import org.openscience.jmol.render.LabelRenderer;
import org.openscience.jmol.render.ChemFrameRenderer;
import org.openscience.jmol.render.MeasureRenderer;
import org.openscience.jmol.render.Axes;
import org.openscience.jmol.render.BoundingBox;
import org.openscience.jmol.script.Eval;

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;
import java.net.URL;
import java.io.InputStream;
import java.io.File;
import java.beans.PropertyChangeListener;

final public class DisplayControl {

  public static DisplayControl control;

  public Component awtComponent;
  public ColorManager colorManager;
  public TransformManager transformManager;
  public SelectionManager selectionManager;
  public MouseManager mouseManager;
  public FileManager fileManager;
  public ModelManager modelManager;
  public RepaintManager repaintManager;
  public StyleManager styleManager;
  public LabelManager labelManager;
  public AxesManager axesManager;
  public MeasurementManager measurementManager;
  public ChemFrameRenderer frameRenderer;
  public MeasureRenderer measureRenderer;
  public AtomRenderer atomRenderer;
  public BondRenderer bondRenderer;
  public LabelRenderer labelRenderer;
  public Distributor distributor;
  public Eval eval;
  public Java12 java12;

  public String strJvmVersion;
  public boolean jvm12orGreater = false;
  public boolean jvm14orGreater = false;

  public DisplayControl(String strJvmVersion, Component awtComponent) {

    this.awtComponent = awtComponent;
    this.strJvmVersion = strJvmVersion;
    jvm12orGreater = (strJvmVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJvmVersion.compareTo("1.4") >= 0);

    control = this;
    colorManager = new ColorManager(this);
    transformManager = new TransformManager(this);
    selectionManager = new SelectionManager(this);
    mouseManager = new MouseManager(awtComponent, this);
    fileManager = new FileManager(this);
    modelManager = new ModelManager(this);
    repaintManager = new RepaintManager(this);
    styleManager = new StyleManager(this);
    labelManager = new LabelManager(this);
    axesManager = new AxesManager(this);
    measurementManager = new MeasurementManager(this);
    distributor = new Distributor(this);

    frameRenderer = new ChemFrameRenderer();
    measureRenderer = new MeasureRenderer();
    atomRenderer = new AtomRenderer(this);
    bondRenderer = new BondRenderer(this);
    labelRenderer = new LabelRenderer(this);

    eval = new Eval(this);
    if (jvm12orGreater)
      java12 = new Java12(this);
  }

  public Component getAwtComponent() {
    return awtComponent;
  }

  public boolean inMotion = false;

  private boolean structuralChange = false;


  public final static byte NOLABELS =  0;
  public final static byte SYMBOLS =   1;
  public final static byte TYPES =     2;
  public final static byte NUMBERS =   3;

  public final static byte QUICKDRAW = 0;
  public final static byte SHADING =   1;
  public final static byte WIREFRAME = 2;
  public final static byte INVISIBLE = 3; // for atoms
  public final static byte BOX =       3; // for bonds
  public final static byte NONE =      4;

  public final static byte COLOR =     -1;
  public final static byte ATOMTYPE =   0;
  public final static byte ATOMCHARGE = 1;

  public void homePosition() {
    // FIXME -- need to hold repaint during this process, but first 
    // figure out the interaction with the current holdRepaint setting
    setCenter(null);
    selectAll();
    transformManager.homePosition();
    refresh();
  }

  public void setGraphicsContext(Graphics g, Rectangle rectClip) {
    atomRenderer.setGraphicsContext(g, rectClip);
    bondRenderer.setGraphicsContext(g, rectClip);
    labelRenderer.setGraphicsContext(g, rectClip);
    maybeEnableAntialiasing(g);
  }

  public boolean hasStructuralChange() {
    return structuralChange;
  }

  public void setStructuralChange() {
    this.structuralChange = true;
  }

  public void resetStructuralChange() {
    structuralChange = false;
  }

  public final Hashtable imageCache = new Hashtable();
  public void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }

  public void logError(String strMsg) {
    System.out.println("strMsg");
  }

  /****************************************************************
   * delegated to TransformManager
   ****************************************************************/

  public void rotateXYBy(int xDelta, int yDelta) {
    transformManager.rotateXYBy(xDelta, yDelta);
    refresh();
  }

  public void rotateZBy(int zDelta) {
    transformManager.rotateZBy(zDelta);
    refresh();
  }

  public void rotateFront() {
    transformManager.rotateFront();
    refresh();
  }

  public void rotateToX(double angleRadians) {
    transformManager.rotateToX(angleRadians);
    refresh();
  }
  public void rotateToY(double angleRadians) {
    transformManager.rotateToY(angleRadians);
    refresh();
  }
  public void rotateToZ(double angleRadians) {
    transformManager.rotateToZ(angleRadians);
    refresh();
  }

  public void rotateToX(int angleDegrees) {
    rotateToX(Math.toRadians(angleDegrees));
  }
  public void rotateToY(int angleDegrees) {
    rotateToY(Math.toRadians(angleDegrees));
  }
  public void rotateToZ(int angleDegrees) {
    rotateToZ(Math.toRadians(angleDegrees));
  }

  public void rotateByX(double angleRadians) {
    transformManager.rotateByX(angleRadians);
    refresh();
  }
  public void rotateByY(double angleRadians) {
    transformManager.rotateByY(angleRadians);
    refresh();
  }
  public void rotateByZ(double angleRadians) {
    transformManager.rotateByZ(angleRadians);
    refresh();
  }
  public void rotateByX(int angleDegrees) {
    rotateByX(Math.toRadians(angleDegrees));
  }
  public void rotateByY(int angleDegrees) {
    rotateByY(Math.toRadians(angleDegrees));
  }
  public void rotateByZ(int angleDegrees) {
    rotateByZ(Math.toRadians(angleDegrees));
  }

  public void rotate(AxisAngle4d axisAngle) {
    transformManager.rotate(axisAngle);
    refresh();
  }

  public void translateXYBy(int xDelta, int yDelta) {
    transformManager.translateXYBy(xDelta, yDelta);
    refresh();
  }

  public void translateToXPercent(int percent) {
    transformManager.translateToXPercent(percent);
    refresh();
  }

  public void translateToYPercent(int percent) {
    transformManager.translateToYPercent(percent);
    refresh();
  }

  public void translateToZPercent(int percent) {
    transformManager.translateToZPercent(percent);
    refresh();
  }

  public int getTranslationXPercent() {
    return transformManager.getTranslationXPercent();
  }

  public int getTranslationYPercent() {
    return transformManager.getTranslationYPercent();
  }

  public int getTranslationZPercent() {
    return transformManager.getTranslationZPercent();
  }

  public void translateByXPercent(int percent) {
    translateToXPercent(getTranslationXPercent() + percent);
  }

  public void translateByYPercent(int percent) {
    translateToYPercent(getTranslationYPercent() + percent);
  }

  public void translateByZPercent(int percent) {
    translateToZPercent(getTranslationZPercent() + percent);
  }

  public void zoomBy(int pixels) {
    transformManager.zoomBy(pixels);
    refresh();
  }

  public int getZoomPercent() {
    return transformManager.zoomPercent;
  }

  public int getZoomPercentSetting() {
    return transformManager.zoomPercentSetting;
  }

  public void zoomToPercent(int percent) {
    transformManager.zoomToPercent(percent);
    refresh();
  }

  public void zoomByPercent(int percent) {
    transformManager.zoomByPercent(percent);
    refresh();
  }

  public void setZoomEnabled(boolean zoomEnabled) {
    transformManager.setZoomEnabled(zoomEnabled);
    refresh();
  }

  public boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  public int getSlabPercentSetting() {
    return transformManager.slabPercentSetting;
  }

  public void slabBy(int pixels) {
    transformManager.slabBy(pixels);
    refresh();
  }

  public void slabToPercent(int percentSlab) {
    transformManager.slabToPercent(percentSlab);
    refresh();
  }

  public void slabByPercent(int percentSlab) {
    transformManager.slabByPercent(percentSlab);
    refresh();
  }

  public void setSlabEnabled(boolean slabEnabled) {
    transformManager.setSlabEnabled(slabEnabled);
    refresh();
  }

  public void setModeSlab(int modeSlab) {
    transformManager.setModeSlab(modeSlab);
    refresh();
  }

  public int getModeSlab() {
    return transformManager.modeSlab;
  }

  public int getSlabValue() {
    return transformManager.slabValue;
  }

  public Matrix4d getPovRotateMatrix() {
    return transformManager.getPovRotateMatrix();
  }

  public Matrix4d getPovTranslateMatrix() {
    return transformManager.getPovTranslateMatrix();
  }

  public void calcViewTransformMatrix() {
    transformManager.calcViewTransformMatrix();
  }

  public void transformPoint(Point3d pointAngstroms, Point3d pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public Point3d transformPoint(Point3d pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public double scaleToScreen(int z, double sizeAngstroms) {
    return transformManager.scaleToScreen(z, sizeAngstroms);
  }

  public int scaleToScreen(int z, int milliAngstroms) {
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  public void setScreenDimension(Dimension dimCurrent) {
    transformManager.setScreenDimension(dimCurrent);
  }

  public Dimension getScreenDimension() {
    return transformManager.dimCurrent;
  }

  public void scaleFitToScreen() {
    transformManager.scaleFitToScreen();
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    transformManager.setPerspectiveDepth(perspectiveDepth);
    refresh();
  }

  public boolean getPerspectiveDepth() {
    return transformManager.perspectiveDepth;
  }

  public void setCameraDepth(double depth) {
    transformManager.setCameraDepth(depth);
  }

  public double getCameraDepth() {
    return transformManager.cameraDepth;
  }

  public int getCameraZ() {
    return transformManager.cameraZ;
  }

  /****************************************************************
   * delegated to ColorManager
   ****************************************************************/

  public void setModeAtomColorProfile(byte mode) {
    colorManager.setModeAtomColorProfile(mode);
    distributor.setColorAtom(mode, null, iterAtom());
    refresh();
  }

  public int getModeAtomColorProfile() {
    return colorManager.modeAtomColorProfile;
  }

  public void setColorOutline(Color c) {
    colorManager.setColorOutline(c);
    refresh();
  }

  public Color getColorOutline() {
    return colorManager.colorOutline;
  }

  public void setColorSelection(Color c) {
    colorManager.setColorSelection(c);
    refresh();
  }

  public Color getColorSelection() {
    return colorManager.getColorSelection();
  }

  public Color getColorRubberband() {
    return colorManager.colorRubberband;
  }

  public void setColorLabel(Color c) {
    colorManager.setColorLabel(c);
    refresh();
  }

  public Color getColorLabel() {
    return colorManager.colorLabel;
  }

  public void setColorDistance(Color c) {
    colorManager.setColorDistance(c);
    refresh();
  }

  public Color getColorDistance() {
    return colorManager.colorDistance;
  }

  public void setColorAngle(Color c) {
    colorManager.setColorAngle(c);
    refresh();
  }

  public Color getColorAngle() {
    return colorManager.colorAngle;
  }

  public void setColorDihedral(Color c) {
    colorManager.setColorDihedral(c);
    refresh();
  }
  public Color getColorDihedral() {
    return colorManager.colorDihedral;
  }

  public void setColorVector(Color c) {
    colorManager.setColorVector(c);
    refresh();
  }

  public Color getColorVector() {
    return colorManager.colorVector;
  }

  public void setColorBackground(Color bg) {
    colorManager.setColorBackground(bg);
    refresh();
  }

  public Color getColorBackground() {
    return colorManager.colorBackground;
  }
  
  public void setColorBackground(String colorName) {
    colorManager.setColorBackground(colorName);
    refresh();
  }

  /*
  public void setColorForeground(String colorName) {
    colorManager.setColorForeground(colorName);
  }
  */

  public Color getColorFromString(String colorName) {
    return colorManager.getColorFromString(colorName);
  }

  public Color getColorAtom(Atom atom) {
    return colorManager.getColorAtom(atom);
  }

  public Color getColorAtom(byte mode, Atom atom) {
    return colorManager.getColorAtom(mode, atom);
  }

  public Color getColorAtomOutline(byte style, Color color) {
    return colorManager.getColorAtomOutline(style, color);
  }

  public void setShowDarkerOutline(boolean showDarkerOutline) {
    colorManager.setShowDarkerOutline(showDarkerOutline);
    refresh();
  }

  public boolean getShowDarkerOutline() {
    return colorManager.showDarkerOutline;
  }

  public Color getDarker(Color color) {
    return colorManager.getDarker(color);
  }

  public void setModeTransparentColors(boolean modeTransparentColors) {
    colorManager.setModeTransparentColors(modeTransparentColors);
  }

  public Color getColorTransparent(Color color) {
    return colorManager.getColorTransparent(color);
  }

  // note that colorBond could be null -- meaning inherit atom color
  public void setColorBond(Color colorBond) {
    colorManager.setColorBond(colorBond);
    distributor.setColorBond(colorBond, iterBond());
    refresh();
  }

  public Color getColorBond() {
    return colorManager.colorBond;
  }

  public Color transparentRed() {
    return getColorTransparent(Color.red);
  }

  public Color transparentGreen() {
    return getColorTransparent(Color.green);
  }

  public Color transparentBlue() {
    return getColorTransparent(Color.blue);
  }

  public Color transparentGrey() {
    return getColorTransparent(Color.gray);
  }

  /****************************************************************
   * delegated to SelectionManager
   ****************************************************************/

  public void addSelection(int atomIndex) {
    selectionManager.addSelection(atomIndex);
    refresh();
  }

  public void addSelection(BitSet set) {
    selectionManager.addSelection(set);
    refresh();
  }

  public void toggleSelection(int atomIndex) {
    selectionManager.toggleSelection(atomIndex);
    refresh();
  }

  public boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  public boolean hasSelectionHalo(Atom atom) {
    return
      selectionHaloEnabled &&
      !repaintManager.fastRendering &&
      selectionManager.isSelected(atom.getAtomNumber());
  }

  public boolean hasSelectionHalo(Atom atom, int iatom) {
    if (!selectionHaloEnabled || repaintManager.fastRendering)
      return false;
    boolean isAtomSelected = isSelected(atom.getAtomNumber());
    if (bondSelectionModeOr && isAtomSelected)
      return true;
    if (!bondSelectionModeOr && !isAtomSelected)
      return false;
    Atom atomOther = atom.getBondedAtom(iatom);
    boolean isOtherSelected =
      selectionManager.isSelected(atomOther.getAtomNumber());
    return isOtherSelected;
  }

  public boolean selectionHaloEnabled = false;
  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    this.selectionHaloEnabled = selectionHaloEnabled;
    refresh();
  }
  
  public boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  boolean bondSelectionModeOr;
  public void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    this.bondSelectionModeOr = bondSelectionModeOr;
    refresh();
  }

  public boolean getBondSelectionModeOr() {
    return bondSelectionModeOr;
  }

  public void selectAll() {
    selectionManager.selectAll();
    refresh();
  }

  public void clearSelection() {
    selectionManager.clearSelection();
    refresh();
  }

  public void setSelectionSet(BitSet set) {
    selectionManager.setSelectionSet(set);
    refresh();
  }

  public void invertSelection() {
    selectionManager.invertSelection();
    // only used from a script, so I do not think a refresh() is necessary
  }

  public void excludeSelectionSet(BitSet set) {
    selectionManager.excludeSelectionSet(set);
    // only used from a script, so I do not think a refresh() is necessary
  }

  public BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  /****************************************************************
   * delegated to MouseManager
   ****************************************************************/
  public static final int ROTATE = 0;
  public static final int ZOOM = 1;
  public static final int XLATE = 2;
  public static final int PICK = 3;
  public static final int DELETE = 4;
  public static final int MEASURE = 5;
  public static final int DEFORM = 6; // mth -- what is this?
  public static final int ROTATE_Z = 7;
  public static final int SLAB_PLANE = 8;

  public void setModeMouse(int modeMouse) {
    mouseManager.setMode(modeMouse);
  }

  public int getModeMouse() {
    return mouseManager.modeMouse;
  }

  public Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  /****************************************************************
   * delegated to FileManager
   ****************************************************************/

  public void setAppletDocumentBase(URL base) {
    fileManager.setAppletDocumentBase(base);
  }

  public URL getURLFromName(String name) {
    return fileManager.getURLFromName(name);
  }

  public InputStream getInputStreamFromName(String name) {
    return fileManager.getInputStreamFromName(name);
  }

  public String openFile(String name) {
    return fileManager.openFile(name);
  }

  public String openFile(File file) {
    return fileManager.openFile(file);
  }

  public String openStringInline(String strModel) {
    return fileManager.openStringInline(strModel);
  }

  /****************************************************************
   * delegated to ModelManager
   ****************************************************************/

  public static final String PROP_CHEM_FILE = "chemFile";
  public static final String PROP_CHEM_FRAME = "chemFrame";

  public void setChemFile(ChemFile chemfile) {
    control.pushHoldRepaint();
    modelManager.setChemFile(chemfile);
    // FIXME mth -- allocate atom shapes in the right place
    distributor.initializeAtomShapes();
    homePosition();
    // don't know if I need this firm refresh here or not
    // FIXME mth -- we need to clear definitions when we open a new file
    // but perhaps not if we are in the midst of executing a script?
    eval.clearDefinitionsAndLoadPredefined();
    clearMeasurements();
    control.setStructuralChange();
    control.popHoldRepaint();
  }

  public ChemFile getChemFile() {
    return modelManager.getChemFile();
  }

  public String getModelName() {
    return modelManager.getModelName();
  }

  public boolean haveFile() {
    return modelManager.haveFile();
  }

  public ChemFrame getFrame() {
    return modelManager.chemframe;
  }

  public ChemFrame[] getFrames() {
    return modelManager.getFrames();
  }

  public double getRotationRadius() {
    return modelManager.getRotationRadius();
  }

  public Point3d getRotationCenter() {
    return modelManager.getRotationCenter();
  }

  public Point3d getBoundingBoxCenter() {
    return modelManager.getBoundingBoxCenter();
  }

  public Point3d getBoundingBoxCorner() {
    return modelManager.getBoundingBoxCorner();
  }

  // FIXME mth -- consolidate these two calls to setFrame

  public void setFrame(int fr) {
    modelManager.setFrame(fr);
    selectAll();
    recalcAxes();
    clearMeasurements();
    structuralChange = true;
    refresh();
  }

  public void setFrame(ChemFrame frame) {
    modelManager.setFrame(frame);
    selectAll();
    recalcAxes();
    clearMeasurements();
    structuralChange = true;
    refresh();
  }

  public int numberOfAtoms() {
    return modelManager.numberOfAtoms();
  }

  public Atom[] getCurrentFrameAtoms() {
    return modelManager.getCurrentFrameAtoms();
  }

  public int findNearestAtomIndex(int x, int y) {
    return modelManager.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
    return modelManager.findAtomsInRectangle(rectRubberBand);
  }

  public void setCenter(Point3d center) {
    modelManager.setRotationCenter(center);
  }

  public void setCenterAsSelected() {
    modelManager.setCenterAsSelected();
    selectAll();
    scaleFitToScreen();
    refresh();
  }

  public void rebond() {
    modelManager.rebond();
    refresh();
  }

  public void setBondFudge(double bf) {
    modelManager.setBondFudge(bf);
    refresh();
  }

  public double getBondFudge() {
    return modelManager.bondFudge;
  }

  public void setAutoBond(boolean ab) {
    modelManager.setAutoBond(ab);
    refresh();
  }

  public boolean getAutoBond() {
    return modelManager.autoBond;
  }

  public void deleteAtom(int atomIndex) {
    // FIXME mth -- deletion
    // after a delete operation, all the sets are messed up
    // the selection set *and* the script sets
    // selectionManager.delete(atomIndex);
    clearSelection();
    modelManager.deleteAtom(atomIndex);
    //            status.setStatus(2, "Atom deleted"); 
    selectAll();
    structuralChange = true;
    refresh();
  }

  public JmolAtomIterator getChemFileIterator() {
    return modelManager.getChemFileIterator();
  }

  public JmolAtomIterator getChemFrameIterator(BitSet set) {
    return modelManager.getChemFrameIterator(set);
  }

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    modelManager.addPropertyChangeListener(pcl);
  }

  public void addPropertyChangeListener(String prop,
                                        PropertyChangeListener pcl) {
    modelManager.addPropertyChangeListener(prop, pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    modelManager.removePropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(String prop,
                                           PropertyChangeListener pcl) {
    modelManager.removePropertyChangeListener(prop, pcl);
  }

  /****************************************************************
   * delegated to MeasurementManager
   ****************************************************************/

  public Vector getDistanceMeasurements() {
    return measurementManager.distanceMeasurements;
  }

  public Vector getAngleMeasurements() {
    return measurementManager.angleMeasurements;
  }

  public Vector getDihedralMeasurements() {
    return measurementManager.dihedralMeasurements;
  }

  public void clearMeasurements() {
    measurementManager.clearMeasurements();
  }

  public void defineMeasure(int[] atomIndices) {
    measurementManager.defineMeasure(atomIndices);
    refresh();
  }

  public void defineMeasure(int atom1, int atom2) {
    measurementManager.defineMeasure(atom1, atom2);
    refresh();
  }

  public void defineMeasure(int atom1, int atom2, int atom3) {
    measurementManager.defineMeasure(atom1, atom2, atom3);
    refresh();
  }

  public void defineMeasure(int atom1, int atom2, int atom3, int atom4) {
    measurementManager.defineMeasure(atom1, atom2, atom3, atom4);
    refresh();
  }

  MeasureWatcher measureWatcher;
  public void setMeasureWatcher(MeasureWatcher measureWatcher) {
    this.measureWatcher = measureWatcher;
  }

  public void measureSelection(int iatom) {
    if (measureWatcher != null)
      measureWatcher.firePicked(iatom);
  }

  /****************************************************************
   * delegated to RepaintManager
   ****************************************************************/

  public void setFastRendering(boolean fastRendering) {
    repaintManager.setFastRendering(fastRendering);
  }

  public boolean getFastRendering() {
    return repaintManager.fastRendering;
  }

  public void setInMotion(boolean inMotion) {
    repaintManager.setInMotion(inMotion);
  }

  public void setWantsGraphics2D(boolean wantsGraphics2D) {
    repaintManager.setWantsGraphics2D(wantsGraphics2D);
  }

  public boolean getWantsGraphics2D() {
    return repaintManager.wantsGraphics2D;
  }

  public boolean getUseGraphics2D() {
    return repaintManager.useGraphics2D;
  }

  public void setWantsAntialias(boolean wantsAntialias) {
    repaintManager.setWantsAntialias(wantsAntialias);
  }

  public boolean getWantsAntialias() {
    return repaintManager.wantsAntialias;
  }

  public void setWantsAntialiasAlways(boolean wantsAntialiasAlways) {
    repaintManager.setWantsAntialiasAlways(wantsAntialiasAlways);
  }

  public boolean getWantsAntialiasAlways() {
    return repaintManager.wantsAntialiasAlways;
  }

  public Image takeSnapshot() {
    return repaintManager.takeSnapshot();
  }

  public void pushHoldRepaint() {
    repaintManager.pushHoldRepaint();
  }

  public void popHoldRepaint() {
    repaintManager.popHoldRepaint();
  }

  public void refresh() {
    repaintManager.refresh();
  }

  public void requestRepaintAndWait() {
    repaintManager.requestRepaintAndWait();
  }

  public void notifyRepainted() {
    repaintManager.notifyRepainted();
  }

  public void render(Graphics g, Rectangle rectClip) {
    repaintManager.render(g, rectClip);
  }

  /****************************************************************
   * routines for java12
   ****************************************************************/
  
  private void maybeEnableAntialiasing(Graphics g) {
    if (repaintManager.useGraphics2D)
      java12.enableAntialiasing(g,
                                repaintManager.wantsAntialias &&
                                (!repaintManager.inMotion ||
                                 repaintManager.wantsAntialiasAlways));
  }

  public void maybeDottedStroke(Graphics g) {
    if (repaintManager.useGraphics2D)
      java12.dottedStroke(g);
  }

  /****************************************************************
   * routines for script support
   ****************************************************************/

  public Eval getEval() {
    return eval;
  }

  public String evalFile(String strFilename) {
    if (strFilename != null) {
      if (! eval.loadFile(strFilename))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String eval(String strScript) {
    if (strScript != null) {
      if (! eval.loadString(strScript))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public void scriptEcho(String str) {
    // FIXME -- if there is a script window it should go there
    // for an applet it needs to go someplace else
    System.out.println(str);
  }

  public void setStyleMarAtomScript(byte style, short mar) {
    distributor.setStyleMarAtom(style, mar, iterAtom());
  }

  public void setStyleAtomScript(byte style) {
    distributor.setStyleAtom(style, iterAtom());
  }

  public void setStyleMarBondScript(byte style, short mar) {
    distributor.setStyleMarBond(style, mar, iterBond());
  }

  public void setStyleBondScript(byte style) {
    distributor.setStyleBond(style, iterBond());
  }

  public void setColorAtomScript(byte mode, Color color) {
    distributor.setColorAtom(mode, color, iterAtom());
  }

  public void setColorBondScript(Color color) {
    distributor.setColorBond(color, iterBond());
  }

  public void setLabelScript(String strLabel) {
    distributor.setLabel(strLabel, iterAtom());
  }

  boolean rasmolHydrogenSetting = true;
  public void setRasmolHydrogenSetting(boolean b) {
    rasmolHydrogenSetting = b;
  }
  
  public boolean getRasmolHydrogenSetting() {
    return rasmolHydrogenSetting;
  }

  boolean rasmolHeteroSetting = true;
  public void setRasmolHeteroSetting(boolean b) {
    rasmolHeteroSetting = b;
  }
  
  public boolean getRasmolHeteroSetting() {
    return rasmolHeteroSetting;
  }

  /****************************************************************
   * delegated to StyleManager
   ****************************************************************/

  /*
   * for rasmol compatibility with continued menu operation:
   *  - if it is from the menu & nothing selected
   *    * set the setting
   *    * apply to all
   *  - if it is from the menu and something is selected
   *    * apply to selection
   *  - if it is from a script
   *    * apply to selection
   *    * possibly set the setting for some things
   */

  JmolAtomIterator iterNull = new JmolAtomIterator();
  private JmolAtomIterator iterAtom() {
    if (! modelManager.haveFile || selectionManager.isEmpty())
      return iterNull;
    return modelManager.getChemFrameIterator(selectionManager.bsSelection);
  }

  private JmolAtomIterator iterBond() {
    if (!modelManager.haveFile || selectionManager.isEmpty())
      return iterNull;
    return modelManager.getChemFrameIterator(selectionManager.bsSelection,
                                             bondSelectionModeOr);
  }


  public void setStyleAtom(byte style) {
    styleManager.setStyleAtom(style);
    distributor.setStyleAtom(style, iterAtom());
    refresh();
  }

  public byte getStyleAtom() {
    return styleManager.styleAtom;
  }

  public void setPercentVdwAtom(int percentVdwAtom) {
    styleManager.setPercentVdwAtom(percentVdwAtom);
    distributor.setMarAtom((short)-percentVdwAtom, iterAtom());
    refresh();
  }

  public int getPercentVdwAtom() {
    return styleManager.percentVdwAtom;
  }

  public short getMarAtom() {
    return (short)-styleManager.percentVdwAtom;
  }

  public void setStyleBond(byte style) {
    styleManager.setStyleBond(style);
    distributor.setStyleBond(style, iterBond());
    refresh();
  }

  public byte getStyleBond() {
    return styleManager.styleBond;
  }

  public void setMarBond(short marBond) {
    styleManager.setMarBond(marBond);
    distributor.setMarBond(marBond, iterBond());
    refresh();
  }

  public short getMarBond() {
    return styleManager.marBond;
  }

  public final static byte MB_NEVER =     0;
  public final static byte MB_WIREFRAME = 1;
  public final static byte MB_SMALL =     2;
  public final static byte MB_ALWAYS =    3;

  public final static short marMultipleBondSmallMaximum = 128;

  public void setModeMultipleBond(byte modeMultipleBond) {
    styleManager.setModeMultipleBond(modeMultipleBond);
    refresh();
  }

  public byte getModeMultipleBond() {
    return styleManager.modeMultipleBond;
  }

  public void setShowMultipleBonds(boolean showMultipleBonds) {
    styleManager.setShowMultipleBonds(showMultipleBonds);
    refresh();
  }

  public boolean getShowMultipleBonds() {
    return styleManager.showMultipleBonds;
  }

  public void setShowAtoms(boolean showAtoms) {
    styleManager.setShowAtoms(showAtoms);
    refresh();
  }

  public boolean getShowAtoms() {
    return styleManager.showAtoms;
  }

  public void setShowBonds(boolean showBonds) {
    styleManager.setShowBonds(showBonds);
    refresh();
  }

  public boolean getShowBonds() {
    return styleManager.showBonds;
  }

  public void setShowHydrogens(boolean showHydrogens) {
    styleManager.setShowHydrogens(showHydrogens);
    refresh();
  }

  public boolean getShowHydrogens() {
    return styleManager.showHydrogens;
  }

  public void setShowVectors(boolean showVectors) {
    styleManager.setShowVectors(showVectors);
    refresh();
  }

  public boolean getShowVectors() {
    return styleManager.showVectors;
  }

  public void setShowMeasurements(boolean showMeasurements) {
    styleManager.setShowMeasurements(showMeasurements);
    refresh();
  }

  public boolean getShowMeasurements() {
    return styleManager.showMeasurements;
  }

  public void setShowMeasurementLabels(boolean showMeasurementLabels) {
    styleManager.setShowMeasurementLabels(showMeasurementLabels);
    refresh();
  }

  public boolean getShowMeasurementLabels() {
    return styleManager.showMeasurementLabels;
  }


  public Font getMeasureFont(int size) {
    return styleManager.getMeasureFont(size);
  }

  public void setPropertyStyleString(String s) {
    styleManager.setPropertyStyleString(s);
    refresh();
  }

  public String getPropertyStyleString() {
    return styleManager.propertyStyleString;
  }

  public void setWireframeRotation(boolean wireframeRotation) {
    styleManager.setWireframeRotation(wireframeRotation);
    // no need to refresh since we are not currently rotating
  }

  public boolean getWireframeRotation() {
    return styleManager.wireframeRotation;
  }

  public void setArrowHeadSize(double ls) {
    styleManager.setArrowHeadSize(ls);
    refresh();
  }

  public double getArrowHeadSize() {
    return styleManager.getArrowHeadSize();
  }

  public double getArrowHeadSize10() {
    return styleManager.getArrowHeadSize10();
  }

  public void setArrowLengthScale(double ls) {
    styleManager.setArrowLengthScale(ls);
    refresh();
  }

  public double getArrowLengthScale() {
    return styleManager.arrowLengthScale;
  }

  public void setArrowHeadRadius(double rs) {
    styleManager.setArrowHeadRadius(rs);
    refresh();
  }

  public double getArrowHeadRadius() {
    return styleManager.arrowHeadRadius;
  }

  public boolean getDebugShowAxis() {
    return false;
  }

  /****************************************************************
   * delegated to LabelManager
   ****************************************************************/

  public void setStyleLabel(byte style) {
    labelManager.setStyleLabel(style);
    distributor.setStyleLabel(style, iterAtom());
    refresh();
  }

  public byte getStyleLabel() {
    return labelManager.styleLabel;
  }

  public String getLabelAtom(Atom atom) {
    return labelManager.getLabelAtom(labelManager.styleLabel, atom);
  }

  public String getLabelAtom(byte styleLabel, Atom atom) {
    return labelManager.getLabelAtom(styleLabel, atom);
  }

  public String getLabelAtom(String strLabel, Atom atom) {
    return labelManager.getLabelAtom(strLabel, atom);
  }

  public void setLabelFontSize(int points) {
    labelManager.setLabelFontSize(points);
    refresh();
  }

  public Font getLabelFont(int diameter) {
    return labelManager.getLabelFont(diameter);
  }

  public Font getFontOfSize(int points) {
    return labelManager.getFontOfSize(points);
  }
  
  public void renderStringOffset(String str, Color color, int points,
                                 int x, int y, int xOffset, int yOffset) {
    labelRenderer.renderStringOffset(str, color, points,
                                     x, y, xOffset, yOffset);
  }

  /****************************************************************
   * delegated to AxesManager
   ****************************************************************/

  public final static byte AXES_NONE = 0;
  public final static byte AXES_UNIT = 1;
  public final static byte AXES_BBOX = 2;

  public void setShowAxes(boolean showAxes) {
    setModeAxes(showAxes ? AXES_BBOX : AXES_NONE);
  }

  public boolean getShowAxes() {
    return axesManager.modeAxes != AXES_NONE;
  }

  public void setModeAxes(byte modeAxes) {
    axesManager.setModeAxes(modeAxes);
    structuralChange = true;
    refresh();
  }

  public byte getModeAxes() {
    return axesManager.modeAxes;
  }

  public void recalcAxes() {
    axesManager.recalc();
  }

  public Axes getAxes() {
    return axesManager.axes;
  }

  public void setShowBoundingBox(boolean showBoundingBox) {
    axesManager.setShowBoundingBox(showBoundingBox);
    structuralChange = true;
    refresh();
  }

  public boolean getShowBoundingBox() {
    return axesManager.showBoundingBox;
  }

  public BoundingBox getBoundingBox() {
    return axesManager.bbox;
  }

  public Color getColorAxes() {
    return axesManager.colorAxes;
  }

  public Color getColorAxesText() {
    return axesManager.colorAxesText;
  }
}
