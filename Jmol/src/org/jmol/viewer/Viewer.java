/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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
package org.jmol.viewer;

import org.jmol.api.*;
import org.jmol.g3d.*;

import java.awt.Graphics;
import java.awt.Image;
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

/*
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
 */

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
    jvm11orGreater =
      (strJavaVersion.compareTo("1.1") >= 0 &&
       // Netscape on MacOS does not implement 1.1 event model
       !(strJavaVendor.startsWith("Netscape") &&
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

  void translateToXPercent(float percent) {
    transformManager.translateToXPercent(percent);
    refresh();
  }

  void translateToYPercent(float percent) {
    transformManager.translateToYPercent(percent);
    refresh();
  }

  void translateToZPercent(float percent) {
    transformManager.translateToZPercent(percent);
    refresh();
  }

  float getTranslationXPercent() {
    return transformManager.getTranslationXPercent();
  }

  float getTranslationYPercent() {
    return transformManager.getTranslationYPercent();
  }

  float getTranslationZPercent() {
    return transformManager.getTranslationZPercent();
  }

  void translateByXPercent(float percent) {
    translateToXPercent(getTranslationXPercent() + percent);
  }

  void translateByYPercent(float percent) {
    translateToYPercent(getTranslationYPercent() + percent);
  }

  void translateByZPercent(float percent) {
    translateToZPercent(getTranslationZPercent() + percent);
  }

  void translateCenterTo(int x, int y) {
    transformManager.translateCenterTo(x, y);
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

  public final static int MAXIMUM_ZOOM = 200;
  public final static int MAXIMUM_ZOOM_PERCENTAGE = MAXIMUM_ZOOM * 100;

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

  boolean getZoomEnabled() {
    return transformManager.zoomEnabled;
  }

  boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  int getSlabPercentSetting() {
    return transformManager.slabPercentSetting;
  }

  void slabByPixels(int pixels) {
    transformManager.slabByPercentagePoints(pixels);
    refresh();
  }

  void depthByPixels(int pixels) {
    transformManager.depthByPercentagePoints(pixels);
    refresh();
  }

  void slabDepthByPixels(int pixels) {
    transformManager.slabDepthByPercentagePoints(pixels);
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
    int height = dim.height;
    int width = dim.width;
    if (getStereoMode() == JmolConstants.STEREO_DOUBLE)
      width = (width + 1) / 2;
    if (dimScreen.width == width && dimScreen.height == height)
      return;
    dimScreen.width = width;
    dimScreen.height = height;
    transformManager.setScreenDimension(width, height);
    transformManager.scaleFitToScreen();
    g3d.setWindowSize(width, height, enableFullSceneAntialiasing);
  }

  public int getScreenWidth() {
    return dimScreen.width;
  }

  public int getScreenHeight() {
    return dimScreen.height;
  }

  void setRectClip(Rectangle clip) {
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
    
  void setDefaultColors(String colorScheme) {
    colorManager.setDefaultColors(colorScheme);
  }

  public void setSelectionArgb(int argb) {
    colorManager.setSelectionArgb(argb);
    refresh();
  }

  int getColixArgb(short colix) {
    return g3d.getColixArgb(colix);
  }

  short getColixSelection() {
    return colorManager.getColixSelection();
  }

  void setRubberbandArgb(int argb) {
    colorManager.setRubberbandArgb(argb);
  }

  short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  void setElementArgb(int elementNumber, int argb) {
    colorManager.setElementArgb(elementNumber, argb);
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

  public void setBackgroundArgb(int argb) {
    colorManager.setBackgroundArgb(argb);
    refresh();
  }
 
  public int getBackgroundArgb() {
    return colorManager.argbBackground;
  }

  public void setColorBackground(String colorName) {
    colorManager.setColorBackground(colorName);
    refresh();
  }

  short getColixBackgroundContrast() {
    return colorManager.colixBackgroundContrast;
  }

  int getArgbFromString(String colorName) {
    return Graphics3D.getArgbFromString(colorName);
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

  short getColixAtom(Atom atom) {
    return colorManager.getColixAtom(atom);
  }

  short getColixAtomPalette(Atom atom, String palette) {
    return colorManager.getColixAtomPalette(atom, palette);
  }

  short getColixHbondType(short order) {
    return colorManager.getColixHbondType(order);
  }

  short getColixFromPalette(float val, float rangeMin, float rangeMax,
                            String palette) {
    return colorManager.getColixFromPalette(val, rangeMin, rangeMax, palette);
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

  boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  boolean hasSelectionHalo(int atomIndex) {
    return selectionHaloEnabled && selectionManager.isSelected(atomIndex);
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

  public void setSelectionSet(BitSet set) {
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

  public BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  int getSelectionCount() {
    return selectionManager.getSelectionCount();
  }

  public void addSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }
  
  public void removeSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
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
    if (! disablePopupMenu && jmolStatusListener != null)
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

  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name) {
    return fileManager.getUnzippedBufferedReaderOrErrorMessageFromName(name);
  }

  public void openFile(String name) {
    /*
    System.out.println("openFile(" + name + ") thread:" + Thread.currentThread() +
                       " priority:" + Thread.currentThread().getPriority());
    */
    clear();
    // keep old screen image while new file is being loaded
    //    forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFile(name);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openFile(" + name + ") " + ms + " ms");
  }

  public void openFiles(String modelName, String[] names) {
    clear();
    // keep old screen image while new file is being loaded
    //    forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFiles(modelName, names);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openFiles() " + ms + " ms");
  }

  public void openStringInline(String strModel) {
     clear();
     fileManager.openStringInline(strModel);
     getOpenFileError();
   }

  public void openDOM(Object DOMNode) {
    clear();
    long timeBegin = System.currentTimeMillis();
    fileManager.openDOM(DOMNode);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openDOM " + ms + " ms");
    getOpenFileError();
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
    System.gc();
  }
  
  public String getOpenFileError() {
    String errorMsg = getOpenFileError1();
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

  Point3f getBoundBoxCenter() {
    return modelManager.getBoundBoxCenter();
  }

  Vector3f getBoundBoxCornerVector() {
    return modelManager.getBoundBoxCornerVector();
  }

  int getBoundBoxCenterX() {
    // FIXME mth 2003 05 31
    // used by the labelRenderer for rendering labels away from the center
    // for now this is returning the center of the screen
    // need to transform the center of the bounding box and return that point
    return dimScreen.width / 2;
  }

  int getBoundBoxCenterY() {
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

  public int getPolymerCountInModel(int modelIndex) {
    return modelManager.getPolymerCountInModel(modelIndex);
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

  void setCenter(String relativeTo, float x, float y, float z) {
    modelManager.setRotationCenter(relativeTo, x, y, z);
    scaleFitToScreen();
  }

  void setCenterBitSet(BitSet bsCenter) {
    modelManager.setCenterBitSet(bsCenter);
    if (! friedaSwitch)
      scaleFitToScreen();
    refresh();
  }

  boolean friedaSwitch = false;

  boolean getFriedaSwitch() {
    return friedaSwitch;
  }

  void setFriedaSwitch(boolean friedaSwitch) {
    this.friedaSwitch = friedaSwitch;
  }

  public void setCenterSelected() {
    setCenterBitSet(selectionManager.bsSelection);
  }

  void setBondTolerance(float bondTolerance) {
    modelManager.setBondTolerance(bondTolerance);
    refresh();
  }

  float getBondTolerance() {
    return modelManager.bondTolerance;
  }

  void setMinBondDistance(float minBondDistance) {
    modelManager.setMinBondDistance(minBondDistance);
    refresh();
  }

  float getMinBondDistance() {
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

  void calcSelectedGroupsCount() {
    modelManager.calcSelectedGroupsCount(selectionManager.bsSelection);
  }

  void calcSelectedMonomersCount() {
    modelManager.calcSelectedMonomersCount(selectionManager.bsSelection);
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

  void setAnimationOn(boolean animationOn,int framePointer) {
    boolean wasAnimating = repaintManager.animationOn;
    System.out.println(" setAnimationOn "+wasAnimating+" "+animationOn+" "+framePointer);
    repaintManager.setAnimationOn(animationOn, framePointer);
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

  public int getDisplayModelIndex() {
    return repaintManager.displayModelIndex;
  }

  FrameRenderer getFrameRenderer() {
    return repaintManager.frameRenderer;
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
    int stereoMode = getStereoMode();
    switch (stereoMode) {
    case JmolConstants.STEREO_NONE:
      setRectClip(clip);
      render1(g, transformManager.getStereoRotationMatrix(false), false, 0, 0);
      break;
    case JmolConstants.STEREO_DOUBLE:
      setRectClip(null);
      render1(g, transformManager.getStereoRotationMatrix(false), false, 0, 0);
      render1(g, transformManager.getStereoRotationMatrix(true), false,
              dimScreen.width, 0);
      break;
    case JmolConstants.STEREO_REDCYAN:
    case JmolConstants.STEREO_REDBLUE:
    case JmolConstants.STEREO_REDGREEN:
      setRectClip(null);
      g3d.beginRendering(rectClip.x, rectClip.y,
                         rectClip.width, rectClip.height,
                         transformManager.getStereoRotationMatrix(true),
                         false);
      repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                            repaintManager.displayModelIndex);
      g3d.endRendering();
      g3d.snapshotAnaglyphChannelBytes();
      g3d.beginRendering(rectClip.x, rectClip.y,
                         rectClip.width, rectClip.height,
                         transformManager.getStereoRotationMatrix(false),
                         false);
      repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                            repaintManager.displayModelIndex);
      g3d.endRendering();
      if (stereoMode == JmolConstants.STEREO_REDCYAN)
	g3d.applyCyanAnaglyph();
      else 
        g3d.applyBlueOrGreenAnaglyph(stereoMode==JmolConstants.STEREO_REDBLUE);
      Image img = g3d.getScreenImage();
      try {
        g.drawImage(img, 0, 0, null);
      } catch (NullPointerException npe) {
        System.out.println("Sun!! ... fix graphics your bugs!");
      }
      g3d.releaseScreenImage();
      break;
    }
    notifyRepainted();
  }

  void render1(Graphics g, Matrix3f matrixRotate, 
               boolean antialias, int x, int y) {
    g3d.beginRendering(rectClip.x, rectClip.y,
                       rectClip.width, rectClip.height,
                       matrixRotate, antialias);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                          repaintManager.displayModelIndex);
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized. 
    g3d.endRendering();
    Image img = g3d.getScreenImage();
    try {
      g.drawImage(img, x, y, null);
    } catch (NullPointerException npe) {
      System.out.println("Sun!! ... fix graphics your bugs!");
    }
    g3d.releaseScreenImage();
  }

  public Image getScreenImage() {
    boolean antialiasThisFrame = true;
    setRectClip(null);
    g3d.beginRendering(rectClip.x, rectClip.y,
                       rectClip.width, rectClip.height, 
                       transformManager.getStereoRotationMatrix(false),
                       antialiasThisFrame);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
                          repaintManager.displayModelIndex);
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
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

  public boolean isScriptExecuting() {
    return eval.isScriptExecuting();
  }

  public void haltScriptExecution() {
    if (eval != null)
      eval.haltExecution();
  }

  boolean chainCaseSensitive = false;

  boolean getChainCaseSensitive() {
    return chainCaseSensitive;
  }

  void setChainCaseSensitive(boolean chainCaseSensitive) {
    this.chainCaseSensitive = chainCaseSensitive;
  }

  boolean ribbonBorder = false;
  boolean getRibbonBorder() {
    return ribbonBorder;
  }
  void setRibbonBorder(boolean borderOn) {
    this.ribbonBorder = borderOn;
  }

  boolean hideNameInPopup = false;
  boolean getHideNameInPopup() {
    return hideNameInPopup;
  }
  void setHideNameInPopup(boolean hideNameInPopup) {
    this.hideNameInPopup = hideNameInPopup;
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

  void setHbondsSolid(boolean hbondsSolid) {
    styleManager.setHbondsSolid(hbondsSolid);
  }

  boolean getHbondsSolid() {
    return styleManager.hbondsSolid;
  }

  public void setMarBond(short marBond) {
    styleManager.setMarBond(marBond);
    setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2);
  }

  int hoverAtomIndex = -1;
  void hoverOn(int atomIndex) {
    if ((eval == null || !eval.isActive()) && atomIndex != hoverAtomIndex) {
      loadShape(JmolConstants.SHAPE_HOVER);
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

  void loadShape(int shapeID) {
    modelManager.loadShape(shapeID);
  }
  
  void setShapeSize(int shapeID, int size) {
    modelManager.setShapeSize(shapeID, size, selectionManager.bsSelection);
    refresh();
  }
  
  int getShapeSize(int shapeID) {
    return modelManager.getShapeSize(shapeID);
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

  void setShapeProperty(int shapeID, String propertyName, int value) {
    setShapeProperty(shapeID, propertyName, new Integer(value));
  }

  void setShapePropertyArgb(int shapeID, String propertyName, int argb) {
    setShapeProperty(shapeID, propertyName,
                     argb == 0 ? null : new Integer(argb|0xFF000000));
  }

  void setShapeColorProperty(int shapeType, int argb) {
    setShapePropertyArgb(shapeType, "color", argb);
  }

  Object getShapeProperty(int shapeType, String propertyName) {
    return modelManager.getShapeProperty(shapeType, propertyName,
                                         Integer.MIN_VALUE);
  }

  Object getShapeProperty(int shapeType,
                                 String propertyName, int index) {
    return modelManager.getShapeProperty(shapeType, propertyName, index);
  }

  int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer)
      ? Integer.MIN_VALUE : ((Integer)value).intValue();
  }

  int getShapeID(String shapeName) {
    for (int i = JmolConstants.SHAPE_MAX; --i >= 0; )
      if (JmolConstants.shapeClassBases[i].equals(shapeName))
        return i;
    String msg = "Unrecognized shape name:" + shapeName;
    System.out.println(msg);
    throw new NullPointerException(msg);
  }

  short getColix(Object object) {
    return Graphics3D.getColix(object);
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
   * mth 2005 06 24
   * and/or these property names should be interned strings
   * so that we can just do == comparisions between strings
   ****************************************************************/

  public boolean getBooleanProperty(String key) {
    if (key.equalsIgnoreCase("perspectiveDepth"))
      return getPerspectiveDepth();
    if (key.equalsIgnoreCase("showAxes"))
      return getShapeShow(JmolConstants.SHAPE_AXES);
    if (key.equalsIgnoreCase("showBoundBox"))
      return getShapeShow(JmolConstants.SHAPE_BBCAGE);
    if (key.equalsIgnoreCase("showUnitcell"))
      return getShapeShow(JmolConstants.SHAPE_UCCAGE);
    if (key.equalsIgnoreCase("showHydrogens"))
      return getShowHydrogens();
    if (key.equalsIgnoreCase("showMeasurements"))
      return getShowMeasurements();
    if (key.equalsIgnoreCase("showSelections"))
      return getSelectionHaloEnabled();
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
    if (key.equalsIgnoreCase("testFlag4"))
      return getTestFlag4();
    if (key.equalsIgnoreCase("chainCaseSensitive"))
      return getChainCaseSensitive();
    if (key.equalsIgnoreCase("hideNameInPopup"))
      return getHideNameInPopup();
    if (key.equalsIgnoreCase("autobond"))
      return getAutoBond();
    if (key.equalsIgnoreCase("greyscaleRendering"))
      return getGreyscaleRendering();
    if (key.equalsIgnoreCase("disablePopupMenu"))
      return getDisablePopupMenu();
    System.out.println("viewer.getBooleanProperty(" +
                       key + ") - unrecognized");
    return false;
  }

  public void setBooleanProperty(String key, boolean value) {
    refresh();
    if (key.equalsIgnoreCase("perspectiveDepth"))
      { setPerspectiveDepth(value); return; }
    if (key.equalsIgnoreCase("showAxes"))
      { setShapeShow(JmolConstants.SHAPE_AXES, value); return; }
    if (key.equalsIgnoreCase("showBoundBox"))
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
    if (key.equalsIgnoreCase("axesOrientationRasmol"))
      { setAxesOrientationRasmol(value); return; }
    if (key.equalsIgnoreCase("zeroBasedXyzRasmol"))
      { setZeroBasedXyzRasmol(value); return; }
    if (key.equalsIgnoreCase("frieda"))
      { setFriedaSwitch(value); return; }
    if (key.equalsIgnoreCase("testFlag1"))
      { setTestFlag1(value); return; }
    if (key.equalsIgnoreCase("testFlag2"))
      { setTestFlag2(value); return; }
    if (key.equalsIgnoreCase("testFlag3"))
      { setTestFlag3(value); return; }
    if (key.equalsIgnoreCase("testFlag4"))
      { setTestFlag4(value); return; }
    if (key.equalsIgnoreCase("chainCaseSensitive"))
      { setChainCaseSensitive(value); return; }
    if (key.equalsIgnoreCase("ribbonBorder"))
      { setRibbonBorder(value); return; }
    if (key.equalsIgnoreCase("hideNameInPopup"))
      { setHideNameInPopup(value); return; }
    if (key.equalsIgnoreCase("autobond"))
      { setAutoBond(value); return; }
    if (key.equalsIgnoreCase("greyscaleRendering"))
      { setGreyscaleRendering(value); return; }
    if (key.equalsIgnoreCase("disablePopupMenu"))
      { setDisablePopupMenu(value); return; }
    System.out.println("viewer.setBooleanProperty(" +
                       key + "," + value + ") - unrecognized");
  }

  boolean testFlag1;
  boolean testFlag2;
  boolean testFlag3;
  boolean testFlag4;
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
  void setTestFlag4(boolean value) {
    testFlag4 = value;
  }
  boolean getTestFlag4() {
    return testFlag4;
  }

  /****************************************************************
   * Graphics3D
   ****************************************************************/

  boolean greyscaleRendering;
  void setGreyscaleRendering(boolean greyscaleRendering) {
    this.greyscaleRendering = greyscaleRendering;
    g3d.setGreyscaleMode(greyscaleRendering);
    refresh();
  }
  boolean getGreyscaleRendering() {
    return greyscaleRendering;
  }

  boolean disablePopupMenu;
  void setDisablePopupMenu(boolean disablePopupMenu) {
    this.disablePopupMenu = disablePopupMenu;
  }
  boolean getDisablePopupMenu() {
    return disablePopupMenu;
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
    public int nextIndex() { return -1; }
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
    if (! styleManager.setMeasureDistanceUnits(units))
      return false;
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "reformatDistances", null);
    return true;
  }

  String getMeasureDistanceUnits() {
    return styleManager.measureDistanceUnits;
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

  public int getAtomArgb(int i) {
    return g3d.getColixArgb(modelManager.getAtomColix(i));
  }

  String getAtomChain(int i) {
    return modelManager.getAtomChain(i);
  }

  public int getAtomModelIndex(int i) {
  	return modelManager.getAtomModelIndex(i);
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

  public int getBondArgb1(int i) {
    return g3d.getColixArgb(modelManager.getBondColix1(i));
  }

  public int getBondModelIndex(int i) {
  	return modelManager.getBondModelIndex(i);
  }
  
  public int getBondArgb2(int i) {
    return g3d.getColixArgb(modelManager.getBondColix2(i));
  }
  
  public Point3f[] getPolymerLeadMidPoints(int modelIndex, int polymerIndex) {
    return modelManager.getPolymerLeadMidPoints(modelIndex, polymerIndex);
  }
  
  ////////////////////////////////////////////////////////////////
  // stereo support
  ////////////////////////////////////////////////////////////////

  void setStereoMode(int stereoMode) {
    transformManager.setStereoMode(stereoMode);
  }

  int getStereoMode() {
    return transformManager.stereoMode;
  }

  float getStereoDegrees() {
    return transformManager.stereoDegrees;
  }

  void setStereoDegrees(float degrees) {
    transformManager.setStereoDegrees(degrees);
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

  public String getJavaVendor() {
    return strJavaVendor;
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

  String formatDecimal(float value, int decimalDigits) {
    return styleManager.formatDecimal(value, decimalDigits);
  }
}
