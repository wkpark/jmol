/* $RCSfile$
 * $Author: egonw $
 * $Date: 2005-11-10 09:52:44 -0600 (Thu, 10 Nov 2005) $
 * $Revision: 4255 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
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

import java.util.BitSet;
import java.util.Hashtable;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.api.SymmetryInterface;
import org.jmol.modelset.Atom;
import org.jmol.util.Logger;

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
                                         Atom[] atomset, BitSet bsAtoms,
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
    else if (asDraw && pointGroup.isDrawType(type, index))
      return pointGroup.drawInfo;
    else if (asInfo && pointGroup.info != null)
      return pointGroup.info;
    return pointGroup.getInfo(modelIndex, asDraw, asInfo, type, index, scale);
  }

  // SpaceGroup methods
  
  public void setSpaceGroup(boolean doNormalize) {
    if (spaceGroup == null)
      spaceGroup = new SpaceGroup(doNormalize);
  }

  public boolean addSpaceGroupOperation(String xyz) {
    return spaceGroup.addSymmetry(xyz);
  }

  public void setLattice(int latt) {
    spaceGroup.setLattice(latt);
  }

  public String getSpaceGroupName() {
    return (symmetryInfo != null ? symmetryInfo.spaceGroup
        : spaceGroup != null ? spaceGroup.getName() : "");
  }

  public Object getSpaceGroup() {
    return spaceGroup;
  }
  
  public void setSpaceGroup(SymmetryInterface symmetry) {
    spaceGroup = (symmetry == null ? null : (SpaceGroup) symmetry.getSpaceGroup());
  }

  public boolean createSpaceGroup(int desiredSpaceGroupIndex, String name,
                                  float[] notionalUnitCell, boolean doNormalize) {
    spaceGroup = SpaceGroup.createSpaceGroup(desiredSpaceGroupIndex, name,
        notionalUnitCell, doNormalize);
    if (spaceGroup != null && Logger.debugging)
      Logger.debug("using generated space group " + spaceGroup.dumpInfo());
    return spaceGroup != null;
  }

  public boolean haveSpaceGroup() {
    return (spaceGroup != null);
  }

  public int determineSpaceGroupIndex(String name) {
    return SpaceGroup.determineSpaceGroupIndex(name);
  }

  public String getSpaceGroupInfo(String name, float[] unitCell) {
    return SpaceGroup.getInfo(name, unitCell);
  }

  public Object getLatticeDesignation() {
    return spaceGroup.getLatticeDesignation();
  }

  public void setFinalOperations(Point3f[] atoms, int iAtomFirst, int noSymmetryCount, boolean doNormalize) {
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

  public void newSpaceGroupPoint(int i, Point3f atom1, Point3f atom2,
                       int transX, int transY, int transZ) {
    spaceGroup.finalOperations[i].newPoint(atom1, atom2, transX, transY, transZ);
  }
    
  public Object rotateEllipsoid(int i, Point3f ptTemp, Vector3f[] axes, Point3f ptTemp1,
                                Point3f ptTemp2) {
    return spaceGroup.finalOperations[i].rotateEllipsoid(ptTemp, axes, unitCell, ptTemp1,
        ptTemp2);
  }

  // UnitCell methods

  public boolean haveUnitCell() {
    return (unitCell != null);
  }

  public String getUnitsymmetryInfo() {
    return (unitCell == null ? "no unit cell information" : unitCell.dumpInfo(false));
  }

  public void setUnitCell(float[] notionalUnitCell) {
    unitCell = new UnitCell(notionalUnitCell);
  }

  public void toCartesian(Point3f pt) {
    if (unitCell == null)
      return;
    unitCell.toCartesian(pt);
  }

  public Object[] getEllipsoid(float[] parBorU) {
    return unitCell.getEllipsoid(parBorU);
  }

  public Point3f ijkToPoint3f(int nnn) {
    return UnitCell.ijkToPoint3f(nnn);
  }

  public void toFractional(Point3f pt) {
    if (unitCell == null)
      return;
    unitCell.toFractional(pt);
  }

  public Point3f[] getUnitCellVertices() {
    return unitCell.getVertices();
  }

  public Point3f getCartesianOffset() {
    return unitCell.getCartesianOffset();
  }

  public float[] getNotionalUnitCell() {
    return unitCell == null ? null : unitCell.getNotionalUnitCell();
  }

  public void toUnitCell(Point3f pt, Point3f offset) {
    if (symmetryInfo.isMultiCell && unitCell != null)
      unitCell.toUnitCell(pt, offset);
  }

  public void setUnitCellOffset(Point3f pt) {
    unitCell.setOffset(pt);
  }

  public void setOffset(int nnn) {
    unitCell.setOffset(nnn);
  }

  public Point3f getFractionalOffset() {
    return unitCell.getFractionalOffset();
  }

  public float getUnitsymmetryInfo(int infoType) {
    return unitCell.getInfo(infoType);
  }

  public int getModelIndex() {
    return symmetryInfo.modelIndex;
  }

  public void setModelIndex(int i) {
    symmetryInfo.modelIndex = i;    
  }

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
    return symmetryInfo.isPeriodic();
  }

  public void setSymmetryInfo(int modelIndex, Hashtable modelAuxiliaryInfo) {
    symmetryInfo = new SymmetryInfo();
    float[] notionalUnitcell = symmetryInfo.setSymmetryInfo(modelIndex,
        modelAuxiliaryInfo);
    if (notionalUnitcell == null)
      return;
    setUnitCell(notionalUnitcell);
    if (Logger.debugging)
      Logger
          .debug("symmetryInfos[" + modelIndex + "]:\n" + unitCell.dumpInfo(true));
  }

  public float getUnitCellInfo(int infoType) {
    return unitCell.getInfo(infoType);
  }

  public String getUnitCellInfo() {
    return (unitCell == null ? "no unit cell information" 
        : unitCell.dumpInfo(false));
  }
}  
