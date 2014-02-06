package org.jmol.api;



import org.jmol.java.BS;

import javajs.util.List;
import org.jmol.util.JmolNode;

public interface SmilesMatcherInterface {

  public abstract String getLastException();

  public int areEqual(String smiles1, String smiles2) throws Exception;

  public abstract BS[] find(String pattern,/* ...in... */String smiles,
                                boolean isSmarts, boolean firstMatchOnly) throws Exception;

  public abstract BS getSubstructureSet(String pattern, JmolNode[] atoms,
                                            int atomCount, BS bsSelected,
                                            boolean isSmarts,
                                            boolean firstMatchOnly) throws Exception;

  public abstract BS[] getSubstructureSetArray(String pattern,
                                                   JmolNode[] atoms,
                                                   int atomCount,
                                                   BS bsSelected,
                                                   BS bsAromatic,
                                                   boolean isSmarts,
                                                   boolean firstMatchOnly) throws Exception;

  public abstract int[][] getCorrelationMaps(String pattern, JmolNode[] atoms,
                                             int atomCount, BS bsSelected,
                                             boolean isSmarts,
                                             boolean firstMatchOnly) throws Exception;

  public abstract String getMolecularFormula(String pattern, boolean isSearch) throws Exception;

  public abstract String getSmiles(JmolNode[] atoms, int atomCount,
                                   BS bsSelected, boolean asBioSmiles,
                                   boolean bioAllowUnmatchedRings,
                                   boolean bioAddCrossLinks, String bioComment,
                                   boolean explicitH) throws Exception;

  public abstract String getRelationship(String smiles1, String smiles2) throws Exception;

  public abstract String reverseChirality(String smiles) throws Exception;

  public abstract void getSubstructureSets(String[] smarts, JmolNode[] atoms, int atomCount,
                                           int flags,
                         BS bsSelected, List<BS> bitSets, List<BS>[] vRings) throws Exception;
}
