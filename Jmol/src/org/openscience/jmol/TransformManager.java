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

import java.awt.Dimension;
import javax.vecmath.Point3d;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;

public class TransformManager {

  DisplayControl control;

  TransformManager(DisplayControl control) {
    this.control = control;
  }

  public void homePosition() {
    matrixRotate.setIdentity();         // no rotations
    setSlabEnabled(false);              // no slabbing
    slabToPercent(100);
    setZoomEnabled(true);
    zoomToPercent(100);
    scaleFitToScreen();
  }

  /****************************************************************
   ROTATIONS
  ****************************************************************/

  public final Matrix4d matrixRotate = new Matrix4d();

  public void rotateXYBy(int xDelta, int yDelta) {
    // what fraction of PI radians do you want to rotate?
    // the full screen width corresponds to a PI (180 degree) rotation
    // if you grab an atom near the outside edge of the molecule,
    // you can essentially "pull it" across the screen and it will
    // track with the mouse cursor

    // the accelerator is just a slop factor ... it felt a litte slow to me
    double rotateAccelerator = 1.1f;

    // a change in the x coordinate generates a rotation about the y axis
    double ytheta = Math.PI * xDelta / minScreenDimension;
    rotateByY(ytheta * rotateAccelerator);
    double xtheta = Math.PI * yDelta / minScreenDimension;
    rotateByX(xtheta * rotateAccelerator);
  }

  public void rotateZBy(int zDelta) {
    double rotateAccelerator = 1.1f;
    double ztheta = Math.PI * zDelta / minScreenDimension;
    rotateByZ(ztheta * rotateAccelerator);
  }

  public void rotateFront() {
    matrixRotate.setIdentity();
  }

  public void rotateToX(double angleRadians) {
    matrixRotate.rotX(angleRadians);
  }
  public void rotateToY(double angleRadians) {
    matrixRotate.rotY(angleRadians);
  }
  public void rotateToZ(double angleRadians) {
    matrixRotate.rotZ(angleRadians);
  }

