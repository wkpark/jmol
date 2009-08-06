/* $RCSfile$
 * $Author: aherraez $
 * $Date: 2009-01-15 21:00:00 +0100 (Thu, 15 Jan 2009) $
 * $Revision: 7752 $

 *
 * Copyright (C) 2003-2009  The Jmol Development Team
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

/*	Based on _VrmlExporter  by rhanson
		and Help from http://x3dgraphics.com/examples/X3dForWebAuthors/index.html
*/
 
package org.jmol.export;

import java.awt.Image;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.Geodesic;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.util.Quaternion;

public class _IdtfExporter extends _Exporter {

  /*
   * by Bob Hanson 8/6/2009 -- preliminary only -- needs testing
   * 
   * after 
   * 
   * write t.idtf
   * 
   * using IDTFConverter.exe, on Windows one can turn these files into VERY COMPACT U3D files.
   * 
   * IDTFConverter.exe -input t.idtf -output t.u3d
   * 
   * see http://sourceforge.net/projects/u3d/
   * see http://en.wikipedia.org/wiki/Universal_3D
   * see http://www.ecma-international.org/publications/standards/Ecma-363.htm
   * in the downloadable zip file, see docs/IntermediateFormat/IDTF Format Description.pdf
   * 
   */
  private AxisAngle4f viewpoint = new AxisAngle4f();
  private boolean haveSphere;
  private boolean haveCylinder;
  private boolean haveCone;
  private boolean haveCircle;
  
  public _IdtfExporter() {
    use2dBondOrderCalculation = true;
    canDoTriangles = false;
    isCartesianExport = true;
  }

  private void output(String data) {
    output.append(data);
  }

  private void output(Tuple3f pt, StringBuffer sb) {
    sb.append(round(pt.x)).append(" ").append(round(pt.y)).append(" ").append(round(pt.z)).append(" ");
  }
  
  private int iObj;
  private Hashtable htDefs = new Hashtable();
  
  final private Point3f ptAtom = new Point3f();
  final private Matrix4f m = new Matrix4f();

  final private StringBuffer models = new StringBuffer();
  final private StringBuffer resources = new StringBuffer();
  final private StringBuffer shading = new StringBuffer();

  public void getHeader() {
    // next is an approximation only 
    getViewpointPosition(ptAtom);
    adjustViewpointPosition(ptAtom);
    viewer.getAxisAngle(viewpoint);
    if (viewpoint.angle == 0)
      viewpoint.y = 1;
    Matrix3f m3 = new Matrix3f();
    m3.set(viewpoint);
    Quaternion q = new Quaternion(new Point4f(0.48537543f, -0.38002273f, -0.4436077f, 0.6505426f));
    Quaternion q2 = new Quaternion(m3);
    q2 = q.mul(q2);
    viewpoint.set(q2.getMatrix());
    m.set(viewpoint);
    ptAtom.set(center);
    ptAtom.scale(-1);
    m.m03 = ptAtom.x;
    m.m13 = ptAtom.y;
    m.m23 = ptAtom.z;
    m.m33 = 1;
    
    output("FILE_FORMAT \"IDTF\"\nFORMAT_VERSION 100\n");
    
    /* the view idea did not work -- no default view??
     * 
    float angle = getFieldOfView();
    output("NODE \"VIEW\" {\n");
    output("NODE_NAME \"DefaultView\"\n");
    output("PARENT_LIST {\nPARENT_COUNT 1\n"); 
    output("PARENT 0 {\n");
    output(getParentItem("", m));
    output("}}\n"); 
    output("RESOURCE_NAME \"View0\"\n"); 
    output("VIEW_DATA {\n"); 
    output("VIEW_ATTRIBUTE_SCREEN_UNIT \"PIXEL\"\n"); 
    output("VIEW_TYPE \"PERSPECTIVE\"\n"); 
    output("VIEW_PROJECTION " + (angle * 180 / Math.PI) + "\n"); 
    output("}}\n");
    resources.append("RESOURCE_LIST \"VIEW\" {\n");
    resources.append("\tRESOURCE_COUNT 1\n");
    resources.append("\tRESOURCE 0 {\n");
    resources.append("\t\tRESOURCE_NAME \"View0\"\n");
    resources.append("\t\tVIEW_PASS_COUNT 1\n");
    resources.append("\t\tVIEW_ROOT_NODE_LIST {\n");
    resources.append("\t\t\tROOT_NODE 0 {\n");
    resources.append("\t\t\t\tROOT_NODE_NAME \"\"\n");
    resources.append("\t\t\t}\n");
    resources.append("\t\t}\n");
    resources.append("\t}\n");
    resources.append("}\n\n");
     */

    /* not ideal */

    output("NODE \"GROUP\" {\n");
    output("NODE_NAME \"jmol\"\n");
    output("PARENT_LIST {\nPARENT_COUNT 1\n"); 
    output("PARENT 0 {\n");
    output(getParentItem("", m));
    output("}}}\n");
    
  }

