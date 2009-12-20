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

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/*
 * copied from _PovrayExporter for now -- needs full revision -- 12/2009
 * 
 */

public class _TachyonExporter extends __RayTracerExporter {

  public void getHeader() {
    super.getHeader();
    //TODO
  }

  public void getFooter() {
    //TODO
  }

  protected void outputCircle(int x, int y, int z, float radius, short colix,
                              boolean doFill) {
    //TODO
  }

  protected void outputCircleScreened(int x, int y, int z, float radius, short colix) {
    //TODO
  }

  protected void outputCylinderCapped(Point3f screenA, Point3f screenB,
                                      float radius, short colix, byte endcaps) {
    // TODO
  }

  protected void outputCylinderConical(Point3f screenA, Point3f screenB,
                                       float radius1, float radius2, short colix) {
    //TODO
  }

  protected void outputComment(String comment) {
    //TODO
  }

  protected void outputCone(Point3f screenBase, Point3f screenTip, float radius,
                            short colix) {
    //TODO
  }

  protected void outputEllipsoid(double[] coef, short colix) {
    //TODO
  }

  protected void outputIsosurface(Point3f[] vertices, Vector3f[] normals,
                                  short[] colixes, int[][] indices,
                                  short[] polygonColixes, int nVertices,
                                  int nPolygons, BitSet bsFaces,
                                  int faceVertexMax, short colix) {
    // TODO
    
  }

  protected void outputSphere(Point3f pt, float radius, short colix) {
    //TODO
  }

  protected void outputSphere(float x, float y, float z, float radius,
                                  short colix) {
    //TODO
  }

  protected void outputTextPixel(int x, int y, int z, int argb) {
    //TODO
  }
  
  protected void outputTriangle(Point3f ptA, Point3f ptB, Point3f ptC, short colix) {
    //TODO
  }

}
