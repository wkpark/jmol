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
  public boolean enabled = false;
  
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

  public V3 prevSetting;
  
  /**
   * Set modulation "t" value, which sets which unit cell in sequence we are looking at.
   * 
   * @param isOn
   * @param t
   * @return 0 (no change), 1 (disabled), 2 (enabled), 3 (new t), 4 (same t)
   * 
   */
  public int setModT(boolean isOn, int t) {
    if (t == Integer.MAX_VALUE) {
      if (enabled == isOn)
        return 0;
      enabled = isOn;
      scale(-1);
      return (enabled ? 2 : 1);
    }
    if (t == this.t)
      return 4;
    if (prevSetting == null)
      prevSetting = new V3(); 
    prevSetting.setT(this);
    this.t = t;
    calculate();
    enabled = false;
    return 3;
  }

}
