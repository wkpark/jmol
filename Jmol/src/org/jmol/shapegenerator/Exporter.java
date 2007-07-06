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

package org.jmol.shapegenerator;

import javax.vecmath.Point3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;

public abstract class Exporter {
  StringBuffer output;
  Graphics3D g3d;
  
  Exporter(Graphics3D g3d, StringBuffer output) {
    this.g3d = g3d;
    this.output = output;
  }
  
  static Exporter allocate(Graphics3D g3d, StringBuffer output, String type) {
    if (type.equals("maya"))
      return new MayaExporter(g3d, output);
    return null;
  }

  abstract void renderBall(Atom atom, short colix); 
  abstract void renderCylinder(Atom atom1, Atom atom2, short colix1, short colix2, byte endcaps, int madBond); 
  abstract void fillSphereCentered(int radius, Point3f pt, short colix);
  abstract void fillTriangle(Point3f ptA, short colixA, short nA, 
                             Point3f ptB, short colixB, short nB, 
                             Point3f ptC, short colixC, short nC);

  String rgbFromColix(short colix) {
    int argb = g3d.getColixArgb(colix);
    return new StringBuffer("(")
    .append((argb >> 8) & 0xFF).append(',')
    .append((argb >> 16) & 0xFF).append(',')
    .append((argb >> 24) & 0xFF).append(')').toString();
  }
}
