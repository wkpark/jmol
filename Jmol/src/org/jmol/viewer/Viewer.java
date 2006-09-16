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

import org.jmol.util.*;

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
import java.util.Vector;
import java.util.Enumeration;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.net.URL;
import java.io.Reader;

/*
 * ****************************************************************
 * The JmolViewer can be used to render client molecules. Clients implement the
 * JmolAdapter. JmolViewer uses this interface to extract information from the
 * client data structures and render the molecule to the supplied
 * java.awt.Component
 * 
 * The JmolViewer runs on Java 1.1 virtual machines. The 3d graphics rendering
 * package is a software implementation of a z-buffer. It does not use Java3D
 * and does not use Graphics2D from Java 1.2. Therefore, it is well suited to
 * building web browser applets that will run on a wide variety of system
 * configurations.
 * 
 * public here is a test for applet-applet and JS-applet communication the idea
 * being that applet.getProperty("jmolViewer") returns this Viewer object,
 * allowing direct inter-process access to public methods.
 * 
 * e.g.
 * 
 * applet.getProperty("jmolApplet").getFullPathName()
 * 
 * ****************************************************************
 */

public class Viewer extends JmolViewer {

  GlobalSettings global = new GlobalSettings();
  Component awtComponent;
  ColorManager colorManager;
  PropertyManager propertyManager;
  StatusManager statusManager;
  TransformManager transformManager;
  SelectionManager selectionManager;
  MouseManager mouseManager;
  FileManager fileManager;
  ModelManager modelManager;
  RepaintManager repaintManager;
  StyleManager styleManager;
  TempManager tempManager;
  PickingManager pickingManager;
  ScriptManager scriptManager;
  Eval eval;
  Graphics3D g3d;
  JmolAdapter modelAdapter;
  String strJavaVendor;
  String strJavaVersion;
  String strOSName;
  Hashtable appletInfo;
  String htmlName = "";
  boolean jvm11orGreater = false;
  boolean jvm12orGreater = false;
  boolean jvm14orGreater = false;

  Viewer(Component awtComponent, JmolAdapter modelAdapter) {
    this.awtComponent = awtComponent;
    this.modelAdapter = modelAdapter;
    strJavaVendor = System.getProperty("java.vendor");
    strOSName = System.getProperty("os.name");
    strJavaVersion = System.getProperty("java.version");
    // Netscape on MacOS does not implement 1.1 event model
    jvm11orGreater = (strJavaVersion.compareTo("1.1") >= 0 && !(strJavaVendor
        .startsWith("Netscape")
        && strJavaVersion.compareTo("1.1.5") <= 0 && "Mac OS".equals(strOSName)));
    jvm12orGreater = (strJavaVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJavaVersion.compareTo("1.4") >= 0);

    Logger.info(JmolConstants.copyright + "\nJmol Version " + getJmolVersion()
        + "\njava.vendor:" + strJavaVendor + "\njava.version:" + strJavaVersion
        + "\nos.name:" + strOSName);
    Logger.info(htmlName + " jvm11orGreater=" + jvm11orGreater
        + "\njvm12orGreater=" + jvm12orGreater + "\njvm14orGreater="
        + jvm14orGreater);

    g3d = new Graphics3D(awtComponent);
    eval = new Eval(this);
    statusManager = new StatusManager(this);
    scriptManager = new ScriptManager(this);
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
    propertyManager = new PropertyManager(this);
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
    if (modelManager.modelsHaveSymmetry())
      styleManager.setCrystallographicDefaults();

    refresh(1, "Viewer:homePosition()");
  }

  final Hashtable imageCache = new Hashtable();

