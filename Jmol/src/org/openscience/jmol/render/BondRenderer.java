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

package org.openscience.jmol.render;

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.g3d.Graphics3D;
import org.openscience.jmol.g3d.Colix;

import java.awt.Rectangle;

public class BondRenderer {

  DisplayControl control;
  public BondRenderer(DisplayControl control) {
    this.control = control;
  }

  Graphics3D g3d;
  Rectangle clip;

  public void setGraphicsContext(Graphics3D g3d, Rectangle clip) {
    this.g3d = g3d;
    this.clip = clip;

    fastRendering = control.getFastRendering();
    colixSelection = control.getColixSelection();
    showMultipleBonds = control.getShowMultipleBonds();
    modeMultipleBond = control.getModeMultipleBond();
  }

  boolean fastRendering;
  short colixSelection;
  boolean showMultipleBonds;
  byte modeMultipleBond;

  int x1, y1, z1;
  int x2, y2, z2;
  int dx, dy;
  int mag2d, mag2d2;
  short colix1, colix2;
  int width;
  byte styleAtom1, styleAtom2;
  int bondOrder;
  byte styleBond;
  short marBond;
  
  private void renderHalo() {
    int x = (x1 + x2) / 2, y = (y1 + y2) / 2, z = (z1 + z2) / 2;
    int halowidth = width / 4;
    if (halowidth < 4) halowidth = 4;
    if (halowidth > 10) halowidth = 10;
    int halodiameter = width + 2 * halowidth;
    g3d.fillCircleCentered(colixSelection, x, y, z+1, halodiameter);
  }

  public void render(AtomShape atomShape1, AtomShape atomShape2, int order,
                     byte style, short mar, short colix, int width) {
    if (style == DisplayControl.NONE)
      return;
    if (atomShape1.z > atomShape2.z) {
      AtomShape t = atomShape1;
      atomShape1 = atomShape2;
      atomShape2 = t;
    }
    styleAtom1 = atomShape1.styleAtom;
    styleAtom2 = atomShape2.styleAtom;
    styleBond = style;
    marBond = mar;
    x1 = atomShape1.x; y1 = atomShape1.y; z1 = atomShape1.z;
    x2 = atomShape2.x; y2 = atomShape2.y; z2 = atomShape2.z;
    this.width = width;
    colix1 = colix2 = colix;
    if (colix == 0) {
      colix1 = atomShape1.colixAtom;
      colix2 = atomShape2.colixAtom;
    }

    bondOrder = getRenderBondOrder(order);

    /*
    if (control.hasSelectionHalo(atomShape1.atom, index1))
      renderHalo();
    */
    if (styleBond != DisplayControl.NONE)
      renderBond();
  }

  int getRenderBondOrder(int order) {
    if (order == 1 || order == BondShape.BACKBONE ||
        !showMultipleBonds ||
        modeMultipleBond == DisplayControl.MB_NEVER ||
        (modeMultipleBond == DisplayControl.MB_SMALL &&
         marBond > DisplayControl.marMultipleBondSmallMaximum) ||
        order == -1)
      return 1;
    return order;
  }

  private void renderBond() {
    boolean lineBond = (styleBond == control.WIREFRAME ||
                        fastRendering ||
                        width <= 1);
    dx = x2 - x1;
    dy = y2 - y1;
    if (dx == 0 && dy == 0) {
      if (! lineBond) {
        int space = width / 8 + 3;
        int step = width + space;
        int y = y1 - ((bondOrder == 1)
                      ? 0
                      : (bondOrder == 2) ? step / 2 : step);
        do {
          g3d.fillCylinder(colix1, colix1, width, x1, y, z1, x1, y, z1);
          y += step;
        } while (--bondOrder > 0);
      }
      return;
    }
    if (bondOrder == 1) {
      if (lineBond)
        g3d.drawLine(colix1, colix2, x1, y1, z1, x2, y2, z2);
      else
        g3d.fillCylinder(colix1, colix2, width, x1, y1, z1, x2, y2, z2);
      return;
    }
    int dx2 = dx * dx;
    int dy2 = dy * dy;
    int mag2d2 = dx2 + dy2;
    mag2d = (int)(Math.sqrt(mag2d2) + 0.5);
    resetAxisCoordinates(lineBond);
    while (true) {
      if (lineBond)
        lineBond();
      else
        cylinderBond();
      if (--bondOrder == 0)
        break;
      stepAxisCoordinates();
    }
  }

  void lineBond() {
    g3d.drawLine(colix1, colix2, xAxis1, yAxis1, z1, xAxis2, yAxis2, z2);
  }

  void cylinderBond() {
    g3d.fillCylinder(colix1, colix2, width,
                      xAxis1, yAxis1, z1, xAxis2, yAxis2, z2);
  }


  int xAxis1, yAxis1, zAxis1, xAxis2, yAxis2, zAxis2, dxStep, dyStep;

  void resetAxisCoordinates(boolean lineBond) {
    int space = width / 8 + 3;
    int step = width + space;
    dxStep = step * dy / mag2d; dyStep = step * -dx / mag2d;

    xAxis1 = x1; yAxis1 = y1; zAxis1 = z1;
    xAxis2 = x2; yAxis2 = y2; zAxis2 = z2;

    if (bondOrder == 2) {
      xAxis1 -= dxStep / 2; yAxis1 -= dyStep / 2;
      xAxis2 -= dxStep / 2; yAxis2 -= dyStep / 2;
    } else if (bondOrder == 3) {
      xAxis1 -= dxStep; yAxis1 -= dyStep;
      xAxis2 -= dxStep; yAxis2 -= dyStep;
    }
  }


  void stepAxisCoordinates() {
    xAxis1 += dxStep; yAxis1 += dyStep;
    xAxis2 += dxStep; yAxis2 += dyStep;
  }
}

