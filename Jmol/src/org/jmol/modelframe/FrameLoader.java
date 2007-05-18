/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-05-18 08:19:45 -0500 (Fri, 18 May 2007) $
 * $Revision: 7742 $

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

package org.jmol.modelframe;

import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Viewer;

import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolBioResolver;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/* 
 * This subclass contains all of the methods used to 
 * load a model. Methods only after model loading
 * are not included here.
 * 
 *  
 */
public final class FrameLoader extends Frame {

  private FrameLoader mergeFrame;
  private boolean merging;
  private boolean isMultiFile;
  private boolean isTrajectory = false;

  private final int[] specialAtomIndexes = new int[JmolConstants.ATOMID_MAX];
  private String[] group3Lists;
  private int[][] group3Counts;
  private Group[] groups;
  private int groupCount;
  

  FrameLoader(Viewer viewer, String name) {
    this.viewer = viewer;
    initializeFrame(name, 1, null, null);
    initializeModelSet(null, null);
  }

  FrameLoader(Viewer viewer, JmolAdapter adapter, Object clientFile, FrameLoader mergeFrame) {
    this.mergeFrame = mergeFrame;
    merging = (mergeFrame != null && mergeFrame.atomCount > 0);
    this.viewer = viewer;
    initializeFrame(adapter.getFileTypeName(clientFile).toLowerCase().intern(),
        adapter.getEstimatedAtomCount(clientFile), adapter
            .getAtomSetCollectionProperties(clientFile), adapter
            .getAtomSetCollectionAuxiliaryInfo(clientFile));
    initializeModelSet(adapter, clientFile);
    adapter.finish(clientFile);
    // dumpAtomSetNameDiagnostics(adapter, clientFile);
  }
/*
  private void dumpAtomSetNameDiagnostics(JmolAdapter adapter, Object clientFile) {
    int frameModelCount = modelCount;
    int adapterAtomSetCount = adapter.getAtomSetCount(clientFile);
    if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
      Logger.debug(
          "----------------\n" + "debugging of AtomSetName stuff\n" +
          "\nframeModelCount=" + frameModelCount +
          "\nadapterAtomSetCount=" + adapterAtomSetCount + "\n -- \n");
      for (int i = 0; i < adapterAtomSetCount; ++i) {
        Logger.debug(
            "atomSetName[" + i + "]=" + adapter.getAtomSetName(clientFile, i) +
            " atomSetNumber[" + i + "]=" + adapter.getAtomSetNumber(clientFile, i));
      }
    }
  }
*/

  private boolean someModelsHaveUnitcells;
  private boolean someModelsHaveFractionalCoordinates;

  private void initializeFrame(String name, int nAtoms, Properties properties,
                       Hashtable info) {
    g3d = viewer.getGraphics3D();
    //long timeBegin = System.currentTimeMillis();
    modelSetTypeName = name;
    isXYZ = (modelSetTypeName == "xyz");
    setZeroBased();
    mmset = new Mmset(this);
    mmset.setModelSetProperties(properties);
    mmset.setModelSetAuxiliaryInfo(info);
    isMultiFile = mmset.getModelSetAuxiliaryInfoBoolean("isMultiFile");
    isPDB = mmset.getModelSetAuxiliaryInfoBoolean("isPDB");
    trajectories = (Vector) mmset.getModelSetAuxiliaryInfo("trajectories");
    isTrajectory = (trajectories != null);
    someModelsHaveSymmetry = mmset
    .getModelSetAuxiliaryInfoBoolean("someModelsHaveSymmetry");
    someModelsHaveUnitcells = mmset
        .getModelSetAuxiliaryInfoBoolean("someModelsHaveUnitcells");
    someModelsHaveFractionalCoordinates = mmset
        .getModelSetAuxiliaryInfoBoolean("someModelsHaveFractionalCoordinates");
    if (merging) {
      someModelsHaveSymmetry |= mergeFrame.mmset
          .getModelSetAuxiliaryInfoBoolean("someModelsHaveSymmetry");
      someModelsHaveUnitcells |= mergeFrame.mmset
          .getModelSetAuxiliaryInfoBoolean("someModelsHaveUnitcells");
      someModelsHaveFractionalCoordinates |= mergeFrame.mmset
          .getModelSetAuxiliaryInfoBoolean("someModelsHaveFractionalCoordinates");
    }
    initializeBuild(nAtoms);
  }

