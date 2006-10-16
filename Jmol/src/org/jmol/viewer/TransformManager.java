/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jmol.viewer;

import org.jmol.util.Logger;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.AxisAngle4f;

import java.util.BitSet;
import java.util.Hashtable;

class TransformManager {

  Viewer viewer;

  TransformManager(Viewer viewer) {
    this.viewer = viewer;
  }

  /* ***************************************************************
   * GENERAL METHODS
   ***************************************************************/

  void homePosition() {
    // reset
    setDefaultRotation();
    setCenter(null);
    translateCenterTo(0, 0);
    matrixRotate.setIdentity(); // no rotations
    //    setSlabEnabled(false);              // no slabbing
    //    slabToPercent(100);
    setZoomEnabled(true);
    zoomToPercent(100);
    scaleFitToScreen();
  }

  final static float twoPI = (float) (2 * Math.PI);
  float spinX, spinY = 30f, spinZ, spinFps = 30f;
  Point3f fixedRotationCenter = new Point3f(0, 0, 0);
  AxisAngle4f fixedRotationAxis;
  float fixedRotationAngle = 0;
  float fixedRotationAngleFramed = 0;
  boolean haveNotifiedNaN = false;
  boolean haveNotifiedCamera = false;
  boolean isSpinInternal = false;
  boolean isSpinFixed = false;
  AxisAngle4f internalRotationAxis;
  Point3f internalRotationCenter = new Point3f(0, 0, 0);
  float internalRotationAngle = 0;

  /* ***************************************************************
   * ROTATIONS
   ***************************************************************/

  // this matrix only holds rotations ... no translations
  // however, it cannot be a Matrix3f because we need to multiply it by
  // a matrix4f which contains translations
  private final Matrix3f matrixRotate = new Matrix3f();
  private final Matrix3f matrixTemp3 = new Matrix3f();
  private final Matrix4f matrixTemp4 = new Matrix4f();
  final AxisAngle4f axisangleT = new AxisAngle4f();
  final Vector3f vectorT = new Vector3f();
  final Vector3f vectorT2 = new Vector3f();
  final Point3f pointT = new Point3f();
  final Point3f pointT2 = new Point3f();
  final static float radiansPerDegree = (float) (2 * Math.PI / 360);
  final static float degreesPerRadian = (float) (360 / (2 * Math.PI));

  void setFixedRotationCenter(Point3f rotationCenter) {
    //fixedRotationCenter = new Point3f();
    fixedRotationCenter.set(rotationCenter);
  }

  void checkFixedRotationCenter() {
    //fixedRotationCenter = new Point3f();
    Point3f pt = viewer.getRotationCenter();
    if (pt == null)
      fixedRotationCenter.set(0, 0, 0);
    else
      fixedRotationCenter.set(pt);
  }

  void setRotationPointXY(Point3f center) {
    Point3i newCenterScreen = transformPoint(center);
    translateCenterTo(newCenterScreen.x, newCenterScreen.y);
  }

  float setRotateInternal(Point3f center, Vector3f axis, float degrees) {
    checkFixedRotationCenter();
    internalRotationCenter.set(center);
    if (internalRotationAxis == null)
      internalRotationAxis = new AxisAngle4f();
    float radians = degrees * radiansPerDegree;
    internalRotationAxis.set(axis, radians);
    return radians;
  }

  float setRotateFixed(Point3f center, Vector3f axis, float degrees) {
    if (center != null)
      fixedRotationCenter.set(center);
    if (fixedRotationAxis == null)
      fixedRotationAxis = new AxisAngle4f();
    float radians = degrees * radiansPerDegree;
    fixedRotationAxis.set(axis, radians);
    return radians;
  }

  void rotateXYBy(int xDelta, int yDelta) {
    // from mouse action
    rotateXRadians(yDelta * radiansPerDegree);
    rotateYRadians(xDelta * radiansPerDegree);
  }

  void rotateZBy(int zDelta) {
    rotateZRadians((float) Math.PI * zDelta / 180);
  }

  void rotateFront() {
    matrixRotate.setIdentity();
  }

  void rotateToX(float angleRadians) {
    matrixRotate.rotX(angleRadians);
  }

  void rotateToY(float angleRadians) {
    matrixRotate.rotY(angleRadians);
  }

  void rotateToZ(float angleRadians) {
    matrixRotate.rotZ(angleRadians);
  }

  synchronized void rotateXRadians(float angleRadians) {
    matrixTemp3.rotX(angleRadians);
    matrixRotate.mul(matrixTemp3, matrixRotate);
  }

  synchronized void rotateYRadians(float angleRadians) {
    if (axesOrientationRasmol)
      angleRadians = -angleRadians;
    matrixTemp3.rotY(angleRadians);
    matrixRotate.mul(matrixTemp3, matrixRotate);
  }

  synchronized void rotateZRadians(float angleRadians) {
    if (axesOrientationRasmol)
      angleRadians = -angleRadians;
    matrixTemp3.rotZ(angleRadians);
    matrixRotate.mul(matrixTemp3, matrixRotate);
  }

  void rotateAxisAngle(Vector3f rotAxis, int degrees) {
    //not used 
    axisangleT.set(rotAxis, degrees * radiansPerDegree);
    rotateAxisAngle(axisangleT);
  }

  synchronized void rotateAxisAngle(AxisAngle4f axisAngle) {
    matrixTemp3.setIdentity();
    matrixTemp3.set(axisAngle);
    matrixRotate.mul(matrixTemp3, matrixRotate);
  }

  void rotateTo(float x, float y, float z, float degrees) {
    //unused
    if (degrees < .01 && degrees > -.01) {
      matrixRotate.setIdentity();
    } else {
      axisangleT.set(x, y, z, degrees * radiansPerDegree);
      matrixRotate.set(axisangleT);
    }
  }

  void rotateTo(AxisAngle4f axisAngle) {
    //unused
    if (axisAngle.angle < .01 && axisAngle.angle > -.01)
      matrixRotate.setIdentity();
    else
      matrixRotate.set(axisAngle);
  }

  /* ***************************************************************
   * *THE* TWO VIEWER INTERFACE METHODS
   ****************************************************************/

  void rotateAxisAngleAtCenter(Point3f rotCenter, Vector3f rotAxis,
                               float degrees, float endDegrees, boolean isSpin) {

    //*THE* Viewer FIXED frame rotation/spinning entry point
    if (rotCenter != null)
      moveRotationCenter(rotCenter);

    setSpinOn(false);

    if (degrees == 0)
      return;
    
    if (rotCenter != null) {
      setRotationPointXY(rotCenter);
    }
    float angle = setRotateFixed(rotCenter, rotAxis, degrees);
    if (isSpin) {
      isSpinInternal = false;
      isSpinFixed = true;
      setSpinOn(true, endDegrees);
      return;
    }
    rotateAxisAngleRadiansFixed(angle);
  }

