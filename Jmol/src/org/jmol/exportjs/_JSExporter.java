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

package org.jmol.exportjs;


import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.util.J2SRequireImport;

@J2SRequireImport( { org.jmol.exportjs.___Exporter.class,
    org.jmol.exportjs.__CartesianExporter.class,
    org.jmol.exportjs.Export3D.class })
public class _JSExporter extends __CartesianExporter {

  private UseTable useTable;

  public _JSExporter() {
    useTable = new UseTable();
  }

  private Map<String, Boolean> htSpheresRendered = new Hashtable<String, Boolean>();

  private Map<String, Object[]> htObjects = new Hashtable<String, Object[]>();

  private void jsSphere(String id, boolean isNew, Point3f pt, Object[] o) {
    // implemented in JavaScript only
    System.out.println(id + " " + isNew + " " + pt + " " + o);
  }

  private void jsCylinder(String id, boolean isNew, Point3f pt1, Point3f pt2,
                           Object[] o) {
    // implemented in JavaScript only
    System.out.println(id + " " + isNew + " " + pt1 + " " + pt2 + " " + o);
  }

  private void addJmolTriangle(String id, Object o) {
    System.out.println(id + " " + o);    
  }

  @Override
  protected void outputSphere(Point3f ptCenter, float radius, short colix,
                              boolean checkRadius) {
    int iRad = (int) (radius * 100);
    String check = round(ptCenter) + (checkRadius ? " " + iRad : "");
    if (htSpheresRendered.get(check) != null)
      return;
    htSpheresRendered.put(check, Boolean.TRUE);
    boolean found = useTable.getDef("S" + colix + "_" + iRad, ret);
    Object[] o;
    if (found)
      o = htObjects.get(ret[0]);
    else
      htObjects.put(ret[0], o = new Object[] { getColor(colix),
          Float.valueOf(radius) });
    jsSphere(ret[0], !found, ptCenter, o);
  }

  private String[] ret = new String[1];

  @Override
  protected boolean outputCylinder(Point3f ptCenter, Point3f pt1, Point3f pt2,
                                   short colix, byte endcaps, float radius,
                                   Point3f ptX, Point3f ptY, boolean checkRadius) {
    // ptX and ptY are ellipse major and minor axes
    // not implemented yet
    if (ptX != null)
      return false;
    float length = pt1.distance(pt2);
    boolean found = useTable.getDef("C" + colix + "_" + (int) (length * 100)
        + "_" + radius + "_" + endcaps, ret);
    Object[] o;
    if (found)
      o = htObjects.get(ret[0]);
    else
      htObjects.put(ret[0], o = new Object[] { getColor(colix),
          new Float(length), new Float(radius) });
    jsCylinder(ret[0], !found, pt1, pt2, o);
    return true;
  }

  @Override
  protected void outputCircle(Point3f pt1, Point3f pt2, float radius,
                              short colix, boolean doFill) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void outputCone(Point3f ptBase, Point3f ptTip, float radius,
                            short colix) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void outputEllipsoid(Point3f center, Point3f[] points, short colix) {
    // TODO Auto-generated method stub

  }

  private Integer getColor(short colix) {
    return Integer.valueOf(g3d.getColorArgbOrGray(colix));
  }

  @Override
  protected void outputSurface(Point3f[] vertices, Vector3f[] normals,
                               short[] colixes, int[][] indices,
                               short[] polygonColixes,
                               int nVertices, int nPolygons, int nFaces, BitSet bsPolygons,
                               int faceVertexMax, short colix,
                               List<Short> colorList, Map<Short, Integer> htColixes, Point3f offset) {
    //TODO OK, this is a major task.
  }
  
  @Override
  protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3,
                                short colix) {
    // an isolated triangle, with a face-based normal
    // polyhedra, nucleic acid bases, drawn triangles, etc.
    Object o = new Object[] { getColor(colix), pt1, pt2, pt3};
    addJmolTriangle(ret[0], o);
  }

  @Override
  protected void output(Tuple3f pt) {
    // TODO Auto-generated method stub

  }

  @Override
  protected void outputHeader() {
    // implemented in JavaScript only
  }

  @Override
  protected void outputFooter() {
    // implemented in JavaScript only
  }

}


