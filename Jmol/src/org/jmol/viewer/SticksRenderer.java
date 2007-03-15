/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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

import java.awt.Rectangle;

class SticksRenderer extends ShapeRenderer {

  boolean showMultipleBonds;
  byte modeMultipleBond;
  boolean showHydrogens;
  byte endcaps;

  boolean ssbondsBackbone;
  boolean hbondsBackbone;
  boolean bondsBackbone;
  boolean hbondsSolid;
  boolean asBits;
  
  Atom atomA, atomB;
  int xA, yA, zA;
  int xB, yB, zB;
  int dx, dy;
  int mag2d, mag2d2;
  short colixA, colixB;
  int width;
  int bondOrder;
  short madBond;

  void render() {
    asBits = false;//viewer.getTestFlag2();
    
    endcaps = Graphics3D.ENDCAPS_SPHERICAL;
    showMultipleBonds = viewer.getShowMultipleBonds();
    modeMultipleBond = viewer.getModeMultipleBond();

    ssbondsBackbone = viewer.getSsbondsBackbone();
    hbondsBackbone = viewer.getHbondsBackbone();
    bondsBackbone = hbondsBackbone | ssbondsBackbone;
    hbondsSolid = viewer.getHbondsSolid();
    Bond[] bonds = frame.bonds;
    for (int i = frame.bondCount; --i >= 0; ) {
      Bond bond = bonds[i];
      if ((bond.shapeVisibilityFlags & myVisibilityFlag) != 0) 
        render(bond);
    }
  }

  void render(Bond bond) {
    madBond = bond.mad;
    int order = bond.order;
    Atom atomA = bond.atom1;
    Atom atomB = bond.atom2;
    if (!bond.atom1.isModelVisible()
        || !bond.atom2.isModelVisible()
        || !g3d.isInDisplayRange(atomA.screenX, atomA.screenY)
        || !g3d.isInDisplayRange(atomB.screenX, atomB.screenY)
        || frame.bsHidden.get(atomA.atomIndex) 
        || frame.bsHidden.get(atomB.atomIndex))
      return;

    colixA = Graphics3D.getColixInherited(bond.colix, atomA.colixAtom);    
    colixB = Graphics3D.getColixInherited(bond.colix, atomB.colixAtom);
    if (bondsBackbone) {
      if (ssbondsBackbone &&
          (order & JmolConstants.BOND_SULFUR_MASK) != 0) {
        // for ssbonds, always render the sidechain,
        // then render the backbone version
        /*
          mth 2004 04 26
          No, we are not going to do this any more
        render(bond, atomA, atomB);
        */
        atomA = getBackboneAtom(atomA);
        atomB = getBackboneAtom(atomB);
      } else if (hbondsBackbone &&
                 (order & JmolConstants.BOND_HYDROGEN_MASK)!=0) {
        atomA = getBackboneAtom(atomA);
        atomB = getBackboneAtom(atomB);
      }
    }
    render(bond, atomA, atomB);
  }

  void render(Bond bond, Atom atomA, Atom atomB) {
    this.atomA = atomA;
    xA = atomA.screenX; yA = atomA.screenY; zA = atomA.screenZ;
    this.atomB = atomB;
    xB = atomB.screenX; yB = atomB.screenY; zB = atomB.screenZ;
    if (zA ==1 || zB == 1)
      return;
    dx = xB - xA;
    dy = yB - yA;
    width = viewer.scaleToScreen((zA + zB)/2, bond.mad);
    bondOrder = getRenderBondOrder(bond.order);
    switch(bondOrder) {
    case 1:
    case 2:
    case 3:
    case 4:
      renderCylinder(0);
      break;
    case JmolConstants.BOND_ORDER_UNSPECIFIED:
    case JmolConstants.BOND_PARTIAL01:
      bondOrder = 1;
      renderCylinder(1);
      break;
    case JmolConstants.BOND_PARTIAL12:
    case JmolConstants.BOND_AROMATIC:
      bondOrder = 2;
      renderCylinder(getAromaticDottedBondMask(bond));
      break;
    case JmolConstants.BOND_STEREO_NEAR:
    case JmolConstants.BOND_STEREO_FAR:
      renderTriangle(bond);
      break;
    default:
      if ((bondOrder & JmolConstants.BOND_HYDROGEN_MASK) != 0) {
        if (hbondsSolid) {
          bondOrder = 1;
          renderCylinder(0);
        } else {
          renderHbondDashed();
        }
        break;
      }
    }
  }
    
