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
import org.jmol.g3d.Xyzd;

import java.awt.Rectangle;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

public final class Atom implements Bspt.Tuple {

  public final static byte VISIBLE_FLAG = 0x01;

  public int atomIndex;
  Frame frame; // maybe we can get rid of this ...
  Group group; // ... if everybody has a group
  short modelNumber; // we want this here for the BallsRenderer
  public Point3f point3f;
  long xyzd;
  //  private short x, y, z;
  //  private short diameter;
  public byte elementNumber;
  byte formalChargeAndFlags;
  // maybe move this out of here ... the value is almost always 100
  byte occupancy;
  Vector3f vibrationVector;
  short bfactor100;
  short madAtom;
  short colixAtom;
  Bond[] bonds;

  boolean isHetero; // pack this bit someplace

  /* move these out of here */
  int atomSerial;
  public String atomName;
  byte specialAtomID;
  float partialCharge;

  public Atom(Frame frame, int atomIndex,
              int modelNumber,
              byte elementNumber,
              String atomName,
              int formalCharge, float partialCharge,
              int occupancy,
              float bfactor,
              float x, float y, float z,
              boolean isHetero, int atomSerial, char chainID,
              String group3, int sequenceNumber, char insertionCode,
              float vibrationX, float vibrationY, float vibrationZ,
              PdbFile pdbFile) {
    /*
    System.out.println("new Atom(" + modelNumber + "," +
                       elementNumber + "," +
                       atomName + "," +
                       atomicCharge + "," +
                       occupancy + "," +
                       bfactor + "," +
                       x + "," + y + "," + z + "," +
                       isHetero + "," + atomSerial + "," + chainID + "," +
                       group3 + "," + sequenceNumber + ","
                       + insertionCode + "," + pdbFile);
    */
    ////////////////////////////////////////////////////////////////
    // these do *not* belong here ... but are here temporarily
    if (group3 == null)
      group3 = "";
    if (sequenceNumber < 0)
      sequenceNumber = -1;
    if (chainID == '\0')
      chainID = ' ';
    if (insertionCode == '\0')
      insertionCode = ' ';
    ////////////////////////////////////////////////////////////////
    JmolViewer viewer = frame.viewer;
    this.frame = frame;
    this.atomIndex = atomIndex;
    this.modelNumber = (short)modelNumber;
    this.elementNumber = elementNumber;
    if (formalCharge == Integer.MIN_VALUE)
      formalCharge = 0;
    this.formalChargeAndFlags = (byte)(formalCharge << 4);
    this.partialCharge = partialCharge; // temporarily here
    this.occupancy = (occupancy < 0
                      ? (byte)0
                      : (occupancy > 100
                         ? (byte)100
                         : (byte)occupancy));
    this.bfactor100 =
      (bfactor == Float.NaN ? Short.MIN_VALUE : (short)(bfactor*100));
    this.atomSerial = atomSerial;
    this.atomName = (atomName == null ? null : atomName.intern());
    specialAtomID = lookupSpecialAtomID(atomName);
    this.colixAtom = viewer.getColixAtom(this);
    setMadAtom(viewer.getMadAtom());
    this.point3f = new Point3f(x, y, z);
    this.isHetero = isHetero;
    // this does not belong here
    // put it in the higher level and pass in the group
    group = pdbFile.registerAtom(this, modelNumber, chainID,
                                 sequenceNumber, insertionCode, group3);
    if (!Float.isNaN(vibrationX) && !Float.isNaN(vibrationY) &&
        !Float.isNaN(vibrationZ)) {
      vibrationVector = new Vector3f(vibrationX, vibrationY, vibrationZ);
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
      bonds = (Bond[])Util.setLength(bonds, i + 1);
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

  final static int MIN_Z = 100;
  final static int MAX_Z = 14383;

  public void transform(JmolViewer viewer) {
    if (madAtom == JmolConstants.MAR_DELETED)
      return;
    Point3i screen = viewer.transformPoint(point3f, vibrationVector);
    int z = screen.z;
    z = ((z < MIN_Z)
         ? MIN_Z
         : ((z > MAX_Z)
            ? MAX_Z
            : z));
    int diameter = viewer.scaleToScreen(z, madAtom);
    xyzd = Xyzd.getXyzd(screen.x, screen.y, z, diameter);
  }

  public int getElementNumber() {
    return elementNumber;
  }

  public String getElementSymbol() {
    return JmolConstants.elementSymbols[elementNumber];
  }

  public String getAtomName() {
    return (atomName != null
            ? atomName : JmolConstants.elementSymbols[elementNumber]);
  }
  
  public String getPdbAtomName4() {
    return atomName == null ? "" : atomName;
  }

  public String getGroup3() {
    return group.getGroup3();
  }

  public boolean isGroup3(String group3) {
    return group.isGroup3(group3);
  }

  public boolean isGroup3Match(String strWildcard) {
    return group.isGroup3Match(strWildcard);
  }

  public int getSeqcode() {
    return group.seqcode;
  }

  public boolean isAtomNameMatch(String strPattern) {
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

  public int getAtomNumber() {
    if (atomSerial != Integer.MIN_VALUE)
      return atomSerial;
    if (frame.modelTypeName == "xyz" &&
        frame.viewer.getZeroBasedXyzRasmol())
      return atomIndex;
    return atomIndex + 1;
  }

  public boolean isHetero() {
    return isHetero;
  }

  public int getFormalCharge() {
    return formalChargeAndFlags >> 4;
  }

  public boolean isVisible() {
    return (formalChargeAndFlags & VISIBLE_FLAG) != 0;
  }

  public float getPartialCharge() {
    return partialCharge;
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
    return JmolConstants.getBondingMar(elementNumber,
                                       formalChargeAndFlags >> 4);
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
    return group.chain.chainID;
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

  public Group getGroup() {
    return group;
  }

  public Polymer getPolymer() {
    return group.polymer;
  }

  public Chain getChain() {
    return group.chain;
  }

  public PdbModel getPdbModel() {
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
    xyzd = Xyzd.NaN;
  }

  public byte getSecondaryStructureType() {
    return group.getStructureType();
  }

  public short getGroupID() {
    return group.groupID;
  }

  public String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  public int getModelID() {
    return group.chain.pdbmodel.modelNumber;
  }
  
  public byte getSpecialAtomID() {
    return specialAtomID;
  }

  public void demoteSpecialAtomImposter() {
    specialAtomID = -1;
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
    // this loop *must* run in reverse direction because of
    // protein mainchain imposters
    for (int i = JmolConstants.specialAtomNames.length; --i >= 0; )
      htAtom.put(JmolConstants.specialAtomNames[i], new Integer(i));
  }

  byte lookupSpecialAtomID(String atomName) {
    if (atomName != null) {
      Integer boxedAtomID = (Integer)htAtom.get(atomName);
      if (boxedAtomID != null)
        return (byte)(boxedAtomID.intValue());
    }
    return -1;
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
      char ch = strFormat.charAt(ich++);
      switch (ch) {
      case 'i':
        strLabel += getAtomNumber();
        break;
      case 'a':
        strLabel += getAtomName();
        break;
      case 'e':
        strLabel += JmolConstants.elementSymbols[elementNumber];
        break;
      case 'x':
        strLabel += point3f.x;
        break;
      case 'y':
        strLabel += point3f.y;
        break;
      case 'z':
        strLabel += point3f.z;
        break;
      case 'C':
        int formalCharge = getFormalCharge();
        if (formalCharge > 0)
          strLabel += "" + formalCharge + "+";
        else if (formalCharge < 0)
          strLabel += "" + -formalCharge + "-";
        else
          strLabel += "0";
        break;
      case 'P':
        float partialCharge = getPartialCharge();
        if (Float.isNaN(partialCharge))
          strLabel += "?";
        else
          strLabel += partialCharge;
        break;
      case 'V':
        strLabel += getVanderwaalsRadiusFloat();
        break;
      case 'I':
        strLabel += getBondingRadiusFloat();
        break;
      case 'b': // these two are the same
      case 't':
        strLabel += (getBfactor100() / 100.0);
        break;
      case 'q':
        strLabel += occupancy;
        break;
      case 'c': // these two are the same
      case 's':
        strLabel += getChainID();
        break;
      case 'M':
        strLabel += "/" + getModelNumber();
        break;
      case 'm':
        strLabel += "<X>";
        break;
      case 'n':
        strLabel += getGroup3();
        break;
      case 'r':
        strLabel += getSeqcodeString();
        break;
      case 'U':
        strLabel += getIdentity();
        break;
      case '{': // client property name
        int ichCloseBracket = strFormat.indexOf('}', ich);
        if (ichCloseBracket > ich) { // also picks up -1 when no '}' is found
          String propertyName = strFormat.substring(ich, ichCloseBracket);
          String value = getClientAtomStringProperty(propertyName);
          if (value != null)
            strLabel += value;
          ich = ichCloseBracket + 1;
          break;
        }
        // malformed will fall into
      default:
        strLabel += "%" + ch;
      }
    }
    strLabel += strFormat.substring(ich);
    if (strLabel.length() == 0)
      return null;
    else
      return strLabel.intern();
  }

  public String getInfo() {
    StringBuffer info = new StringBuffer();
    info.append(getAtomName());
    String group3 = getGroup3();
    if (group3 != null) {
      info.append(' ');
      info.append(group3);
    }
    int seqcode = getSeqcode();
    if (seqcode != -1) {
      info.append(' ');
      info.append(group.getSeqcodeString());
    }
    char chainID = getChainID();
    if (chainID != 0) {
      info.append(" Chain:");
      info.append(chainID);
    }
    if (frame.getModelCount() > 1) {
      info.append(" Model:");
      info.append(modelNumber);
    }
    return "" + info;
  }

  public String getIdentity() {
    StringBuffer info = new StringBuffer();
    if (atomName != null)
      info.append(atomName);
    else {
      info.append(getElementSymbol());
      info.append(" ");
      info.append(getAtomNumber());
    }
    String group3 = getGroup3();
    if (group3 != null && group3.length() > 0) {
      info.append(" [");
      info.append(group3);
      info.append("]");
    }
    /*
    int seqcode = getSeqcode();
    System.out.println("seqcode=" + seqcode);
    if (seqcode != -1) {
      info.append(seqcode);
    }
    */
    char chainID = getChainID();
    if (chainID != 0 && chainID != ' ') {
      info.append(":");
      info.append(chainID);
    }
    if (frame.getModelCount() > 1) {
      info.append("/");
      info.append(modelNumber);
    }
    return "" + info;
  }

  boolean isCursorOnTop(int xCursor, int yCursor, Atom competitor) {
    if ((formalChargeAndFlags & VISIBLE_FLAG) == 0)
      return false;
    int r = Xyzd.getD(xyzd) / 2;
    if (r < 4)
      r = 4;
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
}
