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

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;

import java.net.URL;
import java.io.InputStream;

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
  public AtomRenderer atomRenderer;
  public BondRenderer bondRenderer;

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

    atomRenderer = new AtomRenderer(this);
    bondRenderer = new BondRenderer(this);
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
  public final static byte NONE =      3;
  public final static byte BOX =       4;

  public final static byte ATOMTYPE =   0;
  public final static byte ATOMCHARGE = 1;

  public void homePosition() {
    // FIXME -- need to hold repaint during this process, but first 
    // figure out the interaction with the current holdRepaint setting
    setCenter(null);
    clearSelection();
    transformManager.homePosition();
    refresh();
  }

  public void setGraphicsContext(Graphics g, Rectangle rectClip) {
    atomRenderer.setGraphicsContext(g, rectClip);
    bondRenderer.setGraphicsContext(g, rectClip);
    maybeEnableAntialiasing(g);
  }

  public boolean hasStructuralChange() {
    return structuralChange;
  }

  public void resetStructuralChange() {
    structuralChange = false;
  }

  public final Hashtable imageCache = new Hashtable();
  public void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }

  public void scriptEcho(String str) {
    // FIXME -- if there is a script window it should go there
    // for an applet it needs to go someplace else
    System.out.println(str);
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

  public int screenAtomDiameter(int z, Atom atom) {
    return transformManager.screenAtomDiameter(z, atom,
                                               styleManager.percentVdwAtom);
  }

  public int screenBondWidth(int z) {
    return transformManager.screenBondWidth(z,
                                            styleManager.percentAngstromBond);
  }

  public double scaleToScreen(int z, double sizeAngstroms) {
    return transformManager.scaleToScreen(z, sizeAngstroms);
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

  public void setModeAtomColorProfile(int mode) {
    colorManager.setModeAtomColorProfile(mode);
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

  public void setColorText(Color c) {
    colorManager.setColorText(c);
    refresh();
  }
  public Color getColorText() {
    return colorManager.colorText;
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
  }

  public void setColorForeground(String colorName) {
    colorManager.setColorForeground(colorName);
  }

  public Color getColorFromHexString(String colorName) {
    return colorManager.getColorFromHexString(colorName);
  }
  public Color getColorAtom(Atom atom) {
    return colorManager.getColorAtom(atom);
  }

  public Color getColorAtomOutline(Color color) {
    return colorManager.getColorAtomOutline(color);
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

  public int countSelection() {
    return selectionManager.countSelection();
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

  public void setMeasureMouse(Measure measure) {
    mouseManager.setMeasure(measure);
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

  /****************************************************************
   * delegated to ModelManager
   ****************************************************************/

  public void setChemFile(ChemFile chemfile) {
    modelManager.setChemFile(chemfile);
    styleManager.initializeAtomShapes();
    homePosition();
    // don't know if I need this firm refresh here or not
    refreshFirmly();
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

  public void setFrame(int fr) {
    modelManager.setFrame(fr);
    structuralChange = true;
    clearSelection();
    refresh();
  }

  public void setFrame(ChemFrame frame) {
    modelManager.setFrame(frame);
    structuralChange = true;
    clearSelection();
    refresh();
  }

  public int numberOfAtoms() {
    return modelManager.numberOfAtoms();
  }

  public Atom[] getCurrentFrameAtoms() {
    return modelManager.getCurrentFrameAtoms();
  }

  public void mlistChanged(MeasurementListEvent mle) {
    modelManager.mlistChanged(mle);
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
    clearSelection();
    scaleFitToScreen();
    refresh();
  }

  public void defineMeasure(int atom1, int atom2) {
    modelManager.defineMeasure(atom1, atom2);
    refresh();
  }

  public void defineMeasure(int atom1, int atom2, int atom3) {
    modelManager.defineMeasure(atom1, atom2, atom3);
    refresh();
  }

  public void defineMeasure(int atom1, int atom2, int atom3, int atom4) {
    modelManager.defineMeasure(atom1, atom2, atom3, atom4);
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
    // FIXME -- there are problems with deleting atoms
    // for example, the selection set gets messed up
    // The answer is that delete does *not* delete, it only *hides*
    // in fact, this is completely compatible with rasmol
    modelManager.deleteAtom(atomIndex);
    //            status.setStatus(2, "Atom deleted"); 
    structuralChange = true;
    refresh();
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

  private void maybeEnableAntialiasing(Graphics g) {
    repaintManager.maybeEnableAntialiasing(g);
  }

  public void maybeDottedStroke(Graphics g) {
    repaintManager.maybeDottedStroke(g);
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

  public void setHoldRepaint(boolean holdRepaint) {
    repaintManager.setHoldRepaint(holdRepaint);
  }

  private void refreshFirmly() {
    repaintManager.refreshFirmly();
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

  /****************************************************************
   * delegated to StyleManager
   ****************************************************************/

  public void setStyleLabel(byte style) {
    styleManager.setStyleLabel(style);
    refresh();
  }

  public byte getStyleLabel() {
    return styleManager.styleLabel;
  }

  public void setStyleAtom(byte style) {
    if (selectionManager.countSelection() == 0) 
      styleManager.setStyleAtom(style);
    else
      styleManager.setStyleAtom(style, selectionManager.bsSelection);
    refresh();
  }

  public byte getStyleAtom() {
    return styleManager.styleAtom;
  }

  public void setStyleBond(byte style) {
    styleManager.setStyleBond(style);
    refresh();
  }

  public byte getStyleBond() {
    return styleManager.styleBond;
  }

  public void setPercentAngstromBond(int percentAngstromBond) {
    styleManager.setPercentAngstromBond(percentAngstromBond);
    refresh();
  }

  public int getPercentAngstromBond() {
    return styleManager.percentAngstromBond;
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

  public Font getMeasureFont(int size) {
    return styleManager.getMeasureFont(size);
  }

  public void setShowDarkerOutline(boolean showDarkerOutline) {
    styleManager.setShowDarkerOutline(showDarkerOutline);
    refresh();
  }

  public boolean getShowDarkerOutline() {
    return styleManager.showDarkerOutline;
  }

  public void setPercentVdwAtom(int percentVdwAtom) {
    styleManager.setPercentVdwAtom(percentVdwAtom);
    refresh();
  }

  public int getPercentVdwAtom() {
    return styleManager.percentVdwAtom;
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
    // no need to refresh
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

}
