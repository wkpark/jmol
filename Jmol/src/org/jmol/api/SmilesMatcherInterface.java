package org.jmol.api;

import java.util.BitSet;

public interface SmilesMatcherInterface {

  public abstract BitSet getSubstructureSet(String smiles, JmolNode[] atoms,
                                            int atomCount, BitSet bsSelected,
                                            boolean isSearch, boolean isAll)
      throws Exception;

  public abstract int[][] getCorrelationMaps(String smiles, JmolNode[] atoms,
                                             int atomCount, BitSet bsSelected,
                                             boolean isSearch, boolean isAll) throws Exception;
  
  public abstract BitSet[] getSubstructureSetArray(String smiles, JmolNode[] atoms, int atomCount, 
      BitSet bsSelected, BitSet bsRequired, BitSet bsNot, BitSet bsAromatic, boolean isSearch, boolean isAll) throws Exception;
  
  public abstract int find(String pattern,/* ...in... */ String smiles, boolean isSearch, boolean isAll);
 
  public abstract String getMolecularFormula(String pattern, boolean isSearch);
  
}
