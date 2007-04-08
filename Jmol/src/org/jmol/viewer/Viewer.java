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

import org.jmol.symmetry.UnitCell;
import org.jmol.i18n.GT;

import org.jmol.api.*;
import org.jmol.g3d.*;
import org.jmol.util.CommandHistory;
import org.jmol.util.Logger;
import org.jmol.util.Base64;
import org.jmol.util.JpegEncoder;
import org.jmol.util.TextFormat;
import org.jmol.util.Parser;

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
import javax.vecmath.Point4f;
import javax.vecmath.Point3i;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;
import java.net.URL;
import java.io.ByteArrayOutputStream;
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

  protected void finalize() throws Throwable {
    Logger.debug("viewer finalize " + this);
    super.finalize();
  }

  // these are all private now so we are certain they are not 
  // being accesed by any other classes

  private Component display;
  private Graphics3D g3d;
  private JmolAdapter modelAdapter;

  private CommandHistory commandHistory = new CommandHistory();
  private ColorManager colorManager;
  private Eval eval;
  private FileManager fileManager;
  private ModelManager modelManager;
  public MouseManager mouseManager;
  private PickingManager pickingManager;
  private PropertyManager propertyManager;
  private RepaintManager repaintManager;
  private ScriptManager scriptManager;
  private SelectionManager selectionManager;
  private StateManager stateManager;
  private StateManager.GlobalSettings global;
  private StatusManager statusManager;
  private TempManager tempManager;
  private TransformManager transformManager;

  private String strJavaVendor;
  private String strJavaVersion;
  private String strOSName;
  private String htmlName = "";

  private boolean jvm11orGreater = false;
  private boolean jvm12orGreater = false;
  private boolean jvm14orGreater = false;

  Viewer(Component display, JmolAdapter modelAdapter) {
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("Viewer constructor " + this);
    }
    this.display = display;
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
    stateManager = new StateManager(this);
    g3d = new Graphics3D(display);
    colorManager = new ColorManager(this, g3d);
    initialize();
    statusManager = new StatusManager(this);
    scriptManager = new ScriptManager(this);
    //transformManager = new TransformManager11(this);
    transformManager = new TransformManager10(this);
    selectionManager = new SelectionManager(this);
    if (jvm14orGreater)
      mouseManager = MouseWrapper14.alloc(display, this);
    else if (jvm11orGreater)
      mouseManager = MouseWrapper11.alloc(display, this);
    else
      mouseManager = new MouseManager10(display, this);
    modelManager = new ModelManager(this);
    propertyManager = new PropertyManager(this);
    tempManager = new TempManager();
    pickingManager = new PickingManager(this);
    fileManager = new FileManager(this, modelAdapter);
    repaintManager = new RepaintManager(this);
    eval = new Eval(this);
  }

  /**
   * NOTE: for APPLICATION (not APPLET) call
   * 
   *   setModeMouse(JmolConstants.MOUSE_NONE);
   * 
   * before setting viewer=null
   * 
   * @param display       either DisplayPanel or WrappedApplet
   * @param modelAdapter  the model reader
   * @return              a viewer instance 
   */
  public static JmolViewer allocateViewer(Component display,
                                          JmolAdapter modelAdapter) {
    return new Viewer(display, modelAdapter);
  }

  boolean isSilent = false;
  boolean isApplet = false;
  boolean autoExit = false;
  String writeInfo;
  boolean haveDisplay = true;
  boolean mustRender = true;
  boolean checkScriptOnly = false;
  boolean fileOpenCheck = true;

  public boolean isApplet() {
    return (htmlName.length() > 0);
  }

  public void setAppletContext(String htmlName, URL documentBase, URL codeBase,
                               String appletProxyOrCommandOptions) {
    this.htmlName = htmlName;
    isApplet = (documentBase != null);
    String str = appletProxyOrCommandOptions;
    if (!isApplet) {
      // not an applet -- used to pass along command line options
      if (str.indexOf("-i") >= 0) {
        Logger.setLogLevel(3); //no info, but warnings and errors
        isSilent = true;
      }
      if (str.indexOf("-c") >= 0) {
        checkScriptOnly = true;
        fileOpenCheck = true;
      }
      if (str.indexOf("-C") >= 0) {
        checkScriptOnly = true;
        fileOpenCheck = false;
      }
      if (str.indexOf("-x") >= 0) {
        autoExit = true;
      }
      if (display == null || str.indexOf("-n") >= 0) {
        haveDisplay = false;
        display = null;
      }
      writeInfo = null;
      if (str.indexOf("-w") >= 0) {
        int i = str.indexOf("\1");
        int j = str.lastIndexOf("\1");
        writeInfo = str.substring(i + 1, j);
      }
      mustRender = (haveDisplay || writeInfo != null);
    }

    /*
     Logger.info("jvm11orGreater=" + jvm11orGreater
     + "\njvm12orGreater=" + jvm12orGreater + "\njvm14orGreater="
     + jvm14orGreater);
     */
    if (!isSilent) {
      Logger.info(JmolConstants.copyright + "\nJmol Version "
          + getJmolVersion() + "\njava.vendor:" + strJavaVendor
          + "\njava.version:" + strJavaVersion + "\nos.name:" + strOSName
          + "\nmemory:" + getParameter("_memory") + "\nappletId:" + htmlName);
    }

    if (isApplet)
      fileManager.setAppletContext(documentBase, codeBase,
          appletProxyOrCommandOptions);
    zap(false); //here to allow echos
  }

  static String getJmolVersion() {
    return JmolConstants.version + "  " + JmolConstants.date;
  }

  static int getJmolVersionInt() {
    String s = JmolConstants.version;
    //11.9.999 --> 1109999
    int i = s.indexOf(".");
    int v = Integer.parseInt(s.substring(0, i));
    s = s.substring(i + 1);
    i = s.indexOf(".");
    int subv = Integer.parseInt(s.substring(0, i));
    s = s.substring(i + 1);
    i = 0;
    while (i < s.length() && Character.isDigit(s.charAt(i)))
      i++;
    int subsubv = (i == 0 ? 0 : Integer.parseInt(s.substring(0, i)));
    return v * 100000 + subv * 1000 + subsubv;
  }

  String getHtmlName() {
    return htmlName;
  }

  boolean mustRenderFlag() {
    return mustRender && refreshing;
  }

  static int getLogLevel() {
    for (int i = 0; i < Logger.NB_LEVELS; i++)
      if (Logger.isActiveLevel(i))
        return Logger.NB_LEVELS - i;
    return 0;
  }

  public Component getAwtComponent() {
    return display;
  }

  public boolean handleOldJvm10Event(Event e) {
    return mouseManager.handleOldJvm10Event(e);
  }

  void reset() {
    //Eval.reset()
    //initializeModel
    transformManager.homePosition();
    if (modelManager.useXtalDefaults())
      stateManager.setCrystallographicDefaults();//frame.someModelsHavePeriodicOrigin);
    refresh(1, "Viewer:homePosition()");
  }

  public void homePosition() {
    script("reset");
  }

 
  /*
  final Hashtable imageCache = new Hashtable();

  void flushCachedImages() {
    imageCache.clear();
    colorManager.flushCachedColors();
  }
 */
 
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
  // delegated to StateManager
  // ///////////////////////////////////////////////////////////////

  void initialize() {
    resetAllParameters();
  }

  void resetAllParameters() {
    global = stateManager.getGlobalSettings();
    setIntProperty("_version", getJmolVersionInt());
    colorManager.resetElementColors();
    setObjectColor("background", "black");
    setObjectColor("axis1", "red");
    setObjectColor("axis2", "green");
    setObjectColor("axis3", "blue");
    setAmbientPercent(global.ambientPercent);
    setDiffusePercent(global.diffusePercent);
    setSpecular(global.specular);
    setSpecularPercent(global.specularPercent);
    setSpecularExponent(global.specularExponent);
    setSpecularPower(global.specularPower);
    setZShade(false);
  }

  String listSavedStates() {
    return stateManager.listSavedStates();
  }

  void saveOrientation(String saveName) {
    //from Eval
    stateManager.saveOrientation(saveName);
  }

  boolean restoreOrientation(String saveName, float timeSeconds) {
    //from Eval
    return stateManager.restoreOrientation(saveName, timeSeconds);
  }

  void saveBonds(String saveName) {
    //from Eval
    stateManager.saveBonds(saveName);
  }

  boolean restoreBonds(String saveName) {
    //from Eval
    return stateManager.restoreBonds(saveName);
  }

  void saveState(String saveName) {
    //from Eval
    stateManager.saveState(saveName);
  }

  String getSavedState(String saveName) {
    return stateManager.getSavedState(saveName);
  }

  boolean restoreState(String saveName) {
    //from Eval
    return stateManager.restoreState(saveName);
  }

  void saveSelection(String saveName) {
    //from Eval
    stateManager.saveSelection(saveName, selectionManager.bsSelection);
    stateManager.restoreSelection(saveName); //just to register the # of selected atoms
  }

  boolean restoreSelection(String saveName) {
    //from Eval
    return stateManager.restoreSelection(saveName);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to TransformManager
  // ///////////////////////////////////////////////////////////////

  public float getRotationRadius() {
    return transformManager.getRotationRadius();
  }

  public void setRotationRadius(float angstroms) {
    transformManager.setRotationRadius(angstroms);
  }

  Point3f getRotationCenter() {
    return transformManager.getRotationCenter();
  }

  void setCenterAt(String relativeTo, Point3f pt) {
    //Eval centerAt boundbox|absolute|average {pt}
    transformManager.setCenterAt(relativeTo, pt);
    refresh(0, "Viewer:setCenter(" + relativeTo + ")");
  }

  void setCenterBitSet(BitSet bsCenter, boolean doScale) {
    //Eval 
    //setCenterSelected

    Point3f center = (bsCenter != null && cardinalityOf(bsCenter) > 0 ? getAtomSetCenter(bsCenter)
        : null);
    transformManager.setNewRotationCenter(center, doScale);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  void setNewRotationCenter(Point3f center) {
    // eval ???
    transformManager.setNewRotationCenter(center, true);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  Point3f getNavigationCenter() {
    return transformManager.getNavigationCenter();
  }

  boolean getNavigationCentered() {
    return transformManager.isNavigationCentered();
  }

  float getNavigationDepthPercent() {
    return transformManager.getNavigationDepthPercent();
  }

  void navigate(int keyWhere, int modifiers) {
    transformManager.navigate(keyWhere, modifiers);
    if (!transformManager.vibrationOn)
      refresh(1, "Viewer:navigate()");
  }

  Point3f getNavigationOffset() {
    return transformManager.getNavigationOffset();
  }

  float getNavigationOffsetPercent(char XorY) {
    return transformManager.getNavigationOffsetPercent(XorY);
  }

  boolean getNavigating() {
    return transformManager.getNavigating();
  }

  void move(Vector3f dRot, float dZoom, Vector3f dTrans, float dSlab,
            float floatSecondsTotal, int fps) {
    //from Eval
    transformManager.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  void moveTo(float floatSecondsTotal, Point3f center, Point3f pt,
              float degrees, float zoom, float xTrans, float yTrans,
              float rotationRadius, Point3f navCenter, float xNav, float yNav,
              float navDepth) {
    //from Eval
    transformManager.moveTo(floatSecondsTotal, center, pt, degrees, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  void moveTo(float floatSecondsTotal, Matrix3f rotationMatrix, Point3f center,
              float zoom, float xTrans, float yTrans, float rotationRadius,
              Point3f navCenter, float xNav, float yNav, float navDepth) {
    //from StateManager
    transformManager.moveTo(floatSecondsTotal, rotationMatrix, center, zoom,
        xTrans, yTrans, rotationRadius, navCenter, xNav, yNav, navDepth);
  }

  String getMoveToText(float timespan) {
    return transformManager.getMoveToText(timespan);
  }

  void navigate(float timeSeconds, Point3f[] path, float[] theta,
                int indexStart, int indexEnd) {
    transformManager.navigate(timeSeconds, path, theta, indexStart, indexEnd);
    refresh(1, "navigate");
  }

  void navigate(float timeSeconds, Point3f center) {
    transformManager.navigate(timeSeconds, center);
    refresh(1, "navigate");
  }

  void navigate(float timeSeconds, Point3f[][] pathGuide) {
    transformManager.navigate(timeSeconds, pathGuide);
    refresh(1, "navigate");
  }

  void navigate(float timeSeconds, Vector3f rotAxis, float degrees) {
    transformManager.navigate(timeSeconds, rotAxis, degrees);
    refresh(1, "navigate");
  }

  void navTranslate(float timeSeconds, Point3f center) {
    transformManager.navTranslate(timeSeconds, center);
    refresh(1, "navigate");
  }

  void navTranslatePercent(float timeSeconds, float x, float y) {
    transformManager.navTranslatePercent(timeSeconds, x, y);
    refresh(1, "navigate");
  }
  
  void rotateMolecule(int deltaX, int deltaY) {
    setRotateSelected(true);
    setRotateMolecule(true);
    rotateXYBy(deltaX, deltaY);
    setRotateMolecule(false);
    setRotateSelected(false);
  }

  void rotateXYBy(int xDelta, int yDelta) {
    //mouseSinglePressDrag
    transformManager.rotateXYBy(xDelta, yDelta);
    refresh(1, "Viewer:rotateXYBy()");
  }

  void rotateZBy(int zDelta) {
    //mouseSinglePressDrag
    transformManager.rotateZBy(zDelta);
    refresh(1, "Viewer:rotateZBy()");
  }

  public void rotateFront() {
    //deprecated
    transformManager.rotateFront();
    refresh(1, "Viewer:rotateFront()");
  }

  public void rotateToX(float angleRadians) {
    //deprecated
    transformManager.rotateToX(angleRadians);
    refresh(1, "Viewer:rotateToX()");
  }

  public void rotateToY(float angleRadians) {
    //deprecated
    transformManager.rotateToY(angleRadians);
    refresh(1, "Viewer:rotateToY()");
  }

  public void rotateToZ(float angleRadians) {
    //deprecated
    transformManager.rotateToZ(angleRadians);
    refresh(1, "Viewer:rotateToZ()");
  }

  public void rotateToX(int angleDegrees) {
    //deprecated
    rotateToX(angleDegrees * Measurement.radiansPerDegree);
  }

  public void rotateToY(int angleDegrees) {
    //deprecated
    rotateToY(angleDegrees * Measurement.radiansPerDegree);
  }

  void translateXYBy(int xDelta, int yDelta) {
    //mouseDoublePressDrag, mouseSinglePressDrag
    transformManager.translateXYBy(xDelta, yDelta);
    refresh(1, "Viewer:translateXYBy()");
  }

  void translateToXPercent(float percent) {
    //Eval.translate()
    transformManager.translateToXPercent(percent);
    refresh(1, "Viewer:translateToXPercent()");
  }

  void translateToYPercent(float percent) {
    //Eval.translate()
    transformManager.translateToYPercent(percent);
    refresh(1, "Viewer:translateToYPercent()");
  }

  void translateToZPercent(float percent) {
    transformManager.translateToZPercent(percent);
    refresh(1, "Viewer:translateToZPercent()");
    //Eval.translate()
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

  void zoomBy(int pixels) {
    //MouseManager.mouseSinglePressDrag
    transformManager.zoomBy(pixels);
    refresh(1, "Viewer:zoomBy()");
  }

  public int getZoomPercent() {
    //deprecated 
    return (int) getZoomPercentFloat();
  }

  public float getZoomPercentFloat() {
    return transformManager.getZoomPercentFloat();
  }

  float getMaxZoomPercent() {
    return TransformManager.MAXIMUM_ZOOM_PERCENTAGE;
  }

  void zoomToPercent(float percent) {
    transformManager.zoomToPercent(percent);
    refresh(1, "Viewer:zoomToPercent()");
  }

  void zoomByPercent(int percent) {
    //Eval.zoom
    //MouseManager.mouseWheel
    //stateManager.setCommonDefaults
    transformManager.zoomByPercent(percent);
    refresh(1, "Viewer:zoomByPercent()");
  }

  private void setZoomEnabled(boolean zoomEnabled) {
    global.setParameterValue("zoomEnabled", zoomEnabled);
    transformManager.setZoomEnabled(zoomEnabled);
    refresh(1, "Viewer:setZoomEnabled()");
  }

  void slabReset() {
    transformManager.slabReset();
  }

  boolean getZoomEnabled() {
    return transformManager.zoomEnabled;
  }

  boolean getSlabEnabled() {
    return transformManager.slabEnabled;
  }

  void slabByPixels(int pixels) {
    //MouseManager.mouseSinglePressDrag
    transformManager.slabByPercentagePoints(pixels);
    refresh(0, "Viewer:slabByPixels()");
  }

  void depthByPixels(int pixels) {
    //MouseManager.mouseDoublePressDrag
    transformManager.depthByPercentagePoints(pixels);
    refresh(0, "Viewer:depthByPixels()");
  }

  void slabDepthByPixels(int pixels) {
    //MouseManager.mouseSinglePressDrag
    transformManager.slabDepthByPercentagePoints(pixels);
    refresh(0, "Viewer:slabDepthByPixels()");
  }

  void slabToPercent(int percentSlab) {
    //Eval.slab
    transformManager.slabToPercent(percentSlab);
    refresh(0, "Viewer:slabToPercent()");
  }

  void slabInternal(Point4f plane, boolean isDepth) {
    transformManager.slabInternal(plane, isDepth);
  }

  Point4f getDepthPlane(boolean isInternal) {
    return transformManager.getDepthPlane(isInternal);
  }

  Point4f getSlabPlane(boolean isInternal) {
    return transformManager.getSlabPlane(isInternal);
  }

  void slabInternalReference(Point3f ptRef) {
    transformManager.slabInternalReference(ptRef);
  }

  Point3f getSlabInternalReference() {
    return transformManager.slabRef;
  }

  void depthToPercent(int percentDepth) {
    //Eval.depth
    transformManager.depthToPercent(percentDepth);
    refresh(0, "Viewer:depthToPercent()");
  }

  private void setSlabEnabled(boolean slabEnabled) {
    //Eval.slab
    transformManager.setSlabEnabled(slabEnabled);
    refresh(0, "Viewer:setSlabEnabled()");
  }

  void setSlabDepthInternal(boolean isDepth) {
    transformManager.setSlabDepthInternal(isDepth);
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  void finalizeTransformParameters() {
    //FrameRenderer
    transformManager.finalizeTransformParameters();
    g3d.setSlabAndDepthValues(transformManager.slabValue,
        transformManager.depthValue, global.zShade);
  }

  Point3i transformPoint(Point3f pointAngstroms) {
    return transformManager.transformPoint(pointAngstroms);
  }

  Point3i transformPoint(Point3f pointAngstroms, Vector3f vibrationVector) {
    return transformManager.transformPoint(pointAngstroms, vibrationVector);
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

  void unTransformPoint(Point3f pointScreen, Point3f pointAngstroms) {
    //called by Draw.move2D
    transformManager.unTransformPoint(pointScreen, pointAngstroms);
  }

  float getScalePixelsPerAngstrom() {
    return transformManager.scalePixelsPerAngstrom;
  }

  short scaleToScreen(int z, int milliAngstroms) {
    //all shapes    
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  float scaleToPerspective(int z, float sizeAngstroms) {
    //DotsRenderer
    return transformManager.scaleToPerspective(z, sizeAngstroms);
  }

  private void setScaleAngstromsPerInch(float angstromsPerInch) {
    //Eval.setScale3d
    transformManager.setScaleAngstromsPerInch(angstromsPerInch);
  }

  void setSpinX(int value) {
    //Eval
    transformManager.setSpinX(value);
  }

  String getSpinState() {
    return transformManager.getSpinState(false);
  }

  float getSpinX() {
    return transformManager.spinX;
  }

  void setSpinY(int value) {
    //Eval
    transformManager.setSpinY(value);
  }

  float getSpinY() {
    return transformManager.spinY;
  }

  void setSpinZ(int value) {
    //Eval
    transformManager.setSpinZ(value);
  }

  float getSpinZ() {
    return transformManager.spinZ;
  }

  void setSpinFps(int value) {
    //Eval
    transformManager.setSpinFps(value);
  }

  float getSpinFps() {
    return transformManager.spinFps;
  }

  void setSpinOn(boolean spinOn) {
    //Eval
    //startSpinningAxis
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

  void getRotation(Matrix3f matrixRotation) {
    transformManager.getRotation(matrixRotation);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to ColorManager
  // ///////////////////////////////////////////////////////////////

  private void setDefaultColors(String colorScheme) {
    colorManager.setDefaultColors(colorScheme);
  }

  private void setDefaultTranslucent(float value) {
    global.defaultTranslucent = value;  
  }
  
  float getDefaultTranslucent() {
    return global.defaultTranslucent;
  }
  
  int getColixArgb(short colix) {
    return g3d.getColixArgb(colix);
  }

  void setRubberbandArgb(int argb) {
    //Eval
    colorManager.setRubberbandArgb(argb);
  }

  short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  void setElementArgb(int elementNumber, int argb) {
    //Eval
    global.setParameterValue("_color "
        + JmolConstants.elementNameFromNumber(elementNumber), StateManager
        .escapeColor(argb));
    colorManager.setElementArgb(elementNumber, argb);
  }

  float getVectorScale() {
    return global.vectorScale;
  }

  public void setVectorScale(float scale) {
    global.setParameterValue("vectorScale", scale);
    global.vectorScale = scale;
    refresh(0, "set vectorScale");
  }

  float getVibrationScale() {
    return global.vibrationScale;
  }

  float getVibrationPeriod() {
    return global.vibrationPeriod;
  }

  public void setVibrationScale(float scale) {
    //Eval
    
    transformManager.setVibrationScale(scale);
    global.vibrationScale = scale;
    //because this is public:
    global.setParameterValue("vibrationScale", scale);
  }

  void setVibrationOff() {
    transformManager.setVibrationPeriod(0);    
  }
  
  public void setVibrationPeriod(float period) {
    //Eval
    transformManager.setVibrationPeriod(period);
    period = Math.abs(period);
    global.vibrationPeriod = period;
    //because this is public:
    global.setParameterValue("vibrationPeriod", period);
  }

  void setObjectColor(String name, String colorName) {
    if (colorName == null || colorName.length() == 0)
      return;
    setObjectArgb(name, Graphics3D.getArgbFromString(colorName));
  }

  void setObjectArgb(String name, int argb) {
    int objId = StateManager.getObjectIdFromName(name);
    if (objId < 0)
      return;
    global.objColors[objId] = argb;
    switch(objId) {
    case StateManager.OBJ_BACKGROUND:
      g3d.setBackgroundArgb(argb);
      colorManager.setColixBackgroundContrast(argb);
      break;
    }
    global.setParameterValue(name + "Color", StateManager.escapeColor(argb));
  }
  
  int getObjectArgb(int objId) {
    return global.objColors[objId];
  }

  short getObjectColix(int objId) {
    int argb = getObjectArgb(objId);
    if (argb == 0)
      return getColixBackgroundContrast();
    return g3d.getColix(argb); 
  }
  
 String getObjectState(String name) {
   int objId = StateManager.getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
   if (objId < 0)
     return "";
   short mad = getObjectMad(objId);
   StringBuffer s = new StringBuffer();
   Shape.appendCmd(s, name
      + (mad == 0 ? " off" : mad == 1 ? " on" : mad == -1 ? " dotted" 
          : mad < 20 ? " " + mad : " " + (mad / 2000f)));
  return s.toString();
}
  //for historical reasons, leave these two:
  
  public void setColorBackground(String colorName) {
    setObjectColor("background", colorName);
  }

  public int getBackgroundArgb() {
    return getObjectArgb(StateManager.OBJ_BACKGROUND);
  }

  void setObjectMad(int iShape, String name, short mad) {
    int objId = StateManager.getObjectIdFromName(name.equalsIgnoreCase("axes") ? "axis" : name);
    if (objId < 0)
      return;
    if (mad == -2 || mad == -4) { //turn on if not set "showAxes = true"
      short m = (short)(mad + 3);
      mad = getObjectMad(objId);
      if (mad == 0)
        mad = m;
    }
    global.setParameterValue("show" + name, mad != 0);
    global.objStateOn[objId] = (mad !=0);
    if (mad ==0)
      return;
      global.objMad[objId] = mad;
    setShapeSize(iShape, mad); //just loads it
  }

  short getObjectMad(int objId) {
    return (global.objStateOn[objId] ? global.objMad[objId] : 0);
  }
  
  void setPropertyColorScheme(String scheme) {
    global.propertyColorScheme = scheme;  
  }
  
  String getPropertyColorScheme() {
    return global.propertyColorScheme;  
  }
  
  short getColixBackgroundContrast() {
    return colorManager.colixBackgroundContrast;
  }

  private void setSpecular(boolean specular) {
    //Eval
    colorManager.setSpecular(specular);
    global.specular = specular;
  }

  boolean getSpecular() {
    return colorManager.getSpecular();
  }

  private void setSpecularPower(int specularPower) {
    //Eval
    colorManager.setSpecularPower(Math.abs(specularPower));
    global.specularPower = specularPower;
  }

  private void setSpecularExponent(int specularExponent) {
    //Eval
    colorManager.setSpecularPower(-Math.abs(specularExponent));
    global.specularExponent = specularExponent;
  }

  String getSpecularState() {
    return global.getSpecularState();
  }

  private void setZShade(boolean TF) {
    global.zShade = TF;
  }

  boolean getZShade() {
    return global.zShade;
  }

  private void setAmbientPercent(int ambientPercent) {
    //Eval
    colorManager.setAmbientPercent(ambientPercent);
    global.ambientPercent = ambientPercent;
  }

  int getAmbientPercent() {
    return colorManager.getAmbientPercent();
  }

  private void setDiffusePercent(int diffusePercent) {
    //Eval
    colorManager.setDiffusePercent(diffusePercent);
    global.diffusePercent = diffusePercent;
  }

  int getDiffusePercent() {
    return colorManager.getDiffusePercent();
  }

  private void setSpecularPercent(int specularPercent) {
    //Eval
    colorManager.setSpecularPercent(specularPercent);
    global.specularPercent = specularPercent;
  }

  int getSpecularPercent() {
    return colorManager.getSpecularPercent();
  }

  short getColixAtomPalette(Atom atom, byte pid) {
    return colorManager.getColixAtomPalette(atom, pid);
  }

  short getColixHbondType(short order) {
    return colorManager.getColixHbondType(order);
  }

  int setColorScheme(String colorScheme) {
    //isosurface
    return colorManager.setColorScheme(colorScheme); 
  }

  short getColixFromPalette(float val, float rangeMin, float rangeMax) {
    //isosurface
    return colorManager.getColixFromPalette(val, rangeMin, rangeMax);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to SelectionManager
  // ///////////////////////////////////////////////////////////////

  void select(BitSet bs, boolean isQuiet) {
    //Eval
    selectionManager.select(bs, isQuiet);
  }

  void selectBonds(BitSet bs) {
    selectionManager.selectBonds(bs);
  }

  boolean isBondSelection() {
    return !selectionManager.selectionModeAtoms;
  }

  BitSet getSelectedAtomsOrBonds() {
    return selectionManager.getSelectedAtomsOrBonds();
  }

  BitSet getSelectedBonds() {
    return selectionManager.bsBonds;
  }

  void hide(BitSet bs, boolean isQuiet) {
    //Eval
    selectionManager.hide(bs, isQuiet);
  }

  void display(BitSet bsAll, BitSet bs, boolean isQuiet) {
    //Eval
    selectionManager.display(bsAll, bs, isQuiet);
  }

  BitSet getHiddenSet() {
    return selectionManager.getHiddenSet();
  }

  boolean isSelected(int atomIndex) {
    return selectionManager.isSelected(atomIndex);
  }

  boolean isInSelectionSubset(int atomIndex) {
    return selectionManager.isInSelectionSubset(atomIndex);
  }

  void reportSelection(String msg) {
    if (modelManager.getSelectionHaloEnabled())
      setTainted(true);
    scriptStatus(msg);
  }

  public void selectAll() {
    //initializeModel
    selectionManager.selectAll();
    refresh(0, "Viewer:selectAll()");
  }

  public void clearSelection() {
    //not used in this project; in jmolViewer interface, though
    selectionManager.clearSelection();
    refresh(0, "Viewer:clearSelection()");
  }

  public void setSelectionSet(BitSet set) {
    //not used in this project; in jmolViewer interface, though
    selectionManager.setSelectionSet(set);
    refresh(0, "Viewer:setSelectionSet()");
  }

  void setSelectionSubset(BitSet subset) {
    selectionManager.setSelectionSubset(subset);
  }

  private void setHideNotSelected(boolean TF) {
    selectionManager.setHideNotSelected(TF);
  }

  void invertSelection() {
    //Eval
    selectionManager.invertSelection();
    // only used from a script, so I do not think a refresh() is necessary
  }

  BitSet getSelectionSet() {
    return selectionManager.bsSelection;
  }

  public int getSelectionCount() {
    return selectionManager.getSelectionCount();
  }

  void setFormalCharges(int formalCharge) {
    modelManager.setFormalCharges(selectionManager.bsSelection, formalCharge);
  }

  public void addSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  public void removeSelectionListener(JmolSelectionListener listener) {
    selectionManager.addListener(listener);
  }

  BitSet getAtomBitSet(Object atomExpression) {
    // typically a string such as "(atomno < 3)"
    return Eval.getAtomBitSet(eval, this, atomExpression);
  }

  Vector getAtomBitSetVector(Object atomExpression) {
    return Eval.getAtomBitSetVector(eval, this, atomExpression);
  }

  int firstAtomOf(BitSet bs) {
    return modelManager.firstAtomOf(bs);
  }

  Point3f getAtomSetCenter(BitSet bs) {
    return modelManager.getAtomSetCenter(bs);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to MouseManager
  // ///////////////////////////////////////////////////////////////

  public void setModeMouse(int modeMouse) {
    //call before setting viewer=null
    mouseManager.setModeMouse(modeMouse);
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

  private void setAppletProxy(String appletProxy) {
    //Eval
    fileManager.setAppletProxy(appletProxy);
  }

  private void setDefaultDirectory(String dir) {
    global.defaultDirectory = (dir == null || dir.length() == 0 ? null
        : TextFormat.simpleReplace(dir, "\\", "/"));
  }

  String getDefaultDirectory() {
    return global.defaultDirectory;
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
    //Jmol app file dropper, main, OpenUrlAction, RecentFilesAction
    String type = fileManager.getFileTypeName(name);
    checkHalt("exit");
    // assumes a Jmol script file if no other file type
    script((type == null ? "script " : "load ")
        + StateManager.escape(TextFormat.simpleReplace(name, "\\", "/")));
  }

  void openFile(String name, int[] params, String loadScript, boolean isMerge) {
    //Eval
    if (name == null)
      return;
    if (name.equalsIgnoreCase("string")) {
      openStringInline(fileManager.inlineData, params, isMerge);
      return;
    }
    if (name.equalsIgnoreCase("string[]")) {
      openStringInline(fileManager.inlineDataArray, params, isMerge);
      return;
    }
    if (!isMerge)
      zap(false);
    long timeBegin = System.currentTimeMillis();
    fileManager.openFile(name, params, loadScript, isMerge);
    long ms = System.currentTimeMillis() - timeBegin;
    setStatusFileLoaded(1, name, "", getModelSetName(), null, null);
    String sp = "";
    if (params != null)
      for (int i = 0; i < params.length; i++)
        sp += "," + params[i];
    Logger.info("openFile(" + name + sp + ")" + ms + " ms");
  }

  public void openFiles(String modelName, String[] names) {
    openFiles(modelName, names, null, false);
  }

  void openFiles(String modelName, String[] names, String loadScript, boolean isMerge) {
    //Eval
    if (!isMerge)
      zap(false);
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFiles(modelName, names, loadScript, isMerge);
    long ms = System.currentTimeMillis() - timeBegin;
    for (int i = 0; i < names.length; i++) {
      setStatusFileLoaded(1, names[i], "", getModelSetName(), null, null);
    }
    Logger.info("openFiles(" + names.length + ") " + ms + " ms");
  }

  public void openStringInline(String strModel) {
    //Jmol app file dropper
    openStringInline(strModel, null, false);
  }

  private void openStringInline(String strModel, int[] params, boolean isMerge) {
    //loadInline, openFile, openStringInline
    if (!isMerge)
      clear();
    fileManager.openStringInline(strModel, params, isMerge);
    String errorMsg = getOpenFileError(isMerge);
    if (errorMsg == null)
      setStatusFileLoaded(1, "string", "", getModelSetName(), null, null);
  }

  private void openStringInline(String[] arrayModels, int[] params, boolean isMerge) {
    //loadInline, openFile, openStringInline
    if (!isMerge)
      clear();
    fileManager.openStringInline(arrayModels, params, isMerge);
    String errorMsg = getOpenFileError(isMerge);
    if (errorMsg == null)
      setStatusFileLoaded(1, "string[]", "", getModelSetName(), null, null);
  }

  public char getInlineChar() {
    return global.inlineNewlineChar;
  }

  public void loadInline(String strModel) {
    //applet Console, loadInline, app PasteClipboard
    loadInline(strModel, global.inlineNewlineChar);
  }

  public void loadInline(String strModel, char newLine) {
    loadInline(strModel, newLine, false);
  }

  void loadInline(String strModel, char newLine, boolean isMerge) {
    if (strModel == null)
      return;
    int i;
    int[] A = global.getDefaultLatticeArray();
    Logger.debug(strModel);
    if (newLine != 0 && newLine != '\n') {
      int len = strModel.length();
      for (i = 0; i < len && strModel.charAt(i) == ' '; ++i) {
      }
      if (i < len && strModel.charAt(i) == newLine)
        strModel = strModel.substring(i + 1);
      strModel = TextFormat.simpleReplace(strModel, "" + newLine, "\n");
    }
    String datasep = (String) global.getParameter("dataseparator");
    if (datasep != null && datasep != "" && (i = strModel.indexOf(datasep)) >= 0) {
      int n = 2;
      while ((i = strModel.indexOf(datasep, i + 1)) >= 0)
        n++;
      String[] strModels = new String[n];
      int pt = 0, pt0 = 0;
      for (i = 0; i < n; i++) {
        pt = strModel.indexOf(datasep, pt0);
        if (pt < 0)
          pt = strModel.length();
        strModels[i] = strModel.substring(pt0, pt);
        pt0 = pt + datasep.length();
      }
      openStringInline(strModels, A, isMerge);
      return;
    }
    openStringInline(strModel, A, isMerge);
  }

  public void loadInline(String[] arrayModels) {
    loadInline(arrayModels, false);  
  }
  
  void loadInline(String[] arrayModels, boolean isMerge) {
    //Eval data
    //loadInline
    if (arrayModels == null || arrayModels.length == 0)
      return;
    int[] A = global.getDefaultLatticeArray();
    openStringInline(arrayModels, A, isMerge);
  }

  void loadCoordinates(String coordinateData) {
    modelManager.loadCoordinates(coordinateData);
  }
  
  public void openDOM(Object DOMNode) {
    //applet.loadDOMNode
    clear();
    long timeBegin = System.currentTimeMillis();
    fileManager.openDOM(DOMNode);
    long ms = System.currentTimeMillis() - timeBegin;
    Logger.info("openDOM " + ms + " ms");
    setStatusFileLoaded(1, "JSNode", "", getModelSetName(), null,
        getOpenFileError());
  }

  /**
   * Opens the file, given the reader.
   * 
   * name is a text name of the file ... to be displayed in the window no need
   * to pass a BufferedReader ... ... the FileManager will wrap a buffer around
   * it
   * 
   * not referenced in this project
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

  /**
   * misnamed -- really this opens the file, gets the data, and returns error or null
   * 
   * @return errorMsg
   */
  public String getOpenFileError() {
    return getOpenFileError(false);  
  }
  
  String getOpenFileError(boolean isMerge) {
    String fullPathName = getFullPathName();
    String fileName = getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      setStatusFileNotLoaded(fullPathName, errorMsg);
      if (errorMsg != null)
        zap(errorMsg);
      return errorMsg;
    }
    if (isMerge) {
      modelManager.merge(modelAdapter, clientFile);
      setTainted(true);
    } else {
      openClientFile(fullPathName, fileName, clientFile);
    } 
    return null;
  }

  public void openClientFile(String fullPathName, String fileName,
                             Object clientFile) {
    // maybe there needs to be a call to clear()
    // or something like that here
    // for when CdkEditBus calls this directly
    setStatusFileLoaded(2, fullPathName, fileName, modelManager
        .getModelSetName(), clientFile, null);
    pushHoldRepaint();
    modelManager
        .setClientFile(fullPathName, fileName, modelAdapter, clientFile);
    initializeModel();
    popHoldRepaint();
    setStatusFileLoaded(3, fullPathName, fileName, modelManager
        .getModelSetName(), clientFile, null);
  }

  public String getCurrentFileAsString() {
    if (getFullPathName() == "string") {
      return fileManager.inlineData;
    }
    if (getFullPathName() == "string[]") {
      int modelIndex = getDisplayModelIndex();
      if (modelIndex < 0)
        return "";
      return fileManager.inlineDataArray[modelIndex];
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

  Point3f[] calculateSurface(BitSet bsSelected, BitSet bsIgnore,
                             float envelopeRadius) {
    if (bsSelected == null)
      bsSelected = getSelectionSet();
    return modelManager.calculateSurface(bsSelected, bsIgnore, envelopeRadius);
  }
  
  void addStateScript(String script) {
    modelManager.addStateScript(script);
  }

  public boolean getEchoStateActive() {
    return modelManager.getEchoStateActive();
  }

  void setEchoStateActive(boolean TF) {
    modelManager.setEchoStateActive(TF);
  }

  void zap(boolean notify) {
    //Eval
    //setAppletContext
    clear();
    modelManager.zap();
    initializeModel();
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long bTotal = runtime.totalMemory();
    long bFree = runtime.freeMemory();
    long bMax = 0;
    try {
      bMax = runtime.maxMemory();
    } catch (Exception e) {
    }
    Logger.error("ZAP memory inuse, total, free, max: " + (bTotal - bFree)
        + " " + bTotal + " " + bFree + " " + bMax);
    if (notify)
      setStatusFileLoaded(0, null, null, null, null, null);
     // System.out.println(Token.getSetParameters());
  }

  private void zap(String msg) {
    zap(true);
    echoMessage(msg);
  }

  void echoMessage(String msg) {
    int iShape = JmolConstants.SHAPE_ECHO;
    loadShape(iShape);
    setShapeProperty(iShape, "font", getFont3D("SansSerif", "Plain", 9));
    setShapeProperty(iShape, "target", "error");
    setShapeProperty(iShape, "text", msg);
  }

  private void clear() {
    if (modelManager.getFrame() == null)
      return;
    fileManager.clear();
    repaintManager.clear();
    transformManager.clear();
    pickingManager.clear();
    selectionManager.clear();
    clearAllMeasurements();
    modelManager.clear();
    mouseManager.clear();
    statusManager.clear();
    stateManager.clear(global);
    tempManager.clear();
    //setRefreshing(true);
    refresh(0, "Viewer:clear()");
    System.gc();
  }

  private void initializeModel() {
    reset();
    selectAll();
    transformManager.setCenter();
    if (eval != null)
      eval.clearDefinitionsAndLoadPredefined();
    // there probably needs to be a better startup mechanism for shapes

    repaintManager.initializePointers(1);
    setCurrentModelIndex(0);
    setBackgroundModelIndex(-1);
    setFrankOn(getShowFrank());
    mouseManager.startHoverWatcher(true);
    setTainted(true);
  }

  public String getModelSetName() {
    return modelManager.getModelSetName();
  }

  public String getModelSetFileName() {
    return modelManager.getModelSetFileName();
  }

  public String getUnitCellInfoText() {
    return modelManager.getUnitCellInfoText();
  }

  public String getSpaceGroupInfoText(String spaceGroup) {
    return modelManager.getSpaceGroupInfoText(spaceGroup);
  }

  public int getSpaceGroupIndexFromName(String spaceGroup) {
    return modelManager.getSpaceGroupIndexFromName(spaceGroup);
  }

  void getPolymerPointsAndVectors(BitSet bs, Vector vList) {
    modelManager.getPolymerPointsAndVectors(bs, vList);
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

  public void calculateStructures() {
    //Eval
    modelManager.calculateStructures();
    addStateScript("calculate structure");
  }

  void clearBfactorRange() {
    //Eval
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

  // this is a problem. SmarterJmolAdapter doesn't implement this;
  // it can only return null. 

  String getClientAtomStringProperty(Object clientAtom, String propertyName) {
    if (modelAdapter == null || propertyName == null
        || propertyName.length() == 0)
      return null;
    return modelAdapter.getClientAtomStringProperty(clientAtom, propertyName);
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
  /*  
   * 
    return new FrameExportJmolAdapter(this, getFrame());

    */
    return null;
  }
  
  public Frame getFrame() {
    return modelManager.getFrame();
  }

  Point3f getBoundBoxCenter() {
    return modelManager.getBoundBoxCenter();
  }

  Point3f getAverageAtomPoint() {
    return modelManager.getAverageAtomPoint();
  }

  float calcRotationRadius(Point3f center) {
    return modelManager.calcRotationRadius(center);
  }

  Vector3f getBoundBoxCornerVector() {
    return modelManager.getBoundBoxCornerVector();
  }

  Hashtable getBoundBoxInfo() {
    return modelManager.getBoundBoxInfo();
  }

  int getBoundBoxCenterX() {
    // used by the labelRenderer for rendering labels away from the center
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
    if (modelIndex < 0)
      return modelIndex;
    return modelManager.getModelNumber(modelIndex);
  }

  public int getModelFileNumber(int modelIndex) {
    if (modelIndex < 0)
      return 0;
    return modelManager.getModelFileNumber(modelIndex);
  }

  String getModelNumberDotted(int modelIndex) {
    if (modelIndex < 0)
      return "0";
    return getModelName(-1 - modelIndex);
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

  int getModelNumberIndex(int modelNumber, boolean useModelNumber) {
    return modelManager.getModelNumberIndex(modelNumber, useModelNumber);
  }

  boolean modelSetHasVibrationVectors() {
    return modelManager.modelSetHasVibrationVectors();
  }

  public boolean modelHasVibrationVectors(int modelIndex) {
    return modelSetHasVibrationVectors()
        && modelManager.modelHasVibrationVectors(modelIndex);
  }

  public int getChainCount() {
    return modelManager.getChainCount();
  }

  public int getChainCountInModel(int modelIndex) {
    return modelManager.getChainCountInModel(modelIndex);
  }

  public int getGroupCount() {
    return modelManager.getGroupCount();
  }

  public int getGroupCountInModel(int modelIndex) {
    return modelManager.getGroupCountInModel(modelIndex);
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

  public int getAtomCountInModel(int modelIndex) {
    return modelManager.getAtomCountInModel(modelIndex);
  }

  /**
   * For use in setting a for() construct max value
   * @return used size of the bonds array;
   */
  public int getBondCount() {
    return modelManager.getBondCount();
  }

  /**
   * from JmolPopup.udateModelSetComputedMenu
   * 
   * @param modelIndex the model of interest or -1 for all
   * @return the actual number of connections
   */
  public int getBondCountInModel(int modelIndex) {
    return modelManager.getBondCountInModel(modelIndex);
  }

  BitSet getBondsForSelectedAtoms(BitSet bsAtoms) {
    //eval
    return modelManager.getBondsForSelectedAtoms(bsAtoms);
  }

  boolean frankClicked(int x, int y) {
    return modelManager.frankClicked(x, y);
  }

  int findNearestAtomIndex(int x, int y) {
    //System.out.println("hover x y o :" + x + " " + y 
      //  + "  " + (x + y * getScreenWidth()));
   return modelManager.findNearestAtomIndex(x, y);
  }

  BitSet findAtomsInRectangle(Rectangle rectRubberBand) {
    return modelManager.findAtomsInRectangle(rectRubberBand);
  }

  void toCartesian(Point3f pt) {
    int modelIndex = getCurrentModelIndex();
    if (modelIndex < 0)
      return;
    modelManager.toCartesian(modelIndex, pt);
  }

  void toUnitCell(Point3f pt, Point3f offset) {
    int modelIndex = getCurrentModelIndex();
    if (modelIndex < 0)
      return;
    modelManager.toUnitCell(modelIndex, pt, offset);
  }

  public void setCenterSelected() {
    //depricated
    setCenterBitSet(selectionManager.bsSelection, true);
  }

  public void rebond() {
    //Eval, PreferencesDialog
    modelManager.rebond();
    refresh(0, "Viewer:rebond()");
  }

  public void setBondTolerance(float bondTolerance) {
    global.setParameterValue("bondTolerance", bondTolerance);
    global.bondTolerance = bondTolerance;
  }

  public float getBondTolerance() {
    return global.bondTolerance;
  }

  public void setMinBondDistance(float minBondDistance) {
    //PreferencesDialog
    global.setParameterValue("minBondDistance", minBondDistance);
    global.minBondDistance = minBondDistance;
  }

  public float getMinBondDistance() {
    return global.minBondDistance;
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

  BitSet getAtomsWithin(float distance, Point3f coord) {
    return modelManager.getAtomsWithin(distance, coord);
  }

  BitSet getAtomsWithin(float distance, Point4f plane) {
    return modelManager.getAtomsWithin(distance, plane);
  }

  BitSet getAtomsWithin(String withinWhat, String specInfo, BitSet bs) {
    return modelManager.getAtomsWithin(withinWhat, specInfo, bs);
  }

  BitSet getAtomsWithin(float distance, BitSet bs) {
    return modelManager.getAtomsWithin(distance, bs);
  }

  BitSet getAtomsConnected(float min, float max, int intType, BitSet bs) {
    return modelManager.getAtomsConnected(min, max, intType, bs);
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    return modelManager.getAtomIndexFromAtomNumber(atomNumber);
  }

  public BitSet getElementsPresentBitSet() {
    return modelManager.getElementsPresentBitSet();
  }

  public Hashtable getHeteroList(int modelIndex) {
    return modelManager.getHeteroList(modelIndex);
  }

  BitSet getVisibleSet() {
    return modelManager.getVisibleSet();
  }

  BitSet getClickableSet() {
    return modelManager.getClickableSet();
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

  Vector getAllAtomInfo(Object atomExpression) {
    return modelManager.getAllAtomInfo(getAtomBitSet(atomExpression));
  }

  Vector getAllBondInfo(Object atomExpression) {
    return modelManager.getAllBondInfo(getAtomBitSet(atomExpression));
  }

  Vector getMoleculeInfo(Object atomExpression) {
    return modelManager.getMoleculeInfo(getAtomBitSet(atomExpression));
  }

  public Hashtable getAllChainInfo(Object atomExpression) {
    return modelManager.getAllChainInfo(getAtomBitSet(atomExpression));
  }

  public Hashtable getAllPolymerInfo(Object atomExpression) {
    return modelManager.getAllPolymerInfo(getAtomBitSet(atomExpression));
  }

  public String getStateInfo() {
    StringBuffer s = new StringBuffer("# Jmol state version "
        + getJmolVersion() + ";\n\n");
    //  window state
    s.append(global.getWindowState());
    //  file state
    s.append(fileManager.getState());
    if (modelManager.fileName.equals("zapped"))
      s.append("zap;\n\n");
    //  numerical values
    s.append(global.getState());
    getDataState(s);
    //  definitions, connections, atoms, bonds, labels, echos, shapes
    s.append(modelManager.getState());
    //  frame information
    s.append(repaintManager.getState());
    //  orientation and slabbing
    s.append(transformManager.getState());
    //  display and selections
    s.append(selectionManager.getState());
    s.append("refreshing = true;\n");
    
    return s.toString();
  }

  static Hashtable dataValues = new Hashtable();

  static void setData(String type, Object[] data, int atomCount) {
    //Eval
    /*
     * data[0] -- label
     * data[1] -- string or float[]
     * data[2] -- selection bitset
     * 
     */
    if (type == null) {
      dataValues.clear();
      return;
    }
    if (data[2] != null) {
      float[] f = new float[atomCount];
      Parser.parseFloatArray((String) data[1], (BitSet)data[2], f);
      data[1] = f;
    }
    dataValues.put(type, data);
  }

  static public Object[] getData(String type) {
    if (dataValues == null)
      return null;
    if (type.equalsIgnoreCase("types")) {
      String[] info = new String[2];
      info[0] = "types";
      info[1] = "";
      int n = 0;
      Enumeration e = (dataValues.keys());
      while (e.hasMoreElements())
        info[1] += (n++ == 0 ? ",": "") + e.nextElement();
      return info;
    }
    return (Object[]) dataValues.get(type);
  }

  void setCurrentColorRange(String label) {
    float[] data = getDataFloat(label);
    BitSet bs = (data == null ? null : (BitSet) ((Object[]) getData(label))[2]);
    setCurrentColorRange(data, bs);
  }
  
  void setCurrentColorRange(float[] data, BitSet bs) {
    colorManager.setCurrentColorRange(data, bs, global.propertyColorScheme);
  }

  static public float[] getDataFloat(String label) {
    if (dataValues == null)
      return null;
    Object[] data = getData(label);
    if (data == null || !(data[1] instanceof float[]))
      return null;
    return (float[]) data[1];
  }

  static public float getDataFloat(String label, int atomIndex) {
    if (dataValues != null) {
      Object[] data = getData(label);
      if (data != null && data[1] instanceof float[]) {
        float[] f = (float[]) data[1];
        if (atomIndex < f.length)
          return f[atomIndex];
      }
    }
    return Float.NaN;
  }

  static private void getDataState(StringBuffer s) {
    if (dataValues == null)
      return;
    Enumeration e = (dataValues.keys());
    while (e.hasMoreElements()) {
      String name = (String) e.nextElement();
      if (name.indexOf("property_") == 0) {
        Object data = ((Object[]) dataValues.get(name))[1];
        s.append("DATA \"").append(name).append("\"");
        if (data instanceof float[]) {
          s.append("\n");
          float[] f = (float[]) data;
          for (int i = 0; i < f.length; i++)
            s.append(" ").append(f[i]);
          s.append("\n");
        } else {
          s.append("").append(data);
        }
        s.append("end \"").append(name).append("\";\n");
      }
    }
  }
  
  public String getAltLocListInModel(int modelIndex) {
    return modelManager.getAltLocListInModel(modelIndex);
  }

  public BitSet setConformation() {
    // user has selected some atoms, now this sets that as a conformation
    // with the effect of rewriting the cartoons to match

    return modelManager.setConformation(-1, getSelectionSet());
  }

  // AKA "configuration"
  public BitSet setConformation(int conformationIndex) {
    return modelManager.setConformation(getCurrentModelIndex(),
        conformationIndex);
  }

  int autoHbond(BitSet bsBonds) {
    //Eval
    BitSet bs = getSelectionSet();
    addStateScript("calculate hbonds");
    return autoHbond(bs, bs, bsBonds);
  }

  int autoHbond(BitSet bsFrom, BitSet bsTo, BitSet bsBonds) {
    //Eval
    return modelManager.autoHbond(bsFrom, bsTo, bsBonds);
  }

  boolean hbondsAreVisible() {
    return modelManager.hbondsAreVisible(getCurrentModelIndex());
  }

  public boolean havePartialCharges() {
    return modelManager.havePartialCharges();
  }

  UnitCell getCurrentUnitCell() {
    return modelManager.getUnitCell(getDisplayModelIndex());
  }

  Point3f getCurrentUnitCellOffset() {
    return modelManager.getUnitCellOffset(getDisplayModelIndex());
  }

  void setCurrentUnitCellOffset(int offset) {
    int modelIndex = getCurrentModelIndex();
    if (modelManager.setUnitCellOffset(modelIndex, offset))
      global.setParameterValue("_frame " + getModelNumber(modelIndex)
          + "; unitcell = ", offset);
  }

  void setCurrentUnitCellOffset(Point3f pt) {
    int modelIndex = getCurrentModelIndex();
    if (modelManager.setUnitCellOffset(modelIndex, pt))
      global.setParameterValue("_frame " + getModelNumber(modelIndex)
          + "; unitcell = ", StateManager.escape(pt));
  }

  /* ****************************************************************************
   * delegated to MeasurementManager
   ****************************************************************************/

  String getDefaultMeasurementLabel(int nPoints) {
    switch (nPoints) {
    case 2:
      return global.defaultDistanceLabel;
    case 3:
      return global.defaultAngleLabel;
    default:
      return global.defaultTorsionLabel;
    }
  }

  private void setDefaultMeasurementLabel(int nPoints, String format) {
    switch (nPoints) {
    case 2:
      global.defaultDistanceLabel = format;
    case 3:
      global.defaultAngleLabel = format;
    case 4:
      global.defaultTorsionLabel = format;
    }
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

  void clearAllMeasurements() {
    //Eval only
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "clear", null);
    refresh(0, "Viewer:clearAllMeasurements()");
  }

  public void clearMeasurements() {
    //depricated but in the API -- use "script" directly
    //see clearAllMeasurements()
    script("measures delete");
  }

  private void setJustifyMeasurements(boolean TF) {
    global.justifyMeasurements = TF;
  }

  boolean getJustifyMeasurements() {
    return global.justifyMeasurements;
  }

  void setMeasurementFormats(String strFormat) {
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
  }

  void defineMeasurement(Vector monitorExpressions, float[] rangeMinMax,
                         boolean isDelete, boolean isAllConnected,
                         boolean isShowHide, boolean isHidden, String strFormat) {
    //Eval.monitor()
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setConnected", Boolean.valueOf(
        isAllConnected));
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setRange", rangeMinMax);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormat", strFormat);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isDelete ? "deleteVector"
        : isShowHide ? (isHidden ? "hideVector" : "showVector")
            : "defineVector", monitorExpressions);
    setStatusNewDefaultModeMeasurement("scripted", 1, "?");
  }

  public void deleteMeasurement(int i) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete", new Integer(i));
  }

  void deleteMeasurement(int[] atomCountPlusIndices) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "delete",
        atomCountPlusIndices);
  }

  public void showMeasurement(int[] atomCountPlusIndices, boolean isON) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, isON ? "show" : "hide",
        atomCountPlusIndices);
    refresh(0, "Viewer:showMeasurements()");
  }

  void hideMeasurements(boolean isOFF) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "hideAll",
        Boolean.valueOf(isOFF));
    refresh(0, "hideMeasurements()");
  }

  void toggleMeasurement(int[] atomCountPlusIndices, String strFormat) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, (strFormat == null ? "toggle" : "toggleOn"),
        atomCountPlusIndices);
    if (strFormat != null)
      setShapeProperty(JmolConstants.SHAPE_MEASURES, "setFormats", strFormat);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to RepaintManager
  // ///////////////////////////////////////////////////////////////

  void repaint() {
    //from RepaintManager
    if (display==null)
      return;
    display.repaint();
  }

  void setAnimationDirection(int direction) {// 1 or -1
    //Eval
    repaintManager.setAnimationDirection(direction);
  }

  void reverseAnimation() {
    //Eval
    repaintManager.reverseAnimation();
  }

  int getAnimationDirection() {
    return repaintManager.animationDirection;
  }

  Hashtable getAnimationInfo() {
    return repaintManager.getAnimationInfo();
  }

  public void setAnimationFps(int fps) {
    global.setParameterValue("animationFps", fps);
    //Eval
    //app AtomSetChooser
    repaintManager.setAnimationFps(fps);
  }

  public int getAnimationFps() {
    return repaintManager.animationFps;
  }

  void setAnimationReplayMode(int replay, float firstFrameDelay,
                              float lastFrameDelay) {
    //Eval

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
    //Eval
    boolean wasAnimating = repaintManager.animationOn;
    if (animationOn == wasAnimating)
      return;
    repaintManager.setAnimationOn(animationOn);
  }

  void resumeAnimation() {
    //Eval
    if (repaintManager.animationOn) {
      Logger.debug("animation is ON in resumeAnimation");
      return;
    }
    repaintManager.resumeAnimation();
    refresh(0, "Viewer:resumeAnimation()");
  }

  void pauseAnimation() {
    //Eval
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
    //Eval
    if (repaintManager.setAnimationNext())
      refresh(0, "Viewer:setAnimationNext()");
  }

  void setAnimationPrevious() {
    //Eval
    if (repaintManager.setAnimationPrevious())
      refresh(0, "Viewer:setAnimationPrevious()");
  }

  void setAnimationLast() {
    //Eval
    repaintManager.setAnimationLast();
    refresh(0, "Viewer:setAnimationLast()");
  }

  void rewindAnimation() {
    //Eval
    repaintManager.rewindAnimation();
    refresh(0, "Viewer:rewindAnimation()");
  }

  void setCurrentModelIndex(int modelIndex) {
    //Eval
    //initializeModel
    repaintManager.setCurrentModelIndex(modelIndex);
  }

  int getCurrentModelIndex() {
    return repaintManager.currentModelIndex;
  }

  public int getDisplayModelIndex() {
    // modified to indicate if there is also a background model index
    int modelIndex = repaintManager.currentModelIndex;
    int backgroundIndex = getBackgroundModelIndex();
    return (backgroundIndex >= 0 ? -2 - modelIndex : modelIndex);
  }

  boolean haveFileSet() {
    return (getModelCount() > 1 && getModelNumber(0) > 1000000);
  }

  void setBackgroundModel(int modelNumber) {
    //Eval
    int modelIndex = getModelNumberIndex(modelNumber, !haveFileSet());
    setBackgroundModelIndex(modelIndex);
  }

  private void setBackgroundModelIndex(int modelIndex) {
    //initializeModel
    repaintManager.setBackgroundModelIndex(modelIndex);
  }

  public int getBackgroundModelIndex() {
    return repaintManager.backgroundModelIndex;
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
    //MouseManager, TransformManager
    // Logger.debug("viewer.setInMotion("+inMotion+")");
    if (wasInMotion ^ inMotion) {
      if (inMotion)
        ++motionEventNumber;
      repaintManager.setInMotion(inMotion);
      if (!inMotion)
        repaintManager.refresh();
      wasInMotion = inMotion;
    }
  }

  boolean getInMotion() {
    return repaintManager.inMotion;
  }

  public void pushHoldRepaint() {
    repaintManager.pushHoldRepaint();
  }

  public void popHoldRepaint() {
    repaintManager.popHoldRepaint();
  }

  private boolean refreshing = true;

  private void setRefreshing(boolean TF) {
    refreshing = TF;
  }

  public void refresh(int isOrientationChange, String strWhy) {
    //System.out.println("refresh: " +strWhy);
    repaintManager.refresh();
    statusManager.setStatusViewerRefreshed(isOrientationChange, strWhy);
  }

  void requestRepaintAndWait() {
    if (haveDisplay)
      repaintManager.requestRepaintAndWait();
  }

  public void repaintView() {
    repaintManager.repaintDone();
  }

  private boolean axesAreTainted = false;

  boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  ////////////// screen/image methods ///////////////

  final Dimension dimScreen = new Dimension();

  //final Rectangle rectClip = new Rectangle();

  public void setScreenDimension(Dimension dim) {
    // There is a bug in Netscape 4.7*+MacOS 9 when comparing dimension objects
    // so don't try dim1.equals(dim2)
    int height = dim.height;
    int width = dim.width;
    if (getStereoMode() == JmolConstants.STEREO_DOUBLE)
      width = (width + 1) / 2;
    if (dimScreen.width == width && dimScreen.height == height)
      return;
    resizeImage(width, height, false);
  }

  private void resizeImage(int width, int height, boolean useZoomLarge) {
    dimScreen.width = width;
    dimScreen.height = height;
    transformManager.setScreenDimension(width, height,
        useZoomLarge ? global.zoomLarge : false);
    g3d.setWindowSize(width, height, global.enableFullSceneAntialiasing);
  }
  
  public int getScreenWidth() {
    return dimScreen.width;
  }

  public int getScreenHeight() {
    return dimScreen.height;
  }

  /* not implemented
  private void setRectClip(Rectangle clip) {
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
  */

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    //System.out.println("renderScreen");
    if (isTainted || getSlabEnabled())
      setModelVisibility();
    isTainted = false;
    if (size != null)
      setScreenDimension(size);
    //setRectClip(null);
    int stereoMode = getStereoMode();
    switch (stereoMode) {
    case JmolConstants.STEREO_DOUBLE:
      render1(g, getImage(true, false), dimScreen.width, 0);
    case JmolConstants.STEREO_NONE:
      render1(g, getImage(false, false), 0, 0);
      break;
    case JmolConstants.STEREO_REDCYAN:
    case JmolConstants.STEREO_REDBLUE:
    case JmolConstants.STEREO_REDGREEN:
    case JmolConstants.STEREO_CUSTOM:
      render1(g, getStereoImage(stereoMode, false), 0, 0);
      break;
    }
    repaintView();
  }

  private Image getImage(boolean isDouble, boolean antialias) {
    Matrix3f matrixRotate = transformManager.getStereoRotationMatrix(isDouble);
    boolean twoPass = !getTestFlag1();
    g3d.beginRendering(//rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        matrixRotate, antialias, twoPass);
    repaintManager.render(g3d, modelManager.getFrame()); //, rectClip
    if (twoPass && g3d.setPass2())
      repaintManager.render(g3d, modelManager.getFrame()); //, rectClip
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized.
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  private Image getStereoImage(int stereoMode, boolean antialias) {
    boolean twoPass = !getTestFlag1();
    g3d.beginRendering(//rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        transformManager.getStereoRotationMatrix(true), antialias, twoPass);
    Frame frame = modelManager.getFrame();
    repaintManager.render(g3d, frame);//, rectClip
    if (twoPass && g3d.setPass2())
      repaintManager.render(g3d, frame);//, rectClip      
    g3d.endRendering();
    g3d.snapshotAnaglyphChannelBytes();
    g3d.beginRendering(//rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        transformManager.getStereoRotationMatrix(false), antialias, twoPass);
    repaintManager.render(g3d, frame);//, rectClip
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
    return g3d.getScreenImage();
  }

  private void render1(Graphics g, Image img, int x, int y) {
    if (g != null) {
      try {
        g.drawImage(img, x, y, null);
      } catch (NullPointerException npe) {
        Logger.error("Sun!! ... fix graphics your bugs!");
      }
    }
    g3d.releaseScreenImage();
  }

  public Image getScreenImage() {
    boolean antialias = true;
    boolean isStereo = false;
    //setRectClip(null);
    int stereoMode = getStereoMode();
    switch (stereoMode) {
    case JmolConstants.STEREO_DOUBLE:
      // this allows for getting both eye views in two images
      // because you can adjust using "stereo -2.5", then "stereo +2.5"
      isStereo = true;
      break;
    case JmolConstants.STEREO_REDCYAN:
    case JmolConstants.STEREO_REDBLUE:
    case JmolConstants.STEREO_REDGREEN:
    case JmolConstants.STEREO_CUSTOM:
      return getStereoImage(stereoMode, false);
    }
    return getImage(isStereo, antialias);
  }

  /**
   * @param quality
   * @return base64-encoded version of the image
   */
  public String getJpegBase64(int quality) {
    Image eImage = getScreenImage();
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    JpegEncoder jc = new JpegEncoder(eImage, quality, os);
    jc.Compress();
    byte[] jpeg = os.toByteArray();
    releaseScreenImage();
    return "" + Base64.getBase64(jpeg);
  }

  public void releaseScreenImage() {
    g3d.releaseScreenImage();
  }

  // ///////////////////////////////////////////////////////////////
  // routines for script support
  // ///////////////////////////////////////////////////////////////

  private void setAllowEmbeddedScripts(boolean TF) {
    global.allowEmbeddedScripts = TF;
  }

  boolean getAllowEmbeddedScripts() {
    return global.allowEmbeddedScripts;
  }

  public String evalFile(String strFilename) {
    //app -s flag
    int ptWait = strFilename.indexOf(" -nowait");
    if (ptWait >= 0) {
      return (String) evalStringWaitStatus("String", strFilename.substring(0, ptWait), "",
          true, false);
    }
    return scriptManager.addScript(strFilename, true, false);
  }

  public String script(String strScript) {
    return evalString(strScript);
  }

  String interruptScript = "";

  String getInterruptScript() {
    String s = interruptScript;
    interruptScript = "";
    if ((s != "") && Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug("interrupt: " + s);
    return s;
  }

  public String evalString(String strScript) {
    getInterruptScript();
    boolean isInterrupt = (strScript.length() > 0 && strScript.charAt(0) == '!');
    if (isInterrupt)
      strScript = strScript.substring(1);
    if (checkResume(strScript))
      return "script processing resumed";
    if (checkHalt(strScript))
      return "script execution halted";
    if (isScriptExecuting() && (isInterrupt || eval.isExecutionPaused())) {
      interruptScript = strScript;
      return "!" + strScript;
    }
    return scriptManager.addScript(strScript, false, false);
  }

  boolean usingScriptQueue() {
    return scriptManager.useQueue;
  }

  public void clearScriptQueue() {
    //Eval
    //checkHalt **
    scriptManager.clearQueue();
  }

  public boolean checkResume(String strScript) {
    if (strScript.equalsIgnoreCase("resume")) {
      resumeScriptExecution();
      return true;
    }
    return false;
  }

  public boolean checkHalt(String strScript) {
    String str = strScript.toLowerCase();
    if (str.equals("pause")) {
      pauseScriptExecution();
      return true;
    }
    if (str.startsWith("exit")) {
      haltScriptExecution();
      clearScriptQueue();
      if (checkScriptOnly)
        Logger.info("exit -- stops script checking");
      checkScriptOnly = false;
      return str.equals("exit");
    }
    if (str.startsWith("quit")) {
      haltScriptExecution();
      if (checkScriptOnly)
        Logger.info("quit -- stops script checking");
      checkScriptOnly = false;
      return str.equals("quit");
    }
    return false;
  }

  public String evalStringQuiet(String strScript) {
    if (checkResume(strScript))
      return "script processing resumed";
    if (checkHalt(strScript))
      return "script execution halted";
    return scriptManager.addScript(strScript, false, true);
  }

  /// direct no-queue use:

  public String scriptWait(String strScript) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    String str = (String) evalStringWaitStatus("JSON", strScript,
        "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated", false,
        false);
    GT.setDoTranslate(doTranslateTemp);
    return str;
  }

  public Object scriptWaitStatus(String strScript, String statusList) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    Object ret = evalStringWaitStatus("object", strScript, statusList, false,
        false);
    GT.setDoTranslate(doTranslateTemp);
    return ret;
  }

  public Object evalStringWaitStatus(String returnType, String strScript,
                                     String statusList) {
    scriptManager.waitForQueue();
    return evalStringWaitStatus(returnType, strScript, statusList, false, false);
  }

  int scriptIndex;

  synchronized Object evalStringWaitStatus(String returnType, String strScript,
                                           String statusList,
                                           boolean isScriptFile, boolean isQuiet) {
    // from the scriptManager only!
    if (checkResume(strScript))
      return "script processing resumed"; //be very odd if this fired
    if (checkHalt(strScript))
      return "script execution halted";
    if (strScript == null)
      return null;

    //typically request: "+scriptStarted,+scriptStatus,+scriptEcho,+scriptTerminated"
    //set up first with applet.jmolGetProperty("jmolStatus",statusList)

    //flush list
    String oldStatusList = statusManager.statusList;
    getProperty("String", "jmolStatus", statusList);
    if (checkScriptOnly)
      Logger.info("--checking script:\n" + eval.script + "\n----\n");
    boolean isOK = (isScriptFile ? eval.loadScriptFile(strScript, isQuiet)
        : eval.loadScriptString(strScript, isQuiet));
    String strErrorMessage = eval.getErrorMessage();
    if (isOK) {
      statusManager.setStatusScriptStarted(++scriptIndex, strScript);
      eval.runEval(checkScriptOnly, !checkScriptOnly || fileOpenCheck);
      int msWalltime = eval.getExecutionWalltime();
      strErrorMessage = eval.getErrorMessage();
      statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
      if (isScriptFile && writeInfo != null)
        createImage(writeInfo);
    } else {
      scriptStatus(strErrorMessage);
    }
    if (checkScriptOnly)
      Logger.info((strErrorMessage == null ? "--script check ok"
          : "--script check error\n" + strErrorMessage)
          + "\n(use 'exit' to stop checking)\n");
    if (isScriptFile && autoExit) {
      System.out.flush();
      System.exit(0);
    }
    if (returnType.equalsIgnoreCase("String"))
      return eval.getErrorMessage();
    // get  Vector of Vectors of Vectors info
    Object info = getProperty(returnType, "jmolStatus", statusList);
    // reset to previous status list
    getProperty("object", "jmolStatus", oldStatusList);
    return info;
  }

  boolean checking;

  public String scriptCheck(String strScript) {
    // from ConsoleTextPane.checkCommand() and applet Jmol.scriptProcessor()
    if (strScript == null || checking)
      return null;
    checking = true;
    Object obj = eval.checkScriptSilent(strScript);
    checking = false;
    if (obj instanceof String)
      return (String) obj;
    return null;
  }

  public boolean isScriptExecuting() {
    return eval.isScriptExecuting();
  }

  public void haltScriptExecution() {
    eval.haltExecution();
  }

  public void resumeScriptExecution() {
    eval.resumePausedExecution();
  }

  public void pauseScriptExecution() {
    refresh(0, "pauseScriptExecution");
    eval.pauseExecution();
  }

  private void setDefaultLoadScript(String script) {
    //Eval
    global.defaultLoadScript = script;
  }

  String getDefaultLoadScript() {
    return global.defaultLoadScript;
  }

  private void setLoadFormat(String format) {
    //Eval
    global.loadFormat = format;
  }

  String getLoadFormat() {
    return global.loadFormat;
  }

  String getStandardLabelFormat() {
    return stateManager.getStandardLabelFormat();
  }

  int getRibbonAspectRatio() {
    return global.ribbonAspectRatio;
  }

  private void setRibbonAspectRatio(int ratio) {
    //Eval
    global.ribbonAspectRatio = ratio;
  }

  float getSheetSmoothing() {
    return global.sheetSmoothing;
  }

  private void setSheetSmoothing(float factor0To1) {
    //Eval
    global.sheetSmoothing = factor0To1;
  }

  boolean getSsbondsBackbone() {
    return global.ssbondsBackbone;
  }

  private void setHbondsBackbone(boolean TF) {
    //Eval
    global.hbondsBackbone = TF;
  }

  boolean getHbondsBackbone() {
    return global.hbondsBackbone;
  }

  private void setHbondsSolid(boolean TF) {
    //Eval
    global.hbondsSolid = TF;
  }

  boolean getHbondsSolid() {
    return global.hbondsSolid;
  }

  public void setMarBond(short marBond) {
    global.bondRadiusMilliAngstroms = marBond;
    global.setParameterValue("bondRadiusMilliAngstroms", marBond);
    setShapeSize(JmolConstants.SHAPE_STICKS, marBond * 2);
  }

  int hoverAtomIndex = -1;
  String hoverText;

  void hoverOn(int atomIndex) {
    if (eval != null && isScriptExecuting() || atomIndex == hoverAtomIndex
        || global.hoverDelayMs == 0)
      return;
    loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "text", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "target",
        new Integer(atomIndex));
    hoverText = null;
    hoverAtomIndex = atomIndex;
  }

  int getHoverDelay() {
    return global.hoverDelayMs;
  }

  private void setHoverDelay(int milliSec) {
    global.hoverDelayMs = milliSec;
  }

  void hoverOn(int x, int y, String text) {
    if (eval != null && isScriptExecuting())
      return;
    loadShape(JmolConstants.SHAPE_HOVER);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "xy", new Point3i(x, y, 0));
    setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
    setShapeProperty(JmolConstants.SHAPE_HOVER, "text", text);
    hoverAtomIndex = -1;
    hoverText = text;
  }

  void hoverOff() {
    if (hoverAtomIndex >= 0) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "target", null);
      hoverAtomIndex = -1;
    }
    if (hoverText != null) {
      setShapeProperty(JmolConstants.SHAPE_HOVER, "text", null);
      hoverText = null;
    }
  }

  void setAtomHoverLabel(String text) {
    setShapeProperty(JmolConstants.SHAPE_HOVER, "atomLabel", text);
  }

  void setLabel(String strLabel) {
    //Eval
    if (strLabel != null) // force the class to load and display
      setShapeSize(JmolConstants.SHAPE_LABELS, 0);
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
  }

  void togglePickingLabel(BitSet bs) {
    //eval set toggleLabel (atomset)
    setShapeSize(JmolConstants.SHAPE_LABELS, 0);
    modelManager.setShapeProperty(JmolConstants.SHAPE_LABELS, "toggleLabel",
        null, bs);
    refresh(0, "Viewer:");
  }

  BitSet getBitSetSelection() {
    return selectionManager.bsSelection;
  }

  void loadShape(int shapeID) {
    modelManager.loadShape(shapeID);
  }

  void setShapeSize(int shapeID, int size) {
    //Eval - many
    //stateManager.setCrystallographicDefaults
    //Viewer - many
    setShapeSize(shapeID, size, selectionManager.bsSelection);
  }

  void setShapeSize(int shapeID, int size, BitSet bsAtoms) {
    //above,
    //Eval.configuration
    modelManager.setShapeSize(shapeID, size, bsAtoms);
    refresh(0, "Viewer:setShapeSize(" + shapeID + "," + size + ")");
  }

  int getShapeSize(int shapeID) {
    return modelManager.getShapeSize(shapeID);
  }

  void setShapeProperty(int shapeID, String propertyName, Object value) {
    //Eval
    //many local

    /*
     * Logger.debug("JmolViewer.setShapeProperty("+
     * JmolConstants.shapeClassBases[shapeID]+ "," + propertyName + "," + value +
     * ")");
     */
    if (shapeID < 0)
      return; //not applicable
    modelManager.setShapeProperty(shapeID, propertyName, value,
        selectionManager.bsSelection);
    refresh(0, "Viewer:setShapeProperty()");
  }

  void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bs) {
    //Eval color
    if (shapeID < 0)
      return; //not applicable
    modelManager.setShapeProperty(shapeID, propertyName, value, bs);
    refresh(0, "Viewer:setShapeProperty()");
  }

  void setShapePropertyArgb(int shapeID, String propertyName, int argb) {
    //Eval
    setShapeProperty(shapeID, propertyName, argb == 0 ? null : new Integer(
        argb | 0xFF000000));
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

  short getColix(Object object) {
    return g3d.getColix(object);
  }

  private void setRasmolHydrogenSetting(boolean b) {
    //Eval
    global.rasmolHydrogenSetting = b;
  }

  boolean getRasmolHydrogenSetting() {
    return global.rasmolHydrogenSetting;
  }

  private void setRasmolHeteroSetting(boolean b) {
    //Eval
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
    global.setParameterValue("debugScript", debugScript);
    //Logger.setActiveLevel(Logger.LEVEL_DEBUG, debugScript);
  }

  void atomPicked(int atomIndex, int modifiers) {
    if (!isInSelectionSubset(atomIndex))
      return;
    pickingManager.atomPicked(atomIndex, modifiers);
  }

  void clearClickCount() {
    //MouseManager.clearclickCount()
    mouseManager.clearClickCount();
    setTainted(true);
  }

  private void setPickingMode(String mode) {
    int pickingMode = JmolConstants.GetPickingMode(mode);
    if (pickingMode < 0)
      pickingMode = JmolConstants.PICKING_IDENT;
    pickingManager.setPickingMode(pickingMode);
  }

  int getPickingMode() {
    return pickingManager.getPickingMode();
  }

  private void setPickingStyle(String style) {
    int pickingStyle = JmolConstants.GetPickingMode(style);
    if (pickingStyle < 0)
      pickingStyle = JmolConstants.PICKINGSTYLE_SELECT_JMOL;
    pickingManager.setPickingStyle(pickingStyle);
  }

  void setDrawHover(boolean TF) {
    pickingManager.setDrawHover(TF);
  }

  boolean getDrawHover() {
    return pickingManager.getDrawHover();
  }

  public String getAtomInfo(int atomIndex) {
    return modelManager.getAtomInfo(atomIndex);
  }

  public String getAtomInfoXYZ(int atomIndex) {
    return modelManager.getAtomInfoXYZ(atomIndex, getTestFlag1());
  }

  // //////////////status manager dispatch//////////////

  public Hashtable getMessageQueue() {
    return statusManager.messageQueue;
  }

  Viewer getViewer() {
    return this;
  }

  private void setCallbackFunction(String callbackType, String callbackFunction) {
    //Eval
    if (callbackFunction.equalsIgnoreCase("none"))
      callbackFunction = null;
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

  void setStatusScriptStarted(int iscript, String script) {
    statusManager.setStatusScriptStarted(iscript, script);
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
    setFrankOn(true);
    statusManager.popupMenu(x, y);
  }

  public void setJmolStatusListener(JmolStatusListener jmolStatusListener) {
    statusManager.setJmolStatusListener(jmolStatusListener);
  }

  void setStatusFrameChanged(int frameNo) {
    transformManager.setVibrationPeriod(Float.NaN);
    int modelIndex = repaintManager.currentModelIndex;
    int fileNo = getModelFileNumber(modelIndex);
    int modelNo = fileNo % 1000000;
    int firstNo = getModelFileNumber(repaintManager.firstModelIndex);
    int lastNo = getModelFileNumber(repaintManager.lastModelIndex);
    String s;
    if (fileNo == 0) {
      s = getModelNumberDotted(repaintManager.firstModelIndex) + " - "
          + getModelNumberDotted(repaintManager.lastModelIndex);
      if (firstNo/1000000 == lastNo/1000000)
        fileNo = firstNo;
    } else {
      s = getModelNumberDotted(modelIndex);
    }
    if (fileNo != 0)
      fileNo = (fileNo < 1000000 ? 1 : fileNo / 1000000);

    global.setParameterValue("_currentFileNumber", fileNo);
    global.setParameterValue("_currentModelNumberInFile", modelNo);
    global.setParameterValue("_modelNumber", s);
    global.setParameterValue("_modelName", (modelIndex < 0 ? ""
        : getModelName(modelIndex)));
    statusManager.setStatusFrameChanged(frameNo, fileNo, modelNo, firstNo, lastNo);
  }

  private void setStatusFileLoaded(int ptLoad, String fullPathName, String fileName,
                           String modelName, Object clientFile, String strError) {
    statusManager.setStatusFileLoaded(fullPathName, fileName, modelName,
        clientFile, strError, ptLoad);
  }

  private void setStatusFileNotLoaded(String fullPathName, String errorMsg) {
    setStatusFileLoaded(-1, fullPathName, null, null, null, errorMsg);
  }

  public void scriptEcho(String strEcho) {
    statusManager.setScriptEcho(strEcho);
  }

  void scriptStatus(String strStatus) {
    statusManager.setScriptStatus(strStatus);
  }

  private void setScriptDelay(int nSec) {
    global.scriptDelay = nSec;
  }

  int getScriptDelay() {
    return global.scriptDelay;
  }

  public void showUrl(String urlString) {
    //applet.Jmol
    //app Jmol
    //StatusManager
    if (urlString == null)
      return;
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
    //Eval
    statusManager.showConsole(showConsole);
  }

  void clearConsole() {
    //Eval
    statusManager.clearConsole();
  }

  Object getParameterEscaped(String key) {
    return global.getParameterEscaped(key);
  }

  public Object getParameter(String key) {
    return global.getParameter(key);
  }

  void unsetProperty(String name) {
    global.setParameterValue(name, Float.NaN);
  }

  public boolean getBooleanProperty(String key) {
    return getBooleanProperty(key, true);
  }

  /*****************************************************************************
   * @param key
   * @param doICare  true if you want an error message if it doesn't exist 
   * @return the boolean property mth 2005 06 24 and/or these property names
   *         should be interned strings so that we can just do == comparisions
   *         between strings
   ****************************************************************************/
  public boolean getBooleanProperty(String key, boolean doICare) {
    //JmolPopup
    if (key.equalsIgnoreCase("hideNotSelected"))
      return selectionManager.getHideNotSelected();
    if (key.equalsIgnoreCase("colorRasmol"))
      return colorManager.getDefaultColorRasmol();
    if (key.equalsIgnoreCase("perspectiveDepth"))
      return getPerspectiveDepth();
    if (key.equalsIgnoreCase("showAxes"))
      return getShowAxes();
    if (key.equalsIgnoreCase("showBoundBox"))
      return getShowBbcage();
    if (key.equalsIgnoreCase("showUnitcell"))
      return getShowUnitCell();
    if (key.equalsIgnoreCase("debugScript"))
      return getDebugScript();
    if (key.equalsIgnoreCase("showHydrogens"))
      return getShowHydrogens();
    if (key.equalsIgnoreCase("frank"))
      return getShowFrank();
    if (key.equalsIgnoreCase("showMultipleBonds"))
      return getShowMultipleBonds();
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

    key = key.toLowerCase();
    if (global.htPropertyFlags.containsKey(key)) {
      return ((Boolean) global.htPropertyFlags.get(key)).booleanValue();
    }
    if (doICare)
      Logger.error("viewer.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  public void setStringProperty(String key, String value) {
    //Eval
    while (true) {
      ///11.1.22//
      
      if (key.equalsIgnoreCase("loadFormat")) {
        setLoadFormat(value);
        return;
      }
            
      ///11.1///
      
      if (key.equalsIgnoreCase("backgroundColor")) {
        setObjectColor("background", value);
        return;
      }
      
      if (key.equalsIgnoreCase("axesColor")) {
        setObjectColor("axis1", value);
        setObjectColor("axis2", value);
        setObjectColor("axis3", value);
        return;
      }

      if (key.equalsIgnoreCase("axis1Color")) {
        setObjectColor("axis1", value);
        return;
      }

      if (key.equalsIgnoreCase("axis2Color")) {
        setObjectColor("axis2", value);
        return;
      }

      if (key.equalsIgnoreCase("axis3Color")) {
        setObjectColor("axis3", value);
        return;
      }

      if (key.equalsIgnoreCase("boundBoxColor")) {
        setObjectColor("boundbox", value);
        return;
      }

      if (key.equalsIgnoreCase("unitCellColor")) {
        setObjectColor("unitcell", value);
        return;
      }

      if (key.equalsIgnoreCase("propertyColorScheme")) {
        setPropertyColorScheme(value);
        break;
      }

      if (key.equalsIgnoreCase("hoverLabel")) {
        setAtomHoverLabel(value);
        break;
      }
      ///11.0///
      if (key.equalsIgnoreCase("defaultDistanceLabel")) {
        setDefaultMeasurementLabel(2, value);
        break;
      }
      if (key.equalsIgnoreCase("defaultAngleLabel")) {
        setDefaultMeasurementLabel(3, value);
        break;
      }
      if (key.equalsIgnoreCase("defaultTorsionLabel")) {
        setDefaultMeasurementLabel(4, value);
        break;
      }
      if (key.equalsIgnoreCase("defaultLoadScript")) {
        setDefaultLoadScript(value);
        break;
      }
      if (key.equalsIgnoreCase("appletProxy")) {
        setAppletProxy(value);
        break;
      }
      if (key.equalsIgnoreCase("defaultDirectory")) {
        setDefaultDirectory(value);
        break;
      }
      if (key.equalsIgnoreCase("helpPath")) {
        setHelpPath(value);
        break;
      }
      if (key.equalsIgnoreCase("defaults")) {
        setDefaults(value);
        break;
      }
      if (key.equalsIgnoreCase("defaultColorScheme")) {
        setDefaultColors(value);
        break;
      }
      if (key.equalsIgnoreCase("picking")) {
        setPickingMode(value);
        break;
      }
      if (key.equalsIgnoreCase("pickingStyle")) {
        setPickingStyle(value);
        break;
      }
      if (key.equalsIgnoreCase("dataSeparator")) {
        //just saving this
        break;
      }
      if (key.toLowerCase().indexOf("callback") >= 0) {
        setCallbackFunction(key, value);
        break;
      }
      //not found -- @ is a silent mode indicator
      if (key.charAt(0) != '@') {
        key = key.toLowerCase();
        if (!global.htParameterValues.containsKey(key)) {
          if (global.htPropertyFlags.containsKey(key)) {
            scriptStatus("ERROR: cannot set boolean flag to string value");
            return;
          }
          Logger.warn(key +" -- string variable defined (" + value.length() + " bytes)");
          break;
        }
      }
      break;
    }
    global.setParameterValue(key, value);
  }

  public void setFloatProperty(String key, float value) {
    setFloatProperty(key, value, false);
  }

  private boolean setFloatProperty(String key, float value, boolean isInt) {
    //Eval
    while (true) {
      
      ///11.1///
      if (key.equalsIgnoreCase("defaultTranslucent")) {
        setDefaultTranslucent(value);
        break;
      }


      if (key.equalsIgnoreCase("axesScale")) {
        setAxesScale(value);
        break;
      }
      if (key.equalsIgnoreCase("visualRange")) {
        setVisualRange(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationDepth")) {
        setNavigationDepthPercent(0, value);
        break;
      }
      if (key.equalsIgnoreCase("navigationSpeed")) {
        setNavigationSpeed(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationSlab")) {
        setNavigationSlabOffset(value);
        break;
      }
      if (key.equalsIgnoreCase("cameraDepth")) {
        setCameraDepth(value);
        break;
      }
      if (key.equalsIgnoreCase("rotationRadius")) {
        setRotationRadius(value);
        break;
      }
      if (key.equalsIgnoreCase("hoverDelay")) {
        setHoverDelay((int) (value * 1000));
        break;
      }
      ///11.0///
      if (key.equalsIgnoreCase("sheetSmoothing")) {
        setSheetSmoothing(value);
        break;
      }
      if (key.equalsIgnoreCase("dipoleScale")) {
        setDipoleScale(value);
        break;
      }
      if (key.equalsIgnoreCase("stereoDegrees")) {
        setStereoDegrees(value);
        break;
      }
      if (key.equalsIgnoreCase("vectorScale")) {
        //public -- no need to set
        setVectorScale(value);
        return true;
      }
      if (key.equalsIgnoreCase("vibrationPeriod")) {
        //public -- no need to set
        setVibrationPeriod(value);
        return true;
      }
      if (key.equalsIgnoreCase("vibrationScale")) {
        //public -- no need to set
        setVibrationScale(value);
        return true;
      }
      if (key.equalsIgnoreCase("bondTolerance")) {
        setBondTolerance(value);
        break;
      }
      if (key.equalsIgnoreCase("minBondDistance")) {
        setMinBondDistance(value);
        break;
      }
      if (key.equalsIgnoreCase("scaleAngstromsPerInch")) {
        setScaleAngstromsPerInch(value);
        break;
      }
      if (key.equalsIgnoreCase("solventProbeRadius")) {
        setSolventProbeRadius(value);
        break;
      }
      if (key.equalsIgnoreCase("radius")) { //deprecated
        setFloatProperty("solventProbeRadius", value);
        return true;
      }
      // not found
      if (isInt)
        return false;
      key = key.toLowerCase();
      if (!global.htParameterValues.containsKey(key)) {
        if (global.htPropertyFlags.containsKey(key)) {
          Logger.error("cannot set boolean flag to numeric value");
          scriptEcho("script WARNING: cannot set boolean flag to numeric value");
          return true;
        }
        Logger.warn("viewer.setFloatProperty(" + key + "," + value
            + ") - float variable defined");
        break;
      }
      break;
    }
    global.setParameterValue(key, value);
    return true;
  }

  public void setIntProperty(String key, int value) {
    //Eval
    setIntProperty(key, value, true);
  }

  private void setIntProperty(String key, int value, boolean defineNew) {
    while (true) {
      ///11.1///


      if (key.equalsIgnoreCase("strandCount")) {
        setShapeProperty(JmolConstants.SHAPE_STRANDS, "strandCount",
            new Integer(value >= 0 && value <= 20 ? value : 5));
        break;
      }
      if (key.equalsIgnoreCase("perspectiveModel")) {
        setPerspectiveModel(value);
        break;
      }
      if (key.equalsIgnoreCase("showScript")) {
        setScriptDelay(value);
        break;
      }
      if (key.equalsIgnoreCase("specularPower")) {
        setSpecularPower(value);
        break;
      }
      if (key.equalsIgnoreCase("specularExponent")) {
        setSpecularExponent(value);
        break;
      }
      if (key.equalsIgnoreCase("specular")) {
        setIntProperty("specularPercent", value);
        return;
      }
      if (key.equalsIgnoreCase("diffuse")) {
        setIntProperty("diffusePercent", value);
        return;
      }
      if (key.equalsIgnoreCase("ambient")) {
        setIntProperty("ambientPercent", value);
        return;
      }
      if (key.equalsIgnoreCase("specularPercent")) {
        setSpecularPercent(value);
        break;
      }
      if (key.equalsIgnoreCase("diffusePercent")) {
        setDiffusePercent(value);
        break;
      }
      if (key.equalsIgnoreCase("ambientPercent")) {
        setAmbientPercent(value);
        break;
      }

      if (key.equalsIgnoreCase("ribbonAspectRatio")) {
        setRibbonAspectRatio(value);
        break;
      }
      if (key.equalsIgnoreCase("pickingSpinRate")) {
        setPickingSpinRate(value);
        break;
      }
      if (key.equalsIgnoreCase("animationFps")) {
        setAnimationFps(value);
        break;
      }
      if (key.equalsIgnoreCase("percentVdwAtom")) {
        setPercentVdwAtom(value);
        break;
      }
      if (key.equalsIgnoreCase("bondRadiusMilliAngstroms")) {
        setMarBond((short) value);
        //public method -- no need to set
        return;
      }
      if (key.equalsIgnoreCase("hermiteLevel")) {
        setHermiteLevel(value);
        break;
      }
      //not found
      if ((value != 0 && value != 1)
          || !setBooleanProperty(key, value == 1, false)) {
        if (setFloatProperty(key, value, true))
          return;
        key = key.toLowerCase();
        if (!global.htParameterValues.containsKey(key)) {
          if (global.htPropertyFlags.containsKey(key)) {
            Logger.error("cannot set boolean flag to numeric value");
            scriptEcho("script WARNING: cannot set boolean flag to numeric value");
            return;
          }
          Logger.info("viewer.setIntProperty(" + key + "," + value
              + ") - integer variable defined");
          break;
        }
        break;
      }
      break;
    }
    if (defineNew) {
      if (global.htPropertyFlags.containsKey(key)) {
        scriptStatus(GT
            ._(
                "ERROR: Cannot set value of a boolean to another type. use \"{0}\" first.",
                key + " = NONE"));
        return; // don't allow setting boolean of a numeric
      }
      global.setParameterValue(key, value);
    }
  }

  public void setBooleanProperty(String key, boolean value) {
    setBooleanProperty(key, value, true);
  }

  boolean setBooleanProperty(String key, boolean value, boolean defineNew) {
    boolean notFound = false;
    while (true) {
   
      //11.1.22
      
      if (key.equalsIgnoreCase("appendNew")) {
        setAppendNew(value);
        break;
      }
      
            
      if (key.equalsIgnoreCase("autoFPS")) {
        setAutoFps(value);
        break;
      }
      
      //11.1.21
      
      if (key.equalsIgnoreCase("useNumberLocalization")) {
        setUseNumberLocalization(value);
        break;
      }

      
      //11.1.20
      
      if (key.equalsIgnoreCase("showFrank")) {
        setFrankOn(value);
        break;
      }

      /////
      
      if (key.equalsIgnoreCase("solventProbe")) {
        setSolventOn(value);
        break;
      }

      if (key.equalsIgnoreCase("dynamicMeasurements")) {
        setDynamicMeasurements(value);
        break;
      }

      //11.1.14
      
      if (key.equalsIgnoreCase("allowRotateSelected")) {
        setAllowRotateSelected(value);
        break;
      }

      ///11.1.13///

      if (key.equalsIgnoreCase("showScript")) {
        setIntProperty("showScript", value ? 1 : 0);
        return true;
      }
      ///11.1///
      if (key.equalsIgnoreCase("allowEmbeddedScripts")) {
        setAllowEmbeddedScripts(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationPeriodic")) {
        setNavigationPeriodic(value);
        break;
      }
      if (key.equalsIgnoreCase("zShade")) {
        setZShade(value);
        break;
      }
      if (key.equalsIgnoreCase("drawHover")) {
        setDrawHover(value);
        break;
      }
      if (key.equalsIgnoreCase("navigationMode")) {
        setNavigationMode(value);
        break;
      }
      if (key.equalsIgnoreCase("hideNavigationPoint")) {
        setHideNavigationPoint(value);
        break;
      }
      if (key.equalsIgnoreCase("showNavigationPointAlways")) {
        setShowNavigationPointAlways(value);
        break;
      }
      ///11.0///
      if (key.equalsIgnoreCase("refreshing")) {
        setRefreshing(value);
        break;
      }
      if (key.equalsIgnoreCase("justifyMeasurements")) {
        setJustifyMeasurements(value);
        break;
      }
      if (key.equalsIgnoreCase("ssBondsBackbone")) {
        setSsbondsBackbone(value);
        break;
      }
      if (key.equalsIgnoreCase("hbondsBackbone")) {
        setHbondsBackbone(value);
        break;
      }
      if (key.equalsIgnoreCase("hbondsSolid")) {
        setHbondsSolid(value);
        break;
      }
      if (key.equalsIgnoreCase("specular")) {
        setSpecular(value);
        break;
      }
      if (key.equalsIgnoreCase("slabEnabled")) {
        setSlabEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("zoomEnabled")) {
        setZoomEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("solventProbe")) {
        setSolventOn(value);
        break;
      }
      if (key.equalsIgnoreCase("highResolution")) {
        setHighResolution(value);
        break;
      }
      if (key.equalsIgnoreCase("traceAlpha")) {
        setTraceAlpha(value);
        break;
      }
      if (key.equalsIgnoreCase("zoomLarge")) {
        setZoomLarge(value);
        break;
      }
      if (key.equalsIgnoreCase("languageTranslation")) {
        GT.setDoTranslate(value);
        break;
      }
      if (key.equalsIgnoreCase("hideNotSelected")) {
        setHideNotSelected(value);
        break;
      }
      if (key.equalsIgnoreCase("colorRasmol")) {
        setDefaultColors(value ? "rasmol" : "jmol");
        break;
      }
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
        setDotsSelectedOnly(value);
        break;
      }
      if (key.equalsIgnoreCase("showAxes")) {
        setShowAxes(value);
        return true;
      }
      if (key.equalsIgnoreCase("showBoundBox")) {
        setShowBbcage(value);
        return true;
      }
      if (key.equalsIgnoreCase("showUnitcell")) {
        setShowUnitCell(value);
        return true;
      }
      if (key.equalsIgnoreCase("selectionHalos")) {
        setSelectionHalos(value); //volatile
        break;
      }
      if (key.equalsIgnoreCase("debugScript")) {
        setDebugScript(value);
        break;
      }
      if (key.equalsIgnoreCase("showHydrogens")) {
        setShowHydrogens(value);
        break;
      }
      if (key.equalsIgnoreCase("selectHydrogen")) {
        setRasmolHydrogenSetting(value);
        break;
      }
      if (key.equalsIgnoreCase("selectHetero")) {
        setRasmolHeteroSetting(value);
        break;
      }
      if (key.equalsIgnoreCase("showMultipleBonds")) {
        setShowMultipleBonds(value);
        break;
      }
      if (key.equalsIgnoreCase("showHiddenSelectionHalos")) {
        setShowHiddenSelectionHalos(value);
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
      if (key.equalsIgnoreCase("windowCentered")) {
        setWindowCentered(value);
        break;
      }
      if (key.equalsIgnoreCase("displayCellParameters")) {
        setDisplayCellParameters(value);
        break;
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
      if (key.equalsIgnoreCase("ribbonBorder")) {
        setRibbonBorder(value);
        break;
      }
      if (key.equalsIgnoreCase("cartoonRockets")) {
        setCartoonRocketFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("greyscaleRendering")) {
        setGreyscaleRendering(value);
        break;
      }
      if (setAxesMode(key, value))
          break;
   
      //these next are deprecated because they don't 
      //give much indication what they really do:
      if (key.equalsIgnoreCase("frank"))
        return setBooleanProperty("showFrank", value, true);
      if (key.equalsIgnoreCase("solvent"))
        return setBooleanProperty("solventProbe", value, true);
      if (key.equalsIgnoreCase("bonds")) 
        return setBooleanProperty("showMultipleBonds", value, true);
      if (key.equalsIgnoreCase("hydrogen"))  //deprecated
        return setBooleanProperty("selectHydrogen", value, true);
      if (key.equalsIgnoreCase("hetero"))  //deprecated
        return setBooleanProperty("selectHetero", value, true);
      if (key.equalsIgnoreCase("showSelections"))  //deprecated -- see "selectionHalos"
        return setBooleanProperty("selectionHalos", value, true);
      // these next return, because there is no need to repaint
      while (true) {
        if (key.equalsIgnoreCase("bondModeOr")) {
          setBondSelectionModeOr(value);
          break;
        }
        if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
          setZeroBasedXyzRasmol(value);
          break;
        }
        if (key.equalsIgnoreCase("rangeSelected")) {
          setRangeSelected(value);
          break;
        }
        if (key.equalsIgnoreCase("measureAllModels")) {
          setMeasureAllModels(value);
          break;
        }
        if (key.equalsIgnoreCase("statusReporting")) {
          setAllowStatusReporting(value);
          break;
        }
        if (key.equalsIgnoreCase("chainCaseSensitive")) {
          setChainCaseSensitive(value);
          break;
        }
        if (key.equalsIgnoreCase("hideNameInPopup")) {
          setHideNameInPopup(value);
          break;
        }
        if (key.equalsIgnoreCase("autobond")) {
          setAutoBond(value);
          break;
        }
        if (key.equalsIgnoreCase("disablePopupMenu")) {
          setDisablePopupMenu(value);
          break;
        }
        if (key.equalsIgnoreCase("forceAutoBond")) {
          setForceAutoBond(value);
          break;
        }
        notFound = true;
        break;
      }
      if (!defineNew)
        return !notFound;
      if (!notFound) {
        global.setParameterValue(key, value);
        return true;
      }
      notFound = true;
      break;
    }
    if (!defineNew)
      return !notFound;
    if (notFound) {
      key = key.toLowerCase();
      if (global.htParameterValues.containsKey(key)) {
        scriptStatus(GT._("ERROR: Cannot set value of this variable to a boolean. use \"{0}\" first.", key + " = NONE"));
        return true; // don't allow setting boolean of a numeric
      }
      if (!value && !global.htPropertyFlags.containsKey(key)) {
        Logger.warn("viewer.setBooleanProperty(" + key + "," + value
            + ") - boolean variable defined");
      }
    }
    global.setParameterValue(key, value);
    if (notFound)
      return false;
    setTainted(true);
    refresh(0, "viewer.setBooleanProperty");
    return true;
  }

  void showParameter(String key, boolean ifNotSet, int nMax) {
    String sv = "" + global.getParameterEscaped(key, nMax);
    if (ifNotSet || sv.indexOf("<not set>") < 0)
    showString(key + " = " + sv); 
  }
  
  void showString(String str) {
    Logger.warn(str);
    scriptEcho(str);
  }

  String getAllSettings(int nMax) {
    return global.getAllSettings(nMax);
  }

  ////////  flags and settings ////////

  boolean getDotSurfaceFlag() {
    return global.dotSurface;
  }

  private void setDotSurfaceFlag(boolean TF) {
    global.dotSurface = TF;
  }

  boolean getDotsSelectedOnlyFlag() {
    return global.dotsSelectedOnly;
  }

  private void setDotsSelectedOnly(boolean TF) {
    global.dotsSelectedOnly = TF;
  }

  boolean isRangeSelected() {
    return global.rangeSelected;
  }

  private void setRangeSelected(boolean TF) {
    global.rangeSelected = TF;
  }

  boolean isWindowCentered() {
    return transformManager.isWindowCentered();
  }

  private void setWindowCentered(boolean TF) {
    //setBooleanProperty
    transformManager.setWindowCentered(TF);
  }

  private void setCameraDepth(float percent) {
    transformManager.setCameraDepthPercent(percent);
    refresh(1, "set cameraDepth");
  }

  void setNavigationDepthPercent(float timeSec, float percent) {
    transformManager.setNavigationDepthPercent(timeSec, percent);
    refresh(1, "set navigationDepth");
  }

  private void setNavigationSpeed(float value) {
    global.navigationSpeed = value;
  }

  float getNavigationSpeed() {
    return global.navigationSpeed;
  }

  private void setShowNavigationPointAlways(boolean TF) {
    global.showNavigationPointAlways = TF;
  }

  private void setHideNavigationPoint(boolean TF) {
    global.hideNavigationPoint = TF;
  }

  boolean getShowNavigationPoint() {
    if (!global.navigationMode || !transformManager.canNavigate())
      return false;
    return (getNavigating() && !global.hideNavigationPoint
        || global.showNavigationPointAlways || getInMotion());
  }

  public void setVisualRange(float angstroms) {
    transformManager.setVisualRange(angstroms);
    refresh(1, "set visualRange");
  }

  private void setSolventProbeRadius(float radius) {
    //Eval
    global.solventProbeRadius = radius;
  }

  float getSolventProbeRadius() {
    return global.solventProbeRadius;
  }

  float getCurrentSolventProbeRadius() {
    return global.solventOn ? global.solventProbeRadius : 0;
  }

  private void setSolventOn(boolean isOn) {
    //Eval
    global.solventOn = isOn;
  }

  boolean getSolventOn() {
    return global.solventOn;
  }

  private void setAllowStatusReporting(boolean TF) {
    statusManager.setAllowStatusReporting(TF);
  }

  private void setTestFlag1(boolean value) {
    global.testFlag1 = value;
  }

  boolean getTestFlag1() {
    return global.testFlag1;
  }

  boolean getTestFlag2() {
    return global.testFlag2;
  }

  private void setTestFlag2(boolean value) {
    global.testFlag2 = value;
  }

  boolean getTestFlag3() {
    return global.testFlag3;
  }

  private void setTestFlag3(boolean value) {
    global.testFlag3 = value;
  }

  boolean getTestFlag4() {
    return global.testFlag4;
  }

  private void setTestFlag4(boolean value) {
    global.testFlag4 = value;
  }

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    //setBooleanProperty                      
    //stateManager.setCrystallographicDefaults
    //app preferences dialog   
    global.setParameterValue("perspectiveDepth", perspectiveDepth);
    transformManager.setPerspectiveDepth(perspectiveDepth);
    refresh(0, "Viewer:setPerspectiveDepth()");
  }

  public void setAxesOrientationRasmol(boolean axesOrientationRasmol) {
    //app PreferencesDialog
    //stateManager
    //setBooleanproperty
    transformManager.setAxesOrientationRasmol(axesOrientationRasmol);
    refresh(0, "Viewer:setAxesOrientationRasmol()");
  }

  public boolean getAxesOrientationRasmol() {
    return transformManager.axesOrientationRasmol;
  }
  
  void setAxesScale(float scale) {
    global.axesScale = scale;
    axesAreTainted = true;
    refresh(0, "set axesScale");
  }
  
  float getAxesScale() {
    return global.axesScale;
  }

  boolean setAxesMode(String key, boolean value) {
    if (key.equalsIgnoreCase("axesWindow")) {
      setAxesModeMolecular(!value);
      return true;
    } 
    if (key.equalsIgnoreCase("axesMolecular")) {
      setAxesModeMolecular(value);
      return true;
    }
    if (key.equalsIgnoreCase("axesUnitCell")) {
      setAxesModeUnitCell(value);
      return true;
    }
    return false;
  }
  
  private void setAxesModeMolecular(boolean TF) {
     global.axesMode = (TF ? JmolConstants.AXES_MODE_MOLECULAR
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
    global.removeParameter("axesunitcell");
    global.removeParameter(TF ? "axeswindow" : "axesmolecular");
  }

  void setAxesModeUnitCell(boolean TF) {
    //stateManager
    //setBooleanproperty
    global.axesMode = (TF ? JmolConstants.AXES_MODE_UNITCELL
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
    global.removeParameter("axesmolecular");
    global.removeParameter(TF ? "axeswindow" : "axesunitcell");
  }

  int getAxesMode() {
    return global.axesMode;
  }

  private void setDisplayCellParameters(boolean displayCellParameters) {
    global.displayCellParameters = displayCellParameters;
  }

  boolean getDisplayCellParameters() {
    return global.displayCellParameters;
  }

  public boolean getPerspectiveDepth() {
    return transformManager.getPerspectiveDepth();
  }

  public void setSelectionHalos(boolean TF) {
    // display panel can hit this without a frame, apparently
    if (TF == getSelectionHaloEnabled() || getFrame() == null)
      return;
    global.setParameterValue("selectionHalos", TF);
    loadShape(JmolConstants.SHAPE_HALOS);
    //a frame property, so it is automatically reset
    modelManager.setSelectionHaloEnabled(TF);
  }

  boolean getSelectionHaloEnabled() {
    return modelManager.getSelectionHaloEnabled();
  }

  private void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    //Eval
    global.bondModeOr = bondSelectionModeOr;
    refresh(0, "Viewer:setBondSelectionModeOr()");
  }

  boolean getBondSelectionModeOr() {
    return global.bondModeOr;
  }

  boolean getChainCaseSensitive() {
    return global.chainCaseSensitive;
  }

  private void setChainCaseSensitive(boolean chainCaseSensitive) {
    global.chainCaseSensitive = chainCaseSensitive;
  }

  boolean getRibbonBorder() {
    return global.ribbonBorder;
  }

  private void setRibbonBorder(boolean borderOn) {
    global.ribbonBorder = borderOn;
  }

  boolean getCartoonRocketFlag() {
    return global.cartoonRockets;
  }

  private void setCartoonRocketFlag(boolean TF) {
    global.cartoonRockets = TF;
  }

  boolean getHideNameInPopup() {
    return global.hideNameInPopup;
  }

  private void setHideNameInPopup(boolean hideNameInPopup) {
    global.hideNameInPopup = hideNameInPopup;
  }

  private void setNavigationPeriodic(boolean TF) {
    global.navigationPeriodic = TF;
  }

  boolean getNavigationPeriodic() {
    return global.navigationPeriodic;
  }

  private void setNavigationMode(boolean TF) {
    global.navigationMode = TF;
    if (TF && !transformManager.canNavigate()) {
      setVibrationOff();
      transformManager = transformManager.getNavigationManager(this,
          dimScreen.width, dimScreen.height);
      reset();
    }
    transformManager.setNavigationMode(TF);
    //refresh(1,"set navigationMode");
  }

  boolean getNavigationMode() {
    return global.navigationMode;
  }

  private void setNavigationSlabOffset(float percent) {
    transformManager.setNavigationSlabOffsetPercent(percent);
  }

  private void setPerspectiveModel(int mode) {
    setVibrationOff();
    switch (mode) {
    case 10:
      transformManager = new TransformManager10(this, dimScreen.width,
          dimScreen.height);
      break;
    default:
      transformManager = transformManager.getNavigationManager(this, dimScreen.width, dimScreen.height);
    }
    reset();
  }

  private void setZoomLarge(boolean TF) {
    global.zoomLarge = TF;
    transformManager.scaleFitToScreen(false, global.zoomLarge);
  }

  boolean getZoomLarge() {
    return global.zoomLarge;
  }

  private void setTraceAlpha(boolean TF) {
    global.traceAlpha = TF;
  }

  boolean getTraceAlpha() {
    return global.traceAlpha;
  }

  int getHermiteLevel() {
    return global.hermiteLevel;
  }

  private void setHermiteLevel(int level) {
    global.hermiteLevel = level;
  }

  boolean getHighResolution() {
    return global.highResolutionFlag;
  }

  private void setHighResolution(boolean TF) {
    global.highResolutionFlag = TF;
  }

  private void setSsbondsBackbone(boolean TF) {
    //Eval
    global.ssbondsBackbone = TF;
  }

  String getLoadState() {
    return global.getLoadState();
  }

  public void setAutoBond(boolean TF) {
    //setBooleanProperties
    global.setParameterValue("autobond", TF);
    global.autoBond = TF;
  }

  public boolean getAutoBond() {
    return global.autoBond;
  }

  int makeConnections(float minDistance, float maxDistance, short order,
                      int connectOperation, BitSet bsA, BitSet bsB,
                      BitSet bsBonds, boolean isBonds) {
    //eval
    clearAllMeasurements(); // necessary for serialization
    return modelManager.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds);
  }

  // //////////////////////////////////////////////////////////////
  // Graphics3D
  // //////////////////////////////////////////////////////////////

  private void setGreyscaleRendering(boolean greyscaleRendering) {
    global.greyscaleRendering = greyscaleRendering;
    g3d.setGreyscaleMode(greyscaleRendering);
    refresh(0, "Viewer:setGreyscaleRendering()");
  }

  boolean getGreyscaleRendering() {
    return global.greyscaleRendering;
  }

  private void setDisablePopupMenu(boolean disablePopupMenu) {
    global.disablePopupMenu = disablePopupMenu;
  }

  boolean getDisablePopupMenu() {
    return global.disablePopupMenu;
  }

  private void setForceAutoBond(boolean forceAutoBond) {
    global.forceAutoBond = forceAutoBond;
  }

  boolean getForceAutoBond() {
    return global.forceAutoBond;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  public void setPercentVdwAtom(int percentVdwAtom) {
    global.setParameterValue("percentVdwAtom", percentVdwAtom);
    global.percentVdwAtom = percentVdwAtom;
    setShapeSize(JmolConstants.SHAPE_BALLS, -percentVdwAtom);
  }

  public int getPercentVdwAtom() {
    return global.percentVdwAtom;
  }

  short getMadAtom() {
    return (short) -global.percentVdwAtom;
  }

  public short getMadBond() {
    return (short) (global.bondRadiusMilliAngstroms * 2);
  }

  public short getMarBond() {
    return global.bondRadiusMilliAngstroms;
  }

  /*
   void setModeMultipleBond(byte modeMultipleBond) {
   //not implemented
   global.modeMultipleBond = modeMultipleBond;
   refresh(0, "Viewer:setModeMultipleBond()");
   }
   */

  byte getModeMultipleBond() {
    //sticksRenderer
    return global.modeMultipleBond;
  }

  private void setShowMultipleBonds(boolean TF) {
    //Eval.setBonds
    //stateManager
    global.showMultipleBonds = TF;
    refresh(0, "Viewer:setShowMultipleBonds()");
  }

  boolean getShowMultipleBonds() {
    return global.showMultipleBonds;
  }

  public void setShowHydrogens(boolean TF) {
    //PreferencesDialog
    //setBooleanProperty
    global.setParameterValue("showHydrogens", TF);
    global.showHydrogens = TF;
    refresh(0, "Viewer:setShowHydrogens()");
  }

  public boolean getShowHydrogens() {
    return global.showHydrogens;
  }

  private void setShowHiddenSelectionHalos(boolean TF) {
    //setBooleanProperty
    global.showHiddenSelectionHalos = TF;
    refresh(0, "Viewer:setShowHiddenSelectionHalos()");
  }

  public boolean getShowHiddenSelectionHalos() {
    return global.showHiddenSelectionHalos;
  }

  public void setShowBbcage(boolean value) {
    setObjectMad(JmolConstants.SHAPE_BBCAGE, "boundbox", (short)(value ? -4 : 0));
  }

  public boolean getShowBbcage() {
    return getObjectMad(StateManager.OBJ_BOUNDBOX) !=  0;
  }

  public void setShowUnitCell(boolean value) {
    setObjectMad(JmolConstants.SHAPE_UCCAGE, "unitcell", (short)(value ? -4 : 0));
  }

  public boolean getShowUnitCell() {
    return getObjectMad(StateManager.OBJ_UNITCELL) !=  0;
  }

  public void setShowAxes(boolean value) {
    setObjectMad(JmolConstants.SHAPE_AXES, "axes", (short)(value ? -2 : 0));
  }

  public boolean getShowAxes() {
    return getObjectMad(StateManager.OBJ_AXIS1) !=  0;
  }

  public void setFrankOn(boolean TF) {
    setObjectMad(JmolConstants.SHAPE_FRANK, "frank", (short)(TF ? 1 : 0));
  }

  boolean getShowFrank() {
    return getObjectMad(StateManager.OBJ_FRANK) !=  0;
  }

  public void setShowMeasurements(boolean TF) {
    //setbooleanProperty
    global.setParameterValue("showMeasurements", TF);
    global.showMeasurements = TF;
    refresh(0, "setShowMeasurements()");
  }

  public boolean getShowMeasurements() {
    return global.showMeasurements;
  }

  private void setMeasureAllModels(boolean TF) {
    global.measureAllModels = TF;
  }

  boolean getMeasureAllModelsFlag() {
    return global.measureAllModels;
  }

  void setMeasureDistanceUnits(String units) {
    //stateManager
    //Eval
    global.setMeasureDistanceUnits(units);
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "reformatDistances", null);
  }

  String getMeasureDistanceUnits() {
    return global.getMeasureDistanceUnits();
  }
  
  private void setUseNumberLocalization(boolean TF) {
    global.useNumberLocalization = TF;
    TextFormat.setUseNumberLocalization(TF);
  }
  
  boolean getUseNumberLocalization()  {
    return global.useNumberLocalization;
  }
  
  private void setAppendNew(boolean TF) {
    global.appendNew = TF;
  }
  
  boolean getAppendNew()  {
    return global.appendNew;
  }
  
  private void setAutoFps(boolean TF) {
    global.autoFps = TF;
  }
  
  boolean getAutoFps()  {
    return global.autoFps;
  }
  
  public void setRasmolDefaults() {
    setDefaults("RasMol");
  }

  public void setJmolDefaults() {
    setDefaults("Jmol");
  }

  private void setDefaults(String type) {
    if (type.equalsIgnoreCase("RasMol")) {
      stateManager.setRasMolDefaults();
      return;
    }
    stateManager.setJmolDefaults();
  }

  private void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    //stateManager
    //setBooleanProperty
    global.zeroBasedXyzRasmol = zeroBasedXyzRasmol;
    modelManager.setZeroBased();
  }

  boolean getZeroBasedXyzRasmol() {
    return global.zeroBasedXyzRasmol;
  }

  // //////////////////////////////////////////////////////////////
  // temp manager
  // //////////////////////////////////////////////////////////////

  Point3f[] allocTempPoints(int size) {
    //rockets renderer
    return tempManager.allocTempPoints(size);
  }

  void freeTempPoints(Point3f[] tempPoints) {
    tempManager.freeTempPoints(tempPoints);
  }

  Point3i[] allocTempScreens(int size) {
    //mesh and mps
    return tempManager.allocTempScreens(size);
  }

  void freeTempScreens(Point3i[] tempScreens) {
    tempManager.freeTempScreens(tempScreens);
  }

 /*
  boolean[] allocTempBooleans(int size) {
    return tempManager.allocTempBooleans(size);
  }

  void freeTempBooleans(boolean[] tempBooleans) {
    tempManager.freeTempBooleans(tempBooleans);
  }
  */
  
  byte[] allocTempBytes(int size) {
    //mps renderer
    return tempManager.allocTempBytes(size);
  }

  void freeTempBytes(byte[] tempBytes) {
    tempManager.freeTempBytes(tempBytes);
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

  String formatText(String text) {
    return Text.formatText(this, text);
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

  void setStereoMode(int stereoMode, String state) {
    //Eval -- ok; this is set specially
    global.stereoState = state;
    transformManager.setStereoMode(stereoMode);
    setBooleanProperty("greyscaleRendering",
        stereoMode > JmolConstants.STEREO_DOUBLE);
  }

  void setStereoMode(int[] twoColors, String state) {
    //Eval -- also set specially
    global.stereoState = state;
    transformManager.setStereoMode(twoColors);
    setBooleanProperty("greyscaleRendering", true);
  }

  int getStereoMode() {
    return transformManager.stereoMode;
  }

  float getStereoDegrees() {
    return transformManager.stereoDegrees;
  }

  private void setStereoDegrees(float degrees) {
    //Eval
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
    return true; //deprecated
  }

  // /////////////// getProperty /////////////

  public Object getProperty(String returnType, String infoType, String paramInfo) {
    return getProperty(returnType, infoType, (Object) paramInfo);
  }

  public Object getProperty(String returnType, String infoType, Object paramInfo) {
    // accepts a BitSet paramInfo
    // return types include "JSON", "string", "readable", and anything else returns the Java object.
    return propertyManager.getProperty(returnType, infoType, paramInfo);
  }

  String getModelExtract(Object atomExpression) {
    return fileManager.getFullPathName() + "\nJmol version " + getJmolVersion()
        + "\nEXTRACT: " + atomExpression + "\n"
        + modelManager.getModelExtract(getAtomBitSet(atomExpression));
  }

  String getHexColorFromIndex(short colix) {
    return g3d.getHexColorFromIndex(colix);
  }

  //////////////////////////////////////////////////

  void setModelVisibility() {
    //Eval -- ok - handled specially
    modelManager.setModelVisibility();
  }

  boolean isTainted = true;

  void setTainted(boolean TF) {
    isTainted = TF && refreshing;
    axesAreTainted = TF && refreshing;
  }

  void checkObjectClicked(int x, int y, int modifiers) {
    modelManager.checkObjectClicked(x, y, modifiers);
  }

  void checkObjectHovered(int x, int y) {
    modelManager.checkObjectHovered(x, y);
  }

  void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    modelManager.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers);
  }

  static int cardinalityOf(BitSet bs) {
    int nbitset = 0;
    if (bs != null)
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
                               float degrees, float endDegrees, boolean isSpin, boolean isSelected) {
    // Eval: rotate FIXED
    transformManager.rotateAxisAngleAtCenter(rotCenter, rotAxis, degrees,
        endDegrees, isSpin, isSelected);
  }

  void rotateAboutPointsInternal(Point3f point1, Point3f point2,
                                 float nDegrees, float endDegrees,
                                 boolean isSpin, boolean isSelected) {
    // Eval: rotate INTERNAL
    transformManager.rotateAboutPointsInternal(point1, point2, nDegrees,
        endDegrees, false, isSpin, isSelected);
  }

  private void setPickingSpinRate(int rate) {
    //Eval
    if (rate < 1)
      rate = 1;
    global.pickingSpinRate = rate;
  }

  int getPickingSpinRate() {
    //PickingManager
    return global.pickingSpinRate;
  }

  void startSpinningAxis(int atomIndex1, int atomIndex2, boolean isClockwise) {
    // PickingManager.setAtomPicked  "set picking SPIN"
    Point3f pt1 = modelManager.getAtomPoint3f(atomIndex1);
    Point3f pt2 = modelManager.getAtomPoint3f(atomIndex2);
    startSpinningAxis(pt1, pt2, isClockwise);
  }

  void startSpinningAxis(Point3f pt1, Point3f pt2, boolean isClockwise) {
    //Draw.checkObjectClicked  ** could be difficult
    //from draw object click
    if (getSpinOn()) {
      setSpinOn(false);
      return;
    }
    transformManager.rotateAboutPointsInternal(pt1, pt2,
        global.pickingSpinRate, Float.MAX_VALUE, isClockwise, true, false);
  }

  Vector3f getModelDipole() {
    return modelManager.getModelDipole();
  }

  void getBondDipoles() {
    modelManager.getBondDipoles();
    return;
  }

  private void setDipoleScale(float scale) {
    //Eval
    loadShape(JmolConstants.SHAPE_DIPOLES);
    setShapeProperty(JmolConstants.SHAPE_DIPOLES, "dipoleVectorScale",
        new Float(scale));
  }

  void getAtomIdentityInfo(int atomIndex, Hashtable info) {
    modelManager.getAtomIdentityInfo(atomIndex, info);
  }

  void setDefaultLattice(Point3f ptLattice) {
    //Eval -- handled separately
    global.setDefaultLattice(ptLattice);
  }

  Point3f getDefaultLattice() {
    return global.getDefaultLatticePoint();
  }

  BitSet getTaintedAtoms() {
    return modelManager.getTaintedAtoms();
  }
  
  void setTaintedAtoms(BitSet bs) {
    modelManager.setTaintedAtoms(bs);
  }

  public String getData(String atomExpression, String type) {
    String exp = "";
    if (type.toLowerCase().indexOf("property_") == 0)
      exp = "{selected}.label(\"%{" + type + "}\")";
    else if (type.equalsIgnoreCase("PDB"))
      exp = "{selected and not hetero}.label(\"ATOM  %5i  %-3a%1A%3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines"
          + "+{selected and hetero}.label(\"HETATM%5i %-4a%1A%3n %1c%4R%1E   %8.3x%8.3y%8.3z%6.2Q%6.2b          %2e  \").lines";
    else if (type.equalsIgnoreCase("MOL"))
      exp = "\"line1\nline2\nline3\n\"+(\"\"+{selected}.size)%-3+(\"\"+{selected}.bonds.size)%-3+\"  0  0  0\n\""
          + "+{selected}.labels(\"%10.4x%10.4y%10.4z %-2e  0  0  0  0  0\").lines"
          + "+{selected}.bonds.labels(\"%3D1%3D2%3ORDER  0  0  0\").lines";
    else
      //if(type.equals("XYZ"))
      exp = "\"\" + {selected}.size + \"\n\n\"+{selected}.label(\"%-2e %10.5x %10.5y %10.5z\").lines";
    if (!atomExpression.equals("selected"))
      exp = TextFormat.simpleReplace(exp, "selected", atomExpression);
    return (String) Eval.evaluateExpression(this, exp);
  }
  
  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    //Frame equivalent used in DATA "coord set"
    modelManager.setAtomCoord(atomIndex, x, y, z);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    modelManager.setAtomCoordRelative(atomIndex, x, y, z);
  }

  void setAtomCoordRelative(Point3f offset) {
    //Eval
    modelManager.setAtomCoordRelative(offset, selectionManager.bsSelection);
  }

  void setRotateSelected(boolean TF) {
    transformManager.setRotateSelected(TF);
  }
  
  void setRotateMolecule(boolean TF) {
    transformManager.setRotateMolecule(TF);
  }
  
  void setAllowRotateSelected(boolean TF) {
    global.allowRotateSelected = TF;
  }

  void setDynamicMeasurements(boolean TF) {
    global.dynamicMeasurements = TF;
  }
  
  boolean getDynamicMeasurements() {
    return global.dynamicMeasurements;
  }

  boolean allowRotateSelected() {
    return global.allowRotateSelected;
  }

  void invertSelected(Point3f pt, BitSet bs) {
    //Eval
    modelManager.invertSelected(pt, null, bs);
  }

  void invertSelected(Point3f pt, Point4f plane) {
    //Eval
    modelManager.invertSelected(pt, plane, selectionManager.bsSelection);
  }

  void rotateSelected(Matrix3f mNew, Matrix3f matrixRotate, boolean fullMolecule) {
    modelManager.rotateSelected(mNew, matrixRotate, selectionManager.bsSelection, fullMolecule);
  }

  float functionXY(String functionName, int x, int y) {
    return statusManager.functionXY(functionName, x, y);
  }

  String eval(String strEval) {
    return statusManager.eval(strEval);
  }

  Point3f[] getAdditionalHydrogens(BitSet atomSet) {
    return modelManager.getAdditionalHydrogens(atomSet);
  }

  private void setHelpPath(String url) {
    //Eval 
    global.helpPath = url;
  }

  void getHelp(String what) {
    showUrl(global.helpPath + what);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to stateManager
  // ///////////////////////////////////////////////////////////////

  /*
   * Moved from the consoles to viewer, since this could be of general
   * interest, it's more a property of Eval/Viewer, and the consoles 
   * are really just a mechanism for getting user input and sending 
   * results, not saving a history of it all. Ultimately I hope to integrate
   * the mouse picking and possibly periodic updates of position 
   * into this history to get a full history. We'll see!  BH 9/2006
   * 
   */

  /**
   * Adds one or more commands to the command history
   * @param command  the command to add
   */
  void addCommand(String command) {
    commandHistory.addCommand(command);
  }

  /**
   * Removes one command from the command history
   * @return command removed
   */
  String removeCommand() {
    return commandHistory.removeCommand();
  }

  /**
   * Options include:
   * ;  all                             n == Integer.MAX_VALUE
   * ;  n prev                          n >= 1
   * ;  next                            n == -1
   * ;  set max to -2 - n               n <= -3
   * ;  just clear                      n == -2
   * ;  clear and turn off; return ""   n == 0
   * ;  clear and turn on; return ""    n == Integer.MIN_VALUE;
   *     
   * @param howFarBack  number of lines (-1 for next line)
   * @return one or more lines of command history
   */
  public String getSetHistory(int howFarBack) {
    return commandHistory.getSetHistory(howFarBack);
  }

  // ///////////////////////////////////////////////////////////////
  // image export
  // ///////////////////////////////////////////////////////////////

  void createImage(String type_name) { // or script now
    if (type_name == null)
      return;
    if (type_name.length() == 0)
      type_name = "JPG:jmol.jpg";
    int i = type_name.indexOf(":");
    if (i < 0) {
      i = type_name.length();
      type_name += ":jmol.jpg";
    }
    String type = type_name.substring(0, i);
    String file = type_name.substring(i + 1);
    createImage(file, type, 100);
  }

  /**
   * 
   * @param file filename or null for clipboard
   * @param type_text = data type if quality < Integer.MAX_VALUE; otherwise text
   * @param quality up to 100 for JPG quality; Integer.MAX_VALUE for text
   */
  public void createImage(String file, String type_text, int quality) {
    createImage(file, type_text, quality, -1 , -1);
  }

  void createImage(String file, String type_text, int quality, int width, int height) {
    int saveWidth = dimScreen.width;
    int saveHeight = dimScreen.height;
    if (width > 0 && height > 0)
      resizeImage(width, height, true);
    setModelVisibility();
    try {
    statusManager.createImage(file, type_text, quality);
    } catch (Exception e) {
      Logger.error("Error creating image: " + e.getMessage());
    }
    if (width > 0 && height > 0)
      resizeImage(saveWidth, saveHeight, true);
  }

  //////////unimplemented

  public void setSyncDriver(int syncMode) {
    //it was an idea...
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug(htmlName + " viewer setting sync driver " + syncMode);
    }
    statusManager.setSyncDriver(syncMode);
  }

  public int getSyncMode() {
    return statusManager.getSyncMode();
  }

  /*  
   void debugStack(String msg) {
   //what's the right way to do this?
   try {
   Logger.error(msg);
   String t = null;
   t.substring(3);
   } catch (Exception e) {
   e.printStackTrace();
   }
   }
   */

}
