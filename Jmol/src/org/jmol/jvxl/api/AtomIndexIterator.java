package org.jmol.jvxl.api;

public interface AtomIndexIterator {
  boolean hasNext();
  int next();
  void release();
}
