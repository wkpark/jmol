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
package org.openscience.jmol.viewer;

import org.openscience.jmol.viewer.managers.*;
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.datamodel.*;

import org.openscience.jmol.viewer.script.Eval;

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
import java.util.Iterator;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.AxisAngle4d;
import java.net.URL;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;
import java.awt.event.MouseEvent;

/****************************************************************
 * The JmolViewer can be used to render client molecules. Clients
 * implement the JmolModelAdapter. JmolViewer uses this interface
 * to extract information from the client data structures and
 * render the molecule to the supplied java.awt.Component
 *
 * The default implementation of Jmol uses the CDK library
 * <a href='http://cdk.sourceforge.net'>cdk.sourceforge.net</a>
 *
 * The JmolViewer runs on Java 1.1 virtual machines.
 * The 3d graphics rendering package is a software implementation
 * of a z-buffer. It does not use Java3D and does not use Graphics2D
 * from Java 1.2. Therefore, it is well suited to building web browser
 * applets that will run on a wide variety of system configurations.
 ****************************************************************/

final public class JmolViewer {

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
  public DistributionManager distributionManager;
  public Eval eval;
  public Graphics3D g3d;

  public JmolModelAdapter modelAdapter;

  public String strJvmVersion;
  public boolean jvm12orGreater = false;
  public boolean jvm14orGreater = false;

  JmolStatusListener jmolStatusListener;

  public JmolViewer(Component awtComponent,
                    JmolModelAdapter modelAdapter) {

    this.awtComponent = awtComponent;
    this.modelAdapter = modelAdapter;

    strJvmVersion = System.getProperty("java.version");
    jvm12orGreater = (strJvmVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJvmVersion.compareTo("1.4") >= 0);

    colorManager = new ColorManager(this, modelAdapter);
    transformManager = new TransformManager(this);
    selectionManager = new SelectionManager(this);
    mouseManager = new MouseManager(awtComponent, this);
    fileManager = new FileManager(this);
    repaintManager = new RepaintManager(this);
    modelManager = new ModelManager(this, modelAdapter);
    styleManager = new StyleManager(this);
    labelManager = new LabelManager(this);
    axesManager = new AxesManager(this);
    measurementManager = new MeasurementManager(this);
    distributionManager = new DistributionManager(this);

    eval = new Eval(this);
    g3d = new Graphics3D(this);
  
  }

  public Component getAwtComponent() {
    return awtComponent;
  }

  private boolean structuralChange = false;


  public final static byte NOLABELS  = 0;
  public final static byte SYMBOLS   = 1;
  public final static byte TYPES     = 2;
  public final static byte NUMBERS   = 3;

  public final static byte DELETED   =-1;
  public final static byte NONE      = 0;
  public final static byte WIREFRAME = 1;
  public final static byte SHADED    = 2;
    
  public final static byte COLOR      =-1;
  public final static byte ATOMTYPE   = 0;
  public final static byte ATOMCHARGE = 1;
  public final static byte STRUCTURE  = 2;
  public final static byte AMINO      = 3;
  public final static byte SHAPELY    = 4;
  public final static byte CHAIN      = 5;

