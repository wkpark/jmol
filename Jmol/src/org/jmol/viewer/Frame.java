/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import org.jmol.api.JmolAdapter;
import org.jmol.g3d.Graphics3D;
import javax.vecmath.Point3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Properties;
import java.awt.Rectangle;

final class Frame {

  final Viewer viewer;
  final JmolAdapter adapter;
  final FrameRenderer frameRenderer;
  // NOTE: these strings are interned and are lower case
  // therefore, we can do == comparisions against string constants
  // if (modelSetTypeName == "xyz")
  final String modelSetTypeName;
  final Mmset mmset;
  final Graphics3D g3d;
  // the maximum BondingRadius seen in this set of atoms
  // used in autobonding
  float maxBondingRadius = Integer.MIN_VALUE;
  float maxVanderwaalsRadius = Integer.MIN_VALUE;

  int atomCount;
  Atom[] atoms;
  ////////////////////////////////////////////////////////////////
  // these may or may not be allocated
  // depending upon the AtomSetCollection characteristics
  Object[] clientAtomReferences;
  Vector3f[] vibrationVectors;
  byte[] occupancies;
  short[] bfactor100s;
  float[] partialCharges;
  String[] atomNames;
  int[] atomSerials;
  byte[] specialAtomIDs;
  ////////////////////////////////////////////////////////////////

  int bondCount;
  Bond[] bonds;
  private final static int growthIncrement = 250;
  boolean fileCoordinatesAreFractional;
  float[] notionalUnitcell;
  Matrix3f matrixNotional;
  Matrix3f pdbScaleMatrix;
  Matrix3f pdbScaleMatrixTranspose;
  Vector3f pdbTranslateVector;
  Matrix3f matrixEuclideanToFractional;
  Matrix3f matrixFractionalToEuclidean;

  boolean hasVibrationVectors;
  boolean fileHasHbonds;

  boolean structuresDefined;

  BitSet elementsPresent;

  int groupCount;
  Group[] groups;

  BitSet groupsPresent;

  boolean hasBfactorRange;
  int bfactor100Lo;
  int bfactor100Hi;

  Frame(Viewer viewer, JmolAdapter adapter, Object clientFile) {
    this.viewer = viewer;
    this.adapter = adapter;

    //long timeBegin = System.currentTimeMillis();
    String fileTypeName = adapter.getFileTypeName(clientFile);
    // NOTE: these strings are interned and are lower case
    // therefore, we can do == comparisions against string constants
    // if (modelSetTypeName == "xyz") { }
    this.modelSetTypeName = fileTypeName.toLowerCase().intern();
    mmset = new Mmset(this);
    this.frameRenderer = viewer.getFrameRenderer();
    this.g3d = viewer.getGraphics3D();


    initializeBuild(adapter.getEstimatedAtomCount(clientFile));

    /****************************************************************
     * crystal cell must come first, in case atom coordinates
     * need to be transformed to fit in the crystal cell
     ****************************************************************/
    fileCoordinatesAreFractional =
      adapter.coordinatesAreFractional(clientFile);
    setNotionalUnitcell(adapter.getNotionalUnitcell(clientFile));
    setPdbScaleMatrix(adapter.getPdbScaleMatrix(clientFile));
    setPdbScaleTranslate(adapter.getPdbScaleTranslate(clientFile));

    setModelSetProperties(adapter.getAtomSetCollectionProperties(clientFile));

    currentModelIndex = -1;
    int modelCount = adapter.getAtomSetCount(clientFile);
    setModelCount(modelCount);
    for (int i = 0; i < modelCount; ++i) {
      int modelNumber = adapter.getAtomSetNumber(clientFile, i);
      String modelName = adapter.getAtomSetName(clientFile, i);
      if (modelName == null)
        modelName = "" + modelNumber;
      Properties modelProperties = adapter.getAtomSetProperties(clientFile, i);
      setModelNameNumberProperties(i, modelName, modelNumber,
                                         modelProperties);
    }

    for (JmolAdapter.AtomIterator iterAtom =
           adapter.getAtomIterator(clientFile);
         iterAtom.hasNext(); ) {
      byte elementNumber = (byte)iterAtom.getElementNumber();
      if (elementNumber <= 0)
        elementNumber = JmolConstants.
          elementNumberFromSymbol(iterAtom.getElementSymbol());
      addAtom(iterAtom.getAtomSetIndex(),
              iterAtom.getUniqueID(),
              elementNumber,
              iterAtom.getAtomName(),
              iterAtom.getFormalCharge(),
              iterAtom.getPartialCharge(),
              iterAtom.getOccupancy(),
              iterAtom.getBfactor(),
              iterAtom.getX(), iterAtom.getY(), iterAtom.getZ(),
              iterAtom.getIsHetero(), iterAtom.getAtomSerial(),
              iterAtom.getChainID(),
              iterAtom.getGroup3(),
              iterAtom.getSequenceNumber(), iterAtom.getInsertionCode(),
              iterAtom.getVectorX(), iterAtom.getVectorY(),
              iterAtom.getVectorZ(),
              iterAtom.getClientAtomReference());
    }

    fileHasHbonds = false;
    
    {
      JmolAdapter.BondIterator iterBond =
        adapter.getBondIterator(clientFile);
      if (iterBond != null)
        while (iterBond.hasNext())
          bondAtoms(iterBond.getAtomUniqueID1(),
                    iterBond.getAtomUniqueID2(),
                    iterBond.getEncodedOrder());
    }

    JmolAdapter.StructureIterator iterStructure =
      adapter.getStructureIterator(clientFile);
    if (iterStructure != null) 
      while (iterStructure.hasNext())
        defineStructure(iterStructure.getStructureType(),
                              iterStructure.getStartChainID(),
                              iterStructure.getStartSequenceNumber(),
                              iterStructure.getStartInsertionCode(),
                              iterStructure.getEndChainID(),
                              iterStructure.getEndSequenceNumber(),
                              iterStructure.getEndInsertionCode());
    doUnitcellStuff();
    doAutobond();
    finalizeGroupBuild();
    buildPolymers();
    freeze();
    //long msToBuild = System.currentTimeMillis() - timeBegin;
    //    System.out.println("Build a frame:" + msToBuild + " ms");
    adapter.finish(clientFile);
    finalizeBuild();
    dumpAtomSetNameDiagnostics(clientFile);
  }

