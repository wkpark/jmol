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
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

public class Atom implements Bspt.Tuple {

  public final static byte VISIBLE_FLAG = 1;

  public int atomIndex;
  Frame frame; // maybe we can get rid of this ...
  PdbGroup group; // ... if everybody has a group
  short modelNumber; // we want this here for the BallsRenderer
  public Point3f point3f;
  short x, y, z;
  short diameter;
  public byte elementNumber;
  byte chargeAndFlags;
  // maybe move this out of here ... the value is almost always 100
  byte occupancy;
  short bfactor100;
  short madAtom;
  short colixAtom;
  Bond[] bonds;

  boolean isHetero; // pack this bit someplace

  /* move these out of here */
  int atomSerial;
  String atomName;
  public short atomID = -1;

  public Atom(Frame frame, int atomIndex,
              int modelNumber,
              byte elementNumber,
              String atomName,
              int atomicCharge,
              int occupancy,
              float bfactor,
              float x, float y, float z,
              boolean isHetero, int atomSerial, char chainID,
              String group3, int sequenceNumber, char insertionCode,
              PdbFile pdbFile) {
    JmolViewer viewer = frame.viewer;
    this.frame = frame;
    this.atomIndex = atomIndex;
    this.modelNumber = (short)modelNumber;
    this.elementNumber = elementNumber;
    this.chargeAndFlags = (byte)(atomicCharge << 4);
    this.occupancy = (occupancy < 0
                      ? (byte)0
                      : (occupancy > 100
                         ? (byte)100
                         : (byte)occupancy));
    this.bfactor100 =
      (bfactor == Float.NaN ? Short.MIN_VALUE : (short)(bfactor*100));
    this.atomName = atomName;
    this.colixAtom = viewer.getColixAtom(this);
    setMadAtom(viewer.getMadAtom());
    this.point3f = new Point3f(x, y, z);
    this.isHetero = isHetero;
    this.atomSerial = atomSerial;
    if (pdbFile != null) {
      atomID = lookupAtomID(atomName);
      group = pdbFile.registerAtom(this, modelNumber, chainID,
                                   sequenceNumber, insertionCode, group3);
    }
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

  public void setMadAtom(short madAtom) {
    if (this.madAtom == JmolConstants.MAR_DELETED) return;
    if (madAtom == -1000) // temperature
      madAtom = (short)(bfactor100 * 10 * 2);
    else if (madAtom == -1001) // ionic
      madAtom = (short)(getBondingMar() * 2);
    else if (madAtom < 0)
      madAtom = // we are going from a radius to a diameter
        (short)(-madAtom * getVanderwaalsMar() / 50);
    this.madAtom = madAtom;
  }

  public int getRasMolRadius() {
    if (madAtom == JmolConstants.MAR_DELETED)
      return 0;
    return madAtom / (4 * 2);
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
    frame.setLabel(strLabel, atomIndex);
  }

  public void transform(JmolViewer viewer) {
    if (madAtom == JmolConstants.MAR_DELETED)
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
    
    diameter = viewer.scaleToScreen(z, madAtom);
  }

  public int getElementNumber() {
    return elementNumber;
  }

  public String getElementSymbol() {
    return JmolConstants.elementSymbols[elementNumber];
  }

  public String getAtomName() {
    if (atomName != null)
      return atomName;
    return JmolConstants.elementSymbols[elementNumber];
  }
  
  public String getPdbAtomName4() {
    return atomName;
  }

  public String getGroup3() {
    return group == null ? null : group.getGroup3();
  }

  public boolean isGroup3(String group3) {
    return group == null ? false : group.isGroup3(group3);
  }

  public boolean isGroup3Match(String strWildcard) {
    return group == null ? false : group.isGroup3Match(strWildcard);
  }

  public int getSeqcode() {
    // I wonder if this should be -infinity instead of -1
    return group == null ? -1 : group.seqcode;
  }

  public boolean isAtomNameMatch(String strPattern) {
    if (atomID < 0)
      return false;
    int cchPattern = strPattern.length();
    if (cchPattern > 4) {
      System.err.println("atom wildcard length > 4 : " + strPattern);
      return false;
    }
    String strAtomName = atomNames[atomID];
    int cchAtomName = strAtomName.length();
    int ichAtomName = 0;
    while (strAtomName.charAt(ichAtomName) == ' ')
      ++ichAtomName;
    for (int i = 0; i < cchPattern; ++i, ++ichAtomName) {
      char charWild = strPattern.charAt(i);
      if (charWild == '?')
        continue;
      if (ichAtomName >= cchAtomName ||
          charWild != Character.toUpperCase(strAtomName.charAt(ichAtomName)))
        return false;
    }
    while (ichAtomName < cchAtomName)
      if (strAtomName.charAt(ichAtomName++) != ' ')
        return false;
    return true;
  }

  public int getAtomNumber() {
    if (atomSerial != Integer.MIN_VALUE)
      return atomSerial;
    if (frame.modelType == JmolConstants.MODEL_TYPE_XYZ &&
        frame.viewer.getZeroBasedXyzRasmol())
      return atomIndex;
    return atomIndex + 1;
  }

  public boolean isHetero() {
    return isHetero;
  }

  public int getAtomicCharge() {
    return chargeAndFlags >> 4;
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
    return JmolConstants.vanderwaalsMars[elementNumber];
  }

  public float getVanderwaalsRadiusFloat() {
    return JmolConstants.vanderwaalsMars[elementNumber] / 1000f;
  }

  public short getBondingMar() {
    return JmolConstants.getBondingMar(elementNumber, chargeAndFlags >> 4);
  }
  
  public float getBondingRadiusFloat() {
    return getBondingMar() / 1000f;
  }

  public short getColix() {
    return colixAtom;
  }

  public float getRadius() {
    if (madAtom == JmolConstants.MAR_DELETED)
      return 0;
    return madAtom / (1000f * 2);
  }

  public int getModelNumber() {
    return modelNumber;
  }

  public char getChainID() {
    return group == null ? (char)0 : group.chain.chainID;
  }

  // a percentage value in the range 0-100
  public int getOccupancy() {
    return occupancy;
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  public int getBfactor100() {
    if (bfactor100 == Short.MIN_VALUE)
      return 0;
    return bfactor100;
  }

  public PdbGroup getPdbGroup() {
    return group;
  }

  public PdbPolymer getPdbPolymer() {
    if (group == null)
      return null;
    return group.polymer;
  }

  public PdbChain getPdbChain() {
    if (group == null)
      return null;
    return group.chain;
  }

  public PdbModel getPdbModel() {
    if (group == null)
      return null;
    return group.chain.pdbmodel;
  }
  
  public String getClientAtomStringProperty(String propertyName) {
    Object[] clientAtomReferences = frame.clientAtomReferences;
    return
      ((clientAtomReferences==null || clientAtomReferences.length<=atomIndex)
       ? null
       : (frame.viewer.
          getClientAtomStringProperty(clientAtomReferences[atomIndex],
                                      propertyName)));
  }

  public boolean isDeleted() {
    return madAtom == JmolConstants.MAR_DELETED;
  }

  public void markDeleted() {
    deleteAllBonds();
    madAtom = JmolConstants.MAR_DELETED;
    x = y = z = diameter = 0;
  }

  public byte getSecondaryStructureType() {
    if (group != null)
      return group.getStructureType();
    return 0;
  }

  public short getGroupID() {
    if (group != null)
      return group.groupID;
    return -1;
  }

  public String getSeqcodeString() {
    if (group != null)
      return group.getSeqcodeString();
    return null;
  }

  public int getModelID() {
    if (group != null)
      return group.chain.pdbmodel.modelNumber;
    return 1;
  }
  
  public short getAtomID() {
    return atomID;
  }
  
  public String getAtomPrettyName() {
    return atomPrettyNames[atomID];
  }

  public final static short atomIDMainchainMax = 3;
  
  private static Hashtable htAtom = new Hashtable();
  public static String[] atomNames = new String[128];
  private static String[] atomPrettyNames = new String[128];
  private static short atomIDMax = 0;
  
  static {
    // this loop *must* run in forward direction;
    for (int i = 0; i < JmolConstants.predefinedAtomNames4.length; ++i)
      addAtomName(JmolConstants.predefinedAtomNames4[i]);
  }
  
  // this requires a 4 letter name, in PDB format
  // only here for transition purposes
  static String calcPrettyName(String name) {
    char chBranch = name.charAt(3);
    char chRemote = name.charAt(2);
    switch (chRemote) {
    case 'A':
      chRemote = '\u03B1';
      break;
    case 'B':
      chRemote = '\u03B2';
      break;
    case 'C':
    case 'G':
      chRemote = '\u03B3';
      break;
    case 'D':
      chRemote = '\u03B4';
      break;
    case 'E':
      chRemote = '\u03B5';
      break;
    case 'Z':
      chRemote = '\u03B6';
      break;
    case 'H':
      chRemote = '\u03B7';
    }
    String pretty = name.substring(0, 2).trim();
    if (chBranch != ' ')
      pretty += "" + chRemote + chBranch;
    else
      pretty += chRemote;
    return pretty;
  }
  
  synchronized static short addAtomName(String name) {
    String prettyName = calcPrettyName(name);
    if (atomIDMax == atomNames.length) {
      String[] t;
      t = new String[atomIDMax * 2];
      System.arraycopy(atomNames, 0, t, 0, atomIDMax);
      atomNames = t;
      t = new String[atomIDMax * 2];
      System.arraycopy(atomPrettyNames, 0, t, 0, atomIDMax);
      atomPrettyNames = t;
    }
    short atomID = atomIDMax++;
    atomNames[atomID] = name;
    atomPrettyNames[atomID] = prettyName;
    // if already exists then this is an imposter entry
    if (htAtom.get(name) == null)
      htAtom.put(name, new Short(atomID));
    return atomID;
  }

  short lookupAtomID(String strAtom) {
    Short boxedAtomID = (Short)htAtom.get(strAtom);
    if (boxedAtomID != null)
      return boxedAtomID.shortValue();
    return addAtomName(strAtom);
  }
}
