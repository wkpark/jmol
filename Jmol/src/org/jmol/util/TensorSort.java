package org.jmol.util;

import java.util.Comparator;

/**
 * sort from smallest to largest absolute
 * 
 */
class TensorSort implements Comparator<Object[]> {
  protected float sigmaIso;
  protected TensorSort() {
    // not thread safe
  }
  public int compare(Object[] o1, Object[] o2) {
    float a = Math.abs(((Float) o1[1]).floatValue() - sigmaIso);
    float b = Math.abs(((Float) o2[1]).floatValue() - sigmaIso);
    return (a < b ? -1 : a > b ? 1 : 0);
  }
}