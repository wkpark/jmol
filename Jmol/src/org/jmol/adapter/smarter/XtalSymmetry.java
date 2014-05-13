/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2014-01-10 09:19:33 -0600 (Fri, 10 Jan 2014) $
 * $Revision: 19162 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jmol.adapter.smarter;

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javajs.util.Lst;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P3i;
import javajs.util.PT;
import javajs.util.SB;
import javajs.util.V3;

import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.java.BS;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.Tensor;

/**
 * 
 * A class used by AtomSetCollection for building the symmetry of a 
 * model and generating new atoms based on that symmetry.
 * 
 */
public class XtalSymmetry {

  private AtomSetCollection asc;

  public XtalSymmetry() {
    // for reflection
  }

  public XtalSymmetry set(AtomSetCollection asc) {
    this.asc = asc;
    getSymmetry();
    return this;
  }

  public SymmetryInterface symmetry;

  SymmetryInterface getSymmetry() {
    return (symmetry == null ? (symmetry = Interface.getSymmetry()) : symmetry);
  }
  
  SymmetryInterface setSymmetry(SymmetryInterface symmetry) {
    return (this.symmetry = symmetry);
  }

  private float[] notionalUnitCell = new float[6];
  // expands to 22 for cartesianToFractional matrix as array (PDB)

  private float symmetryRange;
  private boolean doCentroidUnitCell;
  private boolean centroidPacked;
  private float packingError;
  private String filterSymop;


  private void setSymmetryRange(float factor) {
    symmetryRange = factor;
    asc.setInfo("symmetryRange", Float.valueOf(factor));
  }

  private boolean applySymmetryToBonds = false;

  private int[] latticeCells;

  void setLatticeCells(AtomSetCollectionReader acr) {
    
//    int[] latticeCells, boolean applySymmetryToBonds,
//  }
//                       boolean doPackUnitCell, boolean doCentroidUnitCell,
//                       boolean centroidPacked, String strSupercell,
//                       P3 ptSupercell) {
    //set when unit cell is determined
    // x <= 555 and y >= 555 indicate a range of cells to load
    // AROUND the central cell 555 and that
    // we should normalize (z = 1) or pack unit cells (z = -1) or not (z = 0)
    // in addition (Jmol 11.7.36) z = -2 does a full 3x3x3 around the designated cells
    // but then only delivers the atoms that are within the designated cells. 
    // Normalization is the moving of the center of mass into the unit cell.
    // Starting with Jmol 12.0.RC23 we do not normalize a CIF file that 
    // is being loaded without {i j k} indicated.

    latticeCells = acr.latticeCells;
    boolean isLatticeRange = (latticeCells[0] <= 555 && latticeCells[1] >= 555 && (latticeCells[2] == 0
        || latticeCells[2] == 1 || latticeCells[2] == -1));
    doNormalize = latticeCells[0] != 0
        && (!isLatticeRange || latticeCells[2] == 1);
    applySymmetryToBonds = acr.applySymmetryToBonds;
    doPackUnitCell = acr.doPackUnitCell;
    doCentroidUnitCell = acr.doCentroidUnitCell;
    centroidPacked = acr.centroidPacked;
    filterSymop = acr.filterSymop;
    if (acr.strSupercell != null)
      setSuperCell(acr.strSupercell);
    else
      ptSupercell = acr.ptSupercell;
  }

  private P3 ptSupercell;

  public void setSupercellFromPoint(P3 pt) {
    ptSupercell = pt;
    Logger.info("Using supercell " + Escape.eP(pt));
  }

  private float[] fmatSupercell;

  private void setSuperCell(String supercell) {
    if (fmatSupercell != null)
      return;
    fmatSupercell = new float[16];
    if (symmetry.getMatrixFromString(supercell, fmatSupercell, true, 0) == null) {
      fmatSupercell = null;
      return;
    }
    Logger.info("Using supercell \n" + M4.newA16(fmatSupercell));
  }

  private void setNotionalUnitCell(float[] info, M3 matUnitCellOrientation,
                                  P3 unitCellOffset) {
    notionalUnitCell = new float[info.length];
    this.unitCellOffset = unitCellOffset;
    for (int i = 0; i < info.length; i++)
      notionalUnitCell[i] = info[i];
    asc.haveUnitCell = true;
    asc.setAtomSetAuxiliaryInfo("notionalUnitcell", notionalUnitCell);
    asc.setGlobalBoolean(AtomSetCollection.GLOBAL_UNITCELLS);
    getSymmetry().setUnitCell(notionalUnitCell);
    // we need to set the auxiliary info as well, because 
    // ModelLoader creates a new symmetry object.
    if (unitCellOffset != null) {
      symmetry.setOffsetPt(unitCellOffset);
      asc.setAtomSetAuxiliaryInfo("unitCellOffset", unitCellOffset);
    }
    if (matUnitCellOrientation != null) {
      symmetry.setUnitCellOrientation(matUnitCellOrientation);
      asc.setAtomSetAuxiliaryInfo("matUnitCellOrientation", matUnitCellOrientation);
    }
  }

