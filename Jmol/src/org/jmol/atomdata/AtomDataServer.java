package org.jmol.atomdata;

import java.util.BitSet;


public interface AtomDataServer {
  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly);
  
  public void fillAtomData(AtomData atomData, int mode);
}