  private String getParentItem(String name, Matrix4f m) {
    StringBuffer sb= new StringBuffer();
    sb.append("PARENT_NAME \"" + name + "\"\n");
    sb.append("PARENT_TM {\n");
    sb.append(m.m00 + " " + m.m10 + " " + m.m20 + " 0.0\n");
    sb.append(m.m01 + " " + m.m11 + " " + m.m21 + " 0.0\n");
    sb.append(m.m02 + " " + m.m12 + " " + m.m22 + " 0.0\n");
    sb.append(m.m03 + " " + m.m13 + " " + m.m23 + " " + m.m33 + "\n");
    sb.append("}\n");
    return sb.toString();
  }

  private void addColix(short colix, boolean haveColors) {
    String key = "_" + colix;
    if (htDefs.containsKey(key))
      return;
    String color = (haveColors ? "1.0 1.0 1.0" : rgbFractionalFromColix(colix, ' '));
    htDefs.put(key, Boolean.TRUE);
    resources.append("RESOURCE_LIST \"SHADER\" {\n");
    resources.append("RESOURCE_COUNT 1\n");
    resources.append("RESOURCE 0 {\n");
    resources.append("RESOURCE_NAME \"Shader" + key + "\"\n");
    resources.append("ATTRIBUTE_USE_VERTEX_COLOR \"FALSE\"\n");
    resources.append("SHADER_MATERIAL_NAME \"Mat" + key +"\"\n");
    resources.append("SHADER_ACTIVE_TEXTURE_COUNT 0\n");
    resources.append("}}\n");
    resources.append("RESOURCE_LIST \"MATERIAL\" {\n");
    resources.append("RESOURCE_COUNT 1\n");
    resources.append("RESOURCE 0 {\n");
    resources.append("RESOURCE_NAME \"Mat" + key + "\"\n");
    resources.append("MATERIAL_AMBIENT " + color + "\n");
    resources.append("MATERIAL_DIFFUSE " + color + "\n");
    resources.append("MATERIAL_SPECULAR 0.0 0.0 0.0\n");
    resources.append("MATERIAL_EMISSIVE 0.0 0.0 0.0\n");
    resources.append("MATERIAL_REFLECTIVITY 0.00000\n");
    resources.append("MATERIAL_OPACITY " + opacityFractionalFromColix(colix) + "\n");
    resources.append("}}\n");
  }
  
  private void addShader(String key, short colix) {
    shading.append("MODIFIER \"SHADING\" {\n");
    shading.append("MODIFIER_NAME \"" + key + "\"\n");
    shading.append("PARAMETERS {\n");
    shading.append("SHADER_LIST_COUNT 1\n");
    shading.append("SHADING_GROUP {\n");
    shading.append("SHADER_LIST 0 {\n");
    shading.append("SHADER_COUNT 1\n");
    shading.append("SHADER_NAME_LIST {\n");
    shading.append("SHADER 0 NAME: \"Shader_" + colix +"\"\n");
    shading.append("}}}}}\n");
  }

  public void getFooter() {
    htDefs = null;
    outputNodes();
    output(models.toString());
    output(resources.toString());    
    output(shading.toString());    
  }

  private Hashtable htNodes = new Hashtable();
  