  void dumpAtomSetNameDiagnostics(Object clientFile) {
    if (true)
      return;
    int frameModelCount = getModelCount();
    int adapterAtomSetCount = adapter.getAtomSetCount(clientFile);
    System.out.println("----------------\n" +
                       "debugging of AtomSetName stuff\n" +
                       "\nframeModelCount=" + frameModelCount +
                       "\nadapterAtomSetCount=" + adapterAtomSetCount +
                       "\n -- \n"
                       );
    for (int i = 0; i < adapterAtomSetCount; ++i) {
      System.out.println("atomSetName[" + i + "]=" +
                         adapter.getAtomSetName(clientFile, i) +
                         " atomSetNumber[" + i + "]=" +
                         adapter.getAtomSetNumber(clientFile, i));
                         
    }
  }

  private final static int ATOM_GROWTH_INCREMENT = 2000;

  int currentModelIndex;
  Model currentModel;
  char currentChainID;
  Chain currentChain;
  int currentGroupSequenceNumber;
  char currentGroupInsertionCode;
  
  private final Hashtable htAtomMap = new Hashtable();


  void initializeBuild(int atomCountEstimate) {
    currentModel = null;
    currentChainID = '\uFFFF';
    currentChain = null;
    currentGroupInsertionCode = '\uFFFF';

    if (atomCountEstimate <= 0)
      atomCountEstimate = ATOM_GROWTH_INCREMENT;
    atoms = new Atom[atomCountEstimate];
    bonds = new Bond[2 * atomCountEstimate];
    htAtomMap.clear();
    initializeGroupBuild();
  }

  void finalizeBuild() {
    currentModel = null;
    currentChain = null;
    htAtomMap.clear();
  }


  void addAtom(int modelIndex, Object atomUid,
               byte atomicNumber,
               String atomName, 
               int formalCharge, float partialCharge,
               int occupancy,
               float bfactor,
               float x, float y, float z,
               boolean isHetero, int atomSerial, char chainID,
               String group3,
               int groupSequenceNumber, char groupInsertionCode,
               float vectorX, float vectorY, float vectorZ,
               Object clientAtomReference) {
    if (modelIndex != currentModelIndex) {
      currentModel = mmset.getModel(modelIndex);
      currentModelIndex = modelIndex;
      currentChainID = '\uFFFF';
    }
    if (chainID != currentChainID) {
      currentChainID = chainID;
      currentChain = currentModel.getOrAllocateChain(chainID);
      currentGroupInsertionCode = '\uFFFF';
    }
    if (groupSequenceNumber != currentGroupSequenceNumber ||
        groupInsertionCode != currentGroupInsertionCode) {
      currentGroupSequenceNumber = groupSequenceNumber;
      currentGroupInsertionCode = groupInsertionCode;
      startGroup(currentChain, group3,
                 groupSequenceNumber, groupInsertionCode, atomCount);
    }
    if (atomCount == atoms.length)
      growAtomArrays();

    Atom atom = new Atom(viewer,
                         this,
                         currentModelIndex,
                         atomCount,
                         atomicNumber,
                         atomName,
                         formalCharge, partialCharge,
                         occupancy,
                         bfactor,
                         x, y, z,
                         isHetero, atomSerial, chainID,
                         vectorX, vectorY, vectorZ,
                         clientAtomReference);

    atoms[atomCount] = atom;
    ++atomCount;
    htAtomMap.put(atomUid, atom);
  }

  void bondAtoms(Object atomUid1, Object atomUid2, int order) {
    Atom atom1 = (Atom)htAtomMap.get(atomUid1);
    if (atom1 == null) {
      System.out.println("bondAtoms cannot find atomUid1?");
      return;
    }
    Atom atom2 = (Atom)htAtomMap.get(atomUid2);
    if (atom2 == null) {
      System.out.println("bondAtoms cannot find atomUid2?");
      return;
    }
    if (bondCount == bonds.length)
      bonds = (Bond[])Util.setLength(bonds,
                                     bondCount + 2 * ATOM_GROWTH_INCREMENT);
    // note that if the atoms are already bonded then
    // Atom.bondMutually(...) will return null
    Bond bond = atom1.bondMutually(atom2, order, viewer);
    if (bond != null) {
      bonds[bondCount++] = bond;
      if ((order & JmolConstants.BOND_HYDROGEN_MASK) != 0)
        fileHasHbonds = true;
    }
  }

  void growAtomArrays() {
    int newLength = atomCount + ATOM_GROWTH_INCREMENT;
    atoms = (Atom[])Util.setLength(atoms, newLength);
    if (clientAtomReferences != null)
      clientAtomReferences =
        (Object[])Util.setLength(clientAtomReferences, newLength);
    if (vibrationVectors != null)
      vibrationVectors = 
        (Vector3f[])Util.setLength(vibrationVectors, newLength);
    if (occupancies != null)
      occupancies = Util.setLength(occupancies, newLength);
    if (bfactor100s != null)
      bfactor100s = Util.setLength(bfactor100s, newLength);
    if (partialCharges != null)
      partialCharges = Util.setLength(partialCharges, newLength);
    if (atomNames != null)
      atomNames = Util.setLength(atomNames, newLength);
    if (atomSerials != null)
      atomSerials = Util.setLength(atomSerials, newLength);
    if (specialAtomIDs != null)
      specialAtomIDs = Util.setLength(specialAtomIDs, newLength);
  }

  ////////////////////////////////////////////////////////////////
  // special handling for groups
  ////////////////////////////////////////////////////////////////

  final static int defaultGroupCount = 32;
  Chain[] chains = new Chain[defaultGroupCount];
  String[] group3s = new String[defaultGroupCount];
  int[] seqcodes = new int[defaultGroupCount];
  int[] firstAtomIndexes = new int[defaultGroupCount];

  final int[] specialAtomIndexes = new int[JmolConstants.ATOMID_MAX];

  void initializeGroupBuild() {
    groupCount = 0;
  }

  void finalizeGroupBuild() {
    // run this loop in increasing order so that the
    // groups get defined going up
    groups = new Group[groupCount];
    for (int i = 0; i < groupCount; ++i) {
      distinguishAndPropogateGroup(i, chains[i], group3s[i], seqcodes[i],
                                   firstAtomIndexes[i],
                                   (i == groupCount - 1
                                    ? atomCount
                                    : firstAtomIndexes[i + 1]));
      chains[i] = null;
      group3s[i] = null;
    }
  }

  void startGroup(Chain chain, String group3,
                  int groupSequenceNumber, char groupInsertionCode,
                  int firstAtomIndex) {
    if (groupCount == group3s.length) {
      chains = (Chain[])Util.doubleLength(chains);
      group3s = Util.doubleLength(group3s);
      seqcodes = Util.doubleLength(seqcodes);
      firstAtomIndexes = Util.doubleLength(firstAtomIndexes);
    }
    firstAtomIndexes[groupCount] = firstAtomIndex;
    chains[groupCount] = chain;
    group3s[groupCount] = group3;
    seqcodes[groupCount] =
      Group.getSeqcode(groupSequenceNumber, groupInsertionCode);
    ++groupCount;
  }

