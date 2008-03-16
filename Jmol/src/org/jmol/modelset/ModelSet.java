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

import org.jmol.util.BitSetUtil;
import org.jmol.util.Escape;
import org.jmol.util.Logger;
import org.jmol.viewer.JmolConstants;
import org.jmol.viewer.Token;
import org.jmol.atomdata.AtomData;
import org.jmol.shape.Shape;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.vecmath.Point3f;

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
abstract public class ModelSet extends ModelCollection {

  ////////////////////////////////////////////////////////////////

  protected void releaseModelSet() {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; i++)
      shapes[i] = null;
    models = null;
    closest[0] = null;
    super.releaseModelSet();
  }

  //variables that will be reset when a new frame is instantiated

  private boolean selectionHaloEnabled = false;
  private boolean echoShapeActive = false;

  public void setSelectionHaloEnabled(boolean selectionHaloEnabled) {
    if (this.selectionHaloEnabled != selectionHaloEnabled) {
      this.selectionHaloEnabled = selectionHaloEnabled;
    }
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

  protected boolean isZeroBased;

  public void setZeroBased() {
    isZeroBased = isXYZ && viewer.getZeroBasedXyzRasmol();
  }

  protected final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];
  
  private Shape allocateShape(int shapeID) {
    if (shapeID == JmolConstants.SHAPE_HSTICKS || shapeID == JmolConstants.SHAPE_SSSTICKS)
      return null;
    String className = JmolConstants.getShapeClassName(shapeID);
    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape) shapeClass.newInstance();
      shape.initializeShape(viewer, g3d, this, shapeID);
      return shape;
    } catch (Exception e) {
      Logger.error("Could not instantiate shape:" + className, e);
    }
    return null;
  }

  public Shape getShape(int i) {
    //FrameRenderer
    return shapes[i];
  }
  
  public int getModelNumberIndex(int modelNumber, boolean useModelNumber,
                                 boolean doSetTrajectory) {
    if (useModelNumber) {
      for (int i = 0; i < modelCount; i++)
        if (modelNumbers[i] == modelNumber)
          return i;
      return -1;
    }
    //new decimal format:   frame 1.2 1.3 1.4
    for (int i = 0; i < modelCount; i++)
      if (modelFileNumbers[i] == modelNumber) {
        if (doSetTrajectory && isTrajectory(i))
          setTrajectory(i);
        return i;
      }
    return -1;
  }

  public String getTrajectoryInfo() {
    String s = "";
    if (trajectories == null)
      return "";
    for (int i = modelCount; --i >= 0; )
      if (models[i].selectedTrajectory >= 0) {
        s = " or " + getModelNumberDotted(models[i].selectedTrajectory) + s;
        i = models[i].trajectoryBaseIndex; //skip other trajectories
      }
    if (s.length() > 0)
      s = "set trajectory {" + s.substring(4) + "}"; 
    return s;
  }

  public BitSet getBitSetTrajectories() {
    if (trajectories == null)
      return null;
    BitSet bsModels = new BitSet();
    for (int i = modelCount; --i >= 0;)
      if (models[i].selectedTrajectory >= 0) {
        bsModels.set(models[i].selectedTrajectory);
        i = models[i].trajectoryBaseIndex; //skip other trajectories
      }
    return bsModels;
  }

  public void setTrajectory(BitSet bsModels) {
    for (int i = 0; i < modelCount; i++)
      if (bsModels.get(i))
        setTrajectory(i);
  }

  public void setTrajectory(int modelIndex) {
    if (modelIndex < 0 || !models[modelIndex].isTrajectory)
      return;
    // The user has used the MODEL command to switch to a new set of atom coordinates
    // Or has specified a trajectory in a select, display, or hide command.

    // Assign the coordinates and the model index for this set of atoms
    int iFirst = models[modelIndex].firstAtomIndex;
    if (atoms[iFirst].modelIndex == modelIndex)
      return;
    int baseModel = models[modelIndex].trajectoryBaseIndex;
    models[baseModel].selectedTrajectory = modelIndex;
    Point3f[] trajectory = (Point3f[]) trajectories.get(modelIndex);
    BitSet bs = new BitSet();
    int nAtoms = getAtomCountInModel(modelIndex);
    for (int pt = 0, i = iFirst; i < nAtoms && pt < trajectory.length; i++) {
      atoms[i].set(trajectory[pt++]);
      atoms[i].modelIndex = (short) modelIndex;
      bs.set(i);
    }
    // Clear the Binary Search so that select within(),
    // isosurface, and dots will work properly
    bspf.clearBspt(baseModel);
    // Recalculate critical points for cartoons and such
    // note that models[baseModel] and models[modelIndex]
    // point to the same model. So there is only one copy of 
    // the shape business.
    recalculateLeadMidpointsAndWingVectors(baseModel);
    // Recalculate all measures that involve trajectories
    Integer Imodel = new Integer(baseModel);
    for (int i = 0; i < JmolConstants.SHAPE_MAX; i++)
      if (shapes[i] != null)
      setShapeProperty(i, "refreshTrajectories", Imodel, bs);
    if (models[baseModel].hasCalculatedHBonds) {
      clearCalculatedHydrogenBonds(baseModel, null);
      models[baseModel].calcHydrogenBonds(bs, bs);
    }
    int m = viewer.getCurrentModelIndex();
    if (m >= 0 && m != modelIndex 
        && models[m].fileIndex == models[modelIndex].fileIndex)
      viewer.setCurrentModelIndex(modelIndex, false);
  }  

  /**
   * general lookup for integer type -- from Eval
   * @param tokType   
   * @param specInfo  
   * @return bitset; null only if we mess up with name
   */
  public BitSet getAtomBits(int tokType, int specInfo) {
    switch (tokType) {
    case Token.spec_model:
      return getSpecModel(specInfo);
    }
    return super.getAtomBits(tokType, specInfo);
  }

  private BitSet getSpecModel(int modelNumber) {
    int modelIndex = getModelNumberIndex(modelNumber, true, true);
    return (modelIndex < 0 && modelNumber > 0 ? new BitSet()
        : getModelAtomBitSet(modelIndex, true));
  }

  protected final Atom[] closest = new Atom[1];

  public int findNearestAtomIndex(int x, int y) {
    if (atomCount == 0)
      return -1;
    closest[0] = null;
    if (g3d.isAntialiased()) {
      x <<= 1;
      y <<= 1;
    }
    findNearestAtomIndex(x, y, closest);

    for (int i = 0; i < shapes.length && closest[0] == null; ++i)
      if (shapes[i] != null)
        shapes[i].findNearestAtomIndex(x, y, closest);
    int closestIndex = (closest[0] == null ? -1 : closest[0].atomIndex);
    closest[0] = null;
    return closestIndex;
  }

  public void setShapeSize(int shapeID, int size, BitSet bsSelected) {
    if (size != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null)
      shapes[shapeID].setSize(size, bsSelected);
  }

  public Shape loadShape(int shapeID) {
    if (shapes[shapeID] == null)
      shapes[shapeID] = allocateShape(shapeID);
    return shapes[shapeID];
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value,
                        BitSet bsSelected) {
    if (shapes[shapeID] != null)
      shapes[shapeID].setProperty(propertyName.intern(), value, bsSelected);
  }

  public Object getShapeProperty(int shapeID, String propertyName, int index) {
    return (shapes[shapeID] == null ? null 
        : shapes[shapeID].getProperty(propertyName, index));
  }

  public int getShapeIdFromObjectName(String objectName) {
    for (int i = JmolConstants.SHAPE_MIN_MESH_COLLECTION; i < JmolConstants.SHAPE_MAX_MESH_COLLECTION; ++i)
      if (shapes[i] != null && shapes[i].getIndexFromName(objectName) >= 0)
        return i;
    Shape dipoles = shapes[JmolConstants.SHAPE_DIPOLES];
    return (dipoles == null || dipoles.getIndexFromName(objectName) < 0 ? -1
        : JmolConstants.SHAPE_DIPOLES);
  }

  public void setModelVisibility() {
    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.

    BitSet bs = viewer.getVisibleFramesBitSet();
    
    //System.out.println("modelset setvis" + bs);
    //NOT balls (yet)
    for (int i = 1; i < JmolConstants.SHAPE_MAX; i++)
      if (shapes[i] != null)
        shapes[i].setVisibilityFlags(bs);
    //s(bs);
    //
    // BALLS sets the JmolConstants.ATOM_IN_MODEL flag.
    shapes[JmolConstants.SHAPE_BALLS].setVisibilityFlags(bs);

    //set clickability -- this enables measures and such
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null)
        shape.setModelClickability();
    }
  }

  /*
  private Hashtable userProperties;

  void putUserProperty(String name, Object property) {
    if (userProperties == null)
      userProperties = new Hashtable();
    if (property == null)
      userProperties.remove(name);
    else
      userProperties.put(name, property);
  }
*/  

  ////////////////// atomData filling ////////////

  public void fillAtomData(AtomData atomData, int mode) {
    if (mode == AtomData.MODE_GET_ATTACHED_HYDROGENS) {
      int[] nH = new int[1];
      atomData.hAtomRadius = viewer.getVanderwaalsMar(1) / 1000f;
      atomData.hAtoms = getAdditionalHydrogens(atomData.bsSelected, nH);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if(atomData.modelIndex < 0)
      atomData.firstAtomIndex = Math.max(0, BitSetUtil.firstSetBit(atomData.bsSelected));
    else
      atomData.firstAtomIndex = models[atomData.modelIndex].firstAtomIndex;
    atomData.lastModelIndex = atomData.firstModelIndex = (atomCount == 0 ? 0 : atoms[atomData.firstAtomIndex].modelIndex);
    atomData.modelName = getModelNumberDotted(atomData.firstModelIndex);
    super.fillAtomData(atomData, mode);
  }


  ///////// atom and shape selecting /////////
  
  
  public boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    return (frankShape != null && frankShape.wasClicked(x, y));
  }

  public boolean checkObjectHovered(int x, int y) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectHovered(x, y))
      return true;
    shape = shapes[JmolConstants.SHAPE_DRAW];
    if (shape == null || !viewer.getDrawHover())
      return false;
    return shape.checkObjectHovered(x, y);
  }

  public boolean checkObjectClicked(int x, int y, int modifiers) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectClicked(x, y, modifiers))
      return true;
    return ((shape = shapes[JmolConstants.SHAPE_DRAW]) != null
        && shape.checkObjectClicked(x, y, modifiers));
  }
 
  public void checkObjectDragged(int prevX, int prevY, int deltaX, int deltaY,
                          int modifiers) {
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null
          && shape.checkObjectDragged(prevX, prevY, deltaX, deltaY, modifiers))
        break;
    }
  }

  public Hashtable getShapeInfo() {
    Hashtable info = new Hashtable();
    StringBuffer commands = new StringBuffer();
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null) {
        String shapeType = JmolConstants.shapeClassBases[i];
        Vector shapeDetail = shape.getShapeDetail();
        if (shapeDetail != null) {
          Hashtable shapeinfo = new Hashtable();
          shapeinfo.put("obj", shapeDetail);
          info.put(shapeType, shapeinfo);
        }
      }
    }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }


  public void calculateStructures(BitSet bsAtoms) {
    BitSet bsAllAtoms = new BitSet();
    BitSet bsDefined = BitSetUtil.invertInPlace(modelsOf(bsAtoms, bsAllAtoms), modelCount);
    for (int i = 0; i < modelCount; i++)
      if (!bsDefined.get(i))
        addBioPolymerToModel(null, models[i]);
    calculatePolymers(bsDefined);
    calculateStructuresAllExcept(bsDefined, false);
    for (int i = 0; i < shapes.length; ++i)
      if (shapes[i] != null && shapes[i].isBioShape) {
        shapes[i].setSize(0, bsAllAtoms);
        shapes[i].setProperty("color", new Byte(JmolConstants.PALETTE_CPK), bsAllAtoms);
      }
  }


  private BitSet modelsOf(BitSet bsAtoms, BitSet bsAllAtoms) {
    BitSet bsModels = new BitSet(modelCount);
    for (int i = 0; i < atomCount; i++) {
      int modelIndex = models[atoms[i].modelIndex].trajectoryBaseIndex;
      if (bsAtoms != null && !bsAtoms.get(i) || isJmolDataFrame(modelIndex))
        continue;
      bsModels.set(modelIndex);
      bsAllAtoms.set(i);
    }
    return bsModels;
  }


  
  ///// super-overloaded methods ///////
  
  
  private final static boolean useRasMolHbondsCalculation = true;

  public int autoHbond(BitSet bsA, BitSet bsB, BitSet bsBonds) {
    bsPseudoHBonds = new BitSet();
    if (useRasMolHbondsCalculation && bondCount > 0) {
      calcHydrogenBonds(bsA, bsB);
      bsBonds = bsPseudoHBonds;
      return BitSetUtil.cardinalityOf(bsBonds);
    }
    initializeBspf();
    return super.autoHbond(bsA, bsB, bsBonds);
  }
  
  protected void assignAromaticBonds(boolean isUserCalculation) {
    super.assignAromaticBonds(isUserCalculation, null);
    // send a message to STICKS indicating that these bonds
    // should be part of the state of the model. They will 
    // appear in the state as bondOrder commands.
    
    if (isUserCalculation)
      setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MIN_VALUE, bsAromatic);

  }

  public int[] makeConnections(float minDistance, float maxDistance, short order,
                             int connectOperation, BitSet bsA, BitSet bsB,
                             BitSet bsBonds, boolean isBonds) {
    if (connectOperation != JmolConstants.CONNECT_IDENTIFY_ONLY) {
      String stateScript = "connect ";
      if (minDistance != JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE)
        stateScript += minDistance + " ";
      if (maxDistance != JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE)
        stateScript += maxDistance + " ";
      if (isBonds)
        stateScript += Escape.escape(bsA, false) + " ";
      else
        stateScript += Escape.escape(bsA) + " " + Escape.escape(bsB) + " ";
      if (connectOperation != JmolConstants.CONNECT_DELETE_BONDS)
        stateScript += JmolConstants.getBondOrderNameFromOrder(order) + " ";
      stateScript += JmolConstants.connectOperationName(connectOperation);
      stateScript += ";";
      stateScripts.addElement(stateScript);
    }
    return super.makeConnections(minDistance, maxDistance, order,
        connectOperation, bsA, bsB, bsBonds, isBonds);
  }
  
  public void setPdbConectBonding(int baseAtomIndex, int baseModelIndex, BitSet bsExclude) {
    short mad = viewer.getMadBond();
    for (int i = baseModelIndex; i < modelCount; i++) {
      Vector vConnect = (Vector) getModelAuxiliaryInfo(i, "PDB_CONECT_bonds");
      if (vConnect == null)
        continue;
      int nConnect = vConnect.size();
      int[] atomInfo = (int[]) getModelAuxiliaryInfo(i, "PDB_CONECT_firstAtom_count_max");
      int firstAtom = atomInfo[0] + baseAtomIndex;
      int atomMax = firstAtom + atomInfo[1];
      int max = atomInfo[2];
      int[] serialMap = new int[max + 1];
      int iSerial;
      for (int iAtom = firstAtom; iAtom < atomMax; iAtom++)
        if ((iSerial = atomSerials[iAtom]) > 0)
          serialMap[iSerial] = iAtom + 1;
      for (int iConnect = 0; iConnect < nConnect; iConnect++) {
        int[] pair = (int[]) vConnect.get(iConnect);
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
        if (atoms[sourceIndex].isHetero())
          bsExclude.set(sourceIndex);
        if (atoms[targetIndex].isHetero())
          bsExclude.set(targetIndex);
        }
        checkValencesAndBond(atoms[sourceIndex], atoms[targetIndex], order, 
            (order == JmolConstants.BOND_H_REGULAR ? 1 : mad), null);
      }
    }
  }
  
  public void deleteAllBonds() {
    //StateManager
    for (int i = 0; i < stateScripts.size();) 
      if (((String) stateScripts.get(i)).indexOf("connect") == 0)
        stateScripts.removeElementAt(i);
      else
        i++;
    super.deleteAllBonds();
  }

  /* ******************************************************
   * 
   * methods for definining the state 
   * 
   ********************************************************/
 
  public String getPropertyState() {
    BitSet bs;
    StringBuffer commands = new StringBuffer();
    for (byte i = 0; i < TAINT_MAX; i++)
      if((bs = getTaintedAtoms(i)) != null) { 
        getAtomicPropertyState(commands, atoms, atomCount, i, bs, null, null);
      }
    return commands.toString();
  }
  
  public String getState(StringBuffer sfunc, boolean isAll) {
    StringBuffer commands = new StringBuffer();
    if (isAll && sfunc != null) {
      sfunc.append("  _setModelState;\n");
      commands.append("function _setModelState();\n");
    }
    String cmd;

    // connections

    if (isAll) {
      Vector fs = stateScripts;
      int len = fs.size();
      if (len > 0) {
        commands.append("\n");
        for (int i = 0; i < len; i++)
          commands.append("  ").append(fs.get(i)).append("\n");
      }

      commands.append("\n");
      // shape construction

    }

    setModelVisibility();

    commands.append(getProteinStructureState(null, false));
    
    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null && (isAll || JmolConstants.isShapeSecondary(i)) 
          && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
        commands.append(cmd);
    }
    
    for (int i = 0; i < modelCount; i++) {
      String t = frameTitles[i]; 
      if (t != null && t.length() > 0)
        commands.append("  frame " + getModelNumberDotted(i)
            + "; frame title " + Escape.escape(t) + "\n;");
    }
    
    commands.append("  set fontScaling " + viewer.getFontScaling() + "\n;");
    
    if (sfunc != null)
      commands.append("\nend function;\n\n");
    return commands.toString();
  }

  public int getVanderwaalsMar(int i) {
    return viewer.getVanderwaalsMar(i);
  }

  void includeAllRelatedFrames(BitSet bsModels) {
    int j;
    for (int i = 0; i < modelCount; i++) {
      if (bsModels.get(i)) {
        if (isJmolDataFrame(i) && !bsModels.get(j = models[i].dataSourceFrame)) {
          bsModels.set(j);
          includeAllRelatedFrames(bsModels);
          return;
        }
        if (isTrajectory(i) && !bsModels.get(j = models[i].trajectoryBaseIndex)) {
          bsModels.set(j);
          includeAllRelatedFrames(bsModels);
          return;
        }
        continue;
      }
      if (isTrajectory(i) && bsModels.get(models[i].trajectoryBaseIndex)
          || isJmolDataFrame(i) && bsModels.get(models[i].dataSourceFrame))
        bsModels.set(i);
    }
  }
  
  public int deleteAtoms(BitSet bsAtoms, boolean fullModels) {
    
    if (!fullModels) {
      return 0;
    }
    BitSet bs = BitSetUtil.copy(bsAtoms);
    BitSet bsModels = getModelBitSet(bs);
    includeAllRelatedFrames(bsModels);
    int nAtomsDeleted = 0;
    
    int nModelsDeleted = BitSetUtil.cardinalityOf(bsModels);
    if (nModelsDeleted == 0)
      return 0;
    if (nModelsDeleted == modelCount) {
      nAtomsDeleted = atomCount;
      viewer.zap(true);
      return nAtomsDeleted;
    }
    
    // zero out reproducible arrays
    
    bspf = null;
    molecules = null;

    // create a new models array, 
    //   and pre-calculate Model.bsAtoms and Model.atomCount
    Model[] newModels = new Model[modelCount - nModelsDeleted];
    Model[] oldModels = models;
    for (int i = 0, mpt = 0; i < modelCount; i++)
      if (bsModels.get(i)) { // get a good count now
        getAtomCountInModel(i);
        bs.or(getModelAtomBitSet(i, false));
      } else {
        models[i].modelIndex = mpt;
        newModels[mpt++] = models[i];
      }
    models = newModels;
    int oldModelCount = modelCount;
    
    // delete bonds
    deleteBonds(getBondsForSelectedAtoms(bs, true));
    
    // main deletion cycle
    for (int i = 0, mpt = 0; i < oldModelCount; i++) {
      if (!bsModels.get(i)) {
        mpt++;
        continue;
      }
      int nAtoms = oldModels[i].atomCount;
      if (nAtoms == 0)
        continue;
      nAtomsDeleted += nAtoms;
      bs = oldModels[i].bsAtoms;
      int firstAtomIndex = oldModels[i].firstAtomIndex;

      // delete atom arrays and atom bitsets
      super.deleteAtoms(mpt, firstAtomIndex, nAtoms, bs);
      
      // adjust all models after this one
      for (int j = oldModelCount; --j > i; )
          oldModels[j].fixIndices(mpt, nAtoms, bs);

      // adjust all shapes
      Object[] value = new Object[] {newModels, atoms, 
          new int[] {i, firstAtomIndex, nAtoms}};      
      for (int j = 0; j < JmolConstants.SHAPE_MAX; j++)
        if (shapes[j] != null)
          setShapeProperty(j, "deleteModelAtoms", value, bs);
      modelCount--;
    }
    
    //set final values
    bsAll = null;
    calcBoundBoxDimensions(null);
    return nAtomsDeleted;

  }
  
}

