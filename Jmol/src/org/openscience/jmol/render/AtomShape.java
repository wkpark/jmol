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
  public int madAtom;
  public Color colorAtom;
  public int diameter;
  public int numBonds;
  public int[] bondWidths;
  public byte[] styleBonds;
  public int[] madBonds;
  public String strLabel;
  
  public AtomShape(Atom atom,
                   byte styleAtom, int madAtom,
                   byte styleBond, int madBond,
                   Color colorAtom, String strLabel) {
    this.atom = atom;
    numBonds = atom.getBondedCount();
    bondWidths = new int[numBonds];
    styleBonds = new byte[numBonds];
    madBonds = new int[numBonds];
    setStyleMadAtom(styleAtom, madAtom);
    setStyleMadAllBonds(styleBond, madBond);
    this.colorAtom = colorAtom;
    this.strLabel = strLabel;
  }

  public String toString() {
    return "Atom shape for " + atom + ": z = " + z;
  }

  /*
   * What is a MAD?
   *  - just a term that I made up
   *  - an abbreviation for Milli Angstrom Diameter
   * that is
   *  - a *diameter* of either a bond or an atom
   *  - in *millis*, or thousandths of an *angstrom*
   *  - stored as an integer
   *
   * However! In the case of an atom diameter, if the parameter
   * gets passed in as a negative number, then that number
   * represents a percentage of the vdw radius of that atom.
   * This is converted to a normal MAD as soon as possible
   */

  public void setStyleAtom(byte styleAtom) {
    this.styleAtom = styleAtom;
  }

  public void setMadAtom(int madAtom) {
    if (madAtom < 0)
      madAtom = (int)((-20 * madAtom) * atom.getVdwRadius());
    this.madAtom = madAtom;
  }
        
  public void setStyleMadAtom(byte styleAtom, int madAtom) {
    this.styleAtom = styleAtom;
    if (madAtom < 0) {
      // a percentage of the atom vdw
      // radius * 2 * 1000 * -madAtom / 100
      madAtom = (int)((-20 * madAtom) * atom.getVdwRadius());
    }
    this.madAtom = madAtom;
  }
        
  public void setStyleAllBonds(byte styleBond) {
    for (int i = numBonds; --i >= 0; )
      styleBonds[i] = styleBond;
  }

  public void setMadAllBonds(int madBond) {
    for (int i = numBonds; --i >= 0; )
      madBonds[i] = madBond;
  }

  public void setStyleMadAllBonds(byte styleBond, int madBond) {
    for (int i = numBonds; --i >= 0; ) {
      styleBonds[i] = styleBond;
      madBonds[i] = madBond;
    }
  }

  public void setStyleBond(byte styleBond, int indexBond) {
    styleBonds[indexBond] = styleBond;
  }

  public void setMadBond(int madBond, int indexBond) {
    madBonds[indexBond] = madBond;
  }

  public void setStyleMadBond(byte styleBond, int madBond, int indexBond) {
    styleBonds[indexBond] = styleBond;
    madBonds[indexBond] = madBond;
  }

  public void setColorAtom(Color colorAtom) {
    this.colorAtom = colorAtom;
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
    diameter = control.scaleToScreen(z, madAtom);
    for (int i = numBonds; --i >= 0; )
      bondWidths[i] = control.scaleToScreen(z, madBonds[i]);
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