  public void homePosition() {
    // FIXME -- need to hold repaint during this process, but first 
    // figure out the interaction with the current holdRepaint setting
    setCenter(null);
    selectAll();
    transformManager.homePosition();
    refresh();
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

  public void rotateToX(float angleRadians) {
    transformManager.rotateToX(angleRadians);
    refresh();
  }
  public void rotateToY(float angleRadians) {
    transformManager.rotateToY(angleRadians);
    refresh();
  }
  public void rotateToZ(float angleRadians) {
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

  public void rotateByX(float angleRadians) {
    transformManager.rotateByX(angleRadians);
    refresh();
  }
  public void rotateByY(float angleRadians) {
    transformManager.rotateByY(angleRadians);
    refresh();
  }
  public void rotateByZ(float angleRadians) {
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

  public static float toRadians(int degrees) {
    return (degrees * 2) * (float)(Math.PI / 360);
  }

  public void rotate(AxisAngle4f axisAngle) {
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

  public Matrix4f getPovRotateMatrix() {
    return transformManager.getPovRotateMatrix();
  }

  public Matrix4f getPovTranslateMatrix() {
    return transformManager.getPovTranslateMatrix();
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  public void calcTransformMatrices() {
    transformManager.calcTransformMatrices();
  }

  public Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public void transformVector(Vector3f vectorAngstroms,
                              Vector3f vectorTransformed) {
    transformManager.transformVector(vectorAngstroms, vectorTransformed);
  }

  public float scaleToScreen(int z, float sizeAngstroms) {
    return transformManager.scaleToScreen(z, sizeAngstroms);
  }

  public short scaleToScreen(int z, int milliAngstroms) {
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  public float scaleToPerspective(int z, float sizeAngstroms) {
    return transformManager.scaleToPerspective(z, sizeAngstroms);
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

  public void setCameraDepth(float depth) {
    transformManager.setCameraDepth(depth);
  }

  public float getCameraDepth() {
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
    g3d.setSize(dim.width, dim.height);
  }

  public Dimension getScreenDimension() {
    return dimCurrent;
  }

  /****************************************************************
   * delegated to ColorManager
   ****************************************************************/

  public void setModeAtomColorProfile(byte scheme) {
    colorManager.setSchemeDefault(scheme);
    distributionManager.setColixAtom(scheme, Colix.NULL,
                                     atomIteratorSelected());
    refresh();
  }

  public byte getModeAtomColorProfile() {
    return colorManager.schemeDefault;
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
    setColorDotsConvex(color);
    setColorDotsConcave(color);
    setColorDotsSaddle(color);
  }

    public void setColorDotsSaddle(Color color) {
	colorManager.setColorDotsSaddle(color);
    }
    public short getColixDotsSaddle() {
	return colorManager.colixDotsSaddle;
    }


    public void setColorDotsConvex(Color color) {
	colorManager.setColorDotsConvex(color);
    }
    public short getColixDotsConvex() {
	return colorManager.colixDotsConvex;
    }

    public void setColorDotsConcave(Color color) {
	colorManager.setColorDotsConcave(color);
    }
    public short getColixDotsConcave() {
	return colorManager.colixDotsConcave;
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

  // note that colorBond could be null -- meaning inherit atom color
  public void setColorBond(Color colorBond) {
    colorManager.setColorBond(colorBond);
    distributionManager.setColix(Colix.getColix(colorBond),
                                 bondIteratorSelected(BondShape.COVALENT));
    refresh();
  }

  public short getColixBond() {
    return colorManager.colixBond;
  }

  public Color getColorBond() {
    return colorManager.colorBond;
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
      !repaintManager.wireframeRotating &&
      isSelected(atomShape);
  }

  public boolean selectionHaloEnabled = false;
  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    this.selectionHaloEnabled = selectionHaloEnabled;
    refresh();
  }
  
  public boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  private boolean bondSelectionModeOr;
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

  public void setAppletContext(URL documentBase, URL codeBase,
                               String appletProxy) {
    fileManager.setAppletContext(documentBase, codeBase, appletProxy);
  }

  public Object getInputStreamOrErrorMessageFromName(String name) {
    return fileManager.getInputStreamOrErrorMessageFromName(name);
  }

  public void openFile(String name) {
    clear();
    fileManager.openFile(name);
  }

  public void openStringInline(String strModel) {
    clear();
    fileManager.openStringInline(strModel);
  }
  
  public String getOpenFileError() {
    String fullPathName = fileManager.getFullPathName();
    String fileName = fileManager.getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      notifyFileNotLoaded(fullPathName, errorMsg);
      return errorMsg;
    }
    setClientFile(fullPathName, fileName, clientFile);
    notifyFileLoaded(fullPathName, fileName,
                     modelManager.getModelName(), clientFile);
    return null;
  }

  /****************************************************************
   * delegated to ModelManager
   ****************************************************************/

  public void setClientFile(String fullPathName, String fileName,
                            Object clientFile) {
    pushHoldRepaint();
    modelManager.setClientFile(fullPathName, fileName, clientFile);
    setFrame(0);
    homePosition();
    // don't know if I need this firm refresh here or not
    // FIXME mth -- we need to clear definitions when we open a new file
    // but perhaps not if we are in the midst of executing a script?
    eval.clearDefinitionsAndLoadPredefined();
    setStructuralChange();
    popHoldRepaint();
  }

  public Object getClientFile() {
    return modelManager.getClientFile();
  }

  public void clear() {
    modelManager.setClientFile(null, null, null);
    clearMeasurements();
    refresh();
  }

  public int getFrameCount() {
    return modelManager.getFrameCount();
  }

  public String getModelName() {
    return modelManager.getModelName();
  }

  public boolean haveFile() {
    return modelManager.haveFile;
  }

  public JmolFrame getJmolFrame() {
    return modelManager.getJmolFrame();
  }

  public float getRotationRadius() {
    return modelManager.getRotationRadius();
  }

  public Point3f getRotationCenter() {
    return modelManager.getRotationCenter();
  }

  public Point3f getBoundingBoxCenter() {
    return modelManager.getBoundingBoxCenter();
  }

  public Point3f getBoundingBoxCorner() {
    return modelManager.getBoundingBoxCorner();
  }

  public int getBoundingBoxCenterX() {
    // FIXME mth 2003 05 31
    // used by the labelRenderer for rendering labels away from the center
    // for now this is returning the center of the screen
    // need to transform the center of the bounding box and return that point
    return dimCurrent.width / 2;
  }

  public int getBoundingBoxCenterY() {
    return dimCurrent.height / 2;
  }

  public int getNumberOfFrames() {
    return modelManager.getFrameCount();
  }

  public void setFrame(int frameNumber) {
    modelManager.setFrame(frameNumber);
    measurementManager.setJmolFrame(getJmolFrame());
    selectAll();
    recalcAxes();
    structuralChange = true;
    refresh();
  }

  public int getCurrentFrameNumber() {
    return modelManager.getCurrentFrameNumber();
  }

  public int getAtomCount() {
    return modelManager.getAtomCount();
  }

  public int getBondCount() {
    return modelManager.getBondCount();
  }

    public Point3f getPoint3f(int atomIndex) {
	return modelManager.getPoint3f(atomIndex);
    }

  public float getAtomX(Object clientAtom) {
      return modelAdapter.getAtomX(clientAtom);
  }
  public float getAtomY(Object clientAtom) {
      return modelAdapter.getAtomY(clientAtom);
  }
  public float getAtomZ(Object clientAtom) {
      return modelAdapter.getAtomZ(clientAtom);
  }

  public int findNearestAtomIndex(int x, int y) {
    return modelManager.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
    return modelManager.findAtomsInRectangle(rectRubberBand);
  }

  public void setCenter(Point3f center) {
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

  public void setBondTolerance(float bondTolerance) {
    modelManager.setBondTolerance(bondTolerance);
    refresh();
  }

  public float getBondTolerance() {
    return modelManager.bondTolerance;
  }

  public void setMinBondDistance(float minBondDistance) {
    modelManager.setMinBondDistance(minBondDistance);
    refresh();
  }

  public float getMinBondDistance() {
    return modelManager.minBondDistance;
  }

  public void setAutoBond(boolean ab) {
    modelManager.setAutoBond(ab);
    refresh();
  }

  public boolean getAutoBond() {
    return modelManager.autoBond;
  }

  public void setSolventProbeRadius(float radius) {
    modelManager.setSolventProbeRadius(radius);
  }

  public float getSolventProbeRadius() {
    return modelManager.solventProbeRadius;
  }

  public void deleteAtom(int atomIndex) {
    if (measurementManager.deleteMeasurementsReferencing(atomIndex))
      notifyMeasurementsChanged();
    modelManager.deleteAtom(atomIndex);
    //            status.setStatus(2, "Atom deleted"); 
    selectAll();
    structuralChange = true;
    refresh();
  }

  /****************************************************************
   * delegated to MeasurementManager
   ****************************************************************/

  public Object[] getMeasurements(int count) {
    return measurementManager.getMeasurements(count);
  }

  public void clearMeasurements() {
    measurementManager.clearMeasurements();
    refresh();
  }

  public int getMeasurementCount() {
    return measurementManager.getMeasurementCount();
  }

  public int[] getMeasurementIndices(int measurementIndex) {
    return measurementManager.getMeasurementIndices(measurementIndex);
  }

  public String getMeasurementString(int measurementIndex) {
    return measurementManager.getMeasurementString(measurementIndex);
  }

  public void deleteMeasurements(int count) {
    measurementManager.deleteMeasurements(count);
  }

  public void defineMeasurement(int count, int[] atomIndices) {
    measurementManager.defineMeasurement(count, atomIndices);
    refresh();
  }

  public boolean deleteMeasurement(int measurementIndex) {
    boolean deleted = measurementManager.deleteMeasurement(measurementIndex);
    if (deleted)
      refresh();
    return deleted;
  }

  public boolean deleteMeasurement(Object measurement) {
    boolean deleted =
      measurementManager.deleteMeasurement((MeasurementShape)measurement);
    if (deleted)
      refresh();
    return deleted;
  }

  public boolean deleteMeasurement(int count, int[] atomIndices) {
    boolean deleted =
      measurementManager.deleteMeasurement(count, atomIndices);
    if (deleted)
      refresh();
    return deleted;
  }

  /****************************************************************
   * delegated to RepaintManager
   ****************************************************************/

  public FrameRenderer getFrameRenderer() {
    return repaintManager.frameRenderer;
  }

  public void setWireframeRotating(boolean wireframeRotating) {
    repaintManager.setWireframeRotating(wireframeRotating);
  }

  public boolean getWireframeRotating() {
    return repaintManager.wireframeRotating;
  }

  public void setInMotion(boolean inMotion) {
    repaintManager.setInMotion(inMotion);
    checkOversample();
  }

  public boolean getInMotion() {
    return repaintManager.inMotion;
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
    repaintManager.render(g3d, rectClip);
    return g3d.getScreenImage();
  }

  public Image getScreenImage() {
    return g3d.getScreenImage();
  }

  public void checkOversample() {
    boolean tOversample =
      (tOversampleAlways | (!repaintManager.inMotion & tOversampleStopped));
    repaintManager.setOversample(tOversample);
    transformManager.setOversample(tOversample);
  }

  public void setOversample(boolean tOversample) {
    transformManager.setOversample(tOversample);
    repaintManager.setOversample(tOversample);
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
    distributionManager.setStyleMarAtom(style, mar, atomIteratorSelected());
  }

  public void setStyleAtomScript(byte style) {
    distributionManager.setStyleAtom(style, atomIteratorSelected());
  }

  public void setStyleMarBondScript(byte style, short mar) {
    distributionManager.setStyleMar(style, mar,
                                    bondIteratorSelected(BondShape.COVALENT));
  }

  public void setStyleMarBackboneScript(byte style, short mar) {
    distributionManager.setStyleMar(style, mar,
                                    bondIteratorSelected(BondShape.BACKBONE));
  }

  public void setStyleBondScript(byte style) {
    distributionManager.setStyle(style,
                                 bondIteratorSelected(BondShape.COVALENT));
  }

  public void setStyleBondScript(byte style, byte bondType) {
    distributionManager.setStyle(style, bondIteratorSelected(bondType));
  }

  public void setStyleBackboneScript(byte style) {
    distributionManager.setStyle(style,
                                 bondIteratorSelected(BondShape.BACKBONE));
  }

  public void setColorAtomScript(byte scheme, Color color) {
    distributionManager.setColixAtom(scheme, Colix.getColix(color),
                                     atomIteratorSelected());
  }

  public void setColorBondScript(Color color) {
    distributionManager.setColix(Colix.getColix(color),
                                 bondIteratorSelected(BondShape.COVALENT));
  }

  public void setColorBackboneScript(Color color) {
    distributionManager.setColix(Colix.getColix(color),
                                 bondIteratorSelected(BondShape.BACKBONE));
  }

  public void setLabelScript(String strLabel) {
    distributionManager.setLabel(strLabel, atomIteratorSelected());
  }

  public void setDotsOn(boolean dotsOn) {
    getJmolFrame().setDotsOn(dotsOn, selectionManager.bsSelection);
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

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    this.jmolStatusListener = jmolStatusListener;
  }

  public void notifyFileLoaded(String fullPathName, String fileName,
                               String modelName, Object clientFile) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileLoaded(fullPathName, fileName,
                                          modelName, clientFile);
  }

  public void notifyFileNotLoaded(String fullPathName, String errorMsg) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileNotLoaded(fullPathName, errorMsg);
  }

  public void manageScriptTermination() {
    String strErrorMessage = eval.getErrorMessage();
    int msWalltime = eval.getExecutionWalltime();
    eval.resetTerminationNotification();
    if (jmolStatusListener != null)
      jmolStatusListener.notifyScriptTermination(strErrorMessage,
                                                   msWalltime);
    }

  public void scriptEcho(String strEcho) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptEcho(strEcho);
  }

  public void scriptStatus(String strStatus) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptStatus(strStatus);
  }

  public void measureSelection(int iatom) {
    if (jmolStatusListener != null)
      jmolStatusListener.measureSelection(iatom);
  }

  public void notifyMeasurementsChanged() {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyMeasurementsChanged();
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
    System.out.println("viewer.getBooleanProperty(" +
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
    System.out.println("viewer.setBooleanProperty(" +
                       key + "," + value + ") - unrecognized");
  }

  /****************************************************************
   * Graphics3D
   ****************************************************************/

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
    public void release() {}
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
    distributionManager.setStyleAtom(style, atomIteratorSelected());
    refresh();
  }

  public byte getStyleAtom() {
    return styleManager.styleAtom;
  }

  public void setPercentVdwAtom(int percentVdwAtom) {
    styleManager.setPercentVdwAtom(percentVdwAtom);
    distributionManager.setMarAtom((short)-percentVdwAtom,
                                   atomIteratorSelected());
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
    distributionManager.setStyle(style,
                                 bondIteratorSelected(BondShape.COVALENT));
    refresh();
  }

  public byte getStyleBond() {
    return styleManager.styleBond;
  }

  public void setMarBond(short marBond) {
    styleManager.setMarBond(marBond);
    distributionManager.setMar(marBond,
                               bondIteratorSelected(BondShape.COVALENT));
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

  public void setWireframeRotation(boolean wireframeRotation) {
    styleManager.setWireframeRotation(wireframeRotation);
    // no need to refresh since we are not currently rotating
  }

  public boolean getWireframeRotation() {
    return styleManager.wireframeRotation;
  }

  /****************************************************************
   * delegated to LabelManager
   ****************************************************************/

  public void setStyleLabel(byte style) {
    labelManager.setStyleLabel(style);
    distributionManager.setStyleLabel(style, atomIteratorSelected());
    refresh();
  }

  public byte getStyleLabel() {
    return labelManager.styleLabel;
  }

  public String getLabelAtom(AtomShape atomShape, int atomIndex) {
    return labelManager.getLabelAtom(labelManager.styleLabel,
                                     atomShape, atomIndex);
  }

  public String getLabelAtom(byte styleLabel, AtomShape atomShape,
                             int atomIndex) {
    return labelManager.getLabelAtom(styleLabel, atomShape, atomIndex);
  }

  public String getLabelAtom(String strLabel, AtomShape atomShape,
                             int atomIndex) {
    return labelManager.getLabelAtom(strLabel, atomShape, atomIndex);
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
  
  /*
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
  */

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
   * JmolClientAdapter routines
   ****************************************************************/

  public JmolModelAdapter getJmolModelAdapter() {
    return modelAdapter;
  }

  public int getFrameCount(Object clientFile) {
    return modelManager.getFrameCount(clientFile);
  }

  public String getModelName(Object clientFile) {
    return modelManager.getModelName(clientFile);
  }

  public int getAtomicNumber(Object clientAtom) {
    return modelManager.getAtomicNumber(clientAtom);
  }

  public String getAtomicSymbol(int atomicNumber, Object clientAtom) {
    return modelManager.getAtomicSymbol(atomicNumber, clientAtom);
  }

  public String getAtomTypeName(int atomicNumber, Object clientAtom) {
    return modelManager.getAtomTypeName(atomicNumber, clientAtom);
  }

  public float getVanderwaalsRadius(int atomicNumber, Object clientAtom) {
    return modelManager.getVanderwaalsRadius(atomicNumber, clientAtom);
  }

  public float getCovalentRadius(int atomicNumber, Object clientAtom) {
    return modelManager.getCovalentRadius(atomicNumber, clientAtom);
  }

  public String getPdbAtomRecord(Object clientAtom) {
    return modelManager.getPdbAtomRecord(clientAtom);
  }

  public short getColixAtom(AtomShape atom) {
    return colorManager.getColixAtom(atom);
  }

  public short getColixAtomScheme(AtomShape atom, byte scheme) {
    return colorManager.getColixAtomScheme(atom, scheme);
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  public String getAtomicSymbol(int i) {
    return modelManager.getAtomicSymbol(i);
  }

  public float getAtomX(int i) {
    return modelManager.getAtomX(i);
  }

  public float getAtomY(int i) {
    return modelManager.getAtomY(i);
  }

  public float getAtomZ(int i) {
    return modelManager.getAtomZ(i);
  }

  public Point3f getAtomPoint3f(int i) {
    return modelManager.getAtomPoint3f(i);
  }

  public float getAtomRadius(int i) {
    return modelManager.getAtomRadius(i);
  }

  public Color getAtomColor(int i) {
    return Colix.getColor(modelManager.getAtomColix(i));
  }

  public Point3f getBondPoint3f1(int i) {
    return modelManager.getBondPoint3f1(i);
  }

  public Point3f getBondPoint3f2(int i) {
    return modelManager.getBondPoint3f2(i);
  }

  public float getBondRadius(int i) {
    return modelManager.getBondRadius(i);
  }

  public byte getBondOrder(int i) {
    return modelManager.getBondOrder(i);
  }

  public Color getBondColor1(int i) {
    return Colix.getColor(modelManager.getBondColix1(i));
  }

  public Color getBondColor2(int i) {
    return Colix.getColor(modelManager.getBondColix2(i));
  }
}
