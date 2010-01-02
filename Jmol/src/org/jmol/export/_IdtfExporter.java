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

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.geodesic.Geodesic;
import org.jmol.util.MeshSurface;
import org.jmol.util.Quaternion;

public class _IdtfExporter extends __CartesianExporter {

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
   * 
   *   especially http://u3d.svn.sourceforge.net/viewvc/u3d/trunk/Docs/IntermediateFormat/IDTF%20Format%20Description.pdf
   *   and in the downloadable zip file, see Bin/Win32/Release/IDTFConverter.exe
   *     
   * see also http://en.wikipedia.org/wiki/Universal_3D
   * see also http://www.ecma-international.org/publications/standards/Ecma-363.htm
   * 
   * for the complete Windows package, see also http://chemapps.stolaf.edu/jmol/docs/misc/idtf.zip
   * 
   * Development comment:
   * 
   * I have spent quite a bit of time now tearing my hair out trying to 
   * figure out how to do this right. The documentation is so opaque, 
   * I can hardly believe it. Nowhere is there a definition of what the 
   * view matrix really means, how a view relates to actual camera position
   * or how translations are to be applied. For example, I simply cannot
   * figure out how to invoke a default view within a PDF or DeepView short
   * of having the user go in and choose the view. But then when I get that
   * going, the zoom is terribly wrong. A "view" has "units" that are
   * either in pixels or "percent". Here is all we have on that:
   * 
     9.5.4.4 U32: View Node Attributes
     View Node Attributes is a bitfield used to indicate different modes of operation of the view
     node. View Node Attributes are defined for projection mode and for screen position units
     mode. 
   * 
   * Period. That's it. What the heck is "screen position units mode"? Beats me. 
   * Nowhere can I find any documentation that actually demonstrates how do 
   * use this resource.
   * 
   * It's interesting, as well, that the two viewers -- 3D PDF and DeepView --
   * do not give the same result with some of my tests. I can only conclude that
   * view model is ill defined. 
   * 
   * Amazingly, with VRML it was straightforward to convert from Jmol. We have a
   * viewing axisAngle that we can pop straight into VRML and get the proper view.
   * Not so here. I can get initial orientation, but the zoom is all wrong.
   * 
   * So the U3D documentation reads:
   * 
     The default node is a group node with no parents. The default node is located at the world origin
     (the identity transform).
     There is no default model node, default light node, nor default view node.
     
     The default light resource is an ambient light that is enabled, no specularity, and color values
     rgb(0.75, 0.75, 0.75).
     
     A scene graph can have several view nodes that define different viewpoints in the world.
     Although there is no default view node, there is a preference for having the coordinate system
     oriented such that the Z-axis is in the up direction with the Y-axis oriented in the direction of
     the view.
     The default view resource has the following properties: pass count one, root node is the default
     node, and fog disabled.

   * Right, OK. But obviously in a real viewer there IS a default for all of these.
   * I have instead implemented a default view using animation. This is a total hack. 
   * The true default view in both 3D-PDF and DeepView appears to be the rotation associated
   * with quaternion (0.6414883, -0.5258319, 0.3542887, 0.43182528)  
   *  
   * The problem that the documentation itself is totally unhelpful.
   * I had to go through the C++ code for IDTFConverter to find the correct set 
   * of IDTF format fields for the modifiers. Has anyone actually done this??
   * 
   * As it turns out, you can get a default view, at least for orientation. 
   * You just add an animation modifier that negates that default quaternion and applies
   * the correct one. However, ZOOM is a completely different issue. I simply
   * cannot get the default zoom to work here.
   * 
   * Bob Hanson 8/15/2009
   * 
   */
  
  
  /* IDTF documentation is in error with regard to motion resources. This is correct:
   * 
 RESOURCE_LIST "MOTION" {
  RESOURCE_COUNT 1
  RESOURCE 0 {
    RESOURCE_NAME "Motion0"
    MOTION_TRACK_COUNT 1
    MOTION_TRACK_LIST {
      MOTION_TRACK 0 {
        MOTION_TRACK_NAME "M00"
        MOTION_TRACK_SAMPLE_COUNT 1
        KEY_FRAME_LIST {
          KEY_FRAME 0 {
            KEY_FRAME_TIME 0
            KEY_FRAME_DISPLACEMENT 0 0 0
            KEY_FRAME_ROTATION 1 0 0 0
            KEY_FRAME_SCALE 1 1 1
          }
        }
      }
    }
  }
 }

   */
  private AxisAngle4f viewpoint = new AxisAngle4f();
  private boolean haveSphere;
  private boolean haveCylinder;
  private boolean haveCone;
  private boolean haveCircle;
  