  Atom getBackboneAtom(Atom atom) {
    if (atom.group instanceof Monomer)
      return ((Monomer)atom.group).getLeadAtom();
    return atom;
  }

  int getRenderBondOrder(int order) {
    if ((order & JmolConstants.BOND_SULFUR_MASK) != 0)
      order &= ~JmolConstants.BOND_SULFUR_MASK;
    if ((order & JmolConstants.BOND_PARTIAL_MASK) != 0)
      return order;
    if ((order & JmolConstants.BOND_COVALENT_MASK) != 0) {
      if (order == 1 ||
          !showMultipleBonds ||
          modeMultipleBond == JmolConstants.MULTIBOND_NEVER ||
          (modeMultipleBond == JmolConstants.MULTIBOND_NOTSMALL &&
           madBond > JmolConstants.madMultipleBondSmallMaximum)) {
        return 1;
      }
    }
    return order;
  }

  private void renderCylinder(int dottedMask) {
    boolean lineBond = (width <= 1);
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
      if ((dottedMask & 1) != 0) {
        drawDashed(lineBond, xA, yA, zA, xB, yB, zB);
      } else {
        if (lineBond)
          g3d.drawLine(colixA, colixB, xA, yA, zA, xB, yB, zB);
      //  else if (asBits) // time test shows bitset method to be slower
        //  g3d.fillCylinderBits(colixA, colixB, endcaps,
          //    width, xA, yA, zA, xB, yB, zB);
        else
          g3d.fillCylinder(colixA, colixB, endcaps,
                           width, xA, yA, zA, xB, yB, zB);
      }
      return;
    }
    int dxB = dx * dx;
    int dyB = dy * dy;
    int mag2d2 = dxB + dyB;
    if (bondOrder == 4)
      mag2d2 *= 4;
    mag2d = (int)(Math.sqrt(mag2d2) + 0.5);
    resetAxisCoordinates(lineBond);
    while (true) {
      if ((dottedMask & 1) != 0) {
        drawDashed(lineBond, xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
      } else {
        if (lineBond)
          g3d.drawLine(colixA, colixB,
                       xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
        else
          g3d.fillCylinder(colixA, colixB, endcaps, width,
                           xAxis1, yAxis1, zA, xAxis2, yAxis2, zB);
      }
      dottedMask >>= 1;
      if (--bondOrder == 0)
        break;
      stepAxisCoordinates();
    }
  }

  int cylinderNumber;
  int xAxis1, yAxis1, zAxis1, xAxis2, yAxis2, zAxis2, dxStep, dyStep;

  void resetAxisCoordinates(boolean lineBond) {
    cylinderNumber = 0;
    int space = mag2d >> 3;
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
    } else if (bondOrder == 4) {
      xAxis1 -= dxStep * 1.5; yAxis1 -= dyStep * 1.5;
      xAxis2 -= dxStep * 1.5; yAxis2 -= dyStep * 1.5;
    }
  }


  void stepAxisCoordinates() {
    xAxis1 += dxStep; yAxis1 += dyStep;
    xAxis2 += dxStep; yAxis2 += dyStep;
  }

  Rectangle rectTemp = new Rectangle();
  
  /*private boolean isClipVisible(int xA, int yA, int xB, int yB) {
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
    return visible;
  }*/

  /*private void renderDotted() {
    if (dx == 0 && dy == 0)
      return;
    g3d.drawDashedLine(colixA, colixB, 8, 4, xA, yA, zA, xB, yB, zB);
  }*/

  private static int wideWidthMilliAngstroms = 400;

