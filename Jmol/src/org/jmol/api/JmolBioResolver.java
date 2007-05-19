package org.jmol.api;

import java.util.BitSet;

import org.jmol.modelset.Atom;
import org.jmol.modelset.Chain;
import org.jmol.modelset.Group;

public interface JmolBioResolver {

  public Group distinguishAndPropagateGroup(Chain chain, String group3, int seqcode,
                                                  int firstAtomIndex, int maxAtomIndex, 
                                                  int modelIndex, int modelCount,
                                                  int[] specialAtomIndexes,
                                                  byte[] specialAtomIDs, Atom[] atoms);
  
  public void buildBioPolymer(Group group, Group[] groups, int i);
  
  public void clearBioPolymers(Group[] groups, int groupCount, BitSet alreadyDefined);
}

