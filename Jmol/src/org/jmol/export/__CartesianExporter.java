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

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;

abstract public class __CartesianExporter extends __Exporter {

  public __CartesianExporter() {
    isCartesianExport = true;
  }

  private float untransformData(Point3f ptA, Point3f ptB, int madBond,
                                boolean isCartesian) {
    if (isCartesian) {
      // really first order -- but actual coord
      tempP1.set(ptA);
      tempP2.set(ptB);
    } else {
      viewer.unTransformPoint(ptA, tempP1);
      viewer.unTransformPoint(ptB, tempP2);
    }
    if (madBond < 20)
      madBond = 20;
    float radius = madBond / 2000f;
    return radius;
  }

  // these are elaborated in IDTF, MAYA, VRML, or X3D:

  abstract protected void outputCircle(Point3f pt1, Point3f pt2, float radius,
                                       short colix, boolean doFill);

  abstract protected void outputCone(Point3f ptBase, Point3f ptTip,
                                     float radius, short colix);

  abstract protected void outputCylinder(Point3f pt1, Point3f pt2,
                                         short colix1, byte endcaps,
                                         float radius);

  abstract protected void outputEllipsoid(Point3f center, Point3f[] points,
                                          short colix);

  abstract protected void outputSphere(Point3f ptAtom2, float f, short colix);

  abstract protected void outputTextPixel(Point3f pt, int argb);

  abstract protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3,
                                         short colix);

  // these are called by Export3D or a generator:

  void drawAtom(Atom atom, short colix) {
    float radius = atom.getMadAtom() / 2000f;
    outputSphere(atom, radius, colix);
  }

  void drawCircle(int x, int y, int z, int diameter, short colix, boolean doFill) {
    // draw circle
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    float radius = viewer.unscaleToScreen(z, diameter) / 2;
    tempP3.set(x, y, z + 1);
    viewer.unTransformPoint(tempP3, tempP3);
    outputCircle(tempP1, tempP3, radius, colix, doFill);
  }

  void drawPixel(short colix, int x, int y, int z) {
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    outputSphere(tempP1, 0.02f, colix);
  }

  void drawTextPixel(int argb, int x, int y, int z) {
    // text only
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    outputTextPixel(tempP1, argb);
  }

  void fillCone(short colix, byte endcap, int diameter, Point3f screenBase,
                Point3f screenTip) {
    viewer.unTransformPoint(screenBase, tempP1);
    viewer.unTransformPoint(screenTip, tempP2);
    float radius = viewer.unscaleToScreen((int) screenBase.z, diameter) / 2;
    if (radius < 0.05f)
      radius = 0.05f;
    outputCone(tempP1, tempP2, radius, colix);
  }

  void fillCylinder(Point3f ptA, Point3f ptB, short colix1, short colix2,
                    byte endcaps, int diameter, int bondOrder) {
    float radius = untransformData(ptA, ptB, diameter, bondOrder == -1);
    if (colix1 == colix2) {
      outputCylinder(tempP1, tempP2, colix1, endcaps, radius);
    } else {
      tempV2.set(tempP2);
      tempV2.add(tempP1);
      tempV2.scale(0.5f);
      tempP3.set(tempV2);
      outputCylinder(tempP1, tempP3, colix1, Graphics3D.ENDCAPS_FLAT, radius);
      outputCylinder(tempP3, tempP2, colix2, Graphics3D.ENDCAPS_FLAT, radius);
      if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
        outputSphere(tempP1, radius * 1.01f, colix1);
        outputSphere(tempP2, radius * 1.01f, colix2);
      }
    }
  }

  void fillCylinder(short colix, byte endcaps, int diameter, Point3f screenA,
                    Point3f screenB) {
    float radius = untransformData(screenA, screenB, diameter, false);
    outputCylinder(tempP1, tempP2, colix, endcaps, radius);
  }

  void fillEllipsoid(Point3f center, Point3f[] points, short colix, int x,
                     int y, int z, int diameter, Matrix3f toEllipsoidal,
                     double[] coef, Matrix4f deriv, Point3i[] octantPoints) {
    outputEllipsoid(center, points, colix);
  }

  void fillScreenedCircle(short colix, int diameter, int x, int y, int z) {
    drawCircle(x, y, z, diameter, colix, false);
    drawCircle(x, y, z, diameter, Graphics3D.getColixTranslucent(colix, true,
        0.5f), true);
  }

  void fillSphere(short colix, int diameter, Point3f pt) {
    viewer.unTransformPoint(pt, tempP1);
    outputSphere(tempP1, viewer.unscaleToScreen((int) pt.z, diameter) / 2,
        colix);
  }

  void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    outputTriangle(ptA, ptB, ptC, colix);
  }

  void plotImage(int x, int y, int z, Image image, short bgcolix, int width,
                 int height) {
    // note applicable to Cartesian Exporter
  }

  void plotText(int x, int y, int z, short colix, String text, Font3D font3d) {
    // over-written in VRML and X3D
    // trick here is that we use Jmol's standard g3d package to construct
    // the bitmap, but then output to jmolRenderer, which returns control
    // here via drawPixel.
    if (z < 3) {
      viewer.transformPoint(center, tempP3);
      z = (int) tempP3.z;
    }
    g3d.plotText(x, y, z, g3d.getColorArgbOrGray(colix), text, font3d,
        jmolRenderer);
  }

}
