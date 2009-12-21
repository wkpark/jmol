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

package org.jmol.export;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.util.Escape;
import org.jmol.util.Quaternion;
import org.jmol.script.Token;
import org.jmol.viewer.Viewer;

public class _VrmlExporter extends __CartesianExporter {

  // VERY preliminary -- in process -- 7/2007 Bob Hanson
  /*
   * 1/2009 Angel Herraez: # added support for translucency # Jmol info in
   * header # set navigation mode # added support for background color # added
   * support for labels: text, font face and font style; size is hardcoded to
   * 0.4A
   */

  private AxisAngle4f viewpoint = new AxisAngle4f();
  
  private void output(Tuple3f pt) {
    output(round(pt.x) + " " + round(pt.y) + " " + round(pt.z));
  }

  private int iObj;
  private Hashtable htDefs = new Hashtable();
  
  /**
   * Hashtable htDefs contains references to _n where n is a number. 
   * we look up a key for anything and see if an object has been assigned.
   * If it is there, we just return the phrase "USE _n".
   * It it is not there, we return the DEF name that needs to be assigned.
   * The calling method must then make that definition.
   * 
   * @param key
   * @return "_n" or "DEF _n"
   */
  private String getDef(String key) {
    if (htDefs.containsKey(key))
      return "USE " + htDefs.get(key);
    String id = "_" + (iObj++);
    htDefs.put(key, id);
    return id;
  }
  
  protected void outputHeader() {
    output("#VRML V2.0 utf8 Generated by Jmol " + Viewer.getJmolVersion()
        + "\n");
    output("WorldInfo { \n");
    output(" title " + Escape.escape(viewer.getModelSetName()) + "\n"); 
    output(" info [ \"Generated by Jmol " + Viewer.getJmolVersion() + " \", \n");
    output("  \"http://www.jmol.org \", \n");
    output("  \"Creation date: " + getExportDate() + " \" ]\n");
    output("} \n");

    output("NavigationInfo { type \"EXAMINE\" } \n");
    // puts the viewer into model-rotation mode
    output("Background { skyColor ["
        + rgbFractionalFromColix(viewer.getObjectColix(0), ' ') + "] } \n");
    // next is an approximation only 
    getViewpointPosition(tempP1);
    adjustViewpointPosition(tempP1);
    float angle = getFieldOfView();
    viewer.getAxisAngle(viewpoint);
    output("Viewpoint{fieldOfView " + angle 
        + " position " + tempP1.x + " " + tempP1.y + " " + tempP1.z 
        + " orientation " + viewpoint.x + " " + viewpoint.y + " " + (viewpoint.angle == 0 ? 1 : viewpoint.z) + " " + -viewpoint.angle);
    output("\n jump TRUE description \"v1\"\n}\n");
    output("\n#Jmol perspective:\n");
    output("#scalePixelsPerAngstrom: " + viewer.getScalePixelsPerAngstrom(false) + "\n");
    output("#cameraDepth: " + viewer.getCameraDepth() + "\n");
    output("#center: " + center + "\n");
    output("#rotationRadius: " + viewer.getRotationRadius() + "\n");
    output("#boundboxCenter: " + viewer.getBoundBoxCenter() + "\n");
    output("#translationOffset: " + viewer.getTranslationScript() + "\n");
    output("#zoom: " + viewer.getZoomPercentFloat() + "\n");
    output("#moveto command: " + viewer.getOrientationText(Token.moveto) + "\n");
    output("#screen width height dim: " + screenWidth + " " + screenHeight + " " + viewer.getScreenDim() + "\n\n");
    output("Transform{children Transform{translation ");
    tempP1.set(center);
    tempP1.scale(-1);
    output(tempP1);
    output("\nchildren [\n");
  }

  protected void outputFooter() {
    htDefs = null;
    output("\n]\n");
    output("}}\n");
  }

  private void outputAppearance(short colix, boolean isText) {
    String def = getDef((isText ? "T" : "") + colix);
    output(" appearance ");
    if (def.charAt(0) == '_') {
      String color = rgbFractionalFromColix(colix, ' ');
      output(" DEF " + def + " Appearance{material Material{diffuseColor ");
      if (isText)
        output(" 0 0 0 specularColor 0 0 0 ambientIntensity 0.0 shininess 0.0 emissiveColor " 
            + color + " }}");
      else
        output(color + " transparency " + translucencyFractionalFromColix(colix) + "}}");
      return;
    }
    output(def);
  }
  
