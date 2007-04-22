package org.jmol.jvxl.api;

import java.util.BitSet;

import org.jmol.jvxl.data.AtomData;

public interface AtomDataServer {
  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly);
  
  public void fillAtomData(AtomData atomData, int mode);
}