  int addSpaceGroupOperation(AtomSetCollectionReader acr, String xyz) {
    if (acr != null)
      setLatticeCells(acr);
    symmetry.setSpaceGroup(doNormalize);
    return symmetry.addSpaceGroupOperation(xyz, 0);
  }

  public void setLatticeParameter(int latt) {
    symmetry.setSpaceGroup(doNormalize);
    symmetry.setLattice(latt);
  }

  private boolean doNormalize = true;
  private boolean doPackUnitCell = false;

  SymmetryInterface applySymmetryFromReader(AtomSetCollectionReader acr,
                                                   SymmetryInterface readerSymmetry)
      throws Exception {
    asc.setCoordinatesAreFractional(acr.iHaveFractionalCoordinates);
    setNotionalUnitCell(acr.notionalUnitCell, acr.matUnitCellOrientation,
        acr.unitCellOffset);
    asc.setAtomSetSpaceGroupName(acr.sgName);
    setSymmetryRange(acr.symmetryRange); //
    if (acr.doConvertToFractional || acr.fileCoordinatesAreFractional) {
      setLatticeCells(acr);
      boolean doApplySymmetry = true;
      if (acr.ignoreFileSpaceGroupName || !acr.iHaveSymmetryOperators) {
        if (!acr.merging || readerSymmetry == null)
          readerSymmetry = acr.getNewSymmetry();
        doApplySymmetry = readerSymmetry.createSpaceGroup(acr.desiredSpaceGroupIndex,
            (acr.sgName.indexOf("!") >= 0 ? "P1" : acr.sgName),
            acr.notionalUnitCell);
      } else {
        acr.doPreSymmetry();
        readerSymmetry = null;
      }
      if (doApplySymmetry) { 
        if (readerSymmetry != null)
          symmetry.setSpaceGroupFrom(readerSymmetry);
        //parameters are counts of unit cells as [a b c]
        packingError = acr.packingError;
        applySymmetryLattice(acr.ms);
        if (readerSymmetry != null && filterSymop == null)
          asc.setAtomSetSpaceGroupName(readerSymmetry.getSpaceGroupName());
      }
    }
    if (acr.iHaveFractionalCoordinates && acr.merging && readerSymmetry != null) {
      // when merging (with appendNew false), we must return cartesians
      asc.toCartesian(readerSymmetry);
      asc.setCoordinatesAreFractional(false);
      // We no longer allow merging of multiple-model files
      // when the file to be appended has fractional coordinates and vibrations
      acr.addVibrations = false;
    }
    return symmetry;
  }

  private void applySymmetryLattice(MSInterface ms) throws Exception {

    if (!asc.coordinatesAreFractional || symmetry.getSpaceGroup() == null)
      return;

    int maxX = latticeCells[0];
    int maxY = latticeCells[1];
    int maxZ = Math.abs(latticeCells[2]);

    if (fmatSupercell != null) {

      // supercell of the form nx + ny + nz

      // 1) get all atoms for cells necessary

      rminx = Float.MAX_VALUE;
      rminy = Float.MAX_VALUE;
      rminz = Float.MAX_VALUE;
      rmaxx = -Float.MAX_VALUE;
      rmaxy = -Float.MAX_VALUE;
      rmaxz = -Float.MAX_VALUE;

      P3 ptx = setSym(0, 1, 2);
      P3 pty = setSym(4, 5, 6);
      P3 ptz = setSym(8, 9, 10);

      minXYZ = P3i.new3((int) rminx, (int) rminy, (int) rminz);
      maxXYZ = P3i.new3((int) rmaxx, (int) rmaxy, (int) rmaxz);
      applyAllSymmetry(ms);

      // 2) set all atom coordinates to Cartesians

      Atom[] atoms = asc.atoms;
      int atomCount = asc.ac;
      int iAtomFirst = asc.getLastAtomSetAtomIndex();
      for (int i = iAtomFirst; i < atomCount; i++)
        symmetry.toCartesian(atoms[i], true);

      // 3) create the supercell unit cell

      setNotionalUnitCell(new float[] { 0, 0, 0, 0, 0, 0, ptx.x, ptx.y, ptx.z,
          pty.x, pty.y, pty.z, ptz.x, ptz.y, ptz.z }, null,
          (P3) asc.getAtomSetAuxiliaryInfoValue(-1, "unitCellOffset"));
      asc.setAtomSetSpaceGroupName("P1");
      symmetry.setSpaceGroup(doNormalize);
      symmetry.addSpaceGroupOperation("x,y,z", 0);

      // 4) reset atoms to fractional values

      for (int i = iAtomFirst; i < atomCount; i++)
        symmetry.toFractional(atoms[i], true);

      // 5) apply the full lattice symmetry now

      asc.haveAnisou = false;

      // ?? TODO
      asc.setAtomSetAuxiliaryInfo("matUnitCellOrientation", null);
      doPackUnitCell = false; // already done that.
    }

    minXYZ = new P3i();
    maxXYZ = P3i.new3(maxX, maxY, maxZ);
    applyAllSymmetry(ms);
    fmatSupercell = null;
  }