  public void rotateByX(double angleRadians) {
    matrixTemp.rotX(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }
  public void rotateByY(double angleRadians) {
    matrixTemp.rotY(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }
  public void rotateByZ(double angleRadians) {
    matrixTemp.rotZ(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }

  public void rotate(AxisAngle4d axisAngle) {
    matrixTemp.setIdentity();
    matrixTemp.setRotation(axisAngle);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }

  /****************************************************************
   TRANSLATIONS
  ****************************************************************/
  public int xTranslation;
  public int yTranslation;

  public void translateXYBy(int xDelta, int yDelta) {
    xTranslation += xDelta;
    yTranslation += yDelta;
  }

  public void translateToXPercent(int percent) {
    // FIXME -- what is the proper RasMol interpretation of this with zooming?
    xTranslation = (dimCurrent.width/2) + dimCurrent.width * percent / 100;
  }

  public void translateToYPercent(int percent) {
    yTranslation = (dimCurrent.height/2) + dimCurrent.height * percent / 100;
  }

  public void translateToZPercent(int percent) {
    // FIXME who knows what this should be? some type of zoom?
  }

  public int getTranslationXPercent() {
    return (xTranslation - dimCurrent.width/2) * 100 / dimCurrent.width;
  }

  public int getTranslationYPercent() {
    return (yTranslation - dimCurrent.height/2) * 100 / dimCurrent.height;
  }

  public int getTranslationZPercent() {
    return 0;
  }

  /****************************************************************
   ZOOM
  ****************************************************************/
  public boolean zoomEnabled = true;
  // zoomPercent is the current displayed zoom value
  public int zoomPercent = 100;
  // zoomPercentSetting is the current setting of zoom
  // if zoom is not enabled then the two values will be different
  public int zoomPercentSetting = 100;

  public void zoomBy(int pixels) {
    int percent = pixels * zoomPercentSetting / minScreenDimension;
    if (percent == 0)
      percent = (pixels < 0) ? -1 : 1;
    zoomByPercent(percent);
  }

  public int getZoomPercent() {
    return zoomPercent;
  }

  public int getZoomPercentSetting() {
    return zoomPercentSetting;
  }

  public void zoomToPercent(int percentZoom) {
    zoomPercentSetting = percentZoom;
    calcZoom();
  }

  public void zoomByPercent(int percentZoom) {
    int delta = percentZoom * zoomPercentSetting / 100;
    if (delta == 0)
      delta = (percentZoom < 0) ? -1 : 1;
    zoomPercentSetting += delta;
    calcZoom();
  }

  private void calcZoom() {
    if (zoomPercentSetting < 5)
      zoomPercentSetting = 5;
    if (zoomPercentSetting > 1000)
      zoomPercentSetting = 1000;
    zoomPercent = (zoomEnabled) ? zoomPercentSetting : 100;
    scalePixelsPerAngstrom = scaleDefaultPixelsPerAngstrom *
      zoomPercent / 100;
  }

  public void setZoomEnabled(boolean zoomEnabled) {
    if (this.zoomEnabled != zoomEnabled) {
      this.zoomEnabled = zoomEnabled;
      calcZoom();
    }
  }

  /****************************************************************
   SLAB
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
  public final static int SLABREJECT = 0;
  public final static int SLABHALF = 1;
  public final static int SLABHOLLOW = 2;
  public final static int SLABSOLID = 3;
  public final static int SLABSECTION = 4;
  */

  public boolean slabEnabled = false;
  public int modeSlab;
  public int slabValue;
  public int slabPercentSetting = 100;

  public boolean getSlabEnabled() {
    return slabEnabled;
  }

  public int getSlabPercentSetting() {
    return slabPercentSetting;
  }

  public void slabBy(int pixels) {
    int percent = pixels * slabPercentSetting / minScreenDimension;
    if (percent == 0)
      percent = (pixels < 0) ? -1 : 1;
    slabPercentSetting += percent;
    calcSlab();
  }

  public void slabToPercent(int percentSlab) {
    slabPercentSetting = percentSlab;
    calcSlab();
  }

  public void slabByPercent(int percentSlab) {
    int delta = percentSlab * slabPercentSetting / 100;
    if (delta == 0)
      delta = (percentSlab < 0) ? -1 : 1;
    slabPercentSetting += delta;
    calcSlab();
  }

  public void setSlabEnabled(boolean slabEnabled) {
    if (this.slabEnabled != slabEnabled) {
      this.slabEnabled = slabEnabled;
      calcSlab();
    }
  }

  public void setModeSlab(int modeSlab) {
    this.modeSlab = modeSlab;
  }

  public int getModeSlab() {
    return modeSlab;
  }

  private void calcSlab() {
    if (slabEnabled) {
      if (slabPercentSetting < 0)
        slabPercentSetting = 0;
      else if (slabPercentSetting > 100)
        slabPercentSetting = 100;
      // all transformed z coordinates are negative
      // a slab percentage of 100 should map to zero
      // a slab percentage of 0 should map to -diameter
      int radius =
        (int)(control.getRotationRadius() * scalePixelsPerAngstrom);
      slabValue = (int)((-100+slabPercentSetting) * 2*radius / 100);
    }
  }

  /****************************************************************
   PERSPECTIVE
  ****************************************************************/
  public boolean perspectiveDepth = true;
  public double cameraDepth = 3;
  public int cameraZ;

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    this.perspectiveDepth = perspectiveDepth;
    scaleFitToScreen();
  }

  public boolean getPerspectiveDepth() {
    return perspectiveDepth;
  }

  public void setCameraDepth(double depth) {
    cameraDepth = depth;
  }

  public double getCameraDepth() {
    return cameraDepth;
  }

  public int getCameraZ() {
    return cameraZ;
  }

  /****************************************************************
   SCREEN SCALING
  ****************************************************************/
  public Dimension dimCurrent;
  public int minScreenDimension;
  public double scalePixelsPerAngstrom;
  public double scaleDefaultPixelsPerAngstrom;

  public void setScreenDimension(Dimension dimCurrent) {
    this.dimCurrent = dimCurrent;
  }

  public Dimension getScreenDimension() {
    return dimCurrent;
  }

  public void scaleFitToScreen() {
    if (dimCurrent == null || control.getFrame() == null)  {
      return;
    }
    // translate to the middle of the screen
    xTranslation = dimCurrent.width / 2;
    yTranslation = dimCurrent.height / 2;
    // find smaller screen dimension
    minScreenDimension = dimCurrent.width;
    if (dimCurrent.height < minScreenDimension)
      minScreenDimension = dimCurrent.height;
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (minScreenDimension > 2)
      minScreenDimension -= 2;
    scaleDefaultPixelsPerAngstrom =
      minScreenDimension / 2 / control.getRotationRadius();
    if (perspectiveDepth) {
      cameraZ = (int)(cameraDepth * minScreenDimension);
      double scaleFactor = (cameraZ + minScreenDimension/2) / (double)cameraZ;
      // mth - for some reason, I can make the scaleFactor bigger in this
      // case. I do not know why, but there is extra space around the edges.
      // I have looked at it three times and still cannot figure it out
      // so just bump it up a bit.
      scaleFactor += 0.02;
      scaleDefaultPixelsPerAngstrom *= scaleFactor;
    }
    calcZoom();
  }

  /****************************************************************
   * scalings
   ****************************************************************/

  public double scaleToScreen(int z, double sizeAngstroms) {
    // all z's are >= 0
    // so the more positive z is, the smaller the screen scale
    double pixelSize = sizeAngstroms * scalePixelsPerAngstrom;
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraZ) / (cameraZ + z);
    return pixelSize;
  }

  public int scaleToScreen(int z, int milliAngstroms) {
    int pixelSize = (int)(milliAngstroms * scalePixelsPerAngstrom / 1000);
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraZ) / (cameraZ + z);
    return pixelSize;
  }