  synchronized void rotateAxisAngleRadiansFixed(float angleRadians) {
    // for spinning -- reduced number of radians
    fixedRotationAngle = angleRadians;
    axisangleT.set(fixedRotationAxis);
    axisangleT.angle = angleRadians;
    rotateAxisAngle(axisangleT);
  }

  /* ***************************************************************
   * INTERNAL ROTATIONS
   ****************************************************************/

  void rotateAboutPointsInternal(Point3f point1, Point3f point2, float degrees,
                                 float endDegrees, boolean isClockwise,
                                 boolean isSpin) {

    // *THE* Viewer INTERNAL frame rotation entry point

    setSpinOn(false);

    if (degrees == 0)
      return;
    
    Vector3f axis = new Vector3f(point1);
    axis.sub(point2);
    if (isClockwise)
      axis.scale(-1f);
    float angle = setRotateInternal(point1, axis, degrees);

    if (isSpin) {
      isSpinInternal = true;
      isSpinFixed = false;
      setSpinOn(true, endDegrees);
      return;
    }
    rotateAxisAngleRadiansInternal(angle);
  }

  synchronized void rotateAxisAngleRadiansInternal(float radians) {

    // final matrix rotation when spinning or just rotating

    // trick is to apply the current rotation to the internal rotation axis
    // and then save the angle for generating a new fixed point later

    internalRotationAngle = radians;
    vectorT.set(internalRotationAxis.x, internalRotationAxis.y,
        internalRotationAxis.z);
    matrixRotate.transform(vectorT, vectorT2);
    axisangleT.set(vectorT2, radians);
    
    // NOW apply that rotation  

    matrixTemp3.set(axisangleT);
    matrixRotate.mul(matrixTemp3, matrixRotate);
    getNewFixedRotationCenter();
  }

  void getNewFixedRotationCenter() {

    /*
     * (1) determine vector offset VectorT () 
     * (2) translate old point so trueRotationPt is at [0,0,0] (old - true)
     * (3) do axisangle rotation of -radians (pointT2)
     * (4) translate back (pointT2 + vectorT)
     * 
     * The new position of old point is the new rotation center
     * set this, rotate about it, and it will APPEAR that the 
     * rotation was about the desired point and axis!
     *  
     */

    // fractional OPPOSITE of angle of rotation
    axisangleT.set(internalRotationAxis);
    axisangleT.angle = -internalRotationAngle;
    //this is a fraction of the original for spinning
    matrixTemp4.set(axisangleT);

    // apply this to the fixed center point in the internal frame

    vectorT.set(internalRotationCenter);
    pointT2.set(fixedRotationCenter);
    pointT2.sub(vectorT);
    matrixTemp4.transform(pointT2, pointT);

    // return this point to the fixed frame

    pointT.add(vectorT);

    // it is the new fixed rotation center!

    fixedRotationCenter.set(pointT);
    setCenterFromInternalRotation(fixedRotationCenter);
  }

  /* ***************************************************************
   * TRANSLATIONS
   ****************************************************************/
  float xFixedTranslation;
  float yFixedTranslation;

  void translateXYBy(int xDelta, int yDelta) {
    // mouse action only
    xFixedTranslation += xDelta;
    yFixedTranslation += yDelta;
  }

  void translateToXPercent(float percent) {
    xFixedTranslation = (width / 2) + width * percent / 100;
  }

  void translateToYPercent(float percent) {
    yFixedTranslation = (height / 2) + height * percent / 100;
  }

  void translateToZPercent(float percent) {
    // FIXME who knows what this should be? some type of zoom?
  }

  float getTranslationXPercent() {
    return (xFixedTranslation - width / 2) * 100 / width;
  }

  float getTranslationYPercent() {
    return (yFixedTranslation - height / 2) * 100 / height;
  }

  float getTranslationZPercent() {
    return 0;
  }

  String getTranslationScript() {
    String info = "";
    float f = getTranslationXPercent();
    if (f != 0.0)
      info += "translate x " + f + ";";
    f = getTranslationYPercent();
    if (f != 0.0)
      info += "translate y " + f + ";";
    return info;
  }

  void translateCenterTo(int x, int y) {
    xFixedTranslation = x;
    yFixedTranslation = y;
  }

  String getOrientationText() {
    return getMoveToText() + "\nOR\n" + getRotateZyzText(true);
  }

  Hashtable getOrientationInfo() {
    Hashtable info = new Hashtable();
    info.put("moveTo", getMoveToText());
    info.put("center", "center " + getCenterText());
    info.put("rotateZYZ", getRotateZyzText(false));
    info.put("rotateXYZ", getRotateXyzText());
    info.put("transXPercent", new Float(getTranslationXPercent()));
    info.put("transYPercent", new Float(getTranslationYPercent()));
    info.put("zoom", new Integer(zoomPercent));
    return info;
  }

  void getAxisAngle(AxisAngle4f axisAngle) {
    axisAngle.set(matrixRotate);
  }

  String getTransformText() {
    return matrixRotate.toString();
  }

  Matrix3f getMatrixRotate() {
    return matrixRotate;
  }

  void setRotation(Matrix3f matrixRotation) {
    matrixRotate.set(matrixRotation);
  }

  void getRotation(Matrix3f matrixRotation) {
    // hmm ... I suppose that there could be a race condiditon here
    // if matrixRotate is being modified while this is called
    matrixRotation.set(matrixRotate);
  }

  /* ***************************************************************
   * ZOOM
   ****************************************************************/
  boolean zoomEnabled = true;
  // zoomPercent is the current displayed zoom value
  int zoomPercent = 100;
  // zoomPercentSetting is the current setting of zoom
  // if zoom is not enabled then the two values will be different
  int zoomPercentSetting = 100;

  void zoomBy(int pixels) {
    if (pixels > 20)
      pixels = 20;
    else if (pixels < -20)
      pixels = -20;
    int deltaPercent = pixels * zoomPercentSetting / 50;
    if (deltaPercent == 0)
      deltaPercent = (pixels > 0 ? 1 : (deltaPercent < 0 ? -1 : 0));
    int percent = deltaPercent + zoomPercentSetting;
    zoomToPercent(percent);
  }

  int getZoomPercent() {
    return zoomPercent;
  }

  int getZoomPercentSetting() {
    return zoomPercentSetting;
  }

  void zoomToPercent(int percentZoom) {
    zoomPercentSetting = percentZoom;
    calcZoom();
  }

  void zoomByPercent(int percentZoom) {
    int delta = percentZoom * zoomPercentSetting / 100;
    if (delta == 0)
      delta = (percentZoom < 0) ? -1 : 1;
    zoomPercentSetting += delta;
    calcZoom();
  }

  private void setZoomParameters() {
    if (zoomPercentSetting < 5)
      zoomPercentSetting = 5;
    if (zoomPercentSetting > Viewer.MAXIMUM_ZOOM_PERCENTAGE)
      zoomPercentSetting = Viewer.MAXIMUM_ZOOM_PERCENTAGE;
    zoomPercent = (zoomEnabled) ? zoomPercentSetting : 100;
  }
  
