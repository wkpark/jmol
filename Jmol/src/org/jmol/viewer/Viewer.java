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

import org.jmol.api.*;
import org.jmol.g3d.*;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Event;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Properties;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.net.URL;
import java.io.Reader;

/****************************************************************
 * The JmolViewer can be used to render client molecules. Clients
 * implement the JmolAdapter. JmolViewer uses this interface
 * to extract information from the client data structures and
 * render the molecule to the supplied java.awt.Component
 *
 * The JmolViewer runs on Java 1.1 virtual machines.
 * The 3d graphics rendering package is a software implementation
 * of a z-buffer. It does not use Java3D and does not use Graphics2D
 * from Java 1.2. Therefore, it is well suited to building web browser
 * applets that will run on a wide variety of system configurations.
 ****************************************************************/

final public class Viewer extends JmolViewer {

  Component awtComponent;
  ColorManager colorManager;
  TransformManager transformManager;
  SelectionManager selectionManager;
  MouseManager mouseManager;
  FileManager fileManager;
  ModelManager modelManager;
  RepaintManager repaintManager;
  StyleManager styleManager;
  TempManager tempManager;
  PickingManager pickingManager;
  Eval eval;
  Graphics3D g3d;

  JmolAdapter modelAdapter;

  String strJavaVendor;
  String strJavaVersion;
  String strOSName;
  boolean jvm11orGreater = false;
  boolean jvm12orGreater = false;
  boolean jvm14orGreater = false;

  JmolStatusListener jmolStatusListener;

  Viewer(Component awtComponent,
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

  public static JmolViewer allocateViewer(Component awtComponent,
                                          JmolAdapter modelAdapter) {
    return new Viewer(awtComponent, modelAdapter);
  }

  public Component getAwtComponent() {
    return awtComponent;
  }

  public boolean handleOldJvm10Event(Event e) {
    return mouseManager.handleOldJvm10Event(e);
  }

  public void homePosition() {
    setCenter(null);
    transformManager.homePosition();
    refresh();
  }

  final Hashtable imageCache = new Hashtable();
  void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }

  void logError(String strMsg) {
    System.out.println(strMsg);
  }

  /////////////////////////////////////////////////////////////////
  // delegated to TransformManager
  /////////////////////////////////////////////////////////////////

  void rotateXYBy(int xDelta, int yDelta) {
    transformManager.rotateXYBy(xDelta, yDelta);
    refresh();
  }

