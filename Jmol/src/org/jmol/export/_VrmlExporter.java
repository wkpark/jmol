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

import java.awt.Image;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.viewer.Viewer;

public class _VrmlExporter extends _Exporter {

  // VERY preliminary -- in process -- 7/2007 Bob Hanson
  /*
   * 1/2009 Angel Herraez: # added support for translucency # Jmol info in
   * header # set navigation mode # added support for background color # added
   * support for labels: text, font face and font style; size is hardcoded to
   * 0.4A
   */

  public _VrmlExporter() {
    use2dBondOrderCalculation = false;
  }

  void output(String data) {
    output.append(data);
  }

  public void getHeader() {
    output("#VRML V2.0 utf8 Generated by Jmol " + Viewer.getJmolVersion()
        + "\n");
    output("WorldInfo { \n");
    output(" title \" \" \n"); // how to insert here molecule name or filename?
    output(" info [ \"Generated by Jmol " + Viewer.getJmolVersion() + " \", \n");
    output("  \"http://www.jmol.org \", \n");
    output("  \"Creation date: " + getExportDate() + " \" ]\n");
    output("} \n");

    output("NavigationInfo { type \"EXAMINE\" } \n");
    // puts the viewer into model-rotation mode
    output("Background { skyColor "
        + rgbFractionalFromColix(viewer.getObjectColix(0), ' ') + " } \n");

    output("Transform {\n");
    output("translation " + -center.x + " " + -center.y + " " + -center.z
        + "\n");
    output("children [\n\n");
  }

  public void getFooter() {
    // taken care of in finalizeOutput
  }

  public String finalizeOutput() {
    htSpheres = null;
    int n = vSpheres.size();
    for (int i = 0; i < n; i++)
      ((Sphere) vSpheres.get(i)).outputSphere();
    vSpheres = null;

    htCylinders = null;
    n = vCylinders.size();
    for (int i = 0; i < n; i++)
      ((Cylinder) vCylinders.get(i)).outputCylinder();
    vCylinders = null;

    output("\n]\n");
    output("}\n");
    return super.finalizeOutput();
  }

  public void renderAtom(Atom atom, short colix) {
    float r = atom.getMadAtom() / 2000f;
    cacheSphere(atom, r, colix);
  }

  private void cacheSphere(Point3f pt, float radius, short colix) {
    vSpheres.add(new Sphere(pt, radius, colix));
  }

  int iSphere;
  Hashtable htSpheres = new Hashtable();
  Vector vSpheres = new Vector();

  private class Sphere {
    private Point3f spt;
    private String child;

    Sphere(Point3f pt, float radius, short colix) {
      spt = pt;
      String key = "S_" + colix + "_" + (int) (radius * 1000);
      if (htSpheres.containsKey(key)) {
        child = "USE " + htSpheres.get(key);
      } else {
        String id = "S_" + (iSphere++);
        htSpheres.put(key, id);
        child = "DEF " + id + get(radius, colix);
      }
    }

    private String get(float radius, short colix) {
      String color = rgbFractionalFromColix(colix, ' ');
      String translu = translucencyFractionalFromColix(colix);
      StringBuffer sb = new StringBuffer();
      sb.append(" Shape {\n").append(
          "   geometry Sphere { radius " + radius + " }\n").append(
          "   appearance Appearance {\n").append(
          "    material Material { diffuseColor " + color + " transparency "
              + translu + " }\n").append("   }\n  }");
      return sb.toString();
    }

    void outputSphere() {
      output("Transform { translation ");
      output(spt);
      output("\n");
      output(" children [" + child + "]\n}\n");
    }
  }

  public void fillCylinder(Point3f atom1, Point3f atom2, short colix1,
                           short colix2, byte endcaps, int madBond,
                           int bondOrder) {
    // ignoring bond order for vrml -- but this needs fixing
    if (colix1 == colix2) {
      cacheCylinder(atom1, atom2, colix1, endcaps, madBond);
      return;
    }
    tempV2.set(atom2);
    tempV2.add(atom1);
    tempV2.scale(0.5f);
    Point3f pt = new Point3f(tempV2);
    cacheCylinder(atom1, pt, colix1, endcaps, madBond);
    cacheCylinder(pt, atom2, colix2, endcaps, madBond);
  }

