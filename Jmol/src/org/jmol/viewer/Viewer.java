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
import org.jmol.util.*;
import org.jmol.i18n.GT;

import org.jmol.api.*;
import org.jmol.g3d.*;
import org.jmol.util.CommandHistory;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.Event;
import java.text.DecimalFormat;
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

  public void finalize() {
    Logger.debug("viewer finalize " + this);
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
  private MouseManager mouseManager;
  private PickingManager pickingManager;
  private PropertyManager propertyManager;
  private RepaintManager repaintManager;
  private ScriptManager scriptManager;
  private SelectionManager selectionManager;
  private StateManager stateManager = new StateManager(this);
  private StateManager.GlobalSettings global = stateManager.globalSettings;
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
    Logger.debug("Viewer constructor " + this);
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
    g3d = new Graphics3D(display);
    statusManager = new StatusManager(this);
    scriptManager = new ScriptManager(this);
    colorManager = new ColorManager(this, g3d);
    transformManager = new TransformManager(this);
    selectionManager = new SelectionManager(this);
    if (jvm14orGreater)
      mouseManager = MouseWrapper14.alloc(display, this);
    else if (jvm11orGreater)
      mouseManager = MouseWrapper11.alloc(display, this);
    else
      mouseManager = new MouseManager10(display, this);
    modelManager = new ModelManager(this);
    propertyManager = new PropertyManager(this);
    tempManager = new TempManager(this);
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

  public void setAppletContext(String htmlName, URL documentBase, URL codeBase,
                               String appletProxyOrCommandOptions) {
    this.htmlName = htmlName;
    isApplet = (documentBase != null);
    String str = appletProxyOrCommandOptions;
    if (!isApplet) {
      // not an applet -- used to pass along command line options
      if (str.indexOf("-i") >= 0) {
        setLogLevel(3); //no info, but warnings and errors
        isSilent = true;
      }
      if (str.indexOf("-x") >= 0) {
        autoExit = true;
      }
      if (str.indexOf("-n") >= 0) {
        haveDisplay = false;
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
          + "\n" + htmlName);
    }

    if (isApplet)
      fileManager.setAppletContext(documentBase, codeBase,
          appletProxyOrCommandOptions);
    zap(); //here to allow echos
  }

  String getHtmlName() {
    return htmlName;
  }

  boolean mustRenderFlag() {
    return mustRender;
  }

  static void setLogLevel(int ilevel) {
    for (int i = Logger.NB_LEVELS; --i >= 0;)
      Logger.setActiveLevel(i, (Logger.NB_LEVELS - i) <= ilevel);
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
    setBooleanProperty("windowCentered", true);
    transformManager.homePosition();
    if (modelManager.modelsHaveSymmetry())
      stateManager.setCrystallographicDefaults();
    refresh(1, "Viewer:homePosition()");
  }

  public void homePosition() {
    script("reset");
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

  String getJmolVersion() {
    return JmolConstants.version + "  " + JmolConstants.date;
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to StateManager
  // ///////////////////////////////////////////////////////////////

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

  Point3f getRotationCenter() {
    return transformManager.getRotationCenter();
  }

  void setCenter(String relativeTo, Point3f pt) {
    //Eval
    transformManager.setCenter(relativeTo, pt);
    refresh(0, "Viewer:setCenter(" + relativeTo + ")");
  }

  void setCenterBitSet(BitSet bsCenter, boolean doScale) {
    //Eval
    //setCenterSelected
    transformManager.setCenterBitSet(bsCenter, doScale);
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
    transformManager.setNewRotationCenter(center, true);
    refresh(0, "Viewer:setCenterBitSet()");
  }

  void move(Vector3f dRot, int dZoom, Vector3f dTrans, int dSlab,
            float floatSecondsTotal, int fps) {
    //from Eval
    transformManager.move(dRot, dZoom, dTrans, dSlab, floatSecondsTotal, fps);
  }

  public void moveTo(float floatSecondsTotal, Point3f center, Point3f pt,
                     float degrees, float zoom, float xTrans, float yTrans,
                     float rotationRadius) {
    //from Eval
    transformManager.moveTo(floatSecondsTotal, center, pt, degrees, zoom,
        xTrans, yTrans, rotationRadius);
  }

  public void moveTo(float floatSecondsTotal, Matrix3f rotationMatrix,
                     Point3f center, float zoom, float xTrans, float yTrans,
                     float rotationRadius) {
    //from StateManager
    transformManager.moveTo(floatSecondsTotal, rotationMatrix, center, zoom,
        xTrans, yTrans, rotationRadius);
  }

  String getMoveToText(float timespan) {
    return transformManager.getMoveToText(timespan);
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

  final static float radiansPerDegree = (float) (2 * Math.PI / 360);

  public void rotateToX(int angleDegrees) {
    //deprecated
    rotateToX(angleDegrees * radiansPerDegree);
  }

  public void rotateToY(int angleDegrees) {
    //deprecated
    rotateToY(angleDegrees * radiansPerDegree);
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
    return transformManager.getZoomPercent();
  }

  float getZoomPercentFloat() {
    return transformManager.getZoomPercentFloat();
  }

  float getZoomPercentSetting() {
    return transformManager.getZoomPercentSetting();
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

  void depthToPercent(int percentDepth) {
    //Eval.depth
    transformManager.depthToPercent(percentDepth);
    refresh(0, "Viewer:depthToPercent()");
  }

  void setSlabEnabled(boolean slabEnabled) {
    //Eval.slab
    transformManager.setSlabEnabled(slabEnabled);
    refresh(0, "Viewer:setSlabEnabled()");
  }

  public Matrix4f getUnscaledTransformMatrix() {
    return transformManager.getUnscaledTransformMatrix();
  }

  void finalizeTransformParameters() {
    //FrameRenderer
    transformManager.finalizeTransformParameters();
    g3d.setSlabAndDepthValues(transformManager.slabValue,
        transformManager.depthValue);
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
    //Sticks renderer
    return transformManager.scaleToScreen(z, sizeAngstroms);
  }

  short scaleToScreen(int z, int milliAngstroms) {
    //all shapes    
    return transformManager.scaleToScreen(z, milliAngstroms);
  }

  float scaleToPerspective(int z, float sizeAngstroms) {
    //DotsRenderer
    return transformManager.scaleToPerspective(z, sizeAngstroms);
  }

  void scaleFitToScreen() {
    //setCenter
    transformManager.scaleFitToScreen();
  }

  void checkCameraDistance() {
    //RepaintManager
    if (!allowCameraMove())
      return;
    transformManager.checkCameraDistance();
  }

  void setScaleAngstromsPerInch(float angstromsPerInch) {
    //Eval.setScale3d
    transformManager.setScaleAngstromsPerInch(angstromsPerInch);
  }

  public void setVibrationPeriod(float period) {
    //Eval
    transformManager.setVibrationPeriod(period);
  }

  void setSpinX(int value) {
    //Eval
    transformManager.setSpinX(value);
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

  void setDefaultColors(String colorScheme) {
    //Eval
    //stateManager
    if (!isSilent)
      Logger.info("setting color scheme to:" + colorScheme);
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
    //Eval
    colorManager.setRubberbandArgb(argb);
  }

  short getColixRubberband() {
    return colorManager.colixRubberband;
  }

  void setElementArgb(int elementNumber, int argb) {
    //Eval
    colorManager.setElementArgb(elementNumber, argb);
  }

  float getVectorScale() {
    return transformManager.vectorScale;
  }

  public void setVectorScale(float scale) {
    //Eval
    transformManager.setVectorScale(scale);
  }

  public void setVibrationScale(float scale) {
    //Eval
    //AtomSetChooser
    transformManager.setVibrationScale(scale);
  }

  float getVibrationScale() {
    return transformManager.vibrationScale;
  }

  public void setBackgroundArgb(int argb) {
    //Eval
    colorManager.setBackgroundArgb(argb);
    refresh(0, "Viewer:setBackgroundArgb()");
  }

  public int getBackgroundArgb() {
    return colorManager.argbBackground;
  }

  public void setColorBackground(String colorName) {
    //ColorManager.setDefaultColors
    //Jmol applet
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
    //Eval
    colorManager.setSpecular(specular);
  }

  boolean getSpecular() {
    return colorManager.getSpecular();
  }

  void setSpecularPower(int specularPower) {
    //Eval
    colorManager.setSpecularPower(specularPower);
  }

  void setAmbientPercent(int ambientPercent) {
    //Eval
    colorManager.setAmbientPercent(ambientPercent);
  }

  void setDiffusePercent(int diffusePercent) {
    //Eval
    colorManager.setDiffusePercent(diffusePercent);
  }

  void setSpecularPercent(int specularPercent) {
    //Eval
    colorManager.setSpecularPercent(specularPercent);
  }

  short getColixAtomPalette(Atom atom, int pid) {
    return colorManager.getColixAtomPalette(atom, pid);
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

  public void setHideNotSelected(boolean TF) {
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
    //Eval
    modelManager.setFormalCharges(selectionManager.bsSelection, formalCharge);
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

  void setAppletProxy(String appletProxy) {
    //Eval
    fileManager.setAppletProxy(appletProxy);
  }

  public boolean isApplet() {
    return (htmlName.length() > 0);
  }

  void setDefaultDirectory(String defaultDirectory) {
    //Eval
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
    //Jmol app file dropper, main, OpenUrlAction, RecentFilesAction
    //app Jmol BYPASSES SCRIPTING **
    openFile(name, null, null);
  }

  void openFile(String name, int[] params, String loadScript) {
    //Eval
    if (name == null)
      return;
    if (name.equalsIgnoreCase("string")) {
      openStringInline(fileManager.inlineData, params);
      return;
    }
    if (name.equalsIgnoreCase("string[]")) {
      openStringInline(fileManager.inlineDataArray, params);
      return;
    }
    zap();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFile(name, params, loadScript);
    long ms = System.currentTimeMillis() - timeBegin;
    setStatusFileLoaded(1, name, "", modelManager.getModelSetName(), null, null);
    String sp = "";
    if (params != null)
      for (int i = 0; i < params.length; i++)
        sp += "," + params[i];
    Logger.info("openFile(" + name + sp + ")" + ms + " ms");
  }

  public void openFiles(String modelName, String[] names) {
    openFiles(modelName, names, null);
  }
  
  void openFiles(String modelName, String[] names, String loadScript) {
    //Eval
    zap();
    // keep old screen image while new file is being loaded
    // forceRefresh();
    long timeBegin = System.currentTimeMillis();
    fileManager.openFiles(modelName, names, loadScript);
    long ms = System.currentTimeMillis() - timeBegin;
    for (int i = 0; i < names.length; i++) {
      setStatusFileLoaded(1, names[i], "", modelManager.getModelSetName(),
          null, null);
    }
    Logger.info("openFiles(" + names.length + ") " + ms + " ms");
  }

  public void openStringInline(String strModel) {
    //Jmol app file dropper
    openStringInline(strModel, null);
  }

  private void openStringInline(String strModel, int[] params) {
    //loadInline, openFile, openStringInline
    clear();
    fileManager.openStringInline(strModel, params);
    String errorMsg = getOpenFileError();
    if (errorMsg == null)
      setStatusFileLoaded(1, "string", "", modelManager.getModelSetName(),
          null, null);
  }

  private void openStringInline(String[] arrayModels, int[] params) {
    //loadInline, openFile, openStringInline
    clear();
    fileManager.openStringInline(arrayModels, params);
    String errorMsg = getOpenFileError();
    if (errorMsg == null)
      setStatusFileLoaded(1, "string[]", "", modelManager.getModelSetName(),
          null, null);
  }

  public char getInlineChar() {
    return global.inlineNewlineChar;
  }

  public void loadInline(String strModel) {
    //applet Console, loadInline, app PasteClipboard
    loadInline(strModel, global.inlineNewlineChar);
  }

  public void loadInline(String strModel, char newLine) {
    //Eval data
    //loadInline
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
    int[] A = global.getDefaultLatticeArray();
    openStringInline(strModel, A);
  }

  public void loadInline(String[] arrayModels) {
    //Eval data
    //loadInline
    if (arrayModels == null || arrayModels.length == 0)
      return;
    int[] A = global.getDefaultLatticeArray();
    openStringInline(arrayModels, A); 
  }

  public void openDOM(Object DOMNode) {
    //applet.loadDOMNode
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
    String fullPathName = getFullPathName();
    String fileName = getFileName();
    Object clientFile = fileManager.waitForClientFileOrErrorMessage();
    if (clientFile instanceof String || clientFile == null) {
      String errorMsg = (String) clientFile;
      setStatusFileNotLoaded(fullPathName, errorMsg);
      if (errorMsg != null) {
        String msg = errorMsg;
        int pt = msg.lastIndexOf("/");
        if (pt > 0)
          msg = msg.substring(0, pt + 1) + '\n' + msg.substring(pt + 1);
        pt = msg.lastIndexOf("\\");
        if (pt > 0)
          msg = msg.substring(0, pt + 1) + '\n' + msg.substring(pt + 1);
        for (int i = 0; i < 2; i++) {
          pt = msg.indexOf(" ");
          if (pt > 0)
            msg = msg.substring(0, pt) + '\n' + msg.substring(pt + 1);
        }
        zap(msg);
      }
      return errorMsg;
    }
    openClientFile(fullPathName, fileName, clientFile);
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

  public boolean getEchoStateActive() {
    return modelManager.getEchoStateActive();
  }

  public void setEchoStateActive(boolean TF) {
    //Eval
    modelManager.setEchoStateActive(TF);
  }

  public void zap() {
    //Eval
    //setAppletContext
    clear();
    modelManager.zap();
    initializeModel();
  }

  private void zap(String msg) {
    zap();
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
    repaintManager.clear();
    transformManager.clear();
    pickingManager.clear();
    modelManager.clear();
    selectionManager.clear();
    clearAllMeasurements();
    statusManager.clear();
    stateManager.clear(global);
    clearPropertyFlags();
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
    if (modelManager.hasVibrationVectors())
      setShapeSize(JmolConstants.SHAPE_VECTORS, global.defaultVectorMad);
    setFrankOn(global.frankOn);
    repaintManager.initializePointers(1);
    setDisplayModelIndex(0);
    setBackgroundModelIndex(-1);
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
  // it can only return null. Do we need it?

  String getClientAtomStringProperty(Object clientAtom, String propertyName) {
    if (modelAdapter == null)
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
    return modelManager.getExportJmolAdapter();
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
    int modelIndex = getDisplayModelIndex();
    if (modelIndex < 0)
      return;
    modelManager.convertFractionalCoordinates(modelIndex, pt);
  }

  public void setCenterSelected() {
    //depricated
    script("center (selected)");
  }

  public void rebond() {
    //PreferencesDialog
    modelManager.rebond();
    refresh(0, "Viewer:rebond()");
  }

  public void setBondTolerance(float bondTolerance) {
    //PreferencesDialog
    if (bondTolerance == global.bondTolerance)
      return;
    Logger.info("default bond tolerance set to " + bondTolerance);
    global.bondTolerance = bondTolerance;
  }

  public float getBondTolerance() {
    return global.bondTolerance;
  }

  public void setMinBondDistance(float minBondDistance) {
    //PreferencesDialog
    if (minBondDistance == global.minBondDistance)
      return;
    Logger.info("default minimum bond distance set to " + minBondDistance);
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
    //select within(distance, coord) not compilable at the present time
    return modelManager.getAtomsWithin(distance, coord);
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

  String loadScript;
  void setLoadScript(String script) {
      loadScript = script;
  }
  
  public String getStateInfo() {
    StringBuffer s = new StringBuffer();
    //  file line
    s.append(fileManager.getState());
    //  atoms, bonds, labels, echos, shapes
    s.append(modelManager.getState());    
    //  frame information
    s.append(repaintManager.getState());
    //  orientation and slabbing
    s.append(transformManager.getState());
    //  display and selections
    s.append(selectionManager.getState());
    s.append(getPropertyState());
    return s.toString();
  }

  static Hashtable dataValues = new Hashtable();

  public void setData(String type, String[] data) {
    //Eval
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
    return modelManager.setConformation(getDisplayModelIndex(),
        conformationIndex);
  }

  public void autoHbond() {
    //Eval
    BitSet bs = getSelectionSet();
    autoHbond(bs, bs);
  }

  public void autoHbond(BitSet bsFrom, BitSet bsTo) {
    //Eval
    modelManager.autoHbond(bsFrom, bsTo);
  }

  boolean hbondsAreVisible() {
    return modelManager.hbondsAreVisible(getDisplayModelIndex());
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
    //Eval
    modelManager.setUnitCellOffset(getDisplayModelIndex(), offset);
  }

  void setCurrentUnitCellOffset(Point3f pt) {
    //Eval
    modelManager.setUnitCellOffset(getDisplayModelIndex(), pt);
  }

  /* ****************************************************************************
   * delegated to MeasurementManager
   ****************************************************************************/

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

  void defineMeasurement(Vector monitorExpressions, float[] rangeMinMax,
                         boolean isDelete, boolean isAllConnected,
                         boolean isShowHide, boolean isHidden) {
    //Eval.monitor()
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setConnected", new Boolean(
        isAllConnected));
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "setRange", rangeMinMax);
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
        new Boolean(isOFF));
    refresh(0, "hideMeasurements()");
  }

  void toggleMeasurement(int[] atomCountPlusIndices) {
    //Eval
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "toggle",
        atomCountPlusIndices);
  }

  // ///////////////////////////////////////////////////////////////
  // delegated to RepaintManager
  // ///////////////////////////////////////////////////////////////

  void repaint() {
    //from RepaintManager
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

  void rewindAnimation() {
    //Eval
    repaintManager.rewindAnimation();
    refresh(0, "Viewer:rewindAnimation()");
  }

  void setDisplayModelIndex(int modelIndex) {
    //Eval
    //initializeModel
    repaintManager.setDisplayModelIndex(modelIndex);
  }

  public int getDisplayModelIndex() {
    // modified to indicate if there is also a background model index
    int modelIndex = repaintManager.displayModelIndex;
    int backgroundIndex = getBackgroundModelIndex();
    return (backgroundIndex >= 0 ? -2 - modelIndex : modelIndex);
  }

  void setBackgroundModelIndex(int modelIndex) {
    //Eval
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

  public void refresh() {
    //Draw, pauseScriptExecution
    repaintManager.refresh();
  }

  public void refresh(int isOrientationChange, String strWhy) {
    repaintManager.refresh();
    statusManager.setStatusViewerRefreshed(isOrientationChange, strWhy);
  }

  void requestRepaintAndWait() {
    if (haveDisplay)
      repaintManager.requestRepaintAndWait();
  }

  public void repaintView() {
    repaintManager.repaintView();
  }

  private boolean axesAreTainted = false;

  boolean areAxesTainted() {
    boolean TF = axesAreTainted;
    axesAreTainted = false;
    return TF;
  }

  ////////////// screen/image methods ///////////////

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

  public void renderScreenImage(Graphics g, Dimension size, Rectangle clip) {
    if (isTainted || getSlabEnabled())
      setModelVisibility();
    isTainted = false;
    if (size != null)
      setScreenDimension(size);
    setRectClip(null);
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
    g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        matrixRotate, antialias);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
        repaintManager.displayModelIndex);
    // mth 2003-01-09 Linux Sun JVM 1.4.2_02
    // Sun is throwing a NullPointerExceptions inside graphics routines
    // while the window is resized.
    g3d.endRendering();
    return g3d.getScreenImage();
  }

  private Image getStereoImage(int stereoMode, boolean antialias) {
    g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        transformManager.getStereoRotationMatrix(true), antialias);
    repaintManager.render(g3d, rectClip, modelManager.getFrame(),
        repaintManager.displayModelIndex);
    g3d.endRendering();
    g3d.snapshotAnaglyphChannelBytes();
    g3d.beginRendering(rectClip.x, rectClip.y, rectClip.width, rectClip.height,
        transformManager.getStereoRotationMatrix(false), antialias);
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
    return g3d.getScreenImage();
  }

  private void render1(Graphics g, Image img, int x, int y) {
    if (g == null)
      return;
    try {
      g.drawImage(img, x, y, null);
    } catch (NullPointerException npe) {
      Logger.error("Sun!! ... fix graphics your bugs!");
    }
    g3d.releaseScreenImage();
  }

  public Image getScreenImage() {
    boolean antialias = true;
    boolean isStereo = false;
    setRectClip(null);
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

  public String evalFile(String strFilename) {
    // from app only
    return scriptManager.addScript(strFilename, true, false);
  }

  public String script(String strScript) {
    return evalString(strScript);
  }

  public String evalString(String strScript) {
    if (checkResume(strScript))
      return "script processing resumed";
    if (checkHalt(strScript))
      return "script execution halted";
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
      return str.equals("exit");
    }
    if (str.startsWith("quit")) {
      haltScriptExecution();
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
        false, null);
    GT.setDoTranslate(doTranslateTemp);
    return str;
  }

  public Object scriptWaitStatus(String strScript, String statusList) {
    scriptManager.waitForQueue();
    boolean doTranslateTemp = GT.getDoTranslate();
    GT.setDoTranslate(false);
    Object ret = evalStringWaitStatus("object", strScript, statusList, false,
        false, null);
    GT.setDoTranslate(doTranslateTemp);
    return ret;
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
    boolean isOK = (tokenInfo != null ? eval
        .loadTokenInfo(strScript, tokenInfo) : isScriptFile ? eval
        .loadScriptFile(strScript, isQuiet) : eval.loadScriptString(strScript,
        isQuiet));
    if (isOK) {
      eval.runEval();
      String strErrorMessage = eval.getErrorMessage();
      int msWalltime = eval.getExecutionWalltime();
      statusManager.setStatusScriptTermination(strErrorMessage, msWalltime);
      if (isScriptFile && writeInfo != null)
        createImage(writeInfo);
    }
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

  public void resumeScriptExecution() {
    eval.resumePausedExecution();
  }

  public void pauseScriptExecution() {
    refresh();
    eval.pauseExecution();
  }

  void setDefaultLoadScript(String script) {
    //Eval
    global.defaultLoadScript = script;
  }

  String getDefaultLoadScript() {
    return global.defaultLoadScript;
  }

  String getStandardLabelFormat() {
    return stateManager.getStandardLabelFormat();
  }

  int getRibbonAspectRatio() {
    return global.ribbonAspectRatio;
  }

  void setRibbonAspectRatio(int ratio) {
    //Eval
    global.ribbonAspectRatio = ratio;
  }

  float getSheetSmoothing() {
    return global.sheetSmoothing;
  }

  void setSheetSmoothing(float factor0To1) {
    //Eval
    global.sheetSmoothing = factor0To1;
  }

  boolean getSsbondsBackbone() {
    return global.ssbondsBackbone;
  }

  void setHbondsBackbone(boolean TF) {
    //Eval
    global.hbondsBackbone = TF;
  }

  boolean getHbondsBackbone() {
    return global.hbondsBackbone;
  }

  void setHbondsSolid(boolean TF) {
    //Eval
    global.hbondsSolid = TF;
  }

  boolean getHbondsSolid() {
    return global.hbondsSolid;
  }

  public void setMarBond(short marBond) {
    //stateManager.setCommonDefaults, setRasmolDefaults
    if (marBond != global.marBond)
      Logger.info("default bond radius set to " + (marBond / 1000f));
    global.marBond = marBond;
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
    //Eval
    if (strLabel != null) // force the class to load and display
      setShapeSize(JmolConstants.SHAPE_LABELS, global.pointsLabelFontSize);
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel);
  }

  void togglePickingLabel(BitSet bs) {
    //eval set toggleLabel (atomset)
    setShapeSize(JmolConstants.SHAPE_LABELS, global.pointsLabelFontSize);
    modelManager.setShapeProperty(JmolConstants.SHAPE_LABELS, "toggleLabel",
        null, bs);
    refresh(0, "Viewer:");
  }

  BitSet getBitSetSelection() {
    return selectionManager.bsSelection;
  }

  private void setShapeShow(int shapeID, boolean show) {
    setShapeSize(shapeID, show ? -1 : 0);
  }

  boolean getShapeShow(int shapeID) {
    return getShapeSize(shapeID) != 0;
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

  void setShapeProperty(int shapeID, String propertyName, Object value, BitSet bs) {
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
    //Eval
    global.rasmolHydrogenSetting = b;
  }

  boolean getRasmolHydrogenSetting() {
    return global.rasmolHydrogenSetting;
  }

  void setRasmolHeteroSetting(boolean b) {
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
    Logger.setActiveLevel(Logger.LEVEL_DEBUG, debugScript);
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

  void setPickingMode(int pickingMode) {
    //Eval
    pickingManager.setPickingMode(pickingMode);
  }

  int getPickingMode() {
    return pickingManager.getPickingMode();
  }

  void setPickingStyle(int pickingStyle) {
    //Eval
    pickingManager.setPickingStyle(pickingStyle);
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
    //Eval
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
    setFrankOn(true);
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
    //applet.Jmol
    //app Jmol
    //StatusManager
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
    //JmolPopup
    if (key.equalsIgnoreCase("hideNotSelected"))
      return selectionManager.getHideNotSelected();
    if (key.equalsIgnoreCase("colorRasmol"))
      return colorManager.getDefaultColorRasmol();
    if (key.equalsIgnoreCase("perspectiveDepth"))
      return getPerspectiveDepth();
    if (key.equalsIgnoreCase("showAxes"))
      return getShapeShow(JmolConstants.SHAPE_AXES);
    if (key.equalsIgnoreCase("showBoundBox"))
      return getShapeShow(JmolConstants.SHAPE_BBCAGE);
    if (key.equalsIgnoreCase("showUnitcell"))
      return getShapeShow(JmolConstants.SHAPE_UCCAGE);
    if (key.equalsIgnoreCase("debugScript"))
      return getDebugScript();
    if (key.equalsIgnoreCase("showHydrogens"))
      return getShowHydrogens();
    if (key.equalsIgnoreCase("frank"))
      return getFrankOn();
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
    if (key.equalsIgnoreCase("labelsFront"))
      return getLabelsFrontFlag();
    if (key.equalsIgnoreCase("labelsGroup"))
      return getLabelsGroupFlag();
    Logger.error("viewer.getBooleanProperty(" + key + ") - unrecognized");
    return false;
  }

  public void setBooleanProperty(String key, boolean value) {
    //Eval
    boolean isError = false;
    while (true) {
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
      if (key.equalsIgnoreCase("labelPointerBackground")) {
        setLabelPointerBackground(value);
        break;
      }
      if (key.equalsIgnoreCase("labelPointerNoBox")) {
        setLabelPointerNoBox(value);
        break;
      }
      if (key.equalsIgnoreCase("labelPointerBox")) {
        setLabelPointerBox(value);
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
      if (key.equalsIgnoreCase("showSelections")) { //deprecated -- see "selectionHalos"
        setSelectionHaloEnabled(value);
        break;
      }
      if (key.equalsIgnoreCase("debugScript")) {
        setDebugScript(value);
        break;
      }
      if (key.equalsIgnoreCase("frank")) {
        setFrankOn(value);
        break;
      }
      if (key.equalsIgnoreCase("showHydrogens")) {
        setShowHydrogens(value);
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
      if (key.equalsIgnoreCase("adjustCamera")) {
        setAdjustCamera(value);
        break;
      }
      if (key.equalsIgnoreCase("axesWindow")) {
        setBooleanProperty("axesMolecular", !value);
        return;
      }
      if (key.equalsIgnoreCase("axesMolecular")) {
        if (value)
          setBooleanProperty("axesUnitCell", false);
        setAxesModeMolecular(value);
        break;
      }
      if (key.equalsIgnoreCase("axesUnitCell")) {
        if (value)
          setBooleanProperty("axesMolecular", false);
        setAxesModeUnitCell(value);
        return;
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
      if (key.equalsIgnoreCase("labelsFront")) {
        setLabelsFrontFlag(value);
        break;
      }
      if (key.equalsIgnoreCase("labelsGroup")) {
        setLabelsGroupFlag(value);
        break;
      }
      // these next return, because there is no need to repaint
      while (true) {
        if (key.equalsIgnoreCase("zeroBasedXyzRasmol")) {
          setZeroBasedXyzRasmol(value);
          break;
        }
        if (key.equalsIgnoreCase("rangeSelected")) {
          setRangeSelected(value);
          break;
        }
        if (key.equalsIgnoreCase("cameraMove")) {
          setAllowCameraMove(value);
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
        isError = true;
        break;
      }
      if (!isError) {
        setPropertyFlag(key, value);
        return;
      }
      isError = true;
      break;
    }
    if (isError) {
      Logger.error("viewer.setBooleanProperty(" + key + "," + value
          + ") - unrecognized SET option");
      scriptStatus("Script ERROR: unrecognized SET option: set " + key);
      return;
    }
    setPropertyFlag(key, value);
    setTainted(true);
    refresh(0, "viewer.setBooleanProperty");
  }

  ////////  flags and settings ////////

  Hashtable htPropertyFlags = new Hashtable();
  
  final static String volatileProperties = "indicate all volatile properties here in lower case"
    + "";
  final static String unnecessaryProperties = "have no need for saving these"
    + "setfrank;showaxes;showunitcell;showboundbox;debugscript";

  void setPropertyFlag(String key, boolean value) {
    key = key.toLowerCase();
    if (unnecessaryProperties.indexOf(key + ";") < 0)
      htPropertyFlags.put(key, value ? Boolean.TRUE : Boolean.FALSE);  
  }

  void clearPropertyFlags() {
    Enumeration e = htPropertyFlags.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (volatileProperties.indexOf(key) >= 0) 
        htPropertyFlags.remove(key);
    }
  }
  
  String getPropertyState() {
    StringBuffer commands = new StringBuffer();
    Enumeration e = htPropertyFlags.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      commands.append("set "
          + key
          + " "
          + (((Boolean) htPropertyFlags.get(key)).booleanValue() ? "true"
              : "false") + ";\n");
    }
    return commands.toString();
  }
  
  boolean getDotSurfaceFlag() {
    return global.dotSurfaceFlag;
  }

  private void setDotSurfaceFlag(boolean TF) {
    global.dotSurfaceFlag = TF;
  }

  boolean getDotsSelectedOnlyFlag() {
    return global.dotsSelectedOnlyFlag;
  }

  private void setDotsSelectedOnlyFlag(boolean TF) {
    global.dotsSelectedOnlyFlag = TF;
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

  void setWindowCentered(boolean TF) {
    //stateManager
    transformManager.setWindowCentered(TF);
  }

  boolean isCameraAdjustable() {
    return global.adjustCameraFlag;
  }

  private void setAdjustCamera(boolean TF) {
    global.adjustCameraFlag = TF;
  }

  boolean allowCameraMove() {
    return global.allowCameraMoveFlag;
  }

  private void setAllowCameraMove(boolean TF) {
    global.allowCameraMoveFlag = TF;
  }

  void setSolventProbeRadius(float radius) {
    //Eval
    global.solventProbeRadius = radius;
  }

  float getSolventProbeRadius() {
    return global.solventProbeRadius;
  }

  float getCurrentSolventProbeRadius() {
    return global.solventOn ? global.solventProbeRadius : 0;
  }

  void setSolventOn(boolean isOn) {
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

  private void setAxesModeMolecular(boolean TF) {
    global.axesMode = (TF ? JmolConstants.AXES_MODE_MOLECULAR
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
  }

  void setAxesModeUnitCell(boolean TF) {
    //stateManager
    //setBooleanproperty
    global.axesMode = (TF ? JmolConstants.AXES_MODE_UNITCELL
        : JmolConstants.AXES_MODE_BOUNDBOX);
    axesAreTainted = true;
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
    //app DisplayPanel
    if (getSelectionHaloEnabled() != TF)
      script("selectionHalos " + TF);
  }

  void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    //Eval
    //setBooleanProperty
    loadShape(JmolConstants.SHAPE_HALOS);
    modelManager.setSelectionHaloEnabled(selectionHaloEnabled);
  }

  boolean getSelectionHaloEnabled() {
    return modelManager.getSelectionHaloEnabled();
  }

  void setBondSelectionModeOr(boolean bondSelectionModeOr) {
    //Eval
    global.bondSelectionModeOr = bondSelectionModeOr;
    refresh(0, "Viewer:setBondSelectionModeOr()");
  }

  boolean getBondSelectionModeOr() {
    return global.bondSelectionModeOr;
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
    return global.cartoonRocketFlag;
  }

  private void setCartoonRocketFlag(boolean TF) {
    global.cartoonRocketFlag = TF;
  }

  boolean getHideNameInPopup() {
    return global.hideNameInPopup;
  }

  private void setHideNameInPopup(boolean hideNameInPopup) {
    global.hideNameInPopup = hideNameInPopup;
  }

  private void setZoomLarge(boolean TF) {
    global.zoomLarge = TF;
    scaleFitToScreen();
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
  
  void setHermiteLevel(int level) {
    global.hermiteLevel = level;
  }
  
  boolean getHighResolution() {
    return global.highResolutionFlag;
  }

  private void setHighResolution(boolean TF) {
    global.highResolutionFlag = TF;
  }

  private void setLabelPointerBackground(boolean TF) {
    global.labelPointerBackground = TF;
  }

  boolean getLabelPointerBackground() {
    return global.labelPointerBackground;
  }

  private void setLabelPointerNoBox(boolean TF) {
    global.labelPointerNoBox = TF;
  }

  boolean getLabelPointerNoBox() {
    return global.labelPointerNoBox;
  }

  private void setLabelPointerBox(boolean TF) {
    global.labelPointerBox = TF;
  }

  boolean getLabelPointerBox() {
    return global.labelPointerBox;
  }

  void setSsbondsBackbone(boolean TF) {
    //Eval
    global.ssbondsBackbone = TF;
  }

  public void setAutoBond(boolean ab) {
    //app PreferencesDialog
    //setBooleanProperties
    modelManager.setAutoBond(ab);
    refresh(0, "Viewer:setAutoBond()");
  }

  public boolean getAutoBond() {
    return modelManager.autoBond;
  }
  
  int makeConnections(float minDistance, float maxDistance, short order,
                      int connectOperation, BitSet bsA, BitSet bsB) {
    //eval
    clearAllMeasurements(); // necessary for serialization
    return modelManager.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB);
  }

  // //////////////////////////////////////////////////////////////
  // Graphics3D
  // //////////////////////////////////////////////////////////////

  void setGreyscaleRendering(boolean greyscaleRendering) {
    //TranformManager (set stereomode)
    //setBooleanProperty
    global.greyscaleRendering = greyscaleRendering;
    g3d.setGreyscaleMode(greyscaleRendering);
    refresh(0, "Viewer:setGreyscaleRendering()");
  }

  boolean getGreyscaleRendering() {
    return global.greyscaleRendering;
  }

  private void setLabelsFrontFlag(boolean labelsFrontFlag) {
    global.labelsFrontFlag = labelsFrontFlag;
  }

  boolean getLabelsFrontFlag() {
    return global.labelsFrontFlag;
  }

  private void setLabelsGroupFlag(boolean labelsGroupFlag) {
    global.labelsGroupFlag = labelsGroupFlag;
  }

  boolean getLabelsGroupFlag() {
    return global.labelsGroupFlag;
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
    //PreferenceDialog
    //stateManager
    if (percentVdwAtom != global.percentVdwAtom)
      Logger.info("default percent van der Waal radius set to "
          + percentVdwAtom);
    global.percentVdwAtom = percentVdwAtom;
    setShapeSize(JmolConstants.SHAPE_BALLS, -percentVdwAtom);
  }

  public void setFrankOn(boolean TF) {
    //applet, initializeModel
    global.frankOn = TF;
    setShapeSize(JmolConstants.SHAPE_FRANK, TF ? 1 : 0);
  }

  boolean getFrankOn() {
    return global.frankOn;
  }

  public int getPercentVdwAtom() {
    return global.percentVdwAtom;
  }

  short getMadAtom() {
    return (short) -global.percentVdwAtom;
  }

  public short getMadBond() {
    return (short) (global.marBond * 2);
  }

  void setModeMultipleBond(byte modeMultipleBond) {
    //not implemented
    global.modeMultipleBond = modeMultipleBond;
    refresh(0, "Viewer:setModeMultipleBond()");
  }

  byte getModeMultipleBond() {
    //sticksRenderer
    return global.modeMultipleBond;
  }

  void setShowMultipleBonds(boolean TF) {
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
    global.showHydrogens = TF;
    refresh(0, "Viewer:setShowHydrogens()");
  }

  public boolean getShowHydrogens() {
    return global.showHydrogens;
  }

  public void setShowHiddenSelectionHalos(boolean TF) {
    //setBooleanProperty
    global.showHiddenSelectionHalos = TF;
    refresh(0, "Viewer:setShowHiddenSelectionHalos()");
  }

  public boolean getShowHiddenSelectionHalos() {
    return global.showHiddenSelectionHalos;
  }

  public void setShowBbcage(boolean showBbcage) {
    //PreferencesDialog
    setShapeShow(JmolConstants.SHAPE_BBCAGE, showBbcage);
  }

  public boolean getShowBbcage() {
    return getShapeShow(JmolConstants.SHAPE_BBCAGE);
  }

  public void setShowAxes(boolean showAxes) {
    //PreferencesDialog
    setShapeShow(JmolConstants.SHAPE_AXES, showAxes);
  }

  public boolean getShowAxes() {
    return getShapeShow(JmolConstants.SHAPE_AXES);
  }

  public void setShowMeasurements(boolean TF) {
    //PreferencesDialog
    //setbooleanProperty
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

  boolean setMeasureDistanceUnits(String units) {
    //stateManager
    //Eval
    if (!global.setMeasureDistanceUnits(units))
      return false;
    setShapeProperty(JmolConstants.SHAPE_MEASURES, "reformatDistances", null);
    return true;
  }

  String getMeasureDistanceUnits() {
    return global.getMeasureDistanceUnits();
  }

  public void setJmolDefaults() {
    //applet, app initApplication, initVariables
    stateManager.setJmolDefaults();
  }

  public void setRasmolDefaults() {
    //applet, app initApplication, initVariables
    stateManager.setRasmolDefaults();
  }

  void setZeroBasedXyzRasmol(boolean zeroBasedXyzRasmol) {
    //stateManager
    //setBooleanProperty
    global.zeroBasedXyzRasmol = zeroBasedXyzRasmol;
    modelManager.setZeroBased();
  }

  boolean getZeroBasedXyzRasmol() {
    return global.zeroBasedXyzRasmol;
  }

  void setLabelFontSize(int points) {
    //not implemented
    global.pointsLabelFontSize = (points <= 0 ? JmolConstants.LABEL_DEFAULT_FONTSIZE
        : points);
    refresh(0, "Viewer:setLabelFontSize()");
  }

  void setLabelOffset(int xOffset, int yOffset) {
    //not implemented
    global.labelOffsetX = xOffset;
    global.labelOffsetY = yOffset;
    refresh(0, "Viewer:setLabelOffset()");
  }

  int getLabelOffsetX() {
    return global.labelOffsetX;
  }

  int getLabelOffsetY() {
    return global.labelOffsetY;
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

  byte[] allocTempBytes(int size) {
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

  private DecimalFormat[] formatters;

  private static String[] formattingStrings = { "0", "0.0", "0.00", "0.000",
    "0.0000", "0.00000", "0.000000", "0.0000000", "0.00000000", "0.000000000" };

  String formatDecimal(float value, int decimalDigits) {
    if (decimalDigits < 0)
      return "" + value;
    if (formatters == null)
      formatters = new DecimalFormat[formattingStrings.length];
    if (decimalDigits >= formattingStrings.length)
      decimalDigits = formattingStrings.length - 1;
    DecimalFormat formatter = formatters[decimalDigits];
    if (formatter == null)
      formatter = formatters[decimalDigits] = new DecimalFormat(
          formattingStrings[decimalDigits]);
    return formatter.format(value);
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
    //Eval
    transformManager.setStereoMode(stereoMode);
  }

  void setStereoMode(int[] twoColors) {
    //Eval
    transformManager.setStereoMode(twoColors);
  }

  int getStereoMode() {
    return transformManager.stereoMode;
  }

  float getStereoDegrees() {
    return transformManager.stereoDegrees;
  }

  void setStereoDegrees(float degrees) {
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
    return true;
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

  //////////////////////////////////////////////////

  void setModelVisibility() {
    //Eval
    modelManager.setModelVisibility();
  }

  boolean isTainted = true;

  void setTainted(boolean TF) {
    isTainted = TF;
    axesAreTainted = TF;
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
    //Eval
    if (rate < 1)
      rate = 1;
    global.pickingSpinRate = rate;
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

  void setDipoleScale(float scale) {
    //Eval
    loadShape(JmolConstants.SHAPE_DIPOLES);
    setShapeProperty(JmolConstants.SHAPE_DIPOLES, "dipoleVectorScale",
        new Float(scale));
  }

  void getAtomIdentityInfo(int atomIndex, Hashtable info) {
    modelManager.getAtomIdentityInfo(atomIndex, info);
  }

  void setDefaultLattice(Point3f ptLattice) {
    //Eval
    global.setDefaultLattice(ptLattice);
  }

  Point3f getDefaultLattice() {
    return global.getDefaultLatticePoint();
  }

  public void setAtomCoord(int atomIndex, float x, float y, float z) {
    //not implemented
    modelManager.setAtomCoord(atomIndex, x, y, z);
  }

  public void setAtomCoordRelative(int atomIndex, float x, float y, float z) {
    //not implemented
    modelManager.setAtomCoordRelative(atomIndex, x, y, z);
  }

  public void setAtomCoordRelative(Point3f offset) {
    //Eval
    modelManager.setAtomCoordRelative(offset, selectionManager.bsSelection);
  }

  float functionXY(String functionName, int x, int y) {
    return statusManager.functionXY(functionName, x, y);
  }

  Point3f[] getAdditionalHydrogens(BitSet atomSet) {
    return modelManager.getAdditionalHydrogens(atomSet);
  }

  void setHelpPath(String url) {
    //Eval
    global.helpPath = url;
  }

  void getHelp(String what) {
    if (global.helpPath == null)
      global.helpPath = global.defaultHelpPath;
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

  void createImage(String type_name) {
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

  public void createImage(String file, String type, int quality) {
    setModelVisibility();
    statusManager.createImage(file, type, quality);
  }

  //////////unimplemented

  public void setSyncDriver(int syncMode) {
    //it was an idea...
    Logger.debug(htmlName + " viewer setting sync driver " + syncMode);
    statusManager.setSyncDriver(syncMode);
  }

  public int getSyncMode() {
    return statusManager.getSyncMode();
  }
}
