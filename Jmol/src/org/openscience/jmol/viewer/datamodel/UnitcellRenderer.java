/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;


import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class UnitcellRenderer extends ShapeRenderer {

  void render() {
    Unitcell unitcell = (Unitcell)shape;
    short mad = unitcell.mad;
    short colix = unitcell.colix;
    if (mad == 0 || ! unitcell.hasUnitcell)
      return;
    BboxRenderer.render(viewer, g3d, mad, colix, unitcell.vertices,
                        frameRenderer.getTempScreens(8));
    /*
    render(viewer, g3d, mad, bbox.colix, bbox.bboxVertices, bboxScreens);

    Point3i[] screens = frameRenderer.getTempScreens(8);
    for (int i = 8; --i >= 0; )
      viewer.transformPoint(unitcell.vertices[i], screens[i]);
    short colix = unitcell.colix;
    for (int i = 0; i < 24; i += 2) {
      Point3i screenA = screens[Bbox.edges[i]];
      Point3i screenB = screens[Bbox.edges[i+1]];
      if (i < 6) {
        g3d.drawLine(colix, screenA, screenB);
      } else {
        g3d.drawDottedLine(colix, screenA, screenB);
      }
    }
    */

    g3d.drawString("a=" + unitcell.a, colix, 5, 15, 0);
    g3d.drawString("b=" + unitcell.b, colix, 5, 30, 0);
    g3d.drawString("c=" + unitcell.c, colix, 5, 45, 0);
    g3d.drawString("alpha=" + unitcell.alpha, colix, 5, 60, 0);
    g3d.drawString("beta =" + unitcell.beta,  colix, 5, 75, 0);
    g3d.drawString("gamma=" + unitcell.gamma, colix, 5, 90, 0);
  }
}
