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

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Font3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;
import org.jmol.util.BitSetUtil;

public class _VrmlExporter extends _Exporter {

  //VERY  preliminary -- in process -- 7/2007 Bob Hanson

  public _VrmlExporter() {
    use2dBondOrderCalculation = false;
  }

  private void output(String data) {
    output.append(data);
  }
  
  public void getHeader() {
    output("#VRML V2.0 utf8\n");
    output("Transform {\n");
    output("translation " + -center.x + " " + -center.y + " "
        + -center.z + "\n");
    output("children [\n");
  }

  public void getFooter() {
    output("]\n");
    output("}\n");
  }

  public void renderAtom(Atom atom, short colix) {
    String color = rgbFractionalFromColix(colix, ' ');
    float r = atom.getMadAtom() / 2000f;
    output("Transform {\n");
    output("translation " + atom.x + " " + atom.y + " " + atom.z + "\n");
    output("children [\n");
    output("Shape {\n");
    output("geometry Sphere { radius " + r + " }\n");
    output("appearance Appearance {\n");
    output("material Material { diffuseColor " + color + " }\n");
    output("}\n");
    output("}\n");
    output("]\n");
    output("}\n");
  }

  public void fillCylinder(Point3f atom1, Point3f atom2, short colix1,
                         short colix2, byte endcaps, int madBond, int bondOrder) {
    //ignoring bond order for vrml -- but this needs fixing
    if (colix1 == colix2) {
      renderCylinder(atom1, atom2, colix1, endcaps, madBond);
      return;
    }
    tempV2.set(atom2);
    tempV2.add(atom1);
    tempV2.scale(0.5f);
    tempP1.set(tempV2);
    renderCylinder(atom1, tempP1, colix1, endcaps, madBond);
    renderCylinder(tempP1, atom2, colix2, endcaps, madBond);
  }

  public void renderCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, int madBond) {
    String color = rgbFractionalFromColix(colix, ' ');
    float length = pt1.distance(pt2);
    float r = madBond / 2000f;
    tempV1.set(pt2);
    tempV1.add(pt1);
    tempV1.scale(0.5f);
    output("Transform {\n");
    output("translation " + tempV1.x + " " + tempV1.y + " " + tempV1.z
        + "\n");
    tempV1.sub(pt1);
    getAxisAngle(tempV1);
    output("rotation " + tempA.x + " " + tempA.y + " " + tempA.z + " "
        + tempA.angle + "\n");
    output("children[\n");
    output("Shape {\n");
    output("geometry Cylinder { height " + length + " radius " + r
        + " }\n");
    output("appearance Appearance {\n");
    output("material Material { diffuseColor " + color + " }\n");
    output("}\n");
    output("}\n");
    output("]\n");
    output("}\n");
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                               short[] colixes, Vector3f[] normals,
                               int[][] indices, BitSet bsFaces, int nVertices,
                               int faceVertexMax) {

    if (nVertices == 0)
      return;
    int nFaces = 0;
    for (int i = BitSetUtil.length(bsFaces); --i >= 0;)
      if (bsFaces.get(i))
        nFaces += (faceVertexMax == 4 && indices[i].length == 4 ? 2 : 1);
    if (nFaces == 0)
      return;

    String color = rgbFractionalFromColix(colix, ' ');
    output("Shape {\n");
    output("appearance Appearance {\n");
    output("material Material { diffuseColor " + color + " }\n");
    output("}\n");
    output("geometry IndexedFaceSet {\n");
    output("coord Coordinate {\n");
    output("point [\n");
    for (int i = 0; i < nVertices; i++) {
      String sep = " ";
      output(sep + vertices[i].x + " " + vertices[i].y + " " + vertices[i].z
          + "\n");
      if (i == 0)
        sep = ",";
    }
    output("]\n");
    output("}\n");
    output("coordIndex [\n");
    String sep = " ";
    for (int i = BitSetUtil.length(bsFaces); --i >= 0;) {
      if (!bsFaces.get(i))
        continue;
      output(sep + indices[i][0] + " " + indices[i][1] + " " + indices[i][2]
          + " -1\n");
      if (i == 0)
        sep = ",";
      if (faceVertexMax == 4 && indices[i].length == 4)
        output(sep + indices[i][0] + " " + indices[i][2] + " " + indices[i][3]
            + " -1\n");
    }
    output("]\n");
    output("}\n");
    output("}\n");
  }

  public void renderText(Text t) {
  }

  public void drawString(short colix, String str, Font3D font3d, int xBaseline,
                         int yBaseline, int z, int zSlab) {
  }

  public void fillCylinder(short colix, byte endcaps, int diameter, Point3f screenA,
                           Point3f screenB) {
  }

  public void drawCircleCentered(short colix, int diameter, int x,
                                         int y, int z, boolean doFill) {
   //draw circle 
  }

  public void fillScreenedCircleCentered(short colix, int diameter, int x,
                                         int y, int z) {
   //halos 
  }

  public void drawPixel(short colix, int x, int y, int z) {
    //measures
  }

  public void drawTextPixel(int argb, int x, int y, int z) {
    //text only
  }

  public void fillTriangle(short colix, Point3f ptA, Point3f ptB, Point3f ptC) {
    //cartoons
  }

  public void fillCone(short colix, byte endcap, int diameter,
                       Point3f screenBase, Point3f screenTip) {
    //rockets
  }

  
  public void fillSphereCentered(short colix, int diameter, Point3f pt) {
    //cartoons, rockets, trace:    
  }

  public void plotText(int x, int y, int z, int argb, 
                       String text, Font3D font3d) {    
  }

  // not implemented: 
  
  public void fillHermite(short colix, int tension, int diameterBeg,
                          int diameterMid, int diameterEnd,
                          Point3f s0, Point3f s1, Point3f s2, Point3f s3){
    //cartoons, rockets, trace:
  }
  
  public void drawHermite(short colix, int tension,
                             Point3f s0, Point3f s1, Point3f s2, Point3f s3){
    //strands:
  }

  public void drawHermite(short colix, boolean fill, boolean border, int tension,
                            Point3f s0, Point3f s1, Point3f s2, Point3f s3,
                            Point3f s4, Point3f s5, Point3f s6, Point3f s7,
                            int aspectRatio) {
    //cartoons, meshRibbons:
  }

  public void renderEllipsoid(short colix, int x, int y, int z, int diameter, double[] coef, Point3i[] selectedPoints) {
    
  }

  public void plotImage(int x, int y, int z, Image image, short bgcolix, 
                        int width, int height) {
    // TODO
    
  }

  public void renderBackground() {
    // TODO
    
  }
           
}
