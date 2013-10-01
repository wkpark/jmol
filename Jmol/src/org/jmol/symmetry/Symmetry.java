/* $RCSfiodelle$allrueFFFF
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
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
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package org.jmol.symmetry;

import java.util.Hashtable;
import java.util.Map;


import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.modelset.ModelSet;
import org.jmol.script.T;
import org.jmol.util.BS;
import org.jmol.util.Escape;
import org.jmol.util.JmolList;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Matrix3f;
import org.jmol.util.Matrix4f;
import org.jmol.util.P3;
import org.jmol.util.P3i;
import org.jmol.util.Tensor;
import org.jmol.util.Logger;
import org.jmol.util.SimpleUnitCell;
import org.jmol.util.SB;
import org.jmol.util.Tuple3f;
import org.jmol.util.V3;

public class Symmetry implements SymmetryInterface {
  
  // NOTE: THIS CLASS IS VERY IMPORTANT.
  // IN ORDER TO MODULARIZE IT, IT IS REFERENCED USING 
  // xxxx = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");

  /* Symmetry is a wrapper class that allows access to the package-local
   * classes PointGroup, SpaceGroup, SymmetryInfo, and UnitCell.
   * 
   * When symmetry is detected in ANY model being loaded, a SymmetryInterface
   * is established for ALL models.
   * 
   * The SpaceGroup information could be saved with each model, but because this 
   * depends closely on what atoms have been selected, and since tracking that with atom
   * deletion is a bit complicated, instead we just use local instances of that class.
   * 
   * The three PointGroup methods here could be their own interface; they are just here
   * for convenience.
   * 
   * The file readers use SpaceGroup and UnitCell methods
   * 
   * The modelSet and modelLoader classes use UnitCell and SymmetryInfo 
   * 
   */
  private PointGroup pointGroup;
  private SpaceGroup spaceGroup;
  private SymmetryInfo symmetryInfo;
  private UnitCell unitCell;
  
  public Symmetry() {
    // instantiated ONLY using
    // symmetry = (SymmetryInterface) Interface.getOptionInterface("symmetry.Symmetry");
    // DO NOT use symmetry = new Symmetry();
    // as that will invalidate the Jar file modularization    
  }
  
  public SymmetryInterface setPointGroup(SymmetryInterface siLast,
                                         Atom[] atomset, BS bsAtoms,
                                         boolean haveVibration,
                                         float distanceTolerance,
                                         float linearTolerance) {
    pointGroup = PointGroup.getPointGroup(siLast == null ? null
        : ((Symmetry) siLast).pointGroup, atomset, bsAtoms, haveVibration,
        distanceTolerance, linearTolerance);
    return this;
  }
  
  public String getPointGroupName() {
    return pointGroup.getName();
  }

  public Object getPointGroupInfo(int modelIndex, boolean asDraw,
                                  boolean asInfo, String type, int index,
                                  float scale) {
    if (!asDraw && !asInfo && pointGroup.textInfo != null)
      return pointGroup.textInfo;
    else if (asDraw && pointGroup.isDrawType(type, index, scale))
      return pointGroup.drawInfo;
    else if (asInfo && pointGroup.info != null)
      return pointGroup.info;
    return pointGroup.getInfo(modelIndex, asDraw, asInfo, type, index, scale);
  }

  // SpaceGroup methods
  
  public void setSpaceGroup(boolean doNormalize) {
    if (spaceGroup == null)
      spaceGroup = (SpaceGroup.getNull()).set(doNormalize);
  }

  public int addSpaceGroupOperation(String xyz, int opId) {
    return spaceGroup.addSymmetry(xyz, opId);
  }

  public void addSpaceGroupOperationM(Matrix4f mat) {
    spaceGroup.addSymmetry("=" + 
        SymmetryOperation.getXYZFromMatrix(mat, false, false, false), 0);    
  }

  public void setLattice(int latt) {
    spaceGroup.setLatticeParam(latt);
  }

  public String getSpaceGroupName() {
    return (symmetryInfo != null ? symmetryInfo.spaceGroup
        : spaceGroup != null ? spaceGroup.getName() : "");
  }

  public Object getSpaceGroup() {
    return spaceGroup;
  }
  
  public void setSpaceGroupS(SymmetryInterface symmetry) {
    spaceGroup = (symmetry == null ? null : (SpaceGroup) symmetry.getSpaceGroup());
  }

  public boolean createSpaceGroup(int desiredSpaceGroupIndex, String name,
                                  float[] notionalUnitCell) {
    spaceGroup = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex, name,
        notionalUnitCell);
    if (spaceGroup != null && Logger.debugging)
      Logger.debug("using generated space group " + spaceGroup.dumpInfo(null));
    return spaceGroup != null;
  }

  public boolean haveSpaceGroup() {
    return (spaceGroup != null);
  }
  
  

  public String getSpaceGroupInfo(String name, SymmetryInterface cellInfo) {
    return SpaceGroup.getInfo(name, cellInfo);
  }

  public Object getLatticeDesignation() {
    return spaceGroup.getLatticeDesignation();
  }

  public void setFinalOperations(P3[] atoms, int iAtomFirst, int noSymmetryCount, boolean doNormalize) {
    spaceGroup.setFinalOperations(atoms, iAtomFirst, noSymmetryCount, doNormalize);
  }

  public int getSpaceGroupOperationCount() {
    return spaceGroup.finalOperations.length;
  }  
  
  public Matrix4f getSpaceGroupOperation(int i) {
    return spaceGroup.finalOperations[i];
  }
  

  public String getSpaceGroupXyz(int i, boolean doNormalize) {
    return spaceGroup.finalOperations[i].getXyz(doNormalize);
  }

  public void newSpaceGroupPoint(int i, P3 atom1, P3 atom2,
                       int transX, int transY, int transZ) {
    if (spaceGroup.finalOperations == null) {
      // temporary spacegroups don't have to have finalOperations
      if (!spaceGroup.operations[i].isFinalized)
        spaceGroup.operations[i].doFinalize();
      spaceGroup.operations[i].newPoint(atom1, atom2, transX, transY, transZ);
      return;
    }
    spaceGroup.finalOperations[i].newPoint(atom1, atom2, transX, transY, transZ);
  }
    
  public V3[] rotateAxes(int iop, V3[] axes, P3 ptTemp, Matrix3f mTemp) {
    return (iop == 0 ? axes : spaceGroup.finalOperations[iop].rotateAxes(axes, unitCell, ptTemp, mTemp));
  }

  public Object[] getSymmetryOperationDescription(ModelSet modelSet,
                                                  int isym,
                                                  SymmetryInterface cellInfo, P3 pt1,
                                                  P3 pt2, String id) {
    return spaceGroup.operations[isym].getDescription(modelSet, cellInfo, pt1, pt2, id);
  }
    
  public String fcoord(Tuple3f p) {
    return SymmetryOperation.fcoord(p);
  }

  public String getMatrixFromString(String xyz, float[] rotTransMatrix, boolean allowScaling) {
    return SymmetryOperation.getMatrixFromString(null, xyz, rotTransMatrix, allowScaling);
  }

  public P3 ijkToPoint3f(int nnn) {
    P3 cell = new P3();
    SimpleUnitCell.ijkToPoint3f(nnn, cell, 0);
    return cell;
  }

  /// symmetryInfo ////
  
  public boolean getCoordinatesAreFractional() {
    return symmetryInfo.coordinatesAreFractional;
  }

  public int[] getCellRange() {
    return symmetryInfo.cellRange;
  }

  public String getSymmetryInfoString() {
    return symmetryInfo.symmetryInfoString;
  }

  public String[] getSymmetryOperations() {
    return symmetryInfo.symmetryOperations;
  }

  public boolean isPeriodic() {
    return (symmetryInfo == null || symmetryInfo.isPeriodic());
  }

  public void setSymmetryInfo(int modelIndex, Map<String, Object> modelAuxiliaryInfo) {
    symmetryInfo = new SymmetryInfo();
    float[] notionalUnitcell = symmetryInfo.setSymmetryInfo(modelAuxiliaryInfo);
    if (notionalUnitcell == null)
      return;
    setUnitCell(notionalUnitcell);
    modelAuxiliaryInfo.put("infoUnitCell", getUnitCellAsArray(false));
    setOffsetPt((P3) modelAuxiliaryInfo.get("unitCellOffset"));
    if (modelAuxiliaryInfo.containsKey("jmolData"))
      setUnitCellAllFractionalRelative(true);
    Matrix3f matUnitCellOrientation = (Matrix3f) modelAuxiliaryInfo.get("matUnitCellOrientation");
    if (matUnitCellOrientation != null)
      setUnitCellOrientation(matUnitCellOrientation);
    if (Logger.debugging)
      Logger
          .debug("symmetryInfos[" + modelIndex + "]:\n" + unitCell.dumpInfo(true));
  }

  // UnitCell methods
  
  public void setUnitCell(float[] notionalUnitCell) {
    unitCell = UnitCell.newA(notionalUnitCell);
  }

  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  public String getUnitsymmetryInfo() {
    // not used in Jmol?
    return unitCell.dumpInfo(false);
  }

  public void setUnitCellOrientation(Matrix3f matUnitCellOrientation) {
      unitCell.setOrientation(matUnitCellOrientation);
  }

  public void unitize(P3 ptFrac) {
    unitCell.unitize(ptFrac);
  }

  public void toUnitCell(P3 pt, P3 offset) {
    unitCell.toUnitCell(pt, offset);
  }

  public void toCartesian(Tuple3f fpt, boolean isAbsolute) {
    unitCell.toCartesian(fpt, isAbsolute);    
  }

  public P3 toSupercell(P3 fpt) {
    return unitCell.toSupercell(fpt);    
  }

  public void toFractional(Tuple3f pt, boolean isAbsolute) {
    unitCell.toFractional(pt, isAbsolute);
  }

  public float[] getNotionalUnitCell() {
    return unitCell.getNotionalUnitCell();
  }

  public float[] getUnitCellAsArray(boolean vectorsOnly) {
    return unitCell.getUnitCellAsArray(vectorsOnly);
  }

  public Tensor getTensor(float[] parBorU) {
    if (unitCell == null)
      unitCell = UnitCell.newA(new float[] {1,1,1,90,90,90});
    return unitCell.getTensor(parBorU);
  }
  
  public P3[] getUnitCellVertices() {
    return unitCell.getVertices();
  }

  public P3 getCartesianOffset() {
    return unitCell.getCartesianOffset();
  }

  public void setCartesianOffset(Tuple3f origin) {
    unitCell.setCartesianOffset(origin);
  }

  public P3 getFractionalOffset() {
    return unitCell.getFractionalOffset();
  }

  public void setOffsetPt(P3 pt) {
    unitCell.setOffset(pt);
  }

  public void setOffset(int nnn) {
    unitCell.setOffset(ijkToPoint3f(nnn));
  }

  public P3 getUnitCellMultiplier() {
    return unitCell.getUnitCellMultiplier();
  }

  public P3[] getCanonicalCopy(float scale) {
    return unitCell.getCanonicalCopy(scale);
  }

  public float getUnitCellInfoType(int infoType) {
    return unitCell.getInfo(infoType);
  }

  public String getUnitCellInfo() {
    return unitCell.dumpInfo(false);
  }

  public boolean isSlab() {
    return unitCell.isSlab();
  }

  public boolean isPolymer() {
    return unitCell.isPolymer();
  }

  public void setMinMaxLatticeParameters(P3i minXYZ, P3i maxXYZ) {
    unitCell.setMinMaxLatticeParameters(minXYZ, maxXYZ);
  }

  public void setUnitCellAllFractionalRelative(boolean TF) {
    unitCell.setAllFractionalRelative(TF);
  }

  public boolean checkDistance(P3 f1, P3 f2, float distance, float dx, 
                               int iRange, int jRange, int kRange, P3 ptOffset) {
    return unitCell.checkDistance(f1, f2, distance, dx, 
        iRange, jRange, kRange, ptOffset);
  }

  public P3[] getUnitCellVectors() {
    return unitCell.getUnitCellVectors();
  }

  public SymmetryInterface getUnitCell(Tuple3f[] points) {
    Symmetry sym = new Symmetry();
    sym.unitCell = UnitCell.newP(points);
    return sym;
  }

  public boolean isSupercell() {
    return unitCell.isSupercell();
  }

  public String getSymmetryOperationInfo(Map<String, Object> sginfo, int symOp, String drawID, boolean labelOnly) {
    Object[][] infolist = (Object[][]) sginfo.get("operations");
    if (infolist == null)
      return "";
    SB sb = new SB();
    symOp--;
    for (int i = 0; i < infolist.length; i++) {
      if (infolist[i] == null || symOp >= 0 && symOp != i)
        continue;
      if (drawID != null)
        return (String) infolist[i][3];
      if (sb.length() > 0)
        sb.appendC('\n');
      if (!labelOnly) {
        if (symOp < 0)
          sb.appendI(i + 1).append("\t");
        sb.append((String)infolist[i][0]).append("\t"); //xyz
      }
      sb.append((String)infolist[i][2]); //desc
    }
    if (sb.length() == 0 && drawID != null)
      sb.append("draw " + drawID + "* delete");
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getSpaceGroupInfo(ModelSet modelSet, int modelIndex,
                                               String spaceGroup, int symOp,
                                               P3 pt1, P3 pt2,
                                               String drawID) {
    String strOperations = null;
    Map<String, Object> info = null;
    SymmetryInterface cellInfo = null;
    Object[][] infolist = null;
    if (spaceGroup == null) {
      if (modelIndex <= 0)
        modelIndex = (pt1 instanceof Atom ? ((Atom) pt1).modelIndex : modelSet.viewer
            .getCurrentModelIndex());
      if (modelIndex < 0)
        strOperations = "no single current model";
      else if ((cellInfo = modelSet.getUnitCell(modelIndex)) == null)
        strOperations = "not applicable";
      if (strOperations != null) {
        info = new Hashtable<String, Object>();
        info.put("spaceGroupInfo", strOperations);
        info.put("symmetryInfo", "");
      } else if (pt1 == null && drawID == null && symOp != 0) {
        info = (Map<String, Object>) modelSet.getModelAuxiliaryInfoValue(modelIndex,
            "spaceGroupInfo");
      }
      if (info != null)
        return info;
      info = new Hashtable<String, Object>();
      if (pt1 == null && drawID == null && symOp == 0)
        modelSet.setModelAuxiliaryInfo(modelIndex, "spaceGroupInfo", info);
      spaceGroup = cellInfo.getSpaceGroupName();
      String[] list = cellInfo.getSymmetryOperations();
      String jf = "";
      if (list == null) {
        strOperations = "\n no symmetry operations employed";
      } else {
        setSpaceGroup(false);
        strOperations = "\n" + list.length + " symmetry operations employed:";
        infolist = new Object[list.length][];
        for (int i = 0; i < list.length; i++) {
          int iSym = addSpaceGroupOperation("=" + list[i], i + 1);
          if (iSym < 0)
            continue;
          jf += ";" + list[i];
          infolist[i] = (symOp > 0 && symOp - 1 != iSym ? null
              : getSymmetryOperationDescription(modelSet, iSym, cellInfo,
                  pt1, pt2, drawID));
          if (infolist[i] != null)
            strOperations += "\n" + (i + 1) + "\t" + infolist[i][0] + "\t"
                + infolist[i][2];
        }
      }
      jf = jf.substring(jf.indexOf(";") + 1);
      if (spaceGroup.indexOf("[--]") >= 0)
        spaceGroup = jf;
    } else {
      info = new Hashtable<String, Object>();
    }
    info.put("spaceGroupName", spaceGroup);
    String data = getSpaceGroupInfo(spaceGroup, cellInfo);
    if (infolist != null) {
      info.put("operations", infolist);
      info.put("symmetryInfo", strOperations);
    }
    if (data == null || data.equals("?"))
      data = "could not identify space group from name: " + spaceGroup
          + "\nformat: show spacegroup \"2\" or \"P 2c\" "
          + "or \"C m m m\" or \"x, y, z;-x ,-y, -z\"";
    info.put("spaceGroupInfo", data);
    return info;
  }

  public Object getSymmetryInfo(ModelSet modelSet, int iModel, int iAtom, SymmetryInterface uc, String xyz, int op,
                                P3 pt, P3 pt2, String id, int type) {
    if (pt2 != null)
      return modelSet.getSymmetryOperation(iModel, null, op, pt, pt2,
          (id == null ? "sym" : id), type == T.label);
    if (xyz == null) {
      String[] ops = uc.getSymmetryOperations();
      if (ops == null || op == 0 || Math.abs(op) > ops.length)
        return "";
      if (op > 0) {
        xyz = ops[op - 1];
      } else {
        xyz = ops[-1 - op];
      }
    } else {
      op = 0;
    }
    SymmetryInterface symTemp = modelSet.getSymTemp(false);
    symTemp.setSpaceGroup(false);
    int iSym = symTemp.addSpaceGroupOperation((op < 0 ? "!" : "=") + xyz, Math
        .abs(op));
    if (iSym < 0)
      return "";
    symTemp.setUnitCell(uc.getNotionalUnitCell());
    Object[] info;
    pt = P3.newP(pt == null ? modelSet.atoms[iAtom] : pt);
    if (type == T.point) {
      uc.toFractional(pt, false);
      if (Float.isNaN(pt.x))
        return "";
      P3 sympt = new P3();
      symTemp.newSpaceGroupPoint(iSym, pt, sympt, 0, 0, 0);
      symTemp.toCartesian(sympt, false);
      return sympt;
    }
    // null id means "array info only" but here we want the draw commands
    info = symTemp.getSymmetryOperationDescription(modelSet, iSym, uc, pt,
        pt2, (id == null ? "sym" : id));
    int ang = ((Integer) info[9]).intValue();
    /*
     *  xyz (Jones-Faithful calculated from matrix)
     *  xyzOriginal (Provided by operation) 
     *  description ("C2 axis", for example) 
     *  translation vector (fractional)  
     *  translation vector (cartesian)
     *  inversion point 
     *  axis point 
     *  axis vector
     *  angle of rotation
     *  matrix representation
     */
    switch (type) {
    case T.array:
      return info;
    case T.list:
      String[] sinfo = new String[] {
          (String) info[0],
          (String) info[1],
          (String) info[2],
          // skipping DRAW commands here
          Escape.eP((V3) info[4]), Escape.eP((V3) info[5]),
          Escape.eP((P3) info[6]), Escape.eP((P3) info[7]),
          Escape.eP((V3) info[8]), "" + info[9],
          "" + Escape.e(info[10]) };
      return sinfo;
    case T.info:
      return info[0];
    default:
    case T.label:
      return info[2];
    case T.draw:
      return info[3];
    case T.translation:
      // skipping fractional translation
      return info[5]; // cartesian translation
    case T.center:
      return info[6];
    case T.point:
      return info[7];
    case T.axis:
    case T.plane:
      return ((ang == 0) == (type == T.plane) ? (V3) info[8] : null);
    case T.angle:
      return info[9];
    case T.matrix4f:
      return info[10];
    }
  }

  public BS notInCentroid(ModelSet modelSet, BS bsAtoms,
                          int[] minmax) {
    try {
      BS bsDelete = new BS();
      int iAtom0 = bsAtoms.nextSetBit(0);
      JmolMolecule[] molecules = modelSet.getMolecules();
      int moleculeCount = molecules.length;
      Atom[] atoms = modelSet.atoms;
      boolean isOneMolecule = (molecules[moleculeCount - 1].firstAtomIndex == modelSet
          .models[atoms[iAtom0].modelIndex].firstAtomIndex);
      P3 center = new P3();
      boolean centroidPacked = (minmax[6] == 1);
      nextMol: for (int i = moleculeCount; --i >= 0
          && bsAtoms.get(molecules[i].firstAtomIndex);) {
        BS bs = molecules[i].atomList;
        center.set(0, 0, 0);
        int n = 0;
        for (int j = bs.nextSetBit(0); j >= 0; j = bs.nextSetBit(j + 1)) {
          if (isOneMolecule || centroidPacked) {
            center.setT(atoms[j]);
            if (isNotCentroid(center, 1, minmax, centroidPacked)) {
              if (isOneMolecule)
                bsDelete.set(j);
            } else if (!isOneMolecule) {
              continue nextMol;
            }
          } else {
            center.add(atoms[j]);
            n++;
          }
        }
        if (centroidPacked || n > 0 && isNotCentroid(center, n, minmax, false))
          bsDelete.or(bs);
      }
      return bsDelete;
    } catch (Exception e) {
      return null;
    }
  }
  
  private boolean isNotCentroid(P3 center, int n, int[] minmax, boolean centroidPacked) {
    center.scale(1f/n);
    toFractional(center, false);
    //System.out.println("isCentroid ? " + center);
    // we have to disallow just a tiny slice of atoms due to rounding errors
    // so  -0.000001 is OK, but 0.999991 is not.
    if (centroidPacked)
      return (center.x + 0.000005f <= minmax[0] || center.x - 0.000005f > minmax[3] 
         || center.y + 0.000005f <= minmax[1] || center.y - 0.000005f > minmax[4]
         || center.z + 0.000005f <= minmax[2] || center.z - 0.000005f > minmax[5]);
    
    return (center.x + 0.000005f <= minmax[0] || center.x + 0.00005f > minmax[3] 
     || center.y + 0.000005f <= minmax[1] || center.y + 0.00005f > minmax[4]
     || center.z + 0.000005f <= minmax[2] || center.z + 0.00005f > minmax[5]);
  }

  public boolean checkUnitCell(SymmetryInterface uc, P3 cell, P3 ptTemp,
                               boolean isAbsolute) {
   uc.toFractional(ptTemp, isAbsolute);
   float slop = 0.02f;
   // {1 1 1} here is the original cell
   return (ptTemp.x >= cell.x - 1f - slop && ptTemp.x <= cell.x + slop
       && ptTemp.y >= cell.y - 1f - slop && ptTemp.y <= cell.y + slop
       && ptTemp.z >= cell.z - 1f - slop && ptTemp.z <= cell.z + slop);
 }

  public boolean unitCellEquals(SymmetryInterface uc2) {
    return ((Symmetry) (uc2)).unitCell.isSameAs(unitCell);
  }

  public void addLatticeVectors(JmolList<float[]> lattvecs) {
    spaceGroup.addLatticeVectors(lattvecs);
  }

  public int getLatticeOp() {
    return spaceGroup.latticeOp;
  }

  public Matrix4f getOperationGammaIS(int iop) {
    return spaceGroup.finalOperations[iop].gammaIS;
  }

  public int getSiteMultiplicity(P3 pt) {
    return spaceGroup.getSiteMultiplicity(pt,unitCell);
  }




}  
