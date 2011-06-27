package org.jmol.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.jvxl.data.MeshData;
import org.jmol.modelset.BoxInfo;
import org.jmol.script.Token;

public class MeshSurface {


  public void merge(MeshData m) {
    int nV = vertexCount + m.vertexCount;
    if (polygonIndexes == null)
      polygonIndexes = new int[0][];
    if (m.polygonIndexes == null)
      m.polygonIndexes = new int[0][];
    int nP = 0;
    for (int i = 0; i < polygonCount; i++)
      if (polygonIndexes[i] != null && (bsSlabDisplay == null || bsSlabDisplay.get(i)))
        nP++;
    for (int i = 0; i < m.polygonCount; i++)
      if (m.polygonIndexes[i] != null && (m.bsSlabDisplay == null || m.bsSlabDisplay.get(i)))
        nP++;
    if (vertices == null)
      vertices = new Point3f[0];
    vertices = (Point3f[]) ArrayUtil.ensureLength(vertices, nV);
    vertexValues = ArrayUtil.ensureLength(vertexValues, nV);
    boolean haveSources = (vertexSource != null && m.vertexSource != null);
    vertexSource = ArrayUtil.ensureLength(vertexSource, nV);
    int[][] newPolygons = new int[nP][];
    // note -- assuming here this is not colorDensity
    int ipt = mergePolygons(this, 0, 0, newPolygons);
    ipt = mergePolygons(m, ipt, vertexCount, newPolygons);
    for (int i = 0; i < m.vertexCount; i++, vertexCount++) {
      vertices[vertexCount] = m.vertices[i];
      vertexValues[vertexCount] = m.vertexValues[i];
      if (haveSources)
        vertexSource[vertexCount] = m.vertexSource[i];
    }
    bsSlabDisplay = null;
    polygonIndexes = newPolygons;
    polygonCount = nP;
    vertexCount = nV;
  }

  private static int mergePolygons(MeshSurface m, int ipt, int vertexCount, int[][] newPolygons) {
    int[] p;
    for (int i = 0; i < m.polygonCount; i++) {
      if ((p = m.polygonIndexes[i]) == null || m.bsSlabDisplay != null && !m.bsSlabDisplay.get(i))
        continue;
      newPolygons[ipt++] = m.polygonIndexes[i];
      if (vertexCount > 0)
        for (int j = 0; j < 3; j++)
          p[j] += vertexCount;
    }
    return ipt;
  }

  protected static final int SEED_COUNT = 25;

  public int vertexCount;
  public Point3f[] vertices;
  public float[] vertexValues;
  public int[] vertexSource;
  
  public int polygonCount;
  public int[][] polygonIndexes;
  
  public boolean haveQuads;
  public short colix;
  public boolean isColorSolid = true;
  public Point3f offset;
  public Tuple3f[] altVertices;

  public short[] polygonColixes;
  public short[] vertexColixes;
  public Tuple3f[] normals;
  public int normalCount;
  public BitSet bsPolygons;
  public Point3f ptOffset;
  public float scale3d;
  public BitSet[] surfaceSet;
  public int[] vertexSets;
  public int nSets = 0;
  public int checkCount = 2;

  

  public MeshSurface() {
  }
  
  public MeshSurface(int[][] polygonIndexes, Tuple3f[] vertices, int nVertices,
      Tuple3f[] normals, int nNormals) {
    this.polygonIndexes = polygonIndexes;
    if (vertices instanceof Point3f[])
      this.vertices = (Point3f[]) vertices;
    else
      this.altVertices = vertices;
    this.vertexCount = (nVertices == 0 ? vertices.length : nVertices);
    this.normals = normals;
    this.normalCount = (nNormals == 0  && normals != null ? normals.length : nNormals);
  }
  
  public MeshSurface(Point3f[] vertices, float[] vertexValues, int vertexCount,
      int[][] polygonIndexes, int polygonCount, int checkCount) {
    this.vertices = vertices;
    this.vertexValues = vertexValues;
    this.vertexCount = vertexCount;
    this.polygonIndexes = polygonIndexes;
    this.polygonCount = polygonCount;
    this.checkCount = checkCount;
  }

  /**
   * @return The vertices.
   */
  public Tuple3f[] getVertices() {
    return (altVertices == null ? vertices : altVertices);
  }
  
