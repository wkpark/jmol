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


import org.jmol.modelset.Atom;
import org.jmol.util.GData;

import javajs.vec.Matrix3f;
import javajs.vec.Matrix4f;
import javajs.vec.P3;
import javajs.vec.P3i;
import javajs.vec.V3;

/*
 * for PovRay and related ray tracers that use screen coordinates
 * 
 * 
 */

abstract class __RayTracerExporter extends ___Exporter {

  protected boolean isSlabEnabled;
  protected int minScreenDimension;
  
  public __RayTracerExporter() {
    exportType = GData.EXPORT_RAYTRACER;
    lineWidthMad = 2;
  }

  @Override
  protected void outputVertex(P3 pt, P3 offset) {
    setTempVertex(pt, offset, tempP1);
    viewer.transformPt3f(tempP1, tempP1);
    output(tempP1);
  }

  abstract protected void outputCircle(int x, int y, int z, float radius, short colix,
                                       boolean doFill);

  abstract protected void outputCylinder(P3 screenA, P3 screenB, float radius,
                                         short colix, boolean withCaps);
             
  abstract protected void outputCylinderConical(P3 screenA,
                                                P3 screenB, float radius1,
                                                float radius2, short colix);

  abstract protected void outputEllipsoid(P3 center, float radius, double[] coef, short colix);
  
  abstract protected void outputSphere(float x, float y, float z, float radius,
                                    short colix);
  
  abstract protected void outputTextPixel(int x, int y, int z, int argb);

  abstract protected void outputTriangle(P3 ptA, P3 ptB, P3 ptC, short colix);

  abstract protected void outputCone(P3 screenBase, P3 screenTip, float radius,
                                     short colix, boolean isBarb);

  protected P3 getScreenNormal(P3 pt, V3 normal, float factor) {
    if (Float.isNaN(normal.x)) {
      tempP3.set(0, 0, 0);
      return tempP3;
    }
    tempP1.setT(pt);
    tempP1.add(normal);
    viewer.transformPt3f(pt, tempP2);
    viewer.transformPt3f(tempP1, tempP3);
    tempP3.sub(tempP2);
    tempP3.scale(factor);
    return tempP3;
  }

  protected void initVars() {
    isSlabEnabled = viewer.getSlabEnabled();
    minScreenDimension = Math.min(screenWidth, screenHeight);
  }

  // called by Export3D:
  
  @Override
  void drawAtom(Atom atom) {
    outputSphere(atom.screenX, atom.screenY, atom.screenZ,
        atom.screenDiameter / 2f, atom.getColix());
  }

  @Override
  void drawCircle(int x, int y, int z,
                         int diameter, short colix, boolean doFill) {
    //draw circle
    float radius = diameter / 2f;
    outputCircle(x, y, z, radius, colix, doFill);
  }

  @Override
  boolean drawEllipse(P3 ptAtom, P3 ptX, P3 ptY,
                      short colix, boolean doFill) {
    // IDTF only for now
    return false;
  }

  @Override
  void drawPixel(short colix, int x, int y, int z, int scale) {
    //measures, meshRibbon, dots
    outputSphere(x, y, z, 0.75f * scale, colix);
  }

  @Override
  void drawTextPixel(int argb, int x, int y, int z) {
    outputTextPixel(x, y, z, argb);
  }
    
  @Override
  void fillConeScreen(short colix, byte endcap, int screenDiameter, P3 screenBase,
                P3 screenTip, boolean isBarb) {
    outputCone(screenBase, screenTip, screenDiameter / 2f, colix, isBarb);
  }

  @Override
  void drawCylinder(P3 screenA, P3 screenB, short colix1,
                           short colix2, byte endcaps, int madBond,
                           int bondOrder) {
    // from drawBond and fillCylinder here
    if (colix1 == colix2) {
      fillConicalCylinder(screenA, screenB, madBond, colix1, endcaps);
    } else {
      tempV2.setT(screenB);
      tempV2.add(screenA);
      tempV2.scale(0.5f);
      tempP1.setT(tempV2);
      fillConicalCylinder(screenA, tempP1, madBond, colix1, endcaps);
      fillConicalCylinder(tempP1, screenB, madBond, colix2, endcaps);
    }
    if (endcaps != GData.ENDCAPS_SPHERICAL)
      return;
    
    float radius = viewer.scaleToScreen((int) screenA.z, madBond) / 2f;
    if (radius <= 1)
      return;
    outputSphere(screenA.x, screenA.y, screenA.z, radius, colix1);
    radius = viewer.scaleToScreen((int) screenB.z, madBond) / 2f;
    if (radius <= 1)
      return;
    outputSphere(screenB.x, screenB.y, screenB.z, radius, colix2);

  }

  /**
   * 
   * @param screenA
   * @param screenB
   * @param madBond
   * @param colix
   * @param endcaps
   */
  protected void fillConicalCylinder(P3 screenA, P3 screenB,
                                    int madBond, short colix, 
                                    byte endcaps) {
    float radius1 = viewer.scaleToScreen((int) screenA.z, madBond) / 2f;
    if (radius1 == 0)
      return;
    if (radius1 < 1)
      radius1 = 1;
    if (screenA.distance(screenB) == 0) {
      outputSphere(screenA.x, screenA.y, screenA.z, radius1, colix);
      return;
    }
    float radius2 = viewer.scaleToScreen((int) screenB.z, madBond) / 2f;
    if (radius2 == 0)
      return;
    if (radius2 < 1)
      radius2 = 1;
    outputCylinderConical(screenA, screenB, radius1, radius2, colix);
  }

  @Override
  void fillCylinderScreenMad(short colix, byte endcaps, int diameter, 
                               P3 screenA, P3 screenB) {
    float radius = diameter / 2f;
    if (radius == 0)
      return;
    if (radius < 1)
      radius = 1;
    if (screenA.distance(screenB) == 0) {
      outputSphere(screenA.x, screenA.y, screenA.z, radius, colix);
      return;
    }
    outputCylinder(screenA, screenB, radius, colix, endcaps == GData.ENDCAPS_FLAT);
    if (endcaps != GData.ENDCAPS_SPHERICAL || radius <= 1)
      return;
    outputSphere(screenA.x, screenA.y, screenA.z, radius, colix);
    outputSphere(screenB.x, screenB.y, screenB.z, radius, colix);

  }

  @Override
  void fillCylinderScreen(short colix, byte endcaps, int screenDiameter, P3 screenA, 
                                 P3 screenB) {
          // vectors, polyhedra
    fillCylinderScreenMad(colix, endcaps, screenDiameter, screenA, screenB);
  }

  @Override
  void fillSphere(short colix, int diameter, P3 pt) {
    outputSphere(pt.x, pt.y, pt.z, diameter / 2f, colix);
  }
  
  @Override
  protected void fillTriangle(short colix, P3 ptA, P3 ptB, P3 ptC, boolean twoSided) {
    outputTriangle(ptA, ptB, ptC, colix);
  }

  @Override
  void fillEllipsoid(P3 center, P3[] points, short colix, int x,
                       int y, int z, int diameter, Matrix3f toEllipsoidal,
                       double[] coef, Matrix4f deriv, P3i[] octantPoints) {
    float radius = diameter / 2f;
    if (radius == 0)
      return;
    if (radius < 1)
      radius = 1;
    outputEllipsoid(center, radius, coef, colix); 
  }

}
