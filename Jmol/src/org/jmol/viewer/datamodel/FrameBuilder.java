/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2004  The Jmol Development Team
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
package org.jmol.viewer.datamodel;

import org.jmol.api.JmolAdapter;
import org.jmol.viewer.*;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import java.util.Properties;

final public class FrameBuilder {

  final JmolViewer viewer;
  final JmolAdapter adapter;

  public FrameBuilder(JmolViewer viewer, JmolAdapter adapter) {
    this.viewer = viewer;
    this.adapter = adapter;
  }

  boolean fileHasHbonds;

  public Frame buildFrame(Object clientFile) {
    long timeBegin = System.currentTimeMillis();
    String fileTypeName = adapter.getFileTypeName(clientFile);
    initializeBuild(adapter.getEstimatedAtomCount(clientFile));

    Frame frame = new Frame(viewer, fileTypeName);

    /****************************************************************
     * crystal cell must come first, in case atom coordinates
     * need to be transformed to fit in the crystal cell
     ****************************************************************/
    frame.fileCoordinatesAreFractional =
      adapter.coordinatesAreFractional(clientFile);
    frame.setNotionalUnitcell(adapter.getNotionalUnitcell(clientFile));
    frame.setPdbScaleMatrix(adapter.getPdbScaleMatrix(clientFile));
    frame.setPdbScaleTranslate(adapter.getPdbScaleTranslate(clientFile));

    currentModelIndex = -1;
    int modelCount = adapter.getAtomSetCount(clientFile);
    frame.setModelCount(modelCount);
    for (int i = 0; i < modelCount; ++i) {
      int modelNumber = adapter.getAtomSetNumber(clientFile, i);
      String modelName = adapter.getAtomSetName(clientFile, i);
      if (modelName == null)
        modelName = "" + modelNumber;
      Properties modelProperties = adapter.getAtomSetProperties(clientFile, i);
      frame.setModelNameNumberProperties(i, modelName, modelNumber,
                                         modelProperties);
    }

    for (JmolAdapter.AtomIterator iterAtom =
           adapter.getAtomIterator(clientFile);
         iterAtom.hasNext(); ) {
      byte elementNumber = (byte)iterAtom.getElementNumber();
      if (elementNumber <= 0)
        elementNumber = JmolConstants.
          elementNumberFromSymbol(iterAtom.getElementSymbol());
      addAtom(frame,
              iterAtom.getAtomSetIndex(),
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
        frame.defineStructure(iterStructure.getStructureType(),
                              iterStructure.getStartChainID(),
                              iterStructure.getStartSequenceNumber(),
                              iterStructure.getStartInsertionCode(),
                              iterStructure.getEndChainID(),
                              iterStructure.getEndSequenceNumber(),
                              iterStructure.getEndInsertionCode());
    frame.atomCount = atomCount;
    frame.atoms = atoms;
    frame.clientAtomReferences = clientAtomReferences;
    frame.bondCount = bondCount;
    frame.bonds = bonds;
    frame.fileHasHbonds = fileHasHbonds;

    frame.doUnitcellStuff();
    frame.doAutobond();
    finalizeGroupBuild(frame);
    buildPolymers(frame);
    frame.freeze();
    long msToBuild = System.currentTimeMillis() - timeBegin;
    //    System.out.println("Build a frame:" + msToBuild + " ms");
    adapter.finish(clientFile);
    finalizeBuild();
    dumpAtomSetNameDiagnostics(clientFile, frame);
    return frame;
  }

  void dumpAtomSetNameDiagnostics(Object clientFile, Frame frame) {
    int frameModelCount = frame.getModelCount();
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
  
  int atomCount;
  Atom[] atoms;
  Object[] clientAtomReferences;

  int bondCount;
  Bond[] bonds;

  private final Hashtable htAtomMap = new Hashtable();


  void initializeBuild(int atomCountEstimate) {
    currentModel = null;
    currentChainID = '\uFFFF';
    currentChain = null;
    currentGroupInsertionCode = '\uFFFF';

    this.atomCount = 0;
    if (atomCountEstimate <= 0)
      atomCountEstimate = ATOM_GROWTH_INCREMENT;
    atoms = new Atom[atomCountEstimate];
    clientAtomReferences = null;
    this.bondCount = 0;
    bonds = new Bond[2 * atomCountEstimate];
    htAtomMap.clear();
    initializeGroupBuild();
  }

  void finalizeBuild() {
    currentModel = null;
    currentChain = null;
    atoms = null;
    clientAtomReferences = null;
    bonds = null;
    htAtomMap.clear();
  }


  void addAtom(Frame frame,
               int modelIndex, Object atomUid,
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
      System.out.println("modelIndex=" + modelIndex);
      currentModel = frame.mmset.getModel(modelIndex);
      System.out.println("currentModel=" + currentModel);
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
    Atom atom = new Atom(viewer,
                         currentModelIndex,
                         atomCount,
                         atomicNumber,
                         atomName,
                         formalCharge, partialCharge,
                         occupancy,
                         bfactor,
                         x, y, z,
                         isHetero, atomSerial, chainID,
                         vectorX, vectorY, vectorZ);

    if (atomCount == atoms.length)
      atoms = (Atom[])Util.setLength(atoms, atomCount + ATOM_GROWTH_INCREMENT);
    atoms[atomCount] = atom;
    if (clientAtomReference != null) {
      if (clientAtomReferences == null)
        clientAtomReferences = new Object[atoms.length];
      else if (clientAtomReferences.length <= atomCount)
        clientAtomReferences =
          (Object[])Util.setLength(clientAtomReferences, atoms.length);
      clientAtomReferences[atomCount] = clientAtomReference;
    }
    ++atomCount;
    htAtomMap.put(atomUid, atom);
  }

  void bondAtoms(Object atomUid1, Object atomUid2,
                 int order) {
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

  ////////////////////////////////////////////////////////////////
  // special handling for groups
  ////////////////////////////////////////////////////////////////

  final static int defaultGroupCount = 32;
  int groupCount;
  Chain[] chains = new Chain[defaultGroupCount];
  String[] group3s = new String[defaultGroupCount];
  int[] seqcodes = new int[defaultGroupCount];
  int[] firstAtomIndexes = new int[defaultGroupCount];

  Group[] groups;

  final int[] specialAtomIndexes = new int[JmolConstants.ATOMID_MAX];

  void initializeGroupBuild() {
    groupCount = 0;
  }

  void finalizeGroupBuild(Frame frame) {
    // run this loop in increasing order so that the
    // groups get defined going up
    groups = new Group[groupCount];
    for (int i = 0; i < groupCount; ++i) {
      distinguishAndPropogateGroup(i, chains[i], group3s[i], seqcodes[i],
                                   firstAtomIndexes[i],
                                   i == groupCount - 1
                                   ? atomCount : firstAtomIndexes[i + 1]);
      chains[i] = null;
      group3s[i] = null;
    }
    frame.groupCount = groupCount;
    frame.groups = groups;
    groups = null;
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
    
    for (int i = maxAtomIndex; --i >= firstAtomIndex; ) {
      int specialAtomID = atoms[i].specialAtomID;
      if (specialAtomID > 0) {
        if (specialAtomID <  JmolConstants.ATOMID_DISTINGUISHING_ATOM_MAX)
          distinguishingBits |= 1 << specialAtomID;
        specialAtomIndexes[specialAtomID] = i;
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
                                               specialAtomIndexes, atoms);
    } else if (((distinguishingBits & JmolConstants.ATOMID_NUCLEIC_MASK) ==
                JmolConstants.ATOMID_NUCLEIC_MASK)) {
      group = NucleicMonomer.validateAndAllocate(chain, group3, seqcode,
                                                 firstAtomIndex, lastAtomIndex,
                                                 specialAtomIndexes, atoms);
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

  void buildPolymers(Frame frame) {
    Group[] groups = frame.groups;
    for (int i = 0; i < groupCount; ++i) {
      Group group = groups[i];
      if (group instanceof Monomer) {
        Monomer monomer = (Monomer)group;
        if (monomer.polymer == null)
          Polymer.allocatePolymer(groups, i);
      }
    }
  }
}