  /****************************************************************
   TRANSFORMATIONS
  ****************************************************************/

  public final Matrix4d matrixTransform = new Matrix4d();
  private final Point3d point3dScreenTemp = new Point3d();
  private final Point3i point3iScreenTemp = new Point3i();
  private final Matrix4d matrixTemp = new Matrix4d();
  private final Vector3d vectorTemp = new Vector3d();

  public void calcViewTransformMatrix() {
    // you absolutely *must* watch the order of these operations
    matrixTransform.setIdentity();
    // first, translate the coordinates back to the center
    vectorTemp.set(control.getRotationCenter());
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixTransform.mul(matrixRotate);
    matrixTransform.mul(matrixRotate, matrixTransform);
    // now shift so that all z coordinates are >= 0
    // this is important for scaling
    vectorTemp.x = 0;
    vectorTemp.y = 0;
    vectorTemp.z = control.getRotationRadius();
    matrixTemp.setTranslation(vectorTemp);
    matrixTransform.sub(matrixTemp);
    // now scale to screen coordinates
    matrixTemp.set(-scalePixelsPerAngstrom); // invert y & z
    matrixTemp.m00=scalePixelsPerAngstrom; // preserve x
    matrixTransform.mul(matrixTemp, matrixTransform);
    // note that the image is still centered at 0, 0
    // translations come later (to deal with perspective)
    // and all z coordinates are >= 0
  }

  public Point3i transformPoint(Point3d pointAngstroms) {
    matrixTransform.transform(pointAngstroms, point3dScreenTemp);
    int x = (int)point3dScreenTemp.x;
    int y = (int)point3dScreenTemp.y;
    int z = (int)point3dScreenTemp.z;
    if (z < 0) {
      System.out.println("WARNING! DANGER! z < 0! transformPoint()");
      z = 0;
    }
    if (perspectiveDepth) {
      int depth = cameraZ + z;
      point3iScreenTemp.x = ((x * cameraZ) / depth) + xTranslation;
      point3iScreenTemp.y = ((y * cameraZ) / depth) + yTranslation;
      point3iScreenTemp.z = z;
    } else {
      point3iScreenTemp.x = x + xTranslation;
      point3iScreenTemp.y = y + yTranslation;
      point3iScreenTemp.z = z;
    }
    return point3iScreenTemp;
  }

  /****************************************************************
   exports for POV rendering
  ****************************************************************/

  public Matrix4d getPovRotateMatrix() {
    return new Matrix4d(matrixRotate);
  }

  public Matrix4d getPovTranslateMatrix() {
    Matrix4d matrixPovTranslate = new Matrix4d();
    matrixPovTranslate.setIdentity();
    matrixPovTranslate.get(vectorTemp);
    vectorTemp.x = (xTranslation-dimCurrent.width/2) / scalePixelsPerAngstrom;
    vectorTemp.y = -(yTranslation-dimCurrent.height/2)
      / scalePixelsPerAngstrom; // invert y axis
    vectorTemp.z = 0;
    matrixPovTranslate.set(vectorTemp);
    return matrixPovTranslate;
  }
}
