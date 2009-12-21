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

import org.jmol.shape.*;

public class SticksGenerator extends SticksRenderer {
  protected void drawBond(int dottedMask) {
    if (((Export3D)g3d).isCartesianExport() && bondOrder == 1)
      // bypass screen rendering and just use the atoms themselves
      ((Export3D)g3d).fillCylinder(atomA, atomB, colixA, colixB, endcaps, mad, -1);
    else
      // POVRAY always uses screen coordinates
      // Other renderers must use screen coordinates for double bonds
      super.drawBond(dottedMask);
  }
  
  protected void fillCylinder(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB,
                              int yB, int zB) {
    g3d.fillCylinder(colixA, colixB, endcaps, mad == 1 ? diameter : mad, xA, yA, zA, xB, yB, zB);
  }
}
