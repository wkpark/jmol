/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-08 22:18:02 -0500 (Mon, 08 Oct 2007) $
 * $Revision: 8391 $

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

package org.jmol.shapespecial;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;
import javax.vecmath.Vector3f;

import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.Atom;
import org.jmol.shape.Shape;
import org.jmol.shape.ShapeRenderer;

public class EllipsoidsRenderer extends ShapeRenderer {

  protected void render() {
    Ellipsoids ellipsoids = (Ellipsoids) shape;
    if (ellipsoids.mads == null)
      return;
    Atom[] atoms = modelSet.atoms;
    for (int i = modelSet.getAtomCount(); --i >= 0;) {
      Atom atom = atoms[i];
      if (!atom.isShapeVisible(myVisibilityFlag) || modelSet.isAtomHidden(i))
        continue;
      Object[] ellipsoid = atom.getEllipsoid();
      if (ellipsoid == null)
        continue;
      colix = Shape.getColix(ellipsoids.colixes, i, atom);
      if (!g3d.setColix(colix))
        continue;
      render1(atom, ellipsoids.mads[i], ellipsoid);
    }
  }

  private final Point3i[] screens = new Point3i[6];
  private final Point3f[] points = new Point3f[6];
  {
    for (int i = 0; i < 6; i++) {
      screens[i] = new Point3i();
      points[i] = new Point3f();
    }
  }

  void render1(Atom atom, short mad, Object[] ellipsoid) {
    //for now a simple object
    Vector3f[] axes = (Vector3f[]) ellipsoid[0];
    float[] lengths = (float[]) ellipsoid[1];
    float f = mad / 100.0f * 4f;
    points[0].scaleAdd(-lengths[0] * f, axes[0], atom);
    points[1].scaleAdd(lengths[0]* f, axes[0], atom);
    points[2].scaleAdd(-lengths[1]* f, axes[1], atom);
    points[3].scaleAdd(lengths[1]* f, axes[1], atom);
    points[4].scaleAdd(-lengths[2]* f, axes[2], atom);
    points[5].scaleAdd(lengths[2]* f, axes[2], atom);
    for (int i = 0; i < 6; i++)
      viewer.transformPoint(points[i], screens[i]);
    int d = viewer.scaleToScreen(atom.screenZ, 3);
    d -= (d & 1) ^ 1; // round down to odd value

    for (int i = 0; i < 6; i += 2)
      g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, d, screens[i], screens[i + 1]);

  }

}
