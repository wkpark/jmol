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

import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.atomdata.RadiusData.EnumType;
import org.jmol.c.PAL;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.util.C;
import org.jmol.util.Elements;
import org.jmol.util.Point3fi;

import javajs.util.CU;
import javajs.util.List;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.P3;
import org.jmol.util.Tensor;
import org.jmol.util.Edge;
import org.jmol.util.BNode;
import javajs.util.T3;
import javajs.util.V3;
import org.jmol.viewer.JC;
import org.jmol.viewer.Viewer;




public class Atom extends Point3fi implements BNode {

  private final static byte VIBRATION_VECTOR_FLAG = 1;
  private final static byte IS_HETERO_FLAG = 2;
  private final static byte FLAG_MASK = 3;
  
  public static final int RADIUS_MAX = 16;
  public static final float RADIUS_GLOBAL = 16.1f;
  public static short MAD_GLOBAL = 32200;

  public char altloc = '\0';
  public byte atomID;
  public int atomSite;
  public Group group;
  private float userDefinedVanDerWaalRadius;
  byte valence;
  
  private short atomicAndIsotopeNumber;
  private BS atomSymmetry;
  private byte formalChargeAndFlags;

  public byte getAtomID() {
    return atomID;
  }
  
  public short madAtom;

  public short colixAtom;
  byte paletteID = PAL.CPK.id;

  Bond[] bonds;
  
  /**
   * 
   * @return  bonds -- WHICH MAY BE NULL
   * 
   */
  public Bond[] getBonds() {
    return bonds;
  }

  public void setBonds(Bond[] bonds) {
    this.bonds = bonds;  // for Smiles equating
  }
  
  int nBondsDisplayed = 0;
  int nBackbonesDisplayed = 0;
  
  public int getNBackbonesDisplayed() {
    return nBackbonesDisplayed;
  }
  
  public int clickabilityFlags;
  public int shapeVisibilityFlags;

  /**
   * @j2sIgnoreSuperConstructor
   * @j2sOverride
   * 
   * @param modelIndex
   * @param atomIndex
   * @param xyz
   * @param radius
   * @param atomSymmetry
   * @param atomSite
   * @param atomicAndIsotopeNumber
   * @param formalCharge
   * @param isHetero
   * @return this
   */
  
  public Atom setAtom(int modelIndex, int atomIndex,
        P3 xyz, float radius,
        BS atomSymmetry, int atomSite,
        short atomicAndIsotopeNumber, int formalCharge, 
        boolean isHetero) {
    this.mi = (short)modelIndex;
    this.atomSymmetry = atomSymmetry;
    this.atomSite = atomSite;
    this.i = atomIndex;
    this.atomicAndIsotopeNumber = atomicAndIsotopeNumber;
    if (isHetero)
      formalChargeAndFlags = IS_HETERO_FLAG;
    if (formalCharge != 0 && formalCharge != Integer.MIN_VALUE)
      setFormalCharge(formalCharge);
    userDefinedVanDerWaalRadius = radius;
    setT(xyz);
    return this;
  }

  public void setAltLoc(char altLoc) {
    this.altloc = altLoc;
  }
  
  public final void setShapeVisibility(int flag, boolean isVisible) {
    if(isVisible) {
      shapeVisibilityFlags |= flag;        
    } else {
      shapeVisibilityFlags &=~flag;
    }
  }
  
