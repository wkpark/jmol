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

import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.util.Hashtable;
import java.util.BitSet;
import javax.vecmath.Point3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import org.openscience.jmol.io.ChemFileReader;
import org.openscience.jmol.io.ReaderFactory;

final public class DisplayControl {

  public static DisplayControl control;
  Component awtComponent;
  ColorManager colorManager;
  TransformManager transformManager;
  SelectionManager selectionManager;
  MouseManager mouseManager;
  FileManager fileManager;
  ModelManager modelManager;
  RepaintManager repaintManager;

  public DisplayControl(Component awtComponent) {
    control = this;
    this.awtComponent = awtComponent;
    colorManager = new ColorManager(this);
    transformManager = new TransformManager(this);
    selectionManager = new SelectionManager();
    mouseManager = new MouseManager(awtComponent, this);
    fileManager = new FileManager(this);
    modelManager = new ModelManager(this);
    repaintManager = new RepaintManager(this);
  }

  public Component getAwtComponent() {
    return awtComponent;
  }

  public final static int NOLABELS =  0;
  public final static int SYMBOLS =   1;
  public final static int TYPES =     2;
  public final static int NUMBERS =   3;
  public final static int QUICKDRAW = 0;
  public final static int SHADING =   1;
  public final static int WIREFRAME = 2;
  public final static int LINE =      3;
  public final static int ATOMTYPE =   0;
  public final static int ATOMCHARGE = 1;

  // while these variables are public, they should be considered *read-only*
  // to write these variables you *must* use the appropriate set function
  // they are currently used by Atom and AtomShape for transforms & rendering
  public boolean inMotion = false;

  private boolean structuralChange = false;

  public int modeLabel = NOLABELS;
  public void setModeLabel(int mode) {
    if (modeLabel != mode) {
      modeLabel = mode;
      refresh();
    }
  }

  public int modeAtomDraw = QUICKDRAW;
  public void setModeAtomDraw(int mode) {
    if (modeAtomDraw != mode) {
      modeAtomDraw = mode;
      refresh();
    }
  }

  public int modeBondDraw = QUICKDRAW;
  public void setModeBondDraw(int mode) {
    if (modeBondDraw != mode) {
      modeBondDraw = mode;
      refresh();
    }
  }
  public int getModeBondDraw() {
    return modeBondDraw;
  }

  public int percentAngstromBond = 10;
  public void setPercentAngstromBond(int percentAngstromBond) {
    this.percentAngstromBond = percentAngstromBond;
    refresh();
  }
  public int getPercentAngstromBond() {
    return percentAngstromBond;
  }

  public boolean showAtoms = true;
  public void setShowAtoms(boolean showAtoms) {
    if (this.showAtoms != showAtoms) {
      this.showAtoms = showAtoms;
      refresh();
    }
  }
  public boolean getShowAtoms() {
    return showAtoms;
  }

  public boolean showBonds = true;
  public void setShowBonds(boolean showBonds) {
    if (this.showBonds != showBonds) {
      this.showBonds = showBonds;
      refresh();
    }
  }
  public boolean getShowBonds() {
    return showBonds;
  }

  public boolean showHydrogens = true;
  public void setShowHydrogens(boolean showHydrogens) {
    if (this.showHydrogens != showHydrogens) {
      this.showHydrogens = showHydrogens;
      refresh();
    }
  }
  public boolean getShowHydrogens() {
    return showHydrogens;
  }

  public boolean showVectors = false;
  public void setShowVectors(boolean showVectors) {
    if (this.showVectors != showVectors) {
      this.showVectors = showVectors;
      structuralChange = true;
      refresh();
    }
  }
  public boolean getShowVectors() {
    return showVectors;
  }

  public boolean showMeasurements = false;
  public void setShowMeasurements(boolean showMeasurements) {
    if (this.showMeasurements != showMeasurements) {
      this.showMeasurements = showMeasurements;
      structuralChange = true;
      refresh();
    }
  }
  public boolean getShowMeasurements() {
    return showMeasurements;
  }

  public Font getMeasureFont(int size) {
    return new Font("Helvetica", Font.PLAIN, size);
  }

  public boolean showDarkerOutline = false;
  public void setShowDarkerOutline(boolean showDarkerOutline) {
    if (this.showDarkerOutline != showDarkerOutline) {
      this.showDarkerOutline = showDarkerOutline;
      refresh();
    }
  }
  public boolean getShowDarkerOutline() {
    return showDarkerOutline;
  }

  public int percentVdwAtom = 20;
  public void setPercentVdwAtom(int percentVdwAtom) {
    this.percentVdwAtom = percentVdwAtom;
    refresh();
  }

