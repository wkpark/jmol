/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

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

package org.jmol.viewer;

import org.jmol.g3d.*;
import javax.vecmath.*;
class BallsRenderer extends ShapeRenderer {

  int minX, maxX, minY, maxY, minZ, maxZ;
  boolean isNav;
  void render() {
    minX = rectClip.x;
    maxX = minX + rectClip.width;
    minY = rectClip.y;
    maxY = minY + rectClip.height;
    boolean slabbing = viewer.getSlabEnabled();
    isNav = viewer.getNavigationMode();
    if (slabbing) {
      minZ = g3d.getSlab();
      maxZ = g3d.getDepth();
    }
    Atom[] atoms = frame.atoms;
    for (int i = frame.groupCount; --i >= 0;)
      frame.groups[i].minZ = Integer.MAX_VALUE;
    for (int i = frame.atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) == 0)
        continue;
      atom.transform(viewer);
      if (slabbing) {
        if (g3d.isClippedZ(atom.screenZ)) {
          atom.clickabilityFlags = 0;
          //note that in the case of navigation, 
          //maxZ is set to Integer.MAX_VALUE.
          
          //if (isNav)
            //continue;
          int r = atom.screenDiameter / 2;
          if (atom.screenZ < minZ - r || atom.screenZ > maxZ + r)
            continue;
          if (!g3d.isInDisplayRange(atom.screenX, atom.screenY))
            continue;
        }
      }
      // note: above transform is required for all other renderings
      if (atom.group != null) {
        int z = atom.screenZ - atom.screenDiameter / 2 - 2;
        if (z < atom.group.minZ)
          atom.group.minZ = Math.max(1, z);
      }
      if ((atom.shapeVisibilityFlags & myVisibilityFlag) != 0)
        renderBall(atom);
    }

    if (frame.atomCount > 0 && viewer.getShowNavigationPoint()) {
      //testing here
      Point3f T = new Point3f(viewer.getNavigationOffset());
      int x = Math.max(Math.min(viewer.getScreenWidth(),(int) T.x),0);
      int y = Math.max(Math.min(viewer.getScreenHeight(),(int) T.y),0);
      int z = (int) T.z + 1;
      short colix = (viewer.getNavigationCentered() ? Graphics3D.GOLD : Graphics3D.RED);
      g3d.drawRect(colix, x - 10, y, z, 0, 20, 1);
      g3d.drawRect(colix, x, y - 10, z, 0, 1, 20);
      g3d.drawRect(colix, x - 4, y -4, z, 0, 10, 10);
    }
  }

  void renderBall(Atom atom) {
    short colix = atom.colixAtom;
    g3d.fillSphereCentered(colix, atom.screenDiameter,
                           atom.screenX, atom.screenY, atom.screenZ);
  }
}
