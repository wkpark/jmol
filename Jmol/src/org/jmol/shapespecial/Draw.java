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

package org.jmol.shapespecial;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.MouseManager;
import org.jmol.g3d.Graphics3D;
import org.jmol.shape.Mesh;
import org.jmol.shape.MeshCollection;

public class Draw extends MeshCollection {

  // bob hanson hansonr@stolaf.edu 3/2006

  DrawMesh[] dmeshes = new DrawMesh[4];
  DrawMesh thisMesh;
  
  public void allocMesh(String thisID) {
    meshes = dmeshes = (DrawMesh[])ArrayUtil.ensureLength(dmeshes, meshCount + 1);
    currentMesh = thisMesh = dmeshes[meshCount++] = new DrawMesh(thisID, g3d, colix);
  }

  void setPropertySuper(String propertyName, Object value, BitSet bs) {
    currentMesh = thisMesh;
    super.setProperty(propertyName, value, bs);
    thisMesh = (DrawMesh)currentMesh;  
  }
  
 public void initShape() {
    super.initShape();
    myType = "draw";
  }
  
  private final static int MAX_POINTS = 256; // a few extras here
  private Point3f[] ptList = new Point3f[MAX_POINTS];
  private int[] ptIdentifiers = new int[MAX_POINTS];
  private boolean[] reversePoints = new boolean[MAX_POINTS];
  private boolean[] useVertices = new boolean[MAX_POINTS];
  private BitSet[] ptBitSets = new BitSet[MAX_POINTS];
  private BitSet bsAllAtoms = new BitSet();
  private Vector3f offset = new Vector3f();
  private int nPoints;
  private int nbitsets;
  private int ncoord;
  private int nidentifiers;
  private int diameter;
  private Integer rgb;
  private float newScale;
  private float length;
  private boolean isCurve;
  private boolean isArrow;
  private boolean isCircle;
  private boolean isVisible;
  private boolean isPerpendicular;
  private boolean isVertices;
  private boolean isPlane;
  private boolean isReversed;
  private boolean isRotated45;
  private boolean isCrossed;
  private boolean isValid;
  private int indicatedModelIndex = -1;
  private Point3f[][] modelVertices;

