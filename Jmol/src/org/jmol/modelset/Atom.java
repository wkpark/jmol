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

package org.jmol.modelset;

import org.jmol.vecmath.Point3fi;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.viewer.Viewer;
import org.jmol.g3d.Graphics3D;
import org.jmol.util.Quaternion;
import org.jmol.util.TextFormat;

import java.util.BitSet;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

final public class Atom extends Point3fi {

  final static byte VIBRATION_VECTOR_FLAG = 0x01;
  final static byte IS_HETERO_FLAG = 0x02;
  final static byte FORMAL_CHARGE_MASK = 0x1C;
  final static byte VALENCY_MASK = (byte)0xE0;

  Group group;
  int atomIndex;
  BitSet atomSymmetry;
  int atomSite;
  private float userDefinedVanDerWaalRadius;
  
  public int getScreenRadius() {
    return screenDiameter / 2;
  }
  
  short modelIndex;
  private short atomicAndIsotopeNumber;
  private byte formalChargeAndFlags;
  char alternateLocationID;
  short madAtom;
  public short getMadAtom() {
    return madAtom;
  }
  
  short colixAtom;
  byte paletteID = JmolConstants.PALETTE_CPK;

  Bond[] bonds;
  int nBondsDisplayed = 0;
  int nBackbonesDisplayed = 0;
  
  public int getNBackbonesDisplayed() {
    return nBackbonesDisplayed;
  }
  
  int clickabilityFlags;
  int shapeVisibilityFlags;
  boolean isSimple = false;
  public boolean isSimple() {
    return isSimple;
  }
  
  public Atom(Point3f pt) {
    //just a point -- just enough to determine a position
    isSimple = true;
    this.x = pt.x; this.y = pt.y; this.z = pt.z;
    //must be transformed later -- Polyhedra;
    formalChargeAndFlags = 0;
    madAtom = 0;
  }
  
  Atom(Viewer viewer, int modelIndex, int atomIndex,
       BitSet atomSymmetry, int atomSite,
       short atomicAndIsotopeNumber,
       int size, int formalCharge, 
       float x, float y, float z,
       boolean isHetero, char chainID,
       char alternateLocationID,
       float radius) {
    this.modelIndex = (short)modelIndex;
    this.atomSymmetry = atomSymmetry;
    this.atomSite = atomSite;
    this.atomIndex = atomIndex;
    this.atomicAndIsotopeNumber = atomicAndIsotopeNumber;
    if (isHetero)
      formalChargeAndFlags = IS_HETERO_FLAG;
    setFormalCharge(formalCharge);
    this.alternateLocationID = alternateLocationID;
    userDefinedVanDerWaalRadius = radius;
    setMadAtom(viewer, size);
    set(x, y, z);
  }

  public final void setShapeVisibilityFlags(int flag) {
    shapeVisibilityFlags = flag;
  }

