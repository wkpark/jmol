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
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;

import java.awt.Rectangle;

class BondRenderer extends Renderer {

  BondRenderer(JmolViewer viewer, FrameRenderer frameRenderer) {
    super(viewer, frameRenderer);
  }

  boolean wireframeRotating;
  short colixSelection;
  boolean showMultipleBonds;
  byte modeMultipleBond;
  boolean showHydrogens;
  byte endcaps;

  int xA, yA, zA;
  int xB, yB, zB;
  int dx, dy;
  int mag2d, mag2d2;
  short colixA, colixB;
  int width;
  int bondOrder;
  byte styleBond;
  short marBond;
  
  private void renderHalo() {
    int x = (xA + xB) / 2, y = (yA + yB) / 2, z = (zA + zB) / 2;
    int halowidth = width / 4;
    if (halowidth < 4) halowidth = 4;
    if (halowidth > 10) halowidth = 10;
    int halodiameter = width + 2 * halowidth;
    g3d.fillScreenedCircleCentered(colixSelection, halodiameter, x, y, z+1);
  }

  void render() {

    endcaps = viewer.getTestFlag1()
      ? Graphics3D.ENDCAPS_NONE : Graphics3D.ENDCAPS_SPHERICAL;

    wireframeRotating = viewer.getWireframeRotating();
    colixSelection = viewer.getColixSelection();
    showMultipleBonds = viewer.getShowMultipleBonds();
    modeMultipleBond = viewer.getModeMultipleBond();
    showHydrogens = viewer.getShowHydrogens();

    Bond[] bonds = frame.bonds;
    for (int i = frame.bondCount; --i >= 0; )
      render(bonds[i]);
  }

  void render(Bond bond) {
    styleBond = bond.style;
    if (styleBond == JmolConstants.STYLE_NONE)
      return;
    Atom atomA = bond.atom1;
    Atom atomB = bond.atom2;
    if (!showHydrogens && (atomA.atomicNumber == 1 ||
                           atomB.atomicNumber == 1))
      return;
    xA = atomA.x; yA = atomA.y; zA = atomA.z;
    xB = atomB.x; yB = atomB.y; zB = atomB.z;
    dx = xB - xA;
    dy = yB - yA;
    width = viewer.scaleToScreen((zA + zB)/2, bond.mar * 2);
    marBond = bond.mar;
    colixA = colixB = bond.colix;
    if (colixA == 0) {
      colixA = atomA.colixAtom;
      colixB = atomB.colixAtom;
    }
    bondOrder = getRenderBondOrder(bond.order);
    switch(bondOrder) {
    case 1:
    case 2:
    case 3:
      renderCylinder();
      break;
    case JmolConstants.BOND_STEREO_NEAR:
    case JmolConstants.BOND_STEREO_FAR:
      renderTriangle(bond);
      break;
    case JmolConstants.BOND_HYDROGEN:
      renderDotted();
    }
  }

  int getRenderBondOrder(int order) {
    if ((order & JmolConstants.BOND_SULFUR) != 0)
      order &= ~JmolConstants.BOND_SULFUR;
    if ((order & JmolConstants.BOND_COVALENT) != 0) {
      if (order == 1 ||
          !showMultipleBonds ||
          modeMultipleBond == JmolConstants.MULTIBOND_NEVER ||
          (modeMultipleBond == JmolConstants.MULTIBOND_SMALL &&
           marBond > JmolConstants.marMultipleBondSmallMaximum)) {
          // there used to be a test here for order == -1 ? why ?
        return 1;
      }
    }
    return order;
  }

