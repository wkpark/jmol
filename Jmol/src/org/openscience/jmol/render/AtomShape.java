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

package org.openscience.jmol.render;

import org.openscience.jmol.*;
import org.openscience.jmol.g25d.Graphics25D;
import org.openscience.jmol.g25d.Colix;

import java.awt.Rectangle;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class AtomShape extends Shape {

  public Atom atom;
  public byte styleAtom;
  public short marAtom;
  public short colixAtom;
  public int diameter;
  public short marDots = 0;
  public short colixDots;
  public int diameterDots = 0;
  public int numBonds;
  public byte[] bondOrders;
  public short[] bondWidths;  // after a delete operation, these arrays
  public byte[] styleBonds; // will be longer than numBonds
  public short[] marBonds;
  public short[] acolixBonds;
  public String strLabel;
  
  public AtomShape(Atom atom,
                   byte styleAtom, short marAtom, short colixAtom,
                   byte styleBond, short marBond, short colixBond,
                   String strLabel) {
    this.atom = atom;
    numBonds = atom.getBondedCount();
    bondOrders = new byte[numBonds];
    fixBondOrders();

    bondWidths = new short[numBonds];
    styleBonds = new byte[numBonds];
    marBonds = new short[numBonds];
    acolixBonds = new short[numBonds];
    setStyleMarAtom(styleAtom, marAtom);
    setStyleMarAllBonds(styleBond, marBond);
    setStyleMarAllBackbones(DisplayControl.NONE, (short)0);
    setColixAllBonds(colixBond);
    this.colixAtom = colixAtom;
    this.strLabel = strLabel;
  }

  public AtomShape(Atom atom, DisplayControl c) {
    this(atom,
         c.getStyleAtom(), c.getMarAtom(), c.getColixAtom(atom),
         c.getStyleBond(), c.getMarBond(), c.getColixBond(),
         c.getLabelAtom(atom));
  }

  public String toString() {
    return "Atom shape for " + atom + ": z = " + z;
  }

  public void deleteBond(int i) {
    --numBonds;
    int numAbove = numBonds - i;
    if (numAbove > 0) {
      System.arraycopy(bondOrders, i+1, bondOrders, i, numAbove);
      System.arraycopy(bondWidths, i+1, bondWidths, i, numAbove);
      System.arraycopy(styleBonds, i+1, styleBonds, i, numAbove);
      System.arraycopy(marBonds,   i+1, marBonds,   i, numAbove);
      System.arraycopy(acolixBonds, i+1, acolixBonds, i, numAbove);
    }
  }

  private void fixBondOrders() {
    Atom[] bondedAtoms = atom.getBondedAtoms();
    for (int i = numBonds; --i >= 0 ; ) {
      Atom atomOther = bondedAtoms[i];
      bondOrders[i] = (byte) atom.getBondOrder(atomOther);
    }
  }

  /*
   * What is a MAR?
   *  - just a term that I made up
   *  - an abbreviation for Milli Angstrom Radius
   * that is:
   *  - a *radius* of either a bond or an atom
   *  - in *millis*, or thousandths of an *angstrom*
   *  - stored as a short
   *
   * However! In the case of an atom radius, if the parameter
   * gets passed in as a negative number, then that number
   * represents a percentage of the vdw radius of that atom.
   * This is converted to a normal MAR as soon as possible
   *
   * (I know almost everyone hates bytes & shorts, but I like them ...
   *  gives me some tiny level of type-checking ...
   *  a rudimentary form of enumerations/user-defined primitive types)
   */

  public void setStyleAtom(byte styleAtom) {
    this.styleAtom = styleAtom;
  }

  public void setMarAtom(short marAtom) {
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) * atom.getVanderwaalsRadius());
    this.marAtom = marAtom;
  }

  public void setStyleMarAtom(byte styleAtom, short marAtom) {
    this.styleAtom = styleAtom;
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) * atom.getVanderwaalsRadius());
    this.marAtom = marAtom;
  }
        
  public void setColixMarDots(short colixDots, short marDots) {
    this.colixDots = colixDots;
    if (marDots < 0)
      marDots = (short)((-10 * marDots) * atom.getVanderwaalsRadius());
    this.marDots = marDots;
  }

  public int getRasMolRadius() {
    if (styleAtom == DisplayControl.NONE)
      return 0;
    return marAtom / 4;
  }

  public void setStyleAllBonds(byte styleBond) {
    for (int i = numBonds; --i >= 0; )
      if (bondOrders[i] > 0)
        styleBonds[i] = styleBond;
  }

  public void setStyleAllBackbones(byte styleBackbone) {
    for (int i = numBonds; --i >= 0; )
      if (bondOrders[i] == -1)
        styleBonds[i] = styleBackbone;
  }

  public void setMarAllBonds(short marBond) {
    for (int i = numBonds; --i >= 0; )
      if (bondOrders[i] > 0)
        marBonds[i] = marBond;
  }

  public void setMarAllBackbones(short marBackbone) {
    for (int i = numBonds; --i >= 0; )
      if (bondOrders[i] == -1)
        marBonds[i] = marBackbone;
  }

  public void setStyleMarAllBonds(byte styleBond, short marBond) {
    for (int i = numBonds; --i >= 0; ) {
      if (bondOrders[i] > 0) {
        styleBonds[i] = styleBond;
        marBonds[i] = marBond;
      }
    }
  }

  public void setStyleMarAllBackbones(byte styleBackbone, short marBackbone) {
    for (int i = numBonds; --i >= 0; ) {
      if (bondOrders[i] == -1) {
        styleBonds[i] = styleBackbone;
        marBonds[i] = marBackbone;
      }
    }
  }

  public void setStyleBond(byte styleBond, int indexBond) {
    if (bondOrders[indexBond] > 0) 
      styleBonds[indexBond] = styleBond;
  }

  public void setMarBond(short marBond, int indexBond) {
    if (bondOrders[indexBond] > 0)
      marBonds[indexBond] = marBond;
  }

  public void setStyleMarBond(byte styleBond, short marBond, int indexBond) {
    if (bondOrders[indexBond] > 0) {
      styleBonds[indexBond] = styleBond;
      marBonds[indexBond] = marBond;
    }
  }

  public void setStyleBackbone(byte styleBond, int indexBond) {
    if (bondOrders[indexBond] == -1)
      styleBonds[indexBond] = styleBond;
  }

  public void setMarBackbone(short marBackbone, int indexBackbone) {
    if (bondOrders[indexBackbone] == -1)
      marBonds[indexBackbone] = marBackbone;
  }

  public void setStyleMarBackbone(byte styleBackbone,
                                  short marBackbone, int indexBackbone) {
    if (bondOrders[indexBackbone] == -1) {
      styleBonds[indexBackbone] = styleBackbone;
      marBonds[indexBackbone] = marBackbone;
    }
  }

  public void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  public void setColixAllBonds(short colixBond) {
    for (int i = numBonds; --i >= 0; )
      if (bondOrders[i] > 0)
        acolixBonds[i] = colixBond;
  }

  public void setColixBond(short colixBond, int indexBond) {
    if (bondOrders[indexBond] > 0)
      acolixBonds[indexBond] = colixBond;
  }

  public void setColixAllBackbones(short colixBackbone) {
    for (int i = numBonds; --i >= 0; )
      if (bondOrders[i] == -1)
        acolixBonds[i] = colixBackbone;
  }

  public void setColixBackbone(short colixBackbone, int indexBackbone) {
    if (bondOrders[indexBackbone] == -1)
      acolixBonds[indexBackbone] = colixBackbone;
  }

  public void setLabel(String strLabel) {
    // note that strLabel could be null
    this.strLabel = strLabel;
  }

  public void transform(DisplayControl control) {
    Point3i screen = control.transformPoint(atom.getPoint3D());
    x = screen.x;
    y = screen.y;
    z = screen.z;
    diameter = control.scaleToScreen(z, marAtom * 2);
    if (marDots > 0)
      diameterDots = control.scaleToScreen(z, marDots * 2);
    for (int i = numBonds; --i >= 0; )
      bondWidths[i] = (short)control.scaleToScreen(z, marBonds[i] * 2);
  }

  public void render(Graphics25D g25d, DisplayControl control) {
    if (!control.getShowHydrogens() && atom.isHydrogen())
      return;
    if (control.getShowBonds())
      renderBonds(control);
    if (control.getShowAtoms() && isClipVisible(control.atomRenderer.clip))
      control.atomRenderer.render(this);
    if (strLabel != null)
      control.labelRenderer.render(this);
  }

  public void renderBonds(DisplayControl control) {
    Atom[] bondedAtoms = atom.getBondedAtoms();
    if (bondedAtoms == null)
      return;
    for (int i = numBonds; --i >= 0 ; ) {
      Atom atomOther = bondedAtoms[i];
      AtomShape atomShapeOther = atomOther.getAtomShape();
      int zOther = atomShapeOther.z;
      if ((control.getShowHydrogens() || !atomOther.isHydrogen()) &&
          ((z < zOther) ||
           (z==zOther && atom.getAtomNumber()>atomOther.getAtomNumber())) &&
          isBondClipVisible(control.bondRenderer.clip,
                            x, y, atomShapeOther.x, atomShapeOther.y)) {
        Atom[] otherAtomBonds = atomOther.getBondedAtoms();
        for (int j = atomShapeOther.numBonds; --j >= 0; ) {
          if (otherAtomBonds[j] == atom) {
            control.bondRenderer.render(this, i, atomShapeOther, j,
                                        bondOrders[i]);
            // FIXME implement atom.getBondOrder(int indexBond)
          }
        }
      }
    }
  }

  static Rectangle rectTemp = new Rectangle();
  boolean isClipVisible(Rectangle clip) {
    int radius = diameter / 2;
    rectTemp.x = x - radius;
    rectTemp.y = y - radius;
    rectTemp.width = diameter;
    rectTemp.height = diameter;
    //    rectTemp.setRect(x - radius, y - radius, diameter, diameter);
    // note that this is not correct if the atom is selected
    // because the halo may be visible while the atom is not
    boolean visible = clip.intersects(rectTemp);
    /*
    System.out.println("isClipVisible -> " + visible);
    System.out.println(" x=" + x + " y=" + y + " diameter=" + diameter);
    visible = true;
    */
    return visible;
  }

  private boolean isBondClipVisible(Rectangle clip,
                                    int x1, int y1, int x2, int y2) {
    // this is not actually correct, but quick & dirty
    int xMin, width, yMin, height;
    if (x1 < x2) {
      xMin = x1;
      width = x2 - x1;
    } else if (x2 < x1) {
      xMin = x2;
      width = x1 - x2;
    } else {
      xMin = x1;
      width = 1;
    }
    if (y1 < y2) {
      yMin = y1;
      height = y2 - y1;
    } else if (y2 < y1) {
      yMin = y2;
      height = y1 - y2;
    } else {
      yMin = y1;
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
    boolean visible = clip.intersects(rectTemp);
    /*
    System.out.println("bond " + x + "," + y + "->" + x2 + "," + y2 +
                       " & " + clip.x + "," + clip.y +
                       " W " + clip.width + " H " + clip.height +
                       "->" + visible);
    visible = true;
    */
    return visible;
  }

}

