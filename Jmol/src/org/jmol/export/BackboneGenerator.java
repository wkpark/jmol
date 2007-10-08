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

import org.jmol.g3d.Graphics3D;
import org.jmol.shapebio.*;

public class BackboneGenerator extends BackboneRenderer {

  private Exporter exporter;
  
  public void initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    this.exporter = (Exporter)exporter;
  }

  protected void drawLine(short colixA, short colixB, int xA, int yA, int zA, int xB, int yB, int zB) {
    //mads[i] < 0
    fillCylinder(colixA, colixB, Graphics3D.ENDCAPS_FLAT, 1, xA, yA, zA, xB, yB, zB);
  }
  
  Point3f ptA = new Point3f();
  Point3f ptB = new Point3f();

  protected void fillCylinder(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB, int yB, int zB) {
    /*
     * Use the screen points Jmol determines
     *  
     */
    ptA.set(xA, yA, zA);
    ptB.set(xB, yB, zB);
    exporter.renderBond(ptA, ptB, colixA, colixB, endcaps, madBond, 1);
  }
}