  protected void output(Tuple3f pt) {
    output(pt, sbTemp, true);
  }

  private void output(Tuple3f pt, StringBuffer sb, boolean checkpt) {
    if (checkpt)
      checkPoint(pt);
    sb.append(round(pt.x)).append(" ").append(round(pt.y)).append(" ").append(round(pt.z)).append(" ");
  }
  
  private Point3f ptMin = new Point3f(1e10f,1e10f,1e10f);
  private Point3f ptMax = new Point3f(-1e10f,-1e10f,-1e10f);
  
  private void checkPoint(Tuple3f pt) {
    if (pt.x < ptMin.x)
      ptMin.x = pt.x;
    if (pt.y < ptMin.y)
      ptMin.y = pt.y;
    if (pt.z < ptMin.z)
      ptMin.z = pt.z;
    if (pt.x > ptMax.x)
      ptMax.x = pt.x;
    if (pt.y > ptMax.y)
      ptMax.y = pt.y;
    if (pt.z > ptMax.z)
      ptMax.z = pt.z;
  }
  
  private int iObj;
  private Hashtable htDefs = new Hashtable();
  
  final private Matrix4f m = new Matrix4f();

  final private StringBuffer models = new StringBuffer();
  final private StringBuffer resources = new StringBuffer();
  final private StringBuffer modifiers = new StringBuffer();

  protected void outputHeader() {
    // next is an approximation only 
    output("FILE_FORMAT \"IDTF\"\nFORMAT_VERSION 100\n");

    /*
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
    */

    m.setIdentity();
    
    
//    ptAtom.set(center);
//    ptAtom.scale(-1);
//    m.m03 = ptAtom.x;
//    m.m13 = ptAtom.y;
//    m.m23 = ptAtom.z;
//    m.m33 = 1;

    

    output("NODE \"GROUP\" {\n");
    output("NODE_NAME \"Jmol\"\n");
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
    modifiers.append("MODIFIER \"SHADING\" {\n");
    modifiers.append("MODIFIER_NAME \"" + key + "\"\n");
    modifiers.append("PARAMETERS {\n");
    modifiers.append("SHADER_LIST_COUNT 1\n");
    modifiers.append("SHADING_GROUP {\n");
    modifiers.append("SHADER_LIST 0 {\n");
    modifiers.append("SHADER_COUNT 1\n");
    modifiers.append("SHADER_NAME_LIST {\n");
    modifiers.append("SHADER 0 NAME: \"Shader_" + colix +"\"\n");
    modifiers.append("}}}}}\n");
  }

