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
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.adapter.smarter;
import java.util.Hashtable;
import java.util.Properties;

class AtomSetCollection {
  String fileTypeName;
  String collectionName;
  Properties atomSetCollectionProperties = new Properties();

  final static String[] notionalUnitcellTags =
  { "a", "b", "c", "alpha", "beta", "gamma" };

  final static String[] dictRefUnitcellTags =
  {"cif:_cell_length_a", "cif:_cell_length_b", "cif:cell_length_c",
   "cif:_cell_length_alpha", "cif:_cell_length_beta",
   "cif:_cell_length_gamma"};

  int atomCount;
  Atom[] atoms = new Atom[256];
  int bondCount;
  Bond[] bonds = new Bond[256];
  int structureCount;
  Structure[] structures = new Structure[16];
  
  int atomSetCount;
  int currentAtomSetIndex = -1;
  int[] atomSetNumbers = new int[16];
  String[] atomSetNames = new String[16];
  int[] atomSetAtomCounts = new int[16];
  Properties[] atomSetProperties = new Properties[16];

  String errorMessage;

  String spaceGroup;
  float wavelength = Float.NaN;
  boolean coordinatesAreFractional;
  float[] notionalUnitcell;
  float[] pdbScaleMatrix;
  float[] pdbScaleTranslate;

  String[] pdbStructureRecords;

  float[] volumetricOrigin;
  float[][] volumetricSurfaceVectors;
  float[][][] volumetricSurfaceData;

  AtomSetCollection(String fileTypeName) {
    this.fileTypeName = fileTypeName;
    // set the default PATH properties as defined in the SmarterJmolAdapter
    atomSetCollectionProperties.put("PATH_KEY",
                                    SmarterJmolAdapter.PATH_KEY);
    atomSetCollectionProperties.put("PATH_SEPARATOR",
                                    SmarterJmolAdapter.PATH_SEPARATOR);
  }

  protected void finalize() {
    //    System.out.println("Model.finalize() called");
      try{super.finalize();}catch(Throwable t){}
  }

  void finish() {
    atoms = null;
    bonds = null;
    notionalUnitcell = pdbScaleMatrix = pdbScaleTranslate = null;
    pdbStructureRecords = null;
  }

  void freeze() {
    System.out.println("AtomSetCollection.freeze called");
    if (hasAlternateLocations())
      hackAlternateLocationDamage();
  }

  void discardPreviousAtoms() {
    for (int i = atomCount; --i >= 0; )
      atoms[i] = null;
    atomCount = 0;
    atomNameMap.clear();
    atomSetCount = 0;
    currentAtomSetIndex = -1;
    for (int i = atomSetNumbers.length; --i >= 0; ) {
      atomSetNumbers[i] = atomSetAtomCounts[i] = 0;
      atomSetNames[i] = null;
    }
  }

  Atom newCloneAtom(Atom atom) {
    //    System.out.println("newCloneAtom()");
    Atom clone = atom.cloneAtom();
    addAtom(clone);
    return clone;
  }

  // FIX ME This should really also clone the other things pertaining
  // to an atomSet, like the bonds (which probably should be remade...)
  // but also the atomSetProperties and atomSetName...
  void cloneFirstAtomSet() {
    newAtomSet();
    for (int i = 0, firstCount = atomSetAtomCounts[0]; i < firstCount; ++i)
      newCloneAtom(atoms[i]);
  }

  void cloneLastAtomSet() {
    //    System.out.println("cloneLastAtomSet");
    //    System.out.println("b4 atomCount=" + atomCount);
    //    System.out.println("atomSetCount=" + atomSetCount);
    //    System.out.println("atomSetAtomCount=" +
    //                       atomSetAtomCounts[currentAtomSetIndex]);
    int count = getLastAtomSetAtomCount();
    int atomIndex = getLastAtomSetAtomIndex();
    newAtomSet();
    for ( ; --count >= 0; ++atomIndex)
      newCloneAtom(atoms[atomIndex]);
    //    System.out.println("after atomCount=" + atomCount);
  }
  
