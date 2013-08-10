package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

/**
 * A class to group a set of modulations for an atom as a "vibration"
 * Extends V3 so that it will be a displacement, and its value will be an occupancy
 * 
 * @author Bob Hanson hansonr@stolaf.edu 8/9/2013
 * 
 */

public class ModulationSet extends Vibration {

  public JmolList<Modulation> mods;
  public float epsilon;
  public float delta;
  public P3 r;
  public Matrix3f rot;
  public float v = Float.NaN;
  public int t;
  public Map<String, Float> htValues;
  
  public ModulationSet(JmolList<Modulation> list) {
    mods = list;
  }

  /**
   * Determine the overall modulation.
   * 
   */
  public void calculate() {
    set(0, 0, 0);
    htValues = null;
    v = Float.NaN;
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).apply(this);
    rot.transform(this);
  }

  public void addUTens(String utens, float v) {
    if (htValues == null)
      htValues = new Hashtable<String, Float>();
    Float f = htValues.get(utens);
    htValues.put(utens, Float.valueOf(f == null ? v : f.floatValue() + v));
  }

}
