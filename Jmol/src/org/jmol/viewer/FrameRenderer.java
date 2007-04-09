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
//import java.awt.Rectangle;
import org.jmol.util.Logger;

class FrameRenderer {

  boolean logTime;
  long timeBegin;
    
  Viewer viewer;

  ShapeRenderer[] renderers = new ShapeRenderer[JmolConstants.SHAPE_MAX];

  FrameRenderer(Viewer viewer) {
    this.viewer = viewer;
  }

  void render(Graphics3D g3d, Frame frame) {  //, Rectangle rectClip

    if (frame == null || !viewer.mustRenderFlag())
      return;
    //System.out.println("Frame: rendering viewer "+ viewer + " thread " + Thread.currentThread());    
    logTime = viewer.getTestFlag1();

    viewer.finalizeTransformParameters();

    if (logTime)
      timeBegin = System.currentTimeMillis();

    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];

      if (shape == null)
        continue;
      getRenderer(i, g3d).render(g3d, frame, shape); //, rectClip
    }
    if (logTime)
      Logger.info("render time: " + (System.currentTimeMillis() - timeBegin)
          + " ms");
  }

  ShapeRenderer getRenderer(int shapeID, Graphics3D g3d) {
    if (renderers[shapeID] == null)
      renderers[shapeID] = allocateRenderer(shapeID, g3d);
    return renderers[shapeID];
  }
  
  void clear() {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i)
      renderers[i] = null;
  }

  ShapeRenderer allocateRenderer(int shapeID, Graphics3D g3d) {
    String classBase =
      JmolConstants.shapeClassBases[shapeID] + "Renderer";
    String className = "org.jmol.viewer." + classBase;
    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer)shapeClass.newInstance();
      renderer.setViewerG3dShapeID(viewer, g3d, shapeID);
      return renderer;
    } catch (Exception e) {
      Logger.error("Could not instantiate renderer:" + classBase, e);
    }
    return null;
  }
}
