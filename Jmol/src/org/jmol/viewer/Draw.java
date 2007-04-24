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
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Logger;
import org.jmol.g3d.Graphics3D;

class Draw extends MeshCollection {

  // bob hanson hansonr@stolaf.edu 3/2006

  DrawMesh[] dmeshes = new DrawMesh[4];
  DrawMesh thisMesh;
  
  void allocMesh(String thisID) {
    meshes = dmeshes = (DrawMesh[])ArrayUtil.ensureLength(dmeshes, meshCount + 1);
    currentMesh = thisMesh = dmeshes[meshCount++] = new DrawMesh(thisID, g3d, colix);
  }

  void setPropertySuper(String propertyName, Object value, BitSet bs) {
    currentMesh = thisMesh;
    super.setProperty(propertyName, value, bs);
    thisMesh = (DrawMesh)currentMesh;  
  }
  
  void initShape() {
    super.initShape();
    myType = "draw";
  }
  
  final static int MAX_POINTS = 256; // a few extras here
  Point3f[] ptList = new Point3f[MAX_POINTS];
  int[] ptIdentifiers = new int[MAX_POINTS];
  boolean[] reversePoints = new boolean[MAX_POINTS];
  boolean[] useVertices = new boolean[MAX_POINTS];
  BitSet[] ptBitSets = new BitSet[MAX_POINTS];
  BitSet bsAllAtoms = new BitSet();
  Vector3f offset = new Vector3f();
  int nPoints;
  int nbitsets;
  int ncoord;
  int nidentifiers;
  int diameter;
  Integer rgb;
  float newScale;
  float length;
  boolean isCurve;
  boolean isArrow;
  boolean isCircle;
  boolean isVisible;
  boolean isPerpendicular;
  boolean isVertices;
  boolean isPlane;
  boolean isReversed;
  boolean isRotated45;
  boolean isCrossed;
  boolean isValid;

  void setProperty(String propertyName, Object value, BitSet bs) {
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug("draw " + propertyName + " " + value);
    }

    if ("init" == propertyName) {
      colix = Graphics3D.ORANGE;
      nPoints = -1;
      newScale = 0;
      ncoord = nbitsets = nidentifiers = 0;
      isFixed = isReversed = isRotated45 = isCrossed = false;
      isCurve = isArrow = isPlane = isVertices = isPerpendicular = false;
      isVisible = isValid = true;
      length = Float.MAX_VALUE;
      diameter = 0;
      bsAllAtoms.clear();
      rgb = null;
      offset = new Vector3f();
      if (colix == 0)
        colix = Graphics3D.GOLD;
      setPropertySuper("thisID", Mesh.PREVIOUS_MESH_ID, null);
      //fall through to MeshCollection "init"
    }

    if ("colorRGB" == propertyName) {
      rgb = (Integer) value;
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
      if (thisMesh != null) {
        // no points in this script statement
        scaleDrawing(thisMesh, newScale);
        thisMesh.initialize(Mesh.FULLYLIT);
      }
      return;
    }

    if ("diameter" == propertyName) {
      diameter = ((Float) value).intValue();
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
      if (thisMesh != null)
        thisMesh.offset(offset);
      return;
    }

    if ("atomSet" == propertyName) {
      if (BitSetUtil.cardinalityOf((BitSet) value) == 0)
        return;
      ptBitSets[nbitsets++] = (BitSet) value;
      bsAllAtoms.or((BitSet) value);
      nPoints++;
      return;
    }
    if ("set" == propertyName) {
      if (thisMesh == null) {
        allocMesh(null);
        thisMesh.colix = colix;
      }
      thisMesh.isValid = (isValid ? setDrawing() : false);
      if (thisMesh.isValid) {
        if (thisMesh.vertexCount > 2 && length != Float.MAX_VALUE
            && newScale == 1)
          newScale = length;
        scaleDrawing(thisMesh, newScale);
        thisMesh.initialize(Mesh.FULLYLIT);
        setAxes(thisMesh);
        thisMesh.title = title;
        thisMesh.visible = isVisible; 
      }
      nPoints = -1; // for later scaling
      return;
    }
    if ("off" == propertyName) {
      isVisible = false;
      //let pass through
    }

