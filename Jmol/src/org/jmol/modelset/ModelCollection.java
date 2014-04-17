/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2007-10-14 12:33:20 -0500 (Sun, 14 Oct 2007) $
 * $Revision: 8408 $

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

import javajs.J2SIgnoreImport;
import javajs.util.AU;
import javajs.util.Lst;
import javajs.util.P3;
import javajs.util.P4;
import javajs.util.PT;
import javajs.util.Quat;
import javajs.util.SB;
import javajs.util.T3;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import java.util.Map;
import java.util.Properties;

import org.jmol.api.AtomIndexIterator;
import org.jmol.api.Interface;
import org.jmol.api.JmolDSSRParser;
import org.jmol.api.JmolModulationSet;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
import org.jmol.bspt.Bspf;
import org.jmol.bspt.CubeIterator;
import org.jmol.c.PAL;
import org.jmol.c.STR;
import org.jmol.c.VDW;
import org.jmol.util.BSUtil;
import org.jmol.util.Escape;

import org.jmol.util.BoxInfo;
import org.jmol.util.Elements;

import org.jmol.util.Point3fi;
import org.jmol.util.Tensor;
import org.jmol.util.Edge;
import org.jmol.util.JmolMolecule;
import org.jmol.util.Logger;
import org.jmol.util.Triangulator;
import org.jmol.util.Txt;
import javajs.util.V3;
import org.jmol.util.Vibration;
import org.jmol.viewer.JC;
import org.jmol.viewer.ShapeManager;
import org.jmol.java.BS;
import org.jmol.script.T;

@J2SIgnoreImport({javajs.util.XmlUtil.class})
abstract public class ModelCollection extends BondCollection {

  /**
   * initial transfer of model data from old to new model set. Note that all new
   * models are added later, AFTER thfe old ones. This is very important,
   * because all of the old atom numbers must map onto the same numbers in the
   * new model set, or the state script will not run properly, among other
   * problems.
   * 
   * @param mergeModelSet
   */
  protected void mergeModelArrays(ModelSet mergeModelSet) {
    at = mergeModelSet.at;
    bo = mergeModelSet.bo;
    stateScripts = mergeModelSet.stateScripts;
    proteinStructureTainted = mergeModelSet.proteinStructureTainted;
    thisStateModel = -1;
    bsSymmetry = mergeModelSet.bsSymmetry;
    modelFileNumbers = mergeModelSet.modelFileNumbers; // file * 1000000 + modelInFile (1-based)
    modelNumbersForAtomLabel = mergeModelSet.modelNumbersForAtomLabel;
    modelNames = mergeModelSet.modelNames;
    modelNumbers = mergeModelSet.modelNumbers;
    frameTitles = mergeModelSet.frameTitles;
    mergeAtomArrays(mergeModelSet);
  }

  @Override
  protected void releaseModelSet() {
    /*
     * Probably unnecessary, but here for general accounting.
     * 
     * I added this when I was trying to track down a memory bug.
     * I know that you don't have to do this, but I was concerned
     * that somewhere in this mess was a reference to modelSet. 
     * As it turns out, it was in models[i] (which was not actually
     * nulled but, rather, transferred to the new model set anyway).
     * Quite amazing that that worked at all, really. Some models were
     * referencing the old modelset, some the new. Yeiks!
     * 
     * Bob Hanson 11/7/07
     * 
     */
    am = null;
    bsSymmetry = null;
    bsAll = null;
    unitCells = null;
    releaseModelSetBC();
  }

  protected BS bsSymmetry;

  public String modelSetName;
  
  public Model[] am = new Model[1];
  /**
   * model count
   */
  public int mc;
  public SymmetryInterface[] unitCells;
  public boolean haveUnitCells;

  public SymmetryInterface getUnitCell(int modelIndex) {
    if (!haveUnitCells || modelIndex < 0 || modelIndex >= mc)
      return null;
    if (am[modelIndex].simpleCage != null)
      return am[modelIndex].simpleCage;
    return (unitCells == null || modelIndex >= unitCells.length
        || !unitCells[modelIndex].haveUnitCell() ? null : unitCells[modelIndex]);
  }

  public void setModelCage(int modelIndex, SymmetryInterface simpleCage) {
    if (modelIndex < 0 || modelIndex >= mc)
      return;
    am[modelIndex].simpleCage = simpleCage;
    haveUnitCells = true;
  }

  /**
   * 
   * @param type
   * @param plane
   * @param scale
   * @param uc
   * @param flags
   *        1 -- edges only 2 -- triangles only 3 -- both
   * @return Vector
   */
  public Lst<Object> getPlaneIntersection(int type, P4 plane, float scale,
                                           int flags, SymmetryInterface uc) {
    T3[] pts = null;
    switch (type) {
    case T.unitcell:
      if (uc == null)
        return null;
      pts = uc.getCanonicalCopy(scale, true);
      break;
    case T.boundbox:
      pts = boxInfo.getCanonicalCopy(scale);
      break;
    }
    Lst<Object> v = new Lst<Object>();
    v.addLast(pts);
    return intersectPlane(plane, v, flags);
  }

  protected int[] modelNumbers = new int[1]; // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
  protected int[] modelFileNumbers = new int[1]; // file * 1000000 + modelInFile (1-based)
  protected String[] modelNumbersForAtomLabel = new String[1];
  protected String[] modelNames = new String[1];
  public String[] frameTitles = new String[1];

  public String getModelName(int modelIndex) {
    return mc < 1 ? "" : modelIndex >= 0 ? modelNames[modelIndex]
        : modelNumbersForAtomLabel[-1 - modelIndex];
  }

  public String getModelTitle(int modelIndex) {
    return (String) getInfo(modelIndex, "title");
  }

  public String getModelFileName(int modelIndex) {
    return (String) getInfo(modelIndex, "fileName");
  }

  public String getModelFileType(int modelIndex) {
    return (String) getInfo(modelIndex, "fileType");
  }

  public void setFrameTitle(BS bsFrames, Object title) {
    if (title instanceof String) {
      for (int i = bsFrames.nextSetBit(0); i >= 0; i = bsFrames
          .nextSetBit(i + 1))
        frameTitles[i] = (String) title;
    } else {
      String[] list = (String[]) title;
      for (int i = bsFrames.nextSetBit(0), n = 0; i >= 0; i = bsFrames
          .nextSetBit(i + 1))
        if (n < list.length)
          frameTitles[i] = list[n++];
    }
  }