  public boolean isCovalentlyBonded(Atom atomOther) {
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i].isCovalent() 
            && bonds[i].getOtherAtom(this) == atomOther)
          return true;
    return false;
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

  void addDisplayedBond(int stickVisibilityFlag, boolean isVisible) {
    nBondsDisplayed += (isVisible ? 1 : -1);
    setShapeVisibility(stickVisibilityFlag, (nBondsDisplayed > 0));
  } 
  
  public void addDisplayedBackbone(int backboneVisibilityFlag, boolean isVisible) {
    nBackbonesDisplayed += (isVisible ? 1 : -1);
    setShapeVisibility(backboneVisibilityFlag, isVisible);
  }
  
  void deleteBond(Bond bond) {
    // this one is used -- from Bond.deleteAtomReferences
    if (bonds != null)
      for (int i = bonds.length; --i >= 0;)
        if (bonds[i] == bond) {
          deleteBondAt(i);
          return;
        }
  }

  private void deleteBondAt(int i) {
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

  @Override
  public int getBondedAtomIndex(int bondIndex) {
    return bonds[bondIndex].getOtherAtom(this).i;
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

  public void setMadAtom(Viewer vwr, RadiusData rd) {
    madAtom = calculateMad(vwr, rd);
  }
  
  public short calculateMad(Viewer vwr, RadiusData rd) {
    if (rd == null)
      return 0;
    float f = rd.value;
    if (f == 0)
      return 0;
    switch (rd.factorType) {
    case SCREEN:
       return (short) f;
    case FACTOR:
    case OFFSET:
      float r = 0;
      switch (rd.vdwType) {
      case TEMP:
        float tmax = vwr.getBfactor100Hi();
        r = (tmax > 0 ? getBfactor100() / tmax : 0);
        break;
      case HYDRO:
        r = Math.abs(getHydrophobicity());
        break;
      case BONDING:
        r = getBondingRadius();
        break;
      case ADPMIN:
      case ADPMAX:
        r = getADPMinMax(rd.vdwType == VDW.ADPMAX);
        break;
      default:
        r = getVanderwaalsRadiusFloat(vwr, rd.vdwType);
      }
      if (rd.factorType == EnumType.FACTOR)
        f *= r;
      else
        f += r;
      break;
    case ABSOLUTE:
      if (f == RADIUS_GLOBAL)
        return MAD_GLOBAL;
      break;
    }
    short mad = (short) (f < 0 ? f: f * 2000);
    if (mad < 0 && f > 0)
      mad = 0;
    return mad; 
  }

  public float getADPMinMax(boolean isMax) {
    Object[] tensors = getTensors();
    if (tensors == null)
      return 0;
    Tensor t = (Tensor) tensors[0];
    if (t == null || t.iType != Tensor.TYPE_ADP)
      return 0;
    if (group.chain.model.ms.isModulated(i) && t.isUnmodulated)
      t = (Tensor) tensors[1];
    return t.getFactoredValue(isMax ? 2 : 1); 
  }

  public Object[] getTensors() {
    return group.chain.model.ms.getAtomTensorList(i);
  }
  
  public int getRasMolRadius() {
    return Math.abs(madAtom / 8); //  1000r = 1000d / 2; rr = (1000r / 4);
  }

  @Override
  public int getCovalentBondCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    Bond b;
    for (int i = bonds.length; --i >= 0; )
      if (((b = bonds[i]).order & Edge.BOND_COVALENT_MASK) != 0
          && !b.getOtherAtom(this).isDeleted())
        ++n;
    return n;
  }

  @Override
  public int getCovalentHydrogenCount() {
    if (bonds == null)
      return 0;
    int n = 0;
    for (int i = bonds.length; --i >= 0; ) {
      if ((bonds[i].order & Edge.BOND_COVALENT_MASK) == 0)
        continue;
      Atom a = bonds[i].getOtherAtom(this);
      if (a.valence >= 0 && a.getElementNumber() == 1)
        ++n;
    }
    return n;
  }

  @Override
  public Edge[] getEdges() {
    return bonds;
  }
  
  public void setColixAtom(short colixAtom) {
    this.colixAtom = colixAtom;
  }

  public void setPaletteID(byte paletteID) {
    this.paletteID = paletteID;
  }

  public void setTranslucent(boolean isTranslucent, float translucentLevel) {
    colixAtom = C.getColixTranslucent3(colixAtom, isTranslucent, translucentLevel);    
  }

  public boolean isTranslucent() {
    return C.isColixTranslucent(colixAtom);
  }

  @Override
  public int getElementNumber() {
    return Elements.getElementNumber(atomicAndIsotopeNumber);
  }
  
  @Override
  public int getIsotopeNumber() {
    return Elements.getIsotopeNumber(atomicAndIsotopeNumber);
  }
  
  @Override
  public int getAtomicAndIsotopeNumber() {
    return atomicAndIsotopeNumber;
  }

  public void setAtomicAndIsotopeNumber(int n) {
    if (n < 0 || (n & 127) >= Elements.elementNumberMax || n > Short.MAX_VALUE)
      n = 0;
    atomicAndIsotopeNumber = (short) n;
  }

  public String getElementSymbolIso(boolean withIsotope) {
    return Elements.elementSymbolFromNumber(withIsotope ? atomicAndIsotopeNumber : atomicAndIsotopeNumber & 127);    
  }
  
  public String getElementSymbol() {
    return getElementSymbolIso(true);
  }

  public char getAlternateLocationID() {
    return altloc;
  }
  
  boolean isAltLoc(String strPattern) {
    if (strPattern == null)
      return (altloc == '\0');
    if (strPattern.length() != 1)
      return false;
    char ch = strPattern.charAt(0);
    return (ch == '*' 
        || ch == '?' && altloc != '\0' 
        || altloc == ch);
  }

  public boolean isHetero() {
    return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
  }

  public boolean hasVibration() {
    return (formalChargeAndFlags & VIBRATION_VECTOR_FLAG) != 0;
  }

  public void setFormalCharge(int charge) {
    formalChargeAndFlags = (byte)((formalChargeAndFlags & FLAG_MASK) 
        | ((charge == Integer.MIN_VALUE ? 0 : charge > 7 ? 7 : charge < -3 ? -3 : charge) << 2));
  }
  
  void setVibrationVector() {
    formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
  }
  
  @Override
  public int getFormalCharge() {
    return formalChargeAndFlags >> 2;
  }

  // a percentage value in the range 0-100
  public int getOccupancy100() {
    byte[] occupancies = group.chain.model.ms.occupancies;
    return occupancies == null ? 100 : occupancies[i];
  }

  // This is called bfactor100 because it is stored as an integer
  // 100 times the bfactor(temperature) value
  public int getBfactor100() {
    short[] bfactor100s = group.chain.model.ms.bfactor100s;
    if (bfactor100s == null)
      return 0;
    return bfactor100s[i];
  }

  private float getHydrophobicity() {
    float[] values = group.chain.model.ms.hydrophobicities;
    if (values == null)
      return Elements.getHydrophobicity(group.getGroupID());
    return values[i];
  }

  public boolean setRadius(float radius) {
    return !Float.isNaN(userDefinedVanDerWaalRadius = (radius > 0 ? radius : Float.NaN));  
  }
  
  public void deleteBonds(BS bsBonds) {
    valence = -1;
    if (bonds != null)
      for (int i = bonds.length; --i >= 0; ) {
        Bond bond = bonds[i];
        bond.getOtherAtom(this).deleteBond(bond);
        bsBonds.set(bond.index);
      }
    bonds = null;
  }

  @Override
  public boolean isDeleted() {
    return (valence < 0);
  }

  public void setValence(int nBonds) {
    if (isDeleted()) // no resurrection
      return;
    valence = (byte) (nBonds < 0 ? 0 : nBonds < 0xEF ? nBonds : 0xEF);
  }

  @Override
  public int getValence() {
    if (isDeleted())
      return -1;
    int n = valence;
    if (n == 0 && bonds != null)
      for (int i = bonds.length; --i >= 0;)
        n += bonds[i].getValence();
    return n;
  }

  @Override
  public int getImplicitHydrogenCount() {
    return group.chain.model.ms.getImplicitHydrogenCount(this, false);
  }

  int getTargetValence() {
    switch (getElementNumber()) {
    case 6: //C
    case 14: //Si      
      return 4;
    case 5:  // B
    case 7:  // N
    case 15: // P
      return 3;
    case 8: //O
    case 16: //S
      return 2;
    case 1:
    case 9: // F
    case 17: // Cl
    case 35: // Br
    case 53: // I
      return 1;
    }
    return -1;
  }


  public float getDimensionValue(int dimension) {
    return (dimension == 0 ? x : (dimension == 1 ? y : z));
  }

  public float getVanderwaalsRadiusFloat(Viewer vwr, VDW type) {
    // called by atomPropertyFloat as VDW_AUTO,
    // AtomCollection.fillAtomData with VDW_AUTO or VDW_NOJMOL
    // AtomCollection.findMaxRadii with VDW_AUTO
    // AtomCollection.getAtomPropertyState with VDW_AUTO
    // AtomCollection.getVdwRadius with passed on type
    return (Float.isNaN(userDefinedVanDerWaalRadius) 
        ? vwr.getVanderwaalsMarType(atomicAndIsotopeNumber, getVdwType(type)) / 1000f
        : userDefinedVanDerWaalRadius);
  }

  /**
   * 
   * @param type 
   * @return if VDW_AUTO, will return VDW_AUTO_JMOL, VDW_AUTO_RASMOL, or VDW_AUTO_BABEL
   *         based on the model type
   */
  @SuppressWarnings("incomplete-switch")
  private VDW getVdwType(VDW type) {
    switch (type) {
    case AUTO:
      type = group.chain.model.ms.getDefaultVdwType(mi);
      break;
    case NOJMOL:
      type = group.chain.model.ms.getDefaultVdwType(mi);
      if (type == VDW.AUTO_JMOL)
        type = VDW.AUTO_BABEL;
      break;
    }
    return type;
  }

  public float getBondingRadius() {
    float[] rr = group.chain.model.ms.bondingRadii;
    float r = (rr == null ? 0 : rr[i]);
    return (r == 0 ? Elements.getBondingRadius(atomicAndIsotopeNumber,
        getFormalCharge()) : r);
  }

  float getVolume(Viewer vwr, VDW vType) {
    float r1 = (vType == null ? userDefinedVanDerWaalRadius : Float.NaN);
    if (Float.isNaN(r1))
      r1 = vwr.getVanderwaalsMarType(getElementNumber(), getVdwType(vType)) / 1000f;
    double volume = 0;
    if (bonds != null)
      for (int j = 0; j < bonds.length; j++) {
        if (!bonds[j].isCovalent())
          continue;
        Atom atom2 = bonds[j].getOtherAtom(this);
        float r2 = (vType == null ? atom2.userDefinedVanDerWaalRadius : Float.NaN);
        if (Float.isNaN(r2))
          r2 = vwr.getVanderwaalsMarType(atom2.getElementNumber(), atom2
              .getVdwType(vType)) / 1000f;
        float d = distance(atom2);
        if (d > r1 + r2)
          continue;
        if (d + r1 <= r2)
          return 0;

        // calculate hidden spherical cap height and volume
        // A.Bondi, J. Phys. Chem. 68, 1964, 441-451.

        double h = r1 - (r1 * r1 + d * d - r2 * r2) / (2.0 * d);
        volume -= Math.PI / 3 * h * h * (3 * r1 - h);
      }
    return (float) (volume + 4 * Math.PI / 3 * r1 * r1 * r1);
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

  @Override
  public int getIndex() {
    return i;
  }

  @Override
  public int getAtomSite() {
    return atomSite;
  }

  public void setAtomSymmetry(BS bsSymmetry) {
    atomSymmetry = bsSymmetry;
  }

  public BS getAtomSymmetry() {
    return atomSymmetry;
  }

   void setGroup(Group group) {
     this.group = group;
   }

   public Group getGroup() {
     return group;
   }
   
   @Override
  public void getGroupBits(BS bs) {
     group.selectAtoms(bs);
   }
   
   @Override
  public String getAtomName() {
     return (atomID > 0 ? JC.getSpecialAtomName(atomID) 
         : group.chain.model.ms.atomNames[i]);
   }
   
   @Override
  public String getAtomType() {
    String[] atomTypes = group.chain.model.ms.atomTypes;
    String type = (atomTypes == null ? null : atomTypes[i]);
    return (type == null ? getAtomName() : type);
  }
   
   public int getAtomNumber() {
     int[] atomSerials = group.chain.model.ms.atomSerials;
     // shouldn't ever be null.
     return (atomSerials != null ? atomSerials[i] : i);
//        : group.chain.model.modelSet.isZeroBased ? atomIndex : atomIndex);
   }

   public boolean isVisible(int flags) {
     return ((shapeVisibilityFlags & flags) == flags);
   }

   public float getPartialCharge() {
     float[] partialCharges = group.chain.model.ms.partialCharges;
     return partialCharges == null ? 0 : partialCharges[i];
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
   
   String getSymmetryOperatorList() {
    String str = "";
    ModelSet f = group.chain.model.ms;
    int nOps = f.getModelSymmetryCount(mi);
    if (nOps == 0 || atomSymmetry == null)
      return "";
    int[] cellRange = f.getModelCellRange(mi);
    int pt = nOps;
    int n = (cellRange == null ? 1 : cellRange.length);
    for (int i = 0; i < n; i++)
      for (int j = 0; j < nOps; j++)
        if (atomSymmetry.get(pt++))
          str += "," + (j + 1) + "" + cellRange[i];
    return (str.length() == 0 ? "" : str.substring(1));
  }
   
  @Override
  public int getModelIndex() {
    return mi;
  }
   
  int getMoleculeNumber(boolean inModel) {
    return (group.chain.model.ms.getMoleculeIndex(i, inModel) + 1);
  }
   
  private float getFractionalCoord(char ch, boolean asAbsolute) {
    P3 pt = getFractionalCoordPt(asAbsolute);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }
    
  private P3 getFractionalCoordPt(boolean asAbsolute) {
    // asAbsolute TRUE uses the original unshifted matrix
    SymmetryInterface c = getUnitCell();
    if (c == null) 
      return this;
    P3 pt = P3.newP(this);
    c.toFractional(pt, asAbsolute);
    return pt;
  }
  
  SymmetryInterface getUnitCell() {
    return group.chain.model.ms.getUnitCellForAtom(this.i);
  }
  
  private float getFractionalUnitCoord(char ch) {
    P3 pt = getFractionalUnitCoordPt(false);
    return (ch == 'X' ? pt.x : ch == 'Y' ? pt.y : pt.z);
  }

  P3 getFractionalUnitCoordPt(boolean asCartesian) {
    SymmetryInterface c = getUnitCell();
    if (c == null)
      return this;
    P3 pt = P3.newP(this);
    if (group.chain.model.isJmolDataFrame) {
      c.toFractional(pt, false);
      if (asCartesian)
        c.toCartesian(pt, false);
    } else {
      c.toUnitCell(pt, null);
      if (!asCartesian)
        c.toFractional(pt, false);
    }
    return pt;
  }
  
  float getFractionalUnitDistance(P3 pt, P3 ptTemp1, P3 ptTemp2) {
    SymmetryInterface c = getUnitCell();
    if (c == null) 
      return distance(pt);
    ptTemp1.setT(this);
    ptTemp2.setT(pt);
    if (group.chain.model.isJmolDataFrame) {
      c.toFractional(ptTemp1, true);
      c.toFractional(ptTemp2, true);
    } else {
      c.toUnitCell(ptTemp1, null);
      c.toUnitCell(ptTemp2, null);
    }
    return ptTemp1.distance(ptTemp2);
  }
  
  void setFractionalCoord(int tok, float fValue, boolean asAbsolute) {
    SymmetryInterface c = getUnitCell();
    if (c != null)
      c.toFractional(this, asAbsolute);
    switch (tok) {
    case T.fux:
    case T.fracx:
      x = fValue;
      break;
    case T.fuy:
    case T.fracy:
      y = fValue;
      break;
    case T.fuz:
    case T.fracz:
      z = fValue;
      break;
    }
    if (c != null)
      c.toCartesian(this, asAbsolute);
  }
  
  void setFractionalCoordTo(P3 ptNew, boolean asAbsolute) {
    setFractionalCoordPt(this, ptNew, asAbsolute);
  }
  
  public void setFractionalCoordPt(P3 pt, P3 ptNew, boolean asAbsolute) {
    pt.setT(ptNew);
    SymmetryInterface c = getUnitCell();
    if (c != null)
      c.toCartesian(pt, asAbsolute && !group.chain.model.isJmolDataFrame);
  }

  boolean isCursorOnTopOf(int xCursor, int yCursor,
                        int minRadius, Atom competitor) {
    int r = sD / 2;
    if (r < minRadius)
      r = minRadius;
    int r2 = r * r;
    int dx = sX - xCursor;
    int dx2 = dx * dx;
    if (dx2 > r2)
      return false;
    int dy = sY - yCursor;
    int dy2 = dy * dy;
    int dz2 = r2 - (dx2 + dy2);
    if (dz2 < 0)
      return false;
    if (competitor == null)
      return true;
    int z = sZ;
    int zCompetitor = competitor.sZ;
    int rCompetitor = competitor.sD / 2;
    if (z < zCompetitor - rCompetitor)
      return true;
    int dxCompetitor = competitor.sX - xCursor;
    int dx2Competitor = dxCompetitor * dxCompetitor;
    int dyCompetitor = competitor.sY - yCursor;
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
    // for atom picking
    if (useChimeFormat) {
      String group3 = getGroup3(true);
      int chainID = getChainID();
      P3 pt = getFractionalCoordPt(true);
      return "Atom: " + (group3 == null ? getElementSymbol() : getAtomName()) + " " + getAtomNumber() 
          + (group3 != null && group3.length() > 0 ? 
              (isHetero() ? " Hetero: " : " Group: ") + group3 + " " + getResno() 
              + (chainID != 0 && chainID != 32 ? " Chain: " + group.chain.getIDStr() : "")              
              : "")
          + " Model: " + getModelNumber()
          + " Coordinates: " + x + " " + y + " " + z
          + (pt == null ? "" : " Fractional: "  + pt.x + " " + pt.y + " " + pt.z); 
    }
    return getIdentityXYZ(true);
  }

  String getIdentityXYZ(boolean allInfo) {
    P3 pt = (group.chain.model.isJmolDataFrame ? getFractionalCoordPt(false) : this);
    return getIdentity(allInfo) + " " + pt.x + " " + pt.y + " " + pt.z;  
  }
  
  String getIdentity(boolean allInfo) {
    SB info = new SB();
    String group3 = getGroup3(true);
    if (group3 != null && group3.length() > 0) {
      info.append("[");
      info.append(group3);
      info.append("]");
      String seqcodeString = getSeqcodeString();
      if (seqcodeString != null)
        info.append(seqcodeString);
      int chainID = getChainID();
      if (chainID != 0 && chainID != 32) {
        info.append(":");
        String s = getChainIDStr();
        if (chainID >= 256)
          s = PT.esc(s);
        info.append(s);
      }
      if (!allInfo)
        return info.toString();
      info.append(".");
    }
    info.append(getAtomName());
    if (info.length() == 0) {
      // since atomName cannot be null, this is unreachable
      info.append(getElementSymbolIso(false));
      info.append(" ");
      info.appendI(getAtomNumber());
    }
    if (altloc != 0) {
      info.append("%");
      info.appendC(altloc);
    }
    if (group.chain.model.ms.mc > 1) {
      info.append("/");
      info.append(getModelNumberForLabel());
    }
    info.append(" #");
    info.appendI(getAtomNumber());
    return info.toString();
  }

  @Override
  public String getGroup3(boolean allowNull) {
    String group3 = group.getGroup3();
    return (allowNull 
        || group3 != null && group3.length() > 0 
        ? group3 : "UNK");
  }

  @Override
  public String getGroup1(char c0) {
    char c = group.getGroup1();
    return (c != '\0' ? "" + c : c0 != '\0' ? "" + c0 : "");
  }

  @Override
  public boolean isProtein() {
    return group.isProtein();
  }

  boolean isCarbohydrate() {
    return group.isCarbohydrate();
  }

  @Override
  public boolean isNucleic() {
    return group.isNucleic();
  }

  @Override
  public boolean isDna() {
    return group.isDna();
  }
  
  @Override
  public boolean isRna() {
    return group.isRna();
  }

  @Override
  public boolean isPurine() {
    return group.isPurine();
  }

  @Override
  public boolean isPyrimidine() {
    return group.isPyrimidine();
  }

  int getSeqcode() {
    return group.seqcode;
  }

  @Override
  public int getResno() {
    return group.getResno();   
  }

  public boolean isClickable() {
    // certainly if it is not visible, then it can't be clickable
    return (checkVisible() && clickabilityFlags != 0 
        && ((shapeVisibilityFlags | group.shapeVisibilityFlags) & clickabilityFlags) != 0);
  }

  public void setClickable(int flag) {
    if (flag == 0)
      clickabilityFlags = 0;
    else
      clickabilityFlags |= flag;
  }
  
  public boolean checkVisible() {
    if (isVisible(JC.ATOM_VISSET))
      return isVisible(JC.ATOM_VISIBLE);
    boolean isVis = isVisible(JC.ATOM_INFRAME_NOTHIDDEN);
    if (isVis) {
      int flags = shapeVisibilityFlags;
      // Is its PDB group visible in any way (cartoon, e.g.)?
      //  An atom is considered visible if its PDB group is visible, even
      //  if it does not show up itself as part of the structure
      //  (this will be a difference in terms of *clickability*).
      // except BACKBONE -- in which case we only see the lead atoms
      if (group.shapeVisibilityFlags != 0
          && (group.shapeVisibilityFlags != JC.VIS_BACKBONE_FLAG || isLeadAtom()))
        flags |= group.shapeVisibilityFlags;
      // We know that (flags & AIM), so now we must remove that flag
      // and check to see if any others are remaining.
      // Only then is the atom considered visible.
      flags &= JC.ATOM_SHAPE_VIS_MASK;
      // problem with display of bond-only when not clickable. 
      // bit of a kludge here.
      if (flags == JC.VIS_BOND_FLAG && clickabilityFlags == 0)
        flags = 0;
      isVis = (flags != 0);
      if (isVis)
        shapeVisibilityFlags |= JC.ATOM_VISIBLE;
    }
    shapeVisibilityFlags |= JC.ATOM_VISSET;
    return isVis;

  }

  @Override
  public boolean isLeadAtom() {
    return group.isLeadAtom(i);
  }
  
  public float getGroupParameter(int tok) {
    return group.getGroupParameter(tok);
  }

  @Override
  public int getChainID() {
    return group.chain.chainID;
  }

  @Override
  public String getChainIDStr() {
    return group.chain.getIDStr();
  }
  
  public int getSurfaceDistance100() {
    return group.chain.model.ms.getSurfaceDistance100(i);
  }

  public V3 getVibrationVector() {
    return group.chain.model.ms.getVibration(i, false);
  }

  public float getVibrationCoord(char ch) {
    return group.chain.model.ms.getVibrationCoord(i, ch);
  }


  public int getPolymerLength() {
    return group.getBioPolymerLength();
  }

  public int getPolymerIndexInModel() {
    return group.getBioPolymerIndexInModel();
  }

  public int getMonomerIndex() {
    return group.getMonomerIndex();
  }
  
  public int getSelectedGroupCountWithinChain() {
    return group.chain.selectedGroupCount;
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

  public Chain getChain() {
    return group.chain;
  }

  public String getModelNumberForLabel() {
    return group.chain.model.ms.getModelNumberForAtomLabel(mi);
  }
  
  public int getModelNumber() {
    return group.chain.model.ms.getModelNumber(mi) % 1000000;
  }
  
  public int getModelFileIndex() {
    return group.chain.model.fileIndex;
  }
  
  public int getModelFileNumber() {
    return group.chain.model.ms.getModelFileNumber(mi);
  }
  
  @Override
  public String getBioStructureTypeName() {
    return getProteinStructureType().getBioStructureTypeName(true);
  }
  
  public STR getProteinStructureType() {
    return group.getProteinStructureType();
  }
  
  public STR getProteinStructureSubType() {
    return group.getProteinStructureSubType();
  }
  
  public int getStrucNo() {
    return group.getStrucNo();
  }

  public String getStructureId() {
    return group.getStructureId();
  }

  public String getProteinStructureTag() {
    return group.getProteinStructureTag();
  }

  public short getGroupID() {
    return group.groupID;
  }

  public String getSeqcodeString() {
    return group.getSeqcodeString();
  }

  public char getInsertionCode() {
    return group.getInsertionCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    return (this == obj);
  }

  @Override
  public int hashCode() {
    //this overrides the Point3fi hashcode, which would
    //give a different hashcode for an atom depending upon
    //its screen location! Bug fix for 11.1.43 Bob Hanson
    return i;
  }
  
  public Atom findAromaticNeighbor(int notAtomIndex) {
    if (bonds == null)
      return null;
    for (int i = bonds.length; --i >= 0; ) {
      Bond bondT = bonds[i];
      Atom a = bondT.getOtherAtom(this);
      if (bondT.isAromatic() && a.i != notAtomIndex)
        return a;
    }
    return null;
  }

  /**
   * called by isosurface and int comparator via atomProperty()
   * and also by getBitsetProperty() 
   * 
   * @param atom
   * @param tokWhat
   * @return         int value or Integer.MIN_VALUE
   */
  public static int atomPropertyInt(Atom atom, int tokWhat) {
    switch (tokWhat) {
    case T.atomno:
      return atom.getAtomNumber();
    case T.atomid:
      return atom.atomID;
    case T.atomindex:
      return atom.i;
    case T.bondcount:
      return atom.getCovalentBondCount();
    case T.chainno:
      return atom.group.chain.index + 1;
    case T.color:
      return atom.group.chain.model.ms.vwr.getColorArgbOrGray(atom.getColix());
    case T.element:
    case T.elemno:
      return atom.getElementNumber();
    case T.elemisono:
      return atom.atomicAndIsotopeNumber;
    case T.file:
      return atom.getModelFileIndex() + 1;
    case T.formalcharge:
      return atom.getFormalCharge();
    case T.groupid:
      return atom.getGroupID(); //-1 if no group
    case T.groupindex:
      return atom.group.getGroupIndex();
    case T.model:
      //integer model number -- could be PDB/sequential adapter number
      //or it could be a sequential model in file number when multiple files
      return atom.getModelNumber();
    case -T.model:
      //float is handled differently
      return atom.getModelFileNumber();
    case T.modelindex:
      return atom.mi;
    case T.molecule:
      return atom.getMoleculeNumber(true);
    case T.occupancy:
      return atom.getOccupancy100();
    case T.polymer:
      return atom.getGroup().getBioPolymerIndexInModel() + 1;
    case T.polymerlength:
      return atom.getPolymerLength();
    case T.radius:
      // the comparator uses rasmol radius, unfortunately, for integers
      return atom.getRasMolRadius();        
    case T.resno:
      return atom.getResno();
    case T.site:
      return atom.getAtomSite();
    case T.structure:
      return atom.getProteinStructureType().getId();
    case T.substructure:
      return atom.getProteinStructureSubType().getId();
    case T.strucno:
      return atom.getStrucNo();
    case T.symop:
      return atom.getSymOp();
    case T.valence:
      return atom.getValence();
    }
    return 0;      
  }

  int getSymOp() {
    return (atomSymmetry == null ? 0 : atomSymmetry.nextSetBit(0) + 1);
  }

  /**
   * called by isosurface and int comparator via atomProperty() and also by
   * getBitsetProperty()
   * 
   * @param vwr
   * 
   * @param atom
   * @param tokWhat
   * @return float value or value*100 (asInt=true) or throw an error if not
   *         found
   * 
   */
  public static float atomPropertyFloat(Viewer vwr, Atom atom, int tokWhat) {
    switch (tokWhat) {
    case T.adpmax:
      return atom.getADPMinMax(true);
    case T.adpmin:
      return atom.getADPMinMax(false);
    case T.atomx:
    case T.x:
      return atom.x;
    case T.atomy:
    case T.y:
      return atom.y;
    case T.atomz:
    case T.z:
      return atom.z;
    case T.backbone:
    case T.cartoon:
    case T.dots:
    case T.ellipsoid:
    case T.geosurface:
    case T.halo:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.star:
    case T.strands:
    case T.trace:
      return vwr.getAtomShapeValue(tokWhat, atom.group, atom.i);
    case T.bondingradius:
      return atom.getBondingRadius();
    case T.chemicalshift:
      return vwr.getNMRCalculation().getChemicalShift(atom);
    case T.covalentradius:
      return Elements.getCovalentRadius(atom.atomicAndIsotopeNumber);
    case T.eta:
    case T.theta:
    case T.straightness:
      return atom.getGroupParameter(tokWhat);
    case T.fracx:
      return atom.getFractionalCoord('X', true);
    case T.fracy:
      return atom.getFractionalCoord('Y', true);
    case T.fracz:
      return atom.getFractionalCoord('Z', true);
    case T.fux:
      return atom.getFractionalCoord('X', false);
    case T.fuy:
      return atom.getFractionalCoord('Y', false);
    case T.fuz:
      return atom.getFractionalCoord('Z', false);
    case T.hydrophobicity:
      return atom.getHydrophobicity();
    case T.magneticshielding:
      return vwr.getNMRCalculation().getMagneticShielding(atom);
    case T.mass:
      return atom.getMass();
    case T.occupancy:
      return atom.getOccupancy100() / 100f;
    case T.partialcharge:
      return atom.getPartialCharge();
    case T.phi:
    case T.psi:
    case T.omega:
      if (atom.group.chain.model.isJmolDataFrame
          && atom.group.chain.model.jmolFrameType
              .startsWith("plot ramachandran")) {
        switch (tokWhat) {
        case T.phi:
          return atom.getFractionalCoord('X', false);
        case T.psi:
          return atom.getFractionalCoord('Y', false);
        case T.omega:
          if (atom.group.chain.model.isJmolDataFrame
              && atom.group.chain.model.jmolFrameType
                  .equals("plot ramachandran")) {
            float omega = atom.getFractionalCoord('Z', false) - 180;
            return (omega < -180 ? 360 + omega : omega);
          }
        }
      }
      return atom.getGroupParameter(tokWhat);
    case T.radius:
    case T.spacefill:
      return atom.getRadius();
    case T.screenx:
      return atom.sX;
    case T.screeny:
      return atom.group.chain.model.ms.vwr.getScreenHeight() - atom.sY;
    case T.screenz:
      return atom.sZ;
    case T.selected:
      return (vwr.isAtomSelected(atom.i) ? 1 : 0);
    case T.surfacedistance:
      atom.group.chain.model.ms.getSurfaceDistanceMax();
      return atom.getSurfaceDistance100() / 100f;
    case T.temperature: // 0 - 9999
      return atom.getBfactor100() / 100f;
    case T.unitx:
      return atom.getFractionalUnitCoord('X');
    case T.unity:
      return atom.getFractionalUnitCoord('Y');
    case T.unitz:
      return atom.getFractionalUnitCoord('Z');
    case T.vanderwaals:
      return atom.getVanderwaalsRadiusFloat(vwr, VDW.AUTO);
    case T.vectorscale:
      V3 v = atom.getVibrationVector();
      return (v == null ? 0 : v.length() * vwr.getFloat(T.vectorscale));
    case T.vibx:
      return atom.getVibrationCoord('X');
    case T.viby:
      return atom.getVibrationCoord('Y');
    case T.vibz:
      return atom.getVibrationCoord('Z');
    case T.volume:
      return atom.getVolume(vwr, VDW.AUTO);
    }
    return atomPropertyInt(atom, tokWhat);
  }

  private float getMass() {
    float mass = getIsotopeNumber();
    return (mass > 0 ? mass : Elements.getAtomicMass(getElementNumber()));
  }

  public static String atomPropertyString(Viewer vwr, Atom atom, int tokWhat) {
    char ch;
    switch (tokWhat) {
    case T.altloc:
      ch = atom.altloc;
      return (ch == '\0' ? "" : "" + ch);
    case T.atomname:
      return atom.getAtomName();
    case T.atomtype:
      return atom.getAtomType();
    case T.chain:
      return atom.getChainIDStr();
    case T.sequence:
      return atom.getGroup1('?');
    case T.group1:
      return atom.getGroup1('\0');
    case T.group:
      return atom.getGroup3(false);
    case T.element:
      return atom.getElementSymbolIso(true);
    case T.identify:
      return atom.getIdentity(true);
    case T.insertion:
      ch = atom.getInsertionCode();
      return (ch == '\0' ? "" : "" + ch);
    case T.label:
    case T.format:
      String s = atom.group.chain.model.ms.getAtomLabel(atom.i);
      if (s == null)
        s = "";
      return s;
    case T.structure:
      return atom.getProteinStructureType().getBioStructureTypeName(false);
    case T.substructure:
      return atom.getProteinStructureSubType().getBioStructureTypeName(false);
    case T.strucid:
      return atom.getStructureId();
    case T.shape:
      return vwr.getHybridizationAndAxes(atom.i, null, null, "d");
    case T.symbol:
      return atom.getElementSymbolIso(false);
    case T.symmetry:
      return atom.getSymmetryOperatorList();
    }
    return ""; 
  }

  public static T3 atomPropertyTuple(Atom atom, int tok) {
    switch (tok) {
    case T.fracxyz:
      return atom.getFractionalCoordPt(!atom.group.chain.model.isJmolDataFrame);
    case T.fuxyz:
      return atom.getFractionalCoordPt(false);
    case T.unitxyz:
      return (atom.group.chain.model.isJmolDataFrame ? atom.getFractionalCoordPt(false) 
          : atom.getFractionalUnitCoordPt(false));
    case T.screenxyz:
      return P3.new3(atom.sX, atom.group.chain.model.ms.vwr.getScreenHeight() - atom.sY, atom.sZ);
    case T.vibxyz:
      V3 v = atom.getVibrationVector();
      if (v == null)
        v = new V3();
      return v;
    case T.xyz:
      return atom;
    case T.color:
      return CU.colorPtFromInt(
          atom.group.chain.model.ms.vwr.getColorArgbOrGray(atom.getColix())
          );
    }
    return null;
  }

  boolean isWithinStructure(STR type) {
    return group.isWithinStructure(type);
  }
  
  @Override
  public int getOffsetResidueAtom(String name, int offset) {
    return group.getAtomIndex(name, offset);
  }
  
  @Override
  public boolean isCrossLinked(BNode node) {
    return group.isCrossLinked(((Atom) node).getGroup());
  }

  @Override
  public boolean getCrossLinkLeadAtomIndexes(List<Integer> vReturn) {
    return group.getCrossLinkLead(vReturn);
  }
  
  @Override
  public String toString() {
    return getInfo();
  }

  public boolean isWithinFourBonds(Atom atomOther) {
    if (mi != atomOther.mi)
      return  false;
    if (isCovalentlyBonded(atomOther))
      return true; 
    Bond[] bondsOther = atomOther.bonds;
    for (int i = 0; i < bondsOther.length; i++) {
      Atom atom2 = bondsOther[i].getOtherAtom(atomOther);
      if (isCovalentlyBonded(atom2))
        return true;
      for (int j = 0; j < bonds.length; j++)
        if (bonds[j].getOtherAtom(this).isCovalentlyBonded(atom2))
          return true;
    }
    return false;
  }

  @Override
  public BS findAtomsLike(String atomExpression) {
    // for SMARTS searching
    return group.chain.model.ms.vwr.getAtomBitSet(atomExpression);
  }

}