  private final static int ATOM_GROWTH_INCREMENT = 2000;
  private final Hashtable htAtomMap = new Hashtable();

  private void initializeBuild(int atomCountEstimate) {
    if (atomCountEstimate <= 0)
      atomCountEstimate = ATOM_GROWTH_INCREMENT;
    if (merging) {
      atoms = mergeFrame.atoms;
      bonds = mergeFrame.bonds;
    } else {
      atoms = new Atom[atomCountEstimate];
      bonds = new Bond[250 + atomCountEstimate]; // was "2 *" -- WAY overkill.
    }
    htAtomMap.clear();
    initializeGroupBuild();
  }

  private final static int defaultGroupCount = 32;
  private Chain[] chains;
  private String[] group3s;
  private int[] seqcodes;
  private int[] firstAtomIndexes;

  private int currentModelIndex;
  private Model currentModel;
  private char currentChainID;
  private Chain currentChain;
  private int currentGroupSequenceNumber;
  private char currentGroupInsertionCode;
  private String currentGroup3;

  /**
   * also from calculateStructures
   * 
   */
  private void initializeGroupBuild() {
    groupCount = 0;
    chains = new Chain[defaultGroupCount];
    group3s = new String[defaultGroupCount];
    seqcodes = new int[defaultGroupCount];
    firstAtomIndexes = new int[defaultGroupCount];
    currentChainID = '\uFFFF';
    currentChain = null;
    currentGroupInsertionCode = '\uFFFF';
    currentGroup3 = "xxxxx";
    currentModelIndex = -1;
    currentModel = null;
  }

  private Chain nullChain;
  Group nullGroup; // used in Atom

  private int baseModelIndex = 0;
  private int baseModelCount = 0;
  private int baseAtomIndex = 0;
  private int baseBondIndex = 0;
  private boolean appendNew = true;

  private void initializeModelSet(JmolAdapter adapter, Object clientFile) {
    int adapterModelCount = modelCount = (adapter == null ? 1 : adapter
        .getAtomSetCount(clientFile));
    initializeAtomBondModelCounts();
    if (adapter == null) {
      mmset.setModelNameNumberProperties(0, "", 1, null, null, false);
    } else {
      appendNew = (modelCount > 1 || viewer.getAppendNew());
      if (modelCount > 0) {
        Logger.info("frame: haveSymmetry:" + someModelsHaveSymmetry
            + " haveUnitcells:" + someModelsHaveUnitcells
            + " haveFractionalCoord:" + someModelsHaveFractionalCoordinates);
        Logger
            .info(modelCount
                + " model"
                + (modelCount == 1 ? "" : "s")
                + (isTrajectory ? ", " + trajectories.size() + " trajectories"
                    : "")
                + " in this collection. Use getProperty \"modelInfo\" or"
                + " getProperty \"auxiliaryInfo\" to inspect them.");
      }

      iterateOverAllNewModels(adapter, clientFile, adapterModelCount);
      iterateOverAllNewAtoms(adapter, clientFile);
      iterateOverAllNewBonds(adapter, clientFile);
      iterateOverAllNewStructures(adapter, clientFile);

      initializeUnitCellAndSymmetry(adapterModelCount);
      initializeBonding();
    }

    finalizeGroupBuild(); // set group offsets and build monomers
    //only now can we access all of the atom's properties

    buildBioPolymers();
    freeze();
    calcAverageAtomPoint();
    calcBoundBoxDimensions();

    finalizeShapes();
  }

  private void initializeAtomBondModelCounts() {
    atomCount = 0;
    bondCount = 0;
    if (merging) {
      baseModelCount = mergeFrame.modelCount;
      if (appendNew) {
        baseModelIndex = baseModelCount;
        modelCount += baseModelCount;
      } else {
        baseModelIndex = viewer.getCurrentModelIndex();
        if (baseModelIndex < 0)
          baseModelIndex = baseModelCount - 1;
        modelCount = baseModelCount;
      }
      atomCount = baseAtomIndex = mergeFrame.atomCount;
      bondCount = baseBondIndex = mergeFrame.bondCount;
      //baseGroupIndex = mergeFrame.groupCount;
    }
    mmset.setModelCount(modelCount);
  }

