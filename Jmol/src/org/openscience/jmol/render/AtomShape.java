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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;

import javax.vecmath.Point3d;

public class AtomShape extends Shape {

  public Atom atom;
  public byte styleAtom;
  public short marAtom;
  public Color colorAtom;
  public int diameter;
  public int numBonds;
  public int[] bondWidths;
  public byte[] styleBonds;
  public short[] marBonds;
  public Color[] colorBonds;
  public String strLabel;
  
  public AtomShape(Atom atom,
                   byte styleAtom, short marAtom, Color colorAtom,
                   byte styleBond, short marBond, Color colorBond,
                   String strLabel) {
    this.atom = atom;
    numBonds = atom.getBondedCount();
    bondWidths = new int[numBonds];
    styleBonds = new byte[numBonds];
    marBonds = new short[numBonds];
    colorBonds = new Color[numBonds];
    setStyleMarAtom(styleAtom, marAtom);
    setStyleMarAllBonds(styleBond, marBond);
    setColorAllBonds(colorBond);
    this.colorAtom = colorAtom;
    this.strLabel = strLabel;
  }

  public AtomShape(Atom atom, DisplayControl c) {
    this(atom,
         c.getStyleAtom(), c.getMarAtom(), c.getColorAtom(atom),
         c.getStyleBond(), c.getMarBond(), c.getColorBond(),
         c.getLabelAtom(atom));
  }

  public String toString() {
    return "Atom shape for " + atom + ": z = " + z;
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
   * (I know everyone hates bytes & shorts, but I like them ...
   *  gives me some tiny level of type-checking ...
   *  a rudimentary form of enumerations/user-defined primitive types)
   */

  public void setStyleAtom(byte styleAtom) {
    this.styleAtom = styleAtom;
  }

  public void setMarAtom(short marAtom) {
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) * atom.getVdwRadius());
    this.marAtom = marAtom;
  }
        
  public void setStyleMarAtom(byte styleAtom, short marAtom) {
    this.styleAtom = styleAtom;
    if (marAtom < 0) {
      // a percentage of the atom vdw
      // radius * 2 * 1000 * -marAtom / 100
      marAtom = (short)((-10 * marAtom) * atom.getVdwRadius());
    }
    this.marAtom = marAtom;
  }
        
  public void setStyleAllBonds(byte styleBond) {
    for (int i = numBonds; --i >= 0; )
      styleBonds[i] = styleBond;
  }

  public void setMarAllBonds(short marBond) {
    for (int i = numBonds; --i >= 0; )
      marBonds[i] = marBond;
  }

  public void setStyleMarAllBonds(byte styleBond, short marBond) {
    for (int i = numBonds; --i >= 0; ) {
      styleBonds[i] = styleBond;
      marBonds[i] = marBond;
    }
  }

  public void setStyleBond(byte styleBond, int indexBond) {
    styleBonds[indexBond] = styleBond;
  }

  public void setMarBond(short marBond, int indexBond) {
    marBonds[indexBond] = marBond;
  }

  public void setStyleMarBond(byte styleBond, short marBond, int indexBond) {
    styleBonds[indexBond] = styleBond;
    marBonds[indexBond] = marBond;
  }

  public void setColorAtom(Color colorAtom) {
    this.colorAtom = colorAtom;
  }

  public void setColorAllBonds(Color colorBond) {
    for (int i = numBonds; --i >= 0; )
      colorBonds[i] = colorBond;
  }

  public void setColorBond(Color colorBond, int indexBond) {
    colorBonds[indexBond] = colorBond;
  }

  public void setLabel(String strLabel) {
    // note that strLabel could be null
    this.strLabel = strLabel;
  }

  public void transform(DisplayControl control) {
    Point3d screen = control.transformPoint(atom.getPosition());
    x = (int)screen.x;
    y = (int)screen.y;
    z = (int)screen.z;
    diameter = control.scaleToScreen(z, marAtom * 2);
    for (int i = numBonds; --i >= 0; )
      bondWidths[i] = control.scaleToScreen(z, marBonds[i] * 2);
  }

  public void render(Graphics g, DisplayControl control) {
    if (!control.getShowHydrogens() && atom.isHydrogen()) {
      return;
    }
    if (control.getShowBonds()) {
      renderBonds(g, control);
    }
    if (control.getShowAtoms() && isClipVisible(control.atomRenderer.clip)) {
      control.atomRenderer.render(this);
    }
    if (strLabel != null)
      control.labelRenderer.render(this);
  }

  public void renderBonds(Graphics g, DisplayControl control) {
    Atom[] bondedAtoms = atom.getBondedAtoms();
    if (bondedAtoms == null) {
      return;
    }
    for (int i = numBonds; --i >= 0 ; ) {
      Atom atomOther = bondedAtoms[i];
      AtomShape atomShapeOther = atomOther.getAtomShape();
      int zOther = atomShapeOther.z;
      if ((control.getShowHydrogens() || !atomOther.isHydrogen()) &&
          ((z > zOther) ||
           (z==zOther && atom.getAtomNumber()>atomOther.getAtomNumber())) &&
          isBondClipVisible(control.bondRenderer.clip,
                            x, y, atomShapeOther.x, atomShapeOther.y)) {
        Atom[] otherAtomBonds = atomOther.getBondedAtoms();
        for (int j = atomShapeOther.numBonds; --j >= 0; ) {
          if (otherAtomBonds[j] == atom) {
            control.bondRenderer.render(this, i, atomShapeOther, j,
                                        atom.getBondOrder(atomOther));
            // FIXME implement atom.getBondOrder(int)
          }
        }
      }
    }
  }

  private static Rectangle rectTemp = new Rectangle();
  private boolean isClipVisible(Rectangle clip) {
    int radius = diameter / 2;
    rectTemp.setRect(x - radius, y - radius, diameter, diameter);
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
    rectTemp.setRect(xMin, yMin, width, height);
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