  /**
   * @return  faces, if defined (in exporter), otherwise polygonIndexes
   */
  public int[][] getFaces() {
    return polygonIndexes;
  }

  public void setColix(short colix) {
    this.colix = colix;
  }

  public int addVertexCopy(Point3f vertex) { //used by mps and surfaceGenerator
    if (vertexCount == 0)
      vertices = new Point3f[SEED_COUNT];
    else if (vertexCount == vertices.length)
      vertices = (Point3f[]) ArrayUtil.doubleLength(vertices);
    vertices[vertexCount] = new Point3f(vertex);
    return vertexCount++;
  }

  public void addTriangle(int vertexA, int vertexB, int vertexC) {
    addPolygon(new int[] { vertexA, vertexB, vertexC });
  }

  public void addQuad(int vertexA, int vertexB, int vertexC, int vertexD) {
    haveQuads = true;
    addPolygon(new int[] { vertexA, vertexB, vertexC, vertexD });
  }

  protected int addPolygon(int[] polygon) {
    int n = polygonCount;
    if (polygonCount == 0)
      polygonIndexes = new int[SEED_COUNT][];
    else if (polygonCount == polygonIndexes.length)
      polygonIndexes = (int[][]) ArrayUtil.doubleLength(polygonIndexes);
    if (bsSlabDisplay != null)
      bsSlabDisplay.set(polygonCount);
    polygonIndexes[polygonCount++] = polygon;
    return n;
  }

  public void setPolygonCount(int polygonCount) {    
    this.polygonCount = polygonCount;
    if (polygonCount < 0)
      return;
    if (polygonIndexes == null || polygonCount > polygonIndexes.length)
      polygonIndexes = new int[polygonCount][];
  }

  private int addVertexCopy(Point3f vertex, float value, int source) {
    if (vertexSource != null) {
      if (vertexCount >= vertexSource.length)
        vertexSource = ArrayUtil.doubleLength(vertexSource);
      vertexSource[vertexCount] = source;
    }
    return addVertexCopy(vertex, value);
  } 

  public int addVertexCopy(Point3f vertex, float value) {
    if (vertexCount == 0)
      vertexValues = new float[SEED_COUNT];
    else if (vertexCount >= vertexValues.length)
      vertexValues = ArrayUtil.doubleLength(vertexValues);
    vertexValues[vertexCount] = value;
    return addVertexCopy(vertex);
  } 

  public int addTriangleCheck(int vertexA, int vertexB, int vertexC, int check,
                              int check2, int color) {
    return (vertices == null
        || vertexValues != null
        && (Float.isNaN(vertexValues[vertexA])
            || Float.isNaN(vertexValues[vertexB]) 
            || Float.isNaN(vertexValues[vertexC])) 
        || Float.isNaN(vertices[vertexA].x)
        || Float.isNaN(vertices[vertexB].x) 
        || Float.isNaN(vertices[vertexC].x) 
        ? -1 
      : checkCount == 2 ? addPolygon(new int[] { vertexA, vertexB, vertexC, check, check2 },
        color)
      : addPolygon(new int[] { vertexA, vertexB, vertexC, check }));
  }

  private int lastColor;
  private short lastColix;
    
  private int addPolygon(int[] polygon, int color) {
    if (color != 0) {
      if (polygonColixes == null || polygonCount == 0)
        lastColor = 0;
      short colix = (color == lastColor ? lastColix : (lastColix = Graphics3D
          .getColix(lastColor = color)));      
      setPolygonColix(polygonCount, colix);
    }
    return addPolygon(polygon);
  }

  private void setPolygonColix(int index, short colix) {
    if (polygonColixes == null) {
      polygonColixes = new short[SEED_COUNT];
    } else if (index >= polygonColixes.length) {
      polygonColixes = ArrayUtil.doubleLength(polygonColixes);
    }
    polygonColixes[index] = colix;
  }
  
  public void invalidatePolygons() {
    for (int i = polygonCount; --i >= 0;)
      if ((bsSlabDisplay == null || bsSlabDisplay.get(i)) && !setABC(i))
        polygonIndexes[i] = null;
  }

  protected int iA, iB, iC;
  
