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

package org.openscience.jmol.viewer.datamodel;

import org.openscience.jmol.viewer.*;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.g3d.Shade3D;
import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class TraceRenderer extends Renderer {

  TraceRenderer(JmolViewer viewer) {
    this.viewer = viewer;
  }

  Point3i s0 = new Point3i();
  Point3i s1 = new Point3i();
  Point3i s2 = new Point3i();
  Point3i s3 = new Point3i();

  void render(Graphics3D g3d, Rectangle rectClip, Frame frame) {
    Trace trace = frame.trace;
    if (trace == null || !trace.initialized)
      return;
    Atom[][] chains = trace.chains;
    Point3f[][] midPointsChains = trace.midPointsChains;
    short[][] madsChains = trace.madsChains;
    short[][] colixesChains = trace.colixesChains;
    for (int i = chains.length; --i >= 0; )
      renderChain(g3d, chains[i], midPointsChains[i],
                  madsChains[i], colixesChains[i]);
  }

  void renderChain(Graphics3D g3d, Atom[] alphas, Point3f[] midPoints,
                   short[] mads, short[] colixes) {
    for (int i = alphas.length; --i >= 0; ) {
      int mad = mads[i];
      if (mad == 0)
        continue;
      calcScreenCoordinates(midPoints, i);
      int diameter1 = viewer.scaleToScreen(s1.z, mad);
      int diameter2 = viewer.scaleToScreen(s2.z, mad);
      short colix = colixes[i];
      if (colix == 0)
        colix = alphas[i].colixAtom;
      g3d.fillHermite(colix, diameter1, diameter2,
                      s0.x, s0.y, s0.z, s1.x, s1.y, s1.z,
                      s2.x, s2.y, s2.z, s3.x, s3.y, s3.z);
    }
  }

  void calcScreenCoordinates(Point3f[] midPoints, int i1) {
    int i0 = i1 - 1;
    int i2 = i1 + 1;
    int i3 = i1 + 2;
    viewer.transformPoint(midPoints[i1], s1);
    if (i0 < 0)
      s0.set(s1);
    else
      viewer.transformPoint(midPoints[i0], s0);
    viewer.transformPoint(midPoints[i2], s2);
    if (i3 == midPoints.length)
      s3.set(s2);
    else
      viewer.transformPoint(midPoints[i3], s3);
  }
}