  private void calcZoom() {
    setZoomParameters();
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom * zoomPercent / 100;
    System.out.println("calcZoom: " + scaleDefaultPixelsPerAngstrom + " " + zoomPercent);
  }

  void setZoomEnabled(boolean zoomEnabled) {
    if (this.zoomEnabled != zoomEnabled) {
      this.zoomEnabled = zoomEnabled;
      calcZoom();
    }
  }

  void setScaleAngstromsPerInch(float angstromsPerInch) {
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom = 72 / angstromsPerInch;
  }

  /* ***************************************************************
   * SLAB
   ****************************************************************/

  /*
   slab is a term defined and used in rasmol.
   it is a z-axis clipping plane. only atoms behind the slab get rendered.
   100% means:
   - the slab is set to z==0
   - 100% of the molecule will be shown
   50% means:
   - the slab is set to the center of rotation of the molecule
   - only the atoms behind the center of rotation are shown
   0% means:
   - the slab is set behind the molecule
   - 0% (nothing, nada, nil, null) gets shown
   */

  /*
   final static int SLABREJECT = 0;
   final static int SLABHALF = 1;
   final static int SLABHOLLOW = 2;
   final static int SLABSOLID = 3;
   final static int SLABSECTION = 4;
   */

  boolean slabEnabled = false;
  int modeSlab;
  int slabPercentSetting = 100;
  int depthPercentSetting = 0;

  int slabValue;
  int depthValue;

  boolean getSlabEnabled() {
    return slabEnabled;
  }

  int getSlabPercentSetting() {
    return slabPercentSetting;
  }

  void slabByPercentagePoints(int percentage) {
    slabPercentSetting += percentage;
    if (slabPercentSetting < 1)
      slabPercentSetting = 1;
    else if (slabPercentSetting > 100)
      slabPercentSetting = 100;
    if (depthPercentSetting >= slabPercentSetting)
      depthPercentSetting = slabPercentSetting - 1;
  }

  void depthByPercentagePoints(int percentage) {
    depthPercentSetting += percentage;
    if (depthPercentSetting < 0)
      depthPercentSetting = 0;
    else if (depthPercentSetting > 99)
      depthPercentSetting = 99;
    if (slabPercentSetting <= depthPercentSetting)
      slabPercentSetting = depthPercentSetting + 1;
  }

  void slabDepthByPercentagePoints(int percentage) {
    if (percentage > 0) {
      if (slabPercentSetting + percentage > 100)
        percentage = 100 - slabPercentSetting;
    } else {
      if (depthPercentSetting + percentage < 0)
        percentage = 0 - depthPercentSetting;
    }
    slabPercentSetting += percentage;
    depthPercentSetting += percentage;
  }

  void slabToPercent(int percentSlab) {
    slabPercentSetting = percentSlab < 1 ? 1 : percentSlab > 100 ? 100
        : percentSlab;
    if (depthPercentSetting >= slabPercentSetting)
      depthPercentSetting = slabPercentSetting - 1;
  }

  void setSlabEnabled(boolean slabEnabled) {
    this.slabEnabled = slabEnabled;
  }

  // depth is an extension added by OpenRasMol
  // it represents the 'back' of the slab plane
  void depthToPercent(int percentDepth) {
    depthPercentSetting = percentDepth < 0 ? 0 : percentDepth > 99 ? 99
        : percentDepth;
    if (slabPercentSetting <= depthPercentSetting)
      slabPercentSetting = depthPercentSetting + 1;
  }

  // miguel 24 sep 2004 - as I recall, this slab mode stuff is not implemented
  void setModeSlab(int modeSlab) {
    this.modeSlab = modeSlab;
  }

  int getModeSlab() {
    return modeSlab;
  }

  void calcSlabAndDepthValues(float rotationRadius) {
    slabValue = 0;
    depthValue = Integer.MAX_VALUE;
    if (slabEnabled) {
      // miguel 24 sep 2004 -- the comment below does not seem right to me
      // I don't think that all transformed z coordinates are negative
      // any more
      //
      // all transformed z coordinates are negative
      // a slab percentage of 100 should map to zero
      // a slab percentage of 0 should map to -diameter
      int radius = (int) (rotationRadius * scalePixelsPerAngstrom);
      slabValue = ((100 - slabPercentSetting) * 2 * radius / 100)
          + cameraDistance;
      depthValue = ((100 - depthPercentSetting) * 2 * radius / 100)
          + cameraDistance;
    }
  }

  /* ***************************************************************
   * PERSPECTIVE
   ****************************************************************/
  boolean perspectiveDepth = true;
  float cameraDepth = 3;
  int cameraDistance = 1000; // prevent divide by zero on startup
  float cameraDistanceFloat = 1000; // prevent divide by zero on startup

  void setPerspectiveDepth(boolean perspectiveDepth) {
    if (this.perspectiveDepth == perspectiveDepth)
      return;
    this.perspectiveDepth = perspectiveDepth;
    scaleFitToScreen();
  }

  boolean getPerspectiveDepth() {
    return perspectiveDepth;
  }

  void setCameraDepth(float depth) {
    cameraDepth = depth;
    Logger.debug("setCameraDepth: " + depth);
  }

  float getCameraDepth() {
    return cameraDepth;
  }

  void checkCameraDistance() {
    if (!increaseRotationRadius)
      return;
    float backupDistance = cameraDistance - minimumZ + 1f;
    increaseRotationRadius(backupDistance / scalePixelsPerAngstrom);
  }

  /* ***************************************************************
   * SCREEN SCALING
   ****************************************************************/
  boolean tOversample;
  int width, height;
  int width1, height1, width4, height4;
  int screenPixelCount;
  float scalePixelsPerAngstrom;
  float scaleDefaultPixelsPerAngstrom;

  void setScreenDimension(int width, int height) {
    this.width1 = this.width = width;
    this.width4 = width + width;
    this.height1 = this.height = height;
    this.height4 = height + height;
  }

  void setOversample(boolean tOversample) {
    if (this.tOversample == tOversample)
      return;
    this.tOversample = tOversample;
    if (tOversample) {
      width = width4;
      height = height4;
    } else {
      width = width1;
      height = height1;
    }
    scaleFitToScreen();
  }

  private void setTranslationCenterToScreen() {
    // translate to the middle of the screen
    xFixedTranslation = width / 2;
    yFixedTranslation = height / 2;
    // 2005 02 22
    // switch to finding larger screen dimension
    // find smaller screen dimension
    screenPixelCount = width;
    if (height > screenPixelCount)
      screenPixelCount = height;
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (screenPixelCount > 2)
      screenPixelCount -= 2;    
  }
  
  float defaultScaleToScreen() {
    /* 
     * 
     * the presumption here is that the rotation center is at pixel
     * (150,150) of a 300x300 window. Frame.getRotationRadius() returns
     * a rough estimate of the furthest distance from the center of rotation
     * (but not including pmesh, special lines, planes, etc. -- just atoms)
     * 
     * also that we do not want it to be possible for the model to rotate
     * out of bounds of the applet. For internal spinning I had to turn
     * of any calculation that would change the rotation radius.  hansonr
     * 
     */
    return screenPixelCount / 2f / viewer.getRotationRadius()
        * cameraScaleFactor();
  }