  protected void outputCircle(Point3f pt1, Point3f pt2, float radius, short colix,
                            boolean doFill) {
    if (doFill) {
      // draw filled circle

      output("Transform{translation ");
      tempV1.set(pt1);
      tempV1.add(pt2);
      tempV1.scale(0.5f);
      output(tempV1);
      output(" children Billboard{axisOfRotation 0 0 0 children Transform{rotation 1 0 0 1.5708");
      outputCylinderChild(pt1, pt2, colix, Graphics3D.ENDCAPS_FLAT,
          (int) (radius * 2000));
      output("}}}\n");
      return;
    }

    // draw a thin torus

    String child = getDef("C" + colix + "_" + radius);
    outputTransRot(pt1, pt2, 0, 0, 1);
    tempP3.set(1, 1, 1);
    tempP3.scale(radius);
    output(" scale ");
    output(tempP3);
    output(" children ");
    if (child.charAt(0) == '_') {
      output("DEF " + child);
      output(" Billboard{axisOfRotation 0 0 0 children Transform{children");
      output(" Shape{geometry Extrusion{beginCap FALSE convex FALSE endCap FALSE creaseAngle 1.57");
      output(" crossSection [");
      float rpd = 3.1415926f / 180;
      float scale = 0.02f / radius;
      for (int i = 0; i <= 360; i += 10) {
        output(round(Math.cos(i * rpd) * scale) + " ");
        output(round(Math.sin(i * rpd) * scale) + " ");
      }
      output("] spine [");
      for (int i = 0; i <= 360; i += 10) {
        output(round(Math.cos(i * rpd)) + " ");
        output(round(Math.sin(i * rpd)) + " 0 ");
      }
      output("]}");
      outputAppearance(colix, false);
      output("}}}");
    } else {
      output(child);
    }
    output("}\n");
  }

  protected void outputComment(String comment) {
    output("# " + comment + "/n");
  }
  
  protected void outputCone(Point3f ptBase, Point3f ptTip, float radius,
                            short colix) {
    float height = tempP1.distance(tempP2);
    outputTransRot(tempP1, tempP2, 0, 1, 0);
    output(" children ");
    String cone = "o" + (int) (height * 100) + "_" + (int) (radius * 100);
    String child = getDef("c" + cone + "_" + colix);
    if (child.charAt(0) == '_') {
      output("DEF " + child + " Shape{geometry ");
      cone = getDef(cone);
      if (cone.charAt(0) == '_') {
        output("DEF " + cone + " Cone{height " + round(height)
            + " bottomRadius " + round(radius) + "}");
      } else {
        output(cone);
      }
      outputAppearance(colix, false);
      output("}");
    } else {
      output(child);
    }
    output("}\n");
  }

