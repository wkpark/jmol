/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2003  The Jmol Development Team
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
package org.openscience.jmol.viewer.datamodel;

import org.jmol.api.ModelAdapter;
import org.openscience.jmol.viewer.*;
import java.util.Hashtable;
import javax.vecmath.Point3f;

final public class FrameBuilder {

  final JmolViewer viewer;
  final ModelAdapter adapter;

  public FrameBuilder(JmolViewer viewer, ModelAdapter adapter) {
    this.viewer = viewer;
    this.adapter = adapter;
  }

  protected void finalize() {
    System.out.println("FrameBuilder.finalize() called!");
  }

  public Frame buildFrame(Object clientFile) {
    long timeBegin = System.currentTimeMillis();
    String fileTypeName = adapter.getFileTypeName(clientFile);
    initializeBuild(adapter.getAtomCount(clientFile));

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

    for (ModelAdapter.AtomIterator iterAtom =
           adapter.getAtomIterator(clientFile);
         iterAtom.hasNext(); ) {
      byte elementNumber = (byte)iterAtom.getElementNumber();
      if (elementNumber <= 0)
        elementNumber = JmolConstants.
          elementNumberFromSymbol(iterAtom.getElementSymbol());
      addAtom(frame,
              iterAtom.getModelNumber(), iterAtom.getUniqueID(),
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
    
    {
      ModelAdapter.BondIterator iterBond =
        adapter.getBondIterator(clientFile);
      if (iterBond != null)
        while (iterBond.hasNext())
          bondAtoms(iterBond.getAtomUid1(),
                    iterBond.getAtomUid2(),
                    iterBond.getOrder());
    }

    ModelAdapter.StructureIterator iterStructure =
      adapter.getStructureIterator(clientFile);
    if (iterStructure != null) 
      while (iterStructure.hasNext())
        frame.mmset.defineStructure(iterStructure.getStructureType(),
                                       iterStructure.getChainID(),
                                       iterStructure.getStartSequenceNumber(),
                                       iterStructure.getStartInsertionCode(),
                                       iterStructure.getEndSequenceNumber(),
                                       iterStructure.getEndInsertionCode());
  
    frame.atomCount = atomCount;
    frame.atoms = atoms;
    frame.clientAtomReferences = clientAtomReferences;
    frame.bondCount = bondCount;
    frame.bonds = bonds;

    frame.freeze();
    long msToBuild = System.currentTimeMillis() - timeBegin;
    System.out.println("Build a frame:" + msToBuild + " ms");
    adapter.finish(clientFile);
    finalizeBuild();
    return frame;
  }

  private final static int ATOM_GROWTH_INCREMENT = 2000;

  int currentModelID;
  Model currentModel;
  char currentChainID;
  Chain currentChain;
  int currentGroupSequenceNumber;
  char currentGroupInsertionCode;
  Group currentGroup;
  
  int atomCount;
  Atom[] atoms;
  Object[] clientAtomReferences;

  int bondCount;
  Bond[] bonds;

  private final Hashtable htAtomMap = new Hashtable();


  void initializeBuild(int atomCountEstimate) {
    currentModelID = Integer.MIN_VALUE;
    currentModel = null;
    currentChainID = '\uFFFF';
    currentChain = null;
    currentGroupInsertionCode = '\uFFFF';
    currentGroup = null;

    this.atomCount = 0;
    if (atomCountEstimate <= 0)
      atomCountEstimate = ATOM_GROWTH_INCREMENT;
    atoms = new Atom[atomCountEstimate];
    clientAtomReferences = null;
    this.bondCount = 0;
    bonds = new Bond[2 * atomCountEstimate];
    htAtomMap.clear();
  }

  void finalizeBuild() {
    currentModel = null;
    currentChain = null;
    currentGroup = null;
    atoms = null;
    clientAtomReferences = null;
    bonds = null;
    htAtomMap.clear();
  }


  void addAtom(Frame frame,
                      int modelID, Object atomUid,
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
    if (modelID != currentModelID) {
      currentModelID = modelID;
      currentModel = frame.mmset.getOrAllocateModel(modelID);
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
      currentGroup =
        currentChain.allocateGroup(frame, group3,
                                   groupSequenceNumber, groupInsertionCode);
    }
    Atom atom = new Atom(currentGroup,
                         atomCount,
                         atomicNumber,
                         atomName,
                         formalCharge, partialCharge,
                         occupancy,
                         bfactor,
                         x, y, z,
                         isHetero, atomSerial, chainID,
                         vectorX, vectorY, vectorZ);
    currentGroup.registerAtom(atom);

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
    Bond bond = atom1.bondMutually(atom2, order);
    if (bond != null)
      bonds[bondCount++] = bond;
  }
}
