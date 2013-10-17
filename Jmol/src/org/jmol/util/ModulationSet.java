package org.jmol.util;

import java.util.Hashtable;
import java.util.Map;

import javajs.util.List;
import javajs.util.M3;
import javajs.util.M4;
import javajs.util.P3;
import javajs.util.V3;

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

  private List<Modulation> mods;
  private M3 gammaE;
  private int t = Integer.MAX_VALUE;  
  private double[] qlen;
  
  int modDim;
  V3 x456;

  /**
   * A collection of modulations for a specific atom. 
   * 
   * We treat the set of modulation vectors q1,q2,q3,... as
   * a matrix Q with row 1 = q1, row 2 = q2, etc. Then we
   * have Qr = [q1.r, q2.r, q3.r,...]. 
   * 
   * Similarly, we express the x1' - xn' aspects of the operators
   * as the matrix Gamma_I (epsilons) and s_I (shifts). However, 
   * since we are only considering up to n = 3, we can express these
   * together as a 4x4 matrix just for storage. 
   * 
   * Then for X defined as [x4,x5,x6...] (column vector, really)
   * we have:
   * 
   * X' = Gamma_I * X + s_I
   *
   * and
   * 
   * X = Gamma_I^-1(X' - S_I)
   * 
   * not figured out for composite structures
   * 
   * @param id 
   * @param r 
   * @param modDim 
   * @param mods 
   * @param gammaE 
   * @param gammaIS 
   * @param q123w 
   * @param qlen 
   * 
   * 
   */

  public ModulationSet(String id, P3 r, int modDim, 
                       List<Modulation> mods, M3 gammaE, 
                       M4 gammaIS, M4 q123w, double[] qlen) {
    this.id = id;
    this.modDim = modDim;
    this.mods = mods;
    
    // set up x456
    
    this.gammaE = gammaE;
    M3 gammaIinv = new M3();
    gammaIS.getRotationScale(gammaIinv);
    V3 sI = new V3();
    
    gammaIS.get(sI);
    gammaIinv.invert();
    x456 = V3.newV(r);
    //Matrix3f m = new Matrix3f();
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