  private P3 setSym(int i, int j, int k) {
    P3 pt = new P3();
    pt.set(fmatSupercell[i], fmatSupercell[j], fmatSupercell[k]);
    setSymmetryMinMax(pt);
    symmetry.toCartesian(pt, false);
    return pt;
  }

  private float rminx, rminy, rminz, rmaxx, rmaxy, rmaxz;

  private void setSymmetryMinMax(P3 c) {
    if (rminx > c.x)
      rminx = c.x;
    if (rminy > c.y)
      rminy = c.y;
    if (rminz > c.z)
      rminz = c.z;
    if (rmaxx < c.x)
      rmaxx = c.x;
    if (rmaxy < c.y)
      rmaxy = c.y;
    if (rmaxz < c.z)
      rmaxz = c.z;
  }

  private boolean isInSymmetryRange(P3 c) {
    return (c.x >= rminx && c.y >= rminy && c.z >= rminz && c.x <= rmaxx
        && c.y <= rmaxy && c.z <= rmaxz);
  }

  private final P3 ptOffset = new P3();

  private P3 unitCellOffset;

  private P3i minXYZ, maxXYZ;
  private P3 minXYZ0, maxXYZ0;

  public boolean isWithinCell(int dtype, P3 pt, float minX, float maxX,
                              float minY, float maxY, float minZ, float maxZ,
                              float slop) {
    return (pt.x > minX - slop && pt.x < maxX + slop
        && (dtype < 2 || pt.y > minY - slop && pt.y < maxY + slop) && (dtype < 3 || pt.z > minZ
        - slop
        && pt.z < maxZ + slop));
  }

  private boolean checkAll;
  private int bondCount0;

  private int symmetryAddAtoms(int transX, int transY, int transZ,
                               int baseCount, int pt, int iCellOpPt,
                               P3[] cartesians, MSInterface ms)
      throws Exception {
    boolean isBaseCell = (baseCount == 0);
    boolean addBonds = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds);
    int[] atomMap = (addBonds ? new int[noSymmetryCount] : null);
    if (doPackUnitCell)
      ptOffset.set(transX, transY, transZ);

    //symmetryRange < 0 : just check symop=1 set
    //symmetryRange > 0 : check against {1 1 1}

    // if we are not checking special atoms, then this is a PDB file
    // and we return all atoms within a cubical volume around the 
    // target set. The user can later use select within() to narrow that down
    // This saves immensely on time.