    setPropertySuper(propertyName, value, bs);
  }

  Object getProperty(String property, int index) {
    if (property == "command")
      return getDrawCommand(thisMesh);
    if (property == "vertices")
      return getPath(thisMesh);
    if (property.indexOf("getSpinCenter:") == 0)
      return getSpinCenter(property.substring(14), index);
    if (property.indexOf("getSpinAxis:") == 0)
      return getSpinAxis(property.substring(12), index);
    return super.getProperty(property, index);
  }

  Point3f getSpinCenter(String axisID, int modelIndex) {
    int meshIndex = getIndexFromName(axisID);
    return (meshIndex < 0 ? null : getSpinCenter(meshIndex, modelIndex));
   }
   
  Vector3f getSpinAxis(String axisID, int modelIndex) {
    int meshIndex = getIndexFromName(axisID);
    return (meshIndex < 0 ? null : getSpinAxis(meshIndex, modelIndex));
   }
  
  Object getPath(Mesh mesh) {
    if (mesh == null)
      return null;
    return mesh.vertices;
  }
  
  boolean setDrawing() {
    if (thisMesh == null)
      allocMesh(null);
    thisMesh.clear("draw");
    if (nPoints == 0)
      return false;
    int nPoly = 0;
    if (isFixed || isArrow || isCurve || modelCount == 1) {
      // make just ONE copy 
      // arrows and curves simply can't be handled as
      // multiple frames yet
      thisMesh.modelIndex = viewer.getDisplayModelIndex();
      if (thisMesh.modelIndex < 0)
        thisMesh.modelIndex = 0;
      if (isFixed && !isArrow && !isCurve && modelCount > 1)
        thisMesh.modelIndex = -1;
      thisMesh.setPolygonCount(1);
      thisMesh.ptCenters = null;
      thisMesh.modelFlags = null;
      thisMesh.drawTypes = null;
      thisMesh.drawVertexCounts = null;
      thisMesh.diameter = diameter;
      if (rgb != null)
        super.setProperty("color", rgb, null);
      addModelPoints(-1);
      nPoly = setPolygons(nPoly);
    } else {
      // multiple copies, one for each model involved
      BitSet bsAllModels = new BitSet();
      if (nbitsets > 0)
        bsAllModels = viewer.getModelBitSet(bsAllAtoms);
      else if (nidentifiers > 0)
        for (int i = 0; i < nidentifiers; i++)
          for (int j = dmeshes[ptIdentifiers[i]].polygonCount; --j >= 0;)
            bsAllModels.set(j);
      else
        bsAllModels = viewer.getVisibleFramesBitSet();
      thisMesh.setPolygonCount(modelCount);
      thisMesh.ptCenters = new Point3f[modelCount];
      thisMesh.modelFlags = new int[modelCount];
      thisMesh.drawTypes = new int[modelCount];
      thisMesh.drawVertexCounts = new int[modelCount];

      for (int iModel = 0; iModel < modelCount; iModel++) {
        if (bsAllModels.get(iModel)) {
          addModelPoints(iModel);
          setPolygons(nPoly);
          thisMesh.setCenter(iModel);
          thisMesh.drawTypes[iModel] = thisMesh.drawType;
          thisMesh.drawVertexCounts[iModel] = thisMesh.drawVertexCount;
          thisMesh.drawType = DrawMesh.DRAW_MULTIPLE;
          thisMesh.drawVertexCount = -1;
        } else {
          thisMesh.drawTypes[iModel] = DrawMesh.DRAW_NONE;
          thisMesh.polygonIndexes[iModel] = new int[0];
        }
        nPoly++;
      }
    }
    thisMesh.setCenter(-1);
    if (thisMesh.thisID == null) {
      thisMesh.thisID = thisMesh.getDrawType() + (++nUnnamed);
    }
    return true;
  }

  void addPoint(Point3f newPt) {
    ptList[nPoints] = new Point3f(newPt);
    if (offset != null)
      ptList[nPoints].add(offset);    
    if (++nPoints > MAX_POINTS)
      nPoints = MAX_POINTS;
  }

  private void addModelPoints(int iModel) {
    nPoints = ncoord;
    // {x,y,z} points are already defined in ptList
    // $drawID references may be fixed or not
    for (int i = 0; i < nidentifiers; i++) {
      DrawMesh m = dmeshes[ptIdentifiers[i]];
      if (isPlane || isPerpendicular || useVertices[i]) {
        if (reversePoints[i]) {
          if (iModel < 0 || iModel >= m.polygonCount)
            for (int ipt = m.drawVertexCount; --ipt >= 0;)
              addPoint(m.vertices[ipt]);
          else
            for (int ipt = m.drawVertexCounts[iModel]; --ipt >= 0;)
              addPoint(m.vertices[m.polygonIndexes[iModel][ipt]]);
        } else {
          if (iModel < 0 || iModel >= m.polygonCount)
            for (int ipt = 0; ipt < m.drawVertexCount; ipt++)
              addPoint(m.vertices[ipt]);
          else
            for (int ipt = m.drawVertexCounts[iModel]; --ipt >= 0;)
              addPoint(m.vertices[m.polygonIndexes[iModel][ipt]]);
        }
      } else {
        if (iModel < 0 || m.ptCenters == null || m.ptCenters[iModel] == null)
          addPoint(m.ptCenter);
        else
          addPoint(m.ptCenters[iModel]);
      }
    }
    // (atom set) references must be filtered for relevant model
    // note that if a model doesn't have a relevant point, one may
    // get a line instead of a plane, a point instead of a line, etc.
    if (nbitsets == 0)
      return;
    BitSet bsModel = (iModel < 0 ? null : viewer.getModelAtomBitSet(iModel));
    for (int i = 0; i < nbitsets; i++) {
      BitSet bs = (BitSet) ptBitSets[i].clone();
      if (bsModel != null)
        bs.and(bsModel);
      if (BitSetUtil.cardinalityOf(bs) > 0) {
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
    return setPolygon(thisMesh, nPoints, nPoly);
  }

  final Vector3f vAB = new Vector3f();
  final Vector3f vAC = new Vector3f();

  private int setPolygon(DrawMesh mesh, int nVertices, int nPoly) {
    /*
     * for now, just add all new vertices. It's simpler this way
     * though a bit redundant. We could reuse the fixed ones -- no matter
     */

    int drawType = DrawMesh.DRAW_POINT;
    if ((isCurve || isArrow || isCircle) && nVertices >= 2)
      drawType = (isCurve ? DrawMesh.DRAW_CURVE : isArrow ? DrawMesh.DRAW_ARROW
          : DrawMesh.DRAW_CIRCLE);
    if (drawType == DrawMesh.DRAW_POINT) {
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
        Graphics3D.calcNormalizedNormal(ptList[0], ptList[1], ptList[2], normal, vAB, vAC);
        center = new Point3f();
        Graphics3D.calcAveragePointN(ptList, nVertices, center);
        dist = (length == Float.MAX_VALUE ? ptList[0].distance(center) : length);
        normal.scale(dist);
        ptList[0].set(center);
        ptList[1].set(center);
        ptList[1].add(normal);
        nVertices = 2;
      } else if (nVertices == 2 && isPerpendicular) {
        // perpendicular line to line or plane to line
        Graphics3D.calcAveragePoint(ptList[0], ptList[1], center);
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
          Graphics3D.calcNormalizedNormal(ptList[0], ptList[1], ptList[2], normal, vAB, vAC);
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
            Graphics3D.calcAveragePoint(ptList[0], ptList[1], ptList[0]);
            Graphics3D.calcAveragePoint(ptList[1], ptList[2], ptList[1]);
            Graphics3D.calcAveragePoint(ptList[2], ptList[3], ptList[2]);
            Graphics3D.calcAveragePoint(ptList[3], pt, ptList[3]);
          }
          nVertices = 4;
        } else {
          ptList[0].set(center);
          ptList[1].set(center);
          ptList[0].sub(normal);
          ptList[1].add(normal);
        }
      } else if (nVertices == 2 && length != Float.MAX_VALUE) {
        Graphics3D.calcAveragePoint(ptList[0], ptList[1], center);
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
        drawType = DrawMesh.DRAW_LINE;
        break;
      default:
        drawType = DrawMesh.DRAW_PLANE;
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

  private void scaleDrawing(DrawMesh mesh, float newScale) {
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
      if (center == null)
        return;
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

  final Point3f getSpinCenter(int meshIndex, int modelIndex) {
    DrawMesh m = dmeshes[meshIndex];
    if (m.vertices == null)
      return null;
    Point3f pt = (m.ptCenters == null || modelIndex < 0 ? m.ptCenter : m.ptCenters[modelIndex]);
    pt.add(offset);
    return pt;
  }
  
  final Vector3f getSpinAxis(int meshIndex, int modelIndex) {
    DrawMesh m = dmeshes[meshIndex];
    if (m.vertices == null)
      return null;
    return (m.ptCenters == null || modelIndex < 0 ? m.axis : m.axes[modelIndex]);
  }
  
  final void setAxes(DrawMesh m) {
    m.axis = new Vector3f(0, 0, 0);
    m.axes = new Vector3f[m.polygonCount > 0 ? m.polygonCount : 1];
    if (m.vertices == null)
      return;
    int n = 0;
    for (int i = m.polygonCount; --i >= 0;) {
      int[] p = m.polygonIndexes[i];
      m.axes[i] = new Vector3f();
      if (p.length == 0) {
      } else if (m.drawVertexCount == 2 || m.drawVertexCount < 0
          && m.drawVertexCounts[i] == 2) {
        m.axes[i].sub(m.vertices[p[0]],
            m.vertices[p[1]]);
        n++;
      } else {
        Graphics3D.calcNormalizedNormal(m.vertices[p[0]],
            m.vertices[p[1]],
            m.vertices[p[2]], m.axes[i], m.vAB, m.vAC);
        n++;
      }
      m.axis.add(m.axes[i]);
    }
    if (n == 0)
      return;
    m.axis.scale(1f / n);
  }

  void setVisibilityFlags(BitSet bs) {
    /*
     * set all fixed objects visible; others based on model being displayed note
     * that this is NOT done with atoms and bonds, because they have mads. When
     * you say "frame 0" it is just turning on all the mads.
     */
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = dmeshes[i];
      m.visibilityFlags = (m.isValid && m.visible ? myVisibilityFlag : 0);
      if (m.modelIndex >= 0 && !bs.get(m.modelIndex)) {
        m.visibilityFlags = 0;
        continue;
      }
      if (m.modelFlags == null)
        continue;
      for (int iModel = modelCount; --iModel >= 0;)
        m.modelFlags[iModel] = (bs.get(iModel) ? 1 : 0);
    }
  }
  
  final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  DrawMesh pickedMesh = null;
  int pickedModel;
  int pickedVertex;
  final Point3i ptXY = new Point3i();
  
  boolean checkObjectClicked(int x, int y, int modifiers) {
    if (viewer.getPickingMode() == JmolConstants.PICKING_DRAW)
      return false;
    if (!findPickedObject(x, y, false))
      return false;
    if (pickedVertex == 0) {
      viewer.startSpinningAxis(
          pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][0]],
          pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][1]],
          ((modifiers & MouseManager.SHIFT) != 0));
    } else {
      viewer.startSpinningAxis(
          pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][1]],
          pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][0]],
          ((modifiers & MouseManager.SHIFT) != 0));
    }
    return true;
  }

  boolean checkObjectHovered(int x, int y) {
    //if (viewer.getPickingMode() == JmolConstants.PICKING_DRAW)
      //return false;
    if (!findPickedObject(x, y, false))
      return false;
    viewer.hoverOn(x, y, (pickedMesh.title == null ? pickedMesh.thisID
        : pickedMesh.title[0]));
    return true;
  }

  synchronized boolean checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    if (viewer.getPickingMode() != JmolConstants.PICKING_DRAW)
      return false;
    if (!findPickedObject(prevX, prevY, true))
      return false;
    boolean moveAll = false;
    switch (modifiers & MouseManager.BUTTON_MODIFIER_MASK) {
    case MouseManager.SHIFT_LEFT:
      moveAll = true;
      //fall through
    case MouseManager.ALT_LEFT:
      move2D(pickedMesh, pickedMesh.polygonIndexes[pickedModel], pickedVertex,
          prevX + deltaX, prevY + deltaY, moveAll);
      thisMesh = pickedMesh;
      break;
    case MouseManager.ALT_SHIFT_LEFT:
      // reserved -- constrained move?
      return false;
    }
    return true;
  }
  
  void move2D(DrawMesh mesh, int[] vertexes, int iVertex, int x, int y,
              boolean moveAll) {
    if (vertexes == null || vertexes.length == 0)
      return;
    Point3f pt = new Point3f();
    Point3f coord = new Point3f();
    Point3f newcoord = new Point3f();
    Vector3f move = new Vector3f();
    coord.set(mesh.vertices[vertexes[iVertex]]);
    viewer.transformPoint(coord, pt);
    pt.x = x;
    pt.y = y;
    viewer.unTransformPoint(pt, newcoord);
    move.set(newcoord);
    move.sub(coord);
    int klast = -1;
    for (int i = (moveAll ? 0 : iVertex); i < vertexes.length; i++)
      if (moveAll || i == iVertex) {
        int k = vertexes[i];
        if (k == klast)
            break;
        mesh.vertices[k].add(move);
        if (!moveAll)
          break;
        klast = k;
      }
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
      Logger.debug(getDrawCommand(mesh));
    viewer.refresh(0, "draw");
  }
  
  boolean findPickedObject(int x, int y, boolean isPicking) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    pickedModel = 0;
    pickedVertex = 0;
    pickedMesh = null;
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = dmeshes[i];
      if ((isPicking || m.drawType == DrawMesh.DRAW_LINE || m.drawType == DrawMesh.DRAW_MULTIPLE)
          && m.visibilityFlags != 0) {
        int mCount = (m.modelFlags == null ? 1 : modelCount);
        for (int iModel = mCount; --iModel >= 0;) {
          if (m.modelFlags != null && m.modelFlags[iModel] == 0)
            continue;
          for (int iVertex = m.polygonIndexes[iModel].length; --iVertex >= 0;) {
            Point3f v = new Point3f();
            v.set(m.vertices[m.polygonIndexes[iModel][iVertex]]);
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
  
  private String getDrawCommand(DrawMesh mesh) {
    int nVertices = 0;
    if (mesh == null)
      return "no current draw object";
    StringBuffer str = new StringBuffer("draw " + mesh.thisID);
    switch (mesh.drawType) {
    case DrawMesh.DRAW_MULTIPLE:
      return getDrawCommand(mesh, -1);
    case DrawMesh.DRAW_ARROW:
      str.append(" ARROW");
      break;
    case DrawMesh.DRAW_CIRCLE:
      str.append(" CIRCLE"); //not yet implemented
      break;
    case DrawMesh.DRAW_CURVE:
      str.append(" CURVE");
      break;
    case DrawMesh.DRAW_LINE:
      nVertices += 2;
      break;
    case DrawMesh.DRAW_POINT:
      nVertices++;
      break;
    case DrawMesh.DRAW_TRIANGLE:
    case DrawMesh.DRAW_PLANE:
    }
    int modelIndex = viewer.getDisplayModelIndex();
    if (modelIndex < 0)
      return str.toString();
    int mCount = (mesh.modelFlags == null ? 1 : modelCount);
    for (int iModel = 0; iModel < mCount; iModel++) {
      if (mesh.modelFlags != null && mesh.modelFlags[iModel] == 0)
        continue;
      str.append(getVertexList(mesh, iModel, nVertices));
    }
    str.append(";\n").append(getColorCommand("draw", mesh.colix)).append(";");

    return str.toString();
  }

  String getVertexList(Mesh mesh, int iModel, int nVertices) {
    String str = "";
    if (nVertices == 0)
      nVertices = mesh.polygonIndexes[iModel].length;
    for (int i = 0; i < nVertices; i++) {
      Point3f v = new Point3f();
      v.set(mesh.vertices[mesh.polygonIndexes[iModel][i]]);
      str += " " + StateManager.escape(v);
    }
    return str;
  }
  
  String getDrawCommand(DrawMesh mesh, int iModel) {
    StringBuffer str = new StringBuffer();
    if (iModel < 0) {
      for (int i = 0; i < modelCount; i++)
        str.append(getDrawCommand(mesh, i));
      return str.toString();
    }
    int nVertices = 0;
    String nFrame = viewer.getModelNumberDotted(iModel);
    if (modelCount > 1)
      str.append("frame ").append(nFrame).append(";");
    str.append("draw ").append(mesh.thisID).
        append(mesh.drawType == DrawMesh.DRAW_MULTIPLE ? "_" + iModel : "");
    switch (mesh.drawTypes == null ? mesh.drawType : mesh.drawTypes[iModel]) {
    case DrawMesh.DRAW_NONE:
      return "";
    case DrawMesh.DRAW_ARROW:
      str.append(" ARROW");
      break;
    case DrawMesh.DRAW_CIRCLE:
      str.append(" CIRCLE"); //not yet implemented
      break;
    case DrawMesh.DRAW_CURVE:
      str.append(" CURVE");
      break;
    case DrawMesh.DRAW_LINE:
      nVertices += 2;
      break;
    case DrawMesh.DRAW_POINT:
      nVertices++;
      break;
    case DrawMesh.DRAW_TRIANGLE:
    case DrawMesh.DRAW_PLANE:
    }
    String s = getVertexList(mesh, iModel, nVertices);
    if (mesh.diameter > 0)
      s += " diameter " + mesh.diameter;
    if (mesh.drawTriangles)
      s += " mesh";
    if (!mesh.fillTriangles)
      s += " nofill";    
    if (mesh.title != null)
      s += " " + StateManager.escape(mesh.title[0]);
    appendCmd(str, s);
    appendCmd(str, getColorCommand("draw", mesh.colix));
    return str.toString();
  }

  Vector getShapeDetail() {
    Vector V = new Vector();
    if (nPoints == 0)
      return V;
    for (int i = 0; i < meshCount; i++) {
      DrawMesh mesh = dmeshes[i];
      if (mesh.vertexCount == 0)
        continue;
      Hashtable info = new Hashtable();
      info.put("fixed", mesh.ptCenters == null ? Boolean.TRUE : Boolean.FALSE);
      info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
      info.put("drawType", mesh.getDrawType());
      if (mesh.diameter > 0)
        info.put("diameter", new Integer(mesh.diameter));
      info.put("scale", new Float(mesh.scale));
      if (mesh.drawType == DrawMesh.DRAW_MULTIPLE) {
        Vector m = new Vector();
        for (int k = 0; k < modelCount; k++) {
          if (mesh.ptCenters[k] == null)
            continue;            
          Hashtable mInfo = new Hashtable();
          mInfo.put("modelIndex", new Integer(k));
          mInfo.put("command", getDrawCommand(mesh, k));
          mInfo.put("center", mesh.ptCenters[k]);
          int nPoints = mesh.drawVertexCounts[k];
          mInfo.put("vertexCount", new Integer(nPoints));
          if (nPoints > 1)
            mInfo.put("axis", mesh.axes[k]);
          Vector v = new Vector();
          for (int ipt = 0; ipt < nPoints; ipt++)
            v.addElement(mesh.vertices[mesh.polygonIndexes[k][ipt]]);
          mInfo.put("vertices", v);
          if (mesh.drawTypes[k] == DrawMesh.DRAW_LINE) {
            float d = mesh.vertices[mesh.polygonIndexes[k][0]]
                .distance(mesh.vertices[mesh.polygonIndexes[k][1]]);
            mInfo.put("length_Ang", new Float(d));
          }
          m.addElement(mInfo);
        }
        info.put("models", m);
      } else {
        info.put("command", getDrawCommand(mesh));
        info.put("center", mesh.ptCenter);
        if (mesh.drawVertexCount > 1)
          info.put("axis", mesh.axis);
        Vector v = new Vector();
        for (int j = 0; j < mesh.vertexCount; j++)
          v.addElement(mesh.vertices[j]);
        info.put("vertices", v);
        if (mesh.drawType == DrawMesh.DRAW_LINE)
          info.put("length_Ang", new Float(mesh.vertices[0]
              .distance(mesh.vertices[1])));
      }
      V.addElement(info);
    }
    return V;
  }

  String getShapeState() {
    StringBuffer s = new StringBuffer();
    if (nPoints == 0)
      return "";
    for (int i = 0; i < meshCount; i++) {
      DrawMesh mesh = dmeshes[i];
      if (mesh.vertexCount == 0)
        continue;
      s.append(getDrawCommand(mesh, mesh.modelIndex));
      if (!mesh.visible)
        s.append("draw off;\n");
    }
    return s.toString();
  }

}
