package org.jmol.api;

import java.util.BitSet;

import org.jmol.modelset.Atom;

public interface SmilesMatcherInterface {

  public abstract BitSet getSubstructureSet(String smiles, Atom[] atoms, int atomCount, BitSet bsSelected, boolean asSmarts, boolean isAll)
      throws Exception;

  public abstract BitSet[] getSubstructureSetArray(String smiles, Atom[] atoms, int atomCount, 
      BitSet bsSelected, BitSet bsRequired, BitSet bsNot, BitSet bsAromatic, boolean asSmarts, boolean isAll) throws Exception;
  
  public abstract int find(String pattern,/* ...in... */ String smiles, boolean asSmarts, boolean isAll);
  
  
}
