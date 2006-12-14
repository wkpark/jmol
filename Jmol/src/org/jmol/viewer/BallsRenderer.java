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

    if (frame.atomCount > 0 && viewer.getNavigating()) {
      //testing here
      Point3f T = new Point3f(viewer.getNavigationOffset());
      g3d.fillSphereCentered(Graphics3D.GOLD, 6, (int)T.x, (int)T.y,(int)T.z);
      //System.out.println("ballsrend navCenter="
        //  + viewer.getNavigationOffset() + T);
      Point3i S = new Point3i();
      viewer.transformPoint(viewer.getRotationCenter(), S);
      g3d.fillSphereCentered(Graphics3D.RED, 8, S.x, S.y, S.z);
      //System.out.println("ballsrend rotCenter="+S);
      //Point3f P = new Point3f();
      //T.set(S.x, S.y, S.z);
      //int x = viewer.scaleToScreen(S.z, (int)(viewer.getRotationRadius()*1000/4));
      //T.x += x;
      //float calc = (2*viewer.getScreenWidth()-x)/(4f*x);
      //if (calc < 0)
        //calc = 1-(float)Math.exp(-calc * 3);
      //P.set(S.x, S.y, S.z);
      //g3d.fillCylinderBits(Graphics3D.WHITE,Graphics3D.ENDCAPS_FLAT,5,T,P);
      //System.out.println("ballsrend rotCenter="+viewer.getRotationCenter() + S+" rad=" + viewer.getRotationRadius() + "\n scalePixPerAng=" + viewer.getScalePixelsPerAngstrom() + " w="+viewer.getScreenWidth() + " x=" + x + "  calc="+calc);
    }
  }

  void renderBall(Atom atom) {
    g3d.fillSphereCentered(atom.colixAtom, atom.screenDiameter,
                           atom.screenX, atom.screenY, atom.screenZ);
  }
}