  int getFirstAtomSetAtomCount() {
    return atomSetAtomCounts[0];
  }

  int getLastAtomSetAtomCount() {
    return atomSetAtomCounts[currentAtomSetIndex];
  }

  int getLastAtomSetAtomIndex() {
    //    System.out.println("atomSetCount=" + atomSetCount);
    return atomCount - atomSetAtomCounts[currentAtomSetIndex];
  }

  Atom addNewAtom() {
    Atom atom = new Atom();
    addAtom(atom);
    return atom;
  }

  void addAtom(Atom atom) {
    if (atomCount == atoms.length)
      atoms = (Atom[])AtomSetCollectionReader.doubleLength(atoms);
    atoms[atomCount++] = atom;
    if (atomSetCount == 0) {
      atomSetCount = 1;
      currentAtomSetIndex = 0;
      atomSetNumbers[0] = 1;
    }
    atom.atomSetIndex = currentAtomSetIndex;
    ++atomSetAtomCounts[currentAtomSetIndex];
    /*
    System.out.println("addAtom ... after" +
                       "\natomCount=" + atomCount +
                       "\natomSetCount=" + atomSetCount +
                       "\natomSetAtomCounts[" + (currentAtomSetIndex) + "]=" +
                       atomSetAtomCounts[atomSetIndex]);
    */
  }

  void addAtomWithMappedName(Atom atom) {
    addAtom(atom);
    mapMostRecentAtomName();
  }

  Bond addNewBond(int atomIndex1, int atomIndex2) {
    return addNewBond(atomIndex1, atomIndex2, 1);
  }

  Bond addNewBond(String atomName1, String atomName2) {
    return addNewBond(atomName1, atomName2, 1);
  }

  Bond addNewBond(int atomIndex1, int atomIndex2, int order) {
    Bond bond = new Bond(atomIndex1, atomIndex2, order);
    addBond(bond);
    return bond;
  }
  
  Bond addNewBond(String atomName1, String atomName2, int order) {
    return addNewBond(getAtomNameIndex(atomName1),
                      getAtomNameIndex(atomName2),
                      order);
  }

  void addBond(Bond bond) {
    /*
    System.out.println("I see a bond:" + bond.atomIndex1 + "-" +
                       bond.atomIndex2 + ":" + bond.order);
    */
    if (bond.atomIndex1 < 0 ||
        bond.atomIndex2 < 0 ||
        bond.order <= 0) {
      /*
      System.out.println(">>>>>>BAD BOND:" + bond.atomIndex1 + "-" +
                         bond.atomIndex2 + ":" + bond.order);
      */
      return;
    }
    if (bondCount == bonds.length)
      bonds = (Bond[])AtomSetCollectionReader.setLength(bonds, bondCount + 1024);
    bonds[bondCount++] = bond;
  }

  void addStructure(Structure structure) {
    if (structureCount == structures.length)
      structures = (Structure[])AtomSetCollectionReader.setLength(structures,
                                                      structureCount + 32);
    structures[structureCount++] = structure;
  }

  void setCollectionName(String collectionName) {
    if (collectionName != null) {
      collectionName = collectionName.trim();
      if (collectionName.length() > 0)
        this.collectionName = collectionName;
    }
  }

  Hashtable atomNameMap = new Hashtable();

  void mapMostRecentAtomName() {
    if (atomCount > 0) {
      int index = atomCount - 1;
      String atomName = atoms[index].atomName;
      if (atomName != null)
        atomNameMap.put(atomName, new Integer(atomCount - 1));
    }
  }

  void mapAtomName(String atomName, int atomIndex) {
    atomNameMap.put(atomName, new Integer(atomIndex));
  }
  
  int getAtomNameIndex(String atomName) {
    int index = -1;
    Object value = atomNameMap.get(atomName);
    if (value != null)
      index = ((Integer)value).intValue();
    return index;
  }
  