  private void cacheCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, int madBond) {
    vCylinders.add(new Cylinder(pt1, pt2, colix, endcaps, madBond));
  }

  int iCylinder;
  Hashtable htCylinders = new Hashtable();
  Vector vCylinders = new Vector();

  private class Cylinder {
    private Point3f pt1, pt2;
    private String child;

    public Cylinder(Point3f pt1, Point3f pt2, short colix, byte endcaps,
        int madBond) {
      this.pt1 = pt1;
      this.pt2 = pt2;
      float length = pt1.distance(pt2);
      String key = "C_" + colix + "_" + (int) (length * 1000) + "_" + madBond
          + "_" + endcaps;
      if (htCylinders.containsKey(key)) {
        child = "USE " + htCylinders.get(key);
      } else {
        String id = "C_" + (iCylinder++);
        htCylinders.put(key, id);
        child = "DEF " + id + get(length, colix, endcaps, madBond);
      }
    }

    private String get(float length, short colix, byte endcaps, int madBond) {
      String color = rgbFractionalFromColix(colix, ' ');
      String translu = translucencyFractionalFromColix(colix);
      float r = madBond / 2000f;
      StringBuffer sb = new StringBuffer();
      sb.append("  Shape {\n");
      sb.append("   geometry Cylinder { height " + length + " radius " + r
          + " }\n");
      sb.append("   appearance Appearance {\n");
      sb.append("    material Material { diffuseColor " + color
          + " transparency " + translu + " }\n");
      sb.append("   }\n  }\n");
      return sb.toString();
    }

    void outputCylinder() {
      tempV1.set(pt2);
      tempV1.add(pt1);
      tempV1.scale(0.5f);
      output("Transform { translation ");
      output(tempV1);
      tempV1.sub(pt1);
      getAxisAngle(tempV1);
      output(" rotation " + tempA.x + " " + tempA.y + " " + tempA.z + " "
          + tempA.angle + "\n");
      output(" children [ " + child + " ]\n}\n");

    }
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

    String color = rgbFractionalFromColix(colix, ' ');
    String translu = translucencyFractionalFromColix(colix);
    output("Shape {\n");
    output(" appearance Appearance {\n");
    output("  material Material { diffuseColor " + color + " transparency "
        + translu + " }\n");
    output(" }\n");
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
      output(vertices[i].x + " " + vertices[i].y + " " + vertices[i].z + "\n");
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
        s = (normals[i].x + " " + normals[i].y + " " + normals[i].z + "\n");
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
        color = rgbFractionalFromColix(((Short) colorList.get(i)).shortValue(),
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

  public void renderText(Text t) {
  }

  public void drawString(short colix, String str, Font3D font3d, int xBaseline,
                         int yBaseline, int z, int zSlab) {
  }

  public void fillCylinder(short colix, byte endcaps, int diameter,
                           Point3f screenA, Point3f screenB) {
    Point3f ptA = new Point3f();
    Point3f ptB = new Point3f();
    viewer.unTransformPoint(screenA, ptA);
    viewer.unTransformPoint(screenB, ptB);
    int madBond = (int) (viewer.unscaleToScreen(
        (int)((screenA.z + screenB.z) / 2), diameter) * 1000);      
    cacheCylinder(ptA, ptB, colix, endcaps, madBond);

    // nucleic base
  }

  public void drawCircleCentered(short colix, int diameter, int x, int y,
                                 int z, boolean doFill) {
    // draw circle
  }

  public void fillScreenedCircleCentered(short colix, int diameter, int x,
                                         int y, int z) {
    // halos
  }

  public void drawPixel(short colix, int x, int y, int z) {
    // measures
  }

  public void drawTextPixel(int argb, int x, int y, int z) {
    // text only
  }

  void output(Tuple3f pt) {
    output(pt.x + " " + pt.y + " " + pt.z);
  }

  public void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    // nucleic base
    // cartoons
    String color = rgbFractionalFromColix(colix, ' ');
    String translu = translucencyFractionalFromColix(colix);
    output("Shape {\n");
    output(" appearance Appearance {\n");
    output("  material Material { diffuseColor " + color + " transparency "
        + translu + " }\n");
    output(" }\n");
    output(" geometry IndexedFaceSet {\n");
    output(" solid FALSE\n  coord Coordinate {\n   point [\n");
    viewer.unTransformPoint(ptA, pt);
    output(pt);
    output(" ");
    viewer.unTransformPoint(ptB, pt);
    output(pt);
    output(" ");
    viewer.unTransformPoint(ptC, pt);
    output(pt);
    output("   ]\n");
    output("  }\n");
    output("  coordIndex [ 0 1 2 -1 ]\n");
    output(" }\n");
    output("}\n");
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    //
  }

  public void fillSphereCentered(short colix, int diameter, Point3f pt) {
    viewer.unTransformPoint(pt, ptAtom);
    cacheSphere(new Point3f(ptAtom), viewer.unscaleToScreen((int)pt.z, diameter) / 2, colix);
  }

  final private Point3f pt = new Point3f();
  final private Point3f ptAtom = new Point3f();

  public void plotText(int x, int y, int z, int argb, String text, Font3D font3d) {
    // if (!haveAtomPoint)
    // return; // texts other than labels are not processed
    String color = rgbFractionalFromArgb(argb, ' ');
    String useFontStyle = font3d.fontStyle.toUpperCase();
    String preFontFace = font3d.fontFace.toUpperCase();
    String useFontFace = (preFontFace.equals("MONOSPACED") ? "TYPEWRITER"
        : preFontFace.equals("SERIF") ? "SERIF" : "SANS");
    output("Transform {\n");
    pt.set(x, y, z);
    viewer.unTransformPoint(pt, ptAtom);
    output(" translation ");
    output(ptAtom);
    output("\n");
    // These x y z are 3D coordinates of echo or the atom the label is attached
    // to.
    output(" children Billboard {\n");
    output("  axisOfRotation 0 0 0 \n");
    output("  children [\n");
    output("   Transform {\n");
    output("    translation 0.0 0.0 0.0 \n");
    output("    children Shape {\n");
    output("     appearance Appearance {\n");
    output("      material Material { diffuseColor 0 0 0 specularColor 0 0 0 "
        + "ambientIntensity 0.0 shininess 0.0 emissiveColor " + color + " }\n");
    output("     }\n");
    output("     geometry Text {\n");
    output("      fontStyle FontStyle { size 0.4 " + "family \"" + useFontFace
        + "\" style \"" + useFontStyle + "\" } \n");
    output("      string	\"" + text + "\" \n");
    output("     }\n");
    output("    }\n");
    output("   }\n");
    output("  ]\n");
    output(" }\n");
    output("}\n");

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

  // not implemented:

  public void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd, Point3f s0,
                          Point3f s1, Point3f s2, Point3f s3) {
    // cartoons, rockets, trace:
  }

  public void drawHermite(short colix, int tension, Point3f s0, Point3f s1,
                          Point3f s2, Point3f s3) {
    // strands:
  }

  public void drawHermite(short colix, boolean fill, boolean border,
                          int tension, Point3f s0, Point3f s1, Point3f s2,
                          Point3f s3, Point3f s4, Point3f s5, Point3f s6,
                          Point3f s7, int aspectRatio) {
    // cartoons, meshRibbons:
  }

  public void renderEllipsoid(short colix, int x, int y, int z, int diameter,
                              double[] coef, Point3i[] selectedPoints) {
    // good luck!
  }

  public void plotImage(int x, int y, int z, Image image, short bgcolix,
                        int width, int height) {
    // TODO

  }

  public void renderBackground() {
    // TODO

  }

  public void startShapeBuffer() {
    // for now, rather than doing this, it was simpler to
    // just set the hermiteLevel to 5 if it is 0
  }

  public void endShapeBuffer() {
    // processTrianglesAsIsosurface();
  }

  public boolean canDoTriangles() {
    return false;
  }

  public boolean isCartesianExport() {
    return true;
  }

  /*
   * private class Triangle{ short colix; Point3f ptA = new Point3f(); Point3f
   * ptB = new Point3f(); Point3f ptC = new Point3f(); Triangle(short colix,
   * Point3f ptA, Point3f ptB, Point3f ptC) { this.colix = colix;
   * this.ptA.set(ptA); this.ptB.set(ptB); this.ptC.set(ptC); } }
   */

}
