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

import org.jmol.util.BSUtil;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Measure;

import javajs.util.A4;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.Quat;
import javajs.util.T3;
import javajs.util.V3;

import org.jmol.viewer.JC;
import org.jmol.viewer.TransformManager;
import org.jmol.viewer.Viewer;
import org.jmol.java.BS;
import org.jmol.script.T;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.RadiusData;
import org.jmol.shape.Shape;

import javajs.util.List;
import javajs.util.SB;


import java.util.Hashtable;
import java.util.Map;


/*
 * An abstract class always created using new ModelLoader(...)
 * 
 * Merged with methods in Mmset and ModelManager 10/2007  Jmol 11.3.32
 * 
 * ModelLoader simply pulls out all private classes that are
 * necessary only for file loading (and structure recalculation).
 * 
 * What is left here are all the methods that are 
 * necessary AFTER a model is loaded, when it is being 
 * accessed by Viewer, primarily.
 * 
 * Please:
 * 
 * 1) designate any methods used only here as private
 * 2) designate any methods accessed only by ModelLoader as protected
 * 3) designate any methods accessed within modelset as nothing
 * 4) designate any methods accessed only by Viewer as public
 * 
 * Bob Hanson, 5/2007, 10/2007
 * 
 */
 public class ModelSet extends ModelCollection {

  ////////////////////////////////////////////////////////////////

  public ModelSet(Viewer vwr, String name) {
    this.vwr = vwr;
    modelSetName = name;
  }

  @Override
  protected void releaseModelSet() {
    am = null;
    closest[0] = null;
    super.releaseModelSet();
  }

  //variables that will be reset when a new frame is instantiated

  private boolean selectionHaloEnabled = false;
  private boolean echoShapeActive = false;

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    this.selectionHaloEnabled = selectionHaloEnabled;
  }

  public boolean getSelectionHaloEnabled() {
    return selectionHaloEnabled;
  }

  public boolean getEchoStateActive() {
    return echoShapeActive;
  }

  public void setEchoStateActive(boolean TF) {
    echoShapeActive = TF;
  }

  protected String modelSetTypeName;

  public String getModelSetTypeName() {
    return modelSetTypeName;
  }

  public int getModelNumberIndex(int modelNumber, boolean useModelNumber,
                                 boolean doSetTrajectory) {
    if (useModelNumber) {
      for (int i = 0; i < mc; i++)
        if (modelNumbers[i] == modelNumber 
            || modelNumber < 1000000 && modelNumbers[i] == 1000000 + modelNumber)
          return i;
      return -1;
    }
    //new decimal format:   frame 1.2 1.3 1.4
    for (int i = 0; i < mc; i++)
      if (modelFileNumbers[i] == modelNumber) {
        if (doSetTrajectory && isTrajectory(i))
          setTrajectory(i);
        return i;
      }
    return -1;
  }

  public BS getBitSetTrajectories() {
    if (trajectorySteps == null)
      return null;
    BS bsModels = new BS();
    for (int i = mc; --i >= 0;) {
      int t = am[i].getSelectedTrajectory(); 
      if (t >= 0) {
        bsModels.set(t);
        i = am[i].trajectoryBaseIndex; //skip other trajectories
      }
    }
    return bsModels;
  }

  public void setTrajectoryBs(BS bsModels) {
    for (int i = 0; i < mc; i++)
      if (bsModels.get(i))
        setTrajectory(i);
  }

  public void setTrajectory(int modelIndex) {
    if (modelIndex < 0 || !isTrajectory(modelIndex))
      return;
    // The user has used the MODEL command to switch to a new set of atom coordinates
    // Or has specified a trajectory in a select, display, or hide command.

    // Assign the coordinates and the model index for this set of atoms
    if (at[am[modelIndex].firstAtomIndex].mi == modelIndex)
      return;
    int baseModelIndex = am[modelIndex].trajectoryBaseIndex;
    am[baseModelIndex].setSelectedTrajectory(modelIndex);
    setAtomPositions(baseModelIndex, modelIndex, trajectorySteps.get(modelIndex),
        null, 0,
        (vibrationSteps == null ? null : vibrationSteps.get(modelIndex)), true);    
    int m = vwr.getCurrentModelIndex();
    if (m >= 0 && m != modelIndex 
        && am[m].fileIndex == am[modelIndex].fileIndex)
      vwr.setCurrentModelIndexClear(modelIndex, false);
  }  

  public void morphTrajectories(int m1, int m2, float f) {
    if (m1 < 0 || m2 < 0 || !isTrajectory(m1) || !isTrajectory(m2))
      return;
    if (f == 0) {
      setTrajectory(m1);
      return;
    }
    if (f == 1) {
      setTrajectory(m2);
      return;
    }
    int baseModelIndex = am[m1].trajectoryBaseIndex;
    am[baseModelIndex].setSelectedTrajectory(m1);
    setAtomPositions(baseModelIndex, m1, trajectorySteps.get(m1),
        trajectorySteps.get(m2), f, (vibrationSteps == null ? null
            : vibrationSteps.get(m1)), true);
    int m = vwr.getCurrentModelIndex();
    if (m >= 0 && m != m1 && am[m].fileIndex == am[m1].fileIndex)
      vwr.setCurrentModelIndexClear(m1, false);
  }  

  /**
   * A generic way to set atom positions, possibly from trajectories but also
   * possibly from an array. Takes care of all associated issues of changing
   * coordinates.
   * 
   * @param baseModelIndex
   * @param modelIndex
   * @param t1
   * @param t2
   * @param f
   * @param vibs
   * @param isFractional
   */
  private void setAtomPositions(int baseModelIndex, int modelIndex,
                                P3[] t1, P3[] t2,
                                float f, V3[] vibs,
                                boolean isFractional) {
    BS bs = new BS();
    V3 vib = new V3();
    int iFirst = am[baseModelIndex].firstAtomIndex;
    int iMax = iFirst + getAtomCountInModel(baseModelIndex);
    if (f == 0) {
      for (int pt = 0, i = iFirst; i < iMax && pt < t1.length; i++, pt++) {
        at[i].mi = (short) modelIndex;
        if (t1[pt] == null)
          continue;
        if (isFractional)
          at[i].setFractionalCoordTo(t1[pt], true);
        else
          at[i].setT(t1[pt]);
        if (vibrationSteps != null) {
          if (vibs != null && vibs[pt] != null)
            vib = vibs[pt];
          setVibrationVector(i, vib);
        }
        bs.set(i);
      }
    } else {
      P3 p = new P3();
      int n = Math.min(t1.length, t2.length);
      for (int pt = 0, i = iFirst; i < iMax && pt < n; i++, pt++) {
        at[i].mi = (short) modelIndex;
        if (t1[pt] == null || t2[pt] == null)
          continue;
        p.sub2(t2[pt], t1[pt]);
        p.scaleAdd2(f, p, t1[pt]);
        if (isFractional)
          at[i].setFractionalCoordTo(p, true);
        else
          at[i].setT(p);
        bs.set(i);
      } 
    }
    // Clear the Binary Search so that select within(),
    // isosurface, and dots will work properly
    initializeBspf();
    validateBspfForModel(baseModelIndex, false);
    // Recalculate critical points for cartoons and such
    // note that models[baseModel] and models[modelIndex]
    // point to the same model. So there is only one copy of 
    // the shape business.

    recalculateLeadMidpointsAndWingVectors(baseModelIndex);
    // Recalculate all measures that involve trajectories

    sm.refreshShapeTrajectories(baseModelIndex, bs, null);

    if (am[baseModelIndex].hasRasmolHBonds) {
      am[baseModelIndex].clearRasmolHydrogenBonds(null);
      am[baseModelIndex].getRasmolHydrogenBonds(bs, bs, null, false,
          Integer.MAX_VALUE, false, null);
    }
  }

  public P3[] getFrameOffsets(BS bsAtoms) {
    if (bsAtoms == null)
      return null;
    P3[] offsets = new P3[mc];
    for (int i = 0; i < mc; i++)
      offsets[i] = new P3();
    int lastModel = 0;
    int n = 0;
    P3 offset = offsets[0];
    boolean asTrajectory = (trajectorySteps != null && trajectorySteps.size() == mc);
    int m1 = (asTrajectory ? mc : 1);
    for (int m = 0; m < m1; m++) {
      if (asTrajectory)
        setTrajectory(m);
      for (int i = 0; i <= ac; i++) {
        if (i == ac || at[i].mi != lastModel) {
          if (n > 0) {
            offset.scale(-1.0f / n);
            if (lastModel != 0)
              offset.sub(offsets[0]);
            n = 0;
          }
          if (i == ac)
            break;
          lastModel = at[i].mi;
          offset = offsets[lastModel];
        }
        if (!bsAtoms.get(i))
          continue;
        offset.add(at[i]);
        n++;
      }
    }
    offsets[0].set(0, 0, 0);
    return offsets;
  }

  /**
   * general lookup for integer type -- from Eval
   * @param tokType   
   * @param specInfo  
   * @return bitset; null only if we mess up with name
   */
  public BS getAtomBits(int tokType, Object specInfo) {
    switch (tokType) {
    default:
      return BSUtil.andNot(getAtomBitsMaybeDeleted(tokType, specInfo), vwr
          .getDeletedAtoms());
    case T.spec_model:
      int modelNumber = ((Integer) specInfo).intValue();
      int modelIndex = getModelNumberIndex(modelNumber, true, true);
      return (modelIndex < 0 && modelNumber > 0 ? new BS()
          : vwr.getModelUndeletedAtomsBitSet(modelIndex));
    }
  }

  public String getAtomLabel(int i) {
    return (String) vwr.getShapePropertyIndex(JC.SHAPE_LABELS, "label", i);
  }
  
  protected final Atom[] closest = new Atom[1];

  public int findNearestAtomIndex(int x, int y, BS bsNot, int min) {
    if (ac == 0)
      return -1;
    closest[0] = null;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    findNearest2(x, y, closest, bsNot, min);
    sm.findNearestShapeAtomIndex(x, y, closest, bsNot);
    int closestIndex = (closest[0] == null ? -1 : closest[0].i);
    closest[0] = null;
    return closestIndex;
  }

  /*
  private Map userProperties;

  void putUserProperty(String name, Object property) {
    if (userProperties == null)
      userProperties = new Hashtable();
    if (property == null)
      userProperties.remove(name);
    else
      userProperties.put(name, property);
  }
*/  

  ///////// atom and shape selecting /////////

  public String calculateStructures(BS bsAtoms, boolean asDSSP,
                                    boolean doReport,
                                    boolean dsspIgnoreHydrogen,
                                    boolean setStructure) {
    BS bsAllAtoms = new BS();
    BS bsModelsExcluded = BSUtil.copyInvert(modelsOf(bsAtoms, bsAllAtoms),
        mc);
    if (!setStructure)
      return calculateStructuresAllExcept(bsModelsExcluded, asDSSP, doReport,
          dsspIgnoreHydrogen, false, false);
    for (int i = 0; i < mc; i++)
      if (!bsModelsExcluded.get(i))
        am[i].clearBioPolymers();
    calculatePolymers(null, 0, 0, bsModelsExcluded);
    String ret = calculateStructuresAllExcept(bsModelsExcluded, asDSSP, doReport,
        dsspIgnoreHydrogen, true, false);
    vwr.resetBioshapes(bsAllAtoms);
    setStructureIndexes();
    return ret;
  }

  public String calculatePointGroup(BS bsAtoms) {
    return (String) calculatePointGroupForFirstModel(bsAtoms, false,
        false, false, null, 0, 0);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getPointGroupInfo(BS bsAtoms) {
    return (Map<String, Object>) calculatePointGroupForFirstModel(bsAtoms, false,
        false, true, null, 0, 0);
  }
  
  public String getPointGroupAsString(BS bsAtoms, boolean asDraw,
                                      String type, int index, float scale) {
    return (String) calculatePointGroupForFirstModel(bsAtoms, true,
        asDraw, false, type, index, scale);
  }

  private SymmetryInterface pointGroup;
  private Object calculatePointGroupForFirstModel(BS bsAtoms,
                                                  boolean doAll,
                                                  boolean asDraw,
                                                  boolean asInfo, String type,
                                                  int index, float scale) {
    int modelIndex = vwr.getCurrentModelIndex();
    int iAtom = (bsAtoms == null ? -1 : bsAtoms.nextSetBit(0));
    if (modelIndex < 0 && iAtom >= 0)
      modelIndex = at[iAtom].getModelIndex();
    if (modelIndex < 0) {
      modelIndex = vwr.getVisibleFramesBitSet().nextSetBit(0);
      bsAtoms = null;
    }
    BS bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
    if (bsAtoms != null)
      bs.and(bsAtoms);
    iAtom = bs.nextSetBit(0);
    if (iAtom < 0) {
      bs = vwr.getModelUndeletedAtomsBitSet(modelIndex);
      iAtom = bs.nextSetBit(0);
    }
    Object obj = vwr.getShapePropertyIndex(JC.SHAPE_VECTORS, "mad", iAtom);
    boolean haveVibration = (obj != null && ((Integer) obj).intValue() != 0 || vwr
        .isVibrationOn());
    SymmetryInterface symmetry = Interface.getSymmetry();
    pointGroup = symmetry.setPointGroup(pointGroup, at, bs, haveVibration,
        vwr.getFloat(T.pointgroupdistancetolerance), vwr.getFloat(T.pointgrouplineartolerance));
    if (!doAll && !asInfo)
      return pointGroup.getPointGroupName();
    Object ret = pointGroup.getPointGroupInfo(modelIndex, asDraw, asInfo, type,
        index, scale);
    if (asInfo)
      return ret;
    return (mc > 1 ? "frame " + getModelNumberDotted(modelIndex) + "; "
        : "") + ret;
  }
  
  private BS modelsOf(BS bsAtoms, BS bsAllAtoms) {
    BS bsModels = BS.newN(mc);
    boolean isAll = (bsAtoms == null);
    int i0 = (isAll ? ac - 1 : bsAtoms.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsAtoms.nextSetBit(i + 1))) {
      int modelIndex = am[at[i].mi].trajectoryBaseIndex;
      if (isJmolDataFrameForModel(modelIndex))
        continue;
      bsModels.set(modelIndex);
      bsAllAtoms.set(i);
    }
    return bsModels;
  }

  public String getDefaultStructure(BS bsAtoms, BS bsAllAtoms) {
    BS bsModels = modelsOf(bsAtoms, bsAllAtoms);
    SB ret = new SB();
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) 
      if (am[i].isBioModel && am[i].defaultStructure != null)
        ret.append(am[i].defaultStructure);
    return ret.toString();
  }


  
  ///// super-overloaded methods ///////
  
  
  public int[] makeConnections(float minDistance, float maxDistance, int order,
                               int connectOperation, BS bsA, BS bsB,
                               BS bsBonds, boolean isBonds, boolean addGroup, float energy) {
    if (connectOperation == T.auto
        && order != Edge.BOND_H_REGULAR) {
      String stateScript = "connect ";
      if (minDistance != JC.DEFAULT_MIN_CONNECT_DISTANCE)
        stateScript += minDistance + " ";
      if (maxDistance != JC.DEFAULT_MAX_CONNECT_DISTANCE)
        stateScript += maxDistance + " ";
      addStateScript(stateScript, (isBonds ? bsA : null),
          (isBonds ? null : bsA), (isBonds ? null : bsB), " auto", false, true);
    }
    moleculeCount = 0;
    return makeConnections2(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds, addGroup, energy);
  }
  
  @SuppressWarnings("unchecked")
  public void setPdbConectBonding(int baseAtomIndex, int baseModelIndex,
                                  BS bsExclude) {
    short mad = vwr.getMadBond();
    for (int i = baseModelIndex; i < mc; i++) {
      List<int[]> vConnect = (List<int[]>) getModelAuxiliaryInfoValue(i, "PDB_CONECT_bonds");
      if (vConnect == null)
        continue;
      int nConnect = vConnect.size();
      setModelAuxiliaryInfo(i, "initialBondCount", Integer.valueOf(nConnect));
      int[] atomInfo = (int[]) getModelAuxiliaryInfoValue(i, "PDB_CONECT_firstAtom_count_max");
      int firstAtom = atomInfo[0] +  baseAtomIndex;
      int atomMax = firstAtom + atomInfo[1];
      int max = atomInfo[2];
      int[] serialMap = new int[max + 1];
      int iSerial;
      for (int iAtom = firstAtom; iAtom < atomMax; iAtom++)
        if ((iSerial = atomSerials[iAtom]) > 0)
          serialMap[iSerial] = iAtom + 1;
      for (int iConnect = 0; iConnect < nConnect; iConnect++) {
        int[] pair = vConnect.get(iConnect);
        int sourceSerial = pair[0];
        int targetSerial = pair[1];
        short order = (short) pair[2];
        if (sourceSerial < 0 || targetSerial < 0 || sourceSerial > max
            || targetSerial > max)
          continue;
        int sourceIndex = serialMap[sourceSerial] - 1;
        int targetIndex = serialMap[targetSerial] - 1;
        if (sourceIndex < 0 || targetIndex < 0)
          continue;
        if (bsExclude != null) {
          if (at[sourceIndex].isHetero())
            bsExclude.set(sourceIndex);
          if (at[targetIndex].isHetero())
            bsExclude.set(targetIndex);
        }
        checkValencesAndBond(at[sourceIndex], at[targetIndex], order,
            (order == Edge.BOND_H_REGULAR ? 1 : mad), null);
      }
    }
  }
  
  public void deleteAllBonds() {
    moleculeCount = 0;
    for (int i = stateScripts.size(); --i >= 0;) { 
      if (stateScripts.get(i).isConnect()) {
        stateScripts.remove(i);
      }
    }
    deleteAllBonds2();
  }

  /* ******************************************************
   * 
   * methods for definining the state 
   * 
   ********************************************************/

  private void includeAllRelatedFrames(BS bsModels) {
    int j;
    for (int i = 0; i < mc; i++) {
      if (bsModels.get(i)) {
       // if (isJmolDataFrame(i) && !bsModels.get(j = models[i].dataSourceFrame)) {
         // bsModels.set(j);
        //  includeAllRelatedFrames(bsModels);
          //return;
       // }
        if (isTrajectory(i) && !bsModels.get(j = am[i].trajectoryBaseIndex)) {
          bsModels.set(j);
          includeAllRelatedFrames(bsModels);
          return;
        }
        continue;
      }
      if (isTrajectory(i) && bsModels.get(am[i].trajectoryBaseIndex)
          || isJmolDataFrameForModel(i) && bsModels.get(am[i].dataSourceFrame))
        bsModels.set(i);
    }
  }
  
  public BS deleteModels(BS bsAtoms) {
    // full models are deleted for any model containing the specified atoms
    moleculeCount = 0;
    BS bsModels = getModelBitSet(bsAtoms, false);
    includeAllRelatedFrames(bsModels);

    int nModelsDeleted = BSUtil.cardinalityOf(bsModels);
    if (nModelsDeleted == 0)
      return null;

    // clear references to this frame if it is a dataFrame

    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      clearDataFrameReference(i);

    BS bsDeleted;
    if (nModelsDeleted == mc) {
      bsDeleted = getModelAtomBitSetIncludingDeleted(-1, true);
      vwr.zap(true, false, false);
      return bsDeleted;
    }

    // zero out reproducible arrays

    validateBspf(false);

    // create a new models array,
    // and pre-calculate Model.bsAtoms and Model.ac
    Model[] newModels = new Model[mc - nModelsDeleted];
    Model[] oldModels = am;
    bsDeleted = new BS();
    for (int i = 0, mpt = 0; i < mc; i++)
      if (bsModels.get(i)) { // get a good count now
        getAtomCountInModel(i);
        bsDeleted.or(getModelAtomBitSetIncludingDeleted(i, false));
      } else {
        am[i].modelIndex = mpt;
        newModels[mpt++] = am[i];
      }
    am = newModels;
    int oldModelCount = mc;
    // delete bonds
    BS bsBonds = getBondsForSelectedAtoms(bsDeleted, true);
    deleteBonds(bsBonds, true);

    // main deletion cycle
    
    for (int i = 0, mpt = 0; i < oldModelCount; i++) {
      if (!bsModels.get(i)) {
        mpt++;
        continue;
      }
      int nAtoms = oldModels[i].ac;
      if (nAtoms == 0)
        continue;
      BS bs = oldModels[i].bsAtoms;
      int firstAtomIndex = oldModels[i].firstAtomIndex;

      // delete from symmetry set
      BSUtil.deleteBits(bsSymmetry, bs);

      // delete from stateScripts, model arrays and bitsets,
      // atom arrays, and atom bitsets
      deleteModel(mpt, firstAtomIndex, nAtoms, bs, bsBonds);

      // adjust all models after this one
      for (int j = oldModelCount; --j > i;)
        oldModels[j].fixIndices(mpt, nAtoms, bs);

      // adjust all shapes
      vwr.deleteShapeAtoms(new Object[] { newModels, at,
          new int[] { mpt, firstAtomIndex, nAtoms } }, bs);
      mc--;
    }

    // set final values
    deleteModel(-1, 0, 0, null, null);
    return bsDeleted;
  }

  public void setAtomProperty(BS bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    switch (tok) {
    case T.backbone:
    case T.cartoon:
    case T.meshRibbon:
    case T.ribbon:
    case T.rocket:
    case T.strands:
    case T.trace:
      if (fValue > Shape.RADIUS_MAX)
        fValue = Shape.RADIUS_MAX;
      if (values != null) {
        // convert to atom indices
        float[] newValues = new float[ac];
        for (int i = bs.nextSetBit(0), ii = 0; i >= 0; i = bs.nextSetBit(i + 1))
          newValues[i] = values[ii++];
        values = newValues;
      }
      //$FALL-THROUGH$
    case T.halo:
    case T.star:
      RadiusData rd = null;
      int mar = 0;
      if (values == null) {
        if (fValue > Atom.RADIUS_MAX)
          fValue = Atom.RADIUS_GLOBAL;
        if (fValue < 0)
          fValue = 0;
        mar = (int) Math.floor(fValue * 2000);
      } else {
        rd = new RadiusData(values, 0, null, null);
      }
      sm
          .setShapeSizeBs(JC.shapeTokenIndex(tok), mar, rd, bs);
      return;
    }
    setAPm(bs, tok, iValue, fValue, sValue, values, list);
  }
  
  @SuppressWarnings("unchecked")
  public Object getFileData(int modelIndex) {
    if (modelIndex < 0)
      return "";
    Map<String, Object> fileData = (Map<String, Object>) getModelAuxiliaryInfoValue(modelIndex, "fileData");
    if (fileData != null)
      return fileData;
    if (!getModelAuxiliaryInfoBoolean(modelIndex, "isCIF"))
      return getPDBHeader(modelIndex);
    fileData = vwr.getCifData(modelIndex);
    setModelAuxiliaryInfo(modelIndex, "fileData", fileData);
    return fileData;
  }
  
  /** see comments in org.jmol.modelsetbio.AlphaPolymer.java
   * 
   * Struts are calculated for atoms in bs1 connecting to atoms in bs2.
   * The two bitsets may overlap. 
   * 
   * @param bs1
   * @param bs2
   * @return     number of struts found
   */
  @Override
  public int calculateStruts(BS bs1, BS bs2) {
    vwr.setModelVisibility();
    return calculateStrutsMC(bs1, bs2);
  }

  /**
   * these are hydrogens that are being added due to a load 2D command and are
   * therefore not to be flagged as NEW
   * 
   * @param vConnections
   * @param pts
   * @return            BitSet of new atoms
   */
  public BS addHydrogens(List<Atom> vConnections, P3[] pts) {
    int modelIndex = mc - 1;
    BS bs = new BS();
    if (isTrajectory(modelIndex) || am[modelIndex].getGroupCount() > 1) {
      return bs; // can't add atoms to a trajectory or a system with multiple groups!
    }
    growAtomArrays(ac + pts.length);
    RadiusData rd = vwr.getDefaultRadiusData();
    short mad = getDefaultMadFromOrder(1);
    for (int i = 0, n = am[modelIndex].ac + 1; i < vConnections.size(); i++, n++) {
      Atom atom1 = vConnections.get(i);
      // hmm. atom1.group will not be expanded, though...
      // something like within(group,...) will not select these atoms!
      Atom atom2 = addAtom(modelIndex, atom1.group, 1, "H"
          + n, n, n, pts[i], Float.NaN, null, 0, 0, 100, Float.NaN, null, false, (byte) 0, null);
      
      atom2.setMadAtom(vwr, rd);
      bs.set(atom2.i);
      bondAtoms(atom1, atom2, Edge.BOND_COVALENT_SINGLE, mad, null, 0, false, false);
    }
    // must reset the shapes to give them new atom counts and arrays
    sm.loadDefaultShapes(this);
    return bs;
  }

  public void setAtomCoordsRelative(T3 offset, BS bs) {
    setAtomsCoordRelative(bs, offset.x, offset.y, offset.z);
    mat4.setIdentity();
    vTemp.setT(offset);
    mat4.setTranslation(vTemp);
    recalculatePositionDependentQuantities(bs, mat4);
  }

  public void setAtomCoords(BS bs, int tokType, Object xyzValues) {
    setAtomCoord2(bs, tokType, xyzValues);
    switch(tokType) {
    case T.vibx:
    case T.viby:
    case T.vibz:
    case T.vibxyz:
      break;
    default:
      recalculatePositionDependentQuantities(bs, null);
    }
  }

  public void invertSelected(P3 pt, P4 plane, int iAtom,
                             BS invAtoms, BS bs) {
    if (pt != null) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        float x = (pt.x - at[i].x) * 2;
        float y = (pt.y - at[i].y) * 2;
        float z = (pt.z - at[i].z) * 2;
        setAtomCoordRelative(i, x, y, z);
      }
      return;
    }
    if (plane != null) {
      // ax + by + cz + d = 0
      V3 norm = V3.new3(plane.x, plane.y, plane.z);
      norm.normalize();
      float d = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y
          + plane.z * plane.z);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        float twoD = -Measure.distanceToPlaneD(plane, d, at[i]) * 2;
        float x = norm.x * twoD;
        float y = norm.y * twoD;
        float z = norm.z * twoD;
        setAtomCoordRelative(i, x, y, z);
      }
      return;
    }
    if (iAtom >= 0) {
      Atom thisAtom = at[iAtom];
      // stereochemical inversion at iAtom
      Bond[] bonds = thisAtom.bonds;
      if (bonds == null)
        return;
      BS bsAtoms = new BS();
      List<P3> vNot = new  List<P3>();
      BS bsModel = vwr.getModelUndeletedAtomsBitSet(thisAtom.mi);
      for (int i = 0; i < bonds.length; i++) {
        Atom a = bonds[i].getOtherAtom(thisAtom);
        if (invAtoms.get(a.i)) {
            bsAtoms.or(JmolMolecule.getBranchBitSet(at, a.i, bsModel, null, iAtom, true, true));
        } else {
          vNot.addLast(a);
        }
      }
      if (vNot.size() == 0)
        return;
      pt = Measure.getCenterAndPoints(vNot)[0];
      V3 v = V3.newVsub(thisAtom, pt);
      Quat q = Quat.newVA(v, 180);
      moveAtoms(null, q.getMatrix(), null, bsAtoms, thisAtom, true, false);
    }
  }

  private final M3 matTemp = new M3();
  private final M3 matInv = new M3();
  private final M4 mat4 = new M4();
  private final M4 mat4t = new M4();
  private final V3 vTemp = new V3();

  public void setDihedrals(float[] dihedralList, BS[] bsBranches, float f) {
    int n = dihedralList.length / 6;
    if (f > 1)
      f = 1;
    for (int j = 0, pt = 0; j < n; j++, pt += 6) {
      BS bs = bsBranches[j];
      if (bs == null || bs.isEmpty())
        continue;
      Atom a1 = at[(int) dihedralList[pt + 1]];
      V3 v = V3.newVsub(at[(int) dihedralList[pt + 2]], a1);
      float angle = (dihedralList[pt + 5] - dihedralList[pt + 4]) * f;
      A4 aa = A4.newVA(v, (float)(-angle / TransformManager.degreesPerRadian));
      matTemp.setAA(aa);
      ptTemp.setT(a1);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        at[i].sub(ptTemp);
        matTemp.rotate(at[i]);
        at[i].add(ptTemp);
        taintAtom(i, TAINT_COORD);
      }
    }    
  }

  public void moveAtoms(M3 mNew, M3 matrixRotate, V3 translation, BS bs,
                        P3 center, boolean isInternal, boolean translationOnly) {
    if (!translationOnly) {
      if (mNew == null) {
        matTemp.setM3(matrixRotate);
      } else {
        matInv.setM3(matrixRotate);
        matInv.invert();
        ptTemp.set(0, 0, 0);
        matTemp.mul2(mNew, matrixRotate);
        matTemp.mul2(matInv, matTemp);
      }
      if (isInternal) {
        vTemp.setT(center);
        mat4.setIdentity();
        mat4.setTranslation(vTemp);
        mat4t.setToM3(matTemp);
        mat4.mul(mat4t);
        mat4t.setIdentity();
        vTemp.scale(-1);
        mat4t.setTranslation(vTemp);
        mat4.mul(mat4t);
      } else {
        mat4.setToM3(matTemp);
      }
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (isInternal) {
          mat4.rotTrans(at[i]);
        } else {
          ptTemp.add(at[i]);
          mat4.rotTrans(at[i]);
          ptTemp.sub(at[i]);
        }
        taintAtom(i, TAINT_COORD);
      }
      if (!isInternal) {
        ptTemp.scale(1f / bs.cardinality());
        if (translation == null)
          translation = new V3();
        translation.add(ptTemp);
      }
    }
    if (translation != null) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        at[i].add(translation);
      if (!translationOnly) {
        mat4t.setIdentity();
        mat4t.setTranslation(translation);
        mat4.mul2(mat4t, mat4);
      }
    }
    recalculatePositionDependentQuantities(bs, mat4);
  }

  public void recalculatePositionDependentQuantities(BS bs, M4 mat) {
    if (getHaveStraightness())
      calculateStraightness();
    recalculateLeadMidpointsAndWingVectors(-1);
    BS bsModels = getModelBitSet(bs, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      sm.refreshShapeTrajectories(i, bs, mat);
    averageAtomPoint = null;
    /* but we would need to somehow indicate this in the state
    if (ellipsoids != null)
      for (int i = bs.nextSetBit(0); i >= 0 && i < ellipsoids.length; i = bs.nextSetBit(i + 1))
        ellipsoids[i].rotate(mat);
    if (vibrationVectors != null)
      for (int i = bs.nextSetBit(0); i >= 0 && i < vibrationVectors.length; i = bs.nextSetBit(i + 1))
        if (vibrationVectors[i] != null)
            mat.transform(vibrationVectors[i]);
            */
  }

  public BS[] getBsBranches(float[] dihedralList) {
    int n = dihedralList.length / 6;
    BS[] bsBranches = new BS[n];
    Map<String, Boolean> map = new Hashtable<String, Boolean>();
    for (int i = 0, pt = 0; i < n; i++, pt += 6) {
      float dv = dihedralList[pt + 5] - dihedralList[pt + 4];
      if (Math.abs(dv) < 1f)
        continue;
      int i0 = (int) dihedralList[pt + 1];
      int i1 = (int) dihedralList[pt + 2];
      String s = "" + i0 + "_" + i1;
      if (map.containsKey(s))
        continue;
      map.put(s, Boolean.TRUE);
      BS bs = vwr.getBranchBitSet(i1, i0, true);
      Bond[] bonds = at[i0].bonds;
      Atom a0 = at[i0];
      for (int j = 0; j < bonds.length; j++) {
        Bond b = bonds[j];
        if (!b.isCovalent())
          continue;
        int i2 = b.getOtherAtom(a0).i;
        if (i2 == i1)
          continue;
        if (bs.get(i2)) {
          bs = null;
          break;
        }
      }
      bsBranches[i] = bs;
    }
    return bsBranches;
  }

  public M4[] getSymMatrices(int modelIndex) {
    int n = getModelSymmetryCount(modelIndex);
    if (n == 0)
      return null;
    M4[] ops = new M4[n];
    SymmetryInterface unitcell = am[modelIndex].biosymmetry;
    if (unitcell == null)
      unitcell = vwr.getModelUnitCell(modelIndex);
    for (int i = n; --i >= 0;)
      ops[i] = unitcell.getSpaceGroupOperation(i);
    return ops;
  }

}

