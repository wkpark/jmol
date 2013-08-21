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

  public float vOcc = Float.NaN;
  public Map<String, Float> htUij;
  public boolean enabled = false;
  public String id;
  public V3 prevSetting;

  public float vOcc0;

  private JmolList<Modulation> mods;
  private Matrix3f gammaE;
  private int t = Integer.MAX_VALUE;  
  private double[] qlen;
  
  int modDim;
  V3 x456;

  /**
   * A collection of modulations for a specific atom.
   * 
   * @param id 
   * @param r 
   * @param vocc0 
   * @param modDim 
   * @param mods 
   * @param gammaE 
   * @param gammaIS 
   * @param q123 
   * @param qlen 
   * 
   * 
   */

  public ModulationSet(String id, P3 r, float vocc0, int modDim, 
                       JmolList<Modulation> mods, Matrix3f gammaE, 
                       Matrix4f gammaIS, Matrix4f q123w, double[] qlen) {
    this.id = id;
    this.vOcc0 = vocc0;
    this.modDim = modDim;
    this.mods = mods;
    
    // set up x456
    
    this.gammaE = gammaE;
    Matrix3f gammaIinv = new Matrix3f();
    gammaIS.getRotationScale(gammaIinv);
    V3 sI = new V3();
    
    gammaIS.get(sI);
    gammaIinv.invert();
    x456 = V3.newV(r);
    Matrix3f m = new Matrix3f();
    q123w.transform(x456);
    x456.sub(sI);
    gammaIinv.transform(x456);
    if (Logger.debuggingHigh)
      Logger.debug("MODSET create r=" + Escape.eP(r)
        + " si=" + Escape.eP(sI) + " ginv=" + gammaIinv.toString().replace('\n',' ') + " x4=" + x456.x);

    // temporary only - only for d=1:
    this.qlen = qlen;
    
  }

  public void calculate() {
    x = y = z = 0;
    htUij = null;
    vOcc = Float.NaN;
    double offset = (t == Integer.MAX_VALUE ? 0 : qlen[0] * t);
    for (int i = mods.size(); --i >= 0;)
      mods.get(i).apply(this, offset);
    gammaE.transform(this);
  }

  public void addUTens(String utens, float v) {
    if (htUij == null)
      htUij = new Hashtable<String, Float>();
    Float f = htUij.get(utens);
    if (Logger.debuggingHigh)
      Logger.debug("MODSET " + id + " utens=" + utens + " f=" + f + " v="+ v);
    if(f != null)
      v += f.floatValue();
    htUij.put(utens, Float.valueOf(v));

  }

  
  /**
   * Set modulation "t" value, which sets which unit cell 
   * in sequence we are looking at; d=1 only.
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
    if (modDim > 1 || t == this.t)
      return 4;
    if (prevSetting == null)
      prevSetting = new V3(); 
    prevSetting.setT(this);
    this.t = t;
    calculate();
    enabled = false;
    return 3;
  }

  public String getState() {
    return "modulation " + (!enabled ? "OFF" : t == Integer.MAX_VALUE ? "ON" : "" + t);
  }

}