  protected boolean setABC(int i) {
    if (bsSlabDisplay != null && !bsSlabDisplay.get(i))
      return false;
    int[] vertexIndexes = polygonIndexes[i];
    return vertexIndexes != null
          && !(Float.isNaN(vertexValues[iA = vertexIndexes[0]])
            || Float.isNaN(vertexValues[iB = vertexIndexes[1]]) 
            || Float.isNaN(vertexValues[iC = vertexIndexes[2]]));
  }

  public int polygonCount0;
  public int vertexCount0;
  public BitSet bsSlabDisplay;
  public BitSet bsDisplay;

  public StringBuffer slabOptions;
  
  
  public static Object[] getSlabWithinRange(float min, float max) {
    return new Object[] { Integer.valueOf(Token.range), 
        new Float[] {Float.valueOf(min), Float.valueOf(max)}, Boolean.FALSE };
  }

  public static Object[] getSlabObject(int tok, Object data, boolean isCap) {
    return new Object[] { Integer.valueOf(tok), data, Boolean.valueOf(isCap) };
  }

  public void slabPolygons(List<Object[]> slabInfo) {
    for (int i = 0; i < slabInfo.size(); i++)
      if (!slabPolygons(slabInfo.get(i)))
          break;
  }
  
  public boolean slabPolygons(Object[] slabObject) {
    if (polygonCount0 < 0)
      return false; // disabled for some surface types
    int slabType = ((Integer) slabObject[0]).intValue();
    Object slabbingObject = slabObject[1];
    if (slabType == Token.none) {
      if (bsSlabDisplay != null && polygonCount0 != 0) {
        polygonCount = polygonCount0;
        vertexCount = vertexCount0;
        polygonCount0 = vertexCount0 = 0;
        bsSlabDisplay.set(0, (polygonCount == 0 ? vertexCount : polygonCount));
        slabOptions = new StringBuffer(" slab none");
      }
      return false;
    }
    if (polygonCount0 == 0 || vertexCount0 == 0) {
      polygonCount0 = polygonCount;
      vertexCount0 = vertexCount;
      bsSlabDisplay = BitSetUtil.setAll(polygonCount == 0 ? vertexCount
          : polygonCount);
      if (polygonCount == 0 && vertexCount == 0)
        return false;
    }

    boolean andCap = ((Boolean) slabObject[2]).booleanValue();

    StringBuffer sb = new StringBuffer();
    sb.append(andCap ? " cap " : " slab ");

    switch (slabType) {
    case Token.plane:
      Point4f plane = (Point4f) slabbingObject;
      sb.append(Escape.escape(plane));
      getIntersection(0, plane, null, null, null, andCap, false, Token.plane);
      break;
    case Token.boundbox:
      Point3f[] box = (Point3f[]) slabbingObject;
      sb.append("within ").append(Escape.escape(box));
      Point4f[] faces = BoxInfo.getFacesFromCriticalPoints(box);
      for (int i = 0; i < faces.length; i++)
        getIntersection(0, faces[i], null, null, null, andCap, false,
            Token.plane);
      break;
    case Token.within:
    case Token.range:
    case Token.mesh:
      Object[] o = (Object[]) slabbingObject;
      float distance = ((Float) o[0]).floatValue();
      switch (slabType) {
      case Token.within:
        Point3f[] points = (Point3f[]) o[1];
        BitSet bs = (BitSet) o[2];
        sb.append("within ").append(
            bs == null ? Escape.escape(points) : Escape.escape(bs));
        getIntersection(distance, null, points, null, null, andCap, false,
            (distance > 0 ? Token.distance : Token.sphere));
        break;
      case Token.range:
        // isosurface slab within range x.x y.y
        // if y.y < x.x then this effectively means "NOT within range y.y x.x"
        if (vertexValues == null)
          return false;
        float distanceMax = ((Float) o[1]).floatValue();
        sb.append("within range ").append(distance).append(" ").append(
            distanceMax);
        bs = (distanceMax < distance ? BitSetUtil.copy(bsSlabDisplay) : null);
        getIntersection(distance, null, null, null, null, andCap, false,
            Token.min);
        BitSet bsA = (bs == null ? null : BitSetUtil.copy(bsSlabDisplay));
        BitSetUtil.copy(bs, bsSlabDisplay);
        getIntersection(distanceMax, null, null, null, null, andCap, false,
            Token.max);
        if (bsA != null)
          bsSlabDisplay.or(bsA);
        break;
      case Token.mesh:
        MeshSurface mesh = (MeshSurface) o[1];
        //distance = -1;
        getIntersection(0, null, null, null, mesh, andCap, false,
            distance < 0 ? Token.min : Token.max);
        //TODO: unresolved how exactly to store this in the state
        // -- must indicate exact set of triangles to slab and how!
        break;
      }
      break;
    }
    String newOptions = sb.toString();
    if (slabOptions == null)
      slabOptions = new StringBuffer();
    if (slabOptions.indexOf(newOptions) < 0)
      slabOptions.append(newOptions);
    return true;
  }