  void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }

  Hashtable getAppletInfo() {
    Hashtable info = new Hashtable();
    info.put("htmlName", htmlName);
    info.put("version", JmolConstants.version);
    info.put("date", JmolConstants.date);
    info.put("javaVendor", strJavaVendor);
    info.put("javaVersion", strJavaVersion);
    info.put("operatingSystem", strOSName);
    return info;
  }

  final static int getShapeVisibilityFlag(int shapeID) {
    return (4 << shapeID);
  }

  String getJmolVersion() {
    return JmolConstants.version + "  " + JmolConstants.date;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  void move(Vector3f dRot, int dZoom, Vector3f dTrans, int dSlab,
            float floatSecondsTotal, int fps) {
    transformManager.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  public void moveTo(float floatSecondsTotal, Point3f pt, float degrees,
                     int zoom, int xTrans, int yTrans) {
    transformManager.moveTo(floatSecondsTotal, pt, degrees, zoom, xTrans,
        yTrans);
  }

  String getMoveToText(float timespan) {
    return transformManager.getMoveToText(timespan);
  }

  void rotateXYBy(int xDelta, int yDelta) {
    //mouse
    transformManager.rotateXYBy(xDelta, yDelta);
    refresh(1, "Viewer:rotateXYBy()");
  }

  void rotateZBy(int zDelta) {
    //mouse
    transformManager.rotateZBy(zDelta);
    refresh(1, "Viewer:rotateZBy()");
  }

  public void rotateFront() {
    //appdisplaypanel
    transformManager.rotateFront();
  }

  public void rotateToX(float angleRadians) {
    //JmolViewer
    transformManager.rotateToX(angleRadians);
  }

  public void rotateToY(float angleRadians) {
    //JmolViewer
    transformManager.rotateToY(angleRadians);
  }

  public void rotateToZ(float angleRadians) {
    //JmolViewer
    transformManager.rotateToZ(angleRadians);
  }

  final static float radiansPerDegree = (float) (2 * Math.PI / 360);
  final static float degreesPerRadian = (float) (360 / (2 * Math.PI));

  public void rotateToX(int angleDegrees) {
    //appdisplaypanel
    rotateToX(angleDegrees * radiansPerDegree);
  }

  public void rotateToY(int angleDegrees) {
    //appdisplaypanel
    rotateToY(angleDegrees * radiansPerDegree);
  }

  void rotateToZ(int angleDegrees) {
    //appdisplaypanel
    rotateToZ(angleDegrees * radiansPerDegree);
  }

  void rotateXRadians(float angleRadians) {
    //eval move
    transformManager.rotateXRadians(angleRadians);
    refresh(1, "Viewer:rotateXRadians()");
  }

  void rotateYRadians(float angleRadians) {
    //eval move
    transformManager.rotateYRadians(angleRadians);
    refresh(1, "Viewer:rotateYRadians()");
  }

  void rotateZRadians(float angleRadians) {
    //eval move
    transformManager.rotateZRadians(angleRadians);
    refresh(1, "Viewer:rotateZRadians()");
  }

  void rotateXDegrees(float angleDegrees) {
    //deprecated
    rotateXRadians(angleDegrees * radiansPerDegree);
  }

  void rotateYDegrees(float angleDegrees) {
    //deprecated
    rotateYRadians(angleDegrees * radiansPerDegree);
  }

  void rotateZDegrees(float angleDegrees) {
    //deprecated
    rotateZRadians(angleDegrees * radiansPerDegree);
  }

  void rotate(AxisAngle4f axisAngle) {
    //unused
    transformManager.rotateAxisAngle(axisAngle);
  }

  void rotateTo(float xAxis, float yAxis, float zAxis, float degrees) {
    //unused
    transformManager.rotateTo(xAxis, yAxis, zAxis, degrees);
  }

  void rotateTo(AxisAngle4f axisAngle) {
    //unused
    transformManager.rotateTo(axisAngle);
  }

  void translateXYBy(int xDelta, int yDelta) {
    transformManager.translateXYBy(xDelta, yDelta);
    refresh(1, "Viewer:translateXYBy()");
  }

  void translateToXPercent(float percent) {
    transformManager.translateToXPercent(percent);
    refresh(1, "Viewer:translateToXPercent()");
  }

  void translateToYPercent(float percent) {
    transformManager.translateToYPercent(percent);
    refresh(1, "Viewer:translateToYPercent()");
  }

  void translateToZPercent(float percent) {
    transformManager.translateToZPercent(percent);
    refresh(1, "Viewer:translateToZPercent()");
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

  String getTranslationScript() {
    return transformManager.getTranslationScript();
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
    // not called
    transformManager.translateCenterTo(x, y);
  }

  void zoomBy(int pixels) {
    transformManager.zoomBy(pixels);
    refresh(1, "Viewer:zoomBy()");
  }

  public int getZoomPercent() {
    return transformManager.zoomPercent;
  }

  int getZoomPercentSetting() {
    return transformManager.zoomPercentSetting;
  }

  public final static int MAXIMUM_ZOOM = 200;
  public final static int MAXIMUM_ZOOM_PERCENTAGE = MAXIMUM_ZOOM * 1000;

  void zoomToPercent(int percent) {
    transformManager.zoomToPercent(percent);
    refresh(1, "Viewer:zoomToPercent()");
  }

  void zoomByPercent(int percent) {
    transformManager.zoomByPercent(percent);
    refresh(1, "Viewer:zoomByPercent()");
  }

  void setZoomEnabled(boolean zoomEnabled) {
    transformManager.setZoomEnabled(zoomEnabled);
    refresh(1, "Viewer:setZoomEnabled()");
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
    refresh(0, "Viewer:slabByPixels()");
  }

  void depthByPixels(int pixels) {
    transformManager.depthByPercentagePoints(pixels);
    refresh(0, "Viewer:depthByPixels()");
  }

  void slabDepthByPixels(int pixels) {
    transformManager.slabDepthByPercentagePoints(pixels);
    refresh(0, "Viewer:slabDepthByPixels()");
  }

  void slabToPercent(int percentSlab) {
    transformManager.slabToPercent(percentSlab);
    refresh(0, "Viewer:slabToPercent()");
  }

  void depthToPercent(int percentDepth) {
    transformManager.depthToPercent(percentDepth);
    refresh(0, "Viewer:depthToPercent()");
  }

  void setSlabEnabled(boolean slabEnabled) {
    transformManager.setSlabEnabled(slabEnabled);
    refresh(0, "Viewer:setSlabEnabled()");
  }

  void setModeSlab(int modeSlab) {
    transformManager.setModeSlab(modeSlab);
    refresh(0, "Viewer:setModeSlab()");
  }

  int getModeSlab() {
    return transformManager.modeSlab;
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  void finalizeTransformParameters() {
    transformManager.finalizeTransformParameters();
  }

  Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  Point3i transformPoint(Point3f pointAngstroms, Vector3f vibrationVector) {
    return transformManager.transformPoint(pointAngstroms, vibrationVector);
  }

  void transformPoint(Point3f pointAngstroms, Vector3f vibrationVector,
                      Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, vibrationVector,
        pointScreen);
  }

  void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  void transformPoint(Point3f pointAngstroms, Point3f pointScreen) {
    transformManager.transformPoint(pointAngstroms, pointScreen);
  }

  void transformPoints(Point3f[] pointsAngstroms, Point3i[] pointsScreens) {
    transformManager.transformPoints(pointsAngstroms.length, pointsAngstroms,
        pointsScreens);
  }

  void transformVector(Vector3f vectorAngstroms, Vector3f vectorTransformed) {
    transformManager.transformVector(vectorAngstroms, vectorTransformed);
  }

  void unTransformPoint(Point3i pointScreen, Point3f pointAngstroms) {
    transformManager.unTransformPoint(pointScreen, pointAngstroms);
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

  private boolean axesAreTainted = false;

  boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  void setCrystallographicDefaults() {
    styleManager.setCrystallographicDefaults();
  }

  void checkCameraDistance() {
    if (!allowCameraMove())
      return;
    if (transformManager.getIncreaseRotationRadius())
      modelManager.increaseRotationRadius(transformManager
          .getRotationRadiusIncrease());
  }

  final Dimension dimScreen = new Dimension();

  final Rectangle rectClip = new Rectangle();

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
    g3d.setWindowSize(width, height, global.enableFullSceneAntialiasing);
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

  float getSpinX() {
    return transformManager.spinX;
  }

  void setSpinY(int value) {
    transformManager.setSpinY(value);
  }

  float getSpinY() {
    return transformManager.spinY;
  }

  void setSpinZ(int value) {
    transformManager.setSpinZ(value);
  }

  float getSpinZ() {
    return transformManager.spinZ;
  }

  void setSpinFps(int value) {
    transformManager.setSpinFps(value);
  }

  float getSpinFps() {
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

  Hashtable getOrientationInfo() {
    return transformManager.getOrientationInfo();
  }

  Matrix3f getMatrixRotate() {
    return transformManager.getMatrixRotate();
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

  // ///////////////////////////////////////////////////////////////
  // delegated to ColorManager
  // ///////////////////////////////////////////////////////////////

  void setDefaultColors(String colorScheme) {
    colorManager.setDefaultColors(colorScheme);
  }

  public void setSelectionArgb(int argb) {
    colorManager.setSelectionArgb(argb);
    refresh(0, "Viewer:setSelectionArgb()");
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
    refresh(0, "Viewer:setBackgroundArgb()");
  }

  public int getBackgroundArgb() {
    return colorManager.argbBackground;
  }

  public void setColorBackground(String colorName) {
    colorManager.setColorBackground(colorName);
    refresh(0, "Viewer:setColorBackground()");
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

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

  void removeSelection(int atomIndex) {
    selectionManager.removeSelection(atomIndex);
    refresh(0, "Viewer:removeSelection()");
  }

  void removeSelection(BitSet set) {
    selectionManager.removeSelection(set);
    refresh(0, "Viewer:removeSelection()");
  }

  void select(BitSet bs) {
    selectionManager.select(bs);
    reportSelection("" + getSelectionCount() + " atoms selected");
  }

  void addSelection(int atomIndex) {
    selectionManager.addSelection(atomIndex);
    refresh(0, "Viewer:addSelection()");
  }

  void addSelection(BitSet set) {
    selectionManager.addSelection(set);
    refresh(0, "Viewer:addSelection()");
  }

  void toggleSelection(int atomIndex) {
    selectionManager.toggleSelection(atomIndex);
    refresh(0, "Viewer:toggleSelection()");
  }

  void setSelection(int atomIndex) {
    selectionManager.setSelection(atomIndex);
    refresh(0, "Viewer:setSelection()");
  }

  boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  void reportSelection(String msg) {
    if (modelManager.getSelectionHaloEnabled())
      setTainted(true);
    scriptStatus(msg);
  }

  public void selectAll() {
    selectionManager.selectAll();
    refresh(0, "Viewer:selectAll()");
  }

  public void clearSelection() {
    selectionManager.clearSelection();
    refresh(0, "Viewer:clearSelection()");
  }

  public void setSelectionSet(BitSet set) {
    selectionManager.setSelectionSet(set);
    refresh(0, "Viewer:setSelectionSet()");
  }

  void toggleSelectionSet(BitSet set) {
    selectionManager.toggleSelectionSet(set);
    refresh(0, "Viewer:toggleSelectionSet()");
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

  public void addSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  public void removeSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  BitSet getAtomBitSet(String atomExpression) {
    return selectionManager.getAtomBitSet(atomExpression);
  }

  int firstAtomOf(BitSet bs) {
    return modelManager.firstAtomOf(bs);
  }

  Point3f getAtomSetCenter(BitSet bs) {
    return modelManager.getAtomSetCenter(bs);
  }

  Vector getAtomBitSetVector(String atomExpression) {
    return selectionManager.getAtomBitSetVector(atomExpression);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to MouseManager
  // ///////////////////////////////////////////////////////////////

  public void setModeMouse(int modeMouse) {
    // deprecated
  }

  Rectangle getRubberBandSelection() {
    return mouseManager.getRubberBand();
  }

  int getCursorX() {
    return mouseManager.xCurrent;
  }

  int getCursorY() {
    return mouseManager.yCurrent;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to FileManager
  // ///////////////////////////////////////////////////////////////

  public void setAppletContext(String htmlName, URL documentBase, URL codeBase,
                               String appletProxy) {
    this.htmlName = htmlName;
    fileManager.setAppletContext(documentBase, codeBase, appletProxy);
  }

  void setAppletProxy(String appletProxy) {
    fileManager.setAppletProxy(appletProxy);
  }

  public boolean isApplet() {
    return (htmlName.length() > 0);
  }
  
  void setDefaultDirectory(String defaultDirectory) {
    fileManager.setDefaultDirectory(defaultDirectory);
  }

  Object getInputStreamOrErrorMessageFromName(String name) {
    return fileManager.getInputStreamOrErrorMessageFromName(name);
  }

  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name) {
    return fileManager.getUnzippedBufferedReaderOrErrorMessageFromName(name);
  }

  Object getBufferedReaderForString(String string) {
    return fileManager.getBufferedReaderForString(string);
  }

  public void openFile(String name) {
    openFile(name, null);
  }

  public void openFile(String name, int[] params) {
    if (name.equalsIgnoreCase("string")) {
      openStringInline(getInlineData(), params);
      return;
    }
    clear();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFile(name, params);
    long ms = System.currentTimeMillis() - timeBegin;
    setStatusFileLoaded(1, name, "", modelManager.getModelSetName(), null, null);
    String sp = "";
    if (params != null)
      for (int i = 0; i < params.length; i++)
        sp += "," + params[i];
    Logger.info("openFile(" + name + sp + ")" + ms + " ms");
  }

  public void openFiles(String modelName, String[] names) {
    clear();
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFiles(modelName, names);
    long ms = System.currentTimeMillis() - timeBegin;
    for (int i = 0; i < names.length; i++) {
      setStatusFileLoaded(1, names[i], "", modelManager.getModelSetName(),
          null, null);
    }
    Logger.info("openFiles(" + names.length + ") " + ms + " ms");
  }

  public void openStringInline(String strModel) {
    clear();
    fileManager.openStringInline(strModel);
    setStatusFileLoaded(1, "string", "", modelManager.getModelSetName(), null,
        getOpenFileError());
  }

  public void openStringInline(String strModel, int[] params) {
    clear();
    fileManager.openStringInline(strModel, params);
    setStatusFileLoaded(1, "string", "", modelManager.getModelSetName(), null,
        getOpenFileError());
  }

  public String getInlineData() {
    return fileManager.inlineData;
  }

  public void setInlineChar(char newLine) {
    global.inlineNewlineChar = newLine;
  }

  public char getInlineChar() {
    return global.inlineNewlineChar;
  }

  public void loadInline(String strModel) {
    loadInline(strModel, global.inlineNewlineChar);
  }

  public void loadInline(String strModel, char newLine) {
    if (strModel == null)
      return;
    Logger.debug(strModel);
    if (newLine != 0) {
      int len = strModel.length();
      int i;
      for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == newLine)
        strModel = strModel.substring(i + 1);
      strModel = strModel.replace(newLine, '\n');
    }
    int[] A = global.getDefaultLattice();
    openStringInline(strModel, A);
  }

  public void openDOM(Object DOMNode) {
    clear();
    long timeBegin = System.currentTimeMillis();
    fileManager.openDOM(DOMNode);
    long ms = System.currentTimeMillis() - timeBegin;
    Logger.info("openDOM " + ms + " ms");
    setStatusFileLoaded(1, "JSNode", "", modelManager.getModelSetName(), null,
        getOpenFileError());
  }

  /**
   * Opens the file, given the reader.
   * 
   * name is a text name of the file ... to be displayed in the window no need
   * to pass a BufferedReader ... ... the FileManager will wrap a buffer around
   * it
   * 
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
    String fullPathName = getFullPathName();
    String fileName = getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      setStatusFileNotLoaded(fullPathName, errorMsg);
      return errorMsg;
    }
    openClientFile(fullPathName, fileName, clientFile);
    return null;
  }

  public String getCurrentFileAsString() {
    if (getFullPathName() == "string") {
      return fileManager.inlineData;
    }
    if (getFullPathName() == "JSNode") {
      return "<DOM NODE>";
    }
    String pathName = modelManager.getModelSetPathName();
    if (pathName == null)
      return null;
    return fileManager.getFileAsString(pathName);
  }

  public String getFileAsString(String pathName) {
    return fileManager.getFileAsString(pathName);
  }

  public String getFullPathName() {
    return fileManager.getFullPathName();
  }

  public String getFileName() {
    return fileManager.getFileName();
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ModelManager
  // ///////////////////////////////////////////////////////////////

  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    setStatusFileLoaded(2, fullPathName, fileName, modelManager
        .getModelSetName(), clientFile, null);
    pushHoldRepaint();
    modelManager.setClientFile(fullPathName, fileName, clientFile);
    homePosition();
    selectAll();
    setCenter(getRotationCenter());
    if (eval != null)
      eval.clearDefinitionsAndLoadPredefined();
    // there probably needs to be a better startup mechanism for shapes
    if (modelManager.hasVibrationVectors())
      setShapeSize(JmolConstants.SHAPE_VECTORS, styleManager.defaultVectorMad);
    setFrankOn(styleManager.frankOn);
    repaintManager.initializePointers(1);
    setDisplayModelIndex(0);
    setTainted(true);
    popHoldRepaint();
    setStatusFileLoaded(3, fullPathName, fileName, modelManager
        .getModelSetName(), clientFile, null);
  }

  void clear() {
    repaintManager.clearAnimation();
    transformManager.clearVibration();
    transformManager.clearSpin();
    modelManager.setClientFile(null, null, null);
    selectionManager.clearSelection();
    clearMeasurements();
    transformManager.setFixedRotationCenter(new Point3f(0, 0, 0));
    setStatusFileLoaded(0, null, null, null, null, null);
    refresh(0, "Viewer:clear()");
  }

  public String getModelSetName() {
    return modelManager.getModelSetName();
  }

  public String getModelSetFileName() {
    return modelManager.getModelSetFileName();
  }

  public String getSpaceGroupInfoText(String spaceGroup) {
    return modelManager.getSpaceGroupInfoText(spaceGroup);
  }

  public int getSpaceGroupIndexFromName(String spaceGroup) {
    return modelManager.getSpaceGroupIndexFromName(spaceGroup);
  }

  public String getModelSetProperty(String strProp) {
    return modelManager.getModelSetProperty(strProp);
  }

  public Object getModelSetAuxiliaryInfo(String strKey) {
    return modelManager.getModelSetAuxiliaryInfo(strKey);
  }

  public String getModelSetPathName() {
    return modelManager.getModelSetPathName();
  }

  public String getModelSetTypeName() {
    return modelManager.getModelSetTypeName();
  }

  public boolean haveFrame() {
    return modelManager.frame != null;
  }

  void clearBfactorRange() {
    modelManager.clearBfactorRange();
  }

  boolean getPrincipalAxes(int atomIndex, Vector3f z, Vector3f x,
                           String lcaoType, boolean hybridizationCompatible) {
    return modelManager.getPrincipalAxes(atomIndex, z, x, lcaoType,
        hybridizationCompatible);
  }

  BitSet getModelAtomBitSet(int modelIndex) {
    return modelManager.getModelAtomBitSet(modelIndex);
  }

  BitSet getModelBitSet(BitSet atomList) {
    return modelManager.getModelBitSet(atomList);
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

  /*****************************************************************************
   * This is the method that should be used to extract the model data from Jmol.
   * Note that the API provided by JmolAdapter is used to import data into Jmol
   * and to export data out of Jmol.
   * 
   * When exporting, a few of the methods in JmolAdapter do not make sense.
   * openBufferedReader(...) Others may be implemented in the future, but are
   * not currently all pdb specific things Just pass in null for the methods
   * that want a clientFile. The main methods to use are getFrameCount(null) ->
   * currently always returns 1 getAtomCount(null, 0) getAtomIterator(null, 0)
   * getBondIterator(null, 0)
   * 
   * The AtomIterator and BondIterator return Objects as unique IDs to identify
   * the atoms. atomIterator.getAtomUid() bondIterator.getAtomUid1() &
   * bondIterator.getAtomUid2() The ExportJmolAdapter will return the 0-based
   * atom index as a boxed Integer. That means that you can cast the results to
   * get a zero-based atom index int atomIndex =
   * ((Integer)atomIterator.getAtomUid()).intValue(); ... int bondedAtom1 =
   * ((Integer)bondIterator.getAtomUid1()).intValue(); int bondedAtom2 =
   * ((Integer)bondIterator.getAtomUid2()).intValue();
   * 
   * post questions to jmol-developers@lists.sf.net
   * 
   * @return A JmolAdapter
   ****************************************************************************/

  JmolAdapter getExportJmolAdapter() {
    return modelManager.getExportJmolAdapter();
  }

  public Frame getFrame() {
    return modelManager.getFrame();
  }

  public float getRotationRadius() {
    return modelManager.getRotationRadius();
  }

  Point3f getRotationCenter() {
    return modelManager.getRotationCenter();
  }

  Point3f getRotationCenterDefault() {
    return modelManager.getRotationCenterDefault();
  }

  Point3f getBoundBoxCenter() {
    return modelManager.getBoundBoxCenter();
  }

  Vector3f getBoundBoxCornerVector() {
    return modelManager.getBoundBoxCornerVector();
  }

  Hashtable getBoundBoxInfo() {
    return modelManager.getBoundBoxInfo();
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

  String getModelInfoAsString() {
    return modelManager.getModelInfoAsString();
  }

  String getSymmetryInfoAsString() {
    return modelManager.getSymmetryInfoAsString();
  }

  public Properties getModelSetProperties() {
    return modelManager.getModelSetProperties();
  }

  public Hashtable getModelSetAuxiliaryInfo() {
    return modelManager.getModelSetAuxiliaryInfo();
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

  public Hashtable getModelAuxiliaryInfo(int modelIndex) {
    return modelManager.getModelAuxiliaryInfo(modelIndex);
  }

  public Object getModelAuxiliaryInfo(int modelIndex, String keyName) {
    return modelManager.getModelAuxiliaryInfo(modelIndex, keyName);
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

  void convertFractionalCoordinates(Point3f pt) {
    modelManager.convertFractionalCoordinates(getDisplayModelIndex(), pt);
  }

  Point3f getCenter() {
    return modelManager.getRotationCenter();
  }

  void setCenterFromInternalRotation(Point3f center) {
    modelManager.setRotationCenterAndRadiusXYZ(center, false);
  }

  void setCenter(Point3f center) {
    center = modelManager.setRotationCenterAndRadiusXYZ(center, true);
    if (center != null)
      transformManager.setFixedRotationCenter(center);
    refresh(0, "Viewer:setCenter()");
  }

  void setCenter(String relativeTo, Point3f pt) {
    Point3f center = modelManager.setRotationCenterAndRadiusXYZ(relativeTo, pt);
    scaleFitToScreen();
    if (center != null)
      transformManager.setFixedRotationCenter(center);
    refresh(0, "Viewer:setCenter(" + relativeTo + ")");
  }

  void setCenterBitSet(BitSet bsCenter, boolean doScale) {
    Point3f center = modelManager.setCenterBitSet(bsCenter, doScale);
    transformManager.setFixedRotationCenter(center);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  public void setNewRotationCenter(String axisID) {
    //for center [line1]
    Point3f center = getDrawObjectCenter(axisID);
    if (center == null)
      return;
    setNewRotationCenter(center);
  }

  public void setNewRotationCenter(Point3f center) {
    modelManager.setNewRotationCenter(center, true);
    transformManager.setFixedRotationCenter(center);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  void moveRotationCenter(Point3f center) {
    center = modelManager.setRotationCenterAndRadiusXYZ(center, false);
    transformManager.setFixedRotationCenter(center);
    transformManager.setRotationPointXY(center);
  }

  public void setCenterSelected() {
    setCenterBitSet(selectionManager.bsSelection, true);
  }

  public void rebond() {
    modelManager.rebond();
    refresh(0, "Viewer:rebond()");
  }

  public void setBondTolerance(float bondTolerance) {
    styleManager.setBondTolerance(bondTolerance);
    refresh(0, "Viewer:setBOndTolerance()");
  }

  public float getBondTolerance() {
    return styleManager.getBondTolerance();
  }

  public void setMinBondDistance(float minBondDistance) {
    styleManager.setMinBondDistance(minBondDistance);
    refresh(0, "Viewer:setMinBondDistance()");
  }

  public float getMinBondDistance() {
    return styleManager.getMinBondDistance();
  }

  BitSet getAtomBits(String setType) {
    return modelManager.getAtomBits(setType);
  }

  BitSet getAtomBits(String setType, String specInfo) {
    return modelManager.getAtomBits(setType, specInfo);
  }

  BitSet getAtomBits(String setType, int specInfo) {
    return modelManager.getAtomBits(setType, specInfo);
  }

  BitSet getAtomBits(String setType, int[] specInfo) {
    return modelManager.getAtomBits(setType, specInfo);
  }

  BitSet getAtomsWithin(String withinWhat, BitSet bs) {
    return modelManager.getAtomsWithin(withinWhat, bs);
  }

  BitSet getAtomsWithin(String withinWhat, String specInfo, BitSet bs) {
    return modelManager.getAtomsWithin(withinWhat, specInfo, bs);
  }

  BitSet getAtomsWithin(float distance, BitSet bs) {
    return modelManager.getAtomsWithin(distance, bs);
  }

  BitSet getAtomsConnected(float min, float max, BitSet bs) {
    return modelManager.getAtomsConnected(min, max, bs);
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    return modelManager.getAtomIndexFromAtomNumber(atomNumber);
  }

  public BitSet getElementsPresentBitSet() {
    return modelManager.getElementsPresentBitSet();
  }

  BitSet getVisibleSet() {
    return modelManager.getVisibleSet();
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

  void calcSelectedMoleculesCount() {
    modelManager.calcSelectedMoleculesCount(selectionManager.bsSelection);
  }

  String getFileHeader() {
    return modelManager.getFileHeader();
  }

  String getPDBHeader() {
    return modelManager.getPDBHeader();
  }

  public Hashtable getModelInfo() {
    return modelManager.getModelInfo();
  }

  public Hashtable getAuxiliaryInfo() {
    return modelManager.getAuxiliaryInfo();
  }

  public Hashtable getShapeInfo() {
    return modelManager.getShapeInfo();
  }

  int getShapeIdFromObjectName(String objectName) {
    return modelManager.getShapeIdFromObjectName(objectName);
  }

  Vector getAllAtomInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getAllAtomInfo(bs);
  }

  Vector getAllBondInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getAllBondInfo(bs);
  }

  Vector getMoleculeInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getMoleculeInfo(bs);
  }

  public Hashtable getAllChainInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getAllChainInfo(bs);
  }

  public Hashtable getAllPolymerInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getAllPolymerInfo(bs);
  }

  public Hashtable getAllStateInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getAllStateInfo(bs);
  }

  static Hashtable dataValues = new Hashtable();

  public void setData(String type, String[] data) {
    if (type == null) {
      dataValues.clear();
      return;
    }
    dataValues.put(type, data);
  }

  public String[] getData(String type) {
    if (dataValues == null)
      return null;
    if (type.equalsIgnoreCase("types")) {
      String[] info = new String[2];
      info[0] = "types";
      info[1] = "";
      Enumeration e = (dataValues.keys());
      while (e.hasMoreElements())
        info[1] += "," + e.nextElement();
      if (info[1].length() > 0)
        info[1] = info[1].substring(1);
      return info;
    }
    return (String[]) dataValues.get(type);
  }

  public BitSet setConformation() {
    // user has selected some atoms, now this sets that as a conformation
    // with the effect of rewriting the cartoons to match

    return modelManager.setConformation(-1, getSelectionSet());
  }

  public BitSet setConformation(int conformationIndex) {
    return modelManager.setConformation(getDisplayModelIndex(),
        conformationIndex);
  }

  public void autoHbond() {
    BitSet bs = getSelectionSet();
    autoHbond(bs, bs);
  }

  public void autoHbond(BitSet bsFrom, BitSet bsTo) {
    modelManager.autoHbond(bsFrom, bsTo);
  }

  boolean hbondsAreVisible() {
    return modelManager.hbondsAreVisible(getDisplayModelIndex());
  }

  public void recalculateStructure(BitSet bsSelected) {
    modelManager.recalculateStructure(bsSelected);
  }

  /*****************************************************************************
   * delegated to MeasurementManager
   ****************************************************************************/

  public void clearMeasurements() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
    refresh(0, "Viewer:clearMeasurements()");
  }

  public int getMeasurementCount() {
    int count = getShapePropertyAsInt(JmolConstants.SHAPE_MEASURES, "count");
    return count <= 0 ? 0 : count;
  }

  public String getMeasurementStringValue(int i) {
    String str = ""
        + getShapeProperty(JmolConstants.SHAPE_MEASURES, "stringValue", i);
    return str;
  }

  Vector getMeasurementInfo() {
    return (Vector) getShapeProperty(JmolConstants.SHAPE_MEASURES, "info");
  }

  public String getMeasurementInfoAsString() {
    return (String) getShapeProperty(JmolConstants.SHAPE_MEASURES, "infostring");
  }

  public int[] getMeasurementCountPlusIndices(int i) {
    int[] List = (int[]) getShapeProperty(JmolConstants.SHAPE_MEASURES,
        "countPlusIndices", i);
    return List;
  }

  void setPendingMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "pending",
        atomCountPlusIndices);
  }

  void defineMeasurement(Vector monitorExpressions, float[] rangeMinMax,
                         boolean isDelete, boolean isAllConnected,
                         boolean isShowHide, boolean isHidden) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setConnected", new Boolean(
        isAllConnected));
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setRange", rangeMinMax);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isDelete ? "deleteVector"
        : isShowHide ? (isHidden ? "hideVector" : "showVector")
            : "defineVector", monitorExpressions);
    setStatusNewDefaultModeMeasurement("scripted", 1, "?");
  }

  public void deleteMeasurement(int i) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
  }

  void deleteMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete",
        atomCountPlusIndices);
  }

  public void showMeasurement(int[] atomCountPlusIndices, boolean isON) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isON ? "show" : "hide",
        atomCountPlusIndices);
    refresh(0, "Viewer:clearMeasurements()");
  }

  public void hideMeasurements(boolean isOFF) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "hideAll",
        new Boolean(isOFF));
    refresh(0, "setShowMeasurements()");
  }

  void toggleMeasurement(int[] atomCountPlusIndices) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "toggle",
        atomCountPlusIndices);
  }

  void clearAllMeasurements() {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to RepaintManager
  // ///////////////////////////////////////////////////////////////

  void setAnimationDirection(int direction) {// 1 or -1
    repaintManager.setAnimationDirection(direction);
  }

  int getAnimationDirection() {
    return repaintManager.animationDirection;
  }

  Hashtable getAnimationInfo() {
    return repaintManager.getAnimationInfo();
  }

  public void setAnimationFps(int fps) {
    repaintManager.setAnimationFps(fps);
  }

  public int getAnimationFps() {
    return repaintManager.animationFps;
  }

  void setAnimationReplayMode(int replay, float firstFrameDelay,
                              float lastFrameDelay) {
    // 0 means once
    // 1 means loop
    // 2 means palindrome
    repaintManager.setAnimationReplayMode(replay, firstFrameDelay,
        lastFrameDelay);
  }

  int getAnimationReplayMode() {
    return repaintManager.animationReplayMode;
  }

  void setAnimationOn(boolean animationOn) {
    boolean wasAnimating = repaintManager.animationOn;
    if (animationOn == wasAnimating)
      return;
    repaintManager.setAnimationOn(animationOn);
  }

  void resumeAnimation() {
    if (repaintManager.animationOn) {
      Logger.debug("animation is ON in resumeAnimation");
      return;
    }
    repaintManager.resumeAnimation();
    refresh(0, "Viewer:resumeAnimation()");
  }

  void pauseAnimation() {
    if (!repaintManager.animationOn || repaintManager.animationPaused) {
      return;
    }
    repaintManager.pauseAnimation();
    refresh(0, "Viewer:pauseAnimation()");
  }

  void setAnimationRange(int modelIndex1, int modelIndex2) {
    repaintManager.setAnimationRange(modelIndex1, modelIndex2);
  }

  BitSet getVisibleFramesBitSet() {
    return repaintManager.getVisibleFramesBitSet();
  }

  boolean isAnimationOn() {
    return repaintManager.animationOn;
  }

  void setAnimationNext() {
    if (repaintManager.setAnimationNext())
      refresh(0, "Viewer:setAnimationNext()");
  }

  void setAnimationPrevious() {
    if (repaintManager.setAnimationPrevious())
      refresh(0, "Viewer:setAnimationPrevious()");
  }

  void rewindAnimation() {
    repaintManager.rewindAnimation();
    refresh(0, "Viewer:rewindAnimation()");
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

  boolean wasInMotion = false;
  int motionEventNumber;

  public int getMotionEventNumber() {
    return motionEventNumber;
  }

  void setInMotion(boolean inMotion) {
    // Logger.debug("viewer.setInMotion("+inMotion+")");
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

  public void refresh() {
    repaintManager.refresh();
  }

  public void refresh(int isOrientationChange, String strWhy) {
    repaintManager.refresh();
    statusManager.setStatusViewerRefreshed(isOrientationChange, strWhy);
  }

  void requestRepaintAndWait() {
    repaintManager.requestRepaintAndWait();
  }

  public void repaintView() {
    repaintManager.repaintView();
  }

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    if (isTainted || getSlabEnabled()) {
      setModelVisibility();
      modelManager.setModelClickability();
    }
    isTainted = false;
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
    case JmolConstants.STEREO_CUSTOM:
      setRectClip(null);
      g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width,
          rectClip.height, transformManager.getStereoRotationMatrix(true),
          false);
      repaintManager.render(g3d, rectClip, modelManager.getFrame(),
          repaintManager.displayModelIndex);
      g3d.endRendering();
      g3d.snapshotAnaglyphChannelBytes();
      g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width,
          rectClip.height, transformManager.getStereoRotationMatrix(false),
          false);
      repaintManager.render(g3d, rectClip, modelManager.getFrame(),
          repaintManager.displayModelIndex);
      g3d.endRendering();
      switch (stereoMode) {
      case JmolConstants.STEREO_REDCYAN:
        g3d.applyCyanAnaglyph();
        break;
      case JmolConstants.STEREO_CUSTOM:
        g3d.applyCustomAnaglyph(transformManager.stereoColors);
        break;
      case JmolConstants.STEREO_REDBLUE:
        g3d.applyBlueAnaglyph();
        break;
      default:
        g3d.applyGreenAnaglyph();
      }
      Image img = g3d.getScreenImage();
      try {
        g.drawImage(img, 0, 0, null);
      } catch (NullPointerException npe) {
        Logger.error("Sun!! ... fix graphics your bugs!");
      }
      g3d.releaseScreenImage();
      break;
    }
    repaintView();
  }

  void render1(Graphics g, Matrix3f matrixRotate, boolean antialias, int x,
               int y) {
    g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height,
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
      Logger.error("Sun!! ... fix graphics your bugs!");
    }
    g3d.releaseScreenImage();
  }

  public Image getScreenImage() {
    boolean antialiasThisFrame = true;
    setRectClip(null);
    g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        transformManager.getStereoRotationMatrix(false), antialiasThisFrame);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
        repaintManager.displayModelIndex);
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  public String evalFile(String strFilename) {
    return scriptManager.addScript(strFilename, true, false);
  }

  public String script(String strScript) {
    return evalString(strScript);
  }

  public String evalString(String strScript) {
    if (checkHalt(strScript))
      return "script execution halted";
    return scriptManager.addScript(strScript, false, false);
  }

  public void clearScriptQueue() {
    scriptManager.clearQueue();
  }

  public boolean checkHalt(String strScript) {
    String str = "" + strScript.toLowerCase();
    if (str.startsWith("exit")) {
      haltScriptExecution();
      clearScriptQueue();
      return str.equals("exit");
    }
    if (str.startsWith("quit")) {
      haltScriptExecution();
      return str.equals("quit");
    }
    return false;
  }

  public String evalStringQuiet(String strScript) {
    if (checkHalt(strScript))
      return "script execution halted";
    return scriptManager.addScript(strScript, false, true);
  }

  /// direct no-queue use:

  public String scriptWait(String strScript) {
    scriptManager.waitForQueue();
    return evalStringWait(strScript);
  }

  public String evalStringWait(String strScript) {
    scriptManager.waitForQueue();
    return (String) evalStringWaitStatus("JSON", strScript,
        "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated", false,
        false, null);
  }

  public Object scriptWaitStatus(String strScript, String statusList) {
    scriptManager.waitForQueue();
    return evalStringWaitStatus("object", strScript, statusList, false, false,
        null);
  }

  public Object evalStringWaitStatus(String returnType, String strScript,
                                     String statusList) {
    scriptManager.waitForQueue();
    return evalStringWaitStatus(returnType, strScript, statusList, false,
        false, null);
  }

  synchronized Object evalStringWaitStatus(String returnType, String strScript,
                                           String statusList,
                                           boolean isScriptFile,
                                           boolean isQuiet, Vector tokenInfo) {
    // from the scriptManager only!
    if (checkHalt(strScript))
      return "script execution halted";
    if (strScript == null)
      return null;

    //typically request: "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated"
    //set up first with applet.jmolGetProperty("jmolStatus",statusList)

    //flush list
    String oldStatusList = statusManager.statusList;
    getProperty("String", "jmolStatus", statusList);
    boolean isOK = (tokenInfo != null ? eval
        .loadTokenInfo(strScript, tokenInfo) : isScriptFile ? eval
        .loadScriptFile(strScript, isQuiet) : eval.loadScriptString(strScript,
        isQuiet));
    if (isOK) {
      eval.runEval();
      String strErrorMessage = eval.getErrorMessage();
      int msWalltime = eval.getExecutionWalltime();
      statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
    }
    if (returnType.equalsIgnoreCase("String"))
      return eval.getErrorMessage();
    // get  Vector of Vectors of Vectors info
    Object info = getProperty(returnType, "jmolStatus", statusList);
    // reset to previous status list
    getProperty("object", "jmolStatus", oldStatusList);
    return info;
  }

  synchronized public String scriptCheck(String strScript) {
    if (strScript == null)
      return null;
    Object obj = eval.checkScript(strScript);
    if (obj instanceof String)
      return (String) obj;
    return "";
  }

  synchronized public Object compileInfo(String strScript) {
    if (strScript == null)
      return null;
    Object obj = eval.checkScript(strScript);
    if (obj instanceof String)
      return (String) obj;
    return obj;
  }

  public boolean isScriptExecuting() {
    return eval.isScriptExecuting();
  }

  public void haltScriptExecution() {
    eval.haltExecution();
  }

  void setDefaultLoadScript(String script) {
    global.defaultLoadScript = script;
  }

  String getDefaultLoadScript() {
    return global.defaultLoadScript;
  }

  String getStandardLabelFormat() {
    return styleManager.getStandardLabelFormat();
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
    if ((eval == null || !isScriptExecuting()) && atomIndex != hoverAtomIndex) {
      loadShape(JmolConstants.SHAPE_HOVER);
      setShapeProperty(JmolConstants.SHAPE_HOVER, "target", new Integer(
          atomIndex));
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
      setShapeSize(JmolConstants.SHAPE_LABELS, styleManager.pointsLabelFontSize);
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
  }

  void togglePickingLabel(int atomIndex) {
    if (atomIndex != -1) {
      // hack to force it to load
      setShapeSize(JmolConstants.SHAPE_LABELS, styleManager.pointsLabelFontSize);
      modelManager.setShapeProperty(JmolConstants.SHAPE_LABELS, "pickingLabel",
          new Integer(atomIndex), null);
      refresh(0, "Viewer:");
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
    setShapeSize(shapeID, size, selectionManager.bsSelection);
  }

  void setShapeSize(int shapeID, int size, BitSet bsAtoms) {
    modelManager.setShapeSize(shapeID, size, bsAtoms);
    refresh(0, "Viewer:setShapeSize(" + shapeID + "," + size + ")");
  }

  int getShapeSize(int shapeID) {
    return modelManager.getShapeSize(shapeID);
  }

  void setShapeProperty(int shapeID, String propertyName, Object value) {

    /*
     * Logger.debug("JmolViewer.setShapeProperty("+
     * JmolConstants.shapeClassBases[shapeID]+ "," + propertyName + "," + value +
     * ")");
     */
    modelManager.setShapeProperty(shapeID, propertyName, value,
        selectionManager.bsSelection);
    refresh(0, "Viewer:setShapeProperty()");
  }

  void setShapeProperty(int shapeID, String propertyName, int value) {
    setShapeProperty(shapeID, propertyName, new Integer(value));
  }

  void setShapePropertyArgb(int shapeID, String propertyName, int argb) {
    setShapeProperty(shapeID, propertyName, argb == 0 ? null : new Integer(
        argb | 0xFF000000));
  }

  void setShapeColorProperty(int shapeType, int argb) {
    setShapePropertyArgb(shapeType, "color", argb);
  }

  Object getShapeProperty(int shapeType, String propertyName) {
    return modelManager.getShapeProperty(shapeType, propertyName,
        Integer.MIN_VALUE);
  }

  Object getShapeProperty(int shapeType, String propertyName, int index) {
    return modelManager.getShapeProperty(shapeType, propertyName, index);
  }

  int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE
        : ((Integer) value).intValue();
  }

  int getShapeID(String shapeName) {
    for (int i = JmolConstants.SHAPE_MAX; --i >= 0;)
      if (JmolConstants.shapeClassBases[i].equals(shapeName))
        return i;
    String msg = "Unrecognized shape name:" + shapeName;
    Logger.error(msg);
    throw new NullPointerException(msg);
  }

  short getColix(Object object) {
    return Graphics3D.getColix(object);
  }

  void setRasmolHydrogenSetting(boolean b) {
    global.rasmolHydrogenSetting = b;
  }

  boolean getRasmolHydrogenSetting() {
    return global.rasmolHydrogenSetting;
  }

  void setRasmolHeteroSetting(boolean b) {
    global.rasmolHeteroSetting = b;
  }

  boolean getRasmolHeteroSetting() {
    return global.rasmolHeteroSetting;
  }

  boolean getDebugScript() {
    return global.debugScript;
  }

  public void setDebugScript(boolean debugScript) {
    global.debugScript = debugScript;
    Logger.setActiveLevel(Logger.LEVEL_DEBUG, debugScript);
  }

  void atomPicked(int atomIndex, int modifiers) {
    pickingManager.atomPicked(atomIndex, modifiers);
  }

  void clearClickCount() {
    mouseManager.clearClickCount();
  }

  void setPickingMode(int pickingMode) {
    pickingManager.setPickingMode(pickingMode);
  }

  int getPickingMode() {
    return pickingManager.getPickingMode();
  }

  boolean setPickingStyle(String style) {
    return pickingManager.setPickingStyle(style);
  }

  public String getAtomInfo(int atomIndex) {
    return modelManager.getAtomInfo(atomIndex);
  }

  public String getAtomInfoXYZ(int atomIndex) {
    return modelManager.getAtomInfoXYZ(atomIndex);
  }

  // //////////////status manager dispatch//////////////

  public Hashtable getMessageQueue() {
    return statusManager.messageQueue;
  }

  Viewer getViewer() {
    return this;
  }

  void setCallbackFunction(String callbackType, String callbackFunction) {
    statusManager.setCallbackFunction(callbackType, callbackFunction);
  }

  void setStatusAtomPicked(int atomIndex, String info) {
    statusManager.setStatusAtomPicked(atomIndex, info);
  }

  void setStatusAtomHovered(int atomIndex, String info) {
    statusManager.setStatusAtomHovered(atomIndex, info);
  }

  void setStatusNewPickingModeMeasurement(int iatom, String strMeasure) {
    statusManager.setStatusNewPickingModeMeasurement(iatom, strMeasure);
  }

  void setStatusNewDefaultModeMeasurement(String status, int count,
                                          String strMeasure) {
    statusManager.setStatusNewDefaultModeMeasurement(status, count, strMeasure);
  }

  void setStatusScriptStarted(int iscript, String script, String strError) {
    statusManager.setStatusScriptStarted(iscript, script, strError);
  }

  void setStatusUserAction(String info) {
    statusManager.setStatusUserAction(info);
  }

  Vector getStatusChanged(String statusNameList) {
    return statusManager.getStatusChanged(statusNameList);
  }

  void popupMenu(int x, int y) {
    if (global.disablePopupMenu)
      return;
    statusManager.popupMenu(x, y);
  }

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    statusManager.setJmolStatusListener(jmolStatusListener);
  }

  void setStatusFrameChanged(int frameNo) {
    statusManager.setStatusFrameChanged(frameNo);
  }

  void setStatusFileLoaded(int ptLoad, String fullPathName, String fileName,
                           String modelName, Object clientFile, String strError) {
    statusManager.setStatusFileLoaded(fullPathName, fileName, modelName,
        clientFile, strError, ptLoad);
  }

  void setStatusFileNotLoaded(String fullPathName, String errorMsg) {
    setStatusFileLoaded(-1, fullPathName, null, null, null, errorMsg);
  }

  public void scriptEcho(String strEcho) {
    statusManager.setScriptEcho(strEcho);
  }

  void scriptStatus(String strStatus) {
    statusManager.setScriptStatus(strStatus);
  }

  public void showUrl(String urlString) {
    if (urlString.indexOf(":") < 0) {
      String base = fileManager.getAppletDocumentBase();
      if (base == "")
        base = fileManager.getFullPathName();
      if (base.indexOf("/") >= 0) {
        base = base.substring(0, base.lastIndexOf("/") + 1);
      } else if (base.indexOf("\\") >= 0) {
        base = base.substring(0, base.lastIndexOf("\\") + 1);
      }
      urlString = base + urlString;
    }
    Logger.info("showUrl:" + urlString);
    statusManager.showUrl(urlString);
  }

  void showConsole(boolean showConsole) {
    statusManager.showConsole(showConsole);
  }

  /*****************************************************************************
   * mth 2003 05 31 - needs more work this should be implemented using
   * properties or as a hashtable using boxed/wrapped values so that the values
   * could be shared
   * 
   * @param key
   * @return the boolean property mth 2005 06 24 and/or these property names
   *         should be interned strings so that we can just do == comparisions
   *         between strings
   ****************************************************************************/

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
    if (key.equalsIgnoreCase("labelsFront"))
      return getLabelsFrontFlag();
    if (key.equalsIgnoreCase("labelsGroup"))
      return getLabelsGroupFlag();
    Logger.error("viewer.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  public void setBooleanProperty(String key, boolean value) {
    while (true) {
      if (key.equalsIgnoreCase("perspectiveDepth")) {
        setPerspectiveDepth(value);
        break;
      }
      if (key.equalsIgnoreCase("scriptQueue")) {
        scriptManager.setQueue(value);
        break;
      }
      if (key.equalsIgnoreCase("dotSurface")) {
        setDotSurfaceFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("dotsSelectedOnly")) {
        setDotsSelectedOnlyFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("showAxes")) { //deprecated --  see "axes" command
        setShapeShow(JmolConstants.SHAPE_AXES, value);
        break;
      }
      if (key.equalsIgnoreCase("showBoundBox")) { //deprecated -- see "boundBox"

        setShapeShow(JmolConstants.SHAPE_BBCAGE, value);
        break;
      }
      if (key.equalsIgnoreCase("showUnitcell")) { //deprecated -- see "unitcell"
        setShapeShow(JmolConstants.SHAPE_UCCAGE, value);
        break;
      }
      if (key.equalsIgnoreCase("showSelections")) { //deprecated -- see "set display"
        setSelectionHaloEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("showHydrogens")) {
        setShowHydrogens(value);
        break;
      }
      if (key.equalsIgnoreCase("showMeasurements")) {
        setShowMeasurements(value);
        break;
      }
      if (key.equalsIgnoreCase("axesOrientationRasmol")) {
        setAxesOrientationRasmol(value);
        break;
      }
      if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
        setZeroBasedXyzRasmol(value);
        return;
      }
      if (key.equalsIgnoreCase("windowCentered")) {
        setWindowCentered(value);
        break;
      }
      if (key.equalsIgnoreCase("adjustCamera")) {
        setAdjustCamera(value);
        break;
      }
      if (key.equalsIgnoreCase("rangeSelected")) {
        setRangeSelected(value);
        return;
      }
      if (key.equalsIgnoreCase("cameraMove")) {
        setAllowCameraMove(value);
        return;
      }
      if (key.equalsIgnoreCase("axesWindow")) {
        setAxesModeMolecular(false);
        break;
      }
      if (key.equalsIgnoreCase("axesMolecular")) {
        setAxesModeMolecular(value);
        break;
      }
      if (key.equalsIgnoreCase("axesUnitCell")) {
        setAxesModeUnitCell(value);
        break;
      }
      if (key.equalsIgnoreCase("measureAllModels")) {
        setMeasureAllModels(value);
        return;
      }
      if (key.equalsIgnoreCase("displayCellParameters")) {
        setDisplayCellParameters(value);
        break;
      }
      if (key.equalsIgnoreCase("statusReporting")) {
        setAllowStatusReporting(value);
        return;
      }
      if (key.equalsIgnoreCase("testFlag1")) {
        setTestFlag1(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag2")) {
        setTestFlag2(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag3")) {
        setTestFlag3(value);
        break;
      }
      if (key.equalsIgnoreCase("testFlag4")) {
        setTestFlag4(value);
        break;
      }
      if (key.equalsIgnoreCase("chainCaseSensitive")) {
        setChainCaseSensitive(value);
        return;
      }
      if (key.equalsIgnoreCase("ribbonBorder")) {
        setRibbonBorder(value);
        break;
      }
      if (key.equalsIgnoreCase("cartoonRockets")) {
        setCartoonRocketFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("hideNameInPopup")) {
        setHideNameInPopup(value);
        return;
      }
      if (key.equalsIgnoreCase("autobond")) {
        setAutoBond(value);
        return;
      }
      if (key.equalsIgnoreCase("greyscaleRendering")) {
        setGreyscaleRendering(value);
        break;
      }
      if (key.equalsIgnoreCase("labelsFront")) {
        setLabelsFrontFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("labelsGroup")) {
        setLabelsGroupFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("disablePopupMenu")) {
        setDisablePopupMenu(value);
        return;
      }
      Logger.error("viewer.setBooleanProperty(" + key + "," + value
          + ") - unrecognized SET option");
      scriptStatus("Script ERROR: unrecognized SET option: set " + key);
      return;
    }
    setTainted(true);
    refresh(0, "viewer.setBooleanProperty");
  }

  ////////  flags and settings ////////

  boolean getDotSurfaceFlag() {
    return global.dotSurfaceFlag;
  }

  void setDotSurfaceFlag(boolean TF) {
    global.dotSurfaceFlag = TF;
  }

  boolean getDotsSelectedOnlyFlag() {
    return global.dotsSelectedOnlyFlag;
  }

  void setDotsSelectedOnlyFlag(boolean TF) {
    global.dotsSelectedOnlyFlag = TF;
  }

  boolean isRangeSelected() {
    return global.rangeSelected;
  }

  void setRangeSelected(boolean TF) {
    global.rangeSelected = TF;
  }

  boolean isWindowCentered() {
    return modelManager.isWindowCentered();
  }

  void setWindowCentered(boolean TF) {
    modelManager.setWindowCentered(TF);
  }

  boolean isCameraAdjustable() {
    return global.adjustCameraFlag;
  }

  void setAdjustCamera(boolean TF) {
    global.adjustCameraFlag = TF;
  }

  boolean allowCameraMove() {
    return global.allowCameraMoveFlag;
  }

  void setAllowCameraMove(boolean TF) {
    global.allowCameraMoveFlag = TF;
  }

  void setSolventProbeRadius(float radius) {
    global.solventProbeRadius = radius;
  }

  float getSolventProbeRadius() {
    return global.solventProbeRadius;
  }

  float getCurrentSolventProbeRadius() {
    return global.solventOn ? global.solventProbeRadius : 0;
  }

  void setSolventOn(boolean isOn) {
    global.solventOn = isOn;
  }

  boolean getSolventOn() {
    return global.solventOn;
  }

  void setAllowStatusReporting(boolean TF) {
    statusManager.setAllowStatusReporting(TF);
  }

  void setTestFlag1(boolean value) {
    global.testFlag1 = value;
  }

  boolean getTestFlag1() {
    return global.testFlag1;
  }

  void setTestFlag2(boolean value) {
    global.testFlag2 = value;
  }

  boolean getTestFlag2() {
    return global.testFlag2;
  }

  void setTestFlag3(boolean value) {
    global.testFlag3 = value;
  }

  boolean getTestFlag3() {
    return global.testFlag3;
  }

  void setTestFlag4(boolean value) {
    global.testFlag4 = value;
  }

  boolean getTestFlag4() {
    return global.testFlag4;
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    transformManager.setPerspectiveDepth(perspectiveDepth);
    refresh(0, "Viewer:setPerspectiveDepth()");
  }

  public void setAxesOrientationRasmol(boolean axesOrientationRasmol) {
    transformManager.setAxesOrientationRasmol(axesOrientationRasmol);
    refresh(0, "Viewer:setAxesOrientationRasmol()");
  }

  public boolean getAxesOrientationRasmol() {
    return transformManager.axesOrientationRasmol;
  }

  void setAxesModeMolecular(boolean TF) {
    global.axesMode = (TF ? JmolConstants.AXES_MODE_MOLECULAR
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
  }

  void setAxesModeUnitCell(boolean TF) {
    global.axesMode = (TF ? JmolConstants.AXES_MODE_UNITCELL
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
  }

  int getAxesMode() {
    return global.axesMode;
  }

  void setDisplayCellParameters(boolean displayCellParameters) {
    global.displayCellParameters = displayCellParameters;
  }

  boolean getDisplayCellParameters() {
    return global.displayCellParameters;
  }

  public boolean getPerspectiveDepth() {
    return transformManager.getPerspectiveDepth();
  }

  void setCameraDepth(float depth) {
    transformManager.setCameraDepth(depth);
  }

  float getCameraDepth() {
    return transformManager.getCameraDepth();
  }

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    modelManager.setSelectionHaloEnabled(selectionHaloEnabled);
  }

  boolean getSelectionHaloEnabled() {
    return modelManager.getSelectionHaloEnabled();
  }

  void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    global.bondSelectionModeOr = bondSelectionModeOr;
    refresh(0, "Viewer:setBondSelectionModeOr()");
  }

  boolean getBondSelectionModeOr() {
    return global.bondSelectionModeOr;
  }

  boolean getChainCaseSensitive() {
    return global.chainCaseSensitive;
  }

  void setChainCaseSensitive(boolean chainCaseSensitive) {
    global.chainCaseSensitive = chainCaseSensitive;
  }

  boolean getRibbonBorder() {
    return global.ribbonBorder;
  }

  void setRibbonBorder(boolean borderOn) {
    global.ribbonBorder = borderOn;
  }

  boolean getCartoonRocketFlag() {
    return global.cartoonRocketFlag;
  }

  void setCartoonRocketFlag(boolean TF) {
    global.cartoonRocketFlag = TF;
  }

  boolean getHideNameInPopup() {
    return global.hideNameInPopup;
  }

  void setHideNameInPopup(boolean hideNameInPopup) {
    global.hideNameInPopup = hideNameInPopup;
  }

  void setSsbondsBackbone(boolean ssbondsBackbone) {
    styleManager.setSsbondsBackbone(ssbondsBackbone);
  }

  public void setAutoBond(boolean ab) {
    modelManager.setAutoBond(ab);
    refresh(0, "Viewer:setAutoBond()");
  }

  public boolean getAutoBond() {
    return modelManager.autoBond;
  }

  // //////////////////////////////////////////////////////////////
  // Graphics3D
  // //////////////////////////////////////////////////////////////

  void setGreyscaleRendering(boolean greyscaleRendering) {
    global.greyscaleRendering = greyscaleRendering;
    g3d.setGreyscaleMode(greyscaleRendering);
    refresh(0, "Viewer:setGreyscaleRendering()");
  }

  boolean getGreyscaleRendering() {
    return global.greyscaleRendering;
  }

  void setLabelsFrontFlag(boolean labelsFrontFlag) {
    global.labelsFrontFlag = labelsFrontFlag;
  }

  boolean getLabelsFrontFlag() {
    return global.labelsFrontFlag;
  }

  void setLabelsGroupFlag(boolean labelsGroupFlag) {
    global.labelsGroupFlag = labelsGroupFlag;
  }

  boolean getLabelsGroupFlag() {
    return global.labelsGroupFlag;
  }

  void setDisablePopupMenu(boolean disablePopupMenu) {
    global.disablePopupMenu = disablePopupMenu;
  }

  boolean getDisablePopupMenu() {
    return global.disablePopupMenu;
  }

  // ///////////////////////////////////////////////////////////////
  // Frame
  // ///////////////////////////////////////////////////////////////
  /*
   * private BondIterator bondIteratorSelected(byte bondType) { return
   * getFrame().getBondIterator(bondType, selectionManager.bsSelection); }
   */
  final AtomIterator nullAtomIterator = new NullAtomIterator();

  static class NullAtomIterator implements AtomIterator {
    public boolean hasNext() {
      return false;
    }

    public Atom next() {
      return null;
    }

    public void release() {
    }
  }

  final BondIterator nullBondIterator = new NullBondIterator();

  static class NullBondIterator implements BondIterator {
    public boolean hasNext() {
      return false;
    }

    public int nextIndex() {
      return -1;
    }

    public Bond next() {
      return null;
    }
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to StyleManager
  // ///////////////////////////////////////////////////////////////

  /*
   * for rasmol compatibility with continued menu operation: - if it is from the
   * menu & nothing selected * set the setting * apply to all - if it is from
   * the menu and something is selected * apply to selection - if it is from a
   * script * apply to selection * possibly set the setting for some things
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
    return (short) -styleManager.percentVdwAtom;
  }

  public short getMadBond() {
    return (short) (styleManager.marBond * 2);
  }

  void setModeMultipleBond(byte modeMultipleBond) {
    styleManager.setModeMultipleBond(modeMultipleBond);
    refresh(0, "Viewer:setModeMultipleBond()");
  }

  byte getModeMultipleBond() {
    return styleManager.modeMultipleBond;
  }

  void setShowMultipleBonds(boolean showMultipleBonds) {
    styleManager.setShowMultipleBonds(showMultipleBonds);
    refresh(0, "Viewer:setShowMultipleBonds()");
  }

  boolean getShowMultipleBonds() {
    return styleManager.showMultipleBonds;
  }

  public void setShowHydrogens(boolean showHydrogens) {
    styleManager.setShowHydrogens(showHydrogens);
    refresh(0, "Viewer:setShowHydrogens()");
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
    refresh(0, "setShowMeasurements()");
  }

  public void setMeasureAllModels(boolean TF) {
    global.measureAllModels = TF;
  }

  public boolean getMeasureAllModelsFlag() {
    return global.measureAllModels;
  }

  public boolean getShowMeasurements() {
    return styleManager.showMeasurements;
  }

  void setShowMeasurementLabels(boolean showMeasurementLabels) {
    styleManager.setShowMeasurementLabels(showMeasurementLabels);
    refresh(0, "Viewer:setShowMeasurementLabels()");
  }

  boolean getShowMeasurementLabels() {
    return styleManager.showMeasurementLabels;
  }

  /*
   * short getMeasurementMad() { return styleManager.measurementMad; }
   */

  boolean setMeasureDistanceUnits(String units) {
    if (!styleManager.setMeasureDistanceUnits(units))
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
    modelManager.setZeroBased();
  }

  boolean getZeroBasedXyzRasmol() {
    return styleManager.zeroBasedXyzRasmol;
  }

  void setLabelFontSize(int points) {
    styleManager.setLabelFontSize(points);
    refresh(0, "Viewer:setLabelFontSize()");
  }

  void setLabelOffset(int xOffset, int yOffset) {
    styleManager.setLabelOffset(xOffset, yOffset);
    refresh(0, "Viewer:setLabelOffset()");
  }

  int getLabelOffsetX() {
    return styleManager.labelOffsetX;
  }

  int getLabelOffsetY() {
    return styleManager.labelOffsetY;
  }

  // //////////////////////////////////////////////////////////////
  // temp manager
  // //////////////////////////////////////////////////////////////

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

  // //////////////////////////////////////////////////////////////
  // font stuff
  // //////////////////////////////////////////////////////////////
  Font3D getFont3D(int fontSize) {
    return g3d.getFont3D(JmolConstants.DEFAULT_FONTFACE,
        JmolConstants.DEFAULT_FONTSTYLE, fontSize);
  }

  Font3D getFont3D(String fontFace, String fontStyle, int fontSize) {
    return g3d.getFont3D(fontFace, fontStyle, fontSize);
  }

  // //////////////////////////////////////////////////////////////
  // Access to atom properties for clients
  // //////////////////////////////////////////////////////////////

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

  // //////////////////////////////////////////////////////////////
  // stereo support
  // //////////////////////////////////////////////////////////////

  void setStereoMode(int stereoMode) {
    transformManager.setStereoMode(stereoMode);
  }

  void setStereoMode(int[] twoColors) {
    transformManager.setStereoMode(twoColors);
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

  // //////////////////////////////////////////////////////////////
  //
  // //////////////////////////////////////////////////////////////

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

  // /////////////// getProperty /////////////

  public Object getProperty(String returnType, String infoType, String paramInfo) {
    // return types include "JSON", "string", "readable", and anything else returns the Java object.
    return propertyManager.getProperty(returnType, infoType, paramInfo);
  }

  String getModelExtract(String atomExpression) {
    BitSet bs = selectionManager.getAtomBitSet(atomExpression);
    return fileManager.getFullPathName() + "\nJmol version " + getJmolVersion()
        + "\nEXTRACT: " + atomExpression + "\n"
        + modelManager.getModelExtract(bs);
  }

  String simpleReplace(String str, String strFrom, String strTo) {
    if (str == null)
      return str;
    int fromLength = strFrom.length();
    if (fromLength == 0)
      return str;
    boolean isOnce = (strTo.indexOf(strFrom) >= 0);
    int ipt;
    String stemp = "";
    while (str.indexOf(strFrom) >= 0) {
      int ipt0 = 0;
      while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
        stemp += str.substring(ipt0, ipt) + strTo;
        ipt0 = ipt + fromLength;
      }
      str = stemp + str.substring(ipt0, str.length());
      if (isOnce)
        break;
    }
    return str;
  }

  String getHexColorFromIndex(short colix) {
    return g3d.getHexColorFromIndex(colix);
  }

  void setModelVisibility() {
    modelManager.setModelVisibility();
  }

  boolean isTainted = true;

  void setTainted(boolean TF) {
    isTainted = TF;
    axesAreTainted = TF;
  }

  public void setSyncDriver(int syncMode) {
    Logger.debug(htmlName + " viewer setting sync driver " + syncMode);
    statusManager.setSyncDriver(syncMode);
  }

  public int getSyncMode() {
    return statusManager.getSyncMode();
  }

  void checkObjectClicked(int x, int y, int modifiers) {
    modelManager.checkObjectClicked(x, y, modifiers);
  }

  void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    modelManager.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers);
  }

  int cardinalityOf(BitSet bs) {
    int nbitset = 0;
    for (int i = bs.size(); --i >= 0;)
      if (bs.get(i))
        nbitset++;
    return nbitset;
  }

  /* *******************************************************
   * 
   * methods for spinning and rotating
   * 
   * ********************************************************/

  void rotateAxisAngleAtCenter(Point3f rotCenter, Vector3f rotAxis,
                               float degrees, float endDegrees, boolean isSpin) {
    // Eval: rotate FIXED
    if (rotCenter != null)
      moveRotationCenter(rotCenter);

    transformManager.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees,
        endDegrees, isSpin);
  }

  void rotateAboutPointsInternal(Point3f point1, Point3f point2,
                                 float nDegrees, float endDegrees,
                                 boolean isSpin) {
    // Eval: rotate INTERNAL
    transformManager.rotateAboutPointsInternal(point1, point2, nDegrees,
        endDegrees, false, isSpin);
  }

  void setPickingSpinRate(int rate) {
    if (rate < 1)
      rate = 1;
    global.pickingSpinRate = rate;
  }

  void startSpinningAxis(int atomIndex1, int atomIndex2, boolean isClockwise) {
    // picking manager set picking SPIN
    Point3f pt1 = modelManager.getAtomPoint3f(atomIndex1);
    Point3f pt2 = modelManager.getAtomPoint3f(atomIndex2);
    startSpinningAxis(pt1, pt2, isClockwise);
  }

  void startSpinningAxis(Point3f pt1, Point3f pt2, boolean isClockwise) {
    //from draw object click
    if (getSpinOn()) {
      setSpinOn(false);
      return;
    }
    transformManager.rotateAboutPointsInternal(pt1, pt2,
        global.pickingSpinRate, Float.MAX_VALUE, isClockwise, true);
  }

  Point3f getDrawObjectCenter(String axisID) {
    return modelManager.getSpinCenter(axisID, repaintManager.displayModelIndex);
  }

  Vector3f getDrawObjectAxis(String axisID) {
    return modelManager.getSpinAxis(axisID, repaintManager.displayModelIndex);
  }

  Vector3f getModelDipole() {
    return modelManager.getModelDipole();
  }

  void getBondDipoles() {
    modelManager.getBondDipoles();
    return;
  }

  public void setDipoleScale(float scale) {
    loadShape(JmolConstants.SHAPE_DIPOLES);
    setShapeProperty(JmolConstants.SHAPE_DIPOLES, "dipoleVectorScale",
        new Float(scale));
  }

  void getAtomIdentityInfo(int atomIndex, Hashtable info) {
    modelManager.getAtomIdentityInfo(atomIndex, info);
  }

  void setDefaultLattice(Point3f ptLattice) {
    global.setDefaultLattice(ptLattice);
  }

  Point3f getDefaultLattice() {
    return global.ptDefaultLattice;
  }

  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    modelManager.setAtomCoord(atomIndex, x, y, z);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    modelManager.setAtomCoordRelative(atomIndex, x, y, z);
  }

  public void setAtomCoordRelative(Point3f offset) {
    modelManager.setAtomCoordRelative(offset, selectionManager.bsSelection);    
  }
  
  float functionXY(String functionName, int x, int y) {
    return statusManager.functionXY(functionName, x, y);
  }

  Point3f[] getAdditionalHydrogens(BitSet atomSet) {
    return modelManager.getAdditionalHydrogens(atomSet);
  }
  
  void setHelpPath(String url) {
    global.helpPath = url;
  }
  
  void getHelp(String what) {
    if (global.helpPath == null)
      global.helpPath = styleManager.getDefaultHelpPath();
    showUrl(global.helpPath + what);
  }
  
  private class GlobalSettings {

    /*
     *  Mostly these are just saved and restored directly from Viewer.
     *  They are collected here for reference and to ensure that no 
     *  methods are written that bypass viewer's get/set methods.
     *  
     *  Because these are not Frame variables, they should persist past
     *  a new file loading. There is some question in my mind whether all
     *  should be in this category.
     *  
     */

    GlobalSettings() {
      //
    }

    //file loading
    
    char inlineNewlineChar = '|';
    String defaultLoadScript = "";
    Point3f ptDefaultLattice = new Point3f();
    
    void setDefaultLattice(Point3f ptLattice) {
      ptDefaultLattice.set(ptLattice);
    }
    int[] getDefaultLattice() {
      int[] A = new int[4];
      A[1] = (int) ptDefaultLattice.x;
      A[2] = (int) ptDefaultLattice.y;
      A[3] = (int) ptDefaultLattice.z;
      return A;
    }
    
    //centering and perspective
    
    boolean allowCameraMoveFlag = true;
    boolean adjustCameraFlag = true;

    //solvent
   
    boolean solventOn;
    float solventProbeRadius = 1.2f;
    
    //measurements
    
    boolean measureAllModels;

    //rendering
    
    boolean enableFullSceneAntialiasing;
    boolean greyscaleRendering;
    boolean labelsGroupFlag;
    boolean labelsFrontFlag;
    boolean dotsSelectedOnlyFlag;
    boolean dotSurfaceFlag = true;
    boolean displayCellParameters = true;
    int axesMode = JmolConstants.AXES_MODE_BOUNDBOX;

    //atoms and bonds
    
    boolean bondSelectionModeOr;

    //secondary structure + Rasmol
    
    boolean rasmolHydrogenSetting = true;
    boolean rasmolHeteroSetting = true;
    boolean cartoonRocketFlag;
    boolean ribbonBorder;
    boolean chainCaseSensitive;
    boolean rangeSelected;

    //misc
    
    int pickingSpinRate = 10;
    boolean hideNameInPopup;
    boolean disablePopupMenu;
    String helpPath;
    
    //testing
    
    boolean debugScript;
    boolean testFlag1;
    boolean testFlag2;
    boolean testFlag3;
    boolean testFlag4;
  }

}
