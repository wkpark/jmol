/* $RCSfile$
 * $Author$
 * $Date$
 * $Revision$
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
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
import java.util.BitSet;

import org.jmol.util.ArrayUtil;

public final class Model {

  /*
   * In Jmol all atoms and bonds are kept as a pair of arrays in 
   * the overall Frame object. Thus, "Model" is not atoms and bonds. 
   * It is a description of all the:
   * 
   * chains (as defined in the file)
   *   and their associated file-associated groups,  
   * molecules (same, I think, but in terms of secondary structure)
   *   and their associated monomers
   * molecules (as defined by connectivity)
   *  
   * Note that "monomer" extends group. A group only becomes a 
   * monomer if it can be identified as one of the following 
   * PDB/mmCIF types:
   * 
   *   amino  -- has an N, a C, and a CA
   *   alpha  -- has just a CA
   *   nucleic -- has C1',C2',C3',C4',C5',O3', and O5'
   *   phosphorus -- has P
   *   
   * The term "conformation" is a bit loose. It means "what you get
   * when you go with one or another set of alternative locations.
   * 
   * Also held here is the "modelTag" and information
   * about how many atoms there were before symmetry was applied
   * as well as a bit about insertions and alternative locations.
   * 
   * 
   * one model = one animation "frame", but we don't use the "f" word
   * here because that would confuse the issue with the overall "Frame"
   * frame of which there is only one ever in Jmol.
   * 
   * If multiple files are loaded, then they will appear here in 
   * at least as many Model objects. Each vibration will be a complete
   * set of atoms as well. 
   *  
   */
  
  Mmset mmset;
  int modelIndex;   // our 0-based reference
  int modelNumber;  // from adapter -- possibly PDB MODEL record; possibly modelFileNumber
  int fileIndex;   // 0-based file reference
  int modelInFileIndex;   // 0-based index of model in its file
  int modelFileNumber;  // file * 1000000 + modelInFile (1-based)
  String modelNumberDotted = "1.1";
  String modelTag;
  String modelTitle;
  String modelFile;
  int firstMolecule;
  int firstAtomIndex;
  int moleculeCount;
  int nAltLocs;
  int nInsertions;
  boolean isPDB = false;
  private int chainCount = 0;
  private Chain[] chains = new Chain[8];
  private int bioPolymerCount = 0;
  private Polymer[] bioPolymers = new Polymer[8];


  Model(Mmset mmset, int modelIndex, int modelNumber,
      String modelTag, String modelTitle, String modelFile) {
    this.mmset = mmset;
    this.modelIndex = modelIndex;
    this.modelNumber = modelNumber;
    this.modelTag = modelTag;
    this.modelTitle = modelTitle;
    this.modelFile = modelFile;
  }

  void setNAltLocs(int nAltLocs) {
    this.nAltLocs = nAltLocs;  
  }
  
  void setNInsertions(int nInsertions) {
    this.nInsertions = nInsertions;  
  }
  
  void freeze() {
    //Logger.debug("Mmset.freeze() chainCount=" + chainCount);
    chains = (Chain[])ArrayUtil.setLength(chains, chainCount);
    for (int i = 0; i < chainCount; ++i)
      chains[i].freeze();
    bioPolymers = (Polymer[])ArrayUtil.setLength(bioPolymers, bioPolymerCount);
  }

  void clearStructures() {
    chainCount = 0;
    chains = new Chain[8];
    bioPolymerCount = 0;
    bioPolymers = new Polymer[8];
  }
  
  void addSecondaryStructure(byte type,
                             char startChainID, int startSeqcode,
                             char endChainID, int endSeqcode) {
    for (int i = bioPolymerCount; --i >= 0; ) {
      Polymer polymer = bioPolymers[i];
      polymer.addSecondaryStructure(type, startChainID, startSeqcode,
                                    endChainID, endSeqcode);
    }
  }

  void calculateStructures() {
    for (int i = bioPolymerCount; --i >= 0; ) 
      bioPolymers[i].calculateStructures();
  }

  void setConformation(BitSet bsConformation) {
    for (int i = bioPolymerCount; --i >= 0; )
      bioPolymers[i].setConformation(bsConformation, nAltLocs);
  }

  int getChainCount() {
    return chainCount;
  }

  public int getBioPolymerCount() {
    return bioPolymerCount;
  }

  void calcSelectedGroupsCount(BitSet bsSelected) {
    for (int i = chainCount; --i >= 0; )
      chains[i].calcSelectedGroupsCount(bsSelected);
  }

  void calcSelectedMonomersCount(BitSet bsSelected) {
    for (int i = bioPolymerCount; --i >= 0; )
      bioPolymers[i].calcSelectedMonomersCount(bsSelected);
  }

  void selectSeqcodeRange(int seqcodeA, int seqcodeB, BitSet bs) {
    for (int i = chainCount; --i >= 0; )
      chains[i].selectSeqcodeRange(seqcodeA, seqcodeB, bs);
  }

  int getGroupCount() {
    int groupCount = 0;
    for (int i = chainCount; --i >= 0; )
      groupCount += chains[i].getGroupCount();
    return groupCount;
  }

  Chain getChain(char chainID) {
    for (int i = chainCount; --i >= 0; ) {
      Chain chain = chains[i];
      if (chain.getChainID() == chainID)
        return chain;
    }
    return null;
  }

  Chain getChain(int i) {
    return (i < chainCount ? chains[i] : null);
  }

  Chain getOrAllocateChain(char chainID) {
    //Logger.debug("chainID=" + chainID + " -> " + (chainID + 0));
    Chain chain = getChain(chainID);
    if (chain != null)
      return chain;
    if (chainCount == chains.length)
      chains = (Chain[])ArrayUtil.doubleLength(chains);
    return chains[chainCount++] = new Chain(mmset.modelSet, this, chainID);
  }

  public void addBioPolymer(Polymer polymer) {
    if (bioPolymers.length == 0)
      bioPolymers = new Polymer[8];
    if (bioPolymerCount == bioPolymers.length)
      bioPolymers = (Polymer[])ArrayUtil.doubleLength(bioPolymers);
    bioPolymers[bioPolymerCount++] = polymer;
  }

  public Polymer getBioPolymer(int polymerIndex) {
    return bioPolymers[polymerIndex];
  }

  void calcHydrogenBonds(BitSet bsA, BitSet bsB) {
    for (int i = bioPolymerCount; --i >= 0; )
      bioPolymers[i].calcHydrogenBonds(bsA, bsB);
  }
  
  public boolean isAtomHidden(int index) {
    return mmset.modelSet.isAtomHidden(index);
  }
  
  public void addHydrogenBond(Atom atom1, Atom atom2, short order, BitSet bsA, BitSet bsB) {
    mmset.modelSet.addHydrogenBond(atom1, atom2, order, bsA, bsB);
  }

  public int getModelIndex() {
    return modelIndex;
  }  
}
