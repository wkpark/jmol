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

import org.jmol.shapebio.*;

public class RocketsGenerator extends RocketsRenderer {

  protected Exporter exporter;
  
  public void initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    this.exporter = (Exporter)exporter;
  }

  protected void fillSphereCentered(int diameter, Point3i pt) {
    exporter.fillSphereCentered(colixPending, diameter, pt);
  }

  protected void fillCylinderBits(byte endcaps, int diameter, Point3f screenA,
                                  Point3f screenB) {
    exporter.renderBond(screenA, screenB, colixPending, colixPending, endcaps, madPending, 1);
  }

  protected void fillTriangle(Point3f ptA, Point3f ptB, Point3f ptC) {
    exporter.fillTriangle(colixPending, ptA, ptB, ptC);
  }

  protected void fillQuadrilateral(Point3f ptA, Point3f ptB, Point3f ptC,
                                   Point3f ptD) {
    exporter.fillQuadrilateral(colixPending, ptA, ptB, ptC, ptD);
  }

  protected void fillCone(byte endcap, int diameter, Point3f screenBase,
                          Point3f screenTip) {
    exporter.fillCone(colixPending, endcap, madPending, screenBase, screenTip);
  }

  public void fillHermite(int tension, int diameterBeg, int diameterMid,
                          int diameterEnd, Point3i s0, Point3i s1, Point3i s2,
                          Point3i s3) {
    exporter.fillHermite(colix, tension, diameterBeg, diameterMid, diameterEnd,
        s0, s1, s2, s3);
  }

}