  public void getIntersection(float distance, Point4f plane,
                                 Point3f[] ptCenters, List<Point3f[]> vData,
                                 MeshSurface meshSurface, boolean andCap,
                                 boolean doClean, int tokType) {
    Vector3f vNorm = null;
    Vector3f vBC = null;
    Vector3f vAC = null;
    Point3f[] pts = null;
    Point3f[] pts2 = null;
    Vector3f vTemp3 = null;
    Point4f planeTemp = null;
    boolean isSlab = (vData == null);
    boolean isMeshIntersect = (meshSurface != null);
    if (isMeshIntersect) {
      vBC = new Vector3f();
      vAC = new Vector3f();
      vNorm = new Vector3f();
      plane = new Point4f();
      planeTemp = new Point4f();
      vTemp3 = new Vector3f();
      pts2 = new Point3f[] { null, new Point3f(), new Point3f() };
    }
    if (ptCenters != null)
      andCap = false; // can only cap faces
    float[] values = new float[2];
    float[] fracs = new float[2];
    int iD, iE;
    double absD = Math.abs(distance);
    float d1, d2, d3, valA, valB, valC;
    int sourceA = 0, sourceB = 0, sourceC = 0;
    List<int[]> iPts = (andCap ? new ArrayList<int[]>() : null);
    if (polygonCount == 0) {
      for (int i = 0; i < vertexCount; i++) {
        if (Float.isNaN(vertexValues[i]) || checkSlab(tokType, vertices[i], vertexValues[i], distance, plane, ptCenters) > 0)
          bsSlabDisplay.clear(i);
      }
      return;
    }
    int iLast = polygonCount;
    for (int i = 0; i < iLast; i++) {
      if (!setABC(i))
        continue;
      int check1 = polygonIndexes[i][3];
      int check2 = (checkCount == 2 ? polygonIndexes[i][4] : 0);
      Point3f vA = vertices[iA];
      Point3f vB = vertices[iB];
      Point3f vC = vertices[iC];
      valA = vertexValues[iA];
      valB = vertexValues[iB];
      valC = vertexValues[iC];
      if (vertexSource != null) {
        sourceA = vertexSource[iA];
        sourceB = vertexSource[iB];
        sourceC = vertexSource[iC];
      }
      d1 = checkSlab(tokType, vA, valA, distance, plane, ptCenters);
      d2 = checkSlab(tokType, vB, valB, distance, plane, ptCenters);
      d3 = checkSlab(tokType, vC, valC, distance, plane, ptCenters);
      int test1 = (d1 != 0 && d1 < 0 ? 1 : 0) + (d2 != 0 && d2 < 0 ? 2 : 0)
          + (d3 != 0 && d3 < 0 ? 4 : 0);

      // testing -- just looking at new set
      //      if (test1 == 7)
      //      test1 = 0;

      if (isMeshIntersect && test1 != 7 && test1 != 0) {
        //System.out.println(d1 + " " + d2 + " " + d3);
        boolean isOK = (d1 == 0 && d2 * d3 >= 0 || d2 == 0 && (d1 * d3) >= 0 || d3 == 0
            && d1 * d2 >= 0);
        if (isOK)
          continue;
        // We have a potential crossing. Now to find the exact point of crossing
        // the other isosurface.
        if (checkIntersection(vA, vB, vC, meshSurface, pts2, vNorm, vBC, vAC,
            plane, planeTemp, vTemp3)) {
          iD = addVertexCopy(pts2[0], 0, sourceA); // have to choose some source
          //System.out.println("draw d" + iD + " " + Escape.escape(pts2[0]));
          addTriangleCheck(iA, iB, iD, check1 & 1, check2, 0);
          addTriangleCheck(iD, iB, iC, check1 & 2, check2, 0);
          addTriangleCheck(iA, iD, iC, check1 & 4, check2, 0);
          test1 = 0; // toss original    
          iLast = polygonCount;
        } else {
          //if (i == 7490)
          //System.out.println("draw s"  + i + " " + Escape.escape(vA)+ " " + Escape.escape(vB)+ " " + Escape.escape(vC) + " color red");
          // process normally for now  
          // not fully implemented -- need to check other way as well.
        }
      }
      switch (test1) {
      default:
      case 7:
      case 0:
        // all on the same side
        break;
      case 1:
      case 6:
        // BC on same side
        if (ptCenters == null)
          pts = new Point3f[] {
              interpolatePoint(vA, vB, -d1, d2, valA, valB, values, fracs, 0),
              interpolatePoint(vA, vC, -d1, d3, valA, valC, values, fracs, 1) };
        else
          pts = new Point3f[] {
              interpolateSphere(vA, vB, -d1, -d2, absD, valA, valB, values,
                  fracs, 0),
              interpolateSphere(vA, vC, -d1, -d3, absD, valA, valC, values,
                  fracs, 1) };
        break;
      case 2:
      case 5:
        //AC on same side
        if (ptCenters == null)
          pts = new Point3f[] {
              interpolatePoint(vB, vA, -d2, d1, valB, valA, values, fracs, 0),
              interpolatePoint(vB, vC, -d2, d3, valB, valC, values, fracs, 1) };
        else
          pts = new Point3f[] {
              interpolateSphere(vB, vA, -d2, -d1, absD, valB, valA, values,
                  fracs, 0),
              interpolateSphere(vB, vC, -d2, -d3, absD, valB, valC, values,
                  fracs, 1) };
        break;
      case 3:
      case 4:
        //AB on same side need A-C, B-C
        if (ptCenters == null)
          pts = new Point3f[] {
              interpolatePoint(vC, vA, -d3, d1, valC, valA, values, fracs, 0),
              interpolatePoint(vC, vB, -d3, d2, valC, valB, values, fracs, 1) };
        else
          pts = new Point3f[] {
              interpolateSphere(vC, vA, -d3, -d1, absD, valC, valA, values,
                  fracs, 0),
              interpolateSphere(vC, vB, -d3, -d2, absD, valC, valB, values,
                  fracs, 1) };
        break;
      }
      boolean isSlabbed = true;
      if (isSlab) {
        iD = iE = -1;
        //             A
        //            / \
        //           B---C
        switch (test1) {
        case 0:
          // all on the same side -- toss
          break;
        case 7:
          // all on the same side -- keep
          continue;
        case 6:
          // BC on side to keep
          if (fracs[0] == 0 || fracs[1] == 0)
            continue;
          if (fracs[0] == 1)
            iE = iB;
          if (fracs[1] == 1)
            iD = iC;
          if (iD >= 0 && iE >= 0) {
            isSlabbed = false;
            break;
          }
          if (iE < 0) {
            iE = addVertexCopy(pts[0], values[0], sourceB); //AB
            addTriangleCheck(iE, iB, iC, check1 & 3, check2, 0);
          }
          if (iD < 0) {
            iD = addVertexCopy(pts[1], values[1], sourceC); //AC
            addTriangleCheck(iD, iE, iC, check1 & 4 | 1, check2, 0);
          }
          break;
        case 5:
          // AC on side to keep
          if (fracs[0] == 0 || fracs[1] == 0)
            continue;
          if (fracs[0] == 1)
            iD = iA;
          if (fracs[1] == 1)
            iE = iC;
          if (iD >= 0 && iE >= 0) {
            isSlabbed = false;
            break;
          }
          if (iD < 0) {
            iD = addVertexCopy(pts[0], values[0], sourceA); //BA
            addTriangleCheck(iA, iD, iC, check1 & 5, check2, 0);
          }
          if (iE < 0) {
            iE = addVertexCopy(pts[1], values[1], sourceC); //BC
            addTriangleCheck(iD, iE, iC, check1 & 2 | 1, check2, 0);
          }
          break;
        case 3:
          //AB on side to keep
          if (fracs[0] == 0 || fracs[1] == 0)
            continue;
          if (fracs[0] == 1)
            iE = iA;
          if (fracs[1] == 1)
            iD = iB;
          if (iE >= 0 && iD >= 0) {
            isSlabbed = false;
            break;
          }
          if (iE < 0) {
            iE = addVertexCopy(pts[0], values[0], sourceA); //CA
            addTriangleCheck(iA, iB, iE, check1 & 5, check2, 0);
          }
          if (iD < 0) {
            iD = addVertexCopy(pts[1], values[1], sourceB); //CB
            addTriangleCheck(iE, iB, iD, check1 & 2 | 4, check2, 0);
          }
          break;
        case 4:
          //AB on side to toss
          if (fracs[0] == 0 || fracs[1] == 0) {
            isSlabbed = false;
            break;
          }
          if (fracs[0] == 1)
            iD = iA;
          if (fracs[1] == 1)
            iE = iB;
          if (iD >= 0 && iE >= 0)
            continue;
          if (iD < 0)
            iD = addVertexCopy(pts[0], values[0], sourceC); //CA
          if (iE < 0)
            iE = addVertexCopy(pts[1], values[1], sourceC); //CB
          addTriangleCheck(iD, iE, iC, check1 & 6 | 1, check2, 0);
          break;
        case 2:
          //AC on side to toss
          if (fracs[0] == 0 || fracs[1] == 0) {
            isSlabbed = false;
            break;
          }
          if (fracs[0] == 1)
            iE = iA;
          if (fracs[1] == 1)
            iD = iC;
          if (iD >= 0 && iE >= 0)
            continue;
          if (iE < 0)
            iE = addVertexCopy(pts[0], values[0], sourceB); //BA
          if (iD < 0)
            iD = addVertexCopy(pts[1], values[1], sourceB); //BC
          addTriangleCheck(iE, iB, iD, check1 & 3 | 4, check2, 0);
          break;
        case 1:
          // BC on side to toss
          if (fracs[0] == 0 || fracs[1] == 0) {
            isSlabbed = false;
            break;
          }
          if (fracs[0] == 1)
            iD = iB;
          if (fracs[1] == 1)
            iE = iC;
          if (iD >= 0 && iE >= 0)
            continue;
          if (iD < 0)
            iD = addVertexCopy(pts[0], values[0], sourceA); //AB
          if (iE < 0)
            iE = addVertexCopy(pts[1], values[1], sourceA); //AC
          addTriangleCheck(iA, iD, iE, check1 & 5 | 2, check2, 0);
          break;
        }
        bsSlabDisplay.clear(i);
        if (andCap && isSlabbed)
          iPts.add(new int[] { iD, iE });
      } else if (pts != null) {
        vData.add(pts);
      }
    }
    if (andCap && iPts.size() > 0) {
      Point3f center = new Point3f();
      for (int i = iPts.size(); --i >= 0;) {
        int[] ipts = iPts.get(i);
        center.add(vertices[ipts[0]]);
        center.add(vertices[ipts[1]]);
      }
      center.scale(0.5f / iPts.size());
      int v0 = addVertexCopy(center);
      for (int i = iPts.size(); --i >= 0;) {
        int[] ipts = iPts.get(i);
        addTriangleCheck(ipts[0], v0, ipts[1], 0, 0, 0);
      }
    }
    if (!doClean)
      return;
    BitSet bsv = new BitSet();
    BitSet bsp = new BitSet();
    for (int i = 0; i < polygonCount; i++) {
      if (polygonIndexes[i] == null)
        continue;
      bsp.set(i);
      for (int j = 0; j < 3; j++)
        bsv.set(polygonIndexes[i][j]);
    }
    int n = 0;
    int nPoly = bsp.cardinality();
    if (nPoly != polygonCount) {
      int[] map = new int[vertexCount];
      for (int i = 0; i < vertexCount; i++)
        if (bsv.get(i))
          map[i] = n++;
      Point3f[] vTemp = new Point3f[n];
      n = 0;
      for (int i = 0; i < vertexCount; i++)
        if (bsv.get(i))
          vTemp[n++] = vertices[i];
      int[][] pTemp = new int[nPoly][];
      nPoly = 0;
      for (int i = 0; i < polygonCount; i++)
        if (polygonIndexes[i] != null) {
          for (int j = 0; j < 3; j++)
            polygonIndexes[i][j] = map[polygonIndexes[i][j]];
          pTemp[nPoly++] = polygonIndexes[i];
        }
      vertices = vTemp;
      vertexCount = n;
      polygonIndexes = pTemp;
      polygonCount = nPoly;
    }
  }

