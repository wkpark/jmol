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
import org.jmol.util.Point3fi;
import org.jmol.util.TextFormat;
import org.jmol.util.XmlUtil;
import org.jmol.viewer.JmolConstants;
import org.jmol.script.Token;
import org.jmol.api.Interface;
import org.jmol.api.SymmetryInterface;
import org.jmol.atomdata.AtomData;
import org.jmol.atomdata.RadiusData;
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
    if (shapes != null)
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

  protected Shape[] shapes;
  
  
  private Shape allocateShape(int shapeID) {
    if (shapeID == JmolConstants.SHAPE_HSTICKS || shapeID == JmolConstants.SHAPE_SSSTICKS
        || shapeID == JmolConstants.SHAPE_STRUTS)
      return null;
    String className = JmolConstants.getShapeClassName(shapeID);
    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape) shapeClass.newInstance();
      viewer.setShapeErrorState(shapeID, "allocate");
      shape.initializeShape(viewer, g3d, this, shapeID);
      viewer.setShapeErrorState(-1, null);
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
  
  float getAtomShapeValue(int atomIndex, int tok) {
    int iShape = JmolConstants.shapeTokenIndex(tok);
    if (iShape < 0 || shapes[iShape] == null) 
      return 0;
    int mad = shapes[iShape].getSize(atomIndex);
    if (mad == 0) {
      Group group = atoms[atomIndex].getGroup();
      if ((group.shapeVisibilityFlags & shapes[iShape].myVisibilityFlag) == 0)
        return 0;
      mad = shapes[iShape].getSize(group);
    }
    return mad / 2000f;
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
    if (trajectorySteps == null)
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
    if (trajectorySteps == null)
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
    Point3f[] trajectory = (Point3f[]) trajectorySteps.get(modelIndex);
    BitSet bs = new BitSet();
    int iMax = iFirst + getAtomCountInModel(baseModel);
    for (int pt = 0, i = iFirst; i < iMax && pt < trajectory.length && trajectory[pt]!= null; i++) {
      atoms[i].setFractionalCoord(trajectory[pt++]);
      atoms[i].modelIndex = (short) modelIndex;
      bs.set(i);
    }
    // Clear the Binary Search so that select within(),
    // isosurface, and dots will work properly
    initializeBspf();
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

  public Point3f[] getFrameOffsets(BitSet bsAtoms) {
    if (bsAtoms == null)
      return null;
    Point3f[] offsets = new Point3f[modelCount];
    for (int i = 0; i < modelCount; i++)
      offsets[i] = new Point3f();
    int lastModel = 0;
    int n = 0;
    Point3f offset = offsets[0];
    boolean asTrajectory = (trajectorySteps != null && trajectorySteps.size() == modelCount);
    int m1 = (asTrajectory ? modelCount : 1);
    for (int m = 0; m < m1; m++) {
      if (asTrajectory)
        setTrajectory(m);
      for (int i = 0; i <= atomCount; i++) {
        if (i == atomCount || atoms[i].modelIndex != lastModel) {
          if (n > 0) {
            offset.scale(-1.0f / n);
            if (lastModel != 0)
              offset.sub(offsets[0]);
            n = 0;
          }
          if (i == atomCount)
            break;
          lastModel = atoms[i].modelIndex;
          offset = offsets[lastModel];
        }
        if (!bsAtoms.get(i))
          continue;
        offset.add(atoms[i]);
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
  public BitSet getAtomBits(int tokType, Object specInfo) {
    switch (tokType) {
    case Token.spec_model:
      return getSpecModel(((Integer) specInfo).intValue());
    }
    return super.getAtomBits(tokType, specInfo);
  }

  public String getAtomLabel(int i) {
    return (String) getShapeProperty(JmolConstants.SHAPE_LABELS, "label", i);
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

    if (shapes != null)
      for (int i = 0; i < shapes.length && closest[0] == null; ++i)
        if (shapes[i] != null)
          shapes[i].findNearestAtomIndex(x, y, closest);
    int closestIndex = (closest[0] == null ? -1 : closest[0].index);
    closest[0] = null;
    return closestIndex;
  }

  public void setShapeSize(int shapeID, int size, RadiusData rd, BitSet bsSelected) {
    if (shapes == null)
      return;
    viewer.setShapeErrorState(shapeID, "set size");
    if (rd != null && rd.value != 0 || rd == null && size != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null) {
      shapes[shapeID].setShapeSize(size, rd, bsSelected);
    }
    viewer.setShapeErrorState(-1, null);
  }

  public Shape loadShape(int shapeID) {
    if (shapes == null)
      return null;
    if (shapes[shapeID] == null)
      shapes[shapeID] = allocateShape(shapeID);
    return shapes[shapeID];
  }

  public void setShapeProperty(int shapeID, String propertyName, Object value,
                               BitSet bsSelected) {
    if (shapes == null || shapes[shapeID] == null)
      return;
    viewer.setShapeErrorState(shapeID, "set " + propertyName);
    shapes[shapeID].setShapeProperty(propertyName.intern(), value, bsSelected);
    viewer.setShapeErrorState(-1, null);
  }

  public void releaseShape(int shapeID) {
    if (shapes != null) 
      shapes[shapeID] = null;  
  }
  
  public Object getShapeProperty(int shapeID, String propertyName, int index) {
    if (shapes == null || shapes[shapeID] == null)
      return null;
    viewer.setShapeErrorState(shapeID, "get " + propertyName);
    Object result = shapes[shapeID].getProperty(propertyName, index);
    viewer.setShapeErrorState(-1, null);
    return result;
  }

  public boolean getShapeProperty(int shapeID, String propertyName, Object[] data) {
    if (shapes == null || shapes[shapeID] == null)
      return false;
    viewer.setShapeErrorState(shapeID, "get " + propertyName);
    boolean result = shapes[shapeID].getProperty(propertyName, data);
    viewer.setShapeErrorState(-1, null);
    return result;
  }

  public int getShapeIdFromObjectName(String objectName) {
    if (shapes != null)
      for (int i = JmolConstants.SHAPE_MIN_SPECIAL; i < JmolConstants.SHAPE_MAX_MESH_COLLECTION; ++i)
        if (shapes[i] != null && shapes[i].getIndexFromName(objectName) >= 0)
          return i;
    return -1;
  }

  public void setModelVisibility() {
    if (shapes == null)
      return;

    //named objects must be set individually
    //in the future, we might include here a BITSET of models rather than just a modelIndex

    // all these isTranslucent = f() || isTranslucent are that way because
    // in general f() does MORE than just check translucency. 
    // so isTranslucent = isTranslucent || f() would NOT work.

    BitSet bs = viewer.getVisibleFramesBitSet();
    
    //NOT balls (that is done later)
    for (int i = 1; i < JmolConstants.SHAPE_MAX; i++)
      if (shapes[i] != null)
        shapes[i].setVisibilityFlags(bs);
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
      atomData.hAtoms = getAdditionalHydrogens(atomData.bsSelected, nH, true);
      atomData.hydrogenAtomCount = nH[0];
      return;
    }
    if(atomData.modelIndex < 0)
      atomData.firstAtomIndex = (atomData.bsSelected == null ? 0 : Math.max(0, atomData.bsSelected.nextSetBit(0)));
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

  public boolean checkObjectHovered(int x, int y, BitSet bsVisible) {
    Shape shape = shapes[JmolConstants.SHAPE_ECHO];
    if (shape != null && shape.checkObjectHovered(x, y, bsVisible))
      return true;
    shape = shapes[JmolConstants.SHAPE_ISOSURFACE];
    if (shape != null && shape.checkObjectHovered(x, y, bsVisible))
      return true;
    shape = shapes[JmolConstants.SHAPE_DRAW];
    if (shape != null && viewer.getDrawHover() 
        && shape.checkObjectHovered(x, y, bsVisible))
      return true;
    shape = shapes[JmolConstants.SHAPE_FRANK];
    if (viewer.getShowFrank() && shape != null  
        && shape.checkObjectHovered(x, y, bsVisible))
      return true;
    return false;
  }

  public Token checkObjectClicked(int x, int y, int modifiers,
                                    BitSet bsVisible) {
    Shape shape;
    Point3fi pt = null;
    if (viewer.getNavigationMode() && viewer.getNavigateSurface() 
        && (shape = shapes[JmolConstants.SHAPE_ISOSURFACE]) != null && 
        (pt = shape.checkObjectClicked(x, y, modifiers, bsVisible)) != null)
      return new Token(Token.isosurface, pt);

    if (modifiers != 0 && viewer.getBondPicking()
        && (pt = shapes[JmolConstants.SHAPE_STICKS].checkObjectClicked(x, y,
            modifiers, bsVisible)) != null)
      return new Token(Token.bonds, pt);

    if ((shape = shapes[JmolConstants.SHAPE_ECHO])!= null && modifiers != 0
        && (pt = shape.checkObjectClicked(x, y, modifiers, bsVisible)) != null)
      return new Token(Token.echo, pt);
    if ((shape = shapes[JmolConstants.SHAPE_DRAW]) != null && 
        (pt = shape.checkObjectClicked(x, y, modifiers, bsVisible)) != null)
      return new Token(Token.draw, pt);
    return null;
  }
 
  public void checkObjectDragged(int prevX, int prevY, int x, int y,
                          int modifiers, BitSet bsVisible, int iShape) {
    for (int i = iShape; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = shapes[i];
      if (shape != null
          && shape.checkObjectDragged(prevX, prevY, x, y, modifiers, bsVisible)
          || iShape > 0)
        break;
    }
  }

  public Hashtable getShapeInfo() {
    Hashtable info = new Hashtable();
    StringBuffer commands = new StringBuffer();
    if (shapes != null)
      for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
        Shape shape = shapes[i];
        if (shape != null) {
          String shapeType = JmolConstants.shapeClassBases[i];
          Vector shapeDetail = shape.getShapeDetail();
          if (shapeDetail != null)
            info.put(shapeType, shapeDetail);
        }
      }
    if (commands.length() > 0)
      info.put("shapeCommands", commands.toString());
    return info;
  }

  public void calculateStructures(BitSet bsAtoms) {
    BitSet bsAllAtoms = new BitSet();
    BitSet bsDefined = BitSetUtil.copyInvert(modelsOf(bsAtoms, bsAllAtoms),
        modelCount);
    for (int i = 0; i < modelCount; i++)
      if (!bsDefined.get(i))
        addBioPolymerToModel(null, models[i]);
    calculatePolymers(bsDefined);
    calculateStructuresAllExcept(bsDefined, false);
    if (shapes != null)
      for (int i = 0; i < shapes.length; ++i)
        if (shapes[i] != null && shapes[i].isBioShape) {
          shapes[i].setSize(0, bsAllAtoms);
          shapes[i].setShapeProperty("color", new Byte(JmolConstants.PALETTE_CPK),
              bsAllAtoms);
        }
    setStructureIds();
  }

  public String calculatePointGroup(BitSet bsAtoms) {
    return (String) calculatePointGroupForFirstModel(bsAtoms, false,
        false, false, null, 0, 0);
  }

  public Hashtable getPointGroupInfo(BitSet bsAtoms) {
    return (Hashtable) calculatePointGroupForFirstModel(bsAtoms, false,
        false, true, null, 0, 0);
  }
  
  public String getPointGroupAsString(BitSet bsAtoms, boolean asDraw,
                                      String type, int index, float scale) {
    return (String) calculatePointGroupForFirstModel(bsAtoms, true,
        asDraw, false, type, index, scale);
  }

  private SymmetryInterface pointGroup;
  private Object calculatePointGroupForFirstModel(BitSet bsAtoms,
                                                  boolean doAll,
                                                  boolean asDraw,
                                                  boolean asInfo, String type,
                                                  int index, float scale) {
    int modelIndex = viewer.getCurrentModelIndex();
    int iAtom = (bsAtoms == null ? -1 : bsAtoms.nextSetBit(0));
    if (modelIndex < 0 && iAtom >= 0)
      modelIndex = atoms[iAtom].getModelIndex();
    if (modelIndex < 0) {
      modelIndex = viewer.getVisibleFramesBitSet().nextSetBit(0);
      bsAtoms = null;
    }
    BitSet bs = getModelAtomBitSet(modelIndex, true);
    if (bsAtoms != null)
      for (int i = 0; i < atomCount; i++)
        if (atoms[i].modelIndex == modelIndex)
          if (!bsAtoms.get(i))
            bs.clear(i);
    iAtom = bs.nextSetBit(0);
    if (iAtom < 0) {
      bs = getModelAtomBitSet(modelIndex, true);
      iAtom = bs.nextSetBit(0);
    }
    Object obj = getShapeProperty(JmolConstants.SHAPE_VECTORS, "mad", iAtom);
    boolean haveVibration = (obj != null && ((Integer) obj).intValue() != 0 || viewer
        .isVibrationOn());
    SymmetryInterface symmetry = (SymmetryInterface) Interface
        .getOptionInterface("symmetry.Symmetry");
    pointGroup = symmetry.setPointGroup(pointGroup, atoms, bs, haveVibration,
        viewer.getPointGroupTolerance(0), viewer.getPointGroupTolerance(1));
    if (!doAll && !asInfo)
      return pointGroup.getPointGroupName();
    Object ret = pointGroup.getPointGroupInfo(modelIndex, asDraw, asInfo, type,
        index, scale);
    if (asInfo)
      return ret;
    return (modelCount > 1 ? "frame " + getModelNumberDotted(modelIndex) + "; "
        : "") + ret;
  }

  private BitSet modelsOf(BitSet bsAtoms, BitSet bsAllAtoms) {
    BitSet bsModels = new BitSet(modelCount);
    boolean isAll = (bsAtoms == null);
    int i0 = (isAll ? atomCount - 1 : bsAtoms.nextSetBit(0));
    for (int i = i0; i >= 0; i = (isAll ? i - 1 : bsAtoms.nextSetBit(i + 1))) {
      int modelIndex = models[atoms[i].modelIndex].trajectoryBaseIndex;
      if (isJmolDataFrame(modelIndex))
        continue;
      bsModels.set(modelIndex);
      bsAllAtoms.set(i);
    }
    return bsModels;
  }


  
  ///// super-overloaded methods ///////
  
  
  private final static boolean useRasMolHbondsCalculation = true;

  public int autoHbond(BitSet bsA, BitSet bsB, BitSet bsBonds, 
                       float maxXYDistance, float minAttachedAngle) {
    bsPseudoHBonds = new BitSet();
    if (minAttachedAngle == 0 && useRasMolHbondsCalculation && bondCount > 0) {
      calcHydrogenBonds(bsA, bsB);
      bsBonds = bsPseudoHBonds;
      return BitSetUtil.cardinalityOf(bsBonds);
    }
    initializeBspf();
    return super.autoHbond(bsA, bsB, bsBonds, maxXYDistance, minAttachedAngle);
  }
  
  protected void assignAromaticBonds(boolean isUserCalculation) {
    super.assignAromaticBonds(isUserCalculation, null);
    // send a message to STICKS indicating that these bonds
    // should be part of the state of the model. They will 
    // appear in the state as bondOrder commands.
    
    if (isUserCalculation)
      setShapeSize(JmolConstants.SHAPE_STICKS, Integer.MIN_VALUE, null, bsAromatic);
  }

  public int[] makeConnections(float minDistance, float maxDistance, int order,
                             int connectOperation, BitSet bsA, BitSet bsB,
                             BitSet bsBonds, boolean isBonds) {
    if (connectOperation == JmolConstants.CONNECT_DELETE_BONDS 
        || connectOperation == JmolConstants.CONNECT_AUTO_BOND) {
      String stateScript = "connect ";
      if (minDistance != JmolConstants.DEFAULT_MIN_CONNECT_DISTANCE)
        stateScript += minDistance + " ";
      if (maxDistance != JmolConstants.DEFAULT_MAX_CONNECT_DISTANCE)
        stateScript += maxDistance + " ";
      addStateScript(stateScript, (isBonds? bsA : null), 
          (isBonds? null : bsA), (isBonds ? null : bsB),  
          (connectOperation == JmolConstants.CONNECT_DELETE_BONDS ? " delete" : " auto"), 
          false, true);
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
    for (int i = stateScripts.size(); --i >= 0;) 
      if (((StateScript) stateScripts.get(i)).isConnect())
        stateScripts.removeElementAt(i);
    super.deleteAllBonds();
  }

  /* ******************************************************
   * 
   * methods for definining the state 
   * 
   ********************************************************/

  public String getDefinedState(StringBuffer sfunc, boolean isAll) {
    int len = stateScripts.size();
    if (len == 0)
      return "";
    
    boolean haveDefs = false;    
    StringBuffer commands = new StringBuffer();
    String cmd;
    for (int i = 0; i < len; i++) {
      StateScript ss = (StateScript) stateScripts.get(i); 
      if (!ss.postDefinitions && (cmd = ss.toString()).length() > 0) {
        commands.append("  ").append(cmd).append("\n");
        haveDefs = true;
      }
    }

    if (!haveDefs)
      return "";
    cmd = "";
    if (isAll && sfunc != null) {
      sfunc.append("  _setDefinedState;\n");
      cmd = "function _setDefinedState() {\n\n";
    }

    if (sfunc != null)
      commands.append("\n}\n\n");
    return cmd + commands.toString();
  }
  
  public String getState(StringBuffer sfunc, boolean isAll) {
    StringBuffer commands = new StringBuffer();
    if (isAll && sfunc != null) {
      sfunc.append("  _setModelState;\n");
      commands.append("function _setModelState() {\n");
    }
    String cmd;

    // connections

    if (isAll) {

      int len = stateScripts.size();
      for (int i = 0; i < len; i++) {
        StateScript ss = (StateScript) stateScripts.get(i);
        if (ss.postDefinitions && (cmd = ss.toString()).length() > 0)
          commands.append("  ").append(cmd).append("\n");
      }

      for (int i = 0; i < bondCount; i++) {
        if ((bonds[i].order & JmolConstants.BOND_NEW) != 0
            || bonds[i].isHydrogen()) {
          Bond bond = bonds[i];
          commands.append("  connect ").append("({").append(
              bond.atom1.index).append("}) ").append("({").append(
              bond.atom2.index).append("}) ").append(
              JmolConstants.getBondOrderNameFromOrder(bond.order))
              .append(";\n");
        }
      }
      commands.append("\n");
    }

    // shape construction

    setModelVisibility();

    // unnecessary. Removed in 11.5.35 -- oops!

    if (viewer.getBooleanProperty("saveProteinStructureState"))
      commands.append(getProteinStructureState(null, true, false, false));

    if (shapes != null)
      for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
        Shape shape = shapes[i];
        if (shape != null && (isAll || JmolConstants.isShapeSecondary(i))
            && (cmd = shape.getShapeState()) != null && cmd.length() > 1)
          commands.append(cmd);
      }

    if (isAll) {
      for (int i = 0; i < modelCount; i++) {
        String t = frameTitles[i];
        if (t != null && t.length() > 0)
          commands.append("  frame " + getModelNumberDotted(i)
              + "; frame title " + Escape.escape(t) + ";\n");
        if (models[i].orientation != null)
          commands.append("  frame " + getModelNumberDotted(i) + "; "
              + models[i].orientation.getMoveToText(false) + "\n");
      }

      commands.append("  set fontScaling " + viewer.getFontScaling() + ";\n");

    }
    if (sfunc != null)
      commands.append("\n}\n\n");
    return commands.toString();
  }

  private void includeAllRelatedFrames(BitSet bsModels) {
    int j;
    for (int i = 0; i < modelCount; i++) {
      if (bsModels.get(i)) {
       // if (isJmolDataFrame(i) && !bsModels.get(j = models[i].dataSourceFrame)) {
         // bsModels.set(j);
        //  includeAllRelatedFrames(bsModels);
          //return;
       // }
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
  
  public BitSet deleteModels(BitSet bsAtoms) {
    // full models are deleted for any model containing the specified atoms
    BitSet bsModels = getModelBitSet(bsAtoms, false);
    includeAllRelatedFrames(bsModels);
    int nAtomsDeleted = 0;

    int nModelsDeleted = BitSetUtil.cardinalityOf(bsModels);
    if (nModelsDeleted == 0)
      return null;

    // clear references to this frame if it is a dataFrame

    for (int i = bsModels.nextSetBit(0); i >= 0; i = bsModels.nextSetBit(i + 1))
      clearDataFrameReference(i);

    BitSet bsDeleted;
    if (nModelsDeleted == modelCount) {
      bsDeleted = getModelAtomBitSet(-1, true);
      viewer.zap(true, false);
      return bsDeleted;
    }

    // zero out reproducible arrays

    bspf = null;

    // create a new models array,
    // and pre-calculate Model.bsAtoms and Model.atomCount
    Model[] newModels = new Model[modelCount - nModelsDeleted];
    Model[] oldModels = models;
    bsDeleted = new BitSet();
    for (int i = 0, mpt = 0; i < modelCount; i++)
      if (bsModels.get(i)) { // get a good count now
        getAtomCountInModel(i);
        bsDeleted.or(getModelAtomBitSet(i, false));
      } else {
        models[i].modelIndex = mpt;
        newModels[mpt++] = models[i];
      }
    models = newModels;
    int oldModelCount = modelCount;
    // delete bonds
    BitSet bsBonds = getBondsForSelectedAtoms(bsDeleted, true);
    deleteBonds(bsBonds, true);

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
      BitSet bs = oldModels[i].bsAtoms;
      int firstAtomIndex = oldModels[i].firstAtomIndex;

      // delete from symmetry set
      BitSetUtil.deleteBits(bsSymmetry, bs);

      // delete from stateScripts, model arrays and bitsets,
      // atom arrays, and atom bitsets
      deleteModel(mpt, firstAtomIndex, nAtoms, bs, bsBonds);

      // adjust all models after this one
      for (int j = oldModelCount; --j > i;)
        oldModels[j].fixIndices(mpt, nAtoms, bs);

      // adjust all shapes
      Object[] value = new Object[] { newModels, atoms,
          new int[] { mpt, firstAtomIndex, nAtoms } };
      if (shapes != null)
        for (int j = 0; j < JmolConstants.SHAPE_MAX; j++)
          if (shapes[j] != null)
            setShapeProperty(j, "deleteModelAtoms", value, bs);
      modelCount--;
    }

    // set final values
    deleteModel(-1, 0, 0, null, null);
    return bsDeleted;
  }

  public void setLabel(String strLabel, BitSet bsSelection) {
    if (strLabel != null) { // force the class to load and display
      loadShape(JmolConstants.SHAPE_LABELS);
      setShapeSize(JmolConstants.SHAPE_LABELS, 0, null, bsSelection);
    }
    setShapeProperty(JmolConstants.SHAPE_LABELS, "label", strLabel, bsSelection);
  }
  
  public void setAtomProperty(BitSet bs, int tok, int iValue, float fValue,
                              String sValue, float[] values, String[] list) {
    switch (tok) {
    case Token.backbone:
    case Token.cartoon:
    case Token.meshRibbon:
    case Token.ribbon:
    case Token.rocket:
    case Token.strands:
    case Token.trace:
      if (fValue > Shape.RADIUS_MAX)
        fValue = Shape.RADIUS_MAX;
    case Token.halo:
    case Token.star:
      if (values != null)
        return;
      if (fValue > Atom.RADIUS_MAX)
        fValue = Atom.RADIUS_MAX;
      if (fValue < 0)
        fValue = 0;
      setShapeSize(JmolConstants.shapeTokenIndex(tok), (int) (fValue * 2000), null, bs);
      return;
    }
    super.setAtomProperty(bs, tok, iValue, fValue, sValue, values, list);
  }
  
  public void setAtomLabel(String strLabel, int i) {
    if (shapes == null)
      return;
    loadShape(JmolConstants.SHAPE_LABELS);
    shapes[JmolConstants.SHAPE_LABELS].setProperty("label:"+strLabel, new Integer(i), null);
  }

  public Object getFileData(int modelIndex) {
    if (modelIndex < 0)
      return "";
    Hashtable fileData = (Hashtable) getModelAuxiliaryInfo(modelIndex, "fileData");
    if (fileData != null)
      return fileData;
    if (!getModelAuxiliaryInfoBoolean(modelIndex, "isCIF"))
      return getPDBHeader(modelIndex);
    fileData = viewer.getCifData(modelIndex);
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
  public int calculateStruts(BitSet bs1, BitSet bs2) {
    setModelVisibility();
    return super.calculateStruts(bs1, bs2);
  }

  /*
   * <molecule title="acetic_acid.mol"
   * xmlns="http://www.xml-cml.org/schema/cml2/core"
   * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   * xsi:schemaLocation="http://www.xml-cml.org/schema/cml2/core cmlAll.xsd">
   * <atomArray> <atom id="a1" elementType="C" x3="0.1853" y3="0.0096"
   * z3="0.4587"/> <atom id="a2" elementType="O" x3="0.6324" y3="1.0432"
   * z3="0.8951"/> <atom id="a3" elementType="C" x3="-1.0665" y3="-0.1512"
   * z3="-0.3758"/> <atom id="a4" elementType="O" x3="0.7893" y3="-1.1734"
   * z3="0.6766" formalCharge="-1"/> <atom id="a5" elementType="H" x3="-1.7704"
   * y3="-0.8676" z3="0.1055"/> <atom id="a6" elementType="H" x3="-0.8068"
   * y3="-0.5215" z3="-1.3935"/> <atom id="a7" elementType="H" x3="-1.5889"
   * y3="0.8259" z3="-0.4854"/> </atomArray> <bondArray> <bond atomRefs2="a1 a2"
   * order="partial12"/> <bond atomRefs2="a1 a3" order="S"/> <bond
   * atomRefs2="a1 a4" order="partial12"/> <bond atomRefs2="a3 a5" order="S"/>
   * <bond atomRefs2="a3 a6" order="S"/> <bond atomRefs2="a3 a7" order="S"/>
   * </bondArray> </molecule>
   */
  public String getModelCml(BitSet bs, int atomsMax, boolean addBonds) {
    StringBuffer sb = new StringBuffer("");
    int nAtoms = BitSetUtil.cardinalityOf(bs);
    if (nAtoms == 0)
      return "";
    XmlUtil.openTag(sb, "molecule");
    XmlUtil.openTag(sb, "atomArray");
    BitSet bsAtoms = new BitSet();
    for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
      if (--atomsMax < 0)
        break;
      Atom atom = atoms[i];
      String name = atom.getAtomName();
      TextFormat.simpleReplace(name, "\"", "''");
      bsAtoms.set(atom.index);
      XmlUtil
          .appendTag(sb, "atom/", new String[] { 
              "id", "a" + (atom.index + 1),
              "title", atom.getAtomName(),
              "elementType", atom.getElementSymbol(), 
              "x3", "" + atom.x, 
              "y3", "" + atom.y, 
              "z3", "" + atom.z });
    }
    XmlUtil.closeTag(sb, "atomArray");
    if (addBonds) {
      XmlUtil.openTag(sb, "bondArray");
      for (int i = 0; i < bondCount; i++) {
        Bond bond = bonds[i];
        Atom a1 = bond.atom1;
        Atom a2 = bond.atom2;
        if (!bsAtoms.get(a1.index) || !bsAtoms.get(a2.index))
          continue;
        String order = JmolConstants.getCmlOrder(bond.order);
        if (order == null)
          continue;
        XmlUtil.appendTag(sb, "bond/", new String[] { 
            "atomRefs2", "a" + (bond.atom1.index + 1) + " a" + (bond.atom2.index + 1), 
            "order", order, });
      }
      XmlUtil.closeTag(sb, "bondArray");
    }
    XmlUtil.closeTag(sb, "molecule");
    return sb.toString();
  }

}