  void rotateZBy(int zDelta) {
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
  void rotateToZ(int angleDegrees) {
    rotateToZ(angleDegrees * radiansPerDegree);
  }

  void rotateXRadians(float angleRadians) {
    transformManager.rotateXRadians(angleRadians);
    refresh();
  }
  void rotateYRadians(float angleRadians) {
    transformManager.rotateYRadians(angleRadians);
    refresh();
  }
  void rotateZRadians(float angleRadians) {
    transformManager.rotateZRadians(angleRadians);
    refresh();
  }
  void rotateXDegrees(float angleDegrees) {
    rotateXRadians(angleDegrees * radiansPerDegree);
  }
  void rotateYDegrees(float angleDegrees) {
    rotateYRadians(angleDegrees * radiansPerDegree);
  }
  void rotateZDegrees(float angleDegrees) {
    rotateZRadians(angleDegrees * radiansPerDegree);
  }
  void rotateZDegreesScript(float angleDegrees) {
    transformManager.rotateZRadiansScript(angleDegrees * radiansPerDegree);
    refresh();
  }

  final static float radiansPerDegree = (float)(2 * Math.PI / 360);
  final static float degreesPerRadian = (float)(360 / (2 * Math.PI));

  void rotate(AxisAngle4f axisAngle) {
    transformManager.rotate(axisAngle);
    refresh();
  }

  void rotateAxisAngle(float x, float y, float z, float degrees) {
    transformManager.rotateAxisAngle(x, y, z, degrees);
  }

  void rotateTo(float xAxis, float yAxis, float zAxis, float degrees) {
    transformManager.rotateTo(xAxis, yAxis, zAxis, degrees);
  }

  void rotateTo(AxisAngle4f axisAngle) {
    transformManager.rotateTo(axisAngle);
  }

  void translateXYBy(int xDelta, int yDelta) {
    transformManager.translateXYBy(xDelta, yDelta);
    refresh();
  }

  void translateToXPercent(int percent) {
    transformManager.translateToXPercent(percent);
    refresh();
  }

  void translateToYPercent(int percent) {
    transformManager.translateToYPercent(percent);
    refresh();
  }

  void translateToZPercent(int percent) {
    transformManager.translateToZPercent(percent);
    refresh();
  }

  int getTranslationXPercent() {
    return transformManager.getTranslationXPercent();
  }

  int getTranslationYPercent() {
    return transformManager.getTranslationYPercent();
  }

  int getTranslationZPercent() {
    return transformManager.getTranslationZPercent();
  }

  void translateByXPercent(int percent) {
    translateToXPercent(getTranslationXPercent() + percent);
  }

  void translateByYPercent(int percent) {
    translateToYPercent(getTranslationYPercent() + percent);
  }

  void translateByZPercent(int percent) {
    translateToZPercent(getTranslationZPercent() + percent);
  }

  void zoomBy(int pixels) {
    transformManager.zoomBy(pixels);
    refresh();
  }

  public int getZoomPercent() {
    return transformManager.zoomPercent;
  }

  int getZoomPercentSetting() {
    return transformManager.zoomPercentSetting;
  }

  void zoomToPercent(int percent) {
    transformManager.zoomToPercent(percent);
    refresh();
  }

  void zoomByPercent(int percent) {
    transformManager.zoomByPercent(percent);
    refresh();
  }

  void setZoomEnabled(boolean zoomEnabled) {
    transformManager.setZoomEnabled(zoomEnabled);
    refresh();
  }

  boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  int getSlabPercentSetting() {
    return transformManager.slabPercentSetting;
  }

  void slabBy(int pixels) {
    transformManager.slabBy(pixels);
    refresh();
  }

  void slabToPercent(int percentSlab) {
    transformManager.slabToPercent(percentSlab);
    refresh();
  }

  void depthToPercent(int percentDepth) {
    transformManager.depthToPercent(percentDepth);
    refresh();
  }

  void slabByPercent(int percentSlab) {
    transformManager.slabByPercent(percentSlab);
    refresh();
  }

  void setSlabEnabled(boolean slabEnabled) {
    transformManager.setSlabEnabled(slabEnabled);
    refresh();
  }

  void setModeSlab(int modeSlab) {
    transformManager.setModeSlab(modeSlab);
    refresh();
  }

  int getModeSlab() {
    return transformManager.modeSlab;
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  void calcTransformMatrices() {
    transformManager.calcTransformMatrices();
  }

  Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  Point3i transformPoint(Point3f pointAngstroms,
                                Vector3f vibrationVector) {
    return transformManager.transformPoint(pointAngstroms, vibrationVector);
  }

  void transformPoint(Point3f pointAngstroms,
                             Vector3f vibrationVector, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, vibrationVector,
                                    pointScreen);
  }

  void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  void transformPoint(Point3f pointAngstroms, Point3f pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  void transformPoints(Point3f[] pointsAngstroms,
                              Point3i[] pointsScreens) {
    transformManager.transformPoints(pointsAngstroms.length,
                                     pointsAngstroms, pointsScreens);
  }

  void transformVector(Vector3f vectorAngstroms,
                              Vector3f vectorTransformed) {
    transformManager.transformVector(vectorAngstroms, vectorTransformed);
  }

  float getScalePixelsPerAngstrom() {
    return transformManager.scalePixelsPerAngstrom;
  }

  float scaleToScreen(int z, float sizeAngstroms) {
    return transformManager.scaleToScreen(z, sizeAngstroms);
  }

  short scaleToScreen(int z, int milliAngstroms) {
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  float scaleToPerspective(int z, float sizeAngstroms) {
    return transformManager.scaleToPerspective(z, sizeAngstroms);
  }

  void scaleFitToScreen() {
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

  void setCameraDepth(float depth) {
    transformManager.setCameraDepth(depth);
  }

  float getCameraDepth() {
    return transformManager.cameraDepth;
  }

  void checkCameraDistance() {
    if (transformManager.increaseRotationRadius)
      modelManager.
        increaseRotationRadius(transformManager.getRotationRadiusIncrease());
  }

  final Dimension dimScreen = new Dimension();
  final Rectangle rectClip = new Rectangle();

  boolean enableFullSceneAntialiasing = false;

  public void setScreenDimension(Dimension dim) {
    // There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
    // so don't try dim1.equals(dim2)
    if (dim.width == dimScreen.width && dim.height == dimScreen.height)
      return;
    dimScreen.width = dim.width;
    dimScreen.height = dim.height;
    transformManager.setScreenDimension(dim.width, dim.height);
    transformManager.scaleFitToScreen();
    g3d.setSize(dim, enableFullSceneAntialiasing);
  }

  public int getScreenWidth() {
    return dimScreen.width;
  }

  public int getScreenHeight() {
    return dimScreen.height;
  }

  void setRectClip(Rectangle clip, boolean antialiasThisFrame) {
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

  void setScaleAngstromsPerInch(float angstromsPerInch) {
    transformManager.setScaleAngstromsPerInch(angstromsPerInch);
  }

  void setSlabAndDepthValues(int slabValue, int depthValue) {
    g3d.setSlabAndDepthValues(slabValue, depthValue);
  }
  
  public void setVibrationPeriod(float period) {
    transformManager.setVibrationPeriod(period);
  }

  void setVibrationT(float t) {
    transformManager.setVibrationT(t);
  }

  float getVibrationRadians() {
    return transformManager.vibrationRadians;
  }

  void setSpinX(int value) {
    transformManager.setSpinX(value);
  }
  int getSpinX() {
    return transformManager.spinX;
  }

  void setSpinY(int value) {
    transformManager.setSpinY(value);
  }
  int getSpinY() {
    return transformManager.spinY;
  }


  void setSpinZ(int value) {
    transformManager.setSpinZ(value);
  }
  int getSpinZ() {
    return transformManager.spinZ;
  }


  void setSpinFps(int value) {
    transformManager.setSpinFps(value);
  }
  int getSpinFps() {
    return transformManager.spinFps;
  }

  void setSpinOn(boolean spinOn) {
    transformManager.setSpinOn(spinOn);
  }
  boolean getSpinOn() {
    return transformManager.spinOn;
  }

  String getOrientationText() {
    return transformManager.getOrientationText();
  }

  void getAxisAngle(AxisAngle4f axisAngle) {
    transformManager.getAxisAngle(axisAngle);
  }

  String getTransformText() {
    return transformManager.getTransformText();
  }

  void setRotation(Matrix3f matrixRotation) {
    transformManager.setRotation(matrixRotation);
  }

  void getRotation(Matrix3f matrixRotation) {
    transformManager.getRotation(matrixRotation);
  }

  /////////////////////////////////////////////////////////////////
  // delegated to ColorManager
  /////////////////////////////////////////////////////////////////
    
  public void setModeAtomColorProfile(byte palette) {
    colorManager.setPaletteDefault(palette);
    refresh();
  }

  byte getModeAtomColorProfile() {
    return colorManager.paletteDefault;
  }

  void setColorScheme(String colorScheme) {
    colorManager.setColorScheme(colorScheme);
  }

  public void setColorSelection(Color c) {
    colorManager.setColorSelection(c);
    refresh();
  }

  Color getColorSelection() {
    return colorManager.getColorSelection();
  }

  short getColixSelection() {
    return colorManager.getColixSelection();
  }

  void setColorRubberband(Color color) {
    colorManager.setColorRubberband(color);
  }

  short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  public void setColorLabel(Color color) {
    colorManager.setColorLabel(color);
    setShapeColorProperty(JmolConstants.SHAPE_LABELS, color);
    refresh();
  }
  
  void setColorDotsSaddle(Color color) {
    colorManager.setColorDotsSaddle(color);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "dotssaddle", color);
  }

  short getColixDotsSaddle() {
    return colorManager.colixDotsSaddle;
  }

  void setColorDotsConvex(Color color) {
    colorManager.setColorDotsConvex(color);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "dotsconvex", color);
  }

  short getColixDotsConvex() {
    return colorManager.colixDotsConvex;
  }

  void setColorDotsConcave(Color color) {
    colorManager.setColorDotsConcave(color);
    setShapeProperty(JmolConstants.SHAPE_DOTS, "dotsconcave", color);
  }

  short getColixDotsConcave() {
    return colorManager.colixDotsConcave;
  }
  
  public Color getColorLabel() {
    return colorManager.colorLabel;
  }

  short getColixLabel() {
    return colorManager.colixLabel;
  }

  public void setColorMeasurement(Color c) {
    colorManager.setColorMeasurement(c);
    refresh();
  }

  public Color getColorMeasurement() {
    return colorManager.colorDistance;
  }

  void setColorDistance(Color c) {
    colorManager.setColorDistance(c);
    refresh();
  }

  Color getColorDistance() {
    return colorManager.colorDistance;
  }

  short getColixDistance() {
    return colorManager.colixDistance;
  }

  void setColorAngle(Color c) {
    colorManager.setColorAngle(c);
    refresh();
  }

  Color getColorAngle() {
    return colorManager.colorAngle;
  }

  short getColixAngle() {
    return colorManager.colixAngle;
  }

  void setColorTorsion(Color c) {
    colorManager.setColorTorsion(c);
    refresh();
  }
  Color getColorTorsion() {
    return colorManager.colorTorsion;
  }

  short getColixTorsion() {
    return colorManager.colixTorsion;
  }

  public void setColorVector(Color c) {
    colorManager.setColorVector(c);
    refresh();
  }

  public Color getColorVector() {
    return colorManager.colorVector;
  }

  short getColixVector() {
    return colorManager.colixVector;
  }

  float getVectorScale() {
    return transformManager.vectorScale;
  }

  public void setVectorScale(float scale) {
    transformManager.setVectorScale(scale);
  }

  public void setVibrationScale(float scale) {
    transformManager.setVibrationScale(scale);
  }

  float getVibrationScale() {
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

  Color getColorFromString(String colorName) {
    return Graphics3D.getColorFromString(colorName);
  }

  void setSpecular(boolean specular) {
    colorManager.setSpecular(specular);
  }

  boolean getSpecular() {
    return colorManager.getSpecular();
  }

  void setSpecularPower(int specularPower) {
    colorManager.setSpecularPower(specularPower);
  }

  void setAmbientPercent(int ambientPercent) {
    colorManager.setAmbientPercent(ambientPercent);
  }

  void setDiffusePercent(int diffusePercent) {
    colorManager.setDiffusePercent(diffusePercent);
  }

  void setSpecularPercent(int specularPercent) {
    colorManager.setSpecularPercent(specularPercent);
  }

  // x & y light source coordinates are fixed at -1,-1
  // z should be in the range 0, +/- 3 ?
  void setLightsourceZ(float z) {
    colorManager.setLightsourceZ(z);
  }

  int calcIntensity(float x, float y, float z) {
    return colorManager.calcIntensity(x, y, z);
  }

  int calcSurfaceIntensity(Point3f pointA, Point3f pointB,
                                   Point3f pointC) {
    return colorManager.calcSurfaceIntensity(pointA, pointB, pointC);
  }

  short getColixAtom(Atom atom) {
    return colorManager.getColixAtom(atom);
  }

  short getColixAtomPalette(Atom atom, byte palette) {
    return colorManager.getColixAtomPalette(atom, palette);
  }

  short getColixHbondType(short order) {
    return colorManager.getColixHbondType(order);
  }

  short getColixAxes() {
    return colorManager.colixAxes;
  }

  short getColixAxesText() {
    return colorManager.colixAxesText;
  }

  /////////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  /////////////////////////////////////////////////////////////////
  
  void addSelection(int atomIndex) {
    selectionManager.addSelection(atomIndex);
    refresh();
  }

  void addSelection(BitSet set) {
    selectionManager.addSelection(set);
    refresh();
  }

  void toggleSelection(int atomIndex) {
    selectionManager.toggleSelection(atomIndex);
    refresh();
  }

  void setSelection(int atomIndex) {
    selectionManager.setSelection(atomIndex);
    refresh();
  }

  /*
  boolean isSelected(Atom atom) {
    return selectionManager.isSelected(atom.atomIndex);
  }
  */

  boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  boolean hasSelectionHalo(int atomIndex) {
    return
      selectionHaloEnabled &&
      !repaintManager.wireframeRotating &&
      selectionManager.isSelected(atomIndex);
  }

  boolean selectionHaloEnabled = false;
  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    if (this.selectionHaloEnabled != selectionHaloEnabled) {
      this.selectionHaloEnabled = selectionHaloEnabled;
      refresh();
    }
  }
  
  boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  private boolean bondSelectionModeOr;
  void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    this.bondSelectionModeOr = bondSelectionModeOr;
    refresh();
  }

  boolean getBondSelectionModeOr() {
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

  void setSelectionSet(BitSet set) {
    selectionManager.setSelectionSet(set);
    refresh();
  }

  void toggleSelectionSet(BitSet set) {
    selectionManager.toggleSelectionSet(set);
    refresh();
  }

  void invertSelection() {
    selectionManager.invertSelection();
    // only used from a script, so I do not think a refresh() is necessary
  }

  void excludeSelectionSet(BitSet set) {
    selectionManager.excludeSelectionSet(set);
    // only used from a script, so I do not think a refresh() is necessary
  }

  BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  int getSelectionCount() {
    return selectionManager.getSelectionCount();
  }

  /////////////////////////////////////////////////////////////////
  // delegated to MouseManager
  /////////////////////////////////////////////////////////////////

  public void setModeMouse(int modeMouse) {
    // deprecated
  }

  Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  void popupMenu(int x, int y) {
    if (jmolStatusListener != null)
      jmolStatusListener.handlePopupMenu(x, y);
  }

  int getCursorX() {
    return mouseManager.xCurrent;
  }

  int getCursorY() {
    return mouseManager.yCurrent;
  }

  /////////////////////////////////////////////////////////////////
  // delegated to FileManager
  /////////////////////////////////////////////////////////////////

  public void setAppletContext(URL documentBase, URL codeBase,
                               String appletProxy) {
    fileManager.setAppletContext(documentBase, codeBase, appletProxy);
  }

  Object getInputStreamOrErrorMessageFromName(String name) {
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
   * @param fullPathName 
   * @param name
   * @param reader
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

  String getOpenFileError1() {
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

  String getCurrentFileAsString() {
    String pathName = modelManager.getModelSetPathName();
    if (pathName == null)
      return null;
    return fileManager.getFileAsString(pathName);
  }

  String getFileAsString(String pathName) {
    return fileManager.getFileAsString(pathName);
  }

   /////////////////////////////////////////////////////////////////
   // delegated to ModelManager
   /////////////////////////////////////////////////////////////////

  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    pushHoldRepaint();
    modelManager.setClientFile(fullPathName, fileName, clientFile);
    homePosition();
    selectAll();
    if (eval != null)
      eval.clearDefinitionsAndLoadPredefined();
    // there probably needs to be a better startup mechanism for shapes
    if (modelManager.hasVibrationVectors())
      setShapeSize(JmolConstants.SHAPE_VECTORS, 1);
    setFrankOn(styleManager.frankOn);

    popHoldRepaint();
  }

  void clear() {
    repaintManager.clearAnimation();
    transformManager.clearVibration();
    modelManager.setClientFile(null, null, null);
    selectionManager.clearSelection();
    clearMeasurements();
    notifyFileLoaded(null, null, null, null);
    refresh();
  }

  public String getModelSetName() {
    return modelManager.getModelSetName();
  }

  public String getModelSetFileName() {
    return modelManager.getModelSetFileName();
  }

  public String getModelSetPathName() {
    return modelManager.getModelSetPathName();
  }

  String getModelSetTypeName() {
    return modelManager.getModelSetTypeName();
  }

  public boolean haveFrame() {
    return modelManager.frame != null;
  }

  Object getClientFile() {
    // DEPRECATED - use getExportJmolAdapter()
    return null;
  }

  String getClientAtomStringProperty(Object clientAtomReference,
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
   * @return A JmolAdapter
   ****************************************************************/

  JmolAdapter getExportJmolAdapter() {
    return modelManager.getExportJmolAdapter();
  }

  Frame getFrame() {
    return modelManager.getFrame();
  }

  public float getRotationRadius() {
    return modelManager.getRotationRadius();
  }

  Point3f getRotationCenter() {
    return modelManager.getRotationCenter();
  }

  Point3f getBoundingBoxCenter() {
    return modelManager.getBoundingBoxCenter();
  }

  Vector3f getBoundingBoxCornerVector() {
    return modelManager.getBoundingBoxCornerVector();
  }

  int getBoundingBoxCenterX() {
    // FIXME mth 2003 05 31
    // used by the labelRenderer for rendering labels away from the center
    // for now this is returning the center of the screen
    // need to transform the center of the bounding box and return that point
    return dimScreen.width / 2;
  }

  int getBoundingBoxCenterY() {
    return dimScreen.height / 2;
  }

  public int getModelCount() {
    return modelManager.getModelCount();
  }

  public Properties getModelSetProperties() {
    return modelManager.getModelSetProperties();
  }

  public int getModelNumber(int modelIndex) {
    return modelManager.getModelNumber(modelIndex);
  }

  public String getModelName(int modelIndex) {
    return modelManager.getModelName(modelIndex);
  }

  public Properties getModelProperties(int modelIndex) {
    return modelManager.getModelProperties(modelIndex);
  }

  public String getModelProperty(int modelIndex, String propertyName) {
    return modelManager.getModelProperty(modelIndex, propertyName);
  }

  int getModelNumberIndex(int modelNumber) {
    return modelManager.getModelNumberIndex(modelNumber);
  }

  boolean modelSetHasVibrationVectors() {
    return modelManager.modelSetHasVibrationVectors();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return modelManager.modelHasVibrationVectors(modelIndex);
  }

  public int getChainCount() {
    return modelManager.getChainCount();
  }

  public int getGroupCount() {
    return modelManager.getGroupCount();
  }

  public int getPolymerCount() {
    return modelManager.getPolymerCount();
  }

  public int getAtomCount() {
    return modelManager.getAtomCount();
  }

  public int getBondCount() {
    return modelManager.getBondCount();
  }

  boolean frankClicked(int x, int y) { 
    return modelManager.frankClicked(x, y);
  }

  int findNearestAtomIndex(int x, int y) {
    return modelManager.findNearestAtomIndex(x, y);
  }

  BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
    return modelManager.findAtomsInRectangle(rectRubberBand);
  }

  void setCenter(Point3f center) {
    modelManager.setRotationCenter(center);
    refresh();
  }

  Point3f getCenter() {
    return modelManager.getRotationCenter();
  }

  void setCenterBitSet(BitSet bsCenter) {
    modelManager.setCenterBitSet(bsCenter);
    scaleFitToScreen();
    refresh();
  }

  public void setCenterSelected() {
    setCenterBitSet(selectionManager.bsSelection);
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

  void setSolventProbeRadius(float radius) {
    modelManager.setSolventProbeRadius(radius);
  }

  float getSolventProbeRadius() {
    return modelManager.solventProbeRadius;
  }

  float getCurrentSolventProbeRadius() {
    return modelManager.solventOn ? modelManager.solventProbeRadius : 0;
  }

  void setSolventOn(boolean solventOn) {
    modelManager.setSolventOn(solventOn);
  }

  boolean getSolventOn() {
    return modelManager.solventOn;
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    return modelManager.getAtomIndexFromAtomNumber(atomNumber);
  }

  public BitSet getElementsPresentBitSet() {
    return modelManager.getElementsPresentBitSet();
  }

  public BitSet getGroupsPresentBitSet() {
    return modelManager.getGroupsPresentBitSet();
  }

  void calcMinMaxSeqcode(boolean selectedOnly) {
    modelManager.calcMinMaxSeqcode(selectedOnly
                                   ? selectionManager.bsSelection
                                   : null,
                                   rasmolHeteroSetting);
  }

  int getMinSeqcode() {
    return modelManager.getMinSeqcode();
  }

  int getMaxSeqcode() {
    return modelManager.getMaxSeqcode();
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

  void setPendingMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "pending",
                     atomCountPlusIndices);
  }

  void defineMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "define",
                     atomCountPlusIndices);
  }

  public void deleteMeasurement(int i) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
  }

  void deleteMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete",
                     atomCountPlusIndices);
  }

  void toggleMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "toggle",
                     atomCountPlusIndices);
  }

  void clearAllMeasurements() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
  }

  /////////////////////////////////////////////////////////////////
  // delegated to RepaintManager
  /////////////////////////////////////////////////////////////////

  void setAnimationDirection(int direction) {// 1 or -1
    repaintManager.setAnimationDirection(direction);
  }

  int getAnimationDirection() {
    return repaintManager.animationDirection;
  }

  public void setAnimationFps(int fps) {
    repaintManager.setAnimationFps(fps);
  }

  public int getAnimationFps() {
    return repaintManager.animationFps;
  }

  void setAnimationReplayMode(int replay,
                                     float firstFrameDelay,
                                     float lastFrameDelay) {
    // 0 means once
    // 1 means loop
    // 2 means palindrome
    repaintManager.setAnimationReplayMode(replay,
                                          firstFrameDelay, lastFrameDelay);
  }
  int getAnimationReplayMode() {
    return repaintManager.animationReplayMode;
  }

  void setAnimationOn(boolean animationOn) {
    boolean wasAnimating = repaintManager.animationOn;
    repaintManager.setAnimationOn(animationOn);
    if (animationOn != wasAnimating)
      refresh();
  }

  boolean isAnimationOn() {
    return repaintManager.animationOn;
  }

  void setAnimationNext() {
    if (repaintManager.setAnimationNext())
      refresh();
  }

  void setAnimationPrevious() {
    if (repaintManager.setAnimationPrevious())
      refresh();
  }

  boolean setDisplayModelIndex(int modelIndex) {
    return repaintManager.setDisplayModelIndex(modelIndex);
  }

  int getDisplayModelIndex() {
    return repaintManager.displayModelIndex;
  }

  FrameRenderer getFrameRenderer() {
    return repaintManager.frameRenderer;
  }

  void setWireframeRotating(boolean wireframeRotating) {
    repaintManager.setWireframeRotating(wireframeRotating);
  }

  boolean getWireframeRotating() {
    return repaintManager.wireframeRotating;
  }

  int motionEventNumber;

  public int getMotionEventNumber() {
    return motionEventNumber;
  }

  boolean wasInMotion = false;

  void setInMotion(boolean inMotion) {
	//System.out.println("viewer.setInMotion("+inMotion+")");
    if (wasInMotion ^ inMotion) {
      if (inMotion)
        ++motionEventNumber;
      repaintManager.setInMotion(inMotion);
      checkOversample();
      wasInMotion = inMotion;
    }
  }

  boolean getInMotion() {
    return repaintManager.inMotion;
  }

  Image takeSnapshot() {
    return repaintManager.takeSnapshot();
  }

  public void pushHoldRepaint() {
    repaintManager.pushHoldRepaint();
  }

  public void popHoldRepaint() {
    repaintManager.popHoldRepaint();
  }

  void forceRefresh() {
    repaintManager.forceRefresh();
  }

  public void refresh() {
    repaintManager.refresh();
  }

  void requestRepaintAndWait() {
    repaintManager.requestRepaintAndWait();
  }

  public void notifyRepainted() {
    repaintManager.notifyRepainted();
  }

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    manageScriptTermination();
    if (size != null)
      setScreenDimension(size);
    boolean antialiasThisFrame = true;
    setRectClip(clip, antialiasThisFrame);
    g3d.beginRendering(rectClip, antialiasThisFrame);
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
    boolean antialiasThisFrame = true;
    setRectClip(null, antialiasThisFrame);
    // FIXME ... rectClip is messed up for FSAA
    g3d.beginRendering(rectClip, antialiasThisFrame);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                          repaintManager.displayModelIndex);
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
  }

  void checkOversample() {
    boolean tOversample =
      (tOversampleAlways | (!repaintManager.inMotion & tOversampleStopped));
    repaintManager.setOversample(tOversample);
    transformManager.setOversample(tOversample);
  }

  void setOversample(boolean tOversample) {
    transformManager.setOversample(tOversample);
    repaintManager.setOversample(tOversample);
  }

  /////////////////////////////////////////////////////////////////
  // routines for script support
  /////////////////////////////////////////////////////////////////

  Eval getEval() {
    if (eval == null)
      eval = new Eval(this);
    return eval;
  }

  public String evalFile(String strFilename) {
    if (strFilename != null) {
      if (! getEval().loadScriptFile(strFilename, false))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalString(String strScript) {
    if (strScript != null) {
      if (! getEval().loadScriptString(strScript, false))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalStringQuiet(String strScript) {
    if (strScript != null) {
      if (! getEval().loadScriptString(strScript, true))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public void haltScriptExecution() {
    if (eval != null)
      eval.haltExecution();
  }

  void setColorAtomScript(byte palette, Color color) {
    setShapeColor(JmolConstants.SHAPE_BALLS, palette, color);
  }

  public void setColorBond(Color color) {
    colorManager.setColorBond(color);
    setShapeColorProperty(JmolConstants.SHAPE_STICKS, color);
  }
  
  public Color getColorBond() {
    return colorManager.colorBond;
  }

  short getColixBond(int order) {
    if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
      return colorManager.colixHbond;
    if ((order & JmolConstants.BOND_SULFUR_MASK) != 0)
      return colorManager.colixSsbond;
    return colorManager.colixBond;
  }

  void setSsbondsBackbone(boolean ssbondsBackbone) {
    styleManager.setSsbondsBackbone(ssbondsBackbone);
  }

  boolean getSsbondsBackbone() {
    return styleManager.ssbondsBackbone;
  }

  void setHbondsBackbone(boolean hbondsBackbone) {
    styleManager.setHbondsBackbone(hbondsBackbone);
  }

  boolean getHbondsBackbone() {
    return styleManager.hbondsBackbone;
  }

  public void setMarBond(short marBond) {
    styleManager.setMarBond(marBond);
    setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2);
  }

  int hoverAtomIndex = -1;
  void hoverOn(int atomIndex) {
    if ((eval == null || !eval.isActive()) && atomIndex != hoverAtomIndex) {
      setShapeSize(JmolConstants.SHAPE_HOVER, 1);
      setShapeProperty(JmolConstants.SHAPE_HOVER,
                       "target", new Integer(atomIndex));
      hoverAtomIndex = atomIndex;
    }
  }

  void hoverOff() {
    if (hoverAtomIndex >= 0) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
      hoverAtomIndex = -1;
    }
  }

  void setLabel(String strLabel) {
    if (strLabel != null) // force the class to load and display
      setShapeSize(JmolConstants.SHAPE_LABELS,
                   styleManager.pointsLabelFontSize);
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
  }

  void togglePickingLabel(int atomIndex) {
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


  BitSet getBitSetSelection() {
    return selectionManager.bsSelection;
  }

  void setShapeShow(int shapeID, boolean show) {
    setShapeSize(shapeID, show ? -1 : 0);
  }
  
  boolean getShapeShow(int shapeID) {
    return getShapeSize(shapeID) != 0;
  }
  
  void setShapeSize(int shapeID, int size) {
    modelManager.setShapeSize(shapeID, size, selectionManager.bsSelection);
    refresh();
  }
  
  int getShapeSize(int shapeID) {
    return modelManager.getShapeSize(shapeID);
  }
  
  byte getPalette(String colorScheme) {
    if (colorScheme == null)
      return JmolConstants.PALETTE_COLOR;
    byte palette;
    for (palette = 0; palette < JmolConstants.PALETTE_MAX; ++palette)
      if (colorScheme.equals(JmolConstants.colorSchemes[palette]))
        break;
    return palette;
  }

  void setShapeColor(int shapeID, byte palette, Color color) {
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
  
  void setShapeProperty(int shapeID,
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

  void setShapeColorProperty(int shapeType, Color color) {
    setShapeProperty(shapeType, "color", color);
  }

  Object getShapeProperty(int shapeType, String propertyName) {
    return modelManager.getShapeProperty(shapeType, propertyName,
                                         Integer.MIN_VALUE);
  }

  Object getShapeProperty(int shapeType,
                                 String propertyName, int index) {
    return modelManager.getShapeProperty(shapeType, propertyName, index);
  }

  Color getShapePropertyAsColor(int shapeID, String propertyName) {
    return (Color)getShapeProperty(shapeID, propertyName);
  }

  int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer)
      ? Integer.MIN_VALUE : ((Integer)value).intValue();
  }

  Color getColorShape(int shapeID) {
    return (Color)getShapeProperty(shapeID, "color");
  }

  short getColixShape(int shapeID) {
    return g3d.getColix(getColorShape(shapeID));
  }

  int getShapeID(String shapeName) {
    for (int i = JmolConstants.SHAPE_MAX; --i >= 0; )
      if (JmolConstants.shapeClassBases[i].equals(shapeName))
        return i;
    String msg = "Unrecognized shape name:" + shapeName;
    System.out.println(msg);
    throw new NullPointerException(msg);
  }

  short getColix(Color color) {
    return g3d.getColix(color);
  }

  short getColix(Object object) {
    return g3d.getColix(object);
  }

  int strandsCount = 5;

  void setStrandsCount(int strandsCount) {
    if (strandsCount < 0)
      strandsCount = 0;
    if (strandsCount > 20)
      strandsCount = 20;
    this.strandsCount = strandsCount;
  }

  int getStrandsCount() {
    return strandsCount;
  }

  boolean rasmolHydrogenSetting = true;
  void setRasmolHydrogenSetting(boolean b) {
    rasmolHydrogenSetting = b;
  }
  
  boolean getRasmolHydrogenSetting() {
    return rasmolHydrogenSetting;
  }

  boolean rasmolHeteroSetting = true;
  void setRasmolHeteroSetting(boolean b) {
    rasmolHeteroSetting = b;
  }
  
  boolean getRasmolHeteroSetting() {
    return rasmolHeteroSetting;
  }

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    this.jmolStatusListener = jmolStatusListener;
  }

  void notifyFrameChanged(int frameNo) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFrameChanged(frameNo);
  }

  void notifyFileLoaded(String fullPathName, String fileName,
                               String modelName, Object clientFile) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileLoaded(fullPathName, fileName,
                                          modelName, clientFile, null);
  }

  void notifyFileNotLoaded(String fullPathName, String errorMsg) {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyFileLoaded(fullPathName, null, null, null,
                                          errorMsg);
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

  void scriptEcho(String strEcho) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptEcho(strEcho);
  }

  boolean debugScript = false;
  boolean getDebugScript() {
    return debugScript;
  }
  public void setDebugScript(boolean debugScript) {
    this.debugScript = debugScript;
  }

  void scriptStatus(String strStatus) {
    if (jmolStatusListener != null)
      jmolStatusListener.scriptStatus(strStatus);
  }

  /*
  void measureSelection(int iatom) {
    if (jmolStatusListener != null)
      jmolStatusListener.measureSelection(iatom);
  }
  */

  void notifyMeasurementsChanged() {
    if (jmolStatusListener != null)
      jmolStatusListener.notifyMeasurementsChanged();
  }

  void atomPicked(int atomIndex, boolean shiftKey) {
    pickingManager.atomPicked(atomIndex, shiftKey);
  }

  void clearClickCount() {
    mouseManager.clearClickCount();
  }

  void notifyAtomPicked(int atomIndex) {
    if (atomIndex != -1 && jmolStatusListener != null)
      jmolStatusListener.notifyAtomPicked(atomIndex,
                                          modelManager.getAtomInfo(atomIndex));
  }

  public void showUrl(String urlString) {
    if (jmolStatusListener != null)
      jmolStatusListener.showUrl(urlString);
  }

  public void showConsole(boolean showConsole) {
    if (jmolStatusListener != null)
      jmolStatusListener.showConsole(showConsole);
  }

  void setPickingMode(int pickingMode) {
    pickingManager.setPickingMode(pickingMode);
  }

  String getAtomInfo(int atomIndex) {
    return modelManager.getAtomInfo(atomIndex);
  }

  /****************************************************************
   * mth 2003 05 31 - needs more work
   * this should be implemented using properties
   * or as a hashtable using boxed/wrapped values so that the
   * values could be shared
   * @param key
   * @return the boolean property
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

  boolean testFlag1;
  boolean testFlag2;
  boolean testFlag3;
  void setTestFlag1(boolean value) {
    testFlag1 = value;
  }
  boolean getTestFlag1() {
    return testFlag1;
  }
  void setTestFlag2(boolean value) {
    testFlag2 = value;
  }
  boolean getTestFlag2() {
    return testFlag2;
  }
  void setTestFlag3(boolean value) {
    testFlag3 = value;
  }
  boolean getTestFlag3() {
    return testFlag3;
  }

  /****************************************************************
   * Graphics3D
   ****************************************************************/

  boolean tOversampleStopped;
  boolean getOversampleStoppedEnabled() {
    return tOversampleStopped;
  }
  boolean tOversampleAlways;
  boolean getOversampleAlwaysEnabled() {
    return tOversampleAlways;
  }

  void setOversampleAlwaysEnabled(boolean value) {
    tOversampleAlways = value;
    checkOversample();
    refresh();
  }

  void setOversampleStoppedEnabled(boolean value) {
    tOversampleStopped = value;
    checkOversample();
    refresh();
  }

  /////////////////////////////////////////////////////////////////
  // Frame
  /////////////////////////////////////////////////////////////////
  /*
  private BondIterator bondIteratorSelected(byte bondType) {
    return
      getFrame().getBondIterator(bondType, selectionManager.bsSelection);
  }
  */
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

  /////////////////////////////////////////////////////////////////
  // delegated to StyleManager
  /////////////////////////////////////////////////////////////////

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

  public void setFrankOn(boolean frankOn) {
    styleManager.setFrankOn(frankOn);
    setShapeSize(JmolConstants.SHAPE_FRANK, frankOn ? -1 : 0);
  }

  boolean getFrankOn() {
    return styleManager.frankOn;
  }

  public int getPercentVdwAtom() {
    return styleManager.percentVdwAtom;
  }

  short getMadAtom() {
    return (short)-styleManager.percentVdwAtom;
  }

  public short getMadBond() {
    return (short)(styleManager.marBond * 2);
  }

  void setModeMultipleBond(byte modeMultipleBond) {
    styleManager.setModeMultipleBond(modeMultipleBond);
    refresh();
  }

  byte getModeMultipleBond() {
    return styleManager.modeMultipleBond;
  }

  void setShowMultipleBonds(boolean showMultipleBonds) {
    styleManager.setShowMultipleBonds(showMultipleBonds);
    refresh();
  }

  boolean getShowMultipleBonds() {
    return styleManager.showMultipleBonds;
  }

  public void setShowHydrogens(boolean showHydrogens) {
    styleManager.setShowHydrogens(showHydrogens);
    refresh();
  }

  public boolean getShowHydrogens() {
    return styleManager.showHydrogens;
  }

  public void setShowBbcage(boolean showBbcage) {
    setShapeShow(JmolConstants.SHAPE_BBCAGE, showBbcage);
  }

  public boolean getShowBbcage() {
    return getShapeShow(JmolConstants.SHAPE_BBCAGE);
  }

  public void setShowAxes(boolean showAxes) {
    setShapeShow(JmolConstants.SHAPE_AXES, showAxes);
  }

  public boolean getShowAxes() {
    return getShapeShow(JmolConstants.SHAPE_AXES);
  }

  public void setShowMeasurements(boolean showMeasurements) {
    styleManager.setShowMeasurements(showMeasurements);
    refresh();
  }

  public boolean getShowMeasurements() {
    return styleManager.showMeasurements;
  }

  void setShowMeasurementLabels(boolean showMeasurementLabels) {
    styleManager.setShowMeasurementLabels(showMeasurementLabels);
    refresh();
  }

  boolean getShowMeasurementLabels() {
    return styleManager.showMeasurementLabels;
  }

  /*
  short getMeasurementMad() {
    return styleManager.measurementMad;
  }
  */

  boolean setMeasureDistanceUnits(String units) {
    return styleManager.setMeasureDistanceUnits(units);
  }

  String getMeasureDistanceUnits() {
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

  void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    styleManager.setZeroBasedXyzRasmol(zeroBasedXyzRasmol);
  }

  boolean getZeroBasedXyzRasmol() {
    return styleManager.zeroBasedXyzRasmol;
  }

  void setLabelFontSize(int points) {
    styleManager.setLabelFontSize(points);
    refresh();
  }

  void setLabelOffset(int xOffset, int yOffset) {
    styleManager.setLabelOffset(xOffset, yOffset);
    refresh();
  }

  int getLabelOffsetX() {
    return styleManager.labelOffsetX;
  }

  int getLabelOffsetY() {
    return styleManager.labelOffsetY;
  }

  ////////////////////////////////////////////////////////////////
  // temp manager
  ////////////////////////////////////////////////////////////////

  Point3f[] allocTempPoints(int size) {
    return tempManager.allocTempPoints(size);
  }

  void freeTempPoints(Point3f[] tempPoints) {
    tempManager.freeTempPoints(tempPoints);
  }

  Point3i[] allocTempScreens(int size) {
    return tempManager.allocTempScreens(size);
  }

  void freeTempScreens(Point3i[] tempScreens) {
    tempManager.freeTempScreens(tempScreens);
  }

  boolean[] allocTempBooleans(int size) {
    return tempManager.allocTempBooleans(size);
  }

  void freeTempBooleans(boolean[] tempBooleans) {
    tempManager.freeTempBooleans(tempBooleans);
  }

  ////////////////////////////////////////////////////////////////
  // font stuff
  ////////////////////////////////////////////////////////////////
  Font3D getFont3D(int fontSize) {
    return g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
                         JmolConstants.DEFAULT_FONTSTYLE, fontSize);
  }

  Font3D getFont3D(String fontFace, String fontStyle, int fontSize) {
    return g3d.getFont3D(fontFace, fontStyle, fontSize);
  }

  ////////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  ////////////////////////////////////////////////////////////////

  String getElementSymbol(int i) {
    return modelManager.getElementSymbol(i);
  }

  int getElementNumber(int i) {
    return modelManager.getElementNumber(i);
  }

  public String getAtomName(int i) {
    return modelManager.getAtomName(i);
  }

  public int getAtomNumber(int i) {
    return modelManager.getAtomNumber(i);
  }

  float getAtomX(int i) {
    return modelManager.getAtomX(i);
  }

  float getAtomY(int i) {
    return modelManager.getAtomY(i);
  }

  float getAtomZ(int i) {
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

  String getAtomChain(int i) {
    return modelManager.getAtomChain(i);
  }

  String getAtomSequenceCode(int i) {
    return modelManager.getAtomSequenceCode(i);
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

  ////////////////////////////////////////////////////////////////
  //
  ////////////////////////////////////////////////////////////////

  public boolean isJvm12orGreater() {
    return jvm12orGreater;
  }

  public String getOperatingSystemName() {
    return strOSName;
  }

  public String getJavaVersion() {
    return strJavaVersion;
  }

  Graphics3D getGraphics3D() {
    return g3d;
  }

  public boolean showModelSetDownload() {
    return true;
  }
}
