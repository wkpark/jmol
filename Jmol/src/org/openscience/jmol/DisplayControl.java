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
//import org.openscience.jmol.render.ChemFrameRenderer;
import org.openscience.jmol.render.JmolFrame;
//import org.openscience.jmol.render.MeasureRenderer;
import org.openscience.jmol.render.Axes;
import org.openscience.jmol.render.BoundingBox;
import org.openscience.jmol.render.AtomShape;
import org.openscience.jmol.render.MeasurementShape;
import org.openscience.jmol.render.AtomShapeIterator;
import org.openscience.jmol.render.BondShape;
import org.openscience.jmol.render.BondShapeIterator;
import org.openscience.jmol.render.JmolAtom;
import org.openscience.jmol.script.Eval;
import org.openscience.jmol.g25d.Graphics25D;
import org.openscience.jmol.g25d.Colix;

import java.awt.MenuItem;
import java.awt.PopupMenu;

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;
import java.net.URL;
import java.io.InputStream;
import java.io.File;
import java.beans.PropertyChangeListener;
import java.awt.event.MouseEvent;

final public class DisplayControl {

  //  public static DisplayControl control;

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
  //  public ChemFrameRenderer frameRenderer;
  //  public MeasureRenderer measureRenderer;
  public AtomRenderer atomRenderer;
  public BondRenderer bondRenderer;
  public LabelRenderer labelRenderer;
  public Distributor distributor;
  public Eval eval;
  public Java12 java12;
  public Graphics25D g25d;

  public String strJvmVersion;
  public boolean jvm12orGreater = false;
  public boolean jvm14orGreater = false;

  public DisplayControl(String strJvmVersion, Component awtComponent) {

    this.awtComponent = awtComponent;
    this.strJvmVersion = strJvmVersion;
    jvm12orGreater = (strJvmVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJvmVersion.compareTo("1.4") >= 0);

    //    control = this;
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

    //    frameRenderer = new ChemFrameRenderer();
    //    measureRenderer = new MeasureRenderer();
    atomRenderer = new AtomRenderer(this);
    bondRenderer = new BondRenderer(this);
    labelRenderer = new LabelRenderer(this);

    eval = new Eval(this);
    if (jvm12orGreater)
      java12 = new Java12(this);

    g25d = new Graphics25D(this);
  
    // System.out.println("New DisplayControl");
  }

  public Component getAwtComponent() {
    return awtComponent;
  }

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

  public void setGraphicsContext(Graphics25D g25d, Rectangle rectClip) {
    atomRenderer.setGraphicsContext(g25d, rectClip);
    bondRenderer.setGraphicsContext(g25d, rectClip);
    labelRenderer.setGraphicsContext(g25d, rectClip);
    maybeEnableAntialiasing(g25d);
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
    rotateToX(toRadians(angleDegrees));
  }
  public void rotateToY(int angleDegrees) {
    rotateToY(toRadians(angleDegrees));
  }
  public void rotateToZ(int angleDegrees) {
    rotateToZ(toRadians(angleDegrees));
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
    rotateByX(toRadians(angleDegrees));
  }
  public void rotateByY(int angleDegrees) {
    rotateByY(toRadians(angleDegrees));
  }
  public void rotateByZ(int angleDegrees) {
    rotateByZ(toRadians(angleDegrees));
  }

