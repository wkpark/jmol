package org.jmol.atomdata;

import java.io.BufferedInputStream;
import java.util.BitSet;

import org.jmol.modelset.AtomIndexIterator;



public interface AtomDataServer {
  public AtomIndexIterator getWithinAtomSetIterator(BitSet bsSelected,
                                                    boolean isGreaterOnly,
                                                    boolean modelZeroBased);

  public void setIteratorForAtom(AtomIndexIterator iterator, int atomIndex, float distance);

  public void fillAtomData(AtomData atomData, int mode);
  
  public BufferedInputStream getBufferedInputStream(String fullPathName);

}