  public final void setShapeVisibility(int shapeVisibilityFlag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= shapeVisibilityFlag;        
    } else {
      shapeVisibilityFlags &=~shapeVisibilityFlag;
    }
  }
  
  public boolean isBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
  }

  public Bond getBond(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].getOtherAtom(atomOther) != null)
          return bonds[i];
    return null;
  }

  void addDisplayedBond(int stickVisibilityFlag, boolean isVisible){
    nBondsDisplayed+=(isVisible ? 1 : -1);
    setShapeVisibility(stickVisibilityFlag, isVisible);
  } 
  
  public void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible){
    nBackbonesDisplayed+=(isVisible ? 1 : -1);
    setShapeVisibility(backboneVisibilityFlag, isVisible);
  }
  
  void deleteBond(Bond bond) {
    //this one is used -- from Bond.deleteAtomReferences
    for (int i = bonds.length; --i >= 0; )
      if (bonds[i] == bond) {
        deleteBond(i);
        return;
      }
  }

  private void deleteBond(int i) {
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

  public void setMadAtom(Viewer viewer, int size) {
    madAtom = convertEncodedMad(viewer, size);
  }

  public short convertEncodedMad(Viewer viewer, int size) {
    switch (size) {
    case 0:
      return 0;
    case -1000: // temperature
      int diameter = getBfactor100() * 10 * 2;
      if (diameter > 4000)
        diameter = 4000;
      size = diameter;
      break;
    case -1001: // ionic
      size = (getBondingMar() * 2);
      break;
    case -100: // simple van der waals
      size = getVanderwaalsMad(viewer);
    default:
      if (size <= Short.MIN_VALUE) { //ADPMIN
        float d = 2000 * getADPMinMax(false);
        if (size < Short.MIN_VALUE)
          size = (int) (d * (Short.MIN_VALUE - size) / 100f);
        else
          size = (int) d;
        break;
      } else if (size < -2000) {
        // percent of custom size, to diameter
        // -2000 = Jmol, -3000 = Babel, -4000 = RasMol, -5000 = User
        int iMode = (-size / 1000) - 2;
        size = (-size) % 1000;
        size = (int) (size / 50f * viewer.getVanderwaalsMar(
            atomicAndIsotopeNumber % 128, iMode));
      } else if (size < 0) {
        // percent
        //      we are going from a radius to a diameter
        size = -size;
        if (size > 200)
          size = 200;
        size = (int) (size / 100f * getVanderwaalsMad(viewer));
      } else if (size >= Short.MAX_VALUE) { //ADPMAX
          float d = 2000 * getADPMinMax(true);
          if (size > Short.MAX_VALUE)
            size = (int) (d * (size - Short.MAX_VALUE) / 100f);
          else
            size = (int) d;
          break;
      } else if (size >= 10000) {
        // radiusAngstroms = vdw + x, where size = (x*2)*1000 + 10000
        // max is SHORT.MAX_VALUE - 1 = 32766
        // so max x is about 11 -- should be plenty!
        // and vdwMar = vdw * 1000
        // we want mad = diameterAngstroms * 1000 = (radiusAngstroms *2)*1000 
        //             = (vdw * 2 * 1000) + x * 2 * 1000
        //             = vdwMar * 2 + (size - 10000)
        size = size - 10000 + getVanderwaalsMad(viewer);
      }
    }
    return (short) size;
  }

  public float getADPMinMax(boolean isMax) {
    Object[] ellipsoid = getEllipsoid();
    if (ellipsoid == null)
      return 0;
    return ((float[])ellipsoid[1])[isMax ? 5 : 3];
  }

  public int getRasMolRadius() {
    return Math.abs(madAtom / (4 * 2));
  }

  public int getCovalentBondCount() {
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
          && (bonds[i].getOtherAtom(this).getElementNumber()) == 1)
        ++n;
    return n;
  }

  public Bond[] getBonds() {
    return bonds;
  }

  public void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  public void setPaletteID(byte paletteID) {
    this.paletteID = paletteID;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colixAtom = Graphics3D.getColixTranslucent(colixAtom, isTranslucent, translucentLevel);    
  }

  public boolean isTranslucent() {
    return Graphics3D.isColixTranslucent(colixAtom);
  }

  public short getElementNumber() {
    return (short) (atomicAndIsotopeNumber % 128);
  }
  
  public short getIsotopeNumber() {
    return (short) (atomicAndIsotopeNumber >> 7);
  }
  
  public short getAtomicAndIsotopeNumber() {
    return atomicAndIsotopeNumber;
  }

  public String getElementSymbol() {
    return JmolConstants.elementSymbolFromNumber(atomicAndIsotopeNumber);
  }

  public char getAlternateLocationID() {
    return alternateLocationID;
  }
  
  boolean isAlternateLocationMatch(String strPattern) {
    if (strPattern == null)
      return (alternateLocationID == '\0');
    if (strPattern.length() != 1)
      return false;
    char ch = strPattern.charAt(0);
    return (ch == '*' 
        || ch == '?' && alternateLocationID != '\0' 
        || alternateLocationID == ch);
  }

  public boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  void setFormalCharge(int charge) {
    if (charge > 7)
      charge = 7;
    formalChargeAndFlags = (byte)((formalChargeAndFlags & ~FORMAL_CHARGE_MASK) | (charge << 2));
  }
  
  void setVibrationVector() {
    formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
  }
  
  public int getFormalCharge() {
    return formalChargeAndFlags >> 2;
  }

  // a percentage value in the range 0-100
  public int getOccupancy() {
    byte[] occupancies = group.chain.modelSet.occupancies;
    return occupancies == null ? 100 : occupancies[atomIndex];
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  public int getBfactor100() {
    short[] bfactor100s = group.chain.modelSet.bfactor100s;
    if (bfactor100s == null)
      return 0;
    return bfactor100s[atomIndex];
  }

  public boolean setRadius(float radius) {
    return !Float.isNaN(userDefinedVanDerWaalRadius = (radius > 0 ? radius : Float.NaN));  
  }
  
  public void setValency(int nBonds) {
    if (nBonds > 7)
      nBonds = 7;
    formalChargeAndFlags = (byte) ((formalChargeAndFlags & ~VALENCY_MASK) | ((byte) nBonds << 5));
  }

  public int getValence() {
    int n = (formalChargeAndFlags >> 5) & 7;
    if (n == 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    return n;
  }

  public float getDimensionValue(int dimension) {
    return (dimension == 0 ? x : (dimension == 1 ? y : z));
  }

  private int getVanderwaalsMad(Viewer viewer) {
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? viewer.getVanderwaalsMar(atomicAndIsotopeNumber % 128) * 2
        : (int)(userDefinedVanDerWaalRadius * 2000f));
  }

  public float getVanderwaalsRadiusFloat() {
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? group.chain.modelSet.getVanderwaalsMar(atomicAndIsotopeNumber % 128) / 1000f
        : userDefinedVanDerWaalRadius);
  }

  short getBondingMar() {
    return JmolConstants.getBondingMar(atomicAndIsotopeNumber % 128,
        getFormalCharge());
  }

  public float getBondingRadiusFloat() {
    return getBondingMar() / 1000f;
  }

  int getCurrentBondCount() {
    return bonds == null ? 0 : bonds.length;
  }

  public short getColix() {
    return colixAtom;
  }

  public byte getPaletteID() {
    return paletteID;
  }

  public float getRadius() {
    return Math.abs(madAtom / (1000f * 2));
  }

  public int getAtomIndex() {
    return atomIndex;
  }

  public int getAtomSite() {
    return atomSite;
  }

  public BitSet getAtomSymmetry() {
    return atomSymmetry;
  }

   void setGroup(Group group) {
     this.group = group;
   }

   public Group getGroup() {
     return group;
   }
   
   public void transform(Viewer viewer) {
     Point3i screen;
     Vector3f[] vibrationVectors;
     if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 ||
         (vibrationVectors = group.chain.modelSet.vibrationVectors) == null)
       screen = viewer.transformPoint(this);
     else 
       screen = viewer.transformPoint(this, vibrationVectors[atomIndex]);
     screenX = screen.x;
     screenY = screen.y;
     screenZ = screen.z;
     screenDiameter = viewer.scaleToScreen(screenZ, Math.abs(madAtom));
   }

   // note: atomName cannot be null
   // note: atomNames cannot be null
   
   String getAtomName() {
     return group.chain.modelSet.atomNames[atomIndex];
   }
   
   public int getAtomNumber() {
     int[] atomSerials = group.chain.modelSet.atomSerials;
     return (atomSerials != null ? atomSerials[atomIndex] : atomIndex);
//        : group.chain.modelSet.isZeroBased ? atomIndex : atomIndex);
   }

   public boolean isModelVisible() {
     return ((shapeVisibilityFlags & JmolConstants.ATOM_IN_MODEL) != 0);
   }

   public int getShapeVisibilityFlags() {
     return shapeVisibilityFlags;
   }
   
   public boolean isShapeVisible(int shapeVisibilityFlag) {
     return (isModelVisible() 
         && (shapeVisibilityFlags & shapeVisibilityFlag) != 0);
   }

   public float getPartialCharge() {
     float[] partialCharges = group.chain.modelSet.partialCharges;
     return partialCharges == null ? 0 : partialCharges[atomIndex];
   }

   public float getStraightness() {
     return group.getStraightness();
   }

   public Object[] getEllipsoid() {
     return group.chain.modelSet.getEllipsoid(atomIndex);
   }

   /**
    * Given a symmetry operation number, the set of cells in the model, and the
    * number of operations, this method returns either 0 or the cell number (555, 666)
    * of the translated symmetry operation corresponding to this atom.
    * 
    * atomSymmetry is a bitset that is created in adapter.smarter.AtomSetCollection
    * 
    * It is arranged as follows:
    * 
    * |--overall--|---cell1---|---cell2---|---cell3---|...
    * 
    * |012..nOps-1|012..nOps-1|012..nOp-1s|012..nOps-1|...
    * 
    * If a bit is set, it means that the atom was created using that operator
    * operating on the base file set and translated for that cell.
    * 
    * If any bit is set in any of the cell blocks, then the same
    * bit will also be set in the overall block. This allows for
    * rapid determination of special positions and also of
    * atom membership in any operation set.
    * 
    *  Note that it is not necessarily true that an atom is IN the designated
    *  cell, because one can load {nnn mmm 0}, and then, for example, the {-x,-y,-z}
    *  operator sends atoms from 555 to 444. Still, those atoms would be marked as
    *  cell 555 here, because no translation was carried out. 
    *  
    *  That is, the numbers 444 in symop=3444 do not refer to a cell, per se. 
    *  What they refer to is the file-designated operator plus a translation of
    *  {-1 -1 -1/1}. 
    * 
    * @param symop        = 0, 1, 2, 3, ....
    * @param cellRange    = {444, 445, 446, 454, 455, 456, .... }
    * @param nOps         = 2 for x,y,z;-x,-y,-z, for example
    * @return cell number such as 565
    */
   public int getSymmetryTranslation(int symop, int[] cellRange, int nOps) {
     int pt = symop;
     for (int i = 0; i < cellRange.length; i++)
       if (atomSymmetry.get(pt += nOps))
         return cellRange[i];
     return 0;
   }
   
   /**
    * Looks for a match in the cellRange list for this atom within the specified translation set
    * select symop=0NNN for this
    * 
    * @param cellNNN
    * @param cellRange
    * @param nOps
    * @return     matching cell number, if applicable
    */
   public int getCellTranslation(int cellNNN, int[] cellRange, int nOps) {
     int pt = nOps;
     for (int i = 0; i < cellRange.length; i++)
       for (int j = 0; j < nOps;j++, pt++)
       if (atomSymmetry.get(pt) && cellRange[i] == cellNNN)
         return cellRange[i];
     return 0;
   }
   
   private String getSymmetryOperatorList() {
    String str = "";
    ModelSet f = group.chain.modelSet;
    if (atomSymmetry == null || f.cellInfos == null
        || f.cellInfos[modelIndex] == null)
      return "";
    int[] cellRange = f.getModelCellRange(modelIndex);
    if (cellRange == null)
      return "";
    int nOps = f.getModelSymmetryCount(modelIndex);
    int pt = nOps;
    for (int i = 0; i < cellRange.length; i++)
      for (int j = 0; j < nOps; j++)
        if (atomSymmetry.get(pt++))
          str += "," + (j + 1) + "" + cellRange[i];
    return str.substring(1);
  }
   
   public int getModelIndex() {
     return modelIndex;
   }
   
   public int getMoleculeNumber() {
     return (group.chain.modelSet.getMoleculeIndex(atomIndex) + 1);
   }
   
   String getClientAtomStringProperty(String propertyName) {
     Object[] clientAtomReferences = group.chain.modelSet.clientAtomReferences;
     return
       ((clientAtomReferences==null || clientAtomReferences.length<=atomIndex)
        ? null : (group.chain.modelSet.viewer.
           getClientAtomStringProperty(clientAtomReferences[atomIndex],
                                       propertyName)));
   }

   public byte getSpecialAtomID() {
     byte[] specialAtomIDs = group.chain.modelSet.specialAtomIDs;
     return specialAtomIDs == null ? 0 : specialAtomIDs[atomIndex];
   }
   
  public float getFractionalCoord(char ch) {
    Point3f pt = getFractionalCoord();
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }
    
  public Point3f getFractionalCoord() {
    CellInfo[] c = group.chain.modelSet.cellInfos;
    if (c == null)
      return this;
    Point3f pt = new Point3f(this);
    c[modelIndex].toFractional(pt);
    return pt;
  }
  
  void setFractionalCoord(int tok, float fValue) {
    CellInfo[] c = group.chain.modelSet.cellInfos;
    if (c != null)
      c[modelIndex].toFractional(this);
    switch (tok) {
    case Token.fracX:
      x = fValue;
      break;
    case Token.fracY:
      y = fValue;
      break;
    case Token.fracZ:
      z = fValue;
      break;
    }
    if (c != null)
      c[modelIndex].toCartesian(this);
  }
  
  void setFractionalCoord(Point3f ptNew) {
    set(ptNew);
    CellInfo[] c = group.chain.modelSet.cellInfos;
    if (c != null)
      c[modelIndex].toCartesian(this);
  }
  
  boolean isCursorOnTopOf(int xCursor, int yCursor,
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

  /*
   *  DEVELOPER NOTE (BH):
   *  
   *  The following methods may not return 
   *  correct values until after modelSet.finalizeGroupBuild()
   *  
   */
   
  public String getInfo() {
    return getIdentity(true);
  } 

  String getInfoXYZ(boolean useChimeFormat) {
    if (useChimeFormat) {
      String group3 = getGroup3();
      char chainID = getChainID();
      Point3f pt = (group.chain.modelSet.cellInfos == null ? null : getFractionalCoord());
      return "Atom: " + (group3 == null ? getElementSymbol() : getAtomName()) + " " + getAtomNumber() 
          + (group3 != null && group3.length() > 0 ? 
              (isHetero() ? " Hetero: " : " Group: ") + group3 + " " + getResno() 
              + (chainID != 0 && chainID != ' ' ? " Chain: " + chainID : "")              
              : "")
          + " Model: " + getModelNumber()
          + " Coordinates: " + x + " " + y + " " + z
          + (pt == null ? "" : " Fractional: "  + pt.x + " " + pt.y + " " + pt.z); 
    }
    return getIdentity(true) + " " + x + " " + y + " " + z;
  }

  private String getIdentityXYZ() {
    return getIdentity(false) + " " + x + " " + y + " " + z;
  }
  
  private String getIdentity(boolean allInfo) {
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
    if (!allInfo)
      return info.toString();
    if (info.length() > 0)
      info.append(".");
    info.append(getAtomName());
    if (info.length() == 0) {
      // since atomName cannot be null, this is unreachable
      info.append(getElementSymbol());
      info.append(" ");
      info.append(getAtomNumber());
    }
    if (alternateLocationID != 0) {
      info.append("%");
      info.append(alternateLocationID);
    }
    if (group.chain.modelSet.getModelCount() > 1) {
      info.append("/");
      info.append(getModelNumberForLabel());
    }
    info.append(" #");
    info.append(getAtomNumber());
    return info.toString();
  }

  String getGroup3() {
    return group.getGroup3();
  }

  String getGroup1() {
    char c = group.getGroup1();
    return (c == '\0' ? "" : "" + c);
  }

  boolean isGroup3(String group3) {
    return group.isGroup3(group3);
  }

  boolean isProtein() {
    return group.isProtein();
  }

  boolean isCarbohydrate() {
    return group.isCarbohydrate();
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

  int getSeqcode() {
    return group.getSeqcode();
  }

  public int getResno() {
    return group.getResno();   
  }

  public boolean isClickable() {
    // certainly if it is not visible, then it can't be clickable
    if (!isVisible())
      return false;
    int flags = shapeVisibilityFlags | group.shapeVisibilityFlags;
    return ((flags & clickabilityFlags) != 0);
  }

  public int getClickabilityFlags() {
    return clickabilityFlags;
  }
  
  public void setClickable(int flag) {
    if (flag == 0)
      clickabilityFlags = 0;
    else
      clickabilityFlags |= flag;
  }
  
  /**
   * determine if an atom or its PDB group is visible
   * @return true if the atom is in the "select visible" set
   */
  public boolean isVisible() {
    // Is the atom's model visible? Is the atom NOT hidden?
    if (!isModelVisible() || group.chain.modelSet.isAtomHidden(atomIndex))
      return false;
    // Is any shape associated with this atom visible? 
    int flags = shapeVisibilityFlags;
    // Is its PDB group visible in any way (cartoon, e.g.)?
    //  An atom is considered visible if its PDB group is visible, even
    //  if it does not show up itself as part of the structure
    //  (this will be a difference in terms of *clickability*).
    flags |= group.shapeVisibilityFlags;
    // We know that (flags & AIM), so now we must remove that flag
    // and check to see if any others are remaining.
    // Only then is the atom considered visible.
    return ((flags & ~JmolConstants.ATOM_IN_MODEL) != 0);
  }

  public float getGroupPhi() {
    return group.phi;
  }

  public float getGroupPsi() {
    return group.psi;
  }

  public char getChainID() {
    return group.chain.chainID;
  }

  public int getSurfaceDistance100() {
    return group.chain.modelSet.getSurfaceDistance100(atomIndex);
  }

  public Vector3f getVibrationVector() {
    return group.chain.modelSet.getVibrationVector(atomIndex);
  }

  public int getPolymerLength() {
    return group.getBioPolymerLength();
  }

  public Quaternion getQuaternion(char qtype) {
    return group.getQuaternion(qtype);
  }
  
  int getPolymerIndex() {
    return group.getBioPolymerIndex();
  }

  public int getSelectedGroupCountWithinChain() {
    return group.chain.getSelectedGroupCount();
  }

  public int getSelectedGroupIndexWithinChain() {
    return group.getSelectedGroupIndex();
  }

  public int getSelectedMonomerCountWithinPolymer() {
    return group.getSelectedMonomerCount();
  }

  public int getSelectedMonomerIndexWithinPolymer() {
    return group.getSelectedMonomerIndex();
  }

  Chain getChain() {
    return group.chain;
  }

  String getModelNumberForLabel() {
    return group.chain.modelSet.getModelNumberForAtomLabel(modelIndex);
  }
  
  public int getModelNumber() {
    return group.chain.modelSet.getModelNumber(modelIndex) % 1000000;
  }
  
  public int getModelFileIndex() {
    return group.chain.model.fileIndex;
  }
  
  public int getModelFileNumber() {
    return group.chain.modelSet.getModelFileNumber(modelIndex);
  }
  
  public byte getProteinStructureType() {
    return group.getProteinStructureType();
  }
  
  public int getProteinStructureID() {
    return group.getProteinStructureID();
  }

  public int setProteinStructureType(byte type, int indexCurrent) {
    return group.setProteinStructureType(type, indexCurrent);
  }

  public short getGroupID() {
    return group.groupID;
  }

  String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  int getSeqNumber() {
    return group.getSeqNumber();
  }

  public char getInsertionCode() {
    return group.getInsertionCode();
  }
  
  public String formatLabel(String strFormat) {
    return formatLabel(strFormat, '\0', null);
  }

  public String formatLabel(String strFormat, char chAtom, int[]indices) {
    if (strFormat == null || strFormat.length() == 0)
      return null;
    String strLabel = "";
    //boolean isSubscript = false;
    //boolean isSuperscript = false;
    int cch = strFormat.length();
    int ich, ichPercent;
    for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1;) {
      if (ich != ichPercent)
        strLabel += strFormat.substring(ich, ichPercent);
      ich = ichPercent + 1;
      try {
        String strT = "";
        float floatT = Float.NaN;
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
        int precision = Integer.MAX_VALUE;
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
         case '{': parameter value
         case 'A': alternate location identifier
         case 'a': atom name
         case 'b': temperature factor ("b factor")
         case 'C': formal Charge
         case 'c': chain
         case 'D': atom inDex (was "X")
         case 'e': element symbol
         case 'E': insErtion code
         case 'f': phi
         case 'g': selected group index (for testing)
         case 'i': atom number
         case 'I': Ionic radius
         case 'L': polymer Length
         case 'l': atomic element number
         case 'm': group1
         case 'M': Model number
         case 'n': group3
         case 'N': molecule Number
         case 'o': symmetry operator set
         case 'p': psi
         case 'P': Partial charge
         case 'q': occupancy 0-100%
         case 'Q': occupancy 0.00 - 1.00
         case 'r': residue sequence code
         case 'R': residue number
         case 'S': crystallographic Site
         case 's': strand (chain)
         case 't': temperature factor
         case 'T': straighTness
         case 'U': identity
         case 'u': sUrface distance
         case 'v': vibration x, y, or z  vx vy vz
         case 'V': van der Waals
         case 'x': x coord
         case 'X': fractional X coord
         case 'W': identity - with X,Y,Z
         case 'y': y coord
         case 'Y': fractional Y coord
         case 'z': z coord
         case 'Z': fractional Z coord
         case '_': subscript   //reserved
         case '^': superscript //reserved
         */
        char ch0 = ch = strFormat.charAt(ich++);

        if (chAtom != '\0' && ich < cch) {
          if (strFormat.charAt(ich) != chAtom) {
            strLabel = strLabel + "%";
            ich = ichPercent + 1;
             continue;
          }
          ich++;
        }
        switch (ch) {
        case 'A':
          strT = (alternateLocationID != '\0' ? alternateLocationID + ""
              : "");
          break;
        case 'a':
          strT = getAtomName();
          break;
//        case 'b': // see 't'
//        case 'c': // see 's'
        case 'C':
          int formalCharge = getFormalCharge();
          if (formalCharge > 0)
            strT = "" + formalCharge + "+";
          else if (formalCharge < 0)
            strT = "" + -formalCharge + "-";
          else
            strT = "0";
          break;
        case 'D':
          strT = "" + (indices == null ? atomIndex : indices[atomIndex]);
          break;
        case 'e':
          strT = getElementSymbol();
          break;
        case 'E':
          ch = getInsertionCode();
          strT = (ch == '\0' ? "" : "" + ch);
          break;
        case 'f':
          floatT = getGroupPhi();
          break;
        case 'g':
          strT = "" + getSelectedGroupIndexWithinChain();
          break;
        case 'I':
          floatT = getBondingRadiusFloat();
          break;
        case 'i':
          strT = "" + getAtomNumber();
          break;
        case 'L':
          strT = "" + getPolymerLength();
          break;
        case 'l':
          strT = "" + getElementNumber();
          break;
        case 'M':
          strT = getModelNumberForLabel();
          break;
        case 'm':
          strT = getGroup1();
          break;
        case 'N':
          strT = "" + getMoleculeNumber();
          break;
        case 'n':
          strT = getGroup3();
          if (strT == null || strT.length() == 0)
            strT = "UNK";
          break;
        case 'o':
          strT = getSymmetryOperatorList();
          break;
        case 'P':
          floatT = getPartialCharge();
          break;
        case 'p':
          floatT = getGroupPsi();
          break;
        case 'q':
          strT = "" + getOccupancy();
          break;
        case 'Q':
          floatT = getOccupancy() / 100f;
          break;
        case 'R':
          strT = "" + getResno();
          break;
        case 'r':
          strT = getSeqcodeString();
          break;
        case 'S':
          strT = "" + atomSite;
          break;
        case 's':
        case 'c': // these two are the same
          ch = getChainID();
          strT = (ch == '\0' ? "" : "" + ch);
          break;
        case 'T':
          floatT = getStraightness();
          break;
        case 't':
        case 'b': // these two are the same
          floatT = getBfactor100() / 100f;
          break;
        case 'U':
          strT = getIdentity(true);
          break;
        case 'u':
          floatT = getSurfaceDistance100() / 100f;
          break;
        case 'V':
          floatT = getVanderwaalsRadiusFloat();
          break;
        case 'v':
          ch = (ich < strFormat.length() ? strFormat.charAt(ich++) : '\0');
          switch (ch) {
          case 'x':
          case 'y':
          case 'z':
            floatT = group.chain.modelSet.getVibrationCoord(atomIndex, ch);
            break;
          default:
            if (ch != '\0')
              --ich;
            Vector3f v = getVibrationVector();
            if (v == null) {
              floatT = 0;
              break;
            }
            strT = v.x + " " + v.y + " " + v.z;
          }
          break;
        case 'W':
          strT = getIdentityXYZ();
          break;
        case 'x':
          floatT = x;
          break;
        case 'y':
          floatT = y;
          break;
        case 'z':
          floatT = z;
          break;
        case 'X':
        case 'Y':
        case 'Z':
          floatT = getFractionalCoord(ch);
          break;
        case '%':
          strT = "%";
          break;
        case '{': // client property name
          int ichCloseBracket = strFormat.indexOf('}', ich);
          if (ichCloseBracket > ich) { // also picks up -1 when no '}' is found
            String propertyName = strFormat.substring(ich, ichCloseBracket);
            floatT = group.chain.modelSet.viewer.getDataFloat(propertyName, atomIndex);
            if (Float.isNaN(floatT))
              strT = getClientAtomStringProperty(propertyName);
            if (strT != null || !Float.isNaN(floatT)) {
              ich = ichCloseBracket + 1;
              break;
            }
          }
        // malformed will fall into
        default:
          strT = "%" + ch0;
        }
        if (!Float.isNaN(floatT))
          strLabel += TextFormat.format(floatT, width, precision, alignLeft, zeroPad);
        else if (strT != null)
          strLabel += TextFormat.format(strT, width, precision, alignLeft, zeroPad);
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
  
  public boolean equals(Object obj) {
    return (this == obj);
  }

  public int hashCode() {
    //this overrides the Point3fi hashcode, which would
    //give a different hashcode for an atom depending upon
    //its screen location! Bug fix for 11.1.43 Bob Hanson
    return atomIndex;
  }
  
  public Atom findAromaticNeighbor(BitSet notAtoms) {
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && (notAtoms == null || !notAtoms.get(a.atomIndex)))
        return a;
    }
    return null;
  }

  public Atom findAromaticNeighbor(int notAtomIndex) {
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && a.atomIndex != notAtomIndex)
        return a;
    }
    return null;
  }

  /* DEVELOPER NOTE -- ATOM/MODEL DELETION --
   * 
   * The challenge of atom deletion:
   * 
   * Many data structures involve reference to Atom, atomIndex, Model, or modelIndex
   * A first-pass list includes:

org.jmol.modelset
-----------------

Atom.atomIndex
Atom.modelIndex
Bond.atom1
Bond.atom2
Chain.model
Group.firstAtomIndex
Group.lastAtomIndex
Model.modelIndex
Model.fileIndex
Model.firstAtomIndex
Model.firstMolecule
Model.chains
Model.bioPolymers
Model.auxiliaryInfo

AtomCollection.atoms
AtomCollection.atomCount
AtomCollection.atomNames
AtomCollection.atomSerials
AtomCollection.bfactor100s
AtomCollection.bspf
AtomCollection.bsHidden
AtomCollection.bsSurface
AtomCollection.nSurfaceAtoms
AtomCollection.clientAtomReferences
AtomCollection.hasBfactorRange   -- set false
AtomCollection.occupancies
AtomCollection.partialCharges
AtomCollection.specialAtomIDs
AtomCollection.surfaceDistance100s -- set null
AtomCollection.tainted
AtomCollection.vibrationVectors

BondCollection.bonds
BondCollection.bondCount

ModelCollection.averageAtomPoint
ModelCollection.bboxModels
ModelCollection.bboxAtoms
ModelCollection.boxInfo

ModelCollection.modelNumbers
ModelCollection.models
ModelCollection.modelSetAuxiliaryInfo["group3Lists", "group3Counts, "models"]
ModelCollection.molecules -- just set null
ModelCollection.moleculeCount
ModelCollection.stateScripts ?????
ModelCollection.thisStateModel  -- just set -1
ModelCollection.structures
ModelCollection.structureCount

ModelSet.shapes  (many of these hold references that would need adjusting)

CellInfo.modelIndex
Measurement.countPlusIndices
MeasurementPending.countPlusIndices
Polymer.leadAtomIndices -- can be set null in BioPolymer.recalculateLeadMidpointsAndWingVectors()

org.jmol.modelsetbio
--------------------

org.jmol.popup
--------------
 [ would need updating ]

org.jmol.shape
--------------

AtomShape.mads
AtomShape.colixes
AtomShape.paletteIDs
AtomShape.bsSizeSet
AtomShape.bsColixSet
AtomShape.atomCount
AtomShape.atoms

Dots?

Labels.strings
Labels.formats
Labels.bgcolixes
Labels.fids
Labels.offsets
Measures.measurements

Mesh.title (sometimes model-based?)
Mesh.atomIndex
Mesh.modelIndex
Mesh.modelFlags
MeshCollection.meshes
MeshCollection.modelCount
MeshCollection.title ?

Sticks.bsOrderSet
Sticks.bsSizeSet
Sticks.bsColixSet
Sticks.selectedBonds

TextShape.modelIndex

org.jmol.shapebio
-----------------

BioShape.modelIndex
BioShape.leadAtomIndices
BioShapeCollection.atoms
BioShapeRenderer -- all need to be set null

org.jmol.shapespecial
---------------------

Dipole.modelIndex
Dipole.atoms
DrawMesh.drawTypes
DrawMesh.ptCenters
DrawMesh.drawVertexCount
DrawMesh.drawVertexCounts
Draw.modelCount
MolecularOrbital.htModels
Polyhedra.Polyhedrons
Polyhedra.Polyhedron.centralAtom

org.jmol.viewer
---------------

SelectionManager.bsHidden
SelectionManager.bsSelection
SelectionManager.bsSubset
Eval.bsSubset

org.openscience.jmol.app
------------------------

AtomSetChooser ??


   * 
   */
}
