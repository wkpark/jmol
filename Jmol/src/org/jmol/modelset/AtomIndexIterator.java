package org.jmol.modelset;

import java.util.BitSet;

import javax.vecmath.Point3f;


/**
 * note: YOU MUST RELEASE THE ITERATOR
 */
public interface AtomIndexIterator {
  /**
   * @param modelIndex
   * @param zeroBase    an offset used in the AtomIteratorWithinSet only
   * @param atomIndex
   * @param center
   * @param distance
   * @param threadSafe  don't use any cache in order to make this thread safe
   */
  public void set(int modelIndex, int zeroBase, int atomIndex, Point3f center, float distance);
  public void initialize(Point3f center, float distance);
  public void addAtoms(BitSet bsResult);
  public boolean hasNext();
  public int next();
  public float foundDistance2();
  public void release();
}