  protected void outputCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, float radius) {
    outputTransRot(pt1, pt2, 0, 1, 0);
    outputCylinderChild(pt1, pt2, colix, endcaps, radius);
    output("}\n");
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, radius*1.01f, colix);
      outputSphere(pt2, radius*1.01f, colix);
    }
  }

  private void outputCylinderChild(Point3f pt1, Point3f pt2, short colix,
                                   byte endcaps, float radius) {
    output(" children ");    
    float length = round(pt1.distance(pt2));
    String child = getDef("C" + colix + "_" + (int) (length * 100) + "_" + radius
        + "_" + endcaps);
    if (child.charAt(0) == '_') {
      output("DEF " + child);
      output(" Shape{geometry ");
      String cyl = getDef("c" + length + "_" + endcaps + "_" + radius);
      if (cyl.charAt(0) == '_') {
        output("DEF " + cyl + " Cylinder{height " 
            + length + " radius " + radius 
            + (endcaps == Graphics3D.ENDCAPS_FLAT ? "" : " top FALSE bottom FALSE") + "}");
      } else {
        output(cyl);
      }
      outputAppearance(colix, false);
      output("}");
    } else {
      output(child);
    }
  }

  protected void outputEllipsoid(Point3f center, Point3f[] points, short colix) {
    output("Transform{translation ");
    output(center);

    // Hey, hey -- quaternions to the rescue!
    // Just send three points to Quaternion to define a plane and return
    // the AxisAngle required to rotate to that position. That's all there is to
    // it.

    AxisAngle4f a = Quaternion.getQuaternionFrame(center, points[1], points[3])
        .toAxisAngle4f();
    if (!Float.isNaN(a.x))
      output(" rotation " + a.x + " " + a.y + " " + a.z + " " + a.angle);
    tempP3.set(0, 0, 0);
    float sx = points[1].distance(center);
    float sy = points[3].distance(center);
    float sz = points[5].distance(center);
    output(" scale " + sx + " " + sy + " " + sz + " children ");
    outputSphere(tempP3, 1.0f, colix);
    output("}\n");
  }

  protected void outputIsosurface(Point3f[] vertices, Vector3f[] normals,
                                  short[] colixes, int[][] indices,
                                  short[] polygonColixes,
                                  int nVertices, int nPolygons, int nFaces, BitSet bsFaces,
                                  int faceVertexMax, short colix, Vector colorList, Hashtable htColixes) {
    output("Shape {\n");
    outputAppearance(colix, false);
    output(" geometry IndexedFaceSet {\n");

    if (polygonColixes != null)
      output(" colorPerVertex FALSE\n");

    // coordinates

    output("coord Coordinate {\n   point [\n");
    int[] coordMap = new int[nVertices];
    int n = 0;
    for (int i = 0; i < nVertices; i++) {
      if (Float.isNaN(vertices[i].x))
        continue;
      coordMap[i] = n++;
      output(vertices[i]);
      output("\n");
    }
    output("   ]\n");
    output("  }\n");
    output("  coordIndex [\n");
    for (int i = nPolygons; --i >= 0;) {
      if (!bsFaces.get(i))
        continue;
      output(coordMap[indices[i][0]] + " " + coordMap[indices[i][1]] + " "
          + coordMap[indices[i][2]] + " -1\n");
      if (faceVertexMax == 4 && indices[i].length == 4)
        output(coordMap[indices[i][0]] + " " + coordMap[indices[i][2]]
            + " " + coordMap[indices[i][3]] + " -1\n");
    }
    output("  ]\n");
    coordMap = null;

    // normals

    if (normals != null) {
      Hashtable htNormals = new Hashtable();
      Vector vNormals = new Vector();
      int[] normalMap = new int[nVertices];
      output("  solid FALSE\n  normalPerVertex TRUE\n   normal Normal {\n  vector [\n");
      for (int i = 0; i < nVertices; i++) {
        String s;
        if (Float.isNaN(normals[i].x))
          continue;
        s = (round(normals[i].x) + " " + round(normals[i].y) + " " + round(normals[i].z) + "\n");
        if (htNormals.containsKey(s)) {
          normalMap[i] = ((Integer) htNormals.get(s)).intValue();
        } else {
          normalMap[i] = vNormals.size();
          vNormals.add(s);
          htNormals.put(s, new Integer(normalMap[i]));
        }
      }
      htNormals = null;
      n = vNormals.size();
      for (int i = 0; i < n; i++)
        output((String) vNormals.get(i));
      vNormals = null;
      output("   ]\n");
      output("  }\n");
      output("  normalIndex [\n");
      for (int i = nPolygons; --i >= 0;) {
        if (!bsFaces.get(i))
          continue;
        output(normalMap[indices[i][0]] + " " + normalMap[indices[i][1]] + " "
            + normalMap[indices[i][2]] + " -1\n");
        if (faceVertexMax == 4 && indices[i].length == 4)
          output(normalMap[indices[i][0]] + " "
              + normalMap[indices[i][2]] + " " + normalMap[indices[i][3]]
              + " -1\n");
      }
      output("  ]\n");
    }

    // colors

    if (colorList != null) {
      output("  color Color { color [\n");
      int nColors = colorList.size();
      for (int i = 0; i < nColors; i++) {
        String color = rgbFractionalFromColix(((Short) colorList.get(i)).shortValue(),
            ' ');
        output(" ");
        output(color);
        output("\n");
      }
      output("  ] } \n");
      output("  colorIndex [\n");
      for (int i = nPolygons; --i >= 0;) {
        if (!bsFaces.get(i))
          continue;
        if (polygonColixes == null) {
          output(htColixes.get("" + colixes[indices[i][0]]) + " "
              + htColixes.get("" + colixes[indices[i][1]]) + " "
              + htColixes.get("" + colixes[indices[i][2]]) + " -1\n");
          if (faceVertexMax == 4 && indices[i].length == 4)
            output(htColixes.get("" + colixes[indices[i][0]]) + " "
                + htColixes.get("" + colixes[indices[i][2]]) + " "
                + htColixes.get("" + colixes[indices[i][3]]) + " -1\n");
        } else {
          output(htColixes.get("" + polygonColixes[i]) + "\n");
        }
      }
      output("  ]\n");
    }

    output(" }\n");
    output("}\n");
  }

  protected void outputSphere(Point3f center, float radius, short colix) {
    output("Transform{translation ");
    output(center);
    output(" children ");
    String child = getDef("S" + colix + "_" + (int) (radius * 100));
    if (child.charAt(0) == '_') {
      output("DEF " + child);
      output(" Shape{geometry Sphere{radius " + radius + "}");
      outputAppearance(colix, false);
      output("}");
    } else {
      output(child);
    }
    output("}\n");
  }
  
  protected void outputTextPixel(Point3f pt, int argb) {
    String color = rgbFractionalFromArgb(argb, ' ');
    output("Transform{translation ");
    output(pt);
    output(" children ");
    String child = getDef("p" + argb);
    if (child.charAt(0) == '_') {
      output("DEF " + child + " Shape{geometry Sphere{radius 0.01}");
      output(" appearance Appearance{material Material{diffuseColor 0 0 0 specularColor 0 0 0 ambientIntensity 0.0 shininess 0.0 emissiveColor "
          + color + " }}}");
    } else {
      output(child);
    }
    output("}\n");
  }

  private void outputTransRot(Point3f pt1, Point3f pt2, int x, int y, int z) {    
    output("Transform{translation ");
    tempV1.set(pt2);
    tempV1.add(pt1);
    tempV1.scale(0.5f);
    output(tempV1);
    tempV1.sub(pt1);
    getAxisAngle(tempV1, x, y, z);
    output(" rotation " + round(tempA.x) + " " + round(tempA.y) + " " + round(tempA.z) + " "
        + round(tempA.angle));
  }
  
  protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3, short colix) {
    // nucleic base
    // cartoons
    output("Shape{geometry IndexedFaceSet{solid FALSE coord Coordinate{point[");
    output(pt1);
    output(" ");
    output(pt2);
    output(" ");
    output(pt3);
    output("]}coordIndex[ 0 1 2 -1 ]}");
    outputAppearance(colix, false);
    output("}\n");
  }

  void plotText(int x, int y, int z, short colix, String text, Font3D font3d) {
    if (z < 3)
      z = viewer.getFrontPlane();
    String useFontStyle = font3d.fontStyle.toUpperCase();
    String preFontFace = font3d.fontFace.toUpperCase();
    String useFontFace = (preFontFace.equals("MONOSPACED") ? "TYPEWRITER"
        : preFontFace.equals("SERIF") ? "SERIF" : "SANS");
    output("Transform{translation ");
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    output(tempP1);
    // These x y z are 3D coordinates of echo or the atom the label is attached
    // to.
    output(" children ");
    String child = getDef("T" + colix + useFontFace + useFontStyle + "_" + text);
    if (child.charAt(0) == '_') {
      output("DEF " + child + " Billboard{axisOfRotation 0 0 0 children Transform{children Shape{");
      outputAppearance(colix, true);
      output(" geometry Text{fontStyle ");
      String fontstyle = getDef("F" + useFontFace + useFontStyle);
      if (fontstyle.charAt(0) == '_') {
        output("DEF " + fontstyle + " FontStyle{size 0.4 family \"" + useFontFace
            + "\" style \"" + useFontStyle + "\"}");      
      } else {
        output(fontstyle);
      }
      output(" string " + Escape.escape(text) + "}}}}");
    } else {
      output(child);
    }
    output("}\n");
  }

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
