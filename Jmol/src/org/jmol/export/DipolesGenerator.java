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

import org.jmol.shapespecial.*;

public class DipolesGenerator extends DipolesRenderer {

  private _Exporter exporter;
  
  public void initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    this.exporter = (_Exporter)exporter;
  }

  protected void fillCylinder(byte endcaps, int diameter, Point3i screenA,
                              Point3i screenB) {
    exporter.fillCylinder(colix, endcaps, mad, screenA, screenB);
  }

  protected void fillCylinderBits(byte endcaps, int diameter, Point3f screenA,
                                  Point3f screenB) {
    exporter.renderBond(screenA, screenB, colix, colix, endcaps, mad, 1);
  }

  private Point3f ptA = new Point3f();
  private Point3f ptB = new Point3f();

  protected void fillCone(byte endcap, int diameter,
                          Point3i screenBase, Point3i screenTip) {
    ptA.set(screenBase.x, screenBase.y, screenBase.z);
    ptB.set(screenTip.x, screenTip.y, screenTip.z);
    exporter.fillCone(colix, endcap, mad, ptA, ptB);
   }

}
