/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$

 *
 * Copyright (C) 2003-2005  The Jmol Development Team
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

import org.jmol.util.Logger;
import org.jmol.vecmath.Point3fi;
import org.jmol.g3d.Graphics3D;
import org.jmol.bspt.Tuple;

import java.util.Hashtable;
import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

final public class Atom extends Point3fi implements Tuple {

  final static byte VIBRATION_VECTOR_FLAG = 0x02;
  final static byte IS_HETERO_FLAG = 0x04;

  Group group;
  int atomIndex;
  BitSet atomSymmetry;
  int atomSite;
  short screenDiameter;
  short modelIndex; // we want this here for the BallsRenderer
  byte elementNumber;
  byte formalChargeAndFlags;
  byte alternateLocationID;
  short madAtom;
  short colixAtom;
  Bond[] bonds;
  int nBondsDisplayed = 0;
  int nBackbonesDisplayed = 0;
  int clickabilityFlags;
  int shapeVisibilityFlags;
  boolean isSimple = false;

  // miguel 2006 03 25
  // we should talk about this. if you need simple "points" for your shapes
  // then we should consider splitting out the 'pointness' from the
  // atom and have atom inherit from point
  Atom(Point3f pt) { 
    //just a point -- just enough to determine a position
    isSimple = true;
    this.x = pt.x; this.y = pt.y; this.z = pt.z;
    //must be transformed later -- Polyhedra;
    formalChargeAndFlags = 0;
    madAtom = 0;
  }
  
  Atom(Viewer viewer,
       Frame frame,
       int modelIndex,
       int atomIndex,
       BitSet atomSymmetry,
       int atomSite,
       byte elementNumber,
       String atomName,
       int formalCharge, float partialCharge,
       int occupancy,
       float bfactor,
       float x, float y, float z,
       boolean isHetero, int atomSerial, char chainID,
       float vibrationX, float vibrationY, float vibrationZ,
       char alternateLocationID,
       Object clientAtomReference) {
    this.modelIndex = (short)modelIndex;
    this.atomSymmetry = atomSymmetry;
    this.atomSite = atomSite;
    this.atomIndex = atomIndex;
    this.elementNumber = elementNumber;
    this.formalChargeAndFlags = (byte)(formalCharge << 3);
    this.colixAtom = viewer.getColixAtom(this);
    this.alternateLocationID = (byte)alternateLocationID;
    setMadAtom(viewer.getMadAtom());
    this.x = x; this.y = y; this.z = z;
    if (isHetero)
      formalChargeAndFlags |= IS_HETERO_FLAG;

    if (atomName != null) {
      if (frame.atomNames == null)
        frame.atomNames = new String[frame.atoms.length];
      frame.atomNames[atomIndex] = atomName.intern();
    }

    byte specialAtomID = lookupSpecialAtomID(atomName);
    //Logger.debug("atom - "+atomName+" specialAtomID=" + specialAtomID);
    if (specialAtomID != 0) {
      if (frame.specialAtomIDs == null)
        frame.specialAtomIDs = new byte[frame.atoms.length];
      frame.specialAtomIDs[atomIndex] = specialAtomID;
    }

    if (occupancy < 0)
      occupancy = 0;
    else if (occupancy > 100)
      occupancy = 100;
    if (occupancy != 100) {
      if (frame.occupancies == null)
        frame.occupancies = new byte[frame.atoms.length];
      frame.occupancies[atomIndex] = (byte)occupancy;
    }

    if (atomSerial != Integer.MIN_VALUE) {
      if (frame.atomSerials == null)
        frame.atomSerials = new int[frame.atoms.length];
      frame.atomSerials[atomIndex] = atomSerial;
    }

    if (! Float.isNaN(partialCharge)) {
      if (frame.partialCharges == null)
        frame.partialCharges = new float[frame.atoms.length];
      frame.partialCharges[atomIndex] = partialCharge;
    }

    if (! Float.isNaN(bfactor) && bfactor != 0) {
      if (frame.bfactor100s == null)
        frame.bfactor100s = new short[frame.atoms.length];
      frame.bfactor100s[atomIndex] = (short)(bfactor * 100);
    }

    if (!Float.isNaN(vibrationX) && !Float.isNaN(vibrationY) &&
        !Float.isNaN(vibrationZ)) {
      if (frame.vibrationVectors == null)
        frame.vibrationVectors = new Vector3f[frame.atoms.length];
      frame.vibrationVectors[atomIndex] = 
        new Vector3f(vibrationX, vibrationY, vibrationZ);
      formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
    }
    if (clientAtomReference != null) {
      if (frame.clientAtomReferences == null)
        frame.clientAtomReferences = new Object[frame.atoms.length];
      frame.clientAtomReferences[atomIndex] = clientAtomReference;
    }
  }

