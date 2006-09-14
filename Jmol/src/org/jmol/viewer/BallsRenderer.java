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

class BallsRenderer extends ShapeRenderer {

  int minX, maxX, minY, maxY, minZ, maxZ;
  int ballVisibilityFlag;

  void initRenderer() {
    ballVisibilityFlag = Viewer.getShapeVisibilityFlag(JmolConstants.SHAPE_BALLS);
  }
  boolean labelsGroup;
  
  void render() {
    minX = rectClip.x;
    maxX = minX + rectClip.width;
    minY = rectClip.y;
    maxY = minY + rectClip.height;
    boolean slabbing = viewer.getSlabEnabled();
    if (slabbing) {
      minZ = g3d.getSlab();
      maxZ = g3d.getDepth();
    }
    labelsGroup = viewer.getLabelsGroupFlag() && !viewer.getLabelsFrontFlag();
    Atom[] atoms = frame.atoms;
    if (labelsGroup)
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
          int r = atom.screenDiameter / 2;
          if (atom.screenZ < minZ - r || atom.screenZ > maxZ + r)
            continue;
        }
      }
      // note: above transform is required for all other renderings
      if (labelsGroup) {
        if (atom.group != null) {
          int z = atom.getScreenZ() - atom.getScreenD() / 2 - 2;
          if (z < atom.group.minZ)
            atom.group.minZ = Math.max(1,z);
        }
      }
      if ((atom.shapeVisibilityFlags & ballVisibilityFlag) != 0)
        renderBall(atom);
    }
  }

  void renderBall(Atom atom) {
    g3d.fillSphereCentered(atom.colixAtom, atom.screenDiameter,
                           atom.screenX, atom.screenY, atom.screenZ);
  }

}