  void distinguishAndPropogateGroup(int groupIndex,
                                    Chain chain, String group3, int seqcode,
                                    int firstAtomIndex, int maxAtomIndex) {
    //    System.out.println("distinguish & propogate group:" +
    //                       " group3:" + group3 +
    //                       " seqcode:" + Group.getSeqcodeString(seqcode) +
    //                       " firstAtomIndex:" + firstAtomIndex +
    //                       " maxAtomIndex:" + maxAtomIndex);
    int distinguishingBits = 0;
    // clear previous specialAtomIndexes
    for (int i = JmolConstants.ATOMID_MAX; --i >= 0; )
      specialAtomIndexes[i] = Integer.MIN_VALUE;
    
    if (specialAtomIDs != null) {
      for (int i = maxAtomIndex; --i >= firstAtomIndex; ) {
        int specialAtomID = specialAtomIDs[i];
        if (specialAtomID > 0) {
          if (specialAtomID <  JmolConstants.ATOMID_DISTINGUISHING_ATOM_MAX)
            distinguishingBits |= 1 << specialAtomID;
          specialAtomIndexes[specialAtomID] = i;
        }
      }
    }

    int lastAtomIndex = maxAtomIndex - 1;
    if (lastAtomIndex < firstAtomIndex)
      throw new NullPointerException();

    Group group = null;
    //    System.out.println("distinguishingBits=" + distinguishingBits);
    if ((distinguishingBits & JmolConstants.ATOMID_PROTEIN_MASK) ==
        JmolConstants.ATOMID_PROTEIN_MASK) {
      //      System.out.println("may be an AminoMonomer");
      group = AminoMonomer.validateAndAllocate(chain, group3, seqcode,
                                               firstAtomIndex, lastAtomIndex,
                                               specialAtomIndexes, atoms);
    } else if (distinguishingBits == JmolConstants.ATOMID_ALPHA_ONLY_MASK) {
      //      System.out.println("AlphaMonomer.validateAndAllocate");
      group = AlphaMonomer.validateAndAllocate(chain, group3, seqcode,
                                               firstAtomIndex, lastAtomIndex,
                                               specialAtomIndexes,
                                               atoms);
    } else if (((distinguishingBits & JmolConstants.ATOMID_NUCLEIC_MASK) ==
                JmolConstants.ATOMID_NUCLEIC_MASK)) {
      group = NucleicMonomer.validateAndAllocate(chain, group3, seqcode,
                                                 firstAtomIndex, lastAtomIndex,
                                                 specialAtomIndexes,
                                                 atoms);
    } else if (distinguishingBits ==
               JmolConstants.ATOMID_PHOSPHORUS_ONLY_MASK) {
      // System.out.println("PhosphorusMonomer.validateAndAllocate");
      group =
        PhosphorusMonomer.validateAndAllocate(chain, group3, seqcode,
                                              firstAtomIndex, lastAtomIndex,
                                              specialAtomIndexes, atoms);
    }
    if (group == null)
      group = new Group(chain, group3, seqcode, firstAtomIndex, lastAtomIndex);

    chain.addGroup(group);
    groups[groupIndex] = group;

    ////////////////////////////////////////////////////////////////
    for (int i = maxAtomIndex; --i >= firstAtomIndex; )
      atoms[i].setGroup(group);
  }

  ////////////////////////////////////////////////////////////////

  void buildPolymers() {
    for (int i = 0; i < groupCount; ++i) {
      Group group = groups[i];
      if (group instanceof Monomer) {
        Monomer monomer = (Monomer)group;
        if (monomer.polymer == null)
          Polymer.allocatePolymer(groups, i);
      }
    }
  }
////////////////////////////////////////////////////////////////
  FrameExportJmolAdapter exportJmolAdapter;

  JmolAdapter getExportJmolAdapter() {
    if (exportJmolAdapter == null)
      exportJmolAdapter = new FrameExportJmolAdapter(viewer, this);
    return exportJmolAdapter;
  }

  void freeze() {

    ////////////////////////////////////////////////////////////////
    // resize arrays
    if (atomCount < atoms.length) {
      atoms = (Atom[])Util.setLength(atoms, atomCount);
      if (clientAtomReferences != null)
        clientAtomReferences =
          (Object[])Util.setLength(clientAtomReferences, atomCount);
      if (vibrationVectors != null)
        vibrationVectors = 
          (Vector3f[])Util.setLength(vibrationVectors, atomCount);
      if (occupancies != null)
        occupancies = Util.setLength(occupancies, atomCount);
      if (bfactor100s != null)
        bfactor100s = Util.setLength(bfactor100s, atomCount);
      if (partialCharges != null)
        partialCharges = Util.setLength(partialCharges, atomCount);
      if (atomNames != null)
        atomNames = Util.setLength(atomNames, atomCount);
      if (atomSerials != null)
        atomSerials = Util.setLength(atomSerials, atomCount);
      if (specialAtomIDs != null)
        specialAtomIDs = Util.setLength(specialAtomIDs, atomCount);
    }
    if (bondCount < bonds.length)
      bonds = (Bond[])Util.setLength(bonds, bondCount);

    ////////////////////////////////////////////////////////////////
    // see if there are any vectors
    hasVibrationVectors = vibrationVectors != null;

    ////////////////////////////////////////////////////////////////
    //
    hackAtomSerialNumbersForAnimations();

    if (! structuresDefined)
      mmset.calculateStructures();

    ////////////////////////////////////////////////////////////////
    // find things for the popup menus
    findElementsPresent();
    findGroupsPresent();
    mmset.freeze();

    loadShape(JmolConstants.SHAPE_BALLS);
    loadShape(JmolConstants.SHAPE_STICKS);
    if (fileHasHbonds)
      loadShape(JmolConstants.SHAPE_HSTICKS);
    loadShape(JmolConstants.SHAPE_MEASURES);
  }

  void hackAtomSerialNumbersForAnimations() {
    // first, validate that all atomSerials are NaN
    if (atomSerials != null)
      return;
    // now, we'll assign 1-based atom numbers within each model
    int lastModelIndex = Integer.MAX_VALUE;
    int modelAtomIndex = 0;
    atomSerials = new int[atomCount];
    for (int i = 0; i < atomCount; ++i) {
      Atom atom = atoms[i];
      if (atom.modelIndex != lastModelIndex) {
        lastModelIndex = atom.modelIndex;
        modelAtomIndex = 1;
      }
      atomSerials[i] = modelAtomIndex++;
    }
  }

