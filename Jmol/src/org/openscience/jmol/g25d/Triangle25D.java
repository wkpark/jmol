/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.openscience.jmol.g25d;

import org.openscience.jmol.*;

import java.awt.Component;
//import java.awt.Graphics;
import java.awt.image.MemoryImageSource;
import java.awt.Color;
import java.util.Hashtable;

public class Triangle25D {

  DisplayControl control;
  Graphics25D g25d;

  int ax[] = new int[4];
  int ay[] = new int[4];
  int az[] = new int[4];

  public Triangle25D(DisplayControl control, Graphics25D g25d) {
    this.control = control;
    this.g25d = g25d;
  }

  void fillTriangle() {
    int iMinY = 0;
    if (ay[1] < ay[0]) iMinY = 1;
    if (ay[2] < ay[iMinY]) iMinY = 2;
    int iMidY = (iMinY + 1) % 3;
    int iMaxY = (iMinY + 2) % 3;
    if (ay[iMidY] > ay[iMaxY]) { int t = iMidY; iMidY = iMaxY; iMaxY = t; }

    /*
    System.out.println("----fillTriangle\n" +
                       " iMinY=" + iMinY + " iMidY=" + iMidY +
                       " iMaxY=" + iMaxY + "\n" +
                       "  minY=" + ax[iMinY] + "," + ay[iMinY] + "\n" +
                       "  midY=" + ax[iMidY] + "," + ay[iMidY] + "\n" +
                       "  maxY=" + ax[iMaxY] + "," + ay[iMaxY] + "\n");
    */                 

    if (ay[iMinY] < ay[iMidY]) {
      if (ay[iMidY] == ay[iMaxY]) {
        fillUpper(iMinY, iMidY, iMaxY);
        return;
      }
      int dxMax = ax[iMaxY] - ax[iMinY];
      int dyMax = ay[iMaxY] - ay[iMinY];
      int dzMax = az[iMaxY] - az[iMinY];
      int dyMid = ay[iMidY] - ay[iMinY];
      int roundX = (dxMax < 0) ? -dyMax/2 : dyMax/2;
      int roundZ = (dzMax < 0) ? -dyMax/2 : dyMax/2;
      ax[3] = ax[iMinY] + (dxMax * dyMid + roundX) / dyMax;
      ay[3] = ay[iMidY];
      az[3] = az[iMinY] + (dzMax * dyMid + roundZ) / dyMax;
      fillUpper(iMinY, iMidY, 3);
      iMinY = 3;
    }
    fillLower(iMinY, iMidY, iMaxY);
  }

  void fillUpper(int iTop, int iLeft, int iRight) {
    if (ax[iLeft] > ax[iRight]) { int t = iLeft; iLeft = iRight; iRight = t; }
    /*
    System.out.println("fillUpper\n" +
                       "   top=" + ax[iTop] + "," + ay[iTop] + "\n" +
                       "  left=" + ax[iLeft] + "," + ay[iLeft] + "\n" +
                       " right=" + ax[iRight] + "," + ay[iRight] + "\n");
    */
    g25d.plotLine(ax[iTop], ay[iTop], az[iTop],
                  ax[iLeft], ay[iLeft], az[iLeft]);
    g25d.plotLine(ax[iTop], ay[iTop], az[iTop],
                  ax[iRight], ay[iRight], az[iRight]);
    g25d.plotLine(ax[iRight], ay[iRight], az[iRight],
                  ax[iLeft], ay[iLeft], az[iLeft]);
  }

  void fillLower(int iLeft, int iRight, int iBottom) {
    if (ax[iLeft] > ax[iRight]) { int t = iLeft; iLeft = iRight; iRight = t; }
    /*
    System.out.println("fillLower\n" +
                       "  left=" + ax[iLeft] + "," + ay[iLeft] + "\n" +
                       " right=" + ax[iRight] + "," + ay[iRight] + "\n" +
                       "bottom=" + ax[iBottom] + "," + ay[iBottom] + "\n");
    */
    g25d.plotLine(ax[iBottom], ay[iBottom], az[iBottom],
                  ax[iLeft], ay[iLeft], az[iLeft]);
    g25d.plotLine(ax[iBottom], ay[iBottom], az[iBottom],
                  ax[iRight], ay[iRight], az[iRight]);
    g25d.plotLine(ax[iRight], ay[iRight], az[iRight],
                  ax[iLeft], ay[iLeft], az[iLeft]);
  }
}
