/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
package org.jmol.viewer.datamodel;

import org.jmol.viewer.*;
import org.jmol.g3d.Graphics3D;


import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import java.text.NumberFormat;

class UccageRenderer extends ShapeRenderer {

  NumberFormat nf;
  byte fid;

  void initRenderer() {
    nf = NumberFormat.getInstance();
    fid = g3d.getFontFid("Monospaced", 12);
  }

  final Point3i[] screens = new Point3i[8];
  {
    for (int i = 8; --i >= 0; )
      screens[i] = new Point3i();
  }

  void render() {
    Uccage uccage = (Uccage)shape;
    short mad = uccage.mad;
    short colix = uccage.colix;
    if (mad == 0 || ! uccage.hasUnitcell)
      return;
    BbcageRenderer.render(viewer, g3d, mad, colix, frame.unitcellVertices,
                          screens);
    /*
    render(viewer, g3d, mad, bbox.colix, bbox.bboxVertices, bboxScreens);

    Point3i[] screens = frameRenderer.getTempScreens(8);
    for (int i = 8; --i >= 0; )
      viewer.transformPoint(uccage.vertices[i], screens[i]);
    short colix = uccage.colix;
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

    g3d.setFont(fid);
    nf.setMaximumFractionDigits(3);
    nf.setMinimumFractionDigits(3);
    g3d.drawString("a=" + nf.format(uccage.a) + "\u00C5", colix, 5, 15, 0);
    g3d.drawString("b=" + nf.format(uccage.b) + "\u00C5", colix, 5, 30, 0);
    g3d.drawString("c=" + nf.format(uccage.c) + "\u00C5", colix, 5, 45, 0);
    nf.setMaximumFractionDigits(1);
    g3d.drawString("\u03B1=" + nf.format(uccage.alpha) + "\u00B0", colix, 5, 60, 0);
    g3d.drawString("\u03B2=" + nf.format(uccage.beta)  + "\u00B0", colix, 5, 75, 0);
    g3d.drawString("\u03B3=" + nf.format(uccage.gamma) + "\u00B0", colix, 5, 90, 0);
  }
}