  private void initializeMerge() {
    mmset.merge(mergeFrame.mmset);
    bsSymmetry = mergeFrame.bsSymmetry;
    if (mergeFrame.group3Lists != null) {
      for (int i = 0; i < baseModelCount; i++) {
        group3Lists[i] = mergeFrame.group3Lists[i];
        group3Counts[i] = mergeFrame.group3Counts[i];
      }
      group3Lists[modelCount] = mergeFrame.group3Lists[baseModelCount];
      group3Counts[modelCount] = mergeFrame.group3Counts[baseModelCount];
    }

    atomNames = mergeFrame.atomNames;
    clientAtomReferences = mergeFrame.clientAtomReferences;
    vibrationVectors = mergeFrame.vibrationVectors;
    occupancies = mergeFrame.occupancies;
    bfactor100s = mergeFrame.bfactor100s;
    partialCharges = mergeFrame.partialCharges;
    specialAtomIDs = mergeFrame.specialAtomIDs;
    surfaceDistance100s = null;
  }

  private void iterateOverAllNewModels(JmolAdapter adapter, Object clientFile, int adapterModelCount) {

    if (modelCount > 0) {
      nullChain = new Chain(this, mmset.getModel(baseModelIndex), ' ');
      nullGroup = new Group(nullChain, "", 0, -1, -1);
    }

    group3Lists = new String[modelCount + 1];
    group3Counts = new int[modelCount + 1][];
    
    if (merging)
      initializeMerge();

    int ipt = baseModelIndex;
    for (int i = 0; i < adapterModelCount; ++i, ipt++) {
      int modelNumber = (appendNew ? adapter.getAtomSetNumber(clientFile, i) : Integer.MAX_VALUE);
      String modelName = adapter.getAtomSetName(clientFile, i);
      if (modelName == null)
        modelName = (modelNumber == Integer.MAX_VALUE ? "" : "" + modelNumber);
      Properties modelProperties = adapter
          .getAtomSetProperties(clientFile, i);
      Hashtable modelAuxiliaryInfo = adapter.getAtomSetAuxiliaryInfo(
          clientFile, i);
      boolean isPDBModel = mmset.setModelNameNumberProperties(ipt, modelName,
          modelNumber, modelProperties, modelAuxiliaryInfo, isPDB);
      if (isPDBModel) {
        group3Lists[ipt] = JmolConstants.group3List;
        group3Counts[ipt] = new int[JmolConstants.group3Count + 10];
        if (group3Lists[modelCount] == null) {
          group3Lists[modelCount] = JmolConstants.group3List;
          group3Counts[modelCount] = new int[JmolConstants.group3Count + 10];
        }
      }
      if (modelAuxiliaryInfo.containsKey("periodicOriginXyz"))
        someModelsHaveSymmetry = true;
    }
    mmset.finalizeModelNumbers(baseModelCount);
  }
    
  private void iterateOverAllNewAtoms(JmolAdapter adapter, Object clientFile) {
    // atom is created, but not all methods are safe, because it
    // has no group -- this is only an issue for debugging

    short mad = viewer.getMadAtom();
    for (JmolAdapter.AtomIterator iterAtom = adapter
        .getAtomIterator(clientFile); iterAtom.hasNext();) {
      short elementNumber = (short) iterAtom.getElementNumber();
      if (elementNumber <= 0)
        elementNumber = JmolConstants.elementNumberFromSymbol(iterAtom
            .getElementSymbol());
      char alternateLocation = iterAtom.getAlternateLocationID();
      addAtom(iterAtom.getAtomSetIndex() + baseModelIndex, iterAtom.getAtomSymmetry(), iterAtom.getAtomSite(),
          iterAtom.getUniqueID(), elementNumber, iterAtom.getAtomName(), mad,
          iterAtom.getFormalCharge(), iterAtom.getPartialCharge(), iterAtom
              .getOccupancy(), iterAtom.getBfactor(), iterAtom.getX(),
          iterAtom.getY(), iterAtom.getZ(), iterAtom.getIsHetero(), iterAtom
              .getAtomSerial(), iterAtom.getChainID(), iterAtom.getGroup3(),
          iterAtom.getSequenceNumber(), iterAtom.getInsertionCode(), iterAtom
              .getVectorX(), iterAtom.getVectorY(), iterAtom.getVectorZ(),
          alternateLocation, iterAtom.getClientAtomReference(), iterAtom.getRadius());
    }
    
    int iLast = -1;
    for (int i = 0; i < atomCount; i++)
      if (atoms[i].modelIndex != iLast)
        mmset.setFirstAtomIndex(iLast = atoms[i].modelIndex, i);
  }

