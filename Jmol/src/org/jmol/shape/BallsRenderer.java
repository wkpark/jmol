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

package org.jmol.shape;

import org.jmol.g3d.*;
import org.jmol.modelset.Atom;
import org.jmol.modelset.Group;
import org.jmol.viewer.JmolConstants;

import javax.vecmath.*;
public class BallsRenderer extends ShapeRenderer {

  //int minX, minY, maxX, maxY; 
  int minZ, maxZ;
  //boolean isNav;
  protected void render() {
    //minX = rectClip.x;
    //maxX = minX + rectClip.width;
    //minY = rectClip.y;
    //maxY = minY + rectClip.height;
    boolean renderBalls = !viewer.getWireframeRotation() || !viewer.getInMotion();
    slabbing = viewer.getSlabEnabled();
    //isNav = viewer.getNavigationMode();
    if (slabbing) {
      minZ = g3d.getSlab();
      maxZ = g3d.getDepth();
    }
    Atom[] atoms = modelSet.atoms;
    int atomCount = modelSet.getAtomCount();
    for (int i = 0; i < atomCount; i++) {
      Group group = atoms[i].getGroup();
      group.setMinZ(Integer.MAX_VALUE);
      i = Math.max(group.getLastAtomIndex(), i); //just in case!
    }
    
    for (int i = atomCount; --i >= 0;) {
      Atom atom = atoms[i];
      if ((atom.getShapeVisibilityFlags() & JmolConstants.ATOM_IN_MODEL) == 0)
        continue;
      atom.transform(viewer);
      if (slabbing) {
        if (g3d.isClippedZ(atom.screenZ)) {
          atom.setClickable(0);
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
      Group group = atom.getGroup();
      if (group != null) {
        int z = atom.screenZ - atom.screenDiameter / 2 - 2;
        if (z < group.getMinZ())
          group.setMinZ(Math.max(1, z));
      }
      if (renderBalls && atom.screenDiameter > 0 
          && (atom.getShapeVisibilityFlags() & myVisibilityFlag) != 0
          && g3d.setColix(atom.getColix()))
        renderBall(atom);
    }

    if (modelSet.getAtomCount() > 0
        && viewer.getShowNavigationPoint()
        && !isGenerator
        && g3d.setColix(viewer.getNavigationCentered() ? Graphics3D.GOLD
            : Graphics3D.RED)) {
      Point3f T = new Point3f(viewer.getNavigationOffset());
      int x = Math.max(Math.min(viewer.getScreenWidth(), (int) T.x), 0);
      int y = Math.max(Math.min(viewer.getScreenHeight(), (int) T.y), 0);
      int z = (int) T.z + 1;
      //TODO: fix for antialiasDisplay
      g3d.drawRect(x - 10, y, z, 0, 20, 1);
      g3d.drawRect(x, y - 10, z, 0, 1, 20);
      g3d.drawRect(x - 4, y - 4, z, 0, 10, 10);
    }
  }

  protected void renderBall(Atom atom) {
    g3d.fillSphereCentered(atom.screenDiameter,
                           atom.screenX, atom.screenY, atom.screenZ);
  }
}