  private void outputNodes() {
    Enumeration e = htNodes.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      Vector v = (Vector) htNodes.get(key);
      output("NODE \"MODEL\" {\n");
      output("NODE_NAME \"" + key + "\"\n");
      int n = v.size();
      output("PARENT_LIST {\nPARENT_COUNT " + n + "\n"); 
      for (int i = 0; i < n; i++) {
        output("PARENT " + i + " {\n");
        output((String)v.get(i));
        output("}\n");
      }
      output("}\n");
      int i = key.indexOf("_");
      if (i > 0)
        key = key.substring(0,i);
      output("RESOURCE_NAME \"" + key + "_Mesh\"\n}\n");
    }
  }

  public void renderAtom(Atom atom, short colix) {
    float r = atom.getMadAtom() / 2000f;
    outputSphere(atom, r, colix);
  }

  public void drawPixel(short colix, int x, int y, int z) {
    // dots
    pt.set(x, y, z);
    viewer.unTransformPoint(pt, ptAtom);
    outputSphere(ptAtom, 0.02f, colix);
  }
    
  public void fillSphereCentered(short colix, int diameter, Point3f pt) {
    viewer.unTransformPoint(pt, ptAtom);
    outputSphere(ptAtom, viewer.unscaleToScreen((int)pt.z, diameter) / 2, colix);
  }

  private Matrix4f sphereMatrix = new Matrix4f();
  private Matrix4f cylinderMatrix = new Matrix4f();

  private void outputSphere(Point3f center, float radius, short colix) {
    outputEllipsoid(center, radius, radius, radius, null, colix);
  }
  private void outputEllipsoid(Point3f center, float rx, float ry, float rz, AxisAngle4f a, short colix) {
    if (!haveSphere) {
      models.append(getSphereResource());
      haveSphere = true;
      sphereMatrix = new Matrix4f();
    }
    addColix(colix, false);
    String key = "Sphere_" + colix;
    Vector v = (Vector) htNodes.get(key);
    if (v == null) {
      v = new Vector();
      htNodes.put(key, v);
      addShader(key, colix);
    }
    if (a != null) {
      Matrix3f mq = new Matrix3f();
      Matrix3f m = new Matrix3f();
      m.m00 = rx;
      m.m11 = ry;
      m.m22 = rz;
      mq.set(a);
      mq.mul(m);
      sphereMatrix.set(mq);
    } else {
      sphereMatrix.setIdentity();
      sphereMatrix.m00 = rx;
      sphereMatrix.m11 = ry;
      sphereMatrix.m22 = rz;
    }
    sphereMatrix.m03 = center.x;
    sphereMatrix.m13 = center.y;
    sphereMatrix.m23 = center.z;
    sphereMatrix.m33 = 1;
    v.add(getParentItem("jmol", sphereMatrix));
  }

  private String getSphereResource() {
    StringBuffer sb = new StringBuffer();
    sb.append("RESOURCE_LIST \"MODEL\" {\n")
    .append("RESOURCE_COUNT 1\n")
    .append("RESOURCE 0 {\n")
    .append("RESOURCE_NAME \"Sphere_Mesh\"\n")
    .append("MODEL_TYPE \"MESH\"\n")
    .append("MESH {\n");
    int vertexCount = Geodesic.getVertexCount(2);
    short[] f = Geodesic.getFaceVertexes(2);
    int[] faces = new int[f.length];
    for (int i = 0; i < f.length; i++)
      faces[i] = f[i];
    Vector3f[] vertexes = new Vector3f[vertexCount];
    for (int i = 0; i < vertexCount;i++)
      vertexes[i] = Geodesic.getVertexVector(i);
    return getMeshData("Sphere", faces, vertexes, vertexes);
  }

  private String getMeshData(String type, int[] faces, Tuple3f[] vertexes, Tuple3f[] normals) {
    int nFaces = faces.length / 3;
    int vertexCount = vertexes.length;
    int normalCount = normals.length;
    StringBuffer sb = new StringBuffer();
    getMeshHeader(type, nFaces, vertexCount, normalCount, 0, sb);
    sb.append("MESH_FACE_POSITION_LIST { ");
    for (int i = 0; i < faces.length; i++)
      sb.append(faces[i]).append(" ");
    sb.append("}\n");
    sb.append("MESH_FACE_NORMAL_LIST { ");
    for (int i = 0; i < faces.length; i++)
      sb.append(faces[i]).append(" ");
    sb.append("}\n");
    sb.append("MESH_FACE_SHADING_LIST { ");
    for (int i = 0; i < nFaces; i++)
      sb.append("0 ");
    sb.append("}\n");
    sb.append("MODEL_POSITION_LIST { ");
    for (int i = 0; i < vertexCount; i++)
      output(vertexes[i], sb);
    sb.append("}\n");
    sb.append("MODEL_NORMAL_LIST { ");
    for (int i = 0; i < normalCount; i++)
      output(normals[i], sb);
    sb.append("}\n}}}\n");
    return sb.toString();
  }

  private void getMeshHeader(String type, int nFaces, int vertexCount, int normalCount,
                             int colorCount, StringBuffer sb) {
    sb.append("RESOURCE_LIST \"MODEL\" {\n")
        .append("RESOURCE_COUNT 1\n")
        .append("RESOURCE 0 {\n")
        .append("RESOURCE_NAME \"").append(type).append("_Mesh\"\n")
        .append("MODEL_TYPE \"MESH\"\n")
        .append("MESH {\n")
        .append("FACE_COUNT ").append(nFaces).append("\n")
        .append("MODEL_POSITION_COUNT ").append(vertexCount).append("\n")
        .append("MODEL_NORMAL_COUNT ").append(normalCount).append("\n")
        .append("MODEL_DIFFUSE_COLOR_COUNT ").append(colorCount).append("\n")
        .append("MODEL_SPECULAR_COLOR_COUNT 0\n")
        .append("MODEL_TEXTURE_COORD_COUNT 0\n")
        .append("MODEL_BONE_COUNT 0\n")
        .append("MODEL_SHADING_COUNT 1\n")
        .append("MODEL_SHADING_DESCRIPTION_LIST {\n")
          .append("SHADING_DESCRIPTION 0 {\n")
           .append("TEXTURE_LAYER_COUNT 0\n")
           .append("SHADER_ID 0\n}}\n");
  }

  private final Point3f pt2 = new Point3f();
  public void fillCylinder(Point3f ptA, Point3f ptB, short colix1,
                           short colix2, byte endcaps, int diameter,
                           int bondOrder) {
    if (bondOrder == -1) {
      // really first order -- but actual coord
      ptAtom.set(ptA);
      pt2.set(ptB);
    } else {
      viewer.unTransformPoint(ptA, ptAtom);
      viewer.unTransformPoint(ptB, pt2);
    }
    int madBond = diameter;
    if (madBond < 20)
      madBond = 20;
    if (colix1 == colix2) {
      outputCylinder(ptAtom, pt2, colix1, endcaps, madBond);
    } else {
      tempV2.set(pt2);
      tempV2.add(ptAtom);
      tempV2.scale(0.5f);
      pt.set(tempV2);
      outputCylinder(ptAtom, pt, colix1, Graphics3D.ENDCAPS_FLAT, madBond);
      outputCylinder(pt, pt2, colix2, Graphics3D.ENDCAPS_FLAT, madBond);
      if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
        outputSphere(ptAtom, madBond / 2000f*1.01f, colix1);
        outputSphere(pt2, madBond / 2000f*1.01f, colix2);
      }
    }
  }

  private void outputCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, int madBond) {
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, madBond / 2000f*1.01f, colix);
      outputSphere(pt2, madBond / 2000f*1.01f, colix);
    } else if (endcaps == Graphics3D.ENDCAPS_FLAT) {
      outputCircle(pt1, pt2, colix, madBond);      
      outputCircle(pt2, pt1, colix, madBond);      
    }
    if (!haveCylinder) {
      models.append(getCylinderResource());
      haveCylinder = true;
      cylinderMatrix = new Matrix4f();
    }
    addColix(colix, false);
    String key = "Cylinder_" + colix;
    Vector v = (Vector) htNodes.get(key);
    if (v == null) {
      v = new Vector();
      htNodes.put(key, v);
      addShader(key, colix);
    }
    float radius = madBond / 2000f;
    cylinderMatrix.set(getRotationMatrix(pt1, pt2, radius));
    cylinderMatrix.m03 = pt1.x;
    cylinderMatrix.m13 = pt1.y;
    cylinderMatrix.m23 = pt1.z;
    cylinderMatrix.m33 = 1;
    v.add(getParentItem("jmol", cylinderMatrix));
  }

  private void outputCircle(Point3f ptCenter, Point3f ptPerp, short colix, int madBond) {
    if (!haveCircle) {
      models.append(getCircleResource());
      haveCircle = true;
      cylinderMatrix = new Matrix4f();
    }
    addColix(colix, false);
    String key = "Circle_" + colix;
    Vector v = (Vector) htNodes.get(key);
    if (v == null) {
      v = new Vector();
      htNodes.put(key, v);
      addShader(key, colix);
    }
    float radius = madBond / 2000f;
    cylinderMatrix.set(getRotationMatrix(ptCenter, ptPerp, radius));
    cylinderMatrix.m03 = ptCenter.x;
    cylinderMatrix.m13 = ptCenter.y;
    cylinderMatrix.m23 = ptCenter.z;
    cylinderMatrix.m33 = 1;
    v.add(getParentItem("jmol", cylinderMatrix));
  }

  private Matrix3f getRotationMatrix(Point3f pt1, Point3f pt2, float radius) {    
    Matrix3f m = new Matrix3f();
    Matrix3f m1;
    if (pt2.x == pt1.x && pt2.y == pt1.y) {
      m1 = new Matrix3f();
      m1.setIdentity();
    } else {
      tempV1.set(pt2);
      tempV1.sub(pt1);
      tempV2.set(0, 0, 1);
      tempV2.cross(tempV2, tempV1);
      tempV1.cross(tempV1, tempV2);
      Quaternion q = Quaternion.getQuaternionFrame(tempV2, tempV1, null);
      m1 = q.getMatrix();
    }
    m.m00 = radius;
    m.m11 = radius;
    m.m22 = pt2.distance(pt1);
    m1.mul(m);
    return m1;
  }

  private Object getCylinderResource() {
    int ndeg = 10;
    int vertexCount = 360 / ndeg * 2;
    int n = vertexCount / 2;
    int[] faces = new int[vertexCount * 3];
    int fpt = -1;
    for (int i = 0; i < n; i++) {
      faces[++fpt] = i;
      faces[++fpt] = (i + 1) % n;
      faces[++fpt] = (i + n);
      faces[++fpt] = (i + 1) % n;
      faces[++fpt] = (i + 1) % n + n;
      faces[++fpt] = (i + n);
    }
    Point3f[] vertexes = new Point3f[vertexCount];
    Point3f[] normals = new Point3f[vertexCount];
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos(i * ndeg / 180. * Math.PI)); 
      float y = (float) (Math.sin(i * ndeg / 180. * Math.PI)); 
      normals[i] = vertexes[i] = new Point3f(x, y, 0);
    }
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos((i + 0.5) * ndeg / 180 * Math.PI)); 
      float y = (float) (Math.sin((i + 0.5) * ndeg / 180 * Math.PI)); 
      vertexes[i + n] = new Point3f(x, y, 1);
      normals[i + n] = new Point3f(x, y, 0);
    }
    return getMeshData("Cylinder", faces, vertexes, normals);
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                               short[] colixes, Vector3f[] normals,
                               int[][] indices, BitSet bsFaces, int nVertices,
                               int faceVertexMax, short[] polygonColixes,
                               int nPolygons) {
    if (nVertices == 0)
      return;
    int nFaces = 0;
    for (int i = nPolygons; --i >= 0;)
      if (bsFaces.get(i))
        nFaces += (faceVertexMax == 4 && indices[i].length == 4 ? 2 : 1);
    if (nFaces == 0)
      return;

    Vector colorList = null;
    Hashtable htColixes = new Hashtable();
    if (polygonColixes != null)
      colorList = getColorList(0, polygonColixes, nPolygons, bsFaces, htColixes);
    else if (colixes != null)
      colorList = getColorList(0, colixes, nVertices, null, htColixes);
    addColix(colix, polygonColixes != null || colixes != null);
    if (polygonColixes != null) {
      //     output(" colorPerVertex='FALSE'\n");
      return; // for now TODO
    }

    // coordinates, part 1

    int[] coordMap = new int[nVertices];
    int nCoord = 0;
    for (int i = 0; i < nVertices; i++) {
      if (Float.isNaN(vertices[i].x))
        continue;
      coordMap[i] = nCoord++;
    }

    StringBuffer sbFaceCoordIndices = new StringBuffer();
    for (int i = nPolygons; --i >= 0;) {
      if (!bsFaces.get(i))
        continue;
      sbFaceCoordIndices.append(" " + coordMap[indices[i][0]] + " " + coordMap[indices[i][1]] + " "
          + coordMap[indices[i][2]]);
      if (faceVertexMax == 4 && indices[i].length == 4) {
        sbFaceCoordIndices.append(" " + coordMap[indices[i][0]] + " " + coordMap[indices[i][2]]
            + " " + coordMap[indices[i][3]]);
      }
    }

    // normals, part 1  
    
    StringBuffer sbFaceNormalIndices = new StringBuffer();
    Vector vNormals = null;
    if (normals != null) {
      Hashtable htNormals = new Hashtable();
      vNormals = new Vector();
      int[] normalMap = new int[nVertices];
      //output("  solid='FALSE'\n  normalPerVertex='TRUE'\n  ");
      for (int i = 0; i < nVertices; i++) {
        String s;
        if (Float.isNaN(normals[i].x))
          continue;
        s = (" " + round(normals[i].x) + " " + round(normals[i].y) + " " + round(normals[i].z));
        if (htNormals.containsKey(s)) {
          normalMap[i] = ((Integer) htNormals.get(s)).intValue();
        } else {
          normalMap[i] = vNormals.size();
          vNormals.add(s);
          htNormals.put(s, new Integer(normalMap[i]));
        }
      }
      htNormals = null;
      for (int i = nPolygons; --i >= 0;) {
        if (!bsFaces.get(i))
          continue;
        sbFaceNormalIndices.append(" " + normalMap[indices[i][0]] + " " + normalMap[indices[i][1]] + " "
            + normalMap[indices[i][2]]);
        if (faceVertexMax == 4 && indices[i].length == 4)
          sbFaceNormalIndices.append(" " + normalMap[indices[i][0]] + " "
              + normalMap[indices[i][2]] + " " + normalMap[indices[i][3]]);
      }
    }      
    
    // colors, part 1

    StringBuffer sbColorIndexes = new StringBuffer();
    if (colorList != null) {
      for (int i = nPolygons; --i >= 0;) {
        if (!bsFaces.get(i))
          continue;
        if (polygonColixes == null) {
          sbColorIndexes.append(" " + htColixes.get("" + colixes[indices[i][0]]) + " "
              + htColixes.get("" + colixes[indices[i][1]]) + " "
              + htColixes.get("" + colixes[indices[i][2]]));
          if (faceVertexMax == 4 && indices[i].length == 4)
            sbColorIndexes.append(" " + htColixes.get("" + colixes[indices[i][0]]) + " "
                + htColixes.get("" + colixes[indices[i][2]]) + " "
                + htColixes.get("" + colixes[indices[i][3]]));
        } else {
          //TODO polygon colixes
          //output(htColixes.get("" + polygonColixes[i]) + "\n");
        }
      }
    }    


    
    // coordinates, part 2
    
    StringBuffer sbCoords = new StringBuffer();
    for (int i = 0; i < nVertices; i++) {
      if (Float.isNaN(vertices[i].x))
        continue;
      output(vertices[i], sbCoords);
    }
    coordMap = null;

    // normals, part 2

    StringBuffer sbNormals = new StringBuffer();
    int nNormals = 0;
    if (normals != null) {
      nNormals = vNormals.size();
      for (int i = 0; i < nNormals; i++)
        sbNormals.append(vNormals.get(i));
      vNormals = null;
    }

    // colors, part 2

    StringBuffer sbColors = new StringBuffer();
    int nColors = 0;
    if (colorList != null) {
      nColors = colorList.size();
      for (int i = 0; i < nColors; i++) {
        short c = ((Short) colorList.get(i)).shortValue();
        sbColors.append(rgbFractionalFromColix(c, ' '))
                 .append(" ")
                 .append(translucencyFractionalFromColix(c))
                 .append(" ");
      }
    }
    String key = "mesh" + (++iObj);
    addMeshData(key, nFaces, nCoord, nNormals, nColors, sbFaceCoordIndices, sbFaceNormalIndices,
        sbColorIndexes, sbCoords, sbNormals, sbColors);
    Vector v = new Vector();
    htNodes.put(key, v);
    addShader(key, colix);
    cylinderMatrix.setIdentity();
    v.add(getParentItem("jmol", cylinderMatrix));
  }

  private void addMeshData(String key, int nFaces, int nCoord, int nNormals, int nColors, 
                           StringBuffer sbFaceCoordIndices,
                           StringBuffer sbFaceNormalIndices,
                           StringBuffer sbColorIndices, 
                           StringBuffer sbCoords,
                           StringBuffer sbNormals, 
                           StringBuffer sbColors) {
    getMeshHeader(key, nFaces, nCoord, nNormals, nColors, models);
    models.append("MESH_FACE_POSITION_LIST { ")
      .append(sbFaceCoordIndices).append(" }\n")
      .append("MESH_FACE_NORMAL_LIST { ")
      .append(sbFaceNormalIndices).append(" }\n");
    models.append("MESH_FACE_SHADING_LIST { ");
    for (int i = 0; i < nFaces; i++)
      models.append("0 ");
    models.append("}\n");
    if (nColors > 0)
      models.append("MESH_FACE_DIFFUSE_COLOR_LIST { ")
            .append(sbColorIndices).append(" }\n");
    models.append("MODEL_POSITION_LIST { ")
      .append(sbCoords).append(" }\n")
      .append("MODEL_NORMAL_LIST { ")
      .append(sbNormals).append(" }\n");
    if (nColors > 0)
      models.append("MODEL_DIFFUSE_COLOR_LIST { ")
            .append(sbColors)
            .append(" }\n");
    models.append("}}}\n");
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    viewer.unTransformPoint(screenBase, tempP1);
    viewer.unTransformPoint(screenTip, tempP2);
    float radius = viewer.unscaleToScreen((int)screenBase.z, diameter) / 2f;
    if (radius < 0.05f)
      radius = 0.05f;
    if (!haveCone) {
      models.append(getConeResource());
      haveCone = true;
    }
    addColix(colix, false);
    String key = "Cone_" + colix;
    Vector v = (Vector) htNodes.get(key);
    if (v == null) {
      v = new Vector();
      htNodes.put(key, v);
      addShader(key, colix);
    }
    cylinderMatrix.set(getRotationMatrix(tempP1, tempP2, radius));
    cylinderMatrix.m03 = tempP1.x;
    cylinderMatrix.m13 = tempP1.y;
    cylinderMatrix.m23 = tempP1.z;
    cylinderMatrix.m33 = 1;
    v.add(getParentItem("jmol", cylinderMatrix));
  }

  private Object getConeResource() {
    int ndeg = 10;
    int n = 360 / ndeg;
    int vertexCount = n + 1;
    int[] faces = new int[n * 3];
    int fpt = -1;
    for (int i = 0; i < n; i++) {
      faces[++fpt] = i;
      faces[++fpt] = (i + 1) % n;
      faces[++fpt] = n;
    }
    Point3f[] vertexes = new Point3f[vertexCount];
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos(i * ndeg / 180. * Math.PI));
      float y = (float) (Math.sin(i * ndeg / 180. * Math.PI));
      vertexes[i] = new Point3f(x, y, 0);
    }
    vertexes[n] = new Point3f(0, 0, 1);
    return getMeshData("Cone", faces, vertexes, vertexes);
  }
  
  private Object getCircleResource() {
    int ndeg = 10;
    int n = 360 / ndeg;
    int vertexCount = n + 1;
    int[] faces = new int[n * 3];
    int fpt = -1;
    for (int i = 0; i < n; i++) {
      faces[++fpt] = i;
      faces[++fpt] = (i + 1) % n;
      faces[++fpt] = n;
    }
    Point3f[] vertexes = new Point3f[vertexCount];
    Point3f[] normals = new Point3f[vertexCount];
    for (int i = 0; i < n; i++) {
      float x = (float) (Math.cos(i * ndeg / 180. * Math.PI));
      float y = (float) (Math.sin(i * ndeg / 180. * Math.PI));
      vertexes[i] = new Point3f(x, y, 0);
      normals[i] = new Point3f(0, 0, 1);
    }
    vertexes[n] = new Point3f(0, 0, 0);
    normals[n] = new Point3f(0, 0, 1);
    return getMeshData("Circle", faces, vertexes, normals);
  }
  
  public void fillCylinder(short colix, byte endcaps, int diameter,
                           Point3f screenA, Point3f screenB) {
    Point3f ptA = new Point3f();
    Point3f ptB = new Point3f();
    viewer.unTransformPoint(screenA, ptA);
    viewer.unTransformPoint(screenB, ptB);
    int madBond = (int) (viewer.unscaleToScreen(
        (int)((screenA.z + screenB.z) / 2), diameter) * 1000);      
    if (madBond < 20)
      madBond = 20;
    outputCylinder(ptA, ptB, colix, endcaps, madBond);
    // nucleic base
  }

  public void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    // nucleic base
    // just preliminary here 
    viewer.unTransformPoint(ptA, tempP1);
    viewer.unTransformPoint(ptB, tempP2);
    viewer.unTransformPoint(ptC, tempP3);
    addColix(colix, false);
    String key = "T" + (++iObj);
    models.append(getTriangleResource(key, tempP1, tempP2, tempP3));
    Vector v = new Vector();
    htNodes.put(key, v);
    addShader(key, colix);
    if (cylinderMatrix == null)
      cylinderMatrix = new Matrix4f();
    cylinderMatrix.setIdentity();
    v.add(getParentItem("jmol", cylinderMatrix));
  }

  private Object getTriangleResource(String key, Point3f pt1,
                                     Point3f pt2, Point3f pt3) {
    int[] faces = new int[] { 0, 1, 2 };
    Point3f[] vertexes = new Point3f[] { pt1, pt2, pt3 };
    tempV1.set(pt3);
    tempV1.sub(pt1);
    tempV2.set(pt2);
    tempV2.sub(pt1);
    tempV2.cross(tempV2, tempV1);
    tempV2.normalize();
    Vector3f[] normals = new Vector3f[] { tempV2, tempV2, tempV2 };
    return getMeshData(key, faces, vertexes, normals);
  }

  public void plotText(int x, int y, int z, short colix, String text, Font3D font3d) {
    if (z < 3) {
      viewer.transformPoint(center, pt);
      z = (int)pt.z;
    }
/*    String useFontStyle = font3d.fontStyle.toUpperCase();
    String preFontFace = font3d.fontFace.toUpperCase();
    String useFontFace = (preFontFace.equals("MONOSPACED") ? "TYPEWRITER"
        : preFontFace.equals("SERIF") ? "SERIF" : "SANS");
    output("<Transform translation='");
    pt.set(x, y, z);
    viewer.unTransformPoint(pt, ptAtom);
    output(ptAtom);
    output("'>");
    // These x y z are 3D coordinates of echo or the atom the label is attached
    // to.
    output("<Billboard ");
    String child = getDef("T" + colix + useFontFace + useFontStyle + "_" + text);
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "' axisOfRotation='0 0 0'>"
        + "<Transform translation='0.0 0.0 0.0'>"
        + "<Shape>");
      outputAppearance(colix, true);
      output("<Text string=" + Escape.escape(text) + ">");
      output("<FontStyle ");
      String fontstyle = getDef("F" + useFontFace + useFontStyle);
      if (fontstyle.charAt(0) == '_') {
        output("DEF='" + fontstyle + "' size='0.4' family='" + useFontFace
            + "' style='" + useFontStyle + "'/>");      
      } else {
        output(fontstyle + "/>");
      }
      output("</Text>");
      output("</Shape>");
      output("</Transform>");
    } else {
      output(child + ">");
    }
    output("</Billboard>\n");
    output("</Transform>\n");
*/
    /*
     * Unsolved issues: # Non-label texts: echos, measurements :: need to get
     * space coordinates, not screen coord. # Font size: not implemented; 0.4A
     * is hardcoded (resizes with zoom) Java VRML font3d.fontSize = 13.0 size
     * (numeric), but in angstroms, not pixels font3d.fontSizeNominal = 13.0 #
     * Label offsets: not implemented; hardcoded to 0.25A in each x,y,z #
     * Multi-line labels: only the first line is received # Sub/superscripts not
     * interpreted
     */
  }

  public void startShapeBuffer(int iShape) {
  }

  public void endShapeBuffer() {
  }

  public void renderText(Text t) {
  }

  public void drawString(short colix, String str, Font3D font3d, int xBaseline,
                         int yBaseline, int z, int zSlab) {
  }

  public void drawCircleCentered(short colix, int diameter, int x, int y,
                                 int z, boolean doFill) {
    
    /*
     * 
     
    pt.set(x, y, z);
    viewer.unTransformPoint(pt, ptAtom);
    float d = viewer.unscaleToScreen(z, diameter);
    pt.set(x, y, z + 1);
    viewer.unTransformPoint(pt, pt);

    if (doFill) {

      // draw filled circle
      
      output("<Transform translation='");
      tempV1.set(pt);
      tempV1.add(ptAtom);
      tempV1.scale(0.5f);
      output(tempV1);
      output("'><Billboard axisOfRotation='0 0 0'><Transform rotation='1 0 0 1.5708'>");
      outputCylinderChild(ptAtom, pt, colix, Graphics3D.ENDCAPS_FLAT, (int) (d * 1000));
      output("</Transform></Billboard>");
      output("</Transform>\n");
      
      return;
    }
    
    // draw a thin torus

    String child = getDef("C" + colix + "_" + d);
    output("<Transform");
    outputTransRot(pt, ptAtom, 0, 0, 1);
    pt.set(1, 1, 1);
    pt.scale(d/2);
    output(" scale='");
    output(pt);
    output("'>\n<Billboard ");
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "'");
      output(" axisOfRotation='0 0 0'><Transform>");
      output("<Shape><Extrusion beginCap='FALSE' convex='FALSE' endCap='FALSE' creaseAngle='1.57'");
      output(" crossSection='");
      float rpd = 3.1415926f / 180;
      float scale = 0.02f * 2 / d;
      for (int i = 0; i <= 360; i += 10) {
        output(round(Math.cos(i * rpd) * scale) + " ");
        output(round(Math.sin(i * rpd) * scale) + " ");
      }
      output("' spine='");
      for (int i = 0; i <= 360; i += 10) {
        output(round(Math.cos(i * rpd)) + " ");
        output(round(Math.sin(i * rpd)) + " 0 ");
      }
      output("'/>");
      outputAppearance(colix, false);
      output("</Shape></Transform>");
    } else {
      output(child + ">");
    }
    output("</Billboard>\n");
    output("</Transform>\n");
    */
  }

  public void fillScreenedCircleCentered(short colix, int diameter, int x,
                                         int y, int z) {
    drawCircleCentered(colix, diameter, x, y, z, false);
    drawCircleCentered(Graphics3D.getColixTranslucent(colix, true, 0.5f), 
        diameter, x, y, z, true);
  }

  public void drawTextPixel(int argb, int x, int y, int z) {
    // text only
    pt.set(x, y, z);
    viewer.unTransformPoint(pt, ptAtom);
    short colix = Graphics3D.getColix(argb); 
    outputSphere(ptAtom, 0.01f, colix);
  }

  public void plotImage(int x, int y, int z, Image image, short bgcolix,
                        int width, int height) {
    // background, for example
  }

  void renderEllipsoid(Point3f center, Point3f[] points, short colix, int x,
                       int y, int z, int diameter, Matrix3f toEllipsoidal,
                       double[] coef, Matrix4f deriv, Point3i[] octantPoints) {
    //Hey, hey -- quaternions to the rescue!
    // Just send three points to Quaternion to define a plane and return
    // the AxisAngle required to rotate to that position. That's all there is to it.
    
    AxisAngle4f a = Quaternion.getQuaternionFrame(center, points[1], points[3]).toAxisAngle4f();
    float sx = points[1].distance(center);
    float sy = points[3].distance(center);
    float sz = points[5].distance(center);
    outputEllipsoid(center, sx, sy, sz, a, colix);
  }

}
