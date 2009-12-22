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

public class _X3dExporter extends __CartesianExporter {

  private AxisAngle4f viewpoint = new AxisAngle4f();
  
  private void output(Tuple3f pt) {
    output(round(pt));
  }
  UseTable useTable = new UseTable("USE ");
  
  protected void outputHeader() {
    output("<X3D profile='Immersive' version='3.1' "
      + "xmlns:xsd='http://www.w3.org/2001/XMLSchema-instance' "
      + "xsd:noNamespaceSchemaLocation=' http://www.web3d.org/specifications/x3d-3.1.xsd '>"
      + "\n");
    output("<head>\n");
    output("<meta name='title' content=" + Escape.escape(viewer.getModelSetName()) + "/>\n");
    output("<meta name='description' content=' '/>\n");
    output("<meta name='creator' content=' '/>\n");
    output("<meta name='created' content='" + getExportDate() + "'/>\n");
    output("<meta name='generator' content='Jmol "+ Viewer.getJmolVersion() +", http://www.jmol.org'/>\n");
		output("<meta name='license' content='http://www.gnu.org/licenses/licenses.html#LGPL'/>\n");
    output("</head>\n");
    output("<Scene>\n");

    output("<NavigationInfo type='EXAMINE'/>\n");
    // puts the viewer into model-rotation mode
    output("<Background skyColor='" 
      + rgbFractionalFromColix(viewer.getObjectColix(0), ' ') + "'/>\n");
    // next is an approximation only 
    getViewpointPosition(tempP1);
    adjustViewpointPosition(tempP1);
    float angle = getFieldOfView();
    viewer.getAxisAngle(viewpoint);
    output("<Viewpoint fieldOfView='" + angle
      + "' position='" + tempP1.x + " " + tempP1.y + " " + tempP1.z 
      + "' orientation='" + viewpoint.x + " " + viewpoint.y + " " 
      + (viewpoint.angle == 0 ? 1 : viewpoint.z) + " " + -viewpoint.angle
      + "'\n jump='TRUE' description='v1'/>\n");
    output("\n  <!-- Jmol perspective:\n");
    output("  scalePixelsPerAngstrom: " + viewer.getScalePixelsPerAngstrom(false) + "\n");
    output("  cameraDepth: " + viewer.getCameraDepth() + "\n");
    output("  center: " + center + "\n");
    output("  rotationRadius: " + viewer.getRotationRadius() + "\n");
    output("  boundboxCenter: " + viewer.getBoundBoxCenter() + "\n");
    output("  translationOffset: " + viewer.getTranslationScript() + "\n");
    output("  zoom: " + viewer.getZoomPercentFloat() + "\n");
    output("  moveto command: " + viewer.getOrientationText(Token.moveto) + "\n");
    output("  screen width height dim: " + screenWidth + " " + screenHeight + " " 
      + viewer.getScreenDim() 
      + "\n  -->\n\n");

    output("<Transform translation='");
    tempP1.set(center);
    tempP1.scale(-1);
    output(tempP1);
    output("'>\n");
  }

  protected void outputFooter() {
    useTable = null;
    output("</Transform>\n");
    output("</Scene>\n");
    output("</X3D>\n");
  }

  private void outputAppearance(short colix, boolean isText) {  
    String def = useTable.getDef((isText ? "T" : "") + colix);
    output("<Appearance ");
    if (def.charAt(0) == '_') {
      String color = rgbFractionalFromColix(colix, ' ');
      output("DEF='" + def + "'><Material diffuseColor='");
      if (isText)
        output("0 0 0' specularColor='0 0 0' ambientIntensity='0.0' shininess='0.0' emissiveColor='" 
            + color + "'/>");
      else
        output(color + "' transparency='" + translucencyFractionalFromColix(colix) + "'/>" );
    }
    else
      output(def +">");
    output("</Appearance>");
  }
  
