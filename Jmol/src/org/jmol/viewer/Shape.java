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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;
import org.jmol.g3d.*;

import javax.vecmath.Point3f;
import java.util.BitSet;

abstract class Shape {

  Viewer viewer;
  Frame frame;
  Graphics3D g3d;

  final void setViewerG3dFrame(Viewer viewer, Graphics3D g3d, Frame frame) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.frame = frame;
    initShape();
  }

  void initShape() {
  }

  void setSize(int size, BitSet bsSelected) {
  }
  
  void setProperty(String propertyName, Object value,
                          BitSet bsSelected) {
    System.out.println("unassigned property:" + propertyName + ":" + value);
  }

  Object getProperty(String property, int index) {
    return null;
  }

  boolean wasClicked(int x, int y) {
    return false;
  }

  void findNearestAtomIndex(int xMouse, int yMouse, Closest closest) {
  }

  void checkBoundsMinMax(Point3f pointMin, Point3f pointMax) {
  }

}
