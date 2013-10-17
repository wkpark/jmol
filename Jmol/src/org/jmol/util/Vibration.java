package org.jmol.util;

import javajs.util.P3;
import javajs.util.V3;

/**
 * A class to allow for more complex vibrations and associated 
 * phenomena, such as modulated crystals.
 * 
 * @author Bob Hanson hansonr@stolaf.edu
 * 
 */

public class Vibration extends V3 {

  protected final static double twoPI = 2 * Math.PI;

  public void setTempPoint(P3 pt, double t, double scale) {
    pt.scaleAdd2((float) (Math.cos(t * twoPI) * scale), this, pt); 
  }

}