  void defineStructure(String structureType,
                              char startChainID,
                              int startSequenceNumber, char startInsertionCode,
                              char endChainID,
                              int endSequenceNumber, char endInsertionCode) {
    structuresDefined = true;
    mmset.defineStructure(structureType, startChainID,
                          startSequenceNumber, startInsertionCode,
                          endChainID, endSequenceNumber, endInsertionCode);
  }

  int getAtomIndexFromAtomNumber(int atomNumber) {
    for (int i = atomCount; --i >= 0; ) {
      if (atoms[i].getAtomNumber() == atomNumber)
        return i;
      }
    return -1;
  }

  Properties getModelSetProperties() {
    return mmset.getModelSetProperties();
  }

  String getModelSetProperty(String propertyName) {
    return mmset.getModelSetProperty(propertyName);
  }

  boolean modelSetHasVibrationVectors() {
    return hasVibrationVectors;
  }

  boolean hasVibrationVectors() {
    return hasVibrationVectors;
  }

  boolean modelHasVibrationVectors(int modelIndex) {
    if (vibrationVectors != null)
      for (int i = atomCount; --i >= 0; )
        if (atoms[i].modelIndex == modelIndex &&
            vibrationVectors[i] != null)
          return true;
    return false;
  }

  int getModelCount() {
    return mmset.getModelCount();
  }

  int getModelNumber(int modelIndex) {
    return mmset.getModelNumber(modelIndex);
  }

  String getModelName(int modelIndex) {
    return mmset.getModelName(modelIndex);
  }

  String getModelSetTypeName() {
    return modelSetTypeName;
  }

  Properties getModelProperties(int modelIndex) {
    return mmset.getModelProperties(modelIndex);
  }

  String getModelProperty(int modelIndex, String propertyName) {
    return mmset.getModelProperty(modelIndex, propertyName);
  }

  Model getModel(int modelIndex) {
    return mmset.getModel(modelIndex);
  }

  int getModelNumberIndex(int modelNumber) {
    return mmset.getModelNumberIndex(modelNumber);
  }

  ////////////////////////////////////////////////////////////////
  
  void setModelCount(int modelCount) {
    mmset.setModelCount(modelCount);
  }

  void setModelSetProperties(Properties modelSetProperties) {
    mmset.setModelSetProperties(modelSetProperties);
  }
  
  void setModelNameNumberProperties(int modelIndex,
                                    String modelName, int modelNumber,
                                    Properties modelProperties) {
    mmset.setModelNameNumberProperties(modelIndex, modelName, modelNumber,
                                       modelProperties);
  }

  ////////////////////////////////////////////////////////////////

  int getChainCount() {
    return mmset.getChainCount();
  }

  int getGroupCount() {
    return mmset.getGroupCount();
  }

  int getAtomCount() {
    return atomCount;
  }

  Atom[] getAtoms() {
    return atoms;
  }

  Atom getAtomAt(int atomIndex) {
    return atoms[atomIndex];
  }

  Point3f getAtomPoint3f(int atomIndex) {
    return atoms[atomIndex].point3f;
  }

  int getBondCount() {
    return bondCount;
  }

  Bond getBondAt(int bondIndex) {
    return bonds[bondIndex];
  }

  private void addBond(Bond bond) {
    if (bond == null)
      return;
    if (bondCount == bonds.length)
      bonds = (Bond[])Util.setLength(bonds, bondCount + growthIncrement);
    bonds[bondCount++] = bond;
  }

  void bondAtoms(Atom atom1, Atom atom2,
                             int order) {
    addBond(atom1.bondMutually(atom2, order, viewer));
  }

  Shape allocateShape(int shapeID) {
    String classBase = JmolConstants.shapeClassBases[shapeID];
    //    System.out.println("Frame.allocateShape(" + classBase + ")");
    String className = "org.jmol.viewer." + classBase;

    try {
      Class shapeClass = Class.forName(className);
      Shape shape = (Shape)shapeClass.newInstance();
      shape.setViewerG3dFrame(viewer, g3d, this);
      return shape;
    } catch (Exception e) {
      System.out.println("Could not instantiate shape:" + classBase +
                         "\n" + e);
      e.printStackTrace();
    }
    return null;
  }

  final Shape[] shapes = new Shape[JmolConstants.SHAPE_MAX];

  void loadShape(int shapeID) {
    if (shapes[shapeID] == null) {
      shapes[shapeID] = allocateShape(shapeID);
    }
  }

  void setShapeSize(int shapeID, int size, BitSet bsSelected) {
    if (size != 0)
      loadShape(shapeID);
    if (shapes[shapeID] != null)
      shapes[shapeID].setSize(size, bsSelected);
  }

  void setShapeProperty(int shapeID, String propertyName,
                               Object value, BitSet bsSelected) {
    // miguel 2004 11 23
    // Why was I loading this?
    // loadShape(shapeID);
    if (shapes[shapeID] != null)
      shapes[shapeID].setProperty(propertyName, value, bsSelected);
  }

  Object getShapeProperty(int shapeID,
                                 String propertyName, int index) {
    return (shapes[shapeID] == null
            ? null : shapes[shapeID].getProperty(propertyName, index));
  }

  Point3f averageAtomPoint;

  Point3f centerBoundingBox;
  Vector3f boundingBoxCornerVector;
  Point3f minBoundingBox;
  Point3f maxBoundingBox;

  Point3f centerUnitcell;

  //  float radiusBoundingBox;
  Point3f rotationCenter;
  float rotationRadius;
  Point3f rotationCenterDefault;
  float rotationRadiusDefault;

  Point3f getBoundingBoxCenter() {
    findBounds();
    return centerBoundingBox;
  }

  Vector3f getBoundingBoxCornerVector() {
    findBounds();
    return boundingBoxCornerVector;
  }

  Point3f getRotationCenter() {
    findBounds();
    return rotationCenter;
  }

  void increaseRotationRadius(float increaseInAngstroms) {
    rotationRadius += increaseInAngstroms;
  }

  float getRotationRadius() {
    findBounds();
    return rotationRadius;
  }

  void setRotationCenter(Point3f newCenterOfRotation) {
    if (newCenterOfRotation != null) {
      rotationCenter = newCenterOfRotation;
      rotationRadius = calcRotationRadius(rotationCenter);
    } else {
      rotationCenter = rotationCenterDefault;
      rotationRadius = rotationRadiusDefault;
    }
  }

  void clearBounds() {
    rotationCenter = null;
    rotationRadius = 0;
  }

  private void findBounds() {
    if ((rotationCenter != null) || (atomCount <= 0))
      return;
    calcRotationSphere();
  }