  private float cameraScaleFactor() {
    if (!perspectiveDepth)
      return 1;
    /*
     *  Say you have a 300x300 applet. Then we really think of it as a
     *  300x300x300 cube and define the camera distance as some multiple
     *  (cameraDepth) of this 300 pixel "screen depth".
     *  If the camera is far, far away from the model, then there won't 
     *  be much perspective setting, and we don't need a scaling factor. 
     *  But if the camera is close in, then perspective will drive XY points
     *  near the camera outside the applet window unless we scale them down. 
     *  Note that the calculation below reduces to:
     *  
     *  scaleFactor = (cameraDepth + 0.5) / cameraDepth 
     *   
     *  or, simply
     *  
     *  scaleFactor = 1 + (0.5 / cameraDepth)
     *  
     *  I can find nothing anywhere in any code that sets cameraDepth other
     *  than its default value of 3, so I think scaleFactor is always 1.167
     *  prior to adding 0.02 "for luck".
     *    
     *  hansonr
     */

    cameraDistance = (int) (cameraDepth * screenPixelCount);
    cameraDistanceFloat = cameraDistance;
    float scaleFactor = (cameraDistance + screenPixelCount / 2)
        / cameraDistanceFloat;
    // mth - for some reason, I can make the scaleFactor bigger in this
    // case. I do not know why, but there is extra space around the edges.
    // I have looked at it three times and still cannot figure it out
    // so just bump it up a bit.
    scaleFactor += 0.02;
    return scaleFactor;
  }
    
  void scaleFitToScreen() {
    if (width == 0 || height == 0 || !viewer.haveFrame())
      return;
    setTranslationCenterToScreen();
    scaleDefaultPixelsPerAngstrom = defaultScaleToScreen();
    calcZoom();
  }