  /**
   * Sets a property for the AtomSetCollection
   * @param key The poperty key.
   * @param value The property value.
   */
  void setAtomSetCollectionProperty(String key, String value) {
    atomSetCollectionProperties.put(key, value);
  }
  
  ////////////////////////////////////////////////////////////////
  // atomSet stuff
  ////////////////////////////////////////////////////////////////

  void newAtomSet() {
    //    System.out.println("newAtomSet()");
    currentAtomSetIndex = atomSetCount++;
    if (atomSetCount > atomSetNumbers.length) {
      atomSetNumbers = AtomSetCollectionReader.doubleLength(atomSetNumbers);
      atomSetNames = AtomSetCollectionReader.doubleLength(atomSetNames);
      atomSetAtomCounts =
        AtomSetCollectionReader.doubleLength(atomSetAtomCounts);
      atomSetProperties = 
        (Properties[]) AtomSetCollectionReader.doubleLength(atomSetProperties);
    }
    atomSetNumbers[currentAtomSetIndex] = atomSetCount;
  }

  /**
  * Sets the name for the current AtomSet
  *
  * @param atomSetName The name to be associated with the current AtomSet
  */
  void setAtomSetName(String atomSetName) {
    atomSetNames[currentAtomSetIndex] = atomSetName;
  }
  
  /**
  * Sets the name for an AtomSet
  *
  * @param atomSetName The number to be associated with the AtomSet
  * @param atomSetIndex The index of the AtomSet that needs the association
  */
  void setAtomSetName(String atomSetName, int atomSetIndex) {
    atomSetNames[atomSetIndex] = atomSetName;
  }
  
  /**
   * Sets the atom set names of the last n atomSets
   * @param atomSetName The name
   * @param n The number of last AtomSets that need these set
   */
  void setAtomSetNames(String atomSetName, int n) {
    for (int idx = currentAtomSetIndex; --n >= 0; --idx)
      setAtomSetName( atomSetName, idx);
  }

  /**
  * Sets the number for the current AtomSet
  *
  * @param atomSetNumber The number for the current AtomSet.
  */
  void setAtomSetNumber(int atomSetNumber) {
    atomSetNumbers[currentAtomSetIndex] = atomSetNumber;
  }
  
  /**
  * Sets a property for the AtomSet
  *
  * @param key The key for the property
  * @param value The value to be associated with the key
  */
  void setAtomSetProperty(String key, String value) {
    setAtomSetProperty(key, value, currentAtomSetIndex);
  }

  /**
  * Sets the a property for the an AtomSet
  *
  * @param key The key for the property
  * @param value The value for the property
  * @param atomSetIndex The index of the AtomSet to get the property
  */
  void setAtomSetProperty(String key, String value, int atomSetIndex) {
    // lazy instantiation of the Properties object
    if (atomSetProperties[atomSetIndex] == null)
      atomSetProperties[atomSetIndex] = new Properties();
    atomSetProperties[atomSetIndex].put(key, value);
  }

  /**
   * Sets the same properties for the last n atomSets.
   * @param key The key for the property
   * @param value The value of the property
   * @param n The number of last AtomSets that need these set
   */
  void setAtomSetProperties(String key, String value, int n) {
    for (int idx=currentAtomSetIndex; --n >= 0; --idx) {
      setAtomSetProperty(key, value, idx);
    }    
  }
  

  /**
   * Clones the properties of the last atom set and associates it
   * with the current atom set. 
   */
  void cloneLastAtomSetProperties() {
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

  ////////////////////////////////////////////////////////////////
  // special support for alternate locations
  ////////////////////////////////////////////////////////////////

  boolean hasAlternateLocations() {
    for (int i = atomCount; --i >= 0; )
      if (atoms[i].alternateLocationID != '\0')
        return true;
    return false;
  }

  void hackAlternateLocationDamage() {
    System.out.println("hacking alternate location damage");
  }
}
