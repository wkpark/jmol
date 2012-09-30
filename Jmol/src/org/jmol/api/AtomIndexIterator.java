package org.jmol.api;

import javax.util.BitSet;

import javax.vecmath.Point3f;

import org.jmol.atomdata.RadiusData;
import org.jmol.modelset.ModelCollection;


/**
 * note: YOU MUST RELEASE THE ITERATOR
 */
public interface AtomIndexIterator {
  /**
   * @param modelSet 
   * @param modelIndex
   * @param zeroBase    an offset used in the AtomIteratorWithinSet only
   * @param atomIndex
   * @param center
   * @param distance
   * @param rd 
   */
  public void setModel(ModelCollection modelSet, int modelIndex, int zeroBase, int atomIndex, Point3f center, float distance, RadiusData rd);
  public void setCenter(Point3f center, float distance);
  public void addAtoms(BitSet bsResult);
  public boolean hasNext();
  public int next();
  public float foundDistance2();
  public void release();
}
