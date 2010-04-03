package org.jmol.modelset;

public interface AtomIndexIterator {
  boolean hasNext();
  int next();
  float foundDistance2();
}