  private void calcRotationSphere() {
    calcAverageAtomPoint();
    calcBoundingBoxDimensions();
    if (notionalUnitcell != null)
      calcUnitcellDimensions();
    rotationCenter = rotationCenterDefault =
      averageAtomPoint;
    rotationRadius = rotationRadiusDefault =
      calcRotationRadius(rotationCenterDefault);
  }

  private void calcAverageAtomPoint() {
    Point3f average = this.averageAtomPoint = new Point3f();
    for (int i = atomCount; --i >= 0; )
      average.add(atoms[i].point3f);
    average.scale(1f/atomCount);
  }

  final static Point3f[] unitBboxPoints = {
    new Point3f( 1, 1, 1),
    new Point3f( 1, 1,-1),
    new Point3f( 1,-1, 1),
    new Point3f( 1,-1,-1),
    new Point3f(-1, 1, 1),
    new Point3f(-1, 1,-1),
    new Point3f(-1,-1, 1),
    new Point3f(-1,-1,-1),
  };

  final Point3f[] bboxVertices = new Point3f[8];

  private void calcBoundingBoxDimensions() {
    minBoundingBox = new Point3f();
    maxBoundingBox = new Point3f();

    calcAtomsMinMax(minBoundingBox, maxBoundingBox);

    centerBoundingBox = new Point3f(minBoundingBox);
    centerBoundingBox.add(maxBoundingBox);
    centerBoundingBox.scale(0.5f);
    boundingBoxCornerVector = new Vector3f(maxBoundingBox);
    boundingBoxCornerVector.sub(centerBoundingBox);

    for (int i = 8; --i >= 0; ) {
      Point3f bbcagePoint = bboxVertices[i] = new Point3f(unitBboxPoints[i]);
      bbcagePoint.x *= boundingBoxCornerVector.x;
      bbcagePoint.y *= boundingBoxCornerVector.y;
      bbcagePoint.z *= boundingBoxCornerVector.z;
      bbcagePoint.add(centerBoundingBox);
    }
  }

  final static Point3f[] unitCubePoints = {
    new Point3f( 0, 0, 0),
    new Point3f( 0, 0, 1),
    new Point3f( 0, 1, 0),
    new Point3f( 0, 1, 1),
    new Point3f( 1, 0, 0),
    new Point3f( 1, 0, 1),
    new Point3f( 1, 1, 0),
    new Point3f( 1, 1, 1),
  };

  Point3f[] unitcellVertices;

  private void calcUnitcellDimensions() {
    unitcellVertices = new Point3f[8];
    for (int i = 8; --i >= 0; ) {
      Point3f vertex = unitcellVertices[i] = new Point3f();
      matrixFractionalToEuclidean.transform(unitCubePoints[i], vertex);
    }
    centerUnitcell = new Point3f(unitcellVertices[7]);
    centerUnitcell.scale(0.5f);
  }

  private float calcRotationRadius(Point3f center) {

    float maxRadius = 0;
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      float distAtom = center.distance(atom.point3f);
      float radiusVdw = atom.getVanderwaalsRadiusFloat();
      float outerVdw = distAtom + radiusVdw;
      if (outerVdw > maxRadius)
        maxRadius = outerVdw;
    }
    
