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
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.util.Escape;
import org.jmol.util.Quaternion;
import org.jmol.viewer.Viewer;

public class _X3dExporter extends _Exporter {

  private AxisAngle4f viewpoint = new AxisAngle4f();
  
  public _X3dExporter() {
    use2dBondOrderCalculation = true;
    canDoTriangles = false;
    isCartesianExport = true;
  }

  private void output(String data) {
    output.append(data);
  }

  private void output(Tuple3f pt) {
    output.append(round(pt.x)).append(" ").append(round(pt.y)).append(" ").append(round(pt.z));
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
      return "USE='" + htDefs.get(key) +"'";
    String id = "_" + (iObj++);
    htDefs.put(key, id);
    return id;
  }
  
  public void getHeader() {
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

    Point3f boxC = viewer.getBoundBoxCenter();
    Vector3f boxV = viewer.getBoundBoxCornerVector();
    float viewP = boxC.z + (1.5f)* boxV.z; // to get away from the model, 25% of a boundbox dimension
    float scale = round( viewer.getZoomPercentFloat() / 100f );
    viewer.getAxisAngle(viewpoint);
    
    output("<Transform rotation='"
      + round(viewpoint.x) + " " + round(viewpoint.y) + " " 
      + round(viewpoint.z) + " " + round(viewpoint.angle) );
    output("' translation='0 0 " + round(-viewP) 
      + "' scale='" + scale + " " + scale + " " + scale 
      + "'>\n");
    output("<Transform translation='"
      + round(-boxC.x) + " " + round(-boxC.y) + " " + round(-boxC.z) 
      + "'>\n");
  }

  public void getFooter() {
    htDefs = null;
    output("</Transform>\n");
    output("</Transform>\n");
    output("</Scene>\n");
    output("</X3D>\n");
  }

