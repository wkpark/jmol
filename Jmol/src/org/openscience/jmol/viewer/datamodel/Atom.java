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
import org.openscience.jmol.viewer.g3d.Colix;
import org.openscience.jmol.viewer.protein.PdbAtom;

import java.awt.Rectangle;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public class Atom implements Bspt.Tuple {

  private short atomIndex = -1;
  public byte atomicNumber;
  public Object clientAtom;
  public PdbAtom pdbatom;
  Frame frame;
  Point3f point3f;
  int x, y, z;
  byte styleAtom;
  short marAtom;
  short colixAtom;
  short diameter;
  Bond[] bonds;

  String strLabel;

  public Atom(Frame frame, int atomIndex, Object clientAtom) {
    JmolViewer viewer = frame.viewer;
    this.frame = frame;
    this.atomIndex = (short)atomIndex;
    this.clientAtom = clientAtom;
    this.atomicNumber = (byte) viewer.getAtomicNumber(clientAtom);
    if (frame.hasPdbRecords()) {
      String pdbRecord = viewer.getPdbAtomRecord(clientAtom);
      if (pdbRecord != null)
        pdbatom = new PdbAtom(pdbRecord);
    }
    this.colixAtom = viewer.getColixAtom(this);
    setStyleMarAtom(viewer.getStyleAtom(), viewer.getMarAtom());
    this.point3f = new Point3f(viewer.getAtomX(clientAtom),
			       viewer.getAtomY(clientAtom),
			       viewer.getAtomZ(clientAtom));
    this.strLabel = viewer.getLabelAtom(this, atomIndex);
  }

  public Object getClientAtom() {
    return clientAtom;
  }

  public int getAtomIndex() {
    return atomIndex & 0xFFFF;
  }

public boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0; ) {
        Bond bond = bonds[i];
        if ((bond.atom1 == atomOther) ||
            (bond.atom2 == atomOther))
          return true;
      }
    return false;
  }

  public Bond bondMutually(Atom atomOther, int order) {
    if (isBonded(atomOther))
      return null;
    Bond bond = new Bond(this, atomOther, order, frame.viewer);
    addBond(bond);
    atomOther.addBond(bond);
    return bond;
  }

  private void addBond(Bond bond) {
    int i = 0;
    if (bonds == null) {
      bonds = new Bond[1];
    } else {
      i = bonds.length;
      Bond[] bondsNew = new Bond[i + 1];
      System.arraycopy(bonds, 0, bondsNew, 0, i);
      bonds = bondsNew;
    }
    bonds[i] = bond;
  }

  public void deleteBondedAtom(Atom atomToDelete) {
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bond = bonds[i];
      Atom atomBonded =
        (bond.atom1 != this) ? bond.atom1 : bond.atom2;
      if (atomBonded == atomToDelete) {
        deleteBond(i);
        return;
      }
    }
  }

  public void deleteAllBonds() {
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0; )
      frame.deleteBond(bonds[i]);
    if (bonds != null) {
      System.out.println("bond delete error");
      throw new NullPointerException();
    }
  }

  public void deleteBond(Bond bond) {
    for (int i = bonds.length; --i >= 0; )
      if (bonds[i] == bond) {
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
    Bond[] bondsNew = new Bond[newLength];
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
    Bond bond = bonds[bondIndex];
    return (((bond.atom1 == this)
             ? bond.atom2
             : bond.atom1).atomIndex & 0xFFFF);
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
    if (this.styleAtom == JmolConstants.STYLE_DELETED) return;
      this.styleAtom = styleAtom;
  }

  public void setMarAtom(short marAtom) {
    if (this.styleAtom == JmolConstants.STYLE_DELETED) return;
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) * frame.viewer.getVanderwaalsRadius(this));
    this.marAtom = marAtom;
  }

  public void setStyleMarAtom(byte styleAtom, short marAtom) {
    if (this.styleAtom == JmolConstants.STYLE_DELETED) return;
    this.styleAtom = styleAtom;
    if (marAtom < 0)
      marAtom = (short)((-10 * marAtom) * frame.viewer.getVanderwaalsRadius(this));
    this.marAtom = marAtom;
  }
        
  public int getRasMolRadius() {
    if (styleAtom <= JmolConstants.STYLE_NONE)
      return 0;
    return marAtom / 4;
  }

  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & Bond.COVALENT) != 0)
        ++n;
    return n;
  }

  public Bond[] getBonds() {
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
    Point3i screen = viewer.transformPoint(point3f);
    x = screen.x;
    y = screen.y;
    z = screen.z;
    diameter = viewer.scaleToScreen(z, marAtom * 2);
  }

  public int getAtomicNumber() {
    return atomicNumber;
  }

  public String getAtomicSymbol() {
    return frame.viewer.getAtomicSymbol(this);
  }

  public String getAtomTypeName() {
    return frame.viewer.getAtomTypeName(this);
  }

  public int getAtomno() {
    if (pdbatom != null)
      return pdbatom.getAtomNumber();
    return getAtomIndex() +
      (frame.modelType == JmolModelAdapter.MODEL_TYPE_XYZ ? 0 : 1);
  }

  public Point3f getPoint3f() {
    return point3f;
  }

  public float getAtomX() {
    return (float)point3f.x;
  }

  public float getAtomY() {
    return (float)point3f.y;
  }

  public float getAtomZ() {
    return (float)point3f.z;
  }

  public float getDimensionValue(int dimension) {
    return (float)(dimension == 0
		   ? point3f.x
		   : (dimension == 1 ? point3f.y : point3f.z));
  }

  public float getVanderwaalsRadius() {
    return frame.viewer.getVanderwaalsRadius(this);
  }

  public float getCovalentRadius() {
    return frame.viewer.getCovalentRadius(this);
  }

  public short getColix() {
    return colixAtom;
  }

  public float getRadius() {
    if (styleAtom <= JmolConstants.STYLE_NONE)
      return 0;
    float radius = marAtom / 1000f;
    if (styleAtom == JmolConstants.STYLE_WIREFRAME) return -radius;
    return radius;
  }

  public PdbAtom getPdbAtom() {
    return pdbatom;
  }

  public Object markDeleted() {
    deleteAllBonds();
    styleAtom = JmolConstants.STYLE_DELETED;
    Object clientAtom = this.clientAtom;
    this.clientAtom = null;
    return clientAtom;
  }
}
