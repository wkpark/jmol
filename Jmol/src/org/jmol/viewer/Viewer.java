/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Event;
import java.util.Hashtable;
import java.util.BitSet;
import java.util.Properties;
import java.util.Vector;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.net.URL;
import java.io.Reader;

/*******************************************************************************
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
 ******************************************************************************/

final public class Viewer extends JmolViewer {

  Component awtComponent;

  ColorManager colorManager;

  PropertyManager propertyManager;

  StatusManager statusManager;

  TransformManager transformManager;

  SelectionManager selectionManager;

  MouseManager mouseManager;

  FileManager fileManager;

  public ModelManager modelManager;

  RepaintManager repaintManager;

  StyleManager styleManager;

  TempManager tempManager;

  PickingManager pickingManager;

  public Eval eval;

  Graphics3D g3d;

  JmolAdapter modelAdapter;

  String strJavaVendor;

  String strJavaVersion;

  String strOSName;

  Hashtable appletInfo;

  String htmlName="";
  
  boolean jvm11orGreater = false;

  boolean jvm12orGreater = false;

  boolean jvm14orGreater = false;

  Viewer(Component awtComponent, JmolAdapter modelAdapter) {

    this.awtComponent = awtComponent;
    this.modelAdapter = modelAdapter;

    strJavaVendor = System.getProperty("java.vendor");
    strOSName = System.getProperty("os.name");
    strJavaVersion = System.getProperty("java.version");
    jvm11orGreater = (strJavaVersion.compareTo("1.1") >= 0 &&
    // Netscape on MacOS does not implement 1.1 event model
    !(strJavaVendor.startsWith("Netscape")
        && strJavaVersion.compareTo("1.1.5") <= 0 && "Mac OS".equals(strOSName)));
    jvm12orGreater = (strJavaVersion.compareTo("1.2") >= 0);
    jvm14orGreater = (strJavaVersion.compareTo("1.4") >= 0);

    System.out.println(JmolConstants.copyright + "\nJmol Version "
        + JmolConstants.version + "  " + JmolConstants.date + "\njava.vendor:"
        + strJavaVendor + "\njava.version:" + strJavaVersion + "\nos.name:"
        + strOSName);
    System.out.println(htmlName + " jvm11orGreater=" + jvm11orGreater 
        + "\njvm12orGreater=" + jvm12orGreater 
        + "\njvm14orGreater=" + jvm14orGreater);

    g3d = new Graphics3D(awtComponent);
    statusManager = new StatusManager(this);
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
    refresh(1, "Viewer:homePosition()");
  }

  final Hashtable imageCache = new Hashtable();