  protected void outputFooter() {
    htDefs = null;
    outputNodes();
    output(models.toString());
    output(resources.toString());    
    
    output("RESOURCE_LIST \"VIEW\" {\n");
    output("\tRESOURCE_COUNT 1\n");
    output("\tRESOURCE 0 {\n");
    output("\t\tRESOURCE_NAME \"View0\"\n");
    output("\t\tVIEW_PASS_COUNT 1\n");
    output("\t\tVIEW_ROOT_NODE_LIST {\n");
    output("\t\t\tROOT_NODE 0 {\n");
    output("\t\t\t\tROOT_NODE_NAME \"\"\n");
    output("\t\t\t}\n");
    output("\t\t}\n");
    output("\t}\n");
    output("}\n\n");

    
    // unfortunately, this next bit does not work. 
    // there is something about "key frame displacements" that
    // I can't make out. 
    // The first (default) orientation frame has the correct orientation
    // but the wrong zoom, and I simply cannot get the zoom to work out
    // in the second key frame (end of animation). 
    // instead, what happens is the center shifts. I can't figure out where
    // it is going!
    
    //getViewpointPosition(ptAtom);
    //adjustViewpointPosition(ptAtom);
    viewer.getAxisAngle(viewpoint);
    Quaternion q = new Quaternion(viewpoint);
    viewpoint.set(q.getMatrix());
    if (viewpoint.angle == 0)
      viewpoint.z = 1;
    Quaternion q0 = new Quaternion(0.6414883f, -0.5258319f, 0.3542887f, 0.43182528f);
    q = q0.inv().mul(q);
    
    // the next is just an approximation of the true center of the drawing,
    // which DeepView or 3D-PDF will use as the default center of rotation
    tempP3.set(ptMax);
    tempP3.add(ptMin);
    tempP3.scale(0.5f);
    // transform difference to true center -- we will be close unless there are other objects
    tempP3.sub(center);
    tempP1.set(q.transform(tempP3));
    float zoom = viewer.getZoomPercentFloat() / 100f;
    tempP1.scale(zoom);
    String dxyz = tempP1.x + " " + tempP1.y + " " + tempP1.z;
    String scale = " " + zoom;
    scale = scale + scale + scale;

    // the apparent default rotation in DeepView and 3D-PDF

    output("\nRESOURCE_LIST \"MOTION\" {");
    output("\n  RESOURCE_COUNT 1");
    output("\n  RESOURCE 0 {");
    output("\n    RESOURCE_NAME \"Motion0\"");
    output("\n    MOTION_TRACK_COUNT 1");
    output("\n    MOTION_TRACK_LIST {");
    output("\n      MOTION_TRACK 0 {");
    output("\n        MOTION_TRACK_NAME \"M00\"");
    output("\n        MOTION_TRACK_SAMPLE_COUNT 2");
    output("\n        KEY_FRAME_LIST {");
    output("\n          KEY_FRAME 0 {");
    output("\n            KEY_FRAME_TIME 0");
    output("\n            KEY_FRAME_DISPLACEMENT 0 0 0");
    output("\n            KEY_FRAME_ROTATION " + q.toString0123());
    output("\n            KEY_FRAME_SCALE 1 1 1");
    output("\n          }");
    output("\n          KEY_FRAME 1 {");
    output("\n            KEY_FRAME_TIME 1");
    output("\n            KEY_FRAME_DISPLACEMENT " + dxyz);
    output("\n            KEY_FRAME_ROTATION " + q.toString0123());
    output("\n            KEY_FRAME_SCALE" + scale);
    output("\n          }");
    output("\n         }");
    output("\n      }");
    output("\n    }");
    output("\n  }");
    output("\n}\n");
    output("\nMODIFIER \"ANIMATION\" {");
    output("\n  MODIFIER_NAME \"Jmol\"");
    output("\n  PARAMETERS {");
    output("\n    ATTRIBUTE_ANIMATION_PLAYING \"TRUE\"");
    output("\n    ATTRIBUTE_ROOT_BONE_LOCKED \"TRUE\"");
    output("\n    ATTRIBUTE_SINGLE_TRACK \"TRUE\"");
    output("\n    ATTRIBUTE_AUTO_BLEND \"FALSE\"");
    output("\n    TIME_SCALE 1.0");
    output("\n    BLEND_TIME 0.0");
    output("\n    MOTION_COUNT 1");
    output("\n    MOTION_INFO_LIST {");
    output("\n      MOTION_INFO 0 {");
    output("\n        MOTION_NAME \"Motion0\"");
    output("\n        ATTRIBUTE_LOOP \"FALSE\"");
    output("\n        ATTRIBUTE_SYNC \"FALSE\"");
    output("\n        TIME_OFFSET 0.0");
    output("\n        TIME_SCALE 1.0");
    output("\n      }");
    output("\n    }");
    output("\n  }");
    output("\n}\n");

    output(modifiers.toString());    
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

  protected void outputEllipsoid(Point3f center, Point3f[] points, short colix) {
    //Hey, hey -- quaternions to the rescue!
    // Just send three points to Quaternion to define a plane and return
    // the AxisAngle required to rotate to that position. That's all there is to it.
    
    AxisAngle4f a = Quaternion.getQuaternionFrame(center, points[1], points[3]).toAxisAngle4f();
    float sx = points[1].distance(center);
    float sy = points[3].distance(center);
    float sz = points[5].distance(center);
    outputEllipsoid(center, sx, sy, sz, a, colix);
  }

  private Matrix4f sphereMatrix = new Matrix4f();
  private Matrix4f cylinderMatrix = new Matrix4f();

  private void outputEllipsoid(Point3f center, float rx, float ry, float rz, AxisAngle4f a, short colix) {
    if (!haveSphere) {
      models.append(getSphereResource());
      haveSphere = true;
      sphereMatrix = new Matrix4f();
    }
    checkPoint(center);
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
    v.add(getParentItem("Jmol", sphereMatrix));
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
    int nFaces = f.length / 3;
    int[][] faces = new int[nFaces][];
    int fpt = -1;
    for (int i = 0; i < nFaces; i++)
      faces[i] = new int[] { f[++fpt], f[++fpt], f[++fpt] };
    Vector3f[] vertexes = new Vector3f[vertexCount];
    for (int i = 0; i < vertexCount;i++)
      vertexes[i] = Geodesic.getVertexVector(i);
    return getMeshData("Sphere", faces, vertexes, vertexes);
  }

  private String getMeshData(String type, int[][] indices, Tuple3f[] vertexes, Tuple3f[] normals) {
    int nFaces = indices.length;
    int vertexCount = vertexes.length;
    int normalCount = normals.length;
    StringBuffer sb = new StringBuffer();
    getMeshHeader(type, nFaces, vertexCount, normalCount, 0, sb);
    StringBuffer sb1 = new StringBuffer();
    for (int i = 0; i < indices.length; i++) {
      sb1.append(indices[i][0]).append(" ");
      sb1.append(indices[i][1]).append(" ");
      sb1.append(indices[i][2]).append(" ");
    }
    sb.append("MESH_FACE_POSITION_LIST { ");
    sb.append(sb1);
    sb.append("}\n");
    sb.append("MESH_FACE_NORMAL_LIST { ");
    sb.append(sb1);
    sb.append("}\n");
    sb.append("MESH_FACE_SHADING_LIST { ");
    for (int i = 0; i < nFaces; i++)
      sb.append("0 ");
    sb.append("}\n");
    sb.append("MODEL_POSITION_LIST { ");
    for (int i = 0; i < vertexCount; i++)
      output(vertexes[i], sb, false);
    sb.append("}\n");
    sb.append("MODEL_NORMAL_LIST { ");
    for (int i = 0; i < normalCount; i++)
      output(normals[i], sb, false);
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

  protected void outputCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, float radius) {
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, radius*1.01f, colix);
      outputSphere(pt2, radius*1.01f, colix);
    } else if (endcaps == Graphics3D.ENDCAPS_FLAT) {
      outputCircle(pt1, pt2, colix, radius);      
      outputCircle(pt2, pt1, colix, radius);      
    }
    if (!haveCylinder) {
      models.append(getCylinderResource());
      haveCylinder = true;
      cylinderMatrix = new Matrix4f();
    }
    checkPoint(pt1);
    checkPoint(pt2);
    addColix(colix, false);
    String key = "Cylinder_" + colix;
    Vector v = (Vector) htNodes.get(key);
    if (v == null) {
      v = new Vector();
      htNodes.put(key, v);
      addShader(key, colix);
    }
    cylinderMatrix.set(getRotationMatrix(pt1, pt2, radius));
    cylinderMatrix.m03 = pt1.x;
    cylinderMatrix.m13 = pt1.y;
    cylinderMatrix.m23 = pt1.z;
    cylinderMatrix.m33 = 1;
    v.add(getParentItem("Jmol", cylinderMatrix));
  }

  protected void outputCircle(Point3f pt1, Point3f pt2, float radius,
                              short colix, boolean doFill) {
    if (doFill) {
      outputCircle(pt1, pt2, colix, radius);
      return;
    }
    if (true)
      return;
    // the halo edges really slow rendering and aren't that important.
    float rpd = 3.1415926f / 180;
    Point3f[] pts = new Point3f[73];
    for (int i = 0, p = 0; i <= 360; i += 5, p++) {
      pts[p] = new Point3f((float) (Math.cos(i * rpd) * radius), (float) (Math
          .sin(i * rpd) * radius), 0);
      pts[p].add(pt1);
    }
    radius = (int) (0.02f * radius);
    for (int i = 0; i < 72; i++) {
      outputCylinder(pts[i], pts[i + 1], colix, Graphics3D.ENDCAPS_FLAT, radius);
    }
  }

  private void outputCircle(Point3f ptCenter, Point3f ptPerp, short colix, float radius) {
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
    checkPoint(ptCenter);
    cylinderMatrix.set(getRotationMatrix(ptCenter, ptPerp, radius));
    cylinderMatrix.m03 = ptCenter.x;
    cylinderMatrix.m13 = ptCenter.y;
    cylinderMatrix.m23 = ptCenter.z;
    cylinderMatrix.m33 = 1;
    v.add(getParentItem("Jmol", cylinderMatrix));
  }

  private String getCylinderResource() {
    int ndeg = 10;
    int vertexCount = 360 / ndeg * 2;
    int n = vertexCount / 2;
    int[][] faces = new int[vertexCount][];
    int fpt = -1;
    for (int i = 0; i < n; i++) {
      faces[++fpt] = new int[] { i, (i + 1) % n, i + n };
      faces[++fpt] = new int[] { (i + 1) % n, (i + 1) % n + n, i + n };
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


  private StringBuffer sbTemp;
  
  protected void outputFace(int[] face, int[] map, int faceVertexMax) {
    sbTemp.append(" " + map[face[0]] + " " + map[face[1]] + " "
        + map[face[2]]);
    if (faceVertexMax == 4 && face.length == 4) {
      sbTemp.append(" " + map[face[0]] + " " + map[face[2]] + " "
          + map[face[3]]);
    }
  }

  protected void outputSurface(Point3f[] vertices, Vector3f[] normals,
                                  short[] colixes, int[][] indices,
                                  short[] polygonColixes,
                                  int nVertices, int nPolygons, int nFaces, BitSet bsFaces,
                                  int faceVertexMax, short colix, Vector colorList, Hashtable htColixes, Point3f offset) {
    addColix(colix, polygonColixes != null || colixes != null);
    if (polygonColixes != null) {
      //     output(" colorPerVertex='FALSE'\n");
      return; // for now TODO
    }

    // coordinates, part 1

    StringBuffer sbFaceCoordIndices = sbTemp = new StringBuffer();
    int[] map = new int[nVertices];
    int nCoord = getCoordinateMap(vertices, map);
    outputIndices(indices, map, nPolygons, bsFaces, faceVertexMax);

    // normals, part 1  
    
    StringBuffer sbFaceNormalIndices = sbTemp = new StringBuffer();
    Vector vNormals = null;
    if (normals != null) {
      vNormals = new Vector();
      map = getNormalMap(normals, nVertices, vNormals);
      outputIndices(indices, map, nPolygons, bsFaces, faceVertexMax);
    }      
    
    map = null;

    // colors, part 1

    StringBuffer sbColorIndexes = new StringBuffer();
    if (colorList != null) {
      for (int i = nPolygons; --i >= 0;) {
        if (bsFaces != null && !bsFaces.get(i))
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
    
    StringBuffer sbCoords = sbTemp = new StringBuffer();
    outputVertices(vertices, nVertices, offset);

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
    v.add(getParentItem("Jmol", cylinderMatrix));
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

  protected void outputComment(String comment) {
    // no capability
  }

  protected void outputCone(Point3f ptBase, Point3f ptTip, float radius,
                            short colix) {
    if (!haveCone) {
      models.append(getConeResource());
      haveCone = true;
    }
    checkPoint(ptBase);
    checkPoint(ptTip);
    addColix(colix, false);
    String key = "Cone_" + colix;
    Vector v = (Vector) htNodes.get(key);
    if (v == null) {
      v = new Vector();
      htNodes.put(key, v);
      addShader(key, colix);
    }
    cylinderMatrix.set(getRotationMatrix(ptBase, ptTip, radius));
    cylinderMatrix.m03 = ptBase.x;
    cylinderMatrix.m13 = ptBase.y;
    cylinderMatrix.m23 = ptBase.z;
    cylinderMatrix.m33 = 1;
    v.add(getParentItem("Jmol", cylinderMatrix));
  }

  private String getConeResource() {
    MeshSurface m = getConeMesh(null, null, (short) 0);
    return getMeshData("Cone", m.polygonIndexes, m.vertices, m.vertices);
  }
  
  private String getCircleResource() {
    int ndeg = 10;
    int n = 360 / ndeg;
    int vertexCount = n + 1;
    int[][] faces = new int[n][];
    for (int i = 0; i < n; i++)
      faces[i] = new int[] { i, (i + 1) % n, n };
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
  
  protected void outputSphere(Point3f center, float radius, short colix) {
    outputEllipsoid(center, radius, radius, radius, null, colix);
  }

  protected void outputTextPixel(Point3f pt, int argb) {    
    short colix = Graphics3D.getColix(argb); 
    outputSphere(pt, 0.02f, colix);
  }

  protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3, short colix) {
    addColix(colix, false);
    String key = "T" + (++iObj);
    models.append(getTriangleResource(key, pt1, pt2, pt3));
    Vector v = new Vector();
    htNodes.put(key, v);
    addShader(key, colix);
    if (cylinderMatrix == null)
      cylinderMatrix = new Matrix4f();
    cylinderMatrix.setIdentity();
    v.add(getParentItem("Jmol", cylinderMatrix));
  }

  private int[][] triangleFace = new int[1][];
  {
    triangleFace[0] = new int[] { 0, 1, 2 };
  }
  
  private String getTriangleResource(String key, Point3f pt1,
                                     Point3f pt2, Point3f pt3) {
    Point3f[] vertexes = new Point3f[] { pt1, pt2, pt3 };
    tempV1.set(pt3);
    tempV1.sub(pt1);
    tempV2.set(pt2);
    tempV2.sub(pt1);
    tempV2.cross(tempV2, tempV1);
    tempV2.normalize();
    Vector3f[] normals = new Vector3f[] { tempV2, tempV2, tempV2 };
    return getMeshData(key, triangleFace, vertexes, normals);
  }
}
