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

import java.awt.Rectangle;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

class LineRenderer extends ShapeRenderer {

  LineRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    setViewerFrameRenderer(viewer, frameRenderer);
  }

  final Point3i screenOrigin = new Point3i();
  final Point3i screenEnd = new Point3i();
  final Point3i screenArrowHead = new Point3i();

  /*
  final static int shaftDivisor = 6;
  final static int finDivisor = 8;
  final static int[] ax = new int[4];
  final static int[] ay = new int[4];
  final static int[] az = new int[4];
  */

  void render() {
    render(frame.lineCount, frame.lines);
  }

  void render(int lineCount, Line[] lines) {
    short colix = viewer.getColixVector();
    g3d.setColix(colix);
    for (int i = lineCount; --i >= 0; ) {
      Line line = lines[i];
      viewer.transformPoint(line.pointOrigin, screenOrigin);
      viewer.transformPoint(line.pointEnd, screenEnd);
      g3d.drawLine(screenOrigin, screenEnd);
      if (line.pointArrowHead != null) {
        viewer.transformPoint(line.pointArrowHead, screenArrowHead);
        int headWidthPixels =
          (int)(viewer.scaleToScreen(screenArrowHead.z,
                                     line.headWidthAngstroms) + 0.5f);
        g3d.fillCone(colix, Graphics3D.ENDCAPS_NONE, headWidthPixels,
                     screenArrowHead, screenEnd);
        return;
        /*
        int x = screenOrigin.x, y = screenOrigin.y, z = screenOrigin.z;
        int xEnd = screenEnd.x, yEnd = screenEnd.y, zEnd = screenEnd.z;
        int dx = xEnd - x, xHead = xEnd - (dx / shaftDivisor);
        int dy = yEnd - y, yHead = yEnd - (dy / shaftDivisor);
        int mag2d = (int)(Math.sqrt(dx*dx + dy*dy) + 0.5);
        int dz = zEnd - z, zHead = zEnd - (dz / shaftDivisor);
        int headWidthPixels =
          (int)(viewer.scaleToScreen(zHead, line.headWidthAngstroms) + 0.5f);
        
        ax[0] = xEnd; ax[2] = xEnd - dx/finDivisor;
        ay[0] = yEnd; ay[2] = yEnd - dy/finDivisor;
        az[0] = zEnd; az[2] = zEnd - dz/finDivisor;
        int dxHead, dyHead;
        if (mag2d == 0) {
          dxHead = 0;
          dyHead = headWidthPixels;
        } else {
          dxHead = headWidthPixels * -dy / mag2d;
          dyHead = headWidthPixels * dx / mag2d;
        }
        
        ax[1] = xHead - dxHead/2; ax[3] = ax[1] + dxHead;
        ay[1] = yHead - dyHead/2; ay[3] = ay[1] + dyHead;
        az[1] = zHead;            az[3] = zHead;
        g3d.drawfillPolygon4(ax, ay, az);
        */
      }
    }
  }
}

