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
import org.jmol.g3d.Graphics3D;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;


class PruebaRenderer extends ShapeRenderer {

  final Point3f[] points = {
    new Point3f(0, 0, 0),
    new Point3f(10, 0, 0),
    new Point3f(10, 10, 0),
    new Point3f(0, 10, 0),
  };

  final Point3i[] screens = new Point3i[points.length];
  {
    for (int i = screens.length; --i >= 0; )
      screens[i] = new Point3i();
  }

  void render() {
    Prueba prueba = (Prueba)shape;

    viewer.transformPoints(points, screens);
    g3d.fillQuadrilateral(prueba.colix, screens[0],
                          screens[1], screens[2], screens[3]);
  }
}