  private void renderCylinder() {
    boolean lineBond = (styleBond == JmolConstants.STYLE_WIREFRAME ||
                        wireframeRotating ||
                        width <= 1);
    if (dx == 0 && dy == 0) {
      // end-on view
      if (! lineBond) {
        int space = width / 8 + 3;
        int step = width + space;
        int y = yA - ((bondOrder == 1)
                      ? 0
                      : (bondOrder == 2) ? step / 2 : step);
        do {
          g3d.fillCylinder(colixA, colixA, endcaps,
                           width, xA, y, zA, xA, y, zA);
          y += step;
        } while (--bondOrder > 0);
      }
      return;
    }
    if (bondOrder == 1) {
      if (lineBond)
        g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
      else
        g3d.fillCylinder(colixA, colixB, endcaps,
                         width, xA, yA, zA, xB, yB, zB);
      return;
    }
    int dxB = dx * dx;
    int dyB = dy * dy;
    int mag2d2 = dxB + dyB;
    mag2d = (int)(Math.sqrt(mag2d2) + 0.5);
    resetAxisCoordinates(lineBond);
    while (true) {
      if (lineBond)
        g3d.drawLine(colixA, colixB, xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
      else
        g3d.fillCylinder(colixA, colixB, endcaps, width,
                         xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
      if (--bondOrder == 0)
        break;
      stepAxisCoordinates();
    }
  }

  int xAxis1, yAxis1, zAxis1, xAxis2, yAxis2, zAxis2, dxStep, dyStep;

  void resetAxisCoordinates(boolean lineBond) {
    int space = width / 8 + 3;
    int step = width + space;
    dxStep = step * dy / mag2d; dyStep = step * -dx / mag2d;

    xAxis1 = xA; yAxis1 = yA; zAxis1 = zA;
    xAxis2 = xB; yAxis2 = yB; zAxis2 = zB;

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

  Rectangle rectTemp = new Rectangle();
  private boolean isClipVisible(int xA, int yA, int xB, int yB) {
    // this is not actually correct, but quick & dirty
    int xMin, width, yMin, height;
    if (xA < xB) {
      xMin = xA;
      width = xB - xA;
    } else if (xB < xA) {
      xMin = xB;
      width = xA - xB;
    } else {
      xMin = xA;
      width = 1;
    }
    if (yA < yB) {
      yMin = yA;
      height = yB - yA;
    } else if (yB < yA) {
      yMin = yB;
      height = yA - yB;
    } else {
      yMin = yA;
      height = 1;
    }
    // there are some problems with this quick&dirty implementation
    // so I am going to throw in some slop
    xMin -= 5;
    yMin -= 5;
    width += 10;
    height += 10;
    rectTemp.x = xMin;
    rectTemp.y = yMin;
    rectTemp.width = width;
    rectTemp.height = height;
    boolean visible = rectClip.intersects(rectTemp);
    /*
    System.out.println("bond " + x + "," + y + "->" + xB + "," + yB +
                       " & " + rectClip.x + "," + rectClip.y +
                       " W " + rectClip.width + " H " + rectClip.height +
                       "->" + visible);
    visible = true;
    */
    return visible;
  }

  private void renderDotted() {
    if (dx == 0 && dy == 0)
      return;
    g3d.drawDottedLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
  }

  private static float wideWidthAngstroms = 0.4f;

  private void renderTriangle(Bond bond) {
    // for now, always solid
    int mag2d = (int)Math.sqrt(dx*dx + dy*dy);
    int wideWidthPixels = (int)viewer.scaleToScreen(zB, wideWidthAngstroms);
    int dxWide, dyWide;
    if (mag2d == 0) {
      dxWide = 0;
      dyWide = wideWidthPixels;
    } else {
      dxWide = wideWidthPixels * -dy / mag2d;
      dyWide = wideWidthPixels * dx / mag2d;
    }
    /*
    System.out.println("rendering a triangle of color:" + colixA);
    System.out.println("" + xA + "," + yA + "," + zA + " -> " +
                       xB + "," + yB + "," + zB + " -> ");
    System.out.println("wideWidthPixesl=" + wideWidthPixels +
                       " dxWide=" + dxWide + " dyWide=" + dyWide);
    */
    int xWideUp = xB + dxWide/2;
    int xWideDn = xWideUp - dxWide;
    int yWideUp = yB + dyWide/2;
    int yWideDn = yWideUp - dyWide;
    /*
    System.out.println("up=" + xWideUp + "," + yWideUp +
                       " dn=" + xWideDn + "," + yWideDn);
    */
    if (colixA == colixB) {
      g3d.drawfillTriangle(colixA, xA, yA, zA,
                           xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
    } else {
      int xMidUp = (xA + xWideUp) / 2;
      int yMidUp = (yA + yWideUp) / 2;
      int zMid = (zA + zB) / 2;
      int xMidDn = (xA + xWideDn) / 2;
      int yMidDn = (yA + yWideDn) / 2;
      g3d.drawfillTriangle(colixA, xA, yA, zA,
                           xMidUp, yMidUp, zMid, xMidDn, yMidDn, zMid);
      g3d.drawfillTriangle(colixB, xMidUp, yMidUp, zMid,
                           xMidDn, yMidDn, zMid, xWideDn, yWideDn, zB);
      g3d.drawfillTriangle(colixB, xMidUp, yMidUp, zMid,
                           xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
    }
  }
}


