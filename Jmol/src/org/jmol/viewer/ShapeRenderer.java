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

import org.jmol.g3d.Graphics3D;
//import java.awt.Rectangle;

abstract class ShapeRenderer {

  Viewer viewer;
  int myVisibilityFlag;
  int shapeID;

  final void setViewerG3dShapeID(Viewer viewer, Graphics3D g3d, int shapeID) {
    this.viewer = viewer;
    this.g3d = g3d;
    this.shapeID = shapeID;
    myVisibilityFlag = JmolConstants.getShapeVisibilityFlag(shapeID);
    initRenderer();
  }

  void initRenderer() {
  }

  Graphics3D g3d;
  //Rectangle rectClip; //not implemented
  Frame frame;
  Shape shape;

  void render(Graphics3D g3d, Frame frame, Shape shape) { //, Rectangle rectClip
    this.g3d = g3d;
    //this.rectClip = rectClip; //not implemented -- could be a place for optimization
    this.frame = frame;
    this.shape = shape;
    render();
  }

  abstract void render();
}