  private void addAtom(int modelIndex, BitSet atomSymmetry, int atomSite,
                       Object atomUid, short atomicAndIsotopeNumber,
                       String atomName, short mad, int formalCharge,
                       float partialCharge, int occupancy, float bfactor,
                       float x, float y, float z, boolean isHetero,
                       int atomSerial, char chainID, String group3,
                       int groupSequenceNumber, char groupInsertionCode,
                       float vectorX, float vectorY, float vectorZ,
                       char alternateLocationID, Object clientAtomReference,
                       float radius) {

    checkNewGroup(atomCount, modelIndex, chainID, group3, groupSequenceNumber,
        groupInsertionCode);

    if (atomCount == atoms.length)
      growAtomArrays(ATOM_GROWTH_INCREMENT);

    Atom atom = new Atom(this, currentModelIndex, atomCount, atomSymmetry,
        atomSite, atomicAndIsotopeNumber, atomName, mad, formalCharge,
        partialCharge, occupancy, bfactor, x, y, z, isHetero, atomSerial,
        chainID, group3, vectorX, vectorY, vectorZ, alternateLocationID,
        clientAtomReference, radius);
    atoms[atomCount] = atom;
    ++atomCount;
    htAtomMap.put(atomUid, atom);
  }

  private void checkNewGroup(int atomIndex, int modelIndex, char chainID,
                             String group3, int groupSequenceNumber,
                             char groupInsertionCode) {
    String group3i = (group3 == null ? null : group3.intern());
    if (modelIndex != currentModelIndex) {
      currentModel = mmset.getModel(modelIndex);
      currentModelIndex = modelIndex;
      currentChainID = '\uFFFF';
    }
    if (chainID != currentChainID) {
      currentChainID = chainID;
      currentChain = currentModel.getOrAllocateChain(chainID);
      currentGroupInsertionCode = '\uFFFF';
      currentGroupSequenceNumber = -1;
      currentGroup3 = "xxxx";
    }
    if (groupSequenceNumber != currentGroupSequenceNumber
        || groupInsertionCode != currentGroupInsertionCode
        || group3i != currentGroup3) {
      currentGroupSequenceNumber = groupSequenceNumber;
      currentGroupInsertionCode = groupInsertionCode;
      currentGroup3 = group3i;
      if (groupCount == group3s.length) {
        chains = (Chain[]) ArrayUtil.doubleLength(chains);
        group3s = ArrayUtil.doubleLength(group3s);
        seqcodes = ArrayUtil.doubleLength(seqcodes);
        firstAtomIndexes = ArrayUtil.doubleLength(firstAtomIndexes);
      }
      firstAtomIndexes[groupCount] = atomIndex;
      chains[groupCount] = currentChain;
      group3s[groupCount] = group3;
      seqcodes[groupCount] = Group.getSeqcode(groupSequenceNumber,
          groupInsertionCode);
      ++groupCount;
    }
  }

  private void growAtomArrays(int byHowMuch) {
    int newLength = atomCount + byHowMuch;
    atoms = (Atom[]) ArrayUtil.setLength(atoms, newLength);
    if (clientAtomReferences != null)
      clientAtomReferences = (Object[]) ArrayUtil.setLength(
          clientAtomReferences, newLength);
    if (vibrationVectors != null)
      vibrationVectors = (Vector3f[]) ArrayUtil.setLength(vibrationVectors,
          newLength);
    if (occupancies != null)
      occupancies = ArrayUtil.setLength(occupancies, newLength);
    if (bfactor100s != null)
      bfactor100s = ArrayUtil.setLength(bfactor100s, newLength);
    if (partialCharges != null)
      partialCharges = ArrayUtil.setLength(partialCharges, newLength);
    if (atomNames != null)
      atomNames = ArrayUtil.setLength(atomNames, newLength);
    if (atomSerials != null)
      atomSerials = ArrayUtil.setLength(atomSerials, newLength);
    if (specialAtomIDs != null)
      specialAtomIDs = ArrayUtil.setLength(specialAtomIDs, newLength);
  }


  private void iterateOverAllNewBonds(JmolAdapter adapter, Object clientFile) {
    JmolAdapter.BondIterator iterBond = adapter.getBondIterator(clientFile);
  if (iterBond != null)
    while (iterBond.hasNext()) {
      bondAtoms(iterBond.getAtomUniqueID1(), iterBond.getAtomUniqueID2(),
          (short) iterBond.getEncodedOrder());
    }
  }
  
