/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
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

package org.jmol.adapter.smarter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Properties;
import java.util.BitSet;
import javax.vecmath.Point3f;

import org.jmol.symmetry.SpaceGroup;
import org.jmol.symmetry.SymmetryOperation;
import org.jmol.symmetry.UnitCell;
import org.jmol.util.Logger;
import org.jmol.util.ArrayUtil;

public class AtomSetCollection {
  String fileTypeName;
  String collectionName;
  Properties atomSetCollectionProperties = new Properties();
  Hashtable atomSetCollectionAuxiliaryInfo = new Hashtable();
  
  final static String[] globalBooleans = {"someModelsHaveFractionalCoordinates",
    "someModelsHaveSymmetry", "someModelsHaveUnitcells", "isPDB"};

  final static int GLOBAL_FRACTCOORD = 0;
  final static int GLOBAL_SYMMETRY = 1;
  final static int GLOBAL_latticeCells = 2;
  
   final public static String[] notionalUnitcellTags =
  { "a", "b", "c", "alpha", "beta", "gamma" };

  final static String[] dictRefUnitcellTags =
  {"cif:_cell_length_a", "cif:_cell_length_b", "cif:cell_length_c",
   "cif:_cell_length_alpha", "cif:_cell_length_beta",
   "cif:_cell_length_gamma"};

  final private static String[] quantumShellTags = {"S", "P", "SP", "L", "D"};
  
  final public static int getQuantumShellTagID(String tag) {
    for (int i = 0; i < quantumShellTags.length; i++)
      if (tag.equals(quantumShellTags[i]))
        return (i >= 3 ? --i : i); //because "SP" and "L" are the same
    return -1;
  }
  
  int atomCount;
  public int getAtomCount() {
    return atomCount;
  }
  
  Atom[] atoms = new Atom[256];
  
  public Atom[] getAtoms() {
    return atoms;
  }
  
  public Atom getAtom(int i) {
    return atoms[i];
  }
  
  int bondCount;
  
  public int getBondCount() {
    return bondCount;
  }
  
  Bond[] bonds = new Bond[256];
  
  public Bond[] getBonds() {
    return bonds;
  }
  
  public Bond getBond(int i) {
    return bonds[i];
  }
  
  int structureCount;
  Structure[] structures = new Structure[16];
  int atomSetCount;
  public int getAtomSetCount() {
    return atomSetCount;
  }
  
  int currentAtomSetIndex = -1;
  public int getCurrentAtomSetIndex() {
    return currentAtomSetIndex;
  }

  int[] atomSetNumbers = new int[16];
  String[] atomSetNames = new String[16];
  int[] atomSetAtomCounts = new int[16];
  Properties[] atomSetProperties = new Properties[16];
  Hashtable[] atomSetAuxiliaryInfo = new Hashtable[16];
  int[] latticeCells;

  public String errorMessage;

  //float wavelength = Float.NaN;
  boolean coordinatesAreFractional;
  boolean isTrajectory;
  
  float[] notionalUnitCell = new float[6]; 
  // expands to 22 for cartesianToFractional matrix as array (PDB)
  
  SpaceGroup spaceGroup;
  UnitCell unitCell;

  public AtomSetCollection(String fileTypeName) {
    this.fileTypeName = fileTypeName;
    // set the default PATH properties as defined in the SmarterJmolAdapter
    atomSetCollectionProperties.put("PATH_KEY",
                                    SmarterJmolAdapter.PATH_KEY);
    atomSetCollectionProperties.put("PATH_SEPARATOR",
                                    SmarterJmolAdapter.PATH_SEPARATOR);
  }

  
  /**
   * Creates an AtomSetCollection based on an array of AtomSetCollection
   * 
   * @param array Array of AtomSetCollection
   */
  
  public AtomSetCollection(AtomSetCollection[] array) {
    this("Array");
    setAtomSetCollectionAuxiliaryInfo("isMultiFile", Boolean.TRUE);
    for (int i = 0; i < array.length; i++) {
      appendAtomSetCollection(i, array[i]);
    }
  }

  /**
   * Just sets the overall file type after the fact.
   * @param type
   */
  public void setFileTypeName(String type) {
    fileTypeName = type;
  }
  
  Vector trajectories;
  boolean setTrajectory() {
    if (!isTrajectory)
      trajectories = new Vector();
    return (isTrajectory = true);
  }
  
