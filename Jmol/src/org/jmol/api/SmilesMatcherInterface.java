package org.jmol.api;

import java.util.BitSet;

public interface SmilesMatcherInterface {

  public abstract String getLastException();
  
  public abstract BitSet getSubstructureSet(String smiles, JmolNode[] atoms,
                                            int atomCount, BitSet bsSelected,
                                            boolean isSearch, boolean isAll);

  public abstract int[][] getCorrelationMaps(String smiles, JmolNode[] atoms,
                                             int atomCount, BitSet bsSelected,
                                             boolean isSearch, boolean isAll);
  
  public abstract BitSet[] getSubstructureSetArray(String smiles, JmolNode[] atoms, int atomCount, 
      BitSet bsSelected, BitSet bsRequired, BitSet bsNot, BitSet bsAromatic, boolean isSearch, boolean isAll);
  
  public abstract BitSet[] find(String pattern,/* ...in... */ String smiles, boolean isSearch, boolean isAll);
 
  public abstract String getMolecularFormula(String pattern, boolean isSearch);
  
  public abstract String getSmiles(JmolNode[] atoms, int atomCount,
                             BitSet bsSelected, String comment, boolean asBioSmiles);
}
