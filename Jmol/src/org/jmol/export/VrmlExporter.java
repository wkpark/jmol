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

import javax.vecmath.Point3f;
import org.jmol.modelset.Atom;

public class VrmlExporter extends Exporter {
  
  //VERY preliminary -- in process -- 7/2007 Bob Hanson

  public void getHeader() {
    output.append("#VRML V2.0 utf8\n");
    output.append("Separator {\n");
    output.append("  DEF SceneInfo Info {\n");
    output.append("  string \"WebLab Viewer\"\n");
    output.append("  }\n");
    output.append("  DEF Title Info {\n");
    output.append("  string \"molecule-name-here\"\n");
    output.append("  }\n");
    output.append("  DEF Viewer Info {\n");
    output.append("  string \"Jmol \"\n");
    output.append("  }\n");
    output.append("");
    //header stuff
  }

  public void getFooter() {
  }

  public void renderAtom(Atom atom, short colix) {
    //String color = rgbFromColix(colix);
    output.append("Transform {\n");
    output.append("translation " + atom.x + " " + atom.y + " " + atom.z +"\n");
    output.append("children [\"+\"]\n");
    output.append("        Shape {\n");
    output.append("          appearance Appearance {\n");
    output.append("            material Material {\n");
    output.append("              diffuseColor 0.0 1.0 0.0\n");
    output.append("            }\n");
    output.append("          }\n");
    output.append("          geometry Sphere {\n");
    output.append("          }\n");
    output.append("        }\n");
    output.append("      ]\n");
    output.append("    }\n");
    nBalls++;
  }

  public void renderBond(Atom atom1, Atom atom2, short colix1, short colix2,
                      byte endcaps, int madBond) {
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

  public void renderCylinder(Point3f pt1, Point3f pt2, short colix, byte endcaps,
                      int madBond) {
    nCyl++;
    //TODO
  }

  public void fillSphereCentered(int radius, Point3f pt, short colix) {
   //not a mad -- a number of pixels?
   //TODO
  }
  
  public void fillTriangle(Point3f ptA, short colixA, short nA, 
                             Point3f ptB, short colixB, short nB, 
                             Point3f ptC, short colixC, short nC) {
   //this would fill an array, not write directly
   //TODO
  }
}
