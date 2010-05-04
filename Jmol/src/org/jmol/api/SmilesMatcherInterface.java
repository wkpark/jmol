package org.jmol.api;

import java.util.BitSet;

import org.jmol.modelset.Atom;

public interface SmilesMatcherInterface {

  public abstract BitSet getSubstructureSet(String smiles, Atom[] atoms, int atomCount)
      throws Exception;

  public abstract BitSet[] getSubstructureSetArray(String smiles, Atom[] atoms, int atomCount, 
      BitSet bsSelected, BitSet bsRequired, BitSet bsNot) throws Exception;
  
  public abstract int find(String smiles1, String smiles2, boolean oneOnly);
  
}
