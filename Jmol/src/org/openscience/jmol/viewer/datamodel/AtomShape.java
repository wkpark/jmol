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

import org.openscience.jmol.viewer.JmolViewer;
import org.openscience.jmol.viewer.g3d.Graphics3D;
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.protein.ProteinProp;

import java.awt.Rectangle;

import javax.vecmath.Point3d;
import javax.vecmath.Point3i;

public class AtomShape implements Bspt.Tuple {

  public Object clientAtom;
  JmolFrame frame;
  Point3d point3d;
  int x, y, z;
  byte atomicNumber;
  byte styleAtom;
  private short atomIndex = -1;
  short marAtom;
  short colixAtom;
  short diameter;
  short marDots = 0;
  short colixDots;
  short diameterDots = 0;
  BondShape[] bonds;

  String strLabel;
  ProteinProp pprop;
  
  public AtomShape(JmolFrame frame, int atomIndex, Object clientAtom) {
    JmolViewer viewer = frame.viewer;
    this.frame = frame;
    this.atomIndex = (short)atomIndex;
    this.clientAtom = clientAtom;
    this.atomicNumber = (byte) viewer.getAtomicNumber(clientAtom);
    this.colixAtom = viewer.getColixAtom(atomicNumber, clientAtom);
    if (frame.hasPdbRecords()) {
      String pdbRecord = viewer.getPdbAtomRecord(clientAtom);
      if (pdbRecord != null)
        pprop = new ProteinProp(pdbRecord);
    }
    this.colixDots = 0;
    this.marDots = 0;
    setStyleMarAtom(viewer.getStyleAtom(), viewer.getMarAtom());
    this.point3d = viewer.getPoint3d(clientAtom);
    this.strLabel = viewer.getLabelAtom(this, atomIndex);
  }

  public Object getClientAtom() {
    return clientAtom;
  }

  public int getAtomIndex() {
    return atomIndex & 0xFFFF;
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
                                        frame.viewer);
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

  public void deleteAllBonds() {
    if (bonds == null)
      return;
    System.out.println("bonds.length=" + bonds.length);
    for (int i = bonds.length; --i >= 0; )
      frame.deleteBond(bonds[i]);
    if (bonds != null) {
      System.out.println("bond delete error");
      throw new NullPointerException();
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
    System.out.println("deleting bond " + i);
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
    return (((bond.atomShape1 == this)
             ? bond.atomShape2
             : bond.atomShape1).atomIndex & 0xFFFF);
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
    if (this.styleAtom == JmolViewer.DELETED) return;
      this.styleAtom = styleAtom;
  }

  public void setMarAtom(short marAtom) {
    if (this.styleAtom == JmolViewer.DELETED) return;
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) *
                        frame.viewer.getVanderwaalsRadius(atomicNumber,
                                                          clientAtom));
    this.marAtom = marAtom;
  }

  public void setStyleMarAtom(byte styleAtom, short marAtom) {
    if (this.styleAtom == JmolViewer.DELETED) return;
    this.styleAtom = styleAtom;
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) *
                        frame.viewer.getVanderwaalsRadius(atomicNumber,
                                                          clientAtom));
    this.marAtom = marAtom;
  }
        
  public void setColixMarDots(short colixDots, short marDots) {
    if (this.styleAtom == JmolViewer.DELETED) return;
    this.colixDots = colixDots;
    if (marDots < 0)
      marDots = (short)((-10 * marDots) *
                        frame.viewer.getVanderwaalsRadius(atomicNumber, clientAtom));
    this.marDots = marDots;
  }

  public int getRasMolRadius() {
    if (styleAtom <= JmolViewer.NONE)
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

  public void transform(JmolViewer viewer) {
    Point3i screen = viewer.transformPoint(point3d);
    x = screen.x;
    y = screen.y;
    z = screen.z;
    diameter = viewer.scaleToScreen(z, marAtom * 2);
    if (marDots > 0)
      diameterDots = viewer.scaleToScreen(z, marDots * 2);
  }

  public int getAtomicNumber() {
    return frame.viewer.getAtomicNumber(clientAtom);
  }

  public String getAtomicSymbol() {
    return frame.viewer.getAtomicSymbol(atomicNumber, clientAtom);
  }

  public String getAtomTypeName() {
    return frame.viewer.getAtomTypeName(atomicNumber, clientAtom);
  }

  public Point3d getPoint3d() {
    return point3d;
  }

  public double getAtomX() {
    return point3d.x;
  }

  public double getAtomY() {
    return point3d.y;
  }

  public double getAtomZ() {
    return point3d.z;
  }

  public double getDimensionValue(int dimension) {
    return (dimension == 0
            ? point3d.x
            : (dimension == 1 ? point3d.y : point3d.z));
  }

  public double getVanderwaalsRadius() {
    return frame.viewer.getVanderwaalsRadius(atomicNumber, clientAtom);
  }

  public double getCovalentRadius() {
    return frame.viewer.getCovalentRadius(atomicNumber, clientAtom);
  }

  public short getColix() {
    return colixAtom;
  }

  public double getRadius() {
    if (styleAtom <= JmolViewer.NONE)
      return 0;
    double radius = marAtom / 1000.0;
    if (styleAtom == JmolViewer.WIREFRAME) return -radius;
    return radius;
  }

  public ProteinProp getProteinProp() {
    return pprop;
  }

  public Object markDeleted() {
    deleteAllBonds();
    styleAtom = JmolViewer.DELETED;
    Object clientAtom = this.clientAtom;
    this.clientAtom = null;
    return clientAtom;
  }
}