  private static float checkSlab(int tokType, Point3f v, float val, float distance,
                          Point4f plane, Point3f[] ptCenters) {
    switch (tokType) {
    case Token.min:
      return distance - val;
    case Token.max:
      return val - distance;
    case Token.plane:
      return Measure.distanceToPlane(plane, v);
    case Token.distance:
      return minDist(v, ptCenters) - distance;
    default:  
      return -minDist(v, ptCenters) - distance;
    }
  }

  private static boolean checkIntersection(Point3f vA, Point3f vB, Point3f vC,
                                    MeshSurface meshSurface, Point3f[] pts, 
                                    Vector3f vNorm, Vector3f vAB, Vector3f vAC, Point4f plane, Point4f pTemp, Vector3f vTemp3) {
    
    Measure.getPlaneThroughPoints(vA, vB, vC, vNorm, vAB, vAC, plane);
    for (int i = 0; i < meshSurface.polygonCount; i++) {
      Point3f pt = meshSurface.getTriangleIntersection(i, vA, vB, vC, plane, vNorm, vAB, pts[1], pts[2], vAC, pTemp, vTemp3);
      if (pt != null) {
        pts[0] = new Point3f(pt);
        return true; 
      }
    }
    return false;
  }

  
  private Point3f getTriangleIntersection(int i, Point3f vA, Point3f vB, Point3f vC, Point4f plane, Vector3f vNorm, Vector3f vTemp, 
                                          Point3f ptRet, Point3f ptTemp, Vector3f vTemp2, Point4f pTemp, Vector3f vTemp3) {
    return (setABC(i) ? Measure.getTriangleIntersection(vA, vB, vC, plane, vertices[iA], vertices[iB], vertices[iC], vNorm, vTemp, ptRet, ptTemp, vTemp2, pTemp, vTemp3) : null);
  }

