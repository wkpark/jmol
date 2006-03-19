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
import javax.vecmath.Point3i;

class Draw extends MeshCollection {
 
  final static int MAX_POINTS = 256; // a few extras here
  Point3f[] ptList = new Point3f[MAX_POINTS];
  int[] ptIdentifiers = new int[MAX_POINTS];
  boolean[] reversePoints = new boolean[MAX_POINTS];
  boolean[] useVertices = new boolean[MAX_POINTS];
  BitSet[] ptBitSets = new BitSet[MAX_POINTS];
  Point3f xyz = new Point3f();
  int ipt;
  int nPoints = -1;
  int nbitsets = 0;
  int ncoord = 0;
  int nidentifiers = 0;
  float newScale;
  float length = Float.MAX_VALUE;
  boolean isFixed = false;
  boolean isVisible = true;
  boolean isPerpendicular = false;
  boolean isVertices = false;
  boolean isPlane = false;
  boolean isReversed = false;
  boolean isRotated45 = false;
  boolean isCrossed = false;

  void setProperty(String propertyName, Object value, BitSet bs) {
    // System.out.println("draw "+propertyName+" "+value);

    if ("meshID" == propertyName) {
      nPoints = -1;
      isFixed = isReversed = isRotated45 = isCrossed = false;
      isPlane = isVertices = isPerpendicular = false;
      isVisible = true;
      length = Float.MAX_VALUE;
      //let pass through
    }

    if ("length" == propertyName) {
      length = ((Float) value).floatValue();
      return;
    }

    if ("fixed" == propertyName) {
      isFixed = ((Boolean) value).booleanValue();
      return;
    }

    if ("perp" == propertyName) {
      isPerpendicular = true;
      return;
    }

    if ("plane" == propertyName) {
      isPlane = true;
      return;
    }

    if ("vertices" == propertyName) {
      isVertices = true;
      return;
    }

    if ("reverse" == propertyName) {
      isReversed = true;
      return;
    }

    if ("rotate45" == propertyName) {
      isRotated45 = true;
      return;
    }

    if ("crossed" == propertyName) {
      isCrossed = true;
      return;
    }

    if ("points" == propertyName) {
      ipt = nPoints = ncoord = nbitsets = nidentifiers = 0;
      newScale = ((Integer) value).floatValue() / 100;
      if (newScale == 0)
        newScale = 1;
      return;
    }

    if ("scale" == propertyName) {
      newScale = ((Integer) value).floatValue() / 100;
      if (newScale == 0)
        newScale = 0.01f; // very tiny but still sizable;
      if (currentMesh != null) {
        // no points in this script statement
        currentMesh.scaleDrawing(newScale);
        currentMesh.initialize();
      }
      return;
    }

    if ("identifier" == propertyName) {
      String meshID = (String) value;
      int meshIndex = getMeshIndex(meshID);
      if (meshIndex >= 0) {
        reversePoints[nidentifiers] = isReversed;
        useVertices[nidentifiers] = isVertices;
        ptIdentifiers[nidentifiers] = meshIndex;
        nidentifiers++;
        nPoints++;
        isReversed = isVertices = false;
      } else {
        System.out.println("draw identifier " + value + " not found");
      }
      return;
    }

    if ("coord" == propertyName) {
      ptList[ncoord++] = new Point3f((Point3f) value);
      nPoints++;
      return;
    }
    if ("atomSet" == propertyName) {
      if (viewer.cardinalityOf((BitSet) value) == 0)
        return;
      ptBitSets[nbitsets++] = (BitSet) value;
      nPoints++;
      // System.out.println(nPoints + " " + ptBitSets[nbitsets-1]);
      return;
    }
    if ("set" == propertyName) {
      isValid = setDrawing();
      if (isValid) {
        currentMesh.scaleDrawing(newScale);
        currentMesh.initialize();
        currentMesh.setAxes();
        currentMesh.visible = isVisible;
      }
      nPoints = -1; // for later scaling
      return;
    }
    if ("off" == propertyName) {
      isVisible = false; 
      //let pass through
    }


    super.setProperty(propertyName, value, bs);
  }

  boolean setDrawing() {
    if (currentMesh == null)
      allocMesh(null);
    currentMesh.clear("draw");
    if (nPoints == 0)
      return false;
    int nPoly = 0;
    int modelCount = viewer.getModelCount();
    if (nbitsets == 0 && nidentifiers == 0 || modelCount < 2)
      isFixed = true;
    if (isFixed) {
      currentMesh.setPolygonCount(1);
      currentMesh.ptCenters = null;
      currentMesh.visibilityFlags = null;
      nPoly = setVerticesAndPolygons(-1, nPoly);
    } else {
      currentMesh.setPolygonCount(modelCount);
      currentMesh.ptCenters = new Point3f[modelCount];
      currentMesh.visibilityFlags = new int[modelCount];
      for (int iModel = 0; iModel < modelCount; iModel++) {
        // int n0 = currentMesh.vertexCount;
        nPoly = setVerticesAndPolygons(iModel, nPoly);
        currentMesh.setCenter(iModel);
      }
    }
    currentMesh.setCenter(-1);
    return true;
  }