  /**
   * Appends an AtomSetCollection
   * 
   * @param collectionIndex collection index for new model number
   * @param collection AtomSetCollection to append
   */
  protected void appendAtomSetCollection(int collectionIndex,
                                         AtomSetCollection collection) {
    // Initialisations
    int existingAtomsCount = atomCount;
    // auxiliary info
    // Clone each AtomSet
    int clonedAtoms = 0;
    for (int atomSetNum = 0; atomSetNum < collection.atomSetCount; atomSetNum++) {
      newAtomSet();
      atomSetAuxiliaryInfo[currentAtomSetIndex] = collection.atomSetAuxiliaryInfo[atomSetNum];
      setAtomSetAuxiliaryInfo("title", collection.collectionName, currentAtomSetIndex);    
      setAtomSetName(collection.getAtomSetName(atomSetNum));
      Properties properties = collection.getAtomSetProperties(atomSetNum);
      if (properties != null) {
        Enumeration props = properties.keys();
        while ((props != null) && (props.hasMoreElements())) {
          String key = (String) props.nextElement();
          setAtomSetProperty(key, properties.getProperty(key));
        }
      }
      for (int atomNum = 0; atomNum < collection.atomSetAtomCounts[atomSetNum]; atomNum++) {
        try {
          newCloneAtom(collection.atoms[clonedAtoms]);
        } catch (Exception e) {
          errorMessage = "appendAtomCollection error: " + e;
        }
        clonedAtoms++;
      }

      int modelFileNumber = ((Integer)atomSetAuxiliaryInfo[currentAtomSetIndex].get("modelFileNumber")).intValue();
      modelFileNumber += (collectionIndex + 1) * 1000000;
      atomSetAuxiliaryInfo[currentAtomSetIndex].put("modelFileNumber", new Integer(modelFileNumber));

      
      //Structures: We must incorporate any global structures (modelIndex == -1) into this model
      //explicitly. Whew! This is because some cif _data structures have multiple PDB models (1skt)
      for (int i = 0; i < collection.structureCount; i++)
        if (collection.structures[i].modelIndex == atomSetNum
            || collection.structures[i].modelIndex == -1)
          addStructure(collection.structures[i]);

      // names and numbers
      atomSetNames[currentAtomSetIndex] = collection.atomSetNames[atomSetNum];
      atomSetNumbers[currentAtomSetIndex] = ((collectionIndex + 1) * 1000000)
          + collection.atomSetNumbers[atomSetNum];
    }
    // Clone bonds
    for (int bondNum = 0; bondNum < collection.bondCount; bondNum++) {
      Bond bond = collection.bonds[bondNum];
      addNewBond(bond.atomIndex1 + existingAtomsCount, bond.atomIndex2
          + existingAtomsCount, bond.order);
    }
    // Set globals
    for (int i = globalBooleans.length; --i >= 0;)
      if (Boolean.TRUE.equals(
          collection.getAtomSetCollectionAuxiliaryInfo(globalBooleans[i])))
        setGlobalBoolean(i);
  }

  protected void finalize() {
    //Logger.debug("Model.finalize() called");
      try{super.finalize();}catch(Throwable t){}
  }

  void finish() {
    atoms = null;
    bonds = null;
    notionalUnitCell = null;
    spaceGroup = null;
    structures = new Structure[16];
    atomSetNumbers = new int[16];
    atomSetNames = new String[16];
    atomSetAtomCounts = new int[16];
    atomSetProperties = new Properties[16];
    atomSetAuxiliaryInfo = new Hashtable[16];
    structureCount = 0;
    atomSetCount = 0;
    currentAtomSetIndex = -1;
  }

  void freeze() {
    //Logger.debug("AtomSetCollection.freeze; atomCount = " + atomCount);
    if (isTrajectory)
      finalizeTrajectories();
    getAltLocLists();
    getInsertionLists();
  }

  public void discardPreviousAtoms() {
    for (int i = atomCount; --i >= 0; )
      atoms[i] = null;
    atomCount = 0;
    atomSymbolicMap.clear();
    atomSetCount = 0;
    currentAtomSetIndex = -1;
    for (int i = atomSetNumbers.length; --i >= 0; ) {
      atomSetNumbers[i] = atomSetAtomCounts[i] = 0;
      atomSetNames[i] = null;
    }
  }

  public void removeAtomSet() {
    if (currentAtomSetIndex < 0)
      return;
    currentAtomSetIndex--;
    atomSetCount--;
  }
  
  Atom newCloneAtom(Atom atom) throws Exception {
    //Logger.debug("newCloneAtom()");
    Atom clone = atom.cloneAtom();
    addAtom(clone);
    return clone;
  }