  private static float minDist(Point3f pt, Point3f[] ptCenters) {
    float dmin = Integer.MAX_VALUE;
    for (int i = ptCenters.length; --i >= 0;) {
      float d = ptCenters[i].distance(pt);
      if (d < dmin)
        dmin = d;
    }
    return dmin;
  }

  private Point3f interpolateSphere(Point3f v1, Point3f v2, float d1, float d2,
                                    double absD, float val1, float val2, float[] values, float[] fracs, int i) {
    return interpolateFraction(v1, v2, getSphericalInterpolationFraction(absD, d1,
        d2, v1.distance(v2)), val1, val2, values, fracs, i);
  }

  private static Point3f interpolatePoint(Point3f v1, Point3f v2, float d1, float d2, float val1, float val2, float[] values, float[] fracs, int i) {
    return interpolateFraction(v1, v2, d1 / (d1 + d2), val1, val2, values, fracs, i);
  }

  private static Point3f interpolateFraction(Point3f v1, Point3f v2, float f, float val1, float val2, float[] values, float[] fracs, int i) {
    if (f < 0.0001)
      f = 0;
    else if (f > 0.9999)
      f = 1;
    fracs[i] = f;
    values[i] = (val2 - val1) * f + val1;
    return new Point3f(v1.x + (v2.x - v1.x) * f, 
        v1.y + (v2.y - v1.y) * f, 
        v1.z + (v2.z - v1.z) * f);
  }

  public static float getSphericalInterpolationFraction(double r, double valueA,
                                                      double valueB, double d) {
    double ra = Math.abs(r + valueA) / d;
    double rb = Math.abs(r + valueB) / d;
    r /= d;
    double ra2 = ra * ra;
    double q = ra2 - rb * rb + 1;
    double p = 4 * (r * r - ra2);
    double factor = (ra < rb ? 1 : -1);
    return (float) (((q) + factor * Math.sqrt(q * q + p)) / 2);
  }

}
