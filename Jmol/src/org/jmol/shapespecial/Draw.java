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
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

import org.jmol.util.ArrayUtil;
import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.TextFormat;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.MouseManager;
import org.jmol.viewer.Token;
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
  
  private Point3f[] ptList;
  private Vector3f offset = new Vector3f();
  private int nPoints;
  private int diameter;
  private float width;
  private float newScale;
  private float length;
  private boolean isCurve;
  private boolean isArc;
  private boolean isArrow;
  private boolean isLine;
  private boolean isVector;
  private boolean isCircle;
  private boolean isPerpendicular;
  private boolean isCylinder;
  private boolean isVertices;
  private boolean isPlane;
  private boolean isReversed;
  private boolean isRotated45;
  private boolean isCrossed;
  private boolean isValid;
  private boolean noHead;
  private int indicatedModelIndex = -1;
  private int[] modelInfo;
  private boolean makePoints;
  private int nidentifiers;
  private int nbitsets;
  private Point4f plane;
  private BitSet bsAllModels;
  
  private Vector vData;
  private final static int PT_COORD = 1;
  private final static int PT_IDENTIFIER = 2;
  private final static int PT_BITSET = 3;
  private final static int PT_MODEL_INDEX = 4;
  private final static int PT_MODEL_BASED_POINTS = 5;

  public void setProperty(String propertyName, Object value, BitSet bs) {
    if (Logger.debugging) {
      Logger.debug("draw " + propertyName + " " + value);
    }

    if ("init" == propertyName) {
      colix = Graphics3D.ORANGE;
      newScale = 0;
      isFixed = isReversed = isRotated45 = isCrossed = noHead = false;
      isCurve = isArc = isArrow = isPlane = isCircle = isCylinder = isLine = false;
      isVertices = isPerpendicular = isVector = false;
      isValid = true;
      length = Float.MAX_VALUE;
      diameter = 0;
      width = 0;
      indicatedModelIndex = -1;
      offset = null;
      plane = null;
      nidentifiers = nbitsets = 0;
      vData = new Vector();
      modelCount = viewer.getModelCount();
      bsAllModels = null;
      setPropertySuper("thisID", JmolConstants.PREVIOUS_MESH_ID, null);
      setPropertySuper("init", value, bs);
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
      //from saved state -- used to set modelVertices
      indicatedModelIndex = ((Integer) value).intValue();
      if (indicatedModelIndex < 0 || indicatedModelIndex >= modelCount)
        return;
      vData.add(new Object[] { new Integer(PT_MODEL_INDEX),
          (modelInfo = new int[] { indicatedModelIndex, 0 }) });
      return;
    }

    if ("planedef" == propertyName) {
      plane = (Point4f) value;
      return;
    }

    if ("perp" == propertyName) {
      isPerpendicular = true;
      return;
    }

    if ("cylinder" == propertyName) {
      isCylinder = true;
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

    if ("line" == propertyName) {
      isLine = true;
      isCurve = true;
      return;
    }

    if ("arc" == propertyName) {
      isCurve = true;
      isArc = true;
      if (isArrow) {
        isArrow = false;
        isVector = true;
      }
      return;
    }

    if ("circle" == propertyName) {
      isCircle = true;
      return;
    }

    if ("vector" == propertyName) {
      isArrow = true;
      isVector = true;
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

    if ("nohead" == propertyName) {
      noHead = true;
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

    if ("width" == propertyName) {
      width = ((Float) value).floatValue();
      return;
    }

    if ("identifier" == propertyName) {
      String thisID = (String) value;
      int meshIndex = getIndexFromName(thisID);
      if (meshIndex >= 0) {
        vData.add(new Object[] { new Integer(PT_IDENTIFIER),
            new int[] { meshIndex, isReversed ? 1 : 0, isVertices ? 1 : 0 } });
        isReversed = isVertices = false;
        nidentifiers++;
      } else {
        Logger.error("draw identifier " + value + " not found");
        isValid = false;
      }
      return;
    }

    if ("coord" == propertyName) {
      vData.add(new Object[] { new Integer(PT_COORD), value });
      if (indicatedModelIndex >= 0)
        modelInfo[1]++; // counts vertices
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
      bs = (BitSet) value;
      vData.add(new Object[] { new Integer(PT_BITSET), bs });
      nbitsets++;
      if (isCircle && diameter == 0 && width == 0)
        width = viewer.calcRotationRadius(bs) * 2.0f;
      return;
    }

    if ("modelBasedPoints" == propertyName) {
      vData.add(new Object[] { new Integer(PT_MODEL_BASED_POINTS), value });
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
        thisMesh.visible = true;
      }
      nPoints = -1; // for later scaling
      vData = null;
      return;
    }
    
    if (propertyName == "deleteModelAtoms") {
      int modelIndex = ((int[]) ((Object[]) value)[2])[0];
      //int firstAtomDeleted = ((int[])((Object[])value)[2])[1];
      //int nAtomsDeleted = ((int[])((Object[])value)[2])[2];
      for (int i = meshCount; --i >= 0;) {
        if (meshes[i] == null)
          continue;
        if (dmeshes[i].modelFlags != null) {
          dmeshes[i].deleteAtoms(modelIndex);
        } else if (meshes[i].modelIndex == modelIndex) {
          meshCount--;
          if (meshes[i] == currentMesh)
            currentMesh = thisMesh = null;
          meshes = dmeshes = (DrawMesh[]) ArrayUtil
              .deleteElements(meshes, i, 1);
        } else if (meshes[i].modelIndex > modelIndex) {
          meshes[i].modelIndex--;
        }
      }
      return;
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
    String id;
    int vertexIndex = -1;
    int pt = axisID.indexOf("[");
    int pt2;
    if (pt > 0) {
      id = axisID.substring(0,pt);
      if ((pt2 = axisID.lastIndexOf("]")) < pt)
        pt2 = axisID.length();
      try {
        vertexIndex = Integer.parseInt(axisID.substring(pt + 1, pt2)) - 1;
      } catch (Exception e) {
        // ignore
      }
    } else {
      id = axisID;
      vertexIndex = -1;
    }
    int meshIndex = getIndexFromName(id);
    DrawMesh m;
    return (meshIndex < 0 || (m = dmeshes[meshIndex]).vertices == null
        || m.vertexCount <= vertexIndex ? null 
        : vertexIndex >= 0 ? m.vertices[vertexIndex] 
        : m.ptCenters == null || modelIndex < 0 ? m.ptCenter 
        : m.ptCenters[modelIndex]);
  }
   
  private Vector3f getSpinAxis(String axisID, int modelIndex) {
    int meshIndex = getIndexFromName(axisID);
    DrawMesh m;
    return (meshIndex < 0 || (m = dmeshes[meshIndex]).vertices == null ? null :
      m.ptCenters == null || modelIndex < 0 ? m.axis : m.axes[modelIndex]);
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
    int nData = vData.size();
    if (nData == 0)
      return false;
    if (indicatedModelIndex < 0
        && (isFixed || isArrow || isCurve || isCircle || isCylinder || modelCount == 1)) {
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
      if (setPoints(-1, -1))
        setPoints(-1, nPoints);
      setPolygon(0); 
    } else {
      // multiple copies, one for each model involved
      thisMesh.modelIndex = -1;
      thisMesh.setPolygonCount(modelCount);
      thisMesh.ptCenters = new Point3f[modelCount];
      thisMesh.modelFlags = new BitSet();
      thisMesh.drawTypes = new int[modelCount];
      thisMesh.drawVertexCounts = new int[modelCount];
      thisMesh.vertexCount = 0;
      if (indicatedModelIndex >= 0) {
        setPoints(-1, 0);
        thisMesh.drawType = JmolConstants.DRAW_MULTIPLE;
        thisMesh.drawVertexCount = -1;
        indicatedModelIndex = -1;
      } else {
        for (int iModel = 0; iModel < modelCount; iModel++) {
          if (setPoints(iModel, -1)) {
            setPoints(iModel, nPoints);
            setPolygon(iModel); 
            thisMesh.setCenter(iModel);
            thisMesh.drawTypes[iModel] = thisMesh.drawType;
            thisMesh.drawVertexCounts[iModel] = thisMesh.drawVertexCount;
            thisMesh.drawType = JmolConstants.DRAW_MULTIPLE;
            thisMesh.drawVertexCount = -1;
          } else {
            thisMesh.drawTypes[iModel] = JmolConstants.DRAW_NONE;
            thisMesh.polygonIndexes[iModel] = new int[0];
          }
        }
      }
    }
    thisMesh.diameter = diameter;
    thisMesh.isVector = isVector;
    thisMesh.nohead = noHead;
    thisMesh.width = (thisMesh.drawType == JmolConstants.DRAW_CYLINDER ? -Math.abs(width) : width);
    thisMesh.setCenter(-1);
    if (offset != null)
      thisMesh.offset(offset);
    if (thisMesh.thisID == null) {
      thisMesh.thisID = JmolConstants.getDrawTypeName(thisMesh.drawType) + (++nUnnamed);
    }
    return true;
  }

  private void addPoint(Point3f newPt, int iModel) {
    boolean isOK = (iModel < 0 || bsAllModels.get(iModel));
    if (makePoints) {
      if (!isOK)
        return;
      ptList[nPoints] = new Point3f(newPt);
      if (newPt.z == Float.MAX_VALUE || newPt.z == -Float.MAX_VALUE)
        thisMesh.haveXyPoints = true;
    } else if (iModel >= 0) {
      bsAllModels.set(iModel);
    }
    nPoints++;
  }

  private boolean setPoints(int iModel, int n) {
    // {x,y,z} points are already defined in ptList
    // $drawID references may be fixed or not
    // Prior to 11.5.37, points were created in the order:
    //  1) all {x,y,z} points
    //  2) all $drawID points
    //  3) all {atomExpression} points
    //  4) all {atomExpression}.split() points
    // Order is only important when there are four points, 
    // where they may become crossed, so
    // we also provide a flag CROSSED to uncross them
    this.makePoints = (n >= 0);
    if (makePoints) {
      ptList = new Point3f[Math.max(5,n)];
      if (bsAllModels == null)
        bsAllModels = viewer.getVisibleFramesBitSet();
    }
    nPoints = 0;
    int nData = vData.size();
    int modelIndex = 0;
    BitSet bs;
    BitSet bsModel = (iModel < 0 ? null : viewer.getModelAtomBitSet(iModel,
        false));
    for (int i = 0; i < nData; i++) {
      Object[] info = (Object[]) vData.elementAt(i);
      switch (((Integer) info[0]).intValue()) {
      case PT_MODEL_INDEX:
        // from the saved state
        int[] modelInfo = (int[]) info[1];
        modelIndex = modelInfo[0];
        nPoints = modelInfo[1];
        int nVertices = Math.max(nPoints, 3);
        int n0 = thisMesh.vertexCount;
        if (nPoints > 0) {
          int[] p = thisMesh.polygonIndexes[modelIndex] = new int[nVertices];
          for (int j = 0; j < nPoints; j++) {
            info = (Object[]) vData.elementAt(++i);
            p[j] = thisMesh.addVertexCopy((Point3f) info[1]);
          }
          for (int j = nPoints; j < 3; j++) {
            p[j] = n0 + nPoints - 1;
          }
          thisMesh.drawTypes[modelIndex] = thisMesh.drawVertexCounts[modelIndex] = nPoints;
        }
        break;
      case PT_COORD:
        addPoint((Point3f) info[1], (makePoints ? iModel : -1));
        break;
      case PT_BITSET:
        // (atom set) references must be filtered for relevant model
        // note that if a model doesn't have a relevant point, one may
        // get a line instead of a plane, a point instead of a line, etc.
        bs = BitSetUtil.copy((BitSet) info[1]);
        if (bsModel != null)
          bs.and(bsModel);
        if (BitSetUtil.firstSetBit(bs) >= 0)
          addPoint(viewer.getAtomSetCenter(bs), (makePoints ? iModel : -1));
        break;
      case PT_IDENTIFIER:
        int[] idInfo = (int[]) info[1];
        DrawMesh m = dmeshes[idInfo[0]];
        boolean isReversed = (idInfo[1] == 1);
        boolean isVertices = (idInfo[2] == 1);
        if (m.modelIndex > 0 && m.modelIndex != iModel)
          return false;
        if (bsAllModels == null)
          bsAllModels = new BitSet();
        if (isPlane && !isCircle || isPerpendicular || isVertices) {
          if (isReversed) {
            if (iModel < 0 || iModel >= m.polygonCount)
              for (int ipt = m.drawVertexCount; --ipt >= 0;)
                addPoint(m.vertices[ipt], iModel);
            else if (m.polygonIndexes[iModel] != null)
              for (int ipt = m.drawVertexCounts[iModel]; --ipt >= 0;)
                addPoint(m.vertices[m.polygonIndexes[iModel][ipt]], iModel);
          } else {
            if (iModel < 0 || iModel >= m.polygonCount)
              for (int ipt = 0; ipt < m.drawVertexCount; ipt++)
                addPoint(m.vertices[ipt], iModel);
            else if (m.polygonIndexes[iModel] != null)
              for (int ipt = m.drawVertexCounts[iModel]; --ipt >= 0;)
                addPoint(m.vertices[m.polygonIndexes[iModel][ipt]], iModel);
          }
        } else {
          if (iModel < 0 || m.ptCenters == null || m.ptCenters[iModel] == null)
            addPoint(m.ptCenter, iModel);
          else
            addPoint(m.ptCenters[iModel], iModel);
        }
        break;
      case PT_MODEL_BASED_POINTS:
        // from list variables
        String[] modelBasedPoints = (String[]) info[1];
        if (bsAllModels == null)
          bsAllModels = new BitSet();
        for (int j = 0; j < modelBasedPoints.length; j++)
          if (iModel < 0 || j == iModel) {
            Object points = Token
                .unescapePointOrBitsetAsToken(modelBasedPoints[j]);
            if (!(points instanceof Token))
              continue;
            bsAllModels.set(j);
            switch (((Token) points).tok) {
            case Token.point3f:
              addPoint((Point3f) ((Token) points).value, j);
              break;
            case Token.bitset:
              bs = (BitSet) ((Token) points).value;
              if (bsModel != null)
                bs.and(bsModel);
              if (BitSetUtil.firstSetBit(bs) >= 0)
                addPoint(viewer.getAtomSetCenter(bs), j);
              break;
            default:
            //ignored
            }
          }
        break;
      }
    }
    if (makePoints && isCrossed && nPoints == 4) {
      Point3f pt = ptList[1];
      ptList[1] = ptList[2];
      ptList[2] = pt;
    }
    return (nPoints > 0);
  }

  private final Vector3f vAB = new Vector3f();
  private final Vector3f vAC = new Vector3f();

  private void setPolygon(int nPoly) {
    int nVertices = nPoints; 
    int drawType = JmolConstants.DRAW_POINT;
    if (isArc) {
      if (nVertices == 4) {
        drawType = JmolConstants.DRAW_ARC;
      } else {
        isArc = false;
        isVector = false;
        isCurve = false;
        isArrow = true;
      }
    }
    if (isCircle)
      drawType = (isPlane ? JmolConstants.DRAW_CIRCULARPLANE
          : JmolConstants.DRAW_CIRCLE);
    else if ((isCurve || isArrow) && nVertices >= 2 && !isArc)
      drawType = (isLine ? JmolConstants.DRAW_LINE_SEGMENT 
          : isCurve ? JmolConstants.DRAW_CURVE 
          : JmolConstants.DRAW_ARROW);
    if (isVector && !isArc) {
      if (nVertices > 2)
        nVertices = 2;
      else if (plane == null && nVertices != 2)
        isVector = false;
    }
    if (thisMesh.haveXyPoints) {
      isPerpendicular = false;
      if (nVertices == 3 && isPlane)
        isPlane = false;
      length = Float.MAX_VALUE;
      thisMesh.diameter = 0;
    } else if (nVertices == 2 && isVector) {
      ptList[1].add(ptList[0]);
    }
    if (drawType == JmolConstants.DRAW_POINT) {
      Point3f pt;
      Point3f center = new Point3f();
      Vector3f normal = new Vector3f();
      float dist;
      if (nVertices == 1 && plane != null) {
        dist = Graphics3D.distanceToPlane(plane, ptList[0]);
        vAC.set(plane.x, plane.y, plane.z);
        vAC.normalize();
        vAC.scale(-dist);
        vAC.add(ptList[0]);
        ptList[1]= new Point3f(vAC);
        nVertices = -2;
        if (isArrow)
          drawType = JmolConstants.DRAW_ARROW;
      } else if (nVertices == 3 && isPlane && !isPerpendicular) {
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
        Graphics3D.calcNormalizedNormal(ptList[0], ptList[1], ptList[2],
            normal, vAB, vAC);
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
          Graphics3D.calcNormalizedNormal(ptList[0], ptList[1], ptList[2],
              normal, vAB, vAC);
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
        if (isArrow && nVertices != -2)
          isArrow = false;
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
      case -2:
        nVertices = 2;
        break;
      case 1:
        break;
      case 2:
        drawType = (isCylinder ? JmolConstants.DRAW_CYLINDER
            : JmolConstants.DRAW_LINE);
        break;
      default:
        drawType = JmolConstants.DRAW_PLANE;
      }
    }
    thisMesh.drawType = drawType;
    thisMesh.drawVertexCount = nVertices;

    if (nVertices == 0)
      return;
    int nVertices0 = thisMesh.vertexCount;

    for (int i = 0; i < nVertices; i++) {
      thisMesh.addVertexCopy(ptList[i]);
    }
    int npoints = (nVertices < 3 ? 3 : nVertices);
    thisMesh.setPolygonCount(nPoly + 1);
    thisMesh.polygonIndexes[nPoly] = new int[npoints];
    for (int i = 0; i < npoints; i++) {
      thisMesh.polygonIndexes[nPoly][i] = nVertices0
          + (i < nVertices ? i : nVertices - 1);
    }
    return;
  }

  private static void scaleDrawing(DrawMesh mesh, float newScale) {
    /*
     * allows for Draw to scale object
     * have to watch out for double-listed vertices
     * 
     */
    if (newScale == 0 || mesh.vertexCount == 0 || mesh.scale == newScale)
      return;
    float f = newScale / mesh.scale;
    mesh.scale = newScale;
    if (mesh.haveXyPoints || mesh.drawType == JmolConstants.DRAW_ARC)
      return; // done in renderer
    Vector3f diff = new Vector3f();
    int iptlast = -1;
    int ipt = 0;
    for (int i = mesh.polygonCount; --i >= 0;) {
      Point3f center = (mesh.isVector ? mesh.vertices[0] 
          : mesh.ptCenters == null ? mesh.ptCenter
          : mesh.ptCenters[i]);
      if (center == null)
        return;
      if (mesh.polygonIndexes[i] == null)
        continue;
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

  private final static void setAxes(DrawMesh m) {
    m.axis = new Vector3f(0, 0, 0);
    m.axes = new Vector3f[m.polygonCount > 0 ? m.polygonCount : 1];
    if (m.vertices == null)
      return;
    int n = 0;
    for (int i = m.polygonCount; --i >= 0;) {
      int[] p = m.polygonIndexes[i];
      m.axes[i] = new Vector3f();
      if (p == null || p.length == 0) {
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
        m.modelFlags.set(iModel, bs.get(iModel));
    }
  }
  
  private final static int MAX_OBJECT_CLICK_DISTANCE_SQUARED = 10 * 10;

  private DrawMesh pickedMesh = null;
  private int pickedModel;
  private int pickedVertex;
  private final Point3i ptXY = new Point3i();
  
  public Point3f checkObjectClicked(int x, int y, int modifiers, BitSet bsVisible) {
    boolean isPickingMode = (viewer.getPickingMode() == JmolConstants.PICKING_DRAW);
    boolean isSpinMode = (viewer.getPickingMode() == JmolConstants.PICKING_SPIN);
    boolean isDrawPicking = viewer.getDrawPicking();
    if (!isPickingMode && !isDrawPicking && !isSpinMode)
      return null;
    if (!findPickedObject(x, y, false, bsVisible))
      return null;
    Point3f v = pickedMesh.vertices[pickedMesh.polygonIndexes[pickedModel][pickedVertex]];
    if (isDrawPicking && !isPickingMode) {
      if (modifiers != 0) // not mouseMove
        viewer.setStatusAtomPicked(-2, "[\"draw\",\"" + pickedMesh.thisID + "\"," +
          + pickedModel + "," + pickedVertex + "," + v.x + "," + v.y + "," + v.z+"]"
          + (pickedMesh.title == null ? "" 
               : "\"" + pickedMesh.title[pickedModel]+"\""));
      return v; 
    }
    if (modifiers == 0 || pickedMesh.polygonIndexes[pickedModel][0] == pickedMesh.polygonIndexes[pickedModel][1])
      return (modifiers == 0 ? v : null); // single point
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
    return null;
  }

  public boolean checkObjectHovered(int x, int y, BitSet bsVisible) {
    if (!findPickedObject(x, y, false, bsVisible))
      return false;
    if (g3d.isDisplayAntialiased()) {
      //because hover rendering is done in FIRST pass only
      x <<= 1;
      y <<= 1;
    }      
    String s = (pickedMesh.title == null ? pickedMesh.thisID
        : pickedMesh.title[0]);
    if (s.length() > 1 && s.charAt(0) == '>')
      s = s.substring(1);
    viewer.hoverOn(x, y, s);
    return true;
  }

  public synchronized boolean checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers, BitSet bsVisible) {
    if (viewer.getPickingMode() != JmolConstants.PICKING_DRAW)
      return false;
    //  mouse down ?
    if (deltaX == Integer.MIN_VALUE) 
      return findPickedObject(prevX, prevY, true, bsVisible);
    //  mouse up ?
    if (deltaX == Integer.MAX_VALUE) {
      pickedMesh = null;
      return false;
    }
    if (pickedMesh == null)
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
  
  private void move2D(DrawMesh mesh, int[] vertexes, 
                      int iVertex, int x, int y,
                      boolean moveAll) {
    if (vertexes == null || vertexes.length == 0)
      return;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    Point3f pt = new Point3f();
    Point3f coord = new Point3f(mesh.vertices[vertexes[iVertex]]);
    Point3f newcoord = new Point3f();
    Vector3f move = new Vector3f();
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
    if (mesh.ptCenters == null)
      mesh.setCenter(-1);
    else
      for (int i = mesh.ptCenters.length; --i >= 0; )
        mesh.setCenter(i);
    if (Logger.debugging)
      Logger.debug(getDrawCommand(mesh));
    viewer.refresh(3, "draw");
  }
  
  private boolean findPickedObject(int x, int y, boolean isPicking, BitSet bsVisible) {
    int dmin2 = MAX_OBJECT_CLICK_DISTANCE_SQUARED;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
      dmin2 <<= 1;
    }
    pickedModel = 0;
    pickedVertex = 0;
    pickedMesh = null;
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = dmeshes[i];
      if (m.visibilityFlags != 0) {
        int mCount = (m.modelFlags == null ? 1 : modelCount);
        for (int iModel = mCount; --iModel >= 0;) {
          if (m.modelFlags != null && !m.modelFlags.get(iModel) 
              || m.polygonIndexes == null 
              || iModel >= m.polygonIndexes.length || m.polygonIndexes[iModel] == null)
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
    modelCount = viewer.getModelCount();
    if (mesh != null)
      return getDrawCommand(mesh, mesh.modelIndex);
    
    StringBuffer sb = new StringBuffer();
    String key = (explicitID && previousMeshID != null
        && TextFormat.isWild(previousMeshID) ? previousMeshID.toUpperCase()
        : null);
    if (key != null && key.length() == 0)
      key = null;
    for (int i = 0; i < meshCount; i++) {
      DrawMesh m = (DrawMesh) meshes[i];
      if (key == null
          || TextFormat.isMatch(m.thisID.toUpperCase(), key, true, true))
        sb.append(getDrawCommand(m, m.modelIndex));
    }
    return sb.toString();
  }

  private String getDrawCommand(DrawMesh mesh, int iModel) {
    if (mesh.drawType == JmolConstants.DRAW_NONE
       || mesh.drawVertexCount == 0 && mesh.drawVertexCounts == null)
      return "";
    StringBuffer str = new StringBuffer();
    if (!mesh.isFixed && iModel >= 0 && modelCount > 1)
      appendCmd(str,"frame " + viewer.getModelNumberDotted(iModel));
    str.append("  draw ID ").append(Escape.escape(mesh.thisID));
    if (mesh.isFixed)
      str.append(" fixed");
    if (iModel < 0)
      iModel = 0;
    if (mesh.nohead)
      str.append(" noHead");
    if (mesh.scale != 1 && (mesh.haveXyPoints || mesh.drawType == JmolConstants.DRAW_ARC))
      str.append(" scale ").append(mesh.scale);
    if (mesh.width != 0)
      str.append(" diameter ").append((mesh.drawType == JmolConstants.DRAW_CYLINDER ? Math.abs(mesh.width) : mesh.width));
    else if (mesh.diameter > 0)
      str.append(" diameter ").append(mesh.diameter);
    int nVertices = mesh.drawVertexCount > 0 ? mesh.drawVertexCount 
      : mesh.drawVertexCounts[iModel >= 0 ? iModel : 0];
    switch (mesh.drawTypes == null ? mesh.drawType : mesh.drawTypes[iModel]) {
    case JmolConstants.DRAW_LINE_SEGMENT:
      str.append(" LINE");
      break;
    case JmolConstants.DRAW_ARC:
      str.append(mesh.isVector ? " ARROW ARC" : " ARC");
      break;
    case JmolConstants.DRAW_ARROW:
      str.append(mesh.isVector ? " VECTOR" : " ARROW");
      break;
    case JmolConstants.DRAW_CIRCLE:
      str.append(" CIRCLE");
      break;
    case JmolConstants.DRAW_CIRCULARPLANE:
      str.append(" CIRCLE PLANE");
      break;
    case JmolConstants.DRAW_CURVE:
      str.append(" CURVE");
      break;
    case JmolConstants.DRAW_CYLINDER:
      str.append(" CYLINDER");
      break;
    case JmolConstants.DRAW_POINT:
      nVertices = 1; // because this might be multiple points
      break;
    case JmolConstants.DRAW_LINE:
      nVertices = 2; // because this might be multiple lines
      break;
    }
    if (mesh.modelIndex < 0 && !mesh.isFixed) {
      for (int i = 0; i < modelCount; i++)
        if (isPolygonDisplayable(mesh, i)) {
          if (nVertices == 0)
            nVertices = mesh.drawVertexCounts[i];
          str.append(" [ " + i);
          str.append(getVertexList(mesh, i, nVertices));
          str.append(" ] ");
        }
    } else {
      str.append(getVertexList(mesh, iModel, nVertices));
    }
    if (mesh.title != null) {
      String s = "";
      for (int i = 0; i < mesh.title.length; i++)
        s += "|" + mesh.title[i];
      str.append(Escape.escape(s.substring(1)));
    }
    str.append(";\n");
    appendCmd(str, mesh.getState("draw"));
    appendCmd(str, getColorCommand("draw", mesh.colix));
    return str.toString();
  }

  static boolean isPolygonDisplayable(Mesh mesh, int i) {
    return (i < mesh.polygonIndexes.length 
        && mesh.polygonIndexes[i] != null 
        && mesh.polygonIndexes[i].length > 0);
  }
  
  private static String getVertexList(DrawMesh mesh, int iModel, int nVertices) {
    String str = "";
    try {
      if (iModel >= mesh.polygonIndexes.length)
        iModel = 0; // arrows and curves may not have multiple model representations
      boolean adjustPt = (mesh.isVector && mesh.drawType != JmolConstants.DRAW_ARC);
      for (int i = 0; i < nVertices; i++) {
        Point3f pt = mesh.vertices[mesh.polygonIndexes[iModel][i]];
        if (pt.z == Float.MAX_VALUE || pt.z == -Float.MAX_VALUE) {
          str += (i == 0 ? " " : " ,") + "[" + (int) pt.x + " " + (int) pt.y + (pt.z < 0 ? " %]" : "]");
        } else if (adjustPt && i == 1){
          Point3f pt1 = new Point3f(pt);
          pt1.sub(mesh.vertices[mesh.polygonIndexes[iModel][0]]);
          str += " " + Escape.escape(pt1);
        } else {
          str += " " + Escape.escape(pt);
        }
      }
    } catch (Exception e) {
      Logger.error("Unexpected error in Draw.getVertexList");
    }
    return str;
  }
  
  public Vector getShapeDetail() {
    Vector V = new Vector();
    for (int i = 0; i < meshCount; i++) {
      DrawMesh mesh = dmeshes[i];
      if (mesh.vertexCount == 0)
        continue;
      Hashtable info = new Hashtable();
      info.put("fixed", mesh.ptCenters == null ? Boolean.TRUE : Boolean.FALSE);
      info.put("ID", (mesh.thisID == null ? "<noid>" : mesh.thisID));
      info.put("drawType", JmolConstants.getDrawTypeName(mesh.drawType));
      if (mesh.diameter > 0)
        info.put("diameter", new Integer(mesh.diameter));
      if (mesh.width != 0)
        info.put("width", new Float(mesh.width));
      info.put("scale", new Float(mesh.scale));
      if (mesh.drawType == JmolConstants.DRAW_MULTIPLE) {
        Vector m = new Vector();
        modelCount = viewer.getModelCount();
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
    StringBuffer s = new StringBuffer("\n");
    appendCmd(s, "draw delete");
    modelCount = viewer.getModelCount();
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
