package org.jmol.util;

import javajs.util.T3;

/**
 * A class to allow for centered spin vectors.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Spin extends Vibration {
  public Spin() {
    modDim = -2;
  }
  
  @Override
  public void setTempPoint(T3 pt, T3 t456, float scale, float modulationScale) {
    // spins are not animated
  }


}