  private int setVerticesAndPolygons(int iModel, int nPoly) {
    nPoints = ncoord;
    // [x,y,z] points are already defined in ptList
    if (iModel < 0) {
      // add in [drawID] references as overall centers
      for (int i = 0; i < nidentifiers; i++) {
        Mesh m = meshes[ptIdentifiers[i]];
        if (isPlane || isPerpendicular || useVertices[i]) {
          if (reversePoints[i]) {
            for (ipt = m.drawVertexCount; --ipt >= 0;)
              addPoint(m.vertices[ipt]);
          } else {
            for (ipt = 0; ipt < m.drawVertexCount; ipt++) {
              System.out.println(nPoints +" "+m.meshID+ " count="+m.drawVertexCount + " length=" + m.vertices.length);
              addPoint(m.vertices[ipt]);

            }
          }
        } else {
          addPoint(m.ptCenter);
        }
      }
      // add in (atom set) references as overall centers
      for (int i = 0; i < nbitsets; i++)
        addPoint(viewer.getAtomSetCenter(ptBitSets[i]));
    } else {
      // [drawID] references may be fixed or not
      for (int i = 0; i < nidentifiers; i++) {
        if (meshes[ptIdentifiers[i]].ptCenters == null
            || meshes[ptIdentifiers[i]].ptCenters[iModel] == null) {
          addPoint(meshes[ptIdentifiers[i]].ptCenter);
        } else {
          addPoint(meshes[ptIdentifiers[i]].ptCenters[iModel]);
        }
      }
      // (atom set) references must be filtered for relevant model
      // note that if a model doesn't have a relevant point, one may
      // get a line instead of a plane, a point instead of a line, etc.
      BitSet bsModel = viewer.getModelAtomBitSet(iModel);
      for (int i = 0; i < nbitsets; i++) {
        BitSet bs = (BitSet) ptBitSets[i].clone();
        bs.and(bsModel);
        if (viewer.cardinalityOf(bs) > 0) {
          addPoint(viewer.getAtomSetCenter(bs));
        }
      }
    }
    if (nPoints ==4 && isCrossed) {
      Point3f pt = new Point3f(ptList[1]);
      ptList[1].set(ptList[2]);
      ptList[2].set(pt);
    }

    return currentMesh.setPolygon(ptList, nPoints, nPoly, isPlane,
        isPerpendicular, isRotated45, length);
  }

  void addPoint(Point3f newPt) {
    ptList[nPoints++] = new Point3f(newPt); 
    if (nPoints > MAX_POINTS)
      nPoints = MAX_POINTS;
  }
  
  void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed note
     * that this is NOT done with atoms and bonds, because they have mads. When
     * you say "frame 0" it is just turning on all the mads.
     */
    int modelCount = viewer.getModelCount();
    for (int i = meshCount; --i >= 0;) {
      if (meshes[i].visibilityFlags == null)
        continue;
      for (int iModel = modelCount; --iModel >= 0;)
        meshes[i].visibilityFlags[iModel] = (bs.get(iModel) ? 1 : 0);
    }
  }

  final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 5 * 5;

  void checkObjectClicked(int x, int y, boolean isShiftDown) {
    int modelCount = viewer.getModelCount();
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    int nearestModel = 0;
    int nearestVertex = 0;
    Mesh mesh = null;
    Mesh pickedMesh = null;
    for (int i = meshCount; --i >= 0;) {
      mesh = meshes[i];
      if (mesh.drawVertexCount == 2) {
        for (int iModel = modelCount; --iModel >= 0;) {
          if (mesh.visibilityFlags != null && mesh.visibilityFlags[iModel] == 0)
            continue;
          for (int iVertex = mesh.polygonIndexes[iModel].length; --iVertex >= 0;) {
            int d2 = coordinateInRange(x, y,
                mesh.vertices[mesh.polygonIndexes[iModel][iVertex]], dmin2);
            if (d2 >= 0) {
              pickedMesh = mesh;
              dmin2 = d2;
              nearestModel = iModel;
              nearestVertex = iVertex;
            }
          }
        }
      }
    }
    if (pickedMesh != null) {
      if (nearestVertex == 0) {
        viewer.startSpinningAxis(
            pickedMesh.vertices[pickedMesh.polygonIndexes[nearestModel][0]],
            pickedMesh.vertices[pickedMesh.polygonIndexes[nearestModel][1]],
            isShiftDown);
      } else {
        viewer.startSpinningAxis(
            pickedMesh.vertices[pickedMesh.polygonIndexes[nearestModel][1]],
            pickedMesh.vertices[pickedMesh.polygonIndexes[nearestModel][0]],
            isShiftDown);
      }
      return;
    }
  }

  int coordinateInRange(int x, int y, Point3f vertex, int dmin2) {
    int d2 = dmin2;
    Point3i ptXY = viewer.transformPoint(vertex);
    d2 = (x - ptXY.x) * (x - ptXY.x) + (y - ptXY.y) * (y - ptXY.y);
    return (d2 < dmin2 ? d2 : -1);
  }
}
