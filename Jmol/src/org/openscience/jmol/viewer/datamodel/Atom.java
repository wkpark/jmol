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
import org.openscience.jmol.viewer.g3d.*;
import org.openscience.jmol.viewer.pdb.*;

import java.awt.Rectangle;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public class Atom implements Bspt.Tuple {

  public final static byte VISIBLE_FLAG = 1;

  public int atomIndex;
  public PdbAtom pdbAtom;
  short modelNumber;
  Frame frame;
  public Point3f point3f;
  short x, y, z;
  short diameter;
  public byte atomicNumber;
  byte chargeAndFlags;
  short marAtom;
  short colixAtom;
  Bond[] bonds;

  /* move these out of here */
  String atomTypeName;
  String strLabel;

  public Atom(Frame frame, int atomIndex,
              int modelNumber, 
              byte atomicNumber, int atomicCharge, String atomTypeName,
              float x, float y, float z,
              PdbFile pdbFile, String pdbAtomRecord) {
    JmolViewer viewer = frame.viewer;
    this.frame = frame;
    this.atomIndex = atomIndex;
    this.modelNumber = (short)modelNumber;
    this.atomicNumber = atomicNumber;
    this.chargeAndFlags = (byte)(atomicCharge << 4);
    this.atomTypeName = atomTypeName;
    this.colixAtom = viewer.getColixAtom(this);
    setMarAtom(viewer.getMarAtom());
    this.point3f = new Point3f(x, y, z);
    if (pdbFile != null)
      pdbAtom =
        pdbFile.allocatePdbAtom(atomIndex, modelNumber, pdbAtomRecord);
    this.strLabel = viewer.getLabelAtom(this, atomIndex);
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

  public void setMarAtom(short marAtom) {
    if (this.marAtom == JmolConstants.MAR_DELETED) return;
    if (marAtom == -1000) // temperature
      marAtom = getPdbTemperatureMar();
    else if (marAtom == -1001) // ionic
      marAtom = getBondingMar();
    else if (marAtom < 0)
      marAtom =
        (short)(-marAtom * getVanderwaalsMar() / 100);
    this.marAtom = marAtom;
  }

  public int getRasMolRadius() {
    if (marAtom == JmolConstants.MAR_DELETED)
      return 0;
    return marAtom / 4;
  }

  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & JmolConstants.BOND_COVALENT) != 0)
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
    if (marAtom == JmolConstants.MAR_DELETED)
      return;
    Point3i screen = viewer.transformPoint(point3f);
    int t;

    t = screen.x;
    x = ((t < Short.MIN_VALUE)
         ? Short.MIN_VALUE
         : ((t > Short.MAX_VALUE)
            ? Short.MAX_VALUE
            : (short)t));

    t = screen.y;
    y = ((t < Short.MIN_VALUE)
         ? Short.MIN_VALUE
         : ((t > Short.MAX_VALUE)
            ? Short.MAX_VALUE
            : (short)t));

    t = screen.z;
    z = ((t < Short.MIN_VALUE)
         ? Short.MIN_VALUE
         : ((t > Short.MAX_VALUE)
            ? Short.MAX_VALUE
            : (short)t));
    
    diameter = viewer.scaleToScreen(z, marAtom * 2);
  }

  public int getAtomicNumber() {
    return atomicNumber;
  }

  public String getAtomicSymbol() {
    return JmolConstants.atomicSymbols[atomicNumber];
  }

  public String getAtomTypeName() {
    if (atomTypeName != null)
      return atomTypeName;
    if (pdbAtom != null)
      return pdbAtom.getAtomPrettyName();
    return JmolConstants.atomicSymbols[atomicNumber];
  }

  public int getAtomicCharge() {
    return chargeAndFlags >> 4;
  }

  public int getAtomno() {
    if (pdbAtom != null)
      return pdbAtom.getAtomSerial();
    if (frame.modelType == JmolConstants.MODEL_TYPE_XYZ &&
        frame.viewer.getZeroBasedXyzRasmol())
      return atomIndex;
    return atomIndex + 1;
  }

  public Point3f getPoint3f() {
    return point3f;
  }

  public float getAtomX() {
    return point3f.x;
  }

  public float getAtomY() {
    return point3f.y;
  }

  public float getAtomZ() {
    return point3f.z;
  }

  public float getDimensionValue(int dimension) {
    return (dimension == 0
		   ? point3f.x
		   : (dimension == 1 ? point3f.y : point3f.z));
  }

  // FIXME mth 2003-01-10
  // store the vdw & covalent mars in the atom when the atom is created
  // then you can eliminate all the calls involving the model manager
  public short getVanderwaalsMar() {
    return JmolConstants.vanderwaalsMars[atomicNumber];
  }

  public float getVanderwaalsRadiusFloat() {
    return JmolConstants.vanderwaalsMars[atomicNumber] / 1000f;
  }

  public short getBondingMar() {
    return JmolConstants.getBondingMar(atomicNumber, chargeAndFlags >> 4);
  }

  public float getBondingRadiusFloat() {
    return getBondingMar() / 1000f;
  }

  public short getColix() {
    return colixAtom;
  }

  public float getRadius() {
    if (marAtom == JmolConstants.MAR_DELETED)
      return 0;
    return marAtom / 1000f;
  }

  public int getModelNumber() {
    return modelNumber;
  }

  public char getChainID() {
    if (pdbAtom == null)
      return (char)0;
    return pdbAtom.getChainID();
  }

  public PdbAtom getPdbAtom() {
    return pdbAtom;
  }

  public PdbGroup getPdbGroup() {
    if (pdbAtom == null)
      return null;
    return pdbAtom.group;
  }

  public PdbPolymer getPdbPolymer() {
    if (pdbAtom == null)
      return null;
    return pdbAtom.group.polymer;
  }

  public PdbChain getPdbChain() {
    if (pdbAtom == null)
      return null;
    return pdbAtom.group.chain;
  }

  public PdbModel getPdbModel() {
    if (pdbAtom == null)
      return null;
    return pdbAtom.group.chain.pdbmodel;
  }

  short getPdbTemperatureMar() {
    // some .pdb files have a radius (measured in angstroms) stored in the
    // TEMPERATURE field of the .pdb file format
    if (pdbAtom == null)
      return 0;
    // the temperature field is in 100th, but we want *milli* angstroms
    return (short)(pdbAtom.temperature * 10);
  }

  public void markDeleted() {
    deleteAllBonds();
    marAtom = JmolConstants.MAR_DELETED;
    x = y = z = diameter = 0;
  }
}