  public int getPercentVdwAtom() {
    return percentVdwAtom;
  }

  public void setFastRendering(boolean fastRendering) {
    repaintManager.setFastRendering(fastRendering);
  }

  public boolean getFastRendering() {
    return repaintManager.fastRendering;
  }

  public String propertyMode = "";
  public void setPropertyMode(String s) {
    propertyMode = s;
    refresh();
  }
  public String getPropertyMode() {
    return propertyMode;
  }

  public void homePosition() {
    // FIXME -- need to hold repaint during this process, but first 
    // figure out the interaction with the current holdRepaint setting
    setCenter(null);
    clearSelection();
    transformManager.homePosition();
    refresh();
  }

  public void maybeEnableAntialiasing(Graphics g) {
    repaintManager.maybeEnableAntialiasing(g);
  }

  public void maybeDottedStroke(Graphics g) {
    repaintManager.maybeDottedStroke(g);
  }

  public void setChemFile(ChemFile chemfile) {
    modelManager.setChemFile(chemfile);
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

  public void mlistChanged(MeasurementListEvent mle) {
    modelManager.mlistChanged(mle);
  }

  public boolean wireframeRotation = false;
  public void setWireframeRotation(boolean wireframeRotation) {
    this.wireframeRotation = wireframeRotation;
    refresh();
  }

  public boolean getWireframeRotation() {
    return wireframeRotation;
  }

  public void setInMotion(boolean inMotion) {
    repaintManager.setInMotion(inMotion);
  }

  public boolean jvm12orGreater = false;

  public void setJvm12orGreater(boolean jvm12orGreater) {
    this.jvm12orGreater = jvm12orGreater;
    repaintManager.calcUseGraphics2D();
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

  public void setCenter(Point3d center) {
    modelManager.setRotationCenter(center);
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

  public void setCenterAsSelected() {
    modelManager.setCenterAsSelected();
    clearSelection();
    scaleFitToScreen();
    refresh();
  }

  public boolean hasStructuralChange() {
    return structuralChange;
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

  public void resetStructuralChange() {
    structuralChange = false;
  }

  public final Hashtable imageCache = new Hashtable();
  public void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
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

  // FIXME NEEDSWORK -- arrow vector stuff
  private double arrowHeadSize = 10.0f;
  private double arrowHeadRadius = 1.0f;
  private double arrowLengthScale = 1.0f;

  public void setArrowHeadSize(double ls) {
    arrowHeadSize = 10.0f * ls;
    refresh();
  }

  public double getArrowHeadSize() {
    return arrowHeadSize / 10.0f;
  }

  // mth dec 2003
  // for some reason, internal to ArrowLine the raw arrowHeadSize was
  // used, but externally it is multiplied/divided by 10
  // will figure it out and fix it later
  public double getArrowHeadSize10() {
    return arrowHeadSize;
  }

  public void setArrowLengthScale(double ls) {
    arrowLengthScale = ls;
    refresh();
  }

  public double getArrowLengthScale() {
    return arrowLengthScale;
  }

  public void setArrowHeadRadius(double rs) {
    arrowHeadRadius = rs;
    refresh();
  }

  public double getArrowHeadRadius() {
    return arrowHeadRadius;
  }

  public void scriptEcho(String str) {
    // FIXME -- if there is a script window it should go there
    // for an applet it needs to go someplace else
    System.out.println(str);
  }

  /****************************************************************
   delegated to TransformManager
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
    return transformManager.screenAtomDiameter(z, atom, percentVdwAtom);
  }

  public int screenBondWidth(int z) {
    return transformManager.screenBondWidth(z, percentAngstromBond);
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
   delegated to ColorManager
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
   delegated to SelectionManager
  ****************************************************************/

  public void addSelection(Atom atom) {
    selectionManager.addSelection(atom);
    refresh();
  }

  public void removeSelection(Atom atom) {
    selectionManager.removeSelection(atom);
    refresh();
  }

  public void toggleSelection(Atom atom) {
    selectionManager.toggleSelection(atom);
    refresh();
  }

  public void addSelection(Atom[] atoms) {
    selectionManager.addSelection(atoms);
    refresh();
  }

  public void removeSelection(Atom[] atoms) {
    selectionManager.removeSelection(atoms);
    refresh();
  }

  public void clearSelection() {
    selectionManager.clearSelection();
    refresh();
  }

  public int countSelection() {
    return selectionManager.countSelection();
  }

  public boolean isSelected(Atom atom) {
    return selectionManager.isSelected(atom);
  }

  public void setSelectionSet(BitSet set) {
    selectionManager.setSelectionSet(set);
    refresh();
  }

  public BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  /****************************************************************
   delegated to MouseManager
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
   delegated to FileManager
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
}
