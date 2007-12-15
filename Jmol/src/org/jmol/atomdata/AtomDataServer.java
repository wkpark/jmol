package org.jmol.atomdata;

import java.util.BitSet;

import org.jmol.modelset.AtomIndexIterator;


public interface AtomDataServer {
  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased);

  public void fillAtomData(AtomData atomData, int mode);
}