  // FIX ME This should really also clone the other things pertaining
  // to an atomSet, like the bonds (which probably should be remade...)
  // but also the atomSetProperties and atomSetName...
  public void cloneFirstAtomSet() throws Exception {
    newAtomSet();
    for (int i = 0, firstCount = atomSetAtomCounts[0]; i < firstCount; ++i)
      newCloneAtom(atoms[i]);
  }

  public void cloneFirstAtomSetWithBonds(int nBonds) throws Exception {
    cloneFirstAtomSet();
    int firstCount = atomSetAtomCounts[0];
    for (int bondNum = 0; bondNum < nBonds; bondNum++) {
      Bond bond = bonds[bondCount - nBonds];
      addNewBond(bond.atomIndex1 + firstCount, bond.atomIndex2 + firstCount,
          bond.order);
    }

  }

  public void cloneLastAtomSet() throws Exception {
    //Logger.debug("cloneLastAtomSet");
    //Logger.debug("b4 atomCount=" + atomCount);
    //Logger.debug("atomSetCount=" + atomSetCount);
    //Logger.debug("atomSetAtomCount=" +
    //                       atomSetAtomCounts[currentAtomSetIndex]);
    int count = getLastAtomSetAtomCount();
    int atomIndex = getLastAtomSetAtomIndex();
    newAtomSet();
    for ( ; --count >= 0; ++atomIndex)
      newCloneAtom(atoms[atomIndex]);
    //Logger.debug("after atomCount=" + atomCount);
  }
  
  public int getFirstAtomSetAtomCount() {
    return atomSetAtomCounts[0];
  }

  public int getLastAtomSetAtomCount() {
    return atomSetAtomCounts[currentAtomSetIndex];
  }

  public int getLastAtomSetAtomIndex() {
    //Logger.debug("atomSetCount=" + atomSetCount);
    return atomCount - atomSetAtomCounts[currentAtomSetIndex];
  }

  public Atom addNewAtom() {
    Atom atom = new Atom();
    addAtom(atom);
    return atom;
  }

  public void addAtom(Atom atom) {
    if (atomCount == atoms.length)
      atoms = (Atom[])ArrayUtil.doubleLength(atoms);
    atom.atomIndex = atomCount;
    atoms[atomCount++] = atom;
    if (atomSetCount == 0)
      newAtomSet();
    /*
     * WAS: {
      atomSetCount = 1;
      currentAtomSetIndex = 0;
      atomSetNumbers[0] = 1;
      setAtomSetAuxiliaryInfo("modelFileNumber", new Integer(1));
    }
     */
    atom.atomSetIndex = currentAtomSetIndex;
    atom.atomSite = atomSetAtomCounts[currentAtomSetIndex]++;
  }

  public void addAtomWithMappedName(Atom atom) {
    addAtom(atom);
    mapMostRecentAtomName();
  }

  public void addAtomWithMappedSerialNumber(Atom atom) {
    addAtom(atom);
    mapMostRecentAtomSerialNumber();
  }

  public Bond addNewBond(int atomIndex1, int atomIndex2) {
    return addNewBond(atomIndex1, atomIndex2, 1);
  }

  Bond addNewBond(String atomName1, String atomName2) {
    return addNewBond(atomName1, atomName2, 1);
  }

  public Bond addNewBond(int atomIndex1, int atomIndex2, int order) {
    if (atomIndex1 < 0 || atomIndex1 >= atomCount ||
        atomIndex2 < 0 || atomIndex2 >= atomCount)
      return null;
    Bond bond = new Bond(atomIndex1, atomIndex2, order);
    addBond(bond);
    return bond;
  }
  
  public Bond addNewBond(String atomName1, String atomName2, int order) {
    return addNewBond(getAtomNameIndex(atomName1),
                      getAtomNameIndex(atomName2),
                      order);
  }

  public Bond addNewBondWithMappedSerialNumbers(int atomSerial1, int atomSerial2,
                                         int order) {
    return addNewBond(getAtomSerialNumberIndex(atomSerial1),
                      getAtomSerialNumberIndex(atomSerial2),
                      order);
  }

  public void addBond(Bond bond) {
    if (bond.atomIndex1 < 0 ||
        bond.atomIndex2 < 0 ||
        bond.order < 0) {
      if (Logger.isActiveLevel(Logger.LEVEL_DEBUG)) {
        Logger.debug(
            ">>>>>>BAD BOND:" + bond.atomIndex1 + "-" +
            bond.atomIndex2 + ":" + bond.order);
      }
      return;
    }
    if (bondCount == bonds.length)
      bonds = (Bond[])ArrayUtil.setLength(bonds, bondCount + 1024);
    bonds[bondCount++] = bond;
  }

