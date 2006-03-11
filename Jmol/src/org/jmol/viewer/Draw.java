/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2006-02-25 17:19:14 -0600 (Sat, 25 Feb 2006) $
 * $Revision: 4529 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import java.util.BitSet;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

class Draw extends MeshCollection {

  Point3f[] ptList = new Point3f[128];
  Point3f xyz = new Point3f();
  int ipt;
  int npoints = -1;
  float newScale;
  
  void setProperty(String propertyName, Object value, BitSet bs) {
    System.out.println("draw "+propertyName+" "+value);

    if ("points" == propertyName) {
      ipt = npoints = 0;
      newScale = ((Integer)value).floatValue()/100;
      if (newScale == 0)
        newScale = 1;
      return;
    }
    
    if ("scale" == propertyName) {
      newScale = ((Integer)value).floatValue()/100;
      if (newScale == 0)
        return;
      if (currentMesh != null) {
        //no points in this script statement
        scaleDrawing();
        currentMesh.initialize();
      }
      return;
    }
    
    if ("identifier" == propertyName) {
      int meshIndex = getMeshIndex((String)value);
      if (meshIndex >= 0) {
        System.out.println(npoints+" lt? " + ptList.length);
        System.out.println(meshIndex+" lt? " + meshes.length);
        ptList[npoints++] = new Point3f(meshes[meshIndex].ptCenter);
        System.out.println("OK");
        System.out.println(npoints + " " + ptList[npoints-1]);
      } else {
        System.out.println("draw identifier " + value + " not found");
      }
      return;
    }
    
    if ("coord" == propertyName) {
      float x = ((Float)value).floatValue();
      if (ipt == 0) {
        xyz.x = x; 
      } else if (ipt == 1) {
        xyz.y = x; 
      } else if (ipt == 2) {
        xyz.z = x; 
        ptList[npoints++] = new Point3f(xyz);
        System.out.println(npoints + " " + ptList[npoints-1]);
          }
      ipt = (ipt + 1) % 3;
      return;
    }
    if ("atomSet" == propertyName) {
      ptList[npoints++] = new Point3f((Point3f)value);
      System.out.println(npoints + " " + ptList[npoints-1]);
      return;
    } 
    if ("set" == propertyName) {
      isValid = setDrawing();
      if(isValid) {
        scaleDrawing();
        currentMesh.initialize();
        currentMesh.visible = true;
      }
      npoints = -1; //for later scaling
      return;
    }
    if ("meshID" == propertyName) 
      npoints = -1;
    
    super.setProperty(propertyName, value, bs);
  }

  boolean setDrawing() {
    if (currentMesh == null)
      allocMesh(null);
    currentMesh.clear("draw");
    if (npoints == 0)
      return false;
    for (int i = 0; i < npoints; i++)
      currentMesh.addVertexCopy(ptList[i]);
    currentMesh.setPolygonCount(1);
    int n = currentMesh.vertexCount;
    currentMesh.ptCenter = centerOf(currentMesh.vertices, n);    
    int nPoints = n;
    if (n < 3)
      n = 3;
    currentMesh.polygonIndexes[0] = new int[n];
    for (int i = 0; i < n; i++)
      currentMesh.polygonIndexes[0][i] = (i >= nPoints ? nPoints - 1 : i);
    return true;
  }

  void scaleDrawing() {
    if (currentMesh == null || newScale == 0 || 
        currentMesh.vertexCount == 0 || currentMesh.scale == newScale)
      return;
    Vector3f diff = new Vector3f();
    float f = newScale / currentMesh.scale;
    System.out.println("scaledrawing " +f);    
    currentMesh.scale = newScale;
    for (int i = currentMesh.vertexCount; --i >= 0;) {
      diff.sub(currentMesh.vertices[i], currentMesh.ptCenter);
      diff.scale(f);
      diff.add(currentMesh.ptCenter);
      currentMesh.vertices[i].set(diff);
    }
  }
  
  Point3f centerOf(Point3f[] points, int nPoints) {
    Point3f center = new Point3f(0, 0, 0);
    if (nPoints == 0) return center;
    for (int i = nPoints; --i >= 0;) {
      center.add(points[i]);
    }
    center.scale(1.0F / nPoints);
    return center;
  }
}