  public String getFrameTitle(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc ? frameTitles[modelIndex]
        : "");
  }

  public String getModelNumberForAtomLabel(int modelIndex) {
    return modelNumbersForAtomLabel[modelIndex];
  }

  protected BS[] elementsPresent;

  protected boolean isXYZ;
  protected boolean isPDB;

  public Properties modelSetProperties;
  protected Map<String, Object> msInfo;

  protected void calculatePolymers(Group[] groups, int groupCount,
                                   int baseGroupIndex, BS modelsExcluded) {
    if (!isPDB)
      return;

    boolean checkConnections = !vwr.getBoolean(T.pdbsequential);
    for (int i = 0; i < mc; i++)
      if ((modelsExcluded == null || !modelsExcluded.get(i))
          && am[i].isBioModel) {
        am[i].calculatePolymers(groups, groupCount, baseGroupIndex,
            modelsExcluded, checkConnections);
        return;
      }
  }

  /**
   * In versions earlier than 12.1.51, groups[] was a field of ModelCollection.
   * But this is not necessary, and it was wasting space. This method is only
   * called when polymers are recreated.
   * 
   * @return full array of groups in modelSet
   */
  public Group[] getGroups() {
    int n = 0;
    for (int i = 0; i < mc; i++)
      n += am[i].getGroupCount();
    Group[] groups = new Group[n];
    for (int i = 0, iGroup = 0; i < mc; i++)
      for (int j = 0; j < am[i].chainCount; j++)
        for (int k = 0; k < am[i].chains[j].groupCount; k++) {
          groups[iGroup] = am[i].chains[j].groups[k];
          groups[iGroup].groupIndex = iGroup;
          iGroup++;
        }
    return groups;
  }

  /**
   * deprecated due to multimodel issues, but required by an interface -- do NOT
   * remove.
   * 
   * @return just the first unit cell
   * 
   */
  public float[] getNotionalUnitcell() {
    SymmetryInterface c = getUnitCell(0);
    return (c == null ? null : c.getNotionalUnitCell());
  }

  //new way:

  protected boolean someModelsHaveSymmetry;
  protected boolean someModelsHaveAromaticBonds;
  protected boolean someModelsHaveFractionalCoordinates;

  public boolean setCrystallographicDefaults() {
    return !isPDB && someModelsHaveSymmetry
        && someModelsHaveFractionalCoordinates;
  }

  protected final P3 ptTemp = new P3();

  ////////////////////////////////////////////

  private boolean isBbcageDefault;
  private BS bboxModels;
  private BS bboxAtoms;
  private final BoxInfo boxInfo = new BoxInfo();
  {
    boxInfo.addBoundBoxPoint(P3.new3(-10, -10, -10));
    boxInfo.addBoundBoxPoint(P3.new3(10, 10, 10));
  }

  public P3 getBoundBoxCenter(int modelIndex) {
    if (isJmolDataFrameForModel(modelIndex))
      return new P3();
    return boxInfo.getBoundBoxCenter();
  }

  public V3 getBoundBoxCornerVector() {
    return boxInfo.getBoundBoxCornerVector();
  }

  public Point3fi[] getBBoxVertices() {
    return boxInfo.getBoundBoxVertices();
  }

  public BS getBoundBoxModels() {
    return bboxModels;
  }

  public void setBoundBox(P3 pt1, P3 pt2, boolean byCorner, float scale) {
    isBbcageDefault = false;
    bboxModels = null;
    bboxAtoms = null;
    boxInfo.setBoundBox(pt1, pt2, byCorner, scale);
  }

  public String getBoundBoxCommand(boolean withOptions) {
    if (!withOptions && bboxAtoms != null)
      return "boundbox " + Escape.eBS(bboxAtoms);
    ptTemp.setT(boxInfo.getBoundBoxCenter());
    V3 bbVector = boxInfo.getBoundBoxCornerVector();
    String s = (withOptions ? "boundbox " + Escape.eP(ptTemp) + " "
        + Escape.eP(bbVector) + "\n#or\n" : "");
    ptTemp.sub(bbVector);
    s += "boundbox corners " + Escape.eP(ptTemp) + " ";
    ptTemp.scaleAdd2(2, bbVector, ptTemp);
    float v = Math.abs(8 * bbVector.x * bbVector.y * bbVector.z);
    s += Escape.eP(ptTemp) + " # volume = " + v;
    return s;
  }

  public VDW getDefaultVdwType(int modelIndex) {
    return (!am[modelIndex].isBioModel ? VDW.AUTO_BABEL
        : am[modelIndex].hydrogenCount == 0 ? VDW.AUTO_JMOL
            : VDW.AUTO_BABEL); // RASMOL is too small
  }

  public boolean setRotationRadius(int modelIndex, float angstroms) {
    if (isJmolDataFrameForModel(modelIndex)) {
      am[modelIndex].defaultRotationRadius = angstroms;
      return false;
    }
    return true;
  }

  public float calcRotationRadius(int modelIndex, P3 center) {
    if (isJmolDataFrameForModel(modelIndex)) {
      float r = am[modelIndex].defaultRotationRadius;
      return (r == 0 ? 10 : r);
    }
    float maxRadius = 0;
    for (int i = ac; --i >= 0;) {
      if (isJmolDataFrameForAtom(at[i])) {
        modelIndex = at[i].mi;
        while (i >= 0 && at[i].mi == modelIndex)
          i--;
        continue;
      }
      Atom atom = at[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  public void calcBoundBoxDimensions(BS bs, float scale) {
    if (bs != null && bs.nextSetBit(0) < 0)
      bs = null;
    if (bs == null && isBbcageDefault || ac == 0)
      return;
    bboxModels = getModelBS(bboxAtoms = BSUtil.copy(bs), false);
    if (calcAtomsMinMax(bs, boxInfo) == ac)
      isBbcageDefault = true;
    if (bs == null) { // from modelLoader or reset
      if (unitCells != null)
        calcUnitCellMinMax();
    }
    boxInfo.setBbcage(scale);
  }

  public BoxInfo getBoxInfo(BS bs, float scale) {
    if (bs == null)
      return boxInfo;
    BoxInfo bi = new BoxInfo();
    calcAtomsMinMax(bs, bi);
    bi.setBbcage(scale);
    return bi;
  }

  public int calcAtomsMinMax(BS bs, BoxInfo boxInfo) {
    boxInfo.reset();
    int nAtoms = 0;
    boolean isAll = (bs == null);
    int i0 = (isAll ? ac - 1 : bs.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bs.nextSetBit(i + 1))) {
      nAtoms++;
      if (!isJmolDataFrameForAtom(at[i]))
        boxInfo.addBoundBoxPoint(at[i]);
    }
    return nAtoms;
  }

  private void calcUnitCellMinMax() {
    for (int i = 0; i < mc; i++) {
      if (!unitCells[i].getCoordinatesAreFractional())
        continue;
      P3[] vertices = unitCells[i].getUnitCellVertices();
      for (int j = 0; j < 8; j++)
        boxInfo.addBoundBoxPoint(vertices[j]);
    }
  }

  public float calcRotationRadiusBs(BS bs) {
    // Eval getZoomFactor
    P3 center = getAtomSetCenter(bs);
    float maxRadius = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Atom atom = at[i];
      float distAtom = center.distance(atom);
      float outerVdw = distAtom + getRadiusVdwJmol(atom);
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    return (maxRadius == 0 ? 10 : maxRadius);
  }

  /**
   * 
   * @param vAtomSets
   * @param addCenters
   * @return array of two lists of points, centers first if desired
   */

  public P3[][] getCenterAndPoints(Lst<Object[]> vAtomSets, boolean addCenters) {
    BS bsAtoms1, bsAtoms2;
    int n = (addCenters ? 1 : 0);
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      Object[] bss = vAtomSets.get(ii);
      bsAtoms1 = (BS) bss[0];
      if (bss[1] instanceof BS) {
        bsAtoms2 = (BS) bss[1];
        n += Math.min(bsAtoms1.cardinality(), bsAtoms2.cardinality());
      } else {
        n += Math.min(bsAtoms1.cardinality(), ((P3[]) bss[1]).length);
      }
    }
    P3[][] points = new P3[2][n];
    if (addCenters) {
      points[0][0] = new P3();
      points[1][0] = new P3();
    }
    for (int ii = vAtomSets.size(); --ii >= 0;) {
      Object[] bss = vAtomSets.get(ii);
      bsAtoms1 = (BS) bss[0];
      if (bss[1] instanceof BS) {
        bsAtoms2 = (BS) bss[1];
        for (int i = bsAtoms1.nextSetBit(0), j = bsAtoms2.nextSetBit(0); i >= 0
            && j >= 0; i = bsAtoms1.nextSetBit(i + 1), j = bsAtoms2
            .nextSetBit(j + 1)) {
          points[0][--n] = at[i];
          points[1][n] = at[j];
          if (addCenters) {
            points[0][0].add(at[i]);
            points[1][0].add(at[j]);
          }
        }
      } else {
        P3[] coords = (P3[]) bss[1];
        for (int i = bsAtoms1.nextSetBit(0), j = 0; i >= 0 && j < coords.length; i = bsAtoms1
            .nextSetBit(i + 1), j++) {
          points[0][--n] = at[i];
          points[1][n] = coords[j];
          if (addCenters) {
            points[0][0].add(at[i]);
            points[1][0].add(coords[j]);
          }
        }
      }
    }
    if (addCenters) {
      points[0][0].scale(1f / (points[0].length - 1));
      points[1][0].scale(1f / (points[1].length - 1));
    }
    return points;
  }


  public P3 getAtomSetCenter(BS bs) {
    P3 ptCenter = new P3();
    int nPoints = 0;
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (!isJmolDataFrameForAtom(at[i])) {
        nPoints++;
        ptCenter.add(at[i]);
      }
    }
    if (nPoints > 0)
      ptCenter.scale(1.0f / nPoints);
    return ptCenter;
  }

  public P3 getAverageAtomPoint() {
    if (averageAtomPoint == null)
      (averageAtomPoint = new P3()).setT(getAtomSetCenter(vwr.getAllAtoms()));
    return averageAtomPoint;
  }

  protected void setAPm(BS bs, int tok, int iValue, float fValue,
                        String sValue, float[] values, String[] list) {
    setAPa(bs, tok, iValue, fValue, sValue, values, list);
    switch (tok) {
    case T.valence:
    case T.formalcharge:
      if (vwr.getBoolean(T.smartaromatic))
        assignAromaticBonds();
      break;
    }
  }

  public Lst<StateScript> stateScripts = new Lst<StateScript>();
  /*
   * stateScripts are connect commands that must be executed in sequence.
   * 
   * What I fear is that in deleting models we must delete these connections,
   * and in deleting atoms, the bitsets may not be retrieved properly. 
   * 
   * 
   */
  private int thisStateModel = 0;

  public StateScript addStateScript(String script1, BS bsBonds, BS bsAtoms1,
                                    BS bsAtoms2, String script2,
                                    boolean addFrameNumber,
                                    boolean postDefinitions) {
    int iModel = vwr.am.cmi;
    if (addFrameNumber) {
      if (thisStateModel != iModel)
        script1 = "frame "
            + (iModel < 0 ? "all #" + iModel : getModelNumberDotted(iModel))
            + ";\n  " + script1;
      thisStateModel = iModel;
    } else {
      thisStateModel = -1;
    }
    StateScript stateScript = new StateScript(thisStateModel, script1, bsBonds,
        bsAtoms1, bsAtoms2, script2, postDefinitions);
    if (stateScript.isValid()) {
      stateScripts.addLast(stateScript);
    }
    return stateScript;
  }

  /**
   * allows rebuilding of PDB structures; also accessed by ModelManager from
   * Eval
   * 
   * @param alreadyDefined
   *        set to skip calculation
   * @param asDSSP
   * @param doReport
   * @param dsspIgnoreHydrogen
   * @param setStructure
   * @param includeAlpha
   * @return report
   * 
   */
  protected String calculateStructuresAllExcept(BS alreadyDefined,
                                                boolean asDSSP,
                                                boolean doReport,
                                                boolean dsspIgnoreHydrogen,
                                                boolean setStructure,
                                                boolean includeAlpha) {
    freezeModels();
    String ret = "";
    BS bsModels = BSUtil.copyInvert(alreadyDefined, mc);
    //working here -- testing reset
    //TODO bsModels first for not setStructure, after that for setstructure....
    if (setStructure)
      setDefaultStructure(bsModels);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1)) {
      ret += am[i].calculateStructures(asDSSP, doReport,
          dsspIgnoreHydrogen, setStructure, includeAlpha);
    }
    if (setStructure) {
      setStructureIndexes();
    }
    return ret;
  }

  public void setDefaultStructure(BS bsModels) {
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      if (am[i].isBioModel && am[i].defaultStructure == null)
        am[i].defaultStructure = getProteinStructureState(
            am[i].bsAtoms, false, false, 0);
  }

  public void setProteinType(BS bs, STR type) {
    int monomerIndexCurrent = -1;
    int iLast = -1;
    BS bsModels = getModelBS(bs, false);
    setDefaultStructure(bsModels);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (iLast != i - 1)
        monomerIndexCurrent = -1;
      monomerIndexCurrent = at[i].group.setProteinStructureType(type,
          monomerIndexCurrent);
      int modelIndex = at[i].mi;
      proteinStructureTainted = am[modelIndex].structureTainted = true;
      iLast = i = at[i].group.lastAtomIndex;
    }
    int[] lastStrucNo = new int[mc];
    for (int i = 0; i < ac;) {
      int modelIndex = at[i].mi;
      if (!bsModels.get(modelIndex)) {
        i = am[modelIndex].firstAtomIndex + am[modelIndex].ac;
        continue;
      }
      iLast = at[i].getStrucNo();
      if (iLast < 1000 && iLast > lastStrucNo[modelIndex])
        lastStrucNo[modelIndex] = iLast;
      i = at[i].group.lastAtomIndex + 1;
    }
    for (int i = 0; i < ac;) {
      int modelIndex = at[i].mi;
      if (!bsModels.get(modelIndex)) {
        i = am[modelIndex].firstAtomIndex + am[modelIndex].ac;
        continue;
      }
      if (at[i].getStrucNo() > 1000)
        at[i].group.setStrucNo(++lastStrucNo[modelIndex]);
      i = at[i].group.lastAtomIndex + 1;
    }
  }

  void freezeModels() {
    for (int iModel = mc; --iModel >= 0;)
      am[iModel].freeze();
  }

  public Map<STR, float[]> getStructureList() {
    return vwr.getStructureList();
  }

  public void setStructureList(Map<STR, float[]> structureList) {
    for (int iModel = mc; --iModel >= 0;)
      am[iModel].setStructureList(structureList);
  }

  public BS setConformation(BS bsAtoms) {
    BS bsModels = getModelBS(bsAtoms, false);
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      am[i].setConformation(bsAtoms);
    return bsAtoms;
  }

  public BS getConformation(int modelIndex, int conformationIndex, boolean doSet) {
    BS bs = new BS();
    for (int i = mc; --i >= 0;)
      if (i == modelIndex || modelIndex < 0) {
        String altLocs = getAltLocListInModel(i);
        int nAltLocs = getAltLocCountInModel(i);
        if (conformationIndex > 0 && conformationIndex >= nAltLocs)
          continue;
        BS bsConformation = vwr.getModelUndeletedAtomsBitSet(i);
        if (conformationIndex >= 0) {
          if (!am[i].getPdbConformation(bsConformation, conformationIndex))
            for (int c = nAltLocs; --c >= 0;)
              if (c != conformationIndex)
                bsConformation.andNot(getAtomBitsMDa(T.spec_alternate,
                    altLocs.substring(c, c + 1)));
        }
        if (bsConformation.nextSetBit(0) >= 0) {
          bs.or(bsConformation);
          if (doSet)
            am[i].setConformation(bsConformation);
        }
      }
    return bs;
  }

  @SuppressWarnings("unchecked")
  public Map<String, String> getHeteroList(int modelIndex) {
    Map<String, String> htFull = new Hashtable<String, String>();
    boolean ok = false;
    for (int i = mc; --i >= 0;)
      if (modelIndex < 0 || i == modelIndex) {
        Map<String, String> ht = (Map<String, String>) getInfo(
            i, "hetNames");
        if (ht == null)
          continue;
        ok = true;
        for (Map.Entry<String, String> entry : ht.entrySet()) {
          String key = entry.getKey();
          htFull.put(key, entry.getValue());
        }
      }
    return (ok ? htFull
        : (Map<String, String>) getInfoM("hetNames"));
  }

  public Properties getMSProperties() {
    return modelSetProperties;
  }

  public Map<String, Object> getMSInfo() {
    return msInfo;
  }

  public Object getInfoM(String keyName) {
    // the preferred method now
    return (msInfo == null ? null : msInfo
        .get(keyName));
  }

  public boolean getMSInfoB(String keyName) {
    Object val = getInfoM(keyName);
    return (val instanceof Boolean && ((Boolean) val).booleanValue());
  }

  /*
    int getModelSetAuxiliaryInfoInt(String keyName) {
      if (modelSetAuxiliaryInfo != null
          && modelSetAuxiliaryInfo.contains(keyName)) {
        return ((Integer) modelSetAuxiliaryInfo.get(keyName)).intValue();
      }
      return Integer.MIN_VALUE;
    }
  */

  public Lst<P3[]> trajectorySteps;
  protected Lst<V3[]> vibrationSteps;

  protected int mergeTrajectories(boolean isTrajectory) {
    if (trajectorySteps == null) {
      if (!isTrajectory)
        return 0;
      trajectorySteps = new Lst<P3[]>();
    }
    for (int i = trajectorySteps.size(); i < mc; i++)
      trajectorySteps.addLast(null);
    return mc;
  }

  public int getTrajectoryIndex(int modelIndex) {
    return am[modelIndex].trajectoryBaseIndex;
  }

  public boolean isTrajectory(int modelIndex) {
    return am[modelIndex].isTrajectory;
  }

  public boolean isTrajectoryMeasurement(int[] countPlusIndices) {
    if (countPlusIndices == null)
      return false;
    int count = countPlusIndices[0];
    int atomIndex;
    for (int i = 1; i <= count; i++)
      if ((atomIndex = countPlusIndices[i]) >= 0
          && am[at[atomIndex].mi].isTrajectory)
        return true;
    return false;
  }

  public BS getModelBS(BS atomList, boolean allTrajectories) {
    BS bs = new BS();
    int modelIndex = 0;
    boolean isAll = (atomList == null);
    int i0 = (isAll ? 0 : atomList.nextSetBit(0));
    for (int i = i0; i >= 0 && i < ac; i = (isAll ? i + 1 : atomList
        .nextSetBit(i + 1))) {
      bs.set(modelIndex = at[i].mi);
      if (allTrajectories) {
        int iBase = am[modelIndex].trajectoryBaseIndex;
        for (int j = 0; j < mc; j++)
          if (am[j].trajectoryBaseIndex == iBase)
            bs.set(j);
      }
      i = am[modelIndex].firstAtomIndex + am[modelIndex].ac - 1;
    }
    return bs;
  }

  /**
   * only some models can be iterated through. models for which
   * trajectoryBaseIndexes[i] != i are trajectories only
   * 
   * @param allowJmolData
   * @return bitset of models
   */
  public BS getIterativeModels(boolean allowJmolData) {
    BS bs = new BS();
    for (int i = 0; i < mc; i++) {
      if (!allowJmolData && isJmolDataFrameForModel(i))
        continue;
      if (am[i].trajectoryBaseIndex == i)
        bs.set(i);
    }
    return bs;
  }

  public boolean isTrajectorySubFrame(int i) {
    return (am[i].isTrajectory && am[i].trajectoryBaseIndex != i);
  }

  public BS selectDisplayedTrajectories(BS bs) {
    //when a trajectory is selected, the atom's modelIndex is
    //switched to that of the selected trajectory
    //even though the underlying model itself is not changed.
    for (int i = 0; i < mc; i++) {
      if (am[i].isTrajectory
          && at[am[i].firstAtomIndex].mi != i)
        bs.clear(i);
    }
    return bs;
  }

  public void fillAtomData(AtomData atomData, int mode) {
    if ((mode & AtomData.MODE_FILL_MOLECULES) != 0) {
      getMolecules();
      atomData.bsMolecules = new BS[molecules.length];
      atomData.atomMolecule = new int[ac];
      BS bs;
      for (int i = 0; i < molecules.length; i++) {
        bs = atomData.bsMolecules[i] = molecules[i].atomList;
        for (int iAtom = bs.nextSetBit(0); iAtom >= 0; iAtom = bs
            .nextSetBit(iAtom + 1))
          atomData.atomMolecule[iAtom] = i;
      }
    }
    if ((mode & AtomData.MODE_GET_ATTACHED_HYDROGENS) != 0) {
      int[] nH = new int[1];
      atomData.hAtomRadius = vwr.getVanderwaalsMar(1) / 1000f;
      atomData.hAtoms = calculateHydrogens(atomData.bsSelected, nH, false,
          true, null);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if (atomData.modelIndex < 0)
      atomData.firstAtomIndex = (atomData.bsSelected == null ? 0 : Math.max(0,
          atomData.bsSelected.nextSetBit(0)));
    else
      atomData.firstAtomIndex = am[atomData.modelIndex].firstAtomIndex;
    atomData.lastModelIndex = atomData.firstModelIndex = (ac == 0 ? 0
        : at[atomData.firstAtomIndex].mi);
    atomData.modelName = getModelNumberDotted(atomData.firstModelIndex);
    fillADa(atomData, mode);
  }

  public String getModelNumberDotted(int modelIndex) {
    return (mc < 1 || modelIndex >= mc || modelIndex < 0 ? ""
        : Escape.escapeModelFileNumber(modelFileNumbers[modelIndex]));
  }

  public int getModelNumber(int modelIndex) {
    if (modelIndex == Integer.MAX_VALUE)
      modelIndex = mc - 1;
    return modelNumbers[modelIndex];
  }

  public int getModelFileNumber(int modelIndex) {
    return modelFileNumbers[modelIndex];
  }

  public Properties getModelProperties(int modelIndex) {
    return am[modelIndex].properties;
  }

  public String getModelProperty(int modelIndex, String property) {
    Properties props = am[modelIndex].properties;
    return props == null ? null : props.getProperty(property);
  }

  public Map<String, Object> getModelAuxiliaryInfo(int modelIndex) {
    return (modelIndex < 0 ? null : am[modelIndex].auxiliaryInfo);
  }

  public void setInfo(int modelIndex, Object key, Object value) {
    am[modelIndex].auxiliaryInfo.put((String) key, value);
  }

  public Object getInfo(int modelIndex, String key) {
    if (modelIndex < 0) {
      return null;
    }
    return am[modelIndex].auxiliaryInfo.get(key);
  }

  protected boolean getInfoB(int modelIndex, String keyName) {
    Map<String, Object> info = am[modelIndex].auxiliaryInfo;
    return (info != null && info.containsKey(keyName) && ((Boolean) info
        .get(keyName)).booleanValue());
  }

  protected int getInfoI(int modelIndex, String keyName) {
    Map<String, Object> info = am[modelIndex].auxiliaryInfo;
    if (info != null && info.containsKey(keyName)) {
      return ((Integer) info.get(keyName)).intValue();
    }
    return Integer.MIN_VALUE;
  }

  public String getAtomProp(Atom atom, String text) {
    Object data = getInfo(atom.mi, text);
    if (!(data instanceof Object[]))
      return "";
    Object[] sdata = (Object[]) data;
    int iatom = atom.i - am[atom.mi].firstAtomIndex;
    return (iatom < sdata.length ? sdata[iatom].toString() : "");
  }

  public int getInsertionCountInModel(int modelIndex) {
    return am[modelIndex].nInsertions;
  }

  public static int modelFileNumberFromFloat(float fDotM) {
    //only used in the case of select model = someVariable
    //2.1 and 2.10 will be ambiguous and reduce to 2.1  

    int file = (int) Math.floor(fDotM);
    int model = (int) Math.floor((fDotM - file + 0.00001) * 10000);
    while (model != 0 && model % 10 == 0)
      model /= 10;
    return file * 1000000 + model;
  }

  public int getAltLocCountInModel(int modelIndex) {
    return am[modelIndex].nAltLocs;
  }

  public int getChainCount(boolean addWater) {
    int chainCount = 0;
    for (int i = mc; --i >= 0;)
      chainCount += am[i].getChainCount(addWater);
    return chainCount;
  }

  public int getBioPolymerCount() {
    int polymerCount = 0;
    for (int i = mc; --i >= 0;)
      if (!isTrajectorySubFrame(i))
        polymerCount += am[i].getBioPolymerCount();
    return polymerCount;
  }

  public int getBioPolymerCountInModel(int modelIndex) {
    return (modelIndex < 0 ? getBioPolymerCount()
        : isTrajectorySubFrame(modelIndex) ? 0 : am[modelIndex]
            .getBioPolymerCount());
  }

  public void getPolymerPointsAndVectors(BS bs, Lst<P3[]> vList,
                                         boolean isTraceAlpha,
                                         float sheetSmoothing) {
    for (int i = 0; i < mc; ++i)
      am[i].getPolymerPointsAndVectors(bs, vList, isTraceAlpha,
          sheetSmoothing);
  }

  public void recalculateLeadMidpointsAndWingVectors(int modelIndex) {
    if (modelIndex < 0) {
      for (int i = 0; i < mc; i++)
        if (!isTrajectorySubFrame(i))
          am[i].recalculateLeadMidpointsAndWingVectors();
      return;
    }
    am[modelIndex].recalculateLeadMidpointsAndWingVectors();
  }

  public P3[] getPolymerLeadMidPoints(int iModel, int iPolymer) {
    return am[iModel].getPolymerLeadMidPoints(iPolymer);
  }

  public int getChainCountInModelWater(int modelIndex, boolean countWater) {
    if (modelIndex < 0)
      return getChainCount(countWater);
    return am[modelIndex].getChainCount(countWater);
  }

  public int getGroupCount() {
    int groupCount = 0;
    for (int i = mc; --i >= 0;)
      groupCount += am[i].getGroupCount();
    return groupCount;
  }

  public int getGroupCountInModel(int modelIndex) {
    if (modelIndex < 0)
      return getGroupCount();
    return am[modelIndex].getGroupCount();
  }

  public void calcSelectedGroupsCount() {
    BS bsSelected = vwr.bsA();
    for (int i = mc; --i >= 0;)
      am[i].calcSelectedGroupsCount(bsSelected);
  }

  public void calcSelectedMonomersCount() {
    BS bsSelected = vwr.bsA();
    for (int i = mc; --i >= 0;)
      am[i].calcSelectedMonomersCount(bsSelected);
  }

  /**
   * These are not actual hydrogen bonds. They are N-O bonds in proteins and
   * nucleic acids The method is called by AminoPolymer and NucleicPolymer
   * methods, which are indirectly called by ModelCollection.autoHbond
   * 
   * @param bsA
   * @param bsB
   * @param vHBonds
   *        vector of bonds to fill; if null, creates the HBonds
   * @param nucleicOnly
   * @param nMax
   * @param dsspIgnoreHydrogens
   * @param bsHBonds
   */

  public void calcRasmolHydrogenBonds(BS bsA, BS bsB, Lst<Bond> vHBonds,
                                      boolean nucleicOnly, int nMax,
                                      boolean dsspIgnoreHydrogens, BS bsHBonds) {
    boolean isSame = (bsB == null || bsA.equals(bsB));
    for (int i = mc; --i >= 0;)
      if (am[i].isBioModel && am[i].trajectoryBaseIndex == i) {
        if (vHBonds == null) {
          am[i].clearRasmolHydrogenBonds(bsA);
          if (!isSame)
            am[i].clearRasmolHydrogenBonds(bsB);
        }
        am[i].getRasmolHydrogenBonds(bsA, bsB, vHBonds, nucleicOnly, nMax,
            dsspIgnoreHydrogens, bsHBonds);
      }
  }

  public void calculateStraightness() {
    if (getHaveStraightness())
      return;
    char ctype = 'S';//(vwr.getTestFlag3() ? 's' : 'S');
    char qtype = vwr.getQuaternionFrame();
    int mStep = vwr.getInt(T.helixstep);
    // testflag3 ON  --> preliminary: Hanson's original normal-based straightness
    // testflag3 OFF --> final: Kohler's new quaternion-based straightness
    for (int i = mc; --i >= 0;)
      am[i].calculateStraightness(vwr, ctype, qtype, mStep);
    setHaveStraightness(true);
  }

  public Quat[] getAtomGroupQuaternions(BS bsAtoms, int nMax, char qtype) {
    // run through list, getting quaternions. For simple groups, 
    // go ahead and take first three atoms
    // for PDB files, do not include NON-protein groups.
    int n = 0;
    Lst<Quat> v = new Lst<Quat>();
    for (int i = bsAtoms.nextSetBit(0); i >= 0 && n < nMax; i = bsAtoms
        .nextSetBit(i + 1)) {
      Group g = at[i].group;
      Quat q = g.getQuaternion(qtype);
      if (q == null) {
        if (g.seqcode == Integer.MIN_VALUE)
          q = g.getQuaternionFrame(at); // non-PDB just use first three atoms
        if (q == null)
          continue;
      }
      n++;
      v.addLast(q);
      i = g.lastAtomIndex;
    }
    return v.toArray(new Quat[v.size()]);
  }

  public boolean isJmolDataFrameForModel(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc && am[modelIndex].isJmolDataFrame);
  }

  private boolean isJmolDataFrameForAtom(Atom atom) {
    return (am[atom.mi].isJmolDataFrame);
  }

  public void setJmolDataFrame(String type, int modelIndex, int modelDataIndex) {
    Model model = am[type == null ? am[modelDataIndex].dataSourceFrame
        : modelIndex];
    if (type == null) {
      //leaving a data frame -- just set generic to this one if quaternion
      type = am[modelDataIndex].jmolFrameType;
    }
    if (modelIndex >= 0) {
      if (model.dataFrames == null) {
        model.dataFrames = new Hashtable<String, Integer>();
      }
      am[modelDataIndex].dataSourceFrame = modelIndex;
      am[modelDataIndex].jmolFrameType = type;
      model.dataFrames.put(type, Integer.valueOf(modelDataIndex));
    }
    if (type.startsWith("quaternion") && type.indexOf("deriv") < 0) { //generic quaternion
      type = type.substring(0, type.indexOf(" "));
      model.dataFrames.put(type, Integer.valueOf(modelDataIndex));
    }
  }

  public int getJmolDataFrameIndex(int modelIndex, String type) {
    if (am[modelIndex].dataFrames == null) {
      return -1;
    }
    Integer index = am[modelIndex].dataFrames.get(type);
    return (index == null ? -1 : index.intValue());
  }

  protected void clearDataFrameReference(int modelIndex) {
    for (int i = 0; i < mc; i++) {
      Map<String, Integer> df = am[i].dataFrames;
      if (df == null) {
        continue;
      }
      Iterator<Integer> e = df.values().iterator();
      while (e.hasNext()) {
        if ((e.next()).intValue() == modelIndex) {
          e.remove();
        }
      }
    }
  }

  public String getJmolFrameType(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc ? am[modelIndex].jmolFrameType
        : "modelSet");
  }

  public int getJmolDataSourceFrame(int modelIndex) {
    return (modelIndex >= 0 && modelIndex < mc ? am[modelIndex].dataSourceFrame
        : -1);
  }

  public void saveModelOrientation(int modelIndex, Orientation orientation) {
    am[modelIndex].orientation = orientation;
  }

  public Orientation getModelOrientation(int modelIndex) {
    return am[modelIndex].orientation;
  }

  /*
   final static String[] pdbRecords = { "ATOM  ", "HELIX ", "SHEET ", "TURN  ",
   "MODEL ", "SCALE",  "HETATM", "SEQRES",
   "DBREF ", };
   */

  public String getPDBHeader(int modelIndex) {
    return (am[modelIndex].isBioModel ? am[modelIndex]
        .getFullPDBHeader() : getFileHeader(modelIndex));
  }

  public String getFileHeader(int modelIndex) {
    if (modelIndex < 0)
      return "";
    if (am[modelIndex].isBioModel)
      return am[modelIndex].getFullPDBHeader();
    String info = (String) getInfo(modelIndex, "fileHeader");
    if (info == null)
      info = modelSetName;
    if (info != null)
      return info;
    return "no header information found";
  }

  //////////////  individual models ////////////////

  public int getAltLocIndexInModel(int modelIndex, char alternateLocationID) {
    if (alternateLocationID == '\0') {
      return 0;
    }
    String altLocList = getAltLocListInModel(modelIndex);
    if (altLocList.length() == 0) {
      return 0;
    }
    return altLocList.indexOf(alternateLocationID) + 1;
  }

  public int getInsertionCodeIndexInModel(int modelIndex, char insertionCode) {
    if (insertionCode == '\0')
      return 0;
    String codeList = getInsertionListInModel(modelIndex);
    if (codeList.length() == 0)
      return 0;
    return codeList.indexOf(insertionCode) + 1;
  }

  public String getAltLocListInModel(int modelIndex) {
    if (modelIndex < 0)
      return "";
    String str = (String) getInfo(modelIndex, "altLocs");
    return (str == null ? "" : str);
  }

  private String getInsertionListInModel(int modelIndex) {
    String str = (String) getInfo(modelIndex,
        "insertionCodes");
    return (str == null ? "" : str);
  }

  public int getModelSymmetryCount(int modelIndex) {
    return (am[modelIndex].biosymmetryCount > 0 ? am[modelIndex].biosymmetryCount
        : unitCells == null || unitCells[modelIndex] == null ? 0
            : unitCells[modelIndex].getSpaceGroupOperationCount());
  }

  public String getSymmetryInfoString(int modelIndex, String spaceGroup,
                                      int symOp, P3 pt1, P3 pt2, String drawID,
                                      boolean labelOnly) {
    Map<String, Object> sginfo = getSymTemp(true).getSpaceGroupInfo(
        (ModelSet) this, modelIndex, spaceGroup, symOp, pt1, pt2, drawID);
    if (sginfo == null)
      return "";
    return symTemp.getSymmetryInfoString(sginfo, symOp, drawID, labelOnly);
  }

  public int[] getModelCellRange(int modelIndex) {
    if (unitCells == null)
      return null;
    return unitCells[modelIndex].getCellRange();
  }

  public int getLastVibrationVector(int modelIndex, int tok) {
    if (vibrations != null)
      for (int i = ac; --i >= 0;)
        if ((modelIndex < 0 || at[i].mi == modelIndex)
            && vibrations[i] != null
            && vibrations[i].length() > 0
            && (tok == 0 || (tok == T.modulation) == (vibrations[i] instanceof JmolModulationSet)))
          return i;
    return -1;
  }

  public Lst<Object> getModulationList(BS bs, String type, P3 t456) {
    Lst<Object> list = new Lst<Object>();
    if (vibrations != null)
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (vibrations[i] instanceof JmolModulationSet)
          list.addLast(((JmolModulationSet) vibrations[i]).getModulation(type,
              t456));
        else
          list.addLast(null);
    return list;
  }

  public BS getElementsPresentBitSet(int modelIndex) {
    if (modelIndex >= 0)
      return elementsPresent[modelIndex];
    BS bs = new BS();
    for (int i = 0; i < mc; i++)
      bs.or(elementsPresent[i]);
    return bs;
  }

  private String getSymmetryInfoAsStringForModel(int modelIndex) {
    SymmetryInterface unitCell = getUnitCell(modelIndex);
    return (unitCell == null ? "no symmetry information" : unitCell
        .getSymmetryInfoString());
  }

  ///////// molecules /////////

  public int getMoleculeIndex(int atomIndex, boolean inModel) {
    //ColorManager
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++) {
      if (molecules[i].atomList.get(atomIndex))
        return (inModel ? molecules[i].indexInModel : i);
    }
    return 0;
  }

  public BS getMoleculeBitSet(BS bs) {
    // returns cumulative sum of all atoms in molecules containing these atoms
    if (moleculeCount == 0)
      getMolecules();
    BS bsResult = BSUtil.copy(bs);
    BS bsInitial = BSUtil.copy(bs);
    int i = 0;
    BS bsTemp = new BS();
    while ((i = bsInitial.length() - 1) >= 0) {
      bsTemp = getMoleculeBitSetForAtom(i);
      if (bsTemp == null) {
        // atom has been deleted
        bsInitial.clear(i);
        bsResult.clear(i);
        continue;
      }
      bsInitial.andNot(bsTemp);
      bsResult.or(bsTemp);
    }
    return bsResult;
  }

  public BS getMoleculeBitSetForAtom(int atomIndex) {
    if (moleculeCount == 0)
      getMolecules();
    for (int i = 0; i < moleculeCount; i++)
      if (molecules[i].atomList.get(atomIndex))
        return molecules[i].atomList;
    return null;
  }

  public V3 getModelDipole(int modelIndex) {
    if (modelIndex < 0)
      return null;
    V3 dipole = (V3) getInfo(modelIndex, "dipole");
    if (dipole == null)
      dipole = (V3) getInfo(modelIndex, "DIPOLE_VEC");
    return dipole;
  }

  public V3 calculateMolecularDipole(int modelIndex) {
    if (partialCharges == null || modelIndex < 0)
      return null;
    int nPos = 0;
    int nNeg = 0;
    float cPos = 0;
    float cNeg = 0;
    V3 pos = new V3();
    V3 neg = new V3();
    for (int i = 0; i < ac; i++) {
      if (at[i].mi != modelIndex)
        continue;
      float c = partialCharges[i];
      if (c < 0) {
        nNeg++;
        cNeg += c;
        neg.scaleAdd2(c, at[i], neg);
      } else if (c > 0) {
        nPos++;
        cPos += c;
        pos.scaleAdd2(c, at[i], pos);
      }
    }
    if (nNeg == 0 || nPos == 0)
      return null;
    pos.scale(1f / cPos);
    neg.scale(1f / cNeg);
    pos.sub(neg);
    Logger
        .warn("CalculateMolecularDipole: this is an approximate result -- needs checking");
    pos.scale(cPos * 4.8f); //1e-10f * 1.6e-19f/ 3.336e-30f;

    // SUM_Q[(SUM_pos Q_iRi) / SUM_Q   -  (SUM_neg Q_iRi) / (-SUM_Q) ]    
    // this is really just SUM_i (Q_iR_i). Don't know why that would not work. 

    //http://www.chemistry.mcmaster.ca/esam/Chapter_7/section_3.html
    // 1 Debye = 3.336e-30 Coulomb-meter; C_e = 1.6022e-19 C
    return pos;
  }

  public int getMoleculeCountInModel(int modelIndex) {
    //ColorManager
    //not implemented for pop-up menu -- will slow it down.
    int n = 0;
    if (moleculeCount == 0)
      getMolecules();
    if (modelIndex < 0)
      return moleculeCount;
    for (int i = 0; i < mc; i++) {
      if (modelIndex == i)
        n += am[i].moleculeCount;
    }
    return n;
  }

  private BS selectedMolecules = new BS();

  public void calcSelectedMoleculesCount() {
    BS bsSelected = vwr.bsA();
    if (moleculeCount == 0)
      getMolecules();
    selectedMolecules.xor(selectedMolecules);
    //selectedMoleculeCount = 0;
    BS bsTemp = new BS();
    for (int i = 0; i < moleculeCount; i++) {
      BSUtil.copy2(bsSelected, bsTemp);
      bsTemp.and(molecules[i].atomList);
      if (bsTemp.length() > 0) {
        selectedMolecules.set(i);
        //selectedMoleculeCount++;
      }
    }
  }

  /**
   * deletes molecules based on: CENTROID -- molecular centroid is not in unit
   * cell CENTROID PACKED -- all molecule atoms are not in unit cell
   * 
   * @param bs
   * @param minmax
   *        fractional [xmin, ymin, zmin, xmax, ymax, zmax, 1=packed]
   */
  public void setCentroid(BS bs, int[] minmax) {
    BS bsDelete = getNotInCentroid(bs, minmax);
    if (bsDelete != null && bsDelete.nextSetBit(0) >= 0)
      vwr.deleteAtoms(bsDelete, false);
  }

  private BS getNotInCentroid(BS bs, int[] minmax) {
    int iAtom0 = bs.nextSetBit(0);
    if (iAtom0 < 0)
      return null;
    SymmetryInterface uc = getUnitCell(at[iAtom0].mi);
    return (uc == null ? null : uc.notInCentroid((ModelSet) this, bs, minmax));
  }

  public JmolMolecule[] getMolecules() {
    if (moleculeCount > 0)
      return molecules;
    if (molecules == null)
      molecules = new JmolMolecule[4];
    moleculeCount = 0;
    Model m = null;
    BS[] bsModelAtoms = new BS[mc];
    Lst<BS> biobranches = null;
    for (int i = 0; i < mc; i++) {
      // TODO: Trajectories?
      bsModelAtoms[i] = vwr.getModelUndeletedAtomsBitSet(i);
      m = am[i];
      m.moleculeCount = 0;
      biobranches = m.getBioBranches(biobranches);
    }
    // problem, as with 1gzx, is that this does not include non-protein cofactors that are 
    // covalently bonded. So we indicate a set of "biobranches" in JmolMolecule.getMolecules
    molecules = JmolMolecule.getMolecules(at, bsModelAtoms, biobranches,
        null);
    moleculeCount = molecules.length;
    for (int i = moleculeCount; --i >= 0;) {
      m = am[molecules[i].modelIndex];
      m.firstMoleculeIndex = i;
      m.moleculeCount++;
    }
    return molecules;
  }

  //////////// iterators //////////

  //private final static boolean MIX_BSPT_ORDER = false;
  boolean showRebondTimes = true;

  protected void initializeBspf() {
    if (bspf != null && bspf.isInitialized())
      return;
    if (showRebondTimes)
      Logger.startTimer("build bspf");
    Bspf bspf = new Bspf(3);
    if (Logger.debugging)
      Logger.debug("sequential bspt order");
    BS bsNew = BS.newN(mc);
    for (int i = ac; --i >= 0;) {
      // important that we go backward here, because we are going to 
      // use System.arrayCopy to expand the array ONCE only
      Atom atom = at[i];
      if (!atom.isDeleted() && !isTrajectorySubFrame(atom.mi)) {
        bspf.addTuple(am[atom.mi].trajectoryBaseIndex, atom);
        bsNew.set(atom.mi);
      }
    }
    //      }
    if (showRebondTimes) {
      Logger.checkTimer("build bspf", false);
      bspf.stats();
      //        bspf.dump();
    }
    for (int i = bsNew.nextSetBit(0); i >= 0; i = bsNew.nextSetBit(i + 1))
      bspf.validateModel(i, true);
    bspf.validate(true);
    this.bspf = bspf;

  }

  protected void initializeBspt(int modelIndex) {
    initializeBspf();
    if (bspf.isInitializedIndex(modelIndex))
      return;
    bspf.initialize(modelIndex, at,
        vwr.getModelUndeletedAtomsBitSet(modelIndex));
  }

  public void setIteratorForPoint(AtomIndexIterator iterator, int modelIndex,
                                  T3 pt, float distance) {
    if (modelIndex < 0) {
      iterator.setCenter(pt, distance);
      return;
    }
    initializeBspt(modelIndex);
    iterator.setModel(this, modelIndex, am[modelIndex].firstAtomIndex,
        Integer.MAX_VALUE, pt, distance, null);
  }

  public void setIteratorForAtom(AtomIndexIterator iterator, int modelIndex,
                                 int atomIndex, float distance, RadiusData rd) {
    if (modelIndex < 0)
      modelIndex = at[atomIndex].mi;
    modelIndex = am[modelIndex].trajectoryBaseIndex;
    initializeBspt(modelIndex);
    iterator.setModel(this, modelIndex, am[modelIndex].firstAtomIndex,
        atomIndex, at[atomIndex], distance, rd);
  }

  /**
   * @param bsSelected
   * @param isGreaterOnly
   * @param modelZeroBased
   * @param hemisphereOnly
   * @param isMultiModel
   * @return an iterator
   */
  public AtomIndexIterator getSelectedAtomIterator(BS bsSelected,
                                                   boolean isGreaterOnly,
                                                   boolean modelZeroBased,
                                                   boolean hemisphereOnly,
                                                   boolean isMultiModel) {
    //EnvelopeCalculation, IsoSolventReader
    // This iterator returns only atoms OTHER than the atom specified
    // and with the specified restrictions. 
    // Model zero-based means the index returned is within the model, 
    // not the full atom set. broken in 12.0.RC6; repaired in 12.0.RC15

    initializeBspf();
    AtomIteratorWithinModel iter;
    if (isMultiModel) {
      BS bsModels = getModelBS(bsSelected, false);
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1))
        initializeBspt(i);
      iter = new AtomIteratorWithinModelSet(bsModels);
    } else {
      iter = new AtomIteratorWithinModel();
    }
    iter.initialize(bspf, bsSelected, isGreaterOnly, modelZeroBased,
        hemisphereOnly, vwr.isParallel());
    return iter;
  }

  ////////// bonds /////////

  @Override
  public int getBondCountInModel(int modelIndex) {
    return (modelIndex < 0 ? bondCount : am[modelIndex].getBondCount());
  }

  ////////// struts /////////

  /**
   * see comments in org.jmol.modelsetbio.AlphaPolymer.java
   * 
   * Struts are calculated for atoms in bs1 connecting to atoms in bs2. The two
   * bitsets may overlap.
   * 
   * @param bs1
   * @param bs2
   * @return number of struts added
   */
  public int calculateStruts(BS bs1, BS bs2) {
    return calculateStrutsMC(bs1, bs2);
  }

  protected int calculateStrutsMC(BS bs1, BS bs2) {
    // select only ONE model
    makeConnections2(0, Float.MAX_VALUE, Edge.BOND_STRUT, T.delete, bs1,
        bs2, null, false, false, 0);
    int iAtom = bs1.nextSetBit(0);
    if (iAtom < 0)
      return 0;
    Model model = am[at[iAtom].mi];
    return (model.isBioModel ? model.calculateStruts((ModelSet) this, bs1, bs2)
        : 0);
  }

  public int getAtomCountInModel(int modelIndex) {
    return (modelIndex < 0 ? ac : am[modelIndex].ac);
  }

  protected BS bsAll;

  public ShapeManager sm;

  /**
   * note -- this method returns ALL atoms, including deleted.
   * 
   * @param bsModels
   * @return bitset of atoms
   */
  public BS getModelAtomBitSetIncludingDeletedBs(BS bsModels) {
    BS bs = new BS();
    if (bsModels == null && bsAll == null)
      bsAll = BSUtil.setAll(ac);
    if (bsModels == null)
      bs.or(bsAll);
    else
      for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels
          .nextSetBit(i + 1))
        bs.or(getModelAtomBitSetIncludingDeleted(i, false));
    return bs;
  }

  /**
   * Note that this method returns all atoms, included deleted ones. If you
   * don't want deleted atoms, then use
   * vwr.getModelAtomBitSetUndeleted(modelIndex, TRUE)
   * 
   * @param modelIndex
   * @param asCopy
   *        MUST BE TRUE IF THE BITSET IS GOING TO BE MODIFIED!
   * @return either the actual bitset or a copy
   */
  public BS getModelAtomBitSetIncludingDeleted(int modelIndex, boolean asCopy) {
    BS bs = (modelIndex < 0 ? bsAll : am[modelIndex].bsAtoms);
    if (bs == null)
      bs = bsAll = BSUtil.setAll(ac);
    return (asCopy ? BSUtil.copy(bs) : bs);
  }

  protected BS getAtomBitsMaybeDeleted(int tokType, Object specInfo) {
    int[] info;
    BS bs;
    switch (tokType) {
    default:
      return getAtomBitsMDa(tokType, specInfo);
    case T.dssr:
      bs = new BS();
      JmolDSSRParser p = vwr.getDSSRParser();
      Object dssr;
      for (int i = mc; --i >= 0;)
        if ((dssr = getInfo(i, "dssr")) != null)
          bs.or(p
              .getAtomBits(
                  vwr,
                  (String) specInfo,
                  dssr, (am[i].dssrCache == null ? am[i].dssrCache = new Hashtable<String, BS>()
                          : am[i].dssrCache)));
      return bs;
    case T.bonds:
    case T.isaromatic:
      return getAtomBitsMDb(tokType, specInfo);
    case T.basepair:
      return getBasePairBits((String) specInfo);
    case T.boundbox:
      BoxInfo boxInfo = getBoxInfo((BS) specInfo, 1);
      bs = getAtomsWithin(boxInfo.getBoundBoxCornerVector().length() + 0.0001f,
          boxInfo.getBoundBoxCenter(), null, -1);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        if (!boxInfo.isWithin(at[i]))
          bs.clear(i);
      return bs;
    case T.cell:
      // select cell=555 (an absolute quantity)
      // select cell=1505050
      // select cell=1500500500
      bs = new BS();
      info = (int[]) specInfo;
      ptTemp1.set(info[0] / 1000f, info[1] / 1000f, info[2] / 1000f);
      boolean isAbsolute = !vwr.getBoolean(T.fractionalrelative);
      for (int i = ac; --i >= 0;)
        if (isInLatticeCell(i, ptTemp1, ptTemp2, isAbsolute))
          bs.set(i);
      return bs;
    case T.centroid:
      // select centroid=555  -- like cell=555 but for whole molecules
      // if it  is one full molecule, then return the EMPTY bitset      
      bs = BSUtil.newBitSet2(0, ac);
      info = (int[]) specInfo;
      int[] minmax = new int[] { info[0] / 1000 - 1, info[1] / 1000 - 1,
          info[2] / 1000 - 1, info[0] / 1000, info[1] / 1000, info[2] / 1000, 0 };
      for (int i = mc; --i >= 0;) {
        SymmetryInterface uc = getUnitCell(i);
        if (uc == null) {
          BSUtil.andNot(bs, am[i].bsAtoms);
          continue;
        }
        bs.andNot(uc.notInCentroid((ModelSet) this, am[i].bsAtoms, minmax));
      }
      return bs;
    case T.molecule:
      return getMoleculeBitSet((BS) specInfo);
    case T.sequence:
      return getSequenceBits((String) specInfo, null);
    case T.spec_seqcode_range:
      info = (int[]) specInfo;
      int seqcodeA = info[0];
      int seqcodeB = info[1];
      int chainID = info[2];
      bs = new BS();
      boolean caseSensitive = vwr.getBoolean(T.chaincasesensitive);
      if (chainID >= 0 && chainID < 256 && !caseSensitive)
        chainID = chainToUpper(chainID);
      for (int i = mc; --i >= 0;)
        if (am[i].isBioModel)
          am[i].selectSeqcodeRange(seqcodeA, seqcodeB, chainID, bs,
              caseSensitive);
      return bs;
    case T.specialposition:
      bs = BS.newN(ac);
      int modelIndex = -1;
      int nOps = 0;
      for (int i = ac; --i >= 0;) {
        Atom atom = at[i];
        BS bsSym = atom.getAtomSymmetry();
        if (bsSym != null) {
          if (atom.mi != modelIndex) {
            modelIndex = atom.mi;
            if (getModelCellRange(modelIndex) == null)
              continue;
            nOps = getModelSymmetryCount(modelIndex);
          }
          // special positions are characterized by
          // multiple operator bits set in the first (overall)
          // block of nOpts bits.
          // only strictly true with load {nnn mmm 1}

          int n = 0;
          for (int j = nOps; --j >= 0;)
            if (bsSym.get(j))
              if (++n > 1) {
                bs.set(i);
                break;
              }
        }
      }
      return bs;
    case T.symmetry:
      return BSUtil.copy(bsSymmetry == null ? bsSymmetry = BS.newN(ac)
          : bsSymmetry);
    case T.unitcell:
      // select UNITCELL (a relative quantity)
      bs = new BS();
      SymmetryInterface unitcell = vwr.getCurrentUnitCell();
      if (unitcell == null)
        return bs;
      ptTemp1.set(1, 1, 1);
      for (int i = ac; --i >= 0;)
        if (isInLatticeCell(i, ptTemp1, ptTemp2, false))
          bs.set(i);
      return bs;
    }
  }

  private boolean isInLatticeCell(int i, P3 cell, P3 ptTemp, boolean isAbsolute) {
    // this is the one method that allows for an absolute fractional cell business
    // but it is always called with isAbsolute FALSE.
    // so then it is determining values for select UNITCELL and the like.

    int iModel = at[i].mi;
    SymmetryInterface uc = getUnitCell(iModel);
    ptTemp.setT(at[i]);
    return (uc != null && uc.checkUnitCell(uc, cell, ptTemp, isAbsolute));
  }

  /**
   * Get atoms within a specific distance of any atom in a specific set of atoms
   * either within all models or within just the model(s) of those atoms
   * 
   * @param distance
   * @param bs
   * @param withinAllModels
   * @param rd
   * @return the set of atoms
   */
  public BS getAtomsWithinRadius(float distance, BS bs, boolean withinAllModels,
                             RadiusData rd) {
    BS bsResult = new BS();
    BS bsCheck = getIterativeModels(false);
    bs = BSUtil.andNot(bs, vwr.getDeletedAtoms());
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, false,
        false);
    if (withinAllModels) {
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1))
        for (int iModel = mc; --iModel >= 0;) {
          if (!bsCheck.get(iModel))
            continue;
          if (distance < 0) {
            getAtomsWithin(distance, at[i].getFractionalUnitCoordPt(true),
                bsResult, -1);
            continue;
          }
          setIteratorForAtom(iter, iModel, i, distance, rd);
          iter.addAtoms(bsResult);
        }
    } else {
      bsResult.or(bs);
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        if (distance < 0) {
          getAtomsWithin(distance, at[i], bsResult, at[i].mi);
          continue;
        }
        setIteratorForAtom(iter, -1, i, distance, rd);
        iter.addAtoms(bsResult);
      }
    }
    iter.release();
    return bsResult;
  }

  public BS getGroupsWithin(int nResidues, BS bs) {
    BS bsCheck = getIterativeModels(false);
    BS bsResult = new BS();
    for (int iModel = mc; --iModel >= 0;) {
      if (!bsCheck.get(iModel) || !am[iModel].isBioModel)
        continue;
      am[iModel].getGroupsWithin(nResidues, bs, bsResult);
    }
    return bsResult;
  }

  private final P3 ptTemp1 = new P3();
  private final P3 ptTemp2 = new P3();

  public BS getAtomsWithin(float distance, P3 coord, BS bsResult, int modelIndex) {

    if (bsResult == null)
      bsResult = new BS();

    if (distance < 0) { // check all unitCell distances
      distance = -distance;
      for (int i = ac; --i >= 0;) {
        Atom atom = at[i];
        if (modelIndex >= 0 && at[i].mi != modelIndex)
          continue;
        if (!bsResult.get(i)
            && atom.getFractionalUnitDistance(coord, ptTemp1, ptTemp2) <= distance)
          bsResult.set(atom.i);
      }
      return bsResult;
    }

    BS bsCheck = getIterativeModels(true);
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, false,
        false);
    for (int iModel = mc; --iModel >= 0;) {
      if (!bsCheck.get(iModel))
        continue;
      setIteratorForAtom(iter, -1, am[iModel].firstAtomIndex, -1, null);
      iter.setCenter(coord, distance);
      iter.addAtoms(bsResult);
    }
    iter.release();
    return bsResult;
  }

  private BS getBasePairBits(String specInfo) {
    BS bs = new BS();
    if (specInfo.length() % 2 != 0)
      return bs;
    BS bsA = null;
    BS bsB = null;
    Lst<Bond> vHBonds = new Lst<Bond>();
    if (specInfo.length() == 0) {
      bsA = bsB = vwr.getAllAtoms();
      calcRasmolHydrogenBonds(bsA, bsB, vHBonds, true, 1, false, null);
    } else {
      for (int i = 0; i < specInfo.length();) {
        bsA = getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsA.cardinality() == 0)
          continue;
        bsB = getSequenceBits(specInfo.substring(i, ++i), null);
        if (bsB.cardinality() == 0)
          continue;
        calcRasmolHydrogenBonds(bsA, bsB, vHBonds, true, 1, false, null);
      }
    }
    BS bsAtoms = new BS();
    for (int i = vHBonds.size(); --i >= 0;) {
      Bond b = vHBonds.get(i);
      bsAtoms.set(b.atom1.i);
      bsAtoms.set(b.atom2.i);
    }
    return getAtomBitsMDb(T.group, bsAtoms);
  }

  public BS getSequenceBits(String specInfo, BS bs) {
    if (bs == null)
      bs = vwr.getAllAtoms();
    BS bsResult = new BS();
    if (specInfo.length() > 0)
      for (int i = 0; i < mc; ++i)
        if (am[i].isBioModel)
          am[i].getSequenceBits(specInfo, bs, bsResult);
    return bsResult;
  }

  // ////////// Bonding methods ////////////

  public void deleteBonds(BS bsBonds, boolean isFullModel) {
    if (!isFullModel) {
      BS bsA = new BS();
      BS bsB = new BS();
      for (int i = bsBonds.nextSetBit(0); i >= 0; i = bsBonds.nextSetBit(i + 1)) {
        Atom atom1 = bo[i].atom1;
        if (am[atom1.mi].isModelKit)
          continue;
        bsA.clearAll();
        bsB.clearAll();
        bsA.set(atom1.i);
        bsB.set(bo[i].getAtomIndex2());
        addStateScript("connect ", null, bsA, bsB, "delete", false, true);
      }
    }
    dBb(bsBonds, isFullModel);
  }

  protected int[] makeConnections2(float minD, float maxD, int order,
                                   int connectOperation, BS bsA, BS bsB,
                                   BS bsBonds, boolean isBonds,
                                   boolean addGroup, float energy) {
    if (bsBonds == null)
      bsBonds = new BS();
    boolean matchAny = (order == Edge.BOND_ORDER_ANY);
    boolean matchNull = (order == Edge.BOND_ORDER_NULL);
    if (matchNull)
      order = Edge.BOND_COVALENT_SINGLE; //default for setting
    boolean matchHbond = Bond.isOrderH(order);
    boolean identifyOnly = false;
    boolean idOrModifyOnly = false;
    boolean createOnly = false;
    boolean autoAromatize = false;
    switch (connectOperation) {
    case T.delete:
      return deleteConnections(minD, maxD, order, bsA, bsB, isBonds, matchNull);
    case T.legacyautobonding:
    case T.auto:
      if (order != Edge.BOND_AROMATIC)
        return autoBond(bsA, bsB, bsBonds, isBonds, matchHbond,
            connectOperation == T.legacyautobonding);
      idOrModifyOnly = autoAromatize = true;
      break;
    case T.identify:
      identifyOnly = idOrModifyOnly = true;
      break;
    case T.modify:
      idOrModifyOnly = true;
      break;
    case T.create:
      createOnly = true;
      break;
    }
    boolean anyOrNoId = (!identifyOnly || matchAny);
    boolean notAnyAndNoId = (!identifyOnly && !matchAny);
    
    defaultCovalentMad = vwr.getMadBond();
    boolean minDIsFrac = (minD < 0);
    boolean maxDIsFrac = (maxD < 0);
    boolean isFractional = (minDIsFrac || maxDIsFrac);
    boolean checkDistance = (!isBonds
        || minD != JC.DEFAULT_MIN_CONNECT_DISTANCE || maxD != JC.DEFAULT_MAX_CONNECT_DISTANCE);
    if (checkDistance) {
      minD = fixD(minD, minDIsFrac);
      maxD = fixD(maxD, maxDIsFrac);
    }
    short mad = getDefaultMadFromOrder(order);
    int nNew = 0;
    int nModified = 0;
    Bond bondAB = null;
    Atom atomA = null;
    Atom atomB = null;
    char altloc = '\0';
    short newOrder = (short) (order | Edge.BOND_NEW);
    try {
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        if (isBonds) {
          bondAB = bo[i];
          atomA = bondAB.atom1;
          atomB = bondAB.atom2;
        } else {
          atomA = at[i];
          if (atomA.isDeleted())
            continue;
          altloc = (isModulated(i) ? '\0' : atomA.altloc);
        }
        for (int j = (isBonds ? 0 : bsB.nextSetBit(0)); j >= 0; j = bsB
            .nextSetBit(j + 1)) {
          if (isBonds) {
            j = 2147483646; // Integer.MAX_VALUE - 1; one pass only
          } else {
            if (j == i)
              continue;
            atomB = at[j];
            if (atomA.mi != atomB.mi || atomB.isDeleted())
              continue;
            if (altloc != '\0' && altloc != atomB.altloc
                && atomB.altloc != '\0')
              continue;
            bondAB = atomA.getBond(atomB);
          }
          if ((bondAB == null ? idOrModifyOnly : createOnly)
              || checkDistance
              && !isInRange(atomA, atomB, minD, maxD, minDIsFrac, maxDIsFrac,
                  isFractional))
            continue;
          if (bondAB == null) {
            bsBonds.set(bondAtoms(atomA, atomB, order, mad, bsBonds, energy,
                addGroup, true).index);
            nNew++;
          } else {
            if (notAnyAndNoId) {
              bondAB.setOrder(order);
              bsAromatic.clear(bondAB.index);
            }
            if (anyOrNoId || order == bondAB.order
                || newOrder == bondAB.order || matchHbond
                && bondAB.isHydrogen()) {
              bsBonds.set(bondAB.index);
              nModified++;
            }
          }
        }
      }
    } catch (Exception e) {
      // well, we tried -- probably ran over
    }
    if (autoAromatize)
      assignAromaticBondsBs(true, bsBonds);
    if (!identifyOnly)
      sm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE, null,
          bsBonds);
    return new int[] { nNew, nModified };
  }

  public int autoBondBs4(BS bsA, BS bsB, BS bsExclude, BS bsBonds, short mad,
                         boolean preJmol11_9_24) {
    // unfortunately, 11.9.24 changed the order with which atoms were processed
    // for autobonding. This means that state script prior to that that use
    // select BOND will be misread by later version.
    if (preJmol11_9_24)
      return autoBond_Pre_11_9_24(bsA, bsB, bsExclude, bsBonds, mad);
    if (ac == 0)
      return 0;
    if (mad == 0)
      mad = 1;
    if (maxBondingRadius == PT.FLOAT_MIN_SAFE)
      findMaxRadii();
    float bondTolerance = vwr.getFloat(T.bondtolerance);
    float minBondDistance = vwr.getFloat(T.minbonddistance);
    float minBondDistance2 = minBondDistance * minBondDistance;
    int nNew = 0;
    if (showRebondTimes)// && Logger.debugging)
      Logger.startTimer("autobond");
    int lastModelIndex = -1;
    boolean isAll = (bsA == null);
    BS bsCheck;
    int i0;
    if (isAll) {
      i0 = 0;
      bsCheck = null;
    } else {
      if (bsA.equals(bsB)) {
        bsCheck = bsA;
      } else {
        bsCheck = BSUtil.copy(bsA);
        bsCheck.or(bsB);
      }
      i0 = bsCheck.nextSetBit(0);
    }
    AtomIndexIterator iter = getSelectedAtomIterator(null, false, false, true,
        false);
    boolean useOccupation = false;
    for (int i = i0; i >= 0 && i < ac; i = (isAll ? i + 1 : bsCheck
        .nextSetBit(i + 1))) {
      boolean isAtomInSetA = (isAll || bsA.get(i));
      boolean isAtomInSetB = (isAll || bsB.get(i));
      Atom atom = at[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.mi;
      // no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrameForModel(modelIndex)) {
          i = am[modelIndex].firstAtomIndex + am[modelIndex].ac
              - 1;
          continue;
        }
        useOccupation = getInfoB(modelIndex, "autoBondUsingOccupation"); // JANA reader
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadius();
      if (myBondingRadius == 0)
        continue;
      boolean isFirstExcluded = (bsExclude != null && bsExclude.get(i));
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      setIteratorForAtom(iter, -1, i, searchRadius, null);
      while (iter.hasNext()) {
        Atom atomNear = at[iter.next()];
        if (atomNear.isDeleted())
          continue;
        int j = atomNear.i;
        boolean isNearInSetA = (isAll || bsA.get(j));
        boolean isNearInSetB = (isAll || bsB.get(j));
        // BOTH must be excluded in order to ignore bonding
        if (!isNearInSetA && !isNearInSetB
            || !(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA)
            || isFirstExcluded && bsExclude.get(j)
            || useOccupation && occupancies != null && (occupancies[i] < 50) != (occupancies[j] < 50))
          continue;
        short order = getBondOrderFull(myBondingRadius,
            atomNear.getBondingRadius(), iter.foundDistance2(),
            minBondDistance2, bondTolerance);
        if (order > 0
            && checkValencesAndBond(atom, atomNear, order, mad, bsBonds))
          nNew++;
      }
      iter.release();
    }
    if (showRebondTimes)
      Logger.checkTimer("autoBond", false);
    return nNew;
  }

  private int autoBond_Pre_11_9_24(BS bsA, BS bsB, BS bsExclude, BS bsBonds,
                                   short mad) {
    if (ac == 0)
      return 0;
    if (mad == 0)
      mad = 1;
    // null values for bitsets means "all"
    if (maxBondingRadius == PT.FLOAT_MIN_SAFE)
      findMaxRadii();
    float bondTolerance = vwr.getFloat(T.bondtolerance);
    float minBondDistance = vwr.getFloat(T.minbonddistance);
    float minBondDistance2 = minBondDistance * minBondDistance;
    int nNew = 0;
    initializeBspf();
    /*
     * miguel 2006 04 02
     * note that the way that these loops + iterators are constructed,
     * everything assumes that all possible pairs of atoms are going to
     * be looked at.
     * for example, the hemisphere iterator will only look at atom indexes
     * that are >= (or <= ?) the specified atom.
     * if we are going to allow arbitrary sets bsA and bsB, then this will
     * not work.
     * so, for now I will do it the ugly way.
     * maybe enhance/improve in the future.
     */
    int lastModelIndex = -1;
    for (int i = ac; --i >= 0;) {
      boolean isAtomInSetA = (bsA == null || bsA.get(i));
      boolean isAtomInSetB = (bsB == null || bsB.get(i));
      if (!isAtomInSetA && !isAtomInSetB)
        //|| bsExclude != null && bsExclude.get(i))
        continue;
      Atom atom = at[i];
      if (atom.isDeleted())
        continue;
      int modelIndex = atom.mi;
      //no connections allowed in a data frame
      if (modelIndex != lastModelIndex) {
        lastModelIndex = modelIndex;
        if (isJmolDataFrameForModel(modelIndex)) {
          for (; --i >= 0;)
            if (at[i].mi != modelIndex)
              break;
          i++;
          continue;
        }
      }
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadius();
      if (myBondingRadius == 0)
        continue;
      float searchRadius = myBondingRadius + maxBondingRadius + bondTolerance;
      initializeBspt(modelIndex);
      CubeIterator iter = bspf.getCubeIterator(modelIndex);
      iter.initialize(atom, searchRadius, true);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom) iter.nextElement();
        if (atomNear == atom || atomNear.isDeleted())
          continue;
        int atomIndexNear = atomNear.i;
        boolean isNearInSetA = (bsA == null || bsA.get(atomIndexNear));
        boolean isNearInSetB = (bsB == null || bsB.get(atomIndexNear));
        if (!isNearInSetA && !isNearInSetB || bsExclude != null
            && bsExclude.get(atomIndexNear) && bsExclude.get(i) //this line forces BOTH to be excluded in order to ignore bonding
        )
          continue;
        if (!(isAtomInSetA && isNearInSetB || isAtomInSetB && isNearInSetA))
          continue;
        short order = getBondOrderFull(myBondingRadius,
            atomNear.getBondingRadius(), iter.foundDistance2(),
            minBondDistance2, bondTolerance);
        if (order > 0) {
          if (checkValencesAndBond(atom, atomNear, order, mad, bsBonds))
            nNew++;
        }
      }
      iter.release();
    }
    return nNew;
  }

  private int[] autoBond(BS bsA, BS bsB, BS bsBonds, boolean isBonds,
                         boolean matchHbond, boolean legacyAutoBond) {
    if (isBonds) {
      BS bs = bsA;
      bsA = new BS();
      bsB = new BS();
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
        bsA.set(bo[i].atom1.i);
        bsB.set(bo[i].atom2.i);
      }
    }
    return new int[] {
        matchHbond ? autoHbond(bsA, bsB, false) : autoBondBs4(bsA, bsB, null,
            bsBonds, vwr.getMadBond(), legacyAutoBond), 0 };
  }

  private static float hbondMin = 2.5f;

  /**
   * a generalized formation of HBONDS, carried out in relation to calculate
   * HBONDS {atomsFrom} {atomsTo}. The calculation can create pseudo-H bonds for
   * files that do not contain H atoms.
   * 
   * @param bsA
   *        "from" set (must contain H if that is desired)
   * @param bsB
   *        "to" set
   * @param onlyIfHaveCalculated
   * @return negative number of pseudo-hbonds or number of actual hbonds formed
   */
  public int autoHbond(BS bsA, BS bsB, boolean onlyIfHaveCalculated) {
    if (onlyIfHaveCalculated) {
      BS bsModels = getModelBS(bsA, false);
      for (int i = bsModels.nextSetBit(0); i >= 0 && onlyIfHaveCalculated; i = bsModels
          .nextSetBit(i + 1))
        onlyIfHaveCalculated = !am[i].hasRasmolHBonds;
      if (onlyIfHaveCalculated)
        return 0;
    }
    boolean haveHAtoms = false;
    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1))
      if (at[i].getElementNumber() == 1) {
        haveHAtoms = true;
        break;
      }
    BS bsHBonds = new BS();
    boolean useRasMol = vwr.getBoolean(T.hbondsrasmol);
    if (bsB == null || useRasMol && !haveHAtoms) {
      Logger.info((bsB == null ? "DSSP/DSSR " : "RasMol")
          + " pseudo-hbond calculation");
      calcRasmolHydrogenBonds(bsA, bsB, null, false, Integer.MAX_VALUE, false,
          bsHBonds);
      return -BSUtil.cardinalityOf(bsHBonds);
    }
    Logger.info(haveHAtoms ? "Standard Hbond calculation"
        : "Jmol pseudo-hbond calculation");
    BS bsCO = null;
    if (!haveHAtoms) {
      bsCO = new BS();
      for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
        int atomID = at[i].atomID;
        switch (atomID) {
        case JC.ATOMID_TERMINATING_OXT:
        case JC.ATOMID_CARBONYL_OXYGEN:
        case JC.ATOMID_CARBONYL_OD1:
        case JC.ATOMID_CARBONYL_OD2:
        case JC.ATOMID_CARBONYL_OE1:
        case JC.ATOMID_CARBONYL_OE2:
          bsCO.set(i);
          break;
        }
      }
    }
    float maxXYDistance = vwr.getFloat(T.hbondsdistancemaximum);
    float minAttachedAngle = (float) (vwr.getFloat(T.hbondsangleminimum)
        * Math.PI / 180);
    float hbondMax2 = maxXYDistance * maxXYDistance;
    float hbondMin2 = hbondMin * hbondMin;
    float hxbondMin2 = 1;
    float hxbondMax2 = (maxXYDistance > hbondMin ? hbondMin2 : hbondMax2);
    float hxbondMax = (maxXYDistance > hbondMin ? hbondMin : maxXYDistance);
    int nNew = 0;
    float d2 = 0;
    V3 v1 = new V3();
    V3 v2 = new V3();
    if (showRebondTimes && Logger.debugging)
      Logger.startTimer("hbond");
    P3 C = null;
    P3 D = null;
    AtomIndexIterator iter = getSelectedAtomIterator(bsB, false, false, false,
        false);

    for (int i = bsA.nextSetBit(0); i >= 0; i = bsA.nextSetBit(i + 1)) {
      Atom atom = at[i];
      int elementNumber = atom.getElementNumber();
      boolean isH = (elementNumber == 1);
      if (!isH && (haveHAtoms || elementNumber != 7 && elementNumber != 8)
          || isH && !haveHAtoms)
        continue;
      float min2, max2, dmax;
      boolean firstIsCO;
      if (isH) {
        Bond[] b = atom.bonds;
        if (b == null)
          continue;
        boolean isOK = false;
        for (int j = 0; j < b.length && !isOK; j++) {
          Atom a2 = b[j].getOtherAtom(atom);
          int element = a2.getElementNumber();
          isOK = (element == 7 || element == 8);
        }
        if (!isOK)
          continue;
        dmax = hxbondMax;
        min2 = hxbondMin2;
        max2 = hxbondMax2;
        firstIsCO = false;
      } else {
        dmax = maxXYDistance;
        min2 = hbondMin2;
        max2 = hbondMax2;
        firstIsCO = bsCO.get(i);
      }
      setIteratorForAtom(iter, -1, atom.i, dmax, null);
      while (iter.hasNext()) {
        Atom atomNear = at[iter.next()];
        int elementNumberNear = atomNear.getElementNumber();
        if (atomNear == atom || !isH && elementNumberNear != 7
            && elementNumberNear != 8 || isH && elementNumberNear == 1
            || (d2 = iter.foundDistance2()) < min2 || d2 > max2 || firstIsCO
            && bsCO.get(atomNear.i) || atom.isBonded(atomNear)) {
          continue;
        }
        if (minAttachedAngle > 0) {
          v1.sub2(atom, atomNear);
          if ((D = checkMinAttachedAngle(atom, minAttachedAngle, v1, v2,
              haveHAtoms)) == null)
            continue;
          v1.scale(-1);
          if ((C = checkMinAttachedAngle(atomNear, minAttachedAngle, v1, v2,
              haveHAtoms)) == null)
            continue;
        }
        float energy = 0;
        short bo;
        if (isH && !Float.isNaN(C.x) && !Float.isNaN(D.x)) {
          /*
           * A crude calculation based on simple distances. In the NH -- O=C
           * case this reads DH -- A=C
           * 
           * (+) H .......... A (-) | | | | (-) D C (+)
           * 
           * 
           * E = Q/rAH - Q/rAD + Q/rCD - Q/rCH
           */

          bo = Edge.BOND_H_CALC;
          energy = HBond.getEnergy((float) Math.sqrt(d2), C.distance(atom),
              C.distance(D), atomNear.distance(D)) / 1000f;
        } else {
          bo = Edge.BOND_H_REGULAR;
        }
        bsHBonds.set(addHBond(atom, atomNear, bo, energy));
        nNew++;
      }
    }
    iter.release();
    sm.setShapeSizeBs(JC.SHAPE_STICKS, Integer.MIN_VALUE, null,
        bsHBonds);
    if (showRebondTimes)
      Logger.checkTimer("hbond", false);
    return (haveHAtoms ? nNew : -nNew);
  }

  private static P3 checkMinAttachedAngle(Atom atom1, float minAngle, V3 v1,
                                          V3 v2, boolean haveHAtoms) {
    Bond[] bonds = atom1.bonds;
    if (bonds == null || bonds.length == 0)
      return P3.new3(Float.NaN, 0, 0);
    Atom X = null;
    float dMin = Float.MAX_VALUE;
    for (int i = bonds.length; --i >= 0;)
      if (bonds[i].isCovalent()) {
        Atom atomA = bonds[i].getOtherAtom(atom1);
        if (!haveHAtoms && atomA.getElementNumber() == 1)
          continue;
        v2.sub2(atom1, atomA);
        float d = v2.angle(v1);
        if (d < minAngle)
          return null;
        if (d < dMin) {
          X = atomA;
          dMin = d;
        }
      }
    return X;
  }

  //////////// state definition ///////////

  public boolean proteinStructureTainted = false;

  void setStructureIndexes() {
    int id;
    int idnew = 0;
    int lastid = -1;
    int imodel = -1;
    int lastmodel = -1;
    for (int i = 0; i < ac; i++) {
      if ((imodel = at[i].mi) != lastmodel) {
        idnew = 0;
        lastmodel = imodel;
        lastid = -1;
      }
      if ((id = at[i].getStrucNo()) != lastid && id != 0) {
        at[i].getGroup().setStrucNo(++idnew);
        lastid = idnew;
      }
    }
  }

  public String getProteinStructureState(BS bsAtoms, boolean taintedOnly,
                                         boolean needPhiPsi, int mode) {
    if (!isPDB)
      return "";
    for (int i = 0; i < mc; i++)
      if (am[i].isBioModel)
        return am[i].getProteinStructureState(bsAtoms, taintedOnly,
            needPhiPsi, mode);
    return "";
  }

  public String getModelInfoAsString() {
    SB sb = new SB().append("<models count=\"");
    sb.appendI(mc).append("\" modelSetHasVibrationVectors=\"")
        .append(modelSetHasVibrationVectors() + "\">\n<properties>");
    if (modelSetProperties != null) {
      Enumeration<?> e = modelSetProperties.propertyNames();
      while (e.hasMoreElements()) {
        String propertyName = (String) e.nextElement();
        sb.append("\n <property name=\"").append(propertyName)
            .append("\" value=")
            .append(PT.esc(modelSetProperties.getProperty(propertyName)))
            .append(" />");
      }
      sb.append("\n</properties>");
    }
    for (int i = 0; i < mc; ++i) {
      sb.append("\n<model index=\"").appendI(i).append("\" n=\"")
          .append(getModelNumberDotted(i)).append("\" id=")
          .append(PT.esc("" + getInfo(i, "modelID")));
      int ib = vwr.getJDXBaseModelIndex(i);
      if (ib != i)
        sb.append(" baseModelId=").append(
            PT.esc((String) getInfo(ib, "jdxModelID")));
      sb.append(" name=").append(PT.esc(getModelName(i))).append(" title=")
          .append(PT.esc(getModelTitle(i)))
          .append(" hasVibrationVectors=\"")
          .appendB(vwr.modelHasVibrationVectors(i)).append("\" />");
    }
    sb.append("\n</models>");
    return sb.toString();
  }

  public String getSymmetryInfoAsString() {
    SB sb = new SB().append("Symmetry Information:");
    for (int i = 0; i < mc; ++i)
      sb.append("\nmodel #").append(getModelNumberDotted(i)).append("; name=")
          .append(getModelName(i)).append("\n")
          .append(getSymmetryInfoAsStringForModel(i));
    return sb.toString();
  }

  public BS getAtomsConnected(float min, float max, int intType, BS bs) {
    boolean isBonds = bs instanceof BondSet;
    BS bsResult = (isBonds ? new BondSet() : new BS());
    int[] nBonded = new int[ac];
    int i;
    boolean ishbond = (intType == Edge.BOND_HYDROGEN_MASK);
    boolean isall = (intType == Edge.BOND_ORDER_ANY);
    for (int ibond = 0; ibond < bondCount; ibond++) {
      Bond bond = bo[ibond];
      if (isall || bond.is(intType) || ishbond && bond.isHydrogen()) {
        if (isBonds) {
          bsResult.set(ibond);
        } else {
        if (bs.get(bond.atom1.i)) {
          nBonded[i = bond.atom2.i]++;
          bsResult.set(i);
        }
        if (bs.get(bond.atom2.i)) {
          nBonded[i = bond.atom1.i]++;
          bsResult.set(i);
        }
        }
      }
    }
    if (isBonds)
      return bsResult;
    boolean nonbonded = (min == 0);
    for (i = ac; --i >= 0;) {
      int n = nBonded[i];
      if (n < min || n > max)
        bsResult.clear(i);
      else if (nonbonded && n == 0)
        bsResult.set(i);
    }
    return bsResult;
  }

  private SymmetryInterface symTemp;

  public Hashtable<String, BS> htPeaks;

  public SymmetryInterface getSymTemp(boolean forceNew) {
    return (symTemp == null || forceNew ? (symTemp = Interface.getSymmetry()) : symTemp);
  }

  public void createModels(int n) {
    int newModelCount = mc + n;
    Model[] newModels = (Model[]) AU.arrayCopyObject(am, newModelCount);
    validateBspf(false);
    modelNumbers = AU.arrayCopyI(modelNumbers, newModelCount);
    modelFileNumbers = AU.arrayCopyI(modelFileNumbers, newModelCount);
    modelNumbersForAtomLabel = AU.arrayCopyS(modelNumbersForAtomLabel,
        newModelCount);
    modelNames = AU.arrayCopyS(modelNames, newModelCount);
    frameTitles = AU.arrayCopyS(frameTitles, newModelCount);
    int f = getModelFileNumber(mc - 1) / 1000000 + 1;
    for (int i = mc, pt = 0; i < newModelCount; i++) {
      modelNumbers[i] = i + mc;
      modelFileNumbers[i] = f * 1000000 + (++pt);
      modelNumbersForAtomLabel[i] = modelNames[i] = f + "." + pt;
    }
    thisStateModel = -1;
    String[] group3Lists = (String[]) getInfoM("group3Lists");
    if (group3Lists != null) {
      int[][] group3Counts = (int[][]) getInfoM("group3Counts");
      group3Lists = AU.arrayCopyS(group3Lists, newModelCount);
      group3Counts = AU.arrayCopyII(group3Counts, newModelCount);
      msInfo.put("group3Lists", group3Lists);
      msInfo.put("group3Counts", group3Counts);
    }
    unitCells = (SymmetryInterface[]) AU.arrayCopyObject(unitCells,
        newModelCount);
    for (int i = mc; i < newModelCount; i++) {
      newModels[i] = new Model((ModelSet) this, i, -1, null, null, null);
      newModels[i].loadState = " model create #" + i + ";";
    }
    am = newModels;
    mc = newModelCount;
  }

  protected void deleteModel(int modelIndex, int firstAtomIndex, int nAtoms,
                             BS bsAtoms, BS bsBonds) {
    /*
     *   ModelCollection.modelSetAuxiliaryInfo["group3Lists", "group3Counts, "models"]
     * ModelCollection.stateScripts ?????
     */
    if (modelIndex < 0) {
      //final deletions
      validateBspf(false);
      bsAll = null;
      resetMolecules();
      isBbcageDefault = false;
      calcBoundBoxDimensions(null, 1);
      return;
    }

    modelNumbers = (int[]) AU.deleteElements(modelNumbers, modelIndex, 1);
    modelFileNumbers = (int[]) AU.deleteElements(modelFileNumbers, modelIndex,
        1);
    modelNumbersForAtomLabel = (String[]) AU.deleteElements(
        modelNumbersForAtomLabel, modelIndex, 1);
    modelNames = (String[]) AU.deleteElements(modelNames, modelIndex, 1);
    frameTitles = (String[]) AU.deleteElements(frameTitles, modelIndex, 1);
    thisStateModel = -1;
    String[] group3Lists = (String[]) getInfoM("group3Lists");
    int[][] group3Counts = (int[][]) getInfoM("group3Counts");
    int ptm = modelIndex + 1;
    if (group3Lists != null && group3Lists[ptm] != null) {
      for (int i = group3Lists[ptm].length() / 6; --i >= 0;)
        if (group3Counts[ptm][i] > 0) {
          group3Counts[0][i] -= group3Counts[ptm][i];
          if (group3Counts[0][i] == 0)
            group3Lists[0] = group3Lists[0].substring(0, i * 6) + ",["
                + group3Lists[0].substring(i * 6 + 2);
        }
    }
    if (group3Lists != null) {
      msInfo.put("group3Lists",
          AU.deleteElements(group3Lists, modelIndex, 1));
      msInfo.put("group3Counts",
          AU.deleteElements(group3Counts, modelIndex, 1));
    }

    //fix cellInfos array
    if (unitCells != null) {
      unitCells = (SymmetryInterface[]) AU.deleteElements(unitCells,
          modelIndex, 1);
    }

    // correct stateScripts, particularly CONNECT scripts
    for (int i = stateScripts.size(); --i >= 0;) {
      if (!stateScripts.get(i).deleteAtoms(modelIndex, bsBonds, bsAtoms)) {
        stateScripts.remove(i);
      }
    }

    // set to recreate bounding box
    deleteModelAtoms(firstAtomIndex, nAtoms, bsAtoms);
    vwr.deleteModelAtoms(modelIndex, firstAtomIndex, nAtoms, bsAtoms);
  }

  @SuppressWarnings("unchecked")
  public String getMoInfo(int modelIndex) {
    SB sb = new SB();
    for (int m = 0; m < mc; m++) {
      if (modelIndex >= 0 && m != modelIndex) {
        continue;
      }
      Map<String, Object> moData = (Map<String, Object>) vwr
          .getModelAuxiliaryInfoValue(m, "moData");
      if (moData == null) {
        continue;
      }
      Lst<Map<String, Object>> mos = (Lst<Map<String, Object>>) (moData
          .get("mos"));
      int nOrb = (mos == null ? 0 : mos.size());
      if (nOrb == 0) {
        continue;
      }
      for (int i = nOrb; --i >= 0;) {
        Map<String, Object> mo = mos.get(i);
        String type = (String) mo.get("type");
        if (type == null) {
          type = "";
        }
        String units = (String) mo.get("energyUnits");
        if (units == null) {
          units = "";
        }
        Float occ = (Float) mo.get("occupancy");
        if (occ != null) {
          type = "occupancy " + occ.floatValue() + " " + type;
        }
        String sym = (String) mo.get("symmetry");
        if (sym != null) {
          type += sym;
        }
        String energy = "" + mo.get("energy");
        if (Float.isNaN(PT.parseFloat(energy)))
          sb.append(Txt.sprintf("model %-2s;  mo %-2i # %s\n", "sis",
              new Object[] { getModelNumberDotted(m), Integer.valueOf(i + 1),
                  type }));
        else
          sb.append(Txt.sprintf("model %-2s;  mo %-2i # energy %-8.3f %s %s\n",
              "sifss",
              new Object[] { getModelNumberDotted(m), Integer.valueOf(i + 1),
                  mo.get("energy"), units, type }));
      }
    }
    return sb.toString();
  }

  public void assignAtom(int atomIndex, String type, boolean autoBond) {

    if (type == null)
      type = "C";

    // not as simple as just defining an atom.
    // if we click on an H, and C is being defined,
    // this sprouts an sp3-carbon at that position.

    Atom atom = at[atomIndex];
    BS bs = new BS();
    boolean wasH = (atom.getElementNumber() == 1);
    int atomicNumber = Elements.elementNumberFromSymbol(type, true);

    // 1) change the element type or charge

    boolean isDelete = false;
    if (atomicNumber > 0) {
      setElement(atom, atomicNumber);
      vwr.shm.setShapeSizeBs(JC.SHAPE_BALLS, 0, vwr.rd,
          BSUtil.newAndSetBit(atomIndex));
      setAtomName(atomIndex, type + atom.getAtomNumber());
      if (!am[atom.mi].isModelKit)
        taintAtom(atomIndex, TAINT_ATOMNAME);
    } else if (type.equals("Pl")) {
      atom.setFormalCharge(atom.getFormalCharge() + 1);
    } else if (type.equals("Mi")) {
      atom.setFormalCharge(atom.getFormalCharge() - 1);
    } else if (type.equals("X")) {
      isDelete = true;
    } else if (!type.equals(".")) {
      return; // uninterpretable
    }

    // 2) delete noncovalent bonds and attached hydrogens for that atom.

    removeUnnecessaryBonds(atom, isDelete);

    // 3) adjust distance from previous atom.

    float dx = 0;
    if (atom.getCovalentBondCount() == 1)
      if (wasH) {
        dx = 1.50f;
      } else if (!wasH && atomicNumber == 1) {
        dx = 1.0f;
      }
    if (dx != 0) {
      V3 v = V3.newVsub(atom, at[atom.getBondedAtomIndex(0)]);
      float d = v.length();
      v.normalize();
      v.scale(dx - d);
      setAtomCoordRelative(atomIndex, v.x, v.y, v.z);
    }

    BS bsA = BSUtil.newAndSetBit(atomIndex);

    if (atomicNumber != 1 && autoBond) {

      // 4) clear out all atoms within 1.0 angstrom
      validateBspf(false);
      bs = getAtomsWithinRadius(1.0f, bsA, false, null);
      bs.andNot(bsA);
      if (bs.nextSetBit(0) >= 0)
        vwr.deleteAtoms(bs, false);

      // 5) attach nearby non-hydrogen atoms (rings)

      bs = vwr.getModelUndeletedAtomsBitSet(atom.mi);
      bs.andNot(getAtomBitsMDa(T.hydrogen, null));
      makeConnections2(0.1f, 1.8f, 1, T.create, bsA, bs, null, false, false, 0);

      // 6) add hydrogen atoms

    }
    vwr.addHydrogens(bsA, false, true);
  }

  public void deleteAtoms(BS bs) {
    averageAtomPoint = null;
    if (bs == null)
      return;
    BS bsBonds = new BS();
    for (int i = bs.nextSetBit(0); i >= 0 && i < ac; i = bs
        .nextSetBit(i + 1))
      at[i].deleteBonds(bsBonds);
    for (int i = 0; i < mc; i++) {
      am[i].bsAtomsDeleted.or(bs);
      am[i].bsAtomsDeleted.and(am[i].bsAtoms);
      am[i].dssrCache = null;
    }
    deleteBonds(bsBonds, false);
  }

  // atom addition //

  void adjustAtomArrays(int[] map, int i0, int ac) {
    // from ModelLoader, after hydrogen atom addition
    this.ac = ac;
    for (int i = i0; i < ac; i++) {
      at[i] = at[map[i]];
      at[i].i = i;
      Model m = am[at[i].mi];
      if (m.firstAtomIndex == map[i])
        m.firstAtomIndex = i;
      m.bsAtoms.set(i);
    }
    if (vibrations != null)
      for (int i = i0; i < ac; i++)
        vibrations[i] = vibrations[map[i]];
    if (occupancies != null)
      for (int i = i0; i < ac; i++)
        occupancies[i] = occupancies[map[i]];
    if (bfactor100s != null)
      for (int i = i0; i < ac; i++)
        bfactor100s[i] = bfactor100s[map[i]];
    if (partialCharges != null)
      for (int i = i0; i < ac; i++)
        partialCharges[i] = partialCharges[map[i]];
    if (atomTensorList != null) {
      for (int i = i0; i < ac; i++) {
        Object[] list = atomTensorList[i] = atomTensorList[map[i]];
        for (int j = list.length; --j >= 0;) {
          Tensor t = (Tensor) list[j];
          if (t != null)
            t.atomIndex1 = map[t.atomIndex1];
        }
      }
    }
    if (atomNames != null)
      for (int i = i0; i < ac; i++)
        atomNames[i] = atomNames[map[i]];
    if (atomTypes != null)
      for (int i = i0; i < ac; i++)
        atomTypes[i] = atomTypes[map[i]];
    if (atomSerials != null)
      for (int i = i0; i < ac; i++)
        atomSerials[i] = atomSerials[map[i]];
  }

  protected void growAtomArrays(int newLength) {
    at = (Atom[]) AU.arrayCopyObject(at, newLength);
    if (vibrations != null)
      vibrations = (Vibration[]) AU.arrayCopyObject(vibrations, newLength);
    if (occupancies != null)
      occupancies = AU.arrayCopyF(occupancies, newLength);
    if (bfactor100s != null)
      bfactor100s = AU.arrayCopyShort(bfactor100s, newLength);
    if (partialCharges != null)
      partialCharges = AU.arrayCopyF(partialCharges, newLength);
    if (atomTensorList != null)
      atomTensorList = (Object[][]) AU.arrayCopyObject(atomTensorList,
          newLength);
    if (atomNames != null)
      atomNames = AU.arrayCopyS(atomNames, newLength);
    if (atomTypes != null)
      atomTypes = AU.arrayCopyS(atomTypes, newLength);
    if (atomSerials != null)
      atomSerials = AU.arrayCopyI(atomSerials, newLength);
  }

  public Atom addAtom(int modelIndex, Group group, int atomicAndIsotopeNumber,
                      String atomName, int atomSerial, int atomSite, P3 xyz,
                      float radius, V3 vib, int formalCharge,
                      float partialCharge, float occupancy, float bfactor,
                      Lst<Object> tensors, boolean isHetero,
                      byte specialAtomID, BS atomSymmetry) {
    Atom atom = new Atom().setAtom(modelIndex, ac, xyz, radius,
        atomSymmetry, atomSite, (short) atomicAndIsotopeNumber, formalCharge,
        isHetero);
    am[modelIndex].ac++;
    am[modelIndex].bsAtoms.set(ac);
    if (Elements.isElement(atomicAndIsotopeNumber, 1))
      am[modelIndex].hydrogenCount++;
    if (ac >= at.length)
      growAtomArrays(ac + 100); // only due to added hydrogens

    at[ac] = atom;
    setBFactor(ac, bfactor);
    setOccupancy(ac, occupancy);
    setPartialCharge(ac, partialCharge);
    if (tensors != null)
      setAtomTensors(ac, tensors);
    atom.group = group;
    atom.colixAtom = vwr.getColixAtomPalette(atom, PAL.CPK.id);
    if (atomName != null) {
      int i;
      if ((i = atomName.indexOf('\0')) >= 0) {
        if (atomTypes == null)
          atomTypes = new String[at.length];
        atomTypes[ac] = atomName.substring(i + 1);
        atomName = atomName.substring(0, i);
      }
      atom.atomID = specialAtomID;
      if (specialAtomID == 0) {
        if (atomNames == null)
          atomNames = new String[at.length];
        atomNames[ac] = atomName.intern();
      }
    }
    if (atomSerial != Integer.MIN_VALUE) {
      if (atomSerials == null)
        atomSerials = new int[at.length];
      atomSerials[ac] = atomSerial;
    }
    if (vib != null)
      setVibrationVector(ac, vib);
    ac++;
    return atom;
  }

  public String getInlineData(int modelIndex) {
    SB data = null;
    if (modelIndex >= 0)
      data = am[modelIndex].loadScript;
    else
      for (modelIndex = mc; --modelIndex >= 0;)
        if ((data = am[modelIndex].loadScript).length() > 0)
          break;
    int pt = data.lastIndexOf("data \"");
    if (pt < 0)
      return null;
    pt = data.indexOf2("\"", pt + 7);
    int pt2 = data.lastIndexOf("end \"");
    if (pt2 < pt || pt < 0)
      return null;
    return data.substring2(pt + 2, pt2);
  }

  public boolean isAtomPDB(int i) {
    return i >= 0 && am[at[i].mi].isBioModel;
  }

  public boolean isAtomAssignable(int i) {
    return i >= 0 && at[i].mi == mc - 1;
  }

  public boolean haveModelKit() {
    for (int i = 0; i < mc; i++)
      if (am[i].isModelKit)
        return true;
    return false;
  }

  public BS getModelKitStateBitset(BS bs, BS bsDeleted) {
    // task here is to remove bits from bs that are deleted atoms in 
    // models that are model kits.

    BS bs1 = BSUtil.copy(bsDeleted);
    for (int i = 0; i < mc; i++)
      if (!am[i].isModelKit)
        bs1.andNot(am[i].bsAtoms);
    return BSUtil.deleteBits(bs, bs1);
  }

  /**
   * 
   * @param iFirst
   *        0 from ModelLoader.freeze; -1 from Viewer.assignAtom
   * @param baseAtomIndex
   * @param mergeSet
   */
  public void setAtomNamesAndNumbers(int iFirst, int baseAtomIndex,
                                     AtomCollection mergeSet) {
    // first, validate that all atomSerials are NaN
    if (baseAtomIndex < 0)
      iFirst = am[at[iFirst].mi].firstAtomIndex;
    if (atomSerials == null)
      atomSerials = new int[ac];
    if (atomNames == null)
      atomNames = new String[ac];
    // now, we'll assign 1-based atom numbers within each model
    boolean isZeroBased = isXYZ && vwr.getBoolean(T.zerobasedxyzrasmol);
    int lastModelIndex = Integer.MAX_VALUE;
    int atomNo = 1;
    for (int i = iFirst; i < ac; ++i) {
      Atom atom = at[i];
      if (atom.mi != lastModelIndex) {
        lastModelIndex = atom.mi;
        atomNo = (isZeroBased ? 0 : 1);
      }
      // 1) do not change numbers assigned by adapter
      // 2) do not change the number already assigned when merging
      // 3) restart numbering with new atoms, not a continuation of old
      if (i >= -baseAtomIndex) {
        if (atomSerials[i] == 0 || baseAtomIndex < 0)
          atomSerials[i] = (i < baseAtomIndex ? mergeSet.atomSerials[i]
              : atomNo);
        if (atomNames[i] == null || baseAtomIndex < 0)
          atomNames[i] = (atom.getElementSymbol() + atomSerials[i]).intern();
      }

      if (!am[lastModelIndex].isModelKit || atom.getElementNumber() > 0
          && !atom.isDeleted())
        atomNo++;
    }
  }

  public void setUnitCellOffset(SymmetryInterface unitCell, P3 pt, int ijk) {
    //    for (int i = modelIndex; i < modelCount; i++) {
    //      if (i < 0 || modelIndex >= 0 && i != modelIndex
    //          && models[i].trajectoryBaseIndex != modelIndex)
    //        continue;
    //      unitCell = getUnitCell(i);
    if (unitCell == null)
      return;
    if (pt == null)
      unitCell.setOffset(ijk);
    else
      unitCell.setOffsetPt(pt);
    //    }
  }

  public void connect(float[][] connections) {
    // array of [index1 index2 order diameter energy]
    resetMolecules();
    BS bsDelete = new BS();
    for (int i = 0; i < connections.length; i++) {
      float[] f = connections[i];
      if (f == null || f.length < 2)
        continue;
      int index1 = (int) f[0];
      boolean addGroup = (index1 < 0);
      if (addGroup)
        index1 = -1 - index1;
      int index2 = (int) f[1];
      if (index2 < 0 || index1 >= ac || index2 >= ac)
        continue;
      int order = (f.length > 2 ? (int) f[2] : Edge.BOND_COVALENT_SINGLE);
      if (order < 0)
        order &= 0xFFFF; // 12.0.1 was saving struts as negative numbers
      short mad = (f.length > 3 ? (short) (1000f * connections[i][3])
          : getDefaultMadFromOrder(order));
      if (order == 0 || mad == 0 && order != Edge.BOND_STRUT
          && !Bond.isOrderH(order)) {
        Bond b = at[index1].getBond(at[index2]);
        if (b != null)
          bsDelete.set(b.index);
        continue;
      }
      float energy = (f.length > 4 ? f[4] : 0);
      bondAtoms(at[index1], at[index2], order, mad, null, energy,
          addGroup, true);
    }
    if (bsDelete.nextSetBit(0) >= 0)
      deleteBonds(bsDelete, false);
  }

  public boolean allowSpecAtom() {
    // old Chime scripts use *.C for _C
    return mc != 1 || am[0].isBioModel;
  }

  public void setFrameDelayMs(long millis, BS bsModels) {
    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      am[am[i].trajectoryBaseIndex].frameDelay = millis;
  }

  public long getFrameDelayMs(int i) {
    return (i < am.length && i >= 0 ? am[am[i].trajectoryBaseIndex].frameDelay
        : 0);
  }

  public int getModelIndexFromId(String id) {
    boolean haveFile = (id.indexOf("#") >= 0);
    boolean isBaseModel = id.toLowerCase().endsWith(".basemodel");
    if (isBaseModel)
      id = id.substring(0, id.length() - 10);
    int errCode = -1;
    String fname = null;
    for (int i = 0; i < mc; i++) {
      String mid = (String) getInfo(i, "modelID");
      String mnum = (id.startsWith("~") ? "~" + getModelNumberDotted(i) : null);
      if (mnum == null && mid == null && (mid = getModelTitle(i)) == null)
        continue;
      if (haveFile) {
        fname = getModelFileName(i);
        if (fname.endsWith("#molfile")) {
          mid = fname;
        } else {
          fname += "#";
          mid = fname + mid;
        }
      }
      if (id.equalsIgnoreCase(mid) || id.equalsIgnoreCase(mnum))
        return (isBaseModel ? vwr.getJDXBaseModelIndex(i) : i);
      if (fname != null && id.startsWith(fname))
        errCode = -2;
    }
    return (fname == null && !haveFile ? -2 : errCode);
  }

  public Map<String, Object> getAuxiliaryInfo(BS bsModels) {
    Map<String, Object> info = msInfo;
    if (info == null)
      return null;
    Lst<Map<String, Object>> models = new Lst<Map<String, Object>>();
    for (int i = 0; i < mc; ++i) {
      if (bsModels != null && !bsModels.get(i)) {
        continue;
      }
      Map<String, Object> modelinfo = getModelAuxiliaryInfo(i);
      models.addLast(modelinfo);
    }
    info.put("models", models);
    return info;
  }

  public int[][] getDihedralMap(int[] alist) {
    Lst<int[]> list = new Lst<int[]>();
    int n = alist.length;
    Atom ai = null, aj = null, ak = null, al = null;
    for (int i = n - 1; --i >= 0;)
      for (int j = n; --j > i;) {
        ai = at[alist[i]];
        aj = at[alist[j]];
        if (ai.isBonded(aj)) {
          for (int k = n; --k >= 0;)
            if (k != i && k != j && (ak = at[alist[k]]).isBonded(ai))
              for (int l = n; --l >= 0;)
                if (l != i && l != j && l != k
                    && (al = at[alist[l]]).isBonded(aj)) {
                  int[] a = new int[4];
                  a[0] = ak.i;
                  a[1] = ai.i;
                  a[2] = aj.i;
                  a[3] = al.i;
                  list.addLast(a);
                }
        }
      }
    n = list.size();
    int[][] ilist = AU.newInt2(n);
    for (int i = n; --i >= 0;)
      ilist[n - i - 1] = list.get(i);
    return ilist;
  }

  /**
   * Sets the modulation for all atoms in bs.
   * 
   * @param bs
   * @param isOn
   * @param qtOffset
   *        multiples of q or just t.
   * @param isQ
   *        true if multiples of q.
   * 
   * 
   */
  public void setModulation(BS bs, boolean isOn, P3 qtOffset, boolean isQ) {
    if (bsModulated == null) {
      if (isOn)
        bsModulated = new BS();
      else if (bs == null)
        return;
    }
    if (bs == null)
      bs = getModelAtomBitSetIncludingDeleted(-1, false);
    float scale = vwr.getFloat(T.modulation);
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      Vibration v = getVibration(i, false);
      if (!(v instanceof JmolModulationSet))
        continue;
      ((JmolModulationSet) v).setModTQ(at[i], isOn, qtOffset, isQ, scale);
      if (bsModulated != null)
        bsModulated.setBitTo(i, isOn);
    }
  }

  public Point3fi getDynamicAtom(int i, Point3fi pt) {
    Vibration v = getVibration(i, false);
    if (v == null)
      return at[i];
    if (pt == null)
      pt = new Point3fi();
    pt.setT(at[i]);
    pt = vwr.tm.getVibrationPoint(v, pt, Float.NaN);
    pt.sD = -1;
    return pt;
  }

  private Quat[] vOrientations;

  public String getBoundBoxOrientation(int type, BS bsAtoms) {
    int j0 = bsAtoms.nextSetBit(0);
    if (j0 < 0)
      return "{0 0 0 1}";
    int n = (vOrientations == null ? 0 : vOrientations.length);
    if (n == 0) {
      V3[] av = new V3[15 * 15 * 15];
      n = 0;
      P4 p4 = new P4();
      for (int i = -7; i <= 7; i++)
        for (int j = -7; j <= 7; j++)
          for (int k = 0; k <= 14; k++, n++)
            if ((av[n] = V3.new3(i / 7f, j / 7f, k / 14f)).length() > 1)
              --n;
      vOrientations = new Quat[n];
      for (int i = n; --i >= 0;) {
        float cos = (float) Math.sqrt(1 - av[i].lengthSquared());
        if (Float.isNaN(cos))
          cos = 0;
        p4.set4(av[i].x, av[i].y, av[i].z, cos);
        vOrientations[i] = Quat.newP4(p4);
      }
    }
    P3 pt = new P3();
    float vMin = Float.MAX_VALUE;
    Quat q, qBest = null;
    BoxInfo bBest = null;
    float v;
    for (int i = 0; i < n; i++) {
      q = vOrientations[i];
      BoxInfo b = new BoxInfo();
      b.setMargin(0);
      for (int j = j0; j >= 0; j = bsAtoms.nextSetBit(j + 1))
        b.addBoundBoxPoint(q.transformP2(at[j], pt));
      switch (type) {
      default:
      case T.volume:
      case T.best:
        v = (b.bbCorner1.x - b.bbCorner0.x) * (b.bbCorner1.y - b.bbCorner0.y)
            * (b.bbCorner1.z - b.bbCorner0.z);
        break;
      case T.x:
        v = b.bbCorner1.x - b.bbCorner0.x;
        break;
      case T.y:
        v = b.bbCorner1.y - b.bbCorner0.y;
        break;
      case T.z:
        v = b.bbCorner1.z - b.bbCorner0.z;
        break;
      }
      if (v < vMin) {
        qBest = q;
        bBest = b;
        vMin = v;
      }
    }
    if (type != T.volume && type != T.best)
      return qBest.toString();
    // we want dz < dy < dx
    q = Quat.newQ(qBest);
    float dx = bBest.bbCorner1.x - bBest.bbCorner0.x;
    float dy = bBest.bbCorner1.y - bBest.bbCorner0.y;
    float dz = bBest.bbCorner1.z - bBest.bbCorner0.z;
    if (dx < dy) {
      pt.set(0, 0, 1);
      q = Quat.newVA(pt, 90).mulQ(q);
      float f = dx;
      dx = dy;
      dy = f;
    }
    if (dy < dz) {
      if (dz > dx) {
        // is dy < dx < dz
        pt.set(0, 1, 0);
        q = Quat.newVA(pt, 90).mulQ(q);
        float f = dx;
        dx = dz;
        dz = f;
      }
      // is dy < dz < dx
      pt.set(1, 0, 0);
      q = Quat.newVA(pt, 90).mulQ(q);
      float f = dy;
      dy = dz;
      dz = f;
    }
    return (type == T.volume ? vMin + "\t{" + dx + " " + dy + " " + dz + "}"
        : q.getTheta() == 0 ? "{0 0 0 1}" : q.toString());
  }

  private Triangulator triangulator;

  public Lst<Object> intersectPlane(P4 plane, Lst<Object> v, int i) {
    return (triangulator == null ? (triangulator = (Triangulator) Interface
        .getUtil("TriangleData")) : triangulator).intersectPlane(plane, v, i);
  }

  public SymmetryInterface getUnitCellForAtom(int index) {
    if (index < 0 || index > ac)
      return null;
    if (bsModulated != null) {
      Vibration v = getVibration(index, false);
      if (v != null)
        return v.getUnitCell();
    }
    return getUnitCell(at[index].mi);
  }

}
