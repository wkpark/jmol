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
import javax.vecmath.Tuple3f;
import org.jmol.modelset.Atom;

public class MayaExporter extends Exporter {
  
 public void getHeader() {
    output.append("//  Maya ASCII 8.5 scene\n");
    output.append("//  Name: ball_stripped.ma\n");
    //    output.append("//  CreatedBy: Jmol");
    output.append("//  Last modified: Thu, Jul 5, 2007 10:25:55 PM\n");
    output.append("//  Codeset: UTF-8\n");
    output.append("requires maya \"8.5\";\n");
    output.append("currentUnit -l centimeter -a degree -t film;\n");
    output.append("fileInfo \"application\" \"maya\";\n");
    output.append("fileInfo \"product\" \"Maya Unlimited 8.5\";\n");
    output.append("fileInfo \"version\" \"8.5\";\n");
    output.append("fileInfo \"cutIdentifier\" \"200612170012-692032\";\n");
    output.append("fileInfo \"osv\" \"Mac OS X 10.4.9\";  \n");
  }

  public void getFooter() {
    //no footer is necessary
  }

  public void renderAtom(Atom atom, short colix) {
    //String color = rgbFromColix(colix);
    nBalls++;
    name = "nurbsSphere" + nBalls;
    id = "nurbsSphereShape" + nBalls;

    output.append("createNode transform -n \"" + name + "\";\n");
    setAttr("t", atom);
    output.append("createNode nurbsSurface -n \"" + id + "\" -p \"" + name
        + "\";\n");
    addAttr();
    output.append("createNode makeNurbSphere -n \"make" + name + "\";\n");
    output.append(" setAttr \".ax\" -type \"double3\" 0 1 0;\n");
    setAttr("r", atom.getMadAtom() / 2000f);
    setAttr("s", 4);
    setAttr("nsp", 3);
    addConnect();
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
    name = "nurbsCylinder" + nCyl;
    id = "nurbsCylinderShape" + nCyl;
    output.append(" createNode transform -n \"" + name + "\";\n");
    float length = pt1.distance(pt2);
    tempV.set(pt2);
    tempV.sub(pt1);
    tempV.scale(0.5f);
    tempV.add(pt1);
    setAttr("t", tempV);
    tempV.sub(pt1);
    setAttr("r", getRotation(tempV));
    //    output.append(" setAttr \".t\" -type \"double3\" -0.27200355743991733 3.0727117892444111 -0.05978379897102204 ;\n");
    //   output.append(" setAttr \".r\" -type \"double3\" 55.997780776782392 37.250030773306385 3.8482339713562377 ;\n");
    output.append(" createNode nurbsSurface -n \"" + id + "\" -p \"" + name
        + "\";\n");
    addAttr();
    output.append("createNode makeNurbCylinder -n \"make" + name + "\";\n");
    output.append(" setAttr \".ax\" -type \"double3\" 0 1 0;\n");
    float radius = madBond / 2000f;
    setAttr("r", radius);
    setAttr("s", 4);
    setAttr("hr", length / radius);
    addConnect();
  }

  private void setAttr(String attr, float val) {
    output.append(" setAttr \"." + attr + "\" " + val + ";\n");
  }

  private void setAttr(String attr, int val) {
    output.append(" setAttr \"." + attr + "\" " + val + ";\n");
  }

  private void setAttr(String attr, Tuple3f pt) {
    output.append(" setAttr \"." + attr + "\" -type \"double3\" " + pt.x + " "
        + pt.y + " " + pt.z + ";\n");
  }

  private void addAttr() {
    output.append(" setAttr -k off \".v\";\n");
    output.append(" setAttr \".vir\" yes;\n");
    output.append(" setAttr \".vif\" yes;\n");
    output.append(" setAttr \".tw\" yes;\n");
    output.append(" setAttr \".covm[0]\"  0 1 1;\n");
    output.append(" setAttr \".cdvm[0]\"  0 1 1;\n");
  }

  private void addConnect() {
    output.append(" connectAttr \"make" + name + ".os\" \"" + id + ".cr\";\n");
    output.append("connectAttr \"" + id
        + ".iog\" \":initialShadingGroup.dsm\" -na;\n");
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
