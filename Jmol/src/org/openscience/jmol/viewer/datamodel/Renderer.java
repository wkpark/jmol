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

abstract class Renderer {

  JmolViewer viewer;
  FrameRenderer frameRenderer;

  final void setViewerFrameRenderer(JmolViewer viewer,
                                    FrameRenderer frameRenderer) {
    this.viewer = viewer;
    this.frameRenderer = frameRenderer;
    initRenderer();
  }

  void initRenderer() {
  }

  Graphics3D g3d;
  Rectangle rectClip;
  Frame frame;
  Graphic graphic;

  void render(Graphics3D g3d, Rectangle rectClip,
              Frame frame, Graphic graphic) {
    this.g3d = g3d;
    this.rectClip = rectClip;
    this.frame = frame;
    this.graphic = graphic;
    render();
  }

  abstract void render();
}

