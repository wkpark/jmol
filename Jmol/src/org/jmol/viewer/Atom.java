/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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

package org.jmol.viewer;

import org.jmol.g3d.Xyzd;

import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

final class Atom implements Bspt.Tuple {

  final static byte VISIBLE_FLAG = 0x01;
  final static byte VIBRATION_VECTOR_FLAG = 0x02;
  final static byte IS_HETERO_FLAG = 0x04;

  Group group;
  int atomIndex;
  Point3f point3f;
  long xyzd;
  short modelIndex; // we want this here for the BallsRenderer
  byte elementNumber;
  byte formalChargeAndFlags;
  short madAtom;
  short colixAtom;
  Bond[] bonds;

  Atom(Viewer viewer,
       Frame frame,
       int modelIndex,
       int atomIndex,
       byte elementNumber,
       String atomName,
       int formalCharge, float partialCharge,
       int occupancy,
       float bfactor,
       float x, float y, float z,
       boolean isHetero, int atomSerial, char chainID,
       float vibrationX, float vibrationY, float vibrationZ,
       Object clientAtomReference) {
    this.modelIndex = (short)modelIndex;
    this.atomIndex = atomIndex;
    this.elementNumber = elementNumber;
    if (formalCharge == Integer.MIN_VALUE)
      formalCharge = 0;
    this.formalChargeAndFlags = (byte)(formalCharge << 4);
    this.colixAtom = viewer.getColixAtom(this);
    setMadAtom(viewer.getMadAtom());
    this.point3f = new Point3f(x, y, z);
    if (isHetero)
      formalChargeAndFlags |= IS_HETERO_FLAG;

    if (atomName != null) {
      if (frame.atomNames == null)
        frame.atomNames = new String[frame.atoms.length];
      frame.atomNames[atomIndex] = atomName.intern();
    }

    byte specialAtomID = lookupSpecialAtomID(atomName);
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

  void setGroup(Group group) {
    this.group = group;
  }

  boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0; ) {
        Bond bond = bonds[i];
        if ((bond.atom1 == atomOther) ||
            (bond.atom2 == atomOther))
          return true;
      }
    return false;
  }

  Bond bondMutually(Atom atomOther, int order, Viewer viewer) {
    if (isBonded(atomOther))
      return null;
    Bond bond = new Bond(this, atomOther, order, viewer);
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
      bonds = (Bond[])Util.setLength(bonds, i + 1);
    }
    bonds[i] = bond;
  }

  void deleteBondedAtom(Atom atomToDelete) {
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

  void deleteAllBonds() {
    if (bonds == null)
      return;
    for (int i = bonds.length; --i >= 0; )
      group.chain.frame.deleteBond(bonds[i]);
    if (bonds != null) {
      System.out.println("bond delete error");
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

  void setMadAtom(short madAtom) {
    if (this.madAtom == JmolConstants.MAR_DELETED) return;
    if (madAtom == -1000) { // temperature
      int diameter = getBfactor100() * 10 * 2;
      if (diameter > 4000)
        diameter = 4000;
      madAtom = (short)diameter;
    } else if (madAtom == -1001) // ionic
      madAtom = (short)(getBondingMar() * 2);
    else if (madAtom < 0)
      madAtom = // we are going from a radius to a diameter
        (short)(-madAtom * getVanderwaalsMar() / 50);
    this.madAtom = madAtom;
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
      if ((bonds[i].order & JmolConstants.BOND_COVALENT) != 0)
        ++n;
    return n;
  }

  Bond[] getBonds() {
    return bonds;
  }

  void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  Vector3f getVibrationVector() {
    Vector3f[] vibrationVectors = group.chain.frame.vibrationVectors;
    return vibrationVectors == null ? null : vibrationVectors[atomIndex];
  }

  void setLabel(String strLabel) {
    group.chain.frame.setLabel(strLabel, atomIndex);
  }

  final static int MIN_Z = 100;
  final static int MAX_Z = 14383;

  void transform(Viewer viewer) {
    if (madAtom == JmolConstants.MAR_DELETED)
      return;
    Point3i screen;
    Vector3f[] vibrationVectors;
    if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 ||
        (vibrationVectors = group.chain.frame.vibrationVectors) == null)
      screen = viewer.transformPoint(point3f);
    else 
      screen = viewer.transformPoint(point3f, vibrationVectors[atomIndex]);

    int z = screen.z;
    z = ((z < MIN_Z)
         ? MIN_Z
         : ((z > MAX_Z)
            ? MAX_Z
            : z));
    int diameter = viewer.scaleToScreen(z, madAtom);
    xyzd = Xyzd.getXyzd(screen.x, screen.y, z, diameter);
  }

  byte getElementNumber() {
    return elementNumber;
  }

  String getElementSymbol() {
    return JmolConstants.elementSymbols[elementNumber];
  }

  String getAtomNameOrNull() {
    String[] atomNames = group.chain.frame.atomNames;
    return atomNames == null ? null : atomNames[atomIndex];
  }

  String getAtomName() {
    String atomName = getAtomNameOrNull();
    return
      atomName != null ? atomName : JmolConstants.elementSymbols[elementNumber];
  }
  
  String getPdbAtomName4() {
    String atomName = getAtomNameOrNull();
    return atomName != null ? atomName : "";
  }

  String getGroup3() {
    return group.getGroup3();
  }

  boolean isGroup3(String group3) {
    return group.isGroup3(group3);
  }

  boolean isGroup3Match(String strWildcard) {
    return group.isGroup3Match(strWildcard);
  }

  int getSeqcode() {
    return group.seqcode;
  }

  boolean isAtomNameMatch(String strPattern) {
    String atomName = getAtomNameOrNull();
    int cchAtomName = atomName == null ? 0 : atomName.length();
    int cchPattern = strPattern.length();
    int ich;
    for (ich = 0; ich < cchPattern; ++ich) {
      char charWild = strPattern.charAt(ich);
      if (charWild == '?')
        continue;
      if (ich >= cchAtomName ||
          charWild != Character.toUpperCase(atomName.charAt(ich)))
        return false;
    }
    return ich >= cchAtomName;
  }

  int getAtomNumber() {
    int[] atomSerials = group.chain.frame.atomSerials;
    if (atomSerials != null)
      return atomSerials[atomIndex];
    if (group.chain.frame.modelSetTypeName == "xyz" &&
        group.chain.frame.viewer.getZeroBasedXyzRasmol())
      return atomIndex;
    return atomIndex + 1;
  }

  boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  int getFormalCharge() {
    return formalChargeAndFlags >> 4;
  }

  boolean isVisible() {
    return (formalChargeAndFlags & VISIBLE_FLAG) != 0;
  }

  float getPartialCharge() {
    float[] partialCharges = group.chain.frame.partialCharges;
    return partialCharges == null ? 0 : partialCharges[atomIndex];
  }

  Point3f getPoint3f() {
    return point3f;
  }

  float getAtomX() {
    return point3f.x;
  }

  float getAtomY() {
    return point3f.y;
  }

  float getAtomZ() {
    return point3f.z;
  }

  public float getDimensionValue(int dimension) {
    return (dimension == 0
		   ? point3f.x
		   : (dimension == 1 ? point3f.y : point3f.z));
  }

  short getVanderwaalsMar() {
    return JmolConstants.vanderwaalsMars[elementNumber];
  }

  float getVanderwaalsRadiusFloat() {
    return JmolConstants.vanderwaalsMars[elementNumber] / 1000f;
  }

  short getBondingMar() {
    return JmolConstants.getBondingMar(elementNumber,
                                       formalChargeAndFlags >> 4);
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
    float dist2Longest = point3f.distanceSquared(atomChallenger.point3f);
    Bond bondLongest = null;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bond = bonds[i];
      Atom atomOther = bond.atom1 != this ? bond.atom1 : bond.atom2;
      float dist2 = point3f.distanceSquared(atomOther.point3f);
      if (dist2 > dist2Longest) {
        bondLongest = bond;
        dist2Longest = dist2;
      }
    }
    //    System.out.println("atom at " + point3f + " suggests discard of " +
    //                       bondLongest + " dist2=" + dist2Longest);
    return bondLongest;
  }

  float getBondingRadiusFloat() {
    return getBondingMar() / 1000f;
  }

  short getColix() {
    return colixAtom;
  }

  float getRadius() {
    if (madAtom == JmolConstants.MAR_DELETED)
      return 0;
    return madAtom / (1000f * 2);
  }

  char getChainID() {
    return group.chain.chainID;
  }

  // a percentage value in the range 0-100
  int getOccupancy() {
    byte[] occupancies = group.chain.frame.occupancies;
    return occupancies == null ? 100 : occupancies[atomIndex];
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  int getBfactor100() {
    short[] bfactor100s = group.chain.frame.bfactor100s;
    if (bfactor100s == null)
      return 0;
    return bfactor100s[atomIndex];
  }

  Group getGroup() {
    return group;
  }

  int getPolymerLength() {
    return group.getPolymerLength();
  }

  int getPolymerIndex() {
    return group.getPolymerIndex();
  }

  int getAtomIndex() {
    return atomIndex;
  }

  Chain getChain() {
    return group.chain;
  }

  Model getModel() {
    return group.chain.model;
  }

  int getModelIndex() {
    return modelIndex;
  }
  
  String getClientAtomStringProperty(String propertyName) {
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
    xyzd = Xyzd.NaN;
  }

  byte getProteinStructureType() {
    return group.getProteinStructureType();
  }

  short getGroupID() {
    return group.groupID;
  }

  String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  String getModelTag() {
    return group.chain.model.modelTag;
  }

  int getModelTagNumber() {
    try {
      return Integer.parseInt(group.chain.model.modelTag);
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }
  
  byte getSpecialAtomID() {
    byte[] specialAtomIDs = group.chain.frame.specialAtomIDs;
    return specialAtomIDs == null ? 0 : specialAtomIDs[atomIndex];
  }

  void demoteSpecialAtomImposter() {
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
    int cch = strFormat.length();
    int ich, ichPercent;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1; ) {
      if (ich != ichPercent)
        strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      if (ich == cch) {
        --ich; // a percent sign at the end of the string
        break;
      }
      String strT = "";
      char ch = strFormat.charAt(ich++);
      switch (ch) {
      case 'i':
        strT = "" + getAtomNumber();
        break;
      case 'a':
        strT = getAtomName();
        break;
      case 'e':
        strT = JmolConstants.elementSymbols[elementNumber];
        break;
      case 'x':
        strT = "" + point3f.x;
        break;
      case 'y':
        strT = "" + point3f.y;
        break;
      case 'z':
        strT = "" + point3f.z;
        break;
      case 'X':
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
      case 'P':
        strT = "" + getPartialCharge();
        break;
      case 'V':
        strT = "" + getVanderwaalsRadiusFloat();
        break;
      case 'I':
        strT = "" + getBondingRadiusFloat();
        break;
      case 'b': // these two are the same
      case 't':
        strT = "" + (getBfactor100() / 100.0);
        break;
      case 'q':
        strT = "" + getOccupancy();
        break;
      case 'c': // these two are the same
      case 's':
        strT = "" + getChainID();
        break;
      case 'L':
        strT = "" + getPolymerLength();
        break;
      case 'M':
        strT = "/" + getModelTag();
        break;
      case 'm':
        strT = "<X>";
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
      strLabel += strT;
    }
    strLabel += strFormat.substring(ich);
    if (strLabel.length() == 0)
      return null;
    return strLabel.intern();
  }

  String getInfo() {
    return getIdentity();
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
    if (group.chain.frame.getModelCount() > 1) {
      info.append("/");
      info.append(getModelTag());
    }
    info.append(" #");
    info.append(getAtomNumber());
    return "" + info;
  }

  boolean isCursorOnTopOfVisibleAtom(int xCursor, int yCursor,
                                     int minRadius, Atom competitor) {
    return (((formalChargeAndFlags & VISIBLE_FLAG) != 0) &&
            isCursorOnTop(xCursor, yCursor, minRadius, competitor));
  }

  boolean isCursorOnTop(int xCursor, int yCursor,
                        int minRadius, Atom competitor) {
    int r = Xyzd.getD(xyzd) / 2;
    if (r < minRadius)
      r = minRadius;
    int r2 = r * r;
    int dx = Xyzd.getX(xyzd) - xCursor;
    int dx2 = dx * dx;
    if (dx2 > r2)
      return false;
    int dy = Xyzd.getY(xyzd) - yCursor;
    int dy2 = dy * dy;
    int dz2 = r2 - (dx2 + dy2);
    if (dz2 < 0)
      return false;
    if (competitor == null)
      return true;
    int z = Xyzd.getZ(xyzd);
    int zCompetitor = Xyzd.getZ(competitor.xyzd);
    int rCompetitor = Xyzd.getD(competitor.xyzd) / 2;
    if (z < zCompetitor - rCompetitor)
      return true;
    int dxCompetitor = Xyzd.getX(competitor.xyzd) - xCursor;
    int dx2Competitor = dxCompetitor * dxCompetitor;
    int dyCompetitor = Xyzd.getY(competitor.xyzd) - yCursor;
    int dy2Competitor = dyCompetitor * dyCompetitor;
    int r2Competitor = rCompetitor * rCompetitor;
    int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
    return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
  }

  ////////////////////////////////////////////////////////////////
  int getScreenX() { return Xyzd.getX(xyzd); }
  int getScreenY() { return Xyzd.getY(xyzd); }
  int getScreenZ() { return Xyzd.getZ(xyzd); }
  int getScreenD() { return Xyzd.getD(xyzd); }

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
}