  final void setShapeVisibility(int shapeVisibilityFlag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= shapeVisibilityFlag;        
    } else {
      shapeVisibilityFlags &=~shapeVisibilityFlag;
    }
  }
  
  void setGroup(Group group) {
    this.group = group;
  }

  boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  Bond getBond(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(atomOther) != null)
          return bonds[i];
    return null;
  }

  Bond bondMutually(Atom atomOther, short order, Frame frame) {
    if (isBonded(atomOther))
      return null;
    Bond bond = new Bond(this, atomOther, order, frame);
    addBond(bond, frame);
    atomOther.addBond(bond, frame);
    return bond;
  }

  private void addBond(Bond bond, Frame frame) {
    if (bonds == null) {
      bonds = new Bond[1];
      bonds[0] = bond;
    } else {
      bonds = frame.addToBonds(bond, bonds);
    }
  }

  void addDisplayedBond(int stickVisibilityFlag, boolean isVisible){
    nBondsDisplayed+=(isVisible ? 1 : -1);
    setShapeVisibility(stickVisibilityFlag, isVisible);
  } 
  
  void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible){
    nBackbonesDisplayed+=(isVisible ? 1 : -1);
    setShapeVisibility(backboneVisibilityFlag, isVisible);
  }
  
  void deleteBondedAtom(Atom atomToDelete) {
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0;)
      if (bonds[i].getOtherAtom(this) == atomToDelete) {
        deleteBond(i);
        return;
      }
  }

  void deleteAllBonds() {
    if (bonds == null)
      return;
    if (group == null)
      return;
    for (int i = bonds.length; --i >= 0; )
      group.chain.frame.deleteBond(bonds[i]);
    if (bonds != null) {
      Logger.error("bond delete error");
      throw new NullPointerException();
    }
  }

  void deleteBond(Bond bond) {
    for (int i = bonds.length; --i >= 0; )
      if (bonds[i] == bond) {
        deleteBond(i);
        return;
      }
  }

  void deleteBond(int i) {
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

  void clearBonds() {
    bonds = null;
  }

  int getBondedAtomIndex(int bondIndex) {
    return bonds[bondIndex].getOtherAtom(this).atomIndex;
  }

  /*
   * What is a MAR?
   *  - just a term that Miguel made up
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

  void setMadAtom(short madAtom) {
    if (this.madAtom == JmolConstants.MAR_DELETED) return;
    this.madAtom = convertEncodedMad(madAtom);
  }

  short convertEncodedMad(int size) {
    if (size == 0)
      return 0;
    if (size == -1000) { // temperature
      int diameter = getBfactor100() * 10 * 2;
      if (diameter > 4000)
        diameter = 4000;
      size = diameter;
    } else if (size == -1001) // ionic
      size = (getBondingMar() * 2);
    else if (size < 0) {
      size = -size;
      if (size > 200)
        size = 200;
      size = // we are going from a radius to a diameter
        (int)(size / 100f * getVanderwaalsMar() * 2);
    } else if (size >= 10000) {
      // radiusAngstroms = vdw + x, where size = (x*2)*1000 + 10000
      // and vdwMar = vdw * 1000
      // we want mad = diameterAngstroms * 1000 = (radiusAngstroms *2)*1000 
      //             = (vdw * 2 * 1000) + x * 2 * 1000
      //             = vdwMar * 2 + (size - 10000)
      size = size - 10000 + getVanderwaalsMar() * 2;
    }
    return (short)size;
  }

  int getRasMolRadius() {
    if (madAtom == JmolConstants.MAR_DELETED)
      return 0;
    return madAtom / (4 * 2);
  }

  int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0)
        ++n;
    return n;
  }

  int getCovalentHydrogenCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; )
      if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0
          && bonds[i].getOtherAtom(this).elementNumber == 1)
        ++n;
    return n;
  }

  Bond[] getBonds() {
    return bonds;
  }

  void setColixAtom(short colixAtom) {
    if (colixAtom == 0)
      colixAtom = group.chain.frame.viewer.getColixAtomPalette(this, "cpk");
    this.colixAtom = colixAtom;
  }

  void setTranslucent(boolean isTranslucent) {
    colixAtom = Graphics3D.setTranslucent(colixAtom, isTranslucent);
  }

  boolean isTranslucent() {
    return Graphics3D.isColixTranslucent(colixAtom);
  }
  
  Vector3f getVibrationVector() {
    if (group == null)
      return null;
    Vector3f[] vibrationVectors = group.chain.frame.vibrationVectors;
    return vibrationVectors == null ? null : vibrationVectors[atomIndex];
  }

  void setLabel(String strLabel) {
    if (group == null)
      return;
    group.chain.frame.setLabel(strLabel, atomIndex);
  }

  // miguel 2006 03 25
  // not sure what we should do here
  // current implementation of g3d uses a short for the zbuffer coordinate
  // we could consider turning that into an int, but that would have
  // significant implications
  //
  // actually, I think that it might work out just fine. we should use
  // an int in this world, but let g3d deal with the problem of
  // something having a depth that is more than 32K ... in the same
  // sense that g3d will clip if something is not on the screen

  //  final static int MIN_Z = 100;
  //  final static int MAX_Z = 32766;

  void transform(Viewer viewer) {
    Point3i screen;
    Vector3f[] vibrationVectors;
    if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 ||
        (vibrationVectors = group.chain.frame.vibrationVectors) == null)
      screen = viewer.transformPoint(this);
    else 
      screen = viewer.transformPoint(this, vibrationVectors[atomIndex]);
    screenX = screen.x;
    screenY = screen.y;
    screenZ = screen.z;
    screenDiameter = viewer.scaleToScreen(screenZ, madAtom);
  }

  byte getElementNumber() {
    return elementNumber;
  }

  String getElementSymbol() {
    return JmolConstants.elementSymbols[elementNumber];
  }

  String getAtomNameOrNull() {
    if (group == null)
      return null;
    String[] atomNames = group.chain.frame.atomNames;
    return atomNames == null ? null : atomNames[atomIndex];
  }

  String getAtomName() {
    String atomName = getAtomNameOrNull();
    return (atomName != null
            ? atomName : JmolConstants.elementSymbols[elementNumber]);
  }
  
  String getPdbAtomName4() {
    String atomName = getAtomNameOrNull();
    return atomName != null ? atomName : "";
  }

  String getGroup3() {
    if (group == null)
      return null;
    return group.getGroup3();
  }

  String getGroup1() {
    if (group == null)
      return null;
    return group.getGroup1();
  }

  boolean isGroup3(String group3) {
    if (group == null)
      return false;
    return group.isGroup3(group3);
  }

  boolean isGroup3Match(String strWildcard) {
    if (group == null)
      return false;
    return group.isGroup3Match(strWildcard);
  }

  int getSeqcode() {
    if (group == null)
      return -1;
    return group.getSeqcode();
  }

  int getResno() {
    if (group == null)
      return -1;
    return group.getResno();   
  }
  
  boolean isAtomNameMatch(String strPattern) {
    String atomName = getAtomNameOrNull();
    int cchAtomName = atomName == null ? 0 : atomName.length();
    int cchPattern = strPattern.length();
    int ich;
    for (ich = 0; ich < cchPattern; ++ich) {
      char charWild = Character.toUpperCase(strPattern.charAt(ich));
      if (charWild == '?')
        continue;
      if (ich >= cchAtomName ||
          charWild != Character.toUpperCase(atomName.charAt(ich)))
        return false;
    }
    return ich >= cchAtomName;
  }

  boolean isAlternateLocationMatch(String strPattern) {
    if (strPattern == null)
      return (alternateLocationID == 0);
    if (strPattern.length() != 1)
      return false;
    char ch = strPattern.charAt(0);
    return (ch == '*' 
        || ch == '?' && alternateLocationID != '\0' 
        || alternateLocationID == ch);
  }

  int getAtomNumber() {
    if (group == null)
      return 1;
    int[] atomSerials = group.chain.frame.atomSerials;
    if (atomSerials != null)
      return atomSerials[atomIndex];
    if (group.chain.frame.isZeroBased)
      return atomIndex;
    return atomIndex + 1;
  }

  boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  int getFormalCharge() {
    return formalChargeAndFlags >> 3;
  }

  boolean isClickable() {
    if (!isModelVisible())
      return false;
    int flags = shapeVisibilityFlags;
    if (group != null) flags |= group.shapeVisibilityFlags;
    return ((flags & clickabilityFlags) != 0);
  }

  boolean isVisible() {
    if (!isModelVisible())
      return false;
    int flags = shapeVisibilityFlags;
    if (group != null)
      flags |= group.shapeVisibilityFlags;
    // in model AND something to show for it or its group is visible
    return ((flags & ~JmolConstants.ATOM_IN_MODEL) != 0);
  }

  boolean isModelVisible() {
    return ((shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) != 0);
  }

  boolean isShapeVisible(int shapeVisibilityFlag) {
    return (isModelVisible() 
        && (shapeVisibilityFlags & shapeVisibilityFlag) != 0);
  }

  float getPartialCharge() {
    if (group == null)
      return 0;
    float[] partialCharges = group.chain.frame.partialCharges;
    return partialCharges == null ? 0 : partialCharges[atomIndex];
  }

  Point3f getPoint3f() {
    return this;
  }

  float getAtomX() {
    return x;
  }

  float getAtomY() {
    return y;
  }

  float getAtomZ() {
    return z;
  }

  public float getDimensionValue(int dimension) {
    return (dimension == 0
       ? x
       : (dimension == 1 ? y : z));
  }

  short getVanderwaalsMar() {
    return JmolConstants.vanderwaalsMars[elementNumber];
  }

  float getVanderwaalsRadiusFloat() {
    return JmolConstants.vanderwaalsMars[elementNumber] / 1000f;
  }

  short getBondingMar() {
    return JmolConstants.getBondingMar(elementNumber,
                                       formalChargeAndFlags >> 3);
  }

  float getBondingRadiusFloat() {
    return getBondingMar() / 1000f;
  }

  int getCurrentBondCount() {
    return bonds == null ? 0 : bonds.length;
    /*
    int currentBondCount = 0;
    for (int i = (bonds == null ? 0 : bonds.length); --i >= 0; )
      currentBondCount += bonds[i].order & JmolConstants.BOND_COVALENT;
    return currentBondCount;
    */
  }

  // find the longest bond to discard
  // but return null if atomChallenger is longer than any
  // established bonds
  // note that this algorithm works when maximum valence == 0
  Bond getLongestBondToDiscard(Atom atomChallenger) {
    float dist2Longest = distanceSquared(atomChallenger);
    Bond bondLongest = null;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bond = bonds[i];
      float dist2 = distanceSquared(bond.getOtherAtom(this));
      if (dist2 > dist2Longest) {
        bondLongest = bond;
        dist2Longest = dist2;
      }
    }
    //Logger.debug("atom at " + point3f + " suggests discard of " +
    //                       bondLongest + " dist2=" + dist2Longest);
    return bondLongest;
  }

  short getColix() {
    return colixAtom;
  }

  int getArgb() {
    return group.chain.frame.viewer.getColixArgb(colixAtom);
  }

  float getRadius() {
    if (madAtom == JmolConstants.MAR_DELETED)
      return 0;
    return madAtom / (1000f * 2);
  }

  char getChainID() {
    if (group == null)
      return 0;
    return group.chain.chainID;
  }

  // a percentage value in the range 0-100
  int getOccupancy() {
    if (group == null)
      return 100;
    byte[] occupancies = group.chain.frame.occupancies;
    return occupancies == null ? 100 : occupancies[atomIndex];
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  int getBfactor100() {
    if (group == null)
      return 0;
    short[] bfactor100s = group.chain.frame.bfactor100s;
    if (bfactor100s == null)
      return 0;
    return bfactor100s[atomIndex];
  }

  Group getGroup() {
    return group;
  }

  int getPolymerLength() {
    if (group == null)
      return 0;
    return group.getPolymerLength();
  }

  int getPolymerIndex() {
    if (group == null)
      return -1;
    return group.getPolymerIndex();
  }

  int getSelectedGroupCountWithinChain() {
    if (group == null)
      return 0;
    return group.chain.getSelectedGroupCount();
  }

  int getSelectedGroupIndexWithinChain() {
    if (group == null)
      return -1;
    return group.chain.getSelectedGroupIndex(group);
  }

  int getSelectedMonomerCountWithinPolymer() {
    if (group instanceof Monomer) {
      return ((Monomer)group).polymer.selectedMonomerCount;
    }
    return 0;
  }

  int getSelectedMonomerIndexWithinPolymer() {
    if (group instanceof Monomer) {
      Monomer monomer = (Monomer) group;
      return monomer.polymer.getSelectedMonomerIndex(monomer);
    }
    return -1;
  }

  int getAtomIndex() {
    return atomIndex;
  }

  int getAtomSite() {
    return atomSite;
  }

  BitSet getAtomSymmetry() {
    return atomSymmetry;
  }

  String getSymmetryOperatorList() {
    String str = "";
    if (atomSymmetry == null)
      return str;
    for (int i = 0; i < atomSymmetry.size(); i++)
      if (atomSymmetry.get(i))
        str += "," + (i + 1);
    return str.substring(1);
  }
  
  Chain getChain() {
    if (group == null)
      return null;
    return group.chain;
  }

  Model getModel() {
    if (group == null)
      return null;
    return group.chain.model;
  }

  int getModelIndex() {
    return modelIndex;
  }
  
  int getModelNumber() {
    return modelIndex + 1;
  }
  
  String getClientAtomStringProperty(String propertyName) {
    if (group == null)
      return null;
    Object[] clientAtomReferences = group.chain.frame.clientAtomReferences;
    return
      ((clientAtomReferences==null || clientAtomReferences.length<=atomIndex)
       ? null
       : (group.chain.frame.viewer.
          getClientAtomStringProperty(clientAtomReferences[atomIndex],
                                      propertyName)));
  }

  boolean isDeleted() {
    return madAtom == JmolConstants.MAR_DELETED;
  }

  void markDeleted() {
    deleteAllBonds();
    madAtom = JmolConstants.MAR_DELETED;
  }

  byte getProteinStructureType() {
    if (group == null)
      return 0;
    return group.getProteinStructureType();
  }

  short getGroupID() {
    if (group == null)
      return -1;
    return group.groupID;
  }

  String getSeqcodeString() {
    if (group == null)
      return null;
    return group.getSeqcodeString();
  }

  char getInsertionCode() {
    if (group == null)
      return '\0';
    return group.getInsertionCode();
  }

  String getModelTag() {
    if (group == null)
      return null;
    return group.chain.model.modelTag;
  }

  int getModelTagNumber() {
    if (group == null)
      return 0;
    if (group.chain.model.isPDB) { 
      try {
        return Integer.parseInt(group.chain.model.modelTag);
      } finally {}
    }  
    return getModelNumber();
  }
  
  byte getSpecialAtomID() {
    if (group == null)
      return 0;
    byte[] specialAtomIDs = group.chain.frame.specialAtomIDs;
    return specialAtomIDs == null ? 0 : specialAtomIDs[atomIndex];
  }

  void demoteSpecialAtomImposter() {
    if (group == null)
      return;
    group.chain.frame.specialAtomIDs[atomIndex] = 0;
  }
  
  /* ***************************************************************
   * disabled until I figure out how to generate pretty names
   * without breaking inorganic compounds

  // this requires a 4 letter name, in PDB format
  // only here for transition purposes
  static String calcPrettyName(String name) {
    if (name.length() < 4)
      return name;
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
  */
  
  private static Hashtable htAtom = new Hashtable();
  static {
    for (int i = JmolConstants.specialAtomNames.length; --i >= 0; ) {
      String specialAtomName = JmolConstants.specialAtomNames[i];
      if (specialAtomName != null) {
        Integer boxedI = new Integer(i);
        htAtom.put(specialAtomName, boxedI);
      }
    }
  }

  static String generateStarredAtomName(String primedAtomName) {
    int primeIndex = primedAtomName.indexOf('\'');
    if (primeIndex < 0)
      return null;
    return primedAtomName.replace('\'', '*');
  }

  static String generatePrimeAtomName(String starredAtomName) {
    int starIndex = starredAtomName.indexOf('*');
    if (starIndex < 0)
      return starredAtomName;
    return starredAtomName.replace('*', '\'');
  }

  byte lookupSpecialAtomID(String atomName) {
    if (atomName != null) {
      atomName = generatePrimeAtomName(atomName);
      Integer boxedAtomID = (Integer)htAtom.get(atomName);
      if (boxedAtomID != null)
        return (byte)(boxedAtomID.intValue());
    }
    return 0;
  }

  String formatLabel(String strFormat) {
    if (strFormat == null || strFormat.equals(""))
      return null;
    String strLabel = "";
    //int cch = strFormat.length();
    int ich, ichPercent;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1;) {
      if (ich != ichPercent)
        strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      try {
        String strT = "";
        float floatT = 0;
        boolean floatIsSet = false;
        boolean alignLeft = false;
        if (strFormat.charAt(ich) == '-') {
          alignLeft = true;
          ++ich;
        }
        boolean zeroPad = false;
        if (strFormat.charAt(ich) == '0') {
          zeroPad = true;
          ++ich;
        }
        char ch;
        int width = 0;
        while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
          width = (10 * width) + (ch - '0');
          ++ich;
        }
        int precision = -1;
        if (strFormat.charAt(ich) == '.') {
          ++ich;
          if ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
            precision = ch - '0';
            ++ich;
          }
        }
        /*
         * the list:
         * 
         *      case '%':
         case '{':
         case 'A': alternate location identifier
         case 'a': atom name
         case 'b': temperature factor ("b factor")
         case 'C': formal Charge
         case 'c': chain
         case 'D': atom inDex (was "X")
         case 'e': element symbol
         case 'i': atom number
         case 'I': Ionic radius
         case 'L': polymer Length
         case 'm': group1
         case 'M': Model number
         case 'n': group3
         case 'N': molecule Number
         case 'o': symmetry operator set
         case 'P': Partial charge
         case 'q': occupancy
         case 'r': residue sequence code
         case 's': chain
         case 't': temperature factor
         case 'U': identity
         case 'V': Van der Waals
         case 'x': x coord
         case 'X': fractional X coord
         case 'y': y coord
         case 'Y': fractional Y coord
         case 'z': z coord
         case 'Z': fractional Z coord

         */
        switch (ch = strFormat.charAt(ich++)) {
        case 'i':
          strT = "" + getAtomNumber();
          break;
        case 'A':
          strT = (alternateLocationID != 0 ? ((char) alternateLocationID) + ""
              : "");
          break;
        case 'a':
          strT = getAtomName();
          break;
        case 'e':
          strT = JmolConstants.elementSymbols[elementNumber];
          break;
        case 'x':
          floatT = x;
          floatIsSet = true;
          break;
        case 'y':
          floatT = y;
          floatIsSet = true;
          break;
        case 'z':
          floatT = z;
          floatIsSet = true;
          break;
        case 'X':
        case 'Y':
        case 'Z':
          floatT = getFractionalCoord(ch);
          floatIsSet = true;
          break;
        case 'D':
          strT = "" + atomIndex;
          break;
        case 'C':
          int formalCharge = getFormalCharge();
          if (formalCharge > 0)
            strT = "" + formalCharge + "+";
          else if (formalCharge < 0)
            strT = "" + -formalCharge + "-";
          else
            strT = "0";
          break;
        case 'o':
          strT = getSymmetryOperatorList();
          break;
        case 'P':
          floatT = getPartialCharge();
          floatIsSet = true;
          break;
        case 'V':
          floatT = getVanderwaalsRadiusFloat();
          floatIsSet = true;
          break;
        case 'I':
          floatT = getBondingRadiusFloat();
          floatIsSet = true;
          break;
        case 'b': // these two are the same
        case 't':
          floatT = getBfactor100() / 100f;
          floatIsSet = true;
          break;
        case 'q':
          strT = "" + getOccupancy();
          break;
        case 'c': // these two are the same
        case 's':
          strT = "" + getChainID();
          break;
        case 'S':
          strT = "" + atomSite;
          break;
        case 'L':
          strT = "" + getPolymerLength();
          break;
        case 'M':
          strT = "" + getModelTagNumber();
          break;
        case 'm':
          strT = getGroup1();
          break;
        case 'n':
          strT = getGroup3();
          break;
        case 'r':
          strT = getSeqcodeString();
          break;
        case 'U':
          strT = getIdentity();
          break;
        case 'N':
          strT = "" + getMoleculeNumber();
          break;
        case '%':
          strT = "%";
          break;
        case '{': // client property name
          int ichCloseBracket = strFormat.indexOf('}', ich);
          if (ichCloseBracket > ich) { // also picks up -1 when no '}' is found
            String propertyName = strFormat.substring(ich, ichCloseBracket);
            String value = getClientAtomStringProperty(propertyName);
            if (value != null)
              strT = value;
            ich = ichCloseBracket + 1;
            break;
          }
        // malformed will fall into
        default:
          strT = "%" + ch;
        }
        if (floatIsSet) {
          strLabel += format(floatT, width, precision, alignLeft, zeroPad);
        } else if (strT != null) {
          strLabel += format(strT, width, precision, alignLeft, zeroPad);
        }
      } catch (IndexOutOfBoundsException ioobe) {
        ich = ichPercent;
        break;
      }
    }
    strLabel += strFormat.substring(ich);
    if (strLabel.length() == 0)
      return null;
    return strLabel.intern();
  }

  String format(float value, int width, int precision,
                boolean alignLeft, boolean zeroPad) {
    return format(group.chain.frame.viewer.formatDecimal(value, precision),
                  width, 0, alignLeft, zeroPad);
  }

  static String format(String value, int width, int precision,
                       boolean alignLeft, boolean zeroPad) {
    if (value == null)
      return "";
    if (precision > value.length())
      value = value.substring(0, precision);
    int padLength = width - value.length();
    if (padLength <= 0)
      return value;
    boolean isNeg = (zeroPad && !alignLeft && value.charAt(0) == '-');
    char padChar = (zeroPad ? '0' : ' ');
    char padChar0 = (isNeg ? '-' : padChar);

    StringBuffer sb = new StringBuffer();
    if (alignLeft)
      sb.append(value);
    sb.append(padChar0);
    for (int i = padLength; --i > 0;)
      sb.append(padChar);
    if (!alignLeft)
      sb.append(isNeg ? padChar + value.substring(1) : value);
    return "" + sb;
  }

  private float getFractionalCoord(char ch) {
     Point3f pt = getFractionalCoord();
     return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }
    
  Point3f getFractionalCoord() {
    Frame.CellInfo[] c = group.chain.frame.cellInfos;
    if (c == null)
      return this;
    Point3f pt = new Point3f(this);
    c[modelIndex].toFractional(pt);
    return pt;
  }
  
  boolean isInLatticeCell(Point3f cell) {
   return isInLatticeCell(cell, 0.02f);
  }

  boolean isInLatticeCell(Point3f cell, float slop) {
    Point3f pt = getFractionalCoord();
    // {1 1 1} here is the original cell
    if (pt.x < cell.x - 1f - slop || pt.x > cell.x + slop)
      return false;
    if (pt.y < cell.y - 1f - slop || pt.y > cell.y + slop)
      return false;
    if (pt.z < cell.z - 1f - slop || pt.z > cell.z + slop)
      return false;
    return true;
  }

  String getInfo() {
    return getIdentity();
  }

  String getInfoXYZ() {
    return getIdentity() + " " + x + " " + y + " " + z;
  }

  String getIdentity() {
    StringBuffer info = new StringBuffer();
    String group3 = getGroup3();
    String seqcodeString = getSeqcodeString();
    char chainID = getChainID();
    if (group3 != null && group3.length() > 0) {
      info.append("[");
      info.append(group3);
      info.append("]");
    }
    if (seqcodeString != null)
      info.append(seqcodeString);
    if (chainID != 0 && chainID != ' ') {
      info.append(":");
      info.append(chainID);
    }
    String atomName = getAtomNameOrNull();
    if (atomName != null) {
      if (info.length() > 0)
        info.append(".");
      info.append(atomName);
    }
    if (info.length() == 0) {
      info.append(getElementSymbol());
      info.append(" ");
      info.append(getAtomNumber());
    }
    if (alternateLocationID > 0) {
      info.append("%");
      info.append((char) alternateLocationID);
    }
    if (group != null && group.chain != null 
        && group.chain.frame.getModelCount() > 1) {
      info.append("/");
      info.append(getModelTagNumber());
    }
    info.append(" #");
    info.append(getAtomNumber());
    return "" + info;
  }

  int getMoleculeNumber() {
    return (group.chain.frame.getMoleculeIndex(atomIndex)+1);
  }
  
  boolean isCursorOnTopOfClickableAtom(int xCursor, int yCursor,
                                     int minRadius, Atom competitor) {
    return (isClickable() &&
            isCursorOnTop(xCursor, yCursor, minRadius, competitor));
  }

  boolean isCursorOnTop(int xCursor, int yCursor,
                        int minRadius, Atom competitor) {
    int r = screenDiameter / 2;
    if (r < minRadius)
      r = minRadius;
    int r2 = r * r;
    int dx = screenX - xCursor;
    int dx2 = dx * dx;
    if (dx2 > r2)
      return false;
    int dy = screenY - yCursor;
    int dy2 = dy * dy;
    int dz2 = r2 - (dx2 + dy2);
    if (dz2 < 0)
      return false;
    if (competitor == null)
      return true;
    int z = screenZ;
    int zCompetitor = competitor.screenZ;
    int rCompetitor = competitor.screenDiameter / 2;
    if (z < zCompetitor - rCompetitor)
      return true;
    int dxCompetitor = competitor.screenX - xCursor;
    int dx2Competitor = dxCompetitor * dxCompetitor;
    int dyCompetitor = competitor.screenY - yCursor;
    int dy2Competitor = dyCompetitor * dyCompetitor;
    int r2Competitor = rCompetitor * rCompetitor;
    int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
    return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
  }

  ////////////////////////////////////////////////////////////////
  int getScreenX() { return screenX; }
  int getScreenY() { return screenY; }
  int getScreenZ() { return screenZ; }
  int getScreenD() { return screenDiameter; }

  ////////////////////////////////////////////////////////////////

  boolean isProtein() {
    return group.isProtein();
  }

  boolean isNucleic() {
    return group.isNucleic();
  }

  boolean isDna() {
    return group.isDna();
  }
  
  boolean isRna() {
    return group.isRna();
  }

  boolean isPurine() {
    return group.isPurine();
  }

  boolean isPyrimidine() {
    return group.isPyrimidine();
  }

  ////////////////////////////////////////////////////////////////

  Hashtable getPublicProperties() {
    Hashtable ht = new Hashtable();
    ht.put("element", getElementSymbol());
    ht.put("x", new Double(x));
    ht.put("y", new Double(y));
    ht.put("z", new Double(z));
    ht.put("atomIndex", new Integer(atomIndex));
    ht.put("modelIndex", new Integer(modelIndex));
    ht.put("argb", new Integer(getArgb()));
    ht.put("radius", new Double(getRadius()));
    ht.put("atomNumber", new Integer(getAtomNumber()));
    return ht;
  }
}