  private void bondAtoms(Object atomUid1, Object atomUid2, short order) {
    if (defaultCovalentMad == 0)
      defaultCovalentMad = viewer.getMadBond();
    Atom atom1 = (Atom) htAtomMap.get(atomUid1);
    if (atom1 == null) {
      Logger.error("bondAtoms cannot find atomUid1?:" + atomUid1);
      return;
    }
    Atom atom2 = (Atom) htAtomMap.get(atomUid2);
    if (atom2 == null) {
      Logger.error("bondAtoms cannot find atomUid2?:" + atomUid2);
      return;
    }
    // note that if the atoms are already bonded then
    // Atom.bondMutually(...) will return null
    if (atom1.isBonded(atom2))
      return;
    Bond bond = bondMutually(atom1, atom2, order, getDefaultMadFromOrder(order));
    if (bondCount == bonds.length)
      bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount + 2
          * ATOM_GROWTH_INCREMENT);
    setBond(bondCount++, bond);
    //if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
      //fileHasHbonds = true;
  }

  private void iterateOverAllNewStructures(JmolAdapter adapter,
                                           Object clientFile) {
    JmolAdapter.StructureIterator iterStructure = adapter
        .getStructureIterator(clientFile);
    if (iterStructure != null)
      while (iterStructure.hasNext()) {
        if (!iterStructure.getStructureType().equals("turn"))
          defineStructure(iterStructure.getModelIndex() + baseModelIndex,
              iterStructure.getStructureType(),
              iterStructure.getStartChainID(), iterStructure
                  .getStartSequenceNumber(), iterStructure
                  .getStartInsertionCode(), iterStructure.getEndChainID(),
              iterStructure.getEndSequenceNumber(), iterStructure
                  .getEndInsertionCode());
      }

    // define turns LAST. (pulled by the iterator first)
    // so that if they overlap they get overwritten:

    iterStructure = adapter.getStructureIterator(clientFile);
    if (iterStructure != null)
      while (iterStructure.hasNext()) {
        if (iterStructure.getStructureType().equals("turn"))
          defineStructure(iterStructure.getModelIndex() + baseModelIndex,
              iterStructure.getStructureType(),
              iterStructure.getStartChainID(), iterStructure
                  .getStartSequenceNumber(), iterStructure
                  .getStartInsertionCode(), iterStructure.getEndChainID(),
              iterStructure.getEndSequenceNumber(), iterStructure
                  .getEndInsertionCode());
      }
  }
  
  private boolean fileHadDefinedStructures;

  private void defineStructure(int modelIndex, String structureType, char startChainID,
                       int startSequenceNumber, char startInsertionCode,
                       char endChainID, int endSequenceNumber,
                       char endInsertionCode) {
    fileHadDefinedStructures = true; //(in file)
    mmset.defineStructure(modelIndex, structureType, startChainID,
        startSequenceNumber, startInsertionCode, endChainID, endSequenceNumber,
        endInsertionCode);
  }

  ////// symmetry ///////
  
  private void initializeUnitCellAndSymmetry(int adapterModelCount) {
    /*
     * really THREE issues here:
     * 1) does a model have an associated unit cell that could be displayed?
     * 2) are the coordinates fractional and so need to be transformed?
     * 3) does the model have symmetry operations that were applied?
     * 
     * This must be done for each model individually.
     * 
     */

    if (someModelsHaveUnitcells) {
      boolean doPdbScale = (adapterModelCount == 1);
      cellInfos = new CellInfo[modelCount];
      for (int i = 0; i < baseModelCount; i++)
        cellInfos[i] = (mergeFrame.cellInfos != null ? mergeFrame.cellInfos[i]
            : new CellInfo(i, false, mmset.getModelAuxiliaryInfo(i)));
      for (int i = baseModelCount; i < modelCount; i++)
        cellInfos[i] = new CellInfo(i, doPdbScale, mmset.getModelAuxiliaryInfo(i));
    }
    if (someModelsHaveSymmetry) {
      getSymmetrySet();
      for (int iAtom = baseAtomIndex, iModel = -1, i0 = 0; iAtom < atomCount; iAtom++) {
        if (atoms[iAtom].modelIndex != iModel) {
          iModel = atoms[iAtom].modelIndex;
          i0 = baseAtomIndex
              + mmset.getModelAuxiliaryInfoInt(iModel, "presymmetryAtomIndex")
              + mmset.getModelAuxiliaryInfoInt(iModel, "presymmetryAtomCount");
        }
        if (iAtom >= i0)
          bsSymmetry.set(iAtom);
      }
    }
    if (someModelsHaveFractionalCoordinates) {
      for (int i = baseAtomIndex; i < atomCount; i++) {
        int modelIndex = atoms[i].modelIndex;
        if (!cellInfos[modelIndex].coordinatesAreFractional)
          continue;
        cellInfos[modelIndex].toCartesian(atoms[i]);
        if (Logger.isActiveLevel(Logger.LEVEL_DEBUG))
          Logger.debug("atom " + i + ": " + (Point3f) atoms[i]);
      }
    }
  }

  private void initializeBonding() {
    // perform bonding if necessary
    boolean doBond = (bondCount == baseBondIndex 
        || isMultiFile 
        || isPDB && (bondCount - baseBondIndex) < (atomCount - baseAtomIndex) / 2 
        || someModelsHaveSymmetry && !viewer.getApplySymmetryToBonds());
    if (viewer.getForceAutoBond() || doBond && viewer.getAutoBond()
        && getModelSetProperty("noautobond") == null) {
      BitSet bs = null;
      if (merging) {
        bs = new BitSet(atomCount);
        for (int i = baseAtomIndex; i < atomCount; i++)
          bs.set(i);
      }
      Logger.info("Frame: autobonding; use  autobond=false  to not generate bonds automatically");
      autoBond(bs, bs, null);
    } else {
      Logger.info("Frame: not autobonding; use forceAutobond=true to force automatic bond creation");        
    }
  }


  //// average point, bounding box ////
  
  private final Point3f pointMin = new Point3f();
  private final Point3f pointMax = new Point3f();

  private final static Point3f[] unitBboxPoints = { new Point3f(1, 1, 1),
      new Point3f(1, 1, -1), new Point3f(1, -1, 1), new Point3f(1, -1, -1),
      new Point3f(-1, 1, 1), new Point3f(-1, 1, -1), new Point3f(-1, -1, 1),
      new Point3f(-1, -1, -1), };

  private void calcBoundBoxDimensions() {
    calcAtomsMinMax();
    if (cellInfos != null)
      calcUnitCellMinMax();
    centerBoundBox.add(pointMin, pointMax);
    centerBoundBox.scale(0.5f);
    boundBoxCornerVector.sub(pointMax, centerBoundBox);

    for (int i = 8; --i >= 0;) {
      Point3f bbcagePoint = bboxVertices[i] = new Point3f(unitBboxPoints[i]);
      bbcagePoint.x *= boundBoxCornerVector.x;
      bbcagePoint.y *= boundBoxCornerVector.y;
      bbcagePoint.z *= boundBoxCornerVector.z;
      bbcagePoint.add(centerBoundBox);
    }
  }

  private void calcAtomsMinMax() {
    if (atomCount < 2) {
      pointMin.set(-10, -10, -10);
      pointMax.set(10, 10, 10);
      return;
    }
    pointMin.set(atoms[0]);
    pointMax.set(atoms[0]);
    for (int i = atomCount; --i > 0;) {
      // note that the 0 element was set above
      checkMinMax(atoms[i]);
    }
  }

  private void calcUnitCellMinMax() {
    for (int i = 0; i < modelCount; i++) {
      if (!cellInfos[i].coordinatesAreFractional)
        continue;
      Point3f[] vertices = cellInfos[i].getUnitCell().getVertices();
      for (int j = 0; j < 8; j++)
        checkMinMax(vertices[j]);
    }
  }

  private void checkMinMax(Point3f pt) {
    float t = pt.x;
    if (t < pointMin.x)
      pointMin.x = t;
    else if (t > pointMax.x)
      pointMax.x = t;
    t = pt.y;
    if (t < pointMin.y)
      pointMin.y = t;
    else if (t > pointMax.y)
      pointMax.y = t;
    t = pt.z;
    if (t < pointMin.z)
      pointMin.z = t;
    else if (t > pointMax.z)
      pointMax.z = t;
  }

  private void finalizeGroupBuild() {
    // run this loop in increasing order so that the
    // groups get defined going up
    groups = new Group[groupCount];
    for (int i = 0; i < groupCount; ++i) {
      distinguishAndPropagateGroup(i, chains[i], group3s[i], seqcodes[i],
          firstAtomIndexes[i], (i == groupCount - 1 ? atomCount
              : firstAtomIndexes[i + 1]));
      chains[i] = null;
      group3s[i] = null;
    }
    chains = null;
    group3s = null;
    
    if (group3Lists != null) {
      Hashtable info = getModelSetAuxiliaryInfo();
      if (info != null) {
        info.put("group3Lists", group3Lists);
        info.put("group3Counts", group3Counts);
      }
    }
  }

  private boolean haveBioClasses = true;
  private JmolBioResolver jbr = null;

  private void distinguishAndPropagateGroup(int groupIndex, Chain chain, String group3,
                                    int seqcode, int firstAtomIndex,
                                    int maxAtomIndex) {
    /*
     * called by finalizeGroupBuild()
     * 
     * first: build array of special atom names, 
     * for example "CA" for the alpha carbon is assigned #2
     * see JmolConstants.specialAtomNames[]
     * the special atoms all have IDs based on Atom.lookupSpecialAtomID(atomName)
     * these will be the same for each conformation
     * 
     * second: creates the monomers themselves based on this information
     * thus building the byte offsets[] array for each monomer, indicating which
     * position relative to the first atom in the group is which atom.
     * Each monomer.offsets[i] then points to the specific atom of that type
     * these will NOT be the same for each conformation  
     * 
     */
    int lastAtomIndex = maxAtomIndex - 1;

    if (lastAtomIndex < firstAtomIndex)
      throw new NullPointerException();
    int modelIndex = atoms[firstAtomIndex].modelIndex;

    Group group = null;
    if (group3 != null && specialAtomIDs != null && haveBioClasses) {
      if (jbr == null && haveBioClasses) {
        String className = JmolConstants.getShapeClassName(JmolConstants.CLASS_BASE_BIO)+"Resolver";
        try {
          Class shapeClass = Class.forName(className);
          jbr = (JmolBioResolver) shapeClass.newInstance();
          haveBioClasses = true;
        } catch (Exception e) {
          haveBioClasses = false;
        }
      }
      if (haveBioClasses) {
        group = jbr.distinguishAndPropagateGroup(chain, group3, seqcode,
            firstAtomIndex, maxAtomIndex, modelIndex, modelCount,
            specialAtomIndexes, specialAtomIDs, atoms);
      }
    }
    String key;
    if (group == null) {
      group = new Group(chain, group3, seqcode, firstAtomIndex, lastAtomIndex);
      key = "o>";
    } else { 
      key = (group.isProtein() ? "p>" : group.isNucleic() ? "n>"
          : group.isCarbohydrate() ? "c>" : "o>");
    }
    if (group3 != null)
      countGroup(modelIndex, key, group3);
    
    chain.addGroup(group);
    groups[groupIndex] = group;

    for (int i = maxAtomIndex; --i >= firstAtomIndex;)
      atoms[i].setGroup(group);

  }

  private void countGroup(int modelIndex, String code, String group3) {
    if (group3Lists == null || group3Lists[modelIndex] == null)
      return;
    String g3code = (group3 + "   ").substring(0, 3);
    int pt = group3Lists[modelIndex].indexOf(g3code);
    if (pt < 0) {
      group3Lists[modelIndex] += ",[" + g3code + "]";
      pt = group3Lists[modelIndex].indexOf(g3code);
      group3Counts[modelIndex] = (int[]) ArrayUtil.setLength(
          group3Counts[modelIndex], group3Counts[modelIndex].length + 10);
    }
    group3Counts[modelIndex][pt / 6]++;
    pt = group3Lists[modelIndex].indexOf(",[" + g3code);
    if (pt >= 0)
      group3Lists[modelIndex] = group3Lists[modelIndex].substring(0, pt) + code
          + group3Lists[modelIndex].substring(pt + 2);
    //becomes x> instead of ,[ 
    //these will be used for setting up the popup menu
    if (modelIndex < modelCount)
      countGroup(modelCount, code, group3);
  }

  private void buildBioPolymers() {
    for (int i = 0; i < groupCount; ++i) {
      Group group = groups[i];
      if (merging)
        group.setFrame(this);
      if (jbr != null)
        jbr.buildBioPolymer(group, groups, i);
    }
  }

  private void freeze() {

    // resize arrays
    if (atomCount < atoms.length)
      growAtomArrays(0);
    if (bondCount < bonds.length)
      bonds = (Bond[]) ArrayUtil.setLength(bonds, bondCount);

    // free bonds cache 
    
    for (int i = MAX_BONDS_LENGTH_TO_CACHE; --i > 0;) { // .GT. 0
      numCached[i] = 0;
      Bond[][] bondsCache = freeBonds[i];
      for (int j = bondsCache.length; --j >= 0;)
        bondsCache[j] = null;
    }

    setAtomNamesAndNumbers();

    // find elements for the popup menus
    
    findElementsPresent();

    // finalize all group business
    calculateStructures(merging);

    // reset molecules -- important if merging

    modelCount = mmset.getModelCount();
    molecules = null;
    moleculeCount = 0;
    currentModel = null;
    currentChain = null;
    htAtomMap.clear();
  }

  private void setAtomNamesAndNumbers() {
    // first, validate that all atomSerials are NaN
    if (atomSerials == null)
      atomSerials = new int[atomCount];
    // now, we'll assign 1-based atom numbers within each model
    int lastModelIndex = Integer.MAX_VALUE;
    int modelAtomIndex = 0;
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atoms[i];
      if (atom.modelIndex != lastModelIndex) {
        lastModelIndex = atom.modelIndex;
        modelAtomIndex = (isZeroBased ? 0 : 1);
      }
      // 1) do not change numbers assigned by adapter
      // 2) do not change the number already assigned when merging
      // 3) restart numbering with new atoms, not a continuation of old
      
      if (atomSerials[i] == 0)
        atomSerials[i] = (i < baseAtomIndex ? mergeFrame.atomSerials[i]
            : modelAtomIndex++);
    }
    if (atomNames == null)
      atomNames = new String[atomCount];
    for (int i = 0; i < atomCount; ++i)
      if (atomNames[i] == null) {
        Atom atom = atoms[i];
        atomNames[i] = atom.getElementSymbol() + atom.getAtomNumber();
      }
  }

  private void findElementsPresent() {
    elementsPresent = new BitSet[modelCount];
    for (int i = 0; i < modelCount; i++)
      elementsPresent[i] = new BitSet();
    for (int i = atomCount; --i >= 0;) {
      int n = atoms[i].getAtomicAndIsotopeNumber();
      if (n >= 128)
        n = JmolConstants.elementNumberMax
            + JmolConstants.altElementIndexFromNumber(n);
      elementsPresent[atoms[i].modelIndex].set(n);
    }
  }

  /**
   * allows rebuilding of PDB structures;
   * also accessed by ModelManager from Eval
   * 
   * @param rebuild 
   *  
   */
  void calculateStructures(boolean rebuild) {
    if (rebuild) {
      for (int i = JmolConstants.SHAPE_MAX; --i >= 0;)
        if (JmolConstants.isShapeSecondary(i))
          shapes[i] = null;
      if (jbr != null && groupCount > 0)
        jbr.clearBioPolymers(groups, groupCount);
      mmset.clearStructures();
      initializeGroupBuild();
      for (int i = 0; i < atomCount; i++) {
        Atom atom = atoms[i];
        if (atom.group == null)
          checkNewGroup(i, atom.modelIndex, '\0', null, 0, '\0');
        else
          checkNewGroup(i, atom.modelIndex, atom.getChainID(),
              atom.getGroup3(), atom.getSeqNumber(), atom.getInsertionCode());
      }
      finalizeGroupBuild();
      buildBioPolymers();
      fileHadDefinedStructures = false;
      moleculeCount = 0;
    }
    if (!fileHadDefinedStructures)
      mmset.calculateStructures();
    mmset.freeze();
  }

  private void calcAverageAtomPoint() {
    averageAtomPoint.set(0, 0, 0);
    if (atomCount == 0)
      return;
    for (int i = atomCount; --i >= 0;)
      averageAtomPoint.add(atoms[i]);
    averageAtomPoint.scale(1f / atomCount);
  }


  ///////////////  shapes  ///////////////
  
  private void finalizeShapes() {
    if (merging) {
      for (int i = 0; i < JmolConstants.SHAPE_MAX; i++)
        if ((shapes[i] = mergeFrame.shapes[i]) != null)
          shapes[i].setFrame(this);
      viewer.getFrameRenderer().clear();
      merging = false;
      return;
    }
    loadShape(JmolConstants.SHAPE_BALLS);
    loadShape(JmolConstants.SHAPE_STICKS);
    loadShape(JmolConstants.SHAPE_MEASURES);
    loadShape(JmolConstants.SHAPE_BBCAGE);
    loadShape(JmolConstants.SHAPE_UCCAGE);
  }
}
