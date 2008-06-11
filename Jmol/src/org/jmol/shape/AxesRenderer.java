/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2006  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import java.awt.FontMetrics;

import org.jmol.g3d.Font3D;
import org.jmol.g3d.Graphics3D;
import org.jmol.modelset.CellInfo;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.StateManager;

import javax.vecmath.Point3i;

public class AxesRenderer extends FontLineShapeRenderer {

  String[] axisLabels = { "+X", "+Y", "+Z",
                          null, null, null, "a", "b", "c" , "X", "Y", "Z"};

  final Point3i[] axisScreens = new Point3i[6];
  {
    for (int i = 6; --i >= 0; )
      axisScreens[i] = new Point3i();
  }
  final Point3i originScreen = new Point3i();
  
  short[] colixes = new short[3];

  protected void render() {
    Axes axes = (Axes) shape;
    int mad = viewer.getObjectMad(StateManager.OBJ_AXIS1);
    if (mad == 0 || !g3d.checkTranslucent(false))
      return;
    imageFontScaling = viewer.getImageFontScaling();
    if (viewer.areAxesTainted())
      axes.initShape();
    int nPoints = 6;
    int labelPtr = 0;
    CellInfo[] cellInfos = modelSet.getCellInfos();
    boolean isXY = (axes.axisXY.z != 0);
    if (isXY) {
      nPoints = 3;
      labelPtr = 9;
    } else if (viewer.getAxesMode() == JmolConstants.AXES_MODE_UNITCELL
        && cellInfos != null) {
      int modelIndex = viewer.getDisplayModelIndex();
      if (modelIndex < 0 || cellInfos[modelIndex].getUnitCell() == null)
        return;
      nPoints = 3;
      labelPtr = 6;
    }
    boolean isDataFrame = viewer.isJmolDataFrame();
    viewer.transformPointNoClip(axes.getOriginPoint(isDataFrame), originScreen);
    for (int i = nPoints; --i >= 0;)
      viewer.transformPointNoClip(axes.getAxisPoint(i, isDataFrame), axisScreens[i]);
    int aFactor = (g3d.isAntialiased() ? 2 : 1);
    int widthPixels = (mad < 20 ? mad * aFactor : viewer.scaleToScreen(originScreen.z, mad));
    int minZ = originScreen.z;
    int slab = g3d.getSlab();
    if (isXY) {
      g3d.setSlab(0);
      float d = 50f / viewer.getZoomPercentFloat();
      int w = viewer.getScreenWidth();
      int h = viewer.getScreenHeight();
      float factor = (axes.axisXY.z < 0 ? h / 250 : 1);
      int x0 = (int) (axes.axisXY.x * (axes.axisXY.z < 0 ? w / 100f : 1f));
      int y0 = (int) (axes.axisXY.y * (axes.axisXY.z < 0 ? h / 100f : 1f));
      for (int i = 0; i < 3; i++) {
        axisScreens[i].x = (x0 * aFactor + (int) ((axisScreens[i].x - originScreen.x) * factor * d));
        axisScreens[i].y = (h * aFactor - (y0 * aFactor - (int) ((axisScreens[i].y - originScreen.y) * factor * d)));
        minZ = Math.min(minZ, axisScreens[i].z);
      }
      originScreen.x = (int) x0 * aFactor;
      originScreen.y = (h - (int) y0) * aFactor;
      widthPixels = (int) (widthPixels * d);
      if (widthPixels > 5)
        widthPixels = 5;
      for (int i = 0; i < 3; i++)
        axisScreens[i].z -= (minZ - 1);
      originScreen.z  -= (minZ - 1);
    }
    float xCenter = originScreen.x;
    float yCenter = originScreen.y;
    colixes[0] = viewer.getObjectColix(StateManager.OBJ_AXIS1);
    colixes[1] = viewer.getObjectColix(StateManager.OBJ_AXIS2);
    colixes[2] = viewer.getObjectColix(StateManager.OBJ_AXIS3);
    Font3D font = g3d.getFont3DScaled(axes.font3d, imageFontScaling);
    for (int i = nPoints; --i >= 0;) {
      colix = colixes[i % 3];
      g3d.setColix(colix);
      String label = axisLabels[i + labelPtr];
      if (label != null)
        renderLabel(label, font, axisScreens[i].x,
            axisScreens[i].y, axisScreens[i].z, xCenter, yCenter);
      if (mad < 0)
        g3d.drawDottedLine(originScreen, axisScreens[i]);
      else
        g3d.fillCylinder(Graphics3D.ENDCAPS_FLAT, widthPixels,
            originScreen, axisScreens[i]);
    }
    if (nPoints == 3 && !isXY) { //a b c
      colix = viewer.getColixBackgroundContrast();
      g3d.setColix(colix);
      renderLabel("0", font, originScreen.x, originScreen.y, originScreen.z, xCenter, yCenter);
    }
    if (isXY)
      g3d.setSlab(slab);
  }
  
  private void renderLabel(String str, Font3D font3d, int x, int y, int z, float xCenter, float yCenter) {
    FontMetrics fontMetrics = font3d.fontMetrics;
    int strAscent = fontMetrics.getAscent();
    int strWidth = fontMetrics.stringWidth(str);
    float dx = x - xCenter;
    float dy = y - yCenter;
    if ((dx != 0 || dy != 0)) {
      float dist = (float) Math.sqrt(dx * dx + dy * dy);
      dx = (int) (strWidth * 0.75f * dx / dist);
      dy = (int) (strAscent * 0.75f * dy / dist);
      x += dx;
      y += dy;
    }

    int xStrBaseline = x - strWidth / 2;
    int yStrBaseline = y + strAscent / 2;
    g3d.drawString(str, font3d, xStrBaseline, yStrBaseline, z, z);
  }
}
