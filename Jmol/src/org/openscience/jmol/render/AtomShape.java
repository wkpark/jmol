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

import org.openscience.jmol.DisplayControl;
import org.openscience.jmol.g25d.Graphics25D;
import org.openscience.jmol.g25d.Colix;
import org.openscience.jmol.ProteinProp;

import java.awt.Rectangle;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class AtomShape extends Shape implements Bspt.Tuple {

  public JmolFrame frame;
  public short atomIndex = -1;
  public Object clientAtom;
  public Point3d point3d;
  public byte atomicNumber;
  public byte styleAtom;
  public short marAtom;
  public short colixAtom;
  public int diameter;
  public short marDots = 0;
  public short colixDots;
  public int diameterDots = 0;
  public BondShape[] bonds;

  public String strLabel;
  public ProteinProp pprop;
  
  public AtomShape(JmolFrame frame, int atomIndex, Object clientAtom) {
    DisplayControl control = frame.control;
    this.frame = frame;
    this.atomIndex = (short)atomIndex;
    this.clientAtom = clientAtom;
    this.atomicNumber = (byte) control.getAtomicNumber(clientAtom);
    this.colixAtom = control.getColixAtom(atomicNumber, clientAtom);
    if (frame.hasPdbRecords()) {
      String pdbRecord = control.getPdbAtomRecord(clientAtom);
      if (pdbRecord != null)
        pprop = new ProteinProp(pdbRecord);
    }
    this.colixDots = 0;
    this.marDots = 0;
    setStyleMarAtom(control.getStyleAtom(), control.getMarAtom());
    this.point3d = control.getPoint3d(clientAtom);
    this.strLabel = control.getLabelAtom(this, atomIndex);
  }

  public Object getClientAtom() {
    return clientAtom;
  }

  public int getAtomIndex() {
    return atomIndex;
  }

  public boolean isBonded(AtomShape atomShapeOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0; ) {
        BondShape bond = bonds[i];
        if ((bond.atomShape1 == atomShapeOther) ||
            (bond.atomShape2 == atomShapeOther))
          return true;
      }
    return false;
  }

  public BondShape bondMutually(AtomShape atomShapeOther, int order) {
    if (isBonded(atomShapeOther))
      return null;
    BondShape bondShape = new BondShape(this, atomShapeOther, order,
                                        frame.control);
    addBond(bondShape);
    atomShapeOther.addBond(bondShape);
    return bondShape;
  }

  private void addBond(BondShape bondShape) {
    int i = 0;
    if (bonds == null) {
      bonds = new BondShape[1];
    } else {
      i = bonds.length;
      BondShape[] bondsNew = new BondShape[i + 1];
      System.arraycopy(bonds, 0, bondsNew, 0, i);
      bonds = bondsNew;
    }
    bonds[i] = bondShape;
  }

  public void deleteBondedAtomShape(AtomShape atomShapeToDelete) {
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0; ) {
      BondShape bond = bonds[i];
      AtomShape atomShapeBonded =
        (bond.atomShape1 != this) ? bond.atomShape1 : bond.atomShape2;
      if (atomShapeBonded == atomShapeToDelete) {
        deleteBond(i);
        return;
      }
    }
  }

  public void deleteBond(BondShape bondShape) {
    for (int i = bonds.length; --i >= 0; )
      if (bonds[i] == bondShape) {
        deleteBond(i);
        return;
      }
  }

  public void deleteBond(int i) {
    int newLength = bonds.length - 1;
    if (newLength == 0) {
      bonds = null;
      return;
    }
    BondShape[] bondsNew = new BondShape[newLength];
    int j = 0;
    for ( ; j < i; ++j)
      bondsNew[j] = bonds[j];
    for ( ; j < newLength; ++j)
      bondsNew[j] = bonds[j + 1];
    bonds = bondsNew;
  }

  public void clearBonds() {
    bonds = null;
  }

  public int getBondedAtomIndex(int bondIndex) {
    BondShape bond = bonds[bondIndex];
    return ((bond.atomShape1 == this) ? bond.atomShape2 : bond.atomShape1).atomIndex;
  }

  /*
  private void fixBondOrders() {
    Atom[] bondedAtoms = atom.getBondedAtoms();
    for (int i = numBonds; --i >= 0 ; ) {
      Atom atomOther = bondedAtoms[i];
      bondOrders[i] = (byte) atom.getBondOrder(atomOther);
    }
  }
  */

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
      marAtom = (short)((-10 * marAtom) *
                        frame.control.getVanderwaalsRadius(atomicNumber, 
                                                           clientAtom));
    this.marAtom = marAtom;
  }

  public void setStyleMarAtom(byte styleAtom, short marAtom) {
    this.styleAtom = styleAtom;
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) *
                        frame.control.getVanderwaalsRadius(atomicNumber,
                                                           clientAtom));
    this.marAtom = marAtom;
  }
        
  public void setColixMarDots(short colixDots, short marDots) {
    this.colixDots = colixDots;
    if (marDots < 0)
      marDots = (short)((-10 * marDots) *
                        frame.control.getVanderwaalsRadius(atomicNumber,
                                                           clientAtom));
    this.marDots = marDots;
  }

  public int getRasMolRadius() {
    if (styleAtom == DisplayControl.NONE)
      return 0;
    return marAtom / 4;
  }

  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & BondShape.COVALENT) != 0)
        ++n;
    return n;
  }

  public BondShape[] getBonds() {
    return bonds;
  }

  public void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  public void setLabel(String strLabel) {
    // note that strLabel could be null
    this.strLabel = strLabel;
  }

  public void transform(DisplayControl control) {
    Point3i screen = control.transformPoint(point3d);
    x = screen.x;
    y = screen.y;
    z = screen.z;
    diameter = control.scaleToScreen(z, marAtom * 2);
    if (marDots > 0)
      diameterDots = control.scaleToScreen(z, marDots * 2);
    /*
    for (int i = numBonds; --i >= 0; )
      bondWidths[i] = (short)control.scaleToScreen(z, marBonds[i] * 2);
    */
  }

  public void render(Graphics25D g25d, DisplayControl control) {
    if (atomicNumber == 1 && !control.getShowHydrogens())
      return;
    renderBonds(control);
    if (isClipVisible(control.atomRenderer.clip))
      control.atomRenderer.render(this);
    if (strLabel != null)
      control.labelRenderer.render(this);
  }

  public void renderBonds(DisplayControl control) {
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0; ) {
      BondShape bond = bonds[i];
      AtomShape atomShapeOther =
        (bond.atomShape1 == this) ? bond.atomShape2 : bond.atomShape1;
      int zOther = atomShapeOther.z;
      if ((atomShapeOther.atomicNumber != 1 || control.getShowHydrogens()) &&
          ((z < zOther) ||
           (z==zOther && (bond.atomShape1 == this))) &&
          isBondClipVisible(control.bondRenderer.clip,
                            x, y, atomShapeOther.x, atomShapeOther.y))
        bond.render(control);
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

  /*
   * mth 2003 07 30
   * These routines are currently going through the Atom
   * but soon they will go through an interface which defines their behavior
   */

  public int getAtomicNumber() {
    return frame.control.getAtomicNumber(clientAtom);
  }

  public String getAtomicSymbol() {
    return frame.control.getAtomicSymbol(atomicNumber, clientAtom);
  }

  public Point3d getPoint3d() {
    return point3d;
  }

  public double getX() {
    return point3d.x;
  }

  public double getY() {
    return point3d.y;
  }

  public double getZ() {
    return point3d.z;
  }

  public double getDimensionValue(int dimension) {
    return (dimension == 0
            ? point3d.x
            : (dimension == 1 ? point3d.y : point3d.z));
  }

  public double getVanderwaalsRadius() {
    return frame.control.getVanderwaalsRadius(atomicNumber, clientAtom);
  }

  public double getCovalentRadius() {
    return frame.control.getCovalentRadius(atomicNumber, clientAtom);
  }

  public ProteinProp getProteinProp() {
    return pprop;
  }
}