  protected void outputCircle(Point3f pt1, Point3f pt2, float radius, short colix,
                              boolean doFill) {
    if (doFill) {

      // draw filled circle
      
      output("<Transform translation='");
      tempV1.set(tempP3);
      tempV1.add(pt1);
      tempV1.scale(0.5f);
      output(tempV1);
      output("'><Billboard axisOfRotation='0 0 0'><Transform rotation='1 0 0 1.5708'>");
      outputCylinderChild(pt1, tempP3, colix, Graphics3D.ENDCAPS_FLAT, radius);
      output("</Transform></Billboard>");
      output("</Transform>\n");
      
      return;
    }
    
    // draw a thin torus

    String child = useTable.getDef("C" + colix + "_" + radius);
    output("<Transform");
    outputTransRot(tempP3, pt1, 0, 0, 1);
    tempP3.set(1, 1, 1);
    tempP3.scale(radius);
    output(" scale='");
    output(tempP3);
    output("'>\n<Billboard ");
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "'");
      output(" axisOfRotation='0 0 0'><Transform>");
      output("<Shape><Extrusion beginCap='FALSE' convex='FALSE' endCap='FALSE' creaseAngle='1.57'");
      output(" crossSection='");
      float rpd = 3.1415926f / 180;
      float scale = 0.02f / radius;
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
  }

  protected void outputComment(String comment) {
    // ignore
  }

  protected void outputCone(Point3f ptBase, Point3f ptTip, float radius,
                            short colix) {
    float height = ptBase.distance(ptTip);
    output("<Transform");
    outputTransRot(ptBase, ptTip, 0, 1, 0);
    output(">\n<Shape ");
    String cone = "o" + (int) (height * 100) + "_" + (int) (radius * 100);
    String child = useTable.getDef("c" + cone + "_" + colix);
    if (child.charAt(0) == '_') {
      output("DEF='" + child +  "'>");
      cone = useTable.getDef(cone);
      output("<Cone ");
      if (cone.charAt(0) == '_') {
        output("DEF='"+ cone + "' height='" + round(height) 
          + "' bottomRadius='" + round(radius) + "'/>");
      } else {
        output(cone + "/>");
      }
      outputAppearance(colix, false);
    } else {
      output(child + ">");
    }
    output("</Shape>\n");
    output("</Transform>\n");
  }

  protected void outputCylinder(Point3f pt1, Point3f pt2, short colix,
                                byte endcaps, float radius) {
    output("<Transform");
    outputTransRot(pt1, pt2, 0, 1, 0);
    output(">\n");
    outputCylinderChild(pt1, pt2, colix, endcaps, radius);
    output("\n</Transform>\n");
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, radius * 1.01f, colix);
      outputSphere(pt2, radius * 1.01f, colix);
    }
  }

  private void outputCylinderChild(Point3f pt1, Point3f pt2, short colix,
                                   byte endcaps, float radius) {
    float length = pt1.distance(pt2);
    String child = useTable.getDef("C" + colix + "_" + (int) (length * 100) + "_"
        + radius + "_" + endcaps);
    output("<Shape ");
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "'>");
      output("<Cylinder ");
      String cyl = useTable.getDef("c" + round(length) + "_" + endcaps + "_" + radius);
      if (cyl.charAt(0) == '_') {
        output("DEF='"
            + cyl
            + "' height='"
            + round(length)
            + "' radius='"
            + radius
            + "'"
            + (endcaps == Graphics3D.ENDCAPS_FLAT ? ""
                : " top='FALSE' bottom='FALSE'") + "/>");
      } else {
        output(cyl + "/>");
      }
      outputAppearance(colix, false);
    } else {
      output(child + ">");
    }
    output("</Shape>");
  }
  
  protected void outputEllipsoid(Point3f center, Point3f[] points, short colix) {
    output("<Transform translation='");
    output(center);
    output("'");
    
    //Hey, hey -- quaternions to the rescue!
    // Just send three points to Quaternion to define a plane and return
    // the AxisAngle required to rotate to that position. That's all there is to it.
    
    AxisAngle4f a = Quaternion.getQuaternionFrame(center, points[1], points[3]).toAxisAngle4f();
    if (!Float.isNaN(a.x)) 
      output(" rotation='" + a.x + " " + a.y + " " + a.z + " " + a.angle + "'");
    tempP3.set(0, 0, 0);
    float sx = points[1].distance(center);
    float sy = points[3].distance(center);
    float sz = points[5].distance(center);
    output(" scale='" + sx + " " + sy + " " + sz + "'>");
    outputSphere(tempP3, 1.0f, colix);
    output("</Transform>\n");
  }

  protected void outputIsosurface(Point3f[] vertices, Vector3f[] normals,
                                  short[] colixes, int[][] indices,
                                  short[] polygonColixes,
                                  int nVertices, int nPolygons, int nFaces, BitSet bsFaces,
                                  int faceVertexMax, short colix, Vector colorList, Hashtable htColixes) {
    output("<Shape>\n");
    outputAppearance(colix, false);
    output("<IndexedFaceSet \n");

    if (polygonColixes != null)
      output(" colorPerVertex='FALSE'\n");

    // coordinates, part 1

    int[] coordMap = new int[nVertices];
    int n = 0;
    for (int i = 0; i < nVertices; i++) {
      if (Float.isNaN(vertices[i].x))
        continue;
      coordMap[i] = n++;
    }
      
    output("coordIndex='\n");
    for (int i = nPolygons; --i >= 0;) {
      if (!bsFaces.get(i))
        continue;
      output(" " + coordMap[indices[i][0]] + " " + coordMap[indices[i][1]] + " "
          + coordMap[indices[i][2]] + " -1\n");
      if (faceVertexMax == 4 && indices[i].length == 4)
        output(" " + coordMap[indices[i][0]] + " " + coordMap[indices[i][2]]
            + " " + coordMap[indices[i][3]] + " -1\n");
    }
    output("'\n");

    // normals, part 1  
    
    Vector vNormals = null;
    if (normals != null) {
      Hashtable htNormals = new Hashtable();
      vNormals = new Vector();
      int[] normalMap = new int[nVertices];
      output("  solid='FALSE'\n  normalPerVertex='TRUE'\n  ");
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
      
     output("  normalIndex='\n");
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
      output("'\n");
    }      
    
    // colors, part 1
        
    if (colorList != null) {
      output("  colorIndex='\n");
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
      output("'\n");
    }    


    output(">\n");  // closes IndexedFaceSet opening tag
    
    // coordinates, part 2
    
    output("<Coordinate point='\n");
    for (int i = 0; i < nVertices; i++) {
      if (Float.isNaN(vertices[i].x))
        continue;
      output(vertices[i]);
      output("\n");
    }
    output("'/>\n");

    coordMap = null;

    // normals, part 2

    if (normals != null) {
      output("<Normal vector='\n");
      n = vNormals.size();
      for (int i = 0; i < n; i++)
        output((String) vNormals.get(i));
      vNormals = null;
      output("'/>\n");
    }

    // colors, part 2

    if (colorList != null) {
      output("<Color color='\n");
      int nColors = colorList.size();
      for (int i = 0; i < nColors; i++) {
        String color = rgbFractionalFromColix(((Short) colorList.get(i)).shortValue(),
            ' ');
        output(" ");
        output(color);
        output("\n");
      }
      output("'/>\n");
    }
   
    output("</IndexedFaceSet>\n");
    output("</Shape>\n");
    
  }

  protected void outputSphere(Point3f center, float radius, short colix) {
    output("<Transform translation='");
    output(center);
    output("'>\n<Shape ");
    String child = useTable.getDef("S" + colix + "_" + (int) (radius * 100));
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "'>");
      output("<Sphere radius='" + radius + "'/>");
      outputAppearance(colix, false);
    } else {
      output(child + ">");
    }
    output("</Shape>\n");
    output("</Transform>\n");
  }

  private void outputTransRot(Point3f pt1, Point3f pt2, int x, int y, int z) {    
    output(" translation='");
    tempV1.set(pt2);
    tempV1.add(pt1);
    tempV1.scale(0.5f);
    output(tempV1);
    tempV1.sub(pt1);
    getAxisAngle(tempV1, x, y, z);
    output("' rotation='" + round(tempA.x) + " " + round(tempA.y) + " " 
      + round(tempA.z) + " " + round(tempA.angle) + "'" );
  }
  
  protected void outputTriangle(Point3f pt1, Point3f pt2, Point3f pt3, short colix) {
    // nucleic base
    // cartoons
    output("<Shape>\n");
    output("<IndexedFaceSet solid='FALSE' ");
    output("coordIndex='0 1 2 -1'>");
    output("<Coordinate point='");
    output(pt1);
    output(" ");
    output(pt2);
    output(" ");
    output(pt3);
    output("'/>");
    output("</IndexedFaceSet>\n");
    outputAppearance(colix, false);
    output("\n</Shape>\n");
  }

  protected void outputTextPixel(Point3f pt, int argb) {
    // text only
    String color = rgbFractionalFromArgb(argb, ' ');
    output("<Transform translation='");
    output(pt);
    output("'>\n<Shape ");
    String child = useTable.getDef("p" + argb);
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "'>");
      output("<Sphere radius='0.01'/>");
      output("<Appearance><Material diffuseColor='0 0 0' specularColor='0 0 0'"
        + " ambientIntensity='0.0' shininess='0.0' emissiveColor='" 
        + color + "'/></Appearance>'");
    } else {
      output(child + ">");
    }
    output("</Shape>\n");
    output("</Transform>\n");
  }

  void plotText(int x, int y, int z, short colix, String text, Font3D font3d) {
    if (z < 3)
      z = viewer.getFrontPlane();
    String useFontStyle = font3d.fontStyle.toUpperCase();
    String preFontFace = font3d.fontFace.toUpperCase();
    String useFontFace = (preFontFace.equals("MONOSPACED") ? "TYPEWRITER"
        : preFontFace.equals("SERIF") ? "SERIF" : "SANS");
    output("<Transform translation='");
    tempP3.set(x, y, z);
    viewer.unTransformPoint(tempP3, tempP1);
    output(tempP1);
    output("'>");
    // These x y z are 3D coordinates of echo or the atom the label is attached
    // to.
    output("<Billboard ");
    String child = useTable.getDef("T" + colix + useFontFace + useFontStyle + "_" + text);
    if (child.charAt(0) == '_') {
      output("DEF='" + child + "' axisOfRotation='0 0 0'>"
        + "<Transform translation='0.0 0.0 0.0'>"
        + "<Shape>");
      outputAppearance(colix, true);
      output("<Text string=" + Escape.escape(text) + ">");
      output("<FontStyle ");
      String fontstyle = useTable.getDef("F" + useFontFace + useFontStyle);
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


}
