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

import javax.vecmath.Point3i;

import org.jmol.shape.*;

public class StarsGenerator extends StarsRenderer {

  private Exporter exporter;
  
  public void initializeGenerator(Object exporter, String type, StringBuffer output) {
    super.initializeGenerator(exporter, type, output);
    isGenerator = true;
    this.exporter = (Exporter)exporter;
  }

  Point3i p1 = new Point3i();
  Point3i p2 = new Point3i();
  
  protected void drawLine(int x1, int y1, int z1, int x2, int y2, int z2) {
    p1.set(x1, y1, z1);
    p2.set(x2, y2, z2);
    exporter.drawLine(colix, p1, p2);
  }

}