  void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }

  void logError(String strMsg) {
    System.out.println(strMsg);
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
  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  void rotateXYBy(int xDelta, int yDelta) {
    transformManager.rotateXYBy(xDelta, yDelta);
    refresh(1, "Viewer:rotateXYBy()");
  }

  void rotateZBy(int zDelta) {
    transformManager.rotateZBy(zDelta);
    refresh(1, "Viewer:rotateZBy()");
  }

  public void rotateFront() {
    transformManager.rotateFront();
    refresh(1, "Viewer:rotateFront()");
  }

  public void rotateToX(float angleRadians) {
    transformManager.rotateToX(angleRadians);
    refresh(1, "Viewer:rotateToX()");
  }

  public void rotateToY(float angleRadians) {
    transformManager.rotateToY(angleRadians);
    refresh(1, "Viewer:rotateToY()");
  }

  public void rotateToZ(float angleRadians) {
    transformManager.rotateToZ(angleRadians);
    refresh(1, "Viewer:rotateToZ()");
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
    refresh(1, "Viewer:rotateXRadians()");
  }

  void rotateYRadians(float angleRadians) {
    transformManager.rotateYRadians(angleRadians);
    refresh(1, "Viewer:rotateYRadians()");
  }

  void rotateZRadians(float angleRadians) {
    transformManager.rotateZRadians(angleRadians);
    refresh(1, "Viewer:rotateZRadians()");
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
    refresh(1, "Viewer:rotateZDegreesScript()");
  }

  final static float radiansPerDegree = (float) (2 * Math.PI / 360);

  final static float degreesPerRadian = (float) (360 / (2 * Math.PI));

  void rotate(AxisAngle4f axisAngle) {
    transformManager.rotate(axisAngle);
    refresh(1, "Viewer:rotate()");
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
    refresh(1, "Viewer:zoomBy()");
  }

  public int getZoomPercent() {
    return transformManager.zoomPercent;
  }

  int getZoomPercentSetting() {
    return transformManager.zoomPercentSetting;
  }

  public static int MAXIMUM_ZOOM_PERCENTAGE = 5000;

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

  void calcTransformMatrices() {
    transformManager.calcTransformMatrices();
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
    refresh(0, "Viewer:setPerspectiveDepth()");
  }

  public void setAxesOrientationRasmol(boolean axesOrientationRasmol) {
    transformManager.setAxesOrientationRasmol(axesOrientationRasmol);
    refresh(0, "Viewer:setAxesOrientationRasmol()");
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
      modelManager.increaseRotationRadius(transformManager
          .getRotationRadiusIncrease());
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
    g3d.setSize(dimScreen, enableFullSceneAntialiasing);
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

  public void setModeAtomColorProfile(String palette) {
    colorManager.setPaletteDefault(palette);
    refresh(0, "Viewer:setModeAtomColorProfile()");
  }

  String getModeAtomColorProfile() {
    return colorManager.paletteDefault;
  }

  void setDefaultColors(String colorScheme) {
    colorManager.setDefaultColors(colorScheme);
  }

  public void setColorSelection(Color c) {
    colorManager.setColorSelection(c);
    refresh(0, "Viewer:setColorSelection()");
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
    refresh(0, "Viewer:setCOlorLabel()");
  }

  void setElementColor(int elementNumber, Color color) {
    colorManager.setElementColor(elementNumber, color);
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
    refresh(0, "Viewer:setColorMeasurement()");
  }

  public Color getColorMeasurement() {
    return colorManager.colorDistance;
  }

  void setColorDistance(Color c) {
    colorManager.setColorDistance(c);
    refresh(0, "Viewer:setColorDistance()");
  }

  Color getColorDistance() {
    return colorManager.colorDistance;
  }

  short getColixDistance() {
    return colorManager.colixDistance;
  }

  void setColorAngle(Color c) {
    colorManager.setColorAngle(c);
    refresh(0, "Viewer:setColorAngle()");
  }

  Color getColorAngle() {
    return colorManager.colorAngle;
  }

  short getColixAngle() {
    return colorManager.colixAngle;
  }

  void setColorTorsion(Color c) {
    colorManager.setColorTorsion(c);
    refresh(0, "Viewer:setCOlorTorsion()");
  }

  Color getColorTorsion() {
    return colorManager.colorTorsion;
  }

  short getColixTorsion() {
    return colorManager.colixTorsion;
  }

  public void setColorVector(Color c) {
    colorManager.setColorVector(c);
    refresh(0, "Viewer:setColorVector()");
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
    refresh(0, "Viewer:setCOlorBackground()");
  }

  public Color getColorBackground() {
    return colorManager.colorBackground;
  }

  public void setColorBackground(String colorName) {
    colorManager.setColorBackground(colorName);
    refresh(0, "Viewer:setColorBackground()");
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

  short getColixAtom(Atom atom) {
    return colorManager.getColixAtom(atom);
  }

  short getColixAtomPalette(Atom atom, String palette) {
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

  short getColixFromPalette(float val, float rangeMin, float rangeMax,
      String palette) {
    return colorManager.getColixFromPalette(val, rangeMin, rangeMax, palette);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

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

  /*
   * boolean isSelected(Atom atom) { return
   * selectionManager.isSelected(atom.atomIndex); }
   */

  boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  boolean hasSelectionHalo(int atomIndex) {
    return selectionHaloEnabled && !repaintManager.wireframeRotating
        && selectionManager.isSelected(atomIndex);
  }

  boolean selectionHaloEnabled = false;

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    if (this.selectionHaloEnabled != selectionHaloEnabled) {
      this.selectionHaloEnabled = selectionHaloEnabled;
      refresh(0, "Viewer:setSelectionHaloEnabled()");
    }
  }

  boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  private boolean bondSelectionModeOr;

  void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    this.bondSelectionModeOr = bondSelectionModeOr;
    refresh(0, "Viewer:setBondSelectionModeOr()");
  }

  boolean getBondSelectionModeOr() {
    return bondSelectionModeOr;
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

  Object getInputStreamOrErrorMessageFromName(String name) {
    return fileManager.getInputStreamOrErrorMessageFromName(name);
  }

  Object getUnzippedBufferedReaderOrErrorMessageFromName(String name) {
    return fileManager.getUnzippedBufferedReaderOrErrorMessageFromName(name);
  }

  public void openFile(String name) {
    /*
     * System.out.println("openFile(" + name + ") thread:" +
     * Thread.currentThread() + " priority:" +
     * Thread.currentThread().getPriority());
     */
    clear();
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFile(name);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openFile(" + name + ") " + ms + " ms");
  }

  public void openFiles(String modelName, String[] names) {
    clear();
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFiles(modelName, names);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openFiles() " + ms + " ms");
  }

  public void openStringInline(String strModel) {
    clear();
    fileManager.openStringInline(strModel);
    setStatusFileLoaded("string", "", modelManager.getModelSetName(), null,
        getOpenFileError());
  }

  public String getInlineData() {
    return fileManager.inlineData;
  }

  char inlineNewlineChar = '|';

  public void loadInline(String strModel) {
    if (strModel == null)
      return;
    if (inlineNewlineChar != 0) {
      int len = strModel.length();
      int i;
      for (i = 0; i < len && strModel.charAt(0) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == inlineNewlineChar)
        strModel = strModel.substring(i + 1);
      strModel = strModel.replace(inlineNewlineChar, '\n');
    }
    openStringInline(strModel);
  }

  public void openDOM(Object DOMNode) {
    clear();
    long timeBegin = System.currentTimeMillis();
    fileManager.openDOM(DOMNode);
    long ms = System.currentTimeMillis() - timeBegin;
    System.out.println("openDOM " + ms + " ms");
    setStatusFileLoaded("JSNode", "", modelManager.getModelSetName(), null,
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
  }

  public String getOpenFileError() {
    String errorMsg = getOpenFileError1();
    // System.gc();
    // System.runFinalization();
    return errorMsg;
  }

  String getOpenFileError1() {
    String fullPathName = getFullPathName();
    String fileName = fileManager.getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      setStatusFileNotLoaded(fullPathName, errorMsg);
      return errorMsg;
    }
    openClientFile(fullPathName, fileName, clientFile);
    setStatusFileLoaded(fullPathName, fileName, modelManager.getModelSetName(),
        clientFile, null);
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

  // ///////////////////////////////////////////////////////////////
  // delegated to ModelManager
  // ///////////////////////////////////////////////////////////////

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
    repaintManager.initializePointers(1);
    popHoldRepaint();
  }

  void clear() {
    repaintManager.clearAnimation();
    transformManager.clearVibration();
    modelManager.setClientFile(null, null, null);
    selectionManager.clearSelection();
    clearMeasurements();
    setStatusFileLoaded(null, null, null, null, null);
    refresh(0, "Viewer:clear()");
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

  public String getModelSetTypeName() {
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

  String getModelInfo() {
    return modelManager.getModelInfo();
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
    refresh(0, "Viewer:setCenter()");
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
    if (!friedaSwitch)
      scaleFitToScreen();
    refresh(0, "Viewer:setCenterBitSet()");
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

  public void rebond() {
    modelManager.rebond();
    refresh(0, "Viewer:rebond()");
  }

  public void setBondTolerance(float bondTolerance) {
    modelManager.setBondTolerance(bondTolerance);
    refresh(0, "Viewer:setBOndTolerance()");
  }

  public float getBondTolerance() {
    return modelManager.bondTolerance;
  }

  public void setMinBondDistance(float minBondDistance) {
    modelManager.setMinBondDistance(minBondDistance);
    refresh(0, "Viewer:setMinBondDistance()");
  }

  public float getMinBondDistance() {
    return modelManager.minBondDistance;
  }

  public void setAutoBond(boolean ab) {
    modelManager.setAutoBond(ab);
    refresh(0, "Viewer:setAutoBond()");
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

  String getFileHeader() {
    return modelManager.getFileHeader();
  }

  String getPDBHeader() {
    return modelManager.getPDBHeader();
  }

  Hashtable getModelInfoObject() {
    return modelManager.getModelInfoObject();
  }

  Vector getAllAtomInfo(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getAllAtomInfo(bs);
  }

  Vector getBondDetail(String atomExpression) {
    BitSet bs = getAtomBitSet(atomExpression);
    return modelManager.getBondInfoFromBitSet(bs);
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
    System.out.println("getMeasurementStringValue" + i + " " + str);
    return str;
  }

  public int[] getMeasurementCountPlusIndices(int i) {
    int[] List = (int[]) getShapeProperty(JmolConstants.SHAPE_MEASURES,
        "countPlusIndices", i);
    System.out.println(List);
    return List;
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
      System.out.println("animation is ON in resumeAnimation");
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
    // System.out.println("viewer.setInMotion("+inMotion+")");
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

  /*
   * void forceRefresh() { System.out.println("viewer.forceRefresh");
   * repaintManager.forceRefresh(); }
   */

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
      g3d.beginRendering(rectClip, transformManager
          .getStereoRotationMatrix(true), false);
      repaintManager.render(g3d, rectClip, modelManager.getFrame(),
          repaintManager.displayModelIndex);
      g3d.endRendering();
      g3d.snapshotAnaglyphChannelBytes();
      g3d.beginRendering(rectClip, transformManager
          .getStereoRotationMatrix(false), false);
      repaintManager.render(g3d, rectClip, modelManager.getFrame(),
          repaintManager.displayModelIndex);
      g3d.endRendering();
      if (stereoMode == JmolConstants.STEREO_REDCYAN)
        g3d.applyCyanAnaglyph();
      else
        g3d
            .applyBlueOrGreenAnaglyph(stereoMode == JmolConstants.STEREO_REDBLUE);
      Image img = g3d.getScreenImage();
      try {
        g.drawImage(img, 0, 0, null);
      } catch (NullPointerException npe) {
        System.out.println("Sun!! ... fix graphics your bugs!");
      }
      g3d.releaseScreenImage();
      break;
    }
    repaintView();
  }

  void render1(Graphics g, Matrix3f matrixRotate, boolean antialias, int x,
      int y) {
    g3d.beginRendering(rectClip, matrixRotate, antialias);
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
    g3d.beginRendering(rectClip, transformManager
        .getStereoRotationMatrix(false), antialiasThisFrame);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
        repaintManager.displayModelIndex);
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
  }

  void checkOversample() {
    boolean tOversample = (tOversampleAlways | (!repaintManager.inMotion & tOversampleStopped));
    repaintManager.setOversample(tOversample);
    transformManager.setOversample(tOversample);
  }

  void setOversample(boolean tOversample) {
    transformManager.setOversample(tOversample);
    repaintManager.setOversample(tOversample);
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  Eval getEval() {
    if (eval == null)
      eval = new Eval(this);
    return eval;
  }

  public String evalFile(String strFilename) {
    if (strFilename != null) {
      if (!getEval().loadScriptFile(strFilename, false))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalString(String strScript) {
    if (strScript != null) {
      if (!getEval().loadScriptString(strScript, false))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalStringQuiet(String strScript) {
    if (strScript != null) {
      if (!getEval().loadScriptString(strScript, true))
        return eval.getErrorMessage();
      eval.start();
    }
    return null;
  }

  public String evalStringWait(String strScript) {
    if (strScript == null)
      return null;
    getProperty("String", "jmolStatus", "all");
    if (!getEval().loadScriptString(strScript, false))
      return eval.getErrorMessage();
    eval.run();
    String str = (String) getProperty("JSON", "jmolStatus", "all");
    getProperty("String", "jmolStatus", "none");
    return str;
  }

  int iscript = 0;

  public String script(String script) {
    iscript++;
    String strError = evalString(script);
    setStatusScriptStarted(iscript, script, strError);
    return strError;
  }

  public String scriptWait(String script) {
    iscript++;
    String strInfo = evalStringWait(script);
    return strInfo;
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
    modelManager.setShapeSize(shapeID, size, selectionManager.bsSelection);
    refresh(0, "Viewer:setShapeSize()");
  }

  int getShapeSize(int shapeID) {
    return modelManager.getShapeSize(shapeID);
  }

  void setShapeProperty(int shapeID, String propertyName, Object value) {

    /*
     * System.out.println("JmolViewer.setShapeProperty("+
     * JmolConstants.shapeClassBases[shapeID]+ "," + propertyName + "," + value +
     * ")");
     */
    modelManager.setShapeProperty(shapeID, propertyName, value,
        selectionManager.bsSelection);
    refresh(0, "Viewer:setShapeProperty()");
  }

  void setShapeColorProperty(int shapeType, Color color) {
    setShapeProperty(shapeType, "color", color);
  }

  Object getShapeProperty(int shapeType, String propertyName) {
    return modelManager.getShapeProperty(shapeType, propertyName,
        Integer.MIN_VALUE);
  }

  Object getShapeProperty(int shapeType, String propertyName, int index) {
    return modelManager.getShapeProperty(shapeType, propertyName, index);
  }

  Color getShapePropertyAsColor(int shapeID, String propertyName) {
    return (Color) getShapeProperty(shapeID, propertyName);
  }

  int getShapePropertyAsInt(int shapeID, String propertyName) {
    Object value = getShapeProperty(shapeID, propertyName);
    return value == null || !(value instanceof Integer) ? Integer.MIN_VALUE
        : ((Integer) value).intValue();
  }

  Color getColorShape(int shapeID) {
    return (Color) getShapeProperty(shapeID, "color");
  }

  short getColixShape(int shapeID) {
    return Graphics3D.getColix(getColorShape(shapeID));
  }

  int getShapeID(String shapeName) {
    for (int i = JmolConstants.SHAPE_MAX; --i >= 0;)
      if (JmolConstants.shapeClassBases[i].equals(shapeName))
        return i;
    String msg = "Unrecognized shape name:" + shapeName;
    System.out.println(msg);
    throw new NullPointerException(msg);
  }

  short getColix(Color color) {
    return Graphics3D.getColix(color);
  }

  short getColix(Object object) {
    return Graphics3D.getColix(object);
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

  boolean debugScript = false;

  boolean getDebugScript() {
    return debugScript;
  }

  public void setDebugScript(boolean debugScript) {
    this.debugScript = debugScript;
  }

  void atomPicked(int atomIndex, boolean shiftKey) {
    pickingManager.atomPicked(atomIndex, shiftKey);
  }

  void clearClickCount() {
    mouseManager.clearClickCount();
  }

  void setPickingMode(int pickingMode) {
    pickingManager.setPickingMode(pickingMode);
  }

  String getAtomInfo(int atomIndex) {
    return modelManager.getAtomInfo(atomIndex);
  }

  // //////////////status manager dispatch//////////////

  public Hashtable getMessageQueue() {
    return statusManager.messageQueue;
  }

  Viewer getViewer() {
    return this;
  }

  void setStatusAtomPicked(int atomIndex, String info) {
    statusManager.setStatusAtomPicked(atomIndex, info);
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
    if (disablePopupMenu)
      return;
    statusManager.popupMenu(x, y);
  }

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    statusManager.setJmolStatusListener(jmolStatusListener);
  }

  void setStatusFrameChanged(int frameNo) {
    statusManager.setStatusFrameChanged(frameNo);
  }

  void setStatusFileLoaded(String fullPathName, String fileName,
      String modelName, Object clientFile, String strError) {
    statusManager.setStatusFileLoaded(fullPathName, fileName, modelName,
        clientFile, strError);
  }

  void setStatusFileNotLoaded(String fullPathName, String errorMsg) {
    statusManager.setStatusFileLoaded(fullPathName, null, null, null, errorMsg);
  }

  private void manageScriptTermination() {
    if (eval != null && eval.hasTerminationNotification()) {
      String strErrorMessage = eval.getErrorMessage();
      int msWalltime = eval.getExecutionWalltime();
      eval.resetTerminationNotification();
      statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
    }
  }

  void scriptEcho(String strEcho) {
    statusManager.setScriptEcho(strEcho);
  }

  void scriptStatus(String strStatus) {
    statusManager.setScriptStatus(strStatus);
  }

  public void showUrl(String urlString) {
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
    if (key.equalsIgnoreCase("wireframeRotation"))
      return getWireframeRotation();
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
    System.out.println("viewer.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  public void setBooleanProperty(String key, boolean value) {
    refresh(0, "viewer.setBooleanProperty");
    if (key.equalsIgnoreCase("wireframeRotation")) {
      setWireframeRotation(value);
      return;
    }
    if (key.equalsIgnoreCase("perspectiveDepth")) {
      setPerspectiveDepth(value);
      return;
    }
    if (key.equalsIgnoreCase("showAxes")) {
      setShapeShow(JmolConstants.SHAPE_AXES, value);
      return;
    }
    if (key.equalsIgnoreCase("showBoundBox")) {
      setShapeShow(JmolConstants.SHAPE_BBCAGE, value);
      return;
    }
    if (key.equalsIgnoreCase("showUnitcell")) {
      setShapeShow(JmolConstants.SHAPE_UCCAGE, value);
      return;
    }
    if (key.equalsIgnoreCase("showHydrogens")) {
      setShowHydrogens(value);
      return;
    }
    if (key.equalsIgnoreCase("showHydrogens")) {
      setShowHydrogens(value);
      return;
    }
    if (key.equalsIgnoreCase("showMeasurements")) {
      setShowMeasurements(value);
      return;
    }
    if (key.equalsIgnoreCase("showSelections")) {
      setSelectionHaloEnabled(value);
      return;
    }
    if (key.equalsIgnoreCase("oversampleAlways")) {
      setOversampleAlwaysEnabled(value);
      return;
    }
    if (key.equalsIgnoreCase("oversampleStopped")) {
      setOversampleStoppedEnabled(value);
      return;
    }
    if (key.equalsIgnoreCase("axesOrientationRasmol")) {
      setAxesOrientationRasmol(value);
      return;
    }
    if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
      setZeroBasedXyzRasmol(value);
      return;
    }
    if (key.equalsIgnoreCase("frieda")) {
      setFriedaSwitch(value);
      return;
    }
    if (key.equalsIgnoreCase("testFlag1")) {
      setTestFlag1(value);
      return;
    }
    if (key.equalsIgnoreCase("testFlag2")) {
      setTestFlag2(value);
      return;
    }
    if (key.equalsIgnoreCase("testFlag3")) {
      setTestFlag3(value);
      return;
    }
    if (key.equalsIgnoreCase("testFlag4")) {
      setTestFlag4(value);
      return;
    }
    if (key.equalsIgnoreCase("chainCaseSensitive")) {
      setChainCaseSensitive(value);
      return;
    }
    if (key.equalsIgnoreCase("ribbonBorder")) {
      setRibbonBorder(value);
      return;
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
      return;
    }
    if (key.equalsIgnoreCase("disablePopupMenu")) {
      setDisablePopupMenu(value);
      return;
    }
    System.out.println("viewer.setBooleanProperty(" + key + "," + value
        + ") - unrecognized");
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

  /*****************************************************************************
   * Graphics3D
   ****************************************************************************/

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
    refresh(0, "Viewer:setOversampleAlwaysEnabled()");
  }

  void setOversampleStoppedEnabled(boolean value) {
    tOversampleStopped = value;
    checkOversample();
    refresh(0, "Viewer:setOversampleStoppedEnabled()");
  }

  boolean greyscaleRendering;

  void setGreyscaleRendering(boolean greyscaleRendering) {
    this.greyscaleRendering = greyscaleRendering;
    g3d.setGreyscaleMode(greyscaleRendering);
    refresh(0, "Viewer:setGreyscaleRendering()");
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

  public Color getAtomColor(int i) {
    return g3d.getColor(modelManager.getAtomColix(i));
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

  public Color getBondColor1(int i) {
    return g3d.getColor(modelManager.getBondColix1(i));
  }

  public int getBondModelIndex(int i) {
    return modelManager.getBondModelIndex(i);
  }

  public Color getBondColor2(int i) {
    return g3d.getColor(modelManager.getBondColix2(i));
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
    return propertyManager.getProperty(returnType, infoType, paramInfo);
  }

  String getModelExtract(String atomExpression) {
    BitSet bs = selectionManager.getAtomBitSet(atomExpression);
    return fileManager.getFullPathName() + "\nEXTRACT: " + bs + "\nJmol\n"
        + modelManager.getModelExtractFromBitSet(bs);
  }

  String simpleReplace(String str, String strFrom, String strTo) {
    int fromLength = strFrom.length();
    if (str == null || fromLength == 0)
      return str;
    int ipt;
    int ipt0 = 0;
    String sout = "";
    while ((ipt = str.indexOf(strFrom, ipt0)) >= 0) {
      sout += str.substring(ipt0, ipt) + strTo;
      ipt0 = ipt + fromLength;
    }
    sout += str.substring(ipt0, str.length());
    return sout;
  }

  String getHexColorFromIndex(short colix) {
    return g3d.getHexColorFromIndex(colix);
  }

}