  public static double toRadians(int degrees) {
    return degrees / 360.0 * 2 * Math.PI;
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

  /*
  public void transformPoint(Point3d pointAngstroms, Point3d pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }
  */

  public Point3i transformPoint(Point3d pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public double scaleToScreen(int z, double sizeAngstroms) {
    return transformManager.scaleToScreen(z, sizeAngstroms);
  }

  public int scaleToScreen(int z, int milliAngstroms) {
    return transformManager.scaleToScreen(z, milliAngstroms);
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

  private Dimension dimCurrent;

  public void setScreenDimension(Dimension dim) {
    if (dim.equals(dimCurrent))
      return;
    dimCurrent = dim;
    transformManager.setScreenDimension(dim.width, dim.height);
    transformManager.scaleFitToScreen();
    g25d.setSize(dim.width, dim.height);
  }

  public Dimension getScreenDimension() {
    return dimCurrent;
  }

  /****************************************************************
   * delegated to ColorManager
   ****************************************************************/

  public void setModeAtomColorProfile(byte mode) {
    colorManager.setModeAtomColorProfile(mode);
    distributor.setColixAtom(mode, Colix.NULL, atomIteratorSelected());
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
  public short getColixSelection() {
    return colorManager.getColixSelection();
  }

  public short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  public void setColorLabel(Color color) {
    colorManager.setColorLabel(color);
    refresh();
  }

  public void setColorDots(Color color) {
    colorManager.setColorDots(color);
  }

  public Color getColorLabel() {
    return colorManager.colorLabel;
  }

  public short getColixLabel() {
    return colorManager.colixLabel;
  }

  public void setColorDistance(Color c) {
    colorManager.setColorDistance(c);
    refresh();
  }

  public Color getColorDistance() {
    return colorManager.colorDistance;
  }

  public short getColixDistance() {
    return colorManager.colixDistance;
  }

  public void setColorAngle(Color c) {
    colorManager.setColorAngle(c);
    refresh();
  }

  public Color getColorAngle() {
    return colorManager.colorAngle;
  }

  public short getColixAngle() {
    return colorManager.colixAngle;
  }

  public void setColorDihedral(Color c) {
    colorManager.setColorDihedral(c);
    refresh();
  }
  public Color getColorDihedral() {
    return colorManager.colorDihedral;
  }

  public short getColixDihedral() {
    return colorManager.colixDihedral;
  }

  public void setColorVector(Color c) {
    colorManager.setColorVector(c);
    refresh();
  }

  public Color getColorVector() {
    return colorManager.colorVector;
  }

  public short getColixVector() {
    return colorManager.colixVector;
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

  public Color getColorFromString(String colorName) {
    return colorManager.getColorFromString(colorName);
  }

  public short getColixAtom(JmolAtom atom) {
    return colorManager.getColixAtom((Atom)atom);
  }

  public short getColixAtom(Atom atom) {
    return colorManager.getColixAtom(atom);
  }

  public short getColixAtom(byte mode, Atom atom) {
    return colorManager.getColixAtom(mode, atom);
  }

  public short getColixAtom(byte mode, JmolAtom atom) {
    return colorManager.getColixAtom(mode, (Atom)atom);
  }

  public Color getColorAtomOutline(byte style, Color color) {
    return colorManager.getColorAtomOutline(style, color);
  }

  public short getColixAtomOutline(byte style, short colix) {
    return colorManager.getColixAtomOutline(style, colix);
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

  public short getColixTransparent(short colix) {
    return colorManager.getColixTransparent(colix);
  }

  // note that colorBond could be null -- meaning inherit atom color
  public void setColorBond(Color colorBond) {
    colorManager.setColorBond(colorBond);
    distributor.setColix(Colix.getColix(colorBond),
                         bondIteratorSelected(BondShape.COVALENT));
    refresh();
  }

  public short getColixBond() {
    return colorManager.colixBond;
  }

  public Color getColorBond() {
    return colorManager.colorBond;
  }

  public short transparentRed() {
    return getColixTransparent(Colix.RED);
  }

  public short transparentGreen() {
    return getColixTransparent(Colix.GREEN);
  }

  public short transparentBlue() {
    return getColixTransparent(Colix.BLUE);
  }

  public short transparentGrey() {
    return getColixTransparent(Colix.GRAY);
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

  public boolean isSelected(AtomShape atomShape) {
    return isSelected(atomShape.getAtomIndex());
  }

  public boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  public boolean hasSelectionHalo(AtomShape atomShape) {
    return
      selectionHaloEnabled &&
      !repaintManager.fastRendering &&
      isSelected(atomShape);
  }

  /*
  public boolean hasBondSelectionHalo(AtomShape atomShape, int bondIndex) {
    if (!selectionHaloEnabled || repaintManager.fastRendering)
      return false;
    boolean isAtomSelected = isSelected(atomShape);
    if (bondSelectionModeOr && isAtomSelected)
      return true;
    if (!bondSelectionModeOr && !isAtomSelected)
      return false;
    // FIXME 
    int atomIndexOther = atomShape.getBondedAtomIndex(bondIndex);
    boolean isOtherSelected =
      selectionManager.isSelected(atomIndexOther);
    return isOtherSelected;
  }
  */

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
  public static final int POPUP_MENU = 9;

  public void setModeMouse(int modeMouse) {
    mouseManager.setMode(modeMouse);
  }

  public int getModeMouse() {
    return mouseManager.modeMouse;
  }

  public Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  public void popupMenu(MouseEvent e) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(e);
  }

  private MenuItem makeMenuItem(String id) {
    MenuItem mi = new MenuItem(id);
    return mi;
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
    pushHoldRepaint();
    modelManager.setChemFile(chemfile);
    homePosition();
    // don't know if I need this firm refresh here or not
    // FIXME mth -- we need to clear definitions when we open a new file
    // but perhaps not if we are in the midst of executing a script?
    eval.clearDefinitionsAndLoadPredefined();
    clearMeasurements();
    setStructuralChange();
    popHoldRepaint();
  }

  public void clear() {
    modelManager.setChemFile(null);
    clearMeasurements();
    refresh();
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

  public JmolFrame getJmolFrame() {
    return modelManager.getJmolFrame();
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

  public int getBoundingBoxCenterX() {
    // FIXME mth 2003 05 31
    // for now this is returning the center of the screen
    // need to transform the center of the bounding box and return that point
    return dimCurrent.width / 2;
  }

  public int getBoundingBoxCenterY() {
    return dimCurrent.height / 2;
  }

  // FIXME mth -- consolidate these two calls to setFrame

  public int getNumberOfFrames() {
    return modelManager.getNumberOfFrames();
  }

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

  public int getCurrentFrameNumber() {
    return modelManager.getCurrentFrameNumber();
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
    structuralChange = true;
    refresh();
  }

  public void setBondFudge(double bf) {
    modelManager.setBondFudge(bf);
    refresh();
  }

  public double getBondFudge() {
    return modelManager.bondFudge;
  }

  public void setBondTolerance(double bondTolerance) {
    modelManager.setBondTolerance(bondTolerance);
    refresh();
  }

  public double getBondTolerance() {
    return modelManager.bondTolerance;
  }

  public void setMinBondDistance(double minBondDistance) {
    modelManager.setMinBondDistance(minBondDistance);
    refresh();
  }

  public double getMinBondDistance() {
    return modelManager.minBondDistance;
  }

  public void setAutoBond(boolean ab) {
    modelManager.setAutoBond(ab);
    refresh();
  }

  public boolean getAutoBond() {
    return modelManager.autoBond;
  }

  public void deleteAtom(int atomIndex) {
    // FIXME mth -- after a delete operation, all the sets are messed up
    clearSelection();
    clearMeasurements();
    eval.clearDefinitionsAndLoadPredefined();
    //
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

  public boolean deleteMeasurement(MeasurementShape measurement) {
    return measurementManager.deleteMeasurement(measurement);
  }

  public boolean deleteMatchingMeasurement(int atom1, int atom2) {
    return measurementManager.deleteMatchingMeasurement(atom1, atom2);
  }

  public boolean deleteMatchingMeasurement(int atom1, int atom2, int atom3) {
    return measurementManager.deleteMatchingMeasurement(atom1, atom2, atom3);
  }

  public boolean deleteMatchingMeasurement(int atom1, int atom2,
                                           int atom3, int atom4) {
    return measurementManager.deleteMatchingMeasurement(atom1, atom2,
                                                        atom3, atom4);
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
    checkOversample();
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

  public Image renderScreenImage(Rectangle rectClip) {
    if (eval.hasTerminationNotification())
      manageScriptTermination();
    repaintManager.render(g25d, rectClip);
    return g25d.getScreenImage();
  }

  public Image getScreenImage() {
    return g25d.getScreenImage();
  }

  public void checkOversample() {
    boolean tOversample = g25d.tEnabled &
      (tOversampleAlways | (!repaintManager.inMotion & tOversampleStopped));
    repaintManager.setOversample(tOversample);
    transformManager.setOversample(tOversample);
  }

  public void setOversample(boolean tOversample) {
    transformManager.setOversample(tOversample);
    repaintManager.setOversample(tOversample);
  }

  /****************************************************************
   * routines for java12
   ****************************************************************/
  
  private void maybeEnableAntialiasing(Graphics25D g25d) {
    if (repaintManager.useGraphics2D)
      g25d.enableAntialiasing(repaintManager.enableAntialiasing());
  }

  public void maybeDottedStroke(Graphics25D g25d) {
    if (repaintManager.useGraphics2D)
      g25d.dottedStroke();
  }

  public void defaultStroke(Graphics25D g25d) {
    if (repaintManager.useGraphics2D)
      g25d.defaultStroke();
  }

  /****************************************************************
   * routines for script support
   ****************************************************************/

  public Eval getEval() {
    return eval;
  }

  public String evalFile(String strFilename) {
    if (strFilename != null) {
      if (! eval.loadScriptFile(strFilename))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalString(String strScript) {
    if (strScript != null) {
      if (! eval.loadScriptString(strScript))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public void haltScriptExecution() {
    eval.haltExecution();
  }

  public void setStyleMarAtomScript(byte style, short mar) {
    distributor.setStyleMarAtom(style, mar, atomIteratorSelected());
  }

  public void setStyleAtomScript(byte style) {
    distributor.setStyleAtom(style, atomIteratorSelected());
  }

  public void setStyleMarBondScript(byte style, short mar) {
    distributor.setStyleMar(style, mar,
                            bondIteratorSelected(BondShape.COVALENT));
  }

  public void setStyleMarBackboneScript(byte style, short mar) {
    distributor.setStyleMar(style, mar,
                            bondIteratorSelected(BondShape.BACKBONE));
  }

  public void setStyleBondScript(byte style) {
    distributor.setStyle(style, bondIteratorSelected(BondShape.COVALENT));
  }

  public void setStyleBackboneScript(byte style) {
    distributor.setStyle(style, bondIteratorSelected(BondShape.BACKBONE));
  }

  public void setColorAtomScript(byte mode, Color color) {
    distributor.setColixAtom(mode, Colix.getColix(color),
                             atomIteratorSelected());
  }

  public void setColorBondScript(Color color) {
    distributor.setColix(Colix.getColix(color),
                         bondIteratorSelected(BondShape.COVALENT));
  }

  public void setColorBackboneScript(Color color) {
    distributor.setColix(Colix.getColix(color),
                         bondIteratorSelected(BondShape.BACKBONE));
  }

  public void setLabelScript(String strLabel) {
    distributor.setLabel(strLabel, atomIteratorSelected());
  }

  public void setMarDots(short marDots) {
    distributor.setColixMarDots(colorManager.colixDots, marDots,
                                atomIteratorSelected());
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

  JmolStatusListener jmolStatusListener;

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    this.jmolStatusListener = jmolStatusListener;
  }

  public void manageScriptTermination() {
    String strErrorMessage = eval.getErrorMessage();
    int msWalltime = eval.getExecutionWalltime();
    eval.resetTerminationNotification();
    if (jmolStatusListener != null)
      jmolStatusListener.notifyScriptTermination(strErrorMessage, msWalltime);
    }

  public void scriptEcho(String strEcho) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptEcho(strEcho);
  }

  public void scriptStatus(String strStatus) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptStatus(strStatus);
  }

  /****************************************************************
   * mth 2003 05 31 - needs more work
   * this should be implemented using properties
   * or as a hashtable using boxed primitive types so that the
   * boxed values could be shared
   ****************************************************************/

  public boolean getBooleanProperty(String key) {
    if (key.equals("wireframeRotation"))
      return getWireframeRotation();
    if (key.equals("perspectiveDepth"))
      return getPerspectiveDepth();
    if (key.equals("showAxes"))
      return getShowAxes();
    if (key.equals("showBoundingBox"))
      return getShowBoundingBox();
    if (key.equals("showHydrogens"))
      return getShowHydrogens();
    if (key.equals("showVectors"))
      return getShowVectors();
    if (key.equals("showMeasurements"))
      return getShowMeasurements();
    if (key.equals("showSelections"))
      return getSelectionHaloEnabled();
    if (key.equals("oversampleAlways"))
      return getOversampleAlwaysEnabled();
    if (key.equals("oversampleStopped"))
      return getOversampleStoppedEnabled();
    System.out.println("control.getBooleanProperty(" +
                       key + ") - unrecognized");
    return false;
  }

  public void setBooleanProperty(String key, boolean value) {
    if (key.equals("wireframeRotation"))
      { setWireframeRotation(value); return ; }
    if (key.equals("perspectiveDepth"))
      { setPerspectiveDepth(value); return ; }
    if (key.equals("showAxes"))
      { setShowAxes(value); return ; }
    if (key.equals("showBoundingBox"))
      { setShowBoundingBox(value); return ; }
    if (key.equals("showHydrogens"))
      { setShowHydrogens(value); return ; }
    if (key.equals("showHydrogens"))
      { setShowHydrogens(value); return ; }
    if (key.equals("showVectors"))
      { setShowVectors(value); return ; }
    if (key.equals("showMeasurements"))
      { setShowMeasurements(value); return ; }
    if (key.equals("showSelections"))
      { setSelectionHaloEnabled(value); return ; }
    if (key.equals("oversampleAlways"))
      { setOversampleAlwaysEnabled(value); return; }
    if (key.equals("oversampleStopped"))
      { setOversampleStoppedEnabled(value); return; }
    System.out.println("control.setBooleanProperty(" +
                       key + "," + value + ") - unrecognized");
  }

  /****************************************************************
   * Graphics25D
   ****************************************************************/

  public boolean getGraphics25DEnabled() {
    return g25d.tEnabled;
  }

  public void setGraphics25DEnabled(boolean value) {
    g25d.setEnabled(value);
    refresh();
  }

  boolean tOversampleStopped;
  public boolean getOversampleStoppedEnabled() {
    return tOversampleStopped;
  }
  boolean tOversampleAlways;
  public boolean getOversampleAlwaysEnabled() {
    return tOversampleAlways;
  }

  public void setOversampleAlwaysEnabled(boolean value) {
    tOversampleAlways = value;
    checkOversample();
    refresh();
  }

  public void setOversampleStoppedEnabled(boolean value) {
    tOversampleStopped = value;
    checkOversample();
    refresh();
  }

  /****************************************************************
   * JmolFrame
   ****************************************************************/

  private AtomShapeIterator atomIteratorSelected() {
    return getJmolFrame().getAtomIterator(selectionManager.bsSelection);
  }

  private BondShapeIterator bondIteratorSelected(byte bondType) {
    return
      getJmolFrame().getBondIterator(bondType, selectionManager.bsSelection);
  }

  final AtomShapeIterator nullAtomShapeIterator =
    new NullAtomShapeIterator();

  class NullAtomShapeIterator implements AtomShapeIterator {
    public boolean hasNext() { return false; }
    public AtomShape next() { return null; }
  }

  final BondShapeIterator nullBondShapeIterator =
    new NullBondShapeIterator();

  class NullBondShapeIterator implements BondShapeIterator {
    public boolean hasNext() { return false; }
    public BondShape next() { return null; }
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

  public void setStyleAtom(byte style) {
    styleManager.setStyleAtom(style);
    distributor.setStyleAtom(style, atomIteratorSelected());
    refresh();
  }

  public byte getStyleAtom() {
    return styleManager.styleAtom;
  }

  public void setPercentVdwAtom(int percentVdwAtom) {
    styleManager.setPercentVdwAtom(percentVdwAtom);
    distributor.setMarAtom((short)-percentVdwAtom, atomIteratorSelected());
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
    distributor.setStyle(style, bondIteratorSelected(BondShape.COVALENT));
    refresh();
  }

  public byte getStyleBond() {
    return styleManager.styleBond;
  }

  public void setMarBond(short marBond) {
    styleManager.setMarBond(marBond);
    distributor.setMar(marBond, bondIteratorSelected(BondShape.COVALENT));
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
    setStructuralChange();
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
    distributor.setStyleLabel(style, atomIteratorSelected());
    refresh();
  }

  public byte getStyleLabel() {
    return labelManager.styleLabel;
  }

  public String getLabelAtom(JmolAtom atom) {
    return labelManager.getLabelAtom(labelManager.styleLabel, (Atom)atom);
  }

  public String getLabelAtom(Atom atom) {
    return labelManager.getLabelAtom(labelManager.styleLabel, atom);
  }

  public String getLabelAtom(byte styleLabel, Atom atom) {
    return labelManager.getLabelAtom(styleLabel, atom);
  }

  public String getLabelAtom(byte styleLabel, JmolAtom atom) {
    return labelManager.getLabelAtom(styleLabel, (Atom)atom);
  }

  public String getLabelAtom(String strLabel, Atom atom) {
    return labelManager.getLabelAtom(strLabel, atom);
  }

  public String getLabelAtom(String strLabel, JmolAtom atom) {
    return labelManager.getLabelAtom(strLabel, (Atom)atom);
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
  
  public void renderStringOffset(String str, short colix, int points,
                                 int x, int y, int z,
                                 int xOffset, int yOffset) {
    labelRenderer.renderStringOffset(str, colix, points,
                                     x, y, z, xOffset, yOffset);
  }

  public void renderStringOutside(String str, short colix, int pointsFontsize,
                                  int x, int y, int z) {
    labelRenderer.renderStringOutside(str, colix, pointsFontsize, x, y, z);
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

  public short getColixAxes() {
    return axesManager.colixAxes;
  }

  public short getColixAxesText() {
    return axesManager.colixAxesText;
  }

  /****************************************************************
   * Jmol Adapter routines - for now they are here
   ****************************************************************/

  public int getAtomicNumber(JmolAtom atom) {
    return atom.getAtomicNumber();
  }

  public double getVanderwaalsRadius(JmolAtom atom) {
    return atom.getVanderwaalsRadius();
  }

  public Point3d getPoint3d(JmolAtom atom) {
    return atom.getPoint3D();
  }
}