  float scaleToScreen(int z, float sizeAngstroms) {
    // all z's are >= 0
    // so the more positive z is, the smaller the screen scale
    float pixelSize = sizeAngstroms * scalePixelsPerAngstrom;
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraDistance) / z;
    return pixelSize;
  }

  short scaleToScreen(int z, int milliAngstroms) {
    if (milliAngstroms == 0)
      return 0;
    int pixelSize = (int) (milliAngstroms * scalePixelsPerAngstrom / 1000);
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraDistance) / z;
    if (pixelSize == 0)
      return 1;
    return (short) pixelSize;
  }

  float scaleToPerspective(int z, float sizeAngstroms) {
    return (perspectiveDepth
    // mth 2004 04 02 ... what the hell is this ... must be a bug
        ? (sizeAngstroms * cameraDistance) / +z // <-- ??
        : sizeAngstroms);
  }

  /* ***************************************************************
   * TRANSFORMATIONS
   ****************************************************************/

  final Matrix4f matrixTransform = new Matrix4f();
  private final Point3f point3fVibrationTemp = new Point3f();
  private final Point3f point3fScreenTemp = new Point3f();
  private final Point3i point3iScreenTemp = new Point3i();
  private final Matrix4f matrixTemp = new Matrix4f();
  private final Vector3f vectorTemp = new Vector3f();

  /* ***************************************************************
   * RasMol has the +Y axis pointing down
   * And rotations about the y axis are left-handed
   * setting this flag makes Jmol mimic this behavior
   ****************************************************************/
  boolean axesOrientationRasmol = false;

  void setAxesOrientationRasmol(boolean axesOrientationRasmol) {
    this.axesOrientationRasmol = axesOrientationRasmol;
  }

  boolean increaseRotationRadius;

  float minimumZ;

  synchronized void finalizeTransformParameters() {
    calcTransformMatrix();
    calcSlabAndDepthValues(rotationRadius);
    increaseRotationRadius = false;
    minimumZ = Float.MAX_VALUE;
    haveNotifiedNaN = false;
    haveNotifiedCamera = false;   
    setPerspectiveOffset();
  }

  private final Vector3f perspectiveOffset = new Vector3f(0, 0, 0);
  void setPerspectiveOffset() {
    // lock in the perspective so that when you change
    // centers there is no jump
    if(!viewer.isWindowCentered()) {
      matrixTransform.transform(viewer.getRotationCenterDefault(), pointT);
      matrixTransform.transform(viewer.getRotationCenter(), pointT2);
      perspectiveOffset.sub(pointT, pointT2);
    }
    perspectiveOffset.x = xFixedTranslation; 
    perspectiveOffset.y = yFixedTranslation;
    if (!viewer.isCameraAdjustable())
      perspectiveOffset.z = 0;
    /*
     * Note that the effect of this modification is restricted to the 
     * (undocumented) specialized circumstances when both
     * 
     * (a) set windowCentered is false (formerly "set frieda on")
     * 
     *  AND
     *
     * (b) the center has been changed to something other than the default
     * rotation center, either using "set picking center" followed by a user
     * click of an atom, or by a scripted "center (atom expression)".
     * 
     * This adjustment has no effect whatsoever on general use.
     * 
     * Bob Hanson 4/06
     *  
     */
  }
 
  synchronized private void calcTransformMatrix() {
    // you absolutely *must* watch the order of these operations
    matrixTransform.setIdentity();

    // first, translate the coordinates back to the center

    vectorTemp.set(fixedRotationCenter);
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixTransform.mul(matrixRotate);
    matrixTemp.set(stereoFrame ? matrixStereo : matrixRotate);
    matrixTransform.mul(matrixTemp, matrixTransform);
    // we want all z coordinates >= 0, with larger coordinates further away
    // this is important for scaling, and is the way our zbuffer works
    // so first, translate an make all z coordinates negative
    vectorTemp.x = 0;
    vectorTemp.y = 0;
    vectorTemp.z = viewer.getRotationRadius() + cameraDistanceFloat
        / scalePixelsPerAngstrom;
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    if (axesOrientationRasmol)
      matrixTransform.add(matrixTemp); // make all z positive
    else
      matrixTransform.sub(matrixTemp); // make all z negative
    // now scale to screen coordinates
    matrixTemp.setZero();
    matrixTemp.set(scalePixelsPerAngstrom);
    if (!axesOrientationRasmol) {
      // negate y (for screen) and z (for zbuf)
      matrixTemp.m11 = matrixTemp.m22 = -scalePixelsPerAngstrom;
    }
    matrixTransform.mul(matrixTemp, matrixTransform);
    // note that the image is still centered at 0, 0 in the xy plane
    // all z coordinates are (should be) >= 0
    // translations come later (to deal with perspective)
  }

  Matrix4f getUnscaledTransformMatrix() {
    //for povray only
    Matrix4f unscaled = new Matrix4f();
    unscaled.setIdentity();
    if (fixedRotationCenter == null) {
      fixedRotationCenter = new Point3f();
      fixedRotationCenter.set(viewer.getRotationCenter());
    }
    vectorTemp.set(fixedRotationCenter);
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    unscaled.sub(matrixTemp);
    matrixTemp.set(matrixRotate);
    unscaled.mul(matrixTemp, unscaled);
    return unscaled;
  }

  void transformPoints(int count, Point3f[] angstroms, Point3i[] screens) {
    for (int i = count; --i >= 0;)
      screens[i].set(transformPoint(angstroms[i]));
  }

  void transformPoint(Point3f pointAngstroms, Point3i pointScreen) {
    pointScreen.set(transformPoint(pointAngstroms));
  }

  synchronized Point3i transformPoint(Point3f pointAngstroms) {
    matrixTransform.transform(pointAngstroms, point3fScreenTemp);
    return adjustedTemporaryScreenPoint(pointAngstroms);
  }
  
  Point3i adjustedTemporaryScreenPoint(Point3f pointAngstroms) {
    float z = (point3fScreenTemp.z - perspectiveOffset.z);
    if (z < cameraDistance) {
      if (Float.isNaN(point3fScreenTemp.z)) {
        //removed for extending pmesh to points and lines  BH 2/25/06 
        if (!haveNotifiedNaN)
          Logger.debug("NaN seen in TransformPoint");
        haveNotifiedNaN = true;
        z = 1;
      } else {
        if (!spinOn && !haveNotifiedCamera) {
          Logger.debug("need to back up the camera");
          Logger.debug("point3fScreenTemp.z=" + point3fScreenTemp.z
              + " -> z=" + z);
          haveNotifiedCamera = true;
        }
        increaseRotationRadius = true;
        if (z < minimumZ)
          minimumZ = z;
        if (z <= 0) {
          if (!spinOn && !haveNotifiedCamera) {
            Logger.debug("WARNING! DANGER! z <= 0! transformPoint()"
                + " point=" + pointAngstroms + "  ->  " + point3fScreenTemp);
            haveNotifiedCamera = true;
          }
          z = 1;
        }
      }
    }
    point3fScreenTemp.z = z;
    if (perspectiveDepth) {
      float perspectiveFactor = cameraDistanceFloat / z;
      point3fScreenTemp.x *= perspectiveFactor;
      point3fScreenTemp.y *= perspectiveFactor;
    }

    //higher resolution here for spin control. 

    point3fScreenTemp.x += perspectiveOffset.x;
    point3fScreenTemp.y += perspectiveOffset.y;
    if (Float.isNaN(point3fScreenTemp.x) && !haveNotifiedNaN) {
      Logger.debug("NaN found in transformPoint ");
      haveNotifiedNaN = true;
    }

    point3iScreenTemp.x = (int) point3fScreenTemp.x;
    point3iScreenTemp.y = (int) point3fScreenTemp.y;
    point3iScreenTemp.z = (int) point3fScreenTemp.z;
    
    return point3iScreenTemp;
  }

  void unTransformPoint(Point3i screenPt, Point3f coordPt) {
    Point3f pt = new Point3f();
    pt.set(screenPt.x, screenPt.y, screenPt.z);
    pt.x -= perspectiveOffset.x;
    pt.y -= perspectiveOffset.y;
    if (perspectiveDepth) {
      float perspectiveFactor = cameraDistanceFloat / pt.z;
      pt.x /= perspectiveFactor;
      pt.y /= perspectiveFactor;
    }
    pt.z += perspectiveOffset.z;
    Matrix4f m = new Matrix4f();
    m.invert(matrixTransform);
    m.transform(pt, coordPt);
  }
  
  void transformPoint(Point3f pointAngstroms, Point3f screen) {
    
    //used solely by RocketsRenderer
    
    matrixTransform.transform(pointAngstroms, point3fScreenTemp);
    adjustedTemporaryScreenPoint(pointAngstroms);
    screen.set(point3fScreenTemp);
  }

  Point3i transformPoint(Point3f pointAngstroms, Vector3f vibrationVector) {
    if (!vibrationOn || vibrationVector == null)
      matrixTransform.transform(pointAngstroms, point3fScreenTemp);
    else {
      point3fVibrationTemp.scaleAdd(vibrationAmplitude, vibrationVector,
          pointAngstroms);
      matrixTransform.transform(point3fVibrationTemp, point3fScreenTemp);
    }
    return adjustedTemporaryScreenPoint(pointAngstroms);
  }

  void transformPoint(Point3f pointAngstroms, Vector3f vibrationVector,
                      Point3i pointScreen) {
    pointScreen.set(transformPoint(pointAngstroms, vibrationVector));
  }

  void transformVector(Vector3f vectorAngstroms, Vector3f vectorTransformed) {
    matrixTransform.transform(vectorAngstroms, vectorTransformed);
  }

  /* ***************************************************************
   * move/moveTo support
   ****************************************************************/

  void move(Vector3f dRot, int dZoom, Vector3f dTrans, int dSlab,
            float floatSecondsTotal, int fps) {
    int zoom = getZoomPercent();
    int slab = getSlabPercentSetting();
    float transX = getTranslationXPercent();
    float transY = getTranslationYPercent();
    float transZ = getTranslationZPercent();

    long timeBegin = System.currentTimeMillis();
    int timePerStep = 1000 / fps;
    int totalSteps = (int) (fps * floatSecondsTotal);
    float radiansPerDegreePerStep = (float) Math.PI / 180 / totalSteps;
    float radiansXStep = radiansPerDegreePerStep * dRot.x;
    float radiansYStep = radiansPerDegreePerStep * dRot.y;
    float radiansZStep = radiansPerDegreePerStep * dRot.z;
    viewer.setInMotion(true);
    if (totalSteps == 0)
      totalSteps = 1; // to catch a zero secondsTotal parameter
    for (int i = 1; i <= totalSteps; ++i) {
      if (dRot.x != 0)
        rotateXRadians(radiansXStep);
      if (dRot.y != 0)
        rotateYRadians(radiansYStep);
      if (dRot.z != 0)
        rotateZRadians(radiansZStep);
      if (dZoom != 0)
        zoomToPercent(zoom + dZoom * i / totalSteps);
      if (dTrans.x != 0)
        translateToXPercent(transX + dTrans.x * i / totalSteps);
      if (dTrans.y != 0)
        translateToYPercent(transY + dTrans.y * i / totalSteps);
      if (dTrans.z != 0)
        translateToZPercent(transZ + dTrans.z * i / totalSteps);
      if (dSlab != 0)
        slabToPercent(slab + dSlab * i / totalSteps);
      int timeSpent = (int) (System.currentTimeMillis() - timeBegin);
      int timeAllowed = i * timePerStep;
      if (timeSpent < timeAllowed) {
        viewer.requestRepaintAndWait();
        if (!viewer.isScriptExecuting())
          break;
        timeSpent = (int) (System.currentTimeMillis() - timeBegin);
        int timeToSleep = timeAllowed - timeSpent;
        if (timeToSleep > 0) {
          try {
            Thread.sleep(timeToSleep);
          } catch (InterruptedException e) {
          }
        }
      }
    }
    viewer.setInMotion(false);
  }

  AxisAngle4f aaMoveTo;
  AxisAngle4f aaStep;
  AxisAngle4f aaTotal;
  Matrix3f matrixStart;
  Matrix3f matrixInverse;
  Matrix3f matrixStep;
  Matrix3f matrixEnd;
  Vector3f aaStepCenter;
  Point3f ptCenter;
  float targetPixelScale;

  void initializeMoveTo() {
    if (aaMoveTo != null)
      return;
    aaMoveTo = new AxisAngle4f();
    aaStep = new AxisAngle4f();
    aaTotal = new AxisAngle4f();
    matrixStart = new Matrix3f();
    matrixEnd = new Matrix3f();
    matrixStep = new Matrix3f();
    matrixInverse = new Matrix3f();
    aaStepCenter = new Vector3f();
  }
  
  void moveTo(float floatSecondsTotal, Point3f center, Point3f pt, float degrees, int zoom,
              int xTrans, int yTrans) {

    Vector3f axis = new Vector3f(pt);
    initializeMoveTo();
    if (degrees < 0.01f && degrees > -0.01f) {
      matrixEnd.setIdentity();
    } else {
      if (axis.x == 0 && axis.y == 0 && axis.z == 0) {
        // invalid ... no rotation
        int sleepTime = (int) (floatSecondsTotal * 1000) - 30;
        if (sleepTime > 0) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
          }
        }
        return;
      }
      aaMoveTo.set(axis, degrees * (float) Math.PI / 180);
      matrixEnd.set(aaMoveTo);
    }
    moveTo(floatSecondsTotal, null, center, zoom, xTrans, yTrans);
  }

  void moveTo(float floatSecondsTotal, Matrix3f end, Point3f center, int zoom,
              int xTrans, int yTrans) {
    initializeMoveTo();
    if (end != null)
      matrixEnd.set(end);
    ptCenter = (center == null ? fixedRotationCenter : center);
    targetPixelScale = (center == null ? scaleDefaultPixelsPerAngstrom
        : defaultScaleToScreen());
    getRotation(matrixStart);
    matrixInverse.invert(matrixStart);

    matrixStep.mul(matrixEnd, matrixInverse);
    aaTotal.set(matrixStep);

    int fps = 30;
    int totalSteps = (int) (floatSecondsTotal * fps);
    viewer.setInMotion(true);
    if (totalSteps > 1) {
      int frameTimeMillis = 1000 / fps;
      long targetTime = System.currentTimeMillis();
      int zoomStart = getZoomPercent();
      int zoomDelta = zoom - zoomStart;
      float xTransStart = getTranslationXPercent();
      float xTransDelta = xTrans - xTransStart;
      float yTransStart = getTranslationYPercent();
      float yTransDelta = yTrans - yTransStart;
      aaStepCenter.set(ptCenter);
      aaStepCenter.sub(fixedRotationCenter);
      aaStepCenter.scale(1f / totalSteps);
      float pixelScaleDelta = (targetPixelScale - scaleDefaultPixelsPerAngstrom)
          / totalSteps;

      for (int iStep = 1; iStep < totalSteps; ++iStep) {

        getRotation(matrixStart);
        matrixInverse.invert(matrixStart);
        matrixStep.mul(matrixEnd, matrixInverse);
        aaTotal.set(matrixStep);

        aaStep.set(aaTotal);
        aaStep.angle /= (totalSteps - iStep + 1);
        if (aaStep.angle == 0)
          matrixStep.setIdentity();
        else
          matrixStep.set(aaStep);
        matrixStep.mul(matrixStart);
        scaleDefaultPixelsPerAngstrom += pixelScaleDelta;
        zoomToPercent(zoomStart + (zoomDelta * iStep / totalSteps));
        translateToXPercent(xTransStart + (xTransDelta * iStep / totalSteps));
        translateToYPercent(yTransStart + (yTransDelta * iStep / totalSteps));
        setRotation(matrixStep);
        fixedRotationCenter.add(aaStepCenter);
        targetTime += frameTimeMillis;
        if (System.currentTimeMillis() < targetTime) {
          viewer.requestRepaintAndWait();
          if (!viewer.isScriptExecuting())
            break;
          int sleepTime = (int) (targetTime - System.currentTimeMillis());
          if (sleepTime > 0) {
            try {
              Thread.sleep(sleepTime);
            } catch (InterruptedException ie) {
            }
          }
        }
      }
    } else {
      int sleepTime = (int) (floatSecondsTotal * 1000) - 30;
      if (sleepTime > 0) {
        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        }
      }
    }
    zoomToPercent(zoom);
    translateToXPercent(xTrans);
    translateToYPercent(yTrans);
    setRotation(matrixEnd);
    setFixedRotationCenter(ptCenter);
    viewer.setInMotion(false);
  }
  
  String getMoveToText(float timespan) {
    axisangleT.set(matrixRotate);
    float degrees = axisangleT.angle * degreesPerRadian;
    StringBuffer sb = new StringBuffer();
    sb.append("moveto " + timespan);
    if (degrees < 0.01f) {
      sb.append(" 0 0 1 0"); 
    } else {
      vectorT.set(axisangleT.x, axisangleT.y, axisangleT.z);
      vectorT.normalize();
      vectorT.scale(1000);
      truncate0(sb, vectorT.x);
      truncate0(sb, vectorT.y);
      truncate0(sb, vectorT.z);
      truncate1(sb, degrees);
    }
    int zoom = getZoomPercent();
    int tX = (int) getTranslationXPercent();
    int tY = (int) getTranslationYPercent();
    if (zoom != 100 || tX != 0 || tY != 0) {
      sb.append(" ");
      sb.append(zoom);
      if (tX != 0 || tY != 0) {
        sb.append(" ");
        sb.append(tX);
        sb.append(" ");
        sb.append(tY);
      }
    }
    sb.append(" ");
    sb.append(getCenterText());
    return "" + sb + ";";
  }

  String getCenterText() {
    Point3f pt = viewer.getCenter();
    return "{" + pt.x + " " + pt.y + " " + pt.z + "}";
  }
  
  String getMoveToText() {
    return getMoveToText(1);
  }

  String getRotateXyzText() {
    StringBuffer sb = new StringBuffer();
    float m20 = matrixRotate.m20;
    float rY = -(float) Math.asin(m20) * degreesPerRadian;
    float rX, rZ;
    if (m20 > .999f || m20 < -.999f) {
      rX = -(float) Math.atan2(matrixRotate.m12, matrixRotate.m11)
          * degreesPerRadian;
      rZ = 0;
    } else {
      rX = (float) Math.atan2(matrixRotate.m21, matrixRotate.m22)
          * degreesPerRadian;
      rZ = (float) Math.atan2(matrixRotate.m10, matrixRotate.m00)
          * degreesPerRadian;
    }
    sb.append("reset");
    sb.append(";center " + getCenterText());
    if (rX != 0) {
      sb.append("; rotate x");
      truncate1(sb, rX);
    }
    if (rY != 0) {
      sb.append("; rotate y");
      truncate1(sb, rY);
    }
    if (rZ != 0) {
      sb.append("; rotate z");
      truncate1(sb, rZ);
    }
    sb.append(";");
    int zoom = getZoomPercent();
    if (zoom != 100) {
      sb.append(" zoom ");
      sb.append(zoom);
      sb.append(";");
    }
    float tX = getTranslationXPercent();
    if (tX != 0) {
      sb.append(" translate x ");
      sb.append(tX);
      sb.append(";");
    }
    float tY = getTranslationYPercent();
    if (tY != 0) {
      sb.append(" translate y ");
      sb.append(tY);
      sb.append(";");
    }
    return "" + sb;
  }

  String getRotateZyzText(boolean iAddComment) {
    StringBuffer sb = new StringBuffer();
    float m22 = matrixRotate.m22;
    float rY = (float) Math.acos(m22) * degreesPerRadian;
    float rZ1, rZ2;
    if (m22 > .999f || m22 < -.999f) {
      rZ1 = (float) Math.atan2(matrixRotate.m10, matrixRotate.m11)
          * degreesPerRadian;
      rZ2 = 0;
    } else {
      rZ1 = (float) Math.atan2(matrixRotate.m21, -matrixRotate.m20)
          * degreesPerRadian;
      rZ2 = (float) Math.atan2(matrixRotate.m12, matrixRotate.m02)
          * degreesPerRadian;
    }
    if (rZ1 != 0 && rY != 0 && rZ2 != 0 && iAddComment)
      sb.append("#Follows Z-Y-Z convention for Euler angles\n");
    sb.append("reset");
    sb.append(";center " + getCenterText());
    if (rZ1 != 0) {
      sb.append("; rotate z");
      truncate1(sb, rZ1);
    }
    if (rY != 0) {
      sb.append("; rotate y");
      truncate1(sb, rY);
    }
    if (rZ2 != 0) {
      sb.append("; rotate z");
      truncate1(sb, rZ2);
    }
    int zoom = getZoomPercent();
    if (zoom != 100) {
      sb.append("; zoom ");
      sb.append(zoom);
    }
    int tX = (int) getTranslationXPercent();
    if (tX != 0) {
      sb.append("; translate x ");
      sb.append(tX);
    }
    int tY = (int) getTranslationYPercent();
    if (tY != 0) {
      sb.append("; translate y ");
      sb.append(tY);
    }
    sb.append(';');
    return "" + sb;
  }

  static void truncate0(StringBuffer sb, float val) {
    sb.append(' ');
    sb.append(Math.round(val));
  }

  static void truncate1(StringBuffer sb, float val) {
    sb.append(' ');
    sb.append(Math.round(val * 10) / 10f);
  }

  /* ***************************************************************
   * Spin support
   ****************************************************************/

  void setSpinX(float degrees) {
    spinX = degrees;
    if (isSpinInternal || isSpinFixed)
      clearSpin();
  }

  void setSpinY(float degrees) {
    spinY = degrees;
    if (isSpinInternal || isSpinFixed)
      clearSpin();
  }

  void setSpinZ(float degrees) {
    spinZ = degrees;
    if (isSpinInternal || isSpinFixed)
      clearSpin();
  }

  void setSpinFps(int value) {
    if (value <= 0)
      value = 1;
    else if (value > 50)
      value = 50;
    spinFps = value;
  }

  void clearSpin() {
    setSpinOn(false);
    isSpinInternal = false;
    isSpinFixed = false;
    //back to the Chime defaults
  }

  boolean spinOn;
  SpinThread spinThread;

  void setSpinOn(boolean spinOn) {
    setSpinOn(spinOn, Float.MAX_VALUE);
  }

  void setSpinOn(boolean spinOn, float endDegrees) {
    this.spinOn = spinOn;
    if (spinOn) {
      if (spinThread == null) {
        spinThread = new SpinThread(endDegrees);
        spinThread.start();
      }
    } else {
      if (spinThread != null) {
        spinThread.interrupt();
        spinThread = null;
      }
    }
  }

  class SpinThread extends Thread implements Runnable {
    float endDegrees;
    float nDegrees = 0;
    SpinThread (float endDegrees) {
      this.endDegrees = Math.abs(endDegrees);
    }
    public void run() {
      float myFps = spinFps;
      int i = 0;
      long timeBegin = System.currentTimeMillis();
      while (!isInterrupted()) {
        if (myFps != spinFps) {
          myFps = spinFps;
          i = 0;
          timeBegin = System.currentTimeMillis();
        }
        if (myFps == 0 || !spinOn) {
          setSpinOn(false);
          return;
        }
        boolean refreshNeeded = (isSpinInternal && internalRotationAxis.angle != 0
            || isSpinFixed && fixedRotationAxis != null && fixedRotationAxis.angle != 0 
            || !isSpinFixed && !isSpinInternal && (spinX + spinY + spinZ != 0));
        ++i;
        int targetTime = (int) (i * 1000 / myFps);
        int currentTime = (int) (System.currentTimeMillis() - timeBegin);
        int sleepTime = targetTime - currentTime;
        if (sleepTime > 0) {
          if (refreshNeeded && spinOn) {
            float angle = 0;
            if (isSpinInternal || isSpinFixed) {
              angle = (isSpinInternal ? internalRotationAxis : fixedRotationAxis).angle/ myFps;
              if (isSpinInternal) {
                rotateAxisAngleRadiansInternal(angle);
              } else {
                rotateAxisAngleRadiansFixed(angle);
              }
              nDegrees += Math.abs(angle / twoPI * 360f);
            } else { // old way: Rx * Ry * Rz
              if (spinX != 0) {
                rotateXRadians(spinX * radiansPerDegree / myFps);
              }
              if (spinY != 0) {
                rotateYRadians(spinY * radiansPerDegree / myFps);
              }
              if (spinZ != 0) {
                rotateZRadians(spinZ * radiansPerDegree / myFps);
              }
            }
            viewer.refresh(1, "TransformationManager:SpinThread:run()");
            if (nDegrees >= endDegrees - 0.00001)
              setSpinOn(false);
          }
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException e) {
            break;
          }
        }
      }
    }
  }

  /* ***************************************************************
   * Vibration support
   ****************************************************************/

  boolean vibrationOn;
  float vibrationPeriod;
  int vibrationPeriodMs;
  float vibrationAmplitude;
  float vibrationRadians;

  void setVibrationPeriod(float period) {
    if (period <= 0) {
      this.vibrationPeriod = 0;
      this.vibrationPeriodMs = 0;
      clearVibration();
    } else {
      this.vibrationPeriod = period;
      this.vibrationPeriodMs = (int) (period * 1000);
      setVibrationOn(true);
    }
  }

  void setVibrationT(float t) {
    vibrationRadians = t * twoPI;
    vibrationAmplitude = (float) Math.cos(vibrationRadians) * vibrationScale;
  }

  float vectorScale = 1f;

  void setVectorScale(float scale) {
    if (scale >= -10 && scale <= 10)
      vectorScale = scale;
  }

  float vibrationScale = 1f;

  void setVibrationScale(float scale) {
    if (scale >= -10 && scale <= 10)
      vibrationScale = scale;
  }

  VibrationThread vibrationThread;

  private void setVibrationOn(boolean vibrationOn) {
    if (!vibrationOn || !viewer.haveFrame()) {
      if (vibrationThread != null) {
        vibrationThread.interrupt();
        vibrationThread = null;
      }
      this.vibrationOn = false;
      return;
    }
    if (viewer.getModelCount() < 1) {
      this.vibrationOn = false;
      return;
    }
    if (vibrationThread == null) {
      vibrationThread = new VibrationThread();
      vibrationThread.start();
    }
    this.vibrationOn = true;
  }

  void clearVibration() {
    setVibrationOn(false);
  }

  class VibrationThread extends Thread implements Runnable {

    public void run() {
      long startTime = System.currentTimeMillis();
      long lastRepaintTime = startTime;
      try {
        do {
          long currentTime = System.currentTimeMillis();
          int elapsed = (int) (currentTime - lastRepaintTime);
          int sleepTime = 33 - elapsed;
          if (sleepTime > 0)
            Thread.sleep(sleepTime);
          //
          lastRepaintTime = currentTime = System.currentTimeMillis();
          elapsed = (int) (currentTime - startTime);
          float t = (float) (elapsed % vibrationPeriodMs) / vibrationPeriodMs;
          setVibrationT(t);
          viewer.refresh(0, "TransformationManager:VibrationThread:run()");
        } while (!isInterrupted());
      } catch (InterruptedException ie) {
      }
    }
  }

  ////////////////////////////////////////////////////////////////
  // stereo support
  ////////////////////////////////////////////////////////////////

  int stereoMode;
  int[] stereoColors;
  void setStereoMode(int[] twoColors) {
    stereoMode = JmolConstants.STEREO_CUSTOM;
    stereoColors = twoColors;
    viewer.setGreyscaleRendering(true);
  }
  
  void setStereoMode(int stereoMode) {
    stereoColors = null;
    this.stereoMode = stereoMode;
    viewer.setGreyscaleRendering(stereoMode >= JmolConstants.STEREO_REDCYAN);
  }

  float stereoDegrees = 5;
  float stereoRadians = 5 * radiansPerDegree;

  void setStereoDegrees(float stereoDegrees) {
    this.stereoDegrees = stereoDegrees;
    this.stereoRadians = stereoDegrees * radiansPerDegree;
  }

  boolean stereoFrame;

  private final Matrix3f matrixStereo = new Matrix3f();

  synchronized Matrix3f getStereoRotationMatrix(boolean stereoFrame) {
    this.stereoFrame = stereoFrame;
    if (stereoFrame) {
      matrixTemp3.rotY(axesOrientationRasmol ? stereoRadians : -stereoRadians);
      matrixStereo.mul(matrixTemp3, matrixRotate);
    } else {
      matrixStereo.set(matrixRotate);
    }
    return matrixStereo;
  }
  
  /////////// rotation center ////////////
  
  //from Frame:
  
  Point3f rotationCenter;
  float rotationRadius;
  Point3f rotationCenterDefault;
  float rotationRadiusDefault;
  
  Point3f getRotationCenter() {
    return rotationCenter;
  }

  void setRotationCenter(Point3f center) {
    rotationCenter.set(center);  
  }
  
  Point3f getRotationCenterDefault() {
    return rotationCenterDefault;
  }

  void increaseRotationRadius(float increaseInAngstroms) {
    if (isWindowCentered())
      rotationRadius += increaseInAngstroms;
  }

  float getRotationRadius() {
    return rotationRadius;
  }

  Point3f setRotationCenterAndRadiusXYZ(Point3f newCenterOfRotation,
                                        boolean andRadius) {
    if (newCenterOfRotation != null) {
      rotationCenter = newCenterOfRotation;
      if (andRadius && isWindowCentered())
        rotationRadius = viewer.calcRotationRadius(rotationCenter);
    } else {
      rotationCenter = rotationCenterDefault;
      rotationRadius = rotationRadiusDefault;
    }
    return rotationCenter;
  }

  boolean windowCenteredFlag = true;
  
  boolean isWindowCentered() {
    return windowCenteredFlag;
  }

  void setWindowCentered(boolean TF) {
    windowCenteredFlag = TF;
  }

  Point3f setRotationCenterAndRadiusXYZ(String relativeTo, Point3f pt) {
    Point3f pointT = new Point3f(pt);
    if (relativeTo == "average")
      pointT.add(viewer.getAverageAtomPoint());
    else if (relativeTo == "boundbox")
      pointT.add(viewer.getBoundBoxCenter());
    else if (relativeTo != "absolute")
      pointT.set(getRotationCenterDefault());
    setRotationCenterAndRadiusXYZ(pointT, true);
    return pointT;
  }

  void setDefaultRotation() {
    rotationCenter = rotationCenterDefault = viewer.getBoundBoxCenter();
    rotationRadius = rotationRadiusDefault = viewer.calcRotationRadius(rotationCenterDefault);
  }

  //from ModelManager:

  void setCenterBitSet(BitSet bsCenter, boolean doScale) {
    Point3f center = (bsCenter != null && viewer.cardinalityOf(bsCenter) > 0 ? 
        viewer.getAtomSetCenter(bsCenter)
        : getRotationCenterDefault());
    setNewRotationCenter(center, doScale);
  }

  void setNewRotationCenter(Point3f center, boolean doScale) {
    // once we have the center, we need to optionally move it to 
    // the proper XY position and possibly scale
    if (isWindowCentered()) {
      translateToXPercent(0);
      translateToYPercent(0);///CenterTo(0, 0);
      setRotationCenterAndRadiusXYZ(center, true);
      if (doScale)
        scaleFitToScreen();
    } else {
      moveRotationCenter(center);
    }  
    setFixedRotationCenter(center);
  }
  
  // from Viewer:
  
  void setCenter(Point3f center) {
    center = setRotationCenterAndRadiusXYZ(center, true);
    if (center != null)
      setFixedRotationCenter(center);
  }

  void setCenterFromInternalRotation(Point3f center) {
    setRotationCenterAndRadiusXYZ(center, false);
  }

  void moveRotationCenter(Point3f center) {
    center = setRotationCenterAndRadiusXYZ(center, false);
    setFixedRotationCenter(center);
    setRotationPointXY(center);
  }

  void setCenter(String relativeTo, Point3f pt) {
    Point3f center = setRotationCenterAndRadiusXYZ(relativeTo, pt);
    scaleFitToScreen();
    if (center != null)
      setFixedRotationCenter(center);
  }
}