  public void setProperty(String propertyName, Object value, BitSet bs) {
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
      modelVertices = null;
      bsAllAtoms.clear();
      indicatedModelIndex = -1;
      rgb = null;
      offset = new Vector3f();
      if (colix == 0)
        colix = Graphics3D.GOLD;
      setPropertySuper("thisID", JmolConstants.PREVIOUS_MESH_ID, null);
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
    
    if ("modelIndex" == propertyName) {
      indicatedModelIndex = ((Integer) value).intValue();
      if (indicatedModelIndex < 0 || indicatedModelIndex > modelCount)
        return;
      if (ncoord > 0) {
        ptList = new Point3f[5];
      } else {
        modelVertices = new Point3f[modelCount][];
      }      
      modelVertices[indicatedModelIndex] = ptList;
      ncoord = 0;
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
        thisMesh.initialize(JmolConstants.FULLYLIT);
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
        thisMesh.initialize(JmolConstants.FULLYLIT);
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

 public Object getProperty(String property, int index) {
    if (property == "command")
      return getDrawCommand(thisMesh);
    if (property == "vertices")
      return getPath(thisMesh);
    if (property == "type")
      return new Integer(thisMesh == null ? JmolConstants.DRAW_NONE : thisMesh.drawType);
    if (property.indexOf("getSpinCenter:") == 0)
      return getSpinCenter(property.substring(14), index);
    if (property.indexOf("getSpinAxis:") == 0)
      return getSpinAxis(property.substring(12), index);
    return super.getProperty(property, index);
  }

  private Point3f getSpinCenter(String axisID, int modelIndex) {
    int pt = axisID.indexOf(".");
    String id = (pt > 0 ? axisID.substring(0, pt) : axisID);
    int meshIndex = getIndexFromName(id);
    int vertexIndex = (pt > 0 ? Integer.parseInt(axisID.substring(pt + 1)) : 0) - 1;
    return (meshIndex < 0 ? null : getSpinCenter(meshIndex, vertexIndex, modelIndex));
   }
   
  private Vector3f getSpinAxis(String axisID, int modelIndex) {
    int meshIndex = getIndexFromName(axisID);
    return (meshIndex < 0 ? null : getSpinAxis(meshIndex, modelIndex));
   }
  
  private Object getPath(Mesh mesh) {
    if (mesh == null)
      return null;
    return mesh.vertices;
  }
  
  private boolean setDrawing() {
    if (thisMesh == null)
      allocMesh(null);
    thisMesh.clear("draw");
    if (nPoints == 0)
      return false;
    int nPoly = 0;
    if (modelVertices == null
        && (isFixed || isArrow || isCurve || modelCount == 1)) {
      // make just ONE copy 
      // arrows and curves simply can't be handled as
      // multiple frames yet
      thisMesh.isFixed = isFixed;
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
      thisMesh.modelIndex = -1;
      thisMesh.setPolygonCount(modelCount);
      thisMesh.ptCenters = new Point3f[modelCount];
      thisMesh.modelFlags = new int[modelCount];
      thisMesh.drawTypes = new int[modelCount];
      thisMesh.drawVertexCounts = new int[modelCount];
      thisMesh.vertexCount = 0;
      if (indicatedModelIndex >= 0) {
        int nVertices = Math.max(ncoord, 3);
        for (int i = 0; i < modelCount; i++) {
          int n0 = thisMesh.vertexCount;
          Point3f[] pts = modelVertices[i];
          if (pts != null) {
            int[] p = thisMesh.polygonIndexes[i] = new int[nVertices];
            for (int j = 0; j < ncoord; j++) {
              p[j] = thisMesh.addVertexCopy(pts[j]);
            }
            for (int j = ncoord; j < 3; j++) {
              p[j] = n0 + ncoord - 1;
            }
            thisMesh.drawTypes[i] = thisMesh.drawVertexCounts[i] = ncoord;
          }
        }
        thisMesh.drawType = JmolConstants.DRAW_MULTIPLE;
        thisMesh.drawVertexCount = -1;
        modelVertices = null;
      } else {
        for (int iModel = 0; iModel < modelCount; iModel++) {

          if (bsAllModels.get(iModel)) {
            addModelPoints(iModel);
            setPolygons(nPoly);
            thisMesh.setCenter(iModel);
            thisMesh.drawTypes[iModel] = thisMesh.drawType;
            thisMesh.drawVertexCounts[iModel] = thisMesh.drawVertexCount;
            thisMesh.drawType = JmolConstants.DRAW_MULTIPLE;
            thisMesh.drawVertexCount = -1;
          } else {
            thisMesh.drawTypes[iModel] = JmolConstants.DRAW_NONE;
            thisMesh.polygonIndexes[iModel] = new int[0];
          }
          nPoly++;
        }
      }
    }
    thisMesh.setCenter(-1);
    if (thisMesh.thisID == null) {
      thisMesh.thisID = thisMesh.getDrawType() + (++nUnnamed);
    }
    return true;
  }

  private void addPoint(Point3f newPt) {
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

  private final Vector3f vAB = new Vector3f();
  private final Vector3f vAC = new Vector3f();

  private int setPolygon(DrawMesh mesh, int nVertices, int nPoly) {
    /*
     * for now, just add all new vertices. It's simpler this way
     * though a bit redundant. We could reuse the fixed ones -- no matter
     */

    int drawType = JmolConstants.DRAW_POINT;
    if ((isCurve || isArrow || isCircle) && nVertices >= 2)
      drawType = (isCurve ? JmolConstants.DRAW_CURVE : isArrow ? JmolConstants.DRAW_ARROW
          : JmolConstants.DRAW_CIRCLE);
    if (drawType == JmolConstants.DRAW_POINT) {
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
        drawType = JmolConstants.DRAW_LINE;
        break;
      default:
        drawType = JmolConstants.DRAW_PLANE;
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

  private static void scaleDrawing(DrawMesh mesh, float newScale) {
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

  private final Point3f getSpinCenter(int meshIndex, int vertexIndex, int modelIndex) {
    DrawMesh m = dmeshes[meshIndex];
    if (m.vertices == null || m.vertexCount <= vertexIndex)
      return null;
    Point3f pt = (vertexIndex >= 0 ? m.vertices[vertexIndex]
        : m.ptCenters == null || modelIndex < 0 ? m.ptCenter : m.ptCenters[modelIndex]);
    pt.add(offset);
    return pt;
  }
  
  private final Vector3f getSpinAxis(int meshIndex, int modelIndex) {
    DrawMesh m = dmeshes[meshIndex];
    if (m.vertices == null)
      return null;
    return (m.ptCenters == null || modelIndex < 0 ? m.axis : m.axes[modelIndex]);
  }
  
  private final static void setAxes(DrawMesh m) {
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

 public void setVisibilityFlags(BitSet bs) {
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
  
  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  private DrawMesh pickedMesh = null;
  private int pickedModel;
  private int pickedVertex;
  private final Point3i ptXY = new Point3i();
  
  public boolean checkObjectClicked(int x, int y, int modifiers) {
    if (viewer.getPickingMode() == JmolConstants.PICKING_DRAW)
      return false;
    if (!findPickedObject(x, y, false))
      return false;
    if (pickedMesh.polygonIndexes[pickedModel][0] == pickedMesh.polygonIndexes[pickedModel][1])
      return false; // single point
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

  public boolean checkObjectHovered(int x, int y) {
    //if (viewer.getPickingMode() == JmolConstants.PICKING_DRAW)
      //return false;
    if (!findPickedObject(x, y, false))
      return false;
    viewer.hoverOn(x, y, (pickedMesh.title == null ? pickedMesh.thisID
        : pickedMesh.title[0]));
    return true;
  }

  public synchronized boolean checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
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
  
  private void move2D(DrawMesh mesh, int[] vertexes, int iVertex, int x, int y,
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
  
  private boolean findPickedObject(int x, int y, boolean isPicking) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    pickedModel = 0;
    pickedVertex = 0;
    pickedMesh = null;
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = dmeshes[i];
      if ((true || isPicking || m.drawType == JmolConstants.DRAW_LINE || m.drawType == JmolConstants.DRAW_MULTIPLE)
          && m.visibilityFlags != 0) {
        int mCount = (m.modelFlags == null ? 1 : modelCount);
        for (int iModel = mCount; --iModel >= 0;) {
          if (m.modelFlags != null && m.modelFlags[iModel] == 0 || m.polygonIndexes == null)
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

  private int coordinateInRange(int x, int y, Point3f vertex, int dmin2) {
    int d2 = dmin2;
    viewer.transformPoint(vertex, ptXY);
    d2 = (x - ptXY.x) * (x - ptXY.x) + (y - ptXY.y) * (y - ptXY.y);
    return (d2 < dmin2 ? d2 : -1);
  }
  
  private String getDrawCommand(DrawMesh mesh) {
    if (mesh == null)
      return "no current draw object";
    return getDrawCommand(mesh, mesh.modelIndex);
  }

  private String getDrawCommand(DrawMesh mesh, int iModel) {
    if (mesh.drawType == JmolConstants.DRAW_NONE)
      return "";
    StringBuffer str = new StringBuffer();
    if (!mesh.isFixed && iModel >= 0 && modelCount > 1)
      str.append("frame ").append(viewer.getModelNumberDotted(iModel)).append(
          ";\n");
    str.append("draw ").append(mesh.thisID);
    if (mesh.isFixed)
      str.append(" fixed");
    if (iModel < 0)
      iModel = 0;
    if (mesh.diameter > 0)
      str.append(" diameter ").append(mesh.diameter);
    int nVertices = 0;
    switch (mesh.drawTypes == null ? mesh.drawType : mesh.drawTypes[iModel]) {
    case JmolConstants.DRAW_ARROW:
      str.append(" ARROW");
      break;
    case JmolConstants.DRAW_CIRCLE:
      str.append(" CIRCLE"); //not yet implemented
      break;
    case JmolConstants.DRAW_CURVE:
      str.append(" CURVE");
      break;
    case JmolConstants.DRAW_POINT:
      nVertices = 1;
      break;
    case JmolConstants.DRAW_LINE:
      nVertices = 2;
      break;
    }

    if (mesh.modelIndex < 0 && !mesh.isFixed) {
      for (int i = 0; i < modelCount; i++)
        if (isPolygonDisplayable(mesh, i)) {
          str.append(" [ " + i);
          str.append(getVertexList(mesh, i, nVertices));
          str.append(" ] ");
        }
    } else {
      str.append(getVertexList(mesh, iModel, nVertices));
    }
    if (mesh.title != null)
      str.append(" " + Escape.escape(mesh.title[0]));
    str.append(";\n");
    appendCmd(str, mesh.getState("draw"));
    appendCmd(str, getColorCommand("draw", mesh.colix));
    return str.toString();
  }

  static boolean isPolygonDisplayable(Mesh mesh, int i) {
    return (mesh.polygonIndexes[i] != null && mesh.polygonIndexes[i].length > 0 && mesh.polygonIndexes[i].length > 0);
  }
  
  private static String getVertexList(Mesh mesh, int iModel, int nVertices) {
    String str = "";
    if (nVertices == 0)
      nVertices = mesh.polygonIndexes[iModel].length;
    for (int i = 0; i < nVertices; i++) {
      Point3f v = new Point3f();
      try{
      v.set(mesh.vertices[mesh.polygonIndexes[iModel][i]]);
      }catch(Exception e) {
        System.out.println("OHOH");
      }
      str += " " + Escape.escape(v);
    }
    return str;
  }
  
  public Vector getShapeDetail() {
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
      if (mesh.drawType == JmolConstants.DRAW_MULTIPLE) {
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
          if (mesh.drawTypes[k] == JmolConstants.DRAW_LINE) {
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
        if (mesh.drawType == JmolConstants.DRAW_LINE)
          info.put("length_Ang", new Float(mesh.vertices[0]
              .distance(mesh.vertices[1])));
      }
      V.addElement(info);
    }
    return V;
  }

 public String getShapeState() {
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