  private void renderTriangle(Bond bond) {
    // for now, always solid, always opaque
    if (!g3d.checkTranslucent(false))
      return;
    int mag2d = (int)Math.sqrt(dx*dx + dy*dy);
    int wideWidthPixels = viewer.scaleToScreen(zB, wideWidthMilliAngstroms);
    int dxWide, dyWide;
    if (mag2d == 0) {
      dxWide = 0;
      dyWide = wideWidthPixels;
    } else {
      dxWide = wideWidthPixels * -dy / mag2d;
      dyWide = wideWidthPixels * dx / mag2d;
    }
    int xWideUp = xB + dxWide/2;
    int xWideDn = xWideUp - dxWide;
    int yWideUp = yB + dyWide/2;
    int yWideDn = yWideUp - dyWide;
    g3d.setColix(colixA);
    if (colixA == colixB) {
      g3d.drawfillTriangle(xA, yA, zA,
                           xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
    } else {
      int xMidUp = (xA + xWideUp) / 2;
      int yMidUp = (yA + yWideUp) / 2;
      int zMid = (zA + zB) / 2;
      int xMidDn = (xA + xWideDn) / 2;
      int yMidDn = (yA + yWideDn) / 2;
      g3d.drawfillTriangle(xA, yA, zA,
                           xMidUp, yMidUp, zMid, xMidDn, yMidDn, zMid);
      g3d.setColix(colixB);
      g3d.drawfillTriangle(xMidUp, yMidUp, zMid,
                           xMidDn, yMidDn, zMid, xWideDn, yWideDn, zB);
      g3d.drawfillTriangle(xMidUp, yMidUp, zMid,
                           xWideUp, yWideUp, zB, xWideDn, yWideDn, zB);
    }
  }

  void drawDottedCylinder(short colixA, short colixB, int width,
                          int x1, int y1, int z1, int x2, int y2, int z2) {
    int dx = x2 - x1;
    int dy = y2 - y1;
    int dz = z2 - z1;
    boolean ok = g3d.setColix(colixB);
    for (int i = 8; --i >= 0; ) {
      int x = x1 + (dx * i) / 7;
      int y = y1 + (dy * i) / 7;
      int z = z1 + (dz * i) / 7;
      if (i == 3 && !(ok = g3d.setColix(colixA)))
        return;
      if (ok)
        g3d.fillSphereCentered(width, x, y, z);
    }
  }

  private int getAromaticDottedBondMask(Bond bond) {
    Atom atomC = findAromaticNeighbor(bond);
    if (atomC == null)
      return 1;
    int dxAC = atomC.screenX - xA;
    int dyAC = atomC.screenY - yA;
    return (dx * dyAC - dy * dxAC) >= 0 ? 2 : 1;
  }

  private Atom findAromaticNeighbor(Bond bond) {
    Bond[] bonds = atomB.bonds;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      if ((bondT.order & JmolConstants.BOND_AROMATIC) == 0)
        continue;
      if (bondT == bond)
        continue;
      if (bondT.atom1 == atomB)
        return bondT.atom2;
      if (bondT.atom2 == atomB)
        return bondT.atom1;
    }
    return null;
  }

  void drawDashed(boolean lineBond,
                  int xA, int yA, int zA,
                  int xB, int yB, int zB) {
    int dx = xB - xA;
    int dy = yB - yA;
    int dz = zB - zA;
    int i = 2;
    while (i <= 9) {
      int xS = xA + (dx * i) / 12;
      int yS = yA + (dy * i) / 12;
      int zS = zA + (dz * i) / 12;
      i += 3;
      int xE = xA + (dx * i) / 12;
      int yE = yA + (dy * i) / 12;
      int zE = zA + (dz * i) / 12;
      i += 2;
      if (lineBond)
        g3d.drawLine(colixA, colixB, xS, yS, zS, xE, yE, zE);
      else 
        g3d.fillCylinder(colixA, colixB, Graphics3D.ENDCAPS_FLAT, width,
                         xS, yS, zS, xE, yE, zE);
    }
  }

  void renderHbondDashed() {
    boolean lineBond = (width <= 1);
   int dx = xB - xA;
    int dy = yB - yA;
    int dz = zB - zA;
    int i = 1;
    while (i < 10) {
      int xS = xA + (dx * i) / 10;
      int yS = yA + (dy * i) / 10;
      int zS = zA + (dz * i) / 10;
      short colixS = i < 5 ? colixA : colixB;
      i += 2;
      int xE = xA + (dx * i) / 10;
      int yE = yA + (dy * i) / 10;
      int zE = zA + (dz * i) / 10;
      short colixE = i < 5 ? colixA : colixB;
      ++i;
      if (lineBond)
        g3d.drawLine(colixS, colixE, xS, yS, zS, xE, yE, zE);
      else 
        g3d.fillCylinder(colixS, colixE, Graphics3D.ENDCAPS_FLAT, width,
                         xS, yS, zS, xE, yE, zE);
    }
  }
}