    float range2 = symmetryRange * symmetryRange;
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    boolean checkSymmetryMinMax = (isBaseCell && checkRange111);
    checkRange111 &= !isBaseCell;
    int nOperations = symmetry.getSpaceGroupOperationCount();
    if (nOperations == 1)
      asc.checkSpecial = false;
    boolean checkSpecial = asc.checkSpecial;
    boolean checkSymmetryRange = (checkRangeNoSymmetry || checkRange111);
    boolean checkDistances = (checkSpecial || checkSymmetryRange);
    boolean addCartesian = (checkSpecial || checkSymmetryMinMax);
    SymmetryInterface symmetry = this.symmetry;
    if (checkRangeNoSymmetry)
      baseCount = noSymmetryCount;
    int atomMax = firstSymmetryAtom + noSymmetryCount;
    P3 ptAtom = new P3();
    String code = null;
    char subSystemId = '\0';
    for (int iSym = 0; iSym < nOperations; iSym++) {
      if (isBaseCell && iSym == 0 || latticeOnly && iSym > 0
          && iSym != latticeOp)
        continue;

      /* pt0 sets the range of points cross-checked. 
       * If we are checking special positions, then we have to check
       *   all previous atoms. 
       * If we are doing a symmetry range check relative to {1 1 1}, then
       *   we have to check only the base set. (checkRange111 true)
       * If we are doing a symmetry range check on symop=1555 (checkRangeNoSymmetry true), 
       *   then we don't check any atoms and just use the box.
       *    
       */

      int pt0 = (checkSpecial ? pt : checkRange111 ? baseCount : 0);
      float spinOp = (asc.vibScale == 0 ? symmetry.getSpinOp(iSym) : asc.vibScale);
      for (int i = firstSymmetryAtom; i < atomMax; i++) {
        Atom a = asc.atoms[i];
        if (a.ignoreSymmetry)
          continue;
        if (asc.bsAtoms != null && !asc.bsAtoms.get(i))
          continue;

        if (ms == null) {
          symmetry.newSpaceGroupPoint(iSym, a, ptAtom, transX, transY,
              transZ);
        } else {
          symmetry = ms.getAtomSymmetry(a, this.symmetry);
          symmetry.newSpaceGroupPoint(iSym, a, ptAtom, transX, transY,
              transZ);
          // COmmensurate structures may use a symmetry operator
          // to changes space groups.
          code = symmetry.getSpaceGroupOperationCode(iSym);
          if (code != null) {
            subSystemId = code.charAt(0);
            symmetry = ms.getSymmetryFromCode(code);
            if (symmetry.getSpaceGroupOperationCount() == 0)
              finalizeSymmetry(symmetry);
          }
        }
        Atom special = null;
        P3 cartesian = P3.newP(ptAtom);
        symmetry.toCartesian(cartesian, false);
        if (doPackUnitCell) {
          // note that COmmensurate structures may need 
          // modulation at this point.
          symmetry.toUnitCell(cartesian, ptOffset);
          ptAtom.setT(cartesian);
          symmetry.toFractional(ptAtom, false);
          if (!isWithinCell(dtype, ptAtom, minXYZ0.x, maxXYZ0.x, minXYZ0.y,
              maxXYZ0.y, minXYZ0.z, maxXYZ0.z, packingError))
            continue;
          
        }

        if (checkSymmetryMinMax)
          setSymmetryMinMax(cartesian);
        if (checkDistances) {

          /* checkSpecial indicates that we are looking for atoms with (nearly) the
           * same cartesian position.  
           */
          float minDist2 = Float.MAX_VALUE;
          if (checkSymmetryRange && !isInSymmetryRange(cartesian))
            continue;
          int j0 = (checkAll ? asc.ac : pt0);
          String name = a.atomName;
          char id = (code == null ? a.altLoc : subSystemId);
          for (int j = j0; --j >= 0;) {
            float d2 = cartesian.distanceSquared(cartesians[j]);
            if (checkSpecial && d2 < 0.0001) {
              special = asc.atoms[firstSymmetryAtom + j];
              if ((special.atomName == null || special.atomName.equals(name))
                  && special.altLoc == id)
                break;
              special = null;
            }
            if (checkRange111 && j < baseCount && d2 < minDist2)
              minDist2 = d2;
          }
          if (checkRange111 && minDist2 > range2)
            continue;
        }
        int atomSite = a.atomSite;
        if (special != null) {
          if (addBonds)
            atomMap[atomSite] = special.index;
          special.bsSymmetry.set(iCellOpPt + iSym);
          special.bsSymmetry.set(iSym);
        } else {
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          Atom atom1 = asc.newCloneAtom(a);
          atom1.setT(ptAtom);
          if (spinOp != 0 && atom1.vib != null) {
            symmetry.getSpaceGroupOperation(iSym).rotate(atom1.vib);
            atom1.vib.scale(spinOp);
          }
          atom1.atomSite = atomSite;
          if (code != null)
            atom1.altLoc = subSystemId;
          atom1.bsSymmetry = BSUtil.newAndSetBit(iCellOpPt + iSym);
          atom1.bsSymmetry.set(iSym);
          if (addCartesian)
            cartesians[pt++] = cartesian;
          Lst<Object> tensors = a.tensors;
          if (tensors != null) {
            atom1.tensors = null;
            for (int j = tensors.size(); --j >= 0;) {
              Tensor t = (Tensor) tensors.get(j);
              if (t == null)
                continue;
              if (nOperations == 1)
                atom1.addTensor(t.copyTensor(), null, false);
              else
                addRotatedTensor(atom1, t, iSym, false, symmetry);
            }
          }
        }
      }
      if (addBonds) {
        // Clone bonds
        Bond[] bonds = asc.bonds;
        Atom[] atoms = asc.atoms;
        for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
          Bond bond = bonds[bondNum];
          Atom atom1 = atoms[bond.atomIndex1];
          Atom atom2 = atoms[bond.atomIndex2];
          if (atom1 == null || atom2 == null)
            continue;
          int iAtom1 = atomMap[atom1.atomSite];
          int iAtom2 = atomMap[atom2.atomSite];
          if (iAtom1 >= atomMax || iAtom2 >= atomMax)
            asc.addNewBondWithOrder(iAtom1, iAtom2, bond.order);
        }
      }
    }
    return pt;
  }

  /**
   * @param ms
   *        modulated structure interface
   * @throws Exception
   */
  private void applyAllSymmetry(MSInterface ms) throws Exception {
    if (asc.ac == 0)
      return;
    noSymmetryCount = (asc.baseSymmetryAtomCount == 0 ? asc.getLastAtomSetAtomCount()
        : asc.baseSymmetryAtomCount);
    firstSymmetryAtom = asc.getLastAtomSetAtomIndex();
    asc.setTensors();
    bondCount0 = asc.bondCount;
    finalizeSymmetry(symmetry);
    int operationCount = symmetry.getSpaceGroupOperationCount();
    dtype = (int) symmetry.getUnitCellInfoType(SimpleUnitCell.INFO_DIMENSIONS);
    symmetry.setMinMaxLatticeParameters(minXYZ, maxXYZ);
    if (doCentroidUnitCell)
      asc.setInfo("centroidMinMax", new int[] { minXYZ.x,
          minXYZ.y, minXYZ.z, maxXYZ.x, maxXYZ.y, maxXYZ.z,
          (centroidPacked ? 1 : 0) });
    if (ptSupercell != null) {
      asc.setAtomSetAuxiliaryInfo("supercell", ptSupercell);
      switch (dtype) {
      case 3:
        // standard
        minXYZ.z *= (int) Math.abs(ptSupercell.z);
        maxXYZ.z *= (int) Math.abs(ptSupercell.z);
        //$FALL-THROUGH$;
      case 2:
        // slab or standard
        minXYZ.y *= (int) Math.abs(ptSupercell.y);
        maxXYZ.y *= (int) Math.abs(ptSupercell.y);
        //$FALL-THROUGH$;
      case 1:
        // slab, polymer, or standard
        minXYZ.x *= (int) Math.abs(ptSupercell.x);
        maxXYZ.x *= (int) Math.abs(ptSupercell.x);
      }
    }
    if (doCentroidUnitCell || doPackUnitCell || symmetryRange != 0
        && maxXYZ.x - minXYZ.x == 1 && maxXYZ.y - minXYZ.y == 1
        && maxXYZ.z - minXYZ.z == 1) {
      // weird Mac bug does not allow   Point3i.new3(minXYZ) !!
      minXYZ0 = P3.new3(minXYZ.x, minXYZ.y, minXYZ.z);
      maxXYZ0 = P3.new3(maxXYZ.x, maxXYZ.y, maxXYZ.z);
      if (ms != null) {
        ms.setMinMax0(minXYZ0, maxXYZ0);
        minXYZ.set((int) minXYZ0.x, (int) minXYZ0.y, (int) minXYZ0.z);
        maxXYZ.set((int) maxXYZ0.x, (int) maxXYZ0.y, (int) maxXYZ0.z);
      }
      switch (dtype) {
      case 3:
        // standard
        minXYZ.z--;
        maxXYZ.z++;
        //$FALL-THROUGH$;
      case 2:
        // slab or standard
        minXYZ.y--;
        maxXYZ.y++;
        //$FALL-THROUGH$;
      case 1:
        // slab, polymer, or standard
        minXYZ.x--;
        maxXYZ.x++;
      }
    }
    int nCells = (maxXYZ.x - minXYZ.x) * (maxXYZ.y - minXYZ.y)
        * (maxXYZ.z - minXYZ.z);
    int cartesianCount = (asc.checkSpecial ? noSymmetryCount * operationCount
        * nCells : symmetryRange > 0 ? noSymmetryCount * operationCount // checking
    // against
    // {1 1
    // 1}
        : symmetryRange < 0 ? 1 // checking against symop=1555 set; just a box
            : 1 // not checking
    );
    P3[] cartesians = new P3[cartesianCount];
    for (int i = 0; i < noSymmetryCount; i++)
      asc.atoms[i + firstSymmetryAtom].bsSymmetry = BS.newN(operationCount
          * (nCells + 1));
    int pt = 0;
    int[] unitCells = new int[nCells];
    unitCellTranslations = new V3[nCells];
    int iCell = 0;
    int cell555Count = 0;
    float absRange = Math.abs(symmetryRange);
    boolean checkSymmetryRange = (symmetryRange != 0);
    boolean checkRangeNoSymmetry = (symmetryRange < 0);
    boolean checkRange111 = (symmetryRange > 0);
    if (checkSymmetryRange) {
      rminx = Float.MAX_VALUE;
      rminy = Float.MAX_VALUE;
      rminz = Float.MAX_VALUE;
      rmaxx = -Float.MAX_VALUE;
      rmaxy = -Float.MAX_VALUE;
      rmaxz = -Float.MAX_VALUE;
    }
    // always do the 555 cell first

    // incommensurate symmetry can have lattice centering, resulting in 
    // duplication of operators. There's a bug later on that requires we 
    // only do this with the first atom set for now, at least.
    SymmetryInterface symmetry = this.symmetry;
    SymmetryInterface lastSymmetry = symmetry;
    latticeOp = symmetry.getLatticeOp();
    checkAll = (asc.atomSetCount == 1 && asc.checkSpecial && latticeOp >= 0);
    latticeOnly = (asc.checkLatticeOnly && latticeOp >= 0); // CrystalReader

    M4 op = symmetry.getSpaceGroupOperation(0);
    if (doPackUnitCell)
      ptOffset.set(0, 0, 0);
    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          unitCellTranslations[iCell] = V3.new3(tx, ty, tz);
          unitCells[iCell++] = 555 + tx * 100 + ty * 10 + tz;
          if (tx != 0 || ty != 0 || tz != 0 || cartesians.length == 0)
            continue;

          // base cell only

          for (pt = 0; pt < noSymmetryCount; pt++) {
            Atom atom = asc.atoms[firstSymmetryAtom + pt];

            if (ms != null) {
              symmetry = ms.getAtomSymmetry(atom, this.symmetry);
              if (symmetry != lastSymmetry) {
                if (symmetry.getSpaceGroupOperationCount() == 0)
                  finalizeSymmetry(lastSymmetry = symmetry);
                op = symmetry.getSpaceGroupOperation(0);
              }
            }

            P3 c = P3.newP(atom);
            op.rotTrans(c);
            symmetry.toCartesian(c, false);
            if (doPackUnitCell) {
              symmetry.toUnitCell(c, ptOffset);
              atom.setT(c);
              symmetry.toFractional(atom, false);
            }
            atom.bsSymmetry.set(iCell * operationCount);
            atom.bsSymmetry.set(0);
            if (checkSymmetryRange)
              setSymmetryMinMax(c);
            if (pt < cartesianCount)
              cartesians[pt] = c;
          }
          if (checkRangeNoSymmetry) {
            rminx -= absRange;
            rminy -= absRange;
            rminz -= absRange;
            rmaxx += absRange;
            rmaxy += absRange;
            rmaxz += absRange;
          }
          cell555Count = pt = symmetryAddAtoms(0, 0, 0, 0, pt, iCell
              * operationCount, cartesians, ms);
        }
    if (checkRange111) {
      rminx -= absRange;
      rminy -= absRange;
      rminz -= absRange;
      rmaxx += absRange;
      rmaxy += absRange;
      rmaxz += absRange;
    }

    // now apply all the translations
    iCell = 0;
    for (int tx = minXYZ.x; tx < maxXYZ.x; tx++)
      for (int ty = minXYZ.y; ty < maxXYZ.y; ty++)
        for (int tz = minXYZ.z; tz < maxXYZ.z; tz++) {
          iCell++;
          if (tx != 0 || ty != 0 || tz != 0)
            pt = symmetryAddAtoms(tx, ty, tz, cell555Count, pt, iCell
                * operationCount, cartesians, ms);
        }
    if (iCell * noSymmetryCount == asc.ac - firstSymmetryAtom)
      appendAtomProperties(iCell);
    setSymmetryOps();
    asc.setAtomSetAuxiliaryInfo("presymmetryAtomIndex",
        Integer.valueOf(firstSymmetryAtom));
    asc.setAtomSetAuxiliaryInfo("presymmetryAtomCount",
        Integer.valueOf(noSymmetryCount));
    asc.setAtomSetAuxiliaryInfo("latticeDesignation",
        symmetry.getLatticeDesignation());
    asc.setAtomSetAuxiliaryInfo("unitCellRange", unitCells);
    asc.setAtomSetAuxiliaryInfo("unitCellTranslations", unitCellTranslations);
    notionalUnitCell = new float[6];
    reset();
  }

  @SuppressWarnings("unchecked")
  private void appendAtomProperties(int nTimes) {
    Map<String, String> p = (Map<String, String>) asc.getAtomSetAuxiliaryInfoValue(
        -1, "atomProperties");
    if (p == null) {
      return;
    }
    for (Map.Entry<String, String> entry : p.entrySet()) {
      String key = entry.getKey();
      String data = entry.getValue();
      SB s = new SB();
      for (int i = nTimes; --i >= 0;)
        s.append(data);
      p.put(key, s.toString());
    }
  }

  private void finalizeSymmetry(SymmetryInterface symmetry) {
    String name = (String) asc.getAtomSetAuxiliaryInfoValue(-1, "spaceGroup");
    symmetry.setFinalOperations(name, asc.atoms, firstSymmetryAtom,
        noSymmetryCount, doNormalize, filterSymop);
    if (filterSymop != null || name == null || name.equals("unspecified!"))
      asc.setAtomSetSpaceGroupName(symmetry.getSpaceGroupName());
  }

  private void setSymmetryOps() {
    int operationCount = symmetry.getSpaceGroupOperationCount();
    if (operationCount > 0) {
      String[] symmetryList = new String[operationCount];
      for (int i = 0; i < operationCount; i++)
        symmetryList[i] = "" + symmetry.getSpaceGroupXyz(i, doNormalize);
      asc.setAtomSetAuxiliaryInfo("symmetryOperations", symmetryList);
    }
    asc.setAtomSetAuxiliaryInfo("symmetryCount", Integer.valueOf(operationCount));
  }


  private int dtype = 3;
  private V3[] unitCellTranslations;
  private int latticeOp;
  private boolean latticeOnly;
  private int noSymmetryCount;
  private int firstSymmetryAtom;

  private final static int PARTICLE_NONE = 0;
  private final static int PARTICLE_CHAIN = 1;
  private final static int PARTICLE_SYMOP = 2;

  @SuppressWarnings("unchecked")
  public void applySymmetryBio(Map<String, Object> thisBiomolecule,
                               float[] notionalUnitCell,
                               boolean applySymmetryToBonds, String filter) {
    if (latticeCells != null && latticeCells[0] != 0) {
      Logger.error("Cannot apply biomolecule when lattice cells are indicated");
      return;
    }
    int particleMode = (filter.indexOf("BYCHAIN") >= 0 ? PARTICLE_CHAIN
        : filter.indexOf("BYSYMOP") >= 0 ? PARTICLE_SYMOP : PARTICLE_NONE);

    doNormalize = false;
    Lst<M4> biomts = (Lst<M4>) thisBiomolecule.get("biomts");
    if (biomts.size() < 2)
      return;
    symmetry = null;
    if (!Float.isNaN(notionalUnitCell[0])) // PDB can do this; 
      setNotionalUnitCell(notionalUnitCell, null, unitCellOffset);
    getSymmetry().setSpaceGroup(doNormalize);
    //symmetry.setUnitCell(null);
    addSpaceGroupOperation(null, "x,y,z");
    String name = (String) thisBiomolecule.get("name");
    asc.setAtomSetSpaceGroupName(name);
    int len = biomts.size();
    this.applySymmetryToBonds = applySymmetryToBonds;
    bondCount0 = asc.bondCount;
    boolean addBonds = (bondCount0 > asc.bondIndex0 && applySymmetryToBonds);
    int[] atomMap = (addBonds ? new int[asc.ac] : null);
    firstSymmetryAtom = asc.getLastAtomSetAtomIndex();
    int atomMax = asc.ac;
    Map<Integer, BS> ht = new Hashtable<Integer, BS>();
    int nChain = 0;
    Atom[] atoms = asc.atoms;
    switch (particleMode) {
    case PARTICLE_CHAIN:
      for (int i = atomMax; --i >= firstSymmetryAtom;) {
        Integer id = Integer.valueOf(atoms[i].chainID);
        BS bs = ht.get(id);
        if (bs == null) {
          nChain++;
          ht.put(id, bs = new BS());
        }
        bs.set(i);
      }
      asc.bsAtoms = new BS();
      for (int i = 0; i < nChain; i++) {
        asc.bsAtoms.set(atomMax + i);
        Atom a = new Atom();
        a.set(0, 0, 0);
        a.radius = 16;
        asc.addAtom(a);
      }
      int ichain = 0;
      for (Entry<Integer, BS> e : ht.entrySet()) {
        Atom a = atoms[atomMax + ichain++];
        BS bs = e.getValue();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
          a.add(atoms[i]);
        a.scale(1f / bs.cardinality());
        a.atomName = "Pt" + ichain;
        a.chainID = e.getKey().intValue();
      }
      firstSymmetryAtom = atomMax;
      atomMax += nChain;
      break;
    case PARTICLE_SYMOP:
      asc.bsAtoms = new BS();
      asc.bsAtoms.set(atomMax);
      Atom a = atoms[atomMax] = new Atom();
      a.set(0, 0, 0);
      for (int i = atomMax; --i >= firstSymmetryAtom;)
        a.add(atoms[i]);
      a.scale(1f / (atomMax - firstSymmetryAtom));
      a.atomName = "Pt";
      a.radius = 16;
      firstSymmetryAtom = atomMax++;
      break;
    }
    if (filter.indexOf("#<") >= 0) {
      len = Math.min(len,
          PT.parseInt(filter.substring(filter.indexOf("#<") + 2)) - 1);
      filter = PT.rep(filter, "#<", "_<");
    }
    for (int iAtom = firstSymmetryAtom; iAtom < atomMax; iAtom++)
      atoms[iAtom].bsSymmetry = BSUtil.newAndSetBit(0);
    for (int i = 1; i < len; i++) {
      if (filter.indexOf("!#") >= 0) {
        if (filter.indexOf("!#" + (i + 1) + ";") >= 0)
          continue;
      } else if (filter.indexOf("#") >= 0
          && filter.indexOf("#" + (i + 1) + ";") < 0) {
        continue;
      }
      M4 mat = biomts.get(i);
      //Vector3f trans = new Vector3f();    
      for (int iAtom = firstSymmetryAtom; iAtom < atomMax; iAtom++) {
        if (asc.bsAtoms != null && !asc.bsAtoms.get(iAtom))
          continue;
        try {
          int atomSite = atoms[iAtom].atomSite;
          Atom atom1;
          if (addBonds)
            atomMap[atomSite] = asc.ac;
          atom1 = asc.newCloneAtom(atoms[iAtom]);
          if (asc.bsAtoms != null)
            asc.bsAtoms.set(atom1.index);
          atom1.atomSite = atomSite;
          mat.rotTrans(atom1);
          atom1.bsSymmetry = BSUtil.newAndSetBit(i);
          if (addBonds) {
            // Clone bonds
            for (int bondNum = asc.bondIndex0; bondNum < bondCount0; bondNum++) {
              Bond bond = asc.bonds[bondNum];
              int iAtom1 = atomMap[atoms[bond.atomIndex1].atomSite];
              int iAtom2 = atomMap[atoms[bond.atomIndex2].atomSite];
              if (iAtom1 >= atomMax || iAtom2 >= atomMax)
                asc.addNewBondWithOrder(iAtom1, iAtom2, bond.order);
            }
          }
        } catch (Exception e) {
          asc.errorMessage = "appendAtomCollection error: " + e;
        }
      }
      //      mat.m03 /= notionalUnitCell[0]; // PDB could have set this to Float.NaN
      //      if (Float.isNaN(mat.m03))
      //        mat.m03 = 1;
      //      mat.m13 /= notionalUnitCell[1];
      //      mat.m23 /= notionalUnitCell[2];
      if (i > 0)
        symmetry.addBioMoleculeOperation(mat, false);
    }
    noSymmetryCount = atomMax - firstSymmetryAtom;
    asc.setAtomSetAuxiliaryInfo("presymmetryAtomIndex",
        Integer.valueOf(firstSymmetryAtom));
    asc.setAtomSetAuxiliaryInfo("presymmetryAtomCount",
        Integer.valueOf(noSymmetryCount));
    asc.setAtomSetAuxiliaryInfo("biosymmetryCount", Integer.valueOf(len));
    asc.setAtomSetAuxiliaryInfo("biosymmetry", symmetry);
    finalizeSymmetry(symmetry);
    setSymmetryOps();
    reset();
    //TODO: need to clone bonds
  }

  private void reset() {
    asc.coordinatesAreFractional = false;
    asc.setAtomSetAuxiliaryInfo("hasSymmetry", Boolean.TRUE);
    asc.setGlobalBoolean(AtomSetCollection.GLOBAL_SYMMETRY);
  }

  private P3 ptTemp;
  private M3 mTemp;

  public Tensor addRotatedTensor(Atom a, Tensor t, int iSym, boolean reset,
                                 SymmetryInterface symmetry) {
    if (ptTemp == null) {
      ptTemp = new P3();
      mTemp = new M3();
    }
    return a.addTensor(((Tensor) Interface.getUtil("Tensor"))
        .setFromEigenVectors(
            symmetry.rotateAxes(iSym, t.eigenVectors, ptTemp, mTemp),
            t.eigenValues, t.isIsotropic ? "iso" : t.type, t.id, t), null, reset);
  }

  void setTensors() {
    int n = asc.ac;
    for (int i = asc.getLastAtomSetAtomIndex(); i < n; i++) {
      Atom a = asc.atoms[i];
      if (a.anisoBorU == null)
        continue;
      // getTensor will return correct type
      a.addTensor(symmetry.getTensor(a.anisoBorU), null, false);
      if (Float.isNaN(a.bfactor))
        a.bfactor = a.anisoBorU[7] * 100f;
      // prevent multiple additions
      a.anisoBorU = null;
    }
  }

  public void setTimeReversal(int op, int timeRev) {
    symmetry.setTimeReversal(op, timeRev);
  }

}
