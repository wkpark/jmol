/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 15:41:42 -0500 (Fri, 18 May 2007) $
 * $Revision: 7752 $

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

package org.jmol.export;

import java.awt.Image;
import java.io.IOException;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;

/*
 * for PovRay and related ray tracers that use screen coordinates
 * 
 * 
 */

abstract class __RayTracerExporter extends __Exporter {

  protected int nBytes;
  protected boolean isSlabEnabled;
  protected int nText;
  protected int nImage;
  protected float zoom;
  protected int minScreenDimension;
  

  public __RayTracerExporter() {
    isCartesianExport = false;
  }

  protected void output(String data) {
    nBytes += data.length();
    try {
      if (bw == null)
        output.append(data);
      else
        bw.write(data);
    } catch (IOException e) {
      // ignore for now
    }
  }

  abstract protected void outputCircle(int x, int y, int z, float radius, short colix,
                                       boolean doFill);

  abstract protected void outputComment(String comment);

  abstract protected void outputCylinder(Point3f screenA, Point3f screenB, float radius,
                                         short colix, boolean withCaps);
             
  abstract protected void outputCylinderConical(Point3f screenA,
                                                Point3f screenB, float radius1,
                                                float radius2, short colix);

  abstract protected void outputEllipsoid(double[] coef, short colix);
  
  abstract protected void outputSphere(float x, float y, float z, float radius,
                                    short colix);
  
  abstract protected void outputTextPixel(int x, int y, int z, int argb);

  abstract protected void outputTriangle(Point3f ptA, Point3f ptB, Point3f ptC, short colix);

  abstract protected void outputCone(Point3f screenBase, Point3f screenTip, float radius,
                                     short colix);

  abstract protected void outputCircleScreened(int x, int y, int z, float radius, short colix);
  
  protected Point3f getScreenNormal(Point3f pt, Vector3f normal) {
    if (Float.isNaN(normal.x)) {
      tempP3.set(0, 0, 0);
      return tempP3;
    }
    tempP1.set(pt);
    tempP1.add(normal);
    viewer.transformPoint(pt, tempP2);
    viewer.transformPoint(tempP1, tempP3);
    tempP3.sub(tempP2);
    return tempP3;
  }

  protected void getHeader() {
    nBytes = 0;
    isSlabEnabled = viewer.getSlabEnabled();
    zoom = viewer.getRotationRadius() * 2;
    zoom *= 1.1f; // for some reason I need a little more margin
    zoom /= viewer.getZoomPercentFloat() / 100f;
    minScreenDimension = Math.min(screenWidth, screenHeight);
    // more specific next in PovRay and Tachyon
  }

  void drawAtom(Atom atom, short colix) {
    outputSphere(atom.screenX, atom.screenY, atom.screenZ,
        atom.screenDiameter / 2f, colix);
  }

  void drawCircle(int x, int y, int z,
                         int diameter, short colix, boolean doFill) {
    //draw circle
    float radius = diameter / 2f;
    outputCircle(x, y, z, radius, colix, doFill);
  }

  void drawPixel(short colix, int x, int y, int z) {
    //measures, meshRibbon
    outputSphere(x, y, z, 0.75f, colix);
  }

  void drawTextPixel(int argb, int x, int y, int z) {
    outputTextPixel(x, y, z, argb);
  }
    
  void fillCone(short colix, byte endcap, int diameter, Point3f screenBase,
                Point3f screenTip) {
    outputCone(screenBase, screenTip, diameter / 2f, colix);
  }

  void fillCylinder(Point3f screenA, Point3f screenB, short colix1,
                           short colix2, byte endcaps, int madBond,
                           int bondOrder) {
    // from drawBond and fillCylinder here
    if (colix1 == colix2) {
      fillConicalCylinder(screenA, screenB, madBond, colix1, endcaps);
    } else {
      tempV2.set(screenB);
      tempV2.add(screenA);
      tempV2.scale(0.5f);
      tempP1.set(tempV2);
      fillConicalCylinder(screenA, tempP1, madBond, colix1, endcaps);
      fillConicalCylinder(tempP1, screenB, madBond, colix2, endcaps);
    }
    if (endcaps != Graphics3D.ENDCAPS_SPHERICAL)
      return;
    
    float radius = viewer.scaleToScreen((int) screenA.z, madBond / 2);
    outputSphere(screenA.x, screenA.y, screenA.z, radius, colix1);
    radius = viewer.scaleToScreen((int) screenB.z, madBond / 2);
    outputSphere(screenB.x, screenB.y, screenB.z, radius, colix2);

  }

  protected void fillConicalCylinder(Point3f screenA, Point3f screenB,
                                    int madBond, short colix, byte endcaps) {
    float radius1 = viewer.scaleToScreen((int) screenA.z, madBond) / 2f;
    if (screenA.distance(screenB) == 0) {
      outputSphere(screenA.x, screenA.y, screenA.z, radius1, colix);
      return;
    }
    float radius2 = viewer.scaleToScreen((int) screenB.z, madBond) / 2f;
    outputCylinderConical(screenA, screenB, radius1, radius2, colix);
  }

  void fillCylinder(short colix, byte endcaps, int diameter,
                           Point3f screenA, Point3f screenB) {
    float radius = diameter / 2f;
    if (screenA.distance(screenB) == 0) {
      outputSphere(screenA.x, screenA.y, screenA.z, radius, colix);
      return;
    }
    outputCylinder(screenA, screenB, radius, colix, endcaps == Graphics3D.ENDCAPS_FLAT);
    if (endcaps != Graphics3D.ENDCAPS_SPHERICAL)
      return;
    outputSphere(screenA.x, screenA.y, screenA.z, radius, colix);
    outputSphere(screenB.x, screenB.y, screenB.z, radius, colix);

  }

  void fillSphere(short colix, int diameter, Point3f pt) {
    outputSphere(pt.x, pt.y, pt.z, diameter / 2f, colix);
  }
  
  void fillScreenedCircle(short colix, int diameter, int x, int y, int z) {
    outputCircleScreened(x, y, z, diameter / 2f, colix);
  }

  void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    outputTriangle(ptA, ptB, ptC, colix);
  }

  void plotImage(int x, int y, int z, Image image, short bgcolix, int width,
                 int height) {
    outputComment("start image " + (++nImage));
    g3d.plotImage(x, y, z, image, jmolRenderer, bgcolix, width, height);
    outputComment("end image " + nImage);
  }

  void plotText(int x, int y, int z, short colix, String text, Font3D font3d) {
    // trick here is that we use Jmol's standard g3d package to construct
    // the bitmap, but then output to jmolRenderer, which returns control
    // here via drawPixel.
    outputComment("start text " + (++nText) + ": " + text);
    g3d.plotText(x, y, z, g3d.getColorArgbOrGray(colix), text, font3d, jmolRenderer);
    outputComment("end text " + nText + ": " + text);
  }

  void fillEllipsoid(Point3f center, Point3f[] points, short colix, int x,
                       int y, int z, int diameter, Matrix3f toEllipsoidal,
                       double[] coef, Matrix4f deriv, Point3i[] octantPoints) {
    outputEllipsoid(coef, colix);    
  }

}