    return maxRadius;
  }

  final static int measurementGrowthIncrement = 16;
  int measurementCount = 0;
  Measurement[] measurements = null;

  /*==============================================================*
   * selection handling
   *==============================================================*/
  
  boolean frankClicked(int x, int y) {
    Shape frankShape = shapes[JmolConstants.SHAPE_FRANK];
    if (frankShape == null)
      return false;
    return frankShape.wasClicked(x, y);
  }

  final static int minimumPixelSelectionRadius = 4;

  final Closest closest = new Closest();

  int findNearestAtomIndex(int x, int y) {
    closest.atom = null;
    for (int i = 0; i < shapes.length; ++i) {
      Shape shape = shapes[i];
      if (shape != null) {
        shapes[i].findNearestAtomIndex(x, y, closest);
        if (closest.atom != null)
          break;
      }
    }
    int closestIndex = (closest.atom == null ? -1 : closest.atom.atomIndex);
    closest.atom = null;
    return closestIndex;
  }

  // jvm < 1.4 does not have a BitSet.clear();
  // so in order to clear you "and" with an empty bitset.
  final BitSet bsEmpty = new BitSet();
  final BitSet bsFoundRectangle = new BitSet();
  BitSet findAtomsInRectangle(Rectangle rect) {
    bsFoundRectangle.and(bsEmpty);
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      if (rect.contains(atom.getScreenX(), atom.getScreenY()))
        bsFoundRectangle.set(i);
    }
    return bsFoundRectangle;
  }

  BondIterator getBondIterator(short bondType, BitSet bsSelected) {
    return new SelectedBondIterator(bondType, bsSelected);
  }

  class SelectedBondIterator implements BondIterator {

    short bondType;
    int iBond;
    BitSet bsSelected;
    boolean bondSelectionModeOr;

    SelectedBondIterator(short bondType, BitSet bsSelected) {
      this.bondType = bondType;
      this.bsSelected = bsSelected;
      iBond = 0;
      bondSelectionModeOr = viewer.getBondSelectionModeOr();
    }

    public boolean hasNext() {
      for ( ; iBond < bondCount; ++iBond) {
        Bond bond = bonds[iBond];
        // mth 2004 10 20
        // a hack put in here to support bonds of order '0'
        // during implementation of 'bondOrder' script command
        if (bondType != JmolConstants.BOND_ALL_MASK &&
            (bond.order & bondType) == 0)
          continue;
        boolean isSelected1 =
          bsSelected.get(bond.atom1.atomIndex);
        boolean isSelected2 =
          bsSelected.get(bond.atom2.atomIndex);
        if ((!bondSelectionModeOr & isSelected1 & isSelected2) ||
            (bondSelectionModeOr & (isSelected1 | isSelected2)))
          return true;
      }
      return false;
    }

    public Bond next() {
      return bonds[iBond++];
    }
  }

  Bspf bspf;

  void initializeBspf() {
    if (bspf == null) {
      bspf = new Bspf(3);
      for (int i = atomCount; --i >= 0; ) {
        Atom atom = atoms[i];
        if (! atom.isDeleted())
          bspf.addTuple(atom.modelIndex, atom);
      }
    }
  }

  private void clearBspf() {
    bspf = null;
  }

  int getBsptCount() {
    if (bspf == null)
      initializeBspf();
    return bspf.getBsptCount();
  }

  private final WithinIterator withinAtomIterator = new WithinIterator();

  AtomIterator getWithinIterator(Atom atomCenter,
                                             float radius) {
    withinAtomIterator.initialize(atomCenter.modelIndex, atomCenter, radius);
    return withinAtomIterator;
  }

  class WithinIterator implements AtomIterator {

    int bsptIndex;
    Bspt.Tuple center;
    float radius;
    Bspt.SphereIterator bsptIter;

    void initialize(int bsptIndex, Bspt.Tuple center, float radius) {
      initializeBspf();
      this.bsptIndex = bsptIndex;
      bsptIter = bspf.getSphereIterator(bsptIndex);
      this.center = center;
      this.radius = radius;
      bsptIter.initialize(center, radius);
    }
    
    public boolean hasNext() {
      return bsptIter.hasMoreElements();
    }
    
    public Atom next() {
      return (Atom)bsptIter.nextElement();
    }
    
    public void release() {
      bsptIter.release();
      bsptIter = null;
    }
  }
  
  ////////////////////////////////////////////////////////////////
  // autobonding stuff
  ////////////////////////////////////////////////////////////////
  void doAutobond() {
    // perform bonding if necessary
    if (viewer.getAutoBond()) {
      if ((bondCount == 0) ||
          (modelSetTypeName == "pdb" && (bondCount < (atomCount / 2))))
        rebond(false);
    }
  }

  final static boolean showRebondTimes = false;

  private float bondTolerance;
  private float minBondDistance;
  private float minBondDistance2;

  void rebond() {
    rebond(true);
  }

  void rebond(boolean deleteFirst) {
    if (deleteFirst)
      deleteAllBonds();
    if (maxBondingRadius == Integer.MIN_VALUE)
      findMaxRadii();
    bondTolerance = viewer.getBondTolerance();
    minBondDistance = viewer.getMinBondDistance();
    minBondDistance2 = minBondDistance*minBondDistance;

    //char chainLast = '?';
    //int indexLastCA = -1;
    //Atom atomLastCA = null;

    initializeBspf();

    long timeBegin, timeEnd;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      // Covalent bonds
      float myBondingRadius = atom.getBondingRadiusFloat();
      if (myBondingRadius == 0)
        continue;
      float searchRadius =
        myBondingRadius + maxBondingRadius + bondTolerance;
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
      iter.initializeHemisphere(atom, searchRadius);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom)iter.nextElement();
        if (atomNear != atom) {
          int order = getBondOrder(atom, myBondingRadius,
                                   atomNear, atomNear.getBondingRadiusFloat(),
                                   iter.foundDistance2());
          if (order > 0)
            checkValencesAndBond(atom, atomNear, order);
        }
      }
      iter.release();
    }

    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("Time to autoBond=" + (timeEnd - timeBegin));
    }
  }

  private int getBondOrder(Atom atomA, float bondingRadiusA,
                           Atom atomB, float bondingRadiusB,
                           float distance2) {
    //            System.out.println(" radiusA=" + bondingRadiusA +
    //                               " radiusB=" + bondingRadiusB +
    //                         " distance2=" + distance2 +
    //                         " tolerance=" + bondTolerance);
    if (bondingRadiusA == 0 || bondingRadiusB == 0)
      return 0;
    float maxAcceptable = bondingRadiusA + bondingRadiusB + bondTolerance;
    float maxAcceptable2 = maxAcceptable * maxAcceptable;
    if (distance2 < minBondDistance2) {
      //System.out.println("less than minBondDistance");
      return 0;
    }
    if (distance2 <= maxAcceptable2) {
      //System.out.println("returning 1");
      return 1;
    }
    return 0;
  }

  void checkValencesAndBond(Atom atomA, Atom atomB, int order) {
    //    System.out.println("checkValencesAndBond(" +
    //                       atomA.point3f + "," + atomB.point3f + ")");
    if (atomA.getCurrentBondCount() > JmolConstants.MAXIMUM_AUTO_BOND_COUNT ||
        atomB.getCurrentBondCount() > JmolConstants.MAXIMUM_AUTO_BOND_COUNT) {
      System.out.println("maximum auto bond count reached");
      return;
    }
    addBond(atomA.bondMutually(atomB, order, viewer));
  }

  float hbondMax = 3.25f;
  float hbondMin = 2.5f;
  float hbondMin2 = hbondMin * hbondMin;
  
  boolean hbondsCalculated;

  boolean useRasMolHbondsCalculation = true;

  void calcHbonds() {
    if (hbondsCalculated)
      return;
    hbondsCalculated = true;
    if (useRasMolHbondsCalculation) {
      if (mmset != null)
        mmset.calcHydrogenBonds();
      return;
    }
    initializeBspf();
    long timeBegin, timeEnd;
    if (showRebondTimes)
      timeBegin = System.currentTimeMillis();
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      int elementNumber = atom.elementNumber;
      if (elementNumber != 7 && elementNumber != 8)
        continue;
      //float searchRadius = hbondMax;
      Bspt.SphereIterator iter = bspf.getSphereIterator(atom.modelIndex);
      iter.initializeHemisphere(atom, hbondMax);
      while (iter.hasMoreElements()) {
        Atom atomNear = (Atom)iter.nextElement();
        int elementNumberNear = atomNear.elementNumber;
        if (elementNumberNear != 7 && elementNumberNear != 8)
          continue;
        if (atomNear == atom)
          continue;
        if (iter.foundDistance2() < hbondMin2)
          continue;
        if (atom.isBonded(atomNear))
          continue;
        addBond(atom.bondMutually(atomNear, JmolConstants.BOND_H_REGULAR,
                                  viewer));
        System.out.println("adding an hbond between " + atom.atomIndex +
                           " & " + atomNear.atomIndex);
      }
      iter.release();
    }

    if (showRebondTimes) {
      timeEnd = System.currentTimeMillis();
      System.out.println("Time to hbond=" + (timeEnd - timeBegin));
    }
  }


  void deleteAllBonds() {
    for (int i = bondCount; --i >= 0; ) {
      bonds[i].deleteAtomReferences();
      bonds[i] = null;
    }
    bondCount = 0;
  }

  void deleteBond(Bond bond) {
    // what a disaster ... I hate doing this
    for (int i = bondCount; --i >= 0; ) {
      if (bonds[i] == bond) {
        bonds[i].deleteAtomReferences();
        System.arraycopy(bonds, i+1, bonds, i,
                         bondCount - i - 1);
        --bondCount;
        bonds[bondCount] = null;
        return;
      }
    }
  }

  void deleteCovalentBonds() {
    int indexNoncovalent = 0;
    for (int i = 0; i < bondCount; ++i) {
      Bond bond = bonds[i];
      if (! bond.isCovalent()) {
        if (i != indexNoncovalent) {
          bonds[indexNoncovalent++] = bond;
          bonds[i] = null;
        }
      } else {
        bond.deleteAtomReferences();
        bonds[i] = null;
      }
    }
    bondCount = indexNoncovalent;
  }

  void deleteAtom(int atomIndex) {
    clearBspf();
    atoms[atomIndex].markDeleted();
  }

  float getMaxVanderwaalsRadius() {
    if (maxVanderwaalsRadius == Integer.MIN_VALUE)
      findMaxRadii();
    return maxVanderwaalsRadius;
  }

  void setNotionalUnitcell(float[] notionalUnitcell) {
    if (notionalUnitcell != null && notionalUnitcell.length != 6)
      System.out.println("notionalUnitcell length incorrect:" +
                         notionalUnitcell);
    else
      this.notionalUnitcell = notionalUnitcell;
  }

  void setPdbScaleMatrix(float[] pdbScaleMatrixArray) {
    if (pdbScaleMatrixArray == null)
      return;
    if (pdbScaleMatrixArray.length != 9) {
      System.out.println("pdbScaleMatrix.length != 9 :" + 
                        pdbScaleMatrix);
      return;
    }
    pdbScaleMatrix = new Matrix3f(pdbScaleMatrixArray);
    pdbScaleMatrixTranspose = new Matrix3f();
    pdbScaleMatrixTranspose.transpose(pdbScaleMatrix);
  }

  void setPdbScaleTranslate(float[] pdbScaleTranslate) {
    if (pdbScaleTranslate == null)
      return;
    if (pdbScaleTranslate.length != 3) {
      System.out.println("pdbScaleTranslate.length != 3 :" + 
                         pdbScaleTranslate);
      return;
    }
    this.pdbTranslateVector = new Vector3f(pdbScaleTranslate);
  }

  ShapeRenderer getRenderer(int shapeID) {
    return frameRenderer.getRenderer(shapeID, g3d);
  }

  void doUnitcellStuff() {
    if (notionalUnitcell != null) {
      // convert fractional coordinates to cartesian
      constructFractionalMatrices();
      if (fileCoordinatesAreFractional)
        convertFractionalToEuclidean();
    }
    /*
      mth 2004 03 06
      We do not want to pack the unitcell automatically.
    putAtomsInsideUnitcell();
    */
  }

  void constructFractionalMatrices() {
    matrixNotional = new Matrix3f();
    calcNotionalMatrix(notionalUnitcell, matrixNotional);
    if (pdbScaleMatrix != null) {
      //      System.out.println("using PDB Scale matrix");
      matrixEuclideanToFractional = new Matrix3f();
      matrixEuclideanToFractional.transpose(pdbScaleMatrix);
      matrixFractionalToEuclidean = new Matrix3f();
      matrixFractionalToEuclidean.invert(matrixEuclideanToFractional);
    } else {
      //      System.out.println("using notional unit cell");
      matrixFractionalToEuclidean = matrixNotional;
      matrixEuclideanToFractional = new Matrix3f();
      matrixEuclideanToFractional.invert(matrixFractionalToEuclidean);
    }
  }

  final static float toRadians = (float)Math.PI * 2 / 360;

  void calcNotionalMatrix(float[] notionalUnitcell,
                          Matrix3f matrixNotional) {
    // note that these are oriented as columns, not as row
    // this is because we will later use the transform method,
    // which multiplies the matrix by the point, not the point by the matrix
    float gamma = notionalUnitcell[5];
    float beta  = notionalUnitcell[4];
    float alpha = notionalUnitcell[3];
    float c = notionalUnitcell[2];
    float b = notionalUnitcell[1];
    float a = notionalUnitcell[0];
    
    /* some intermediate variables */
    float cosAlpha = (float)Math.cos(toRadians * alpha);
    //float sinAlpha = (float)Math.sin(toRadians * alpha);
    float cosBeta  = (float)Math.cos(toRadians * beta);
    //float sinBeta  = (float)Math.sin(toRadians * beta);
    float cosGamma = (float)Math.cos(toRadians * gamma);
    float sinGamma = (float)Math.sin(toRadians * gamma);
    
    
    // 1. align the a axis with x axis
    matrixNotional.setColumn(0, a, 0, 0);
    // 2. place the b is in xy plane making a angle gamma with a
    matrixNotional.setColumn(1, b * cosGamma, b * sinGamma, 0);
    // 3. now the c axis,
    // http://server.ccl.net/cca/documents/molecular-modeling/node4.html
    float V = a * b * c *
      (float) Math.sqrt(1.0 - cosAlpha*cosAlpha -
                        cosBeta*cosBeta -
                        cosGamma*cosGamma +
                        2.0*cosAlpha*cosBeta*cosGamma);
    matrixNotional.
      setColumn(2, c * cosBeta,
                c * (cosAlpha - cosBeta*cosGamma)/sinGamma,
                V/(a * b * sinGamma));
  }

  void putAtomsInsideUnitcell() {
    /*
     * find connected-sets ... aka 'molecules'
     * convert to fractional coordinates
     * for each connected-set
     *   find its center
     *   if the center is outside the unitcell
     *     move the atoms
     * convert back to euclidean coordinates
     ****************************************************************/
    convertEuclideanToFractional();
    // but for now, just do one connected-set
    Point3f adjustment = findFractionalAdjustment();
    if (adjustment.x != 0 || adjustment.y != 0 || adjustment.z != 0)
      applyFractionalAdjustment(adjustment);
    convertFractionalToEuclidean();
  }

  void convertEuclideanToFractional() {
    for (int i = atomCount; --i >= 0; )
      matrixEuclideanToFractional.transform(atoms[i].point3f);
  }

  void convertFractionalToEuclidean() {
    for (int i = atomCount; --i >= 0; )
      matrixFractionalToEuclidean.transform(atoms[i].point3f);
  }

  Point3f findFractionalAdjustment() {
    Point3f pointMin = new Point3f();
    Point3f pointMax = new Point3f();
    calcAtomsMinMax(pointMin, pointMax);
    pointMin.add(pointMax);
    pointMin.scale(0.5f);

    Point3f fractionalCenter = pointMin;
    System.out.println("fractionalCenter=" + fractionalCenter);
    Point3f adjustment = pointMax;
    adjustment.set((float)Math.floor(fractionalCenter.x),
                   (float)Math.floor(fractionalCenter.y),
                   (float)Math.floor(fractionalCenter.z));
    return adjustment;
  }

  void applyFractionalAdjustment(Point3f adjustment) {
    System.out.println("applyFractionalAdjustment(" + adjustment + ")");
    for (int i = atomCount; --i >= 0; )
      atoms[i].point3f.sub(adjustment);
  }

  void calcAtomsMinMax(Point3f pointMin, Point3f pointMax) {
    float minX, minY, minZ, maxX, maxY, maxZ;
    Point3f pointT;
    pointT = atoms[0].point3f;
    minX = maxX = pointT.x;
    minY = maxY = pointT.y;
    minZ = maxZ = pointT.z;
    
    for (int i = atomCount; --i > 0; ) {
      // note that the 0 element was set above
      pointT = atoms[i].point3f;
      float t;
      t = pointT.x;
      if (t < minX) { minX = t; }
      else if (t > maxX) { maxX = t; }
      t = pointT.y;
      if (t < minY) { minY = t; }
      else if (t > maxY) { maxY = t; }
      t = pointT.z;
      if (t < minZ) { minZ = t; }
      else if (t > maxZ) { maxZ = t; }
    }
    pointMin.set(minX, minY, minZ);
    pointMax.set(maxX, maxY, maxZ);
  }

  void setLabel(String label, int atomIndex) {
  }

  void findElementsPresent() {
    elementsPresent = new BitSet();
    for (int i = atomCount; --i >= 0; )
      elementsPresent.set(atoms[i].elementNumber);
  }

  BitSet getElementsPresentBitSet() {
    return elementsPresent;
  }

  void findGroupsPresent() {
    Group groupLast = null;
    groupsPresent = new BitSet();
    for (int i = atomCount; --i >= 0; ) {
      if (groupLast != atoms[i].group) {
        groupLast = atoms[i].group;
        groupsPresent.set(groupLast.getGroupID());
      }
    }
  }

  void calcMinMaxSeqcode(BitSet bsSelected, boolean heteroSetting) {
    mmset.calcMinMaxSeqcode(bsSelected, heteroSetting);
  }

  int getMinSeqcode() {
    return mmset.minSeqcode;
  }

  int getMaxSeqcode() {
    return mmset.maxSeqcode;
  }

  void findMaxRadii() {
    for (int i = atomCount; --i >= 0; ) {
      Atom atom = atoms[i];
      float bondingRadius = atom.getBondingRadiusFloat();
      if (bondingRadius > maxBondingRadius)
        maxBondingRadius = bondingRadius;
      float vdwRadius = atom.getVanderwaalsRadiusFloat();
      if (vdwRadius > maxVanderwaalsRadius)
        maxVanderwaalsRadius = vdwRadius;
    }
  }

  BitSet getGroupsPresentBitSet() {
    return groupsPresent;
  }

  void calcBfactorRange() {
    if (! hasBfactorRange) {
      bfactor100Lo = bfactor100Hi = atoms[0].getBfactor100();
      for (int i = atomCount; --i > 0; ) {
        int bf = atoms[i].getBfactor100();
        if (bf < bfactor100Lo)
          bfactor100Lo = bf;
        else if (bf > bfactor100Hi)
          bfactor100Hi = bf;
      }
      hasBfactorRange = true;
    }
  }

  int getBfactor100Lo() {
    if (! hasBfactorRange)
      calcBfactorRange();
    return bfactor100Lo;
  }

  int getBfactor100Hi() {
    if (! hasBfactorRange)
      calcBfactorRange();
    return bfactor100Hi;
  }

  ////////////////////////////////////////////////////////////////
  // measurements
  ////////////////////////////////////////////////////////////////

  float getDistance(int atomIndexA, int atomIndexB) {
    return atoms[atomIndexA].point3f.distance(atoms[atomIndexB].point3f);
  }

  Vector3f vectorBA;
  Vector3f vectorBC;

  float getAngle(int atomIndexA, int atomIndexB, int atomIndexC) {
    if (vectorBA == null) {
      vectorBA = new Vector3f();
      vectorBC = new Vector3f();
    }
    Point3f pointA = atoms[atomIndexA].point3f;
    Point3f pointB = atoms[atomIndexB].point3f;
    Point3f pointC = atoms[atomIndexC].point3f;
    vectorBA.sub(pointA, pointB);
    vectorBC.sub(pointC, pointB);
    float angle = vectorBA.angle(vectorBC);
    float degrees = toDegrees(angle);
    return degrees;
  }

  float getTorsion(int atomIndexA, int atomIndexB,
                          int atomIndexC, int atomIndexD) {
    return computeTorsion(atoms[atomIndexA].point3f,
                          atoms[atomIndexB].point3f,
                          atoms[atomIndexC].point3f,
                          atoms[atomIndexD].point3f);
  }

  static float toDegrees(float angleRadians) {
    return angleRadians * 180 / (float)Math.PI;
  }

  static float computeTorsion(Point3f p1, Point3f p2,
                              Point3f p3, Point3f p4) {

    float ijx = p1.x - p2.x;
    float ijy = p1.y - p2.y;
    float ijz = p1.z - p2.z;

    float kjx = p3.x - p2.x;
    float kjy = p3.y - p2.y;
    float kjz = p3.z - p2.z;

    float klx = p3.x - p4.x;
    float kly = p3.y - p4.y;
    float klz = p3.z - p4.z;

    float ax = ijy * kjz - ijz * kjy;
    float ay = ijz * kjx - ijx * kjz;
    float az = ijx * kjy - ijy * kjx;
    float cx = kjy * klz - kjz * kly;
    float cy = kjz * klx - kjx * klz;
    float cz = kjx * kly - kjy * klx;

    float ai2 = 1f / (ax * ax + ay * ay + az * az);
    float ci2 = 1f / (cx * cx + cy * cy + cz * cz);

    float ai = (float)Math.sqrt(ai2);
    float ci = (float)Math.sqrt(ci2);
    float denom = ai * ci;
    float cross = ax * cx + ay * cy + az * cz;
    float cosang = cross * denom;
    if (cosang > 1) {
      cosang = 1;
    }
    if (cosang < -1) {
      cosang = -1;
    }

    float torsion = toDegrees((float)Math.acos(cosang));
    float dot  =  ijx*cx + ijy*cy + ijz*cz;
    float absDot =  Math.abs(dot);
    torsion = (dot/absDot > 0) ? torsion : -torsion;
    return torsion;
  }

  ////////////////////////////////////////////////////////////////

  BitSet getGroupBitSet(int atomIndex) {
    BitSet bsGroup = new BitSet();
    atoms[atomIndex].group.selectAtoms(bsGroup);
    return bsGroup;
  }

  BitSet getChainBitSet(int atomIndex) {
    BitSet bsChain = new BitSet();
    atoms[atomIndex].group.chain.selectAtoms(bsChain);
    return bsChain;
  }
}
