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
import javax.vecmath.Vector3f;

import org.jmol.util.Logger;

class Draw extends MeshCollection {

  // bob hanson hansonr@stolaf.edu 3/2006

  final static int MAX_POINTS = 256; // a few extras here
  Point3f[] ptList = new Point3f[MAX_POINTS];
  int[] ptIdentifiers = new int[MAX_POINTS];
  boolean[] reversePoints = new boolean[MAX_POINTS];
  boolean[] useVertices = new boolean[MAX_POINTS];
  BitSet[] ptBitSets = new BitSet[MAX_POINTS];
  BitSet bsAllAtoms = new BitSet();
  
  Vector3f offset = new Vector3f();
  Point3f xyz = new Point3f();
  int ipt;
  int nPoints;
  int nbitsets;
  int ncoord;
  int nidentifiers;
  float newScale;
  float length;
  boolean isCurve;
  boolean isArrow;
  boolean isCircle;
  boolean isFixed;
  boolean isVisible;
  boolean isPerpendicular;
  boolean isVertices;
  boolean isPlane;
  boolean isReversed;
  boolean isRotated45;
  boolean isCrossed;
  boolean isValid;

  void setProperty(String propertyName, Object value, BitSet bs) {
    Logger.debug("draw " + propertyName + " " + value);

    if ("init" == propertyName) {
      nPoints = -1;
      ipt = ncoord = nbitsets = nidentifiers = 0;
      isFixed = isReversed = isRotated45 = isCrossed = false;
      isCurve = isArrow = isPlane = isVertices = isPerpendicular = false;
      isVisible = isValid = true;
      length = Float.MAX_VALUE;
      offset = new Vector3f();
      super.setProperty("thisID", null, null);
      return;
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

    if ("curve" == propertyName) {
      isCurve = true;
      return;
    }

    if ("arrow" == propertyName) {
      isArrow = true;
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
      nPoints = 0;
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
        scaleDrawing(currentMesh, newScale);
        currentMesh.initialize();
      }
      return;
    }

    if ("identifier" == propertyName) {
      String thisID = (String) value;
      int meshIndex = getIndexFromName(thisID);
      if (meshIndex >= 0) {
        reversePoints[nidentifiers] = isReversed;
        useVertices[nidentifiers] = isVertices;
        ptIdentifiers[nidentifiers] = meshIndex;
        nidentifiers++;
        nPoints++;
        isReversed = isVertices = false;
      } else {
        Logger.error("draw identifier " + value + " not found");
        isValid = false;
      }
      return;
    }

    if ("coord" == propertyName) {
      if (ncoord == MAX_POINTS)
        return;
      ptList[ncoord++] = new Point3f((Point3f) value);
      nPoints++;
      return;
    }

    if ("offset" == propertyName) {
      offset = new Vector3f((Point3f) value);
      return;
    }

    if ("atomSet" == propertyName) {
      if (viewer.cardinalityOf((BitSet) value) == 0)
        return;
      ptBitSets[nbitsets++] = (BitSet) value;
      bsAllAtoms.or((BitSet) value);
      nPoints++;
      return;
    }
    if ("set" == propertyName) {
      if (currentMesh == null)
        allocMesh(null);
      currentMesh.isValid = (isValid ? setDrawing() : false);
      if (currentMesh.isValid) {
        scaleDrawing(currentMesh, newScale);
        currentMesh.initialize();
        currentMesh.setAxes();
        currentMesh.drawOffset = offset;
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

  Object getProperty(String property, int index) {
    if (property == "command")
      return getDrawCommand(currentMesh);
    return super.getProperty(property, index);
  }

  boolean setDrawing() {
    if (currentMesh == null)
      allocMesh(null);
    currentMesh.clear("draw");
    if (nPoints == 0)
      return false;
    int nPoly = 0;
    int modelCount = viewer.getModelCount();
    if (isFixed) {
      // make just ONE copy 
      currentMesh.setPolygonCount(1);
      currentMesh.ptCenters = null;
      currentMesh.modelFlags = null;
      addModelPoints(-1);
      nPoly = setPolygons(nPoly);
    } else {
      // multiple copies, one for each model involved
      BitSet bsAllModels = new BitSet();
      if (nbitsets > 0)
        bsAllModels = viewer.getModelBitSet(bsAllAtoms);
      else
        bsAllModels = viewer.getVisibleFramesBitSet();
      currentMesh.setPolygonCount(modelCount);
      currentMesh.ptCenters = new Point3f[modelCount];
      currentMesh.modelFlags = new int[modelCount];
      for (int iModel = 0; iModel < modelCount; iModel++) {
        if (bsAllModels.get(iModel)) {
          // int n0 = currentMesh.vertexCount;
          addModelPoints(iModel);
          nPoly = setPolygons(nPoly);
          currentMesh.setCenter(iModel);
        }
      }
    }
    currentMesh.setCenter(-1);
    return true;
  }

  void addPoint(Point3f newPt) {
    ptList[nPoints++] = new Point3f(newPt);
    if (nPoints > MAX_POINTS)
      nPoints = MAX_POINTS;
  }

  private void addModelPoints(int iModel) {
    nPoints = ncoord;
    // {x,y,z} points are already defined in ptList
    // $drawID references may be fixed or not
    for (int i = 0; i < nidentifiers; i++) {
      Mesh m = meshes[ptIdentifiers[i]];
      if (isPlane || isPerpendicular || useVertices[i]) {
        if (reversePoints[i]) {
          for (ipt = m.drawVertexCount; --ipt >= 0;)
            addPoint(m.vertices[ipt]);
        } else {
          for (ipt = 0; ipt < m.drawVertexCount; ipt++)
            addPoint(m.vertices[ipt]);
        }
      } else {
        if (iModel < 0 || m.ptCenters == null || m.ptCenters[iModel] == null)
          addPoint(m.ptCenter);
        else
          addPoint(m.ptCenters[iModel]);
      }
    }
    if (iModel < 0)
      return;
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

  private int setPolygons(int nPoly) {
    if (nPoints == 4 && isCrossed) {
      Point3f pt = new Point3f(ptList[1]);
      ptList[1].set(ptList[2]);
      ptList[2].set(pt);
    }
    return setPolygon(currentMesh, nPoints, nPoly);
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();

  private int setPolygon(Mesh mesh, int nVertices, int nPoly) {
    /*
     * for now, just add all new vertices. It's simpler this way
     * though a bit redundant. We could reuse the fixed ones -- no matter
     */

    int drawType = Mesh.DRAW_POINT;
    if ((isCurve || isArrow || isCircle) && nVertices >= 2)
      drawType = (isCurve ? Mesh.DRAW_CURVE : isArrow ? Mesh.DRAW_ARROW
          : Mesh.DRAW_CIRCLE);
    if (drawType == Mesh.DRAW_POINT) {
      Point3f pt;
      Point3f center = new Point3f();
      Vector3f normal = new Vector3f();
      float dist;
      if (nVertices == 3 && isPlane && !isPerpendicular) {
        // three points define a plane
        pt = new Point3f(ptList[1]);
        pt.sub(ptList[0]);
        pt.scale(0.5f);
        ptList[3] = new Point3f(ptList[2]);
        ptList[2].add(pt);
        ptList[3].sub(pt);
        nVertices = 4;
      } else if (nVertices >= 3 && !isPlane && isPerpendicular) {
        // normal to plane
        g3d.calcNormalizedNormal(ptList[0], ptList[1], ptList[2], normal, vAB, vAC);
        center = new Point3f(ptList[0]);
        for (int i = 1; i < nVertices; i++)
          center.add(ptList[i]);
        center.scale(1f / nVertices);
        dist = (length == Float.MAX_VALUE ? ptList[0].distance(center) : length);
        normal.scale(dist);
        ptList[0].set(center);
        ptList[1].set(center);
        ptList[1].add(normal);
        nVertices = 2;
      } else if (nVertices == 2 && isPerpendicular) {
        // perpendicular line to line or plane to line
        g3d.calcAveragePoint(ptList[0], ptList[1], center);
        dist = (length == Float.MAX_VALUE ? ptList[0].distance(center) : length);
        if (isPlane && length != Float.MAX_VALUE)
          dist /= 2f;
        if (isPlane && isRotated45)
          dist *= 1.4142f;
        g3d.calcXYNormalToLine(ptList[0], ptList[1], normal);
        normal.scale(dist);
        if (isPlane) {
          ptList[2] = new Point3f(center);
          ptList[2].sub(normal);
          pt = new Point3f(center);
          pt.add(normal);
          //          pt
          //          |
          //  0-------+--------1
          //          |
          //          2
          g3d.calcNormalizedNormal(ptList[0], ptList[1], ptList[2], normal);
          normal.scale(dist);
          ptList[3] = new Point3f(center);
          ptList[3].add(normal);
          ptList[1].set(center);
          ptList[1].sub(normal);
          ptList[0].set(pt);
          //             
          //       pt,0 1
          //          |/
          //   -------+--------
          //         /|
          //        3 2

          if (isRotated45) {
            g3d.calcAveragePoint(ptList[0], ptList[1], ptList[0]);
            g3d.calcAveragePoint(ptList[1], ptList[2], ptList[1]);
            g3d.calcAveragePoint(ptList[2], ptList[3], ptList[2]);
            g3d.calcAveragePoint(ptList[3], pt, ptList[3]);
          }
          nVertices = 4;
        } else {
          ptList[0].set(center);
          ptList[1].set(center);
          ptList[0].sub(normal);
          ptList[1].add(normal);
        }
      } else if (nVertices == 2 && length != Float.MAX_VALUE) {
        g3d.calcAveragePoint(ptList[0], ptList[1], center);
        normal.set(ptList[1]);
        normal.sub(center);
        normal.scale(0.5f / normal.length() * length);
        ptList[0].set(center);
        ptList[1].set(center);
        ptList[0].sub(normal);
        ptList[1].add(normal);
      }
      if (nVertices > 4)
        nVertices = 4; // for now

      switch (nVertices) {
      case 1:
        break;
      case 2:
        drawType = Mesh.DRAW_LINE;
        break;
      default:
        drawType = Mesh.DRAW_PLANE;
      }
    }
    mesh.drawType = drawType;

    mesh.drawVertexCount = nVertices;

    if (nVertices == 0)
      return nPoly;
    int nVertices0 = mesh.vertexCount;

    for (int i = 0; i < nVertices; i++) {
      mesh.addVertexCopy(ptList[i]);
    }
    int npoints = (nVertices < 3 ? 3 : nVertices);
    mesh.setPolygonCount(nPoly + 1);
    mesh.polygonIndexes[nPoly] = new int[npoints];
    for (int i = 0; i < npoints; i++) {
      mesh.polygonIndexes[nPoly][i] = nVertices0
          + (i < nVertices ? i : nVertices - 1);
    }
    return nPoly + 1;
  }

  private void scaleDrawing(Mesh mesh, float newScale) {
    /*
     * allows for Draw to scale object
     * have to watch out for double-listed vertices
     * 
     */
    if (newScale == 0 || mesh.vertexCount == 0 || mesh.scale == newScale)
      return;
    Vector3f diff = new Vector3f();
    float f = newScale / mesh.scale;
    mesh.scale = newScale;
    int iptlast = -1;
    int ipt = 0;
    for (int i = mesh.polygonCount; --i >= 0;) {
      Point3f center = (mesh.ptCenters == null ? mesh.ptCenter
          : mesh.ptCenters[i]);
      iptlast = -1;
      for (int iV = mesh.polygonIndexes[i].length; --iV >= 0;) {
        ipt = mesh.polygonIndexes[i][iV];
        if (ipt == iptlast)
          continue;
        iptlast = ipt;
        diff.sub(mesh.vertices[ipt], center);
        diff.scale(f);
        diff.add(center);
        mesh.vertices[ipt].set(diff);
      }
    }
  }

  void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed note
     * that this is NOT done with atoms and bonds, because they have mads. When
     * you say "frame 0" it is just turning on all the mads.
     */
    int modelCount = viewer.getModelCount();
    for (int i = meshCount; --i >= 0;) {
      Mesh m = meshes[i];
      m.visibilityFlags = (m.isValid && m.visible ? myVisibilityFlag : 0);
      if (m.modelFlags == null)
        continue;
      for (int iModel = modelCount; --iModel >= 0;) {
        m.modelFlags[iModel] = (bs.get(iModel) ? 1 : 0);
      }
    }
  }

  final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  Mesh pickedMesh = null;
  int pickedModel;
  int pickedVertex;
  final Point3i ptXY = new Point3i();
  
  void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    boolean isPicking = (viewer.getPickingMode() == JmolConstants.PICKING_DRAW);
    if (!isPicking)
      return;
    if (!findPickedObject(prevX, prevY, true))
      return;
    boolean moveAll = false;
    switch (modifiers & MouseManager.BUTTON_MODIFIER_MASK) {
    case MouseManager.SHIFT_LEFT:
      moveAll = true;
    case MouseManager.ALT_LEFT:
      move2D(pickedMesh, pickedMesh.polygonIndexes[pickedModel], pickedVertex,
          prevX + deltaX, prevY + deltaY, moveAll);
      currentMesh = pickedMesh;
      break;
    case MouseManager.ALT_SHIFT_LEFT:
      // reserved -- constrained move?
      break;
    }
  }
  void move2D(Mesh mesh, int[] vertexes, int iVertex, int x, int y,
              boolean moveAll) {
    Point3i pt = new Point3i();
    Point3f coord = new Point3f();
    boolean addOffset = (mesh.drawOffset != null && mesh.drawOffset.length() > 0);
    coord.set(mesh.vertices[vertexes[iVertex]]);
    if (addOffset)
      coord.add(mesh.drawOffset);
    viewer.transformPoint(coord, pt);
    int dx = x - pt.x;
    int dy = y - pt.y;
    for (int i = (moveAll ? vertexes.length : iVertex + 1); --i >= 0;)
      if (moveAll || i == iVertex) {
        if (moveAll) {
          coord.set(mesh.vertices[vertexes[i]]);
          if (addOffset)
            coord.add(mesh.drawOffset);
          viewer.transformPoint(coord, pt);
        }
        pt.x += dx;
        pt.y += dy;
        viewer.unTransformPoint(pt, coord);
        if (addOffset)
          coord.sub(mesh.drawOffset);
        mesh.vertices[vertexes[i]].set(coord);
        if (!moveAll)
          break;
      }
    if (Logger.isActiveLevel(Logger.LEVEL_INFO))
      Logger.info(getDrawCommand(mesh));
    viewer.refresh();
  }
  
  void checkObjectClicked(int x, int y, int modifiers) {
    boolean isPicking = (viewer.getPickingMode() == JmolConstants.PICKING_DRAW);
    if (isPicking)
      return;
    if (!findPickedObject(x, y, false))
      return;
    if (pickedVertex == 0) {
      viewer.startSpinningAxis(
          pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][0]],
          pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][1]],
          ((modifiers & MouseManager.SHIFT) != 0));
      return;
    }
    viewer.startSpinningAxis(
        pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][1]],
        pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][0]],
        ((modifiers & MouseManager.SHIFT) != 0));
  }

  boolean findPickedObject(int x, int y, boolean isPicking) {
    int modelCount = viewer.getModelCount();
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    pickedModel = 0;
    pickedVertex = 0;
    pickedMesh = null;
    for (int i = meshCount; --i >= 0;) {
      Mesh m = meshes[i];
      if ((isPicking ||m.drawVertexCount == 2) && m.visibilityFlags != 0) {
        boolean addOffset = (m.drawOffset != null && m.drawOffset.length() > 0);
        int mCount = (m.modelFlags == null ? 1 : modelCount);
        for (int iModel = mCount; --iModel >= 0;) {
          if (m.modelFlags != null && m.modelFlags[iModel] == 0)
            continue;
          for (int iVertex = m.polygonIndexes[iModel].length; --iVertex >= 0;) {
            Point3f v = new Point3f();
            v.set(m.vertices[m.polygonIndexes[iModel][iVertex]]);
            if(addOffset)
              v.add(m.drawOffset);
            int d2 = coordinateInRange(x, y, v, dmin2);
            if (d2 >= 0) {
              pickedMesh = m;
              dmin2 = d2;
              pickedModel = iModel;
              pickedVertex = iVertex;
            }
          }
        }
      }
    }
    return (pickedMesh != null);
  }

  int coordinateInRange(int x, int y, Point3f vertex, int dmin2) {
    int d2 = dmin2;
    viewer.transformPoint(vertex, ptXY);
    d2 = (x - ptXY.x) * (x - ptXY.x) + (y - ptXY.y) * (y - ptXY.y);
    return (d2 < dmin2 ? d2 : -1);
  }
  
  String getDrawCommand(Mesh mesh) {
    int nVertices = 0;
    if (mesh == null)
      return "no current draw object";
    String str = "draw " + mesh.thisID;
    switch (mesh.drawType) {
    case Mesh.DRAW_ARROW:
      str += " ARROW";
      break;
    case Mesh.DRAW_CIRCLE:
      str += " CIRCLE"; //not yet implemented
      break;
    case Mesh.DRAW_CURVE:
      str += " CURVE";
      break;
    case Mesh.DRAW_LINE:
      nVertices++;
    case Mesh.DRAW_POINT:
      nVertices++;
      break;
    case Mesh.DRAW_TRIANGLE:
    case Mesh.DRAW_PLANE:
    }
    int modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      return str;
    int modelCount = viewer.getModelCount();
    int mCount = (mesh.modelFlags == null ? 1 : modelCount);
    for (int iModel = 0; iModel < mCount; iModel++) {
      if (mesh.modelFlags != null && mesh.modelFlags[iModel] == 0)
        continue;
      if (nVertices == 0)
        nVertices = mesh.polygonIndexes[iModel].length;
      boolean addOffset = (mesh.drawOffset != null && mesh.drawOffset.length() > 0);
      for (int i = 0; i < nVertices; i++) {
        Point3f v = new Point3f();
        v.set(mesh.vertices[mesh.polygonIndexes[iModel][i]]);
        if (addOffset)
          v.add(mesh.drawOffset);
        str += " {" + v.x + " " + v.y + " " + v.z + "}";
      }
    }
    return str;
  }
}
