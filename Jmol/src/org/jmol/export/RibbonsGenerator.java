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
import javax.vecmath.Point3i;

import org.jmol.shapebio.RibbonsRenderer;

public class RibbonsGenerator extends RibbonsRenderer {

  private _Exporter exporter;
  
  public void initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    this.exporter = (_Exporter)exporter;
  }

  protected void drawHermite(int tension, Point3i s0, Point3i s1, Point3i s2,
                             Point3i s3) {
    exporter.drawHermite(colix, tension, s0, s1, s2, s3);
  }

  protected void drawHermite(boolean fill, boolean border, int tension,
                             Point3i s0, Point3i s1, Point3i s2, Point3i s3,
                             Point3i s4, Point3i s5, Point3i s6, Point3i s7,
                             int aspectRatio) {
    exporter.drawHermite(colix, fill, border, tension, s0, s1, s2, s3, s4, s5,
        s6, s7, aspectRatio);
  }

  private Point3f ptA = new Point3f();
  private Point3f ptB = new Point3f();

  protected void fillCylinder(short colixA, short colixB, byte endcaps,
                              int diameter, int xA, int yA, int zA, int xB, int yB, int zB) {
    /*
     * Use the screen points Jmol determines 
     * 
     * this also uses the diameter -- fixed at 3 pixels, but will be 0.003 angstroms here.
     *  
     */
    ptA.set(xA, yA, zA);
    ptB.set(xB, yB, zB);
    exporter.renderBond(ptA, ptB, colixA, colixB, endcaps, diameter, 1);
  }


}
