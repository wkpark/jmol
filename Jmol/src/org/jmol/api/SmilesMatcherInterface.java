package org.jmol.api;



import javajs.util.Lst;
import javajs.util.P3;

import org.jmol.java.BS;
import org.jmol.util.Node;

public interface SmilesMatcherInterface {

  // Truly public
  
  public int areEqual(String smiles1, String smiles2) throws Exception;

  public abstract BS[] find(String pattern,/* ...in... */String smiles,
                                boolean isSmarts, boolean firstMatchOnly) throws Exception;

  public abstract String getLastException();

  public abstract String getMolecularFormula(String pattern, boolean isSearch) throws Exception;

  public abstract String getRelationship(String smiles1, String smiles2) throws Exception;

  public abstract String reverseChirality(String smiles) throws Exception;

  public abstract String polyhedronToSmiles(int[][] faces, int atomCount, P3[] points) throws Exception;

  
  // Internal -- Jmol use only -- 
  
  public abstract BS getSubstructureSet(String pattern, Node[] atoms,
                                            int ac, BS bsSelected,
                                            int flags) throws Exception;

  public abstract BS[] getSubstructureSetArray(String pattern,
                                                   Node[] atoms,
                                                   int ac,
                                                   BS bsSelected,
                                                   BS bsAromatic,
                                                   int flags) throws Exception;

  public abstract int[][] getCorrelationMaps(String pattern, Node[] atoms,
                                             int ac, BS bsSelected,
                                             int flags) throws Exception;

  public abstract void getSubstructureSets(String[] smarts, Node[] atoms, int ac,
                                           int flags,
                         BS bsSelected, Lst<BS> bitSets, Lst<BS>[] vRings) throws Exception;

  public abstract String getSmiles(Node[] atoms, int ac, BS bsSelected,
                                      String bioComment, int flags) throws Exception;

  public abstract String cleanSmiles(String smiles);

}
