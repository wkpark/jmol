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

import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class BboxRenderer extends Renderer {

  final Point3i[] bboxScreen = new Point3i[8];
  {
    for (int i = 8; --i >= 0; )
      bboxScreen[i] = new Point3i();
  }

  BboxRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    super(viewer, frameRenderer);
  }

  void render() {
    Bbox bbox = (Bbox)graphic;
    if (! bbox.showBoundingBox)
      return;
    for (int i = 8; --i >= 0; )
      viewer.transformPoint(bbox.bboxPoints[i], bboxScreen[i]);
    short colix = viewer.getColixAxes();
    for (int i = 0; i < 24; i += 2)
      g3d.drawDottedLine(colix,
                         bboxScreen[bbox.edges[i]],
                         bboxScreen[bbox.edges[i+1]]);
  }
}
