/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
package org.jmol.viewer;

import org.jmol.api.JmolAdapter;
import org.jmol.g3d.*;
import org.jmol.viewer.managers.*;
import org.jmol.viewer.datamodel.*;

import org.jmol.viewer.script.Eval;

import java.lang.reflect.Array;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Event;
import java.awt.Cursor;
import java.util.Hashtable;
import java.util.Vector;
import java.util.BitSet;
import java.util.Iterator;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.AxisAngle4d;
import java.net.URL;
import java.io.InputStream;
import java.io.Reader;
import java.io.File;

/****************************************************************
 * The JmolViewer can be used to render client molecules. Clients
 * implement the JmolAdapter. JmolViewer uses this interface
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
  public TempManager tempManager;
  public PickingManager pickingManager;
  public Eval eval;
  public Graphics3D g3d;

  public JmolAdapter modelAdapter;

  public String strJavaVendor;
  public String strJavaVersion;
  public String strOSName;
  public boolean jvm11orGreater = false;
  public boolean jvm12orGreater = false;
  public boolean jvm14orGreater = false;

  JmolStatusListener jmolStatusListener;

  public JmolViewer(Component awtComponent,
                    JmolAdapter modelAdapter) {

    this.awtComponent = awtComponent;
    this.modelAdapter = modelAdapter;

    strJavaVendor = System.getProperty("java.vendor");
    strOSName = System.getProperty("os.name");
    strJavaVersion = System.getProperty("java.version");
    jvm11orGreater = (strJavaVersion.compareTo("1.1") >= 0 &&
                      // Netscape on MacOS does not implement 1.1 event model
                      ! (strJavaVendor.startsWith("Netscape") &&
                         strJavaVersion.compareTo("1.1.5") <= 0 &&
                         "Mac OS".equals(strOSName)));
    jvm12orGreater = (strJavaVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJavaVersion.compareTo("1.4") >= 0);
    
    System.out.println(JmolConstants.copyright +
                       "\nJmol Version " + JmolConstants.version +
                       "  " + JmolConstants.date +
                       "\njava.vendor:" + strJavaVendor +
                       "\njava.version:" + strJavaVersion +
                       "\nos.name:" + strOSName);

    g3d = new Graphics3D(awtComponent);
    colorManager = new ColorManager(this, g3d);
    transformManager = new TransformManager(this);
    selectionManager = new SelectionManager(this);
    if (jvm14orGreater) 
      mouseManager = MouseWrapper14.alloc(awtComponent, this);
    else if (jvm11orGreater)
      mouseManager = MouseWrapper11.alloc(awtComponent, this);
    else
      mouseManager = new MouseManager10(awtComponent, this);
    fileManager = new FileManager(this, modelAdapter);
    repaintManager = new RepaintManager(this);
    modelManager = new ModelManager(this, modelAdapter);
    styleManager = new StyleManager(this);
    tempManager = new TempManager(this);
    pickingManager = new PickingManager(this);
  }

  public Component getAwtComponent() {
    return awtComponent;
  }

  public boolean handleOldJvm10Event(Event e) {
    return mouseManager.handleOldJvm10Event(e);
  }

  public void homePosition() {
    // FIXME -- need to hold repaint during this process, but first 
    // figure out the interaction with the current holdRepaint setting
    setCenter(null);
    selectAll();
    transformManager.homePosition();
    refresh();
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
    rotateToX(angleDegrees * radiansPerDegree);
  }
  public void rotateToY(int angleDegrees) {
    rotateToY(angleDegrees * radiansPerDegree);
  }
  public void rotateToZ(int angleDegrees) {
    rotateToZ(angleDegrees * radiansPerDegree);
  }

  public void rotateXRadians(float angleRadians) {
    transformManager.rotateXRadians(angleRadians);
    refresh();
  }
  public void rotateYRadians(float angleRadians) {
    transformManager.rotateYRadians(angleRadians);
    refresh();
  }
  public void rotateZRadians(float angleRadians) {
    transformManager.rotateZRadians(angleRadians);
    refresh();
  }
  public void rotateXDegrees(float angleDegrees) {
    rotateXRadians(angleDegrees * radiansPerDegree);
  }
  public void rotateYDegrees(float angleDegrees) {
    rotateYRadians(angleDegrees * radiansPerDegree);
  }
  public void rotateZDegrees(float angleDegrees) {
    rotateZRadians(angleDegrees * radiansPerDegree);
  }
  public void rotateZDegreesScript(float angleDegrees) {
    transformManager.rotateZRadiansScript(angleDegrees * radiansPerDegree);
    refresh();
  }

  final static float radiansPerDegree = (float)(2 * Math.PI / 360);
  final static float degreesPerRadian = (float)(360 / (2 * Math.PI));

  public void rotate(AxisAngle4f axisAngle) {
    transformManager.rotate(axisAngle);
    refresh();
  }

  public void rotateAxisAngle(float x, float y, float z, float degrees) {
    transformManager.rotateAxisAngle(x, y, z, degrees);
  }

  public void rotateTo(float xAxis, float yAxis, float zAxis, float degrees) {
    transformManager.rotateTo(xAxis, yAxis, zAxis, degrees);
  }

  public void rotateTo(AxisAngle4f axisAngle) {
    transformManager.rotateTo(axisAngle);
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

  public void depthToPercent(int percentDepth) {
    transformManager.depthToPercent(percentDepth);
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

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  public void calcTransformMatrices() {
    transformManager.calcTransformMatrices();
  }

  public Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  public Point3i transformPoint(Point3f pointAngstroms,
                                Vector3f vibrationVector) {
    return transformManager.transformPoint(pointAngstroms, vibrationVector);
  }

  public void transformPoint(Point3f pointAngstroms,
                             Vector3f vibrationVector, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, vibrationVector,
                                    pointScreen);
  }

  public void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public void transformPoint(Point3f pointAngstroms, Point3f pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  public void transformPoints(Point3f[] pointsAngstroms, Point3i[] pointsScreens) {
    transformManager.transformPoints(pointsAngstroms.length,
                                     pointsAngstroms, pointsScreens);
  }

  public void transformVector(Vector3f vectorAngstroms,
                              Vector3f vectorTransformed) {
    transformManager.transformVector(vectorAngstroms, vectorTransformed);
  }

  public float getScalePixelsPerAngstrom() {
    return transformManager.scalePixelsPerAngstrom;
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

  public void setAxesOrientationRasmol(boolean axesOrientationRasmol) {
    transformManager.setAxesOrientationRasmol(axesOrientationRasmol);
    refresh();
  }
  public boolean getAxesOrientationRasmol() {
    return transformManager.axesOrientationRasmol;
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

  public void checkCameraDistance() {
    if (transformManager.increaseRotationRadius)
      modelManager.
        increaseRotationRadius(transformManager.getRotationRadiusIncrease());
  }

  final Dimension dimScreen = new Dimension();
  final Rectangle rectClip = new Rectangle();

  public void setScreenDimension(Dimension dim) {
    // note that there is a bug in MacOS when comparing dimension objects
    // so don't try dim1.equals(dim2)
    if (dim.width == dimScreen.width && dim.height == dimScreen.height)
      return;
    dimScreen.width = dim.width;
    dimScreen.height = dim.height;
    transformManager.setScreenDimension(dim.width, dim.height);
    transformManager.scaleFitToScreen();
    g3d.setSize(dim);
  }

  public int getScreenWidth() {
    return dimScreen.width;
  }

  public int getScreenHeight() {
    return dimScreen.height;
  }

  public void setRectClip(Rectangle clip) {
    if (clip == null) {
      rectClip.x = rectClip.y = 0;
      rectClip.setSize(dimScreen);
    } else {
      rectClip.setBounds(clip);
      // on Linux platform with Sun 1.4.2_02 I am getting a clipping rectangle
      // that is wider than the current window during window resize
      if (rectClip.x < 0)
        rectClip.x = 0;
      if (rectClip.y < 0)
        rectClip.y = 0;
      if (rectClip.x + rectClip.width > dimScreen.width)
        rectClip.width = dimScreen.width - rectClip.x;
      if (rectClip.y + rectClip.height > dimScreen.height)
        rectClip.height = dimScreen.height - rectClip.y;
    }
  }

  public void setScaleAngstromsPerInch(float angstromsPerInch) {
    transformManager.setScaleAngstromsPerInch(angstromsPerInch);
  }

  public void setSlabAndDepthValues(int slabValue, int depthValue) {
    g3d.setSlabAndDepthValues(slabValue, depthValue);
  }
  
  public void setVibrationPeriod(float period) {
    transformManager.setVibrationPeriod(period);
  }

  public void setVibrationT(float t) {
    transformManager.setVibrationT(t);
  }

  public float getVibrationRadians() {
    return transformManager.vibrationRadians;
  }

  public void setSpinX(int value) {
    transformManager.setSpinX(value);
  }
  public int getSpinX() {
    return transformManager.spinX;
  }

  public void setSpinY(int value) {
    transformManager.setSpinY(value);
  }
  public int getSpinY() {
    return transformManager.spinY;
  }


  public void setSpinZ(int value) {
    transformManager.setSpinZ(value);
  }
  public int getSpinZ() {
    return transformManager.spinZ;
  }


  public void setSpinFps(int value) {
    transformManager.setSpinFps(value);
  }
  public int getSpinFps() {
    return transformManager.spinFps;
  }

  public void setSpinOn(boolean spinOn) {
    transformManager.setSpinOn(spinOn);
  }
  public boolean getSpinOn() {
    return transformManager.spinOn;
  }

  public String getOrientationText() {
    return transformManager.getOrientationText();
  }

  public void getAxisAngle(AxisAngle4f axisAngle) {
    transformManager.getAxisAngle(axisAngle);
  }

  public String getTransformText() {
    return transformManager.getTransformText();
  }

  public void setRotation(Matrix3f matrixRotation) {
    transformManager.setRotation(matrixRotation);
  }

  public void getRotation(Matrix3f matrixRotation) {
    transformManager.getRotation(matrixRotation);
  }

  /****************************************************************
   * delegated to ColorManager
   ****************************************************************/

  public void setModeAtomColorProfile(byte palette) {
    colorManager.setPaletteDefault(palette);
    refresh();
  }

  public byte getModeAtomColorProfile() {
    return colorManager.paletteDefault;
  }

  public void setColorScheme(String colorScheme) {
    colorManager.setColorScheme(colorScheme);
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

  public void setColorRubberband(Color color) {
    colorManager.setColorRubberband(color);
  }

  public short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  public void setColorLabel(Color color) {
    colorManager.setColorLabel(color);
    setShapeColorProperty(JmolConstants.SHAPE_LABELS, color);
    refresh();
  }
  
  public void setColorDotsSaddle(Color color) {
    colorManager.setColorDotsSaddle(color);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "dotssaddle", color);
  }

  public short getColixDotsSaddle() {
    return colorManager.colixDotsSaddle;
  }

  public void setColorDotsConvex(Color color) {
    colorManager.setColorDotsConvex(color);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "dotsconvex", color);
  }

  public short getColixDotsConvex() {
    return colorManager.colixDotsConvex;
  }

  public void setColorDotsConcave(Color color) {
    colorManager.setColorDotsConcave(color);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "dotsconcave", color);
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

  public void setColorMeasurement(Color c) {
    colorManager.setColorMeasurement(c);
    refresh();
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

  public void setColorTorsion(Color c) {
    colorManager.setColorTorsion(c);
    refresh();
  }
  public Color getColorTorsion() {
    return colorManager.colorTorsion;
  }

  public short getColixTorsion() {
    return colorManager.colixTorsion;
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

  public float getVectorScale() {
    return transformManager.vectorScale;
  }

  public void setVectorScale(float scale) {
    transformManager.setVectorScale(scale);
  }

  public void setVibrationScale(float scale) {
    transformManager.setVibrationScale(scale);
  }

  public float getVibrationScale() {
    return transformManager.vibrationScale;
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
    return Graphics3D.getColorFromString(colorName);
  }

  public void setSpecular(boolean specular) {
    colorManager.setSpecular(specular);
  }

  public boolean getSpecular() {
    return colorManager.getSpecular();
  }

  public void setSpecularPower(int specularPower) {
    colorManager.setSpecularPower(specularPower);
  }

  public void setAmbientPercent(int ambientPercent) {
    colorManager.setAmbientPercent(ambientPercent);
  }

  public void setDiffusePercent(int diffusePercent) {
    colorManager.setDiffusePercent(diffusePercent);
  }

  public void setSpecularPercent(int specularPercent) {
    colorManager.setSpecularPercent(specularPercent);
  }

  // x & y light source coordinates are fixed at -1,-1
  // z should be in the range 0, +/- 3 ?
  public void setLightsourceZ(float z) {
    colorManager.setLightsourceZ(z);
  }

  public int calcIntensity(float x, float y, float z) {
    return colorManager.calcIntensity(x, y, z);
  }

  public int calcSurfaceIntensity(Point3f pointA, Point3f pointB,
                                   Point3f pointC) {
    return colorManager.calcSurfaceIntensity(pointA, pointB, pointC);
  }

  public short getColixAtom(Atom atom) {
    return colorManager.getColixAtom(atom);
  }

  public short getColixAtomPalette(Atom atom, byte palette) {
    return colorManager.getColixAtomPalette(atom, palette);
  }

  public short getColixHbondType(short order) {
    return colorManager.getColixHbondType(order);
  }

  public short getColixAxes() {
    return colorManager.colixAxes;
  }

  public short getColixAxesText() {
    return colorManager.colixAxesText;
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

  public boolean isSelected(Atom atom) {
    return selectionManager.isSelected(atom.atomIndex);
  }

  public boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  public boolean hasSelectionHalo(Atom atom) {
    return
      selectionHaloEnabled &&
      !repaintManager.wireframeRotating &&
      isSelected(atom);
  }

  public boolean selectionHaloEnabled = false;
  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    if (this.selectionHaloEnabled != selectionHaloEnabled) {
      this.selectionHaloEnabled = selectionHaloEnabled;
      refresh();
    }
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

  public int getSelectionCount() {
    return selectionManager.getSelectionCount();
  }

  /****************************************************************
   * delegated to MouseManager
   ****************************************************************/

  public void setModeMouse(int modeMouse) {
    // deprecated
  }

  public Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  public void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  public int getCursorX() {
    return mouseManager.xCurrent;
  }

  public int getCursorY() {
    return mouseManager.yCurrent;
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
    /*
    System.out.println("openFile(" + name + ") thread:" + Thread.currentThread() +
                       " priority:" + Thread.currentThread().getPriority());
    */
    clear();
    forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFile(name);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openFile(" + name + ") " + ms + " ms");
  }

  public void openStringInline(String strModel) {
    clear();
    fileManager.openStringInline(strModel);
    /*return*/ getOpenFileError();
  }

  /**
   * Opens the file, given the reader.
   *
   * name is a text name of the file ... to be displayed in the window
   * no need to pass a BufferedReader ...
   * ... the FileManager will wrap a buffer around it
   */
  public void openReader(String fullPathName, String name, Reader reader) {
    clear();
    fileManager.openReader(fullPathName, name, reader);
    getOpenFileError();
  }
  
  public String getOpenFileError() {
    String errorMsg = getOpenFileError1();
//    System.gc();
//   System.runFinalization();
    return errorMsg;
  }

  public String getOpenFileError1() {
    String fullPathName = fileManager.getFullPathName();
    String fileName = fileManager.getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      notifyFileNotLoaded(fullPathName, errorMsg);
      return errorMsg;
    }
    openClientFile(fullPathName, fileName, clientFile);
    notifyFileLoaded(fullPathName, fileName,
                     modelManager.getModelSetName(), clientFile);
    return null;
  }

  /****************************************************************
   * delegated to ModelManager
   ****************************************************************/

  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    pushHoldRepaint();
    modelManager.setClientFile(fullPathName, fileName, clientFile);
    homePosition();
    if (eval != null)
      eval.clearDefinitionsAndLoadPredefined();
    // there probably needs to be a better startup mechanism for shapes
    if (modelManager.hasVibrationVectors())
      setShapeSize(JmolConstants.SHAPE_VECTORS, 1);
    
    popHoldRepaint();
  }

  public void clear() {
    repaintManager.clearAnimation();
    transformManager.clearVibration();
    modelManager.setClientFile(null, null, null);
    selectionManager.clearSelection();
    clearMeasurements();
    refresh();
  }

  public String getModelSetName() {
    return modelManager.getModelSetName();
  }

  public String getModelFileHeader() {
    return modelManager.getModelFileHeader();
  }

  public boolean haveFrame() {
    return modelManager.frame != null;
  }

  public Object getClientFile() {
    // DEPRECATED - use getExportJmolAdapter()
    return null;
  }

  public String getClientAtomStringProperty(Object clientAtomReference,
                                            String propertyName) {
    return modelManager.getClientAtomStringProperty(clientAtomReference,
                                                    propertyName);
  }

  /****************************************************************
   * This is the method that should be used to extract the model
   * data from Jmol.
   * Note that the API provided by JmolAdapter is used to
   * import data into Jmol and to export data out of Jmol.
   *
   * When exporting, a few of the methods in JmolAdapter do
   * not make sense.
   *   openBufferedReader(...)
   * Others may be implemented in the future, but are not currently
   *   all pdb specific things
   * Just pass in null for the methods that want a clientFile.
   * The main methods to use are
   *   getFrameCount(null) -> currently always returns 1
   *   getAtomCount(null, 0)
   *   getAtomIterator(null, 0)
   *   getBondIterator(null, 0)
   *
   * The AtomIterator and BondIterator return Objects as unique IDs
   * to identify the atoms.
   *   atomIterator.getAtomUid()
   *   bondIterator.getAtomUid1() & bondIterator.getAtomUid2()
   * The ExportJmolAdapter will return the 0-based atom index as
   * a boxed Integer. That means that you can cast the results to get
   * a zero-based atom index
   *  int atomIndex = ((Integer)atomIterator.getAtomUid()).intValue();
   * ...
   *  int bondedAtom1 = ((Integer)bondIterator.getAtomUid1()).intValue();
   *  int bondedAtom2 = ((Integer)bondIterator.getAtomUid2()).intValue();
   *
   * post questions to jmol-developers@lists.sf.net
   ****************************************************************/

  public JmolAdapter getExportJmolAdapter() {
    return modelManager.getExportJmolAdapter();
  }

  public Frame getFrame() {
    return modelManager.getFrame();
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

  public Vector3f getBoundingBoxCornerVector() {
    return modelManager.getBoundingBoxCornerVector();
  }

  public int getBoundingBoxCenterX() {
    // FIXME mth 2003 05 31
    // used by the labelRenderer for rendering labels away from the center
    // for now this is returning the center of the screen
    // need to transform the center of the bounding box and return that point
    return dimScreen.width / 2;
  }

  public int getBoundingBoxCenterY() {
    return dimScreen.height / 2;
  }

  public int getNumberOfFrames() {
    // FIXME
    return 1;
  }

  public int getModelCount() {
    return modelManager.getModelCount();
  }

  public int getModelNumber(int modelIndex) {
    return modelManager.getModelNumber(modelIndex);
  }

  public String getModelName(int modelIndex) {
    return modelManager.getModelName(modelIndex);
  }

  public int getModelNumberIndex(int modelNumber) {
    return modelManager.getModelNumberIndex(modelNumber);
  }

  public int getChainCount() {
    return modelManager.getChainCount();
  }

  public int getGroupCount() {
    return modelManager.getGroupCount();
  }

  public int getAtomCount() {
    return modelManager.getAtomCount();
  }

  public int getBondCount() {
    return modelManager.getBondCount();
  }

  public boolean frankClicked(int x, int y) { 
    return modelManager.frankClicked(x, y);
  }

  public int findNearestAtomIndex(int x, int y) {
    return modelManager.findNearestAtomIndex(x, y);
  }

  public BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
    return modelManager.findAtomsInRectangle(rectRubberBand);
  }

  public void setCenter(Point3f center) {
    modelManager.setRotationCenter(center);
    refresh();
  }

  public Point3f getCenter() {
    return modelManager.getRotationCenter();
  }

  public void setCenterBitSet(BitSet bsCenter) {
    modelManager.setCenterBitSet(bsCenter);
    scaleFitToScreen();
    refresh();
  }

  public void rebond() {
    modelManager.rebond();
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

  public float getCurrentSolventProbeRadius() {
    return modelManager.solventOn ? modelManager.solventProbeRadius : 0;
  }

  public void setSolventOn(boolean solventOn) {
    modelManager.setSolventOn(solventOn);
  }

  public boolean getSolventOn() {
    return modelManager.solventOn;
  }

  public int getAtomIndexFromAtomNumber(int atomNumber) {
    return modelManager.getAtomIndexFromAtomNumber(atomNumber);
  }

  public BitSet getElementsPresentBitSet() {
    return modelManager.getElementsPresentBitSet();
  }

  public BitSet getGroupsPresentBitSet() {
    return modelManager.getGroupsPresentBitSet();
  }

  /****************************************************************
   * delegated to MeasurementManager
   ****************************************************************/

  public void clearMeasurements() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
    refresh();
  }

  public int getMeasurementCount() {
    int count = getShapePropertyAsInt(JmolConstants.SHAPE_MEASURES, "count");
    return count <= 0 ? 0 : count;
  }

  public String getMeasurementStringValue(int i) {
    return
      "" + getShapeProperty(JmolConstants.SHAPE_MEASURES, "stringValue", i);
  }

  public int[] getMeasurementCountPlusIndices(int i) {
    return (int[])
      getShapeProperty(JmolConstants.SHAPE_MEASURES, "countPlusIndices", i);
  }

  public void setPendingMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "pending",
                     atomCountPlusIndices);
  }

  public void defineMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "define",
                     atomCountPlusIndices);
  }

  public void deleteMeasurement(int i) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
  }

  public void deleteMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete",
                     atomCountPlusIndices);
  }

  public void toggleMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "toggle",
                     atomCountPlusIndices);
  }

  public void clearAllMeasurements() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
  }

  /****************************************************************
   * delegated to RepaintManager
   ****************************************************************/

  public void setAnimationDirection(int direction) {// 1 or -1
    repaintManager.setAnimationDirection(direction);
  }

  public int getAnimationDirection() {
    return repaintManager.animationDirection;
  }

  public void setAnimationFps(int fps) {
    repaintManager.setAnimationFps(fps);
  }

  public int getAnimationFps() {
    return repaintManager.animationFps;
  }

  public void setAnimationReplayMode(int replay,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    // 0 means once
    // 1 means loop
    // 2 means palindrome
    repaintManager.setAnimationReplayMode(replay,
                                          firstFrameDelay, lastFrameDelay);
  }
  public int getAnimationReplayMode() {
    return repaintManager.animationReplayMode;
  }

  public void setAnimationOn(boolean animationOn) {
    boolean wasAnimating = repaintManager.animationOn;
    repaintManager.setAnimationOn(animationOn);
    if (animationOn != wasAnimating)
      refresh();
  }

  public boolean isAnimationOn() {
    return repaintManager.animationOn;
  }

  public void setAnimationNext() {
    if (repaintManager.setAnimationNext())
      refresh();
  }

  public void setAnimationPrevious() {
    if (repaintManager.setAnimationPrevious())
      refresh();
  }

  public boolean setDisplayModelIndex(int modelIndex) {
    return repaintManager.setDisplayModelIndex(modelIndex);
  }

  public int getDisplayModelIndex() {
    return repaintManager.displayModelIndex;
  }

  public FrameRenderer getFrameRenderer() {
    return repaintManager.frameRenderer;
  }

  public void setWireframeRotating(boolean wireframeRotating) {
    repaintManager.setWireframeRotating(wireframeRotating);
  }

  public boolean getWireframeRotating() {
    return repaintManager.wireframeRotating;
  }

  public int motionEventNumber;
  public boolean wasInMotion = false;

  public void setInMotion(boolean inMotion) {
	//System.out.println("viewer.setInMotion("+inMotion+")");
    if (wasInMotion ^ inMotion) {
      if (inMotion)
        ++motionEventNumber;
      repaintManager.setInMotion(inMotion);
      checkOversample();
      wasInMotion = inMotion;
    }
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

  public void forceRefresh() {
    repaintManager.forceRefresh();
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

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    manageScriptTermination();
    if (size != null)
      setScreenDimension(size);
    setRectClip(clip);
    g3d.setRectClip(rectClip);
    g3d.beginRendering(false);
    /*
    System.out.println("renderScreenImage() thread:" + Thread.currentThread() +
                       " priority:" + Thread.currentThread().getPriority());
    */
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                          repaintManager.displayModelIndex);
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized. 
    g3d.endRendering();
    Image img = g3d.getScreenImage();
    try {
      g.drawImage(img, 0, 0, null);
    } catch (NullPointerException npe) {
      System.out.println("Sun!! ... fix graphics your bugs!");
    }
    g3d.releaseScreenImage();
    notifyRepainted();
  }

  public Image getScreenImage() {
    setRectClip(null);
    g3d.setRectClip(rectClip);
    g3d.beginRendering(false);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                          repaintManager.displayModelIndex);
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
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
    if (eval == null)
      eval = new Eval(this);
    return eval;
  }

  public String evalFile(String strFilename) {
    if (strFilename != null) {
      if (! getEval().loadScriptFile(strFilename))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalString(String strScript) {
    if (strScript != null) {
      if (! getEval().loadScriptString(strScript))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public void haltScriptExecution() {
    if (eval != null)
      eval.haltExecution();
  }

  public void setColorAtomScript(byte palette, Color color) {
    setShapeColor(JmolConstants.SHAPE_BALLS, palette, color);
  }

  public void setColorBond(Color color) {
    colorManager.setColorBond(color);
    setShapeColorProperty(JmolConstants.SHAPE_STICKS, color);
  }
  
  public Color getColorBond() {
    return colorManager.colorBond;
  }

  public short getColixBond(int order) {
    if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
      return colorManager.colixHbond;
    if ((order & JmolConstants.BOND_SULFUR_MASK) != 0)
      return colorManager.colixSsbond;
    return colorManager.colixBond;
  }

  public void setSsbondsBackbone(boolean ssbondsBackbone) {
    styleManager.setSsbondsBackbone(ssbondsBackbone);
  }

  public boolean getSsbondsBackbone() {
    return styleManager.ssbondsBackbone;
  }

  public void setHbondsBackbone(boolean hbondsBackbone) {
    styleManager.setHbondsBackbone(hbondsBackbone);
  }

  public boolean getHbondsBackbone() {
    return styleManager.hbondsBackbone;
  }

  public void setMarBond(short marBond) {
    styleManager.setMarBond(marBond);
    setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2);
  }

  int hoverAtomIndex = -1;
  public void hoverOn(int atomIndex) {
    if ((eval == null || !eval.isActive()) && atomIndex != hoverAtomIndex) {
      setShapeSize(JmolConstants.SHAPE_HOVER, 1);
      setShapeProperty(JmolConstants.SHAPE_HOVER,
                       "target", new Integer(atomIndex));
      hoverAtomIndex = atomIndex;
    }
  }

  public void hoverOff() {
    if (hoverAtomIndex >= 0) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
      hoverAtomIndex = -1;
    }
  }

  public void setLabel(String strLabel) {
    if (strLabel != null) // force the class to load and display
      setShapeSize(JmolConstants.SHAPE_LABELS,
                   styleManager.pointsLabelFontSize);
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
  }

  public void togglePickingLabel(int atomIndex) {
    if (atomIndex != -1) {
      // hack to force it to load
      setShapeSize(JmolConstants.SHAPE_LABELS,
                   styleManager.pointsLabelFontSize);
      modelManager.setShapeProperty(JmolConstants.SHAPE_LABELS,
                                    "pickingLabel",
                                    new Integer(atomIndex), null);
      refresh();
    }
  }


  public BitSet getBitSetSelection() {
    return selectionManager.bsSelection;
  }

  public void setShapeShow(int shapeID, boolean show) {
    setShapeSize(shapeID, show ? -1 : 0);
  }
  
  public boolean getShapeShow(int shapeID) {
    return getShapeSize(shapeID) != 0;
  }
  
  public void setShapeSize(int shapeID, int size) {
    modelManager.setShapeSize(shapeID, size, selectionManager.bsSelection);
    refresh();
  }
  
  public int getShapeSize(int shapeID) {
    return modelManager.getShapeSize(shapeID);
  }
  
  public byte getPalette(String colorScheme) {
    if (colorScheme == null)
      return JmolConstants.PALETTE_COLOR;
    byte palette;
    for (palette = 0; palette < JmolConstants.PALETTE_MAX; ++palette)
      if (colorScheme.equals(JmolConstants.colorSchemes[palette]))
        break;
    return palette;
  }

  public void setShapeColor(int shapeID, byte palette, Color color) {
    System.out.println("setShapeColor()");
    if (palette == JmolConstants.PALETTE_COLOR) {
      System.out.println("PALETTE_COLOR");
      modelManager.setShapeProperty(shapeID, "colorScheme", null,
                                    selectionManager.bsSelection);
      modelManager.setShapeProperty(shapeID, "color", color,
                                    selectionManager.bsSelection);
    } else {
      System.out.println("other PALETTE");
      modelManager.setShapeProperty(shapeID, "colorScheme",
                                    JmolConstants.colorSchemes[palette],
                                    selectionManager.bsSelection);
    }
    refresh();
  }
  
  public void setShapeProperty(int shapeID,
                               String propertyName, Object value) {

    /*
    System.out.println("JmolViewer.setShapeProperty("+
                       JmolConstants.shapeClassBases[shapeID]+
                       "," + propertyName + "," + value + ")");
    */
    modelManager.setShapeProperty(shapeID, propertyName, value,
                                  selectionManager.bsSelection);
    refresh();
  }

  public void setShapeColorProperty(int shapeType, Color color) {
    setShapeProperty(shapeType, "color", color);
  }

  public Object getShapeProperty(int shapeType, String propertyName) {
    return modelManager.getShapeProperty(shapeType, propertyName,
                                         Integer.MIN_VALUE);
  }

  public Object getShapeProperty(int shapeType,
                                 String propertyName, int index) {
    return modelManager.getShapeProperty(shapeType, propertyName, index);
  }

  public Color getShapePropertyAsColor(int shapeID, String propertyName) {
    return (Color)getShapeProperty(shapeID, propertyName);
  }

  public int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer)
      ? Integer.MIN_VALUE : ((Integer)value).intValue();
  }

  public Color getColorShape(int shapeID) {
    return (Color)getShapeProperty(shapeID, "color");
  }

  public short getColixShape(int shapeID) {
    return g3d.getColix(getColorShape(shapeID));
  }

  public int getShapeID(String shapeName) {
    for (int i = JmolConstants.SHAPE_MAX; --i >= 0; )
      if (JmolConstants.shapeClassBases[i].equals(shapeName))
        return i;
    String msg = "Unrecognized shape name:" + shapeName;
    System.out.println(msg);
    throw new NullPointerException(msg);
  }

  public short getColix(Color color) {
    return g3d.getColix(color);
  }

  public short getColix(Object object) {
    return g3d.getColix(object);
  }

  int strandsCount = 5;

  public void setStrandsCount(int strandsCount) {
    if (strandsCount < 0)
      strandsCount = 0;
    if (strandsCount > 20)
      strandsCount = 20;
    this.strandsCount = strandsCount;
  }

  public int getStrandsCount() {
    return strandsCount;
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

  public void notifyFrameChanged(int frameNo) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFrameChanged(frameNo);
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

  private void manageScriptTermination() {
    if (eval != null && eval.hasTerminationNotification()) {
      String strErrorMessage = eval.getErrorMessage();
      int msWalltime = eval.getExecutionWalltime();
      eval.resetTerminationNotification();
      if (jmolStatusListener != null)
        jmolStatusListener.notifyScriptTermination(strErrorMessage,
                                                   msWalltime);
    }
  }

  public void scriptEcho(String strEcho) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptEcho(strEcho);
  }

  boolean debugScript = false;
  public boolean getDebugScript() {
    return debugScript;
  }
  public void setDebugScript(boolean debugScript) {
    this.debugScript = debugScript;
  }

  public void scriptStatus(String strStatus) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptStatus(strStatus);
  }

  /*
  public void measureSelection(int iatom) {
    if (jmolStatusListener != null)
      jmolStatusListener.measureSelection(iatom);
  }
  */

  public void notifyMeasurementsChanged() {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyMeasurementsChanged();
  }

  public void atomPicked(int atomIndex) {
    pickingManager.atomPicked(atomIndex);
  }

  public void notifyAtomPicked(int atomIndex) {
    if (atomIndex != -1 && jmolStatusListener != null)
      jmolStatusListener.notifyAtomPicked(atomIndex,
                                          modelManager.getAtomInfo(atomIndex));
  }

  public void setPickingMode(int pickingMode) {
    pickingManager.setPickingMode(pickingMode);
  }

  public String getAtomInfo(int atomIndex) {
    return modelManager.getAtomInfo(atomIndex);
  }

  /****************************************************************
   * mth 2003 05 31 - needs more work
   * this should be implemented using properties
   * or as a hashtable using boxed/wrapped values so that the
   * values could be shared
   ****************************************************************/

  public boolean getBooleanProperty(String key) {
    if (key.equalsIgnoreCase("wireframeRotation"))
      return getWireframeRotation();
    if (key.equalsIgnoreCase("perspectiveDepth"))
      return getPerspectiveDepth();
    if (key.equalsIgnoreCase("showAxes"))
      return getShapeShow(JmolConstants.SHAPE_AXES);
    if (key.equalsIgnoreCase("showBoundingBox"))
      return getShapeShow(JmolConstants.SHAPE_BBCAGE);
    if (key.equalsIgnoreCase("showUnitcell"))
      return getShapeShow(JmolConstants.SHAPE_UCCAGE);
    if (key.equalsIgnoreCase("showHydrogens"))
      return getShowHydrogens();
    if (key.equalsIgnoreCase("showMeasurements"))
      return getShowMeasurements();
    if (key.equalsIgnoreCase("showSelections"))
      return getSelectionHaloEnabled();
    if (key.equalsIgnoreCase("oversampleAlways"))
      return getOversampleAlwaysEnabled();
    if (key.equalsIgnoreCase("oversampleStopped"))
      return getOversampleStoppedEnabled();
    if (key.equalsIgnoreCase("axesOrientationRasmol"))
      return getAxesOrientationRasmol();
    if (key.equalsIgnoreCase("zeroBasedXyzRasmol"))
      return getZeroBasedXyzRasmol();
    if (key.equalsIgnoreCase("testFlag1"))
      return getTestFlag1();
    if (key.equalsIgnoreCase("testFlag2"))
      return getTestFlag2();
    if (key.equalsIgnoreCase("testFlag3"))
      return getTestFlag3();
    System.out.println("viewer.getBooleanProperty(" +
                       key + ") - unrecognized");
    return false;
  }

  public void setBooleanProperty(String key, boolean value) {
    refresh();
    if (key.equalsIgnoreCase("wireframeRotation"))
      { setWireframeRotation(value); return; }
    if (key.equalsIgnoreCase("perspectiveDepth"))
      { setPerspectiveDepth(value); return; }
    if (key.equalsIgnoreCase("showAxes"))
      { setShapeShow(JmolConstants.SHAPE_AXES, value); return; }
    if (key.equalsIgnoreCase("showBoundingBox"))
      { setShapeShow(JmolConstants.SHAPE_BBCAGE, value); return; }
    if (key.equalsIgnoreCase("showUnitcell"))
      { setShapeShow(JmolConstants.SHAPE_UCCAGE, value); return; }
    if (key.equalsIgnoreCase("showHydrogens"))
      { setShowHydrogens(value); return; }
    if (key.equalsIgnoreCase("showHydrogens"))
      { setShowHydrogens(value); return; }
    if (key.equalsIgnoreCase("showMeasurements"))
      { setShowMeasurements(value); return; }
    if (key.equalsIgnoreCase("showSelections"))
      { setSelectionHaloEnabled(value); return; }
    if (key.equalsIgnoreCase("oversampleAlways"))
      { setOversampleAlwaysEnabled(value); return; }
    if (key.equalsIgnoreCase("oversampleStopped"))
      { setOversampleStoppedEnabled(value); return; }
    if (key.equalsIgnoreCase("axesOrientationRasmol"))
      { setAxesOrientationRasmol(value); return; }
    if (key.equalsIgnoreCase("zeroBasedXyzRasmol"))
      { setZeroBasedXyzRasmol(value); return; }
    if (key.equalsIgnoreCase("testFlag1"))
      { setTestFlag1(value); return; }
    if (key.equalsIgnoreCase("testFlag2"))
      { setTestFlag2(value); return; }
    if (key.equalsIgnoreCase("testFlag3"))
      { setTestFlag3(value); return; }
    System.out.println("viewer.setBooleanProperty(" +
                       key + "," + value + ") - unrecognized");
  }

  public boolean testFlag1;
  public boolean testFlag2;
  public boolean testFlag3;
  void setTestFlag1(boolean value) {
    testFlag1 = value;
  }
  public boolean getTestFlag1() {
    return testFlag1;
  }
  void setTestFlag2(boolean value) {
    testFlag2 = value;
  }
  public boolean getTestFlag2() {
    return testFlag2;
  }
  void setTestFlag3(boolean value) {
    testFlag3 = value;
  }
  public boolean getTestFlag3() {
    return testFlag3;
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
   * Frame
   ****************************************************************/

  private BondIterator bondIteratorSelected(byte bondType) {
    return
      getFrame().getBondIterator(bondType, selectionManager.bsSelection);
  }

  final AtomIterator nullAtomIterator =
    new NullAtomIterator();

  static class NullAtomIterator implements AtomIterator {
    public boolean hasNext() { return false; }
    public Atom next() { return null; }
    public void release() {}
  }
  
  final BondIterator nullBondIterator =
    new NullBondIterator();
  
  static class NullBondIterator implements BondIterator {
    public boolean hasNext() { return false; }
    public Bond next() { return null; }
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

  public void setPercentVdwAtom(int percentVdwAtom) {
    styleManager.setPercentVdwAtom(percentVdwAtom);
    setShapeSize(JmolConstants.SHAPE_BALLS, -percentVdwAtom);
  }

  public int getPercentVdwAtom() {
    return styleManager.percentVdwAtom;
  }

  public short getMadAtom() {
    return (short)-styleManager.percentVdwAtom;
  }

  public short getMadBond() {
    return (short)(styleManager.marBond * 2);
  }

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

  /*
  public short getMeasurementMad() {
    return styleManager.measurementMad;
  }
  */

  public boolean setMeasureDistanceUnits(String units) {
    return styleManager.setMeasureDistanceUnits(units);
  }

  public String getMeasureDistanceUnits() {
    return styleManager.measureDistanceUnits;
  }

  public void setWireframeRotation(boolean wireframeRotation) {
    styleManager.setWireframeRotation(wireframeRotation);
    // no need to refresh since we are not currently rotating
  }

  public boolean getWireframeRotation() {
    return styleManager.wireframeRotation;
  }

  public void setJmolDefaults() {
    styleManager.setJmolDefaults();
  }

  public void setRasmolDefaults() {
    styleManager.setRasmolDefaults();
  }

  public void setShowFrank(boolean showFrank) {
    styleManager.setShowFrank(showFrank);
  }
  public boolean getShowFrank() {
    return styleManager.showFrank;
  }


  public void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    styleManager.setZeroBasedXyzRasmol(zeroBasedXyzRasmol);
  }

  public boolean getZeroBasedXyzRasmol() {
    return styleManager.zeroBasedXyzRasmol;
  }

  public void setLabelFontSize(int points) {
    styleManager.setLabelFontSize(points);
    refresh();
  }

  public void setLabelOffset(int xOffset, int yOffset) {
    styleManager.setLabelOffset(xOffset, yOffset);
    refresh();
  }

  public int getLabelOffsetX() {
    return styleManager.labelOffsetX;
  }

  public int getLabelOffsetY() {
    return styleManager.labelOffsetY;
  }

  ////////////////////////////////////////////////////////////////
  // temp manager
  ////////////////////////////////////////////////////////////////

  public Point3f[] allocTempPoints(int size) {
    return tempManager.allocTempPoints(size);
  }

  public void freeTempPoints(Point3f[] tempPoints) {
    tempManager.freeTempPoints(tempPoints);
  }

  public Point3i[] allocTempScreens(int size) {
    return tempManager.allocTempScreens(size);
  }

  public void freeTempScreens(Point3i[] tempScreens) {
    tempManager.freeTempScreens(tempScreens);
  }

  public boolean[] allocTempBooleans(int size) {
    return tempManager.allocTempBooleans(size);
  }

  public void freeTempBooleans(boolean[] tempBooleans) {
    tempManager.freeTempBooleans(tempBooleans);
  }

  ////////////////////////////////////////////////////////////////
  // font stuff
  ////////////////////////////////////////////////////////////////
  public Font3D getFont3D(int fontSize) {
    return g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
                         JmolConstants.DEFAULT_FONTSTYLE, fontSize);
  }

  public Font3D getFont3D(String fontFace, String fontStyle, int fontSize) {
    return g3d.getFont3D(fontFace, fontStyle, fontSize);
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  public String getElementSymbol(int i) {
    return modelManager.getElementSymbol(i);
  }

  public int getElementNumber(int i) {
    return modelManager.getElementNumber(i);
  }

  public String getAtomName(int i) {
    return modelManager.getAtomName(i);
  }

  public int getAtomNumber(int i) {
    return modelManager.getAtomNumber(i);
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
    return g3d.getColor(modelManager.getAtomColix(i));
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

  public short getBondOrder(int i) {
    return modelManager.getBondOrder(i);
  }

  public Color getBondColor1(int i) {
    return g3d.getColor(modelManager.getBondColix1(i));
  }

  public Color getBondColor2(int i) {
    return g3d.getColor(modelManager.getBondColix2(i));
  }

}
