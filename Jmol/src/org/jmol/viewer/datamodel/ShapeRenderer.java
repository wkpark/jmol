/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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

package org.jmol.viewer.datamodel;

import org.jmol.g3d.Graphics3D;
import org.jmol.viewer.*;
import java.awt.Rectangle;

abstract class ShapeRenderer {

  JmolViewer viewer;
  FrameRenderer frameRenderer;

  final void setViewerFrameRenderer(JmolViewer viewer,
                                    FrameRenderer frameRenderer,
                                    Graphics3D g3d) {
    this.viewer = viewer;
    this.frameRenderer = frameRenderer;
    this.g3d = g3d;
    initRenderer();
  }

  void initRenderer() {
  }

  Graphics3D g3d;
  Rectangle rectClip;
  Frame frame;
  int displayModelIndex;
  Shape shape;

  void render(Graphics3D g3d, Rectangle rectClip,
              Frame frame, int displayModelIndex, Shape shape) {
    this.g3d = g3d;
    this.rectClip = rectClip;
    this.frame = frame;
    this.displayModelIndex = displayModelIndex;
    this.shape = shape;
    render();
  }

  abstract void render();
}