  public void addStructure(Structure structure) {
    if (structureCount == structures.length)
      structures = (Structure[])ArrayUtil.setLength(structures,
                                                      structureCount + 32);
    structure.modelIndex = currentAtomSetIndex;
    structures[structureCount++] = structure;
  }

  void setAtomSetSpaceGroupName(String spaceGroupName) {
    setAtomSetAuxiliaryInfo("spaceGroup", spaceGroupName+"");
  }
    
  void setCoordinatesAreFractional(boolean coordinatesAreFractional) {
    this.coordinatesAreFractional = coordinatesAreFractional;
    setAtomSetAuxiliaryInfo(
        "coordinatesAreFractional",
        Boolean.valueOf(coordinatesAreFractional));
    if (coordinatesAreFractional)
      setGlobalBoolean(GLOBAL_FRACTCOORD);    
  }
  
  void setLatticeCells(int[] latticeCells, boolean applySymmetryToBonds) {
    //set when unit cell is determined
    // x <= 555 and y >= 555 indicate a range of cells to load
    // AROUND the central cell 555 and that
    // we should normalize (z = 1) or not (z = 0)
    this.latticeCells = latticeCells;
    isLatticeRange = (latticeCells[2] == 0 || latticeCells[2] == 1) && (latticeCells[0] <= 555  && latticeCells[1] >= 555);
    doNormalize = (!isLatticeRange || latticeCells[2] == 1);
    setApplySymmetryToBonds(applySymmetryToBonds);
  }
  
  boolean setNotionalUnitCell(float[] info) {
    notionalUnitCell = new float[info.length];
    for (int i = 0; i < info.length; i++)
      notionalUnitCell[i] = info[i];
    setAtomSetAuxiliaryInfo("notionalUnitcell", notionalUnitCell);
    setGlobalBoolean(GLOBAL_latticeCells);
    unitCell = new UnitCell(notionalUnitCell);
    return true;
  }

  void setGlobalBoolean(int globalIndex) {
    setAtomSetCollectionAuxiliaryInfo(globalBooleans[globalIndex], Boolean.TRUE);
  }
  
  boolean addSymmetry(String xyz) {
    if (spaceGroup == null)
      spaceGroup = new SpaceGroup(doNormalize);
    if (!spaceGroup.addSymmetry(xyz))
      return false;
    return true;
  }
  
  public void setLatticeParameter(int latt) {
    if (spaceGroup == null)
      spaceGroup = new SpaceGroup(doNormalize);
    spaceGroup.setLattice(latt);
  }
  
   void applySymmetry() throws Exception {
     //parameters are counts of unit cells as [a b c]
     applySymmetry(latticeCells[0], latticeCells[1], latticeCells[2]);
   }

   void applySymmetry(SpaceGroup spaceGroup) throws Exception {
     this.spaceGroup = spaceGroup;
     //parameters are counts of unit cells as [a b c]
     applySymmetry(latticeCells[0], latticeCells[1], latticeCells[2]);
   }

   boolean doNormalize = true;
   boolean isLatticeRange = false;
   
   void applySymmetry(int maxX, int maxY, int maxZ) throws Exception {
    if (coordinatesAreFractional && spaceGroup != null)
      applyAllSymmetry(maxX, maxY, maxZ);
   }

