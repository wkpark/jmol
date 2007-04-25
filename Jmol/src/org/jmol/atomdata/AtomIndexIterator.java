package org.jmol.atomdata;

public interface AtomIndexIterator {
  boolean hasNext();
  int next();
  void release();
}
