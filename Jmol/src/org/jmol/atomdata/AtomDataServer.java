package org.jmol.atomdata;

import java.util.BitSet;

import javax.vecmath.Point3f;


public interface AtomDataServer {
  public AtomIndexIterator getWithinAtomSetIterator(int atomIndex,
                                                    float distance,
                                                    BitSet bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased);

  public AtomIndexIterator getWithinAtomSetIterator(int modelIndex,
                                                    Point3f center,
                                                    float distance,
                                                    BitSet bsSelected);

  public void fillAtomData(AtomData atomData, int mode);
}