   private void applyAllSymmetry(int maxX, int maxY, int maxZ) throws Exception {
     
    int count = getLastAtomSetAtomCount();
    int atomIndex = getLastAtomSetAtomIndex();
    bondCount0 = bondCount;
    
    SymmetryOperation[] finalOperations = spaceGroup.getFinalOperations(atoms,
        atomIndex, count, doNormalize);
    int operationCount = finalOperations.length;
    int minX = 0;
    int minY = 0;
    int minZ = 0;
    if (isLatticeRange) {
      //alternative format for indicating a range of cells:
      //{111 666}
      //555 --> {0 0 0}
      minX = (maxX / 100) - 5;
      minY = (maxX % 100) / 10 - 5;
      minZ = (maxX % 10) - 5;
      //555 --> {1 1 1}
      maxX = (maxY / 100) - 4;
      maxZ = (maxY % 10) - 4;
      maxY = (maxY % 100) / 10 - 4;
    }
    int nCells = (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    cartesians = new Point3f[count * operationCount * nCells];
    for (int i = 0; i < count; i++)
      atoms[i + atomIndex].bsSymmetry = new BitSet(operationCount * (nCells + 1));
    int pt = 0;
    int[] unitCells = new int[nCells];
    int iCell = 0;
    for (int tx = minX; tx < maxX; tx++)
      for (int ty = minY; ty < maxY; ty++)
        for (int tz = minZ; tz < maxZ; tz++) {
          unitCells[iCell++] = 555 + tx * 100 + ty * 10 + tz;
          if (tx == 0 && ty == 0 && tz == 0 && cartesians.length > 0) {
              for (pt = 0; pt < count; pt++) {
                Atom atom = atoms[atomIndex + pt];
                cartesians[pt] = new Point3f(atom);
                finalOperations[0].transform(cartesians[pt]);
                unitCell.toCartesian(cartesians[pt]);
                atom.bsSymmetry.set(iCell * operationCount);
                atom.bsSymmetry.set(0);
              }
              pt = symmetryAddAtoms(finalOperations, atomIndex, count, 0, 0, 0, pt, iCell * operationCount);
            }            
        }
    iCell = 0;
    for (int tx = minX; tx < maxX; tx++)
      for (int ty = minY; ty < maxY; ty++)
        for (int tz = minZ; tz < maxZ; tz++) {
          iCell++;
          if (tx != 0 || ty != 0 || tz != 0) 
            pt = symmetryAddAtoms(finalOperations, atomIndex, count, tx, ty,
                tz, pt, iCell * operationCount);
        }
    if (operationCount > 0) {
      String[] symmetryList = new String[operationCount];
      for (int i = 0; i < operationCount; i++)
        symmetryList[i] = ""
            + (doNormalize ? finalOperations[i].getXyz() : finalOperations[i]
                .getXyzOriginal());
      setAtomSetAuxiliaryInfo("symmetryOperations", symmetryList);
    }
    setAtomSetAuxiliaryInfo("presymmetryAtomIndex", new Integer(atomIndex));
    setAtomSetAuxiliaryInfo("presymmetryAtomCount", new Integer(count));
    setAtomSetAuxiliaryInfo("symmetryCount", new Integer(operationCount));
    setAtomSetAuxiliaryInfo("latticeDesignation", spaceGroup
        .getLatticeDesignation());
    setAtomSetAuxiliaryInfo("unitCellRange", unitCells);
    spaceGroup = null;
    notionalUnitCell = new float[6];
    coordinatesAreFractional = false; //turn off global fractional conversion -- this wil be model by model
    setGlobalBoolean(GLOBAL_SYMMETRY);
  }
  
  Point3f[] cartesians;
  int bondCount0;
  int bondIndex0;
  boolean applySymmetryToBonds = false;
  
  void setApplySymmetryToBonds(boolean TF) {
    applySymmetryToBonds = TF;
  }
  
  private int symmetryAddAtoms(SymmetryOperation[] finalOperations,
                               int atomIndex, int count, int transX,
                               int transY, int transZ, int pt, int iCellOpPt) throws Exception {
    boolean isBaseCell = (transX == 0 && transY == 0 && transZ == 0);
    int nOperations = finalOperations.length;
    int[] atomMap = new int[count];
    for (int iSym = 0; iSym < nOperations; iSym++) {
      if (isBaseCell && finalOperations[iSym].getXyz().equals("x,y,z"))
        continue;
      int pt0 = pt;
      int i1 = atomIndex;
      int i2 = i1 + count;
      for (int i = i1; i < i2; i++) {
        Atom atom = new Atom();
        finalOperations[iSym].newPoint(atoms[i], atom, transX, transY, transZ);
        Point3f cartesian = new Point3f(atom);
        unitCell.toCartesian(cartesian);

        //System.out.println((Point3f) atoms[i] + " " + (Point3f) atom + " "
        //    + transX + " " + transY + " " + transZ + " " + cartesian
        //    + finalOperations[iSym].getXyz());

        Atom special = null;
        for (int j = pt0; --j >= 0;) {
          //            System.out.println(j + ": " + cartesians[j] + " ?cart? " + cartesian + " d=" + cartesian.distance(cartesians[j]));

          if (cartesian.distance(cartesians[j]) < 0.01) {
            special = atoms[atomIndex + j];
            break;
          }
        }
        if (special != null) {
          atomMap[atoms[i].atomSite] = special.atomIndex;
          special.bsSymmetry.set(iCellOpPt + iSym);
          special.bsSymmetry.set(iSym);

          // System.out.println(iSym + " " + finalOperations[iSym].getXyz()
          // + " special set: " + i + " " + " " + special.bsSymmetry);

          //        System.out.println(cartesian+"Y" + "X" + finalOperations[iSym].getXyz() + " " + transX+" "+transY+" "+transZ);
        } else {
          atomMap[atoms[i].atomSite] = atomCount;
          Atom atom1 = newCloneAtom(atoms[i]);
          atom1.x = atom.x;
          atom1.y = atom.y;
          atom1.z = atom.z;
          //          System.out.println(cartesian + " " + (Point3f)atom + "X" + finalOperations[iSym].getXyz() + " " + transX+" "+transY+" "+transZ);
          atom1.atomSite = atoms[i].atomSite;
          atom1.bsSymmetry = new BitSet();
          atom1.bsSymmetry.set(iCellOpPt + iSym);
          atom1.bsSymmetry.set(iSym);
          cartesians[pt++] = cartesian;
        }
      }
      if (bondCount0 > bondIndex0 && applySymmetryToBonds) {
        // Clone bonds
        for (int bondNum = bondIndex0; bondNum < bondCount0; bondNum++) {
          Bond bond = bonds[bondNum];
          int iAtom1 = atomMap[atoms[bond.atomIndex1].atomSite];
          int iAtom2 = atomMap[atoms[bond.atomIndex2].atomSite];
          if (iAtom1 >= i2 || iAtom2 >= i2)
          addNewBond(iAtom1, iAtom2, bond.order);
        }
      }
    }
    return pt;
  }
  
  
  
  public void setCollectionName(String collectionName) {
    if (collectionName != null) {
      collectionName = collectionName.trim();
      if (collectionName.length() == 0)
        return;
        this.collectionName = collectionName;
    }
  }

  Hashtable atomSymbolicMap = new Hashtable();

  void mapMostRecentAtomName() {
    //from ?? 
    if (atomCount > 0) {
      int index = atomCount - 1;
      String atomName = atoms[index].atomName;
      if (atomName != null)
        atomSymbolicMap.put(atomName, new Integer(index));
    }
  }

  void mapMostRecentAtomSerialNumber() {
    // from ?? 
    if (atomCount > 0) {
      int index = atomCount - 1;
      int atomSerial = atoms[index].atomSerial;
      if (atomSerial != Integer.MIN_VALUE)
        atomSymbolicMap.put(new Integer(atomSerial), new Integer(index));
    }
  }

  void mapAtomName(String atomName, int atomIndex) {
    atomSymbolicMap.put(atomName, new Integer(atomIndex));
  }

  public int getAtomNameIndex(String atomName) {
    //for new Bond -- inconsistent with mmCIF altLoc
    int index = -1;
    Object value = atomSymbolicMap.get(atomName);
    if (value != null)
      index = ((Integer)value).intValue();
    return index;
  }

  int getAtomSerialNumberIndex(int serialNumber) {
    int index = -1;
    Object value = atomSymbolicMap.get(new Integer(serialNumber));
    if (value != null)
      index = ((Integer)value).intValue();
    return index;
  }
  
  /**
   * Sets a property for the AtomSetCollection
   * @param key The poperty key.
   * @param value The property value.
   */
  public void setAtomSetCollectionProperty(String key, String value) {
    atomSetCollectionProperties.put(key, value);
  }
  
  String getAtomSetCollectionProperty(String key) {
    return (String) atomSetCollectionProperties.get(key);
  }
  
  public void setAtomSetCollectionAuxiliaryInfo(String key, Object value) {
    atomSetCollectionAuxiliaryInfo.put(key, value);
  }
  
  /**
   * Sets the partial atomic charges based on atomSetCollection auxiliary info
   *
   * @param auxKey The auxiliary key name that contains the charges
   * @return true if the data exist; false if not  
   */

  public boolean setAtomSetCollectionPartialCharges(String auxKey) {
    if (! atomSetCollectionAuxiliaryInfo.containsKey(auxKey))
      return false;
    Vector atomData = (Vector) atomSetCollectionAuxiliaryInfo.get(auxKey);
    for (int i = atomData.size(); --i >= 0;) 
      atoms[i].partialCharge = ((Float)atomData.get(i)).floatValue();
    return true;
  }
  
  public void mapPartialCharge(String atomName, float charge) {
    atoms[getAtomNameIndex(atomName)].partialCharge = charge;  
  }
  
  public Object getAtomSetCollectionAuxiliaryInfo(String key) {
    return atomSetCollectionAuxiliaryInfo.get(key);
  }
  
  ////////////////////////////////////////////////////////////////
  // atomSet stuff
  ////////////////////////////////////////////////////////////////
  
  int nTrajectories = 0;
  Point3f[] trajectory;

  void addTrajectory() {
    if (trajectory.length == 0) //done with FIRST atom set
      trajectory = new Point3f[atomCount];
    for (int i = 0; i < atomCount; i++)
      trajectory[i] = new Point3f(atoms[i]);
    trajectories.addElement(trajectory);
    nTrajectories++;
    //System.out.println(" nTrajectories:" + nTrajectories + " coord 4: " + trajectory[4]);
  }
  
  void finalizeTrajectories() {
    if (trajectory == null || trajectory.length == 0 || nTrajectories == 0)
      return;
    addTrajectory();
    //reset atom positions to original trajectory
    Point3f[] trajectory = (Point3f[])trajectories.get(0);
    for (int i = 0; i < atomCount; i++)
      atoms[i].set(trajectory[i]);
    setAtomSetCollectionAuxiliaryInfo("trajectories", trajectories);
  }
  
  public void newAtomSet() {
    bondIndex0 = bondCount;
    if (isTrajectory) {
      if (trajectory == null && atomCount > 0)
        trajectory = new Point3f[0];
      if (trajectory != null) { // not BEFORE first atom set
        addTrajectory();
      }
      trajectory = new Point3f[atomCount];
      discardPreviousAtoms();
    }
    currentAtomSetIndex = atomSetCount++;
    if (atomSetCount > atomSetNumbers.length) {
      atomSetNumbers = ArrayUtil.doubleLength(atomSetNumbers);
      atomSetNames = ArrayUtil.doubleLength(atomSetNames);
      atomSetAtomCounts = ArrayUtil.doubleLength(atomSetAtomCounts);
      atomSetProperties = (Properties[]) ArrayUtil
          .doubleLength(atomSetProperties);
      atomSetAuxiliaryInfo = (Hashtable[]) ArrayUtil
          .doubleLength(atomSetAuxiliaryInfo);
    }
    atomSetNumbers[currentAtomSetIndex] = atomSetCount;
    // miguel 2006 03 22
    // added this clearing of the atomSymbolicMap to support V3000
    // seems that it should have been here all along, but apparently
    // noone else needed it
    atomSymbolicMap.clear();
    setAtomSetAuxiliaryInfo("modelFileNumber", new Integer(
        currentAtomSetIndex + 1));
    setAtomSetAuxiliaryInfo("title", collectionName);    
  }

  /**
  * Sets the name for the current AtomSet
  *
  * @param atomSetName The name to be associated with the current AtomSet
  */
  public void setAtomSetName(String atomSetName) {
    setAtomSetName(atomSetName, currentAtomSetIndex);
  }
  
  /**
  * Sets the name for an AtomSet
  *
  * @param atomSetName The number to be associated with the AtomSet
  * @param atomSetIndex The index of the AtomSet that needs the association
  */
  public void setAtomSetName(String atomSetName, int atomSetIndex) {
    atomSetNames[atomSetIndex] = atomSetName;
  }
  
  /**
   * Sets the atom set names of the last n atomSets
   * @param atomSetName The name
   * @param n The number of last AtomSets that need these set
   */
  public void setAtomSetNames(String atomSetName, int n) {
    for (int idx = currentAtomSetIndex; --n >= 0; --idx)
      setAtomSetName( atomSetName, idx);
  }

  /**
  * Sets the number for the current AtomSet
  *
  * @param atomSetNumber The number for the current AtomSet.
  */
  public void setAtomSetNumber(int atomSetNumber) {
    atomSetNumbers[currentAtomSetIndex] = atomSetNumber;
  }
  
  /**
  * Sets a property for the AtomSet
  *
  * @param key The key for the property
  * @param value The value to be associated with the key
  */
  public void setAtomSetProperty(String key, String value) {
    setAtomSetProperty(key, value, currentAtomSetIndex);
  }

  /**
  * Sets auxiliary information for the AtomSet
  *
  * @param key The key for the property
  * @param value The value to be associated with the key
  */
  public void setAtomSetAuxiliaryInfo(String key, Object value) {
    setAtomSetAuxiliaryInfo(key, value, currentAtomSetIndex);
  }

  /**
   * Sets the partial atomic charges based on atomSet auxiliary info
   *
   * @param auxKey The auxiliary key name that contains the charges
   * @return true if the data exist; false if not  
   */

  boolean setAtomSetPartialCharges(String auxKey) {
    if (!atomSetAuxiliaryInfo[currentAtomSetIndex].containsKey(auxKey))
      return false;
    Vector atomData = (Vector) getAtomSetAuxiliaryInfo(currentAtomSetIndex, auxKey);
    for (int i = atomData.size(); --i >= 0;)
      atoms[i].partialCharge = ((Float) atomData.get(i)).floatValue();
    return true;
  }
  
  Object getAtomSetAuxiliaryInfo(int index, String key) {
    return  atomSetAuxiliaryInfo[index].get(key);
  }
  
  /**
  * Sets the a property for the an AtomSet
  *
  * @param key The key for the property
  * @param value The value for the property
  * @param atomSetIndex The index of the AtomSet to get the property
  */
  public void setAtomSetProperty(String key, String value, int atomSetIndex) {
    // lazy instantiation of the Properties object
    if (atomSetProperties[atomSetIndex] == null)
      atomSetProperties[atomSetIndex] = new Properties();
    atomSetProperties[atomSetIndex].put(key, value);
  }

  /**
   * Sets auxiliary information for the an AtomSet
   *
   * @param key The key for the property
   * @param value The value for the property
   * @param atomSetIndex The index of the AtomSet to get the property
   */
  void setAtomSetAuxiliaryInfo(String key, Object value, int atomSetIndex) {
    if (atomSetAuxiliaryInfo[atomSetIndex] == null)
      atomSetAuxiliaryInfo[atomSetIndex] = new Hashtable();
    //Logger.debug(atomSetIndex + " key="+ key + " value="+ value);
    if (value == null)
      return;
    atomSetAuxiliaryInfo[atomSetIndex].put(key, value);
  }

  /**
   * Sets the same properties for the last n atomSets.
   * @param key The key for the property
   * @param value The value of the property
   * @param n The number of last AtomSets that need these set
   */
  public void setAtomSetProperties(String key, String value, int n) {
    for (int idx=currentAtomSetIndex; --n >= 0; --idx) {
      setAtomSetProperty(key, value, idx);
    }    
  }
  

  /**
   * Clones the properties of the last atom set and associates it
   * with the current atom set. 
   */
  public void cloneLastAtomSetProperties() {
    cloneAtomSetProperties(currentAtomSetIndex-1);
  }

  /**
   * Clones the properties of an atom set and associated it with the
   * current atom set.
   * @param index The index of the atom set whose properties are to be cloned.
   */
  void cloneAtomSetProperties(int index) {
    atomSetProperties[currentAtomSetIndex] = 
      (Properties) atomSetProperties[index].clone();
  }
/*
  // currently not needed because we take the atomSetCount directly
  int getAtomSetCount() {
    return atomSetCount;
  }
*/

  int getAtomSetNumber(int atomSetIndex) {
    return atomSetNumbers[atomSetIndex];
  }

  String getAtomSetName(int atomSetIndex) {
    return atomSetNames[atomSetIndex];
  }
  
  Properties getAtomSetProperties(int atomSetIndex) {
    return atomSetProperties[atomSetIndex];
  }

  Hashtable getAtomSetAuxiliaryInfo(int atomSetIndex) {
    return atomSetAuxiliaryInfo[atomSetIndex];
  }

  ////////////////////////////////////////////////////////////////
  // special support for alternate locations and insertion codes
  ////////////////////////////////////////////////////////////////

  boolean hasAlternateLocations() {
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].alternateLocationID != '\0')
        return true;
    return false;
  }

  void getAltLocLists() {
    if (!hasAlternateLocations())
      return;
    String[] lists = new String[atomSetCount];
    for (int i = 0; i < atomSetCount; i++)
      lists[i] = "";
    for (int i = 0; i < atomCount; i++) {
      char id = atoms[i].alternateLocationID;
      if (id == '\0' || lists[atoms[i].atomSetIndex].indexOf(id) >= 0)
        continue;
      lists[atoms[i].atomSetIndex] += id;
    }
    for (int i = 0; i < atomSetCount; i++)
      if (lists[i].length() > 0)
        setAtomSetAuxiliaryInfo("altLocs", lists[i], i);
  }
  
  boolean hasInsertions() {
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].insertionCode != '\0')
        return true;
    return false;
  }

  void getInsertionLists() {
    if (!hasInsertions())
      return;
    String[] lists = new String[atomSetCount];
    for (int i = 0; i < atomSetCount; i++)
      lists[i] = "";
    for (int i = 0; i < atomCount; i++) {
      char id = atoms[i].insertionCode;
      if (id == '\0' || lists[atoms[i].atomSetIndex].indexOf(id) >= 0)
        continue;
      lists[atoms[i].atomSetIndex] += id;
    }
    for (int i = 0; i < atomSetCount; i++)
      if (lists[i].length() > 0)
        setAtomSetAuxiliaryInfo("insertionCodes", lists[i], i);
  }

}