  private void outputAppearance(short colix, boolean isText) {  
    String def = getDef((isText ? "T" : "") + colix);
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
  
  public void renderAtom(Atom atom, short colix) {
    float r = atom.getMadAtom() / 2000f;
    outputSphere(atom, r, colix);
  }

  public void drawPixel(short colix, int x, int y, int z) {
    pt.set(x, y, z);
    viewer.unTransformPoint(pt, ptAtom);
    outputSphere(ptAtom, 0.01f, colix);
  }
    
  public void fillSphereCentered(short colix, int diameter, Point3f pt) {
    viewer.unTransformPoint(pt, ptAtom);
    outputSphere(ptAtom, viewer.unscaleToScreen((int)pt.z, diameter) / 2, colix);
  }

  private void outputSphere(Point3f center, float radius, short colix) {
    output("<Transform translation='");
    output(center);
    output("'>\n<Shape ");
    String child = getDef("S" + colix + "_" + (int) (radius * 100));
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
    output("<Transform");
    outputTransRot(pt1, pt2, 0, 1, 0);
    output(">\n");
    outputCylinderChild(pt1, pt2, colix, endcaps, madBond);
    output("\n</Transform>\n");
    if (endcaps == Graphics3D.ENDCAPS_SPHERICAL) {
      outputSphere(pt1, madBond / 2000f*1.01f, colix);
      outputSphere(pt2, madBond / 2000f*1.01f, colix);
    }
  }

  private void outputCylinderChild(Point3f pt1, Point3f pt2, short colix,
                                   byte endcaps, int madBond) {
    float length = round(pt1.distance(pt2));
    String child = getDef("C" + colix + "_" + (int) (length * 100) + "_" + madBond
        + "_" + endcaps);
    output("<Shape ");
    if (child.charAt(0) == '_') {
      float r = madBond / 2000f;
      output("DEF='" + child + "'>");
      output("<Cylinder ");
      String cyl = getDef("c" + length + "_" + endcaps + "_" + madBond);
      if (cyl.charAt(0) == '_') {
        output("DEF='" + cyl + "' height='" 
            + round(length) + "' radius='" + r + "'" 
            + (endcaps == Graphics3D.ENDCAPS_FLAT ? "" : " top='FALSE' bottom='FALSE'") + "/>");
      } else {
        output(cyl + "/>");
      }
      outputAppearance(colix, false);
    } else {
      output(child + ">");
    }
    output("</Shape>");
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
  
  public void fillCone(short colix, byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    viewer.unTransformPoint(screenBase, tempP1);
    viewer.unTransformPoint(screenTip, tempP2);
    float d = viewer.unscaleToScreen((int)screenBase.z, diameter);
    if (d < 0.1f)
      d = 0.1f;
    float height = tempP1.distance(tempP2);
    output("<Transform");
    outputTransRot(tempP1, tempP2, 0, 1, 0);
    output(">\n<Shape ");
    String child = getDef("c" + (int) (height * 100) + "_" + (int) (d * 100));
    if (child.charAt(0) == '_') {
      output("DEF='" + child +  "'>");
      output("<Cone height='" + round(height) + "' bottomRadius='" + round(d/2) + "'/>");
      outputAppearance(colix, false);
    } else {
      output(child + "/>");
    }
    output("</Shape>\n");
    output("</Transform>\n");
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
    // cartoons
    output("<Shape>\n");
    output("<IndexedFaceSet solid='FALSE' ");
    output("coordIndex='0 1 2 -1'>");
    output("<Coordinate point='");
    viewer.unTransformPoint(ptA, pt);
    output(pt);
    output(" ");
    viewer.unTransformPoint(ptB, pt);
    output(pt);
    output(" ");
    viewer.unTransformPoint(ptC, pt);
    output(pt);
    output("'/>");
    output("</IndexedFaceSet>\n");
    outputAppearance(colix, false);
    output("\n</Shape>\n");
  }

  final private Point3f pt = new Point3f();
  final private Point3f ptAtom = new Point3f();

  public void plotText(int x, int y, int z, short colix, String text, Font3D font3d) {
    if (z < 3) {
      viewer.transformPoint(center, pt);
      z = (int)pt.z;
    }
    String useFontStyle = font3d.fontStyle.toUpperCase();
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

  int iShapeBuffer;
  
  public void startShapeBuffer(int iShape) {
    iShapeBuffer = 1;
    switch(iShapeBuffer = iShape) {
    default:
    
    }
  }

  public void endShapeBuffer() {
    switch(iShapeBuffer) {
    default:
      output("}}\n");
    }
    iShapeBuffer = 0;
  }

  public void renderText(Text t) {
  }

  public void drawString(short colix, String str, Font3D font3d, int xBaseline,
                         int yBaseline, int z, int zSlab) {
  }

  public void drawCircleCentered(short colix, int diameter, int x, int y,
                                 int z, boolean doFill) {
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
    String color = rgbFractionalFromArgb(argb, ' ');
    output("<Transform translation='");
    output(ptAtom);
    output("'>\n<Shape ");
    String child = getDef("p" + argb);
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

  public void plotImage(int x, int y, int z, Image image, short bgcolix,
                        int width, int height) {
    // background, for example
  }

  void renderEllipsoid(Point3f center, Point3f[] points, short colix, int x,
                       int y, int z, int diameter, Matrix3f toEllipsoidal,
                       double[] coef, Matrix4f deriv, Point3i[] octantPoints) {
    output("<Transform translation='");
    output(center);
    output("'");
    
    //Hey, hey -- quaternions to the rescue!
    // Just send three points to Quaternion to define a plane and return
    // the AxisAngle required to rotate to that position. That's all there is to it.
    
    AxisAngle4f a = Quaternion.getQuaternionFrame(center, points[1], points[3]).toAxisAngle4f();
    if (!Float.isNaN(a.x)) 
      output(" rotation='" + a.x + " " + a.y + " " + a.z + " " + a.angle + "'");
    pt.set(0, 0, 0);
    float sx = points[1].distance(center);
    float sy = points[3].distance(center);
    float sz = points[5].distance(center);
    output(" scale='" + sx + " " + sy + " " + sz + "'>");
    outputSphere(pt, 1.0f, colix);
    output("</Transform>\n");
  }

}
