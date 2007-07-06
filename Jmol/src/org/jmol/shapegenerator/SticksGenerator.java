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

import org.jmol.shape.*;

public class SticksGenerator extends SticksRenderer {

  private Exporter exporter;
  
  public Object initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    if (exporter == null)
      exporter = Exporter.allocate(g3d, output, type);
    this.exporter = (Exporter)exporter;
    return exporter;
  }

  protected void renderCylinder(int dottedMask) {
    //boolean lineBond = (width <= 1);
    //if (bondOrder == 1) {
      //if ((dottedMask & 1) != 0) {
       // drawDashed(lineBond, xA, yA, zA, xB, yB, zB);
      //} else {
        //if (lineBond)
        //  g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
      //  else if (asBits) // time test shows bitset method to be slower
        //  g3d.fillCylinderBits(colixA, colixB, endcaps,
          //    width, xA, yA, zA, xB, yB, zB);
       // else
          exporter.renderCylinder(atomA, atomB, colixA, colixB, endcaps, madBond);
      //}
    //}
  }

}
