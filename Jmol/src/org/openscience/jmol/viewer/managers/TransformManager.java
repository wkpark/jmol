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
package org.openscience.jmol.viewer.managers;

import org.openscience.jmol.viewer.JmolViewer;
import java.awt.Dimension;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.AxisAngle4f;

public class TransformManager {

  JmolViewer viewer;

  public TransformManager(JmolViewer viewer) {
    this.viewer = viewer;
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

  public final Matrix4f matrixRotate = new Matrix4f();

  public void rotateXYBy(int xDelta, int yDelta) {
    // what fraction of PI radians do you want to rotate?
    // the full screen width corresponds to a PI (180 degree) rotation
    // if you grab an atom near the outside edge of the molecule,
    // you can essentially "pull it" across the screen and it will
    // track with the mouse cursor

    // the accelerator is just a slop factor ... it felt a litte slow to me
    float rotateAccelerator = 1.1f;

    // a change in the x coordinate generates a rotation about the y axis
    float ytheta = (float)Math.PI * xDelta / minScreenDimension;
    rotateByY(ytheta * rotateAccelerator);
    float xtheta = (float)Math.PI * yDelta / minScreenDimension;
    rotateByX(xtheta * rotateAccelerator);
  }

  public void rotateZBy(int zDelta) {
    float rotateAccelerator = 1.1f;
    float ztheta = (float)Math.PI * zDelta / minScreenDimension;
    rotateByZ(ztheta * rotateAccelerator);
  }

  public void rotateFront() {
    matrixRotate.setIdentity();
  }

  public void rotateToX(float angleRadians) {
    matrixRotate.rotX(angleRadians);
  }
  public void rotateToY(float angleRadians) {
    matrixRotate.rotY(angleRadians);
  }
  public void rotateToZ(float angleRadians) {
    matrixRotate.rotZ(angleRadians);
  }

  public void rotateByX(float angleRadians) {
    matrixTemp.rotX(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }
  public void rotateByY(float angleRadians) {
    matrixTemp.rotY(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }
  public void rotateByZ(float angleRadians) {
    matrixTemp.rotZ(angleRadians);
    matrixRotate.mul(matrixTemp, matrixRotate);
  }

  public void rotate(AxisAngle4f axisAngle) {
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
    xTranslation = (width/2) + width * percent / 100;
  }

  public void translateToYPercent(int percent) {
    yTranslation = (height/2) + height * percent / 100;
  }

  public void translateToZPercent(int percent) {
    // FIXME who knows what this should be? some type of zoom?
  }

  public int getTranslationXPercent() {
    return (xTranslation - width/2) * 100 / width;
  }

  public int getTranslationYPercent() {
    return (yTranslation - height/2) * 100 / height;
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
        (int)(viewer.getRotationRadius() * scalePixelsPerAngstrom);
      slabValue = (int)((-100+slabPercentSetting) * 2*radius / 100);
    }
  }

  /****************************************************************
   PERSPECTIVE
  ****************************************************************/
  public boolean perspectiveDepth = true;
  public float cameraDepth = 3;
  public int cameraZ = 1000; // prevent divide by zero on startup

  public void setPerspectiveDepth(boolean perspectiveDepth) {
    this.perspectiveDepth = perspectiveDepth;
    scaleFitToScreen();
  }

  public boolean getPerspectiveDepth() {
    return perspectiveDepth;
  }

  public void setCameraDepth(float depth) {
    cameraDepth = depth;
  }

  public float getCameraDepth() {
    return cameraDepth;
  }

  public int getCameraZ() {
    return cameraZ;
  }

  /****************************************************************
   SCREEN SCALING
  ****************************************************************/
  boolean tOversample;
  public int width,height;
  public int width1, height1, width4, height4;
  public int minScreenDimension;
  public float scalePixelsPerAngstrom;
  public float scaleDefaultPixelsPerAngstrom;

  public void setScreenDimension(int width, int height) {
    this.width1 = this.width = width;
    this.width4 = width + width;
    this.height1 = this.height = height;
    this.height4 = height + height;
  }

  public void setOversample(boolean tOversample) {
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

  public void scaleFitToScreen() {
    if (width == 0 || height == 0 || !viewer.haveFile())
      return;
    // translate to the middle of the screen
    xTranslation = width / 2;
    yTranslation = height / 2;
    // find smaller screen dimension
    minScreenDimension = width;
    if (height < minScreenDimension)
      minScreenDimension = height;
    // ensure that rotations don't leave some atoms off the screen
    // note that this radius is to the furthest outside edge of an atom
    // given the current VDW radius setting. it is currently *not*
    // recalculated when the vdw radius settings are changed
    // leave a very small margin - only 1 on top and 1 on bottom
    if (minScreenDimension > 2)
      minScreenDimension -= 2;
    scaleDefaultPixelsPerAngstrom =
      minScreenDimension / 2 / viewer.getRotationRadius();
    if (perspectiveDepth) {
      cameraZ = (int)(cameraDepth * minScreenDimension);
      float scaleFactor = (cameraZ + minScreenDimension/2) / (float)cameraZ;
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

  public float scaleToScreen(int z, float sizeAngstroms) {
    // all z's are >= 0
    // so the more positive z is, the smaller the screen scale
    float pixelSize = sizeAngstroms * scalePixelsPerAngstrom;
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraZ) / (cameraZ + z);
    return pixelSize;
  }

  public float scaleToPerspective(int z, float sizeAngstroms) {
    return (perspectiveDepth
            ? (sizeAngstroms * cameraZ) / (cameraZ + z)
            : sizeAngstroms);
  }

  public short scaleToScreen(int z, int milliAngstroms) {
    int pixelSize = (int)(milliAngstroms * scalePixelsPerAngstrom / 1000);
    if (perspectiveDepth)
      pixelSize = (pixelSize * cameraZ) / (cameraZ + z);
    return (short)pixelSize;
  }

  /****************************************************************
   TRANSFORMATIONS
  ****************************************************************/

  public final Matrix4f matrixPointTransform = new Matrix4f();
  public final Matrix4f matrixVectorTransform = new Matrix4f();
  private final Point3f point3dScreenTemp = new Point3f();
  private final Point3i point3iScreenTemp = new Point3i();
  private final Matrix4f matrixTemp = new Matrix4f();
  private final Vector3f vectorTemp = new Vector3f();

  public void calcTransformMatrices() {
    calcPointTransformMatrix();
    calcVectorTransformMatrix();
  }

  private void calcPointTransformMatrix() {
    // you absolutely *must* watch the order of these operations
    matrixPointTransform.setIdentity();
    // first, translate the coordinates back to the center
    vectorTemp.set(viewer.getRotationCenter());
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixPointTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixPointTransform.mul(matrixRotate);
    matrixPointTransform.mul(matrixRotate, matrixPointTransform);
    // now shift so that all z coordinates are >= 0
    // this is important for scaling
    vectorTemp.x = 0;
    vectorTemp.y = 0;
    vectorTemp.z = viewer.getRotationRadius();
    matrixTemp.setTranslation(vectorTemp);
    matrixPointTransform.sub(matrixTemp);
    // now scale to screen coordinates
    matrixTemp.set(-scalePixelsPerAngstrom); // invert y & z
    matrixTemp.m00=scalePixelsPerAngstrom; // preserve x
    matrixPointTransform.mul(matrixTemp, matrixPointTransform);
    // note that the image is still centered at 0, 0
    // translations come later (to deal with perspective)
    // and all z coordinates are >= 0
  }

  private void calcVectorTransformMatrix() {
    matrixVectorTransform.setIdentity();
    // first, translate the coordinates back to the center
    vectorTemp.set(viewer.getRotationCenter());
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    matrixVectorTransform.sub(matrixTemp);
    // now, multiply by angular rotations
    // this is *not* the same as  matrixVectorTransform.mul(matrixRotate);
    matrixVectorTransform.mul(matrixRotate, matrixVectorTransform);
    // now scale to screen coordinates
    matrixTemp.set(-scalePixelsPerAngstrom); // invert y & z
    matrixTemp.m00=scalePixelsPerAngstrom; // preserve x
    matrixVectorTransform.mul(matrixTemp, matrixVectorTransform);
  }

  public Matrix4f getUnscaledTransformMatrix() {
    Matrix4f unscaled = new Matrix4f();
    unscaled.setIdentity();
    vectorTemp.set(viewer.getRotationCenter());
    matrixTemp.setZero();
    matrixTemp.setTranslation(vectorTemp);
    unscaled.sub(matrixTemp);
    unscaled.mul(matrixRotate, unscaled);
    return unscaled;
  }

  public Point3i transformPoint(Point3f pointAngstroms) {
    matrixPointTransform.transform(pointAngstroms, point3dScreenTemp);
    int z = (int)(point3dScreenTemp.z + 0.5);
    if (z < 0) {
      System.out.println("WARNING! DANGER! z < 0! transformPoint()");
      z = 0;
    }
    point3iScreenTemp.z = z;
    if (perspectiveDepth) {
      float perspectiveFactor = (float)cameraZ / (cameraZ + z);
      point3dScreenTemp.x *= perspectiveFactor;
      point3dScreenTemp.y *= perspectiveFactor;
    }
    point3iScreenTemp.x = (int)(point3dScreenTemp.x + xTranslation);
    point3iScreenTemp.y = (int)(point3dScreenTemp.y + yTranslation);
    return point3iScreenTemp;
  }

  public void transformVector(Vector3f vectorAngstroms,
                              Vector3f vectorTransformed) {
    matrixVectorTransform.transform(vectorAngstroms, vectorTransformed);
  }

  /*
  public void transformVector(Point3f pointAngstroms,
                              Point3f vectorAngstroms,
                              float scale,
                              Point3i results) {
    point3dScreenTemp.set(vectorAngstroms);
    point3dScreenTemp.scaleAdd(scale, pointAngstroms);
    Point3i pointT = transformPoint(point3dScreenTemp);
    results.set(pointT);
  }
  */

  /****************************************************************
   exports for POV rendering
  ****************************************************************/

  public Matrix4f getPovRotateMatrix() {
    return new Matrix4f(matrixRotate);
  }

  public Matrix4f getPovTranslateMatrix() {
    Matrix4f matrixPovTranslate = new Matrix4f();
    matrixPovTranslate.setIdentity();
    matrixPovTranslate.get(vectorTemp);
    vectorTemp.x = (xTranslation-width/2) / scalePixelsPerAngstrom;
    vectorTemp.y = -(yTranslation-height/2)
      / scalePixelsPerAngstrom; // invert y axis
    vectorTemp.z = 0;
    matrixPovTranslate.set(vectorTemp);
    return matrixPovTranslate;
  }
}
