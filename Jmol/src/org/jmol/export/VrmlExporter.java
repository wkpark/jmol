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

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Font3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Text;

public class VrmlExporter extends Exporter {

  //VERY  preliminary -- in process -- 7/2007 Bob Hanson

  public VrmlExporter() {
    use2dBondOrderCalculation = false;
  }

  public void getHeader() {
    output.append("#VRML V2.0 utf8\n");
    output.append("Transform {\n");
    output.append("translation " + -center.x + " " + -center.y + " "
        + -center.z + "\n");
    output.append("children [\n");
  }

  public void getFooter() {
    output.append("]\n");
    output.append("}\n");
  }

  public void renderAtom(Atom atom, short colix) {
    String color = rgbFractionalFromColix(colix, ' ');
    float r = atom.getMadAtom() / 2000f;
    output.append("Transform {\n");
    output.append("translation " + atom.x + " " + atom.y + " " + atom.z + "\n");
    output.append("children [\n");
    output.append("Shape {\n");
    output.append("geometry Sphere { radius " + r + " }\n");
    output.append("appearance Appearance {\n");
    output.append("material Material { diffuseColor " + color + " }\n");
    output.append("}\n");
    output.append("}\n");
    output.append("]\n");
    output.append("}\n");
    nBalls++;
  }

  public void renderBond(Point3f atom1, Point3f atom2, short colix1,
                         short colix2, byte endcaps, int madBond, int bondOrder) {
    //ignoring bond order for vrml -- but this needs fixing
    if (colix1 == colix2) {
      renderCylinder(atom1, atom2, colix1, endcaps, madBond);
      return;
    }
    temp2.set(atom2);
    temp2.add(atom1);
    temp2.scale(0.5f);
    tempP.set(temp2);
    renderCylinder(atom1, tempP, colix1, endcaps, madBond);
    renderCylinder(tempP, atom2, colix2, endcaps, madBond);
  }

  public void renderCylinder(Point3f pt1, Point3f pt2, short colix,
                             byte endcaps, int madBond) {
    nCyl++;
    String color = rgbFractionalFromColix(colix, ' ');
    float length = pt1.distance(pt2);
    float r = madBond / 2000f;
    tempV.set(pt2);
    tempV.add(pt1);
    tempV.scale(0.5f);
    output.append("Transform {\n");
    output.append("translation " + tempV.x + " " + tempV.y + " " + tempV.z
        + "\n");
    tempV.sub(pt1);
    getAxisAngle(tempV);
    output.append("rotation " + tempA.x + " " + tempA.y + " " + tempA.z + " "
        + tempA.angle + "\n");
    output.append("children[\n");
    output.append("Shape {\n");
    output.append("geometry Cylinder { height " + length + " radius " + r
        + " }\n");
    output.append("appearance Appearance {\n");
    output.append("material Material { diffuseColor " + color + " }\n");
    output.append("}\n");
    output.append("}\n");
    output.append("]\n");
    output.append("}\n");
  }

  public void fillSphereCentered(int mad, Point3f pt, short colix) {
    //not a mad -- a number of pixels?
    //TODO
  }

  public void renderIsosurface(Point3f[] vertices, short colix,
                               short[] colixes, short[] normals,
                               int[][] indices, BitSet bsFaces, int nVertices,
                               int nPoints) {

    String color = rgbFractionalFromColix(colix, ' ');
    output.append("Shape {\n");
    output.append("appearance Appearance {\n");
    output.append("material Material { diffuseColor " + color + " }\n");
    output.append("}\n");
    output.append("geometry IndexedFaceSet {\n");
    output.append("coord Coordinate {\n");
    output.append("point [\n");
    for (int i = 0; i < nVertices; i++) {
      String sep = " ";
      output.append(sep + vertices[i].x + " " + vertices[i].y + " "
          + vertices[i].z + "\n");
      if (i == 0)
        sep = ",";
    }
    output.append("]\n");
    output.append("}\n");
    output.append("coordIndex [\n");
    String sep = " ";
    for (int i = 0; i < nPoints; i++)
      if (bsFaces.get(i)) {
        output.append(sep + indices[i][0] + " " + indices[i][1] + " "
            + indices[i][2] + " -1\n");
        if (i == 0)
          sep = ",";
      }
    output.append("]\n");
    output.append("}\n");
    output.append("}\n");
  }

  public void renderText(Text t) {
  }

  public void drawString(short colix, String str, Font3D font3d, int xBaseline,
                         int yBaseline, int z, int zSlab) {
  }

  public void fillCylinder(short colix, byte endcaps, int diameter, Point3i screenA,
                           Point3i screenB) {
  }

  public void drawDottedLine(short colix, Point3i pointA, Point3i pointB) {
  }

  public void drawPoints(short colix, int count, int[] coordinates) { 
  }

}
